package maspack.render.GL.GL2;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;

/**
 * Canvas for debugging display issues
 * @author antonio
 *
 */
public class GLCanvas2 extends GLCanvas {

   private static final long serialVersionUID = -1430720920614379656L;
   
   public GLCanvas2(GLCapabilities cap) {
      super(cap);
   }

   @Override
   public void destroy () {
      System.out.println ("Canvas is destroyed");
      super.destroy ();
   }
   
   @Override
   public void revalidate () {
      System.out.println ("Canvas is revalidated");
      super.revalidate ();
   }
   
   @Override
   public void invalidate () {
      System.out.println ("Canvas is invalidated");
      super.invalidate ();
   }
   
   @Override
   public void addNotify () {
      System.out.println ("Canvas is added");
      super.addNotify ();
   }
   
   @Override
   public void removeNotify () {
      System.out.println ("Canvas is removed");
      super.removeNotify ();
      super.destroy ();
   }
   
   @Override
   protected void finalize () throws Throwable {
      System.out.println ("Canvas finalized");
      super.finalize ();
   }
}
