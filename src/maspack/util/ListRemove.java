/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.*;

/**
 * A utility class to remove items from a list, and (later) return them to the
 * list in the same order. The basic usage model is this:
 * <pre>
 * {@code
 *    List<C> list;
 *    ...
 *    ListRemove<C> listRemove = new ListRemove<C>(list);
 *    for (int i=0; i<list.size(); i++) {
 *       if (item i should be removed) {
 *          listRemove.remove (i);
 *       }
 *    }
 *    listRemove.Remove(); // do the actual removal
 *    ...
 *    listRemove.undo(); // undo the removal
 * }
 * </pre>
 * Item to be removed are specified by index. The class attempts
 * to preform the remove in O(n) time.
 */
public class ListRemove<C> {

   private enum State {
      OPEN,
      EXECUTED,
      UNDONE
   }
   
   protected State myState;
   protected List<C> myList;
   protected ArrayList<C> myRemoved;
   protected int[] myIndices;

   public ListRemove (List<C> list) {
      myList = list;
      myIndices = new int[myList.size()];
      myState = State.OPEN;
   }

   public List<C> getList() {
      return myList;
   }

   public ArrayList<C> getRemoved() {
      return myRemoved;
   }

   public void requestRemove (int idx) {
      if (myState != State.OPEN) {
         throw new IllegalStateException (
            "Remove has already been called");
      }         
      if (idx < 0 || idx >= myIndices.length) {
         throw new ArrayIndexOutOfBoundsException (
            "index=" + idx + ", size=" + myIndices.length);
      }
      myIndices[idx] = 1;
   }

   public void requestRemoveAll (Collection<Integer> idxs) {
      if (myState != State.OPEN) {
         throw new IllegalStateException (
            "Remove has already been called");
      }         
      for (int idx : idxs) {
         requestRemove (idx);
      }
   }

   boolean myExecutedP = false;

   protected int[] compactIndices (int[] indices) {
      int numr = 0;
      for (int i=0; i<indices.length; i++) {
         if (indices[i] == 1) {
            numr++;
         }
      }
      int[] compact = new int[numr];
      int k = 0;
      for (int i=0; i<indices.length; i++) {
         if (indices[i] == 1) {
            compact[k++] = i;
         }
      }
      return compact;      
   }

   public void remove() {
      if (myState == State.EXECUTED) {
         throw new IllegalStateException (
            "Remove has already been called");
      }
      if (myIndices.length > myList.size()) {
         throw new IllegalStateException (
            "List has shrunk since ListRemove was created");
      }
      myState = State.EXECUTED;
      myIndices = compactIndices (myIndices);
      myRemoved = new ArrayList<C>(myIndices.length);
      if (myIndices.length == 0) {
         return;
      }
      if (myList instanceof ArrayList<?>) {
         int k = 0;
         int j = 0;
         for (int i=0; i<myList.size(); i++) {
            if (k < myIndices.length && myIndices[k] == i) {
               myRemoved.add (myList.get(i));
               k++;
            }
            else {
               if (j < i) {
                  myList.set (j, myList.get(i));
               }
               j++;
            }
         }
         // now remove items from list end:
         for (int i=myList.size()-1; i>=j; i--) {
            myList.remove (i);
         }
      }
      else {
         ListIterator<C> li = myList.listIterator();
         int k = 0;
         int size = myList.size();
         for (int i=0; i<size; i++) {
            C comp = li.next();
            if (k < myIndices.length && myIndices[k] == i) {
               li.remove();
               myRemoved.add (comp);
               k++;
            }
         }
      }
   }

   public boolean undo() {
      if (myState != State.EXECUTED) {
         throw new IllegalStateException (
            "Remove has not yet been called");
      }
      myState = State.UNDONE;
      if (myRemoved.size() == 0) {
         return false;
      }
      if (myList instanceof ArrayList<?>) {
         // create space at list end:
         for (int i=0; i<myRemoved.size(); i++) {
            myList.add (null);
         }
         int k = myIndices.length-1;
         int j = myList.size()-1-myRemoved.size();
         for (int i=myList.size()-1; i>=0; i--) {
            if (k >= 0 && myIndices[k] == i) {
               myList.set (i, myRemoved.get(k));
               k--;
            }
            else {
               if (j < i) {
                  myList.set (i, myList.get(j));
               }
               j--;
            }
         }
         
      }
      else {
         ListIterator<C> li = myList.listIterator();
         int k = 0;
         int size = myList.size() + myRemoved.size();
         for (int i=0; i<size; ) {
            while (k < myIndices.length && myIndices[k] == i) {
               li.add (myRemoved.get(k++));
               i++;
            }
            if (li.hasNext()) {
               li.next();
               i++;
            }
         }
      }
      return true;
   }

}
