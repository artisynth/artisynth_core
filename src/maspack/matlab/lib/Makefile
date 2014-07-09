# C-Files
ROOT_DIR = ../../../..

default: libs

# C-Files compile

MATLAB_HOME = /usr/local/Matlab

LIB_TARGET_DIR = $(ROOT_DIR)/lib/Linux

CC_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(MATLAB_HOME)/extern/include -I.

CC_FLAGS = -g -fno-strict-aliasing -fPIC -pthread -W -Wall -Wno-unused -Wno-parentheses
LDS = -L$(LIB_TARGET_DIR) -L$(MATLAB_HOME)/bin/glnx86 -leng -lmx -lm

JNI_H_FILES = \
	maspack_matlab_MatlabInterface.h

maspack_matlab_MatlabInterface.h: ../MatlabInterface.java
	$(JAVAH) $(JAVAHFLAGS) maspack.matlab.MatlabInterface

MatlabJNI.o: maspack_matlab_MatlabInterface.h MatlabJNI.cc
	g++ $(CC_FLAGS) $(CC_INCS) -c -o MatlabJNI.o MatlabJNI.cc

$(LIB_TARGET_DIR)/libMatlabInterface.so: MatlabJNI.o
	g++  -shared $(CC_FLAGS) -static-libgcc -o $@ MatlabJNI.o $(LDS) 

libs:	$(LIB_TARGET_DIR)/libMatlabInterface.so

depend:	$(JNI_H_FILES)

localclean:
	rm -rf maspack_matlab_MatlabInterface.h $(LIB_TARGET_DIR)/libMatlabInterface.so

-include $(ROOT_DIR)/Makefile.base


