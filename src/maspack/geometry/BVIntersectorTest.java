/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderableUtils;
import maspack.util.FunctionTimer;
import maspack.util.PathFinder;
import maspack.util.RandomGenerator;
import maspack.util.TestException;

public class BVIntersectorTest {

   PolygonalMesh myComplexMesh1;

   public static final double EPS = 1e-13;

   public BVIntersectorTest() {
      String meshFileName =
         PathFinder.expand (
            "${srcdir PolygonalMesh}/sampleData/osCoxaeRight108v.obj");
      try {
         myComplexMesh1 = new PolygonalMesh (new File (meshFileName));
         Point3d c = new Point3d();
         myComplexMesh1.computeCentroid (c);
         myComplexMesh1.transform (new RigidTransform3d (-c.x, -c.y, -c.z));
      }
      catch (Exception e) {
         System.out.println (e);
      }
      RandomGenerator.setSeed (0x1234);
   }

   /**
    * This is a wrapper class around TriangleTriangleIntersection which we set
    * up so that we can test equality, and hence perform hashing, based on
    * whether the face pairs in a TriangleTriangleIntersection are the same.
    */
   private class TriTriIntersect {

      TriTriIntersection myTi;
      
      public TriTriIntersect (TriTriIntersection ti) {
         myTi = ti;
      }

      public boolean equals (Object obj) {
         if (obj instanceof TriTriIntersect) {
            TriTriIntersect other = (TriTriIntersect)obj;
            return ((myTi.face0 == other.myTi.face0 &&
                     myTi.face1 == other.myTi.face1) ||
                    (myTi.face0 == other.myTi.face1 &&
                     myTi.face1 == other.myTi.face0));
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return (myTi.face0.hashCode() + myTi.face1.hashCode());
      }

      public String toString() {
         return "("+myTi.face0.getIndex()+"-"+myTi.face1.getIndex()+")";
      }
   }

   /**
    * This is a wrapper class around TrianglePlaneIntersection which we set up
    * so that we can test equality, and hence perform hashing, based on the
    * faces in a TrianglePlaneleIntersection are the same.
    */
   private class TriPlaneIntersect {

      TriPlaneIntersection myTp;
      
      public TriPlaneIntersect (TriPlaneIntersection tp) {
         myTp = tp;
      }

      public boolean equals (Object obj) {
         if (obj instanceof TriPlaneIntersect) {
            TriPlaneIntersect other = (TriPlaneIntersect)obj;
            return (myTp.face == other.myTp.face);
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return (myTp.face.hashCode());
      }

      public String toString() {
         return "("+myTp.face.getIndex()+")";
      }
   }

   private void getAllIntersectingLeafNodes (
      ArrayList<BVNode> nodes1, ArrayList<BVNode> nodes2,
      BVTree bvh1, BVTree bvh2) {

      BVNodeTester tester = new BVBoxNodeTester (bvh1, bvh2);

      RigidTransform3d X21 = new RigidTransform3d();
      X21.mulInverseLeft (bvh1.getBvhToWorld(), bvh2.getBvhToWorld());

      ArrayList<BVNode> leafs1 = bvh1.getLeafNodes ();
      ArrayList<BVNode> leafs2 = bvh2.getLeafNodes ();
      for (int i=0; i<leafs1.size(); i++) {
         for (int j=0; j<leafs2.size(); j++) {
            if (!tester.isDisjoint (leafs1.get(i), leafs2.get(j), X21)) {
               nodes1.add (leafs1.get(i));
               nodes2.add (leafs2.get(j));
            }
         }
      }
   }

   private void getAllIntersectingLeafNodes (
      ArrayList<BVNode> nodes, BVTree bvh, Plane plane) {

      ArrayList<BVNode> leafs = bvh.getLeafNodes ();
      for (int i=0; i<leafs.size(); i++) {
         if (leafs.get(i).intersectsPlane (plane.normal, plane.offset)) {
            nodes.add (leafs.get(i));
         }
      }
   }

   ArrayList<TriTriIntersection> intersectAllFaces (
      PolygonalMesh mesh1, PolygonalMesh mesh2) {
      
      RigidTransform3d X21 = new RigidTransform3d();
      X21.mulInverseLeft (mesh1.getMeshToWorld(), mesh2.getMeshToWorld());

      TriangleIntersector intersector = new TriangleIntersector();
      Point3d q0 = new Point3d();
      Point3d q1 = new Point3d();
      Point3d q2 = new Point3d();

      ArrayList<TriTriIntersection> intersections =
         new ArrayList<TriTriIntersection>();
      
      for (Face face1 : mesh1.getFaces()) {

         HalfEdge he;
         he = face1.firstHalfEdge();
         Point3d p0 = he.head.pnt;
         he = he.getNext();
         Point3d p1 = he.head.pnt;
         he = he.getNext();
         Point3d p2 = he.head.pnt;

         for (Face face2 : mesh2.getFaces()) {

            he = face2.firstHalfEdge();
            q0.transform (X21, he.head.pnt);
            he = he.getNext();
            q1.transform (X21, he.head.pnt);
            he = he.getNext();
            q2.transform (X21, he.head.pnt);

            Point3d[] points =
               intersector.intersectTriangleTriangle (
                  p0, p1, p2, q0, q1, q2);
            if (points != null) {
               // map intersection points to world coords
               for (int k=0; k<points.length; k++) {
                  points[k].transform (mesh1.getMeshToWorld());
               }
               intersections.add (
                  new TriTriIntersection (
                     face1, face2, points));
            }
         }
      }            
      return intersections;
   }

   ArrayList<TriPlaneIntersection> intersectAllFaces (
      PolygonalMesh mesh, Plane plane) {
      
      TriangleIntersector intersector = new TriangleIntersector();

      if (!mesh.meshToWorldIsIdentity()) {
         plane = new Plane(plane);
         plane.inverseTransform (mesh.getMeshToWorld());
      }

      ArrayList<TriPlaneIntersection> intersections =
         new ArrayList<TriPlaneIntersection>();
      
      for (Face face : mesh.getFaces()) {

         HalfEdge he;
         he = face.firstHalfEdge();
         Point3d p0 = he.head.pnt;
         he = he.getNext();
         Point3d p1 = he.head.pnt;
         he = he.getNext();
         Point3d p2 = he.head.pnt;

         ArrayList<Point3d> points =
            intersector.intersectTrianglePlane (
               p0, p1, p2, plane);
         if (points != null && points.size() > 0) {
            // map intersection points to world coords
            for (int k=0; k<points.size(); k++) {
               points.get(k).transform (mesh.getMeshToWorld());
            }
            intersections.add (
               new TriPlaneIntersection (
                  face, plane, points));
         }
      }
      return intersections;
   }

   private ArrayList<TriTriIntersection> intersectLeafNodes (
      BVTree bvh1, BVTree bvh2, RigidTransform3d X21,
      BVIntersector intersector) {
      
      ArrayList<TriTriIntersection> intersections =
         new ArrayList<TriTriIntersection>();
      ArrayList<BVNode> nodes1 = new ArrayList<BVNode>();
      ArrayList<BVNode> nodes2 = new ArrayList<BVNode>();

      getAllIntersectingLeafNodes (nodes1, nodes2, bvh1, bvh2);
      for (int i=0; i<nodes1.size(); i++) {
         intersector.intersectBoundingVolumeTriangles (
            intersections, nodes1.get(i), nodes2.get(i), X21);
      }
      for (TriTriIntersection isec : intersections) {
         for (int k=0; k<isec.points.length; k++) {
            isec.points[k].transform (bvh1.getBvhToWorld());
         }
      }
      return intersections;
   }

   ArrayList<TriPlaneIntersection> intersectLeafNodes (
      BVTree bvh, Plane plane, BVIntersector intersector) {

      if (bvh.getBvhToWorld() != RigidTransform3d.IDENTITY) {
         plane = new Plane (plane);
         plane.inverseTransform (bvh.getBvhToWorld());
      }      

      ArrayList<TriPlaneIntersection> intersections =
         new ArrayList<TriPlaneIntersection>();
      ArrayList<BVNode> nodes = new ArrayList<BVNode>();

      getAllIntersectingLeafNodes (nodes, bvh, plane);
      for (int i=0; i<nodes.size(); i++) {
         intersector.intersectBoundingVolumeTriangles (
            intersections, nodes.get(i), plane);
      }
      for (TriPlaneIntersection isec : intersections) {
         for (int k=0; k<isec.points.length; k++) {
            isec.points[k].transform (bvh.getBvhToWorld());
         }
      }
      return intersections;
   }


   private void checkTriPlaneIntersectionsEqual (
      String name1, HashSet<TriPlaneIntersect> set1, 
      String name2, HashSet<TriPlaneIntersect> set2, double tol) {

      checkSetsEqual (name1, set1, name2, set2);
      for (TriPlaneIntersect tp1 : set1) {
         for (TriPlaneIntersect tp2 : set2) {
            if (tp1.equals (tp2)) {
               // check the actual points
               if (!checkPointsEqual (tp1.myTp.points, tp2.myTp.points, tol)) {
                  System.out.println (
                     "Intersection points differ for face "+tp1);
                  throw new TestException (
                     name1 + " vs. " + name2 + ": intersection points differ");
               }
               break;
            }
         }
      }
   }

   private void checkTriTriIntersectionsEqual (
      String name1, HashSet<TriTriIntersect> set1, 
      String name2, HashSet<TriTriIntersect> set2, double tol) {

      checkSetsEqual (name1, set1, name2, set2);
      for (TriTriIntersect tf1 : set1) {
         for (TriTriIntersect tf2 : set2) {
            if (tf1.equals (tf2)) {
               // check the actual points
               if (!checkPointsEqual (
                      tf1.myTi.points,
                      tf2.myTi.points, tol)) {
                  System.out.println (
                     "Intersection points differ for face pair "+tf1);
                  throw new TestException (
                     name1 + " vs. " + name2 + ": intersection points differ");
               }
               break;
            }
         }
      }
   }

   private boolean checkPointsEqual (
      Point3d[] pnts1, Point3d[] pnts2, double tol) {
      if (pnts1.length != pnts2.length) {
         System.out.println (
            "pnts1.length=" + pnts1.length + "pnts2.length=" + pnts2.length);
         return false;
      }
      for (int i=0; i<pnts1.length; i++) {
         if (!pnts1[i].epsilonEquals (pnts2[i], tol)) {
            System.out.println ("points differ, i="+i+":");
            System.out.println (pnts1[i]);
            System.out.println (pnts2[i]);
            return false;
         }
      }
      return true;
   }

   private void checkSetsEqual (
      String name1, HashSet<? extends Object> set1, 
      String name2, HashSet<? extends Object> set2) {

      if (!set1.containsAll (set2)) {
         System.out.println (name1 + " size = " + set1.size());
         System.out.println (name2 + " size = " + set2.size());
         System.out.println ("Missing intersections:");
         for (Object ti : set2) {
            if (!set1.contains (ti)) {
               System.out.println (" " + ti);
            }
         }
         throw new TestException (name2 + " not fully contained in " + name1);
      }
      if (!set2.containsAll (set1)) {
         System.out.println (name1 + " size = " + set1.size());
         System.out.println (name2 + " size = " + set2.size());
         System.out.println ("Missing intersections:");
         for (Object ti : set1) {
            if (!set2.contains (ti)) {
               System.out.println (" " + ti);
            }
         }
         throw new TestException (name1 + " not fully contained in " + name2);
      }
   }

   HashSet<TriTriIntersect> createTriTriIntersectionSet (
      Collection<TriTriIntersection> intersections) {

      HashSet<TriTriIntersect> set = new HashSet<TriTriIntersect>();
      for (TriTriIntersection ti : intersections) {
         set.add (new TriTriIntersect (ti));
      }
      return set;
   }

   HashSet<TriPlaneIntersect> createTriPlaneIntersectionSet (
      Collection<TriPlaneIntersection> intersections) {

      HashSet<TriPlaneIntersect> set = new HashSet<TriPlaneIntersect>();
      for (TriPlaneIntersection tp : intersections) {
         set.add (new TriPlaneIntersect (tp));
      }
      return set;
   }

   public void testMeshIntersection (
      PolygonalMesh mesh1, BVTree bvh1,
      PolygonalMesh mesh2, BVTree bvh2,
      RigidTransform3d X1W, RigidTransform3d X2W) {

      RigidTransform3d X21 = new RigidTransform3d();
      X21.mulInverseLeft (X1W, X2W);

      mesh1.setMeshToWorld (X1W);
      mesh2.setMeshToWorld (X2W);
      bvh1.setBvhToWorld (X1W);
      bvh2.setBvhToWorld (X2W);

      BVIntersector intersector = new BVIntersector();
      ArrayList<TriTriIntersection> intersections =
         new ArrayList<TriTriIntersection>();
      intersector.intersectMeshMesh (intersections, bvh1, bvh2);

      ArrayList<TriTriIntersection> bruteForceIntersections =
         intersectAllFaces (mesh1, mesh2);

      ArrayList<TriTriIntersection> leafIntersections =
         intersectLeafNodes (bvh1, bvh2, X21, intersector);

      ArrayList<TriTriIntersection> obbtreeIntersections =
         new ArrayList<TriTriIntersection>();
//      mesh1.getObbtree().intersectFully (
//         mesh2.getObbtree(), obbtreeIntersections, new TriangleIntersector());

      // // convert points of obbIntersections

      // for (TriangleTriangleIntersection isect : obbtreeIntersections) {
      //    for (Point3d pnt : isect.points) {
      //       pnt.inverseTransform (X1W);
      //    }
      // }

      HashSet<TriTriIntersect> bruteForceSet =
         createTriTriIntersectionSet (bruteForceIntersections);
      HashSet<TriTriIntersect> leafSet =
         createTriTriIntersectionSet (leafIntersections);
//      HashSet<TriTriIntersect> obbtreeSet =
//         createTriTriIntersectionSet (obbtreeIntersections);
      HashSet<TriTriIntersect> intersectionSet =
         createTriTriIntersectionSet (intersections);

      double tol = (bvh1.getRadius()+bvh2.getRadius())*EPS;

      checkTriTriIntersectionsEqual (
         "intersections", intersectionSet,
         "brute force intersections", bruteForceSet, tol);
      checkTriTriIntersectionsEqual (
         "intersections", intersectionSet,
         "leaf intersections", leafSet, tol);
//      checkTriTriIntersectionsEqual (
//         "intersections", intersectionSet,
//         "obbtree intersections", obbtreeSet, tol);
   }

   public void testMeshIntersection (PolygonalMesh mesh1) {
      
      Point3d center = new Point3d();
      double radius = RenderableUtils.getRadiusAndCenter (center, mesh1);

      PolygonalMesh mesh2 = new PolygonalMesh (mesh1);

      OBBTree OBBTree1 = new OBBTree (mesh1);
      OBBTree OBBTree2 = new OBBTree (mesh2);
      AABBTree AABBTree1 = new AABBTree (mesh1);
      AABBTree AABBTree2 = new AABBTree (mesh2);

      int numtrials = 100;
      for (int i=0; i<numtrials; i++) {
         RigidTransform3d X2W = new RigidTransform3d();
         RigidTransform3d X21 = new RigidTransform3d();
         RigidTransform3d X1W = new RigidTransform3d();
         X1W.R.setRandom();
         X1W.p.setRandom();
         X1W.p.scale (radius);
         X21.R.setRandom();
         X21.p.setRandom();
         X21.p.scale (radius);
         X2W.mul (X1W, X21);

         testMeshIntersection (mesh1, OBBTree1, mesh2, OBBTree2, X1W, X2W);
         testMeshIntersection (mesh1, AABBTree1, mesh2, OBBTree2, X1W, X2W);
         testMeshIntersection (mesh1, OBBTree1, mesh2, AABBTree2, X1W, X2W);
         testMeshIntersection (mesh1, AABBTree1, mesh2, AABBTree2, X1W, X2W);
         
         
      }
   }

   public void timeMeshIntersection (PolygonalMesh mesh1) {
      
      Point3d center = new Point3d();
      double radius = RenderableUtils.getRadiusAndCenter (center, mesh1);

      PolygonalMesh mesh2 = new PolygonalMesh (mesh1);
      BVIntersector intersector = new BVIntersector();
      TriangleIntersector ti = new TriangleIntersector();

      ArrayList<TriTriIntersection> intersections =
         new ArrayList<TriTriIntersection>();

      OBBTree obbTree1 = new OBBTree (mesh1);
      OBBTree obbTree2 = new OBBTree (mesh2);
      AABBTree aabbTree1 = new AABBTree (mesh1);
      AABBTree aabbTree2 = new AABBTree (mesh2);

      FunctionTimer obbTimer = new FunctionTimer();
      FunctionTimer aabbTimer = new FunctionTimer();
      //FunctionTimer oldTimer = new FunctionTimer();      

      RigidTransform3d X2W = new RigidTransform3d();
      RigidTransform3d X1W = new RigidTransform3d();

      int numcases = 50;
      int timingcnt = 500;
      for (int i=0; i<numcases; i++) {
         X2W.R.setRandom();
         X2W.p.setRandom();
         X2W.p.scale (radius);
         X1W.R.setRandom();
         X1W.p.setRandom();
         X1W.p.scale (radius);

         obbTree1.setBvhToWorld (X1W);
         aabbTree1.setBvhToWorld (X1W);
         obbTree2.setBvhToWorld (X2W);
         aabbTree2.setBvhToWorld (X2W);        
         mesh1.setMeshToWorld (X1W);
         mesh2.setMeshToWorld (X2W);

         obbTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            intersections.clear();
            intersector.intersectMeshMesh (
               intersections, obbTree1, obbTree2);
         }
         obbTimer.stop();
         aabbTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            intersections.clear();
            intersector.intersectMeshMesh (
               intersections, aabbTree1, aabbTree2);          
         }
         aabbTimer.stop();
         // oldTimer.restart();
         // for (int j=0; j<timingcnt; j++) {
         //    intersections.clear();
         //    obbTree1.intersectFully (obbTree2, intersections, ti);
         // }
         // oldTimer.stop();
      }
      int cnt = numcases*timingcnt;
      System.out.println (
         "mesh intersection with OBB: " + obbTimer.result(cnt));
      System.out.println (
         "mesh intersection with AABB: " + aabbTimer.result(cnt));
      // System.out.println (
      //    "mesh intersection with old OBB: " + oldTimer.result(cnt));
   }      

   public void testPlaneIntersection (
      PolygonalMesh mesh, BVTree bvh, Plane plane) {

      BVIntersector intersector = new BVIntersector();
      ArrayList<TriPlaneIntersection> intersections =
         new ArrayList<TriPlaneIntersection>();
      intersector.intersectMeshPlane (intersections, bvh, plane);

      ArrayList<TriPlaneIntersection> bruteForceIntersections =
         intersectAllFaces (mesh, plane);

      ArrayList<TriPlaneIntersection> leafIntersections =
         intersectLeafNodes (bvh, plane, intersector);

      ArrayList<TriPlaneIntersection> obbtreeIntersections =
         new ArrayList<TriPlaneIntersection>();
//      mesh.getObbtree().intersect (
//         plane, obbtreeIntersections, new TriangleIntersector());
      // // convert points of obbIntersections
      // 
      // for (TrianglePlaneIntersection isect : obbtreeIntersections) {
      //    for (Point3d pnt : isect.points) {
      //       pnt.inverseTransform (bvh.getBvhToWorld());
      //    }
      // }

      double tol = bvh.getRadius()*EPS;

      HashSet<TriPlaneIntersect> bruteForceSet =
         createTriPlaneIntersectionSet (bruteForceIntersections);
      HashSet<TriPlaneIntersect> leafSet =
         createTriPlaneIntersectionSet (leafIntersections);
//      HashSet<TriPlaneIntersect> obbtreeSet =
//         createTriPlaneIntersectionSet (obbtreeIntersections);
      HashSet<TriPlaneIntersect> intersectionSet =
         createTriPlaneIntersectionSet (intersections);

      checkTriPlaneIntersectionsEqual (
         "intersections", intersectionSet,
         "brute force intersections", bruteForceSet, tol);
      checkTriPlaneIntersectionsEqual (
         "intersections", intersectionSet,
         "leaf intersections", leafSet, tol);
//      checkTriPlaneIntersectionsEqual (
//         "intersections", intersectionSet,
//         "obbtree intersections", obbtreeSet, tol);

   }

   public void testPlaneIntersection (
      PolygonalMesh mesh, BVTree bvh, RigidTransform3d XMW) {

      Point3d center = new Point3d();
      double radius = bvh.getRadius();
      bvh.getCenter (center);

      mesh.setMeshToWorld (XMW);
      bvh.setBvhToWorld (XMW);

      Random rand = RandomGenerator.get();

      int numtrials = 100;
      for (int i=0; i<numtrials; i++) {
         Plane plane = new Plane();
         plane.normal.setRandom();
         plane.offset = 3*radius*(rand.nextDouble()-0.5);

         testPlaneIntersection (mesh, bvh, plane);
      }
    }

   public void testPlaneIntersection (PolygonalMesh mesh) {
 
      OBBTree OBBTree = new OBBTree (mesh);
      AABBTree AABBTree = new AABBTree (mesh);

      RigidTransform3d XMW = new RigidTransform3d();
      testPlaneIntersection (mesh, OBBTree, XMW);
      testPlaneIntersection (mesh, AABBTree, XMW);
      XMW.setRandom ();
      testPlaneIntersection (mesh, OBBTree, XMW);
      testPlaneIntersection (mesh, AABBTree, XMW);
   }

   public void timing() {
      timeMeshIntersection (myComplexMesh1);
   }

   public void test() {

      testMeshIntersection (MeshFactory.createBox (1.0, 1.5, 2.0));
      testMeshIntersection (MeshFactory.createSphere (1.0, 7));
      if (myComplexMesh1 != null) {
         testMeshIntersection (myComplexMesh1);
      }

      // PolygonalMesh box = MeshFactory.createBox (1.0, 1.0, 1.0);
      // AABBTree bvh = new AABBTree (box);
      // testPlaneIntersection (box, bvh, new Plane (1, 0, 0, 0.51));

      testPlaneIntersection (MeshFactory.createBox (1.0, 1.5, 2.0));
      testPlaneIntersection (MeshFactory.createSphere (1.0, 7));
      if (myComplexMesh1 != null) {
         testPlaneIntersection (myComplexMesh1);
      }

   }

   // Results of time trials compared with old OBBTree code:
   // mesh intersection with OBB: 167.13170437000002 usec
   // mesh intersection with AABB: 169.31956995000002 usec 
   // mesh intersection with old OBB: 174.82318436 usec

   public static void main (String[] args) {

      BVIntersectorTest tester = new BVIntersectorTest();
      boolean doTiming = false;
      boolean doTesting = true;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
            doTesting = false;
         }
         else {
            System.out.println (
               "Usage: java "+tester.getClass().getName()+" [-timing]");
            System.exit(1);
         }
      }      

      try {
         if (doTesting) {
            tester.test();
            System.out.println ("\nPassed\n");
         }
         if (doTiming) {
            tester.timing();
         }
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }

   }

}
