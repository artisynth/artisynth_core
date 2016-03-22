package maspack.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Dynamic integer array (saves on memory vs {@link ArrayList})
 * @author Antonio
 *
 */
public class DynamicIntArray implements Iterable<Integer> {

   public static final int DEFAULT_INITIAL_CAPACITY = 10;

   int[] elementData;
   int size;


   public DynamicIntArray() {
      this(DEFAULT_INITIAL_CAPACITY);
   }

   public DynamicIntArray(int initialCapacity) {
      elementData = new int[initialCapacity];
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

   public void add(int e) {
      ensureCapacity (size+1);
      elementData[size++] = e;
   }
   
   public void addAll(int[] e) {
      ensureCapacity (size+e.length);
      for (int i=0; i<e.length; ++i) {
         elementData[size++] = e[i];
      }
   }

   public void clear() {
      size = 0;
   }

   public void remove(int idx) {
      --size;
      for (int i=idx; i<size; ++i) {
         elementData[i] = elementData[i+1];
      }
   }

   public void trimToSize() {
      if (elementData.length != size) {
         elementData = Arrays.copyOf (elementData, size);
      }
   }

   public int get(int idx) {
      return elementData[idx];
   }

   public void set(int idx, int e) {
      elementData[idx] = e;
   }

   public int[] getData() {
      return elementData;
   }

   @Override
   public Iterator<Integer> iterator () {

      return new Iterator<Integer>() {

         int cursor = -1;
         boolean valid = false;

         @Override
         public boolean hasNext () {
            if (cursor < size-1) {
               return true;
            }
            return false;
         }

         @Override
         public Integer next () {
            if (cursor == size-1) {
               throw new NoSuchElementException();
            }
            cursor++;
            valid = true;
            return elementData[cursor];
         }

         @Override
         public void remove () {

            if (!valid) {
               throw new IllegalStateException();
            }

            --size;
            for (int i=cursor; i<size; ++i) {
               elementData[i] = elementData[i+1];
            }
            --cursor;
            valid = false;

         }
      };
   }
   
   /**
    * Creates a shallow copy
    */
   @Override
   public DynamicIntArray clone ()  {
      DynamicIntArray out = new DynamicIntArray ();
      out.elementData = Arrays.copyOf (elementData, size);
      out.size = size;
      
      return out;
   }

}
