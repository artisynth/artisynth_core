package maspack.render.GL;

/**
 * Holds a reference to a resource so that it will
 * not be destroyed before the resource has a chance
 * to be acquired by something else.  Make sure the
 * object to be held is valid throughout the construction
 * of this box.
 * @author Antonio
 *
 */
public class GLResourceBox<S extends GLResource> {
   
   S item;
   
   public GLResourceBox(S item) {
      item.acquire ();
      this.item = item;
   }
   
   /**
    * Gets the held item.  Whatever uses it is
    * still responsible for acquiring a reference
    * to the item.
    * @return the item
    */
   public S get() {
      return item;
   }
   
   /**
    * Increments the reference count before
    * returning the object, for convenience.
    * @return the item
    */
   public S getAcquired() {
      item.acquire ();
      return item;
   }
   
   /**
    * Release the item and throw away the box
    */
   public void dispose() {
      if (item != null) {
         item.release (); // release hold
         item = null;
      }
   }
   
   @Override
   protected void finalize () throws Throwable {
      dispose();
   }

}
