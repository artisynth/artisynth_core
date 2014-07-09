/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Iterator;

public interface ListView<E> extends Iterable<E> {
   /**
    * Return an iterator over all elements in this list.
    * 
    * @return iterator over list elements
    */
   Iterator<E> iterator();

   /**
    * Get the element at a particular index, or null if there is no such
    * element.
    * 
    * @param idx
    * index of the element
    * @return element at specified index
    */
   E get (int idx);

   /**
    * Get the number of elements in this list.
    * 
    * @return number of elements
    */
   int size();

   // /**
   // * Get the index of a particular element in this list, or -1 if the
   // * specified element is not present.
   // *
   // * @param elem element to search for
   // * @return index of the element within this list
   // */
   // int indexOf (Object elem);

   /**
    * Returns true if a particular element is contained in this list.
    * 
    * @param elem
    * element to search for
    * @return true if the element is contained in this list
    */
   boolean contains (Object elem);
}
