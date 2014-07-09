/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3x1Block;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix6d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNd;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FemUtilities;

public class MFreeStiffnessBlock {
   
   MFreeNode3d myRowNode;
   MFreeNode3d myColNode;
   
   Matrix3d myK;
   // Extra K matrix components for which we don't want to apply stiffness damping
   private Matrix3d myKX; 
   
   Matrix3x3Block myBlk;
   int myBlkNum;
   int myRefCnt;
   // block used in the incompressibility constraint matrix.
   Matrix3x1Block myDivBlk;
   // Matrix3x1Block myDivBlk1;

   public void zeroStiffness() {
      myK.setZero();
      if (myKX != null) {
         myKX.setZero();
      }
   }

   public void addStiffness (Matrix3d K) {
      myK.add (K);
   }

   public void getStiffness (Matrix3d K){
      K.set (myK);
      if (myKX != null) {
         K.add (myKX);
      }
   }
   
   public Matrix3d getK() {
      return myK;
   }

//    public void addNondampedStiffness (Matrix3d K) {
//       myKX.add (K);
//    }

   /** 
    * Sets the stiffness components of this node neighbour to the transpose of
    * the stiffness components of another node neighbour. 
    */
   public void setTransposedStiffness (MFreeStiffnessBlock nbr) {
      myK.transpose (nbr.myK);
      if (nbr.myKX != null) {
         if (myKX == null) {
            myKX = new Matrix3d();
         }
         myKX.transpose (nbr.myKX);
      }
   }

   public MFreeStiffnessBlock (MFreeNode3d row, MFreeNode3d col) {
      myRowNode = row;
      myColNode = col;
      myK = new Matrix3d();
      myRefCnt = 1;
   }

   public void setBlock (Matrix3x3Block blk) {
      myBlk = blk;
   }
   
   public void setBlockNumber (int num) {
      myBlkNum = num;
   }

   public void addVelJacobian (
      double s, double stiffnessDamping, double massDamping) {
      addVelJacobian (myBlk, s, stiffnessDamping, massDamping);
   }

   public void addVelJacobian (
      Matrix3d blk, double s, double stiffnessDamping, double massDamping) {
      // System.out.println (
      // "addVelJacobian: myK=\n" + myK.toString("%10.5f"));
      blk.scaledAdd (-s * stiffnessDamping, myK, blk);
      //blk.scaledAdd (-s * stiffnessDamping, myKX, blk);
      if (massDamping != 0) {
         //XXX not sure if should be row or col
         //double d = -s * massDamping * myNode.getEffectiveMass();
         double d = -s * massDamping * myColNode.getMass();
         blk.m00 += d;
         blk.m11 += d;
         blk.m22 += d;
      }
   }

   public MFreeNode3d getRowNode() {
      return myRowNode;
   }
   
   public MFreeNode3d getColNode() {
      return myColNode;
   }

//   public void addPosJacobian (SparseNumberedBlockMatrix S, double s) {
//      addPosJacobian ((Matrix3x3Block)S.getBlockByNumber(myBlkNum), s);
//   }

   public void addPosJacobian (Matrix3d blk, double s) {
      blk.scaledAdd (-s, myK, blk);
      if (myKX != null) {
         blk.scaledAdd (-s, myKX, blk);
      }
   }

   public void addDampingForce (Vector3d fd) {
      // XXX not sure if should be row or col
      fd.mulAdd (myK, myColNode.getFalseVelocity(), fd);
   }

   public void addDilationalStiffness (
      double kp, Vector3d intGi, Vector3d intGj) {

      if (FemModel3d.noIncompressStiffnessDamping) {
         if (myKX == null) {
            myKX = new Matrix3d();
         }
         FemUtilities.addDilationalStiffness (myKX, kp, intGi, intGj);
      }
      else {
         FemUtilities.addDilationalStiffness (myK, kp, intGi, intGj);
      }
      
   }
   
   public void addDilationalStiffness (
      double[] Kp, MatrixBlock GT_i, MatrixBlock GT_j) {

      if (FemModel3d.noIncompressStiffnessDamping) {
         if (myKX == null) {
            myKX = new Matrix3d();
         }
         FemUtilities.addDilationalStiffness (myKX, Kp, GT_i, GT_j);
      }
      else {
         FemUtilities.addDilationalStiffness (myK, Kp, GT_i, GT_j);
      }
      
   }
   
   public void addDilationalStiffness (
      MatrixNd Rinv, MatrixBlock GT_i, MatrixBlock GT_j) {

      if (FemModel3d.noIncompressStiffnessDamping) {
         if (myKX == null) {
            myKX = new Matrix3d();
         }
         FemUtilities.addDilationalStiffness (myKX, Rinv, GT_i, GT_j);
      }
      else {
         FemUtilities.addDilationalStiffness (myK, Rinv, GT_i, GT_j);
      }
      
   }
   
   public void addIncompressibilityStiffness (
      double s, Vector3d intGi, Vector3d intGj) {

      if (FemModel3d.noIncompressStiffnessDamping) {
         if (myKX == null) {
            myKX = new Matrix3d();
         }
         FemUtilities.addIncompressibilityStiffness (myKX, s, intGi, intGj);
      }
      else {
         FemUtilities.addIncompressibilityStiffness (myK, s, intGi, intGj);
      }
      
   }

   public void addMaterialStiffness (
      Vector3d gi, Matrix6d D, double p,
      SymmetricMatrix3d sig, Vector3d gj, double dv) {

      FemUtilities.addMaterialStiffness (myK, gi, D, sig, gj, dv);      
      if (FemModel3d.noIncompressStiffnessDamping) {
         if (myKX == null) {
            myKX = new Matrix3d();
         }
         FemUtilities.addPressureStiffness (myKX, gi, p, gj, dv);  
         FemUtilities.addPressureStiffness (myK, gi, -p, gj, dv);  
      }
      
   }
   
}
