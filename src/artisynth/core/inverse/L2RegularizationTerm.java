/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

public class L2RegularizationTerm extends LeastSquaresTermBase {
   
   /*
    * Weight factors to emphasize or de-emphasize certain elements of the regularization term
    */
   VectorNd weights = null;
   MatrixNd W = null;

   public static final double defaultWeight = 0.0001;
   
   public L2RegularizationTerm() {
      this(defaultWeight);
   }
   
   public L2RegularizationTerm(double weight) {
      super(weight);
   }
 
   /** Old constructor: both arguments are no longer used */
   public L2RegularizationTerm(TrackingController controller, int size) {
      this();
   }
   
   /** Old constructor: argument is no longer used */
   public L2RegularizationTerm(TrackingController controller) {
      this();
   }
   
   /**
    * Set the regularization weights for each activation value.
    * @param w weights for activation values
    */
   public void setWeights(VectorNd w) {
      weights = w;
   }
   
   @Override
   protected void compute (double t0, double t1) {
      int size = H.rowSize ();
      H.setIdentity();
      H.scale(Math.sqrt(myWeight));
      
      if (weights != null && weights.size() != size) {
         // XXX doesn't seem like the best way to go about this
         System.out.println ("Weights and term size mismatched.");
         weights = null;
      }
      // if null weights, do nothing, as though W == Identity
      if (weights != null) {         
         if (W == null) {
            W = new MatrixNd(size, size);
         }
         /* make a diagonal matrix from the weights and multiply H by it */
         W.setZero ();
         for (int i = 0; i < size; i++) {
            W.set (i, i, Math.sqrt (weights.get (i)));
         }
         H.mul (W);
      }
      
//      if (TrackingController.isDebugTimestep (t0, t1)) {
//         System.out.println("dt = " + dt + "    |Hl2| = " + H.frobeniusNorm());
//      }
   }
   
   @Override
   public int getRowSize () {
      return mySize; //term is square
   }
}
