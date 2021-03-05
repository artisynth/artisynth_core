# Makefile for creating native solver libraries. At the moment, the
# only supported solver is MKL Pardiso. Support for Umfpack has been
# dropped because the solver is slow.
#
# As of November 2015, the first three version numbers of the
# PardisoJNI library are set to reflect the numbers of the MKL
# distribution on which it is based. So for example,
# PardisoJNI.so.11.1.2.x is based on MKL 11.1.2.
#
# When linking with MKL, for help deciding what libraries to use, see
# http://software.intel.com/en-us/articles/intel-mkl-link-line-advisor
#
# To use this Makefile on a generic system, the environment variable
# MKL_HOME should be set to the root folder for the current MKL
# distribution, which is then used to locate the relevant lib and
# include directories.  Where these are located exactly depend on the
# system. If MKL_HOME is not set, it will be set to the current
# default location(s) on the primary ArtiSynth development machines.
#

ROOT_DIR = ../../../..

default: libs

SYSTEM = $(shell uname)
MACHINE = $(shell uname -m) # not sure how portable this is ...
ifeq ($(findstring 64,$(MACHINE)),64)
   ARCH = 64
else
   ARCH = 32
endif
CC_COMP = g++
C_COMP = gcc

JAVAH = javah

# Currently, we are stuck at MKL 11.1.2, because the way we use hyrid
# solves breaks for MKL 11.1.3 and higher.

# Pardiso version number based on MKL 11.1.2, with support for setting
# the maximum number of threads. Make sure there is no whitespace at
# the end.
PARDISO_VERSION = 2021.1

ifeq ($(SYSTEM),Linux)
	ifndef MKL_HOME
	   MKL_HOME = /data/intel/oneapi/mkl/2021.1.1
        endif
        ifndef MKL_THREAD_LIB
	   MKL_THREAD_LIB = /data/intel/oneapi/compiler/2021.1.1/linux/compiler/lib
        endif
	ifeq ($(ARCH), 64)
	   NATIVE_DIR = Linux64
	   PARDISO = pardisoX86_64
	   MKL=$(MKL_HOME)/lib/intel64
	   # At last check, intel MKL blas were about 20% faster
           #
	   # link with iomp5 because libiomp5 is more compatible when
	   # running ArtiSynth under matlab. Matlab uses libiomp5, so
	   # we save an extra load of a thread-enabled dll and are
	   # hence less likely to encounter the error "dlopen: cannot
	   # load any more object with static TLS".
           #
           # the option -Wl,--wrap=memcpy tells the linker to replace
           # references to memcpy with wrap_memcpy, which is defined
	   # wrap_memcpy.c and which explicitly calls in a earlier
	   # version of memcpy that does not depend on glbic 2.14
           #

           # -Wl,--wrap=memcpy
	   LDS_PARDISO_MKL = -L$(MKL_THREAD_LIB)/intel64_lin \
	     -Wl,--start-group \
             $(MKL)/libmkl_intel_lp64.a $(MKL)/libmkl_intel_thread.a \
             $(MKL)/libmkl_core.a -Wl,--end-group \
	     -ldl -liomp5 -lpthread -lm
	   CC_FLAGS = -m64 -O2 -fno-strict-aliasing -fpermissive -fPIC -pthread -DLINUX
	else
	   # 32 bit Linux is no longer supported, so this may not work
	   NATIVE_DIR = Linux
	   PARDISO = pardisoX86
	   MKL=$(MKL_HOME)/lib/ia32
	   LDS_PARDISO_MKL = -Wl,--start-group \
             $(MKL)/libmkl_intel.a $(MKL)/libmkl_gnu_thread.a \
             $(MKL)/libmkl_core.a -Wl,--end-group -Wl,--wrap=memcpy \
             -fopenmp -lm
	   CC_FLAGS = -O2 -fno-strict-aliasing -fPIC -pthread -DLINUX 
	endif
	CC_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
		-I$(MKL_HOME)/include -I.
	PARDISO_MKL_OBJS = PardisoMklJNI.o pardisoMkl.o wrap_memcpy.o
	PARDISO_MKL_TARGET = libPardisoJNI.so.$(PARDISO_VERSION)
        # the use of rpath ensures that library dependencies can also be loaded
        # from lib/Linux or lib/Linux64; 
        # see https://github.com/bimargulies/jni-origin-testbed
	LIB_FLAGS = -shared -Wl,-rpath='$$ORIGIN' -static-libstdc++ -static-libgcc
else ifeq ($(findstring CYGWIN,$(SYSTEM)),CYGWIN)
        #
        # NOTE: THIS SECTION IS OBSOLETE since 2014, as Windows dll for
        # Pardiso are now built using nmake and the Intel compiler. See
        # INTEL_COMPILATION and NMakefile and NMakefile32 for details.
        # 
        # This section uses mingw to compile Pardiso on Windows. It is
        # included for historical reference only and would need to be
        # updated to make it work at the present.
        #
	ifndef MKL_HOME
	   MKL_HOME = "C:\Program Files (x86)\Intel\Composer XE 2013 SP1"
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
	   MKL=$(MKL_HOME)/mkl/lib/ia32
	   MKX=$(MKL_HOME)/compiler/lib/ia32
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
	CC_INCS = -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32" -I.
	PARDISO_MKL_OBJS = PardisoMklJNI.o pardisoMkl.o
	PARDISO_MKL_TARGET = PardisoJNI.$(PARDISO_VERSION).dll
	LIB_FLAGS = -shared -Wl,--add-stdcall-alias
else ifeq ($(SYSTEM),Darwin)
	ifndef MKL_HOME
	   MKL_HOME = /opt/intel/oneapi/mkl/2021.1.1
        endif
        ifndef MKL_THREAD_LIB
	   MKL_THREAD_LIB = /opt/intel/oneapi/compiler/2021.1.1/mac/compiler/lib
        endif
	ifndef JAVA_HOME
	   JAVA_HOME = /Library/Java/JavaVirtualMachines/jdk1.8.0_271.jdk/Contents/Home
        endif
	NATIVE_DIR = MacOS64
	MKL=$(MKL_HOME)/lib
	LDS_PARDISO_MKL = -L$(MKL) -L$(MKL_THREAD_LIB) \
		$(MKL)/libmkl_intel_lp64.a $(MKL)/libmkl_intel_thread.a \
		$(MKL)/libmkl_core.a -liomp5 -lpthread     
	CC_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin -I$(MKL_HOME)/include -I.
	CC_FLAGS = -m64 -fno-strict-aliasing -fPIC -DDARWIN -fpermissive
	PARDISO_MKL_OBJS = PardisoMklJNI.o pardisoMkl.o
	PARDISO_MKL_TARGET = libPardisoJNI.$(PARDISO_VERSION).dylib
	LIB_FLAGS = -bundle -framework JavaVM 
endif

LIB_TARGET_DIR = $(ROOT_DIR)/lib/$(NATIVE_DIR)

LIB_TARGETS = $(LIB_TARGET_DIR)/$(PARDISO_MKL_TARGET)

JNI_H_FILES = maspack_solvers_PardisoSolver.h

# pardiso JNI and test programs

maspack_solvers_PardisoSolver.h: ../PardisoSolver.java
	$(JAVAH) $(JAVAHFLAGS) maspack.solvers.PardisoSolver

# Used only on Linux systems. Allows us to use a version of memcpy
# that does not depend on glibc 2.14
wrap_memcpy.o: wrap_memcpy.c
	$(C_COMP) $(C_FLAGS) -fPIC -c -o wrap_memcpy.o wrap_memcpy.c

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

$(LIB_TARGET_DIR)/$(PARDISO_MKL_TARGET): $(PARDISO_MKL_OBJS) 
	$(CC_COMP)  $(LIB_FLAGS) $(CC_FLAGS) -o $@ \
	$(PARDISO_MKL_OBJS) $(LDS_PARDISO_MKL) 
ifeq ($(SYSTEM),Darwin)
        # add to @loader_path to libiomp5 name so that we don't need to
        # set DYLD_LIBRARY_PATH in applications; see
        # http://dssheep.blogspot.ca/2011/11/how-to-avoid-dyldlibrarypath-with-jni.html
	install_name_tool -change "@rpath/libiomp5.dylib" "@loader_path/libiomp5.dylib" $@
endif

.PHONY: pardiso
pardiso:	$(LIB_TARGET_DIR)/$(PARDISO_MKL_TARGET)

pardisoTestExample: pardisoTestExample.c
	$(C_COMP) $(CC_FLAGS) -o pardisoTestExample pardisoTestExample.c \
	$(LDS_PARDISO_MKL)

depend:	$(JNI_H_FILES)

clean::
	rm -rf pardisoTest pardiso*Example *.obj

ifeq ($(findstring CYGWIN,$(SYSTEM)),CYGWIN)
# on Cygwin, we don't build the libs; we use the Intel compiler and nmake
# for that. Instead, we only use the install command to update the
# 32 and 64 bit dll files on the server.
libs:	
	echo "dll files should be built using Intel compiler and nmake"

PARDISO_DLL_64 = $(ROOT_DIR)/lib/Windows64/$(PARDISO_MKL_TARGET)
PARDISO_DLL_32 = $(ROOT_DIR)/lib/Windows/$(PARDISO_MKL_TARGET)

install:
	rsync -Ctrlgo $(PARDISO_DLL_64) $(WEB_LIB_URL)/Windows64
	rsync -Ctrlgo $(PARDISO_DLL_32) $(WEB_LIB_URL)/Windows
	$(MAKE_HASHES_AND_FIX_PERMISSIONS)
else

libs:	$(LIB_TARGET_DIR)/$(PARDISO_MKL_TARGET)

install: $(LIB_TARGETS)
	rsync -Ctrlgo $(LIB_TARGETS) $(WEB_LIB_URL)/$(NATIVE_DIR)
	$(MAKE_HASHES_AND_FIX_PERMISSIONS)
endif

-include $(ROOT_DIR)/Makefile.base
