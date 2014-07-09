#include <jni.h>
#include "maspack_matlab_MatlabInterface.h"
#include <stdio.h>
#include <string.h>
#include "engine.h"

#define DEFAULT_BUFFERSIZE 65536

Engine* ep;

char outputBuffer[DEFAULT_BUFFERSIZE];

JNIEXPORT void JNICALL 
Java_maspack_matlab_MatlabInterface_open(JNIEnv *env, jobject obj, const jstring startcmd) {
  const char *c_string = env->GetStringUTFChars(startcmd, 0);
  if (!(ep = engOpen(c_string))) {
    jclass exception;
    env->ReleaseStringUTFChars(startcmd, c_string);
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Opening Matlab failed.");
    return;
  }
  env->ReleaseStringUTFChars(startcmd, c_string);
	/* indicate that output should not be discarded but stored in */
	/* outputBuffer */
  engOutputBuffer(ep, outputBuffer, DEFAULT_BUFFERSIZE);
}

JNIEXPORT void JNICALL 
Java_maspack_matlab_MatlabInterface_close(JNIEnv *env, jobject obj) {
	if (engClose(ep) == 1) {
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Closing Matlab failed.");
    return;
  }
}

JNIEXPORT void JNICALL
Java_maspack_matlab_MatlabInterface_evalString(JNIEnv *env, jobject obj, const jstring j_string) {
  const char *c_string;
	c_string = env->GetStringUTFChars(j_string, 0);
  if (engEvalString(ep, c_string) != 0) {
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Error while sending/receiving data.");
	}
  env->ReleaseStringUTFChars(j_string, c_string);
}

JNIEXPORT jstring JNICALL
Java_maspack_matlab_MatlabInterface_getOutputString(JNIEnv *env, jobject obj, jint numberOfChars) {
  char *c_string;
	jstring j_string;
	if (numberOfChars > DEFAULT_BUFFERSIZE) {
		numberOfChars = DEFAULT_BUFFERSIZE;
	}
  c_string = (char *) malloc ( sizeof(char)*(numberOfChars+1) );
	c_string[numberOfChars] = 0;
  strncpy(c_string, outputBuffer, numberOfChars);
	j_string = env->NewStringUTF(c_string);
	free(c_string);
  return j_string;
}

JNIEXPORT void JNICALL
Java_maspack_matlab_MatlabInterface_putArray(JNIEnv *env, jobject obj, 
		const jstring name, jdoubleArray array) {

	jboolean isCopy;
  	mxArray *mxarr = NULL;
  	const char *str = env->GetStringUTFChars(name, 0);
	if (str == NULL) {
		return; /* OutOfMemoryError already thrown */
	}
  	
  	int len = env->GetArrayLength(array);
  	double *carr = env->GetDoubleArrayElements(array, &isCopy);
  	if (carr == NULL) {
  		return;  /* OutOfMemoryError already thrown */
  	}
 	mxarr = mxCreateNumericMatrix(1, len, mxDOUBLE_CLASS, mxREAL);
	memcpy((void *)mxGetPr(mxarr), carr, len*sizeof(*carr));
	
    if (engPutVariable(ep, str, mxarr) != 0) {
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Error while putting variable");
	}
  	
  	// release mem
  	env->ReleaseDoubleArrayElements(array, carr, JNI_ABORT);
  	env->ReleaseStringUTFChars(name, str);
  	mxDestroyArray(mxarr);
  	
}


JNIEXPORT void JNICALL
Java_maspack_matlab_MatlabInterface_getArray(JNIEnv *env, jobject obj,
		jstring name, jdoubleArray result) {

  jboolean isCopy;			
  mxArray *mxarr = NULL;

  const char *str = env->GetStringUTFChars(name, 0);
//	printf("Retrieving Variable %s...\n", str);
  
   if ((mxarr = engGetVariable(ep, str)) == NULL) {
  	printf("engGetVariable failed for %s ...\n", str);
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Error while getting variable");
   }

	int len = mxGetNumberOfElements(mxarr);
//	printf("array length = %d\n", len);
	
	double carr[len];
    memcpy((void *)carr, (void *)mxGetPr(mxarr), sizeof(carr));
  	mxDestroyArray(mxarr);
    
    double *ar = env->GetDoubleArrayElements (result, &isCopy);
	for (int i=0; i<len; i++)
	 { ar[i] = carr[i];
	 }
	env->ReleaseDoubleArrayElements (result, ar, 0); // 0 = copyback
  	env->ReleaseStringUTFChars(name, str);
}

JNIEXPORT void JNICALL
Java_maspack_matlab_MatlabInterface_putIntArray(JNIEnv *env, jobject obj, 
		const jstring name, jintArray array) {

	jboolean isCopy;
  	mxArray *mxarr = NULL;
  	const char *str = env->GetStringUTFChars(name, 0);
	if (str == NULL) {
		return; /* OutOfMemoryError already thrown */
	}
  	
  	int len = env->GetArrayLength(array);
  	int *carr = env->GetIntArrayElements(array, &isCopy);
  	if (carr == NULL) {
  		return;  /* OutOfMemoryError already thrown */
  	}
  	
  	printf("size of int = %d\n", sizeof(*carr));
  	
 	mxarr = mxCreateNumericMatrix(1, len, mxINT32_CLASS, mxREAL);
	memcpy((void *)mxGetPr(mxarr), carr, len*sizeof(*carr));
	
    if (engPutVariable(ep, str, mxarr) != 0) {
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Error while putting variable");
	}
  	
  	// release mem
  	env->ReleaseIntArrayElements(array, carr, JNI_ABORT);
  	env->ReleaseStringUTFChars(name, str);
  	mxDestroyArray(mxarr);
  	
}


JNIEXPORT void JNICALL
Java_maspack_matlab_MatlabInterface_getIntArray(JNIEnv *env, jobject obj,
		jstring name, jintArray result) {

  jboolean isCopy;			
  mxArray *mxarr = NULL;

  const char *str = env->GetStringUTFChars(name, 0);
//	printf("Retrieving Variable %s...\n", str);
  
   if ((mxarr = engGetVariable(ep, str)) == NULL) {
  	printf("engGetVariable failed for %s ...\n", str);
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Error while getting variable");
   }

	int len = mxGetNumberOfElements(mxarr);
//	printf("array length = %d\n", len);
	
	int carr[len];
    memcpy((void *)carr, (void *)mxGetPr(mxarr), sizeof(carr));
  	mxDestroyArray(mxarr);
    
    int *ar = env->GetIntArrayElements (result, &isCopy);
	for (int i=0; i<len; i++)
	 { ar[i] = carr[i];
	 }
	env->ReleaseIntArrayElements (result, ar, 0); // 0 = copyback
  	env->ReleaseStringUTFChars(name, str);
}




JNIEXPORT void JNICALL
Java_maspack_matlab_MatlabInterface_putMatrix(JNIEnv *env, jobject obj, 
		const jstring name, jobjectArray mat) {

	int i, j;
	jboolean isCopy;
	jdoubleArray jar;
  	mxArray *mxarr = NULL;
  	double* ar;
  	const char *str = env->GetStringUTFChars(name, 0);
	if (str == NULL) {
		return; /* OutOfMemoryError already thrown */
	}
	
	jint rows = env->GetArrayLength(mat);
	jar = (jdoubleArray)env->GetObjectArrayElement(mat, 0);
	jint cols = env->GetArrayLength(jar);
  	
	double carr[rows*cols];
  	if (carr == NULL) {
  		return;  /* OutOfMemoryError already thrown */
  	}
  	for (i = 0; i < rows; i++)
    {
	    jar = (jdoubleArray)env->GetObjectArrayElement(mat, i);
	    ar = env->GetDoubleArrayElements(jar, &isCopy);
		for (j = 0; j < cols; j++) {
			carr[i+(j*rows)] = ar[j];
		}
		env->ReleaseDoubleArrayElements (jar, ar, JNI_ABORT);
	}   	
  	
 	mxarr = mxCreateNumericMatrix(rows, cols, mxDOUBLE_CLASS, mxREAL);
	memcpy((void *)mxGetPr(mxarr), carr, sizeof(carr));
	
    if (engPutVariable(ep, str, mxarr) != 0) {
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Error while putting variable");
	}
  	
  	// release mem
  	env->ReleaseStringUTFChars(name, str);
  	mxDestroyArray(mxarr);
  	
}




JNIEXPORT void JNICALL
Java_maspack_matlab_MatlabInterface_getMatrix(JNIEnv *env, jobject obj,
		jstring name, jobjectArray result) {

  int i, j;

  jboolean isCopy;			
  mxArray *mxarr = NULL;
  jdoubleArray jar;
  double* ar;
  
  jclass doubleArrCls = env->FindClass("[D");
  if (doubleArrCls == NULL) {
  	printf("can't find class [D\n");
	return; /* exception thrown */
  }

  const char *str = env->GetStringUTFChars(name, 0);
//	printf("Retrieving Variable %s...\n", str);
  
   if ((mxarr = engGetVariable(ep, str)) == NULL) {
  	printf("engGetVariable failed for %s ...\n", str);
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Error while getting variable");
   }

	// check size matches result matrix
	int rows = mxGetM(mxarr);
	int cols = mxGetN(mxarr);
//	printf("matrix size = %d x %d\n", rows, cols);
	jint jrows = env->GetArrayLength(result);
	jar = (jdoubleArray)env->GetObjectArrayElement(result, 0);
	jint jcols = env->GetArrayLength(jar);
//	printf("result matrix size = %d x %d\n", jrows, jcols);
   if (rows!=jrows || cols!=jcols) {
  	printf("getMatrix(): result matrix wrong size %d x %d, expecting %d x %d ",
  			 jrows, jcols, rows, cols);
	  jclass exception;
    exception = env->FindClass("java/io/IOException");
    if (exception == 0) return;
    env->ThrowNew(exception, "Bad result matrix size");
   }

	int len = mxGetNumberOfElements(mxarr);
//	printf("mxarr len = %d\n", len);

	double carr[len];
    memcpy((void *)carr, (void *)mxGetPr(mxarr), sizeof(carr));
  	mxDestroyArray(mxarr);
	
    for (i = 0; i < rows; i++)
    {
	    jar = (jdoubleArray)env->GetObjectArrayElement(result, i);
	    ar = env->GetDoubleArrayElements(jar, &isCopy);
		for (j = 0; j < cols; j++) {
			ar[j] = carr[i+(j*rows)];
		}
		env->ReleaseDoubleArrayElements (jar, ar, 0); // 0 = copyback
	}   	
  	env->ReleaseStringUTFChars(name, str);
}
