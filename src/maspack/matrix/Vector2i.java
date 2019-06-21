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
 * Implements a 2 element integer vector, along its most commonly used
 * operations.
 * 
 * <p>
 * The size of these vectors is fixed.
 */
public class Vector2i extends VectoriBase {

   /**
    * Global zero vector. Should not be modified.
    */
   public static final Vector2i ZERO = new Vector2i();

   /**
    * First element
    */
   public int x;

   /**
    * Second element
    */
   public int y;

   /**
    * Creates a 2-vector and initializes its elements to 0.
    */
   public Vector2i() {
   }

   /**
    * Creates a 2-vector by copying an existing one.
    * 
    * @param v
    * vector to be copied
    */
   public Vector2i (Vector2i v) {
      set (v);
   }

   /**
    * Creates a 2-vector by copying an existing Vectori. The
    * size of the copied vector must be at least 2.
    * 
    * @param v
    * vector to be copied
    */
   public Vector2i (Vectori v) {
      if (v.size() < 2) {
         throw new IllegalArgumentException (
            "v must have a size of at least 2");
      }
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
   public Vector2i (int x, int y) {
      this.x = x;
      this.y = y;
   }

   /**
    * Creates a 2-vector with the supplied element values.
    * 
    * @param values
    * element values
    */
   public Vector2i (int[] values) {
      this.x = values[0];
      this.y = values[1];
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
    * Gets a single element of this vector. Elements 0 and 1 correspond to
    * x and y.
    * 
    * @param i
    * element index
    * @return element value throws ArrayIndexOutOfBoundsException if i is not in
    * the range 0 to 1.
    */
   public int get (int i) {
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
   public void get (int[] values) {
      if (values.length < 2) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= 2");
      }
      values[0] = x;
      values[1] = y;
   }

   /**
    * Sets a single element of this vector. Elements 0 and 1 correspond to
    * x and y.
    * 
    * @param i
    * element index
    * @param value
    * element value
    * @throws ArrayIndexOutOfBoundsException
    * if i is not in the range 0 to 1.
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
         default: {
            throw new ArrayIndexOutOfBoundsException (i);
         }
      }
   }

   /**
    * Sets the elements of this vector from an array of ints. The array
    * must have a length of at least 2.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (int[] values) {
      if (values.length < 2) {
         throw new IllegalArgumentException (
            "argument 'values' must have a length of at least 2");
      } 
      x = values[0];
      y = values[1];
   }

   /**
    * Sets the values of this vector to those of v1.
    * 
    * @param v1
    * vector whose values are copied
    */
   public void set (Vector2i v1) {
      x = v1.x;
      y = v1.y;
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
   public Vector2i add (Vector2i v1, Vector2i v2) {
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
   public Vector2i add (Vector2i v1) {
      x += v1.x;
      y += v1.y;
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
   public Vector2i sub (Vector2i v1, Vector2i v2) {
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
   public Vector2i sub (Vector2i v1) {
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
   public Vector2i negate (Vector2i v1) {
      x = -v1.x;
      y = -v1.y;
      return this;
   }

   /**
    * Negates this vector in place.
    * @return this vector
    */
   public Vector2i negate() {
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
   public Vector2i scale (double s) {
      x = (int)(s*x);
      y = (int)(s*y);
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
   public Vector2i scale (double s, Vector2i v1) {
      x = (int)(s*v1.x);
      y = (int)(s*v1.y);
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
   public Vector2i scaledAdd (double s, Vector2i v1, Vector2i v2) {
      x = (int)(s*v1.x) + v2.x;
      y = (int)(s*v1.y) + v2.y;
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
   public Vector2i scaledAdd (double s, Vector2i v1) {
      x += (int)(s*v1.x);
      y += (int)(s*v1.y);
      return this;
   }

   /**
    * {@inheritDoc}
    */
   public double norm() {
      return Math.sqrt (x * x + y * y);
   }

   /**
    * {@inheritDoc}
    */
   public double normSquared() {
      return x * x + y * y;
   }

   /**
    * {@inheritDoc}
    */
   public int maxElement() {
      int max = x;
      if (y > max) {
         max = y;
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
      return max;
   }

   /**
    * {@inheritDoc}
    */
   public int oneNorm() {
      return Math.abs (x) + Math.abs (y);
   }

   /**
    * Returns true if the elements of this vector exactly equal those of vector
    * <code>v1</code>.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal
    */
   public boolean equals (Vector2i v1) {
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
    * @param z
    * value for third element
    */
   public void set (int x, int y, int z) {
      this.x = x;
      this.y = y;
   }

   /**
    * Sets the elements of this vector to their absolute values.
    * @return this vector
    */
   public Vector2i absolute() {
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
   public Vector2i absolute (Vector2i v1) {
      x = Math.abs (v1.x);
      y = Math.abs (v1.y);
      return this;
   }

   /**
    * Sorts the contents of this vector by absolute element value, with x being
    * set to the largest value and z being set to the smallest value.
    */
   public void sortAbsolute() {
      int absx = (x < 0 ? -x : x);
      int absy = (y < 0 ? -y : y);
      int tmp;

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
    * Returns the index (0, 1, or 2) of the element of this vector with the
    * largest absolute value.
    * 
    * @return index of largest absolute value
    */
   public int maxAbsIndex() {
      int absx = (x < 0 ? -x : x);
      int absy = (y < 0 ? -y : y);

      if (absx >= absy) {
         return absx;
      }
      else {
         return absy;
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

      if (absx <= absy) {
         return absx;
      }
      else {
         return absy;
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
   public void sort (Vector2i v1) {
      set (v1);
      sort();
   }

   /**
    * Sorts the contents of this vector by element value, with x being set to
    * the largest value and z being set to the smallest value.
    */
   public void sort() {
      int tmp;

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
    * Computes the element-wise maximum of this vector and vector v and places
    * the result in this vector.
    * 
    * @param v
    * vector to compare with
    * @return this vector
    */
   public Vector2i max (Vector2i v) {
      if (v.x > x) {
         x = v.x;
         return this;
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
   public Vector2i min (Vector2i v) {
      if (v.x < x) {
         x = v.x;
         return this;
      }
      if (v.y < y) {
         y = v.y;
      }
      return this;
   }

   public Vector2i clone() {
      return (Vector2i)super.clone();
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
         pw.print (x+" "+y);
      }
      else {
         pw.print (fmt.format(x)+" "+fmt.format(y));
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
         rtok.scanToken (']');
      }
      else {
         rtok.pushBack();
         x = rtok.scanInteger();
         y = rtok.scanInteger();
      }
   }

}
