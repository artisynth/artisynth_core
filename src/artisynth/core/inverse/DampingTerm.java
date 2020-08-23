/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import artisynth.core.modelbase.*;
import maspack.matrix.*;

/**
 * Cost term that inhibits how quickly excitation values change
 */
public class DampingTerm extends QPCostTermBase {

   public static final double defaultWeight = 1e-5;

   public DampingTerm () {
      this(null);
   }
   
   public DampingTerm (String name) {
      setName (name);
      setWeight (defaultWeight);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
      TrackingController controller = getController();
      if (controller != null) {
         double h = t1 - t0;
         if (h > 0) {
            double s = myWeight/h;
            for (int i=0; i<Q.rowSize(); i++) {
               Q.add (i, i, s);
            }
            if (!controller.getComputeIncrementally()) {
               VectorNd prevEx = controller.getExcitations();
               for (int i=0; i<Q.rowSize(); i++) {
                  p.add (i, -s*prevEx.get(i));
               }
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      if (getParent() == hcomp && getParent() instanceof TrackingController) {
         TrackingController tcon = (TrackingController)getParent();
         tcon.myDampingTerm = this;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      if (getParent() == hcomp && getParent() instanceof TrackingController) {
         TrackingController tcon = (TrackingController)getParent();
         tcon.myDampingTerm = null;
      }
   }

}
