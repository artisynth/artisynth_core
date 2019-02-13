/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import maspack.util.Clonable;
import maspack.util.NumberFormat;
import maspack.util.RandomGenerator;
import maspack.util.InternalErrorException;
import maspack.util.ReaderTokenizer;

/**
 * A specialized 4 x 4 matrix that implements a three-dimensional rigid body
 * transformation with scaling in homogeneous coordinates.
 * 
 * <p>
 * A rigid body transformation is used to transform a point from one spatial
 * coordinate frame into another. If x0 and x1 denote the point in the original
 * frame 0 and target frame 1, respectively, then the transformation is computed
 * according to
 * 
 * <pre>
 * x1 = s R x0 + p
 * </pre>
 * 
 * where R is a 3 x 3 rotation matrix and p is a translation vector. In
 * homogenous coordinates, this operation can be represented as
 * 
 * <pre>
 * [ x1 ]   [ s R   p ] [ x0 ]
 * [    ] = [         ] [    ]
 * [  1 ]   [  0    1 ] [  1 ]
 * </pre>
 * 
 * <p>
 * The inverse of such a transformation is given by
 * by
 * 
 * <pre>
 *         [  T      T    ]
 *  (-1)   [ R/s   -R p/s ]
 * A     = [              ]
 *         [ 0      1     ]
 * </pre>
 * 
 * <p>
 * In this class, the fields R, s, and p are exposed, and users can manipulate 
 * them as desired. For example, specifying a rotation using Euler angles would
 * be done using the {@link maspack.matrix.RotationMatrix3d#setEuler setEuler}
 * method in R. This allows us to minimize the number of methods in the
 * RigidTransform3d class itself.
 */

public class ScaledRigidTransform3d extends AffineTransform3dBase implements
Clonable {
   private static final long serialVersionUID = 1L;

   /**
    * Specifies a string representation of this transformation as an 8-tuple
    * consisting of a translation vector followed by a rotation axis and the
    * corresponding angle (in degrees) followed by the scale
    */
   public static final int AXIS_ANGLE_STRING = 1;

   /**
    * Specifies a string representation of this transformation as a 3 x 4 matrix
    * (i.e., with the 4th row ommitted).
    */
   public static final int MATRIX_3X4_STRING = 2;

   /**
    * Specifies a string representation of this transformation as a 4 x 4
    * matrix.
    */
   public static final int MATRIX_4X4_STRING = 3;

   /**
    * Rotation matrix associated with this transformation.
    */
   public final RotationMatrix3d R;
   
   /**
    * Scaled rotation matrix
    */
   private final Matrix3d A;

   /**
    * Scale factor associated with this transform
    */
   public double s;

   /**
    * Translation vector associated with this transformation.
    */
   public final Vector3d p;

   /**
    * Global identity transform. Should not be modified.
    */
   public static final ScaledRigidTransform3d IDENTITY =
      new ScaledRigidTransform3d();


   private boolean myExternalComponentsValidP;

   /**
    * Creates a new transformation initialized to the identity.
    */
   public ScaledRigidTransform3d() {
      this.R = new RotationMatrix3d();
      this.p = new Vector3d();
      s = 1;
      A = new Matrix3d();
      A.scale(s, this.R);
      M = A;
      b = p;
      myExternalComponentsValidP = true;
   }

   /**
    * Creates a new transformation with the specified translation vector and
    * rotation matrix.
    * 
    * @param p
    * translation vector
    * @param R
    * rotation matrix
    */
   public ScaledRigidTransform3d (Vector3d p, RotationMatrix3d R) {
      this.R = new RotationMatrix3d (R);
      this.p = new Vector3d (p);
      s = 1;
      A = new Matrix3d();
      A.scale(s, this.R);
      M = A;
      b = this.p;
      myExternalComponentsValidP = true;
   }

   /**
    * Creates a new transformation with the specified translation vector.
    * 
    * @param px
    * x translation coordinate
    * @param py
    * y translation coordinate
    * @param pz
    * z translation coordinate
    */
   public ScaledRigidTransform3d (double px, double py, double pz) {
      this.R = new RotationMatrix3d();
      this.p = new Vector3d (px, py, pz);
      s = 1;
      A = new Matrix3d();
      A.scale(s, this.R);
      M = A;
      b = p;
      myExternalComponentsValidP = true;
   }

   /**
    * Creates a new transformation with the specified translation and rotation.
    * 
    * @param px
    * x translation coordinate
    * @param py
    * y translation coordinate
    * @param pz
    * z translation coordinate
    * @param roll
    * roll angle (rotation about z axis, radians)
    * @param pitch
    * pitch angle (rotation about new y axis, radians)
    * @param yaw
    * yaw angle (rotation about new x axis, radians)
    */
   public ScaledRigidTransform3d (
      double px, double py, double pz, double roll, double pitch, double yaw) {
      this.R = new RotationMatrix3d ();
      this.R.setRpy (roll, pitch, yaw);
      this.p = new Vector3d (px, py, pz);     
      s = 1;
      A = new Matrix3d();
      A.scale(s, this.R);
      M = A;
      b = p;
      myExternalComponentsValidP = true;
   }

   /**
    * Creates a new transformation with the specified translation and rotation.
    * 
    * @param px
    * x translation coordinate
    * @param py
    * y translation coordinate
    * @param pz
    * z translation coordinate
    * @param ux
    * x rotation axis coordinate
    * @param uy
    * y rotation axis coordinate
    * @param uz
    * z rotation axis coordinate
    * @param ang
    * angle of rotation about the axis (radians)
    */
   public ScaledRigidTransform3d (
      double px, double py, double pz, double ux,
      double uy, double uz, double ang) {
      this.R = new RotationMatrix3d (ux, uy, uz, ang);
      this.p = new Vector3d (px, py, pz);
      s = 1;
      A = new Matrix3d();
      A.scale(s, this.R);
      M = A;
      b = p;
      myExternalComponentsValidP = true;
   }

   /**
    * Creates a new transformation which is a copy of an existing one.
    * 
    * @param X
    * transform to copy
    */
   public ScaledRigidTransform3d (ScaledRigidTransform3d X) {
      R = new RotationMatrix3d (X.R);
      p = new Vector3d (X.p);
      s = X.s;
      A = new Matrix3d();
      A.scale(s, this.R);
      M = A;
      b = p;
      myExternalComponentsValidP = true;
   }

   /**
    * Creates a new transformation with the specified translation vector and
    * rotation.
    * 
    * @param p
    * translation vector
    * @param axisAng
    * axis-angle describing the rotation
    */
   public ScaledRigidTransform3d (Vector3d p, AxisAngle axisAng) {
      this.R = new RotationMatrix3d (axisAng);
      this.p = new Vector3d (p);
      s = 1;
      A = new Matrix3d();
      A.scale(s, this.R);
      M = A;
      b = this.p;
      myExternalComponentsValidP = true;
   }

   public void set (Matrix M) {
      super.set (M);
      updateExternalComponents();
   }

   public void set (AffineTransform3dBase A) {
      super.set (A);
      updateExternalComponents();
   }

   /**
    * {@inheritDoc}
    */
   public void set (int i, int j, double value) { 
      super.set (i, j, value);
      myExternalComponentsValidP = false;
   }

   /**
    * {@inheritDoc}
    */
   public void set (double[] vals) {
      super.set (vals);
      myExternalComponentsValidP = false;
   }

   /**
    * {@inheritDoc}
    */
   public void setColumn (int j, double[] values) {
      super.setColumn (j, values);
      myExternalComponentsValidP = false;
   }

   /**
    * {@inheritDoc}
    */
   public void setRow (int j, double[] values) {
      super.setRow (j, values);
      myExternalComponentsValidP = false;
   }

   /**
    * Post-multiplies this transformation by another and places the result in
    * this transformation.
    * 
    * @param X
    * transformation to multiply by
    */
   public void mul (ScaledRigidTransform3d X) {
      mul (this, X);
   }

   /**
    * Multiplies transformation X1 by X2 and places the result in this
    * transformation.
    * 
    * @param X1
    * first transformation
    * @param X2
    * second transformation
    */
   public void mul (ScaledRigidTransform3d X1, ScaledRigidTransform3d X2) {

      if (!X1.myExternalComponentsValidP) {
         X1.updateExternalComponents();
      }
      if (!X2.myExternalComponentsValidP) {
         X2.updateExternalComponents();
      }

      double p1x = X1.p.x;
      double p1y = X1.p.y;
      double p1z = X1.p.z;
      
      X1.R.mul (p, X2.p);
      p.scale(X1.s);
      p.x += p1x;
      p.y += p1y;
      p.z += p1z;
      
      R.mul (X1.R, X2.R);
      
      s = X1.s*X2.s;
      
      updateInternalMatrix();
   }
   
   protected void validate() {
      Matrix3d RRT = new Matrix3d();
      RRT.mulTransposeRight (R, R);
      if (!RRT.epsilonEquals (Matrix3d.IDENTITY, 1e-13)) {
         System.out.println ("R=\n" + R.toString ("%14.10f"));
         throw new InternalErrorException (
            "R is not orthogonal");
      }
      
   }

   public void updateInternalMatrix() {
      A.scale(s, R);
      validate();
      // assumes that external components are valid, and explictly sets this
      myExternalComponentsValidP = true;
   }

   public void updateExternalComponents() {
      s = Math.cbrt(A.determinant());
      // could also do s = sqrt(trace(A*A')/3), if we assume that A
      // is scaled orthogonal
      R.scale (1/s, A);
      myExternalComponentsValidP = true;
   }

   /**
    * Post-multiplies this transformation by the inverse of transformation X and
    * places the result in this transformation.
    * 
    * @param X
    * right-hand transformation
    */
   public void mulInverse (ScaledRigidTransform3d X) {
      mulInverseRight (this, X);
   }

   /**
    * Multiplies transformation X1 by the inverse of transformation X2 and
    * places the result in this transformation.
    * 
    * @param X1
    * left-hand transformation
    * @param X2
    * right-hand transformation
    */
   public void mulInverseRight (
      ScaledRigidTransform3d X1, ScaledRigidTransform3d X2) {

      if (!X1.myExternalComponentsValidP) {
         X1.updateExternalComponents();
      }
      if (!X2.myExternalComponentsValidP) {
         X2.updateExternalComponents();
      }

      double p1x = X1.p.x;
      double p1y = X1.p.y;
      double p1z = X1.p.z;

      if (X1 == this) {
         X1 = new ScaledRigidTransform3d (this);
      }
      if (X2 == this) {
         X2 = new ScaledRigidTransform3d (this);
      }
      
      s = X1.s/X2.s;
      R.mulInverseRight (X1.R, X2.R);

      R.mul (p, X2.p);
      p.scale(s);
      p.x = p1x - p.x;
      p.y = p1y - p.y;
      p.z = p1z - p.z;
      
      updateInternalMatrix();
   }

   /**
    * Multiplies the inverse of transformation X1 by transformation X2 and
    * places the result in this transformation.
    * 
    * @param X1
    * left-hand transformation
    * @param X2
    * right-hand transformation
    */
   public void mulInverseLeft (
      ScaledRigidTransform3d X1, ScaledRigidTransform3d X2) {

      if (!X1.myExternalComponentsValidP) {
         X1.updateExternalComponents();
      }
      if (!X2.myExternalComponentsValidP) {
         X2.updateExternalComponents();
      }

      p.sub (X2.p, X1.p);
      X1.R.mulTranspose (p, p);
      p.scale(1.0/X1.s);
      
      R.mulInverseLeft (X1.R, X2.R);
      
      s = X2.s/X1.s;
      
      updateInternalMatrix();
   }

   /**
    * Multiplies the inverse of transformation X1 by the inverse of
    * transformation X2 and places the result in this transformation.
    * 
    * @param X1
    * left-hand transformation
    * @param X2
    * right-hand transformation
    */
   public void mulInverseBoth (
      ScaledRigidTransform3d X1, ScaledRigidTransform3d X2) {
      
      if (!X1.myExternalComponentsValidP) {
         X1.updateExternalComponents();
      }
      if (!X2.myExternalComponentsValidP) {
         X2.updateExternalComponents();
      }

      double p1x = X1.p.x;
      double p1y = X1.p.y;
      double p1z = X1.p.z;
      
      X2.R.mulTranspose (p, X2.p);
      p.scale(1.0/X2.s);
      p.x += p1x;
      p.y += p1y;
      p.z += p1z;
      X1.R.mulTranspose (p);
      p.scale(-1.0/X1.s);
      
      R.mulInverseBoth (X1.R, X2.R);
      
      s = 1.0/(X1.s*X2.s);
      
      updateInternalMatrix();
      
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure translation, and places the result in this
    * transformation. If p2 is the translation vector of the second
    * transformation, then this is the equivalent of adding R p2 to this
    * transformations translation vector.
    * 
    * @param x
    * translation component of the second transformation
    * @param y
    * translation component of the second transformation
    * @param z
    * translation component of the second transformation
    */
   public void mulXyz (double x, double y, double z) {

      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      double px = p.x;
      double py = p.y;
      double pz = p.z;
      
      p.set (x, y, z);
      R.mul (p, p);
      p.scale(s);
      
      p.x += px;
      p.y += py;
      p.z += pz;
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure rotation about the x axis, and places the result in
    * this transformation. If R2 is the rotation matrix of the second
    * transformation, then this is the equivalent of multiplying R by R2.
    * 
    * @param ang
    * rotation about the x axis (in radians) for the second transform
    */
   public void mulRotX (double ang) {

      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      double s = Math.sin (ang);
      double c = Math.cos (ang);

      double x01 = c * R.m01 + s * R.m02;
      double x11 = c * R.m11 + s * R.m12;
      double x21 = c * R.m21 + s * R.m22;

      double x02 = -s * R.m01 + c * R.m02;
      double x12 = -s * R.m11 + c * R.m12;
      double x22 = -s * R.m21 + c * R.m22;

      R.m01 = x01;
      R.m11 = x11;
      R.m21 = x21;

      R.m02 = x02;
      R.m12 = x12;
      R.m22 = x22;
      
      updateInternalMatrix();
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure rotation about the y axis, and places the result in
    * this transformation. If R2 is the rotation matrix of the second
    * transformation, then this is the equivalent of multiplying R by R2.
    * 
    * @param ang
    * rotation about the y axis (in radians) for the second transform
    */
   public void mulRotY (double ang) {

      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      double s = Math.sin (ang);
      double c = Math.cos (ang);

      double x00 = c * R.m00 - s * R.m02;
      double x10 = c * R.m10 - s * R.m12;
      double x20 = c * R.m20 - s * R.m22;

      double x02 = s * R.m00 + c * R.m02;
      double x12 = s * R.m10 + c * R.m12;
      double x22 = s * R.m20 + c * R.m22;

      R.m00 = x00;
      R.m10 = x10;
      R.m20 = x20;

      R.m02 = x02;
      R.m12 = x12;
      R.m22 = x22;
      
      updateInternalMatrix();
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure rotation about the z axis, and places the result in
    * this transformation. If R2 is the rotation matrix of the second
    * transformation, then this is the equivalent of multiplying R by R2.
    * 
    * @param ang
    * rotation about the z axis (in radians) for the second transform
    */
   public void mulRotZ (double ang) {

      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      double s = Math.sin (ang);
      double c = Math.cos (ang);

      double x00 = c * R.m00 + s * R.m01;
      double x10 = c * R.m10 + s * R.m11;
      double x20 = c * R.m20 + s * R.m21;

      double x01 = -s * R.m00 + c * R.m01;
      double x11 = -s * R.m10 + c * R.m11;
      double x21 = -s * R.m20 + c * R.m21;

      R.m00 = x00;
      R.m10 = x10;
      R.m20 = x20;

      R.m01 = x01;
      R.m11 = x11;
      R.m21 = x21;
      
      updateInternalMatrix();
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure rotation, expressed as an axis-angle, and places the
    * result in this transformation. If R2 is the rotation matrix of the second
    * transformation, then this is the equivalent of multiplying R by R2.
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
      mulRotation (new RotationMatrix3d (ux, uy, uz, ang));
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure rotation, expressed as an axis-angle, and places the
    * result in this transformation. If R2 is the rotation matrix of the second
    * transformation, then this is the equivalent of multiplying R by R2.
    * 
    * @param axisAng
    * axis-angle representation of the rotation
    */
   public void mulAxisAngle (AxisAngle axisAng) {
      mulRotation (new RotationMatrix3d (axisAng));
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure rotation, and places the result in this
    * transformation. If R2 is the rotation matrix of the second transformation,
    * then this is the equivalent of multiplying R by R2.
    * 
    * @param R2
    * rotation for the second transformation
    */
   public void mulRotation (RotationMatrix3d R2) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      R.mul (R2);
      updateInternalMatrix();
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure rotation, expressed by roll-pitch-yaw angles, and
    * places the result in this transformation. If R2 is the rotation matrix of
    * the second transformation, then this is the equivalent of multiplying R by
    * R2. See {@link maspack.matrix.RotationMatrix3d#setRpy
    * RotationMatrix3d.setRpy} for a description of roll-pitch-yaw angles.
    * 
    * @param roll
    * first angle (radians)
    * @param pitch
    * second angle (radians)
    * @param yaw
    * third angle (radians)
    */
   public void mulRpy (double roll, double pitch, double yaw) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      R.mulRpy (roll, pitch, yaw);
      updateInternalMatrix();
   }

   /**
    * Post-multiplies this transformation by an implicit second transformation
    * consisting of a pure rotation, expressed by Euler angles, and places the
    * result in this transformation. If R2 is the rotation matrix of the second
    * transformation, then this is the equivalent of multiplying R by R2. See
    * {@link maspack.matrix.RotationMatrix3d#setEuler RotationMatrix3d.setEuler}
    * for a complete description of Euler angles.
    * 
    * @param phi
    * first Euler angle (radians)
    * @param theta
    * second Euler angle (radians)
    * @param psi
    * third Euler angle (radians)
    */
   public void mulEuler (double phi, double theta, double psi) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      R.mulEuler (phi, theta, psi);
      updateInternalMatrix();
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverse (Vector4d vr, Vector4d v1) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      double x = (v1.x - p.x * v1.w)/s;
      double y = (v1.y - p.y * v1.w)/s;
      double z = (v1.z - p.z * v1.w)/s;

      vr.x = R.m00 * x + R.m10 * y + R.m20 * z;
      vr.y = R.m01 * x + R.m11 * y + R.m21 * z;
      vr.z = R.m02 * x + R.m12 * y + R.m22 * z;
      
      vr.w = v1.w;
      
      return true;
   }

   /**
    * {@inheritDoc}
    * 
    * @return true (transform is never singular)
    */
   public boolean invert() {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      p.inverseTransform (R);
      p.scale(-1.0/s);
      R.transpose();
      s = 1.0/s;
      
      updateInternalMatrix();
      return true;
   }

   /**
    * Inverts transform X and places the result in this transform.
    * 
    * @param X
    * transform to invert
    * @return true (transform is never singular)
    */
   public boolean invert (ScaledRigidTransform3d X) {
      if (!X.myExternalComponentsValidP) {
         X.updateExternalComponents();
      }
      p.inverseTransform (X.R, X.p);
      p.scale(-1.0/X.s);
      R.transpose (X.R);
      s = 1.0/X.s;
      
      updateInternalMatrix();
      return true;
   }

   /**
    * Returns a string representation of this transformation as a 4 x 4 matrix.
    */
   public String toString() {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat ("%g"), MATRIX_4X4_STRING);
   }

   /**
    * Returns a string representation of this transformation as a 4 x 4 matrix,
    * with each number formatted according to a supplied numeric format.
    * 
    * @param numberFmtStr
    * numeric format string (see {@link NumberFormat NumberFormat})
    */
   public String toString (String numberFmtStr) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, new NumberFormat (numberFmtStr), MATRIX_4X4_STRING);
   }

   /**
    * Returns a specified string representation of this transformation, with
    * each number formatted according to the a supplied numeric format.
    * 
    * @param numberFmtStr
    * numeric format string (see {@link NumberFormat NumberFormat})
    * @param outputCode
    * desired representation, which should be either
    * {@link #AXIS_ANGLE_STRING AXIS_ANGLE_STRING},
    * {@link #MATRIX_4X4_STRING MATRIX_4X4_STRING}, or
    * {@link #MATRIX_3X4_STRING MATRIX_3X4_STRING}
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
    * {@link #AXIS_ANGLE_STRING AXIS_ANGLE_STRING},
    * {@link #MATRIX_4X4_STRING MATRIX_4X4_STRING}, or
    * {@link #MATRIX_3X4_STRING MATRIX_3X4_STRING}
    */
   public String toString (NumberFormat numberFmt, int outputCode) {
      StringBuffer sbuf = new StringBuffer (80);
      return toString (sbuf, numberFmt, outputCode);
   }

   String toString (StringBuffer sbuf, NumberFormat numberFmt, int outputCode) {
      sbuf.setLength (0);
      if (outputCode == MATRIX_3X4_STRING || outputCode == MATRIX_4X4_STRING) {
         sbuf.append ("[ ");
         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
               numberFmt.format (A.get (i, j), sbuf);
               sbuf.append (' ');
            }
            numberFmt.format (p.get (i), sbuf);
            if (i < 2 || outputCode == MATRIX_4X4_STRING) {
               sbuf.append ("\n  ");
            }
         }
         if (outputCode == MATRIX_4X4_STRING) {
            for (int j = 0; j < 3; j++) {
               numberFmt.format (0, sbuf);
               sbuf.append (' ');
            }
            numberFmt.format (1, sbuf);
         }
         sbuf.append (" ]");
      }
      else if (outputCode == AXIS_ANGLE_STRING) {
         AxisAngle axisAng = new AxisAngle();

         sbuf.append ("[ ");
         numberFmt.format (p.x, sbuf);
         sbuf.append (' ');
         numberFmt.format (p.y, sbuf);
         sbuf.append (' ');
         numberFmt.format (p.z, sbuf);
         sbuf.append ("  ");
         R.getAxisAngle (axisAng);
         numberFmt.format (axisAng.axis.x, sbuf);
         sbuf.append (' ');
         numberFmt.format (axisAng.axis.y, sbuf);
         sbuf.append (' ');
         numberFmt.format (axisAng.axis.z, sbuf);
         sbuf.append (' ');
         numberFmt.format (Math.toDegrees (axisAng.angle), sbuf);
         sbuf.append (' ');
         numberFmt.format(s, sbuf);
         sbuf.append (" ]");
      }
      else {
         throw new IllegalArgumentException ("Unknown display format");
      }
      return sbuf.toString();
   }

//   /**
//    * Premultiplies this transform by an affine transform, and places the rigid
//    * body component in this transform and the stretch and shear component in S.
//    * More specifically, let this transform be originally described by
//    * 
//    * <pre>
//    *        [s0 R0  p0  ]
//    * this = [           ]
//    *        [   0    1  ]
//    * </pre>
//    * 
//    * and the affine transform be described by
//    * 
//    * <pre>
//    *        [  A   pa  ]     [  Sa Ra   pa  ]
//    * Xa =   [          ]  =  [              ]
//    *        [  0    1  ]     [    0      1  ]
//    * </pre>
//    * 
//    * where the A component of Xa is factored into Sa Ra using a left polar
//    * decomposition with sign adjustment to ensure that Ra is right-handed. Then
//    * we form the product
//    * 
//    * <pre>
//    *           [  s0 Sa Ra R0   Sa Ra p0 + pa  ]
//    * Xa this = [                               ]
//    *           [      0               1        ]
//    * </pre>
//    * 
//    * and then set the rotation and translation of this transform to Ra R0 and
//    * Sa Ra p0 + pa.  This transform's scale, s0, remains unchanged.
//    * 
//    * @param Xa
//    * affine transform to pre-multiply this transform by
//    * @param Sa
//    * optional argument to return stretch and shear.
//    */
//   public void mulAffineLeft (AffineTransform3dBase Xa, Matrix3d Sa) {
//
//      if (Xa instanceof ScaledRigidTransform3d) { 
//         // then no matrix decomposition is needed
//         this.mul ((ScaledRigidTransform3d)Xa, this);
//         if (Sa != null) {
//            Sa.setIdentity();
//         }
//         return;
//      }
//
//      RotationMatrix3d Ra = new RotationMatrix3d();
//      SVDecomposition3d SVD = new SVDecomposition3d();
//      SVD.leftPolarDecomposition (Sa, Ra, Xa.M);
//
//      // if (Sa == null) {
//      //    Sa = new Matrix3d();
//      // }
//
//      // SVDecomposition3d SVD = new SVDecomposition3d();
//      // SVD.factor (Xa.M);
//      // Matrix3d U = SVD.getU();
//      // Matrix3d V = SVD.getV();
//      // Vector3d sig = new Vector3d();
//      // SVD.getS (sig);
//
//      // double detU = U.orthogonalDeterminant();
//      // double detV = V.orthogonalDeterminant();
//      // if (detV * detU < 0) { /* then one is negative and the other positive */
//      //    if (detV < 0) { /* negative last column of V */
//      //       V.m02 = -V.m02;
//      //       V.m12 = -V.m12;
//      //       V.m22 = -V.m22;
//      //       sig.z = -sig.z;
//      //    }
//      //    else /* detU < 0 */
//      //    { /* negative last column of U */
//      //       U.m02 = -U.m02;
//      //       U.m12 = -U.m12;
//      //       U.m22 = -U.m22;
//      //       sig.z = -sig.z;
//      //    }
//      // }
//      // // now set Ra = U * V' and Sa = U diag(sig) U'
//      // V.mulTransposeRight (U, V);
//      // Ra.set (V);
//
//      // Sa.set (U);
//      // Sa.mulDiagonalRight (sig);
//      // Sa.mulTransposeRight (Sa, U);
//
//      p.mulAdd (Xa.M, p, Xa.b);
//      R.mul (Ra, R);
//      
//      updateInternalMatrix();
//
//   }

//   /**
//    * {@inheritDoc}
//    */
//   @Override
//   public void leftRigidFactor (AffineTransform3d XS, RigidTransform3d XR) {
//      if (XS != null) {
//         XS.A.setIdentity();
//         XS.A.scale(this.s);
//         XS.p.setZero();
//      }
//      if (XR != null) {
//         XR.R.set(R);
//         XR.p.set(p);
//      }
//   }

   /**
    * Reads the contents of this transformation from a ReaderTokenizer. There
    * are four allowed formats, each of which is delimited by square brackets.
    * 
    * <p>
    * The first format is a set of 8 numbers in which the first three numbers
    * give the x, y, and z coordinates of the translation vector, the next three
    * numbers give the x, y, and z coordinates of a rotation axis, and the next
    * number gives a rotation angle, in degrees, about that axis, and the final
    * number gives the scale factor. For example,
    * 
    * <pre>
    * [ 10 20 30  0 1 0 90  2.0]
    * </pre>
    * 
    * defines a transformation with a translation of (10, 20, 30), a
    * rotation of 90 degrees about the y axis, and a scale of 2.0
    * 
    * <p>
    * The second format format is a set of 12 numbers describing the first three
    * rows of the transformation matrix in row-major order. For example,
    * 
    * <pre>
    * [ 0  -1  0  10 
    *   1   0  0  20
    *   0   0  1  20 ]
    * </pre>
    * 
    * defines a transformation with a translation of (10,20, 30) and a rotation
    * of 90 degrees about the z axis.
    * 
    * <p>
    * The third format is a set of 16 numbers describing all elements of the
    * transformation matrix in row-major order. The last four numbers, which
    * represent the fourth row, are actually ignored and instead assumed to be
    * 0, 0, 0, 1, in keeping with the structure of the transformation matrix. A
    * transformation with a translation of (30, 40, 50) and a rotation of 180
    * degrees about the x axis would be represented as
    * 
    * <pre>
    * [  1  0  0  30
    *    0 -1  0  40
    *    0  0 -1  50 
    *    0  0  0  1 ]
    * </pre>
    * 
    * <p>
    * The fourth format consists of a series of simple translational or
    * rotational transformation, which are the multiplied together to form the
    * final transformation. The following simple transformations may be
    * specified:
    * 
    * <dl>
    * <dt><code>trans</code> x y z
    * <dd>A translation with the indicated x, y, and z values;
    * <dt><code>rotX</code> ang
    * <dd>A rotation of "ang" degrees about the x axis;
    * <dt><code>rotY</code> ang
    * <dd>A rotation of "ang" degrees about the y axis;
    * <dt><code>rotZ</code> ang
    * <dd>A rotation of "ang" degrees about the z axis;
    * <dt><code>rotAxis</code> x y z ang
    * <dd>A rotation of "ang" degrees about an arbitrary axis parallel to x, y,
    * and z.
    * <dt><code>scale</code> s
    * <dd>A scale of s
    * </dl>
    * 
    * For example, the string
    * 
    * <pre>
    * [ rotX 45 trans 0 0 100 rot 1 1 0 90 scale 2]
    * </pre>
    * 
    * describes a transformation which the product of a rotation of 45 degrees
    * about the y axis, a translation of 100 units along the z axis, a
    * rotation of 90 degrees about the axis (1, 1, 0), and s scaling of 2
    * 
    * @param rtok
    * ReaderTokenizer from which to read the transformation. Number Parsing
    * should be enabled
    * @throws IOException
    * if an I/O error occured or if the transformation description is not
    * consistent with one of the above formats.
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');

      double[] values = new double[16];
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
               throw new IOException (
                  "']' expected, got " + rtok.tokenName() +
                  "', line " + rtok.lineno());
            }
         }
         else if (nvals != 8 && nvals != 12 && nvals != 16) {
            throw new IOException (
               "Unexpected number of numeric values (" +
               nvals + "), line " + rtok.lineno());
         }
         else if (nvals == 8) {
            p.set (values[0], values[1], values[2]);
            R.setAxisAngle (values[3], values[4], values[5],
                            Math.toRadians (values[6]));
            s = values[7];
            updateInternalMatrix();
         }
         else if (nvals == 12 || nvals == 16) {
            A.m00 = values[0];
            A.m01 = values[1];
            A.m02 = values[2];

            A.m10 = values[4];
            A.m11 = values[5];
            A.m12 = values[6];

            A.m20 = values[8];
            A.m21 = values[9];
            A.m22 = values[10];
            
            p.set (values[3], values[7], values[11]);
            updateExternalComponents();
         }
      }
      else {
         p.set (0, 0, 0);
         R.setIdentity();
         s = 1.0;
         myExternalComponentsValidP = true;

         while (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equalsIgnoreCase ("trans")) {
               if (rtok.scanNumbers (values, 3) != 3) {
                  throw new IOException (
                     "keyword 'trans' requires 3 double values");
               }
               mulXyz (values[0], values[1], values[2]);
            }
            else if (rtok.sval.equalsIgnoreCase ("rotAxis")) {
               if (rtok.scanNumbers (values, 4) != 4) {
                  throw new IOException (
                     "keyword 'rotAxis' requires 4 double values");
               }
               mulAxisAngle (values[0], values[1], values[2],
                             Math.toRadians (values[3]));
            }
            else if (rtok.sval.equalsIgnoreCase ("rotX")) {
               if (rtok.scanNumbers (values, 1) != 1) {
                  throw new IOException (
                     "keyword 'rotX' requires a double value");
               }
               mulRotX (Math.toRadians (values[0]));
            }
            else if (rtok.sval.equalsIgnoreCase ("rotY")) {
               if (rtok.scanNumbers (values, 1) != 1) {
                  throw new IOException (
                     "keyword 'rotY' requires a double value");
               }
               mulRotY (Math.toRadians (values[0]));
            }
            else if (rtok.sval.equalsIgnoreCase ("rotZ")) {
               if (rtok.scanNumbers (values, 1) != 1) {
                  throw new IOException (
                     "keyword 'rotZ' requires a double value");
               }
               mulRotZ (Math.toRadians (values[0]));
            }
            else if (rtok.sval.equalsIgnoreCase("scale")) {
               if (rtok.scanNumbers (values, 1) != 1) {
                  throw new IOException (
                     "keyword 'scale' requires a double value");
               }
               scale(values[0]);
            }
            
            else {
               throw new IOException (
                  "Unexpected keyword '" + rtok.sval +
                  "', line " + rtok.lineno());
            }
            rtok.nextToken();
         }
         if (rtok.ttype != ']') {
            throw new IOException ("']' expected, line " + rtok.lineno());
         }
      }
   }

   // public static void main (String[] args)
   // {
   // RigidTransform3d X = new RigidTransform3d();
   // while (true)
   // {
   // try
   // { ReaderTokenizer rtok =
   // new ReaderTokenizer (new InputStreamReader(System.in));
   //
   // X.scan (rtok);
   // System.out.println (X.toString ("%9.4f", MATRIX_4X4_STRING));
   // System.out.println (X.toString ("%9.4f", MATRIX_3X4_STRING));
   // System.out.println (X.toString ("%9.4f", AXIS_ANGLE_STRING));
   // }
   // catch (Exception e)
   // { e.printStackTrace();
   // }
   // }
   // }

   public void setRandom() {
      R.setRandom();
      p.setRandom();
      
      // random scale
      Random gen = RandomGenerator.get();
      s = 2*gen.nextDouble()-1;

      updateInternalMatrix();
      validate();
   }
   
   /**
    * Returns the scale factor
    */
   public double getScale() {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      return s;
   }
   
   /**
    * Sets the scale factor to use in this transform
    * 
    * @param s scale factor
    */
   public void setScale (double s) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      this.s = s;
      updateInternalMatrix();
   }
   
   /**
    * Post-multiplies this transform by a uniform scaling transform:
    * 
    *  [ s I   0 ]
    *  [  0    1 ]
    *  
    * @param s scale factor
    */
   public void scale(double s) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      this.s = this.s*s;
   }
   
   @Override
   public void setRotation(AxisAngle axisAng) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      R.set(new RotationMatrix3d(axisAng));
      updateInternalMatrix();
   }
   
   @Override
   public void setRotation(Quaternion quat) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      R.set(quat);
      updateInternalMatrix();
   }
   
   @Override
   public void setRotation(RotationMatrix3d R) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      this.R.set(R);
      updateInternalMatrix();
   }
   
   public void getRotation(RotationMatrix3d R) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      R.set(this.R);
   }

   public ScaledRigidTransform3d copy() {
      return new ScaledRigidTransform3d (this);
   }

   /**
    * Sets this rigid transform to one that provides the best fit of q to p in
    * the least-squares sense: p ~ X q
    * 
    * @param p
    * set of target 3d points
    * @param q
    * set of input 3d points
    */
   public void fit (
      ArrayList<Point3d> p, ArrayList<Point3d> q)
      throws ImproperSizeException {
      assert p.size() == q.size();
      int n = p.size();
      if (n < 3)
         throw new ImproperSizeException ("3 or more data points required");

      MatrixNd Q = new MatrixNd (n, 3);
      MatrixNd P = new MatrixNd (n, 3);
      MatrixNd A = new MatrixNd (3, 3);
      Matrix3d U = new Matrix3d();
      Matrix3d V = new Matrix3d();
      Vector3d vec = new Vector3d();
      Vector3d qmean = new Vector3d();
      Vector3d pmean = new Vector3d();
      SVDecomposition svd = new SVDecomposition();

      for (int i = 0; i < n; i++) {
         qmean.add (q.get (i));
         pmean.add (p.get (i));
      }
      qmean.scale (1.0 / n);
      pmean.scale (1.0 / n);

      double sq = 0;
      for (int i = 0; i < n; i++) {
         vec.sub (q.get (i), qmean);
         Q.setRow (i, vec);
         sq += vec.normSquared();

         vec.sub (p.get (i), pmean);
         P.setRow (i, vec);
      }
      sq /= n;

      A.mulTransposeLeft (P, Q);
      A.scale (1.0 / n);
      svd.factor (A);
      svd.get (U, vec, V);

      double detU = U.orthogonalDeterminant();
      double detV = V.orthogonalDeterminant();
      if (detV * detU < 0) { /* then one is negative and the other positive */
         if (detV < 0) { /* negative last column of V */
            V.m02 = -V.m02;
            V.m12 = -V.m12;
            V.m22 = -V.m22;
            vec.z = -vec.z;
         }
         else /* detU < 0 */
         { /* negative last column of U */
            U.m02 = -U.m02;
            U.m12 = -U.m12;
            U.m22 = -U.m22;
            vec.z = -vec.z;
         }
      }

      this.R.mulTransposeRight (U, V);
      double s = (vec.x + vec.y + vec.z) / sq;
      
      this.s = s;

      qmean.transform (this.R);
      qmean.scale(s);
      
      this.p.sub (pmean, qmean);
      myExternalComponentsValidP = true;
   }

   
   public RigidTransform3d getRigidPart(RigidTransform3d trans) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      if (trans == null) {
         trans = new RigidTransform3d();
      }
      trans.setRotation(this.R);
      trans.setTranslation(this.p);
      return trans;
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformPnt (Vector3d pr, Vector3d p0) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      pr.sub (p0, b);
      // replace M.mulInverse (pr) with:
      R.mulTranspose (pr);
      pr.scale (1/s);
   }

   /**
    * {@inheritDoc}
    */
   public void transformCovec (Vector3d nr, Vector3d n0) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      // replace M.mulInverseTranspose (nr, n0) with:
      R.mul (nr, n0);
      nr.scale (1/s);
   }

   /**
    * {@inheritDoc}
    */
   public void inverseTransformVec (Vector3d vr, Vector3d v0) {
      if (!myExternalComponentsValidP) {
         updateExternalComponents();
      }
      // replace M.mulInverse (vr, v0) with:
      R.mulTranspose (vr, v0);
      vr.scale (1/s);
   }
}
