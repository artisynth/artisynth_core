# This NMakefile builds MumpsJNI.<version>.dll, the native side of
# maspack.solvers.MumpsSolver, mirroring NMakefile (Pardiso)'s structure and
# conventions. Should be run in the "x64 Native Tools Command Prompt", using
#
# > nmake /F NMakefile.mumps
#
# The result is a stand-alone DLL: everything is statically linked EXCEPT
# libiomp5md.dll, which is shared with PardisoJNI (see LIBMP below) and
# must already be shipped alongside it in lib\Windows64.
#
#   MKL                    static, and exports nothing by default -- MSVC
#                           DLLs export only __declspec(dllexport) symbols,
#                           unlike Linux where MKL's 18k symbols had to be
#                           hidden explicitly with a version script.
#   MUMPS, PORD, libmpiseq  static (this is an MPI-free, OpenMP-threaded
#                           MUMPS build -- libmpiseq is the sequential MPI
#                           stub)
#   METIS, SCOTCH           static, built specifically for this library
#   libiomp5md              shared (see above). MUMPS' Fortran is compiled
#                           by ifx -Qopenmp, which targets libiomp5 natively
#                           -- no GOMP-vs-iomp5 translation trick needed
#                           here the way Linux's -fopenmp/-liomp5 pairing
#                           was.
#
# IMPORTANT: libiomp5md.dll (and libifcoremd/libmmd/svml_dispmd below) must
# be the SAME version PardisoJNI was built against -- MUMPS compiled by ifx
# 2026.1 needs OpenMP 5.1's __kmpc_masked, which older libiomp5md.dll builds
# don't export, and Intel's OpenMP runtime refuses to let two *different*
# copies of itself load in one process regardless of DLL filename (a
# private renamed copy for just Mumps was tried and rejected for exactly
# this reason). PardisoJNI must therefore also be (re)built against this
# same oneAPI version; see NMakefile.
#
# /MD (dynamic CRT) is used deliberately, matching what MUMPS_5.9.1's own
# Makefile.inc (OPTC/OPTF) and METIS/SCOTCH's CMake Release builds already
# link against -- mixing /MD and /MT across statically-linked objects
# produces CRT mismatches (duplicate symbols, or worse, mismatched heaps at
# runtime).
#
# No .def file or version script is needed: MumpsJNI.cc's JNI entry points
# are already wrapped in JNIEXPORT, which expands to __declspec(dllexport)
# on Windows (see jni_md.h) -- nothing else gets exported.
#
# The locations of MUMPS, MKL, SCOTCH, METIS and JAVA_HOME may be
# overridden on the nmake command line, e.g.
#   nmake /F NMakefile.mumps MUMPS_HOME=D:\packages\MUMPS_5.9.1

ROOT_DIR = ..\..\..\..
LIB_TARGET_DIR = $(ROOT_DIR)\lib\Windows64

MUMPS_VERSION = 5.9.1
MUMPS_TARGET = MumpsJNI.$(MUMPS_VERSION).dll

CC_COMP = cl
CC_FLAGS = /O2 /MD /DWINDOWS_COMPILER

!IFNDEF JAVA_HOME
JAVA_HOME = "C:\Program Files\Java\jdk-1.8"
!ENDIF
!IFNDEF MUMPS_HOME
MUMPS_HOME = C:\Users\lloyd\packages\MUMPS_5.9.1
!ENDIF
!IFNDEF SCOTCH_HOME
SCOTCH_HOME = C:\Users\lloyd\packages\scotch-7.0.12-static
!ENDIF
!IFNDEF METIS_HOME
METIS_HOME = C:\Users\lloyd\packages\metis-5.1.0-static
!ENDIF
!IFNDEF INTEL_HOME
INTEL_HOME = "C:\Program Files (x86)\Intel\oneAPI"
!ENDIF
!IFNDEF MKL_HOME
MKL_HOME = $(INTEL_HOME)\mkl\2026.1
!ENDIF
!IFNDEF THREAD_HOME
THREAD_HOME = $(INTEL_HOME)\compiler\2026.1\lib
!ENDIF

CC_INCS = /I. /I$(JAVA_HOME)\include /I$(JAVA_HOME)\include\win32 \
	  /I$(MUMPS_HOME)\include /I$(MKL_HOME)\include

# static MUMPS, together with its ordering packages and the sequential MPI
# stub -- fully-qualified paths, so no /LIBPATH: needed for these
LDS_MUMPS = \
	$(MUMPS_HOME)\lib\libdmumps.lib $(MUMPS_HOME)\lib\libmumps_common.lib \
	$(MUMPS_HOME)\lib\libpord.lib   $(MUMPS_HOME)\libseq\libmpiseq.lib \
	$(SCOTCH_HOME)\lib\esmumps.lib $(SCOTCH_HOME)\lib\scotch.lib \
	$(SCOTCH_HOME)\lib\scotcherr.lib \
	$(METIS_HOME)\lib\libmetis.lib

# static MKL, for the BLAS/LAPACK used by the factorization -- link.exe
# resolves these iteratively, so no --start-group/--end-group equivalent
# is needed the way it was on Linux
LDS_MKL = /LIBPATH:$(MKL_HOME)\lib \
	mkl_intel_lp64.lib mkl_intel_thread.lib mkl_core.lib

# multithreading library, shared with PardisoJNI -- see NMakefile. Also
# on this /LIBPATH: everything else ifx's -Qopenmp-compiled objects embed
# /DEFAULTLIB references to (ifconsol.lib, libifcoremd.lib, ...) lives in
# the same THREAD_HOME directory.
LIBMP = /LIBPATH:$(THREAD_HOME) libiomp5md.lib

default: libs

libs: $(LIB_TARGET_DIR)\$(MUMPS_TARGET)

# Regenerated whenever MumpsSolver.java's native method declarations
# change -- see the equivalent rule in NMakefile (Pardiso) for why this
# matters: without it, a new native method silently links under a mangled
# symbol name instead of failing the build.
maspack_solvers_MumpsSolver.h: ..\MumpsSolver.java
	$(JAVA_HOME)\bin\javac -h . -d $(ROOT_DIR)\classes \
	   -cp $(ROOT_DIR)\classes ..\MumpsSolver.java

# hybrid (preconditioned iterative) solves, shared with the Pardiso JNI
# library -- see NMakefile
hybridSolve.obj: hybridSolve.cc hybridSolve.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) /c hybridSolve.cc

mumps.obj: mumps.cc mumps.h hybridSolve.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) /c mumps.cc

MumpsJNI.obj: MumpsJNI.cc mumps.h maspack_solvers_MumpsSolver.h hybridSolve.h hybridSolveJNI.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) /c MumpsJNI.cc

# build command for the DLL
$(LIB_TARGET_DIR)\$(MUMPS_TARGET): MumpsJNI.obj mumps.obj hybridSolve.obj
	$(CC_COMP) $(CC_FLAGS) MumpsJNI.obj mumps.obj hybridSolve.obj \
	$(LDS_MUMPS) \
	/link $(LDS_MKL) $(LIBMP) /DLL /out:$@

# Check that the DLL depends only on what we expect, and exports only the
# JNI entry points. Mirrors "make -f Makefile.mumps check" on Linux.
check: $(LIB_TARGET_DIR)\$(MUMPS_TARGET)
	@echo dependencies:
	dumpbin /DEPENDENTS $**
	@echo exported symbols:
	dumpbin /EXPORTS $**
	@echo libiomp5md.dll (and libifcoremd/libmmd/svml_dispmd) must already
	@echo be present in $(LIB_TARGET_DIR), built against the SAME oneAPI
	@echo version as PardisoJNI.

clean.mumps:
	del /q mumps.obj MumpsJNI.obj hybridSolve.obj maspack_solvers_MumpsSolver.h 2>NUL
