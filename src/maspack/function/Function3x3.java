/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

public abstract class Function3x3 implements MIMOFunction {

   double[] tmpIn = new double[3];
   
   public void eval(double x, double y, double z, double [] out) {
      tmpIn[0] = x;
      tmpIn[1] = y;
      tmpIn[2] = z;
      eval(tmpIn, out);
   }
   
   public int getInputSize() {
      return 3;
   }
   
   public int getOutputSize() {
      return 3;
   }
   
}
