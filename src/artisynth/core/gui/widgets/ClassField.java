/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.widgets;

import maspack.util.ClassAliases;
import maspack.util.StringHolder;
import maspack.util.BooleanHolder;
import maspack.widgets.LabeledTextField;
import maspack.properties.Property;

public class ClassField extends LabeledTextField {
   static public final class AllClasses {
      private AllClasses() {
      }
   };

   private static final long serialVersionUID = 1L;
   public static final Class All = AllClasses.class;
   Object myValue = Property.VoidValue;

   /**
    * Creates a ClassField with specified label text.
    * 
    * @param labelText
    * text for the control label
    * @param ncols
    * approximate width of the text field in columns
    */
   public ClassField (String labelText, int ncols) {
      super (labelText, ncols);
      setValue (All);
   }

   /**
    * Creates a ClassField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the class
    * @param ncols
    * approximate width of the text field in columns
    */
   public ClassField (String labelText, Class initialValue, int ncols) {
      super (labelText, ncols);
      setValue (initialValue);
   }

   public String getStringValue() {
      if (myValue instanceof Class) {
         return ((Class)myValue).getName();
      }
      else {
         return "";
      }
   }

   protected Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      if (text.equals ("*")) {
         return validValue (All, errMsg);
      }
      else {
         Class cls = ClassAliases.resolveClass (text);
         if (cls != null) {
            return validValue (cls, errMsg);
         }
         else {
            return illegalValue ("No class found for '" + text + "'", errMsg);
         }
      }
   }

   protected String valueToText (Object value) {
      if (value == All) {
         return "*";
      }
      else {
         return ((Class)value).getName();
      }
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected void updateDisplay() {
      String newText = null;
      if (myValue == Property.VoidValue) {
         newText = "";
      }
      else {
         newText = valueToText (getInternalValue());
      }
      myLastText = newText;
      myTextField.setText (newText);
      setReverseTextBackground (false);
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      Object obj = validateBasic (value, Class.class, errMsg);
      return obj;
   }

   protected Object getInternalValue() {
      return myValue;
   }

}
