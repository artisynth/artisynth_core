/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import java.util.ArrayList;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;

public class SegmentedPlanarCoupling extends RigidBodyCoupling {
   private ArrayList<Plane> myPlanes;
   private ArrayList<Point3d> myPoints;
   private Point3d myPnt = new Point3d(); // temporary
   private Point3d myTmp = new Point3d(); // temporary
   private Vector3d myNrm = new Vector3d(); // temporary
   private boolean myUnilateral = false;

   private void makePlanesFromSegments (ArrayList<Point3d> segPoints) {
      myPlanes = new ArrayList<Plane>();
      // now create the planes
      Vector3d u = new Vector3d();
      Vector3d nrm = new Vector3d();
      Vector3d y = new Vector3d (0, 1, 0);
      for (int i = 1; i < segPoints.size(); i++) {
         u.sub (segPoints.get(i), segPoints.get (i - 1));
         nrm.cross (u, y);
         nrm.normalize();
         myPlanes.add (new Plane (nrm, nrm.dot (segPoints.get(i))));
      }
   }

   private void makeSegmentPoints (double[] segs) {
      myPoints = new ArrayList<Point3d>();
      // make private copy of the points
      for (int i = 0; i < segs.length; i += 2) {
         Point3d pnt = new Point3d (segs[i], 0.0, segs[i + 1]);
         myPoints.add (pnt);
      }
   }

   /**
    * Returns a list of points describing the piecewise linear curve in the D
    * frame x-z plane which defines the plane segments. These returned points
    * are read-only and should not be modified by the caller.
    * 
    * @return points defining the plane segments (should not be modified)
    */
   public ArrayList<Point3d> getSegmentPoints() {
      return myPoints;
   }

   /**
    * Returns a list of the planes in frame D. The returned planes are read-only
    * and should not be modified by the caller.
    * 
    * @return plane segments (should not be modified)
    */
   public ArrayList<Plane> getPlanes() {
      return myPlanes;
   }

   /**
    * Returns the number of planar segments associated with this coupling.
    * 
    * @return number of planes
    */
   public int numPlanes() {
      return myPlanes.size();
   }

   public void setUnilateral (boolean unilateral) {
      myUnilateral = unilateral;
   }

   public boolean isUnilateral() {
      return myUnilateral;
   }

   public SegmentedPlanarCoupling() {
      super();
   }

//   public SegmentedPlanarCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXFA (TCA);
//      setXDB (XDB);
//   }

//   public SegmentedPlanarCoupling (RigidTransform3d TCA, RigidTransform3d XDB,
//   double[] segs) {
//      this();
//      setXFA (TCA);
//      setXDB (XDB);
//      setSegments (segs);
//   }
   
   public SegmentedPlanarCoupling (double[] segs) {
      this();
      setSegments (segs);
   }
   

   /**
    * Sets the plane segments associated with this SegmentedPlanarCoupling. The
    * segments are defined by a sequence of x-z coordinate pairs in the D
    * coordinate frame. The number of segments equals the number of pairs minus
    * one, so there must be at least two pairs (which would define a single
    * plane). Segments are assumed to be contiguous, and the normal for each is
    * defined by u X y, where u is a vector in the direction of the segment
    * (from first to last coordinate pair) and y is the direction of the D frame
    * y axis. The first and last plane segments are assumed to extend to
    * infinity.
    * 
    * @param segs
    * x-z coordinate pairs defining the segments
    */
   public void setSegments (double[] segs) {
      makeSegmentPoints (segs);
      makePlanesFromSegments (myPoints);
   }

   /**
    * Returns the plane closest to a specified point in the D coordinate frame.
    * 
    * @param p
    * point to find nearest plane to
    */
   public Plane closestPlane (Point3d p) {
      int nearestIdx = -1;
      double dsqrMin = 0;

      // u will be a vector in the direction of the segment
      Vector3d u = new Vector3d();
      // v will be a vector from p to the first segment point
      Vector3d v = new Vector3d();

      for (int i = 0; i < myPlanes.size(); i++) {
         double dplane = myPlanes.get(i).distance (p);
         double dsqr = dplane * dplane;

         u.sub (myPoints.get (i + 1), myPoints.get(i));
         double segLength = u.norm();
         u.scale (1 / segLength);

         v.sub (myPoints.get(i), p);
         double vdotu = v.dot (u);
         if (vdotu > 0 && i > 0) {
            dsqr += (vdotu * vdotu);
         }
         else if (vdotu < -segLength && i < myPlanes.size() - 1) {
            dsqr += (vdotu + segLength) * (vdotu + segLength);
         }
         if (nearestIdx == -1 || dsqr < dsqrMin) {
            nearestIdx = i;
            dsqrMin = dsqr;
         }
      }
      return myPlanes.get (nearestIdx);
   }

   @Override
   public int maxUnilaterals() {
      return myUnilateral ? 1 : 0;
   }

   @Override
   public int numBilaterals() {
      return myUnilateral ? 0 : 1;
   }

   private Plane doProject (RigidTransform3d TGD, RigidTransform3d TCD) {
      myTmp.set (TCD.p);
      Plane plane = closestPlane (myTmp);
      plane.project (myTmp, myTmp);
      if (TGD != null) {
         TGD.p.set (myTmp); 
         TGD.R.set (TCD.R);
      }
      return plane;
   }

   @Override
   public void projectToConstraint (RigidTransform3d TGD, RigidTransform3d TCD) {
      doProject (TGD, TCD);
   }

   public void initializeConstraintInfo (ConstraintInfo[] info) {
      info[0].flags = LINEAR;
      if (!myUnilateral) {
         info[0].flags |= BILATERAL;
      }
   }

   @Override
   public void getConstraintInfo (
      ConstraintInfo[] info, RigidTransform3d TGD, RigidTransform3d TCD,
      RigidTransform3d XERR, boolean setEngaged) {
      
      myErr.set (XERR);
      Plane plane = doProject (null, TCD);
      myPnt.set (TCD.p);

      myNrm.inverseTransform (TGD, plane.normal);

      info[0].flags = LINEAR;
      if (!myUnilateral) {
         info[0].flags |= BILATERAL;
      }

      info[0].wrenchC.set (myNrm, Vector3d.ZERO);
      //double d = plane.distance (myPnt);
      double d = info[0].wrenchC.dot (myErr);

      info[0].distance = d;
      info[0].dotWrenchC.setZero();
      if (setEngaged) {
         if (myUnilateral && d < getContactDistance()) {
            info[0].engaged = 1;
         }
      }
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      for (int i = 0; i < myPoints.size(); i++) {
         myPoints.get(i).scale (s);
      }
      makePlanesFromSegments (myPoints);
   }


   public void transformGeometry (
      GeometryTransformer gt,  RigidTransform3d TFW, RigidTransform3d TDW) {
      
      // the points should be changed only by whatever cannot be accomodated
      // by the change to TDW. This is the same situation we have when we
      // transform a mesh that also contains a pose
      RigidTransform3d TDWnew = new RigidTransform3d(TDW);
      gt.transform (TDWnew);
      Vector3d del0 = new Vector3d();
      del0.set (myPoints.get(0));
      for (int i = 0; i < myPoints.size(); i++) {
         Point3d pnt = myPoints.get(i);
         pnt.transform (TDW);
         gt.transformPnt (pnt);
         pnt.inverseTransform (TDWnew);
      }
      makePlanesFromSegments (myPoints);
      
   }
   
   // /**
   //  * For planar couplings, we do not change F relative to A when D changes
   //  * relative to the world. This method hence becomes a noop.
   //  * 
   //  * @param XAW
   //  * new pose of body A in world coordinates
   //  * @param XBW
   //  * pose of body B in world coordinates
   //  */
   // public void updateXFA (RigidTransform3d XAW, RigidTransform3d XBW) {
   // }

}
