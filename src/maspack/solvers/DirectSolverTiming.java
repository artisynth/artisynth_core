/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.util.ArrayList;
import java.util.Arrays;

import maspack.matrix.Matrix;
import maspack.solvers.FemMatrixGenerator.CRSMatrix;
import maspack.solvers.FemMatrixGenerator.ElemType;
import maspack.util.NumberFormat;

/**
 * Times the analyze, factor and solve phases of the direct sparse solvers
 * across a range of matrix sizes, using the synthetic 2D and 3D finite element
 * matrices produced by {@link FemMatrixGenerator}.
 *
 * <p>Usage:
 * <pre>
 *  java maspack.solvers.DirectSolverTiming \
 *     [-elem hex|tet|quad|tri] [-gridres spec,spec,...] [-sizes n,n,...] \
 *     [-solvers Pardiso,Mumps,...] [-threads n] [-reps n] [-nrhs n] \
 *     [-cons n] [-consRatio r] [-verbose]
 * </pre>
 * where {@code -gridres} gives the grid resolutions to test and {@code -sizes}
 * instead gives approximate matrix sizes. Each {@code -gridres} specification
 * takes the form {@code RX[xRY[xRZ]]}, where {@code RX}, {@code RY} and {@code
 * RZ} are the numbers of nodes along the x, y and z axes. Missing resolutions
 * default to the previous one, so that {@code 16} means {@code 16x16x16} and
 * {@code 40x8} means {@code 40x8x8}. {@code RZ} is ignored for 2D element
 * types.
 *
 * <p>{@code -cons} appends the indicated number of constraint rows to produce
 * a symmetric indefinite KKT matrix, while {@code -consRatio} instead gives
 * the approximate ratio of constraints to the overall matrix size; if both are
 * specified, the last one prevails. {@code -reps} sets the number of times the
 * factor and solve phases are repeated (the fastest time is reported).
 */
public class DirectSolverTiming {

   ElemType myElemType = ElemType.HEX;
   int[][] myGridRes = null;
   int[] mySizes = null;
   int myNumThreads = -1;
   int myReps = 3;
   int myNrhs = 1;
   int myNumCons = 0;
   double myConsRatio = -1;
   int myNodesPerCon = 4;
   boolean myWarmup = true;
   boolean myVerbose = false;
   ArrayList<SparseSolverId> mySolverIds = new ArrayList<>();

   private static final int[] DEFAULT_RES_3D = new int[] {8, 12, 16, 20, 24};
   private static final int[] DEFAULT_RES_2D =
      new int[] {32, 64, 128, 256, 512};

   NumberFormat myIfmt = new NumberFormat ("%8d");
   NumberFormat myGfmt = new NumberFormat ("%9.3g");
   NumberFormat myFfmt = new NumberFormat ("%9.2f");

   /**
    * Timing results for one solver on one matrix.
    */
   static class Result {
      double analyzeMsec;
      double factorMsec;
      double solveMsec;
      long nnzFactors;
      double residual;
      String error;
      String repTimes;
   }

   /**
    * Returns the grid resolutions to be tested, or {@code null} if matrix
    * sizes have been specified instead.
    */
   int[][] gridResolutions() {
      if (myGridRes != null) {
         return myGridRes;
      }
      else if (mySizes != null) {
         return null;
      }
      else {
         int[] res = (myElemType.getDimension() == 3 ?
                      DEFAULT_RES_3D : DEFAULT_RES_2D);
         int[][] gridRes = new int[res.length][];
         for (int i=0; i<res.length; i++) {
            gridRes[i] = new int[] { res[i], res[i], res[i] };
         }
         return gridRes;
      }
   }

   /**
    * Returns the number of constraints to be added to a matrix of the
    * indicated size.
    */
   int numConstraints (int size) {
      if (myConsRatio > 0) {
         return (int)Math.round (myConsRatio*size);
      }
      else {
         return myNumCons;
      }
   }

   CRSMatrix createMatrix (FemMatrixGenerator gen, int[] res, int size) {
      CRSMatrix M;
      int dofs = myElemType.getDimension();
      if (res != null) {
         if (dofs == 3) {
            M = gen.create (myElemType, res[0], res[1], res[2], dofs);
         }
         else {
            M = gen.create (myElemType, res[0], res[1], 1, dofs);
         }
      }
      else {
         M = gen.createForSize (myElemType, size);
      }
      int numCons = numConstraints (M.size);
      if (numCons > 0) {
         M = gen.addConstraints (M, numCons, myNodesPerCon, dofs);
      }
      return M;
   }

   /**
    * Returns the label used to identify a grid resolution in the output.
    */
   String gridLabel (int[] res) {
      if (res == null) {
         return "-";
      }
      else if (myElemType.getDimension() == 3) {
         return res[0]+"x"+res[1]+"x"+res[2];
      }
      else {
         return res[0]+"x"+res[1];
      }
   }

   Result timeSolver (SparseSolverId id, CRSMatrix M) {
      Result res = new Result();
      DirectSolver solver;
      try {
         solver = id.createDirectSolver();
      }
      catch (Exception e) {
         res.error = e.getMessage();
         return res;
      }
      if (solver == null) {
         res.error = "not a direct solver";
         return res;
      }
      if (myNumThreads > 0) {
         // set explicitly on the solver: setDefaultNumThreads() is only
         // applied when a solver initializes itself, which has already
         // happened by this point
         ((DirectSolverBase)solver).setNumThreads (myNumThreads);
      }
      int size = M.size;
      double[] vals = M.copyVals();
      double[] b = new double[size];
      double[] x = new double[size];
      double[] B = null;
      double[] X = null;
      for (int i=0; i<size; i++) {
         b[i] = 1.0;
      }
      if (myNrhs > 1) {
         B = new double[size*myNrhs];
         X = new double[size*myNrhs];
         Arrays.fill (B, 1.0);
      }
      try {
         long t0 = System.nanoTime();
         solver.analyze (vals, M.colIdxs, M.rowOffs, size, M.type);
         res.analyzeMsec = (System.nanoTime()-t0)/1e6;

         res.factorMsec = Double.MAX_VALUE;
         res.solveMsec = Double.MAX_VALUE;
         StringBuilder reptimes =
            (myVerbose ? new StringBuilder() : null);
         for (int rep=0; rep<myReps; rep++) {
            t0 = System.nanoTime();
            solver.factor (vals);
            double fmsec = (System.nanoTime()-t0)/1e6;

            t0 = System.nanoTime();
            if (myNrhs > 1) {
               solver.solve (X, B, myNrhs);
            }
            else {
               solver.solve (x, b);
            }
            double smsec = (System.nanoTime()-t0)/1e6;

            res.factorMsec = Math.min (res.factorMsec, fmsec);
            res.solveMsec = Math.min (res.solveMsec, smsec);
            if (reptimes != null) {
               reptimes.append (
                  String.format (
                     "   rep %d: factor %9.2f solve %9.2f%n",
                     rep, fmsec, smsec));
            }
         }
         if (reptimes != null) {
            res.repTimes = reptimes.toString();
         }
         if (myNrhs > 1) {
            System.arraycopy (X, 0, x, 0, size);
         }
         res.nnzFactors = solver.getNumNonZerosInFactors();
         res.residual = M.residual (x, b);
      }
      catch (Exception e) {
         res.error = e.getMessage();
         if (myVerbose) {
            e.printStackTrace();
         }
      }
      solver.dispose();
      return res;
   }

   void printHeader() {
      System.out.println (
         "elem=" + myElemType +
         " dofs/node=" + myElemType.getDimension() +
         " nrhs=" + myNrhs +
         " reps=" + myReps +
         (myConsRatio > 0 ? " consRatio=" + myConsRatio :
          (myNumCons > 0 ? " constraints=" + myNumCons : "")) +
         (myNumThreads > 0 ? " threads=" + myNumThreads : "") +
         "; times in msec");
      System.out.println (
         String.format (
            "%-10s%-12s%8s %9s%9s %9s %9s %9s %9s",
            "solver", "grid", "size", "nnz", "analyze",
            "factor", "solve", "nnzL", "residual"));
   }

   void printResult (
      SparseSolverId id, String grid, CRSMatrix M, Result res) {

      StringBuilder sb = new StringBuilder();
      sb.append (String.format ("%-10s", id.toString()));
      sb.append (String.format ("%-12s", grid));
      sb.append (myIfmt.format (M.size));
      sb.append (" ");
      sb.append (myGfmt.format ((double)M.numFullVals()));
      if (res.error != null) {
         sb.append ("  FAILED: " + res.error);
      }
      else {
         sb.append (myFfmt.format (res.analyzeMsec));
         sb.append (" ");
         sb.append (myFfmt.format (res.factorMsec));
         sb.append (" ");
         sb.append (myFfmt.format (res.solveMsec));
         sb.append (" ");
         sb.append (myGfmt.format ((double)res.nnzFactors));
         sb.append (" ");
         sb.append (myGfmt.format (res.residual));
      }
      System.out.println (sb.toString());
      if (res.repTimes != null) {
         System.out.print (res.repTimes);
      }
   }

   /**
    * Runs a small discarded solve for each solver, so that the timings are not
    * polluted by the one-time costs (library loading, JNI method resolution,
    * MKL and OpenMP thread pool creation) incurred on the first call.
    */
   void warmupSolvers() {
      FemMatrixGenerator gen = new FemMatrixGenerator();
      CRSMatrix M = gen.create (myElemType, 4, 4, 4, 3);
      int savedReps = myReps;
      int savedNrhs = myNrhs;
      myReps = 1;
      myNrhs = 1;
      for (SparseSolverId id : mySolverIds) {
         timeSolver (id, M);
      }
      myReps = savedReps;
      myNrhs = savedNrhs;
   }

   public void run() {
      if (mySolverIds.size() == 0) {
         // use all the direct solvers whose native libraries are available
         for (SparseSolverId id : SparseSolverId.values()) {
            if (id.isDirect()) {
               try {
                  DirectSolver solver = id.createDirectSolver();
                  if (solver != null) {
                     solver.dispose();
                     mySolverIds.add (id);
                  }
               }
               catch (Exception e) {
                  System.out.println (
                     "Ignoring solver " + id + ": " + e.getMessage());
               }
            }
         }
      }
      if (myWarmup) {
         warmupSolvers();
      }
      printHeader();
      FemMatrixGenerator gen = new FemMatrixGenerator();
      int[][] gridRes = gridResolutions();
      int numCases = (gridRes != null ? gridRes.length : mySizes.length);
      for (int i=0; i<numCases; i++) {
         int[] res = (gridRes != null ? gridRes[i] : null);
         int size = (gridRes != null ? -1 : mySizes[i]);
         CRSMatrix M = createMatrix (gen, res, size);
         String grid = gridLabel (res);
         for (SparseSolverId id : mySolverIds) {
            printResult (id, grid, M, timeSolver (id, M));
         }
         if (mySolverIds.size() > 1) {
            System.out.println ("");
         }
      }
   }

   private static int[] parseIntList (String str) {
      String[] strs = str.split (",");
      int[] vals = new int[strs.length];
      for (int i=0; i<strs.length; i++) {
         vals[i] = Integer.parseInt (strs[i].trim());
      }
      return vals;
   }

   /**
    * Parses a comma separated list of grid resolution specifications, each of
    * the form {@code RX[xRY[xRZ]]}, with missing resolutions defaulting to the
    * previous one.
    */
   private static int[][] parseGridRes (String str) {
      String[] specs = str.split (",");
      int[][] gridRes = new int[specs.length][];
      for (int i=0; i<specs.length; i++) {
         String[] strs = specs[i].trim().split ("[xX]");
         if (strs.length < 1 || strs.length > 3) {
            throw new IllegalArgumentException (
               "grid resolution '"+specs[i].trim()+
               "' should have the form RX[xRY[xRZ]]");
         }
         int[] res = new int[3];
         for (int j=0; j<3; j++) {
            if (j < strs.length) {
               res[j] = Integer.parseInt (strs[j].trim());
            }
            else {
               res[j] = res[j-1]; // default to the previous resolution
            }
         }
         gridRes[i] = res;
      }
      return gridRes;
   }

   private static void printUsageAndExit() {
      System.out.println (
         "Usage: java maspack.solvers.DirectSolverTiming\n" +
         "  [-elem hex|tet|quad|tri] [-gridres RX[xRY[xRZ]],...]\n" +
         "  [-sizes n,n,...] [-solvers Pardiso,Mumps,...] [-threads n]\n" +
         "  [-reps n] [-nrhs n] [-cons n] [-consRatio r] [-nodesPerCon n]\n" +
         "  [-nowarmup] [-verbose]");
      System.exit (1);
   }

   public static void main (String[] args) {
      DirectSolverTiming timing = new DirectSolverTiming();
      for (int i=0; i<args.length; i++) {
         String arg = args[i];
         if (arg.equals ("-elem") && i+1 < args.length) {
            timing.myElemType =
               ElemType.valueOf (args[++i].toUpperCase());
         }
         else if (arg.equals ("-gridres") && i+1 < args.length) {
            timing.myGridRes = parseGridRes (args[++i]);
         }
         else if (arg.equals ("-sizes") && i+1 < args.length) {
            timing.mySizes = parseIntList (args[++i]);
         }
         else if (arg.equals ("-solvers") && i+1 < args.length) {
            for (String str : args[++i].split (",")) {
               timing.mySolverIds.add (SparseSolverId.valueOf (str.trim()));
            }
         }
         else if (arg.equals ("-threads") && i+1 < args.length) {
            timing.myNumThreads = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-reps") && i+1 < args.length) {
            timing.myReps = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-nrhs") && i+1 < args.length) {
            timing.myNrhs = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-cons") && i+1 < args.length) {
            timing.myNumCons = Integer.parseInt (args[++i]);
            timing.myConsRatio = -1; // last of -cons/-consRatio prevails
         }
         else if (arg.equals ("-consRatio") && i+1 < args.length) {
            timing.myConsRatio = Double.parseDouble (args[++i]);
            timing.myNumCons = 0; // last of -cons/-consRatio prevails
         }
         else if (arg.equals ("-nodesPerCon") && i+1 < args.length) {
            timing.myNodesPerCon = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-nowarmup")) {
            timing.myWarmup = false;
         }
         else if (arg.equals ("-verbose")) {
            timing.myVerbose = true;
         }
         else {
            printUsageAndExit();
         }
      }
      timing.run();
   }
}
