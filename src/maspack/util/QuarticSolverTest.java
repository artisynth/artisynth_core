/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


/**
 * Test the cubic roots methods in CubicRoots.java
 */
public class QuarticSolverTest {

   boolean verbose = false;
   
   public QuarticSolverTest() {
   }

   public double[] coefsFromRoots (
      double r0, double r1, double r2, double r3, int numReal) {
      
      double[] coefs = new double[5];

      if (numReal == 0) {
         // r0, r1, r2, r3 define the complex double roots
         // r0+r1*i, r0-r1*i, r2+r3*i, r2-r3*i, 
         double r01 = r0*r0 + r1*r1;
         double r23 = r2*r2 + r3*r3;
         coefs[0] = 1;
         coefs[1] = -(r0+r1+r2+r3);
         coefs[2] = (r01 + r0*r2 + r0*r3 + r1*r2 + r1*r3 + r23);
         coefs[3] = -(r01*r2 + r01*r3 + r0*r23 + r1*r23);
         coefs[4] = r01*r23;
      }
      else if (numReal == 2) {
         // r2 and r3 define the complex double roots r2+r3*i, r2-r3*i         
         double r23 = r2*r2 + r3*r3;
         coefs[0] = 1;
         coefs[1] = -(r0+r1+r2+r3);
         coefs[2] = (r0*r1 + r0*r2 + r0*r3 + r1*r2 + r1*r3 + r23);
         coefs[3] =  -(r0*r1*r2 + r0*r1*r3 + r0*r23 + r1*r23);
         coefs[4] = r0*r1*r23;
      }
      else {
         // all roots are real
         coefs[0] = 1;
         coefs[1] = -(r0+r1+r2+r3);
         coefs[2] = (r0*r1 + r0*r2 + r0*r3 + r1*r2 + r1*r3 + r2*r3);
         coefs[3] = -(r0*r1*r2 + r0*r1*r3 + r0*r2*r3 + r1*r2*r3);
         coefs[4] = r0*r1*r2*r3;
      }
      return coefs;
   }
      

   public void roottest (
      double r0, double r1, double r2, double r3,
      int numReal, double x0, double x1) {
      
      ArrayList<Double> checkList = new ArrayList<Double>();

      if (numReal >= 2) {
         if (r0 >= x0 && r0 <= x1) {
            checkList.add (r0);
         }
         if (r1 != r0 && r1 >= x0 && r1 <= x1) {
            checkList.add (r1);
         }
      }
      if (numReal == 4) {
         if (r2 != r1 && r2 >= x0 && r2 <= x1) {
            checkList.add (r2);
         }
         if (r3 != r2 && r3 >= x0 && r3 <= x1) {
            checkList.add (r3);
         }
      }
      double[] check = new double[checkList.size()];
      for (int i=0; i<check.length; i++) {
         check[i] = checkList.get(i);
      }

      double[] coefs = coefsFromRoots (r0, r1, r2, r3, numReal);

      dotest (coefs[0], coefs[1], coefs[2], coefs[3], coefs[4], x0, x1, check);
   }

   public double dotest (double a, double b, double c, double d, double e,
                         double x0, double x1, double ... check) {

      double[] roots = new double[4];

      TestException ex = null;

      double ctol = 1e-8*Math.abs(x1-x0);

      double maxerr = 0;
      int nroots;
      nroots = QuarticSolver.getRoots (roots, a, b, c, d, e, x0, x1);

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
         System.out.println (
            "Quartic: a="+a+", b="+b+", c="+c+", d="+d+", e=" + e);
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

   public void generateRandomQuartics (int num, double min, double max) {

      myRandomRoots = new double[num][];
      myRandomCoefs = new double[num][];

      Random rand = RandomGenerator.get();

      for (int i=0; i<num; i++) {
         int numReal = 2*rand.nextInt(3);

         double r0 = (max-min)*rand.nextDouble() + min;
         double r1 = (max-min)*rand.nextDouble() + min;
         double r2 = (max-min)*rand.nextDouble() + min;
         double r3 = (max-min)*rand.nextDouble() + min;

         ArrayList<Double> roots = new ArrayList<Double>();

         if (numReal >= 2) {
            // then r2 and r3 define two complex conjugate roots; use
            // these only generating coefs
            roots.add (r0);
            roots.add (r1);
         }
         if (numReal == 4) {
            roots.add (r2);
            roots.add (r3);
         }
         Collections.sort(roots);
         myRandomRoots[i] = ArraySupport.toDoubleArray (roots);
         myRandomCoefs[i] = coefsFromRoots (r0, r1, r2, r3, numReal);
      }
   }

   public double randomRootTest (double min, double max) {

      double maxerr = 0;

      for (int i=0; i<myRandomRoots.length; i++) {

         double[] coefs = myRandomCoefs[i];
         double[] check = myRandomRoots[i];

         double err =
            dotest (
               coefs[0], coefs[1], coefs[2], coefs[3], coefs[4], min, max, check);
         if (err > maxerr) {
            maxerr = err;
         }
      }
      return maxerr;
   }

   public void randomRootTiming (String msg, int cnt, double min, double max) {

      double[] roots = new double[3];

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (int i=0; i<cnt; i++) {
         
         for (int j=0; j<myRandomCoefs.length; j++) {

            double[] coefs = myRandomCoefs[j];

            QuarticSolver.getRoots (
               roots, coefs[0], coefs[1], coefs[2], coefs[3], coefs[4], min, max);
         }
      }
      timer.stop();
      System.out.println (msg + " " + timer.result(cnt*myRandomRoots.length));
   }

   public void test() {

      int FOUR = 4;

      dotest (1.1746205619679573, -6.060230980489098, -1.0343527114689612,
              -4.504641407043793, -0.8298363248116367, -1, 1,
              -0.183350288619263);
      dotest (5.0, 4.0, -3.0, 2.0, 1.0,  -2.0, 1.0,  -1.37225623, -0.31422057);
      dotest (1.0,-1.0, 1.0, -1.0,-1.0,  -1.0, 2.0,  -0.51879006, 1.290648801);
      dotest (1.0, 0.0, 1.0, -1.0,-1.0,  -1.0, 1.0,  -0.56984029, 1.00000000);
      dotest (5.0, 4.0, 3.0, 2.0, 1.0,    -1.0, 1.0  /*no roots*/);
      // dotest (4.0, -3.0, 2, 1.0,  -1.0, 1.0,  -0.304480966);
      // dotest (1.0, 2.0, 2.0, 1.0, -1.0, 1.0,  -1.0000000);
      // dotest (1.0, -2.0, -0.5, 1, -1.0, 1.0,  -0.7071067811, 0.707106781);
      // dotest (1.0, -2.0, -0.5, 1, -1.0, 2.0,  -0.707106781, 0.707106781, 2.0);
      // dotest (3.0, 1.0,-1.0,-1.0, -1.0, 1.0,  0.7356705613704);
      // dotest (3.0, 1.0,-1.0, 0.0, -1.0, 1.0,  -0.767591879, 0, 0.434258545);


         
      roottest (0, 0, 0, 0, FOUR, -1, 1);

      roottest (1, 1, 1, 1, FOUR, -1, 2);

      roottest (0.00001, 0.00001, 0.00001, 0.0002, FOUR, -1, 1);

      double min = -10;
      double max =  10;
      double err;

      generateRandomQuartics (100000, min, max);
         
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
         QuarticSolver.iterationCount = 0;
         QuarticSolver.bisectionCount = 0;
         randomRootTiming ("Bridson method: ", timingCnt, min, max);
         System.out.println (
            " average iterations: " +
            QuarticSolver.iterationCount/(double)myRandomCoefs.length);
         System.out.println (
            " average bisections: " +
            QuarticSolver.bisectionCount/(double)myRandomCoefs.length);
         System.out.println (
            " average per root: " +
            QuarticSolver.iterationCount/(double)nroots);
      }  

   }


   private void printUsageAndExit (int code) {
      System.out.println ("Usage: java "+getClass()+" [-verbose] [-help]");
      System.exit (code); 
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      QuarticSolverTest tester = new QuarticSolverTest();

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
