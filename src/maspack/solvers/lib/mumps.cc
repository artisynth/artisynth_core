/* -------------------------------------------------------------------- */
/* C++ wrapper around the MUMPS sparse direct solver. See mumps.h for    */
/* an overview of how the MUMPS API differs from that of Pardiso.        */
/* -------------------------------------------------------------------- */

#include "mumps.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

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
#define MAX_FACTOR_RETRIES 6
// largest ICNTL(14) (workspace increase, in percent) the retries will request
#define MAX_WORKSPACE_INCREASE 20000

// ICNTL(8) values used by the adaptive scaling policy: 77 lets MUMPS choose,
// which for these systems usually means scaling computed during analysis,
// while 8 computes it during each factorization
#define SCALING_ANALYSIS 77
#define SCALING_FACTOR   8

// states of the adaptive strategy policy
#define STRATEGY_TRIAL_PENDING 0
#define STRATEGY_DECIDED       1

// the alternative strategy is kept only if it reduces the number of entries in
// the factors to this fraction or less of the incumbent's
#define STRATEGY_MARGIN 0.9

// the choice is redone if the matrix size or number of values has changed by
// more than this fraction since it was made
#define STRATEGY_RESIZE_FRACTION 0.25

// OpenMP and MKL thread control. Declared explicitly so that this file does
// not need to be compiled with -fopenmp.
// Note that the MKL routines must be referred to by their C names
// (MKL_Set_Num_Threads); the lower case names are the Fortran entry points,
// which take their argument by reference.
extern "C" {
   int omp_get_max_threads (void);
   void omp_set_num_threads (int num);
#ifndef NO_MKL
   int MKL_Get_Max_Threads (void);
   void MKL_Set_Num_Threads (int num);
#endif
#ifdef LIBOMP
   void kmp_set_blocktime (int msec);
#endif
}

#ifdef NO_MKL
// Without MKL (macOS arm64, where BLAS comes from Accelerate) there is no
// separate BLAS thread count to set: Accelerate threads internally and has
// no thread control.
static void MKL_Set_Num_Threads (int num) {}
#endif

#ifdef LIBOMP
/**
 * With LLVM's libomp, idle OpenMP threads by default go to sleep after
 * KMP_BLOCKTIME ms, which libomp sets to 0 on hybrid (performance +
 * efficiency core) CPUs such as Apple Silicon. MUMPS runs many short
 * parallel regions, so its threads then sleep and are re-woken constantly:
 * on an M5 Pro this made factorizations with 4-15 threads slower than with
 * one. A blocktime of 1 ms fixes that (a 1.7x speedup at 4-8 threads)
 * without the CPU cost of longer spinning. The blocktime is a property of
 * the calling thread, so it is set before every MUMPS call; an explicit
 * KMP_BLOCKTIME setting in the environment is left alone.
 */
static void applyBlocktime()
{
   static int useEnvironment = -1;
   if (useEnvironment < 0) {
      useEnvironment = (getenv ("KMP_BLOCKTIME") != NULL);
   }
   if (!useEnvironment) {
      kmp_set_blocktime (1);
   }
}
#endif

Mumps::Mumps()
{
   myInstanceActive = 0;
   mySym = -1;
   myInitError = 0;
   myLastInfo1 = 0;
   myLastInfo2 = 0;
   myVerbose = (getenv ("MUMPS_JNI_VERBOSE") != NULL);
   myStrategyTrace = (getenv ("MUMPS_STRATEGY_TRACE") != NULL);

   mySize = 0;
   myMaxSize = 0;
   myNumVals = 0;
   myMaxNumVals = 0;
   myIrn = NULL;
   myJcn = NULL;
   myVals = NULL;

   myRhs = NULL;
   myMaxRhsSize = 0;

   myRowOffs = NULL;
   myCurVals = NULL;

   // MUMPS default settings. Negative int values mean "leave at the MUMPS
   // default"; the exceptions are the settings whose default we deliberately
   // change, which are documented in MumpsSolver.java.
   myReorderMethod = MUMPS_AUTO_REORDER;
   myApplyWeightedMatchings = -1;
   myScaling = -1;              // adaptive; see adaptStrategy()
   myPhaseNumThreads = -1;
   myPreferredScaling = -1;
   myStrategyDecided = 0;
   myDecidedSize = 0;
   myDecidedNumVals = 0;
   myNumStrategyTrials = 0;
   myNumStaleSwitches = 0;
   myNumReanalyses = 0;
   resetScalingPolicy();
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
   int scaling = (myScaling >= 0 ? myScaling : myActiveScaling);
   if (scaling >= 0) {
      myId.ICNTL(8) = scaling;
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
#ifdef LIBOMP
   applyBlocktime();
#endif
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
      free (myCurVals);
      myIrn = (MUMPS_INT*)malloc (numVals*sizeof(MUMPS_INT));
      myJcn = (MUMPS_INT*)malloc (numVals*sizeof(MUMPS_INT));
      myVals = (double*)malloc (numVals*sizeof(double));
      myCurVals = (double*)malloc (numVals*sizeof(double));
      if (myIrn == NULL || myJcn == NULL || myVals == NULL ||
          myCurVals == NULL) {
         myMaxNumVals = 0;
         return -13;  // MUMPS code for an allocation problem
      }
      myMaxNumVals = numVals;
   }
   if (size > myMaxSize) {
      free (myRowOffs);
      myRowOffs = (int*)malloc ((size+1)*sizeof(int));
      if (myRowOffs == NULL) {
         myMaxSize = 0;
         return -13;
      }
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
   // the iterative structure references arrays which allocateMatrix may
   // reallocate
   clearIterativeStructure();
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
   // keep the row offsets, so that the CRS form is available for the matrix
   // products needed by iterative solves
   for (i=0; i<=size; i++) {
      myRowOffs[i] = rowOffs[i];
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

   // each analysis produces a fresh scaling, so the choice starts over
   resetScalingPolicy();
   applySettings();
   // remember the thread count, which later phases must match
   myPhaseNumThreads = omp_get_max_threads();
   rcode = callMumps (JOB_ANALYZE);
   if (rcode < 0) {
      mySize = 0;
      return rcode;
   }
   getAnalysisStatistics();
   setIterativeStructure (
      size, numVals, myRowOffs, myJcn, myCurVals, sym != MUMPS_UNSYMMETRIC);
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

/**
 * Resets the adaptive strategy state for a new analysis, starting from the
 * strategy remembered from earlier analyses of this instance.
 */
void Mumps::resetScalingPolicy()
{
   if (myPreferredScaling < 0) {
      myPreferredScaling = SCALING_ANALYSIS;
   }
   myActiveScaling = myPreferredScaling;
   myStrategyTried = 0;
   // the strategy is chosen once per matrix, and again if the matrix has
   // changed substantially in size, since that may change which is better
   int redo = !myStrategyDecided;
   if (myStrategyDecided && myDecidedSize > 0) {
      double sizeChange =
         fabs (mySize-myDecidedSize)/(double)myDecidedSize;
      double valsChange =
         fabs (myNumVals-myDecidedNumVals)/(double)myDecidedNumVals;
      if (sizeChange > STRATEGY_RESIZE_FRACTION ||
          valsChange > STRATEGY_RESIZE_FRACTION) {
         redo = 1;
      }
   }
   myStrategyState = (redo ? STRATEGY_TRIAL_PENDING : STRATEGY_DECIDED);
}

/**
 * Returns the number of entries in the factors, as reported by INFOG(29),
 * whose absolute value is in millions when negative.
 */
static long long numFactorEntries (int infog29)
{
   long long nnz = infog29;
   return (nnz < 0 ? -nnz*1000000 : nnz);
}

/**
 * Restores the number of threads used when the matrix was analyzed. MUMPS
 * requires the factorization and solve phases to run with the same number of
 * threads as the analysis, failing with error -58 (INFOG(2) holding the
 * analysis count) when ICNTL(48) multithreaded tree parallelism is active,
 * which it is by default. Since the OpenMP thread count is a process
 * property, another solver in the same process can change it between our
 * phases -- for instance when it is created, or when its own analysis is
 * throttled for a smaller matrix -- so it is re-asserted here.
 */
void Mumps::applyPhaseThreads()
{
   if (myPhaseNumThreads > 0 && omp_get_max_threads() != myPhaseNumThreads) {
      omp_set_num_threads (myPhaseNumThreads);
      MKL_Set_Num_Threads (myPhaseNumThreads);
   }
}

void Mumps::strategyTrace (const char* msg)
{
   if (myStrategyTrace) {
      printf ("MUMPS strategy: %s (size=%d ICNTL(8)=%d ICNTL(12)=%d "
              "delayed=%d null=%d nnzFactors=%d trials=%d staleSwitches=%d "
              "reanalyses=%d)\n",
              msg, mySize, myActiveScaling, myId.INFOG(24), myId.INFOG(13),
              myId.INFOG(28), myId.INFOG(29), myNumStrategyTrials,
              myNumStaleSwitches, myNumReanalyses);
      fflush (stdout);
   }
}

/**
 * Performs one factorization, increasing the workspace and retrying if it
 * turns out to be too small.
 *
 * Unlike Pardiso, MUMPS preallocates its workspace from the estimates made
 * during the analyze phase, and numerical pivoting can cause this to be
 * exceeded. The remedy is to increase ICNTL(14) and factor again. INFO(2)
 * gives the number of entries missing (in millions if negative) and INFO(20)
 * the estimated workspace size, so the increase can be sized directly rather
 * than guessed.
 */
int Mumps::factorOnce()
{
   applyPhaseThreads();
   applySettings();
   int rcode = callMumps (JOB_FACTOR);
   int ntries = 0;
   while (isWorkspaceError (rcode) && ntries < MAX_FACTOR_RETRIES) {
      int increase = myId.ICNTL(14);
      if (increase <= 0) {
         increase = 20;    // the MUMPS default
      }
      long long missing = myId.INFO(2);
      long long estimate = myId.INFO(20);
      if (missing < 0) {
         missing = -missing*1000000;
      }
      if (estimate < 0) {
         estimate = -estimate*1000000;
      }
      if (missing > 0 && estimate > 0) {
         // add the missing fraction of the estimate, plus a margin
         increase += (int)((100*missing)/estimate) + 20;
      }
      else {
         increase *= 2;
      }
      if (increase > MAX_WORKSPACE_INCREASE) {
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
   return rcode;
}

/**
 * Re-runs the analysis with the indicated value of ICNTL(8), and factors
 * again. The matrix structure and values are unchanged.
 */
int Mumps::reanalyzeAndFactor (int scaling)
{
   myActiveScaling = scaling;
   myNumReanalyses++;
   applyPhaseThreads();
   applySettings();
   int rcode = callMumps (JOB_ANALYZE);
   if (rcode < 0) {
      return rcode;
   }
   getAnalysisStatistics();
   return factorOnce();
}

/**
 * Chooses the analysis strategy for this instance, when the scaling has not
 * been set explicitly (myScaling < 0).
 *
 * ICNTL(8) determines not only the scaling but, through MUMPS's automatic
 * choices, the ordering strategy used for symmetric matrices:
 *
 * - ICNTL(8)=77 typically leads to a constrained ordering (ICNTL(12)=3) and a
 *   scaling computed during the analysis. For some KKT systems this is much
 *   the better choice, giving no delayed pivots at all. Its drawback is that
 *   the scaling is computed once, and becomes stale when a single analysis is
 *   followed by factorizations of changing values, as in a simulation: a stale
 *   scaling can make a pivot appear to be null, and null pivot detection
 *   (ICNTL(24)) then "fixes" it, silently corrupting the solution.
 *
 * - ICNTL(8)=8 leads to an ordering on the compressed graph (ICNTL(12)=2) and
 *   a scaling computed during each factorization, so it never goes stale. For
 *   other KKT systems this is the better choice, again by a wide margin.
 *
 * Since both depend on the analysis, the strategy is chosen by trying them:
 * if the first factorization after an analysis delays more than
 * 1/STRATEGY_DELAYED_DIVISOR of the pivots, the analysis is redone with the
 * other strategy, and whichever delays fewer pivots is kept. The winner is
 * remembered for later analyses of the same instance, so a simulation whose
 * constraints keep changing pays for the trial only once. Null pivots found
 * while using the analysis scaling mean it has gone stale, so the analysis is
 * redone with factorization scaling, which is then kept.
 */
int Mumps::adaptStrategy (int rcode)
{
   if (myScaling >= 0 || rcode < 0) {
      return rcode;     // set explicitly, or the factorization failed
   }
   int nullPivots = myId.INFOG(28);
   int delayed = myId.INFOG(13);
   int other = (myActiveScaling == SCALING_ANALYSIS ?
                SCALING_FACTOR : SCALING_ANALYSIS);

   if (myActiveScaling == SCALING_ANALYSIS && nullPivots > 0) {
      // the analysis scaling has gone stale
      myNumStaleSwitches++;
      strategyTrace ("stale analysis scaling: switching");
      myStrategyState = STRATEGY_DECIDED;
      myPreferredScaling = SCALING_FACTOR;
      myStrategyDecided = 1;
      myDecidedSize = mySize;
      myDecidedNumVals = myNumVals;
      rcode = reanalyzeAndFactor (SCALING_FACTOR);
      strategyTrace ("switched");
      return rcode;
   }
   if (myStrategyState == STRATEGY_TRIAL_PENDING && !myStrategyTried) {
      myStrategyState = STRATEGY_DECIDED;
      int firstScaling = myActiveScaling;
      long long firstEntries = numFactorEntries (myId.INFOG(29));
      myStrategyTried = 1;
      myNumStrategyTrials++;
      strategyTrace ("trying alternative strategy");
      int rc2 = reanalyzeAndFactor (other);
      long long otherEntries = numFactorEntries (myId.INFOG(29));
      if (rc2 < 0 || myId.INFOG(28) > 0 ||
          otherEntries > STRATEGY_MARGIN*firstEntries) {
         // not enough better to be worth switching
         strategyTrace ("keeping original strategy");
         myPreferredScaling = firstScaling;
         rcode = reanalyzeAndFactor (firstScaling);
      }
      else {
         myPreferredScaling = other;
         rcode = rc2;
      }
      myStrategyDecided = 1;
      myDecidedSize = mySize;
      myDecidedNumVals = myNumVals;
      strategyTrace ("strategy chosen");
   }
   return rcode;
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
   int rcode = factorOnce();
   rcode = adaptStrategy (rcode);
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

   applyPhaseThreads();
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

/* --------------------------------------------------------------------
 * Iterative (hybrid) solves. The GMRES and CGS iterations are implemented
 * by HybridSolver (hybridSolve.cc), which calls precondSolve() below.
 * -------------------------------------------------------------------- */

/**
 * Applies the preconditioner, z = M^{-1} r, using the current factorization.
 * MUMPS solves in place, so r is copied into z and solved there.
 */
int Mumps::precondSolve (double* z, const double* r)
{
   applyPhaseThreads();
   memcpy (z, r, mySize*sizeof(double));
   myId.nrhs = 1;
   myId.rhs = z;
   int rcode = callMumps (JOB_SOLVE);
   myId.rhs = myRhs;
   return rcode;
}

/**
 * Disables refinement and error analysis within the preconditioner solves,
 * since the outer iteration controls the accuracy.
 */
int Mumps::beginIterations()
{
   applySettings();
   myId.ICNTL(10) = 0;
   myId.ICNTL(11) = 0;
   return 0;
}

void Mumps::endIterations()
{
   myId.nrhs = 1;
   applySettings();  // restores ICNTL(10)
}

int Mumps::releaseMatrix()
{
   terminateInstance();
   clearIterativeStructure();
   releaseIterativeWork();
   free (myIrn);
   free (myJcn);
   free (myVals);
   free (myRhs);
   free (myRowOffs);
   free (myCurVals);
   myIrn = NULL;
   myJcn = NULL;
   myVals = NULL;
   myRhs = NULL;
   myRowOffs = NULL;
   myCurVals = NULL;
   myMaxNumVals = 0;
   myMaxSize = 0;
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

/**
 * Sets ICNTL(8), which selects the scaling MUMPS applies. A value < 0 selects
 * the adaptive policy in adaptStrategy(); values of 7 and 8 compute the
 * scaling during each factorization, while 77 lets MUMPS choose, which for
 * these systems usually means computing it during the analysis.
 */
int Mumps::setScaling (int value)
{
   myScaling = value;
   applySettings();
   return 0;
}

/**
 * Returns the requested value of ICNTL(8), or, when the scaling is being
 * chosen adaptively, the value MUMPS actually used (INFOG(33)) once a phase
 * has been run.
 */
int Mumps::getScaling()
{
   if (myScaling >= 0) {
      return myScaling;
   }
   else if (myInstanceActive) {
      return myId.INFOG(33);
   }
   else {
      return -1;
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

int Mumps::getNumStrategyTrials()
{
   return myNumStrategyTrials;
}

int Mumps::getNumStaleSwitches()
{
   return myNumStaleSwitches;
}

int Mumps::getNumReanalyses()
{
   return myNumReanalyses;
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
