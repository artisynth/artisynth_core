/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;

public abstract class LeastSquaresTermBase extends QPTermBase
   implements LeastSquaresTerm {
   
   protected int myRowSize;
   
   protected MatrixNd H = new MatrixNd(); //left hand side
   protected VectorNd f = new VectorNd(); //right hand side
  
   public static final double defaultWeight = 1;
   
   public static final boolean DEFAULT_ENABLED = true;
   private boolean enabled = DEFAULT_ENABLED;

   public static PropertyList myProps =
      new PropertyList (LeastSquaresTermBase.class, QPTermBase.class);

   static {
      myProps.add ("enabled isEnabled setEnabled", "enable this constraint", DEFAULT_ENABLED);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public LeastSquaresTermBase() {
      this(defaultWeight);
   }
   
   public LeastSquaresTermBase(double weight) {
      myWeight = weight;
   }

   /**
    * Appends the provided rows to the cost term
    * @param H left hand side
    * @param f right hand side
    */
   public void addRows (MatrixNd H, VectorNd f) {
      if (H.colSize() == mySize && H.rowSize() == f.size()) {
         int oldRowSize = myRowSize;
         myRowSize += H.rowSize();
         setSize (mySize); //allocates extra space required to store the term
         this.H.setSubMatrix (oldRowSize,0,H);
         this.f.setSubVector (oldRowSize,f);
      } else {
         throw new IllegalArgumentException("Wrong argument sizes");
      }
   }
   
   public void getQP(MatrixNd Q, VectorNd P, double t0, double t1) {
      compute(t0,t1);
      this.Q.mulTransposeLeft (H,H);
      this.P.mulTranspose (H,f);
      this.P.negate ();
      Q.add (this.Q);
      P.add (this.P);
   }
   
   /**
    * Gets the current least squares term and returns the index of the last row
    * @param H container to store the left hand side
    * @param f container to store the right hand side
    * @param rowoff start from this row
    * @param t0
    * @param t1
    */
   public int getTerm (MatrixNd H, VectorNd f, int rowoff, double t0, double t1) {
      compute(t0,t1);
      H.setSubMatrix(rowoff, 0, this.H);
      f.setSubVector(rowoff, this.f);
      return rowoff+getRowSize();
   }
   
   @Override
   public void setSize(int size) {
      super.setSize (size);
      int rowSize = getRowSize();
      H.setSize (rowSize,size);
      f.setSize (rowSize);
   }
   

   public boolean isEnabled () {
      return enabled;
   }

   public void setEnabled (boolean enabled) {
      this.enabled = enabled;
   }
}
