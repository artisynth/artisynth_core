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
public class MotionTarget {

   public enum TargetActivity {
      /**
       * Position target is active, velocity target is inactive
       */
      Position,

      /**
       * Velocity target is active, position target is inactive
       */
      Velocity,

      /**
       * Position and velocity targets are both active
       */
      PositionVelocity,

      /**
       * Position and velocity targets are both inactive
       */
      None,

      /**
       * Position and velocity targets are both initially inactive,
       * but will each be activated when a value is set for them
       */
      Auto
   };

   protected TargetActivity myActivity = TargetActivity.Auto;
   protected boolean myActivityExplicit = false;

   protected boolean isPositionActive (TargetActivity activity) {
      return (activity == TargetActivity.Position ||
              activity == TargetActivity.PositionVelocity);
   }

   public boolean isPositionActive () {
      return isPositionActive (myActivity);
   }

   protected boolean isVelocityActive (TargetActivity activity) {
      return (activity == TargetActivity.Velocity ||
              activity == TargetActivity.PositionVelocity);
   }

   public boolean isVelocityActive () {
      return isVelocityActive (myActivity);
   }

   public TargetActivity getActivity() {
      return myActivity;
   }

   public void setActivity (TargetActivity activity) {
      myActivity = activity;
      myActivityExplicit = (activity != TargetActivity.Auto);
   }

   protected boolean positionStateActivated (
      TargetActivity oldActivity, TargetActivity newActivity) {
      
      return ((oldActivity != TargetActivity.Position &&
               oldActivity != TargetActivity.PositionVelocity) &&
              (newActivity == TargetActivity.Position ||
               newActivity == TargetActivity.PositionVelocity));
   }

   protected void maybeAddActivity (TargetActivity activity) {
      if (!myActivityExplicit) {
         if (activity == TargetActivity.Position) {
            if (isVelocityActive (myActivity)) {
               myActivity = TargetActivity.PositionVelocity;
            }
            else {
               myActivity = TargetActivity.Position;
            }
         }
         else if (activity == TargetActivity.Velocity) {
            if (isPositionActive (myActivity)) {
               myActivity = TargetActivity.PositionVelocity;
            }
            else {
               myActivity = TargetActivity.Velocity;
            }
         }
      }
   }
}

