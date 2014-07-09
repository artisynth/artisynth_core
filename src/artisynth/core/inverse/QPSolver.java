/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

public interface QPSolver
{
   
   public enum Type 
   {
      IPOPT,
      MATLAB,
      DANTZIG
   }

   public void solve(double[] x, double[][] Q, double[] f, double[][] A,
      double[] b, double[] lb, double[] ub, double[] x0) 
      throws Exception;

}
