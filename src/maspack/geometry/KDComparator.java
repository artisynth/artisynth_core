/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;


public interface KDComparator<T>  {

   /**
    * Compares a(dim) to b(dim) 
    * 
    */
   public int compare(T a, T b, int dim);
   
   /**
    * Computes the distance between a and b
    * for nearest neighbour searches
    */
   public double distance(T a, T b);
   
   /**
    * Computes the axis-aligned distance
    * between a and b, ||a(dim)-b(dim)|| for
    * nearest neighbour searches
    */
   public double distance(T a, T b, int dim);
   
}
