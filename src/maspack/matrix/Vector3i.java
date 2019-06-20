/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a 3 element integer vector, along its most commonly used
 * operations.
 * 
 * <p>
 * The size of these vectors is fixed.
 */
public class Vector3i extends VectoriBase {

   /**
    * Global zero vector. Should not be modified.
    */
   public static final Vector3i ZERO = new Vector3i();

   /**
    * First element
    */
   public int x;

   /**
    * Second element
    */
   public int y;

   /**
    * Third element
    */
   public int z;

   /**
    * Creates a 3-vector and initializes its elements to 0.
    */
   public Vector3i() {
   }

   /**
    * Creates a 3-vector by copying an existing one.
    * 
    * @param v
    * vector to be copied
    */
   public Vector3i (Vector3i v) {
      set (v);
   }

   /**
    * Creates a 3-vector by copying an existing Vectori. The
    * size of the copied vector must be at least 3.
    * 
    * @param v
    * vector to be copied
    */
   public Vector3i (Vectori v) {
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
   public Vector3i (int x, int y, int z) {
      set (x, y, z);
   }

   /**
    * Creates a 3-vector with the supplied element values.
    * 
    * @param values
    * element values
    */
   public Vector3i (int[] values) {
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
   public int get (int i) {
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
   public void get (int[] values) {
      if (values.length < 3) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= 3");
      }
      values[0] = x;
      values[1] = y;
      values[2] = z;
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
   public void set (int i, int value) {
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
    * Sets the elements of this vector from an array of ints. The array
    * must have a length of at least 3.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (int[] values) {
      if (values.length < 3) {
         throw new IllegalArgumentException (
            "argument 'values' must have a length of at least 3");
      } 
      x = values[0];
      y = values[1];
      z = values[2];
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
    * Sets the values of this vector to those of v1, converting to integer
    * using regular cast conversion. May result in a loss of precision.
    * 
    * @param v1
    * vector whose values are copied and rounded
    */
   public void set (Vector3d v1) {
      x = (int)v1.x;
      y = (int)v1.y;
      z = (int)v1.z;
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
   public Vector3i add (Vector3i v1, Vector3i v2) {
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
   public Vector3i add (Vector3i v1) {
      x += v1.x;
      y += v1.y;
      z += v1.z;
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
   public Vector3i sub (Vector3i v1, Vector3i v2) {
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
   public Vector3i sub (Vector3i v1) {
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
   public Vector3i negate (Vector3i v1) {
      x = -v1.x;
      y = -v1.y;
      z = -v1.z;
      return this;
   }

   /**
    * Negates this vector in place.
    * @return this vector
    */
   public Vector3i negate() {
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
   public Vector3i scale (double s) {
      x = (int)(s*x);
      y = (int)(s*y);
      z = (int)(s*z);
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
   public Vector3i scale (double s, Vector3i v1) {
      x = (int)(s*v1.x);
      y = (int)(s*v1.y);
      z = (int)(s*v1.z);
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
   public Vector3i scaledAdd (double s, Vector3i v1, Vector3i v2) {
      x = (int)(s*v1.x) + v2.x;
      y = (int)(s*v1.y) + v2.y;
      z = (int)(s*v1.z) + v2.z;
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
   public Vector3i scaledAdd (double s, Vector3i v1) {
      x += (int)(s*v1.x);
      y += (int)(s*v1.y);
      z += (int)(s*v1.z);
      return this;
   }

   /**
    * {@inheritDoc}
    */
   public double norm() {
      return Math.sqrt (x * x + y * y + z * z);
   }

   /**
    * {@inheritDoc}
    */
   public double normSquared() {
      return x * x + y * y + z * z;
   }

   /**
    * {@inheritDoc}
    */
   public int maxElement() {
      int max = x;
      if (y > max) {
         max = y;
      }
      if (z > max) {
         max = z;
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   public int minElement() {
      int min = x;
      if (y < min) {
         min = y;
      }
      if (z < min) {
         min = z;
      }
      return min;
   }

   /**
    * {@inheritDoc}
    */
   public int infinityNorm() {
      int max = Math.abs (x);
      if (Math.abs (y) > max) {
         max = Math.abs (y);
      }
      if (Math.abs (z) > max) {
         max = Math.abs (z);
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   public int oneNorm() {
      return Math.abs (x) + Math.abs (y) + Math.abs (z);
   }

   /**
    * Returns true if the elements of this vector exactly equal those of vector
    * <code>v1</code>.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal
    */
   public boolean equals (Vector3i v1) {
      return (x == v1.x && y == v1.y && z == v1.z);
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
   public void set (int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   /**
    * Sets the elements of this vector to their absolute values.
    * @return this vector
    */
   public Vector3i absolute() {
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
   public Vector3i absolute (Vector3i v1) {
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
      int absx = (x < 0 ? -x : x);
      int absy = (y < 0 ? -y : y);
      int absz = (z < 0 ? -z : z);
      int tmp;

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
      int absx = (x < 0 ? -x : x);
      int absy = (y < 0 ? -y : y);
      int absz = (z < 0 ? -z : z);

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
      int absx = (x < 0 ? -x : x);
      int absy = (y < 0 ? -y : y);
      int absz = (z < 0 ? -z : z);

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
   public void sort (Vector3i v1) {
      set (v1);
      sort();
   }

   /**
    * Sorts the contents of this vector by element value, with x being set to
    * the largest value and z being set to the smallest value.
    */
   public void sort() {
      int tmp;

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
    * {@inheritDoc}
    */
   public void setRandom() {
      super.setRandom();
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (int lower, int upper) {
      super.setRandom (lower, upper);
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (int lower, int upper, Random generator) {
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
   public void updateBounds (Vector3i vmin, Vector3i vmax) {
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
   public Vector3i max (Vector3i v) {
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
    * @return this 
    */
   public Vector3i min (Vector3i v) {
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

   public Vector3i clone() {
      return (Vector3i)super.clone();
   }

   /**
    * {@inheritDoc}
    */
   public void write (PrintWriter pw, NumberFormat fmt, boolean withBrackets)
      throws IOException {
      if (withBrackets) {
         pw.print ("[ ");
      }
      if (fmt.isFloatingPoint()) {
         pw.print (x+" "+y+" "+z);
      }
      else {
         pw.print (fmt.format(x)+" "+fmt.format(y)+" "+fmt.format(z));
      }
      if (withBrackets) {
         pw.print (" ]");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok) throws IOException {
      if (rtok.nextToken() == '[') {
         x = rtok.scanInteger();
         y = rtok.scanInteger();
         z = rtok.scanInteger();
         rtok.scanToken (']');
      }
      else {
         rtok.pushBack();
         x = rtok.scanInteger();
         y = rtok.scanInteger();
         z = rtok.scanInteger();
      }
   }

}
