/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC), Fabien Pean (ETHZ)
 * (method reference returns)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.Clonable;

/**
 * Implements a 4 element vector, along with its most commonly used operations.
 * 
 * <p>
 * The size of these vectors is fixed.
 */
public class Vector4d extends VectorBase
   implements Clonable, VectorObject<Vector4d> {

   private static double DOUBLE_PREC = 2.220446049250313e-16;

   /**
    * Global zero vector. Should not be modified.
    */
   public static final Vector4d ZERO = new Vector4d();

   /**
    * Global unit vector along the x axis. Should not be modified.
    */
   public static final Vector4d X_UNIT = new Vector4d (1, 0, 0, 0);

   /**
    * Global unit vector along the y axis. Should not be modified.
    */
   public static final Vector4d Y_UNIT = new Vector4d (0, 1, 0, 0);

   /**
    * Global unit vector along the z axis. Should not be modified.
    */
   public static final Vector4d Z_UNIT = new Vector4d (0, 0, 1, 0);

   /**
    * Global unit vector along the w axis. Should not be modified.
    */
   public static final Vector4d W_UNIT = new Vector4d (0, 0, 0, 1);

   /**
    * Global vector containing ones. Should not be modified.
    */
   public static final Vector4d ONES = new Vector4d (1, 1, 1, 1);

   /**
    * First element
    */
   public double x;

   /**
    * Second element
    */
   public double y;

   /**
    * Third element
    */
   public double z;

   /**
    * Fourth element
    */
   public double w;

   /**
    * Creates a 4-vector and initializes its elements to 0.
    */
   public Vector4d() {
   }

   /**
    * Creates a 4-vector by copying an existing one.
    * 
    * @param v
    * vector to be copied
    */
   public Vector4d (Vector4d v) {
      set (v);
   }

   /**
    * Creates a 4-vector with the supplied element values.
    * 
    * @param x
    * first element
    * @param y
    * second element
    * @param z
    * third element
    * @param w
    * fourth element
    */
   public Vector4d (double x, double y, double z, double w) {
      set (x, y, z, w);
   }

   /**
    * Returns the size of this vector (which is always 4)
    * 
    * @return 4
    */
   public int size() {
      return 4;
   }

   /**
    * Gets the first three values of this vector.
    * 
    * @param v
    * returns the first three values
    */
   public void get (Vector3d v) {
      v.x = this.x;
      v.y = this.y;
      v.z = this.z;
   }

   /**
    * Gets a single element of this vector. Elements 0, 1, 2, and 3 correspond
    * to x, y, z, and w.
    * 
    * @param i
    * element index
    * @return element value throws ArrayIndexOutOfBoundsException if i is not in
    * the range 0 to 3.
    */
   public double get (int i) {
      switch (i) {
         case 0: {
            return x;
         }
         case 1: {
            return y;
         }
         case 2: {
            return z;
         }
         case 3: {
            return w;
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
      if (values.length < 4) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= 4");
      }
      values[0] = x;
      values[1] = y;
      values[2] = z;
      values[3] = w;
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
      if (idx + 3 >= v1.size) {
         throw new ArrayIndexOutOfBoundsException();
      }
      v1.buf[idx + 0] = x;
      v1.buf[idx + 1] = y;
      v1.buf[idx + 2] = z;
      v1.buf[idx + 3] = w;
   }

   /**
    * Sets a single element of this vector. Elements 0, 1, 2, and 3 correspond
    * to x, y, z, and w.
    * 
    * @param i
    * element index
    * @param value
    * element value
    * @throws ArrayIndexOutOfBoundsException
    * if i is not in the range 0 to 3.
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
         case 2: {
            z = value;
            break;
         }
         case 3: {
            w = value;
            break;
         }
         default: {
            throw new ArrayIndexOutOfBoundsException (i);
         }
      }
   }

   /**
    * Sets the elements of this vector from an array of doubles. The array
    * must have a length of at least 4.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (double[] values) {
      if (values.length < 4) {
         throw new IllegalArgumentException (
            "argument 'values' must have a length of at least 4");
      } 
      x = values[0];
      y = values[1];
      z = values[2];
      w = values[3];
   }
   
   /**
    * {@inheritDoc}
    */
   public int set (double[] values, int idx) {
      x = values[idx++];
      y = values[idx++];
      z = values[idx++];
      w = values[idx++];
      return idx;
   }

   /**
    * Sets the values of this vector to those of v1.
    * 
    * @param v1
    * vector whose values are copied
    */
   public void set (Vector4d v1) {
      x = v1.x;
      y = v1.y;
      z = v1.z;
      w = v1.w;
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
      if (idx + 3 >= v1.size) {
         throw new ArrayIndexOutOfBoundsException();
      }
      x = v1.buf[idx + 0];
      y = v1.buf[idx + 1];
      z = v1.buf[idx + 2];
      w = v1.buf[idx + 3];
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
   public Vector4d add (Vector4d v1, Vector4d v2) {
      x = v1.x + v2.x;
      y = v1.y + v2.y;
      z = v1.z + v2.z;
      w = v1.w + v2.w;
      return this;
   }

   /**
    * Adds this vector to v1 and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    */
   public Vector4d add (Vector4d v1) {
      x += v1.x;
      y += v1.y;
      z += v1.z;
      w += v1.w;
      return this;
   }

   /**
    * Adds specified increments to the components of this vector.
    * 
    * @param dx
    * x increment
    * @param dy
    * y increment
    * @param dz
    * z increment
    * @param dw
    * w increment
    * @return this vector
    */
   public Vector4d add (double dx, double dy, double dz, double dw) {
      x += dx;
      y += dy;
      z += dz;
      w += dw;
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
   public Vector4d sub (Vector4d v1, Vector4d v2) {
      x = v1.x - v2.x;
      y = v1.y - v2.y;
      z = v1.z - v2.z;
      w = v1.w - v2.w;
      return this;
   }

   /**
    * Subtracts v1 from this vector and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    */
   public Vector4d sub (Vector4d v1) {
      x -= v1.x;
      y -= v1.y;
      z -= v1.z;
      w -= v1.w;
      return this;
   }

   /**
    * Sets this vector to the negative of v1.
    * 
    * @param v1
    * vector to negate
    * @return this vector
    */
   public Vector4d negate (Vector4d v1) {
      x = -v1.x;
      y = -v1.y;
      z = -v1.z;
      w = -v1.w;
      return this;
   }

   /**
    * Negates this vector in place.
    * @return this vector
    */
   public Vector4d negate() {
      x = -x;
      y = -y;
      z = -z;
      w = -w;
      return this;
   }

   /**
    * Scales the elements of this vector by <code>s</code>.
    * 
    * @param s
    * scaling factor
    * @return this vector
    */
   public Vector4d scale (double s) {
      x = s * x;
      y = s * y;
      z = s * z;
      w = s * w;
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
   public Vector4d scale (double s, Vector4d v1) {
      x = s * v1.x;
      y = s * v1.y;
      z = s * v1.z;
      w = s * v1.w;
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
   public void interpolate (Vector4d v1, double s, Vector4d v2) {
      x = (1 - s) * v1.x + s * v2.x;
      y = (1 - s) * v1.y + s * v2.y;
      z = (1 - s) * v1.z + s * v2.z;
      w = (1 - s) * v1.w + s * v2.w;
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
   public void interpolate (double s, Vector4d v1) {
      x = (1 - s) * x + s * v1.x;
      y = (1 - s) * y + s * v1.y;
      z = (1 - s) * z + s * v1.z;
      w = (1 - s) * w + s * v1.w;
   }

   // /**
   // * Computes <code>s this + v1</code> and places
   // * the result in this vector.
   // *
   // * @param s scaling factor
   // * @param v1 vector to be added
   // */
   // public void scaledAdd (double s, Vector4d v1)
   // {
   // x = s*x + v1.x;
   // y = s*y + v1.y;
   // z = s*z + v1.z;
   // w = s*w + v1.w;
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
   public Vector4d scaledAdd (double s, Vector4d v1) {
      x += s * v1.x;
      y += s * v1.y;
      z += s * v1.z;
      w += s * v1.w;
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
   public Vector4d scaledAdd (double s, Vector4d v1, Vector4d v2) {
      x = s * v1.x + v2.x;
      y = s * v1.y + v2.y;
      z = s * v1.z + v2.z;
      w = s * v1.w + v2.w;
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
   public Vector4d combine (double s1, Vector4d v1, double s2, Vector4d v2) {
      x = s1 * v1.x + s2 * v2.x;
      y = s1 * v1.y + s2 * v2.y;
      z = s1 * v1.z + s2 * v2.z;
      w = s1 * v1.w + s2 * v2.w;
      return this;
   }

   /**
    * Returns the 2 norm of this vector. This is the square root of the sum of
    * the squares of the elements.
    * 
    * @return vector 2 norm
    */
   public double length() {
      return Math.sqrt (x * x + y * y + z * z + w * w);
   }

   /**
    * Returns the square of the 2 norm of this vector. This is the sum of the
    * squares of the elements.
    * 
    * @return square of the 2 norm
    */
   public double lengthSquared() {
      return x * x + y * y + z * z + w * w;
   }

   /**
    * Returns the Euclidean distance between this vector and vector v.
    * 
    * @return distance between this vector and v
    */
   public double distance (Vector4d v) {
      double dx = x - v.x;
      double dy = y - v.y;
      double dz = z - v.z;
      double dw = w - v.w;

      return Math.sqrt (dx * dx + dy * dy + dz * dz + dw * dw);
   }

   /**
    * Returns the squared of the Euclidean distance between this vector and
    * vector v.
    * 
    * @return squared distance between this vector and v
    */
   public double distanceSquared (Vector4d v) {
      double dx = x - v.x;
      double dy = y - v.y;
      double dz = z - v.z;
      double dw = w - v.w;

      return (dx * dx + dy * dy + dz * dz + dw * dw);
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
      if (z > max) {
         max = z;
      }
      if (w > max) {
         max = w;
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
      if (z < min) {
         min = z;
      }
      if (w < min) {
         min = w;
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
      if (Math.abs (z) > max) {
         max = Math.abs (z);
      }
      if (Math.abs (w) > max) {
         max = Math.abs (w);
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
      return Math.abs (x) + Math.abs (y) + Math.abs (z) + Math.abs (w);
   }

   /**
    * Returns the dot product of this vector and v1.
    * 
    * @param v1
    * right-hand vector
    * @return dot product
    */
   public double dot (Vector4d v1) {
      return x * v1.x + y * v1.y + z * v1.z + w * v1.w;
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
   public double angle (Vector4d v1) {
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
   public Vector4d normalize() {
      double lenSqr = x * x + y * y + z * z + w * w;
      double err = lenSqr - 1;
      if (err > (2 * DOUBLE_PREC) || err < -(2 * DOUBLE_PREC)) {
         double len = Math.sqrt (lenSqr);
         x /= len;
         y /= len;
         z /= len;
         w /= len;
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
   public Vector4d normalize (Vector4d v1) {
      double lenSqr = v1.x * v1.x + v1.y * v1.y + v1.z * v1.z + v1.w * v1.w;
      double err = lenSqr - 1;
      if (err > (2 * DOUBLE_PREC) || err < -(2 * DOUBLE_PREC)) {
         double len = Math.sqrt (lenSqr);
         x = v1.x / len;
         y = v1.y / len;
         z = v1.z / len;
         w = v1.w / len;
      }
      else {
         x = v1.x;
         y = v1.y;
         z = v1.z;
         w = v1.w;
      }
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
   public boolean epsilonEquals (Vector4d v1, double eps) {
      return (abs (x - v1.x) <= eps &&
              abs (y - v1.y) <= eps &&
              abs (z - v1.z) <= eps &&
              abs (w - v1.w) <= eps);
   }

   /**
    * Returns true if the elements of this vector exactly equal those of vector
    * <code>v1</code>.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal
    */
   public boolean equals (Vector4d v1) {
      return (x == v1.x && y == v1.y && z == v1.z && w == v1.w);
   }

   /**
    * Sets the elements of this vector to zero.
    */
   public void setZero() {
      x = 0;
      y = 0;
      z = 0;
      w = 0;
   }

   /**
    * Sets the elements of this vector to the prescribed values.
    * 
    * @param x
    * value for first element
    * @param y
    * value for second element
    * @param z
    * value for third element
    * @param w
    * value for fourth element
    */
   public void set (double x, double y, double z, double w) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
   }

   /**
    * Sets the elements of this vector to the prescribed values.
    * 
    * @param v
    * vector giving the first three element values
    * @param w
    * value for the fourth element
    */
   public void set (Vector3d v, double w) {
      this.x = v.x;
      this.y = v.y;
      this.z = v.z;
      this.w = w;
   }

   /**
    * Sets the values of this vector to a homogeneous representation formed from
    * a 3-vector and a weighting factor w. The weighting factor scales the x, y,
    * and z elements of v1 and sets the w field of this vector.
    * 
    * @param v1
    * 3-vector
    * @param w
    * weighting factor
    */
   public void setToHomogeneous (Vector3d v1, double w) {
      this.x = w * v1.x;
      this.y = w * v1.y;
      this.z = w * v1.z;
      this.w = w;
   }

   /**
    * Sets the elements of this vector to their absolute values.
    * @return this vector
    */
   public Vector4d absolute() {
      x = Math.abs (x);
      y = Math.abs (y);
      z = Math.abs (z);
      w = Math.abs (w);
      return this;
   }

   /**
    * Sets the elements of this vector to the absolute value of v1.
    * 
    * @param v1
    * vector to take the absolute value of
    * @return this vector
    */
   public Vector4d absolute (Vector4d v1) {
      x = Math.abs (v1.x);
      y = Math.abs (v1.y);
      z = Math.abs (v1.z);
      w = Math.abs (v1.w);
      return this;
   }

   /**
    * Sorts the contents of the vector by the absolute value of its components.
    */
   public void sortAbsolute() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);
      double absz = (z < 0 ? -z : z);
      double absw = (w < 0 ? -w : w);
      double tmp;

      if (absx < absy) {
         tmp = x;
         x = y;
         y = tmp;
         tmp = absx;
         absx = absy;
         absy = tmp;
      }
      if (absx < absz) {
         tmp = x;
         x = z;
         z = tmp;
         tmp = absx;
         absx = absz;
         absz = tmp;
      }
      if (absx < absw) {
         tmp = x;
         x = w;
         w = tmp;
         tmp = absx;
         absx = absw;
         absw = tmp;
      }
      if (absy < absz) {
         tmp = y;
         y = z;
         z = tmp;
         tmp = absy;
         absy = absz;
         absz = tmp;
      }
      if (absy < absw) {
         tmp = y;
         y = w;
         w = tmp;
         tmp = absy;
         absy = absw;
         absw = tmp;
      }
      if (absz < absw) {
         tmp = z;
         z = w;
         w = tmp;
         tmp = absz;
         absz = absw;
         absw = tmp;
      }
   }

   /**
    * Returns the index (0, 1, 2, or 3) of the element of v with the largest
    * absolute value.
    * 
    * @return index
    */
   public int maxAbsIndex() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);
      double absz = (z < 0 ? -z : z);
      double absw = (w < 0 ? -w : w);

      if (absx >= absy) {
         if (absx >= absz) {
            return (absx >= absw) ? 0 : 3;
         }
         else {
            return (absz >= absw) ? 2 : 3;
         }
      }
      else {
         if (absy >= absz) {
            return (absy >= absw) ? 1 : 3;
         }
         else {
            return (absz >= absw) ? 2 : 3;
         }
      }
   }

   /**
    * Returns the index (0, 1, 2, or 3) of the element of v with the smallest
    * absolute value.
    * 
    * @return index
    */
   public int minAbsIndex() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);
      double absz = (z < 0 ? -z : z);
      double absw = (w < 0 ? -w : w);

      if (absx <= absy) {
         if (absx <= absz) {
            return (absx <= absw) ? 0 : 3;
         }
         else {
            return (absz <= absw) ? 2 : 3;
         }
      }
      else {
         if (absy <= absz) {
            return (absy <= absw) ? 1 : 3;
         }
         else {
            return (absz <= absw) ? 2 : 3;
         }
      }
   }

   /**
    * Sorts the contents of vector v1 by element value, with x being set to the
    * largest value and w being set to the smallest value, and places the
    * results in this vector.
    * 
    * @param v1
    * vector to sort
    */
   public void sort (Vector4d v1) {
      set (v1);
      sort();
   }

   /**
    * Sorts the contents of this vector by element value, with x being set to
    * the largest value and w being set to the smallest value.
    */
   public void sort() {
      double tmp;

      if (x < y) {
         tmp = x;
         x = y;
         y = tmp;
      }
      if (x < z) {
         tmp = x;
         x = z;
         z = tmp;
      }
      if (x < w) {
         tmp = x;
         x = w;
         w = tmp;
      }
      if (y < z) {
         tmp = y;
         y = z;
         z = tmp;
      }
      if (y < w) {
         tmp = y;
         y = w;
         w = tmp;
      }
      if (z < w) {
         tmp = z;
         z = w;
         w = tmp;
      }
   }

   /**
    * Multiplies matrix X by vector v1 and places the result in this vector.
    * 
    * @param X
    * matrix
    * @param v1
    * vector
    * @return this vector
    */
   public Vector4d mul (Matrix4dBase X, Vector4d v1) {
      X.mul (this, v1);
      return this;
   }
   
   

   private void mulMat (Vector4d vr, Matrix3dBase R, Vector4d v1) {
      double x = R.m00 * v1.x + R.m01 * v1.y + R.m02 * v1.z;
      double y = R.m10 * v1.x + R.m11 * v1.y + R.m12 * v1.z;
      double z = R.m20 * v1.x + R.m21 * v1.y + R.m22 * v1.z;
      vr.x = x;
      vr.y = y;
      vr.z = z;
      vr.w = v1.w;
   }

   private void mulMatTranspose (Vector4d vr, Matrix3dBase R, Vector4d v1) {
      double x = R.m00 * v1.x + R.m10 * v1.y + R.m20 * v1.z;
      double y = R.m01 * v1.x + R.m11 * v1.y + R.m21 * v1.z;
      double z = R.m02 * v1.x + R.m12 * v1.y + R.m22 * v1.z;
      vr.x = x;
      vr.y = y;
      vr.z = z;
      vr.w = v1.w;
   }

   public void transform (RotationMatrix3d R) {
      mulMat (this, R, this);
   }

   public void transform (RotationMatrix3d R, Vector4d v1) {
      mulMat (this, R, v1);
   }

   public void inverseTransform (RotationMatrix3d R) {
      mulMatTranspose (this, R, this);
   }

   public void inverseTransform (RotationMatrix3d R, Vector4d v1) {
      mulMatTranspose (this, R, v1);
   }

   // public void transform (RigidTransform3d X) {
   //    transform (X, this);
   // }

   // public void transform (RigidTransform3d X, Vector4d v1) {
   //    double ww = v1.w;
   //    mulMat (this, X.R, v1);
   //    x = v1.x + ww * X.p.x;
   //    y = v1.y + ww * X.p.y;
   //    z = v1.z + ww * X.p.z;
   //    w = ww;
   // }

   public void inverseTransform (RigidTransform3d X) {
      inverseTransform (X, this);
   }

   public void inverseTransform (RigidTransform3d X, Vector4d v1) {
      double ww = v1.w;
      x = v1.x - ww * X.p.x;
      y = v1.y - ww * X.p.y;
      z = v1.z - ww * X.p.z;
      mulMatTranspose (this, X.R, this);
      w = ww;
   }

   public void transform (AffineTransform3dBase X) {
      transform (X, this);
   }

   public void transform (AffineTransform3dBase X, Vector4d v1) {
      double ww = v1.w;
      mulMat (this, X.getMatrix(), v1);
      Vector3d b = X.getOffset();
      x = v1.x + ww * b.x;
      y = v1.y + ww * b.y;
      z = v1.z + ww * b.z;
      w = ww;
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
   public void updateBounds (Vector4d vmin, Vector4d vmax) {
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
      if (z > vmax.z) {
         vmax.z = z;
      }
      if (z < vmin.z) {
         vmin.z = z;
      }
      if (w > vmax.w) {
         vmax.w = w;
      }
      if (w < vmin.w) {
         vmin.w = w;
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
   public Vector4d max (Vector4d v) {
      if (v.x > x) {
         x = v.x;
      }
      if (v.y > y) {
         y = v.y;
      }
      if (v.z > z) {
         z = v.z;
      }
      if (v.w > w) {
         w = v.w;
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
   public Vector4d min (Vector4d v) {
      if (v.x < x) {
         x = v.x;
      }
      if (v.y < y) {
         y = v.y;
      }
      if (v.z < z) {
         z = v.z;
      }
      if (v.w < w) {
         w = v.w;
      }
      return this;
   }

   public Vector4d clone() {
      return (Vector4d)super.clone();
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
   public void addObj (Vector4d v1) {
      add (v1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Vector4d v1) {
      scaledAdd (s, v1);
   }
}
