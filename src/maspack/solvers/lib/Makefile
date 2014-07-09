# C-Files
# If linking with MKL, for help deciding what libraries to use, check out
# http://software.intel.com/en-us/articles/intel-mkl-link-line-advisor
#
# Pardiso seems to be compiled with 32 bit integers, so we use the
# lp64 libraries instead of the ilp64 libraries.
#
ROOT_DIR = ../../../..

default: libs

# C-Files compile

SYSTEM = $(shell uname)
MACHINE = $(shell uname -m) # not sure how portable this is ...
ifeq ($(findstring 64,$(MACHINE)),64)
   ARCH = 64
else
   ARCH = 32
endif
CC_COMP = g++
C_COMP = gcc

UMFPACK_DIR = $(ROOT_DIR)/src/maspack/umfpackfiles
JAVAH = javah

ifeq ($(SYSTEM),Linux)
	ifndef MKL_HOME
	   MKL_HOME = /scratch/intel/mkl/10.2.4.032
        endif
	ifeq ($(ARCH), 64)
	   NATIVE_DIR = Linux64
	   PARDISO = pardisoX86_64
	   MKL=$(MKL_HOME)/lib/em64t
	   # MKL=$(MKL_HOME)/lib/intel64 -- for MKL 11
	   # At last check, intel MKL blas were about 20% faster
	   LDS_PARDISO_MKL = -Wl,--start-group \
             $(MKL)/libmkl_intel_lp64.a $(MKL)/libmkl_gnu_thread.a \
             $(MKL)/libmkl_core.a -Wl,--end-group -fopenmp -lm
	else
	   NATIVE_DIR = Linux
	   PARDISO = pardisoX86
	   MKL=$(MKL_HOME)/lib/32
	   # MKL=$(MKL_HOME)/lib/ia32 -- for MKL 11
	   LDS_PARDISO_MKL = -Wl,--start-group \
             $(MKL)/libmkl_intel.a $(MKL)/libmkl_gnu_thread.a \
             $(MKL)/libmkl_core.a -Wl,--end-group -fopenmp -lm
	endif
	CC_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
		-I$(UMFPACK_DIR)/include -I.
	CC_FLAGS = -g -fno-strict-aliasing -fPIC -pthread -DLINUX 
        # need to specify libraries repeatedly to resolve circular dependenices
	LDS_UMFPACK = -lm -L$(UMFPACK_DIR)/lib/Linux -lumfpack -lamd -lgoto

	UMFPACK_TARGET = libSolversUmfpack.so
	PARDISO_MKL_TARGET = libPardisoJNI.so.1.0
	LIB_FLAGS = -shared
else ifeq ($(findstring CYGWIN,$(SYSTEM)),CYGWIN)
	ifndef MKL_HOME
	   MKL_HOME = C:/Program Files/Intel/ComposerXE-2011
        endif
	ifeq ($(findstring 64,$(SYSTEM)),64)
	   # 64 bit architecture
	   NATIVE_DIR = Windows64
	   LIB_TARGET_DIR = $(ROOT_DIR)/lib/Windows64
	   MKL=$(MKL_HOME)/mkl/lib/intel64
	   MKX=$(MKL_HOME)/compiler/lib/intel64
	   CC_COMP = x86_64-w64-mingw32-g++
	   C_COMP = x86_64-w64-mingw32-gcc
           CC_FLAGS = -g -m64 -static-libstdc++ -static-libgcc -D_JNI_IMPLEMENTATION_ -DCYGWIN
	   LDS_PARDISO_MKL = -Wl,--start-group \
		"$(MKL)"/mkl_intel_lp64.lib \
		"$(MKL)"/mkl_intel_thread.lib \
	        "$(MKL)"/mkl_core.lib \
	        chkstk_64.obj \
                -L$(LIB_TARGET_DIR) -liomp5md \
                -Wl,--end-group 
	else
	   NATIVE_DIR = Windows
	   LIB_TARGET_DIR = $(ROOT_DIR)/lib/Windows
	   #MKL=C:/Program Files/Intel/ComposerXE-2011/mkl/lib/ia32
	   MKL=$(MKL_HOME)/mkl/lib/ia32
	   MKX=$(MKL_HOME)/compiler/lib/ia32
	   #LDS_PARDISO_MKL = "$(MKL)"/mkl_intel_c_dll.lib "$(MKL)"/mkl_intel_thread_dll.lib \
	    "$(MKL)"/mkl_core_dll.lib -L$(LIB_TARGET_DIR) -liomp5md 
	   #C_COMP = gcc-3
	   #CC_COMP = g++-3
	   #CC_FLAGS = -g -fno-strict-aliasing -W -Wall -Wno-unused \
		-Wno-parentheses -mno-cygwin -D_JNI_IMPLEMENTATION_ -DCYGWI qN
#                "$(MKL)"/mkl_blas95.lib
	   LDS_PARDISO_MKL = -Wl,--start-group \
		"$(MKL)"/mkl_intel_c.lib \
		"$(MKL)"/mkl_intel_thread.lib \
	        "$(MKL)"/mkl_core.lib \
                -Wl,--end-group chkstk_32.obj llmul_32.obj \
		-L$(LIB_TARGET_DIR) -liomp5md 
	   CC_COMP = i686-w64-mingw32-g++
	   C_COMP = i686-w64-mingw32-gcc
	   CC_FLAGS = -g -static-libstdc++ -static-libgcc -D_JNI_IMPLEMENTATION_ -DCYGWIN
	endif   
	ifndef JAVA_HOME
	   JAVA_HOME = C:/Program Files/Java/jdk1.6.0_37
        endif
	CC_INCS = -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32" \
		-I$(UMFPACK_DIR)/include -I.
	LDS_UMFPACK = -lm -L$(UMFPACK_DIR)/lib/Windows -lumfpack -lamd -lgoto
	UMFPACK_TARGET = SolversUmfpack.dll
	PARDISO_MKL_TARGET = PardisoJNI.1.0.dll
	LIB_FLAGS = -shared -Wl,--add-stdcall-alias
else ifeq ($(SYSTEM),Darwin)
	NATIVE_DIR = Darwin-x86_64
        JAVA_INC=/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
	CC_INCS = -I$(JAVA_INC) -I$(UMFPACK_DIR)/include -I.
	CC_FLAGS = -m64 -g -fno-strict-aliasing -fPIC -DDARWIN -fpermissive -faltivec
	#CC_FLAGS = -m64 -g -fno-strict-aliasing -fPIC -W -Wall -Wno-unused \
	#	-Wno-parentheses -DDARWIN -fpermissive -faltivec
	LDS_UMFPACK = -lm -L$(UMFPACK_DIR)/lib/Darwin -lumfpack -lamd \
		-lcrypto -framework vecLib 
	COMPOSER=/opt/intel/composerxe/lib
	MKL=/opt/intel/composerxe/mkl/lib
	LDS_PARDISO_MKL = -L$(MKL) -L$(COMPOSER) \
		$(MKL)/libmkl_intel_lp64.a $(MKL)/libmkl_intel_thread.a \
		$(MKL)/libmkl_core.a -liomp5 -lpthread     
	UMFPACK_TARGET = libSolversUmfpack.dylib
	PARDISO_MKL_TARGET = libPardisoJNI.1.0.dylib
	LIB_FLAGS = -bundle -framework JavaVM
endif

LIB_TARGET_DIR = $(ROOT_DIR)/lib/$(NATIVE_DIR)
LIB_TARGETS = $(LIB_TARGET_DIR)/$(PARDISO_MKL_TARGET)

JNI_H_FILES = maspack_solvers_UmfpackSolver.h maspack_solvers_PardisoSolver.h

# umfpack JNI

maspack_solvers_UmfpackSolver.h: ../UmfpackSolver.java
	$(JAVAH) $(JAVAHFLAGS) maspack.solvers.UmfpackSolver

UmfpackJNI.o: maspack_solvers_UmfpackSolver.h UmfpackJNI.c
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o UmfpackJNI.o UmfpackJNI.c

$(LIB_TARGET_DIR)/$(UMFPACK_TARGET): UmfpackJNI.o
	$(CC_COMP) $(LIB_FLAGS) $(CC_FLAGS) -static-libgcc -o $@ UmfpackJNI.o \
		$(LDS_UMFPACK)

.PHONY: umfpack
umfpack:	$(LIB_TARGET_DIR)/$(UMFPACK_TARGET)

# pardiso JNI and test programs

maspack_solvers_PardisoSolver.h: ../PardisoSolver.java
	$(JAVAH) $(JAVAHFLAGS) maspack.solvers.PardisoSolver

testUtils.o: testUtils.h testUtils.cc
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o testUtils.o testUtils.cc

pardisoMkl.o: pardisoMkl.cc pardisoMkl.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o pardisoMkl.o pardisoMkl.cc

PardisoMklJNI.o: maspack_solvers_PardisoSolver.h PardisoMklJNI.cc pardisoMkl.h
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o PardisoMklJNI.o PardisoMklJNI.cc

pardisoMklTest.o: pardisoMklTest.cc 
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c -o pardisoMklTest.o pardisoMklTest.cc

pardiso_mkl_bug: pardiso_mkl_bug.c
	$(C_COMP) $(C_FLAGS) -o pardiso_mkl_bug pardiso_mkl_bug.c
	$(LDS_PARDISO_MKL)

pardisoMklTest: pardisoMkl.o pardisoMklTest.o pardisoMkl.h
	$(CC_COMP) $(CC_FLAGS) -o pardisoMklTest pardisoMklTest.o pardisoMkl.o \
		-L$(LIB_TARGET_DIR) $(LDS_PARDISO_MKL)

$(LIB_TARGET_DIR)/$(PARDISO_MKL_TARGET): PardisoMklJNI.o pardisoMkl.o
	$(CC_COMP)  $(LIB_FLAGS) $(CC_FLAGS) -o $@ \
		PardisoMklJNI.o pardisoMkl.o $(LDS_PARDISO_MKL) 

.PHONY: pardiso
pardiso:	$(LIB_TARGET_DIR)/$(PARDISO_MKL_TARGET)

pardisoTestExample: pardisoTestExample.c
	$(C_COMP) $(CC_FLAGS) -o pardisoTestExample pardisoTestExample.c \
	$(LDS_PARDISO_MKL)

libs:	$(LIB_TARGET_DIR)/$(PARDISO_MKL_TARGET)

depend:	$(JNI_H_FILES)

clean::
	rm -rf pardisoTest pardiso*Example

install: $(LIB_TARGETS)
	rsync -Crlgo $(LIB_TARGETS) $(WEB_LIB_URL)/$(NATIVE_DIR)
	$(MAKE_HASHES_AND_FIX_PERMISSIONS)

-include $(ROOT_DIR)/Makefile.base
