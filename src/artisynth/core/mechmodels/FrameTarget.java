/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.mechmodels.MotionTarget.TargetActivity;
import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.util.*;

/**
 * Contains motion target information for a point.
 */
public class FrameTarget extends MotionTarget {
   protected Point3d myPos;
   protected Quaternion myRot;
   protected Twist myVel;

   protected Twist myTmpVel;
   protected Vector3d myTmpVec;
   protected Quaternion myTmpRot;

   public FrameTarget (TargetActivity explicitActivity) {
      myPos = new Point3d();
      myRot = new Quaternion();
      myVel = new Twist();

      myTmpRot = new Quaternion();
      myTmpVec = new Vector3d();
      myTmpVel = new Twist();

      setActivity (explicitActivity);
   }

   protected void rotDerivative (Quaternion qvel) {
      //
      // For w in base coordinates, vel = 1/2 (0, w) rot
      //
      Vector3d w = myVel.w;

      qvel.s = -0.5 * w.dot (myRot.u);
      qvel.u.cross (w, myRot.u);
      qvel.u.scaledAdd (myRot.s, w, qvel.u);
      qvel.u.scale (0.5);
   }

   public void setTargetPos (Vector3d pos) {
      maybeAddActivity (TargetActivity.Position);
      myPos.set (pos);
   }

   public Point3d getTargetPos (FrameState state) {
      if (myActivity == TargetActivity.Position ||
          myActivity == TargetActivity.PositionVelocity) {
         return myPos;
      }
      else {
         return state.pos;
      }
   }     
      
   public void setTargetRot (Quaternion rot) {
      maybeAddActivity (TargetActivity.Position);
      myRot.set (rot);
   }
      
   public void setTargetRot (AxisAngle axisAng) {
      maybeAddActivity (TargetActivity.Position);
      myRot.setAxisAngle (axisAng);
   }
      
   public void setTargetRot (RotationMatrix3d R) {
      maybeAddActivity (TargetActivity.Position);
      myRot.set (R);
   }
      
   public Quaternion getTargetRot (FrameState state) {
      if (myActivity == TargetActivity.Position ||
          myActivity == TargetActivity.PositionVelocity) {
         return myRot;
      }
      else {
         return state.rot;
      }
   }     
      
   public AxisAngle getTargetAxisAngle (FrameState state) {
      if (myActivity == TargetActivity.Position ||
          myActivity == TargetActivity.PositionVelocity) {
         AxisAngle axisAng = new AxisAngle();
         myRot.getAxisAngle (axisAng);
         return axisAng;
      }
      else {
         return state.getAxisAngle();
      }
   }     

   public RigidTransform3d getTargetPose (FrameState state) {
      if (myActivity == TargetActivity.Position ||
          myActivity == TargetActivity.PositionVelocity) {
         RigidTransform3d X = new RigidTransform3d();
         X.p.set (myPos);
         X.R.set (myRot);
         return X;
      }
      else {
         return state.XFrameToWorld;
      }
   }     
      
   public void setTargetVel (Twist vel) {
      maybeAddActivity (TargetActivity.Velocity);
      myVel.set (vel);
   }

   public Twist getTargetVel (FrameState state) {
      if (myActivity == TargetActivity.Velocity ||
          myActivity == TargetActivity.PositionVelocity) {
         return myVel;
      }
      else {
         return state.getVelocity();
      }
   }

   public void syncState (TargetActivity prev, FrameState state) {

      if (!isPositionActive (prev) && isPositionActive (myActivity)) {
         myPos.set (state.pos);
         myRot.set (state.rot);
      }
      if (!isVelocityActive (prev) && isVelocityActive (myActivity)) {
         myVel.set (state.getVelocity());
      }
   }

   public void getTargetVel (
      Twist velt, double s, double h, FrameState state) {

      switch (myActivity) {
         case Position: {
            // estimate target velocity from position
            velt.v.sub (myPos, state.pos);
            velt.v.scale (1/h);
            myTmpRot.mulInverseLeft (state.rot, myRot);
            myTmpRot.log (velt.w);
            velt.w.scale (2/h);
            // transform velocity to world coords:
            state.rot.transform (velt.w, velt.w);
            break;
         }
         case Velocity: {
            if (s == 0) {
               velt.set (state.getVelocity());
            }
            else if (s == 1) {
               velt.set (myVel);
            }
            else {
               velt.combine (1-s, state.getVelocity(), s, myVel);
            }                  
            break;
         }
         case PositionVelocity: {
            if (s == 0) {
               velt.set (state.getVelocity());
            }
            else if (s == 1) {
               velt.set (myVel);
            }
            else {
               Twist stateVel = state.getVelocity();
               Vector3d.hermiteVelocity (
                  velt.v, state.pos, stateVel.v, myPos, myVel.v, s, h);
               Quaternion.sphericalHermiteGlobal (
                  myTmpRot, velt.w, state.rot, stateVel.w, myRot, myVel.w, s, h);
            }
            break;
         }
         case Auto:
         case None: {
            velt.set (state.getVelocity());
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented interpolation mode " + myActivity);
         }
      }
   }

   public void getTargetPos (
      Vector3d post, Quaternion rott, double s, double h, FrameState state) {

      switch (myActivity) {
         case Position: {
            if (s == 0) {
               post.set (state.pos);
               rott.set (state.rot);
            }
            else if (s == 1) {
               post.set (myPos);
               rott.set (myRot);
            }
            else {
               post.combine (1-s, state.pos, s, myPos);
               rott.sphericalInterpolate (state.rot, s, myRot);
            }
            break;
         }
         case Velocity: {
            // assume constant acceleration model
            if (s == 0) {
               post.set (state.pos);
               rott.set (state.rot);
            }
            else {
               Twist stateVel = state.getVelocity();
               post.combine (1-s/2, stateVel.v, s/2, myVel.v);
               post.scale (s*h);
               post.add (state.pos);
               // this is a hack: estimate an appropriate average velocity for the
               // value of s, then use that to drive toward the target position.
               Vector3d vec = new Vector3d();
               vec.combine (1-s/2, stateVel.w, s/2, myVel.w);
               vec.scale (h/2);
               rott.setExp (s, vec);
               rott.mul (rott, state.rot);
            }
            break;
         }
         case PositionVelocity: {
            if (s == 0) {
               post.set (state.pos);
               rott.set (state.rot);
            }
            else if (s == 1) {
               post.set (myPos);
               rott.set (myRot);
            }
            else {
               Twist stateVel = state.getVelocity();
               Vector3d.hermiteInterpolate (
                  post, state.pos, stateVel.v, myPos, myVel.v, s, h);
               Quaternion.sphericalHermiteGlobal (
                  rott, null, state.rot, stateVel.w, myRot, myVel.w, s, h);
            }
            break;
         }
         case None:
         case Auto: {
            post.set (state.pos);
            rott.set (state.rot);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented interpolation mode " + myActivity);
         }
      }
   }

   public int getTargetVel (
      double[] buf, double s, double h, FrameState state, int idx) {

      getTargetVel (myTmpVel, s, h, state);
      // map velocity to body coordinates
      myTmpVel.inverseTransform (state.XFrameToWorld.R);
      buf[idx++] = myTmpVel.v.x;
      buf[idx++] = myTmpVel.v.y;
      buf[idx++] = myTmpVel.v.z;
      buf[idx++] = myTmpVel.w.x;
      buf[idx++] = myTmpVel.w.y;
      buf[idx++] = myTmpVel.w.z;
      return idx;
   }

   public int setTargetVel (double[] buf, FrameState state, int idx) {

      maybeAddActivity (TargetActivity.Velocity);

      myVel.v.x = buf[idx++];
      myVel.v.y = buf[idx++];
      myVel.v.z = buf[idx++];
      myVel.w.x = buf[idx++];
      myVel.w.y = buf[idx++];
      myVel.w.z = buf[idx++];
      // map from body to rotated world coordinates
      myVel.transform (state.XFrameToWorld.R);
      return idx;
   }

   public int getTargetPos (
      double[] buf, double s, double h, FrameState state, int idx) {

      getTargetPos (myTmpVec, myTmpRot, s, h, state);
      buf[idx++] = myTmpVec.x;
      buf[idx++] = myTmpVec.y;
      buf[idx++] = myTmpVec.z;
      buf[idx++] = myTmpRot.s;
      buf[idx++] = myTmpRot.u.x;
      buf[idx++] = myTmpRot.u.y;
      buf[idx++] = myTmpRot.u.z;
      return idx;
   }

   public int setTargetPos (double[] buf, int idx) {

      maybeAddActivity (TargetActivity.Position);

      myPos.x = buf[idx++];
      myPos.y = buf[idx++];
      myPos.z = buf[idx++];
      myRot.s = buf[idx++];
      myRot.u.x = buf[idx++];
      myRot.u.y = buf[idx++];
      myRot.u.z = buf[idx++];
      return idx;
   }

}
