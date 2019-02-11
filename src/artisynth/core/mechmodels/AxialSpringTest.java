/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.util.*;

import java.util.Random;

import artisynth.core.materials.LinearAxialMaterial;

public class AxialSpringTest extends UnitTest {
   private double DEFAULT_K = 2;
   private double DEFAULT_D = 10;
   private double DEFAULT_R = 1;

   AxialSpringTest() {
      Random randGen = RandomGenerator.get();
      randGen.setSeed (0x1234);
   }

   private SparseNumberedBlockMatrix createJacobian (AxialSpring spring) {
      Point pnt0 = spring.getFirstPoint();
      Point pnt1 = spring.getSecondPoint();

      pnt0.setSolveIndex (0);
      pnt1.setSolveIndex (1);

      SparseNumberedBlockMatrix S = new SparseNumberedBlockMatrix();
      if (pnt0 instanceof PlanarComponent) {
         S.addBlock (0, 0, new Matrix2x2Block());
      }
      else {
         S.addBlock (0, 0, new Matrix3x3Block());
      }
      if (pnt1 instanceof PlanarComponent) {
         S.addBlock (1, 1, new Matrix2x2Block());
      }
      else {
         S.addBlock (1, 1, new Matrix3x3Block());
      }
      return S;
   }

   private void checkJacobian (
      SparseBlockMatrix S, AxialSpring spring, Matrix3d M) {
      Matrix3d Mneg = new Matrix3d();
      Mneg.negate (M);
      MatrixNd J = new MatrixNd (6, 6);
      J.setSubMatrix (0, 0, M);
      J.setSubMatrix (3, 3, M);
      J.setSubMatrix (3, 0, Mneg);
      J.setSubMatrix (0, 3, Mneg);
      Point pnt0 = spring.getFirstPoint();
      Point pnt1 = spring.getSecondPoint();
      if (pnt0 instanceof PlanarComponent) {
         RotationMatrix3d R = ((PlanarComponent)pnt0).getPlaneToWorld().R;
         MatrixNd G = new MatrixNd (6, 5);
         for (int i = 3; i < 6; i++) {
            G.set (i, i - 1, 1);
         }
         G.copySubMatrix (0, 0, 3, 2, R, 0, 0);
         // System.out.println ("G=\n" + G.toString("%8.3f"));
         J.mul (G);
         J.mulTransposeLeft (G, J);
      }
      if (pnt1 instanceof PlanarComponent) {
         RotationMatrix3d R = ((PlanarComponent)pnt1).getPlaneToWorld().R;
         MatrixNd G = new MatrixNd (J.colSize(), J.colSize() - 1);
         int Ridx0 = J.colSize() - 3;
         for (int i = 0; i < Ridx0; i++) {
            G.set (i, i, 1);
         }
         G.copySubMatrix (0, 0, 3, 2, R, Ridx0, Ridx0);
         J.mul (G);
         J.mulTransposeLeft (G, J);
      }
      if (!J.epsilonEquals (S, 1e-10)) {
         throw new TestException ("\nJ is\n"
         + (new MatrixNd (S)).toString ("%10.5f") + "expected\n"
         + J.toString ("%10.5f"));
      }
   }

   private void testForceAndJacobians (AxialSpring spring) {
      Vector3d u = new Vector3d();
      Matrix3d uuT = new Matrix3d();
      Matrix3d Iminus_uuT = new Matrix3d();
      Vector3d delv = new Vector3d();
      Matrix3d udelvT = new Matrix3d();
      double l;
      Vector3d f = new Vector3d();
      Matrix3d Fxx = new Matrix3d();
      Matrix3d Fvv = new Matrix3d();
      Vector3d fcheck = new Vector3d();

      LinearAxialMaterial mat = (LinearAxialMaterial)spring.getMaterial();
      double k = mat.getStiffness();
      double d = mat.getDamping();
      double r = spring.getRestLength();

      Point pnt0 = spring.getFirstPoint();
      Point pnt1 = spring.getSecondPoint();

      Point3d pos = new Point3d();
      Vector3d vel = new Vector3d();
      pos.setRandom();
      vel.setRandom();
      pnt0.setPosition (pos);
      pnt1.setVelocity (vel);
      pos.setRandom();
      vel.setRandom();
      pnt0.setPosition (pos);
      pnt1.setVelocity (vel);

      SparseNumberedBlockMatrix S = createJacobian (spring);

      u.sub (pnt1.getPosition(), pnt0.getPosition());
      l = u.norm();
      u.scale (1 / l);
      uuT.outerProduct (u, u);

      Iminus_uuT.setIdentity();
      Iminus_uuT.sub (uuT);
      delv.sub (pnt1.getVelocity(), pnt0.getVelocity());
      udelvT.outerProduct (u, delv);

      double fmag = k * (l - r) + d * delv.dot (u);

      fcheck.scale (fmag, u);

      spring.computeForce (f);
      if (!f.epsilonEquals (fcheck, 1e-10)) {
         throw new TestException ("\nforce is " + f.toString ("%10.5f")
         + ", expected " + fcheck.toString ("%10.5f"));
      }

      spring.computeForcePositionJacobian (Fxx);
      Matrix3d FxxCheck = checkForcePositionJacobian (spring);
      if (!Fxx.epsilonEquals (FxxCheck, 1e-10)) {
         System.out.println ("pos0=" + pnt0.getPosition());
         System.out.println ("vel0=" + pnt0.getVelocity());
         System.out.println ("pos1=" + pnt1.getPosition());
         System.out.println ("vel1=" + pnt1.getVelocity());

         throw new TestException ("\nFxx is\n" + Fxx.toString ("%10.5f")
         + "expected\n" + FxxCheck.toString ("%10.5f"));
      }
      // if (!(pnt0 instanceof PlanarComponent) &&
      // !(pnt1 instanceof PlanarComponent))
      // {
      // Matrix3d FxxDiscrete = discreteForcePositionJacobian (spring);
      // if (!Fxx.epsilonEquals (FxxDiscrete, 1e-4))
      // { throw new TestException (
      // "\nFxx is\n"+Fxx.toString("%10.5f")+
      // "expected\n"+FxxDiscrete.toString("%10.5f"));
      // }
      // }

      spring.computeForceVelocityJacobian (Fvv);
      Matrix3d FvvCheck = checkForceVelocityJacobian (spring);
      if (!Fvv.epsilonEquals (FvvCheck, 1e-10)) {
         throw new TestException ("\nFvv is\n" + Fvv.toString ("%10.5f")
         + "expected\n" + FvvCheck.toString ("%10.5f"));
      }
      // if (!(pnt0 instanceof PlanarComponent) &&
      // !(pnt1 instanceof PlanarComponent))
      // {
      // Matrix3d FvvDiscrete = discreteForceVelocityJacobian (spring);
      // if (!Fvv.epsilonEquals (FvvDiscrete, 1e-4))
      // { throw new TestException (
      // "\nFvv is\n"+Fvv.toString("%10.5f")+
      // "expected\n"+FvvDiscrete.toString("%10.5f"));
      // }
      // }

      // now test full Jacobian calculation

      spring.addSolveBlocks (S);
      spring.addPosJacobian (S, 1);
      checkJacobian (S, spring, Fxx);
      S.setZero();
      spring.addVelJacobian (S, 1);
      checkJacobian (S, spring, Fvv);
   }

   private Matrix3d discreteForcePositionJacobian (AxialSpring spring) {
      Vector3d force0 = new Vector3d();
      Vector3d forced = new Vector3d();
      Matrix3d Mxx = new Matrix3d();

      Point pnt0 = spring.getFirstPoint();
      spring.computeForce (force0);
      Point3d pos0 = new Point3d();
      Point3d posd = new Point3d();
      pnt0.getPosition (pos0);

      Vector3d delf = new Vector3d();
      double dx = 0.0000001;
      for (int j = 0; j < 3; j++) {
         posd.set (pos0);
         posd.set (j, posd.get (j) + dx);
         pnt0.setPosition (posd);
         spring.computeForce (forced);
         delf.sub (forced, force0);
         delf.scale (1 / dx);
         Mxx.setColumn (j, delf);
      }
      pnt0.setPosition (pos0);
      return Mxx;
   }

   private Matrix3d discreteForceVelocityJacobian (AxialSpring spring) {
      Vector3d force0 = new Vector3d();
      Vector3d forced = new Vector3d();
      Matrix3d Mvv = new Matrix3d();

      Point pnt0 = spring.getFirstPoint();
      spring.computeForce (force0);
      Point3d vel0 = new Point3d();
      Point3d veld = new Point3d();
      pnt0.getVelocity (vel0);

      Vector3d delf = new Vector3d();
      double dx = 0.00000001;
      for (int j = 0; j < 3; j++) {
         veld.set (vel0);
         veld.set (j, veld.get (j) + dx);
         pnt0.setVelocity (veld);
         spring.computeForce (forced);
         delf.sub (forced, force0);
         delf.scale (1 / dx);
         Mvv.setColumn (j, delf);
      }
      pnt0.setVelocity (vel0);
      return Mvv;
   }

   private Matrix3d checkForcePositionJacobian (AxialSpring spring) {
      Matrix3d Mxx = new Matrix3d();
      Point pnt0 = spring.getFirstPoint();
      Point pnt1 = spring.getSecondPoint();

      Vector3d u = new Vector3d();
      u.sub (pnt1.getPosition(), pnt0.getPosition());
      double len = u.norm();
      if (len == 0) {
         Mxx.setZero();
      }
      else {
         u.scale (1 / len);
         Vector3d delVel = new Vector3d();
         delVel.sub (pnt1.getVelocity(), pnt0.getVelocity());
         double ldot = u.dot (delVel);

         double F = spring.computeF (len, ldot);

         // dFdl is the derivative of force magnitude with respect to length
         double dFdl = spring.computeDFdl (len, ldot);

         // dFdldot is the derivative of force magnitude with respect
         // to the time derivative of length
         double dFdldot = spring.computeDFdldot (len, ldot);

         Matrix3d UU = new Matrix3d();
         Matrix3d I_minusUU = new Matrix3d();
         Matrix3d UV = new Matrix3d();

         UU.outerProduct (u, u);
         I_minusUU.setIdentity();
         I_minusUU.sub (UU);
         UV.outerProduct (u, delVel);

         Mxx.mul (UV, I_minusUU);
         Mxx.scale (-dFdldot / len);
         Mxx.scaledAdd (-dFdl, UU);
         Mxx.scaledAdd (-F / len, I_minusUU);

         // Formulation in Eddy Boxerman's thesis: seems to be in error
         // Mxx.sub (I_minusUU, UU);
         // Mxx.scale (u.dot (delVel));
         // Mxx.add (UV);
         // Mxx.scale (-dFdldot/len);
      }
      return Mxx;
   }

   private Matrix3d checkForceVelocityJacobian (AxialSpring spring) {
      Matrix3d Mvv = new Matrix3d();
      Point pnt0 = spring.getFirstPoint();
      Point pnt1 = spring.getSecondPoint();

      Vector3d u = new Vector3d();
      u.sub (pnt1.getPosition(), pnt0.getPosition());
      double len = u.norm();
      if (len == 0) {
         Mvv.setZero();
      }
      else {
         u.scale (1 / len);
         Vector3d delVel = new Vector3d();
         delVel.sub (pnt1.getVelocity(), pnt0.getVelocity());
         double ldot = u.dot (delVel);

         // dFdldot is the derivative of force magnitude with respect
         // to the time derivative of length
         double dFdldot = spring.computeDFdldot (len, ldot);

         Mvv.outerProduct (u, u);
         Mvv.scale (-dFdldot);
      }
      return Mvv;
   }

   public void test() {
      Point p0 = new Point();
      Point p1 = new Point();

      RigidTransform3d XplaneToWorld = new RigidTransform3d();
      XplaneToWorld.R.setAxisAngle (1, 2, 3, 3.4);

      AxialSpring spring33 = new AxialSpring (DEFAULT_K, DEFAULT_D, DEFAULT_R);
      spring33.setFirstPoint (p0);
      spring33.setSecondPoint (p1);

      int numTests = 1000;

      AxialSpring.myIgnoreCoriolisInJacobian = false;
      for (int i = 0; i < numTests; i++) {
         testForceAndJacobians (spring33);
      }
   }

   public static void main (String[] args) {
      AxialSpringTest tester = new AxialSpringTest();
      tester.runtest();
   }
}
