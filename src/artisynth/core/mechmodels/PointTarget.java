/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.util.*;

/**
 * Contains motion target information for a point.
 */
public class PointTarget extends MotionTarget {
   protected Point3d myPos;
   protected Vector3d myVel;

   public PointTarget (TargetActivity explicitActivity) {
      myPos = new Point3d();
      myVel = new Vector3d();
      setActivity (explicitActivity);
   }

   public void setTargetPos (Point3d pos) {
      maybeAddActivity (TargetActivity.Position);
      myPos.set (pos);
   }
   
   public Point3d getTargetPos (PointState state) {
      if (myActivity == TargetActivity.Position ||
          myActivity == TargetActivity.PositionVelocity) {
         return myPos;
      }
      else {
         return state.pos;
      }
   }

   public Point3d getTargetPos (Point3d posState) {
      if (myActivity == TargetActivity.Position ||
          myActivity == TargetActivity.PositionVelocity) {
         return myPos;
      }
      else {
         return posState;
      }
   }

   public void setTargetVel (Vector3d vel) {
      maybeAddActivity (TargetActivity.Velocity);
      myVel.set (vel);
   }

   public Vector3d getTargetVel (PointState state) {
      if (myActivity == TargetActivity.Velocity ||
          myActivity == TargetActivity.PositionVelocity) {
         return myVel;
      }
      else {
         return state.vel;
      }
   }
   
   public Vector3d getTargetVel (Vector3d velState) {
      if (myActivity == TargetActivity.Velocity ||
          myActivity == TargetActivity.PositionVelocity) {
         return myVel;
      }
      else {
         return velState;
      }
   }

   public void syncState (TargetActivity prev, PointState state) {
      if (!isPositionActive(prev) && isPositionActive(myActivity)) {
         myPos.set (state.pos);
      }
      if (!isVelocityActive(prev) && isVelocityActive(myActivity)) {
         myVel.set (state.vel);
      }
   }
   
   public void syncState (
      TargetActivity prev, Point3d posState, Vector3d velState) {
      if (!isPositionActive(prev) && isPositionActive(myActivity)) {
         myPos.set (posState);
      }
      if (!isVelocityActive(prev) && isVelocityActive(myActivity)) {
         myVel.set (velState);
      }
   }
   
   public void getTargetVel (
      Vector3d velt, double s, double h, PointState state) {
      getTargetVel (velt, s, h, state.pos, state.vel);
   }

   public void getTargetVel (
      Vector3d velt, double s, double h, Point3d posState, Vector3d velState) {

      switch (myActivity) {
         case Position: {
            // estimate target velocity from position
            velt.sub (myPos, posState);
            velt.scale (1/h);
            break;
         }
         case Velocity: {
            velt.combine (1-s, velState, s, myVel);
            break;
         }
         case PositionVelocity: {
            double a3, a2;

            a3 = 2*(posState.x - myPos.x) + (velState.x + myVel.x)*h;
            a2 = 3*(myPos.x - posState.x) - (2*velState.x + myVel.x)*h;
            velt.x = ((3*a3*s + 2*a2)*s)/h + velState.x;

            a3 = 2*(posState.y - myPos.y) + (velState.y + myVel.y)*h;
            a2 = 3*(myPos.y - posState.y) - (2*velState.y + myVel.y)*h;
            velt.y = ((3*a3*s + 2*a2)*s)/h + velState.y;

            a3 = 2*(posState.z - myPos.z) + (velState.z + myVel.z)*h;
            a2 = 3*(myPos.z - posState.z) - (2*velState.z + myVel.z)*h;
            velt.z = ((3*a3*s + 2*a2)*s)/h + velState.z;

            // velt.sub (posState, myPos);
            // velt.scale ( 6*(s-1)*s/h );
            // velt.scaledAdd ( ((3*s-4)*s+1), velState);
            // velt.scaledAdd ( (3*s-2)*s, myVel);

            break;
         }
         case Auto:
         case None: {
            velt.set (velState);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented interpolation mode " + myActivity);
         }
      }
   }
   
   public void getTargetPos (
      Vector3d velt, double s, double h, PointState state) {
      getTargetPos (velt, s, h, state.pos, state.vel);
   }
   
   public void getTargetPos (
      Vector3d post, double s, double h, Point3d posState, Vector3d velState) {

      switch (myActivity) {
         case Position: {
            post.combine (1-s, posState, s, myPos);
            break;
         }
         case Velocity: {
            // assume constant acceleration model
            post.combine (1-s/2, velState, s/2, myVel);
            post.scale (s*h);
            post.add (posState);
            break;
         }
         case PositionVelocity: {
            double a3, a2;

            a3 = 2*(posState.x - myPos.x) + (velState.x + myVel.x)*h;
            a2 = 3*(myPos.x - posState.x) - (2*velState.x + myVel.x)*h;
            post.x = ((a3*s + a2)*s + velState.x*h)*s + posState.x;

            a3 = 2*(posState.y - myPos.y) + (velState.y + myVel.y)*h;
            a2 = 3*(myPos.y - posState.y) - (2*velState.y + myVel.y)*h;
            post.y = ((a3*s + a2)*s + velState.y*h)*s + posState.y;

            a3 = 2*(posState.z - myPos.z) + (velState.z + myVel.z)*h;
            a2 = 3*(myPos.z - posState.z) - (2*velState.z + myVel.z)*h;
            post.z = ((a3*s + a2)*s + velState.z*h)*s + posState.z;

            // post.sub (posState, myPos);
            // post.scale ((2*s-3)*s*s);
            // post.scaledAdd ( ((s-2)*s+1)*s*h, velState);
            // post.scaledAdd ( (s-1)*s*s*h, myVel);
            // post.add (posState);
            break;
         }
         case None:
         case Auto: {
            post.set (posState);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented interpolation mode " + myActivity);
         }
      }
   }

   public int getTargetVel (
      double[] buf, double s, double h, PointState state, int idx) {
      return getTargetVel (buf, s, h, state.pos, state.vel, idx);
   }

   public int getTargetVel (
      double[] buf, double s, double h, 
      Point3d posState, Vector3d velState, int idx) {

      Vector3d tmp = new Vector3d();
      getTargetVel (tmp, s, h, posState, velState);
      buf[idx++] = tmp.x;
      buf[idx++] = tmp.y;
      buf[idx++] = tmp.z;
      return idx;
   }

   public int setTargetVel (double[] buf, int idx) {

      maybeAddActivity (TargetActivity.Velocity);
      myVel.x = buf[idx++];
      myVel.y = buf[idx++];
      myVel.z = buf[idx++];
      return idx;
   }

   public int getTargetPos (
      double[] buf, double s, double h, PointState state, int idx) {
      return getTargetPos (buf, s, h, state.pos, state.vel, idx);
   }

   public int getTargetPos (
      double[] buf, double s, double h, 
      Point3d posState, Vector3d velState, int idx) {

      Vector3d tmp = new Vector3d();
      getTargetPos (tmp, s, h, posState, velState);
      buf[idx++] = tmp.x;
      buf[idx++] = tmp.y;
      buf[idx++] = tmp.z;
      return idx;
   }

   public int setTargetPos (double[] buf, int idx) {

      maybeAddActivity (TargetActivity.Position);
      myPos.x = buf[idx++];
      myPos.y = buf[idx++];
      myPos.z = buf[idx++];
      return idx;
   }


}
