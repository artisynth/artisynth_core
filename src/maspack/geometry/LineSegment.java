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

   public void updateBounds (Point3d min, Point3d max) {
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


}
