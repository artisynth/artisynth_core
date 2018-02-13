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
import maspack.util.InternalErrorException;
import maspack.matrix.*;

/**
 * Utility methods for quadratic shapes and surfaces.
 */
public class QuadraticUtils {

   private static double DOUBLE_PREC = 1e-16;
   private static double EPS = 100*DOUBLE_PREC;

   /**
    * Special value, equal to <code>Double.MAX_VALUE</code>, that indicates a
    * point is outside a particular solid.
    */
   public static double OUTSIDE = Double.MAX_VALUE;

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
    * Computes the distance from a point to the surface of an ellipsoid,
    * along with the associated normal. If the point is outside the
    * ellipsoid such that the algebraic distance <code>a</code> defined by
    * <pre>
    *     x^2   y^2   z^2
    * a = --- + --- + ---
    *     a^2   b^2   c^2
    * </pre>
    * exceeds <code>amax</code>, the method returns
    * {@link #OUTSIDE} and the normal is not computed.
    */
   public static double ellipsoidPenetrationDistance (
      Vector3d nrm, Vector3d pos, double a, double b, double c, double amax) {
      
      return ellipsoidPenetrationDistance (
         nrm, pos, a, b, c, 1e-12, amax, null);
   }

   public static double ellipsoidPenetrationDistance (
      Vector3d nrm, Vector3d pos, double a, double b, double c,
      double tol, double amax, IntHolder numIters) {
      
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

      // compute algebraic distance
      double adist = x*x/aSqr + y*y/bSqr + z*z/cSqr;
      if (adist > amax) {
         return OUTSIDE;
      }
      else if (adist < DOUBLE_PREC) {
         // point is at the center, so look for nearest along the principal
         // axes
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
         //System.out.println ("icnt=" + icnt);
         nrm.normalize();
         if (numIters != null) {
            numIters.value = icnt;
         }
         ps.sub (pos);
         return -ps.dot(nrm);
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
         double d = ellipsoidPenetrationDistance (nrm, p0, a, b, c, 1.0);
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

   /**
    * Find the point <code>pt</code> on an axis-aligned ellipsoid that
    * is tangent to the line <code>pa</code>-<code>pt</code>, lies
    * in the plane defined by <code>p0</code> and <code>nrm</code>,
    * and is nearest to <code>p0</code>.
    *
    * <p>The method works by projecting the ellipsoid into either YZ, ZX, or XY
    * planes, depending on which is closest to the plane in question, and then
    * finding the tangent points for the associated ellipse.
    */
   public static boolean ellipsoidSurfaceTangentInPlane (
      Point3d pt, Point3d pa, Point3d p0, Vector3d nrm,
      double a, double b, double c) {

      // quadratic coefficents for ellispod
      double a0 = 1/(a*a);
      double a1 = 1/(b*b);
      double a2 = 1/(c*c);
      double a9 = -1;

      // Start by seeing if pa is inside the ellipsoid. If it is, just set pa
      // to the surface projection of pa and return.
      double adist = a0*pa.x*pa.x + a1*pa.y*pa.y + a2*pa.z*pa.z;
      if (adist <= 1) {
         Vector3d cnrm = new Vector3d();
         double d = ellipsoidPenetrationDistance (cnrm, pa, a, b, c, 1.0);
         pt.scaledAdd (-d, cnrm, pa);
         return true;
      }

      // compute offset for (p0, nrm) plane
      double off = nrm.dot(p0);
      double[] bc = new double[6];

      Vector2d[] pnts = new Vector2d[] { new Vector2d(), new Vector2d() };
      Point3d[] tans = new Point3d[] { new Point3d(), new Point3d() };

      int nr = 0;
      switch (nrm.maxAbsIndex()) {
         case 0: {
            // YZ plane
            double rx = nrm.y/nrm.x;
            double ry = nrm.z/nrm.x;
            double rz = off/nrm.x;

            bc[0] = a1 + a0*rx*rx;
            bc[1] = a2 + a0*ry*ry;
            bc[2] = 2*a0*rx*ry;
            bc[3] = - 2*a0*rz*rx;
            bc[4] = - 2*a0*rz*ry;
            bc[5] = a9 + a0*rz*rz;

            nr = DistanceGridSurfCalc.findTangentPoints (pnts, bc, pa.y, pa.z);
            for (int i=0; i<nr; i++) {
               double y = pnts[i].x;
               double z = pnts[i].y;
               double x = rz - rx*y - ry*z;
               tans[i].set (x, y, z);
            }
            break;
         }
         case 1: {
            // ZX plane
            double rx = nrm.z/nrm.y;
            double ry = nrm.x/nrm.y;
            double rz = off/nrm.y;

            bc[0] = a2 + a1*rx*rx;
            bc[1] = a0 + a1*ry*ry;
            bc[2] = 2*a1*rx*ry;
            bc[3] = - 2*a1*rz*rx;
            bc[4] = - 2*a1*rz*ry;
            bc[5] = a9 + a1*rz*rz;

            nr = DistanceGridSurfCalc.findTangentPoints (pnts, bc, pa.z, pa.x);
            for (int i=0; i<nr; i++) {
               double z = pnts[i].x;
               double x = pnts[i].y;
               double y = rz - rx*z - ry*x;
               tans[i].set (x, y, z);
            }
            break;
         }
         case 2: {
            // XY plane
            double rx = nrm.x/nrm.z;
            double ry = nrm.y/nrm.z;
            double rz = off/nrm.z;

            bc[0] = a0 + a2*rx*rx;
            bc[1] = a1 + a2*ry*ry;
            bc[2] = 2*a2*rx*ry;
            bc[3] = - 2*a2*rz*rx;
            bc[4] = - 2*a2*rz*ry;
            bc[5] = a9+ a2*rz*rz;

            nr = DistanceGridSurfCalc.findTangentPoints (pnts, bc, pa.x, pa.y);
            for (int i=0; i<nr; i++) {
               double x = pnts[i].x;
               double y = pnts[i].y;
               double z = rz - rx*x - ry*y;
               tans[i].set (x, y, z);
            }
            break;
         }
         default: {
            throw new InternalErrorException (
               "Vector3d.maxAbsIndex() returned value "+nrm.maxAbsIndex());
         }
      }
      if (nr == 2) {
         // if there are two tangent points, take the nearest to p0
         if (tans[0].distance(p0) < tans[1].distance(p0)) {
            pt.set (tans[0]);
         }
         else {
            pt.set (tans[1]);
         }
         return true;
      }
      else if (nr == 1) {
         pt.set (tans[0]);
         return true;
      }
      else {
         // if no tangents computed, use p0 as a fall back and return false
         pt.set (p0);
         return false;
      }
   }

   /**
    * Function evaluator for a one-dimensional Newton-based root
    * solver. 
    */
   private static interface NewtonEvaluator {

      /**
       * Evaluates f(s) and f'(s) at s. The function value f(s) is returned,
       * and f'(s) can be queried afterwards by <code>getDeriv()</code>.
       */
      public double eval (double s);

      /**
       * Returns the f(s) value computed by the most recent call to
       * <code>eval(s)</code>.
       */
      public double getValue();

      /**
       * Returns the f'(s) value computed by the most recent call to
       * <code>eval(s)</code>.
       */
      public double getDeriv();

      /**
       * Returns the number of iterations that were used to find the root.
       */
      public int getIterationCnt();

      /**
       * Sets the number of iterations that were used to find the root.
       */
      public void setIterationCnt (int cnt);
   }

   private static abstract class NewtonEvaluatorBase implements NewtonEvaluator {
      protected double value;
      protected double deriv;
      protected int icnt;

      public double getValue() {
         return value;
      }

      public double getDeriv() {
         return deriv;
      }

      public int getIterationCnt() {
         return icnt;
      }

      public void setIterationCnt(int cnt) {
         icnt = cnt;
      }
   }

   static boolean debug = false;

   /**
    * Uses a robust Newton method to solve for the root of a function on the
    * interval <code>[slo,shi]</code>, within a tolerance <code>stol</code>.
    * It is assumed that the root is already bracketed.  The function and its
    * derivative are evaluated by <code>func</code>, and the iteration starts
    * at the value <code>s0</code>. When Newton iteration fails to make
    * progress toward the root, the method falls back on bisection.
    */
   private static double findRoot (
      NewtonEvaluator func, double slo, double shi, double s0, double stol) {

      double flo = func.eval (slo);
      double fhi = func.eval (shi);

      int maxIter = 1074; //Eberly mentions this is max required for bisection

      if (flo == 0) {
         return slo;
      }
      else if (fhi == 0) {
         return shi;
      }
      else if (flo*fhi < 0) {
         double snew = s0;
         int i;
         for (i=0; i<maxIter; i++) {
            double fnew = func.eval (snew);
            double dnew = func.getDeriv();

            double ftol = Math.max (dnew, DOUBLE_PREC)*stol;
            if (Math.abs(fnew) < ftol) {
               //System.out.println ("fnew=" + fnew);
               //System.out.println ("snew=" + snew);
               break;
            }
            if((flo<0 && fnew>0) || (flo>0 && fnew<0)){
               // if sign change between lo and new
               shi=snew;
               fhi=fnew;
            }
            else { // otherwise sign change between hi and new
               slo=snew;
               flo=fnew;
            }
            if (slo > shi) {
               throw new InternalErrorException (
                  "interval exchanged, slo=" + slo + " shi=" + shi);
            }
            
            double sx = 0;
            if (dnew != 0) {
               sx = snew - fnew/dnew;
            }
            // if Newton's method answer not within the interval,
            // bisect instead
            if (sx <= slo || sx >= shi) {
               snew = (slo+shi)/2;
               if (snew == slo || snew == shi) {
                  // further progress not possible
                  break;
               }
            }
            else {
               snew = sx;
            }
            if (shi-slo <= stol) {
               break;                     
            }  
         }
         func.setIterationCnt (i);
         return snew;
      }
      else {
         throw new IllegalArgumentException (
            "Root not bracketed: f("+slo+")="+flo+", f("+shi+")=" + fhi);
      }
   }

   // iteration count stats for ellipse distance calculation
   static int ellipseIterationTotal = 0;
   static int ellipseCallTotal = 0;

   /**
    * Evaluates the special quartic
    * <pre>
    * f(t) = a4 t^4 + a3 t^3 + a1 t - a4
    * </pre>
    * whose roots indicate the nearest point to q on an ellipse.
    */
   private static class EllipseEvaluator extends NewtonEvaluatorBase {

      double a4;
      double a3;
      double a1;

      EllipseEvaluator (double a, double b, Vector2d q) {
         a4 = b*q.y;
         a3 = 2*(a*a - b*b + a*q.x);
         a1 = 2*(b*b - a*a + a*q.x);
      }

      public double eval (double s) {
         value = ((a4*s+a3)*s*s+a1)*s - a4;
         deriv = (4*a4*s+3*a3)*s*s+a1;
         return value;
      }

   }

   /**
    * Find the point <code>p</code> on an ellipse closest to a query point
    * <code>qp</code>, and return the corresponding distance. The distance is
    * positive if <code>qp</code> is outside the ellipse, and negative if it is
    * inside.
    *
    * Modified from "Distance from a Point to an Ellipse, an Ellipsoidm or a
    * Hyperellipsoid", by David Eberly.
    */
   public static double nearestPointEllipse (
      Vector2d p, double a, double b, Vector2d qp) {

      Vector2d q = new Vector2d (qp);

      if (a == b) {
         // circular case
         double mag = q.norm();
         if (mag == 0) {
            // at the center and all points are equidistant; pick (a,0)
            p.set (a, 0);
         }
         else {
            p.scale (a/mag, q);
         }
         return -a;         
      }

      boolean swapped = false;
      boolean negatex = false;
      boolean negatey = false;
      if (a < b) {
         // swap x and y so that a > b
         double tmp;
         tmp = a; a = b; b = tmp;
         tmp = q.x; q.x = q.y; q.y = tmp;         
         swapped = true;
      }
      double dtol = a*1e2*DOUBLE_PREC; // distance tolerance

      // Reflect axes as needed so that q is in the first quadrant.  The
      // corresponding p will be in the first quadrant as well.
      if (q.x < -dtol) {
         q.x = -q.x;
         negatex = true;
      }
      if (q.y < -dtol) {
         q.y = -q.y;
         negatey = true;
      }
      double dist = 0;

      if (q.y > dtol) {
         if (q.x > dtol) {
            double g = sqr(q.x/a) + sqr(q.y/b) - 1;
            if (Math.abs(g) > EPS) {
               // Instead of using Eberly's function, we solve for f(t) == 0 on
               // the interval [0,1], where t is the tangent half-angle of the
               // ellipse parameterization angle and f(t) is a quartic with a
               // single root corresponding to the point p where (p-q) is
               // parallel to the ellipse normal.

               EllipseEvaluator func = new EllipseEvaluator (a, b, q);
               // make a rough guess for the initial half-angle:
               double t0 = q.y/(Math.sqrt(q.x*q.x + q.y*q.y)+q.x);
               double t = findRoot (func, 0, 1, t0, DOUBLE_PREC);
               // recover the ellipse point from t:
               double sin = 2*t/(1+t*t);
               double cos = (1-t*t)/(1+t*t);
               p.set (a*cos, b*sin);
               // to compute distance, take dot product of (q-p) with normal
               double nx = b*cos;
               double ny = a*sin;
               dist = ((nx*(q.x-p.x) + ny*(q.y-p.y))/
                       Math.sqrt (a*a*sin*sin + b*b*cos*cos));

               // record iteration stats
               ellipseIterationTotal += func.getIterationCnt();
               ellipseCallTotal++;
            }
            else {
               // we are on the ellipse, within precision, so p = q
               p.set (q);
               dist = 0;
            }
         }
         else {
            // on the y axis, within precision
            p.set (0.0, b);
            dist = q.y - b;
         }
      }
      else {
         // on the x axis, within  precision
         double num = a*q.x;
         double dem = a*a - b*b;
         boolean computed = false;
         if (num < dem) {
            double disc = 1-sqr(num/dem);
            if (disc > EPS) {
               // far enough in the interior that the nearest point is not at
               // (a,0) but is instead on the ellipse. This situation does not
               // occur for circles, where a == b.
               p.set (a*num/dem, b*Math.sqrt(disc));
               dist = -Math.sqrt ((p.x-q.x)*(p.x-q.x) + p.y*p.y);
               computed = true;
            }
         }
         if (!computed) {
            p.set (a, 0.0);
            dist = q.x - a;
         }
      }

      // undo axis swapping and reflection
      if (negatex) {
         p.x = -p.x;
      }
      if (negatey) {
         p.y = -p.y;
      }
      if (swapped) {
         double tmp = p.x; p.x = p.y; p.y = tmp;         
      }
      return dist;
   }

   /**
    * Describes the axis reordering needed to ensure a > b > c.
    */
   private static enum AxisOrdering {
      A_B_C,
      A_C_B,
      C_A_B,
      B_A_C,
      B_C_A,
      C_B_A
   }

   private static final double sqr (double x) {
      return x*x;
   }

   // iteration count stats for ellipsoid distance calculation
   static int ellipsoidIterationTotal = 0;
   static int ellipsoidCallTotal = 0;

   /**
    * Evaluates Eberly's function
    * <pre>
    * f(s) = (n0/(s+r0))^2 + (n1/(s+r1))^2 + (z2/(s+1))^2 - 1
    * </pre>
    * which is zero at the ellipsoid point p where (p-q) is parallel
    * to the ellipsoid normal.
    */
   private static class EberlyEvaluator extends NewtonEvaluatorBase {
      double r0;
      double r1;
      double z2;
      double n0;
      double n1;

      public EberlyEvaluator (double a, double b, double c, Vector3d q) {
         r0 = sqr(a/c);
         r1 = sqr(b/c);
         n0 = r0*(q.x/a);
         n1 = r1*(q.y/b);
         z2 = q.z/c;
      }

      public EberlyEvaluator (
         double r0, double r1, double z2, double n0, double n1) {

         this.r0 = r0;
         this.r1 = r1;
         this.z2 = z2;
         this.n0 = n0;
         this.n1 = n1;
      }

      public double eval (double s) {

         double rat0Sqr = sqr(n0/(s+r0));
         double rat1Sqr = sqr(n1/(s+r1));
         double rat2Sqr = sqr(z2/(s+1));

         value = rat0Sqr + rat1Sqr + rat2Sqr - 1;
         deriv = -2*(rat0Sqr/(s+r0) + rat1Sqr/(s+r1) + rat2Sqr/(s+1));
         return value;
      }

      /**
       * Computes an appropriate upper bound for s given the current algebraic
       * value of the ellipse function g for the query point.
       */
      public double getShi (double g) {
         if (g < 0) {
            // q is inside the ellipse
            return 0;
         }
         else {
            // q is outside the ellipse
            return Math.sqrt (sqr(n0) + sqr(n1) + sqr(z2)) - 1;
         }
      }

      /**
       * Computes the ellipsoid point p corresponding to s
       */
      public void getPosition (Vector3d p, double s, Vector3d q) {
         p.set (r0*q.x/(s+r0), r1*q.y/(s+r1), q.z/(s+1));
      }
   }

   /**
    * Find the point <code>p</code> on an ellipsoid closest to
    * <code>qp</code>, and return the corresponding distance. The distance is
    * positive if <code>qp</code> is outside the ellipsoid, and negative if it
    * is inside.
    *
    * Modified from "Distance from a Point to an Ellipse, an Ellipsoidm or a
    * Hyperellipsoid", by David Eberly.
    */
   public static double nearestPointEllipsoid (
      Vector3d p, double a, double b, double c, Vector3d qp) {

      Vector3d q = new Vector3d (qp);
      AxisOrdering reordering = AxisOrdering.A_B_C;

      // reorder as needed so that a >= b >= c
      if (a >= b) {
         if (b >= c) {
            // nothing to do
         }
         else if (a >= c) { // order as a, c, b
            double tmp = b; b = c; c = tmp;
            tmp = q.y; q.y = q.z; q.z = tmp;
            reordering = AxisOrdering.A_C_B;
         }
         else { // order as c, a, b
            double tmp = a; a = c; c = b; b = tmp;
            tmp = q.x; q.x = q.z; q.z = q.y; q.y = tmp;
            reordering = AxisOrdering.C_A_B;
         }
      }
      else {
         if (a >= c) { // order as b, a, c
            double tmp = a; a = b; b = tmp;
            tmp = q.x; q.x = q.y; q.y = tmp;
            reordering = AxisOrdering.B_A_C;
         }
         else if (b >= c) { // order as b, c, a
            double tmp = a; a = b; b = c; c = tmp;
            tmp = q.x; q.x = q.y; q.y = q.z; q.z = tmp;
            reordering = AxisOrdering.B_C_A;
         }
         else { // order as c, b, a
            double tmp = a; a = c; c = tmp;
            tmp = q.x; q.x = q.z; q.z = tmp;
            reordering = AxisOrdering.C_B_A;
         }
      }            

      double dtol = a*1e2*DOUBLE_PREC; // distance tolerance

      double dist = 0;

      if (a == b) {
         if (b == c) {
            // spherical case
            double r = q.norm();
            if (r == 0) {
               // at the center and all points are equidistant; pick (a,0,0)
               p.set (a, 0, 0);
               dist = -a;         
            }
            else {
               p.scale (a/r, q);
               dist = r-a;
            }
         }
         else {
            // oblate spheroid
            Vector2d rvec = new Vector2d(q.x, q.y);
            double r = rvec.norm();
            if (r == 0) {
               // on the z-axis
               p.set (0, 0, q.z >= 0 ? c : -c);
               dist = q.z - c;
            }
            else {
               // on an ellipse in the r/z plane
               rvec.scale (1/r);
               Vector2d pr = new Vector2d();
               dist = nearestPointEllipse (pr, a, c, new Vector2d(r, q.z));
               p.set (pr.x*rvec.x, pr.x*rvec.y, pr.y);
            }
         }
      }
      else if (b == c) {
         // prolate spheroid
         Vector2d rvec = new Vector2d(q.y, q.z);
         double r = rvec.norm();
         if (r == 0) {
            // on the x-axis
            p.set (q.x >= 0 ? a : -a, 0, 0);
            dist = q.x - a;
         }
         else {
            // on an ellipse in the x/r plane
            rvec.scale (1/r);
            Vector2d pr = new Vector2d();
            dist = nearestPointEllipse (pr, a, b, new Vector2d(q.x, r));
            p.set (pr.x, pr.y*rvec.x, pr.y*rvec.y);
         }
      }
      else {
         // triaxial case

         boolean negatex = false;
         boolean negatey = false;
         boolean negatez = false;

         // reflect axes as needed to get into the first octant
         if (q.x < -dtol) {
            q.x = -q.x;
            negatex = true;
         }
         if (q.y < -dtol) {
            q.y = -q.y;
            negatey = true;
         }
         if (q.z < -dtol) {
            q.z = -q.z;
            negatez = true;
         }

         if (q.z > dtol) {
            if (q.y > dtol) {
               if (q.x > dtol) {
                  double g = sqr(q.x/a) + sqr(q.y/b) + sqr(q.z/c) - 1;
                  if (g != 0) {
                     // q completely within first octant
                     EberlyEvaluator func = 
                        new EberlyEvaluator (a, b, c, q);

                     double slo = q.z/c-1.0;
                     double shi = func.getShi(g);
                     double s0 = (slo+shi)/2.0;
                     double s =
                        findRoot (func, slo, shi, s0, a*DOUBLE_PREC/(c*c));
                     // get p and distance from s
                     func.getPosition (p, s, q);
                     dist = Math.sqrt(
                        sqr(p.x-q.x)+sqr(p.y-q.y)+sqr(p.z-q.z));
                     if (g < 0) {
                        // negate distance if inside the ellipsoid
                        dist = -dist;
                     }
                     // record iteration stats
                     ellipsoidIterationTotal += func.getIterationCnt();
                     ellipsoidCallTotal++;
                  }
                  else {
                     p.set (q);
                     dist = 0;
                  }
               }
               else {
                  // in the y/z plane, within precision
                  Vector2d pr = new Vector2d();
                  dist = nearestPointEllipse (pr, b, c, new Vector2d(q.y, q.z));
                  p.set (0.0, pr.x, pr.y);
               }
            }
            else { 
               // in the x/z plane, within precision
               if (q.x > dtol) {
                  Vector2d pr = new Vector2d();
                  dist = nearestPointEllipse (pr, a, c, new Vector2d(q.x, q.z));
                  p.set (pr.x, 0.0, pr.y);
               }
               else {
                  // on the z axis, within precision
                  p.set (0, 0, c);
                  dist = q.z - c;
               }
            }
         }
         else { 
            // in the x/y plane, within precision. If q is outside a
            // sub-ellipse with minor axis lengths as and bs, then p is on the
            // ellipse in the x/y plane. Otherwise, p lies on the ellpsoid
            // outside the x/y plane.
            double as = (a*a-c*c)/a;
            double bs = (b*b-c*c)/b;
            boolean computed = false;
            if (q.x < as && q.y < bs) {
               double ratx = q.x/as;
               double raty = q.y/bs;
               double disc = 1 - ratx*ratx - raty*raty;
               if (disc > EPS) {
                  p.set (a*ratx, b*raty, c*Math.sqrt(disc));
                  dist = -Math.sqrt (sqr(p.x-q.x) + sqr(p.y-q.y) + p.z*p.z);
                  computed = true;
               }
            }
            if (!computed) {
               // p is on ellipse in the x/y plane
               Vector2d pr = new Vector2d();
               dist = nearestPointEllipse (pr, a, b, new Vector2d(q.x, q.y));
               p.set (pr.x, pr.y, 0.0);
            }
         }

         // undo axis negation

         if (negatex) {
            p.x = -p.x;
         }
         if (negatey) {
            p.y = -p.y;
         }
         if (negatez) {
            p.z = -p.z;
         }
      }

      // undo reordering

      switch (reordering) {
         case A_C_B: {
            double tmp = p.y; p.y = p.z; p.z = tmp;
            break;
         }
         case C_A_B: {
            double tmp = p.x; p.x = p.y; p.y = p.z; p.z = tmp;
            break;
         }
         case B_A_C: {
            double tmp = p.x; p.x = p.y; p.y = tmp;
            break;
         }
         case B_C_A: {
            double tmp = p.x; p.x = p.z; p.z = p.y; p.y = tmp;
            break;
         }
         case C_B_A: {
            double tmp = p.x; p.x = p.z; p.z = tmp;
            break;
         }
      }            

      return dist;

   }

}
