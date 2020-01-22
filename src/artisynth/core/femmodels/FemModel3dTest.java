/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.spatialmotion.*;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
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
         cl.set(n.getLocalPosition());
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

      SparseNumberedBlockMatrix Mdot = new SparseNumberedBlockMatrix();
      mech.buildMassMatrix (Mdot);

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

      SparseNumberedBlockMatrix M = new SparseNumberedBlockMatrix();
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

//   private void writeState (String fileName, FemModel3d fem) {
//      try {
//         PrintWriter pw = new PrintWriter (new FileWriter (fileName));
//         for (FemNode3d n : fem.getNodes()) {
//            pw.println (n.getLocalPosition());
//            pw.println (n.getLocalVelocity());
//         }
//         pw.println (fem.getFrame().getPose());
//         pw.println (fem.getFrame().getVelocity());
//         pw.close();
//      }
//      catch (Exception e) {
//         e.printStackTrace(); 
//      }
//   }

   private static double EPS = 1e-12;

   private FemNode3d getOrCreateNode (
      FemModel3d fem, double x, double y, double z) {
      Point3d pos = new Point3d (x, y, z);
      for (FemNode3d n : fem.getNodes()) {
         if (pos.epsilonEquals (n.getPosition(), EPS)) {
            return n;
         }
      }
      FemNode3d n = new FemNode3d (pos);
      fem.addNode (n);
      return n;
   }
   
   private boolean near (double x, double y) {
      return Math.abs(x-y) <= EPS;
   }

   /**
    * For testing purposes, create a FemModel consisting of a 2 x 2 x 2 grid of
    * hex elements and a 4 x 4 grid of quad shell elements in the x-y plane,
    * centered at the origin, with the hexes and quad shells having unit
    * widths.
    */
   private FemModel3d createCombinedShellVolumeModel () {
      FemModel3d fem = new FemModel3d();
      FemFactory.createHexGrid (fem, 2, 2, 2, 2, 2, 2);

      // number and width of shell elements in X and Y
      int numX = 4;
      double widthX = 1;
      int numY = 4;
      double widthY = 1;

      for (int i=0; i<numX; i++) {
         for (int j=0; j<numY; j++) {
            double x0 = i*widthX - (numX*widthX)/2;
            double x1 = x0 + widthX;
            double y0 = j*widthY - (numY*widthY)/2;
            double y1 = y0 + widthY;
            double z = 0;
            
            FemNode3d n0 = getOrCreateNode (fem, x0, y0, z);
            FemNode3d n1 = getOrCreateNode (fem, x1, y0, z);
            FemNode3d n2 = getOrCreateNode (fem, x1, y1, z);
            FemNode3d n3 = getOrCreateNode (fem, x0, y1, z);
            ShellQuadElement e = new ShellQuadElement (n0, n1, n2, n3, 0.01);
            fem.addShellElement (e);
         }
      }
      double xmax = numX*widthX/2;
      double ymax = numY*widthY/2;
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = n.getPosition();
         if (near (Math.abs(p.x), xmax) && near (Math.abs(p.y), ymax)) {
            // corner node
            n.setDynamic (false);
         }
      }
      return fem;
   }

   private enum ElemType {
      ANY,
      SHELL,
      VOLUME
   };

   private ElemType elementType (FemElement3dBase e) {
      return (e instanceof ShellElement3d ? ElemType.SHELL : ElemType.VOLUME);
   }

   private String elementDescription (ElemType etype, int num) {
      
      return "" + etype + " element " + num;
   }

   private String elementDescription (FemElement3dBase e) {
      return elementDescription (elementType(e), e.getNumber());
   }

   private void testFindElem (
      FemModel3d fem, ElemType type, double x, double y, double z, 
      double chkx, double chky, double chkz, ElemType chkType, int chkNum) {

      Point3d pos = new Point3d (x, y, z);
      Point3d loc = new Point3d ();
      Point3d chk = new Point3d (chkx, chky, chkz);
      FemElement3dBase e = null;
      switch (type) {
         case ANY: {
            e = fem.findNearestElement (loc, pos);
            break;
         }
         case SHELL: {
            e = fem.findNearestShellElement (loc, pos);
            break;
         }
         case VOLUME: {
            e = fem.findNearestVolumetricElement (loc, pos);
            break;
         }
      }
      checkEquals ("location for nearest element", loc, chk, EPS);
      String edesc = elementDescription (e);
      if (chkNum == -1) {
         if (chkType != elementType(e)) {
            throw new TestException (
               "nearest element: expected "+chkType+", got " + edesc);
         }
      }
      else {
         String chkdesc = elementDescription (chkType, chkNum);
         if (!edesc.equals (chkdesc)) {
            throw new TestException (
               "nearest element: expected "+chkdesc+", got " + edesc);
         }
      }
   }

   // private boolean isCornerNode (FemNode3d n, double xmax, double ymax) {
   //    Point3d p = n.getPosition();
   //    if ((near(p.x,xmax) && near(p.y,ymax)) ||
   //        (near(p.x,-xmax) && near(p.y,ymax)) ||
   //        (near(p.x,-xmax) && near(p.y,-ymax)) ||
   //        (near(p.x,xmax) && near(p.y,-ymax))) {
   //       return true;
   //    }
   //    else {
   //       return false;
   //    }
   // }

   // /**
   //  * Create a Fem model with shell and volumetric elements so we can test fem
   //  * copy.
   //  */
   // private FemModel3d createCombinedShellFem () {
      
   //    FemModel3d fem = new FemModel3d();
   //    // Dimensions of volume model
   //    double volX = 5;       
   //    double volY = 5;
   //    double volZ = 10;    
 
   //    // Number of volume elements per X, Y and Z
   //    int numX = 3;
   //    int numY = 3;
   //    int numZ = 5;

   //    // number of shell elements (in X,Y) to build on either side of the volume
   //    int shellMargin = 3;

   //    FemFactory.createHexGrid (
   //       fem, volX, volY, volZ, numX, numY, numZ);

   //    // number and width of shell elements in X and Y
   //    int numShellX = numX+2*(shellMargin);
   //    double widthX = (volX/numX);
   //    int numShellY = numY+2*(shellMargin);
   //    double widthY = (volY/numY);
      
   //    for (int i=0; i<numShellX; i++) {
   //       for (int j=0; j<numShellY; j++) {
   //          double x0 = i*widthX - (numShellX*widthX)/2;
   //          double x1 = x0 + widthX;
   //          double y0 = j*widthY - (numShellY*widthY)/2;
   //          double y1 = y0 + widthY;
   //          double z = volZ/2;
            
   //          FemNode3d n0 = getOrCreateNode (fem, x0, y0, z);
   //          FemNode3d n1 = getOrCreateNode (fem, x1, y0, z);
   //          FemNode3d n2 = getOrCreateNode (fem, x1, y1, z);
   //          FemNode3d n3 = getOrCreateNode (fem, x0, y1, z);
   //          ShellQuadElement e = new ShellQuadElement (n0, n1, n2, n3, 0.01);
   //          fem.addShellElement (e);
   //       }
   //    }
   //    // make the corner nodes non-dynamic 
   //    double xmax = numShellX*widthX/2.0;
   //    double ymax = numShellY*widthY/2.0;
   //    for (FemNode3d n : fem.getNodes()) {
   //       if (isCornerNode (n, xmax, ymax)) {
   //          n.setDynamic (false);
   //       }
   //    }
   //    FemMeshComp shellSurfComp = FemMeshComp.createShellSurface ("shell", fem);
   //    fem.addMeshComp (shellSurfComp);
   //    return fem;
   // }

   private void checkModelsEqual (String msg, FemModel3d fem0, FemModel3d fem1) {
      if (!ComponentTestUtils.savedFilesAreEqual (fem0, fem1, true)) {
         throw new TestException (msg+" are not equal");
      }
   }

   private void testFemCopy() {
      FemModel3d fem = createCombinedShellVolumeModel();
      
      FemModel3d restFem = fem.copy (0, null);
      checkModelsEqual ("fem and restFem", fem, restFem);

      // advance the fem forward in time ...
      MechModel mech = new MechModel ();
      mech.addModel (fem);
      double t0 = 0;
      for (int i=1; i<=100; i++) {
         double t1 = 0.01*i;
         mech.preadvance (t0, t1, /*flags=*/0);
         mech.advance (t0, t1, /*flags=*/0);
         t0 = t1;
      }
      FemModel3d restFem2 = fem.copy (CopyableComponent.REST_POSITION, null);
      checkModelsEqual ("restFem and restFem2", restFem, restFem2);
   }

   private void testFindNearestElement() {
      FemModel3d fem = createCombinedShellVolumeModel();

      ElemType ANY = ElemType.ANY;
      ElemType SHELL = ElemType.SHELL;
      ElemType VOLUME = ElemType.VOLUME;

      testFindElem (fem, ANY,    0.0, 0.0, 9.0,   0.0, 0.0, 1.0, VOLUME, -1);
      testFindElem (fem, ANY,    0.5, 0.5, 9.0,   0.5, 0.5, 1.0, VOLUME, 7);
      testFindElem (fem, ANY,    9.0, 0.0, 0.0,   2.0, 0.0, 0.0, SHELL, -1);
      testFindElem (fem, ANY,    9.0, 0.5, 0.5,   2.0, 0.5, 0.0, SHELL, 14);
      testFindElem (fem, ANY,    9.0, 0.0, 9.0,   1.0, 0.0, 1.0, VOLUME, -1);
      testFindElem (fem, ANY,    9.0,-0.5, 9.0,   1.0,-0.5, 1.0, VOLUME, 5);

      testFindElem (fem, SHELL,  0.0, 0.0, 9.0,   0.0, 0.0, 0.0, SHELL, -1);
      testFindElem (fem, SHELL,  0.5, 0.5, 9.0,   0.5, 0.5, 0.0, SHELL, 10);
      testFindElem (fem, SHELL,  9.0, 0.0, 0.0,   2.0, 0.0, 0.0, SHELL, -1);
      testFindElem (fem, SHELL,  9.0, 0.5, 0.5,   2.0, 0.5, 0.0, SHELL, 14);
      testFindElem (fem, SHELL,  9.0, 0.0, 9.0,   2.0, 0.0, 0.0, SHELL, -1);
      testFindElem (fem, SHELL,  9.0,-0.5, 9.0,   2.0,-0.5, 0.0, SHELL, 13);

      testFindElem (fem, VOLUME, 0.0, 0.0, 9.0,   0.0, 0.0, 1.0, VOLUME, -1);
      testFindElem (fem, VOLUME, 0.5, 0.5, 9.0,   0.5, 0.5, 1.0, VOLUME, 7);
      testFindElem (fem, VOLUME, 9.0, 0.0, 0.0,   1.0, 0.0, 0.0, VOLUME, -1);
      testFindElem (fem, VOLUME, 9.0, 0.5, 0.5,   1.0, 0.5, 0.5, VOLUME, 7);
      testFindElem (fem, VOLUME, 9.0, 0.0, 9.0,   1.0, 0.0, 1.0, VOLUME, -1);
      testFindElem (fem, VOLUME, 9.0,-0.5, 9.0,   1.0,-0.5, 1.0, VOLUME, 5);
   }

   public void test() {
      //testFrameRelativeMass();
      testFindNearestElement();
      testFemCopy();
   }

   public static void main (String[] args) {
      FemModel3dTest tester = new FemModel3dTest();
      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }

}
