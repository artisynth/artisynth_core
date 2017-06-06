/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Set of renderables sorted in ascending order by zOrder.  
 * This is very similar to what would be a {@code SortedSet<IsRenderable>}, 
 * except I do no checks to ensure elements are 
 * distinct according to compareTo(...) with respect to zOrder. In fact, most 
 * elements will have the same zOrder=0, in which case they are sorted 
 * according to the original order they were added to the list (stable sort).
 * Unlike {@code List<IsRenderable>}, order is not guaranteed.
 * 
 * <p>
 * The zOrder is read from render properties if the IsRenderable is an instance
 * of HasRenderProps.  Otherwise, a zOrder can be specified in 
 * {@link #add(IsRenderable,int)}, or is assumed to be 0.
 * 
 * @author Antonio
 *
 */
public class SortedRenderableList implements Collection<IsRenderable> {

   public static int defaultCapacity = 10;
   public static int defaultIncrement = 5;

   int modCount = 0;    // for concurrent modification detection
   int size = 0;
   int myIncrement = defaultIncrement;
   IsRenderable[] myArray;
   int[] myZOrderKeys;
   int myMaxSelectionQueries = 0;

   /**
    * Constructs an empty list with the specified initial capacity.
    *
    * @param  initialCapacity  the initial capacity of the list
    */
   public SortedRenderableList(int initialCapacity) {
      super();
      if (initialCapacity < 0) {
         initialCapacity = defaultCapacity;
      }

      this.myArray = new IsRenderable[initialCapacity];
      this.myZOrderKeys = new int[initialCapacity];
   }

   /**
    * Constructs an empty list with an initial capacity of ten.
    */
   public SortedRenderableList() {
      this(defaultCapacity);
   }

   private int getMaxSelectionQueries (IsRenderable r) {
      if (r instanceof IsSelectable) {
         IsSelectable s = (IsSelectable)r;
         return Math.max (1, s.numSelectionQueriesNeeded());
      }
      else {
         return 0;
      }
   }
   
   protected void printSelectionQueriesNeeded() {
      for (IsRenderable r : this) {
         if (r instanceof IsSelectable) {
            IsSelectable s = (IsSelectable)r;
            System.out.println (r.getClass().getName() + 
              " " + Math.max (1, s.numSelectionQueriesNeeded()));
         }       
      }
   }

   /**
    * Returns the maximum number of selection queries required for all
    * renderables in this list. This number is needed by viewer-based selection
    * software.
    */
   public int numSelectionQueriesNeeded() {
      return myMaxSelectionQueries;
   }

   /**
    * Constructs a set containing the elements of the specified
    * collection.
    *
    * @param c the collection whose elements are to be placed into this list
    * @throws NullPointerException if the specified collection is null
    */
   public SortedRenderableList(Collection<? extends IsRenderable> c) {

      myArray = new IsRenderable[c.size()];
      myZOrderKeys = new int[c.size()];

      c.toArray(myArray);
      for (int i=0; i<myArray.length; i++) {
         myZOrderKeys[i] = getZOrderKey(myArray[i]);
      }

      insertionSort(myArray, myZOrderKeys);
   }
   
   /**
    * Most of the time, we will insert at end of list, so start from back
    * if key is >= 0.  If key < 0, start search from front.
    * For our particular application, this should be faster than a binary
    * search.
    * @param val key to insert
    * @param keys list of keys
    * @param size size of array
    * @return the index to insert element
    */
   private static int findInsertionPoint(int val, int[] keys, int size) {
      if (val >= 0) {
         for (int i = size-1; i >= 0; i--) {
            if (keys[i] <= val) {
               return i+1;
            }
         }
         return 0;
      } else {
         for (int i=0; i < size; i++) {
            if (keys[i] > val) {
               return i;
            }
         }
         return size;
      }
   }
   
   /**
    * Search for object.  if key <= 0, start from beginning
    */
   private int findIndexOf(IsRenderable val, int key) {
      if (key <=0 ) {
         for (int i=0; i<size; i++) {
            if (myArray[i] == val) {
               return i;
            }
         }
      } else {
         for (int i=size-1; i>=0; i--) {
            if (myArray[i] == val) {
               return i;
            }
         }
      }
      return -1;
   }
   
   /**
    * Inserts an element at specified position
    */
   private void insert(IsRenderable val, int key, int pos, 
      IsRenderable[] vals, int keys[], int back) {
      
      // shift everything up
      for (int j=back; j>=pos+1; j--) {
         keys[j] = keys[j-1];
         vals[j] = vals[j-1];
      }
      keys[pos] = key;
      vals[pos] = val;
      myMaxSelectionQueries += getMaxSelectionQueries (val);
   }
   
   private void insertionSort (IsRenderable[] data, int keys[])  {
      int size = data.length;
      for (int i = 1; i < size; i++) {
         int pos = findInsertionPoint(keys[i], keys, i);
         insert(data[i], keys[i], pos, data, keys, i);
      }
   }

   private static int getZOrderKey(IsRenderable r) {
      if (r instanceof HasRenderProps) {
         return ((HasRenderProps)r).getRenderProps().getZOrder();
      } else {
         return 0;
      }
   }

   /**
    * Sets the size to grow the array when we read capacity
    */
   public void setIncrement(int inc) {
      if (inc > 0) {
         myIncrement = inc;
      } else {
         myIncrement = defaultIncrement;
      }
   }

   /**
    * Increases the capacity to ensure that it can hold at least the
    * number of elements specified by the minimum capacity argument.
    *
    * @param minCapacity the desired minimum capacity
    */
   private void grow(int minCapacity) {
      // overflow-conscious code
      int oldCapacity = myArray.length;
      int newCapacity = oldCapacity + myIncrement;
      if (newCapacity < minCapacity)
         newCapacity = minCapacity;
      myArray = Arrays.copyOf(myArray, newCapacity);
      myZOrderKeys = Arrays.copyOf(myZOrderKeys, newCapacity);
   }
   
   /**
    * Ensure this has can support at least minCapacity elements
    */
   private void ensureCapacity(int minCapacity) {
      if (myArray.length < minCapacity) {
         grow(minCapacity);
      }
   }

   /**
    * Returns the number of elements in the set
    */
   public int size() {
      return size;
   }

   /**
    * Returns true if set is empty
    */
   public boolean isEmpty() {
      return size == 0;
   }

   private int findIndexOf(Object o) {
      if (!(o instanceof IsRenderable)) {
         return -1;
      }
      IsRenderable glr = (IsRenderable)o;
      return findIndexOf(glr, getZOrderKey(glr));
   }
   
   /**
    * Determines whether the set contains a specified object
    */
   public boolean contains(Object o) {
      if (!(o instanceof IsRenderable)) {
         return false;
      }
      IsRenderable glr = (IsRenderable)o;
      int idx = findIndexOf(glr, getZOrderKey(glr));
      if (idx >=0) {
         return true;
      }
      return false;
   }

   /**
    * Basic iterator
    */
   private class Itr implements Iterator<IsRenderable> {
       int cursor;       // index of next element to return
       int lastRet = -1; // index of last element returned; -1 if no such
       int expectedModCount = modCount;

       public boolean hasNext() {
           return cursor != size;
       }

       public IsRenderable next() {
           checkForComodification();
           int i = cursor;
           if (i >= size)
               throw new NoSuchElementException();
           if (i >= myArray.length)
               throw new ConcurrentModificationException();
           cursor = i + 1;
           lastRet = i;
           return myArray[i];
       }

       public void remove() {
           if (lastRet < 0)
               throw new IllegalStateException();
           checkForComodification();

           try {
               SortedRenderableList.this.remove(lastRet);
               cursor = lastRet;
               lastRet = -1;
               expectedModCount = modCount;
           } catch (IndexOutOfBoundsException ex) {
               throw new ConcurrentModificationException();
           }
       }

       final void checkForComodification() {
           if (modCount != expectedModCount)
               throw new ConcurrentModificationException();
       }
   }
   
   /**
    * Returns an iterator for looping through elements
    */
   public Iterator<IsRenderable> iterator() {
     return new Itr();
   }

   /**
    * Returns a copy of the IsRenderable array
    */
   public IsRenderable[] toArray() {
      return Arrays.copyOf(myArray, size);
   }

   
   /**
    * Fills an array with the current sorted IsRenderables.  A new array
    * is created if '{@code a}' is too small.
    */
   @SuppressWarnings("unchecked")
   public <T> T[] toArray(T[] a) {
      if (a.length < size)
         return (T[]) Arrays.copyOf(myArray, size, a.getClass());
     System.arraycopy(myArray, 0, a, 0, size);
     if (a.length > size)
         a[size] = null;
     return a;
   }

   /**
    * Adds an element to this set, sorted ascending by the zOrder parameter
    * if exists (otherwise assumed zOrder = 0).  For equal zOrders, the 
    * original order is maintained (stable sort).
    */
   public boolean add(IsRenderable e) {
      int key = getZOrderKey(e);
      return add(e, key);
   }
   
   /**
    * Adds an element to this set, sorted ascending by the zOrder parameter
    * if exists.  For equal zOrders, the original order is maintained 
    * (stable sort).
    */
   public boolean add(IsRenderable e, int zOrder) {
      size++;
      ensureCapacity(size);
      int pos = findInsertionPoint(zOrder, myZOrderKeys, size-1);
      insert(e, zOrder, pos, myArray, myZOrderKeys, size-1);
      return true;
   }

   /**
    * Removes an element from this set
    */
   public boolean remove(Object o) {
      if (!(o instanceof IsRenderable)) {
         return false;
      }
      IsRenderable glr = (IsRenderable)o;
      int key = getZOrderKey(glr);
      int pos = findIndexOf(glr, key);
      if (pos >=0) {
         remove(pos);
         return true;
      }
      return false;
   }
   
   public IsRenderable remove(int idx) {
      
      IsRenderable out = myArray[idx];
      for (int i=idx; i<size; i++) {
         myArray[i]  = myArray[i+1];
         myZOrderKeys[i] = myZOrderKeys[i];
      }
      size--;
      myArray[size] = null;
      myZOrderKeys[size] = 0;
      myMaxSelectionQueries -= getMaxSelectionQueries (out);
      return out;
   }

   /**
    * Returns true if this set contains all elements in the 
    * supplied collection
    */
   public boolean containsAll(Collection<?> c) {
      for (Object o : c) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   /**
    * Adds all elements in {@code c} to this set.  Always returns true;
    */
   public boolean addAll(Collection<? extends IsRenderable> c) {
      ensureCapacity(size+c.size());
      for (IsRenderable glr : c) {
         add(glr);
      }
      return true;
   }

   /**
    * Retains only the elements that are also found in the collection
    * {@code c}, essentially performing an intersection.  Returns true
    * if the collection is modified
    */
   public boolean retainAll(Collection<?> c) {
      boolean changed = false;
      for (int i=0; i<size; i++) {
         if (!c.contains(myArray[i])) {
            remove(i);
            changed = true;
            i--;
            size--;
         }
      }
      return changed;
   }

   /**
    * Removes all elements in this set that are found in 
    * {@code c}.  Returns true if the set is modified.
    */
   public boolean removeAll(Collection<?> c) {
      boolean changed = false;
      for (Object o : c) {
         int idx = findIndexOf(o);
         if (idx >= 0) {
            remove(idx);
            changed = true;
         }
      }
      return changed;
   }

   /**
    * Clears all elements from this set
    */
   public void clear() {
      size = 0;
      Arrays.fill(myArray, null);
      Arrays.fill(myZOrderKeys, 0);
      myMaxSelectionQueries = 0;
   }

   /**
    * Gets the first element in the set
    */
   public IsRenderable first() {
     return myArray[0];
   }

   /**
    * Gets the last element in the set
    */
   public IsRenderable last() {
      return myArray[size-1];
   }

   public IsRenderable get(int index) {
      return myArray[index];
   }

}
