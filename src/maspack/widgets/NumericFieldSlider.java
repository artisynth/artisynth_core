/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.util.*;

/** 
 * Declares common methods for DoubleFieldSlider and IntFieldSlider that also
 * have fields and hard ranges in addition to being sliders.
 */
public interface NumericFieldSlider extends NumericSlider {

   public void setRange (NumericInterval range);

   public NumericInterval getRange();

   public void setAutoRangingEnabled (boolean enable);

   public boolean getAutoRangingEnabled();
}