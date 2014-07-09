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
import maspack.matrix.Vector3d;
import maspack.properties.Property;

import java.io.IOException;
import java.io.StringReader;

/**
 * This is a field that can contain either one number representing a uniform
 * scaling, or three numbers representing scaling along the x, y, and z axes.
 * The associated value is either a Double or a Vector3d.
 */
public class ScaleField extends LabeledTextField {
   private static final long serialVersionUID = 1L;

   Object myValue = Property.VoidValue;
   private String validFmtChars = "eEfgaA";

   /**
    * Creates a default ScaleField with an empty label.
    */
   public ScaleField() {
      this ("");
   }

   /**
    * Creates a new ScaleField with specified label text.
    * 
    * @param labelText
    * text for the control label
    */
   public ScaleField (String labelText) {
      super (labelText, 5 * 4);
      initialize();
      setValue (new Double (1));
   }

   /**
    * Creates a new ScaleField with specified label text and initial uniform
    * scale value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial uniform scale value
    */
   public ScaleField (String labelText, double initialValue) {
      super (labelText, 5 * 4);
      initialize();
      setValue (initialValue);
   }

   /**
    * Creates a new ScaleField with specified label text and initial
    * 3-dimensional scaling.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * 3d scaling vector
    */
   public ScaleField (String labelText, Vector3d initialValue) {
      super (labelText, 5 * 4);
      initialize();
      setValue (initialValue);
   }

   private void initialize() {
      setFormat ("%.5g");
      myTextField.setHorizontalAlignment (JTextField.RIGHT);
   }

   public Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      if (isBlank (text)) {
         return setVoidIfPossible (errMsg);
      }
      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (text));
      double[] tmp = new double[3];

      try {
         if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
            return illegalValue ("Missing or malformed number", errMsg);
         }
         tmp[0] = rtok.nval;
         if (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            for (int i = 1; i < 3; i++) {
               if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
                  return illegalValue (
                     "Missing or malformed number for scale element " + i,
                     errMsg);
               }
               tmp[i] = rtok.nval;
            }
            if (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
               return illegalValue (
                  "Extra characters after third scale element", errMsg);
            }
            return validValue (new Vector3d (tmp[0], tmp[1], tmp[2]), errMsg);
         }
         else {
            return validValue (new Double (tmp[0]), errMsg);
         }
      }
      catch (IOException e) {
         return illegalValue ("Improperly formed scale value(s)", errMsg);
      }
   }

   protected String valueToText (Object value) {
      if (value instanceof Vector3d) {
         Vector3d vec = (Vector3d)value;
         return (
            myFmt.format(vec.x) + ' ' +
            myFmt.format(vec.y) + ' ' +
            myFmt.format (vec.z));
      }
      else if (value instanceof Double) {
         return (myFmt.format (((Double)value).doubleValue()));
      }
      else {
         return "";
      }
   }

  protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         if (value instanceof Number) {
            value = new Double (((Number)value).doubleValue());
         }
         else if (value instanceof Vector3d) {
            value = new Vector3d ((Vector3d)value);
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   private static Class[] myTypes =
      new Class[] { Vector3d.class, Number.class };

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, myTypes, errMsg);
   }

   protected Object getInternalValue() {
      // should we return a copy?
      return myValue;
   }
}
