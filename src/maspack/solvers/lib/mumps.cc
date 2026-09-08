/* -------------------------------------------------------------------- */
/* C++ wrapper around the MUMPS sparse direct solver. See mumps.h for    */
/* an overview of how the MUMPS API differs from that of Pardiso.        */
/* -------------------------------------------------------------------- */

#include "mumps.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

// MUMPS job values
#define JOB_INIT     -1
#define JOB_END      -2
#define JOB_ANALYZE   1
#define JOB_FACTOR    2
#define JOB_SOLVE     3

// tells MUMPS to use MPI_COMM_WORLD (which, in the sequential build used
// here, is provided by the libmpiseq stub)
#define USE_COMM_WORLD -987654

// macros so that indices match those in the MUMPS documentation
#define ICNTL(I) icntl[(I)-1]
#define CNTL(I)  cntl[(I)-1]
#define INFO(I)  info[(I)-1]
#define INFOG(I) infog[(I)-1]

// maximum number of times a factorization is retried with a larger
// workspace after MUMPS reports that its internal workarrays are too small
#define MAX_FACTOR_RETRIES 4

// OpenMP and MKL thread control. Declared explicitly so that this file does
// not need to be compiled with -fopenmp, or to include the MKL headers.
// Note that the MKL routines must be referred to by their C names
// (MKL_Set_Num_Threads); the lower case names are the Fortran entry points,
// which take their argument by reference.
extern "C" {
   int omp_get_max_threads (void);
   void omp_set_num_threads (int num);
   int MKL_Get_Max_Threads (void);
   void MKL_Set_Num_Threads (int num);
}

Mumps::Mumps()
{
   myInstanceActive = 0;
   mySym = -1;
   myInitError = 0;
   myLastInfo1 = 0;
   myLastInfo2 = 0;
   myVerbose = (getenv ("MUMPS_JNI_VERBOSE") != NULL);

   mySize = 0;
   myMaxSize = 0;
   myNumVals = 0;
   myMaxNumVals = 0;
   myIrn = NULL;
   myJcn = NULL;
   myVals = NULL;

   myRhs = NULL;
   myMaxRhsSize = 0;

   // MUMPS default settings. Negative int values mean "leave at the MUMPS
   // default"; the exceptions are the settings whose default we deliberately
   // change, which are documented in MumpsSolver.java.
   myReorderMethod = MUMPS_AUTO_REORDER;
   myApplyWeightedMatchings = -1;
   myApplyScaling = -1;
   mySymOrderingStrategy = -1;
   myMaxRefinementSteps = 0;
   myWorkspaceIncrease = -1;
   myMaxWorkingMemory = 0;
   myNullPivotDetection = 1;      // enabled, unlike the MUMPS default
   myNullPivotThreshold = 0.0;    // automatic threshold
   myStaticPivotTolerance = -1.0; // static pivoting disabled

   clearStatistics();

   if (sizeof(MUMPS_INT) != sizeof(int)) {
      printf ("MumpsJNI: MUMPS_INT is not the same size as int; "
              "MUMPS must be built LP64\n");
      myInitError = -1;
   }
}

Mumps::~Mumps()
{
   releaseMatrix();
}

void Mumps::clearStatistics()
{
   myNumNonZerosInFactors = 0;
   myNumNegEigenvalues = -1;
   myNumPosEigenvalues = -1;
   myNumTinyPivots = 0;
   myNumNullPivots = 0;
   myNumDelayedPivots = 0;
   myFirstNullPivot = 0;
   myNumRefinementSteps = 0;
   myReorderMethodUsed = MUMPS_AUTO_REORDER;
   myAnalysisMemoryUsage = 0;
   myPeakAnalysisMemoryUsage = 0;
   myFactorSolveMemoryUsage = 0;
}

/**
 * Applies the cached control settings to the MUMPS instance. This must be
 * done after every JOB=-1 call, since that resets ICNTL and CNTL to their
 * defaults, and is also done before each phase so that settings changed
 * between phases take effect.
 */
void Mumps::applySettings()
{
   if (!myInstanceActive) {
      return;
   }
   // output streams: error, diagnostic, global, and print level
   if (myVerbose) {
      myId.ICNTL(1) = 6;
      myId.ICNTL(2) = 0;
      myId.ICNTL(3) = 6;
      myId.ICNTL(4) = 2;
   }
   else {
      myId.ICNTL(1) = -1;
      myId.ICNTL(2) = -1;
      myId.ICNTL(3) = -1;
      myId.ICNTL(4) = 0;
   }
   myId.ICNTL(7) = myReorderMethod;
   if (myApplyWeightedMatchings >= 0) {
      // 0 disables matching; 5 (maximize the product of the diagonal) is the
      // variant recommended for augmented/saddle point systems
      myId.ICNTL(6) = (myApplyWeightedMatchings > 0 ? 5 : 0);
   }
   if (myApplyScaling >= 0) {
      // 0 disables scaling; 77 lets MUMPS choose when scaling is enabled
      myId.ICNTL(8) = (myApplyScaling > 0 ? 77 : 0);
   }
   if (mySymOrderingStrategy >= 0) {
      myId.ICNTL(12) = mySymOrderingStrategy;
   }
   // process the root node sequentially, so that the inertia (INFOG(12)) is
   // exact and null pivots on the root are detected
   myId.ICNTL(13) = 1;
   myId.ICNTL(10) = myMaxRefinementSteps;
   if (myWorkspaceIncrease >= 0) {
      myId.ICNTL(14) = myWorkspaceIncrease;
   }
   if (myMaxWorkingMemory > 0) {
      myId.ICNTL(23) = myMaxWorkingMemory;
   }
   myId.ICNTL(24) = (myNullPivotDetection ? 1 : 0);
   myId.CNTL(3) = myNullPivotThreshold;
   myId.CNTL(4) = myStaticPivotTolerance;
}

/**
 * Calls MUMPS for the indicated job, and records INFOG(1) and INFOG(2).
 * Returns INFOG(1), which is 0 on success, positive for a warning, and
 * negative for an error.
 */
int Mumps::callMumps (int job)
{
   myId.job = job;
   dmumps_c (&myId);
   myLastInfo1 = myId.INFOG(1);
   myLastInfo2 = myId.INFOG(2);
   return myLastInfo1;
}

void Mumps::terminateInstance()
{
   if (myInstanceActive) {
      myId.job = JOB_END;
      dmumps_c (&myId);
      myInstanceActive = 0;
      mySym = -1;
   }
}

/**
 * Creates a MUMPS instance with the indicated symmetry. SYM can only be set
 * before JOB=-1, so an existing instance is always terminated first.
 */
int Mumps::initInstance (int sym)
{
   terminateInstance();

   memset (&myId, 0, sizeof(myId));
   myId.comm_fortran = USE_COMM_WORLD;
   myId.par = 1;     // the host takes part in the computation
   myId.sym = sym;
   myId.job = JOB_INIT;
   dmumps_c (&myId);
   myLastInfo1 = myId.INFOG(1);
   myLastInfo2 = myId.INFOG(2);
   if (myLastInfo1 < 0) {
      return myLastInfo1;
   }
   myInstanceActive = 1;
   mySym = sym;
   applySettings();
   return 0;
}

int Mumps::allocateMatrix (int size, int numVals)
{
   if (numVals > myMaxNumVals) {
      free (myIrn);
      free (myJcn);
      free (myVals);
      myIrn = (MUMPS_INT*)malloc (numVals*sizeof(MUMPS_INT));
      myJcn = (MUMPS_INT*)malloc (numVals*sizeof(MUMPS_INT));
      myVals = (double*)malloc (numVals*sizeof(double));
      if (myIrn == NULL || myJcn == NULL || myVals == NULL) {
         myMaxNumVals = 0;
         return -13;  // MUMPS code for an allocation problem
      }
      myMaxNumVals = numVals;
   }
   if (size > myMaxSize) {
      myMaxSize = size;
   }
   return 0;
}

int Mumps::allocateRhs (int size)
{
   if (size > myMaxRhsSize) {
      free (myRhs);
      myRhs = (double*)malloc (size*sizeof(double));
      if (myRhs == NULL) {
         myMaxRhsSize = 0;
         return -13;
      }
      myMaxRhsSize = size;
   }
   return 0;
}

int Mumps::setMatrix (
   const double* vals, const int* rowOffs, const int* colIdxs,
   int size, int numVals, int sym)
{
   int i, k;

   if (myInitError != 0) {
      return myInitError;
   }
   int err = allocateMatrix (size, numVals);
   if (err != 0) {
      myLastInfo1 = err;
      myLastInfo2 = numVals;
      return err;
   }
   // Convert CRS to the MUMPS assembled coordinate format. Both index arrays
   // are already 1-based, so only the row offsets need to be expanded. For
   // symmetric matrices, the (upper triangular) entries are used as-is, since
   // MUMPS accepts either triangle.
   for (i=0; i<size; i++) {
      int end = rowOffs[i+1]-1;
      for (k=rowOffs[i]-1; k<end; k++) {
         myIrn[k] = i+1;
         myJcn[k] = colIdxs[k];
      }
   }
   for (k=0; k<numVals; k++) {
      myVals[k] = vals[k];
   }
   mySize = size;
   myNumVals = numVals;
   clearStatistics();

   // a fresh instance is created for every analyze, both because SYM may have
   // changed and because this guarantees a clean analysis phase
   int rcode = initInstance (sym);
   if (rcode < 0) {
      mySize = 0;
      return rcode;
   }
   myId.n = size;
   myId.nnz = (MUMPS_INT8)numVals;
   myId.nz = numVals;   // for backward compatibility
   myId.irn = myIrn;
   myId.jcn = myJcn;
   myId.a = myVals;

   applySettings();
   rcode = callMumps (JOB_ANALYZE);
   if (rcode < 0) {
      mySize = 0;
      return rcode;
   }
   getAnalysisStatistics();
   return rcode;
}

void Mumps::getAnalysisStatistics()
{
   myReorderMethodUsed = myId.INFOG(7);
   // INFOG(20): estimated number of entries in the factors. Negative values
   // indicate millions of entries.
   int nz = myId.INFOG(20);
   myNumNonZerosInFactors =
      (nz < 0 ? -(long long)nz*1000000 : (long long)nz);
   // INFOG(16) and INFOG(17) are in Mbytes
   myPeakAnalysisMemoryUsage = myId.INFOG(16)*1024;
   myAnalysisMemoryUsage = myId.INFOG(17)*1024;
}

void Mumps::getFactorStatistics()
{
   myNumDelayedPivots = myId.INFOG(13);
   myNumTinyPivots = myId.INFOG(25);
   myNumNullPivots = myId.INFOG(28);
   if (mySym == MUMPS_SYMMETRIC || mySym == MUMPS_SPD) {
      // INFOG(12) gives the number of negative pivots for symmetric
      // matrices. Null pivots are excluded from this count, so the number of
      // positive pivots is what is left over.
      myNumNegEigenvalues = myId.INFOG(12);
      myNumPosEigenvalues =
         mySize - myNumNegEigenvalues - myNumNullPivots;
   }
   else {
      // for SYM=0, INFOG(12) counts off-diagonal pivots instead
      myNumNegEigenvalues = -1;
      myNumPosEigenvalues = -1;
   }
   myFirstNullPivot = 0;
   if (myNumNullPivots > 0 && myId.pivnul_list != NULL) {
      myFirstNullPivot = myId.pivnul_list[0];
   }
   // INFOG(29): actual number of entries in the factors, negative if in
   // millions
   int nz = myId.INFOG(29);
   myNumNonZerosInFactors =
      (nz < 0 ? -(long long)nz*1000000 : (long long)nz);
   myFactorSolveMemoryUsage = myId.INFOG(21)*1024;
}

/**
 * Returns true if a MUMPS error indicates that one of its internal
 * workarrays was too small, which can be fixed by increasing ICNTL(14) and
 * factoring again. Allocation failures (-13) and exceeding the user-supplied
 * memory limit (-19) are excluded, since increasing the workspace would only
 * make those worse.
 */
static int isWorkspaceError (int info1)
{
   switch (info1) {
      case -8: case -9: case -11: case -12:
      case -14: case -15: case -17: case -20: {
         return 1;
      }
      default: {
         return 0;
      }
   }
}

int Mumps::factorMatrix (const double* vals)
{
   int k;

   if (mySize == 0) {
      return -3;  // MUMPS code for calling a phase out of sequence
   }
   if (vals != NULL) {
      for (k=0; k<myNumVals; k++) {
         myVals[k] = vals[k];
      }
   }
   applySettings();
   int rcode = callMumps (JOB_FACTOR);

   // Unlike Pardiso, MUMPS preallocates its workspace from the estimates
   // made during the analyze phase, and numerical pivoting can cause this to
   // be exceeded. The remedy is to increase ICNTL(14) and factor again.
   int ntries = 0;
   while (isWorkspaceError (rcode) && ntries < MAX_FACTOR_RETRIES) {
      int increase = myId.ICNTL(14);
      increase = (increase <= 0 ? 60 : 2*increase);
      if (increase > 1000) {
         break;
      }
      if (myVerbose) {
         printf ("MumpsJNI: factorization error %d; "
                 "retrying with ICNTL(14)=%d\n", rcode, increase);
      }
      myId.ICNTL(14) = increase;
      rcode = callMumps (JOB_FACTOR);
      ntries++;
   }
   if (rcode >= 0) {
      getFactorStatistics();
   }
   return rcode;
}

int Mumps::solveMatrix (double *x, const double* b)
{
   return solveMatrix (x, b, 1);
}

int Mumps::solveMatrix (double *x, const double* b, int nrhs)
{
   int i;

   if (mySize == 0) {
      return -3;
   }
   int err = allocateRhs (mySize*nrhs);
   if (err != 0) {
      myLastInfo1 = err;
      myLastInfo2 = mySize*nrhs;
      return err;
   }
   // MUMPS solves in place, overwriting the right hand side
   for (i=0; i<mySize*nrhs; i++) {
      myRhs[i] = b[i];
   }
   myId.nrhs = nrhs;
   if (nrhs > 1) {
      myId.lrhs = mySize;
   }
   myId.rhs = myRhs;

   applySettings();
   int rcode = callMumps (JOB_SOLVE);
   myNumRefinementSteps = myId.INFOG(15);
   if (rcode >= 0) {
      for (i=0; i<mySize*nrhs; i++) {
         x[i] = myRhs[i];
      }
   }
   // reset nrhs, since MUMPS keeps it between calls
   myId.nrhs = 1;
   return rcode;
}

int Mumps::releaseMatrix()
{
   terminateInstance();
   free (myIrn);
   free (myJcn);
   free (myVals);
   free (myRhs);
   myIrn = NULL;
   myJcn = NULL;
   myVals = NULL;
   myRhs = NULL;
   myMaxNumVals = 0;
   myMaxRhsSize = 0;
   mySize = 0;
   myNumVals = 0;
   return 0;
}

int Mumps::getInitError()
{
   return myInitError;
}

int Mumps::getLastErrorInfo()
{
   return myLastInfo2;
}

/**
 * Sets the number of threads used by MUMPS. MUMPS itself is threaded with
 * OpenMP, while the BLAS operations which dominate the factorization are
 * threaded by MKL, so both are set here. A value <= 0 restores the default
 * OpenMP thread count.
 */
int Mumps::setNumThreads (int num)
{
   int prev = omp_get_max_threads();
   if (num > 0) {
      omp_set_num_threads (num);
      MKL_Set_Num_Threads (num);
   }
   else {
      // restore the OpenMP default, which is normally OMP_NUM_THREADS
      const char* env = getenv ("OMP_NUM_THREADS");
      int n = (env != NULL ? atoi(env) : 0);
      if (n <= 0) {
         n = omp_get_max_threads();
      }
      omp_set_num_threads (n);
      MKL_Set_Num_Threads (n);
   }
   return prev;
}

int Mumps::getNumThreads()
{
   return omp_get_max_threads();
}

int Mumps::setMaxRefinementSteps (int nsteps)
{
   myMaxRefinementSteps = nsteps;
   applySettings();
   return 0;
}

int Mumps::getMaxRefinementSteps()
{
   return myMaxRefinementSteps;
}

int Mumps::getNumRefinementSteps()
{
   return myNumRefinementSteps;
}

int Mumps::setReorderMethod (int method)
{
   if (method < 0 || method > MUMPS_AUTO_REORDER) {
      method = MUMPS_AUTO_REORDER;
   }
   myReorderMethod = method;
   applySettings();
   return 0;
}

int Mumps::getReorderMethod()
{
   return myReorderMethod;
}

int Mumps::getReorderMethodUsed()
{
   return myReorderMethodUsed;
}

int Mumps::setStaticPivotTolerance (double tol)
{
   myStaticPivotTolerance = tol;
   applySettings();
   return 0;
}

double Mumps::getStaticPivotTolerance()
{
   return myStaticPivotTolerance;
}

int Mumps::setNullPivotDetection (int enable)
{
   myNullPivotDetection = (enable != 0);
   applySettings();
   return 0;
}

int Mumps::getNullPivotDetection()
{
   return myNullPivotDetection;
}

int Mumps::setNullPivotThreshold (double thresh)
{
   myNullPivotThreshold = thresh;
   applySettings();
   return 0;
}

double Mumps::getNullPivotThreshold()
{
   return myNullPivotThreshold;
}

int Mumps::setApplyScaling (int enable)
{
   myApplyScaling = enable;
   applySettings();
   return 0;
}

/**
 * Returns whether scaling is enabled. If MUMPS chose automatically, the
 * effective value is read back from INFOG(33).
 */
int Mumps::getApplyScaling()
{
   if (myApplyScaling >= 0) {
      return (myApplyScaling > 0);
   }
   else if (myInstanceActive && myId.INFOG(33) != 0) {
      return (myId.INFOG(33) != 77 ? 1 : 0);
   }
   else {
      return 0;
   }
}

int Mumps::setApplyWeightedMatchings (int enable)
{
   myApplyWeightedMatchings = enable;
   applySettings();
   return 0;
}

/**
 * Returns whether weighted matchings are enabled. If MUMPS chose
 * automatically, the effective value is read back from INFOG(23).
 */
int Mumps::getApplyWeightedMatchings()
{
   if (myApplyWeightedMatchings >= 0) {
      return (myApplyWeightedMatchings > 0);
   }
   else if (myInstanceActive) {
      return (myId.INFOG(23) != 0);
   }
   else {
      return 0;
   }
}

int Mumps::setSymOrderingStrategy (int strategy)
{
   mySymOrderingStrategy = strategy;
   applySettings();
   return 0;
}

int Mumps::getSymOrderingStrategy()
{
   if (mySymOrderingStrategy >= 0) {
      return mySymOrderingStrategy;
   }
   else if (myInstanceActive) {
      return myId.INFOG(24);  // value of ICNTL(12) effectively used
   }
   else {
      return 0;
   }
}

int Mumps::setWorkspaceIncrease (int percent)
{
   myWorkspaceIncrease = percent;
   applySettings();
   return 0;
}

int Mumps::getWorkspaceIncrease()
{
   if (myInstanceActive) {
      return myId.ICNTL(14);
   }
   else {
      return myWorkspaceIncrease;
   }
}

int Mumps::setMaxWorkingMemory (int mbytes)
{
   myMaxWorkingMemory = mbytes;
   applySettings();
   return 0;
}

int Mumps::getMaxWorkingMemory()
{
   return myMaxWorkingMemory;
}

long long Mumps::getNumNonZerosInFactors()
{
   return myNumNonZerosInFactors;
}

int Mumps::getNumNegEigenvalues()
{
   return myNumNegEigenvalues;
}

int Mumps::getNumPosEigenvalues()
{
   return myNumPosEigenvalues;
}

int Mumps::getNumTinyPivots()
{
   return myNumTinyPivots;
}

int Mumps::getNumNullPivots()
{
   return myNumNullPivots;
}

int Mumps::getNumDelayedPivots()
{
   return myNumDelayedPivots;
}

int Mumps::getFirstNullPivot()
{
   return myFirstNullPivot;
}

int Mumps::getAnalysisMemoryUsage()
{
   return myAnalysisMemoryUsage;
}

int Mumps::getPeakAnalysisMemoryUsage()
{
   return myPeakAnalysisMemoryUsage;
}

int Mumps::getFactorSolveMemoryUsage()
{
   return myFactorSolveMemoryUsage;
}

int Mumps::getSize()
{
   return mySize;
}

int Mumps::getNumVals()
{
   return myNumVals;
}
