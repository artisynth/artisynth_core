/* --------------------------------------------------------------------
 * TEST VERSION: JNI implementation of maspack.solvers.AccelerateSolver,
 * a direct solver based on the sparse factorizations in Apple's Accelerate
 * framework (Sparse/Solve.h): LDL^T for symmetric matrices, and LU for
 * unsymmetric ones. The LU factorizations were added to Accelerate in macOS
 * 15.5, and so are used through a runtime check (luAvailable()) while the
 * deployment target stays at 13.0; on earlier systems, unsymmetric matrices
 * are rejected with ERR_LU_UNAVAILABLE.
 *
 * Solves are followed by maxRefine steps of iterative refinement (default
 * 1, as for Pardiso with symmetric matrices; environment variable
 * ACCELERATE_REFINE overrides): r = b - A x is computed from the stored
 * matrix, A dx = r is solved with the factorization, and x += dx.
 *
 * Iterative (hybrid) solves are provided by HybridSolver (hybridSolve.cc,
 * compiled with -DNO_MKL), using the most recent factorization as the
 * preconditioner. The preconditioner solves do no refinement, since the
 * outer iteration controls the accuracy.
 *
 * ArtiSynth passes CRS format with 1-based indices: the upper triangle for
 * symmetric matrices, and the full matrix otherwise. With indices made
 * 0-based, the upper triangle in CRS format is exactly the lower triangle
 * in CSC format, and a full matrix in CRS format is its transpose in CSC
 * format, which Accelerate is given with the transpose attribute set. So no
 * reordering of the values is needed. The 1-based arrays are also kept, for
 * the matrix products of the hybrid solves.
 *
 * See Makefile.accelerate.
 * -------------------------------------------------------------------- */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <sys/sysctl.h>
#include <Accelerate/Accelerate.h>
#include "hybridSolve.h"
#include "hybridSolveJNI.h"
#include "maspack_solvers_AccelerateSolver.h"

#define ERR_NOT_SYMMETRIC     -100
#define ERR_NO_MATRIX         -101
#define ERR_NO_FACTORIZATION  -102
#define ERR_OUT_OF_MEMORY     -103
#define ERR_LU_UNAVAILABLE    -104

/* Returns true if Accelerate provides LU factorizations (macOS 15.5). For
 * testing, setting the environment variable ACCELERATE_NO_LU makes LU
 * unavailable, as on earlier systems. */
static bool luAvailable() {
   if (getenv ("ACCELERATE_NO_LU") != NULL) {
      return false;
   }
   if (__builtin_available (macOS 15.5, *)) {
      return true;
   }
   return false;
}

/* Accelerate reports parameter errors through this callback; without one,
 * it logs the error and executes __builtin_trap(), which would kill the
 * JVM. The message is kept per thread and copied into the solver. */
static __thread char theLastError[256];

static void reportError (const char *message) {
   strncpy (theLastError, message, sizeof(theLastError)-1);
   theLastError[sizeof(theLastError)-1] = '\0';
}

static SparseOrder_t orderFromEnv() {
   const char *env = getenv ("ACCELERATE_ORDER");
   if (env != NULL) {
      if (strcmp (env, "amd") == 0) {
         return SparseOrderAMD;
      }
      else if (strcmp (env, "mtmetis") == 0) {
         if (__builtin_available (macOS 26.0, *)) {
            return SparseOrderMTMetis;
         }
      }
   }
   return SparseOrderMetis;
}

class AccSolver : public HybridSolver {

  public:

   int n;
   long nnz;
   bool symmetric;         // if false, the matrix is factored with LU
   long *colStarts;        // 0-based CSC lower triangle, or CSC transpose
   int *rowIndices;
   double *vals;           // values of the most recent factorization
   int *rowOffs1;          // 1-based CRS, as supplied, for hybrid solves
   int *colIdxs1;
   double *curVals;        // current values for hybrid solves
   double *xbuf;           // solve work space, n*maxRhs
   double *bbuf;
   double *rbuf;           // refinement residual and correction
   double *dxbuf;
   int maxRhs;
   int maxRefine;          // max iterative refinement steps per solve
   int lastRefine;         // refinement steps done in the last solve
   SparseMatrix_Double A;
   SparseOpaqueSymbolicFactorization sym;
   bool haveSym;
   SparseOpaqueFactorization_Double F;
   bool haveNum;
   SparseOrder_t order;
   char lastError[256];

   AccSolver();
   ~AccSolver();

   void noteError();
   void freeFactors();
   void freeMatrix();
   int setMatrix (
      const double* vals, const int* rowOffs, const int* colIdxs,
      int size, int numVals, bool symmetric);
   int factor();
   int solve (double* x, const double* b, int nrhs);
   void factorSolve (double* x, const double* b, int nrhs);
   double residual (double* r, const double* x, const double* b, int nrhs);

  protected:

   int precondSolve (double* z, const double* r);
};

AccSolver::AccSolver() {
   n = 0;
   nnz = 0;
   symmetric = true;
   colStarts = NULL;
   rowIndices = NULL;
   vals = NULL;
   rowOffs1 = NULL;
   colIdxs1 = NULL;
   curVals = NULL;
   xbuf = NULL;
   bbuf = NULL;
   rbuf = NULL;
   dxbuf = NULL;
   maxRhs = 0;
   const char *env = getenv ("ACCELERATE_REFINE");
   maxRefine = (env != NULL ? atoi (env) : 1);
   lastRefine = 0;
   memset (&A, 0, sizeof(A));
   haveSym = false;
   haveNum = false;
   order = orderFromEnv();
   lastError[0] = '\0';
}

AccSolver::~AccSolver() {
   freeMatrix();
}

void AccSolver::noteError() {
   strncpy (lastError, theLastError, sizeof(lastError));
   theLastError[0] = '\0';
}

void AccSolver::freeFactors() {
   if (haveNum) {
      SparseCleanup (F);
      haveNum = false;
   }
   if (haveSym) {
      SparseCleanup (sym);
      haveSym = false;
   }
}

void AccSolver::freeMatrix() {
   clearIterativeStructure();
   releaseIterativeWork();
   freeFactors();
   free (colStarts);
   free (rowIndices);
   free (vals);
   free (rowOffs1);
   free (colIdxs1);
   free (curVals);
   free (xbuf);
   free (bbuf);
   free (rbuf);
   free (dxbuf);
   colStarts = NULL;
   rowIndices = NULL;
   vals = NULL;
   rowOffs1 = NULL;
   colIdxs1 = NULL;
   curVals = NULL;
   xbuf = NULL;
   bbuf = NULL;
   rbuf = NULL;
   dxbuf = NULL;
   maxRhs = 0;
   n = 0;
   nnz = 0;
}

static SparseNumericFactorOptions numericOptions() {
   SparseNumericFactorOptions opts;
   memset (&opts, 0, sizeof(opts));
   opts.control = SparseDefaultControl;
   opts.scalingMethod = SparseScalingDefault;
   opts.scaling = NULL;
   opts.pivotTolerance = 0.01;                 // Accelerate's defaults
   opts.zeroTolerance = 1e-4*__DBL_EPSILON__;
   return opts;
}

/* Sets the matrix structure and values, and performs the symbolic
 * factorization. vals may be NULL. */
int AccSolver::setMatrix (
   const double* jvals, const int* rowOffs, const int* colIdxs,
   int size, int numVals, bool sym_) {

   freeMatrix();
   if (!sym_ && !luAvailable()) {
      snprintf (lastError, sizeof(lastError),
                "LU factorization requires macOS 15.5 or later");
      return ERR_LU_UNAVAILABLE;
   }
   symmetric = sym_;
   n = size;
   nnz = numVals;
   size_t nv = (numVals > 0 ? numVals : 1);
   colStarts = (long*)malloc ((size+1)*sizeof(long));
   rowIndices = (int*)malloc (nv*sizeof(int));
   vals = (double*)calloc (nv, sizeof(double));
   rowOffs1 = (int*)malloc ((size+1)*sizeof(int));
   colIdxs1 = (int*)malloc (nv*sizeof(int));
   curVals = (double*)calloc (nv, sizeof(double));
   if (!colStarts || !rowIndices || !vals ||
       !rowOffs1 || !colIdxs1 || !curVals) {
      freeMatrix();
      return ERR_OUT_OF_MEMORY;
   }
   memcpy (rowOffs1, rowOffs, (size+1)*sizeof(int));
   memcpy (colIdxs1, colIdxs, numVals*sizeof(int));
   for (int i=0; i<=size; i++) {
      colStarts[i] = rowOffs[i]-1;
   }
   for (long k=0; k<numVals; k++) {
      rowIndices[k] = colIdxs[k]-1;
   }
   if (jvals != NULL) {
      memcpy (vals, jvals, numVals*sizeof(double));
   }

   // upper triangle CRS == lower triangle CSC; full CRS == transposed CSC
   SparseAttributes_t attr;
   memset (&attr, 0, sizeof(attr));
   if (symmetric) {
      attr.transpose = false;
      attr.triangle = SparseLowerTriangle;
      attr.kind = SparseSymmetric;
   }
   else {
      attr.transpose = true;
      attr.kind = SparseOrdinary;
   }
   A.structure.rowCount = size;
   A.structure.columnCount = size;
   A.structure.columnStarts = colStarts;
   A.structure.rowIndices = rowIndices;
   A.structure.attributes = attr;
   A.structure.blockSize = 1;
   A.data = vals;

   SparseSymbolicFactorOptions sopts;
   memset (&sopts, 0, sizeof(sopts));
   sopts.control = SparseDefaultControl;
   sopts.orderMethod = order;
   sopts.order = NULL;
   sopts.ignoreRowsAndColumns = NULL;
   sopts.malloc = malloc;
   sopts.free = free;
   sopts.reportError = reportError;
   SparseFactorization_t ftype = SparseFactorizationLDLTTPP;
   if (!symmetric) {
      if (__builtin_available (macOS 15.5, *)) {
         ftype = SparseFactorizationLUTPP;
      }
   }
   sym = SparseFactor (ftype, A.structure, sopts);
   if (sym.status != SparseStatusOK) {
      noteError();
      int status = sym.status;
      SparseCleanup (sym);
      return status;
   }
   haveSym = true;
   lastError[0] = '\0';
   setIterativeStructure (
      size, numVals, rowOffs1, colIdxs1, curVals, symmetric ? 1 : 0);
   return 0;
}

/* Numeric factorization of the values in vals (same structure). The first
 * factorization after an analysis creates the factor; later ones reuse its
 * storage via SparseRefactor. */
int AccSolver::factor() {
   if (!haveSym) {
      return ERR_NO_MATRIX;
   }
   SparseNumericFactorOptions nopts = numericOptions();
   if (!haveNum) {
      F = SparseFactor (sym, A, nopts);
      haveNum = true;
   }
   else {
      SparseRefactor (A, &F, nopts);
   }
   if (F.status != SparseStatusOK) {
      noteError();
      return F.status;
   }
   return 0;
}

/* r = b - A x for nrhs columns, using the factored matrix (lower triangle
 * in CSC format if symmetric, otherwise the transpose in CSC format, i.e.,
 * CRS). Returns the max abs entry of r. */
double AccSolver::residual (
   double* r, const double* x, const double* b, int nrhs) {
   double rmax = 0;
   for (int c=0; c<nrhs; c++) {
      const double *xc = x + (long)c*n;
      double *rc = r + (long)c*n;
      memcpy (rc, b + (long)c*n, n*sizeof(double));
      if (!symmetric) {
         // row j of A is column j of the stored transpose
         for (int j=0; j<n; j++) {
            double sum = 0;
            for (long k=colStarts[j]; k<colStarts[j+1]; k++) {
               sum += vals[k]*xc[rowIndices[k]];
            }
            rc[j] -= sum;
         }
      }
      else {
         for (int j=0; j<n; j++) {
            double xj = xc[j];
            double sum = 0;
            for (long k=colStarts[j]; k<colStarts[j+1]; k++) {
               int i = rowIndices[k];
               double v = vals[k];
               rc[i] -= v*xj;
               if (i != j) {
                  sum += v*xc[i];
               }
            }
            rc[j] -= sum;
         }
      }
      for (int i=0; i<n; i++) {
         double a = fabs (rc[i]);
         if (a > rmax) {
            rmax = a;
         }
      }
   }
   return rmax;
}

/* Solves with the factorization only, without refinement. b is not
 * modified, although the Accelerate API takes it as non-const. */
void AccSolver::factorSolve (double* x, const double* b, int nrhs) {
   if (nrhs == 1) {
      DenseVector_Double bv = { n, (double*)b };
      DenseVector_Double xv = { n, x };
      SparseSolve (F, bv, xv);
   }
   else {
      SparseAttributes_t attr;
      memset (&attr, 0, sizeof(attr));
      DenseMatrix_Double B = { n, nrhs, n, attr, (double*)b };
      DenseMatrix_Double X = { n, nrhs, n, attr, x };
      SparseSolve (F, B, X);
   }
}

/* Solves with the factorization, followed by up to maxRefine refinement
 * steps. rbuf and dxbuf must hold at least n*nrhs values. */
int AccSolver::solve (double* x, const double* b, int nrhs) {
   factorSolve (x, b, nrhs);
   lastRefine = 0;
   for (int step=0; step<maxRefine; step++) {
      if (residual (rbuf, x, b, nrhs) == 0) {
         break;
      }
      factorSolve (dxbuf, rbuf, nrhs);
      for (long i=0; i<(long)n*nrhs; i++) {
         x[i] += dxbuf[i];
      }
      lastRefine++;
   }
   if (theLastError[0] != '\0') {
      noteError();
      return -4;
   }
   return 0;
}

int AccSolver::precondSolve (double* z, const double* r) {
   if (!haveNum || F.status != SparseStatusOK) {
      return ERR_NO_FACTORIZATION;
   }
   factorSolve (z, r, 1);
   if (theLastError[0] != '\0') {
      noteError();
      return -4;
   }
   return 0;
}

/* --- JNI --- */

JNIEXPORT jlong JNICALL Java_maspack_solvers_AccelerateSolver_doInit
   (JNIEnv *env, jclass cls) {
   return (jlong)(new AccSolver());
}

JNIEXPORT void JNICALL Java_maspack_solvers_AccelerateSolver_doRelease
   (JNIEnv *env, jobject obj, jlong handle) {
   delete (AccSolver*)handle;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_AccelerateSolver_doSetMatrix
   (JNIEnv *env, jobject obj, jlong handle, jdoubleArray jvals,
    jintArray jrowOffs, jintArray jcolIdxs, jint size, jint numVals,
    jboolean symmetric) {

   AccSolver *s = (AccSolver*)handle;
   jint *rowOffs = env->GetIntArrayElements (jrowOffs, NULL);
   jint *colIdxs = env->GetIntArrayElements (jcolIdxs, NULL);
   double *vals = NULL;
   if (jvals != NULL) {
      vals = env->GetDoubleArrayElements (jvals, NULL);
   }
   int rcode = s->setMatrix (
      vals, rowOffs, colIdxs, size, numVals, symmetric);
   if (vals != NULL) {
      env->ReleaseDoubleArrayElements (jvals, vals, JNI_ABORT);
   }
   env->ReleaseIntArrayElements (jrowOffs, rowOffs, JNI_ABORT);
   env->ReleaseIntArrayElements (jcolIdxs, colIdxs, JNI_ABORT);
   return rcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_AccelerateSolver_doFactor
   (JNIEnv *env, jobject obj, jlong handle, jdoubleArray jvals) {

   AccSolver *s = (AccSolver*)handle;
   if (!s->haveSym) {
      return ERR_NO_MATRIX;
   }
   if (jvals != NULL) {
      env->GetDoubleArrayRegion (jvals, 0, s->nnz, s->vals);
   }
   return s->factor();
}

JNIEXPORT jint JNICALL Java_maspack_solvers_AccelerateSolver_doSolve
   (JNIEnv *env, jobject obj, jlong handle,
    jdoubleArray jx, jdoubleArray jb, jint nrhs) {

   AccSolver *s = (AccSolver*)handle;
   if (!s->haveNum || s->F.status != SparseStatusOK) {
      return ERR_NO_FACTORIZATION;
   }
   int n = s->n;
   if (nrhs > s->maxRhs) {
      free (s->xbuf);
      free (s->bbuf);
      free (s->rbuf);
      free (s->dxbuf);
      long len = (long)n*nrhs*sizeof(double);
      s->xbuf = (double*)malloc (len);
      s->bbuf = (double*)malloc (len);
      s->rbuf = (double*)malloc (len);
      s->dxbuf = (double*)malloc (len);
      if (!s->xbuf || !s->bbuf || !s->rbuf || !s->dxbuf) {
         s->maxRhs = 0;
         return ERR_OUT_OF_MEMORY;
      }
      s->maxRhs = nrhs;
   }
   env->GetDoubleArrayRegion (jb, 0, (long)n*nrhs, s->bbuf);
   int rcode = s->solve (s->xbuf, s->bbuf, nrhs);
   if (rcode == 0) {
      env->SetDoubleArrayRegion (jx, 0, (long)n*nrhs, s->xbuf);
   }
   return rcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_AccelerateSolver_doIterativeSolve
   (JNIEnv *env, jobject obj, jlong handle, jdoubleArray jvals,
    jdoubleArray jxvec, jdoubleArray jbvec, jdouble tol, jint method,
    jint maxSolves, jint restart, jboolean critical) {
   return hybridSolveJNI (
      env, (AccSolver*)handle, jvals, jxvec, jbvec, tol, method, maxSolves,
      restart, critical);
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_AccelerateSolver_doGetLastIterativeSolves
   (JNIEnv *env, jobject obj, jlong handle) {
   return ((AccSolver*)handle)->getLastIterativeSolves();
}

JNIEXPORT jdouble JNICALL
Java_maspack_solvers_AccelerateSolver_doGetLastIterativeResidual
   (JNIEnv *env, jobject obj, jlong handle) {
   return ((AccSolver*)handle)->getLastIterativeResidual();
}

JNIEXPORT void JNICALL
Java_maspack_solvers_AccelerateSolver_doGetLastIterativeTimes
   (JNIEnv *env, jobject obj, jlong handle, jdoubleArray jtimes) {
   double times[3];
   ((AccSolver*)handle)->getLastIterativeTimes (times);
   env->SetDoubleArrayRegion (jtimes, 0, 3, times);
}

JNIEXPORT jlong JNICALL
Java_maspack_solvers_AccelerateSolver_doGetNumNonZerosInFactors
   (JNIEnv *env, jobject obj, jlong handle) {
   AccSolver *s = (AccSolver*)handle;
   if (!s->haveSym) {
      return -1;
   }
   // lower bound: pivoting may delay pivots and enlarge the factors
   return (jlong)(s->sym.factorSize_Double/sizeof(double));
}

JNIEXPORT jint JNICALL Java_maspack_solvers_AccelerateSolver_doGetInertia
   (JNIEnv *env, jobject obj, jlong handle, jintArray jposZeroNeg) {
   AccSolver *s = (AccSolver*)handle;
   if (!s->haveNum || s->F.status != SparseStatusOK) {
      return ERR_NO_FACTORIZATION;
   }
   if (!s->symmetric) {
      return ERR_NOT_SYMMETRIC; // inertia is only defined for LDL^T
   }
   int pos, zero, neg;
   int rc = SparseGetInertia (s->F, &pos, &zero, &neg);
   if (rc != 0) {
      return rc;
   }
   jint vals[3] = { pos, zero, neg };
   env->SetIntArrayRegion (jposZeroNeg, 0, 3, vals);
   return 0;
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_AccelerateSolver_doSetMaxRefinementSteps
   (JNIEnv *env, jobject obj, jlong handle, jint nsteps) {
   AccSolver *s = (AccSolver*)handle;
   int prev = s->maxRefine;
   s->maxRefine = (nsteps < 0 ? 0 : nsteps);
   return prev;
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_AccelerateSolver_doGetMaxRefinementSteps
   (JNIEnv *env, jobject obj, jlong handle) {
   return ((AccSolver*)handle)->maxRefine;
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_AccelerateSolver_doGetNumRefinementSteps
   (JNIEnv *env, jobject obj, jlong handle) {
   return ((AccSolver*)handle)->lastRefine;
}

JNIEXPORT jstring JNICALL Java_maspack_solvers_AccelerateSolver_doGetLastError
   (JNIEnv *env, jobject obj, jlong handle) {
   AccSolver *s = (AccSolver*)handle;
   if (s->lastError[0] == '\0') {
      return NULL;
   }
   return env->NewStringUTF (s->lastError);
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_AccelerateSolver_doGetPhysicalCoreCount
   (JNIEnv *env, jclass cls) {
   return hybridGetPhysicalCoreCount();
}

JNIEXPORT jboolean JNICALL
Java_maspack_solvers_AccelerateSolver_doIsLUAvailable
   (JNIEnv *env, jclass cls) {
   return luAvailable() ? JNI_TRUE : JNI_FALSE;
}
