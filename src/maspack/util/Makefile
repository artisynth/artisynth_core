ROOT_DIR = ../../..
JAVAC = javac
JAVA = java
JAVACFLAGS += source 1.4
PACKAGE_NAME = maspack.util

PACKAGE_JAVA_FILES = 

JAVA_TEST_PROGRAMS = \
	ArraySortTest \
	BinaryStreamTest \
	CubicSolverTest \
	DynamicArrayTest \
	ListRemoveTest \
	NumberFormatTest \
	NumericIntervalTest \
	ReaderTokenizerTest \
	RoundTest

        # QuarticSolverTest

OTHER_CLASSES = 

default: build

CLASSES:: $(PACKAGE_CLASSES)  $(OTHER_CLASSES)

cubicRoots.o: cubicRoots.cc
	$(CC) -O3 -c $^

cubicRoots: cubicRoots.o
	$(CC) $^ -o $@ -lm

-include $(ROOT_DIR)/Makefile.base
