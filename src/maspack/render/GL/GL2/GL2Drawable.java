package maspack.render.GL.GL2;

import com.jogamp.opengl.GL2;

public interface GL2Drawable extends GL2Resource {

   /**
    * Use whatever internal resources that exist and draw to provided context
    */
   public void draw(GL2 gl);
   
}
