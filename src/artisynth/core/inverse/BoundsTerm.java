package artisynth.core.inverse;

import java.util.ArrayList;

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
   protected ArrayList<Double> myUpperBoundsList = null;
   protected ArrayList<Double> myLowerBoundsList = null;
   
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
   
   /**
    * Sets upper and lower bounds.
    * The bounds can be individually set or they can take on a default value otherwise.
    * Null values in the lists are interpreted as "use default"
    * @param lowerBound default lower limit
    * @param upperBound default upper limit
    * @param lowerBoundsList list of individual lower bounds
    * @param upperBoundsList list of individual upper bounds
    */
   public void setBounds(double lowerBound, double upperBound, 
      ArrayList<Double> lowerBoundsList, ArrayList<Double> upperBoundsList) {

      myLowerBoundsList = lowerBoundsList;
      myUpperBoundsList = upperBoundsList;
      setBounds(lowerBound, upperBound);
   }
   
   public void clearBounds() {
      useLowerBound = false;
      useUpperBound = false;
      myLowerBoundsList = null;
      myUpperBoundsList = null;
      setSize(mySize); //resizes and recomputes cost term
   }
   
   private void computeBounds() {
      int row = myRowSize;
      try {
         if (useLowerBound) {
            for (int i = 0; i < mySize; i++) {
               double bound;               
               if (myLowerBoundsList != null && myLowerBoundsList.get (i) != null) {
                  /* try to grab the individual value from the list */ 
                  bound = myLowerBoundsList.get (i);
               } else {
                  /* if the list is null or the Double at i is null, use default */
                  bound = myLowerBound;
               }

               H.set (row, i, 1.0); // x >= lb
               f.set (row++, bound);
            }
         }
         if (useUpperBound) {
            for (int i = 0; i < mySize; i++) {
               double bound;
               if (myUpperBoundsList != null && myUpperBoundsList.get (i) != null) {
                  bound = myUpperBoundsList.get (i);
               } else {
                  bound = myUpperBound;
               }

               H.set (row, i, -1.0); // -x >= -ub
               f.set (row++, -bound);
            }
         }
      } catch (IndexOutOfBoundsException e) {
         e.printStackTrace ();
         System.out.println ("Bounds list(s) not valid. Using default bounds instead.");
         myUpperBoundsList = null; // clear the invalid lists
         myLowerBoundsList = null;
         computeBounds(); // try again
      }
//      System.out.println (H);
//      System.out.println (f);
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
