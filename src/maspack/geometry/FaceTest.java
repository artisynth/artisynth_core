/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.*;

public class FaceTest {
   Face face;

   private final static double EPS = 1e-12;

   // vertex points

   Point3d p0 = new Point3d (1, 0, 0);
   Point3d p1 = new Point3d (0, 1, 0);
   Point3d p2 = new Point3d (-1, 0, 0);
   Point3d p3 = new Point3d (0, -1, 0);
   Point3d p4 = new Point3d (2, 0, 0);
   Point3d p5 = new Point3d (1, 2, 0);

   Vertex3d vtx0 = new Vertex3d (p0, 0);
   Vertex3d vtx1 = new Vertex3d (p1, 1);
   Vertex3d vtx2 = new Vertex3d (p2, 2);
   Vertex3d vtx3 = new Vertex3d (p3, 3);
   Vertex3d vtx4 = new Vertex3d (p4, 4);
   Vertex3d vtx5 = new Vertex3d (p5, 5);

   double[] distChecks =
      new double[] { 3, 0, 1, 2, 0, 0, 2, 1, -1, 1.6, 0.8, 0, 0.9, 3, 0.4, 1,
                    2, 0, -0.5, 1, 0.2, -0.25, 0.75, 0, -3, 1, 0.5, -1, 0, 0,
                    0.3, -0.2, 0.1, 0.3, 0, 0, 0.5, 0.3, 1.7, 0.5, 0.3, 0, };

   public FaceTest() {
      face = Face.create (vtx4, vtx5, vtx2);
      RandomGenerator.setSeed (0x1234);
   }

   private Face createTransformedFace (Face face, RigidTransform3d T) {
      Vertex3d[] vtxs = face.getVertices();
      Vertex3d[] newVtxs = new Vertex3d[vtxs.length];
      for (int i=0; i<vtxs.length; i++) {
         newVtxs[i] = new Vertex3d (new Point3d(vtxs[i].pnt));
         newVtxs[i].pnt.transform (T);
      }
      return Face.create (newVtxs);
   }

   public void testNearestPoint () {
      Point3d pt = new Point3d();
      Point3d pn = new Point3d();
      Point3d pnCheck = new Point3d();

      Face face = Face.create (vtx4, vtx5, vtx2);

      // AffineTransform3d X = new AffineTransform3d();
      // X.setIdentity();
      // RigidTransform3d T = new RigidTransform3d();
      // T.setRandom();
      // X.set (T);

      double[] buf = distChecks;
      for (int i = 0; i < distChecks.length / 6; i++) {
         pt.set (buf[i * 6 + 0], buf[i * 6 + 1], buf[i * 6 + 2]);
         pnCheck.set (buf[i * 6 + 3], buf[i * 6 + 4], buf[i * 6 + 5]);

         face.nearestPoint (pn, pt);
         if (!pn.epsilonEquals (pnCheck, 1e-8)) {
            throw new TestException (
               "distance check for pt " + pt.toString ("%8.3f") +
               "\nexpected " + pnCheck.toString ("%8.3f") +
               "\n     got " + pn.toString ("%8.3f"));
         }
      }

      //AffineTransform3d X = new AffineTransform3d();
      //X.setIdentity();
      RigidTransform3d T = new RigidTransform3d();
      T.setRandom();
      //X.set (T);

      face = createTransformedFace (face, T);

      for (int i = 0; i < distChecks.length / 6; i++) {
         pt.set (buf[i * 6 + 0], buf[i * 6 + 1], buf[i * 6 + 2]);
         pnCheck.set (buf[i * 6 + 3], buf[i * 6 + 4], buf[i * 6 + 5]);

         pt.transform (T);
         pnCheck.transform (T);

         face.nearestPoint (pn, pt);
         if (!pn.epsilonEquals (pnCheck, 1e-8)) {
            throw new TestException (
               "distance check " + i + " for pt " + pt.toString ("%8.3f") +
               "\nexpected " + pnCheck.toString ("%8.3f") +
               "\n     got " + pn.toString ("%8.3f"));
         }
      }
   }

   private void checkCentroidAndCovariance (
      Face face, Point3d centroidChk, Matrix3d CChk, double areaChk) {

      Point3d centroid = new Point3d();
      Matrix3d C = new Matrix3d();
      face.computeCentroid (centroid);
      double area = face.computeCovariance (C);

      if (!centroid.epsilonEquals (centroidChk, EPS)) {
         throw new TestException (
            "computed centroid is "+centroid+", expected "+centroidChk);
      }
      if (!C.epsilonEquals (CChk, EPS)) {
         throw new TestException (
            "computed C is\n"+C+", expected\n"+CChk);
      }
      if (Math.abs(area-areaChk) > EPS) {
         throw new TestException (
            "computed area is "+area+", expected "+areaChk);
      }
   }

   private void testCentroidAndCovariance (
      Face face, Point3d centroidChk, Matrix3d CChk, double areaChk) {

      checkCentroidAndCovariance (face, centroidChk, CChk, areaChk);
      
      RigidTransform3d T = new RigidTransform3d();
      T.setRandom();

      face = createTransformedFace (face, T);
      Point3d centroidTrans = new Point3d (centroidChk);
      Matrix3d CTrans = new Matrix3d ();
      centroidTrans.transform (T);

      CovarianceUtils.transformCovariance (CTrans, CChk, centroidChk, areaChk, T);

      checkCentroidAndCovariance (face, centroidTrans, CTrans, areaChk);
   }      

   public void testCentroidAndCovariance() {

      Point3d centroidChk = new Point3d();
      Matrix3d CChk = new Matrix3d();
      Face face;
      double areaChk;

      face = Face.create (vtx0, vtx1, vtx2);
      centroidChk.set (0, 1/3.0, 0);
      CChk.set (1/6.0, 0, 0, 0, 1/6.0, 0, 0, 0, 0);
      areaChk = 1;
      testCentroidAndCovariance (face, centroidChk, CChk, areaChk);

      face = Face.create (vtx0, vtx1, vtx2, vtx3);
      centroidChk.set (0, 0, 0);
      CChk.set (1/3.0, 0, 0, 0, 1/3.0, 0, 0, 0, 0);
      areaChk = 2;
      testCentroidAndCovariance (face, centroidChk, CChk, areaChk);

      face = Face.create (vtx1, vtx2, vtx3, vtx4, vtx5);
      centroidChk.set (1/1.8, 1/3.0, 0);
      CChk.set (3.25, 1.375, 0, 1.375, 2.25, 0, 0, 0, 0);
      areaChk = 4.5;
      testCentroidAndCovariance (face, centroidChk, CChk, areaChk);


   }

   public void test() throws TestException {
      testNearestPoint();
      testCentroidAndCovariance();
   }

   public static void main (String[] args) {
      FaceTest tester = new FaceTest();
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
