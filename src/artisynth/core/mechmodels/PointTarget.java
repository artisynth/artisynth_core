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
import maspack.util.*;

/**
 * Contains motion target information for a point.
 */
public class PointTarget extends MotionTarget {
   protected Point3d myPos;
   protected Vector3d myVel;
   protected Vector3d myTmp;

   public PointTarget (TargetActivity explicitActivity) {
      myPos = new Point3d();
      myVel = new Vector3d();
      myTmp = new Vector3d();
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

   public void syncState (TargetActivity prev, PointState state) {
      if (!isPositionActive(prev) && isPositionActive(myActivity)) {
         myPos.set (state.pos);
      }
      if (!isVelocityActive(prev) && isVelocityActive(myActivity)) {
         myVel.set (state.vel);
      }
   }

   public void getTargetVel (
      Vector3d velt, double s, double h, PointState state) {

      switch (myActivity) {
         case Position: {
            // estimate target velocity from position
            velt.sub (myPos, state.pos);
            velt.scale (1/h);
            break;
         }
         case Velocity: {
            velt.combine (1-s, state.vel, s, myVel);
            break;
         }
         case PositionVelocity: {
            double a3, a2;

            a3 = 2*(state.pos.x - myPos.x) + (state.vel.x + myVel.x)*h;
            a2 = 3*(myPos.x - state.pos.x) - (2*state.vel.x + myVel.x)*h;
            velt.x = ((3*a3*s + 2*a2)*s)/h + state.vel.x;

            a3 = 2*(state.pos.y - myPos.y) + (state.vel.y + myVel.y)*h;
            a2 = 3*(myPos.y - state.pos.y) - (2*state.vel.y + myVel.y)*h;
            velt.y = ((3*a3*s + 2*a2)*s)/h + state.vel.y;

            a3 = 2*(state.pos.z - myPos.z) + (state.vel.z + myVel.z)*h;
            a2 = 3*(myPos.z - state.pos.z) - (2*state.vel.z + myVel.z)*h;
            velt.z = ((3*a3*s + 2*a2)*s)/h + state.vel.z;

            // velt.sub (state.pos, myPos);
            // velt.scale ( 6*(s-1)*s/h );
            // velt.scaledAdd ( ((3*s-4)*s+1), state.vel);
            // velt.scaledAdd ( (3*s-2)*s, myVel);

            break;
         }
         case Auto:
         case None: {
            velt.set (state.vel);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented interpolation mode " + myActivity);
         }
      }
   }

   public void getTargetPos (
      Vector3d post, double s, double h, PointState state) {

      switch (myActivity) {
         case Position: {
            post.combine (1-s, state.pos, s, myPos);
            break;
         }
         case Velocity: {
            // assume constant acceleration model
            post.combine (1-s/2, state.vel, s/2, myVel);
            post.scale (s*h);
            post.add (state.pos);
            break;
         }
         case PositionVelocity: {
            double a3, a2;

            a3 = 2*(state.pos.x - myPos.x) + (state.vel.x + myVel.x)*h;
            a2 = 3*(myPos.x - state.pos.x) - (2*state.vel.x + myVel.x)*h;
            post.x = ((a3*s + a2)*s + state.vel.x*h)*s + state.pos.x;

            a3 = 2*(state.pos.y - myPos.y) + (state.vel.y + myVel.y)*h;
            a2 = 3*(myPos.y - state.pos.y) - (2*state.vel.y + myVel.y)*h;
            post.y = ((a3*s + a2)*s + state.vel.y*h)*s + state.pos.y;

            a3 = 2*(state.pos.z - myPos.z) + (state.vel.z + myVel.z)*h;
            a2 = 3*(myPos.z - state.pos.z) - (2*state.vel.z + myVel.z)*h;
            post.z = ((a3*s + a2)*s + state.vel.z*h)*s + state.pos.z;

            // post.sub (state.pos, myPos);
            // post.scale ((2*s-3)*s*s);
            // post.scaledAdd ( ((s-2)*s+1)*s*h, state.vel);
            // post.scaledAdd ( (s-1)*s*s*h, myVel);
            // post.add (state.pos);
            break;
         }
         case None:
         case Auto: {
            post.set (state.pos);
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

      getTargetVel (myTmp, s, h, state);
      buf[idx++] = myTmp.x;
      buf[idx++] = myTmp.y;
      buf[idx++] = myTmp.z;
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

      getTargetPos (myTmp, s, h, state);
      buf[idx++] = myTmp.x;
      buf[idx++] = myTmp.y;
      buf[idx++] = myTmp.z;
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
