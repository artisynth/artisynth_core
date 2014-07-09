/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Point3d;

public abstract class Function1x3 implements SIMOFunction {
	
	public abstract Point3d eval(double x); 
	
	@Override
	public void eval(double in, double[] out) {
		Point3d tmp = eval(in);
		out[0] = tmp.x;
		out[1] = tmp.y;
		out[2] = tmp.z;
	}
	
	@Override
	public int getOutputSize() {
		return 1;
	}
   
}
