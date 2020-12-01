package artisynth.core.mechmodels;

import java.util.ArrayList;

import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;

/**
 * Class that modifiers a solve or stiffness matrix and associated force vector.
 * This can be used to convert ArtiSynth stiffness matrices to alternate
 * forms that may be more suitable for different kinds of analysis.
 */
public interface SolveMatrixModifier {

   /**
    * Modifies in place a solve or stiffness matrix {@code K}, along with an
    * (optional) force vector {@code f} and component list {@code comps}.
    * If provided, {@code comps} lists the components associated with
    * each block entry in {@code K}, and will be modified if any
    * rows/columns in {@code K} are permuted or deleted.
    * If provided, {@code f} and {@code comps} should have
    * sizes equal to the row (or column) size of {@code K}.
    *
    * @param K stiffness matrix to transform
    * @param f if non-null, force vector to transform
    * @param comps if non-null, components associated with each 
    * block entry in {@code K}.
    */
   public void modify (
      SparseBlockMatrix K, VectorNd f, ArrayList<DynamicComponent> comps);  
}