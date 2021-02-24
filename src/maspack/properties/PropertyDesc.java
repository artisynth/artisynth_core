/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import java.util.ArrayList;

import maspack.matrix.AxisAngle;
import maspack.matrix.DenseMatrix;
import maspack.matrix.Matrix;
import maspack.matrix.Vector;
import maspack.matrix.Vectori;
import maspack.util.ClassAliases;
import maspack.util.DoubleInterval;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.NumericInterval;
import maspack.util.Range;
import maspack.util.ReaderTokenizer;
import maspack.util.Scan;
import maspack.util.Scannable;
import maspack.util.Write;

/**
 * Provides information and implementation code for specific properties exported
 * by a class.
 */
public class PropertyDesc implements PropertyInfo {
   protected String myName;
   protected String myDescription;
   protected Class<?> myHostClass;
   protected Class<?> myValueClass;
   protected TypeCode myValueType = TypeCode.OTHER;
   protected NumberFormat myFmt = null;
   protected boolean myInheritableP = false;
   protected boolean myNullValueOK = false;

   public static boolean debug = false;
   protected Method myGetMethod;
   protected Method mySetMethod;
   protected Method myGetRangeMethod;
   protected Method myGetDefaultMethod; // not current used
   protected Method myGetModeMethod;
   protected Method mySetModeMethod;
   protected Method myCreateMethod;

   boolean myDefaultIsAuto = false;
   Object myDefaultValue;
   PropertyMode myDefaultMode = PropertyMode.Explicit;
   boolean myAutoWriteP = true;
   protected Edit myEdit = Edit.Always;
   protected ExpandState myWidgetExpandState = ExpandState.Unexpandable;
   protected boolean mySliderAllowed = true;
   boolean myReadOnlyP = false;
   NumericInterval myNumericRange = null;
   protected int myDimension;
   protected boolean mySharableP = false;

   protected ArrayList<Class<?>> myAllowedTypes = null;

   public static final int REGULAR = 0;
   public static final int READ_ONLY = 1;
   public static final int INHERITABLE = 2;

   public enum TypeCode {
      BYTE,
      CHAR,
      SHORT,
      INT,
      LONG,
      FLOAT,
      DOUBLE,
      BOOLEAN,
      SHORT_ARRAY,
      INT_ARRAY,
      LONG_ARRAY,
      FLOAT_ARRAY,
      DOUBLE_ARRAY,
      COLOR,
      AXIS_ANGLE,
      ENUM,
      VECTOR,
      VECTORI,
      MATRIX,
      STRING,
      SCANABLE,
      DIMENSION,
      POINT,
      FONT,
      OTHER
   };

   /**
    * Creates an empty PropertyDesc.
    */
   protected PropertyDesc() {
      super();
   }

   /**
    * Creates a new PropertyDesc object with a specific property name and host
    * class.
    * 
    * @param name
    * name of the property.
    * @param hostClass
    * host class for the property.
    */
   public PropertyDesc (String name, Class<?> hostClass) {
      setName (name);
      setHostClass (hostClass);
   }

   /**
    * Creates a new PropertyDesc for a property with a specified name, host
    * class, and value class.
    * 
    * @param name
    * property name
    * @param hostClass
    * class object for the host class.
    * @param valueClass
    * class object for the value.
    */
   public PropertyDesc (String name, Class<?> hostClass, Class<?> valueClass) {
      this (name, hostClass);
      setPropertyType (valueClass);
   }

   /**
    * Sets this PropertyDesc from an existing one and a 
    * a specified host class.
    */
   public void set (PropertyDesc desc, Class<?> hostClass) {
      setName (desc.myName);
      setHostClass (hostClass);
      myDescription = desc.myDescription;
      myValueClass = desc.myValueClass;
      myValueType = desc.myValueType;
      myFmt = desc.myFmt;
      if (desc.myGetMethod != null) {
         initGetMethod (desc.myGetMethod.getName());
      }
      if (desc.mySetMethod != null) {
         initSetMethod (desc.mySetMethod.getName());
      }
      if (desc.myGetRangeMethod != null) {
         initGetRangeMethod (desc.myGetRangeMethod.getName());
      }
      if (desc.myGetDefaultMethod != null) {
         initGetDefaultMethod (desc.myGetDefaultMethod.getName());
      }
      if (desc.myGetModeMethod != null) {
         initGetModeMethod (desc.myGetModeMethod.getName());
      }
      if (desc.mySetModeMethod != null) {
         initSetModeMethod (desc.mySetModeMethod.getName());
      }
      if (desc.myCreateMethod != null) {
         tryToInitCreateMethod (desc.myCreateMethod.getName());
      }
      if (myDefaultIsAuto) {
         myDefaultValue = createDefaultValue();
      }
      else {
         myDefaultValue = desc.myDefaultValue;
      }
      myAutoWriteP = desc.myAutoWriteP;
      myEdit = desc.myEdit;
      myWidgetExpandState = desc.myWidgetExpandState;
      mySliderAllowed = desc.mySliderAllowed;
      myDimension = desc.myDimension;
      myReadOnlyP = desc.myReadOnlyP;
      myInheritableP = desc.myInheritableP;
      myDefaultMode = desc.myDefaultMode;
      mySharableP = desc.mySharableP;
      myNullValueOK = desc.myNullValueOK;
      if (desc.myNumericRange != null) {
         myNumericRange = desc.myNumericRange.clone();
      }
      else {
         myNumericRange = null;
      }
   }

   private void setFormatIfNecessary (String fmtStr) {
      if (myFmt == null) {
         setFormat (new NumberFormat (fmtStr));
      }
   }

   private int getDimensionFromType() {
      switch (myValueType) {
         case COLOR: {
            return 3;
         }
         case AXIS_ANGLE: {
            return 4;
         }
         case VECTOR: {
            try {
               Vector v = (Vector)myValueClass.newInstance();
               if (v.isFixedSize()) {
                  return v.size();
               }
            }
            catch (Exception e) {
               System.out.println ("Warning: cannot create instance of "
               + myValueClass);
               return -1;
            }
            break;
         }
         case VECTORI: {
            try {
               Vectori v = (Vectori)myValueClass.newInstance();
               if (v.isFixedSize()) {
                  return v.size();
               }
            }
            catch (Exception e) {
               System.out.println ("Warning: cannot create instance of "
               + myValueClass);
               return -1;
            }
            break;
         }
         case MATRIX: {
            try {
               DenseMatrix M = (DenseMatrix)myValueClass.newInstance();
               if (M.isFixedSize()) {
                  return M.rowSize() * M.colSize();
               }
            }
            catch (Exception e) {
               System.out.println ("Warning: cannot create instance of "
               + myValueClass);
               return -1;
            }
            break;
         }
         case DOUBLE_ARRAY: {
            
         }
         default: {
            if (typeIsNumeric()) {
               return 1;
            }
         }
      }
      return -1;
   }

   protected int getDimensionFromDefaultValue() {
      if (myDefaultValue instanceof Vector) {
         return ((Vector)myDefaultValue).size();
      }
      else if (myDefaultValue instanceof Vectori) {
         return ((Vectori)myDefaultValue).size();
      }
      else if (myDefaultValue instanceof Matrix) {
         Matrix M = (Matrix)myDefaultValue;
         return M.rowSize() * M.colSize();
      }
      else if (myDefaultValue instanceof double[]) {
         return ((double[])myDefaultValue).length;
      }
      else {
         return -1;
      }
   }

   boolean typeIsNumeric() {
      switch (myValueType) {
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

   protected void setPropertyType (Class<?> cls) {
      myValueClass = cls;
      myValueType = getTypeCode (cls);
      if (myValueType == TypeCode.MATRIX || myValueType == TypeCode.VECTOR) {
         setFormatIfNecessary ("%.6g");
      }
      else if (myValueType == TypeCode.VECTORI) {
         setFormatIfNecessary ("%d");
      }
      setDimension (getDimensionFromType());
   }

   public static TypeCode getTypeCode (Class<?> cls) {
      TypeCode code;

      if (cls.isArray()) {
         Class<?> compCls = cls.getComponentType();

         if (compCls == Short.TYPE) {
            code = TypeCode.SHORT_ARRAY;
         }
         else if (compCls == Integer.TYPE) {
            code = TypeCode.INT_ARRAY;
         }
         else if (compCls == Long.TYPE) {
            code = TypeCode.LONG_ARRAY;
         }
         else if (compCls == Float.TYPE) {
            code = TypeCode.FLOAT_ARRAY;
         }
         else if (compCls == Double.TYPE) {
            code = TypeCode.DOUBLE_ARRAY;
         }
         else {
            code = TypeCode.OTHER;
         }
      }
      else if (cls == Byte.TYPE || Byte.class.isAssignableFrom (cls)) {
         code = TypeCode.BYTE;
      }
      else if (cls == Character.TYPE || Character.class.isAssignableFrom (cls)) {
         code = TypeCode.CHAR;
      }
      else if (cls == Short.TYPE || Short.class.isAssignableFrom (cls)) {
         code = TypeCode.SHORT;
      }
      else if (cls == Integer.TYPE || Integer.class.isAssignableFrom (cls)) {
         code = TypeCode.INT;
      }
      else if (cls == Long.TYPE || Long.class.isAssignableFrom (cls)) {
         code = TypeCode.LONG;
      }
      else if (cls == Float.TYPE || Float.class.isAssignableFrom (cls)) {
         code = TypeCode.FLOAT;
      }
      else if (cls == Double.TYPE || Double.class.isAssignableFrom (cls)) {
         code = TypeCode.DOUBLE;
      }
      else if (cls == Boolean.TYPE || Boolean.class.isAssignableFrom (cls)) {
         code = TypeCode.BOOLEAN;
      }
      else if (Color.class.isAssignableFrom (cls)) {
         code = TypeCode.COLOR;
      }
      else if (AxisAngle.class.isAssignableFrom (cls)) {
         code = TypeCode.AXIS_ANGLE;
      }
      else if (cls.isEnum()) {
         code = TypeCode.ENUM;
      }
      else if (Vector.class.isAssignableFrom (cls)) {
         code = TypeCode.VECTOR;
      }
      else if (Vectori.class.isAssignableFrom (cls)) {
         code = TypeCode.VECTORI;
      }
      else if (DenseMatrix.class.isAssignableFrom (cls)) {
         code = TypeCode.MATRIX;
      }
      else if (String.class.isAssignableFrom (cls)) {
         code = TypeCode.STRING;
      }
      else if (Scannable.class.isAssignableFrom (cls)) {
         code = TypeCode.SCANABLE;
      }
      else if (java.awt.Dimension.class.isAssignableFrom (cls)) {
         code = TypeCode.DIMENSION;
      }
      else if (java.awt.Point.class.isAssignableFrom (cls)) {
         code = TypeCode.POINT;
      }
      else if (Font.class.isAssignableFrom (cls)) {
         code = TypeCode.FONT;
      }
      else {
         code = TypeCode.OTHER;
      }
      return code;
   }

   /**
    * Returns the value code of the property associated with this descriptor.
    * 
    * @return property value code
    */
   protected TypeCode getTypeCode() {
      return myValueType;
   }

   /**
    * {@inheritDoc}
    */
   public Class<?> getValueClass() {
      return myValueClass;
   }

   static protected boolean isJavaIdentifier (String name) {
      int len = name.length();
      if (!Character.isJavaIdentifierStart (name.charAt (0))) {
         return false;
      }
      for (int i = 1; i < len; i++) {
         if (!Character.isJavaIdentifierPart (name.charAt(i))) {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns the name of the property associated with this descriptor.
    * 
    * @return name of the property
    */
   public String getName() {
      return myName;
   }

   /**
    * Sets the name of the property associated with this descriptor.
    * 
    * @param name
    * new name of the property
    */
   public void setName (String name) {
      myName = new String (name);
   }

   /**
    * {@inheritDoc}
    * 
    * @see #setDescription
    */
   public String getDescription() {
      return myDescription;
   }

   /**
    * Sets a text description for the property.
    * 
    * @param text
    * new text description
    * @see #getDescription
    */
   public void setDescription (String text) {
      myDescription = new String (text);
   }

   /**
    * {@inheritDoc}
    */
   public Class<?> getHostClass() {
      return myHostClass;
   }

   /**
    * Sets the host class of the property.
    * 
    * @param hostClass
    * host class of the property.
    */
   protected void setHostClass (Class<?> hostClass) {
      myHostClass = hostClass;
   }

   /**
    * Sets the formatter used to convert numeric components of the property's
    * value into text. The formatter is specified using a C <code>printf</code>-style
    * format string. For a description of the format string syntax, see
    * {@link maspack.util.NumberFormat NumberFormat}. The format should be
    * consistent with the numeric type (e.g., <code>%d</code> or
    * <code>%x</code> for integers, <code>%g</code>, <code>$f</code> or
    * <code>%e</code> for floats).
    * 
    * @param fmtStr
    * numeric format string for the property
    * @throws IllegalArgumentException
    * if the format string syntax is invalid
    * @see #getFormat
    */
   public void setPrintFormat (String fmtStr) {
      if (fmtStr == null) {
         myFmt = null;
      }
      else {
         myFmt = new NumberFormat (fmtStr);
         // enable alternate formatting for hex or octal, so that
         // a leading 0x or 0 will always be printed
         if ("oxX".indexOf (myFmt.getConversionChar()) != -1) {
            myFmt.setAlternate (true);
         }
      }
   }

   /**
    * Directly sets the formatter used to convert numeric components of the
    * property's value into text.
    * 
    * @param fmt
    * numeric formatter for the property
    * @see #getFormat
    * @see #setPrintFormat(String)
    */
   public void setFormat (NumberFormat fmt) {
      if (fmt == null) {
         myFmt = null;
      }
      else {
         myFmt = new NumberFormat (fmt);
         // enable alternate formatting for hex or octal, so that
         // a leading 0x or 0 will always be printed
         if ("oxX".indexOf (myFmt.getConversionChar()) != -1) {
            myFmt.setAlternate (true);
         }
      }
   }

   /**
    * Returns the formatter used to convert numeric components of the property's
    * value into text. The formatter may be null if there is no numeric data
    * associated with the property.
    * 
    * @return numeric formatter
    * @see #setFormat(NumberFormat)
    * @see #setPrintFormat(String)
    */
   public NumberFormat getFormat() {
      return myFmt;
   }

   /**
    * {@inheritDoc}
    * 
    * @see #setFormat(NumberFormat)
    * @see #setPrintFormat(String)
    */
   public String getPrintFormat() {
      return myFmt != null ? myFmt.toString() : null;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getAutoWrite() {
      return !myReadOnlyP && myAutoWriteP;
   }

   public void setAutoWrite (boolean enable) {
      myAutoWriteP = enable;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getNullValueOK() {
      return myNullValueOK;
   }

   public void setNullValueOK (boolean allowed) {
      if (!allowed && myDefaultValue == null) {
         throw new IllegalArgumentException (
            "Null value disabled but default value is null");
      }
      myNullValueOK = allowed;
   }

   /**
    * {@inheritDoc}
    */
   public Edit getEditing() {
      return myEdit;
   }

   public void setEditing (Edit edit) {
      myEdit = edit;
   }

   /**
    * {@inheritDoc}
    */
   public ExpandState getWidgetExpandState() {
      return myWidgetExpandState;
   }

   public void setWidgetExpandState (ExpandState state) {
      myWidgetExpandState = state;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSliderAllowed() {
      return mySliderAllowed;
   }

   public void setSliderAllowed (boolean allowed) {
      mySliderAllowed = allowed;
   }
 
   /**
    * {@inheritDoc}
    */
   public boolean isReadOnly() {
      return myReadOnlyP;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasRestrictedRange() {
      return (myGetRangeMethod != null ||
              (typeIsNumeric() && myNumericRange != null));
   }

   void setReadOnly (boolean readonly) {
      myReadOnlyP = readonly;
   }

   /**
    * {@inheritDoc}
    */
   public int getDimension() {
      return myDimension;
   }

   public void setDimension (int dim) {
      myDimension = dim;
   }

   public boolean isSharable() {
      return mySharableP;
   }

   void setSharable (boolean sharable) {
      mySharableP = sharable;
   }

   protected Method locateMethod (String methodName, Class<?>... parameterTypes) {
      try {
         return myHostClass.getMethod (methodName, parameterTypes);
      }
      catch (Exception e) {
         // e.printStackTrace();
         throw new IllegalArgumentException ("method '" + methodName
         + "' for property '" + myName + "'\nin " + myHostClass
         + " not found or inaccessible");
      }
   }

   protected void checkReturnType (Method method, Class<?> requiredType) {
      Class<?> retType = method.getReturnType();
      if ((requiredType == Void.TYPE && retType != Void.TYPE) ||
          (requiredType != Void.TYPE &&
           !requiredType.isAssignableFrom (retType))) {
         throw new IllegalArgumentException (
            "method '"+method.getName()+"' for property '"+myName+
            "'\nin "+myHostClass+" returns "+retType+" instead of "+requiredType);
      }
   }

   private void initGetMethod (String methodName) {
      myGetMethod = locateMethod (methodName);
      if (myValueClass == null) {
         setPropertyType (myGetMethod.getReturnType());
      }
      checkReturnType (myGetMethod, myValueClass);
   }

   private void initSetMethod (String methodName) {
      if (myValueClass == null) {
         throw new IllegalStateException (
            "attempt to set set method with value class unknown");
      }
      mySetMethod = locateMethod (methodName, myValueClass);
   }
   
   private void initGetRangeMethod (String methodName) {
      if (myValueClass == null) {
         throw new IllegalStateException (
            "attempt to set getRange method with value class unknown");
      }
      myGetRangeMethod = locateMethod (methodName);
      checkReturnType (myGetRangeMethod, Range.class);
   }

   // not currently used
   private void initGetDefaultMethod (String methodName) {
      if (myValueClass == null) {
         throw new IllegalStateException (
            "attempt to set getDefault method with value class unknown");
      }
      myGetDefaultMethod = locateMethod (methodName);
      checkReturnType (myGetDefaultMethod, myValueClass);
   }

   private void maybeSetGetRangeMethod (String methodName) {
      if (myValueClass == null) {
         throw new IllegalStateException (
            "attempt to set getRange method with value class unknown");
      }
      try {
         myGetRangeMethod = myHostClass.getMethod (methodName);
         checkReturnType (myGetRangeMethod, Range.class);
      }
      catch (Exception e) {
         myGetRangeMethod = null;
      }
   }

   // not currently used
   private void maybeSetGetDefaultMethod (String methodName) {
      if (myValueClass == null) {
         throw new IllegalStateException (
            "attempt to set getDefault method with value class unknown");
      }
      try {
         myGetDefaultMethod = myHostClass.getMethod (methodName);
         checkReturnType (myGetDefaultMethod, myValueClass);
      }
      catch (Exception e) {
         myGetDefaultMethod = null;
      }
   }

   private void initGetModeMethod (String methodName) {
      myGetModeMethod = locateMethod (methodName);
      checkReturnType (myGetModeMethod, PropertyMode.class);
   }

   private void initSetModeMethod (String methodName) {
      mySetModeMethod = locateMethod (methodName, PropertyMode.class);
      checkReturnType (mySetModeMethod, Void.TYPE);
   }

   private void tryToInitCreateMethod (String methodName) {
      try {
         myCreateMethod = myHostClass.getMethod (methodName);
         checkReturnType (myCreateMethod, myValueClass);
      }
      catch (Exception e) {
         myCreateMethod = null;
      }
   }

   public void setGetMethod (String methodName) {
      initGetMethod (methodName);
   }

   public void setSetMethod (String methodName) {
      initSetMethod (methodName);
   }

   public void setGetModeMethod (String methodName) {
      initGetModeMethod (methodName);
   }

   public void setSetModeMethod (String methodName) {
      initSetModeMethod (methodName);
   }

   private Object checkDefaultValue (Object defaultValue) {
      Class<? extends Object> defaultClass = defaultValue.getClass();
      String errorMsg = null;
      boolean floatingDefaultValue =
         (defaultClass == Float.class || defaultClass == Double.class);

      if (myValueClass == Byte.TYPE || myValueClass == Byte.class) {
         if (!floatingDefaultValue && defaultValue instanceof Number) {
            Byte value = new Byte (((Number)defaultValue).byteValue());
            if (!value.equals (defaultValue)) {
               errorMsg = "default value not expressible as a byte";
            }
            else {
               return value;
            }
         }
      }
      else if (myValueClass == Short.TYPE || myValueClass == Short.class) {
         if (!floatingDefaultValue && defaultValue instanceof Number) {
            Short value = new Short (((Number)defaultValue).shortValue());
            if (!value.equals (defaultValue)) {
               errorMsg = "default value not expressible as a short";
            }
            else {
               return value;
            }
         }
      }
      else if (myValueClass == Integer.TYPE || myValueClass == Integer.class) {
         if (!floatingDefaultValue && defaultValue instanceof Number) {
            Integer value = new Integer (((Number)defaultValue).intValue());
            if (!value.equals (defaultValue)) {
               errorMsg = "default value not expressible as an integer";
            }
            else {
               return value;
            }
         }
      }
      else if (myValueClass == Long.TYPE || myValueClass == Long.class) {
         if (!floatingDefaultValue && defaultValue instanceof Number) {
            return new Long (((Number)defaultValue).longValue());
         }
      }
      else if (myValueClass == Float.TYPE || myValueClass == Float.class) {
         if (defaultValue instanceof Number) {
            return new Float (((Number)defaultValue).floatValue());
         }
      }
      else if (myValueClass == Double.TYPE || myValueClass == Double.class) {
         if (defaultValue instanceof Number) {
            return new Double (((Number)defaultValue).doubleValue());
         }
      }
      else if (myValueClass == Character.TYPE) {
         if (defaultValue instanceof Character) {
            return defaultValue;
         }
      }
      else if (myValueClass == Boolean.TYPE) {
         if (defaultValue instanceof Boolean) {
            return defaultValue;
         }
      }
      else if (myValueClass.isAssignableFrom (defaultValue.getClass())) {
         return defaultValue;
      }
      if (errorMsg == null) {
         errorMsg =
            "default value has type " + defaultValue.getClass().getName()
            + ", expecting " + myValueClass.getName();
      }
      throw new IllegalArgumentException ("property '" + myName + "': "
      + errorMsg);
   }

   protected Object createDefaultValue() {
      if (myValueClass == Byte.TYPE || myValueClass == Byte.class) {
         return new Byte ((byte)0);
      }
      else if (myValueClass == Short.TYPE || myValueClass == Short.class) {
         return new Short ((short)0);
      }
      else if (myValueClass == Integer.TYPE || myValueClass == Integer.class) {
         return new Integer (0);
      }
      else if (myValueClass == Long.TYPE || myValueClass == Long.class) {
         return new Long (0);
      }
      else if (myValueClass == Float.TYPE || myValueClass == Float.class) {
         return new Float (0);
      }
      else if (myValueClass == Double.TYPE || myValueClass == Double.class) {
         return new Double (0);
      }
      else if (myValueClass == Character.TYPE) {
         return new Character ('\000');
      }
      else if (myValueClass == Boolean.TYPE) {
         return new Boolean (false);
      }
      else {
         try {
            return myValueClass.newInstance();
         }
         catch (Exception e) {
            throw new IllegalArgumentException ("property '" + myName
            + "': can't automatically create default value for "
            + myValueClass.getName() + "\n" + e.getMessage());
         }
      }
   }

   public void setDefaultValue (Object value) {
      if (myValueClass == null) {
         throw new IllegalStateException (
            "Can't set default value without knowing value class");
      }
      if (value == Property.VoidValue) {
         myDefaultValue = Property.VoidValue;
      }
      else if (value == Property.AutoValue) {
         myDefaultValue = createDefaultValue();
         myDefaultIsAuto = true;
      }
      else if (value == null) {
         myDefaultValue = null;
         setNullValueOK (true);
      }
      else {
         myDefaultValue = checkDefaultValue (value);
      }
   }

   public Object getDefaultValue() {
      return myDefaultValue;
   }

   private void checkGetAndSetMethods (Object host) {
      checkHostClass (host);
      if (myGetMethod == null) {
         throw new IllegalStateException ("no get method for property '"
         + myName + "'");
      }
      if (mySetMethod == null && !myReadOnlyP) {
         throw new IllegalStateException ("no set method for property '"
         + myName + "'");
      }
   }

   /**
    * {@inheritDoc}
    */
   public Property createHandle (HasProperties host) {
      checkGetAndSetMethods (host);
      if (isInheritable()) {
         return new InheritablePropertyHandle (host, this);
      }
      else {
         return new GenericPropertyHandle (host, this);
      }
   }

   protected void checkHostClass (Object host) {
      if (!myHostClass.isAssignableFrom (host.getClass())) {
         throw new IllegalArgumentException ("host class "
         + host.getClass().getName() + " not assignable from "
         + myHostClass.getName());
      }
   }

   protected void methodInvocationError (
      Exception e, HasProperties host, Method method) {
      e.printStackTrace(); 
      throw new InternalErrorException (
         "Error invoking "+host.getClass().getName()+"."+method.getName());
   }

   public Object createInstance (HasProperties host)
      throws InstantiationException, IllegalAccessException {
      if (myCreateMethod == null) {
         return myValueClass.newInstance();
      }
      else {
         try {
            return myCreateMethod.invoke (host);
         }
         catch (Exception e) {
            methodInvocationError (e, host, myCreateMethod);
         }
         return null;
      }
   }

   public Object getValue (HasProperties host) {
      if (myGetMethod == null) {
         return host.getProperty (myName).get();
      }
      else {
         checkHostClass (host);
         try {
            return myGetMethod.invoke (host);
         }
         catch (Exception e) {
            methodInvocationError (e, host, myGetMethod);
         }
         return null;
      }
   }

   public Range getRange (HasProperties host) {
      if (myGetRangeMethod != null) {
         checkHostClass (host);
         try {
            return (Range)myGetRangeMethod.invoke (host);
         }
         catch (Exception e) {
            methodInvocationError (e, host, myGetRangeMethod);
         }
         return null;
      }
      else if (typeIsNumeric()) {
         return myNumericRange;
      }
      else {
         return null;
      }
   }

   public void setValue (HasProperties host, Object value) {
      if (mySetMethod == null) {
         host.getProperty (myName).set (value);
      }
      else {
         checkHostClass (host);
         try {
            mySetMethod.invoke (host, value);
         }
         catch (Exception e) {
            methodInvocationError (e, host, mySetMethod);
         }
      }
   }

   protected InheritableProperty getInheritableProperty (HasProperties host) {
      try {
         return (InheritableProperty)host.getProperty (myName);
      }
      catch (ClassCastException e) {
         throw new InternalErrorException ("property '" + myName
         + "' is not inheritable");
      }
   }

   public PropertyMode getMode (HasProperties host) {
      if (myGetModeMethod == null) {
         return getInheritableProperty (host).getMode();
      }
      else {
         checkHostClass (host);
         try {
            return (PropertyMode)myGetModeMethod.invoke (host);
         }
         catch (Exception e) {
            methodInvocationError (e, host, myGetModeMethod);
         }
         return null;
      }
   }

   public void setMode (HasProperties host, PropertyMode mode) {
      if (mySetModeMethod == null) {
         getInheritableProperty (host).setMode (mode);
      }
      else {
         checkHostClass (host);
         try {
            mySetModeMethod.invoke (host, mode);
         }
         catch (Exception e) {
            methodInvocationError (e, host, mySetModeMethod);
         }
      }
   }

   protected static String capitalize (String str) {
      return Character.toUpperCase (str.charAt (0)) + str.substring (1);
   }

   static String propTypeName (int propType) {
      switch (propType) {
         case REGULAR:
            return "regular";
         case READ_ONLY:
            return "readOnly";
         case INHERITABLE:
            return "inheritable";
         default:
            return "???";
      }
   }

   static String[] parseMethodNames (
      String nameAndMethods, String[] defaults, int propType) {
      String[] splits = nameAndMethods.split ("\\s+");
      if (splits.length < 1) {
         throw new IllegalArgumentException (
            "Error: Blank name/method specifier");
      }
      int maxMethods = 3;
      if (propType == READ_ONLY) {
         maxMethods = 1;
      }
      else if (propType == INHERITABLE) {
         maxMethods = 5;
      }
      if (splits.length > maxMethods + 1) {
         throw new IllegalArgumentException (
            "Error: too many method names specified for "
            + propTypeName (propType) + "property");
      }

      String[] strs = new String[maxMethods + 2];

      String propName = splits[0];

      int qidx = propName.indexOf (':');
      if (qidx != -1) {
         strs[maxMethods + 1] = propName.substring (qidx + 1); // qualifier
         propName = propName.substring (0, qidx);
      }
      strs[0] = propName;

      if (!isJavaIdentifier (propName)) {
         throw new IllegalArgumentException ("Error: property name '"
         + propName + "' is not a Java identifier");
      }

      for (int i = 1; i < maxMethods + 1; i++) {
         if (i >= splits.length || splits[i].equals ("*")) {
            strs[i] = defaults[i - 1].replace ("XXX", capitalize (propName));
         }
         else {
            if (!isJavaIdentifier (splits[i])) {
               throw new IllegalArgumentException ("Error: method name '"
               + splits[i] + "' is not a Java identifier");
            }
            strs[i] = splits[i];
         }
      }
      return strs;
   }

   protected void parseQualifier (String qualifier) {
      if (qualifier.equals ("Inherited")) {
         myDefaultMode = PropertyMode.Inherited;
      }
      else if (qualifier.equals ("Explicit")) {
         myDefaultMode = PropertyMode.Explicit;
      }
      else {
         throw new IllegalArgumentException (
            "Unknown or inappropriate qualifier " + qualifier
            + " for property '" + myName + "'");
      }
      if (!isInheritable()) {
         throw new IllegalArgumentException ("Inappropriate qualifier "
         + qualifier + " for non-inheritable property '" + myName + "'");
      }
   }

   static String[] defaultNames =
     { "getXXX", "setXXX", "getXXXRange", "getXXXMode", "setXXXMode" };

   static public boolean initialize (
      PropertyDesc desc, String nameAndMethods, Class<?> hostClass,
      String descriptor, Object defaultValue, String options, int propType) {
      String[] strs;
      strs = parseMethodNames (nameAndMethods, defaultNames, propType);
      if (strs == null) {
         return false;
      }
      String propName = strs[0];
      String qualifier = strs[strs.length - 1];

      desc.setName (propName);
      desc.setHostClass (hostClass);
      desc.setGetMethod (strs[1]);
      if (propType == REGULAR || propType == INHERITABLE) {
         desc.setSetMethod (strs[2]);
         desc.maybeSetGetRangeMethod (strs[3]);
      }

      if (propType == INHERITABLE) {
         desc.setGetModeMethod (strs[4]);
         desc.setSetModeMethod (strs[5]);
      }

      if (descriptor != null) {
         desc.setDescription (descriptor);
      }
      if (propType == READ_ONLY) {
         desc.setReadOnly (true);
         desc.setAutoWrite (false);
      }
      if (options != null) {
         desc.parseOptions (options);
      }
      if (propType == INHERITABLE) {
         desc.setInheritable (true);
      }
      if (qualifier != null) {
         desc.parseQualifier (qualifier);
      }
      if (desc.isReadOnly() && defaultValue != Property.VoidValue) {
         System.err.println (
            "Warning: default value ignored for read-only property " +
            propName);
      }
      desc.setDefaultValue (defaultValue);
      if (desc.myDimension == -1 &&
          desc.typeIsNumeric() &&
          desc.myDefaultValue != null) {
         desc.setDimension (desc.getDimensionFromDefaultValue());
      }
      
      // For composite properties, see if there is a createXXX method
      if (CompositeProperty.class.isAssignableFrom (desc.myValueClass)) {
         desc.tryToInitCreateMethod ("create" + capitalize (propName));
      }

      return true;
   }

   public boolean valueEqualsDefault (Object value) {
      if (myDefaultValue == Property.VoidValue) {
         return false;
      }
      else if (myDefaultValue == null) {
         return value == null;
      }
      else if (myValueClass.isArray()) {
         switch (myValueType) {
            case SHORT_ARRAY: {
               short[] array = (short[])value;
               short[] deflt = (short[])myDefaultValue;
               if (array.length != deflt.length) {
                  return false;
               }
               for (int i = 0; i < array.length; i++) {
                  if (array[i] != deflt[i]) {
                     return false;
                  }
               }
               break;
            }
            case INT_ARRAY: {
               int[] array = (int[])value;
               int[] deflt = (int[])myDefaultValue;
               if (array.length != deflt.length) {
                  return false;
               }
               for (int i = 0; i < array.length; i++) {
                  if (array[i] != deflt[i]) {
                     return false;
                  }
               }
               break;
            }
            case LONG_ARRAY: {
               long[] array = (long[])value;
               long[] deflt = (long[])myDefaultValue;
               if (array.length != deflt.length) {
                  return false;
               }
               for (int i = 0; i < array.length; i++) {
                  if (array[i] != deflt[i]) {
                     return false;
                  }
               }
               break;
            }
            case FLOAT_ARRAY: {
               float[] array = (float[])value;
               float[] deflt = (float[])myDefaultValue;
               if (array.length != deflt.length) {
                  return false;
               }
               for (int i = 0; i < array.length; i++) {
                  if (array[i] != deflt[i]) {
                     return false;
                  }
               }
               break;
            }
            case DOUBLE_ARRAY: {
               double[] array = (double[])value;
               double[] deflt = (double[])myDefaultValue;
               if (array.length != deflt.length) {
                  return false;
               }
               for (int i = 0; i < array.length; i++) {
                  if (array[i] != deflt[i]) {
                     return false;
                  }
               }
               break;
            }
            default:
               break;
         }
         return true;
      }
      else if (Vector.class.isAssignableFrom (myValueClass)) {
         return ((Vector)myDefaultValue).equals ((Vector)value);
      }
      else if (Vectori.class.isAssignableFrom (myValueClass)) {
         return ((Vectori)myDefaultValue).equals ((Vectori)value);
      }
      else if (Matrix.class.isAssignableFrom (myValueClass)) {
         return ((Matrix)myDefaultValue).equals ((Matrix)value);
      }
      else {
         boolean equal = myDefaultValue.equals (value);
         return equal;
      }
   }
      
   int cnt = 0;

   /**
    * {@inheritDoc}
    * 
    * @see #setNumericRange
    */
   public NumericInterval getDefaultNumericRange() {
      return myNumericRange;
   }

   /**
    * Sets a NumericRange for this property. A null argument will cause any
    * existing NumericRange to be removed. At present, numeric ranges are mainly
    * used when automatically creating GUI sliders to manipulate property
    * values.
    * 
    * @param rng
    * new numeric range for this property (value is copied).
    */
   public void setNumericRange (NumericInterval rng) {
      myNumericRange = (rng == null ? null : new DoubleInterval (rng));
   }

   private static String classToName (Class<? extends Object> cls) {
      // was ClassAliases.getAliasOrName (cls);
      return cls.getName();
   }

   private static Class<?> nameToClass (String name) {
      // was: ClassAliases.resolveClass (name)
      try {
         return Class.forName (name);
      }
      catch (Exception e) {
         return null;
      }
   }

   protected static void writeMatrix (PrintWriter pw, Matrix M, NumberFormat fmt)
      throws IOException {
      pw.print ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      M.write (pw, fmt);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   protected static void writeVector (PrintWriter pw, Vector v, NumberFormat fmt)
      throws IOException {
      pw.print ("[ ");
      v.write (pw, fmt);
      pw.println (" ]");
   }

   protected static void writeVectori (
      PrintWriter pw, Vectori v, NumberFormat fmt)
      throws IOException {
      pw.print ("[ ");
      v.write (pw, fmt);
      pw.println (" ]");
   }

   protected static void writeAxisAngle (
      PrintWriter pw, AxisAngle axisAng, NumberFormat fmt) {
      pw.print ("[ ");
      pw.print (fmt.format (axisAng.axis.x));
      pw.print (' ');
      pw.print (fmt.format (axisAng.axis.y));
      pw.print (' ');
      pw.print (fmt.format (axisAng.axis.z));
      pw.print (' ');
      pw.print (fmt.format (Math.toDegrees (axisAng.angle)));
      pw.println (" ]");
   }

   protected static AxisAngle scanAxisAngle (ReaderTokenizer rtok)
      throws IOException {
      rtok.scanToken ('[');
      double x = rtok.scanNumber();
      double y = rtok.scanNumber();
      double z = rtok.scanNumber();
      double ang = rtok.scanNumber();
      rtok.scanToken (']');

      return new AxisAngle (x, y, z, Math.toRadians (ang));
   }

   public boolean writeIfNonDefault (
      HasProperties host, PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {

      Object value = getValue (host);
      PropertyMode mode = PropertyMode.Explicit;

      if (isInheritable()) {
         mode = getMode (host);
      }
      if (mode == PropertyMode.Explicit &&
          (getDefaultMode() == PropertyMode.Inherited ||
           !valueEqualsDefault (value)) &&
          (value == null || ClassAliases.isClassValid(value.getClass()))) {
         pw.print (myName + "=");
         writeValue (value, pw, fmt, ref);
         return true;
      }
      else if (mode != getDefaultMode()) {
         pw.println (myName + ":" + mode + " ");
         return true;
      }
      else {
         return false;
      }
   }

   public static boolean writeValue (
      Object value, PrintWriter pw, TypeCode valueType, Class<?> valueClass,
      NumberFormat floatFmt, NumberFormat fmt, Object ref)
      throws IOException {

      switch (valueType) {
         case BYTE: {
            if (fmt == null) {
               pw.println ("0x"
               + Integer.toHexString (((Byte)value).byteValue()));
            }
            else {
               pw.println (fmt.format (((Byte)value).byteValue()));
            }
            break;
         }
         case CHAR: {
            pw.println ("'" + ((Character)value).charValue() + "'");
            break;
         }
         case SHORT: {
            if (fmt == null) {
               pw.println (((Short)value).shortValue());
            }
            else {
               pw.println (fmt.format (((Short)value).shortValue()));
            }
            break;
         }
         case INT: {
            if (fmt == null) {
               pw.println (((Integer)value).intValue());
            }
            else {
               pw.println (fmt.format (((Integer)value).intValue()));
            }
            break;
         }
         case LONG: {
            if (fmt == null) {
               pw.println (((Long)value).longValue());
            }
            else {
               pw.println (fmt.format (((Long)value).longValue()));
            }
            break;
         }
         case FLOAT: {
            if (floatFmt == null) {
               pw.println (((Float)value).floatValue());
            }
            else {
               pw.println (floatFmt.format (((Float)value).floatValue()));
            }
            break;
         }
         case DOUBLE: {
            if (floatFmt == null) {
               pw.println (((Double)value).doubleValue());
            }
            else {
               pw.println (floatFmt.format (((Double)value).doubleValue()));
            }
            break;
         }
         case BOOLEAN: {
            pw.println (((Boolean)value).booleanValue());
            break;
         }
         case SHORT_ARRAY: {
            Write.writeShorts (pw, (short[])value, fmt);
            break;
         }
         case INT_ARRAY: {
            Write.writeInts (pw, (int[])value, fmt);
            break;
         }
         case LONG_ARRAY: {
            Write.writeLongs (pw, (long[])value, fmt);
            break;
         }
         case FLOAT_ARRAY: {
            Write.writeFloats (pw, (float[])value, floatFmt);
            break;
         }
         case DOUBLE_ARRAY: {
            Write.writeDoubles (pw, (double[])value, floatFmt);
            break;
         }
         case COLOR: {
            Write.writeColor (pw, (Color)value);
            break;
         }
         case AXIS_ANGLE: {
            writeAxisAngle (pw, (AxisAngle)value, floatFmt);
            break;
         }
         case ENUM: {
            pw.println ((Enum<?>)value);
            break;
         }
         case VECTOR: {
            writeVector (pw, (Vector)value, floatFmt);
            break;
         }
         case VECTORI: {
            writeVectori (pw, (Vectori)value, floatFmt);
            break;
         }
         case MATRIX: {
            writeMatrix (pw, (DenseMatrix)value, floatFmt);
            break;
         }
         case STRING: {
            Write.writeString (pw, (String)value);
            break;
         }
         case DIMENSION: {
            java.awt.Dimension dim = (java.awt.Dimension)value;
            pw.println ("[ " + dim.width + " " + dim.height + " ]");
            break;
         }
         case POINT: {
            java.awt.Point loc = (java.awt.Point)value;
            pw.println ("[ " + loc.x + " " + loc.y + " ]");
            break;
         }
         case FONT: {
            Font font = (Font)value;
            Write.writeFont (pw, font);
            break;
         }
         case SCANABLE: {
            if (value != null && valueClass != value.getClass()) {
               if (valueClass.isAssignableFrom (value.getClass())) {
                  pw.print (value.getClass().getName() + " ");
               }
               else {
                  throw new IOException ("Value class " + value.getClass()
                  + " not a subclass of " + valueClass.getName());
               }
            }
            if (value == null) {
               pw.println ("null");
            }
            else {
               ((Scannable)value).write (pw, floatFmt, ref);
            }
            break;
         }
         default: {
            throw new IOException (
               "Unknown class type for value: " + value.getClass().getName());
         }
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public void writeValue (
      Object value, PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      // supplied format for floating point only
      NumberFormat floatFmt = (fmt != null ? fmt : myFmt);

      try {
         writeValue (
            value, pw, myValueType, myValueClass, floatFmt, myFmt, ref);
      }
      catch (IOException e) {
         throw new IOException (
            "Can't property '"+myName+"': " + e.getMessage());
      }
   }

   /**
    * Static version of {@link #scanValue}, with the value type and
    * class explicitly specified.
    * 
    * @param rtok 
    * ReaderTokenizer supplying input tokens used to specify the property value
    * @param valueType
    * Code specifying the class type of the value. It is possible to
    * obtain this from {@code valueClass} by calling {@link #getTypeCode}.
    * @param valueClass
    * Class type for the value
    * @return scanned object value
    * @throws IOException
    * if the input is not in the correct format, an I/O error occurred, or the
    * value has a type which PropertyInfo does not know about
    */
   public static Object scanValue (
      ReaderTokenizer rtok, TypeCode valueType, Class<?> valueClass)
      throws IOException {

      switch (valueType) {
         case BYTE: {
            return (new Byte ((byte)rtok.scanInteger()));
         }
         case CHAR: {
            String charStr = rtok.scanQuotedString ('\'');
            if (charStr.length() == 0) {
               throw new IOException ("No character between quotes, line "
               + rtok.lineno());
            }
            else if (charStr.length() > 1) {
               throw new IOException (
                  "Multiple characters between quotes, line " + rtok.lineno());
            }
            else {
               return new Character (charStr.charAt (0));
            }
         }
         case SHORT: {
            return (new Short (rtok.scanShort()));
         }
         case INT: {
            return (new Integer (rtok.scanInteger()));
         }
         case LONG: {
            return (new Long (rtok.scanLong()));
         }
         case FLOAT: {
            return (new Float ((float)rtok.scanNumber()));
         }
         case DOUBLE: {
            return (new Double (rtok.scanNumber()));
         }
         case BOOLEAN: {
            return (new Boolean (rtok.scanBoolean()));
         }
         case SHORT_ARRAY: {
            return Scan.scanShorts (rtok);
         }
         case INT_ARRAY: {
            return Scan.scanInts (rtok);
         }
         case LONG_ARRAY: {
            return Scan.scanLongs (rtok);
         }
         case FLOAT_ARRAY: {
            return Scan.scanFloats (rtok);
         }
         case DOUBLE_ARRAY: {
            return Scan.scanDoubles (rtok);
         }
         case COLOR: {
            return Scan.scanColor (rtok);
         }
         case AXIS_ANGLE: {
            return scanAxisAngle (rtok);
         }
         case ENUM: {
            rtok.scanWord();

            Enum<?>[] validEnums = (Enum[])valueClass.getEnumConstants();
            for (int i = 0; i < validEnums.length; i++) {
               if (validEnums[i].toString().equals (rtok.sval)) {
                  return validEnums[i];
               }
            }
            throw new IOException ("Enum '" + rtok.sval
            + "' not recognized, line " + rtok.lineno());
         }
         case VECTOR: {
            Vector vobj;
            try {
               vobj = (Vector)valueClass.newInstance();
            }
            catch (Exception e) {
               e.printStackTrace();
               throw new IOException ("Cannot instantiate Vector");
            }
            vobj.scan (rtok);
            return vobj;
         }
         case VECTORI: {
            Vectori vobj;
            try {
               vobj = (Vectori)valueClass.newInstance();
            }
            catch (Exception e) {
               e.printStackTrace();
               throw new IOException ("Cannot instantiate Vectori");
            }
            vobj.scan (rtok);
            return vobj;
         }
         case MATRIX: {
            DenseMatrix mobj;
            try {
               mobj = (DenseMatrix)valueClass.newInstance();
            }
            catch (Exception e) {
               e.printStackTrace();
               throw new IOException ("Cannot instantiate Matrix");
            }
            mobj.scan (rtok);
            return mobj;
         }
         case STRING: {
            return rtok.scanQuotedString ('"');
         }
         case DIMENSION: {
            rtok.scanToken ('[');
            int w = rtok.scanInteger();
            int h = rtok.scanInteger();
            rtok.scanToken (']');
            return new java.awt.Dimension (w, h);
         }
         case POINT: {
            rtok.scanToken ('[');
            int x = rtok.scanInteger();
            int y = rtok.scanInteger();
            rtok.scanToken (']');
            return new java.awt.Point (x, y);
         }
         case FONT: {
            return Scan.scanFont (rtok);
         }
         case SCANABLE: {
            Object sobj = Scan.scanInstance (rtok, valueClass);
            if (sobj instanceof Scannable) {
               // paranoid - should be Scannable if valueType is SCANNABLE
               ((Scannable)sobj).scan (rtok, null);
            }
            return sobj;
         }
         default: {
            throw new IOException (
               "Unknown class type for value: " + valueClass.getName());
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public Object scanInstance (ReaderTokenizer rtok) throws IOException {
      try {
         return Scan.scanInstance (rtok, (Class<?>)myValueClass);
      }
      catch (IOException e) {
         throw new IOException (
            "Property '"+myName+"': " + e.getMessage());
      }      
   }

   /**
    * {@inheritDoc}
    */
   public Object scanValue (ReaderTokenizer rtok) throws IOException {
      try {
         return scanValue (rtok, myValueType, myValueClass);
      }
      catch (IOException e) {
         throw new IOException (
            "Property '"+myName+"': " + e.getMessage());
      }
   }

   protected void parseOptions (String optionStr)
      throws IllegalArgumentException {
      String[] tokens = optionStr.split ("\\s+");

      for (int i = 0; i < tokens.length; i++) {
         int firstChar = tokens[i].charAt (0);
         String token = tokens[i];

         if (firstChar == '%') {
            try {
               setPrintFormat (token);
            }
            catch (IllegalArgumentException e) {
               System.err.println ("Property '" + myName
               + "': illegal numeric format string");
            }
         }
         else if (firstChar == '[' || firstChar == '(') {
            NumericInterval range = new DoubleInterval (token);
            setNumericRange (range);
         }
         else if (token.equals ("NE") || token.equals ("NeverEdit")) {
            setEditing (Edit.Never);
         }
         else if (token.equals ("NW") || token.equals ("NoAutoWrite")) {
            setAutoWrite (false);
         }
         else if (token.equals ("AE") || token.equals ("AlwaysEdit")) {
            setEditing (Edit.Always);
         }
         else if (token.equals ("1E") || token.equals ("SingleEdit")) {
            setEditing (Edit.Single);
         }
         else if (token.equals ("XE") || token.equals ("ExpandedEdit")) {
            setWidgetExpandState (ExpandState.Expanded);
         }
         else if (token.equals ("CE") || token.equals ("ContractedEdit")) {
            setWidgetExpandState (ExpandState.Contracted);
         }
         else if (token.equals ("AW") || token.equals ("AutoWrite")) {
            setAutoWrite (true);
         }
         else if (token.equals ("SH") || token.equals ("Sharable")) {
            setSharable (true);
         }
         else if (token.equals ("NV") || token.equals ("NullOK")) {
            setNullValueOK (true);
         }
         else if (token.equals ("NS") || token.equals ("NoSlider")) {
            setSliderAllowed (false);
         }
         else if (token.startsWith ("D")) {
            String dimStr;
            if (token.startsWith ("Dimension")) {
               dimStr = token.substring ("Dimension".length());
            }
            else {
               dimStr = token.substring ("D".length());
            }
            int dim;
            try {
               dim = Integer.parseInt (dimStr);
               if (myDimension != -1 && dim != myDimension) {
                  throw new IllegalArgumentException ("Property '" + myName
                  + "': dimension specified as " + dim + " but is known to be "
                  + myDimension);
               }
               else if (myDimension == -1 && !typeIsNumeric()) {
                  System.err.println (
                     "Warning: dimension specified for non-numeric property '" +
                     myName + "'");
               }
               setDimension (dim);
            }
            catch (NumberFormatException e) {
               throw new IllegalArgumentException ("Property '" + myName
               + "': malformed dimension expression: " + token);
            }

         }
         else {
            throw new IllegalArgumentException ("Property '" + myName
            + " unrecognized option token " + token);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isInheritable() {
      return myInheritableP;
   }

   /**
    * {@inheritDoc}
    */
   public PropertyMode getDefaultMode() {
      return myDefaultMode;
   }

   void setInheritable (boolean enable) {
      myInheritableP = enable;
   }
   
   public void setAllowedTypes (List<Class<?>> types) {
      if (types == null) {
         myAllowedTypes = null;
      }
      else {
         myAllowedTypes = new ArrayList<Class<?>>(types.size());
         for (Class<?> clazz : types) {
            if (clazz == null || !myValueClass.isAssignableFrom (clazz)) {
               throw new IllegalArgumentException (
                  "Type "+clazz+" is not a subclass of the valueClass "+
                  myValueClass+" for property '"+myName+"'");
            }
            myAllowedTypes.add (clazz);
         }
      }
   }

   /**
    * {@inheritDoc}
    */   
   public List<Class<?>> getAllowedTypes() {
      return myAllowedTypes;
   }
}
