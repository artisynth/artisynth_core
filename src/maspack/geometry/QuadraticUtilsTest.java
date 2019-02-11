/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.Random;

import maspack.util.*;
import maspack.matrix.*;

/**
 * Utility methods for quadratic shapes and surfaces.
 */
public class QuadraticUtilsTest extends UnitTest {

   private static double DOUBLE_PREC = 2e-16;
   private static double EPS = 100*DOUBLE_PREC;
   /**
    * Alternative implementation of ellipsoidPenetrationDistance() written by
    * Omar Zarifi. Uses Newton iteration. Fasters, but can fail to converge and
    * also can confuse maximums with minimums.
    */
   public static double ellipsoidPenetrationDistanceOmar (
      Vector3d nrm, Vector3d pos, Vector3d p0, double a, double b, double c,
      double tol, IntHolder numIters, boolean debug) {

      int maxi = 1000;
      if (numIters != null) {
         maxi = numIters.value;
         numIters.value = 0;
      }

      double asq = a*a;
      double bsq = b*b;
      double csq = c*c;

      double msq = p0.x * p0.x / asq + p0.y * p0.y / bsq + p0.z * p0.z / csq;
      if (msq >= 1) {
         return 0;
      }
      double m = Math.sqrt (msq);

      if (tol <= 0) {
         tol = 1e-8*(a+b+c);
      }
   
      // Current guess for Newton's method (fourth component is the Lagrange
      // multiplier).
      double guess[] = new double[]{p0.x / m, p0.y / m, p0.z / m, 0};
      // Value of the residual; note that there are only 3 components: fourth
      // residual is always zero, since we project every guess onto the surface
      // of the ellipsoid.
      Point3d res = new Point3d(
         guess[0] - p0.x, guess[1] - p0.y, guess[2] - p0.z);
   
      double alpha = 1;
      double beta = 1;
      double gamma = 1;
   
      // Loop until convergence.
      int icnt = 0;
      while(res.norm() > tol && ++icnt < maxi) {
         double X = guess[0] / asq;
          double Y = guess[1] / bsq;
         double Z = guess[2] / csq;
         
         // Find the Newton perturbations.
         double dl = (X * res.x / alpha + Y * res.y / beta + Z * res.z / gamma)
            / (X * X / alpha + Y * Y / beta + Z * Z / gamma);
         double dx = (res.x - X * dl) / alpha;
         double dy = (res.y - Y * dl) / beta;
         double dz = (res.z - Z * dl) / gamma;
         
         // Perturb the guess and project onto the surface of the ellipsoid.
         guess[0] = guess[0] - dx;
         guess[1] = guess[1] - dy;
         guess[2] = guess[2] - dz;
         guess[3] = guess[3] - dl;
         m = Math.sqrt(guess[0] * guess[0] / asq + guess[1] * guess[1] / bsq
                       + guess[2] * guess[2] / csq);
         guess[0] = guess[0] / m;
         guess[1] = guess[1] / m;
         guess[2] = guess[2] / m;
      
         alpha = 1 + guess[3] / asq;
         beta = 1 + guess[3] / bsq;
         gamma = 1 + guess[3] / csq;
      
         res.x = alpha * guess[0] - p0.x;
         res.y = beta * guess[1] - p0.y;
         res.z = gamma * guess[2] - p0.z;
         if (debug && icnt < 10) {
            System.out.println (""+icnt+" res=" + res.norm());
         }
      }

      pos.set (guess[0], guess[1], guess[2]);

      nrm.sub(pos, p0);
      double dist = -nrm.norm();
      // nrm.scale (-1/dist);
      // System.out.println ("               nrm=" + nrm.toString ("%12.7f"));


      nrm.x = 2*guess[0]/asq;
      nrm.y = 2*guess[1]/bsq;
      nrm.z = 2*guess[2]/csq;
      nrm.normalize();
      if (numIters != null) {
         numIters.value = icnt;
      }
      return dist;
   }

   private int signCross2d (Point3d p0, Point3d p1) {
      double xprod = p0.x*p1.y - p0.y*p1.x;
      return xprod >= 0 ? 1 : -1;
   }

   public void sphereSurfaceTangent (
      Point3d pr, Point3d p0, Point3d p1, double r) {

      RigidTransform3d XBW = new RigidTransform3d();
      Vector3d del10 = new Vector3d();
      Vector3d delc0 = new Vector3d();
      Vector3d xprod = new Vector3d();

      del10.sub (p1, p0);
      delc0.negate (p0);
      xprod.cross (delc0, del10);
      XBW.R.setZDirection (xprod);

      // transform into this coordinate system and so a circle tangent
      // calculation

      Point3d p0loc = new Point3d(p0);
      Point3d p1loc = new Point3d(p1);    
      p0loc.inverseTransform (XBW);
      p1loc.inverseTransform (XBW);

      Point3d t0loc = new Point3d();
      Point3d t1loc = new Point3d();

      if (QuadraticUtils.circleTangentPoints (
             t0loc, t1loc, p0loc, r) == 0) {
         // not sure what to do here ...
         pr.set (p0);
         return;
      }
      if (signCross2d (p0loc, p1loc) == signCross2d (p0loc, t1loc)) {
         // use t1 instead of t0
         t0loc.set (t1loc);
      }
      pr.transform (XBW, t0loc);
   }

   public void testEllipsoidSurfaceTangent () {
      int numTrials = 100000;

      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      Point3d pt = new Point3d();
      Point3d pcheck = new Point3d();

      double r = 1.0;

      Random rand = RandomGenerator.get();

      for (int i=0; i<numTrials; i++) {
         p0.setRandom();
         p1.setRandom();
         double dist0 = r*(rand.nextDouble()+1.1);
         double dist1 = r*(0.5*rand.nextDouble() + 0.5);
         p0.scale (dist0/p0.norm());
         p1.scale (dist1/p1.norm());

         QuadraticUtils.ellipsoidSurfaceTangent (pt, p0, p1, r, r, r);
         sphereSurfaceTangent (pcheck, p0, p1, r);
         double err = pt.distance(pcheck);
         if (err > r*1e-8) {
            System.out.println ("p0=" + p0);
            System.out.println ("p1=" + p1);
            System.out.println ("pt=" + pt);
            System.out.println ("pcheck=" + pcheck);
            throw new TestException (
               "ellipsoidSurfaceTangent: error for sphere=" + err);
         }
      }
   }

   public void testEllipsoidPenetrationDistance (boolean printInfo) {

      double a = 2.0;
      double b = 4.0;
      double c = 1.2;

      Vector3d nrm = new Vector3d();
      Vector3d pos = new Vector3d();

      IntHolder iterCnt = new IntHolder();

      int numTrials = 100000;
      Point3d[] pnts = new Point3d[numTrials];
      for (int k=0; k<numTrials; k++) {
         pos.setRandom();
         pos.x *= a;
         pos.y *= b;
         pos.z *= c;
         pnts[k] = new Point3d(pos);
      }
      FunctionTimer timer =new FunctionTimer();
      timer.start();
      int iters = 0;
      int penetrations = 0;
      int failures = 0;
      int maxNumIters = 0;
      int maxi = 1000;
      for (int k=0; k<numTrials; k++) {
         iterCnt.value = maxi;
         // double d =
         //    ellipsoidPenetrationDistanceOmar (
         //       nrm, pos, pnts[k], a, b, c, /*tol=*/1e-15, iterCnt, false);
         double d =
            QuadraticUtils.ellipsoidPenetrationDistance (
               nrm, pnts[k], a, b, c, /*tol=*/1e-12, 1.0, iterCnt);
         if (d != 0) {
            penetrations++;
            iters += iterCnt.value;
            if (iterCnt.value > maxNumIters) {
               maxNumIters = iterCnt.value;
            }
         }
         if (iterCnt.value == maxi) {
            failures++;
         }
      }
      timer.stop();
      if (printInfo) {
         System.out.println ("compute time:  " + timer.result(numTrials));
         System.out.println ("penetrations:  " + penetrations);
         System.out.println ("failures:      " + failures);
         System.out.println ("max num iters: " + maxNumIters);
         System.out.println ("average iters: " + ((double)iters)/penetrations);
      }
      else {
         if (failures > 0) {
            throw new TestException (
               "ellipsoidPenetrationDistance() failed "+failures+
               " times out of "+numTrials);
         }
      }
   }      

   private double sqr (double x) {
      return x*x;
   }

   /**
    * Test nearestPointEllipse() for an ellipse with minor axis lengths
    * <code>a</code> and <code>b</code>. This method works in reverse, by
    * starting with a point <code>pchk</code> on the ellipse defined by the
    * parametric coordinate <code>theta</code>, and then
    * finding a query point <code>q</code> for which <code>pchk</code> is the
    * nearest point. <code>q</code> is
    * determined by moving away from <code>pchk</code> along the direction of
    * the curve normal <code>nrm</code> at <code>pchk</code> by a distance
    * <code>dchk</code>, such that
    * <pre>
    *    q = pchk + dchk * nrm.
    * </pre>
    * <code>dchk</code> is determined from
    * <pre>
    *    dchk = dratio * ndist
    * </pre>
    * where <code>ndist</code> is the distance along the normal from
    * <code>pchk</code> to the boundary of its quadrant. In cases
    * where <code>pchk</code> lies on the x or y axis, 
    * and the resulting <code>q</code> is inside the ellipse
    * (i.e., dratio {@code <} 0), it may be necessary to futher restrict 
    * <code>dchk</code>.
    *
    * <p>
    * Given <code>q</code> and <code>dchk</code>, this method
    * then verifies that
    * <pre>
    *    d = nearestPointEllipse (p, a, b, q);
    * </pre>
    * returns <code>d</code> and <code>p</code> values that match
    * <code>dchk</code> and <code>pchk</code>.
    */
   public void testEllipseDist (
      double a, double b, double theta, double dratio) {

      double sin = Math.sin(Math.toRadians(theta));
      double cos = Math.cos(Math.toRadians(theta));

      // find test point and normal on ellipse
      Vector2d pchk = new Vector2d (a*cos, b*sin);
      Vector2d nrm = new Vector2d (b*cos, a*sin);
      nrm.normalize();

      double dchk;

      // compute distance from pchk to the inner boundary of its quadrant:
      double ndist = Double.MAX_VALUE;
      for (int i=0; i<2; i++) {
         if (Math.abs(nrm.get(i)) > EPS) {
            double d = Math.abs(pchk.get(i)/nrm.get(i));
            if (d < ndist) {
               ndist = d;
            }
         }
      }

      // If one component of pchk is near 0, that means pchk is on either the
      // x or y axis. Then if dratio < 0, that means the desired q
      // is inside the ellipse, and it may be necessary to restrict dchk.
      int i0 = pchk.minAbsIndex();  
      if (Math.abs(pchk.get(i0)) < EPS && dratio < 0)  {
         // Find index i1 corresponding to the other axis
         int i1 = (i0 == 0 ? 1 : 0);

         // a0 and a1 are the lengths of the semimajor axes for axes i0 and i1
         double a0 = (i0 == 0 ? a : b);
         double a1 = (i1 == 0 ? a : b);
         
         // pchk lies on the axis indexed by i1, and so q will also lie along
         // this axis at a (negative) distance from pchk nominally given by
         // a1*dratio. If a1 > a0, the nearest point to q will be pchk only if
         // dratio is clipped to (a1*a1-a0*a0)/(a1*a1)-1 or else the nearest
         // point to q will not be at pchk but will instead lie elsewhere on
         // the ellipse.
         if (a1 > a0) {
            dratio = Math.max (dratio, (a1*a1-a0*a0)/(a1*a1)-1);
         }
         dchk = a1*dratio;
      }
      else {
         dchk = dratio*ndist;
      }
      
      // compute q from pchk and dchk
      Vector2d q = new Vector2d();
      q.scaledAdd (dchk, nrm, pchk);

      // make sure p and d returned by nearestPointEllipse match pchk and dchk.
      Vector2d p = new Vector2d();
      double d = QuadraticUtils.nearestPointEllipse (p, a, b, q);
      double tol = 1e-10;
      if (!pchk.epsilonEquals (p, tol)) {
         throw new TestException (
            "near point is "+p+", expected "+pchk);
      }
      if (Math.abs(dchk-d) > tol) {
         throw new TestException (
            "distance is "+d+", expected "+dchk);
      }
   }

   public void testEllipseDist (
      double a, double b, double p0x, double p0y, double pnx, double pny) {
      
      Vector2d pn = new Vector2d();
      Vector2d p0 = new Vector2d(p0x, p0y);
      double d = QuadraticUtils.nearestPointEllipse (pn, a, b, p0);
      Vector2d diff = new Vector2d();
      diff.sub (p0, pn);
      Vector2d pchk = new Vector2d (pnx, pny);
      double dchk = diff.norm();
      if (p0.x*p0.x/(a*a) + p0.y*p0.y/(b*b) < 1) {
         dchk = -dchk;
      }
      double tol = 1e-12;
      if (!pchk.epsilonEquals (pn, tol)) {
         throw new TestException (
            "near point is "+pn+", expected "+pchk);
      }
      if (Math.abs(dchk-d) > tol) {
         throw new TestException (
            "distance is "+d+", expected "+dchk);
      }
   }

   /**
    * Test nearestPointEllipse()
    */
   public void testEllipseDistance () {
      // on y axis
      testEllipseDist (2.0, 1.0, 0.0, 0.0, 0.0, 1.0);
      testEllipseDist (2.0, 1.0, 0.0, 0.5, 0.0, 1.0);
      testEllipseDist (2.0, 1.0, 0.0, 1.0, 0.0, 1.0);
      testEllipseDist (2.0, 1.0, 0.0, 1.5, 0.0, 1.0);

      // on x axis
      testEllipseDist (2.0, 1.0, 1.0, 0.0, 4.0/3, 0.74535599249993);
      testEllipseDist (2.0, 1.0, 1.5, 0.0, 2.0, 0.0);
      testEllipseDist (2.0, 1.0, 2.0, 0.0, 2.0, 0.0);
      testEllipseDist (2.0, 1.0, 3.0, 0.0, 2.0, 0.0);

      // on ellipse itself
      testEllipseDist (2.0, 1.0, 0.5, 0.96824583655185, 0.5, 0.96824583655185);

      double[] thetaValues = new double[] {
         0, 0.01, 1, 5, 10, 45, 60, 89, 89.9999, 90 };
      double[] dratios = new double[] {
         -1.0, -0.999999, -0.9, -0.5, -0.00001, 0.0, 0.00001, 0.5, 1.0 };

      for (double theta : thetaValues) {
         for (double dratio :  dratios) {
            testEllipseDist (2.0, 1.0, theta, dratio);
         }
      }

      // test all around the ellipse
      thetaValues = new double[] {
         0, 45, 90, 135, 180, -135, -90, -45};
      dratios = new double[] {
         -0.9999, -0.5, 0.0, 0.5, 1.0 
      };

      for (double theta : thetaValues) {
         for (double dratio :  dratios) {
            testEllipseDist (2.0, 1.0, theta, dratio);
            testEllipseDist (1.0, 2.0, theta, dratio);
         }
      }

      int rtotal = QuadraticUtils.ellipseCallTotal;
      int itotal = QuadraticUtils.ellipseIterationTotal;
      if (!mySilentP) {
         System.out.println (
            "Ellipse root stats: calls="+ rtotal + 
            " avg iterations=" + (itotal/(double)rtotal));
      }
   }

   public double clipDratio (double a, double b, double dratio) {
      if (a > b && dratio < 0) {
         double mindratio = (a*a-b*b)/(a*a)-1;
         if (dratio < mindratio) {
            dratio = mindratio;
         }
      }
      return dratio;
   }         

   /**
    * Test nearestPointEllipsoid() for an ellipsoid with minor axis lengths
    * <code>a</code>, <code>b</code>, and <code>c</code>. This method works in
    * reverse, by starting with a point <code>pchk</code> on the ellipsoid
    * defined by the parametric coordinates <code>theta</code>
    * and<code>phi</code>, and then finding a query point <code>q</code> for
    * which <code>pchk</code> is the nearest point. <code>q</code> is
    * determined by moving away from <code>pchk</code> along the direction of
    * the surface normal <code>nrm</code> at <code>pchk</code> by a distance
    * <code>dchk</code>, such that
    * <pre>
    *    q = pchk + dchk * nrm.
    * </pre>
    * <code>dchk</code> is determined from
    * <pre>
    *    dchk = dratio * ndist
    * </pre>
    * where <code>ndist</code> is the distance along the normal from
    * <code>pchk</code> to the boundary of its octant. In cases
    * where <code>pchk</code> lies on the x/y, y/z or z/x planes
    * and the resulting <code>q</code> is inside the ellipsoid (i.e.,
    * dratio {@code <} 0), it may be necessary to futher
    * restrict <code>dchk</code>.
    *
    * <p>
    * Given <code>q</code> and <code>dchk</code>, this method
    * then verifies that
    * <pre>
    *    d = nearestPointEllipsoid (p, a, b, c, q);
    * </pre>
    * returns <code>d</code> and <code>p</code> values that match
    * <code>dchk</code> and <code>pchk</code>.
    */
   public void testEllipsoidDist (
      double a, double b, double c, double theta, double phi, double dratio) {

      double st = Math.sin(Math.toRadians(theta));
      double ct = Math.cos(Math.toRadians(theta));
      double sp = Math.sin(Math.toRadians(phi));
      double cp = Math.cos(Math.toRadians(phi));

      //System.out.println (
      //   "NEW TEST a=" +a+" b=" + b + " c=" + c +
      //   " theta=" + theta + " phi=" + phi + " dratio=" + dratio);
 
      // find test point and normal on ellipoid surface
      Vector3d pchk = new Vector3d (a*ct*sp, b*st*sp, c*cp);
      Vector3d nrm = new Vector3d ();
      if (sp == 0) {
         nrm.set (0, 0, cp > 0 ? 1 : -1);
      }
      else {
         nrm.set (b*c*ct*sp*sp, a*c*st*sp*sp, a*b*sp*cp);
         nrm.normalize();
      }
      
      double dchk = Double.MAX_VALUE;

      // compute distance from pchk to the inner boundary of its octant:
      double ndist = Double.MAX_VALUE;
      for (int i=0; i<3; i++) {
         if (Math.abs(nrm.get(i)) > EPS) {
            double d = Math.abs(pchk.get(i)/nrm.get(i));
            if (d < ndist) {
               ndist = d;
            }
         }
      }

      // If one component of pchk is near 0, that means pchk is in either the
      // x/y, y/z or z/x planes. Then if dratio < 0, that means the desired q
      // is inside the ellipsoid, and it may be necessary to restrict dchk.
      int i0 = pchk.minAbsIndex();  
      if (Math.abs(pchk.get(i0)) < EPS && dratio < 0)  {
         // Find indices i1 and i2 corresponding to the components of the
         // plane containing pchk.
         int i1 = 0;
         int i2 = 0;
         switch (i0) {
            case 0: i1 = 1; i2 = 2; break;
            case 1: i1 = 0; i2 = 2; break;
            case 2: i1 = 0; i2 = 1; break;
         }
         Vector3d semiAxes = new Vector3d (a, b, c);
         // a0, a1, a2 are the lengths of the semimajor axes for axes i0, i1,
         // and i2
         double a0 = semiAxes.get(i0);
         double a1 = semiAxes.get(i1);
         double a2 = semiAxes.get(i2);

         if (a1 > a0 && a2 > a0) {
            // the axes i1 and i2 of the plane containing pchk have the two
            // largest semimajor axis lengths. That means that a query point q
            // for which pchk is the nearest point must lie one or outside a
            // sub-ellipse within this plane, with semimajor axies lengths
            // <code>as</code> and <code>bs</code> (see the Eberly paper for
            // details on this). dchk must be clipped appropriately so that the
            // q lies outside this sub-ellipse.
            double as = (a1*a1-a0*a0)/a1;
            double bs = (a2*a2-a0*a0)/a2;
            double p1 = pchk.get(i1);
            double p2 = pchk.get(i2);
            // aa, bb, cc are the quadratic equation coefficients for
            // the intersection of the displacement pchk + lam*nrm with
            // the sub-ellipse
            double n1 = nrm.get(i1);
            double n2 = nrm.get(i2);

            double aa = sqr(n1/as) + sqr(n2/bs);
            double bb = 2*(p1*n1/(as*as) + p2*n2/(bs*bs));
            double cc = sqr(p1/as) + sqr(p2/bs) - 1;
            double lam = (-bb+Math.sqrt(bb*bb - 4*aa*cc))/(2*aa);
            dchk = Math.max (dratio*ndist, lam);
         }          
         else if (Math.abs(pchk.get(i1)) < EPS) {
            // pchk lies on the axis indexed by i2, and so q will also lie
            // along this axis at a (negative) distance from pchk nominally
            // given by a2*dratio. If a2 > a1, the nearest point to q will be
            // pchk only if dratio is be clipped to
            // (a2*a2-a1*a1)/(a2*a2)-1. Otherwise, the nearest point to q will
            // lie elsewhere on the ellipse in the i1/i2 plane.
            if (a2 > a1) {
               dratio = Math.max (dratio, (a2*a2-a1*a1)/(a2*a2)-1);
            }
            dchk = a2*dratio;
         }
         else if (Math.abs(pchk.get(i2)) < EPS) {
            // pchk lies on the axis indexed by i1. Clip dratio as described
            // in the previous comment, only with a2 and a1 interchanged.
            if (a1 > a2) {
               dratio = Math.max (dratio, (a1*a1-a2*a2)/(a1*a1)-1);
            }
            dchk = a1*dratio;
         }
      }
      
      if (dchk == Double.MAX_VALUE) {
         dchk = dratio*ndist;
      }

      // compute q from pchk and dchk
      Vector3d q = new Vector3d();
      q.scaledAdd (dchk, nrm, pchk);

      // make sure p and d returned by nearestPointEllipsoid match pchk and
      // dchk.
      Vector3d p = new Vector3d();
      double d = QuadraticUtils.nearestPointEllipsoid (p, a, b, c, q);
      double tol = 1e-10;
      if (!pchk.epsilonEquals (p, tol)) {
         throw new TestException (
            "near point is "+p+", expected "+pchk);
      }
      if (Math.abs(dchk-d) > tol) {
         throw new TestException (
            "distance is "+d+", expected "+dchk);
      }
   }

   public void timeEllipsoidEberly (double a, double b, double c, int numtests) {
      Vector3d p = new Vector3d();
      Vector3d q = new Vector3d();
      RandomGenerator.setSeed (0x1234);
      QuadraticUtils.ellipsoidIterationTotal = 0;
      QuadraticUtils.ellipsoidCallTotal = 0;
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<numtests; i++) {
         q.x = RandomGenerator.nextDouble (-a, a);
         q.y = RandomGenerator.nextDouble (-b, b);
         q.z = RandomGenerator.nextDouble (-c, c);
         q.scale (1.2);
         QuadraticUtils.nearestPointEllipsoid (p, a, b, c, q);
      }
      timer.stop();
      int rtotal = QuadraticUtils.ellipsoidCallTotal;
      int itotal = QuadraticUtils.ellipsoidIterationTotal;      

      System.out.println (
         "Eberly: " + timer.result(numtests) + " calls="+ rtotal + 
         " avg iterations=" + (itotal/(double)rtotal));
   }

   public void timeEllipsoidLloyd (double a, double b, double c, int numtests) {
      Vector3d nrm = new Vector3d();
      Vector3d q = new Vector3d();
      RandomGenerator.setSeed (0x1234);
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<numtests; i++) {
         q.x = RandomGenerator.nextDouble (-a, a);
         q.y = RandomGenerator.nextDouble (-b, b);
         q.z = RandomGenerator.nextDouble (-c, c);
         q.scale (1.2);
         QuadraticUtils.ellipsoidPenetrationDistance (nrm, q, a, b, c, 100.0);
      }
      timer.stop();
      System.out.println ("Lloyd: " + timer.result(numtests));
   }

   /**
    * Test nearestPointEllipsoid()
    */
   public void testEllipsoidDistance () {

      double[] thetaValues = new double[] {
         0, 0.0001, 1, 45, 89, 89.9999, 90,
         91, 135, 179, 180, 181, -135, -90, -45};
      double[] phiValues = new double[] {
         0, 0.0001, 1, 45, 89, 89.9999, 90, 91, 135, 179, 180};

      double[] dratios = new double[] {
         -0.9999, -0.9, -0.5, -0.0001, 0.0, 0.0001, 0.5, 1.0 
      };

      for (double theta : thetaValues) {
         for (double phi : phiValues) {
            for (double dratio :  dratios) {
               testEllipsoidDist (3.0, 2.0, 1.0, theta, phi, dratio);
               // test with minor axes arranged out of order:
               testEllipsoidDist (3.0, 1.0, 2.0, theta, phi, dratio);
               testEllipsoidDist (2.0, 1.0, 3.0, theta, phi, dratio);
               testEllipsoidDist (2.0, 3.0, 1.0, theta, phi, dratio);
               testEllipsoidDist (1.0, 2.0, 3.0, theta, phi, dratio);
               testEllipsoidDist (1.0, 3.0, 2.0, theta, phi, dratio);

               // special cases:

               // sphere:
               testEllipsoidDist (3.0, 3.0, 3.0, theta, phi, dratio);
               // oblate spheroid:
               testEllipsoidDist (3.0, 3.0, 1.0, theta, phi, dratio);
               testEllipsoidDist (1.0, 3.0, 3.0, theta, phi, dratio);
               testEllipsoidDist (3.0, 1.0, 3.0, theta, phi, dratio);
               // prolate spheroid:
               testEllipsoidDist (1.0, 1.0, 3.0, theta, phi, dratio);
               testEllipsoidDist (3.0, 1.0, 1.0, theta, phi, dratio);
               testEllipsoidDist (1.0, 3.0, 1.0, theta, phi, dratio);
            }
         }
      }
      int rtotal = QuadraticUtils.ellipsoidCallTotal;
      int itotal = QuadraticUtils.ellipsoidIterationTotal;
      if (!mySilentP) {
         System.out.println (
            "Ellipsoid root stats: calls="+ rtotal + 
            " avg iterations=" + (itotal/(double)rtotal));
      }
   }

   void timeEllipsoidMethods () {
      int cnt = 100000;
      timeEllipsoidEberly (3, 2, 1, cnt);
      timeEllipsoidLloyd (3, 2, 1, cnt);
   }

   public void test() {
      RandomGenerator.setSeed (0x1234);
      //testEllipsoidPenetrationDistance (/*printInfo=*/false);
      testEllipsoidSurfaceTangent ();
      testEllipseDistance ();
      testEllipsoidDistance ();
   }

   public static void main (String[] args) {

      QuadraticUtilsTest tester = new QuadraticUtilsTest();
      tester.setSilent (true);
      boolean doTiming = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-verbose")) {
            tester.setSilent (false);
         }
         else if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.out.println (
               "Usage: java "+tester.getClass().getName() +
               " [-verbose] [-timing]");
            System.exit(1);
         }
      }
      if (doTiming) {
         tester.timeEllipsoidMethods();
      }
      else {
         tester.runtest();
      }
   }

}
