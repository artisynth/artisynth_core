/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.matrix.VectorBase;

/**
 * A place-holder class used to indicate the size of a vector but contain no
 * specific data.
 */
public class VoidVector extends VectorBase {
   private int mySize;

   public VoidVector (int size) {
      mySize = size;
   }

   public int size() {
      return mySize;
   }

   public void set (int idx, double val) {
      // do nothing
   }

   public int set (double[] values, int idx) {
      return idx+mySize;
   }

   public double get (int idx) {
      return 0;
   }
   
   public VoidVector clone() {
      VoidVector vec = (VoidVector)super.clone();
      return vec;
   }
}
