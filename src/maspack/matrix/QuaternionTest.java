/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.*;

import maspack.util.*;

class QuaternionTest extends VectorTest {
   void add (Vector vr, Vector v1) {
      ((Quaternion)vr).add ((Quaternion)v1);
   }

   void add (Vector vr, Vector v1, Vector v2) {
      ((Quaternion)vr).add ((Quaternion)v1, (Quaternion)v2);
   }

   void sub (Vector vr, Vector v1) {
      ((Quaternion)vr).sub ((Quaternion)v1);
   }

   void sub (Vector vr, Vector v1, Vector v2) {
      ((Quaternion)vr).sub ((Quaternion)v1, (Quaternion)v2);
   }

   void negate (Vector vr, Vector v1) {
      ((Quaternion)vr).negate ((Quaternion)v1);
   }

   void negate (Vector vr) {
      ((Quaternion)vr).negate();
   }

   void scale (Vector vr, double s, Vector v1) {
      ((Quaternion)vr).scale (s, (Quaternion)v1);
   }

   void scale (Vector vr, double s) {
      ((Quaternion)vr).scale (s);
   }

   void setZero (Vector vr) {
      ((Quaternion)vr).setZero();
   }

   void interpolate (Vector vr, double s, Vector v1) {
      ((Quaternion)vr).interpolate (s, (Quaternion)v1);
   }

   void interpolate (Vector vr, Vector v1, double s, Vector v2) {
      ((Quaternion)vr).interpolate ((Quaternion)v1, s, (Quaternion)v2);
   }

   void scaledAdd (Vector vr, double s, Vector v1) {
      ((Quaternion)vr).scaledAdd (s, (Quaternion)v1);
   }

   void scaledAdd (Vector vr, double s, Vector v1, Vector v2) {
      ((Quaternion)vr).scaledAdd (s, (Quaternion)v1, (Quaternion)v2);
   }

   void combine (Vector vr, double a, Vector v1, double b, Vector v2) {
      ((Quaternion)vr).combine (a, (Quaternion)v1, b, (Quaternion)v2);
   }

   void normalize (Vector vr) {
      ((Quaternion)vr).normalize();
   }

   void normalize (Vector vr, Vector v1) {
      ((Quaternion)vr).normalize ((Quaternion)v1);
   }

   void set (Vector vr, Vector v1) {
      ((Quaternion)vr).set ((Quaternion)v1);
   }

   void mulCheck (Quaternion qr, Quaternion q1, Quaternion q2) {
      double s = q1.s * q2.s - q1.u.dot (q2.u);
      Vector3d u = new Vector3d();
      u.combine (q1.s, q2.u, q2.s, q1.u);
      u.crossAdd (q1.u, q2.u, u);
      qr.s = s;
      qr.u.set (u);
   }

   void invertCheck (Quaternion qr, Quaternion q1) {
      double d = q1.s * q1.s + q1.u.dot (q1.u);
      qr.s = q1.s / d;
      qr.u.scale (-1 / d, q1.u);
   }

   double angleCheck (Quaternion q1, Quaternion q2) {
      Quaternion q1unit = new Quaternion();
      Quaternion q2unit = new Quaternion();
      Quaternion q1inv = new Quaternion();
      Quaternion qd = new Quaternion();

      q1unit.normalize (q1);
      q2unit.normalize (q2);
      invertCheck (q1inv, q1unit);
      mulCheck (qd, q1inv, q2unit);
      double ang = 2 * Math.atan2 (qd.u.norm(), qd.s);
      if (ang > Math.PI) {
         return 2 * Math.PI - ang;
      }
      else {
         return ang;
      }
   }

   void slerpCheck (Quaternion qr, Quaternion q1, double a, Quaternion q2) {
      Quaternion qd = new Quaternion();
      Quaternion q1inv = new Quaternion();
      Quaternion q1unit = new Quaternion();
      Quaternion q2unit = new Quaternion();
      AxisAngle axisAng = new AxisAngle();

      q1unit.normalize (q1);
      q2unit.normalize (q2);
      invertCheck (q1inv, q1unit);
      mulCheck (qd, q1inv, q2unit);
      axisAng.set (qd);
      axisAng.angle *= a;
      qd.setAxisAngle (axisAng);
      mulCheck (qr, q1unit, qd);
   }

   void testMulAndInverse (Quaternion qr, Quaternion q1, Quaternion q2) {
      Quaternion q1inv = new Quaternion();
      Quaternion q2inv = new Quaternion();

      invertCheck (q1inv, q1);
      invertCheck (q2inv, q2);

      saveResult (qr);
      mulCheck (qr, q1, q2);
      saveExpectedResult (qr);
      qr.mul (q1, q2);
      checkAndRestoreResult (qr, EPSILON);

      mulCheck (qr, qr, q1);
      saveExpectedResult (qr);
      qr.mul (q1);
      checkAndRestoreResult (qr, EPSILON);

      invertCheck (qr, q1);
      saveExpectedResult (qr);
      qr.invert (q1);
      checkAndRestoreResult (qr, EPSILON);

      invertCheck (qr, qr);
      saveExpectedResult (qr);
      qr.invert();
      checkAndRestoreResult (qr, EPSILON);

      qr.set (1, 0, 0, 0);
      saveExpectedResult (qr);
      qr.mulInverseRight (q1, q1);
      checkAndRestoreResult (qr, EPSILON);

      qr.set (1, 0, 0, 0);
      saveExpectedResult (qr);
      qr.mulInverseLeft (q1, q1);
      checkAndRestoreResult (qr, EPSILON);

      mulCheck (qr, qr, q1inv);
      saveExpectedResult (qr);
      qr.mulInverse (q1);
      checkAndRestoreResult (qr, EPSILON);

      mulCheck (qr, q1, q2inv);
      saveExpectedResult (qr);
      qr.mulInverseRight (q1, q2);
      checkAndRestoreResult (qr, EPSILON);

      mulCheck (qr, q1inv, q2);
      saveExpectedResult (qr);
      qr.mulInverseLeft (q1, q2);
      checkAndRestoreResult (qr, EPSILON);

      mulCheck (qr, q1inv, q2inv);
      saveExpectedResult (qr);
      qr.mulInverseBoth (q1, q2);
      checkAndRestoreResult (qr, EPSILON);
   }

   void testSetRotation (Quaternion qr, Quaternion q1) {
      RotationMatrix3d R = new RotationMatrix3d();
      AxisAngle axisAng = new AxisAngle();
      Quaternion q1Unit = new Quaternion();
      Quaternion q1Neg = new Quaternion();

      q1Unit.normalize (q1);
      q1Neg.negate (q1Unit);

      saveResult (qr);
      qr.set (q1);
      saveExpectedResult (qr);
      R.set (q1);
      qr.set (R);
      if (Math.abs (qr.norm() - 1) > EPSILON) {
         throw new TestException ("quaternion set from rotation not normalized");
      }
      if (!q1Unit.epsilonEquals (qr, EPSILON) &&
          !q1Neg.epsilonEquals (qr, EPSILON)) {
         throw new TestException ("expecting:\n" + q1Unit.toString ("%9.4f")
         + " (or its negative)\n" + "got:\n" + qr.toString ("%9.4f"));
      }
      restoreResult (qr);

      qr.set (q1);
      saveExpectedResult (qr);
      axisAng.set (q1);
      qr.setAxisAngle (axisAng);
      if (Math.abs (qr.norm() - 1) > EPSILON) {
         throw new TestException (
            "quaternion set from axisAngle not normalized");
      }
      if (!q1Unit.epsilonEquals (qr, EPSILON) &&
          !q1Neg.epsilonEquals (qr, EPSILON)) {
         throw new TestException ("expecting:\n" + q1Unit.toString ("%9.4f")
         + " (or its negative)\n" + "got:\n" + qr.toString ("%9.4f"));
      }
      restoreResult (qr);
   }

   void testSphericalInterpolation (
      Quaternion qr, Quaternion q1, double a, Quaternion q2) {
      Quaternion qcheck = new Quaternion();
      Quaternion qcheckNeg = new Quaternion();

      double ang = angleCheck (q1, q2);
      if (Math.abs (ang - q1.rotationAngle (q2)) > EPSILON) {
         throw new TestException ("expecting rotation angle: " + ang + "\n"
         + "got: " + q1.rotationAngle (q2));
      }

      Quaternion q1scaled = new Quaternion (q1);
      Quaternion q2scaled = new Quaternion (q2);
      q1scaled.scale (12.34);
      q2scaled.scale (0.045);

      saveResult (qr);
      slerpCheck (qcheck, q1, a, q2);
      qcheckNeg.negate (qcheck);
      qr.sphericalInterpolate (q1, a, q2);
      checkUnitQuaternion ("sphericalInterpolation:", qr, qcheck);
      // qr.sphericalInterpolate (q1scaled, a, q2scaled);
      // checkUnitQuaternion ("sphericalInterpolation:", qr, qcheck);
      restoreResult (qr);
   }

   private double rangeAngle (double ang) {
      while (ang > Math.PI) {
         ang -= 2*Math.PI;
      }
      while (ang <= -Math.PI) {
         ang += 2*Math.PI;
      }
      return ang;
   }

   private void checkOutput (
      String msg, Quaternion qr, Quaternion qcheck,
      Vector3d wr, Vector3d wcheck, double eps){
      
      if (wcheck != null) {
         if (!wcheck.epsilonEquals (wr, eps)) {
            throw new TestException (
               msg + " Bad angular velocity. Expecting:\n" +
               wcheck.toString ("%9.4f") + "\ngot:\n" + wr.toString ("%9.4f"));
         }
      }
      if (qcheck != null) {
         Quaternion qcheckNeg = new Quaternion();
         qcheckNeg.negate (qcheck);
         if (!qcheck.epsilonEquals (qr, eps) &&
             !qcheckNeg.epsilonEquals (qr, eps)) {
            throw new TestException (
               msg + " Bad rotation. Expecting:\n" + qcheck.toString ("%9.4f")
               + " (or its negative),\ngot:\n" + qr.toString ("%9.4f"));
         }
      }
   }

   private void computeHermite (
      Quaternion qr, Quaternion q0, Vector3d w0, Quaternion q1, Vector3d w1,
      double s, double h, boolean global) {

      Quaternion qa = new Quaternion();
      Quaternion qb = new Quaternion();
      Quaternion qx = new Quaternion();

      if (global) {
         qa.extrapolateWorld (q0, w0, h/3);
         qb.extrapolateWorld (q1, w1, -h/3);
      }
      else {
         qa.extrapolateLocal (q0, w0, h/3);
         qb.extrapolateLocal (q1, w1, -h/3);
      }
      qx.mulInverseLeft (q0, qa);
      qx.sphericalInterpolate (Quaternion.IDENTITY, s*s*s-3*s*s+3*s, qx);
      qr.mul (q0, qx);

      qx.mulInverseLeft (qa, qb);
      qx.sphericalInterpolate (Quaternion.IDENTITY, -2*s*s*s + 3*s*s, qx);
      qr.mul (qx);

      qx.mulInverseLeft (qb, q1);
      qx.sphericalInterpolate (Quaternion.IDENTITY, s*s*s, qx);
      qr.mul (qx);
   }

   /** 
    * Test sphericalHermite interpolation about a specific axis. This allows us
    * to check the results against regular Hermite interpolation.
    */
   void testSphericalHermite (int cnt, Vector3d axis, double s, double h) {
      Random rand = RandomGenerator.get();

      double ang0 = 2*Math.PI*(rand.nextDouble()-0.5);
      double ang1 = ang0 + Math.PI*(rand.nextDouble()-0.5);
      double vel0, vel1;

      vel0 = rangeAngle(ang1-ang0)/h + 0.5*Math.PI*(rand.nextDouble()-0.5)/h;
      vel1 = rangeAngle(ang1-ang0)/h + 0.5*Math.PI*(rand.nextDouble()-0.5)/h;

      // test with special cases:

      switch (cnt) {
         case 0: {
            vel0 = 0;
            break;
         }
         case 1: {
            vel1 = 0;
            break;
         }
         case 2: {
            vel0 = vel1 = 0;
            break;
         }
         case 3: {
            ang0 = ang1;
            break;
         }
         case 4: {
            ang0 = ang1;
            vel0 = vel1 = 0;
            break;
         }
      }
      
      Vector3d unit = new Vector3d (axis);
      unit.normalize();

      Vector3d w0 = new Vector3d();
      w0.scale (vel0, unit);
      Quaternion q0 = new Quaternion (new AxisAngle (unit, ang0));
      Vector3d w1 = new Vector3d();
      w1.scale (vel1, unit);
      Quaternion q1 = new Quaternion (new AxisAngle (unit, ang1));

      Vector3d wr = new Vector3d();
      Quaternion qr = new Quaternion();

      double b1 = (2*s-3)*s*s;
      double b2 = ((s-2)*s+1)*s*h;
      double b3 = (s-1)*s*s*h;
      double ang = b1*(ang0-ang1) + b2*vel0 + b3*vel1 + ang0;

      Quaternion qcheck = new Quaternion (new AxisAngle (unit, ang));
      Quaternion qcheckNeg = new Quaternion();
      qcheckNeg.negate (qcheck);

      double c1 = 6*(s-1)*s/h;
      double c2 = (3*s-4)*s+1;
      double c3 = (3*s-2)*s;
      double vel = c1*(ang0-ang1) + c2*vel0 + c3*vel1;

      Vector3d wcheck = new Vector3d();
      wcheck.scale (vel, unit);

      // pre-transform everything by a base rotation, to make sure
      // that the local/global stuff is working correctly
      Quaternion qbase = new Quaternion();
      qbase.setRandomUnit();      
      q0.mul (qbase, q0);
      q1.mul (qbase, q1);
      qcheck.mul (qbase, qcheck);
      qcheckNeg.mul (qbase, qcheckNeg);

      Quaternion.sphericalHermiteLocal (qr, wr, q0, w0, q1, w1, s, h);
      checkOutput ("HermiteLocal:", qr, qcheck, wr, wcheck, 10*EPSILON);

      q0.transform (w0, w0);
      q1.transform (w1, w1);
      Quaternion.sphericalHermiteGlobal (qr, wr, q0, w0, q1, w1, s, h);
      qr.transform (wcheck, wcheck);

      checkOutput ("HermiteGlobal:", qr, qcheck, wr, wcheck, 10*EPSILON);

      // Now try arbitrary end-point velocities (i.e., velocities
      // not aligned with the rotation axis between q0 and q1.
      // We test the derivatives numerically.

      vel0 = 0.5*Math.PI*(rand.nextDouble()-0.5)/h;
      vel1 = 0.5*Math.PI*(rand.nextDouble()-0.5)/h;

      w0.setRandom();
      w0.scale (vel0/w0.norm());
      w1.setRandom();
      w1.scale (vel1/w1.norm());

      Quaternion qx = new Quaternion();
      double delta_s = 1e-8;

      Quaternion.sphericalHermiteGlobal (qr, wr, q0, w0, q1, w1, s, h);
      Quaternion.sphericalHermiteGlobal (qx, null, q0, w0, q1, w1, s+delta_s, h);
      computeHermite (qcheck, q0, w0, q1, w1, s, h, /*global=*/true);

      qx.sub (qr);
      qx.scale (1/(delta_s*h));
      qx.mulInverseRight (qx, qr);
      wcheck.scale (2, qx.u);

      checkOutput ("HermiteGlobal, general vel:",
                   qr, qcheck, wr, wcheck, 1e-6);

      Quaternion.sphericalHermiteLocal (qr, wr, q0, w0, q1, w1, s, h);
      Quaternion.sphericalHermiteLocal (qx, null, q0, w0, q1, w1, s+delta_s, h);
      computeHermite (qcheck, q0, w0, q1, w1, s, h, /*global=*/false);

      qx.sub (qr);
      qx.scale (1/(delta_s*h));
      qx.mulInverseLeft (qr, qx);
      wcheck.scale (2, qx.u);

      checkOutput ("HermiteLocal, general vel:",
                   qr, qcheck, wr, wcheck, 1e-6);
   }

   void checkUnitQuaternion (String msg, Quaternion qr, Quaternion qcheck) {
      Quaternion qcheckNeg = new Quaternion();
      qcheckNeg.negate (qcheck);
      if (Math.abs (qr.norm() - 1) > EPSILON) {
         throw new TestException (
            msg + " result not normalized");
      }
      if (!qcheck.epsilonEquals (qr, EPSILON) &&
          !qcheckNeg.epsilonEquals (qr, EPSILON)) {
         throw new TestException (msg + " expecting:\n"+qcheck.toString ("%9.4f")
         + " (or its negative)\n" + "got:\n" + qr.toString ("%9.4f"));
      }
   }

   void testExtrapolation (
      Quaternion qr, Quaternion q1, Vector3d w, double t) {

      Quaternion q1Unit = new Quaternion();
      Quaternion qcheck = new Quaternion();
      Quaternion qang = new Quaternion ();
      Quaternion qx = new Quaternion();

      double c2 = Math.cos (w.norm()*t/2);
      double s2 = Math.sin (w.norm()*t/2);
      Vector3d u = new Vector3d();
      u.normalize (w);
      u.scale (s2);
      qang.set (c2, u);

      qx.scale (12.3, q1);

      saveResult (qr);
      q1Unit.normalize (q1);
      qcheck.mul (q1Unit, qang);
      qr.extrapolateLocal (q1, w, t);
      checkUnitQuaternion ("extrapolateLocal:", qr, qcheck);
      qr.extrapolateLocal (qx, w, t);
      checkUnitQuaternion ("extrapolateLocal:", qr, qcheck);
      restoreResult (qr);

      saveResult (qr);
      qcheck.mul (qang, q1Unit);
      qr.extrapolateWorld (q1, w, t);
      checkUnitQuaternion ("extrapolateWorld:", qr, qcheck);
      qr.extrapolateWorld (qx, w, t);
      checkUnitQuaternion ("extrapolateWorld:", qr, qcheck);
      restoreResult (qr);
   }

   /** 
    * Test sphericalBezier interpolation about a specific axis. This allows us
    * to check the results against regular Hermite interpolation.
    */
   void testSphericalBezier (int cnt, Vector3d axis, double s, double h) {
      Random rand = RandomGenerator.get();

      double ang0 = 2*Math.PI*(rand.nextDouble()-0.5);
      double ang1 = ang0 + Math.PI*(rand.nextDouble()-0.5);
      double vel0, vel1;

      vel0 = rangeAngle(ang1-ang0)/h + 0.5*Math.PI*(rand.nextDouble()-0.5)/h;
      vel1 = rangeAngle(ang1-ang0)/h + 0.5*Math.PI*(rand.nextDouble()-0.5)/h;

      Quaternion qa = new Quaternion();
      Quaternion qb = new Quaternion();

      Vector3d unit = new Vector3d (axis);
      unit.normalize();

      Vector3d w0 = new Vector3d();
      w0.scale (vel0, unit);
      Quaternion q0 = new Quaternion (new AxisAngle (unit, ang0));
      Vector3d w1 = new Vector3d();
      w1.scale (vel1, unit);
      Quaternion q1 = new Quaternion (new AxisAngle (unit, ang1));

      qa.extrapolateWorld (q0, w0, h/3);
      qb.extrapolateWorld (q1, w1, -h/3);

      Quaternion qr = new Quaternion();

      double b1 = (2*s-3)*s*s;
      double b2 = ((s-2)*s+1)*s*h;
      double b3 = (s-1)*s*s*h;
      double ang = b1*(ang0-ang1) + b2*vel0 + b3*vel1 + ang0;

      Quaternion qcheck = new Quaternion (new AxisAngle (unit, ang));

      Quaternion.sphericalBezierShoemake (qr, q0, qa, qb, q1, s);
      checkUnitQuaternion ("sphericalBezierShoemake:", qr, qcheck);

      Quaternion.sphericalBezier (qr, q0, qa, qb, q1, s);
      checkUnitQuaternion ("sphericalBezier:", qr, qcheck);
   }

   void testTransform (
      Vector3d vr, Quaternion q1, Vector3d v1) {

      Vector3d vcheck = new Vector3d();
      RotationMatrix3d R = new RotationMatrix3d();

      R.set (q1);
      vcheck.transform (R, v1);

      saveResult (vr);
      q1.transform (vr, v1);
      if (!vcheck.epsilonEquals (vr, EPSILON)) {
         throw new TestException ("expecting:\n" + vcheck.toString ("%9.4f")
         + "\ngot:\n" + vr.toString ("%9.4f"));
      }
      restoreResult (vr);

      vcheck.inverseTransform (R, v1);
      saveResult (vr);
      q1.inverseTransform (vr, v1);
      if (!vcheck.epsilonEquals (vr, EPSILON)) {
         throw new TestException ("expecting:\n" + vcheck.toString ("%9.4f")
         + "\ngot:\n" + vr.toString ("%9.4f"));
      }
      restoreResult (vr);
   }

   public void execute() {
      Quaternion vr = new Quaternion();
      Quaternion v1 = new Quaternion();
      Quaternion v2 = new Quaternion();
      Vector3d w = new Vector3d();
      Vector3d ur = new Vector3d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (v1);
      testSetZero (vr);

      Random randGen = RandomGenerator.get();

      for (int i=0; i<100; i++) {
         AxisAngle axisAng = new AxisAngle();
         axisAng.angle = randGen.nextDouble() - 0.5;
         axisAng.axis.setRandom();
         double c = Math.cos(axisAng.angle/2);
         double s = Math.sin(axisAng.angle/2);
         Vector3d u = new Vector3d(axisAng.axis);
         u.normalize ();
         u.scale (s);
         v1.set (c, u);
         w.setRandom();
         testTransform (ur, v1, w);
      }

      for (int cnt = 0; cnt < 100; cnt++) {
         v1.setRandom();
         v2.setRandom();
         vr.setRandom();
         ur.setRandom();
         w.setRandom();

         testAdd (vr, v1, v2);
         testAdd (vr, vr, vr);

         testSub (vr, v1, v2);
         testSub (vr, vr, vr);

         testNegate (vr, v1);
         testNegate (vr, vr);

         testScale (vr, 1.23, v1);
         testScale (vr, 1.23, vr);

         testSet (vr, v1);
         testSet (vr, vr);

         testNormalize (vr, v1);
         testNormalize (vr, vr);

         testCombine (vr, 0.123, v1, 0.677, v2);
         testCombine (vr, 0.123, vr, 0.677, vr);

         testNorms (v1);

         testMulAndInverse (vr, v1, v2);
         testSetRotation (vr, v1);

         double a = RandomGenerator.get().nextDouble();
         testSphericalInterpolation (vr, v1, a, v2);
         testSphericalInterpolation (vr, v1, a, v1);
         v1.normalize();
         v2.normalize();
         testSphericalInterpolation (vr, v1, a, v2);
         testSphericalInterpolation (vr, vr, a, v2);
         testSphericalInterpolation (vr, v1, a, vr);

         testExtrapolation (vr, v1, w, 0);
         testExtrapolation (vr, v1, w, 1.1);
         testExtrapolation (vr, v1, w, -0.3);
         testExtrapolation (vr, vr, w, 1.4);

         testTransform (ur, v1, w);
         testTransform (ur, v1, ur);

         testSphericalHermite (cnt, Vector3d.X_UNIT, 0, 1);
         testSphericalHermite (cnt, Vector3d.X_UNIT, 1, 1);
         testSphericalHermite (cnt, Vector3d.X_UNIT, 0.5, 1);
         testSphericalHermite (cnt, Vector3d.X_UNIT, 0.1, 1);
         testSphericalHermite (cnt, Vector3d.X_UNIT, 0.9, 1);

         testSphericalHermite (cnt, Vector3d.X_UNIT, 0.5, 0.2);
         testSphericalHermite (cnt, Vector3d.X_UNIT, 0.1, 0.3);
 
         testSphericalHermite (cnt, new Vector3d (1,1,1), 0, 0.2);
         testSphericalHermite (cnt, new Vector3d (1,2,3), 1, 0.2);

         testSphericalHermite (cnt, new Vector3d (1,1,1), 0.5, 0.2);
         testSphericalHermite (cnt, new Vector3d (1,2,3), 0.1, 0.3);
         testSphericalHermite (cnt, new Vector3d (1,2,3), 0.9, 0.3);

         testSphericalBezier (cnt, new Vector3d(1, 0, 0), 0, 2);
         testSphericalBezier (cnt, new Vector3d(1, 0, 0), 1, 2);
         testSphericalBezier (cnt, new Vector3d(1, 0, 0), 0.5, 1);
         testSphericalBezier (cnt, new Vector3d(1, 0, 0), 0.1, 1);
      }

   }

   public static void main (String[] args) {
      QuaternionTest test = new QuaternionTest();

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
