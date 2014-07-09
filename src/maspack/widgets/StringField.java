/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.properties.Property;
import maspack.util.StringHolder;
import maspack.util.BooleanHolder;

public class StringField extends LabeledTextField {
   private static final long serialVersionUID = 1L;
   Object myValue = Property.VoidValue;
   StringHolder myHolder;

   /**
    * Creates a StringField with an empty label text and a default number of
    * columns.
    */
   public StringField() {
      this ("", 20);
   }

   /**
    * Creates a StringField with specified label text.
    * 
    * @param labelText
    * text for the control label
    * @param ncols
    * approximate width of the text field in columns
    */
   public StringField (String labelText, int ncols) {
      super (labelText, ncols);
      setNullValueEnabled (true);
      setValue ("");
   }

   /**
    * Creates a StringField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the string
    * @param ncols
    * approximate width of the text field in columns
    */
   public StringField (String labelText, String initialValue, int ncols) {
      super (labelText, ncols);
      setNullValueEnabled (true);
      setValue (initialValue);
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

   public String getStringValue() {
      if (myValue instanceof String) {
         return (String)myValue;
      }
      else {
         return "";
      }
   }

   protected Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      return validValue (text, errMsg);
   }

   protected String valueToText (Object value) {
      if (value instanceof String) {
         return (String)value;
      }
      else {
         return "";
      }
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         if (value != Property.VoidValue) {
            String str = null;
            if (value instanceof String) {
               str = (String)value;
            }
            else if (value != null) {
               str = value.toString();
               value = str;
            }
            if (myHolder != null) {
               myHolder.value = str;
            }
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object validateValue (
      Object value, maspack.util.StringHolder errMsg) {
      return validateBasic (value, Object.class, errMsg);
   }

   protected Object getInternalValue() {
      return myValue;
   }

}
