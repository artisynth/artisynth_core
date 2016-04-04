package maspack.render.GL.GL3;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.GL.GLResourceBase;

public abstract class GL3ResourceBase extends GLResourceBase implements GL3Resource {

   protected GL3ResourceBase() {
      super();
   }
   
   @Override
   protected void internalDispose (GL gl) {
      dispose((GL3)gl);
   }
   
   public abstract void dispose(GL3 gl);
   
   @Override
   public boolean disposeInvalid (GL3 gl) {
      if (!isValid ()) {
         dispose(gl);
         return true;
      }
      return false;
   }
   
   @Override
   public GL3ResourceBase acquire () {
      return (GL3ResourceBase)super.acquire ();
   }

}
