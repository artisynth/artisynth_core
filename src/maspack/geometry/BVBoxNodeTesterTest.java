/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.RandomGenerator;
import maspack.util.TestException;
import maspack.util.InternalErrorException;

public class BVBoxNodeTesterTest {

   //BVBoxNodeTester myTester = new BVBoxNodeTester();

   private static final double EPS = 1e-15;

   /**
    * Represents a separating plane as a point and a normal
    */
   private class SeparatingPlane {
      Point3d myPnt;
      Vector3d myNrm;

      SeparatingPlane() {
         myPnt = new Point3d();
         myNrm = new Vector3d();
      }

      SeparatingPlane (Point3d pnt, Vector3d nrm) {
         myPnt = new Point3d(pnt);
         myNrm = new Vector3d(nrm);
      }

      SeparatingPlane (double px, double py, double pz,
                       double nx, double ny, double nz) {
         myPnt = new Point3d(px, py, pz);
         myNrm = new Vector3d(nx, ny, nz);
         myNrm.normalize();
      }

      SeparatingPlane (SeparatingPlane plane) {
         myPnt = new Point3d(plane.myPnt);
         myNrm = new Vector3d(plane.myNrm);
      }

      void transform (RigidTransform3d X) {
         myPnt.transform (X);
         myNrm.transform (X);
      }

      public String toString () {
         return ("( pnt=" + myPnt.toString("%8.3f") +
                 " nrm=" + myNrm.toString("%8.5f") + ")");
      }
   }
        
   // private RigidTransform3d getNodeToBase (BVNode node) {
   //    RigidTransform3d X = new RigidTransform3d();
   //    if (node instanceof AABBNode) {
   //       AABBNode aabbNode = (AABBNode)node;
   //       X.p.add (aabbNode.myMax, aabbNode.myMin);
   //       X.p.scale (0.5);
   //    }
   //    else if (node instanceof OBBNode) {
   //       X.set (((OBBNode)node).obb.X);
   //    }
   //    else {
   //       throw new InternalErrorException (
   //          "node must be either an AABBNode or an OBBNode");
   //    }
   //    return X;      
   // }

   // private Vector3d getHalfWidths (BVNode node) {
   //    Vector3d hw = new Vector3d();
   //    if (node instanceof AABBNode) {
   //       AABBNode aabbNode = (AABBNode)node;
   //       hw.sub (aabbNode.myMax, aabbNode.myMin);
   //       hw.scale (0.5);
   //    }
   //    else if (node instanceof OBBNode) {
   //       hw.set (((OBBNode)node).obb.halfWidths);
   //    }
   //    else {
   //       throw new InternalErrorException (
   //          "node must be either an AABBNode or an OBBNode");
   //    }
   //    return hw;
   // }

   // private Point3d[] getVertexPoints (BVNode node) {
   //    Vector3d hw = BVBoxNodeTest.getHalfWidths (node);

   //    return new Point3d[] {
   //       new Point3d ( hw.x,  hw.y,  hw.z),
   //       new Point3d ( hw.x,  hw.y, -hw.z),
   //       new Point3d ( hw.x, -hw.y,  hw.z),
   //       new Point3d ( hw.x, -hw.y, -hw.z),
   //       new Point3d (-hw.x,  hw.y,  hw.z),
   //       new Point3d (-hw.x,  hw.y, -hw.z),
   //       new Point3d (-hw.x, -hw.y,  hw.z),
   //       new Point3d (-hw.x, -hw.y, -hw.z)
   //    };
   // }

   private double getOuterPoint (
      Point3d pnt, BVNode node, SeparatingPlane plane) {

      Vector3d hw = BVBoxNodeTest.getHalfWidths (node);

      Point3d[] pnts = BVBoxNodeTest.getVertexPoints (node);
      double max = Double.NEGATIVE_INFINITY;
      int maxIdx = -1;
      Vector3d del = new Vector3d();
      for (int i=0; i<pnts.length; i++) {
         del.sub (pnts[i], plane.myPnt);
         double d = del.dot (plane.myNrm);
         if (d > max) {
            max = d;
            maxIdx = i;
         }
      }
      pnt.set (pnts[maxIdx]);
      return max;
   }

   private double getInnerPoint (
      Point3d pnt, BVNode node, SeparatingPlane plane) {

      Vector3d hw = BVBoxNodeTest.getHalfWidths (node);

      Point3d[] pnts = BVBoxNodeTest.getVertexPoints (node);
      double min = Double.POSITIVE_INFINITY;
      int minIdx = -1;
      Vector3d del = new Vector3d();
      for (int i=0; i<pnts.length; i++) {
         del.sub (pnts[i], plane.myPnt);
         double d = del.dot (plane.myNrm);
         if (d < min) {
            min = d;
            minIdx = i;
         }
      }
      pnt.set (pnts[minIdx]);
      return min;
   }

   private SeparatingPlane[] getSeparatingPlanes (BVNode node) {

      Vector3d hw = BVBoxNodeTest.getHalfWidths (node);
      return new SeparatingPlane[] {
         // for the faces:
         new SeparatingPlane ( hw.x,   0.0,   0.0,    1,  0,  0 ),
         new SeparatingPlane (-hw.x,   0.0,   0.0,   -1,  0,  0 ),
         new SeparatingPlane (  0.0,  hw.y,   0.0,    0,  1,  0 ),
         new SeparatingPlane (  0.0, -hw.y,   0.0,    0, -1,  0 ),
         new SeparatingPlane (  0.0,   0.0,  hw.z,    0,  0,  1 ),
         new SeparatingPlane (  0.0,   0.0, -hw.z,    0,  0, -1 ),
         // for the vertices:
         new SeparatingPlane ( hw.x,  hw.y,  hw.z,    1,  1,  1 ),
         new SeparatingPlane ( hw.x,  hw.y, -hw.z,    1,  1, -1 ),
         new SeparatingPlane ( hw.x, -hw.y,  hw.z,    1, -1,  1 ),
         new SeparatingPlane ( hw.x, -hw.y, -hw.z,    1, -1, -1 ),
         new SeparatingPlane (-hw.x,  hw.y,  hw.z,   -1,  1,  1 ),
         new SeparatingPlane (-hw.x,  hw.y, -hw.z,   -1,  1, -1 ),
         new SeparatingPlane (-hw.x, -hw.y,  hw.z,   -1, -1,  1 ),
         new SeparatingPlane (-hw.x, -hw.y, -hw.z,   -1, -1, -1 ),
         // for the edges:
         new SeparatingPlane ( hw.x,  hw.y,   0.0,    1,  1,  0 ),
         new SeparatingPlane ( hw.x, -hw.y,   0.0,    1, -1,  0 ),
         new SeparatingPlane (-hw.x,  hw.y,   0.0,   -1,  1,  0 ),
         new SeparatingPlane (-hw.x, -hw.y,   0.0,   -1, -1,  0 ),
         new SeparatingPlane ( hw.x,   0.0,  hw.z,    1,  0,  1 ),
         new SeparatingPlane ( hw.x,   0.0, -hw.z,    1,  0, -1 ),
         new SeparatingPlane (-hw.x,   0.0,  hw.z,   -1,  0,  1 ),
         new SeparatingPlane (-hw.x,   0.0, -hw.z,   -1,  0, -1 ),
         new SeparatingPlane (  0.0,  hw.y,  hw.z,    0,  1,  1 ),
         new SeparatingPlane (  0.0,  hw.y, -hw.z,    0,  1, -1 ),
         new SeparatingPlane (  0.0, -hw.y,  hw.z,    0, -1,  1 ),
         new SeparatingPlane (  0.0, -hw.y, -hw.z,    0, -1, -1 )
      };
   }

   private void printNodeInfo (String msg, BVNode node) {

      System.out.println (msg);
      System.out.println ("type=" + node.getClass());
      System.out.println (
         "hw=" + BVBoxNodeTest.getHalfWidths (node).toString ("%8.3f"));
      System.out.println (
         "X=\n" + BVBoxNodeTest.getNodeToBase (node).toString ("%8.3f"));
   }

   private void printTestInfo (
      BVNode node1, BVNode node2, SeparatingPlane plane, 
      RigidTransform3d XBA, RigidTransform3d XBA_MOD) {

      printNodeInfo ("node1:", node1);
      printNodeInfo ("node2:", node2);
      System.out.println ("plane: " + plane);
      System.out.println ("XBA=\n" + XBA.toString ("%8.3f"));
      System.out.println ("XBA_MOD=\n" + XBA_MOD.toString ("%8.3f"));
   }

   private void testDisjoint (
      BVNode node1, BVNode node2,
      SeparatingPlane plane, RigidTransform3d XBA, BVNodeTester tester) {

      RigidTransform3d X1A = BVBoxNodeTest.getNodeToBase (node1);
      RigidTransform3d X2B = BVBoxNodeTest.getNodeToBase (node2);
      RigidTransform3d X12 = new RigidTransform3d ();

      X12.mulInverseBoth (X2B, XBA);
      X12.mul (X1A);

      SeparatingPlane plane2 = new SeparatingPlane (plane);
      plane2.transform (X12);
      SeparatingPlane planeA = new SeparatingPlane (plane);
      planeA.transform (X1A);

      Point3d pntO = new Point3d();
      Point3d pntI = new Point3d();

      double dmax = getOuterPoint (pntO, node2, plane2);
      double dmin = getInnerPoint (pntI, node2, plane2);
      pntO.transform (X2B);
      pntO.transform (XBA);
      pntI.transform (X2B);
      pntI.transform (XBA);
      
      RigidTransform3d XBA_MOD = new RigidTransform3d (XBA);

      Vector3d del = new Vector3d();
      del.sub (planeA.myPnt, pntO);
      //XBA_MOD.p.scaledAdd (-dmax, planeA.myNrm, XBA.p);
      XBA_MOD.p.add (del, XBA.p);

      if (tester.isDisjoint (node1, node2, XBA_MOD)) {
         printTestInfo (node1, node2, plane, XBA, XBA_MOD);
         throw new TestException (
            "node1 and node2 are disjoint when they shouldn't be");
      }
      //System.out.println ("pntI=" + pntI);
      // del.sub (pntI, planeA.myPnt);
      // XBA_MOD.p.sub (XBA.p, del);
      // XBA_MOD.p.scaledAdd (-dmax+1e10, planeA.myNrm, XBA.p);
      //System.out.println ("dmin=" + dmin);
      
      del.sub (planeA.myPnt, pntI);
      del.scaledAdd (-1e-10, planeA.myNrm);
      XBA_MOD.p.add (del, XBA.p);
      if (tester.isDisjoint (node1, node2, XBA_MOD)) {
         printTestInfo (node1, node2, plane, XBA, XBA_MOD);
         throw new TestException (
            "node1 and node2 are disjoint - should be just touching");
      }
      XBA_MOD.p.scaledAdd ( 2e-10, planeA.myNrm);         
      if (!tester.isDisjoint (node1, node2, XBA_MOD)) {
         printTestInfo (node1, node2, plane, XBA, XBA_MOD);
         throw new TestException (
            "node1 and node2 are not disjoint - should be just separated");
      }
   }

   private void testNodes (BVNode node1, BVNode node2) {

      BVBoxNodeTester tester = new BVBoxNodeTester (node1, node2);
      SeparatingPlane planes[] = getSeparatingPlanes (node1);
      ArrayList<RigidTransform3d> Xlist = new ArrayList<RigidTransform3d>();

      double PI = Math.PI;

      Xlist.add (new RigidTransform3d ());
      Xlist.add (new RigidTransform3d (0, 0, 0, 1, 0, 0,  PI/4));
      Xlist.add (new RigidTransform3d (0, 0, 0, 1, 0, 0, -PI/4));
      Xlist.add (new RigidTransform3d (0, 0, 0, 0, 1, 0,  PI/4));
      Xlist.add (new RigidTransform3d (0, 0, 0, 0, 1, 0, -PI/4));
      Xlist.add (new RigidTransform3d (0, 0, 0, 0, 0, 1,  PI/4));
      Xlist.add (new RigidTransform3d (0, 0, 0, 0, 0, 1, -PI/4));
      Xlist.add (new RigidTransform3d (0, 0, 0, 1, -1, 0, PI/4));

      int numRandomX = 10;
      for (int i=0; i<numRandomX; i++) {
         RigidTransform3d X = new RigidTransform3d();
         X.R.setRandom();
         Xlist.add (X);
      }

      for (int i=0; i<planes.length; i++) {
         for (int j=0; j<Xlist.size(); j++) {
            testDisjoint (node1, node2, planes[i], Xlist.get(j), tester);
         }
      }
   }

   public void test() {
      //AABBNode aabbNode = new AABBNode (-1, 0, -2, 0, 2, 1);
      //OBBNode obbNode = new OBBNode (new OBB(3, 4, 5, null), null);

      AABB aabbNode = new AABB (-1, -1, -1, 1, 1, 1);
      OBB obbNode = new OBB (2, 2, 2);

      testNodes (aabbNode, aabbNode);
      testNodes (aabbNode, obbNode);
      testNodes (obbNode, aabbNode);
      testNodes (obbNode, obbNode);

      aabbNode = new AABB (-1, 0, -2, 0, 2, 1);
      obbNode = new OBB (3, 4, 5);

      testNodes (aabbNode, aabbNode);
      testNodes (aabbNode, obbNode);
      testNodes (obbNode, aabbNode);
      testNodes (obbNode, obbNode);

      RigidTransform3d X = new RigidTransform3d();
      X.setRandom();
      obbNode = new OBB (3, 4, 5, X);

      testNodes (aabbNode, obbNode);
      testNodes (obbNode, aabbNode);
      testNodes (obbNode, obbNode);
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      BVBoxNodeTesterTest tester = new BVBoxNodeTesterTest();
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }
      System.out.println ("\nPassed\n");
   }
}
