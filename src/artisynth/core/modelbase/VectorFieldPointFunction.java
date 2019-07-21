package artisynth.core.modelbase;

import maspack.matrix.VectorObject;

public interface VectorFieldPointFunction<T extends VectorObject<T>> 
   extends FieldPointFunction {

   T eval (FieldPoint def);
}
