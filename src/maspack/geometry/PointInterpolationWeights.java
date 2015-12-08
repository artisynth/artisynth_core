package maspack.geometry;

import java.util.Collection;
import maspack.matrix.*;

/**
 * Computes normalized weights for point-based interpolation given a reference
 * point and a set of support points.
 */
public interface PointInterpolationWeights {

   /**
    * Given a reference point <code>p</code>, computes and returns a set of
    * weights <code>w_i </code>that can be used for interpolation of values
    * associated with a set of support points <code>p_i</code>. The weights are
    * normalized so that they sum to unity. The weights should also be computed
    * such that
    * <pre>
    * p = sum w_i p_i
    * </pre>
    * If the support points do not have sufficient coverage, the last
    * condition may not be possible. In that case, the method should return false.
    */
   public boolean compute (
      VectorNd weights, Vector3d p, Collection<Vector3d> support);
}
