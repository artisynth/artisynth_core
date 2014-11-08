/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

public interface LeastSquaresTerm extends QPTerm
{
   public int getTerm (MatrixNd H, VectorNd b, int rowoff, double t0, double t1);
   
   /**
    * Returns the number of rows. Note that the number of columns is
    * constrained by the QP-problem and can be queried using getSize().
    * @return number of rows of the least squares problem
    */
   public int getRowSize();
}
