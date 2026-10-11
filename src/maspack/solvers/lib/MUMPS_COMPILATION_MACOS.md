# Building MUMPS and the MUMPS JNI library on macOS (Apple Silicon)

This is the macOS arm64 counterpart of `MUMPS_COMPILATION.md` (Linux).
It produces `lib/MacOSArm64/libMumpsJNI.5.9.1.dylib`, which is loaded by
an arm64 JVM (`NativeLibraryManager` system type `MacOSArm64`).

Verified 2026-10-10 on an M5 Pro, macOS 26, Xcode Command Line Tools
(Apple clang 21), Homebrew at `/opt/homebrew` and Oracle JDK 21 arm64.
The minimum macOS version is 13.0.

## How it differs from Linux

| Item | Linux | macOS arm64 |
|---|---|---|
| BLAS/LAPACK | oneMKL, static | Apple **Accelerate** (system framework). No MKL exists for arm64, so there is also no Pardiso. |
| `-DGEMMT_AVAILABLE` | yes | no: Accelerate has no `?gemmt` |
| Compilers | gcc/gfortran | Apple clang for METIS, SCOTCH and the JNI C++; GCC 16.2 **built from source** for MUMPS |
| OpenMP runtime | libiomp5, shared with Pardiso | LLVM **libomp**, static, built from source |
| libgfortran, libquadmath | shared, shipped | **static** |
| Shipped files | JNI library + libiomp5, libgfortran, libquadmath | the JNI library only |
| Minimum OS | | **macOS 13.0** |
| Symbol hiding | version script + `--exclude-libs` | `-exported_symbols_list` (`_Java_*`) |
| JNI sources | | `-DNO_MKL`: hybridSolve.cc uses a plain CRS mat-vec instead of MKL sparse BLAS, and mumps.cc makes no MKL thread calls. `-DLIBOMP`: mumps.cc sets the blocktime to 1 ms. |
| Thread count | physical cores | performance cores only (`-DDARWIN` in hybridSolve.cc) |

The runtimes are built from source because Homebrew's are built for the
host OS (macOS 26), and linking them, even statically, would make that the
minimum. `MACOS_COMPILE_ENV.md` explains this and gives the GCC and
libomp builds.

libomp is used instead of gfortran's own libgomp because, on Apple
Silicon, MUMPS with libgomp got *slower* with more threads. With libomp,
the blocktime setting in `mumps.cc` and the performance-core count matter
for performance. "Threads" in `../MUMPS_SOLVER.md` gives the details and
measurements.

## Prerequisites

Set up the compile environment as described in `MACOS_COMPILE_ENV.md`,
sections 1-5: Command Line Tools, a JDK, the Homebrew packages, GCC and
libomp. Watch for the `AR` gotcha described there.

## Source trees (in ~/packages)

    MUMPS_5.9.1  metis-5.1.0  scotch-v7.0.12      as for Linux

## Install prefixes (generated)

    ~/packages/metis-5.1.0-static
    ~/packages/scotch-7.0.12-static
    ~/packages/gcc-16.2.0-macos13         MACOS_COMPILE_ENV.md
    ~/packages/libomp-23.1.3-macos13      MACOS_COMPILE_ENV.md

Build in this order: GCC and libomp, then METIS, SCOTCH, MUMPS and the
JNI library. The steps below assume a deployment target of 13.0. For
another target, see `MACOS_COMPILE_ENV.md`.

Scripts in `~/packages` do most of this:

| Script | Does |
|---|---|
| `macos_toolchain_build` | GCC and libomp (`MACOS_COMPILE_ENV.md`) |
| `mumps_build_all` | steps 1-3, and checks that every static library is arm64 and built for the minimum macOS version |
| `mumps_clean_all` | removes the METIS and SCOTCH builds, and runs MUMPS' `make clean` |

`mumps_build_all` is shared with Linux. On macOS it takes `MACOS_MIN`
(default 13.0) and `GCC_HOME` (passed to MUMPS' make as `GCCDIR`). It
also checks that `Makefile.inc` is the macOS one, with a matching
`-mmacosx-version-min`. The steps it follows are:

    export MACOSX_DEPLOYMENT_TARGET=13.0

### 1. METIS 5.1.0

cmake 4 rejects METIS' `cmake_minimum_required(2.8)`, hence the policy flag.

    cd ~/packages/metis-5.1.0
    cmake -S . -B build -DGKLIB_PATH=$PWD/GKlib -DSHARED=0 \
      -DCMAKE_POLICY_VERSION_MINIMUM=3.5 -DCMAKE_C_COMPILER=clang \
      -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
      -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DCMAKE_C_FLAGS="-fPIC -O2" \
      -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_INSTALL_PREFIX=$HOME/packages/metis-5.1.0-static
    cmake --build build -j 12 && cmake --build build --target install

Some upstream warnings are benign:

- GKlib `sscanf` fortify.
- `iabs` applied to a float in `balance.c`.
- `ld: search path '/home/karypis/local/lib' not found`.

### 2. SCOTCH 7.0.12

This needs bison >= 3 (macOS has 2.3), so Homebrew's goes first on PATH.

    cd ~/packages/scotch-v7.0.12
    export PATH=/opt/homebrew/opt/bison/bin:/opt/homebrew/opt/flex/bin:$PATH
    cmake -S . -B build-static -DBUILD_SHARED_LIBS=OFF -DCMAKE_C_COMPILER=clang \
      -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
      -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DCMAKE_C_FLAGS="-fPIC -O2" \
      -DCMAKE_BUILD_TYPE=Release -DINTSIZE=32 -DTHREADS=ON \
      -DBUILD_PTSCOTCH=OFF -DBUILD_LIBESMUMPS=ON -DBUILD_FORTRAN=OFF \
      -DBUILD_LIBSCOTCHMETIS=OFF -DINSTALL_METIS_HEADERS=OFF \
      -DUSE_ZLIB=OFF -DUSE_LZMA=OFF -DUSE_BZ2=OFF -DENABLE_TESTS=OFF \
      -DCMAKE_INSTALL_PREFIX=$HOME/packages/scotch-7.0.12-static
    cmake --build build-static -j 12 && cmake --build build-static --target install

`-DBUILD_FORTRAN=OFF` is used because MUMPS calls SCOTCH's C API through
esmumps.

### 3. MUMPS 5.9.1

The macOS `Makefile.inc` is kept as `MUMPS_5.9.1/Makefile.inc.macos-arm64`
and the Linux one as `Makefile.inc.linux-x86_64`. Copy the macOS one to
`Makefile.inc`. It differs from the Linux one in:

    GCCDIR  = $(HOME)/packages/gcc-16.2.0-macos13
    CC      = $(GCCDIR)/bin/gcc
    FC      = $(GCCDIR)/bin/gfortran
    FL      = $(GCCDIR)/bin/gfortran
    AR      = ar vr # the trailing space is required: MUMPS uses $(AR)$@
    LIBEXT_SHARED = .dylib
    SONAME        = -install_name
    SHARED_OPT    = -dynamiclib -undefined dynamic_lookup
    LAPACK    = -framework Accelerate
    LIBBLAS   = -framework Accelerate
    LIBOTHERS = -lpthread -lm
    OPTF = -O2 -fopenmp -fPIC -fallow-argument-mismatch -mmacosx-version-min=13.0
    OPTC = -O2 -fopenmp -fPIC -I. -mmacosx-version-min=13.0
    OPTL = -O2 -fopenmp -fPIC -mmacosx-version-min=13.0

A few other choices are deliberate:

- There is no `-DGEMMT_AVAILABLE`.
- `-fallow-argument-mismatch` is required by gfortran >= 10.
- There is no `-mcpu=native`, so the code runs on any M-series Mac.

METISDIR, SCOTCHDIR and the orderings are as for Linux.

    cd ~/packages/MUMPS_5.9.1
    env -u AR make -j 12 all

About 1500 warnings are expected:

- Fortran argument mismatches, downgraded from errors by
  `-fallow-argument-mismatch`.
- GCC 16 deprecations of OpenMP `master` and `omp_set_nested`.

There should be no "built for newer macOS version" linker warnings. If
there are, a runtime with a newer `minos` was picked up.

### 4. The JNI library

    cd artisynth_core/src/maspack/solvers/lib
    make -f Makefile.mumps
    make -f Makefile.mumps check

`check` should print:

- architecture `arm64`,
- minimum macOS `13.0`,
- dependencies on `Accelerate`, `libc++` and `libSystem` only,
- only `_Java_maspack_solvers_MumpsSolver_*` exported (49 entry points).

The static archives are linked by full path, since with `-l` ld64 prefers
a `.dylib` of the same name. `GCC_HOME`, `LIBOMP_HOME`, `MUMPS_HOME`,
`METIS_HOME`, `SCOTCH_HOME` and `JAVA_HOME` can be overridden on the make
command line.

## Verify

Under the arm64 JDK, from `artisynth_core`:

    J=$(/usr/libexec/java_home -v 21 -a arm64)/bin/java
    $J -cp "classes:$(ls lib/*.jar | tr '\n' ':')" maspack.solvers.MumpsSolverTest -verbose

This should load `lib/MacOSArm64/libMumpsJNI.5.9.1.dylib` and end with
`Passed`. Under the x86_64 JDK, the same command reports
`MUMPS not available; test skipped`, since there is no x86_64 build.

This has not yet been tested on an actual macOS 13, 14 or 15 machine (see
`MACOS_COMPILE_ENV.md`).

## Alternatives that were evaluated

- **OpenBLAS 0.3.34** (static, OpenMP) instead of Accelerate:
  - 3x slower at 1 thread, equal at 8, and 25% faster at 15.
  - Its `?gemmt` is a loop of gemv calls, so `-DGEMMT_AVAILABLE` made it
    3-9x *slower*.
- **Accelerate's own sparse solver** (`Sparse/Solve.h`): this is now the
  separate `AccelerateSolver`; see `../ACCELERATE_SOLVER.md` and
  `ACCELERATE_COMPILATION.md`.
