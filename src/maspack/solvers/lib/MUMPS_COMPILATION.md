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

`~/packages/scotch-7.0.12` (no "v") is an old *install prefix*, not source, and
is no longer referenced by anything. It can be deleted.

## Install prefixes (generated)

    ~/packages/metis-5.1.0-static
    ~/packages/scotch-7.0.12-static

`MUMPS_5.9.1/Makefile.inc` points at these. Build order is therefore
METIS -> SCOTCH -> MUMPS -> bundle.

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

    rm -rf ~/packages/metis-5.1.0/build     ~/packages/metis-5.1.0-static
    rm -rf ~/packages/scotch-v7.0.12/build-static ~/packages/scotch-7.0.12-static

MUMPS' own `make clean` is thorough: it removes `lib/*.a`, `lib/*.so`, all
objects and `.mod` files, `include/mumps_int_def.h` (generated), and cleans
`PORD/`, `libseq/` and `examples/`.

`clean.mumps` removes only the JNI objects and the generated version script,
not the installed `libMumpsJNI.so.*` in `lib/Linux64/`.

--------------------------------------------------------------------------
## Environment gotcha

`~/.cshrc` sets `OMP_NUM_THREADS 1`, which silently defeats the threaded
build. Override it when timing or running.

See `MUMPS_COMPILATION_WINDOWS.md` for the Windows port, and the header
comment of `Makefile.mumps` for the reasoning behind each link-line decision.
