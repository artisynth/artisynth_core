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
import maspack.util.NumericInterval;
import maspack.util.ReaderTokenizer;
import maspack.util.StringHolder;
import maspack.util.BooleanHolder;
import maspack.util.Range;
import maspack.matrix.VectorNi;
import maspack.matrix.Vectori;
import maspack.properties.*;

import java.io.StringReader;

public class VectoriField extends LabeledTextField {
   private static final long serialVersionUID = 1L;

   protected Object myValue = Property.VoidValue;
   Vectori myResultHolder;
   int myVectorSize;
   private String validFmtChars = "dx";
   boolean myAutoClipP = true;

   protected IntegerInterval myRange = new IntegerInterval ();

   public static PropertyList myProps =
      new PropertyList (VectoriField.class, LabeledTextField.class);

   static {
      // Remove range property for now, since no real need for interactive setting
      // myProps.add ("range * *", "numeric range", myDefaultRange, "%.6g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a default VectoriField with an empty label and a vector size of
    * one.
    */
   public VectoriField() {
      this ("", 1);
   }

   /**
    * Creates a new VectoriField with specified label text and number of
    * elements.
    * 
    * @param labelText
    * text for the control label
    * @param vectorSize
    * number of elements in the vector
    */
   public VectoriField (String labelText, int vectorSize) {
      super (labelText, 5 * vectorSize);
      initialize (vectorSize, "%d");
      // setValue (new VectorNi (vectorSize));
   }

   /**
    * Creates a new VectoriField with specified label text and initial value. A
    * format string is provided which specifies how to convert numeric values to
    * text.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the vector
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    */
   public VectoriField (String labelText, Vectori initialValue, String fmtStr) {
      super (labelText, 5 * initialValue.size());
      initialize (initialValue.size(), fmtStr);
      setValue (initialValue);
   }

   private void initialize (int size, String fmtStr) {
      myVectorSize = size;
      setFormat (fmtStr);
      myTextField.setHorizontalAlignment (JTextField.RIGHT);
   }

   /**
    * Returns the current result holder for this control.
    * 
    * @return result holder
    * @see #setResultHolder
    */
   public Vectori getResultHolder() {
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
   public void setResultHolder (Vectori holder) {
      if (holder != null && holder.size() != myVectorSize) {
         throw new IllegalArgumentException ("holder has size "
         + holder.size() + ", expecting " + myVectorSize);
      }
      myResultHolder = holder;
      myValue = myResultHolder;
      updateDisplay();
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
    * Sets the size of the vector associated with this vector field. Changing
    * the vector size will cause the result holder to be cleared, and the
    * current vector value to be reset to a zero vector of the indicated size.
    * The number of columns in the field will also be reset.
    * 
    * @param size
    * new vector size
    */
   public void setVectorSize (int size) {
      if (size < 1) {
         throw new IllegalArgumentException ("size must be greater than 1");
      }
      if (size != myVectorSize) {
         setColumns (5 * size);
         myResultHolder = null;
         myVectorSize = size;
         setValue (new VectorNi (size));
      }
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
      if (myValue instanceof Vectori) {
         return ((Vectori)myValue).get (idx);
      }
      else {
         return 0;
      }
   }

   public VectorNi getVectorValue() {
      if (myValue instanceof Vectori) {
         return new VectorNi ((Vectori)myValue);
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
   public void setRange (int min, int max) {
      if (min > max) { // Not sure this is the best way to handle this
         max = min = (max + min) / 2;
      }
      myRange.set (min, max);
      if (myValue instanceof Vectori) {
         Object chk = myRange.validate ((Vectori)myValue, /* clip= */true, null);
         if (chk != myValue) {
            setValue (chk);
            return;
         }
      }
      updateDisplay();
   }

   public void setRange (NumericInterval range) {
      setRange ((int)range.getLowerBound(), (int)range.getUpperBound());
   }

   public NumericInterval getRange() {
      return new IntegerInterval (myRange);
   }

   public Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      if (isBlank (text)) {
         return setVoidIfPossible (errMsg);
      }
      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (text));
      VectorNi tmp = new VectorNi (myVectorSize);

      try {
         for (int i = 0; i < myVectorSize; i++) {
            if (rtok.nextToken() == ReaderTokenizer.TT_EOF) {
               return illegalValue ("Missing elements (there should be "
                                    + myVectorSize + ")", errMsg);
            }
            else if (!rtok.tokenIsInteger()) {
               return illegalValue (
                  "Malformed integer for element " + i, errMsg);
            }
            tmp.set (i, (int)rtok.nval);
         }
         if (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            if (rtok.tokenIsInteger()) {
               return illegalValue ("Too many elements (there should be "
               + myVectorSize + ")", errMsg);
            }
            else {
               return illegalValue (
                  "Extra characters after last element", errMsg);
            }
         }
         return validValue (tmp, errMsg);
      }
      catch (Exception e) {
         return illegalValue ("Improperly formed vector", errMsg);
      }
   }

   protected String valueToText (Object vec) {
      return ((Vectori)vec).toString (myFmt);
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         if (value instanceof Vectori) { // make a defensive copy of the
                                          // supplied vector
            Vectori vecValue = (Vectori)value;
            if (myResultHolder != null) { // use the result holder as the value
                                          // itself. Reason
               // is that the result holder can be guaranteed to
               // have the Vectori sub-type, since it was supplied
               // by the user.
               for (int i = 0; i < myVectorSize; i++) {
                  myResultHolder.set (i, vecValue.get (i));
               }
               value = myResultHolder;
            }
            else { // make a defensive copy of the supplied vector
               value = new VectorNi (vecValue);
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
      value = validateBasic (value, Vectori.class, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      if (value instanceof Vectori) {
         if (((Vectori)value).size() != myVectorSize) {
            return illegalValue ("value must be a Vectori of size "
            + myVectorSize, errMsg);
         }
         value = myRange.validate ((Vectori)value, myAutoClipP, errMsg);
         return value == Range.IllegalValue ? Property.IllegalValue : value;
      }
      else {
         return validValue (value, errMsg);
      }
   }

   protected Object getInternalValue() {
      // should we return a copy?
      return myValue;
   }

   /**
    * {@inheritDoc}
    */
   public void setGUIVoidEnabled (boolean enable) {
      super.setGUIVoidEnabled (enable);
   }

}
