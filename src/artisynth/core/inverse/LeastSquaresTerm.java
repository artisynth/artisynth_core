/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

public interface LeastSquaresTerm
{

   public void setWeight(double w);
   
   public int getTargetSize();
   
   public int getTerm (
      MatrixNd H, VectorNd b, int rowoff, double t0, double t1);
   
   public void dispose();
   
}
