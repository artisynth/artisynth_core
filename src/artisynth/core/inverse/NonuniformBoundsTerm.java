package artisynth.core.inverse;

import maspack.matrix.VectorNd;

/**
 * Term to create (in)equality constraints:
 * -set lower, upper or both bounds with convenient methods
 * -optionally add constraints using addRows(MatrixNd H, VectorNd f)
 * @author Teun, Ian
 */
public class NonuniformBoundsTerm extends LeastSquaresTermBase {

   protected VectorNd myLowerBound = new VectorNd ();
   protected VectorNd myUpperBound = new VectorNd (); 
   protected boolean useLowerBound = false;
   protected boolean useUpperBound = false;
   
   public void setLowerBound(VectorNd lowerBound) {
      myLowerBound.set (lowerBound);
      useLowerBound = true;
      computeBounds (); // recomputes cost term
   }
   
   public void setUpperBound(VectorNd upperBound) {
      myUpperBound.set(upperBound);
      useUpperBound = true;
      computeBounds (); // recomputes cost term
   }
   
   public void setBounds(VectorNd lowerBound, VectorNd upperBound) {
      myLowerBound.set (lowerBound);
      useLowerBound = true;
      myUpperBound.set(upperBound);
      useUpperBound = true;
      computeBounds (); // recomputes cost term
   }
   
   public void clearBounds() {
      useLowerBound = false;
      useUpperBound = false;
      computeBounds (); // recomputes cost term
   }
   
   private void computeBounds() {
      int row = myRowSize;
      if (useLowerBound) {
         for (int i = 0; i < mySize; i++) {
            H.set (row, i, 1.0); // x >= lb
            f.set (row++, myLowerBound.get (i));
         }
      }
      if (useUpperBound) {
         for (int i = 0; i < mySize; i++) {
            H.set (row, i, -1.0); // -x >= -ub
            f.set (row++, -myUpperBound.get (i));
         }
      }
   }
   
   @Override
   protected void compute (double t0, double t1) {
      // static term, nothing to compute
   }

   @Override
   public int getRowSize () {
      //myRowSize specifies the number of rows added manually
      //to this blocks of mySize are added for each bound
      int result = myRowSize;
      result += useLowerBound ? mySize : 0;
      result += useUpperBound ? mySize : 0;
      return result;
   }
  
   @Override
   public void setSize(int size) {
      super.setSize(size);
      myLowerBound.setSize (size);
      myUpperBound.setSize (size);
      computeBounds (); // recomputes cost term
   }
}
