/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

class TestBody {
   SpatialInertia myInertia;
   RigidTransform3d myPose;
   Wrench myCoriolisForce;
   Quaternion myQuat;
   Quaternion myQvel;
   Twist myVel;
   Twist myTmpVel;
   int myIndex;
   Point3d mySphereCenter;
   double mySphereRadius;

   TestBody() {
      myInertia = new SpatialInertia();
      myPose = new RigidTransform3d();
      myCoriolisForce = new Wrench();
      myVel = new Twist();
      myTmpVel = new Twist();
      myQuat = new Quaternion();
      myQvel = new Quaternion();
   }

   void setPose (RigidTransform3d X) {
      myPose.set (X);
   }

   void getPose (RigidTransform3d X) {
      X.set (myPose);
   }

   RigidTransform3d getPose() {
      return myPose;
   }

   void getBodyVel (Twist vel) {
      vel.inverseTransform (myPose.R, myVel);
   }

   void setBodyVel (Twist vel) {
      myVel.transform (myPose.R, vel);
   }

   void getVel (Twist vel) {
      vel.set (myVel);
   }

   void setInertia (SpatialInertia X) {
      myInertia.set (X);
   }

   SpatialInertia getInertia() {
      return myInertia;
   }

   void extrapolatePose (Twist vel, int h) {
      vel.extrapolateTransform (myPose, h);
   }

   void advancePose (double h, Twist vel) {
      // vel is a velocity in body coordinates. Convert to world coordinates
      myTmpVel.transform (myPose.R, vel);

      myPose.p.scaledAdd (h, myTmpVel.v, myPose.p);

      myQuat.set (myPose.R);
      Vector3d w = myTmpVel.w;

      //
      // For w in world coordinates, vel = 1/2 (0, w) rot
      //

      myQvel.s = -0.5 * w.dot (myQuat.u);
      myQvel.u.cross (w, myQuat.u);
      myQvel.u.scaledAdd (myQuat.s, w, myQvel.u);
      myQvel.u.scale (0.5);

      myQuat.scaledAdd (h, myQvel, myQuat);
      myQuat.normalize();
      myPose.R.set (myQuat);
   }

   int getIndex() {
      return myIndex;
   }

   void setIndex (int idx) {
      myIndex = idx;
   }

   void computeBodyForce (Wrench wr) {
      Point3d com = new Point3d();
      myInertia.getCenterOfMass (com);
      wr.f.set (0, 0, -9.8 * myInertia.getMass());
      wr.f.inverseTransform (myPose);
      wr.m.cross (com, wr.f);

      Twist bodyVel = new Twist();
      bodyVel.inverseTransform (myPose.R, myVel);

      myInertia.coriolisForce (myCoriolisForce, bodyVel);
      wr.add (myCoriolisForce);
   }

   void setSphere (double radius, Point3d pos) {
      if (pos != null && radius > 0) {
         mySphereRadius = radius;
         mySphereCenter = new Point3d (pos);
      }
      else {
         mySphereCenter = null;
         mySphereRadius = 0;
      }
   }

   boolean hasSphere() {
      return mySphereCenter != null;
   }

   double getSphereRadius() {
      return mySphereRadius;
   }

   void getSpherePosition (Point3d pos) {
      pos.transform (myPose, mySphereCenter);
   }

   double getSphereDistance (TestBody bodyB) {
      return getSphereContact (null, null, bodyB);
   }

   double getSphereContact (Point3d pnt, Vector3d nrm, TestBody bodyB) {
      if (!hasSphere() || !bodyB.hasSphere()) {
         throw new IllegalArgumentException (
            "one or both bodies does not have a sphere");
      }
      Point3d centerA = new Point3d();
      Point3d centerB = new Point3d();
      getSpherePosition (centerA);
      bodyB.getSpherePosition (centerB);
      Vector3d vecBA = new Vector3d();

      vecBA.sub (centerA, centerB);
      double dist = vecBA.norm();
      if (pnt != null) {
         vecBA.scale (1 / dist);
         nrm.inverseTransform (myPose, vecBA);
         pnt.scaledAdd (-mySphereRadius, nrm, mySphereCenter);
         Vector3d normal = new Vector3d();
         normal.transform (myPose, nrm);
         Point3d point = new Point3d();
         point.transform (myPose, pnt);
      }
      return dist - mySphereRadius - bodyB.mySphereRadius;
   }

   double getPlaneDistance (Plane plane) {
      return getPlaneContact (null, null, plane);
   }

   double getPlaneContact (Point3d pnt, Vector3d nrm, Plane plane) {
      if (!hasSphere()) {
         throw new IllegalArgumentException ("body does not have a sphere");
      }
      Point3d centerA = new Point3d();
      getSpherePosition (centerA);
      double dist = plane.distance (centerA);
      if (pnt != null) {
         nrm.inverseTransform (myPose, plane.getNormal());
         pnt.scaledAdd (-mySphereRadius, nrm, mySphereCenter);
      }
      return dist - mySphereRadius;
   }

}
