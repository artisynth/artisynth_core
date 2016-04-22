package maspack.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Dynamic integer array (saves on memory vs {@link ArrayList})
 * @author Antonio
 *
 */
public class DynamicIntArray extends ModifiedVersionBase implements Cloneable {

   public static final int DEFAULT_INITIAL_CAPACITY = 10;

   int[] elementData;
   int size;


   public DynamicIntArray() {
      this(DEFAULT_INITIAL_CAPACITY);
   }

   public DynamicIntArray(int initialCapacity) {
      elementData = new int[initialCapacity];
   }
   
   public DynamicIntArray(int... vals) {
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

   public void add(int e) {
      ensureCapacity (size+1);
      elementData[size++] = e;
      notifyModified ();
   }
   
   public void addAll(int[] e) {
      ensureCapacity (size+e.length);
      for (int i=0; i<e.length; ++i) {
         elementData[size++] = e[i];
      }
      notifyModified ();
   }

   public void clear() {
      size = 0;
      notifyModified ();
   }

   public int remove(int idx) {
      --size;
      int out = elementData[idx];
      for (int i=idx; i<size; ++i) {
         elementData[i] = elementData[i+1];
      }
      notifyModified ();
      return out;
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
      notifyModified ();
   }

   /**
    * Provides direct access to the underlying array.  If the array is modified,
    * then the version numbering will be out of sync until {@link #notifyModified()}
    * is called.
    * @return the underlying array.  
    */
   public int[] getArray() {
      return elementData;
   }
   
   /**
    * Creates a shallow copy
    */
   @Override
   public DynamicIntArray clone ()  {
      
      DynamicIntArray out;
      try {
         out = (DynamicIntArray)(super.clone ());
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Failed to clone");
      }
      out.elementData = Arrays.copyOf (elementData, size);
      out.size = size;
      
      return out;
   }

}
