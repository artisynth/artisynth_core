package artisynth.core.modelbase;

import maspack.matrix.VectorObject;

public interface IndexedField<T extends VectorObject<T>> extends VectorField<T> {

   public T getValueByIndex (int idx);

   public void setValueByIndex (int idx, T value);

   public void clearIndexedValues();

   public void ensureIndexedCapacity (int cap);

}

