# Makefile for building the MUMPS JNI library, libMumpsJNI.so.<version>,
# which implements the native side of maspack.solvers.MumpsSolver.
#
# The result is a stand-alone shared library: everything is statically linked
# EXCEPT libiomp5, libgfortran, libquadmath and libc:
#
#   MKL           static, and hidden (--exclude-libs,ALL plus a version
#                 script which exports only the JNI entry points)
#   MUMPS, PORD   static, together with the libmpiseq MPI stub, since this
#                 is an MPI-free (but OpenMP threaded) MUMPS build
#   METIS, SCOTCH static, built PIC specifically for this library; see
#                 MUMPS_COMPILATION.md for how they were made
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
# Usage:
#
#   make -f Makefile.mumps            build the library
#   make -f Makefile.mumps check      build it and check its dependencies
#   make -f Makefile.mumps clean.mumps
#
# The locations of MUMPS, MKL, SCOTCH and METIS may be overridden on the
# command line or through the environment. Building those packages in the
# first place is described in MUMPS_COMPILATION.md; the Windows port is
# described in MUMPS_COMPILATION_WINDOWS.md.

ROOT_DIR = ../../../..

SYSTEM = $(shell uname)
MACHINE = $(shell uname -m)

CC_COMP = g++

# MUMPS version, which becomes the version number of the JNI library. Make
# sure there is no whitespace at the end.
MUMPS_VERSION = 5.9.1

ifndef MUMPS_HOME
   MUMPS_HOME = $(HOME)/packages/MUMPS_$(MUMPS_VERSION)
endif
ifndef MKL_HOME
   MKL_HOME = /opt/intel/oneapi2025/mkl/latest
endif
ifndef MKL_THREAD_LIB
   MKL_THREAD_LIB = /opt/intel/oneapi2025/compiler/latest/lib
endif
ifndef SCOTCH_HOME
   SCOTCH_HOME = $(HOME)/packages/scotch-7.0.12-static
endif
ifndef METIS_HOME
   METIS_HOME = $(HOME)/packages/metis-5.1.0-static
endif
ifndef JAVA_HOME
   JAVA_HOME = /usr/lib/jvm/default-java
endif

ifeq ($(findstring 64,$(MACHINE)),64)
   NATIVE_DIR = Linux64
else
   NATIVE_DIR = Linux
endif

LIB_TARGET_DIR = $(ROOT_DIR)/lib/$(NATIVE_DIR)
MUMPS_TARGET = libMumpsJNI.so.$(MUMPS_VERSION)

MUMPS_OBJS = MumpsJNI.o mumps.o

CC_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
	  -I$(MUMPS_HOME)/include -I.

CC_FLAGS = -m64 -O2 -fno-strict-aliasing -fPIC -pthread -DLINUX

# static MUMPS, together with its ordering packages and the sequential MPI
# stub
LDS_MUMPS = \
	$(MUMPS_HOME)/lib/libdmumps.a $(MUMPS_HOME)/lib/libmumps_common.a \
	$(MUMPS_HOME)/lib/libpord.a   $(MUMPS_HOME)/lib/libmpiseq.a \
	$(SCOTCH_HOME)/lib/libesmumps.a $(SCOTCH_HOME)/lib/libscotch.a \
	$(SCOTCH_HOME)/lib/libscotcherr.a \
	$(METIS_HOME)/lib/libmetis.a

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

default: mumps

mumps_version.map:
	echo '{ global: Java_*; local: *; };' > mumps_version.map

maspack_solvers_MumpsSolver.h: ../MumpsSolver.java
	javac -h . -d $(ROOT_DIR)/classes -cp "$(ROOT_DIR)/classes" \
	   ../MumpsSolver.java

mumps.o: mumps.cc mumps.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o mumps.o mumps.cc

MumpsJNI.o: MumpsJNI.cc mumps.h maspack_solvers_MumpsSolver.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o MumpsJNI.o MumpsJNI.cc

$(LIB_TARGET_DIR)/$(MUMPS_TARGET): $(MUMPS_OBJS) mumps_version.map
	$(CC_COMP) $(LIB_FLAGS) $(CC_FLAGS) -o $@ \
	$(MUMPS_OBJS) $(LDS_MUMPS_JNI)

.PHONY: mumps check clean.mumps

mumps: $(LIB_TARGET_DIR)/$(MUMPS_TARGET)

# Check that the library depends only on the libraries we expect, and that it
# exports only the JNI entry points.
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

clean.mumps:
	rm -f $(MUMPS_OBJS) mumps_version.map
