package artisynth.core.inverse;

import artisynth.core.mechmodels.BodyConnector;
import maspack.matrix.VectorNd;

/**
 * @deprecated Class has been replaced by ConstraintForceTarget for greater
 * clarity.
 */
public class ForceTarget extends ConstraintForceTarget {

   public ForceTarget () {
      super();
   }

   public ForceTarget (BodyConnector con, VectorNd targetLambda) {
      super (con, targetLambda);
   }


}
