/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;

public class LeastSquaresSolver
{
   QPSolver myQPSolver;
   double myLowerBound = 0d;
   double myUpperBound = 1d;
   
   boolean debug = false;
   boolean profiling = false;
   NumberFormat fmt = new NumberFormat("%g");

   FunctionTimer solvetimer;
   FunctionTimer qptimer;
   
   int exSize = 0, rowSize = 0;
   
   public LeastSquaresSolver() 
   {
      myQPSolver = new DantzigQP();
   }
   
   public void dispose() {
      myQPSolver = null; // may need explicit dispose to be called from a single thread
   }
   
   SparseBlockMatrix mySolveMatrix;
   int mySolveMatrixType;
   
   VectorNd lb = new VectorNd(0);
   VectorNd ub = new VectorNd(0);
   
   MatrixNd Q = new  MatrixNd(0,0);
   double[][] Qbuf;
   VectorNd ff = new VectorNd(0);
   VectorNd x = new VectorNd(0);
   VectorNd x0 = new VectorNd(0);
   
   MatrixNd H = new MatrixNd(0,0);
   VectorNd b = new VectorNd(0);
   
   /**
    * Solve a bounded least squares problem as a QP
    * 
    * @param a
    * @param C
    * @param d
    * @param a0
    */
   public void solve(VectorNd a, MatrixNd C, VectorNd d, VectorNd a0) {
      if (rowSize != C.rowSize() || exSize != a.size() )
	 resizeVariables(C.rowSize(), a.size());
      
      // a and a0 may have a capacity greater than size which causes exception
      // in Dantzig solver
      x.set(a);
      x0.set(a0);

      Q.mulTransposeLeft(C, C);
      ff.mulTranspose(C, d);
      ff.negate();
      Q.get(Qbuf);

      try {
	 myQPSolver.solve(x.getBuffer(), Qbuf, ff.getBuffer(), null, null, lb
	       .getBuffer(), ub.getBuffer(), x0.getBuffer());
	 if (debug) {
            printVars (x, C, d, Q, ff, lb, ub, x0);
	 }
      } catch (Exception e) {
	 e.printStackTrace();
      }

      a.set(x); // copy result back to a
   }
   
   
   private void printVars(VectorNd x, MatrixNd C, VectorNd d, MatrixNd Q,
	 VectorNd f, VectorNd lb, VectorNd ub, VectorNd x0) {
      System.out.println("\nC = [\n" + C.toString(fmt) + "];");
      System.out.println("d = [" + d.toString(fmt) + "]';");
      System.out.println("Q = [\n" + Q.toString(fmt) + "];");
      System.out.println("f = [" + f.toString(fmt) + "]';");
      System.out.println("lb = [" + lb.toString(fmt) + "]';");
      System.out.println("ub = [" + ub.toString(fmt) + "]';");
      System.out.println("x0 = [" + x0.toString(fmt) + "]';");
      if (x == null) System.out.println("solve failed");
      else
	 System.out.println("x = [" + x.toString(fmt) + "];");
   }
   
   private void resetBounds() {
      assert lb.size() == ub.size();
      for (int i = 0; i < lb.size(); i++)
      {
         lb.set (i, myLowerBound);
         ub.set (i, myUpperBound);
      }
   }
   
   
   public void setBounds(double lower, double upper) {
      myLowerBound = lower;
      myUpperBound = upper;
      resetBounds();
   }
   
   // allows different bounds for each parameter
   // NOTE: bounds are reset if the vectors are ever resized
   public void setBounds(VectorNd lower, VectorNd upper) {
      
      // we already check that lb.size() == ub.size() when we resize
      // so we just need to ensure the supplied upper and lower are valid
      assert lower.size() == lb.size();		
      assert lower.size() == upper.size();
      
      for (int i=0; i<lb.size(); i++) {
	 	lb.set(lower);
	 	ub.set(upper);
      }
      
   }
   
   public void resizeVariables(int rowSize, int exSize) {
      this.rowSize = rowSize;
      this.exSize = exSize;
      
      Q = new MatrixNd (exSize, exSize);
      Qbuf = new double[exSize][exSize];

      
      ff= new VectorNd (exSize);
      x = new VectorNd(exSize);
      x0 = new VectorNd(exSize);
      
      lb= new VectorNd (exSize);
      ub= new VectorNd (exSize);
      resetBounds();
      
      H = new MatrixNd(rowSize, exSize);
      b = new VectorNd(rowSize);
      
   }

}
