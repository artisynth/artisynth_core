/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC), Fabien Pean (ETHZ)
 * (method reference returns)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import maspack.util.Clonable;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a 3 element vector, along with its most commonly used operations.
 * 
 * <p>
 * The size of these vectors is fixed.
 */
public class Vector3d extends VectorBase
   implements java.io.Serializable, Clonable, VectorObject<Vector3d> {
   
   private static final long serialVersionUID = 1L;

   private static double DOUBLE_PREC = 2.220446049250313e-16;

   /**
    * Global zero vector. Should not be modified.
    */
   public static final Vector3d ZERO = new Vector3d();

   /**
    * Global unit vector along the x axis. Should not be modified.
    */
   public static final Vector3d X_UNIT = new Vector3d (1, 0, 0);

   /**
    * Global unit vector along the y axis. Should not be modified.
    */
   public static final Vector3d Y_UNIT = new Vector3d (0, 1, 0);

   /**
    * Global unit vector along the z axis. Should not be modified.
    */
   public static final Vector3d Z_UNIT = new Vector3d (0, 0, 1);

   /**
    * Global unit vector along the negative x axis. Should not be modified.
    */
   public static final Vector3d NEG_X_UNIT = new Vector3d (-1, 0, 0);

   /**
    * Global unit vector along the negative y axis. Should not be modified.
    */
   public static final Vector3d NEG_Y_UNIT = new Vector3d (0, -1, 0);

   /**
    * Global unit vector along the negative z axis. Should not be modified.
    */
   public static final Vector3d NEG_Z_UNIT = new Vector3d (0, 0, -1);

   /**
    * Global vector containing ones. Should not be modified.
    */
   public static final Vector3d ONES = new Vector3d (1, 1, 1);

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
    * Creates a 3-vector and initializes its elements to 0.
    */
   public Vector3d() {
   }

   /**
    * Creates a 3-vector by copying an existing one.
    * 
    * @param v
    * vector to be copied
    */
   public Vector3d (Vector3d v) {
      set (v);
   }

   /**
    * Creates a 3-vector for an integer 3 vector.
    * 
    * @param v
    * vector to be copied
    */
   public Vector3d (Vector3i v) {
      set (v);
   }

   /**
    * Creates a 3-vector by copying an existing Vector. The
    * size of the copied vector must be at least 3.
    * 
    * @param v
    * vector to be copied
    */
   public Vector3d (Vector v) {
      if (v.size() < 3) {
         throw new IllegalArgumentException (
            "v must have a size of at least 3");
      }
      set (v);
   }

   /**
    * Creates a 3-vector with the supplied element values.
    * 
    * @param x
    * first element
    * @param y
    * second element
    * @param z
    * third element
    */
   public Vector3d (double x, double y, double z) {
      set (x, y, z);
   }

   /**
    * Creates a 3-vector with the supplied element values.
    * 
    * @param values
    * element values
    */
   public Vector3d (double[] values) {
      set (values[0], values[1], values[2]);
   }

   /**
    * Returns the size of this vector (which is always 3)
    * 
    * @return 3
    */
   public int size() {
      return 3;
   }

   /**
    * Gets a single element of this vector. Elements 0, 1, and 2 correspond to
    * x, y, and z.
    * 
    * @param i
    * element index
    * @return element value throws ArrayIndexOutOfBoundsException if i is not in
    * the range 0 to 2.
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
         default: {
            throw new ArrayIndexOutOfBoundsException (i);
         }
      }
   }

   /**
    * {@inheritDoc}
    */  
   public void get (double[] values) {
      if (values.length < 3) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= 3");
      }
      values[0] = x;
      values[1] = y;
      values[2] = z;
   }

   /**
    * Copies the elements of this vector into an array of floats.
    * 
    * @param values
    * array into which values are copied
    */
   public void get (float[] values) {
      values[0] = (float)x;
      values[1] = (float)y;
      values[2] = (float)z;
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
      if (idx + 2 >= v1.size) {
         throw new ArrayIndexOutOfBoundsException();
      }
      v1.buf[idx + 0] = x;
      v1.buf[idx + 1] = y;
      v1.buf[idx + 2] = z;
   }

   /**
    * Copies the elements of this vector into a Matrix3x1.
    * 
    * @param M
    * matrix into which values are copied
    */
   public void get (Matrix3x1 M) {
      M.m00 = x;
      M.m10 = y;
      M.m20 = z;
   }

   /**
    * Sets a single element of this vector. Elements 0, 1, and 2 correspond to
    * x, y, and z.
    * 
    * @param i
    * element index
    * @param value
    * element value
    * @throws ArrayIndexOutOfBoundsException
    * if i is not in the range 0 to 2.
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
         default: {
            throw new ArrayIndexOutOfBoundsException (i);
         }
      }
   }

   /**
    * Sets the elements of this vector from an array of doubles. The array
    * must have a length of at least 3.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (double[] values) {
      if (values.length < 3) {
         throw new IllegalArgumentException (
            "argument 'values' must have a length of at least 3");
      } 
      x = values[0];
      y = values[1];
      z = values[2];
   }

   /**
    * {@inheritDoc}
    */
   public int set (double[] values, int idx) {
      x = values[idx++];
      y = values[idx++];
      z = values[idx++];
      return idx;
   }

   /**
    * Sets the values of this vector to those of v1.
    * 
    * @param v1
    * vector whose values are copied
    */
   public void set (Vector3d v1) {
      x = v1.x;
      y = v1.y;
      z = v1.z;
   }

   /**
    * Sets the values of this vector to those of v1.
    * 
    * @param v1
    * vector whose values are copied
    */
   public void set (Vector3i v1) {
      x = v1.x;
      y = v1.y;
      z = v1.z;
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
      if (idx + 2 >= v1.size) {
         throw new ArrayIndexOutOfBoundsException();
      }
      x = v1.buf[idx + 0];
      y = v1.buf[idx + 1];
      z = v1.buf[idx + 2];
   }

   /**
    * Sets the elements of this vector from a Matrix3x1.
    * 
    * @param M
    * matrix from which values are taken
    */
   public void set (Matrix3x1 M) {
      x = M.m00;
      y = M.m10;
      z = M.m20;
   }

   /**
    * Sets the values of this vector from a homogeneous representation stored in
    * the 4-vector v1. This involves dividing the x, y, and z elements of v1 by
    * its w element.
    * 
    * @param v1
    * homogenous vector
    */
   public void setFromHomogeneous (Vector4d v1) {
      x = v1.x / v1.w;
      y = v1.y / v1.w;
      z = v1.z / v1.w;
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
   public Vector3d add (Vector3d v1, Vector3d v2) {
      x = v1.x + v2.x;
      y = v1.y + v2.y;
      z = v1.z + v2.z;
      return this;
   }

   /**
    * Adds this vector to v1 and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    */
   public Vector3d add (Vector3d v1) {
      x += v1.x;
      y += v1.y;
      z += v1.z;
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
    * @return this vector
    */
   public Vector3d add (double dx, double dy, double dz) {
      x += dx;
      y += dy;
      z += dz;
      return this;
   }

   /**
    * Subtracts vector v2 from v1 and places the result in this vector.
    * 
    * @param v1
    * left-hand vector
    * @param v2
    * right-hand vector
    * @return this vector
    */
   public Vector3d sub (Vector3d v1, Vector3d v2) {
      x = v1.x - v2.x;
      y = v1.y - v2.y;
      z = v1.z - v2.z;
      return this;
   }

   /**
    * Subtracts v1 from this vector and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    */
   public Vector3d sub (Vector3d v1) {
      x -= v1.x;
      y -= v1.y;
      z -= v1.z;
      return this;
   }

   /**
    * Sets this vector to the negative of v1.
    * 
    * @param v1
    * vector to negate
    * @return this vector
    */
   public Vector3d negate (Vector3d v1) {
      x = -v1.x;
      y = -v1.y;
      z = -v1.z;
      return this;
   }

   /**
    * Negates this vector in place.
    * @return this vector
    */
   public Vector3d negate() {
      x = -x;
      y = -y;
      z = -z;
      return this;
   }

   /**
    * Scales the elements of this vector by <code>s</code>.
    * 
    * @param s
    * scaling factor
    * @return this vector
    */
   public Vector3d scale (double s) {
      x *= s;
      y *= s;
      z *= s;
      return this;
   }
   
   /**
    * Scales the elements of this vector by the values in the given direction.
    * 
    * @param sx
    * scaling factor in the x direction
    * @param sy
    * scaling factor in the y direction
    * @param sz
    * scaling factor in the z direction
    * @return this vector
    */
   public Vector3d scale (double sx, double sy, double sz) {
      x *= sx;
      y *= sy;
      z *= sz;
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
   public Vector3d scale (double s, Vector3d v1) {
      x = s * v1.x;
      y = s * v1.y;
      z = s * v1.z;
      return this;
   }

//   /**
//    * Divides the elements of vector v1 by <code>s</code> and places the
//    * results in this vector.
//    * 
//    * @param s
//    * factor to divide by
//    * @param v1
//    * vector to be divided
//    */
//   void divide (double s, Vector3d v1) {
//      x = v1.x / s;
//      y = v1.y / s;
//      z = v1.z / s;
//   }

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
   public void interpolate (Vector3d v1, double s, Vector3d v2) {
      x = (1 - s) * v1.x + s * v2.x;
      y = (1 - s) * v1.y + s * v2.y;
      z = (1 - s) * v1.z + s * v2.z;
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
   public void interpolate (double s, Vector3d v1) {
      x = (1 - s) * x + s * v1.x;
      y = (1 - s) * y + s * v1.y;
      z = (1 - s) * z + s * v1.z;
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
   public Vector3d scaledAdd (double s, Vector3d v1, Vector3d v2) {
      x = s * v1.x + v2.x;
      y = s * v1.y + v2.y;
      z = s * v1.z + v2.z;
      return this;
   }

   /**
    * Computes <code>s v1</code> and adds the result to this vector.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled and added
    * @return this vector
    */
   public Vector3d scaledAdd (double s, Vector3d v1) {
      x += s * v1.x;
      y += s * v1.y;
      z += s * v1.z;
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
   public Vector3d combine (double s1, Vector3d v1, double s2, Vector3d v2) {
      x = s1 * v1.x + s2 * v2.x;
      y = s1 * v1.y + s2 * v2.y;
      z = s1 * v1.z + s2 * v2.z;
      return this;
   }

   /**
    * Returns the 2 norm of this vector. This is the square root of the sum of
    * the squares of the elements.
    * 
    * @return vector 2 norm
    */
   public double norm() {
      return Math.sqrt (x * x + y * y + z * z);
   }

   /**
    * Returns the square of the 2 norm of this vector. This is the sum of the
    * squares of the elements.
    * 
    * @return square of the 2 norm
    */
   public double normSquared() {
      return x * x + y * y + z * z;
   }

   /**
    * Returns the Euclidean distance between this vector and vector v.
    * 
    * @return distance between this vector and v
    */
   public double distance (Vector3d v) {
      double dx = x - v.x;
      double dy = y - v.y;
      double dz = z - v.z;

      return Math.sqrt (dx * dx + dy * dy + dz * dz);
   }

   /**
    * Returns the squared of the Euclidean distance between this vector and
    * vector v.
    * 
    * @return squared distance between this vector and v
    */
   public double distanceSquared (Vector3d v) {
      double dx = x - v.x;
      double dy = y - v.y;
      double dz = z - v.z;

      return (dx * dx + dy * dy + dz * dz);
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
      return max;
   }

   /**
    * Returns the 1 norm of this vector. This is the sum of the absolute values
    * of the elements.
    * 
    * @return vector 1 norm
    */
   public double oneNorm() {
      return Math.abs (x) + Math.abs (y) + Math.abs (z);
   }

   /**
    * Returns the dot product of this vector and v1.
    * 
    * @param v1
    * right-hand vector
    * @return dot product
    */
   public double dot (Vector3d v1) {
      return x * v1.x + y * v1.y + z * v1.z;
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
   public double angle (Vector3d v1) {
      double cos = dot (v1) / (norm() * v1.norm());
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
   public Vector3d normalize() {
      double lenSqr = x * x + y * y + z * z;
      if (lenSqr == 0) {
         return this;
      }
      double err = lenSqr - 1;
      if (err > (2 * DOUBLE_PREC) || err < -(2 * DOUBLE_PREC)) {
         double len = Math.sqrt (lenSqr);
         x /= len;
         y /= len;
         z /= len;
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
   public Vector3d normalize (Vector3d v1) {
      double lenSqr = v1.x * v1.x + v1.y * v1.y + v1.z * v1.z;
      double err = lenSqr - 1;
      if (err > (2 * DOUBLE_PREC) || err < -(2 * DOUBLE_PREC)) {
         double len = Math.sqrt (lenSqr);
         x = v1.x / len;
         y = v1.y / len;
         z = v1.z / len;
      }
      else {
         x = v1.x;
         y = v1.y;
         z = v1.z;
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
   public Vector3d perpendicular (Vector3d v1) {
      double absx = (v1.x >= 0 ? v1.x : -v1.x);
      double absy = (v1.y >= 0 ? v1.y : -v1.y);
      double absz = (v1.z >= 0 ? v1.z : -v1.z);

      if (absx <= absy) {
         if (absx <= absz) {
            x = 0; // smallest component is x
            y = -v1.z;
            z = v1.y;
         }
         else // smallest component is z
         {
            x = -v1.y;
            y = v1.x;
            z = 0;
         }
      }
      else {
         if (absy <= absz) {
            x = v1.z; // smallest component is y
            y = 0;
            z = -v1.x;
         }
         else // smallest component is z
         {
            x = -v1.y;
            y = v1.x;
            z = 0;
         }
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
   public boolean epsilonEquals (Vector3d v1, double eps) {
      return (abs (x - v1.x) <= eps &&
              abs (y - v1.y) <= eps &&
              abs (z - v1.z) <= eps);
   }

   /**
    * Returns true if the elements of this vector exactly equal those of vector
    * <code>v1</code>.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal
    */
   public boolean equals (Vector3d v1) {
      return (x == v1.x && y == v1.y && z == v1.z);
   }

   /**
    * Returns true if all the elements of this vector are greater than those of
    * vector<code>v1</code>.
    * 
    * @param v1 vector to compare with
    * @return true if all elements of this vector are greater
    */
   public boolean greater (Vector3d v1) {
      return (x > v1.x && y > v1.y && z > v1.z);
   }

   /**
    * Returns true if all the elements of this vector are greater than or equal
    * to those of vector<code>v1</code>.
    * 
    * @param v1 vector to compare with
    * @return true if all elements of this vector are greater or equal
    */
   public boolean greaterEquals (Vector3d v1) {
      return (x >= v1.x && y >= v1.y && z >= v1.z);
   }

   /**
    * Sets the elements of this vector to zero.
    */
   public void setZero() {
      x = 0;
      y = 0;
      z = 0;
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
    */
   public void set (double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   /**
    * Sets the elements of this vector to their absolute values.
    * @return this vector
    */
   public Vector3d absolute() {
      x = Math.abs (x);
      y = Math.abs (y);
      z = Math.abs (z);
      return this;
   }

   /**
    * Sets the elements of this vector to the absolute value of v1.
    * 
    * @param v1
    * vector to take the absolute value of
    * @return this vector
    */
   public Vector3d absolute (Vector3d v1) {
      x = Math.abs (v1.x);
      y = Math.abs (v1.y);
      z = Math.abs (v1.z);
      return this;
   }

   /**
    * Sorts the contents of this vector by absolute element value, with x being
    * set to the largest value and z being set to the smallest value.
    */
   public void sortAbsolute() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);
      double absz = (z < 0 ? -z : z);
      double tmp;

      if (absx >= absy) {
         if (absy >= absz) { // output x, y, z
            // nothing to do
         }
         else if (absx >= absz) { // output x, z, y
            tmp = y;
            y = z;
            z = tmp;
         }
         else { // ouput z, x, y
            tmp = x;
            x = z;
            z = y;
            y = tmp;
         }
      }
      else {
         if (absx >= absz) { // output y, x, z
            tmp = x;
            x = y;
            y = tmp;
         }
         else if (absy >= absz) { // output y, z, x
            tmp = x;
            x = y;
            y = z;
            z = tmp;
         }
         else { // output z, y, x
            tmp = x;
            x = z;
            z = tmp;
         }
      }
   }

   /**
    * Returns the index (0, 1, or 2) of the element of this vector with the
    * largest absolute value.
    * 
    * @return index of largest absolute value
    */
   public int maxAbsIndex() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);
      double absz = (z < 0 ? -z : z);

      if (absx >= absy) {
         return (absx >= absz) ? 0 : 2;
      }
      else {
         return (absy >= absz) ? 1 : 2;
      }
   }

   /**
    * Returns the index (0, 1, or 2) of the element of this vector with the
    * smallest absolute value.
    * 
    * @return index of smallest absolute value
    */
   public int minAbsIndex() {
      double absx = (x < 0 ? -x : x);
      double absy = (y < 0 ? -y : y);
      double absz = (z < 0 ? -z : z);

      if (absx <= absy) {
         return (absx <= absz) ? 0 : 2;
      }
      else {
         return (absy <= absz) ? 1 : 2;
      }
   }

   /**
    * Sorts the contents of vector v1 by element value, with x being set to the
    * largest value and z being set to the smallest value, and places the
    * results in this vector.
    * 
    * @param v1
    * vector to sort
    */
   public void sort (Vector3d v1) {
      set (v1);
      sort();
   }

   /**
    * Sorts the contents of this vector by element value, with x being set to
    * the largest value and z being set to the smallest value.
    */
   public void sort() {
      double tmp;

      if (x >= y) {
         if (y >= z) { // output x, y, z
            // nothing to do
         }
         else if (x >= z) { // output x, z, y
            tmp = y;
            y = z;
            z = tmp;
         }
         else { // ouput z, x, y
            tmp = x;
            x = z;
            z = y;
            y = tmp;
         }
      }
      else {
         if (x >= z) { // output y, x, z
            tmp = x;
            x = y;
            y = tmp;
         }
         else if (y >= z) { // output y, z, x
            tmp = x;
            x = y;
            y = z;
            z = tmp;
         }
         else { // output z, y, x
            tmp = x;
            x = z;
            z = tmp;
         }
      }
   }

   /**
    * Computes the cross product of v1 and v2 and places the result in this
    * vector.
    * 
    * @param v1
    * left-hand vector
    * @param v2
    * right-hand vector
    * @return this vector
    */
   public Vector3d cross (Vector3d v1, Vector3d v2) {
      double tmpx = v1.y * v2.z - v1.z * v2.y;
      double tmpy = v1.z * v2.x - v1.x * v2.z;
      double tmpz = v1.x * v2.y - v1.y * v2.x;

      x = tmpx;
      y = tmpy;
      z = tmpz;
      return this;
   }

   /**
    * Computes the cross product of this vector and v1, and places the result in
    * this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    */
   public Vector3d cross (Vector3d v1) {
      double tmpx = y * v1.z - z * v1.y;
      double tmpy = z * v1.x - x * v1.z;
      double tmpz = x * v1.y - y * v1.x;

      x = tmpx;
      y = tmpy;
      z = tmpz;
      return this;
   }

   /**
    * Computes the cross product of v1 and v2, adds this to v3, and places the
    * result in this vector.
    * 
    * @param v1
    * left-hand vector for cross product
    * @param v2
    * right-hand vector for cross product
    * @param v3
    * vector to add
    * @return this vector
    */
   public Vector3d crossAdd (Vector3d v1, Vector3d v2, Vector3d v3) {
      double tmpx = v1.y * v2.z - v1.z * v2.y;
      double tmpy = v1.z * v2.x - v1.x * v2.z;
      double tmpz = v1.x * v2.y - v1.y * v2.x;

      x = tmpx + v3.x;
      y = tmpy + v3.y;
      z = tmpz + v3.z;
      return this;
   }

   /**
    * Computes the cross product v2 x v1, where v2 = p1 - p2 and v1 = p1 - p0,
    * and then sets the length of the result to the angle between v1 and
    * v2. The result is returned in this vector.
    *
    * @param p0
    * tail point for vector v1
    * @param p1
    * head point for vectors v1 and v2
    * @param p2
    * tail point for vector v2
    */
   public void angleWeightedCross (Vector3d p0, Vector3d p1, Vector3d p2) {

      double u1x = p1.x-p0.x;
      double u1y = p1.y-p0.y;
      double u1z = p1.z-p0.z;
      double mag1 = Math.sqrt (u1x*u1x + u1y*u1y + u1z*u1z);
      if (mag1 == 0) {
         setZero();
         return;
      }
      u1x /= mag1;
      u1y /= mag1;
      u1z /= mag1;

      double u2x = p1.x-p2.x;
      double u2y = p1.y-p2.y;
      double u2z = p1.z-p2.z;
      double mag2 = Math.sqrt (u2x*u2x + u2y*u2y + u2z*u2z);
      if (mag2 == 0) {
         setZero();
         return;
      }
      u2x /= mag2;
      u2y /= mag2;
      u2z /= mag2;

      double x = u2y*u1z - u2z*u1y;
      double y = u2z*u1x - u2x*u1z;
      double z = u2x*u1y - u2y*u1x;

      double sin = Math.sqrt (x*x + y*y + z*z);
      double cos = (u1x*u2x + u1y*u2y + u1z*u2z);

      if (sin == 0) {
         setZero();
         return;
      }
      double w = Math.atan2 (sin, cos)/sin;
      this.x = w*x;
      this.y = w*y;
      this.z = w*z;
   }

   /**
    * Computes the cross product v2 x v1, where v2 = p1 - p2 and v1 = p1 - p0,
    * and then sets the length of the result to the angle between v1 and
    * v2. The result is added to this vector.
    *
    * @param p0
    * tail point for vector v1
    * @param p1
    * head point for vectors v1 and v2
    * @param p2
    * tail point for vector v2
    */
   public void angleWeightedCrossAdd (Vector3d p0, Vector3d p1, Vector3d p2) {

      double u1x = p1.x-p0.x;
      double u1y = p1.y-p0.y;
      double u1z = p1.z-p0.z;
      double mag1 = Math.sqrt (u1x*u1x + u1y*u1y + u1z*u1z);
      if (mag1 == 0) {
         return;
      }
      u1x /= mag1;
      u1y /= mag1;
      u1z /= mag1;

      double u2x = p1.x-p2.x;
      double u2y = p1.y-p2.y;
      double u2z = p1.z-p2.z;
      double mag2 = Math.sqrt (u2x*u2x + u2y*u2y + u2z*u2z);
      if (mag2 == 0) {
         return;
      }
      u2x /= mag2;
      u2y /= mag2;
      u2z /= mag2;

      double x = u2y*u1z - u2z*u1y;
      double y = u2z*u1x - u2x*u1z;
      double z = u2x*u1y - u2y*u1x;

      double sin = Math.sqrt (x*x + y*y + z*z);
      double cos = (u1x*u2x + u1y*u2y + u1z*u2z);

      if (sin == 0) {
         return;
      }
      double w = Math.atan2 (sin, cos)/sin;
      this.x += w*x;
      this.y += w*y;
      this.z += w*z;
   }

   /**
    * Returns the area of the triangle whose vertices are formed by this vector,
    * and the vectors v1 and v2.
    * 
    * @param v1
    * second vertex
    * @param v2
    * third vertex
    * @return area of the triangle
    */
   public double triangleArea (Vector3d v1, Vector3d v2) {
      double d1x = v1.x - x;
      double d1y = v1.y - y;
      double d1z = v1.z - z;

      double d2x = v2.x - x;
      double d2y = v2.y - y;
      double d2z = v2.z - z;

      double xx = d1y * d2z - d1z * d2y;
      double yy = d1z * d2x - d1x * d2z;
      double zz = d1x * d2y - d1y * d2x;

      return Math.sqrt (xx * xx + yy * yy + zz * zz) / 2;
   }

   /**
    * Applies a rotational transformation to this vector, in place. This is
    * equivalent to multiplying the rotation matrix by this vector.
    * 
    * @param R
    * rotational transformation matrix
    */
   public void transform (RotationMatrix3d R) {
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
   public void transform (RotationMatrix3d R, Vector3d v1) {
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
   public void inverseTransform (RotationMatrix3d R) {
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
   public void inverseTransform (RotationMatrix3d R, Vector3d v1) {
      R.mulTranspose (this, v1);
   }

   /**
    * Applies a affine transformation to this vector, in place.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform3dBase X) {
      X.M.mul (this);
   }

   /**
    * Applies an affine transformation to the vector v1, and places the result
    * in this vector.
    * 
    * @param X
    * affine transformation
    * @param v1
    * vector to be transformed
    */
   public void transform (AffineTransform3dBase X, Vector3d v1) {
      X.M.mul (this, v1);
   }

   /**
    * Applies an inverse affine transformation to this vector, in place.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform3dBase X) {
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
   public void inverseTransform (AffineTransform3dBase X, Vector3d v1) {
      X.M.mulInverse (this, v1);
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
   public Vector3d mul (Matrix3dBase X, Vector3d v1) {
      X.mul (this, v1);
      return this;
   }

   /**
    * Multiplies matrix X by vector v1, adds vector v2, and places the result in
    * this vector.
    * 
    * @param X
    * matrix
    * @param v1
    * vector to multiply
    * @param v2
    * vector to add
    * @return this vector
    */
   public Vector3d mulAdd (Matrix3dBase X, Vector3d v1, Vector3d v2) {
      X.mulAdd (this, v1, v2);
      return this;
   }

   /**
    * Multiplies the transpose of matrix X by vector v1 and places the result in
    * this vector.
    * 
    * @param X
    * matrix
    * @param v1
    * vector to multiply
    * @return this vector
    */
   public Vector3d mulTranspose (Matrix3dBase X, Vector3d v1) {
      X.mulTranspose (this, v1);
      return this;
   }

   /**
    * Multiplies the transpose of matrix X by vector v1, adds vector v2, and
    * places the result in this vector.
    * 
    * @param X
    * matrix
    * @param v1
    * vector to multiply
    * @param v2
    * vector to add
    * @return this vector
    */
   public Vector3d mulTransposeAdd (Matrix3dBase X, Vector3d v1, Vector3d v2) {
      X.mulTransposeAdd (this, v1, v2);
      return this;
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
    * minimum and maximum corner points, which are changed if necessary.  This
    * method will only grow bounds, not shrink them.
    * 
    * @param vmin
    * minimum corner of the bounding box
    * @param vmax
    * maximum corner of the bounding box
    */
   public void updateBounds (Vector3d vmin, Vector3d vmax) {
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
   }

   /**
    * Computes the element-wise maximum of this vector and vector v and places
    * the result in this vector.
    * 
    * @param v
    * vector to compare with
    * @return this vector
    */
   public Vector3d max (Vector3d v) {
      if (v.x > x) {
         x = v.x;
      }
      if (v.y > y) {
         y = v.y;
      }
      if (v.z > z) {
         z = v.z;
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
   public Vector3d min (Vector3d v) {
      if (v.x < x) {
         x = v.x;
      }
      if (v.y < y) {
         y = v.y;
      }
      if (v.z < z) {
         z = v.z;
      }
      return this;
   }


   /** 
    * Interpolates the value at a location along a cubic Hermite spline.
    * The spline itself is defined by position and velocities at interval
    * end-points. The location is defined by the parameter <code>s</code>
    * in the range [0,1], and the actual time duration of the interval 
    * is given by <code>h</code>.
    * 
    * @param pr returns interpolated position
    * @param p0 position at the interval beginning
    * @param v0 velocity at the interval beginning
    * @param p1 position at the interval end
    * @param v1 velocity at the interval end
    * @param s interpolation location on the interval (in the range [0,1])
    * @param h interval time duration
    */
   public static void hermiteInterpolate (
      Vector3d pr, Vector3d p0, Vector3d v0, Vector3d p1, Vector3d v1,
      double s, double h) {

      double b1 = (2*s-3)*s*s;
      double b2 = ((s-2)*s+1)*s*h;
      double b3 = (s-1)*s*s*h;

      pr.x = b1*(p0.x-p1.x) + b2*v0.x + b3*v1.x + p0.x;
      pr.y = b1*(p0.y-p1.y) + b2*v0.y + b3*v1.y + p0.y;
      pr.z = b1*(p0.z-p1.z) + b2*v0.z + b3*v1.z + p0.z;
   }

   /** 
    * Interpolates velocity at a location along a cubic Hermite spline.
    * The spline itself is defined by position and velocities at interval
    * end-points. The location is defined by the parameter <code>s</code>
    * in the range [0,1], and the actual time duration of the interval 
    * is given by <code>h</code>.
    * 
    * @param vr returns interpolated velocity
    * @param p0 position at the interval beginning
    * @param v0 velocity at the interval beginning
    * @param p1 position at the interval end
    * @param v1 velocity at the interval end
    * @param s interpolation location on the interval (in the range [0,1])
    * @param h interval time duration
    */
   public static void hermiteVelocity (
      Vector3d vr, Vector3d p0, Vector3d v0, Vector3d p1, Vector3d v1,
      double s, double h) {

      double c1 = 6*(s-1)*s/h;
      double c2 = (3*s-4)*s+1;
      double c3 = (3*s-2)*s;

      vr.x = c1*(p0.x-p1.x) + c2*v0.x + c3*v1.x;
      vr.y = c1*(p0.y-p1.y) + c2*v0.y + c3*v1.y;
      vr.z = c1*(p0.z-p1.z) + c2*v0.z + c3*v1.z;
   }


   // public Vector3d copy()
   // {
   // return new Vector3d(this);
   // }

   // public Vector3d copyAndNegate()
   // {
   // Vector3d tmp = new Vector3d(this);
   // tmp.negate();
   // return tmp;
   // }

   // public Vector3d copyAndAdd (Vector3d v2)
   // {
   // Vector3d res = new Vector3d(this);
   // res.add (v2);
   // return res;
   // }

   // public Vector3d copyAndSub (Vector3d v2)
   // {
   // Vector3d res = new Vector3d(this);
   // res.sub (v2);
   // return res;
   // }

   // public Vector3d copyAndSubLeft (Vector3d v2)
   // {
   // Vector3d res = new Vector3d(this.size());
   // res.sub (v2, res);
   // return res;
   // }

   // public Vector3d copyAndScale (double s)
   // {
   // Vector3d res = new Vector3d(this);
   // res.scale (s);
   // return res;
   // }

   public Vector3d clone() {
      return (Vector3d)super.clone();
   }

   // Dec 9, 2008. John Lloyd: removed hashCode/equals override, since it was
   // causing confusion. For now equals (Object obj) should return true only if
   // the objects are identical. If equals based on contents are required, then
   // one should create a subclass.
   // /**
   // * Hash code based on spatial coordinates.
   // */
   // @Override
   // public int hashCode()
   // {
   // final int PRIME = 31;
   // int result = 1;
   // long temp;
   // temp = Double.doubleToLongBits(this.x);
   // result = PRIME * result + (int) (temp ^ (temp >>> 32));
   // temp = Double.doubleToLongBits(this.y);
   // result = PRIME * result + (int) (temp ^ (temp >>> 32));
   // temp = Double.doubleToLongBits(this.z);
   // result = PRIME * result + (int) (temp ^ (temp >>> 32));
   // return result;
   // }

   // /**
   // * Two Vector3d are equal, if their spatial coordinates are equal.
   // */
   // @Override
   // public boolean equals(Object obj)
   // {
   // if (this == obj)
   // return true;
   // if (obj == null)
   // return false;
   // if (!super.equals(obj))
   // return false;
   // if (getClass() != obj.getClass())
   // return false;
   // final Vector3d other = (Vector3d) obj;
   // if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x))
   // return false;
   // if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y))
   // return false;
   // if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(other.z))
   // return false;
   // return true;
   // }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, boolean withBrackets)
      throws IOException {
      if (withBrackets) {
         pw.print ("[ ");
      }
      pw.print (fmt.format(x)+" "+fmt.format(y)+" "+fmt.format(z));
      if (withBrackets) {
         pw.print (" ]");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      if (rtok.nextToken() == '[') {
         x = rtok.scanNumber();
         y = rtok.scanNumber();
         z = rtok.scanNumber();
         rtok.scanToken (']');
      }
      else {
         rtok.pushBack();
         x = rtok.scanNumber();
         y = rtok.scanNumber();
         z = rtok.scanNumber();
      }
   }

   public static void main (String[] args) {
      FunctionTimer timer = new FunctionTimer();

      Vector3d v1 = new Vector3d();
      Vector3d v2 = new Vector3d();
      Vector3d vr = new Vector3d();

      v1.setRandom();
      v2.setRandom();

      int cnt = 10000000;

      for (int i = 0; i < cnt; i++) {
         vr.sub (v1, v2);
         vr.add (v1, v2);
         v1.dot (v2);
      }

      timer.start();
      for (int i = 0; i < cnt; i++) {
         vr.sub (v1, v2);
      }
      timer.stop();
      System.out.println ("Vector3d.sub: " + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         vr.add (v1, v2);
      }
      timer.stop();
      System.out.println ("Vector3d.add: " + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         v1.dot (v2);
      }
      timer.stop();
      System.out.println ("Vector3d.dot: " + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         vr.scale (1 / .33, v1);
      }
      timer.stop();
      System.out.println ("Vector3d.scale: " + timer.result (cnt));

//      timer.start();
//      for (int i = 0; i < cnt; i++) {
//         vr.divide (.33, v1);
//      }
//      timer.stop();
//      System.out.println ("Vector3d.divide: " + timer.result (cnt));
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
   public void addObj (Vector3d v1) {
      add (v1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, Vector3d v1) {
      scaledAdd (s, v1);
   }
}
