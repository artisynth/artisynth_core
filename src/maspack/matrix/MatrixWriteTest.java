/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.matrix.Matrix.WriteFormat;
import java.io.*;
import maspack.util.*;

class MatrixWriteTest {
   public static void main (String[] args) {
      try {
         MatrixNd M = new MatrixNd (3, 3);
         M.set (new double[] { 1, 0, 0.5, 2.3, 4.1, 0, 0, 2, 3 });
         PrintWriter pw = new PrintWriter (System.out);
         System.out.println ("dense:");
         M.write (pw, new NumberFormat ("%8.3f"), WriteFormat.Dense);
         System.out.println ("sparse:");
         M.write (pw, new NumberFormat ("%8.3f"), WriteFormat.Sparse);
         System.out.println ("CRS:");
         M.write (pw, new NumberFormat ("%8.3f"), WriteFormat.CRS);
         System.out.println ("MatrixMarket:");
         M.write (pw, new NumberFormat ("%8.3f"), WriteFormat.MatrixMarket);
      }
      catch (Exception e) {
         e.printStackTrace();
      }

   }
}
