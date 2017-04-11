/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;
import java.io.*;

import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;
import maspack.util.TestException;
import maspack.util.NumberFormat;

class VectoriTest {
   static double DOUBLE_PREC = 2.220446049250313e-16;
   static double EPSILON = 10 * DOUBLE_PREC;

   Exception eActual;
   Exception eExpected;

   VectorNi vrsave = new VectorNi (1);
   VectorNi vx = new VectorNi (1);

   void add (Vectori vr, Vectori v1, Vectori v2) {
   }

   void add (Vectori vr, Vectori v1) {
   }

   void sub (Vectori vr, Vectori v1, Vectori v2) {
   }

   void sub (Vectori vr, Vectori v1) {
   }

   void negate (Vectori vr) {
   }

   void negate (Vectori vr, Vectori v1) {
   }

   void scale (Vectori vr, double s) {
   }

   void scale (Vectori vr, double s, Vectori v1) {
   }

   void scaledAdd (Vectori vr, double s, Vectori v1) {
   }

   void scaledAdd (Vectori vr, double s, Vectori v1, Vectori v2) {
   }

   void set (Vectori vr, Vectori v1) {
   }

   void setZero (Vectori vr) {
   }

   private String elementFailMessage (String msg, int i) {
      return (msg + "(" + i + ") failed:");
   }

   void checkResult (
      Vectori vr, Vectori vc, Exception eactual, Exception eexpected) {

      MatrixTest.checkExceptions (eactual, eexpected);

      if (!vr.equals (vc)) {
         VectorNi ME = new VectorNi (vr);
         VectorNi v1 = new VectorNi (vc);
         ME.sub (v1);
         ME.absolute();
         throw new TestException ("Expected result:\n"
         + vc.toString (new NumberFormat ("%d")) + "\n" + "Actual result:\n"
         + vr.toString (new NumberFormat ("%d")) + "\n" + "max err: "
                                  + ME.maxElement());
      }
   }

   void checkResult (int res, int chk) {
      MatrixTest.checkExceptions (eActual, eExpected);
      if (res != chk) {
         throw new TestException (
            "Expected result:\n" + chk + "Actual result:\n" + res);
      }
   }

   protected void saveResult (Vectori vr) {
      eActual = null;
      vrsave.set (vr);
   }

   protected void saveExpectedResult (Vectori vr) {
      vx.set (vr);
      vr.set (vrsave);
   }

   protected void checkAndRestoreResult (Vectori vr) {
      checkResult (vr, vx, eActual, eExpected);
      vr.set (vrsave);
      eActual = null;
   }

   protected void restoreResult (Vectori vr) {
      vr.set (vrsave);
      eActual = null;
   }

   void testGeneric (Vectori vr) {
      testSetAndGet (vr);
      testScanAndWrite (vr);
   }

   void testSetZero (Vectori vr) {
      vrsave.set (vr);
      setZero (vr);
      for (int i = 0; i < vr.size(); i++) {
         if (vr.get (i) != 0) {
            throw new TestException (elementFailMessage ("setZero", i));
         }
      }
      vr.set (vrsave);
   }

   void testSetAndGet (Vectori vr) {
      Random randGen = RandomGenerator.get();
      int size = vr.size();
      int[] setBuf = new int[size];
      int[] getBuf = new int[size];
      for (int i = 0; i < size; i++) {
         int value = randGen.nextInt();
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
            throw new TestException ("set(Vectori) failed for i=" + i + ": get="
            + getBuf[i] + ", set=" + setBuf[i]);
         }
      }
   }

   void doScanWrite (Vectori vr, String fmt, boolean withBrackets) {
      StringWriter sw = new StringWriter();
      try {
         vr.write (new PrintWriter(sw), new NumberFormat(fmt), withBrackets);
         vr.scan (new ReaderTokenizer(new StringReader (sw.toString())));
      }
      catch (Exception e) {
         throw new TestException ("scan/write error: "+e.getMessage());
      }     
   }

   void doScanString (Vectori vr, String fmt) {
      try {
         vr.scan (new ReaderTokenizer(new StringReader (vr.toString(fmt))));
      }
      catch (Exception e) {
         throw new TestException ("scan/write error: "+e.getMessage());
      }     
   }

   void testScanAndWrite (Vectori vr) {
      saveResult (vr);
      saveExpectedResult (vr);
      eExpected = null;
      eActual = null;
      doScanWrite (vr, "%d", /*brackets=*/false);
      checkAndRestoreResult (vr);
      doScanWrite (vr, "%d", /*brackets=*/true);
      checkAndRestoreResult (vr);
      doScanWrite (vr, "0x%x", /*brackets=*/false);
      checkAndRestoreResult (vr);
      doScanWrite (vr, "0x%x", /*brackets=*/true);
      checkAndRestoreResult (vr);
      doScanString (vr, "%d");
      checkAndRestoreResult (vr);
      doScanString (vr, "0x%x");
      checkAndRestoreResult (vr);
   }

   void testAdd (Vectori vr, Vectori v1, Vectori v2) {
      saveResult (vr);
      eExpected = addCheck (vr, v1, v2);
      saveExpectedResult (vr);
      try {
         add (vr, v1, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
      eExpected = addCheck (vr, vr, v1);
      saveExpectedResult (vr);
      try {
         add (vr, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testScaledAdd (Vectori vr, double scale, Vectori v1, Vectori v2) {
      saveResult (vr);
      eExpected = scaledAddCheck (vr, scale, v1, v2);
      saveExpectedResult (vr);
      try {
         scaledAdd (vr, scale, v1, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
      eExpected = scaledAddCheck (vr, scale, v1, vr);
      saveExpectedResult (vr);
      try {
         scaledAdd (vr, scale, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testSub (Vectori vr, Vectori v1, Vectori v2) {
      saveResult (vr);
      eExpected = subCheck (vr, v1, v2);
      saveExpectedResult (vr);
      try {
         sub (vr, v1, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
      eExpected = subCheck (vr, vr, v1);
      saveExpectedResult (vr);
      try {
         sub (vr, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testNorms (Vectori v1) {
      double expectedNorm;
      expectedNorm = VectoriBase.computeOneNorm (v1);
      if (v1.oneNorm() != expectedNorm) {
         throw new TestException ("oneNorm: expected " + expectedNorm
         + ", got " + v1.oneNorm());
      }
      expectedNorm = VectoriBase.computeInfinityNorm (v1);
      if (v1.infinityNorm() != expectedNorm) {
         throw new TestException ("infinityNorm: expected " + expectedNorm
         + ", got " + v1.infinityNorm());
      }
      double norm = Math.sqrt (VectoriBase.computeNormSquared (v1));
      if (Math.abs (v1.norm() - norm) > EPSILON * norm) {
         throw new TestException ("norm: expected " + norm + ", got "
         + v1.norm());
      }
      double normSquared = VectoriBase.computeNormSquared (v1);
      if (Math.abs (v1.normSquared() - normSquared) > EPSILON * normSquared) {
         throw new TestException ("normSquared: expected " + normSquared
         + ", got " + v1.normSquared());
      }
      if (v1.maxElement() != VectoriBase.getMaxElement (v1)) {
         throw new TestException ("maxElement: expected "
         + VectoriBase.getMaxElement (v1) + ", got " + v1.maxElement());
      }
      if (v1.minElement() != VectoriBase.getMinElement (v1)) {
         System.out.println ("v1=" + v1);
         throw new TestException ("minElement: expected "
         + VectoriBase.getMinElement (v1) + ", got " + v1.minElement());
      }
   }

   void testNegate (Vectori vr, Vectori v1) {
      saveResult (vr);
      eExpected = scaleCheck (vr, v1, -1);
      saveExpectedResult (vr);
      try {
         negate (vr, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = scaleCheck (vr, vr, -1);
      saveExpectedResult (vr);
      try {
         negate (vr);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testScale (Vectori vr, double s, Vectori v1) {
      saveResult (vr);
      eExpected = scaleCheck (vr, v1, s);
      saveExpectedResult (vr);
      try {
         scale (vr, s, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = scaleCheck (vr, vr, s);
      saveExpectedResult (vr);
      try {
         scale (vr, s);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testSet (Vectori vr, Vectori v1) {
      saveResult (vr);
      eExpected = scaleCheck (vr, v1, 1);
      saveExpectedResult (vr);
      try {
         set (vr, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   private Exception checkSizes (Vectori vr, Vectori v1, Vectori v2) {
      if (vr instanceof VectorNi && vr == v1) {
         // then this is a += type operation, and v1 is allowed to
         // have a size greater than that of vr
         if (v2.size() < v1.size()) {
            return new ImproperSizeException ("Incompatible dimensions");
         }
      }
      else {
         if (v1.size() != v2.size()) {
            return new ImproperSizeException ("Incompatible dimensions");
         }
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

   Exception addCheck (Vectori vr, Vectori v1, Vectori v2) {
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

   Exception subCheck (Vectori vr, Vectori v1, Vectori v2) {
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

   Exception scaleCheck (Vectori vr, Vectori v1, double scale) {
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

   Exception scaledAddCheck (Vectori vr, double scale, Vectori v1, Vectori v2) {
      // flip v1 and v2 for size check because we're really doing vr = v2 + s v1
      Exception e = checkSizes (vr, v2, v1); 
      if (e != null) {
         return e;
      }
      int[] buf = new int[v2.size()];
      for (int i = 0; i < v2.size(); i++) {
         buf[i] = (int)(scale*v1.get(i)) + v2.get(i);
      }
      vr.set (buf);
      return null;
   }

}
