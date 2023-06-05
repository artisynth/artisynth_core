/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

public abstract class Function3x3 implements FunctionNxM {
   
   public int inputSize() {
      return 3;
   }
   
   public int outputSize() {
      return 3;
   }
   
}
