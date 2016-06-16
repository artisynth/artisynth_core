/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.ArrayList;

/**
 * A 3 x 3 matrix which implements general 2D affine transformations. The matrix
 * has the specific form
 * 
 * <pre>
 *     [  A   p  ]
 * M = [         ]
 *     [  0   1  ]
 * </pre>
 * 
 * where A is a 2 x 2 matrix and p is a 2-vector. In homogeneous coordinates,
 * this implements an affine transform of the form <br>
 * A v + b <br>
 */
public class AffineTransform2d extends AffineTransform2dBase {

   /**
    * Global identity transform. Should not be modified.
    */
   public static final AffineTransform2d IDENTITY = new AffineTransform2d();

   /**
    * Matrix component.
    */
   public final Matrix2d A;

   /**
    * Vector component.
    */
   public final Vector2d p;

   /**
    * Creates an AffineTransform2d and initializes it to the identity.
    */
   public AffineTransform2d() {
      A = new Matrix2d();
      p = new Vector2d();
      M = A;
      b = p;
      setIdentity();
   }

   /**
    * Creates an AffineTransform2d and initializes its components to the
    * specified values.
    * 
    * @param A
    * value for the A matrix
    * @param p
    * value for the p vector
    */
   public AffineTransform2d (Vector2d p, Matrix2d A) {
      this.A = new Matrix2d (A);
      this.p = new Vector2d (p);
      M = this.A;
      b = this.p;
   }
   
   /**
    * Creates an AffineTransform2d and initializes it
    * from an existing on
    * 
    * @param T initializing transform
    */
   public AffineTransform2d (AffineTransform2dBase T) {
      this.A = new Matrix2d (T.getMatrix ());
      this.p = new Vector2d (T.getOffset ());
      M = this.A;
      b = this.p;
   }

   /**
    * Multiplies this transformation transformation X and places the result in
    * this transformation.
    * 
    * @param X
    * right-hand transformation
    */
   public void mul (AffineTransform2dBase X) {
      mul (this, X);
   }

   /**
    * {@inheritDoc}
    */
   public void mul (AffineTransform2dBase X1, AffineTransform2dBase X2) {
      super.mul (X1, X2);
   }

   /**
    * Multiplies this transformation by the inverse of transformation X and
    * places the result in this transformation.
    * 
    * @param X
    * right-hand transformation
    * @return false if X is singular
    */
   public boolean mulInverse (AffineTransform2dBase X) {
      return super.mulInverseRight (this, X);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseRight (
      AffineTransform2dBase X1, AffineTransform2dBase X2) {
      return super.mulInverseRight (X1, X2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseLeft (
      AffineTransform2dBase X1, AffineTransform2dBase X2) {
      return super.mulInverseLeft (X1, X2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean mulInverseBoth (
      AffineTransform2dBase X1, AffineTransform2dBase X2) {
      return super.mulInverseBoth (X1, X2);
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert() {
      return super.invert();
   }

   /**
    * {@inheritDoc}
    */
   public boolean invert (AffineTransform2dBase X) {
      return super.invert (X);
   }

   /**
    * Sets this affine transform to the rigid body transform described by X.
    * 
    * @param X
    * rigid body transform to copy
    */
   public void set (RigidTransform2d X) {
      M.set (X.M);
      b.set (X.b);
   }

   /**
    * Sets this affine transform to one that provides the best fit of q to p in
    * the least-squares sense: p ~ X q
    * 
    * @param p
    * set of target 2d points
    * @param q
    * set of input 2d points
    */
   public void fit (ArrayList<Point2d> p, ArrayList<Point2d> q)
      throws ImproperSizeException {
      assert p.size() == q.size();
      int n = p.size();
      if (n < 3)
         throw new ImproperSizeException ("3 or more data points required");

      MatrixNd Q = new MatrixNd (n, 3);
      MatrixNd P = new MatrixNd (n, 2);
      MatrixNd A = new MatrixNd (3, 2);
      QRDecomposition qr = new QRDecomposition();

      for (int i = 0; i < n; i++) {
         Q.setRow (i, new double[] { q.get (i).x, q.get (i).y, 1.0 });
         P.setRow (i, p.get (i));
      }

      qr.factor (Q);
      qr.solve (A, P);
      A.transpose();
      A.getSubMatrix (0, 0, this.M);
      this.b.set (A.get (0, 2), A.get (1, 2));
   }

   public void setRandom() {
      A.setRandom();
      p.setRandom();
   }
   
   @Override
   public AffineTransform2d copy () {
      return new AffineTransform2d(this);
   }

}
