/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.util.*;

/** 
 * Declares some common methods for double and integer slider widgets.
 */
public interface NumericSlider {

   public void setSliderRange (NumericInterval range);

   public NumericInterval getSliderRange();

   public double getDoubleValue();
}