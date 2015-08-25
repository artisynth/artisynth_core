/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.util.RandomGenerator;
import maspack.util.QuarticSolver;
import maspack.util.IntHolder;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.matrix.*;

/**
 * Utility methods for quadratic shapes and surfaces.
 */
public class QuadraticUtils {

   private static double DOUBLE_PREC = 1e-16;

   /**
    * Cpmputes the two tangent points <code>t0</code> and <code>t1</code>
    * between a circle of radius <code>r</code> and a point <code>px</code>
    * exterior to it.  The circle is assumed to be centered at the origin and
    * lie in the x-y plane, and so the z coordinate of <code>px</code> is
    * ignored. The method returns the number of distinct tangent points, which
    * is normally 2. If <code>px</code> lies on the circle, there is only one
    * tangent (which is equal to <code>px</code>) and the method returns 1. If
    * <code>px</code> lies inside the circle, there are no tangent points, the
    * method returns 0, and <code>t0</code> and <code>t1</code> are undefined.
    *
    * @param t0 returns the first tangent point
    * @param t1 returns the second tangent point
    * @param px point for which the tangents should be computed
    * @param r circle radius
    * @return number of unique tangent points
    */
   public static int circleTangentPoints (
      Point3d t0, Point3d t1, Point3d px, double r) {

      double rpSqr = px.x*px.x + px.y*px.y;
      double desc = rpSqr - r*r;
      if (desc < 0) {
         // no solutions
         return 0;
      }
      else if (desc == 0) {
         // just one solution
         t0.set (px);
         t1.set (px);
         t0.z = 0;
         t1.z = 0;
         return 1;
      }
      else {
         double rp = Math.sqrt(rpSqr);
         double sqrt = Math.sqrt(desc);
         if (Math.abs(px.x) > Math.abs(px.y)) {
            // solve for y first
            t0.y = r*(r*px.y + px.x*sqrt)/rpSqr;
            t0.x = (r*r - t0.y*px.y)/px.x;
            t1.y = r*(r*px.y - px.x*sqrt)/rpSqr;
            t1.x = (r*r - t1.y*px.y)/px.x;
         }
         else {
            // solve for x first
            t0.x = r*(r*px.x + px.y*sqrt)/rpSqr;
            t0.y = (r*r - t0.x*px.x)/px.y;
            t1.x = r*(r*px.x - px.y*sqrt)/rpSqr;
            t1.y = (r*r - t1.x*px.x)/px.y;
         }
         t0.z = 0;
         t1.z = 0;
         return 2;
      }
   }

   /**
    * Computes the distance from a point inside an ellipsoid to its surface,
    * along with the associated normal. If the point is outside the ellipsoid,
    * the method returns 0 and the normal is not computed.
    */
   public static double ellipsoidPenetrationDistance (
      Vector3d nrm, Vector3d pos, double a, double b, double c) {
      
      return ellipsoidPenetrationDistance (nrm, pos, a, b, c, 1e-12, null);
   }

   public static double ellipsoidPenetrationDistance (
      Vector3d nrm, Vector3d pos, double a, double b, double c,
      double tol, IntHolder numIters) {
      
      int maxi = 1000;
      if (numIters != null) {
         maxi = numIters.value;
         numIters.value = 0;
      }

      double aSqr = a*a;
      double bSqr = b*b;
      double cSqr = c*c;

      double x = pos.x;
      double y = pos.y;
      double z = pos.z;

      if (tol <= 0) {
         tol = 1e-8;
      }

      double adist = x*x/aSqr + y*y/bSqr + z*z/cSqr;
      if (adist > 1.0) {
         return 0;
      }
      else if (adist < DOUBLE_PREC) {
         int minAxis;
         if (a <= b) {
            minAxis = (a <= c ? 0 : 2);
         }
         else {
            minAxis = (b <= c ? 1 : 2);
         }
         switch (minAxis) {
            case 0: {
               nrm.set (x >= 0 ? 1 : -1, 0, 0);
               return -a;
            }
            case 1: {
               nrm.set (0, y >= 0 ? 1 : -1, 0);
               return -b;
            }
            case 2: {
               nrm.set (0, 0, z >= 0 ? 1 : -1);
               return -c;
            }
            default: {
               // can't happen
               return 0;
            }
         }
      }
      else {
         Vector3d ps = new Vector3d();
         Vector3d n = new Vector3d();
         Vector3d bvec = new Vector3d(2*pos.x/aSqr, 2*pos.y/bSqr, 2*pos.z/cSqr);
         ps.scale (1/Math.sqrt(adist), pos);
         nrm.set (ps.x/aSqr, ps.y/bSqr, ps.z/cSqr);
         int icnt = 0;
         double res = 0;

         //System.out.println ("nrm=" + nrm.toString ("%14.10f"));
         do {
            //System.out.println ("ps= " + ps.toString ("%18.14f"));
            n.set (nrm);
            double aa = n.x*n.x/aSqr + n.y*n.y/bSqr + n.z*n.z/cSqr;
            double bb = bvec.dot (n);
            double cc = adist-1;
            double disc = Math.max(0, bb*bb - 4*aa*cc);
            double lam = (-bb+Math.sqrt(disc))/(2*aa);
            //System.out.println ("lam=" + lam);
            ps.scaledAdd (lam, n, pos);
            nrm.set (ps.x/aSqr, ps.y/bSqr, ps.z/cSqr);
            //System.out.println ("nrm=" + nrm.toString ("%14.10f"));
            res = nrm.distance(n);
         }
         while (++icnt < maxi && res > tol);
         nrm.normalize();
         if (numIters != null) {
            numIters.value = icnt;
         }
         double d = ps.distance (pos);
         return -d;
      }
   }

   /**
    * Computes the numerator of d^2 for a given value of t
    */   
   private static double computeDistanceSqr (
      double t, double b1, double b2, double b3,
      double b4, double b5, double b6) {

      double s = 2*t/(1+t*t);
      double c = (1-t*t)/(1+t*t);
      return b1*s*s + b2*c*c + b3*s*c + b4*s + b5*c + b6;
   }

   /**
    * Find the point <code>pt</code> for an axis-aligned ellipsoid that
    * is tangent to the line <code>p0</code>-<code>pt</code> and closest
    * to the line defined by <code>p0</code> and <code>p1</code>.
    */
   public static void ellipsoidSurfaceTangent (
      Point3d pt, Point3d p0, Point3d p1, double a, double b, double c) {

      double asq = a*a;
      double bsq = b*b;
      double csq = c*c;

      Point3d p = new Point3d();
      Point3d q = new Point3d();
      Vector3d u = new Vector3d();
      Vector3d r = new Vector3d();

      // Start by seeing if p0 is inside the ellipsoid. If it is, just set pt
      // to the surface projection of p0 and return.
      double adist = p0.x*p0.x/asq + p0.y*p0.y/bsq + p0.z*p0.z/csq;
      if (adist <= 1) {
         Vector3d nrm = new Vector3d();
         double d = ellipsoidPenetrationDistance (nrm, p0, a, b, c);
         pt.scaledAdd (-d, nrm, p0);
         return;
      }

      // Find a transformation for the plane on which tangent point must
      // lie. We compute the coordinate directions for the plane.
      Vector3d newz = new Vector3d(p0.x / asq, p0.y / bsq, p0.z / csq);
      newz.normalize();
      RigidTransform3d TPW = new RigidTransform3d();
      TPW.R.setZDirection (newz);
      TPW.p.scale (1.0/adist, p0);

      // Now find the quadratic coefficinets A, B, C, and F for the ellipse in
      // the plane. The cioefs D and E will be zero.
      RotationMatrix3d R = TPW.R;
      p.set (TPW.p);
      double A = R.m00*R.m00/asq + R.m10*R.m10/bsq + R.m20*R.m20/csq;
      double B = 2*(R.m00*R.m01/asq + R.m10*R.m11/bsq + R.m20*R.m21/csq);
      double C = R.m01*R.m01/asq + R.m11*R.m11/bsq + R.m21*R.m21/csq;
      //D = 2*(R.m00*p.x/asq + R.m10*p.y/bsq + R.m20*p.z/csq);
      //E = 2*(R.m01*p.x/asq + R.m11*p.y/bsq + R.m21*p.z/csq);
      double F = p.x*p.x/asq + p.y*p.y/bsq + p.z*p.z/csq - 1;

      // find the rotation angle that aligns the ellipse with the principal axes
      double theta = 0;
      if (Math.abs(C-A) > 100*DOUBLE_PREC*(A+C)) {
         theta = Math.atan2(B, A-C)/2;
         TPW.R.mulRotZ (theta);
      }
      // recompute A and C for the rotated ellipse. B will now be zero.
      A = R.m00*R.m00/asq + R.m10*R.m10/bsq + R.m20*R.m20/csq;
      C = R.m01*R.m01/asq + R.m11*R.m11/bsq + R.m21*R.m21/csq;

      // set p and q to p0 and p1 with respect to the plane
      p.inverseTransform (TPW, p0);
      q.inverseTransform (TPW, p1);
      u.sub (p, q);
      r.cross (p, q);

      // compute a and b values for the ellipse, store in aa and bb
      double aasq = -F/A;
      double bbsq = -F/C;
      double aa = Math.sqrt (aasq);
      double bb = Math.sqrt (bbsq);

      // distance squared from a point x on the ellipse to the line is
      //
      //       ||(x-p) X (x-q)||^2 
      // d^2 = ------------------
      //            ||q-p||^2   
      //
      // Expressing x parametrically as x = ( a cos(theta), b sin(theta), 0 )^T
      // and expanding leads to an expression for the numerator of d^2 given by
      // the polynomial
      //
      // b1 S^2 + b2 C^2 + b3 S C + b4 S + b5 C + b6
      // 
      // where S = sin(theta) and C = cos(theta) and the coefficients bi
      // are given by:
      double b1 = bbsq*(u.z*u.z + u.x*u.x);
      double b2 = aasq*(u.z*u.z + u.y*u.y);
      double b3 = -2*aa*bb*u.x*u.y;
      double b4 = 2*bb*(u.z*r.x - u.x*r.z);
      double b5 = 2*aa*(u.y*r.z - u.z*r.y);
      double b6 = r.normSquared();

      // Differentiating, applying the tan half angle substitution
      //       
      //        2t            (1 - t^2)
      // S = ---------    C = ---------
      //     (1 + t^2)        (1 + t^2)
      //
      // and removing the numerator by multiplying by (1 + t^2)^2
      // results in a forth order polynomial
      //
      // a1 t^4 + a2 t^3 + a3 t^2 + a4 t + a5
      //
      // with coefficients given by:
      double a1 = b3-b4;
      double a2 = 4*(b2-b1)-2*b5;
      double a3 = -6*b3;
      double a4 = 4*(b1-b2)-2*b5;
      double a5 = b3+b4;

      // The maximum and minimum distances will correspond to the zeros of this
      // polynomial. We start by looking for zeros in the interval [-1, 1],
      // which corresponds to the theta interval [-PI/2, PI/2]. For each zero,
      // we compute the distance and then use the overall minimum distance.
      double thetaMin = 0;
      double dsqMin = Double.POSITIVE_INFINITY;
      double[] roots = new double[4];
      int numr = QuarticSolver.getRoots (roots, a1, a2, a3, a4, a5, -1, 1);
      for (int i=0; i<numr; i++) {
         double dsq = computeDistanceSqr (roots[i], b1, b2, b3, b4, b5, b6);
         if (dsq < dsqMin) {
            dsqMin = dsq;
            thetaMin = 2*Math.atan (roots[i]);
         }
      }
      // Now we search for zeros corresponding to the theta in the interval
      // [PI/2, 3*PI/2]. For this, we use the tan half angle substitution
      // for phi = theta - PI, which gives the same polynomial for the
      // numerator of d^2, only with S and C negated. This means we only
      // need to negate b4 and b4:
      b4 = -b4;
      b5 = -b5;
      // and then recompute some of the a coefficents for the derivative
      // polynomial
      a1 = b3-b4;
      a2 = 4*(b2-b1)-2*b5;
      a4 = 4*(b1-b2)-2*b5;
      a5 = b3+b4;
      // Now we find roots again, using a slightly expanded interval to make
      // sure we catch roots on the boundary.
      numr = QuarticSolver.getRoots (roots, a1, a2, a3, a4, a5, -1.001, 1.001);
      for (int i=0; i<numr; i++) {
         double dsq = computeDistanceSqr (roots[i], b1, b2, b3, b4, b5, b6);
         if (dsq < dsqMin) {
            dsqMin = dsq;
            thetaMin = 2*Math.atan (roots[i]) + Math.PI;
         }
      }
      // compute pt in local coordinates and then convert back to world
      // coordinates:
      pt.set (aa*Math.cos(thetaMin), bb*Math.sin(thetaMin), 0);
      pt.transform (TPW);
   }

}
