/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * An object representing the set of valid values that an object can have.
 */
public interface Range extends Scannable, Clonable {
//   /**
//    * Returns true if a specified object has a value allowed by (and a type
//    * compatible with) this Range.
//    * 
//    * @param obj
//    * object to be tested
//    * @return true if the object's value is within range
//    */
//   boolean withinRange (Object obj);
   
   /**
    * Special object to indicate an illegal value,
    */
   public static final Class<?> IllegalValue = IllegalValue.class;
   
   /**
    * Returns true if the specified object is valid for this Range,
    * and false otherwise. If the object is not valid, and <code>errMsg</code>
    * is not <code>null</code>, then <code>errMsg.value</code> should
    * be set to a message describing why the object is not valid.
    * 
    * @param obj Object to be testes
    * @param errMsg Optional handle for storing error message
    * @return true if the object is valid
    */
   boolean isValid (Object obj, StringHolder errMsg);

   /**
    * Projects an object to lie within the range allowed by this Range, if
    * possible.  If <code>obj</code> is already within the range, then
    * <code>obj</code> should be returned unchanged. Otherwise, if it can be
    * projected to the range, then a new (projected) object should be
    * returned. Otherwise, if it cannot be projected, {@link #IllegalValue}
    * should be returned.
    *
    * <p>
    * In particular, <code>projectToRange(obj) != obj</code> should
    * be a valid indication that the <code>obj</code> is not within range.
    *
    * @param obj
    * object to be projected
    * @return original obj if within range, or a projected object, or
    * <code>null</code>.
    */
   Object makeValid (Object obj);

   // /**
   //  * Returns true if a specified object is type compatible with this Range.
   //  * 
   //  * @param obj
   //  * object to be tested
   //  * @return true if the object is type compatible with this Range
   //  */
   // boolean isTypeCompatible (Object obj);

//   /**
//    * Returns a string description of the allowed values of this Range.  This
//    * will be mainly used for automatically creating information messages for
//    * the user. Some examples are:
//    *
//    * <pre>
//    *    Values must lie in the numeric range [0,11]
//    *
//    *    Values must be instances of Vector3d or Point3d
//    * </pre>
//    * It is permissible to return <code>null</code> as a default.
//    *
//    * @return a string description of this range
//    */
//   String getDescription();

   /** 
    * Returns true if this range is empty - i.e., if there are no valid
    * values. This will typically result from the intersection of two
    * non-overlapping ranges.
    * 
    * @return true if this range has no valid values.
    */
   boolean isEmpty();

   /** 
    * Intersects the set of valid values of this Range with those of
    * another. If the resulting intersection is null, then {@link #isEmpty}
    * should subsequently return <code>true</code>.
    * 
    * @param r range to intersect with.
    */
   void intersect (Range r);

}

class IllegalValue {
}

