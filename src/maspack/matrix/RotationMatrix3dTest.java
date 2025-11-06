/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.ArrayList;

import maspack.util.RandomGenerator;
import maspack.util.FunctionTimer;
import maspack.util.TestException;

class RotationMatrix3dTest extends MatrixTest {

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   void mul (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).mul ((RotationMatrix3d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((RotationMatrix3d)MR).mul ((RotationMatrix3d)M1, (RotationMatrix3d)M2);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).mulInverse ((RotationMatrix3d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((RotationMatrix3d)MR).mulInverseRight (
         (RotationMatrix3d)M1, (RotationMatrix3d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((RotationMatrix3d)MR).mulInverseLeft (
         (RotationMatrix3d)M1, (RotationMatrix3d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((RotationMatrix3d)MR).mulInverseBoth (
         (RotationMatrix3d)M1, (RotationMatrix3d)M2);
   }

   void invert (Matrix MR) {
      ((RotationMatrix3d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).invert ((RotationMatrix3d)M1);
   }

   void transpose (Matrix MR) {
      ((RotationMatrix3d)MR).transpose();
   }

   void transpose (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).transpose ((RotationMatrix3d)M1);
   }

   void negate (Matrix MR) {
      ((RotationMatrix3d)MR).negate();
   }

   void negate (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).negate ((RotationMatrix3d)M1);
   }

   void set (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).set ((RotationMatrix3d)M1);
   }

   void testSetRotations (RotationMatrix3d RR, RotationMatrix3d R1) {
      AxisAngle axisAng = new AxisAngle();
      Quaternion quat = new Quaternion();
      double[] angs = new double[3];
      double[] singAngs = new double[3];

      saveResult (RR);
      MX.set (R1);
      R1.getRpy (angs);
      RR.setRpy (angs);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getEuler (angs);
      RR.setEuler (angs);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getZyzAngles (angs);
      RR.setZyzAngles (angs);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getYxyAngles (angs);
      RR.setYxyAngles (angs);
      checkAndRestoreResult (RR, EPSILON);

      RotationMatrix3d RS = new RotationMatrix3d();
      angs[0] = 0;
      angs[1] = 0;
      RS.setZyzAngles (angs);
      RS.getZyzAngles (singAngs);
      checkEquals ("singular z-y-z angles", singAngs, angs, EPSILON);

      RS.setYxyAngles (angs);
      RS.getYxyAngles (singAngs);
      checkEquals ("singular y-x-y angles", singAngs, angs, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getXyzAngles (angs);
      RR.setXyzAngles (angs);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getZyxAngles (angs);
      RR.setZyxAngles (angs);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getAxisAngle (axisAng);
      RR.setAxisAngle (axisAng);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getAxisAngle (axisAng);
      quat.set (R1);
      axisAng.set (quat);
      quat.setAxisAngle (axisAng);
      RR.set (quat);
      RR.getAxisAngle (axisAng);
      checkAndRestoreResult (RR, EPSILON);
   }

   void setZDirectionCheck (RotationMatrix3d RR, Vector3d zdir) {
      Vector3d xcol = new Vector3d();
      Vector3d ycol = new Vector3d();
      Vector3d zcol = new Vector3d();

      if (zdir.norm() == 0) {
         RR.setIdentity();
      }
      else {
         zcol.normalize (zdir);
         double axis_x = -zcol.y;
         double axis_y = zcol.x;
         double len = Math.sqrt (axis_x * axis_x + axis_y * axis_y);
         double ang = Math.atan2 (len, zcol.z);
         if (len != 0) {
            RR.setAxisAngle (axis_x / len, axis_y / len, 0, ang);
            RR.getColumn (0, xcol);
         }
         else {
            xcol.set (1, 0, 0);
         }
         ycol.cross (zcol, xcol);
         RR.setColumn (0, xcol);
         RR.setColumn (1, ycol);
         RR.setColumn (2, zcol);
      }
   }

   void testSetZDirection (RotationMatrix3d RR, Vector3d zdir) {
      saveResult (RR);
      setZDirectionCheck (RR, zdir);
      MX.set (RR);
      RR.setZDirection (zdir);
      checkAndRestoreResult (RR, EPSILON);
   }

   void setYDirectionCheck (RotationMatrix3d RR, Vector3d ydir) {
      Vector3d xcol = new Vector3d();
      Vector3d ycol = new Vector3d();
      Vector3d zcol = new Vector3d();

      if (ydir.norm() == 0) {
         RR.setIdentity();
      }
      else {
         ycol.normalize (ydir);
         double axis_z = -ycol.x;
         double axis_x = ycol.z;
         double len = Math.sqrt (axis_z * axis_z + axis_x * axis_x);
         double ang = Math.atan2 (len, ycol.y);
         if (len != 0) {
            RR.setAxisAngle (axis_x / len, 0, axis_z / len, ang);
            RR.getColumn (2, zcol);
         }
         else {
            zcol.set (0, 0, 1);
         }
         xcol.cross (ycol, zcol);
         RR.setColumn (0, xcol);
         RR.setColumn (1, ycol);
         RR.setColumn (2, zcol);
      }
   }

   void testSetYDirection (RotationMatrix3d RR, Vector3d ydir) {
      saveResult (RR);
      setYDirectionCheck (RR, ydir);
      MX.set (RR);
      RR.setYDirection (ydir);
      checkAndRestoreResult (RR, EPSILON);
   }

   void setXDirectionCheck (RotationMatrix3d RR, Vector3d xdir) {
      Vector3d xcol = new Vector3d();
      Vector3d ycol = new Vector3d();
      Vector3d zcol = new Vector3d();

      if (xdir.norm() == 0) {
         RR.setIdentity();
      }
      else {
         xcol.normalize (xdir);
         double axis_y = -xcol.z;
         double axis_z = xcol.y;
         double len = Math.sqrt (axis_y * axis_y + axis_z * axis_z);
         double ang = Math.atan2 (len, xcol.x);
         if (len != 0) {
            RR.setAxisAngle (0, axis_y / len, axis_z / len, ang);
            RR.getColumn (1, ycol);
         }
         else {
            ycol.set (0, 1, 0);
         }
         zcol.cross (xcol, ycol);
         RR.setColumn (0, xcol);
         RR.setColumn (1, ycol);
         RR.setColumn (2, zcol);
      }
   }

   void testSetXDirection (RotationMatrix3d RR, Vector3d xdir) {
      saveResult (RR);
      setXDirectionCheck (RR, xdir);
      MX.set (RR);
      RR.setXDirection (xdir);
      checkAndRestoreResult (RR, EPSILON);
   }

   void setXYDirectionsCheck (RotationMatrix3d RR, Vector3d xdir, Vector3d ydir) {
      Vector3d xcol = new Vector3d();
      Vector3d ycol = new Vector3d();
      Vector3d zcol = new Vector3d();

      xcol.normalize (xdir);
      zcol.cross (xdir, ydir);
      zcol.normalize();
      ycol.cross (zcol, xcol);

      RR.setColumn (0, xcol);
      RR.setColumn (1, ycol);
      RR.setColumn (2, zcol);
   }

   void testSetXYDirections (RotationMatrix3d RR, Vector3d xdir, Vector3d ydir) {
      saveResult (RR);
      setXYDirectionsCheck (RR, xdir, ydir);
      MX.set (RR);
      RR.setXYDirections (xdir, ydir);
      checkAndRestoreResult (RR, EPSILON);
   }

   void setYZDirectionsCheck (RotationMatrix3d RR, Vector3d ydir, Vector3d zdir) {
      Vector3d xcol = new Vector3d();
      Vector3d ycol = new Vector3d();
      Vector3d zcol = new Vector3d();

      ycol.normalize (ydir);
      xcol.cross (ydir, zdir);
      xcol.normalize();
      zcol.cross (xcol, ycol);

      RR.setColumn (0, xcol);
      RR.setColumn (1, ycol);
      RR.setColumn (2, zcol);
   }

   void testSetYZDirections (RotationMatrix3d RR, Vector3d ydir, Vector3d zdir) {
      saveResult (RR);
      setYZDirectionsCheck (RR, ydir, zdir);
      MX.set (RR);
      RR.setYZDirections (ydir, zdir);
      checkAndRestoreResult (RR, EPSILON);
   }

   void setZXDirectionsCheck (RotationMatrix3d RR, Vector3d zdir, Vector3d xdir) {
      Vector3d xcol = new Vector3d();
      Vector3d ycol = new Vector3d();
      Vector3d zcol = new Vector3d();

      zcol.normalize (zdir);
      ycol.cross (zdir, xdir);
      ycol.normalize();
      xcol.cross (ycol, zcol);

      RR.setColumn (0, xcol);
      RR.setColumn (1, ycol);
      RR.setColumn (2, zcol);
   }

   void testSetZXDirections (RotationMatrix3d RR, Vector3d zdir, Vector3d xdir) {
      saveResult (RR);
      setZXDirectionsCheck (RR, zdir, xdir);
      MX.set (RR);
      RR.setZXDirections (zdir, xdir);
      checkAndRestoreResult (RR, EPSILON);
   }

   void testMulRot (RotationMatrix3d RR) {
      RotationMatrix3d R0 = new RotationMatrix3d(RR);
      RotationMatrix3d RX = new RotationMatrix3d();
      saveResult (RR);

      RX.set (R0);
      RX.mulRotX (Math.PI/2);
      MX.set (RX);
      RR.mulRotX90();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotX (Math.PI);
      MX.set (RX);
      RR.mulRotX180();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotX (3*Math.PI/2);
      MX.set (RX);
      RR.mulRotX270();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotY (Math.PI/2);
      MX.set (RX);
      RR.mulRotY90();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotY (Math.PI);
      MX.set (RX);
      RR.mulRotY180();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotY (3*Math.PI/2);
      MX.set (RX);
      RR.mulRotY270();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotZ (Math.PI/2);
      MX.set (RX);
      RR.mulRotZ90();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotZ (Math.PI);
      MX.set (RX);
      RR.mulRotZ180();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotZ (3*Math.PI/2);
      MX.set (RX);
      RR.mulRotZ270();
      checkAndRestoreResult (RR, EPSILON);
   }

   void testNormalize (RotationMatrix3d RR) {
      saveResult (RR);
      MX.set (RR);
      RR.normalize();

      checkAndRestoreResult (RR, EPSILON);
   }

   void testPreciseAxisAngleSet () {
      AxisAngle axisAng = new AxisAngle();
      RotationMatrix3d R = new RotationMatrix3d();
      RotationMatrix3d RX = new RotationMatrix3d();

      axisAng.set (1, 0, 0, 0);
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (1, 0, 0, Math.toRadians(90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX90();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (1, 0, 0, Math.toRadians(180));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX180();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (1, 0, 0, Math.toRadians(270));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (1, 0, 0, Math.toRadians(-90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (-1, 0, 0, Math.toRadians(90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX270();
      checkResult (R, RX, "setAxisAnglePrecise");
      

      axisAng.set (0, 1, 0, Math.toRadians(90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotY90();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 1, 0, Math.toRadians(180));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotY180();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 1, 0, Math.toRadians(270));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotY270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 1, 0, Math.toRadians(-90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotY270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 0, 1, Math.toRadians(90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotZ90();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 0, 1, Math.toRadians(180));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotZ180();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 0, 1, Math.toRadians(270));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotZ270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 0, 1, Math.toRadians(-90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotZ270();
      checkResult (R, RX, "setAxisAnglePrecise");
   }

   static ArrayList<double[]> specialAngles = new ArrayList<>();
   static {
      specialAngles.add (new double[] {0, 0, 0});
      specialAngles.add (new double[] {0, Math.PI/2, 0});
      specialAngles.add (new double[] {0, -Math.PI/2, 0});
   }
   
   void testRpySolutions() {
      int ntests = 100;

      RotationMatrix3d R = new RotationMatrix3d();
      RotationMatrix3d RX = new RotationMatrix3d();
      VectorNd rpy = new VectorNd(3);
      VectorNd alt = new VectorNd(3);
      VectorNd out = new VectorNd(3);

      for (int i=0; i<ntests; i++) {
         // tests within canonical range
         rpy.set(0, RandomGenerator.nextDouble(-Math.PI, Math.PI));
         rpy.set(1, RandomGenerator.nextDouble(-Math.PI/2, Math.PI/2));
         rpy.set(2, RandomGenerator.nextDouble(-Math.PI, Math.PI));
         R.setRpy (rpy.getBuffer());

         // check alternate solution
         alt.set(0, rpy.get(0) + Math.PI);
         alt.set(1, Math.PI - rpy.get(1));
         alt.set(2, rpy.get(2) + Math.PI);
         RX.setRpy (alt.getBuffer());
         checkEquals ("testRpySolutions", R, RX, 1e-13);

         R.getRpy (out.getBuffer());
         if (!out.epsilonEquals (rpy, 1e-14)) {
            throw new TestException (
               "set/getRpy, canonical range: got "+out+", expected\n" + rpy);
         }
      }
      for (double[] angs : specialAngles) {
         rpy.set(angs);
         R.setRpy (angs);
         R.getRpy (out.getBuffer());
         if (!out.epsilonEquals (rpy, 1e-14)) {
            throw new TestException (
               "set/getRpy, specials: got "+out+", expected\n" + rpy);
         }        
      }
      for (int i=0; i<ntests; i++) {
         // tests outside canonical range
         rpy.set(0, RandomGenerator.nextDouble(-4*Math.PI, 4*Math.PI));
         rpy.set(1, RandomGenerator.nextDouble(-4*Math.PI, 4*Math.PI));
         rpy.set(2, RandomGenerator.nextDouble(-4*Math.PI, 4*Math.PI));
         R.setRpy (rpy.getBuffer());      

         R.getZyxAngles (out.getBuffer(), rpy.getBuffer(), 0, 1.0);
         if (!out.epsilonEquals (rpy, 1e-14)) {
            throw new TestException (
               "set/getRpy, extended range: got "+out+", expected\n" + rpy);
         }
         rpy.scale (RTOD);
         R.getZyxAngles (out.getBuffer(), rpy.getBuffer(), 0, RTOD);
         if (!out.epsilonEquals (rpy, RTOD*1e-14)) {
            throw new TestException (
               "set/getRpy, scale ext. range: got "+out+", expected\n" + rpy);
         }
      }
      for (int i=0; i<ntests; i++) {
         // singularity tests
         rpy.set(0, RandomGenerator.nextDouble(-2*Math.PI, 2*Math.PI));
         int n = RandomGenerator.nextInt (-3, 2);
         rpy.set(1, Math.PI/2 + n*Math.PI);
         rpy.set(2, RandomGenerator.nextDouble(-2*Math.PI, 2*Math.PI));
         R.setRpy (rpy.getBuffer());      

         R.getZyxAngles (out.getBuffer(), rpy.getBuffer(), 0, 1.0);
         if (!out.epsilonEquals (rpy, 1e-14)) {
            throw new TestException (
               "set/getRpy, singularites: got "+out+", expected\n" + rpy);
         }
         rpy.scale (RTOD);
         R.getZyxAngles (out.getBuffer(), rpy.getBuffer(), 0, RTOD);
         if (!out.epsilonEquals (rpy, RTOD*1e-14)) {
            throw new TestException (
               "set/getRpy, scale singularities: got "+out+", expected\n" + rpy);
         }
      }
   }

   void testXyzSolutions() {
      int ntests = 100;

      RotationMatrix3d R = new RotationMatrix3d();
      RotationMatrix3d RX = new RotationMatrix3d();
      VectorNd xyz = new VectorNd(3);
      VectorNd alt = new VectorNd(3);
      VectorNd out = new VectorNd(3);

      for (int i=0; i<ntests; i++) {
         // tests within canonical range
         xyz.set(0, RandomGenerator.nextDouble(-Math.PI, Math.PI));
         xyz.set(1, RandomGenerator.nextDouble(-Math.PI/2, Math.PI/2));
         xyz.set(2, RandomGenerator.nextDouble(-Math.PI, Math.PI));
         R.setXyz (xyz.getBuffer());

         // check alternate solution
         alt.set(0, xyz.get(0) + Math.PI);
         alt.set(1, Math.PI - xyz.get(1));
         alt.set(2, xyz.get(2) + Math.PI);
         RX.setXyz (alt.getBuffer());
         checkEquals ("testRpySolutions", R, RX, 1e-13);

         R.getXyz (out.getBuffer());
         if (!out.epsilonEquals (xyz, 1e-14)) {
            throw new TestException (
               "set/getRpy, canonical range: got "+out+", expected\n" + xyz);
         }
      }
      for (double[] angs : specialAngles) {
         xyz.set(angs);
         R.setXyz (angs);
         R.getXyz (out.getBuffer());
         if (!out.epsilonEquals (xyz, 1e-14)) {
            throw new TestException (
               "set/getRpy, specials: got "+out+", expected\n" + xyz);
         }        
      }      
      for (int i=0; i<ntests; i++) {
         // tests outside canonical range
         xyz.set(0, RandomGenerator.nextDouble(-2*Math.PI, 2*Math.PI));
         xyz.set(1, RandomGenerator.nextDouble(-2*Math.PI, 2*Math.PI));
         xyz.set(2, RandomGenerator.nextDouble(-2*Math.PI, 2*Math.PI));
         R.setXyz (xyz.getBuffer());      

         R.getXyzAngles (out.getBuffer(), xyz.getBuffer(), 0, 1.0);
         if (!out.epsilonEquals (xyz, 1e-14)) {
            throw new TestException (
               "set/getXyz, extended range: got "+out+", expected\n" + xyz);
         }
         xyz.scale (RTOD);
         R.getXyzAngles (out.getBuffer(), xyz.getBuffer(), 0, RTOD);
         if (!out.epsilonEquals (xyz, RTOD*1e-14)) {
            throw new TestException (
               "set/getXyz, scaled ext. range: got "+out+", expected\n" + xyz);
         }
      }
      for (int i=0; i<ntests; i++) {
         // singularity tests
         xyz.set(0, RandomGenerator.nextDouble(-2*Math.PI, 2*Math.PI));
         int n = RandomGenerator.nextInt (-3, 2);
         xyz.set(1, Math.PI/2 + n*Math.PI);
         xyz.set(2, RandomGenerator.nextDouble(-2*Math.PI, 2*Math.PI));
         R.setXyz (xyz.getBuffer());      

         R.getXyzAngles (out.getBuffer(), xyz.getBuffer(), 0, 1.0);
         if (!out.epsilonEquals (xyz, 1e-14)) {
            throw new TestException (
               "set/getXyz, singularites: got "+out+", expected\n" + xyz);
         }
         xyz.scale (RTOD);
         R.getXyzAngles (out.getBuffer(), xyz.getBuffer(), 0, RTOD);
         if (!out.epsilonEquals (xyz, RTOD*1e-14)) {
            throw new TestException (
               "set/getXyz, scaled singularities: got "+out+", expected\n" + xyz);
         }
      }
   }

   void timeSetZDirection() {
      ArrayList<Vector3d> dirs = new ArrayList<>();
      int ntests = 1000000;
      for (int i=0; i<ntests; i++) {
         Vector3d dir = new Vector3d();
         dir.setRandom();
         dir.normalize();
         dirs.add (dir);
      }
      // warmup:
      RotationMatrix3d R = new RotationMatrix3d();
      for (Vector3d dir : dirs) {
         R.setZDirection (dir);
      }
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      for (Vector3d dir : dirs) {
         R.setZDirection (dir);
      }
      timer.stop();
      System.out.println ("setZDirection: " + timer.result(ntests));
   }


   public void timing () {
      timeSetZDirection();
   }      
   
   public void execute() {
      RotationMatrix3d RR = new RotationMatrix3d();
      RotationMatrix3d R1 = new RotationMatrix3d();
      RotationMatrix3d R2 = new RotationMatrix3d();

      testGeneric (R1);

      for (int i = 0; i < 100; i++) {
         R1.setRandom();
         R2.setRandom();
         RR.setRandom();

         testMul (RR, R1, R2);
         testMul (RR, RR, RR);

         testMulInverse (RR, R1, R2);
         testMulInverse (RR, RR, RR);

         testNegate (RR, R1);
         testNegate (RR, RR);

         testSet (RR, R1);
         testSet (RR, RR);

         testTranspose (RR, R1);
         testTranspose (RR, RR);

         testInvert (RR, R1);
         testInvert (RR, RR);

         testNorms (R1);

         testSetRotations (RR, R1);
         testNormalize (RR);

         testMulRot (RR);
      }

      for (int i = 0; i < 100; i++) {
         Vector3d zdir = new Vector3d();
         zdir.setRandom();
         zdir.scale (10);
         testSetZDirection (RR, zdir);
         testSetYDirection (RR, zdir);
         testSetXDirection (RR, zdir);

         Vector3d xdir = new Vector3d();
         xdir.setRandom();
         zdir.scale (1.4);
         testSetXYDirections (RR, xdir, zdir);
         testSetYZDirections (RR, xdir, zdir);
         testSetZXDirections (RR, xdir, zdir);

         double ex = EPSILON * RandomGenerator.get().nextDouble();
         double ey = EPSILON * RandomGenerator.get().nextDouble();
         testSetZDirection (RR, new Vector3d (ex, ey, 1));
         testSetZDirection (RR, new Vector3d (ex, ey, -1));

         testSetYDirection (RR, new Vector3d (ey, 1, ex));
         testSetYDirection (RR, new Vector3d (ey, -1, ex));

         testSetXDirection (RR, new Vector3d (1, ex, ey));
         testSetXDirection (RR, new Vector3d (-1, ex, ey));
      }
      testSetZDirection (RR, new Vector3d (0, 0, 0));
      testSetZDirection (RR, new Vector3d (0, 0, 1));
      testSetZDirection (RR, new Vector3d (0, 0, -1));
 
      testSetYDirection (RR, new Vector3d (0, 0, 0));
      testSetYDirection (RR, new Vector3d (0, 1, 0));
      testSetYDirection (RR, new Vector3d (0, -1, 0));

      testSetXDirection (RR, new Vector3d (0, 0, 0));
      testSetXDirection (RR, new Vector3d (1, 0, 0));
      testSetXDirection (RR, new Vector3d (-1, 0, 0));

      testPreciseAxisAngleSet ();

      testRpySolutions();
      testXyzSolutions();
   }

//   private void RPYtest() {
//      double [] rpy = {10, 90-1e-16, -72};
//      
//      RotationMatrix3d R = new RotationMatrix3d();
//      double rpyRad[] = new double[3];
//      double rpyOrigRad[] = new double[3];
//
//      for (int i=0; i<3; i++) {
//         rpyOrigRad[i] = Math.toRadians(rpy[i]);
//      }
//      
//      R.setRpy(rpyOrigRad);
//      R.getRpy(rpyRad);
//      
//      EulerFilter.filter(rpyOrigRad, rpyRad, 1e-8, rpyRad);
//      
//      for (int i=0; i<3; i++) {
//         rpyRad[i] = Math.toDegrees(rpyRad[i]);
//      }
//      
//      System.out.printf("Original degrees: %f, %f, %f\n", rpy[0], rpy[1], rpy[2]);
//      System.out.printf("Recovered degrees: %f, %f, %f\n", rpyRad[0], rpyRad[1], rpyRad[2]);
//      
//   }
   
   public static void main (String[] args) {
      RotationMatrix3dTest test = new RotationMatrix3dTest();

      boolean doTiming = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.out.println ("Unknown option "+args[i]+"; ignoring");
         }
      }

      RandomGenerator.setSeed (0x1234);

      if (doTiming) {
         test.timing();
      }
      else {
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
}
