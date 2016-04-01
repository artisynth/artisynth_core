package maspack.render.GL;

import javax.media.opengl.GL;

public interface GLGarbageSource {
   
   public void garbage(GL gl);
   
   public void dispose(GL gl);

}
