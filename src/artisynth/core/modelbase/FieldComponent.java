package artisynth.core.modelbase;

public interface FieldComponent extends ModelComponent {

   /**
    * Used internally by the system to clear cached values
    * for subclasses that support value caching.
    */
   public void clearCacheIfNecessary();
}
