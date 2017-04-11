/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.awt.Color;

import maspack.matrix.AxisAngle;
import maspack.matrix.DenseMatrix;
import maspack.matrix.Vector;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.matrix.Vectori;
import maspack.matrix.Vector2i;
import maspack.matrix.Vector3i;
import maspack.util.InternalErrorException;

public class NumericConverter {
   protected Object myObj;
   protected double[] myArray;
   protected PropertyDesc.TypeCode myType;
   protected int myDimension;

   public NumericConverter (Object valueObject) {
      myType = PropertyDesc.getTypeCode (valueObject.getClass());
      if (!typeIsNumeric (myType)) {
         throw new IllegalArgumentException ("object of type "
         + valueObject.getClass() + " is not numeric");
      }
      try {
         myDimension = allocateCacheObjects (valueObject, myType);
      }
      catch (Exception e) {
         e.printStackTrace();
         throw new InternalErrorException ("Unable to create cache object for "
         + valueObject.getClass());
      }
   }

   public NumericConverter (NumericConverter conv) {
      myType = conv.myType;
      try {
         myDimension = allocateCacheObjects (conv.myObj, myType);
      }
      catch (Exception e) {
         e.printStackTrace();
         throw new InternalErrorException ("Unable to create cache object for "
         + conv.myObj.getClass());
      }
   }

   /**
    * Returns true if a specified object or class is compatible with a numeric
    * property.
    * 
    * @param objOrClass
    * object or class to test
    */
   public static boolean isNumeric (Object objOrClass) {
      Class<?> cls;
      if (objOrClass instanceof Class) {
         cls = (Class<?>)objOrClass;
      }
      else {
         cls = objOrClass.getClass();
      }
      return typeIsNumeric (PropertyDesc.getTypeCode (cls));
   }

   public static int getDimension (Object value) {
      
      switch (PropertyDesc.getTypeCode(value.getClass())) {
         case SHORT_ARRAY: {
            return ((short[])value).length;
         }
         case INT_ARRAY: {
            return ((int[])value).length;
         }
         case LONG_ARRAY: {
            return ((long[])value).length;
         }
         case FLOAT_ARRAY: {
            return ((float[])value).length;
         }
         case DOUBLE_ARRAY: {
            return ((double[])value).length;
         }
         case VECTOR: {
            return ((Vector)value).size();
         }
         case VECTORI: {
            return ((Vectori)value).size();
         }
         case MATRIX: {
            return ((DenseMatrix)value).rowSize()*((DenseMatrix)value).colSize();
         }
         case COLOR: {
            return 3;
         }
         case AXIS_ANGLE: {
            return 4;
         }
         case BYTE:
         case SHORT:
         case INT:
         case LONG:
         case FLOAT:
         case DOUBLE:
         case BOOLEAN: {
            return 1;
         }
         default: {
            return 0;
         }
      }
   }

   private static boolean typeIsNumeric (PropertyDesc.TypeCode type) {
      switch (type) {
         case SHORT_ARRAY:
         case INT_ARRAY:
         case LONG_ARRAY:
         case FLOAT_ARRAY:
         case DOUBLE_ARRAY:
         case VECTOR:
         case VECTORI:
         case MATRIX:
         case COLOR:
         case AXIS_ANGLE:
         case BYTE:
         case SHORT:
         case INT:
         case LONG:
         case FLOAT:
         case DOUBLE:
         case BOOLEAN: {
            return true;
         }
         default: {
            return false;
         }
      }
   }

   /**
    * Returns labels for the individual fields of a numeric type,
    * if any. If no field names are known, then null is returned.
    * 
    * @param objOrClass
    * object or class to test
    */
   public static String[] getFieldNames (Object objOrClass) {
      Class<?> cls;
      if (objOrClass instanceof Class) {
         cls = (Class<?>)objOrClass;
      }
      else {
         cls = objOrClass.getClass();
      }
      if (!isNumeric(cls)) {
         return null;
      }
      else if (Vector2d.class.isAssignableFrom (cls)) {
         return new String [] { "x", "y" };
      }
      else if (Vector3d.class.isAssignableFrom (cls)) {
         return new String [] { "x", "y", "z" };
      }
      else if (Vector4d.class.isAssignableFrom (cls)) {
         return new String [] { "x", "y", "z", "w" };
      }
      else if (Color.class.isAssignableFrom (cls)) {
         return new String [] { "r", "g", "b", "a" };
      }
      else if (AxisAngle.class.isAssignableFrom (cls)) {
         return new String [] { "ux", "uy", "uz", "ang" };
      }
      else {
         return null;
      }
   }

   public int getDimension() {
      return myDimension;
   }

   private int allocateCacheObjects (Object propObj, PropertyDesc.TypeCode type)
      throws InstantiationException, IllegalAccessException {
      switch (type) {
         case SHORT_ARRAY: {
            short[] ar = (short[])propObj;
            myDimension = ar.length;
            myObj = new short[myDimension];
            break;
         }
         case INT_ARRAY: {
            int[] ar = (int[])propObj;
            myDimension = ar.length;
            myObj = new int[myDimension];
            break;
         }
         case LONG_ARRAY: {
            long[] ar = (long[])propObj;
            myDimension = ar.length;
            myObj = new long[myDimension];
            break;
         }
         case FLOAT_ARRAY: {
            float[] ar = (float[])propObj;
            myDimension = ar.length;
            myObj = new float[myDimension];
            break;
         }
         case DOUBLE_ARRAY: {
            double[] ar = (double[])propObj;
            myDimension = ar.length;
            myObj = new double[myDimension];
            break;
         }
         case VECTOR: {
            Vector v = (Vector)propObj;
            myDimension = v.size();
            v = (Vector)propObj.getClass().newInstance();
            if (v.isFixedSize() == false) {
               v.setSize (myDimension);
            }
            myObj = (Object)v;
            break;
         }
         case VECTORI: {
            Vectori v = (Vectori)propObj;
            myDimension = v.size();
            v = (Vectori)propObj.getClass().newInstance();
            if (v.isFixedSize() == false) {
               v.setSize (myDimension);
            }
            myObj = (Object)v;
            break;
         }
         case MATRIX: {
            DenseMatrix m = (DenseMatrix)propObj;
            int numRows = m.rowSize();
            int numCols = m.colSize();
            myDimension = numRows * numCols;
            m = (DenseMatrix)propObj.getClass().newInstance();
            if (m.isFixedSize() == false)
               m.setSize (numRows, numCols);
            myObj = (Object)m;
            break;
         }
         case COLOR: {
            myDimension = 4;
            break;
         }
         case AXIS_ANGLE: {
            myDimension = 4;
            break;
         }
         case BYTE:
         case SHORT:
         case INT:
         case LONG:
         case FLOAT:
         case DOUBLE:
         case BOOLEAN: {
            myDimension = 1;
            break;
         }
         default: { // prop object has non-numeric type
            myDimension = 0;
            throw new IllegalArgumentException ("valueOject typecode ("
            + type.name() + ") is a non-numeric type");
         }
      }
      myArray = new double[myDimension];
      return myDimension;
   }

   public Object arrayToObject (double[] vals) {
      int i;
      if (vals.length < myDimension) {
         throw new IllegalArgumentException (
            "array not large enough for object");
      }
      switch (myType) {
         case SHORT_ARRAY: {
            short[] shortAr = (short[])myObj;
            for (i = 0; i < myDimension; i++)
               shortAr[i] = (short)vals[i];
            break;
         }
         case INT_ARRAY: {
            int[] intAr = (int[])myObj;
            for (i = 0; i < myDimension; i++)
               intAr[i] = (int)vals[i];
            break;
         }
         case LONG_ARRAY: {
            long[] longAr = (long[])myObj;
            for (i = 0; i < myDimension; i++)
               longAr[i] = (long)vals[i];
            break;
         }
         case FLOAT_ARRAY: {
            float[] floatAr = (float[])myObj;
            for (i = 0; i < myDimension; i++)
               floatAr[i] = (float)vals[i];
            break;
         }
         case DOUBLE_ARRAY: {
            double[] doubleAr = (double[])myObj;
            for (i = 0; i < myDimension; i++)
               doubleAr[i] = (double)vals[i];
            break;
         }
         case VECTOR: {
            Vector vec = (Vector)myObj;
            for (i = 0; i < myDimension; i++)
               vec.set (i, vals[i]);
            break;
         }
         case VECTORI: {
            Vectori vec = (Vectori)myObj;
            for (i = 0; i < myDimension; i++)
               vec.set (i, (int)vals[i]);
            break;
         }
         case MATRIX: {
            DenseMatrix mat = (DenseMatrix)myObj;
            i = 0;
            for (int j = 0; j < mat.rowSize(); j++)
               for (int k = 0; k < mat.colSize(); k++) {
                  mat.set (j, k, vals[i++]);
               }
            break;
         }
         case COLOR: {
            myObj =
               new Color (
                  (float)vals[0], (float)vals[1], (float)vals[2],
                  (float)vals[3]);
            break;
         }
         case AXIS_ANGLE: {
            myObj =
               new AxisAngle (vals[0], vals[1], vals[2],
                              Math.toRadians (vals[3]));
            break;
         }
         case BYTE: {
            return (byte)vals[0];
         }
         case SHORT: {
            return (short)vals[0];
         }
         case INT: {
            return (int)vals[0];
         }
         case LONG: {
            return (long)vals[0];
         }
         case FLOAT: {
            return (float)vals[0];
         }
         case DOUBLE: {
            return (double)vals[0];
         }
         case BOOLEAN: {
            if (vals[0] == 0.0)
               return false;
            else
               return true;
         }
         default: {
            myObj = null;
            throw new IllegalArgumentException ("valueOject typecode ("
            + myType.name() + ")" + " is a non-numeric type");
         }
      }
      return myObj;
   }

   public double[] objectToArray (Object obj) {
      objectToArray (myArray, obj);
      return myArray;
   }

   public void objectToArray (double[] array, Object obj) {
      int i;
      if (array.length < myDimension) {
         throw new IllegalArgumentException (
            "array not large enough for object");
      }
      switch (myType) {
         case SHORT_ARRAY: {
            short[] shortAr = (short[])obj;
            for (i = 0; i < myDimension; i++)
               array[i] = (double)shortAr[i];
            break;
         }
         case INT_ARRAY: {
            int[] intAr = (int[])obj;
            for (i = 0; i < myDimension; i++)
               array[i] = (double)intAr[i];
            break;
         }
         case LONG_ARRAY: {
            long[] longAr = (long[])obj;
            for (i = 0; i < myDimension; i++)
               array[i] = (double)longAr[i];
            break;
         }
         case FLOAT_ARRAY: {
            float[] floatAr = (float[])obj;
            for (i = 0; i < myDimension; i++)
               array[i] = (double)floatAr[i];
            break;
         }
         case DOUBLE_ARRAY: {
            double[] doubleAr = (double[])obj;
            for (i = 0; i < myDimension; i++)
               array[i] = doubleAr[i];
            break;
         }
         case VECTOR: {
            Vector vec = (Vector)obj;
            for (i = 0; i < myDimension; i++)
               array[i] = vec.get(i);
            break;
         }
         case VECTORI: {
            Vectori vec = (Vectori)obj;
            for (i = 0; i < myDimension; i++)
               array[i] = vec.get(i);
            break;
         }
         case MATRIX: {
            DenseMatrix mat = (DenseMatrix)obj;
            i = 0;
            for (int j = 0; j < mat.rowSize(); j++) {
               for (int k = 0; k < mat.colSize(); k++) {
                  array[i++] = mat.get (j, k);
               }
            }
            break;
         }
         case COLOR: {
            Color inColor = (Color)obj;
            array[0] = inColor.getRed() / 255.0;
            array[1] = inColor.getGreen() / 255.0;
            array[2] = inColor.getBlue() / 255.0;
            array[3] = inColor.getAlpha() / 255.0;
            break;
         }
         case AXIS_ANGLE: {
            AxisAngle axisAng = (AxisAngle)obj;
            array[0] = axisAng.axis.x;
            array[1] = axisAng.axis.y;
            array[2] = axisAng.axis.z;
            array[3] = Math.toDegrees (axisAng.angle);
            break;
         }
         case BYTE: {
            array[0] = (Byte)obj;
            break;
         }
         case SHORT: {
            array[0] = (Short)obj;
            break;
         }
         case INT: {
            array[0] = (Integer)obj;
            break;
         }
         case LONG: {
            array[0] = (Long)obj;
            break;
         }
         case FLOAT: {
            array[0] = (Float)obj;
            break;
         }
         case DOUBLE: {
            array[0] = (Double)obj;
            break;
         }
         case BOOLEAN: {
            if ((Boolean)obj)
               array[0] = 1.0;
            else
               array[0] = 0.0;
            break;
         }
         default: {
            throw new IllegalArgumentException ("valueObject typecode ("
            + myType.name() + ")" + " is a non-numeric type");
         }
      }
   }

}
