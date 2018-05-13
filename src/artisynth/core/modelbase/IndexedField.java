package artisynth.core.modelbase;

public interface IndexedField<T> extends Field<T> {

   public T getValueByIndex (int idx);

   public void setValueByIndex (int idx, T value);

   public void clearIndexedValues();

   public void ensureIndexedCapacity (int cap);

}

