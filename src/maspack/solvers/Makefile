ROOT_DIR = ../../..
PACKAGE_NAME = maspack.solvers

PACKAGE_JAVA_FILES = 

JAVA_TEST_PROGRAMS = \
	KKTSolverTest \
	KellerLCPSolverTest \
	LemkeContactSolverTest \
	DantzigLCPSolverTest \
	DantzigQPSolverTest \
	PardisoSolverTest \
	MurtyLCPSolverTest \
	CGSolverTest \
	TriDiagonalSolverTest
        # MurtySparseContactSolverTest \
        # CRSolverTest    CRSolver seems to not work
        # DantzigQPSolverTest

OTHER_CLASSES = 

default: build

CLASSES:: $(PACKAGE_CLASSES) $(OTHER_CLASSES)

-include $(ROOT_DIR)/Makefile.base

