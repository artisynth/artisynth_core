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

/**
 * Contains motion target information for a point.
 */
public class PointTargetTest extends UnitTest {

   PointTarget target = new PointTarget(null);
   PointState state = new PointState();

   void check (Vector3d res, Vector3d check) {
      if (!res.epsilonEquals (check, 1e-8)) {
         throw new TestException ("Expecting "+check+", got " + res);
      }
   }      

   void check (Vector3d res, double x, double y, double z) {
      check (res, new Vector3d (x, y, z));
   }      

   public void test() {
      Vector3d res = new Vector3d();
      target.myPos.set (2, 4, 6);
      target.myVel.set (0.5, 1, 1.5);
      state.pos.set (1, 2, 3);
      state.vel.set (0, 0, 0);

      target.setActivity (TargetActivity.None);
      target.getTargetPos (res, 1, 1, state);
      check (res, state.pos);
      target.getTargetVel (res, 1, 1, state);
      check (res, state.vel);

      target.setActivity (TargetActivity.Position);
      target.getTargetPos (res, 1, 1, state);
      check (res, target.myPos);
      target.getTargetPos (res, 0, 1, state);
      check (res, state.pos);
      target.getTargetPos (res, 0.5, 1, state);
      check (res, 1.5, 3, 4.5);

      target.getTargetVel (res, 1, 1, state);
      check (res, 1, 2, 3);
      target.getTargetVel (res, 0, 1, state);
      check (res, 1, 2, 3);
      target.getTargetVel (res, 0.5, 2, state);
      check (res, 0.5, 1, 1.5);
 
      target.setActivity (TargetActivity.Velocity);

      target.getTargetVel (res, 1, 1, state);
      check (res, target.myVel);
      target.getTargetVel (res, 0, 1, state);
      check (res, state.vel);
      target.getTargetVel (res, 0.5, 1, state);
      check (res, 0.25, 0.5, 0.75);

      state.vel.set (0.5, -0.5, 0.5);

      target.getTargetPos (res, 1, /*h=*/0.5, state);
      check (res, 1.25, 2.125, 3.5);
      target.getTargetPos (res, 0, /*h=*/0.5, state);
      check (res, state.pos);
      target.getTargetPos (res, 0.5, /*h=*/0.5, state);
      check (res, 1.125, 1.96875, 3.1875);

      target.setActivity (TargetActivity.PositionVelocity);

      target.getTargetPos (res, 1, /*h=*/0.5, state);
      check (res, target.myPos);
      target.getTargetPos (res, 0, /*h=*/0.5, state);
      check (res, state.pos);
      target.getTargetPos (res, 0.5, /*h=*/0.5, state);
      check (res, 1.5, 2.90625, 4.4375);
      target.getTargetPos (res, 0.25, /*h=*/0.5, state);
      check (res, 1.1796875, 2.25390625, 3.46875);

      target.getTargetVel (res, 1, /*h=*/0.5, state);
      check (res, target.myVel);
      target.getTargetVel (res, 0, /*h=*/0.5, state);
      check (res, state.vel);
      target.getTargetVel (res, 0.5, /*h=*/0.5, state);
      check (res, 2.75, 5.875, 8.5);
      target.getTargetVel (res, 0.25, /*h=*/0.5, state);
      check (res, 2.1875, 4.09375, 6.375);

  }

   public static void main (String[] args) {
      PointTargetTest tester = new PointTargetTest();
      tester.runtest();
   }
}
