/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;

/**
 * Utility methods for transforming between matrices from world to planar
 * coordinates.
 */
public class PlanarProjection {
   /**
    * Left projects the matrix M1 onto the planar coordinate system and stores
    * the result in MR. Specifically, this method computes
    * 
    * <pre>
    *          T
    *    MR = R  M1
    * </pre>
    * 
    * where R is the 3 x 2 orthogonal matrix formed from the first two columns
    * of the rotation from plane to world coordinates.
    * 
    * @param MR
    * projected matrix
    * @param R
    * rotation from plane to world coordinates
    * @param M1
    * matrix to be projected
    */
   public static void leftProject (Matrix2d MR, RotationMatrix3d R, Matrix3x2 M1) {
      MR.m00 = R.m00 * M1.m00 + R.m10 * M1.m10 + R.m20 * M1.m20;
      MR.m10 = R.m01 * M1.m00 + R.m11 * M1.m10 + R.m21 * M1.m20;
      MR.m01 = R.m00 * M1.m01 + R.m10 * M1.m11 + R.m20 * M1.m21;
      MR.m11 = R.m01 * M1.m01 + R.m11 * M1.m11 + R.m21 * M1.m21;
   }

   /**
    * Left projects the matrix M1 onto the planar coordinate system and stores
    * the result in MR. Specifically, this method computes
    * 
    * <pre>
    *          T
    *    MR = R  M1
    * </pre>
    * 
    * where R is the 3 x 2 orthogonal matrix formed from the first two columns
    * of the rotation from plane to world coordinates.
    * 
    * @param MR
    * projected matrix
    * @param R
    * rotation from plane to world coordinates
    * @param M1
    * matrix to be projected
    */
   public static void leftProject (Matrix2x3 MR, RotationMatrix3d R, Matrix3d M1) {
      MR.m00 = R.m00 * M1.m00 + R.m10 * M1.m10 + R.m20 * M1.m20;
      MR.m10 = R.m01 * M1.m00 + R.m11 * M1.m10 + R.m21 * M1.m20;
      MR.m01 = R.m00 * M1.m01 + R.m10 * M1.m11 + R.m20 * M1.m21;
      MR.m11 = R.m01 * M1.m01 + R.m11 * M1.m11 + R.m21 * M1.m21;
      MR.m02 = R.m00 * M1.m02 + R.m10 * M1.m12 + R.m20 * M1.m22;
      MR.m12 = R.m01 * M1.m02 + R.m11 * M1.m12 + R.m21 * M1.m22;
   }

   /**
    * Right projects the matrix M1 onto the planar coordinate system and stores
    * the result in MR. Specifically, this method computes
    * 
    * <pre>
    *    MR = M1 R
    * </pre>
    * 
    * where R is the 3 x 2 orthogonal matrix formed from the first two columns
    * of the rotation from plane to world coordinates.
    * 
    * @param MR
    * projected matrix
    * @param R
    * rotation from plane to world coordinates
    * @param M1
    * matrix to be projected
    */
   public static void rightProject (
      Matrix2d MR, RotationMatrix3d R, Matrix2x3 M1) {
      MR.m00 = M1.m00 * R.m00 + M1.m01 * R.m10 + M1.m02 * R.m20;
      MR.m10 = M1.m10 * R.m00 + M1.m11 * R.m10 + M1.m12 * R.m20;
      MR.m01 = M1.m00 * R.m01 + M1.m01 * R.m11 + M1.m02 * R.m21;
      MR.m11 = M1.m10 * R.m01 + M1.m11 * R.m11 + M1.m12 * R.m21;
   }

   /**
    * Right projects the matrix M1 onto the planar coordinate system and stores
    * the result in MR. Specifically, this method computes
    * 
    * <pre>
    *    MR = M1 R
    * </pre>
    * 
    * where R is the 3 x 2 orthogonal matrix formed from the first two columns
    * of the rotation from plane to world coordinates.
    * 
    * @param MR
    * projected matrix
    * @param R
    * rotation from plane to world coordinates
    * @param M1
    * matrix to be projected
    */
   public static void rightProject (
      Matrix3x2 MR, RotationMatrix3d R, Matrix3d M1) {
      MR.m00 = M1.m00 * R.m00 + M1.m01 * R.m10 + M1.m02 * R.m20;
      MR.m10 = M1.m10 * R.m00 + M1.m11 * R.m10 + M1.m12 * R.m20;
      MR.m20 = M1.m20 * R.m00 + M1.m21 * R.m10 + M1.m22 * R.m20;
      MR.m01 = M1.m00 * R.m01 + M1.m01 * R.m11 + M1.m02 * R.m21;
      MR.m11 = M1.m10 * R.m01 + M1.m11 * R.m11 + M1.m12 * R.m21;
      MR.m21 = M1.m20 * R.m01 + M1.m21 * R.m11 + M1.m22 * R.m21;
   }
}
