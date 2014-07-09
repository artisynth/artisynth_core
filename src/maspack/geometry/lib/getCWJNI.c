#include <jni.h>
#include <fpu_control.h>
#include "maspack_geometry_RobustPreds.h"


JNIEXPORT jint JNICALL Java_maspack_geometry_RobustPreds_jniGetCW(
	JNIEnv *env, jclass jcls 
) {
   short ret;
   _FPU_GETCW(ret);
   if( ret != 895 && ret != 4722 && ret != 4735) {
      printf("\n Warning! The FPU CONtrol word is weird: %d ", ret);
      fflush(stdout);
   }
   return ret;
}
