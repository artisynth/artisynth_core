/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

public class DampingTerm extends LeastSquaresTermBase {
   
   protected TrackingController myController;

   public static final double defaultWeight = 0.001;
   
   public DampingTerm(TrackingController controller) {
      this(controller, defaultWeight);
   }
   
   public DampingTerm(TrackingController controller, double weight) {
      super(weight);
      myController = controller;
   }
 
   /**
    * Old constructor: the size is determined automatically from the
    * tracking controller, the second parameter is no longer used.
    * @param controller
    * @param size this parameter is no longer used
    */
   public DampingTerm(TrackingController controller, int size) {
      this(controller, defaultWeight);
   }

   public void dispose() {
      // nothing to dispose
   }

   @Override
   protected void compute (double t0, double t1) {
      H.setIdentity();
      H.scale(Math.sqrt(myWeight));
      myController.getExcitations(f, 0);
      f.scale(Math.sqrt(myWeight));
   }

   @Override
   public int getRowSize () {
      return mySize;
   }
}
