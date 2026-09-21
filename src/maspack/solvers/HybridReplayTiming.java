/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

import maspack.matrix.Matrix;
import maspack.matrix.NumericalException;
import maspack.util.FastDoubleParser;

/**
 * Replays the solves recorded by {@link MurtyMechSolver#writeSolveInfo} for a
 * simulation with (at most) bilateral constraints, to measure hybrid
 * direct/iterative solves on real ArtiSynth matrix sequences.
 *
 * <p>Each step's KKT system
 * <pre>
 *   [ M   GT ] [  vel ]   [ bm ]
 *   [ G  -Rg ] [ -lam ] = [ bg ]
 * </pre>
 * is assembled exactly as by MurtyMechSolver (upper triangular CRS, with M's
 * entries followed by those of GT in each row). All steps are read and
 * assembled before any timing, and cached in a binary file, so the timed
 * region of each step contains only the solver calls (including their JNI
 * array copies, as in MurtyMechSolver).
 *
 * <p>Two modes:
 * <ul>
 * <li><i>policy</i> (default): replays MurtyMechSolver's refactoring policy
 * for each solver and method: a hybrid solve is attempted when the running
 * average hybrid time is below 0.8 times the running average direct
 * (factor+solve) time, with exponential weight 0.25; after a failed hybrid
 * solve, or otherwise, the step is factored and solved directly. Method
 * DIRECT disables hybrid solves, and gives the baseline.
 * <li><i>lag</i> ({@code -lag j0,...}): factors step j0 once, then solves
 * each later step iteratively, until two consecutive failures, to show how
 * far a factorization remains a useful preconditioner.
 * </ul>
 *
 * <pre>
 * java -Xmx12g maspack.solvers.HybridReplayTiming -data file.txt
 *    [-cache dir] [-cacheOnly] [-out steps.csv] [-summary summary.csv]
 *    [-solvers Pardiso,Mumps] [-methods DIRECT,NATIVE,GMRES,CGS]
 *    [-threads 8] [-tolExp 10] [-maxSolves 50] [-restart 20] [-steps n]
 *    [-lag 0,50] [-lagMaxSolves 100]
 * </pre>
 * NATIVE (Pardiso's own iterative solve) is only run for Pardiso.
 */
public class HybridReplayTiming {

   static final int CACHE_MAGIC = 0x48525431; // "HRT1"

   String myDataFile = null;
   String myCacheDir = null;
   boolean myCacheOnly = false;
   String myStepsFile = null;
   String mySummaryFile = null;
   String[] mySolvers = new String[] { "Pardiso" };
   String[] myMethods = new String[] { "DIRECT", "GMRES" };
   int myNumThreads = 8;
   int myTolExp = 10;
   int myMaxSolves = 50;
   int myRestart = 20;
   int myMaxSteps = Integer.MAX_VALUE;
   int[] myLagStarts = null;
   int myLagMaxSolves = 100;
   // MurtyMechSolver's policy parameters
   double myHybridRatio = 0.8;
   double myTimingWeight = 0.25;

   /**
    * Assembled solve data for all steps.
    */
   static class ReplayData {
      String name;
      int sizeM;
      int sizeG;
      int size;
      int numVals;
      int numSteps;
      int[] rowOffs;    // 1-based, length size+1
      int[] colIdxs;    // 1-based
      double[][] vals;  // KKT values for each step
      double[][] rhs;   // [bm; bg] for each step
      double[][] ysol;  // recorded solution [vel; -lam] for each step
   }

   /* --- text reading --- */

   /**
    * CharSequence view of a byte token, for FastDoubleParser.
    */
   static class ByteChars implements CharSequence {
      byte[] buf;
      int len;

      void set (byte[] buf, int len) {
         this.buf = buf;
         this.len = len;
      }

      public int length() {
         return len;
      }

      public char charAt (int i) {
         return (char)buf[i];
      }

      public CharSequence subSequence (int start, int end) {
         return new String (buf, start, end-start);
      }

      public String toString() {
         return new String (buf, 0, len);
      }
   }

   /**
    * Minimal whitespace tokenizer over an ASCII byte stream.
    */
   static class Scanner {
      InputStream in;
      byte[] buf = new byte[1<<22];
      int pos = 0;
      int len = 0;
      byte[] tok = new byte[64];
      int tlen = 0;
      ByteChars chars = new ByteChars();

      Scanner (InputStream in) {
         this.in = in;
      }

      int read() throws IOException {
         if (pos == len) {
            len = in.read (buf, 0, buf.length);
            pos = 0;
            if (len <= 0) {
               len = 0;
               return -1;
            }
         }
         return buf[pos++] & 0xff;
      }

      static boolean isSpace (int c) {
         return c == ' ' || c == '\n' || c == '\r' || c == '\t';
      }

      boolean next() throws IOException {
         int c;
         do {
            c = read();
         }
         while (isSpace (c));
         if (c < 0) {
            return false;
         }
         tlen = 0;
         while (c >= 0 && !isSpace (c)) {
            if (tlen == tok.length) {
               tok = Arrays.copyOf (tok, 2*tlen);
            }
            tok[tlen++] = (byte)c;
            c = read();
         }
         return true;
      }

      String word() throws IOException {
         if (!next()) {
            throw new EOFException ("unexpected end of file");
         }
         return new String (tok, 0, tlen, "US-ASCII");
      }

      void expect (String str) throws IOException {
         String w = word();
         if (!w.equals (str)) {
            throw new IOException ("expected '"+str+"', got '"+w+"'");
         }
      }

      int nextInt() throws IOException {
         if (!next()) {
            throw new EOFException ("unexpected end of file");
         }
         int i = 0;
         boolean neg = false;
         if (tok[0] == '-') {
            neg = true;
            i = 1;
         }
         int val = 0;
         for ( ; i<tlen; i++) {
            int d = tok[i] - '0';
            if (d < 0 || d > 9) {
               throw new IOException (
                  "bad integer '"+new String (tok, 0, tlen)+"'");
            }
            val = 10*val + d;
         }
         return neg ? -val : val;
      }

      double nextDouble() throws IOException {
         if (!next()) {
            throw new EOFException ("unexpected end of file");
         }
         chars.set (tok, tlen);
         return FastDoubleParser.parseDouble (chars);
      }

      void readInts (int[] vals) throws IOException {
         for (int i=0; i<vals.length; i++) {
            vals[i] = nextInt();
         }
      }

      void readDoubles (double[] vals) throws IOException {
         for (int i=0; i<vals.length; i++) {
            vals[i] = nextDouble();
         }
      }
   }

   static int headerValue (String token, String key) throws IOException {
      if (!token.startsWith (key+"=")) {
         throw new IOException ("expected "+key+"=, got '"+token+"'");
      }
      return Integer.parseInt (token.substring (key.length()+1));
   }

   /**
    * Reads an int array, and checks that it equals the one read for the first
    * step, if any. Returns the array to keep.
    */
   static int[] readStructure (
      Scanner scan, int[] prev, int len, String what, int step)
      throws IOException {
      int[] vals = new int[len];
      scan.readInts (vals);
      if (prev != null && !Arrays.equals (vals, prev)) {
         throw new IOException (
            what + " changed at step " + step + "; not supported");
      }
      return prev != null ? prev : vals;
   }

   static ReplayData readText (File file, int maxSteps) throws IOException {
      ReplayData data = new ReplayData();
      ArrayList<double[]> valsList = new ArrayList<>();
      ArrayList<double[]> rhsList = new ArrayList<>();
      ArrayList<double[]> ysolList = new ArrayList<>();
      int[] mRowOffs = null;
      int[] mColIdxs = null;
      int[] gRowOffs = null;
      int[] gColIdxs = null;
      double[] mvals = null;
      double[] gvals = null;
      double[] rg = null;
      double[] lamIn = null;
      int sizeM = -1;
      int sizeG = -1;

      try (InputStream in = new FileInputStream (file)) {
         Scanner scan = new Scanner (in);
         int step = 0;
         while (step < maxSteps && scan.next()) {
            String w = new String (scan.tok, 0, scan.tlen, "US-ASCII");
            if (!w.equals ("STEP:")) {
               throw new IOException ("expected 'STEP:', got '"+w+"'");
            }
            int msize = headerValue (scan.word(), "Msize");
            scan.word(); // Mtype
            scan.word(); // Mversion
            int gsize = headerValue (scan.word(), "Gsize");
            int nsize = headerValue (scan.word(), "Nsize");
            int dsize = headerValue (scan.word(), "Dsize");
            if (nsize != 0 || dsize != 0) {
               throw new IOException (
                  "step "+step+": unilateral or friction constraints "+
                  "are not supported");
            }
            if (step == 0) {
               sizeM = msize;
               sizeG = gsize;
               mvals = null;
               rg = new double[sizeG];
               lamIn = new double[sizeG];
            }
            else if (msize != sizeM || gsize != sizeG) {
               throw new IOException ("sizes changed at step "+step);
            }
            scan.expect ("M:");
            mRowOffs = readStructure (
               scan, mRowOffs, sizeM+1, "M row offsets", step);
            int nnzM = mRowOffs[sizeM]-1;
            mColIdxs = readStructure (
               scan, mColIdxs, nnzM, "M column indices", step);
            if (mvals == null) {
               mvals = new double[nnzM];
            }
            scan.readDoubles (mvals);
            double[] b = new double[sizeM+sizeG];
            double[] y = new double[sizeM+sizeG];
            scan.expect ("bm:");
            for (int i=0; i<sizeM; i++) {
               b[i] = scan.nextDouble();
            }
            scan.expect ("vel_output:");
            for (int i=0; i<sizeM; i++) {
               y[i] = scan.nextDouble();
            }
            int nnzG = 0;
            if (sizeG > 0) {
               scan.expect ("GT:");
               gRowOffs = readStructure (
                  scan, gRowOffs, sizeM+1, "GT row offsets", step);
               nnzG = gRowOffs[sizeM]-1;
               gColIdxs = readStructure (
                  scan, gColIdxs, nnzG, "GT column indices", step);
               if (gvals == null) {
                  gvals = new double[nnzG];
               }
               scan.readDoubles (gvals);
               scan.expect ("Rg:");
               scan.readDoubles (rg);
               scan.expect ("bg:");
               for (int i=0; i<sizeG; i++) {
                  b[sizeM+i] = scan.nextDouble();
               }
               scan.expect ("lam_input:");
               scan.readDoubles (lamIn);
               scan.expect ("lam_output:");
               for (int i=0; i<sizeG; i++) {
                  y[sizeM+i] = -scan.nextDouble();
               }
            }
            if (step == 0) {
               buildStructure (
                  data, sizeM, sizeG, mRowOffs, mColIdxs, gRowOffs, gColIdxs);
            }
            double[] vals = new double[data.numVals];
            int k = 0;
            for (int i=0; i<sizeM; i++) {
               for (int p=mRowOffs[i]-1; p<mRowOffs[i+1]-1; p++) {
                  vals[k++] = mvals[p];
               }
               if (sizeG > 0) {
                  for (int p=gRowOffs[i]-1; p<gRowOffs[i+1]-1; p++) {
                     vals[k++] = gvals[p];
                  }
               }
            }
            for (int i=0; i<sizeG; i++) {
               vals[k++] = -rg[i];
            }
            valsList.add (vals);
            rhsList.add (b);
            ysolList.add (y);
            step++;
         }
      }
      data.numSteps = valsList.size();
      data.vals = valsList.toArray (new double[0][]);
      data.rhs = rhsList.toArray (new double[0][]);
      data.ysol = ysolList.toArray (new double[0][]);
      return data;
   }

   /**
    * Builds the upper triangular KKT structure: in each of the first sizeM
    * rows, M's entries followed by GT's (with columns offset by sizeM); then
    * a diagonal entry for each constraint row.
    */
   static void buildStructure (
      ReplayData data, int sizeM, int sizeG, int[] mRowOffs, int[] mColIdxs,
      int[] gRowOffs, int[] gColIdxs) {

      data.sizeM = sizeM;
      data.sizeG = sizeG;
      data.size = sizeM + sizeG;
      int nnzM = mRowOffs[sizeM]-1;
      int nnzG = (sizeG > 0 ? gRowOffs[sizeM]-1 : 0);
      data.numVals = nnzM + nnzG + sizeG;
      data.rowOffs = new int[data.size+1];
      data.colIdxs = new int[data.numVals];
      int k = 0;
      for (int i=0; i<sizeM; i++) {
         data.rowOffs[i] = k+1;
         for (int p=mRowOffs[i]-1; p<mRowOffs[i+1]-1; p++) {
            data.colIdxs[k++] = mColIdxs[p];
         }
         if (sizeG > 0) {
            for (int p=gRowOffs[i]-1; p<gRowOffs[i+1]-1; p++) {
               data.colIdxs[k++] = sizeM + gColIdxs[p];
            }
         }
      }
      for (int i=0; i<sizeG; i++) {
         data.rowOffs[sizeM+i] = k+1;
         data.colIdxs[k++] = sizeM+i+1;
      }
      data.rowOffs[data.size] = k+1;
   }

   /* --- binary cache --- */

   static ByteBuffer myIoBuf =
      ByteBuffer.allocateDirect (1<<23).order (ByteOrder.nativeOrder());

   static void writeInts (FileChannel ch, int[] a) throws IOException {
      int n = myIoBuf.capacity()/4;
      for (int off=0; off<a.length; off+=n) {
         int len = Math.min (n, a.length-off);
         myIoBuf.clear();
         myIoBuf.asIntBuffer().put (a, off, len);
         myIoBuf.limit (4*len);
         while (myIoBuf.hasRemaining()) {
            ch.write (myIoBuf);
         }
      }
   }

   static void writeDoubles (FileChannel ch, double[] a) throws IOException {
      int n = myIoBuf.capacity()/8;
      for (int off=0; off<a.length; off+=n) {
         int len = Math.min (n, a.length-off);
         myIoBuf.clear();
         myIoBuf.asDoubleBuffer().put (a, off, len);
         myIoBuf.limit (8*len);
         while (myIoBuf.hasRemaining()) {
            ch.write (myIoBuf);
         }
      }
   }

   static void fill (FileChannel ch, int nbytes) throws IOException {
      myIoBuf.clear();
      myIoBuf.limit (nbytes);
      while (myIoBuf.hasRemaining()) {
         if (ch.read (myIoBuf) < 0) {
            throw new EOFException ("unexpected end of cache file");
         }
      }
      myIoBuf.flip();
   }

   static void readInts (FileChannel ch, int[] a) throws IOException {
      int n = myIoBuf.capacity()/4;
      for (int off=0; off<a.length; off+=n) {
         int len = Math.min (n, a.length-off);
         fill (ch, 4*len);
         myIoBuf.asIntBuffer().get (a, off, len);
      }
   }

   static void readDoubles (FileChannel ch, double[] a) throws IOException {
      int n = myIoBuf.capacity()/8;
      for (int off=0; off<a.length; off+=n) {
         int len = Math.min (n, a.length-off);
         fill (ch, 8*len);
         myIoBuf.asDoubleBuffer().get (a, off, len);
      }
   }

   static void writeCache (ReplayData d, File file) throws IOException {
      try (FileChannel ch = FileChannel.open (
              file.toPath(), StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING,
              StandardOpenOption.WRITE)) {
         writeInts (ch, new int[] {
               CACHE_MAGIC, d.sizeM, d.sizeG, d.numVals, d.numSteps });
         writeInts (ch, d.rowOffs);
         writeInts (ch, d.colIdxs);
         for (int k=0; k<d.numSteps; k++) {
            writeDoubles (ch, d.vals[k]);
            writeDoubles (ch, d.rhs[k]);
            writeDoubles (ch, d.ysol[k]);
         }
      }
   }

   static ReplayData readCache (File file, int maxSteps) throws IOException {
      ReplayData d = new ReplayData();
      try (FileChannel ch = FileChannel.open (
              file.toPath(), StandardOpenOption.READ)) {
         int[] hdr = new int[5];
         readInts (ch, hdr);
         if (hdr[0] != CACHE_MAGIC) {
            throw new IOException ("bad cache file " + file);
         }
         d.sizeM = hdr[1];
         d.sizeG = hdr[2];
         d.size = d.sizeM + d.sizeG;
         d.numVals = hdr[3];
         d.numSteps = Math.min (hdr[4], maxSteps);
         d.rowOffs = new int[d.size+1];
         d.colIdxs = new int[d.numVals];
         readInts (ch, d.rowOffs);
         readInts (ch, d.colIdxs);
         d.vals = new double[d.numSteps][];
         d.rhs = new double[d.numSteps][];
         d.ysol = new double[d.numSteps][];
         for (int k=0; k<d.numSteps; k++) {
            d.vals[k] = new double[d.numVals];
            d.rhs[k] = new double[d.size];
            d.ysol[k] = new double[d.size];
            readDoubles (ch, d.vals[k]);
            readDoubles (ch, d.rhs[k]);
            readDoubles (ch, d.ysol[k]);
         }
      }
      return d;
   }

   ReplayData loadData() throws IOException {
      File dataFile = new File (myDataFile);
      String name = dataFile.getName().replaceFirst ("\\.txt$", "");
      File cacheFile = null;
      if (myCacheDir != null) {
         new File (myCacheDir).mkdirs();
         cacheFile = new File (myCacheDir, name + ".bin");
      }
      long t0 = System.nanoTime();
      ReplayData data;
      if (cacheFile != null && cacheFile.exists() &&
          cacheFile.lastModified() >= dataFile.lastModified()) {
         data = readCache (cacheFile, myMaxSteps);
         System.out.printf (
            "%s: read cache in %.1f s%n", name, (System.nanoTime()-t0)/1e9);
      }
      else {
         data = readText (dataFile, myMaxSteps);
         System.out.printf (
            "%s: parsed text in %.1f s%n", name, (System.nanoTime()-t0)/1e9);
         if (cacheFile != null) {
            writeCache (data, cacheFile);
         }
      }
      data.name = name;
      System.out.printf (
         "%s: sizeM=%d sizeG=%d nnz(upper)=%d steps=%d%n",
         name, data.sizeM, data.sizeG, data.numVals, data.numSteps);
      return data;
   }

   /* --- solving --- */

   static double norm (double[] v) {
      double sum = 0;
      for (double x : v) {
         sum += x*x;
      }
      return Math.sqrt (sum);
   }

   static double relDiff (double[] a, double[] b) {
      double sum = 0;
      for (int i=0; i<a.length; i++) {
         double d = a[i]-b[i];
         sum += d*d;
      }
      double nb = norm (b);
      return Math.sqrt (sum)/(nb > 0 ? nb : 1);
   }

   double updateAvg (double tnew, double tavg) {
      return (tavg == 0 ? tnew : myTimingWeight*tnew + (1-myTimingWeight)*tavg);
   }

   DirectSolver createSolver (String solverName, String method, int maxSolves) {
      DirectSolver solver =
         SparseSolverId.valueOf (solverName).createDirectSolver();
      ((DirectSolverBase)solver).setNumThreads (myNumThreads);
      solver.setIterativeMaxSolves (maxSolves);
      solver.setIterativeTolerance (Math.pow (10.0, -myTolExp));
      ((DirectSolverBase)solver).setGmresRestart (myRestart);
      if (solver instanceof PardisoSolver) {
         ((PardisoSolver)solver).setUseNativeIterativeSolve (
            method.equals ("NATIVE"));
      }
      if (method.equals ("GMRES") || method.equals ("CGS")) {
         solver.setIterativeMethod (DirectSolver.IterativeMethod.valueOf (method));
      }
      return solver;
   }

   /**
    * Returns the number of preconditioner solves of the last iterative solve,
    * given its return status.
    */
   static int lastSolves (DirectSolver solver, int status) {
      int solves = solver.getLastIterativeSolves();
      // Pardiso's own iterative solve reports CG iterations only
      return solves >= 0 ? solves : Math.abs (status);
   }

   static int perturbedPivots (DirectSolver solver) {
      if (solver instanceof PardisoSolver) {
         return ((PardisoSolver)solver).getNumPerturbedPivots();
      }
      return -1;
   }

   static double msecSince (long t0) {
      return (System.nanoTime()-t0)/1e6;
   }

   void factorAndSolve (DirectSolver solver, double[] vals, double[] y,
                        double[] b) {
      solver.factor (vals);
      if (solver.getState() != DirectSolver.FACTORED) {
         throw new NumericalException (
            "factor failed: " + solver.getErrorMessage());
      }
      solver.solve (y, b);
   }

   void runPolicy (ReplayData d, String solverName, String method,
                   PrintWriter steps, PrintWriter summary) {
      boolean hybrid = !method.equals ("DIRECT");
      DirectSolver solver = createSolver (solverName, method, myMaxSolves);
      double[] y = new double[d.size];

      long t0 = System.nanoTime();
      solver.analyze (d.vals[0], d.colIdxs, d.rowOffs, d.size, Matrix.SYMMETRIC);
      double analyzeMs = msecSince (t0);
      // untimed warm-up, so that first-touch costs are not charged to step 0
      factorAndSolve (solver, d.vals[0], y, d.rhs[0]);

      double avgDirect = 0;
      double avgHybrid = 0;
      double totalMs = 0;
      double directMs = 0;
      double hybridOkMs = 0;
      double hybridFailMs = 0;
      int numDirect = 0;
      int numHybrid = 0;
      int numFails = 0;
      int totalSolves = 0;
      int maxPerturbed = 0;
      double maxResid = 0;
      double maxErr = 0;

      for (int k=0; k<d.numSteps; k++) {
         double[] vals = d.vals[k];
         double[] b = d.rhs[k];
         double hms = 0;
         double dms = 0;
         int solves = 0;
         boolean solved = false;
         if (hybrid && avgDirect > 0 && avgHybrid < myHybridRatio*avgDirect) {
            long t = System.nanoTime();
            int status = solver.iterativeSolve (vals, y, b);
            hms = msecSince (t);
            solves = lastSolves (solver, status);
            totalSolves += solves;
            if (status > 0) {
               solved = true;
               avgHybrid = updateAvg (hms, avgHybrid);
               numHybrid++;
               hybridOkMs += hms;
            }
            else {
               numFails++;
               hybridFailMs += hms;
            }
         }
         if (!solved) {
            long t = System.nanoTime();
            factorAndSolve (solver, vals, y, b);
            dms = msecSince (t);
            avgHybrid = 0;
            avgDirect = updateAvg (dms, avgDirect);
            numDirect++;
            directMs += dms;
            maxPerturbed = Math.max (maxPerturbed, perturbedPivots (solver));
         }
         totalMs += hms + dms;
         double resid = DirectSolver.residual (
            d.rowOffs, d.colIdxs, vals, d.size, y, b, true)/norm (b);
         double err = relDiff (y, d.ysol[k]);
         maxResid = Math.max (maxResid, resid);
         maxErr = Math.max (maxErr, err);
         String action = solved ? "H" : (hms > 0 ? "F" : "D");
         if (steps != null) {
            steps.printf (
               "%s,%s,%d,%s,policy,-1,%d,%s,%.3f,%.3f,%d,%.3e,%.3e,-1%n",
               d.name, solverName, myNumThreads, method, k, action, hms, dms,
               solves, resid, err);
         }
      }
      solver.dispose();

      System.out.printf (
         "  %-7s %-6s total=%9.1f ms  direct=%3d (%8.1f ms)  "+
         "hybrid=%3d (%8.1f ms)  fails=%2d (%7.1f ms)  solves=%4d  "+
         "analyze=%.1f  pertPiv=%d  maxResid=%.1e  maxSolErr=%.1e%n",
         solverName, method, totalMs, numDirect, directMs, numHybrid,
         hybridOkMs, numFails, hybridFailMs, totalSolves, analyzeMs,
         maxPerturbed, maxResid, maxErr);
      if (summary != null) {
         summary.printf (
            "%s,%d,%d,%s,%d,%s,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%d,%.3f,%d,%.3e,"+
            "%.3e%n",
            d.name, d.size, d.numVals, solverName, myNumThreads, method,
            totalMs, numDirect, directMs, numHybrid, hybridOkMs, numFails,
            hybridFailMs, totalSolves, analyzeMs, maxPerturbed, maxResid,
            maxErr);
         summary.flush();
      }
   }

   void runLag (ReplayData d, String solverName, String method, int j0,
                PrintWriter steps) {
      if (j0 >= d.numSteps-1) {
         return;
      }
      DirectSolver solver = createSolver (solverName, method, myLagMaxSolves);
      double[] y = new double[d.size];
      solver.analyze (d.vals[j0], d.colIdxs, d.rowOffs, d.size, Matrix.SYMMETRIC);
      factorAndSolve (solver, d.vals[j0], y, d.rhs[j0]);
      long t = System.nanoTime();
      factorAndSolve (solver, d.vals[j0], y, d.rhs[j0]);
      double directMs = msecSince (t);
      double norm0 = norm (d.vals[j0]);

      System.out.printf (
         "  lag %s %s j0=%d: direct=%.1f ms, perturbed pivots=%d%n",
         solverName, method, j0, directMs, perturbedPivots (solver));
      System.out.println (
         "    step  lag  change    solves  time(ms)  xdirect  resid");
      int consecutiveFails = 0;
      for (int k=j0+1; k<d.numSteps; k++) {
         double[] vals = d.vals[k];
         double[] b = d.rhs[k];
         t = System.nanoTime();
         int status = solver.iterativeSolve (vals, y, b);
         double hms = msecSince (t);
         int solves = lastSolves (solver, status);
         double resid = DirectSolver.residual (
            d.rowOffs, d.colIdxs, vals, d.size, y, b, true)/norm (b);
         // relative change in the matrix values since the factorization
         double change = relDiff (vals, d.vals[j0]);
         System.out.printf (
            "    %4d %4d  %.2e  %4d%s  %8.1f  %6.2f  %.1e%n",
            k, k-j0, change, solves, status > 0 ? " " : "F", hms,
            hms/directMs, resid);
         if (steps != null) {
            steps.printf (
               "%s,%s,%d,%s,lag,%d,%d,%s,%.3f,%.3f,%d,%.3e,-1,%.3e%n",
               d.name, solverName, myNumThreads, method, j0, k,
               status > 0 ? "H" : "F", hms, directMs, solves, resid, change);
         }
         if (status > 0) {
            consecutiveFails = 0;
         }
         else if (++consecutiveFails >= 2) {
            break;
         }
      }
      solver.dispose();
   }

   void run() throws IOException {
      ReplayData data = loadData();
      if (myCacheOnly) {
         return;
      }
      PrintWriter steps = null;
      PrintWriter summary = null;
      if (myStepsFile != null) {
         boolean exists = new File (myStepsFile).exists();
         steps = new PrintWriter (new FileWriter (myStepsFile, true));
         if (!exists) {
            steps.println (
               "dataset,solver,threads,method,mode,j0,step,action,hybridMs,"+
               "directMs,solves,relResid,solErr,valChange");
         }
      }
      if (mySummaryFile != null) {
         boolean exists = new File (mySummaryFile).exists();
         summary = new PrintWriter (new FileWriter (mySummaryFile, true));
         if (!exists) {
            summary.println (
               "dataset,size,nnz,solver,threads,method,totalMs,numDirect,"+
               "directMs,numHybrid,hybridMs,numFails,failMs,solves,analyzeMs,"+
               "pertPiv,maxResid,maxSolErr");
         }
      }
      for (String solverName : mySolvers) {
         for (String method : myMethods) {
            if (method.equals ("NATIVE") && !solverName.equals ("Pardiso")) {
               continue;
            }
            if (myLagStarts != null) {
               if (!method.equals ("DIRECT")) {
                  for (int j0 : myLagStarts) {
                     runLag (data, solverName, method, j0, steps);
                  }
               }
            }
            else {
               runPolicy (data, solverName, method, steps, summary);
            }
            if (steps != null) {
               steps.flush();
            }
         }
      }
      if (steps != null) {
         steps.close();
      }
      if (summary != null) {
         summary.close();
      }
   }

   static int[] parseInts (String str) {
      String[] strs = str.split (",");
      int[] vals = new int[strs.length];
      for (int i=0; i<strs.length; i++) {
         vals[i] = Integer.parseInt (strs[i].trim());
      }
      return vals;
   }

   static void printUsageAndExit() {
      System.out.println (
         "Usage: java maspack.solvers.HybridReplayTiming -data <file.txt>\n"+
         "  [-cache dir] [-cacheOnly] [-out steps.csv] [-summary summary.csv]\n"+
         "  [-solvers Pardiso,Mumps] [-methods DIRECT,NATIVE,GMRES,CGS]\n"+
         "  [-threads 8] [-tolExp 10] [-maxSolves 50] [-restart 20]\n"+
         "  [-steps n] [-lag 0,50] [-lagMaxSolves 100]");
      System.exit (1);
   }

   public static void main (String[] args) throws IOException {
      HybridReplayTiming timing = new HybridReplayTiming();
      for (int i=0; i<args.length; i++) {
         String arg = args[i];
         boolean hasValue = (i+1 < args.length);
         if (arg.equals ("-data") && hasValue) {
            timing.myDataFile = args[++i];
         }
         else if (arg.equals ("-cache") && hasValue) {
            timing.myCacheDir = args[++i];
         }
         else if (arg.equals ("-cacheOnly")) {
            timing.myCacheOnly = true;
         }
         else if (arg.equals ("-out") && hasValue) {
            timing.myStepsFile = args[++i];
         }
         else if (arg.equals ("-summary") && hasValue) {
            timing.mySummaryFile = args[++i];
         }
         else if (arg.equals ("-solvers") && hasValue) {
            timing.mySolvers = args[++i].split (",");
         }
         else if (arg.equals ("-methods") && hasValue) {
            timing.myMethods = args[++i].split (",");
         }
         else if (arg.equals ("-threads") && hasValue) {
            timing.myNumThreads = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-tolExp") && hasValue) {
            timing.myTolExp = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-maxSolves") && hasValue) {
            timing.myMaxSolves = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-restart") && hasValue) {
            timing.myRestart = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-steps") && hasValue) {
            timing.myMaxSteps = Integer.parseInt (args[++i]);
         }
         else if (arg.equals ("-lag") && hasValue) {
            timing.myLagStarts = parseInts (args[++i]);
         }
         else if (arg.equals ("-lagMaxSolves") && hasValue) {
            timing.myLagMaxSolves = Integer.parseInt (args[++i]);
         }
         else {
            printUsageAndExit();
         }
      }
      if (timing.myDataFile == null) {
         printUsageAndExit();
      }
      timing.run();
   }
}
