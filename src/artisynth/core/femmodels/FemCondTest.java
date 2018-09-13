/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;

// Tests the condition number of FEM stiffness matrics
public class FemCondTest {

   private static double eps = 1e-15;


   public static void main (String[] args) {
      FemModel3d fem = new FemModel3d();
      fem.setDensity (1);
      double wx = 2;

      FemFactory.createHexGrid (fem, wx, 1, 1, 1, 1, 1);
      
      for (FemNode n : fem.getNodes()) {
         if (n.getPosition().x <= -wx/2 + eps) {
            n.setDynamic (false);
         }
      }
      int nactive = fem.numActiveComponents();
      System.out.println ("nactive=" + nactive);
      MatrixNd K = new MatrixNd (fem.getActiveStiffnessMatrix());
      System.out.println ("K="+K.rowSize()+"X"+K.colSize());
      SVDecomposition svd = new SVDecomposition();
      svd.factor (K);
      System.out.println ("cond=" + svd.condition());
   }
}
