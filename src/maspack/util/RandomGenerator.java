/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Random;

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
   
   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      // print out a few random integers as a test
      for (int i=0; i<10; i++) {
         System.out.println (RandomGenerator.get().nextInt());
      }
      
   }
}
