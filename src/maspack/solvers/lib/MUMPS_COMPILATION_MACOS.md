# Building MUMPS and the MUMPS JNI library on macOS (Apple Silicon)

macOS arm64 counterpart of `MUMPS_COMPILATION.md` (Linux). Produces
`lib/MacOSArm64/libMumpsJNI.5.9.1.dylib`, loaded by an arm64 JVM
(`NativeLibraryManager` system type `MacOSArm64`).

Verified 2026-10-09 on an M5 Pro, macOS 26, Xcode Command Line Tools
(Apple clang 21), Homebrew at `/opt/homebrew`, Oracle JDK 21 arm64.

## How it differs from Linux

| Item | Linux | macOS arm64 |
|---|---|---|
| BLAS/LAPACK | oneMKL, static | Apple **Accelerate** (system framework). No MKL exists for arm64, so there is also no Pardiso. |
| `-DGEMMT_AVAILABLE` | yes | no: Accelerate has no `?gemmt` |
| Compilers | gcc/gfortran | Apple clang (METIS, SCOTCH, JNI C++); GCC 16.2 **built from source** (MUMPS) |
| OpenMP runtime | libiomp5, shared with Pardiso | LLVM **libomp**, static, built from source |
| libgfortran, libquadmath | shared, shipped | **static** |
| Shipped files | JNI library + libiomp5, libgfortran, libquadmath | the JNI library only |
| Minimum OS | | **macOS 13.0** |
| Symbol hiding | version script + `--exclude-libs` | `-exported_symbols_list` (`_Java_*`) |
| JNI sources | | `-DNO_MKL` (hybridSolve.cc: plain CRS mat-vec instead of MKL sparse BLAS; mumps.cc: no MKL thread calls), `-DLIBOMP` (mumps.cc: blocktime 1 ms) |

Why the runtimes are built from source: Homebrew's libgfortran, libquadmath,
libgomp and libomp are all built for macOS 26 (`otool -l lib | grep minos`).
Linking any of them, even statically, would make macOS 26 the minimum. They
are rebuilt from the same sources and configuration Homebrew uses, but with
`MACOSX_DEPLOYMENT_TARGET=13.0`. macOS 13 is also the minimum for
Accelerate's sparse `SparseGetInertia`, if that solver is ever used.

Why libomp and not gfortran's own libgomp: on Apple Silicon, MUMPS with
libgomp got *slower* with more threads. libomp is faster, but by default
it sets `KMP_BLOCKTIME=0` on hybrid (P+E core) CPUs
(`openmp/runtime/src/kmp.h`), so idle threads sleep after each of MUMPS'
many short parallel regions. That caused ~500k context switches per
factorization. `mumps.cc` therefore calls `kmp_set_blocktime(1)` before
each MUMPS call (unless `KMP_BLOCKTIME` is set in the environment). 216k
Laplacian refactorization at 8 threads: 1.01 s, vs 2.10 s without; 1 thread
1.68 s. Thread scaling saturates at ~4-8 threads, since Accelerate's
DGEMM runs on the shared AMX/SME matrix units.

## Prerequisites

    brew install gcc cmake bison flex     # gcc only as a build aid; gmp/mpfr/mpc/isl come with it

**Gotcha:** if the shell defines `AR` (here it was set to a directory), it
breaks builds that use `AR ?= ar`. The commands below use `env -u AR`.

## Source trees (in ~/packages)

    MUMPS_5.9.1  metis-5.1.0  scotch-v7.0.12      as for Linux
    gcc-16.2.0.tar.xz                           brew fetch --build-from-source gcc
    gcc-16.2.0.diff                             Homebrew's Darwin patch (same fetch)
    llvm-project-23.1.3.src.tar.xz              brew fetch --build-from-source libomp

## Install prefixes (generated)

    ~/packages/metis-5.1.0-static
    ~/packages/scotch-7.0.12-static
    ~/packages/gcc-16.2.0-macos13
    ~/packages/libomp-23.1.3-macos13

Build order: METIS, SCOTCH, GCC, libomp, MUMPS, JNI library.

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

### 2. SCOTCH 7.0.12

Needs bison >= 3 (macOS has 2.3), so Homebrew's comes first on PATH.

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

### 3. GCC 16.2 (C and Fortran), runtimes for macOS 13

Homebrew's configure options (`gcc-16 -v`) and patch, minus C++/ObjC, no
bootstrap (host compiler Apple clang; only the runtimes ship). Takes a
few minutes.

    cd ~/packages && tar xJf gcc-16.2.0.tar.xz
    cd gcc-16.2.0 && patch -p1 < ../gcc-16.2.0.diff
    mkdir build-macos13 && cd build-macos13
    env -u AR MACOSX_DEPLOYMENT_TARGET=13.0 ../configure \
      --prefix=$HOME/packages/gcc-16.2.0-macos13 \
      --disable-nls --enable-checking=release --with-gcc-major-version-only \
      --enable-languages=c,fortran --disable-bootstrap --disable-multilib \
      --with-gmp=/opt/homebrew/opt/gmp --with-mpfr=/opt/homebrew/opt/mpfr \
      --with-mpc=/opt/homebrew/opt/libmpc --with-isl=/opt/homebrew/opt/isl \
      --with-zstd=/opt/homebrew/opt/zstd --with-system-zlib \
      --build=aarch64-apple-darwin25 --with-sysroot=$(xcrun --show-sdk-path) \
      CC=clang CXX=clang++
    env -u AR MACOSX_DEPLOYMENT_TARGET=13.0 make -j 15 \
      BOOT_LDFLAGS=-Wl,-headerpad_max_install_names \
      LDFLAGS_FOR_TARGET=-Wl,-headerpad_max_install_names
    env -u AR make install

Check: `otool -l lib/libgfortran.a | grep minos` gives 13.0. The compiler's
own default target remains the host OS, so `-mmacosx-version-min=13.0`
must still be passed everywhere (Makefile.inc does).

### 4. libomp 23.1.3 (static)

Only a few subtrees of the LLVM source are needed:

    cd ~/packages
    tar xJf llvm-project-23.1.3.src.tar.xz \
       llvm-project-23.1.3.src/{runtimes,openmp,cmake,llvm/cmake,llvm/utils,third-party}
    cd llvm-project-23.1.3.src
    env -u AR cmake -S runtimes -B build-static-macos13 \
      -DLLVM_ENABLE_RUNTIMES=openmp -DLIBOMP_ENABLE_SHARED=OFF \
      -DLIBOMP_INSTALL_ALIASES=OFF -DOPENMP_ENABLE_OMPT_TOOLS=OFF \
      -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=clang \
      -DCMAKE_CXX_COMPILER=clang++ -DCMAKE_OSX_ARCHITECTURES=arm64 \
      -DCMAKE_OSX_DEPLOYMENT_TARGET=13.0 \
      -DCMAKE_INSTALL_PREFIX=$HOME/packages/libomp-23.1.3-macos13
    cmake --build build-static-macos13 -j 12
    cmake --install build-static-macos13

### 5. MUMPS 5.9.1

`MUMPS_5.9.1/Makefile.inc` for macOS differs from the Linux one in:

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

(no `-DGEMMT_AVAILABLE`; `-fallow-argument-mismatch` is required by
gfortran >= 10; no `-mcpu=native`, so the code runs on any M-series Mac).
METISDIR/SCOTCHDIR and the orderings are as for Linux.

    cd ~/packages/MUMPS_5.9.1
    env -u AR make -j 12 all

~1500 warnings are expected (Fortran argument mismatches downgraded by
`-fallow-argument-mismatch`; GCC 16 deprecations of OpenMP `master` and
`omp_set_nested`).

### 6. The JNI library

    cd artisynth_core/src/maspack/solvers/lib
    make -f Makefile.mumps
    make -f Makefile.mumps check

`check` should print architecture `arm64`, minimum macOS `13.0`,
dependencies only `Accelerate`, `libc++` and `libSystem`, and only
`_Java_maspack_solvers_MumpsSolver_*` exported. Only the JNI headers are
used from `JAVA_HOME`, so any JDK will do for building.

## Verify

Under the arm64 JDK:

    J=$(/usr/libexec/java_home -v 21 -a arm64)/bin/java
    $J -cp "classes:$(ls lib/*.jar | tr '\n' ':')" maspack.solvers.MumpsSolverTest -verbose

Expected: it loads `lib/MacOSArm64/libMumpsJNI.5.9.1.dylib` and ends with
`Passed`. Under the x86_64 JDK the same command reports
`MUMPS not available; test skipped`, as there is no x86_64 build.

Not yet verified: running on an actual macOS 13 (or 14/15) machine. The
minos field and the absence of linker version warnings show the library
*declares* 13.0, but only a load on an older system proves it.

## Alternatives that were evaluated

- **OpenBLAS 0.3.34** (static, OpenMP) instead of Accelerate: 3x slower
  at 1 thread, equal at 8, 25% faster at 15. Its `?gemmt` is a loop of
  gemv calls, so `-DGEMMT_AVAILABLE` made it 3-9x *slower*.
- **Accelerate's own sparse solver** (`Sparse/Solve.h`, LDL^T with
  threshold partial pivoting): competitive (1.5x faster at 30-80k
  unknowns, 1.2x slower at 260k), no runtimes to build, but no thread
  control and far fewer controls than MUMPS. Its SBK pivoting returns
  wrong answers with status OK on KKT systems, and its default AMD
  ordering is 15x slower than Metis on them.
