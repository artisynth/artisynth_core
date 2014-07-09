#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <math.h>

#include "geoPred.h"

#include "vecmath/vector.h"
#include "vecmath/matrix.h"


#define PI 3.141592653589793

#define MAX_BUFFER 1023 

double epsilon;

FILE *fp_out; // Record log info here

unsigned int nRandomTrials=10000;

typedef struct {
   vector3 s0;
   vector3 s1;
   vector3 s2;

   vector3 t0;
   vector3 t1;
   vector3 t2;

   int is0;
   int is1;
   int is2;

   int it0;
   int it1;
   int it2;

   vector3 intersectPt;

   int result0;
   int result1;

   char szName[ MAX_BUFFER+1 ];
} segTriCase;

typedef struct {
   vector3 s0;
   vector3 s1;

   vector3 t0;
   vector3 t1;
   vector3 t2;

   vector3 u0;
   vector3 u1;
   vector3 u2;

   int result0;
   int result1;

   char szName[ MAX_BUFFER+1 ];
} closestInterCase;

/*
 * ------------------------------------------------------------------
 * Test cases defined below
 * ------------------------------------------------------------------
 */

segTriCase segTriCases[] = {
   { 
      {  0.0,  0.0, -0.5 }, // s0
      {  0.0,  0.0,  0.5 }, // s1
      {  0.0,  0.0,  1.0 }, // s2

      { -1.0,  0.0,  0.0 }, // t0
      {  1.0,  1.0,  0.0 }, // t1
      {  1.0, -1.0,  0.0 }, // t2

      0, 1, 2, 3, 4, 5, // Indices

      { -100, -100, -100 },  // result stored here

      0,0, // result0,1
      "Segment-Tri - sanity check" // Test name
   },
   { 
      {  0.0,  0.0, -0.5 }, // s0
      {  0.0,  0.2,  0.0 }, // s1
      {  0.0,  0.0,  1.0 }, // s2

      { -1.0,  0.0,  0.0 }, // t0
      {  1.0,  1.0,  0.0 }, // t1
      {  1.0, -1.0,  0.0 }, // t2

      0, 1, 3, 2, 4, 5, // Indices

      { -100, -100, -100 },  // result stored here

      0,0, // result0,1
      "Segment-Tri - one pt in plane" // Test name
   },
   { 
      //{  1.0,  1.0,  0.0 }, // s0
      //{  1.0, -1.0,  0.0 }, // s1
      //{  0.0,  0.0,  1.0 }, // s2

      //{ -1.0,  0.0,  0.0 }, // t0
      //{  1.0,  1.0,  0.0 }, // t1
      //{  1.0, -1.0,  0.0 }, // t2

      //1, 5, 3, 2, 4, 0, // Indices
      { 5.743528e-2, 1.858655e-2, 9.985632e-2 },
      { 5.759240e-2, 2.127672e-2, 9.810183e-2 },
      { 0, 0, 0 },

      { 5.635739e-2, 2.089498e-2, 9.773670e-2 },
      { 5.743528e-2, 1.858655e-2, 9.985632e-2 },
      { 5.759240e-2, 2.127672e-2, 9.810183e-2 },
      
      418899, 418889, 0, 416975, 417235, 416995,

      { -100, -100, -100 },  // result stored here

      0,0, // result0,1
      "Segment-Tri - co-planar edge, points outside tri" // Test name
   },
   { 
      {  0.0,  0.0, -0.5 }, // s0
      {  1.0,  0.2,  0.0 }, // s1
      {  0.0,  0.0,  1.0 }, // s2

      { -1.0,  0.0,  0.0 }, // t0
      {  1.0,  1.0,  0.0 }, // t1
      {  1.0, -1.0,  0.0 }, // t2

      0, 1, 2, 3, 4, 5, // Indices

      { -100, -100, -100 },  // result stored here

      0,0, // result0,1
      "Segment-Tri - One point on edge A" // Test name
   },
   { 
      {  0.0,  0.0, -0.5 }, // s0
      {  1.0,  0.2,  0.0 }, // s1
      {  0.0,  0.0,  1.0 }, // s2

      {  2.0,  0.0,  0.2 }, // t0
      {  1.0, -1.0,  0.0 }, // t1
      {  1.0,  1.0,  0.0 }, // t2

      0, 1, 2, 6, 5, 4, // Indices

      { -100, -100, -100 },  // result stored here

      0,0, // result0,1
      "Segment-Tri - One point on edge B" // Test name
   },
   { 
      {  0.0,  0.0, -0.5 }, // s0
      { -1.0,  0.0,  0.0 }, // s1
      {  0.0,  0.0,  1.0 }, // s2

      { -1.0,  0.0,  0.0 }, // t0
      {  1.0,  1.0,  0.0 }, // t1
      {  1.0, -1.0,  0.0 }, // t2

      0, 1, 2, 3, 4, 5, // Indices

      { -100, -100, -100 },  // result stored here

      0,0, // result0,1
      "Segment-Tri - One point on point A" // Test name
   },
   { 
      {  0.0,  0.0, -0.5 }, // s0
      { -1.0,  0.0,  0.0 }, // s1
      {  0.0,  0.0,  1.0 }, // s2

      { -1.0,  0.0,  0.0 }, // t0
      {  1.0, -1.0,  0.0 }, // t2
      { -2.0,  0.0,  0.0 }, // t1

      0, 1, 2, 3, 5, 6, // Indices

      { -100, -100, -100 },  // result stored here

      0,0, // result0,1
      "Segment-Tri - One point on point B" // Test name
   },
   { 
      {  0.0,  0.0, -0.5 }, // s0
      { -1.0,  0.0,  0.0 }, // s1
      {  0.0,  0.0,  1.0 }, // s2

      { -1.0,  0.0,  0.0 }, // t0
      { -2.0,  0.0,  0.0 }, // t1
      {  1.0,  1.0,  0.0 }, // t2

      0, 1, 2, 3, 6, 4, // Indices

      { -100, -100, -100 },  // result stored here

      0,0, // result0,1
      "Segment-Tri - One point on point C" // Test name
   }
};

closestInterCase closestInterCases[] = {
   {
      {  0.0,  0.0, -1.0 }, // s0
      {  0.0,  0.0,  1.0 }, // s1

      { -1.0,  0.0,  0.0 }, // t0
      {  1.0,  1.0,  0.0 }, // t1
      {  1.0, -1.0,  0.0 }, // t2

      { -1.0,  0.0, -0.5 }, // u0
      {  1.0,  1.0, -0.5 }, // u1
      {  1.0, -1.0, -0.5 },  // u2

      0,0, // result0,1
      "Closest Inter - sanity check" // Test name
   },
   {
      {  0.0,  0.0,  0.0 }, // s0
      {  1.0,  0.0,  0.0 }, // s1

      {  0.0,  1.0, -1.0 }, // t0
      {  0.0,  1.0,  1.0 }, // t1
      {  1.0, -1.0,  0.0 }, // t2

      { 0.75,  0.5, -0.25 }, // u0
      { 0.75,  0.5, -0.3 }, // u1
      { 0.25, -0.5,  0.0 },  // u2

      0,0, // result0,1
      "Closest Inter - crossing and same inter", // Test name
   }
};

int permuteSTS[2][2] = 
{
   { 0,2 },
   { 2,0 }
};
int permuteSTT[6][3] =
{
   { 3, 4, 5 },
   { 5, 3, 4 },
   { 4, 5, 3 },
   { 3, 5, 4 },
   { 4, 3, 5 },
   { 5, 4, 3 }
};

int permuteCIT[6][3] =
{
   { 2, 3, 4 },
   { 4, 2, 3 },
   { 3, 4, 2 },
   { 2, 4, 3 },
   { 3, 2, 4 },
   { 4, 3, 2 }
};
int permuteCIU[6][3] =
{
   { 5, 6, 7 },
   { 6, 7, 5 },
   { 7, 5, 6 },
   { 7, 6, 5 },
   { 6, 5, 7 },
   { 5, 7, 6 }
};

/*
 * Generate 3 random euler angles
 * 0 <= r0, r2 <= 2*pi
 * 0 <= r1     <= pi
 */
void getRandomAngles( double *r0, double *r1, double *r2 ) {
   // random Euler angles
   double _r0 = (double)rand();
   double _r1 = (double)rand();
   double _r2 = (double)rand();
   _r0 /= RAND_MAX; _r1 /= RAND_MAX; _r2 /= RAND_MAX;
   _r0 *= 2*PI; _r1 *= PI; _r2 *= 2*PI;

   *r0 = _r0;
   *r1 = _r1;
   *r2 = _r2;
}

/*
 * Create a 3x3 rotation matrix from euler angles
 * xang, yang, and zang
 * Store the result in result
 */ 
void makeRotationMatrix( double xang, double yang, double zang, matrix3x3 *result ) {
   result->A[0][0] = cos(yang)*cos(zang);
   result->A[1][0] = cos(yang)*sin(zang);
   result->A[2][0] = -sin(yang);

   result->A[0][1] = -cos(xang)*sin(zang)+sin(xang)*sin(yang)*cos(zang);
   result->A[1][1] = cos(xang)*cos(zang) +sin(xang)*sin(yang)*sin(zang);
   result->A[2][1] = sin(xang)*cos(yang);

   result->A[0][2] = sin(xang)*sin(zang) +cos(xang)*sin(yang)*cos(zang);
   result->A[1][2] = -sin(xang)*cos(zang)+cos(xang)*sin(yang)*sin(zang);
   result->A[2][2] = cos(xang)*cos(yang);

   //fprintf( fp_matrices,"%.60e, %.60e, %.60e, ", rot[0][0], rot[1][0], rot[2][0]);
   //fprintf( fp_matrices,"%.60e, %.60e, %.60e, ", rot[0][1], rot[1][1], rot[2][1]);
   //fprintf( fp_matrices,"%.60e, %.60e, %.60e, ", rot[0][2], rot[1][2], rot[2][2]);
}

void makeRandomRotationMatrix( matrix3x3 *result ) {
   double _r0, _r1, _r2;

   getRandomAngles( &_r0, &_r1, &_r2 );
   makeRotationMatrix( _r0, _r1, _r2, result );

   return;
}

void multiplySTCaseRes( const segTriCase *c, matrix3x3 *A, segTriCase *result) {
   multiply( A, &c->s0, &result->s0 );
   multiply( A, &c->s1, &result->s1 );
   multiply( A, &c->s2, &result->s2 );

   multiply( A, &c->t0, &result->t0 );
   multiply( A, &c->t1, &result->t1 );
   multiply( A, &c->t2, &result->t2 );
}

void multiplySTCase( segTriCase *c, matrix3x3 *A) {
   segTriCase temp;

   multiplySTCaseRes(c,A, &temp);
   memcpy( (void *) c, (void *) &temp, sizeof(segTriCase) );
}

void multiplyCICaseRes( const closestInterCase *c, matrix3x3 *A, closestInterCase *result) {
   multiply( A, &c->s0, &result->s0 );
   multiply( A, &c->s1, &result->s1 );

   multiply( A, &c->t0, &result->t0 );
   multiply( A, &c->t1, &result->t1 );
   multiply( A, &c->t2, &result->t2 );

   multiply( A, &c->u0, &result->u0 );
   multiply( A, &c->u1, &result->u1 );
   multiply( A, &c->u2, &result->u2 );
}

void multiplyCICase( closestInterCase *c, matrix3x3 *A) {
   closestInterCase temp;

   multiplyCICaseRes(c,A, &temp);
   memcpy( (void *) c, (void *) &temp, sizeof(closestInterCase) );
}

char *getSTDepthName( const depthST depth ) {
   switch( depth ) {
   case D_SHEWCHUK:
      return "Shewchuk's orient3d";
      break;
   case D_SOS:
      return "SoS code (perturbation)";
      break;
   case D_COPLANAR:
      return "Coplanar code";
      break;
   default:
      return "Error, unrecognized depth?";
   }
}

char *getCIDepthName( const depthCI depth ) {
   switch( depth ) {
   case D_INTERVAL:
      return "intervalClosestIntersection";
      break;
   case D_EXACT:
      return "exactClosestIntersection";
      break;
   default:
      return "Error, unrecognized depth?";
   }
}

void printSTCase( const segTriCase *c, FILE * fp, unsigned int caseNumber, const char *szSubTestName, const unsigned int nSubTest, const depthST depth ) {
   fprintf(fp, "\n----------------------------------------");
   fprintf(fp, "\n Segment Triangle case number: %i", caseNumber );
   fprintf(fp, "\n   %s subcase: %i", szSubTestName, nSubTest );
   fprintf(fp, "\n   result0: %i result1: %i", c->result0, c->result1 );
   fprintf(fp, "\n");
   fprintf(fp, "\n s0: (%A, %A, %A)", c->s0.x[0],c->s0.x[1],c->s0.x[2] );
   fprintf(fp, "\n s1: (%A, %A, %A)", c->s1.x[0],c->s1.x[1],c->s1.x[2] );
   fprintf(fp, "\n s2: (%A, %A, %A)", c->s2.x[0],c->s2.x[1],c->s2.x[2] );
   fprintf(fp, "\n");
   fprintf(fp, "\n t0: (%A, %A, %A)", c->t0.x[0],c->t0.x[1],c->t0.x[2] );
   fprintf(fp, "\n t1: (%A, %A, %A)", c->t1.x[0],c->t1.x[1],c->t1.x[2] );
   fprintf(fp, "\n t2: (%A, %A, %A)", c->t2.x[0],c->t2.x[1],c->t2.x[2] );
   fprintf(fp, "\n");
   fprintf(fp, "\n Depth reached: %i", depth);
   fprintf(fp, "\n   %s was reached", getSTDepthName(depth));
   
}

void printCICase( const closestInterCase *c, FILE * fp, unsigned int caseNumber, const char *szSubTestName, const unsigned int nSubTest, const depthCI depth ) {
   fprintf(fp, "\n----------------------------------------");
   fprintf(fp, "\n Closest Intersection case number: %i", caseNumber );
   fprintf(fp, "\n   %s subcase: %i", szSubTestName, nSubTest );
   fprintf(fp, "\n   result0: %i result1: %i", c->result0, c->result1 );
   fprintf(fp, "\n");
   fprintf(fp, "\n s0: (%A, %A, %A)", c->s0.x[0],c->s0.x[1],c->s0.x[2] );
   fprintf(fp, "\n s1: (%A, %A, %A)", c->s1.x[0],c->s1.x[1],c->s1.x[2] );
   fprintf(fp, "\n");
   fprintf(fp, "\n t0: (%A, %A, %A)", c->t0.x[0],c->t0.x[1],c->t0.x[2] );
   fprintf(fp, "\n t1: (%A, %A, %A)", c->t1.x[0],c->t1.x[1],c->t1.x[2] );
   fprintf(fp, "\n t2: (%A, %A, %A)", c->t2.x[0],c->t2.x[1],c->t2.x[2] );
   fprintf(fp, "\n");
   fprintf(fp, "\n u0: (%A, %A, %A)", c->u0.x[0],c->u0.x[1],c->u0.x[2] );
   fprintf(fp, "\n u1: (%A, %A, %A)", c->u1.x[0],c->u1.x[1],c->u1.x[2] );
   fprintf(fp, "\n u2: (%A, %A, %A)", c->u2.x[0],c->u2.x[1],c->u2.x[2] );
   fprintf(fp, "\n");
   fprintf(fp, "\n Depth reached: %i", depth);
   fprintf(fp, "\n   %s was reached", getCIDepthName(depth));
}

/*
 * return 0 if the case fails
 * return n > 0 indicating the depth which was hit
 *    n == 1 passed both high-low tests
 *    n == 2 hit the exactArithmetic code
 */
int processSTCase( segTriCase *c, depthST *depth ) {
   int result0, result1;
   depthST depth0, depth1;

   result0 = intersectSegmentTriangle_d(
               c->is0, c->s0.x,
               c->is1, c->s1.x,
               c->it0, c->t0.x,
               c->it1, c->t1.x,
               c->it2, c->t2.x,
               c->intersectPt.x,
               &depth0
            );
   result1 = intersectSegmentTriangle_d(
               c->is1, c->s1.x,
               c->is2, c->s2.x,
               c->it0, c->t0.x,
               c->it1, c->t1.x,
               c->it2, c->t2.x,
               c->intersectPt.x,
               &depth1
            );
   c->result0 = result0;
   c->result1 = result1;

   (*depth) = MAX( depth0, depth1 );

   // error case
   if( result0 == result1 ) 
      return 1; 

   return 0;
}

/*
 * segment endpoints are flipped and 
 * results compared
 * 
 * if result0 is -1, 0, 1, we expect 
 * result1 to be  1, 0, -1, respectively
 */
int processCICase( closestInterCase *c, depthCI *depth) {
   int result0, result1;
   depthCI depth0, depth1;

   result0 = closestIntersection_d(
               c->s0.x, c->s1.x,
               c->t0.x, c->t1.x, c->t2.x,
               c->u0.x, c->u1.x, c->u2.x,
               &depth0
            );

   result1 = closestIntersection_d(
               c->s1.x, c->s0.x,
               c->t0.x, c->t1.x, c->t2.x,
               c->u0.x, c->u1.x, c->u2.x,
               &depth1
            );
   c->result0 = result0;
   c->result1 = result1;

   (*depth) = MAX( depth0, depth1 );
   
   if(result1 == -2 || result0 == -2)
      printf("\n******uh oh!!!!!**********");
   
   // error case
   if( result0*(-1) != result1 ) { 
      //printf("\nresult0: %i, result1: %i", result0, result1);
      return 1;
   }

   return 0;
}

void addSTNoise( segTriCase *c ) {
   addNoise( &c->s0 );
   addNoise( &c->s1 );
   addNoise( &c->s2 );

   addNoise( &c->t0 );
   addNoise( &c->t1 );
   addNoise( &c->t2 );
}

void addCINoise( closestInterCase *c ) {
   addNoise( &c->s0 );
   addNoise( &c->s1 );

   addNoise( &c->t0 );
   addNoise( &c->t1 );
   addNoise( &c->t2 );

   addNoise( &c->u0 );
   addNoise( &c->u1 );
   addNoise( &c->u2 );
}

int runSTTest( const segTriCase *c, FILE *fp_out, unsigned int nCurrCase, depthST *max, int bRecord, int compare0[], int compare1[], int bReport ) {
   int nFailed=0;
   int res;
   int nSubTest=0;
   depthST maximumDepthReached=D_SHEWCHUK;
   depthST depth;

   res = processSTCase( (segTriCase *) c, &depth);
   maximumDepthReached= MAX( maximumDepthReached, depth );
   if( bRecord ) {
      compare0[nSubTest] = c->result0;
      compare1[nSubTest] = c->result1;
   }
   if( res || 
      (!bRecord && 
        ( c->result0 != compare0[nSubTest] || 
          c->result1 != compare1[nSubTest] )) ) {
      if( bReport ) {
         nFailed++;
         printSTCase( c, fp_out, nCurrCase, "n/a", 0, depth );
      }
   }
   nSubTest++;

   // Do some random rotation tests
   matrix3x3 rot;
   segTriCase spun;
   int i;
   for( i=0; i<nRandomTrials; i++ ) {
      makeRandomRotationMatrix( &rot );
      multiplySTCaseRes( c, &rot, &spun );
      res = processSTCase( (segTriCase *) &spun, &depth );
      maximumDepthReached= MAX( maximumDepthReached, depth );

      if( bRecord ) {
         compare0[nSubTest] = c->result0;
         compare1[nSubTest] = c->result1;
      }
      if( res || 
         (!bRecord && 
           ( c->result0 != compare0[nSubTest] || 
             c->result1 != compare1[nSubTest] )) ) {
         if( bReport ) {
            nFailed++;
            printSTCase( &spun, fp_out, nCurrCase, "Random Rotation", nSubTest, depth );
         }
      }
      nSubTest++;
   }
   
   // Add some random noise
   //nSubTest=0;
   for( i=0; i<nRandomTrials; i++ ) {
      memcpy( (void *) &spun, (void *) c, sizeof(segTriCase) );
      addSTNoise( (segTriCase *) &spun );
      res = processSTCase( (segTriCase *) &spun, &depth );
      maximumDepthReached= MAX( maximumDepthReached, depth );

      if( bRecord ) {
         compare0[nSubTest] = c->result0;
         compare1[nSubTest] = c->result1;
      }
      if( res || 
         (!bRecord && 
           ( c->result0 != compare0[nSubTest] || 
             c->result1 != compare1[nSubTest] )) ) {
         if( bReport ) {
            nFailed++;
            printSTCase( &spun, fp_out, nCurrCase, "Small Noise", nSubTest, depth );
         }
      }
      nSubTest++;
   }


   (*max) = maximumDepthReached;
   return nFailed;
}

int runCITest( const closestInterCase *c, FILE *fp_out, unsigned int nCurrCase, depthCI *max, int bRecord, int compare0[], int compare1[] ) {
   int nFailed=0;
   int res;
   int nSubTest=0;
   depthCI depth;
   depthCI maximumDepthReached=D_INTERVAL;

   res = processCICase( (closestInterCase *) c, &depth );
   maximumDepthReached= MAX( maximumDepthReached, depth );
   if( bRecord ) {
      compare0[nSubTest] = c->result0;
      compare1[nSubTest] = c->result1;
   }
   if( res || 
      (!bRecord && 
        ( c->result0 != compare0[nSubTest] || 
          c->result1 != compare1[nSubTest] )) ) {
      nFailed ++;
      printCICase( c, fp_out, nCurrCase, "n/a", nSubTest, depth );
   }
   nSubTest++;

   // Do some random rotation tests
   matrix3x3 rot;
   closestInterCase spun;
   int i;
   for( i=0; i<nRandomTrials; i++ ) {
      makeRandomRotationMatrix( &rot );
      multiplyCICaseRes( c, &rot, &spun );
      if( nCurrCase == 1 && nSubTest == 7 )
         res = processCICase( (closestInterCase *) &spun, &depth );
      else
         res = processCICase( (closestInterCase *) &spun, &depth );
      maximumDepthReached= MAX( maximumDepthReached, depth );

      if( bRecord ) {
         compare0[nSubTest] = c->result0;
         compare1[nSubTest] = c->result1;
      }
      if( res || 
         (!bRecord && 
           ( c->result0 != compare0[nSubTest] || 
             c->result1 != compare1[nSubTest] )) ) {
         nFailed++;
         printCICase( &spun, fp_out, nCurrCase, "Random Rotation", nSubTest, depth );
      }
      nSubTest++;
   }
   
   // Add some random noise
   for( i=0; i<nRandomTrials; i++ ) {
      memcpy( (void *) &spun, (void *) c, sizeof(closestInterCase) );
      addCINoise( &spun );
      res = processCICase( (closestInterCase *) &spun, &depth );
      maximumDepthReached= MAX( maximumDepthReached, depth );

      if( bRecord ) {
         compare0[nSubTest] = c->result0;
         compare1[nSubTest] = c->result1;
      }
      if( res || 
         (!bRecord && 
           ( c->result0 != compare0[nSubTest] || 
             c->result1 != compare1[nSubTest] )) ) {
         nFailed++;
         printCICase( &spun, fp_out, nCurrCase, "Small Noise", nSubTest, depth );
      }
      nSubTest++;
   }

   (*max) = maximumDepthReached;
   return nFailed;
}

void permuteSTCase(segTriCase *c, segTriCase *res, int j, int k) {
   memcpy( (void *) res, (void *) c, sizeof(segTriCase) );

   memcpy( (void*) &(res->s0), (void*) ((void*)c+sizeof(vector3)*permuteSTS[j][0]), sizeof(vector3) );
   memcpy( (void*) &(res->s2), (void*) ((void*)c+sizeof(vector3)*permuteSTS[j][1]), sizeof(vector3) );

   memcpy( (void*) &(res->t0), (void*) ((void*)c+sizeof(vector3)*permuteSTT[k][0]), sizeof(vector3) );
   memcpy( (void*) &(res->t1), (void*) ((void*)c+sizeof(vector3)*permuteSTT[k][1]), sizeof(vector3) );
   memcpy( (void*) &(res->t2), (void*) ((void*)c+sizeof(vector3)*permuteSTT[k][2]), sizeof(vector3) );

}
void permuteCICase(closestInterCase *c, closestInterCase *res, int j, int k) {
   memcpy( (void *) res, (void *) c, sizeof(closestInterCase) );

   memcpy( (void*) &(res->t0), (void*) ((void*)c+sizeof(vector3)*permuteCIT[j][0]), sizeof(vector3) );
   memcpy( (void*) &(res->t1), (void*) ((void*)c+sizeof(vector3)*permuteCIT[j][1]), sizeof(vector3) );
   memcpy( (void*) &(res->t2), (void*) ((void*)c+sizeof(vector3)*permuteCIT[j][2]), sizeof(vector3) );

   memcpy( (void*) &(res->u0), (void*) ((void*)c+sizeof(vector3)*permuteCIU[k][0]), sizeof(vector3) );
   memcpy( (void*) &(res->u1), (void*) ((void*)c+sizeof(vector3)*permuteCIU[k][1]), sizeof(vector3) );
   memcpy( (void*) &(res->u2), (void*) ((void*)c+sizeof(vector3)*permuteCIU[k][2]), sizeof(vector3) );
}

/*
 * ---------------------------------------------------------------------
 * Main code starts now.
 * ---------------------------------------------------------------------
 */
int main(int argc, char** argv) {
   unsigned int numSTCases = sizeof(segTriCases) /sizeof(segTriCase);
   unsigned int numCICases = sizeof(closestInterCases) /sizeof(closestInterCase);

   unsigned int nCurrSTCase=0;
   unsigned int nCurrCICase=0;

   unsigned int nFailed=0;
   unsigned int nCurrTestIndex=0;

   unsigned int nSeed=4631973;

   matrix3x3 rot;

   depthST maxST=0;
   depthCI maxCI=0;

   FILE *fp_err = fopen("error.log", "w+");
   if( !fp_err )
      fp_err = stdout;

   // Don't forget to initialize the exact code!!!
   epsilon = exactinit();
   printf("\nepsilon: %e", epsilon);

   // ------------------------------------
   // Do the segment-triangle cases 
   // ------------------------------------
   int i;
   int *aResult0, *aResult1, *aResult2, *aResult3, *aResult4, *aResult5;
   // this many because noise + rotation + one no-noise case
   int nResults = nRandomTrials*2+1;
   aResult0 = malloc( nResults * sizeof(int) );
   aResult1 = malloc( nResults * sizeof(int) );
   aResult2 = malloc( nResults * sizeof(int) );
   aResult3 = malloc( nResults * sizeof(int) );
   aResult4 = malloc( nResults * sizeof(int) );
   aResult5 = malloc( nResults * sizeof(int) );

   int nStartNum = 3;
   //for( i=0; i<numSTCases; i++ ) {
   for( i=0; i<2; i++ ) { // test 0, test 1
      srand(nSeed);
      depthST temp;
      nFailed += runSTTest( &segTriCases[i], fp_err, nCurrSTCase, &temp, 1, aResult0, aResult1, 1 );
      maxST = MAX( maxST, temp );

      // Permute the vertices
      int j;
      int k;
      segTriCase *c = &(segTriCases[i]);
      segTriCase spun;
      for( j=0; j<2; j++) { // permute segments
         for( k=0; k<6; k++ ) {
            permuteSTCase(c, &spun, j, k);

            srand(nSeed);
            if( j == 0 )
               nFailed += runSTTest( &spun, fp_err, nCurrSTCase, &temp, 0, aResult0, aResult1, 1 );
            else // Don't forget to reverse the results for switched segments
               nFailed += runSTTest( &spun, fp_err, nCurrSTCase, &temp, 0, aResult1, aResult0, 1 );
            maxST = MAX( maxST, temp );
         } // k
      } // j

      nCurrTestIndex++;
      nCurrSTCase++;
   }

   for( i=2; i<nStartNum; i++ ) { // test 2,3,4
      srand(nSeed);
      depthST temp;
      runSTTest( &segTriCases[i], fp_err, nCurrSTCase, &temp, 1, aResult0, aResult1, 0 );
      double di0 = segTriCases[i].intersectPt.x[0];
      double di1 = segTriCases[i].intersectPt.x[1];
      double di2 = segTriCases[i].intersectPt.x[2];
      if( di0 - ((segTriCases[i].s0.x[0]+segTriCases[i].s1.x[0]) /2 ) > epsilon ||
          di1 - ((segTriCases[i].s0.x[1]+segTriCases[i].s1.x[1]) /2 ) > epsilon ||
          di2 - ((segTriCases[i].s0.x[2]+segTriCases[i].s1.x[2]) /2 ) > epsilon ) {
         printf("\n\ndi: ( %e, %e, %e )", di0, di1, di2);
         printf("\ns0: ( %e, %e, %e )", segTriCases[i].s0.x[0], segTriCases[i].s0.x[1], segTriCases[i].s0.x[2]);
         printf("\ns1: ( %e, %e, %e )", segTriCases[i].s1.x[0], segTriCases[i].s1.x[1], segTriCases[i].s1.x[2]);
         nFailed++;
      }
      maxST = MAX( maxST, temp );

      // Permute the vertices
      int j;
      int k;
      segTriCase *c = &(segTriCases[i]);
      segTriCase spun;
      for( j=0; j<2; j++) { // permute segments
         for( k=0; k<6; k++ ) {
            permuteSTCase(c, &spun, j, k);

            srand(nSeed);
            runSTTest( &spun, fp_err, nCurrSTCase, &temp, 1, aResult1, aResult0, 0 );
            di0 = segTriCases[i].intersectPt.x[0];
            di1 = segTriCases[i].intersectPt.x[1];
            di2 = segTriCases[i].intersectPt.x[2];
            if( di0 - ((segTriCases[i].s0.x[0]+segTriCases[i].s1.x[0]) /2 ) > epsilon ||
                di1 - ((segTriCases[i].s0.x[1]+segTriCases[i].s1.x[1]) /2 ) > epsilon ||
                di2 - ((segTriCases[i].s0.x[2]+segTriCases[i].s1.x[2]) /2 ) > epsilon ) {
               printf("\n\ndi: ( %e, %e, %e )", di0, di1, di2);
               printf("\ns0: ( %e, %e, %e )", segTriCases[i].s0.x[0], segTriCases[i].s0.x[1], segTriCases[i].s0.x[2]);
               printf("\ns1: ( %e, %e, %e )", segTriCases[i].s1.x[0], segTriCases[i].s1.x[1], segTriCases[i].s1.x[2]);
               nFailed++;
            }
            maxST = MAX( maxST, temp );
         } // k
      } // j

      nCurrTestIndex++;
      nCurrSTCase++;
   }
   { // Point on Edge Test
      // i == 2 and 3 are together
      depthST temp;
      // we expect some failed tests here, so don't record failed, and don't report failures (print case info)
      srand(nSeed);
      runSTTest( &segTriCases[nStartNum++], fp_err, nCurrSTCase, &temp, 1, aResult0, aResult1, 0 );
      maxST = MAX( maxST, temp );
      srand(nSeed);
      runSTTest( &segTriCases[nStartNum++], fp_err, nCurrSTCase, &temp, 1, aResult2, aResult3, 0 );
      maxST = MAX( maxST, temp );

      int nCount;
      for( nCount=0; nCount< nResults; nCount++ ) {
         // For each set of tests, there should be exactly 1 intersection
         int sum = aResult0[nCount] + aResult1[nCount] + aResult2[nCount] + aResult3[nCount];
         if( sum != 1 ) {
            fprintf(fp_err, "\nError in test 2/3 Point on Edge test, nCount: %i", nCount);
            nFailed++;
         }

      }
   }

   { // Point on Point test
      // i == 4, 5 and 6 are together
      depthST temp;
      // we expect some failed tests here, so don't record failed, and don't report failures (print case info)
      srand(nSeed);
      runSTTest( &segTriCases[nStartNum++], fp_err, nCurrSTCase, &temp, 1, aResult0, aResult1, 0 );
      maxST = MAX( maxST, temp );
      srand(nSeed);
      runSTTest( &segTriCases[nStartNum++], fp_err, nCurrSTCase, &temp, 1, aResult2, aResult3, 0 );
      maxST = MAX( maxST, temp );
      srand(nSeed);
      runSTTest( &segTriCases[nStartNum++], fp_err, nCurrSTCase, &temp, 1, aResult4, aResult5, 0 );
      maxST = MAX( maxST, temp );

      int nCount;
      for( nCount=0; nCount< nResults; nCount++ ) {
         // For each set of tests, there should be exactly 1 intersection
         int sum = aResult0[nCount] + aResult1[nCount] + aResult2[nCount] + aResult3[nCount] + aResult4[nCount] + aResult5[nCount];
         if( sum != 1 ) {
            fprintf(fp_err, "\nError in test 4/5/6 Point on Point test, nCount: %i", nCount);
            nFailed++;
         }

      }
   }

   // ------------------------------------
   // Do the Closest Intersection cases 
   // ------------------------------------
   for( i=0; i<numCICases; i++ ) {
      srand(nSeed);
      depthCI temp;
      nFailed += runCITest( &closestInterCases[i], fp_err, nCurrCICase, &temp, 1, aResult0, aResult1 );
      maxCI = MAX( maxCI, temp );

      // Permute the vertices
      int j;
      int k;
      closestInterCase *c = &(closestInterCases[i]);
      closestInterCase spun;
      for( j=0; j<6; j++) { // permute segments
         for( k=0; k<6; k++ ) {
            permuteCICase(c, &spun, j, k);

            srand(nSeed);
            nFailed += runCITest( &spun, fp_err, nCurrCICase, &temp, 0, aResult0, aResult1 );

            maxST = MAX( maxST, temp );
         } // k
      } // j

      nCurrTestIndex++;
      nCurrCICase++;
   }

   free(aResult0);
   free(aResult1);
   free(aResult2);
   free(aResult3);
   free(aResult4);
   free(aResult5);
   // --------------------------------
   // All tests are done!
   // --------------------------------

   printf("\n --- %i tests were run ", nCurrTestIndex);
   printf("\n --- %i segment-triangle tests were run", numSTCases);
   printf("\n        %s was reached", getSTDepthName(maxST));
   printf("\n --- %i closest intersection tests were run", numCICases);
   printf("\n        %s was reached", getCIDepthName(maxCI));
   printf("\n");
   if( nFailed ) {
      printf("\n--------WARNING! %i tests failed --------\n", nFailed );
      printf("\n --- See error.log for details\n");
   } else {
      printf("\nTest successful! no failures!\n");
   }

   if( fp_err && fp_err != stdout )
      fclose( fp_err );

   return 0;

}


