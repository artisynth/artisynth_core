package maspack.util;

/**
 * Class for tracking the dispose status of an object.  The 
 * purpose is to not hold a reference to the original object,
 * so we can track whether or not it has been disposed
 * by garbage collection.
 * @author Antonio
 *
 */
public interface DisposeObserver {

   /**
    * Basic implementation.  
    */
   public static abstract class DisposeObserverImpl implements DisposeObserver {
      volatile boolean disposed;
      
      protected void dispose() {
         disposed = true;
      }
      
      @Override
      public boolean isDisposed () {
         return disposed;
      }
      
   }
   
   /**
    * @return dispose status of tracked object
    */
   public boolean isDisposed();
   
}
