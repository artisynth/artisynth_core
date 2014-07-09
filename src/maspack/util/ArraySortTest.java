/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Random;

/**
 * Test class for ArraySort.
 */
public class ArraySortTest {

   Random myRand = new Random (0x1234);

   int[] randomInts (int n) {
      int[] list = new int[n];
      for (int i=0; i<n; i++) {
         list[i] = i;
      }
      for (int i=0; i<n; i++) {
         int i0 = myRand.nextInt(n);
         int i1 = myRand.nextInt(n);
         if (i0 != i1) {
            int tmp = list[i0];
            list[i0] = list[i1];
            list[i1] = tmp;
         }
      }
      return list;
   }

   void checkSort (int[] keys, double[] vals) {
      checkSort (keys, vals, 0, keys.length-1);
   }

   void checkSort (int[] keys, double[] vals, int lo, int hi) {
      if (keys != null) {
         for (int i=lo; i<=hi; i++) {
            if (i > lo && keys[i] <= keys[i-1]) {
               throw new TestException (
                  "key["+i+"]="+keys[i]+", key["+(i-1)+"]="+keys[i-1]);
            }
         }
      }
      if (vals != null) {
         for (int i=lo; i<=hi; i++) {
            if (i > lo && vals[i] <= vals[i-1]) {
               throw new TestException (
                  "val["+i+"]="+vals[i]+", val["+(i-1)+"]="+vals[i-1]);
            }
         }
      }
   }

   void setKeys (int[] keys, int[] keys0) {
      for (int i=0; i<keys.length; i++) {
         keys[i] = keys0[i];
      }
   }

   void setVals (double[] vals, double[] vals0) {
      for (int i=0; i<vals.length; i++) {
         vals[i] = vals0[i];
      }
   }

   public void timing() {
      FunctionTimer timer = new FunctionTimer();

      int testSize = 8;

      int[] keys0 = randomInts(testSize);
      double[] vals0 = new double[keys0.length];
      for (int i=0; i<vals0.length; i++) {
         vals0[i] = keys0[i];
      }
      int[] keys = new int[keys0.length];
      double[] vals = new double[vals0.length];

      int loopCnt = 100000;

      for (int i=0; i<loopCnt; i++) {
         setKeys (keys, keys0);
         ArraySort.quickSort (keys, vals);
         setKeys (keys, keys0);
         ArraySort.bubbleSort (keys, vals);
      }

      timer.start();
      for (int i=0; i<loopCnt; i++) {
         setKeys (keys, keys0);
         ArraySort.bubbleSort (keys, vals);
      }
      timer.stop();
      System.out.println ("Bubble sort: " + timer.result(loopCnt));

      timer.start();
      for (int i=0; i<loopCnt; i++) {
         setKeys (keys, keys0);
         ArraySort.quickSort (keys, vals);
      }
      timer.stop();
      System.out.println ("Quick sort: " + timer.result(loopCnt));
   }

   public void test() {

      int testSize = 100;

      int[] keys0 = randomInts(testSize);
      double[] vals0 = new double[keys0.length];
      for (int i=0; i<vals0.length; i++) {
         vals0[i] = keys0[i];
      }
      int[] keys = new int[keys0.length];
      double[] vals = new double[vals0.length];

      setKeys (keys, keys0);
      setVals (vals, vals0);
      ArraySort.quickSort (keys, vals);
      checkSort (keys, vals);

      setKeys (keys, keys0);
      setVals (vals, vals0);
      ArraySort.quickSort (keys, vals, 5, 20);
      checkSort (keys, vals, 5, 20);

      setKeys (keys, keys0);
      setVals (vals, vals0);
      ArraySort.quickSort (vals, keys);
      checkSort (keys, vals);

      setKeys (keys, keys0);
      setVals (vals, vals0);
      ArraySort.quickSort (vals, keys, 5, 20);
      checkSort (keys, vals, 5, 20);

      setKeys (keys, keys0);
      setVals (vals, vals0);
      ArraySort.bubbleSort (keys, vals);
      checkSort (keys, vals);

      setKeys (keys, keys0);
      setVals (vals, vals0);
      ArraySort.bubbleSort (keys, vals, 5, 20);
      checkSort (keys, vals, 5, 20);
   }

   private void printUsageAndExit (int code) {
      System.out.println ("Usage: java "+getClass()+" [-timing] [-help]");
      System.exit (code); 
   }

   public static void main (String[] args) {
      ArraySortTest tester = new ArraySortTest();

      boolean doTiming = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else if (args[i].equals ("-help")) {
            tester.printUsageAndExit (0);
         }
         else {
            tester.printUsageAndExit (1);
         }
      }
      if (doTiming) {
         tester.timing();
      }
      else {
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
}
