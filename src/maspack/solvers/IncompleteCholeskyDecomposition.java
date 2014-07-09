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
public class IncompleteCholeskyDecomposition implements LinearTransformNd {
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

   public SparseMatrixNd C = null;
   int n;

   /**
    * Creates an uninitialized CholeskyDecomposition.
    */
   public IncompleteCholeskyDecomposition() {
   }

   /**
    * Creates a CholeskyDecomposition for the Matrix specified by M.
    * 
    * @param M
    * matrix to perform the Cholesky decomposition on
    * @throws ImproperSizeException
    * if M is not square
    */
   public IncompleteCholeskyDecomposition (SparseMatrixNd M)
   throws ImproperSizeException {
      factor (M);
   }

   /**
    * Peforms an Cholesky decomposition on the Matrix M.
    * 
    * @param A
    * matrix on which to perform the Cholesky decomposition
    * @throws ImproperSizeException
    * if M is not square
    * @throws IllegalArgumentException
    * if M is detected to be not symmetric positive definite
    */
   public void factor (SparseMatrixNd A) {
      // int n = A.rowSize();
      // L = new SparseMatrixNd(n, n);
      //        
      // for(int i = 0; i < n; i++)
      // {
      // double v;
      //            
      // SparseMatrixCell icell = A.rows[i];
      //            
      // SparseMatrixCell tolcell = icell;
      // double inorm = 0;
      // while(tolcell != null)
      // {
      // inorm += tolcell.value * tolcell.value;
      // tolcell = tolcell.next;
      // }
      // inorm = Math.sqrt(inorm);
      //            
      // //compute the diagonal offset parts of the row
      // while(icell.j < i || icell != null)
      // {
      // v = icell.value;
      // // System.out.println("initial value " + v);
      //
      // SparseMatrixCell ikcell = L.rows[i];
      // SparseMatrixCell jkcell = L.rows[icell.j];
      //                
      // while(ikcell != null && ikcell.j < icell.j && jkcell.j < icell.j)
      // {
      //                    
      // if(ikcell.j == jkcell.j)
      // {
      // v -= ikcell.value * jkcell.value;
      // // System.out.println("multiplying values " + ikcell.value + " " +
      // jkcell.value);
      // }
      // if(ikcell.j < jkcell.j)
      // {
      // ikcell = ikcell.next;
      // }
      // else
      // {
      // jkcell = jkcell.next;
      // }
      // }
      //                
      // while(jkcell.j != icell.j)
      // jkcell = jkcell.next;
      //                
      // v /= jkcell.value;
      //                
      // L.set(i, icell.j, v);
      // icell = icell.next;
      // }
      //            
      // //compute diagonal
      // v = icell.value;
      // SparseMatrixCell ikcell = L.rows[i];
      // while(ikcell != null && ikcell.j < i)
      // {
      // v -= ikcell.value * ikcell.value;
      // ikcell = ikcell.next;
      // }
      //            
      // if(v <= 0)
      // v = inorm * 0.001;//A.get(i, i);
      //            
      // v = Math.sqrt(v);
      //            
      // if(Double.isNaN(v) || v == 0)
      // {
      // System.out.println("NaN created");
      // System.out.println(v);
      // }
      //            
      // L.set(i, i, v);
      // }

      n = A.rowSize();
      C = new SparseMatrixNd (A);

      for (int i = 0; i < n; i++) {
         SparseMatrixCell cellij = C.getRow(i);
         while (cellij != null && cellij.j < i) {
            double vij = cellij.value;
            int j = cellij.j;

            SparseMatrixCell cellik = C.getRow(i);
            SparseMatrixCell celljk = C.getRow(j);

            while (cellik != null && cellik.j < cellij.j && celljk != null &&
                   celljk.j < cellij.j) {
               if (cellik.j == celljk.j) {
                  vij -= cellik.value * celljk.value;
               }

               if (cellik.j < celljk.j) {
                  cellik = cellik.next;
               }
               else {
                  celljk = celljk.next;
               }
            }

            while (celljk.j != j) {
               celljk = celljk.next;
            }
            vij /= celljk.value;

            if (Double.isNaN (vij)) {
               System.out.println ("NaN " + celljk.value);
            }

            cellij.value = vij;
            cellij = cellij.next;
         }

         double vii = cellij.value;

         SparseMatrixCell cellik = C.getRow(i);
         while (cellik != null && cellik.j < i) {
            vii -= cellik.value * cellik.value;
            cellik = cellik.next;
         }

         if (vii <= 0) {
            vii = cellij.value;
         }
         System.out.println (vii);
         vii = Math.sqrt (vii);
         cellij.value = vii;

         SparseMatrixCell cellii = cellij;

         cellij = cellij.next;
         while (cellij != null) {
            C.removeEntry (cellij, cellii, null);
            cellij = cellij.next;
         }
      }
   }

   public void factor (SparseMatrixNd A, double tol) {
      n = A.rowSize();

      // modified ILUT as per matlab manual

      // L.set(A);
      //        
      // //slow but functional version
      // VectorNd vec = new VectorNd(n);
      // for(int i = 0; i < n; i++)
      // {
      // L.getRow(i, vec);
      // double toli = vec.norm() * tol;
      // for(int k = 0; k < i; k++)
      // {
      // double aik = vec.get(k) / L.get(k,k);
      // vec.set(k, 0);
      // if(Math.abs(aik) > toli)
      // {
      // for(int j = k+1; j < n; j++)
      // {
      // double aij = vec.get(j) - aik * L.get(k, j);
      // vec.set(j, aij);
      // }
      // }
      // }
      //            
      // for(int j = 0; j < n; j++)
      // {
      // if(Math.abs(vec.get(j)) < toli)
      // {
      // vec.set(j, 0);
      // }
      // }
      //            
      // L.setRow(i, vec);
      // }
      //        
      // SparseMatrixNd goodL = new SparseMatrixNd(L);

      // //update L to get incomplete cholesky
      // VectorNd vec = new VectorNd(n);
      // for(int i = 0; i < n; i++)
      // {
      // L.getRow(i, vec);
      // double diag = vec.get(i);
      // System.out.println(diag);
      // vec.scale(1.0/Math.sqrt(diag));
      // L.setRow(i, vec);
      // }

      C = new SparseMatrixNd (A);

      for (int i = 0; i < n; i++) {
         SparseMatrixCell cellik = C.getRow(i);

         double itol = 0;
         while (cellik != null) {
            itol += cellik.value * cellik.value;
            cellik = cellik.next;
         }
         itol = Math.sqrt (itol) * tol;

         // System.out.println("first itol " + itol);

         cellik = C.getRow(i);

         while (cellik != null && cellik.j < i) {
            int k = cellik.j;

            double aik = cellik.value / A.get (k, k);

            if (Math.abs (aik) > itol) {
               SparseMatrixCell cellkj = C.getRow(k);
               while (cellkj != null && cellkj.j <= k)
                  cellkj = cellkj.next;

               SparseMatrixCell cellij = cellik;

               while (cellkj != null) {
                  int j = cellkj.j;

                  while (cellij != null && cellij.j < j &&
                         cellij.next != null && cellij.next.j <= j)
                     cellij = cellij.next;

                  double diff = aik * cellkj.value;

                  if (cellij != null && cellij.j == j) {
                     cellij.value -= diff;
                  }
                  else {
                     SparseMatrixCell cellnew =
                        new SparseMatrixCell (i, j, -diff);
                     C.addEntry (cellnew, cellij, C.prevColEntry (i, j));
                  }

                  cellkj = cellkj.next;
               }
            }

            C.removeEntry (cellik, null, i == 0 ? null : C.getRow(k));
            cellik = cellik.next;
            // L.set(i, k, 0);
         }

         int maxj = i;
         double max = 0;

         cellik = C.getRow(i);
         itol = 0;
         while (cellik != null) {
            double tmp = Math.abs (cellik.value);
            if (tmp > max) {
               maxj = cellik.j;
               max = tmp;
            }

            itol += cellik.value * cellik.value;
            cellik = cellik.next;
         }
         itol = Math.sqrt (itol) * tol;

         // System.out.println(maxj);
         // System.out.println("second itol " + itol);

         SparseMatrixCell lastcellik = C.getRow(i);
         // if(lastcellik.value <= 0)
         // lastcellik.value = Math.sqrt(itol);
         cellik = lastcellik.next;
         while (cellik != null) {
            if (Math.abs (cellik.value) < itol && cellik.j != maxj) {
               C.removeEntry (cellik, lastcellik, C.prevColEntry (i, cellik.j));
            }
            else {
               lastcellik = cellik;
            }
            cellik = cellik.next;
         }
      }

      // if(!L.epsilonEquals(goodL, 1e-6))
      // {
      // System.out.println(new MatrixNd(L));
      // // System.out.println(new MatrixNd(goodL));
      //            
      // System.out.println("matrix different");
      //            
      // System.exit(0);
      // }

      for (int i = 0; i < n; i++) {
         SparseMatrixCell cellij = C.getRow(i);

         double diagdiv = 1.0 / Math.sqrt (cellij.value);

         while (cellij != null) {
            cellij.value *= diagdiv;
            cellij = cellij.next;
         }
      }

      C.transpose();

      // System.out.println("Cholesky L");
      // System.out.println(new MatrixNd(this.L));
   }

   // public void factorDense(SparseMatrixNd M) throws ImproperSizeException
   // {
   // setSize(M.rowSize());
   //        
   // for(int i = 0; i < n; i++)
   // {
   // double v;
   //            
   // //compute the diagonal offset parts of the row
   // for(int j = 0; j < i; j++)
   // {
   // v = M.get(i, j);
   // if(v != 0)
   // {
   // for(int k = 0; k < j; k++)
   // {
   // v -= L.get(i, k) * L.get(j, k);
   // }
   //                    
   // v /= L.get(j, j);
   //                    
   // L.set(i, j, v);
   // }
   // }
   //            
   // //compute diagonal
   // v = M.get(i, i);
   // for(int k = 0; k < i; k++)
   // {
   // double tmp = L.get(i, k);
   // v -= tmp * tmp;
   // }
   // v = Math.sqrt(v);
   // L.set(i, i, v);
   // }
   // }

   /**
    * Gets the lower-triangular matrix L associated with the Cholesky
    * decomposition.
    * 
    * @param L
    * lower triangular matrix
    * @throws ImproperStateException
    * if this CholeskyDecomposition is uninitialized
    * @throws ImproperSizeException
    * if L is not of the proper dimension and cannot be resized
    */
   public void get (MatrixNd L) {
      L.set (this.C);
   }

   /**
    * Solves L * x = b
    */
   public boolean solve (VectorNd x, VectorNd b)
      throws ImproperStateException, ImproperSizeException {
      double[] xbuf = x.getBuffer();
      double[] bbuf = b.getBuffer();
      for (int i = 0; i < n; i++) {
         double v = bbuf[i];

         SparseMatrixCell ijcell = C.getRow(i);
         while (ijcell.j < i) {
            v -= xbuf[ijcell.j] * ijcell.value;
            ijcell = ijcell.next;
         }

         v /= C.get (i, i);

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
   //            
   // v /= L.get(i, i);
   //            
   // x.set(i, v);
   // }
   //        
   // return true;
   // }

   /**
    * Solves L' * x = b
    */
   public boolean solveTranspose (VectorNd x, VectorNd b)
      throws ImproperStateException, ImproperSizeException {
      double[] xbuf = x.getBuffer();
      double[] bbuf = b.getBuffer();
      for (int i = (n - 1); i >= 0; i--) {
         double v = bbuf[i];

         SparseMatrixCell jicell = C.getCol(i);
         jicell = jicell.down;
         while (jicell != null) {
            v -= xbuf[jicell.i] * jicell.value;
            jicell = jicell.down;
         }

         v /= C.get (i, i);

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
      return C.colSize();
   }

   public int rowSize() {
      // TODO Auto-generated method stub
      return C.rowSize();
   }

   public void mul (VectorNd vr, VectorNd v1) {
      solve (vr, v1);
      solveTranspose (vr, vr);
   }
}
