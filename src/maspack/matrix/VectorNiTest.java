/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.RandomGenerator;
import maspack.util.TestException;


class VectorNiTest extends VectorTest {

   Exception eActual;
   Exception eExpected;

   VectorNi vrsave = new VectorNi (1);
   VectorNi vx = new VectorNi (1);

   private String elementFailMessage (String msg, int i) {
      return (msg + "(" + i + ") failed:");
   }

   void checkResult (
      VectorNi vr, VectorNi vc, Exception eactual, Exception eexpected) {
      MatrixTest.checkExceptions (eactual, eexpected);
      if (!vr.equals (vc)) {
         VectorNi ME = new VectorNi (vr);
         VectorNi v1 = new VectorNi (vc);
         ME.sub (v1);
         ME.absolute();
         throw new TestException (
            "Expected result:\n"
            + vc.toString () + "\n" + "Actual result:\n"
            + vr.toString () + "\n" + "max err: "
            + ME.maxElement());
      }
   }

   protected void saveResult (VectorNi vr) {
      eActual = null;
      vrsave.set (vr);
   }

   protected void saveExpectedResult (VectorNi vr) {
      vx.set (vr);
      vr.set (vrsave);
   }

   protected void checkAndRestoreResult (VectorNi vr) {
      checkResult (vr, vx, eActual, eExpected);
      vr.set (vrsave);
      eActual = null;
   }

   protected void restoreResult (VectorNi vr) {
      vr.set (vrsave);
      eActual = null;
   }

   void testGeneric (VectorNi vr) {
      testSetAndGet (vr);
   }

   void testSetZero (VectorNi vr) {
      vrsave.set (vr);
      vr.setZero ();
      for (int i = 0; i < vr.size(); i++) {
         if (vr.get (i) != 0) {
            throw new TestException (elementFailMessage ("setZero", i));
         }
      }
      vr.set (vrsave);
   }

   void testSetAndGet (VectorNi vr) {
      Random randGen = RandomGenerator.get();
      int size = vr.size();
      int[] setBuf = new int[size];
      int[] getBuf = new int[size];
      for (int i = 0; i < size; i++) {
         int value = randGen.nextInt(1000);
         vr.set (i, value);
         if (vr.get (i) != value) {
            throw new TestException (elementFailMessage ("get/set", i));
         }
         setBuf[i] = value;
      }
      vr.get (getBuf);
      for (int i = 0; i < size; i++) {
         if (getBuf[i] != setBuf[i]) {
            throw new TestException (elementFailMessage ("set", i));
         }
      }
      vx.set (vr);
      vr.set (vx);
      vr.get (getBuf);
      for (int i = 0; i < size; i++) {
         if (getBuf[i] != setBuf[i]) {
            throw new TestException ("set(VectorNi) failed for i=" + i + ": get="
            + getBuf[i] + ", set=" + setBuf[i]);
         }
      }
   }

   void testAdd (VectorNi vr, VectorNi v1, VectorNi v2) {
      saveResult (vr);
      eExpected = addCheck (vr, v1, v2);
      saveExpectedResult (vr);
      try {
         vr.add (v1, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
      eExpected = addCheck (vr, vr, v1);
      saveExpectedResult (vr);
      try {
         vr.add (v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testSub (VectorNi vr, VectorNi v1, VectorNi v2) {
      saveResult (vr);
      eExpected = subCheck (vr, v1, v2);
      saveExpectedResult (vr);
      try {
         vr.sub (v1, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
      eExpected = subCheck (vr, vr, v1);
      saveExpectedResult (vr);
      try {
         vr.sub (v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   private int computeOneNorm (VectorNi v) {
      int norm = 0;
      for (int i=0; i<v.size(); i++) {
         norm += Math.abs(v.get(i));
      }
      return norm;
   }

   private int computeInfinityNorm (VectorNi v) {
      int norm = 0;
      for (int i=0; i<v.size(); i++) {
         int abs = Math.abs(v.get(i));
         if (abs > norm) {
            norm = abs;
         }
      }
      return norm;
   }

   private int getMaxElement (VectorNi v) {
      int max = 0;
      for (int i=0; i<v.size(); i++) {
         int val = v.get(i);
         if (i == 0) {
            max = val;
         }
         else {
            max = Math.max (val, max);
         }
      }
      return max;
   }

   private int getMinElement (VectorNi v) {
      int min = 0;
      for (int i=0; i<v.size(); i++) {
         int val = v.get(i);
         if (i == 0) {
            min = val;
         }
         else {
            min = Math.min (val, min);
         }
      }
      return min;
   }

   void testNorms (VectorNi v1) {
      int expectedNorm;
      expectedNorm = computeOneNorm (v1);
      if (v1.oneNorm() != expectedNorm) {
         throw new TestException ("oneNorm: expected " + expectedNorm
         + ", got " + v1.oneNorm());
      }
      expectedNorm = computeInfinityNorm (v1);
      if (v1.infinityNorm() != expectedNorm) {
         throw new TestException ("infinityNorm: expected " + expectedNorm
         + ", got " + v1.infinityNorm());
      }
      if (v1.maxElement() != getMaxElement (v1)) {
         throw new TestException ("maxElement: expected "
         + getMaxElement (v1) + ", got " + v1.maxElement());
      }
      if (v1.minElement() != getMinElement (v1)) {
         System.out.println ("v1=" + v1);
         throw new TestException ("minElement: expected "
         + getMinElement (v1) + ", got " + v1.minElement());
      }
   }

   void testNegate (VectorNi vr, VectorNi v1) {
      saveResult (vr);
      eExpected = scaleCheck (vr, v1, -1);
      saveExpectedResult (vr);
      try {
         vr.negate (v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = scaleCheck (vr, vr, -1);
      saveExpectedResult (vr);
      try {
         vr.negate ();
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testScale (VectorNi vr, double s, VectorNi v1) {
      saveResult (vr);
      eExpected = scaleCheck (vr, v1, s);
      saveExpectedResult (vr);
      try {
         vr.scale (s, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = scaleCheck (vr, vr, s);
      saveExpectedResult (vr);
      try {
         vr.scale (s);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testSet (VectorNi vr, VectorNi v1) {
      saveResult (vr);
      eExpected = scaleCheck (vr, v1, 1);
      saveExpectedResult (vr);
      try {
         vr.set (v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   public void execute() {
      VectorNi vr_2 = new VectorNi (2);
      VectorNi vr_9 = new VectorNi (9);
      VectorNi vr_11 = new VectorNi (11);
      VectorNi v1_3 = new VectorNi (3);
      VectorNi v1_9 = new VectorNi (9);
      VectorNi v2_9 = new VectorNi (9);
      VectorNi v1_11 = new VectorNi (11);
      VectorNi v2_11 = new VectorNi (11);

      MatrixNd M3x2 = new MatrixNd (3, 2);
      MatrixNd M9x9 = new MatrixNd (9, 9);
      MatrixNd M11x9 = new MatrixNd (11, 9);

      RandomGenerator.setSeed (0x1234);

      testGeneric (v1_9);
      testSetZero (vr_9);

      for (int i = 0; i < 100; i++) {
         v1_3.setRandom();
         v1_9.setRandom();
         v2_9.setRandom();
         vr_9.setRandom();
         v1_11.setRandom();
         v2_11.setRandom();
         vr_11.setRandom();

         M3x2.setRandom();
         M9x9.setRandom();
         M11x9.setRandom();

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

         testNorms (v1_9);
      }
   }

   private Exception checkSizes (VectorNi vr, VectorNi v1, VectorNi v2) {
      if (v1.size() != v2.size()) {
         return new ImproperSizeException ("Incompatible dimensions");
      }
      if (vr.size() != v1.size()) {
         if (vr.isFixedSize()) {
            return new ImproperSizeException ("Incompatible dimensions");
         }
         else {
            vr.setSize (v1.size());
         }
      }
      return null;
   }

   Exception addCheck (VectorNi vr, VectorNi v1, VectorNi v2) {
      Exception e = checkSizes (vr, v1, v2);
      if (e != null) {
         return e;
      }
      int[] buf = new int[v1.size()];
      for (int i = 0; i < v1.size(); i++) {
         buf[i] = v1.get (i) + v2.get (i);
      }
      vr.set (buf);
      return null;
   }

   Exception subCheck (VectorNi vr, VectorNi v1, VectorNi v2) {
      Exception e = checkSizes (vr, v1, v2);
      if (e != null) {
         return e;
      }
      int[] buf = new int[v1.size()];
      for (int i = 0; i < v1.size(); i++) {
         buf[i] = v1.get (i) - v2.get (i);
      }
      vr.set (buf);
      return null;
   }

   Exception scaleCheck (VectorNi vr, VectorNi v1, double scale) {
      Exception e = checkSizes (vr, v1, v1);
      if (e != null) {
         return e;
      }
      int[] buf = new int[v1.size()];
      for (int i = 0; i < v1.size(); i++) {
         buf[i] = (int)(scale*v1.get(i));
      }
      vr.set (buf);
      return null;
   }

   public static void main (String[] args) {
      VectorNiTest test = new VectorNiTest();

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
