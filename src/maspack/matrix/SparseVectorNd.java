/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.util.Random;

import maspack.util.RandomGenerator;

/**
 * Implements sparse general vectors, along with their most commonly used
 * operations.
 * 
 * <p>
 * These vectors can be resized, either explicitly through a call to {@link
 * #setSize setSize}, or implicitly through operations that require the vector
 * size to be modified.
 */
public class SparseVectorNd extends VectorBase implements java.io.Serializable {
   private static final long serialVersionUID = 1L;
   SparseVectorCell elems;
   int size;

   /**
    * Creates a vector of a specific size, and initializes its elements to 0. It
    * is legal to create a vector with a size of zero.
    * 
    * @param size
    * size of the vector
    * @throws ImproperSizeException
    * if size is negative
    */
   public SparseVectorNd (int size) throws ImproperSizeException {
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
   public SparseVectorNd (int size, double[] values)
   throws ImproperSizeException {
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
   public SparseVectorNd (double[] values) {
      resetSize (values.length);
      set (values);
   }

   /**
    * Creates a vector whose size and elements are the same as an existing
    * Vector.
    * 
    * @param v
    * vector object to be copied.
    */
   public SparseVectorNd (Vector v) {
      resetSize (v.size());
      if (v instanceof SparseVectorNd) { // faster to use SparseVectorNd type
         set ((SparseVectorNd)v);
      }
      else {
         set (v);
      }
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
    * implementation, SparseVectorNd objects always have variable size, and so
    * this routine always returns false.
    * 
    * @return true if this vector is of fixed size
    * @see SparseVectorNd#setSize
    */
   public boolean isFixedSize() {
      return false;
   }

   /**
    * Sets the size of this vector.
    * 
    * <p>
    * If a vector is resized, then previous element values which are still
    * within the new vector dimension are preserved. Other (new) element values
    * are undefined.
    * 
    * @param newSize
    * new vector size
    */
   public void setSize (int newSize) throws ImproperSizeException {
      if (newSize < 0) {
         throw new ImproperSizeException ("Negative size");
      }
      resetSize (newSize);
   }

   SparseVectorCell prevEntry (int i) {
      SparseVectorCell prev = null;
      SparseVectorCell cell;
      for (cell = elems; cell != null; cell = cell.next) {
         if (cell.i >= i) {
            return prev;
         }
         prev = cell;
      }
      return prev;
   }

   public void addEntry (SparseVectorCell cell, SparseVectorCell prev) {
      if (prev == null) {
         cell.next = elems;
         elems = cell;
      }
      else {
         cell.next = prev.next;
         prev.next = cell;
      }
   }

   private void duplicateElements (SparseVectorCell cell, SparseVectorCell prev) {
      while (cell != null) {
         SparseVectorCell newCell = new SparseVectorCell (cell);
         addEntry (newCell, prev);
         prev = newCell;
         cell = cell.next;
      }
   }

   private void duplicateAndScaleElements (
      SparseVectorCell cell, SparseVectorCell prev, double s) {
      while (cell != null) {
         SparseVectorCell newCell = new SparseVectorCell (cell);
         newCell.value *= s;
         addEntry (newCell, prev);
         prev = newCell;
         cell = cell.next;
      }
   }

   private void scaleElements (SparseVectorCell cell, double s) {
      while (cell != null) {
         cell.value *= s;
         cell = cell.next;
      }
   }

   public void removeEntry (SparseVectorCell cell, SparseVectorCell prev) {
      if (prev == null) {
         elems = cell.next;
      }
      else {
         prev.next = cell.next;
      }
   }

   void resetSize (int newSize) throws ImproperSizeException {
      if (newSize < size) {
         SparseVectorCell prev = prevEntry (newSize);
         if (prev != null) {
            prev.next = null;
         }
         else {
            elems = null;
         }
      }
      this.size = newSize;
   }

   /**
    * Gets a single element of this vector.
    * 
    * @param i
    * element index
    * @return element value
    */
   public double get (int i) {
      SparseVectorCell cell;
      for (cell = elems; cell != null && cell.i <= i; cell = cell.next) {
         if (cell.i == i) {
            return cell.value;
         }
      }
      return 0;
   }
   
   public SparseVectorCell getCells() {
      return elems;
   }

   /**
    * {@inheritDoc}
    */
   public void get (double[] values) {
      if (values.length < size) {
         throw new IllegalArgumentException (
            "argument 'values' must have length >= "+size);
      }
      SparseVectorCell cell = elems;
      for (int i = 0; i < size; i++) {
         if (cell != null && i < cell.i) {
            values[i] = 0;
         }
         else {
            values[i] = cell.value;
            cell = cell.next;
         }
      }
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
      SparseVectorCell prev = prevEntry (i);
      SparseVectorCell cell = (prev == null ? elems : prev.next);
      if (cell != null && cell.i == i) { // then a cell already exists at this
                                          // spot
         if (value != 0) {
            cell.value = value;
         }
         else { // delete the cell
            removeEntry (cell, prev);
         }
      }
      else { // no cell exists at this spot
         if (value != 0) {
            cell = new SparseVectorCell (i, value);
            addEntry (cell, prev);
         }
      }
      // System.out.print ("after set:");
      // prev = null;
      // boolean bad = false;
      // for (cell=elems; cell!=null; cell=cell.next)
      // { System.out.print (" " + cell);
      // if (prev != null && prev.i == cell.i)
      // { bad = true;
      // }
      // prev = cell;
      // }
      // System.out.println ("");
      // if (bad)
      // { System.out.println ("BAD");
      // System.exit(1);
      // }
   }

   /**
    * {@inheritDoc}
    */
   public void set (double[] values) {
      if (values.length != size) {
         resetSize (values.length);
      }
      elems = null;
      SparseVectorCell prev = null;
      for (int i = 0; i < size; i++) {
         if (values[i] != 0) {
            SparseVectorCell cell = new SparseVectorCell (i, values[i]);
            addEntry (cell, prev);
            prev = cell;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public int set (double[] values, int idx) {
      elems = null;
      SparseVectorCell prev = null;
      for (int i = 0; i < size; i++) {
         if (values[idx] != 0) {
            SparseVectorCell cell = new SparseVectorCell (i, values[idx]);
            addEntry (cell, prev);
            prev = cell;
         }
         idx++;
      }
      return idx;
   }

   /**
    * Sets the size and values of this vector to those of v1.
    * 
    * @param v1
    * vector whose size and values are copied
    * @throws ImproperSizeException
    * if this vector needs resizing but is of fixed size
    */
   public void set (SparseVectorNd v1) {
      if (v1 != this) {
         this.size = v1.size;
         setZero();
         duplicateElements (v1.elems, null);
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
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public void add (SparseVectorNd v1, SparseVectorNd v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (size != v1.size) {
         resetSize (v1.size);
      }
      if (v1 == this) {
         add (v2);
      }
      else if (v2 == this) {
         add (v1);
      }
      else {
         setZero();
         SparseVectorCell cell1 = v1.elems;
         SparseVectorCell cell2 = v2.elems;
         SparseVectorCell tail = null;
         SparseVectorCell newCell = null;
         while (cell1 != null && cell2 != null) {
            if (cell1.i < cell2.i) {
               newCell = new SparseVectorCell (cell1);
               cell1 = cell1.next;
            }
            else if (cell1.i > cell2.i) {
               newCell = new SparseVectorCell (cell2);
               cell2 = cell2.next;
            }
            else {
               newCell =
                  new SparseVectorCell (cell1.i, cell1.value + cell2.value);
               cell1 = cell1.next;
               cell2 = cell2.next;
            }
            addEntry (newCell, tail);
            tail = newCell;
         }
         if (cell1 != null) {
            duplicateElements (cell1, tail);
         }
         else if (cell2 != null) {
            duplicateElements (cell2, tail);
         }
      }
   }

   /**
    * Adds this vector to v1 and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @throws ImproperSizeException
    * if this vector and v1 have different sizes
    */
   public void add (SparseVectorNd v1) throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (v1 == this) { // just iterate through and double everything
         SparseVectorCell cell;
         for (cell = elems; cell != null; cell = cell.next) {
            cell.value += cell.value;
         }
      }
      else {
         SparseVectorCell cell1 = elems;
         SparseVectorCell cell2 = v1.elems;
         SparseVectorCell tail = null;
         SparseVectorCell curCell = null;
         while (cell1 != null && cell2 != null) {
            if (cell1.i < cell2.i) { // leave cell where it is
               curCell = cell1;
               cell1 = cell1.next;
            }
            else if (cell1.i > cell2.i) {
               curCell = new SparseVectorCell (cell2);
               addEntry (curCell, tail);
               cell2 = cell2.next;
            }
            else {
               curCell = cell1;
               cell1.value += cell2.value;
               cell1 = cell1.next;
               cell2 = cell2.next;
            }
            tail = curCell;
         }
         if (cell2 != null) {
            duplicateElements (cell2, tail);
         }
      }
   }

   /**
    * Subtracts vector v1 from v2 and places the result in this vector. This
    * vector is resized if necessary.
    * 
    * @param v1
    * left-hand vector
    * @param v2
    * right-hand vector
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public void sub (SparseVectorNd v1, SparseVectorNd v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (size != v1.size) {
         resetSize (v1.size);
      }
      if (v1 == this) {
         sub (v2);
      }
      else if (v2 == this) {
         sub (v1);
         negate();
      }
      else {
         setZero();
         SparseVectorCell cell1 = v1.elems;
         SparseVectorCell cell2 = v2.elems;
         SparseVectorCell tail = null;
         SparseVectorCell newCell = null;
         while (cell1 != null && cell2 != null) {
            if (cell1.i < cell2.i) {
               newCell = new SparseVectorCell (cell1);
               cell1 = cell1.next;
            }
            else if (cell1.i > cell2.i) {
               newCell = new SparseVectorCell (cell2.i, -cell2.value);
               cell2 = cell2.next;
            }
            else {
               newCell =
                  new SparseVectorCell (cell1.i, cell1.value - cell2.value);
               cell1 = cell1.next;
               cell2 = cell2.next;
            }
            addEntry (newCell, tail);
            tail = newCell;
         }
         if (cell1 != null) {
            duplicateElements (cell1, tail);
         }
         else if (cell2 != null) {
            duplicateAndScaleElements (cell2, tail, -1);
         }
      }
   }

   /**
    * Subtracts v1 from this vector and places the result in this vector.
    * 
    * @param v1
    * right-hand vector
    * @throws ImproperSizeException
    * if this vector and v1 have different sizes
    */
   public void sub (SparseVectorNd v1) throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      if (v1 == this) { // just zero everything
         setZero();
      }
      else {
         SparseVectorCell cell1 = elems;
         SparseVectorCell cell2 = v1.elems;
         SparseVectorCell tail = null;
         SparseVectorCell curCell = null;
         while (cell1 != null && cell2 != null) {
            if (cell1.i < cell2.i) { // leave cell where it is
               curCell = cell1;
               cell1 = cell1.next;
            }
            else if (cell1.i > cell2.i) {
               curCell = new SparseVectorCell (cell2.i, -cell2.value);
               addEntry (curCell, tail);
               cell2 = cell2.next;
            }
            else {
               curCell = cell1;
               cell1.value -= cell2.value;
               cell1 = cell1.next;
               cell2 = cell2.next;
            }
            tail = curCell;
         }
         if (cell2 != null) {
            duplicateAndScaleElements (cell2, tail, -1);
         }
      }
   }

   /**
    * Sets this vector to the negative of v1. This vector is resized if
    * necessary.
    * 
    * @param v1
    * vector to negate
    * @throws ImproperSizeException
    * if this vector needs resizing but is of fixed size
    */
   public void negate (SparseVectorNd v1) {
      if (v1 == this) {
         negate();
      }
      else {
         setZero();
         this.size = v1.size;
         duplicateAndScaleElements (v1.elems, null, -1);
      }
   }

   /**
    * Negates this vector in place.
    */
   public void negate() {
      SparseVectorCell cell;
      for (cell = elems; cell != null; cell = cell.next) {
         cell.value = -cell.value;
      }
   }

   /**
    * Scales the elements of this vector by <code>s</code>.
    * 
    * @param s
    * scaling factor
    */
   public void scale (double s) {
      scaleElements (elems, s);
   }

   /**
    * Scales the elements of vector v1 by <code>s</code> and places the
    * results in this vector. This vector is resized if necessary.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled
    * @throws ImproperSizeException
    * if this vector needs resizing but is of fixed size
    */
   public void scale (double s, SparseVectorNd v1) {
      if (v1 == this) {
         scale (s);
      }
      else {
         setZero();
         this.size = v1.size;
         duplicateAndScaleElements (v1.elems, null, s);
      }
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
   public void interpolate (SparseVectorNd v1, double s, SparseVectorNd v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      combine (1 - s, v1, s, v2);
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
    * if this vector and v1 have different sizes
    */
   public void interpolate (double s, SparseVectorNd v1)
      throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      combine (1 - s, this, s, v1);
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
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public void scaledAdd (double s, SparseVectorNd v1, SparseVectorNd v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      size = v1.size;
      SparseVectorCell cell1 = v1.elems;
      SparseVectorCell cell2 = v2.elems;
      SparseVectorCell tail = null;
      SparseVectorCell curCell = null;
      if (v1 != this && v2 != this) {
         setZero();
      }
      while (cell1 != null && cell2 != null) {
         if (cell1.i < cell2.i) {
            if (v1 == this) {
               curCell = cell1;
               curCell.value *= s;
            }
            else {
               curCell = new SparseVectorCell (cell1.i, s * cell1.value);
               addEntry (curCell, tail);
            }
            cell1 = cell1.next;
         }
         else if (cell1.i > cell2.i) {
            if (v2 == this) {
               curCell = cell2;
            }
            else {
               curCell = new SparseVectorCell (cell2.i, cell2.value);
               addEntry (curCell, tail);
            }
            cell2 = cell2.next;
         }
         else {
            if (v1 == this) {
               curCell = cell1;
            }
            else if (v2 == this) {
               curCell = cell2;
            }
            else {
               curCell = new SparseVectorCell (cell1);
               addEntry (curCell, tail);
            }
            curCell.value = s * cell1.value + cell2.value;
            cell1 = cell1.next;
            cell2 = cell2.next;
         }
         tail = curCell;
      }
      if (cell1 != null) {
         if (v1 == this) {
            scaleElements (cell1, s);
         }
         else {
            duplicateAndScaleElements (cell1, tail, s);
         }
      }
      else if (cell2 != null) {
         if (v2 != this) {
            duplicateElements (cell2, tail);
         }
      }
   }

   /**
    * Computes <code>s v1</code> and adds the result to this vector.
    * 
    * @param s
    * scaling factor
    * @param v1
    * vector to be scaled and added
    * @throws ImproperSizeException
    * if this vector and v1 have different sizes
    */
   public void scaledAdd (double s, SparseVectorNd v1)
      throws ImproperSizeException {
      scaledAdd (s, v1, this);
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
    * @throws ImproperSizeException
    * if v1 and v2 have different sizes, or if this vector needs resizing but is
    * of fixed size
    */
   public void combine (
      double s1, SparseVectorNd v1, double s2, SparseVectorNd v2)
      throws ImproperSizeException {
      if (v1.size != v2.size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      size = v1.size;
      SparseVectorCell cell1 = v1.elems;
      SparseVectorCell cell2 = v2.elems;
      SparseVectorCell tail = null;
      SparseVectorCell curCell = null;
      if (v1 != this && v2 != this) {
         setZero();
      }
      while (cell1 != null && cell2 != null) {
         if (cell1.i < cell2.i) {
            if (v1 == this) {
               curCell = cell1;
               curCell.value *= s1;
            }
            else {
               curCell = new SparseVectorCell (cell1.i, s1 * cell1.value);
               addEntry (curCell, tail);
            }
            cell1 = cell1.next;
         }
         else if (cell1.i > cell2.i) {
            if (v2 == this) {
               curCell = cell2;
               curCell.value *= s2;
            }
            else {
               curCell = new SparseVectorCell (cell2.i, s2 * cell2.value);
               addEntry (curCell, tail);
            }
            cell2 = cell2.next;
         }
         else {
            if (v1 == this) {
               curCell = cell1;
            }
            else if (v2 == this) {
               curCell = cell2;
            }
            else {
               curCell = new SparseVectorCell (cell1);
               addEntry (curCell, tail);
            }
            curCell.value = s1 * cell1.value + s2 * cell2.value;
            cell1 = cell1.next;
            cell2 = cell2.next;
         }
         tail = curCell;
      }
      if (cell1 != null) {
         if (v1 == this) {
            scaleElements (cell1, s1);
         }
         else {
            duplicateAndScaleElements (cell1, tail, s1);
         }
      }
      else if (cell2 != null) {
         if (v2 == this) {
            scaleElements (cell2, s2);
         }
         else {
            duplicateAndScaleElements (cell2, tail, s2);
         }
      }
   }

   /**
    * Returns the 2 norm of this vector. This is the square root of the sum of
    * the squares of the elements.
    * 
    * @return vector 2 norm
    */
   public double norm() {
      double sumSqr = 0;
      SparseVectorCell cell;
      for (cell = elems; cell != null; cell = cell.next) {
         sumSqr += cell.value * cell.value;
      }
      return Math.sqrt (sumSqr);
   }

   /**
    * Returns the square of the 2 norm of this vector. This is the sum of the
    * squares of the elements.
    * 
    * @return square of the 2 norm
    */
   public double normSquared() {
      double sumSqr = 0;
      SparseVectorCell cell;
      for (cell = elems; cell != null; cell = cell.next) {
         sumSqr += cell.value * cell.value;
      }
      return sumSqr;
   }

   /**
    * Returns the maximum element value of this vector.
    * 
    * @return maximal element
    */
   public double maxElement() {
      double max = Double.NEGATIVE_INFINITY;
      SparseVectorCell cell;
      int nonZeroCnt = 0;
      for (cell = elems; cell != null; cell = cell.next) {
         if (cell.value > max) {
            max = cell.value;
         }
         nonZeroCnt++;
      }
      if (nonZeroCnt < size && max < 0) {
         max = 0;
      }
      return max;
   }

   /**
    * Returns the minimum element value of this vector.
    * 
    * @return minimal element
    */
   public double minElement() {

      double min = Double.POSITIVE_INFINITY;
      SparseVectorCell cell;
      int nonZeroCnt = 0;
      for (cell = elems; cell != null; cell = cell.next) {
         if (cell.value < min) {
            min = cell.value;
         }
         nonZeroCnt++;
      }
      if (nonZeroCnt < size && min > 0) {
         min = 0;
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
      double max = 0;
      SparseVectorCell cell;
      for (cell = elems; cell != null; cell = cell.next) {
         double abs = Math.abs (cell.value);
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
      SparseVectorCell cell;
      for (cell = elems; cell != null; cell = cell.next) {
         sum += Math.abs (cell.value);
      }
      return sum;
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
   public double dot (SparseVectorNd v1) throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      double sum = 0;
      SparseVectorCell cell1 = elems;
      SparseVectorCell cell2 = v1.elems;
      while (cell1 != null && cell2 != null) {
         if (cell1.i < cell2.i) {
            cell1 = cell1.next;
         }
         else if (cell1.i > cell2.i) {
            cell2 = cell2.next;
         }
         else {
            sum += cell1.value * cell2.value;
            cell1 = cell1.next;
            cell2 = cell2.next;
         }
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
   public double angle (SparseVectorNd v1) throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
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
    */
   public void normalize() {
      double norm = norm();
      scaleElements (elems, 1 / norm);
   }

   /**
    * Computes a unit vector in the direction of v1 and places the result in
    * this vector. This vector is resized if necessary.
    * 
    * @param v1
    * vector to normalize
    * @throws ImproperSizeException
    * if this vector needs resizing but is of fixed size
    */
   public void normalize (SparseVectorNd v1) {
      if (v1 == this) {
         normalize();
      }
      else {
         size = v1.size;
         double norm = v1.norm();
         setZero();
         duplicateAndScaleElements (v1.elems, null, 1 / norm);
      }
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
   public boolean epsilonEquals (SparseVectorNd v1, double eps)
      throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseVectorCell cell1 = elems;
      SparseVectorCell cell2 = v1.elems;
      while (cell1 != null && cell2 != null) {
         if (cell1.i < cell2.i) {
            if (!(abs (cell1.value) <= eps)) {
               return false;
            }
            cell1 = cell1.next;
         }
         else if (cell1.i > cell2.i) {
            if (!(abs (cell2.value) <= eps)) {
               return false;
            }
            cell2 = cell2.next;
         }
         else {
            if (!(abs (cell1.value - cell2.value) <= eps)) {
               return false;
            }
            cell1 = cell1.next;
            cell2 = cell2.next;
         }
      }
      while (cell1 != null) {
         if (!(abs (cell1.value) <= eps)) {
            return false;
         }
         cell1 = cell1.next;
      }
      while (cell2 != null) {
         if (!(abs (cell2.value) <= eps)) {
            return false;
         }
         cell2 = cell2.next;
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
   public boolean equals (SparseVectorNd v1) throws ImproperSizeException {
      if (v1.size != size) {
         throw new ImproperSizeException ("Incompatible dimensions");
      }
      SparseVectorCell cell1 = elems;
      SparseVectorCell cell2 = v1.elems;
      while (cell1 != null && cell2 != null) {
         if (cell1.i < cell2.i) {
            if (cell1.value != 0) {
               return false;
            }
            cell1 = cell1.next;
         }
         else if (cell1.i > cell2.i) {
            if (cell2.value != 0) {
               return false;
            }
            cell2 = cell2.next;
         }
         else {
            if (cell1.value != cell2.value) {
               return false;
            }
            cell1 = cell1.next;
            cell2 = cell2.next;
         }
      }
      while (cell1 != null) {
         if (cell1.value != 0) {
            return false;
         }
         cell1 = cell1.next;
      }
      while (cell2 != null) {
         if (cell2.value != 0) {
            return false;
         }
         cell2 = cell2.next;
      }
      return true;
   }

   /**
    * Sets the elements of this vector to zero.
    */
   public void setZero() {
      elems = null;
   }

   /**
    * Sets the elements of this vector to their absolute values.
    */
   public void absolute() {
      SparseVectorCell cell = elems;
      while (cell != null) {
         cell.value = Math.abs (cell.value);
         cell = cell.next;
      }
   }

   /**
    * Sets the elements of this vector to the absolute value of v1. This vector
    * is resized if necessary.
    * 
    * @param v1
    * vector to take the absolute value of
    * @throws ImproperSizeException
    * if this vector needs resizing but is of fixed size
    */
   public void absolute (SparseVectorNd v1) {
      if (v1 == this) {
         absolute();
      }
      else {
         size = v1.size;
         SparseVectorCell cell = elems;
         SparseVectorCell prev = null;
         while (cell != null) {
            SparseVectorCell newCell =
               new SparseVectorCell (cell.i, Math.abs (cell.value));
            addEntry (newCell, prev);
            prev = newCell;
            cell = cell.next;
         }
      }
   }

   // private void quickSort (double[] buf, int left, int right)
   // {
   // double pivot;
   // int lHold, rHold;

   // lHold = left;
   // rHold = right;
   // pivot = buf[left];
   // while (left < right)
   // {
   // while ((buf[right] <= pivot) && (left < right))
   // { right--;
   // }
   // if (left != right)
   // {
   // buf[left] = buf[right];
   // left++;
   // }
   // while ((buf[left] >= pivot) && (left < right))
   // { left++;
   // }
   // if (left != right)
   // {
   // buf[right] = buf[left];
   // right--;
   // }
   // }
   // buf[left] = pivot;
   // int pivotIndex = left;
   // left = lHold;
   // right = rHold;
   // if (left < pivotIndex)
   // { quickSort(buf, left, pivotIndex-1);
   // }
   // if (right > pivotIndex)
   // { quickSort(buf, pivotIndex+1, right);
   // }
   // }

   // /**
   // * Sorts the contents of this vector by element value,
   // * from largest to smallest value.
   // */
   // public void sort()
   // {
   // quickSort (buf, 0, size-1);
   // }

   // /**
   // * Sorts the contents of vector v1 by element value,
   // * from largest to smallest value, and places the result
   // * into this vector.
   // *
   // * @param v1 vector to sort
   // */
   // public void sort(SparseVectorNd v1)
   // {
   // set (v1);
   // quickSort (buf, 0, size-1);
   // }

   // /**
   // * Multiplies matrix M by the vector b and places the result
   // * in this vector. This vector is resized if necessary.
   // *
   // * @param M left-hand matrix
   // * @param b right-hand vector
   // * @throws ImproperSizeException if the size of b does not equal the
   // * number of columns of M, or if this vector needs resizing but is of
   // * fixed size
   // */
   // public void mul (MatrixNd M, SparseVectorNd b)
   // {
   // double[] res;
   // boolean resizeLater = false;

   // if (b.size != M.ncols)
   // { throw new ImproperSizeException ("Incompatible dimensions");
   // }
   // if (size != M.nrows)
   // { if (isFixedSize())
   // { throw new ImproperSizeException ("Incompatible dimensions");
   // }
   // else if (b != this)
   // { setSize (M.nrows);
   // }
   // else
   // { resizeLater = true;
   // }
   // }
   // if (b == this)
   // { res = new double[M.nrows];
   // }
   // else
   // { res = buf;
   // }

   // int idx = M.base;
   // for (int i=0; i<M.nrows; i++)
   // { double sum = 0;
   // for (int j=0; j<M.ncols; j++)
   // { sum += M.buf[idx+j]*b.buf[j];
   // }
   // idx += M.width;
   // res[i] = sum;
   // }

   // if (resizeLater)
   // { setSize (M.nrows);
   // }
   // if (res != buf)
   // { for (int i=0; i<size; i++)
   // { buf[i] = res[i];
   // }
   // }
   // }

   // /**
   // * Multiplies the transpose of matrix M by the vector b and places the
   // * result in this vector. Note that this is equivalent to
   // * pre-multiplying M by b. This vector is resized if necessary.
   // *
   // * @param M left-hand matrix
   // * @param b right-hand vector
   // * @throws ImproperSizeException if the size of b does not equal the
   // * number of rows of M, or if this vector needs resizing but is of
   // * fixed size
   // */

   // public void mulTranspose (MatrixNd M, SparseVectorNd b)
   // {
   // double[] res;
   // boolean resizeLater = false;

   // if (b.size != M.nrows)
   // { throw new ImproperSizeException ("Incompatible dimensions");
   // }
   // if (size != M.ncols)
   // { if (isFixedSize())
   // { throw new ImproperSizeException ("Incompatible dimensions");
   // }
   // else if (b != this)
   // { setSize (M.ncols);
   // }
   // else
   // { resizeLater = true;
   // }
   // }
   // if (b == this)
   // { res = new double[M.ncols];
   // }
   // else
   // { res = buf;
   // }

   // for (int j=0; j<M.ncols; j++)
   // { double sum = 0;
   // int idx = M.base+j;
   // for (int i=0; i<M.nrows; i++)
   // { sum += b.buf[i]*M.buf[idx];
   // idx += M.width;
   // }
   // res[j] = sum;
   // }

   // if (resizeLater)
   // { setSize (M.ncols);
   // }
   // if (res != buf)
   // { for (int i=0; i<size; i++)
   // { buf[i] = res[i];
   // }
   // }
   // }

   /**
    * {@inheritDoc}
    */
   public void setRandom() {
      setRandom (-0.5, 0.5, RandomGenerator.get());
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper) {
      setRandom (lower, upper, RandomGenerator.get());
   }

   /**
    * {@inheritDoc}
    */
   public void setRandom (double lower, double upper, Random generator) {
      setZero();
      double range = upper - lower;
      for (int k = 0; k < Math.max (1, size / 2); k++) {
         int i;
         do {
            i = generator.nextInt (size);
         }
         while (get (i) != 0);
         set (i, generator.nextDouble() * range + lower);
      }
   }
   
   public SparseVectorNd clone() {
      SparseVectorNd vec = (SparseVectorNd)super.clone();
      SparseVectorCell prev = null;
      for (SparseVectorCell cell=elems; cell!=null; cell=cell.next) {
         SparseVectorCell newc = new SparseVectorCell (cell);
         if (prev == null) {
            vec.elems = newc;
         }
         else {
            prev.next = newc;
         }
      }
      return vec;
   }
}
