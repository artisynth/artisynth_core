package maspack.render.GL;

import com.jogamp.opengl.GL;

import maspack.util.ReferenceCounted;

/**
 * Resource used by GL
 */
public interface GLResource extends ReferenceCounted {
   
   /**
    * Decrement reference count and dispose if no longer referenced, 
    * returning true if disposed
    */
   public boolean releaseDispose(GL gl);
      
   /**
    * Dispose if object is unreferenced
    * @param gl context
    * @return true if disposed
    */
   public boolean disposeUnreferenced(GL gl);
   
   /**
    * Discard data, regardless of existing references
    */
   public void dispose(GL gl);
   
   /**
    * Returns true if resource has been disposed
    */
   public boolean isDisposed();
   
   /**
    * Returns true if resource can be used (i.e. hasn't been disposed)
    * @return <code>true</code> if the resource can be used
    */
   public boolean isValid();
   
   /**
    * Dispose an invalid resource
    * @return false if still valid, true otherwise
    */
   public boolean disposeInvalid(GL gl);
   
   @Override
   public GLResource acquire ();
}
