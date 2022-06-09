package maspack.matrix;

import java.util.*;
import maspack.util.*;

public class SparseBlockSignatureTest extends UnitTest {

   SparseBlockMatrix createRandomBlockMatrix (
      int numBlkRows, int numBlkCols, int maxBlksPerRow) {
      
      int[] blkRowSizes = new int[numBlkRows];
      int[] blkColSizes;
      for (int bi=0; bi<numBlkRows; bi++) {
         blkRowSizes[bi] = RandomGenerator.nextDouble() > 0.8 ? 6 : 3;
      }
      if (numBlkRows == numBlkCols) {
         blkColSizes = blkRowSizes;
      }
      else {
         blkColSizes = new int[numBlkCols];
         for (int bi=0; bi<numBlkCols; bi++) {
            blkColSizes[bi] = RandomGenerator.nextDouble() > 0.8 ? 6 : 3;
         }
      }
      SparseBlockMatrix S = new SparseBlockMatrix (blkRowSizes, blkColSizes);
      for (int bi=0; bi<numBlkRows; bi++) {
         int numBlks =
            RandomGenerator.nextInt (0, Math.min(numBlkCols, maxBlksPerRow));
         boolean[] marked = new boolean[numBlkCols];
         int k = 0;
         do {
            int bj = RandomGenerator.nextInt(0, numBlkCols-1);
            if (!marked[bj]) {
               marked[bj] = true;
               k++;
            }
         }
         while (k < numBlks);
         for (int bj=0; bj<marked.length; bj++) {
            if (marked[bj]) {
               MatrixBlock blk = MatrixBlockBase.alloc (
                  S.getBlockRowSize(bi), S.getBlockColSize(bj));
               S.addBlock (bi, bj, blk);
            }
         }
      }
      return S;
   }

   void check (int numBlkRows, int numBlkCols, int maxBlksPerRow) {
      int ntests = 1000;
      int cnt = 10;
      FunctionTimer sigTimer = new FunctionTimer();
      FunctionTimer equalsTimer = new FunctionTimer();

      SparseBlockSignature hash;
      for (int k=0; k<ntests; k++) {
         SparseBlockMatrix S =
            createRandomBlockMatrix (numBlkRows, numBlkCols, maxBlksPerRow);

         SparseBlockSignature prev = S.getSignature(/*vertical=*/true);
         sigTimer.restart();
         for (int i=0; i<cnt; i++) {
            SparseBlockSignature sig = S.getSignature(/*vertical=*/true);
            sig.equals (prev);
         }
         sigTimer.stop();

         SparseBlockMatrix C = S.clone();
         equalsTimer.restart();
         for (int i=0; i<cnt; i++) {
            S.blockStructureEquals (C);
         }
         equalsTimer.stop();
      }
      System.out.printf (
         "%d x %d: signature=%s blockEqualsCheck=%s\n",
         numBlkRows, numBlkCols, 
         sigTimer.result (ntests*cnt),
         equalsTimer.result (ntests*cnt));
   }

   SparseBlockSignatureTest() {
   }

   public void check() {
      System.out.println ("Time needed for signature checking:");
      check (50, 50, 5);
      check (100, 100, 10);
      check (200, 200, 10);
   }

   public void test() {
      testPrevIdxsSpecial();
      testPrevIdxsGeneral();
      testGeneral();
   }
         
   public void testGeneral() {
      int ntests = 10000;
      SparseBlockSignature prevSig = null;
      SparseBlockMatrix prevS = null;
      for (int i=0; i<ntests; i++) {
         int maxSize = 100;
         int nrows = RandomGenerator.nextInt (1, maxSize);
         int ncols = RandomGenerator.nextInt (1, maxSize);
         SparseBlockMatrix S =
            createRandomBlockMatrix (nrows, ncols, Math.min(10, maxSize/2));
         if (RandomGenerator.get().nextBoolean()) {
            S.setVerticallyLinked(true);
         }
         SparseBlockSignature sig = S.getSignature(/*vertical=*/true);
         if (!S.signatureEquals (sig)) {
             throw new TestException (
               "signature test fails on same matrix");
         }
         if (prevS != null) {
            if (S.isVerticallyLinked() != prevS.isVerticallyLinked() ||
                !S.blockStructureEquals (prevS)) {
               if (prevSig.equals (sig)) {
                  throw new TestException (
                     "signature equals that of previous matrix");
               }
               if (prevS.signatureEquals (sig)) {
                  throw new TestException (
                     "signature test equals that of previous matrix");
               }
            }
         }
         SparseBlockMatrix C = S.clone();
         SparseBlockSignature chk = C.getSignature(/*vertical=*/true);
         if (!sig.equals (chk)) {
            throw new TestException (
               "signature not equal to that of matrix clone");
         }
         prevSig = sig;         
         prevS = S;

         int[] order = sig.getOrderedEntries();
         sig.verifyLexicalMergeSort (order);
      }
   }

   VectorNi computePrevIdxs (SparseBlockMatrix S, SparseBlockMatrix P) {
      SparseBlockSignature sig = S.getSignature(/*vertical=*/true);
      SparseBlockSignature sigPrev = P.getSignature(/*vertucal=*/true);
      return new VectorNi (sig.computePrevColIdxs(sigPrev));
   }

   void checkPrevIdxs (
      SparseBlockMatrix S, SparseBlockMatrix P, int... idxs) {
      VectorNi prevIdxs = computePrevIdxs (S, P);
      VectorNi chkIdxs = new VectorNi (idxs);
      checkEquals ("prevIdxs", prevIdxs, chkIdxs);
   }

   void checkPrevIdxs (
      SparseBlockMatrix S, SparseBlockMatrix P) {

      SparseBlockSignature sigS = S.getSignature (/*vertical=*/true);
      SparseBlockSignature sigP = P.getSignature (/*vertical=*/true);
      VectorNi prevIdxs = new VectorNi(sigS.computePrevColIdxs (sigP));
      VectorNi chkIdxs = new VectorNi(sigS.computePrevColIdxsAlt (sigP));
      System.out.println ("check: " + chkIdxs);
      System.out.println ("previ: " + prevIdxs);
      checkEquals ("prevIdxs:", prevIdxs, chkIdxs);
   }
   
   public void testPrevIdxsSpecial() {
      int[] rowSizes = new int[] {3, 3, 3};
      SparseBlockMatrix S, P;
      
      S = new SparseBlockMatrix (rowSizes, new int[0]);
      P = new SparseBlockMatrix (rowSizes, new int[0]);
      S.setVerticallyLinked(true);
      P.setVerticallyLinked(true);

      // self indexing, block size = 1
      S.addBlock (0, 0, new Matrix3x1Block());
      S.addBlock (1, 1, new Matrix3x1Block());
      S.addBlock (2, 2, new Matrix3x1Block());
      S.addBlock (3, 3, new Matrix3x1Block());
      
      checkPrevIdxs (S, S, 0, 1, 2, 3);

      // permutation, block size = 1
      
      P.addBlock (1, 0, new Matrix3x1Block());
      P.addBlock (3, 1, new Matrix3x1Block());
      P.addBlock (0, 2, new Matrix3x1Block());
      P.addBlock (2, 3, new Matrix3x1Block());
      
      checkPrevIdxs (S, P, 2, 0, 3, 1);
      checkPrevIdxs (P, S, 1, 3, 0, 2);

      // columns added or removed, block size = 1
      P = new SparseBlockMatrix (rowSizes, new int[0]);
      P.setVerticallyLinked(true);

      P.addBlock (3, 0, new Matrix3x1Block());
      P.addBlock (0, 1, new Matrix3x1Block());

      checkPrevIdxs (S, P, 1, -1, -1, 0);
      checkPrevIdxs (P, S, 3, 0);

      // self indexing, block size = 2
      
      S = new SparseBlockMatrix (rowSizes, new int[0]);
      S.setVerticallyLinked(true);

      S.addBlock (0, 0, new Matrix3x2Block());
      S.addBlock (1, 1, new Matrix3x2Block());
      S.addBlock (2, 2, new Matrix3x2Block());
      S.addBlock (3, 3, new Matrix3x2Block());
      
      checkPrevIdxs (S, S, 0, 1, 2, 3, 4, 5, 6, 7);

      // permutation, block size = 2

      P = new SparseBlockMatrix (rowSizes, new int[0]);
      P.setVerticallyLinked(true);
      P.addBlock (1, 0, new Matrix3x2Block());
      P.addBlock (3, 1, new Matrix3x2Block());
      P.addBlock (0, 2, new Matrix3x2Block());
      P.addBlock (2, 3, new Matrix3x2Block());
      
      checkPrevIdxs (S, P, 4, 5, 0, 1, 6, 7, 2, 3);
      checkPrevIdxs (P, S, 2, 3, 6, 7, 0, 1, 4, 5);

      // columns added or removed, block size = 2
      P = new SparseBlockMatrix (rowSizes, new int[0]);
      P.setVerticallyLinked(true);

      P.addBlock (3, 0, new Matrix3x2Block());
      P.addBlock (0, 1, new Matrix3x2Block());

      checkPrevIdxs (S, P, 2, 3, -1, -1, -1, -1, 0, 1);
      checkPrevIdxs (P, S, 6, 7, 0, 1);

      // mixed block sizes
      S = new SparseBlockMatrix (rowSizes, new int[0]);
      S.setVerticallyLinked(true);

      S.addBlock (0, 0, new Matrix3x3Block());
      S.addBlock (1, 1, new Matrix3x3Block());

      P = new SparseBlockMatrix (rowSizes, new int[0]);
      P.setVerticallyLinked(true);

      P.addBlock (0, 0, new Matrix3x1Block());
      P.addBlock (0, 1, new Matrix3x2Block());
      P.addBlock (1, 2, new Matrix3x2Block());
      P.addBlock (1, 3, new Matrix3x1Block());

      checkPrevIdxs (S, P, 0, 1, 2, 3, 4, 5);
      checkPrevIdxs (P, S, 0, 1, 2, 3, 4, 5);

      // mixed block sizes, permuted

      S = new SparseBlockMatrix (rowSizes, new int[0]);
      S.setVerticallyLinked(true);

      S.addBlock (0, 0, new Matrix3x3Block());
      S.addBlock (1, 1, new Matrix3x3Block());
      S.addBlock (2, 2, new Matrix3x3Block());

      P = new SparseBlockMatrix (rowSizes, new int[0]);
      P.setVerticallyLinked(true);

      P.addBlock (2, 0, new Matrix3x1Block());
      P.addBlock (1, 1, new Matrix3x2Block());
      P.addBlock (0, 2, new Matrix3x1Block());
      P.addBlock (2, 3, new Matrix3x2Block());
      P.addBlock (0, 4, new Matrix3x2Block());
      P.addBlock (1, 5, new Matrix3x1Block());

      checkPrevIdxs (S, P, 3, 6, 7, 1, 2, 8, 0, 4, 5);
      checkPrevIdxs (P, S, 6, 3, 4, 0, 7, 8, 1, 2, 5);

      // mixed block sizes, with missing blocks

      P = new SparseBlockMatrix (rowSizes, new int[0]);
      P.setVerticallyLinked(true);

      P.addBlock (2, 0, new Matrix3x1Block());
      P.addBlock (1, 1, new Matrix3x2Block());
      P.addBlock (0, 2, new Matrix3x1Block());
      P.addBlock (2, 3, new Matrix3x1Block());

      checkPrevIdxs (S, P, 3, -1, -1, 1, 2, -1, 0, 4, -1);
      checkPrevIdxs (P, S, 6, 3, 4, 0, 7);
   }

   public void testPrevIdxsGeneral() {
      int numBlkRows = 10;
      int[] rowSizes = new int[numBlkRows];
      for (int i=0; i<numBlkRows; i++) {
         rowSizes[i] = 3;
      }
      int numBlockPatterns = 8; // fewer block patterns means more overlap
      ArrayList<int[]> blockPatterns = new ArrayList<>();
      for (int i=0; i<numBlockPatterns; i++) {
         int[] pattern;
         do {
            pattern = RandomGenerator.randomSubsequence (numBlkRows);
         }
         while (pattern.length == 0);
         blockPatterns.add (pattern);         
      }
      SparseBlockMatrix S, P;

      int ntests = 1000;
      for (int cnt=0; cnt<ntests; cnt++) {
         S = new SparseBlockMatrix (rowSizes, new int[0]);
         P = new SparseBlockMatrix (rowSizes, new int[0]);

         int maxcols = 10;

         int ncols = RandomGenerator.nextInt (maxcols/2, maxcols);
         for (int bj=0; bj<ncols; bj++) {
            int[] pattern =
               blockPatterns.get(RandomGenerator.nextInt(numBlockPatterns));
            int colSize = RandomGenerator.nextInt (1,4);
            for (int bi : pattern) {
               S.addBlock (bi, bj, MatrixBlockBase.alloc (3, colSize));
            }
         }

         ncols = RandomGenerator.nextInt (maxcols/2, maxcols);
         for (int bj=0; bj<ncols; bj++) {
            int[] pattern =
               blockPatterns.get(RandomGenerator.nextInt(numBlockPatterns));
            int colSize = RandomGenerator.nextInt (1,4);
            for (int bi : pattern) {
               P.addBlock (bi, bj, MatrixBlockBase.alloc (3, colSize));
            }
         }

         checkPrevIdxs (S, P);
         checkPrevIdxs (P, S);
      }
   }      

   public static void main (String[] args) {
      SparseBlockSignatureTest tester = new SparseBlockSignatureTest();

      RandomGenerator.setSeed (0x1234);
      boolean check = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-check")) {
            check = true;
         }
         else {
            System.out.println (
               "Usage: maspack.util.SparseSignatureTest [-check]");
            System.exit(1);
         }
      }
      if (check) {
         tester.check();
      }
      else {
         tester.runtest();
      }
   }
   
}
