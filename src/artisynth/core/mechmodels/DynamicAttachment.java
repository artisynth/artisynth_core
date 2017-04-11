/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.MatrixBlock;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import artisynth.core.modelbase.*;

/**
 * Component that implements attachment between dynamic components.
 * A component 'a' can be attached to one or more "master" components
 * 'b' if it's position q_a can be made an
 * explicit differentiable function of the positions q_m of the masters:
 * <pre>
 * q_a = f (q_m)
 * </pre>
 * Differentiating means that the attached component velocity u_a
 * is a linear function of the master velocities u_m:
 * <pre>
 * u_a = -G (u_m)
 * </pre>
 * where G = -(d f)/(d q_m) is the "constraint matrix".
 */
public abstract class DynamicAttachment extends ModelComponentBase {

   private int attachedMasterCnt; // used internally for ordering the attachments
   private MatrixBlock[] myMasterBlks = null;

   public abstract DynamicComponent[] getMasters();

   public int numMasters() {
      return getMasters().length;
   }
   
   public abstract void invalidateMasters();

   public boolean containsMaster (DynamicComponent comp) {
      DynamicComponent[] masters = getMasters();
      for (int i = 0; i < masters.length; i++) {
         if (masters[i] == comp) {
            return true;
         }
      }
      return false;
   }

   public boolean oneMasterActive() {
      DynamicComponent[] masters = getMasters();
      for (int i = 0; i < masters.length; i++) {
         if (masters[i].isActive()) {
            return true;
         }
      }
      return false;
   }
   
   /**
    * Every master component should contain a back reference to each
    * attachment that references it. This method adds the back reference
    * for this attachment to each of the masters.
    */
   public void addBackRefs() {
      DynamicComponent[] masters = getMasters();
      if (masters != null) {
         for (int i = 0; i < masters.length; i++) {
            masters[i].addMasterAttachment (this);
         }     
      }
   }
   
   /**
    * Removes the back reference to this attachment's slave component
    * from each of the master component.
    */
   public void removeBackRefs() {
      DynamicComponent[] masters = getMasters();
      if (masters != null) {
         for (int i = 0; i < masters.length; i++) {
            masters[i].removeMasterAttachment (this);
         }      
      }
   }
 
   /**
    * Calls {@link #addBackRefs} if this attachment is currently
    * connected to the hierarchy.
    */
   protected void addBackRefsIfConnected() {
      if (isConnectedToHierarchy()) {
         addBackRefs();
      }
   }

   /**
    * Calls {@link #removeBackRefs} if this attachment is currently
    * connected to the hierarchy.
    */
   protected void removeBackRefsIfConnected() {
      if (isConnectedToHierarchy()) {
         removeBackRefs();
      }
   }

  /**
    * Returns the slave DynamicMechComponent associated with this attachment.
    * In some cases, the attachment may connect some other entity (such
    * as a mesh vertex) to the master components, in which case this method
    * should return <code>null</code>.  
    * 
    * @return slave DynamicMechComponent, if any
    */
   public abstract DynamicComponent getSlave();
   
   /**
    * Returns the block index within the system solve matrix of the 
    * slave DynamicMechComponent associated with this attachment.
    * If there is no such component (see {@link #getSlave()}),
    * this method returns -1.
    * 
    * @return solve index of slave DynamicMechComponent, or -1.
    */
   public abstract int getSlaveSolveIndex();

   public abstract void updatePosStates();

   public abstract void updateVelStates();

   /**
    * Update attachment to reflect changes in the slave state.
    */
   public abstract void updateAttachment();

   public abstract void applyForces();

   /** 
    * Reduces a sparse column matrix (such as the transpose of a constraint
    * matrix) to account for this attachment. This is done by applying
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
    */
   public void reduceConstraints (SparseBlockMatrix GT, VectorNd dg) {
      DynamicComponent[] masters = getMasters();
      if (masters.length == 0) {
         return;
      }
      int bs = getSlaveSolveIndex();
      if (bs == -1) {
         return;
      }

      double[] dbuf = null;
      double[] gbuf = null;
      int ssize = 0;
      if (dg != null) {
         gbuf = dg.getBuffer();
         ssize = GT.getBlockRowSize(bs);
         dbuf = getNegatedDerivative (new VectorNd (ssize));
      }
      
      MatrixBlock blk = GT.firstBlockInRow (bs);
      while (blk != null) {
         int bj = blk.getBlockCol();
         for (int i = 0; i < masters.length; i++) {
            if (masters[i].getSolveIndex() != -1) {
               int bm = masters[i].getSolveIndex();
               MatrixBlock depBlk = GT.getBlock (bm, bj);
               if (depBlk == null) {
                  depBlk = MatrixBlockBase.alloc (
                     GT.getBlockRowSize(bm), blk.colSize());
                  //depBlk = createRowBlock (blk.colSize());
                  GT.addBlock (bm, bj, depBlk);
               }
               mulSubGT (depBlk, blk, i);               
               if (gbuf != null && dbuf != null) {
                  int goff = GT.getBlockColOffset (bj);
                  blk.mulTransposeAdd (gbuf, goff, dbuf, 0);
               }
            }
         }
         blk = blk.next();
      }
   }

   private double[] getNegatedDerivative (VectorNd dvec) {
      double[] dbuf = dvec.getBuffer();
      if (getDerivative (dbuf, 0)) {
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
   public void reduceRowMatrix (SparseBlockMatrix G) {
      if (!G.isVerticallyLinked()) {
         throw new IllegalArgumentException (
            "Matrix G is not vertically linked");
      }
      DynamicComponent[] masters = getMasters();
      if (masters.length == 0) {
         return;
      }      
      int bs = getSlaveSolveIndex();
      if (bs == -1) {
         return;
      }
      MatrixBlock blk = G.firstBlockInCol (bs);
      while (blk != null) {
         int bi = blk.getBlockRow();
         for (int j = 0; j < masters.length; j++) {
            if (masters[j].getSolveIndex() != -1) {
               int bm = masters[j].getSolveIndex();
               MatrixBlock depBlk = G.getBlock (bi, bm);
               if (depBlk == null) {
                  depBlk = MatrixBlockBase.alloc (
                     blk.rowSize(), G.getBlockColSize (bm)); 
                  //depBlk = createColBlock (blk.rowSize());
                  G.addBlock (bi, bm, depBlk);
               }
               mulSubG (depBlk, blk, j);               
            }
         }
         blk = blk.down();
      }
   }

   public abstract void addMassToMasters();
   
   public void reduceMass (SparseBlockMatrix M, VectorNd f) {
      DynamicComponent[] masters = getMasters();
      if (masters.length == 0) {
         return;
      }      
      int bs = getSlaveSolveIndex();
      if (bs == -1) {
         return;
      }
      double[] dbuf = null;
      double[] fbuf = null;
      int soff = -1;
      if (f != null) {
         fbuf = f.getBuffer();
         soff = M.getBlockRowOffset(bs);
         dbuf = getNegatedDerivative (new VectorNd (M.getBlockRowSize(bs)));
      }

      MatrixBlock sblk = M.getBlock (bs, bs);
      if (fbuf != null && dbuf != null) {
         // add coriolis term to existing fictitious force term for the slave
         sblk.mulAdd (fbuf, soff, dbuf, 0);
      }
      //System.out.println ("f1=" + f.toString ("%7.4f"));
      for (int i = 0; i < masters.length; i++) {
         int bm = masters[i].getSolveIndex();
         if (bm != -1) {
            MatrixBlock mblk = M.getBlock (bm, bm);
            addMassToMaster (mblk, sblk, i);
            if (fbuf != null) {
               int moff = M.getBlockRowOffset (bm);
               mulSubGT (fbuf, moff, fbuf, soff, i);
            }
         }
      }
      //System.out.println ("f2=" + f.toString ("%7.4f"));
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix S, boolean[] reduced) {
      DynamicComponent[] masters = getMasters();
      if (masters.length == 0) {
         return;
      }
      //System.out.println ("add solve blocks");

      if (!S.isVerticallyLinked()) {
         throw new IllegalArgumentException (
            "Matrix G is not vertically linked");
      }
      int bs = getSlaveSolveIndex();
      if (bs == -1) {
         return;
      }
      // rows first
      MatrixBlock blk = S.firstBlockInRow (bs);
      while (blk != null) {
         int bj = blk.getBlockCol();
         for (int i = 0; i < masters.length; i++) {
            if (masters[i].getSolveIndex() != -1) {
               int bm = masters[i].getSolveIndex();
               if (S.getBlock (bm, bj) == null) {
                  MatrixBlock newBlk = MatrixBlockBase.alloc (
                     S.getBlockRowSize(bm), blk.colSize());
                  //S.addBlock (bm, bj, createRowBlock (blk.colSize()));
                  S.addBlock (bm, bj, newBlk);
                  // System.out.println ("adding " + bm + " " + bj);
               }
            }
         }
         blk = blk.next();
      }
      reduced[bs] = true;
      // columns next
      blk = S.firstBlockInCol (bs);
      while (blk != null) {
         int bi = blk.getBlockRow();
         if (!reduced[bi]) {
            for (int i = 0; i < masters.length; i++) {
               if (masters[i].getSolveIndex() != -1) {
                  int bm = masters[i].getSolveIndex();
                  if (S.getBlock (bi, bm) == null) {
                     MatrixBlock newBlk = MatrixBlockBase.alloc (
                        blk.rowSize(), S.getBlockColSize(bm));
                     //S.addBlock (bi, bm, createColBlock (blk.rowSize()));
                     S.addBlock (bi, bm, newBlk);
                     // System.out.println ("adding " + bi + " " + bm);
                  }
               }
            }
         }
         blk = blk.down();
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
      SparseBlockMatrix S, VectorNd f, boolean[] reduced) {

      DynamicComponent[] masters = getMasters();
      if (masters.length == 0) {
         return;
      }
      if (myMasterBlks == null || masters.length > myMasterBlks.length) {
         //System.out.println ("new num master blocks: " + masters.length);
         myMasterBlks = new MatrixBlock[masters.length];
      }

      int bm;
      int bs = getSlaveSolveIndex();
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
         dbuf = getNegatedDerivative (new VectorNd (ssize));
      }
      
      //
      // rows first: M = P M
      //
      MatrixBlock blk = S.firstBlockInRow (bs);
      // get first row block for each master
      for (int i = 0; i < masters.length; i++) {
         if ((bm = masters[i].getSolveIndex()) != -1) {
            myMasterBlks[i] = S.firstBlockInRow (bm);
         }
      }
      while (blk != null) {
         int bj = blk.getBlockCol();
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
               mulSubGT (depBlk, blk, i);
               myMasterBlks[i] = depBlk;
            }
         }
         blk.setZero();
         blk = blk.next();
      }
      reduced[bs] = true;
      //                      T
      // columns next: M = M P, and f += M dg
      //
      blk = S.firstBlockInCol (bs);
      for (int i = 0; i < masters.length; i++) {
         if ((bm = masters[i].getSolveIndex()) != -1) {
            myMasterBlks[i] = S.firstBlockInCol (bm);
         }
      }
      while (blk != null) {
         int bi = blk.getBlockRow();
         if (!reduced[bi]) {
            for (int i = 0; i < masters.length; i++) {
               if ((bm = masters[i].getSolveIndex()) != -1) {
                  MatrixBlock depBlk = myMasterBlks[i];
                  while (depBlk.getBlockRow() < bi) {
                     depBlk = depBlk.down();
                  }
                  if (depBlk.getBlockRow() != bi) {
                     throw new InternalErrorException (
                        "slave blk at ("+bi+","+bs+"), master at ("+
                        depBlk.getBlockRow()+","+depBlk.getBlockCol()+")");
                  }
                  mulSubG (depBlk, blk, i);
                  myMasterBlks[i] = depBlk;
               }
            }
            if (fbuf != null && dbuf != null) {
               int foff = S.getBlockRowOffset (bi);
               blk.mulAdd (fbuf, foff, dbuf, 0);
            }
            blk.setZero();
         }
         blk = blk.down();
      }

      if (fbuf != null) {
         // f = P f
         for (int i = 0; i < masters.length; i++) {
            if ((bm = masters[i].getSolveIndex()) != -1) {
               int moff = S.getBlockRowOffset (bm);
               mulSubGT (fbuf, moff, fbuf, soff, i);
            }
         }
         // zero out the slave term
         for (int i=0; i<ssize; i++) {
            fbuf[soff+i] = 0;
         }
      }
   }

   protected abstract void addMassToMaster (
      MatrixBlock mblk, MatrixBlock sblk, int i);

   public abstract boolean getDerivative (double[] buf, int idx);

   /** 
    * Computes
    * <pre>
    *       T
    * D -= G  B
    * </pre>
    * where D and B are matrices associated with master and slave components,
    * respectively, and G is the constraint matrix for the attachment.
    * @param D dependent matrix associated with a master component
    * @param B matrix associated with a slave component
    */
   protected abstract void mulSubGT (
      MatrixBlock D, MatrixBlock B, int idx);

   /** 
    * Computes
    * <pre>
    * D -= B G
    * </pre>
    * where D and B are matrices associated with master and slave components,
    * respectively, and G is the constraint matrix for the attachment.
    * @param D dependent matrix associated with a master component
    * @param B matrix associated with a slave component
    */
   protected abstract void mulSubG (
      MatrixBlock D, MatrixBlock B, int idx);

   /** 
    * Computes
    * <pre>
    *       T
    * y -= G  x
    * </pre>
    * where y and x are vectors associated with master and slave components,
    * respectively, and G is the constraint matrix for the attachment.
    * @param ybuf buffer into which to store result
    * @param yoff offset into ybuf
    * @param xbuf buffer containing right hand side vector
    * @param xoff offset into xbuf
    * @param idx master component index
    */
   protected abstract void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx);

//   protected abstract MatrixBlock createRowBlock (int colSize);
//
//   protected abstract MatrixBlock createColBlock (int rowSize);

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

   /**
    * Orders a list of (possibly interdependent) attachments so that the
    * masters of any given attachment do not depend on any attachments further
    * along the list. This means that state can be updated correctly by
    * starting at the beginning of the list and running through to the end.  It
    * also implicitly requires that the attachment configuration does not
    * contain loops.
    */
   public static ArrayList<DynamicAttachment> createOrderedList (
      List<DynamicAttachment> list) {
      
      ArrayList<DynamicAttachment> result = new ArrayList<DynamicAttachment>();
      LinkedList<DynamicAttachment> queue = new LinkedList<DynamicAttachment>();
      for (DynamicAttachment a : list) {
         a.attachedMasterCnt = 0;
         for (DynamicComponent m : a.getMasters()) {
            if (m.isAttached()) {
               a.attachedMasterCnt++;
            }
         }
         if (a.attachedMasterCnt == 0) {
            queue.offer (a);
         }
      }
      while (!queue.isEmpty()) {
         DynamicAttachment a = queue.poll();
         if (a.getSlave() != null) {
            LinkedList<DynamicAttachment> masterAttachments =
               a.getSlave().getMasterAttachments();
            if (masterAttachments != null) {
               for (DynamicAttachment b : masterAttachments) {
                  b.attachedMasterCnt--;
                  if (b.attachedMasterCnt == 0) {
                     queue.offer (b);
                  }
               }
            }
         }
         result.add (a);
      }
      return result;      
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      DynamicComponent s = getSlave();
      if (s != null) {
         refs.add (s);
      }
      for (DynamicComponent m : getMasters()) {
         refs.add (m);
      }
   }

   /**
    * Update the attachment position state whenever we connect to the parent
    * (i.e., plug in to the hierarchy).
    */
   public void connectToHierarchy () {
      updatePosStates();
      super.connectToHierarchy ();
      DynamicComponent slave = getSlave();
      if (slave != null) {
         slave.setAttached (this);
      }
      addBackRefs();
   }
   
   /**
    * Update the attachment position state whenever we connect to the parent
    * (i.e., plug in to the hierarchy).
    */
   public void disconnectFromHierarchy () {
      super.disconnectFromHierarchy ();
      DynamicComponent slave = getSlave();
      if (slave != null) {
         slave.setAttached (null);
      }
      removeBackRefs();
   }

   public Object clone() throws CloneNotSupportedException {
      DynamicAttachment a = (DynamicAttachment)super.clone();
      a.myMasterBlks = null;
      return a;
   }

   public DynamicAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      DynamicAttachment a = (DynamicAttachment)super.copy (flags, copyMap);
      a.myMasterBlks = null;
      return a;
   }   
  
}
