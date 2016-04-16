/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.geometry.Rectangle2d;
import maspack.matrix.VectorBase;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.util.BooleanHolder;
import maspack.util.StringHolder;

public class RectangleField extends VectorField {
   private static final long serialVersionUID = -250964885050886911L;

   private static VectorNd rectangleToVector (Rectangle2d rect) {
      VectorNd vec = new VectorNd (4);
      vec.set (0, rect.x);
      vec.set (1, rect.y);
      vec.set (2, rect.width);
      vec.set (3, rect.height);
      return vec;
   }

   private static Rectangle2d vectorToRectangle (VectorBase v) {
      return new Rectangle2d (
         v.get (0), v.get (1), v.get (2), v.get (3));
   }
   
   /**
    * Creates a default RectangleField with an empty label.
    */
   public RectangleField() {
      this ("");
   }

   public RectangleField (String labelText) {
      super (labelText, 4);
   }

   public RectangleField (String labelText,
   Rectangle2d initialValue) {
      this (labelText, initialValue, "%.6g");
   }

   public RectangleField (String labelText,
      Rectangle2d initialValue, String fmtStr) {
      super (labelText, rectangleToVector (initialValue), fmtStr);
   }

   public Rectangle2d getMatrixValue() {
      if (myValue instanceof VectorNd) {
         return (Rectangle2d)getInternalValue();
      }
      else {
         return null;
      }
   }

   public Object getInternalValue() {
      // Translate myValue into a Rectangle if it
      // is not void-valued or null
      if (myValue instanceof VectorNd) {
         return vectorToRectangle ((VectorNd)myValue);
      }
      else {
         return myValue;
      }
   }

   protected boolean updateInternalValue (Object value) {
      if (value instanceof Rectangle2d) {
         value = rectangleToVector ((Rectangle2d)value);
      }
      return super.updateInternalValue (value);
   }

   protected String valueToText (Object value) {
      if (value instanceof Rectangle2d) {
         value = rectangleToVector ((Rectangle2d)value);
      }
      return super.valueToText (value);
   }

   public Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      corrected.value = false;
      Object value = super.textToValue (text, corrected, errMsg);
      if (value == Property.IllegalValue) {
         return Property.IllegalValue;
      }
      if (value instanceof VectorNd) {
         VectorNd tmp = (VectorNd)value;
         value =
            new Rectangle2d (tmp.get(0), tmp.get(1), tmp.get(2), tmp.get(3));
      }
      return validValue (value, errMsg);
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      if (value != null && value != Property.VoidValue) {
         if (!(value instanceof Rectangle2d)) {
            return illegalValue ("value must be a Rectangle", errMsg);
         }
         value = rectangleToVector ((Rectangle2d)value);
      }
      Object checkedValue = super.validateValue (value, errMsg);
      if (checkedValue != Property.IllegalValue && checkedValue != value) { 
         // value was modfied; translate back
         checkedValue = vectorToRectangle ((VectorNd)value);
      }
      return checkedValue;
   }
}
