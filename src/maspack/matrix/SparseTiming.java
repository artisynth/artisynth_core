/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.FunctionTimer;

public class SparseTiming {
   public static void main (String[] args) {
      int loopCnt;

      FunctionTimer timer = new FunctionTimer();
      for (int size = 50; size <= 200; size += 50) {
         System.out.println ("size: " + size);
         MatrixNd MR = new MatrixNd (size, size);
         MatrixNd M1 = new MatrixNd (size, size);
         MatrixNd M2 = new MatrixNd (size, size);
         VectorNd v1 = new VectorNd (size);
         VectorNd vr = new VectorNd (size);

         SparseMatrixNd SR = new SparseMatrixNd (size, size);
         SparseMatrixNd S1 = new SparseMatrixNd (size, size);
         SparseMatrixNd S2 = new SparseMatrixNd (size, size);

         loopCnt = 10;

         S1.setRandom();
         S2.setRandom();
         M1.set (S1);
         M2.set (S2);
         v1.setRandom();

         timer.start();
         for (int i = 0; i < loopCnt; i++) {
            MR.mul (M1, M2);
         }
         timer.stop();
         System.out.println ("dense matrix multiply: "+timer.result (loopCnt));

         timer.start();
         for (int i = 0; i < loopCnt; i++) {
            SR.mul (S1, S2);
         }
         timer.stop();
         System.out.println ("sparse matrix multiply: "+timer.result (loopCnt));

         loopCnt *= size;

         timer.start();
         for (int i = 0; i < loopCnt; i++) {
            vr.mul (MR, v1);
         }
         timer.stop();
         System.out.println ("dense vector multiply: "+timer.result (loopCnt));

         timer.start();
         for (int i = 0; i < loopCnt; i++) {
            vr.mul (SR, v1);
         }
         timer.stop();
         System.out.println ("sparse vector multiply: "+timer.result (loopCnt));

         if (!MR.epsilonEquals (SR, 1e-9)) {
            System.out.println ("results not equal");
         }

      }

   }
}
