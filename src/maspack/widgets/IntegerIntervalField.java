/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.*;

import maspack.util.IntegerInterval;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorBase;

import java.io.IOException;
import java.io.StringReader;

import maspack.properties.Property;
import maspack.util.StringHolder;
import maspack.util.BooleanHolder;

public class IntegerIntervalField extends LabeledMultiTextField {
   private static final long serialVersionUID = 1L;

   Object myValue = Property.VoidValue;
   private String validFmtChars = "dxo";

   /**
    * Creates a default IntRangeField with an empty label.
    */
   public IntegerIntervalField() {
      this ("");
   }

   /**
    * Creates a new IntRangeField with a specified label.
    * 
    * @param labelText
    * text for the control label
    */
   public IntegerIntervalField (String labelText) {
      super (labelText, new String[] { "min:", "max:" }, 6);
      setHorizontalAlignment (JTextField.RIGHT);
      setFormat ("%d");
      updateDisplay();
   }

   /**
    * Creates a new IntRangeField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial range value
    * @throws IllegalArgumentException
    * if the size of initialValue vector does not equal vectorSize
    */
   public IntegerIntervalField (String labelText, IntegerInterval initialValue) {
      super (labelText, new String[] { "min:", "max:" }, 6);
      setHorizontalAlignment (JTextField.RIGHT);
      setFormat ("%d");
      setValue (initialValue);
   }

   /**
    * Creates a new IntRangeField with specified label text and initial value. A
    * format string is provided which specifies how to convert numeric values to
    * text.
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
   public IntegerIntervalField (String labelText, IntegerInterval initialValue, String fmtStr) {
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

   public int getLowerBound() {
      if (myValue instanceof IntegerInterval) {
         return (int)((IntegerInterval)myValue).getLowerBound();
      }
      else {
         return Integer.MIN_VALUE;
      }
   }

   public int getUpperBound() {
      if (myValue instanceof IntegerInterval) {
         return (int)((IntegerInterval)myValue).getUpperBound();
      }
      else {
         return Integer.MAX_VALUE;
      }
   }

   public IntegerInterval getRangeValue() {
      if (myValue instanceof IntegerInterval) {
         return new IntegerInterval ((IntegerInterval)myValue);
      }
      else {
         return null;
      }
   }

   @Override
   public Object textToValue (
      String[] text, boolean[] corrected, StringHolder errMsg) {
      IntegerInterval tmp = new IntegerInterval();
      BooleanHolder clipped = new BooleanHolder();
      if (LabeledTextField.isBlank (text[0])
      || LabeledTextField.isBlank (text[1])) {
         return setVoidIfPossible (errMsg);
      }
      try {
         tmp.setLowerBound (IntegerField.parseInt (text[0], clipped));
         corrected[0] = clipped.value;
      }
      catch (Exception e) {
         return illegalValue ("Improperly formed number for minimum", errMsg);
      }
      try {
         tmp.setUpperBound (IntegerField.parseInt (text[1], clipped));
         corrected[1] = clipped.value;
      }
      catch (Exception e) {
         return illegalValue ("Improperly formed number for maximum", errMsg);
      }
      return validValue (tmp, errMsg);
   }

   @Override
   protected String[] valueToText (Object obj) {
      String[] strs = new String[2];
      if (obj instanceof IntegerInterval) {
         IntegerInterval rng = (IntegerInterval)obj;
         strs[0] = IntegerField.valueToText (rng.getLowerBound(), myFmt);
         strs[1] = IntegerField.valueToText (rng.getUpperBound(), myFmt);
         return strs;
      }
      else {
         strs[0] = strs[1] = "";
      }
      return strs;
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) { // defensive copy
         if (value instanceof IntegerInterval) {
            value = new IntegerInterval ((IntegerInterval)value);
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, IntegerInterval.class, errMsg);
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
