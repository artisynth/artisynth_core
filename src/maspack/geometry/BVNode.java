/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.IsRenderable;

/**
 * Base class for bounding volumes such as oriented and axis-aligned bounding
 * boxes. It contains infrastructure that allows these bounding volumes to be
 * assembled into a hierarchy.
 */
public abstract class BVNode implements IsRenderable {

   BVNode myNext;
   BVNode myFirstChild;
   BVNode myLastChild;
   BVNode myParent;
   Boundable[] myElements;
   int myNumber;
   
   public BVNode() {
   }

   /**
    * Adds a child volume to this bounding volume.
    *
    * @param child child to be added
    */
   public void addChild (BVNode child) {
      if (myFirstChild == null) {
         myFirstChild = child;
      }
      else {
         myLastChild.myNext = child;
      }
      child.myNext = null;
      myLastChild = child;
      child.myParent = this;
   }

   /**
    * Returns the first child volume, if any, associated with this bounding
    * volume
    *
    * @return first child
    */
   public BVNode getFirstChild() {
      return myFirstChild;
   }

   /**
    * Returns the next sibling volume, if any, associated with the bounding
    * volume.
    *
    * @return next sibling
    */
   public BVNode getNext() {
      return myNext;
   }

   /**
    * Returns the number of this bounding volume. Numbers can be assigned,
    * usually in depth first order, after a bounding volume tree has been
    * built. Numbers exist for diagnostic purposes only.
    *
    * @return current number of this bounding volume.
    */
   public int getNumber() {
      return myNumber;
   }

   /**
    * Sets the number of this bounding volume. Numbers can be assigned, usually
    * in depth first order, after a bounding volume tree has been built.
    * Numbers exists for diagnostic purposes only.
    *
    * @param num number 
    */
   public void setNumber (int num) {
      myNumber = num;
   }

   /**
    * Returns an approximate "radius" for this bounding volume. This does
    * not have to be precise, but should reflect roughly the maximum distance
    * of the volume boundary from it's center.
    *
    * @return approximate radius of the volume
    */
   public abstract double getRadius();

   /**
    * Returns a center point for this bounding volume.
    *
    * @param center returns to center point
    */
   public abstract void getCenter (Vector3d center);

   /**
    * Returns the number of child volumes contained by this bounding volume.
    *
    * @return number of child volumes
    */
   public int numChildren() {
      int num = 0;
      for (BVNode child = myFirstChild; child != null; child = child.myNext) {
         num++;
      }
      return num;
   }

   /**
    * Returns the boundable elements directly contained by this bounding
    * volume. Generally, a bounding volume will only contain elements if it is
    * a leaf volume.
    *
    * @return boundable elements contained by this bounding volume.
    */
   public Boundable[] getElements() {
      return myElements;
   }

   /**
    * Returns the number of boundable elements directly contained by this
    * bounding volume. Generally, a bounding volume will only contain elements
    * if it is a leaf volume.
    *
    * @return number of boundable elements contained by this bounding volume.
    */
   public int getNumElements() {
      return myElements.length;
   }

   /**
    * Sets the boundable elements directly contained by this bounding
    * volume. Generally, a bounding volume will only contain elements if it is
    * a leaf volume. The array specified is used directly; it is not copied.
    *
    * @param elements boundable elements to be contained
    */
   public void setElements (Boundable[] elements) {
      myElements = elements;
   }

   /**
    * Gets the parent volume, if any, for this bounding volume.
    *
    * @return parent volume
    */
   public BVNode getParent() {
      return myParent;
   }

   /**
    * Sets the parent volume for this bounding volume.
    *
    * @param parent parent volume
    */
   public void setParent (BVNode parent) {
      myParent = parent;
   }

   /**
    * Returns true if this bounding volume is a leaf node; i.e.,
    * if it has no children.
    *
    * @return true if this bounding volume is a leaf node
    */
   public boolean isLeaf() {
      return myFirstChild == null;
   }

   /**
    * Returns true if this bounding volume contains a prescribed point.
    *
    * @param pnt point to be tested
    * @return true if this bounding volume contains pnt.
    */
   public abstract boolean containsPoint (Point3d pnt);

   /**
    * Returns true if this bounding volume intersects a line. The line
    * is described by a point and a direction, such that points x along
    * the line can be described by a parameter s according to
    * <pre>
    * x = origin + s dir
    * </pre>
    * The line can be given finite bounds by specifying maximum and
    * minimum bounds for s.
    *
    * @param lam if non-null, returns the lower and upper values of s
    * that define the intersection region
    * @param origin originating point for the line
    * @param dir direction of the line
    * @param min minimum s value for the line, or -infinity if there
    * is no minimum value
    * @param max maximum s value for the line, or +infinity if there
    * is no maximum value
    * @return true if this bounding volume intersects the line
    */
   public abstract boolean intersectsLine (
      double[] lam, Point3d origin, Vector3d dir, double min, double max);
   
   /**
    * Returns true if this bounding volume intersects a sphere. By specifying a
    * negative value for the radius, the method can be used as a test to see if
    * the center point is located a certain distance within the bounding
    * volume.
    *
    * @param center center point of the sphere
    * @param r radius of the sphere
    * @return true if this bounding volume intersects the sphere
    */
   public abstract boolean intersectsSphere (Point3d center, double r);
   
   /**
    * Returns true if this bounding volume intersects a plane
    * described by
    * <pre>
    * n^T x = d
    * </pre>
    *
    * @param n normal direction of the plane
    * @param d dot product offset
    * @return true if this bounding volume intersects the plane
    */
   public abstract boolean intersectsPlane (Vector3d n, double d);
   
   /**
    * Returns true if this bounding volume intersects a line segment.
    *
    * @param p1 first segment end point
    * @param p2 second segment end point
    * @return true if this bounding volume intersects the line segment.
    */
   public abstract boolean intersectsLineSegment (Point3d p1, Point3d p2);

   /**
    * Returns the distance of a point to this bounding volume, or 0
    * if the point is on or inside it.
    *
    * @param pnt point to check distance for
    * @return distance of pnt to this volume
    */
   public abstract double distanceToPoint (Point3d pnt);

   /**
    * Returns the distance of this bounding volume along a line from a
    * point. If the point is on or inside the bounding volume, then 0 is
    * returned. The line is described by a point and a direction, such that
    * points x along the line can be described by a parameter s according to
    * <pre>
    * x = origin + s dir
    * </pre>
    * The line can be given finite bounds by specifying maximum and
    * minimum bounds for s.
    *
    * @param origin originating point for the line
    * @param dir direction of the line
    * @param min minimum s value for the line, or -infinity if there
    * is no minimum value
    * @param max maximum s value for the line, or +infinity if there
    * is no maximum value
    * @return distance of the point to the volume along the line
    */
   public abstract double distanceAlongLine (
      Point3d origin, Vector3d dir, double min, double max);

   /**
    * Returns true if an entire set of elements is contained within this
    * bounding volume. This method is mainly intended for diagnostic purposes.
    *
    * @param elements set of boundable elements to check
    * @param tol specifies the minimum amount by which each element
    * must be inside
    */
   public abstract boolean isContained (Boundable[] elements, double tol);
   
   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      return 0;
   }

   /**
    * Returns the depth of this bounding volume within a tree. A
    * depth of 0 indicates the root of the tree.
    *
    * @return depth of this bounding volume within a tree
    */
   public int getDepth() {
      int depth = 0;
      BVNode ancestor = getParent();
      while (ancestor != null) {
         ancestor = ancestor.getParent();
         depth++;
      }
      return depth;
   }
   
   /**
    * Update node
    * @param margin supplied margin around boundary
    * @return true if node was modified
    */
   abstract boolean update(double margin);
   
   abstract boolean updateForPoint(Point3d pnt, double margin);
   
   /**
    * Scales this bounding volume node by the indicated scale factor
    * <code>s</code>. This can be used to enlarge a volume to 
    * accommodate a margin.
    * 
    * @param s scaling factor
    */
   abstract void scale (double s);
   
   public boolean updateFor(Boundable b, double margin) {

      boolean modified = false;
      for (int i=0; i<b.numPoints (); i++) {
         Point3d pnt = b.getPoint (i);
         modified |= updateForPoint(pnt, margin);
      }
      return modified;
   }
   
}
