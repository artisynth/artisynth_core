package artisynth.core.inverse;

import maspack.matrix.*;
import artisynth.core.modelbase.*;

/**
 * Interface for tracking controller target components. A target component is
 * created for each motion or force target that is added to the controller, and
 * is used to specify the desired target trajectory and associated weights.
 */
public interface TrackingTarget extends ModelComponent {

   /**
    * Queries the number of DOFs in the target.
    *
    * @return number of target DOFs
    */
   public int getTargetSize();

   /**
    * Queries the weight of this target.
    *
    * @return target weight
    * @see #setWeight
    */
   public double getWeight();

   /**
    * Sets the weight for this target. Target weights have a default value of
    * 1, and are used to prioritize this target in the tracking computation.
    * Targets with higher weights will be tracked more accurately; targets with
    * a weight of 0 will not be tracked at all.
    *
    * @param w new target weight
    * @see #getWeight
    */
   public void setWeight (double w);

   /**
    * Queries the subweights of this target.
    *
    * @return target subweights
    * @see #setSubWeights
    */
   public Vector getSubWeights();

   /**
    * Sets the subweights for this target. The number of subweights is equal to
    * {@link #getTargetSize}, and each has a default value of 1. Subweights are
    * multiplied by the target weight (returned by {@link #getWeight}) to yield
    * a net weight used to prioritize each degree of freedom in the tracking
    * computation, with higher weights resulting in more accurate tracking and
    * a weight of 0 resulting in no tracking at all.
    *
    * @param subw new target subweights
    * @see #getSubWeights
    */
   public void setSubWeights (VectorNd subw);

   /**
    * Returns the source component associated with this target.  This is the
    * component whose positions or forces are intended to track the
    * desired target.
    *
    * @return target source component
    */
   public ModelComponent getSourceComp();


}
