/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

public interface ScalableUnits {
   /**
    * Scales all distance coordinates.
    * 
    * @param s
    * scaling factor
    */
   public void scaleDistance (double s);

   /**
    * Scales all mass units.
    * 
    * @param s
    * scaling factor
    */
   public void scaleMass (double s);
}
