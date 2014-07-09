/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

public class ConstantFuntion1x1 implements SISOFunction {

   double c;
   
   public ConstantFuntion1x1(double c) {
      setVal(c);
   }
   
   public double getVal() {
      return c;
   }
   
   public void setVal(double val) {
      c = val;
   }
   
   public double eval(double in) {
      return c;
   }

}
