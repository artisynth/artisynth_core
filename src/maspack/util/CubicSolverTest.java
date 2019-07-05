/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.ArrayList;
import java.util.Random;

/**
 * Test the cubic roots methods in CubicRoots.java
 */
public class CubicSolverTest {

   boolean myUseAlgebraicRoots = true;
   boolean verbose = false;
   
   public CubicSolverTest() {
   }

   public double[] coefsFromRoots (
      double r0, double r1, double r2, boolean oneRealRoot) {
      
      double[] coefs = new double[4];

      if (oneRealRoot) {
         // then r1 and r2 define the complex double roots r1+r2*i, r1-r2*i
         double r1mag2 = r1*r1 + r2*r2;
         coefs[0] = 1;
         coefs[1] = -(r0+2*r1);
         coefs[2] = (2*r0*r1 + r1mag2);
         coefs[3] = -r0*r1mag2;
         
      }
      else {
         coefs[0] = 1;
         coefs[1] = -(r0+r1+r2);
         coefs[2] = (r0*r1 + r2*r0 + r1*r2);
         coefs[3] = -r0*r1*r2;
      }
      return coefs;
   }
      

   public void roottest (
      double r0, double r1, double r2,
      boolean oneRealRoot, double x0, double x1) {
      
      ArrayList<Double> checkList = new ArrayList<Double>();

      if (r0 >= x0 && r0 <= x1) {
         checkList.add (r0);
      }
      if (!oneRealRoot) {
         if (r1 != r0 && r1 >= x0 && r1 <= x1) {
            checkList.add (r1);
         }
         if (r2 != r1 && r2 >= x0 && r2 <= x1) {
            checkList.add (r2);
         }
      }
      double[] check = new double[checkList.size()];
      for (int i=0; i<check.length; i++) {
         check[i] = checkList.get(i);
      }

      double[] coefs = coefsFromRoots (r0, r1, r2, oneRealRoot);

      dotest (coefs[0], coefs[1], coefs[2], coefs[3], x0, x1, check);
   }

   public double dotest (double a, double b, double c, double d,
                       double x0, double x1, double ... check) {
      double[] roots = new double[3];

      TestException ex = null;

      double ctol = 1e-8*Math.abs(x1-x0);

      double maxerr = 0;
      int nroots;
      if (myUseAlgebraicRoots) {
         nroots = CubicSolver.getRootsAlgebraic (roots, a, b, c, d, x0, x1);
      }
      else {
         nroots = CubicSolver.getRoots (roots, a, b, c, d, x0, x1);
      }
      
      if (nroots != check.length) {
         ex = new TestException ("got "+nroots+", expecting "+check.length);
      }
      else {
         for (int i=0; i<nroots; i++) {
            double err = Math.abs (roots[i]-check[i]);
            if (err >= ctol) {
               ex = new TestException (
                  "root "+i+" = "+roots[i]+", expecting "+check[i]);
               break;
            }
            if (err > maxerr) {
               maxerr = err;
            }
         }
      }
      if (ex != null) {
         System.out.println ("Cubic: a="+a+", b="+b+", c="+c+", d="+d);
         System.out.println ("Range: x0="+x0+", x1="+x1);
         System.out.print ("Computed roots: [ ");
         for (int i=0; i<nroots; i++) {
            System.out.print (roots[i]+" ");
         }
         System.out.println ("]");
         System.out.print ("Expected roots: [ ");
         for (int i=0; i<check.length; i++) {
            System.out.print (check[i]+" ");
         }
         System.out.println ("]");
         throw ex;
      }
      return maxerr;
   }

   double[][] myRandomRoots;
   double[][] myRandomCoefs;

   public void generateRandomCubics (int num, double min, double max) {

      myRandomRoots = new double[num][];
      myRandomCoefs = new double[num][];

      Random rand = RandomGenerator.get();

      for (int i=0; i<num; i++) {
         boolean oneRealRoot = rand.nextBoolean();

         double r0 = (max-min)*rand.nextDouble() + min;
         double r1 = (max-min)*rand.nextDouble() + min;
         double r2 = (max-min)*rand.nextDouble() + min;

         if (oneRealRoot) {
            // then r1 and r2 define two complex conjugate roots; use
            // these only generating coefs

            myRandomRoots[i] = new double[] { r0 };
         }
         else {
            double tmp;
            // do a quick inline sort to put roots in order
            if (r0 > r2) {
               tmp = r0; r0 = r2; r2 = tmp;
            }
            if (r0 > r1) {
               tmp = r0; r0 = r1; r1 = tmp;
            }
            if (r1 > r2) {
               tmp = r1; r1 = r2; r2 = tmp;
            }
            myRandomRoots[i] = new double[] { r0, r1, r2 };
         }
         myRandomCoefs[i] = coefsFromRoots (r0, r1, r2, oneRealRoot);
         // System.out.println (myRandomCoefs[i][0]+" "+
         //                     myRandomCoefs[i][1]+" "+
         //                     myRandomCoefs[i][2]+" "+
         //                     myRandomCoefs[i][3]);

      }
   }

   public double randomRootTest (double min, double max) {

      double maxerr = 0;

      for (int i=0; i<myRandomRoots.length; i++) {

         double[] coefs = myRandomCoefs[i];
         double[] check = myRandomRoots[i];

         double err =
            dotest (coefs[0], coefs[1], coefs[2], coefs[3], min, max, check);
         if (err > maxerr) {
            maxerr = err;
         }
      }
      return maxerr;
   }

   public void cubeRootTiming (String msg) {

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<myRandomRoots.length; i++) {

         Math.pow (myRandomRoots[i][0], 1/3.0);
      }
      timer.stop();
      System.out.println (msg + " " + timer.result(myRandomRoots.length));
   }

   public void acosTiming (String msg) {

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<myRandomRoots.length; i++) {

         Math.acos (-0.5934343111);
      }
      timer.stop();
      System.out.println (msg + " " + timer.result(myRandomRoots.length));
   }

   public void cosTiming (String msg) {

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<myRandomRoots.length; i++) {

         Math.cos (3.1);
      }
      timer.stop();
      System.out.println (msg + " " + timer.result(myRandomRoots.length));
   }

   public void squareRootTiming (String msg) {

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<myRandomRoots.length; i++) {

         Math.sqrt (myRandomRoots[i][0]);
      }
      timer.stop();
      System.out.println (msg + " " + timer.result(myRandomRoots.length));
   }

   public void randomRootTiming (String msg, int cnt, double min, double max) {

      double[] roots = new double[3];

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<cnt; i++) {
         
         for (int j=0; j<myRandomCoefs.length; j++) {

            double[] coefs = myRandomCoefs[j];

            if (myUseAlgebraicRoots) {
               CubicSolver.getRootsAlgebraic (
                  roots, coefs[0], coefs[1], coefs[2], coefs[3], min, max);
            }
            else {
               CubicSolver.getRoots (
                  roots, coefs[0], coefs[1], coefs[2], coefs[3], min, max);
            }
         }
      }
      timer.stop();
      System.out.println (msg + " " + timer.result(cnt*myRandomRoots.length));
   }

   public void test() {

      myUseAlgebraicRoots = false;

      dotest (4.0, 3.0, 2.0, 1.0, -1.0, 1.0,   -0.60582958);

      dotest (1.0,-1.0, 1.0,-1.0, -1.0, 1.0,   1.000000000);

      dotest (1.0, 0.0,-1.0, 0.0, -1.0, 1.0,   -1.00000000, 0, 1.000000000);
      
      dotest (-4.0, 3.0, -2.0, 1.0, -1.0, 1.0,  0.60582958);
      
      dotest (4.0, 5.0, 2.0, 1.0,   -1.0, 1.0, -1.000000);

      dotest (4.0, 6.0, 2.0, 1.0,   -1.0, 1.0  /*no roots*/);

      dotest (4.0, -3.0, 2, 1.0,  -1.0, 1.0,  -0.304480966);

      dotest (1.0, 2.0, 2.0, 1.0, -1.0, 1.0,  -1.0000000);

      dotest (1.0, -2.0, -0.5, 1, -1.0, 1.0,  -0.7071067811, 0.707106781);

      dotest (1.0, -2.0, -0.5, 1, -1.0, 2.0,  -0.707106781, 0.707106781, 2.0);

      dotest (3.0, 1.0,-1.0,-1.0, -1.0, 1.0,  0.7356705613704);

      dotest (3.0, 1.0,-1.0, 0.0, -1.0, 1.0,  -0.767591879, 0, 0.434258545);

      myUseAlgebraicRoots = false;
      roottest (0, 0, 0, false, -1, 1);
      roottest (0.00001, 0.00001, 0.00001, false, -1, 1);
      roottest (1, 1, 1, false, -1, 2);
      roottest (0, 1, 1, false, -1, 2);
      roottest (0, 1, 1.00001, false, -1, 2);
      roottest (0, 1.00001, 1.00002, false, -1, 2);
      roottest (-1, -1, 1, false, -2, 2);
      roottest (-1.00001, -1, 1, false, -2, 2);
      roottest (-1, 0, 1, false, -2, 2);
      roottest (-10000, 0, 1, false, -2, 2);
      roottest (-1, 1000, 2000, false, -2, 2);

      double min = -10;
      double max =  10;
      double err;

      generateRandomCubics (100000, min, max);

      myUseAlgebraicRoots = true;
      err = randomRootTest (min, max);
      if (verbose) {
         System.out.println ("max algebraic err=" + err);
      }
      myUseAlgebraicRoots = false;
      err = randomRootTest (min, max);
      if (verbose) {
         System.out.println ("max Bridson err=" + err);
      }

      if (verbose) {
         
         int nroots = 0;
         for (int i=0; i<myRandomRoots.length; i++) {
            nroots += myRandomRoots[i].length;
         }
         int timingCnt = 1;
         myUseAlgebraicRoots = true;
         randomRootTiming ("Algebraic method: ", timingCnt, min, max);
         myUseAlgebraicRoots = false;
         CubicSolver.iterationCount = 0;
         CubicSolver.bisectionCount = 0;
         randomRootTiming ("Bridson method: ", timingCnt, min, max);
         System.out.println (
            " average iterations: " +
            CubicSolver.iterationCount/(double)myRandomCoefs.length);
         System.out.println (
            " average bisections: " +
            CubicSolver.bisectionCount/(double)myRandomCoefs.length);
         System.out.println (
            " average per root: " +
            CubicSolver.iterationCount/(double)nroots);
         cubeRootTiming ("cube roots: ");
         acosTiming ("acos: ");
         cosTiming ("cos: ");
         squareRootTiming ("square roots: ");
         //dotest (1.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0);
      }      
   }


   private void printUsageAndExit (int code) {
      System.out.println ("Usage: java "+getClass()+" [-verbose] [-help]");
      System.exit (code); 
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      CubicSolverTest tester = new CubicSolverTest();

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-verbose")) {
            tester.verbose = true;
         }
         else if (args[i].equals ("-help")) {
            tester.printUsageAndExit (0);
         }
         else {
            tester.printUsageAndExit (1);
         }
      }
      try {
         tester.test();
         System.out.println ("\nPassed\n"); 
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
}
