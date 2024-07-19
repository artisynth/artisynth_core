package artisynth.core.inverse;

import maspack.matrix.VectorNd;
import maspack.matrix.MatrixNd;

/**
 * Cost term proportional to the sum of the excitation values.
 *
 * -- used for l-2 regularization of delta-activations
 */
public class L2RegularizationTermForDeltas extends QPCostTermBase {

   public static final double defaultWeight = 1e-3;
   
   public L2RegularizationTermForDeltas () {
      this(defaultWeight);
   }
   
   public L2RegularizationTermForDeltas (double weight) {
      super(weight);
   }
   
   @Override
   public void getQP (MatrixNd Q, VectorNd p, double t0, double t1) {
      TrackingController controller = getController();
      if (controller != null) {
         VectorNd act = new VectorNd (controller.numExciters());
         controller.getExcitations (act, 0);
         double s = 1.0;
         if (controller.getNormalizeCostTerms()) {
            // divide by trace of I with size of Q
            s = 1/Math.sqrt(Q.rowSize());
         }         
         for (int i=0; i<Q.rowSize(); i++) {
            Q.add (i, i, s*myWeight);
            p.add (i, s*myWeight*act.get (i));
         }
      }
   }

}
