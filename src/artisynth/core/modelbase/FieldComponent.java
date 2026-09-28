package artisynth.core.modelbase;

public interface FieldComponent extends ModelComponent {

   /**
    * Used internally by the system to clear cached values for subclasses that
    * support value caching.
    */
   public void clearCacheIfNecessary();

   /**
    * Performs whatever internal updates are required (such as populating data
    * caches) to ensure that subsequent queries are thread-safe, provided that
    * the defining values of the field remain unchanged. Must be called
    * before the field is queried from multiple threads.
    */
   default void updateForConcurrentAccess() {
   }
}
