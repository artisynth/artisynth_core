ROOT_DIR = ../../../..

LIB_TARGET_DIR = $(ROOT_DIR)/lib/Linux
CLASS_DIR = $(ROOT_DIR)/classes/maspack/geometry
CLASS_ROOT_DIR = $(ROOT_DIR)/classes

default: libs

-include $(ROOT_DIR)/Makefile.base

ifndef ARTISYNTH_CORE_DIR
ARTISYNTH_CORE_DIR = $(ROOT_DIR)/../artisynth_core
endif
CLASS_ROOT_DIR := .:$(ROOT_DIR)/classes:$(ARTISYNTH_CORE_DIR)/classes

ifeq ($(findstring CYGWIN,$(SYSTEM)),CYGWIN)
   # If we are compiling under cygwin, we may need to set ARCH=64
   # explicitly, because cygwin often runs as 32-bits even on 64 bit
   # machines. We may also make want to set ARCH=32 if we are cross
   # compiling for 32-bit Windows from a 64 bit machine
   ARCH=64
endif

CC_COMP = g++
CC_FLAGS = -O2 -DTETLIBRARY -I.

C_COMP = gcc
C_FLAGS = -O2 -std=c99 -mfpmath=sse -msse -I.
#C_FLAGS = -O2 -g -fPIC -std=c99 -mfpmath=sse -msse -I.
#C_FLAGS = -O0 -g -fPIC -std=c99 -mfpmath=sse -msse -I.

# current version of tetgen. Make sure there is no whitespace at the end.
TETGEN_VERSION = 1.5.1.1
TETGEN_DIR = tetgen1.5.1

TETGEN_OBJ_FILES = $(TETGEN_DIR)/tetgen.o $(TETGEN_DIR)/predicates.o

ifeq ($(SYSTEM),Linux)
	ifeq ($(ARCH), 64) 
	   NATIVE_DIR = Linux64
	   X_FLAGS = -DAMD_64 -DPOINTER_EXCEEDS_LONG
	else
	   NATIVE_DIR = Linux
	   X_FLAGS = -m32
	endif
	TETGEN_TARGET = libTetgenJNI.so.$(TETGEN_VERSION)
	ROBUST_TARGET = libRobustPreds.so.1.1
	ROBUST_TARGET_OLD = libRobustPreds.so.0.0
	GETCW_TARGET = libGetCW.so
	SHEWCHUK_DEF = -DLINUX
	LIB_FLAGS = -shared
	X_FLAGS += -fPIC
	C_FLAGS += -fPIC
	CC_INCS = -I$(JAVA_HOME)/include/ -I$(JAVA_HOME)/include/linux -I$(TETGEN_DIR)
else ifeq ($(findstring CYGWIN,$(SYSTEM)),CYGWIN)
	X_FLAGS = -D_JNI_IMPLEMENTATION_ -DCYGWIN
	ifeq ($(ARCH), 64) 
	   NATIVE_DIR = Windows64
	   C_COMP = x86_64-w64-mingw32-gcc  # Mingw-w64, gcc, 64 bit target
	   CC_COMP = x86_64-w64-mingw32-g++ # Mingw-w64, g++, 64 bit target 
       X_FLAGS += -m64 -DPTR_EXCEEDS_LONG -DAMD_64
	else
	   NATIVE_DIR = Windows
	   C_COMP = i686-w64-mingw32-gcc  # Mingw-w64, gcc, 32 bit target
	   CC_COMP = i686-w64-mingw32-g++ # Mingw-w64, g++, 32 bit target
	endif

	C_FLAGS += -static-libgcc
	CC_FLAGS += -static-libstdc++

	ifndef JAVA_HOME
	   JAVA_HOME = C:/Program Files/Java/jdk1.6.0_24
	endif
	CC_INCS = -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32" -I$(TETGEN_DIR)
	TETGEN_TARGET = TetgenJNI.$(TETGEN_VERSION).dll
	ROBUST_TARGET = RobustPreds.1.1.dll
	ROBUST_TARGET_OLD = RobustPreds.0.0.dll
	SHEWCHUK_DEF = -DCPU86
	COLON = :
	SEMICOLON = ;
	CLASS_ROOT_DIR := $(subst $(COLON),$(SEMICOLON),$(CLASS_ROOT_DIR))
	CLASS_ROOT_DIR := "$(subst /,\,$(CLASS_ROOT_DIR))"
	LIB_FLAGS = -shared -Wl,--add-stdcall-alias -static -static-libgcc
else ifeq ($(findstring MINGW,$(SYSTEM)),MINGW)
	X_FLAGS = -D_JNI_IMPLEMENTATION_
	ifeq ($(ARCH), 64)
	   NATIVE_DIR = Windows64
	   C_COMP = x86_64-w64-mingw32-gcc  # Mingw-w64, gcc, 64 bit target
	   CC_COMP = x86_64-w64-mingw32-g++ # Mingw-w64, g++, 64 bit target 
	   X_FLAGS += -m64 -DPTR_EXCEEDS_LONG -DAMD_64
	else
	   NATIVE_DIR = Windows
	   C_COMP = i686-w64-mingw32-gcc  # Mingw-w64, gcc, 32 bit target
	   CC_COMP = i686-w64-mingw32-g++ # Mingw-w64, g++, 32 bit target
	endif
	C_FLAGS += -static-libgcc
	CC_FLAGS += -static-libstdc++

	ifndef JAVA_HOME
	   JAVA_HOME = C:/Program Files/Java/jdk1.6.0_24
	endif
	CC_INCS = -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32"  -I"$(JAVA_HOME)/include/linux" -I$(TETGEN_DIR)
	TETGEN_TARGET = TetgenJNI.$(TETGEN_VERSION).dll
	ROBUST_TARGET = RobustPreds.1.0.dll
	ROBUST_TARGET_OLD = RobustPreds.0.0.dll
	SHEWCHUK_DEF = -DCPU86
	LIB_FLAGS = -shared -Wl,--add-stdcall-alias -static -static-libgcc
else ifeq ($(SYSTEM),Windows)
	X_FLAGS = -D_JNI_IMPLEMENTATION_
	ifeq ($(ARCH), 64)
	   NATIVE_DIR = Windows64
	   X_FLAGS += -m64 -DPTR_EXCEEDS_LONG -DAMD_64
	else
	   NATIVE_DIR = Windows
	endif
	C_FLAGS += -static-libgcc
	CC_FLAGS += -static-libstdc++
	ifndef JAVA_HOME
	   JAVA_HOME = C:/Program Files/Java/jdk1.6.0_24
	endif
	CC_INCS = -I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32" -I$(TETGEN_DIR)
	TETGEN_TARGET = TetgenJNI.$(TETGEN_VERSION).dll
	ROBUST_TARGET = RobustPreds.1.0.dll
	ROBUST_TARGET_OLD = RobustPreds.0.0.dll
	SHEWCHUK_DEF = -DCPU86

	COLON = :
	SEMICOLON = ;
	CLASS_ROOT_DIR := $(subst $(COLON),$(SEMICOLON),$(CLASS_ROOT_DIR))
	CLASS_ROOT_DIR := "$(subst /,\,$(CLASS_ROOT_DIR))"
	LIB_FLAGS = -shared -Wl,--add-stdcall-alias -static -static-libgcc
else ifeq ($(SYSTEM),Darwin)
        # XXX assume that all MacOS systems are 64 bit. That's because
        # we only have 64 bit pardiso for MacOS, and Java has a 64 bit
        # configuration, regardless of whether we are booting the 32
        # or 64 bit version of snow leopard
	NATIVE_DIR = MacOS64
	JAVA_INC=/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
	CC_INCS = -I$(JAVA_INC) -I$(TETGEN_DIR)
	X_FLAGS = -fPIC -DDARWIN -DAMD_64
	CC_FLAGS += -fpermissive
	TETGEN_TARGET = libTetgenJNI.$(TETGEN_VERSION).dylib
	ROBUST_TARGET = libRobustPreds.1.1.dylib
	ROBUST_TARGET_OLD = libRobustPreds.0.0.dylib
	LIB_FLAGS = -bundle -framework JavaVM
endif

LIB_TARGET_DIR = $(ROOT_DIR)/lib/$(NATIVE_DIR)

C_FLAGS += $(X_FLAGS)
CC_FLAGS += $(X_FLAGS)

JNI_H_FILES = \
	maspack_geometry_TetgenTessellator.h \
	maspack_geometry_RobustPreds.h

maspack_geometry_TetgenTessellator.h: $(CLASS_DIR)/TetgenTessellator.class
	$(JAVAH) $(JAVAHFLAGS) -classpath $(CLASS_ROOT_DIR) maspack.geometry.TetgenTessellator

maspack_geometry_RobustPreds.h: $(CLASS_DIR)/RobustPreds.class
	$(JAVAH) $(JAVAHFLAGS) -classpath .:$(CLASS_ROOT_DIR) maspack.geometry.RobustPreds

# these are the predicates used by Tetgen
# use -O0 to make sure optimbgization is disabled for predicates.o
# aho: tetgen replies that it works just fine with optimization, 
#      consider removing the -O0
# $(TETGEN_DIR)/predicates.o: $(TETGEN_DIR)/predicates.cxx
# 	$(CC_COMP) $(CC_FLAGS) -O0 -o $@ -c $<

# tetgen.o and predicates.o
$(TETGEN_DIR)/%.o: $(TETGEN_DIR)/%.cxx
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -o $@ -c $<

tetgenJNI.o: tetgenJNI.cc $(JNI_H_FILES)
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -o $@ -c $<

tetgenTest: tetgenTest.cc tetgenJNI.o $(TETGEN_OBJ_FILES)
	$(CC_COMP) $(CC_FLAGS) $(CC_INCS) -static-libgcc  -o $@ $^ -lm

$(LIB_TARGET_DIR)/$(TETGEN_TARGET): tetgenJNI.o $(TETGEN_OBJ_FILES)
	$(CC_COMP) $(CC_FLAGS) $(LIB_FLAGS) -o $@ $^ -lm

# I am not using the optimization 0 because it is twice as slow
# I wonder why they do it for predicates.cxx?
predicatesC.o: predicatesC.c predicatesC.h
	$(C_COMP) $(C_FLAGS) $(CC_INCS) $(SHEWCHUK_DEF) -c predicatesC.c

geoPredShewchuck.o: geoPredShewchuck.c predicatesC.h 
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -c $^

exact.o: exact.c exactArithmetic.h exact.h floatStatus.h
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -c $^

geoPredAJL.o: geoPredAJL.c exactArithmetic.h exact.h floatStatus.h
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -c $^

RobustPredsJNI.o: RobustPredsJNI.c $(JNI_H_FILES)
	$(C_COMP) $(C_FLAGS) $(CC_INCS) $(SHEWCHUK_DEF) -c RobustPredsJNI.c

geoPred.o: geoPred.c geoPred.h ahoVectorMacro.h
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -c $^

geoPredTest: geoPred.o geoPredTest.c
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -o $@ $^ -lm

ajlTimeTest: geoPred.o ahoTimeTest.c geoPredAJL.o exact.o
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -o $@ $^ -lm

shewchuckTimeTest: geoPred.o ahoTimeTest.c geoPredShewchuck.o predicatesC.o
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -o $@ $^ -lm

vector.o: vecmath/vector.c vecmath/vector.h 
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -c $^ 
matrix.o: vecmath/matrix.c vecmath/matrix.h
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -c $^

AJLConsistencyTest: geoPred.o ahoConsistentTest.c geoPredAJL.o exact.o vector.o matrix.o
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -o $@ $^ -lm

shewchukConsistencyTest: geoPred.o ahoConsistentTest.c geoPredShewchuck.o predicatesC.o vector.o matrix.o
	$(C_COMP) $(C_FLAGS) $(CC_INCS) -o $@ $^ -lm

#getCWJNI.o: getCWJNI.c $(JNI_H_FILES)
#	$(C_COMP) $(C_FLAGS) $(CC_INCS) -c $^

# $(LIB_TARGET_DIR)/$(ROBUST_TARGET_OLD): RobustPredsJNI.o geoPred.o geoPredAJL.o exact.o
#	$(C_COMP) $(C_FLAGS) $(LIB_FLAGS) -o $@ $^ -lm

$(LIB_TARGET_DIR)/$(ROBUST_TARGET): RobustPredsJNI.o geoPred.o geoPredShewchuck.o predicatesC.o
	$(C_COMP) $(C_FLAGS) $(LIB_FLAGS) -o $@ $^ -lm
#$(LIB_TARGET_DIR)/$(GETCW_TARGET): getCWJNI.o
#	$(C_COMP) $(C_FLAGS) $(LIB_FLAGS) -o $@ $^ -lm

LIB_ROBUST_TARGET = $(LIB_TARGET_DIR)/$(ROBUST_TARGET)
LIB_TETGEN_TARGET = $(LIB_TARGET_DIR)/$(TETGEN_TARGET)

LIB_TARGETS = $(LIB_ROBUST_TARGET) $(LIB_TETGEN_TARGET)

libs:	$(LIB_TARGETS)
#$(LIB_TARGET_DIR)/$(GETCW_TARGET)

install: $(LIB_TARGETS)
	rsync -Ctrlgo $(LIB_TARGETS) $(WEB_LIB_URL)/$(NATIVE_DIR)
	$(MAKE_HASHES_AND_FIX_PERMISSIONS)

depend:	$(JNI_H_FILES)

info:
	@echo SYSTEM: $(SYSTEM)
	@echo ARCH: $(ARCH)
	@echo TETGEN_TARGET: $(TETGEN_TARGET)

clean CLEAN::
	$(call RM_CALL,$(TETGEN_OBJ_FILES))


