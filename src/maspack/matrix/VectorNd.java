/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC), Fabien Pean (ETHZ)
 * (method reference returns)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;
import java.util.Arrays;

import maspack.util.Clonable;
import maspack.util.RandomGenerator;

/**
 * Implements general vectors, along with their most commonly used operations.
 * 
 * <p>
 * These vectors can be resized, either explicitly through a call to {@link
 * #setSize setSize}, or implicitly through operations that require the vector
 * size to be modified.
 */
public class VectorNd extends VectorBase
   implements java.io.Serializable, Clonable, VectorObject<VectorNd> {
   
   private static final long serialVersionUID = 1L;
   private boolean rowVectorP = false;
   double[] buf = new double[0];
   int size;
   boolean explicitBuffer = false;

   /**
    * Returns the internal buffer used to store the elements in this vector.
    * When possible, applications should access the vector elements using the
    * various set and get methods. However, if efficiency requires it, this
    * buffer can be used directly.
    * 
    * <p>
    * Note that the buffer may be larger than the vector. The i-th element in
    * the vector corresponds to the i-th entry in the buffer.
    * 
    * <p>
    * If this vector is resized, then the internal buffer may change and the
    * buffer previously returned by this routine may no longer be valid.
    * 
    * @return internal buffer for this vector
    * @see VectorNd#setBuffer
    */
   public double[] getBuffer() {
      return buf;
   }

   /**
    * Creates a vector with an initial size of zero.
    */
   public VectorNd() {
   }

   /**
    * Creates a vector of a specific size, and initializes its elements to 0. It
    * is legal to create a vector with a size of zero.
    * 
    * @param size
    * size of the vector
    * @throws ImproperSizeException
    * if size is negative
    */
   public VectorNd (int size) throws ImproperSizeException {
      if (size < 0) {
         throw new ImproperSizeException ("Negative size");
      }
      resetSize (size);
   }

   /**
    * Creates a vector of a specific size, and initializes its elements from an
    * array of values.
    * 
    * @param size
    * size of the vector
    * @param values
    * element values for the new vector
    * @throws ImproperSizeException
    * if size is negative
    */
   public VectorNd (int size, double[] values) throws ImproperSizeException {
      if (size < 0) {
         throw new ImproperSizeException ("Negative size");
      }
      resetSize (size);
      set (values);
   }

   /**
    * Creates a vector from an array of doubles. The vector size is determined
    * by the size of this array.
    * 
    * @param values
    * element values for the new vector
    */
   public VectorNd (double[] values) {
      resetSize (values.length);
      set (values);
   }

   /**
    * Creates a vector from an array of floats. The vector size is determined
    * by the size of this array.
    * 
    * @param values
    * element values for the new vector
    */
   public VectorNd (float[] values) {
      resetSize (values.length);
      set (values);
   }

   // public VectorNd (VectorNd v1)
   // {
   // resetSize (v1.size);
   // set (v1);
   // }

   /**
    * Creates a vector whose size and elements are the same as an existing
    * Vector.
    * 
    * @param v
    * vector object to be copied.
    */
   public VectorNd (Vector v) {
      resetSize (v.size());
      if (v instanceof VectorNd) { // faster to use VectorNd type
         set ((VectorNd)v);
      }
      else {
         set (v);
      }
   }

   /**
    * Creates a vector whose size and elements match a particular Matrix.
    * 
    * @param M
    * matrix object to be copied.
    * @throws ImproperSizeException
    * if the matrix cannot be interpreted as a row or column vector.
    */
   public VectorNd (Matrix M) {
      set (M);
   }

   /**
    * Returns the size of this vector.
    * 
    * @return size of the vector
    */
   public int size() {
      return size;
   }

   /**
    * Returns true if this vector is of fixed size. In the present
    * implementation, VectorNd objects always have variable size, and so this
    * routine always returns false.
    * 
    * @return true if this vector is of fixed size
    * @see VectorNd#setSize
    */
   public boolean isFixedSize() {
      return false;
   }

   /**
    * Sets the size of this vector. This operation may enlarge the internal
    * buffer associated with this vector, invalidating buffers previously
    * returned by {@link #getBuffer getBuffer}.
    * 
    * <p>
    * If a vector is resized, then previous element values which are still
    * within the new vector dimension are preserved. Other (new) element values
    * are undefined.
    * 
    * @param newSize
    * new vector size
    * @throws ImproperSizeException
    * if this vector has an explicit internal buffer and that buffer is too
    * small for the requested size
    */
   public void setSize (int newSize) throws ImproperSizeException {
      if (newSize < 0) {
         throw new ImproperSizeException ("Negative size");
      }
      else if (buf.length >= newSize) {
         size = newSize;
      }
      else {
         resetSizeAndCopy (newSize, newSize);
      }
   }

   public int adjustSize (int inc) throws ImproperSizeException {
      int oldSize = size;
      size += inc;
      if (size < 0) {
         size = 0;
      }
      else if (size > buf.length) {
         if (explicitBuffer) {
            size -= inc;
            throw new ImproperSizeException (
               "Adjusted vector size too large for explicit internal buffer");
         }
         else {
            int newcap = 2*size;
            resizeBuffer (oldSize, newcap);
         }
      }
      return oldSize;
   }

   private void resizeBuffer (int size, int newlen) {
      buf = Arrays.copyOf (buf, newlen);
   }

   public int getCapacity () {
      return buf.length;
   }

   public void setCapacity (int newcap) {
      if (newcap < 0) {
         throw new IllegalArgumentException ("capacity must be non-negative");
      }
      if (newcap != buf.length) {
         if (newcap < size) {
            resizeBuffer (newcap, newcap);
            size = newcap;
         }
         else {
            resizeBuffer (size, newcap);
         }
      }
   }

//   /**
//    * Sets the size of this vector. This operation is identical to
//    * {@link #setSize setSize}, <i>except</i> that the internal buffer may be
//    * grown larger than required, so that further size increases can be done
//    * quickly.
//    * 
//    * <p>
//    * If the vector is enlarged, then previous element values which are still
//    * within the new vector dimension are preserved. Other (new) element values
//    * are undefined.
//    * 
//    * @param newSize
//    * new vector size
//    * @throws ImproperSizeException
//    * if this vector has an explicit internal buffer and that buffer is too
//    * small for the requested size
//    */
//   public void setSizeAndCapacity (int newSize) throws ImproperSizeException {
//      if (newSize < 0) {
//         throw new ImproperSizeException ("Negative size");
//      }
//      int minCap = Math.max (3 * newSize / 2, newSize);
//      resetSizeAndCopy (newSize, minCap);
//   }

   void resetSizeAndCopy (int newSize, int minCap) throws ImproperSizeException {
      if (buf.length < minCap) {
         if (explicitBuffer) {
            throw new ImproperSizeException (
               "Requested vector size too large for explicit internal buffer");
         }
         else {
            resizeBuffer (size, minCap);
         }
      }
      this.size = newSize;
   }

   void resetSize (int newSize) throws ImproperSizeException {
      if (buf.length < newSize) {
         if (explicitBuffer) {
            throw new ImproperSizeException (
               "Requested vector size too large for explicit internal buffer");
         }
         else {
            buf = new double[newSize];
         }
      }
      this.size = newSize;
   }

   /**
    * Explicitly sets the size and internal buffer associated with this vector.
    * Any previous values will be discarded, and the vector will assume the new
    * values presently contained within the buffer. The length of
    * <code>buffer</code> must equal or exceed the specified size. The vector
    * can continue to be resized as long as the requested sizes do not exceed
    * the buffer size.
    * 
    * @param size
    * new vector size
    * @param buffer
    * explicit buffer for this vector
    * @throws ImproperSizeException
    * if the specified buffer is too small for the requested size
    * @see VectorNd#unsetBuffer
    */
   public void setBuffer (int size, double[] buffer) {
      if (buffer.length < size) {
         throw new ImproperSizeException (
            "Buffer too small for requested vector size");
      }
      explicitBuffer = true;
      this.size = size;
      buf = buffer;
   }

   /**
    * Removes an explicit buffer provided for this vector and replaces it with a
    * default implicit buffer. The vector retains its present size but all
    * values are replaced with zero.
    * 
    * @throws IllegalStateException
    * if this vector does not have an explicit buffer given by
    * {@link #setBuffer setBuffer}
    * @see VectorNd#setBuffer
    */
   public void unsetBuffer() {
      if (!explicitBuffer) {
         throw new IllegalStateException (
            "Vector does not have an explicit buffer");
      }
      explicitBuffer = false;
      buf = new double[size];
   }

   /**
    * Gets a single element of this vector.
    * 
    * @param i
    * element index
    * @return element value
    */
   public double get (int i) {
      return buf[i];
   }

   /**
    * {@inheritDoc}
    */
   public void get (double[] values) {
      if (values.length < size) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= "+size);
      }     
      for (int i = 0; i < size; i++) {
         values[i] = buf[i];
      }
   }

   /**
    * Copies the elements of this vector into an array of doubles,
    * starting at a particular location.
    * 
    * @param values
    * array into which values are copied
    * @param idx starting point within values where copying should begin
    * @return updated idx value
    */
   public int get (double[] values, int idx) {
      for (int i = 0; i < size; i++) {
         values[idx++] = buf[i];
      }
      return idx;
   }

   /**
    * Sets a single element of this vector.
    * 
    * @param i
    * element index
    * @param value
    * element value
    */
   public void set (int i, double value) {
      buf[i] = value;
   }

   /** 
    * Appends a value to the end of this vector, increasing its size by one.
    * If the vector's capacity (i.e., the length of the underlying array) needs
    * to be increased, this is done by an extended amount in order to reduce
    * the overall number of capacity increases that may be incurred by a
    * sequence of <code>append</code> calls.
    * 
    * @param value value to append to the end of this vector
    * @throws ImproperSizeException if the capacity needs to be increased but
    * the internal buffer is explicit and so cannot be increased.
    */
   public void append (double value) {
      if (size >= buf.length) {
         if (explicitBuffer) {
            throw new ImproperSizeException (
               "Adjusted vector size too large for explicit internal buffer");
         }
         else {
            int newcap = Math.max (2*size, 4);
            resizeBuffer (size, newcap);
         }
      }
      buf[size++] = value;
      // if (++size > buf.length) {
      //    if (explicitBuffer) {
      //       size--;
      //       throw new ImproperSizeException (
      //          "Adjusted vector size too large for explicit internal buffer");
      //    }
      //    else {
      //       int newcap = 2*size;
      //       resizeBuffer (size-1, newcap);
      //    }
      // }
      // buf[size-1] = value;
   }

   /**
    * Sets the elements of this vector from an array of doubles. If the
    * array length is less than the current size, this vector is resized
    * to the array length.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (double[] values) {
      if (values.length != size()) {
         resetSize (values.length);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = values[i];
      }
   }

   /**
    * Sets the elements of this vector from an array of floats. If the
    * array length is less than the current size, this vector is resized
    * to the array length.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (float[] values) {
      if (values.length != size()) {
         resetSize (values.length);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = values[i];
      }
   }


   /**
    * {@inheritDoc}
    */
   public int set (double[] values, int idx) {
      for (int i = 0; i < size; i++) {
         buf[i] = values[idx++];
      }
      return idx;
   }

   /**
    * Sets the size and values of this vector to those of v1.
    * 
    * @param v1
    * vector whose size and values are copied
    */
   public void set (VectorNd v1) {
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = v1.buf[i];
      }
      rowVectorP = v1.rowVectorP;
   }

   /**
    * Gets a subset of the values of this vector, beginning at a specified
    * offset, and places them in <code>v1</code>.
    * 
    * @param off
    * offset where copying should begin in this vector
    * @param v1
    * vector returning the sub-vector values
    * @throws ImproperSizeException
    * if this vector is not large enough to accomodate the specified subvector
    */
   public void getSubVector (int off, Vector v1) {
      int v1size = v1.size();
      if (size < off + v1size) {
         throw new ImproperSizeException (
            "vector not large enough for sub-vector");
      }
      if (v1 instanceof VectorNd) {
         double[] v1buf = ((VectorNd)v1).buf;
         for (int i = 0; i < v1size; i++) {
            v1buf[i] = buf[i + off];
         }
      }
      else {
         for (int i = 0; i < v1size; i++) {
            v1.set (i, buf[i + off]);
         }
      }
   }

   /**
    * Sets a subset of the values of this vector, beginning at a specified
    * offset, to the values of v1.
    * 
    * @param off
    * offset where copying should begin in this vector
    * @param v1
    * vector whose values are copied
    * @throws ImproperSizeException
    * if this vector is not large enough to accomodate the specified subvector
    */
   public void setSubVector (int off, Vector v1) {
      int v1size = v1.size();
      if (size < off + v1size) {
         throw new ImproperSizeException (
            "vector not large enough for sub-vector");
      }
      if (v1 instanceof VectorNd) {
         double[] v1buf = ((VectorNd)v1).buf;
         for (int i = 0; i < v1size; i++) {
            buf[i + off] = v1buf[i];
         }
      }
      else {
         for (int i = 0; i < v1size; i++) {
            buf[i + off] = v1.get(i);
         }
      }
   }

   /**
    * Gets a subset of the values of this vector, whose indices are
    * specified by <code>idxs</code>, and places them in <code>v1</code>.
    * 
    * @param idxs
    * indices of the values in this vector that are to be obtained
    * @param v1
    * vector returning the sub-vector values
    * @throws ImproperSizeException
    * if the size of <code>idxs</code> is less than <code>v1</code>
    * @throws ArrayIndexOutOfBoundsException if any of the indices in
    * <code>idxs</code> are out of bounds
    */
   public void getSubVector (int[] idxs, Vector v1) {
      int v1size = v1.size();
      if (idxs.length < v1size) {
         throw new ImproperSizeException (
            "'idxs' not large enough to cover all of 'v1'");
      }
      if (v1 instanceof VectorNd) {
         double[] v1buf = ((VectorNd)v1).buf;
         for (int i = 0; i < v1size; i++) {
            v1buf[i] = buf[idxs[i]];
         }
      }
      else {
         for (int i = 0; i < v1size; i++) {
            v1.set (i, buf[idxs[i]]);
         }
      }      
   }

   /**
    * Sets a subset of the values of this vector, whose indices are
    * specified by <code>idxs</code>, to the values of <code>v1</code>.
    * 
    * @param idxs
    * indices of the values in this vector that are to be set
    * @param v1
    * vector containing the sub-vector values
    * @throws ImproperSizeException
    * if the size of <code>idxs</code> is less than <code>v1</code>
    * @throws ArrayIndexOutOfBoundsException if any of the indices in
    * <code>idxs</code> are out of bounds
    */
   public void setSubVector (int[] idxs, Vector v1) {
      int v1size = v1.size();
      if (idxs.length < v1size) {
         throw new ImproperSizeException (
            "'idxs' not large enough to cover all of 'v1'");
      }
      if (v1 instanceof VectorNd) {
         double[] v1buf = ((VectorNd)v1).buf;
         for (int i = 0; i < v1size; i++) {
            buf[idxs[i]] = v1buf[i];
         }
      }
      else {
         for (int i = 0; i < v1size; i++) {
            buf[idxs[i]] = v1.get(i);
         }
      }
   }

   /**
    * Adds vector v1 to v2 and places the result in this vector. This vector is
    * resized if necessary.
    * 
    * @param v1
    * left-hand vector
    * @param v2
    * right-hand vector
    * @return this vector
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public VectorNd add (VectorNd v1, VectorNd v2) throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = v1.buf[i] + v2.buf[i];
      }
      return this;
   }

   /**
    * Adds this vector to v1 and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    * @throws ImproperSizeException
    * if v1 has a size less than this vector
    */
   public VectorNd add (VectorNd v1) throws ImproperSizeException {
      if (v1.size < size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      for (int i = 0; i < size; i++) {
         buf[i] += v1.buf[i];
      }
      return this;
   }

   /**
    * Adds a value to the i-th element of this vector.
    * 
    * @param i
    * index of the element
    * @param value
    * value to be added
    */
   public void add (int i, double value) {
      buf[i] += value;
   }

   /**
    * Subtracts vector v2 from v1 and places the result in this vector. This
    * vector is resized if necessary.
    * 
    * @param v1
    * left-hand vector
    * @param v2
    * right-hand vector
    * @return this vector
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public VectorNd sub (VectorNd v1, VectorNd v2) throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = v1.buf[i] - v2.buf[i];
      }
      return this;
   }

   /**
    * Subtracts v1 from this vector and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @return this vector
    * @throws ImproperSizeException
    * if v1 has a size less than this vector
    */
   public VectorNd sub (VectorNd v1) throws ImproperSizeException {
      if (v1.size < size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      for (int i = 0; i < size; i++) {
         buf[i] -= v1.buf[i];
      }
      return this;
   }

   /**
    * Sets this vector to the negative of v1. This vector is resized if
    * necessary.
    * 
    * @param v1
    * vector to negate
    * @return this vector
    */
   public VectorNd negate (VectorNd v1) {
      if (v1.size != size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = -v1.buf[i];
      }
      return this;
   }

   /**
    * Negates this vector in place.
    * @return this vector
    */
   public VectorNd negate() {
      for (int i = 0; i < size; i++) {
         buf[i] = -buf[i];
      }
      return this;
   }

   /**
    * Scales the elements of this vector by <code>s</code>.
    * 
    * @param s
    * scaling factor
    * @return this vector
    */
   public VectorNd scale (double s) {
      for (int i = 0; i < size; i++) {
         buf[i] = s * buf[i];
      }
      return this;
   }

   /**
    * Scales the elements of vector v1 by <code>s</code> and places the
    * results in this vector. This vector is resized if necessary.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    * @return this vector
    */
   public VectorNd scale (double s, VectorNd v1) {
      if (v1.size != size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = s * v1.buf[i];
      }
      return this;
   }

   /**
    * Computes the interpolation <code>(1-s) v1 + s v2</code> and places the
    * result in this vector. This vector is resized if necessary.
    * 
    * @param v1
    * left-hand vector
    * @param s
    * interpolation factor
    * @param v2
    * right-hand vector
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public void interpolate (VectorNd v1, double s, VectorNd v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = (1 - s) * v1.buf[i] + s * v2.buf[i];
      }
   }

   /**
    * Computes the interpolation <code>(1-s) this + s v1</code> and places the
    * result in this vector.
    * 
    * @param s
    * interpolation factor
    * @param v1
    * right-hand vector
    * @throws ImproperSizeException
    * if v1 has a size less than this vector
    */
   public void interpolate (double s, VectorNd v1) throws ImproperSizeException {
      if (v1.size < size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      for (int i = 0; i < size; i++) {
         buf[i] = (1 - s) * buf[i] + s * v1.buf[i];
      }
   }

   /**
    * Computes <code>s v1 + v2</code> and places the result in this vector.
    * This vector is resized if necessary.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    * @param v2
    * vector to be added
    * @return this vector
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public VectorNd scaledAdd (double s, VectorNd v1, VectorNd v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = s * v1.buf[i] + v2.buf[i];
      }
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
    * @throws ImproperSizeException
    * if v1 has a size less than this vector
    */
   public VectorNd scaledAdd (double s, VectorNd v1) throws ImproperSizeException {
      if (v1.size < size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      for (int i = 0; i < size; i++) {
         buf[i] += s * v1.buf[i];
      }
      return this;
   }

   /**
    * Computes <code>s1 v1 + s2 v2</code> and places the result in this
    * vector. This vector is resized if necessary.
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
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public VectorNd combine (double s1, VectorNd v1, double s2, VectorNd v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = s1 * v1.buf[i] + s2 * v2.buf[i];
      }
      return this;
   }

   /**
    * Returns the 2 norm of this vector. This is the square root of the sum of
    * the squares of the elements.
    * 
    * @return vector 2 norm
    */
   public double norm() {
      double sum = 0;
      for (int i = 0; i < size; i++) {
         sum += buf[i] * buf[i];
      }
      return Math.sqrt (sum);
   }

   /**
    * Returns the square of the 2 norm of this vector. This is the sum of the
    * squares of the elements.
    * 
    * @return square of the 2 norm
    */
   public double normSquared() {
      double sum = 0;
      for (int i = 0; i < size; i++) {
         sum += buf[i] * buf[i];
      }
      return sum;
   }

   /**
    * Returns the maximum element value of this vector.
    * 
    * @return maximal element
    */
   public double maxElement() {
      return buf[maxIndex()];
   }

   /**
    * Returns the index of the maximum element of this vector.
    * 
    * @return maximal element index
    */
   public int maxIndex() {
      double max = Double.NEGATIVE_INFINITY;
      int idx = -1;
      for (int i = 0; i < size; i++) {
         if (buf[i] > max) {
            max = buf[i];
            idx = i;
         }
      }
      return idx;
   }

   /**
    * Returns the minimum element value of this vector.
    * 
    * @return minimal element
    */
   public double minElement() {
      return buf[minIndex()];
   }

   /**
    * Returns the index of the minimum element of this vector.
    * 
    * @return minimal element index
    */
   public int minIndex() {
      double min = Double.POSITIVE_INFINITY;
      int idx = -1;
      for (int i = 0; i < size; i++) {
         if (buf[i] < min) {
            min = buf[i];
            idx = i;
         }
      }
      return idx;
   }

   /**
    * Returns the infinity norm of this vector. This is the maximum absolute
    * value over all elements.
    * 
    * @return vector infinity norm
    */
   public double infinityNorm() {
      double max = 0;
      for (int i = 0; i < size; i++) {
         double abs = Math.abs (buf[i]);
         if (abs > max) {
            max = abs;
         }
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
      double sum = 0;
      for (int i = 0; i < size; i++) {
         sum += Math.abs (buf[i]);
      }
      return sum;
   }
   
   /**
    * Returns the mean value of the elements of this vector. 
    * 
    * @return vector mean
    */
   public double mean() {
      double sum = 0;
      for (int i = 0; i < size; i++) {
         sum += buf[i];
      }
      return sum / size;
   }


   /**
    * Returns the dot product of this vector and v1.
    * 
    * @param v1
    * right-hand vector
    * @return dot product
    * @throws ImproperSizeException
    * if this vector and v1 have different sizes
    */
   public double dot (VectorNd v1) throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      double sum = 0;
      for (int i = 0; i < size; i++) {
         sum += buf[i] * v1.buf[i];
      }
      return sum;
   }

   /**
    * Returns the angle between this vector and v1. The angle is defined as
    * <code>acos(c)</code>, where <code>c</code> is the dot product of unit
    * vectors parallel to this vector and v1.
    * 
    * @param v1
    * right-hand vector
    * @return angle between vectors, in radians
    * @throws ImproperSizeException
    * if this vector and v1 have different sizes
    */
   public double angle (VectorNd v1) throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      double sum = 0;
      double norm1Squared = 0;
      double norm2Squared = 0;
      for (int i = 0; i < size; i++) {
         sum += buf[i] * v1.buf[i];
         norm1Squared += buf[i] * buf[i];
         norm2Squared += v1.buf[i] * v1.buf[i];
      }
      double cos = sum / (Math.sqrt (norm1Squared) * Math.sqrt (norm2Squared));
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
   public VectorNd normalize() {
      double norm = norm();
      for (int i = 0; i < size; i++) {
         buf[i] /= norm;
      }
      return this;
   }

   /**
    * Computes a unit vector in the direction of v1 and places the result in
    * this vector. This vector is resized if necessary.
    * 
    * @param v1
    * vector to normalize
    * @return this vector
    * @throws ImproperSizeException
    * if this vector needs resizing but is of fixed size
    */
   public VectorNd normalize (VectorNd v1) {
      if (size != v1.size) {
         resetSize (v1.size);
      }
      double norm = v1.norm();
      for (int i = 0; i < size; i++) {
         buf[i] = v1.buf[i] / norm;
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
    * @return false if the vectors are not equal within the specified tolerance,
    * or have different sizes
    */
   public boolean epsilonEquals (VectorNd v1, double eps) {
      if (v1.size != size) {
         return false;
      }
      for (int i = 0; i < size; i++) {
         double dist = Math.abs (buf[i] - v1.buf[i]);
         if (!(dist <= eps)) {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if the elements of this vector exactly equal those of vector
    * <code>v1</code>.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal or have different sizes
    */
   public boolean equals (VectorNd v1) {
      if (v1.size != size) {
         return false;
      }
      for (int i = 0; i < size; i++) {
         if (buf[i] != v1.buf[i]) {
            return false;
         }
      }
      return true;
   }

   /** 
    * Returns the sum of all the elements in this vector.
    * 
    * @return sum of all the elements
    */   
   public double sum() {
      double sum = 0;
      for (int i = 0; i < size; i++) {
         sum += buf[i];
      }
      return sum;
   }

   /**
    * Sets the elements of this vector to zero.
    */
   public void setZero() {
      for (int i = 0; i < size; i++) {
         buf[i] = 0;
      }
   }

   /**
    * Sets the elements of this vector to their absolute values.
    * @return this vector
    */
   public VectorNd absolute() {
      for (int i = 0; i < size; i++) {
         buf[i] = Math.abs (buf[i]);
      }
      return this;
   }

   /**
    * Sets the elements of this vector to the absolute value of v1. This vector
    * is resized if necessary.
    * 
    * @param v1
    * vector to take the absolute value of
    * @return this vector
    * @throws ImproperSizeException
    * if this vector needs resizing but is of fixed size
    */
   public VectorNd absolute (VectorNd v1) {
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = Math.abs (v1.buf[i]);
      }
      return this;
   }

   private void quickSort (double[] buf, int left, int right) {
      double pivot;
      int lHold, rHold;

      lHold = left;
      rHold = right;
      pivot = buf[left];
      while (left < right) {
         while ((buf[right] <= pivot) && (left < right)) {
            right--;
         }
         if (left != right) {
            buf[left] = buf[right];
            left++;
         }
         while ((buf[left] >= pivot) && (left < right)) {
            left++;
         }
         if (left != right) {
            buf[right] = buf[left];
            right--;
         }
      }
      buf[left] = pivot;
      int pivotIndex = left;
      left = lHold;
      right = rHold;
      if (left < pivotIndex) {
         quickSort (buf, left, pivotIndex - 1);
      }
      if (right > pivotIndex) {
         quickSort (buf, pivotIndex + 1, right);
      }
   }

   /**
    * Sorts the contents of this vector by element value, from largest to
    * smallest value.
    */
   public void sort() {
      quickSort (buf, 0, size - 1);
   }

   /**
    * Sorts the contents of vector v1 by element value, from largest to smallest
    * value, and places the result into this vector.
    * 
    * @param v1
    * vector to sort
    */
   public void sort (VectorNd v1) {
      set (v1);
      quickSort (buf, 0, size - 1);
   }

   /**
    * Multiplies matrix M by the vector b and places the result in this vector.
    * This vector is resized if necessary.
    * 
    * @param M
    * left-hand matrix
    * @param b
    * right-hand vector
    * @return this vector
    * @throws ImproperSizeException
    * if the size of b does not equal the number of columns of M, or if this
    * vector needs resizing but is of fixed size
    */
   public VectorNd mul (Matrix M, VectorNd b) {
      M.mul (this, b);
      return this;
   }

   /**
    * Multiplies the transpose of matrix M by the vector b and places the result
    * in this vector. Note that this is equivalent to pre-multiplying M by b.
    * This vector is resized if necessary.
    * 
    * @param M
    * left-hand matrix
    * @param b
    * right-hand vector
    * @return this vector
    * @throws ImproperSizeException
    * if the size of b does not equal the number of rows of M, or if this vector
    * needs resizing but is of fixed size
    */
   public VectorNd mulTranspose (Matrix M, VectorNd b) {
      M.mulTranspose (this, b);
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
    * Adds a random perturbation to this vector, in the range
    * <code>[-lim,lim]</code>, where 
    * <pre>
    * lim = eps * infNorm
    * </pre>
    * with <code>eps</code> supplied as an argument and <code>infNorm</code>
    * is the infinityNorm of this vector.
    * @param eps generates the noise range
    */
   public void addNoise (double eps) {
      addNoise (eps, RandomGenerator.get());
   }

   /** 
    * Adds a random perturbation to this vector, in the range
    * <code>[-lim,lim]</code>, where 
    * <pre>
    * lim = eps * infNorm
    * </pre>
    * with <code>eps</code> supplied as an argument and <code>infNorm</code>
    * is the infinityNorm of this vector.
    * @param eps generates the noise range
    * @param generator random number generator to use
    */
   public void addNoise (double eps, Random generator) {
      double rng = 2*eps*infinityNorm();
      for (int i=0; i<size; i++) {
         buf[i] += rng*(generator.nextDouble() - 0.5);
      }
   }

   /**
    * Computes the element-wise maximum of this vector and vector v and places
    * the result in this vector.
    * 
    * @param v
    * vector to compare with
    * @return this vector
    * @throws ImproperSizeException
    * if this vector and v have different sizes
    */

   public VectorNd max (VectorNd v) throws ImproperSizeException {
      if (v.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      for (int i = 0; i < size; i++) {
         if (v.buf[i] > buf[i]) {
            buf[i] = v.buf[i];
         }
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
    * @throws ImproperSizeException
    * if this vector and v have different sizes
    */
   public VectorNd min (VectorNd v) throws ImproperSizeException {
      if (v.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      for (int i = 0; i < size; i++) {
         if (v.buf[i] < buf[i]) {
            buf[i] = v.buf[i];
         }
      }
      return this;
   }

//   public VectorNd copy() {
//      return new VectorNd (this);
//   }
//
//   public VectorNd copyAndNegate() {
//      VectorNd tmp = new VectorNd (this);
//      tmp.negate();
//      return tmp;
//   }
//
//   public VectorNd copyAndAdd (VectorNd v2) {
//      VectorNd res = new VectorNd (this);
//      res.add (v2);
//      return res;
//   }
//
//   public VectorNd copyAndSub (VectorNd v2) {
//      VectorNd res = new VectorNd (this);
//      res.sub (v2);
//      return res;
//   }
//
//   public VectorNd copyAndSubLeft (VectorNd v2) {
//      VectorNd res = new VectorNd (this.size());
//      res.sub (v2, res);
//      return res;
//   }
//
//   public VectorNd copyAndScale (double s) {
//      VectorNd res = new VectorNd (this);
//      res.scale (s);
//      return res;
//   }

   public boolean isRowVector() {
      return rowVectorP;
   }

   public boolean setRowVector (boolean isRow) {
      rowVectorP = isRow;
      return true;
   }

   public VectorNd clone() {
      VectorNd vec = (VectorNd)super.clone();
      vec.buf = new double[size];
      for (int i = 0; i < size; i++) {
         vec.buf[i] = buf[i];
      }
      explicitBuffer = false;
      return vec;
   }

   /**
    * Rearrange the elements of this vector according to the specified
    * permutation, such that each element i is replaced by element
    * permutation[i].
    * 
    * @param permutation
    * describes the element exchanges
    * @throws ImproperSizeException
    * if the length of <code>permutation</code> is less than the size of this
    * vector.
    */
   public void permute (int[] permutation) {
      if (permutation.length < size) {
         throw new ImproperSizeException ("permutation argument too short");
      }
      // brute force way for now: allocate temporary memory
      double[] tmp = new double[size];
      for (int i = 0; i < size; i++) {
         tmp[i] = buf[i];
      }
      for (int i = 0; i < size; i++) {
         buf[i] = tmp[permutation[i]];
      }
   }

   /**
    * Exchange elements i and j of this vector.
    */
   public void exchangeElements (int i, int j) {
      if (i < 0 || i >= size) {
         throw new ArrayIndexOutOfBoundsException (
            "index i is "+i+"; must be in the range 0 to "+(size-1));
      }
      if (j < 0 || j >= size) {
         throw new ArrayIndexOutOfBoundsException (
            "index j is "+j+"; must be in the range 0 to "+(size-1));
      }
      double tmp = buf[i];
      buf[i] = buf[j];
      buf[j] = tmp;
   }

   /** 
    * Returns the distance between this vector and another. The distance
    * is just the Euclidean norm of the different between the two vectors.
    * 
    * @param v vector to find distance with respect to
    * @return distance from v
    */
   public double distance (VectorNd v) {
      if (v.size != size) {
        throw new ImproperSizeException ("vectors have different sizes");
      }
      double mag2 = 0;
      for (int i=0; i<size; i++) {
         double del = buf[i] - v.buf[i];
         mag2 += del*del;
      }
      return Math.sqrt(mag2);
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
      VectorNd pr, VectorNd p0, VectorNd v0, VectorNd p1, VectorNd v1,
      double s, double h) {

      double b1 = (2*s-3)*s*s;
      double b2 = ((s-2)*s+1)*s*h;
      double b3 = (s-1)*s*s*h;

      int size = pr.size();
      if (size != p0.size() || size != v0.size() ||
          size != p1.size() || size != v1.size() ) {
         throw new ImproperSizeException (
            "arguments have inconsistent sizes, expecting "+size);
      }

      double[] prb = pr.getBuffer();
      double[] p0b = p0.getBuffer();
      double[] v0b = v0.getBuffer();
      double[] p1b = p1.getBuffer();
      double[] v1b = v1.getBuffer();

      for (int i=0; i<size; i++) {
         prb[i] = b1*(p0b[i]-p1b[i]) + b2*v0b[i] + b3*v1b[i] + p0b[i];
      }
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
   public void addObj (VectorNd v1) {
      add (v1);
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAddObj (double s, VectorNd v1) {
      scaledAdd (s, v1);
   }
}
