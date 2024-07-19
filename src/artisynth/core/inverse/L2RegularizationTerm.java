/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import artisynth.core.modelbase.*;
import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.util.*;

/**
 * Cost term that minimizes the 2-norm of the computed excitations.  Used to
 * regularize exciter redundancies.
 */
public class L2RegularizationTerm extends QPCostTermBase {
   
   /*
    * Weight factors to emphasize or de-emphasize certain elements of the regularization term
    */

   public static final double defaultWeight = 0.0001;
   
   public L2RegularizationTerm() {
      super (defaultWeight);
   }
   
   public L2RegularizationTerm (String name) {
      super (defaultWeight);
      setName (name);
   }

   public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
      TrackingController controller = getController();
      if (controller != null) { 
         int nume = controller.numExciters();
         double s = myWeight;
         if (controller.getNormalizeCostTerms()) {
            //  divide by trace of the weight matrix
            double trace = 0;
            for (int i=0; i<nume; i++) {
               trace += controller.myExciters.get(i).getWeight();
            }
            if (trace != 0) {
               s = myWeight/trace;
            }            
         }
         if (!controller.getComputeIncrementally()) {
            for (int i=0; i<nume; i++) {
               double sw = s*controller.myExciters.get(i).getWeight();
               Q.add (i, i, sw);
            }
         }
         else {
            VectorNd prevEx = controller.getExcitations();
            for (int i=0; i<nume; i++) {
               double sw = s*controller.myExciters.get(i).getWeight();
               Q.add (i, i, sw);
               p.add (i, sw*prevEx.get(i));
            }
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void connectToHierarchy (CompositeComponent hcomp) {
      if (getParent() == hcomp && getParent() instanceof TrackingController) {
         TrackingController tcon = (TrackingController)getParent();
         tcon.myL2RegularizationTerm = this;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      if (getParent() == hcomp && getParent() instanceof TrackingController) {
         TrackingController tcon = (TrackingController)getParent();
         tcon.myL2RegularizationTerm = null;
      }
   }

}
