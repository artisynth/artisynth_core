/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import maspack.matrix.LinearTransformNd;
import maspack.matrix.VectorNd;

public interface IterativeSolver {
   public enum ToleranceType {
      RelativeResidual, AbsoluteResidual, AbsoluteError
   };

   public double getTolerance();

   public void setTolerance (double tol);

   public ToleranceType getToleranceType();

   public void setToleranceType (ToleranceType type);

   public int getMaxIterations();

   public void setMaxIterations (int max);

   public int getNumIterations();

   public boolean solve (VectorNd x, LinearTransformNd A, VectorNd b);

   public double getRelativeResidual();

   public boolean isCompatible (int matrixType);
}
