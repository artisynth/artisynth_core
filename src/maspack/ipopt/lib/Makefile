# C-Files
ROOT_DIR = ../../../..

default: libs

# C-Files compile
LIB_TARGET_DIR = $(ROOT_DIR)/lib/Linux
IPOPT_HOME = $(HOME)/ipopt
LIB_IPOPT = $(IPOPT_HOME)/build/lib

CC_INCS = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(IPOPT_HOME)/Ipopt/src/Interfaces -I.

CC_FLAGS = -g -fno-strict-aliasing -fPIC -pthread -W -Wall -Wno-unused -Wno-parentheses
LDS = -L$(LIB_TARGET_DIR) -L$(LIB_IPOPT) -lipopt -lpardiso_GNU_IA32 -L/usr/lib -llapack -lblas -lm -lstdc++
CLINKFLAGS =  -Wl,--rpath -Wl,$(LIB_IPOPT)

JNI_H_FILES = \
	maspack_ipopt_IpoptInterface.h

maspack_ipopt_IpoptInterface.h: ../IpoptInterface.java
	$(JAVAH) $(JAVAHFLAGS) maspack.ipopt.IpoptInterface

IpoptJNI.o: maspack_ipopt_IpoptInterface.h IpoptJNI.c
	g++ $(CC_FLAGS) $(CC_INCS) $(CLINKFLAGS) -c -o IpoptJNI.o IpoptJNI.c
	
$(LIB_TARGET_DIR)/libIpoptInterface.so: IpoptJNI.o
	g++  -shared $(CC_FLAGS) -static-libgcc -o $@ IpoptJNI.o $(LDS) 

libs:	$(LIB_TARGET_DIR)/libIpoptInterface.so

depend:	$(JNI_H_FILES)

localclean:
	rm -rf maspack_ipopt_IpoptInterface.h $(LIB_TARGET_DIR)/libIpoptInterface.so

-include $(ROOT_DIR)/Makefile.base


