/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import artisynth.core.modelbase.ScanTest;
import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

public class MultiPointSpringTest extends UnitTest {

   private void zeroForces (ArrayList<Point> pnts) {
      for (int i=0; i<pnts.size(); i++) {
         pnts.get(i).zeroForces();
      }
   }

   private void getForce (VectorNd fvec, ArrayList<Point> pnts) {
      double[] buf = fvec.getBuffer();
      int idx = 0;
      for (int i=0; i<pnts.size(); i++) {
         Vector3d f = pnts.get(i).getForce();
         buf[idx++] = f.x;
         buf[idx++] = f.y;
         buf[idx++] = f.z;
      }
   }

   private void getPos (VectorNd xvec, ArrayList<Point> pnts) {
      double[] x = xvec.getBuffer();
      int idx = 0;
      for (Point p : pnts) {
         idx = p.getPosState (x, idx);
      }
   }

   private void setPos (VectorNd xvec, ArrayList<Point> pnts) {
      double[] x = xvec.getBuffer();
      int idx = 0;
      for (Point p : pnts) {
         idx = p.setPosState (x, idx);
      }
   }

   private void getVel (VectorNd vvec, ArrayList<Point> pnts) {
      double[] v = vvec.getBuffer();
      int idx = 0;
      for (Point p : pnts) {
         idx = p.getVelState (v, idx);
      }
   }

   private void setVel (VectorNd vvec, ArrayList<Point> pnts) {
      double[] v = vvec.getBuffer();
      int idx = 0;
      for (Point p : pnts) {
         idx = p.setVelState (v, idx);
      }
   }

   private MatrixNd numericPosJacobian (
      MultiPointSpring spring, ArrayList<Point> pnts) {

      int size = 3*pnts.size();
      VectorNd f0 = new VectorNd (size);
      VectorNd f = new VectorNd (size);
      VectorNd x0 = new VectorNd (size);
      VectorNd x = new VectorNd (size);
      VectorNd dfdx = new VectorNd (size);

      MatrixNd K = new MatrixNd (size, size);

      zeroForces (pnts);
      spring.applyForces (0);
      getForce (f0, pnts);
      getPos (x0, pnts);

      double delx = 1e-8;

      for (int j=0; j<size; j++) {
         x.set (x0);
         x.set (j, x.get(j)+delx);
         setPos (x, pnts);
         zeroForces (pnts);
         spring.applyForces (0);
         getForce (f, pnts);
         dfdx.sub (f, f0);
         dfdx.scale (1/delx);
         K.setColumn (j, dfdx); 
      }
      return K;
   }

   private MatrixNd numericVelJacobian (
      MultiPointSpring spring, ArrayList<Point> pnts) {

      int size = 3*pnts.size();
      VectorNd f0 = new VectorNd (size);
      VectorNd f = new VectorNd (size);
      VectorNd v0 = new VectorNd (size);
      VectorNd v = new VectorNd (size);
      VectorNd dfdv = new VectorNd (size);

      MatrixNd K = new MatrixNd (size, size);

      zeroForces (pnts);
      spring.applyForces (0);
      getForce (f0, pnts);
      getVel (v0, pnts);

      double delv = 1e-8;

      for (int j=0; j<size; j++) {
         v.set (v0);
         v.set (j, v.get(j)+delv);
         setVel (v, pnts);
         zeroForces (pnts);
         spring.applyForces (0);
         getForce (f, pnts);
         dfdv.sub (f, f0);
         dfdv.scale (1/delv);
         K.setColumn (j, dfdv); 
      }
      return K;
   }

   double compareMatrices (SparseBlockMatrix M, MatrixNd Mcheck) {
      MatrixNd Mdense = new MatrixNd (M);
      Mdense.sub (Mcheck);
      double normErr = Mdense.infinityNorm();
      double normCheck = Mcheck.infinityNorm();
      if (normErr == 0 && normCheck == 0) {
         return 0;
      }
      else {
         return normErr/normCheck;
      }
   }

   public void test (int numPnts) {
      test (numPnts, null);
   }

   public void test (int numPnts, int[] passiveSegs) {
      // need to have zero damping to get Jacobian to pass
      MultiPointSpring spring = new MultiPointSpring(1, 2, 3);
      ArrayList<Point> pnts = new ArrayList<Point>();
      Point3d pos = new Point3d();
      Vector3d vel = new Vector3d();
      for (int i=0; i<numPnts; i++) {
         Point pnt = new Particle ();
         pos.setRandom();
         pnt.setPosition (pos);
         vel.setRandom();
         pnt.setVelocity (vel);
         pnts.add (pnt);
         spring.addPoint (pnt);
      }
      if (passiveSegs != null) {
         for (int idx : passiveSegs) {
            if (idx < numPnts-1) {
               spring.setSegmentPassive (idx, true);
            }
         }
      }
      int[] blkSizes = new int[numPnts];
      for (int i=0; i<numPnts; i++) {
         pnts.get(i).setSolveIndex (i);
         blkSizes[i] = 3;
      }
      SparseNumberedBlockMatrix M = new SparseNumberedBlockMatrix (blkSizes);
      spring.addSolveBlocks (M);
      spring.addPosJacobian (M, 1);

      //System.out.println ("K=\n" + M.toString("%8.3f"));
      MatrixNd Kcheck = numericPosJacobian (spring, pnts);
      double err = compareMatrices (M, Kcheck);
      if (err > 2e-7) {
         throw new TestException (
            "Numeric position Jacobian differs from analytic by " + err);
      }
      //System.out.println ("pos err = " + err);

      M.setZero();
      spring.addVelJacobian (M, 1);
      //System.out.println ("D=\n" + M.toString("%8.3f"));
      MatrixNd Dcheck = numericVelJacobian (spring, pnts);
      err = compareMatrices (M, Dcheck);
      if (err > 1e-7) {
         throw new TestException (
            "Numeric velocity Jacobian differs from analytic by " + err);
      }
      //System.out.println ("vel err = " + err);

      // test scan and write. Create a MechModel to use as a reference object
      // for the points
      MechModel mech = new MechModel ("test");
      for (Point p : pnts) {
         mech.addParticle ((Particle)p);
      }
      ScanTest.testScanAndWrite (spring, mech, null);
   }

   public void test() {
      //tester.test(0); // force and Jacobians should be 0
      //tester.test(1); // force and Jacobians should be 0
      test(2);
      test(3);
      test(4);
      test(5);
      test(2, new int[] {0});               
      test(4, new int[] {0, 1});               
      test(5, new int[] {0, 2, 4});   
   }

   public static void main (String[] args) {
      MultiPointSpringTest tester = new MultiPointSpringTest();
      // have to set myIgnoreCoriolisInJacobian to false since otherwise
      // non-symmetric Jacobian terms will not be computed and the Jacobian
      // will not match the numeric check
      PointSpringBase.myIgnoreCoriolisInJacobian = false;
      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}
