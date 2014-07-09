/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

/**
 * Defines an implicit linear transformation implemented by matrix-vector
 * multiplication
 */
public interface LinearTransformNd {
   /**
    * Applies this transformation to vector v1 and returns the result in vr.
    * 
    * @param v1
    * vector to transform
    * @param vr
    * vector in which result is returned
    */
   public void mul (VectorNd vr, VectorNd v1);

   /**
    * Number of rows in the matrix associated with this transformation. This
    * will equal the size of the output vector in {@link #mul mul}.
    * 
    * @return number of rows
    */
   public int rowSize();

   /**
    * Number of columns in the matrix associated with this transformation. This
    * will equal the size of the input vector in {@link #mul mul}.
    * 
    * @return number of columns
    */
   public int colSize();
}
