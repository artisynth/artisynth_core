/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;

import maspack.util.Clonable;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;
import maspack.util.Scan;
import maspack.util.TestException;

/**
 * Implements a square sparse matrix composed of MatrixBlocks.
 */
public class SparseBlockMatrix extends SparseMatrixBase implements Clonable {
   protected MatrixBlockRowList[] myRows;
   protected MatrixBlockColList[] myCols;
   protected int myNumBlockRows;
   protected int myNumBlockCols;

   static public int warningLevel = 1;

   // amount by which to increase row/column information arrays when
   // matrix is enlarged on demand.
   // private static int bufferSizeInc = 256;

   protected int[] myRowOffsets;  // starting row index for each block row
   protected int[] myColOffsets;  // starting col index for each block col
   protected int myNumRows;
   protected int myNumCols;

   // cached row offsets for the methods addNumNonZerosByRow(), getCRSIndices(),
   // and getCRSValues(): 
   protected int[] myCRSRowOffs;
   // signature variables the the current CRSRowOffs:
   protected Partition myRowIndicesPartition = Partition.None;
   protected int myCRSNumBlkRows = -1;
   protected int myCRSNumBlkCols = -1; 
   
   // cached col offsets for the methods addNumNonZerosByCol(), getCCSIndices(),
   // and getCCSValues():
   protected int[] myCCSColOffs;
   // signature variables the the current CCSColOffs:
   protected Partition myColIndicesPartition = Partition.None;
   protected int myCCSNumBlkRows = -1;
   protected int myCCSNumBlkCols = -1;


   protected boolean myVerticallyLinkedP = false;

   public enum PrintFormat {
      MatrixMarket, CRS, CCS,
   }

   private class BlockSize {
      int numBlkRows;
      int numBlkCols;

      BlockSize (int nbr, int nbc) {
         numBlkRows = nbr;
         numBlkCols = nbc;
      }

      public String toString() {
         return "" + numBlkRows + "x" + numBlkCols;
      }
   }

   private int[] resizeIntArray (int[] array, int size) {
      int[] newArray = new int[size];
      for (int i = 0; i < array.length && i < size; i++) {
         newArray[i] = array[i];
      }
      return newArray;
   }

   private void invalidateCRSandCCSOffsets() {
      myRowIndicesPartition = Partition.None;
      myCRSNumBlkRows = -1;
      myCRSNumBlkCols = -1;
      myCRSRowOffs = null;

      myColIndicesPartition = Partition.None;
      myCCSNumBlkRows = -1;
      myCCSNumBlkCols = -1;
      myCCSColOffs = null;
   }

   public void setRowCapacity (int newCap) {
      int nbk = myNumBlockRows;
      MatrixBlockRowList[] newRows = new MatrixBlockRowList[newCap];
      for (int i = 0; i < Math.min(nbk,newCap); i++) {
         newRows[i] = myRows[i];
      }
      // myRowOffsets needs at least one more entry than num cols
      myRowOffsets = resizeIntArray (myRowOffsets, newCap+1);
      myRows = newRows;
   }      

   public void addRow (int size) {
      int nbk = myNumBlockRows;
      if (nbk == myRows.length) {
         setRowCapacity (Math.max (((nbk+1)*3)/2, 256));
      }
      myRowOffsets[nbk+1] = myRowOffsets[nbk]+size;
      myRows[nbk] = new MatrixBlockRowList();
      myNumRows += size;

      myNumBlockRows++;
   }

   public void addRows (int[] sizes, int num) {
      int nbk = myNumBlockRows;
      if (nbk+num > myRows.length) {
         setRowCapacity (Math.max (((nbk+num)*3)/2, 256));
      }
      int nrows = 0;
      for (int k=0; k<num; k++) {
         myRowOffsets[nbk+k+1] = myRowOffsets[nbk+k]+sizes[k];
         nrows += sizes[k];
         myRows[nbk+k] = new MatrixBlockRowList();
      }
      myNumRows += nrows;
      myNumBlockRows += num;
   }
         
   public void removeRow (int rowIdx) {
      if (rowIdx < 0 || rowIdx >= myNumBlockRows) {
         throw new IllegalArgumentException (
            "Block row index " + rowIdx +
            " not in range [0,"+(myNumBlockRows-1)+"]");
      }
      if (isVerticallyLinked()) {
         // adjust vertical linkages for removed row
         MatrixBlock blk;
         for (blk=firstBlockInRow(rowIdx); blk!=null; blk=blk.next()) {
            int bj = blk.getBlockCol();
            MatrixBlock prevBlk = null;
            MatrixBlock vertBlk=firstBlockInCol(bj);
            while (vertBlk != blk && vertBlk != null) {
               prevBlk = vertBlk;
               vertBlk = vertBlk.down();
            }
            if (vertBlk == null) {
               throw new InternalErrorException (
                  "Block("+rowIdx+","+bj+") not found in vertical linking");
            }
            if (prevBlk == null) {
               myCols[bj].myHead = blk.down();
            }
            else {
               prevBlk.setDown (blk.down());
            }
            disposeBlock (blk);
         }
      }
      else {
         MatrixBlock blk;
         for (blk=firstBlockInRow(rowIdx); blk!=null; blk=blk.next()) {
            disposeBlock (blk);
         }
      }
      
      // adjust row offsets for remaining blocks
      for (int bi=rowIdx+1; bi<myNumBlockRows; bi++) {
         for (MatrixBlock blk=firstBlockInRow(bi); blk!=null; blk=blk.next()) {
            blk.setBlockRow (bi-1);
         }
      }

      // shift info for rows after rowIdx
      int rsize = getBlockRowSize(rowIdx);
      for (int k=rowIdx; k<myNumBlockRows-1; k++) {
         myRows[k] = myRows[k+1];
         myRowOffsets[k] = myRowOffsets[k+1]-rsize;
      }
      // adjust final offset 
      myRowOffsets[myNumBlockRows-1] = myRowOffsets[myNumBlockRows]-rsize;

      myNumRows -= rsize;
      myNumBlockRows--;
      invalidateCRSandCCSOffsets();
   }

   public void setColCapacity (int newCap) {
      int nbk = myNumBlockCols;
      MatrixBlockColList[] newCols = new MatrixBlockColList[newCap];
      for (int i = 0; i < Math.min(nbk, newCap); i++) {
         newCols[i] = myCols[i];
      }
      // myColOffsets needs at least one more entry than num cols
      myColOffsets = resizeIntArray (myColOffsets, newCap+1);
      myCols = newCols;
   }      

   public void addCol (int size) {
      int nbk = myNumBlockCols;
      if (nbk == myCols.length) {
         setColCapacity (Math.max (((nbk+1)*3)/2, 256));
      }
      myColOffsets[nbk+1] = myColOffsets[nbk]+size;
      if (myVerticallyLinkedP) {
         myCols[nbk] = new MatrixBlockColList();
      }
      myNumCols += size;
      myNumBlockCols++;
   }

   public void addCols (int[] sizes, int num) {
      int nbk = myNumBlockCols;
      if (nbk+num > myCols.length) {
         setColCapacity (Math.max (((nbk+num)*3)/2, 256));
      }
      int ncols = 0;
      for (int k=0; k<num; k++) {
         myColOffsets[nbk+k+1] = myColOffsets[nbk+k]+sizes[k];
         ncols += sizes[k];
         if (myVerticallyLinkedP) {
            myCols[nbk+k] = new MatrixBlockColList();
         }
      }
      myNumCols += ncols;
      myNumBlockCols += num;
   }

   public void removeCol (int colIdx) {
      if (colIdx < 0 || colIdx >= myNumBlockCols) {
         throw new IllegalArgumentException (
            "Block column index " + colIdx +
            " not in range [0,"+(myNumBlockCols-1)+"]");
      }

      // adjust horizontal linkages for removed col
      if (isVerticallyLinked()) {
         MatrixBlock blk;
         for (blk=firstBlockInCol(colIdx); blk!=null; blk=blk.down()) {
            int bi = blk.getBlockRow();
            MatrixBlock prevBlk = null;
            MatrixBlock horzBlk=firstBlockInRow(bi);
            while (horzBlk != blk && horzBlk != null) {
               prevBlk = horzBlk;
               horzBlk = horzBlk.next();
            }
            if (horzBlk == null) {
               throw new InternalErrorException (
                  "Block("+bi+","+colIdx+") not found in horizontal linking");
            }
            if (prevBlk == null) {
               myRows[bi].myHead = blk.next();
            }
            else {
               prevBlk.setNext (blk.next());
            }
            disposeBlock (blk);
         }
         for (int bj=colIdx+1; bj<myNumBlockCols; bj++) {
            for (blk=firstBlockInCol(bj); blk!=null; blk=blk.down()) {
               blk.setBlockCol (bj-1);
            }
         }
      }
      else {
         for (int bi=0; bi<numBlockRows(); bi++) {
            MatrixBlock prevBlk = null;
            MatrixBlock colBlk = null;
            for (MatrixBlock blk=firstBlockInRow(bi); blk!=null; blk=blk.next()) {
               int bj = blk.getBlockCol();
               if (bj < colIdx) {
                  prevBlk = blk;
               }
               else if (bj == colIdx) {
                  colBlk = blk;
               }
               else {
                  blk.setBlockCol (bj-1);
               }
            }
            if (colBlk != null && colBlk.getBlockCol() == colIdx) {
               if (prevBlk == null) {
                  myRows[bi].myHead = colBlk.next();
               }           
               else {
                  prevBlk.setNext (colBlk.next());
               }
               disposeBlock (colBlk);
            }
         }
      }

      // shift info for cols after colIdx
      int csize = getBlockColSize(colIdx);
      for (int k=colIdx; k<myNumBlockCols-1; k++) {
         myCols[k] = myCols[k+1];
         myColOffsets[k] = myColOffsets[k+1]-csize;
      }
      // adjust final offset 
      myColOffsets[myNumBlockCols-1] = myColOffsets[myNumBlockCols]-csize;
      
      myNumCols -= csize;
      myNumBlockCols--; 
      invalidateCRSandCCSOffsets();
   }

   public void removeCols (int[] blkColIdxs) {

      if (blkColIdxs.length > myNumBlockCols) {
         throw new IllegalArgumentException (
            "Number of block cols to delete exceeds number of block cols " +
            myNumBlockCols);
      }
      if (blkColIdxs.length == 0) {
         return; // trivial case
      }

      for (int k=0; k<blkColIdxs.length; k++) {
         int bidx = blkColIdxs[k];
         if (bidx < 0 || bidx >= myNumBlockCols) {
            throw new IllegalArgumentException (
               "Block col to delete "+bidx+" is out of range (0,"+
               myNumBlockCols+")");
         }
         if (k<blkColIdxs.length-1) {
            if (bidx >= blkColIdxs[k+1]) {
               throw new IllegalArgumentException (
                  "Block cols to delete not arranged in ascending order");
            }
         }
      }

      int[] newColIdxs = new int[myNumBlockCols];

      int k = 0;
      int bx = 0; // updated value bj
      int dsize = 0; // total number of deleted columns 
      int ndel = blkColIdxs.length; // number of deleted block columns
      for (int bj=0; bj<myNumBlockCols; bj++) {
         if (k < ndel && bj == blkColIdxs[k]) {
            dsize += getBlockColSize(bj);
            newColIdxs[bj] = -1;
            k++;
         }
         else {
            myCols[bx] = myCols[bj];
            myColOffsets[bx] = myColOffsets[bj]-dsize;
            newColIdxs[bj] = bx;
            bx++;
         }
      }
      // adjust final offset 
      myColOffsets[myNumBlockCols-ndel] = myColOffsets[myNumBlockCols]-dsize;
      
      myNumCols -= dsize;
      myNumBlockCols -= ndel; 

      // adjust column offsets for remaining blocks
      for (int bi=0; bi<myNumBlockRows; bi++) {
         MatrixBlock prev = null;
         for (MatrixBlock blk=firstBlockInRow(bi); blk!=null; blk=blk.next()) {
            int bj = blk.getBlockCol();
            if (newColIdxs[bj] == -1) {
               // delete block
               if (prev == null) {
                  myRows[bi].myHead = blk.next();
               }
               else {
                  prev.setNext (blk.next());
               }
               disposeBlock (blk);
            }
            else {
               blk.setBlockCol (newColIdxs[bj]);
               prev = blk;
            }
         }
      }
      invalidateCRSandCCSOffsets();
   }

   public void removeRows (int[] blkRowIdxs) {

      if (blkRowIdxs.length > myNumBlockRows) {
         throw new IllegalArgumentException (
            "Number of block rows to delete exceeds number of block rows " +
            myNumBlockRows);
      }
      if (blkRowIdxs.length == 0) {
         return; // trivial case
      }
      
      for (int k=0; k<blkRowIdxs.length; k++) {
         int bidx = blkRowIdxs[k];
         if (bidx < 0 || bidx >= myNumBlockRows) {
            throw new IllegalArgumentException (
               "Block row to delete "+bidx+" is out of range (0,"+
               myNumBlockRows+")");
         }
         if (k<blkRowIdxs.length-1) {
            if (bidx >= blkRowIdxs[k+1]) {
               throw new IllegalArgumentException (
                  "Block rows to delete not arranged in ascending order");
            }
         }
      }

      int ndel = blkRowIdxs.length; // number of deleted block rows
      int[] newRowIdxs = new int[myNumBlockRows];
      MatrixBlockRowList[] deletedRows = new MatrixBlockRowList[ndel];

      // remove rows and shifting existing rows to fill spaces
      MatrixBlock blk;
      int k = 0;
      int bx = 0; // updated value bi
      int dsize = 0; // total number of deleted rows
      for (int bi=0; bi<myNumBlockRows; bi++) {
         if (k < ndel && bi == blkRowIdxs[k]) {
            dsize += getBlockRowSize(bi);
            deletedRows[k] = myRows[bi];
            newRowIdxs[bi] = -1;
            k++;
         }
         else {
            myRows[bx] = myRows[bi];
            myRowOffsets[bx] = myRowOffsets[bi]-dsize;
            newRowIdxs[bi] = bx;
            bx++;
         }
      }

      // adjust final offset 
      myRowOffsets[myNumBlockRows-ndel] = myRowOffsets[myNumBlockRows]-dsize;

      myNumRows -= dsize;
      myNumBlockRows -= ndel;

      if (isVerticallyLinked()) {
         // adjust column offsets for remaining blocks
         for (int bj=0; bj<myNumBlockCols; bj++) {
            MatrixBlock prev = null;
            for (blk=firstBlockInCol(bj); blk!=null; blk=blk.down()) {
               int bi = blk.getBlockRow();
               if (newRowIdxs[bi] == -1) {
                  // delete block
                  if (prev == null) {
                     myCols[bj].myHead = blk.down();
                  }
                  else {
                     prev.setDown (blk.down());
                  }
               }
               else {
                  blk.setBlockRow (newRowIdxs[bi]);
                  prev = blk;
               }
            }
         }
      }
      else {
         for (int bi=0; bi<myNumBlockRows; bi++) {
            // adjust row offsets
            for (blk=firstBlockInRow(bi); blk!=null; blk=blk.next()) {
               blk.setBlockRow (bi);
            }
         }
      }

      // dispose of blocks. Needed for SparseNumberedBlockMatrix
      for (k=0; k<ndel; k++) {
         for (blk=deletedRows[k].myHead; blk!=null; blk=blk.next()) {
            disposeBlock (blk);
         }
      }
      invalidateCRSandCCSOffsets();
      
   }

   public void removeAllCols () {
      for (int bi=0; bi<myNumBlockRows; bi++) {
         myRows[bi].removeAll();
      }
      myNumBlockCols = 0;
      myNumCols = 0;
      invalidateCRSandCCSOffsets();
   }

   public void removeAllRows () {
      if (myVerticallyLinkedP) {
         for (int bj=0; bj<myNumBlockRows; bj++) {
            myCols[bj].removeAll();
         }
      }
      myNumBlockRows = 0;
      myNumRows = 0;
      invalidateCRSandCCSOffsets();
   }

   public SparseBlockMatrix() {
      this (new int[0]);
   }

   public SparseBlockMatrix (int[] rowColSizes) {
      this (rowColSizes, rowColSizes);
   }

   protected void initRowColSizes (int[] rowSizes, int[] colSizes) {
      myNumRows = 0;
      myNumBlockRows = rowSizes.length;
      myRowOffsets = new int[myNumBlockRows+1];
      // myOffsets = new int[myNumBlockRows][];
      for (int i = 0; i < myNumBlockRows; i++) {
         myRowOffsets[i] = myNumRows;
         myNumRows += rowSizes[i];
      }
      myRowOffsets[myNumBlockRows] = myNumRows;

      myNumCols = 0;
      myNumBlockCols = colSizes.length;
      myColOffsets = new int[myNumBlockCols+1];
      for (int i = 0; i < myNumBlockCols; i++) {
         myColOffsets[i] = myNumCols;
         myNumCols += colSizes[i];
      }
      myColOffsets[myNumBlockCols] = myNumCols;
         //      }
      myRows = new MatrixBlockRowList[myNumBlockRows];
      for (int i = 0; i < myNumBlockRows; i++) {
         myRows[i] = new MatrixBlockRowList();
      }
      myCols = new MatrixBlockColList[myNumBlockCols];
      if (myVerticallyLinkedP) {
         for (int i = 0; i < myNumBlockCols; i++) {
            myCols[i] = new MatrixBlockColList();
         }
      }
   }

   public SparseBlockMatrix (int[] rowSizes, int[] colSizes) {
      initRowColSizes (rowSizes, colSizes);
   }

   public int getBlockRowOffset (int bi) {
      return myRowOffsets[bi];
   }

   public int getBlockRowSize (int bi) {
      if (bi >= 0 && bi < myNumBlockRows) {
         return myRowOffsets[bi+1] - myRowOffsets[bi];
      }
      else {
         return -1;
      }
   }
   
   public int[] getBlockRowSizes() {
      int[] sizes = new int[numBlockRows()];
      for (int bi=0; bi<sizes.length; bi++) {
         sizes[bi] = getBlockRowSize(bi);
      }
      return sizes;
   }

   public int getBlockColOffset (int bj) {
      return myColOffsets[bj];
   }

   public int getBlockColSize (int bj) {
      if (bj >= 0 && bj < myNumBlockCols) {
         return myColOffsets[bj+1] - myColOffsets[bj];
      }
      else {
         return -1;
      }
   }
   
   public int[] getBlockColSizes() {
      int[] sizes = new int[numBlockCols()];
      for (int bj=0; bj<sizes.length; bj++) {
         sizes[bj] = getBlockColSize(bj);
      }
      return sizes;
   }

   public int numBlocks() {
      int num = 0;
      if (isVerticallyLinked() && myNumBlockRows > myNumBlockCols) {
         for (int bj=0; bj<myNumBlockCols; bj++) {
            num += myCols[bj].size(); 
         }
      }
      else {
         for (int bi=0; bi<myNumBlockRows; bi++) {
            num += myRows[bi].size(); 
         }
      }
      return num;
   }

   protected MatrixBlock doAddBlock (int bi, int bj, MatrixBlock blk) {
      if (bi > myNumBlockRows || bj > myNumBlockCols) {
         throw new IllegalArgumentException (
            "Requested block location "+bi+","+bj+
            " is out of bounds; matrix block size is "+
            numBlockRows()+"X"+numBlockCols());
      }
      MatrixBlock oldBlk = null;
      int rowSize = blk.rowSize();
      int colSize = blk.colSize();
      if (bi == myNumBlockRows) {
         addRow (rowSize);
      }
      if (bj == myNumBlockCols) {
         addCol (colSize);
      }
      if (rowSize != (myRowOffsets[bi+1]-myRowOffsets[bi]) ||
          colSize != (myColOffsets[bj+1]-myColOffsets[bj])) {
         throw new IllegalArgumentException (
            "Nonconforming block size at bi="+bi+", bj="+bj+": " + blk.getSize() +
            ", expecting " + getBlockRowSize(bi)+"x"+getBlockColSize(bj));
      }
      blk.setBlockCol (bj);
      blk.setBlockRow (bi);
      oldBlk = myRows[bi].add (blk);
      if (myVerticallyLinkedP) {
         if (myCols[bj].add (blk) != oldBlk) {
            throw new InternalErrorException (
               "BlockRowList.add() and BlockColList.add() return different values");
         }
      }
      invalidateCRSandCCSOffsets();
      return oldBlk;
   }

   public int addBlock (int bi, int bj, MatrixBlock blk) {
      doAddBlock (bi, bj, blk);
      return 0;
   }

   public MatrixBlock addBlock (int bi, int bj, Matrix M) {
      MatrixBlock blk = MatrixBlockBase.alloc (M.rowSize(), M.colSize());
      blk.set (M);
      addBlock (bi, bj, blk);
      return blk;
   }

   protected boolean doRemoveBlock (MatrixBlock oldBlk) {
      MatrixBlock blk = getBlock (oldBlk.getBlockRow(), oldBlk.getBlockCol());
      if (blk == oldBlk) {
         myRows[oldBlk.getBlockRow()].remove (oldBlk);
         if (myVerticallyLinkedP) {
            myCols[oldBlk.getBlockCol()].remove (oldBlk);
         }
         invalidateCRSandCCSOffsets();
         return true;
      }
      else {
         return false;
      }
   }

   public boolean removeBlock (MatrixBlock oldBlk) {
      if (doRemoveBlock (oldBlk)) {
         return true;
      }
      else {
         return false;
      }
   }

   public MatrixBlock removeBlock (int bi, int bj) {
      MatrixBlock blk = getBlock (bi, bj);
      if (blk != null) {
         removeBlock (blk);
      }
      return blk;
   }

   public void removeAllBlocks() {
      for (int bi=0; bi<myNumBlockRows; bi++) {
         myRows[bi].removeAll();
      }
      if (myVerticallyLinkedP) {
         for (int bj=0; bj<myNumBlockCols; bj++) {
            myCols[bj].removeAll();
         }
      }
      invalidateCRSandCCSOffsets();
   }

   public MatrixBlock getBlock (int bi, int bj) {
      if (bi < 0 || bi >= myNumBlockRows || bj < 0 || bj >= myNumBlockCols) {
         return null;
      }
      return myRows[bi].get (bj);
   }

   public MatrixBlock firstBlockInRow (int bi) {
      return myRows[bi].myHead;
   }

   public MatrixBlock firstBlockInCol (int bj) {
      if (myVerticallyLinkedP) {
         return myCols[bj].myHead;
      }
      else {
         MatrixBlock blk;
         for (int bi=0; bi<myNumBlockRows; bi++) {
            if ((blk = myRows[bi].get(bj)) != null) {
               return blk;
            }
         }
         return null;
      }
   }

   public int rowSize() {
      return myNumRows;
   }

   public int rowSize (int numBlkRows) {
      return myRowOffsets [numBlkRows];
   }

   public int numBlockRows() {
      return myNumBlockRows;
   }

   public int numBlockCols() {
      return myNumBlockCols;
   }

   public int colSize (int numBlkCols) {
      return myColOffsets[numBlkCols];
   }

   public int colSize() {
      return myNumCols;
   }

   public boolean elementIsNonZero (int i, int j) {
      return getElementBlock (i, j) != null;
   }

   public boolean valueIsNonZero (int i, int j) {
      if (i < 0 || i >= myNumRows || j < 0 || j >= myNumCols) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      MatrixBlock blk = getElementBlock (i, j);
      if (blk != null) {
         int bi = blk.getBlockRow();
         int bj = blk.getBlockCol();
         return blk.valueIsNonZero (i - myRowOffsets[bi], j - myColOffsets[bj]);
      }
      else {
         return false;
      }
   }

   public MatrixBlock getElementBlock (int i, int j) {
      int bi = getBlockRow (i);
      if (bi == -1) {
         return null;
      }
      for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk = blk.next()) {
         int colOff = myColOffsets[blk.getBlockCol()];
         if (j < colOff) {
            return null;
         }
         else if (j < colOff + blk.colSize()) {
            return blk;
         }
      }
      return null;
   }

   public double get (int i, int j) {
      if (i < 0 || i >= myNumRows || j < 0 || j >= myNumCols) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      MatrixBlock blk = getElementBlock (i, j);
      if (blk != null) {
         int bi = blk.getBlockRow();
         int bj = blk.getBlockCol();
         return blk.get (i - myRowOffsets[bi], j - myColOffsets[bj]);
      }
      else {
         return 0;
      }
   }

   public void set (int i, int j, double val) {
      set (i, j, val, /*setZeros=*/false);
   }

   private void set (int i, int j, double val, boolean setZeros) {
      if (i < 0 || i >= myNumRows || j < 0 || j >= myNumCols) {
         throw new ArrayIndexOutOfBoundsException ("" + i + "," + j);
      }
      MatrixBlock blk = getElementBlock (i, j);
      int bi, bj;
      if (blk == null) {
         if (setZeros ||val != 0) {
            bi = getBlockRow(i);
            bj = getBlockCol(j);
            blk = MatrixBlockBase.alloc (
               getBlockRowSize(bi), getBlockColSize(bj));
            addBlock (bi, bj, blk);
         }
      }
      if (blk != null) {
         bi = blk.getBlockRow();
         bj = blk.getBlockCol();
         blk.set (i - myRowOffsets[bi], j - myColOffsets[bj], val);
      }
   }

   public void set (double[] values, int[] indices, int nvals) {
      if (values.length < nvals) {
         throw new IllegalArgumentException (
            "nvals exceeds length of values array");
      }
      if (indices.length < 2 * nvals) {
         throw new IllegalArgumentException ("insufficient index values");
      }
      setZero();
      for (int k = 0; k < nvals; k++) {
         set (indices[2 * k], indices[2 * k + 1], values[k], /*setZeros=*/true);
      }
   }

   public int numNonZeroVals() {
      return numNonZeroVals (Partition.Full, rowSize(), colSize());
   }

   public int numNonZeroVals (Partition part) {
      return numNonZeroVals (part, rowSize(), colSize());
   }

   private BlockSize getBlockSize (int numRows, int numCols) {
      if (numRows > rowSize() || numCols > colSize()) {
         throw new IllegalArgumentException (
            "submatrix "+getSubMatrixStr(0,numRows-1,0,numCols-1)+
            " exceeds "+getSize()+" matrix size");
      }
      int numBlkRows, numBlkCols;
      numBlkRows = getAlignedBlockRow (numRows);
      numBlkCols = getAlignedBlockCol (numCols);
      if (numBlkRows == -1 || numBlkCols == -1) {
         throw new IllegalArgumentException (
            "submatrix "+getSubMatrixStr(0,numRows-1,0,numCols-1)+
            " is not block aligned");
      }
      return new BlockSize (numBlkRows, numBlkCols);
   }

   public int numNonZeroVals (Partition part, int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      int num = 0;
      switch (part) {
         case None: {
            return 0;
         }
         case Full: {
            for (int bi = 0; bi < bsize.numBlkRows; bi++) {
               for (MatrixBlock blk = myRows[bi].myHead;
                    blk != null && blk.getBlockCol() < bsize.numBlkCols;
                    blk = blk.next()) {
                  num += blk.numNonZeroVals();
               }
            }
            break;
         }
         case UpperTriangular: {
            for (int bi = 0; bi < bsize.numBlkRows; bi++) {
               for (MatrixBlock blk = myRows[bi].myHead;
                    blk != null && blk.getBlockCol() < bsize.numBlkCols;
                    blk = blk.next()) {
                  if (blk.getBlockCol() > bi) {
                     num += blk.numNonZeroVals();
                  }
                  else if (blk.getBlockCol() == bi) {
                     num += blk.numNonZeroVals (
                        Partition.UpperTriangular, blk.rowSize(), blk.colSize());
                  }
               }
            }
            break;
         }
         case LowerTriangular: {
            for (int bi = 0; bi < bsize.numBlkRows; bi++) {
               MatrixBlock blk;
               for (blk=myRows[bi].myHead; blk!=null; blk=blk.next()) { 
                  int bj = blk.getBlockCol();
                  if (bj > bi || bj >= bsize.numBlkCols) {
                     break;
                  }
                  if (bj < bi) {
                     num += blk.numNonZeroVals();
                  }
                  else { // bj == bi
                     num += blk.numNonZeroVals (
                        Partition.LowerTriangular, blk.rowSize(), blk.colSize());
                  }
               }
            }           
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented partition: "
            + part);
         }
      }
      return num;
   }

   public void setZero() {
      for (int bi = 0; bi < myNumBlockRows; bi++) {
         for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk =
            blk.next()) {
            blk.setZero();
         }
      }
   }

   public void scale (double s) {
      for (int bi = 0; bi < myNumBlockRows; bi++) {
         for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk =
            blk.next()) {
            blk.scale (s);
         }
      }
   }

   public void negate() {
      for (int bi = 0; bi < myNumBlockRows; bi++) {
         for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk =
            blk.next()) {
            blk.negate();
         }
      }
   }

   // protected void mulVec (double[] res, double[] vec, int nr, int nc) {
   //    for (int i=0; i<nr; i++) {
   //       res[i] = 0;
   //    }
   //    mulAddVec (res, vec, nr, nc);
   // }

   // @Override
   // protected void mulAddVec (double[] res, double[] vec, int nr, int nc) {
   //    for (int bi=0; bi<myNumBlockRows && myRowOffsets[bi]<nr; bi++) {
   //       int rowOff = myRowOffsets[bi];
   //       for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
   //          int colOff = myColOffsets[blk.getBlockCol()];
   //          if (colOff >= nc) {
   //             break;
   //          }
   //          blk.mulAdd (res, rowOff, vec, colOff);
   //       }
   //    }
   // }

   // protected void mulTransposeVec (double[] res, double[] vec, int nr, int nc) {
   //    // note that here nr and nc refer to the *transposed* matrix
   //    for (int j=0; j<nr; j++) {
   //       res[j] = 0;
   //    }
   //    for (int bi=0; bi<myNumBlockRows && myRowOffsets[bi]<nc; bi++) {
   //       int rowOff = myRowOffsets[bi];
   //       for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk = blk.next()) {
   //          int colOff = myColOffsets[blk.getBlockCol()];
   //          if (colOff >= nr) {
   //             break;
   //          }
   //          blk.mulTransposeAdd (res, colOff, vec, rowOff);
   //       }        
   //    }
   // }

   protected void mulVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {
      for (int i=0; i<nr; i++) {
         res[i] = 0;
      }
      mulAddVec (res, vec, r0, nr, c0, nc);
   }

   protected void mulAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      int rowf = r0 + nr;
      int colf = c0 + nc;
      int bi0 = getBlockRow (r0);
      
      // not a valid row
      if (bi0 < 0) {
         return;
      }

      for (int bi=bi0; bi<myNumBlockRows && myRowOffsets[bi]<rowf; bi++) {
         int rowOff = myRowOffsets[bi];
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
            int colOff = myColOffsets[blk.getBlockCol()];
            if (colOff >= colf) {
               break;
            }
            else if (colOff >= c0) {
               blk.mulAdd (res, rowOff-r0, vec, colOff-c0);
            }
         }
      }
   }

   /**
    * Computes the product
    * <pre>
    * MR = MS M1
    * </pre>
    * where {@code MS} is this matrix, and stores the result in {@code MR},
    * which is sized appropriately. If {@code MR == M1} and the required
    * size of {@code MR} does not equal that of {@code M1}, then an internal
    * copy of {@code M1} is made for computational purposes and {@code M1}
    * is resized as required.
    */
   public void mul (MatrixNd MR, MatrixNd M1) {
      if (colSize() != M1.rowSize()) {
         throw new ImproperSizeException (
            "M1 row size "+M1.rowSize()+" != sparse matrix col size "+colSize());
      }
      if (MR.rowSize() != rowSize() && MR == M1) {
         // make internal copy for computational purposes
         M1 = new MatrixNd (M1);
      }
      if (MR.colSize() != M1.colSize() || MR.rowSize() != rowSize()) {
         MR.setSize (rowSize(), M1.colSize());
      }

      int m = rowSize();
      int n = M1.colSize();
      int r = colSize();
      
      double[] buf1 = M1.getBuffer();
      int w1 = M1.getBufferWidth();
      int base1 = M1.getBufferBase();

      double[] bufr = MR.getBuffer();
      int wr = MR.getBufferWidth();
      int baser = MR.getBufferBase();

      double[] col1 = new double[r];
      double[] colr = new double[m];

      for (int j=0; j<n; j++) {
         // col1 = M1 (:,j)
         int off = j + base1;
         for (int i=0; i<r; i++) {
            col1[i] = buf1[off];
            off += w1;
         }

         // colr = this * col1
         for (int i=0; i<m; i++) {
            colr[i] = 0;
         }
         for (int bi=0; bi<numBlockRows(); bi++) {
            int rowOff = myRowOffsets[bi];
            for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
               int colOff = myColOffsets[blk.getBlockCol()];
               blk.mulAdd (colr, rowOff, col1, colOff);
            }
         }

         // MR (:,j) = colr
         off = j + baser;
         for (int i=0; i<m; i++) {
            bufr[off] = colr[i];
            off += wr;
         }
      }
   }

   /**
    * Computes the product
    * <pre>
    * MR = M1 MS
    * </pre>
    * where {@code MS} is this matrix, and stores the result in {@code MR},
    * which is sized appropriately. If {@code MR == M1} and the required
    * size of {@code MR} does not equal that of {@code M1}, then an internal
    * copy of {@code M1} is made for computational purposes and {@code M1}
    * is resized as required.
    */
   public void mulLeft (MatrixNd MR, MatrixNd M1) {
      if (rowSize() != M1.colSize()) {
         throw new ImproperSizeException (
            "M1 col size "+M1.colSize()+" != sparse matrix row size "+rowSize());
      }
      if (MR.colSize() != colSize() && MR == M1) {
         // make internal copy for computational purposes
         M1 = new MatrixNd (M1);
      }
      if (MR.rowSize() != M1.rowSize() || MR.colSize() != colSize()) {
         MR.setSize (M1.rowSize(), colSize());
      }

      int m = M1.rowSize();
      int n = colSize();
      int r = rowSize();

      double[] buf1 = M1.getBuffer();
      int w1 = M1.getBufferWidth();
      int base1 = M1.getBufferBase();

      double[] bufr = MR.getBuffer();
      int wr = MR.getBufferWidth();
      int baser = MR.getBufferBase();

      double[] row1 = new double[r];
      double[] rowr = new double[n];

      for (int i=0; i<m; i++) {
         // row1 = M1 (i,:)
         int off = i*w1 + base1;
         for (int j=0; j<r; j++) {
            row1[j] = buf1[off++];
         }

         // rowr = row1 * this
         for (int j=0; j<n; j++) {
            rowr[j] = 0;
         }
         for (int bi=0; bi<numBlockRows(); bi++) {
            int rowOff = myRowOffsets[bi];
            for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
               int colOff = myColOffsets[blk.getBlockCol()];
               blk.mulTransposeAdd (rowr, colOff, row1, rowOff);
            }
         }

         // MR (i,:) = rowr
         off = i*wr + baser;
         for (int j=0; j<n; j++) {
            bufr[off++] = rowr[j];
         }
      }
   }

   /**
    * Computes the product
    * <pre>
    * MR = MS M1^T
    * </pre>
    * 
    * here {@code MS} is this matrix, and stores the result in {@code MR},
    * which will be sized appropriately. If {@code MR == M1}, then an internal
    * copy will be made of {@code M1} for computational purposes
    * and {@code M1} will be resized as needed to store the result.
    */
   public void mulTransposeRight (MatrixNd MR, MatrixNd M1) {
      if (colSize() != M1.colSize()) {
         throw new ImproperSizeException (
            "M1 col size "+M1.colSize()+" != sparse matrix col size "+colSize());
      }
      if (MR == M1) {
         // internal copy for computational purposes
         M1 = new MatrixNd (M1);
      }
      if (MR.colSize() != M1.rowSize() || MR.rowSize() != rowSize()) {
         MR.setSize (rowSize(), M1.rowSize());
      }

      int m = rowSize();
      int n = M1.rowSize();
      int r = colSize();

      double[] buf1 = M1.getBuffer();
      int w1 = M1.getBufferWidth();
      int base1 = M1.getBufferBase();

      double[] bufr = MR.getBuffer();
      int wr = MR.getBufferWidth();
      int baser = MR.getBufferBase();

      double[] col1 = new double[r];
      double[] colr = new double[m];

      for (int j=0; j<n; j++) {
         // col1 = M1 (j,:)
         int off = j*w1 + base1;
         for (int i=0; i<r; i++) {
            col1[i] = buf1[off++];
         }

         // colr = this * col1
         for (int i=0; i<m; i++) {
            colr[i] = 0;
         }
         for (int bi=0; bi<numBlockRows(); bi++) {
            int rowOff = myRowOffsets[bi];
            for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
               int colOff = myColOffsets[blk.getBlockCol()];
               blk.mulAdd (colr, rowOff, col1, colOff);
            }
         }

         // MR (:,j) = colr
         off = j + baser;
         for (int i=0; i<m; i++) {
            bufr[off] = colr[i];
            off += wr;
         }
      }
   }

   protected void mulTransposeVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {
      // note that here r0, nr, c0 and nc refer to the *transposed* matrix
      for (int j=0; j<nr; j++) {
         res[j] = 0;
      }
      mulTransposeAddVec (res, vec, r0, nr, c0, nc);
   }

   protected void mulTransposeAddVec (
      double[] res, double[] vec, int r0, int nr, int c0, int nc) {

      int rowf = c0 + nc;
      int colf = r0 + nr;
      int bi0 = getBlockRow (c0);

      for (int bi=bi0; bi<myNumBlockRows && myRowOffsets[bi]<rowf; bi++) {
         int rowOff = myRowOffsets[bi];
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk = blk.next()) {
            int colOff = myColOffsets[blk.getBlockCol()];
            if (colOff >= colf) {
               break;
            }
            else if (colOff >= r0) {
               blk.mulTransposeAdd (res, colOff-r0, vec, rowOff-c0);
            }
         }        
      }
   }

   protected void mulCheckArgs (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      super.mulCheckArgs (vr, v1, r0, nr, c0, nc);
      if (getAlignedBlockRow (r0) == -1 ||
          getAlignedBlockRow (r0+nr) == -1 ||
          getAlignedBlockCol (c0) == -1 ||
          getAlignedBlockCol (c0+nc) == -1) {
         throw new ImproperSizeException (
            "Specified submatrix "+getSubMatrixStr(r0,nr,c0,nc)+
            " is not block aligned");
      }
   }

   protected void mulTransposeCheckArgs (
      VectorNd vr, VectorNd v1, int r0, int nr, int c0, int nc) {
      super.mulTransposeCheckArgs (vr, v1, r0, nr, c0, nc);
      if (getAlignedBlockCol (r0) == -1 ||
          getAlignedBlockCol (r0+nr) == -1 ||
          getAlignedBlockRow (c0) == -1 ||
          getAlignedBlockRow (c0+nc) == -1) {
         throw new ImproperSizeException (
            "Specified submatrix "+getSubMatrixStr(r0,nr,c0,nc)+
            " is not block aligned");
      }
   }

   void updateRowIndices (Partition part, int numBlkRows, int numBlkCols) {
      int numRows = rowSize (numBlkRows);
      myCRSRowOffs = new int[numRows + 1];
      int off = 0;
      for (int bi = 0; bi < numBlkRows; bi++) {
         for (MatrixBlock blk = myRows[bi].myHead;
              blk != null && blk.getBlockCol() < numBlkCols;
              blk = blk.next()) {
            if (part == Partition.UpperTriangular) {
               if (blk.getBlockRow() == blk.getBlockCol()) {
                  blk.addNumNonZerosByRow (myCRSRowOffs, off, part);
               }
               else if (blk.getBlockRow() < blk.getBlockCol()) {
                  blk.addNumNonZerosByRow (myCRSRowOffs, off, Partition.Full);
               }
            }
            else {
               blk.addNumNonZerosByRow (myCRSRowOffs, off, part);
            }
         }
         off += getBlockRowSize(bi);
      }
      int accum = 0;
      for (int i = 0; i < numRows; i++) {
         double num = myCRSRowOffs[i];
         myCRSRowOffs[i] = accum;
         accum += num;
      }
      myCRSRowOffs[numRows] = accum;
      myRowIndicesPartition = part;
      myCRSNumBlkRows = numBlkRows;
      myCRSNumBlkCols = numBlkCols;
   }

   void updateColIndices (Partition part, int numBlkRows, int numBlkCols) {
      int numCols = colSize (numBlkCols);
      myCCSColOffs = new int[numCols + 1];
      // int off = 0;

      for (int bi = 0; bi < numBlkRows; bi++) {
         MatrixBlock blk;
         for (blk = myRows[bi].myHead; blk != null; blk = blk.next()) {
            Partition blkPart = part;
            int bj = blk.getBlockCol();
            if (bj >= numBlkCols) {
               break;
            }
            if (part == Partition.LowerTriangular) {
               if (bj > bi) {
                  break;
               }
               else if (bj < bi) {
                  blkPart = Partition.Full;
               }
            }
            else if (part != Partition.Full) {
               throw new UnsupportedOperationException (
                  "Matrix partition " + part + " not supported");
            }
            blk.addNumNonZerosByCol (myCCSColOffs, myColOffsets[bj], blkPart);
         }
      }
      int accum = 0;
      for (int i = 0; i < numCols; i++) {
         double num = myCCSColOffs[i];
         myCCSColOffs[i] = accum;
         accum += num;
      }
      myCCSColOffs[numCols] = accum;
      myColIndicesPartition = part;
      myCCSNumBlkRows = numBlkRows;
      myCCSNumBlkCols = numBlkCols;
   }

   /**
    * Returns the block row index corresponding to an element row index.
    * 
    * @param i
    * element row index
    * @return corresponding block row index, or -1 if the element index is out
    * of range.
    */
   public int getBlockRow (int i) {
      if (i < 0 || i >= myNumRows) {
         return -1;
      }
      // do a search between biLo <= bi < biHi, guided by the local difference
      // between the number of rows and blocks.
      int biLo = 0;
      int biHi = myNumBlockRows;
      // initial guess; compute as double to prevent overflow
      int bi = (int)(i*(myNumBlockRows/(double)myNumRows));
      while (true) {
         if (i < myRowOffsets[bi]) { // reduce bi
            biHi = bi;
         }
         else if (i >= myRowOffsets[bi+1]) { // increase bi
            biLo = bi+1;
         }
         else { // done
            return bi;
         }
         int nrows = myRowOffsets[biHi] - myRowOffsets[biLo];
         // compute as double to prevent overflow:
         bi = biLo + (int)((i-myRowOffsets[biLo])*((biHi-biLo)/(double)nrows));
      }
   }

   /**
    * Returns the block column index corresponding to an element column index.
    * 
    * @param j
    * element column index
    * @return corresponding block column index, or -1 if the element index is
    * out of range.
    */
   public int getBlockCol (int j) {
      if (j < 0 || j >= myNumCols) {
         return -1;
      }
      // do a search between biLo <= bi < biHi, guided by the local difference
      // between the number of columns and blocks.
      int biLo = 0;
      int biHi = myNumBlockCols;
      // initial guess; compute as double to prevent overflow
      int bi = (int)(j*(myNumBlockCols/(double)myNumCols));
      while (true) {
         if (j < myColOffsets[bi]) { // reduce bi
            biHi = bi;
         }
         else if (j >= myColOffsets[bi+1]) { // increase bi
            biLo = bi+1;
         }
         else { // done
            return bi;
         }
         int ncols = myColOffsets[biHi] - myColOffsets[biLo];
         // compute as double to prevent overflow:
         bi = biLo + (int)((j-myColOffsets[biLo])*((biHi-biLo)/(double)ncols));
      }
   }

   /**
    * If a specified element row index is aligned with the beginning of a block
    * row, then this method returns the corresponding block row index. The
    * element row index may equal the number of matrix rows, in which case the
    * corresponding block row equals the number of block rows in the matrix.
    * 
    * @param i
    * element row index
    * @return corresponding aligned block row index, or -1 if the index is not
    * aligned.
    */
   public int getAlignedBlockRow (int i) {
      if (i == myNumRows) {
         return myNumBlockRows;
      }
      else {
         int bi = getBlockRow (i);
         if (bi == -1 || myRowOffsets[bi] != i) {
            return -1;
         }
         else {
            return bi;
         }
      }
   }

   /**
    * Ian TODO
    */
   public SparseBlockMatrix getSubMatrix (
      int bi, int bj, int numBlkRows, int numBlkCols, SparseBlockMatrix dest) {
      return null;
   }

   /**
    * If a specified element column index is aligned with the beginning of a
    * block column, then this method returns the corresponding block column
    * index. The element column index may equal the number of matrix columns, in
    * which case the corresponding block column equals the number of block
    * columns in the matrix.
    * 
    * @param j
    * element column index
    * @return corresponding aligned block column index, or -1 if the index is
    * not aligned.
    */
   public int getAlignedBlockCol (int j) {
      if (j == myNumCols) {
         return myNumBlockCols;
      }
      else {
         int bj = getBlockCol (j);
         if (bj == -1 || myColOffsets[bj] != j) {
            return -1;
         }
         else {
            return bj;
         }
      }
   }

   /**
    * Gets the compressed row storage (CRS) indices for a principal sub-matrix
    * of this matrix delimited by the first <code>numRows</code> rows and the
    * first <code>numCols</code> columns. The sub-matrix must be
    * block-aligned. Indices are 0-based and the sub-matrix is traversed in
    * row-major order.
    * 
    * @param colIdxs
    * returns the column indices of each non-zero element. This array must have
    * a length equal at least to the number of non-zero elements in the
    * sub-matrix.
    * @param rowOffs
    * returns the offsets into <code>colIdxs</code> corresponding to the first
    * non-zero element in each row. This array must have a length equal at least
    * to the number of rows in the sub-matrix.
    * @param part
    * specifies what portion of the sub-matrix to store; must be either
    * {@link maspack.matrix.Matrix.Partition#Full Full} or
    * {@link maspack.matrix.Matrix.Partition#UpperTriangular UpperTriangular}
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero elements
    */
   public int getCRSIndices (
      int[] colIdxs, int[] rowOffs, Partition part, int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      if (myRowIndicesPartition != part ||
          myCRSNumBlkRows != bsize.numBlkRows ||
          myCRSNumBlkCols != bsize.numBlkCols) {
         updateRowIndices (part, bsize.numBlkRows, bsize.numBlkCols);
      }
      int nnz = doGetBlockCRSIndices (
         colIdxs, 0, myCRSRowOffs, part, bsize.numBlkRows, bsize.numBlkCols);
      for (int i=0; i<nnz; i++) {
         // XXX increment colIdxs since doGetBlockCRSIndices is 0-based
         colIdxs[i]++;
      }
      if (rowOffs != null) {
         for (int i = 0; i < numRows; i++) {
            rowOffs[i] = myCRSRowOffs[i]+1;
         }
         rowOffs[numRows] = nnz+1;
      }
      return nnz;
   }

   /**
    * Gets the compressed row storage (CRS) values for a principal sub-matrix of
    * this matrix delimited by the first <code>numRows</code> rows and the
    * first <code>numCols</code> columns. The sub-matrix must be
    * block-aligned, and it is traversed in row-major order.
    * 
    * @param vals
    * returns the value of each non-zero element. This array must have a length
    * equal at least to the number of non-zero elements in the sub-matrix.
    * @param part
    * specifies what portion of the sub-matrix to store; must be either
    * {@link maspack.matrix.Matrix.Partition#Full Full} or
    * {@link maspack.matrix.Matrix.Partition#UpperTriangular UpperTriangular}
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero elements
    */
   public int getCRSValues (
      double[] vals, Partition part, int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      if (myRowIndicesPartition != part ||
          myCRSNumBlkRows != bsize.numBlkRows ||
          myCRSNumBlkCols != bsize.numBlkCols) {
         updateRowIndices (part, bsize.numBlkRows, bsize.numBlkCols);
      }
      return doGetBlockCRSValues (
         vals, myCRSRowOffs, part, bsize.numBlkRows, bsize.numBlkCols);
   }

   public int getBlockCRSIndices (
      int[] colIdxs, int colOff, int[] offsets, Partition part) {
      return getBlockCRSIndices (
         colIdxs, colOff, offsets, part, myNumRows, myNumCols);
   }

   public int getBlockCRSIndices (
      int[] colIdxs, int colOff, int[] offsets, Partition part, 
      int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      return doGetBlockCRSIndices (
         colIdxs, colOff, offsets, part, bsize.numBlkRows, bsize.numBlkCols);
   }

   private int doGetBlockCRSIndices (
      int[] colIdxs, int colOff, int[] offsets, Partition part, 
      int numBlkRows, int numBlkCols) {

      int off = 0;
      int nnz = 0;
      int[] localOffsets = null;
      for (int bi = 0; bi < numBlkRows; bi++) {
         int nrows = getBlockRowSize(bi);
         if (localOffsets == null || localOffsets.length != nrows) {
            localOffsets = new int[nrows];
         }
         for (int i = 0; i < nrows; i++) {
            localOffsets[i] = offsets[off + i];
         }
         for (MatrixBlock blk = myRows[bi].myHead;
              blk != null && blk.getBlockCol() < numBlkCols;
              blk = blk.next()) {
            int bj = blk.getBlockCol();
            int localColOff = myColOffsets[bj] + colOff;
            if (part == Partition.UpperTriangular) {
               if (bj == bi) {
                  nnz += blk.getBlockCRSIndices (
                     colIdxs, localColOff, localOffsets, part);
               }
               else if (bj > bi) {
                  nnz += blk.getBlockCRSIndices (
                     colIdxs, localColOff, localOffsets, Partition.Full);
               }
            }
            else if (part == Partition.Full) {
               nnz += blk.getBlockCRSIndices (
                  colIdxs, localColOff, localOffsets, Partition.Full);
            }
            else {
               throw new UnsupportedOperationException ("Matrix partition "
               + part + " not supported");
            }
         }
         // Update the offsets *unless* offsets == myRowIndices, which
         // indicates this routine is being called by getCRSIndices()
         // and we don't want to update the offsets
         if (offsets != myCRSRowOffs) {
            for (int i = 0; i < nrows; i++) {
               offsets[off + i] = localOffsets[i];
            }
         }
         off += nrows;
      }
      return nnz;
   }

   public int getBlockCRSValues (double[] vals, int[] offsets, Partition part) {
      return getBlockCRSValues (vals, offsets, part, myNumRows, myNumCols);
   }

   public int getBlockCRSValues (
      double[] vals, int[] offsets, Partition part, int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      return doGetBlockCRSValues (
         vals, offsets, part, bsize.numBlkRows, bsize.numBlkCols);
   }

   private int doGetBlockCRSValues (
      double[] vals, int[] offsets, Partition part, 
      int numBlkRows, int numBlkCols) {
      int off = 0;
      int nnz = 0;
      int[] localOffsets = null;
      for (int bi = 0; bi < numBlkRows; bi++) {
         int nrows = getBlockRowSize(bi);
         if (localOffsets == null || localOffsets.length != nrows) {
            localOffsets = new int[nrows];
         }
         for (int i = 0; i < nrows; i++) {
            localOffsets[i] = offsets[off + i];
         }
         for (MatrixBlock blk = myRows[bi].myHead;
              blk != null && blk.getBlockCol() < numBlkCols;
              blk = blk.next()) {
            int bj = blk.getBlockCol();
            if (part == Partition.UpperTriangular) {
               if (bj == bi) {
                  nnz += blk.getBlockCRSValues (vals, localOffsets, part);
               }
               else if (bj > bi) {
                  nnz += blk.getBlockCRSValues (
                     vals, localOffsets, Partition.Full);
               }
            }
            else if (part == Partition.Full) {
               nnz += blk.getBlockCRSValues (vals, localOffsets, Partition.Full);
            }
            else {
               throw new UnsupportedOperationException ("Matrix partition "
               + part + " not supported");
            }
         }
         // Update the offsets *unless* offsets == myRowIndices, which
         // indicates this routine is being called by getCRSValues()
         // and we don't want to update the offsets
         if (offsets != myCRSRowOffs) {
            for (int i = 0; i < nrows; i++) {
               offsets[off + i] = localOffsets[i];
            }
         }
         off += nrows;
      }
      return nnz;
   }

   public void addNumNonZerosByRow (int[] offsets, int idx, Partition part) {
      addNumNonZerosByRow (offsets, idx, part, myNumRows, myNumCols);
   }

   public void addNumNonZerosByRow (
      int[] offsets, int firstRowIdx, Partition part, 
      int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      if (myRowIndicesPartition != part ||
          myCRSNumBlkRows != bsize.numBlkRows ||
          myCRSNumBlkCols != bsize.numBlkCols) {
         updateRowIndices (part, bsize.numBlkRows, bsize.numBlkCols);
      }
      // myRowIndices has length = numRows+1; last entry gives total
      // number of non-zeros.
      for (int i = 0; i < numRows; i++) {
         offsets[i + firstRowIdx] += (myCRSRowOffs[i + 1] - myCRSRowOffs[i]);
      }
   }

   /**
    * Gets the compressed column storage (CCS) indices for a principal
    * sub-matrix of this matrix delimited by the first <code>numRows</code>
    * rows and the first <code>numCols</code> columns. The sub-matrix must be
    * block-aligned. Indices are 0-based and the sub-matrix is traversed in
    * column-major order.
    * 
    * @param rowIdxs
    * returns the row indices of each non-zero element. This array must have a
    * length equal at least to the number of non-zero elements in the
    * sub-matrix.
    * @param colOffs
    * returns the offsets into <code>rowIdxs</code> corresponding to the first
    * non-zero element in each column. This array must have a length equal at
    * least to the number of columns in the sub-matrix.
    * @param part
    * specifies what portion of the sub-matrix to store; must be either
    * {@link maspack.matrix.Matrix.Partition#Full Full} or
    * {@link maspack.matrix.Matrix.Partition#LowerTriangular LowerTriangular}
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero elements
    */
   public int getCCSIndices (
      int[] rowIdxs, int[] colOffs, Partition part, int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      if (myColIndicesPartition != part ||
          myCCSNumBlkRows != bsize.numBlkRows ||
          myCCSNumBlkCols != bsize.numBlkCols) {
         updateColIndices (part, bsize.numBlkRows, bsize.numBlkCols);
      }
      int nnz = doGetBlockCCSIndices (
         rowIdxs, 0, myCCSColOffs, part, bsize.numBlkRows, bsize.numBlkCols);
      for (int i=0; i<nnz; i++) {
         // XXX increment rowIdxs since doGetBlockCRSIndices is 0-based
         rowIdxs[i]++;
      }      
      if (colOffs != null) {
         for (int i = 0; i < numCols; i++) {
            colOffs[i] = myCCSColOffs[i]+1;
         }
         colOffs[numCols] = nnz+1;
      }
      return nnz;
   }

   /**
    * Gets the compressed column storage (CCS) values for a principal sub-matrix
    * of this matrix delimited by the first <code>numRows</code> rows and the
    * first <code>numCols</code> columns. The sub-matrix must be block aligned
    * and it is traversed in column-major order.
    * 
    * @param vals
    * returns the value of each non-zero element. This array must have a length
    * equal at least to the number of non-zero elements in the sub-matrix.
    * @param part
    * specifies what portion of the sub-matrix to store; must be either
    * {@link maspack.matrix.Matrix.Partition#Full Full} or
    * {@link maspack.matrix.Matrix.Partition#LowerTriangular LowerTriangular}
    * @param numRows
    * number of rows delimiting the sub-matrix
    * @param numCols
    * number of columns delimiting the sub-matrix
    * @return number of non-zero elements
    */
   public int getCCSValues (
      double[] vals, Partition part, int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      if (myColIndicesPartition != part ||
          myCCSNumBlkRows != bsize.numBlkRows ||
          myCCSNumBlkCols != bsize.numBlkCols) {
         updateColIndices (part, bsize.numBlkRows, bsize.numBlkCols);
      }
      return doGetBlockCCSValues (
         vals, myCCSColOffs, part, bsize.numBlkRows, bsize.numBlkCols);
   }

   public int getBlockCCSIndices (
      int[] rowIdxs, int rowOff, int[] offsets, Partition part) {
      return getBlockCCSIndices (
         rowIdxs, rowOff, offsets, part, myNumRows, myNumCols);
   }

   public int getBlockCCSIndices (
      int[] rowIdxs, int rowOff, int[] offsets, Partition part, int numRows,
      int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      return doGetBlockCCSIndices (
         rowIdxs, rowOff, offsets, part, bsize.numBlkRows, bsize.numBlkCols);
   }

   private int doGetBlockCCSIndices (
      int[] rowIdxs, int rowOff, int[] offsets, Partition part, int numBlkRows,
      int numBlkCols) {
      int off = 0;
      int nnz = 0;
      int[] localOffsets = null;

      int[][] offsetsArray = new int[numBlkCols][];
      for (int bj=0; bj<numBlkCols; bj++) {
         int ncols = getBlockColSize(bj);
         localOffsets = new int[ncols];
         for (int i=0; i<ncols; i++) {
            localOffsets[i] = offsets[off+i];
         }
         offsetsArray[bj] = localOffsets;
         off += ncols;
      }
      for (int bi=0; bi<numBlkRows; bi++) {
         int localRowOff = myRowOffsets[bi] + rowOff;
         MatrixBlock blk;
         for (blk=myRows[bi].myHead; blk!=null; blk=blk.next()) { 
            int bj = blk.getBlockCol();
            if (bj >= numBlkCols) {
               break;
            }
            Partition blkPart = part;
            if (part == Partition.LowerTriangular) {
               if (bj > bi) {
                  break;
               }
               else if (bj < bi) {
                  blkPart = Partition.Full;
               }
            }
            else if (part != Partition.Full) {
               throw new UnsupportedOperationException (
                  "Matrix partition " + part + " not supported");
            }
            nnz += blk.getBlockCCSIndices (
               rowIdxs, localRowOff, offsetsArray[bj], blkPart);
         }
      }
      // Update the offsets *unless* offsets == myColIndices, which
      // indicates this routine is being called by getCCSValues()
     // and we don't want to update the offsets
     if (offsets != myCCSColOffs) {
        off = 0;
        for (int bj=0; bj<numBlkCols; bj++) {
           int ncols = getBlockColSize(bj);
           localOffsets = offsetsArray[bj];
           for (int i=0; i<ncols; i++) {
               offsets[off+i] = localOffsets[i];
            }
           off += ncols;
        }
     }

     return nnz;
   }

   public void getBlockCCSValues (double[] vals, int[] offsets, Partition part) {
      getBlockCCSValues (vals, offsets, part, myNumRows, myNumCols);
   }

   public int getBlockCCSValues (
      double[] vals, int[] offsets, Partition part, int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);
      return doGetBlockCCSValues (
         vals, offsets, part, bsize.numBlkRows, bsize.numBlkCols);
   }

   private int doGetBlockCCSValues (
      double[] vals, int[] offsets, Partition part, int numBlkRows,
      int numBlkCols) {
      int off = 0;
      int nnz = 0;
      int[] localOffsets = null;

      int[][] offsetsArray = new int[numBlkCols][];
      for (int bj=0; bj<numBlkCols; bj++) {
         int ncols = getBlockColSize(bj);
         localOffsets = new int[ncols];
         for (int i=0; i<ncols; i++) {
            localOffsets[i] = offsets[off+i];
         }
         offsetsArray[bj] = localOffsets;
         off += ncols;
      }
      for (int bi=0; bi<numBlkRows; bi++) {
         MatrixBlock blk;
         for (blk=myRows[bi].myHead; blk!=null; blk=blk.next()) { 
            int bj = blk.getBlockCol();
            if (bj >= numBlkCols) {
               break;
            }
            Partition blkPart = part;
            if (part == Partition.LowerTriangular) {
               if (bj > bi) {
                  break;
               }
               else if (bj < bi) {
                  blkPart = Partition.Full;
               }
            }
            else if (part != Partition.Full) {
               throw new UnsupportedOperationException (
                  "Matrix partition " + part + " not supported");
            }
            nnz += blk.getBlockCCSValues (vals, offsetsArray[bj], blkPart);
         }
      }
      // Update the offsets *unless* offsets == myColIndices, which
      // indicates this routine is being called by getCCSValues()
     // and we don't want to update the offsets
     if (offsets != myCCSColOffs) {
        off = 0;
        for (int bj=0; bj<numBlkCols; bj++) {
           int ncols = getBlockColSize(bj);
           localOffsets = offsetsArray[bj];
           for (int i=0; i<ncols; i++) {
               offsets[off+i] = localOffsets[i];
            }
           off += ncols;
        }
     }
      return nnz;
   }

   public void addNumNonZerosByCol (
      int[] offsets, int idx, Partition part, int numRows, int numCols) {
      BlockSize bsize = getBlockSize (numRows, numCols);

      if (myColIndicesPartition != part ||
          myCCSNumBlkRows != bsize.numBlkRows ||
          myCCSNumBlkCols != bsize.numBlkCols) {
         updateColIndices (part, bsize.numBlkRows, bsize.numBlkCols);
      }
      // myColIndices has length = numCols+1; last entry gives total
      // number of non-zeros.
      for (int j = 0; j < numCols; j++) {
         offsets[j + idx] += (myCCSColOffs[j + 1] - myCCSColOffs[j]);
      }
   }

   public void add (SparseBlockMatrix M) {
      scaledAdd (1, M, M.numBlockRows(), M.numBlockCols());
   }

   public void add (SparseBlockMatrix M, int numBlkRows, int numBlkCols) {
      scaledAdd (1, M, numBlkRows, numBlkCols);
   }

   public void sub (SparseBlockMatrix M) {
      scaledAdd (-1, M, M.numBlockRows(), M.numBlockCols());
   }

   public void sub (SparseBlockMatrix M, int numBlkRows, int numBlkCols) {
      scaledAdd (-1, M, numBlkRows, numBlkCols);
   }

   public void scaledAdd (double s, SparseBlockMatrix M) {
      scaledAdd (s, M, M.numBlockRows(), M.numBlockCols());
   }
   
   public void scaledAdd (
      double s, SparseBlockMatrix M, int numBlkRows, int numBlkCols) {

      if (numBlkRows > numBlockRows() || numBlkCols > numBlockCols()) {
         throw new IllegalArgumentException (
            "Number of block rows and/or columns in M submatrix exceeds "+
            "number in result matrix");
      }
      if (numBlkRows > M.numBlockRows() || numBlkCols > M.numBlockCols()) {
         throw new IllegalArgumentException (
            "Number of block rows and/or columns in M submatrix exceeds "+
            "number in M itself");
      }
      for (int bi=0; bi<numBlkRows; bi++) {
         if (getBlockRowSize(bi) != M.getBlockRowSize(bi)) {
            throw new IllegalArgumentException (
               "Block row sizes do not conform");
         }
      }
      for (int bj=0; bj<numBlkCols; bj++) {
         if (getBlockColSize(bj) != M.getBlockColSize(bj)) {
            throw new IllegalArgumentException (
               "Block col sizes do not conform");
         }
      }
      for (int bi=0; bi<numBlkRows; bi++) {
         MatrixBlock blkM = M.myRows[bi].myHead;
         MatrixBlock blkR = myRows[bi].myHead;
         while (blkM != null && blkM.getBlockCol() < numBlkCols) {
            int bj = blkM.getBlockCol();
            while (blkR != null && blkR.getBlockCol() < bj) {
               blkR = blkR.next();
            }
            if (blkR == null || blkR.getBlockCol() > bj) {
               // need to create and add a block
               blkR = blkM.clone();
               if (s != 1) {
                  blkR.scale (s);
               }
               addBlock (bi, bj, blkR);
            }
            else {
               if (s == 1) {
                  blkR.add (blkM);
               }
               else if (s == -1) {
                  blkR.sub (blkM);
               }
               else {
                  blkR.scaledAdd (s, blkM);
               }
            }
            blkM = blkM.next();
         }
      }
   }
   
//   /**
//    * Sets the current matrix S to (1/2)(S+S^T), making it symmetric.  For this
//    * to succeed, the current matrix must have consistent block row/column sizes.
//    * 
//    * @return true if matrix structure has changed
//    */
//   public boolean setSymmetric() {
//      
//      // check sizes match column sizes
//      if (numBlockRows() != numBlockCols()) {
//         throw new IllegalArgumentException (
//            "Number of block rows must match number of block columns in M");
//      }
//      for (int i=0; i<numBlockRows(); ++i) {
//         if (getBlockRowSize(i) != getBlockColSize(i)) {
//            throw new IllegalArgumentException("M does not have consistent block"
//               + " row and column sizes");
//         }
//      }
//      
//      // reset partition info
//      myRowIndicesPartition = Partition.None;
//      myRowIndicesNumBlkRows = -1;
//      myRowIndicesNumBlkCols = -1;
//      myRowIndices = null;
//
//      myColIndicesPartition = Partition.None;
//      myColIndicesNumBlkRows = -1;
//      myColIndicesNumBlkCols = -1;
//      myColIndices = null;
//      
//      // go through and modify/add blocks
//      boolean structureModified = false;
//      for (int bi=0; bi<myNumBlockRows; bi++) {
//         MatrixBlock blk = myRows[bi].firstBlock();
//         
//         while (blk != null) {
//            
//            // get corresponding column
//            int bj = blk.getBlockCol();
//            
//            if (bi != bj) {
//               MatrixBlock blkS = getBlock(bj, bi);
//               
//               // both exist, modify blocks to be symmetric
//               if (blkS != null) {
//                  
//                  // make symmetric
//                  for (int i=0; i<blk.rowSize(); ++i) {
//                     for (int j=0; j<blk.colSize(); ++j) {
//                        double val = (blk.get(i, j) + blkS.get(j, i))/2;
//                        blk.set(i,j, val);
//                        blkS.set(j, i, val);
//                     }
//                  }
//                  
//               } else {
//                  
//                  // only one exists, add new symmetric block
//                  blk.scale(0.5);
//                  MatrixBlock newBlk = blk.createTranspose();
//                  addBlock(bj, bi, newBlk);
//                  structureModified = true;
//                  
//               }
//               
//            } else if (bi == bj) {
//               
//               // diagonal
//               for (int i=0; i<blk.rowSize()-1; ++i) {
//                  for (int j=i+1; j<blk.colSize(); ++j) {
//                     double val = (blk.get(i, j)+blk.get(j, i))/2;
//                     blk.set(i, j, val ); 
//                     blk.set(j, i, val);
//                  }
//               }
//            }
//            
//            blk = blk.next();
//         }
//      }
//    
//      return structureModified;
//   }
   
//   /**
//    * Sets the current matrix to (1/2)(M+M^T). For this to succeed, M
//    * must have consistent block row/column sizes.
//    *  
//    * @param M input matrix
//    */
//   public void setSymmetric (SparseBlockMatrix M) {
//      
//      if (M == this) {
//         setSymmetric();
//         return;
//      }
//      
//      // check sizes match column sizes
//      if (M.numBlockRows() != M.numBlockCols()) {
//         throw new IllegalArgumentException (
//            "Number of block rows must match number of block columns in M");
//      }
//      for (int i=0; i<M.numBlockRows(); ++i) {
//         if (M.getBlockRowSize(i) != M.getBlockColSize(i)) {
//            throw new IllegalArgumentException("M does not have consistent block"
//               + " row and column sizes");
//         }
//      }
//      
//      // create structure
//      myNumBlockRows = M.myNumBlockRows;
//      myNumBlockCols = M.myNumBlockCols;
//      myNumRows = M.myNumRows;
//      myNumCols = M.myNumCols;
//
//      myVerticallyLinkedP = M.myVerticallyLinkedP;
//
//      myRows = new MatrixBlockRowList[myNumBlockRows];
//      myRowOffsets = new int[myNumBlockRows+1];
//      for (int bi = 0; bi < myNumBlockRows; bi++) {
//         myRows[bi] = new MatrixBlockRowList();
//         myRowOffsets[bi] = M.myRowOffsets[bi];
//      }
//      myRowOffsets[myNumBlockRows] = M.myRowOffsets[myNumBlockRows];
//
//      myCols = new MatrixBlockColList[myNumBlockCols];
//      myColOffsets = new int[myNumBlockCols+1];
//      for (int bj = 0; bj < myNumBlockCols; bj++) {
//         if (myVerticallyLinkedP) {
//            myCols[bj] = new MatrixBlockColList();
//         }
//         myColOffsets[bj] = M.myColOffsets[bj];
//      }
//      myColOffsets[myNumBlockCols] = M.myColOffsets[myNumBlockCols];
//
//      myRowIndicesPartition = Partition.None;
//      myRowIndicesNumBlkRows = -1;
//      myRowIndicesNumBlkCols = -1;
//      myRowIndices = null;
//
//      myColIndicesPartition = Partition.None;
//      myColIndicesNumBlkRows = -1;
//      myColIndicesNumBlkCols = -1;
//      myColIndices = null;
//
//      // go through and fill in blocks where both M(bi, bj) and M(bj, bi) exist
//      for (int bi=0; bi<myNumBlockRows; bi++) {
//         MatrixBlock blk = M.myRows[bi].firstBlock();
//         
//         while (blk != null) {
//            
//            // get corresponding column
//            int bj = blk.getBlockCol();
//            
//            if (bi != bj) {
//               MatrixBlock blkS = M.getBlock(bj, bi);
//               
//               // both exist, modify blocks to be symmetric
//               if (blkS != null) {
//                  MatrixBlock newR = blk.clone();
//                  
//                  // make symmetric
//                  for (int i=0; i<blk.rowSize(); ++i) {
//                     for (int j=0; j<blk.colSize(); ++j) {
//                        double val = (blk.get(i, j) + blkS.get(j, i))/2;
//                        newR.set(i,j, val);
//                     }
//                  }
//                  
//                  addBlockWithoutNumber (bi, bj, newR);
//                  newR.setBlockNumber (blk.getBlockNumber());
//                  
//                  MatrixBlock newC = newR.createTranspose();
//                  addBlockWithoutNumber (bj, bi, newC);
//                  newC.setBlockNumber (blkS.getBlockNumber());
//               } else {
//                  
//                  // only one exists, add block to keep numbers valid
//                  MatrixBlock newR = blk.clone();
//                  newR.scale(0.5);
//                  
//                  addBlockWithoutNumber (bi, bj, newR);
//                  newR.setBlockNumber (blk.getBlockNumber());
//                  
//               }
//               
//            } else if (bi == bj) {
//               MatrixBlock newBlk = blk.clone();
//               for (int i=0; i<newBlk.rowSize()-1; ++i) {
//                  for (int j=i+1; j<newBlk.colSize(); ++j) {
//                     double val = (newBlk.get(i, j)+newBlk.get(j, i))/2;
//                     newBlk.set(i, j, val ); 
//                     newBlk.set(j, i, val);
//                  }
//               }
//               addBlockWithoutNumber (bi, blk.getBlockCol(), newBlk);
//               newBlk.setBlockNumber (blk.getBlockNumber());
//            }
//            
//            blk = blk.next();
//         }
//      }
//      
//      // add new blocks when only one of M(bi, bj) or M(bj, bi) exists
//      for (int bi=0; bi<myNumBlockRows; bi++) {
//         MatrixBlock blk = M.myRows[bi].firstBlock();
//         
//         while (blk != null) {
//            
//            // get corresponding column
//            int bj = blk.getBlockCol();
//            
//            if (bi != bj) {
//               MatrixBlock blkS = M.getBlock(bj, bi);
//               
//               // only one exists add the mirroring block
//               if (blkS == null) {
//                  // only one exists, add block to keep numbers valid
//                  MatrixBlock newC = blk.createTranspose();
//                  newC.scale(0.5);
//                  addBlock (bj, bi, newC);
//               }
//               
//            } 
//            
//            blk = blk.next();
//         }
//      }
//   }

   public String toString (String fmtStr, int nrows, int ncols) {
      int numValues = numNonZeroVals();
      double[] values = new double[numValues];
      int[] colIdxs = new int[numValues];
      int[] rowOffs = new int[rowSize()+1];

      getCRSIndices (colIdxs, rowOffs, Partition.Full);
      getCRSValues (values, Partition.Full);

      StringBuffer buf = new StringBuffer (20 * numValues);
      NumberFormat fmt = new NumberFormat (fmtStr);
      int rowIdx = 0;
      for (int i=0; i<numValues; i++){
         while ((rowIdx+1) < rowSize() && i >= rowOffs[rowIdx+1]-1){
            rowIdx++;
         }
         buf.append ("(" + rowIdx + " " + (colIdxs[i]-1) + " " +
                     fmt.format(values[i]) + ")\n");
      }
      return buf.toString();
   }

   public String getBlockPattern () {
      return getBlockPattern (0, numBlockRows());
   }         

   public String getBlockPattern (int minRow, int maxRow) {
      
      int nrows = numBlockRows();
      int ncols = numBlockCols();
      StringBuilder buf = new StringBuilder ((nrows + 1) * ncols + 1);
      maxRow = Math.min (maxRow, nrows);
      for (int i = minRow; i < maxRow; i++) {
         for (int j = 0; j < ncols; j++) {
            if (j > 0) {
               buf.append (' ');
            }
            buf.append (getBlock (i, j) == null ? '.' : 'X');
         }
         buf.append ('\n');
      }
      return buf.toString();
   }

   public void printBlocks (PrintStream ps) {
      for (int bi = 0; bi < myNumBlockRows; bi++) {
         for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk =
            blk.next()) {
            ps.print (blk.rowSize() + "x" + blk.colSize() + "@ "
            + blk.getBlockRow() + "," + blk.getBlockCol() + " ");
         }
         ps.println ("");
      }
   }

   /**
    * Sets the nonzero elements of this matrix to random values
    * in the range -0.5 (inclusive) to 0.5 (exclusive).
    */
   public void setRandomValues () {
      setRandomValues (-0.5, 0.5, RandomGenerator.get(), /*symmetric=*/false);
   }

   /**
    * Sets the nonzero elements of this matrix to random values
    * in the range -0.5 (inclusive) to 0.5 (exclusive).
    *
    * @param symmetric if true, sets transposed values to the same value
    * (ignored if the matrix is not square or does not have identical
    * row and block sizes).
    */
   public void setRandomValues (boolean symmetric) {
      setRandomValues (-0.5, 0.5, RandomGenerator.get(), symmetric);
   }

   /**
    * Sets the nonzero elements of this matrix to random values
    * in a specified range.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param generator
    * random number generator
    * @param symmetric if true, sets transposed values to the same value
    * (ignored if the matrix is not square or does not have identical
    * row and block sizes).
    */
   public void setRandomValues (
      double lower, double upper, Random generator, boolean symmetric) {

      if (myNumRows != myNumCols) {
         symmetric = false;
      }
      if (symmetric) {
         for (int bi=1; bi<=myNumBlockRows; bi++) {
            if (myRowOffsets[bi] != myColOffsets[bi]) {
               symmetric = false;
               break;
            }
         }
      }

      for (int bi = 0; bi < myNumBlockRows; bi++) {
         for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk =
            blk.next()) {
            if (blk instanceof DenseMatrixBase) {
               ((DenseMatrixBase)blk).setRandom (lower, upper, generator);
            }
            if (symmetric) {
               int bj = blk.getBlockCol();
               if (bj != bi) {
                  MatrixBlock blkT = myRows[bj].get(bi);
                  if (blkT != null) {
                     setTranspose (blkT, blk);
                  }
               }
               else {
                  setSymmetric (blk);
               }
            }
         }
      }
   }

   private void setSymmetric (DenseMatrix M) {
      if (M.rowSize() != M.colSize()) {
         return;
      }
      for (int i=0; i<M.rowSize(); i++) {
         for (int j=i+1; j<M.colSize(); j++) {
            M.set (j, i, M.get (i, j));
         }
      }
   }

   private void setTranspose (DenseMatrix MT, DenseMatrix M) {
      if (MT.rowSize() != M.colSize() || MT.colSize() != M.rowSize()) {
         return;
      }
      for (int i=0; i<MT.rowSize(); i++) {
         for (int j=0; j<MT.colSize(); j++) {
            MT.set (i, j, M.get (j, i));
         }
      }
   }

   public void set (Matrix M) {
      if (M instanceof SparseBlockMatrix) {
         set ((SparseBlockMatrix)M);
      }
      else {
         super.set (M);
      }
   }

   /**
    * Sets this SparseBlockMatrix to a copy of another SparseBlockMatrix.
    *
    * @param M matrix to be copied
    */
   public void set (SparseBlockMatrix M) {

      myNumBlockRows = M.myNumBlockRows;
      myNumBlockCols = M.myNumBlockCols;
      myNumRows = M.myNumRows;
      myNumCols = M.myNumCols;

      myVerticallyLinkedP = M.myVerticallyLinkedP;

      myRows = new MatrixBlockRowList[myNumBlockRows];
      myRowOffsets = new int[myNumBlockRows+1];
      for (int bi = 0; bi < myNumBlockRows; bi++) {
         myRows[bi] = new MatrixBlockRowList();
         myRowOffsets[bi] = M.myRowOffsets[bi];
      }
      myRowOffsets[myNumBlockRows] = M.myRowOffsets[myNumBlockRows];

      myCols = new MatrixBlockColList[myNumBlockCols];
      myColOffsets = new int[myNumBlockCols+1];
      for (int bj = 0; bj < myNumBlockCols; bj++) {
         if (myVerticallyLinkedP) {
            myCols[bj] = new MatrixBlockColList();
         }
         myColOffsets[bj] = M.myColOffsets[bj];
      }
      myColOffsets[myNumBlockCols] = M.myColOffsets[myNumBlockCols];

      invalidateCRSandCCSOffsets();

      for (int bi=0; bi<myNumBlockRows; bi++) {
         MatrixBlock blk = M.myRows[bi].firstBlock();
         while (blk != null) {
            MatrixBlock newBlk = blk.clone();
            newBlk.setBlockNumber (blk.getBlockNumber());
            addBlock (bi, blk.getBlockCol(), newBlk);
            blk = blk.next();
         }
      }
   }

   /**
    * Creates a clone of this SparseBlockMatrix, along with clones of all the
    * associated MatrixBlocks.
    */
   public SparseBlockMatrix clone() {
      SparseBlockMatrix M;
      try {
         M = (SparseBlockMatrix)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException (
            "clone not supported for super class of SparseBlockMatrix");
      }
      M.set (this);
      return M;
   }

   public void getRow (int i, double[] values) {
      getRow (i, values, 0);
   }

   public void getRow (int i, double[] values, int off) {
      if (off + values.length < myNumCols) {
         throw new ImproperSizeException ("'values' is insufficiently large");
      }
      if (i < 0 || i >= myNumRows) {
         throw new ArrayIndexOutOfBoundsException ("requested row=" + i
         + ", number of rows=" + myNumRows);
      }
      // zero all values to start; this is easier ...
      for (int j = 0; j < myNumCols; j++) {
         values[off+j] = 0;
      }
      int bi = getBlockRow (i);
      int ilocal = i - myRowOffsets[bi];
      for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk = blk.next()) {
         int offlocal = myColOffsets[blk.getBlockCol()] + off;
         blk.getRow (ilocal, values, offlocal);
      }
   }
   
   public void zeroRow(int i) {

      if (i < 0 || i >= myNumRows) {
         throw new ArrayIndexOutOfBoundsException ("requested row=" + i
            + ", number of rows=" + myNumRows);
      }
      
      int bi = getBlockRow (i);
      int ilocal = i - myRowOffsets[bi];
      for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk = blk.next()) {
         for (int j=0; j<blk.rowSize(); j++) {
            blk.set(ilocal, j, 0);
         }
      }
   }

   public void getColumn (int j, double[] values) {
      getColumn (j, values, 0, rowSize());
   }

   public void getColumn (int j, double[] values, int off, int nrows) {
      if (off + values.length < nrows) {
         throw new ImproperSizeException ("'values' is insufficiently large");
      }
      int numBlkRows = 0;
      if (nrows > 0) {
         numBlkRows = getAlignedBlockRow (nrows);
         if (numBlkRows == -1) {
            throw new IllegalArgumentException (
               "specified number of rows "+nrows+" is not block aligned");
         }
      }
      int bj = getBlockCol (j);
      if (bj == -1) {
         throw new ArrayIndexOutOfBoundsException (
            "requested col=" + j + ", number of cols=" + myNumCols);
      }
      int jlocal = j - myColOffsets[bj];
      // zero all values to start; this is easier ...
      for (int i = 0; i < nrows; i++) {
         values[off+i] = 0;
      }
      for (int bi=0; bi<numBlkRows; bi++) {
         MatrixBlock blk = myRows[bi].get (bj);
         if (blk != null) {
            blk.getColumn (jlocal, values, myRowOffsets[bi]+off);
            //values[myRowOffsets[bi]+off] = 0;
         }
      }
   }
   
   public void zeroColumn(int j) {
      int bj = getBlockCol (j);
      if (bj == -1) {
         throw new ArrayIndexOutOfBoundsException (
            "requested col=" + j + ", number of cols=" + myNumCols);
      }
      int jlocal = j - myColOffsets[bj];
      for (int bi=0; bi<myNumBlockRows; bi++) {
         MatrixBlock blk = myRows[bi].get (bj);
         if (blk != null) {
            for (int k =0; k<blk.colSize(); k++) {
               blk.set(k, jlocal, 0);
            }
         }
      }
   }

   /** 
    * {@inheritDoc}
    */
   public void checkConsistency () {
      int nrows = myNumRows;
      int ncols = myNumCols;
      for (int i=0; i<nrows; i++) {
         for (int j=0; j<ncols; j++) {
            MatrixBlock blk = getElementBlock (i, j);
            if (blk != null) {
               int bi = blk.getBlockRow();
               int bj = blk.getBlockCol();
               if (get (i, j) !=
                   blk.get (i-myRowOffsets[bi], j-myColOffsets[bj])) {
                  throw new TestException (
                     "Bad row or col offsets at " + i + "," + j);
               }
            }
            else {
               if (get (i, j) != 0) {
                  throw new TestException ("Zero expected at " + i + "," + j);
               }
            }
         }
      }
      for (int bi=0; bi<myNumBlockRows; bi++) {
         int cnt = 0;
         int rsize = myRowOffsets[bi+1]-myRowOffsets[bi];
         int lastBj = -1;
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
            if (cnt >= myNumBlockCols) {
               throw new TestException (
                  "block row " + bi +
                  ": block traverse exceeds number of block cols");
            }
            if (blk.getBlockRow() != bi) {
                throw new TestException (
                   "block row " + bi +
                   ": blk.getBlockRow() returns "+blk.getBlockRow());
            }
            int bj = blk.getBlockCol();
            if (lastBj != -1 && bj <= lastBj) {
               throw new TestException (
                  "block row " + bi +
                  ": blocks at block cols "+lastBj+" and "+bj+" not in order");
            }
            if (bj >= myNumBlockCols) {
               throw new TestException (
                  "block row " + bi +
                  ": block at block col "+bj+
                  " exceeds numBlockCols "+ myNumBlockCols);
            }
            int csize = getBlockColSize(bj);
            if (blk.colSize() != csize) {
               throw new TestException (
                  "Block("+bi+","+bj+") has colSize "+blk.colSize()+" vs "+csize);
            }
            if (blk.rowSize() != rsize) {
               throw new TestException (
                  "Block("+bi+","+bj+") has rowSize "+blk.rowSize()+" vs "+rsize);
            }
            if (myVerticallyLinkedP) {
               MatrixBlock cblk = myCols[bj].get(bi);
               if (cblk != blk) {
                  throw new TestException (
                     "Block("+bi+","+bj+") not found column-wise");
               }
            }
            lastBj = bj;
            cnt++;               
         }
      }
      if (myVerticallyLinkedP) {
         for (int bj=0; bj<myNumBlockCols; bj++) {
            int cnt = 0;
            int csize = myColOffsets[bj+1]-myColOffsets[bj];
            int lastBi = -1;
            for (MatrixBlock blk=myCols[bj].myHead; blk!=null; blk=blk.down()) {
               if (cnt >= myNumBlockRows) {
                  throw new TestException (
                     "block col " + bj +
                     ": block traverse exceeds number of block rows");
               }
               if (blk.getBlockCol() != bj) {
                  throw new TestException (
                     "block col " + bj +
                     ": blk.getBlockCol() returns "+blk.getBlockCol());
               }
               int bi = blk.getBlockRow();
               if (lastBi != -1 && bi <= lastBi) {
                  throw new TestException (
                     "block col " + bj +
                     ": blocks at block rows "+lastBi+" and "+bi+" not in order");
               }
               if (bi >= myNumBlockRows) {
                  throw new TestException (
                     "block col " + bj +
                     ": block at block row "+bi+
                     " exceeds numBlockRows "+ myNumBlockRows);
               }
               int rsize = getBlockRowSize(bi);
               if (blk.colSize() != csize) {
                  throw new TestException (
                     "Block("+bi+","+bj+") has colSize "+blk.colSize()+
                     " vs "+csize);
               }
               if (blk.rowSize() != rsize) {
                  throw new TestException (
                     "Block("+bi+","+bj+") has rowSize "+blk.rowSize()+
                     " vs "+rsize);
               }
               lastBi = bi;
               cnt++;               
            }
         }
      }
      
      
      if (myRowOffsets[myNumBlockRows] != myNumRows) {
         throw new TestException (
            "Last row offset != number of rows "+myNumRows);
      }
      if (myColOffsets[myNumBlockCols] != myNumCols) {
         throw new TestException (
            "Last col offset != number of cols "+myNumCols);
      }
   }

   public void perturb (double tol) {
      Random randGen = RandomGenerator.get();
      for (int bi = 0; bi < myNumBlockRows; bi++) {
         for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk =
                 blk.next()) {
            for (int i=0; i<blk.rowSize(); i++) {
               for (int j=0; j<blk.colSize(); j++) {
                  double eps = tol*(randGen.nextDouble()-0.5);
                  blk.set (i, j, blk.get (i, j) + eps);
               }
            }
         }
      }
   }

   public void setVerticallyLinked (boolean enable) {
      if (myVerticallyLinkedP != enable) {
         if (enable) {
            createVerticalLinks();
         }
         else {
            clearVerticalLinks();
         }
         myVerticallyLinkedP = enable;
      }
   }

   public boolean isVerticallyLinked() {
      return myVerticallyLinkedP;
   }

   private void clearVerticalLinks() {
      for (int bj=0; bj<myCols.length; bj++) {
         if (myCols[bj] != null) {
            myCols[bj].removeAll(); // presently just sets head =tail = null
            myCols[bj] = null;
         }
      }
   }

   private void createVerticalLinks() {
      // make sure all entries in myCols are non-null, and
      // clear any which are already present
      for (int bj=0; bj<myNumBlockCols; bj++) {
         if (myCols[bj] != null) {
            myCols[bj].removeAll();
         }
         else {
            myCols[bj] = new MatrixBlockColList();
         }
      }
      for (int bi=0; bi<myNumBlockRows; bi++) {
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
            int bj = blk.getBlockCol();
            myCols[bj].add (blk);
         }
      }
   }

   public static SparseBlockMatrix buildKKTSystem (
      SparseBlockMatrix M, SparseBlockMatrix GT) {

      SparseBlockMatrix A = M.clone();
      for (int bj=0; bj<GT.myNumBlockCols; bj++) {
         A.addCol (GT.getBlockColSize(bj));
         A.addRow (GT.getBlockColSize(bj));
      }
      for (int bi=0; bi<GT.myNumBlockRows; bi++) {
         for (MatrixBlock blk=GT.myRows[bi].myHead; blk!=null; blk=blk.next()) {
            MatrixBlock blkNew = blk.clone();
            int bj = M.numBlockCols() + blk.getBlockCol();
            A.addBlock (bi, bj, blkNew);

            // allocate a transpose of blk:
            MatrixBlock blkTsp =
               MatrixBlockBase.alloc (blk.colSize(), blk.rowSize());
            for (int i=0; i<blk.rowSize(); i++) {
               for (int j=0; j<blk.colSize(); j++) {
                  blkTsp.set (j, i, blk.get (i, j));
               }
            }
            A.addBlock (bj, bi, blkTsp);
         }
      }
      return A;
   }
   
   public SparseBlockMatrix createSubMatrix(int numBlockRows, int numBlockCols) {
      if (numBlockRows > myNumBlockRows || numBlockCols > myNumBlockCols) {
         throw new IllegalArgumentException (
            "requested block size "+numBlockRows+"x"+numBlockCols+
            " exceeds matrix dimensions");
      }
      int[] irows = new int[numBlockRows];
      for (int i=0; i<numBlockRows; i++) {
         irows[i] = i;
      }
      int[] icols = new int[numBlockCols];
      for (int i=0; i<numBlockCols; i++) {
         icols[i] = i;
      }
      return createSubMatrix (irows, icols);
   }

   public SparseBlockMatrix createSubMatrix(int[] irows, int[] icols) {
      if (irows.length < 1 || icols.length < 1)
	 return null;
      
      SparseBlockMatrix M = new SparseBlockMatrix();
      M.myVerticallyLinkedP = false;

      M.myNumRows = 0;
      M.myNumBlockRows = irows.length;
      M.myRows = new MatrixBlockRowList[M.myNumBlockRows];
      M.myRowOffsets = new int[M.myNumBlockRows + 1];
      for (int bi = 0; bi < irows.length; bi++) {
	 M.myRows[bi] = new MatrixBlockRowList();
	 M.myRowOffsets[bi] = M.myNumRows;
	 M.myNumRows += myRowOffsets[irows[bi]+1] - myRowOffsets[irows[bi]];
      }
      M.myRowOffsets[M.myNumBlockRows] = M.myNumRows;

      M.myNumCols = 0;
      M.myNumBlockCols = icols.length;
      M.myCols = new MatrixBlockColList[M.myNumBlockCols];
      M.myColOffsets = new int[M.myNumBlockCols + 1];
      for (int bj = 0; bj < icols.length; bj++) {
	 M.myColOffsets[bj] = M.myNumCols;
	 M.myNumCols += myColOffsets[icols[bj]+1] - myColOffsets[icols[bj]];
      }
      M.myColOffsets[M.myNumBlockCols] = M.myNumCols;

      invalidateCRSandCCSOffsets();

      int[] idxMap = getIdxMap(icols, myNumBlockCols);
      for (int bi = 0; bi < irows.length; bi++) {
	 MatrixBlock blk = myRows[irows[bi]].firstBlock();
	 while (blk != null) {
	    if (idxMap[blk.getBlockCol()] != -1) {
	       MatrixBlock newBlk = blk.clone();
	       M.addBlock(bi, idxMap[blk.getBlockCol()], newBlk);
	    }
	    blk = blk.next();
	 }
      }

      return M;
   }
   
   public void setSubMatrix(SparseBlockMatrix M, int[] irows, int[] icols) {
      int[] idxMap = getIdxMap(icols, myNumBlockCols);
      for (int bi = 0; bi < irows.length; bi++) {
	 MatrixBlock blk = myRows[irows[bi]].firstBlock();
	 while (blk != null) {
	    if (idxMap[blk.getBlockCol()] != -1) {
	       M.getBlock(bi, idxMap[blk.getBlockCol()]).set(blk);
	    }
	    blk = blk.next();
	 }
      }
   }
   
   public void getSubMatrixRow (int i, double[] values, int off, int[] icols) {
      int ncols = 0;
      int[] colOffsets = new int[icols.length+1];
      for (int bj = 0; bj < icols.length; bj++) {
	 colOffsets[bj] = ncols;
	 ncols += myColOffsets[icols[bj]+1] - myColOffsets[icols[bj]];
      }
      colOffsets[icols.length] = ncols;
      
      if (off + values.length < ncols) {
         throw new ImproperSizeException ("'values' is insufficiently large");
      }
      if (i < 0 || i >= myNumRows) {
         throw new ArrayIndexOutOfBoundsException ("requested row=" + i
         + ", number of rows=" + myNumRows);
      }
      // zero all values to start; this is easier ...
      for (int j = 0; j < ncols; j++) {
         values[j] = 0;
      }
      int bi = getBlockRow (i);
      int ilocal = i - myRowOffsets[bi];
      int[] idxMap = getIdxMap(icols, myNumBlockCols);
      for (MatrixBlock blk = myRows[bi].myHead; blk != null; blk = blk.next()) {
	 if (idxMap[blk.getBlockCol()] != -1) {
	    blk.getRow (
               ilocal, values, colOffsets[idxMap[blk.getBlockCol()]]+off);
	 }
      }
   }
   
   public void getSubMatrixColumn (int j, double[] values, int off, int[] irows) {
      int nrows = 0;
      int[] rowOffsets = new int[irows.length+1];
      for (int bi = 0; bi < irows.length; bi++) {
	 rowOffsets[bi] = nrows;
	 nrows += myRowOffsets[irows[bi]+1] - myRowOffsets[irows[bi]];
      }
      rowOffsets[irows.length] = nrows;
      
      if (off + values.length < nrows) {
         throw new ImproperSizeException ("'values' is insufficiently large");
      }
      int bj = getBlockCol (j);
      if (bj == -1) {
         throw new ArrayIndexOutOfBoundsException (
            "requested col=" + j + ", number of cols=" + myNumCols);
      }
      int jlocal = j - myColOffsets[bj];
      // zero all values to start; this is easier ...
      for (int i = 0; i < nrows; i++) {
         values[i] = 0;
      }

      for (int bi=0; bi<irows.length; bi++) {
         MatrixBlock blk = myRows[irows[bi]].get (bj);
         if (blk != null) {
            blk.getColumn (jlocal, values, rowOffsets[bi]+off);
         }
      }
   }
 
   public void mulSubMatrixVec (double[] res, double[] vec, int[] irows, int[] icols) {
      int nrows = 0;
      int[] rowOffsets = new int[irows.length+1];
      for (int bi = 0; bi < irows.length; bi++) {
	 rowOffsets[bi] = nrows;
	 nrows += myRowOffsets[irows[bi]+1] - myRowOffsets[irows[bi]];
      }
      rowOffsets[irows.length] = nrows;
      
      int ncols = 0;
      int[] colOffsets = new int[icols.length+1];
      for (int bj = 0; bj < icols.length; bj++) {
	 colOffsets[bj] = ncols;
	 ncols += myColOffsets[icols[bj]+1] - myColOffsets[icols[bj]];
      }
      colOffsets[icols.length] = ncols;
      
      for (int i=0; i<nrows; i++) {
         res[i] = 0;
      }

      int[] idxMap = getIdxMap(icols, myNumBlockCols);
      for (int bi=0; bi < irows.length; bi++) {
         for (MatrixBlock blk=myRows[irows[bi]].myHead; blk!=null; blk=blk.next()) {
	    if (idxMap[blk.getBlockCol()] != -1) {
	       blk.mulAdd(res, rowOffsets[bi], vec, colOffsets[idxMap[blk.getBlockCol()]]);
	    }
         }
      }
   }

   public int getSubMatrixRowSize(int[] irows) {
      int nrows = 0;
      for (int bi = 0; bi < irows.length; bi++) {
	 nrows += myRowOffsets[irows[bi] + 1] - myRowOffsets[irows[bi]];
      }
      return nrows;
   }

   public int getSubMatrixColSize(int[] icols) {
      int ncols = 0;
      for (int bj = 0; bj < icols.length; bj++) {
	 ncols += myColOffsets[icols[bj] + 1] - myColOffsets[icols[bj]];
      }
      return ncols;
   }
   
   public static int[] getRange(int idx, int numIdxs) {
      int[] idxs = new int[numIdxs];
      for (int i = idx; i < numIdxs; i++) {
	 idxs[i] = i;
      }
      return idxs;
   }
   
   public static int[] getComp(int[] idxs, int size) {
      int[] compIdxs = new int[size-idxs.length];
      for (int i = 0, j = 0, k = 0; i < size; i++) {
	 if (j < idxs.length && i==idxs[j])
	    j++;
	 else
	    compIdxs[k++]=i;
      }
      return compIdxs;
   }
   
   private int[] getIdxMap(int[] idxs, int size) {
      int[] idxMap = new int[size];
      for (int i = 0, k = 0; i < size; i++) {
	 if (k < idxs.length && i == idxs[k]) {
	    idxMap[i] = k++;
	 } else {
	    idxMap[i] = -1;
	 }
      }
      return idxMap;
   }

   public void writeBlocks (PrintWriter pw) throws IOException {
      writeBlocks (pw, new NumberFormat ("%g"));
   }

   /** 
    * Writes the contents of this matrix to a PrintWriter, using a sparse
    * block structure. The entire output is
    * surrounded in square brackets <code>[ ]</code>. The opening bracket
    * is then followed by two integer vectors, each contained within
    * square brackets, giving the number of rows in each block row and
    * the number of columns in each block column. This is then followed
    * by the blocks, presented in row-major order, with each
    * individual block written using the format
    * <pre>
    * [ bi bj nr nc
    *   xx xx xx
    *   xx xx xx
    *   xx xx xx
    * ]
    * </pre>
    * where <code>bi</code> and <code>bj</code> give the block row and column
    * indices, <code>nr</code> and <code>nc</code> give the number
    * of rows and columns in the block, and <code>xx</code> denotes the
    * individual numeric values in dense format. The latter are
    * formatted using a C <code>printf</code> style as
    * described by the parameter <code>NumberFormat</code>.
    *
    * @param pw
    * PrintWriter to write this matrix to
    * @param fmt
    * numeric format
    */   
   public void writeBlocks (PrintWriter pw, NumberFormat fmt) throws IOException {
      pw.print ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.print ("[ ");
      for (int bi=0; bi<numBlockRows(); bi++) {
         pw.print (getBlockRowSize(bi)+" ");
      }
      pw.println ("]");
      pw.print ("[ ");
      for (int bj=0; bj<numBlockCols(); bj++) {
         pw.print (getBlockColSize(bj)+" ");
      }
      pw.println ("]");
      for (int bi=0; bi<numBlockRows(); bi++) {
         for (MatrixBlock blk=myRows[bi].myHead; blk != null; blk = blk.next()) {
            pw.println ("[ "+bi+" "+blk.getBlockCol());
            IndentingPrintWriter.addIndentation (pw, 2);
            blk.write (pw, fmt);
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");      
      pw.flush();
   }

   /** 
    * Sets the sizing and contents of the matrix to values read from
    * a ReaderTokenizer. The format is assumed to be the sparse
    * block structure described for {@link #writeBlocks writeBlocks}.
    * 
    * @param rtok ReaderTokenizer from which values are read.
    */
   public void scanBlocks (ReaderTokenizer rtok) throws IOException {

      LinkedList<MatrixBlock> blks = new LinkedList<MatrixBlock>();
      rtok.scanToken ('[');
      int[] rowSizes = Scan.scanInts (rtok);
      for (int bi=0; bi<rowSizes.length; bi++) {
         if (rowSizes[bi] <= 0) {
            throw new IOException (
               "size for block row "+bi+" is non-positive: "+rowSizes[bi]);
         }
      }
      int[] colSizes = Scan.scanInts (rtok);
      for (int bj=0; bj<colSizes.length; bj++) {
         if (colSizes[bj] <= 0) {
            throw new IOException (
               "size for block col "+bj+" is non-positive: "+colSizes[bj]);
         }
      }

      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         rtok.scanToken ('[');
         int bi = rtok.scanInteger();
         int bj = rtok.scanInteger();
         if (bi >= rowSizes.length || bj >= colSizes.length) {
            throw new IOException (
               "Block at "+bi+","+bj+" is out of bounds");
         }
         MatrixBlock blk = MatrixBlockBase.alloc (rowSizes[bi], colSizes[bj]);
         blk.scan (rtok);
         rtok.scanToken (']');         
         blk.setBlockRow (bi);
         blk.setBlockCol (bj);
         blks.add (blk);
      }
      removeAllBlocks();

      myCRSNumBlkRows = -1;
      myCRSNumBlkCols = -1;
      myCRSRowOffs = null;
      myCCSNumBlkRows = -1;
      myCCSNumBlkCols = -1;
      myCCSColOffs = null;

      initRowColSizes (rowSizes, colSizes);

      for (MatrixBlock blk : blks) {
         addBlock (blk.getBlockRow(), blk.getBlockCol(), blk);
      }
      
   }

   /**
    * Returns a {@link SparseBlockSignature} that uniquely describes the block
    * structure of this matrix. The functionality is the same as {@link
    * #getSignature(boolean)}, with {@code vertical} used if
    * the matrix is vertically linked and the number of blocks
    * exceeds the number of block columns.
    */
   public SparseBlockSignature getSignature() {
       boolean vertical =
         (isVerticallyLinked() && myNumBlockRows > myNumBlockCols);     
       return getSignature (vertical);
   }
   
   /**
    * Returns a {@link SparseBlockSignature} that uniquely describes the block
    * structure of this matrix. The signature contains the block row and column
    * sizes of the matrix, whether the signature is horizontal or vertical (see
    * below), plus a data array with the following additional information:
    *
    * <pre>
    *  * row offset for each block row, plus total number of rows
    *  * column offset for each block column, plus total number of columns
    *  * for each block row (or column):
    *      the number of blocks in the row (or column),
    *      followed by the column (or row) index  of each block
    * </pre>
    * <p>
    * A horizontal signature lists the block column indices of each block
    * in row major order, with -1 indicating the start of each block row.
    * <p>
    * A vertical signature lists the block row indices of each block in column
    * major order, with -1 indicating the start of each block column.
    *
    * <p>A vertical signature is used if {@code vertical} is {@code true}. A
    * vertical signature will also cause the matrix to be vertically linked, if
    * it is not already.
    *
    * <p>Note that horizontal and vertical signatures will not match, even if
    * the block structure of the matrix is the same.
    *
    * @param vertical if {@code true}, returns a 
    * @return matrix signature
    */
   public SparseBlockSignature getSignature (boolean vertical) {
      if (vertical && !isVerticallyLinked()) {
         setVerticallyLinked (true);
      }
      return new SparseBlockSignature (this, vertical);
   }

   public boolean signatureEquals (SparseBlockSignature sig) {
      if (sig.isVertical() && !isVerticallyLinked()) {
         setVerticallyLinked (true);
      }
      return sig.equals (this);
   }

   /**
    * Returns true if the structure of this SparseBlockMatrix matches
    * that of another. This means that the matrices must match in
    * terms of size, row and column blocks sizes, and block placement.
    * 
    * @param M1
    * matrix to compare with
    * @return false if the matrices have different structures.
    */
   public boolean blockStructureEquals (SparseBlockMatrix M1) {
      if (M1 == this) {
         return true;
      }
      if (M1.myNumBlockRows != myNumBlockRows ||
          M1.myNumBlockCols != myNumBlockCols) {
         return false;
      }
      for (int bi=0; bi<=myNumBlockRows; bi++) {
         if (M1.myRowOffsets[bi] != myRowOffsets[bi]) {
            return false;
         }
      }
      for (int bj=0; bj<=myNumBlockCols; bj++) {
         if (M1.myColOffsets[bj] != myColOffsets[bj]) {
            return false;
         }
      }
      for (int bi=0; bi<myNumBlockRows; bi++) {
         MatrixBlock matchingBlk=M1.myRows[bi].myHead;
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
            if (matchingBlk == null) {
               return false;
            }
            else if (blk.getBlockCol() != matchingBlk.getBlockCol()) {
               return false;
            }
            matchingBlk=matchingBlk.next();
         }
         if (matchingBlk != null) {
            return false;
         }
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public double frobeniusNormSquared() {
      // returns sqrt(sum (diag (M'*M))
      
      double f = 0;
      for (int bi=0; bi<myNumBlockRows; bi++) {
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
            f += blk.frobeniusNormSquared();
         }
      }
      return f;
   }
   
   @Override
   public double maxNorm() {  
      double m = 0;
      for (int bi=0; bi<myNumBlockRows; bi++) {
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
            double n = blk.maxNorm();
            if (n > m) {
               m = n;
            }
         }
      }
      return m;
   }

   @Override
   public boolean isSymmetric (double tol) {
      for (int bi=0; bi<myNumBlockRows; bi++) {
         for (MatrixBlock blk=myRows[bi].myHead; blk!=null; blk=blk.next()) {
            int bj = blk.getBlockCol();
            if (bi == bj) {
               if (!blk.isSymmetric (tol)) {
                  return false;
               }
            }
            else {
               MatrixBlock blkT = getBlock (bj, bi);
               MatrixNd T;
               if (blkT == null) {
                  // Matrix is structurally assymetric but might still be
                  // numerically symmetric. Create a zero block to compare to
                  T = new MatrixNd (blk.rowSize(), blk.colSize());
               }
               else {
                  T = new MatrixNd(blkT);
                  T.transpose();
               }
               if (!T.epsilonEquals (blk, tol)) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   protected void disposeBlock (MatrixBlock blk) {
   }

   public boolean hasSymmetricBlockSizing () {
      if (numBlockRows() != numBlockCols()) {
         return false;
      }
      for (int bi=0; bi<numBlockRows(); bi++) {
         if (getBlockRowSize(bi) != getBlockColSize(bi)) {
            return false;
         }
      }
      return true;
   }   

   static public SparseBlockMatrix createTranspose (SparseBlockMatrix S) {
      int[] rowSizes = S.getBlockRowSizes();
      int[] colSizes = S.getBlockColSizes();
      SparseBlockMatrix M = new SparseBlockMatrix (colSizes, rowSizes);
      for (int bi=0; bi<S.numBlockRows(); bi++) {
         MatrixBlock blk;
         for (blk=S.firstBlockInRow(bi); blk!=null; blk=blk.next()) {
            int bj = blk.getBlockCol();
            M.addBlock (bj, bi, blk.createTranspose());
         }
      }
      return M;
   }
}
