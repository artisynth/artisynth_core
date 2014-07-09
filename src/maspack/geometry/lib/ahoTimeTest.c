#include <stdio.h>
#include <string.h>
//#include <signal.h>
//#include <time.h>
#include <stdlib.h>
#include <sys/time.h>
#include "geoPred.h"
//#include "robustpreds.h"

#define PI 3.14159

void *malloc(size_t);
void srand(unsigned int);
long int random();
double cos(double);
double sin(double);
unsigned int time(void *);

void rotatepoint(double *r[3], double p[3]) {
   double res[3];
   memset(res, 0, 3*sizeof(double));

   for(int i=0; i<3; i++) {
      for(int j=0; j<3; j++) {
         res[i] += r[i][j]*p[i];
      }
   }

   for(int i=0; i<3; i++) {
      p[i] = res[i];
   }

}

double **makeRotationMatrix(double xang, double yang, double zang) {
   double **rot;
   // allocate some memory
   rot = (double**) malloc(3*sizeof(double*));
   for( int i=0; i<3; i++ ) {
      rot[i] = (double*) malloc(3*sizeof(double));
   }

   rot[0][0] = cos(yang)*cos(zang);
   rot[1][0] = cos(yang)*sin(zang);
   rot[2][0] = -sin(yang);

   rot[0][1] = -cos(xang)*sin(zang)+sin(xang)*sin(yang)*cos(zang);
   rot[1][1] = cos(xang)*sin(zang) +sin(xang)*sin(yang)*sin(zang);
   rot[2][1] = sin(xang)*cos(yang);

   rot[0][2] = sin(xang)*sin(zang) +cos(xang)*sin(yang)*cos(zang);
   rot[1][2] = -sin(xang)*cos(zang)+cos(xang)*sin(yang)*cos(zang);
   rot[2][2] = cos(xang)*cos(yang);

   return rot;
}
// cleanup memory
void clearRotationMatrix(double **r) {
   for(int i=0; i<3; i++) {
      free((void*)r[i]);
   }

   free((void*)r);
}

void doubleprint(number)
double number;
{
  unsigned long long no;
  unsigned long long sign, expo;
  int exponent;
  int i, bottomi;

  no = *(unsigned long long *) &number;
  sign = no & 0x8000000000000000ll;
  expo = (no >> 52) & 0x7ffll;
  exponent = (int) expo;
  exponent = exponent - 1023;
  if (sign) {
    printf("-");
  } else {
    printf(" ");
  }
  if (exponent == -1023) {
    printf(
      "0.0000000000000000000000000000000000000000000000000000_     (   )");
  } else {
    printf("1.");
    bottomi = -1;
    for (i = 0; i < 52; i++) {
      if (no & 0x0008000000000000ll) {
        printf("1");
        bottomi = i;
      } else {
        printf("0");
      }
      no <<= 1;
    }
    printf("_%d  (%d)", exponent, exponent - 1 - bottomi);
  }
}
// This came from predicatesC.c
double doublerand()
{
  double result;
  double expo;
  long a, b, c;
  long i;

  a = random();// doubleprint(a); printf("\n");
  b = random();// doubleprint(b); printf("\n");
  c = random();// doubleprint(c); printf("\n");
  result = (double) (a - 1073741824) * 8388608.0 + (double) (b >> 8);
  for (i = 512, expo = 2; i <= 131072; i *= 2, expo = expo * expo) {
    if (c & i) {
      result *= expo;
    }
  }
  return result;
}
//struct timeval {
//   time_t tv_sec;
//   suseconds_t tv_usec;
//} tStart, tEnd, tDiff;
struct timeval tStart, tEnd, tDiff;
   
// result = end-start;
void time_diff(struct timeval *start, struct timeval *end, struct timeval *result) {
   (*result).tv_sec = (*end).tv_sec - (*start).tv_sec;
   (*result).tv_usec = (*end).tv_usec - (*start).tv_usec;
}

int in_us(struct timeval *result) {
   return (*result).tv_sec*1000000 + (*result).tv_usec;
}

//static void myHandler(int signum,siginfo_t * info,void * v) {
//   printf("Caught signal %d\n", signum);
//   printf("siginfo.code: %d\n", (*info).si_code);
//   printf("siginfo.si_status: %d\n", (*info).si_status);
//   printf("siginfo.si_errno: %d\n", (*info).si_errno);
//
//}

int main(int argc, char** argv) {

   double s0[3], s1[3], t0[3], t1[3], t2[3], cIntPoint[3];
	int is0,is1,it0,it1,it2;
   int result;

   // vertex indices
	is0=0; is1=1; // segment
	it0=2; it1=3; it2=4; // triangle

   /*
    * Initialize Shewchuck's predicates
    */
   exactinit(); 
   int nTestCounter = 0;
   const int lenmax = 1023;
   char szTestName[lenmax+1]; // +1 for null
   int numIters = 1000;
   //int numIters = 1;

   // seed the rand generator
   gettimeofday(&tStart, NULL);
   //unsigned int iseed = in_us(&tStart);
   unsigned int iseed = 1978651987351;
   srand(iseed);

   /*
    * test 3
    * sanity check
    */
   strncpy( szTestName,
      "sanity check",
      lenmax);
   nTestCounter++;
   // segment coords
   s0[0] =  0.5; s0[1] = 0.5;  s0[2] = -1;
   s1[0] =  0.5; s1[1] = 0.5;  s1[2] = 1;

   // triangle coords
   t0[0] = -1; t0[1] =  0;  t0[2] = 0;
   t1[0] =  1; t1[1] =  1;  t1[2] = 0;
   t2[0] =  1; t2[1] = -1;  t2[2] = 0;

   // result
   cIntPoint[0] = -100;
   cIntPoint[1] = -100;
   cIntPoint[2] = -100;

   gettimeofday(&tStart, NULL);
   for( int i = 0; i < numIters; i++ ) {
      result = intersectSegmentTriangle(is0, s0, is1, s1, it0, t0, it1, t1, it2, t2, cIntPoint);
   }
   gettimeofday(&tEnd, NULL);
   time_diff(&tStart, &tEnd, &tDiff);
   printf("Test %i: %s\n", nTestCounter, szTestName);
   printf("  %i us, %i iterations\n", in_us(&tDiff), numIters);
   {
      printf("  result: %i cIntPoint: ( %f, %f, %f ) \n", result, cIntPoint[0],cIntPoint[1],cIntPoint[2]  );
   }

   /*
    * test 1
    * one coincident vertex
    */
   strncpy( szTestName,
      "One Coincident Vertex",
      lenmax);
   nTestCounter++;
   // segment coords
   s0[0] = -1; s0[1] = 0;  s0[2] = 0;
   s1[0] =  0; s1[1] = 0;  s1[2] = 1;

   // triangle coords
   t0[0] = -1; t0[1] =  0;  t0[2] = 0;
   t1[0] =  1; t1[1] =  1;  t1[2] = 0;
   t2[0] =  1; t2[1] = -1;  t2[2] = 0;

   // result
   cIntPoint[0] = -100;
   cIntPoint[1] = -100;
   cIntPoint[2] = -100;

   gettimeofday(&tStart, NULL);
   for( int i = 0; i < numIters; i++ ) {
      result = intersectSegmentTriangle(is0, s0, is1, s1, it0, t0, it1, t1, it2, t2, cIntPoint);
   }
   gettimeofday(&tEnd, NULL);
   time_diff(&tStart, &tEnd, &tDiff);
   printf("Test %i: %s\n", nTestCounter, szTestName);
   printf("  %i us, %i iterations\n", in_us(&tDiff), numIters);
   {
      printf("  result: %i cIntPoint: ( %f, %f, %f ) \n", result, cIntPoint[0],cIntPoint[1],cIntPoint[2]  );
   }

   /*
    * test 2
    * one vertex in plane
    */
   strncpy( szTestName,
      "One Vertex in plane",
      lenmax);
   nTestCounter++;
   // segment coords
   s0[0] =  0; s0[1] = 0;  s0[2] = 0;
   s1[0] =  0; s1[1] = 0;  s1[2] = 1;

   // triangle coords
   t0[0] = -1; t0[1] =  0;  t0[2] = 0;
   t1[0] =  1; t1[1] =  1;  t1[2] = 0;
   t2[0] =  1; t2[1] = -1;  t2[2] = 0;

   // result
   cIntPoint[0] = -100;
   cIntPoint[1] = -100;
   cIntPoint[2] = -100;

   gettimeofday(&tStart, NULL);
   for( int i = 0; i < numIters; i++ ) {
      result = intersectSegmentTriangle(is0, s0, is1, s1, it0, t0, it1, t1, it2, t2, cIntPoint);
   }
   gettimeofday(&tEnd, NULL);
   time_diff(&tStart, &tEnd, &tDiff);
   printf("Test %i: %s\n", nTestCounter, szTestName);
   printf("  %i us, %i iterations\n", in_us(&tDiff), numIters);
   {
      printf("  result: %i cIntPoint: ( %f, %f, %f ) \n", result, cIntPoint[0],cIntPoint[1],cIntPoint[2]  );
   }

   ///*
   // * test 2
   // * slight perturbation of one vertex in plane
   // */
   //strncpy( szTestName,
   //   "Perturbed - One Vertex in plane",
   //   lenmax);
   //nTestCounter++;
   //// segment coords
   //s0[0] =  0; s0[1] = 0;  s0[2] = 0;
   //s1[0] =  0; s1[1] = 0;  s1[2] = 1;

   //// triangle coords
   //t0[0] = -1; t0[1] =  0;  t0[2] = 0;
   //t1[0] =  1; t1[1] =  1;  t1[2] = 0;
   //t2[0] =  1; t2[1] = -1;  t2[2] = 0;

   //for(int i = 0; i<3; i++) {
   //  s0[i] += doublerand();
   //  s1[i] += doublerand();
   //  t0[i] += doublerand();
   //  t1[i] += doublerand();
   //  t2[i] += doublerand();
   //}

   //// result
   //cIntPoint[0] = -100;
   //cIntPoint[1] = -100;
   //cIntPoint[2] = -100;

   //gettimeofday(&tStart, NULL);
   //for( int i = 0; i < numIters; i++ ) {
   //   result = intersectSegmentTriangle(is0, s0, is1, s1, it0, t0, it1, t1, it2, t2, cIntPoint);
   //}
   //gettimeofday(&tEnd, NULL);
   //time_diff(&tStart, &tEnd, &tDiff);
   //printf("Test %i: %s\n", nTestCounter, szTestName);
   //printf("  %i us, %i iterations\n", in_us(&tDiff), numIters);
   //{
   //   printf("  result: %i cIntPoint: ( %f, %f, %f ) \n", result, cIntPoint[0],cIntPoint[1],cIntPoint[2]  );
   //}

   ///*
   // * test 2
   // * slight perturbation of one vertex in plane
   // */
   //strncpy( szTestName,
   //   "Perturbed - One Vertex in plane",
   //   lenmax);
   //nTestCounter++;
   //// segment coords
   //s0[0] =  0; s0[1] = 0;  s0[2] = 0;
   //s1[0] =  0; s1[1] = 0;  s1[2] = 1;

   //// triangle coords
   //t0[0] = -1; t0[1] =  0;  t0[2] = 0;
   //t1[0] =  1; t1[1] =  1;  t1[2] = 0;
   //t2[0] =  1; t2[1] = -1;  t2[2] = 0;

   //for(int i = 0; i<3; i++) {
   //  s0[i] += doublerand();
   //  s1[i] += doublerand();
   //  t0[i] += doublerand();
   //  t1[i] += doublerand();
   //  t2[i] += doublerand();
   //}

   //// result
   //cIntPoint[0] = -100;
   //cIntPoint[1] = -100;
   //cIntPoint[2] = -100;

   //gettimeofday(&tStart, NULL);
   //for( int i = 0; i < numIters; i++ ) {
   //   result = intersectSegmentTriangle(is0, s0, is1, s1, it0, t0, it1, t1, it2, t2, cIntPoint);
   //}
   //gettimeofday(&tEnd, NULL);
   //time_diff(&tStart, &tEnd, &tDiff);
   //printf("Test %i: %s\n", nTestCounter, szTestName);
   //printf("  %i us, %i iterations\n", in_us(&tDiff), numIters);
   //{
   //   printf("  result: %i cIntPoint: ( %f, %f, %f ) \n", result, cIntPoint[0],cIntPoint[1],cIntPoint[2]  );
   //}

   /*
    * test 2
    * Random rotation
    */
   strncpy( szTestName,
      "Random rotation",
      lenmax);
   nTestCounter++;
   // segment coords
   s0[0] =  0; s0[1] = 0;  s0[2] = 0;
   s1[0] =  0; s1[1] = 0;  s1[2] = 1;

   // triangle coords
   t0[0] = -1; t0[1] =  0;  t0[2] = 0;
   t1[0] =  1; t1[1] =  1;  t1[2] = 0;
   t2[0] =  1; t2[1] = -1;  t2[2] = 0;

   double r0 = (double)random();
   double r1 = (double)random();
   double r2 = (double)random();
   r0 /= RAND_MAX; r1 /= RAND_MAX; r2 /= RAND_MAX;
   r0 *= 2*PI; r1 *= 2*PI; r2 *= 2*PI;

   // Rotate the problem
   double **rot = makeRotationMatrix(r0,r1,r2);
   for(int i=0;i<3;i++) {
      s0[i] +=3; s1[i] +=3;
      t0[i] +=3; t1[i] +=3; t2[i] +=3;
   }
   rotatepoint(rot,s0);
   rotatepoint(rot,s1);
   rotatepoint(rot,t0);
   rotatepoint(rot,t1);
   rotatepoint(rot,t2);
   clearRotationMatrix(rot); // cleanup memory
   for(int i=0;i<3;i++) {
      s0[i] -=3; s1[i] -=3;
      t0[i] -=3; t1[i] -=3; t1[i] -=3;
   }

   // result
   cIntPoint[0] = -100;
   cIntPoint[1] = -100;
   cIntPoint[2] = -100;

   gettimeofday(&tStart, NULL);
   for( int i = 0; i < numIters; i++ ) {
      result = intersectSegmentTriangle(is0, s0, is1, s1, it0, t0, it1, t1, it2, t2, cIntPoint);
   }
   gettimeofday(&tEnd, NULL);
   time_diff(&tStart, &tEnd, &tDiff);
   printf("Test %i: %s\n", nTestCounter, szTestName);
   printf("  r0=%f, r1=%f, r2=%f\n", r0, r1, r2);
   printf("  %i us, %i iterations\n", in_us(&tDiff), numIters);
   printf("  result: %i\n", result );
   printf("         s0: ( %f, %f, %f ) \n", s0[0], s0[1], s0[2]);
   printf("  cIntPoint: ( %f, %f, %f ) \n", cIntPoint[0],cIntPoint[1],cIntPoint[2]  );
   return result;

}
