package maspack.matrix;

import maspack.util.*;

/**
 * Test the relative speed of fixed-size Vector and Matrix objects implemented
 * with fields vs. arrays.
 */
public class TimeTest { 

   static class Vec3d {
      double[] buf = new double[3];

      final double x() {
         return buf[0];
      }

      final double y() {
         return buf[1];
      }

      final double z() {
         return buf[2];
      }

      void set (Vector3d v) {
         buf[0] = v.x;
         buf[1] = v.y;
         buf[2] = v.z;
      }
   }

   static class Mat3d {
      double[] buf = new double[9];

      void set (Matrix3d M) {
         buf[0] = M.m00;
         buf[1] = M.m01;
         buf[2] = M.m02;
         buf[3] = M.m10;
         buf[4] = M.m11;
         buf[5] = M.m12;
         buf[6] = M.m20;
         buf[7] = M.m21;
         buf[8] = M.m22;
      }

      void mul (Vec3d vr, Vec3d v1) {
         double[] b1 = v1.buf;
         
         vr.buf[0] = buf[0]*b1[0] + buf[1]*b1[1] + buf[2]*b1[2];
         vr.buf[1] = buf[3]*b1[0] + buf[4]*b1[1] + buf[5]*b1[2];
         vr.buf[2] = buf[6]*b1[0] + buf[7]*b1[1] + buf[8]*b1[2];
      }
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);

      Vector3d v1 = new Vector3d();
      Vector3d vr = new Vector3d();
      Matrix3d A = new Matrix3d();

      Vec3d z1 = new Vec3d();
      Vec3d zr = new Vec3d();
      Mat3d B = new Mat3d();
      v1.setRandom();
      A.setRandom();

      z1.set (v1);
      B.set (A);

      int cnt = 1000000000;
      FunctionTimer timer = new FunctionTimer();
      for (int i=0; i<cnt; i++) {
         A.mul (vr, v1);
         B.mul (zr, z1);
      }
      timer.start();
      for (int i=0; i<cnt; i++) {
         A.mul (vr, v1);
      }
      timer.stop();
      System.out.println ("Fields: " + timer.result(cnt));
      timer.start();
      for (int i=0; i<cnt; i++) {
         B.mul (zr, z1);
      }
      System.out.println ("vr=" + vr);
      System.out.println ("zr=" + zr.x() + " " + zr.y() + " " + zr.z());
      timer.stop();
      System.out.println ("Arrays: " + timer.result(cnt));
   }
}
