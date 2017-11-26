#include <jni.h>
#include "geoPred.h"
//#include "robustpreds.h"
#include "maspack_geometry_RobustPreds.h"

/////////////////////////////////////////
// OS-specific fpu access
/////////////////////////////////////////
#ifdef CPU86 // windows
#include <float.h>
//unsigned int _control87( unsigned int new, unsigned int mask );
#pragma fenv_access (on)
// For -mno-cygwin
#ifndef _MCW_PC // precision control
#define _MCW_PC 0x00030000
#endif /* _MCW_PC */
#ifndef _PC_53
#define _PC_53 0X00010000
#endif /* _PC_53 */
#endif /* CPU86 */

#ifdef LINUX
#include <fpu_control.h>
#endif /* LINUX */

#ifdef DARWIN
#ifndef _FPU_GETCW
#define _FPU_GETCW(cw) __asm__ __volatile__ ("fnstcw %0" : "=m" (*&cw))
#endif
#ifndef _FPU_SETCW
#define _FPU_SETCW(cw) __asm__ __volatile__ ("fldcw %0" : : "m" (*&cw))
#endif /* _FPU_SETCW */
#endif /* DARWIN */

#if defined (LINUX) || defined (DARWIN)
#include <fenv.h>
static long cwordOld;
static short cword;
#ifndef FPU_PRECISION_CONTROL
#define FPU_PRECISION_CONTROL (3<<8)
#endif
#ifndef FPU_PRECISION_DOUBLE
#define FPU_PRECISION_DOUBLE  (2<<8)
#endif
#ifndef FPU_PRECISION_SINGLE
#define FPU_PRECISION_SINGLE  (0<<8)
#endif
#endif
#ifdef CPU86
static unsigned int cwordOld;
#endif

static jfieldID fieldIDPoint3dx, fieldIDPoint3dy, fieldIDPoint3dz;

void saveState() {
#ifdef CPU86
   // Save old state
   cwordOld = _controlfp(0,0);
#endif

#if defined (LINUX) || defined (DARWIN)
   short getCW=0;
   _FPU_GETCW(getCW);
   cwordOld = getCW;
#endif
}

void setPrecision() {
#ifdef CPU86
#ifdef SINGLE
  _controlfp(_PC_24, _MCW_PC); /* Set FPU control word for single precision. */
#else /* not SINGLE */
  _controlfp(_PC_53, _MCW_PC); /* Set FPU control word for double precision. */
#endif /* not SINGLE */
#endif /* CPU86 */

#if defined (LINUX) || defined (DARWIN)
   cword = cwordOld & ~FPU_PRECISION_CONTROL;
#ifdef SINGLE
   cword = cword | FPU_PRECISION_SINGLE;
#else
   cword = cword | FPU_PRECISION_DOUBLE;
#endif
	//__asm volatile ("fclex " :: ); 
   feclearexcept(FE_ALL_EXCEPT); // there is hell to pay if we don't clear flags before unmasking
   _FPU_SETCW(cword);
#endif // LINUX || DARWIN
}

void restorePrecision() {
#ifdef CPU86
   _controlfp(cwordOld, _MCW_PC);
#endif

#if defined (LINUX) || defined (DARWIN)
   feclearexcept(FE_ALL_EXCEPT); // there is hell to pay if we don't clear flags before unmasking
   _FPU_SETCW(cwordOld);
#endif
}

JNIEXPORT jint JNICALL Java_maspack_geometry_RobustPreds_jniInit(
	JNIEnv *env, jclass aClass, 
	jobject jPoint3d
) {
   saveState();

	jclass jPoint3dCls = (*env)->GetObjectClass(env, jPoint3d);
	fieldIDPoint3dx = (*env)->GetFieldID(env, jPoint3dCls, "x", "D");
	fieldIDPoint3dy = (*env)->GetFieldID(env, jPoint3dCls, "y", "D");
	fieldIDPoint3dz = (*env)->GetFieldID(env, jPoint3dCls, "z", "D");
   exactinit(); // initialize Shewchuck's predicates

   restorePrecision();
   return 0; // return value not used
}

/**
 * Just like, jniOrient3d, except that it also returns the volume. Not
 * currently used; included for possible use later.
 */
JNIEXPORT jint JNICALL Java_maspack_geometry_RobustPreds_jniOrient3dv(
   JNIEnv *env, jclass jcls,
   jint i0, jdouble p0x, jdouble p0y, jdouble p0z,
   jint i1, jdouble p1x, jdouble p1y, jdouble p1z,
   jint i2, jdouble p2x, jdouble p2y, jdouble p2z,
   jint i3, jdouble p3x, jdouble p3y, jdouble p3z,
   jdoubleArray volume
) {
   setPrecision();
   double p0[3];
   double p1[3];
   double p2[3];
   double p3[3];

   p0[0] = p0x;
   p0[1] = p0y;
   p0[2] = p0z;

   p1[0] = p1x;
   p1[1] = p1y;
   p1[2] = p1z;

   p2[0] = p2x;
   p2[1] = p2y;
   p2[2] = p2z;

   p3[0] = p3x;
   p3[1] = p3y;
   p3[2] = p3z;

   double vol;
   int result = nasaOrient3d (i0, p0, i1, p1, i2, p2, i3, p3, &vol);

   // populate volume
   jboolean copy;
   jdouble* velems = (*env)->GetDoubleArrayElements(env,volume, &copy);
   jint n = (*env)->GetArrayLength(env,volume);
   if (n > 0) {
      velems[0] = vol;
   }
   if (copy == JNI_TRUE) {
      (*env)->ReleaseDoubleArrayElements(env,volume, velems, 0); // copy elements
   }

   restorePrecision();
   return result;
}

JNIEXPORT jint JNICALL Java_maspack_geometry_RobustPreds_jniOrient3d(
	JNIEnv *env, jclass jcls,
	jint i0, jdouble p0x, jdouble p0y, jdouble p0z,
	jint i1, jdouble p1x, jdouble p1y, jdouble p1z,
	jint i2, jdouble p2x, jdouble p2y, jdouble p2z,
	jint i3, jdouble p3x, jdouble p3y, jdouble p3z
) {
   setPrecision();
   double p0[3];
   double p1[3];
   double p2[3];
   double p3[3];

   p0[0] = p0x;
   p0[1] = p0y;
   p0[2] = p0z;

   p1[0] = p1x;
   p1[1] = p1y;
   p1[2] = p1z;

   p2[0] = p2x;
   p2[1] = p2y;
   p2[2] = p2z;

   p3[0] = p3x;
   p3[1] = p3y;
   p3[2] = p3z;

   double volume;
   //return nasaOrient3d (i0, p0, i1, p1, i2, p2, i3, p3, &volume);
   int result = nasaOrient3d (i0, p0, i1, p1, i2, p2, i3, p3, &volume);
   restorePrecision();
   return result;
}

JNIEXPORT jint JNICALL Java_maspack_geometry_RobustPreds_jniIntersectSegmentTriangle(
	JNIEnv *env, jclass aClass, 
	jint is0, jdouble s0x, jdouble s0y, jdouble s0z,
	jint is1, jdouble s1x, jdouble s1y, jdouble s1z,
	jint it0, jdouble t0x, jdouble t0y, jdouble t0z,
	jint it1, jdouble t1x, jdouble t1y, jdouble t1z,
	jint it2, jdouble t2x, jdouble t2y, jdouble t2z,
	jobject jPoint3d
) {
   setPrecision();
   double s0[3], s1[3], t0[3], t1[3], t2[3], cIntPoint[3];
   s0[0] = s0x;
   s0[1] = s0y;
   s0[2] = s0z;
   s1[0] = s1x;
   s1[1] = s1y;
   s1[2] = s1z;
   t0[0] = t0x;
   t0[1] = t0y;
   t0[2] = t0z;
   t1[0] = t1x;
   t1[1] = t1y;
   t1[2] = t1z;
   t2[0] = t2x;
   t2[1] = t2y;
   t2[2] = t2z;

   int result = intersectSegmentTriangle(is0, s0, is1, s1, it0, t0, it1, t1, it2, t2, cIntPoint);
   if (result != 0) {
	   (*env)->SetDoubleField(env, jPoint3d, fieldIDPoint3dx, cIntPoint[0]);
	   (*env)->SetDoubleField(env, jPoint3d, fieldIDPoint3dy, cIntPoint[1]);
	   (*env)->SetDoubleField(env, jPoint3d, fieldIDPoint3dz, cIntPoint[2]);
   }
   restorePrecision();
   return result;
}

JNIEXPORT jint JNICALL Java_maspack_geometry_RobustPreds_jniClosestIntersection(
 JNIEnv * env, jclass cls,
 jdouble ax, jdouble ay, jdouble az,
 jdouble bx, jdouble by, jdouble bz,
 jdouble c0x, jdouble c0y, jdouble c0z,
 jdouble c1x, jdouble c1y, jdouble c1z,
 jdouble c2x, jdouble c2y, jdouble c2z,
 jdouble d0x, jdouble d0y, jdouble d0z,
 jdouble d1x, jdouble d1y, jdouble d1z,
 jdouble d2x, jdouble d2y, jdouble d2z
) {
   setPrecision();
   double a[3], b[3], c0[3], c1[3], c2[3], d0[3], d1[3], d2[3];
   a[0] = ax;
   a[1] = ay;
   a[2] = az;
   b[0] = bx;
   b[1] = by;
   b[2] = bz;
   c0[0] = c0x;
   c0[1] = c0y;
   c0[2] = c0z;
   c1[0] = c1x;
   c1[1] = c1y;
   c1[2] = c1z;
   c2[0] = c2x;
   c2[1] = c2y;
   c2[2] = c2z;
   d0[0] = d0x;
   d0[1] = d0y;
   d0[2] = d0z;
   d1[0] = d1x;
   d1[1] = d1y;
   d1[2] = d1z;
   d2[0] = d2x;
   d2[1] = d2y;
   d2[2] = d2z;
   int q = closestIntersection(a, b, c0, c1, c2, d0, d1, d2);
   restorePrecision();
/*
FILE *fout;
fout = fopen("geoPred.txt", "a+");
fprintf(fout, "\n");
fprintf(fout, "a := Point3fd x: %.18e y: %.18e z: %.18e.\n", ax, ay, az);
fprintf(fout, "b := Point3fd x: %.18e y: %.18e z: %.18e.\n", bx, by, bz);
fprintf(fout, "c0 := Point3fd x: %.18e y: %.18e z: %.18e.\n", c0x, c0y, c0z);
fprintf(fout, "c1 := Point3fd x: %.18e y: %.18e z: %.18e.\n", c1x, c1y, c1z);
fprintf(fout, "c2 := Point3fd x: %.18e y: %.18e z: %.18e.\n", c2x, c2y, c2z);
fprintf(fout, "d0 := Point3fd x: %.18e y: %.18e z: %.18e.\n", d0x, d0y, d0z);
fprintf(fout, "d1 := Point3fd x: %.18e y: %.18e z: %.18e.\n", d1x, d1y, d1z);
fprintf(fout, "d2 := Point3fd x: %.18e y: %.18e z: %.18e.\n", d2x, d2y, d2z);
fprintf(fout, "q := %d\n", q);
fclose(fout);
*/
	return q;
}
