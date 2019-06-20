/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.*;

/**
 * Implements a quaternion as a fixed-size 4 element vector. Element 0 is the
 * scalar element, while elements 1-3 are the vector components.
 */
public class Quaternion extends VectorBase {
   private static double DOUBLE_PREC = 2.220446049250313e-16;

   public static final Quaternion IDENTITY = new Quaternion(1, 0, 0, 0);
   public static final Quaternion ZERO = new Quaternion();

   /**
    * Scalar element
    */
   public double s;

   /**
    * Vector element
    */
   public Vector3d u;

   private Vector3d utmp;

   /**
    * Creates a Quaternion and initializes its elements to 0.
    */
   public Quaternion() {
      u = new Vector3d();
      utmp = new Vector3d();
   }

   /**
    * Creates a Quaternion by copying an existing one.
    * 
    * @param q
    * quaternion to be copied
    */
   public Quaternion (Quaternion q) {
      this();
      set (q);
   }

   /**
    * Creates a Quaternion from an AxisAngle.
    * 
    * @param axisAng
    * axis-angle used to specify the rotation
    */
   public Quaternion (AxisAngle axisAng) {
      this();
      setAxisAngle (axisAng);
   }

   /**
    * Creates a Quaternion with the supplied element values.
    * 
    * @param s
    * scalar element
    * @param ux
    * first vector element
    * @param uy
    * second vector element
    * @param uz
    * third vector element
    */
   public Quaternion (double s, double ux, double uy, double uz) {
      this();
      set (s, ux, uy, uz);
   }

   /**
    * Creates a Quaternion with the supplied element values.
    * 
    * @param s
    * scalar element
    * @param u
    * vector component
    */
   public Quaternion (double s, Vector3d u) {
      this();
      set (s, u);
   }

   /**
    * Returns the size of this quaternion (which is always 4)
    * 
    * @return 4
    */
   public int size() {
      return 4;
   }

   /**
    * Gets a single element of this quaternion. Elements 0, 1, 2, and 3
    * correspond to s, u.x, u.y, and u.z.
    * 
    * @param i
    * element index
    * @return element value throws ArrayIndexOutOfBoundsException if i is not in
    * the range 0 to 3.
    */
   public double get (int i) {
      switch (i) {
         case 0: {
            return s;
         }
         case 1: {
            return u.x;
         }
         case 2: {
            return u.y;
         }
         case 3: {
            return u.z;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException (i);
         }
      }
   }

   /**
    * Copies the elements of this quaternion into an array of doubles. The
    * array must have a length {@code >=} 4.
    * 
    * @param values
    * array into which values are copied
    */
   public void get (double[] values) {
      if (values.length < 4) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= 4");
      }
      values[0] = s;
      values[1] = u.x;
      values[2] = u.y;
      values[3] = u.z;
   }

   /**
    * Sets a single element of this quaternion. Elements 0, 1, 2, and 3
    * correspond to s, u.x, u.y, and u.z.
    * 
    * @param i
    * element index
    * @param value
    * element value
    * @throws ArrayIndexOutOfBoundsException
    * if i is not in the range 0 to 3.
    */
   public void set (int i, double value) {
      switch (i) {
         case 0: {
            s = value;
            break;
         }
         case 1: {
            u.x = value;
            break;
         }
         case 2: {
            u.y = value;
            break;
         }
         case 3: {
            u.z = value;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException (i);
         }
      }
   }

   public void setIdentity() {
      s = 1;
      u.setZero();
   }

   /**
    * Sets the elements of this quaternion from an array of doubles. The array
    * must have a length of at least 4.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (double[] values) {
      if (values.length < 4) {
         throw new IllegalArgumentException (
            "argument 'values' must have a length of at least 4");
      }     
      s = values[0];
      u.x = values[1];
      u.y = values[2];
      u.z = values[3];
   }

   /**
    * Sets the elements of this quaternion from an array of doubles, starting
    * from a particular location. The array must extend for at least
    * 4 elements beyond that location.
    * 
    * @param values
    * array from which values are copied
    * @param idx starting point within values from which copying should begin
    * @return updated idx value
    */
   public int set (double[] values, int idx) {
      if (values.length < idx+4) {
         throw new IllegalArgumentException (
            "argument 'values' must extend for at least 4 elements past 'idx'");
      }     
      s = values[idx++];
      u.x = values[idx++];
      u.y = values[idx++];
      u.z = values[idx++];
      return idx;
   }

   /**
    * Sets the values of this quaternion to those of q1.
    * 
    * @param q1
    * quaternion whose values are copied
    */
   public void set (Quaternion q1) {
      s = q1.s;
      u.x = q1.u.x;
      u.y = q1.u.y;
      u.z = q1.u.z;
   }

   /**
    * Adds q1 quaternion to q2 and places the result in this quaternion.
    * 
    * @param q1
    * left-hand quaternion
    * @param q2
    * right-hand quaternion
    */
   public void add (Quaternion q1, Quaternion q2) {
      u.x = q1.u.x + q2.u.x;
      u.y = q1.u.y + q2.u.y;
      u.z = q1.u.z + q2.u.z;
      s = q1.s + q2.s;
   }

   /**
    * Adds this quaternion to q1 and places the result in this quaternion.
    * 
    * @param q1
    * right-hand quaternion
    */
   public void add (Quaternion q1) {
      u.x += q1.u.x;
      u.y += q1.u.y;
      u.z += q1.u.z;
      s += q1.s;
   }

   /**
    * Subtracts quaternion q1 from q2 and places the result in this quaternion.
    * 
    * @param q1
    * left-hand quaternion
    * @param q2
    * right-hand quaternion
    */
   public void sub (Quaternion q1, Quaternion q2) {
      u.x = q1.u.x - q2.u.x;
      u.y = q1.u.y - q2.u.y;
      u.z = q1.u.z - q2.u.z;
      s = q1.s - q2.s;
   }

   /**
    * Subtracts q1 from this quaternion and places the result in this
    * quaternion.
    * 
    * @param q1
    * right-hand quaternion
    */
   public void sub (Quaternion q1) {
      u.x -= q1.u.x;
      u.y -= q1.u.y;
      u.z -= q1.u.z;
      s -= q1.s;
   }

   /**
    * Sets this quaternion to the negative of q1.
    * 
    * @param q1
    * quaternion to negate
    */
   public void negate (Quaternion q1) {
      u.x = -q1.u.x;
      u.y = -q1.u.y;
      u.z = -q1.u.z;
      s = -q1.s;
   }

   /**
    * Negates this quaternion in place.
    */
   public void negate() {
      u.x = -u.x;
      u.y = -u.y;
      u.z = -u.z;
      s = -s;
   }

   /**
    * Scales the elements of this quaternion by <code>r</code>.
    * 
    * @param r
    * scaling factor
    */
   public void scale (double r) {
      u.x = r * u.x;
      u.y = r * u.y;
      u.z = r * u.z;
      s = r * s;
   }

   /**
    * Scales the elements of quaternion q1 by <code>r</code> and places the
    * results in this quaternion.
    * 
    * @param r
    * scaling factor
    * @param q1
    * quaternion to be scaled
    */
   public void scale (double r, Quaternion q1) {
      u.x = r * q1.u.x;
      u.y = r * q1.u.y;
      u.z = r * q1.u.z;
      s = r * q1.s;
   }

   /**
    * Computes the interpolation <code>(1-r) q1 + r q2</code> and places the
    * result in this quaternion.
    * 
    * @param q1
    * left-hand quaternion
    * @param r
    * interpolation factor
    * @param q2
    * right-hand quaternion
    */
   public void interpolate (Quaternion q1, double r, Quaternion q2) {
      u.x = (1 - r) * q1.u.x + r * q2.u.x;
      u.y = (1 - r) * q1.u.y + r * q2.u.y;
      u.z = (1 - r) * q1.u.z + r * q2.u.z;
      s = (1 - r) * q1.s + r * q2.s;
   }

   /**
    * Computes the interpolation <code>(1-r) this + r q1</code> and places the
    * result in this quaternion.
    * 
    * @param r
    * interpolation factor
    * @param q1
    * right-hand quaternion
    */
   public void interpolate (double r, Quaternion q1) {
      u.x = (1 - r) * u.x + r * q1.u.x;
      u.y = (1 - r) * u.y + r * q1.u.y;
      u.z = (1 - r) * u.z + r * q1.u.z;
      s = (1 - r) * s + r * q1.s;
   }

   private void quaternionVectorProduct (
      Vector3d u, double s1, Vector3d u1, double s2, Vector3d u2) {

      double u1x = u1.x;
      double u1y = u1.y;
      double u1z = u1.z;
      
      double u2x = u2.x;
      double u2y = u2.y;
      double u2z = u2.z;
      
      u.x = s1*u2x + s2*u1x + u1y*u2z - u1z*u2y;
      u.y = s1*u2y + s2*u1y + u1z*u2x - u1x*u2z;
      u.z = s1*u2z + s2*u1z + u1x*u2y - u1y*u2x;
   }      

   /**
    * Computes a spherical (great circle) interpolation between quaternions q1
    * and q2 and places the result in this quaternion. The result is a unit
    * quaternion. Non-unit quaternions can be used for input.
    * 
    * <p>
    * In the literature, this operation is sometimes known as "Slerp", a name
    * that was coined by Ken Showmake when he described the process in SIGGRAPH
    * 1985.
    * 
    * @param q1
    * left-hand quaternion
    * @param r
    * interpolation factor
    * @param q2
    * right-hand quaternion
    */
   public void sphericalInterpolate (Quaternion q1, double r, Quaternion q2) {

      // Old code for creating unit quaternions if needed
      // if (unitErr > (2 * DOUBLE_PREC) || unitErr < -(2 * DOUBLE_PREC)) {
      //    System.out.println ("q1 not unit " + q1.norm());
      //    q1Unit = new Quaternion (q1);
      //    q1Unit.scale (1 / Math.sqrt (lenSqr));
      // }

      double stmp = q1.s*q2.s + q1.u.dot(q2.u);
      quaternionVectorProduct (utmp, -q2.s, q2.u, q1.s, q1.u);
      double mag = utmp.norm();
      double ang = Math.atan2 (mag, stmp);
      if (Math.abs(ang) < DOUBLE_PREC) {
         normalize (q1);
      }
      else {
         if (ang > Math.PI/2) {
            ang -= Math.PI;
         }
         stmp = Math.cos (ang*r);
         utmp.scale (Math.sin (ang*r)/mag);
         double q1s = q1.s;
         s = q1s*stmp - q1.u.dot(utmp);
         quaternionVectorProduct (u, q1s, q1.u, stmp, utmp);
         normalize();
      }

      // Old slerp code from Shoemake 1985
      // double signQ1 = 1;
      // double cos = q1.dot (q2);
      // if (cos < 0) {
      //    signQ1 = -1;
      //    cos = -cos;
      // }

      // (stmp, utmp) form a quaternion perpendicular to q1
      // double stmp = -cos * signQ1 * q1.s + q2.s;
      // utmp.scaledAdd (-cos * signQ1, q1.u, q2.u);
      // double sin = Math.sqrt (stmp * stmp + utmp.normSquared());

      // if (sin < ANGLE_EPSILON) {
      //    interpolate (q1, r, q2);
      //    System.out.println ("interpolate");
      // }
      // else {
      //    double ang = Math.atan2 (sin, cos);
      //    double sin_r = Math.sin (ang * r);
      //    double cos_r = Math.cos (ang * r);
      //    s = cos_r * signQ1 * q1.s + sin_r / sin * stmp;
      //    u.combine (cos_r * signQ1, q1.u, sin_r / sin, utmp);
      //    System.out.println ("combine");
      // }
      // normalize();
   }

   /**
    * Computes a normalized rotational interpolation between quaternions q1 and
    * q2 and places the result in this quaternion. The result is a unit
    * quaternion.
    * 
    * <p>
    * This operation is sometimes known as "Nlerp", and is faster than spherical
    * interpolation. Essentially, it involves doing a linear interpolation
    * between q1 qnd q2 and then normalizing the result. q1 and q2 are assumed
    * to be unit quaternions. If they are not, unit versions of them are created
    * internally although this will slow the routine down. Also, if -q1 is
    * closer to q2 than q1, then the interpolation is done between -q1 and q2;
    * this ensures that the shortest rotationa; path is used.
    * 
    * @param q1
    * left-hand quaternion
    * @param r
    * interpolation factor
    * @param q2
    * right-hand quaternion
    */
   public void normalizedInterpolate (Quaternion q1, double r, Quaternion q2) {
      double lenSqr, unitErr;
      // create new normalized quaternions if necessary

      Quaternion q1Unit = q1;
      lenSqr = q1.normSquared();
      unitErr = lenSqr - 1;
      if (unitErr > (2 * DOUBLE_PREC) || unitErr < -(2 * DOUBLE_PREC)) {
         q1Unit = new Quaternion (q1);
         q1Unit.scale (1 / Math.sqrt (lenSqr));
      }

      Quaternion q2Unit = q2;
      lenSqr = q2.normSquared();
      unitErr = lenSqr - 1;
      if (unitErr > (2 * DOUBLE_PREC) || unitErr < -(2 * DOUBLE_PREC)) {
         q2Unit = new Quaternion (q2);
         q2Unit.scale (1 / Math.sqrt (lenSqr));
      }

      double cos = q1Unit.dot (q2Unit);
      if (cos < 0) {
         q1Unit.negate();
         cos = -cos;
      }
      interpolate (q1Unit, r, q2Unit);
      normalize();
   }

   public void getAxisAngle (AxisAngle axisAng) {
      double mag = u.norm();
      double ang = Math.atan2 (mag, s);
      if (ang > Math.PI/2) {
         ang = ang - Math.PI;
         mag = -mag;
      }
      axisAng.angle = 2*ang;
      if (mag < DOUBLE_PREC) {
         axisAng.axis.set (1, 0, 0);
      }
      else {
         axisAng.axis.scale (1/mag, u);      
      }
   }
   
   public void getRotationMatrix(RotationMatrix3d R) {
      double n2 = 1.0/normSquared ();
      double qr = this.s;
      double qi = this.u.x;
      double qj = this.u.y;
      double qk = this.u.z;
      R.m00 = 1-2*n2*(qj*qj + qk*qk);
      R.m01 = 2*n2*(qi*qj - qk*qr);
      R.m02 = 2*n2*(qi*qk + qj*qr);
      R.m10 = 2*n2*(qi*qj + qk*qr);
      R.m11 = 1-2*n2*(qi*qi + qk*qk);
      R.m12 = 2*n2*(qj*qk-qi*qr);
      R.m20 = 2*n2*(qi*qk-qj*qr);
      R.m21 = 2*n2*(qj*qk + qi*qr);
      R.m22 = 1-2*n2*(qi*qi + qj*qj);
   }

   public void setExp (double scale, Vector3d v) {
      double ang = v.norm();
      if (Math.abs (ang) < DOUBLE_PREC) {
         s = 1;
         u.setZero();
      }
      else {
         s = Math.cos (scale*ang);
         u.scale (Math.sin (scale*ang)/ang, v);
      }
   }

   public double log (Vector3d v) {
      double mag = u.norm();
      double ang = Math.atan2 (mag, s);
      if (ang > Math.PI/2) {
         ang = ang - Math.PI;
      }
      if (mag < DOUBLE_PREC) {
         ang = 0;
         v.setZero();
      }
      else {
         v.scale (ang/mag, u);
      }
      return ang;
   }

   /** 
    * Interpolates a point on a quaternion curve using the Bezier interpolation
    * scheme described by Shoemake in his 1985 SIGGRAPH paper, "Animating
    * Rotation with Quaternion Curves".
    *
    * <p>The inputs to the procedure consist of four orientations defining the
    * desired orientation at the interval end-points, along with two
    * intermediate control points. The desired interpolation location is
    * described by a parameter <code>s</code>.  A unit quaternion is
    * returned. The input quaternions are assumed to be unit quaternions.
    * 
    * @param qr interpolated rotation result
    * @param q0 unit quaternion giving the rotation at the interval beginning
    * @param qa unit quaternion giving the first control point
    * @param qb unit quaternion giving the second control point
    * @param q1 unit quaternion giving the rotation at the interval end
    * @param s interpolation location on the interval (in the range [0,1])
    */   
   public static void sphericalBezierShoemake (
      Quaternion qr,
      Quaternion q0, Quaternion qa, Quaternion qb, Quaternion q1, double s) {

      // Note that if we want to specify end-point velocities instead,
      // we should compute the control points qa and qb from
      // 
      // qa.extrapolateWorld (q0, w0, h/3);
      // qb.extrapolateWorld (q1, w1, -h/3);

      Quaternion qx = new Quaternion();
      Quaternion qy = new Quaternion();
      Quaternion qz = qr;
      // ensure that qr can also be an input
      if (qr == q0 || qr == qa || qr == qb || qr == q1) {
         qz = new Quaternion();
      }
      qx.sphericalInterpolate (q0, s, qa);
      qy.sphericalInterpolate (qa, s, qb);
      qz.sphericalInterpolate (qx, s, qy);
      qx.sphericalInterpolate (qb, s, q1);
      qy.sphericalInterpolate (qy, s, qx);
      qz.sphericalInterpolate (qz, s, qy);

      qz.normalize();
      if (qz != qr) {
         qr.set (qz);
      }
      // Old code for computing the velocity. While this seems to give the
      // correct velocity at the interval end-points, it does not seem to do so
      // within the interval. Hence we don't compute velocity any more.
      //
      // See ``A Compact Differential Formula for the First Derivative of a
      // Unit Quaternion Curve'', by Kim, Kim, and Shin (Journal of
      // Visualization and Computer Animation, for more on this problem
      //
//       if (vr != null) {
//          qa.mulInverseLeft (qa, qb);
//          double mag = qa.u.norm();
//          double ang = 2*Math.atan2 (mag, qa.s);
//          vr.scale (3*ang/(h*mag), qa.u);
//          qr.transform (vr, vr);
//       }
   }

   /** 
    * Interpolates a point on a quaternion curve using cubic Bezier
    * interpolation. The method used is described ``A General Construction
    * Scheme for Unit Quaternion Curves with Simple High Order Derivatives'',
    * by Kim, Kim, and Shin, in SIGGRAPH '95.
    *
    * <p>The inputs to the procedure consist of four orientations defining the
    * desired orientation at the interval end-points, along with two
    * intermediate control points. The desired interpolation location is
    * described by a parameter <code>s</code>.  A unit quaternion is
    * returned. The input quaternions are assumed to be unit quaternions.
    * 
    * @param qr interpolated rotation result
    * @param q0 unit quaternion giving the rotation at the interval beginning
    * @param qa unit quaternion giving the first control point
    * @param qb unit quaternion giving the second control point
    * @param q1 unit quaternion giving the rotation at the interval end
    * @param s interpolation location on the interval (in the range [0,1])
    */   
   public static void sphericalBezier (
      Quaternion qr,
      Quaternion q0, Quaternion qa, Quaternion qb, Quaternion q1, double s) {

      Vector3d vx = new Vector3d();
      Quaternion qx = new Quaternion();
      Quaternion qz = qr;

      // ensure that qr can also be an input
      if (qr == q0 || qr == qa || qr == qb || qr == q1) {
         qz = new Quaternion();
      }

      qx.mulInverseLeft (q0, qa);
      qx.log (vx);
      qx.setExp (((s-3)*s+3)*s, vx);
      qz.mul (q0, qx);

      qx.mulInverseLeft (qa, qb);
      qx.log (vx);
      qx.setExp ((-2*s+3)*s*s, vx);
      qz.mul (qx);

      qx.mulInverseLeft (qb, q1);
      qx.log (vx);
      qx.setExp (s*s*s, vx);
      qz.mul (qx);
      qz.normalize();

      if (qz != qr) {
         qr.set (qz);
      }
   }

   /** 
    * Interpolates a point on a quaternion curve using cubic Hermite
    * interpolation. The method used is described ``A General Construction
    * Scheme for Unit Quaternion Curves with Simple High Order Derivatives'',
    * by Kim, Kim, and Shin, in SIGGRAPH '95. We use this method instead of the
    * de Casteljau and 'squad' procedures described by Shoemake because it
    * admits an easy way to interpolate the angular velocity.
    *
    * <p>The inputs to the procedure consist of orientations and angular
    * velocities at the beginning and end-points of an interval, along with the
    * desired interpolation location (described by a parameter <code>s</code>)
    * and the length of time associated with the interval (<code>h</code>).
    * The angular velocities are supplied (and returned) in global coordinates.
    * If local coordinates are preferred, then the routine {@link
    * #sphericalHermiteLocal} should be used instead. The result quaternion is
    * a unit quaternion. The input quaternions are assumed to be unit
    * quaternions.
    * 
    * @param qr interpolated rotation result
    * @param vr interpolated angular velocity result in global coordinates (can
    * be set to <code>null</code> if not desired).
    * @param q0 unit quaternion giving rotation at the interval beginning
    * @param w0 angular velocity (in global coordinates) at the interval beginning
    * @param q1 unit quaternion giving rotation at the interval end
    * @param w1 angular velocity (in global coordinates) at the interval end
    * @param s interpolation location on the interval (in the range [0,1])
    * @param h interval time duration
    */   
   public static void sphericalHermiteGlobal (
      Quaternion qr, Vector3d vr,
      Quaternion q0, Vector3d w0, Quaternion q1, Vector3d w1,
      double s, double h) {

      Vector3d vx = new Vector3d();
      Quaternion qb = new Quaternion();
      Quaternion qx = new Quaternion();
      Quaternion qz = qr;
      if (qr == q0 || qr == q1) {
         qz = new Quaternion();
      }

      vx.scale (h/6, w0);
      q0.inverseTransform (vx, vx);
      qx.setExp (((s-3)*s+3)*s, vx);
      qz.mul (q0, qx);
      if (vr != null) {
         vx.scale ((3*s-6)*s+3);
         qz.transform (vr, vx);
      }
      // compute qa in qx
      qx.extrapolateWorld (q0, w0, h/3); 
      qb.extrapolateWorld (q1, w1, -h/3);      
      qx.mulInverseLeft (qx, qb);
      qx.log (vx);
      qx.setExp ((-2*s+3)*s*s, vx);
      qz.mul (qx);
      if (vr != null) {
         vx.scale ((-6*s+6)*s);
         qz.transform (vx, vx);
         vr.add (vx);
      }
      vx.scale (h/6, w1);
      q1.inverseTransform (vx, vx);
      qx.setExp (s*s*s, vx);
      qz.mul (qx);
      qz.normalize();
      if (vr != null) {
         vx.scale (3*s*s);
         qz.transform (vx, vx);
         vr.add (vx);
         vr.scale (2/h);
      }
      if (qz != qr) {
         qr.set (qz);
      }
   }
         
   /** 
    * Interpolates a point on a quaternion curve using cubic Hermite
    * interpolation. This method is identical to {@link
    * #sphericalHermiteGlobal} except that angular velocities are input (and
    * returned) in local instead of global coordinates.
    * 
    * @param qr interpolated rotation result
    * @param vr interpolated angular velocity result in local coordinates
    * (can be set to <code>null</code> if not desired).
    * @param q0 unit quaternion giving rotation at the interval beginning
    * @param w0 angular velocity (in local coordinates) at the interval beginning
    * @param q1 unit quaternion giving rotation at the interval end
    * @param w1 angular velocity (in local coordinates) at the interval end
    * @param s interpolation location on the interval (in the range [0,1])
    * @param h interval time duration
    */   
   public static void sphericalHermiteLocal (
      Quaternion qr, Vector3d vr,
      Quaternion q0, Vector3d w0, Quaternion q1, Vector3d w1,
      double s, double h) {

      Vector3d vx = new Vector3d();
      Quaternion qb = new Quaternion();
      Quaternion qx = new Quaternion();
      Quaternion qz = qr;
      if (qr == q0 || qr == q1) {
         qz = new Quaternion();
      }

      vx.scale (h/6, w0);
      qx.setExp (((s-3)*s+3)*s, vx);
      qz.mul (q0, qx);
      if (vr != null) {
         vx.scale ((3*s-6)*s+3);
         qz.transform (vr, vx);
      }
      // compute qa in qx
      qx.extrapolateLocal (q0, w0, h/3); 
      qb.extrapolateLocal (q1, w1, -h/3);      
      qx.mulInverseLeft (qx, qb);
      qx.log (vx);
      qx.setExp ((-2*s+3)*s*s, vx);
      qz.mul (qx);
      if (vr != null) {
         vx.scale ((-6*s+6)*s);
         qz.transform (vx, vx);
         vr.add (vx);
      }
      vx.scale (h/6, w1);
      qx.setExp (s*s*s, vx);
      qz.mul (qx);
      qz.normalize();
      if (vr != null) {
         vx.scale (3*s*s);
         qz.transform (vx, vx);
         vr.add (vx);
         vr.scale (2/h);
         qz.inverseTransform (vr, vr);
      }
      if (qz != qr) {
         qr.set (qz);
      }
   }
         
   /** 
    * Implements the 'squad' interpolation described by Ken Shoemake in his
    * 1987 SIGGRAPH course notes. This is also a Bezier curve interpolation
    * technique, but achieved using only three interpolation steps instead of
    * the six required by the de Casteljau algorithm. Details of squad can be
    * found in <a
    * href="http://www.geometrictools.com/Documentation/Quaternions.pdf">
    * Quaternion Algebra and Calculus</a>, by David Eberly. However, it should
    * be noted that the derivative information supplied in that paper is
    * incorrect, as mentioned in ``A Compact Differential Formula for the First
    * Derivative of a Unit Quaternion Curve'', by Kim, Kim, and Shin (Journal
    * of Visualization and Computer Animation. In fact, the derivative formula
    * is rather complicated, making interpolation of angular velocity
    * difficult. If angular velocity is also desired, one can use {@link
    * #sphericalHermiteLocal} or {@link #sphericalHermiteGlobal}.
    * 
    * @param qr result quaternion
    * @param q0 initial quaternion
    * @param qa first control point quaternion
    * @param qb second control point quaternion
    * @param q1 final quaternion
    * @param s parametric distance along the interal (in the range [0,1]).
    */
   public static void sphericalQuad (
      Quaternion qr, 
      Quaternion q0, Quaternion qa, Quaternion qb, Quaternion q1,
      double s) {

      Quaternion qu = new Quaternion();
      Quaternion qz = qr;
      // ensure that qr can also be an input
      if (qr == q0 || qr == qa || qr == qb || qr == q1) {
         qz = new Quaternion();
      }

      qu.sphericalInterpolate (q0, s, q1);
      // store qv in qz
      qz.sphericalInterpolate (qa, s, qb);
      qz.sphericalInterpolate (qu, 2*s*(1-s), qz);

      qz.normalize();
      if (qz != qr) {
         qr.set (qz);
      }

   }
         
   /**
    * Computes a spherical (great circle) interpolation between this quaternion
    * and q1 and places the result in this quaternion. It is assumed that both
    * are unit quaternions; otherwise, the results are undefined.
    * 
    * @param r
    * interpolation factor
    * @param q1
    * right-hand quaternion
    */
   public void sphericalInterpolate (double r, Quaternion q1) {
      sphericalInterpolate (this, r, q1);
   }

   /**
    * Computes a normalized rotational interpolation between this quaternion and
    * q1 and places the result in this quaternion. The result is a unit
    * quaternion.
    * 
    * @param r
    * interpolation factor
    * @param q1
    * right-hand quaternion
    * @see #normalizedInterpolate(Quaternion,double,Quaternion)
    */
   public void normalizedInterpolate (double r, Quaternion q1) {
      normalizedInterpolate (this, r, q1);
   }

   /** 
    * Interpolates a point on a quaternion curve using direct cubic Hermite
    * interpolation. The result final result is normalized. While relatively
    * fast, this scheme can be inaccurate for large displacements or changes in
    * velocity. In such cases, {@link #sphericalHermiteGlobal} or {@link
    * #sphericalHermiteLocal} should be used instead.
    *
    * <p>The inputs to the procedure consist of orientations and angular
    * velocities at the beginning and end-points of an interval, along with the
    * desired interpolation location (described by a parameter <code>s</code>)
    * and the length of time associated with the interval (<code>h</code>).
    * The angular velocities are supplied (and returned) in global coordinates.
    * If local coordinates are preferred, then the routine {@link
    * #normalizedHermiteLocal} should be used instead. The result quaternion is
    * a unit quaternion. The input quaternions are assumed to be unit
    * quaternions.
    * 
    * @param qr interpolated rotation result
    * @param vr interpolated angular velocity result in global coordinates (can
    * be set to <code>null</code> if not desired).
    * @param q0 unit quaternion giving rotation at the interval beginning
    * @param w0 angular velocity (in global coordinates) at the interval beginning
    * @param q1 unit quaternion giving rotation at the interval end
    * @param w1 angular velocity (in global coordinates) at the interval end
    * @param s interpolation location on the interval (in the range [0,1])
    * @param h interval time duration
    */   
   protected static void normalizedHermiteGlobal (
      Quaternion qr, Vector3d vr,
      Quaternion q0, Vector3d w0, Quaternion q1, Vector3d w1,
      double s, double h) {

      // TODO: implement
   }
         
   /** 
    * Interpolates a point on a quaternion curve using direct cubic Hermite
    * interpolation. This method is identical to {@link
    * #normalizedHermiteGlobal} except that angular velocities are input (and
    * returned) in local instead of global coordinates.
    * 
    * @param qr interpolated rotation result
    * @param vr interpolated angular velocity result in local coordinates
    * (can be set to <code>null</code> if not desired).
    * @param q0 unit quaternion giving rotation at the interval beginning
    * @param w0 angular velocity (in local coordinates) at the interval beginning
    * @param q1 unit quaternion giving rotation at the interval end
    * @param w1 angular velocity (in local coordinates) at the interval end
    * @param s interpolation location on the interval (in the range [0,1])
    * @param h interval time duration
    */   
   protected static void normalizedHermiteLocal (
      Quaternion qr, Vector3d vr,
      Quaternion q0, Vector3d w0, Quaternion q1, Vector3d w1,
      double s, double h) {

      // TODO: implement
   }
         
   /** 
    * Extrapolates a quaternion q1 by a constant angular velocity applied for
    * time t. It is assumed that the velocity is represented in the local
    * (rotated) coordinates associated with q1. This computation produces a
    * unit quaternion. q1 can be a non-unit quaternion.
    * 
    * @param q1 quaternion to extrapolate
    * @param w angular velocity used to form extrapolation
    * @param t extrapolation time
    */   
   public void extrapolateLocal (Quaternion q1, Vector3d w, double t) {
      double wmag = w.norm();
      double ang = wmag*t;

      double c2 = Math.cos(ang/2);
      double s2 = Math.sin(ang/2);
      if (Math.abs(ang) < DOUBLE_PREC) {
         normalize (q1);
         return;
      }

      // form the product q1 * (cos(ang/2), sin(ang/2)*wunit);      
      double stmp = q1.s*c2 - s2*q1.u.dot(w)/wmag;
      utmp.cross (q1.u, w);
      utmp.scaledAdd (q1.s, w, utmp);
      utmp.scale (s2/wmag);
      u.scaledAdd (c2, q1.u, utmp);
      s = stmp;
      normalize();
   }

   /** 
    * Extrapolates a quaternion q1 by a constant angular velocity applied for
    * time t. It is assumed that the velocity is represented in world
    * coordinates. This computation produces a unit quaternion. q1 can be a
    * non-unit quaternion.
    * 
    * @param q1 quaternion to extrapolate
    * @param w angular velocity used to form extrapolation
    * @param t extrapolation time
    */   
   public void extrapolateWorld (Quaternion q1, Vector3d w, double t) {
      double wmag = w.norm();
      double ang = wmag*t;
      double c2 = Math.cos(ang/2);
      double s2 = Math.sin(ang/2);

      // if (unitErr > (2 * DOUBLE_PREC) || unitErr < -(2 * DOUBLE_PREC)) {
      //    q1 = new Quaternion (q1);
      //    q1.scale (1 / Math.sqrt (lenSqr));
      // }

      if (Math.abs(ang) < DOUBLE_PREC) {
         normalize (q1);
         return;
      }

      // form the product (cos(ang/2), sin(ang/2)*wunit) * q1;      
      double stmp = c2*q1.s - s2*w.dot(q1.u)/wmag;
      utmp.cross (w, q1.u);
      utmp.scaledAdd (q1.s, w, utmp);
      utmp.scale (s2/wmag);
      u.scaledAdd (c2, q1.u, utmp);
      s = stmp;
      normalize();
   }

   /**
    * Computes <code>r q1</code> and add the result to this quaternion.
    * 
    * @param r
    * scaling factor
    * @param q1
    * quaternion to be scaled and added
    */
   public void scaledAdd (double r, Quaternion q1) {
      u.x += r * q1.u.x;
      u.y += r * q1.u.y;
      u.z += r * q1.u.z;
      s += r * q1.s;
   }

   /**
    * Computes <code>r q1 + q2</code> and places the result in this
    * quaternion.
    * 
    * @param r
    * scaling factor
    * @param q1
    * quaternion to be scaled
    * @param q2
    * quaternion to be added
    */
   public void scaledAdd (double r, Quaternion q1, Quaternion q2) {
      u.x = r * q1.u.x + q2.u.x;
      u.y = r * q1.u.y + q2.u.y;
      u.z = r * q1.u.z + q2.u.z;
      s = r * q1.s + q2.s;
   }

   /**
    * Computes <code>r1 q1 + r2 q2</code> and places the result in this
    * quaternion.
    * 
    * @param r1
    * left-hand scaling factor
    * @param q1
    * left-hand quaternion
    * @param r2
    * right-hand scaling factor
    * @param q2
    * right-hand quaternion
    */
   public void combine (double r1, Quaternion q1, double r2, Quaternion q2) {
      u.x = r1 * q1.u.x + r2 * q2.u.x;
      u.y = r1 * q1.u.y + r2 * q2.u.y;
      u.z = r1 * q1.u.z + r2 * q2.u.z;
      s = r1 * q1.s + r2 * q2.s;
   }

   /**
    * Returns the 2 norm of this quaternion. This is the square root of the sum
    * of the squares of the elements.
    * 
    * @return quaternion 2 norm
    */
   public double length() {
      return Math.sqrt (u.x * u.x + u.y * u.y + u.z * u.z + s * s);
   }

   /**
    * Returns the square of the 2 norm of this quaternion. This is the sum of
    * the squares of the elements.
    * 
    * @return square of the 2 norm
    */
   public double lengthSquared() {
      return u.x * u.x + u.y * u.y + u.z * u.z + s * s;
   }

   /**
    * Sets this quaternion to the conjugate of q1. Conjugation entails negating
    * the values of u.
    * 
    * @param q1
    * quaternion to conjugate
    */
   public void conjugate (Quaternion q1) {
      u.negate (q1.u);
      s = q1.s;
   }

   /**
    * Conjugates this quaternion in place. Conjugation entails negating the
    * values of u.
    */
   public void conjugate() {
      u.negate();
   }

   /**
    * Sets this quaternion to the inverse of q1.
    * 
    * @param q1
    * quaternion to invert
    */
   public void invert (Quaternion q1) {
      double magSqr = q1.s * q1.s + q1.u.dot (q1.u);
      s = q1.s / magSqr;
      u.scale (-1 / magSqr, q1.u);
   }

   /**
    * Inverts this quaternion in place.
    */
   public void invert() {
      invert (this);
   }

   /**
    * Multiplies this quaternion by quaternion q1 and places the result in this
    * quaternion.
    * 
    * @param q1
    * right-hand quaternion
    */
   public void mul (Quaternion q1) {
      mul (this, q1);
   }

   /**
    * Multiplies quaternion q1 by quaternion q2 and places the result in this
    * quaternion.
    * 
    * @param q1
    * left-hand quaternion
    * @param q2
    * right-hand quaternion
    */
   public void mul (Quaternion q1, Quaternion q2) {
      double stmp = q1.s * q2.s - q1.u.dot (q2.u);
      utmp.cross (q1.u, q2.u);
      utmp.scaledAdd (q1.s, q2.u, utmp);
      u.scaledAdd (q2.s, q1.u, utmp);
      s = stmp;
   }

   /**
    * Post-multiplies this quaternion by the inverse of quaternion q1 and places
    * the result in this quaternion.
    * 
    * @param q1
    * right-hand quaternion
    */
   public void mulInverse (Quaternion q1) {
      mulInverseRight (this, q1);
   }

   /**
    * Multiplies quaternion q1 by the inverse of quaternion q2 and places the
    * result in this quaternion.
    * 
    * @param q1
    * left-hand quaternion
    * @param q2
    * right-hand quaternion
    */
   public void mulInverseRight (Quaternion q1, Quaternion q2) {
      double d = q2.s * q2.s + q2.u.dot (q2.u);
      double stmp = q1.s * q2.s + q1.u.dot (q2.u);

      utmp.cross (q2.u, q1.u);
      utmp.scaledAdd (-q1.s, q2.u, utmp);
      utmp.scaledAdd (q2.s, q1.u, utmp);
      u.scale (1 / d, utmp);
      s = stmp / d;
   }

   /**
    * Multiplies the inverse of quaternion q1 by quaternion q2 and places the
    * result in this quaternion.
    * 
    * @param q1
    * left-hand quaternion
    * @param q2
    * right-hand quaternion
    */
   public void mulInverseLeft (Quaternion q1, Quaternion q2) {
      double d = q1.s * q1.s + q1.u.dot (q1.u);
      double stmp = q1.s * q2.s + q1.u.dot (q2.u);

      utmp.cross (q2.u, q1.u);
      utmp.scaledAdd (q1.s, q2.u, utmp);
      utmp.scaledAdd (-q2.s, q1.u, utmp);
      u.scale (1 / d, utmp);
      s = stmp / d;
   }

   /**
    * Multiplies the inverse of quaternion q1 by the inverse of quaternion q2
    * and places the result in this quaternion.
    * 
    * @param q1
    * left-hand quaternion
    * @param q2
    * right-hand quaternion
    */
   public void mulInverseBoth (Quaternion q1, Quaternion q2) {
      double stmp = q2.s * q1.s - q2.u.dot (q1.u);
      utmp.cross (q2.u, q1.u);
      utmp.scaledAdd (q2.s, q1.u, utmp);
      utmp.scaledAdd (q1.s, q2.u, utmp);

      double d = stmp * stmp + utmp.dot (utmp);
      u.scale (-1 / d, utmp);
      s = stmp / d;
   }

   /**
    * Returns the Euclidean distance between this quaternion and quaternion q.
    * 
    * @return distance between this quaternion and q
    */
   public double distance (Quaternion q) {
      double dx = u.x - q.u.x;
      double dy = u.y - q.u.y;
      double dz = u.z - q.u.z;
      double dw = s - q.s;

      return Math.sqrt (dx * dx + dy * dy + dz * dz + dw * dw);
   }

   /**
    * Returns the squared of the Euclidean distance between this quaternion and
    * quaternion q.
    * 
    * @return squared distance between this quaternion and q
    */
   public double distanceSquared (Quaternion q) {
      double dx = u.x - q.u.x;
      double dy = u.y - q.u.y;
      double dz = u.z - q.u.z;
      double dw = s - q.s;

      return (dx * dx + dy * dy + dz * dz + dw * dw);
   }

   /**
    * Returns the infinity norm of this quaternion. This is the maximum absolute
    * value over all elements, treating it as a 4-vector.
    * 
    * @return quaternion infinity norm
    */
   public double infinityNorm() {
      double max = Math.abs (u.x);
      if (Math.abs (u.y) > max) {
         max = Math.abs (u.y);
      }
      if (Math.abs (u.z) > max) {
         max = Math.abs (u.z);
      }
      if (Math.abs (s) > max) {
         max = Math.abs (s);
      }
      return max;
   }

   /**
    * Returns the 1 norm of this quaternion. This is the sum of the absolute
    * values of the elements, treating it as a 4-vector.
    * 
    * @return quaternion 1 norm
    */
   public double oneNorm() {
      return Math.abs (s) + Math.abs (u.x) + Math.abs (u.y) + Math.abs (u.z);
   }

   /**
    * Returns the dot product of this quaternion and q1.
    * 
    * @param q1
    * right-hand quaternion
    * @return dot product
    */
   public double dot (Quaternion q1) {
      return u.x * q1.u.x + u.y * q1.u.y + u.z * q1.u.z + s * q1.s;
   }

   /**
    * Returns the angular distance between the rotations represented by this
    * quaternion and q1. The returned value is in the range 0 to Math.PI.
    * 
    * @param q1
    * right-hand quaternion
    * @return angular rotational distance, in radians
    */
   public double rotationAngle (Quaternion q1) {
      // compute inv(this) * q1, except for the scale factor
      double stmp = s * q1.s + u.dot (q1.u);

      utmp.cross (q1.u, u);
      utmp.scaledAdd (s, q1.u, utmp);
      utmp.scaledAdd (-q1.s, u, utmp);

      // now compute the angle

      double ang = 2 * Math.atan2 (utmp.norm(), stmp);
      if (ang > Math.PI) {
         return Math.max (2 * Math.PI - ang, 0);
      }
      else {
         return ang;
      }
   }

   /**
    * Normalizes this quaternion in place.
    */
   public void normalize() {
      double lenSqr = s*s + u.x*u.x + u.y*u.y + u.z*u.z;
      double err = lenSqr - 1;
      if (err > (2*DOUBLE_PREC) || err < -(2*DOUBLE_PREC)) {
         if (lenSqr == 0) {
            set (1, 0, 0, 0);
         }
         else {
            // we can divide or scale. We divide for now, even though it may be
            // slower, since it leaves the regression test results in the
            // ultra-sensitive rigid body collision code unchanged.
            double len = Math.sqrt (lenSqr);
            u.x /= len;
            u.y /= len;
            u.z /= len;
            s /= len;
            // double scale = 1/Math.sqrt (lenSqr);
            // u.x = q1.u.x * scale;
            // u.y = q1.u.y * scale;
            // u.z = q1.u.z * scale;
            // s = q1.s * scale;
         }
      }
   }

   /**
    * Computes a unit quaternion in the direction of q1 and places the result in
    * this quaternion.
    * 
    * @param q1
    * quaternion to normalize
    */
   public void normalize (Quaternion q1) {
      double lenSqr =
         q1.s * q1.s + q1.u.x * q1.u.x + q1.u.y * q1.u.y + q1.u.z * q1.u.z;
      double err = lenSqr - 1;
      if (err > (2 * DOUBLE_PREC) || err < -(2 * DOUBLE_PREC)) {
         if (lenSqr == 0) {
            set (1, 0, 0, 0);
         }
         else {
            // we can divide or scale. See comment in previous function.
            double len = Math.sqrt (lenSqr);
            u.x = q1.u.x / len;
            u.y = q1.u.y / len;
            u.z = q1.u.z / len;
            s = q1.s / len;
            // double scale = 1/Math.sqrt (lenSqr);
            // u.x = q1.u.x * scale;
            // u.y = q1.u.y * scale;
            // u.z = q1.u.z * scale;
            // s = q1.s * scale;
         }
      }
      else {
         u.x = q1.u.x;
         u.y = q1.u.y;
         u.z = q1.u.z;
         s = q1.s;
      }
   }

   /**
    * Returns true if the elements of this quaternion equal those of quaternion
    * <code>q1</code>within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param q1
    * quaternion to compare with
    * @param eps
    * comparison tolerance
    * @return false if the vectors are not equal within the specified tolerance
    */
   public boolean epsilonEquals (Quaternion q1, double eps) {
      return (Math.abs (u.x - q1.u.x) <= eps && Math.abs (u.y - q1.u.y) <= eps &&
              Math.abs (u.z - q1.u.z) <= eps && Math.abs (s - q1.s) <= eps);
   }

   /**
    * Returns true if the elements of this quaternion exactly equal those of
    * quaternion <code>q1</code>.
    * 
    * @param q1
    * quaternion to compare with
    * @return false if the vectors are not equal
    */
   public boolean equals (Quaternion q1) {
      return (u.x == q1.u.x && u.y == q1.u.y && u.z == q1.u.z && s == q1.s);
   }

   /**
    * Sets this quaternion to a unit quaternion corresponding to a specified
    * rotation.
    * 
    * @param axisAng
    * gives the rotation axis and corresponding angle
    */
   public void setAxisAngle (AxisAngle axisAng) {
      double cos = Math.cos (axisAng.angle / 2);
      double sin = Math.sin (axisAng.angle / 2);

      s = cos;
      u.normalize (axisAng.axis);
      u.scale (sin);
   }
   
   public void setAxisAngle(Vector3d axis, double angle) {
      double cos = Math.cos (angle / 2);
      double sin = Math.sin (angle / 2);

      s = cos;
      u.normalize (axis);
      u.scale (sin);
   }
   
   public void set(AxisAngle axisAng) {
      setAxisAngle(axisAng);
   }

   /**
    * Sets this quaternion to a unit quaternion corresponding to a specified
    * rotation.
    * 
    * @param R
    * rotation specified as a matrix
    */
   public void set (RotationMatrix3d R) {
      // Based on the follwoing algorithm:
      // 
      // Let the elements of the matrix be given by Q_ij. For i,j,k
      // such that epsilon (i,j,k) = 1, use the maximum of
      // 
      // q_i = 1/2 sqrt (1 + Q_ii - Q_jj - Q_kk)
      // q = 1/2 sqrt (1 + Q_ii + Q_jj + Q_kk)
      // 
      // and the relations
      // 
      // q_j q_k = 1/4 (Q_kj + Q_jk)
      // q q_i = 1/4 (Q_kj - Q_jk)
      // 
      // We use this algorithm because it is numerically well behaved.

      final int S_MAX = 1;
      final int X_MAX = 2;
      final int Y_MAX = 3;
      final int Z_MAX = 4;

      int max;
      double sumC, sumX, sumY, sumZ;
      double a;

      sumC = R.m00 + R.m11 + R.m22;
      sumX = R.m00 - R.m11 - R.m22;
      sumY = R.m11 - R.m22 - R.m00;
      sumZ = R.m22 - R.m00 - R.m11;

      if (sumC > sumX) {
         if (sumY > sumZ) {
            max = (sumC > sumY) ? S_MAX : Y_MAX;
         }
         else {
            max = (sumC > sumZ) ? S_MAX : Z_MAX;
         }
      }
      else {
         if (sumY > sumZ) {
            max = (sumX > sumY) ? X_MAX : Y_MAX;
         }
         else {
            max = (sumX > sumZ) ? X_MAX : Z_MAX;
         }
      }

      switch (max) {
         case S_MAX:
            /* printf ("S_MAX\n"); */

            a = Math.sqrt (1.0 + sumC) * 0.5;
            s = a;
            a *= 4.0;
            u.x = (R.m21 - R.m12) / a;
            u.y = (R.m02 - R.m20) / a;
            u.z = (R.m10 - R.m01) / a;
            break;
         case X_MAX:
            /* printf ("X_MAX\n"); */

            a = Math.sqrt (1.0 + sumX) * 0.5;
            u.x = a;
            a *= 4.0;
            s = (R.m21 - R.m12) / a;
            u.y = (R.m01 + R.m10) / a;
            u.z = (R.m02 + R.m20) / a;
            break;
         case Y_MAX:
            /* printf ("Y_MAX\n"); */

            a = Math.sqrt (1.0 + sumY) * 0.5;
            u.y = a;
            a *= 4.0;
            s = (R.m02 - R.m20) / a;
            u.x = (R.m01 + R.m10) / a;
            u.z = (R.m21 + R.m12) / a;
            break;
         case Z_MAX:
            /* printf ("Z_MAX\n"); */

            a = Math.sqrt (1.0 + sumZ) * 0.5;
            u.z = a;
            a *= 4.0;
            s = (R.m10 - R.m01) / a;
            u.y = (R.m21 + R.m12) / a;
            u.x = (R.m02 + R.m20) / a;
            break;
      }
      if (s < 0.0) {
         s = -s;
         u.x = -u.x;
         u.y = -u.y;
         u.z = -u.z;
      }
      // TODO
   }

   /**
    * Sets the elements of this quaternion to zero.
    */
   public void setZero() {
      u.x = 0;
      u.y = 0;
      u.z = 0;
      s = 0;
   }

   /**
    * Sets the elements of this quaternion to the prescribed values.
    * 
    * @param s
    * scalar value
    * @param ux
    * first vector value
    * @param uy
    * second vector value
    * @param uz
    * thirs vector value
    */
   public void set (double s, double ux, double uy, double uz) {
      this.u.x = ux;
      this.u.y = uy;
      this.u.z = uz;
      this.s = s;
   }

   /**
    * Sets the elements of this quaternion to the prescribed values.
    * 
    * @param s
    * scalar value
    * @param u 
    * vector value
    */
   public void set (double s, Vector3d u) {
      this.u.x = u.x;
      this.u.y = u.y;
      this.u.z = u.z;
      this.s = s;
   }

   /**
    * Sets this quaternion to a random unit value.
    */
   public void setRandomUnit() {
      setRandom();
      normalize();
   }

   /** 
    * Transforms a vector v1 by the rotation implied by this quaternion. More
    * specifically, this method converts v1 from rotated coordinates to base
    * coordinates, by forming the product
    * <pre>
    * vr = q (0, v1) inv(q)
    * </pre>
    * where q is this quaternion.
    * 
    * @param vr result vector
    * @param v1 vector to be transformed
    */
   public void transform (Vector3d vr, Vector3d v1) {
      double dot = u.dot(v1);
      utmp.cross (u, v1);
      vr.scale (s*s, v1);
      vr.scaledAdd (2*s, utmp);
      vr.crossAdd (u, utmp, vr);
      vr.scaledAdd (dot, u);
   }

   /** 
    * Transforms a vector v1 by the inverse rotation implied by this
    * quaternion. More specifically, this method converts v1 from world
    * coordinates to rotated coordinates, by forming the product
    * <pre>
    * vr = inv(q) (0, v1) q
    * </pre>
    * where q is this quaternion.
    * 
    * @param vr result vector
    * @param v1 vector to be transformed
    */
   public void inverseTransform (Vector3d vr, Vector3d v1) {
      double dot = u.dot(v1);
      utmp.cross (v1, u);
      vr.scale (s*s, v1);
      vr.scaledAdd (2*s, utmp);
      vr.crossAdd (utmp, u, vr);
      vr.scaledAdd (dot, u);
   }

   /**
    * Sets this quaternion to a random unit value, using a supplied random
    * number generator.
    * 
    * @param generator
    * random number generator
    */
   public void setRandomUnit (Random generator) {
      setRandom (-0.5, 0.5, generator);
      normalize();
   }

   /**
    * Sets the elements of this quaternion to uniformly distributed random
    * values in the range -0.5 (inclusive) to 0.5 (exclusive).
    */
   public void setRandom() {
      super.setRandom();
   }

   /**
    * Sets the elements of this quaternion to uniformly distributed random
    * values in a specified range.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    */
   public void setRandom (double lower, double upper) {
      super.setRandom (lower, upper);
   }

   /**
    * Sets the elements of this quaternion to uniformly distributed random
    * values in a specified range, using a supplied random number generator.
    * 
    * @param lower
    * lower random value (inclusive)
    * @param upper
    * upper random value (exclusive)
    * @param generator
    * random number generator
    */
   public void setRandom (double lower, double upper, Random generator) {
      super.setRandom (lower, upper, generator);
   }

   public String axisAngleString() {
      NumberFormat afmt = new NumberFormat ("%8.3f");
      double mag = u.norm();
      double ang = Math.atan2 (mag, s);
      if (ang > Math.PI/2) {
         ang = ang - Math.PI;
         mag = -mag;
      }
      Vector3d axis = new Vector3d();
      if (Math.abs (ang) < DOUBLE_PREC) {
         ang = 0;
      }
      else {
         axis.scale (1/mag, u);
      }
      return afmt.format (2*Math.toDegrees(ang))+" "+axis.toString ("%8.5f");
   }

   public Quaternion clone() {
      Quaternion q = (Quaternion)super.clone();
      q.u = new Vector3d(u);
      q.utmp = new Vector3d();
      return q;
   }
}

