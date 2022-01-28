package maspack.matrix;

import maspack.util.*;

public class SparseSignatureTest extends UnitTest {

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

      SparseSignature hash;
      for (int k=0; k<ntests; k++) {
         SparseBlockMatrix S =
            createRandomBlockMatrix (numBlkRows, numBlkCols, maxBlksPerRow);

         SparseSignature prev = S.getSignature();
         sigTimer.restart();
         for (int i=0; i<cnt; i++) {
            SparseSignature sig = S.getSignature();
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

   SparseSignatureTest() {
   }

   public void check() {
      System.out.println ("Time needed for signature checking:");
      check (50, 50, 5);
      check (100, 100, 10);
      check (200, 200, 10);
   }

   public void test() {
      int ntests = 10000;
      SparseSignature prevSig = null;
      SparseBlockMatrix prevS = null;
      for (int i=0; i<ntests; i++) {
         int nrows = RandomGenerator.nextInt (1, 100);
         int ncols = RandomGenerator.nextInt (1, 100);
         SparseBlockMatrix S = createRandomBlockMatrix (nrows, ncols, 10);
         if (RandomGenerator.get().nextBoolean()) {
            S.setVerticallyLinked(true);
         }
         SparseSignature sig = S.getSignature();
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
         SparseSignature chk = C.getSignature();
         if (!sig.equals (chk)) {
            throw new TestException (
               "signature not equal to that of matrix clone");
         }
         prevSig = sig;         
         prevS = S;
      }
   }

   public static void main (String[] args) {
      SparseSignatureTest tester = new SparseSignatureTest();

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
