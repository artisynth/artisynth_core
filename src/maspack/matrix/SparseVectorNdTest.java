/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;
import maspack.util.TestException;

class SparseVectorNdTest extends VectorTest {
   void add (Vector vr, Vector v1) {
      ((SparseVectorNd)vr).add ((SparseVectorNd)v1);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void add (Vector vr, Vector v1, Vector v2) {
      ((SparseVectorNd)vr).add ((SparseVectorNd)v1, (SparseVectorNd)v2);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void sub (Vector vr, Vector v1) {
      ((SparseVectorNd)vr).sub ((SparseVectorNd)v1);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void sub (Vector vr, Vector v1, Vector v2) {
      ((SparseVectorNd)vr).sub ((SparseVectorNd)v1, (SparseVectorNd)v2);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void negate (Vector vr, Vector v1) {
      ((SparseVectorNd)vr).negate ((SparseVectorNd)v1);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void negate (Vector vr) {
      ((SparseVectorNd)vr).negate();
      consistencyCheck ((SparseVectorNd)vr);
   }

   void scale (Vector vr, double s, Vector v1) {
      ((SparseVectorNd)vr).scale (s, (SparseVectorNd)v1);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void scale (Vector vr, double s) {
      ((SparseVectorNd)vr).scale (s);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void setZero (Vector vr) {
      ((SparseVectorNd)vr).setZero();
      consistencyCheck ((SparseVectorNd)vr);
   }

   void interpolate (Vector vr, double s, Vector v1) {
      ((SparseVectorNd)vr).interpolate (s, (SparseVectorNd)v1);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void interpolate (Vector vr, Vector v1, double s, Vector v2) {
      ((SparseVectorNd)vr).interpolate (
         (SparseVectorNd)v1, s, (SparseVectorNd)v2);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void scaledAdd (Vector vr, double s, Vector v1) {
      ((SparseVectorNd)vr).scaledAdd (s, (SparseVectorNd)v1);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void scaledAdd (Vector vr, double s, Vector v1, Vector v2) {
      ((SparseVectorNd)vr).scaledAdd (s, (SparseVectorNd)v1, (SparseVectorNd)v2);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void combine (Vector vr, double a, Vector v1, double b, Vector v2) {
      ((SparseVectorNd)vr).combine (
         a, (SparseVectorNd)v1, b, (SparseVectorNd)v2);
      consistencyCheck ((SparseVectorNd)vr);
   }

   double dot (Vector v1, Vector v2) {
      return ((SparseVectorNd)v1).dot ((SparseVectorNd)v2);
   }

   double angle (Vector v1, Vector v2) {
      return ((SparseVectorNd)v1).angle ((SparseVectorNd)v2);
   }

   void normalize (Vector vr) {
      ((SparseVectorNd)vr).normalize();
      consistencyCheck ((SparseVectorNd)vr);
   }

   void normalize (Vector vr, Vector v1) {
      ((SparseVectorNd)vr).normalize ((SparseVectorNd)v1);
      consistencyCheck ((SparseVectorNd)vr);
   }

   void set (Vector vr, Vector v1) {
      ((SparseVectorNd)vr).set ((SparseVectorNd)v1);
      consistencyCheck ((SparseVectorNd)vr);
   }

   // void testMul (SparseVectorNd vr, MatrixNd M, SparseVectorNd v1)
   // {
   // saveResult (vr);
   // eExpected = mulCheck (vr, M, v1);
   // saveExpectedResult (vr);
   // try
   // { vr.mul (M, v1);
   // }
   // catch (Exception e)
   // { eActual = e;
   // }
   // checkAndRestoreResult (vr);

   // eExpected = mulTransposeCheck (vr, M, v1);
   // saveExpectedResult (vr);
   // try
   // { vr.mulTranspose (M, v1);
   // }
   // catch (Exception e)
   // { eActual = e;
   // }
   // checkAndRestoreResult (vr);
   // }

   // Exception mulCheck (SparseVectorNd vr, MatrixNd M, SparseVectorNd v1)
   // {
   // if (M.colSize() != v1.size())
   // { return new ImproperSizeException ("Incompatible dimensions");
   // }
   // else if (M.rowSize() != vr.size())
   // { if (vr.isFixedSize())
   // { return new ImproperSizeException ("Incompatible dimensions");
   // }
   // }
   // SparseVectorNd res = new SparseVectorNd (M.rowSize());
   // for (int i=0; i<M.rowSize(); i++)
   // { double sum = 0;
   // for (int j=0; j<M.colSize(); j++)
   // { sum += M.get (i, j)*v1.get(j);
   // }
   // res.set (i, sum);
   // }
   // vr.set (res);
   // return null;
   // }

   // Exception mulTransposeCheck (
   // SparseVectorNd vr, MatrixNd M, SparseVectorNd v1)
   // {
   // if (M.rowSize() != v1.size())
   // { return new ImproperSizeException ("Incompatible dimensions");
   // }
   // else if (M.colSize() != vr.size())
   // { if (vr.isFixedSize())
   // { return new ImproperSizeException ("Incompatible dimensions");
   // }
   // }
   // SparseVectorNd res = new SparseVectorNd (M.colSize());
   // for (int i=0; i<M.colSize(); i++)
   // { double sum = 0;
   // for (int j=0; j<M.rowSize(); j++)
   // { sum += M.get (j, i)*v1.get(j);
   // }
   // res.set (i, sum);
   // }
   // vr.set (res);
   // return null;
   // }

   void consistencyCheck (SparseVectorNd vr) throws TestException {
      SparseVectorCell cell;
      SparseVectorCell prev = null;
      for (cell = vr.elems; cell != null; cell = cell.next) {
         if (prev != null && prev.i >= cell.i) {
            throw new TestException ("cell " + cell + " follows " + prev);
         }
         if (cell.i >= vr.size) {
            throw new TestException (
               "vector contains cell "+cell+" with exceeding size "+vr.size);
         }
         prev = cell;
      }

      for (int i = 0; i < vr.size; i++) {
         cell = vr.elems;
         while (cell != null && cell.i != i) {
            cell = cell.next;
         }
         if (vr.get (i) == 0) {
            if (cell != null) {
               throw new TestException ("cell found at zero location ("+i+")");
            }
         }
         else {
            if (cell.i != i) {
               throw new TestException (
                  "cell at location (" + i + ") has indices (" + cell.i + ")");
            }
            if (cell.value != vr.get (i)) {
               throw new TestException (
                  "cell at location (" + i + ") has value " +
                  cell.value + " vs. " + vr.get (i));
            }
         }
      }
   }

   public void execute() {
      SparseVectorNd vr_9 = new SparseVectorNd (9);
      SparseVectorNd vr_11 = new SparseVectorNd (11);
      SparseVectorNd v1_9 = new SparseVectorNd (9);
      SparseVectorNd v2_9 = new SparseVectorNd (9);
      SparseVectorNd v1_11 = new SparseVectorNd (11);
      SparseVectorNd v2_11 = new SparseVectorNd (11);

      // MatrixNd M3x3 = new MatrixNd(3,3);
      // MatrixNd M4x3 = new MatrixNd(4,3);

      RandomGenerator.setSeed (0x1234);

      testGeneric (v1_9);
      testSetZero (vr_9);

      for (int i = 0; i < 100; i++) {
         v1_9.setRandom();
         v2_9.setRandom();
         vr_9.setRandom();
         v1_11.setRandom();
         v2_11.setRandom();
         vr_11.setRandom();

         // M3x3.setRandom();
         // M4x3.setRandom();

         testAdd (vr_9, v1_9, v2_9);
         testAdd (vr_9, v1_9, v2_11);
         testAdd (vr_9, v1_11, v2_9);
         testAdd (vr_9, v1_11, v2_11);
         testAdd (vr_9, vr_9, vr_9);

         testSub (vr_9, v1_9, v2_9);
         testSub (vr_9, v1_9, v2_11);
         testSub (vr_9, v1_11, v2_9);
         testSub (vr_9, v1_11, v2_11);
         testSub (vr_9, vr_9, vr_9);

         testNegate (vr_9, v1_9);
         testNegate (vr_9, v1_11);
         testNegate (vr_9, vr_9);

         testScale (vr_9, 1.23, v1_9);
         testScale (vr_9, 1.23, v1_11);
         testScale (vr_9, 1.23, vr_9);

         testSet (vr_9, v1_9);
         testSet (vr_9, v1_11);
         testSet (vr_9, vr_9);

         testNormalize (vr_9, v1_9);
         testNormalize (vr_9, v1_11);
         testNormalize (vr_9, vr_9);

         testCombine (vr_9, 0.123, v1_9, 0.677, v2_9);
         testCombine (vr_9, 0.123, v1_11, 0.677, v2_9);
         testCombine (vr_9, 0.123, v1_9, 0.677, v2_11);
         testCombine (vr_9, 0.123, v1_11, 0.677, v2_11);
         testCombine (vr_9, 0.123, vr_9, 0.677, vr_9);

         // testMul (vr_9, M3x3, v1_9);
         // testMul (vr_9, M3x3, vr_9);
         // testMul (vr_11, M4x3, v1_9);
         // testMul (vr_11, M4x3, vr_9);
         // testMul (vr_9, M4x3, v1_11);
         // testMul (vr_9, M4x3, vr_11);

         testNorms (v1_9);
         testDotAndAngle (v1_9, v2_9);
      }
   }

   public static void main (String[] args) {
      SparseVectorNdTest test = new SparseVectorNdTest();

      try {
         test.execute();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }

      System.out.println ("\nPassed\n");
   }
}
