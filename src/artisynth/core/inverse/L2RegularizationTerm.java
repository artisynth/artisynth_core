/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;

public class L2RegularizationTerm extends LeastSquaresTermBase {

   public static final double defaultWeight = 0.0001;
   
   protected int mySize;
   protected MatrixNd identity = new MatrixNd ();
   protected TrackingController myController;

   
   public L2RegularizationTerm(TrackingController controller) {
      this(controller, controller.numExcitations());
   }
 
   public L2RegularizationTerm(TrackingController controller, int size) {
      super(defaultWeight);
      myController = controller;
      resize (controller.numExcitations ());
   }
   
   public void dispose() {
      // nothing to dispose
   }

   public int getTargetSize() {
      checksize();
      return mySize;
   }

   private void checksize() {
      int m = myController.getExciters ().size ();
      if (mySize != m)
         resize(m);
   }

   private void resize(int size) {
      identity.setSize (size, size);
      identity.setIdentity();
      identity.scale(Math.sqrt(myWeight));
      mySize = size;
   }
   
   public int getTerm (
      MatrixNd H, VectorNd b, int rowoff, double t0, double t1) {
      checksize ();
      H.setSubMatrix(rowoff, 0, identity);
      return rowoff+mySize;
   }

   public void setWeight(double w) {
      identity.scale(Math.sqrt(w)/Math.sqrt(myWeight));
      super.setWeight(w);
   }
   
   public MatrixNd getReg() {
      return identity;
   }

}
