package maspack.render.GL.GL3;

import com.jogamp.opengl.GL3;

public interface GL3SharedDrawable extends GL3Resource {

   /**
    * A version number corresponding to the necessary bound state
    * of the drawable's attributes.  If this version number has 
    * changed since last use, then attributes need to be re-bound.
    * 
    * @return bind version number
    */
   public int getBindVersion();
   
   /**
    * Binds attributes to the currently active GL object
    * @param gl active context
    */
   public void bind(GL3 gl);
   
   @Override
   public GL3SharedDrawable acquire(); 
   
   /**
    * Draw to the provided active context
    * @param gl active context
    */
   public void draw(GL3 gl);
   
   /**
    * Draw using instanced rendering to the active context
    * @param gl context handle
    * @param instanceCount number of instances
    */
   public void drawInstanced(GL3 gl, int instanceCount);
   
   /**
    * Compares to another drawable to see if same
    * @param other drawable to compare to
    * @return true if identical
    */
   public boolean equals(GL3SharedDrawable other);
   
   /**
    * Unique hashcode for identifying object
    * @return hashcode
    */
   @Override
   public int hashCode();
   
}
