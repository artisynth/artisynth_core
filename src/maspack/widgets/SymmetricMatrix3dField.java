/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.VectorBase;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.util.BooleanHolder;
import maspack.util.StringHolder;

public class SymmetricMatrix3dField extends VectorField {
   private static final long serialVersionUID = 3600583724619478752L;

   private static VectorNd matrixToVector (SymmetricMatrix3d M) {
      VectorNd vec = new VectorNd (6);
      vec.set (0, M.m00);
      vec.set (1, M.m11);
      vec.set (2, M.m22);
      vec.set (3, M.m01);
      vec.set (4, M.m02);
      vec.set (5, M.m12);
      return vec;
   }

   private static SymmetricMatrix3d vectorToMatrix (VectorBase v) {
      return new SymmetricMatrix3d (
         v.get (0), v.get (1), v.get (2), v.get (3), v.get (4), v.get (5));
   }

   /**
    * Creates a default SymmetricMatrix3dField with an empty label.
    */
   public SymmetricMatrix3dField() {
      this ("");
   }

   public SymmetricMatrix3dField (String labelText) {
      super (labelText, 6);
   }

   public SymmetricMatrix3dField (String labelText,
   SymmetricMatrix3d initialValue) {
      this (labelText, initialValue, "%.6g");
   }

   public SymmetricMatrix3dField (String labelText,
   SymmetricMatrix3d initialValue, String fmtStr) {
      super (labelText, matrixToVector (initialValue), fmtStr);
   }

   public SymmetricMatrix3d getMatrixValue() {
      if (myValue instanceof VectorNd) {
         return (SymmetricMatrix3d)getInternalValue();
      }
      else {
         return null;
      }
   }

   public Object getInternalValue() {
      // Translate myValue into a SymmetricMatrix3d if it
      // is not void-valued or null
      if (myValue instanceof VectorNd) {
         return vectorToMatrix ((VectorNd)myValue);
      }
      else {
         return myValue;
      }
   }

   protected boolean updateInternalValue (Object value) {
      if (value instanceof SymmetricMatrix3d) {
         value = matrixToVector ((SymmetricMatrix3d)value);
      }
      return super.updateInternalValue (value);
   }

   protected String valueToText (Object value) {
      if (value instanceof SymmetricMatrix3d) {
         value = matrixToVector ((SymmetricMatrix3d)value);
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
            new SymmetricMatrix3d (tmp.get(0), tmp.get(1), tmp.get(2),
                                   tmp.get(3), tmp.get(4), tmp.get(5));
      }
      return validValue (value, errMsg);
   }

   protected Object validateValue (Object value, StringHolder errMsg) {
      if (value != null && value != Property.VoidValue) {
         if (!(value instanceof SymmetricMatrix3d)) {
            return illegalValue ("value must be a SymmetricMatrix3d", errMsg);
         }
         value = matrixToVector ((SymmetricMatrix3d)value);
      }
      Object checkedValue = super.validateValue (value, errMsg);
      if (checkedValue != Property.IllegalValue && checkedValue != value) { 
         // value was modfied; translate back
         checkedValue = vectorToMatrix ((VectorNd)value);
      }
      return checkedValue;
   }

}
