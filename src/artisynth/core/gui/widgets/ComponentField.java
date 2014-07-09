/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.widgets;

import artisynth.core.driver.Main;

public class ComponentField extends ComponentPropertyField {
   /**
    * Creates a ComponentField with specified label text.
    * 
    * @param labelText
    * text for the control label
    * @param ncols
    * approximate width of the text field in columns
    */
   public ComponentField (String labelText, int ncols, Main main) {
      super (labelText, ncols, main);
      addParentButton();
      setValue (mySelectionManager.getLastSelected());
      updateDisplay();
   }
}
