/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.security.InvalidParameterException;

import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;

/**
 * Dual Quaternions a + A + e*(b + B), where 'e' is the dual parameter Useful
 * for representing rigid transformations and blending
 * 
 * see: Dual Quaternions for Rigid Transformation Blending (Kavan, 2006)
 * 
 * @author antonio
 * 
 */
public class DualQuaternion {

   public static double DEFAULT_DIB_TOLERANCE = 1e-8;
   public static int DEFAULT_DIB_MAX_ITERS = 3;

   private double a, b; // scalar parts
   private Vector3d A = new Vector3d(); // vector parts
   private Vector3d B = new Vector3d();

   // temporary variables for manipulations
   private Quaternion rQtmp = new Quaternion();
   private Quaternion tQtmp = new Quaternion();
   private Vector3d ltmp = new Vector3d();
   private Vector3d mtmp = new Vector3d();
   Vector3d dtmp = new Vector3d();
   Vector3d rtmp = new Vector3d();
   double[] tmp = new double[8];

   /**
    * Constructs the identity Dual Quaternion
    */
   public DualQuaternion () {
      setIdentity();
   }

   /**
    * Creates a dual quaternion with the supplied values
    * 
    * @param a
    * scalar part of the real portion
    * @param A
    * vector part of the real portion
    * @param b
    * scalar part of the dual portion
    * @param B
    * vector part of the dual portion
    */
   public DualQuaternion (double a, Vector3d A, double b, Vector3d B) {
      set(a, A, b, B);
   }

   /**
    * Creates a dual quaternion by copying an existing one
    */
   public DualQuaternion (DualQuaternion d) {
      set(d);
   }

   /**
    * Creates a dual quaternion from a rigid transformation
    */
   public DualQuaternion (RigidTransform3d A) {
      set(A);
   }

   /**
    * Creates a dual quaternion from two regular quaternions
    * 
    * @param qReal
    * the "real" part, corresponding to the rotation
    * @param qDual
    * the "dual" part, corresponding to translation
    */
   public DualQuaternion (Quaternion qReal, Quaternion qDual) {
      set(qReal, qDual);
   }

   /**
    * Sets to the identity dual quaternion 1 + 0e
    */
   public void setIdentity() {
      a = 1;
      A.setZero();
      b = 0;
      B.setZero();
   }

   /**
    * Sets to the zero quaternion 0 + 0e
    */
   public void setZero() {
      a = 0;
      b = 0;
      A.setZero();
      B.setZero();
   }

   /**
    * Sets parameters according to supplied values
    * 
    * @param a
    * real scalar
    * @param A
    * real vector
    * @param b
    * dual scalar
    * @param B
    * dual vector
    */
   public void set(double a, Vector3d A, double b, Vector3d B) {
      this.a = a;
      this.b = b;
      this.A.set(A);
      this.B.set(B);
   }

   /**
    * Sets parameters based on the 8-element array of vals:
    * 
    * @param vals
    * 0: real scalar 1-3: real vector 4: dual scalar 5-7: dual vector
    */
   public void set(double[] vals) {
      this.a = vals[0];
      this.A.x = vals[1];
      this.A.y = vals[2];
      this.A.z = vals[3];
      this.b = vals[4];
      this.B.x = vals[5];
      this.B.y = vals[6];
      this.B.z = vals[7];
   }

   /**
    * Fills the 8-element vector of values
    * 
    * @param vals
    * 0: real scalar 1-3: real vector 4: dual scalar 5-7: dual vector
    */
   public void get(double[] vals) {
      vals[0] = this.a;
      vals[1] = this.A.x;
      vals[2] = this.A.y;
      vals[3] = this.A.z;
      vals[4] = this.b;
      vals[5] = this.B.x;
      vals[6] = this.B.y;
      vals[7] = this.B.z;
   }

   /**
    * Copies an existing dual quaternion
    */
   public void set(DualQuaternion d) {
      this.a = d.a;
      this.b = d.b;
      this.A.set(d.A);
      this.B.set(d.B);
   }

   /**
    * Copies this dual quaternion to supplied d
    */
   public void get(DualQuaternion d) {
      d.a = this.a;
      d.b = this.b;
      d.A.set(this.A);
      d.B.set(this.B);
   }

   /**
    * Sets the values of this dual quaternion to represent the supplied rigid
    * transformation
    */
   public void set(RigidTransform3d trans) {
      rQtmp.set(trans.R);
      this.a = rQtmp.s;
      this.A.set(rQtmp.u);

      tQtmp.set(0, trans.p.x / 2, trans.p.y / 2, trans.p.z / 2);
      tQtmp.mul(rQtmp);
      this.b = tQtmp.s;
      this.B.set(tQtmp.u);
   }

   /**
    * Sets values based on real and dual quaternions
    * 
    * @param qReal
    * the rotation part
    * @param qDual
    * the dual translation part
    */
   public void set(Quaternion qReal, Quaternion qDual) {
      a = qReal.s;
      b = qDual.s;
      A.set(qReal.u);
      B.set(qDual.u);
   }

   /**
    * Gets the real (rotation) quaternion associated with this
    */
   public Quaternion getReal() {
      return new Quaternion(a, A);
   }

   /**
    * Gets the dual (translation) quaternion associated with this
    */
   public Quaternion getDual() {
      return new Quaternion(b, B);
   }

   /**
    * Gets the real (rotation) quaternion associated with this
    */
   public void getReal(Quaternion q) {
      q.set(a, A);
   }

   /**
    * Gets the dual (translation) quaternion associated with this
    */
   public void getDual(Quaternion q) {
      q.set(b, B);
   }

   /**
    * Sets the real (rotation) quaternion associated with this
    */
   public void setReal(Quaternion q) {
      a = q.s;
      A.set(q.u);
   }

   /**
    * Gets the dual (translational) quaternion associated with this
    */
   public void setDual(Quaternion q) {
      b = q.s;
      B.set(q.u);
   }
   
   /**
    * Fills trans with the rigid transformation implied
    * by this dual quaternion
    */
   public void getRigidTransform3d(RigidTransform3d trans) {
      RotationMatrix3d R = trans.R;
      Vector3d t = trans.p;
      
      R.m00 = a*a+A.x*A.x - A.y*A.y -A.z*A.z;
      R.m01 = 2*(A.x*A.y-a*A.z);
      R.m02 = 2*(A.x*A.z+a*A.y);
      R.m10 =  2*(A.x*A.y+a*A.z);
      R.m11 =  a*a - A.x*A.x + A.y*A.y -A.z*A.z;;
      R.m12 = 2*(A.y*A.z-a*A.x);
      R.m20 = 2*(A.x*A.z-a*A.y);
      R.m21 = 2*(A.y*A.z+a*A.x);
      R.m22 =  a*a - A.x*A.x - A.y*A.y + A.z*A.z;
      
      t.x = 2*(a*B.x - A.x*b + A.y*B.z - A.z*B.y);
      t.y = 2*(a*B.y - A.x*B.z - A.y*b +A.z*B.x);
      t.z = 2*(a*B.z + A.x*B.y - A.y*B.x - A.z*b);
   }
   
   /**
    * Gets the value at the supplied index, 1-4 are real parameters,
    * 5-7 are dual.
    */
   public double get(int idx) {
      if (idx == 0) {
         return a;
      } else if (idx <=3) {
         return A.get(idx-3);
      } else if (idx ==4) {
         return b;
      } else if (idx <= 7) {
         return B.get(idx-4);
      }
      throw new InvalidParameterException("Paramter mest be in [0,7]");
   }
   
   /**
    * Sets the value at the supplied index, 1-4 are real parameters,
    * 5-7 are dual.
    */
   public void set(int idx, double val) {
      if (idx == 0) {
         a = val;
      } else if (idx <=3) {
         A.set(idx-3, val);
      } else if (idx ==4) {
         b = val;
      } else if (idx <= 7) {
         B.set(idx-4, val);
      }
      throw new InvalidParameterException("Paramter mest be in [0,7]");
   }

   /**
    * Adds two dual quaternions and places the result in this
    */
   public void add(DualQuaternion q1, DualQuaternion q2) {
      this.a = q1.a + q2.a;
      this.b = q1.b + q2.b;
      this.A.add(q1.A, q2.A);
      this.B.add(q1.B, q2.B);
   }

   /**
    * Adds the supplied dual quaternion to this
    */
   public void add(DualQuaternion q) {
      add(this, q);
   }

   /**
    * Computes the scaled addition s*q1+q2 and places the result in this
    */
   public void scaledAdd(double s, DualQuaternion q1, DualQuaternion q2) {
      this.a = s * q1.a + q2.a;
      this.b = s * q1.b + q2.b;
      this.A.scaledAdd(s, q1.A, q2.A);
      this.B.scaledAdd(s, q1.B, q2.B);
   }

   /**
    * Adds s*q1 to this
    */
   public void scaledAdd(double s, DualQuaternion q) {
      scaledAdd(s, q, this);
   }

   /**
    * Computes the scaled addition s*q1+q2 and places the result in this
    */
   public void scaledAdd(DualScalar s, DualQuaternion q1, DualQuaternion q2) {
      double sa = s.getReal();
      double sb = s.getDual();

      this.B.scaledAdd(sa, q1.B, q2.B);
      this.B.scaledAdd(sb, q1.A);
      this.b = sb * q1.a + sa * q1.b + q2.b;

      this.a = sa * q1.a + q2.a;
      this.A.scaledAdd(sa, q1.A, q2.A);
   }

   /**
    * Adds s*q to this
    */
   public void scaledAdd(DualScalar s, DualQuaternion q) {
      scaledAdd(s, q, this);
   }

   /**
    * Computes q1-q2 and places the result in this
    */
   public void sub(DualQuaternion q1, DualQuaternion q2) {
      this.a = q1.a - q2.a;
      this.b = q1.b - q2.b;
      this.A.sub(q1.A, q2.A);
      this.B.sub(q1.B, q2.B);
   }

   /**
    * Subtracts q from this
    */
   public void sub(DualQuaternion q) {
      sub(this, q);
   }

   /**
    * Multiplies two dual quaternions and places the result in this. Note:
    * multiplication is not transitive
    * 
    * @param q1
    * the left-hand dual quaternion
    * @param q2
    * the right-hand dual quaternion
    */
   public void mul(DualQuaternion q1, DualQuaternion q2) {
      tmp[0] = -q1.A.x * q2.A.x - q1.A.y * q2.A.y -
         q1.A.z * q2.A.z + q1.a * q2.a;
      tmp[1] = q1.a * q2.A.x - q1.A.z * q2.A.y +
         q1.A.y * q2.A.z + q1.A.x * q2.a;
      tmp[2] = q1.A.z * q2.A.x + q1.a * q2.A.y -
         q1.A.x * q2.A.z + q1.A.y * q2.a;
      tmp[3] = -q1.A.y * q2.A.x + q1.A.x * q2.A.y +
         q1.a * q2.A.z + q1.A.z * q2.a;
      tmp[4] = -q1.B.x * q2.A.x - q1.B.y * q2.A.y -
         q1.B.z * q2.A.z + q1.b * q2.a -
         q1.A.x * q2.B.x - q1.A.y * q2.B.y -
         q1.A.z * q2.B.z + q1.a * q2.b;
      tmp[5] = q1.b * q2.A.x - q1.B.z * q2.A.y +
         q1.B.y * q2.A.z + q1.B.x * q2.a +
         q1.a * q2.B.x - q1.A.z * q2.B.y +
         q1.A.y * q2.B.z + q1.A.x * q2.b;
      tmp[6] = q1.B.z * q2.A.x + q1.b * q2.A.y -
         q1.B.x * q2.A.z + q1.B.y * q2.a +
         q1.A.z * q2.B.x + q1.a * q2.B.y -
         q1.A.x * q2.B.z + q1.A.y * q2.b;
      tmp[7] = -q1.B.y * q2.A.x + q1.B.x * q2.A.y +
         q1.b * q2.A.z + q1.B.z * q2.a -
         q1.A.y * q2.B.x + q1.A.x * q2.B.y +
         q1.a * q2.B.z + q1.A.z * q2.b;

      set(tmp);
   }

   /**
    * Multiplies this on the right by the dual quaternion q
    */
   public void mulRight(DualQuaternion q) {
      mul(this, q);
   }

   /**
    * Multiplies this on the left by the dual quaternion q
    */
   public void mulLeft(DualQuaternion q) {
      mul(q, this);
   }

   /**
    * Conjugates q and places the result in this (a+A + e*(b+B))^* = a-A +
    * e*(b-B)
    */
   public void conjugate(DualQuaternion q) {
      this.a = q.a;
      A.negate(q.A);
      this.b = q.b;
      B.negate(q.B);
   }

   /**
    * Conjugates this
    */
   public void conjugate() {
      conjugate(this);
   }

   /**
    * Computes the squared norm of the supplied dual quaternion. Note the result
    * is a dual scalar.
    */
   public static DualScalar normSquared(DualQuaternion q) {
      double a = q.a * q.a + q.A.dot(q.A);
      double b = 2 * (q.A.dot(q.B) + q.a * q.b);
      return new DualScalar(a, b);
   }

   /**
    * Computes the norm of the supplied dual quaternion. Note the result is a
    * dual scalar
    */
   public static DualScalar norm(DualQuaternion q) {
      DualScalar n = normSquared(q);
      n.sqrt();
      return n;
   }

   /**
    * Computes the norm, which is a dual scalar
    */
   public DualScalar norm() {
      return norm(this);
   }

   /**
    * Computes the squared norm, which is a dual scalar
    */
   public DualScalar normSquared() {
      return normSquared(this);
   }

   /**
    * Scales q by the dual scalar s and places the result in this
    */
   public void scale(DualScalar s, DualQuaternion q) {

      double sa = s.getReal();
      double sb = s.getDual();

      this.B.set(q.B);
      this.B.scale(sa);
      this.B.scaledAdd(sb, q.A);
      this.b = sb * q.a + sa * q.b;

      this.a = sa * q.a;
      this.A.set(q.A);
      this.A.scale(sa);

   }

   /**
    * Scales the dual quaternion q by s and places the result in this
    */
   public void scale(double s, DualQuaternion q) {

      this.B.set(q.B);
      this.B.scale(s);
      this.b = s * q.b;

      this.a = s * q.a;
      this.A.set(q.A);
      this.A.scale(s);

   }

   /**
    * Scales this by the supplied dual scalar
    */
   public void scale(DualScalar s) {
      scale(s, this);
   }

   /**
    * Scales this by the supplied value
    */
   public void scale(double s) {
      scale(s, this);
   }

   /**
    * Normalizes the supplied dual quaternion and places the result in this
    * ||result|| = 1 + e*0
    */
   public void normalize(DualQuaternion q) {
      DualScalar n = norm(q);
      n.invert();
      this.scale(n, q);
   }

   /**
    * Normalizes this dual quaternion
    */
   public void normalize() {
      normalize(this);
   }

   /**
    * Computes the inverse qinv = q^(-1) s.t. qinv*q = q*qinv = 1+0e and places
    * the result in this.
    */
   public void invert(DualQuaternion q) {
      DualScalar n = normSquared(q);
      n.invert();
      conjugate(q);
      scale(n);
   }

   /**
    * Inverts this dual quaternion s.t. qinv*q = q*qinv = 1+0e
    */
   public void invert() {
      invert(this);
   }

   /**
    * Performs the rigid transformation implied by q to vector v and places the
    * result in vr. Note that this only applies the rotational component on a
    * vector.
    * 
    * @param vr
    * result
    * @param q
    * transform
    * @param v
    * input
    */
   public static void transform(Vector3d vr, DualQuaternion q, Vector3d v) {

      // Old code, before optimization
      //
      // double Ax2 = q.A.x * q.A.x;
      // double Ay2 = q.A.y * q.A.y;
      // double Az2 = q.A.z * q.A.z;
      // double Aa2 = q.a * q.a;
      //
      // double x = v.x * (Ax2 - Ay2 - Az2 + Aa2)
      //    + 2 * v.y * (q.A.y * q.A.x - q.a * q.A.z)
      //    + 2 * v.z * (q.A.x * q.A.z + q.A.y * q.a);
      // double y = 2 * v.x * (q.A.x * q.A.y + q.A.z * q.a)
      //    + v.y * (-Ax2 + Ay2 - Az2 + Aa2)
      //    + 2 * v.z * (-q.A.x * q.a + q.A.z * q.A.y);
      // double z = 2 * v.x * (q.A.x * q.A.z - q.A.y * q.a)
      //    + 2 * v.y * (q.A.x * q.a + q.A.z * q.A.y)
      //    + v.z * (-Ax2 - Ay2 + Az2 + Aa2);
      // vr.set(x, y, z);

      // basic rotation
      Vector3d tmp = new Vector3d();
      tmp.cross (q.A, v);
      tmp.scaledAdd (q.a, v);
      tmp.cross (q.A, tmp);
      vr.scaledAdd (2, tmp, v);
   }

   /**
    * Performs the rigid transformation implied by this dual quaternion to
    * vector v and places the result in vr. Note that this only applies the
    * rotational component on a vector.
    * 
    * @param vr
    * result
    * @param v
    * input
    */
   public void transform(Vector3d vr, Vector3d v) {
      transform(vr, this, v);
   }

   /**
    * Performs the rigid transformation implied by this dual quaternion
    * 
    * @param v
    * vector to transform
    */
   public void transform(Vector3d v) {
      transform(v, this, v);
   }

   /**
    * Performs the rigid transformation implied by the dual quaternion q to
    * point p and places the result in pr.
    * 
    * @param pr
    * result
    * @param q
    * transform
    * @param p
    * input
    */
   public static void transform(Point3d pr, DualQuaternion q, Point3d p) {

      // Old code, before optimization
      // 
      // double Ax2 = q.A.x * q.A.x;
      // double Ay2 = q.A.y * q.A.y;
      // double Az2 = q.A.z * q.A.z;
      // double Aa2 = q.a * q.a;
      //
      // double x = 2 * (-q.b * q.A.x + q.B.z * q.A.y
      //    - q.B.y * q.A.z + q.B.x * q.a)
      //    + p.x * (Ax2 - Ay2 - Az2 + Aa2)
      //    + 2 * p.y * (q.A.y * q.A.x - q.a * q.A.z)
      //    + 2 * p.z * (q.A.x * q.A.z + q.A.y * q.a);
      // double y = 2 * (-q.B.z * q.A.x - q.b * q.A.y
      //    + q.B.x * q.A.z + q.B.y * q.a)
      //    + 2 * p.x * (q.A.x * q.A.y + q.A.z * q.a)
      //    + p.y * (-Ax2 + Ay2 - Az2 + Aa2)
      //    + 2 * p.z * (-q.A.x * q.a + q.A.z * q.A.y);
      // double z = 2 * (q.B.y * q.A.x - q.B.x * q.A.y
      //    - q.b * q.A.z + q.B.z * q.a)
      //    + 2 * p.x * (q.A.x * q.A.z - q.A.y * q.a)
      //    + 2 * p.y * (q.A.x * q.a + q.A.z * q.A.y)
      //    + p.z * (-Ax2 - Ay2 + Az2 + Aa2);
      //
      // pr.set(x, y, z);

      // rotate first
      transform ((Vector3d)pr, q, (Vector3d)p);

      // extract and add translation
      Vector3d tmp = new Vector3d();
      tmp.cross (q.A, q.B);
      tmp.scaledAdd (q.a, q.B);
      tmp.scaledAdd (-q.b, q.A);
      pr.scaledAdd (2, tmp);
   }

   /**
    * Performs the rigid transformation implied by this dual quaternion to point
    * p and places the result in pr.
    * 
    * @param pr
    * result
    * @param p
    * input
    */
   public void transform(Point3d pr, Point3d p) {
      transform(pr, this, p);
   }

   /**
    * Performs the rigid transformation implied by this dual quaternion
    * 
    * @param p
    * transformed vector
    */
   public void transform(Point3d p) {
      transform(p, this, p);
   }

   /**
    * Performs the inverse of the rigid transformation implied by the dual
    * quaternion q to point p and places the result in pr.
    * 
    * @param pr
    * result
    * @param q
    * transform to invert
    * @param p
    * input
    */
   public static void inverseTransform(Point3d pr, DualQuaternion q, Point3d p) {
      //
      // Old code, before optimization
      // 
      // double Ax2 = q.A.x * q.A.x;
      // double Ay2 = q.A.y * q.A.y;
      // double Az2 = q.A.z * q.A.z;
      // double Aa2 = q.a * q.a;

      // double x = 2 * (q.b * q.A.x + q.B.z * q.A.y
      //    - q.B.y * q.A.z - q.B.x * q.a)
      //    + p.x * (Ax2 - Ay2 - Az2 + Aa2)
      //    + 2 * p.y * (q.A.y * q.A.x + q.a * q.A.z)
      //    + 2 * p.z * (q.A.x * q.A.z - q.A.y * q.a);
      // double y = 2 * (-q.B.z * q.A.x + q.b * q.A.y
      //    + q.B.x * q.A.z - q.B.y * q.a)
      //    + 2 * p.x * (q.A.x * q.A.y - q.A.z * q.a)
      //    + p.y * (-Ax2 + Ay2 - Az2 + Aa2)
      //    + 2 * p.z * (q.A.x * q.a + q.A.z * q.A.y);
      // double z = 2 * (q.B.y * q.A.x - q.B.x * q.A.y
      //    + q.b * q.A.z - q.B.z * q.a)
      //    + 2 * p.x * (q.A.x * q.A.z + q.A.y * q.a)
      //    + 2 * p.y * (-q.A.x * q.a + q.A.z * q.A.y)
      //    + p.z * (-Ax2 - Ay2 + Az2 + Aa2);
      // pr.set(x, y, z);

      // extract and remove translation
      Vector3d tmp = new Vector3d();
      tmp.cross (q.A, q.B);
      tmp.scaledAdd (q.a, q.B);
      tmp.scaledAdd (-q.b, q.A);
      pr.scaledAdd (-2, tmp, p);

      // apply inverse rotation 
      inverseTransform ((Vector3d)pr, q, (Vector3d)pr);
   }

   /**
    * Performs the inverse of the rigid transformation implied by this dual
    * quaternion to point p and places the result in pr.
    * 
    * @param pr
    * result
    * @param p
    * input
    */
   public void inverseTransform(Point3d pr, Point3d p) {
      inverseTransform(pr, this, p);
   }

   /**
    * Performs the inverse of the rigid transformation implied by this dual
    * quaternion to point p
    * 
    * @param p
    * input
    */
   public void inverseTransform(Point3d p) {
      inverseTransform(p, this, p);
   }

   /**
    * Performs the inverse of the rotation implied by the dual quaternion q to
    * vector v and places the result in vr.
    * 
    * @param vr
    * result
    * @param q
    * transform to invert
    * @param v
    * input
    */
   public static void
      inverseTransform(Vector3d vr, DualQuaternion q, Vector3d v) {

      // Old code, before optimization
      // 
      // // basic rotation
      // double Ax2 = q.A.x * q.A.x;
      // double Ay2 = q.A.y * q.A.y;
      // double Az2 = q.A.z * q.A.z;
      // double Aa2 = q.a * q.a;
      //
      // double x = v.x * (Ax2 - Ay2 - Az2 + Aa2)
      //    + 2 * v.y * (q.A.y * q.A.x + q.a * q.A.z)
      //    + 2 * v.z * (q.A.x * q.A.z - q.A.y * q.a);
      // double y = 2 * v.x * (q.A.x * q.A.y - q.A.z * q.a)
      //    + v.y * (-Ax2 + Ay2 - Az2 + Aa2)
      //    + 2 * v.z * (q.A.x * q.a + q.A.z * q.A.y);
      // double z = 2 * v.x * (q.A.x * q.A.z + q.A.y * q.a)
      //    + 2 * v.y * (-q.A.x * q.a + q.A.z * q.A.y)
      //    + v.z * (-Ax2 - Ay2 + Az2 + Aa2);
      //
      // vr.set(x, y, z);

      Vector3d tmp = new Vector3d();
      tmp.cross (v, q.A);
      tmp.scaledAdd (q.a, v);
      tmp.cross (q.A, tmp);
      vr.scaledAdd (-2, tmp, v);
   }

   /**
    * Performs the inverse of the rotation implied by this dual quaternion to
    * vector v and places the result in vr.
    * 
    * @param vr
    * result
    * @param v
    * input
    */
   public void inverseTransform(Vector3d vr, Vector3d v) {
      inverseTransform(vr, this, v);
   }

   /**
    * Performs the inverse of the rotation implied by this dual quaternion to
    * vector v and places the result in vr.
    * 
    * @param v
    * input and result
    */
   public void inverseTransform(Vector3d v) {
      inverseTransform(v, this, v);
   }

   /**
    * Raises the supplied quaternion to the power e according to euler's formula
    * and places the result in this. Note: only applies to unit quaternions
    */
   public void pow(DualQuaternion q, double e) {

      // q = [ cos(theta/2), sin(theta/2)L]
      // + eps [-alpha/2*sin(theta/2), sin(theta/2)M + alpha/2*cos(theta/2)L]

      double normA = getScrewParameters(ltmp, mtmp, tmp, q);

      // pure translation
      if (normA < 1e-15) {
         set(q);
         this.B.scale(e);
         normalize();
         return;
      }

      // exponentiate
      double theta = tmp[0] * e;
      double alpha = tmp[1] * e;

      // convert back
      setScrewParameters(ltmp, mtmp, theta, alpha);

   }

   /**
    * Raises the supplied quaternion to the dual power e according to euler's
    * formula and places the result in this. Note: only applies to unit
    * quaternions
    */
   public void pow(DualQuaternion q, DualScalar e) {

      // q = [ cos(theta/2), sin(theta/2)L]
      // + eps [-alpha/2*sin(theta/2), sin(theta/2)M + alpha/2*cos(theta/2)L]

      double normA = getScrewParameters(ltmp, mtmp, tmp, q);
      // pure translation
      if (normA < 1e-14) {
         set(q);
         this.B.scale(e.getReal());
         normalize();
         return;
      }

      // exponentiate
      double alpha = tmp[1] * e.getReal() + tmp[0] * e.getDual();
      double theta = tmp[0] * e.getReal();
      setScrewParameters(ltmp, mtmp, theta, alpha);
      normalize();

   }

   /**
    * Extracts screw parameters from the dual quaternion q
    * 
    * @param l
    * the screw axis
    * @param m
    * the moment vector
    * @param angles
    * the dual angle angles[0] + e*angles[1]
    * @param q
    * input
    * @return the norm of the real vector component of q, which must
    * be {@code >} 0 for
    * screw parameters to be reliable
    */
   public static double getScrewParameters(Vector3d l, Vector3d m,
      double[] angles, DualQuaternion q) {

      double normA = q.A.norm();
      // pure translation
      if (normA < 1e-15) {
         l.set(q.B);
         l.normalize();
         m.setZero();
         angles[0] = 0;
         angles[1] = 2 * q.B.norm();
         return normA;
      }

      l.normalize(q.A);
      angles[0] = 2 * Math.atan2(normA, q.a);
      //      if (angles[0] > Math.PI / 2) {
      //         angles[0] -= Math.PI;
      //      }
      angles[1] = -2 * q.b / normA;
      m.scale(1.0 / normA, q.B);
      m.scaledAdd(q.a * q.b / normA / normA, l);

      return normA;

   }

   /**
    * Extracts screw parameters from this dual quaternion
    * 
    * @param l
    * the screw axis
    * @param m
    * the moment vector
    * @param angles
    * the dual angle angles[0] + e*angles[1]
    * @return the norm of the real vector component, which must be
    * {@code >} 0 for screw
    * parameters to be reliable
    */
   public double getScrewParameters(Vector3d l, Vector3d m, double[] angles) {
      return getScrewParameters(l, m, angles, this);
   }

   /**
    * Sets this quaternion based on screw parameters
    * 
    * @param l
    * the screw axis
    * @param m
    * the moment vector
    * @param theta
    * the real part of the screw angle
    * @param alpha
    * the dual part of the screw angle (pitch)
    */
   public void setScrewParameters(Vector3d l, Vector3d m, double theta,
      double alpha) {
      // convert back
      double cosa = Math.cos(theta / 2);
      double sina = Math.sin(theta / 2);

      this.a = cosa;
      this.A.scale(sina, ltmp);

      this.b = -alpha / 2 * sina;
      this.B.scale(sina, mtmp);
      this.B.scaledAdd(alpha / 2 * cosa, ltmp);
      normalize();
   }

   /**
    * Dual Quaternion Linear Blending (DLB) of two dual quaternions q = (1-t)*q1
    * + t*q2, with ||q|| = 1. Note that to ensure shortest path, q1 and q2
    * should be in the same direction {@code (q1.q2 > 0)}
    */
   public void dualQuaternionLinearBlending(DualQuaternion q1, double t,
      DualQuaternion q2) {

      // DLB = (1-t)*q1 + t*q2
      double s = 1;
      // // ensure shortest path
      // double d = q1.a * q2.a + q1.A.dot(q2.A);
      // if (d < 0) {
      // s = -1;
      // }
      scale(1 - t, q1);
      scaledAdd(s * t, q2);
      normalize();

   }

   /**
    * Dual Quaternion Linear Blending (DLB) of many dual quaternions. out =
    * sum(w[i]*q[i], i=1..n), with ||out|| = 1. Signs on the dual quaternions
    * should remain consistent.
    * 
    * @param w
    * vector of weights
    * @param q
    * vector of dual quaternions
    * @param numq
    * number of dual quaternions
    */
   public void dualQuaternionLinearBlending (
      double[] w, DualQuaternion[] q, int numq) {

      scale(w[0], q[0]);
      for (int i = 1; i < numq; i++) {
         scaledAdd(w[i], q[i]);
      }
      normalize();

   }

   /**
    * Performs the logarithm of q, which results in a zero scalar part, and
    * places the answer in this. This only applies to unit dual quaternions
    */
   public void log(DualQuaternion q) {
      getScrewParameters(ltmp, mtmp, tmp, q);
      this.a = 0;
      this.b = 0;
      this.A.scale(tmp[0] / 2, ltmp);
      this.B.scale(tmp[1] / 2, ltmp);
      this.B.scaledAdd(tmp[0] / 2, mtmp);
   }

   /**
    * Performs the logarithm of this, which results in a zero scalar part. This
    * only applies to unit dual quaternions
    */
   public void log() {
      log(this);
   }

   private void exp(Vector3d A, Vector3d B) {
      double theta = A.norm();
      if (theta < 1e-15) {
         this.a = 1;
         this.A.setZero();
         this.b = 0;
         this.B.setZero();
         return;
      }

      double alpha = A.dot(B) / 2 / theta;
      ltmp.normalize(A);
      mtmp.scale(1.0 / theta, B);
      mtmp.scaledAdd(-A.dot(B) / 2 / theta / theta / theta, A);
      setScrewParameters(ltmp, mtmp, theta, alpha);
   }

   /**
    * Performs the exponentiation to dual quaternion q, which must have a zero
    * scalar part. The result is placed in this.
    */
   public void exp(DualQuaternion q) {
      exp(q.A, q.B);
   }
   
   /**
    * Dual Quaternion Iterative Blending (DIB), which approximates ScLeRP and
    * applies to multiple inputs.
    * 
    * @param w
    * array of weights
    * @param q
    * quaternions
    * @param numq
    * number of quaternions
    * @param tol
    * convergence tolerance
    * @param maxIters
    * max iterations (should be small, ~3)
    */
   public void dualQuaternionIterativeBlending(
      double[] w, DualQuaternion[] q, int numq, double tol, int maxIters) {
      
      dualQuaternionLinearBlending(w, q, numq); // initial state

      // iterative
      DualQuaternion bTmp = new DualQuaternion();
      DualQuaternion bqTmp = new DualQuaternion();

      
      int nSteps = 0;
      double tol2 = tol * tol;
      double err = tol2 + 1;
      
      while (err > tol2 && nSteps < maxIters) {

         bTmp.conjugate(this);
         dtmp.setZero();
         rtmp.setZero();
         for (int i = 0; i < numq; i++) {
            
            bqTmp.mul(bTmp, q[i]);
            getScrewParameters(ltmp, mtmp, tmp, bqTmp);
            
            rtmp.scaledAdd(w[i] * tmp[0] / 2, ltmp);
            dtmp.scaledAdd(w[i] * tmp[1] / 2, ltmp);
            dtmp.scaledAdd(w[i] * tmp[0] / 2, mtmp);
         }
         bqTmp.exp(rtmp, dtmp);

         mul(this, bqTmp);

         err = rtmp.normSquared() + dtmp.normSquared();
         nSteps++;
      }

   }

   /**
    * Dual Quaternion Iterative Blending (DIB), which approximates ScLeRP and
    * applies to multiple inputs. Uses default tolerance and maximum iterations
    * 
    * @param w
    * array of weights
    * @param q
    * quaternions
    * @param numq
    * number of quaternions
    */
   public void dualQuaternionIterativeBlending (
      double[] w, DualQuaternion[] q, int numq) {
      dualQuaternionIterativeBlending(
         w, q, numq, DEFAULT_DIB_TOLERANCE, DEFAULT_DIB_MAX_ITERS);
   }

   /**
    * Dot product of this and supplied dual quaternion
    * 
    */
   public double dot(DualQuaternion q) {
      return dot(this, q);
   }

   /**
    * Dot product of the two supplied dual quaternions q1.a*q2.a + q1.A . q2.A
    */
   public static double dot(DualQuaternion q1, DualQuaternion q2) {
      return q1.A.dot(q2.A) + q1.a * q2.a;
   }

   /**
    * Screw Linear Interpolation ScLERP between two dual quaternions t represent
    * the normalized distance between the two (t=0.5 is half-way), q =
    * q1*(q1^-1q2)^t. For the shortest distance, q1 and q2 should have the same
    * orientation.
    * 
    */
   public void screwLinearInterpolate(DualQuaternion q1, double t,
      DualQuaternion q2) {

      // ScLERP = q1(q1^-1 q2)^t
      conjugate(q1);

      // correct for shortest distance
      mul(this, q2);
      // double d = q1.a * q2.a + q1.A.dot(q2.A);
      // if (d < 0) {
      // scale(-1);
      // }
      pow(this, t);
      mul(q1, this);

   }

   /**
    * Converts to a string
    */
   public String toString() {
      return toString("%g");
   }
   
   public String toString(NumberFormat fmt) {
      String str = fmt.format(a) + " " + A.toString(fmt) + " "
         + fmt.format(b) + " " + B.toString(fmt);
      return str;
   }
   
   public String toString(String fmtStr) {
      NumberFormat fmt = new NumberFormat(fmtStr);
      return toString(fmt);
   }

   /**
    * Number of entries = 8
    */
   public int size() {
      return 8;
   }

   /**
    * Returns a copy of this dual quaternion
    */
   public DualQuaternion clone() {
      return new DualQuaternion(this);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      RigidTransform3d T = new RigidTransform3d();
      T.setRandom();
      DualQuaternion dq = new DualQuaternion();
      dq.set (T);
      System.out.println ("norm=" + dq.norm());
      System.out.println
         ("inner=" + (dq.a*dq.b + dq.A.dot(dq.B)));
      System.out.println
         ("normSquared=" + dq.normSquared());
      Quaternion qt = new Quaternion();
      Quaternion qr = new Quaternion();
      dq.getReal (qr);
      dq.getDual (qt);
      System.out.println ("qr=" + qr);
      System.out.println ("qt=" + qt);
      System.out.println ("Me=" + 2*(qr.s*qt.s + qr.u.dot(qt.u)));
   }
}
