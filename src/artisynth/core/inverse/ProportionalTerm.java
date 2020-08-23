package artisynth.core.inverse;

import maspack.matrix.*;

/**
 * Cost term that is proportional to the sum of the excitation values.  In
 * combination with a lower bound of zero, this term adds a cost on the
 * L1-norm.
 * 
 * @author Teun
 */
public class ProportionalTerm extends QPCostTermBase {

   public static final double defaultWeight = 1e-3;
   
   public ProportionalTerm() {
      this(defaultWeight);
   }
   
   public ProportionalTerm(double weight) {
      super(weight);
   }
   
   @Override
   public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
      TrackingController controller = getController();
      if (controller != null) {
         for (int i=0; i<p.size(); i++) {
            p.add (i,myWeight);
         }     
      }
   }

}
