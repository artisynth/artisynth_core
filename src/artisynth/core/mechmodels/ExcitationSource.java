/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

/**
 * A container class comprising both an excitation component and a gain value.
 */
public class ExcitationSource {
   protected ExcitationComponent myComp;
   protected double myGain;

   public ExcitationSource (ExcitationComponent comp, double gain) {
      myComp = comp;
      myGain = gain;
   }

   public double getGain() {
      return myGain;
   }

   public void setGain (double gain) {
      myGain = gain;
   }
   
   public ExcitationComponent getComponent() {
      return myComp;
   }
}