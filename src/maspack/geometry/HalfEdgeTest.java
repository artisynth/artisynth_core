/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;

public class HalfEdgeTest {

   private final static double EPS = 1e-12;

   public HalfEdgeTest() {
      RandomGenerator.setSeed (0x1234);
   }

   private void checkCentroidAndCovariance (
      HalfEdge he, Point3d centroidChk, Matrix3d CChk) {

      Point3d centroid = new Point3d();
      Matrix3d C = new Matrix3d();
      he.computeCentroid (centroid);
      he.computeCovariance (C);

      if (!centroid.epsilonEquals (centroidChk, EPS)) {
         throw new TestException (
            "computed centroid is "+centroid+", expected "+centroidChk);
      }
      if (!C.epsilonEquals (CChk, EPS)) {
         throw new TestException (
            "computed C is\n"+C+", expected\n"+CChk);
      }
   }

   private void testCentroidAndCovariance (
      HalfEdge he, Point3d centroidChk, Matrix3d CChk) {

      checkCentroidAndCovariance (he, centroidChk, CChk);
      
      RigidTransform3d T = new RigidTransform3d();
      T.setRandom();

      he.tail.pnt.transform (T);
      he.head.pnt.transform (T);

      Point3d centroidTrans = new Point3d (centroidChk);
      Matrix3d CTrans = new Matrix3d ();
      centroidTrans.transform (T);
      double l = he.length();

      CovarianceUtils.transformCovariance (CTrans, CChk, centroidChk, l, T);

      checkCentroidAndCovariance (he, centroidTrans, CTrans);
   }      

   public void testCentroidAndCovariance() {

      Point3d centroidChk = new Point3d();
      Matrix3d CChk = new Matrix3d();
      HalfEdge he;

      Vertex3d vtx0 = new Vertex3d (new Point3d (2, 0, 0));
      Vertex3d vtx1 = new Vertex3d (new Point3d (-2, 0, 0));

      he = new HalfEdge (vtx0, vtx1, null);

      double l = he.length();
      centroidChk.setZero();
      CChk.set (l*l*l/12.0, 0, 0, 0, 0, 0, 0, 0, 0);
      testCentroidAndCovariance (he, centroidChk, CChk);
   }

   public void test() throws TestException {
      testCentroidAndCovariance();
   }

   public static void main (String[] args) {
      HalfEdgeTest tester = new HalfEdgeTest();
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
