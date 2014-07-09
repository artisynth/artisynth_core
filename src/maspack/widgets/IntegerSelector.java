/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.util.IntHolder;
import maspack.util.InternalErrorException;

public class IntegerSelector extends LabeledComboBox {
   private static final long serialVersionUID = 1L;
   IntHolder myHolder;

   /**
    * Creates an IntSelector with specified label text, initial value, and
    * values to choose from. Chooser text strings for each of the available
    * values are generated from the values themselves.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial integer value
    * @param values
    * set of values which may be selected
    */
   public IntegerSelector (String labelText, int initialValue, int[] values) {
      super (labelText);
      String[] names = new String[values.length];
      Integer[] ivals = new Integer[values.length];
      for (int i = 0; i < names.length; i++) {
         names[i] = Integer.toString (values[i]);
         ivals[i] = new Integer (values[i]);
      }
      setListItems (names, ivals);
      setValue (initialValue);
   }

   /**
    * Creates an IntSelector with specified label text, initial value, and
    * values to choose from. Chooser text strings for each of the available
    * values are supplied by the argument names.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial integer value
    * @param values
    * set of values which may be selected
    * @param names
    * names for each of the allowed values
    */
   public IntegerSelector (String labelText, int initialValue, int[] values,
   String[] names) {
      super (labelText);
      if (names.length != values.length) {
         throw new IllegalArgumentException (
            "Must have same number of names and values");
      }
      Integer[] ivals = new Integer[values.length];
      for (int i = 0; i < values.length; i++) {
         ivals[i] = new Integer (values[i]);
      }
      setListItems (names, ivals);
      setValue (initialValue);
   }

   /**
    * Creates an IntSelector with specified label text and values to choose
    * from. Chooser text strings for each of the available values are generated
    * from the values themselves.
    * 
    * @param labelText
    * text for the control label
    * @param values
    * set of values which may be selected
    */
   public IntegerSelector (String labelText, int[] values) {
      super (labelText);
      String[] names = new String[values.length];
      Integer[] ivals = new Integer[values.length];
      for (int i = 0; i < names.length; i++) {
         names[i] = Integer.toString (values[i]);
         ivals[i] = new Integer (values[i]);
      }
      setListItems (names, ivals);
      if (ivals.length > 0) {
         setValue (ivals[0]);
      }
   }

   /**
    * Creates an IntSelector with specified label text and values to choose
    * from. Chooser text strings for each of the available values are supplied
    * by the argument names.
    * 
    * @param labelText
    * text for the control label
    * @param values
    * set of values which may be selected
    * @param names
    * names for each of the allowed values
    */
   public IntegerSelector (String labelText, int[] values, String[] names) {
      super (labelText);
      if (names.length != values.length) {
         throw new IllegalArgumentException (
            "Must have same number of names and values");
      }
      Integer[] ivals = new Integer[values.length];
      for (int i = 0; i < values.length; i++) {
         ivals[i] = new Integer (values[i]);
      }
      setListItems (names, ivals);
      if (ivals.length > 0) {
         setValue (ivals[0]);
      }
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */
   public IntHolder getResultHolder() {
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
   public void setResultHolder (IntHolder holder) {
      myHolder = holder;
   }

   protected Object convertToListObject (Object value)
      throws IllegalValueException {
      if (!(value instanceof Number)) {
         throw new IllegalValueException ("value must be a Number");
      }
      return new Integer (((Number)value).intValue());
   }

   protected void updateResultHolder (Object value) {
      if (myHolder != null) {
         myHolder.value = ((Integer)value).intValue();
      }
   }

}
