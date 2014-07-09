/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.util.StringHolder;

public class StringSelector extends LabeledComboBox {
   private static final long serialVersionUID = 1L;
   StringHolder myHolder;

   /**
    * Creates a default StringSelector with an empty label and an empty set of
    * values.
    */
   public StringSelector() {
      this ("", new String[0]);
   }

   /**
    * Creates a StringSelector with specified label text, initial value, and
    * values to choose from.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial string value
    * @param values
    * set of values which may be selected
    */
   public StringSelector (String labelText, String initialValue, String[] values) {
      super (labelText);
      setListItems (values, values);
      setValue (initialValue);
   }

   /**
    * Creates a StringSelector with specified label text and values to choose
    * from.
    * 
    * @param labelText
    * text for the control label
    * @param values
    * set of values which may be selected
    */
   public StringSelector (String labelText, String[] values) {
      super (labelText);
      setListItems (values, values);
      if (values.length > 0) {
         setValue (values[0]);
      }
   }

   public void setSelections (String[] values, String initialValue) {
      setListItems (values, values);
      setValue (initialValue != null ? initialValue : values[0]);
   }

   public void setSelections (String[] values) {
      setListItems (values, values);
   }

   public boolean containsSelection (String value) {
      for (NameValuePair pair : myNameValueList) {
         if (pair.name.equals (value)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */
   public StringHolder getResultHolder() {
      return myHolder;
   }

   /**
    * Sets the result holder for this control, into which updated values are
    * copied. No copying is performed if the result holder is set to null.
    * 
    * @param holder
    * new result holder for this control
    * @see #getResultHolder
    */
   public void setResultHolder (StringHolder holder) {
      myHolder = holder;
   }

   protected Object convertToListObject (Object value)
      throws IllegalValueException {
      if (!(value instanceof String)) {
         throw new IllegalValueException ("value must be a String");
      }
      return value;
   }

   protected void updateResultHolder (Object value) {
      if (myHolder != null) {
         myHolder.value = (String)value;
      }
   }
}
