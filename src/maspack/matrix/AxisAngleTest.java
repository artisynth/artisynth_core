package maspack.matrix;

import maspack.util.*;

public class AxisAngleTest extends UnitTest {

   public static double EPS = 1e-16;

   public void testQuaternionConversion() {
      int ntests = 100;
      double tol = 100*EPS;      
      Quaternion q = new Quaternion();
      Quaternion qnew = new Quaternion();
      AxisAngle axisAng = new AxisAngle();
      AxisAngle axisAngNew = new AxisAngle();
      for (int i=0; i<ntests; i++) {
         q.setRandom();
         q.normalize();
         axisAng.set (q);    // set axisAng from q
         axisAng.get (qnew); // set qnew from axisAng
         if (!qnew.epsilonEquals (q, tol)) {
            throw new TestException (
               "Quaterion to AxisAngle and back: got " + qnew + "\nexpected "+q);
         }
         axisAng.setRandom();
         axisAng.get (q);    // set q from axisAng
         axisAngNew.set (q); // set axisAngNew from q
         if (!axisAngNew.epsilonEquals (axisAng, tol)) {
            throw new TestException (
               "AxisAngle to Quaterion and back: got" +
               axisAngNew + "\nexpected "+ axisAng);
         }
      }
   }

   void testGetSet (RotationMatrix3d R, int off) {
      AxisAngle axisAng = new AxisAngle (R);
      AxisAngle axisAngChk = new AxisAngle();
      double[] vals = new double[4+off];
      double tol = 200*EPS;
      for (RotationRep rotRep : RotationRep.values()) {      
         double s = 1;
         axisAng.get (vals, null, off, rotRep, s);
         axisAngChk.set (vals, off, rotRep, s);
         if (!axisAng.epsilonEquals (axisAngChk, tol)) {
            System.out.println ("original axisAng=" + axisAng);
            System.out.println ("recreated axisAng=" + axisAngChk);
            throw new TestException (
               "get/set does not preserve value for "+rotRep);
         }
         s = 4.567;
         axisAng.get (vals, null, off, rotRep, s);
         axisAngChk.set (vals, off, rotRep, s);
         if (!axisAng.epsilonEquals (axisAngChk, tol)) {
            System.out.println ("original axisAng=" + axisAng);
            System.out.println ("recreated axisAng=" + axisAngChk);
            throw new TestException (
               "get/set does not preserve value with s != 1 for "+rotRep);
         }
      }
   }

   public void testGetSet() {
      int ntests = 100;
      for (int i=0; i<ntests; i++) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.setRandom();
         testGetSet (R, 0);
         testGetSet (R, 3);
      }
   }

   void testSetGet (int off) {
      for (RotationRep rotRep : RotationRep.values()) {   
         int ntests = 100;
         for (int i=0; i<ntests; i++) {
            VectorNd valsIn = QuaternionTest.randomRotValues(rotRep);
            testSetGet (valsIn, rotRep, off);
         }
         for (VectorNd valsIn : QuaternionTest.specialRotValues(rotRep)) {
            testSetGet (valsIn, rotRep, off);
         }
      }
   }

   void testSetGet (VectorNd valsIn, RotationRep rotRep, int off) {
      AxisAngle axisAng = new AxisAngle ();
      double tol = 100*EPS*valsIn.norm();
      double[] vals = new double[valsIn.size()+off];
      double[] refs = new double[valsIn.size()+off];
      for (int i=0; i<valsIn.size(); i++) {
         vals[i+off] = valsIn.get(i);
         refs[i+off] = valsIn.get(i);
      }
      double s = 1;
      axisAng.set (vals, off, rotRep, s);
      axisAng.get (vals, refs, off, rotRep, s);
      VectorNd valsOut = new VectorNd(valsIn.size());
      for (int i=0; i<valsIn.size(); i++) {
         valsOut.set(i, vals[i+off]);
      }
      checkEquals ("set/get, s=1, valsOut", valsOut, valsIn, tol);
      s = 4.567;
      if (rotRep != RotationRep.AXIS_ANGLE &&
          rotRep != RotationRep.AXIS_ANGLE_DEG) {
         for (int i=0; i<valsIn.size(); i++) {
            vals[i+off] *= s;
            refs[i+off] *= s;
         }
      }
      else {
         vals[3+off] *= s;
         refs[3+off] *= s;
      }
      axisAng.set (vals, off, rotRep, s);
      axisAng.get (vals, refs, off, rotRep, s);
      if (rotRep != RotationRep.AXIS_ANGLE &&
          rotRep != RotationRep.AXIS_ANGLE_DEG) {
         for (int i=0; i<valsIn.size(); i++) {
            valsOut.set(i, vals[i+off]/s);
         }
      }
      else {
         valsOut.set(3, vals[3+off]/s);
      }
      checkEquals ("set/get, s=4.567 valsOut", valsOut, valsIn, tol);
      if (rotRep == RotationRep.QUATERNION) {
         // negate the refs to see if we get a negative valsOut
         for (int i=0; i<valsIn.size(); i++) {
            refs[i+off] *= -1;
         }
         axisAng.get (vals, refs, off, rotRep, s);
         for (int i=0; i<valsIn.size(); i++) {
            valsOut.set(i, vals[i+off]/s);
         }
         valsIn.negate();
         checkEquals ("set/get, quaternion negate valsOut", valsOut, valsIn, tol);
      }
   }

   public void testSetGet() {
      testSetGet (0);
      testSetGet (3);
   }

   public void test() {
      testQuaternionConversion();
      testGetSet();
      testSetGet();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      AxisAngleTest tester = new AxisAngleTest();
      tester.runtest();
   }

}
