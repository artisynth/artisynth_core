/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.TestException;
import maspack.util.FunctionTimer;
import maspack.util.UnitTest;
import maspack.fileutil.NativeLibraryManager;

public class TetgenTessellatorTest extends UnitTest {

   TetgenTessellator myTessellator = null;
   boolean verbose = true;

   public TetgenTessellatorTest () {
   }

   private double[] plane = new double[] {
      0, 0, 0,
      1, 0, 0, 
      2, 0, 0, 
      2, 1, 0, 
      1, 1, 0, 
      0, 1, 0, 
      0, 0, 1
   };

   private double[] brickCoords = new double[] {
      0, 0, 1,
      2, 0, 1,
      2, 1, 1,
      0, 1, 1,
      0, 0, 0,
      2, 0, 0,
      2, 1, 0,
      0, 1, 0,
   };

   private double getFaceArea (Point3d[] pnts, int idx0, int idx1, int idx2) {

      Vector3d d1 = new Vector3d();
      Vector3d d2 = new Vector3d();
      Vector3d nrm = new Vector3d();
      d1.sub (pnts[idx1], pnts[idx0]);
      d2.sub (pnts[idx2], pnts[idx0]);
      nrm.cross (d1, d2);
      return nrm.norm();
   }

   private void printHullFaces () {
      Point3d[] pnts = myTessellator.getPoints();
      int[] hullFaces = myTessellator.getHullFaces();
      if (hullFaces.length == 0) {
         System.out.println ("Hull faces: NONE");
      }
      else {
         System.out.println ("Hull faces:");
         for (int i=0; i<hullFaces.length; i += 3) {
            double area = getFaceArea (
               pnts, hullFaces[i], hullFaces[i+1], hullFaces[i+2]);
            System.out.printf (
               "%d %d %d area=%g\n",
               hullFaces[i], hullFaces[i+1], hullFaces[i+2], area);
         }
      }
   }

   private void testTetOrientation (Point3d[] pnts, int[] tets, int idx) {

      Point3d p0 = pnts[tets[idx]];
      Point3d p1 = pnts[tets[idx+1]];
      Point3d p2 = pnts[tets[idx+2]];
      Point3d p3 = pnts[tets[idx+3]];

      Vector3d d10 = new Vector3d();
      Vector3d d12 = new Vector3d();
      Vector3d d13 = new Vector3d();
      Vector3d xprod = new Vector3d();

      d10.sub (p0, p1);
      d12.sub (p2, p1);
      d13.sub (p3, p1);
      xprod.cross (d12, d13);
      if (xprod.dot (d10) > 0) {
         System.out.println ("POSITIVE");
      }
      else {
         System.out.println ("NEGATIVE");
      }
   }

   private void printTets () {
      int[] tets = myTessellator.getTets();
      if (tets.length == 0) {
         System.out.println ("Tets: NONE");
      }
      else {
         System.out.println ("Tets:");
         for (int i=0; i<tets.length; i += 4) {
            System.out.printf (
               "%d %d %d %d\n", tets[i], tets[i+1], tets[i+2], tets[i+3]);
         }
      }
   }

   private void printPoints () {
      Point3d[] pnts = myTessellator.getPoints();
      if (pnts.length == 0) {
         System.out.println ("Points: NONE");
      }
      else {
         System.out.println ("Points:");
         for (int i=0; i<pnts.length; i++) {
            System.out.printf (
               "%g %g %g\n", pnts[i].x, pnts[i].y, pnts[i].z);
         }
      }
   }

   FunctionTimer timer = new FunctionTimer();
   
   protected void checkHull() {
      if (!myTessellator.checkConvexHull (System.out)) {
         throw new TestException ("Convex hull check failed");
      }
   }

   public void test() {
      myTessellator = new TetgenTessellator();
      myTessellator.buildFromPoints (plane);
      if (!mySilentP) {
         printHullFaces();
         printTets();
      }
      checkHull();

      PolygonalMesh mesh;
      mesh = MeshFactory.createQuadBox (3, 2, 1);
      myTessellator.buildFromMesh (mesh, 0);
      if (!mySilentP) {
         System.out.println ("Quad Cube");
         printHullFaces();
         printTets();
      }
      checkHull();

      mesh.triangulate();
      myTessellator.buildFromMesh (mesh, 0);
      if (!mySilentP) {
         System.out.println ("Triangle Cube");
         printHullFaces();
         printTets();
      }
      checkHull();

      mesh = MeshFactory.createQuadSphere (1.0, 8);
      myTessellator.buildFromMesh (mesh, 0);
      if (!mySilentP) {
         System.out.println ("Quad Sphere, num vertices=" + mesh.numVertices());
         printHullFaces();
         printTets();
         printPoints();
      }
      checkHull();

      myTessellator.dispose();
   }

   public void setSilent (boolean silent) {
      super.setSilent(silent);
      if (!silent) {
         NativeLibraryManager.setFlags (NativeLibraryManager.VERBOSE);
      }
      else {
         NativeLibraryManager.setFlags (0);
      }
   }

   public static void main (String[] args) {

      TetgenTessellatorTest tester = new TetgenTessellatorTest();
      tester.setSilent (true);
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-verbose")) {
            tester.setSilent (false);
         }
         else {
            System.out.println (
               "Usage: java "+tester.getClass().getName() + " [-verbose]");
            System.exit(1);
         }
      }
      tester.runtest();
   }

}
