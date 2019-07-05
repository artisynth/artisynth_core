/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.util.*;

/**
 * Unit test for ListRemove.
 */
public class ListRemoveTest extends UnitTest {

   Random myRand = RandomGenerator.get();

   private String toString (List<Integer> list) {
      StringBuilder builder = new StringBuilder();
      for (int i=0; i<list.size(); i++) {
         if (i > 0) {
            builder.append (" ");
         }
         builder.append (list.get(i));
      }
      return builder.toString();
   }

   private ArrayList<Integer> createList (int num) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (int i=0; i<num; i++) {
         list.add (i);
      }
      return list;
   }

   private ArrayList<Integer> createList (int[] array) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (int i=0; i<array.length; i++) {
         list.add (array[i]);
      }
      return list;
   }

   private ArrayList<Integer> randomCull (int num) {

      ArrayList<Integer> list = new ArrayList<Integer>();
      for (int i=0; i<num; i++) {
         if (myRand.nextInt(2) == 1) {
            list.add (i);
         }
      }
      return list;
   }

   public void testCull (List<Integer> list, ArrayList<Integer> culledList) {

      ArrayList<Integer> savedList = new ArrayList<Integer>();
      savedList.addAll (list);
      ListRemove<Integer> remove = new ListRemove<Integer> (list);
      int k = 0;
      for (int i=0; i<list.size(); i++) {
         if (k < culledList.size() && culledList.get(k) == i) {
            k++;
         }
         else {
            remove.requestRemove (i);
         }
      }
      remove.remove();
      
      if (!list.equals(culledList)) {
         throw new TestException (
            "Execute: expected '"+toString(culledList)+
            "', got '"+toString(list)+"'");
      }
      remove.undo();      
      if (!list.equals(savedList)) {
         throw new TestException (
            "Undo: expected '"+toString(savedList)+
            "', got '"+toString(list)+"'");
      }
   }

   public void testSpecific (int num, int[] culled) {

      ArrayList<Integer> arrayList = createList (num);
      LinkedList<Integer> linkedList = new LinkedList<Integer>();
      linkedList.addAll (arrayList);

      ArrayList<Integer> culledList = createList (culled);

      testCull (arrayList, culledList);
      testCull (linkedList, culledList);
   }

   public void testRandom (int num) {
      ArrayList<Integer> arrayList = createList (num);
      LinkedList<Integer> linkedList = new LinkedList<Integer>();
      linkedList.addAll (arrayList);
      ArrayList<Integer> culledList = randomCull (num);

      testCull (arrayList, culledList);
      testCull (linkedList, culledList);
   }

   public void test() {
      // remove first element
      testSpecific (10, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9});
      // remove last element
      testSpecific (10, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8});
      // remove middle element            
      testSpecific (10, new int[] {0, 1, 2, 3, 4, 6, 7, 8, 9});
      // remove two first elements
      testSpecific (10, new int[] {2, 3, 4, 5, 6, 7, 8, 9});
      // remove two last elements
      testSpecific (10, new int[] {0, 1, 2, 3, 4, 5, 6, 7});
      // remove two middle elements
      testSpecific (10, new int[] {0, 1, 2, 3, 6, 7, 8, 9});

      testSpecific (1, new int[] {});
      testSpecific (1, new int[] {0});
      testSpecific (2, new int[] {0, 1});
      testSpecific (2, new int[] {0});
      testSpecific (2, new int[] {1});
      testSpecific (2, new int[] {});

      testSpecific (3, new int[] {0, 1, 2});
      testSpecific (3, new int[] {0, 1});
      testSpecific (3, new int[] {1, 2});
      testSpecific (3, new int[] {0, 2});
      testSpecific (3, new int[] {0});
      testSpecific (3, new int[] {1});
      testSpecific (3, new int[] {2});
      testSpecific (3, new int[] {});

      testSpecific (4, new int[] {0, 1, 2, 3});
      testSpecific (4, new int[] {0, 1, 2});
      testSpecific (4, new int[] {0, 1, 3});
      testSpecific (4, new int[] {0, 2, 3});
      testSpecific (4, new int[] {1, 2, 3});
      testSpecific (4, new int[] {2, 3});
      testSpecific (4, new int[] {1, 3});
      testSpecific (4, new int[] {1, 2});
      testSpecific (4, new int[] {0, 3});
      testSpecific (4, new int[] {0, 2});
      testSpecific (4, new int[] {0, 1});
      testSpecific (4, new int[] {0});
      testSpecific (4, new int[] {1});
      testSpecific (4, new int[] {2});
      testSpecific (4, new int[] {3});
      testSpecific (4, new int[] {});

      int ntests = 10000;
      for (int i=0; i<ntests; i++) {
         testRandom (20);
      }
      
   }

   public static void main (String[] args) {
      ListRemoveTest tester = new ListRemoveTest();
      RandomGenerator.setSeed (0x1234);
      tester.runtest();
   }
}

