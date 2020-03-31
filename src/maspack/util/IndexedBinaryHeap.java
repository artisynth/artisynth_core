/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Antonio
 *
 * Implements a Balanced Binary Heap structure, much like {@link java.util.PriorityQueue}.
 * This exposes the {@code update} function which can re-order the queue if any
 * internal values are updated.  The difference between this and {@link BinaryHeap} is that
 * it works solely using indices.  This is to speed up the update procedure.
 *
 */
public class IndexedBinaryHeap {

   public static final boolean DEFAULT_MIN_HEAP = true;

   protected transient int [] heap; // actual heap
   protected transient int [] heapIndex;  // storage with data location in heap

   protected int size;
   protected boolean minHeap;
   protected int cmpSgn = 1;
   protected Comparator<Integer> comparator;
   protected transient int modCount = 0; // for detecting concurrent modification

   /**
    * Creates a minimum {@code BinaryHeap} with given data capacity/maximum data
    * array size
    */
   public IndexedBinaryHeap (int dataSize, Comparator<Integer> indexedComparator) {
      this(dataSize, indexedComparator, DEFAULT_MIN_HEAP);
   }

   /**
    * Creates a {@code BinaryHeap} according to the supplied parameters
    * @param dataSize the size of the indexed array, [0, dataSize-1]
    * @param indexedComparator {@code Comparator} to use to compare elements 
    * @param min if true, creates a min-heap, otherwise creates a max-heap
    */
   public IndexedBinaryHeap (int dataSize, Comparator<Integer> indexedComparator, boolean min) {

      size = 0;
      heap = new int[dataSize];
      heapIndex = new int[dataSize];
      Arrays.fill(heap, -1);
      Arrays.fill(heapIndex, -1);
      comparator = indexedComparator;
      setMinHeap(min);
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
   public Comparator<Integer> comparator() {
      return comparator;
   }

   /**
    * Number of elements in the heap
    */
   public int size() {
      return size;
   }

   /**
    * Returns whether or not the heap is empty
    */
   public boolean isEmpty() {
      return (size == 0);
   }

  
   public int[] toArray() {
      int[] out = new int[size];
      for (int i=0; i<size; i++) {
         out[i] = heap[i];
      }
      return out;
   }

   @SuppressWarnings("unchecked")
   public <T, E> T[] toArray(T[] a, E[] data) {

      T[] r = a;
      if (a.length < size) {
         r = allocate(a, size);
      }
      for (int i=0; i<size; i++) {
         r[i] = (T)data[heap[i]];
      }

      return r;
   }

   /**
    * Inserts data element idx into the heap
    * @param idx index of the internal data array to add to the heap
    * @return true if added, false if already in heap
    */
   public boolean add(int idx) {      
      if (heapIndex[idx] >= 0) {
         return false;  // already in heap
      }

      heap[size] = idx;
      heapIndex[idx] = size;
      bubbleUp(size);
      size++;

      return true;
   }

   /**
    * Looks at the index at the top of the heap, without removing it
    */
   public int peek() {
      if (size == 0) {
         return -1;
      }
      return heap[0];
   }
   
   /**
    * Retrieves and removes the index of the top element in the heap
    * @return retrieved top element index
    */
   public int poll() {
      modCount++;
      if (size == 0) {
         return -1;
      }
      int outIdx = heap[0];

      size--;
      heapIndex[heap[0]] = -1;   // removing from heap
      heapIndex[heap[size]] = 0; // moving to zero
      heap[0] = heap[size];
      heap[size] = -1;

      if (size > 0) {
         bubbleDown(0);
      }
      return outIdx;
   }

   /**
    * Removes data element at index idx from the heap
    * @param idx index of the element to remove
    * @return true if element was in heap and removed, 
    *  false if not in heap
    */

   public boolean remove(int idx) {
      int hidx = heapIndex[idx];
      if (hidx < 0) {
         return false;
      }
      removeAt(hidx);
      return true;
   }

   /**
    * Finds the largest (or smallest) element in the heap for a
    * min-(or max-) heap.  Returns the entry's index
    */
   public int peekLast() {
      int idx = findLargest();
      if (idx >= 0) {
         return heap[idx];
      }
      return -1;
   }

   /**
    * Finds and removes largest (or smallest) element in the heap for a
    * min-(or max-) heap. Returns the entry's index
    */
   public int pollLast() {
      int idx = findLargest();
      if (idx >= 0) {
         removeAt(idx);
         return heap[idx];
      }
      return -1;
   }

   protected int findLargest() {
      if (size == 0) {
         return -1;
      }

      int largestIdx = size/2;
      for (int leafIdx=largestIdx+1; leafIdx<size; leafIdx++ ) {
         int c = comparator.compare(heap[leafIdx], heap[largestIdx]);
         if (c > 0) {
            largestIdx = leafIdx;
         }
      }
      return largestIdx;
   }

   /**
    * Removes an element at a specified index.
    * @param idx
    * @return index of the removed element, or -1 element not found
    */
   protected int removeAt(int idx) {

      if (idx < 0 || idx >= size) {
         throw new IndexOutOfBoundsException();
      } else if (idx == 0) {
         poll();
         return -1;
      }

      size--;
      heapIndex[heap[idx]] = -1;
      heapIndex[heap[size]] = idx;
      heap[idx] = heap[size];
      heap[size] = -1;

      int moved = heap[size];

      modCount++;

      if (idx == size) {
         return -1;
      }

      if (size > 0 && idx < size) {
         int compareToParent = 0;
         if (idx > 0) {
            compareToParent = cmpSgn*comparator.compare(heap[idx], heap[(idx-1)/2]);
            if (idx > 0 && compareToParent < 0) {
               bubbleUp(idx);
               // if an object has been moved up, return it
               if (heap[idx] != moved) {
                  return moved;
               }
            } else {
               bubbleDown(idx);
            }
         }
      }

      return -1;
   }
   
   public void clear() {
      size = 0;
      modCount++;
      Arrays.fill(heap, -1);
      Arrays.fill(heapIndex, -1);
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
   
   protected void bubbleDown(int idx) {

      int objIndex = heap[idx];
      int pos = idx;

      int child;
      while ((child = (pos * 2 + 1)) < size) {

         // potentially move to next child
         if (child < (size-1)
            && cmpSgn
            * comparator.compare(heap[child + 1], heap[child]) < 0) {
            child++;
         }

         // potentially terminate
         if (cmpSgn * comparator.compare(heap[child], objIndex) >= 0) {
            break;
         }

         // otherwise fill pos and continue
         heapIndex[heap[child]] = pos;
         heap[pos] = heap[child];
         pos = child;
        
      }
      heapIndex[objIndex] = pos;
      heap[pos] = objIndex;

   }

   protected void bubbleUp(int idx) {
      int pos = idx;
      int objIndex = heap[pos];

      int parent = (pos - 1) / 2;
      while (pos > 0
         && cmpSgn * comparator.compare(objIndex, heap[parent]) < 0) {
         
         heapIndex[heap[parent]] = pos;
         heap[pos] = heap[parent];
         pos = parent;
         parent = (pos - 1) / 2;
      }
      heapIndex[objIndex] = pos;
      heap[pos] = objIndex;
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
    * Initializes a heap from an array of data indices
    */
   public void set(int[] valIdxs) {
      
      clear();
      for (int i=0; i<valIdxs.length; i++) {
         if (valIdxs[i] < 0 || valIdxs[i] >= heapIndex.length) {
            throw new IndexOutOfBoundsException("Index at " + i + " not within a valid range");
         }
         heap[i] = valIdxs[i];
         heapIndex[valIdxs[i]] = i;
      }
      size = valIdxs.length;
      update();
   }
   
   /**
    * Initializes a heap, adding all entries
    */
   public void setAll() {
      clear();
      for (int i=0; i<heapIndex.length; i++) {
         heap[i] = i;
         heapIndex[i] = i;
      }
      size = heapIndex.length;
      update();
   }

   /**
    * Adjusts the list starting at entry number idx
    */
   public void update(int idx) {
      int hidx = heapIndex[idx];
      if (hidx >= 0) {
         doUpdate(hidx);
      }
   }
   
   private void doUpdate(int hidx) {
      if (hidx > 0) {
         // check if we need to bubble up or down
         int compareToParent = cmpSgn*comparator.compare(heap[hidx], heap[(hidx-1)/2]);
         if (compareToParent < 0) {
            bubbleUp(hidx);
         } else {
            bubbleDown(hidx);
         }
      } else {
         bubbleDown(hidx);
      }
   }

}
