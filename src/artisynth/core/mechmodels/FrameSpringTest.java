/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.util.RandomGenerator;
import maspack.util.TestException;
import maspack.util.UnitTest;
import maspack.function.Diff1Function1x1Base;
import maspack.function.CubicFunction1x1;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;

public class FrameSpringTest extends UnitTest {

   RigidBody myFrameA;
   RigidBody myFrameB;
   RigidTransform3d myTAW = new RigidTransform3d();
   RigidTransform3d myTBW = new RigidTransform3d();
   RigidTransform3d myT21 = new RigidTransform3d();
   RigidTransform3d myT1A = new RigidTransform3d();
   RigidTransform3d myT2B = new RigidTransform3d();

   FrameSpring mySpring;

   double dthe = 1e-8;
   double dpos = 1e-8;

   Twist dx[] = new Twist[6];

   void setSpringAndFrames (
      RigidTransform3d TAW, RigidTransform3d T21, 
      RigidTransform3d T1A, RigidTransform3d T2B) {

      mySpring = new FrameSpring ("");
      mySpring.setJacobianSymmetric (false);
      mySpring.setAttachFrameA (T1A);
      mySpring.setAttachFrameB (T2B);
      myFrameA = new RigidBody ("");
      myFrameA.setInertia (new SpatialInertia());
      myFrameB = new RigidBody ("");
      myFrameB.setInertia (new SpatialInertia());

      mySpring.setFrameA (myFrameA);
      mySpring.setFrameB (myFrameB);
      mySpring.myApplyRestPose = true;

      myTAW.set (TAW);
      myFrameA.setPose (myTAW);
      myTBW.mul (myTAW, T1A);
      myTBW.mul (T21);
      myTBW.mulInverseRight (myTBW, T2B);
      myFrameB.setPose (myTBW);

      myT21.set (T21);
      myT1A.set (T1A);
      myT2B.set (T2B);

      dx[0] = new Twist (dpos, 0, 0, 0, 0, 0);
      dx[1] = new Twist (0, dpos, 0, 0, 0, 0);
      dx[2] = new Twist (0, 0, dpos, 0, 0, 0);
      dx[3] = new Twist (0, 0, 0, dthe, 0, 0);
      dx[4] = new Twist (0, 0, 0, 0, dthe, 0);
      dx[5] = new Twist (0, 0, 0, 0, 0, dthe);
   }

   private Matrix3d scaledCrossProd (double s, Vector3d u) {
      Matrix3d T = new Matrix3d();
      T.m01 = -u.z;
      T.m02 =  u.y;
      T.m12 = -u.x;
      T.m10 =  u.z;
      T.m20 = -u.y;
      T.m21 =  u.x;
      T.scale (s);
      return T;
   }

   private Matrix3d computeU (RotationMatrix3d R21) {

      Matrix3d U = new Matrix3d();
      AxisAngle axisAng = new AxisAngle();
      R21.getAxisAngle (axisAng);

      mySpring.computeU (U, axisAng);
      return U;
   }      

   private void addSubMatrix (MatrixNd J, int i0, int j0, Matrix3dBase M) {
      J.add (i0  , j0  , M.m00);
      J.add (i0  , j0+1, M.m01);
      J.add (i0  , j0+2, M.m02);

      J.add (i0+1, j0  , M.m10);
      J.add (i0+1, j0+1, M.m11);
      J.add (i0+1, j0+2, M.m12);

      J.add (i0+2, j0  , M.m20);
      J.add (i0+2, j0+1, M.m21);
      J.add (i0+2, j0+2, M.m22);
   }

   private void getForces (Wrench fA, Wrench fB) {
      fA.set (myFrameA.getForce());
      if (!Frame.dynamicVelInWorldCoords) {
         fA.inverseTransform (myFrameA.getPose().R);
      }
      fB.set (myFrameB.getForce());
      if (!Frame.dynamicVelInWorldCoords) {
         fB.inverseTransform (myFrameB.getPose().R);
      }
//       myFrameA.computeTotalBodyForce (fA);
//       myFrameB.computeTotalBodyForce (fB);
   }

   private void setFrameVelocity (Frame F, Twist vel) {
      if (Frame.dynamicVelInWorldCoords) {
         F.setVelocity (vel);
      }
      else {
         F.setBodyVelocity (vel);
      }
   }

   public void special (Twist velA, Twist velB) {

      setFrameVelocity (myFrameA, velA);
      setFrameVelocity (myFrameB, velB);
      myFrameA.setForce (Wrench.ZERO);
      myFrameB.setForce (Wrench.ZERO);

      Wrench fA0 = new Wrench();
      Wrench fB0 = new Wrench();
      Wrench fA = new Wrench();
      Wrench fB = new Wrench();

      mySpring.applyForces (0);
      getForces (fA0, fB0);
      System.out.println ("fA0=" + fA0);
      System.out.println ("fB0=" + fB0);

      myFrameA.extrapolatePose (new Twist (0, 0, 0, 0, 0, dthe), 1);

      myFrameA.setForce (Wrench.ZERO);
      myFrameB.setForce (Wrench.ZERO);
      mySpring.applyForces (0);

      getForces (fA, fB);
      System.out.println ("fA=" + fA);
      System.out.println ("fB=" + fB);
      
      fA.sub (fA0);
      fA.scale (1/dthe);
      fB.sub (fB0);
      fB.scale (1/dthe);
      System.out.println ("dA=" + fA.toString ("%9.5f"));
      System.out.println ("dB=" + fB.toString ("%9.5f"));
   }

   public MatrixNd numericPosJacobian (Twist velA, Twist velB) {

      setFrameVelocity (myFrameA, velA);
      setFrameVelocity (myFrameB, velB);
      myFrameA.setForce (Wrench.ZERO);
      myFrameB.setForce (Wrench.ZERO);

      Wrench fA0 = new Wrench();
      Wrench fB0 = new Wrench();
      Wrench fA = new Wrench();
      Wrench fB = new Wrench();

      mySpring.applyForces (0);
      getForces (fA0, fB0);
      
      MatrixNd J = new MatrixNd (12, 12);
      Twist dv = new Twist();

      for (int j=0; j<12; j++){
         if (j < 6) {
            dv.set (dx[j]);
            if (Frame.dynamicVelInWorldCoords) {
               dv.inverseTransform (myTAW.R, dv);
            }
            myFrameA.extrapolatePose (dv, 1);
         }
         else {
            dv.set (dx[j-6]);
            if (Frame.dynamicVelInWorldCoords) {
               dv.inverseTransform (myTBW.R, dv);
            }
            myFrameB.extrapolatePose (dv, 1);
         }
         myFrameA.setForce (Wrench.ZERO);
         myFrameB.setForce (Wrench.ZERO);
         mySpring.applyForces (0);
         getForces (fA, fB);
         fA.sub (fA0);
         fB.sub (fB0);
         if ((j%6)<3) {
            fA.scale (1/dpos);
            fB.scale (1/dpos);
         }
         else {
            fA.scale (1/dthe);
            fB.scale (1/dthe);
         }
         for (int i=0; i<6; i++) {
            J.set (i,   j, fA.get(i));
            J.set (i+6, j, fB.get(i));
         }
         myFrameA.setPose (myTAW);
         myFrameB.setPose (myTBW);
      }
      return J;
   }

   public MatrixNd computeVelJacobian (Twist velA, Twist velB) {

      setFrameVelocity (myFrameA, velA);
      setFrameVelocity (myFrameB, velB);
      myFrameA.setSolveIndex (0);
      myFrameB.setSolveIndex (1);

      SparseNumberedBlockMatrix S = new SparseNumberedBlockMatrix();
      S.addBlock (0, 0, new Matrix6dBlock());
      S.addBlock (0, 1, new Matrix6dBlock());
      S.addBlock (1, 1, new Matrix6dBlock());
      S.addBlock (1, 0, new Matrix6dBlock());

      double s = 0.1235; // make sure scaling works
      mySpring.addSolveBlocks (S);
      mySpring.addVelJacobian (S, s);
      MatrixNd M = new MatrixNd (12, 12);
      M.setSubMatrix (0, 0, S.getBlock (0, 0));
      M.setSubMatrix (0, 6, S.getBlock (0, 1));
      M.setSubMatrix (6, 6, S.getBlock (1, 1));
      M.setSubMatrix (6, 0, S.getBlock (1, 0));
      M.scale (1/s);
      return M;
   }

   public MatrixNd computePosJacobian (Twist velA, Twist velB) {

      setFrameVelocity (myFrameA, velA);
      setFrameVelocity (myFrameB, velB);
      myFrameA.setSolveIndex (0);
      myFrameB.setSolveIndex (1);

      SparseNumberedBlockMatrix S = new SparseNumberedBlockMatrix();
      S.addBlock (0, 0, new Matrix6dBlock());
      S.addBlock (0, 1, new Matrix6dBlock());
      S.addBlock (1, 1, new Matrix6dBlock());
      S.addBlock (1, 0, new Matrix6dBlock());

      double s = 0.1235; // make sure scaling works
      mySpring.addSolveBlocks (S);
      mySpring.addPosJacobian (S, s);
      MatrixNd M = new MatrixNd (12, 12);
      M.setSubMatrix (0, 0, S.getBlock (0, 0));
      M.setSubMatrix (0, 6, S.getBlock (0, 1));
      M.setSubMatrix (6, 6, S.getBlock (1, 1));
      M.setSubMatrix (6, 0, S.getBlock (1, 0));
      M.scale (1/s);
      return M;
   }

   public MatrixNd numericVelJacobian (Twist velA, Twist velB) {
      Twist velAx = new Twist();
      Twist velBx = new Twist();

      Wrench fA0 = new Wrench();
      Wrench fB0 = new Wrench();
      Wrench fA = new Wrench();
      Wrench fB = new Wrench();

      setFrameVelocity (myFrameA, velA);
      setFrameVelocity (myFrameB, velB);
      myFrameA.setForce (Wrench.ZERO);
      myFrameB.setForce (Wrench.ZERO);
      mySpring.applyForces (0);
      getForces (fA0, fB0);

      MatrixNd J = new MatrixNd (12, 12);

      for (int j=0; j<12; j++) {
         if (j < 6) {
            velAx.add (velA, dx[j]);
            velBx.set (velB);
         }
         else {
            velAx.set (velA);
            velBx.add (velB, dx[j-6]);
         }
         setFrameVelocity (myFrameA, velAx);
         setFrameVelocity (myFrameB, velBx);
         myFrameA.setForce (Wrench.ZERO);
         myFrameB.setForce (Wrench.ZERO);
         mySpring.applyForces (0);
         getForces (fA, fB);
         fA.sub (fA0);
         fB.sub (fB0);
         if ((j%6)<3) {
            fA.scale (1/dpos);
            fB.scale (1/dpos);
         }
         else {
            fA.scale (1/dthe);
            fB.scale (1/dthe);
         }
         for (int i=0; i<6; i++) {
            J.set (i,   j, fA.get(i));
            J.set (i+6, j, fB.get(i));
         }
      }
      return J;
   }

   public static Vector3d getRQ (RotationMatrix3d R) {
      AxisAngle axisAng = new AxisAngle();
      R.getAxisAngle (axisAng);
      Vector3d rq = new Vector3d (axisAng.axis);
      rq.scale (axisAng.angle);
      return rq;
   }

   public void test (
      FrameMaterial mat, Twist velA, Twist velB,
      boolean printData, double errLim) {
      
      mySpring.setMaterial (mat);
      MatrixNd JC = computePosJacobian (velA, velB);
      if (printData) {
         System.out.println ("ComputedPosJ=\n" + JC.toString ("%9.5f"));
      }
      MatrixNd JN = numericPosJacobian (velA, velB);
      if (printData) {
         System.out.println ("NumericPosJ=\n" + JN.toString ("%9.5f"));
      }

      double mag = Math.max (1e-10, JN.frobeniusNorm());
      double err;

      JC.sub (JN);
      err = JC.frobeniusNorm()/mag;
      if (printData) {
         System.out.println ("Difference=\n" + JC.toString ("%9.5f"));
         System.out.println ("error=" + err);
      }
      if (err > errLim) {
         throw new TestException ("Position Jacobian error=" + err);
      }
      
      JC = computeVelJacobian (velA, velB);
      if (printData) {
         System.out.println ("ComputedVelJ=\n" + JC.toString ("%9.5f"));
      }

      JN = numericVelJacobian (velA, velB);
      if (printData) {
         System.out.println ("NumericVelJ=\n" + JN.toString ("%9.5f"));
      }
      
      JC.sub (JN);
      err = JC.frobeniusNorm()/mag;
      if (printData) {
         System.out.println ("Difference=\n" + JC.toString ("%9.5f"));
         System.out.println ("error=" + err);
      }
      if (err > errLim) {
         throw new TestException ("Velocity Jacobian error=" + err);
      }

      ScanTest.testScanAndWrite (mat, null, "%g");
   }

   public void testInitialTDC () {
      LinearFrameMaterial linMat = 
         //new LinearFrameMaterial (10, 0.5, 0.0, 0.2); // kt, kr, dt, dr
         //new LinearFrameMaterial (10.0, 0.5, 2.0, 0.2);
         new LinearFrameMaterial (10.0, 0.5, 2, 0.2);

      boolean modT1A = false;

      RigidTransform3d TAW = new RigidTransform3d();
      RigidTransform3d T21 = new RigidTransform3d();
      RigidTransform3d T1A = new RigidTransform3d();
      RigidTransform3d T2B = new RigidTransform3d();
      RigidTransform3d TDC0 = new RigidTransform3d();

      T21.setRandom();
      TDC0.setRandom();

      Twist velA = new Twist();
      Twist velB = new Twist();
      velA.set (0.1, 0.2, 0.3, 0.4, 0.3, 0.2);
      velB.set (0.3,-0.2, 0.5, -0.1, 0.4, 0);

      if (modT1A) {
         RigidTransform3d T1AX = new RigidTransform3d();
         RigidTransform3d T21X = new RigidTransform3d();

         T1AX.mul (T1A, TDC0);
         T21X.mulInverseLeft (TDC0, T21);
         setSpringAndFrames (TAW, T21X, T1AX, T2B);
         mySpring.setTDC0 (new RigidTransform3d());
         mySpring.myApplyRestPose = false;
      }
      else {
         setSpringAndFrames (TAW, T21, T1A, T2B);
         mySpring.setTDC0 (TDC0);
         mySpring.myApplyRestPose = true;
      }

      System.out.println ("modT1A=" + modT1A);

      test (linMat, velA, velB, true, 1e-6);

      System.out.println ("modT1A=" + modT1A);
   }

   public void test() {
      RigidTransform3d TAW = new RigidTransform3d ();
      RigidTransform3d T1A = new RigidTransform3d ();
      RigidTransform3d T2B = new RigidTransform3d ();
      RigidTransform3d T21 = new RigidTransform3d ();
      Twist velA = new Twist();
      Twist velB = new Twist();

      Frame.dynamicVelInWorldCoords = true;

      RotAxisFrameMaterial rotAxisMat =
         // new RotAxisFrameMaterial (10, 0.5, 2.0, 0.2); // kt, kr, dt, dr
         new RotAxisFrameMaterial (10, 0.5, 2.0, 0.2); // kt, kr, dt, dr

      LinearFrameMaterial linMat = 
         //new LinearFrameMaterial (10, 0.5, 0.0, 0.2); // kt, kr, dt, dr
         new LinearFrameMaterial (10.0, 0.5, 2.0, 0.2);

      LinearFrameMaterial linMatAniso = new LinearFrameMaterial();
      linMatAniso.setStiffness (10, 20, 30);
      linMatAniso.setRotaryStiffness (0.5, 0.5, 1.0);
      linMatAniso.setDamping (2.0, 3.0, 4.0);
      linMatAniso.setRotaryDamping (0.2, 0.4, 0.6);

      PowerFrameMaterial powMat;
      int cnt = 40;
      for (int i=0; i<cnt; i++) {
         T21.setRandom();
         TAW.setRandom();
         T1A.setRandom();
         T2B.setRandom();
         velA.setRandom();
         velB.setRandom();

         setSpringAndFrames (TAW, T21, T1A, T2B);

         if ((i%2) != 0) {
            mySpring.setUseTransformDC (false);
         }
         if (((i/2)%2) != 0) {
            RigidTransform3d TRest = new RigidTransform3d();
            TRest.setRandom();
            mySpring.setTDC0 (TRest);
         }

         test (rotAxisMat, velA, velB, false, 1e-6);
         linMat.setUseXyzAngles(false);
         test (linMat, velA, velB, false, 1e-6);
         linMat.setUseXyzAngles(true);
         test (linMat, velA, velB, false, 1e-6);
         linMatAniso.setUseXyzAngles(false);
         test (linMatAniso, velA, velB, false, 1e-6);
         linMatAniso.setUseXyzAngles(true);
         test (linMatAniso, velA, velB, false, 1e-6);

         powMat = new PowerFrameMaterial (10.0, /*n*/1, 0.5, /*rn*/1, 2.0, 0.2);
         test (powMat, velA, velB, false, 1e-6);
         powMat.setDeadband (0.2);
         test (powMat, velA, velB, false, 1e-6);

         powMat = new PowerFrameMaterial (10.0, /*n*/2, 0.5, /*rn*/2, 2.0, 0.2);
         test (powMat, velA, velB, false, 1e-6);
         powMat.setDeadband (0.2, 0.1, 0.3);
         test (powMat, velA, velB, false, 1e-6);

         powMat = new PowerFrameMaterial (10.0, /*n*/3, 0.5, /*rn*/3, 2.0, 0.2);
         test (powMat, velA, velB, false, 1e-6);
         powMat.setUpperDeadband (0.2, 0.1, 0.3);
         powMat.setLowerDeadband (-0.3, 0, -0.2);
         test (powMat, velA, velB, false, 1e-6);
         powMat.setUpperRotaryDeadband (0.2, 0.1, 0.3);
         powMat.setLowerRotaryDeadband (-0.3, 0, -0.2);
         test (powMat, velA, velB, false, 1e-6);

         CubicFunction1x1 transFxn = new CubicFunction1x1 (0, 5, 1, 0);
         CubicFunction1x1 rotFxn = new CubicFunction1x1 (0, 10, -1, 0);
         FunctionFrameMaterial fxnMat =
            new FunctionFrameMaterial (transFxn, rotFxn, 1.0, 2.0);
         test (fxnMat, velA, velB, false, 1e-6);
      }
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      FrameSpringTest tester = new FrameSpringTest();
      //tester.testInitialTDC();
      tester.runtest();
   }


}
