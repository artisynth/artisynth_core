/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

public class L2RegularizationTerm extends LeastSquaresTermBase {

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
   
   @Override
   protected void compute (double t0, double t1) {
      H.setIdentity();
      H.scale(Math.sqrt(myWeight));
//      if (TrackingController.isDebugTimestep (t0, t1)) {
//         System.out.println("dt = " + dt + "    |Hl2| = " + H.frobeniusNorm());
//      }
   }
   
   @Override
   public int getRowSize () {
      return mySize; //term is square
   }
}
