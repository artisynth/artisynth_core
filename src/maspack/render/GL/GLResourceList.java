package maspack.render.GL;

import java.util.Iterator;
import java.util.LinkedList;

import com.jogamp.opengl.GL;

public class GLResourceList<T extends GLResource> implements GLGarbageSource {
   
   public LinkedList<T> resources;
   
   public GLResourceList() {
      resources = new LinkedList<>();
   }
   
   public void track(T resource) {
      synchronized (resources) {
         resources.add (resource);
      }
   }
   
   @Override
   /**
    * Removes and frees any unused GL resources tracked by this list.
    * "Unused" refers to resources that have no other references, and have
    * not been used since last collection.
    */
   public void garbage(GL gl) {
      synchronized(resources) {
         Iterator<T> it = resources.iterator ();
         while (it.hasNext ()) {
            T resource = it.next ();
            if (resource.isDisposed ()) {
               it.remove ();
            } else if (resource.disposeUnreferenced (gl)) {
               it.remove ();
            }
         }
      }
   }
   
   @Override
   public void dispose (GL gl) {
      synchronized(resources) {
         for (GLResource res : resources) {
            res.dispose (gl);
         }
         resources.clear ();
      }
   }

}
