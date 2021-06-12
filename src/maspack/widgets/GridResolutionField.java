/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import javax.swing.*;

import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.StringHolder;
import maspack.util.BooleanHolder;
import maspack.matrix.AxisAngle;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.render.GridResolution;

import java.io.IOException;
import java.io.StringReader;

public class GridResolutionField extends LabeledTextField {
   private static final long serialVersionUID = 1L;

   Object myValue = Property.VoidValue;

   /**
    * Creates a default GridResolutionField with an empty label.
    */
   public GridResolutionField() {
      this ("");
   }

   /**
    * Creates a new GridResolutionField with specified label text.
    * 
    * @param labelText
    * text for the control label
    */
   public GridResolutionField (String labelText) {
      super (labelText, 6);
      initialize();
      setValue (new GridResolution (1, 10));
   }

   /**
    * Creates a new GridResolutionField with specified label text and initial
    * value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the GridRes
    */
   public GridResolutionField (String labelText, GridResolution initialValue) {
      super (labelText, 6);
      initialize();
      setValue (initialValue);
   }

   private void initialize() {
      setFormat ("%.4g");
      myTextField.setHorizontalAlignment (JTextField.RIGHT);
   }

   public GridResolution getGridResolutionValue() {
      if (myValue instanceof GridResolution) {
         return new GridResolution ((GridResolution)myValue);
      }
      else {
         return null;
      }
   }

   public Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (text));
      double cellSize = 1;
      int numDivisions = 1;
      
      text = text.trim();
      if (text.equals ("*")) {
         // return a resolution with 0 cellSize, indicating auto-sizing.
         return validValue (new GridResolution(0, 1), errMsg);
      }

      if (isBlank (text)) {
         return illegalValue ("Void value not permited", errMsg);
      }

      try {
         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
            return illegalValue ("Improperly formed cell size", errMsg);
         }
         cellSize = rtok.nval;
         if (rtok.nextToken() == ReaderTokenizer.TT_EOF) {
            return validValue (new GridResolution (cellSize, 1), errMsg);
         }
         else if (rtok.ttype != '/') {
            return illegalValue ("Expecting '/' after cell size", errMsg);
         }
         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
            return illegalValue (
               "Expecting number of cell divisions after '/'", errMsg);
         }
         if (rtok.nval < 1) {
            return illegalValue (
               "number of cell divisions must be positive integer", errMsg);
         }
         numDivisions = (int)rtok.nval;
         if (numDivisions != rtok.nval) {
            corrected.value = true;
         }
         return validValue (
            new GridResolution (cellSize, numDivisions), errMsg);
      }
      catch (Exception e) {
         return illegalValue ("Improperly formed resolution", errMsg);
      }
   }

   protected String valueToText (Object value) {
      if (value instanceof GridResolution) {
         return ((GridResolution)value).toString (myFmt);
      }
      else {
         return "";
      }
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         if (value instanceof GridResolution) { // make defensive copy
            value = new GridResolution ((GridResolution)value);
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, GridResolution.class, errMsg);
   }

   protected Object getInternalValue() {
      return myValue;
   }
}
