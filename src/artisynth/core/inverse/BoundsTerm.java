package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;

/**
 * Term to create (in)equality constraints:
 * -set lower, upper or both bounds with convenient methods
 * -optionally add constraints using addRows(MatrixNd H, VectorNd f)
 * @author Teun
 */
public class BoundsTerm extends LeastSquaresTermBase {

   protected double myLowerBound;
   protected double myUpperBound; 
   protected boolean useLowerBound = false;
   protected boolean useUpperBound = false;
   
   public void setLowerBound(double lowerBound) {
      myLowerBound = lowerBound;
      useLowerBound = true;
      setSize(mySize); //resizes and recomputes cost term
   }
   
   public void setUpperBound(double upperBound) {
      myUpperBound = upperBound;
      useUpperBound = true;
      setSize(mySize); //resizes and recomputes cost term
   }
   
   public void setBounds(double lowerBound, double upperBound) {
      myLowerBound = lowerBound;
      useLowerBound = true;
      myUpperBound = upperBound;
      useUpperBound = true;
      setSize(mySize); //resizes and recomputes cost term
   }
   
   public void clearBounds() {
      useLowerBound = false;
      useUpperBound = false;
      setSize(mySize); //resizes and recomputes cost term
   }
   
   private void computeBounds() {
      int row = myRowSize;
      if (useLowerBound) {
         for (int i = 0; i < mySize; i++) {
            H.set (row, i, 1.0); // x >= lb
            f.set (row++, myLowerBound);
         }
      }
      if (useUpperBound) {
         for (int i = 0; i < mySize; i++) {
            H.set (row, i, -1.0); // -x >= -ub
            f.set (row++, -myUpperBound);
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
      computeBounds();
   }
}
