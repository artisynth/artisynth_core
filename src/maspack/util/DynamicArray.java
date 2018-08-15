/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements a resizable array of objects (similar to ArrayList), but
 * provides access to the backing buffer
 */
public class DynamicArray<T> implements Iterable<T>, Clonable {
   private int mySize;
   private T[] myBuffer;

   /**
    * Creates a new dynamic array from a fixed-size array.
    * The array is copied internally.
    * 
    * @param array initial array 
    */
   public DynamicArray(T[] array) {
      myBuffer = Arrays.copyOf(array, array.length);
      mySize = array.length;
   }

   /**
    * Creates an empty dynamic array (size=0) of the given type
    * @param type class type for internal storage
    */
   public DynamicArray (Class<T> type) {
      this(type, 0);
   }

   /**
    * Creates a dynamic array of initial fixed size.  The array
    * is populated with null elements.
    * 
    * @param type class type for internal storage
    * @param size initial number of elements
    */
   public DynamicArray (Class<T> type, int size) {
      T[] buff = createArray(type, size);
      myBuffer = buff;
      mySize = size;
   }
   
   /**
    * Creates a fixed-size array of a given size
    * @param type class type for storage
    * @param size size of array
    * @return the created array
    */
   private static <S> S[] createArray(Class<S> type, int size) {
      @SuppressWarnings("unchecked")
      S[] buff = (S[])Array.newInstance (type, size);
      return buff;
   }

   /**
    * Adjusts the internal storage to match the current size of the array
    */
   public void trimToSize() {
      if (myBuffer.length > mySize) {
         myBuffer = Arrays.copyOf(myBuffer, mySize);
      }
   }

   /**
    * Ensures the dynamic array has a minimum storage capacity
    * before it will need to "grow" to fit new elements
    * 
    * @param minCapacity minimum storage capacity
    */
   public void ensureCapacity (int minCapacity) {
      if (minCapacity > myBuffer.length) {
         int newCapacity = (myBuffer.length * 3) / 2 + 1;
         if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
         }
         myBuffer = Arrays.copyOf(myBuffer, newCapacity);
      }
   }

   /**
    * Current number of elements in the dynamic array
    * @return array size
    */
   public int size() {
      return mySize;
   }
   
   /**
    * Sets the size of the dynamic array, either growing or trimming
    * elements as required
    * @param size new size of the array
    */
   public void resize(int size) {
      ensureCapacity(size);
      // clear elements larger than new size
      for (int i=size; i<mySize; ++i) {
         myBuffer[i] = null;
      }
      mySize = size;
   }

   /**
    * Adds a new item to the back of the dynamic array, growing the size by one
    * @param value new value to add to the array
    */
   public void add (T value) {
      ensureCapacity (mySize + 1);
      myBuffer[mySize++] = value;
   }

   /**
    * Adds a collection of items to the dynamic array, growing the size of the
    * array to fit all elements.
    * @param collection items to append to the array
    */
   public void addAll (Collection<T> collection) {
      ensureCapacity (mySize + collection.size());
      int idx = mySize;
      for (T value : collection) {
         myBuffer[idx++] = value;
      }
      mySize += collection.size();
   }

   /**
    * Adds a collection of items to the dynamic array, growing the size
    * of the array to fit all elements
    * @param array items to append to the array
    */
   public void addAll (DynamicArray<T> array) {
      ensureCapacity (mySize + array.size());
      int idx = mySize;
      for (T value : array) {
         myBuffer[idx++] = value;
      }
      mySize += array.size();
   }

   /**
    * Retrieves the item at the given index.  The index must be in the range [0, this.size()-1].
    * @param idx index of item to return
    * @return the item
    */
   public final T get (int idx) {
      return myBuffer[idx];
   }
   
   private boolean elementEquals(T a, T b) {
      if (a == b) {
         return true;
      } else if (a == null || b == null) {
         return false;
      }
      return a.equals(b);
   }
   
   /**
    * Searches the array to find the index of the first element that matches the
    * one provided.
    * @param val item to search for
    * @return index of item if found, or -1 if not found
    */
   public int indexOf(T val) {
      for (int i=0; i<mySize; ++i) {
         if (elementEquals(val, myBuffer[i])) {
            return i;
         }
      }
      return -1;
   }
   
   /**
    * Searches the array to find if the item already exists in the array
    * @param val item to search for
    * @return true if found, false otherwise
    */
   public boolean contains(T val) {
      return indexOf(val) != -1;
   }
   
   /**
    * Provides direct access to the underlying array.  The underlying array is 
    * automatically trimmed to the correct size before being returned.
    * @return the underlying array
    */
   public T[] getArray() {
      if (mySize != myBuffer.length) {
         trimToSize();
      }
      return myBuffer;
   }

   /**
    * Sets the value at the given index.  The index must be smaller than
    * the current size of the array.  To grow the array, use {@link #add(Object)},
    * or set the dynamic array size using {@link #resize(int)}.
    * @param idx index of item to set
    * @param value value to set
    */
   public void set (int idx, T value) {
      myBuffer[idx] = value;
   }

   private class MyIterator implements Iterator<T> {
      int myIndex = 0;

      public boolean hasNext() {
         return (myIndex < mySize);
      }

      public T next() throws NoSuchElementException {
         if (myIndex >= mySize) {
            throw new NoSuchElementException ("index " + myIndex);
         }
         return myBuffer[myIndex++];
      }

      public void remove() throws UnsupportedOperationException {
         throw new UnsupportedOperationException();
      }
   }


   @Override
   /**
    * Provides an iterator for sequentially iterating through items
    * in this dynamic array
    */
   public Iterator<T> iterator() {
      return new MyIterator();
   }
   
   @Override
   public DynamicArray<T> clone() {
      DynamicArray<T> copy = new DynamicArray<>(getArray());
      return copy;
   }

   public void clear () {
      mySize = 0;
      for (int i=0; i<myBuffer.length; ++i) {
         myBuffer[i] = null;
      }
   }

}
