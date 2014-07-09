/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.render.ConvexPoly2d.Vertex2d;
import maspack.matrix.*;
import maspack.util.*;

public class ConvexPoly2dTest extends UnitTest {

   static private double EPS = 1e-14;
   static private double CHK_EPS = 1e-11;

   public ConvexPoly2dTest() {
      RandomGenerator.setSeed (0x1234);
   }

   void checkResult (ConvexPoly2d poly, ConvexPoly2d check) {
      if (!check.checkConsistency()) {
         throw new TestException ("check polygon is inconsistent");
      }
      if (!poly.checkConsistency()) {
         throw new TestException ("polygon is inconsistent");
      }
      if (poly.numVertices() == 0 || check.numVertices() == 1) {
         // this is OK, since 1 vertex is an unstable result
         return;
      }
      if (!poly.epsilonEquals(check, CHK_EPS)) {
         throw new TestException (
            "polygon does not equal check\nExpected:\n" +
            check.toString ("%9.5f") + "\nGot:\n" + poly.toString ("%9.5f"));
      }
   }         

   void testCentroid (
      ConvexPoly2d poly, Point2d check) {
      
      Point2d cent = new Point2d();
      poly.computeCentroid (cent);
      if (!cent.epsilonEquals (check, CHK_EPS)) {
         throw new TestException (
            "computed centroid:\n" + cent + "\nExpected:\n" + check);
      }
   }


   void testIntersect (
      ConvexPoly2d poly, ConvexPoly2d check, 
      double nx, double ny, double d) {

      ConvexPoly2d test = new ConvexPoly2d(poly);
      test.intersectHalfPlane (nx, ny, d);
      checkResult (test, check);

      // now transform the system and do the same checks. This
      // is to test for robustness
      int ntrials = 10;
      for (int i=0; i<ntrials; i++) {
         AffineTransform2d T1 = new AffineTransform2d();
         AffineTransform2d T2 = new AffineTransform2d();
         T1.setRandom();
         T2.invert (T1);
         T2.invert ();

         ConvexPoly2d testT = new ConvexPoly2d(poly);
         ConvexPoly2d checkT = new ConvexPoly2d(check);

         // transform the half plane using T2, which should be a 
         // slightly perturbed version of T1
         Vector2d nT = new Vector2d(nx, ny);
         T2.A.mulInverseTranspose (nT, nT);
         double dT = d + nT.dot (T2.p);

         testT.transform (T1);
         checkT.transform (T1);
         testT.intersectHalfPlane (nT.x, nT.y, dT);
         checkResult (testT, checkT);
      }
   }

   public void testIntersectHalfPlane() {
      ConvexPoly2d poly;
      ConvexPoly2d check;

      poly = new ConvexPoly2d (0.0, 0.0,  1.0, 0.0,  0.0, 1.0);
      check = new ConvexPoly2d (0.0, 0.0,  1.0, 0.0,  0.0, 1.0);

      // clip with a bunch of half-planes that don't intersect the poly
      testIntersect (poly, check, -1, 0, -1-EPS);
      testIntersect (poly, check, -1, 0, -2);
      testIntersect (poly, check, 0, -1, -1-EPS);
      testIntersect (poly, check, 0, -1, -2);
      testIntersect (poly, check, 1, 1, -EPS);
      testIntersect (poly, check, 1, 1, -1);

      ConvexPoly2d triangle =
         new ConvexPoly2d (0.0, 0.0,  1.0, 0.0,  0.0, 1.0);
      ConvexPoly2d square =
         new ConvexPoly2d (0.0, 0.0,  1.0, 0.0,  1.0, 1.0,  0.0, 1.0);
      poly = new ConvexPoly2d (triangle);
      checkResult (poly, triangle);

      // clip with a bunch of half-planes that anihilate the poly
      check = new ConvexPoly2d();
      poly = new ConvexPoly2d (triangle);
      testIntersect (poly, check, 1, 0, 1+EPS);
      poly = new ConvexPoly2d (triangle);
      testIntersect (poly, check, 1, 0, 2);
      poly = new ConvexPoly2d (triangle);
      testIntersect (poly, check, 0, 1, 1+EPS);
      poly = new ConvexPoly2d (triangle);
      testIntersect (poly, check, 0, 1, 2);
      poly = new ConvexPoly2d (triangle);
      testIntersect (poly, check, -1, -1, EPS);
      poly = new ConvexPoly2d (triangle);
      testIntersect (poly, check, -1, -1, 1);

      // clip off one vertex
      poly = new ConvexPoly2d (triangle);
      check =
         new ConvexPoly2d (0.0, 0.0,  0.5, 0.0,  0.5, 0.5,  0.0, 1.0);
      testIntersect (poly, check, -1, 0, -0.5);
      poly = new ConvexPoly2d (triangle);
      check =
         new ConvexPoly2d (0.0, 0.0,  1.0, 0.0,  0.5, 0.5,  0.0, 0.5);
      testIntersect (poly, check, 0, -1, -0.5);
      poly = new ConvexPoly2d (triangle);
      check =
         new ConvexPoly2d (0.5, 0.0,  1.0, 0.0,  0.0, 1.0,  0.0, 0.5);
      testIntersect (poly, check, 1, 1, 0.5);

      // clip off two vertices
      poly = new ConvexPoly2d (triangle);
      check =
         new ConvexPoly2d (0.5, 0.0,  1.0, 0.0,  0.5, 0.5);
      testIntersect (poly, check, 1, 0, 0.5);
      poly = new ConvexPoly2d (triangle);
      check =
         new ConvexPoly2d (0.0, 0.5,  0.5, 0.5,  0.0, 1.0);
      testIntersect (poly, check, 0, 1, 0.5);
      poly = new ConvexPoly2d (triangle);
      check =
         new ConvexPoly2d (0.0, 0.0,  0.5, 0.0,  0.0, 0.5);
      testIntersect (poly, check, -1, -1, -0.5);

      // clip off three vertices
      poly = new ConvexPoly2d (square);
      check =
         new ConvexPoly2d (0.0, 0.0,  0.5, 0.0,  0.0, 0.5);
      testIntersect (poly, check, -1, -1, -0.5);
      poly = new ConvexPoly2d (square);
      check =
         new ConvexPoly2d (1.0, 0.5,  1.0, 1.0,  0.5, 1.0);
      testIntersect (poly, check, 1, 1, 1.5);

      // add vertex
      poly = new ConvexPoly2d (square);
      check =
         new ConvexPoly2d (
            0.5, 0.0,  1.0, 0.0,  1.0, 1.0,  0.0, 1.0,  0.0, 0.5);
      testIntersect (poly, check, 1, 1, 0.5);
      poly = new ConvexPoly2d (square);
      check =
         new ConvexPoly2d (
            0.0, 0.0,  1.0, 0.0,  1.0, 0.5,  0.5, 1.0,  0.0, 1.0);
      testIntersect (poly, check, -1, -1, -1.5);
         
      // clip with a bunch of half-planes that just touch the vertices
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (1.0, 0.0);
      testIntersect (poly, check, 1, 0, 1);
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (0.0, 1.0);
      testIntersect (poly, check, 0, 1, 1);
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (0.0, 0.0);
      testIntersect (poly, check, -1, -1, 0);

      // clip with a bunch of half-planes that just touch the vertices
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (triangle);
      testIntersect (poly, check, -1, 0, -1);
      testIntersect (poly, check, 0, -1, -1);
      testIntersect (poly, check, 1, 1, 0);

      // clip with a bunch of half-planes that just touch the vertices
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (triangle);
      testIntersect (poly, check, -1, 0, -1+EPS);
      testIntersect (poly, check, 0, -1, -1+EPS);
      testIntersect (poly, check, 1, 1, EPS);

      // clip with a bunch of half-planes that just touch the edges
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (triangle);
      testIntersect (poly, check, 1, 0, 0);
      testIntersect (poly, check, 0, 1, 0);
      testIntersect (poly, check, -1, -1, -1);

      // clip with a bunch of half-planes that just touch the edges
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (triangle);
      testIntersect (poly, check, 1, 0, EPS);
      testIntersect (poly, check, 0, 1, EPS);
      testIntersect (poly, check, -1, -1, -1+EPS);

      // clip with a bunch of half-planes that just touch the edges
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (0.0, 0.0,  0.0, 1.0);
      testIntersect (poly, check, -1, 0, -EPS);
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (0.0, 0.0,  1.0, 0.0);
      testIntersect (poly, check, 0, -1, -EPS);
      poly = new ConvexPoly2d (triangle);
      check = new ConvexPoly2d (1.0, 0.0,  0.0, 1.0);
      testIntersect (poly, check, 1, 1, 1-EPS);
   }


   public void testConstruction() {
      ConvexPoly2d poly;
      ConvexPoly2d check;
      Vertex2d v1, v2, v3;

      poly = new ConvexPoly2d ();
      v1 = poly.addVertex (0, 0);
      if (!poly.checkConsistency()) {
         throw new TestException ("polygon is inconsistent");
      }
      v2 = poly.addVertex (1, 0);
      if (!poly.checkConsistency()) {
         throw new TestException ("polygon is inconsistent");
      }
      v3 = poly.addVertex (0, 1);
      if (!poly.checkConsistency()) {
         throw new TestException ("polygon is inconsistent");
      }
   }

   public void testCentroid () {

      ConvexPoly2d poly;

      poly = new ConvexPoly2d (0.0, 0.0,  1.0, 0.0,  1.0, 1.0,  0.0, 1.0);
      testCentroid (poly, new Point2d(0.5, 0.5));

      poly = new ConvexPoly2d (0.0, 0.0,  1.0, 0.0,  0.0, 1.0);
      testCentroid (poly, new Point2d(1/3.0, 1/3.0));
   }

   public void test() {
      testConstruction();
      testIntersectHalfPlane();
      testCentroid();
   }

   public static void main (String[] args) {
      ConvexPoly2dTest tester = new ConvexPoly2dTest();
      tester.runtest();
   }

}
