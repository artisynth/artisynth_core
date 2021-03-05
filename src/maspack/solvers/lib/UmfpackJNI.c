#include <jni.h>
#include <umfpack.h>
#include "maspack_solvers_UmfpackSolver.h"

#include <iostream>

JNIEXPORT jint JNICALL Java_maspack_solvers_UmfpackSolver_umfpack_1di_1symbolic(JNIEnv *env, jobject obj, jint n_row, jint n_col, jintArray Ap, jintArray Ai, jdoubleArray Ax, jlongArray Sym, jdoubleArray Control, jdoubleArray Info)
{
	jboolean isCopy;

	int *cAp = (int*)env->GetIntArrayElements(Ap, &isCopy);
	int *cAi = (int*)env->GetIntArrayElements(Ai, &isCopy);
	double *cAx = (Ax == NULL)?NULL:env->GetDoubleArrayElements(Ax, &isCopy);
	long *cSym = (long*)env->GetLongArrayElements(Sym, &isCopy);
	double* cControl = (Control == NULL)?NULL:env->GetDoubleArrayElements(Control, &isCopy);
	double* cInfo = (Info == NULL)?NULL:env->GetDoubleArrayElements(Info, &isCopy);
	
	int r = umfpack_di_symbolic(n_row, n_col, cAp, cAi, cAx, (void**)cSym, cControl, cInfo);
	
	env->ReleaseIntArrayElements(Ap, (jint*)cAp, JNI_ABORT);
	env->ReleaseIntArrayElements(Ai, (jint*)cAi, JNI_ABORT);
	if(cAx != NULL) env->ReleaseDoubleArrayElements(Ax, cAx, JNI_ABORT);
	env->ReleaseLongArrayElements(Sym, (jlong*)cSym, 0);
	if(cControl != NULL) env->ReleaseDoubleArrayElements(Control, cControl, 0);
	if(cInfo != NULL) env->ReleaseDoubleArrayElements(Info, cInfo, 0);
	
	return r;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_UmfpackSolver_umfpack_1di_1numeric(JNIEnv *env, jobject obj, jintArray Ap, jintArray Ai, jdoubleArray Ax, jlongArray Sym, jlongArray Num, jdoubleArray Control, jdoubleArray Info)
{
	jboolean isCopy;

	int *cAp = (int*)env->GetIntArrayElements(Ap, &isCopy);
	int *cAi = (int*)env->GetIntArrayElements(Ai, &isCopy);
	double *cAx = env->GetDoubleArrayElements(Ax, &isCopy);
	long *cSym = (long*)env->GetLongArrayElements(Sym, &isCopy);
	long *cNum = (long*)env->GetLongArrayElements(Num, &isCopy);
	double* cControl = (Control == NULL)?NULL:env->GetDoubleArrayElements(Control, &isCopy);
	double* cInfo = (Info == NULL)?NULL:env->GetDoubleArrayElements(Info, &isCopy);
	
	int r = umfpack_di_numeric(cAp, cAi, cAx, (void*)*cSym, (void**)cNum, cControl, cInfo);
	
	env->ReleaseIntArrayElements(Ap, (jint*)cAp, JNI_ABORT);
	env->ReleaseIntArrayElements(Ai, (jint*)cAi, JNI_ABORT);
	env->ReleaseDoubleArrayElements(Ax, cAx, JNI_ABORT);
	env->ReleaseLongArrayElements(Sym, (jlong*)cSym, JNI_ABORT);
	env->ReleaseLongArrayElements(Num, (jlong*)cNum, 0);
	if(cControl != NULL) env->ReleaseDoubleArrayElements(Control, cControl, 0);
	if(cInfo != NULL) env->ReleaseDoubleArrayElements(Info, cInfo, 0);
	
	return r;
}

JNIEXPORT jint JNICALL Java_maspack_solvers_UmfpackSolver_umfpack_1di_1solve(JNIEnv *env, jobject obj, jint sys, jintArray Ap, jintArray Ai, jdoubleArray Ax, jdoubleArray X, jdoubleArray B, jlongArray Num, jdoubleArray Control, jdoubleArray Info)
{
	jboolean isCopy;

	int *cAp = (int*)env->GetIntArrayElements(Ap, &isCopy);
	int *cAi = (int*)env->GetIntArrayElements(Ai, &isCopy);
	double *cAx = env->GetDoubleArrayElements(Ax, &isCopy);
	double *cX = env->GetDoubleArrayElements(X, &isCopy);
	double *cB = env->GetDoubleArrayElements(B, &isCopy);
	long *cNum = (long*)env->GetLongArrayElements(Num, &isCopy);
	double* cControl = (Control == NULL)?NULL:env->GetDoubleArrayElements(Control, &isCopy);
	double* cInfo = (Info == NULL)?NULL:env->GetDoubleArrayElements(Info, &isCopy);
	
	int r = umfpack_di_solve(sys, cAp, cAi, cAx, cX, cB, (void*)*cNum, cControl, cInfo);
	
	env->ReleaseIntArrayElements(Ap, (jint*)cAp, JNI_ABORT);
	env->ReleaseIntArrayElements(Ai, (jint*)cAi, JNI_ABORT);
	env->ReleaseDoubleArrayElements(Ax, cAx, JNI_ABORT);
	env->ReleaseDoubleArrayElements(X, cX, 0);
	env->ReleaseDoubleArrayElements(B, cB, JNI_ABORT);
	env->ReleaseLongArrayElements(Num, (jlong*)cNum, JNI_ABORT);
	if(cControl != NULL) env->ReleaseDoubleArrayElements(Control, cControl, 0);
	if(cInfo != NULL) env->ReleaseDoubleArrayElements(Info, cInfo, 0);
	
	return r;
}

JNIEXPORT void JNICALL Java_maspack_solvers_UmfpackSolver_umfpack_1di_1free_1symbolic(JNIEnv *env, jobject obj, jlongArray Sym)
{
	jboolean isCopy;
	long *cSym = (long*)env->GetLongArrayElements(Sym, &isCopy);
	
	umfpack_di_free_symbolic((void**)cSym);
	
	env->ReleaseLongArrayElements(Sym, (jlong*)cSym, 0);
}

JNIEXPORT void JNICALL Java_maspack_solvers_UmfpackSolver_umfpack_1di_1free_1numeric(JNIEnv *env, jobject obj, jlongArray Num)
{
	jboolean isCopy;
	long *cNum = (long*)env->GetLongArrayElements(Num, &isCopy);
	
	umfpack_di_free_numeric((void**)cNum);
	
	env->ReleaseLongArrayElements(Num, (jlong*)cNum, 0);
}

JNIEXPORT void JNICALL Java_maspack_solvers_UmfpackSolver_umfpack_1di_1defaults(JNIEnv *env, jobject obj, jdoubleArray Control)
{
	jboolean isCopy;
	double* cControl = (Control == NULL)?NULL:env->GetDoubleArrayElements(Control, &isCopy);

	umfpack_di_defaults(cControl);
	
	if(cControl != NULL) env->ReleaseDoubleArrayElements(Control, cControl, 0);
}
