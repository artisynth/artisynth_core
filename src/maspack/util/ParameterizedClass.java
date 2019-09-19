/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

/**
 * Describes a class that is parameterized by an additional class type. An good
 * example of this is a container whose contents must be instances of the type
 * parameter.
 */
public interface ParameterizedClass {

   /**
    * Returns the base type for the class parameterization.
    *
    * @return parameterization type
    */
   Class<?> getParameterType();
   
   /**
    * Queries whether this class explicitly utilizes a parameterized type.
    * This methods allows ParameterizedType subclasses to <i>not</i> be
    * explicitly parameterized.
    *
    * @return <code>true</code> if this class utilizes a parameterized type.
   */
   boolean hasParameterizedType();

}
