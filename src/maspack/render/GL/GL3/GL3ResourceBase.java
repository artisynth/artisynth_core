package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

public abstract class GL3ResourceBase implements GL3Resource {

   private int refCount;
   
   protected GL3ResourceBase() {
      refCount = 0;
   }
   
   @Override
   public abstract void init(GL3 gl);
   
   @Override
   public void acquire() {
      ++refCount;
   }

   @Override
   public void release(GL3 gl) {
      --refCount;
      if (refCount <= 0) {
         dispose(gl);
      }
   }

   @Override
   public abstract void dispose(GL3 gl);

   @Override
   public abstract boolean isValid();

}
