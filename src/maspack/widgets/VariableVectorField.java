/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.io.StringReader;

import javax.swing.JTextField;

import maspack.matrix.Vector;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.util.BooleanHolder;
import maspack.util.ReaderTokenizer;
import maspack.util.StringHolder;
import maspack.util.Range;

/**
 * Similar to VectorField, but allows resizing after initialization
 * 
 * @author Antonio
 * 
 */
public class VariableVectorField extends VectorField {
   private static final long serialVersionUID = 4224017737154389561L;

   /**
    * Creates a default VectorField with an empty label and a vector size of
    * one.
    */
   public VariableVectorField () {
      this("", 1);
   }

   /**
    * Creates a new VariableVectorField with specified label text and number of
    * elements.
    * 
    * @param labelText
    * text for the control label
    * @param vectorSize
    * number of elements in the vector
    */
   public VariableVectorField (String labelText, int vectorSize) {
      super(labelText, 5 * vectorSize);
      initialize(vectorSize, "%.6g");
      // setValue (new VectorNd (vectorSize));
   }

   /**
    * Creates a new VariableVectorField with specified label text and initial
    * value. A format string is provided which specifies how to convert numeric
    * values to text.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the vector
    * @param fmtStr
    * format string (printf style; see {@link #setFormat setFormat})
    */
   public VariableVectorField (String labelText, Vector initialValue,
      String fmtStr) {
      super(labelText, 5 * initialValue.size());
      initialize(initialValue.size(), fmtStr);
      setValue(initialValue);
   }

   private void initialize(int size, String fmtStr) {
      myVectorSize = size;
      setFormat(fmtStr);
      myTextField.setHorizontalAlignment(JTextField.RIGHT);
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
   public void setVectorSize(int size) {
      if (size < 0) {
         size = 0;
      }
      if (size != myVectorSize) {
         setColumns(5 * size);
         myResultHolder = null;
         myVectorSize = size;
         setValue(new VectorNd(size));
      }
   }

   public Object textToValue(
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      if (isBlank(text)) {
         return setVoidIfPossible(errMsg);
      }
      ReaderTokenizer rtok = new ReaderTokenizer(new StringReader(text));
      VectorNd tmp = new VectorNd(myVectorSize);

      int idx = 0;
      try {
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            if (idx == tmp.size()) {
               tmp.adjustSize(5);
            }
            if (rtok.ttype == ReaderTokenizer.TT_NUMBER) {
               tmp.set(idx++, rtok.nval);
            } else {
               return illegalValue(
                  "Malformed number for element " + idx, errMsg);
            }
         }
      } catch (Exception e) {
         return illegalValue("Error parsing text", errMsg);
      }

      tmp.setSize(idx);
      return validValue(tmp, errMsg);
   }

   protected Object validateValue(Object value, StringHolder errMsg) {
      value = validateBasic(value, Vector.class, errMsg);
      if (value == Property.IllegalValue) {
         return value;
      }
      if (value instanceof Vector) {
         value = myRange.validate((Vector)value, myAutoClipP, errMsg);
         return value == Range.IllegalValue ? Property.IllegalValue : value;
      }
      else {
         return validValue(value, errMsg);
      }
   }
   
   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         if (value instanceof Vector) { // make a defensive copy of the
                                          // supplied vector
            Vector vecValue = (Vector)value;
            if (myResultHolder != null) { // use the result holder as the value
                                          // itself. Reason
               // is that the result holder can be guaranteed to
               // have the Vector sub-type, since it was supplied
               // by the user.
               myResultHolder.set(vecValue);
               value = myResultHolder;
            }
            else { // make a defensive copy of the supplied vector
               value = new VectorNd (vecValue);
            }
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

}
