/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Comparator;

public class ArrayIndexComparator<E> implements Comparator<Integer> {
   
   private E[] data;
   private Comparator<E> myComparator;
   
   public ArrayIndexComparator(E[] data) {
      this.data = data;
      myComparator = null;
   }
   
   public ArrayIndexComparator(E[] data, Comparator<E> comparator) {
      this.data = data;
      myComparator = comparator;
   }

   @SuppressWarnings("unchecked")
   @Override
   public int compare(Integer o1, Integer o2) {
      E data1 = data[o1];
      E data2 = data[o2];
      
      if (myComparator != null) {
         return myComparator.compare(data1, data2);
      } else if (data1 instanceof Comparable<?>) {
         return ((Comparable<E>)data1).compareTo(data2);
      }
      return 0;
   }
   
   public Integer[] createIndexArray() {
      Integer[] idxs = new Integer[data.length];
      for (int i=0; i<data.length; i++) {
         idxs[i] = i;
      }
      return idxs;
   }

}
