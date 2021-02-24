/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.util.Random;

import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;

/**
 * A specialized 3 x 3 orthogonal matrix, with determinant 1, that described a
 * three-dimensional rotation.
 * 
 * <p>
 * This is used to rotate a 3 dimensional vector from one coordinate frame into
 * another. If v0 and v1 denote the vector in the original frame 0 and target
 * frame 1, respectively, then the rotation is computed according to
 * 
 * <pre>
 * v1 = R v0
 * </pre>
 * 
 * where R is the rotation matrix. The columns of R represent the directions of
 * the axes of frame 0 with respect to frame 1.
 * 
 * <p>
 * If R01 is a rotation from frame 0 to frame 1, and R12 is a rotation from
 * frame 1 to frame 2, then the rotation from frame 0 to frame 2 is given by the
 * product
 * 
 * <pre>
 * R02 = R12 R01
 * </pre>
 * 
 * In this way, a rotation can be created by multiplying a series of
 * sub-rotations.
 * 
 * <p>
 * If R01 is a rotation from frame 0 to frame 1, then the inverse rotation R10
 * is a rotation from frame 1 to frame 0, and is given by the transpose of R.
 */
public class RotationMatrix3d extends Matrix3dBase {
   private static final long serialVersionUID = 1L;

   /**
    * Global identity rotation. Should not be modified.
    */
   public static final RotationMatrix3d IDENTITY = new RotationMatrix3d();

   /**
    * Global rotation defined by rotating 90 degrees counter-clockwise about
    * the x axis. If used to defined an "eye" to "world" viewing transform, the
    * resulting z direction in eye coordinates is "up".  Should not be
    * modified.
    */
   public static final RotationMatrix3d ROT_X_90 =
      new RotationMatrix3d (1, 0, 0, 0, 0, -1, 0, 1, 0);      

   /**
    * Specifies a string representation of this rotation as a 4-tuple consisting
    * of a rotation axis and the corresponding angle (in degrees).
    */
   public static final int AXIS_ANGLE_STRING = 1;

   /**
    * Specifies a string representation of this rotation as a 3 x 3 matrix.
    */
   public static final int MATRIX_STRING = 2;

   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;
   
   /**
    * Creates a new rotation initialized to the specified entries.
    *
    * <p> NOTE: the user is responsible for ensuring the that specified matrix
    * is orthogonal.
    */
   public RotationMatrix3d (double m00, double m01, double m02, 
                            double m10, double m11, double m12, 
                            double m20, double m21, double m22) {

      this.m00 = m00;
      this.m01 = m01;
      this.m02 = m02;

      this.m10 = m10;
      this.m11 = m11;
      this.m12 = m12;

      this.m20 = m20;
      this.m21 = m21;
      this.m22 = m22;
   }
   
   /**
    * Creates a new rotation initialized to the specified entries. Stored in row major.
    *
    * <p> NOTE: the user is responsible for ensuring the that specified matrix
    * is orthogonal.
    */
   public RotationMatrix3d (double[] m) {

      this.m00 = m[0];
      this.m01 = m[1];
      this.m02 = m[2];

      this.m10 = m[3];
      this.m11 = m[4];
      this.m12 = m[5];

      this.m20 = m[6];
      this.m21 = m[7];
      this.m22 = m[8];
   }

   /**
    * Creates a new rotation initialized to the identity.
    */
   public RotationMatrix3d() {
      m00 = 1.0;
      m01 = 0.0;
      m02 = 0.0;

      m10 = 0.0;
      m11 = 1.0;
      m12 = 0.0;

      m20 = 0.0;
      m21 = 0.0;
      m22 = 1.0;
   }

   /**
    * Creates a new rotation specified by a rotation about an axis.
    * 
    * @param axisAng
    * gives the rotation axis and corresponding angle
    */
   public RotationMatrix3d (AxisAngle axisAng) {
      setAxisAngle (axisAng);
   }

   /**
    * Creates a new rotation specified by a quaternion.
    * 
    * @param quat
    * quaternion specifying the rotation.
    */
   public RotationMatrix3d (Quaternion quat) {
      set (quat);
   }

   /**
    * Creates a new rotation specified by a rotation about an axis.
    * 
    * @param u
    * gives the rotation axis
    * @param ang
    * rotation angle (radians)
    */
   public RotationMatrix3d (Vector3d u, double ang) {
      setAxisAngle (u, ang);
   }

   /**
    * Creates a new rotation specified by a rotation about an axis.
    * 
    * @param ux
    * axis x coordinate
    * @param uy
    * axis y coordinate
    * @param uz
    * axis z coordinate
    * @param ang
    * angle of rotation about the axis (radians)
    */
   public RotationMatrix3d (double ux, double uy, double uz, double ang) {
      setAxisAngle (ux, uy, uz, ang);
   }

   /**
    * Creates a new rotation which is a copy of an existing one.
    * 
    * @param R
    * rotation to copy
    */
   public RotationMatrix3d (RotationMatrix3d R) {
      set (R);
   }

   /**
    * Post-multiplies this rotation by another and places the result in this
    * rotation.
    * 
    * @param R1
    * rotation to multiply by
    */
   public void mul (RotationMatrix3d R1) {
      super.mul (R1);
   }

   /**
    * Multiplies rotation R1 by R2 and places the result in this rotation.
    * 
    * @param R1
    * first rotation
    * @param R2
    * second rotation
    */
   public void mul (RotationMatrix3d R1, RotationMatrix3d R2) {
      super.mul (R1, R2);
   }

   /**
    * Post-multiplies this rotation by the inverse of rotation R1 and places the
    * result in this rotation.
    * 
    * @param R1
    * right-hand rotation
    */
   public void mulInverse (RotationMatrix3d R1) {
      // double scaleSquared = R1.m00*R1.m00 + R1.m10*R1.m10 + R1.m20*R1.m20;
      super.mulTranspose (R1);
      // if (Math.abs(scaleSquared-1) > EPS)
      // { super.scale (1/scaleSquared);
      // }
   }

   /**
    * Multiplies the inverse of rotation R1 by rotation R2 and places the result
    * in this rotation.
    * 
    * @param R1
    * left-hand rotation
    * @param R2
    * right-hand rotation
    */
   public void mulInverseLeft (RotationMatrix3d R1, RotationMatrix3d R2) {
      super.mulTransposeLeft (R1, R2);
   }

   /**
    * Multiplies rotation R1 by the inverse of rotation R2 and places the result
    * in this rotation.
    * 
    * @param R1
    * left-hand rotation
    * @param R2
    * right-hand rotation
    */
   public void mulInverseRight (RotationMatrix3d R1, RotationMatrix3d R2) {
      super.mulTransposeRight (R1, R2);
   }

   /**
    * Multiplies the inverse of rotation R1 by the inverse of rotation R2 and
    * places the result in this rotation.
    * 
    * @param R1
    * left-hand rotation
    * @param R2
    * right-hand rotation
    */
   public void mulInverseBoth (RotationMatrix3d R1, RotationMatrix3d R2) {
      super.mulTransposeBoth (R1, R2);
   }

   /**
    * Inverts this rotation in place. This is the same as taking the transpose
    * of the matrix.
    */
   public boolean invert() {
      super.transpose();
      return true;
   }

   /**
    * Inverts rotation R1 and places the result in this rotation. This is the
    * same as taking the transpose of R1.
    * 
    * @param R1
    * rotation to invert
    */
   public boolean invert (RotationMatrix3d R1) {
      super.transpose (R1);
      return true;
   }

   /**
    * Transposes this rotation in place. This is the same as taking the inverse
    * of the matrix.
    */
   public void transpose() {
      super.transpose();
   }

   /**
    * Transposes rotation R1 and places the result in this rotation. This is the
    * same as taking the invesre of R1.
    * 
    * @param R1
    * rotation to transpose
    */
   public void transpose (RotationMatrix3d R1) {
      super.transpose (R1);
   }

   /**
    * Sets this rotation to the negative of R1.
    * 
    * @param R1
    * rotation to negate
    */
   protected void negate (RotationMatrix3d R1) {
      super.negate (R1);
   }

   /**
    * Negates this rotation in place.
    */
   public void negate() {
      super.negate (this);
   }

   /**
    * {@inheritDoc}
    * 
    * @return true (matrix is never singular)
    */
   public boolean mulInverse (Vector3d vr, Vector3d v1) {
      mulTranspose (vr, v1);
      return true;
   }

   /**
    * {@inheritDoc}
    * 
    * @return true (matrix is never singular)
    */
   public boolean mulInverse (Vector3d vr) {
      mulTranspose (vr, vr);
      return true;
   }

   /**
    * Multiplies the column vector v1 by the inverse transpose of this matrix
    * and places the result in vr. For a rotation matrix, this is equivalent to
    * simply multiplying by the matrix.
    * 
    * @param vr
    * result vector
    * @param v1
    * vector to multiply by
    * @return true (matrix is never singular)
    */
   public boolean mulInverseTranspose (Vector3d vr, Vector3d v1) {
      mul (vr, v1);
      return true;
   }

   /**
    * Multiplies vector vr by the inverse transpose of this matrix, in place.
    * For a rotation matrix, this is equivalent to simply multiplying by the
    * matrix.
    * 
    * @param vr
    * vector to multiply
    * @return true (matrix is never singular)
    */
   public boolean mulInverseTranspose (Vector3d vr) {
      mul (vr, vr);
      return true;
   }

   /**
    * Normalizes this rotation, so as to ensure that its its columns are
    * orthonormal. This is done as follows: the third column is renormalized,
    * then the first column is recomputed as the normalized cross product of the
    * second and third columns, and finally the second column is recomputed as
    * the cross product of the third and first columns.
    */
   public void normalize() {
      // a = unit(a)
      // n = unit(oXa)
      // o = aXn

      double mag;

      // normalize third column:

      mag = Math.sqrt (m02 * m02 + m12 * m12 + m22 * m22);
      m02 /= mag;
      m12 /= mag;
      m22 /= mag;

      // recompute first column:

      m00 = m11 * m22 - m21 * m12;
      m10 = m21 * m02 - m01 * m22;
      m20 = m01 * m12 - m11 * m02;

      mag = Math.sqrt (m00 * m00 + m10 * m10 + m20 * m20);
      m00 /= mag;
      m10 /= mag;
      m20 /= mag;

      // recompute last column
      m01 = m12 * m20 - m22 * m10;
      m11 = m22 * m00 - m02 * m20;
      m21 = m02 * m10 - m12 * m00;
   }

   // public void transform (Vector3d vr, Vector3d v1)
   // {
   // super.mul (vr, v1);
   // }

   // public void transform (Vector3d vr)
   // {
   // super.mul (vr);
   // }

   // public void inverseTransform (Vector3d vr, Vector3d v1)
   // {
   // super.mulTranspose (vr, v1);
   // }

   // public void inverseTransform (Vector3d vr)
   // {
   // super.mulTranspose (vr);
   // }

   /**
    * Sets this rotation using two vectors to indicate the directions of the x
    * and y axes. The z axis is then formed from their cross product. The x, y,
    * and z axes correspond to the first, second, and third matrix columns.
    * {@code xdir} and {@code ydir} must be non-zero and non-parallel or an
    * exception will be thrown.
    *
    * <p>
    * {@code xdir} and {@code ydir} do not need to be perpendicular.
    * The x axis is formed by normalizing xdir. The z axis is then found by
    * normalizing the cross product of x and ydir. Finally, the y axis is
    * determined from the cross product of the z and x axes.
    * 
    * @param xdir
    * direction for the x axis
    * @param ydir
    * indicates direction for the y axis (not necessarily perpendicular to
    * {@code xdir})
    * @throws IllegalArgumentException if {@code xdir} or {@code ydir}
    * are zero, or if they are parallel.
    */
   public void setXYDirections (Vector3d xdir, Vector3d ydir) {
      Vector3d zdir = new Vector3d();
      double mag = xdir.norm();
      if (mag == 0) {
         throw new IllegalArgumentException ("xdir is zero");
      }
      m00 = xdir.x/mag;
      m10 = xdir.y/mag;
      m20 = xdir.z/mag;
      zdir.cross (xdir, ydir);
      mag = zdir.norm();
      if (mag == 0) {
         throw new IllegalArgumentException (
            "ydir is zero, or xdir and ydir are parallel");
      }
      m02 = zdir.x/mag;
      m12 = zdir.y/mag;
      m22 = zdir.z/mag;

      m01 = m12 * m20 - m22 * m10;
      m11 = m22 * m00 - m02 * m20;
      m21 = m02 * m10 - m12 * m00;
   }

   /**
    * Sets this rotation using two vectors to indicate the directions of the y
    * and z axes. The y axis is then formed from their cross product. The x, y,
    * and z axes correspond to the first, second, and third matrix columns.
    * {@code ydir} and {@code zdir} must be non-zero and non-parallel or an
    * exception will be thrown.
    *
    * <p>
    * {@code ydir} and {@code zdir} do not need to be perpendicular.
    * The y axis is formed by normalizing ydir. The x axis is then found by
    * normalizing the cross product of y and zdir. Finally, the z axis is
    * determined from the cross product of the x and y axes.
    * 
    * @param ydir
    * direction for the y axis
    * @param zdir
    * indicates direction for the z axis (not necessarily perpendicular to
    * {@code ydir})
    * @throws IllegalArgumentException if {@code ydir} or {@code zdir}
    * are zero, or if they are parallel.
    */
   public void setYZDirections (Vector3d ydir, Vector3d zdir) {
      Vector3d xdir = new Vector3d();
      double mag = ydir.norm();
      if (mag == 0) {
         throw new IllegalArgumentException ("ydir is zero");
      }
      m01 = ydir.x/mag;
      m11 = ydir.y/mag;
      m21 = ydir.z/mag;
      xdir.cross (ydir, zdir);
      mag = xdir.norm();
      if (mag == 0) {
         throw new IllegalArgumentException (
            "zdir is zero, or ydir and zdir are parallel");
      }
      m00 = xdir.x/mag;
      m10 = xdir.y/mag;
      m20 = xdir.z/mag;

      m02 = m10 * m21 - m20 * m11;
      m12 = m20 * m01 - m00 * m21;
      m22 = m00 * m11 - m10 * m01;
   }

   /**
    * Sets this rotation using two vectors to indicate the directions of the z
    * and x axes. The y axis is then formed from their cross product. The x, y,
    * and z axes correspond to the first, second, and third matrix columns.
    * {@code zdir} and {@code xdir} must be non-zero and non-parallel or an
    * exception will be thrown.
    *
    * <p>
    * {@code zdir} and {@code xdir} do not need to be perpendicular.
    * The z axis is formed by normalizing zdir. The y axis is then found by
    * normalizing the cross product of z and xdir. Finally, the x axis is
    * determined from the cross product of the y and z axes.
    * 
    * @param zdir
    * direction for the z axis
    * @param xdir
    * indicates direction for the x axis (not necessarily perpendicular to
    * {@code xdir})
    * @throws IllegalArgumentException if {@code zdir} or {@code xdir}
    * are zero, or if they are parallel.
    */
   public void setZXDirections (Vector3d zdir, Vector3d xdir) {
      Vector3d ydir = new Vector3d();
      double mag = zdir.norm();
      if (mag == 0) {
         throw new IllegalArgumentException ("zdir is zero");
      }
      m02 = zdir.x/mag;
      m12 = zdir.y/mag;
      m22 = zdir.z/mag;
      ydir.cross (zdir, xdir);
      mag = ydir.norm();
      if (mag == 0) {
         throw new IllegalArgumentException (
            "xdir is zero, or zdir and xdir are parallel");
      }
      m01 = ydir.x/mag;
      m11 = ydir.y/mag;
      m21 = ydir.z/mag;

      m00 = m11 * m22 - m21 * m12;
      m10 = m21 * m02 - m01 * m22;
      m20 = m01 * m12 - m11 * m02;
   }

   /**
    * Sets this rotation to one represented by the quaternion q.
    * 
    * @param q
    * quaternion representing the rotation
    */
   public void set (Quaternion q) {
      double d = (q.s * q.s + q.u.dot (q.u)) / 2;

      double s_d = q.s / d;
      double ux_d = q.u.x / d;
      double uy_d = q.u.y / d;
      double uz_d = q.u.z / d;

      double uxx = q.u.x * ux_d;
      double uyy = q.u.y * uy_d;
      double uzz = q.u.z * uz_d;

      double uxy = q.u.x * uy_d;
      double uxz = q.u.x * uz_d;
      double uyz = q.u.y * uz_d;

      double sux = s_d * q.u.x;
      double suy = s_d * q.u.y;
      double suz = s_d * q.u.z;

      m00 = 1 - uyy - uzz;
      m10 = uxy + suz;
      m20 = uxz - suy;

      m01 = uxy - suz;
      m11 = 1 - uxx - uzz;
      m21 = uyz + sux;

      m02 = uxz + suy;
      m12 = uyz - sux;
      m22 = 1 - uxx - uyy;
   }

   /**
    * Sets this rotation explicitly from the supplied matrix entries.  This
    * method calls {@link #normalize normalize} to ensure that the resulting
    * matrix is orthogonal.
    * 
    * @param m00
    * element (0,0)
    * @param m01
    * element (0,1)
    * @param m02
    * element (0,2)
    * @param m10
    * element (1,0)
    * @param m11
    * element (1,1)
    * @param m12
    * element (1,2)
    * @param m20
    * element (2,0)
    * @param m21
    * element (2,1)
    * @param m22
    * element (2,2)
    */
   public void set (
      double m00, double m01, double m02,
      double m10, double m11, double m12,
      double m20, double m21, double m22) {

      this.m00 = m00;
      this.m01 = m01;
      this.m02 = m02;
      this.m10 = m10;
      this.m11 = m11;
      this.m12 = m12;
      this.m20 = m20;
      this.m21 = m21;
      this.m22 = m22;
      normalize();
   }

   /**
    * Sets this rotation to one produced by rotating about an axis.
    * 
    * @param axisAng
    * gives the rotation axis and corresponding angle
    */
   public void setAxisAngle (AxisAngle axisAng) {
      setAxisAngle (
         axisAng.axis.x, axisAng.axis.y, axisAng.axis.z, axisAng.angle);
   }

   private int flipAngle (int deg) {
      if (deg == 90) {
         return 270;
      }
      else if (deg == 270) {
         return 90;
      }
      else {
         return deg;
      }
   }

   /**
    * Sets this rotation to one produced by rotating about an axis.
    * If the angle is an exact multiple of 90 degrees and the axis
    * is a primary axis, create an exact representation.
    * 
    * @param axisAng
    * gives the rotation axis and corresponding angle
    */
   public void setAxisAnglePrecise (AxisAngle axisAng) {
      int deg = -1;
      if (axisAng.angle == 0) {
         deg = 0;
      }
      else if (axisAng.angle == Math.toRadians(90)) {
         deg = 90;
      }
      else if (axisAng.angle == Math.toRadians(180)) {
         deg = 180;
      }
      else if (axisAng.angle == Math.toRadians(-90) ||
               axisAng.angle == Math.toRadians(270)) {
         deg = 270;
      }
      if (deg >= 0) {
         if (axisAng.axis.y == 0 && axisAng.axis.z == 0) {
            if (axisAng.axis.x < 0) {
               deg = flipAngle (deg);
            }
            setIdentity();
            switch (deg) {
               case 90: mulRotX90(); break;
               case 180: mulRotX180(); break;
               case 270: mulRotX270(); break;
            }
            return;
         }
         else if (axisAng.axis.x == 0 && axisAng.axis.z == 0) {
            if (axisAng.axis.y < 0) {
               deg = flipAngle (deg);
            }
            setIdentity();
            switch (deg) {
               case 90: mulRotY90(); break;
               case 180: mulRotY180(); break;
               case 270: mulRotY270(); break;
            }
            return;
         }
         else if (axisAng.axis.x == 0 && axisAng.axis.y == 0) {
            if (axisAng.axis.z < 0) {
               deg = flipAngle (deg);
            }
            setIdentity();
            switch (deg) {
               case 90: mulRotZ90(); break;
               case 180: mulRotZ180(); break;
               case 270: mulRotZ270(); break;
            }
            return;
         }
      }
      setAxisAngle (
         axisAng.axis.x, axisAng.axis.y, axisAng.axis.z, axisAng.angle);
   }

   /**
    * Sets this rotation to one produced by rotating about an axis.
    * 
    * @param axis
    * gives the rotation axis
    * @param ang
    * rotation angle (radians)
    */
   public void setAxisAngle (Vector3d axis, double ang) {
      setAxisAngle (axis.x, axis.y, axis.z, ang);
   }

   /**
    * Sets the rotation to one produced by rotating about an axis. The axis does
    * not have to be of unit length.
    * 
    * @param ux
    * axis x coordinate
    * @param uy
    * axis y coordinate
    * @param uz
    * axis z coordinate
    * @param ang
    * angle of rotation about the axis (radians)
    */
   public void setAxisAngle (double ux, double uy, double uz, double ang) {
      double s = Math.sin (ang);
      double c = Math.cos (ang);
      double ver = 1 - c;

      double x = ux;
      double y = uy;
      double z = uz;

      if (ang == 0) {
         setIdentity();
         return;
      }

      double len = Math.sqrt (x * x + y * y + z * z);
      if (len != 0) {
         x /= len;
         y /= len;
         z /= len;
      }
      else {
         setIdentity();
         return;
      }

      double xv = ver * x;
      double xs = x * s;
      double yv = ver * y;
      double ys = y * s;
      double zv = ver * z;
      double zs = z * s;

      m00 = x * xv + c;
      m10 = x * yv + zs;
      m20 = x * zv - ys;

      m01 = y * xv - zs;
      m11 = y * yv + c;
      m21 = y * zv + xs;

      m02 = z * xv + ys;
      m12 = z * yv - xs;
      m22 = z * zv + c;
   }

   /**
    * Sets this rotation to one produced by rotating about the x axis.
    * 
    * @param ang
    * angle of rotation (radians)
    */
   public void setRotX (double ang) {
      double s = Math.sin (ang);
      double c = Math.cos (ang);
      m00 = 1;
      m01 = 0;
      m02 = 0;
      m10 = 0;
      m11 = c;
      m12 = -s;
      m20 = 0;
      m21 = s;
      m22 = c;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting of
    * a rotation about the x axis, and places the result in this rotation.
    * 
    * @param ang
    * angle (radians) for the second rotation
    */
   public void mulRotX (double ang) {
      RotationMatrix3d Tmp = new RotationMatrix3d();
      Tmp.setRotX (ang);
      mul (Tmp);
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 90 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotX90 () {
      double t02 = m02;
      double t12 = m12;
      double t22 = m22;

      m02 = -m01;
      m12 = -m11;
      m22 = -m21;

      m01 = t02;
      m11 = t12;
      m21 = t22;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 180 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotX180 () {
      m01 = -m01;
      m11 = -m11;
      m21 = -m21;

      m02 = -m02;
      m12 = -m12;
      m22 = -m22;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 270 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotX270 () {
      double t02 = m02;
      double t12 = m12;
      double t22 = m22;

      m02 = m01;
      m12 = m11;
      m22 = m21;

      m01 = -t02;
      m11 = -t12;
      m21 = -t22;
   }

   /**
    * Sets this rotation to one produced by rotating about the y axis.
    * 
    * @param ang
    * angle of rotation (radians)
    */
   public void setRotY (double ang) {
      double s = Math.sin (ang);
      double c = Math.cos (ang);
      m00 = c;
      m01 = 0;
      m02 = s;
      m10 = 0;
      m11 = 1;
      m12 = 0;
      m20 = -s;
      m21 = 0;
      m22 = c;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting of
    * a rotation about the y axis, and places the result in this rotation.
    * 
    * @param ang
    * angle (radians) for the second rotation
    */
   public void mulRotY (double ang) {
      RotationMatrix3d Tmp = new RotationMatrix3d();
      Tmp.setRotY (ang);
      mul (Tmp);
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 90 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotY90 () {
      double t00 = m00;
      double t10 = m10;
      double t20 = m20;

      m00 = -m02;
      m10 = -m12;
      m20 = -m22;

      m02 = t00;
      m12 = t10;
      m22 = t20;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 180 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotY180 () {
      m00 = -m00;
      m10 = -m10;
      m20 = -m20;

      m02 = -m02;
      m12 = -m12;
      m22 = -m22;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 270 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotY270 () {
      double t00 = m00;
      double t10 = m10;
      double t20 = m20;

      m00 = m02;
      m10 = m12;
      m20 = m22;

      m02 = -t00;
      m12 = -t10;
      m22 = -t20;
   }

   /**
    * Sets this rotation to one produced by rotating about the z axis.
    * 
    * @param ang
    * angle of rotation (radians)
    */
   public void setRotZ (double ang) {
      double s = Math.sin (ang);
      double c = Math.cos (ang);
      m00 = c;
      m01 = -s;
      m02 = 0;
      m10 = s;
      m11 = c;
      m12 = 0;
      m20 = 0;
      m21 = 0;
      m22 = 1;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting of
    * a rotation about the z axis, and places the result in this rotation.
    * 
    * @param ang
    * angle (radians) for the second rotation
    */
   public void mulRotZ (double ang) {
      RotationMatrix3d Tmp = new RotationMatrix3d();
      Tmp.setRotZ (ang);
      mul (Tmp);
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 90 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotZ90 () {
      double t01 = m01;
      double t11 = m11;
      double t21 = m21;

      m01 = -m00;
      m11 = -m10;
      m21 = -m20;

      m00 = t01;
      m10 = t11;
      m20 = t21;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 180 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotZ180 () {
      m00 = -m00;
      m10 = -m10;
      m20 = -m20;

      m01 = -m01;
      m11 = -m11;
      m21 = -m21;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation consisting
    * of a rotation of 270 degrees about the x axis, and places the result in
    * this rotation. This method is supplied in order to provide an exact
    * numerical answer for this special case.
    */
   public void mulRotZ270 () {
      double t01 = m01;
      double t11 = m11;
      double t21 = m21;

      m01 = m00;
      m11 = m10;
      m21 = m20;

      m00 = -t01;
      m10 = -t11;
      m20 = -t21;
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation expressed as
    * an axis-angle, and places the result in this rotation.
    * 
    * @param ux
    * rotation axis x component
    * @param uy
    * rotation axis y component
    * @param uz
    * rotation axis z component
    * @param ang
    * rotation angle (in radians)
    */
   public void mulAxisAngle (double ux, double uy, double uz, double ang) {
      RotationMatrix3d Tmp = new RotationMatrix3d();
      Tmp.setAxisAngle (ux, uy, uz, ang);
      mul (Tmp);
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation expressed as
    * an axis-angle, and places the result in this rotation.
    * 
    * @param u
    * rotation axis
    * @param ang
    * rotation angle (in radians)
    */
   public void mulAxisAngle (Vector3d u, double ang) {
      RotationMatrix3d Tmp = new RotationMatrix3d();
      Tmp.setAxisAngle (u.x, u.y, u.z, ang);
      mul (Tmp);
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation expressed as
    * an axis-angle, and places the result in this rotation.
    * 
    * @param axisAng
    * axis-angle representation of the rotation
    */
   public void mulAxisAngle (AxisAngle axisAng) {
      RotationMatrix3d Tmp = new RotationMatrix3d();
      Tmp.setAxisAngle (axisAng);
      mul (Tmp);
   }

   /**
    * Returns the axis-angle parameters corresponding to this rotation.
    * 
    * @return axis-angle parameters
    */
   public AxisAngle getAxisAngle() {
      AxisAngle axisAng = new AxisAngle();
      axisAng.set (this);
      return axisAng;
   }

   /**
    * Returns the axis-angle parameters corresponding to this rotation.
    * 
    * @param axisAng
    * axis-angle parameters
    */
   public void getAxisAngle (AxisAngle axisAng) {
      axisAng.set (this);
   }

   /**
    * Sets this rotation to one produced by roll-pitch-yaw angles. The
    * coordinate frame corresponding to these angles is produced by a rotation
    * of roll about the z axis, followed by a rotation of pitch about the new y
    * axis, and finally a rotation of yaw about the new x axis.
    * 
    * @param roll
    * first angle (radians)
    * @param pitch
    * second angle (radians)
    * @param yaw
    * third angle (radians)
    * @see #getRpy
    */
   public void setRpy (double roll, double pitch, double yaw) {
      double sroll, spitch, syaw, croll, cpitch, cyaw;

      sroll = Math.sin (roll);
      croll = Math.cos (roll);
      spitch = Math.sin (pitch);
      cpitch = Math.cos (pitch);
      syaw = Math.sin (yaw);
      cyaw = Math.cos (yaw);

      m00 = croll * cpitch;
      m10 = sroll * cpitch;
      m20 = -spitch;

      m01 = croll * spitch * syaw - sroll * cyaw;
      m11 = sroll * spitch * syaw + croll * cyaw;
      m21 = cpitch * syaw;

      m02 = croll * spitch * cyaw + sroll * syaw;
      m12 = sroll * spitch * cyaw - croll * syaw;
      m22 = cpitch * cyaw;
   }

   /**
    * Sets this rotation to one produced by roll-pitch-yaw angles.
    * 
    * @param angs
    * contains the angles (roll, pitch, and yaw, in that order) in radians.
    * @see #setRpy(double,double,double)
    */
   public void setRpy (double[] angs) {
      setRpy (angs[0], angs[1], angs[2]);
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation expressed by
    * roll-pitch-yaw angles, and places the result in this rotation.
    * 
    * @param roll
    * first angle (radians)
    * @param pitch
    * second angle (radians)
    * @param yaw
    * third angle (radians)
    */
   public void mulRpy (double roll, double pitch, double yaw) {
      RotationMatrix3d Tmp = new RotationMatrix3d();
      Tmp.setRpy (roll, pitch, yaw);
      mul (Tmp);
   }

   /**
    * Gets the roll-pitch-yaw angles corresponding to this rotation.
    * 
    * @param angs
    * returns the angles (roll, pitch, and yaw, in that order) in radians.
    * @see #setRpy(double,double,double)
    */
   public void getRpy (double[] angs) {
      double sroll, croll, nx, ny, p;
         
      nx = m00;
      ny = m10;
      if (Math.abs (nx) < EPSILON && Math.abs (ny) < EPSILON) {
         angs[0] = 0.;
         angs[1] = Math.atan2 (-m20, nx);
         angs[2] = Math.atan2 (-m12, m11);
      }
      else {
         angs[0] = (p = Math.atan2 (ny, nx));
         sroll = Math.sin (p);
         croll = Math.cos (p);
         angs[1] = Math.atan2 (-m20, croll * nx + sroll * ny);
         angs[2] =
            Math.atan2 (sroll * m02 - croll * m12, croll * m11 - sroll * m01);
      }
   }

   /**
    * Sets this rotation to one produced by Euler angles. The coordinate frame
    * corresponding to Euler angles is produced by a rotation of phi about the z
    * axis, followed by a rotation of theta about the new y axis, and finally a
    * rotation of psi about the new x axis.
    * 
    * @param phi
    * first Euler angle (radians)
    * @param theta
    * second Euler angle (radians)
    * @param psi
    * third Euler angle (radians)
    * @see #getEuler
    */
   public void setEuler (double phi, double theta, double psi) {
      /*
       * Set the rotational component as specified by the input Euler angles.
       */

      double sphi, sthe, spsi, cphi, cthe, cpsi;

      sphi = Math.sin (phi);
      cphi = Math.cos (phi);
      sthe = Math.sin (theta);
      cthe = Math.cos (theta);
      spsi = Math.sin (psi);
      cpsi = Math.cos (psi);

      m00 = cphi * cthe * cpsi - sphi * spsi;
      m10 = sphi * cthe * cpsi + cphi * spsi;
      m20 = -sthe * cpsi;

      m01 = -cphi * cthe * spsi - sphi * cpsi;
      m11 = -sphi * cthe * spsi + cphi * cpsi;
      m21 = sthe * spsi;

      m02 = cphi * sthe;
      m12 = sphi * sthe;
      m22 = cthe;
   }

   /**
    * Sets this rotation to one produced by Euler angles.
    * 
    * @param angs
    * contains the Euler angles (phi, theta, and psi, in that order) in radians.
    * @see #setEuler(double,double,double)
    */
   public void setEuler (double[] angs) {
      setEuler (angs[0], angs[1], angs[2]);
   }

   /**
    * Post-multiplies this rotation by an implicit second rotation described by
    * Euler angles, and places the result in this rotation.
    * 
    * @param phi
    * first Euler angle (radians)
    * @param theta
    * second Euler angle (radians)
    * @param psi
    * third Euler angle (radians)
    * @see #setEuler(double,double,double)
    */
   public void mulEuler (double phi, double theta, double psi) {
      RotationMatrix3d Tmp = new RotationMatrix3d();
      Tmp.setEuler (phi, theta, psi);
      mul (Tmp);
   }

   /**
    * Gets the Euler angles corresponding to this rotation.
    * 
    * @param angs
    * returns the Euler angles (phi, theta, and psi, in that order) in radians.
    * @see #setEuler(double,double,double)
    */
   public void getEuler (double[] angs) {
      /*
       * Return the Euler angles associated with the rotational component.
       */
      double sphi, cphi, p;

      if (Math.abs (m12) > EPSILON || Math.abs (m02) > EPSILON) {
         angs[0] = (p = Math.atan2 (m12, m02));
         sphi = Math.sin (p);
         cphi = Math.cos (p);
         angs[1] = Math.atan2 (cphi * m02 + sphi * m12, m22);
         angs[2] =
            Math.atan2 (-sphi * m00 + cphi * m10, -sphi * m01 + cphi * m11);
      }
      else {
         angs[0] = 0.;
         angs[1] = Math.atan2 (m02, m22);
         angs[2] = Math.atan2 (m10, m11);
      }
   }

   /**
    * Sets this rotation to one in which the z axis points in a specified
    * direction. The x and y axes are set to corresponding values that are
    * ``nearest'' to the identity. If {@code dirz} is zero, the rotation is set
    * the identity.
    * 
    * @param dirz
    * direction for the new z axis. Does not need to be normalized.
    */
   public void setZDirection (Vector3d dirz) {
      double axis_x, axis_y;

      axis_x = -dirz.y;
      axis_y = dirz.x;
      double len = Math.sqrt (axis_x * axis_x + axis_y * axis_y);

      if (len != 0) {
         double ang = Math.atan2 (len, dirz.z);
         setAxisAngle (axis_x / len, axis_y / len, 0.0, ang);
      }
      else if (dirz.z >= 0) {
         setIdentity();
      }
      else {
         setIdentity();
         m11 = -1;
         m22 = -1;
      }
   }

   /**
    * Sets this rotation to one in which the x axis points in a specified
    * direction. The y and z axes are set to corresponding values that are
    * ``nearest'' to the identity. If {@code dirx} is zero, the rotation is set
    * the identity.
    * 
    * @param dirx
    * direction for the new x axis. Does not need to be normalized.
    */
   public void setXDirection (Vector3d dirx) {
      double axis_y, axis_z;

      axis_y = -dirx.z;
      axis_z = dirx.y;
      double len = Math.sqrt (axis_y * axis_y + axis_z * axis_z);

      if (len != 0) {
         double ang = Math.atan2 (len, dirx.x);
         setAxisAngle (0.0, axis_y / len, axis_z / len, ang);
      }
      else if (dirx.x >= 0) {
         setIdentity();
      }
      else {
         setIdentity();
         m00 = -1;
         m22 = -1;
      }
   }

   /**
    * Sets this rotation to one in which the y axis points in a specified
    * direction. The z and x axes are set to corresponding values that are
    * ``nearest'' to the identity. If {@code diry} is zero, the rotation is set
    * the identity.
    * 
    * @param diry
    * direction for the new y axis. Does not need to be normalized.
    */
   public void setYDirection (Vector3d diry) {
      double axis_z, axis_x;

      axis_z = -diry.x;
      axis_x = diry.z;
      double len = Math.sqrt (axis_z * axis_z + axis_x * axis_x);

      if (len != 0) {
         double ang = Math.atan2 (len, diry.y);
         setAxisAngle (axis_x / len, 0.0, axis_z / len, ang);
      }
      else if (diry.y >= 0) {
         setIdentity();
      }
      else {
         setIdentity();
         m00 = -1;
         m11 = -1;
      }
   }

   /**
    * Rotates this rotation so that the z axis points in a specified direction.
    * This is done by rotating about an axis perpendicular to the orginal and
    * new z axes.
    * 
    * @param dirz
    * direction for the new z axis
    */
   public void rotateZDirection (Vector3d dirz) {
      Vector3d newZ = new Vector3d (dirz);
      newZ.normalize();
      Vector3d oldZ = new Vector3d (m02, m12, m22); // original z axis
      Vector3d axis = new Vector3d();

      axis.cross (oldZ, newZ);
      double cos = newZ.dot (oldZ);
      double sin = axis.norm();
      double ang = Math.atan2 (sin, cos);

      if (ang != 0 && sin == 0) // just rotate about x
      {
         axis.set (1, 0, 0);
      }
      if (ang != 0) {
         RotationMatrix3d Tmp = new RotationMatrix3d();
         // System.out.println ("axis=" + axis.toString("%14.9f"));
         // System.out.println ("ang=" + Math.toDegrees (ang));
         // System.out.println ("cos=" + cos);
         // System.out.println ("sin=" + sin);
         Tmp.setAxisAngle (axis, ang);
         mul (Tmp, this);
      }
   }

   /**
    * Sets this rotation to one produced by rotating about a random axis by a
    * random angle.
    */
   public void setRandom() {
      setRandom (RandomGenerator.get());
   }

   /**
    * Sets this rotation to one produced by rotating about a random axis by a
    * random angle, using a supplied random number generator.
    * 
    * @param generator
    * random number generator
    */
   public void setRandom (Random generator) {
      double x = generator.nextDouble() - 0.5;
      double y = generator.nextDouble() - 0.5;
      double z = generator.nextDouble() - 0.5;
      double ang = 2 * Math.PI * (generator.nextDouble() - 0.5);

      setAxisAngle (x, y, z, ang);
   }

   private final int X_MAX = 1;
   private final int Y_MAX = 2;
   private final int Z_MAX = 3;

   /**
    * Gets the rotation axis-angle representation for this rotation.  To keep
    * the representation unique, the axis is normalized and the angle is kept
    * in the range 0 {@code <=} angle {@code <} Math.PI.
    * 
    * @param axis
    * returns the rotation axis
    * @return the rotation angle (in radians)
    */
   public double getAxisAngle (Vector3d axis) {
      double cosine, tmpX, tmpY, tmpZ, tmpS;
      double versine, sine = 0;
      int max;

      cosine = 0.5 * (m00 + m11 + m22 - 1.0);

      if (cosine > 0.0) { /* then we can compute things the easy way. */

         tmpX = m21 - m12;
         tmpY = m02 - m20;
         tmpZ = m10 - m01;

         tmpS = Math.sqrt (tmpX * tmpX + tmpY * tmpY + tmpZ * tmpZ);

         if (tmpS <= DOUBLE_PREC) {
            axis.y = axis.z = 0.0;
            axis.x = 1.0;
            return 0;
         }
         axis.x = tmpX / tmpS;
         axis.y = tmpY / tmpS;
         axis.z = tmpZ / tmpS;
         sine = tmpS / 2.0;
      }
      else {
         versine = 1.0 - cosine;

         tmpX = Math.abs (m00 - cosine);
         tmpY = Math.abs (m11 - cosine);
         tmpZ = Math.abs (m22 - cosine);

         if (tmpX >= tmpY) {
            max = (tmpX >= tmpZ ? X_MAX : Z_MAX);
         }
         else {
            max = (tmpY >= tmpZ ? Y_MAX : Z_MAX);
         }
         switch (max) {
            case X_MAX: {
               if (versine <= DOUBLE_PREC || tmpX <= 0.0) {
                  axis.y = axis.z = 0.0;
                  axis.x = 1.0;
                  return 0;
               }
               if ((tmpS = m21 - m12) >= 0.0) {
                  axis.x = Math.sqrt (tmpX / versine);
               }
               else {
                  axis.x = -Math.sqrt (tmpX / versine);
               }
               axis.y = (m10 + m01) / (2 * axis.x * versine);
               axis.z = (m02 + m20) / (2 * axis.x * versine);
               sine = tmpS / (2 * axis.x);
               break;
            }
            case Y_MAX: {
               if (versine <= DOUBLE_PREC || tmpY <= 0.0) {
                  axis.y = axis.z = 0.0;
                  axis.x = 1.0;
                  return 0;
               }
               if ((tmpS = m02 - m20) >= 0.0) {
                  axis.y = Math.sqrt (tmpY / versine);
               }
               else {
                  axis.y = -Math.sqrt (tmpY / versine);
               }
               axis.x = (m10 + m01) / (2 * axis.y * versine);
               axis.z = (m21 + m12) / (2 * axis.y * versine);
               sine = tmpS / (2 * axis.y);
               break;
            }
            case Z_MAX: {
               if (versine <= DOUBLE_PREC || tmpZ <= 0.0) {
                  axis.y = axis.z = 0.0;
                  axis.x = 1.0;
                  return 0;
               }
               if ((tmpS = m10 - m01) >= 0.0) {
                  axis.z = Math.sqrt (tmpZ / versine);
               }
               else {
                  axis.z = -Math.sqrt (tmpZ / versine);
               }
               axis.x = (m02 + m20) / (2 * axis.z * versine);
               axis.y = (m21 + m12) / (2 * axis.z * versine);
               sine = tmpS / (2 * axis.z);
               break;
            }
         }
         if (sine < 0) {
            sine = -sine;
            axis.x = -axis.x;
            axis.y = -axis.y;
            axis.z = -axis.z;
         }
      }
      return Math.atan2 (sine, cosine);
   }

   private final boolean isAAEntry (double x, double tol) {
      return (Math.abs(x) <= tol ||
              Math.abs(x-1) <= tol ||
              Math.abs(x+1) <= tol);
   }

   /**
    * Returns true if this rotation transformtion is axis-aligned - that is, if
    * all entries consist of either 1, -1, or 0. Entries must equal these
    * values within a tolerance specified by <code>tol</code>. The
    * implementation for this method assumes that the entries are properly
    * formed for a rotation matrix.
    *
    * @param tol tolerance to test whether entries are zero or one.
    * @return true if this rotation is axis-aligned.
    */
   public boolean isAxisAligned (double tol) {
      return
         (isAAEntry(m00,tol) && isAAEntry(m01,tol) && isAAEntry(m02,tol) &&
          isAAEntry(m10,tol) && isAAEntry(m11,tol) && isAAEntry(m12,tol) &&
          isAAEntry(m20,tol) && isAAEntry(m21,tol) && isAAEntry(m22,tol));
   }

   public void checkOrthonormality() {
      Vector3d xdir = new Vector3d();
      Vector3d ydir = new Vector3d();
      Vector3d zdir = new Vector3d();

      double EPS = 1e-15;

      getColumn (0, xdir);
      getColumn (1, ydir);
      getColumn (2, zdir);
      if (Math.abs(xdir.norm()-1) > EPS) {
         throw new IllegalStateException ("x axis does not have unit length");
      }
      if (Math.abs(ydir.norm()-1) > EPS) {
         throw new IllegalStateException ("y axis does not have unit length");
      }
      if (Math.abs(zdir.norm()-1) > EPS) {
         throw new IllegalStateException ("z axis does not have unit length");
      }
      Vector3d xprod = new Vector3d();
      xprod.cross (xdir, ydir);
      if (!xprod.epsilonEquals (zdir, EPS)) {
         throw new IllegalStateException ("x X y != z");
      }
      xprod.cross (ydir, zdir);
      if (!xprod.epsilonEquals (xdir, EPS)) {
         throw new IllegalStateException ("y X z != x");
      }
      xprod.cross (zdir, xdir);
      if (!xprod.epsilonEquals (ydir, EPS)) {
         throw new IllegalStateException ("z X x != y");
      }
   }

   /**
    * Returns true if this rotation transformtion is axis-aligned - that is, if
    * all entries consist of either 1, -1, or 0.
    *
    * @return true if this rotation is axis-aligned.
    */
   public boolean isAxisAligned () {
      return isAxisAligned (0);
   }

   /**
    * Returns a string representation of this transformation as a 3 x 3 matrix.
    */
   public String toString() {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat ("%g"), MATRIX_STRING);
   }

   /**
    * Returns a string representation of this transformation as a 3 x 3 matrix,
    * with each number formatted according to a supplied numeric format.
    * 
    * @param numberFmtStr
    * numeric format string (see {@link NumberFormat NumberFormat})
    */
   public String toString (String numberFmtStr) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat (numberFmtStr), MATRIX_STRING);
   }

   /**
    * Returns a specified string representation of this transformation, with
    * each number formatted according to the a supplied numeric format.
    * 
    * @param numberFmtStr
    * numeric format string (see {@link NumberFormat NumberFormat})
    * @param outputCode
    * desired representation, which should be either
    * {@link #AXIS_ANGLE_STRING AXIS_ANGLE_STRING} or {@link #MATRIX_STRING
    * MATRIX_STRING}
    */
   public String toString (String numberFmtStr, int outputCode) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat (numberFmtStr), outputCode);
   }

   /**
    * Returns a specified string representation of this transformation, with
    * each number formatted according to the a supplied numeric format.
    * 
    * @param numberFmt
    * numeric format
    * @param outputCode
    * desired representation, which should be either
    * {@link #AXIS_ANGLE_STRING AXIS_ANGLE_STRING} or {@link #MATRIX_STRING
    * MATRIX_STRING}
    */
   public String toString (NumberFormat numberFmt, int outputCode) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, numberFmt, outputCode);
   }

   String toString (StringBuffer sbuf, NumberFormat numberFmt, int outputCode) {
      sbuf.setLength (0);
      if (outputCode == MATRIX_STRING) {
         sbuf.append ("[ ");
         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
               numberFmt.format (get (i, j), sbuf);
               sbuf.append (' ');
            }
            if (i < 2) {
               sbuf.append ("\n  ");
            }
         }
         sbuf.append (" ]");
      }
      else if (outputCode == AXIS_ANGLE_STRING) {
         AxisAngle axisAng = new AxisAngle();

         sbuf.append ("[ ");
         getAxisAngle (axisAng);
         numberFmt.format (axisAng.axis.x, sbuf);
         sbuf.append (' ');
         numberFmt.format (axisAng.axis.y, sbuf);
         sbuf.append (' ');
         numberFmt.format (axisAng.axis.z, sbuf);
         sbuf.append (' ');
         numberFmt.format (Math.toDegrees (axisAng.angle), sbuf);
         sbuf.append (" ]");
      }
      else {
         throw new IllegalArgumentException ("Unknown display format");
      }
      return sbuf.toString();
   }

   /**
    * Reads the contents of this rotation from a ReaderTokenizer. There are two
    * allowed formats, each of which is delimited by square brackets.
    * 
    * <p>
    * The first format is a set of 4 numbers describing the rotation in
    * axis-angle notation. The rotation axis is given first, followed by the
    * rotation angle, in degrees. The axis does not need to be normalized. For
    * example,
    * 
    * <pre>
    * [ 0 1 0 90 ]
    * </pre>
    * 
    * defines a rotation of 90 degrees about the y axis.
    * 
    * <p>
    * The second format format is a set of 9 numbers describing the elements of
    * the rotation matrix in row-major order. For example,
    * 
    * <pre>
    * [ 0  -1  0
    *   1   0  0
    *   0   0  1 ]
    * </pre>
    * 
    * defines a rotation of 90 degrees about the z axis.
    * 
    * <p>
    * The third format consists of a series of simple rotations, which are the
    * multiplied together to form a final rotation. The following simple
    * rotations may be specified:
    * 
    * <dl>
    * <dt><code>rotX</code> ang
    * <dd>A rotation of "ang" degrees about the x axis;
    * <dt><code>rotY</code> ang
    * <dd>A rotation of "ang" degrees about the y axis;
    * <dt><code>rotZ</code> ang
    * <dd>A rotation of "ang" degrees about the z axis;
    * </dl>
    * 
    * For example, the string
    * 
    * <pre>
    * [ rotX 45 rotZ 90 ]
    * </pre>
    * 
    * describes a rotation which is the product of a rotation of 45 degrees
    * about the y axis and rotation of 90 degrees about the z axis.
    * 
    * @param rtok
    * ReaderTokenizer from which to read the rotation. Number parsing should be
    * enabled.
    * @throws IOException
    * if an I/O error occured or if the rotation description is not consistent
    * with one of the above formats.
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');

      double[] values = new double[9];
      int nvals = 0;

      rtok.nextToken();
      if (rtok.tokenIsNumber()) {
         do {
            values[nvals++] = rtok.nval;
            rtok.nextToken();
         }
         while (nvals < values.length && rtok.tokenIsNumber());
         if (rtok.ttype != ']') {
            if (rtok.tokenIsNumber()) {
               throw new IOException (
                  "Too many numeric values, line " + rtok.lineno());
            }
            else {
               throw new IOException ("']' expected, line " + rtok.lineno());
            }
         }
         else if (nvals != 4 && nvals != 9) {
            throw new IOException (
               "Unexpected number of numeric values ("+nvals+"), line "+
               rtok.lineno());
         }
         else if (nvals == 4) {
            setAxisAngle (values[0], values[1], values[2],
                          Math.toRadians (values[3]));
         }
         else if (nvals == 9) {
            m00 = values[0];
            m01 = values[1];
            m02 = values[2];

            m10 = values[3];
            m11 = values[4];
            m12 = values[5];

            m20 = values[6];
            m21 = values[7];
            m22 = values[8];
         }
      }
      else {
         setIdentity();
         double[] ang = new double[1];
         while (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equalsIgnoreCase ("rotX")) {
               if (rtok.scanNumbers (ang, 1) != 1) {
                  throw new IOException (
                     "keyword 'rotX' requires a double value");
               }
               mulRotX (Math.toRadians (ang[0]));
            }
            else if (rtok.sval.equalsIgnoreCase ("rotY")) {
               if (rtok.scanNumbers (ang, 1) != 1) {
                  throw new IOException (
                     "keyword 'rotY' requires a double value");
               }
               mulRotY (Math.toRadians (ang[0]));
            }
            else if (rtok.sval.equalsIgnoreCase ("rotZ")) {
               if (rtok.scanNumbers (ang, 1) != 1) {
                  throw new IOException (
                     "keyword 'rotZ' requires a double value");
               }
               mulRotZ (Math.toRadians (ang[0]));
            }
            else {
               throw new IOException (
                  "Unexpected keyword '" + rtok.sval+"', line " + rtok.lineno());
            }
            rtok.nextToken();
         }
         if (rtok.ttype != ']') {
            throw new IOException ("']' expected, line " + rtok.lineno());
         }
      }
   }

   public static void main (String[] args) {
      RotationMatrix3d X = new RotationMatrix3d();

      int cnt = 10000000;
      RotationMatrix3d R = new RotationMatrix3d();
      Vector3d u = new Vector3d();
      int neg = 0;
      for (int i=0; i<cnt; i++) {
         R.setRandom();
         double ang = R.getAxisAngle (u);
         if (ang < 0) {
            neg++;
         }
      }
      System.out.println ("neg=" + neg);
   }

}
