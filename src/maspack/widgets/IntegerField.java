/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.*;
import maspack.util.IntHolder;
import maspack.util.BooleanHolder;
import maspack.util.NumericInterval;
import maspack.util.IntegerInterval;
import maspack.util.NumberFormat;
import maspack.util.StringHolder;
import maspack.util.Range;

import maspack.properties.*;

public class IntegerField extends LabeledTextField {
   private static final long serialVersionUID = 1L;
   IntHolder myHolder;
   Object myValue = Property.VoidValue;
   private String validFmtChars = "diouxX";
   private boolean myAutoClipP = true;

   protected IntegerInterval myRange = new IntegerInterval();
   private static IntegerInterval myDefaultRange = new IntegerInterval();

   private static int defaultNumCols = 8;

   public static PropertyList myProps =
      new PropertyList (IntegerField.class, LabeledTextField.class);

   static {
      // Remove range property for now, since no real need for interactive setting
      //myProps.add ("range * *", "value range", myDefaultRange, "%d");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a default IntField with an empty label.
    */
   public IntegerField() {
      this ("");
   }

   /**
    * Creates an IntField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial integer value
    */
   public IntegerField (String labelText, int initialValue) {
      super (labelText, defaultNumCols);
      initialize ("%d");
      setValue (initialValue);
   }

   /**
    * Creates an IntField with specified label text, initial value, and format
    * for converting numeric values to text.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial int value
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    */
   public IntegerField (String labelText, int initialValue, String fmtStr) {
      super (labelText, defaultNumCols);
      initialize (fmtStr);
      setValue (initialValue);
   }

   /**
    * Creates an IntField with specified label text.
    * 
    * @param labelText
    * text for the control label
    */
   public IntegerField (String labelText) {
      super (labelText, defaultNumCols);
      initialize ("%d");
      setValue (0);
   }

   private void initialize (String fmtStr) {
      setFormat (fmtStr);
      myTextField.setHorizontalAlignment (JTextField.RIGHT);
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

   /**
    * {@inheritDoc}
    * 
    * The format conversion character must be appropriate for floating point
    * values (i.e., one of "diouxX").
    */
   public void setFormat (String fmtStr) {
      setFormat (new NumberFormat (fmtStr));
   }

   /**
    * {@inheritDoc}
    * 
    * The format conversion character must be appropriate for floating point
    * values (i.e., one of "diouxX").
    */
   public void setFormat (NumberFormat fmt) {
      if (validFmtChars.indexOf (fmt.getConversionChar()) == -1) {
         throw new IllegalArgumentException (
            "format character must be one of '" + validFmtChars + "'");
      }
      myFmt = fmt;
   }

   public int getIntValue() {
      if (myValue instanceof Number) {
         return ((Number)myValue).intValue();
      }
      else {
         return 0;
      }
   }

   public double getDoubleValue() {
      if (myValue instanceof Number) {
         return ((Number)myValue).doubleValue();
      }
      else {
         return 0;
      }
   }

   static int parseInt (String text, BooleanHolder clipped)
      throws NumberFormatException {
      boolean negate = false;
      clipped.value = false;
      int value = 0;
      int i = 0;
      int radix = 10;
      int len = text.length();
      while (i < len && Character.isWhitespace (text.charAt (i))) {
         i++;
      }
      if (i < len && text.charAt (i) == '-') {
         negate = true;
         i++;
      }
      else if (i < len && text.charAt (i) == '+') {
         i++;
      }
      if (i == len) {
         throw new NumberFormatException ("No number specified");
      }
      if (text.regionMatches (/* ignoreCase= */true, i, "max", 0, 3)) {
         return negate ? -Integer.MAX_VALUE : Integer.MAX_VALUE;
      }
      else if (text.regionMatches (/* ignoreCase= */true, i, "min", 0, 3)) {
         if (negate) { // because -min = (max+1)
            clipped.value = true;
            return Integer.MAX_VALUE;
         }
         else {
            return Integer.MIN_VALUE;
         }
      }
      if (text.charAt (i) == '0') {
         i++;
         if (i < len) {
            int c = text.charAt (i);
            if (c == 'x' || c == 'X') {
               radix = 16;
               i++;
            }
            else if (c == 'o') {
               radix = 8;
               i++;
            }
         }
         else {
            return 0;
         }
      }
      else {
         radix = 10;
      }
      while (i < len && !Character.isWhitespace (text.charAt (i))) {
         int d = Character.digit (text.charAt (i), radix);
         if (d == -1) {
            throw new NumberFormatException ("Improperly formed number");
         }
         if (negate) {
            if (value >= (Integer.MIN_VALUE + d) / radix) {
               value = value * radix - d;
            }
            else {
               value = Integer.MIN_VALUE;
               clipped.value = true;
            }
         }
         else {
            if (value <= (Integer.MAX_VALUE - d) / radix) {
               value = value * radix + d;
            }
            else {
               value = Integer.MAX_VALUE;
               clipped.value = true;
            }
         }
         i++;
      }
      return value;
   }


   protected int getMinimum() {
      return (int)myRange.getLowerBound();
   }

   protected int getMaximum() {
      return (int)myRange.getUpperBound();
   }

   protected boolean clipValueToRange (IntegerInterval range) {
      int max = (int)range.getUpperBound();
      int min = (int)range.getLowerBound();
      if (myValue instanceof Number) {
         int ivalue = getIntValue();
         if (ivalue > max) {
            setValue (max);
            return true;
         }
         else if (ivalue < min) {
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
    * @see #getMaximum
    * @see #getMinimum
    */
   public void setRange (int min, int max) {
      if (min > max) { // Not sure this is the best way to handle this
         max = min = (max + min) / 2;
      }
      setRange (new IntegerInterval (min, max));
   }

   public void setRange (NumericInterval range) {
      if (!range.equals (myRange)) {
         myRange.set (range);
         clipValueToRange (myRange);
      }
   }

   public IntegerInterval getRange() {
      return new IntegerInterval (myRange);
   }

   protected static String valueToText (Object value, NumberFormat fmt) {
      int x = ((Number)value).intValue();
      if (x == Integer.MIN_VALUE) {
         return "min";
      }
      else if (x == Integer.MAX_VALUE) {
         return "max";
      }
      String s = fmt.format (x).trim();
      if (fmt.getConversionChar() == 'x' && !s.startsWith ("0x")) {
         s = "0x" + s;
      }
      else if (fmt.getConversionChar() == 'X' && !s.startsWith ("0X")) {
         s = "0X" + s;
      }
      else if (fmt.getConversionChar() == 'o') {
         if (s.startsWith ("0") && s.length() > 1) {
            s = s.substring (1);
         }
         s = "0o" + s;
      }
      return s;
   }

   protected String valueToText (Object value) {
      return valueToText (value, myFmt);
   }

   protected Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      if (isBlank (text)) {
         return setVoidIfPossible (errMsg);
      }
      try {
         return validValue (parseInt (text, corrected), errMsg);
      }
      catch (NumberFormatException e) {
         return illegalValue ("Improperly formed number", errMsg);
      }
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (myValue, value)) {
         if (value instanceof Number) {
            int newValue = ((Number)value).intValue();
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

   /**
    * {@inheritDoc}
    */
   public void setGUIVoidEnabled (boolean enable) {
      super.setGUIVoidEnabled (enable);
   }

   // public String[] getActions()
   // {
   // return new String[] { "set range" };
   // }

   // public void actionPerformed (ActionEvent e)
   // {
   // String cmd = e.getActionCommand();
   // if (cmd.equals ("set range"))
   // { Window win = SwingUtilities.getWindowAncestor (this);
   // if (win != null)
   // { IntRangeDialog dialog = IntRangeDialog.createDialog (
   // win, "set range", myMin, myMax);
   // GuiUtils.locateVertically (dialog, this,GuiUtils.BELOW);
   // GuiUtils.locateHorizontally (dialog, this,GuiUtils.CENTER);
   // dialog.show();
   // if (dialog.getReturnValue() == OptionPanel.OK_OPTION)
   // { setRange (dialog.getMinimum(), dialog.getMaximum());
   // updateDisplay();
   // }
   // }
   // }
   // }

}
