# Makefile for building the MUMPS JNI library, libMumpsJNI.so.<version>
# (Linux) or libMumpsJNI.<version>.dylib (macOS arm64), which implements the
# native side of maspack.solvers.MumpsSolver.
#
# Linux: the result is a stand-alone shared library: everything is statically
# linked EXCEPT libiomp5, libgfortran, libquadmath and libc:
#
#   MKL           static, and hidden (--exclude-libs,ALL plus a version
#                 script which exports only the JNI entry points)
#   MUMPS, PORD   static, together with the libmpiseq MPI stub, since this
#                 is an MPI-free (but OpenMP threaded) MUMPS build
#   METIS, SCOTCH static, built PIC specifically for this library
#   libiomp5      shared, found via $ORIGIN, and deliberately shared with
#                 libPardisoJNI: MUMPS is compiled by gfortran -fopenmp, but
#                 we link WITHOUT -fopenmp and with -liomp5 instead, since
#                 libiomp5 implements the full GOMP ABI. That yields ONE
#                 OpenMP runtime in the process instead of libgomp + libiomp5.
#   libgfortran   shared, found via $ORIGIN. Ubuntu's libgfortran.a is not
#                 built -fPIC and so cannot be linked into a .so.
#
# libiomp5.so, libgfortran.so.5 and libquadmath.so.0 must therefore be
# shipped in lib/$(NATIVE_DIR) alongside the JNI library. libquadmath is
# needed explicitly (see the -Wl,--no-as-needed below) because it is only an
# indirect dependency, of libgfortran, and DT_RUNPATH - unlike the legacy
# DT_RPATH - is not searched when resolving a dependency's own dependencies.
#
# macOS arm64 (Apple Silicon): there is no MKL (and hence no Pardiso), so
# BLAS/LAPACK come from Apple's Accelerate framework, and the library is
# fully self-contained: it depends only on libSystem, libc++ and Accelerate,
# so nothing else needs to be shipped in lib/MacOSArm64:
#
#   MUMPS, PORD,  static, as on Linux
#   METIS, SCOTCH
#   libgfortran,  static, from a GCC built from source with
#   libquadmath   MACOSX_DEPLOYMENT_TARGET=13.0 (Homebrew's are built for
#                 macOS 26, which would make that the minimum version)
#   libomp        static, LLVM's OpenMP runtime, also built for macOS 13.
#                 It implements the GOMP ABI used by gfortran -fopenmp, and
#                 was measured to be much faster than libgomp here.
#                 mumps.cc (-DLIBOMP) sets its blocktime to 1 ms.
#   Accelerate    system framework (BLAS/LAPACK). hybridSolve.cc (-DNO_MKL)
#                 replaces MKL's sparse matrix-vector product with a loop.
#
# Only the JNI entry points are exported (-exported_symbols_list), so the
# statically linked runtimes cannot collide with other libraries loaded into
# the JVM. The minimum macOS version is 13.0. See MUMPS_COMPILATION_MACOS.md.
#
# Usage:
#
#   make -f Makefile.mumps            build the library
#   make -f Makefile.mumps check      build it and check its dependencies
#   make -f Makefile.mumps clean.mumps
#
# The locations of MUMPS, MKL, SCOTCH, METIS (and on macOS, GCC and libomp)
# may be overridden on the command line or through the environment.

ROOT_DIR = ../../../..

SYSTEM = $(shell uname)
MACHINE = $(shell uname -m)

# MUMPS version, which becomes the version number of the JNI library. Make
# sure there is no whitespace at the end.
MUMPS_VERSION = 5.9.1

ifndef MUMPS_HOME
   MUMPS_HOME = $(HOME)/packages/MUMPS_$(MUMPS_VERSION)
endif
ifndef SCOTCH_HOME
   SCOTCH_HOME = $(HOME)/packages/scotch-7.0.12-static
endif
ifndef METIS_HOME
   METIS_HOME = $(HOME)/packages/metis-5.1.0-static
endif

MUMPS_OBJS = MumpsJNI.o mumps.o hybridSolve.o

# static MUMPS, together with its ordering packages and the sequential MPI
# stub
LDS_MUMPS = \
	$(MUMPS_HOME)/lib/libdmumps.a $(MUMPS_HOME)/lib/libmumps_common.a \
	$(MUMPS_HOME)/lib/libpord.a   $(MUMPS_HOME)/lib/libmpiseq.a \
	$(SCOTCH_HOME)/lib/libesmumps.a $(SCOTCH_HOME)/lib/libscotch.a \
	$(SCOTCH_HOME)/lib/libscotcherr.a \
	$(METIS_HOME)/lib/libmetis.a

ifeq ($(SYSTEM),Darwin)

ifneq ($(MACHINE),arm64)
   $(error Makefile.mumps supports macOS on arm64 only (uname -m = $(MACHINE)))
endif

CC_COMP = clang++

ifndef GCC_HOME
   GCC_HOME = $(HOME)/packages/gcc-16.2.0-macos13
endif
ifndef LIBOMP_HOME
   LIBOMP_HOME = $(HOME)/packages/libomp-23.1.3-macos13
endif
# only the (architecture independent) JNI headers are used, so any JDK will do
ifndef JAVA_HOME
   JAVA_HOME = $(shell /usr/libexec/java_home -v 21)
endif

MACOS_MIN_VERSION = 13.0

NATIVE_DIR = MacOSArm64
MUMPS_TARGET = libMumpsJNI.$(MUMPS_VERSION).dylib

CC_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin \
	  -I$(MUMPS_HOME)/include -I.

CC_FLAGS = -arch arm64 -mmacosx-version-min=$(MACOS_MIN_VERSION) \
	   -O2 -fno-strict-aliasing -fPIC -DDARWIN -DNO_MKL -DLIBOMP

# Static archives are given by full path, since with -l ld64 prefers a
# .dylib of the same name. libgcc.a supplies the helper routines (e.g.
# outline atomics) used by the gfortran-compiled code.
LDS_MUMPS_JNI = $(LDS_MUMPS) \
	$(GCC_HOME)/lib/libgfortran.a $(GCC_HOME)/lib/libquadmath.a \
	$(shell $(GCC_HOME)/bin/gcc -print-libgcc-file-name) \
	$(LIBOMP_HOME)/lib/libomp.a \
	-framework Accelerate

LIB_FLAGS = -dynamiclib -install_name @rpath/$(MUMPS_TARGET) \
	-Wl,-exported_symbols_list,mumps_exports.txt -Wl,-dead_strip

EXPORTS_FILE = mumps_exports.txt

else # Linux

CC_COMP = g++

ifndef MKL_HOME
   MKL_HOME = /opt/intel/oneapi2026/mkl/latest
endif
ifndef MKL_THREAD_LIB
   MKL_THREAD_LIB = /opt/intel/oneapi2026/compiler/latest/lib
endif
ifndef JAVA_HOME
   JAVA_HOME = /usr/lib/jvm/default-java
endif

ifeq ($(findstring 64,$(MACHINE)),64)
   NATIVE_DIR = Linux64
else
   NATIVE_DIR = Linux
endif

MUMPS_TARGET = libMumpsJNI.so.$(MUMPS_VERSION)

CC_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
	  -I$(MUMPS_HOME)/include -I$(MKL_HOME)/include -I.

CC_FLAGS = -m64 -O2 -fno-strict-aliasing -fPIC -pthread -DLINUX

# static MKL, for the BLAS and LAPACK used by the factorization
LDS_MKL = -Wl,--start-group \
	$(MKL_HOME)/lib/libmkl_gf_lp64.a $(MKL_HOME)/lib/libmkl_intel_thread.a \
	$(MKL_HOME)/lib/libmkl_core.a -Wl,--end-group

LDS_MUMPS_JNI = $(LDS_MUMPS) $(LDS_MKL) \
	-L$(MKL_THREAD_LIB) -liomp5 \
	-lgfortran -Wl,--no-as-needed -lquadmath -Wl,--as-needed \
	-static-libgcc -lpthread -lm -ldl

# hide everything except the JNI entry points, so that the statically linked
# MKL and MUMPS symbols cannot collide with those of other libraries (in
# particular libPardisoJNI) loaded into the same process
LIB_FLAGS = -shared -Wl,-rpath='$$ORIGIN' \
	-Wl,--version-script=mumps_version.map -Wl,--exclude-libs,ALL \
	-static-libstdc++ -static-libgcc

EXPORTS_FILE = mumps_version.map

endif

LIB_TARGET_DIR = $(ROOT_DIR)/lib/$(NATIVE_DIR)

default: mumps

mumps_version.map:
	echo '{ global: Java_*; local: *; };' > mumps_version.map

mumps_exports.txt:
	echo '_Java_*' > mumps_exports.txt

# Regenerated whenever MumpsSolver.java's native method declarations
# change -- see the equivalent rule in ../Makefile (Pardiso) for why this
# matters: without it, a new native method silently links under a mangled
# symbol name instead of failing the build.
maspack_solvers_MumpsSolver.h: ../MumpsSolver.java
	javac -h . -d $(ROOT_DIR)/classes -cp "$(ROOT_DIR)/classes" \
	   ../MumpsSolver.java

# hybrid (preconditioned iterative) solves, shared with the Pardiso JNI library
hybridSolve.o: hybridSolve.cc hybridSolve.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o hybridSolve.o hybridSolve.cc

mumps.o: mumps.cc mumps.h hybridSolve.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o mumps.o mumps.cc

MumpsJNI.o: MumpsJNI.cc mumps.h hybridSolve.h hybridSolveJNI.h maspack_solvers_MumpsSolver.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o MumpsJNI.o MumpsJNI.cc

$(LIB_TARGET_DIR)/$(MUMPS_TARGET): $(MUMPS_OBJS) $(EXPORTS_FILE)
	mkdir -p $(LIB_TARGET_DIR)
	$(CC_COMP) $(LIB_FLAGS) $(CC_FLAGS) -o $@ \
	$(MUMPS_OBJS) $(LDS_MUMPS_JNI)

.PHONY: mumps check clean.mumps

mumps: $(LIB_TARGET_DIR)/$(MUMPS_TARGET)

# Check that the library depends only on the libraries we expect, and that it
# exports only the JNI entry points.
ifeq ($(SYSTEM),Darwin)
check: $(LIB_TARGET_DIR)/$(MUMPS_TARGET)
	@echo "architecture: `lipo -archs $<`"
	@echo "minimum macOS version:" \
	   `otool -l $< | awk '/LC_BUILD_VERSION/{f=1} f&&/minos/{print $$2; f=0}'`
	@echo "dependencies (expect only libc++, Accelerate, libSystem):"
	@otool -L $< | tail -n +2
	@echo "exported symbols (expect only _Java_maspack_solvers_MumpsSolver_*):"
	@nm -gU $< | awk '{print "  "$$3}' | grep -v '^  _Java_' || true
	@nm -gU $< | grep -c ' _Java_' | sed 's/^/  JNI entry points: /'
else
check: $(LIB_TARGET_DIR)/$(MUMPS_TARGET)
	@echo "dependencies:"
	@readelf -d $< | grep -E 'NEEDED|RUNPATH'
	@echo "exported symbols:"
	@readelf --dyn-syms -W $< | \
	   awk '$$7!="UND" && ($$5=="GLOBAL"||$$5=="WEAK"){print "  "$$8}' | \
	   grep -v '^  $$' | head -20
	@echo "libraries which must be shipped in $(LIB_TARGET_DIR):"
	@echo "  libiomp5.so       $(MKL_THREAD_LIB)/libiomp5.so"
	@echo "  libgfortran.so.5  `gcc -print-file-name=libgfortran.so.5`"
	@echo "  libquadmath.so.0  `gcc -print-file-name=libquadmath.so.0`"
endif

clean.mumps:
	rm -f $(MUMPS_OBJS) mumps_version.map mumps_exports.txt
