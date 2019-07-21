package artisynth.core.modelbase;

import maspack.matrix.VectorObject;

public interface ElementField<T extends VectorObject<T>> extends VectorField<T> {

   public T getValue (int elemNum);

}
