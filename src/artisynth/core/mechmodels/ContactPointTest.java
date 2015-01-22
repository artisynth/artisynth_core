/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Tests the methods in ContactPoint; specifically, the ones that reconstruct
 * vertex weights.
 */
public class ContactPointTest extends UnitTest {

   private static double EPS = 1e-13;

   public void test() {
      int cnt = 1000;
      for (int i=0; i<cnt; i++) {
         Point3d p1 = new Point3d();
         Point3d p2 = new Point3d();
         Point3d p3 = new Point3d();

         p1.setRandom ();
         p2.setRandom ();
         p3.setRandom ();

         switch (i%4) {
            case 1: {
               // project to y-z plane
               p1.x = p2.x = p3.x = 0;
               break;
            }
            case 2: {
               // project to x-z plane
               p1.y = p2.y = p3.y = 0;
               break;
            }
            case 3: {
               // project to x-y plane
               p1.z = p2.z = p3.z = 0;
               break;
            }
         }
         
         Vertex3d v1 = new Vertex3d(p1);
         Vertex3d v2 = new Vertex3d(p2);
         Vertex3d v3 = new Vertex3d(p3);

         testForEdge (v1, v2, 0);
         testForEdge (v1, v2, 1);
         testForEdge (v1, v2, 0.1234);
         testForEdge (v1, v2, 0.003);
         testForEdge (v1, v2, 0.9998);

         testForTriangle (v1, v2, v3, 1, 0);
         testForTriangle (v1, v2, v3, 0, 0);
         testForTriangle (v1, v2, v3, 0, 1);
         testForTriangle (v1, v2, v3, 0.456, 0.2214);
         testForTriangle (v1, v2, v3, 0.1, 0.7);               
      }
   }

   void checkResult (ContactPoint cp, Vertex3d[] vtxs, double[] wgts) {

      if (vtxs.length != cp.myVtxs.length) {
         throw new TestException (
            ""+cp.myVtxs.length+" computed vertices vs. "+vtxs.length);
      }
      for (int i=0; i<vtxs.length; i++) {
         if (cp.myVtxs[i] != vtxs[i]) {
            throw new TestException ("Unequal vertices at " + i);
         }
         if (Math.abs(cp.myWgts[i]-wgts[i]) > EPS) {
            throw new TestException (
               "Unequal weights at " + i +
               ": "+cp.myWgts[i]+", expected "+wgts[i]);
         }
      }
   }

   void testForEdge (
      Vertex3d v1, Vertex3d v2, double s1) {

      Vertex3d[] vtxs = new Vertex3d[] { v1, v2 };
      double[] wgts = new double[] { s1, 1-s1};

      Point3d p0 = new Point3d();
      for (int i=0; i<vtxs.length; i++) {
         p0.scaledAdd (wgts[i], vtxs[i].pnt);
      }
      ContactPoint cp = new ContactPoint (p0);
      cp.setForEdge (v1, v2);
      checkResult (cp, vtxs, wgts);
   }

   void testForTriangle (
      Vertex3d v1, Vertex3d v2, Vertex3d v3, double s1, double s2) {

      Vertex3d[] vtxs = new Vertex3d[] { v1, v2, v3 };
      double[] wgts = new double[] { s1, s2, 1-s1-s2};

      Point3d p0 = new Point3d();
      for (int i=0; i<vtxs.length; i++) {
         p0.scaledAdd (wgts[i], vtxs[i].pnt);
      }
      ContactPoint cp = new ContactPoint (p0);
      cp.setForTriangle (v1, v2, v3);
      checkResult (cp, vtxs, wgts);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      ContactPointTest tester = new ContactPointTest();
      tester.runtest();
   }
}
