/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.mechmodels.MotionTarget.TargetActivity;
import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

/**
 * Contains motion target information for a point.
 */
public class FrameTargetTest extends UnitTest {

   FrameTarget target = new FrameTarget(null);
   FrameState state = new FrameState();

   void check (Vector res, Vector check) {
      if (!res.epsilonEquals (check, 1e-8)) {
         throw new TestException ("Expecting "+check+", got " + res);
      }
   }      

   void check (Vector res, double x, double y, double z) {
      VectorNd check = new VectorNd(res.size());
      check.set (0, x);
      check.set (1, y);
      check.set (2, z);
      check (res, check);
   }      

   public void test() {
      Vector3d pos = new Vector3d();
      Quaternion rot = new Quaternion();
      Twist vel = new Twist();

      target.myPos.set (2, 4, 6);
      target.myVel.set (0.5, 1, 1.5, 0.1, 0.2, 0.3);
      state.pos.set (1, 2, 3);
      state.setVelocity (Twist.ZERO);

      target.setActivity (TargetActivity.None);
      target.getTargetPos (pos, rot, 1, 1, state);
      check (pos, state.pos);
      check (rot, state.rot);
      target.getTargetVel (vel, 1, 1, state);
      check (vel, state.getVelocity());

      target.setActivity (TargetActivity.Position);
      target.getTargetPos (pos, rot, 1, 1, state);
      check (pos, target.myPos);
      check (rot, target.myRot);
      target.getTargetPos (pos, rot, 0, 1, state);
      check (pos, state.pos);
      check (rot, state.rot);
      target.getTargetPos (pos, rot, 0.5, 1, state);
      check (pos, 1.5, 3, 4.5);
      //check (rot, XXX);

      target.getTargetVel (vel, 1, 1, state);
      check (vel, 1, 2, 3);
      target.getTargetVel (vel, 0, 1, state);
      check (vel, 1, 2, 3);
      target.getTargetVel (vel, 0.5, 2, state);
      check (vel, 0.5, 1, 1.5);
 
      target.setActivity (TargetActivity.Velocity);

      target.getTargetVel (vel, 1, 1, state);
      check (vel, target.myVel);
      target.getTargetVel (vel, 0, 1, state);
      check (vel, state.getVelocity());
      target.getTargetVel (vel, 0.5, 1, state);
      check (vel.v, 0.25, 0.5, 0.75);
      //check (vel.w, xxx);

      state.setVelocity (new Twist (0.5, -0.5, 0.5, 0.05, -0.01, 0.02));

      target.getTargetPos (pos, rot, 1, /*h=*/0.5, state);
      check (pos, 1.25, 2.125, 3.5);
      //check (rot, XXX);
      target.getTargetPos (pos, rot, 0, /*h=*/0.5, state);
      check (pos, state.pos);
      check (rot, state.rot);
      target.getTargetPos (pos, rot, 0.5, /*h=*/0.5, state);
      check (pos, 1.125, 1.96875, 3.1875);
      //check (rot, XXX);

      target.setActivity (TargetActivity.PositionVelocity);

      target.getTargetPos (pos, rot, 1, /*h=*/0.5, state);
      check (pos, target.myPos);
      check (rot, target.myRot);
      target.getTargetPos (pos, rot, 0, /*h=*/0.5, state);
      check (pos, state.pos);
      check (rot, state.rot);
      target.getTargetPos (pos, rot, 0.5, /*h=*/0.5, state);
      check (pos, 1.5, 2.90625, 4.4375);
      //check (rot, XXX);
      target.getTargetPos (pos, rot, 0.25, /*h=*/0.5, state);
      check (pos, 1.1796875, 2.25390625, 3.46875);
      //check (rot, XXX);

      target.getTargetVel (vel, 1, /*h=*/0.5, state);
      check (vel, target.myVel);
      target.getTargetVel (vel, 0, /*h=*/0.5, state);
      check (vel, state.getVelocity());
      target.getTargetVel (vel, 0.5, /*h=*/0.5, state);
      check (vel.v, 2.75, 5.875, 8.5);
      //check (vel.w, xxx);
      target.getTargetVel (vel, 0.25, /*h=*/0.5, state);
      check (vel.v, 2.1875, 4.09375, 6.375);
      //check (vel.w, xxx);

  }

   public static void main (String[] args) {
      FrameTargetTest tester = new FrameTargetTest();
      tester.runtest();
   }
}
