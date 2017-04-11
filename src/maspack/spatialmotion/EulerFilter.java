/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

public class EulerFilter {

   private static int FLIP_FLAG = 0x0001;
   private static int SHIFT_FLAG = 0x0002;

   /**
    * Performs an Euler filter to prevent artifacts from gimbal lock. Out of all
    * possible solutions, finds the one that is closest to the reference set of
    * angles.
    * 
    * @param ref
    * Reference set of angles {roll, pitch, yaw}
    * @param rpy
    * Current rpy angles to adjust {roll, pitch, yaw}
    * @param eps
    * Threshold to deal with singularity at (yaw - k*Pi/2) {@code <} eps
    * @param out
    * Adjusted rpy angles
    * @return Flag consisting of potentially FLIP_FLAG if angles flipped, or
    * SHIFT_FLAG if yaw/roll shifted
    */
   public static int filter(double[] ref, double[] rpy, double eps,
      double[] out) {

      // correct for full rotations
      int result = 0;

      double roll = findNearestAngle(ref[0], rpy[0]);
      double pitch = findNearestAngle(ref[1], rpy[1]);
      double yaw = findNearestAngle(ref[2], rpy[2]);

      // check if degenerate, re-adjust yaw and roll to be more like original
      if (Math.abs(Math.cos(pitch)) < eps) {

         int c = 1;
         if (Math.sin(pitch) > 0) {
            c = -1;
         }
         double a = yaw + c * roll;
         out[2] = (ref[2] + a - c * ref[0]) / 2;
         out[1] = pitch;
         out[0] = c * (a - out[2]);
         
         return SHIFT_FLAG;

      } else {

         // check the two alternatives
         double d1 = (roll - ref[0]) * (roll - ref[0])
            + (pitch - ref[1]) * (pitch - ref[1])
            + (yaw - ref[2]) * (yaw - ref[2]);

         out[0] = findNearestAngle(ref[0], roll + Math.PI);
         double nearestSingularPitch;
         //out[1] = findNearestAngle(ref[1], Math.PI / 2 - pitch);
         if (Math.sin (pitch) > 0) {
            nearestSingularPitch = findNearestAngle (pitch, Math.PI/2);
         }
         else {
            nearestSingularPitch = findNearestAngle (pitch, -Math.PI/2);
         }
         out[1] = findNearestAngle(ref[1], 2*nearestSingularPitch - pitch);
         out[2] = findNearestAngle(ref[2], yaw - Math.PI);

         double d2 = (out[0] - ref[0]) * (out[0] - ref[0])
            + (out[1] - ref[1]) * (out[1] - ref[1])
            + (out[2] - ref[1]) * (out[2] - ref[2]);
         
         if (d1 < d2) {
            out[0] = roll;
            out[1] = pitch;
            out[2] = yaw;
         } else {
            result = FLIP_FLAG;
         }
         
      }

      return result;

   }

   /**
    * Given an angle <code>ang</code>, find an equivalent angle that is within
    * +/- PI of a given reference angle <code>ref</code>.
    * 
    * @param ref
    * reference angle (radians)
    * @param ang
    * initial angle (radians)
    * @return angle equivalent to <code>ang</code> within +/- PI of
    * <code>ref</code>.
    */
   public static double findNearestAngle(double ref, double ang) {
      while (ang - ref > Math.PI) {
         ang -= 2 * Math.PI;
      }
      while (ang - ref < -Math.PI) {
         ang += 2 * Math.PI;
      }
      return ang;
   }

   public static boolean isShifted(int flag) {
      return ((flag & SHIFT_FLAG) > 0);
   }

   public static boolean isFlipped(int flag) {
      return ((flag & FLIP_FLAG) > 0);
   }

}
