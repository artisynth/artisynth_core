/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.util.*;
import java.io.*;
import java.util.*;

public class ScannableListTest {
   private class ListItem implements Scannable {
      int myNum;

      public ListItem (int num) {
         myNum = num;
      }

      public int getNum() {
         return myNum;
      }

      /**
       * {@inheritDoc}
       */
      public boolean isWritable() {
         return true;
      }
      
      public void write (PrintWriter wr, NumberFormat fmt, Object ref)
         throws IOException {
         // stub
      }

      public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
         // stub
      }
   }

   private void doAssert (String msg, boolean pred) {
      if (!pred) {
         throw new TestException ("assertion failed: " + msg);
      }
   }

   private void throwListContentException (
      ScannableList<ListItem> list, int[] nums) {
      StringBuilder sbuf = new StringBuilder();
      sbuf.append ("Expected list [");
      for (int i = 0; i < nums.length; i++) {
         sbuf.append (" " + nums[i]);
      }
      sbuf.append ("]\n");
      sbuf.append ("Got list [");
      for (int i = 0; i < list.size(); i++) {
         sbuf.append (" " + list.get (i).getNum());
      }
      sbuf.append ("]");
      throw new TestException (sbuf.toString());
   }

   private void checkList (ScannableList<ListItem> list, int[] nums) {
      doAssert ("wrong sized list", list.size() == nums.length);
      doAssert ("isEmpty incorrect", list.isEmpty() == (nums.length == 0));
      for (int i = 0; i < nums.length; i++) {
         if (list.get (i).getNum() != nums[i]) {
            throwListContentException (list, nums);
         }
         doAssert ("contains failed", list.contains (list.get (i)));
         doAssert ("indexOf failed", list.indexOf (list.get (i)) == i);
      }
      Iterator<ListItem> it = list.iterator();
      int i = 0;
      while (it.hasNext()) {
         doAssert ("iterator failed", it.next() == list.get (i++));
      }
      ListItem[] array = list.toArray (new ListItem[0]);
      doAssert ("toArray gives wrong size", array.length == nums.length);
      for (i = 0; i < array.length; i++) {
         doAssert ("toArray gives wrong element", array[i] == list.get (i));
      }
      array = list.toArray (new ListItem[nums.length]);
      doAssert ("toArray (C[]) gives wrong size", array.length == nums.length);
      for (i = 0; i < array.length; i++) {
         doAssert ("toArray(C[]) gives wrong element", array[i] == list.get (i));
      }
   }

   private ScannableList<ListItem> createList (int[] nums) {
      ScannableList<ListItem> list =
         new ScannableList<ListItem> (ListItem.class);
      for (int i = 0; i < nums.length; i++) {
         list.add (new ListItem (nums[i]));
      }
      return list;
   }

   public void test() {
      ScannableList<ListItem> list =
         new ScannableList<ListItem> (ListItem.class);

      checkList (list, new int[] {});
      for (int i = 0; i < 5; i++) {
         list.add (new ListItem (i));
      }
      checkList (list, new int[] { 0, 1, 2, 3, 4 });
      list.add (0, new ListItem (5));
      checkList (list, new int[] { 5, 0, 1, 2, 3, 4 });
      list.add (6, new ListItem (6));
      checkList (list, new int[] { 5, 0, 1, 2, 3, 4, 6 });
      list.add (2, new ListItem (7));
      checkList (list, new int[] { 5, 0, 7, 1, 2, 3, 4, 6 });
      list.remove (2);
      checkList (list, new int[] { 5, 0, 1, 2, 3, 4, 6 });
      list.remove (list.get (6));
      checkList (list, new int[] { 5, 0, 1, 2, 3, 4 });
      list.remove (list.get (0));
      checkList (list, new int[] { 0, 1, 2, 3, 4 });
      Iterator<ListItem> it = list.iterator();
      while (it.hasNext()) {
         it.next();
         it.remove();
      }
      checkList (list, new int[] {});
      for (int i = 0; i < 5; i++) {
         list.add (new ListItem (i));
      }
      checkList (list, new int[] { 0, 1, 2, 3, 4 });

      // check for concurrent mod exception
      boolean exceptionOccured = false;
      list = createList (new int[] { 0, 1, 2, 3, 4 });
      it = list.iterator();
      it.next();
      list.add (new ListItem (8));
      try {
         it.next();
      }
      catch (ConcurrentModificationException e) {
         exceptionOccured = true;
      }
      doAssert ("no comod exception on add", exceptionOccured);

      exceptionOccured = false;
      list = createList (new int[] { 0, 1, 2, 3, 4 });
      it = list.iterator();
      it.next();
      list.remove (3);
      try {
         it.next();
      }
      catch (ConcurrentModificationException e) {
         exceptionOccured = true;
      }
      doAssert ("no comod exception on remove", exceptionOccured);

   }

   public static void main (String[] args) {
      ScannableListTest tester = new ScannableListTest();
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
