/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * Describes a class that is parameterized by an additional class type. An good
 * example of this is a container whose contents must be instances of the type
 * parameter.
 */
public interface ParameterizedClass<C> {

   /**
    * Returns the base type for the class parameterization.
    *
    * @return parameterization class
    */
   Class<C> getTypeParameter();

   /**
    * Queries whether this class explicitly utilizes a parameterized type.
    *
    * @return <code>true</code> if this class utilizes a parameterized type.
    */
   boolean hasParameterizedType();

}
