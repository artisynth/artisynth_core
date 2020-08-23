/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.interpolation;

import maspack.matrix.VectorNd;
import maspack.util.TestException;

class NumericListTest {
   private int myVsize = 2;
   NumericList list = new NumericList (myVsize);

   NumericListKnot newKnot (double t, double v0, double v1) {
      NumericListKnot knot = new NumericListKnot (myVsize);
      knot.t = t;
      knot.v.set (0, v0);
      knot.v.set (1, v1);
      return knot;
   }

   // private String listContentsStr()
   // {
   // String s = "";
   // NumberFormat fmt = new NumberFormat ("%8.3f");
   // for (Iterator<NumericListKnot> it=list.iterator(); it.hasNext(); )
   // { NumericListKnot knot = it.next();
   // s += fmt.format(knot.t) + " " + knot.v.toString(fmt) + "\n";
   // }
   // return s;
   // }

   NumericList makeTestList (double[] vals) {
      int k = 0;
      NumericList newList = new NumericList (myVsize);
      while (k < vals.length - myVsize) {
         NumericListKnot knot = new NumericListKnot (myVsize);
         knot.t = vals[k++];
         for (int i = 0; i < myVsize; i++) {
            knot.v.set (i, vals[k++]);
         }
         knot.next = null;
         if (newList.myTail == null) {
            knot.prev = null;
            newList.myHead = knot;
         }
         else {
            knot.prev = newList.myTail;
            newList.myTail.next = knot;
         }
         newList.myTail = knot;
      }
      return newList;
   }

   void checkContents (NumericList list, double[] vals) {
      NumericList check = makeTestList (vals);
      if (!list.equals (check)) {
         throw new TestException (
            "Expecting list\n" + check.toString ("%8.3f") +
            "Got:\n" + list.toString ("%8.3f"));
      }
   }

   void checkInterpolation (NumericList list, double t, double v0, double v1) {
      VectorNd v = new VectorNd (myVsize);
      VectorNd vcheck = new VectorNd (myVsize);
      vcheck.set (0, v0);
      vcheck.set (1, v1);
      list.interpolate (v, t);
      if (!v.epsilonEquals (vcheck, 1e-9)) {
         throw new TestException (
            "Interpolation at time " + t + "\n" +
            "Got " + v.toString ("%8.3f") +
            ", expected " + vcheck.toString ("%8.3f"));
      }
   }

   public NumericListTest() {
   }

   public void test() {
      list.add (newKnot (0, 0, 0));
      list.add (newKnot (2, 4, 0));
      list.add (newKnot (4, 0, 0));

      checkContents (list, new double[] { 0, 0, 0, 2, 4, 0, 4, 0, 0 });
      checkInterpolation (list, 0, 0, 0);
      checkInterpolation (list, -1, 0, 0);
      checkInterpolation (list, 1, 0, 0);
      checkInterpolation (list, 2, 4, 0);
      checkInterpolation (list, 3, 4, 0);
      checkInterpolation (list, 4, 0, 0);
      checkInterpolation (list, 5, 0, 0);

      list.clear();
      list.add (newKnot (4, 0, 0));
      list.add (newKnot (2, 4, 0));
      list.add (newKnot (0, 0, 0));

      checkContents (list, new double[] { 0, 0, 0, 2, 4, 0, 4, 0, 0 });

      NumericList newList = null;

      newList = (NumericList)list.clone();

      checkContents (newList, new double[] { 0, 0, 0, 2, 4, 0, 4, 0, 0 });

      list.clear();
      list.add (newKnot (3, 1, 0));
      list.add (newKnot (5, 3, 0));
      list.add (newKnot (2, 2, 0));

      checkContents (list, new double[] { 2, 2, 0, 3, 1, 0, 5, 3, 0 });

      checkInterpolation (list, 1, 0, 0);
      checkInterpolation (list, 2, 2, 0);
      checkInterpolation (list, 4, 1, 0);
      list.getInterpolation().setDataExtended (true);
      checkInterpolation (list, 5, 3, 0);
      checkInterpolation (list, 6, 3, 0);
      list.getInterpolation().setDataExtended (false);
      checkInterpolation (list, 6, 0, 0);

      list.setInterpolation (new Interpolation (
         Interpolation.Order.Linear, false));

      checkInterpolation (list, 1, 0, 0);
      checkInterpolation (list, 2, 2, 0);
      checkInterpolation (list, 4, 2, 0);
      list.getInterpolation().setDataExtended (true);
      checkInterpolation (list, 5, 3, 0);
      checkInterpolation (list, 6, 3, 0);
      list.getInterpolation().setDataExtended (false);
      checkInterpolation (list, 6, 0, 0);

   }

   public static void main (String[] args) {
      NumericListTest tester = new NumericListTest();

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
