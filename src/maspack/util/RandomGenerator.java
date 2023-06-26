/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

public class RandomGenerator {
   private static Random randGen;

   public static Random get() {
      if (randGen == null) {
         randGen = new Random();
      }
      return randGen;
   }

   public static void set (Random generator) {
      randGen = generator;
   }

   public static void setSeed (int seed) {
      if (randGen == null) {
         randGen = new Random();
      }
      randGen.setSeed (seed);
   }

   public static double nextDouble (double min, double max) {
      double x = get().nextDouble();
      return (max-min)*x + min;
   }
   
   public static double nextDouble () {
      return get().nextDouble();
   }
   
   /**
    * Next random number from a normal (Gaussian) distribution
    * with mean zero and unit standard deviation.
    * @return next normal value
    */
   public static double nextGaussian() {
      return randGen.nextGaussian();
   }
   
   /**
    * Generates a random number from a normal (Gaussian) 
    * distribution with mean mu and s.d. sigma
    * @param mu mean
    * @param sigma standard deviation
    * @return next normal value
    */
   public static double nextGaussian(double mu, double sigma) {
      return randGen.nextGaussian()*sigma+mu;
   }

   /**
    * Generate a random number between min and max, inclusive
    * @param min minimum value
    * @param max maximum value
    * @return random number between min and max
    */
   public static int nextInt (int min, int max) {
      int x = get().nextInt(max-min+1);
      return x+min;
   }
   
   /**
    * Generate a random number between 0 and n-1
    * @param n upper limit
    * @return random number between 0 and n-1
    */
   public static int nextInt (int n) {
      return get().nextInt(n);
   }

   /**
    * Generates a random subsequence of the integers 0, ... n-1.
    *
    * @param n size of the subsequence
    */
   public static int[] randomSubsequence (int n) {
      return randomSubsequence (0, n-1);
   }
   
   /**
    * Generates a random subsequence of the integers in the range
    * {@code min, ... max},
    *
    * @param n size of the subsequence
    */
   public static int[] randomSubsequence (int min, int max) {
      int n = max - min + 1;
      ArrayList<Integer> list = new ArrayList<>();
      for (int i=0; i<n; i++) {
         if (randGen.nextBoolean()) {
            list.add (min+i);
         }
      }
      return ArraySupport.toIntArray (list);
   }
   
   /**
    * Generates a random non-repeating sequence of {@code n} integers in the
    * range {@code min}, {@code max} (inclusive). {@code n} must be
    * {@code <= r}, where {@code r = max - min + 1}.
    *
    * <p>Note: this method has a complexity of {@code O(r)}.
    *
    * @param min minimum sequence value
    * @param max maximum sequence value
    * @param n number of sequence values
    */
   public static int[] randomSequence (int min, int max, int n) {
      if (n > max-min+1) {
         throw new IllegalArgumentException ("n exceeds max-min+1");
      }
      ArrayList<Integer> list = new ArrayList<>(max-min+1);
      for (int k=min; k<=max; k++) {
         list.add (k);
      }
      Collections.shuffle (list, randGen);
      return ArraySupport.toIntArray (list.subList (0, n));
   }

   /**
    * Generates a random sequence of {@code n} numbers that sum to 1.
    *
    * @param n number of sequence values
    */
   public static double[] randomUnityPartition (int n) {
      double[] sequence = new double[n];
      double sum = 0;
      for (int i=0; i<n; i++) {
         sequence[i] = randGen.nextDouble();
         sum += sequence[i];
      }
      for (int i=0; i<n; i++) {
         sequence[i] /= sum;
      }
      return sequence;
   }
   
   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      // print out a few random integers as a test
      for (int i=0; i<10; i++) {
         System.out.println (RandomGenerator.get().nextInt());
      }
      
   }
}
