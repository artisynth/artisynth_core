/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.*;

import maspack.util.DoubleHolder;
import maspack.util.BooleanHolder;
import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;
import maspack.util.NumericInterval;
import maspack.util.Range;
import maspack.util.StringHolder;

import maspack.properties.*;

/**
 * @author lloyd
 * 
 */
public class DoubleField extends LabeledTextField {
   private static final long serialVersionUID = 1L;
   DoubleHolder myHolder;
   Object myValue = Property.VoidValue;
   private String validFmtChars = "eEfgaA";
   private boolean myAutoClipP = true;

   private static FormatRange myFormatRange = new FormatRange("eEfgaA");

   private static double inf = Double.POSITIVE_INFINITY;

   protected DoubleInterval myRange = new DoubleInterval (-inf, inf);
   private static DoubleInterval myDefaultRange = new DoubleInterval (-inf, inf);
   
   private static int defaultNumCols = 8;

   public static PropertyList myProps =
      new PropertyList (DoubleField.class, LabeledTextField.class);

   static {
      // Remove range property for now, since no real need for interactive setting
      // myProps.add ("range * *", "numeric range", myDefaultRange, "%.6g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a DoubleField with an empty label.
    */
   public DoubleField() {
      this ("");
   }

   /**
    * Creates a DoubleField with specified label text and format for converting
    * numeric values to text.
    * 
    * @param labelText
    * text for the control label
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    */
   public DoubleField (String labelText, String fmtStr) {
      super (labelText, defaultNumCols);
      initialize (fmtStr);
      setValue (0);
   }

   /**
    * Creates a DoubleField with specified label text, initial value, and format
    * for converting numeric values to text.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial double value
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    */
   public DoubleField (String labelText, double initialValue, String fmtStr) {
      super (labelText, defaultNumCols);
      initialize (fmtStr);
      setValue (initialValue);
   }

   /**
    * Creates a DoubleField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial double value
    */
   public DoubleField (String labelText, double initialValue) {
      super (labelText, defaultNumCols);
      initialize ("%.6g");
      setValue (initialValue);
   }

   /**
    * Creates a DoubleField with specified label text.
    * 
    * @param labelText
    * text for the control label
    */
   public DoubleField (String labelText) {
      super (labelText, defaultNumCols);
      initialize ("%.6g");
      setValue (0);
   }

   private void initialize (String fmtStr) {
      myFmt = new NumberFormat (fmtStr);
      myTextField.setHorizontalAlignment (JTextField.RIGHT);
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
      super.setFormat (fmt);
   }

   public Range getFormatRange () {
      return myFormatRange;
   }

   public double getDoubleValue() {
      if (myValue instanceof Number) {
         return ((Number)myValue).doubleValue();
      }
      else {
         return 0;
      }
   }

//    /**
//     * Gets the minimum value associated with this control.
//     * 
//     * @return minimum control value
//     * @see #setRange
//     */
//    public double getMinimum() {
//       return myRange.getLowerBound();
//    }

//    /**
//     * Gets the maximum value associated with this control.
//     * 
//     * @return maximum control value
//     * @see #setRange
//     */
//    public double getMaximum() {
//       return myRange.getUpperBound();
//    }

   protected boolean clipValueToRange (DoubleInterval range) {
      double max = range.getUpperBound();
      double min = range.getLowerBound();
      if (myValue instanceof Number) {
         double dvalue = getDoubleValue();
         if (dvalue > max) {
            setValue (max);
            return true;
         }
         else if (dvalue < min) {
            setValue (min);
            return true;
         }
      }
      else {
         updateDisplay();
      }
      return false;
   }

   /**
    * Sets the minimum and maximum values associated with this control.
    * 
    * @param min
    * minimum value
    * @param max
    * maximum value
    * @see #getRange
    */
   public void setRange (double min, double max) {
      if (min > max) { // Not sure this is the best way to handle this
         max = min = (max + min) / 2;
      }
      setRange (new DoubleInterval (min, max));
   }

   public void setRange (NumericInterval range) {
      if (!range.equals (myRange)) {
         myRange.set (range);
         clipValueToRange (myRange);
      }
   }

   public NumericInterval getRange() {
      return new DoubleInterval (myRange);
   }

   static double parseDouble (String text) {
      double val = 0;
      if (text.regionMatches (/* ignoreCase= */true, 0, "inf", 0, 3)) {
         return Double.POSITIVE_INFINITY;
      }
      else if (text.regionMatches (/* ignoreCase= */true, 0, "-inf", 0, 4)) {
         return Double.NEGATIVE_INFINITY;
      }
      try {
         val = Double.parseDouble (text);
      }
      catch (NumberFormatException e) {
         throw new IllegalValueException ("Missing or malformed number");
      }
      return val;
   }

   protected Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      if (isBlank (text)) {
         return setVoidIfPossible (errMsg);
      }
      try {
         double val = Double.parseDouble (text);
         return validValue (Double.parseDouble (text), errMsg);
      }
      catch (NumberFormatException e) {
         return illegalValue ("Improperly formed number", errMsg);
      }
   }

   protected String valueToText (Object value) {
      double x = ((Number)value).doubleValue();
      return myFmt.format (x).trim();
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (myValue, value)) {
         if (value instanceof Number) {
            double newValue = ((Number)value).doubleValue();
            if (myHolder != null) {
               myHolder.value = newValue;
            }
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      value = validateBasic (value, Number.class, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      if (value instanceof Number) {
         value = myRange.validate ((Number)value, myAutoClipP, errMsg);
         return value == Range.IllegalValue ? Property.IllegalValue : value;
      }
      else {
         return validValue (value, errMsg);
      }
   }

   protected Object getInternalValue() {
      return myValue;
   }

   public void actionPerformed (ActionEvent e) {
   }

   /**
    * {@inheritDoc}
    */
   public void setGUIVoidEnabled (boolean enable) {
      super.setGUIVoidEnabled (enable);
   }

   public void setLabelStretchable (boolean stretchable) {
      if (stretchable != isLabelStretchable()) {
         if (stretchable) {
            myTextField.setMinimumSize (myTextField.getPreferredSize());
            myTextField.setMaximumSize (myTextField.getPreferredSize());
         }
         else {
            myTextField.setMinimumSize (null);
            myTextField.setMaximumSize (null);
         }
         super.setLabelStretchable (stretchable);
      }
   }

}
