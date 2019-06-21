/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC), Fabien Pean (ETHZ)
 * (method reference returns)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import maspack.util.ReaderTokenizer;

/**
 * Implements a general vector of integers. We have provided integer
 * implementations of all methods of VectorNd for which it makes sense to do
 * so.
 * 
 * <p> These vectors can be resized, either explicitly through a call to {@link
 * #setSize setSize}, or implicitly through operations that require the vector
 * size to be modified.
 */
public class VectorNi extends VectoriBase implements java.io.Serializable {
   private static final long serialVersionUID = 1L;

   int[] buf = new int[0];
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
    * @see VectorNi#setBuffer
    */
   public int[] getBuffer() {
      return buf;
   }

   /**
    * Creates a vector with an initial size of zero.
    */
   public VectorNi() {
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
   public VectorNi (int size) throws ImproperSizeException {
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
   public VectorNi (int size, int[] values) throws ImproperSizeException {
      if (size < 0) {
         throw new ImproperSizeException ("Negative size");
      }
      resetSize (size);
      set (values);
   }

   /**
    * Creates a vector from an array of ints. The vector size is determined
    * by the size of this array.
    * 
    * @param values
    * element values for the new vector
    */
   public VectorNi (int[] values) {
      resetSize (values.length);
      set (values);
   }

   /**
    * Creates a new VectorNi from an existing one.
    * 
    * @param vec VectorNi to be copied
    */
   public VectorNi (VectorNi vec) {
      set (vec);
   }

   /**
    * Creates a new VectorNi from an existing Vectori.
    * 
    * @param vec Vectori to be copied
    */
   public VectorNi (Vectori vec) {
      set (vec);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int size() {
      return size;
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
            buf = new int[newSize];
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
    * @see VectorNi#unsetBuffer
    */
   public void setBuffer (int size, int[] buffer) {
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
    * @see VectorNi#setBuffer
    */
   public void unsetBuffer() {
      if (!explicitBuffer) {
         throw new IllegalStateException (
            "Vector does not have an explicit buffer");
      }
      explicitBuffer = false;
      buf = new int[size];
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int get (int i) {
      return buf[i];
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void get (int[] values) {
      if (values.length < size) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= "+size);
      }         
      for (int i = 0; i < size; i++) {
         values[i] = buf[i];
      }
   }

   /**
    * Copies the elements of this vector into an array of int,
    * starting at a particular location.
    * 
    * @param values
    * array into which values are copied
    * @param idx starting point within values where copying should begin
    * @return updated idx value
    */
   public int get (int[] values, int idx) {
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
   /**
    * {@inheritDoc}
    */
   @Override
   public void set (int i, int value) {
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
   public void append (int value) {
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
    * {@inheritDoc}
    */
   @Override
   public void set (int[] values) {
      if (values.length != size) {
         resetSize (values.length);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = values[i];
      }
   }

   /**
    * Sets the elements of this vector from an array of ints,
    * starting from a particular location.
    * 
    * @param values
    * array into which values are copied
    * @param idx starting point within values from which copying should begin
    * @return updated idx value
    */
   public int set (int[] values, int idx) {
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
   public void set (VectorNi v1) {
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = v1.buf[i];
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
   public void setSubVector (int off, VectorNi v1) {
      if (size < off + v1.size) {
         throw new ImproperSizeException (
            "vector not large enough for sub-vector");
      }
      for (int i = 0; i < v1.size; i++) {
         buf[i + off] = v1.buf[i];
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
   public VectorNi add (VectorNi v1, VectorNi v2) throws ImproperSizeException {
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
   public VectorNi add (VectorNi v1) throws ImproperSizeException {
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
   public void add (int i, int value) {
      buf[i] += value;
   }

   /**
    * Subtracts vector v1 from v2 and places the result in this vector. This
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
   public VectorNi sub (VectorNi v1, VectorNi v2) throws ImproperSizeException {
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
   public VectorNi sub (VectorNi v1) throws ImproperSizeException {
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
   public VectorNi negate (VectorNi v1) {
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
    * 
    * @return this vector
    */
   public VectorNi negate() {
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
   public VectorNi scale (double s) {
      for (int i = 0; i < size; i++) {
         buf[i] = (int)(s*buf[i]);
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
   public VectorNi scale (double s, VectorNi v1) {
      if (v1.size != size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = (int)(s*v1.buf[i]);
      }
      return this;
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
    * if v1 and v2 have different sizes
    */
   public VectorNi scaledAdd (double s, VectorNi v1, VectorNi v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = (int)(s*v1.buf[i]) + v2.buf[i];
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
   public VectorNi scaledAdd (double s, VectorNi v1)
      throws ImproperSizeException {
      if (v1.size < size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      for (int i = 0; i < size; i++) {
         buf[i] += (int)(s*v1.buf[i]);
      }
      return this;
   }

   /**
    * Returns true if this vector is of fixed size. In the present
    * implementation, VectorNi objects always have variable size, and so this
    * routine always returns false.
    * 
    * @return true if this vector is of fixed size
    * @see VectorNi#setSize
    */
   @Override
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
   @Override   
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

   /**
    * {@inheritDoc}
    */
   @Override
   public double norm() {
      return Math.sqrt(normSquared());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double normSquared() {
      int sqrSum = 0;
      for (int i = 0; i < size; i++) {
         sqrSum += buf[i]*buf[i];
      }
      return sqrSum;
      
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int oneNorm() {
      int sum = 0;
      for (int i = 0; i < size; i++) {
         sum += Math.abs (buf[i]);
      }
      return sum;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public int infinityNorm() {
      int max = 0;
      for (int i = 0; i < size; i++) {
         int abs = Math.abs (buf[i]);
         if (abs > max) {
            max = abs;
         }
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int maxElement() {
      if (size == 0) {
         return 0;
      }
      int max = Integer.MIN_VALUE;
      for (int i = 0; i < size; i++) {
         if (buf[i] > max) {
            max = buf[i];
         }
      }
      return max;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int minElement() {
      if (size == 0) {
         return 0;
      }
      int min = Integer.MAX_VALUE;
      for (int i = 0; i < size; i++) {
         if (buf[i] < min) {
            min = buf[i];
         }
      }
      return min;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals (Vectori v1) {
      if (v1 instanceof VectorNi) {
         return equals ((VectorNi)v1);
      }
      else {
         if (v1.size() != size) {
            return false;
         }
         for (int i = 0; i < size; i++) {
            if (buf[i] != v1.get(i)) {
               return false;
            }
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
   public boolean equals (VectorNi v1) {
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
   public int sum() {
      int sum = 0;
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
    *
    * @return this vector
    */
   public VectorNi absolute() {
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
   public VectorNi absolute (VectorNi v1) {
      if (size != v1.size) {
         resetSize (v1.size);
      }
      for (int i = 0; i < size; i++) {
         buf[i] = Math.abs (v1.buf[i]);
      }
      return this;
   }

   private void quickSort (int[] buf, int left, int right) {
      int pivot;
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
   public void sort (VectorNi v1) {
      set (v1);
      quickSort (buf, 0, size - 1);
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
   public VectorNi max (VectorNi v) throws ImproperSizeException {
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
   public VectorNi min (VectorNi v) throws ImproperSizeException {
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

   public VectorNi clone() {
      VectorNi vec = null;
      try {
         vec = (VectorNi)super.clone();
      }
      catch (Exception e) {
         // Can't happen, right?
      }
      vec.buf = new int[size];
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
      int[] tmp = new int[size];
      for (int i = 0; i < size; i++) {
         tmp[i] = buf[i];
      }
      for (int i = 0; i < size; i++) {
         buf[i] = tmp[permutation[i]];
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
    * Sets the contents of this vector to values read from a ReaderTokenizer.
    * The input should consist of a sequence of integers, separated by white
    * space and optionally surrounded by square brackets <code>[ ]</code>.
    * 
    * <p>
    * If the input is not surrounded by square brackets, then the number of
    * values should equal the current {@link #size size} of this vector.
    * 
    * <p> If the input is surrounded by square brackets, then all values up to
    * the closing bracket are read, and this vector will be resized to
    * resulting number of values.
    * For example,
    * 
    * <pre>
    * [ 1 4 5 3 ]
    * </pre>
    * 
    * defines a vector of size 4.
    * 
    * @param rtok
    * Tokenizer from which vector values are read. Number parsing should be
    * enabled.
    * @throws ImproperSizeException
    * if this vector has a fixed size which is incompatible with the input
    */
   @Override
   public void scan (ReaderTokenizer rtok) throws IOException {
      super.scan (rtok);
   }

}
