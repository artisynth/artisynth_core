/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.spatialmotion.*;

import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;

public class FemModel3dTest extends UnitTest {

   /**
    * Computes the masss matrix and fictitious forces for a frame-relative FEM
    * model. The rows and columns are ordered assuming that all components are
    * "active", with the nodal rows and appearing first, and those for the
    * frame appearing last. Used for testing and debugging only.
    */
   public static MatrixNd computeFrameRelativeMass (VectorNd f, FemModel3d fem) {

      int nnodes = fem.numNodes();
      Frame frame = fem.getFrame();

      int size = 3*nnodes + 6;

      f.setSize (size);

      MatrixNd M = new MatrixNd (size, size);
      Matrix3d X = new Matrix3d();
      Vector3d tmp = new Vector3d();
      Vector3d w = frame.getVelocity().w;
      Wrench fc = new Wrench();
      Vector3d cl = new Vector3d();
      Vector3d cw = new Vector3d();
      Vector3d v = new Vector3d();

      int fidx = 3*nnodes;
      RotationMatrix3d R = frame.getPose().R;
      SpatialInertia S = new SpatialInertia();
      for (int i=0; i<nnodes; i++) {
         FemNode3d n = fem.getNodes().get(i);
         double m = n.getMass();
         n.getLocalPosition(cl);
         cw.transform (R, cl);

         // compute matrix components
         S.addPointMass (m, cw);
         X.setIdentity();
         X.scale (m);
         M.setSubMatrix (3*i, 3*i, X);
         X.scale (m, R);
         M.setSubMatrix (fidx, 3*i, X);         
         X.transpose ();
         M.setSubMatrix (3*i, fidx, X);         
         X.crossProduct (cw, R);
         X.scale (m);
         M.setSubMatrix (fidx+3, 3*i, X);
         X.transpose ();
         M.setSubMatrix (3*i, fidx+3, X);

         // compute frictional force components
         Vector3d m2wXv = new Vector3d();
         v.transform (R, n.getLocalVelocity());
         m2wXv.cross (w, v);
         m2wXv.scale (2*m);
         fc.f.add (m2wXv);
         tmp.cross (cw, m2wXv);
         fc.m.add (tmp);
         
         tmp.cross (w, cw);
         tmp.cross (w, tmp); 
         tmp.scale (m);
         tmp.add (m2wXv);
         tmp.inverseTransform (R);
         tmp.negate();
         f.setSubVector (3*i, tmp);
      }
      M.setSubMatrix (fidx, fidx, S);

      S.getCenterOfMass(cw);
      Matrix3dBase Jc = S.getRotationalInertia();

      Jc.mul (v, w);
      v.cross (w, v);
      fc.m.add (v);
      v.cross (cw, w);
      v.cross (cw, v);
      v.scale (S.getMass());
      v.cross (w, v);
      fc.m.sub (v);

      v.cross (w, cw);
      v.cross (w, v);
      v.scale (S.getMass());
      fc.f.add (v);

      fc.negate();
      f.setSubVector (fidx, fc.f);
      f.setSubVector (fidx+3, fc.m);

      return M;
   }

   private void checkMassMatrix (
      SparseBlockMatrix M, VectorNd f, FemModel3d fem, double t) {

      int size = 3*fem.numNodes()+6;
      VectorNd fchk = new VectorNd ();
      MatrixNd Mchk = computeFrameRelativeMass (fchk, fem);
      double tol = M.frobeniusNorm()*1e-12;

      if (!Mchk.epsilonEquals (M, tol)) {
         System.out.println ("M=\n" + M.toString ("%7.3f"));
         System.out.println ("Mchk=\n" + Mchk.toString ("%7.3f"));
         throw new TestException ("ERROR: mass matrix not equal at t=" + t);
      }
      if (!fchk.epsilonEquals (f, tol)) {
         System.out.println ("f=   \n" + f.toString ("%7.3f"));
         System.out.println ("fchk=\n" + fchk.toString ("%7.3f"));
         throw new TestException ("ERROR: mass forces not equal at t=" + t);
      }      
   }

   /**
    * Check mass forces is intended to check the computation of fictitious
    * forces by ensuring that they are equal to dot M * vel.
    */
   private void checkMassForces (
      SparseBlockMatrix M, VectorNd f, MechModel mech,
      FemModel3d fem, double t) {

      int vsize = mech.getActiveVelStateSize();
      int psize = mech.getActivePosStateSize();

      VectorNd velState0 = new VectorNd(vsize);
      VectorNd posState0 = new VectorNd(psize);
      VectorNd posState  = new VectorNd(psize);

      mech.getActivePosState (posState0);
      mech.getActiveVelState (velState0);

      SparseBlockMatrix Mdot = M.clone();

      // advance mech by a small time
      double h = 1e-9;
      posState.set (posState0);
      mech.addActivePosImpulse (posState, h, velState0);
      mech.setActivePosState (posState);

      VectorNd fchk = new VectorNd();
      
      mech.getMassMatrix (Mdot, fchk, t);
      mech.setActivePosState (posState0);
      mech.setActiveVelState (velState0);

      // Now compute the fictitious forces using
      //
      // fchk = -\dot M vel
      //
      // Because the inertial frame used for M is moving *with* the coordinate
      // frame origin, the component of vel corresponding to the translational
      // body velocity should be set to zero.
      //
      Mdot.sub (M);
      Mdot.scale (1/h);
      velState0.setSubVector (vsize-6, Vector3d.ZERO);
      Mdot.mul (fchk, velState0);
      //System.out.println ("Mdot=\n" + Mdot.toString("%12.6f"));
      fchk.scale (-1);

      double tol = f.infinityNorm()*1e-6;

      // Also, correct fchk to account for the fact that the f_i are computed
      // in rotating coordinates, as opposed to the inertial coordinates.
      // 
      // Specifically, let M_i and M_si be the i-th block rows of the mass
      // matrix in the rotating and inertial coordinate frames, respectively,
      // and let f_i and fchk_i be the corresponding component of f and fchk.
      // Let R be the frame orientation and w the angular velocity in inertial
      // coordinates.
      //
      // Then f = -R^T \dot M_si vel, while for the mass matrix we have M_i =
      // R^T M_si and fchk_i = -\dot M_i vel. Expanding \dot M_i gives
      //
      // \dot M_i = -[R^T w] R^T M_si + R^T \dot\M_si 
      //          = -[R^T w] M_i + R^T \dot\M_si 
      //
      // and so 
      //
      // fchk_i = [R^T w] M_i vel + f
      //
      VectorNd mv = new VectorNd(vsize);
      M.mul (mv, velState0);
      RotationMatrix3d R = fem.getFrame().getPose().R;
      Vector3d w = new Vector3d();
      for (int i=0; i<fem.numNodes(); i++) {
         Vector3d fi = new Vector3d();
         Vector3d wmvi = new Vector3d();

         w.inverseTransform (R, fem.getFrame().getVelocity().w);
         mv.getSubVector (i*3, wmvi);
         wmvi.cross (w, wmvi);
         fchk.getSubVector (i*3, fi);
         fi.sub (wmvi);
         fchk.setSubVector (i*3, fi);
      }

      if (!fchk.epsilonEquals (f, tol)) {
         System.out.println ("f=   \n" + f.toString ("%8.4f"));
         System.out.println ("fchk=\n" + fchk.toString ("%8.4f"));
         throw new TestException (
            "ERROR: mass forces not equal to dot(M) v at t=" + t);
      }  
   }

   public void testFrameRelativeMass() {

      double density = 1000;
      double wx = 1.0;
      double wy = 0.2;
      double wz = 0.2;

      // create a FemModel

      FemModel3d fem =
         FemFactory.createHexGrid (null, wx, wy, wz, 1, 1, 1);
      LinearMaterial lmat = new LinearMaterial (100000, 0.33);
      fem.setDensity (density);
      fem.setMaterial (lmat);     
      fem.attachFrame (new RigidTransform3d (-0.25, 0.05, 0.05));
      //fem.attachFrame (new RigidTransform3d ());
      fem.setFrameRelative (true);
      fem.setParticleDamping (0);
      fem.setStiffnessDamping (0);

      fem.getFrame().setVelocity (new Twist (0, 0, 0, 0, 2*Math.PI, 0));
      fem.setGravity (0, 0, 0);

      PolygonalMesh mesh = MeshFactory.createBox (wx, wy, wz);
      mesh.inverseTransform (new RigidTransform3d (-0.25, 0.05, 0.05));

      RigidBody beam = RigidBody.createFromMesh ("box", mesh, density, 1);
      beam.setVelocity (new Twist (0, 0, 0, 0, 2*Math.PI, 0));

      //readState ("nodeState.txt", fem);

      MechModel mech = new MechModel();
      mech.addModel (fem);
      //mech.addRigidBody (beam);

      SparseBlockMatrix M = new SparseBlockMatrix();
      VectorNd f = new VectorNd();
      mech.buildMassMatrix (M);

      double t0 = 0;
      double h = 0.01;
      for (int i=0; i<1000; i++) {
         double t1 = t0 + h;
         mech.preadvance (t0, t1, /*flags=*/0);
         mech.advance (t0, t1, /*flags=*/0);

         mech.getMassMatrix (M, f, t1);
         checkMassMatrix (M, f, fem, t1);
         checkMassForces (M, f, mech, fem, t1);
         t0 = t1;
      }
      //writeState ("nodeState.txt", fem);

   }

   private void writeState (String fileName, FemModel3d fem) {
      try {
         PrintWriter pw = new PrintWriter (new FileWriter (fileName));
         for (FemNode3d n : fem.getNodes()) {
            pw.println (n.getLocalPosition());
            pw.println (n.getLocalVelocity());
         }
         pw.println (fem.getFrame().getPose());
         pw.println (fem.getFrame().getVelocity());
         pw.close();
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }

   private void readState (String fileName, FemModel3d fem) {
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new FileReader (fileName));
         Vector3d tmp = new Vector3d();
         for (FemNode3d n : fem.getNodes()) {
            tmp.scan (rtok);
            n.setLocalPosition(tmp);
            tmp.scan (rtok);
            n.setLocalVelocity(tmp);
         }
         RigidTransform3d T = new RigidTransform3d();
         Twist v = new Twist();
         T.scan (rtok);
         v.scan (rtok);
         fem.getFrame().setPose (T);
         fem.getFrame().setVelocity (v);
         rtok.close();
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }
   public void test() {
      testFrameRelativeMass();
   }

   public static void main (String[] args) {
      FemModel3dTest tester = new FemModel3dTest();
      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }

}
