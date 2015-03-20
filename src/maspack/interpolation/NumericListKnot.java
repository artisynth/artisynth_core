/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.interpolation;

import maspack.matrix.*;

/**
 * A vector and associated scalar value t that forms a single knot in a numeric
 * list.
 */
public class NumericListKnot {
   // Keep our own links around because it makes things a bit easier ...

   protected NumericListKnot prev;
   protected NumericListKnot next;

   /**
    * Vector value.
    */
   public VectorNd v;

   /**
    * Scalar value.
    */
   public double t;

   /**
    * Back-pointer to the list associated with this knot. Used to check to see
    * if the knot is a valid member of a particluar list.
    */
   protected NumericList myList = null;

   /**
    * Creates a new NumericListKnot with a vector of a specified size.
    * 
    * @param vsize
    * size of the vector
    */
   public NumericListKnot (int vsize) {
      v = new VectorNd (vsize);
      myList = null;
   }

   /**
    * Creates a new NumericListKnot which is a copy of an existing one.
    * 
    * @param knot
    * knot to be copied
    */
   public NumericListKnot (NumericListKnot knot) {
      v = new VectorNd (knot.v);
      t = knot.t;
      myList = null;
   }

   public NumericListKnot getNext() {
      return next;
   }
   
   public NumericListKnot getPrev() {
      return prev;
   }
   
}
