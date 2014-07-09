/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

/**
 * An object which exports access to internal attributes via {@link
 * maspack.properties.Property Property} objects.
 */
public interface HasProperties {
   /**
    * Returns a property associated with a specified path name. All properties
    * exported by this object must have a name which is a valid Java identifier.
    * Specifying one of these names causes the corresponding Property to be
    * returned. Handles to sub-properties can also be obtained by delimiting the
    * sub-property name(s) by '<code>.</code>' characters. A sub-property
    * exists if the value of a property is itself an object which exports
    * properties. It is only possible to obtain a sub-property handle if each of
    * its ancestor properties exports their values by reference.
    * 
    * @param pathName
    * name of the desired property or sub-property
    * @return handle to the property
    */
   public Property getProperty (String pathName);

   /**
    * Returns a list giving static information about all properties exported by
    * this object.
    * 
    * @return static information for all exported properties
    */
   public PropertyInfoList getAllPropertyInfo();
}
