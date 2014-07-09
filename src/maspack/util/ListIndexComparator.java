/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.Comparator;
import java.util.List;

public class ListIndexComparator<E> implements Comparator<Integer> {
   
   private List<E> data;
   private Comparator<E> myComparator;
   
   public ListIndexComparator(List<E> data) {
      this.data = data;
      myComparator = null;
   }
   
   public ListIndexComparator(List<E> data, Comparator<E> comparator) {
      this.data = data;
      myComparator = comparator;
   }

   @SuppressWarnings("unchecked")
   @Override
   public int compare(Integer o1, Integer o2) {
      E data1 = data.get(o1);
      E data2 = data.get(o2);
      
      if (myComparator != null) {
         return myComparator.compare(data1, data2);
      } else if (data1 instanceof Comparable<?>) {
         return ((Comparable<E>)data1).compareTo(data2);
      }
      return 0;
   }
   
   public Integer[] createIndexArray() {
      Integer[] idxs = new Integer[data.size()];
      for (int i=0; i<data.size(); i++) {
         idxs[i] = i;
      }
      return idxs;
   }

}
