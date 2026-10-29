# Building the Accelerate JNI library on macOS (Apple Silicon)

This is the counterpart of `MUMPS_COMPILATION_MACOS.md` for
`maspack.solvers.AccelerateSolver`, which is described in
`../ACCELERATE_SOLVER.md`. It produces
`lib/MacOSArm64/libAccelerateJNI.1.0.0.dylib`, which is loaded by an arm64
JVM (`NativeLibraryManager` system type `MacOSArm64`).

Verified 2026-10-10 on an M5 Pro, macOS 26.6, Xcode Command Line Tools
(Apple clang 21, SDK 27.0) and Oracle JDK 21 arm64. The minimum macOS
version is 13.0.

## Differences from the MUMPS library

| Item | libMumpsJNI | libAccelerateJNI |
|---|---|---|
| Solver | MUMPS 5.9.1, static, with METIS, SCOTCH and PORD | Accelerate's `Sparse/Solve.h` (system framework) |
| Compilers | GCC 16.2 built from source (Fortran), clang++ | Apple clang++ only |
| Runtimes | static libgfortran, libquadmath and libomp | none |
| Third-party sources | MUMPS, METIS, SCOTCH, GCC, LLVM | none |
| Sources | `MumpsJNI.cc`, `mumps.cc`, `hybridSolve.cc` | `accelerateJNI.cc`, `hybridSolve.cc` |
| Dependencies | Accelerate, libc++, libSystem | Accelerate, libc++, libSystem |
| Minimum OS | 13.0 (runtimes built for it) | 13.0 (`SparseGetInertia`) |

The Accelerate headers use clang's `overloadable` functions, so this
library cannot be compiled with GCC.

## Prerequisites

Only sections 1 (Command Line Tools) and 2 (JDK) of
`MACOS_COMPILE_ENV.md` are needed: no Homebrew, GCC or libomp. The SDK
must be **macOS 26 or later**, because `accelerateJNI.cc` uses
`SparseFactorizationLUTPP` (declared from the 15.5 SDK on) and
`SparseOrderMTMetis` (from the 26 SDK on). Check the version with
`xcrun --show-sdk-version`.

## Build

    cd artisynth_core/src/maspack/solvers/lib
    make -f Makefile.accelerate

Flags (`CXXFLAGS` in `Makefile.accelerate`):

- `-arch arm64 -mmacosx-version-min=13.0`: the deployment target. To
  change it, see `MACOS_COMPILE_ENV.md`. It cannot go below 13.0, which
  `SparseGetInertia` requires.
- `-DNO_MKL`: hybridSolve.cc uses a plain CRS matrix-vector product, as
  for MUMPS.
- `-DDARWIN`: hybridSolve.cc counts performance cores only on Macs with
  efficiency cores (see "Threads" in `../MUMPS_SOLVER.md`).
  `AccelerateSolver.getPhysicalCoreCount()` reports this count, but it
  has no effect on Accelerate itself.

The object files have an `acc_` prefix, so they do not collide with those
of `Makefile.mumps`, which compiles the same `hybridSolve.cc`.
`maspack_solvers_AccelerateSolver.h` is regenerated with `javac -h`
whenever `AccelerateSolver.java` changes. This needs ArtiSynth's
`classes` to be compiled. Without it, a new native method would fail only
at run time, with `UnsatisfiedLinkError`.

`make -f Makefile.accelerate clean` removes the objects and the
generated header.

## Newer APIs, used through runtime checks

`accelerateJNI.cc` targets 13.0 and guards anything newer with
`__builtin_available`, so the same library loads on every supported
version:

| Feature | Needs | Without it |
|---|---|---|
| LU (`SparseFactorizationLUTPP`), for unsymmetric matrices | macOS 15.5 | `AccelerateSolver.isLUAvailable()` is false and analyzing an unsymmetric matrix fails. `KKTSolver` then uses MUMPS instead. |
| `SparseOrderMTMetis` (env `ACCELERATE_ORDER=mtmetis`) | macOS 26 | METIS ordering |

If code is added that uses a newer API without such a check, the compiler
warns with `-Wunguarded-availability`, which is on by default. The library
should build with no warnings.

## Check

There is no `check` target. Run the equivalent commands from
`artisynth_core`:

    L=lib/MacOSArm64/libAccelerateJNI.1.0.0.dylib
    lipo -archs $L                                   # arm64
    otool -l $L | grep -A3 LC_BUILD_VERSION | grep minos   # 13.0
    otool -L $L          # Accelerate, libc++, libSystem only
    nm -gU $L | grep -c ' _Java_'                    # 17 JNI entry points
    nm -gU $L | grep -v ' _Java_'                    # nothing

As with libMumpsJNI, only the JNI entry points are exported
(`-Wl,-exported_symbol,'_Java_*'`). This keeps the C++ symbols of
`accelerateJNI.cc` and `hybridSolve.cc` private, and lets `-dead_strip`
remove unused code.

The objects and the library depend on `Makefile.accelerate`, so changing
the compile or link flags rebuilds them.

## Verify

Under the arm64 JDK, from `artisynth_core`:

    J=$(/usr/libexec/java_home -v 21 -a arm64)/bin/java
    CP="classes:$(ls lib/*.jar | tr '\n' ':')"
    $J -cp "$CP" maspack.solvers.AccelerateSolverTest -verbose
    ACCELERATE_NO_LU=1 $J -cp "$CP" maspack.solvers.AccelerateSolverTest

Both should end with `Passed`.

- The first run covers symmetric (SPD and indefinite) solves, inertia,
  singular detection, iterative refinement, hybrid GMRES and CGS solves
  and, on macOS 15.5 or later, the unsymmetric (LU) tests.
- `ACCELERATE_NO_LU` simulates a pre-15.5 system, which checks that
  unsymmetric matrices are rejected.

Under an x86_64 JDK, the test reports `Accelerate solver not available;
test skipped`.

Like the MUMPS library, this has not been tested on an actual macOS
13, 14 or 15 machine (see `MACOS_COMPILE_ENV.md`).
