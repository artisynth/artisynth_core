/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Arrays;

import maspack.util.TestException;

/**
 * A version of SparseBlockMatrix that allows blocks to be accessed by number
 */
public class SparseNumberedBlockMatrix extends SparseBlockMatrix {

   // code to implement the number map
                                               
   protected MatrixBlock[] myNumberMap;
   // next number to be assigned when all free numbers are used up
   protected int myMaxNumber; 
   protected int[] myFreeNumbers;
   protected int myNumFreeNumbers;
   protected int myInitialCapacity = -1;

   private int allocNumber() {
      int num;
      if (myNumFreeNumbers > 0) {
         num = myFreeNumbers[--myNumFreeNumbers];
      }
      else {
         num = myMaxNumber;
      }
      if (num >= myMaxNumber) {
         myMaxNumber = num + 1;
      }
      if (num >= myNumberMap.length) {
         myNumberMap = Arrays.copyOf (myNumberMap, Math.max(2*num, num+1));
      }
      return num;
   }

   private void freeNumber (int num) {
      if (myNumFreeNumbers >= myFreeNumbers.length) {
         // grow the list of free numbers
         myFreeNumbers = Arrays.copyOf (
            myFreeNumbers, (3*myFreeNumbers.length)/2+1);
      }
      myFreeNumbers[myNumFreeNumbers++] = num;
      if (num == myMaxNumber - 1) {
         int k = num - 1;
         while (k >= 0 && myNumberMap[k] == null) {
            k--;
         }
         myMaxNumber = k + 1;
      }
   }

   public SparseNumberedBlockMatrix() {
      this (new int[0], -1);
   }

   public SparseNumberedBlockMatrix (int[] rowColSizes) {
      this (rowColSizes, rowColSizes, -1);
   }

   public SparseNumberedBlockMatrix (int[] rowSizes, int[] colSizes) {
      this (rowSizes, colSizes, -1);
   }

   public SparseNumberedBlockMatrix (int[] rowColSizes, int initialCapacity) {
      this (rowColSizes, rowColSizes, initialCapacity);
   }

   protected void initRowColSizes (int[] rowSizes, int[] colSizes) {
      super.initRowColSizes (rowSizes, colSizes);

      if (myInitialCapacity < 0) {
         int initialCapacity =
            Math.max(16, Math.max (myNumBlockRows, myNumBlockCols));
         myNumberMap = new MatrixBlock[initialCapacity];
      }
   }

   public SparseNumberedBlockMatrix (
      int[] rowSizes, int[] colSizes, int initialCapacity) {
      super (rowSizes, colSizes);

      myInitialCapacity = initialCapacity;
      if (initialCapacity < 0) {
         initialCapacity =
            Math.max(16, Math.max (myNumBlockRows, myNumBlockCols));
      }
      myNumberMap = new MatrixBlock[initialCapacity];
      myFreeNumbers = new int[0];
      myMaxNumber = 0;
      myNumFreeNumbers = 0;
   }

   public int addBlock (int bi, int bj, MatrixBlock blk) {
      MatrixBlock oldBlk = addBlockWithoutNumber (bi, bj, blk);
      int num;
      if (oldBlk == null) {
         num = allocNumber();
      }
      else {
         // just reuse the old number
         num = oldBlk.getBlockNumber();
      }
      blk.setBlockNumber (num);
      myNumberMap[num] = blk;
      return num;
   }

   public boolean removeBlock (MatrixBlock oldBlk) {
      if (removeBlockWithoutNumber (oldBlk)) {
         int num = oldBlk.getBlockNumber();
         myNumberMap[num] = null;
         freeNumber (num);
         oldBlk.setBlockNumber (-1);
         return true;
      }
      else {
         return false;
      }
   }

   public void removeAllBlocks() {
      super.removeAllBlocks();
      for (int i=0; i<myNumberMap.length; i++) {
         myNumberMap[i] = null;
      }
      myMaxNumber = 0;
      myNumFreeNumbers = 0;
   }

   public MatrixBlock getBlockByNumber (int num) {
      return myNumberMap[num];
   }

   public void checkConsistency() {
      super.checkConsistency();

      int numBlksChk = 0;
      for (int bi=0; bi<myNumBlockRows; bi++) {
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk = blk.next()) {
            if (myNumberMap[blk.getBlockNumber()] != blk) {
               throw new TestException (
                  "inconsistent number map entry for blk "+blk.getBlockNumber());
            }
            numBlksChk++;
         }
      }
      int numBlks = 0;
      int numFree = 0;
      for (int i=0; i<myNumberMap.length; i++) {
         MatrixBlock blk = myNumberMap[i];
         // see if i is a free number 
         boolean iIsFree = false;
         for (int k=0; k<myNumFreeNumbers; k++) {
            if (myFreeNumbers[k] == i) {
               iIsFree = true;
               break;
            }
         } 
         if (blk != null) {
            if (iIsFree) {
               throw new TestException (
                  "number map index "+i+" is free but contains a block");
            }
            if (blk.getBlockNumber() != i) {
               throw new TestException (
                  "inconsistent number map entry for blk "+blk.getBlockNumber());
            }
            numBlks++;
         }
         else {
            // see if i is a free number 
            if (i < myMaxNumber) {
               if (!iIsFree) {
                  throw new TestException (
                     "number "+i+" is not on the free list");
               }
               numFree++;
            }
            else {
               if (iIsFree) {
                  throw new TestException (
                     "number "+i+" is on the free list");
               }
            }
         }
      }
      if (numBlks != numBlksChk) {
         throw new TestException (
            "map has "+numBlks+" but matrix has only "+numBlksChk);
      }
      if (numBlks+numFree != myMaxNumber) {
         throw new TestException (
            "myMaxNumber="+myMaxNumber+", expecting "+(numBlks+numFree));
      }

   }

   /**
    * Creates a clone of this NumberedSparseBlockMatrix, along with clones of
    * all the associated MatrixBlocks.
    */
   public SparseNumberedBlockMatrix clone() {
      SparseNumberedBlockMatrix M = (SparseNumberedBlockMatrix)super.clone();

      M.myNumberMap = new MatrixBlock[myNumberMap.length];
      for (int bi=0; bi<myNumBlockRows; bi++) {
         for (MatrixBlock blk=M.myRows[bi].myHead; blk!=null; blk = blk.next()) {
            M.myNumberMap[blk.getBlockNumber()] = blk;
         }
      }
      M.myFreeNumbers = Arrays.copyOf (myFreeNumbers, myNumFreeNumbers);
      return M;
   }
}
