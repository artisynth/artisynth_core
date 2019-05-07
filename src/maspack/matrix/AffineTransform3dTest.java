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

//   void testRigidFactor (AffineTransform3d X) {
//      AffineTransform3d XS = new AffineTransform3d();
//      RigidTransform3d XR = new RigidTransform3d();
//      X.leftRigidFactor (XS, XR);
//      AffineTransform3d XC = new AffineTransform3d();
//      XC.mul (XS, XR);
//      if (!XC.epsilonEquals (X, 1e-10)) {
//         System.out.println ("X= \n" + X.toString ("%12.8f"));
//         System.out.println ("XC=\n" + XC.toString ("%12.8f"));
//         System.out.println ("XS=\n" + XS.toString ("%12.8f"));
//         System.out.println ("XR=\n" + XR.toString ("%12.8f"));
//
//         throw new TestException ("testRigidFactor failed");
//      }
//      XS.set (X);
//      XS.leftRigidFactor (XS, XR);
//      XC.mul (XS, XR);
//      if (!XC.epsilonEquals (X, 1e-10)) {
//         System.out.println ("X= \n" + X.toString ("%12.8f"));
//         System.out.println ("XC=\n" + XC.toString ("%12.8f"));
//         System.out.println ("XS=\n" + XS.toString ("%12.8f"));
//         System.out.println ("XR=\n" + XR.toString ("%12.8f"));
//
//         throw new TestException ("testRigidFactor failed");
//      }
//   }

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

   void testTransforms (AffineTransform3dBase X) {

      double EPS = 1e-13;

      Point3d p0 = new Point3d();
      Point3d pr = new Point3d();
      Point3d pchk = new Point3d();

      Vector3d v0 = new Vector3d();
      Vector3d vr = new Vector3d();
      Vector3d vchk = new Vector3d();

      Vector3d n0 = new Vector3d();
      Vector3d nr = new Vector3d();
      Vector3d nchk = new Vector3d();

      p0.setRandom();
      v0.setRandom();
      n0.setRandom();
      n0.normalize();

      X.M.mul (pchk, p0);
      pchk.add (X.b);

      X.transformPnt (pr, p0);
      checkEquals ("transformPoint", pr, pchk, EPS);
      pr.set (p0);
      X.transformPnt (pr, pr);
      checkEquals ("transformPoint", pr, pchk, EPS);
      X.inverseTransformPnt (pr, pr);
      checkEquals ("inverseTransformPoint", pr, p0, EPS);
      X.inverseTransformPnt (pr, pchk);
      checkEquals ("inverseTransformPoint", pr, p0, EPS);
      

      X.M.mul (vchk, v0);

      X.transformVec (vr, v0);
      checkEquals ("transformVector", vr, vchk, EPS);
      vr.set (v0);
      X.transformVec (vr, vr);
      checkEquals ("transformVector", vr, vchk, EPS);
      X.inverseTransformVec (vr, vr);
      checkEquals ("inverseTransformVector", vr, v0, EPS);
      X.inverseTransformVec (vr, vchk);
      checkEquals ("inverseTransformVector", vr, v0, EPS);
      
      X.M.mulInverseTranspose (nchk, n0);

      X.transformCovec (nr, n0);
      checkEquals ("transformNormal", nr, nchk, EPS);
      nr.set (n0);
      X.transformCovec (nr, nr);
      checkEquals ("transformNormal", nr, nchk, EPS);
      X.inverseTransformCovec (nr, nr);
      checkEquals ("inverseTransformNormal", nr, n0, EPS);
      X.inverseTransformCovec (nr, nchk);
      checkEquals ("inverseTransformNormal", nr, n0, EPS);

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

         testTransforms (X1);
      }
   }

   public void timing() {
      FunctionTimer timer = new FunctionTimer();

      AffineTransform3d X = new AffineTransform3d();
      X.setRandom();
      int nvecs = 1000000;
      int cnt = 10;
      Vector3d[] vecs = new Vector3d[nvecs];
      Vector3d[] chks = new Vector3d[nvecs];
      for (int i=0; i<vecs.length; i++) {
         vecs[i] = new Vector3d();
         vecs[i].setRandom();
         chks[i] = new Vector3d(vecs[i]);
         X.transformVec (vecs[i], vecs[i]);
         X.inverseTransformVec (vecs[i], vecs[i]);
      }

      System.out.println (
         "Comparative timing of forward and inverse vector transforms:");

      timer.start();
      for (int k=0; k<cnt; k++) {
         for (int i=0; i<vecs.length; i++) {
            X.transformVec (vecs[i], vecs[i]);
         }
      }
      timer.stop();
      System.out.println ("forward transform: " + timer.result(nvecs*cnt));

      timer.start();
      for (int k=0; k<cnt; k++) {
         for (int i=0; i<vecs.length; i++) {
            X.inverseTransformVec (vecs[i], vecs[i]);
         }
      }
      
      timer.stop();
      System.out.println ("inverse transform: " + timer.result(nvecs*cnt));

   }

   public static void main (String[] args) {
      AffineTransform3dTest tester = new AffineTransform3dTest();

      boolean doTiming = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.out.println ("Unknown option "+args[i]+"; ignoring");
         }
      }

      RandomGenerator.setSeed (0x1234);

      if (doTiming) {
         tester.timing();
      }
      else {
         try {
            tester.execute();
         }
         catch (Exception e) {
            e.printStackTrace();
            System.exit (1);
         }
         System.out.println ("\nPassed\n");
      }
      
   }
}
