/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

public class LinearFunction1x1 extends PolyFunction1x1Base {

   public LinearFunction1x1() {
   }
   
   public LinearFunction1x1 (double a1, double a0) {
      set (a1, a0);
   }

   public int numCoefficients() {
      return 2;
   }
   
   public void set (double a1, double a0) {
      setCoefficients (a0, a1);
   }
}
