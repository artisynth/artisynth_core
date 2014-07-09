/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.JTextField;

import maspack.matrix.Vector;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;
import maspack.util.NumericInterval;
import maspack.util.Range;
import maspack.util.StringHolder;

public class VectorMultiField extends LabeledMultiTextField {
   private static final long serialVersionUID = 1L;

   Object myValue = Property.VoidValue;
   Vector myResultHolder;
   int myVectorSize;
   private String validFmtChars = "eEfgaA";
   boolean myAutoClipP = true;

   private static double inf = Double.POSITIVE_INFINITY;

   protected DoubleInterval myRange = new DoubleInterval (-inf, inf);
   private static DoubleInterval myDefaultRange = new DoubleInterval (-inf, inf);

   public static PropertyList myProps =
      new PropertyList (VectorMultiField.class, LabeledMultiTextField.class);

   static {
      // Remove range property for now, since no real need for interactive setting
      // myProps.add ("range * *", "numeric range", myDefaultRange, "%.6g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a new VectorMultiField with specified label text and number of
    * elements.
    * 
    * @param labelText
    * text for the control label
    * @param vectorSize
    * number of elements in the vector
    */
   public VectorMultiField (String labelText, int vectorSize) {
      super (labelText, vectorSize, 6);
      initialize (vectorSize, "%.6g");
      setValue (new VectorNd (vectorSize));
   }

   /**
    * Creates a new VectorMultiField with specified label text and initial
    * value. A format string is provided which specifies how to convert numeric
    * values to text.
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
   public VectorMultiField (String labelText, Vector initialValue, String fmtStr) {
      super (labelText, initialValue.size(), 6);
      initialize (initialValue.size(), fmtStr);
      setValue (initialValue);
   }

   /**
    * Creates a new VectorMultiField with specified label text, labels for each
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
    * @throws IllegalArgumentException
    * if the number of fieldLabels does not equal the size of the vector as
    * indicated by the initial value.
    */
   public VectorMultiField (String labelText, String[] fieldLabels,
   Vector initialValue, String fmtStr) {
      super (labelText, fieldLabels, 6);
      if (fieldLabels.length != initialValue.size()) {
         throw new IllegalArgumentException ("Number of field labels "
         + fieldLabels.length + " does not equal vector size "
         + initialValue.size());
      }
      initialize (initialValue.size(), fmtStr);
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
   public Vector getResultHolder() {
      return myResultHolder;
   }

   /**
    * Sets the result holder for this control, into which updated` values are
    * copied. No copying is performed if the result holder is set to null.
    * 
    * @param holder
    * new result holder for this control
    * @see #getResultHolder
    * @throws IllegalArgumentException
    * if holder does not have the same size as the vector associated with this
    * control.
    */
   public void setResultHolder (Vector holder) {
      if (holder.size() != myVectorSize) {
         throw new IllegalArgumentException ("holder has size "
         + holder.size() + ", expecting " + myVectorSize);
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

   public double getValue (int idx) {
      if (myValue instanceof Vector) {
         return ((Vector)myValue).get (idx);
      }
      else {
         return 0;
      }
   }

   public VectorNd getVectorValue() {
      if (myValue instanceof Vector) {
         return new VectorNd ((Vector)myValue);
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
   public double getMinimum() {
      return myRange.getLowerBound();
   }

   /**
    * Gets the maximum value associated with this control.
    * 
    * @return maximum control value
    * @see #setRange
    */
   public double getMaximum() {
      return myRange.getUpperBound();
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
   public void setRange (double min, double max) {
      if (min > max) { // Not sure this is the best way to handle this
         max = min = (max + min) / 2;
      }
      myRange.set (min, max);
      if (myValue instanceof Vector) {
         Object chk = myRange.validate ((Vector)myValue, /* clip= */true, null);
         if (chk != myValue) {
            setValue (chk);
         }
      }
      else {
         updateDisplay();
      }
   }

   public void setRange (NumericInterval range) {
      setRange (range.getLowerBound(), range.getUpperBound());
   }

   public NumericInterval getRange() {
      return new DoubleInterval (myRange);
   }

   private double parseDouble (String text, int idx) {
      double val = 0;
      if (text.equals ("inf") || text.equals ("Inf")) {
         return Double.POSITIVE_INFINITY;
      }
      else if (text.equals ("-inf") || text.equals ("-Inf")) {
         return Double.NEGATIVE_INFINITY;
      }
      try {
         val = Double.parseDouble (text);
      }
      catch (NumberFormatException e) {
         throw new IllegalValueException (
            "Missing or malformed number for element " + idx);
      }
      return val;
   }

   @Override
   public Object textToValue (
      String[] text, boolean[] corrected, StringHolder errMsg) {
      VectorNd tmp = new VectorNd (myVectorSize);

      try {
         for (int i = 0; i < myVectorSize; i++) {
            if (LabeledTextField.isBlank (text[i])) {
               return setVoidIfPossible (errMsg);
            }
            double val = parseDouble (text[i], i);
            corrected[i] = false;
            tmp.set (i, val);
         }
      }
      catch (Exception e) {
         return illegalValue (e.getMessage(), errMsg);
      }
      return validValue (tmp, errMsg);
   }

   @Override
   protected String[] valueToText (Object vec) {
      Vector vbase = (Vector)vec;
      String[] strs = new String[myVectorSize];
      for (int i = 0; i < myVectorSize; i++) {
         strs[i] = myFmt.format (vbase.get (i));
      }
      return strs;
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         if (value instanceof Vector) { // make a defensive copy of the
                                          // supplied vector
            VectorNd vecValue = new VectorNd ((Vector)value);
            if (myResultHolder != null) {
               for (int i = 0; i < myVectorSize; i++) {
                  myResultHolder.set (i, vecValue.get (i));
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
      value = validateBasic (value, Vector.class, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      if (value instanceof Vector) {
         if (((Vector)value).size() != myVectorSize) {
            return illegalValue ("value must be a Vector of size "
            + myVectorSize, errMsg);
         }
         value = myRange.validate ((Vector)value, myAutoClipP, errMsg);
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
