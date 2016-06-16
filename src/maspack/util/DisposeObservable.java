package maspack.util;

/**
 * Simple interface to allow tracking of disposed resources without
 * holding a reference to them.  This allows for garbage collection
 * and detection.
 * @author Antonio
 *
 */
public interface DisposeObservable {
   
   /**
    * Whether or not the current object is disposed
    * @return dispose status
    */
   public boolean isDisposed();
   
   /**
    * Return an "observer" object that tracks the dispose status
    * of this object.
    */
   public DisposeObserver getDisposeObserver();
   
}
