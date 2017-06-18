/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

/**
 * Multi-input, single output
 */
public interface MISOFunction {
   double eval(double[] in);
   int getInputSize();
}
