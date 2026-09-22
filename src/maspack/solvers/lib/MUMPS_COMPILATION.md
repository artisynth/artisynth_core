# Building MUMPS and its ordering packages on Linux

Prerequisites for `Makefile.mumps`, which links the resulting static libraries
into `libMumpsJNI.so.<version>`.  This file covers only the upstream packages;
the JNI library itself is built with `make -f Makefile.mumps`.

Verified end to end 2026-09-08 on Ubuntu 20.04 / gcc 9.4 / oneMKL 2025.3,
starting from three pristine source trees.

## Source trees (inputs)

    ~/packages/MUMPS_5.9.1        MUMPS 5.9.1 source
    ~/packages/metis-5.1.0        METIS 5.1.0 source
    ~/packages/scotch-v7.0.12     SCOTCH 7.0.12 source   <-- note the "v"

## Install prefixes (generated)

    ~/packages/metis-5.1.0-static
    ~/packages/scotch-7.0.12-static

`MUMPS_5.9.1/Makefile.inc` points at these. Build order is therefore
METIS -> SCOTCH -> MUMPS -> bundle.

## How MUMPS is configured

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
## (a) Build

### 1. METIS 5.1.0 -> static PIC

    cd ~/packages/metis-5.1.0
    cmake -H. -Bbuild -DGKLIB_PATH=$PWD/GKlib -DSHARED=0 \
          -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DCMAKE_C_FLAGS="-fPIC -O2" \
          -DCMAKE_BUILD_TYPE=Release \
          -DCMAKE_INSTALL_PREFIX=$HOME/packages/metis-5.1.0-static
    cmake --build build -j8
    cmake --build build --target install

### 2. SCOTCH 7.0.12 -> static PIC

    cd ~/packages/scotch-v7.0.12
    cmake -S . -B build-static -DBUILD_SHARED_LIBS=OFF \
          -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DCMAKE_C_FLAGS="-fPIC -O2" \
          -DCMAKE_BUILD_TYPE=Release -DINTSIZE=32 -DTHREADS=ON \
          -DBUILD_PTSCOTCH=OFF -DBUILD_LIBESMUMPS=ON \
          -DBUILD_LIBSCOTCHMETIS=OFF -DINSTALL_METIS_HEADERS=OFF \
          -DUSE_ZLIB=OFF -DUSE_LZMA=OFF -DUSE_BZ2=OFF -DENABLE_TESTS=OFF \
          -DCMAKE_INSTALL_PREFIX=$HOME/packages/scotch-7.0.12-static
    cmake --build build-static -j8
    cmake --build build-static --target install

### 3. MUMPS

    cd ~/packages/MUMPS_5.9.1
    make -j8 all          # static  .a  -- this is what the bundle needs
    make -j8 allshared    # shared  .so -- optional, for non-bundle use

### 4. The JNI library

    cd artisynth_core/src/maspack/solvers/lib
    make -f Makefile.mumps

Ship to `artisynth_core/lib/Linux64/`: `libMumpsJNI.so.5.9.1` plus the three
runtime libraries it loads from `$ORIGIN` — `libiomp5.so`, `libgfortran.so.5`
and `libquadmath.so.0`.  `make -f Makefile.mumps check` prints where to copy
each of them from.

--------------------------------------------------------------------------
## (b) Verify

    cd artisynth_core/src/maspack/solvers/lib
    make -f Makefile.mumps check

That prints the library's NEEDED/RUNPATH entries and its exported symbols.
Expect only `libiomp5.so`, `libgfortran.so.5`, `libquadmath.so.0` and libc
among the dependencies, `RUNPATH [$ORIGIN]`, and nothing exported but
`Java_maspack_solvers_MumpsSolver_*`.

Baseline-ISA check (MUMPS/METIS/SCOTCH code must be plain x86-64; MKL's own
AVX-512 kernels are runtime-dispatched and expected):

    objdump -d ../lib/libdmumps.a | grep -cE '\bvfmadd|\b(shlx|andn|bzhi)\b'
    # expect 0

--------------------------------------------------------------------------
## (c) Clean

    cd artisynth_core/src/maspack/solvers/lib && make -f Makefile.mumps clean.mumps

    cd ~/packages/MUMPS_5.9.1 && make clean

    cd ~/packages
    rm -rf metis-5.1.0/build metis-5.1.0-static
    rm -rf scotch-v7.0.12/build scotch-v7.0.12/build-static scotch-7.0.12-static

MUMPS' own `make clean` is thorough: it removes `lib/*.a`, `lib/*.so`, all
objects and `.mod` files, `include/mumps_int_def.h` (generated), and cleans
`PORD/`, `libseq/` and `examples/`.

`clean.mumps` removes only the JNI objects and the generated version script,
not the installed `libMumpsJNI.so.*` in `lib/Linux64/`.

--------------------------------------------------------------------------
## Environment gotcha

For binary repeatability reasons, the environment variable
OMP_NUM_THREADS is set to 1 on some systems, which silently defeats
the threaded build. Override it when timing or running.
