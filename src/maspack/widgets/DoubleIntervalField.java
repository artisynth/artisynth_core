/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.*;

import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;

import maspack.properties.Property;
import maspack.util.StringHolder;

public class DoubleIntervalField extends LabeledMultiTextField {
   private static final long serialVersionUID = 1L;

   Object myValue = Property.VoidValue;
   private String validFmtChars = "eEfgaA";

   /**
    * Creates a default DoubleRangeField with an empty label.
    */
   public DoubleIntervalField() {
      this ("");
   }

   /**
    * Creates a new DoubleRangeField with a specified label.
    * 
    * @param labelText
    * text for the control label
    */
   public DoubleIntervalField (String labelText) {
      super (labelText, new String[] { "min:", "max:" }, 6);
      setHorizontalAlignment (JTextField.RIGHT);
      setFormat ("%.6g");
      updateDisplay();
   }

   /**
    * Creates a new DoubleRangeField with specified label text and initial
    * value. A format string is provided which specifies how to convert numeric
    * values to text.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial range value
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    * @throws IllegalArgumentException
    * if the size of initialValue vector does not equal vectorSize
    */
   public DoubleIntervalField (
      String labelText, DoubleInterval initialValue, String fmtStr) {
      super (labelText, new String[] { "min:", "max:" }, 6);
      setHorizontalAlignment (JTextField.RIGHT);
      setFormat (fmtStr);
      setValue (initialValue);
   }

   /**
    * {@inheritDoc}
    * 
    * The format conversion character must be appropriate for floating point
    * values (i.e., one of "eEfgaA").
    */
   public void setFormat (String fmtStr) {
      setFormat (new NumberFormat (fmtStr));
   }

   /**
    * {@inheritDoc}
    * 
    * The format conversion character must be appropriate for floating point
    * values (i.e., one of "eEfgaA").
    */
   public void setFormat (NumberFormat fmt) {
      if (validFmtChars.indexOf (fmt.getConversionChar()) == -1) {
         throw new IllegalArgumentException (
            "format character must be one of '" + validFmtChars + "'");
      }
      myFmt = fmt;
   }

   public double getLowerBound() {
      if (myValue instanceof DoubleInterval) {
         return ((DoubleInterval)myValue).getLowerBound();
      }
      else {
         return Double.NEGATIVE_INFINITY;
      }
   }

   public double getUpperBound() {
      if (myValue instanceof DoubleInterval) {
         return ((DoubleInterval)myValue).getUpperBound();
      }
      else {
         return Double.POSITIVE_INFINITY;
      }
   }

   public DoubleInterval getRangeValue() {
      if (myValue instanceof DoubleInterval) {
         return new DoubleInterval ((DoubleInterval)myValue);
      }
      else {
         return null;
      }
   }

   @Override
   public Object textToValue (
      String[] text, boolean[] corrected, StringHolder errMsg) {
      DoubleInterval tmp = new DoubleInterval();
      if ((LabeledTextField.isBlank (text[0]) ||
           LabeledTextField.isBlank (text[1]))) {
         return setVoidIfPossible (errMsg);
      }
      try {
         tmp.setLowerBound (DoubleField.parseDouble (text[0]));
         corrected[0] = false;
      }
      catch (Exception e) {
         return illegalValue ("Improperly formed number for minimum", errMsg);
      }
      try {
         tmp.setUpperBound (DoubleField.parseDouble (text[1]));
         corrected[1] = false;
      }
      catch (Exception e) {
         return illegalValue ("Improperly formed number for maximum", errMsg);
      }
      return validValue (tmp, errMsg);
   }

   @Override
   protected String[] valueToText (Object obj) {
      String[] strs = new String[2];
      if (obj instanceof DoubleInterval) {
         DoubleInterval rng = (DoubleInterval)obj;
         strs[0] = myFmt.format (rng.getLowerBound());
         strs[1] = myFmt.format (rng.getUpperBound());
         return strs;
      }
      else {
         strs[0] = strs[1] = "";
      }
      return strs;
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) { // defensive copy
         if (value instanceof DoubleInterval) {
            value = new DoubleInterval ((DoubleInterval)value);
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, DoubleInterval.class, errMsg);
   }

   /**
    * {@inheritDoc}
    */
   public void setGUIVoidEnabled (boolean enable) {
      super.setGUIVoidEnabled (enable);
   }

   protected Object getInternalValue() {
      // should we return a copy?
      return myValue;
   }
}
