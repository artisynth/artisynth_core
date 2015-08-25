/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;

/** 
 * Implements stiffness warping for a particular 3d fem element.
 *
 * <p>Note: it is important that all these methods are called from the same
 * (simulation) thread. In particular, they should not be called by methods
 * activated by the GUI.
 */
public class StiffnessWarper3d {
 
   Matrix3d[][] K0;
   Vector3d[] f0;

   // A and R are used to compute stiffness warping. 
   protected Matrix3d A = new Matrix3d();
   protected Matrix3d R = new Matrix3d();
   protected Vector3d tmp = new Vector3d();
   protected Vector3d pos = new Vector3d();
   protected SVDecomposition3d SVD = new SVDecomposition3d();

   protected Matrix3d J0inv = new Matrix3d();
   protected double myConditionNum = 0;

   public StiffnessWarper3d (int numNodes) {
      K0 = new Matrix3d[numNodes][numNodes];
      f0 = new Vector3d[numNodes];

      for (int i=0; i<numNodes; i++) {
         for (int j=0; j<numNodes; j++) {
            K0[i][j] = new Matrix3d();
         }
         f0[i] = new Vector3d();
      }
   }

   public void computeInitialStiffness (FemElement3d e, double E, double nu) {

      IntegrationPoint3d[] ipnts = e.getIntegrationPoints();
      IntegrationData3d[] idata = e.getIntegrationData();

      Matrix6d D = new Matrix6d(); // fill in ...
      double dia = (1 - nu) / (1 - 2 * nu);
      double off = nu / (1 - 2 * nu);

      D.m00 = dia; D.m01 = off; D.m02 = off;
      D.m10 = off; D.m11 = dia; D.m12 = off;
      D.m20 = off; D.m21 = off; D.m22 = dia;
      D.m33 = 0.5;
      D.m44 = 0.5;
      D.m55 = 0.5;
      D.scale (E/(1+nu));


      for (int i=0; i<e.myNodes.length; i++) {
         for (int j=0; j<e.myNodes.length; j++) {
            K0[i][j].setZero();
         }
         f0[i].setZero();
      }

      for (int k=0; k<ipnts.length; k++) {
         IntegrationPoint3d pt = ipnts[k];
         double dv = idata[k].myDetJ0*pt.getWeight();
         Vector3d[] GNx = pt.updateShapeGradient(idata[k].myInvJ0);

         for (int i=0; i<e.myNodes.length; i++) {
            for (int j=0; j<e.myNodes.length; j++) {
               FemUtilities.addMaterialStiffness (
                  K0[i][j], GNx[i], D, SymmetricMatrix3d.ZERO, GNx[j], dv);
            }
         }
      }      
      for (int i=0; i<e.myNodes.length; i++) {
         tmp.setZero();
         for (int j=0; j<e.myNodes.length; j++) {
            K0[i][j].mulAdd (tmp, e.myNodes[j].myRest, tmp);
//             if (e.getNumber() == 0 && i<4 && j<4) {
//                System.out.println ("K0["+i+"]["+j+"]");
//                System.out.println (K0[i][j].toString("%10.5f"));
//             }
         }
         f0[i].set (tmp);
      }
   }

   public void setInitialJ (
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {

      tmp.sub (n1.myRest, n0.myRest);
      A.setColumn (0, tmp);
      tmp.sub (n2.myRest, n0.myRest);
      A.setColumn (1, tmp);
      tmp.sub (n3.myRest, n0.myRest);
      A.setColumn (2, tmp);

      J0inv.invert (A);
      myConditionNum = A.infinityNorm() * J0inv.infinityNorm();
   }

   public double getConditionNum () {
      return myConditionNum;
   }

   public void computeWarping (
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {
      
      tmp.sub (n1.getLocalPosition(), n0.getLocalPosition());
      A.setColumn (0, tmp);
      tmp.sub (n2.getLocalPosition(), n0.getLocalPosition());
      A.setColumn (1, tmp);
      tmp.sub (n3.getLocalPosition(), n0.getLocalPosition());
      A.setColumn (2, tmp);

      A.mul (J0inv);
      computeRotation (A, null);
   }

   public void computeRotation (Matrix3d F, SymmetricMatrix3d P) {
      try {
         SVD.factor (F);
      }
      catch (Exception e) {
         System.out.println ("F=\n" + F.toString ("%g"));
         R.setIdentity();
      }
      Matrix3d U = SVD.getU();
      Matrix3d V = SVD.getV();
      SVD.getS (tmp);

      double detU = U.orthogonalDeterminant();
      double detV = V.orthogonalDeterminant();
      if (detV * detU < 0) { /* then one is negative and the other positive */
         if (detV < 0) { /* negative last column of V */
            V.m02 = -V.m02;
            V.m12 = -V.m12;
            V.m22 = -V.m22;
         }
         else /* detU < 0 */
         { /* negative last column of U */
            U.m02 = -U.m02;
            U.m12 = -U.m12;
            U.m22 = -U.m22;
         }
         tmp.z = -tmp.z;
      }
      R.mulTransposeRight (U, V);
      if (P != null) {
         // place the symmetric part in P
         P.mulDiagTransposeRight (V, tmp);
//          Matrix3d C = new Matrix3d();
//          C.mul (R, P);
//          C.sub (F);
//          double max = tmp.infinityNorm();
//          if (C.frobeniusNorm() > 1e-14*max) {
//             System.out.println ("Error!!! " + C.frobeniusNorm());
//          }
      }
   }

   public void addNodeStiffness (Matrix3d Kij, int i, int j, boolean warping) {
      if (warping) {
         A.mulTransposeRight (K0[i][j], R);
         A.mul (R, A);
         Kij.add (A);
      }
      else {
         Kij.add (K0[i][j]);
      }
   }

   public void addNodeStiffness (
      FemNodeNeighbor nbr, int i, int j, boolean warping) {
      if (warping) {
         A.mulTransposeRight (K0[i][j], R);
         A.mul (R, A);
         nbr.addStiffness (A);
      }
      else {
         nbr.addStiffness (K0[i][j]);
      }
   }

   public void addNodeForce (
      Vector3d f, int i, FemNode3d[] nodes, boolean warping) {
      if (warping) {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            R.mulTranspose (pos, nodes[j].getLocalPosition());
            K0[i][j].mulAdd (tmp, pos, tmp);
         }
         tmp.sub (f0[i]);
         R.mul (tmp, tmp);
         f.add (tmp);
      }
      else {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            K0[i][j].mulAdd (tmp, nodes[j].getLocalPosition(), tmp);
         }
         tmp.sub (f0[i]);
         f.add (tmp);
      }
   }
   
   // required for static analysis
   public void addNodeForce0(Vector3d f, int i, boolean warping) {
      if (warping) {
         R.mul(tmp, f0[i]);
         f.add (tmp);
      } else {
         f.add(f0[i]);
      }
   }
   
   // required for static analysis
   public void addNodeForce0(VectorNd f, int offset, int i, boolean warping) {
      if (warping) {
         R.mul(tmp, f0[i]);
         f.set(offset, f.get(offset) + tmp.x);
         f.set(offset+1, f.get(offset+1) + tmp.y);
         f.set(offset+2, f.get(offset+2) + tmp.z);
      } else {
         f.set(offset, f.get(offset) + f0[i].x);
         f.set(offset+1, f.get(offset+1) + f0[i].y);
         f.set(offset+2, f.get(offset+2) + f0[i].z);
      }
   }
}
