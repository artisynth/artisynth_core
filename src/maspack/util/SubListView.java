/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class SubListView<E> implements ListView<E> {
   protected ListView<E> myListView;

   protected abstract boolean isMember (Object e);

   public SubListView (ListView<E> view) {
      myListView = view;
   }

   private class SubIterator implements Iterator<E> {
      Iterator<E> mySuperit;
      E myNext;

      private E getNext() {
         while (mySuperit.hasNext()) {
            E e = mySuperit.next();
            if (isMember (e)) {
               return e;
            }
         }
         return null;
      }

      SubIterator() {
         mySuperit = myListView.iterator();
         myNext = getNext();
      }

      public boolean hasNext() {
         return myNext != null;
      }

      public E next() {
         if (myNext == null) {
            throw new NoSuchElementException();
         }
         E result = myNext;
         myNext = getNext();
         return result;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * Return an iterator over all elements in this list.
    * 
    * @return iterator over list elements
    */
   public Iterator<E> iterator() {
      return new SubIterator();
   }

   /**
    * Get the element at a particular index, or null if there is no such
    * element.
    * 
    * @param idx
    * index of the element
    * @return element at specified index
    */
   public E get (int idx) {
      int i = 0;
      for (E e : myListView) {
         if (isMember (e)) {
            if (i++ == idx) {
               return e;
            }
         }
      }
      return null;
   }

   /**
    * Get the number of elements in this list.
    * 
    * @return number of elements
    */
   public int size() {
      int count = 0;
      for (E e : myListView) {
         if (isMember (e)) {
            count++;
         }
      }
      return count;
   }

   /**
    * Returns true if a particular element is contained in this list.
    * 
    * @param elem
    * element to search for
    * @return true if the element is contained in this list
    */
   public boolean contains (Object elem) {
      return (myListView.contains (elem) && isMember (elem));
   }

}
