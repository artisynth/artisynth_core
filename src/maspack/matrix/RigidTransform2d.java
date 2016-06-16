/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.ArrayList;

/**
 * A specialized 3 x 3 matrix that implements a two-dimensional rigid body
 * transformation in homogeneous coordinates.
 * 
 * <p>
 * A rigid body transformation is used to transform a point from one spatial
 * coordinate frame into another. If x0 and x1 denote the point in the orginal
 * frame 0 and target frame 1, respectively, then the transformation is computed
 * according to
 * 
 * <pre>
 * x1 = R x0 + p
 * </pre>
 * 
 * where R is a 2 x 2 rotation matrix and p is a translation vector. In
 * homogenous coordinates, this operation can be represented as
 * 
 * <pre>
 * [ x1 ]   [ R  p ] [ x0 ]
 * [    ] = [      ] [    ]
 * [  1 ]   [ 0  1 ] [  1 ]
 * </pre>
 * 
 * The components p and R of the transformation represent the position and
 * orientation of frame 0 with respect to frame 1. In particular, the
 * translation vector p gives the origin position, while the columns of R give
 * the directions of the axes.
 * 
 * <p>
 * If X01 is a transformation from frame 0 to frame 1, and X12 is a
 * transformation from frame 1 to frame 2, then the transformation from frame 0
 * to frame 2 is given by the product
 * 
 * <pre>
 * X02 = X12 X01
 * </pre>
 * 
 * In this way, a transformation can be created by multiplying a series of
 * sub-transformations.
 * 
 * <p>
 * If X01 is a transformation from frame 0 to frame 1, then the inverse
 * transformation X10 is a transformation from frame 1 to frame 0, and is given
 * by
 * 
 * <pre>
 *       [  T    T   ]
 *       [ R   -R  p ]
 * X10 = [           ]
 *       [ 0     1   ]
 * </pre>
 */
public class RigidTransform2d extends AffineTransform2dBase {
   /**
    * Global identity transform. Should not be modified.
    */
   public static final RigidTransform2d IDENTITY = new RigidTransform2d();

   /**
    * Rotation matrix associated with this transformation.
    */
   public final RotationMatrix2d R;

   /**
    * Translation vector associated with this transformation.
    */
   public final Vector2d p;

   /**
    * Creates a new transformation initialized to the identity.
    */
   public RigidTransform2d() {
      this.R = new RotationMatrix2d();
      this.p = new Vector2d();
      M = R;
      b = p;
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
   public RigidTransform2d (Vector2d p, RotationMatrix2d R) {
      this.R = new RotationMatrix2d (R);
      this.p = new Vector2d (p);
      M = R;
      b = p;
   }

   /**
    * Creates a new transformation with the specified translation vector and
    * rotation angle
    * 
    * @param p
    * translation vector
    * @param ang
    * rotation angle (radians)
    */
   public RigidTransform2d (Vector2d p, double ang) {
      this.R = new RotationMatrix2d (ang);
      this.p = new Vector2d (p);
      M = this.R;
      b = this.p;
   }

   /**
    * Creates a new transformation with the specified translation values and
    * rotation angle
    * 
    * @param x
    * translation x component
    * @param y
    * translation y component
    * @param ang
    * rotation angle (radians)
    */
   public RigidTransform2d (double x, double y, double ang) {
      this.R = new RotationMatrix2d (ang);
      this.p = new Vector2d (x, y);
      M = R;
      b = p;
   }

   /**
    * Creates a new transformation which is a copy of an existing one.
    * 
    * @param X
    * transform to copy
    */
   public RigidTransform2d (RigidTransform2d X) {
      this.R = new RotationMatrix2d (X.R);
      this.p = new Vector2d (X.p);
      M = R;
      b = p;
   }

   /**
    * Post-multiplies this transformation by another and places the result in
    * this transformation.
    * 
    * @param X
    * transformation to multiply by
    */
   public void mul (RigidTransform2d X) {
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
   public void mul (RigidTransform2d X1, RigidTransform2d X2) {
      double p1x = X1.p.x;
      double p1y = X1.p.y;
      X1.R.mul (p, X2.p);
      p.x += p1x;
      p.y += p1y;
      R.mul (X1.R, X2.R);
   }

   /**
    * Post-multiplies this transformation by the inverse of transformation X and
    * places the result in this transformation.
    * 
    * @param X
    * right-hand transformation
    */
   public void mulInverse (RigidTransform2d X) {
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
   public void mulInverseRight (RigidTransform2d X1, RigidTransform2d X2) {
      double p1x = X1.p.x;
      double p1y = X1.p.y;
      R.mulInverseRight (X1.R, X2.R);
      R.mul (p, X2.p);
      p.x = p1x - p.x;
      p.y = p1y - p.y;
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
   public void mulInverseLeft (RigidTransform2d X1, RigidTransform2d X2) {
      p.sub (X2.p, X1.p);
      X1.R.mulTranspose (p, p);
      R.mulInverseLeft (X1.R, X2.R);
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
   public void mulInverseBoth (RigidTransform2d X1, RigidTransform2d X2) {
      double p1x = X1.p.x;
      double p1y = X1.p.y;
      X2.R.mulTranspose (p, X2.p);
      p.x += p1x;
      p.y += p1y;
      X1.R.mulTranspose (p);
      p.negate();
      R.mulInverseBoth (X1.R, X2.R);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverse (Vector3d vr, Vector3d v1) {
      double x = v1.x - p.x * v1.z;
      double y = v1.y - p.y * v1.z;

      vr.x = R.m00 * x + R.m10 * y;
      vr.y = R.m01 * x + R.m11 * y;
      vr.z = v1.z;
      return true;
   }

   /**
    * {@inheritDoc}
    * 
    * @return true (transform is never singular)
    */
   public boolean invert() {
      p.inverseTransform (R);
      p.negate();
      R.transpose();
      return true;
   }

   /**
    * Inverts transform X and places the result in this transform.
    * 
    * @param X
    * transform to invert
    * @return true (transform is never singular)
    */
   public boolean invert (RigidTransform2d X) {
      p.inverseTransform (X.R, X.p);
      p.negate();
      R.transpose (X.R);
      return true;
   }

   /**
    * Sets this rigid transform to one that provides the best fit of q to p in
    * the least-squares sense: p ~ X q
    * 
    * @param p
    * set of target 2d points
    * @param q
    * set of input 2d points
    * @param doScaling
    * flag to apply uniform scaling in addition to rigid transform
    */
   public void fit (
      ArrayList<Point2d> p, ArrayList<Point2d> q, boolean doScaling)
      throws ImproperSizeException {
      assert p.size() == q.size();
      int n = p.size();
      if (n < 2)
         throw new ImproperSizeException ("2 or more data points required");

      MatrixNd Q = new MatrixNd (n, 2);
      MatrixNd P = new MatrixNd (n, 2);
      MatrixNd A = new MatrixNd (2, 2);
      Matrix2d U = new Matrix2d();
      Matrix2d V = new Matrix2d();
      Vector2d vec = new Vector2d();
      Vector2d qmean = new Vector2d();
      Vector2d pmean = new Vector2d();
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

      double detU = U.determinant();
      double detV = V.determinant();
      if (detV * detU < 0) { /* then one is negative and the other positive */
         if (detV < 0) { /* negative last column of V */
            V.m01 = -V.m01;
            V.m11 = -V.m11;
            vec.y = -vec.y;
         }
         else /* detU < 0 */
         { /* negative last column of U */
            U.m01 = -U.m01;
            U.m11 = -U.m11;
            vec.y = -vec.y;
         }
      }

      this.R.mulTransposeRight (U, V);
      if (doScaling) {
         double s = (vec.x + vec.y) / sq;
         this.R.scale (s, this.R);
      }

      qmean.transform (this.R);
      this.p.sub (pmean, qmean);
   }

   public void fit (ArrayList<Point2d> p, ArrayList<Point2d> q)
      throws ImproperSizeException {
      fit (p, q, false /* no scaling */);
   }
   
   @Override
   public RigidTransform2d copy() {
      return new RigidTransform2d(this);
   }

}
