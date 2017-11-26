/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.RandomGenerator;

class RigidTransform3dTest extends AffineTransform3dTest {
   void mul (Matrix MR, Matrix M1) {
      ((RigidTransform3d)MR).mul ((RigidTransform3d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((RigidTransform3d)MR).mul ((RigidTransform3d)M1, (RigidTransform3d)M2);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((RigidTransform3d)MR).mulInverse ((RigidTransform3d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((RigidTransform3d)MR).mulInverseRight (
         (RigidTransform3d)M1, (RigidTransform3d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((RigidTransform3d)MR).mulInverseLeft (
         (RigidTransform3d)M1, (RigidTransform3d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((RigidTransform3d)MR).mulInverseBoth (
         (RigidTransform3d)M1, (RigidTransform3d)M2);
   }

   void invert (Matrix MR) {
      ((RigidTransform3d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((RigidTransform3d)MR).invert ((RigidTransform3d)M1);
   }

   void set (Matrix MR, Matrix M1) {
      ((RigidTransform3d)MR).set ((RigidTransform3d)M1);
   }

   void testSpecialMuls (RigidTransform3d RR) {
      Random rand = RandomGenerator.get();

      double x = rand.nextDouble();
      double y = rand.nextDouble();
      double z = rand.nextDouble();
      double ang = rand.nextDouble();

      RigidTransform3d RC = new RigidTransform3d();

      saveResult (RR);
      RC.setIdentity();
      RC.p.set (x, y, z);
      mulCheck (MX, RR, RR, RC);
      RR.mulXyz (x, y, z);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.R.setAxisAngle (1, 0, 0, ang);
      mulCheck (MX, RR, RR, RC);
      RR.mulRotX (ang);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.R.setAxisAngle (0, 1, 0, ang);
      mulCheck (MX, RR, RR, RC);
      RR.mulRotY (ang);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.R.setAxisAngle (0, 0, 1, ang);
      mulCheck (MX, RR, RR, RC);
      RR.mulRotZ (ang);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.R.setAxisAngle (x, y, z, ang);
      mulCheck (MX, RR, RR, RC);
      RR.mulAxisAngle (x, y, z, ang);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.R.setRpy (x, y, z);
      mulCheck (MX, RR, RR, RC);
      RR.mulRpy (x, y, z);
      checkAndRestoreResult (RR, EPSILON);

      RC.setIdentity();
      RC.R.setEuler (x, y, z);
      mulCheck (MX, RR, RR, RC);
      RR.mulEuler (x, y, z);
      checkAndRestoreResult (RR, EPSILON);
   }

   public void execute() {
      RigidTransform3d XR = new RigidTransform3d();
      RigidTransform3d X1 = new RigidTransform3d();
      RigidTransform3d X2 = new RigidTransform3d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (X1);

      for (int i = 0; i < 100; i++) {
         X1.R.setRandom();
         X1.p.setRandom();
         X2.R.setRandom();
         X2.p.setRandom();
         XR.R.setRandom();
         XR.p.setRandom();

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
      RigidTransform3dTest test = new RigidTransform3dTest();

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
