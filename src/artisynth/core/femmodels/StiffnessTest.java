/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.util.RandomGenerator;
import artisynth.core.materials.*;
/**
 * Tests the stiffness matrix for a tet element.
 */ 
public class StiffnessTest {

   FemMuscleModel fem;
   FemNode3d[] nodes;
   TetElement tet;
   MuscleBundle mus;

   public Vector3d[] getShapeGradient () {
      IntegrationPoint3d pt = tet.getIntegrationPoints()[0];
      IntegrationData3d dt = tet.getIntegrationData()[0];
      Matrix3d invJ = new Matrix3d();
      double detJ = pt.computeInverseJacobian (invJ, tet.getNodes());
//      pt.computeJacobianAndGradient (tet.getNodes(), dt.myInvJ0);
//      double detJ = pt.computeInverseJacobian();
      double dv = detJ*pt.getWeight();
      Vector3d[] tmp = pt.updateShapeGradient(invJ);

      Vector3d[] GNx = new Vector3d[4];
      for (int i=0; i<4; i++) {
         GNx[i] = new Vector3d(tmp[i]);
      }
      return GNx;
   }

   public double getDeltaVolume() {
      IntegrationPoint3d pt = tet.getIntegrationPoints()[0];
//      IntegrationData3d dt = tet.getIntegrationData()[0];
//      pt.computeJacobianAndGradient (tet.getNodes(), dt.myInvJ0);
      double detJ = pt.computeJacobianDeterminant(tet.getNodes());
      return detJ*pt.getWeight();
   }

   public MatrixNd getGeometricStiffness() {
      Vector3d[] GNx = getShapeGradient();
      double dv = getDeltaVolume();
      MatrixNd KG = new MatrixNd (12,12);

      Vector3d tmp = new Vector3d();
      for (int i=0; i<4; i++) {
         for (int j=0; j<4; j++) {
            double Kg = tmp.dot(GNx[i])*dv;
            KG.set (3*i+0, 3*j+0, Kg);
            KG.set (3*i+1, 3*j+1, Kg);
            KG.set (3*i+2, 3*j+2, Kg);
         }
      }
      KG.negate();
      return KG;
   }

   public MatrixNd getNumericalGeometricStiffness (double h) {

      fem.updateForces(0);
      int size = fem.getActiveVelStateSize();
      MatrixNd NG = new MatrixNd (size, size);
      VectorNd f0 = new VectorNd (size);
      VectorNd fj = new VectorNd (size);
      VectorNd x0 = new VectorNd (size);
      VectorNd xj = new VectorNd (size);

      fem.updateForces (0);
      fem.getActiveForces (f0);
      fem.getActivePosState (x0);

      Vector3d tmp = new Vector3d();
      Vector3d[] GNx = getShapeGradient();
      double dv0 = getDeltaVolume();
      for (int i=0; i<GNx.length; i++) {
         tmp.scale (dv0, GNx[i]);
         tmp.scale (-1);
         f0.set (i*3+0, tmp.x);
         f0.set (i*3+1, tmp.y);
         f0.set (i*3+2, tmp.z);
      }

      for (int j=0; j<size; j++) {
         xj.set (x0);
         xj.set (j, xj.get(j) + h);
         fem.setActivePosState (xj);
         fem.updateForces(0);
         GNx = getShapeGradient();
         double dv = getDeltaVolume();
         for (int i=0; i<GNx.length; i++) {
            tmp.scale (-dv);
            fj.set (i*3+0, tmp.x);
            fj.set (i*3+1, tmp.y);
            fj.set (i*3+2, tmp.z);
         }
         fj.sub (f0);
         fj.scale (1/h);
         NG.setColumn (j, fj);
      }
      fem.setActivePosState (x0);
      return NG;
   }

   public MatrixNd getNumericalMaterialStiffness (double h) {

      fem.updateForces(0);
      int size = fem.getActiveVelStateSize();
      MatrixNd NM = new MatrixNd (size, size);
      VectorNd f0 = new VectorNd (size);
      VectorNd fj = new VectorNd (size);
      VectorNd x0 = new VectorNd (size);
      VectorNd xj = new VectorNd (size);
      
      fem.updateForces (0);
      fem.getActiveForces (f0);
      fem.getActivePosState (x0);

      Vector3d tmp = new Vector3d();
      Vector3d[] GNx = getShapeGradient();
      double dv0 = getDeltaVolume();
      for (int i=0; i<GNx.length; i++) {
         tmp.scale (dv0, GNx[i]);
         tmp.scale (-1);
         f0.set (i*3+0, tmp.x);
         f0.set (i*3+1, tmp.y);
         f0.set (i*3+2, tmp.z);
      }

      for (int j=0; j<size; j++) {
         xj.set (x0);
         xj.set (j, xj.get(j) + h);
         fem.setActivePosState (xj);
         fem.updateForces(0);
         // GNx = getShapeGradient();
         double dv = getDeltaVolume();
         for (int i=0; i<GNx.length; i++) {
            tmp.scale (-dv0);
            fj.set (i*3+0, tmp.x);
            fj.set (i*3+1, tmp.y);
            fj.set (i*3+2, tmp.z);
         }
         fj.sub (f0);
         fj.scale (1/h);
         NM.setColumn (j, fj);
      }
      fem.setActivePosState (x0);
      return NM;
   }

   public MatrixNd getNumericalStiffness (double h) {
      int size = fem.getActiveVelStateSize();
      MatrixNd N = new MatrixNd (size, size);
      VectorNd f0 = new VectorNd (size);
      VectorNd fj = new VectorNd (size);
      VectorNd x0 = new VectorNd (size);
      VectorNd xj = new VectorNd (size);

      fem.updateForces (0);
      fem.getActiveForces (f0);
      fem.getActivePosState (x0);
      
      for (int j=0; j<size; j++) {
         xj.set (x0);
         xj.set (j, xj.get(j) + h);
         fem.setActivePosState (xj);
         fem.updateForces (0);
         fem.getActiveForces (fj);

         fj.sub (f0);
         fj.scale (1/h);
         N.setColumn (j, fj);
      }
      fem.setActivePosState (x0);
      return N;
   }

   private double matrixError (MatrixNd M1, MatrixNd M2) {
      double norm = Math.max(M1.frobeniusNorm(), M2.frobeniusNorm());

      MatrixNd MD = new MatrixNd (M1);
      MD.sub (M2);
      if (norm == 0) {
         return 0;
      }
      else {
         return MD.frobeniusNorm()/norm;
      }
   }

   private void testStiffness () {
      SparseBlockMatrix S = fem.getActiveStiffnessMatrix();

      // Vector3d[] GNx = getShapeGradient();
      // for (int i=0; i<4; i++) {
      //    System.out.println (GNx[i].toString ("%8.3f"));
      // }
      
      // System.out.println ("stress=\n" + getStress().toString("%10.5f"));

      // System.out.println ("S=\n" + S.toString ("%12.7f"));
      
      MatrixNd KG = getGeometricStiffness();
      MatrixNd KM = new MatrixNd(S);
      KM.sub (KG);

      MatrixNd N = getNumericalStiffness (1e-8);

      // MatrixNd NG = getNumericalGeometricStiffness (1e-8);
      // MatrixNd NM = getNumericalMaterialStiffness (1e-8);

      // System.out.println ("KG=\n" + KG.toString ("%12.7f"));
      // System.out.println ("KM=\n" + KM.toString ("%12.7f"));
      //System.out.println ("NG=\n" + NG.toString ("%12.7f"));
      //System.out.println ("NM=\n" + NM.toString ("%12.7f"));

      //System.out.println ("NM symmetric=" + NM.isSymmetric(1e-6));
      //System.out.println ("NG symmetric=" + NG.isSymmetric(1e-6));

      // MatrixNd NX = new MatrixNd (NM);
      // NX.add (NG);
      // System.out.println ("NX Error=" + matrixError (new MatrixNd(S), NX));

      // System.out.println ("N=\n" + N.toString ("%12.7f"));
      
      System.out.println ("Error=" + matrixError (new MatrixNd(S), N));
   }      

   public void testMaterial (FemMaterial mat) {
      fem.setMaterial (mat);
      System.out.println ("testing " + mat + ":");
      testStiffness();
   }

   public void testMuscleMaterial (
      MuscleMaterial mat, Vector3d dir, double excitation) {

      System.out.println ("testing " + mat + ":");
      //fem.setMaterial (new MooneyRivlinMaterial (0, 0, 0, 0, 0, 0));
      //fem.setMaterial (new IncompressibleMaterial (1e7));
      fem.setMaterial (new IncompressibleMaterial (0));
      fem.setMuscleMaterial (mat);
      mus.setExcitation (excitation);
      MuscleElementDesc desc = new MuscleElementDesc (tet, dir);
      mus.addElement (desc);

      double[] xpos = new double[] { 0.3, 0.5, 0.8, 1.2, 1.5, 1.6, 2 };

      testStiffness();
      for (int i=0; i<xpos.length; i++) {
         nodes[1].setPosition (xpos[i], 0, 0);
         testStiffness();         
      }

      mus.removeElement (desc);
   }   

   private void perturbNode (FemNode3d node, double scale) {
      Point3d vec = new Point3d();
      vec.setRandom ();
      vec.scale (scale);
      vec.add (node.getPosition());
      node.setPosition (vec);
   }

   public StiffnessTest () {
      fem = new FemMuscleModel();
      fem.setDensity (1.0);
      fem.setGravity (Vector3d.ZERO);
      nodes = new FemNode3d[4];
      nodes[0] = new FemNode3d (0, 0, 0);
      nodes[1] = new FemNode3d (1, 0, 0);
      nodes[2] = new FemNode3d (0, 1, 0);
      nodes[3] = new FemNode3d (0, 0, 1);
      fem.addNode (nodes[0]);
      fem.addNode (nodes[1]);
      fem.addNode (nodes[2]);
      fem.addNode (nodes[3]);

      for (int i=0; i<nodes.length; i++) {
         perturbNode (nodes[i], 0.2);
         System.out.println ("x"+i+"=" + nodes[i].getPosition());
      }
      
      // use to invert element
      //nodes[1].setPosition (new Point3d (-0.1, 0, 0));

      tet = new TetElement (nodes[0], nodes[1], nodes[2], nodes[3]);
      fem.addElement (tet);      

      mus = new MuscleBundle ("muscle");
      fem.addMuscleBundle (mus);
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      StiffnessTest tester = new StiffnessTest();

      MooneyRivlinMaterial moonriv =
         new MooneyRivlinMaterial (1.2, 3.4, 0, 0, 0, 0);
      // moonriv.setJLimit (0.2);
   
      tester.testMaterial (moonriv);
      //tester.testMaterial (new ConstantStress (10, 0, 0, 0, 0, 0));
      tester.testMaterial (new StVenantKirchoffMaterial (12, 0.3));
      tester.testMaterial (new NeoHookeanMaterial ());
      tester.testMaterial (new IncompNeoHookeanMaterial (123.0, 666.0));

      tester.testMaterial (
          new OgdenMaterial (
           new double[] {3e5, 1e4, 2e3, 3e3, 4e3, 1.5e2},
           new double[] {1, 2, 3, 4, 5, 6},
           1e5));

      tester.testMaterial (new OgdenMaterial ());
      tester.testMuscleMaterial (
         new GenericMuscle (1.4, 3e5, 0.05, 6.6), Vector3d.X_UNIT, 1.0);
      // //tester.testMuscleMaterial (
      // //  new GenericMuscle (0, 1e3, 0, 0, 0), Vector3d.X_UNIT, 0.1);
      // tester.testMuscleMaterial (
      //    new FullBlemkerMuscle (1.4, 1.0, 3e5, 0.05, 6.6, 0, 0),
      //     Vector3d.X_UNIT, 0.0);
      tester.testMuscleMaterial (
         new BlemkerMuscle (1.4, 1.0, 3e5, 0.05, 6.6), Vector3d.X_UNIT, 1.0);

      //tester.testMaterial (new StVenantKirchoffMaterial (10, 0));
      //tester.testMaterial (new IncompressibleMaterial (10));

   }

}
