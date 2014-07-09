/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements a resizable array of objects. One of the intended uses is to
 * maintain a preallocated object buffer.
 */
public class DynamicArray<T> implements Iterable<T> {
   private int mySize;
   private T[] myBuffer;
   private Class<T> myType;

   public DynamicArray (Class<T> type) {
      myType = type;
      ensureCapacity (10);
   }

   public DynamicArray (Class<T> type, int size) {
      myType = type;
      ensureCapacity (size);
      setSize (size);
   }

   public void trimToSize() {
      if (myBuffer.length > mySize) {
         T[] oldBuffer = myBuffer;
         myBuffer = (T[])Array.newInstance (myType, mySize);
         System.arraycopy (oldBuffer, 0, myBuffer, 0, oldBuffer.length);
      }
   }

   public void ensureCapacity (int minCapacity) {
      if (myBuffer == null || minCapacity > myBuffer.length) {
         T[] oldBuffer = myBuffer;
         int newCapacity;
         if (oldBuffer != null) {
            newCapacity = (oldBuffer.length * 3) / 2 + 1;
         }
         else {
            newCapacity = 0;
         }
         if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
         }
         myBuffer = (T[])Array.newInstance (myType, newCapacity);
         if (oldBuffer != null) {
            System.arraycopy (oldBuffer, 0, myBuffer, 0, oldBuffer.length);
         }
      }
   }

   // private void fill (int idxStart, int idxEnd)
   // {
   // for (int i=idxStart; i<idxEnd; i++)
   // { try
   // { myBuffer[i] = myType.newInstance();
   // }
   // catch (Exception e)
   // { throw new UnsupportedOperationException (
   // "Can't instantiate objects of type " + myType);
   // }
   // }
   // }

   private T createInstance() {
      try {
         return myType.newInstance();
      }
      catch (Exception e) {
         throw new UnsupportedOperationException (
            "Can't instantiate objects of type "+myType, e);
      }
   }

   public void setSize (int size) {
      if (size > myBuffer.length) {
         ensureCapacity (size);
      }
      if (size > mySize) {
         for (int i = mySize; i < size; i++) {
            if (myBuffer[i] == null) {
               myBuffer[i] = createInstance();
            }
         }
      }
      mySize = size;
   }

   public void increaseSize (int inc) {
      setSize (mySize + inc);
   }

   public void ensureSize (int size) {
      if (mySize < size) {
         setSize (size);
      }
   }

   public int size() {
      return mySize;
   }

   public void add (T value) {
      if (value == null) {
         throw new IllegalArgumentException ("null values not permitted");
      }
      ensureCapacity (mySize + 1);
      myBuffer[mySize++] = value;
   }

   public void addAll (Collection<T> collection) {
      ensureCapacity (mySize + collection.size());
      int idx = mySize;
      for (T value : collection) {
         if (value == null) {
            throw new IllegalArgumentException ("null values not permitted");
         }
         myBuffer[idx++] = value;
      }
      mySize += collection.size();
   }

   public void addAll (DynamicArray<T> array) {
      ensureCapacity (mySize + array.size());
      int idx = mySize;
      for (T value : array) {
         if (value == null) {
            throw new IllegalArgumentException ("null values not permitted");
         }
         myBuffer[idx++] = value;
      }
      mySize += array.size();
   }

   public final T get (int idx) {
      return myBuffer[idx];
   }

   public void set (int idx, T value) {
      if (value == null) {
         throw new IllegalArgumentException ("null values not permitted");
      }
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
