# Porting the stand-alone MUMPS JNI library to Windows

Handoff notes from the Linux build session (2026-09-07/08).
Companion files in this directory: `Makefile.mumps` — its header comment and
link line carry the reasoning behind every decision and are the primary
reference — and `MUMPS_COMPILATION.md` for building the upstream packages.

--------------------------------------------------------------------------
## 1. What the Linux build actually is

MUMPS 5.9.1, **MPI-free but OpenMP-threaded**, linked against Intel oneMKL,
packaged as a single stand-alone `libMumpsJNI.so.5.9.1` in
`artisynth_core/lib/Linux64/`, alongside `libPardisoJNI.so.2025.3`.

Configuration decisions (all verified on Linux):

| Item          | Choice                                          |
|---------------|-------------------------------------------------|
| Parallelism   | OpenMP only. No MPI; MUMPS' `libseq`/`libmpiseq` stub replaces it. No ScaLAPACK. |
| Integers      | 32-bit, LP64 interface throughout (MUMPS, METIS `IDXTYPEWIDTH 32`, SCOTCH `INTSIZE=32`) |
| Orderings     | PORD (bundled) + METIS 5.1.0 + SCOTCH 7.0.12. `-Dpord -Dmetis -Dscotch`. NOT `-Dptscotch`/`-Dparmetis` (those are the MPI variants). |
| BLAS/LAPACK   | Intel oneMKL, static, hidden from the DLL's export table |
| OpenMP runtime| `libiomp5` — **shared, one per process, deliberately shared with Pardiso** |
| Linkage       | Everything static except the OpenMP + language runtimes |

Measured on Linux: 216k-unknown 3D Laplacian, 7.15 s @ 1 thread -> 2.78 s @ 8
threads. All 7 `ICNTL(7)` orderings work from inside the library, which exports
only its `Java_maspack_solvers_MumpsSolver_*` entry points;
`libPardisoJNI.so.2025.3` exports 18,387 symbols.

Target application: symmetric indefinite KKT systems `[M G^T; G R]`, `SYM=2`.
Recommended ordering: **METIS** (`ICNTL(7)=5`) with default `ICNTL(12)`;
`ICNTL(13)=1` if exact inertia is wanted in `INFOG(12)`.

--------------------------------------------------------------------------
## 2. What to copy to the Windows machine

**Copy SOURCE trees only.** The `*-static` prefixes hold Linux ELF `.a`
archives and are worthless on Windows.

| Copy | Path | Notes |
|------|------|-------|
| YES | `packages/MUMPS_5.9.1` | Run `make clean` first — drops ~65 MB of `.o`/`.a`/`.mod`. Keep `Makefile.inc`, which carries the commented rationale for every setting. |
| YES | `packages/scotch-v7.0.12` | The **source** tree. Delete `build/` and `build-static/` first (19 MB of Linux objects). |
| YES | `packages/metis-5.1.0.tar.gz` | The extracted METIS source tree no longer exists on the Linux box — only the tarball. |
| no  | `packages/metis-5.1.0-static` | Linux binaries. The build recipe is in `MUMPS_COMPILATION.md`. |
| no  | `packages/scotch-7.0.12-static` | Same. |
| no  | `packages/scotch-7.0.12` | The earlier shared install, superseded. |

Suggested staging on Linux:

```sh
cd /home/lloyd/packages
make -C MUMPS_5.9.1 clean
rm -rf scotch-v7.0.12/build scotch-v7.0.12/build-static
tar czf mumps-win-handoff.tar.gz \
    MUMPS_5.9.1 scotch-v7.0.12 metis-5.1.0.tar.gz
```

Note `MUMPS_5.9.1/Makefile.inc` is the *Linux* config. On Windows start from
`Make.inc/Makefile.WIN.MS-Intel.SEQ` instead, but read the Linux `Makefile.inc`
for the commented rationale of each setting.

`scotch-7.0.12/include-mumps/` was a workaround for SCOTCH installing a
METIS-compatibility `metis.h` that shadows the real one. It is **not needed**
if you build SCOTCH with `-DBUILD_LIBSCOTCHMETIS=OFF -DINSTALL_METIS_HEADERS=OFF`,
which is what the static build already does.

--------------------------------------------------------------------------
## 3. What must be installed on Windows (not copyable)

1. **Intel oneMKL for Windows** — separate download. Static libs are
   `mkl_intel_lp64.lib mkl_intel_thread.lib mkl_core.lib`.
   Note: there is no `mkl_gf_*` on Windows; the Linux build used
   `libmkl_gf_lp64.a` only because it was compiled by gfortran.
2. **A Fortran compiler** — see section 4. This is the one real decision.
3. **CMake** (>= 3.18 preferred) for METIS and SCOTCH.
4. **JDK** for `jni.h` / `jni_md.h`.
5. **GNU make + a POSIX shell** if you drive MUMPS' own Makefile — it is a
   GNU makefile and the Windows template still says `RM = /bin/rm -f`.
   MSYS2 or Git-for-Windows bash providing `make`/`rm`, with `cl`/`ifx` doing
   the actual compiling, is the usual arrangement.

--------------------------------------------------------------------------
## 4. Compiler choice — the Linux answer INVERTS on Windows

On Linux we concluded gfortran was fine and Intel `ifx` was not worth
installing. **Do not carry that conclusion across.** On Windows, use
**Intel Fortran (`ifx`) + MSVC (`cl`)**.

Why it flips:

- On Linux, gfortran and its runtime are present on every target machine, so
  shipping `libgfortran.so.5` + `libquadmath.so.0` cost 3.2 MB and nothing else.
  On Windows there is no system Fortran runtime at all — a gfortran build means
  shipping MinGW runtime DLLs into an otherwise-MSVC application.
- MUMPS ships `Make.inc/Makefile.WIN.MS-Intel.SEQ` for exactly this combination.
- The MKL interface layer (`mkl_intel_lp64.lib`) and the OpenMP runtime
  (`libiomp5md.dll`) are both the Intel-native ones — no ABI shims.
- `libiomp5md.dll` is almost certainly what ArtiSynth's Windows Pardiso DLL
  already uses, so MUMPS and Pardiso share one OpenMP runtime, exactly as on
  Linux.
- `-static-intel` (or `/libs:static`) links the Intel Fortran runtime into the
  DLL, so there is nothing to ship for Fortran.

MinGW-w64 gfortran is the fallback if an Intel toolchain is impossible. It
mirrors the Linux build closely and its `libgfortran.a` *is* linkable into a
DLL (the `-fPIC` problem is Linux-specific and does not exist on Windows), but
you then ship `libgfortran-5.dll`/`libquadmath-0.dll`/`libwinpthread-1.dll`
into an MSVC application and link MSVC COFF static MKL libs from MinGW. Both
work, neither is pleasant. Prefer Intel.

--------------------------------------------------------------------------
## 5. Problems that DISAPPEAR on Windows

- **Non-PIC `libgfortran.a`** — the central Linux blocker. `-fPIC` is
  meaningless on Windows; nothing to work around.
- **`DT_RUNPATH` not inherited by transitive dependencies** — the bug that made
  the shipped `libquadmath.so.0` silently unused. No ELF dynamic tags on
  Windows, so it cannot recur. (The Linux fix, for the record, was
  `-Wl,--no-as-needed -lquadmath -Wl,--as-needed` to force a direct
  `DT_NEEDED`.)
- **Symbol leakage.** On Linux we needed `-Wl,--version-script` +
  `-Wl,--exclude-libs,ALL` to stop 18k MKL symbols entering the export table.
  Windows DLLs export nothing unless declared, so `__declspec(dllexport)` on
  the JNI entry points (or a `.def` file) gives the same result for free.
- **`--as-needed`** — not a thing on the MSVC linker.

## 6. Problems that APPEAR on Windows

- **No `$ORIGIN`.** Windows has no runpath. The default search order does not
  include the loading DLL's own directory, so a DLL sitting next to
  `MumpsAll.dll` in `lib/Windows64/` is not automatically found.
  Two mitigations, use both:
  1. Statically link *everything* except `libiomp5md.dll`. With Intel Fortran
     this is achievable — the Linux build could not do it only because of
     `libgfortran.a`.
  2. For `libiomp5md.dll`, mirror whatever ArtiSynth already does for Pardiso
     on Windows. Note that the loader matches already-loaded modules by name
     first, so if Pardiso loads first the problem does not arise — but do not
     rely on load order. (This Linux checkout has no `lib/Windows64/` to
     inspect; check the Windows side of the repo.)
- **METIS 5.1.0 under MSVC.** It is from 2013. GKlib bundles `ms_stdint.h` /
  `ms_inttypes.h` for MSVC and it does build, but the stock CMake is finicky.
  Keep `IDXTYPEWIDTH 32`.
- **MUMPS' Makefile is GNU make**, not nmake. See section 3 item 5. If that
  proves painful, third-party CMake ports of the MUMPS source exist and handle
  Windows; unverified here.
- **Do not add `-march=native`.** The Linux `Makefile.inc` originally had it
  and it has since been removed — it had emitted FMA and BMI2 into MUMPS' own
  code, which would `SIGILL` on anything older than Haswell (2013). The Linux
  build is now plain `-O2` (baseline x86-64: no FMA, no BMI2, no AVX, no
  AVX-512 outside MKL) and measured 3% slower at 1 thread, faster than noise
  at 8 — the flops are all in MKL, which dispatches on the CPU at run time.
  Keep the Windows build equally conservative.

--------------------------------------------------------------------------
## 7. Build recipes to translate

METIS (from `MUMPS_COMPILATION.md`; Linux form):

    cmake -H. -Bbuild -DGKLIB_PATH=$PWD/GKlib -DSHARED=0 \
          -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DCMAKE_C_FLAGS="-fPIC -O2" \
          -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=<prefix>

SCOTCH 7.0.12 (Linux form):

    cmake -S . -B build-static -DBUILD_SHARED_LIBS=OFF \
          -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DCMAKE_C_FLAGS="-fPIC -O2" \
          -DCMAKE_BUILD_TYPE=Release -DINTSIZE=32 -DTHREADS=ON \
          -DBUILD_PTSCOTCH=OFF -DBUILD_LIBESMUMPS=ON \
          -DBUILD_LIBSCOTCHMETIS=OFF -DINSTALL_METIS_HEADERS=OFF \
          -DUSE_ZLIB=OFF -DUSE_LZMA=OFF -DUSE_BZ2=OFF -DENABLE_TESTS=OFF \
          -DCMAKE_INSTALL_PREFIX=<prefix>

On Windows drop `-fPIC` / `CMAKE_POSITION_INDEPENDENT_CODE` (no-ops), keep
everything else. `ENABLE_TESTS=OFF` was needed for CMake 3.16 (`cmake -E cat`
arrived in 3.18); harmless to keep. `BUILD_LIBSCOTCHMETIS=OFF` matters twice:
it keeps SCOTCH's `METIS_NodeND` from colliding with real METIS in the static
link, and stops SCOTCH installing a shadowing `metis.h`.

Link line: translate the `LDS_MUMPS` / `LDS_MKL` / `LDS_MUMPS_JNI` variables in
`Makefile.mumps` piece by piece —
`libdmumps` `libmumps_common` `libpord` `libmpiseq`, then
`libesmumps` `libscotch` `libscotcherr`, then `libmetis`, then the MKL trio
(`--start-group`/`--end-group` has no MSVC equivalent and is not needed;
`link.exe` resolves archives iteratively), then `libiomp5md.lib`.

Do NOT compile MUMPS with `-fopenmp`/`-qopenmp` *and* link a second OpenMP
runtime. On Linux we compiled with `-fopenmp` but linked `-liomp5` instead of
`-lgomp`, because libiomp5 implements the full GOMP ABI. With `ifx` the
question does not arise: `-qopenmp` targets libiomp5 natively.

--------------------------------------------------------------------------
## 8. Verification checklist (mirror of what was done on Linux)

1. `dumpbin /DEPENDENTS MumpsAll.dll` — expect only system DLLs plus
   `libiomp5md.dll`. Anything else is something you forgot to link statically.
2. `dumpbin /EXPORTS MumpsAll.dll` — expect only the entry points you declared with `__declspec(dllexport)` — not MKL's internals.
3. Solve a small problem at each `ICNTL(7)=0,2,3,4,5,6,7` and confirm
   `INFOG(7)` echoes the request and the solutions match — this is what
   catches a missing or mis-linked ordering package.
4. Time it at 1 thread vs N to confirm threading is live. **Watch for
   `OMP_NUM_THREADS=1` in the environment** — it is set in the Linux box's
   `~/.cshrc` and silently defeats the threaded build.
5. Load it into one process together with the Pardiso DLL and confirm both
   still work.

--------------------------------------------------------------------------
## 9. Licensing (unchanged by the port)

- MUMPS: CeCILL-C — source-availability obligation
- SCOTCH: CeCILL-C — same
- METIS: Apache-2.0
- MKL: redistributable under Intel's license

Static linking does not remove the CeCILL-C obligation.
