/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;

/**
 * Defines an object that can emit and recieve muscle control excitations.
 */
public interface ExcitationComponent extends ModelComponent, RequiresInitialize {
   /**
    * Combination rules for excitations.
    */
   public enum CombinationRule {
      /**
       * Indicates that excitations should be combined by direct summation.
       */
      Sum
   }

   /**
    * Sets the rule for combining excitations.
    * 
    * @param rule
    * rule for combining excitations
    */
   public void setCombinationRule (CombinationRule rule);

   /**
    * Return the combination rule for excitations.
    * 
    * @return combination rule for excitations
    */
   public CombinationRule getCombinationRule();

   /**
    * SetsJust reiterating  the primary excitation for this component.
    * 
    * @param e
    * excitation value
    */
   public void setExcitation (double e);

   /**
    * Returns the primary excitation for this component.
    * 
    * @return primary excitation value
    */
   public double getExcitation();

   /**
    * Adds a new excitation source to this Excitable with a specified gain.
    * 
    * @param ex
    * excitatation source to be added
    * @param gain
    * gain for the source
    */
   public void addExcitationSource (ExcitationComponent ex, double gain);

   /**
    * Sets the gain for an excitation source in this component.
    * 
    * @param ex
    * excitatation source whose gain is to be modified
    * @param gain
    * new gain for the source
    * @return false if the source is not present in this component.
    */
   public boolean setExcitationGain (ExcitationComponent ex, double gain);

   /**
    * Gets the gain for an excitation source in this component.
    * 
    * @param ex
    * excitatation source whose gain is to be queried
    * @return source gain value, or -1 if the source is not present in this
    * component.
    */
   public double getExcitationGain (ExcitationComponent ex);

   /**
    * Removes an excitation source from this Excitable. Returns false if the
    * source was not present.
    * 
    * @param ex
    * excitatation source to be removed
    * @return true if the source was present and removed
    */
   public boolean removeExcitationSource (ExcitationComponent ex);

   /**
    * Returns the net excitation for this Excitable. The net excitation is the
    * combination of the primary excitation and the net excitations of all the
    * excitation sources.
    *
    * @return net excitation for this Excitable
    */
   public double getNetExcitation();

//   /**
//    * Returns the default weight that should be used when this excitation
//    * component is being used for inverse actuation control.
//    *
//    * @return default weight for inverse actuation control
//    */
//   public double getDefaultActivationWeight();
}
