/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

public class QRDecomposition3d {
   private Matrix3d Q;
   private Matrix3d R;

   /**
    * Computes P A, where P is the Householder matrix for the vector (1 v1 v2),
    * and returns the result in this matrix.
    */
   private void rowHouseMul (double v1, double v2, Matrix3d A) {
      double beta = -2 / (1 + v1 * v1 + v2 * v2);
      double w0 = beta * (A.m00 + A.m10 * v1 + A.m20 * v2);
      double w1 = beta * (A.m01 + A.m11 * v1 + A.m21 * v2);
      double w2 = beta * (A.m02 + A.m12 * v1 + A.m22 * v2);

      A.m00 += w0;
      A.m10 += v1 * w0;
      A.m20 += v2 * w0;

      A.m01 += w1;
      A.m11 += v1 * w1;
      A.m21 += v2 * w1;

      A.m02 += w2;
      A.m12 += v1 * w2;
      A.m22 += v2 * w2;
   }

   /**
    * Computes P A, where P is a Householder matrix for the vector (1 v1) that
    * operates on the lower-right 2x2 submatrix of A, and returns the result in
    * this matrix.
    */
   private void rowHouseMul (double v2, Matrix3d A) {
      double beta = -2 / (1 + v2 * v2);
      double w0 = beta * (A.m10 + A.m20 * v2);
      double w1 = beta * (A.m11 + A.m21 * v2);
      double w2 = beta * (A.m12 + A.m22 * v2);

      A.m10 += w0;
      A.m20 += v2 * w0;

      A.m11 += w1;
      A.m21 += v2 * w1;

      A.m12 += w2;
      A.m22 += v2 * w2;
   }

   public void set (Matrix3dBase A) {
      double v1, v2;

      R.set (A);

      // compute HouseHolder vector to reduce R00 - R20
      double mu = Math.sqrt (R.m00 * R.m00 + R.m10 * R.m10 + R.m20 * R.m20);
      v1 = R.m10;
      v2 = R.m20;
      if (mu != 0) {
         double beta = R.m00 + (R.m00 >= 0 ? mu : -mu);
         v1 /= beta;
         v2 /= beta;
      }
      // and reduce ...
      rowHouseMul (v1, v2, R);
      Q.setIdentity();
      rowHouseMul (v1, v2, Q);

      // compute HouseHolder vector to reduce R11 - R21
      mu = Math.sqrt (R.m11 * R.m11 + R.m21 * R.m21);
      v2 = R.m21;
      if (mu != 0) {
         double beta = R.m11 + (R.m11 >= 0 ? mu : -mu);
         v2 /= beta;
      }
      // and reduce ...
      rowHouseMul (v2, R);
      rowHouseMul (v2, Q);
      Q.transpose();

      // Just to make sure :-)
      R.m10 = 0;
      R.m20 = 0;
      R.m21 = 0;
   }

   public QRDecomposition3d() {
      Q = new Matrix3d();
      R = new Matrix3d();
   }

   // public boolean isQRightHanded()
   // {
   // double ax = Q.m10*Q.m21 - Q.m20*Q.m11;
   // double ay = Q.m20*Q.m01 - Q.m00*Q.m21;
   // double az = Q.m00*Q.m11 - Q.m10*Q.m01;
   // return (ax*Q.m02 + ay*Q.m12 + az*Q.m22 > 0);
   // }

   public void getQ (Matrix3dBase Q) {
      Q.set (this.Q);
   }

   public void getR (Matrix3dBase R) {
      R.set (this.R);
   }
}
