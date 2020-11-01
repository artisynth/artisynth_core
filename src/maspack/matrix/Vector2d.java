/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC), Fabien P��an (ETHZ)
 * (method reference returns)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.Clonable;

/**
 * Implements a 2 element vector, along with its most commonly used operations.
 * 
 * <p>
 * The size of these vectors is fixed.
 */
public class Vector2d extends VectorBase
   implements Clonable, VectorObject<Vector2d> {

   private static double DOUBLE_PREC = 2.220446049250313e-16;

   /**
    * Global zero vector. Should not be modified.
    */
   public static final Vector2d ZERO = new Vector2d();

   /**
    * Global unit vector along the x axis. Should not be modified.
    */
   public static final Vector2d X_UNIT = new Vector2d (1, 0);

   /**
    * Global unit vector along the y axis. Should not be modified.
    */
   public static final Vector2d Y_UNIT = new Vector2d (0, 1);

   /**
    * Global vector containing ones. Should not be modified.
    */
   public static final Vector2d ONES = new Vector2d (1, 1);

   /**
    * First element
    */
   public double x;

   /**
    * Second element
    */
   public double y;

   /**
    * Creates a 2-vector and initializes its elements to 0.
    */
   public Vector2d() {
   }

   /**
    * Creates a 2-vector by copying an existing one.
    * 
    * @param v
    * vector to be copied
    */
   public Vector2d (Vector2d v) {
      set (v);
   }

   /**
    * Creates a 2-vector with the supplied element values.
    * 
    * @param x
    * first element
    * @param y
    * second element
    */
   public Vector2d (double x, double y) {
      set (x, y);
   }

   /**
    * Creates a 2-vector with the supplied element values.
    * 
    * @param values
    * element values
    */
   public Vector2d (double[] values) {
      set (values[0], values[1]);
   }

   /**
    * Returns the size of this vector (which is always 2)
    * 
    * @return 2
    */
   public int size() {
      return 2;
   }

   /**
    * Gets a single element of this vector. Elements 0 and 1 correspond to x and
    * y.
    * 
    * @param i
    * element index
    * @return element value throws ArrayIndexOutOfBoundsException if i is not in
    * the range 0 to 1.
    */
   public double get (int i) {
      switch (i) {
         case 0: {
            return x;
         }
         case 1: {
            return y;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException (i);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void get (double[] values) {
      if (values.length < 2) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= 2");
      }
      values[0] = x;
      values[1] = y;
   }

   /**
    * Copies the values of this vector into a general length vector v1, starting
    * at a specified index.
    * 
    * @param v1
    * vector into which values are to be copied
    * @param idx
    * starting index for copying values
    * @throws ArrayIndexOutOfBoundsException
    * if idx specifies a region within v1 that exceeds its bounds
    */
   public void get (VectorNd v1, int idx) {
      if (idx + 1 >= v1.size) {
         throw new ArrayIndexOutOfBoundsException();
      }
      v1.buf[idx + 0] = x;
      v1.buf[idx + 1] = y;
   }

   /**
    * Sets a single element of this vector. Elements 0 and 1 correspond to x and
    * y.
    * 
    * @param i
    * element index
    * @param value
    * element value
    * @throws ArrayIndexOutOfBoundsException
    * if i is not in the range 0 to 1.
    */
   public void set (int i, double value) {
      switch (i) {
         case 0: {
            x = value;
            break;
         }
         case 1: {
            y = value;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException (i);
         }
      }
   }

   /**
    * Sets the elements of this vector from an array of doubles. The array
    * must have a length of at least 2.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (double[] values) {
      if (values.length < 2) {
         throw new IllegalArgumentException (
            "argument 'values' must have a length of at least 2");
      }      
      x = values[0];
      y = values[1];
   }

   /**
    * {@inheritDoc}
    */
   public int set (double[] values, int idx) {
      x = values[idx++];
      y = values[idx++];
      return idx;
   }

   /**
    * Sets the values of this vector to those of v1.
    * 
    * @param v1
    * vector whose values are copied
    */
   public void set (Vector2d v1) {
      x = v1.x;
      y = v1.y;
   }

   /**
    * Sets the values of this vector from the elements of a general length
    * vector v1, starting at a specified index.
    * 
    * @param v1
    * vector from which new values are copied
    * @param idx
    * starting index for new values
    * @throws ArrayIndexOutOfBoundsException
    * if idx specifies a region within v1 that exceeds its bounds
    */
   public void set (VectorNd v1, int idx) {
      if (idx + 1 >= v1.size) {
         throw new ArrayIndexOutOfBoundsException();
      }
      x = v1.buf[idx + 0];
      y = v1.buf[idx + 1];
   }

   /**
    * Adds vector v1 to v2 and places the result in this vector.
    * 
    * @param v1
    * left-hand vector
    * @param v2
    * right-hand vector
    * @return this vector
    */
   public Vector2d add (Vector2d v1, Vector2d v2) {
      x = v1.x + v2.x;
      y = v1.y + v2.y;
      return this;
   }

   /**
    * Adds this vector to v1 and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    */
   public Vector2d add (Vector2d v1) {
      x += v1.x;
      y += v1.y;
      return this;
   }

   /**
    * Adds specified increments to the components of this vector.
    * 
    * @param dx
    * x increment
    * @param dy
    * y increment
    * @return this vector
    */
   public Vector2d add (double dx, double dy) {
      x += dx;
      y += dy;
      return this;
   }

   /**
    * Subtracts vector v1 from v2 and places the result in this vector.
    * 
    * @param v1
    * left-hand vector
    * @param v2
    * right-hand vector
    * @return this vector
    */
   public Vector2d sub (Vector2d v1, Vector2d v2) {
      x = v1.x - v2.x;
      y = v1.y - v2.y;
      return this;
   }

   /**
    * Subtracts v1 from this vector and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    */
   public Vector2d sub (Vector2d v1) {
      x -= v1.x;
      y -= v1.y;
      return this;
   }

   /**
    * Sets this vector to the negative of v1.
    * 
    * @param v1
    * vector to negate
    * @return this vector
    */
   public Vector2d negate (Vector2d v1) {
      x = -v1.x;
      y = -v1.y;
      return this;
   }

   /**
    * Negates this vector in place.
    * @return this vector
    */
   public Vector2d negate() {
      x = -x;
      y = -y;
      return this;
   }

   /**
    * Scales the elements of this vector by <code>s</code>.
    * 
    * @param s
    * scaling factor
    * @return this vector
    */
   public Vector2d scale (double s) {
      x = s * x;
      y = s * y;
      return this;
   }

   /**
    * Scales the elements of vector v1 by <code>s</code> and places the
    * results in this vector.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    * @return this vector
    */
   public Vector2d scale (double s, Vector2d v1) {
      x = s * v1.x;
      y = s * v1.y;
      return this;
   }

   /**
    * Rotates the vector
    * @return this vector
    */
   public Vector2d rotate (double cosine, double sine, Vector2d a) {
      double xn =  cosine * a.x - sine * a.y;
      y = sine * a.x + cosine * a.y;
      x = xn;
      return this;
   }

   /**
    * Computes the interpolation <code>(1-s) v1 + s v2</code> and places the
    * result in this vector.
    * 
    * @param v1
    * left-hand vector
    * @param s
    * interpolation factor
    * @param v2
    * right-hand vector
    */
   public void interpolate (Vector2d v1, double s, Vector2d v2) {
      double xn = (1 - s) * v1.x + s * v2.x;
      y = (1 - s) * v1.y + s * v2.y;
      x = xn;
   }

   /**
    * Computes the interpolation <code>(1-s) this + s v1</code> and places the
    * result in this vector.
    * 
    * @param s
    * interpolation factor
    * @param v1
    * right-hand vector
    */
   public void interpolate (double s, Vector2d v1) {
      double xn = (1 - s) * x + s * v1.x;
      y = (1 - s) * y + s * v1.y;
      x = xn;
   }

   // /**
   // * Computes <code>s this + v1</code> and places
   // * the result in this vector.
   // *
   // * @param s scaling factor
   // * @param v1 vector to be added
   // */
   // public void scaledAdd (double s, Vector2d v1)
   // {
   // x = s*x + v1.x;
   // y = s*y + v1.y;
   // }

   /**
    * Computes <code>s v1</code> and adds the result to this vector.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled and added
    * @return this vector
    */
   public Vector2d scaledAdd (double s, Vector2d v1) {
      x += s * v1.x;
      y += s * v1.y;
      return this;
   }

   /**
    * Computes <code>s v1 + v2</code> and places the result in this vector.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    * @param v2
    * vector to be added
    * @return this vector
    */
   public Vector2d scaledAdd (double s, Vector2d v1, Vector2d v2) {
      x = s * v1.x + v2.x;
      y = s * v1.y + v2.y;
      return this;
   }

   /**
    * Computes <code>s1 v1 + s2 v2</code> and places the result in this
    * vector.
    * 
    * @param s1
    * left-hand scaling factor
    * @param v1
    * left-hand vector
    * @param s2
    * right-hand scaling factor
    * @param v2
    * right-hand vector
    * @return this vector
    */
   public Vector2d combine (double s1, Vector2d v1, double s2, Vector2d v2) {
      x = s1 * v1.x + s2 * v2.x;
      y = s1 * v1.y + s2 * v2.y;
      return this;
   }

   /**
    * Returns the 2 norm of this vector. This is the square root of the sum of
    * the squares of the elements.
    * 
    * @return vector 2 norm
    */
   public double length() {
      return Math.sqrt (x * x + y * y);
   }

   /**
    * Returns the square of the 2 norm of this vector. This is the sum of the
    * squares of the elements.
    * 
    * @return square of the 2 norm
    */
   public double lengthSquared() {
      return x * x + y * y;
   }

   /**
    * Returns the Euclidean distance between this vector and vector v.
    * 
    * @return distance between this vector and v
    */
   public double distance (Vector2d v) {
      double dx = x - v.x;
      double dy = y - v.y;

      return Math.sqrt (dx * dx + dy * dy);
   }

   /**
    * Returns the squared of the Euclidean distance between this vector and
    * vector v.
    * 
    * @return squared distance between this vector and v
    */
   public double distanceSquared (Vector2d v) {
      double dx = x - v.x;
      double dy = y - v.y;

      return (dx * dx + dy * dy);
   }

   /**
    * Returns the maximum element value of this vector.
    * 
    * @return maximal element
    */
   public double maxElement() {
      double max = x;
      if (y > max) {
         max = y;
      }
      return max;
   }

   /**
    * Returns the minimum element value of this vector.
    * 
    * @return minimal element
    */
   public double minElement() {
      double min = x;
      if (y < min) {
         min = y;
      }
      return min;
   }

   /**
    * Returns the infinity norm of this vector. This is the maximum absolute
    * value over all elements.
    * 
    * @return vector infinity norm
    */
   public double infinityNorm() {
      double max = Math.abs (x);
      if (Math.abs (y) > max) {
         max = Math.abs (y);
      }
      return max;
   }

   /**
    * Returns the 1 norm of this vector. This is the sum of the absolute values
    * of the elements.
    * 
    * @return vector 1 norm
    */
   public double oneNorm() {
      return Math.abs (x) + Math.abs (y);
   }

   /**
    * Returns the dot product of this vector and v1.
    * 
    * @param v1
    * right-hand vector
    * @return dot product
    */
   public double dot (Vector2d v1) {
      return x * v1.x + y * v1.y;
   }

   /**
    * Returns the angle between this vector and v1. The angle is defined as
    * <code>acos(c)</code>, where <code>c</code> is the dot product of unit
    * vectors parallel to this vector and v1.
    * 
    * @param v1
    * right-hand vector
    * @return angle between vectors, in radians
    */
   public double angle (Vector2d v1) {
      double cos = dot (v1) / (length() * v1.length());
      // check against cos going out of bounds because of
      // numerical reasons
      if (cos >= 1) {
         return 0;
      }
      else if (cos <= -1) {
         return Math.PI;
      }
      else {
         return Math.acos (cos);
      }
   }

   /**
    * Normalizes this vector in place.
    * @return this vector
    */
   public Vector2d normalize() {
      double lenSqr = x * x + y * y;
      double err = lenSqr - 1;
      if (err > (2 * DOUBLE_PREC) || err < -(2 * DOUBLE_PREC)) {
         double len = Math.sqrt (lenSqr);
         x /= len;
         y /= len;
      }
      return this;
   }

   /**
    * Computes a unit vector in the direction of v1 and places the result in
    * this vector.
    * 
    * @param v1
    * vector to normalize
    * @return this vector
    */
   public Vector2d normalize (Vector2d v1) {
      double lenSqr = v1.x * v1.x + v1.y * v1.y;
      double err = lenSqr - 1;
      if (err > (2 * DOUBLE_PREC) || err < -(2 * DOUBLE_PREC)) {
         double len = Math.sqrt (lenSqr);
         x = v1.x / len;
         y = v1.y / len;
      }
      else {
         x = v1.x;
         y = v1.y;
      }
      return this;
   }

   /**
    * Sets this vector to one which is perpendicular to v1. The resulting vector
    * will not be normalized.
    * 
    * @param v1
    * perpendicular reference vector
    * @return this vector
    */
   public Vector2d perpendicular (Vector2d v1) {
      double xn = -v1.y;
      y = v1.x;
      x = xn;
      return this;
   }

   /**
    * Returns true if the elements of this vector equal those of vector
    * <code>v1</code>within a prescribed tolerance <code>epsilon</code>.
    * 
    * @param v1
    * vector to compare with
    * @param eps
    * comparison tolerance
    * @return false if the vectors are not equal within the specified tolerance
    */
   public boolean epsilonEquals (Vector2d v1, double eps) {
      return (abs (x - v1.x) <= eps && abs (y - v1.y) <= eps);
   }

   /**
    * Returns true if the elements of this vector exactly equal those of vector
    * <code>v1</code>.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal
    */
   public boolean equals (Vector2d v1) {
      return (x == v1.x && y == v1.y);
   }

   /**
    * Sets the elements of this vector to zero.
    */
   public void setZero() {
      x = 0;
      y = 0;
   }

   /**
    * Sets the elements of this vector to the prescribed values.
    * 
    * @param x
    * value for first element
    * @param y
    * value for second element
    */
   public void set (double x, double y) {
      this.x = x;
      this.y = y;
   }

   /**
    * Sets the elements of this vector to their absolute values.
    * @return this vector
    */
   public Vector2d absolute() {
      x = Math.abs (x);
      y = Math.abs (y);
      return this;
   }

   /**
    * Sets the elements of this vector to the absolute value of v1.
    * 
    * @param v1
    * vector to take the absolute value of
    * @return this vector
    */
   public Vector2d absolute (Vector2d v1) {
      x = Math.abs (v1.x);
      y = Math.abs (v1.y);
      return this;
   }

   /**
    * Sorts the contents of this vector by absolute element value, with x being
    * set to the largest value and y being set to the smallest value.
    */
   public void sortAbsolute() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);
      double tmp;

      if (absx >= absy) { // output x, y
         // nothing to do
      }
      else { // output y, x
         tmp = x;
         x = y;
         y = tmp;
      }
   }

   /**
    * Returns the index (0 or 1) of the element of v with the largest absolute
    * value.
    * 
    * @return index of largest absolute value
    */
   public int maxAbsIndex() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);

      if (absx >= absy) {
         return 0;
      }
      else {
         return 1;
      }
   }

   /**
    * Returns the index (0 or 1) of the element of v with the smallest absolute
    * value.
    * 
    * @return index of smallest absolute value
    */
   public int minAbsIndex() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);

      if (absx <= absy) {
         return 0;
      }
      else {
         return 1;
      }
   }

   /**
    * Sorts the contents of vector v1 by element value, with x being set to the
    * largest value and y being set to the smallest value, and places the
    * results in this vector.
    * 
    * @param v1
    * vector to sort
    */
   public void sort (Vector2d v1) {
      set (v1);
      sort();
   }

   /**
    * Sorts the contents of this vector by element value, with x being set to
    * the largest value and z being set to the smallest value.
    */
   public void sort() {
      double tmp;

      if (x >= y) { // output x, y
         // nothing to do
      }
      else { // output y, x
         tmp = x;
         x = y;
         y = tmp;
      }
   }

   /**
    * Computes the cross product of v1 and v2.
    * 
    * @param v1
    * first vector
    * @param v2
    * second vector
    * @return cross product
    */
   public double cross (Vector2d v1, Vector2d v2) {
      return v1.x * v2.y - v1.y * v2.x;
   }

   /**
    * Computes the cross product of this vector and v1.
    * 
    * @param v2
    * second vector
    * @return cross product
    */
   public double cross (Vector2d v2) {
      return x * v2.y - y * v2.x;
   }

   /**
    * Applies a rotational transformation to this vector, in place. This is
    * equivalent to multiplying the rotation matrix by this vector.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void transform (RotationMatrix2d R) {
      R.mul (this);
   }

   /**
    * Applies a rotational transformation to the vector v1 and stores the result
    * in this vector. This is equivalent to multiplying the rotation matrix by
    * v1.
    * 
    * @param R
    * rotational transformation matrix
    * @param v1
    * vector to transform
    */
   public void transform (RotationMatrix2d R, Vector2d v1) {
      R.mul (this, v1);
   }

   /**
    * Applies an inverse rotational transformation to this vector, in place.
    * This is equivalent to multiplying the transpose (or inverse) of the
    * rotation matrix by this vector.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void inverseTransform (RotationMatrix2d R) {
      R.mulTranspose (this);
   }

   /**
    * Applies an inverse rotational transformation to the vector v1, and stores
    * the result in this vector. This is equivalent to multiplying the transpose
    * (or inverse) of the rotation matrix by this v1.
    * 
    * @param R
    * rotational transformation matrix
    * @param v1
    * vector to transform
    */
   public void inverseTransform (RotationMatrix2d R, Vector2d v1) {
      R.mulTranspose (this, v1);
   }

   /**
    * Applies a affine transformation to this vector, in place.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform2dBase X) {
      X.M.mul (this);
   }

   /**
    * Applies a affine transformation to the vector v1, and places the result in
    * this vector.
    * 
    * @param X
    * affine transformation
    * @param v1
    * vector to be transformed
    */
   public void transform (AffineTransform2dBase X, Vector2d v1) {
      X.M.mul (this, v1);
   }

   /**
    * Applies an inverse affine transformation to this vector, in place.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform2dBase X) {
      X.M.mulInverse (this);
   }

   /**
    * Applies an inverse affine transformation to the vector v1, and places the
    * result in this vector.
    * 
    * @param X
    * affine transformation
    * @param v1
    * vector to be transformed
    */
   public void inverseTransform (AffineTransform2dBase X, Vector2d v1) {
      X.M.mulInverse (this, v1);
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom() {
      super.setRandom();
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper) {
      super.setRandom (lower, upper);
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper, Random generator) {
      super.setRandom (lower, upper, generator);
   }

   /**
    * Updates a bounding box to include this vector. The box is described by
    * minimum and maximum corner points, which are changed if necessary.
    * 
    * @param vmin
    * minimum corner of the bounding box
    * @param vmax
    * maximum corner of the bounding box
    */
   public void updateBounds (Vector2d vmin, Vector2d vmax) {
      if (x > vmax.x) {
         vmax.x = x;
      }
      if (x < vmin.x) {
         vmin.x = x;
      }
      if (y > vmax.y) {
         vmax.y = y;
      }
      if (y < vmin.y) {
         vmin.y = y;
      }
   }

   /**
    * Computes the element-wise maximum of this vector and vector v and places
    * the result in this vector.
    * 
    * @param v
    * vector to compare with
    * @return this vector
    */
   public Vector2d max (Vector2d v) {
      if (v.x > x) {
         x = v.x;
      }
      if (v.y > y) {
         y = v.y;
      }
      return this;
   }

   /**
    * Computes the element-wise minimum of this vector and vector v and places
    * the result in this vector.
    * 
    * @param v
    * vector to compare with
    * @return this vector
    */
   public Vector2d min (Vector2d v) {
      if (v.x < x) {
         x = v.x;
      }
      if (v.y < y) {
         y = v.y;
      }
      return this;
   }

   public Vector2d clone() {
      return (Vector2d)super.clone();
   }

   /* VectorObject implementation. It is currently necessary to define the
    * scale and add methods as scaleObj(), addObj(), and scaledAddObj(), since
    * the corresponding scale(), add() and scaledAdd() methods have
    * incompatible return types across different classes (some return a
    * reference to their object, while others return {@code void}).
    */

   /**
    * {@inheritDoc}
    */
   public void scaleObj (double s) {
      scale (s, this);
   }

   /**
    * {@inheritDoc}
    */
   public void addObj (Vector2d v1) {
      add (v1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Vector2d v1) {
      scaledAdd (s, v1);
   }

}
