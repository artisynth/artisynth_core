/* -------------------------------------------------------------------- */
/* C++ wrapper around the MUMPS sparse direct solver, providing the      */
/* functionality needed by maspack.solvers.MumpsSolver.                  */
/*                                                                       */
/* Structured to parallel pardisoMkl.h, so that the two solvers can be   */
/* maintained together. Differences forced by the MUMPS API:             */
/*                                                                       */
/*  - MUMPS uses an assembled coordinate format (irn/jcn/a) rather than   */
/*    CRS, so setMatrix() expands the row offsets into row indices. For   */
/*    symmetric matrices, MUMPS accepts either triangle, so the upper     */
/*    triangular CRS data supplied by ArtiSynth is used as-is.            */
/*                                                                       */
/*  - SYM must be set *before* the MUMPS instance is initialized          */
/*    (JOB=-1), and changing it requires terminating (JOB=-2) and         */
/*    reinitializing the instance. Since JOB=-1 also resets ICNTL/CNTL to */
/*    their defaults, all control settings are cached in this class and   */
/*    reapplied before each phase.                                        */
/*                                                                       */
/*  - MUMPS keeps pointers to irn/jcn/a, and reads them again during the  */
/*    factor and solve phases, so this class holds its own copies.        */
/*                                                                       */
/*  - The solve phase overwrites the right hand side with the solution,   */
/*    so an internal rhs buffer is used.                                  */
/* -------------------------------------------------------------------- */

#ifndef MUMPS_WRAPPER_H
#define MUMPS_WRAPPER_H

#include "dmumps_c.h"

// values for ICNTL(7), the fill reducing ordering method
#define MUMPS_AMD_REORDER     0
#define MUMPS_USER_REORDER    1
#define MUMPS_AMF_REORDER     2
#define MUMPS_SCOTCH_REORDER  3
#define MUMPS_PORD_REORDER    4
#define MUMPS_METIS_REORDER   5
#define MUMPS_QAMD_REORDER    6
#define MUMPS_AUTO_REORDER    7

// values for SYM
#define MUMPS_UNSYMMETRIC 0
#define MUMPS_SPD         1
#define MUMPS_SYMMETRIC   2

class Mumps {

  private:

	DMUMPS_STRUC_C myId;
	int myInstanceActive;    // true if JOB=-1 has been called
	int mySym;               // SYM value of the current instance
	int myInitError;         // error (if any) incurred during initialization

	int myLastInfo1;         // INFOG(1) from the most recent call
	int myLastInfo2;         // INFOG(2) from the most recent call

	int myVerbose;           // enables MUMPS diagnostic output

	// matrix in assembled coordinate format. MUMPS retains these pointers
	// across the analyze, factor and solve phases.
	int mySize;
	int myMaxSize;
	int myNumVals;
	int myMaxNumVals;
	MUMPS_INT *myIrn;
	MUMPS_INT *myJcn;
	double *myVals;

	double *myRhs;           // rhs/solution buffer, since MUMPS solves in place
	int myMaxRhsSize;

	// statistics, read back from INFOG after each phase
	long long myNumNonZerosInFactors;
	int myNumNegEigenvalues;
	int myNumPosEigenvalues;
	int myNumTinyPivots;
	int myNumNullPivots;
	int myNumDelayedPivots;
	int myFirstNullPivot;
	int myNumRefinementSteps;
	int myReorderMethodUsed;
	int myAnalysisMemoryUsage;
	int myPeakAnalysisMemoryUsage;
	int myFactorSolveMemoryUsage;

	// control settings, cached because JOB=-1 resets ICNTL and CNTL.
	// Those which are int valued use negative values to indicate that
	// the MUMPS default should be used.
	int myReorderMethod;         // ICNTL(7)
	int myApplyWeightedMatchings;// ICNTL(6)
	int myApplyScaling;          // ICNTL(8)
	int mySymOrderingStrategy;   // ICNTL(12)
	int myMaxRefinementSteps;    // ICNTL(10)
	int myWorkspaceIncrease;     // ICNTL(14)
	int myMaxWorkingMemory;      // ICNTL(23)
	int myNullPivotDetection;    // ICNTL(24)
	double myNullPivotThreshold; // CNTL(3)
	double myStaticPivotTolerance; // CNTL(4)

	void applySettings();
	int callMumps (int job);
	int initInstance (int sym);
	void terminateInstance();
	int allocateMatrix (int size, int numVals);
	int allocateRhs (int size);
	void clearStatistics();
	void getAnalysisStatistics();
	void getFactorStatistics();

  public:

	Mumps();
	~Mumps();

	int getInitError();
	int getLastErrorInfo();

	int setNumThreads (int num);
	int getNumThreads();

	int setMaxRefinementSteps (int nsteps);
	int getMaxRefinementSteps();
	int getNumRefinementSteps();

	int setReorderMethod (int method);
	int getReorderMethod();
	int getReorderMethodUsed();

	int setStaticPivotTolerance (double tol);
	double getStaticPivotTolerance();

	int setNullPivotDetection (int enable);
	int getNullPivotDetection();

	int setNullPivotThreshold (double thresh);
	double getNullPivotThreshold();

	int setApplyScaling (int enable);
	int getApplyScaling();

	int setApplyWeightedMatchings (int enable);
	int getApplyWeightedMatchings();

	int setSymOrderingStrategy (int strategy);
	int getSymOrderingStrategy();

	int setWorkspaceIncrease (int percent);
	int getWorkspaceIncrease();

	int setMaxWorkingMemory (int mbytes);
	int getMaxWorkingMemory();

	long long getNumNonZerosInFactors();
	int getNumNegEigenvalues();
	int getNumPosEigenvalues();
	int getNumTinyPivots();
	int getNumNullPivots();
	int getNumDelayedPivots();
	int getFirstNullPivot();

	int getAnalysisMemoryUsage();
	int getPeakAnalysisMemoryUsage();
	int getFactorSolveMemoryUsage();

	// Sets the matrix, in CRS format with 1-based indices, and performs the
	// analyze phase. sym is one of MUMPS_UNSYMMETRIC, MUMPS_SPD or
	// MUMPS_SYMMETRIC; for the latter two, only the upper (or lower)
	// triangle should be supplied.
	int setMatrix (
	   const double* vals, const int* rowOffs, const int* colIdxs,
	   int size, int numVals, int sym);

	int factorMatrix (const double* vals);

	int solveMatrix (double *x, const double* b);
	int solveMatrix (double *x, const double* b, int nrhs);

	int releaseMatrix();

	int getSize();
	int getNumVals();
};

#endif
