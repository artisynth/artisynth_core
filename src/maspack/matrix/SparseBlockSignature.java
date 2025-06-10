package maspack.matrix;

import java.util.Arrays;

import maspack.util.TestException;
import maspack.util.InternalErrorException;

/**
 * Structure signature for a sparse matrix, and a sparse block matrix in
 * particular.
 */
public class SparseBlockSignature {
   
   int myNumBlockRows;
   int myNumBlockCols;
   int myRowSize;
   int myColSize;
   int[] myRowOffsets;
   int[] myColOffsets;
   int[] myData;
   int[] myBlockOffs;
   int[] myOrderedEntries;

   boolean myVertical;

   public SparseBlockSignature (
      int rowSize, int colSize, boolean vertical, int[] data) {
      myNumBlockRows = rowSize;
      myNumBlockCols = colSize;
      myVertical = vertical;
      if (data == null) {
         throw new IllegalArgumentException (
            "data argument can't be null");
      }
      myData = data;
   }
   
   public SparseBlockSignature (SparseBlockMatrix S, boolean vertical) {
      if (vertical && !S.isVerticallyLinked()) {
         throw new IllegalArgumentException (
            "vertical set to ture but matrix is not vertically linked");
      }
      myNumBlockRows = S.numBlockRows();
      myNumBlockCols = S.numBlockCols();
      myRowSize = S.rowSize();
      myColSize = S.colSize();
      myRowOffsets = Arrays.copyOf (S.myRowOffsets, myNumBlockRows+1);
      myColOffsets = Arrays.copyOf (S.myColOffsets, myNumBlockCols+1);
      myVertical = vertical;

      int[] data;
      if (vertical) {
         data = new int[myNumBlockCols+S.numBlocks()];
      }
      else {
         data = new int[myNumBlockRows+S.numBlocks()];
      }
      int k = 0;
      MatrixBlock blk;
      if (vertical) {
         for (int bj=0; bj<myNumBlockCols; bj++) {
            int numBlksIdx = k++;
            for (blk=S.firstBlockInCol(bj); blk!=null; blk=blk.down()) {
               data[k++] = blk.getBlockRow();
            }
            data[numBlksIdx] = k-numBlksIdx-1;
         }
      }
      else {
         for (int bi=0; bi<myNumBlockRows; bi++) {
            int numBlksIdx = k++;
            for (blk=S.firstBlockInRow(bi); blk!=null; blk=blk.next()) {
               data[k++] = blk.getBlockCol();
            }
            data[numBlksIdx] = k-numBlksIdx-1;
         }
      }
      myData = data;
   }

   public boolean equals (SparseBlockMatrix S) {
      if (myVertical && !S.isVerticallyLinked()) {
         throw new IllegalArgumentException (
            "signature is vertical but matrix is not vertically linked");
      }
      if (S.rowSize() != myRowSize || S.numBlockRows() != myNumBlockRows ||
          S.colSize() != myColSize || S.numBlockCols() != myNumBlockCols) {
         return false;
      }
      for (int bi=0; bi<=myNumBlockRows; bi++) {
         if (S.myRowOffsets[bi] != myRowOffsets[bi]) {
            return false;
         }
      }
      for (int bj=0; bj<=myNumBlockCols; bj++) {
         if (S.myColOffsets[bj] != myColOffsets[bj]) {
            return false;
         }
      }
      int dataLength;
      if (isVertical()) {
         dataLength = myNumBlockCols+S.numBlocks();
      }
      else {
         dataLength = myNumBlockRows+S.numBlocks();
      }
      if (myData.length != dataLength) {
         return false;
      }
      int k = 0;
      MatrixBlock blk;
      if (isVertical()) {
         for (int bj=0; bj<myNumBlockCols; bj++) {
            int numBlksIdx = k++;
            for (blk=S.firstBlockInCol(bj); blk!=null; blk=blk.down()) {
               if (myData[k++] != blk.getBlockRow()) {
                  return false;
               }
            }
            if (myData[numBlksIdx] != k-numBlksIdx-1) {
               return false;
            }
         }
      }
      else {
         for (int bi=0; bi<myNumBlockRows; bi++) {
            int numBlksIdx = k++;
            for (blk=S.firstBlockInRow(bi); blk!=null; blk=blk.next()) {
               if (myData[k++] != blk.getBlockCol()) {
                  return false;
               }
            }
            if (myData[numBlksIdx] != k-numBlksIdx-1) {
               return false;
            }
         }
      }
      return true;
   }

   public int rowSize() {
      return myNumBlockRows;
   }

   public int colSize() {
      return myNumBlockCols;
   }

   public int[] getData() {
      return myData;
   }

   public boolean isVertical() {
      return myVertical;
   }

   public boolean equals (SparseBlockSignature sig) {
      if (myNumBlockRows != sig.myNumBlockRows || 
          myNumBlockCols != sig.myNumBlockCols || 
          myVertical != sig.myVertical || 
          myData.length != sig.myData.length) {
         return false;
      }
      for (int bi=0; bi<=myNumBlockRows; bi++) {
         if (myRowOffsets[bi] != sig.myRowOffsets[bi]) {
            return false;
         }
      }      
      for (int bj=0; bj<=myNumBlockCols; bj++) {
         if (myColOffsets[bj] != sig.myColOffsets[bj]) {
            return false;
         }
      }      
      for (int i=0; i<myData.length; i++) {
         if (myData[i] != sig.myData[i]) {
            return false;
         }
      }
      return true;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append ("[ ");
      sb.append (" rowSize=" + myNumBlockRows);
      sb.append (" colSize=" + myNumBlockCols);
      sb.append (" vertical=" + myVertical);
      sb.append (" data.length=" + myData.length);
      sb.append ("]");
      return sb.toString();
   }

   int numBlockEntries() {
      return myVertical ? myNumBlockCols : myNumBlockRows;
   }

   public int[] getOrderedEntries () {
      if (myOrderedEntries == null) {
         int numEntries = numBlockEntries();
         myBlockOffs = new int[numEntries];
         myOrderedEntries = new int[numEntries];
         int[] oldOrder = new int[numEntries];
         int off = 0;
         for (int i=0; i<numEntries; i++) {
            oldOrder[i] = i;
            myOrderedEntries[i] = i;
            myBlockOffs[i] = off;
            off += myData[off]+1;
         }
         lexicalSplitMerge (myOrderedEntries, oldOrder, 0, numEntries);
      }
      return myOrderedEntries;
   }

   public void lexicalSplitMerge (
      int[] newOrder, int[] oldOrder, int ibegin, int iend) {

      if (iend-ibegin <= 1) {
         return;
      }
      int imid = (iend+ibegin)/2;
      lexicalSplitMerge (oldOrder, newOrder, ibegin, imid);
      lexicalSplitMerge (oldOrder, newOrder, imid, iend);
      lexicalMerge (newOrder, oldOrder, ibegin, imid, iend);
   }

   void lexicalMerge (
      int[] newOrder, int[] oldOrder, int ibegin, int imid, int iend) {
      
      int i = ibegin;
      int j = imid;

      for (int k=ibegin; k<iend; k++) {
         if (i < imid &&
             (j >= iend || compareBlockEntries (
                myBlockOffs[oldOrder[i]], myBlockOffs[oldOrder[j]]) <= 0)) {
            newOrder[k] = oldOrder[i++];
         }
         else {
            newOrder[k] = oldOrder[j++];
         }
      }
   }

   void verifyLexicalMergeSort (int[] order) {
      int numEntries = numBlockEntries();
      if (order.length != numEntries) {
         throw new TestException (
            "order.length=" + order.length + ", expected " + numEntries);
      }
      // make sure all entries are accounted for
      boolean[] present = new boolean[numEntries];
      for (int i=0; i<numEntries; i++) {
         present[order[i]] = true;
      }
      for (int i=0; i<numEntries; i++) {
         if (!present[i]) {
            throw new TestException (
               "entry "+i+" is not present in the sort");
         }
      }
      for (int i=0; i<numEntries-1; i++) {
         if (compareBlockEntries (
                myBlockOffs[order[i]], myBlockOffs[order[i+1]]) > 0) {
            System.out.println ("order=" + new VectorNi(order));
            printOffsets ("blockOffsets:", myBlockOffs, myData);
            throw new TestException (
               "Sorted entry at "+i+" exceeds entry at "+(i+1));
         }
      }
   }

   int getBlockColSize (int bj) {
      return myColOffsets[bj+1] - myColOffsets[bj];
   }
   
   int getBlockRowSize (int bi) {
      return myRowOffsets[bi+1] - myRowOffsets[bi];
   }
   
   int compareBlockEntries (
      int bj0, int bj1, SparseBlockSignature sig1) {

      return compareBlockEntries (
         myBlockOffs[bj0], myData,
         sig1.myBlockOffs[bj1], sig1.myData);
   }

   private int compareBlockEntries (int off0, int off1) {
      return compareBlockEntries (off0, myData, off1, myData);
   }
   
   private int compareBlockEntries (
      int off0, int[] data0, int off1, int[] data1) {
      
      int numb0 = data0[off0++];
      int numb1 = data1[off1++];
      if (numb0 < numb1) {
         return -1;
      }
      else if (numb0 > numb1) {
         return 1;
      }
      else {
         for (int i=0; i<numb0; i++) {
            int bidx0 = data0[off0++];
            int bidx1 = data1[off1++];
            if (bidx0 < bidx1) {
               return -1;
            }
            else if (bidx0 > bidx1) {
               return 1;
            }
         }
         return 0;
      }
   }

   public int[] getRowOrdering (int[] blockOrdering) {
      if (blockOrdering.length != myNumBlockRows) {
         throw new IllegalArgumentException (
            "blockOrdering length "+blockOrdering.length+
            " != num block rows "+myNumBlockRows);
      }
      int[] ordering = new int[myData[myNumBlockRows]];
      int i = 0;
      for (int bi=0; bi<blockOrdering.length; bi++) {
         int offIdx = blockOrdering[bi];
         int off = myData[offIdx];
         int size = myData[offIdx+1] - off;
         while (size > 0) {
            ordering[i++] = off++;
            size--;
         }
      }
      return ordering;
   }

   public int[] getColOrdering (int[] blockOrdering) {
      if (blockOrdering.length != myNumBlockCols) {
         throw new IllegalArgumentException (
            "blockOrdering length "+blockOrdering.length+
            " != num block cols "+myNumBlockCols);
      }
      int base = myNumBlockRows+1;
      int[] ordering = new int[myData[base+myNumBlockCols]];
      int i = 0;
      for (int bj=0; bj<blockOrdering.length; bj++) {
         int offIdx = blockOrdering[bj]+base;
         int off = myData[offIdx];
         int size = myData[offIdx+1] - off;
         while (size > 0) {
            ordering[i++] = off++;
            size--;
         }
      }
      return ordering;
   }

   String getOffStr (int off, int[] data) {
      int size = data[off++];
      StringBuilder sb = new StringBuilder();
      sb.append (size + ": ");
      for (int i=0; i<size; i++) {
         sb.append (data[off++]+" ");
      }
      return sb.toString();
   }

   void printOffsets (String msg, int[] offs, int[] data) {
      System.out.println (msg);
      for (int i=0; i<offs.length; i++) {
         System.out.println (" " + getOffStr (offs[i], data));
      }
   }

   /**
    * Helper class used by computePrevColIdxsAlt.
    */
   private class BlockCols {
      int col;
      int colMax;

      boolean hasColsLeft() {
         return col < colMax;
      }

      void init (SparseBlockSignature sig, int bj) {
         col = sig.myColOffsets[bj];
         colMax = col + sig.getBlockColSize (bj);
      }
   }

   /**
    * Compute the previous column indices of the matrix identified by {@code
    * sigP} with respect the matrix indentifies by this signature.  The
    * functionality is the same as {@link #computePrevColIdxs}. Despite
    * attempts to make this method more efficient than the latter, it is not
    * clear which is actually faster, with some timings showing it slower by a
    * factor of around 1.5 or more for matrices dominated by contact
    * constraints.
    */
   public int[] computePrevColIdxsAlt (SparseBlockSignature sigP) {
      // in the following code, S and P denote the matrices associated with
      // this signature and sipP, respectively
      
      int[] orderedS = getOrderedEntries();
      int[] orderedP = sigP.getOrderedEntries();
      int[] prevIdxs = new int[myColSize];

      for (int i=0; i<prevIdxs.length; i++) {
         prevIdxs[i] = -1;
      }
      if (orderedP.length > 0) {
         BlockCols pcols = new BlockCols();
         BlockCols scols = new BlockCols();
         int kp, ks; // indices into sorted block entries for S and P
         int jp, js; // indices into original block entries for S and P
         
         kp = 0;
         pcols.init (sigP, (jp=orderedP[kp]));
         
         // interate over sorted S blocks and advance kp to match:
         for (ks=0; ks<orderedS.length && kp <orderedP.length; ks++) {
            scols.init (this, (js=orderedS[ks]));

            int res; // comparison result between S and P blocks
            // skip ordered P blocks for which are "less" the S
            while ((res=compareBlockEntries (js, jp, sigP)) > 0 &&
                   ++kp < orderedP.length) {
               pcols.init (sigP, (jp=orderedP[kp]));
            }
            if (res == 0 && kp < orderedP.length) {
               // current S and P blocks match, so try to find matching columns
               while (scols.hasColsLeft()) {
                  if (!pcols.hasColsLeft()) {
                     // no more columns left in P block, so look at the next one
                     if (++kp < orderedP.length) {
                        pcols.init (sigP, (jp=orderedP[kp]));
                        res = compareBlockEntries (js, jp, sigP);
                        if (res != 0) {
                           break; // P block exceeds S
                        }
                     }
                     else {
                        break; // no more P block left
                     }
                  }
                  prevIdxs[scols.col++] = pcols.col++;
               }
            }
         }
      }
      return prevIdxs;
   }      

   /**
    * Compute the previous column indices of the matrix identified by {@code
    * sigP} with respect the matrix indentifies by this signature.
    */   
   public int[] computePrevColIdxs (SparseBlockSignature prev) {

      if (prev.myVertical != myVertical) {
         throw new IllegalArgumentException (
            "vertical setting for the two signatures do not match");
      }

      int numEntries = numBlockEntries();
      int[] blkOrder = getOrderedEntries ();

      int numEntriesPrev = prev.numBlockEntries();
      int[] blkOrderPrev = prev.getOrderedEntries();
      
      int base = 1+myNumBlockRows;
      int[] colOrder = new int[myColSize];
      int[] colOffs = new int[myColSize];

      int i = 0;
      for (int k=0; k<blkOrder.length; k++) {
         int bj = blkOrder[k];
         int off = myColOffsets[bj];
         int size = myColOffsets[bj+1] - off;
         int colOff = myBlockOffs[bj];
         while (size > 0) {
            colOffs[i] = colOff;
            colOrder[i++] = off++;
            size--;
         }
      }

      base = 1+prev.myNumBlockRows;
      int[] colOrderPrev = new int[prev.myColSize];
      int[] colOffsPrev = new int[prev.myColSize];

      i = 0;
      for (int k=0; k<blkOrderPrev.length; k++) {
         int bj = blkOrderPrev[k];
         int off = prev.myColOffsets[bj];
         int size = prev.myColOffsets[bj+1] - off;
         int colOff = prev.myBlockOffs[bj];
         while (size > 0) {
            colOffsPrev[i] = colOff;
            colOrderPrev[i++] = off++;
            size--;
         }
      }

      int[] prevIdxs = new int[myColSize];
      for (i=0; i<prevIdxs.length; i++) {
         prevIdxs[i] = -1;
      }
      int k = 0;
      for (i=0; i<prevIdxs.length; i++) {
         int res = 0;
         while (k < prev.myColSize &&
                (res=compareBlockEntries (
                   colOffs[i], myData,
                   colOffsPrev[k], prev.myData)) > 0) {
            k++;
         }
         if (k == prev.myColSize) {
            break;
         }
         if (res == 0) {
            prevIdxs[colOrder[i]] = colOrderPrev[k++];
         }
      }
      return prevIdxs;
   }
}
