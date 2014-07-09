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
      double x = randGen.nextDouble();
      return (max-min)*x + min;
   }

   public static void main (String[] args) {

      RandomGenerator.setSeed (0x1234);
      // print out a few random integers as a test
      for (int i=0; i<10; i++) {
         System.out.println (RandomGenerator.get().nextInt());
      }
      
   }
}
