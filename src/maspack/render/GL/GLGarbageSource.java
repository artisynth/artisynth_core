package maspack.render.GL;

import com.jogamp.opengl.GL;

public interface GLGarbageSource {
   
   public void garbage(GL gl);
   
   public void dispose(GL gl);

}
