/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import maspack.util.Range;

/**
 * A handle object used to access values and obtain information for a specific
 * property exported by an object.
 */
public interface Property {
   /**
    * Special class type indicating no specified value. This is used instead of
    * <code>null</code> since the latter may itself be a valid value.
    */
   public static final Class<?> VoidValue = VoidValue.class;

   /**
    * Special class type indicating that a value is to be determined
    * automatically.
    */
   public static final Class<?> AutoValue = AutoValue.class;

   /**
    * Special class type indicating an invalid value.
    */
   public static final Class<?> IllegalValue = InvalidValue.class;

   /**
    * Special value of double that can optionally be used to denote default
    * values. Magnitude is large enough that it is unlikely to occur, while
    * also being easy to type into an interface.
    */
   public static double DEFAULT_DOUBLE = -1e100;

   /**
    * Returns the value associated with this property. By default, the caller
    * should (defensively) assume that this value is returned by reference, and
    * that modifying it will therefore cause changes within the host.
    * 
    * @return value object
    * @see #getInfo
    */
   public Object get();

   /**
    * Sets the value associated with this property. This routine will have no
    * effect if the {@link maspack.properties.PropertyInfo PropertyInfo} method
    * {@link maspack.properties.PropertyInfo#isReadOnly isReadOnly} returns
    * true.
    * 
    * @param value
    * object containing the value to be set.
    * @see #getInfo
    */
   public void set (Object value);

   /**
    * Returns a range that indicates what values are legal for 
    * for this property, or <code>null</code> if the
    * property does not have any range limits (i.e., if
    * any value of appropriate type specified to {@link #get}
    * is valid.
    * 
    * @return range limits for the property, or <code>null</code>
    * if there are no limits.
    */
   public Range getRange();
   
//   /**
//    * Validates a specified value for this property. If value is legal, then it
//    * should be returned directly by the method. If the value is not legal, then
//    * the method may either return an alternate legal value, or #InvalidValue.
//    * The argument <code>errMsg</code>, if non-null, is used to return either
//    * <code>null</code> for legal values or an informative error message for
//    * illegal ones.
//    * 
//    * @param value
//    * object containing the value to be validated
//    * @param errMsg
//    * optional argument used to return error messages for illegal values
//    * @return the value object, if the value is legal, or an alternate legal
//    * value or #InvalidValue, if the value if not legal.
//    */
//   public Object validate (Object value, StringHolder errMsg);

   /**
    * Returns the name associated with this Property. This is a convenience
    * routine, since the name can also be obtained using
    * 
    * <pre>
    * getInfo().getName();
    * </pre>
    * 
    * @return name associated with this property
    */
   public String getName();

   /**
    * Returns the host object exporting this property.
    * 
    * @return host exporting this property
    */
   public HasProperties getHost();

   /**
    * Gets static information about the property.
    * 
    * @return static property information
    */
   public PropertyInfo getInfo();

   // /**
   // * Returns true if the value returned by {@link #get get} is a
   // * reference to an internal host object and should therefore not be
   // * modified.
   // *
   // * @return true if the property value is returned by reference
   // */
   // boolean valueReturnedByReference();

}

class VoidValue {
}

class AutoValue {
}

class InvalidValue {
}
