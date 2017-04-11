/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import maspack.util.Clonable;

/**
 * An object containing properties which may be inherited from an ancestor
 * object.
 */
public interface CompositeProperty extends HasProperties, Clonable {

   /**
    * Returns the host for this property
    *
    * @return host for this property
    */
   HasProperties getPropertyHost();

   /**
    * If this object is a property, returns the corresponding information
    *
    * @return property information for this object
    */
   PropertyInfo getPropertyInfo();

   /**
    * Sets the host for this property.
    * 
    * @param host
    * host for this property
    */
   void setPropertyHost (HasProperties host);

   /**
    * Sets the property information for this property.
    * 
    * @param info
    * property information
    */
   void setPropertyInfo (PropertyInfo info);

   /**
    * Returns a clone of this composite property.
    * 
    * @return clone of this property
    */
   Object clone() throws CloneNotSupportedException;
}
