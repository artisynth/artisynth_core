/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Base class for a bounding volume tree composed of a hierarchy of bounding
 * volumes.
 * 
 * @author lloyd
 */
public abstract class BVTree implements IsRenderable {
   // protected int myMaxDepth = -1;
   protected int myMaxLeafElements = 2;
   protected double myMargin = 0;
   protected static final double INF = Double.POSITIVE_INFINITY;

   protected RigidTransform3d myBvhToWorld = RigidTransform3d.IDENTITY;
   
   /**
    * Returns an approximate "radius" for this bounding volume hierarchy.
    * This is just the radius of the root bounding volume.
    *
    * @return approximate radius of this bounding volume hierarchy.
    */
   public double getRadius() {
      BVNode root = getRoot();
      if (root != null) {
         return root.getRadius();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns a center point for this bounding volume hierarchy.
    * This is just the center of the root bounding volume, transformed
    * into world coordinates.
    *
    * @param center returns the center of this bounding volume hierarchy.
    */
   public void getCenter (Vector3d center) {
      BVNode root = getRoot();
      if (root != null) {
         root.getCenter (center);
         center.transform (myBvhToWorld.R);
         center.add (myBvhToWorld.p);
      }
      else {
         center.set (myBvhToWorld.p);
      }
   }

   /**
    * Returns the transform that converts points from the local coordinates of
    * this tree to world coordinates.
    *  
    * @return transform from tree to world coordinates. Should not be modified.
    * @see #setBvhToWorld
    */
   public RigidTransform3d getBvhToWorld() {
      return myBvhToWorld;      
   }

   /**
    * Sets the transform that converts points from the local coordinates of
    * this tree to world coordinates. If the tree is associated with a
    * mesh, then this transform should generally be set to the same value
    * as that returned by {@link MeshBase#getMeshToWorld}.
    *
    * @param X new transform value
    * @see #getBvhToWorld
    */
   public void setBvhToWorld (RigidTransform3d X) {
      if (X.equals (RigidTransform3d.IDENTITY)) {
         myBvhToWorld = RigidTransform3d.IDENTITY;
      }
      else if (myBvhToWorld == RigidTransform3d.IDENTITY) {
         myBvhToWorld = new RigidTransform3d(X);
      }
      else {
         myBvhToWorld.set (X);
      }
   }

   /**
    * Returns the transform that converts points from the local coordinates of
    * this tree to world coordinates.
    *
    * @param X returns the transform for this tree
    * @see #setBvhToWorld
    */
   public void getBvhToWorld (RigidTransform3d X) {
      X.set (myBvhToWorld);
   }

   //protected BVNode myRoot;

   /**
    * Returns the root bounding volume for this tree
    * 
    * @return root bounding volume
    */
   public abstract BVNode getRoot();

//   public int getMaxDepth() {
//      return myMaxDepth;
//   }
//
//   public void setMaxDepth (int max) {
//      myMaxDepth = max;
//   }

   /**
    * Returns the maximum number of elements that may be contained
    * in a leaf node.
    * 
    * @return maximum number of leaf elements
    * @see #setMaxLeafElements
    */
   public int getMaxLeafElements() {
      return myMaxLeafElements;
   }

   /**
    * Sets the maximum number of elements that may be contained in a leaf node.
    * Setting this value will only be effective for subsequent
    * <code>build</code> calls.
    *
    * @param max maximum number of leaf elements
    * @see #getMaxLeafElements
    */
   public void setMaxLeafElements (int max) {
      myMaxLeafElements = max;
   }
   
   /**
    * Sets the extra distance margin by which bounding volumes in this tree
    * should surround their elements. This means that all elements within the
    * tree should be at least a distance <code>margin</code> away from the
    * boundaries of their containing volumes. Setting this value will only be
    * effective for subsequent <code>build</code> calls or
    * {@link #update} calls.
    *
    * <p> The purpose of a margin is mainly to overcome numercial errors
    * in determining containment.
    * 
    * @param margin extra distance margin
    * @see #getMargin
    * @see #update
    */
   public void setMargin (double margin) {
      if (margin < 0) {
         myMargin = 0;
      }
      else {
         myMargin = margin;
      }
   }
   
   /**
    * Returns the extra distance margin by which bounding volumes in this
    * tree should surround their elements.
    * 
    * @return extra distance margin
    * @see #setMargin
    */
   public double getMargin () {
      return myMargin;
   }

   // public abstract void build (
   //    IndexedPointSource[] elements, int nelems, int maxPoints);

   /**
    * Builds a bounding volume tree for a set of elements.
    * 
    * @param elist elements around which this tree is to be built.
    */
   public void build (Collection<? extends Boundable> elist) {
      Boundable[] elems = elist.toArray (new Boundable[0]);
      build (elems, elems.length);
   }

   /**
    * Builds a bounding volume tree for a set of elements.
    * 
    * @param elems elements around which this tree is to be built.
    * @param num number of elements
    */
   public abstract void build (Boundable[] elems, int num);

   /**
    * Builds a bounding volume tree for a mesh. The element types
    * used to build the mesh are the same as those returned by
    * {@link #getElementsForMesh}.
    * 
    * @param mesh mesh for which the tree should be built
    */
   public void build (MeshBase mesh) {
      Collection<? extends Boundable> elist = getElementsForMesh (mesh);
      build (elist);
   }  
   
   // /**
   //  * Builds a bounding volume tree for the elements in a mesh. Mesh types
   //  * currently supported include {@link PolygonalMesh}, {@link PolylineMesh},
   //  * and {@link PointMesh}.
   //  * 
   //  * @param mesh mesh for whose elements this tree is to be built.
   //  */
   // public void build (MeshBase mesh) {
   //    adjustMaxLeafElementsForMesh();
   //    build (getElementsForMesh());
   // }

   protected void setMaxLeafElementsForMesh (MeshBase mesh, int maxLeafElems) {
      if (mesh instanceof PolylineMesh) {
         if (maxLeafElems < 3) {
            maxLeafElems = 3;
         }
      }
      else if (mesh instanceof PointMesh) {
         if (maxLeafElems < 4) {
            maxLeafElems = 4;
         }
      }
      setMaxLeafElements (maxLeafElems);
   }      

   protected Collection<? extends Boundable> getElementsForMesh (MeshBase mesh) {
      if (mesh instanceof PolygonalMesh) {
         return ((PolygonalMesh)mesh).getFaces();
      }
      else if (mesh instanceof PolylineMesh) {
         PolylineMesh pmesh = (PolylineMesh)mesh;
         
         int numElems = 0;
         for (Polyline line : pmesh.getLines()) {
            numElems += line.numVertices()-1;
         }
         ArrayList<Boundable> elist =
            new ArrayList<Boundable>(numElems);
         for (Polyline line : pmesh.getLines()) {
            elist.addAll(Arrays.asList(line.getSegments()));
         }
         return elist;
      }
      else {
         return mesh.getVertices();
      }
   }            

   protected void setMarginForMesh (MeshBase mesh, double margin) {
      if (margin < 0) {
         setMargin (1e-8*RenderableUtils.getRadius(mesh));
      }
      else {
         setMargin (margin);
      }
   }      

//   public void intersectRay (
//      ArrayList<BVNode> nodes, Point3d origin, Vector3d dir) {
//      recursivelyIntersectRay (nodes, origin, dir, myRoot);
//   }

   /**
    * Returns a list of all leaf nodes in this tree which contain
    * a specified point.
    * 
    * @param nodes returns all leaf nodes containing the point
    * @param pnt point to test for (world coordinates)
    */
   public void intersectPoint (ArrayList<BVNode> nodes, Point3d pnt) {
      if (myBvhToWorld != RigidTransform3d.IDENTITY) {
         pnt = new Point3d (pnt);
         pnt.inverseTransform (myBvhToWorld);
      }
      recursivelyIntersectPoint (nodes, pnt, getRoot());
   }

   protected void recursivelyIntersectPoint (
      ArrayList<BVNode> nodes, Point3d pnt, BVNode node) {
      if (node == null) {
         System.out.println("Warning: null node in BVTree");
         return;
      }
      if (node.containsPoint (pnt)) {
         if (node.isLeaf()) {
            nodes.add (node);
            return;
         }
         else {
            BVNode child = node.myFirstChild;
            while (child != null) {
               recursivelyIntersectPoint (nodes, pnt, child);
               child = child.myNext;
            }
         }
      }
   }

   /**
    * Returns a list of all leaf nodes in this tree which intersect a sphere.
    * 
    * @param nodes returns all leaf nodes intersecting the sphere
    * @param center center of the sphere (world coordinates)
    * @param r sphere radius
    */
   public void intersectSphere (
      ArrayList<BVNode> nodes, Point3d center, double r) {

      if (myBvhToWorld != RigidTransform3d.IDENTITY) {
         center = new Point3d (center);
         center.inverseTransform (myBvhToWorld);
      }
      recursivelyIntersectSphere (nodes, center, r, getRoot());
   }

   protected void recursivelyIntersectSphere (
      ArrayList<BVNode> nodes, Point3d origin, double r, BVNode node) {
      if (node.intersectsSphere (origin, r)) {
         if (node.isLeaf()) {
            nodes.add (node);
            return;
         }
         else {
            BVNode child = node.myFirstChild;
            while (child != null) {
               recursivelyIntersectSphere (nodes, origin, r, child);
               child = child.myNext;
            }
         }
      }
   }

   /**
    * Returns a list of all leaf nodes in this tree which intersect a plane.
    * 
    * @param nodes returns all leaf nodes intersecting the plane
    * @param plane plane to intersect with (world coordinates)
    */
   public void intersectPlane (ArrayList<BVNode> nodes, Plane plane) {

      if (myBvhToWorld != RigidTransform3d.IDENTITY) {
         plane = new Plane (plane);
         plane.inverseTransform (myBvhToWorld);
      }
      recursivelyIntersectPlane (nodes, plane, getRoot());
   }

   protected void recursivelyIntersectPlane (
      ArrayList<BVNode> nodes, Plane plane, BVNode node) {
      if (node == null) {
         System.out.println("Warning: null node in BVTree");
         return;
      }
      if (node.intersectsPlane (plane.normal, plane.offset)) {
         if (node.isLeaf()) {
            nodes.add (node);
            return;
         }
         else {
            BVNode child = node.myFirstChild;
            while (child != null) {
               recursivelyIntersectPlane (nodes, plane, child);
               child = child.myNext;
            }
         }
      }
   }

   /**
    * Returns a list of all leaf nodes in this tree which intersect a line.
    * The line is described by a point and a direction, such that points x
    * along the line can be described by a parameter s according to
    * <pre>
    * x = origin + s dir
    * </pre>
    * The line can be given finite bounds by specifying maximum and
    * minimum bounds for s.
    *
    * @param nodes returns all leaf nodes intersecting the line
    * @param origin originating point for the line (world coordinates)
    * @param dir direction of the line (world coordinates)
    * @param min minimum s value for the line, or -infinity if there
    * is no minimum value
    * @param max maximum s value for the line, or +infinity if there
    * is no maximum value
    */
   public void intersectLine (
      ArrayList<BVNode> nodes, Point3d origin, Vector3d dir,
      double min, double max) {

      // transform to the mesh's coordinate system
      if (myBvhToWorld != RigidTransform3d.IDENTITY) {
         origin = new Point3d (origin); 
         origin.inverseTransform (myBvhToWorld); 
         dir = new Vector3d (dir);
         dir.inverseTransform (myBvhToWorld);
      }
      recursivelyIntersectLine (nodes, origin, dir, getRoot(), min, max);
   }

   protected void recursivelyIntersectLine (
     ArrayList<BVNode> nodes, Point3d origin, Vector3d dir, BVNode node,
     double min, double max) {
     
     if (node.intersectsLine (null, origin, dir, min, max)) {
        if (node.isLeaf()) {
           nodes.add (node);
           return;
        }
        else {
           BVNode child = node.myFirstChild;
           while (child != null) {
              recursivelyIntersectLine (nodes, origin, dir, child, min, max);
              child = child.myNext;
           }
        }
     }
   }
 
   /**
    * Returns all intersecting pairs of leaf nodes between this tree and
    * another tree. The pairs are returned in two arrays of equal length.  The
    * relative coordinates frames of each tree (as specified by {@link
    * #getBvhToWorld}) are taken into account when computing this intersection.
    * 
    * @param nodes1 intersecting leaf nodes from this tree
    * @param nodes2 intersecting leaf nodes from the other tree
    * @param bvt other tree to intersect with
    */
   public void intersectTree (
      ArrayList<BVNode> nodes1, ArrayList<BVNode> nodes2,
      BVTree bvt) {  

      RigidTransform3d X21 = new RigidTransform3d();
      X21.mulInverseLeft (myBvhToWorld, bvt.myBvhToWorld);

      intersectTree (nodes1, nodes2, bvt, X21);
   }

   protected void recursivelyIntersectTree (
      ArrayList<BVNode> nodes1, ArrayList<BVNode> nodes2,
      BVNode node1, BVNode node2, BVNodeTester tester, RigidTransform3d X21) {

      boolean disjoint = tester.isDisjoint (node1, node2, X21); 
      if (!disjoint) {
         if (node1.isLeaf() && node2.isLeaf()) {
            nodes1.add (node1);
            nodes2.add (node2);
         }
         else if (node1.isLeaf()) {
            BVNode child2 = node2.myFirstChild;
            while (child2 != null) {
               recursivelyIntersectTree (
                  nodes1, nodes2, node1, child2, tester, X21);
               child2 = child2.myNext;
            }
         }
         else if (node2.isLeaf()) {
            BVNode child1 = node1.myFirstChild;
            while (child1 != null) {
               recursivelyIntersectTree (
                  nodes1, nodes2, child1, node2, tester, X21);
               child1 = child1.myNext;
            }
         }
         else {
            BVNode child1 = node1.myFirstChild;
            while (child1 != null) {
               BVNode child2 = node2.myFirstChild;
               while (child2 != null) {
                  recursivelyIntersectTree (
                     nodes1, nodes2, child1, child2, tester, X21);
                  child2 = child2.myNext;
               }
               child1 = child1.myNext;
            }
         }
      }
   }

   /**
    * Numbers all nodes in this tree in depth-first order and returns the total
    * number of nodes.
    * 
    * @return number of nodes in the tree
    */
   public int numberNodes () {
      return numberNodes (getRoot(), 0);
   }

   protected int numberNodes (BVNode node, int num) {
      if (node != null) {
         node.setNumber (num++);
         BVNode child;
         for (child=node.getFirstChild(); child!=null; child=child.getNext()) {
            num = numberNodes (child, num);
         }
      }
      return num;      
   }      

   /**
    * Returns all intersecting pairs of leaf nodes between this tree and
    * another tree. The pairs are returned in two arrays of equal length.  The
    * relative coordinate transformation between the two tree is specified
    * explicity and not based on the values returned by {@link #getBvhToWorld}.
    * 
    * @param nodes1 intersecting leaf nodes from this tree
    * @param nodes2 intersecting leaf nodes from the other tree
    * @param bvt other tree to intersect with
    * @param X21 transform from the coordinate frame of the other
    * tree to that of this tree.
    */
   public void intersectTree (
      ArrayList<BVNode> nodes1, ArrayList<BVNode> nodes2,
      BVTree bvt, RigidTransform3d X21) {  

      BVNodeTester tester = null;

      if ((!(this instanceof AABBTree) && !(this instanceof OBBTree)) ||
          (!(bvt instanceof AABBTree) && !(bvt instanceof OBBTree))) {
         throw new IllegalArgumentException (
            "Each bounding volume hierarchy must be an AABBTree or an OBBTree");
      }
      tester = new BVBoxNodeTester (this, bvt);
      recursivelyIntersectTree (
         nodes1, nodes2, getRoot(), bvt.getRoot(), tester, X21); 
   }

   /**
    * Returns a list of all leaf nodes in this tree which intersect a line
    * segment.
    *
    * @param p1 first segment end point
    * @param p2 second segment end point
    * @param nodes returns all leaf nodes intersecting the line segment
    */
   public void intersectLineSegment (
      ArrayList<BVNode> nodes, Point3d p1, Point3d p2) {

      // transform to the mesh's coordinate system
      if (myBvhToWorld != RigidTransform3d.IDENTITY) {
         p1 = new Point3d (p1); 
         p1.inverseTransform (myBvhToWorld);
         p2 = new Point3d (p2);
         p2.inverseTransform (myBvhToWorld);
      }
      recursivelyIntersectLineSegment (nodes, p1, p2, getRoot());
   }

   protected void recursivelyIntersectLineSegment (
      ArrayList<BVNode> nodes, Point3d p1, Point3d p2, BVNode node) {
      if (node.intersectsLineSegment(p1, p2)) {
         if (node.isLeaf()) {
            nodes.add (node);
            return;
         }
         else {
            BVNode child = node.myFirstChild;
            while (child != null) {
               recursivelyIntersectLineSegment (nodes, p1, p2, child);
               child = child.myNext;
            }
         }
      }
   }

   /**
    * Updates the bounding volumes in this tree to ensure that they
    * properly contain their enclosed elements. This should be called
    * when the positions of the elements changes (such when the vertices
    * of mesh change position).
    *
    * @see #setMargin 
    */
   public abstract void update();

   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      return 0;
   }

   /**
    * {@inheritDoc}
    */
   public void updateBounds (Vector3d min, Vector3d max) {
      if (getRoot() != null) {
         getRoot().updateBounds (min, max);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void prerender (RenderList list) {
   }
   
   /**
    * {@inheritDoc}
    */
   public void render (Renderer renderer, int flags) {
      if (getRoot() != null) {
         recursivelyRender (renderer, flags, getRoot());
      }
   }

   protected void recursivelyRender (
      Renderer renderer, int flags, BVNode node) {
      node.render (renderer, flags);
      BVNode child = node.myFirstChild;
      while (child != null) {
         recursivelyRender (renderer, flags, child);
         child = child.myNext;
      }
   }

   public void print() {
      PrintWriter pw =
         new IndentingPrintWriter (new OutputStreamWriter (System.out));
      print (pw);
      pw.flush();
   }

   public void print (PrintWriter pw) {
      recursivelyPrint (pw, getRoot());
   }

   public void printElement (PrintWriter pw, Boundable src) {
      if (src instanceof Vertex3d) {
         pw.println ("Vertex " + ((Vertex3d)src).getIndex());
      }
      else if (src instanceof Face) {
         Face face = (Face)src;
         pw.println ("Face " + face.getIndex());
         IndentingPrintWriter.addIndentation (pw, 2);
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            pw.println ("Vertex "+he.head.getIndex());
            he = he.getNext();
         }
         while (he != he0);
         IndentingPrintWriter.addIndentation (pw, -2);
      }
      else if (src instanceof LineSegment) {
         LineSegment seg = (LineSegment)src;
         pw.println (
            "LineSegment "+seg.myVtx0.getIndex()+"-"+seg.myVtx1.getIndex());
      }
      else {
         pw.println (src);
      }
   }

   protected void recursivelyPrint (PrintWriter pw, BVNode node) {
      pw.println ("Node "+node.getNumber());
      IndentingPrintWriter.addIndentation (pw, 2);
      if (node.isLeaf()) {
         // for (Boundable elem : node.getElements()) {
         //    printElement (pw, elem);
         // }
      }
      else {
         BVNode child = node.myFirstChild;
         while (child != null) {
            recursivelyPrint (pw, child);
            child = child.myNext;
         }         
      }
      IndentingPrintWriter.addIndentation (pw, -2);
   }
   
   /**
    * Returns all the leaf nodes in this tree.
    * 
    * @return all leaf nodes
    */
   public ArrayList<BVNode> getLeafNodes () {
      ArrayList<BVNode> nodes = new ArrayList<BVNode>();
      getLeafNodes (nodes, getRoot());
      return nodes;
   }

   private void getLeafNodes (
      ArrayList<BVNode> nodes, BVNode node) {

      if (node.isLeaf()) {
         nodes.add (node);
      }
      else {
         BVNode child;
         for (child=node.getFirstChild(); child!=null; child=child.getNext()) {
            getLeafNodes (nodes, child);
         }
      }
   }

   /**
    * Returns the number of nodes in this tree.
    * 
    * @return number of nodes in the tree
    */
   public int numNodes() {
      return numNodes (getRoot());
   }

   protected int numNodes (BVNode node) {
      int num = 1;
      BVNode child;
      for (child = node.myFirstChild; child != null; child = child.myNext) {
         num += numNodes (child);
      }
      return num;
   }

   void printNumLeafFaces (BVNode node) {
      if (node.isLeaf()) {
         System.out.print (node.myNumber+":"+node.myElements.length+" ");
      }
      else {
         BVNode child;
         for (child=node.myFirstChild; child!=null; child=child.myNext) {
            printNumLeafFaces (child);
         }
      }
   }

   public void printNumLeafFaces (String msg) {
      System.out.print (msg);
      printNumLeafFaces (getRoot());
      System.out.println ("");
   }


}
