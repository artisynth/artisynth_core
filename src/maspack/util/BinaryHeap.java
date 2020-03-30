/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Antonio
 *
 * Implements a Balanced Binary Heap structure, much like {@link java.util.PriorityQueue}.
 * This exposes the {@code update} function which can re-order the queue if any
 * internal values are updated.
 *
 * @param <E> generic class with which the heap is associated
 */
public class BinaryHeap<E> implements Collection<E> {

   public static final int DEFAULT_CAPACITY = 32;
   public static final boolean DEFAULT_MIN_HEAP = true;
   
   public static class DefaultComparator<E> implements Comparator<E> {
      @SuppressWarnings("unchecked")
      @Override
      public int compare(E o1, E o2) {
         if (o1 instanceof Comparable) {
            return ((Comparable<? super E>)o1).compareTo(o2);
         }
         return 0;
      }
   }

   protected transient Object[] objs;
   protected int size;
   protected boolean minHeap;
   protected int cmpSgn = 1;
   protected Comparator<E> comparator;
   protected transient int modCount = 0; // for detecting concurrent modification

   /**
    * Creates a minimum {@code BinaryHeap} with with default initial capacity
    * and natural ordering comparator
    */
   public BinaryHeap () {
      this(DEFAULT_CAPACITY, null, DEFAULT_MIN_HEAP);
   }

   /**
    * Creates a minimum {@code BinaryHeap} with with supplied initial capacity
    * and natural ordering
    * @param capacity initial capacity
    */
   public BinaryHeap (int capacity) {
      this(capacity, null, DEFAULT_MIN_HEAP);
   }
   
   /**
    * Creates a minimum {@code BinaryHeap} that uses the supplied comparator
    * to order values
    */
   public BinaryHeap(Comparator<E> c) {
      this(DEFAULT_CAPACITY, c, DEFAULT_MIN_HEAP);
   }
   
   /**
    * Creates a {@code BinaryHeap} according to the supplied parameters
    * @param capacity initial capacity
    * @param c {@code Comparator} to use to compare elements 
    * @param min if true, creates a min-heap, otherwise creates a max-heap
    */
   public BinaryHeap (int capacity, Comparator<E> c, boolean min) {

      size = 0;

      if (capacity <= 0) {
         capacity = DEFAULT_CAPACITY;
      }
      objs = new Object[capacity];

      if (c == null) {
         c = new DefaultComparator<E>();
      }
      comparator = c;
      setMinHeap(min);
   }
   
   @SuppressWarnings("unchecked")
   protected E elementData(int idx) {
      return (E)objs[idx];
   }
   
   protected void setMinHeap(boolean min) {
      this.minHeap = min;
      if (min) {
         cmpSgn = 1;
      } else {
         cmpSgn = -1;
      }
   }

   /**
    * Returns the comparator used to order elements
    */
   public Comparator<E> comparator() {
      return comparator;
   }

   @Override
   /**
    * Number of elements in the heap
    */
   public int size() {
      return size;
   }

   @Override
   /**
    * Returns whether or not the heap is empty
    */
   public boolean isEmpty() {
      return (size == 0);
   }

   @Override
   /**
    * Returns whether or not the heap contains an object
    */
   public boolean contains(Object o) {
      for (int i = 0; i < size; i++) {
         if (objs[i] == o) {
            return true;
         }
      }
      return false;
   }

   /**
    * Iterator class
    */
   protected final class Itr implements Iterator<E> {
      
      // index to return next
      private int idx = 0;

      // last index returned
      private int lastRet = -1;

      // elements that may have been bypassed by element removals
      private ArrayDeque<E> skipped = null;

      // last element returned from the skipped list
      private E lastRetSkipped = null;
      
      // for detecting concurrent modification
      private int expectedModCount = modCount;

      public boolean hasNext() {
          return idx < size ||
              (skipped != null && !skipped.isEmpty());
      }

      public E next() {
          if (expectedModCount != modCount)
              throw new ConcurrentModificationException();
          if (idx < size)
              return elementData(lastRet = idx++);
          if (skipped != null) {
              lastRet = -1;
              lastRetSkipped = skipped.poll();
              if (lastRetSkipped != null)
                  return lastRetSkipped;
          }
          throw new NoSuchElementException();
      }

      public void remove() {
          if (expectedModCount != modCount)
              throw new ConcurrentModificationException();
          if (lastRet != -1) {
              E moved = BinaryHeap.this.removeAt(lastRet);
              lastRet = -1;
              if (moved == null)
                  idx--;
              else {
                  if (skipped == null)
                      skipped = new ArrayDeque<E>();
                  skipped.add(moved);
              }
          } else if (lastRetSkipped != null) {
              BinaryHeap.this.remove(lastRetSkipped);
              lastRetSkipped = null;
          } else {
              throw new IllegalStateException();
          }
          expectedModCount = modCount;
      }
   }
   
   @Override
   public Iterator<E> iterator() {
      return new Itr();
   }

   @Override
   public Object[] toArray() {
      return Arrays.copyOf(objs, size);
   }

   @Override
   public <T> T[] toArray(T[] a) {

      T[] r = a;
      if (a.length < size) {
         r = allocate(a, size);
      }
      System.arraycopy(objs, 0, r, 0, size);

      return r;
   }

   /**
    * Inserts an element into the Binary Heap
    * @return true if the element is added (always)
    */
   public boolean offer(E e) {
      return add(e);
   }
   
   /**
    * Inserts an element into the Binary Heap
    * @return true if the element is added (always)
    */
   @Override
   public boolean add(E e) {

      if (e == null) {
         throw new NullPointerException("Cannot add an instance of null");
      }
      modCount++;
      
      if (size == objs.length) {
         ensureCapacity(2 * size);
      }

      objs[size++] = e;
      bubbleUp(size - 1);
      return true;
   }

   /**
    * Looks at the top element in the heap, without removing it
    */
   public E peek() {
      if (size == 0) {
         return null;
      }
      return elementData(0);
   }

   /**
    * Retrieves and removes the top element in the heap
    */
   public E poll() {
      modCount++;
      if (size == 0) {
         return null;
      }
      E out = elementData(0);

      size--;
      objs[0] = objs[size];
      objs[size] = null;
      
      if (size > 0) {
         bubbleDown(0);
      }

      return out;
   }

   @Override
   public boolean remove(Object o) {
      
      int idx = indexOf(o);
      if (idx < 0) {
         return false;
      }
      removeAt(idx);
      
      return true;
   }
   
   // finds the last element of the heap
   public E peekLast() {
      int idx = findLargest();
      if (idx >= 0) {
         return elementData(idx);
      }
      return null;
   }
   
   public E pollLast() {
      int idx = findLargest();
      if (idx >= 0) {
         E largest = elementData(idx);
         removeAt(idx);
         return largest;
      }
      return null;
   }
   
   protected int findLargest() {
      if (size == 0) {
         return -1;
      }
      
      // first leaf index is floor(size/2)
      int largestIdx = size/2;
      for (int leafIdx=largestIdx+1; leafIdx<size; leafIdx++ ) {
         int c = comparator.compare(elementData(leafIdx), elementData(largestIdx));
         if (c > 0) {
            largestIdx = leafIdx;
         }
      }
      return largestIdx;
   }
   
   /**
    * Removes an element at a specified index.  Not that this
    * @param idx
    * @return element at the index
    */
   protected E removeAt(int idx) {
      
      if (idx < 0 || idx >= size) {
         throw new IndexOutOfBoundsException();
      } else if (idx == 0) {
         poll();
         return null;
      }
      
      size--;
      objs[idx] = objs[size];
      E moved = elementData(size);
      objs[size] = null;
      
      modCount++;
      
      if (idx == size) {
         return null;
      }
      
      if (size > 0 && idx < size) {
         int compareToParent = 0;
         if (idx > 0) {
            compareToParent = cmpSgn*comparator.compare(elementData(idx), elementData((idx-1)/2));
            if (idx > 0 && compareToParent < 0) {
               bubbleUp(idx);
               // if an object has been moved up, return it
               if (objs[idx] != moved) {
                  return moved;
               }
            } else {
               bubbleDown(idx);
            }
         }
      }
      
      return null;
   }

   protected int indexOf(Object o) {
      for (int i = 0; i < size; i++) {
         if (objs[i] == o) {
            return i;
         }
      }
      return -1;
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      for (Object o : c) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      for (E e : c) {
         add(e);
      }
      return true;
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      boolean changed = false;
      Iterator<?> it = c.iterator();
      while (it.hasNext()) {
         changed |= remove(it.next());
      }
      return changed;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      Iterator<E> it = iterator();
      boolean changed = false;
      while (it.hasNext()) {
         E e = it.next();
         if (!c.contains(e)) {
            it.remove();
            changed = true;
         }
      }
      return changed;
   }

   @Override
   public void clear() {
      size = 0;
      modCount++;
      Arrays.fill(objs, null);
   }

   /**
    * Returns true if this is a min-heap
    * @return true if this is a min-heap
    */
   public boolean isMinHeap() {
      return minHeap;
   }

   /**
    * Returns true if this is a max-heap
    */
   public boolean isMaxHeap() {
      return !minHeap;
   }

   protected <T> T[] allocate(T[] a, int size) {
      @SuppressWarnings("unchecked")
      T[] r = (T[])java.lang.reflect.Array
      .newInstance(a.getClass().getComponentType(), size);
      return r;
   }

   /**
    * Ensures the heap's capacity is at least the supplied value
    */
   public void ensureCapacity(int capacity) {
      if (objs.length < capacity) {
         objs = Arrays.copyOf(objs, capacity);
      }
   }

   protected void bubbleDown(int idx) {

      E obj = elementData(idx);
      int pos = idx;

      int child;
      while ((child = (pos * 2 + 1)) < size) {

         // potentially move to next child
         if (child < (size-1)
            && cmpSgn
            * comparator.compare(elementData(child + 1), elementData(child)) < 0) {
            child++;
         }

         // potentially terminate
         if (cmpSgn * comparator.compare(elementData(child), obj) >= 0) {
            break;
         }

         // otherwise fill pos and continue
         objs[pos] = objs[child];
         pos = child;
      }

      objs[pos] = obj;

   }

   protected void bubbleUp(int idx) {
      int pos = idx;
      E obj = elementData(pos);

      int parent = (pos - 1) / 2;
      while (pos > 0
         && cmpSgn * comparator.compare(obj, elementData(parent)) < 0) {
         objs[pos] = objs[parent];
         pos = parent;
         parent = (pos - 1) / 2;
      }
      objs[pos] = obj;
   }

   /**
    * Re-orders the heap.  This should be called if any of the heap's priorities
    * have changed.
    */
   public void update() {
      for (int i = (size - 1) / 2; i >= 0; i--) {
         bubbleDown(i);
      }
   }
   
   /**
    * Initializes a heap from an array
    */
   public void set(E[] vals) {
      ensureCapacity(vals.length);
      System.arraycopy(vals, 0, objs, 0, vals.length);
      size = vals.length;
      update();
   }
   
   public void set(Collection<E> vals) {
      ensureCapacity(vals.size());
      Object[] valArray = vals.toArray();
      System.arraycopy(valArray, 0, objs, 0, vals.size());
      size = vals.size();
      update();
   }

   public void update(E val) {
      int idx = indexOf(val);
      if (idx > 0) {
         // check if we need to bubble up or down
         int compareToParent = cmpSgn*comparator.compare(elementData(idx), elementData((idx-1)/2));
         if (compareToParent < 0) {
            bubbleUp(idx);
         } else {
            bubbleDown(idx);
         }
      } else {
         bubbleDown(idx);
      }
   }

}
