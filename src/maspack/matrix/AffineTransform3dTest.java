/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.*;

class AffineTransform3dTest extends MatrixTest {
   protected boolean isReadOnly (int i, int j) {
      if (i == 3) {
         return true;
      }
      else {
         return false;
      }
   }

   void testRigidFactor (AffineTransform3d X) {
      AffineTransform3d XS = new AffineTransform3d();
      RigidTransform3d XR = new RigidTransform3d();
      X.leftRigidFactor (XS, XR);
      AffineTransform3d XC = new AffineTransform3d();
      XC.mul (XS, XR);
      if (!XC.epsilonEquals (X, 1e-10)) {
         System.out.println ("X= \n" + X.toString ("%12.8f"));
         System.out.println ("XC=\n" + XC.toString ("%12.8f"));
         System.out.println ("XS=\n" + XS.toString ("%12.8f"));
         System.out.println ("XR=\n" + XR.toString ("%12.8f"));

         throw new TestException ("testRigidFactor failed");
      }
      XS.set (X);
      XS.leftRigidFactor (XS, XR);
      XC.mul (XS, XR);
      if (!XC.epsilonEquals (X, 1e-10)) {
         System.out.println ("X= \n" + X.toString ("%12.8f"));
         System.out.println ("XC=\n" + XC.toString ("%12.8f"));
         System.out.println ("XS=\n" + XS.toString ("%12.8f"));
         System.out.println ("XR=\n" + XR.toString ("%12.8f"));

         throw new TestException ("testRigidFactor failed");
      }
   }

   protected double getReadOnly (int i, int j) {
      if (i == 3 && j < 3) {
         return 0;
      }
      else if (i == 3 && j == 3) {
         return 1;
      }
      else {
         return -1;
      }
   }

   void mul (Matrix MR, Matrix M1) {
      ((AffineTransform3d)MR).mul ((AffineTransform3d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((AffineTransform3d)MR).mul ((AffineTransform3d)M1, (AffineTransform3d)M2);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((AffineTransform3d)MR).mulInverse ((AffineTransform3d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((AffineTransform3d)MR).mulInverseRight (
         (AffineTransform3d)M1, (AffineTransform3d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((AffineTransform3d)MR).mulInverseLeft (
         (AffineTransform3d)M1, (AffineTransform3d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((AffineTransform3d)MR).mulInverseBoth (
         (AffineTransform3d)M1, (AffineTransform3d)M2);
   }

   void invert (Matrix MR) {
      ((AffineTransform3d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((AffineTransform3d)MR).invert ((AffineTransform3d)M1);
   }

   void set (Matrix MR, Matrix M1) {
      ((AffineTransform3d)MR).set ((AffineTransform3d)M1);
   }

   public void execute() {
      AffineTransform3d XR = new AffineTransform3d();
      AffineTransform3d X1 = new AffineTransform3d();
      AffineTransform3d X2 = new AffineTransform3d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (X1);

      for (int i = 0; i < 100; i++) {
         X1.A.setRandom();
         X1.p.setRandom();
         X2.A.setRandom();
         X2.p.setRandom();
         XR.A.setRandom();
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

         testRigidFactor (X1);

         // testSetRotations (XR, X1);
         // testNormalize (XR);
      }
   }

   public static void main (String[] args) {
      AffineTransform3dTest test = new AffineTransform3dTest();

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
