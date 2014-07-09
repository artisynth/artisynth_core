#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include "IpoptTest.h"
#include "IpStdCInterface.h"
#include "maspack_ipopt_IpoptInterface.h"  

/* Ipopt NLP Callback Function Declarations */
Bool eval_f(Index n, Number* x, Bool new_x,
            Number* obj_value, UserDataPtr user_data);

Bool eval_grad_f(Index n, Number* x, Bool new_x,
                 Number* grad_f, UserDataPtr user_data);

Bool eval_g(Index n, Number* x, Bool new_x,
            Index m, Number* g, UserDataPtr user_data);

Bool eval_jac_g(Index n, Number *x, Bool new_x,
                Index m, Index nele_jac,
                Index *iRow, Index *jCol, Number *values,
                UserDataPtr user_data);

Bool eval_h(Index n, Number *x, Bool new_x, Number obj_factor,
            Index m, Number *lambda, Bool new_lambda,
            Index nele_hess, Index *iRow, Index *jCol,
            Number *values, UserDataPtr user_data);

IpoptProblem nlp = NULL;
bool debug = false;

jmethodID MID_callbackTest;
jmethodID MID_Eval_F;
jmethodID MID_Eval_Grad_F;
jmethodID MID_Eval_G;
jmethodID MID_Eval_Jac_G;
jmethodID MID_Eval_H;

JNIEnv *jniEnv; // JNI 
static jobject ipoptObj = NULL; // IpoptInteface object

JNIEXPORT void JNICALL 
Java_maspack_ipopt_IpoptInterface_initialize(JNIEnv *env, jclass cls)
{
	// cache JNI environment pointer
    jniEnv = env;
	
	// cache callback method IDs
	MID_callbackTest = env->GetMethodID(cls, "callbackTest", "(I)Z");
	if (MID_callbackTest == NULL) return; 
    MID_Eval_F = env->GetMethodID(cls, "Eval_F", "(I[DZ[D)Z");
	if (MID_Eval_F == NULL) return; 
    MID_Eval_Grad_F = env->GetMethodID(cls, "Eval_Grad_F", "(I[DZ[D)Z");
	if (MID_Eval_Grad_F == NULL) return; 
    MID_Eval_G = env->GetMethodID(cls, "Eval_G", "(I[DZI[D)Z");
	if (MID_Eval_G == NULL) return; 
    MID_Eval_Jac_G = env->GetMethodID(cls, "Eval_Jac_G", "(I[DZII[I[I[D)Z");
	if (MID_Eval_Jac_G == NULL) return; 
    MID_Eval_H = env->GetMethodID(cls, "Eval_H", "(I[DZDI[DZI[I[I[D)Z");
	if (MID_Eval_H == NULL) return; 
    
}

JNIEXPORT void JNICALL 
Java_maspack_ipopt_IpoptInterface_setObj(JNIEnv *env, jobject obj)
{
//    printf("Attemping to cache ipoptObj in native code\n");
	ipoptObj = env->NewGlobalRef(obj);
	if (ipoptObj == NULL) return; // exception already thrown
		    
//    if (env == jniEnv)
//      printf("cachedEnv same in second call\n");
}

JNIEXPORT void JNICALL
Java_maspack_ipopt_IpoptInterface_createNLP(JNIEnv *env, jobject obj,
	jint n, jdoubleArray x_L, jdoubleArray x_U, jint m,
	jdoubleArray g_L, jdoubleArray g_U, jint nele_jac, jint nele_hess,
	    jint index_style)
{
//	printf("create() - native code\n");
	jboolean isCopy;
	double *_g_L = env->GetDoubleArrayElements (g_L, &isCopy);
	double *_g_U = env->GetDoubleArrayElements (g_U, &isCopy);
	double *_x_L = env->GetDoubleArrayElements (x_L, &isCopy);
	double *_x_U = env->GetDoubleArrayElements (x_U, &isCopy);
	
	int i;
	nlp = CreateIpoptProblem(n, _x_L, _x_U, 
		m, _g_L, _g_U, 
		nele_jac, nele_hess, index_style, &eval_f, &eval_g, 
		&eval_grad_f, &eval_jac_g, &eval_h);
	if (nlp==NULL)
	{
  		printf("CreateIpoptProblem() failed...\n");
	  	jclass exception;
    	exception = env->FindClass("java/io/IOException");
    	if (exception == 0) return;
    	env->ThrowNew(exception, "Error while creating ipopt problem");
	}
		
	env->ReleaseDoubleArrayElements (g_L, _g_L, JNI_ABORT);
	env->ReleaseDoubleArrayElements (g_U, _g_U, JNI_ABORT);
	env->ReleaseDoubleArrayElements (x_L, _x_L, JNI_ABORT);
	env->ReleaseDoubleArrayElements (x_U, _x_U, JNI_ABORT);	
//	printf("CreateIpoptProblem() successful...\n");
}

JNIEXPORT void JNICALL
Java_maspack_ipopt_IpoptInterface_addIpoptIntOption(JNIEnv *env, 
	jobject obj, jstring name, jint val)
{
	char *_name = const_cast<char*>(env->GetStringUTFChars(name, 0));
	if (nlp != NULL)
	{
		AddIpoptIntOption(nlp, _name, val);
	}
	env->ReleaseStringUTFChars(name, _name);
	
}

JNIEXPORT void JNICALL
Java_maspack_ipopt_IpoptInterface_addIpoptNumOption(JNIEnv *env, 
	jobject obj, jstring name, jdouble val)
{
	char *_name = const_cast<char*>(env->GetStringUTFChars(name, 0));
	if (nlp != NULL)
	{
		AddIpoptNumOption(nlp, _name, val);
	}
	env->ReleaseStringUTFChars(name, _name);
	
}

JNIEXPORT void JNICALL
Java_maspack_ipopt_IpoptInterface_addIpoptStrOption(JNIEnv *env, 
	jobject obj, jstring name, jstring val)
{
	char *_name = const_cast<char*>(env->GetStringUTFChars(name, 0));
	char *_val = const_cast<char*>(env->GetStringUTFChars(val, 0));
	if (nlp != NULL)
	{
		AddIpoptStrOption(nlp, _name, _val);
	}
	env->ReleaseStringUTFChars(name, _name);
	env->ReleaseStringUTFChars(val, _val);
}

JNIEXPORT void JNICALL
Java_maspack_ipopt_IpoptInterface_solveNLP(JNIEnv *env, jobject obj,
	jdoubleArray x, jdoubleArray obj_factor, 
	jdoubleArray mult_x_L, jdoubleArray mult_x_U, jint n)
{
	enum ApplicationReturnStatus status; /* Solve return code */
	jboolean isCopy;
	double *_x = env->GetDoubleArrayElements (x, &isCopy);
	double *_mult_x_L = env->GetDoubleArrayElements (mult_x_L, &isCopy);
	double *_mult_x_U = env->GetDoubleArrayElements (mult_x_U, &isCopy);
	double *_obj_factor = env->GetDoubleArrayElements (obj_factor, &isCopy);
	int i;
	
	if (nlp != NULL)
	{
//	printf("IpoptSolve() - native code\n");
	status = IpoptSolve(nlp, _x, NULL, _obj_factor, NULL, 
						_mult_x_L, _mult_x_U, NULL);
//	printf("IpoptSolve() - complete\n");

	if (debug) {
	if (status == Solve_Succeeded) {
		
		printf("\n\nSolution of the primal variables, x\n");
	    for (i = 0; i < n; i++) {
		    printf("x[%d] = %e\n", i, _x[i]);	
	    }
    	printf("\n\nSolution of the bound multipliers, z_L and z_U\n");
	    for (i=0; i<n; i++) {
			printf("z_L[%d] = %e\n", i, _mult_x_L[i]);
	    }
    	for (i=0; i<n; i++) {
	    	printf("z_U[%d] = %e\n", i, _mult_x_U[i]);
    	}

	    printf("\n\nObjective value\n");
    	printf("f(x*) = %e\n", _obj_factor[0]);
	}
	}
 	 /* free allocated memory */
  	FreeIpoptProblem(nlp);
  	nlp = NULL;
	}
	 
	env->ReleaseDoubleArrayElements (x, _x, 0); // 0 = copy back
	env->ReleaseDoubleArrayElements (obj_factor, _obj_factor, 0); // 0 = copy back
	env->ReleaseDoubleArrayElements (mult_x_L, _mult_x_L, 0);  // 0 = copy back
	env->ReleaseDoubleArrayElements (mult_x_U, _mult_x_U, 0);  // 0 = copy back
}

            
/* Ipopt NLP Callback Function Implementations */
Bool eval_f(Index _n, Number* _x, Bool _new_x,
            Number* _obj_value, UserDataPtr user_data)
{
	jboolean isCopy;
	Bool status;
	int i;
//	printf("C callback: eval_f\n");
	if (ipoptObj == NULL || jniEnv == NULL)
	{
		printf("null jni env or obj\n");
		return false;
	}
	
	jint n = _n;
	jboolean new_x = _new_x;
	jdoubleArray x = jniEnv->NewDoubleArray(n);
	if (x == NULL) return false;
	double *x_buf = jniEnv->GetDoubleArrayElements (x, &isCopy);
	for (i=0; i<n; i++)
	{	x_buf[i]=_x[i];
//		printf("x[%d] = %e\n", i, _x[i]);
//		printf("x_buf[%d] = %e\n", i, x_buf[i]);
	}
	jniEnv->ReleaseDoubleArrayElements (x, x_buf, 0); // 0 = copy back
	
	jdoubleArray obj_value = jniEnv->NewDoubleArray(1);
	if (obj_value == NULL) return false;

//	printf("ipoptObj = %x\n", *ipoptObj);
	status = jniEnv->CallBooleanMethod(ipoptObj, MID_Eval_F, n, x, new_x, obj_value);
	
	double*	obj_value_buf = jniEnv->GetDoubleArrayElements (obj_value, &isCopy);
	*_obj_value = obj_value_buf[0];
	jniEnv->ReleaseDoubleArrayElements (obj_value, obj_value_buf, JNI_ABORT);

	return status;
}

Bool eval_grad_f(Index _n, Number* _x, Bool _new_x,
                 Number* _grad_f, UserDataPtr user_data)
{
	jboolean isCopy;
	Bool status;
	int i;
//	printf("C callback: eval_grad_f\n");
	if (ipoptObj == NULL || jniEnv == NULL)
	{
		printf("null jni env or obj\n");
		return false;
	}
	jint n = _n;
	jboolean new_x = _new_x;
	jdoubleArray x = jniEnv->NewDoubleArray(n);
	if (x == NULL) return false;
	double *x_buf = jniEnv->GetDoubleArrayElements (x, &isCopy);
	for (i=0; i<n; i++)
	{	x_buf[i]=_x[i];
	}
	jniEnv->ReleaseDoubleArrayElements (x, x_buf, 0); // 0 = copy back
	jdoubleArray grad_f = jniEnv->NewDoubleArray(n);
	if (grad_f == NULL) return false;

	status = jniEnv->CallBooleanMethod(ipoptObj, MID_Eval_Grad_F, n, x, new_x, grad_f);
	
	double*	grad_f_buf = jniEnv->GetDoubleArrayElements (grad_f, &isCopy);
	for (i=0; i<n; i++)
	{	_grad_f[i]=grad_f_buf[i];
	}
	jniEnv->ReleaseDoubleArrayElements (grad_f, grad_f_buf, JNI_ABORT);

	return status;

		
}
	
Bool eval_g(Index _n, Number* _x, Bool _new_x,
            Index _m, Number* _g, UserDataPtr user_data)
{
	jboolean isCopy;
	Bool status;
	int i;
//	printf("C callback: eval_g\n");
	if (ipoptObj == NULL || jniEnv == NULL)
	{
		printf("null jni env or obj\n");
		return false;
	}
	jint n = _n;
	jboolean new_x = _new_x;
	jint m = _m;
	jdoubleArray x = jniEnv->NewDoubleArray(n);
	if (x == NULL) return false;
	double *x_buf = jniEnv->GetDoubleArrayElements (x, &isCopy);
	for (i=0; i<n; i++)
	{	x_buf[i]=_x[i];
	}
	jniEnv->ReleaseDoubleArrayElements (x, x_buf, 0); // 0 = copy back
	jdoubleArray g = jniEnv->NewDoubleArray(m);
	if (g == NULL) return false;

	status = jniEnv->CallBooleanMethod(ipoptObj, MID_Eval_G, n, x, new_x, m, g);
	
	double*	g_buf = jniEnv->GetDoubleArrayElements (g, &isCopy);
	for (i=0; i<m; i++)
	{	_g[i]=g_buf[i];
	}

	jniEnv->ReleaseDoubleArrayElements (g, g_buf, JNI_ABORT);
	return status;
}
	
Bool eval_jac_g(Index _n, Number *_x, Bool _new_x,
                Index _m, Index _nele_jac,
                Index *_iRow, Index *_jCol, Number *_values,
                UserDataPtr user_data)
{
	jboolean isCopy;
	Bool status;
	int i;
//	printf("C callback: eval_jac_g\n");
	if (ipoptObj == NULL || jniEnv == NULL)
	{
		printf("null jni env or obj\n");
		return false;
	}
	jint n = _n;
	jboolean new_x = _new_x;
	jint m = _m;
	jint nele_jac = _nele_jac;
	jdoubleArray values = NULL;
	jdoubleArray x = NULL;
	jintArray iRow = NULL;
	jintArray jCol = NULL;
	
	if (_values == NULL) // only get structure (indices)
	{	
		if (_iRow == NULL || _jCol == NULL)
		{	printf("Error indices null when _values is null\n");
			return false;
		}
		iRow = jniEnv->NewIntArray(nele_jac);
		if (iRow == NULL) return false;
		jCol = jniEnv->NewIntArray(nele_jac);
		if (jCol == NULL) return false;
		
	}
	else
	{
		values = jniEnv->NewDoubleArray(nele_jac);
		if (values == NULL) return false;
		if (_x == NULL)
		{	printf("Error _x null when _values is not\n");
			return false;
		}
		x = jniEnv->NewDoubleArray(n);
		double *x_buf = jniEnv->GetDoubleArrayElements (x, &isCopy);
		for (i=0; i<n; i++)
		{	//printf("x[%d] = %e\n", i, _x[i]);
			x_buf[i]=_x[i];
		}
		jniEnv->ReleaseDoubleArrayElements (x, x_buf, 0); // 0 = copy back
	}

//	printf("ipoptObj = %x\n", *ipoptObj);
	status = jniEnv->CallBooleanMethod(ipoptObj, MID_Eval_Jac_G, 
		n, x, new_x, m, nele_jac, iRow, jCol, values);
//	printf("returned from Eval_Jac_G\n");

	if (_values == NULL) // copy back new indices
	{	
		int* iRow_buf = jniEnv->GetIntArrayElements (iRow, &isCopy);
		int* jCol_buf = jniEnv->GetIntArrayElements (jCol, &isCopy);
		for (i=0; i<nele_jac; i++)
		{	_iRow[i]=iRow_buf[i];
			_jCol[i]=jCol_buf[i];
		}
		jniEnv->ReleaseIntArrayElements (iRow, iRow_buf, JNI_ABORT);
		jniEnv->ReleaseIntArrayElements (jCol, jCol_buf, JNI_ABORT);
	}
	else // copy back new values
	{	
		double*	values_buf = jniEnv->GetDoubleArrayElements (values, &isCopy);
		for (i=0; i<nele_jac; i++)
		{	_values[i]=values_buf[i];
		}
		jniEnv->ReleaseDoubleArrayElements (values, values_buf, JNI_ABORT);
//		for (i=0; i<nele_jac; i++)
//			printf("val[%d]=%e\n", i, _values[i]);
	}

	return status;
}
	
Bool eval_h(Index _n, Number *_x, Bool _new_x, Number _obj_factor,
            Index _m, Number *_lambda, Bool _new_lambda,
            Index _nele_hess, Index *_iRow, Index *_jCol,
            Number *_values, UserDataPtr user_data)
{

	jboolean isCopy;
	Bool status;
	int i;
//	printf("C callback: eval_h\n");
	if (ipoptObj == NULL || jniEnv == NULL)
	{
		printf("null jni env or obj\n");
		return false;
	}
//	if (_x==NULL)
//	{
//		printf("null found\n");
//	}
	
	jint n = _n;
	jboolean new_x = _new_x;
	jdouble obj_factor = _obj_factor;
	jint m = _m;
	jint nele_hess = _nele_hess;
	jboolean new_lambda = _new_lambda;
	jdoubleArray values = NULL;
	jdoubleArray x = NULL;	

	if (_x != NULL)	{
	x = jniEnv->NewDoubleArray(n);
	double *x_buf = jniEnv->GetDoubleArrayElements (x, &isCopy);
		for (i=0; i<n; i++)
	{	
		x_buf[i]=_x[i];
	}
	jniEnv->ReleaseDoubleArrayElements (x, x_buf, 0); // 0 = copy back
	}
	
	jdoubleArray lambda = NULL;
	if (_lambda != NULL) {
	lambda = jniEnv->NewDoubleArray(n);
	double *lambda_buf = jniEnv->GetDoubleArrayElements (lambda, &isCopy);
	for (i=0; i<n; i++)
	{	lambda_buf[i]=_lambda[i];
	}
	jniEnv->ReleaseDoubleArrayElements (lambda, lambda_buf, 0); // 0 = copy back
	}

	if (_values != NULL ) 
	{	//printf("values null\n");
		values = jniEnv->NewDoubleArray(nele_hess);
	}
	jintArray iRow = NULL;
	jintArray jCol = NULL;
	if (_iRow != NULL && _jCol != NULL)
	{	
		iRow = jniEnv->NewIntArray(nele_hess);
		if (iRow == NULL) return false;
		jCol = jniEnv->NewIntArray(nele_hess);
		if (jCol == NULL) return false;
	}
	
//	printf("ipoptObj = %x\n", *ipoptObj);
	status = jniEnv->CallBooleanMethod(ipoptObj, MID_Eval_H, 
		n, x, new_x, obj_factor, m, lambda, new_lambda,
		nele_hess, iRow, jCol, values);
	
	if (_iRow != NULL && _jCol != NULL) // copy back new indices
	{	
		int* iRow_buf = jniEnv->GetIntArrayElements (iRow, &isCopy);
		int* jCol_buf = jniEnv->GetIntArrayElements (jCol, &isCopy);
		for (i=0; i<nele_hess; i++)
		{	_iRow[i]=iRow_buf[i];
			_jCol[i]=jCol_buf[i];
		}
		jniEnv->ReleaseIntArrayElements (iRow, iRow_buf, JNI_ABORT);
		jniEnv->ReleaseIntArrayElements (jCol, jCol_buf, JNI_ABORT);
	}
	
	if (_values != NULL) // copy back new values
	{	double*	values_buf = jniEnv->GetDoubleArrayElements (values, &isCopy);
		for (i=0; i<nele_hess; i++)
		{	_values[i]=values_buf[i];
		}
		jniEnv->ReleaseDoubleArrayElements (values, values_buf, JNI_ABORT);
	}
	return status;
}

/* Test Code */

JNIEXPORT void JNICALL
Java_maspack_ipopt_IpoptInterface_testCallbacks(JNIEnv *env, jobject obj)
{
//	ipoptObj = obj;
	printf("Testing callbacks from native code\n");
//	jdouble x = 51.0;
//	jint n = 10;
//	int size = 9;
//	jintArray iarr = env->NewIntArray(size);
//	jdoubleArray darr = env->NewDoubleArray(size);
//	jboolean b = true;
//	env->CallBooleanMethod(obj, MID_callbackTest, n);
//	env->CallBooleanMethod(obj, MID_Eval_F, n, darr, b, darr);
//	env->CallBooleanMethod(obj, MID_Eval_Grad_F, n, darr, b, darr);
//	env->CallBooleanMethod(obj, MID_Eval_G, n, darr, b, n, darr);
//	env->CallBooleanMethod(obj, MID_Eval_Jac_G, n, darr, b, n, n, iarr, iarr, darr);
//	env->CallBooleanMethod(obj, MID_Eval_H, n, darr, b, x, n, darr, b, n, iarr, iarr, darr);
	
	IpoptTest *tester = new IpoptTest();
    tester->test();
}

IpoptTest::IpoptTest()
{ 	printf("Starting IpoptTest...\n"); 
}

IpoptTest::~IpoptTest()
{}

void IpoptTest::test()
{
	printf("testing callbacks from C...\n");
	Index n = 4;
	Index m = 2;
	Bool new_x = true;
	Number* x = (Number*)malloc(sizeof(Number)*n);
	x[0] = 1.0;
	x[1] = 5.0;
	x[2] = 9.0;
	x[3] = 1.0;
	Number* f = (Number*)malloc(sizeof(Number)*1);
//	jniEnv->CallBooleanMethod(ipoptObj, MID_Eval_F, n, x, new_x, f);
	eval_f(n, x, new_x, f, NULL);
	printf("f(x) = %8.2f\n", f[0]);
	eval_g(n, x, new_x, m, f, NULL);
	printf("g(x) = %8.2f\n", f[0]);
	
	Number* gradF = (Number*)malloc(sizeof(Number)*n);
	eval_grad_f(n, x, new_x, gradF, NULL);
  	for (int i=0; i<n; i++) {
    	printf("gradF[%d] = %e\n", i, gradF[i]);
   	}	
   	
	Index nJ = 8;
	Index* row = (Index*)malloc(sizeof(Index)*nJ);
	Index* col = (Index*)malloc(sizeof(Index)*nJ);
	Number* val = (Number*)malloc(sizeof(Number)*nJ);
	
	printf("eval_jac_g with values\n");	
	eval_jac_g(n, x, new_x, m, nJ, row, col, val, NULL);
  	for (int i=0; i<nJ; i++) {
    	printf("val[%d] = %e\n", i, val[i]);
   	}
   	
	printf("eval_jac_g with NULL values\n");	
   	eval_jac_g(n, NULL, new_x, m, nJ, row, col, NULL, NULL);
  	for (int i=0; i<nJ; i++) {
    	printf("val[%d] = %e\n", i, val[i]);
   	}
	
}
