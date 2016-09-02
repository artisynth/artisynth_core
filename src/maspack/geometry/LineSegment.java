/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;

public class LineSegment implements Boundable {

   public Vertex3d myVtx0;
   public Vertex3d myVtx1;

   public LineSegment (Vertex3d vtx0, Vertex3d vtx1) {
      myVtx0 = vtx0;
      myVtx1 = vtx1;
   }

   public void computeCentroid (Vector3d centroid) {
      centroid.add (myVtx0.pnt, myVtx1.pnt);
      centroid.scale (0.5);
   }

  /**
    * Computes covariance of this line segment and returns its length.
    * <p>
    * The formula was determined by substituting the
    * the parametric form for x
    * <pre>
    * x = (1-s) p0 + s p1
    * </pre>
    * into the general formula for C
    * <pre>
    * C = \int_V \rho x x^T dV
    * </pre>
    * and evaluating the integral over the s interval [0,1].
    * 
    * @param C 
    * returns the covariance
    * @return length of the line segment
    */
   public double computeCovariance (Matrix3d C) {
      return CovarianceUtils.computeLineSegmentCovariance (
         C, myVtx0.pnt, myVtx1.pnt);
   }
   
   public double getLength() {
      return myVtx0.pnt.distance(myVtx1.pnt);
   }

   public void updateBounds (Vector3d min, Vector3d max) {
      myVtx0.pnt.updateBounds (min, max);
      myVtx1.pnt.updateBounds (min, max);
   }

   // implementation of IndexedPointSet

   public int numPoints() {
      return 2;
   }

   public Point3d getPoint (int idx) {
      switch (idx) {
         case 0: return myVtx0.pnt;
         case 1: return myVtx1.pnt;
         default: {
            throw new ArrayIndexOutOfBoundsException ("idx=" + idx);
         }
      }
   }
   
   /**
    * Determine the line segment direction
    * @param dir direction vector to populate
    */
   public void getDirection(Vector3d dir) {
      dir.set(myVtx0.getWorldPoint());
      dir.sub(myVtx1.getWorldPoint());
      double nrm = dir.norm();
      if (nrm > 0) {
         dir.scale(1.0/nrm);
      }
   }

   /**
    * Computes the projection parameter of a point <code>px</code>
    * with respect to a line defined by points <code>p0</code> and
    * <code>p1</code>. This is the value <i>s</i> such that
    * <pre>
    * pp = (1-s) p0 + s p1
    * </pre>
    * gives the projection of <code>px</code> onto the line. If
    * <code>p0</code> and <code>p1</code> are identical, the
    * method returns positive infinity.

    * @param p0 first point defining the line
    * @param p1 second point defining the libe
    * @param px point for which the project parameter should be computed
    * @return parameter s which projects px onto the line
    */
   public static double projectionParameter (
      Point3d p0, Point3d p1, Point3d px) {
      
      Vector3d del10 = new Vector3d();      
      Vector3d delx0 = new Vector3d();      

      del10.sub (p1, p0);
      delx0.sub (px, p0);
      double len10Sqr = del10.normSquared();
      if (len10Sqr == 0) {
         return Double.POSITIVE_INFINITY;
      }
      else {
         return del10.dot(delx0)/len10Sqr;
      }      
   }
   
   /**
    * Computes the distance of a point
    * @param pnt point to compute nearest distance from
    * @return nearest distance to point
    */
   public double distance(Point3d pnt) {
      return distance(myVtx0.getWorldPoint(), myVtx1.getWorldPoint(), pnt);
   }

   /**
    * Computes the distance of a point <code>px</code>
    * to a line segment defined by points <code>p0</code> and
    * <code>p1</code>.

    * @param p0 first point defining the segment
    * @param p1 second point defining the segment
    * @param px point to compute distance to
    * @return distance of px from the segment
    */
   public static double distance (Point3d p0, Point3d p1, Point3d px) {

      double s = projectionParameter (p0, p1, px);
      if (s >= 1.0) {
         return px.distance (p1);
      }
      else if (s <= 0) {
         return px.distance (p0);
      }
      else {
         Vector3d ps = new Vector3d();
         ps.combine (1-s, p0, s, p1);
         return px.distance (ps);
      }
   }

}
