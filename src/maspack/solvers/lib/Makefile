CC_COMP = icl
CC_FLAGS = /O2 /Qopenmp /DINTEL_COMPILER /DCYGWIN
JAVA_HOME = "C:\Program Files\Java\jdk1.6.0_37"
CC_INCS = /I. /I$(JAVA_HOME)\include /I$(JAVA_HOME)\include\win32
MKL_HOME = "C:\Program Files (x86)\Intel\Composer XE 2013 SP1"
MKL_64 = $(MKL_HOME)"\mkl\lib\intel64"
MKL_32 = $(MKL_HOME)"\mkl\lib\ia32"
PARDISO_LIBS_64 = mkl_intel_lp64.lib mkl_core.lib mkl_intel_thread.lib
PARDISO_LIBS_32 = mkl_intel_c.lib mkl_core.lib mkl_intel_thread.lib

default: PardisoJNI.1.0.dll

pardisoMkl.obj: pardisoMkl.cc pardisoMkl.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) /c /o pardisoMkl.obj pardisoMkl.cc  

pardisoMklJNI.obj: maspack_solvers_PardisoSolver.h PardisoMklJNI.cc pardisoMkl.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) /c /o pardisoMklJNI.obj pardisoMklJNI.cc  

pardisoMklTest.obj: pardisoMklTest.cc 
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) /c /o pardisoMklTest.obj pardisoMklTest.cc

pardisoMklTest.exe: pardisoMkl.obj pardisoMklTest.obj pardisoMkl.h
	$(CC_COMP) $(CC_FLAGS) /o pardisoMklTest.exe pardisoMklTest.obj \
		pardisoMkl.obj /Qmkl $(PARDISO_LIBS_64)

PardisoJNI.1.0.dll: PardisoMklJNI.obj pardisoMkl.obj
	$(CC_COMP) $(CC_FLAGS) -o $@ PardisoMklJNI.obj pardisoMkl.obj \
	$(PARDISO_LIBS_64) /LD /MT /link

hello.obj: hello.cc
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) /c /o hello.obj hello.cc

hello.exe: hello.obj
	$(CC_COMP) $(CC_FLAGS) /Qmkl:parallel /o hello.exe hello.obj mkl_intel_lp64.lib mkl_core.lib mkl_intel_thread.lib

