/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Dynamic integer array (saves on memory vs {@link ArrayList})
 * @author Antonio
 *
 */
public class DynamicBooleanArray extends ModifiedVersionBase
   implements Cloneable {

   public static final int DEFAULT_INITIAL_CAPACITY = 10;

   boolean[] elementData;
   int size;

   /**
    * Dynamic array of integers with default capacity of 10
    */
   public DynamicBooleanArray() {
      this(DEFAULT_INITIAL_CAPACITY);
   }

   /**
    * Dynamic array of integers with provided initial capacity
    * @param initialCapacity initial capacity.  If {@code <=} 0, uses 
    * the default capacity
    */
   public DynamicBooleanArray(int initialCapacity) {
      if (initialCapacity <= 0) {
         initialCapacity = DEFAULT_INITIAL_CAPACITY;
      }
      elementData = new boolean[initialCapacity];
   }
   
   public DynamicBooleanArray(boolean... vals) {
      elementData = Arrays.copyOf (vals, vals.length);
      notifyModified ();
   }

   public void ensureCapacity(int cap) {

      int oldCap = elementData.length;
      int newCap = oldCap;

      if (newCap - cap < 0) {  // overflow aware
         newCap = oldCap + (oldCap >> 1);  // 1.5x growth
      }
      if (newCap - cap < 0) {
         newCap = cap;
      }

      if (newCap > oldCap) {
         elementData = Arrays.copyOf (elementData, newCap);
      }
   }

   public int size() {
      return size;
   }

   public void add(boolean e) {
      ensureCapacity (size+1);
      elementData[size++] = e;
      notifyModified ();
   }
   
   public void addAll(boolean[] e) {
      if (e.length > 0) {
         ensureCapacity (size+e.length);
         for (int i=0; i<e.length; ++i) {
            elementData[size++] = e[i];
         }
         notifyModified ();
      }
   }

   public void clear() {
      if (size > 0) {
         size = 0;
         notifyModified ();
      }
   }

   public boolean remove(int idx) {
      --size;
      boolean out = elementData[idx];
      for (int i=idx; i<size; ++i) {
         elementData[i] = elementData[i+1];
      }
      notifyModified ();
      return out;
   }
   
   /**
    * Remove a specified number of elements starting at the
    * provided index.
    * @param idx starting index to remove
    * @param count number of elements to remove
    */
   public void remove(int idx, int count) {
      if (count > 0) {
         size -= count;
         for (int i=idx; i<size; ++i) {
            elementData[i] = elementData[i+count];
         }
         notifyModified ();
      }
   }

   public void trimToSize() {
      if (elementData.length != size) {
         elementData = Arrays.copyOf (elementData, size);
      }
   }
   
   /**
    * Resizes the array.  If the new size is smaller than the current array size,
    * then the trailing elements are removed from the array.  If the new size is
    * greater than the current, then the array is grown, and new elements are 
    * initialized to zero.
    * @param size new size
    */
   public void resize(int size) {
      if (size < this.size) {
         this.size = size;
         notifyModified ();
      } else if (size > this.size) {
         ensureCapacity (size);
         for (int i=this.size; i<size; ++i) {
            elementData[i] = false;
         }
         notifyModified ();
      }
   }
   
   /**
    * Returns a copy of a portion of the array 
    * @param start starting index to copy
    * @param size number of elements
    * @return portion of array
    */
   public DynamicBooleanArray slice(int start, int size) {
      DynamicBooleanArray out = new DynamicBooleanArray (size);
      for (int i=0; i<size; ++i) {
         out.add (get(start+i));
      }
      return out;
   }
   
   /**
    * Performs an in-place slice, modifying this array directly
    * @param start starting index to keep
    * @param size number of elements
    */
   public void chop(int start, int size) {
      if (start > 0 || size < this.size) {
         for (int i=0; i<size; ++i) {
            set(i, get(start+i));
         }
         this.size = size;
         notifyModified ();
      }
   }

   public boolean get(int idx) {
      return elementData[idx];
   }

   /**
    * Sets the value at index <code>idx</code> to the provided <code>e</code>.
    * If <code>idx==size()</code>, then the element is appended to the array.
    * @param idx index at which to modify the value
    * @param e new value
    */
   public void set(int idx, boolean e) {
      if (idx == size) {
         add(e);
      } else {
         if (elementData[idx] != e) {
            elementData[idx] = e;
            notifyModified ();
         }
      }
   }

   /**
    * Provides direct access to the underlying array.  If the array is modified,
    * then the version numbering will be out of sync until {@link #notifyModified()}
    * is called.  The underying array is automatically trimmed to the correct size
    * before being returned.
    * @return the underlying array.  
    */
   public boolean[] getArray() {
      if (size != elementData.length) {
         trimToSize();
      }
      return elementData;
   }
      
   /**
    * Creates a shallow copy
    */
   @Override
   public DynamicBooleanArray clone ()  {
      
      DynamicBooleanArray out;
      try {
         out = (DynamicBooleanArray)(super.clone ());
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Failed to clone");
      }
      out.elementData = Arrays.copyOf (elementData, size);
      out.size = size;
      
      return out;
   }

}
