package maspack.render.GL.GL3;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.GL.GLResourceBase;

public abstract class GL3ResourceBase extends GLResourceBase implements GL3Resource {

   protected GL3ResourceBase() {
      super();
   }
   
   @Override
   public abstract void dispose(GL3 gl);
   
   @Override
   public void dispose (GL gl) {
      dispose((GL3)gl);
   }
   
   @Override
   public boolean disposeInvalid (GL3 gl) {
      if (!isValid ()) {
         dispose(gl);
         return true;
      }
      return false;
   }
   
   @Override
   public boolean disposeInvalid (GL gl) {
      return super.disposeInvalid ((GL3)gl);
   }
   
   @Override
   public boolean disposeUnreferenced (GL3 gl) {
      if (getReferenceCount () == 0) {
         dispose (gl);
         return true;
      }
      return false;
   }
   
   @Override
   public boolean disposeUnreferenced (GL gl) {
      return super.disposeUnreferenced ((GL3)gl);
   }
   
   @Override
   public boolean releaseDispose (GL3 gl) {
      long r = releaseAndCount();
      if (r == 0) {
         dispose (gl);
         return true;
      }
      return false;
   }
   
   @Override
   public boolean releaseDispose (GL gl) {
      return super.releaseDispose ((GL3)gl);
   }
   
   @Override
   public GL3ResourceBase acquire () {
      return (GL3ResourceBase)super.acquire ();
   }

}
