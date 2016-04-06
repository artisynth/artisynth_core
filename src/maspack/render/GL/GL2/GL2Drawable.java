package maspack.render.GL.GL2;

import javax.media.opengl.GL2;

public interface GL2Drawable extends GL2Resource {

   /**
    * Use whatever internal resources that exist and draw to provided context
    * @param gl
    */
   public void draw(GL2 gl);
   
}
