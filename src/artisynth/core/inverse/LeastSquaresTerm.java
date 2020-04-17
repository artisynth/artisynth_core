/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

/**
 * Defines a QPTerm that can be both a cost and a constraint term.
 */
public interface LeastSquaresTerm extends QPCostTerm, QPConstraintTerm {
   
}
