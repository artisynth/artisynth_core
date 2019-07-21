package artisynth.core.modelbase;

import maspack.matrix.VectorObject;

public interface NodalField<T extends VectorObject<T>> extends VectorField<T> {

   public T getValue (int nodeNum);

   public T getValue (int[] nodeNums, double[] weights);

}
