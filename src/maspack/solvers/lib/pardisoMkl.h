/* -------------------------------------------------------------------- */
/* Header file for artisynth_sparse_solver.c				*/
/* -------------------------------------------------------------------- */
/* Author:(C) Chad Decker, EECE, UBC					*/
/* Last Modified by Chad Decker on Jan 10th 2006			*/
/* -------------------------------------------------------------------- */

/* PARDISO prototype. */

#if defined(WINDOWS_COMPILER)

#include "mkl.h"
#define DLLEXPORT 
#define PARDISO pardiso

#else 

#if defined(_WIN32) || defined(_WIN64)
#define PARDISO pardiso
#define DLLEXPORT __declspec(dllexport) 
#else
#define PARDISO pardiso_
#define DLLEXPORT 
#endif

#if defined(MKL_ILP64)
#define MKL_INT long long
#else
#define MKL_INT int
#endif

extern "C" {
   extern MKL_INT PARDISO
	(void *, MKL_INT *, MKL_INT *, MKL_INT *, MKL_INT *, MKL_INT *,
	double *, MKL_INT *, MKL_INT *, MKL_INT *, MKL_INT *, MKL_INT *,
	MKL_INT *, double *, double *, MKL_INT *);

   extern void pardisoinit (void *, MKL_INT *, MKL_INT *);

}

#endif

#define ERR_INCONSISTENT_INPUT      -1
#define ERR_MEMORY                  -2
#define ERR_REORDERING              -3
#define ERR_NUMERICAL               -4
#define ERR_INTERNAL                -5
#define ERR_PREORDERING             -6
#define ERR_DIAGONAL                -7
#define ERR_INT_OVERFLOW            -8
#define ERR_NO_LICENCE             -10
#define ERR_LICENCE_EXPIRED        -11
#define ERR_WRONG_USERHOST         -12
#define ERR_ITERATION_LIMIT       -100
#define ERR_ITERATION_CONVERGENCE -101
#define ERR_ITERATION_ERROR       -102
#define ERR_ITERATION_BREAKDOWN   -103

//  pt	    integer array  
//          contains internal address pointers; used to organize internal 
//          memory management
//  maxfct  (or &maxfct) integer  
//          defines the number of factors with identical sparsity structures 
//          to be retained in memory at once (generally set to '1')
//  mnum    (or &mnum)	integer	 
//          scalar value that defines the actual matrix for the solution phase 
//          (generally set to '1')
//  mtype   (or &mtype)	 integer  
//          scalar value that defines the matrix type (e.g., real versus 
//          complex, symmetric versus unsymmetric)
//  phase   (or &phase)	 integer  
//          indicates the starting and ending phase of solver execution, which 
//          controls the execution steps undertaken by the solver
//  n	    (or &n)  integer  
//          defines the number of equations (or rows) in the linear system
//  a	    real/complex array	
//          contains the nonzero values of the array
//  ia	    integer array  
//          contains the column indexes of elements in array a that contain 
//          non-zero elements from row i of the coefficient matrix of the 
//          linear system
//  ja	    integer array  
//          contains the column indexes of the coefficient matrix of the 
//          linear system in compressed sparse-row format, sorted in 
//          ascending order
//  perm    integer array  
//          contains the permutation vector
//  nrhs    (or &nrhs)	integer	 
//          defines the number of right-hand sides that must be solved for
//  iparm   integer array  
//          contains a series of control parameters for the PARDISO solver; 
//          notably, setting iparm(1)=0 causes all other elements of the array 
//          to set to their default values, except iparm(3), which identifies 
//          the number of processors that are available for execution, and 
//          which has no default value.
//  msglvl  (or &msglvl) integer  
//          defines whether the solver outputs statistical message information
//  b       real/complex array  
//          on entry, contains the right-hand side vector/matrix B; 
//          on output, contains the solution if iparm(6)=1 (the default value 
//          for iparm(6)=0)
//  x       real/complex array
//          on output, contains the solution if iparm(6)=0 
//  error   (or &error)  integer	
//          identifies error conditions (0=no error)
//  dparam  floating point parameters (mainly for AMG iterative solver)

class DLLEXPORT Pardiso4 
{
  private:

	double *myVals;   // non-zero matrix values

	int mySize;	   // matrix size
	int myMaxSize;     // maximum matrix size
        int myInitError;   // error (if any) incurred during initialization

	int myNumVals;	   // number of non-zero elements
	int myMaxNumVals; // maximum number of non-zero elements

	int *myRows;	   // row start indices
	int *myCols;	   // column indices for each element

        int myMessageLevel;

#define PARDISO_DIRECT 0
#define PARDISO_MULTI_LEVEL 1
        int mySolverType;

        int myLastPhase;

#define REAL_SYMMETRIC -2
#define REAL_SYMMETRIC_POSDEF 2	      
#define REAL_SYMMETRIC_INDEF -2
#define REAL_UNSYMMETRIC 11
	int myMatrixType;		// 
	int myNumRightHandSides;
#define NUM_INTERNAL_PTRS 64
	void *myInternalStore[NUM_INTERNAL_PTRS];
#define NUM_PARAMS 64
	int myIParams[NUM_PARAMS];
	int myMaxFact;
	int myFactorization;

	int myNumThreads;

	int myNumNonZerosInFactors; // nnz for most recent factor
	int myNumNegEigenvalues; // num negative eigenvalues for sym indefinite
	int myNumPosEigenvalues; // num positive eigenvalues for sym indefinite
	int myNumPerturbedPivots;

	int mySPDZeroPivot;
	int myPeakAnalysisMemoryUsage;
	int myAnalysisMemoryUsage;
	int myFactorSolveMemoryUsage;

	// Pardiso control parameters. All of these should be set to -1
	// to indicate a default value.

#define AMD_REORDER              0
#define METIS_REORDER            2
#define METIS_REORDER_PARALLEL   3
	int myReorderMethod;      // default: METIS_REORDER_PARALLEL
        int myMaxRefinementSteps; // default: 2 for regular, 1 for symmetric 
        int myNumRefinementSteps; // read back after solve
	int myPivotPerturbation;  // default: 8 for sym indefinite, 13 otherwise
	int myApplyScaling;       // default: 1 (apply)
	int myApplyWeightedMatchings;  // default: 1 (apply)
	int myUse2x2Pivoting;     // default: 1 (apply)
	int myMatrixChecking;
	int muNumThreads;

	// store explicit user-supplied values (if any) for quantities
	// whose default values depend on the matrix type:
	int myExplicitPivotPerturbation;
	int myExplicitMaxRefinementSteps;
	int myExplicitApplyScaling;
	int myExplicitApplyWeightedMatchings;
	int myExplicitUse2x2Pivoting;
	int myExplicitReorderMethod;
	int myExplicitNumThreads;

  protected:
	/* int getActualMaxRefinementSteps(); */
	/* int getActualReorderMethod(); */
	/* int getActualPivotPerturbation(); */
	/* int getActualApplyScaling(); */
	/* int getActualApplyWeightedMatchings(); */
	/* int getActualUse2x2Pivoting(); */

  public:
	Pardiso4();
	~Pardiso4();

	int getInitError();

	int getMaxRefinementSteps();
	int setMaxRefinementSteps(int nsteps);
	int getNumRefinementSteps();

	int setReorderMethod (int method);
	int getReorderMethod();

	int setPivotPerturbation (int perturb);
	int getPivotPerturbation ();

	int setApplyScaling (int apply);
	int getApplyScaling ();

	int setApplyWeightedMatchings (int apply);
	int getApplyWeightedMatchings ();

	int setUse2x2Pivoting (int enable);
	int getUse2x2Pivoting ();

	int setMatrixChecking (int enable);
	int getMatrixChecking ();

	int setMessageLevel (int level);
	int getMessageLevel ();

        // not supported since MKL doesn't allow per-instance thread
        // setting for Pardiso
	//int setNumThreads (int n);
	//int getNumThreads ();

	int getNumNonZerosInFactors();
	int getNumNegEigenvalues();
	int getNumPosEigenvalues();
	int getNumPerturbedPivots();

	int getSPDZeroPivot();
	int getPeakAnalysisMemoryUsage();
	int getAnalysisMemoryUsage();
	int getFactorSolveMemoryUsage();

	int setMatrix (const double* vals, const int* rows, const int *cols,
			int n, int nVals, int type);

        int releaseMatrix ();

	int factorMatrix ();
	int factorMatrix (const double* vals);

	int solveMatrix (double *x, double* b);
	int iterativeSolve (
           const double *vals, double *x, double* b, int tolExp);

	int factorAndSolve (
           const double* vals, double *x, double* b, int tolExp);

	int getSize();
	int getNumVals();

	void setSize (int size);
	void setNumVals (int num);

	// buffers for solution and RHS. 
	double *myX;
	double *myB;
};

#define null  0
#define false 0
#define true 1


