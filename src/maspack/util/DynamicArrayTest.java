/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

public class DynamicArrayTest {
   private void doassert (boolean pred) {
      if (!pred) {
         throw new TestException ("assertion failed");
      }
   }

   static class VectorX {
      double x;
      double y; 
      double z;
      
      public VectorX() {
      }
   }

   public void test() {
      DynamicArray<VectorX> array0 =
         new DynamicArray<VectorX> (VectorX.class);
      doassert (array0.size() == 0);
      VectorX[] twists = new VectorX[5];
      for (int i = 0; i < 5; i++) {
         twists[i] = new VectorX();
         twists[i].x = i;
         array0.add (twists[i]);
      }
      doassert (array0.size() == 5);
      for (int i = 0; i < 5; i++) {
         doassert (array0.get(i).x == i);
      }
      array0.resize (10);
      doassert (array0.size() == 10);
      for (int i = 0; i < 10; i++) {
         if (i < 5) {
            doassert (array0.get(i).x == i);
         }
         else {
            doassert (array0.get(i) == null);
            VectorX vec = new VectorX();
            vec.x = i;
            array0.set (i, vec);
         }
      }

      for (int i = 0; i < 10; i++) {
         doassert (array0.get(i).x == i);
      }

      DynamicArray<VectorX> array1 =
         new DynamicArray<VectorX> (VectorX.class);
      array1.addAll (array0);
      doassert (array1.size() == 10);
      for (int i = 0; i < 10; i++) {
         doassert (array1.get(i) != null);
      }

      DynamicArray<VectorX> array2 =
         new DynamicArray<VectorX> (VectorX.class, 0);
      doassert (array2.size() == 0);

   }

   public static void main (String[] args) {
      DynamicArrayTest tester = new DynamicArrayTest();
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

   
      
