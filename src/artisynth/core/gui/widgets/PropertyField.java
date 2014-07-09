/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.widgets;

import maspack.properties.Property;
import maspack.widgets.PropertyWidget;
import artisynth.core.driver.Main;

public class PropertyField extends ComponentPropertyField {
   /**
    * Creates a PropertyField with specified label text.
    * 
    * @param labelText
    * text for the control label
    * @param ncols
    * approximate width of the text field in columns
    */
   public PropertyField (String labelText, int ncols, Main main) {
      super (labelText, ncols, main);
      addParentButton();
      addPropertySelector();
      setValue (mySelectionManager.getLastSelected());
      updateDisplay();
   }

   /**
    * Sets this field to accept only numeric properties (i.e., those whose
    * values can be mapped onto a vector).
    * 
    * @param enable
    * if true, enables only numeric properties
    */
   public void setNumericOnly (boolean enable) {
      if (myNumericOnly != enable) {
         myNumericOnly = enable;
         // clear to ensure property selection list will be rebuilt ...
         myLastSelectorHost = null;
         if (enable) {
            Property prop = getProperty();
            if (prop != null && !isNumeric (prop.getInfo())) {
               updateValueAndDisplay (getValueForHost());
               return;
            }
         }
         updatePropertySelector(); // will redo property selector entries
      }
   }

   /**
    * Returns true if this field accepts only numeric properties.
    * 
    * @return true if only numeric properties are allowed
    */
   public boolean isNumericOnly() {
      return myNumericOnly;
   }

   /**
    * Sets this field to accept only properties for which widgets can be
    * created.
    * 
    * @param enable
    * if true, enables only properties which are widgetable
    */
   public void setWidgetableOnly (boolean enable) {
      if (myWidgetableOnly != enable) {
         myWidgetableOnly = enable;
         // clear to ensure property selection list will be rebuilt ...
         myLastSelectorHost = null;
         if (enable) {
            Property prop = getProperty();
            if (prop != null && !PropertyWidget.canCreate (prop.getInfo())) {
               updateValueAndDisplay (getValueForHost());
               return;
            }
         }
         updatePropertySelector(); // will redo property selector entries
      }
   }

   /**
    * Returns true if this field accepts only widgetable properties.
    * 
    * @return true if only widgetable properties are allowed
    */
   public boolean isWidgetableOnly() {
      return myWidgetableOnly;
   }


}
