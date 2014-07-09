/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.TestException;

import java.io.*;

import argparser.*;

public class LemkeContactSolverTest {
   Wrench wapplied = null;
   Twist vel0 = null;
   Vector3d[] nrms = new Vector3d[0];
   Point3d[] pnts = new Point3d[0];
   Point3d bpos = new Point3d();
   double[] fcoefs = new double[0];
   SpatialInertia myInertia;
   double restitution = 0;
   Twist otherBodyVelocity = new Twist();
   double[] normalVelocityLimits = new double[0];

   ContactInfo[] contacts;

   LemkeContactSolver solver = null;

   FunctionTimer timer = new FunctionTimer();

   int randomInputCnt = 0;
   boolean verbose = false;

   public LemkeContactSolverTest (boolean useTradSolver, boolean multiBody) {
      myInertia = new SpatialInertia();
      myInertia.set (1, 1, 1, 1);
      this.useTradSolver = useTradSolver;
      // if (useTradSolver)
      // { if (multiBody)
      // { solver = new ContactSolverMbO();
      // }
      // else
      // { solver = new ContactSolverO();
      // }
      // }
      // else
      {
         // if (multiBody)
         // { // solver = new ContactSolverMbX();
         // }
         // else
         {
            solver = new LemkeContactSolver();
         }
      }
   }

   int[] numFrictionDirs = new int[] { 4 };

   private void buildCircle (int npnts, double angle) {
      pnts = new Point3d[npnts];
      nrms = new Vector3d[npnts];
      fcoefs = new double[npnts];

      double ca = Math.cos (angle);
      double sa = Math.sin (angle);

      for (int i = 0; i < npnts; i++) {
         double ci = Math.cos (2 * Math.PI * i / (double)npnts);
         double si = Math.sin (2 * Math.PI * i / (double)npnts);

         pnts[i] = new Point3d (ci, si, 0);
         nrms[i] = new Vector3d (-ca * ci, -ca * si, sa);
         fcoefs[i] = 0.2;
      }
   }

   public void parseInput (ReaderTokenizer rtok) throws IOException {
      MatrixNd vals = new MatrixNd (0, 0);
      VectorNd vec = new VectorNd (0);

      RandomGenerator.setSeed (0x1234);

      timingStream = null;
      if (doTiming) {
         try {
            timingStream =
               new PrintStream (new BufferedOutputStream (new FileOutputStream (
                  testName + ".m")));
            timingStream.println ("times" + testName + " = [];");
            timingStream.println ("npivs" + testName + " = [];");
            timingStream.println ("npnts" + testName + " = [];");
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }

      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equals ("pnts")) {
               rtok.scanCharacter ('=');
               vec.scan (rtok);
               pnts = new Point3d[vec.size() / 3];
               for (int i = 0; i < pnts.length; i++) {
                  pnts[i] = new Point3d();
                  pnts[i].set (vec.get (i * 3 + 0), vec.get (i * 3 + 1),
                               vec.get (i * 3 + 2));
               }
            }
            else if (rtok.sval.equals ("nrms")) {
               rtok.scanCharacter ('=');
               vec.scan (rtok);
               nrms = new Point3d[vec.size() / 3];
               for (int i = 0; i < nrms.length; i++) {
                  nrms[i] = new Point3d();
                  nrms[i].set (vec.get (i * 3 + 0), vec.get (i * 3 + 1),
                               vec.get (i * 3 + 2));
                  nrms[i].normalize();
               }
            }
            else if (rtok.sval.equals ("fdirs")) {
               rtok.scanCharacter ('=');
               vec.scan (rtok);
               if (vec.size() < 1) {
                  throw new IOException (
                     "friction direction list must have at least one entry");
               }
               numFrictionDirs = new int[vec.size()];
               for (int i = 0; i < vec.size(); i++) {
                  if (vec.get(i) < 4 || ((int)vec.get(i) % 2 != 0)) {
                     throw new IOException (
                        "number of friction directions must be >= 4 and even, line "
                        + rtok.lineno());
                  }
                  numFrictionDirs[i] = (int)vec.get(i);
               }
            }
            else if (rtok.sval.equals ("wext")) {
               rtok.scanCharacter ('=');
               wapplied = new Wrench();
               vel0 = null;
               wapplied.scan (rtok);
            }
            else if (rtok.sval.equals ("vel0")) {
               rtok.scanCharacter ('=');
               wapplied = null;
               vel0 = new Twist();
               vel0.scan (rtok);
            }
            else if (rtok.sval.equals ("bpos")) {
               rtok.scanCharacter ('=');
               bpos.scan (rtok);
            }
            else if (rtok.sval.equals ("fcoefs")) {
               rtok.scanCharacter ('=');
               vec.scan (rtok);
               fcoefs = new double[vec.size()];
               vec.get (fcoefs);
            }
            else if (rtok.sval.equals ("inertia")) {
               rtok.scanCharacter ('=');
               myInertia.scan (rtok);
            }
            else if (rtok.sval.equals ("random")) {
               int cnt = rtok.scanInteger();
               dotest (cnt);
            }
            else if (rtok.sval.equals ("circle")) {
               int npnts = rtok.scanInteger();
               double angle = rtok.scanNumber();
               buildCircle (npnts, Math.toRadians (angle));
            }
            else if (rtok.sval.equals ("restitution")) {
               rtok.scanCharacter ('=');
               restitution = rtok.scanNumber();
            }
            else if (rtok.sval.equals ("normalVelocityLimits")) {
               rtok.scanCharacter ('=');
               vec.scan (rtok);
               normalVelocityLimits = new double[vec.size()];
               vec.get (normalVelocityLimits);
            }
            else if (rtok.sval.equals ("otherBodyVelocity")) {
               rtok.scanCharacter ('=');
               otherBodyVelocity.scan (rtok);
            }
            else if (rtok.sval.equals ("test")) {
               dotest (0);
            }
            else {
               throw new IOException ("Unrecognized keyword " + rtok.sval
               + ", line " + rtok.lineno());
            }
         }
         else {
            throw new IOException ("Unrecognized token " + rtok.ttype
            + ", line " + rtok.lineno());
         }
      }

      if (timingStream != null) {
         try {
            timingStream.close();
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   PrintStream timingStream = null;

   private int runsolver (
      int runcnt, int numd, Wrench wapplied, boolean randomInput) {
      Wrench wapp = null;
      Twist tw0 = null;
      if (wapplied == null && vel0 == null) {
         throw new IllegalArgumentException ("vel0 or wext must be specified");
      }
      if (wapplied != null) {
         wapp = new Wrench();
         if (randomInput) {
            wapp.f.setRandom();
            wapp.m.setRandom();
            wapp.f.z -= 1;
         }
         else {
            wapp.set (wapplied);
         }
      }
      else {
         tw0 = new Twist();
         if (randomInput) {
            tw0.v.setRandom();
            tw0.w.setRandom();
            tw0.v.z -= 1;
         }
         else {
            tw0.set (vel0);
         }
      }

      solver.resetPivotCount();
      Twist vel = new Twist();
      timer.start();
      int status = 0;
      for (int k = 0; k < runcnt; k++) { // status = solver.solve (vel, pnts,
                                          // nrms, fcoefs, pnts.length,
         // bpos, wapp, numd);
         solver.setNumFrictionDirections (numd);
         // solver.setFrictionEnabled (false);
         // solver.setDebug (LemkeContactSolver.SHOW_BASIS |
         // LemkeContactSolver.SHOW_COLS);
         if (wapp != null) {
            status =
               solver.solve (
                  vel, contacts, contacts.length, myInertia, wapp, restitution);
         }
         else {
            status =
               solver.solve (
                  vel, contacts, contacts.length, myInertia, tw0, restitution);
         }
         // System.out.println (solver.getConfigString());
      }
      timer.stop();
      if (randomInput && solver.getPivotCount() == 0) {
         return 0;
      }
      else if (status != LemkeContactSolver.SOLVED) {
         System.out.println ("no solution");
         System.out.println ("wapp=[" + wapp + "]");
         return 0;
      }
      else {
         if (verbose || doTiming) {
            System.out.print (pnts.length + ":" + numd + "("
                              + solver.getPivotCount() / runcnt + " pivots)");
            if (doTiming) {
               System.out.print ("  " + timer.result (runcnt));
            }
            System.out.println ("");
            System.out.print ("vel = [");
            NumberFormat fmt = new NumberFormat ("%8.3f");
            for (int i = 0; i < 6; i++) {
               String numStr = fmt.format (vel.get(i));
               if (numStr.equals ("  -0.000")) {
                  numStr = "   0.000";
               }
               System.out.print (numStr + " ");
            }
            System.out.println ("]");
         }
      }
      return (int)(timer.getTimeUsec() / runcnt);
   }

   private void dotest (int randCnt) throws IOException {
      // check problem dimensions
      // if (pnts.length == 0)
      // { throw new IOException ("pnts not defined");
      // }
      // if (nrms.length == 0)
      // { throw new IOException ("nrms not defined");
      // }
      if (pnts.length != nrms.length) {
         throw new IOException (
            "pnts and nrms must have the same number of coordinates");
      }
      if (fcoefs.length != 0 && pnts.length != fcoefs.length) {
         throw new IOException (
            "number of fcoefs inconsistent with number of points");
      }
      if (normalVelocityLimits.length != 0 && 
          pnts.length != normalVelocityLimits.length) {
         throw new IOException (
            "number of normalVelocityLimits inconsistent with number of points");
      }

      contacts = new ContactInfo[pnts.length];
      for (int i = 0; i < contacts.length; i++) {
         contacts[i] = new ContactInfo (pnts[i], nrms[i]);
         contacts[i].pnt.sub (bpos);
         if (fcoefs.length != 0) {
            contacts[i].setMu (fcoefs[i]);
         }
         else {
            contacts[i].setMu (0);
         }
         if (normalVelocityLimits.length != 0) {
            contacts[i].setNormalVelocityLimit (normalVelocityLimits[i]);
         }
         else {
            contacts[i].setNormalVelocityLimit (0);
         }
         contacts[i].otherBodyVelocity.set (otherBodyVelocity);
      }

      if (randCnt == 0 && randomInputCnt != 0) {
         randCnt = randomInputCnt;
      }

      // int[] numdirs = new int [] {4, 8, 16};
      int[] numdirs = new int[] { 8 };

      for (int i = 0; i < numFrictionDirs.length; i++) {
         int runcnt = doTiming ? timingCnt : 1;
         int numd = numFrictionDirs[i];
         if (randomInputCnt == 0) {
            if (runsolver (runcnt, numd, wapplied, false) == 0) {
               throw new TestException ("Solution failed");
            }
         }
         else {
            int[] times = new int[randCnt];
            int[] pivots = new int[randCnt];
            for (int k = 0; k < randCnt;) {
               int usec = runsolver (runcnt, numd, wapplied, true);
               if (usec != 0) {
                  times[k] = usec;
                  pivots[k] = solver.getPivotCount() / runcnt;
                  k++;
               }
            }
            if (timingStream != null) {
               timingStream.print ("npivs" + testName + " = [ npivs" + testName
               + " [");
               for (int k = 0; k < randCnt; k++) {
                  timingStream.print (" " + pivots[k]);
               }
               timingStream.println ("]' ];");
               timingStream.println ("npnts" + testName + " = [ npnts"
               + testName + " " + pnts.length + " ];");
               timingStream.print ("times" + testName + " = [ times" + testName
               + " [ ");
               for (int k = 0; k < randCnt; k++) {
                  timingStream.print (" " + times[k]);
               }
               timingStream.println ("]' ];");
            }
         }
      }
   }

   boolean doTiming = false;
   int timingCnt = 1;
   boolean useTradSolver = false;
   String testName = "";

   public static void main (String[] args) {
      DoubleHolder epsilon = new DoubleHolder (1e-12);
      StringHolder fileName = new StringHolder ("contactTest.txt");
      StringHolder testName = new StringHolder ("");
      IntHolder debug = new IntHolder (0);
      IntHolder randomInput = new IntHolder (0);
      BooleanHolder vsize = new BooleanHolder (true);
      BooleanHolder reduce = new BooleanHolder (false);
      BooleanHolder smartDirs = new BooleanHolder (false);
      BooleanHolder timing = new BooleanHolder (false);
      IntHolder timingCnt = new IntHolder (1);
      BooleanHolder tradSolver = new BooleanHolder (false);
      BooleanHolder multiBody = new BooleanHolder (false);
      BooleanHolder tradInc = new BooleanHolder (false);
      BooleanHolder verbose = new BooleanHolder (false);

      ArgParser parser = new ArgParser ("java contact.LemkeContactSolverTest");

      parser.addOption ("-f %s # test file name", fileName);
      parser.addOption ("-D %d # debug code", debug);
      parser.addOption ("-vsize %v # variable LCP size", vsize);
      parser.addOption ("-reduce %v # reducible LCP size", reduce);
      parser.addOption ("-timing %v # time the functions", timing);
      parser.addOption ("-timingCnt %d # timing count", timingCnt);
      parser.addOption ("-trad %v # use traditional solver", tradSolver);
      parser.addOption ("-multiBody %v # use multi-body solver", multiBody);
      parser.addOption ("-verbose %v # print solution info", verbose);
      parser.addOption (
         "-tradinc %v # use traditional solver with incremental pivoting",
         tradInc);
      parser.addOption ("-random %d # random input cnt", randomInput);
      parser.addOption ("-smartDirs %v # smart friction directions", smartDirs);
      parser.addOption ("-eps %f # epsilon", epsilon);
      parser.addOption ("-name %s # test name", testName);

      parser.matchAllArgs (args);

      if (tradInc.value) {
         tradSolver.value = true;
      }
      LemkeContactSolverTest tester =
         new LemkeContactSolverTest (tradSolver.value, multiBody.value);

      if (tradInc.value) {
         tester.testName = testName.value + "Tradinc";
      }
      else if (tradSolver.value) {
         tester.testName = testName.value + "Trad";
      }
      else if (vsize.value) {
         tester.testName = testName.value + "Vsize";
      }
      else {
         tester.testName = testName.value + "Reg";
      }

      tester.solver.setDebug (debug.value);
      tester.solver.setEpsilon (epsilon.value);

      if (!tradSolver.value) {
         // if (multiBody.value)
         // { ContactSolverMbX solverx = (ContactSolverMbX)tester.solver;
         // solverx.setVariableSize(vsize.value);
         // // solverx.setReducibleSize(reduce.value);
         // solverx.setSmartDirections(smartDirs.value);
         // }
         // else
         {
            LemkeContactSolver solverx = (LemkeContactSolver)tester.solver;
            solverx.setVariableSize (vsize.value);
            solverx.setReducibleSize (reduce.value);
            solverx.setSmartDirections (smartDirs.value);
         }
      }
      // else
      // { if (tradInc.value)
      // { if (multiBody.value)
      // { ContactSolverMbO solvero = (ContactSolverMbO)tester.solver;
      // solvero.setIncrementalPivoting (true);
      // }
      // else
      // { ContactSolverO solvero = (ContactSolverO)tester.solver;
      // solvero.setIncrementalPivoting (true);
      // }
      // }
      // }

      tester.doTiming = timing.value;
      tester.timingCnt = timingCnt.value;
      tester.randomInputCnt = randomInput.value;

      int cnt = tester.doTiming ? 2 : 1;
      for (int i = 0; i < cnt; i++) {
         try {
            Reader reader = new FileReader (fileName.value);
            ReaderTokenizer rtok = new ReaderTokenizer (reader);
            rtok.parseNumbers (true);
            rtok.commentChar ('#');

            tester.parseInput (rtok);
            reader.close();
         }
         catch (Exception e) {
            e.printStackTrace();
            System.exit (1);
         }
      }

      System.out.println ("\nPassed\n");
   }
}
