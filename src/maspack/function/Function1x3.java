/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.function;

import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;

public abstract class Function1x3 implements Function1xN {
	
	public abstract Point3d eval(double x); 
	
	@Override
	public void eval (VectorNd out, double in) {
	   out.set (eval(in));
	}
	
	@Override
	public int outputSize() {
		return 1;
	}
   
}
