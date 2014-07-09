/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.ImproperSizeException;
import maspack.matrix.ImproperStateException;
import maspack.matrix.LinearTransformNd;
import maspack.matrix.MatrixNd;
import maspack.matrix.SparseMatrixCell;
import maspack.matrix.SparseMatrixNd;
import maspack.matrix.SparseVectorCell;
import maspack.matrix.SparseVectorNd;
import maspack.matrix.VectorNd;

/**
 * Constructs the Cholesky decomposition of a symmetric positive definite
 * matrix. This takes the form <br>
 * M = L L' <br>
 * where M is the original matrix, L is a lower-triangular matrix and L' denotes
 * its transpose. Once this decomposition has been constructed, it can be used
 * to perform various computations related to M, such as solving equations,
 * computing the determinant, or estimating the condition number.
 * 
 * <p>
 * Providing a separate class for the Cholesky decomposition allows an
 * application to perform such decompositions repeatedly without having to
 * reallocate temporary storage space.
 */
public class IncompleteLUDecomposition implements LinearTransformNd {
   // private double[] buf;
   // private double[] sol;
   // private int n;
   // private boolean initialized = false;
   //
   // // these are used only for condition number estimation
   // private double[] pvec = new double[0];
   // private double[] pneg = new double[0];
   // private double[] ppos = new double[0];
   // private double[] yvec = new double[0];

   public SparseMatrixNd LU = null;
   public SparseMatrixNd L = null;
   public SparseMatrixNd U = null;
   int n;

   /**
    * Creates an uninitialized CholeskyDecomposition.
    */
   public IncompleteLUDecomposition() {
   }

   /**
    * Creates a CholeskyDecomposition for the Matrix specified by M.
    * 
    * @param M
    * matrix to perform the Cholesky decomposition on
    * @throws ImproperSizeException
    * if M is not square
    */
   public IncompleteLUDecomposition (SparseMatrixNd M)
   throws ImproperSizeException {
      factor (M);
   }

   /**
    * Peforms an Cholesky decomposition on the Matrix M.
    * 
    * @param A
    * matrix on which to perform the Cholesky decomposition
    */
   public void factor (SparseMatrixNd A) {
      n = A.rowSize();
      LU = new SparseMatrixNd (A);

      for (int k = 0; k < n; k++) {
         SparseMatrixCell cellik = LU.getCol(k);

         while (cellik != null && cellik.i < k)
            cellik = cellik.down;

         double akk = cellik.value;
         cellik = cellik.down;

         while (cellik != null) {
            int i = cellik.i;
            cellik.value /= akk;
            double aik = cellik.value;

            SparseMatrixCell cellij = LU.getRow(i);
            SparseMatrixCell cellkj = LU.getRow(k);

            while (cellij != null && cellij.j <= k)
               cellij = cellij.next;

            while (cellkj != null && cellkj.j <= k)
               cellkj = cellkj.next;

            while (cellij != null && cellkj != null) {
               if (cellij.j == cellkj.j)
                  cellij.value -= aik * cellkj.value;

               if (cellij.j < cellkj.j)
                  cellij = cellij.next;
               else
                  cellkj = cellkj.next;
            }

            cellik = cellik.down;
         }
      }
   }

   public void factor (SparseMatrixNd A, double droptol) {
      n = A.rowSize();
      L = new SparseMatrixNd (n, n);
      U = new SparseMatrixNd (n, n);

      for (int i = 0; i < n; i++) {
         SparseVectorNd w = new SparseVectorNd (n);

         SparseVectorCell cellprev = null;
         SparseMatrixCell cellik = A.getRow(i);
         double itol = 0;
         while (cellik != null) {
            SparseVectorCell wk = new SparseVectorCell (cellik.j, cellik.value);
            w.addEntry (wk, cellprev);
            cellprev = wk;
            // w.set(cellik.j, cellik.value);

            itol += cellik.value * cellik.value;
            cellik = cellik.next;
         }
         itol = Math.sqrt (itol) * droptol;

         // cellik = A.rows[i];
         // while(cellik != null)
         // {
         // System.out.println(cellik.j + " " + cellik.value + " " +
         // w.get(cellik.j));
         // cellik = cellik.next;
         // }

         cellprev = null;
         SparseVectorCell wk = w.getCells();

         while (wk != null && wk.i < i) {
            int k = wk.i;

            wk.value = wk.value / U.get (k, k);

            if (Math.abs (wk.value) > itol) {
               SparseMatrixCell uk = U.getRow(k);
               while (uk != null && uk.j <= k)
                  uk = uk.next;

               // SparseVectorCell wj = wk;

               while (uk != null) {
                  // while(wj != null && wj.i < uk.j && wj.next != null &&
                  // wj.next.i <= uk.j)
                  // wj = wj.next;
                  //                        
                  // if(wj.i == uk.j)
                  // wj.value = wj.value - wk.value * uk.value;
                  // else
                  // {
                  // w.set(uk.j, -wk.value * uk.value);
                  // w.addEntry(new SparseVectorCell(uk.j, -wk.value *
                  // uk.value), wj);
                  // }

                  double newval = w.get (uk.j) - wk.value * uk.value;
                  w.set (uk.j, newval);
                  // System.out.println(newval);

                  uk = uk.next;
               }

               cellprev = wk;
               wk = wk.next();
            }
            else {
               // System.out.println("dropping " + itol);

               w.set (wk.i, 0);

               // w.removeEntry(wk, cellprev);
               if (cellprev != null)
                  wk = cellprev.next();
               else
                  wk = w.getCells();
            }
         }

         cellprev = null;

         SparseMatrixCell mcellprev = null;

         wk = w.getCells();
         while (wk != null) {
            if (Math.abs (wk.value) > itol || wk.i == i) {
               SparseMatrixCell newcell =
                  new SparseMatrixCell (i, wk.i, wk.value);

               if (wk.i == i)
                  mcellprev = null;

               if (wk.i < i) {
                  // L.set(i, wk.i, wk.value);
                  L.addEntry (newcell, mcellprev, L.prevColEntry (i, wk.i));
               }
               else {
                  // U.set(i, wk.i, wk.value);
                  U.addEntry (newcell, mcellprev, U.prevColEntry (i, wk.i));
               }

               mcellprev = newcell;
            }

            wk = wk.next();
         }

         L.set (i, i, 1.0);
      }
   }

   // public void factor(SparseMatrixNd A, double tol)
   // {
   // n = A.rowSize();
   //        
   // //ILUT
   //        
   // L = new SparseMatrixNd(A);
   //        
   // for(int i = 0; i < n; i++)
   // {
   // SparseMatrixCell cellik = L.rows[i];
   //            
   // double itol = 0;
   // while(cellik != null)
   // {
   // itol += cellik.value * cellik.value;
   // cellik = cellik.next;
   // }
   // itol = Math.sqrt(itol) * tol;
   //            
   // // System.out.println("first itol " + itol);
   //            
   // cellik = L.rows[i];
   //            
   // while(cellik != null && cellik.j < i)
   // {
   // int k = cellik.j;
   //                 
   // double aik = cellik.value / L.get(k, k);
   //                
   // if(Math.abs(aik) > itol)
   // {
   // SparseMatrixCell cellkj = L.rows[k];
   // while(cellkj != null && cellkj.j <= k)
   // cellkj = cellkj.next;
   //                    
   // SparseMatrixCell cellij = cellik;
   //                    
   // while(cellkj != null)
   // {
   // int j = cellkj.j;
   //                        
   // while(cellij != null && cellij.j < j && cellij.next != null &&
   // cellij.next.j <= j)
   // cellij = cellij.next;
   //                        
   // double diff = aik * cellkj.value;
   //                        
   // if(cellij != null && cellij.j == j)
   // {
   // cellij.value -= diff;
   // }
   // else
   // {
   // SparseMatrixCell cellnew = new SparseMatrixCell(i, j, -diff);
   // L.addEntry(cellnew, cellij, L.prevColEntry(i, j));
   // }
   //                        
   // cellkj = cellkj.next;
   // }
   // }
   //
   // L.removeEntry(cellik, null, i==0?null:L.rows[k]);
   // cellik = cellik.next;
   // // L.set(i, k, 0);
   // }
   //            
   // int maxj = i;
   // double max = 0;
   //            
   // cellik = L.rows[i];
   // itol = 0;
   // while(cellik != null)
   // {
   // double tmp = Math.abs(cellik.value);
   // if(tmp > max)
   // {
   // maxj = cellik.j;
   // max = tmp;
   // }
   //                
   // itol += cellik.value * cellik.value;
   // cellik = cellik.next;
   // }
   // itol = Math.sqrt(itol) * tol;
   //            
   // // System.out.println(maxj);
   // // System.out.println("second itol " + itol);
   //            
   // SparseMatrixCell lastcellik = L.rows[i];
   // // if(lastcellik.value <= 0)
   // // lastcellik.value = Math.sqrt(itol);
   // cellik = lastcellik.next;
   // while(cellik != null)
   // {
   // if(Math.abs(cellik.value) < itol && cellik.j != maxj)
   // {
   // L.removeEntry(cellik, lastcellik, L.prevColEntry(i, cellik.j));
   // }
   // else
   // {
   // lastcellik = cellik;
   // }
   // cellik = cellik.next;
   // }
   // }
   // }

   /**
    * Gets the lower and upper triangular matrices L and U associated with this
    * decomposition.
    * 
    * @param LU
    * lower and upper triangular matrices (sharing the same space)
    * @throws ImproperStateException
    * if this decomposition is uninitialized
    * @throws ImproperSizeException
    * if LU is not of the proper dimension and cannot be resized
    */
   public void get (MatrixNd LU) {
      LU.set (this.LU);
   }

   /**
    * Solves L * x = b
    */
   public boolean solveL (VectorNd x, VectorNd b)
      throws ImproperStateException, ImproperSizeException {
      double[] xbuf = x.getBuffer();
      double[] bbuf = b.getBuffer();
      for (int i = 0; i < n; i++) {
         double v = bbuf[i];

         SparseMatrixCell ijcell = L.getRow(i);
         while (ijcell.j < i) {
            v -= xbuf[ijcell.j] * ijcell.value;
            ijcell = ijcell.next;
         }

         // v /= L.get(i, i);

         xbuf[i] = v;
      }

      return true;
   }

   // public boolean solveDense(Vector x, Vector b) throws
   // ImproperStateException, ImproperSizeException
   // {
   // for(int i = 0; i < n; i++)
   // {
   // double v = b.get(i);
   //            
   // for(int j = 0; j < i; j++)
   // {
   // v -= x.get(j) * L.get(i, j);
   // }
   // cols
   // v /= L.get(i, i);
   //            
   // x.set(i, v);
   // }
   //        
   // return true;
   // }

   /**
    * Solves U * x = b
    */
   public boolean solveU (VectorNd x, VectorNd b)
      throws ImproperStateException, ImproperSizeException {
      double[] xbuf = x.getBuffer();
      double[] bbuf = b.getBuffer();
      for (int i = (n - 1); i >= 0; i--) {
         double v = bbuf[i];

         SparseMatrixCell jicell = U.getRow(i);

         while (jicell != null && jicell.j <= i)
            jicell = jicell.next;

         while (jicell != null) {
            v -= xbuf[jicell.j] * jicell.value;
            jicell = jicell.next;
         }

         v /= U.get (i, i);

         xbuf[i] = v;
      }

      return true;
   }

   // public boolean solveTransposeDense(Vector x, Vector b) throws
   // ImproperStateException, ImproperSizeException
   // {
   // System.out.println("solveTransposeDense");
   // for(int i = (n-1); i >= 0; i--)
   // {
   // double v = b.get(i);
   //            
   // for(int j = (i+1); j < n; j++)
   // {
   // v -= x.get(j) * L.get(j, i);
   // }
   //            
   // v /= L.get(i, i);
   //            
   // x.set(i, v);
   // }
   //        
   // return true;
   // }

   public int colSize() {
      // TODO Auto-generated method stub
      return LU.colSize();
   }

   public int rowSize() {
      // TODO Auto-generated method stub
      return LU.rowSize();
   }

   public void mul (VectorNd vr, VectorNd v1) {
      solveL (vr, v1);
      solveU (vr, vr);
   }
}
