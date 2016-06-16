/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.RandomGenerator;

public class RotationMatrix2d extends Matrix2dBase {
   /**
    * Global identity rotation. Should not be modified.
    */
   public static final RotationMatrix2d IDENTITY = new RotationMatrix2d();

   public RotationMatrix2d() {
      m00 = 1.0;
      m01 = 0.0;
      m10 = 0.0;
      m11 = 1.0;
   }

   public RotationMatrix2d (double ang) {
      setAngle (ang);
   }

   public RotationMatrix2d (RotationMatrix2d R) {
      set (R);
   }

   public void mul (RotationMatrix2d r1) {
      super.mul (r1);
   }

   public void mul (RotationMatrix2d r1, RotationMatrix2d r2) {
      super.mul (r1, r2);
   }

   public void mulInverse (RotationMatrix2d r1) {
      super.mulTranspose (r1);
   }

   public void mulInverseLeft (RotationMatrix2d r1, RotationMatrix2d r2) {
      super.mulTransposeLeft (r1, r2);
   }

   public void mulInverseRight (RotationMatrix2d r1, RotationMatrix2d r2) {
      super.mulTransposeRight (r1, r2);
   }

   public void mulInverseBoth (RotationMatrix2d r1, RotationMatrix2d r2) {
      super.mulTransposeBoth (r1, r2);
   }

   /**
    * {@inheritDoc}
    * 
    * @return true (matrix is never singular)
    */
   public boolean mulInverse (Vector2d vr, Vector2d v1) {
      double x = m00 * v1.x + m10 * v1.y;
      double y = m01 * v1.x + m11 * v1.y;
      vr.x = x;
      vr.y = y;
      return true;
   }

   /**
    * {@inheritDoc}
    * 
    * @return true (matrix is never singular)
    */
   public boolean mulInverse (Vector2d vr) {
      double x = m00 * vr.x + m10 * vr.y;
      double y = m01 * vr.x + m11 * vr.y;
      vr.x = x;
      vr.y = y;
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
   public boolean mulInverseTranspose (Vector2d vr, Vector2d v1) {
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
   public boolean mulInverseTranspose (Vector2d vr) {
      mul (vr, vr);
      return true;
   }

   public boolean invert() {
      super.transpose();
      return true;
   }

   public boolean invert (RotationMatrix2d r1) {
      super.transpose (r1);
      return true;
   }

   public void transform (Vector2d vr, Vector2d v1) {
      super.mul (vr, v1);
   }

   public void transform (Vector2d vr) {
      super.mul (vr);
   }

   public void inverseTransform (Vector2d vr, Vector2d v1) {
      super.mulTranspose (vr, v1);
   }

   public void inverseTransform (Vector2d vr) {
      super.mulTranspose (vr);
   }

   public void setAngle (double ang) {
      double s = Math.sin (ang);
      double c = Math.cos (ang);
      m00 = c;
      m10 = s;
      m01 = -s;
      m11 = c;
   }

   public void mulAngle (double ang) {
      double s = Math.sin (ang);
      double c = Math.cos (ang);

      double x00 = m00 * c + m01 * s;
      double x10 = m10 * c + m11 * s;
      double x01 = -m00 * s + m01 * c;
      double x11 = -m10 * s + m11 * c;

      m00 = x00;
      m01 = x01;
      m10 = x10;
      m11 = x11;
   }

   public double getAngle() {
      return Math.atan2 (m10, m00);
   }

   public void setRandom() {
      setRandom (RandomGenerator.get());
   }

   public void setRandom (Random generator) {
      double ang = 2 * Math.PI * (generator.nextDouble() - 0.5);
      setAngle (ang);
   }
   
   @Override
   public RotationMatrix2d clone () {
      return new RotationMatrix2d (this);
   }
}
