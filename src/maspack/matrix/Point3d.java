/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * A 3D spatial point.
 * 
 * The only difference between a point and a vector is in the the way it is
 * transformed by an affine transformation. In homogeneous coordinates, a point
 * is assigned a 1 in for its fourth entry, so the affine transformation of a
 * point takes the form <br>
 * A p + b <br>
 */
public class Point3d extends Vector3d {
   private static final long serialVersionUID = 1L;

   /**
    * Global zero point. Should not be modified.
    */
   public static final Point3d ZERO = new Point3d();
   public static final Point3d POSITIVE_INFINITY = 
      new Point3d(Double.POSITIVE_INFINITY, 
         Double.POSITIVE_INFINITY, 
         Double.POSITIVE_INFINITY);
   public static final Point3d NEGATIVE_INFINITY = 
      new Point3d(Double.NEGATIVE_INFINITY, 
         Double.NEGATIVE_INFINITY, 
         Double.NEGATIVE_INFINITY);

   /**
    * Creates a Point3d and initializes it to zero.
    */
   public Point3d() {
   }

   
   /**
    * Creates a Point3d by copying a Point3d
    * 
    * @param v
    * point to be copied
    */
   public Point3d (Point3d v) {
      set (v.x, v.y, v.z);
   }   
   
   
   /**
    * Creates a Point3d by copying a vector
    * 
    * @param v
    * vector to be copied
    */
   public Point3d (Vector3d v) {
      set (v);
   }

   /**
    * Creates a Point3d by copying an existing Vector. The
    * size of the copied vector must be at least 3.
    * 
    * @param v
    * vector to be copied
    */
   public Point3d (Vector v) {
      if (v.size() < 3) {
         throw new IllegalArgumentException (
            "v must have a size of at least 3");
      }
      set (v);
   }

   /**
    * Creates a Point3d with the supplied element values.
    * 
    * @param values
    * element values
    */
   public Point3d (double[] values) {
      set (values[0], values[1], values[2]);
   }

   /**
    * Creates a Point3d with the supplied element values.
    * 
    * @param x
    * first element
    * @param y
    * second element
    * @param z
    * third element
    */
   public Point3d (double x, double y, double z) {
      set (x, y, z);
   }

   /**
    * Applies an affine transformation to this point, in place.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform3dBase X) {
      X.M.mul (this);
      add (X.b);
   }

   /**
    * Applies an affine transformation to the point p1, and places the result in
    * this point.
    * 
    * @param X
    * affine transformation
    * @param p1
    * point to be transformed
    */
   public void transform (AffineTransform3dBase X, Vector3d p1) {
      X.M.mul (this, p1);
      add (X.b);
   }

   /**
    * Applies an inverse affine transformation to this point, in place.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform3dBase X) {
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
   public void inverseTransform (AffineTransform3dBase X, Vector3d p1) {
      sub (p1, X.b);
      X.M.mulInverse (this);
   }
   
   @Override
   public Point3d clone () {
      return (Point3d)super.clone ();
   }
}
