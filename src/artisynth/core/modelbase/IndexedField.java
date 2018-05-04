package artisynth.core.modelbase;

public interface IndexedField<T> extends Field<T> {

   public T getValueByIndex (int idx);

   public T setValueByIndex (int idx, T value);

}

