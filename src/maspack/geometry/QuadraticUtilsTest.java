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

   private static double DOUBLE_PREC = 1e-16;


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

      // double a = 2.0;
      // double b = 4.0;
      // double c = 1.2;

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
               nrm, pnts[k], a, b, c, /*tol=*/1e-12, iterCnt);
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

   public void test() {
      RandomGenerator.setSeed (0x1234);
      //testEllipsoidPenetrationDistance (/*printInfo=*/false);
      testEllipsoidSurfaceTangent ();
   }

   public static void main (String[] args) {

      QuadraticUtilsTest tester = new QuadraticUtilsTest();
      tester.runtest();
   }

}
