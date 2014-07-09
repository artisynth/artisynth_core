/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class CountedList<A> extends ArrayList<A> {

   private static final long serialVersionUID = 1L;
   private HashMap<A,Integer> counts;
   
   public CountedList() {
      super();
      counts = new HashMap<A,Integer>();
   }
   
   public CountedList(int size) {
      super(size);
      counts = new HashMap<A,Integer>(size);
   }
   
   @Override 
   public boolean add(A val) {
      int cnt = getCount(val);
      if (cnt == 0) {
         super.add(val);
      }
      counts.put(val, cnt+1);
      return true;
   }
   
   @Override
   public void add(int idx, A val) {
      int cnt = getCount(val);
      if (cnt == 0) {
         super.add(idx, val);
      }
      counts.put(val, cnt+1);
   }
   
   @Override
   public boolean addAll(Collection<? extends A> vals) {
      for (A val : vals) {
         add(val);
      }
      return true;
   }
   
   @Override
   public boolean addAll(int idx, Collection<? extends A> vals) {
      for (A val : vals) {
         int cnt = getCount(val);
         if (cnt == 0) {
            super.add(idx, val);
            idx++;
         }
         counts.put(val, cnt+1);
      }
      return true;
   }
   
   @Override
   public A remove(int idx) {
      A a = get(idx);
      int cnt = getCount(a);
      if (cnt == 1 ) {
         super.remove(idx);
         counts.remove(a);
      } else {
         counts.put(a, cnt-1);
      }
      
      return a;
   }
   
   @SuppressWarnings("unchecked")
   public boolean remove(Object b) {
      
      int cnt = 0;
      cnt = getCount(b);
      
      if (cnt < 1) {
         return false;
      } else if (cnt == 1) {
         super.remove(b);
         counts.remove(b);
      } else {
         counts.put((A)b, cnt-1);
      }
      return true;
   }
   
   @Override
   public boolean removeAll(Collection<?> objs) {
      boolean out = false;
      for (Object obj : objs) {
         out = out | remove(obj);
      }
      return out;
   }
   
   @Override
   public boolean retainAll(Collection<?> objs) {
      for (Object key : counts.keySet()) {
         if (!objs.contains(key)) {
            counts.remove(key);
         }
      }
      return super.retainAll(objs);
   }
   
   public int getCount(Object obj) {
      Integer a = counts.get(obj);
      if (a != null) {
         return a;
      }
      return 0;
   }
   
}
