package maspack.render.GL.GL2;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import maspack.render.GL.GLResourceBase;

public abstract class GL2ResourceBase extends GLResourceBase implements GL2Resource {

   protected GL2ResourceBase() {
      super();
   }
   
   @Override
   public abstract void dispose(GL2 gl);
   
   @Override
   public void dispose (GL gl) {
      dispose((GL2)gl);
   }
   
   @Override
   public boolean disposeInvalid (GL2 gl) {
      if (!isValid ()) {
         dispose(gl);
         return true;
      }
      return false;
   }
   
   @Override
   public boolean disposeInvalid (GL gl) {
      return super.disposeInvalid ((GL2)gl);
   }
   
   @Override
   public boolean disposeUnreferenced (GL2 gl) {
      if (getReferenceCount () == 0) {
         dispose (gl);
         return true;
      }
      return false;
   }
   
   @Override
   public boolean disposeUnreferenced (GL gl) {
      return super.disposeUnreferenced ((GL2)gl);
   }
   
   @Override
   public boolean releaseDispose (GL2 gl) {
      long r = releaseAndCount();
      if (r == 0) {
         dispose (gl);
         return true;
      }
      return false;
   }
   
   @Override
   public boolean releaseDispose (GL gl) {
      return super.releaseDispose ((GL2)gl);
   }
   
   @Override
   public GL2ResourceBase acquire () {
      return (GL2ResourceBase)super.acquire ();
   }

}
