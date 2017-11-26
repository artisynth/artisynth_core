/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.RandomGenerator;

class ScaledRigidTransform3dTest extends AffineTransform3dTest {
   void mul (Matrix MR, Matrix M1) {
      ((ScaledRigidTransform3d)MR).mul ((ScaledRigidTransform3d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((ScaledRigidTransform3d)MR).mul (
         (ScaledRigidTransform3d)M1, (ScaledRigidTransform3d)M2);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((ScaledRigidTransform3d)MR).mulInverse ((ScaledRigidTransform3d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((ScaledRigidTransform3d)MR).mulInverseRight (
         (ScaledRigidTransform3d)M1, (ScaledRigidTransform3d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((ScaledRigidTransform3d)MR).mulInverseLeft (
         (ScaledRigidTransform3d)M1, (ScaledRigidTransform3d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((ScaledRigidTransform3d)MR).mulInverseBoth (
         (ScaledRigidTransform3d)M1, (ScaledRigidTransform3d)M2);
   }

   void invert (Matrix MR) {
      ((ScaledRigidTransform3d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((ScaledRigidTransform3d)MR).invert ((ScaledRigidTransform3d)M1);
   }

   void set (Matrix MR, Matrix M1) {
      ((ScaledRigidTransform3d)MR).set ((ScaledRigidTransform3d)M1);
   }

   void testSpecialMuls (ScaledRigidTransform3d RR) {
      Random rand = RandomGenerator.get();

      double x = rand.nextDouble();
      double y = rand.nextDouble();
      double z = rand.nextDouble();
      double ang = rand.nextDouble();

      ScaledRigidTransform3d RC = new ScaledRigidTransform3d();

      saveResult (RR);
      RC.setIdentity();
      RC.p.set (x, y, z);
      mulCheck (MX, RR, RR, RC);
      RR.mulXyz (x, y, z);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.setRotation (new AxisAngle (1, 0, 0, ang));
      mulCheck (MX, RR, RR, RC);
      RR.mulRotX (ang);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.setRotation (new AxisAngle (0, 1, 0, ang));
      mulCheck (MX, RR, RR, RC);
      RR.mulRotY (ang);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.setRotation (new AxisAngle (0, 0, 1, ang));
      mulCheck (MX, RR, RR, RC);
      RR.mulRotZ (ang);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.setRotation (new AxisAngle (x, y, z, ang));
      mulCheck (MX, RR, RR, RC);
      RR.mulAxisAngle (x, y, z, ang);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RotationMatrix3d R = new RotationMatrix3d();
      R.setRpy (x, y, z);
      RC.setRotation (R);
      mulCheck (MX, RR, RR, RC);
      RR.mulRpy (x, y, z);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      R.setEuler (x, y, z);
      RC.setRotation (R);
      mulCheck (MX, RR, RR, RC);
      RR.mulEuler (x, y, z);
      checkAndRestoreResult (RR, EPSILON);
   }

   public void execute() {
      ScaledRigidTransform3d XR = new ScaledRigidTransform3d();
      ScaledRigidTransform3d X1 = new ScaledRigidTransform3d();
      ScaledRigidTransform3d X2 = new ScaledRigidTransform3d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (X1);

      for (int i = 0; i < 100; i++) {
         X1.setRandom();
         X2.setRandom();
         XR.setRandom();

         testMul (XR, X1, X2);
         testMul (XR, XR, XR);

         testMulInverse (XR, X1, X2);
         testMulInverse (XR, XR, XR);

         testSet (XR, X1);
         testSet (XR, XR);

         testInvert (XR, X1);
         testInvert (XR, XR);

         testNorms (X1);

         testSpecialMuls (XR);

         testTransforms (X1);
      }
   }

   public static void main (String[] args) {
      ScaledRigidTransform3dTest test = new ScaledRigidTransform3dTest();
      test.mulTol = 1e-14;

      try {
         test.execute();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }

      System.out.println ("\nPassed\n");
   }

}
