// C++ warpper class for the Pardiso matrix solve.
// Authors: John E. Lloyd, Chad Decker, UBC 2006
// -------------------------------------------------------------------- 

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>
#include "pardisoMkl.h"

#define debug 0
#define stats 0
#define sparseopt 0 // should be left as zero

/* If spd is turned on, so must sym */
#define spd   0 /*  Symmetric positive definite */
#define sym   0 /*  Matrix is Symmetric */
/*  If both are set to zero, then the matrix is assumed to be unsymmetric */

#define ffactor 40  /* Represents the expected sparsity of the matrix*/

extern "C" {
  void kmp_set_warnings_off (void);
}

Pardiso4::Pardiso4 ()
{
//#ifdef DARWIN
//   int stat = putenv("OMP_NUM_THREADS=1");
//#endif
   int i;

   myVals = (double*)0;
   mySize = 0;
   myMaxSize = 0;	
   myMessageLevel = 0;
   mySolverType = PARDISO_DIRECT;
   myLastPhase = 0;
//        mySolverType = PARDISO_MULTI_LEVEL;

   myNumVals = 0;
   myMaxNumVals = 0;

   myExplicitPivotPerturbation = -1;
   myExplicitMaxRefinementSteps = -1;
   myExplicitApplyScaling = -1;
   myExplicitApplyWeightedMatchings = -1;
   myExplicitUse2x2Pivoting = -1;
   myExplicitReorderMethod = -1;

   // -1 ensures default value will be set later
   myPivotPerturbation = -1;
   myMaxRefinementSteps = -1;
   myApplyScaling = -1;
   myApplyWeightedMatchings = -1;
   myUse2x2Pivoting = -1;
   myReorderMethod = -1;

   myMaxFact = 1;
   myFactorization = 1;
   myNumRefinementSteps = 0;
   myMatrixChecking = 0;

   myNumNonZerosInFactors = 0;
   myNumNegEigenvalues = 0;
   myNumPosEigenvalues = 0;
   myNumPerturbedPivots = 0;

   mySPDZeroPivot = 0;
   myPeakAnalysisMemoryUsage = 0;
   myAnalysisMemoryUsage = 0;
   myFactorSolveMemoryUsage = 0;

   myRows = (int*)0;
   myCols = (int*)0;

   myMatrixType = REAL_UNSYMMETRIC;
   myNumRightHandSides = 1;
   for (i=0; i<NUM_INTERNAL_PTRS; i++)
    { myInternalStore[i] = 0; 
    }
   for (i=0; i<64; i++) {
     myIParams[i] = 0;
   }
   int type = REAL_SYMMETRIC; // assume this by default
   pardisoinit (myInternalStore, &type, myIParams);
   // after calling init, we go ahead and set a bunch of the control
   // parameters explicity anyway ...
   myIParams[0] = 1; /* Don't use solver default values */
   myIParams[1] = getReorderMethod(); /* Fill-in reordering method */
   /* Numbers of processors; if 0, defaults to max number or MKL_NUM_THREADS */
   myIParams[2] = 0;
   myIParams[3] = 0; /* No iterative-direct algorithm */
   myIParams[4] = 0; /* No user fill-in reducing permutation */
   myIParams[5] = 0; /* Write solution into x */
   myIParams[6] = 0; /* Output: num refinement steps */
   myIParams[7] = getMaxRefinementSteps();
   myIParams[8] = 0; /* Not in use */
   myIParams[9] = getPivotPerturbation(); 
   myIParams[10] = getApplyScaling();
   myIParams[11] = 0; /* Not in use */
   myIParams[12] = getApplyWeightedMatchings();
   myIParams[13] = 0; /* Output: Number of perturbed pivots */
   myIParams[14] = 0; /* Not in use */
   myIParams[15] = 0; /* Not in use */
   myIParams[16] = 0; /* Not in use */
   myIParams[17] = -1; /* Output: Number of nonzeros in the factor LU */
   myIParams[18] = -1; /* Output: Mflops for LU factorization */
   myIParams[19] = 0; /* Output: Numbers of CG Iterations */
   myIParams[20] = getUse2x2Pivoting();

   int solver = 0;
   myInitError = 0;
   // this doesn't specifiy number of threads in Intel Pardiso
   //myIParams[2] = getNumThreads(); 

   myX = (double*)0;
   myB = (double*)0;

   // suppress warning messages, because of use of deprecated methods
   // omp_set/get_nested() by MKL 2020.
   kmp_set_warnings_off();
}

Pardiso4::~Pardiso4()
{
   releaseMatrix();
   if (myMaxSize > 0)
    { delete[] myRows;
      if (myX != (double*)0)
       { delete[] myX;
         myX = (double*)0;
       }
      if (myB != (double*)0)
       { delete[] myB;
         myB = (double*)0;
       }
      myMaxSize = 0;
    }
   if (myMaxNumVals > 0)
    { delete[] myCols;
      delete[] myVals;
      myMaxNumVals = 0;
    }
}

int Pardiso4::getInitError() {
   return myInitError;
}

int Pardiso4::getNumNonZerosInFactors () {
   return myNumNonZerosInFactors;
}

int Pardiso4::getNumNegEigenvalues () {
   return myNumNegEigenvalues;
}

int Pardiso4::getNumPosEigenvalues () {
   return myNumPosEigenvalues;
}

int Pardiso4::getNumPerturbedPivots () {
   return myNumPerturbedPivots;
}

int Pardiso4::getSPDZeroPivot () {
   return mySPDZeroPivot;
}

int Pardiso4::getPeakAnalysisMemoryUsage () {
   return myPeakAnalysisMemoryUsage;
}

int Pardiso4::getAnalysisMemoryUsage () {
   return myAnalysisMemoryUsage;
}

int Pardiso4::getFactorSolveMemoryUsage () {
   return myFactorSolveMemoryUsage;
}

int Pardiso4::setMaxRefinementSteps (int nsteps) {
   int prev = myMaxRefinementSteps;
   myExplicitMaxRefinementSteps = nsteps;
   myMaxRefinementSteps = nsteps;
   return prev;
}

int Pardiso4::getMaxRefinementSteps () {
   if (myMaxRefinementSteps < 0) {
     if (myExplicitMaxRefinementSteps >= 0) {
       myMaxRefinementSteps = myExplicitMaxRefinementSteps;
     }
     else {
       // set default value
       if (myMatrixType == REAL_UNSYMMETRIC) {
	 myMaxRefinementSteps = 2;
       }
       else if (myMatrixType == REAL_SYMMETRIC) {
	 myMaxRefinementSteps = 1;
       }
       else {
	 myMaxRefinementSteps = 1;
       }       
     }
   }
   return myMaxRefinementSteps;
}

int Pardiso4::getNumRefinementSteps () {
   return myNumRefinementSteps;
}

// int Pardiso4::getActualMaxRefinementSteps() {
//    if (myMaxRefinementSteps >= 0) {
//      return myMaxRefinementSteps;
//    }
//    else if (myMatrixType == REAL_UNSYMMETRIC) {
//      return 2;
//    }
//    else if (myMatrixType == REAL_SYMMETRIC) {
//      return 1;
//    }
//    else {
//      return 1;
//    }
// }

int Pardiso4::setReorderMethod (int method) {
   int prev = myReorderMethod;
   myExplicitReorderMethod = method;
   myReorderMethod = method;
   return prev;
}

int Pardiso4::getReorderMethod () {
   if (myReorderMethod < 0) {
     if (myExplicitReorderMethod >= 0) {
       myReorderMethod = myExplicitReorderMethod;
     }
     else {
       // set default value
       myReorderMethod = METIS_REORDER_PARALLEL;
     }
   }
   return myReorderMethod;
}

// int Pardiso4::getActualReorderMethod() {
//    if (myReorderMethod >= 0) {
//      return myReorderMethod;
//    }
//    else {
//      return METIS_REORDER_PARALLEL;
//    }
// }

int Pardiso4::setPivotPerturbation (int perturb) {
   int prev = myPivotPerturbation;
   myExplicitPivotPerturbation = perturb;
   myPivotPerturbation = perturb;
   return prev;
}

int Pardiso4::getPivotPerturbation () {
   if (myPivotPerturbation < 0) {
     if (myExplicitPivotPerturbation >= 0) {
       myPivotPerturbation = myExplicitPivotPerturbation;
     }
     else {
       // set default value
       if (myMatrixType == REAL_SYMMETRIC) {
	 myPivotPerturbation = 8;
       }
       else {
	 myPivotPerturbation = 13;
       }
     }
   }
   return myPivotPerturbation;
}

// int Pardiso4::getActualPivotPerturbation() {
//    if (myPivotPerturbation >= 0) {
//      return myPivotPerturbation;
//    }
//    else if (myMatrixType == REAL_SYMMETRIC) {
//      return 8;
//    }
//    else {
//      return 13;
//    }
// }

int Pardiso4::setApplyScaling (int apply) {
   int prev = myApplyScaling;
   myExplicitApplyScaling = apply;
   myApplyScaling = apply;
   return prev;
}

int Pardiso4::getApplyScaling () {
   if (myApplyScaling < 0) {
     if (myExplicitApplyScaling >= 0) {
       myApplyScaling = myExplicitApplyScaling;
     }
     else {
       // set default value
       if (myMatrixType == REAL_SYMMETRIC_POSDEF) {
	 myApplyScaling = 0;
       }
       else {
	 myApplyScaling = 1;
       }
     }
   }
   return myApplyScaling;
}

// int Pardiso4::getActualApplyScaling() {
//    if (myApplyScaling >= 0) {
//      return myApplyScaling;
//    }
//    else if (myMatrixType == REAL_SYMMETRIC_POSDEF) {
//      return 0;
//    }
//    else {
//      return 1;
//    }
// }

int Pardiso4::setApplyWeightedMatchings (int apply) {
   int prev = myApplyWeightedMatchings;
   myExplicitApplyWeightedMatchings = apply;
   myApplyWeightedMatchings = apply;
   return prev;
}

int Pardiso4::getApplyWeightedMatchings () {
   if (myApplyWeightedMatchings < 0) {
     if (myExplicitApplyWeightedMatchings >= 0) {
       myApplyWeightedMatchings = myExplicitApplyWeightedMatchings;
     }
     else {
       // set default value
       if (myMatrixType == REAL_SYMMETRIC_POSDEF) {
	 myApplyWeightedMatchings = 0;
       }
       else {
	 myApplyWeightedMatchings = 1;
       }
     }
   }   
   return myApplyWeightedMatchings;
}

// int Pardiso4::getActualApplyWeightedMatchings() {
//    if (myApplyWeightedMatchings >= 0) {
//      return myApplyWeightedMatchings;
//    }
//    else if (myMatrixType == REAL_SYMMETRIC_POSDEF) {
//      return 0;
//    }
//    else {
//      return 1;
//    }
// }

int Pardiso4::setUse2x2Pivoting (int enable) {
   int prev = myUse2x2Pivoting;
   myExplicitUse2x2Pivoting = enable;
   myUse2x2Pivoting = enable;
   return prev;
}

int Pardiso4::getUse2x2Pivoting () {
   if (myUse2x2Pivoting < 0) {
     if (myExplicitUse2x2Pivoting >= 0) {
       myUse2x2Pivoting = myExplicitUse2x2Pivoting;
     }
     else {
       // set default value
       if (myMatrixType == REAL_SYMMETRIC_POSDEF) {
	 myUse2x2Pivoting = 0;
       }
       else {
	 myUse2x2Pivoting = 1;
       }
     }
   }
   return myUse2x2Pivoting;
}

// int Pardiso4::getActualUse2x2Pivoting() {
//    if (myUse2x2Pivoting >= 0) {
//      return myUse2x2Pivoting;
//    }
//    else {
//      return 1;
//    }
// }

int Pardiso4::setMatrixChecking (int enable) {
   int prev = myMatrixChecking;
   myMatrixChecking = enable;
   return prev;
}

int Pardiso4::getMatrixChecking () {
   return myMatrixChecking;
}

int Pardiso4::setMessageLevel (int level) {
   int prev = myMessageLevel;
   myMessageLevel = level;
   return prev;
}

int Pardiso4::getMessageLevel () {
   return myMessageLevel;
}

// Not supported since MKL doesn't allow per-instance thread setting
// for Pardiso
//
// int Pardiso4::setNumThreads (int num) {
//    int prev = getNumThreads();
//    if (num < 0) {
//       num = 0;
//    }
//    mkl_domain_set_num_threads (num, MKL_DOMAIN_PARDISO);
//    return prev;
// }

// int Pardiso4::getNumThreads () {
//    return mkl_domain_get_max_threads (MKL_DOMAIN_PARDISO);
// }

int Pardiso4::setMatrix (
   const double* vals, const int* rowIdxs, const int *colIdxs, int size, int numVals, int type)
{
   int i;

   if (mySize > 0)
    { releaseMatrix(); 
    }
   if (myInitError != 0) 
    { // error occured on initialization
      return myInitError; 
    }

   setNumVals (numVals);
   setSize (size);

   // copy non-zero matrix values and column indices
   for (i=0; i<numVals; i++)
    { myVals[i] = vals[i];
      myCols[i] = colIdxs[i]; 
    }
   // copy row indices
   for (i=0; i<size; i++)
    { myRows[i] = rowIdxs[i];
    }
   myRows[size] = numVals+1;
   myMatrixType = type;

   // reset these because their default values may change
   myPivotPerturbation = -1;
   myMaxRefinementSteps = -1;
   myApplyScaling = -1;
   myApplyWeightedMatchings = -1;
   myUse2x2Pivoting = -1;

   int error = 0;
   int phase = 11;
   int idummy;
   double ddummy;

   myIParams[1] = getReorderMethod();
   myIParams[9] = getPivotPerturbation();
   myIParams[10] = getApplyScaling();
   myIParams[12] = getApplyWeightedMatchings();
   myIParams[20] = getUse2x2Pivoting();
   myIParams[26] = getMatrixChecking();
   myIParams[14] = 0; // output: peak memory
   myIParams[15] = 0; // output: memory

   myIParams[3] = 0; /* no iterative solving (paranoid, setting anyway) */
   // myIParams[17] = -1; /* return number of nonzeros in factors */
   for (i=0; i<NUM_INTERNAL_PTRS; i++)
    { myInternalStore[i] = 0; 
    }
   myIParams[17] = -1;
   PARDISO (myInternalStore, &myMaxFact, &myFactorization, &myMatrixType,
            &phase, &mySize, myVals, myRows, myCols, &idummy, 
            &myNumRightHandSides, myIParams, &myMessageLevel,
            &ddummy, &ddummy, &error);

   myPeakAnalysisMemoryUsage = myIParams[14];
   myAnalysisMemoryUsage = myIParams[15];

   if (error != 0)
    { printf ("\nPardiso: ERROR during symbolic factorization: %d\n", error); 
    }
   else
    { myLastPhase = phase; 
    }
   myNumNonZerosInFactors = myIParams[17];
   return error;
}

int Pardiso4::releaseMatrix ()
{
   int error = 0;
   if (mySize > 0)
    { int phase = -1;
      int nrhs = 0;
      mySize = 0;
      PARDISO (myInternalStore, &myMaxFact, &myFactorization, &myMatrixType,
               &phase, &mySize, null, null, null, null,
               &nrhs, myIParams, &myMessageLevel,
               null, null, &error);
      if (error != 0)
       { printf ("\nPardiso: ERROR during matrix release: %d\n", error); 
       }
    }
   return error;
}   

int Pardiso4::factorMatrix ()
{
   return factorMatrix((double*)0);
}

int Pardiso4::factorMatrix (const double* vals)
{
   int error;
   int phase = 22;
   int idummy;
   double ddummy;

   int i;
   // copy new values
   if (vals != (double*)0) {
     for (i=0; i<myNumVals; i++) {
       myVals[i] = vals[i]; 
     }
   }

   myIParams[3] = 0; /* no iterative solving (paranoid, setting anyway) */
   myIParams[9] = getPivotPerturbation();
   myIParams[20] = getUse2x2Pivoting();
   myIParams[26] = 0; // no matrix checking here
   myIParams[16] = 0; // output: memory usage
   myIParams[29] = 0; // output: SPD zero pivot

   //myIParams[17] = -1; /* return number of nonzeros in factors */
//   printf ("C-index=%d\n", myIParams[12]);
   PARDISO (myInternalStore, &myMaxFact, &myFactorization, &myMatrixType,
            &phase, &mySize, myVals, myRows, myCols, &idummy, 
            &myNumRightHandSides, myIParams, &myMessageLevel,
            &ddummy, &ddummy, &error);
   myNumPerturbedPivots = myIParams[13];
   // if (myIParams[13] > 0)
   //  { printf ("Pardiso: num perturbed pivots=%d\n", myIParams[13]);
   //  }
   if (error != 0)
    { printf ("\nPardiso: ERROR during numeric factorization: %d\n", error); 
      if (myMatrixType == REAL_SYMMETRIC_POSDEF) {
	mySPDZeroPivot = myIParams[29];
      }
    }
   else
    { myLastPhase = phase; 
      if (myMatrixType == REAL_SYMMETRIC) {
	myNumPosEigenvalues = myIParams[21];
	myNumNegEigenvalues = myIParams[22];
      }
      else {
	myNumPosEigenvalues = -1;
	myNumNegEigenvalues = -1;
      }
      myFactorSolveMemoryUsage = myIParams[16];
    }
   //myNumNonZerosInFactors = myIParams[17];
   return error;	
}

/* This method is not currently used ... */
int Pardiso4::factorAndSolve (
   const double* vals, double* x, double* b, int tolExp)
{
   int error;
   int phase = 23;
   int idummy;
   myIParams[7] = getMaxRefinementSteps();
   myIParams[3] = 0; /* default: no iterative solving */
   myIParams[9] = getPivotPerturbation();
   myIParams[20] = getUse2x2Pivoting();
   myIParams[26] = 0; // no matrix checking here
   myIParams[16] = 0; // output: memory usage
   myIParams[29] = 0; // output: SPD zero pivot

   int i;
   if (vals != (double*)0)
    { for (i=0; i<myNumVals; i++)
       { myVals[i] = vals[i]; 
       }
    }

   if (tolExp > 0)
    { if (myMatrixType == REAL_SYMMETRIC_POSDEF ||
          myMatrixType == REAL_SYMMETRIC)
       { myIParams[3] = 10*tolExp + 2; 
       }
      else
       { myIParams[3] = 10*tolExp + 1; 
       }
    }

   PARDISO (myInternalStore, &myMaxFact, &myFactorization, &myMatrixType,
            &phase, &mySize, myVals, myRows, myCols, &idummy, 
            &myNumRightHandSides, myIParams, &myMessageLevel,
            b, x, &error);
   myNumPerturbedPivots = myIParams[13];
   if (error != 0)
    { printf ("\nPardiso: ERROR during numeric factor and solve: %d\n", error); 
      if (myMatrixType == REAL_SYMMETRIC_POSDEF) {
	mySPDZeroPivot = myIParams[29];
      }
    }
   else
    { myLastPhase = phase; 
      if (myMatrixType == REAL_SYMMETRIC) {
	myNumPosEigenvalues = myIParams[21];
	myNumNegEigenvalues = myIParams[22];
      }
      else {
	myNumPosEigenvalues = -1;
	myNumNegEigenvalues = -1;
      }
      myNumRefinementSteps = myIParams[6];
      myFactorSolveMemoryUsage = myIParams[16];
    }
   return error;	
}

int Pardiso4::solveMatrix (double* x, double* b)
{
   int error;
   // phase = 23 will do a factor and a solve
   int phase = 33;
   int idummy;
   //double ddummy;
   myIParams[7] = getMaxRefinementSteps();
   myIParams[3] = 0; /* no iterative solving */
   myIParams[26] = 0; // no matrix checking here

   PARDISO (myInternalStore, &myMaxFact, &myFactorization, &myMatrixType,
            &phase, &mySize, myVals, myRows, myCols, &idummy, 
            &myNumRightHandSides, myIParams, &myMessageLevel,
            b, x, &error);

   if (error != 0)
    { printf ("\nPardiso: ERROR during solution: %d\n", error); 
    }
   else
    { myLastPhase = phase; 
      myNumRefinementSteps = myIParams[6];
    }
   return error;	
}

int Pardiso4::iterativeSolve (
   const double* vals, double* x, double* b, int tolExp)
{
   int error;
   // phase = 23 will do a direct factor/solve if iterative solve fails
   int phase = 33;
   int idummy;
   //double ddummy;
   myIParams[7] = getMaxRefinementSteps();
   myIParams[26] = 0; // no matrix checking here

   int i;
   if (vals != (double*)0)
    { for (i=0; i<myNumVals; i++)
       { myVals[i] = vals[i]; 
       }
    }

   // We used to negate myIParams[3] in order to force the solve to
   // NOT do any refactoring on its own. This was a nonstandard
   // feature described to us by Olaf Schenk. In current versions of
   // MKL pardiso, it seems that this has been supplanted by phase =
   // 33, and also that MKL uses abs(myIParams[3]), so that negating
   // has no effect.
   if (myMatrixType == REAL_SYMMETRIC_POSDEF ||
       myMatrixType == REAL_SYMMETRIC)
    { myIParams[3] = (10*tolExp + 2);
    }
   else
    { myIParams[3] = (10*tolExp + 1);
    }

   PARDISO (myInternalStore, &myMaxFact, &myFactorization, &myMatrixType,
            &phase, &mySize, myVals, myRows, myCols, &idummy, 
            &myNumRightHandSides, myIParams, &myMessageLevel,
            b, x, &error);

   if (error != 0) {
      return myIParams[19];
    }
   else 
    { myLastPhase = phase;
      myNumRefinementSteps = myIParams[6];
      return myIParams[19]; 
    }
}

void Pardiso4::setSize (int size)
{
   if (size > myMaxSize)
    { // need extra space for matrix size info at end
      if (myRows != (int*)0)
       { delete[] myRows;
       }
      if (myX != (double*)0)
       { delete[] myX;
       }
      if (myB != (double*)0)
       { delete[] myB;           
       }
      myRows = new int[size+1];
      myX = new double[size];
      myB = new double[size];
      myMaxSize = size;
    }
   mySize = size;
}

int Pardiso4::getSize()
{
   return mySize;
}

int Pardiso4::getNumVals()
{
   return myNumVals;
}

void Pardiso4::setNumVals (int num)
{
   if (num > myMaxNumVals)
    {
      if (myVals != (double*)0)
       { delete[] myVals;
       }
      if (myCols != (int*)0)
       { delete[] myCols;           
       }
      myVals = new double[num];
      myCols = new int[num];
      myMaxNumVals = num;
    }
   myNumVals = num;
}

#if 0
// This was added in an unsuccessful attempt to replace xerbla in
// Windows MKL and hence resolve undefined symbol errors in xerbla.obj
// when linking using mingw on Cygwin.
//
// The undefined symbols included security_check_cookie and __GSHandlerCheck.

void XERBLA(char* srname, int* info, int len){
// srname - name of the function that called xerbla
// info - position of the invalid parameter in the parameter list
// len - length of the name in bytes
   printf("\nXERBLA is called :%s: %d\n",srname,*info);
}

#endif

