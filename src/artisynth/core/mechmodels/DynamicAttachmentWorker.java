package artisynth.core.mechmodels;

import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.MatrixBlock;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import artisynth.core.modelbase.*;

/**
 * Worker class that applies DynamicAttachments to reduce a dynamic system.
 */
public class DynamicAttachmentWorker {

   private MatrixBlock[] myMasterBlks = null;

   public int getSlaveSolveIndex (DynamicAttachment at) {
      DynamicComponent slave = at.getSlave();
      return slave != null ? slave.getSolveIndex() : -1;
   }

   /** 
    * Reduces a sparse column matrix (such as the transpose of a constraint
    * matrix) to account for an attachment. This is done by applying
    * the transform
    * <pre>
    * GT = P GT
    * </pre>
    * where
    * <pre>
    *             T
    *     [ I -Gka  ]
    * P = [         ]
    *     [ 0   0   ]
    * </pre>
    * and Gka is the constraint matrix for this attachment.
    * 
    * @param at attachment engendering the reduction
    * @param GT matrix to be reduced
    * @param dg derivative information to be reduced
    * @param zeroReducedBlocks if {@code false}, does not zero the blocks
    * in {@code GT} associated with the attachment. Although this is
    * not the correct behavior, it can be used in situations where it does not
    * interfere with the final solution and the block contents are used
    * by the attachment for other computations.
    */
   public void reduceConstraints (
      DynamicAttachment at, SparseBlockMatrix GT, VectorNd dg, 
      boolean zeroReducedBlocks) {
      DynamicComponent[] masters = at.getMasters();
      if (masters.length == 0 || GT.colSize() == 0) {
         return;
      }
      int bs = getSlaveSolveIndex(at);
      if (bs == -1) {
         return;
      }

      double[] dbuf = null;
      double[] gbuf = null;
      int ssize = 0;
      if (dg != null) {
         gbuf = dg.getBuffer();
         ssize = GT.getBlockRowSize(bs);
         dbuf = getNegatedDerivative (at, new VectorNd (ssize));
      }
      
      MatrixBlock srowBlk = GT.firstBlockInRow (bs);
      while (srowBlk != null) {
         int bj = srowBlk.getBlockCol();
         for (int i = 0; i < masters.length; i++) {
            if (masters[i].getSolveIndex() != -1) {
               int bm = masters[i].getSolveIndex();
               MatrixBlock depBlk = GT.getBlock (bm, bj);
               if (depBlk == null) {
                  depBlk = MatrixBlockBase.alloc (
                     GT.getBlockRowSize(bm), srowBlk.colSize());
                  //depBlk = createRowBlock (srowBlk.colSize());
                  GT.addBlock (bm, bj, depBlk);
               }
               at.mulSubGTM (depBlk, srowBlk, i);
               if (gbuf != null && dbuf != null) {
                  int goff = GT.getBlockColOffset (bj);
                  srowBlk.mulTransposeAdd (gbuf, goff, dbuf, 0);
               }
            }
         }
         if (zeroReducedBlocks) {
            srowBlk.setZero();
         }
         srowBlk = srowBlk.next();
      }
   }

   private double[] getNegatedDerivative (
      DynamicAttachment at, VectorNd dvec) {
      double[] dbuf = dvec.getBuffer();
      if (at.getDerivative (dbuf, 0)) {
         dvec.negate();
         return dbuf;
      }
      else {
         return null;
      }
   }

   /** 
    * Reduces a sparse row matrix (such as a constraint
    * matrix) to account for this attachment. This is done by applying
    * the transform
    * <pre>
    *        T
    * G = G P
    * </pre>
    * where
    * <pre>
    *             T
    *     [ I -Gka  ]
    * P = [         ]
    *     [ 0   0   ]
    * </pre>
    * and Gka is the constraint matrix for this attachment.
    *
    * <p>At present, this method requires the matrix G to be vertically linked.
    */
   public void reduceRowMatrix (
      DynamicAttachment at, SparseBlockMatrix G) {
      if (!G.isVerticallyLinked()) {
         throw new IllegalArgumentException (
            "Matrix G is not vertically linked");
      }
      DynamicComponent[] masters = at.getMasters();
      if (masters.length == 0) {
         return;
      }      
      int bs = getSlaveSolveIndex(at);
      if (bs == -1) {
         return;
      }
      MatrixBlock scolBlk = G.firstBlockInCol (bs);
      while (scolBlk != null) {
         int bi = scolBlk.getBlockRow();
         for (int j = 0; j < masters.length; j++) {
            if (masters[j].getSolveIndex() != -1) {
               int bm = masters[j].getSolveIndex();
               MatrixBlock depBlk = G.getBlock (bi, bm);
               if (depBlk == null) {
                  depBlk = MatrixBlockBase.alloc (
                     scolBlk.rowSize(), G.getBlockColSize (bm)); 
                  //depBlk = createColBlock (scolBlk.rowSize());
                  G.addBlock (bi, bm, depBlk);
               }
               at.mulSubMG (depBlk, scolBlk, j);               
            }
         }
         scolBlk.setZero();
         scolBlk = scolBlk.down();
      }
   }

   public void addSolveBlocks (
      DynamicAttachment at, SparseNumberedBlockMatrix S,
      boolean[] reduced) {

      at.setSlaveAffectsStiffness (false);
      DynamicComponent[] masters = at.getMasters();
      if (masters.length == 0) {
         return;
      }
      //System.out.println ("add solve blocks");

      if (!S.isVerticallyLinked()) {
         throw new IllegalArgumentException (
            "Matrix G is not vertically linked");
      }
      int bs = getSlaveSolveIndex(at);
      if (bs == -1) {
         return;
      }

      MatrixBlock srowBlk = S.firstBlockInRow (bs);      
      MatrixBlock scolBlk = S.firstBlockInCol (bs);
      // optimization: this attachment does not affect stiffness if the slave
      // itself has no force and it is not connected to any other force
      // effectors.
      if (srowBlk.next() == null && scolBlk.down() == null &&
          !at.getSlave().hasForce()) {
         return;
      }
      at.setSlaveAffectsStiffness (true);

      // rows first
      while (srowBlk != null) {
         int bj = srowBlk.getBlockCol();
         for (int i = 0; i < masters.length; i++) {
            if (masters[i].getSolveIndex() != -1) {
               int bm = masters[i].getSolveIndex();
               if (S.getBlock (bm, bj) == null) {
                  MatrixBlock newBlk = MatrixBlockBase.alloc (
                     S.getBlockRowSize(bm), srowBlk.colSize());
                  S.addBlock (bm, bj, newBlk);
               }
            }
         }
         srowBlk = srowBlk.next();
      }
      reduced[bs] = true;
      // columns next
      // scolBlk needs to be recomputed in case row operations changed it 
      scolBlk = S.firstBlockInCol (bs);
      while (scolBlk != null) {
         int bi = scolBlk.getBlockRow();
         if (!reduced[bi]) {
            for (int i = 0; i < masters.length; i++) {
               if (masters[i].getSolveIndex() != -1) {
                  int bm = masters[i].getSolveIndex();
                  if (S.getBlock (bi, bm) == null) {
                     MatrixBlock newBlk = MatrixBlockBase.alloc (
                        scolBlk.rowSize(), S.getBlockColSize(bm));
                     S.addBlock (bi, bm, newBlk);
                  }
               }
            }
         }
         scolBlk = scolBlk.down();
      }
   }

  /** 
    * Reduces the system matrix to account for this attachment. This
    * is done by applying the transform
    * <pre>
    *          T
    * M = P M P
    * </pre>
    * where
    * <pre>
    *             T
    *     [ I -Gka  ]
    * P = [         ]
    *     [ 0   0   ]
    * </pre>
    * and Gka is the constraint matrix for this attachment.
    */
   public void addAttachmentJacobian (
      DynamicAttachment at, 
      SparseBlockMatrix S, VectorNd f, boolean[] reduced) {
      
      if (!at.slaveAffectsStiffness()) {
         return;
      }
      DynamicComponent[] masters = at.getMasters();
      if (myMasterBlks == null || masters.length > myMasterBlks.length) {
         //System.out.println ("new num master blocks: " + masters.length);
         myMasterBlks = new MatrixBlock[masters.length];
      }

      int bm;
      int bs = getSlaveSolveIndex(at);
      //System.out.println ("bs=" + bs);
      if (bs == -1) {
         return;
      }

      // boolean debug =
      // (this instanceof PointParticleAttachment && getSlave().getNumber() == 0);

      double[] dbuf = null;
      double[] fbuf = null;
      int soff = -1;
      int ssize = 0;
      if (f != null) {
         fbuf = f.getBuffer();
         soff = S.getBlockRowOffset(bs);
         ssize = S.getBlockRowSize(bs);
         dbuf = getNegatedDerivative (at, new VectorNd (ssize));
      }
      
      //
      // rows first: M = P M
      //
      MatrixBlock srowBlk = S.firstBlockInRow (bs);
      // get first row block for each master
      for (int i = 0; i < masters.length; i++) {
         if ((bm = masters[i].getSolveIndex()) != -1) {
            myMasterBlks[i] = S.firstBlockInRow (bm);
         }
      }
      while (srowBlk != null) {
         int bj = srowBlk.getBlockCol();
         for (int i = 0; i < masters.length; i++) {
            if ((bm = masters[i].getSolveIndex()) != -1) {
               MatrixBlock depBlk = myMasterBlks[i];
               while (depBlk.getBlockCol() < bj) {
                  depBlk = depBlk.next();
               }
               if (depBlk.getBlockCol() != bj) {
                  throw new InternalErrorException (
                     "slave blk at ("+bs+","+bj+"), master at ("+
                     depBlk.getBlockRow()+","+depBlk.getBlockCol()+")");
               }
               at.mulSubGTM (depBlk, srowBlk, i);
               myMasterBlks[i] = depBlk;
            }
         }
         srowBlk.setZero();
         srowBlk = srowBlk.next();
      }
      reduced[bs] = true;
      //                      T
      // columns next: M = M P, and f += M dg
      //
      MatrixBlock scolBlk = S.firstBlockInCol (bs);
      for (int i = 0; i < masters.length; i++) {
         if ((bm = masters[i].getSolveIndex()) != -1) {
            myMasterBlks[i] = S.firstBlockInCol (bm);
         }
      }
      while (scolBlk != null) {
         int bi = scolBlk.getBlockRow();
         if (!reduced[bi]) {
            for (int i = 0; i < masters.length; i++) {
               if ((bm = masters[i].getSolveIndex()) != -1) {
                  MatrixBlock depBlk = myMasterBlks[i];
                  while (depBlk.getBlockRow() < bi) {
                     depBlk = depBlk.down();
                  }
                  if (depBlk.getBlockRow() != bi) {
                     System.out.println ("S=\n" + S.getBlockPattern());
                     System.out.println ("slave is " + bs);
                     throw new InternalErrorException (
                        "slave blk at ("+bi+","+bs+"), master at ("+
                        depBlk.getBlockRow()+","+depBlk.getBlockCol()+")");
                  }
                  at.mulSubMG (depBlk, scolBlk, i);
                  myMasterBlks[i] = depBlk;
               }
            }
            if (fbuf != null && dbuf != null) {
               int foff = S.getBlockRowOffset (bi);
               scolBlk.mulAdd (fbuf, foff, dbuf, 0);
            }
            scolBlk.setZero();
         }
         scolBlk = scolBlk.down();
      }

      if (fbuf != null) {
         // f = P f
         for (int i = 0; i < masters.length; i++) {
            if ((bm = masters[i].getSolveIndex()) != -1) {
               int moff = S.getBlockRowOffset (bm);
               at.mulSubGT (fbuf, moff, fbuf, soff, i);
            }
         }
         // zero out the slave term
         for (int i=0; i<ssize; i++) {
            fbuf[soff+i] = 0;
         }
      }
   }

   /**
    * Orders a list of (possibly interdependent) attachments so that
    * state can be updated correctly by starting at the beginning of the list
    * and running through to the end. Unattached components will be at
    * the beginning of the list, and the masters of any given attachment do not
    * depend on any attachments further along the list. This
    * implicitly requires that the attachment configuration does not
    * contain loops.
    */
   public ArrayList<DynamicAttachment> createOrderedList (
      List<DynamicAttachment> list) {
      
      ArrayList<DynamicAttachment> result = new ArrayList<DynamicAttachment>();
      LinkedList<DynamicAttachment> queue = new LinkedList<DynamicAttachment>();
      HashMap<DynamicAttachment,Integer> attachedMasterCnts =
         new HashMap<DynamicAttachment,Integer>();
      for (DynamicAttachment a : list) {
         int attachedMasterCnt = 0;
         for (DynamicComponent m : a.getMasters()) {
            if (m.isAttached()) {
               attachedMasterCnt++;
            }
         }
         if (attachedMasterCnt == 0) {
            queue.offer (a);
         }
         attachedMasterCnts.put (a, attachedMasterCnt);
      }
      while (!queue.isEmpty()) {
         DynamicAttachment a = queue.poll();
         if (a.getSlave() != null) {
            LinkedList<DynamicAttachment> masterAttachments =
               a.getSlave().getMasterAttachments();
            if (masterAttachments != null) {
               for (DynamicAttachment b : masterAttachments) {
                  int attachedMasterCnt = attachedMasterCnts.get (b);
                  attachedMasterCnt--;
                  if (attachedMasterCnt == 0) {
                     queue.offer (b);
                  }
                  attachedMasterCnts.put (b, attachedMasterCnt);
               }
            }
         }
         result.add (a);
      }
      return result;      
   }

   public static boolean containsLoops (List<DynamicAttachment> list) {
      HashMap<DynamicComponent,DynamicAttachment> map =
         new HashMap<DynamicComponent,DynamicAttachment>();
      for (DynamicAttachment a : list) {
         DynamicComponent slave = a.getSlave();
         if (slave != null) {
            map.put (a.getSlave(), a);
         }
      }
      for (DynamicAttachment a : list) {
         DynamicComponent slave = a.getSlave();
         if (slave != null) {
            if (containsLoop (a, slave, map)) {
               return true;
            }
         }
      }
      return false;
   }

   public static boolean containsLoop (
      DynamicAttachment a, DynamicComponent slave,
      HashMap<DynamicComponent,DynamicAttachment> map) {
      
      DynamicAttachment b;
      for (DynamicComponent m : a.getMasters()) {
         if (m == slave) {
            return true;
         }
         else if ((b = m.getAttachment()) != null ||
                  (map != null && (b = map.get (m)) != null)) {
            if (containsLoop (b, slave, map)) {
               return true;
            }
         }
      }
      return false;
   }   

}
