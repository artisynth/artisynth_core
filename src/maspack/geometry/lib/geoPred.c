#include <math.h>
#include <fenv.h>
#include <stdio.h>
#include "geoPred.h"
#include "ahoVectorMacro.h"

// dominant plane identifiers. These also give the index of the dominant
// normal direction.
#define YZ_PLANE 0
#define ZX_PLANE 1
#define XY_PLANE 2

#define DOUBLE_PREC 1e-16

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

/* If intersection exists, return flags and set point[]=x,y,z If no
 * intersection, return 0. Flags values are defined in geoPred.h.
 */
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

double computePlanarArea (double p0[3], double p1[3], double p2[3], int plane) {

   if (plane == XY_PLANE) {
      double d1x = p1[0] - p0[0];
      double d2x = p2[0] - p0[0];
      double d1y = p1[1] - p0[1];
      double d2y = p2[1] - p0[1];
      return d1x*d2y - d1y*d2x;
   }
   else if (plane == YZ_PLANE) {
      double d1y = p1[1] - p0[1];
      double d2y = p2[1] - p0[1];
      double d1z = p1[2] - p0[2];
      double d2z = p2[2] - p0[2];
      return d1y*d2z - d1z*d2y;
   }
   else { /* plane == ZX_PLANE */ 
      double d1z = p1[2] - p0[2];
      double d2z = p2[2] - p0[2];
      double d1x = p1[0] - p0[0];
      double d2x = p2[0] - p0[0];
      return d1z*d2x - d1x*d2z;
   }
}

void clipIntervalAgainstEdge (
   double sminmax[2], double s0[3], double s1[3], double p0[3], double p1[3],
   int plane, double sign, double tol) {

   double a0 = sign*computePlanarArea (p0, p1, s0, plane);
   double a1 = sign*computePlanarArea (p0, p1, s1, plane);

   //printf ("a0=%g a1=%g\n", a0, a1);
   if (ABS(a0) < tol && ABS(a1) < tol) {
      // don't clip if there is no clear edge crossig
      return;
   }

   double smin = sminmax[0];
   double smax = sminmax[1];

   if (a1 < 0) {
      if (a0 > 0) {
         double s = a0/(a0-a1);
         smax = MAX(smin,s);
      }
      else if (a0 < a1) {
         smin = MIN(1.0,smax);
      }
      else {
         smax = MAX(smin,0.0);
      }
   }
   else if (a0 < 0) {
      double s = -a0/(a1-a0);
      smin = MIN(s,smax);
   }
   
   sminmax[0] = smin;
   sminmax[1] = smax;
}

/**
 * Tests for the intersection of a line segment [s0,s1] with the
 * triangle [t0,t1,t2]. Returns 0 is there is no intersection and a
 * non-zero set of flag values if there is. Flag values are defined in
 * geoPred.h.
 */
extern int intersectSegmentTriangle_d(
   int is0, double s0[3], int is1, double s1[3],
   int it0, double t0[3], int it1, double t1[3], int it2, double t2[3],
   double point[3], depthST *depth
   ) {
   int ws0, ws1, w0, w1, w2;
   double v0[2]; // v0 and v1 are two-double precision values
   double v1[2];
   double b0, b1, b2;
   int rcode = GP_INTERSECTS;

   /*
    * First determine if the two points are on opposite sides of the
    * plane. Given how points are ordered in the calls to orient, a
    * positive value means that the point is *outside* the triangle.
    * 
    * We use nasaOrient3d_vol(), which is slower than nasaOrient3d_d()
    * but which returns the determinant volume as a two-double
    * precision value. This will later enable the intersection point
    * to be computed to higher precision.
    */
   (*depth) = D_SHEWCHUK;
   depthST depthTemp=D_SHEWCHUK;
   ws1 = nasaOrient3d_vol(is1, s1, it0, t0, it1, t1, it2, t2, v1, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
   if (v1[0] == 0) {
      rcode |= GP_S1_COPLANAR;
   }
   ws0 = nasaOrient3d_vol(is0, s0, it0, t0, it1, t1, it2, t2, v0, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
   if (v0[0] == 0) {
      rcode |= GP_S0_COPLANAR;
   }
   if (ws1 == ws0) {
      return 0;
   }

   if (ws0 == 1) {
      rcode |= GP_S0_OUTSIDE;
   }

   /*
    * Now check if the edge passes the plane within
    * the face edges
    */
   w0 = nasaOrient3d_d(is0, s0, is1, s1, it1, t1, it2, t2, &b0, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
   if (b0 == 0) {
      rcode |= GP_E12_ON_SEGMENT;
   }
   w1 = nasaOrient3d_d(is0, s0, is1, s1, it2, t2, it0, t0, &b1, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
   if (w1 != w0) {
      return 0;
   }
   if (b1 == 0) {
      rcode |= GP_E20_ON_SEGMENT;
   }
   w2 = nasaOrient3d_d(is0, s0, is1, s1, it0, t0, it1, t1, &b2, &depthTemp);
   (*depth) = MAX( (*depth), depthTemp );
   if (w2 != w0) {
      return 0;
   }
   if (b2 == 0) {
      rcode |= GP_E01_ON_SEGMENT;
   }

   /*
    * When computing the intersection point, we clip the line segment
    * against the edges of the plane. This provides a safeguard
    * against in case the line segment is nearly coplanar.
    */

   // min and max bounds on the s value used for computing the value
   // of the intersection point within the edge segment
   double sminmax[2];   
   sminmax[0] = 0;
   sminmax[1] = 1;
   
   // compute triangle normal
   double D01[3], D02[3], NRM[3];
   SUBVEC(t1,t0,D01);
   SUBVEC(t2,t0,D02);
   CROSS(D01,D02,NRM);

   // find the dominant direction and associated plane for the triangle norml
   double nrmMax = ABS(NRM[0]);
   double sign = NRM[0] >= 0 ? 1 : -1;
   int nrmPlane = YZ_PLANE;

   if (ABS(NRM[1]) > nrmMax) {
      nrmMax = ABS(NRM[1]);
      sign = NRM[1] >= 0 ? 1 : -1;
      nrmPlane = ZX_PLANE;
   }
   if (ABS(NRM[2]) > nrmMax) {
      nrmMax = ABS(NRM[2]);
      sign = NRM[2] >= 0 ? 1 : -1;
      nrmPlane = XY_PLANE;
   }

   if (nrmMax == 0) {
      // very special case where triangle has degenerated to a line
      // segment.  Because of tie breaking, there should be no
      // intersection, because it would require orient (s0, s1, ta,
      // tb) = 0 for at least one triangle edge.
      return 0;
   }


   // compute the edge segment direction and length
   double DIR[3]; 
   SUBVEC(s1,s0,DIR);
   double segLen = sqrt(NORM_SQUARE(DIR));
   double nrmLen = sqrt(NORM_SQUARE(NRM));
   double cos = 1.0; 
   if (segLen > 0) {
      // cosine of the angle between the segment and the normal
      cos = (DIR[0]*NRM[0] + DIR[1]*NRM[1] + DIR[2]*NRM[2])/(segLen*nrmLen);
   }

   if (ABS(cos) < 0.0001) {
      //printf ("cos=%g\n", cos);
      // The edge segment is close to parallel with the triangle.
      // Because of this, we clip sminmax against the triangle to help
      // ensure a more robust solution in case there are numerical
      // issues associated with the computation of s.

      // clipInterval tolerance should be an area, hence we square seglen
      double tol = segLen*segLen*DOUBLE_PREC;

      clipIntervalAgainstEdge (sminmax, s0, s1, t0, t1, nrmPlane, sign, tol);
      //printf ("sminmax %15.13f %15.13f\n", sminmax[0], sminmax[1]);
      clipIntervalAgainstEdge (sminmax, s0, s1, t1, t2, nrmPlane, sign, tol);
      //printf ("sminmax %15.13f %15.13f\n", sminmax[0], sminmax[1]);
      clipIntervalAgainstEdge (sminmax, s0, s1, t2, t0, nrmPlane, sign, tol);
      //printf ("sminmax %15.13f %15.13f\n", sminmax[0], sminmax[1]);
   }

   double vt = ABS(v0[0])+ABS(v1[0]);
   if (vt > 0) {
      // compute intersection value along the segment. Check first to
      // see if intersection lies on a specific vertex:

      if ((rcode & GP_V0_ON_SEGMENT) == GP_V0_ON_SEGMENT) {
         // intersection is at vertex 0
         point[0] = t0[0];
         point[1] = t0[1];
         point[2] = t0[2];
      }
      else if ((rcode & GP_V1_ON_SEGMENT) == GP_V1_ON_SEGMENT) {
         // intersection is at vertex 1
         point[0] = t1[0];
         point[1] = t1[1];
         point[2] = t1[2];
      }
      else if ((rcode & GP_V2_ON_SEGMENT) == GP_V2_ON_SEGMENT) {
         // intersection is at vertex 2
         point[0] = t2[0];
         point[1] = t2[1];
         point[2] = t2[2];
      }
      else {
         // compute intersection from ratio of v0 and v1
         double s;
         if (v0[1] != 0 || v1[1] != 0) {
            // v0 and/or v1 have extra precision data associated with them.
            // compute s using two-double precision arithmetic
            s = computeSegmentScale (v0, v1);
         }
         else {
            // v0 and v1 do not have extra precision data; compute s normally
            s = ABS(v0[0])/vt;
         }
         //sminmax[0] = 0;
         //sminmax[1] = 1;
         if (s < sminmax[0]) {
            //printf ("clipping s min from %15.13f to %15.13f\n", s, sminmax[0]); 
            s = sminmax[0];
         }
         else if (s > sminmax[1]) {
            //printf ("clipping s max from %15.13f to %15.13f\n", s, sminmax[1]);
            s = sminmax[1];
         }
         point[0] = (1-s)*s0[0] + s*s1[0];
         point[1] = (1-s)*s0[1] + s*s1[1];
         point[2] = (1-s)*s0[2] + s*s1[2];
         //printf ("point.x=%g\n", point[0]);
      }
      return rcode;
   }
   else {
      (*depth) = D_COPLANAR;
      // coplanar case: take the midpoint of the clipped segment. It
      // is assumed that the edge segment was determined to be
      // parallel to the triangle and that sminmax was hence clipped
      // to the triangle.
      double s = (sminmax[0]+sminmax[1])/2.0;
      
      point[0] = (1-s)*s0[0] + s*s1[0];
      point[1] = (1-s)*s0[1] + s*s1[1];
      point[2] = (1-s)*s0[2] + s*s1[2];
      return rcode;
   }

}

// Old coplanar code - not currently used.
extern int coplanarCode_d(
   int is0, double s0[3], int is1, double s1[3],
   int it0, double t0[3], int it1, double t1[3], int it2, double t2[3],
   double point[3], depthST *depth) {

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
   else {
      t01 = -100;
   }

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
   else {
      t12 = -100;
   }
   
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
   else {
      t20 = -100;
   }
   
   // Now figure out the midpoint of the two intersecting edges
   double tta, ttb;
   if( t01 > 1+epsilon || t01 < -epsilon ) { // did not cross edge 01
      if( t_1 < 0 ) {
         tta = 0;
      }
      else if(t_1 >1) {
         tta = 1;
      }
      else {
         tta = t_1;
      }

      if( t_2 < 0 ) {
         ttb = 0;
      }
      else if(t_2 >1) {
         ttb = 1;
      }
      else {
         ttb = t_2;
      }
   }
   
   else if( t12 > 1+epsilon || t12 < -epsilon ) { // did not cross edge 12
      if( t_0 < 0 ) {
         tta = 0;
      }
      else if(t_0 >1) {
         tta = 1;
      }
      else {
         tta = t_0;
      }

      if( t_2 < 0 ) {
         ttb = 0;
      }
      else if(t_2 >1) {
         ttb = 1;
      }
      else {
         ttb = t_2;
      }
   }
   
   else if( t20 > 1+epsilon || t20 < -epsilon ) { // did not cross edge 20
      //else {
      if( t_0 < 0 ) {
         tta = 0;
      }
      else if(t_0 >1) {
         tta = 1;
      }
      else{
         tta = t_0;
      }

      if( t_1 < 0 ) {
         ttb = 0;
      }
      else if(t_1 >1){
         ttb = 1;
      }
      else {
         ttb = t_1;
      }
   }
   else {
      printf("\nerror, hasn't crossed any edge");
   }

   double tt=tta+ttb*0.5;
   if( tt < 0 || tt > 1 ) {
      printf("\ntt = %e, tta = %e, ttb = %e",tt,tta,ttb);
      printf("\n   t_0 = %e, t_1 = %e, t_2 = %e",t_0,t_1,t_2);
      printf("\n   t01 = %e, t12 = %e, t20 = %e",t01,t12,t20);
      printf("\n   s0 = [ %e, %e, %e ]", s0[0], s0[1], s0[2]);
      printf("\n   s1 = [ %e, %e, %e ]", s1[0], s1[1], s1[2]);
      printf("\n   t0 = [ %e, %e, %e ]", t0[0], t0[1], t0[2]);
      printf("\n   t1 = [ %e, %e, %e ]", t1[0], t1[1], t1[2]);
      printf("\n   t2 = [ %e, %e, %e ]", t2[0], t2[1], t2[2]);
      printf("\n   is0=%d is1=%d it0=%d it1=%d it2=%d", is0, is1, it0, it1, it2);
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

