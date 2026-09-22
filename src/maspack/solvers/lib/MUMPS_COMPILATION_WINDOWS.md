# Building the MUMPS JNI library on Windows

Status: DONE (2026-09-21). `MumpsJNI.5.9.1.dll` builds, links, and runs
correctly alongside `PardisoJNI.2021.1.1.dll` in one JVM process
(`MumpsSolverTest`, `PardisoSolverTest`, and a combined-load test all pass).
See `MUMPS_COMPILATION.md` for the Linux build (source trees, METIS/SCOTCH
config) — that doc has no Windows content; this one covers only what's
different on Windows.

Build with `nmake /F NMakefile.mumps` (Mumps) and `nmake /F NMakefile`
(Pardiso) from the "x64 Native Tools Command Prompt". Both makefiles carry
the authoritative reasoning in their own header comments and link lines —
this file is the condensed "why" and the gotchas, not a step-by-step redo.

## Toolchain

VS Community 2026 (internal version 18) + oneAPI 2026.1, `ifx` only
(classic `ifort` is gone). `setvars.bat`'s component dispatch is broken on
this install — call the component scripts directly, vcvars64 first:

    call "...\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvars64.bat"
    call "...\Intel\oneAPI\compiler\2026.1\env\vars.bat"
    call "...\Intel\oneAPI\mkl\2026.1\env\vars.bat"

Static METIS 5.1.0 / SCOTCH 7.0.12 / MUMPS 5.9.1 libs are prerequisites,
built with GNU make (MUMPS' Makefiles aren't nmake-compatible) from
`Make.inc/Makefile.WIN.MS-Intel.SEQ` as the base `Makefile.inc`. Fixes
needed on top of that template:

1. **METIS/SCOTCH CMake predates CMake 3.5** — add
   `-DCMAKE_POLICY_VERSION_MINIMUM=3.5` to both configure lines.
2. **GKlib's `gk_arch.h`** redefines `rint`/`INFINITY` for any `_MSC_VER`,
   corrupting `corecrt_math.h`. Patch the guard to
   `defined(__MSC__) && _MSC_VER < 1900`.
3. **METIS has no Windows `install` target** (`METIS_INSTALL FALSE` under
   MSVC) — copy `include/metis.h` and `build/libmetis/metis.lib` by hand,
   **renaming to `libmetis.lib`** (CMake drops the `lib` prefix on Windows;
   MUMPS' `Makefile.inc` expects it). SCOTCH's `install` target works but
   has the same no-prefix naming (`scotch.lib`, `scotcherr.lib`,
   `esmumps.lib`) — set `LSCOTCH` accordingly.
4. **`IMETIS` needs an explicit `-I`** in `Makefile.inc` (unlike
   `IPORD`/`ISCOTCH`, which already have one) or `metis.h` isn't found.
5. **oneMKL 2026.1 has no `mkl_intel_c.lib`** (old Composer-XE name). Use
   `mkl_intel_lp64.lib mkl_intel_thread.lib mkl_core.lib libiomp5md.lib`.
6. **Invoke `make` unqualified**, not by full path — MUMPS' Makefile
   recurses via `$(MAKE)`, and a quoted full path with spaces breaks
   unquoted in its internal `sh -c` line. Put
   `C:\Program Files\Git\mingw64\bin` on PATH and run `make -j8 all` (or
   `make d` alone to skip `examples/`, which doesn't link on Windows and
   isn't needed — cosmetic, not part of the JNI bundle).

## The `/MD` requirement, and the shared `hybridSolve.obj` trap

MUMPS statically links prebuilt libs (`libdmumps.lib`, METIS's
`libmetis.lib`, SCOTCH's `scotch.lib`/etc) that were themselves built
`/MD` (METIS/SCOTCH's CMake Release default, and MUMPS' own Windows
template). All end up in one `MumpsJNI.dll`, so `NMakefile.mumps` compiles
its own objects `/MD` too — mixing `/MD` and `/MT` objects in one binary
produces `LNK4098` and a real CRT mismatch (duplicate symbols / mismatched
heap), not just a warning to ignore.

`hybridSolve.cc` (the shared GMRES/CGS hybrid-solve code) is compiled
separately by *both* `NMakefile` and `NMakefile.mumps`, into an object of
the same name (`hybridSolve.obj`) in the same `lib/` directory. Neither
makefile's dependency rule knows about the other's flags — whichever you
build second silently reuses the first's `.obj` if its timestamp is newer,
regardless of which CRT it was compiled with. `NMakefile` now also uses
`/MD` (matching Mumps) specifically so this is safe either build order;
if that ever changes again, delete `hybridSolve.obj` before switching
which target you build.

## Dynamic loading: DLLs must be pre-loaded in the right order

Windows has no `$ORIGIN`/runpath — a DLL's own directory (`lib\Windows64`)
is **not** searched for its dependencies unless that dependency is already
loaded in the process by name, or the directory is otherwise on the
search path (app dir, system dirs, PATH). `NativeLibraryManager` works
around this by pre-loading shared runtime DLLs explicitly, by full path,
before the JNI library that needs them — this makes them resolve by
already-loaded-name lookup regardless of search path.

- **`libiomp5md.dll`**, shared with Pardiso: both JNI libraries must load
  the *same* copy. MUMPS compiled by ifx 2026.1 needs OpenMP 5.1's
  `__kmpc_masked`/`__kmpc_end_masked`; an older `libiomp5md.dll` (e.g. one
  built for an older MKL) lacks these exports and MumpsJNI fails to load
  with "The specified procedure could not be found". A private,
  renamed copy of the newer DLL is **not** a fix — Intel's OpenMP runtime
  refuses to let two different copies of itself coexist in one process
  (`OMP: Error #15`) even under different filenames. The only real fix is
  rebuilding *both* JNI libraries against the same oneAPI version so they
  share one `libiomp5md.dll`.
- **`libmmd.dll` / `svml_dispmd.dll` / `libifcoremd.dll`** (Intel's
  Fortran/SVML runtime): needed only by MUMPS, not Pardiso (no Fortran
  code), and not present in any standard system search location — only in
  `lib\Windows64`. `MumpsSolver.doLoadLibraries()` originally pre-loaded
  only `libiomp5md` before `MumpsJNI.5.9.1.dll`, which left these three
  unresolved (`libifcoremd.dll` itself depends on `libmmd.dll`, which
  isn't found either, since neither is resident yet) —
  `UnsupportedOperationException: MUMPS not available`. Fixed by
  pre-loading `libmmd`, then `svml_dispmd`, then `libifcoremd` (dependency
  order) before `MumpsJNI.5.9.1.dll`, mirroring the existing `libiomp5md`
  pre-load — see the `Windows32`/`Windows64` case in
  `MumpsSolver.doLoadLibraries()`.

If a future MUMPS/ifx rebuild introduces a new dynamically-linked Intel
runtime DLL, expect the same failure mode and the same fix: add it to the
pre-load list before `MumpsJNI` loads.

## Verification

    nmake /F NMakefile.mumps check

checks deps/exports (`dumpbin /DEPENDENTS`, `/EXPORTS`) — expect only
`libiomp5md.dll` + its own Intel runtime deps + system DLLs, and only the
`Java_maspack_solvers_MumpsSolver_*` entry points (currently 48). Then
run `MumpsSolverTest`, `PardisoSolverTest`, and a combined test
instantiating both solvers in one JVM, solving the same matrix, and
comparing results — this is what actually catches the dynamic-loading
issue above, since `check` alone can't (it doesn't execute the DLL).

## Licensing (unchanged by the port)

MUMPS and SCOTCH: CeCILL-C (source-availability obligation, not removed by
static linking). METIS: Apache-2.0. MKL: redistributable under Intel's
license.
