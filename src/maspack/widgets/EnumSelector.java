/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.util.EnumHolder;
import maspack.util.InternalErrorException;
import maspack.properties.*;

public class EnumSelector extends LabeledComboBox {
   private static final long serialVersionUID = 1L;
   EnumHolder myHolder;

   /**
    * Creates an EnumSelector with an empty label and an empty set of values.
    */
   public EnumSelector() {
      this ("", new Enum[0]);
   }

   /**
    * Creates an EnumSelector with specified label text, initial value, and
    * values to choose from. If <code>values</code> is null, then all the
    * enumerated types associated with <code>initialValue</code> are made
    * available for selection. Chooser text strings for each of the available
    * values are taken directly from the enumerated type names.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial integer value
    * @param values
    * (optional) set of values which may be selected; if null, all values
    * associated with initialValue are used
    */
   public EnumSelector (String labelText, Enum initialValue, Enum[] values) {
      super (labelText);
      if (values == null) {
         values = (Enum[])initialValue.getDeclaringClass().getEnumConstants();
      }
      String[] names = new String[values.length];
      for (int i = 0; i < names.length; i++) {
         names[i] = values[i].toString();
      }
      setListItems (names, values);
      setValue (initialValue);
   }

   /**
    * Creates an EnumSelector with specified label text and values to choose
    * from. Chooser text strings for each of the available values are supplied
    * by the argument <code>names</code>.
    * 
    * @param labelText
    * text for the control label
    * @param values
    * of values which may be selected
    * @param names
    * names for each of the allowed values
    */
   public EnumSelector (String labelText, Enum[] values, String[] names) {
      super (labelText);
      if (names.length != values.length) {
         throw new IllegalArgumentException (
            "Must have same number of names and values");
      }
      setListItems (names, values);
      setValue (values[0]);
   }

   /**
    * Creates an EnumSelector with specified label text and values to choose
    * from.
    * 
    * @param labelText
    * text for the control label
    * @param values
    * set of values which may be selected
    */
   public EnumSelector (String labelText, Enum[] values) {
      super (labelText);
      String[] names = new String[values.length];
      for (int i = 0; i < values.length; i++) {
         names[i] = values[i].toString();
      }
      setListItems (names, values);
      if (values.length > 0) {
         setValue (values[0]);
      }
   }

   public void setSelections (Enum[] values) {
      Enum initialValue = values[0];
      Enum currentValue = (Enum)getValue();
      for (Enum e : values) {
         if (e == currentValue) {
            initialValue = e;
            break;
         }
      }
      setSelections (values, initialValue);
   }

   public void setSelections (Enum[] values, Enum initialValue) {
      String[] names = new String[values.length];
      for (int i = 0; i < values.length; i++) {
         names[i] = values[i].toString();
      }
      setSelections (values, names, initialValue);
   }

   public void setSelections (Enum[] values, String[] names, Enum initialValue) {
      if (!(getValue() instanceof Enum) ||
          initialValue != (Enum)getValue() ||
          !listItemsEqual (names, values)) {
         setListItems (names, values);
         setValue (initialValue != null ? initialValue : values[0]);
      }
      //setListItems (names, values);
      //setValue (initialValue != null ? initialValue : values[0]);
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */
   public EnumHolder getResultHolder() {
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
   public void setResultHolder (EnumHolder holder) {
      myHolder = holder;
   }

   protected Object convertToListObject (Object value)
      throws IllegalValueException {
      if (!(value instanceof Enum)) {
         throw new IllegalValueException ("value must be an enum");
      }
      return value;
   }

   protected void updateResultHolder (Object value) {
      if (myHolder != null) {
         myHolder.value = (Enum)value;
      }
   }

}
