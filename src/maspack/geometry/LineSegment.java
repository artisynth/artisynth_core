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

public class LineSegment extends Feature implements Boundable {

   public Vertex3d myVtx0;
   public Vertex3d myVtx1;

   private static double DOUBLE_PREC = 2.0e-16;

   public LineSegment (Vertex3d vtx0, Vertex3d vtx1) {
      super(Feature.LINE_SEGMENT);
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
    * gives the projection of <code>px-p0</code> onto the line. If
    * <code>p0</code> and <code>p1</code> are identical, the
    * method returns positive infinity.
    *
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
    * For two lines defined by the points <code>p0, p1</code>
    * and <code>pa, pb</code>, computes the parameters
    * <code>s</code> and <code>t</code> such that
    * <pre>
    * ps = (1-s) p0 + s p1
    * pt = (1-t) pa + t pb
    * </pre>
    * are the points on the two lines which are nearest to
    * each other. If the two lines are parallel, <code>s</code>
    * and <code>t</code> are both set to 0 and the method returns
    * <code>false</code>.
    *
    * @param params returns the values of s and t 
    * @param p0 first point defining the first line
    * @param p1 second point defining the first line
    * @param pa first point defining the second line
    * @param pb second point defining the second line
    * @return returns <code>true</code> if the lines are
    * not parallel and <code>false</code> if they are
    */
   public static boolean nearestPointParameters (
      double[] params, Point3d p0, Point3d p1, Point3d pa, Point3d pb) {

      Vector3d u01 = new Vector3d();
      Vector3d uab = new Vector3d();

      u01.sub (p1, p0);
      double len01 = u01.norm();
      u01.scale (1/len01);
      uab.sub (pb, pa);
      double lenab = uab.norm();
      uab.scale (1/lenab);

      Vector3d tmp = new Vector3d();
      tmp.cross (u01, uab);
      double denom = tmp.normSquared();
      if (denom < 100 * DOUBLE_PREC) {
         params[0] = 0;
         params[1] = 0;
         return false;
      }
      else {
         tmp.sub (p0, pa);
         double k1 = -u01.dot (tmp);
         double k2 = uab.dot (tmp);
         double dotU = u01.dot (uab);
         params[0] = (k1 + dotU * k2) / (len01 * denom);
         params[1] = (dotU * k1 + k2) / (lenab * denom);
         return true;
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
   
   public double getProjectionParameter(Point3d pnt) {
      Point3d p0 = myVtx0.getWorldPoint();
      Point3d p1 = myVtx1.getWorldPoint();
      return projectionParameter(p0, p1, pnt);
   }
   
   @Override
   public void nearestPoint(Point3d nearest, Point3d pnt) {
      Point3d p0 = myVtx0.getWorldPoint();
      Point3d p1 = myVtx1.getWorldPoint();
      
      double s = projectionParameter(p0, p1, pnt);
      if (s >= 1.0) {
         nearest.set(p0);
      }
      else if (s <= 0) {
         nearest.set(p1);
      }
      else {
         nearest.combine (1-s, p0, s, p1);
      }
   }

}
