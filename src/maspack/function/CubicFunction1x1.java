/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

public class CubicFunction1x1 extends PolyFunction1x1Base {

   public CubicFunction1x1() {
   }
   
   public CubicFunction1x1 (double a3, double a2, double a1, double a0) {
      set (a3, a2, a1, a0);
   }
   
   public int numCoefficients() {
      return 4;
   }
   
   public void set (double a3, double a2, double a1, double a0) {
      setCoefficients (a0, a1, a2, a3);
   }

   public CubicFunction1x1 clone() {
      return (CubicFunction1x1)super.clone();
   }

}
