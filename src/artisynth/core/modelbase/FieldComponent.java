package artisynth.core.modelbase;

import maspack.matrix.VectorObject;

public interface FieldComponent extends Field, ModelComponent {

   public void clearCacheIfNecessary();
}
