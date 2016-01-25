/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.*;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import maspack.geometry.OBB.Method;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.RenderableUtils;

public class BVTreeTest {

   PolygonalMesh myComplexMesh1;
   PolygonalMesh myComplexMesh2;

   AABBTree myAABBTree1;
   AABBTree myAABBTree2;
   OBBTree myOBBTree1;
   OBBTree myOBBTree2;

   public BVTreeTest() {
      String meshFileName =
         PathFinder.expand (
            "${srcdir PolygonalMesh}/sampleData/osCoxaeRight108v.obj");
      try {
         myComplexMesh1 = new PolygonalMesh (new File (meshFileName));
      }
      catch (Exception e) {
         System.out.println (e);
      }
      myComplexMesh2 = new PolygonalMesh (myComplexMesh1);
      myAABBTree1 = new AABBTree (myComplexMesh1, 2);
      myAABBTree2 = new AABBTree (myComplexMesh2, 2);
      myOBBTree1 = new OBBTree (myComplexMesh1, 2, 0.001);
      myOBBTree2 = new OBBTree (myComplexMesh2, 2);
      myAABBTree1.numberNodes();
      myAABBTree2.numberNodes();
      myOBBTree1.numberNodes();
      myOBBTree2.numberNodes();
   }

   private class BVNodePair {
      BVNode myNode1;
      BVNode myNode2;

      BVNodePair (BVNode node1, BVNode node2) {
         myNode1 = node1;
         myNode2 = node2;
      }

      public int hashCode () {
         return (myNode1.hashCode() + myNode2.hashCode());
      }

      public boolean equals (Object obj) {
         if (obj instanceof BVNodePair) {
            BVNodePair pair = (BVNodePair)obj;
            return ((myNode1 == pair.myNode1 && myNode2 == pair.myNode2) ||
                    (myNode1 == pair.myNode2 && myNode2 == pair.myNode1));
         }
         else {
            return false;
         }
      }

      public boolean hasIntersectingFaces (RigidTransform3d X21) {
         return BVTreeTest.hasIntersectingFaces (myNode1, myNode2, X21);
      }

      public String toString() {
         return "("+myNode1.getNumber()+","+myNode2.getNumber()+")";
      }
   }

   private static boolean hasIntersectingFaces (
      BVNode node1, BVNode node2, RigidTransform3d X21) {

      TriangleIntersector ti = new TriangleIntersector();
      Point3d v0 = new Point3d();
      Point3d v1 = new Point3d();
      Point3d v2 = new Point3d();

      Boundable[] elems1 = node1.getElements();
      Boundable[] elems2 = node2.getElements();
      for (int i=0; i<elems1.length; i++) {
         if (elems1[i] instanceof Face) {
            Face face1 = (Face)elems1[i];

            HalfEdge he = face1.firstHalfEdge();
            Point3d p0 = he.head.pnt;
            he = he.getNext();
            Point3d p1 = he.head.pnt;
            he = he.getNext();
            Point3d p2 = he.head.pnt;

            for (int j=0; j<elems2.length; j++) {
               if (elems2[j] instanceof Face) {
                  Face face2 = (Face)elems2[j];

                  he = face2.firstHalfEdge();
                  v0.transform (X21, he.head.pnt);
                  he = he.getNext();
                  v1.transform (X21, he.head.pnt);
                  he = he.getNext();
                  v2.transform (X21, he.head.pnt);
                  
                  if (ti.intersectTriangleTriangle (
                         p0, p1, p2, v0, v1, v2) != null) {
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }
   
   private HashSet<BVNodePair> getAllIntersectingLeafNodes (
      BVTree bvh1, BVTree bvh2, RigidTransform3d X21) {

      BVNodeTester tester = new BVBoxNodeTester (bvh1, bvh2);

      HashSet<BVNodePair> pairs = new HashSet<BVNodePair>();
      ArrayList<BVNode> leafs1 = bvh1.getLeafNodes();
      ArrayList<BVNode> leafs2 = bvh2.getLeafNodes();
      for (int i=0; i<leafs1.size(); i++) {
         for (int j=0; j<leafs2.size(); j++) {
            if (!tester.isDisjoint (leafs1.get(i), leafs2.get(j), X21)) {
               pairs.add (new BVNodePair (leafs1.get(i), leafs2.get(j)));
            }
         }
      }
      return pairs;
   }

   private void checkNodeSetsEqual (
      String msg, ArrayList<BVNode> nodes, HashSet<BVNode> check, Point3d pnt) {

      if (!nodes.containsAll (check)) {
         System.out.println ("nodes.size=" + nodes.size());
         System.out.println ("check.size=" + check.size());
         HashSet<BVNode> missing = new HashSet<BVNode>();
         missing.addAll (check);
         missing.removeAll (nodes);
         System.out.print ("Missing nodes:");
         BVNode firstMissing = null;
         for (BVNode node : missing) {
            System.out.print (" " + node.getNumber());
            if (firstMissing == null) {
               firstMissing = node;
            }
         }
         System.out.println ("");
         System.out.println ("Trace back for first missing node:");
         for (BVNode node=firstMissing; node!=null; node=node.getParent()) {
            System.out.println (
               " Node " + node.getNumber() + " " + node.containsPoint(pnt));
         }
         throw new TestException (
            msg + " does not produce all nodes found by exhaustive search");
      }

      if (!check.containsAll (nodes)) {
         System.out.println ("nodes.size=" + nodes.size());
         System.out.println ("check.size=" + check.size());
         HashSet<BVNode> extra = new HashSet<BVNode>();
         extra.addAll (nodes);
         extra.removeAll (check);
         System.out.print ("Extra nodes:");
         for (BVNode node : extra) {
            System.out.print (" " + node.getNumber());
         }
         System.out.println ("");    
         throw new TestException (
            msg + " produces excess nodes over exhaustive search");
      }
   }

   public void testIntersectPoint (BVTree bvh) {
      Point3d center = new Point3d();
      double radius = RenderableUtils.getRadiusAndCenter (center, bvh);

      ArrayList<BVNode> leafNodes = bvh.getLeafNodes();

      int numtrials = 100;
      for (int i=0; i<numtrials; i++) {
         Point3d pnt = new Point3d();
         pnt.setRandom();
         pnt.scale (2*radius);
         pnt.add (center);

         ArrayList<BVNode> nodes = new ArrayList<BVNode>();
         bvh.intersectPoint (nodes, pnt);
         HashSet<BVNode> check = new HashSet<BVNode>();
         for (BVNode node : leafNodes) {
            if (node.containsPoint (pnt)) {
               check.add (node);
            }
         }
         checkNodeSetsEqual ("intersectPoint:", nodes, check, pnt);
      }
   }

   public void testIntersectTree (
      BVTree bvh1, BVTree bvh2, RigidTransform3d X21) {

      ArrayList<BVNode> nodes1 = new ArrayList<BVNode>();
      ArrayList<BVNode> nodes2 = new ArrayList<BVNode>();

      bvh1.intersectTree (nodes1, nodes2, bvh2, X21);
      HashSet<BVNodePair> allpairs =
         getAllIntersectingLeafNodes (bvh1, bvh2, X21);

      if (nodes1.size() != nodes2.size()) {
         throw new TestException (
            "Nodes lists returned by intersectHierarchy() have different sizes: "+
            nodes1.size() + " vs. " + nodes2.size());
      }
      HashSet<BVNodePair> foundpairs = new HashSet<BVNodePair>();
      for (int i=0; i<nodes1.size(); i++) {
         BVNode node1 = nodes1.get(i);
         BVNode node2 = nodes2.get(i);
         BVNodePair pair = new BVNodePair (node1, node2);
         if (!allpairs.contains (pair)) {
            throw new TestException (
               "Node pair (" + node1 + " " + node2 + ") missing");
         }
         foundpairs.add (pair);
      }
      for (BVNodePair pair : allpairs) {
         if (!foundpairs.contains (pair)) {
            if (pair.hasIntersectingFaces (X21)) {
               throw new TestException (
                  "Node pair "+pair+" with intersecting faces " +
                  " found by exhaustive search missing from intersection");
            }
         }
      }
   }

   public void testIntersectTree() {
      int numtrials = 100;

      double radius = RenderableUtils.getRadius (myComplexMesh1);

      for (int i=0; i<numtrials; i++) {
         RigidTransform3d X21 = new RigidTransform3d();
         X21.R.setRandom();
         X21.p.setRandom();
         X21.p.scale (0.25*radius);

         testIntersectTree (myAABBTree1, myAABBTree2, X21);
         testIntersectTree (myAABBTree1, myOBBTree2, X21);
         testIntersectTree (myOBBTree1, myAABBTree2, X21);
         testIntersectTree (myOBBTree1, myOBBTree2, X21);
      }
   }

   public void testIntersections() {
      testIntersectPoint (myAABBTree1);
      //myOBBTree1.print();
      testIntersectPoint (myOBBTree1);
   }

   private void testPointContainedInAncestors (Point3d pnt, BVNode node) {
      for (BVNode n=node; node!=null; node=node.getParent()) {
         if (!n.containsPoint (pnt)) {
            throw new TestException (
               "Point "+pnt+" contained in leaf node "+node.getNumber()+
               " not contained in ancestor "+n.getNumber());
         }
      }
   }

   private void testLineSegmentContainedInAncestors (
      Point3d pnt0, Point3d pnt1, BVNode node) {

      for (BVNode n=node; node!=null; node=node.getParent()) {
         if (!n.intersectsLineSegment (pnt0, pnt1)) {
            throw new TestException (
               "Line segment ("+pnt0+","+pnt1+") contained in leaf node "+
               node.getNumber()+" not contained in ancestor "+n.getNumber());
         }
      }
   }

   private void testFeatureContainedInAncestors (
      Boundable elem, BVNode node) {

      if (elem instanceof Vertex3d) {
         testPointContainedInAncestors (((Vertex3d)elem).pnt, node);
      }
      else if (elem instanceof LineSegment) {
         LineSegment seg = (LineSegment)elem;
         testPointContainedInAncestors (seg.myVtx0.pnt, node);
         testPointContainedInAncestors (seg.myVtx1.pnt, node);
         testLineSegmentContainedInAncestors (
            seg.myVtx0.pnt, seg.myVtx1.pnt, node);
      }
      else if (elem instanceof Face) {
         Face face = (Face)elem;
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            testPointContainedInAncestors (he.head.pnt, node);
            testLineSegmentContainedInAncestors (
               he.tail.pnt, he.head.pnt, node);
            he = he.getNext();
         }
         while (he != he0);
      }
   }

   public void testFeaturesContainedInAncestors (BVTree bvh) {
      ArrayList<BVNode> leaves = bvh.getLeafNodes();
      for (BVNode node : leaves) {
         Boundable[] elems = node.getElements();
         for (int i=0; i<elems.length; i++) {
            testFeatureContainedInAncestors (elems[i], node);
         }
      }
   }

   public void testFeaturesBoundedByNodes (BVNode node, double tol) {
      ArrayList<Boundable> list = new ArrayList<Boundable>();
      collectAllBoundables (list, node);
      if (!node.isContained (list.toArray(new Boundable[0]), tol)) {
         for (Boundable b : list) {
            Face face = (Face)b;
            HalfEdge he0 = face.firstHalfEdge();
            HalfEdge he = he0;
            do {
               if (!node.intersectsSphere (he.head.pnt, -tol/2)) {
                  System.out.println (
                     "pnt " + he.head.pnt + " not contained in node");
               }
               he = he.getNext();
            }
            while (he != he0);
            System.out.println (b);
         }
         OBB obb = (OBB)node;
         System.out.println ("tol=" + tol);
         System.out.println ("hw=" + obb.myHalfWidths);
         System.out.println ("X=\n" + obb.myX);
         
         throw new TestException (
            "Node "+node.getNumber()+" does not bound all its boundables");
      }
      BVNode child;
      for (child=node.getFirstChild(); child!=null; child=child.getNext()) {
         testFeaturesBoundedByNodes (child, tol);
      }
   }

   private void collectAllBoundables (ArrayList<Boundable> list, BVNode node) {
      if (node.isLeaf()) {
         Boundable[] elems = node.getElements();
         for (int i=0; i<elems.length; i++) {
            list.add (elems[i]);
         }
      }
      else {
         BVNode child;
         for (child=node.getFirstChild(); child!=null; child=child.getNext()) {
            collectAllBoundables (list, child);
         }
      }
   }

   public void testContainment (MeshBase mesh) {

      double radius = RenderableUtils.getRadius (mesh);
      double noiseAmplitude = radius/Math.pow (mesh.numVertices(), 1/3.0);
      
      int numtrials = 50;
      Vector3d del = new Vector3d();
      for (int i=0; i<numtrials; i++) {
         if (i > 0) {
            del.setRandom ();
            del.scale (noiseAmplitude);
            for (Vertex3d vtx : mesh.getVertices()) {
               vtx.pnt.add (del);
            }
            mesh.notifyVertexPositionsModified();
         }

         ArrayList<BVTree> trees = new ArrayList<BVTree>();
         trees.add (new AABBTree (mesh));
         trees.add (new OBBTree (mesh));
         trees.add (new OBBTree (mesh, Method.Covariance));
         trees.add (new OBBTree (mesh, Method.Points));
         
         for (BVTree tree : trees) {
            testFeaturesContainedInAncestors (tree);
            testFeaturesBoundedByNodes (tree.getRoot(), tree.getMargin());
         }
      }
   }

   public void testUpdating (MeshBase mesh) {

      double radius = RenderableUtils.getRadius (mesh);
      double noiseAmplitude = radius/Math.pow (mesh.numVertices(), 1/3.0);

      AABBTree aabbTree = new AABBTree (mesh);
      OBBTree obbTree = new OBBTree (mesh);
      
      int numtrials = 50;
      Vector3d del = new Vector3d();
      for (int i=0; i<numtrials; i++) {
         del.setRandom ();
         del.scale (noiseAmplitude);
         for (Vertex3d vtx : mesh.getVertices()) {
            vtx.pnt.add (del);
         }
         aabbTree.update();
         obbTree.update();

         testFeaturesContainedInAncestors (aabbTree);
         testFeaturesContainedInAncestors (obbTree);
         testFeaturesBoundedByNodes (aabbTree.getRoot(), aabbTree.getMargin());
         testFeaturesBoundedByNodes (obbTree.getRoot(), obbTree.getMargin());
      }
   }
      
   public void test() {
      testIntersectTree();
      testContainment (MeshFactory.createBox (1.0, 1.5, 2.0));
      testContainment (MeshFactory.createSphere (1.0, 7));
      testContainment (new PolygonalMesh (myComplexMesh1));
      testContainment (MeshFactory.createSphericalPolyline (8.0, 12, 12));
      testContainment (MeshFactory.createRandomPointMesh (200, 5));

      testUpdating (MeshFactory.createBox (1.0, 1.5, 2.0));
      testUpdating (MeshFactory.createSphere (1.0, 7));
      testUpdating (new PolygonalMesh (myComplexMesh1));
      testUpdating (MeshFactory.createSphericalPolyline (8.0, 12, 12));
      testUpdating (MeshFactory.createRandomPointMesh (200, 5));
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      BVTreeTest tester = new BVTreeTest();
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

