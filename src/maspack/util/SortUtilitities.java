package maspack.util;

import java.util.AbstractList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortUtilitities {

   private static class IndexWrapper extends AbstractList<Integer> {
      int[] idxs;
      int start;
      int count;
      
      public IndexWrapper(int[] idxs, int start, int count) {
         this.idxs = idxs;
         this.start = start;
         this.count = count;
      }
      
      @Override
      public Integer set (int index, Integer element) {
         int idx = index+start;
         Integer old = idxs[idx];
         idxs[idx] = element;
         return old;
      }

      @Override
      public Integer get (int index) {
         return idxs[index+start];
      }

      @Override
      public int size () {
         return count;
      }
   }

   private static class ArrayIndexComparator<E> implements Comparator<Integer> {

      private E[] data;
      private Comparator<E> myComparator;

      public ArrayIndexComparator(E[] data, Comparator<E> comparator) {
         this.data = data;
         myComparator = comparator;
      }

      @Override
      public int compare(Integer o1, Integer o2) {
         E data1 = data[o1];
         E data2 = data[o2];

         if (myComparator != null) {
            return myComparator.compare(data1, data2);
         } else if (data1 instanceof Comparable<?>) {
            @SuppressWarnings("unchecked")
            Comparable<E> e1 = (Comparable<E>)data1;
            return e1.compareTo(data2);
         }
         return 0;
      }
   }


   private static class ListIndexComparator<E> implements Comparator<Integer> {

      private List<E> data;
      private Comparator<E> myComparator;

      public ListIndexComparator(List<E> data, Comparator<E> comparator) {
         this.data = data;
         myComparator = comparator;
      }

      @Override
      public int compare(Integer o1, Integer o2) {
         E data1 = data.get (o1);
         E data2 = data.get (o2);

         if (myComparator != null) {
            return myComparator.compare(data1, data2);
         } else if (data1 instanceof Comparable<?>) {
            @SuppressWarnings("unchecked")
            Comparable<E> e1 = (Comparable<E>)data1;
            return e1.compareTo(data2);
         }
         return 0;
      }
   }

   private static int[] createIntIndexArray(int n) {
      int[] out = new int[n];
      for (int i=0; i<n; ++i) {
         out[i] = i;
      }
      return out;
   }

   public static<E> int[] sortIndices(List<E> list, Comparator<E> comparator) {
      int[] out = createIntIndexArray(list.size());
      sortIndices (out, list, comparator);
      return out;
   }
   
   public static<E extends Comparable<E>> int[] sortIndices(List<E> list) {
      int[] out = createIntIndexArray(list.size());
      sortIndices (out, list, null);
      return out;
   }

   public static<E extends Comparable<E>> void sortIndices(int[] idxs, List<E> elements) {
      sortIndices (idxs, 0, idxs.length, elements, null);
   }
   
   public static<E extends Comparable<E>> void sortIndices(int[] idxs, int start, int count, List<E> elements) {
      sortIndices (idxs, start, count, elements, null);
   }
   
   public static<E> void sortIndices(int[] idxs, List<E> elements, Comparator<E> comparator) {
      sortIndices (idxs, 0, idxs.length, elements, comparator);
   }
   
   public static<E> void sortIndices(int[] idxs, int start, int count, List<E> elements, Comparator<E> comparator) {

      // wrapper list
      IndexWrapper wrapper = new IndexWrapper (idxs, start, count);
      Comparator<Integer> comp = new ListIndexComparator<> (elements, comparator);
      Collections.sort (wrapper, comp);

   }

   public static<E extends Comparable<E>> int[] sortIndices(E[] list) {
      return sortIndices(list, null);
   }
   
   public static<E> int[] sortIndices(E[] list, Comparator<E> comparator) {
      int[] out = createIntIndexArray(list.length);
      sortIndices (out, list, comparator);
      return out;
   }

   public static<E extends Comparable<E>> void sortIndices(int[] idxs, E[] elements) {
      sortIndices (idxs, 0, idxs.length, elements, null);
   }
   
   public static<E extends Comparable<E>> void sortIndices(int[] idxs, int start, int count, E[] elements) {
      sortIndices (idxs, start, count, elements, null);
   }
   
   public static<E> void sortIndices(int[] idxs, E[] elements, Comparator<E> comparator) {
      sortIndices(idxs, 0, idxs.length, elements, comparator);
   }
   
   public static<E> void sortIndices(int[] idxs, int start, int count, E[] elements, Comparator<E> comparator) {
      // wrapper list
      IndexWrapper wrapper = new IndexWrapper (idxs, start, count);
      Comparator<Integer> comp = new ArrayIndexComparator<> (elements, comparator);
      Collections.sort (wrapper, comp);

   }
}
