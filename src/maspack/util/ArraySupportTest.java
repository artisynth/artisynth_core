package maspack.util;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;

public class ArraySupportTest extends UnitTest {

   void testRemove (int n, int[] idxs, int[] chk) {
      ArrayList<Integer> list = new ArrayList<>();
      for (int i=0; i<n; i++) {
         list.add (i);
      }
      ArraySupport.removeListItems (list, idxs);
      ArrayList<Integer> check = new ArrayList<>();
      for (int i=0; i<chk.length; i++) {
         check.add (chk[i]);
      }
      if (!list.equals (check)) {
         throw new TestException (
            "Editted list = " + list + "; expected " + check);
      }
   }

   void testRemove (int n, int[] idxs) {
      ArrayList<Integer> list = new ArrayList<>();
      for (int i=0; i<n; i++) {
         list.add (i);
      }
      ArraySupport.removeListItems (list, idxs);
      boolean[] removed = new boolean[n];
      for (int k=0; k<idxs.length; k++) {
         removed[idxs[k]] = true;
      }
      ArrayList<Integer> check = new ArrayList<>();
      for (int i=0; i<n; i++) {
         if (!removed[i]) {
            check.add (i);
         }
      }
      if (!list.equals (check)) {
         throw new TestException (
            "Editted list = " + list + "; expected " + check);
      }
   }

   void testArrayListRemove () {
      testRemove (5, new int[] { 3 }, new int[] { 0, 1, 2, 4 });
      testRemove (5, new int[] { }, new int[] { 0, 1, 2, 3, 4 });
      testRemove (5, new int[] { 4 }, new int[] { 0, 1, 2, 3, });
      testRemove (5, new int[] { 0 }, new int[] { 1, 2, 3, 4 });
      testRemove (5, new int[] { 1, 2, 3 }, new int[] { 0, 4 });
      testRemove (5, new int[] { 3, 2, 1 }, new int[] { 0, 4 });
      testRemove (5, new int[] { 3, 4, 0, 2, 1 }, new int[] { });
      testRemove (5, new int[] { 3, 4, 0, 1 }, new int[] { 2 });
      testRemove (5, new int[] { 3, 4, 2, 1 }, new int[] { 0 });
      testRemove (5, new int[] { 3, 0, 2, 1 }, new int[] { 4 });

      testRemove (6, new int[] { 1, 2, 4, 5 }, new int[] { 0, 3 });
      
      int ntests = 1000;
      int size = 30;
      for (int i=0; i<ntests; i++) {
         int[] idxs = RandomGenerator.randomSubsequence(size);
         testRemove (size, idxs);         
      }
   }

   public void test() {
      testArrayListRemove();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      ArraySupportTest tester = new ArraySupportTest();
      tester.runtest();
   }

}
