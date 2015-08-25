/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Implements a single linked list of MatrixBlocks, sorted by their index
 * values. Used for constructing columns of a sparse matrix of MatrixBlocks.
 */
public class MatrixBlockColList {
   protected MatrixBlock myHead;
   protected MatrixBlock myTail;
   protected MatrixBlock myLast; // last block added, or following last removed

   public MatrixBlockColList() {
      myHead = myTail = myLast = null;
   }

   public boolean isEmpty() {
      return myHead == null;
   }

   public int size() {
      int cnt = 0;
      for (MatrixBlock blk = myHead; blk != null; blk = blk.down()) {
         cnt++;
      }
      return cnt;
   }

   public MatrixBlock get (int bi) {
      if (myTail == null) {
         return null;
      }
      else if (myTail.getBlockRow() <= bi) {
         return myTail.getBlockRow() == bi ? myTail : null;
      }
      for (MatrixBlock blk = myHead; blk != null; blk = blk.down()) {
         if (blk.getBlockRow() == bi) {
            return blk;
         }
         else if (blk.getBlockRow() > bi) {
            return null;
         }
      }
      return null;
   }

   private void remove (MatrixBlock prevBlk, MatrixBlock oldBlk) {
      myLast = oldBlk.down();
      if (prevBlk != null) {
         prevBlk.setDown (oldBlk.down());
      }
      else {
         myHead = oldBlk.down();
      }
      if (oldBlk.down() == null) {
         myTail = prevBlk;
      }
   }

   public boolean remove (MatrixBlock oldBlk) {
      MatrixBlock prevBlk = null;
      MatrixBlock blk;
      for (blk = myHead; blk != null; blk = blk.down()) {
         if (blk == oldBlk) {
            break;
         }
         prevBlk = blk;
      }
      if (blk == null) {
         return false;
      }
      remove (prevBlk, oldBlk);
      return true;
   }

   public void removeAll() {
      myHead = myTail = myLast = null;
   }

   public MatrixBlock add (MatrixBlock newBlk) {
      MatrixBlock prevBlk = null;
      MatrixBlock oldBlk = null;
      int bi = newBlk.getBlockRow();

      if (myHead != null) { // ensure consistency
         if (myHead.colSize() != newBlk.colSize()) {
            throw new IllegalArgumentException ("inconsistent column size");
         }
      }
      // // check end of list first in case blocks are being added in order
      // if (myTail == null || myTail.getBlockRow() < bi) {
      //    prevBlk = myTail;
      // }
      // else {
      //    for (MatrixBlock blk = myHead; blk != null; blk = blk.down()) {
      //       if (blk.getBlockRow() == bi) {
      //          int bj = blk.getBlockCol();
      //          if (SparseBlockMatrix.warningLevel > 0) {
      //             System.out.println (
      //               "Warning: replacing SparseBlockMatrix block ("+bi+","+bj+")");
      //          }
      //          remove (prevBlk, blk);
      //          oldBlk = blk;
      //          break;
      //       }
      //       else if (blk.getBlockRow() > bi) {
      //          break;
      //       }
      //       else {
      //          prevBlk = blk;
      //       }
      //    }
      // }

      // check myLast to start search for insertion location, in case
      // blocks are being added in order
      MatrixBlock start = myHead;
      if (myLast != null && myLast.getBlockRow() < bi) {
         start = myLast;
      }
      for (MatrixBlock blk = start; blk != null; blk = blk.down()) {
         if (blk.getBlockRow() == bi) {
            int bj = blk.getBlockCol();
            if (SparseBlockMatrix.warningLevel > 0) {
               System.out.println (
                  "Warning: replacing SparseBlockMatrix block ("+bi+","+bj+")");
            }
            remove (prevBlk, blk);
            oldBlk = blk;
            break;
         }
         else if (blk.getBlockRow() > bi) {
            break;
         }
         else {
            prevBlk = blk;
         }
      }

      if (prevBlk == null) { // add to beginning of list
         newBlk.setDown (myHead);
         if (myHead == null) {
            myTail = newBlk;
         }
         myHead = newBlk;
      }
      else { // add after prevBlk
         newBlk.setDown (prevBlk.down());
         if (prevBlk.down() == null) {
            myTail = newBlk;
         }
         prevBlk.setDown (newBlk);
      }
      myLast = newBlk;
      return oldBlk;
   }

   // public void mulAdd (double[] y, double[] x)
   // {
   // for (MatrixBlock blk=myHead; blk!=null; blk=blk.down())
   // { blk.mulAdd (y, x);
   // }
   // }

   // public void mulAdd (double[] y, double[] x, int numBlkRows)
   // {
   // for (MatrixBlock blk=myHead;
   // blk!=null && blk.getBlockRow()<numBlkRows; blk=blk.down())
   // { blk.mulAdd (y, x);
   // }
   // }
}
