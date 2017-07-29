package maspack.render.GL;

import java.util.ArrayList;

import com.jogamp.opengl.GL;

public class GLGarbageBin<T extends GLResource> implements GLGarbageSource {

   ArrayList<T> trash;

   public GLGarbageBin () {
      trash = new ArrayList<>();
   }

   public synchronized void trash(T resource) {
      if (resource != null) {
         trash.add (resource);
      }
   }

   @Override
   public synchronized void garbage (GL gl) {
      for (T resource : trash) {
         if (!resource.isDisposed ()) {
            resource.dispose (gl);
         }
      }
      trash.clear ();
   }
   
   @Override
   public void dispose (GL gl) {
      garbage (gl);
   }

}
