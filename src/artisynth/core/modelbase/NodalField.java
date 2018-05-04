package artisynth.core.modelbase;

public interface NodalField<T> extends Field<T> {

   public T getValue (int nodeNum);

   public T getValue (int[] nodeNums, double[] weights);

}
