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
 * General interface for vectors of integers. The functionality is a subset of
 * that provided by {@link Vector}, which defines vectors of doubles.
 */
public interface Vectori extends Clonable {
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
   public int get (int i);

   /**
    * Copies the elements of this vector into an array of doubles. The
    * array length must be {@code >=} the size of the vector.
    * 
    * @param values
    * array into which values are copied
    */
   public void get (int[] values);

   /**
    * Sets a single element of this vector.
    * 
    * @param i
    * element index
    * @param value
    * element value
    */
   public void set (int i, int value);

   /**
    * Sets the elements of this vector from an array of doubles. If the
    * vector has a fixed size, then the array must have a length {@code >=}
    * the current vector size. Otherwise, the vector is resized to the
    * array length.
    * 
    * @param values
    * array from which values are copied
    */
   public void set (int[] values);

   /**
    * Sets the values of this vector to those of another vector.
    * 
    * @param v
    * vector from which values are copied
    * @throws ImproperSizeException
    * vectors have different sizes and this vector cannot be resized
    * accordingly.
    */
   public void set (Vectori v);

   /**
    * Returns true if this vector is of fixed size. If this vector is not of
    * fixed size, then it can be resized dynamically, either explicitly using
    * {@link #setSize setSize}, or implicitly when used as a result for various
    * vector operations.
    * 
    * @return true if this vector is of fixed size
    * @see Vectori#setSize
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
    * @see Vectori#isFixedSize
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
   public int oneNorm();

   /**
    * Returns the infinity norm of this vector. This is the maximum absolute
    * value over all elements.
    * 
    * @return vector infinity norm
    */
   public int infinityNorm();

   /**
    * Returns the maximum element value.
    * 
    * @return maximal element
    */
   public int maxElement();

   /**
    * Returns the minimum element value.
    * 
    * @return minimal element
    */
   public int minElement();

   /**
    * Returns true if the elements of this vector exactly equal those of vector
    * <code>v1</code>. If the vectors have different sizes, false is
    * returned.
    * 
    * @param v1
    * vector to compare with
    * @return false if the vectors are not equal or have different sizes
    */
   public boolean equals (Vectori v1);

   /**
    * Writes the contents of this vector to a PrintWriter. Element values are
    * separated by spaces, and each element is formatted using a C
    * <code>printf</code> style (for integers) as decribed by the parameter
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
    * separated by spaces, and optionally surrounded by square brackets <code>[
    * ]</code> if <code>withBrackets</code> is set true. Each element is
    * formatted using a C <code>printf</code> style (for integers) as decribed
    * by the parameter <code>NumberFormat</code>.
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
   public void scan (ReaderTokenizer rtok) throws IOException;

   /**
    * Returns a String representation of this vector, in which each element is
    * formatted using a C <code>printf</code> style format string (for
    * integers). The exact format for this string is described in the
    * documentation for {@link
    * maspack.util.NumberFormat#set(String)}{NumberFormat.set(String)}.  Note
    * that when called numerous times, {@link #toString(NumberFormat)
    * toString(NumberFormat)} will be more efficient because the {@link
    * maspack.util.NumberFormat NumberFormat} will not need to be recreated
    * each time from a specification string.
    * 
    * @param fmtStr printf style format string
    * @return String representation of this vector
    * @see maspack.util.NumberFormat
    */
   public String toString (String fmtStr);
   
   /**
    * Returns a String representation of this vector, in which each element is
    * formatted using a C <code>printf</code> style format (for integers) as
    * decribed by the parameter <code>NumberFormat</code>.
    * 
    * @param fmt
    * numeric format
    * @return String representation of this vector
    */
   public String toString (NumberFormat fmt);

}
