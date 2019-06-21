/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Random;

import maspack.matrix.Matrix.Partition;
import maspack.util.IndentingPrintWriter;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;
import maspack.util.TestException;


public class SparseBlockMatrixTest extends MatrixTest {
   SparseBlockMatrix Mat;
   SparseBlockMatrix MatSym;
   //SparseBlockMatrix MatGen;
   SparseBlockMatrix MatAll;
   Random randGen;

   SparseBlockMatrix createSum (
      double s, SparseBlockMatrix M1, SparseBlockMatrix M2, 
      int numBlkRows, int numBlkCols) {

      SparseBlockMatrix MR = M1.clone();
      for (int bi=0; bi<numBlkRows; bi++) {
         for (int bj=0; bj<numBlkCols; bj++) {
            MatrixBlock blk1 = MR.getBlock (bi, bj);
            MatrixBlock blk2 = M2.getBlock (bi, bj);
            if (blk1 == null && blk2 != null) {
               MatrixBlock blkNew = blk2.clone();
               blkNew.scale (s);
               MR.addBlock (bi, bj, blkNew);
            }
            else if (blk1 != null && blk2 != null) {
               blk1.scaledAdd (s, blk2);
            }
         }
      }
      return MR;
   }

   SparseBlockMatrix createRandom (
      int[] rowSizes, int[] colSizes, double density) {
      SparseBlockMatrix M = new SparseBlockMatrix (rowSizes, colSizes);

      for (int bi=0; bi<rowSizes.length; bi++) {
         for (int bj=0; bj<colSizes.length; bj++) {
            double val = randGen.nextDouble();
            if (val < density) {
               int nr = rowSizes[bi];
               int nc = colSizes[bj];
               MatrixBlock blk = MatrixBlockBase.alloc (nr, nc);
               MatrixNd MX = new MatrixNd (nr, nc);
               MX.setRandom();
               blk.set (MX);
               M.addBlock (bi, bj, blk);
            }
         }
      }
      return M;
   }

   /**
    * Create a sparse block matrix from a specially formatted string composed
    * of single characters giving the block row/col sizes and occupancy.
    */
   SparseBlockMatrix create (String str) {

      // strip out white space except newlines:
      str = str.replaceAll (" ", "");
      String lines[] = str.split ("\\n");
      int numBlkCols = lines[0].length();
      int numBlkRows = lines.length-1;
      int[] colSizes = new int[numBlkCols];
      for (int k=0; k<numBlkCols; k++) {
         int c = lines[0].charAt(k);
         if (Character.isDigit (c)) {
            colSizes[k] = c - '0';
         }
         else {
            throw new IllegalArgumentException (
               "first line, char "+k+": expected digit");
         }
      }
      int[] rowSizes = new int[numBlkRows];
      int[][] occupied = new int[numBlkRows][numBlkCols];
      for (int i=0; i<numBlkRows; i++) {
         for (int j=0; j<numBlkCols; j++) {
            occupied[i][j] = -1;
         }
      }
      for (int i=0; i<numBlkRows; i++) {
         int c = lines[i+1].charAt(0);
         if (Character.isDigit (c)) {
            rowSizes[i] = c - '0';
         }
         else {
            throw new IllegalArgumentException (
               "line "+(i+1)+": first character is not a digit");
         }
         for (int k=1; k<lines[i+1].length() && k-1<numBlkCols; k++) {
            c = lines[i+1].charAt(k);
            if (c == 'x') {
               occupied[i][k-1] = 0;
            }
            else if (Character.isDigit(c)) {
               occupied[i][k-1] = c - '0';
            }
            else if (c != '.') {
               throw new IllegalArgumentException (
                  "line "+(i+1)+": character "+k+" is not 'x', '.', "+
                  "or a digit");
            }
         }
      }
      SparseBlockMatrix M = new SparseBlockMatrix (rowSizes, colSizes);
      for (int bi=0; bi<numBlkRows; bi++) {
         for (int bj=0; bj<numBlkCols; bj++) {
            int val = occupied[bi][bj];
            if (val != -1) {
               MatrixBlock blk =
                  MatrixBlockBase.alloc (rowSizes[bi], colSizes[bj]);
               if (val > 0) {
                  for (int i=0; i<rowSizes[bi]; i++) {
                     for (int j=0; j<colSizes[bj]; j++) {
                        blk.set (i, j, val);
                     }
                  }
               }
               M.addBlock (bi, bj, blk);
            }
         }
      }
      return M;
   }

   SparseBlockMatrixTest() {

      int[] rowSizes = new int[] { 3, 3, 6, 6 };

      randGen = RandomGenerator.get();
      randGen.setSeed (0x1234);

      MatSym = new SparseBlockMatrix (rowSizes);
      //MatGen = new SparseBlockMatrix (rowSizes);
      Mat = new SparseBlockMatrix (rowSizes);
      MatAll = new SparseBlockMatrix();

      MatSym.addBlock (0, 0, new Matrix3x3Block());
      MatSym.addBlock (1, 1, new Matrix3x3Block());
      MatSym.addBlock (0, 1, new Matrix3x3Block());
      MatSym.addBlock (1, 0, new Matrix3x3Block());
      MatSym.addBlock (2, 2, new Matrix6dBlock());
      MatSym.addBlock (3, 3, new Matrix6dBlock());
      MatSym.addBlock (2, 3, new Matrix6dBlock());
      MatSym.addBlock (3, 2, new Matrix6dBlock());
      MatSym.addBlock (0, 3, new Matrix3x6Block());
      MatSym.addBlock (1, 2, new Matrix3x6Block());
      MatSym.addBlock (3, 0, new Matrix6x3Block());
      MatSym.addBlock (2, 1, new Matrix6x3Block());

      // Mat is similar to MatSym except it has some blocks removed
      // to make it unsymmetric
      Mat.addBlock (0, 0, new Matrix3x3Block());
      Mat.addBlock (1, 1, new Matrix3x3Block());
      Mat.addBlock (0, 1, new Matrix3x3Block());
      Mat.addBlock (1, 0, new Matrix3x3Block());
      Mat.addBlock (2, 2, new Matrix6dBlock());
      Mat.addBlock (3, 3, new Matrix6dBlock());
      Mat.addBlock (2, 3, new Matrix6dBlock());
      Mat.addBlock (3, 2, new Matrix6dBlock());
      Mat.addBlock (0, 3, new Matrix3x6Block());
      Mat.addBlock (1, 2, new Matrix3x6Block());
      Mat.addBlock (3, 0, new Matrix6x3Block());
      Mat.addBlock (2, 1, new Matrix6x3Block());

      Mat.removeBlock (Mat.getBlock (1, 0));
      Mat.removeBlock (Mat.getBlock (2, 3));
      Mat.removeBlock (Mat.getBlock (2, 1));

      // MatGen is the same as MatSym, expect that it uses
      // generic Matrix3dBlocks with no overriden functions.

//       MatGen.addBlock (0, 0, new Matrix3dBlock (1, 1));
//       MatGen.addBlock (1, 1, new Matrix3dBlock (1, 1));
//       MatGen.addBlock (0, 1, new Matrix3dBlock (1, 1));
//       MatGen.addBlock (1, 0, new Matrix3dBlock (1, 1));
//       MatGen.addBlock (2, 2, new Matrix3dBlock (2, 2));
//       MatGen.addBlock (3, 3, new Matrix3dBlock (2, 2));
//       MatGen.addBlock (2, 3, new Matrix3dBlock (2, 2));
//       MatGen.addBlock (3, 2, new Matrix3dBlock (2, 2));
//       MatGen.addBlock (0, 3, new Matrix3dBlock (1, 2));
//       MatGen.addBlock (1, 2, new Matrix3dBlock (1, 2));
//       MatGen.addBlock (3, 0, new Matrix3dBlock (2, 1));
//       MatGen.addBlock (2, 1, new Matrix3dBlock (2, 1));

      // MatAll tests as many of the Matrix block types as possible

      MatAll.addBlock (0, 0, new Matrix2x2Block());
      MatAll.addBlock (0, 1, new Matrix2x3Block());
      MatAll.addBlock (1, 0, new Matrix3x2Block());
      MatAll.addBlock (1, 1, new Matrix3x3Block());
      MatAll.addBlock (1, 2, new Matrix3x6Block());
      MatAll.addBlock (2, 1, new Matrix6x3Block());
      MatAll.addBlock (2, 2, new Matrix6dBlock());
      MatAll.addBlock (2, 3, new MatrixNdBlock (6, 4));
      MatAll.addBlock (3, 2, new MatrixNdBlock (4, 6));
      MatAll.addBlock (3, 3, new MatrixNdBlock (4, 4));
      MatAll.addBlock (4, 1, new Matrix1x3Block());
      MatAll.addBlock (4, 2, new Matrix1x6Block());
      MatAll.addBlock (1, 4, new Matrix3x1Block());
      MatAll.addBlock (2, 4, new Matrix6x1Block());
      MatAll.addBlock (5, 5, new Matrix6dBlock());
      MatAll.addBlock (2, 5, new Matrix6dBlock());
      MatAll.addBlock (5, 2, new Matrix6dBlock());
      MatAll.addBlock (6, 6, new Matrix3x3DiagBlock());
      MatAll.addBlock (6, 1, new Matrix3x3DiagBlock());
      MatAll.addBlock (1, 6, new Matrix3x3DiagBlock());
      MatAll.addBlock (7, 1, new Matrix4x3Block());
      MatAll.addBlock (1, 7, new Matrix3x4Block());

      // System.out.println ("MatSym blocks");
      // MatSym.printBlocks (System.out);
      // System.out.println ("Mat blocks");
      // Mat.printBlocks (System.out);
   }

//    private void setMatrixRandom (Matrix M, boolean symmetric) {
//       for (int i = 0; i < M.rowSize(); i++) {
//          if (symmetric) {
//             for (int j = i; j < M.colSize(); j++) {
//                double val = randGen.nextDouble();
//                M.set (i, j, val);
//                M.set (j, i, val);
//             }
//          }
//          else // part == Full
//          {
//             for (int j = 0; j < M.colSize(); j++) {
//                double val = randGen.nextDouble();
//                M.set (i, j, val);
//             }
//          }
//       }
//    }

   private void testStructure (SparseBlockMatrix M) {
      int nrows = M.rowSize();
      int ncols = M.colSize();
      for (int i = 0; i < nrows; i++) {
         for (int j = 0; j < ncols; j++) {
            MatrixBlock blk = M.getElementBlock (i, j);
            if (blk != null) {
               int bi = blk.getBlockRow();
               int bj = blk.getBlockCol();
               if (M.get (i, j) != blk.get (i - M.myRowOffsets[bi], j
               - M.myColOffsets[bj])) {
                  throw new TestException (
                     "Bad row or col offsets at " + i + "," + j);
               }
            }
            else {
               if (M.get (i, j) != 0) {
                  throw new TestException ("Zero expected at " + i + "," + j);
               }
            }
         }
      }
   }

   private void testGetCRS (SparseBlockMatrix S) {
      int maxBlocks = Math.min (S.numBlockRows(), S.numBlockCols());
      // test the Matrix.getCCS routines while we're at it ...
      MatrixNd M = new MatrixNd();
      M.set (S);

      int size = 0;
      for (int bi = 0; bi < maxBlocks; bi++) {
         size = S.rowSize (bi + 1);
         testGetCRS (S, size, size);
         testGetCRS (M, size, size);
      }
      for (int bi = maxBlocks - 1; bi > 0; bi--) {
         size = S.rowSize (bi + 1);
         testGetCRS (S, size, size);
         testGetCRS (M, size, size);
      }
   }

   private void testGetCCS (SparseBlockMatrix S) {
      int maxBlocks = Math.min (S.numBlockRows(), S.numBlockCols());
      // test the Matrix.getCCS routines while we're at it ...
      MatrixNd M = new MatrixNd();
      M.set (S);

      int size = 0;
      for (int bi = 0; bi < maxBlocks; bi++) {
         size = S.rowSize (bi + 1);
         testGetCCS (S, size, size);
         testGetCCS (M, size, size);
      }
      for (int bi = maxBlocks - 1; bi > 0; bi--) {
         size = S.rowSize (bi + 1);
         testGetCCS (S, size, size);
         testGetCCS (M, size, size);
      }
   }

   private void testMul (SparseBlockMatrix M) {
      int maxBlocks = Math.min (M.numBlockRows(), M.numBlockCols());
      for (int nb = 1; nb <= maxBlocks; nb++) {
         testMul (M, 0, nb, 0, nb);
         testMul (M, 0, nb, 0, maxBlocks);
         testMul (M, 0, maxBlocks, 0, nb);
         if (nb < maxBlocks) {
            testMul (M, 1, nb, 0, nb);            
            testMul (M, 0, nb, 1, nb);            
            testMul (M, 1, nb, 1, nb);            
         }
         if (nb < maxBlocks-1) {
            testMul (M, 2, nb, 0, nb);            
            testMul (M, 2, nb, 1, nb);            
            testMul (M, 1, nb, 2, nb);            
            testMul (M, 0, nb, 2, nb);            
         }
      }
   }

   private void testMul (
      SparseBlockMatrix M, int rb0, int nbr, int cb0, int nbc) {
               
      int r0 = M.getBlockRowOffset (rb0);
      int nr = M.getBlockRowOffset (rb0+nbr) - r0;
      int c0 = M.getBlockColOffset (cb0);
      int nc = M.getBlockColOffset (cb0+nbc) - c0;

      VectorNd y = new VectorNd (nr);
      VectorNd ysave = new VectorNd (nr);
      VectorNd ycheck = new VectorNd (nr);
      VectorNd x = new VectorNd (nc);
      x.setRandom();

      for (int i=r0; i<r0+nr; i++) {
         double sum = 0;
         for (int j=c0; j<c0+nc; j++) {
            sum += M.get(i,j)*x.get (j-c0);
         }
         ycheck.set (i-r0, sum);
      }

      if (r0 == 0 && c0 == 0) {
         M.mul (y, x, nr, nc);
         checkResult ("mul", y, ycheck, 1e-10);
      }
      M.mul (y, x, r0, nr, c0, nc);
      checkResult ("mul", y, ycheck, 1e-10);
      ysave.set (y);
      ycheck.add (ycheck);
      if (r0 == 0 && c0 == 0) {
         M.mulAdd (y, x, nr, nc);
         checkResult ("mulAdd", y, ycheck, 1e-10);
      }
      y.set (ysave);
      M.mulAdd (y, x, r0, nr, c0, nc);
      checkResult ("mulAdd", y, ycheck, 1e-10);
   }

   private void testMulTranspose (SparseBlockMatrix M) {
      int maxBlocks = Math.min (M.numBlockRows(), M.numBlockCols());
      for (int nb = 1; nb <= maxBlocks; nb++) {
         testMulTranspose (M, 0, nb, 0, nb);
         testMulTranspose (M, 0, nb, 0, maxBlocks);
         testMulTranspose (M, 0, maxBlocks, 0, nb);
         if (nb < maxBlocks) {
            testMulTranspose (M, 1, nb, 0, nb);            
            testMulTranspose (M, 0, nb, 1, nb);            
            testMulTranspose (M, 1, nb, 1, nb);            
         }
         if (nb < maxBlocks-1) {
            testMulTranspose (M, 2, nb, 0, nb);            
            testMulTranspose (M, 2, nb, 1, nb);            
            testMulTranspose (M, 1, nb, 2, nb);            
            testMulTranspose (M, 0, nb, 2, nb);            
         }
      }
   }

   private void testMulTranspose (
      SparseBlockMatrix M, int rb0, int nbr, int cb0, int nbc) {
               
      int r0 = M.getBlockColOffset (rb0);
      int nr = M.getBlockColOffset (rb0+nbr) - r0;
      int c0 = M.getBlockRowOffset (cb0);
      int nc = M.getBlockRowOffset (cb0+nbc) - c0;

      VectorNd y = new VectorNd (nr);
      VectorNd ycheck = new VectorNd (nr);
      VectorNd x = new VectorNd (nc);
      x.setRandom();

      for (int i=r0; i<r0+nr; i++) {
         double sum = 0;
         for (int j=c0; j<c0+nc; j++) {
            sum += M.get(j,i)*x.get(j-c0);
         }
         ycheck.set (i-r0, sum);
      }

      if (r0 == 0 && c0 ==0) {
         M.mulTranspose (y, x, nr, nc);
         checkResult ("mulTranspose", y, ycheck, 1e-10);
      }
      M.mulTranspose (y, x, r0, nr, c0, nc);
      checkResult ("mulTranspose", y, ycheck, 1e-10);
      // ysave.set (y);
      // ycheck.add (ycheck);
      // if (r0 == 0 && c0 ==0) {
      //    M.mulTransposeAdd (y, x, nr, nc);
      //    checkResult ("mulTransposeAdd", y, ycheck, 1e-10);
      // }
      // y.set (ysave);
      // M.mulTransposeAdd (y, x, r0, nr, c0, nc);
      // checkResult ("mulTransposeAdd", y, ycheck, 1e-10);
   }

   private void testGetCRS (Matrix M, int nrows, int ncols) {
      SparseBlockMatrix S = null;
      int maxvals;
      if (M instanceof SparseBlockMatrix) {
         S = (SparseBlockMatrix)M;
         maxvals = S.numNonZeroVals (Partition.Full, nrows, ncols);
      }
      else {
         maxvals = nrows * ncols;
      }
      //System.out.println ("maxvals=" + maxvals);
      double[] vals = new double[maxvals];
      int[] colIdxs = new int[maxvals];
      int[] rowOffs = new int[nrows+1];

      Partition[] parts =
         new Partition[] { Partition.Full, Partition.UpperTriangular };

      for (int k = 0; k < parts.length; k++) {
         Partition part = parts[k];
         int nnz = M.getCRSIndices (colIdxs, rowOffs, part, nrows, ncols);
         for (int i=0; i<nrows; i++) {
            //System.out.println (" rowOff " + i + " " + rowOffs[i]);
         }
         for (int i=0; i<maxvals; i++) {
            //System.out.println (" colIdx " + i + " " + colIdxs[i]);
         }
         
         M.getCRSValues (vals, part, nrows, ncols);

         for (int i=0; i<maxvals; i++) {
            //System.out.println (" vals   " + i + " " + vals[i]);
         }

         int nvals = maxvals;
         if (part == Partition.LowerTriangular) {
            nvals = (nvals - nrows) / 2 + nrows;
         }
         // System.out.println ("part=" + part);
         // for (int i=0; i<nvals; i++)
         // { System.out.println (colIdxs[i] + " " + vals[i]);
         // }
         int vi = 0;
         for (int i = 0; i < nrows; i++) {
            if (rowOffs[i] != vi+1) {
               throw new TestException ("rowOffs[" + i + "]: got " + rowOffs[i]
               + ", expected " + (vi+1));
            }
            int j0 = (part == Partition.UpperTriangular ? i : 0);
            for (int j = j0; j < ncols; j++) {
               if (S == null || S.valueIsNonZero (i, j)) {
                  if (vals[vi] != M.get (i, j)) {
                     throw new TestException ("get vals for " + i + "," + j
                     + ": got " + vals[vi] + ", expected " + M.get (i, j));
                  }
                  if (colIdxs[vi] != j+1) {
                     throw new TestException ("get colIdxs for " + i + "," + j
                     + ": got " + colIdxs[vi] + ", expected " + (j+1));
                  }
                  vi++;
               }
            }
         }
         if (rowOffs[nrows] != nnz+1) {
            throw new TestException ("rowOffs["+nrows+"]: got "+rowOffs[nrows]
               + ", expected " + (nnz+1));
         }
         
      }
   }

   private void testGetCCS (Matrix M, int nrows, int ncols) {
      SparseBlockMatrix S = null;
      int maxvals;
      if (M instanceof SparseBlockMatrix) {
         S = (SparseBlockMatrix)M;
         maxvals = S.numNonZeroVals (Partition.Full, nrows, ncols);
      }
      else {
         maxvals = nrows * ncols;
      }
      double[] vals = new double[maxvals];
      int[] rowIdxs = new int[maxvals];
      int[] colOffs = new int[ncols+1];

      Partition[] parts =
         new Partition[] { Partition.Full, Partition.LowerTriangular };

      for (int k = 0; k < parts.length; k++) {
         Partition part = parts[k];
         M.getCCSIndices (rowIdxs, colOffs, part, nrows, ncols);
         M.getCCSValues (vals, part, nrows, ncols);

         // System.out.println ("part=" + part);
         int nvals = maxvals;
         if (part == Partition.LowerTriangular) {
            nvals = (nvals - nrows) / 2 + nrows;
         }
         // for (int i=0; i<nvals; i++)
         // { System.out.println (rowIdxs[i] + " " + vals[i]);
         // }
         int vi = 0;
         for (int j = 0; j < ncols; j++) {
            if (colOffs[j] != (vi+1)) {
               throw new TestException ("colOffs[" + j + "]: got " + colOffs[j]
               + ", expected " + (vi+1));
            }
            int i0 = (part == Partition.LowerTriangular ? j : 0);
            for (int i = i0; i < nrows; i++) {
               if (S == null || S.valueIsNonZero (i, j)) {
                  if (vals[vi] != M.get (i, j)) {
                     throw new TestException ("get vals for " + j + "," + i
                     + ": got " + vals[vi] + ", expected " + M.get (i, j));
                  }
                  if (rowIdxs[vi] != i+1) {
                     throw new TestException ("get rowIdxs for " + j + "," + i
                     + ": got " + rowIdxs[vi] + ", expected " + (i+1));
                  }
                  vi++;
               }
            }
         }

      }
   }

   private void testAutoSize (SparseBlockMatrix M) {
      SparseBlockMatrix S = new SparseBlockMatrix();

      for (int bi = 0; bi < M.numBlockRows(); bi++) {
         MatrixBlock blk = M.firstBlockInRow (bi);
         int bj = 0;
         while (blk != null && (bj = blk.getBlockCol()) < bi) {
            S.addBlock (bi, bj, (MatrixBlock)blk.clone());
            blk = blk.next();
         }
         // don't use .down() because that might not be supported
         // if M is not vertically linked

         // blk = M.firstBlockInCol (bi);
         // while (blk != null && (bj = blk.getBlockRow()) <= bi) {
         //    S.addBlock (bj, bi, (MatrixBlock)blk.clone());
         //    blk = blk.down();
         // }
         for (bj = 0; bj <= bi; bj++) {
            blk = M.getBlock (bj, bi);
            if (blk != null) {
               S.addBlock (bj, bi, (MatrixBlock)blk.clone());
            }
         }
         int nrows = M.rowSize (bi + 1);
         int ncols = M.colSize (bi + 1);
         MatrixNd Msub = new MatrixNd (nrows, ncols);
         for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
               Msub.set (i, j, M.get (i, j));
            }
         }
         if (!Msub.equals (S)) {
            System.out.println ("Copied matrix=[\n" + S.toString ("%8.3f")
            + "]");
            System.out.println ("Expected=[\n" + Msub.toString ("%8.3f") + "]");
            throw new TestException ("AutoSizing copy failed at bi=" + bi);
         }
      }
   }

   private void testVerticalLinking (SparseBlockMatrix M) {
      SparseBlockMatrix S = M.clone();
      S.setVerticallyLinked (false);
      S.checkConsistency();
      S.setVerticallyLinked (true);
      S.checkConsistency();
      S.setVerticallyLinked (false);
      S.checkConsistency();
   }

   private void testScanBlocks (SparseBlockMatrix M) {
      SparseBlockMatrix X = new SparseBlockMatrix ();
      StringWriter sw = new StringWriter();
      try {
         IndentingPrintWriter pw = new IndentingPrintWriter (
            new PrintWriter (sw));
         M.writeBlocks (pw);
         pw.flush();
         ReaderTokenizer rtok = new ReaderTokenizer (
            new StringReader (sw.toString()));
         X.scanBlocks (rtok);
      }
      catch (Exception e) {
         e.printStackTrace(); 
         throw new TestException (
            "Error writing/scaning blocks: " + e);
      }
      X.checkConsistency();
      if (!X.equals (M) || !X.blockStructureEquals (M)) {
         throw new TestException (
            "Error writing/scaning: scanned matrix not equal to orginal");
      }
   }

   private void testClone (SparseBlockMatrix M) {
      SparseBlockMatrix Mclone = M.clone();
      if (!Mclone.equals (M)) {
         throw new TestException ("cloned matrix not equal to original");
      }
      for (int bi = 0; bi < M.numBlockRows(); bi++) {
         for (int bj = 0; bj < M.numBlockCols(); bj++) {
            MatrixBlock blk = M.getBlock (bi, bj);
            MatrixBlock blkClone = Mclone.getBlock (bi, bj);
            if ((blk == null) != (blkClone == null)) {
               throw new TestException ("cloned matrix has bad block at " + bi
               + ", " + bj);
            }
            if (blk != null && blk == blkClone) {
               throw new TestException ("MatrixBlock not cloned at " + bi
               + ", " + bj);
            }
//             if (blkClone != null &&
//                 Mclone.getBlockByNumber (
//                    blkClone.getBlockNumber()) != blkClone) {
//                throw new TestException ("Cloned block " + bi + ", " + bj
//                + " not retrievable with its own number");
//             }
         }
      }
   }

   private void testStructureEquals (
      SparseBlockMatrix M0, SparseBlockMatrix M1, boolean equal) {
      if (M0.blockStructureEquals (M1) != equal) {
         System.out.println ("M0=\n" + M0.getBlockPattern());
         System.out.println ("M1=\n" + M1.getBlockPattern());
         throw new TestException (
            "structureEquals: result="+(!equal)+", expecting "+equal);
      }
   }


   private void testStructureEquals () {
      ArrayList<SparseBlockMatrix> mats =
         new ArrayList<SparseBlockMatrix>();

      mats.add (create (
                   "    3 3 6 4\n" + 
                   "  3 x . . x\n" +
                   "  3 . x . x\n" +
                   "  4 . x x ."));
      mats.add (create (
                   "    3 3 6 6\n" + 
                   "  3 x . . x\n" +
                   "  3 . x . x\n" +
                   "  4 . x x ."));
      mats.add (create (
                   "    3 3 6 6\n" + 
                   "  3 x . . x\n" +
                   "  3 . x . x\n" +
                   "  6 . x x ."));
      mats.add (create (
                   "    3 3 6 6\n" + 
                   "  3 x . x x\n" +
                   "  3 x x . x\n" +
                   "  6 . x x ."));
      mats.add (create (
                   "    3 3 6 6 4\n" + 
                   "  3 x . . x .\n" +
                   "  3 . x . x x\n" +
                   "  6 . x x . x"));
      mats.add (create (
                   "    3 3 6 6 4\n" + 
                   "  3 x . . x .\n" +
                   "  3 . x . x .\n" +
                   "  6 . x x . x"));
      for (int i=0; i<mats.size(); i++) {
         for (int j=0; j<mats.size(); j++) {
            testStructureEquals (mats.get(i), mats.get(j), i==j);
         }
      }
   }

   private void testAdd() {
      
      // SparseBlockMatrix M1 =
      //    create (
      //       "   3 2 3 4\n" +
      //       " 3 1 . 1 1\n" +
      //       " 4 . 1 . 1\n" +
      //       " 4 1 1 . 1");

      // SparseBlockMatrix Mcheck =
      //    create (
      //       "   3 2 3 4\n" +
      //       " 3 2 . 2 2\n" +
      //       " 4 . 2 . 2\n" +
      //       " 4 2 2 . 2");

      
      // SparseBlockMatrix MR = M1.clone();
      // MR.add (M1);
      // checkEqual (MR, Mcheck);

      int rowSizes[] = new int[] {3, 2, 3, 4, 5, 7, 6};
      int colSizes[] = new int[] {3, 4, 1, 4, 5, 3, 6};

      int cnt = 100;

      for (int i=0; i<cnt; i++) {
         SparseBlockMatrix M1 = createRandom (rowSizes, colSizes, 0.33);
         SparseBlockMatrix M2 = createRandom (rowSizes, colSizes, 0.33);
         SparseBlockMatrix MR, Mcheck;

         double[] scales = new double[] { 1.0, -1.0, 2.3, 0, -6.7 };

         for (int j=0; j<scales.length; j++) {
            double s = scales[j];

            MR = M1.clone();
            Mcheck = createSum (s, M1, M2, rowSizes.length, colSizes.length);
            MR.scaledAdd (s, M2);
            checkEqual (MR, Mcheck);
            
            MR = M1.clone();
            Mcheck = createSum (s, M1, M2, 3, 4);
            MR.scaledAdd (s, M2, 3, 4);
            checkEqual (MR, Mcheck);
            
            MR = M1.clone();
            Mcheck = createSum (s, M1, M2, 2, 5);
            MR.scaledAdd (s, M2, 2, 5);
            checkEqual (MR, Mcheck);
            
            MR = M1.clone();
            Mcheck = createSum (s, M1, M2, 6, 2);
            MR.scaledAdd (s, M2, 6, 2);
            checkEqual (MR, Mcheck);
         }
      }
   }

   private void checkEqual (SparseBlockMatrix M1, SparseBlockMatrix M2) {
      if (!M1.blockStructureEquals (M2)) {
         System.out.println ("Structure of M1:");
         M1.printBlocks (System.out);
         System.out.println ("Structure of M2:");
         M2.printBlocks (System.out);
         throw new TestException (
            "Matrix structures are unequal");
      }
      if (!M1.equals (M2)) {
         System.out.println ("M1:\n" + M1);
         System.out.println ("M2:\n" + M2);
         throw new TestException (
            "Matrices are unequal");
      }
   }

   public void test() {

      MatSym.setRandomValues (true);
      //setMatrixRandom (MatGen, /* symmetric= */true);
      Mat.setRandomValues (true);
      MatAll.setRandomValues (true);

//       testGetNonZeros(MatSym, Partition.Full);
//       testGetNonZeros(MatSym, Partition.UpperTriangular);
//       testGetNonZeros(Mat, Partition.Full);
//       testGetNonZeros(MatGen, Partition.Full);
//       testGetNonZeros(MatGen, Partition.UpperTriangular);

      for (int i=0; i<10; i++) {
         testGeneric (MatSym);
         testGeneric (Mat);
         testGeneric (MatAll);
      }

      testGet (MatSym);
      testGet (Mat);
      testGet (MatAll);

      testGetCRS (MatSym);
      testGetCRS (Mat);
      //testGetCRS (MatGen);
      testGetCRS (MatAll);

      testGetCCS (MatSym);
      testGetCCS (Mat);
      //testGetCCS (MatGen);
      testGetCCS (MatAll);

      testMul (MatSym);
      testMul (Mat);
      //testMul (MatGen);
      testMul (MatAll);

      testMulTranspose (MatSym);
      testMulTranspose (Mat);
      //testMulTranspose (MatGen);
      testMulTranspose (MatAll);

      testStructure (MatSym);
      testStructure (Mat);
      //testStructure (MatGen);
      testStructure (MatAll);

      testAutoSize (MatSym);
      testAutoSize (Mat);
      //testAutoSize (MatGen);
      testAutoSize (MatAll);

      testClone (MatSym);
      testClone (Mat);
      testClone (MatAll);

      testVerticalLinking (MatSym);
      testVerticalLinking (Mat);
      testVerticalLinking (MatAll);

      testScanBlocks (MatSym);
      testScanBlocks (Mat);
      testScanBlocks (MatAll);

      testStructureEquals ();
      testAdd ();
   }

   public static void main (String[] args) {
      SparseBlockMatrixTest tester = new SparseBlockMatrixTest();
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
