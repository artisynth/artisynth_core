/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;
import java.io.*;
import maspack.util.*;

import java.awt.Color;

/**
 * List containing information about a set of properties.
 */
public interface PropertyInfoList extends Iterable<PropertyInfo> {
   /**
    * Gets information for a specific named property. If the named property is
    * not defined in this list, <code>null</code> is returned.
    * 
    * @param name
    * name of the property
    * @return information for this property, if present
    */
   public PropertyInfo get (String name);

   // /**
   // * Gets information for a specific property by index, where index
   // * describes the numeric location of the property within this list.
   // *
   // * @param idx index of the property within this list
   // * @return information for this property
   // * @throws ArrayIndexOutOfBoundsException if index is
   // * not in the range [0, {@link #size size()}-1].
   // */
   // public PropertyInfo get (int idx);

   /**
    * Returns an iterator over all PropertyInfo structures contained in this
    * list.
    * 
    * @return iterator for this list
    */
   public Iterator<PropertyInfo> iterator();

   /**
    * Returns the number of properties described in this list.
    * 
    * @return size of this list
    */
   public int size();

   /**
    * Returns tree if any properties in this list are inheritable.
    * 
    * @return true if list contains inheritable properties
    */
   public boolean hasNoInheritableProperties();

}
