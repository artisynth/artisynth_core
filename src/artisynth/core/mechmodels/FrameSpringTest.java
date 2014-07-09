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
import artisynth.core.materials.*;

public class FrameSpringTest {

   RigidBody myFrameA;
   RigidBody myFrameB;
   RigidTransform3d myXAW = new RigidTransform3d();
   RigidTransform3d myXBW = new RigidTransform3d();
   RigidTransform3d myX21 = new RigidTransform3d();
   RigidTransform3d myX1A = new RigidTransform3d();
   RigidTransform3d myX2B = new RigidTransform3d();

   FrameSpring mySpring;

   double dthe = 1e-8;
   double dpos = 1e-8;

   Twist dx[] = new Twist[6];

   FrameSpringTest (
      RigidTransform3d X21, RigidTransform3d X1A, RigidTransform3d X2B) {

      mySpring = new FrameSpring ("");
      mySpring.setJacobianSymmetric (false);
      mySpring.setAttachFrameA (X1A);
      mySpring.setAttachFrameB (X2B);
      myFrameA = new RigidBody ("");
      myFrameA.setInertia (new SpatialInertia());
      myFrameB = new RigidBody ("");
      myFrameB.setInertia (new SpatialInertia());

      mySpring.setFrameA (myFrameA);
      mySpring.setFrameB (myFrameB);

      myXAW.setRandom();
      myFrameA.setPose (myXAW);
      myXBW.mul (myXAW, X1A);
      myXBW.mul (X21);
      myXBW.mulInverseRight (myXBW, X2B);
      myFrameB.setPose (myXBW);

      myX21.set (X21);
      myX1A.set (X1A);
      myX2B.set (X2B);

      dx[0] = new Twist (dpos, 0, 0, 0, 0, 0);
      dx[1] = new Twist (0, dpos, 0, 0, 0, 0);
      dx[2] = new Twist (0, 0, dpos, 0, 0, 0);
      dx[3] = new Twist (0, 0, 0, dthe, 0, 0);
      dx[4] = new Twist (0, 0, 0, 0, dthe, 0);
      dx[5] = new Twist (0, 0, 0, 0, 0, dthe);
   }

   private Matrix3d scaledCrossProd (double s, Vector3d u) {
      Matrix3d X = new Matrix3d();
      X.m01 = -u.z;
      X.m02 =  u.y;
      X.m12 = -u.x;
      X.m10 =  u.z;
      X.m20 = -u.y;
      X.m21 =  u.x;
      X.scale (s);
      return X;
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
               dv.inverseTransform (myXAW.R, dv);
            }
            myFrameA.extrapolatePose (dv, 1);
         }
         else {
            dv.set (dx[j-6]);
            if (Frame.dynamicVelInWorldCoords) {
               dv.inverseTransform (myXBW.R, dv);
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
         myFrameA.setPose (myXAW);
         myFrameB.setPose (myXBW);
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

   public static void test (
      FrameSpringTest deriv, FrameMaterial mat, Twist velA, Twist velB,
      boolean printData, double errLim) {
      
      deriv.mySpring.setMaterial (mat);
      MatrixNd JC = deriv.computePosJacobian (velA, velB);
      if (printData) {
         System.out.println ("ComputedPosJ=\n" + JC.toString ("%9.5f"));
      }
      MatrixNd JN = deriv.numericPosJacobian (velA, velB);
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
      
      JC = deriv.computeVelJacobian (velA, velB);
      if (printData) {
         System.out.println ("ComputedVelJ=\n" + JC.toString ("%9.5f"));
      }

      JN = deriv.numericVelJacobian (velA, velB);
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
   
   }

   public static void main (String[] args) {
      RigidTransform3d X1A = new RigidTransform3d ();
      RigidTransform3d X2B = new RigidTransform3d ();
      RigidTransform3d X21 = new RigidTransform3d ();

      RandomGenerator.setSeed (0x1234);
      Frame.dynamicVelInWorldCoords = true;

      X1A.setRandom();
      X2B.setRandom();

      X21.R.setAxisAngle (2, 3, 1, Math.toRadians (88));
      X21.p.set (1, 2, 3);

      //X1A.p.set (1, 0, 0);
      //X21.p.set (1, 0, 0);

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

      FrameSpringTest deriv = new FrameSpringTest (X21, X1A, X2B);

      Twist velA = new Twist();
      Twist velB = new Twist();
      velA.set (0.1, 0.2, 0.3, 0.4, 0.3, 0.2);
      velB.set (0.3,-0.2, 0.5, -0.1, 0.4, 0);

      test (deriv, rotAxisMat, velA, velB, false, 1e-6);
      test (deriv, linMat, velA, velB, false, 1e-6);
      test (deriv, linMatAniso, velA, velB, false, 1e-6);

      System.out.println ("\nPASSED\n");
   }


}
