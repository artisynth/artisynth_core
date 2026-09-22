/* --------------------------------------------------------------------
 * Hybrid direct/iterative solves; see hybridSolve.h. Methods:
 *
 *  GMRES   restarted GMRES(m) with right preconditioning. The preconditioned
 *          basis vectors are stored (as in flexible GMRES), so that no extra
 *          solve is needed per restart and a slightly varying preconditioner
 *          is tolerated. One preconditioner solve and one matrix product per
 *          iteration, and the residual is minimized.
 *  CGS     Sonneveld's Conjugate Gradients Squared (SIAM J. Sci. Stat.
 *          Comput. 10(1), 1989), right preconditioned. Two solves and two
 *          matrix products per iteration.
 *
 * All methods start from x = 0, and succeed when the true relative residual
 * ||b - A x||/||b|| is <= tol.
 * -------------------------------------------------------------------- */

#include <stdlib.h>
#include <string.h>
#include <cmath>
#include <chrono>

#ifdef WINDOWS_COMPILER
#include <windows.h>
#elif defined(DARWIN)
#include <sys/sysctl.h>
#else
#include <stdio.h>
#endif

#include "mkl_spblas.h"
#include "hybridSolve.h"

/* --------------------------------------------------------------------
 * hybridGetPhysicalCoreCount(): see hybridSolve.h. Queried once (below)
 * and cached, since this is a machine property, not something that can
 * change between calls.
 * -------------------------------------------------------------------- */

#ifdef WINDOWS_COMPILER

// GetLogicalProcessorInformation (the non-"Ex" form) is used for
// simplicity: one fixed-size struct per entry, no variable-length records
// to walk. Its known limitation is systems with more than 64 logical
// processors (multiple "processor groups"), which it does not enumerate
// across -- not a machine this code is expected to run on; the "Ex" form
// with GetLogicalProcessorInformationEx would be needed there.
static int queryPhysicalCoreCount()
{
   DWORD len = 0;
   GetLogicalProcessorInformation (NULL, &len);
   if (GetLastError() != ERROR_INSUFFICIENT_BUFFER || len == 0) {
      return -1;
   }
   SYSTEM_LOGICAL_PROCESSOR_INFORMATION* buf =
      (SYSTEM_LOGICAL_PROCESSOR_INFORMATION*)malloc (len);
   if (buf == NULL) {
      return -1;
   }
   int count = -1;
   if (GetLogicalProcessorInformation (buf, &len)) {
      int n = (int)(len/sizeof(SYSTEM_LOGICAL_PROCESSOR_INFORMATION));
      count = 0;
      for (int i=0; i<n; i++) {
         if (buf[i].Relationship == RelationProcessorCore) {
            count++;
         }
      }
   }
   free (buf);
   return count;
}

#elif defined(DARWIN)

// macOS: hw.physicalcpu is exactly this (as opposed to hw.logicalcpu,
// which includes hyperthreads) -- no parsing needed.
static int queryPhysicalCoreCount()
{
   int count;
   size_t size = sizeof(count);
   if (sysctlbyname ("hw.physicalcpu", &count, &size, NULL, 0) != 0) {
      return -1;
   }
   return count;
}

#else

// Linux: count distinct (physical id, core id) pairs in /proc/cpuinfo. A
// plain line-oriented parse rather than a dependency on libnuma/hwloc.
static int queryPhysicalCoreCount()
{
   FILE* f = fopen ("/proc/cpuinfo", "r");
   if (f == NULL) {
      return -1;
   }
   // far more (physicalId,coreId) pairs than any real machine has
   const int MAX_PAIRS = 4096;
   long long seen[MAX_PAIRS];
   int numSeen = 0;
   int physId = 0;
   char line[256];
   while (fgets (line, sizeof(line), f)) {
      int val;
      if (sscanf (line, "physical id : %d", &val) == 1) {
         physId = val;
      }
      else if (sscanf (line, "core id : %d", &val) == 1) {
         long long key = ((long long)physId << 32) | (unsigned int)val;
         bool found = false;
         for (int i=0; i<numSeen; i++) {
            if (seen[i] == key) {
               found = true;
               break;
            }
         }
         if (!found && numSeen < MAX_PAIRS) {
            seen[numSeen++] = key;
         }
      }
   }
   fclose (f);
   return numSeen > 0 ? numSeen : -1;
}

#endif

static int thePhysicalCoreCount = -2; // -2: not yet queried

int hybridGetPhysicalCoreCount()
{
   if (thePhysicalCoreCount == -2) {
      thePhysicalCoreCount = queryPhysicalCoreCount();
   }
   return thePhysicalCoreCount;
}

static double dot (const double* a, const double* b, int n)
{
   double sum = 0;
   for (int i=0; i<n; i++) {
      sum += a[i]*b[i];
   }
   return sum;
}

double HybridSolver::norm2 (const double* a, int n)
{
   return std::sqrt (dot (a, a, n));
}

/**
 * Wall clock time in seconds, for timing the parts of an iterative solve.
 */
double HybridSolver::wallTime()
{
   using namespace std::chrono;
   return duration<double>(steady_clock::now().time_since_epoch()).count();
}

HybridSolver::HybridSolver()
{
   myIterSize = 0;
   myIterNumVals = 0;
   myIterRowOffs = NULL;
   myIterColIdxs = NULL;
   myIterCurVals = NULL;
   myIterSymmetric = 0;
   myCsrHandle = NULL;
   myIterX = NULL;
   myIterB = NULL;
   myIterVecSize = 0;
   myWork = NULL;
   myMaxWorkSize = 0;
   myLastIterativeSolves = 0;
   myLastIterativeResidual = -1;
   myIterTotalTime = 0;
   myIterSolveTime = 0;
   myIterMatVecTime = 0;
}

HybridSolver::~HybridSolver()
{
   clearIterativeStructure();
   releaseIterativeWork();
}

void HybridSolver::setIterativeStructure (
   int size, int numVals, const int* rowOffs, const int* colIdxs,
   double* curVals, int symmetric)
{
   clearIterativeStructure();
   myIterSize = size;
   myIterNumVals = numVals;
   myIterRowOffs = rowOffs;
   myIterColIdxs = colIdxs;
   myIterCurVals = curVals;
   myIterSymmetric = symmetric;
}

void HybridSolver::clearIterativeStructure()
{
   if (myCsrHandle != NULL) {
      mkl_sparse_destroy ((sparse_matrix_t)myCsrHandle);
      myCsrHandle = NULL;
   }
   myIterSize = 0;
   myIterNumVals = 0;
   myIterRowOffs = NULL;
   myIterColIdxs = NULL;
   myIterCurVals = NULL;
}

void HybridSolver::releaseIterativeWork()
{
   free (myWork);
   myWork = NULL;
   myMaxWorkSize = 0;
   free (myIterX);
   free (myIterB);
   myIterX = NULL;
   myIterB = NULL;
   myIterVecSize = 0;
}

int HybridSolver::ensureWork (long long size)
{
   if (size > myMaxWorkSize) {
      free (myWork);
      myWork = (double*)malloc (size*sizeof(double));
      if (myWork == NULL) {
         myMaxWorkSize = 0;
         return -13;
      }
      myMaxWorkSize = size;
   }
   return 0;
}

/**
 * Computes y = A x for the current values. The MKL handle wraps the CRS
 * arrays without copying them, and is not optimized (which could make MKL
 * keep internal copies), so that updates to the current values are seen
 * directly.
 */
int HybridSolver::matVec (double* y, const double* x)
{
   sparse_status_t status;
   if (myCsrHandle == NULL) {
      sparse_matrix_t A = NULL;
      // MKL does not modify the index arrays, but its interface is not const
      status = mkl_sparse_d_create_csr (
         &A, SPARSE_INDEX_BASE_ONE, myIterSize, myIterSize,
         (MKL_INT*)myIterRowOffs, (MKL_INT*)(myIterRowOffs+1),
         (MKL_INT*)myIterColIdxs, myIterCurVals);
      if (status != SPARSE_STATUS_SUCCESS) {
         return -1;
      }
      myCsrHandle = A;
   }
   struct matrix_descr descr;
   if (!myIterSymmetric) {
      descr.type = SPARSE_MATRIX_TYPE_GENERAL;
   }
   else {
      // only the upper triangle is stored
      descr.type = SPARSE_MATRIX_TYPE_SYMMETRIC;
      descr.mode = SPARSE_FILL_MODE_UPPER;
      descr.diag = SPARSE_DIAG_NON_UNIT;
   }
   double t0 = wallTime();
   status = mkl_sparse_d_mv (
      SPARSE_OPERATION_NON_TRANSPOSE, 1.0, (sparse_matrix_t)myCsrHandle,
      descr, x, 0.0, y);
   myIterMatVecTime += wallTime()-t0;
   return (status == SPARSE_STATUS_SUCCESS ? 0 : -1);
}

/**
 * Computes r = b - A x and returns its norm, or -1 if the product failed.
 */
double HybridSolver::trueResidual (double* r, const double* x, const double* b)
{
   if (matVec (r, x) != 0) {
      return -1;
   }
   for (int i=0; i<myIterSize; i++) {
      r[i] = b[i] - r[i];
   }
   return norm2 (r, myIterSize);
}

int HybridSolver::timedPrecondSolve (double* z, const double* r)
{
   double t0 = wallTime();
   int rcode = precondSolve (z, r);
   myIterSolveTime += wallTime()-t0;
   return rcode;
}

int HybridSolver::beginIterations()
{
   return 0;
}

void HybridSolver::endIterations()
{
}

int HybridSolver::gmresSolve (
   double* x, const double* b, double tol, int maxSolves, int restart)
{
   int n = myIterSize;
   int m = (restart < maxSolves ? restart : maxSolves);
   if (m < 1) {
      m = 1;
   }
   if (ensureWork ((long long)(2*m+3)*n) != 0) {
      return -1;
   }
   double* V = myWork;                        // m+1 basis vectors
   double* Z = myWork + (long long)(m+1)*n;   // m preconditioned vectors
   double* r = myWork + (long long)(2*m+1)*n;
   double* w = r + n;

   // Hessenberg matrix, stored column major, and Givens rotations
   double* H = (double*)calloc ((m+1)*m, sizeof(double));
   double* cs = (double*)malloc (m*sizeof(double));
   double* sn = (double*)malloc (m*sizeof(double));
   double* g = (double*)malloc ((m+1)*sizeof(double));
   double* y = (double*)malloc (m*sizeof(double));
   #define HH(i,j) H[(i)+(j)*(m+1)]

   double bnorm = norm2 (b, n);
   double tolRes = tol*bnorm;
   memset (x, 0, n*sizeof(double));
   memcpy (r, b, n*sizeof(double));
   double beta = bnorm;
   double prevBeta = beta;
   int nsolves = 0;
   int status = 0; // 1 for converged, -1 for failed

   while (status == 0) {
      int i, j, l;
      int k = 0;   // number of basis vectors used in this cycle
      for (i=0; i<n; i++) {
         V[i] = r[i]/beta;
      }
      g[0] = beta;
      for (j=0; j<m && nsolves<maxSolves; j++) {
         double* vj = V + (long long)j*n;
         double* zj = Z + (long long)j*n;
         if (timedPrecondSolve (zj, vj) < 0 || matVec (w, zj) != 0) {
            status = -1;
            break;
         }
         nsolves++;
         // modified Gram-Schmidt
         for (i=0; i<=j; i++) {
            double* vi = V + (long long)i*n;
            double h = dot (w, vi, n);
            HH(i,j) = h;
            for (l=0; l<n; l++) {
               w[l] -= h*vi[l];
            }
         }
         double hn = norm2 (w, n);
         // apply the previous rotations to the new column, and then compute
         // the rotation which eliminates HH(j+1,j)
         for (i=0; i<j; i++) {
            double t = cs[i]*HH(i,j) + sn[i]*HH(i+1,j);
            HH(i+1,j) = -sn[i]*HH(i,j) + cs[i]*HH(i+1,j);
            HH(i,j) = t;
         }
         double den = std::sqrt (HH(j,j)*HH(j,j) + hn*hn);
         if (den == 0 || !std::isfinite (den)) {
            status = -1;
            break;
         }
         cs[j] = HH(j,j)/den;
         sn[j] = hn/den;
         HH(j,j) = den;
         g[j+1] = -sn[j]*g[j];
         g[j] = cs[j]*g[j];
         k = j+1;
         if (std::fabs(g[j+1]) <= tolRes || hn == 0) {
            break;  // converged, according to the residual estimate
         }
         double* vnext = V + (long long)(j+1)*n;
         for (i=0; i<n; i++) {
            vnext[i] = w[i]/hn;
         }
      }
      if (status != 0) {
         break;
      }
      // x += Z y, where y solves the k x k triangular system H y = g
      for (i=k-1; i>=0; i--) {
         double s = g[i];
         for (l=i+1; l<k; l++) {
            s -= HH(i,l)*y[l];
         }
         y[i] = s/HH(i,i);
      }
      for (l=0; l<k; l++) {
         double* zl = Z + (long long)l*n;
         for (i=0; i<n; i++) {
            x[i] += y[l]*zl[i];
         }
      }
      beta = trueResidual (r, x, b);
      if (beta >= 0 && beta <= tolRes) {
         status = 1;
      }
      else if (!(beta >= 0) || nsolves >= maxSolves || !(beta < 0.9*prevBeta)) {
         // failed, out of solves, or stagnating over a restart cycle
         status = -1;
      }
      prevBeta = beta;
   }
   #undef HH
   free (H);
   free (cs);
   free (sn);
   free (g);
   free (y);
   myLastIterativeSolves = nsolves;
   myLastIterativeResidual = (beta >= 0 ? beta/bnorm : -1);
   return (status == 1 ? nsolves : 0);
}

int HybridSolver::cgsSolve (
   double* x, const double* b, double tol, int maxSolves)
{
   int n = myIterSize;
   if (ensureWork (8LL*n) != 0) {
      return -1;
   }
   double* r  = myWork;
   double* rt = r + n;     // shadow residual
   double* p  = r + 2LL*n;
   double* q  = r + 3LL*n;
   double* u  = r + 4LL*n;
   double* ph = r + 5LL*n; // M^{-1} p
   double* uh = r + 6LL*n; // M^{-1} (u + q)
   double* t  = r + 7LL*n;

   double bnorm = norm2 (b, n);
   double tolRes = tol*bnorm;
   memset (x, 0, n*sizeof(double));
   memcpy (r, b, n*sizeof(double));
   memcpy (rt, b, n*sizeof(double));
   double rho0 = 1.0;
   double rnorm = bnorm;
   int nsolves = 0;
   int iter = 0;       // iterations since the last (re)start
   int totalIters = 0;
   int status = 0;
   int i;

   while (status == 0) {
      if (nsolves+2 > maxSolves) {
         status = -1;
         break;
      }
      double rho = dot (rt, r, n);
      if (rho == 0 || !std::isfinite (rho)) {
         status = -1;
         break;
      }
      if (iter == 0) {
         memcpy (u, r, n*sizeof(double));
         memcpy (p, u, n*sizeof(double));
      }
      else {
         double beta = rho/rho0;
         for (i=0; i<n; i++) {
            u[i] = r[i] + beta*q[i];
            p[i] = u[i] + beta*(q[i] + beta*p[i]);
         }
      }
      if (timedPrecondSolve (ph, p) < 0 || matVec (t, ph) != 0) {
         status = -1;
         break;
      }
      nsolves++;
      double sigma = dot (rt, t, n);
      if (sigma == 0 || !std::isfinite (sigma)) {
         status = -1;
         break;
      }
      double alpha = rho/sigma;
      for (i=0; i<n; i++) {
         q[i] = u[i] - alpha*t[i];
         t[i] = u[i] + q[i];
      }
      if (timedPrecondSolve (uh, t) < 0 || matVec (t, uh) != 0) {
         status = -1;
         break;
      }
      nsolves++;
      for (i=0; i<n; i++) {
         x[i] += alpha*uh[i];
         r[i] -= alpha*t[i];
      }
      rho0 = rho;
      iter++;
      totalIters++;
      rnorm = norm2 (r, n);
      if (!std::isfinite (rnorm) || rnorm > 1e8*bnorm) {
         status = -1;
         break;
      }
      if (rnorm <= tolRes) {
         // the recursively updated residual can drift from the true one, so
         // check the latter, and restart from it if it is not yet small enough
         rnorm = trueResidual (r, x, b);
         if (rnorm >= 0 && rnorm <= tolRes) {
            status = 1;
         }
         else if (!(rnorm >= 0)) {
            status = -1;
         }
         else {
            memcpy (rt, r, n*sizeof(double));
            iter = 0;
            rho0 = 1.0;
         }
      }
   }
   if (status != 1) {
      rnorm = trueResidual (t, x, b);
   }
   myLastIterativeSolves = nsolves;
   myLastIterativeResidual = (rnorm >= 0 ? rnorm/bnorm : -1);
   return (status == 1 ? totalIters : 0);
}

int HybridSolver::iterate (
   int method, double* x, const double* b, double tol, int maxSolves,
   int restart)
{
   if (method == HYBRID_CGS) {
      return cgsSolve (x, b, tol, maxSolves);
   }
   else {
      return gmresSolve (x, b, tol, maxSolves, restart);
   }
}

int HybridSolver::iterativeSolve (
   const double* vals, double* x, const double* b, double tol,
   int method, int maxSolves, int restart)
{
   double t0 = wallTime();
   myLastIterativeSolves = 0;
   myLastIterativeResidual = -1;
   myIterTotalTime = 0;
   myIterSolveTime = 0;
   myIterMatVecTime = 0;
   if (myIterSize == 0) {
      return 0;
   }
   if (vals != myIterCurVals) {
      // not already copied into the buffer from getIterativeBuffers()
      memcpy (myIterCurVals, vals, myIterNumVals*sizeof(double));
   }
   if (norm2 (b, myIterSize) == 0) {
      memset (x, 0, myIterSize*sizeof(double));
      myLastIterativeResidual = 0;
      myIterTotalTime = wallTime()-t0;
      return 1;
   }
   if (maxSolves <= 0) {
      maxSolves = 100;
   }
   int iters = 0;
   if (beginIterations() >= 0) {
      iters = iterate (method, x, b, tol, maxSolves, restart);
      endIterations();
   }
   myIterTotalTime = wallTime()-t0;
   return (iters > 0 ? iters : 0);
}

int HybridSolver::getIterativeBuffers (double** vals, double** x, double** b)
{
   if (myIterSize == 0) {
      return -1;
   }
   if (myIterSize > myIterVecSize) {
      free (myIterX);
      free (myIterB);
      myIterX = (double*)malloc (myIterSize*sizeof(double));
      myIterB = (double*)malloc (myIterSize*sizeof(double));
      if (myIterX == NULL || myIterB == NULL) {
         free (myIterX);
         free (myIterB);
         myIterX = NULL;
         myIterB = NULL;
         myIterVecSize = 0;
         return -13;
      }
      myIterVecSize = myIterSize;
   }
   *vals = myIterCurVals;
   *x = myIterX;
   *b = myIterB;
   return 0;
}

int HybridSolver::getIterativeSize()
{
   return myIterSize;
}

int HybridSolver::getIterativeNumVals()
{
   return myIterNumVals;
}

int HybridSolver::getLastIterativeSolves()
{
   return myLastIterativeSolves;
}

double HybridSolver::getLastIterativeResidual()
{
   return myLastIterativeResidual;
}

void HybridSolver::getLastIterativeTimes (double* times)
{
   times[0] = myIterTotalTime;
   times[1] = myIterSolveTime;
   times[2] = myIterMatVecTime;
}
