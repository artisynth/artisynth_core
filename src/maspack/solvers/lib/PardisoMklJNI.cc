#include <jni.h>
#include "pardisoMkl.h"
#include "mkl_service.h"
#include "maspack_solvers_PardisoSolver.h"

#include <stdlib.h>
#include <stdio.h>
// #include <pthread.h>
#include <time.h>

#ifdef LINUX
#include <unistd.h>
#endif

JNIEXPORT jlong JNICALL Java_maspack_solvers_PardisoSolver_doInit (
   JNIEnv *env, jobject obj)
{
   jlong handle = (jlong)(new Pardiso4());
   return handle;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doGetInitError (
   JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getInitError();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetNumThreads (
      JNIEnv *env, jobject obj, jlong handle, jint num)
{
   // handle is ignored since setting is global
   int prev = mkl_domain_get_max_threads (MKL_DOMAIN_PARDISO);
   if (num < 0) {
      num = 0;
   }
   mkl_domain_set_num_threads (num, MKL_DOMAIN_PARDISO);
   return prev;
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetNumThreads (
      JNIEnv *env, jobject obj, jlong handle)
{
   // handle is ignored since setting is global
   return mkl_domain_get_max_threads (MKL_DOMAIN_PARDISO);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetMaxRefinementSteps (
      JNIEnv *env, jobject obj, jlong handle, jint nsteps)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->setMaxRefinementSteps (nsteps);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetMaxRefinementSteps (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getMaxRefinementSteps ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetNumRefinementSteps (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getNumRefinementSteps ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetNumNegEigenvalues (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getNumNegEigenvalues ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetNumPosEigenvalues (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getNumPosEigenvalues ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetNumPerturbedPivots (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getNumPerturbedPivots ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetSPDZeroPivot (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getSPDZeroPivot ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetPeakAnalysisMemoryUsage (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getPeakAnalysisMemoryUsage ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetAnalysisMemoryUsage (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getAnalysisMemoryUsage ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetFactorSolveMemoryUsage (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getFactorSolveMemoryUsage ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetReorderMethod (
      JNIEnv *env, jobject obj, jlong handle, jint method)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->setReorderMethod (method);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetReorderMethod (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getReorderMethod ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetPivotPerturbation (
      JNIEnv *env, jobject obj, jlong handle, jint perturb)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->setPivotPerturbation (perturb);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetPivotPerturbation (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getPivotPerturbation ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetApplyScaling (
      JNIEnv *env, jobject obj, jlong handle, jint nsteps)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->setApplyScaling (nsteps);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetApplyScaling (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getApplyScaling ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetApplyWeightedMatchings (
      JNIEnv *env, jobject obj, jlong handle, jint apply)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->setApplyWeightedMatchings (apply);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetApplyWeightedMatchings (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getApplyWeightedMatchings ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetUse2x2Pivoting (
      JNIEnv *env, jobject obj, jlong handle, jint enable)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->setUse2x2Pivoting (enable);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetUse2x2Pivoting (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getUse2x2Pivoting ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetMatrixChecking (
      JNIEnv *env, jobject obj, jlong handle, jint enable)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->setMatrixChecking (enable);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetMatrixChecking (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getMatrixChecking ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doSetMessageLevel (
      JNIEnv *env, jobject obj, jlong handle, jint level)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->setMessageLevel (level);
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetMessageLevel (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getMessageLevel ();
}

JNIEXPORT jint JNICALL
   Java_maspack_solvers_PardisoSolver_doGetNumNonZerosInFactors (
      JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return pardiso->getNumNonZerosInFactors ();
}

static int doSetMatrix (
   JNIEnv *env, Pardiso4 *pardiso, jdoubleArray jvals, jintArray jrowIdxs, 
   jintArray jcolIdxs, jint size, jint numVals, int type)
{
	// if jint and int are not same size, we have problems
	if (sizeof(int) != sizeof(jint))
	 { printf(
	      "PardisoJNI.cc: sizeof(jint)=%zd, sizeof(int)=%zd, aborting...\n", 
	      sizeof(jint), sizeof(int));
	   return -1;
	 }
	
	jboolean isCopy;
	double *vals = env->GetDoubleArrayElements (jvals, &isCopy);
	int *rowsIdxs = (int*)env->GetIntArrayElements (jrowIdxs, &isCopy);
	int *colsIdxs = (int*)env->GetIntArrayElements (jcolIdxs, &isCopy);

	int retcode = pardiso->setMatrix (
	   vals, rowsIdxs, colsIdxs, size, numVals, type);

	env->ReleaseIntArrayElements (jcolIdxs, (jint*)colsIdxs, JNI_ABORT);
	env->ReleaseIntArrayElements (jrowIdxs, (jint*)rowsIdxs, JNI_ABORT);
	env->ReleaseDoubleArrayElements (jvals, vals, JNI_ABORT);
	return retcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doSetMatrix (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals, jintArray jrowIdxs, jintArray jcolIdxs,
   jint size, jint numVals)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return doSetMatrix (
	   env, pardiso, jvals, jrowIdxs,
	   jcolIdxs, size, numVals, REAL_UNSYMMETRIC);
}

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doSetSPDMatrix (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals, jintArray jrowIdxs, jintArray jcolIdxs,
   jint size, jint numVals)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	return doSetMatrix (
	   env, pardiso, jvals, jrowIdxs, jcolIdxs, size,
	   numVals, REAL_SYMMETRIC_POSDEF);
}

// pthread_t Pardiso_busyThread;

// void *Pardiso_busybusy (void *arg)
// {
// 	struct timespec sleepTime;
// 	sleepTime.tv_sec = 0;
// 	sleepTime.tv_nsec = 5000000; // 5 msec

// 	while (1)
// 	 { printf ("busy busy\n");
// 	   nanosleep (&sleepTime, NULL);
// 	 }
// 	return 0;
// }

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doSetSymmetricMatrix (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals, jintArray jrowIdxs, jintArray jcolIdxs,
   jint size, jint numVals)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	int retval = 0;

 	retval = doSetMatrix (
 	   env, pardiso, jvals, jrowIdxs, jcolIdxs, size,
 	   numVals, REAL_SYMMETRIC);
// 	pthread_t busyThread;

// 	if (pthread_create (&Pardiso_busyThread, (pthread_attr_t*)0,
// 			    Pardiso_busybusy, (void*)0) != 0)
// 	 { perror("can't creat thread");
// 	   exit(1);
// 	 }
//	pardiso->setSize(size);
	return retval;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doFactorMatrix__J_3D (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	jboolean isCopy;
	int retcode = 0;

	double *vals = env->GetDoubleArrayElements (jvals, &isCopy);
	retcode = pardiso->factorMatrix (vals);
	env->ReleaseDoubleArrayElements (jvals, vals, JNI_ABORT);
	return retcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doFactorMatrix__J (
   JNIEnv *env, jobject obj, jlong handle)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	int retcode = 0;
	retcode = pardiso->factorMatrix ();
	return retcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doFactorAndSolve (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals, jdoubleArray jxvec, jdoubleArray jbvec, jint tolExp)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	jboolean isCopy;
	int retcode = 0;
        int size = pardiso->getSize();
	int i;

	double *vals = env->GetDoubleArrayElements (jvals, &isCopy);
        double *bvec = env->GetDoubleArrayElements (jbvec, &isCopy);
	for (i=0; i<size; i++)
	 { pardiso->myB[i] = bvec[i];
	 }
	env->ReleaseDoubleArrayElements (jbvec, bvec, JNI_ABORT);
        
	retcode = pardiso->factorAndSolve (
           vals, pardiso->myX, pardiso->myB, tolExp);

	double *xvec = env->GetDoubleArrayElements (jxvec, &isCopy);
	for (i=0; i<size; i++)
	 { xvec[i] = pardiso->myX[i];
	 }
	env->ReleaseDoubleArrayElements (jxvec, xvec, 0);

	env->ReleaseDoubleArrayElements (jvals, vals, JNI_ABORT);
	return retcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doSolve (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jxvec, jdoubleArray jbvec)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	jboolean isCopy;
	int retcode = 0;
	int size = pardiso->getSize();
	int i;

	double *bvec = env->GetDoubleArrayElements (jbvec, &isCopy);
	for (i=0; i<size; i++)
	 { pardiso->myB[i] = bvec[i];
	 }
	env->ReleaseDoubleArrayElements (jbvec, bvec, JNI_ABORT);

	retcode = pardiso->solveMatrix (pardiso->myX, pardiso->myB);

	double *xvec = env->GetDoubleArrayElements (jxvec, &isCopy);
	for (i=0; i<size; i++)
	 { xvec[i] = pardiso->myX[i];
	 }
	env->ReleaseDoubleArrayElements (jxvec, xvec, 0);
// 	// DEBUG
// 	double *xvec = env->GetDoubleArrayElements (jxvec, &isCopy);
// 	for (i=0; i<size; i++)
// 	 { xvec[i] = 0;
// 	 }
// 	env->ReleaseDoubleArrayElements (jxvec, xvec, 0);

	return retcode;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_PardisoSolver_doIterativeSolve (
   JNIEnv *env, jobject obj, jlong handle,
   jdoubleArray jvals, jdoubleArray jxvec, jdoubleArray jbvec, jint tolExp)
{
	Pardiso4* pardiso = (Pardiso4*)handle;
	jboolean isCopy;
	int retcode = 0;
	int size = pardiso->getSize();
	int i;

	double *vals = env->GetDoubleArrayElements (jvals, &isCopy);
        double *bvec = env->GetDoubleArrayElements (jbvec, &isCopy);
	for (i=0; i<size; i++)
	 { pardiso->myB[i] = bvec[i];
	 }
	env->ReleaseDoubleArrayElements (jbvec, bvec, JNI_ABORT);

	// NOTE: myX will contain the most recent x values set by either
	// iterativeSolve, factorAndSolve, or solveMatrix.
	retcode = pardiso->iterativeSolve (
           vals, pardiso->myX, pardiso->myB, tolExp);

	double *xvec = env->GetDoubleArrayElements (jxvec, &isCopy);
	for (i=0; i<size; i++)
	 { xvec[i] = pardiso->myX[i];
	 }
	env->ReleaseDoubleArrayElements (jxvec, xvec, 0);
// 	// DEBUG
// 	double *xvec = env->GetDoubleArrayElements (jxvec, &isCopy);
// 	for (i=0; i<size; i++)
// 	 { xvec[i] = 0;
// 	 }
// 	env->ReleaseDoubleArrayElements (jxvec, xvec, 0);
        env->ReleaseDoubleArrayElements (jvals, vals, JNI_ABORT);

	return retcode;
}

JNIEXPORT void JNICALL Java_maspack_solvers_PardisoSolver_doRelease (
   JNIEnv *env, jobject obj, jlong handle)
{
	if (handle != 0)
	 { 
	   delete (Pardiso4*)handle; 
	 }
}

/**
 * This method is a hook that gives us access to _exit(), which is
 * is in turn needed to exit ArtiSynth on the MacBook Pro 8.2
 * version of Ubuntu, since otherwise the JVM exit process encounters
 * a SEGV in XQueryExtension. This method was put into PardisoSolver
 * purely ; it has nothing to do with Pardiso per-se.
 */
JNIEXPORT void JNICALL Java_maspack_solvers_PardisoSolver_doExit (
   JNIEnv *env, jobject obj, jint code)
{
#ifdef LINUX   
   _exit (code);
#endif
}
