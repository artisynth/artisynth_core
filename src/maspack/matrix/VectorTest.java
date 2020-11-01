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

class VectorTest {
   static double DOUBLE_PREC = 2.220446049250313e-16;
   static double EPSILON = 10 * DOUBLE_PREC;

   Exception eActual;
   Exception eExpected;

   VectorNd vrsave = new VectorNd (1);

   boolean equals (Vector vr, Vector v1) {
      return false;
   }

   boolean epsilonEquals (Vector vr, Vector v1, double tol) {
      return false;
   }

   VectorNd vx = new VectorNd (1);

   void add (Vector vr, Vector v1, Vector v2) {
   }

   void add (Vector vr, Vector v1) {
   }

   void sub (Vector vr, Vector v1, Vector v2) {
   }

   void sub (Vector vr, Vector v1) {
   }

   void negate (Vector vr) {
   }

   void negate (Vector vr, Vector v1) {
   }

   void scale (Vector vr, double s) {
   }

   void scale (Vector vr, double s, Vector v1) {
   }

   void interpolate (Vector vr, double s, Vector v1) {
   }

   void interpolate (Vector vr, Vector v1, double s, Vector v2) {
   }

   void scaledAdd (Vector vr, double s, Vector v1) {
   }

   void scaledAdd (Vector vr, double s, Vector v1, Vector v2) {
   }

   void combine (Vector vr, double a, Vector v1, double b, Vector v2) {
   }

   double dot (Vector vr, Vector v1) {
      return 0;
   }

   double angle (Vector vr, Vector v1) {
      return 0;
   }

   void normalize (Vector vr) {
   }

   void normalize (Vector vr, Vector v1) {
   }

   void set (Vector vr, Vector v1) {
   }

   void setZero (Vector vr) {
   }

   private String elementFailMessage (String msg, int i) {
      return (msg + "(" + i + ") failed:");
   }

   void checkResult (
      Vector vr, Vector vc, Exception eactual, Exception eexpected,
      double epsilon) {
      MatrixTest.checkExceptions (eactual, eexpected);
      double tol = 0;
      if (epsilon != 0) {
         tol = vr.infinityNorm() * epsilon;
      }
      if (!vr.epsilonEquals (vc, tol)) {
         VectorNd ME = new VectorNd (vr);
         VectorNd v1 = new VectorNd (vc);
         ME.sub (v1);
         ME.absolute();
         throw new TestException ("Expected result:\n"
         + vc.toString (new NumberFormat ("%9.4f")) + "\n" + "Actual result:\n"
         + vr.toString (new NumberFormat ("%9.4f")) + "\n" + "max err: "
         + ME.maxElement() + ", tol=" + tol);
      }
   }

   void checkResult (double res, double chk, double epsilon) {
      MatrixTest.checkExceptions (eActual, eExpected);
      double tol = 0;
      if (epsilon != 0) {
         tol = Math.abs (chk) * epsilon;
      }
      if (Math.abs (res - chk) > tol) {
         throw new TestException ("Expected result:\n" + chk
         + "Actual result:\n" + res + "tol=" + tol);
      }
   }

   protected void saveResult (Vector vr) {
      eActual = null;
      vrsave.set (vr);
   }

   protected void saveExpectedResult (Vector vr) {
      vx.set (vr);
      vr.set (vrsave);
   }

   protected void checkAndRestoreResult (Vector vr) {
      checkResult (vr, vx, eActual, eExpected, 0);
      vr.set (vrsave);
      eActual = null;
   }

   protected void checkAndRestoreResult (Vector vr, double epsilon) {
      checkResult (vr, vx, eActual, eExpected, epsilon);
      vr.set (vrsave);
      eActual = null;
   }

   protected void restoreResult (Vector vr) {
      vr.set (vrsave);
      eActual = null;
   }

   void testGeneric (Vector vr) {
      testSetAndGet (vr);
      testScanAndWrite (vr);
   }

   void testSetZero (Vector vr) {
      vrsave.set (vr);
      setZero (vr);
      for (int i = 0; i < vr.size(); i++) {
         if (vr.get (i) != 0) {
            throw new TestException (elementFailMessage ("setZero", i));
         }
      }
      vr.set (vrsave);
   }

   void testEquals (VectorBase v1, VectorBase v2) {
      VectorNd v1save = new VectorNd (v1);
      VectorNd v2save = new VectorNd (v2);

      v1.setRandom();

      double EPS = 1e-14;
      if (!equals (v1, v1)) {
         throw new TestException ("matrix not equal to itself");
      }
      if (!epsilonEquals (v1, v1, 0)) {
         throw new TestException (
            "matrix not epsilon equal to itself with EPS = 0");
      }
      v2.setRandom();
      for (int i=0; i<v2.size(); i++) {
         v2.set (i, v1.get(i)+EPS*v2.get(i));
      }
      if (!epsilonEquals (v1, v2, EPS)) {
         throw new TestException (
            "matrix not epsilon equal to small perturbation");
      }
      if (epsilonEquals (v1, v2, 0.0001*EPS)) {
         throw new TestException (
            "matrix epsilon equal to small perturbation with very small EPS");
      }
      v2.set (v1);
      // set random entry to NaN
      int i = RandomGenerator.nextInt (0, v1.size()-1);
      v2.set (i, 0.0/0.0);
      if (equals (v1, v2)) {
         throw new TestException ("matrix equal to matrix containing NaN");
      }
      if (epsilonEquals (v1, v2, EPS)) {
         throw new TestException (
            "matrix epsilon equal to matrix containing NaN");
      }      
      v1.set (v1save);
      v2.set (v2save);
   }
         
   void testSetAndGet (Vector vr) {
      Random randGen = RandomGenerator.get();
      int size = vr.size();
      double[] setBuf = new double[size];
      double[] getBuf = new double[size];
      for (int i = 0; i < size; i++) {
         double value = randGen.nextDouble();
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
            throw new TestException ("set(Vector) failed for i=" + i + ": get="
            + getBuf[i] + ", set=" + setBuf[i]);
         }
      }
   }

   void doScanWrite (Vector vr, String fmt, boolean withBrackets) {
      StringWriter sw = new StringWriter();
      try {
         vr.write (new PrintWriter(sw), new NumberFormat(fmt), withBrackets);
         vr.scan (new ReaderTokenizer(new StringReader (sw.toString())));
      }
      catch (Exception e) {
         throw new TestException ("scan/write error: "+e.getMessage());
      }     
   }

   void doScanString (Vector vr, String fmt) {
      try {
         vr.scan (new ReaderTokenizer(new StringReader (vr.toString(fmt))));
      }
      catch (Exception e) {
         throw new TestException ("scan/write error: "+e.getMessage());
      }     
   }

   void testScanAndWrite (Vector vr) {
      saveResult (vr);
      saveExpectedResult (vr);
      eExpected = null;
      eActual = null;
      doScanWrite (vr, "%g", /*brackets=*/false);
      checkAndRestoreResult (vr);
      doScanWrite (vr, "%g", /*brackets=*/true);
      checkAndRestoreResult (vr);
      doScanString (vr, "%g");
      checkAndRestoreResult (vr);
   }

   void testAdd (Vector vr, Vector v1, Vector v2) {
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

   void testScaledAdd (Vector vr, double scale, Vector v1, Vector v2) {
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

   void testSub (Vector vr, Vector v1, Vector v2) {
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

   void testNorms (Vector v1) {
      double expectedNorm;
      expectedNorm = VectorBase.computeOneNorm (v1);
      if (v1.oneNorm() != expectedNorm) {
         throw new TestException ("oneNorm: expected " + expectedNorm
         + ", got " + v1.oneNorm());
      }
      expectedNorm = VectorBase.computeInfinityNorm (v1);
      if (v1.infinityNorm() != expectedNorm) {
         throw new TestException ("infinityNorm: expected " + expectedNorm
         + ", got " + v1.infinityNorm());
      }
      double norm = Math.sqrt (VectorBase.computeNormSquared (v1));
      if (Math.abs (v1.norm() - norm) > EPSILON * norm) {
         throw new TestException ("norm: expected " + norm + ", got "
         + v1.norm());
      }
      double normSquared = VectorBase.computeNormSquared (v1);
      if (Math.abs (v1.normSquared() - normSquared) > EPSILON * normSquared) {
         throw new TestException ("normSquared: expected " + normSquared
         + ", got " + v1.normSquared());
      }
      if (v1.maxElement() != VectorBase.getMaxElement (v1)) {
         throw new TestException ("maxElement: expected "
         + VectorBase.getMaxElement (v1) + ", got " + v1.maxElement());
      }
      if (v1.minElement() != VectorBase.getMinElement (v1)) {
         System.out.println ("v1=" + v1);
         throw new TestException ("minElement: expected "
         + VectorBase.getMinElement (v1) + ", got " + v1.minElement());
      }
   }

   void testNegate (Vector vr, Vector v1) {
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

   void testNormalize (Vector vr, Vector v1) {
      saveResult (vr);
      eExpected = normalizeCheck (vr, v1);
      saveExpectedResult (vr);
      try {
         normalize (vr, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      double norm = vr.norm();
      if (Math.abs (norm - 1) > EPSILON) {
         throw new TestException ("Bad normalize: norm is " + norm
         + ", but should be 1");
      }
      checkAndRestoreResult (vr, EPSILON);
      if (eExpected == null) { // check with normal result
         VectorNd v1save = new VectorNd (v1.size());
         v1save.set (v1);
         v1.set (vx);
         vr.set (v1save);
         normalize (vr, v1);
         checkAndRestoreResult (vr, EPSILON);
         v1.set (v1save);
      }

      vr.set (v1);
      try {
         normalize (vr);
      }
      catch (Exception e) {
         eActual = e;
      }
      norm = vr.norm();
      if (Math.abs (norm - 1) > EPSILON) {
         throw new TestException ("Bad normalize: norm is " + norm
         + ", but should be 1");
      }
      checkAndRestoreResult (vr, EPSILON);
   }

   void testScale (Vector vr, double s, Vector v1) {
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

   void testDotAndAngle (Vector v1, Vector v2) {
      if (v1.size() != v2.size()) {
         eExpected = new ImproperSizeException ("Incompatible dimensions");
      }
      double dExpected = 0;
      for (int i = 0; i < v1.size(); i++) {
         dExpected += v1.get (i) * v2.get (i);
      }
      double d = 0;
      try {
         d = dot (v1, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkResult (d, dExpected, EPSILON);

      double angExpected = Math.acos (dExpected / (v1.norm() * v2.norm()));
      double ang = 0;
      try {
         ang = angle (v1, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkResult (ang, angExpected, EPSILON);
   }

   void testCombine (Vector vr, double a, Vector v1, double b, Vector v2) {
      saveResult (vr);
      eExpected = combineCheck (vr, a, v1, b, v2);
      saveExpectedResult (vr);
      try {
         combine (vr, a, v1, b, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = combineCheck (vr, (1 - a), vr, a, v1);
      saveExpectedResult (vr);
      try {
         interpolate (vr, a, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = combineCheck (vr, (1 - a), v1, a, v2);
      saveExpectedResult (vr);
      try {
         interpolate (vr, v1, a, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = combineCheck (vr, 1, vr, a, v1);
      saveExpectedResult (vr);
      try {
         scaledAdd (vr, a, v1);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);

      eExpected = combineCheck (vr, a, v1, 1, v2);
      saveExpectedResult (vr);
      try {
         scaledAdd (vr, a, v1, v2);
      }
      catch (Exception e) {
         eActual = e;
      }
      checkAndRestoreResult (vr);
   }

   void testSet (Vector vr, Vector v1) {
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

   private Exception checkSizes (Vector vr, Vector v1, Vector v2) {
      if (vr instanceof VectorNd && vr == v1) {
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

   Exception addCheck (Vector vr, Vector v1, Vector v2) {
      Exception e = checkSizes (vr, v1, v2);
      if (e != null) {
         return e;
      }
      double[] buf = new double[v1.size()];
      for (int i = 0; i < v1.size(); i++) {
         buf[i] = v1.get (i) + v2.get (i);
      }
      vr.set (buf);
      return null;
   }

   Exception subCheck (Vector vr, Vector v1, Vector v2) {
      Exception e = checkSizes (vr, v1, v2);
      if (e != null) {
         return e;
      }
      double[] buf = new double[v1.size()];
      for (int i = 0; i < v1.size(); i++) {
         buf[i] = v1.get (i) - v2.get (i);
      }
      vr.set (buf);
      return null;
   }

   Exception scaleCheck (Vector vr, Vector v1, double scale) {
      Exception e = checkSizes (vr, v1, v1);
      if (e != null) {
         return e;
      }
      double[] buf = new double[v1.size()];
      for (int i = 0; i < v1.size(); i++) {
         buf[i] = scale * v1.get (i);
      }
      vr.set (buf);
      return null;
   }

   Exception scaledAddCheck (Vector vr, double scale, Vector v1, Vector v2) {
      // flip size check because we're really doing vr = v2 + s v1
      Exception e = checkSizes (vr, v2, v1);
      if (e != null) {
         return e;
      }
      double[] buf = new double[v2.size()];
      for (int i = 0; i < v2.size(); i++) {
         buf[i] = scale*v1.get(i) + v2.get(i);
      }
      vr.set (buf);
      return null;
   }

   Exception combineCheck (Vector vr, double a, Vector v1, double b, Vector v2) {
      Exception e = checkSizes (vr, v1, v2);
      if (e != null) {
         return e;
      }
      double[] buf = new double[v1.size()];
      for (int i = 0; i < v1.size(); i++) {
         buf[i] = a * v1.get (i) + b * v2.get (i);
      }
      vr.set (buf);
      return null;
   }

   Exception normalizeCheck (Vector vr, Vector v1) {
      Exception e = checkSizes (vr, v1, v1);
      if (e != null) {
         return e;
      }
      double[] buf = new double[v1.size()];
      double norm = Math.sqrt (VectorBase.computeNormSquared (v1));
      for (int i = 0; i < v1.size(); i++) {
         buf[i] = v1.get (i) / norm;
      }
      vr.set (buf);
      return null;
   }
}
