/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import artisynth.core.util.ScalableUnits;
import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

public class FrameState implements ScalableUnits {
   Point3d pos;
   Quaternion rot;
   Twist vel; // spatial velocity in world coordinates

   RigidTransform3d XFrameToWorld;

   public FrameState() {
      pos = new Point3d();
      rot = new Quaternion (1, 0, 0, 0);
      vel = new Twist();
      XFrameToWorld = new RigidTransform3d();
   }

   public void set (FrameState state) {
      pos.set (state.pos);
      rot.set (state.rot);
      vel.set (state.vel);
      updatePose();
   }

   public int set (VectorNd x, int idx) {
      double[] buf = x.getBuffer();
      pos.x = buf[idx++];
      pos.y = buf[idx++];
      pos.z = buf[idx++];
      rot.s = buf[idx++];
      rot.u.x = buf[idx++];
      rot.u.y = buf[idx++];
      rot.u.z = buf[idx++];
      vel.v.x = buf[idx++];
      vel.v.y = buf[idx++];
      vel.v.z = buf[idx++];
      vel.w.x = buf[idx++];
      vel.w.y = buf[idx++];
      vel.w.z = buf[idx++];
      updatePose();
      return idx;
   }

   public int get (VectorNd x, int idx) {
      double[] buf = x.getBuffer();
      buf[idx++] = pos.x;
      buf[idx++] = pos.y;
      buf[idx++] = pos.z;
      buf[idx++] = rot.s;
      buf[idx++] = rot.u.x;
      buf[idx++] = rot.u.y;
      buf[idx++] = rot.u.z;
      buf[idx++] = vel.v.x;
      buf[idx++] = vel.v.y;
      buf[idx++] = vel.v.z;
      buf[idx++] = vel.w.x;
      buf[idx++] = vel.w.y;
      buf[idx++] = vel.w.z;
      return idx;
   }

   public void setPose (RigidTransform3d X) {
      XFrameToWorld.set (X);
      rot.set (X.R);
      pos.set (X.p);
      // Update pose since pos and rot are the fundamental state variables
      updatePose();
   }

   /**
    * Adjusts the pose of this FrameState by an increment specified as a twist
    * delx. The increment is specified in the frame of the pose itself. In other
    * words, if the frame pose is X, and dX is the rigid transform associated
    * with delx, then this routine computes
    * 
    * <pre>
    *   X = X dX
    * </pre>
    * 
    * @param delx
    * incremental displacement
    */
   public void adjustPose (Twist delx) {
      delx.extrapolateTransform (XFrameToWorld, 1);
      rot.set (XFrameToWorld.R);
      pos.set (XFrameToWorld.p);
      // Update pose since pos and rot are the fundamental state variables
      updatePose();
   }

   /**
    * Adjusts the pose of this FrameState by a velocity (in Frame coordinates)
    * applied over a time interval h. In other words, if the frame pose is X,
    * and vel is a twist specifying the velocity, then this routine computes
    * 
    * <pre>
    *  X = X vel h
    * </pre>
    * 
    * @param vel
    * velocity in frame coordinates
    * @param h
    * time interval over which to apply the velocity
    */
   public void adjustPose (Twist vel, double h) {
      vel.extrapolateTransform (XFrameToWorld, h);
      rot.set (XFrameToWorld.R);
      rot.normalize();
      pos.set (XFrameToWorld.p);
      // Update pose since pos and rot are the fundamental state variables
      updatePose();
   }

   public void updatePose() {
      rot.normalize();
      XFrameToWorld.p.set (pos);
      XFrameToWorld.R.set (rot);
   }

   public RigidTransform3d getPose() {
      return XFrameToWorld;
   }

   public void getPose (RigidTransform3d X) {
      X.set (XFrameToWorld);
   }

   public Point3d getPosition() {
      return pos;
   }

   public void setPosition (Point3d p) {
      pos.set (p);
      XFrameToWorld.p.set (p);
   }

   public Quaternion getRotation () {
      return rot;
   }

   public void setRotation (Quaternion q) {
      rot.set (q);
      updatePose();
   }

   /**
    * Sets the velocity of the frame in world coordinates.
    * 
    * @param v
    * spatial velocity
    */
   public void setVelocity (Twist v) {
      vel.set (v);
   }

   /**
    * Adds a velocity to this frame's existing velocity.
    * 
    * @param v
    * spatial velocity to add
    */
   public void addVelocity (Twist v) {
      vel.add (v);
   }

   /**
    * Adds a scaled velocity to this frame's existing velocity.
    * 
    * @param s
    * scale factor
    * @param v
    * spatial velocity to add
    */
   public void addScaledVelocity (double s, Twist v) {
      vel.scaledAdd (s, v);
   }

   /**
    * Returns the velocity of the frame in world coordinates.
    *
    * @return
    * frame velocity in world coordinates (should not be modified)
    */
   public Twist getVelocity () {
      return vel;
   }

   /**
    * Gets the velocity of the frame in world coordinates.
    * 
    * @param v
    * returns the spatial velocity
    */
   public void getVelocity (Twist v) {
      v.set (vel);
   }

   /**
    * Gets the velocity of the frame in body coordinates.
    * 
    * @param v
    * returns the spatial velocity
    */
   public void getBodyVelocity (Twist v) {
      v.inverseTransform (XFrameToWorld.R, vel);
   }

   /**
    * Sets the velocity of the frame in body coordinates.
    * 
    * @param v
    * spatial velocity
    */
   public void setBodyVelocity (Twist v) {
      vel.transform (XFrameToWorld.R, v);
   }

   /**
    * Computes the time derivative of qrot given the current value of w.
    * 
    * @param qvel
    * returns the derivative of qrot
    */
   public void rotDerivative (Quaternion qvel) {
      //
      // For w in base coordinates, vel = 1/2 (0, w) rot
      //
      Vector3d w = vel.w;

      qvel.s = -0.5 * w.dot (rot.u);
      qvel.u.cross (w, rot.u);
      qvel.u.scaledAdd (rot.s, w, qvel.u);
      qvel.u.scale (0.5);
   }


   public int getPos (double[] buf, int idx) {
      buf[idx++] = pos.x;
      buf[idx++] = pos.y;
      buf[idx++] = pos.z;
      buf[idx++] = rot.s;
      buf[idx++] = rot.u.x;
      buf[idx++] = rot.u.y;
      buf[idx++] = rot.u.z;
      return idx;
   }

   public AxisAngle getAxisAngle() {
      AxisAngle axisAng = new AxisAngle();
      XFrameToWorld.R.getAxisAngle (axisAng);
      return axisAng;
   }

   public int getVel (double[] buf, int idx) {
      buf[idx++] = vel.v.x;
      buf[idx++] = vel.v.y;
      buf[idx++] = vel.v.z;
      buf[idx++] = vel.w.x;
      buf[idx++] = vel.w.y;
      buf[idx++] = vel.w.z;
      return idx;
   }

   public int getBodyVel (double[] buf, int idx, Twist bodyVel) {
      bodyVel.inverseTransform (XFrameToWorld.R, vel);
      buf[idx++] = bodyVel.v.x;
      buf[idx++] = bodyVel.v.y;
      buf[idx++] = bodyVel.v.z;
      buf[idx++] = bodyVel.w.x;
      buf[idx++] = bodyVel.w.y;
      buf[idx++] = bodyVel.w.z;
      return idx;
   }

   public int setPos (double[] buf, int idx) {
      pos.x = buf[idx++];
      pos.y = buf[idx++];
      pos.z = buf[idx++];
      rot.s = buf[idx++];
      rot.u.x = buf[idx++];
      rot.u.y = buf[idx++];
      rot.u.z = buf[idx++];
      updatePose();
      return idx;
   }

   public int setVel (double[] buf, int idx) {
      vel.v.x = buf[idx++];
      vel.v.y = buf[idx++];
      vel.v.z = buf[idx++];
      vel.w.x = buf[idx++];
      vel.w.y = buf[idx++];
      vel.w.z = buf[idx++];
      return idx;
   }

   public int setBodyVel (double[] buf, int idx) {
      vel.v.x = buf[idx++];
      vel.v.y = buf[idx++];
      vel.v.z = buf[idx++];
      vel.w.x = buf[idx++];
      vel.w.y = buf[idx++];
      vel.w.z = buf[idx++];
      vel.transform (XFrameToWorld.R);
      return idx;
   }

//   public ComponentState duplicate() {
//      return new FrameState();
//   }

   public String toString() {
      return (pos.toString() + " " + rot.toString() + " " + vel.toString());
   }

   public String toString (NumberFormat fmt) {
      return (pos.toString (fmt) + " " +
              rot.toString (fmt) + " " + vel.toString (fmt));
   }

   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }

   public void scaleDistance (double s) {
      pos.scale (s);
      vel.v.scale (s);
      updatePose();
   }

   public void scaleMass (double s) {
   }

   public boolean equals (ComponentState state) {
      if (state instanceof FrameState) {
         FrameState otherState = (FrameState)state;
         return (pos.equals (otherState.pos) || 
                 rot.equals (otherState.rot) || 
                 vel.equals (otherState.vel));
      }
      else {
         return false;
      }
   }            

}
