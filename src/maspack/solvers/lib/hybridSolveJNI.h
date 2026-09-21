/*
 * JNI access for HybridSolver::iterativeSolve(), shared by the Pardiso and
 * MUMPS JNI wrappers. Header only, so that no extra object files are needed.
 *
 * Two ways of accessing the Java arrays are supported:
 *
 * - Get/ReleaseDoubleArrayElements, held for the duration of the solve.
 *   HotSpot returns copies, so the value array is allocated and copied, and
 *   then copied again into the solver's current-value storage.
 *
 * - GetPrimitiveArrayCritical, used only to copy vals and b into the
 *   solver's native buffers, and the result back into x. Each copy is a
 *   single memcpy, with no allocation. The critical region is never held
 *   during the solve: the JNI specification forbids JNI calls and blocking
 *   inside it, and on JDK 21 and earlier an open critical region (the GC
 *   locker) defers garbage collection for all threads, so a long region
 *   could stall other threads or cause allocation failures. (JDK 22 G1 pins
 *   regions instead, which needs no change here.) The code works whether or
 *   not the JVM returns a copy, and falls back to Get/SetDoubleArrayRegion if
 *   critical access fails without a pending exception.
 */
#ifndef HYBRID_SOLVE_JNI_H
#define HYBRID_SOLVE_JNI_H

#include <string.h>
#include <jni.h>
#include "hybridSolve.h"

// Copies n values from a Java array into dst. Returns false if a Java
// exception is pending.
static inline bool hybridCopyFromJava (
   JNIEnv* env, jdoubleArray jarr, double* dst, jsize n)
{
   double* src = (double*)env->GetPrimitiveArrayCritical (jarr, NULL);
   if (src != NULL) {
      memcpy (dst, src, n*sizeof(double));
      env->ReleasePrimitiveArrayCritical (jarr, src, JNI_ABORT);
      return true;
   }
   if (env->ExceptionCheck()) {
      return false;
   }
   env->GetDoubleArrayRegion (jarr, 0, n, dst);
   return !env->ExceptionCheck();
}

// Copies n values from src into a Java array. Returns false if a Java
// exception is pending.
static inline bool hybridCopyToJava (
   JNIEnv* env, jdoubleArray jarr, const double* src, jsize n)
{
   double* dst = (double*)env->GetPrimitiveArrayCritical (jarr, NULL);
   if (dst != NULL) {
      memcpy (dst, src, n*sizeof(double));
      env->ReleasePrimitiveArrayCritical (jarr, dst, 0);
      return true;
   }
   if (env->ExceptionCheck()) {
      return false;
   }
   env->SetDoubleArrayRegion (jarr, 0, n, src);
   return !env->ExceptionCheck();
}

// Performs solver->iterativeSolve() on Java arrays, using critical copies if
// critical is true (see above). Returns the number of iterations on success,
// or 0 on failure (in which case a Java exception may be pending).
static inline jint hybridSolveJNI (
   JNIEnv* env, HybridSolver* solver, jdoubleArray jvals, jdoubleArray jxvec,
   jdoubleArray jbvec, jdouble tol, jint method, jint maxSolves, jint restart,
   jboolean critical)
{
   if (!critical) {
      jboolean isCopy;
      double *vals = env->GetDoubleArrayElements (jvals, &isCopy);
      double *bvec = env->GetDoubleArrayElements (jbvec, &isCopy);
      double *xvec = env->GetDoubleArrayElements (jxvec, &isCopy);

      int retcode = solver->iterativeSolve (
         vals, xvec, bvec, tol, method, maxSolves, restart);

      env->ReleaseDoubleArrayElements (jxvec, xvec, 0);
      env->ReleaseDoubleArrayElements (jbvec, bvec, JNI_ABORT);
      env->ReleaseDoubleArrayElements (jvals, vals, JNI_ABORT);
      return retcode;
   }
   double* vals;
   double* x;
   double* b;
   if (solver->getIterativeBuffers (&vals, &x, &b) < 0) {
      return 0;
   }
   int n = solver->getIterativeSize();
   if (!hybridCopyFromJava (env, jvals, vals, solver->getIterativeNumVals()) ||
       !hybridCopyFromJava (env, jbvec, b, n)) {
      return 0;
   }
   int retcode = solver->iterativeSolve (
      vals, x, b, tol, method, maxSolves, restart);
   hybridCopyToJava (env, jxvec, x, n);
   return retcode;
}

#endif
