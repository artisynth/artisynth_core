package maspack.render.GL.GL3;

import javax.media.opengl.GL3;

/**
 * GL resource that can be drawn
 */
public interface GL3Drawable extends GL3Resource {
   
   /**
    * Draw the object
    */
   public void draw(GL3 gl);
}
