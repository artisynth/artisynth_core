/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matlab;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import maspack.matrix.Matrix;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.matrix.Matrix.Partition;
import maspack.solvers.DirectSolver;
import maspack.util.IndentingPrintWriter;

public class MatlabSolver implements DirectSolver
{
   boolean debug = false;
   private MatlabInterface matlab;

   Matrix matrix = null;
   int A = 0;
   int[] factorization = new int[] { 0 };
   int numVals, subsetSize;
   double[] rowIdxs, colIdxs;
   double[] vals;
   double[] I, J;

   public MatlabSolver() 
   {
      try
      {
	 matlab = new MatlabInterface();
	 matlab.open();
      }
      catch (UnsupportedOperationException e)
      {
//         System.out.println("java.library.path = \n" + System.getProperty("java.library.path"));
//	 System.out.println("MatlabSolver: unable to open interface.");
	 System.out.println(e.getMessage());
	 if (debug) {System.err.println(e.getMessage());}
	 throw e; 
      }
   }
   
   public void test()
   {
      try
      {
	 double[] I = new double[]{1,2,3,4,5};
//	 int[] I = new int[]{1,2,3,4,5};
	 double[] S = new double[]{3.3, 4.5, 6.6, 1.0, 9.0};
	 matlab.setSparse(I, I, S);
	 matlab.evalString("inv(A)");
	 
	 System.out.println(matlab.getOutputString(500));

      }
      catch (Exception e)
      {
	 System.out.println("MatlabInterface Test failed.");
	 System.err.println(e.getMessage());
      }
   }
   
   private void free()
   {
      matlab.clearWorkspace();
   }

   public void analyze(Matrix M, int size, int type)
   {
      free();
      boolean typedebug = true;
      if (M instanceof maspack.matrix.SparseBlockMatrix)
      {
	 if (typedebug) {System.out.println("analyse() found sparse matrix");}
	 numVals = ((SparseBlockMatrix) M).numNonZeroVals(
            Partition.Full, size, size);
      }
      else
      {
	 if (typedebug) {System.out.println("analyse() found dense matrix");}
	 numVals = size * size;
      }
      
      subsetSize = size;
      matrix = M;
      
      int[] cols = new int[numVals];
      int[] rowOffs = new int[size+1]; 
      M.getCRSIndices(cols, rowOffs, Partition.Full, size, size);
            
      int rows[] = new int[cols.length];
      getRowIndices(cols, rowOffs, rows);
      
      rowIdxs = new double[rows.length];
      int2double(rows, rowIdxs);
      //incIndices(rowIdxs); 

      colIdxs = new double[cols.length];
      int2double(cols, colIdxs);
      //incIndices(colIdxs);
      
      vals = new double[numVals];
      
      try
      { 
	 matlab.setSparseIndices(rowIdxs, colIdxs);
      }
      catch (Exception e)
      { throw new IllegalArgumentException(
	       "Matrix indices could not be set");
      }
      
      if (debug)
      {
	 System.out.println("\n\nMatlabSolver.analyze():");
//	 vals = new double[numVals];
//	 M.getCRSValues(vals, 
//		  SparseBlockMatrix.Partition.Full, subsetSize, subsetSize);
//	 printArray("vals", vals);
//	 printArray("colIdxs", cols);
//	 printArray("rowIdxs", rows);
//	 printArray("rowOffs", rowOffs);
//	 printData(rowIdxs, colIdxs, vals);
//	 try
//	 { matlab.setSparse(rowIdxs, colIdxs, vals);
//	 } catch (IOException e)
//	 { System.err.println(e.getMessage());
//	 }
// querySparseMatrix();
//	 queryFullMatrix();
      }
   }

   public void factor()
   {
      if (matrix != null)
      {
	 // vals allocated in analyze()
	 matrix.getCRSValues(vals, 
		  SparseBlockMatrix.Partition.Full, subsetSize, subsetSize);
	 
	 try {
//	    matlab.setSparse(rowIdxs, colIdxs, vals);
	    matlab.setSparseVals(vals);
	    if (debug) { 
		 System.out.println("\n\nMatlabSolver.factor():");
//		 printArray("vals", vals);
//		 printArray("colIdxs", colIdxs);
//		 printArray("rowIdxs", rowIdxs);
//		 querySparseMatrix();
//		 queryFullMatrix();
		 }
	 } catch (Exception e)
	 {
	    throw new IllegalArgumentException(
		     "Matrix could not be factored");
	 }
      }
   }
   
   public void analyzeAndFactor(Matrix M)
   {
      analyze(M, M.colSize(), 0);
      factor();
   }
   
   public void querySparseMatrix()
   {
      try{
	 matlab.evalString("A");
	 System.out.println(matlab.getOutputString(500));
      } catch (IOException e)
      {
	 System.err.println("Query for sparse matrix \"A\" failed");
	 System.err.println(e.getMessage());
      }
   }
   
   public void queryFullMatrix()
   {
      try{
	 matlab.evalString("F=full(A)");
	 System.out.println(matlab.getOutputString(500));
      } catch (IOException e)
      {
	 System.err.println("Query for sparse matrix \"F=full(A)\" failed");
	 System.err.println(e.getMessage());
      }
   }

   public void solve(VectorNd x, VectorNd b)
   {
      solve(x.getBuffer(), b.getBuffer());
   }

   public void autoFactorAndSolve (VectorNd x, VectorNd b, int tolExp)
   {
     factor();
     solve(x.getBuffer(), b.getBuffer());
   }

   double[] myX;
   double[] myB;
   
   public synchronized void solve(double[] x, double[] b)
   {
      matlab.solveSparse(x,b); // A matrix set in factor()
//      matlab.solveSparse(rowIdxs, colIdxs, vals, x, b);
      if (debug) { 
	 System.out.println("MatlabSolver.solve Ax = b");
//	 queryFullMatrix();
//	 querySparseMatrix();
//	 printArray("b", b);
//	 printArray("x", x);
	 
	 // uncomment to write data to file:
//	 myX = x;
//	 myB = b;
//	 write("solverData.txt");
      }
      

   }
   
   
   public void write(String filename)
   {
      System.out.println("writing data to file...");
      try
      {
	 String fullpath = filename;
	 System.out.println("write data to " + fullpath);
         IndentingPrintWriter pw = 
            new IndentingPrintWriter(
                     new FileWriter(fullpath));
	 writeArray(pw, "I", rowIdxs);
	 writeArray(pw, "J", colIdxs);
	 writeArray(pw, "S", vals);
	 writeArray(pw, "x", myX);
	 writeArray(pw, "b", myB);
	 pw.close();
	 System.out.println("... done writing " + filename);
      }
      catch (Exception e)
      {
	 System.out.println("Unable to open datafile for writing.");
	 e.printStackTrace();
      }
      
   }
   
   
   
   
   /*
    *  helper functions
    */

   public void writeArray(IndentingPrintWriter pw, String name, double[] ar)
   {
	 pw.print(name + " = [ ");
	 for (int i = 0; i < ar.length; i++)
	 {
	    pw.printf("%g, ",ar[i]);
	 }
	 pw.print(" ]\n");
   }
   
   public void writeArray(IndentingPrintWriter pw, String name, int[] ar)
   {
	 pw.print(name + " = [ ");
	 for (int i = 0; i < ar.length; i++)
	 {
	    pw.printf("%8.3f, ",ar[i]);
	 }
	 pw.print(" ]\n");
   }
   
   private void getRowIndices(int[] cols, int[] offs, int[] rows)
   {
      if (cols == null || offs == null || rows == null)
	 return;
      if (cols.length != rows.length)
	 return;
      int cnt = 0;
      for (int i = 0; i < cols.length; i++)
      {
	 if (i >= offs[cnt+1]-1 && cnt+1 < offs.length-1)
	    cnt++;
	 rows[i] = cnt;
      }
   }

   public void printData(double[] rows, double[] cols, double[] vals)
   {
      if (rows.length != cols.length || rows.length != vals.length)
	 return;
      for (int i = 0; i < rows.length; i++)
      {
	 System.out.printf("(%4.1f , %4.1f) = %4.2f\n", rows[i], cols[i],
		  vals[i]);
      }
   }

   public void printIdxs(double[] rows, double[] cols)
   {
      if (rows.length != cols.length)
	 return;
      for (int i = 0; i < rows.length; i++)
      {
	 System.out.printf("(%4.1f , %4.1f)\n", rows[i], cols[i]);
      }
   }

   public void printArray(String name, int[] ar)
   {
      System.out.print(name + " = ");
      printArray(ar);
   }

   public void printArray(int[] ar)
   {
      for (int i = 0; i < ar.length; i++)
      {
	 System.out.print(ar[i] + ", ");
      }
      System.out.println();
   }

   public void printArray(String name, double[] ar)
   {
      System.out.print(name + " = ");
      printArray(ar);
   }

   public void printArray(double[] ar)
   {
      for (int i = 0; i < ar.length; i++)
      {
	 System.out.print(ar[i] + ", ");
      }
      System.out.println();
   }

   public void int2double(int[] ar, double[] dar)
   {
      if (ar == null || dar == null)
	 return;
      if (dar.length != ar.length)
	 return;
      for (int i = 0; i < ar.length; i++)
      {
	 dar[i] = (double) ar[i];
      }
   }

   public void incAll(int[] ar)
   {
      for (int i = 0; i < ar.length; i++)
      {
	 ar[i] += 1;
      }
   }

   public void incIndices(double[] ar)
   {
      for (int i = 0; i < ar.length; i++)
      {
	 ar[i] += 1;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasAutoIterativeSolving()
    { 
      return false;
    }

   public void dispose()
    {
      if (matlab != null)
       { free();
         matlab = null;
       }
    }

}
