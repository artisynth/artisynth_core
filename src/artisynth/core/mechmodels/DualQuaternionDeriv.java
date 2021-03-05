package artisynth.core.mechmodels;

import java.util.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;

/**
 * Utility methods for computing derivatives assocaited with dual quaternions.
 */
public class DualQuaternionDeriv {

   private static void setLowerSkewSymmetric (Matrix4x3 M, Vector3d v) {
      M.m00 = 0;
      M.m01 = 0;
      M.m02 = 0;

      M.m10 = 0;
      M.m21 = 0;
      M.m32 = 0;

      M.m11 = -v.z;
      M.m12 = v.y;
      M.m22 = -v.x;

      M.m20 = v.z;
      M.m30 = -v.y;
      M.m31 = v.x;
   }

   private static void addLowerDiag (Matrix4x3 M, double s) {
      M.m10 += s;
      M.m21 += s;
      M.m32 += s;
   }

   /**
    * Compute the angular displacement derivatives of the rotational and
    * translational parts of the dual quaternion {@code dq} representing the
    * relative motion of a frame described with respect to a base frame. The
    * angular displacement is defined with respect to the frame's current
    * position (in world coordinates). If the current frame position id {@code
    * TW} and the base frame is {@code TWO}, then the relative motion is
    * described by
    * <pre>
    * TR = TW * inv(TW0)
    * </pre>
    *
    * @param DQrDr returns the derivative of the rotational component
    * @param DQtDr returns the derivative of the translational component
    * @param dqr dual quaternion describing the relative motion
    * @param TW0 base frame
    */
   public static void computeDerivs (
      Matrix4x3 DQrDr, Matrix4x3 DQtDr, DualQuaternion dqr,
      RigidTransform3d TW0) {

      // First, work out the derivatives with respect to the dual quaternion
      // qdw that describes the current frame position. This is given by
      //
      // dqw = dqr * dqw0
      //
      // where dqw0 is the dual quaternion representing the base frame

      DualQuaternion dqw = new DualQuaternion();
      DualQuaternion dqw0 = new DualQuaternion (TW0);
      dqw.mul (dqr, dqw0);
      // get rotational and translation components of dqw
      Quaternion qr = new Quaternion();
      Quaternion qt = new Quaternion();
      dqw.getReal (qr);
      dqw.getDual (qt);     

      // Compute DQrDr

      setLowerSkewSymmetric (DQrDr, qr.u);
      DQrDr.negate();
      addLowerDiag (DQrDr, qr.s);
      DQrDr.scale (0.5);
      DQrDr.m00 = -0.5*qr.u.x;
      DQrDr.m01 = -0.5*qr.u.y;
      DQrDr.m02 = -0.5*qr.u.z;

      // Compute DQtDr

      Vector3d p = new Vector3d();
      // extract p from qt
      p.cross (qr.u, qt.u);
      p.scaledAdd (qr.s, qt.u);
      p.scaledAdd (-qt.s, qr.u);
      p.scale (2);

      Matrix3d PX = new Matrix3d();
      PX.setSkewSymmetric (p);

      Vector3d tmp = new Vector3d();
      tmp.scale (qr.s, p);
      tmp.crossAdd (qr.u, p, tmp);
      tmp.scale (-1/4.0);

      Matrix3d M = new Matrix3d();
      M.addOuterProduct (qr.u, p);
      M.addOuterProduct (p, qr.u);
      M.scaledAdd (-qr.s, PX);
      M.addDiagonal (-qr.u.dot(p));

      DQtDr.setSubMatrix00 (tmp);
      M.scale (-1/4.0);
      DQtDr.setSubMatrix10 (M);

      // Now transform the derivates from dqw to dqr, using the formulas
      //
      // DQrDr = DQrDr qr0
      // 
      // DQtDr = DQrDr qt0 + DQtDr qr0
      //
      // where qr0 and qt0 are the rotational and translational component of
      // the inverse of dqw0.

      DualQuaternion dqw0inv = new DualQuaternion();
      dqw0inv.invert(dqw0);
      // make sure dqw * dqw0inv = dqr (instead of -dqr)
      // Note: this condition doesn't seem to arise
      DualQuaternion dqx = new DualQuaternion();      
      dqx.mul (dqw, dqw0inv);
      if (dqx.dot (dqr) < 0) {
         dqw0inv.scale (-1);
      }
      // extra rotational and translational components
      Quaternion qr0 = new Quaternion();
      Quaternion qt0 = new Quaternion();
      dqw0inv.getReal (qr0);
      dqw0inv.getDual (qt0);

      Matrix4x3 Dtmp0 = new Matrix4x3();
      Matrix4x3 Dtmp1 = new Matrix4x3();
      DualQuaternionDeriv.postmulDeriv (Dtmp1, DQtDr, qr0);
      DualQuaternionDeriv.postmulDeriv (Dtmp0, DQrDr, qt0);
      DQtDr.add (Dtmp0, Dtmp1);
      DualQuaternionDeriv.postmulDeriv (Dtmp0, DQrDr, qr0);
      DQrDr.set (Dtmp0);      
   }

   /**
    * Compute derivative of the rotational part of a dual quaternion
    * with respect to angular velocity.
    *
    * @param D returns the 4x3 derivative
    * @param qr rotational part of the dual quaternuon
    */
   public static void computeDQrDr (Matrix4x3 D, Quaternion qr) {
      setLowerSkewSymmetric (D, qr.u);
      D.negate();
      addLowerDiag (D, qr.s);
      D.scale (0.5);
      D.m00 = -0.5*qr.u.x;
      D.m01 = -0.5*qr.u.y;
      D.m02 = -0.5*qr.u.z;
   }

   /**
    * Post-multiplies in place the columns of a 4x3 quaternion derivative
    * matrix by a quaternon q.
    */
   public static void postmulDeriv (Matrix4x3 DR, Matrix4x3 D1, Quaternion q) {

      double t, vx, vy, vz;

      double s = q.s;
      Vector3d u = q.u;

      t = D1.m00; vx = D1.m10; vy = D1.m20; vz = D1.m30;

      DR.m00 = t*s - vx*u.x - vy*u.y - vz*u.z;
      DR.m10 = t*u.x + s*vx + vy*u.z - vz*u.y;
      DR.m20 = t*u.y + s*vy + vz*u.x - vx*u.z;
      DR.m30 = t*u.z + s*vz + vx*u.y - vy*u.x;

      t = D1.m01; vx = D1.m11; vy = D1.m21; vz = D1.m31;

      DR.m01 = t*s - vx*u.x - vy*u.y - vz*u.z;
      DR.m11 = t*u.x + s*vx + vy*u.z - vz*u.y;
      DR.m21 = t*u.y + s*vy + vz*u.x - vx*u.z;
      DR.m31 = t*u.z + s*vz + vx*u.y - vy*u.x;

      t = D1.m02; vx = D1.m12; vy = D1.m22; vz = D1.m32;

      DR.m02 = t*s - vx*u.x - vy*u.y - vz*u.z;
      DR.m12 = t*u.x + s*vx + vy*u.z - vz*u.y;
      DR.m22 = t*u.y + s*vy + vz*u.x - vx*u.z;
      DR.m32 = t*u.z + s*vz + vx*u.y - vy*u.x;
   }

   /**
    * Compute derivative of the translation part of a dual quaternion
    * with respect to angular velocity.
    *
    * @param D returns the 4x3 derivative
    * @param qt translational part of the dual quaternuon
    * @param qr rotational part of the dual quaternuon
    */
   public static void computeDQtDr (Matrix4x3 D, Quaternion qt, Quaternion qr) {
      Vector3d p = new Vector3d();
      // extract p from qt
      p.cross (qr.u, qt.u);
      p.scaledAdd (qr.s, qt.u);
      p.scaledAdd (-qt.s, qr.u);
      p.scale (2);

      Matrix3d PX = new Matrix3d();
      PX.setSkewSymmetric (p);

      Vector3d tmp = new Vector3d();
      tmp.scale (qr.s, p);
      tmp.crossAdd (qr.u, p, tmp);
      tmp.scale (-1/4.0);

      Matrix3d M = new Matrix3d();
      M.addOuterProduct (qr.u, p);
      M.addOuterProduct (p, qr.u);
      M.scaledAdd (-qr.s, PX);
      M.addDiagonal (-qr.u.dot(p));

      D.setSubMatrix00 (tmp);
      M.scale (-1/4.0);
      D.setSubMatrix10 (M);
   }

   /**
    * Sets M to
    * <pre>
    *  u  -s I - [u]
    * </pre>
    */
   private static void set (Matrix3x4 M, double s, Vector3d u) {
      M.m00 = u.x;
      M.m10 = u.y;
      M.m20 = u.z;

      M.m01 = -s;
      M.m02 = u.z;
      M.m03 = -u.y;

      M.m11 = -u.z;
      M.m12 = -s;
      M.m13 = u.x;

      M.m21 = u.y;
      M.m22 = -u.x;
      M.m23 = -s;
   }

   public static Matrix4x3 computeNumericDQtDr (
      RigidTransform3d TW, RigidTransform3d TW0) {

      if (TW0 == null) {
         TW0 = RigidTransform3d.IDENTITY;
      }
      RigidTransform3d TD = new RigidTransform3d();
      TD.mulInverseRight (TW, TW0);
      DualQuaternion dq = new DualQuaternion();
      dq.set (TD);
      Quaternion qt0 = new Quaternion(); // translation part
      dq.getDual (qt0);

      Matrix4x3 DQtQr = new Matrix4x3();
      double h = 1e-8;
      RigidTransform3d Tinc = new RigidTransform3d();
      RigidTransform3d TX = new RigidTransform3d();
      Quaternion qt = new Quaternion();

      for (int j=0; j<3; j++) {
         Twist vel = new Twist();
         vel.w.set (j, h);
         vel.setTransform (Tinc);
         TX.p.set (TW.p);
         TX.R.mul (Tinc.R, TW.R);
         TD.mulInverseRight (TX, TW0);
         dq.set (TD);
         dq.getDual (qt);
         qt.sub (qt0);
         qt.scale (1/h);
         DQtQr.setColumn (j, qt);
      }
      return DQtQr;
   }

   public static Matrix4x3 computeNumericDQrDr (
      RigidTransform3d TW, RigidTransform3d TW0) {

      if (TW0 == null) {
         TW0 = RigidTransform3d.IDENTITY;
      }
      RigidTransform3d TD = new RigidTransform3d();
      TD.mulInverseRight (TW, TW0);
      DualQuaternion dq = new DualQuaternion();
      dq.set (TD);
      Quaternion qr0 = new Quaternion(); // translation part
      dq.getReal (qr0);

      Matrix4x3 DQrQr = new Matrix4x3();
      double h = 1e-8;
      RigidTransform3d Tinc = new RigidTransform3d();
      RigidTransform3d TX = new RigidTransform3d();
      Quaternion qr = new Quaternion();

      for (int j=0; j<3; j++) {
         Twist vel = new Twist();
         vel.w.set (j, h);
         vel.setTransform (Tinc);
         TX.p.set (TW.p);
         TX.R.mul (Tinc.R, TW.R);
         TD.mulInverseRight (TX, TW0);
         dq.set (TD);
         dq.getReal (qr);
         qr.sub (qr0);
         qr.scale (1/h);
         DQrQr.setColumn (j, qr);
      }
      return DQrQr;
   }

   /**
    * Computes the Jr and Jtt matrices for a normalized dual quaternion, its
    * original real and dual magnitudes m0 and me, and an original point
    * position r.
    *
    * @param Jr returns the Jr matrix 
    * @param Jtt returns the Jtt matrix
    * @param dq normalized dual quaternion
    * @param m0 real magnitude of original non-normalized dual quaternion
    * @param me dual magnitude of original non-normalized dual quaternion
    * @param r original point position
    */
   public static void computeJrJtt (
      Matrix3x4 Jr, Matrix3x4 Jtt,
      DualQuaternion dq, double m0, double me, Point3d r) {
      
      Quaternion qr = new Quaternion();
      Quaternion qt = new Quaternion();
      dq.getReal (qr);
      dq.getDual (qt);

      Vector3d u = qr.u;
      double s = qr.s;
      Vector3d z = qt.u;
      double t = qt.s;

      // Compute Jtr directly into Jr

      Matrix3d M = new Matrix3d();
      Vector3d y = new Vector3d();
      set (Jr, t, z);
      y.cross (z, u);
      y.scaledAdd (-s, z);
      y.scaledAdd (t, u);
      M.outerProduct (y, u);
      M.scale (2);
      Jr.addSubMatrix01 (M);
      y.scale (2*s);
      Jr.addSubMatrix00 (y);
      Jr.scale (2);

      // compute Jrr and add it directly into Jr

      Vector3d w = new Vector3d();
      Vector3d b = new Vector3d();
      Vector3d v = new Vector3d();

      w.cross (u,r);
      b.cross (u,w);
      b.scaledAdd (s, w);
      b.scale (2);
      v.scaledAdd (-s, b, w);
      v.scale (2);
      Jr.addSubMatrix00 (v);
      v.scaledAdd (s, r, w);
      M.setSkewSymmetric (v);
      v.add (b, r);
      M.addOuterProduct (v, u);
      M.addDiagonal (-r.dot(u));
      M.scale (-2);
      Jr.addSubMatrix01 (M);

      // Compute Jtt and then add it into Jr:
      set (Jtt, s, u);
      Jtt.scale (-2);
      Jr.scaledAdd (-me/(2*m0), Jtt);      
   }
}

