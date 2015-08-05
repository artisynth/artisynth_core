/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.io.StringReader;

import javax.swing.JFrame;
import javax.swing.JTextField;

import maspack.matrix.AxisAngle;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.util.BooleanHolder;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.StringHolder;

public class AxisAngleField extends LabeledTextField {
   private static final long serialVersionUID = 1L;

   Object myValue = Property.VoidValue;

   private NumberFormat myAxisFmt = new NumberFormat ("%.5g");
   private String validFmtChars = "eEfgaA";

   /**
    * Creates a default AxisAngleField with an empty label.
    */
   public AxisAngleField() {
      this ("");
   }

   /**
    * Creates a new AxisAngleField with specified label text.
    * 
    * @param labelText
    * text for the control label
    */
   public AxisAngleField (String labelText) {
      super (labelText, 5 * 4);
      initialize();
      setValue (new AxisAngle());
   }

   /**
    * Creates a new AxisAngleField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the AxisAngle
    */
   public AxisAngleField (String labelText, AxisAngle initialValue) {
      super (labelText, 5 * 4);
      initialize();
      setValue (initialValue);
   }

   private void initialize() {
      setFormat ("%.5g");
      myTextField.setHorizontalAlignment (JTextField.RIGHT);
   }

   public AxisAngle getAxisAngleValue() {
      if (myValue instanceof AxisAngle) {
         return new AxisAngle ((AxisAngle)myValue);
      }
      else {
         return null;
      }
   }

   public Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      ReaderTokenizer rtok = new ReaderTokenizer (new StringReader (text));
      VectorNd tmp = new VectorNd (4);

      try {
         for (int i = 0; i < 4; i++) {
            if (rtok.nextToken() != ReaderTokenizer.TT_NUMBER) {
               return illegalValue ("Missing or malformed number for element "
               + i, errMsg);
            }
            tmp.set (i, rtok.nval);
         }
         if (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            // check if specified degrees or radians
            if (rtok.tokenIsWord()) {
               if (rtok.sval.toLowerCase().equals("r") ||
                  rtok.sval.toLowerCase().equals("rad") ||
                  rtok.sval.toLowerCase().equals("radians")) {
                  // radians
                  tmp.set(3, Math.toDegrees(tmp.get(3)));
               } else if  (rtok.sval.toLowerCase().equals("d") ||
                  rtok.sval.toLowerCase().equals("deg") ||
                  rtok.sval.toLowerCase().equals("degrees")) {
                  // already in degrees
               } else {
                  return illegalValue ("Only angle unit specifier allowed after last entry", errMsg);
               }
            } else {
               return illegalValue ("Extra characters after last element", errMsg);
            }
         }
         return validValue (new AxisAngle (
                               tmp.get(0), tmp.get(1), tmp.get(2),
                               Math.toRadians (tmp.get (3))), errMsg);
      }
      catch (Exception e) {
         return illegalValue ("Improperly formed 4-tuple", errMsg);
      }
   }

   protected String valueToText (Object value) {
      if (value instanceof AxisAngle) {
         AxisAngle axisAng = (AxisAngle)value;
         return (myAxisFmt.format (axisAng.axis.x) + ' '
                 + myAxisFmt.format (axisAng.axis.y) + ' '
                 + myAxisFmt.format (axisAng.axis.z) + ' '
                 + myFmt.format (Math.toDegrees (axisAng.angle)));
      }
      else {
         return "";
      }
   }

   protected boolean updateInternalValue (Object value) {
      if (!valuesEqual (value, myValue)) {
         if (value instanceof AxisAngle) { // make defensive copy
            value = new AxisAngle ((AxisAngle)value);
         }
         myValue = value;
         return true;
      }
      else {
         return false;
      }
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      return validateBasic (value, AxisAngle.class, errMsg);
   }

   protected Object getInternalValue() {
      // should we return a copy?
      return myValue;
   }

   public static void main (String[] args) {
      JFrame frame = new JFrame ("AxisAngleField Test");
      LabeledComponentPanel panel = new LabeledComponentPanel();
      AxisAngleField widget = new AxisAngleField ();

      frame.getContentPane().add (panel);

      RotationMatrix3d R = new RotationMatrix3d (1, 0, 0, Math.toRadians(45));
      widget.setValue (new AxisAngle(R));

      panel.addWidget ("axisAng", widget);

      // panel.addWidget (widget);

      frame.pack();
      frame.setVisible (true);
   }

}
