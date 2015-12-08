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
public class MatrixBlockRowList {
   protected MatrixBlock myHead;
   protected MatrixBlock myTail;
   protected MatrixBlock myLast; // last block added, or following last removed

   public MatrixBlockRowList() {
      myHead = myTail = myLast = null;
   }

   public boolean isEmpty() {
      return myHead == null;
   }

   public int size() {
      int cnt = 0;
      for (MatrixBlock blk = myHead; blk != null; blk = blk.next()) {
         cnt++;
      }
      return cnt;
   }

   public MatrixBlock firstBlock() {
      return myHead;
   }

   public MatrixBlock get (int bj) {
      if (myTail == null) {
         return null;
      }
      else if (myTail.getBlockCol() <= bj) {
         MatrixBlock blk = myTail.getBlockCol() == bj ? myTail : null;
         return blk;
      }
      for (MatrixBlock blk = myHead; blk != null; blk = blk.next()) {
         if (blk.getBlockCol() == bj) {
            return blk;
         }
         else if (blk.getBlockCol() > bj) {
            return null;
         }
      }
      return null;
   }

   private void remove (MatrixBlock prevBlk, MatrixBlock oldBlk) {
      myLast = oldBlk.next();
      if (prevBlk != null) {
         prevBlk.setNext (oldBlk.next());
      }
      else {
         myHead = oldBlk.next();
      }
      if (oldBlk.next() == null) {
         myTail = prevBlk;
      }
   }

   public boolean remove (MatrixBlock oldBlk) {
      MatrixBlock prevBlk = null;
      MatrixBlock blk;
      for (blk = myHead; blk != null; blk = blk.next()) {
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
      int bj = newBlk.getBlockCol();

      if (myHead != null) { // ensure consistency
         if (myHead.rowSize() != newBlk.rowSize()) {
            throw new IllegalArgumentException ("inconsistent row size");
         }
      }
      // // check end of list first in case blocks are being added in order
      // if (myTail == null || myTail.getBlockCol() < bj) {
      //    prevBlk = myTail;
      // }
      // else {
      //    for (MatrixBlock blk = myHead; blk != null; blk = blk.next()) {
      //       if (blk.getBlockCol() == bj) {
      //          int bi = blk.getBlockRow();
      //          if (SparseBlockMatrix.warningLevel > 0) {
      //             System.out.println (
      //               "Warning: replacing SparseBlockMatrix block ("+bi+","+bj+")");
      //          }
      //          remove (prevBlk, blk);
      //          oldBlk = blk;
      //          break;
      //       }
      //       else if (blk.getBlockCol() > bj) {
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
      if (myLast != null && myLast.getBlockCol() < bj) {
         start = myLast;
      }
      for (MatrixBlock blk = start; blk != null; blk = blk.next()) {
         if (blk.getBlockCol() == bj) {
            int bi = blk.getBlockRow();
            if (SparseBlockMatrix.warningLevel > 0) {
               System.out.println (
                  "Warning: replacing SparseBlockMatrix block ("+bi+","+bj+")");
            }
            remove (prevBlk, blk);
            oldBlk = blk;
            break;
         }
         else if (blk.getBlockCol() > bj) {
            break;
         }
         else {
            prevBlk = blk;
         }
      }

      if (prevBlk == null) { // add to beginning of list
         newBlk.setNext (myHead);
         if (myHead == null) {
            myTail = newBlk;
         }
         myHead = newBlk;
      }
      else { // add after prevBlk
         newBlk.setNext (prevBlk.next());
         if (prevBlk.next() == null) {
            myTail = newBlk;
         }
         prevBlk.setNext (newBlk);
      }
      myLast = newBlk;
      return oldBlk;
   }

   // public void mulAdd (double[] y, double[] x)
   // {
   // for (MatrixBlock blk=myHead; blk!=null; blk=blk.next())
   // { blk.mulAdd (y, blk.getRowOffset(), x, blk.getColOffset());
   // }
   // }
   //
   // public void mulAdd (double[] y, double[] x, int ncols)
   // {
   // for (MatrixBlock blk=myHead;
   // blk!=null && blk.getColOffset()<ncols; blk=blk.next())
   // { blk.mulAdd (y, blk.getRowOffset(), x, blk.getColOffset());
   // }
   // }
}
