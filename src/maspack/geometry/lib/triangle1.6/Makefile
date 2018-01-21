ROOT_DIR = ../../../../..

LIB_TARGET_DIR = $(ROOT_DIR)/lib/Linux64
CLASS_DIR = $(ROOT_DIR)/classes/maspack/geometry
CLASS_ROOT_DIR = $(ROOT_DIR)/classes

default: libs

-include $(ROOT_DIR)/Makefile.base

ifndef ARTISYNTH_CORE_DIR
ARTISYNTH_CORE_DIR = $(ROOT_DIR)/../artisynth_core
endif
CLASS_ROOT_DIR := .:$(ROOT_DIR)/classes:$(ARTISYNTH_CORE_DIR)/classes

# OS Detection
ifndef SYSTEM
   ifeq ($(OS),Windows_NT)
      SYSTEM := Windows
      ifndef ARCH
         ARCH := 64
         ifeq ($(PROCESSOR_ARCHITECTURE),x86)
            ARCH := 32
            echo hello
         endif
      endif
   else
      SYSTEM := $(shell uname)
      MACHINE = $(shell uname -m)
      ifndef ARCH
         ifeq ($(findstring 64,$(MACHINE)),64)
            ARCH = 64
         else
            ARCH = 32
         endif
      endif
   endif
endif

CC_COMP = g++
CC_FLAGS = -g -O0 -fno-stack-protector -DANSI_DECLARATORS -DNO_TIMER -DTRILIBRARY -I.

C_COMP = gcc
C_FLAGS = -O0 -g -fPIC -std=c99 -mfpmath=sse -msse -I.

POINTER = unsigned long

ifeq ($(SYSTEM),Linux)
	ifeq ($(ARCH), 64) 
	   NATIVE_DIR = Linux64
	   X_FLAGS = -DAMD_64 -m64 -DPTR_EXCEEDS_LONG
	else
	   NATIVE_DIR = Linux
	   X_FLAGS = -m32 -DPOINTER="$(POINTER)"
	endif
	TRIANGLE_TARGET = libTriangleJNI.so.1.6
	LIB_FLAGS = -shared -lm
	X_FLAGS += -fPIC
	CC_INCS = -I"$(JAVA_HOME)/include/" -I"$(JAVA_HOME)/include/linux"
else ifeq ($(SYSTEM),Windows)
	ifeq ($(ARCH), 64) 
	   NATIVE_DIR = Windows64
	   X_FLAGS = -DAMD_64 -m64 -DPTR_EXCEEDS_LONG
	else
	   NATIVE_DIR = Windows32
	   X_FLAGS = -m32 -DPOINTER="$(POINTER)"
	endif
	TRIANGLE_TARGET = TriangleJNI.1.6.dll
	LIB_FLAGS = -shared -lm
	X_FLAGS += -fPIC
	CC_INCS = -I"$(JAVA_HOME)/include/" -I"$(JAVA_HOME)/include/win32"
	
	COLON = :
	SEMICOLON = ;
	CLASS_ROOT_DIR := $(subst $(COLON),$(SEMICOLON),$(CLASS_ROOT_DIR))
	CLASS_ROOT_DIR := "$(subst /,\,$(CLASS_ROOT_DIR))"
else ifeq ($(findstring CYGWIN,$(SYSTEM)),CYGWIN)
	X_FLAGS = -D_JNI_IMPLEMENTATION_ -DCYGWIN
	ifeq ($(ARCH), 64) 
	   NATIVE_DIR = Windows64
	   C_COMP = gcc # x86_64-w64-mingw32-gcc # gcc
	   C_FLAGS += -static-libgcc
	   CC_COMP = g++ #x86_64-w64-mingw32-g++ # g++
           CC_FLAGS += -static-libstdc++
	   X_FLAGS += -m64 -DPTR_EXCEEDS_LONG -DAMD_64
	else
	   NATIVE_DIR = Windows
	   C_COMP = i686-w64-mingw32-gcc
	   C_FLAGS += -static-libgcc
	   CC_COMP = i686-w64-mingw32-g++
	   CC_FLAGS += -static-libstdc++
	   X_FLAGS = -m32 -DPOINTER="$(POINTER)"
	endif
	ifndef JAVA_HOME
	   JAVA_HOME = C:/Program Files/Java/jdk1.6.0_24
        endif
	CC_INCS = -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32"
	TRIANGLE_TARGET = TriangleJNI.1.6.dll
	COLON = :
	SEMICOLON = ;
	CLASS_ROOT_DIR := $(subst $(COLON),$(SEMICOLON),$(CLASS_ROOT_DIR))
	CLASS_ROOT_DIR := "$(subst /,\,$(CLASS_ROOT_DIR))"
	LIB_FLAGS = -shared -Wl,--add-stdcall-alias -static-libgcc -lm
else ifeq ($(SYSTEM),Darwin)
        # XXX assume that all MacOS systems are 64 bit. That's because
        # we only have 64 bit pardiso for MacOS, and Java has a 64 bit
        # configuration, regardless of whether we are booting the 32
        # or 64 bit version of snow leopard
        NATIVE_DIR = Darwin-x86_64
        JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
	CC_INCS = -I"$(JAVA_HOME)/include"
	X_FLAGS = -fPIC -DDARWIN -DAMD_64
	CC_FLAGS += -fpermissive -faltivec
	TRIANGLE_TARGET = libTriangleJNI.1.6.dylib
	LIB_FLAGS = -bundle -framework JavaVM
endif

LIB_TARGET_DIR = $(ROOT_DIR)/lib/$(NATIVE_DIR)
LIB_TARGETS = \
	$(LIB_TARGET_DIR)/$(TRIANGLE_TARGET) 
	
C_FLAGS += $(X_FLAGS)
CC_FLAGS += $(X_FLAGS)

JNI_H_FILES = \
	maspack_geometry_TriangleTessellator.h

maspack_geometry_TriangleTessellator.h: $(CLASS_DIR)/TriangleTessellator.class
	$(JAVAH) $(JAVAHFLAGS) -classpath $(CLASS_ROOT_DIR) maspack.geometry.TriangleTessellator

triangle.o: triangle.c
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c triangle.c

# these are the predicates used by Triangle
# use -O0 to make sure optimization is disabled for predicates.o
# predicates.o: predicates.cxx
# 	$(CC_COMP) $(CC_FLAGS) -O0 $(CC_INCS) -c predicates.cxx

triangleJNI.o: triangleJNI.cpp $(JNI_H_FILES)
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -c triangleJNI.cpp

$(LIB_TARGET_DIR)/$(TRIANGLE_TARGET): triangleJNI.o triangle.o # predicates.o
	$(CC_COMP) $(CC_FLAGS) $(LIB_FLAGS) -o $@ $^ -lm

libs:	$(LIB_TARGETS)

install: $(LIB_TARGETS)
	rsync -Crlgo $(LIB_TARGETS) $(WEB_LIB_URL)/$(NATIVE_DIR)
	$(MAKE_HASHES_AND_FIX_PERMISSIONS)

depend:	$(JNI_H_FILES)

info:
	@echo SYSTEM: $(SYSTEM)
	@echo ARCH: $(ARCH)

clean CLEAN::
	$(call RM_CALL,*.o maspack_geometry_TriangleTessellator.h)

