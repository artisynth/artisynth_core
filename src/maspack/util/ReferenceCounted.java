package maspack.util;

/**
 * Manually counts held references using a {@link #acquire()} and {@link #release()}
 * mechanism.
 * @author Antonio
 */
public interface ReferenceCounted {

   /**
    * Signal the resource that something is holding a reference to it.
    * @return a reference to the acquired object for convenience.
    */
   public ReferenceCounted acquire();
   
   /**
    * Signal the resource that something is holding a reference to it.
    * @return the current reference count
    */
   public long acquireAndCount();
   
   /**
    * Signal the resource that a previous hold is complete
    */
   public void release();
   
   /**
    * Signal the resource that a previous hold is complete.
    * @return the current reference count
    */
   public long releaseAndCount();
   
   /**
    * Number of active references
    * @return current reference count
    */
   public long getReferenceCount();
   
}
