/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.RandomGenerator;
import maspack.util.TestException;

class PlaneTest {
   Random myRandGen;
   private double EPS = 1e-10;

   public PlaneTest() {
      RandomGenerator.setSeed (0x1234);
      myRandGen = RandomGenerator.get();
   }

   public void testIntersectPlane() {
      double numTrials = 10;
      Vector3d x = new Vector3d();

      for (int i=0; i<numTrials; i++) {
         Plane p1 = new Plane();
         Plane p2 = new Plane();
         p1.setRandom();
         p2.setRandom();

         Point3d pnt = new Point3d();
         Vector3d dir = new Vector3d();
         boolean status = p1.intersectPlane (pnt, dir, p2);
         if (status == false) {
            throw new TestException ("planes reported as parallel");
         }
         // verify that the solution lies on both planes
         for (int k=-2; k<3; k++) {
            x.scaledAdd (k, dir, pnt);
            if (p1.distance (x) > EPS) {
               throw new TestException (
                  "intersection does not lie on first plane");
            }
            if (p2.distance (x) > EPS) {
               throw new TestException (
                  "intersection does not lie on second plane");
            }
         }
         status = p1.intersectPlane (pnt, dir, p1);
         if (status != false) {
            throw new TestException ("planes not reported as parallel");
         }
      }
   }

   public void testFit() {
      Vector3d nrm = new Vector3d();
      Plane plane = new Plane();

      for (int numPnts = 3; numPnts < 20; numPnts++) {
         Point3d[] pnts = new Point3d[numPnts];

         for (int i = 0; i < 5; i++) {
            nrm.setRandom();
            nrm.normalize();
            double offset = myRandGen.nextDouble();
            plane.set (nrm, offset);

            for (int k = 0; k < numPnts; k++) {
               pnts[k] = new Point3d();
               pnts[k].setRandom();
               plane.project (pnts[k], pnts[k]);
            }
            plane.fit (pnts, numPnts);
            if (plane.normal.dot (nrm) < 0) {
               nrm.negate();
               offset = -offset;
            }
            if (!plane.normal.epsilonEquals (nrm, EPS)) {
               throw new TestException (
                  "Got normal " + plane.normal.toString() + ", expected " + nrm);
            }
            if (Math.abs (plane.offset - offset) > EPS) {
               throw new TestException (
                  "Got offset " + plane.offset + ", expected " + offset);
            }
         }
      }
   }

   public void testSet() {
      Point3d[] pnts = new Point3d[] {
         new Point3d(), new Point3d(), new Point3d()};
      for (int k=0; k<10; k++) {
         for (int i=0; i<pnts.length; i++) {
            pnts[i].setRandom();
         }
         Plane plane = new Plane (pnts[0], pnts[1], pnts[2]);
         for (int i=0; i<pnts.length; i++) {
            if (plane.distance (pnts[i]) > 1e-16) {
               throw new TestException (
                  "distance of point from associated plane is " +
                  plane.distance (pnts[i]) + "; should be nearly 0");
            }
         }
      }
   }

   public void test() {
      testFit();
      testIntersectPlane();
      testSet();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      PlaneTest tester = new PlaneTest();
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
