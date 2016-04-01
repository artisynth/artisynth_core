package maspack.render.GL.GL2;

import javax.media.opengl.GL2;

public interface GL2Drawable {

   /**
    * Use whatever internal resources that exist and draw to provided context
    * @param gl
    */
   public void draw(GL2 gl);
   
   /**
    * Release hold of any internal resources, prepare for disposal of object.  Ensure that
    * it is safe to call dispose multiple times.  To prevent memory leaks, this should be called
    * explicitly.  A previously disposed object cannot be used again.
    */
   public void dispose();
   
   /**
    * Checks if the drawable is still valid (can be drawn), usually checking if any
    * internal GL resources are still valid.
    * @return
    */
   public boolean isValid();
   
   /**
    * Whether or not the drawable has been disposed.  If true, it should not be used, instead being
    * discarded.
    * @return
    */
   public boolean isDisposed();
   
   /**
    * Checks if resources are invalid.  If they are (and drawable is not already
    * disposed) then will execute {@link #dispose()}, releasing any resources. 
    * @return true if invalid or disposed
    */
   public boolean disposeInvalid();
}
