/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;

public class FemNodeNeighbor {
   protected FemNode3d myNode;
   protected Matrix3d myK;

   // Extra K matrix components for directors in shell nodes
   protected Matrix3d myK01; 
   protected Matrix3d myK10;
   protected Matrix3d myK11;

   // Extra K matrix components for which we don't want to apply stiffness damping
   protected Matrix3d myKX; 
   
   //protected Matrix3x3Block myBlk;
   protected int myBlkNum;
   protected int myBlkNum01;
   protected int myBlkNum10;
   protected int myBlkNum11;
   protected int myRefCnt;
   // block used in the incompressibility constraint matrix.
   protected Matrix3x1Block myDivBlk;
   // Matrix3x1Block myDivBlk1;

   public void zeroStiffness() {
      myK.setZero();
      if (myKX != null) {
         myKX.setZero();
      }
      if (myK01 != null) {
         myK01.setZero();
         myK10.setZero();
         myK11.setZero();
      }
   }
   
   protected void allocateDirectorStiffness() {
      myK01 = new Matrix3d();
      myK10 = new Matrix3d();
      myK11 = new Matrix3d();
   }
   
   protected void deallocateDirectorStiffness() {
      myK01 = null;
      myK10 = null;
      myK11 = null;
   }
   
   public Matrix3d getK()  {
      return myK;
   }
   
   public Matrix3x1Block getDivBlk() {
      return myDivBlk;
   }
   
   public void setDivBlk(Matrix3x1Block blk) {
      myDivBlk = blk;
   }

   /** 
    * Sets the stiffness components of this node neighbour to the transpose of
    * the stiffness components of another node neighbour. 
    */
   public void setTransposedStiffness (FemNodeNeighbor nbr) {
      myK.transpose (nbr.myK);
      if (nbr.myKX != null) {
         if (myKX == null) {
            myKX = new Matrix3d();
         }
         myKX.transpose (nbr.myKX);
      }
      if (nbr.myK01 != null) {
         if (myK01 == null) {
            allocateDirectorStiffness();
         }
         myK01.transpose (nbr.myK01);
         myK10.transpose (nbr.myK10);
         myK11.transpose (nbr.myK11);
      }
   }
   
   public void setRefCount(int count) {
      myRefCnt = count;
   }
   
   public void increaseRefCount() {
      myRefCnt++;
   }
   
   public void decreaseRefCount() {
      myRefCnt--;
   }
   

   public FemNodeNeighbor (FemNode3d node) {
      myNode = node;
      myK = new Matrix3d();
      myRefCnt = 1;
   }
   
   public void setBlockNumber (int num) {
      myBlkNum = num;
   }

   public int getBlockNumber() {
      return myBlkNum;
   }

   private int getOrCreateBlock (SparseNumberedBlockMatrix S, int bi, int bj) {
      if (bi != -1 && bj != -1) {
         Matrix3x3Block blk = (Matrix3x3Block)S.getBlock(bi, bj);
         if (blk == null) {
            blk = new Matrix3x3Block();
            S.addBlock(bi, bj, blk);
         }
         return blk.getBlockNumber();
      }
      else {
         return -1;
      }
   }

   private Matrix3d setKAsNeeded (Matrix3d K, int blkNum) {
      if (blkNum == -1) {
         return null;
      }
      else if (K != null) {
         return K;
      }
      else {
         return new Matrix3d();
      }
   }          

   public void addSolveBlocks (
      SparseNumberedBlockMatrix S, FemNode3d node) {
      
      int bi0 = node.getLocalSolveIndex();
      int bj0 = myNode.getLocalSolveIndex();
      int bi1 = node.getBackSolveIndex();
      int bj1 = myNode.getBackSolveIndex();

      myBlkNum = getOrCreateBlock (S, bi0, bj0);
      myBlkNum01 = getOrCreateBlock (S, bi0, bj1);
      myBlkNum10 = getOrCreateBlock (S, bi1, bj0);
      myBlkNum11 = getOrCreateBlock (S, bi1, bj1);

      myK01 = setKAsNeeded (myK01, myBlkNum01);
      myK10 = setKAsNeeded (myK10, myBlkNum10);
      myK11 = setKAsNeeded (myK11, myBlkNum11);         
   }

   private void addMassDamping (Matrix3d blk, double d) {
      blk.m00 += d;
      blk.m11 += d;
      blk.m22 += d;
   }

   public void addVelJacobian (
      SparseNumberedBlockMatrix S, FemNode3d node, double sm, double sk) {
    
      Matrix3x3Block blk;
      if (myBlkNum != -1) {
         blk = (Matrix3x3Block)S.getBlockByNumber(myBlkNum);
         blk.scaledAdd (sk, myK, blk);
         if (node == myNode && node.isActiveLocal()) {
            addMassDamping (blk, sm*myNode.getMass());
         }
      }
      if (myBlkNum01 != -1) {
         blk = (Matrix3x3Block)S.getBlockByNumber(myBlkNum01);
         blk.scaledAdd (sk, myK01, blk);

         if (myBlkNum10 != -1) {
            blk = (Matrix3x3Block)S.getBlockByNumber(myBlkNum10);
            blk.scaledAdd (sk, myK10, blk);
         }
         if (myBlkNum11 != -1) {
            blk = (Matrix3x3Block)S.getBlockByNumber(myBlkNum11);
            blk.scaledAdd (sk, myK11, blk);
            if (node == myNode && node.isActiveLocal()) {
               addMassDamping (blk, sm*myNode.getMass());
            }
         }
      }
   }

   public void addPosJacobian (
      SparseNumberedBlockMatrix S, FemNode3d node, double s) {
    
      Matrix3x3Block blk;
      if (myBlkNum != -1) {
         blk = (Matrix3x3Block)S.getBlockByNumber(myBlkNum);
         blk.scaledAdd (s, myK, blk);
         if (myKX != null) {
            blk.scaledAdd (s, myKX, blk);
         }
      }
      if (myBlkNum01 != -1) {
         blk = (Matrix3x3Block)S.getBlockByNumber(myBlkNum01);
         blk.scaledAdd (s, myK01, blk);

         if (myBlkNum10 != -1) {
            blk = (Matrix3x3Block)S.getBlockByNumber(myBlkNum10);
            blk.scaledAdd (s, myK10, blk);
         }
         if (myBlkNum11 != -1) {
            blk = (Matrix3x3Block)S.getBlockByNumber(myBlkNum11);
            blk.scaledAdd (s, myK11, blk);
         }
      }
   }

   /**
    * @deprecated 
    */
   public void addPosJacobian (Matrix3d blk, double s) {
      blk.scaledAdd (-s, myK, blk);
      if (myKX != null) {
         blk.scaledAdd (-s, myKX, blk);
      }
   }
   
   public FemNode3d getNode() {
      return myNode;
   }

   public void addDampingForce (Vector3d fd) {
      // XXX check that we want to use local velocity for this
      fd.mulAdd (myK, myNode.getLocalVelocity(), fd);
   }

   public void addStiffnessDampingForce (Vector3d fd, Vector3d fb) {
      // XXX check that we want to use local velocity for this
      fd.mulAdd (myK, myNode.getLocalVelocity(), fd);
      if (myK01 != null) {
         fd.mulAdd (myK01, myNode.myBackVel, fd);
      }
      if (fb != null) {
         fb.mulAdd (myK10, myNode.getLocalVelocity(), fb);
         fb.mulAdd (myK11, myNode.myBackVel, fb);
      }
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
      double kp, Matrix3x1 intGi, Matrix3x1 intGj) {

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
      addPressureStiffness(gi, p, gj, dv);
   }
   
   // Sanchez, April 27, 2013
   // Separated pressure term so I can compute incompressibility component separately
   public void addMaterialStiffness(Vector3d gi, Matrix6d D,
      SymmetricMatrix3d sig, Vector3d gj, double dv) {
      FemUtilities.addMaterialStiffness (myK, gi, D, sig, gj, dv);
   }
   
   public void addMaterialStiffness(Vector3d gi, Matrix6d D, Vector3d gj, double dv) {
      FemUtilities.addMaterialStiffness (myK, gi, D, gj, dv);
   }

   /**
    * Geometric strain-based stiffess
    */
   public void addGeometricStiffness (
      Vector3d gi, SymmetricMatrix3d sig, Vector3d gj, double dv) {
      FemUtilities.addGeometricStiffness (myK, gi, sig, gj, dv);   
   }
   
   public void addPressureStiffness( Vector3d gi, double p,
      Vector3d gj, double dv) {
      
      if (FemModel3d.noIncompressStiffnessDamping) {
         if (myKX == null) {
            myKX = new Matrix3d();
         }
         FemUtilities.addPressureStiffness (myKX, gi, p, gj, dv);  
         FemUtilities.addPressureStiffness (myK, gi, -p, gj, dv);  
      }
      
   }
   
   public void addShellMaterialStiffness(
      double iN, double jN, Vector3d idN, Vector3d jdN, double dv, double t,
      Vector3d[] gct, SymmetricMatrix3d matStress, Matrix6d matTangent, 
      Vector3d gi, Matrix3d invJ) {
      FemUtilities.addShellMaterialStiffness(
         this, iN, jN, idN, jdN, dv, t, invJ, matStress, matTangent);
   }  
   
}
