/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

public class AxisAlignedRotationTest extends UnitTest {

   private boolean checkAxis (String name, Vector3d axis) {
      if (name.equals ("X")) {
         return axis.equals (Vector3d.X_UNIT);
      }
      else if (name.equals ("Y")) {
         return axis.equals (Vector3d.Y_UNIT);
      }
      else if (name.equals ("Z")) {
         return axis.equals (Vector3d.Z_UNIT);
      }
      else if (name.equals ("NX")) {
         return axis.equals (Vector3d.NEG_X_UNIT);
      }
      else if (name.equals ("NY")) {
         return axis.equals (Vector3d.NEG_Y_UNIT);
      }
      else if (name.equals ("NZ")) {
         return axis.equals (Vector3d.NEG_Z_UNIT);
      }
      else {
         throw new TestException ("Unrecognized axis name "+name);
      }
   }

   private void checkAxes (AxisAlignedRotation ar) {
      RotationMatrix3d R = ar.getMatrix();
      Vector3d xvec = new Vector3d();
      Vector3d yvec = new Vector3d();
      R.getColumn (0, xvec);
      R.getColumn (1, yvec);
      String name = ar.toString();
      String[] axisNames = name.split("_");
      if (axisNames.length != 2) {
         throw new TestException ("unrecognized name "+name);
      }
      if (!checkAxis (axisNames[0], xvec)) {
         throw new TestException ("axis "+axisNames[0]+" does not match "+xvec);
      }
      if (!checkAxis (axisNames[1], yvec)) {
         throw new TestException ("axis "+axisNames[1]+" does not match "+yvec);
      }
      // now perturb the rotation and see if we get the same axis back
      int ntrials = 1000;
      RotationMatrix3d X = new RotationMatrix3d();
      RotationMatrix3d P = new RotationMatrix3d();
      Vector3d u = new Vector3d();
      for (int i=0; i<ntrials; i++) {
         u.setRandom();
         double ang = RandomGenerator.nextDouble (-0.1, 0.1);
         P.setAxisAngle (u, ang);
         X.mul (R, P);
         AxisAlignedRotation result = AxisAlignedRotation.getNearest (X);
         if (result != ar) {
            throw new TestException (
               "Perturbation check failed for "+ar+": got "+result);
         }
      }
   }

   public void test() {
      for (AxisAlignedRotation ar : AxisAlignedRotation.values()) {
         checkAxes (ar);
      }
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);

      (new AxisAlignedRotationTest()).runtest();
   }

}
