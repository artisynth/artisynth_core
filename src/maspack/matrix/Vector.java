/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.util.Clonable;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * General interface for vectors. It provides methods for setting and getting
 * the elements of a vector, and to do various non-modifying queries such as
 * finding out its size, computing it's norms, or comparing it with other
 * vectors. There is also a method {@link #setSize setSize} for resizing, which
 * can be used unless the vector size is fixed (which can be determined using
 * {@link #isFixedSize isFixedSize}).
 * 
 * <p>
 * This base class does not publicly support vector operations such as addition
 * or scaling. The reason for this is that specific implementations may have a
 * specialized structure which could be compromised by arbitrary operations. For
 * instance, an implementation that represents only unit vectors, should not
 * allow an <code>add</code> operation.
 * 
 * <p>
 * Of course, it is possible to corrupt any special implementation structure
 * using the <code>set</code> methods provided in this base class, but it was
 * felt that not including such routines would be overly restrictive. It is
 * therefore up to the user to safeguard implementation integrity against misuse
 * of the <code>set</code> methods.
 * 
 * <p>
 * Note that indices for vector elements are zero-based, so that the range of
 * valid indices for a vector of length n is <code>[0, ... , n-1]</code>.
 */
public interface Vector extends Clonable {
   /**
    * Returns the number of elements in this vector.
    * 
    * @return number of elements
    */
   public int size();

   /**
    * Gets a single element of this vector.
    * 
    * @param i
    * element index
    * @return element value
    */
   public double get (int i);

   /**
    * Copies the elements of this vector into an array of doubles. The
    * array length must be {@code >=} the size of the vector.
    * 
    * @param values
    * array into which values are copied
    */
   public void get (double[] values);

   /**
    * Copies the elements of this vector into another vector object.
    * 
    * @param v1
    * vector object into which values are copied
    * @throws ImproperSizeException
    * if the vector objects have different sizes
    */
   public void get (Vector v1) throws ImproperSizeException;

   /**
    * Sets a single element of this vector.
    * 
    * @param i
    * element index
    * @param value
    * element value
    */
   public void set (int i, double value);

   /**
    * Sets the elements of this vector from an array of doubles. If the vector
    * has a fixed size, then the array must have a length {@code >=} the
    * current vector size. Otherwise, the vector is resized to the array
    * length.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (double[] values);

   /**
    * Sets the elements of this vector from an array of doubles,
    * starting from a particular location.
    * 
    * @param values
    * array from which values are copied
    * @param idx starting point within values from which copying should begin
    * @return updated idx value
    */
   public int set (double[] values, int idx);
      
    /**
   }
    * Sets the values of this vector to those of another vector.
    * 
    * @param v
    * vector from which values are copied
    * @throws ImproperSizeException
    * vectors have different sizes and this vector cannot be resized
    * accordingly.
    */
   public void set (Vector v);

   /**
    * Sets the values of this vector to those of a matrix object. The matrix
    * must have either one column or one row; in the latter case, an attempt
    * will be made to declare this vector a column vector, and if this fails,
    * then an ImproperSizeException will be raised.
    * 
    * @param M
    * matrix from which values are copied
    * @throws ImproperSizeException
    * if this vector cannot be sized to match the dimensions of the matrix.
    */
   public void set (Matrix M);

   /**
    * Returns true if this vector is of fixed size. If this vector is not of
    * fixed size, then it can be resized dynamically, either explicitly using
    * {@link #setSize setSize}, or implicitly when used as a result for various
    * vector operations.
    * 
    * @return true if this vector is of fixed size
    * @see Vector#setSize
    */
   public boolean isFixedSize();

   /**
    * Sets the size of this vector. This operation is only supported if
    * {@link #isFixedSize isFixedSize} returns false.
    * 
    * @param n
    * new size
    * @throws UnsupportedOperationException
    * if this operation is not supported
    * @see Vector#isFixedSize
    */
   public void setSize (int n) throws UnsupportedOperationException;

   /**
    * Returns the 2 norm of this vector. This is the square root of the sum of
    * the squares of the elements.
    * 
    * @return vector 2 norm
    */
   public double norm();

   /**
    * Returns the square of the 2 norm of this vector. This is the sum of the
    * squares of the elements.
    * 
    * @return square of the 2 norm
    */
   public double normSquared();

   /**
    * Returns the 1 norm of this vector. This is the sum of the absolute values
    * of the elements.
    * 
    * @return vector 1 norm
    */
   public double oneNorm();

   /**
    * Returns the infinity norm of this vector. This is the maximum absolute
    * value over all elements.
    * 
    * @return vector infinity norm
    */
   public double infinityNorm();

   /**
    * Returns the maximum element value.
    * 
    * @return maximal element
    */
   public double maxElement();

   /**
    * Returns the minimum element value.
    * 
    * @return minimal element
    */
   public double minElement();

   /**
    * Returns the dot product of this vector and v1.
    * 
    * @param v1
    * right-hand vector
    * @return dot product
    * @throws ImproperSizeException
    * if this vector and v1 have different sizes
    */
   public double dot (Vector v1);

   /**
    * Returns true if the elements of this vector equal those of vector
    * <code>v1</code>within a prescribed tolerance <code>epsilon</code>.
    * If the vectors have different sizes, false is returned.
    * 
    * @param v1
    * vector to compare with
    * @param eps
    * comparison tolerance
    * @return false if the vectors are not equal within the specified tolerance,
    * or have different sizes
    */
   public boolean epsilonEquals (Vector v1, double eps);

   /**
    * Returns true if the elements of this vector exactly equal those of vector
    * <code>v1</code>. If the vectors have different sizes, false is
    * returned.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal or have different sizes
    */
   public boolean equals (Vector v1);

   /**
    * Writes the contents of this vector to a PrintWriter. Element values are
    * separated by spaces, and each element is formatted using a C
    * <code>printf</code> style as decribed by the parameter
    * <code>NumberFormat</code>.
    * 
    * @param pw
    * PrintWriter to write this vector to
    * @param fmt
    * numeric format
    */
   public void write (PrintWriter pw, NumberFormat fmt) throws IOException;

   /**
    * Writes the contents of this vector to a PrintWriter. Element values are
    * separated by spaces, and optionally surrounded by square brackets
    * <code>[ ]</code> if <code>withBrackets</code> is set true. Each
    * element is formatted using a C <code>printf</code> style as decribed by
    * the parameter <code>NumberFormat</code>.
    * 
    * @param pw
    * PrintWriter to write this vector to
    * @param fmt
    * numeric format
    * @param withBrackets
    * if true, causes the output to be surrounded by square brackets.
    */
   public void write (PrintWriter pw, NumberFormat fmt, boolean withBrackets)
      throws IOException;

   /**
    * Sets the contents of this vector to values read from a ReaderTokenizer.
    * The input should consist of a sequence of numbers, separated by white
    * space and optionally surrounded by square brackets <code>[ ]</code>.
    * 
    * <p>
    * If the input is not surrounded by square brackets, then the number of
    * values should equal the current {@link #size size} of this vector.
    * 
    * <p>
    * If the input is surrounded by square brackets, then all values up to the
    * closing bracket are read, and the resulting number of values should either
    * equal the current {@link #size size} of this vector, or this vector should
    * be resizeable to fit the input. For example,
    * 
    * <pre>
    * [ 1.2 4 5 3.1 ]
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
   public void scan (ReaderTokenizer rtok) throws IOException;

   /**
    * Returns a String representation of this vector, in which each element is
    * formatted using a C <code>printf</code> style format string. The
    * exact format for this string is described in the documentation for
    * {@link maspack.util.NumberFormat#set(String)}{NumberFormat.set(String)}.
    * 
    * @param fmtStr printf style format string
    * @return String representation of this vector
    * @see maspack.util.NumberFormat
    */
   public String toString (String fmtStr);
   
   /**
    * Returns a String representation of this vector, in which each element is
    * formatted using a C <code>printf</code> style format as decribed by the
    * parameter <code>NumberFormat</code>.
    * 
    * @param fmt
    * numeric format
    * @return String representation of this vector
    */
   public String toString (NumberFormat fmt);

}
