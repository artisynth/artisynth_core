/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

public class DampingTerm extends LeastSquaresTermBase {

   public static final double defaultWeight = 0.001;
   
   protected int mySize;
   protected MatrixNd identity = new MatrixNd ();
   protected VectorNd prevEx = new VectorNd ();
   protected TrackingController myController;
   
   public DampingTerm(TrackingController controller) {
      this(controller, controller.numExcitations());
   }
 
   public DampingTerm(TrackingController controller, int size) {
      super(defaultWeight);
      myController = controller;
      resize (size);
   }

   public void dispose() {
      // nothing to dispose
   }

   public int getTargetSize() {
      checksize ();
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
      prevEx.setSize (size);
      mySize = size;
   }

   public int getTerm (
      MatrixNd H, VectorNd b, int rowoff, double t0, double t1) {
      checksize();
      myController.getExcitations(prevEx, 0);
      
      //==========================================================
      // ADDED: previously,  we had sqrt(w)I x_n = x_{n-1}, 
      //        we need sqrt(w)I x_n = sqrt(w)x_{n-1}
      //==========================================================      
      prevEx.scale(Math.sqrt(myWeight));	 
      // might we also want to include dt?
      //==========================================================
      
      H.setSubMatrix(rowoff, 0, identity);
      b.setSubVector(rowoff, prevEx);
      return rowoff+mySize;
   }

   public void setWeight(double w) {
      identity.scale(Math.sqrt(w)/Math.sqrt(myWeight));
      super.setWeight(w);
   }

}
