/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.JTextField;

import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.ArraySupport;
import maspack.util.BooleanHolder;
import maspack.util.IntegerInterval;
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.StringHolder;

public class IntegerMultiField extends LabeledMultiTextField {
   private static final long serialVersionUID = 1L;

   Object myValue = Property.VoidValue;
   int[] myResultHolder;
   int myVectorSize;
   private String validFmtChars = "dxo";
   boolean myAutoClipP = true;

   private IntegerInterval myRange = new IntegerInterval();
   private static IntegerInterval myDefaultRange = new IntegerInterval();

   public static PropertyList myProps =
      new PropertyList (IntegerMultiField.class, LabeledMultiTextField.class);

   static {
      // Remove range property for now, since no real need for interactive setting
      //myProps.add ("range * *", "value range", myDefaultRange, "%d");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a new IntMultiField with specified label text and number of
    * elements.
    * 
    * @param labelText
    * text for the control label
    * @param vectorSize
    * number of elements in the vector
    */
   public IntegerMultiField (String labelText, int vectorSize) {
      super (labelText, vectorSize, 6);
      initialize (vectorSize, "%d");
      setValue (new int[vectorSize]);
   }

   /**
    * Creates a new IntMultiField with specified label text and initial value. A
    * format string is provided which specifies how to convert numeric values to
    * text.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the vector
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    * @throws IllegalArgumentException
    * if the size of initialValue vector does not equal vectorSize
    */
   public IntegerMultiField (String labelText, int[] initialValue, String fmtStr) {
      super (labelText, initialValue.length, 6);
      initialize (initialValue.length, fmtStr);
      setValue (initialValue);
   }

   /**
    * Creates a new IntMultiField with specified label text, labels for each
    * field, and initial value. A format string is provided which specifies how
    * to convert numeric values to text.
    * 
    * @param labelText
    * text for the control label
    * @param fieldLabels
    * labels for each field
    * @param initialValue
    * initial value for the vector
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    * @param ncols
    * column width of each field. The actual field with is roughly equal to this
    * number times the width of a wide character, such as a 'M'.
    * @throws IllegalArgumentException
    * if the number of fieldLabels does not equal the size of the vector as
    * indicated by the initial value.
    */
   public IntegerMultiField (String labelText, String[] fieldLabels,
   int[] initialValue, String fmtStr, int ncols) {
      super (labelText, fieldLabels, ncols);
      if (fieldLabels.length != initialValue.length) {
         throw new IllegalArgumentException ("Number of field labels "
         + fieldLabels.length + " does not equal vector size "
         + initialValue.length);
      }
      initialize (initialValue.length, fmtStr);
      setValue (initialValue);
   }

   private void initialize (int size, String fmtStr) {
      myVectorSize = size;
      setFormat (fmtStr);
      setHorizontalAlignment (JTextField.RIGHT);
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */
   public int[] getResultHolder() {
      return myResultHolder;
   }

   /**
    * Sets the result holder for this control, into which updated values are
    * copied. No copying is performed if the result holder is set to null.
    * 
    * @param holder
    * new result holder for this control
    * @see #getResultHolder
    * @throws IllegalArgumentException
    * if holder does not have the same size as the vector associated with this
    * control.
    */
   public void setResultHolder (int[] holder) {
      if (holder.length != myVectorSize) {
         throw new IllegalArgumentException ("holder has length "
         + holder.length + ", expecting " + myVectorSize);
      }
      myResultHolder = holder;
   }

   /**
    * Returns the size of the vector value associated with this control.
    * 
    * @return size of this control's vector value.
    */
   public int getVectorSize() {
      return myVectorSize;
   }

   /**
    * {@inheritDoc}
    * 
    * The format conversion character must be appropriate for integer values
    * (i.e., one of "dox").
    */
   public void setFormat (String fmtStr) {
      setFormat (new NumberFormat (fmtStr));
   }

   /**
    * {@inheritDoc}
    * 
    * The format conversion character must be appropriate for integer values
    * (i.e., one of "dox").
    */
   public void setFormat (NumberFormat fmt) {
      if (validFmtChars.indexOf (fmt.getConversionChar()) == -1) {
         throw new IllegalArgumentException (
            "format character must be one of '" + validFmtChars + "'");
      }
      myFmt = fmt;
   }

   public double getValue (int idx) {
      if (myValue instanceof int[]) {
         return ((int[])myValue)[idx];
      }
      else {
         return 0;
      }
   }

   public int[] getVectorValue() {
      if (myValue instanceof int[]) {
         return ArraySupport.copy ((int[])myValue);
      }
      else {
         return null;
      }
   }

   /**
    * Gets the minimum value associated with this control.
    * 
    * @return minimum control value
    * @see #setRange
    */
   public int getMinimum() {
      return (int)myRange.getLowerBound();
   }

   /**
    * Gets the maximum value associated with this control.
    * 
    * @return maximum control value
    * @see #setRange
    */
   public int getMaximum() {
      return (int)myRange.getUpperBound();
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
      myRange.set (min, max);
      if (myValue instanceof int[]) {
         Object chk = myRange.validate ((int[])myValue, /* clip= */true, null);
         if (chk != myValue) {
            setValue (chk);
            return;
         }
      }
      updateDisplay();
   }

   public void setRange (IntegerInterval range) {
      setRange ((int)range.getLowerBound(), (int)range.getUpperBound());
   }

   public IntegerInterval getRange() {
      return new IntegerInterval (myRange);
   }

   @Override
   public Object textToValue (
      String[] text, boolean[] corrected, StringHolder errMsg) {
      int[] tmp = new int[myVectorSize];
      BooleanHolder clipped = new BooleanHolder();

      for (int i = 0; i < myVectorSize; i++) {
         if (LabeledTextField.isBlank (text[i])) {
            return setVoidIfPossible (errMsg);
         }
         try {
            tmp[i] = IntegerField.parseInt (text[i], clipped);
            corrected[i] = clipped.value;
         }
         catch (Exception e) {
            return illegalValue (e.getMessage(), errMsg);
         }
      }
      return validValue (tmp, errMsg);
   }

   @Override
   protected String[] valueToText (Object vec) {
      int[] vbase = (int[])vec;
      String[] strs = new String[myVectorSize];
      for (int i = 0; i < myVectorSize; i++) {
         strs[i] = IntegerField.valueToText (vbase[i], myFmt);
      }
      return strs;
   }

   static boolean vecValuesEqual (Object obj1, Object obj2) {
      if (obj1 == null && obj2 == null) {
         return true;
      }
      else if (obj1 == Property.VoidValue && obj2 == Property.VoidValue) {
         return true;
      }
      else if (obj1 instanceof int[] && obj2 instanceof int[]) {
         return ArraySupport.equals ((int[])obj1, (int[])obj2);
      }
      else {
         return false;
      }
   }

   protected boolean updateInternalValue (Object value) {
      if (!vecValuesEqual (value, myValue)) {
         if (value instanceof int[]) { // make a defensive copy of the supplied
                                       // vector
            int[] vecValue = ArraySupport.copy ((int[])value);
            if (myResultHolder != null) {
               for (int i = 0; i < myVectorSize; i++) {
                  myResultHolder[i] = vecValue[i];
               }
            }
            value = vecValue;
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      value = validateBasic (value, (new int[0]).getClass(), errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      if (value instanceof int[]) {
         if (((int[])value).length != myVectorSize) {
            return illegalValue ("value must be a Vector of size "
            + myVectorSize, errMsg);
         }
         value = myRange.validate ((int[])value, myAutoClipP, errMsg);
         return value == Range.IllegalValue ? Property.IllegalValue : value;
      }
      else {
         return validValue (value, errMsg);
      }
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
