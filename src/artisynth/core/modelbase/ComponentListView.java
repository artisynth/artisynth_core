/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;
import java.io.*;
import maspack.util.*;

/**
 * A read-only view of a ComponentList that allows it's contents to be queried
 * but not modified.
 * 
 * @author John E Lloyd
 * 
 * @param <E>
 * Model component type found in this list
 */
public interface ComponentListView<E extends ModelComponent> extends
ListView<E> {
   /**
    * Return an iterator over all components in this list.
    * 
    * @return iterator over list components
    */
   Iterator<E> iterator();

   /**
    * Get the component at a particular index, or null if there is no such
    * component.
    * 
    * @param idx
    * index of the component
    * @return component at specified index
    */
   E get (int idx);

   /**
    * Get the component with the specified number, or null if there is no such
    * component.
    * 
    * @param num
    * number of the component
    * @return component with specified number
    */
   E getByNumber (int num);

   /**
    * Get the component with particular name, or null if there is no such
    * component.
    * 
    * @param name
    * name of the component
    * @return component with specified name
    */
   E get (String name);

   /**
    * Get the number of components in this list.
    * 
    * @return number of components
    */
   int size();

   /**
    * Get the index of a particular component in this list, or -1 if the
    * specified component is not present.
    * 
    * @param comp
    * component to search for
    * @return index of the component within this list
    */
   int indexOf (ModelComponent comp);

   /**
    * Returns true if a particular component is contained in this list.
    * 
    * @param comp
    * component to search for
    * @return true if the component is contained in this list
    */
   boolean contains (E comp);

   /**
    * Returns the number that will be assigned to the next component added to
    * this list.
    * 
    * @return next component number for this list
    */
   public int nextComponentNumber();

   /**
    * Gets the name of this component list.
    * 
    * @return name of this list
    */
   String getName();

   /**
    * Gets the short name of this component list.
    * 
    * @return short name of this list
    */
   String getShortName();
}
