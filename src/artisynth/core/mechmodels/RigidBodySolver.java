/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;

import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import maspack.matrix.*;
import maspack.util.*;
import maspack.solvers.KKTSolver;
import maspack.solvers.SparseSolverId;
import maspack.spatialmotion.*;

/** 
 * Solves a sub-set of a MechSystem defined by rigid bodies
 */
public class RigidBodySolver {

   private static final int G_MATRIX = 1;
   private static final int N_MATRIX = 2;
   private static final int D_MATRIX = 3;
   
   private int myStructureVersion = -1;
   private int myBilateralVersion = -1;

   private int myNumActiveBodies = -1;
   // maps component indices to body indices
   private int[] myCompBodyMap;
   // maps body indices to component indices
   private int[] myBodyCompMap;
   // block sizes for rigid body mass matrix
   private int[] myMassSizes;

   // indices of rigid body velocity subvector
   private int[] myVelIdxs;
   // indices of rigid body bilateral constraint subvector
   private int[] myLamIdxs;
   // indices of rigid body unilateral constraint subvector
   private int[] myTheIdxs;
   // indices of rigid body friction constraint subvector
   private int[] myPhiIdxs;

   int mySizeM = 0;
   SparseBlockMatrix myMass;
   int mySizeG = 0;
   SparseBlockMatrix myGT;
   int mySizeN = 0;
   SparseBlockMatrix myNT;
   int mySizeD = 0;
   SparseBlockMatrix myDT;

   VectorNd myRg = new VectorNd();
   VectorNd myRn = new VectorNd();

   VectorNd myBf = new VectorNd();
   VectorNd myBg = new VectorNd();
   VectorNd myBn = new VectorNd();
   VectorNd myBd = new VectorNd();
   VectorNd myFlim = new VectorNd();

   VectorNd myVel = new VectorNd();
   VectorNd myLam = new VectorNd();
   VectorNd myThe = new VectorNd();
   VectorNd myPhi = new VectorNd();

   KKTSolver mySolver;

   // map from local GT indices to global GT indices
   private int[] myGTMap;

   // map from local NT indices to global NT indices
   private int[] myNTMap;

   // map from local DT indices to global DT indices
   private int[] myDTMap;

   private MechSystem mySys;
   private SparseSolverId myMatrixSolver = SparseSolverId.Pardiso;

   public RigidBodySolver (MechSystem sys) {
      if (sys instanceof MechSystemBase) {
         myMatrixSolver = ((MechSystemBase)sys).getMatrixSolver();
      }
      else {
         myMatrixSolver = MechSystemBase.getDefaultMatrixSolver();
      }
      mySys = sys;
   }

   /**
    * XXX this is currently a hack - a rigid body is assumed to correspond to
    * any component whose velocity state size is >= 6. Instead, we should get this
    * information from MechSystem in some way.
    */
   private boolean componentIsRigidBody (int idx, SparseBlockMatrix M) {
      return M.getBlockRowSize(idx) >= 6;
   }

   private void doUpdateStructure (SparseBlockMatrix M, SparseBlockMatrix GT) {
      int nactive = mySys.numActiveComponents();
      myNumActiveBodies = 0;
      // ci means component index
      for (int ci=0; ci<nactive; ci++) {
         if (componentIsRigidBody (ci, M)) {
            myNumActiveBodies++;
         }
      }

      if (myNumActiveBodies > 0) {
         myBodyCompMap = new int[myNumActiveBodies];
         myCompBodyMap = new int[nactive];

         // bi means body index
         int bi = 0;
         for (int ci=0; ci<nactive; ci++) {
            if (componentIsRigidBody (ci, M)) {
               myCompBodyMap[ci] = bi;
               myBodyCompMap[bi] = ci;
               bi++;
            }
            else {
               myCompBodyMap[ci] = -1;
            }
         }
         
         // create local mass matrix
         
         myMassSizes = new int[myNumActiveBodies];
         mySizeM = 0;
         for (bi=0; bi<myNumActiveBodies; bi++) {
            int size = M.getBlockRowSize(myBodyCompMap[bi]);
            myMassSizes[bi] = size;
            mySizeM += size;
         }

         myVelIdxs = new int[mySizeM];
         myMass = new SparseBlockMatrix (myMassSizes);
         int boff = 0;
         for (bi=0; bi<myNumActiveBodies; bi++) {
            int ci = myBodyCompMap[bi];
            myMass.addBlock (bi, bi, M.getBlock(ci,ci).clone());
            int vidx = M.getBlockRowOffset (ci);
            int size = myMassSizes[bi];
            for (int k=0; k<size; k++) {
               myVelIdxs[boff+k] = vidx++;
            }
            boff += size;
         }
         myVel = new VectorNd (mySizeM);
         myBf = new VectorNd (mySizeM);

         // create local GT matrix

         myGT = new SparseBlockMatrix (myMassSizes, new int[0]);
         myGTMap = createConstraintMatrix (myGT, GT, G_MATRIX);
         mySizeG = myGT.colSize();
         myLamIdxs = createConstraintIdxs (GT, myGTMap, mySizeG);
         myLam.setSize (mySizeG);
         myBg.setSize (mySizeG);
         myRg.setSize (mySizeG); // leave at zero for now
         
         mySolver.analyze (myMass, mySizeM, myGT, myRg, Matrix.SPD);
      }
   }

   private void updateGT (SparseBlockMatrix GT) {
      for (int bk=0; bk<myGTMap.length; bk++) {
         int bj = myGTMap[bk];
         MatrixBlock blkSrc = GT.firstBlockInCol(bj);
         MatrixBlock blkDst = myGT.firstBlockInCol(bk);
         while (blkDst != null) {
            blkDst.set (blkSrc);
            blkDst = blkDst.down();
            blkSrc = blkSrc.down();
         }
      }
   }

   private void updateMass (SparseBlockMatrix M) {
      for (int bi=0; bi<myNumActiveBodies; bi++) {
         int ci = myBodyCompMap[bi];
         MatrixBlock blk = myMass.getBlock(bi,bi);
         blk.set (M.getBlock(ci,ci));
      }
   }

   private int[] createConstraintMatrix (
      SparseBlockMatrix XT, SparseBlockMatrix XTGlobal, int type) {
      
      // alloc max possible size for column map
      int[] colMap = new int[XTGlobal.numBlockCols()];
      int bk = 0;
      int bi;
      XTGlobal.setVerticallyLinked (true);
      int nactive = mySys.numActiveComponents();
      for (int bj=0; bj<XTGlobal.numBlockCols(); bj++) {
         MatrixBlock blkA = null;
         MatrixBlock blkB = null;
         MatrixBlock blk = XTGlobal.firstBlockInCol(bj);
         int numBodies = 0;
         while (blk != null) {
            bi = blk.getBlockRow();
            // don't add the constraint if we find a non-body constraint,
            // or the constraint involves more than two bodies, or if this
            // is a D matrix and the block column size is one and hence
            // indicates a unidirectional constraint
            if (!componentIsRigidBody (bi, XTGlobal) || 
                ++numBodies > 2 || 
                (type == D_MATRIX && blk.colSize() == 1)) {
               blkA = null;
               break;
            }
            if (bi < nactive) {
               if (blkA == null) {
                  blkA = blk;
               }
               else {
                  blkB = blk;
               }
            }
            blk = blk.down();
         }
         if (blkA != null) {
            bi = blkA.getBlockRow();
            XT.addBlock (myCompBodyMap[bi], bk, blkA.clone());
            if (blkB != null) {
               bi = blkB.getBlockRow();
               XT.addBlock (myCompBodyMap[bi], bk, blkB.clone()); 
            }
            colMap[bk] = bj;
            bk++;
         }
      }
      XT.setVerticallyLinked (true); // do we always need this?
      return Arrays.copyOf (colMap, XT.numBlockCols());
   }

   private int[] createConstraintIdxs (
      SparseBlockMatrix XTGlobal, int[] colMap, int sizeX) {

      int[] idxs = new int[sizeX];
      int k = 0;
      for (int bk=0; bk<colMap.length; bk++) {
         int bj = colMap[bk];
         int vidx = XTGlobal.getBlockColOffset(bj);
         for (int i=0; i<XTGlobal.getBlockColSize(bj); i++) {
            idxs[k++] = vidx++;
         }
      }   
      return idxs;
   }

   private void updateNT (SparseBlockMatrix NT) {
      myNT = new SparseBlockMatrix (myMassSizes, new int[0]);
      myNTMap = createConstraintMatrix (myNT, NT, N_MATRIX);
      mySizeN = myNT.colSize();
      myTheIdxs = createConstraintIdxs (NT, myNTMap, mySizeN);
      myThe.setSize (mySizeN);
      myBn.setSize (mySizeN);
      myRn.setSize (mySizeN); // leave at zero for now
   }

   public int[] getDTMap() {
      return myDTMap;
   }

   private void updateDT (SparseBlockMatrix DT) {
      myDT = new SparseBlockMatrix (myMassSizes, new int[0]);
      myDTMap = createConstraintMatrix (myDT, DT, D_MATRIX);
      mySizeD = myDT.colSize();
      myPhiIdxs = createConstraintIdxs (DT, myDTMap, mySizeD);
      myPhi.setSize (mySizeD);
      myBd.setSize (mySizeD);
      myFlim.setSize (mySizeD);
   }

   public boolean updateStructure (
      SparseBlockMatrix M, SparseBlockMatrix GT, int GTversion) {
      if (mySolver == null) {
         mySolver = new KKTSolver(myMatrixSolver);
      }
      if (myStructureVersion != mySys.getStructureVersion() ||
          myBilateralVersion != GTversion || 
          GTversion == -1) {
         doUpdateStructure(M, GT);
         myStructureVersion = mySys.getStructureVersion();
         myBilateralVersion = GTversion;
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Called to ensure that structure will be updated the next time
    * updateStructure is called.
    */
   public void resetBilateralVersion() {
      myBilateralVersion = -1;
   }

   public boolean projectVelocity (
      SparseBlockMatrix M, SparseBlockMatrix GT, SparseBlockMatrix NT,
      VectorNd bg, VectorNd bn,
      VectorNd vel, VectorNd lam, VectorNd the) {

      if (mySizeM == 0) {
         return false;
      }

      // create bf from vel
      vel.getSubVector (myVelIdxs, myVel);
      myMass.mul (myBf, myVel);

      updateMass (M);
      updateGT (GT);
      updateNT (NT);

      if (mySizeG == 0 && mySizeN == 0) {
         return false;
      }

      bg.getSubVector (myLamIdxs, myBg);
      bn.getSubVector (myTheIdxs, myBn);

      mySolver.factor (myMass, mySizeM, myGT, myRg, myNT, myRn);
      mySolver.solve (myVel, myLam, myThe, myBf, myBg, myBn);

      vel.setSubVector (myVelIdxs, myVel);
      lam.setSubVector (myLamIdxs, myLam);
      the.setSubVector (myTheIdxs, myThe);
      // add lam to the total force result?
      return true;
   }

   public boolean projectPosition (
      SparseBlockMatrix M, SparseBlockMatrix GT, SparseBlockMatrix NT,
      VectorNd bg, VectorNd bn,
      VectorNd vel, VectorNd lam, VectorNd the) {

      if (mySizeM == 0) {
         return false;
      }
      myBf.setZero();

      updateMass (M);
      updateGT (GT);
      updateNT (NT);

      if (mySizeG == 0 && mySizeN == 0) {
         return false;
      }

      bg.getSubVector (myLamIdxs, myBg);
      bn.getSubVector (myTheIdxs, myBn);

      mySolver.factor (myMass, mySizeM, myGT, myRg, myNT, myRn);
      mySolver.solve (myVel, myLam, myThe, myBf, myBg, myBn);

      int boff = 0;
      for (int i=0; i<myNumActiveBodies; i++) {
         int size = myMassSizes[i];
         VectorNd bvel = new VectorNd(size);
         myVel.getSubVector (boff, bvel);
         boff += size;
      }

      vel.setSubVector (myVelIdxs, myVel);
      lam.setSubVector (myLamIdxs, myLam);
      the.setSubVector (myTheIdxs, myThe);
      // add lam to the total force result?
      return true;
   }

   public boolean projectFriction (
      SparseBlockMatrix M,
      SparseBlockMatrix GT, SparseBlockMatrix NT, SparseBlockMatrix DT,
      VectorNd Rg, VectorNd bg, VectorNd Rn, VectorNd bn, VectorNd bd,
      FrictionInfo[] finfo, VectorNd vel,
      VectorNd lam, VectorNd the, VectorNd phi) {
      
      if (mySizeM == 0) {
         myDTMap = new int[0];
         return false;
      }

      updateMass (M);
      updateGT (GT);
      updateNT (NT);
      updateDT (DT);

      if (mySizeD == 0) {
         return false;
      }

      lam.getSubVector (myLamIdxs, myLam);
      Rg.getSubVector (myLamIdxs, myRg);
      bg.getSubVector (myLamIdxs, myBg);
      the.getSubVector (myTheIdxs, myThe);
      Rn.getSubVector (myTheIdxs, myRn);
      bn.getSubVector (myTheIdxs, myBn);
      bd.getSubVector (myPhiIdxs, myBd);

      // remove initial constraints from righthand side. This means
      // we will 'solve' for theta again, but greatly reduces chatter.
      myGT.mul (myBf, myLam);
      myNT.mulAdd (myBf, myThe);
      myBf.negate();
      // create bf from vel
      vel.getSubVector (myVelIdxs, myVel);
      myMass.mulAdd (myBf, myVel);

      // XXX also need to set bd from info.offset - or compute this elsewhere
      // set the friction limits
      int k = 0;
      for (int bk=0; bk<myDT.numBlockCols(); bk++) {
         FrictionInfo info = finfo[myDTMap[bk]];
         double phiMax;
         if ((info.flags & FrictionInfo.BILATERAL) != 0) {
            phiMax = info.getMaxFriction (lam);
         }
         else {
            phiMax = info.getMaxFriction (the);
         }         
         //System.out.println ("fm"+bk+" "+phiMax);
         for (int i=0; i<myDT.getBlockColSize(bk); i++) {
            myFlim.set (k++, phiMax);
         }
      }

      
      mySolver.factor (myMass, mySizeM, myGT, myRg, myNT, myRn, myDT);
      mySolver.solve (myVel, myLam, myThe, myPhi, myBf, myBg, myBn, myBd, myFlim);

      // ArraySupport.print ("zstate", mySolver.getZState());
      // System.out.println ("the: " + myThe.toString ("%8.3f"));
      // System.out.println ("phi: " + myPhi.toString ("%8.3f"));

      vel.setSubVector (myVelIdxs, myVel);
      phi.getSubVector (myPhiIdxs, myPhi);
      // add lam to the total force result?
      return true;
   }

   public boolean[] getZBasic() {
      return mySolver.getZBasic();
   }

   public int[] getZState() {
      return mySolver.getZState();
   }

   public void dispose() {
      if (mySolver != null) {
         mySolver.dispose();
         mySolver = null;
      }
   }

   public void finalize() {
      dispose();
   }
   
}
