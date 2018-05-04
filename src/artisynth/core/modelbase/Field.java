package artisynth.core.modelbase;

import maspack.matrix.Vector3d;

public interface Field<T> {

   public T getValue (Vector3d pos);

}
