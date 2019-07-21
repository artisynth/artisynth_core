package artisynth.core.modelbase;

import maspack.matrix.Point3d;
import maspack.matrix.VectorObject;

public interface VectorField<T extends VectorObject<T>> extends Field {

   public T getValue (Point3d pos);

   public VectorFieldPointFunction<T> createFieldFunction (boolean useRestPos);
}
