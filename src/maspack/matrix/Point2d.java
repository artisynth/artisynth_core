/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * A 2D spatial point.
 * 
 * The only difference between a point and a vector is in the the way it is
 * transformed by an affine transformation. In homogeneous coordinates, a point
 * is assigned a 1 in for its third entry, so the affine transformation of a
 * point takes the form <br>
 * A p + b <br>
 */
public class Point2d extends Vector2d {
   /**
    * Global zero point. Should not be modified.
    */
   public static final Point2d ZERO = new Point2d();

   /**
    * Creates a Point2d and initializes it to zero.
    */
   public Point2d() {
   }

   /**
    * Creates a Point2d by copying a vector
    * 
    * @param v
    * vector to be copied
    */
   public Point2d (Vector2d v) {
      set (v);
   }

   /**
    * Creates a Point2d with the supplied element values.
    * 
    * @param x
    * first element
    * @param y
    * second element
    */
   public Point2d (double x, double y) {
      set (x, y);
   }

   /**
    * Creates a Point2d with the supplied element values.
    * 
    * @param values
    * element values
    */
   public Point2d (double[] values) {
      set (values[0], values[1]);
   }

   /**
    * Applies a affine transformation to this point, in place.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform2dBase X) {
      X.M.mul (this);
      add (X.b);
   }

   /**
    * Applies a affine transformation to the point p1, and places the result in
    * this point.
    * 
    * @param X
    * affine transformation
    * @param p1
    * point to be transformed
    */
   public void transform (AffineTransform2dBase X, Vector2d p1) {
      X.M.mul (this, p1);
      add (X.b);
   }

   /**
    * Applies an inverse affine transformation to this point, in place.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform2dBase X) {
      sub (X.b);
      X.M.mulInverse (this);
   }

   /**
    * Applies an inverse affine transformation to the point p1, and places the
    * result in this vector.
    * 
    * @param X
    * affine transformation
    * @param p1
    * point to be transformed
    */
   public void inverseTransform (AffineTransform2dBase X, Vector2d p1) {
      sub (p1, X.b);
      X.M.mulInverse (this);
   }
}
