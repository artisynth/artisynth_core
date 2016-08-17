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
public class DynamicArray<T> implements Iterable<T> {
   private int mySize;
   private T[] myBuffer;

   public DynamicArray(T[] array) {
      myBuffer = array;
      mySize = array.length;
   }

   public DynamicArray (Class<T> type) {
      this(type, 0);
   }

   public DynamicArray (Class<T> type, int size) {
      @SuppressWarnings("unchecked")
      T[] buff = (T[])Array.newInstance (type, mySize);
      myBuffer = buff;
      mySize = size;
   }

   public void trimToSize() {
      if (myBuffer.length > mySize) {
         myBuffer = Arrays.copyOf(myBuffer, mySize);
      }
   }

   public void ensureCapacity (int minCapacity) {
      if (minCapacity > myBuffer.length) {
         int newCapacity = (myBuffer.length * 3) / 2 + 1;
         if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
         }
         myBuffer = Arrays.copyOf(myBuffer, newCapacity);
      }
   }

   public int size() {
      return mySize;
   }

   public void add (T value) {
      ensureCapacity (mySize + 1);
      myBuffer[mySize++] = value;
   }

   public void addAll (Collection<T> collection) {
      ensureCapacity (mySize + collection.size());
      int idx = mySize;
      for (T value : collection) {
         myBuffer[idx++] = value;
      }
      mySize += collection.size();
   }

   public void addAll (DynamicArray<T> array) {
      ensureCapacity (mySize + array.size());
      int idx = mySize;
      for (T value : array) {
         myBuffer[idx++] = value;
      }
      mySize += array.size();
   }

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
   
   public int indexOf(T val) {
      for (int i=0; i<mySize; ++i) {
         if (elementEquals(val, myBuffer[i])) {
            return i;
         }
      }
      return -1;
   }
   
   public boolean contains(T val) {
      return indexOf(val) != -1;
   }
   
   /**
    * Provides direct access to the underlying array.  The underying array is 
    * automatically trimmed to the correct size before being returned.
    * @return the underlying array.  
    */
   public T[] getArray() {
      if (mySize != myBuffer.length) {
         trimToSize();
      }
      return myBuffer;
   }

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

   public Iterator<T> iterator() {
      return new MyIterator();
   }

}
