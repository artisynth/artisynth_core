#include <math.h>
#include <fenv.h>
#include <stdio.h>
#include "geoPred.h"
#include "ahoVectorMacro.h"

/*
 * geoPred.c
 * This file is the entry point for intersectSegmentTriangle
 * and closestIntersection fucntions, which are called from
 * RobustPredsJNI.
 *
 * The implementation of nasaOrient3d and exactClosestIntersection
 * live in geoPredShewchuck and geoPredAJL, for their respective
 * versions
 *
 * See Aftosmis paper (Robust and Efficient Cartesian...)
 */

/* If intersection exists, return 1 and set point[]=x,y,z
 * If no intersection, return 0.
 * If error encountered, return < 0 */

extern int intersectSegmentTriangle(
	int is0, double s0[3], int is1, double s1[3],
	int it0, double t0[3], int it1, double t1[3], int it2, double t2[3],
	double point[3]
) {
   depthST dummy;
   return intersectSegmentTriangle_d(
      is0, s0, is1, s1,
      it0, t0, it1, t1, it2, t2,
      point, &dummy
   );
}


extern int intersectSegmentTriangle_d(
	int is0, double s0[3], int is1, double s1[3],
	int it0, double t0[3], int it1, double t1[3], int it2, double t2[3],
	double point[3], depthST *depth
) {
	int w0, w1, w2;
	double b0, b1, b2;
   /*
    * First determine if the two points are on opposite
    * sides of the plane
    */
   (*depth) = D_SHEWCHUK;
   depthST depthTemp=D_SHEWCHUK;
	w0 = nasaOrient3d_d(is1, s1, it0, t0, it1, t1, it2, t2, &b0, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
	if (w0 < 0) 
      return w0;
	w1 = nasaOrient3d_d(is0, s0, it0, t0, it1, t1, it2, t2, &b0, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
	if (w1 < 0) 
      return w1;
	if (w1 == w0) 
      return 0;
   /*
    * Now check if the edge passes the plane within
    * the face edges
    */
	w0 = nasaOrient3d_d(is0, s0, is1, s1, it1, t1, it2, t2, &b0, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
	if (w0 < 0) 
      return w0;
	w1 = nasaOrient3d_d(is0, s0, is1, s1, it2, t2, it0, t0, &b1, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
	if (w1 < 0) 
      return w1;
	if (w1 != w0) 
      return 0;
	w2 = nasaOrient3d_d(is0, s0, is1, s1, it0, t0, it1, t1, &b2, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
	if (w2 < 0) 
      return w2;
	if (w2 != w0) 
      return 0;

	/* Compute the intersection point based on the triangle barycentric
	 * coordinates, and then project it onto the line segment. */
	double t = b0 + b1 + b2;

	if (t != 0) {
		/* Compute vector from s0 to the triangle intersection point,
		 * and project it onto the segment. */
		point[0] = (t0[0]*b0 + t1[0]*b1 + t2[0]*b2) / t - s0[0];
		point[1] = (t0[1]*b0 + t1[1]*b1 + t2[1]*b2) / t - s0[1];
		point[2] = (t0[2]*b0 + t1[2]*b1 + t2[2]*b2) / t - s0[2];

		double ex = s1[0] - s0[0];
		double ey = s1[1] - s0[1];
		double ez = s1[2] - s0[2];
		double r = sqrt(ex*ex + ey*ey + ez*ez);
		ex /= r;	/* e is a unit vector from s0 to s1 */
		ey /= r;
		ez /= r;
		double w = point[0]*ex + point[1]*ey + point[2]*ez;
		if (w <= 0) {
			point[0] = s0[0];
			point[1] = s0[1];
			point[2] = s0[2];
		} else {
			if (w >= r) {
				point[0] = s1[0];
				point[1] = s1[1];
				point[2] = s1[2];
			} else {
				point[0] = ex*w + s0[0];
				point[1] = ey*w + s0[1];
				point[2] = ez*w + s0[2];
			}
		}
		return 1;
	}

   (*depth) = D_COPLANAR;
   /*
    * aho - I believe we only reach here if the 
    * triangle and segment are co-planar... other 
    * degenerate cases should still return volumes
    * (b0, b1, b2) for at least 1 of the predicates
    * 
    * Now the question is how to react. There is definitely
    * an intersection, (due to SoS). What intersection
    * should we return? Surely it should be on the segment
    * and within the plane of the triangle. Beyond this
    * does it matter??
    *
    * Find the intersection by testing 3 edges:
    * Solve (D10  -D  DxD10) (t10  t  tv)^T = (e0 - V0)
    * 3 times, one for each edge
    * tv should be 0 (check)
    * if DxD10 is zero, it's ok, just skip this one
    */

   //printf("\nEntering coplanar edge-face code...\n");
   double D01[3], D12[3], D20[3], D[3];
   double t_0, t_1, t_2;
   double t01, t12, t20, tv;
   double epsilon = 1e-16;

   SUBVEC(s0,s1,D); // store -D instead
   SUBVEC(t1,t0,D01);
   SUBVEC(t2,t1,D12);
   SUBVEC(t0,t2,D20);

   double DxD01[3];
   CROSS(D,D01,DxD01);
   double DxD12[3];
   CROSS(D,D12,DxD12);
   double DxD20[3];
   CROSS(D,D20,DxD20);
   
   // Edge 01
   if(NORM_SQUARE(DxD01) > epsilon) {
      double res[3];
      double e0v0[3];
      SUBVEC(s0,t0,e0v0);
      double* A[3];
      A[0] = D01;
      A[1] = D;
      A[2] = DxD01;
      Solve3x3(A,e0v0,res);
      t01 = res[0];
      t_0 = res[1];
      tv = res[2];
   }
   else
      t01 = -100;

   // Edge 12
   if(NORM_SQUARE(DxD12) > epsilon) {
      double res[3];
      double e0v1[3];
      SUBVEC(s0,t1,e0v1);
      double* A[3];
      A[0] = &D12[0];
      A[1] = &D[0];
      A[2] = &DxD12[0];
      Solve3x3(A,e0v1,res);
      t12 = res[0];
      t_1 = res[1];
      tv = res[2];
   }
   else
      t12 = -100;
   
   // Edge 20
   if(NORM_SQUARE(DxD20) > epsilon) {
      double res[3];
      double e0v2[3];
      SUBVEC(s0,t2,e0v2);
      double* A[3];
      A[0] = &D20[0];
      A[1] = &D[0];
      A[2] = &DxD20[0];
      Solve3x3(A,e0v2,res);
      t20 = res[0];
      t_2 = res[1];
      tv = res[2];
   }
   else
      t20 = -100;
   
   // Now figure out the midpoint of the two intersecting edges
   double tta, ttb;
   if( t01 > 1+epsilon || t01 < -epsilon ) { // did not cross edge 01
      if( t_1 < 0 )
         tta = 0;
      else if(t_1 >1)
         tta = 1;
      else
         tta = t_1;

      if( t_2 < 0 )
         ttb = 0;
      else if(t_2 >1)
         ttb = 1;
      else
         ttb = t_2;
   }
   
   else if( t12 > 1+epsilon || t12 < -epsilon ) { // did not cross edge 12
      if( t_0 < 0 )
         tta = 0;
      else if(t_0 >1)
         tta = 1;
      else
         tta = t_0;

      if( t_2 < 0 )
         ttb = 0;
      else if(t_2 >1)
         ttb = 1;
      else
         ttb = t_2;
   }
   
   else if( t20 > 1+epsilon || t20 < -epsilon ) { // did not cross edge 20
   //else {
      if( t_0 < 0 )
         tta = 0;
      else if(t_0 >1)
         tta = 1;
      else
         tta = t_0;

      if( t_1 < 0 )
         ttb = 0;
      else if(t_1 >1)
         ttb = 1;
      else
         ttb = t_1;
   }
   else
      printf("\nerror, hasn't crossed any edge");

   double tt=tta+ttb*0.5;
   if( tt < 0 || tt > 1 ) {
      printf("\ntt = %e, tta = %e, ttb = %e",tt,tta,ttb);
      printf("\n   t_0 = %e, t_1 = %e, t_2 = %e",t_0,t_1,t_2);
      printf("\n   t01 = %e, t12 = %e, t20 = %e",t01,t12,t20);
      printf("\n");
      return -10; // now return the error
   }
   // point = s1+tt*D
   point[0] = s0[0] - tt*D[0]; // remember D stores -D
   point[1] = s0[1] - tt*D[1]; // remember D stores -D
   point[2] = s0[2] - tt*D[2]; // remember D stores -D
/*
   printf("\n  intersectionPoint: (%e, %e, %e)", point[0], point[1], point[2]);
   printf("\n s0+s1/2: (%e, %e, %e)", (s0[0]+s1[0])/2, (s0[1]+s1[1])/2, (s0[2]+s1[2])/2 );
   printf("\n  s0: (%e, %e, %e), s1: (%e, %e, %e)", s0[0], s0[1], s0[2], s1[0], s1[1], s1[2]);
   printf("\n  t0: (%e, %e, %e), t1: (%e, %e, %e), t2: (%e, %e, %e)", t0[0], t0[1], t0[2], t1[0], t1[1], t1[2], t2[0], t2[1], t2[2]);
   */


   return 1; // we have an intersection because of SoS

	//return -10; 
}


/* Return 
 * 1 if d is closer
 * 0 if c and d are equidistant
 * -1 if c is closer
 * -2 if an error was encountered (ie MaxDoubles is set too low). */
extern int closestIntersection(
	double a[3], double b[3],
	double c0[3], double c1[3], double c2[3], 
	double d0[3], double d1[3], double d2[3] )
{
   depthCI dummy;
   return closestIntersection_d(
      a, b, 
      c0, c1, c2,
      d0, d1, d2, &dummy 
   );

}

extern int closestIntersection_d(
	double a[3], double b[3],
	double c0[3], double c1[3], double c2[3], 
	double d0[3], double d1[3], double d2[3], depthCI *depth
) {
	double result;
   int w;
   (*depth)=D_INTERVAL;
	w = intervalClosestIntersection(a, b, c0, c1, c2, d0, d1, d2, &result);
	if (w == 1) 
      return result > 0 ? 1 : (result == 0 ? 0 : -1);

   (*depth)=D_EXACT;
	w = exactClosestIntersection(a, b, c0, c1, c2, d0, d1, d2, &result);
	return w == 1 ? (result > 0 ? 1 : (result == 0 ? 0 : -1)) : -2;
}

