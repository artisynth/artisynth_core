/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.widgets;

import maspack.widgets.IllegalValueException;
import maspack.widgets.LabeledComboBox;
import artisynth.core.modelbase.ModelComponent;

public class ModelComponentSelector extends LabeledComboBox {

   private static final long serialVersionUID = 1L;
   ModelComponent myHolder;

   /**
    * Creates a default ModelComponentSelector with an empty label and an empty set of
    * values.
    */
   public ModelComponentSelector() {
      this ("", new ModelComponent[0]);
   }

   /**
    * Creates a ModelComponentSelector with specified label text, initial value, and
    * values to choose from.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial string value
    * @param values
    * set of values which may be selected
    */
   public ModelComponentSelector (String labelText, ModelComponent initialValue, ModelComponent[] values) {
      super (labelText);
      setSelections(values);
      setValue (initialValue);
   }

   /**
    * Creates a ModelComponentSelector with specified label text and values to choose
    * from.
    * 
    * @param labelText
    * text for the control label
    * @param values
    * set of values which may be selected
    */
   public ModelComponentSelector (String labelText, ModelComponent[] values) {
      super (labelText);
      setSelections(values);
      if (values.length > 0) {
         setValue (values[0]);
      }
   }

   public void setSelections (ModelComponent[] values, ModelComponent initialValue) {
      setListItems (getNames(values), values);
      setValue (initialValue != null ? initialValue : values[0]);
   }

   private String[] getNames(ModelComponent[] values) {
      String [] names = new String[values.length];
      for (int i=0; i<names.length; i++) {
         names[i] = getNameOrNumber(values[i]);
      }
      return names;
   }
   
   private String getNameOrNumber(ModelComponent mc) {
      if (mc == null) {
         return "null";
      }
      String out = mc.getName();
      if (out == null || "".equals(out)) {
         out = mc.getNumber() + " {" + mc.getClass().getSimpleName() + "}";
      }
      return out;
   }
   
   public void setSelections (ModelComponent[] values) {
      String [] names = getNames(values);
      setListItems (names, values);
   }

   public boolean containsSelection (ModelComponent value) {
      for (NameValuePair pair : myNameValueList) {
         if (pair.name.equals (value)) {
            return true;
         }
      }
      return false;
   }

   protected Object convertToListObject (Object value)
      throws IllegalValueException {
      if (value == null && myNullEnabled) {
         return value;
      }
      if (!(value instanceof ModelComponent)) {
         throw new IllegalValueException ("value must be a model component");
      }
      return value;
   }

   protected void updateResultHolder (Object value) {
      myHolder = (ModelComponent)value;
   }
   
   public void setNullValueEnabled (boolean enable) {
      myNullEnabled = enable;
      if (enable) {
         addNullToList();
      } else {
         removeNullFromList();
      }
   }
   
}
