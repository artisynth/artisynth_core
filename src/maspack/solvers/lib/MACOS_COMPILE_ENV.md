# Compile environment for the macOS arm64 native libraries

Sets up the tools needed to build ArtiSynth's native libraries for
Apple Silicon (`lib/MacOSArm64`) so that they run on a chosen minimum
macOS version (the *deployment target*). The library build documents
refer here:

- `MUMPS_COMPILATION_MACOS.md` (libMumpsJNI) needs everything below.
- `ACCELERATE_COMPILATION.md` (libAccelerateJNI) needs only sections 1
  and 2.
- `../../geometry/lib/Makefile` (RobustPreds, TetgenJNI) also needs
  only sections 1 and 2.

Verified 2026-10-10 on an M5 Pro, macOS 26.6, Xcode Command Line Tools
(Apple clang 21, SDK 27.0) and Homebrew at `/opt/homebrew`. The deployment
target was 13.0.

## Choosing the deployment target

The libraries currently target **macOS 13.0**. Every Apple Silicon Mac can
run a current macOS, so an old system is a user's choice (often a managed
lab machine), not a hardware limit. Requiring 26 would exclude Sequoia
(15) users, which was about 10% of one 2026 sample, and probably more on
academic machines.

What constrains the choice:

| Item | First macOS | Handling |
|---|---|---|
| Accelerate `SparseGetInertia` (LDL^T inertia) | 13.0 | hard floor for libAccelerateJNI |
| Accelerate `SparseFactorizationLUTPP` (unsymmetric) | 15.5 | runtime check (`__builtin_available`) |
| Accelerate `SparseOrderMTMetis` | 26.0 | runtime check |
| Homebrew's libgfortran, libquadmath, libgomp, libomp | host OS (26) | rebuilt from source below |

The last row is why GCC and libomp are built from source here. A library
is marked with the deployment target it was compiled for (`minos`), and
the linker gives the result the largest `minos` of anything linked into
it, even statically. Homebrew's bottles are built for the OS they are
installed on. You can see this with:

    otool -l <lib> | grep minos

The target appears in these places, so all of them must agree when it
changes:

- `MACOSX_DEPLOYMENT_TARGET`, when building GCC and libomp below.
- `-DCMAKE_OSX_DEPLOYMENT_TARGET`, for METIS and SCOTCH.
- `-mmacosx-version-min` in MUMPS' `Makefile.inc` (`OPTF`, `OPTC`, `OPTL`).
- `MACOS_MIN_VERSION` in `Makefile.mumps`.
- `CXXFLAGS` in `Makefile.accelerate`.
- `MACOS_ARCH_FLAGS` in `../../geometry/lib/Makefile`.

The commands below use these two variables:

    export MACOS_MIN=13.0     # deployment target
    export SUFFIX=macos13     # names the install prefixes

The Makefiles default to the `macos13` prefixes. For another target, pass
`GCC_HOME` and `LIBOMP_HOME` to `Makefile.mumps`.

## 1. Xcode Command Line Tools

    xcode-select --install

These provide Apple clang (C, C++), `ld`, `lipo`, `otool`, `nm` and the
macOS SDK (`xcrun --show-sdk-path`). The full Xcode app is not needed.
The SDK must be at least as new as the newest API used at run time:
libAccelerateJNI refers to `SparseOrderMTMetis`, so it needs SDK 26 or
later. Apple clang has no OpenMP and no Fortran, which is why MUMPS needs
the GCC and libomp below.

## 2. JDK

Builds use only the JNI headers (`include`, `include/darwin`), which are
the same for every architecture. The Makefiles therefore take any JDK 21:
`JAVA_HOME`, or by default `/usr/libexec/java_home -v 21`.

The arm64 libraries can only be loaded by an **arm64** JVM. For the tests:

    J=$(/usr/libexec/java_home -v 21 -a arm64)/bin/java

The Makefiles regenerate the JNI headers (`maspack_solvers_*.h`) with
`javac -h` when the solver's Java source changes. This needs ArtiSynth's
`classes` directory to be compiled.

## 3. Homebrew packages

    brew install gcc cmake bison flex

- `gcc` is only a build aid: it pulls in gmp, mpfr, libmpc, isl and zstd,
  which the GCC built below uses. Its own compilers and runtimes are not
  used for anything that ships.
- cmake 4 is fine for METIS, given `-DCMAKE_POLICY_VERSION_MINIMUM=3.5`.
- SCOTCH needs bison >= 3 and flex. macOS has bison 2.3, so the keg-only
  Homebrew versions are put first on `PATH` when building it.

Shell gotchas:

- **`AR`:** if the environment defines `AR` (here it was set to a
  directory), builds that use `AR ?= ar` break, with "Permission denied"
  at the archive step. Hence `env -u AR` below.
- **zsh:** zsh does not word-split `$VAR`, so make or cmake options cannot
  be passed in a variable. Spell them out, or use bash.

## Scripted build of sections 4 and 5

`~/packages/macos_toolchain_build` does sections 4 and 5, extracts the
sources from the Homebrew cache if needed, and checks the results:

    brew fetch --build-from-source gcc libomp     # if the sources aren't in ~/packages
    ~/packages/macos_toolchain_build              # or: ... gcc | libomp
    MACOS_MIN=14.0 ~/packages/macos_toolchain_build    # another target

`MACOS_MIN` defaults to 13.0, and `SUFFIX` to `macos<major>`. Run the
script with `-help` for the other settings. `~/packages/macos_toolchain_clean`,
with the same variables, removes the build trees and prefixes. The
sections below give the steps the script follows.

## 4. GCC (C and Fortran) with runtimes for the target

This uses the same source, Homebrew patch and configure options as
Homebrew's `gcc` formula, minus C++ and Objective-C, built with
`MACOSX_DEPLOYMENT_TARGET` set. It is not bootstrapped: the host compiler
is Apple clang, and only the target runtimes (libgfortran, libquadmath,
libgcc) end up in a shipped library. The build takes a few minutes.

To get the sources, run:

    brew fetch --build-from-source gcc

It downloads `gcc-16.2.0.tar.xz` and Homebrew's Darwin patch
`gcc-16.2.0.diff` (Iain Sandoe's Apple Silicon branch) into `$(brew --cache)/downloads`,
with hash prefixes on the names. `brew --cache --build-from-source gcc`
prints the tarball's path. Copy the patch to `~/packages`; here it is
`gcc-16.2.0-homebrew.diff`. For a newer GCC, use whatever version and
patch the formula then names, and check its options with `gcc-NN -v`.

    cd ~/packages
    tar xJf $(brew --cache --build-from-source gcc)
    cd gcc-16.2.0 && patch -p1 < ../gcc-16.2.0-homebrew.diff
    mkdir build-$SUFFIX && cd build-$SUFFIX
    env -u AR MACOSX_DEPLOYMENT_TARGET=$MACOS_MIN ../configure \
      --prefix=$HOME/packages/gcc-16.2.0-$SUFFIX \
      --disable-nls --enable-checking=release --with-gcc-major-version-only \
      --enable-languages=c,fortran --disable-bootstrap --disable-multilib \
      --with-gmp=/opt/homebrew/opt/gmp --with-mpfr=/opt/homebrew/opt/mpfr \
      --with-mpc=/opt/homebrew/opt/libmpc --with-isl=/opt/homebrew/opt/isl \
      --with-zstd=/opt/homebrew/opt/zstd --with-system-zlib \
      --build=aarch64-apple-darwin$(uname -r | cut -d. -f1) \
      --with-sysroot=$(xcrun --show-sdk-path) \
      --with-pkgversion="ArtiSynth $SUFFIX target libs" CC=clang CXX=clang++
    env -u AR MACOSX_DEPLOYMENT_TARGET=$MACOS_MIN make -j 15 \
      BOOT_LDFLAGS=-Wl,-headerpad_max_install_names \
      LDFLAGS_FOR_TARGET=-Wl,-headerpad_max_install_names
    env -u AR make install

Notes:

- The `--build` triple uses the host's Darwin major version (25 for
  macOS 26).
- Expect harmless noise: `clang++: error: unsupported option
  '-print-multi-os-directory'`, from a configure probe.
- To check the result, run
  `otool -l lib/libgfortran.a | grep minos` in the install prefix. It
  should report `$MACOS_MIN` for libgfortran.a, libquadmath.a and libgomp.a.
  libgcc.a reports 11.0, which is fine.
- **The new compiler's default target is still the host OS.** Every
  compile and link must pass `-mmacosx-version-min=$MACOS_MIN` explicitly;
  MUMPS' `Makefile.inc` and `Makefile.mumps` do.

## 5. libomp (LLVM OpenMP runtime), static

MUMPS uses libomp rather than gfortran's libgomp, because libgomp got
*slower* with more threads on Apple Silicon. libomp implements the GOMP
ABI that `gfortran -fopenmp` emits, so it can replace libgomp at link time.

Use the same version and options as Homebrew's `libomp` formula:

    brew fetch --build-from-source libomp

That downloads `llvm-project-23.1.3.src.tar.xz`. Only a few subtrees are
needed (34 MB):

    cd ~/packages
    tar xJf $(brew --cache --build-from-source libomp) \
       llvm-project-23.1.3.src/{runtimes,openmp,cmake,llvm/cmake,llvm/utils,third-party}
    cd llvm-project-23.1.3.src
    env -u AR MACOSX_DEPLOYMENT_TARGET=$MACOS_MIN cmake -S runtimes \
      -B build-static-$SUFFIX -DLLVM_ENABLE_RUNTIMES=openmp \
      -DLIBOMP_ENABLE_SHARED=OFF -DLIBOMP_INSTALL_ALIASES=OFF \
      -DOPENMP_ENABLE_OMPT_TOOLS=OFF -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++ \
      -DCMAKE_OSX_ARCHITECTURES=arm64 -DCMAKE_OSX_DEPLOYMENT_TARGET=$MACOS_MIN \
      -DCMAKE_INSTALL_PREFIX=$HOME/packages/libomp-23.1.3-$SUFFIX
    cmake --build build-static-$SUFFIX -j 12
    cmake --install build-static-$SUFFIX

The result is `lib/libomp.a`, with `minos` equal to the target. It should
have 134 `GOMP_` symbols and export `kmp_set_blocktime`, which can be
checked with `nm`. libomp is written in C++, so links that use it need
`-lc++`.

## Not yet verified

All builds so far have run on macOS 26 only. The `minos` field, and the
absence of linker "built for newer macOS version" warnings, show that the
libraries *declare* the target. Only loading them on an actual
macOS 13/14/15 machine proves it, since a symbol missing from an older
libSystem would show up only at load time.

## Clean

`~/packages/macos_toolchain_clean` does this. It leaves the source trees,
so a rebuild doesn't need the Homebrew cache:

    rm -rf ~/packages/gcc-16.2.0/build-$SUFFIX ~/packages/gcc-16.2.0-$SUFFIX
    rm -rf ~/packages/llvm-project-23.1.3.src/build-static-$SUFFIX \
           ~/packages/libomp-23.1.3-$SUFFIX
