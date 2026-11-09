package artisynth.core.inverse;

import maspack.matrix.VectorNd;
import maspack.matrix.MatrixNd;

/**
 * Cost term that regularizes excitation changes, proportional to the squared
 * norm of {@code a - a0}, where {@code a0} are the excitations at the start
 * of the step.
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
         double s = controller.identityNormalization (Q.rowSize());
         // In incremental mode, the QP variables are the excitation changes
         // themselves; otherwise, they are the excitations a, and the cost
         // (1/2) |a - a0|^2 requires p -= a0.
         boolean incremental = controller.getComputeIncrementally();
         for (int i=0; i<Q.rowSize(); i++) {
            Q.add (i, i, s*myWeight);
            if (!incremental) {
               p.add (i, -s*myWeight*act.get (i));
            }
         }
      }
   }

}
