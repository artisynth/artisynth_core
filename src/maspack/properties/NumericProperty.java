/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

/**
 * Property object which returns a set of numeric values
 */
public interface NumericProperty extends Property {
   /**
    * Returns the number of numeric values associated with this NumericProperty
    * object.
    * 
    * @return number of numeric values
    */
   public int numValues();
}
