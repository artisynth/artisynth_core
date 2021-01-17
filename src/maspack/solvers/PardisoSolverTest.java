/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.util.*;
import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;
import maspack.solvers.PardisoSolver.ReorderMethod;

import java.io.*;
import java.util.*;

import java.awt.event.*;
import javax.swing.*;

public class PardisoSolverTest implements ActionListener {

   private int myMatrixType = Matrix.INDEFINITE;
   private Partition myPartition = Partition.Full;
   private boolean verbose = false;

   private static double EPS = 1e-13;

   // Increments the values of an integer array. We need this because
   // CRS indices were originally zero-based
   private static int[] incIndices(int[] idxs) {
      int[] newIdxs = new int[idxs.length];
      for (int i=0; i<idxs.length; i++) {
	 newIdxs[i] = idxs[i]+1;
      }
      return newIdxs;
   }

   public String getMatrixType (int code) {
      switch (code) {
         case Matrix.INDEFINITE: return "INDEFINITE";
         case Matrix.SYMMETRIC: return "SYMMETRIC";
         case Matrix.SPD: return "SPD";
         default: return "Unknown";
      }
   }

   public void setMatrixType (String typeStr) {
      if (typeStr.equals ("INDEFINITE")) {
         myMatrixType = Matrix.INDEFINITE;
         myPartition = Partition.Full;
      }
      else if (typeStr.equals ("SYMMETRIC")) {
         myMatrixType = Matrix.SYMMETRIC;
         myPartition = Partition.UpperTriangular;
      }
      else if (typeStr.equals ("SPD")) {
         myMatrixType = Matrix.SPD;
         myPartition = Partition.UpperTriangular;
      }
      else {
         throw new IllegalArgumentException (
            "Unrecognized matrix type: " + typeStr);
      }
   }

   private Random myRandom = new Random (0x1234);

   public void perturbSymmetricMatrix (SparseMatrixNd S, double eps) {
      int numVals =
         S.numNonZeroVals (Partition.UpperTriangular, S.rowSize(), S.colSize());
      double[] values = new double[numVals];
      int[] colIdxs = new int[numVals];
      int[] rowOffs = new int[S.rowSize()+1];

      S.getCRSIndices (colIdxs, rowOffs, Partition.UpperTriangular);
      S.getCRSValues (values, Matrix.Partition.UpperTriangular);

      for (int i = 0; i < numVals; i++) {
         values[i] += (myRandom.nextDouble() - 0.5) * eps;
      }
      S.setCRSValues (
         values, colIdxs, rowOffs, numVals, S.rowSize(),
         Partition.UpperTriangular);
   }

   private class TestThread extends Thread {
      public void run() {
         dotest();
      }
   }

   private class DummyThread extends Thread {
      public void run() {
         while (true) {
         }
      }
   }

   public void threadtest() {
      DummyThread dummy = new DummyThread();
      dummy.start();
      TestThread thread = new TestThread();
      thread.setPriority (Thread.NORM_PRIORITY+1);
      thread.start();
      while (thread.isAlive()) {
         try {
            Thread.sleep (10);
         }
         catch (Exception e)  {
         }
      }
   }

   public void frametest() {
      JFrame frame = new JFrame();
      JButton button = new JButton ("test");
      button.setActionCommand ("test");
      button.addActionListener (this);
      frame.getContentPane().add (button);
      frame.setVisible(true);
   }

   public void actionPerformed (ActionEvent e) {
      if (e.getActionCommand().equals ("test")) {
         threadtest();
      }
   }

   public void testFromFile (ReaderTokenizer rtok) throws IOException {

      PardisoSolver solver = new PardisoSolver();

      int size;
      int[] rowOffs;
      int[] colIdxs;
      double[] vals;
      VectorNd rhs;
      VectorNd x;
      VectorNd res;

      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         if (rtok.tokenIsWord()) {
            setMatrixType (rtok.sval);
         }
         else {
            rtok.pushBack();
         }
         size = rtok.scanInteger();
         rowOffs = new int[size+1];
         for (int i=0; i<size+1; i++) {
            rowOffs[i] = rtok.scanInteger();
         }
         int nvals = rowOffs[size]-1;
         colIdxs = new int[nvals];
         vals = new double[nvals];
         rhs = new VectorNd(size);
         res = new VectorNd(size);
         x = new VectorNd(size);
         for (int i=0; i<nvals; i++) {
            colIdxs[i] = rtok.scanInteger();
         }
         for (int i=0; i<nvals; i++) {
            vals[i] = rtok.scanNumber();
         }
         if (rtok.nextToken() == ReaderTokenizer.TT_EOF) {
            // just set the rhs to ones
            System.out.println ("testing with a rhs of ones ...");
            for (int i=0; i<size; i++) {
               rhs.set (i, 1);
            }
         }
         else {
            rtok.pushBack();
            for (int i=0; i<size; i++) {
               rhs.set (i, rtok.scanNumber());
            }
         }
         SparseMatrixCRS M = new SparseMatrixCRS(size, size);
         System.out.println (
            "solving "+getMatrixType(myMatrixType)+" matrix");
         M.setCRSValues (vals, colIdxs, rowOffs, nvals, size, myPartition);
         solver.analyze (M, size, myMatrixType);
         System.out.println (
            "numNonZerosInFactors=" + solver.getNumNonZerosInFactors());
         solver.factor ();
         solver.solve (x, rhs);
         M.mul (res, x);
         res.sub (rhs);
         if (size > 10) {
            System.out.println ("answer (first 10 values):");
         }
         else {
            System.out.println ("answer:");
         }
         for (int i=0; i<Math.min(size,10); i++) {
            System.out.println (x.get(i)+" ");
         }        
         System.out.println ("residual norm=" + res.norm()/rhs.norm());
         // PrintWriter pw = new PrintWriter (new FileWriter ("foo"));
         // M.write (pw, new NumberFormat("%g"), Matrix.WriteFormat.CRS);
      }
   }

   void testParameterAccessMethods (PardisoSolver solver) {

      int defaultNumThreads = solver.getNumThreads();
      solver.setNumThreads (3);
      TestSupport.doassert (
         solver.getNumThreads() == 3,
         "solver.getNumThreads() == 3");
      solver.setNumThreads (-1);
      TestSupport.doassert (
         solver.getNumThreads()==defaultNumThreads, 
         "solver.getNumThreads()==defaultNumThreads");
      
      ReorderMethod defaultReorder = solver.getReorderMethod();
      solver.setReorderMethod (ReorderMethod.AMD);
      TestSupport.doassert (
         solver.getReorderMethod() == ReorderMethod.AMD,
         "solver.getReorderMethod() == ReorderMethod.AMD");
      solver.setReorderMethod (ReorderMethod.DEFAULT);
      TestSupport.doassert (
         solver.getReorderMethod()==defaultReorder, 
         "solver.getReorderMethod()==defaultReorder");

      int defaultMaxRefinement = solver.getMaxRefinementSteps();
      solver.setMaxRefinementSteps (5);
      TestSupport.doassert (
         solver.getMaxRefinementSteps()==5,
         "solver.getMaxRefinementSteps()==5");
      solver.setMaxRefinementSteps (-1);

      TestSupport.doassert (
         solver.getMaxRefinementSteps()==defaultMaxRefinement,
         "solver.getMaxRefinementSteps()==defaultMaxRefinement");

      solver.setMatrixChecking (true);
      TestSupport.doassert (
         solver.getMatrixChecking(),
         "solver.getMatrixChecking()");

      solver.setMessageLevel (1);
      TestSupport.doassert (
         solver.getMessageLevel()==1,
         "solver.getMessageLevel()==1");
      solver.setMessageLevel (0);

      int defaultPerturbation = solver.getPivotPerturbation();
      solver.setPivotPerturbation (10);
      TestSupport.doassert (
         solver.getPivotPerturbation()==10,
         "solver.getPivotPerturbation()==10");
      solver.setPivotPerturbation (-1);
      TestSupport.doassert (
         solver.getPivotPerturbation()==defaultPerturbation,
         "solver.getPivotPerturbation()==defaultPerturbation");

      boolean defaultScaling = solver.getApplyScaling();
      solver.setApplyScaling (0);
      TestSupport.doassert (
         solver.getApplyScaling()==false,
         "solver.getApplyScaling()==false");
      solver.setApplyScaling (-1);
      TestSupport.doassert (
         solver.getApplyScaling()==defaultScaling,
         "solver.getApplyScaling()==defaultScaling");
      
      boolean defaultMatchings = solver.getApplyWeightedMatchings();
      solver.setApplyWeightedMatchings (0);
      TestSupport.doassert (
         solver.getApplyWeightedMatchings()==false,
         "solver.getApplyWeightedMatchings()==false");
      solver.setApplyWeightedMatchings (-1);
      TestSupport.doassert (
         solver.getApplyWeightedMatchings()==defaultMatchings,
         "solver.getApplyWeightedMatchings()==defaultMatchings");
      
      boolean default2x2Pivoting = solver.getUse2x2Pivoting();
      solver.setUse2x2Pivoting (0);
      TestSupport.doassert (
         solver.getUse2x2Pivoting()==false,
         "solver.getUse2x2Pivoting()==false");
      solver.setUse2x2Pivoting (-1);
      TestSupport.doassert (
         solver.getUse2x2Pivoting()==default2x2Pivoting,
         "solver.getUse2x2Pivoting()==default2x2Pivoting");
   }      

   private String toString (String fmtStr, double[] x) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      StringBuilder sbuild = new StringBuilder();
      for (int i=0; i<x.length; i++) {
         sbuild.append (fmt.format (x[i]) + " ");
      }
      return sbuild.toString();
   }

   private void checkSolution (double[] x, double[] chk) {
      for (int i=0; i<x.length; i++) {
         if (Math.abs(x[i]-chk[i]) > EPS) {
            throw new TestException (
               "Incorrect solution:\nGot:\n" +
               toString("%18.13f", x) +
               "\nExpected:\n" +
               toString("%18.13f", chk));
         }
      }
   }               

   public void dotest () {

      PardisoSolver solver = new PardisoSolver();
      System.out.println ("Pardiso: max threads=" + solver.getNumThreads());
      solver.setNumThreads (4);
      System.out.println ("Pardiso: max threads=" + solver.getNumThreads());
      solver.setNumThreads (0);
      int i;

      // set test symmetric matrix:
      // M = [3 1 2 0 0
      // 1 0 1 2 0
      // 2 1 4 1 0
      // 0 2 1 0 6
      // 0 0 0 6 2]
      NumberFormat fmt = new NumberFormat ("%10.5f");
      double[] vals3 = new double[] { 3, 1, 2, 0, 1, 2, 4, 1, 0, 6, 2 };
      int[] rows3 = incIndices (new int[] { 0, 3, 6, 8, 10, 11 });
      int[] cols3 = incIndices (new int[] { 0, 1, 2, 1, 2, 3, 2, 3, 3, 4, 4 });
      double[] b3 = new double[] { 1, 2, 3, 4, 5 };
      double[] x3 = new double[5];
      MatrixNd M = new MatrixNd (5, 5);
      M.setCRSValues (vals3, cols3, rows3, 11, 5, Partition.UpperTriangular);
      solver.analyze (M, 5, Matrix.SYMMETRIC);
      // solver.analyze (M, 5, Matrix.SYMMETRIC);

      //int[] rows3_1 = incIndices (rows3);
      //int[] cols3_1 = incIndices (cols3);
      // solver.setSymmetricMatrix (vals3, rows3_1, cols3_1, 5, 11);
      // solver.factorMatrix(vals3);
      solver.factor();

      TestSupport.doassert (
         solver.getNumNegEigenvalues()==2, "getNumNegEigenvalues()==2");
      TestSupport.doassert (
         solver.getNumPosEigenvalues()==3, "getNumPosEigenvalues()==3");
      TestSupport.doassert (
         solver.getNumPerturbedPivots()==0, "getNumPerturbedPivots()==0");
      TestSupport.doassert (
         solver.getSPDZeroPivot()==0, "getSPDZeroPivot()==0");

      testParameterAccessMethods (solver);

      if (verbose) {
         System.out.println (
            "peak analysis memory=" + solver.getPeakAnalysisMemoryUsage());
         System.out.println (
            "analysis memory=" + solver.getAnalysisMemoryUsage());
         System.out.println (
            "factor solve memory=" + solver.getFactorSolveMemoryUsage());
      }
      
      solver.solve (x3, b3);
      if (verbose) {
         System.out.println ("Sparse symmetric:");
         for (i = 0; i < 5; i++) {
            System.out.println (fmt.format (x3[i]));
         }
      }
      checkSolution (
         x3,
         new double[] {
            0.1111111111111, 
            -0.8888888888889,
            0.7777777777778,
            0.5555555555556,
            0.8333333333333 
         });

      // Now change matrix but keep topology:
      // M = [3 1 2 0 0
      // 1 10 1 2 0
      // 2 1 4 1 0
      // 0 2 1 10 5
      // 0 0 0 5 2]
      double[] vals4 = { 3, 1, 2, 10, 1, 2, 4, 1, 10, 5, 2 };
      M.setCRSValues (vals4, cols3, rows3, 11, 5, Partition.UpperTriangular);
      // solver.factorMatrix(vals4);
      solver.factor();
      solver.solve (x3, b3);
      if (verbose) {
         System.out.println ("Sparse symmetric, different values:");
         for (i = 0; i < 5; i++) {
            System.out.println (fmt.format (x3[i]));
         }
      }
      checkSolution (
         x3,
         new double[] {
            0.6032064128257,
            -0.4368737474950,
            -0.1863727454910,
            2.9759519038076,
            -4.9398797595190
         });
      
      // Now test factor and solve
      // M = [5 1 2 0 0
      // 1 12 1 2 0
      // 2 1 4 1 0
      // 0 2 1 9 5
      // 0 0 0 5 2]
      double[] vals5 = { 5, 1, 2, 12, 1, 2, 4, 1, 9, 5, 2 };
      M.setCRSValues (vals5, cols3, rows3, 11, 5, Partition.UpperTriangular);
      solver.autoFactorAndSolve (x3, b3, 0);
      if (verbose) {
         System.out.println ("Sparse symmetric, factor and solve:");
         for (i = 0; i < 5; i++) {
            System.out.println (fmt.format (x3[i]));
         }
      }
      checkSolution (
         x3,
         new double[] {
            0.1966035271065,
            -0.2482037883736,
            0.1325930764206,
            2.3246244284781,
            -3.3115610711953
         });

      double[] vals = new double[] { 1, 2, 3, 0, 4, 0, 5, 0, 6 };
      int[] rows = incIndices (new int[] { 0, 3, 6, 10 });
      int[] cols = incIndices (new int[] { 0, 1, 2, 0, 1, 2, 0, 1, 2 });
      //int[] rows_1 = incIndices (rows);
      //int[] cols_1 = incIndices (cols);
      double x[] = new double[3];
      double[] b1 = new double[] { 1, 2, 3 };

      M = new MatrixNd(3,3);
      M.setCRSValues (vals, cols, rows, 9, 3, Partition.Full);
      solver.analyze (M, 3, 0);
      solver.factor();
      TestSupport.doassert (
         solver.getNumNegEigenvalues()==-1, "getNumNegEigenvalues()==-1");
      TestSupport.doassert (
         solver.getNumPosEigenvalues()==-1, "getNumPosEigenvalues()==-1");

      // solver.setMatrix (vals, rows_1, cols_1, 3, 9);
      // solver.factorMatrix();
      solver.solve (x, b1);
      if (verbose) {
         System.out.println ("Dense unsymmetric:");
         for (i=0; i<3; i++)
            { System.out.println (fmt.format(x[i]));
            }
      }
      checkSolution (x, new double[] { 1, 0.5, -1/3.0 });

      double[] b2 = new double[] { 4, 5, 6 };

      solver.solve (x, b2);
      if (verbose) {
         System.out.println ("Dense unsymmetric, second solution:");
         for (i=0; i<3; i++) {
            System.out.println (fmt.format(x[i]));
         }
      }
      checkSolution (x, new double[] { 1, 1.25, 1/6.0 });

      // double[] vals2 = new double[]
      // { 26, 2, 33, 20, 6, 45 };
      // int[] rows2 = new int[]
      // { 0, 3, 5
      // };
      // int[] cols2 = new int[]
      // { 0, 1, 2, 1, 2, 2
      // };
      // M.setCRSValues (vals2, cols2, rows2,
      // 6, 3, Partition.UpperTriangular);
      // int[] rows2_1 = incIndices(rows2);
      // int[] cols2_1 = incIndices(cols2);

      // solver.analyze (M, 3, Matrix.SYMMETRIC);
      // solver.factor();
      // // solver.setSymmetricMatrix (vals2, rows2_1, cols2_1, 3, 6);
      // // solver.factorMatrix();
      // solver.solve (x, b1);
      // System.out.println ("Dense symmetric:");
      // for (i=0; i<3; i++)
      // { System.out.println (fmt.format(x[i]));
      // }

      FunctionTimer timer = new FunctionTimer();
      SparseMatrixNd S = new SparseMatrixNd (2529, 2529);
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (
               new BufferedReader (
                  new FileReader (
                     PathFinder.getSourceRelativePath (
                        this, "testMatrix.mat"))));
         S.scan (rtok);
      }
      catch (Exception e) {
         e.printStackTrace();
      }

      int size = S.rowSize();
      if (verbose) {
         System.out.println ("scanned solve matrix, size=" + size);
         System.out.println ("number of non-zeros: " + S.numNonZeroVals());
      }
      VectorNd bvec = new VectorNd (size);
      VectorNd xvec = new VectorNd (size);
      VectorNd check = new VectorNd (size);
      for (i = 0; i < size; i++) {
         bvec.set (i, 1);
      }

      // norm is the Euclidean norm of all non-zero elements
      int numElements = S.numExplicitElements();
      double[] values = new double[numElements];
      S.getExplicitElements (values);
      double norm = 0;
      for (int k = 0; k < numElements; k++) {
         norm += values[k] * values[k];
      }
      norm = Math.sqrt (norm / numElements);
      // System.out.println ("norm=" + norm);

      solver.analyze (S, size, Matrix.SYMMETRIC);

      int cnt = 10;
      timer.start();
      for (int k = 0; k < cnt; k++) {
         solver.factor();
         solver.solve (xvec, bvec);
      }
      timer.stop();
      if (verbose) {
         System.out.println (" separate factor and solve time: "
                             + timer.result (cnt));
      }
      S.mul (check, xvec);
      check.sub (bvec);
      if (verbose) {
         System.out.println ("reg error=" + check.infinityNorm());
      }
      double regTol = 1e-10;
      if (check.infinityNorm() > regTol) {
         throw new TestException ("large matrix error exceeds " + regTol);
      }

      perturbSymmetricMatrix (S, norm / 10000);

      timer.start();
      // solver.factor();
      // solver.solve (xvec, bvec);
      int iterCnt = solver.iterativeSolve (xvec, bvec, 10);
      timer.stop();
      if (verbose) {
         System.out.println ("iterative solve: " + iterCnt);
         System.out.println ("iterative factor and solve time: "
                             + timer.result (1));
      }
      S.mul (check, xvec);
      check.sub (bvec);
      if (verbose) {
         System.out.println ("CG error=" + check.infinityNorm());
      }
      double CGtol = 1e-9;
      if (check.infinityNorm() > CGtol) {
         throw new TestException ("CG error exceeds " + CGtol);
      }
   }

   private static void printUsage () {
      System.out.println (
         "Usage: java maspack.solvers.PardisoSolverTest [-help] [-verbose]");
      System.out.println (
         "            [-matrixType=(INDEFINITE|SYMMETRIC|SPD)] [<testFile>]");
   }

   public static void main (String[] args) {
      PardisoSolverTest tester = new PardisoSolverTest();
      PardisoSolver.printThreadInfo = false;
      boolean printHelp = false;
      String testFileName = null;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-help")) {
            printUsage();
            System.exit(1); 
         }
         else if (args[i].startsWith ("-matrixType=")) {
            tester.setMatrixType (args[i].substring ("-matrixType=".length()));
         }
         else if (args[i].equals ("-verbose")) {
            tester.verbose = true;
         }
         else if (!args[i].startsWith ("-") && testFileName == null) {
            testFileName = args[i];
         }
         else {
            printUsage();
            System.exit(1); 
         }
      }
      
      if (testFileName != null) {
         try {
            ReaderTokenizer rtok = new ReaderTokenizer (
               new BufferedReader (new FileReader (testFileName)));
            tester.testFromFile (rtok);
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
      }
      else {
         try {
            tester.dotest();
         }
         catch (Exception e) {
            e.printStackTrace(); 
            System.exit(1);
         }
         System.out.println ("\nPassed\n");
      }
      
      //tester.dotest();
      //tester.frametest();
      //tester.threadtest();
   }
}
