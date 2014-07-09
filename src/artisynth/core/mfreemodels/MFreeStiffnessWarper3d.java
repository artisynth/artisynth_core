/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import artisynth.core.femmodels.FemNodeNeighbor;
import artisynth.core.femmodels.FemUtilities;
import artisynth.core.femmodels.IntegrationData3d;

/** 
 * Implements stiffness warping for a particular integration region.
 *
 * <p>Note: it is important that all these methods are called from the same
 * (simulation) thread. In particular, they should not be called by methods
 * activated by the GUI.
 */
public class MFreeStiffnessWarper3d {
 
   Matrix3d[][] K0;
   MFreeNode3d[] myNodes;
   MFreeElement3d myElem;
   
   // A and R are used to compute stiffness warping. 
   protected Matrix3d A = new Matrix3d();
   protected Matrix3d R = new Matrix3d();
   protected Vector3d tmp = new Vector3d();
   protected Vector3d pos = new Vector3d();
   protected SVDecomposition3d SVD = new SVDecomposition3d();

   protected double myConditionNum = 0;

   public MFreeStiffnessWarper3d (int numNodes) {
      K0 = new Matrix3d[numNodes][numNodes];
      
      for (int i=0; i<numNodes; i++) {
         for (int j=0; j<numNodes; j++) {
            K0[i][j] = new Matrix3d();  
         }
      }
   }

   public void computeInitialStiffness (MFreeElement3d e, double E, double nu) {

      ArrayList<MFreeIntegrationPoint3d> ipnts = e.getIntegrationPoints();
      ArrayList<IntegrationData3d> idata = e.getIntegrationData();
      ArrayList<int[]> idxs = e.getIntegrationIndices();
      VectorNd iwgts = e.getIntegrationWeights();
      myNodes = e.getNodes();
      myElem = e;
      
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
      }
      
      for (int k=0; k<ipnts.size(); k++) {
         MFreeIntegrationPoint3d pt = ipnts.get(k);
         IntegrationData3d idat = idata.get(k);
         int[] ids = idxs.get(k);
         double dv = iwgts.get(k)*idat.getDetJ0();
         Vector3d[] GNx = pt.updateShapeGradient(idat.getInvJ0());
         
         for (int i=0; i<e.myNodes.length; i++) {
            for (int j=0; j<e.myNodes.length; j++) {
               Matrix3d KA = new Matrix3d();
               
               FemUtilities.addMaterialStiffness (
                  KA, GNx[ids[i]], D, SymmetricMatrix3d.ZERO, GNx[ids[j]], dv);
               K0[i][j].add(KA);
               
//               if (e.myNodes[i] == 0)
               
//               
//               String pntStr = "ipnt(" + (pt.getID()+1) + "," + (e.myNodes[i].getNumber()+1) + "," + (e.myNodes[j].getNumber()+1) + "," + (e.getNumber()+1) + ","+
//                  (e.getGrandParent().getNumber()+1) + ")";
//               System.out.println(pntStr + ".dv = " + dv + ";");
//               System.out.println(pntStr + ".K0 = [" + KA + "];");
//               System.out.println(pntStr + ".gi = [" + GNx[ids[i]] + "];");
//               System.out.println(pntStr + ".gj = [" + GNx[ids[j]] + "];");
               
            }
         }
      }      
   }
   
   public Matrix3d[][] getInitialStiffnesses() {
      return K0;
   }
   
   public Matrix3d getRotation() {
      return R;
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
      }
   }

   public void addNodeStiffness(Matrix3d Kij, boolean[][] active, int i, int j, boolean warping) {
      
      if (active[i][j]) {
         if (warping) {
            A.mulTransposeRight (K0[i][j], R);
            A.mul (R, A);
            Kij.add (A);
         }
         else {
            Kij.add (K0[i][j]);
         }
      }
   }
   
   public void addNodeStiffness (FemNodeNeighbor nbr, boolean [][] active, int i, int j, boolean warping) {
      
      if (active[i][j]) {
         if (warping) {
            A.mulTransposeRight (K0[i][j], R);
            A.mul (R, A);
            nbr.addStiffness (A);
         }
         else {
            
//            String pntStr = "stiff(" + (myElem.myNodes[i].getNumber()+1) + "," + (myElem.myNodes[j].getNumber()+1) + "," + (myElem.getNumber()+1) + ","+
//               (myElem.getGrandParent().getNumber() + 1) + ")";
//            System.out.println(pntStr + ".K = [" + K0[i][j] + "];");
//            System.out.println(pntStr + ".nbr = [" + (myElem.myNodes[i].getNumber()+1) + "," +(nbr.getNode().getNumber()+1) + "];");
            
            nbr.addStiffness (K0[i][j]);
            
            
            
            
         }
      }
   }

   public void addNodeForce (
      Vector3d f, boolean[][] active, int i, MFreeNode3d[] nodes, boolean warping, FemNodeNeighbor[] nbr) {

      if (warping) {
         tmp.setZero();
         //R.setIdentity();
         for (int j=0; j<nodes.length; j++) {
            if (active[i][j]) { 
               R.mulTranspose (pos, nodes[j].getFalsePosition());
               pos.sub(nodes[j].getRestPosition());
               // R.mulTranspose (pos, nodes[j].getFalseDisplacement());
               K0[i][j].mulAdd (tmp, pos, tmp);
            }
         }
         R.mul (tmp, tmp);
         f.add (tmp);
         
      }
      else {
         tmp.setZero();
         for (int j=0; j<nodes.length; j++) {
            if (active[i][j]) {
               Vector3d tmpF = new Vector3d();
               nodes[j].getFalseDisplacement(pos);
               K0[i][j].mulAdd (tmp, pos, tmp);
               K0[i][j].mul(tmpF,pos);
               
            }
         }
         f.add (tmp);
      }
   }
}
