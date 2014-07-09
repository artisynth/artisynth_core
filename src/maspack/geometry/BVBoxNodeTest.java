/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;

import maspack.geometry.OBB.Method;
import maspack.matrix.*;
import maspack.util.*;

public class BVBoxNodeTest {

   private static final double EPS = 1e-13;
   private static final double INF = Double.POSITIVE_INFINITY;

   static RigidTransform3d getNodeToBase (BVNode node) {
      RigidTransform3d X = new RigidTransform3d();
      if (node instanceof AABB) {
         AABB aabbNode = (AABB)node;
         X.p.add (aabbNode.myMax, aabbNode.myMin);
         X.p.scale (0.5);
      }
      else if (node instanceof OBB) {
         X.set (((OBB)node).myX);
      }
      else {
         throw new InternalErrorException (
            "node must be either an AABBNode or an OBBNode");
      }
      return X;      
   }

   static Vector3d getHalfWidths (BVNode node) {
      Vector3d hw = new Vector3d();
      if (node instanceof AABB) {
         AABB aabbNode = (AABB)node;
         hw.sub (aabbNode.myMax, aabbNode.myMin);
         hw.scale (0.5);
      }
      else if (node instanceof OBB) {
         hw.set (((OBB)node).myHalfWidths);
      }
      else {
         throw new InternalErrorException (
            "node must be either an AABBNode or an OBBNode");
      }
      return hw;
   }

   static Point3d[] getVertexPoints (BVNode node) {
      Vector3d hw = BVBoxNodeTest.getHalfWidths (node);

      return new Point3d[] {
         new Point3d ( hw.x,  hw.y,  hw.z),
         new Point3d ( hw.x,  hw.y, -hw.z),
         new Point3d ( hw.x, -hw.y,  hw.z),
         new Point3d ( hw.x, -hw.y, -hw.z),
         new Point3d (-hw.x,  hw.y,  hw.z),
         new Point3d (-hw.x,  hw.y, -hw.z),
         new Point3d (-hw.x, -hw.y,  hw.z),
         new Point3d (-hw.x, -hw.y, -hw.z)
      };
   }

   private double checkDistanceToPoint (BVNode node, Point3d pnt) {

      Point3d pntx = new Point3d();
      RigidTransform3d X = getNodeToBase (node);
      Vector3d hw = getHalfWidths (node);

      pntx.inverseTransform (X, pnt);

      double x, y, z;
      if ((x = pntx.x-hw.x) <= 0) {
         if ((x = -pntx.x-hw.x) <= 0) {
            x = 0;
         }
      }
      if ((y = pntx.y-hw.y) <= 0) {
         if ((y = -pntx.y-hw.y) <= 0) {
            y = 0;
         }
      }
      if ((z = pntx.z-hw.z) <= 0) {
         if ((z = -pntx.z-hw.z) <= 0) {
            z = 0;
         }
      }
      return Math.sqrt (x*x + y*y + z*z);
   }

   private boolean checkIntersectPoint (BVNode node, Point3d pnt) {

      Point3d pntx = new Point3d();
      RigidTransform3d X = getNodeToBase (node);
      Vector3d hw = getHalfWidths (node);
   
      pntx.inverseTransform (X, pnt);

      return (Math.abs(pntx.x) <= hw.x &&
              Math.abs(pntx.y) <= hw.y &&
              Math.abs(pntx.z) <= hw.z);
   }

   private boolean checkIntersectSphere (BVNode node, Point3d pnt, double r) {

      Point3d pntx = new Point3d();
      RigidTransform3d X = getNodeToBase (node);
      Vector3d hw = getHalfWidths (node);
   
      pntx.inverseTransform (X, pnt);

      return (pntx.x+r >= -hw.x && pntx.x-r <= hw.x &&
              pntx.y+r >= -hw.y && pntx.y-r <= hw.y &&
              pntx.z+r >= -hw.z && pntx.z-r <= hw.z);
   }

   private boolean checkIntersectPlane (BVNode node, Vector3d n, double d) {

      Vector3d nx = new Vector3d();
      RigidTransform3d X = getNodeToBase (node);
      Point3d[] pnts = getVertexPoints (node);
   
      nx.inverseTransform (X, n);
      d -= n.dot (X.p);

      double lastx = 1;
      for (int i=0; i<pnts.length; i++) {
         double x = pnts[i].dot (nx) - d;
         if (i > 0 && x*lastx <= 0) {
            return true;
         }
         lastx = x;
      }
      return false;
   }

   private boolean checkIntersectLine (
      double[] lam, BVNode node, Point3d pnt, Vector3d dir) {

      Point3d pntx = new Point3d();
      Vector3d dirx = new Vector3d();
      RigidTransform3d X = getNodeToBase (node);
      Point3d[] pnts = getVertexPoints (node);
      Vector3d hw = getHalfWidths (node);
   
      dirx.inverseTransform (X, dir);
      pntx.inverseTransform (X, pnt);

      double min = -INF;
      double max = INF;
      double l0, l1;

      for (int i=0; i<3; i++) {
         double d = dirx.get(i);
         if (d == 0) {
            if (+hw.get(i) - pntx.get(i) < 0 ||
                -hw.get(i) - pntx.get(i) > 0) {
               // then perpendicukar ray misses the box
               lam[0] = INF;
               lam[1] = -INF;
               return false;
            }
         }
         else {
            double invd = 1/dirx.get(i);
            if (invd >= 0) {
               l0 = (-hw.get(i)-pntx.get(i))*invd;
               l1 = ( hw.get(i)-pntx.get(i))*invd;
            }
            else {
               l0 = ( hw.get(i)-pntx.get(i))*invd;
               l1 = (-hw.get(i)-pntx.get(i))*invd;
            }
            if (l0 > min) {
               min = l0;
            }
            if (l1 < max) {
               max = l1;
            }
            if (min > max) {
               lam[0] = min;
               lam[1] = max;
               return false;
            }
         }
      }
      lam[0] = min;
      lam[1] = max;
      return true;
   }

   public void testDistances (BVNode node) {

      RigidTransform3d X = getNodeToBase (node);
      Vector3d hw = getHalfWidths (node);
      double radius = hw.norm();

      Point3d pnt = new Point3d();
      Vector3d dir = new Vector3d();
      int cnt = 10000;
      for (int i=0; i<cnt; i++) {
         double d, dchk;
         pnt.setRandom();
         pnt.scale (2*radius);
         pnt.add (X.p);

         d = node.distanceToPoint (pnt);
         dchk = checkDistanceToPoint (node, pnt);
         if (Math.abs (d-dchk) > EPS) {
            throw new TestException (
               "distance for pnt "+pnt+" is " + d + ", expected " + dchk);
         }
      }
   }

   private void printNodeInfo (BVNode node) {
      System.out.println ("Node info:");
      System.out.println ("type=" + node.getClass());
      RigidTransform3d X = getNodeToBase (node);
      Vector3d hw = getHalfWidths (node);
      System.out.println ("hw=" + hw);
      System.out.println ("X=\n" + X);
   }

   private boolean intervalsEqual (double[] lam, double[] chk, double tol) {

      if (lam[0] != lam[0]) {
         return false;
      }
      else if (lam[0] == INF) {
         if (chk[0] != INF) {
            return false;
         }
      }
      else if (lam[0] == -INF) {
         if (chk[0] != -INF) {
            return false;
         }
      }
      else {
         if (Math.abs (lam[0]-chk[0]) >= tol) {
            return false;
         }
      }
      if (lam[1] != lam[1]) {
         return false;
      }
      else if (lam[1] == INF) {
         if (chk[1] != INF) {
            return false;
         }
      }
      else if (lam[1] == -INF) {
         if (chk[1] != -INF) {
            return false;
         }
      }
      else {
         if (Math.abs (lam[1]-chk[1]) >= tol) {
            return false;
         }
      }
      return true;
   }

   private void testLineDistAndIntersect (
      BVNode node,
      double px, double py, double pz, double dx, double dy, double dz,
      boolean statusChk, double lam0, double lam1) {

      testLineDistAndIntersect (
         node, new Point3d (px, py, pz), new Vector3d (dx, dy, dz),
         statusChk, new double[] { lam0, lam1 });
   }

   private void testIntersectLine (
      BVNode node, Point3d pnt, Vector3d dir,
      double min, double max, boolean statusChk, double[] chk) {

      double[] lam = new double[2];
      boolean status = node.intersectsLine (lam, pnt, dir, min, max);
      
      if (status != statusChk) {
         System.out.println ("pnt=" + pnt + ", dir=" + dir);
         printNodeInfo (node);
         throw new TestException (
            "intersectRay returned " + status +
            ", expected " + statusChk);
      }
      if (status && !intervalsEqual (lam, chk, EPS)) {
         System.out.println ("pnt=" + pnt + ", dir=" + dir);
         printNodeInfo (node);
         throw new TestException (
            "intersectRay returned interval ["+lam[0]+","+lam[1]+
            "], expected ["+chk[0]+","+chk[1]+"]");
      }
      double d = node.distanceAlongLine (pnt, dir, min, max);
      double dcheck;
      if (status == false || lam[1] < min || lam[0] > max) {
         dcheck = INF;
      }
      else if (lam[0] > 0) {
         dcheck = lam[0];
      }
      else if (lam[1] < 0) {
         dcheck = -lam[1];
      }
      else {
         dcheck = 0;
      }
      if ((dcheck == INF && d != INF) ||
          (dcheck == -INF && d != -INF) ||
          (Math.abs (d-dcheck) > EPS)) {
         System.out.println ("pnt=" + pnt + ", dir=" + dir);
         printNodeInfo (node);
         throw new TestException (
            "distanceToRay returned "+d+", expected "+dcheck);
      }
   }

   private void testLineDistAndIntersect (
      BVNode node, Point3d pnt, Vector3d dir, boolean statusChk, double[] chk) {

      testIntersectLine (node, pnt, dir, -INF, INF, statusChk, chk);
      testIntersectLine (node, pnt, dir, chk[1]+EPS, INF, false, chk);
      testIntersectLine (node, pnt, dir, chk[0]-EPS, INF, statusChk, chk);
      double mid = (chk[0]+chk[1])/2;
      testIntersectLine (
         node, pnt, dir, mid, INF, statusChk, new double[] { mid, chk[1]} );
      testIntersectLine (
         node, pnt, dir, -INF, mid, statusChk, new double[] { chk[0], mid} );
      testIntersectLine (node, pnt, dir, -INF, chk[0]-EPS, false, chk);
      testIntersectLine (node, pnt, dir, -INF, chk[1]+EPS, statusChk, chk);

      double[] lam = new double[2];
      boolean status = node.intersectsLine (lam, pnt, dir, -INF, INF);

      double d = node.distanceAlongLine (pnt, dir, 0, INF);
      double dcheck;
      if (status == false || lam[1] < 0) {
         dcheck = INF;
      }
      else if (lam[0] <= 0) {
         dcheck = 0;
      }
      else {
         dcheck = lam[0];
      }
      if ((dcheck == INF && d != INF) ||
          (dcheck == -INF && d != -INF) ||
          (Math.abs (d-dcheck) > EPS)) {
         System.out.println ("pnt=" + pnt + ", dir=" + dir);
         printNodeInfo (node);
         throw new TestException (
            "distanceToRay returned "+d+", expected "+dcheck);
      }
   }      

   public void testSpecialCases () {

      double s = 1.25836677; // s is the box half-width

      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      nodes.add ( new AABB (-s, -s, -s, s, s, s));
      nodes.add ( new OBB (2*s, 2*s, 2*s));

      for (int i=0; i<nodes.size(); i++) {
         BVNode node = nodes.get(i);

         // axis aligned rays through the center
         testLineDistAndIntersect (node, 0, 0, 0, 1, 0, 0, true, -s, s);
         testLineDistAndIntersect (node, 0, 0, 0, 0, 1, 0, true, -s, s);
         testLineDistAndIntersect (node, 0, 0, 0, 0, 0, 1, true, -s, s);

         // axis aligned rays outside the box
         testLineDistAndIntersect (node, 0, s+EPS, 0, 1, 0, 0, false, -s, s);
         testLineDistAndIntersect (node, 0, 0, s+EPS, 1, 0, 0, false, -s, s);
         testLineDistAndIntersect (node, s+EPS, 0, 0, 0, 1, 0, false, -s, s);
         testLineDistAndIntersect (node, 0, 0, s+EPS, 0, 1, 0, false, -s, s);
         testLineDistAndIntersect (node, s+EPS, 0, 0, 0, 0, 1, false, -s, s);
         testLineDistAndIntersect (node, 0, s+EPS, 0, 0, 0, 1, false, -s, s);

         // axis aligned rays just inside the box
         testLineDistAndIntersect (node, 0, s-EPS, 0, 1, 0, 0, true, -s, s);
         testLineDistAndIntersect (node, 0, 0, s-EPS, 1, 0, 0, true, -s, s);
         testLineDistAndIntersect (node, s-EPS, 0, 0, 0, 1, 0, true, -s, s);
         testLineDistAndIntersect (node, 0, 0, s-EPS, 0, 1, 0, true, -s, s);
         testLineDistAndIntersect (node, s-EPS, 0, 0, 0, 0, 1, true, -s, s);
         testLineDistAndIntersect (node, 0, s-EPS, 0, 0, 0, 1, true, -s, s);

         // axis aligned rays on the edges, origins at extreme vertices
         testLineDistAndIntersect (node, s, s, s, 1, 0, 0, true, -2*s, 0);
         testLineDistAndIntersect (node, s, s, s, 0, 1, 0, true, -2*s, 0);
         testLineDistAndIntersect (node, s, s, s, 0, 0, 1, true, -2*s, 0);

         testLineDistAndIntersect (node, -s, -s, -s, 1, 0, 0, true, 0, 2*s);
         testLineDistAndIntersect (node, -s, -s, -s, 0, 1, 0, true, 0, 2*s);
         testLineDistAndIntersect (node, -s, -s, -s, 0, 0, 1, true, 0, 2*s);

         // axis aligned rays on the edges, origins at midpoints
         testLineDistAndIntersect (node, 0, s, s, 1, 0, 0, true, -s, s);
         testLineDistAndIntersect (node, 0, s, -s, 1, 0, 0, true, -s, s);
         testLineDistAndIntersect (node, 0, -s, s, 1, 0, 0, true, -s, s);
         testLineDistAndIntersect (node, 0, -s, -s, 1, 0, 0, true, -s, s);

         testLineDistAndIntersect (node, s, 0, s, 0, 1, 0, true, -s, s);
         testLineDistAndIntersect (node, s, 0, -s, 0, 1, 0, true, -s, s);
         testLineDistAndIntersect (node, -s, 0, s, 0, 1, 0, true, -s, s);
         testLineDistAndIntersect (node, -s, 0, -s, 0, 1, 0, true, -s, s);

         testLineDistAndIntersect (node, s, s, 0, 0, 0, 1, true, -s, s);
         testLineDistAndIntersect (node, s, -s, 0, 0, 0, 1, true, -s, s);
         testLineDistAndIntersect (node, -s, s, 0, 0, 0, 1, true, -s, s);
         testLineDistAndIntersect (node, -s, -s, 0, 0, 0, 1, true, -s, s);
      }
   }

   void checkAABB (AABB aabb, AABB aabbChk) {
      if (!aabb.myMax.epsilonEquals (aabbChk.myMax, EPS)) {
         throw new TestException (
            "checkAABB: myMax=" + aabb.myMax + ", expected "+aabbChk.myMax);
      }
      if (!aabb.myMin.epsilonEquals (aabbChk.myMin, EPS)) {
         throw new TestException (
            "checkAABB: myMin=" + aabb.myMin + ", expected "+aabbChk.myMin);
      }
   }

   void checkOBB (OBB obb, OBB obbChk) {
      if (!obb.myHalfWidths.epsilonEquals (obbChk.myHalfWidths, EPS)) {
         throw new TestException (
            "checkOBB: myHalfWidths=" + obb.myHalfWidths +
            ", expected "+obbChk.myHalfWidths);
      }
      if (!obb.myX.epsilonEquals (obbChk.myX, EPS)) {
         throw new TestException (
            "checkOBB: myX=\n" + obb.myX + ", expected\n"+obbChk.myX);
      }
   }

   void testNodeCreation () {
      // create AABB and OBB for some special meshes and see that
      // we get what we expect:

      PolygonalMesh box =
         MeshFactory.createBox (3, 2, 1);
      Face[] boxFaces = box.getFaces().toArray(new Face[0]);
      PolygonalMesh ellipsoid = 
         MeshFactory.createSphere (1, 12);
      ellipsoid.scale (1.5, 1, 0.5);
      Face[] ellipsoidFaces = ellipsoid.getFaces().toArray(new Face[0]);

      double tol = 0.11;

      AABB aabbBox = new AABB ();
      aabbBox.set (boxFaces, boxFaces.length, tol);

      AABB aabbChk = new AABB();
      aabbChk.myMin.set (-1.5-tol, -1-tol, -0.5-tol);
      aabbChk.myMax.set (+1.5+tol, +1+tol, +0.5+tol);

      checkAABB (aabbBox, aabbChk);

      AABB aabbEllipsoid = new AABB ();
      aabbEllipsoid.set (ellipsoidFaces, ellipsoidFaces.length, tol);

      checkAABB (aabbEllipsoid, aabbChk);
             
      OBB obbChk = new OBB();
      obbChk.myHalfWidths.set (+1.5+tol, +1+tol, +0.5+tol);

      Method[] methods = new Method[] {
         Method.ConvexHull, Method.Covariance, Method.Points };

      OBB obbBox = new OBB ();
      for (int i=0; i<methods.length; i++) {
         obbBox.set (boxFaces, boxFaces.length, tol, methods[i]);
         checkOBB (obbBox, obbChk);
      }

      OBB obbEllipsoid = new OBB ();
      for (int i=0; i<methods.length; i++) {
         obbEllipsoid.set (
            ellipsoidFaces, ellipsoidFaces.length, tol, methods[i]);
         checkOBB (obbEllipsoid, obbChk);
      }

      RigidTransform3d T = new RigidTransform3d();
      T.setRandom();
      box.transform (T);
      ellipsoid.transform (T);

      obbChk.myX.set (T);

      for (int i=0; i<methods.length; i++) {
         obbBox.set (boxFaces, boxFaces.length, tol, methods[i]);
         checkOBB (obbBox, obbChk);
      }

      for (int i=0; i<methods.length; i++) {
         obbEllipsoid.set (
            ellipsoidFaces, ellipsoidFaces.length, tol, methods[i]);
         checkOBB (obbEllipsoid, obbChk);
      }
   }

   public void testIntersections (BVNode node) {

      RigidTransform3d X = getNodeToBase (node);
      Vector3d hw = getHalfWidths (node);
      double radius = hw.norm();

      Random randGen = RandomGenerator.get();

      Point3d pnt = new Point3d();
      Vector3d dir = new Vector3d();
      int cnt = 10000;
      for (int i=0; i<cnt; i++) {
         boolean status, statusChk;
         pnt.setRandom();
         pnt.scale (2*radius);
         pnt.add (X.p);

         status = node.containsPoint (pnt);
         statusChk = checkIntersectPoint (node, pnt);
         if (status != statusChk) {
            System.out.println ("pnt=" + pnt);
            printNodeInfo (node);
            throw new TestException (
               "intersectPoint returned " + status +
               ", expected " + statusChk);
         }
         double r = 0.1*radius*randGen.nextDouble();
         status = node.intersectsSphere (pnt, r);
         statusChk = checkIntersectSphere (node, pnt, r);
         if (status != statusChk) {
            System.out.println ("pnt=" + pnt + ", r=" + r);
            printNodeInfo (node);
            throw new TestException (
               "intersectSphere returned " + status +
               ", expected " + statusChk);
         }

         dir.setRandom();
         dir.normalize();
         double d = 2*radius*randGen.nextDouble();
         status = node.intersectsPlane (dir, d);
         statusChk = checkIntersectPlane (node, dir, d);
         if (status != statusChk) {
            System.out.println ("dir=" + dir + ", d=" + d);
            printNodeInfo (node);
            throw new TestException (
               "intersectPlane returned " + status +
               ", expected " + statusChk);
         }

         double[] chk = new double[2];
         statusChk = checkIntersectLine (chk, node, pnt, dir);
         testLineDistAndIntersect (node, pnt, dir, statusChk, chk);
      }
   }

   public void test() {

      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      nodes.add ( new AABB (-1, -1, -1, 1, 1, 1));
      nodes.add ( new AABB (-1, -2, -0.5, 4, 6, 7));
      nodes.add ( new OBB (1, 2, 3));

      RigidTransform3d X = new RigidTransform3d();
      X.setRandom();
      nodes.add ( new OBB (1, 2, 3, X));

      for (BVNode node : nodes) {
         testDistances (node);
         testIntersections (node);
      }
      testSpecialCases();
      testNodeCreation();
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      BVBoxNodeTest tester = new BVBoxNodeTest();
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
