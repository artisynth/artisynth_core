/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.util.DoubleHolder;
import maspack.util.NumberFormat;
import maspack.util.InternalErrorException;

public class DoubleSelector extends LabeledComboBox {
   private static final long serialVersionUID = 1L;
   DoubleHolder myHolder;

   /**
    * Creates a DoubleSelector with specified label text, initial value, and
    * values to choose from. Chooser text strings for each of the available
    * values are generated from the values themselves using a printf-style
    * conversion format; for a description of the format string syntax, see
    * {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial double value
    * @param values
    * set of values which may be selected
    * @param fmtStr
    * printf-style format string for converting allowed values into chooser text
    * values.
    */

   public DoubleSelector (String labelText, double initialValue,
   double[] values, String fmtStr) {
      super (labelText);
      NumberFormat fmt = new NumberFormat (fmtStr);
      String[] names = new String[values.length];
      Double[] dvals = new Double[values.length];
      for (int i = 0; i < names.length; i++) {
         names[i] = fmt.format (values[i]);
         dvals[i] = new Double (values[i]);
      }
      setListItems (names, dvals);
      setValue (initialValue);
   }

   /**
    * Creates a DoubleSelector with specified label text, initial value, and
    * values to choose from. Chooser text strings for each of the available
    * values are supplied by the argument names.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial double value
    * @param values
    * set of values which may be selected
    * @param names
    * names for each of the allowed values
    */

   public DoubleSelector (String labelText, double initialValue,
   double[] values, String[] names) {
      super (labelText);
      if (names.length != values.length) {
         throw new IllegalArgumentException (
            "Must have same number of names and values");
      }
      Double[] dvals = new Double[values.length];
      for (int i = 0; i < names.length; i++) {
         dvals[i] = new Double (values[i]);
      }
      setListItems (names, dvals);
      setValue (initialValue);
   }

   /**
    * Creates a DoubleSelector with specified label text and and values to
    * choose from. Chooser text strings for each of the available values are
    * generated from the values themselves using a printf-style conversion
    * format; for a description of the format string syntax, see
    * {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param labelText
    * text for the control label
    * @param values
    * set of values which may be selected
    * @param fmtStr
    * printf-style format string for converting allowed values into chooser text
    * values.
    */
   public DoubleSelector (String labelText, double[] values, String fmtStr) {
      super (labelText);
      NumberFormat fmt = new NumberFormat (fmtStr);
      String[] names = new String[values.length];
      Double[] dvals = new Double[values.length];
      for (int i = 0; i < names.length; i++) {
         names[i] = fmt.format (values[i]);
         dvals[i] = new Double (values[i]);
      }
      setListItems (names, dvals);
      if (dvals.length > 0) {
         setValue (dvals[0]);
      }
   }

   /**
    * Creates a DoubleSelector with specified label text and values to choose
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

   public DoubleSelector (String labelText, double[] values, String[] names) {
      super (labelText);
      if (names.length != values.length) {
         throw new IllegalArgumentException (
            "Must have same number of names and values");
      }
      Double[] dvals = new Double[values.length];
      for (int i = 0; i < names.length; i++) {
         dvals[i] = new Double (values[i]);
      }
      setListItems (names, dvals);
      if (dvals.length > 0) {
         setValue (dvals[0]);
      }
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */

   public DoubleHolder getResultHolder() {
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

   public void setResultHolder (DoubleHolder holder) {
      myHolder = holder;
   }

   protected Object convertToListObject (Object value)
      throws IllegalValueException {
      if (!(value instanceof Number)) {
         throw new IllegalValueException ("value must be a Number");
      }
      return new Double (((Number)value).doubleValue());
   }

   protected void updateResultHolder (Object value) {
      if (myHolder != null) {
         myHolder.value = ((Double)value).doubleValue();
      }
   }
}
