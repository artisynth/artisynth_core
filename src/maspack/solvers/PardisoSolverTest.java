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

public class PardisoSolverTest extends DirectSolverTestBase
   implements ActionListener {

   private int myMatrixType = Matrix.INDEFINITE;
   private Partition myPartition = Partition.Full;


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
         try {
            dotest();
         }
         catch (IOException e) {
            e.printStackTrace();
         }
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
         // test for multiple right-hand sides
         int nrhs = 20;
         double[] X = new double[nrhs*size];
         double[] B = new double[nrhs*size];
         for (int i=0; i<size; i++) {
            for (int j=0; j<nrhs; j++) {
               B[j*size+i] = rhs.get(i);
            }
         }
         SparseCRSMatrix M = new SparseCRSMatrix (size, size);
         System.out.println (
            "solving "+getMatrixType(myMatrixType)+" matrix");
         M.setCRSValues (vals, colIdxs, rowOffs, nvals, size, myPartition);
         FunctionTimer timer = new FunctionTimer();
         timer.start();
         solver.analyze (M, size, myMatrixType);
         timer.stop();
         if (verbose) {
            System.out.println ("analyze time=" + timer.result(1));
         }
         System.out.println (
            "numNonZerosInFactors=" + solver.getNumNonZerosInFactors());
         timer.start();
         solver.factor ();
         timer.stop();
         if (verbose) {
            System.out.println ("factor time=" + timer.result(1));
         }
         timer.start();
         solver.solve (x, rhs);
         timer.stop();
         if (verbose) {
            System.out.println ("solve time=" + timer.result(1));
         }
         timer.start();
         solver.setMaxRefinementSteps (0);
         solver.solve (X, B, nrhs);
         timer.stop();
         if (verbose) {
            System.out.println (
               "solve time for "+nrhs+" rhs (no refinement) =" + timer.result(1));
         }
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
         double maxerr = 0;
         for (int i=0; i<size; i++) {
            for (int j=0; j<nrhs; j++) {
               maxerr = Math.max (Math.abs(X[j*size+i] - x.get(i)), maxerr);
            }
         }
         System.out.println ("maxerr for "+nrhs+" rhs = " + maxerr);
         // PrintWriter pw = new PrintWriter (new FileWriter ("foo"));
         // M.write (pw, new NumberFormat("%g"), Matrix.WriteFormat.CRS);
      }
   }

   void symmetricTest1() {
      PardisoSolver solver = new PardisoSolver();

      int size = 4;
      int nvals = 6;
      int[] colIdxs = new int[] { 1, 3, 2, 4, 3, 4 };
      int[] rowOffs = new int[] { 1, 3, 5, 6, 7 };
      double[] valuesDiag = new double[] { 1, 0, 1, 0, 1, 1 };
      double[] valuesOff = new double[] { 1, 1, 1, 1, 0, 0 };

      double[] rhs = new double[] { 1, 1, 1, 1 };
      double[] x = new double[4];

      SparseCRSMatrix M = new SparseCRSMatrix (size, size);
      M.setCRSValues (
         valuesOff, colIdxs, rowOffs, nvals, size, Partition.UpperTriangular);
      solver.analyze (M, size, Matrix.SYMMETRIC);
      solver.factor (valuesDiag);
      solver.solve (x, rhs);
      //System.out.println ("diag= " + new VectorNd(x));
      solver.factor (valuesOff);
      solver.solve (x, rhs);
      //System.out.println ("off= " + new VectorNd(x));
   }

   void symmetricTest2() {
      PardisoSolver solver = new PardisoSolver();
      solver.setMaxRefinementSteps(0);

      CRSValues crs0 = new CRSValues();
      CRSValues crs1 = new CRSValues();

      crs0.scan ("AF.txt");
      crs1.scan ("AZ.txt");

      int size = crs0.rowSize();
      int nvals = crs0.numNonZeros();
      VectorNd x = new VectorNd(size);
      VectorNd y = new VectorNd(size);
      x.setAll (1.0);
      solver.analyze (
         crs0.getValues(), crs0.getColIdxs(), crs0.getRowOffs(),
         size, Matrix.SYMMETRIC);
      solver.factor (crs0.getValues());
      solver.solve (y, x);
      solver.factor (crs1.getValues());
      solver.solve (y, x);
   }

   void testParameterAccessMethods (PardisoSolver solver) {

      int defaultNumThreads = solver.getNumThreads();
      // see how many threads are available up to 4:
      solver.setNumThreads (4);
      int maxNumThreads = solver.getNumThreads();
      for (int n=1; n<=maxNumThreads; n++) {
         solver.setNumThreads (n);
         int nchk = solver.getNumThreads();
         if (n != nchk) {
            throw new TestException (
               "solver.getNumThreads() = "+nchk+", expected " + n);
         }
      }
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

      boolean defaultMatrixChecking = solver.getMatrixChecking();
      solver.setMatrixChecking (true);
      TestSupport.doassert (
         solver.getMatrixChecking(),
         "solver.getMatrixChecking()");
      solver.setMatrixChecking (defaultMatrixChecking);

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




   protected DirectSolver createSolver() {
      return new PardisoSolver();
   }

   protected boolean setShowPerturbedPivots (boolean enable) {
      boolean prev = PardisoSolver.getShowPerturbedPivots();
      PardisoSolver.setShowPerturbedPivots (enable);
      return prev;
   }

   /**
    * Tests that a matrix which is not positive definite, but which is
    * declared SPD, fails to factor and reports the offending row.
    */
   public void testSPDFailure() {
      PardisoSolver solver = new PardisoSolver();
      boolean showPivots = setShowPerturbedPivots (false);
      double[] npdVals = new double[] { 4, 1, 4, 1, 4, 1, 4, 1, -4 };
      int[] npdCols = incIndices (new int[] { 0, 1, 1, 2, 2, 3, 3, 4, 4 });
      int[] npdRows = incIndices (new int[] { 0, 2, 4, 6, 8, 9 });
      solver.analyze (npdVals, npdCols, npdRows, 5, Matrix.SPD);
      try {
         solver.factor (npdVals);
         throw new TestException (
            "factoring a non-positive-definite SPD matrix did not throw "+
            "an exception");
      }
      catch (NumericalException e) {
         check ("error message is set", solver.getErrorMessage() != null);
         checkEquals ("getSPDZeroPivot()", solver.getSPDZeroPivot(), 5);
         if (verbose) {
            System.out.println ("expected error: " + e.getMessage());
         }
      }
      solver.dispose();
      PardisoSolver.setShowPerturbedPivots (showPivots);
   }


   /**
    * Tests the capabilities which Pardiso reports.
    */
   public void testCapabilities() {
      PardisoSolver solver = new PardisoSolver();
      check ("hasIterativeSolves()", solver.hasIterativeSolves());
      checkEquals (
         "hasMultipleRhsSolves()", solver.hasMultipleRhsSolves(),
         PardisoSolver.supportsMultipleRhs);
      solver.dispose();
   }

   /**
    * Tests the statistics which Pardiso reports for a larger factorization.
    */
   public void testStatistics() throws IOException {
      PardisoSolver solver = new PardisoSolver();

      SparseMatrixNd S = loadTestMatrix();
      int size = S.rowSize();
      VectorNd b = new VectorNd (size);
      VectorNd x = new VectorNd (size);
      b.setAll (1.0);

      solver.analyze (S, size, Matrix.SYMMETRIC);
      solver.factor();
      solver.solve (x, b);

      check ("getNumNonZerosInFactors() > 0",
             solver.getNumNonZerosInFactors() > 0);
      check ("getPeakAnalysisMemoryUsage() > 0",
             solver.getPeakAnalysisMemoryUsage() > 0);
      check ("getAnalysisMemoryUsage() > 0",
             solver.getAnalysisMemoryUsage() > 0);
      check ("getFactorSolveMemoryUsage() > 0",
             solver.getFactorSolveMemoryUsage() > 0);
      if (verbose) {
         System.out.println (
            "nnz in factors=" + solver.getNumNonZerosInFactors());
         System.out.println (
            "peak analysis memory=" + solver.getPeakAnalysisMemoryUsage());
         System.out.println (
            "analysis memory=" + solver.getAnalysisMemoryUsage());
         System.out.println (
            "factor solve memory=" + solver.getFactorSolveMemoryUsage());
      }
      solver.dispose();
   }

   public void dotest () throws IOException {
      testBasics();
      testSPDFailure();
      testCapabilities();
      testStatistics();

      // the parameter access methods are tested on a factored solver
      PardisoSolver solver = new PardisoSolver();
      solver.analyze (symVals, symColIdxs, symRowOffs, 5, Matrix.SYMMETRIC);
      solver.factor (symVals);
      testParameterAccessMethods (solver);
      solver.dispose();
   }

   public void test() throws IOException {
      dotest();
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
      
      //tester.symmetricTest1();
      //tester.symmetricTest2();

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
