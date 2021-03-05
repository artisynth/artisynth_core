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

ObjectSizeAgentJar: $(ROOT_DIR)/lib/ObjectSizeAgent.jar

$(ROOT_DIR)/lib/ObjectSizeAgent.jar: ObjectSizeAgent.class
	echo 'Premain-class: maspack.util.ObjectSizeAgent' > MANIFEST.MF
	jar cmf MANIFEST.MF $@ $<
	rm -f MANIFEST.MF

-include $(ROOT_DIR)/Makefile.base
