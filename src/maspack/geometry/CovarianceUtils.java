/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
/**
 * Set of utility methods to compute and transform Covariances.
 */
public class CovarianceUtils {

   public static void computePointCovariance (Matrix3d C, Point3d p) {
      C.outerProduct (p, p);
   }

   public static void addScaledPointCovariance (Matrix3d C, double s, Point3d p) {
      C.addScaledOuterProduct (s, p, p);
   }

   public static double computeLineSegmentCovariance (
      Matrix3d C, Point3d p0, Point3d p1) {

      double l = p0.distance (p1);
      C.m00 = 2*(p0.x*p0.x + p0.x*p1.x + p1.x*p1.x);
      C.m11 = 2*(p0.y*p0.y + p0.y*p1.y + p1.y*p1.y);
      C.m22 = 2*(p0.z*p0.z + p0.z*p1.z + p1.z*p1.z);

      C.m01 = 2*p0.x*p0.y + p0.x*p1.y + p1.x*p0.y + 2*p1.x*p1.y;
      C.m02 = 2*p0.x*p0.z + p0.x*p1.z + p1.x*p0.z + 2*p1.x*p1.z;
      C.m12 = 2*p0.y*p0.z + p0.y*p1.z + p1.y*p0.z + 2*p1.y*p1.z;
      C.scale (l/6);

      C.m10 = C.m01;
      C.m20 = C.m02;
      C.m21 = C.m12;
      return l;
   }

   public static double addTriangleCovariance (
      Matrix3d C, Point3d p0, Point3d p1, Point3d p2) {

      double d1x = p1.x - p0.x;
      double d1y = p1.y - p0.y;
      double d1z = p1.z - p0.z;
      double d2x = p2.x - p0.x;
      double d2y = p2.y - p0.y;
      double d2z = p2.z - p0.z;
      double nx = d1y * d2z - d1z * d2y;
      double ny = d1z * d2x - d1x * d2z;
      double nz = d1x * d2y - d1y * d2x;
      double a = Math.sqrt (nx*nx + ny*ny + nz*nz)/2;

      // compute and add covariance for triangle
      double pcx = (p0.x + p1.x + p2.x) / 3;
      double pcy = (p0.y + p1.y + p2.y) / 3;
      double pcz = (p0.z + p1.z + p2.z) / 3;

      C.m00 += a * (9*pcx*pcx + p0.x*p0.x + p1.x*p1.x + p2.x*p2.x);
      C.m11 += a * (9*pcy*pcy + p0.y*p0.y + p1.y*p1.y + p2.y*p2.y);
      C.m22 += a * (9*pcz*pcz + p0.z*p0.z + p1.z*p1.z + p2.z*p2.z);
         
      C.m01 += a * (9*pcx*pcy + p0.x*p0.y + p1.x*p1.y + p2.x*p2.y);
      C.m02 += a * (9*pcx*pcz + p0.x*p0.z + p1.x*p1.z + p2.x*p2.z);
      C.m12 += a * (9*pcy*pcz + p0.y*p0.z + p1.y*p1.z + p2.y*p2.z);

      return a;
   }


   /**
    * Transforms a covariance matrix into a new coordinate system, given its
    * associated centroid 'area' (or volume).
    *
    * @param CT returns the transformed covariance
    * @param C covariance to transform
    * @param cent centroid associated with the covariance (in old coordinates)
    * @param a area (or length or volume) associated with the covariance
    * @param T transformation from old to new coordinates
    */
   public static void transformCovariance (
      Matrix3d CT, Matrix3d C, Point3d cent, double a, RigidTransform3d T) {

      Point3d centRot = new Point3d (cent);
      Matrix3d CTrans = new Matrix3d ();
      centRot.transform (T.R);

      CT.mul (T.R, C);
      CT.mulTransposeRight (CT, T.R);
      CT.addScaledOuterProduct (a, T.p, T.p);
      CT.addScaledOuterProduct (a, centRot, T.p);
      CT.addScaledOuterProduct (a, T.p, centRot);
   }
}
