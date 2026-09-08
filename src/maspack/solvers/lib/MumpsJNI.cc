/* -------------------------------------------------------------------- */
/* JNI layer connecting maspack.solvers.MumpsSolver to the MUMPS solver. */
/* All the real work is done by the Mumps class in mumps.cc; this file   */
/* simply unpacks the Java arguments.                                    */
/* -------------------------------------------------------------------- */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>

#include "mumps.h"
#include "maspack_solvers_MumpsSolver.h"

JNIEXPORT jlong JNICALL Java_maspack_solvers_MumpsSolver_doInit (
   JNIEnv *env, jobject obj)
{
   return (jlong)(new Mumps());
}

JNIEXPORT void JNICALL Java_maspack_solvers_MumpsSolver_doRelease (
   JNIEnv *env, jobject obj, jlong handle)
{
   Mumps* mumps = (Mumps*)handle;
   if (mumps != NULL) {
      delete mumps;
   }
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doGetInitError (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getInitError();
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doGetLastErrorInfo (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getLastErrorInfo();
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doSetNumThreads (
   JNIEnv *env, jobject obj, jlong handle, jint num)
{
   return ((Mumps*)handle)->setNumThreads (num);
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doGetNumThreads (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNumThreads();
}

/* --- statistics --- */

JNIEXPORT jlong JNICALL
Java_maspack_solvers_MumpsSolver_doGetNumNonZerosInFactors (
   JNIEnv *env, jobject obj, jlong handle)
{
   return (jlong)((Mumps*)handle)->getNumNonZerosInFactors();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetNumNegEigenvalues (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNumNegEigenvalues();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetNumPosEigenvalues (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNumPosEigenvalues();
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doGetNumTinyPivots (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNumTinyPivots();
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doGetNumNullPivots (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNumNullPivots();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetNumDelayedPivots (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNumDelayedPivots();
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doGetFirstNullPivot (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getFirstNullPivot();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetAnalysisMemoryUsage (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getAnalysisMemoryUsage();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetPeakAnalysisMemoryUsage (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getPeakAnalysisMemoryUsage();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetFactorSolveMemoryUsage (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getFactorSolveMemoryUsage();
}

/* --- control settings --- */

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doSetMaxRefinementSteps (
   JNIEnv *env, jobject obj, jlong handle, jint nsteps)
{
   return ((Mumps*)handle)->setMaxRefinementSteps (nsteps);
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetMaxRefinementSteps (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getMaxRefinementSteps();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetNumRefinementSteps (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNumRefinementSteps();
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doSetReorderMethod (
   JNIEnv *env, jobject obj, jlong handle, jint method)
{
   return ((Mumps*)handle)->setReorderMethod (method);
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doGetReorderMethod (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getReorderMethod();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetReorderMethodUsed (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getReorderMethodUsed();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doSetStaticPivotTolerance (
   JNIEnv *env, jobject obj, jlong handle, jdouble tol)
{
   return ((Mumps*)handle)->setStaticPivotTolerance (tol);
}

JNIEXPORT jdouble JNICALL
Java_maspack_solvers_MumpsSolver_doGetStaticPivotTolerance (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getStaticPivotTolerance();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doSetNullPivotDetection (
   JNIEnv *env, jobject obj, jlong handle, jint enable)
{
   return ((Mumps*)handle)->setNullPivotDetection (enable);
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetNullPivotDetection (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNullPivotDetection();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doSetNullPivotThreshold (
   JNIEnv *env, jobject obj, jlong handle, jdouble thresh)
{
   return ((Mumps*)handle)->setNullPivotThreshold (thresh);
}

JNIEXPORT jdouble JNICALL
Java_maspack_solvers_MumpsSolver_doGetNullPivotThreshold (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getNullPivotThreshold();
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doSetApplyScaling (
   JNIEnv *env, jobject obj, jlong handle, jint apply)
{
   return ((Mumps*)handle)->setApplyScaling (apply);
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doGetApplyScaling (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getApplyScaling();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doSetApplyWeightedMatchings (
   JNIEnv *env, jobject obj, jlong handle, jint apply)
{
   return ((Mumps*)handle)->setApplyWeightedMatchings (apply);
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetApplyWeightedMatchings (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getApplyWeightedMatchings();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doSetSymOrderingStrategy (
   JNIEnv *env, jobject obj, jlong handle, jint strategy)
{
   return ((Mumps*)handle)->setSymOrderingStrategy (strategy);
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetSymOrderingStrategy (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getSymOrderingStrategy();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doSetWorkspaceIncrease (
   JNIEnv *env, jobject obj, jlong handle, jint percent)
{
   return ((Mumps*)handle)->setWorkspaceIncrease (percent);
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetWorkspaceIncrease (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getWorkspaceIncrease();
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doSetMaxWorkingMemory (
   JNIEnv *env, jobject obj, jlong handle, jint mbytes)
{
   return ((Mumps*)handle)->setMaxWorkingMemory (mbytes);
}

JNIEXPORT jint JNICALL
Java_maspack_solvers_MumpsSolver_doGetMaxWorkingMemory (
   JNIEnv *env, jobject obj, jlong handle)
{
   return ((Mumps*)handle)->getMaxWorkingMemory();
}

/* --- analyze, factor and solve --- */

static int doSetMatrix (
   JNIEnv *env, Mumps *mumps, jdoubleArray jvals, jintArray jrowOffs,
   jintArray jcolIdxs, jint size, jint numVals, int sym)
{
   // if jint and int are not the same size, we have problems
   if (sizeof(int) != sizeof(jint)) {
      printf ("MumpsJNI.cc: sizeof(jint)=%zd, sizeof(int)=%zd, aborting...\n",
              sizeof(jint), sizeof(int));
      return -1;
   }
   jboolean isCopy;
   double *vals = env->GetDoubleArrayElements (jvals, &isCopy);
   int *rowOffs = (int*)env->GetIntArrayElements (jrowOffs, &isCopy);
   int *colIdxs = (int*)env->GetIntArrayElements (jcolIdxs, &isCopy);

   int retcode = mumps->setMatrix (
      vals, rowOffs, colIdxs, size, numVals, sym);

   env->ReleaseIntArrayElements (jcolIdxs, (jint*)colIdxs, JNI_ABORT);
   env->ReleaseIntArrayElements (jrowOffs, (jint*)rowOffs, JNI_ABORT);
   env->ReleaseDoubleArrayElements (jvals, vals, JNI_ABORT);
   return retcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doSetMatrix (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals, jintArray jrowOffs, jintArray jcolIdxs,
   jint size, jint numVals)
{
   return doSetMatrix (
      env, (Mumps*)handle, jvals, jrowOffs, jcolIdxs, size, numVals,
      MUMPS_UNSYMMETRIC);
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doSetSPDMatrix (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals, jintArray jrowOffs, jintArray jcolIdxs,
   jint size, jint numVals)
{
   return doSetMatrix (
      env, (Mumps*)handle, jvals, jrowOffs, jcolIdxs, size, numVals,
      MUMPS_SPD);
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doSetSymmetricMatrix (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals, jintArray jrowOffs, jintArray jcolIdxs,
   jint size, jint numVals)
{
   return doSetMatrix (
      env, (Mumps*)handle, jvals, jrowOffs, jcolIdxs, size, numVals,
      MUMPS_SYMMETRIC);
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doFactorMatrix (
   JNIEnv *env, jobject obj, jlong handle, jdoubleArray jvals)
{
   Mumps* mumps = (Mumps*)handle;
   jboolean isCopy;

   double *vals = env->GetDoubleArrayElements (jvals, &isCopy);
   int retcode = mumps->factorMatrix (vals);
   env->ReleaseDoubleArrayElements (jvals, vals, JNI_ABORT);
   return retcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doSolve__J_3D_3D (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jxvec, jdoubleArray jbvec)
{
   Mumps* mumps = (Mumps*)handle;
   jboolean isCopy;

   double *bvec = env->GetDoubleArrayElements (jbvec, &isCopy);
   double *xvec = env->GetDoubleArrayElements (jxvec, &isCopy);

   int retcode = mumps->solveMatrix (xvec, bvec);

   env->ReleaseDoubleArrayElements (jxvec, xvec, 0);
   env->ReleaseDoubleArrayElements (jbvec, bvec, JNI_ABORT);
   return retcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_MumpsSolver_doSolve__J_3D_3DI (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jxvec, jdoubleArray jbvec, jint nrhs)
{
   Mumps* mumps = (Mumps*)handle;
   jboolean isCopy;

   double *bvec = env->GetDoubleArrayElements (jbvec, &isCopy);
   double *xvec = env->GetDoubleArrayElements (jxvec, &isCopy);

   int retcode = mumps->solveMatrix (xvec, bvec, nrhs);

   env->ReleaseDoubleArrayElements (jxvec, xvec, 0);
   env->ReleaseDoubleArrayElements (jbvec, bvec, JNI_ABORT);
   return retcode;
}
