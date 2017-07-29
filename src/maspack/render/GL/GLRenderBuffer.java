package maspack.render.GL;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;

public class GLRenderBuffer extends GLResourceBase {

   private int id;
   private int width;
   private int height;
   private int nsamples;
   private int format;
   
   public GLRenderBuffer(int id) {
      this.id = id;
      width = 0;
      height = 0;
   }
   
   public void bind(GL gl) {
      gl.glBindRenderbuffer (GL.GL_RENDERBUFFER, id);
   }
   
   public void configure(GL gl, int width, int height, int format, int nsamples) {
      bind(gl);
      if (this.width != width || this.height != height || this.format != format || this.nsamples != nsamples) {
         // Create secondary FBO
         if (nsamples > 1 && (gl.isGL2GL3())) {
            GL2GL3 gl23 = (GL2GL3)gl;
            gl23.glRenderbufferStorageMultisample (GL.GL_RENDERBUFFER, nsamples, format, width, height);
         } else {
            gl.glRenderbufferStorage (GL.GL_RENDERBUFFER, format, width, height);   
         }                     
         this.width = width;
         this.format = format;
         this.height = height;
         this.nsamples = nsamples;
      }
   }
   
   public int getId() {
      return id;
   }
   
   public int getWidth() {
      return width;
   }
   
   public int getHeight() {
      return height;
   }
   
   public int getFormat() {
      return format;
   }
   
   public int getNumSamples() {
      return nsamples;
   }
   
   public boolean isMultisample() {
      return (nsamples != 1);
   }
   
   public void unbind(GL gl) {
      gl.glBindRenderbuffer (GL.GL_RENDERBUFFER, 0);
   }
   
   @Override
   public GLRenderBuffer acquire () {
      return (GLRenderBuffer)super.acquire ();
   }

   @Override
   public void dispose (GL gl) {
      if (id != -1) {
         gl.glDeleteRenderbuffers (1, new int[]{id}, 0);
         id = -1;
      }
   }

   @Override
   public boolean isDisposed () {
      return (id == -1);
   }
   
   public static GLRenderBuffer generate(GL gl) {
      int[] buff = new int[1];
      gl.glGenRenderbuffers (1, buff, 0);
      GLRenderBuffer out = new GLRenderBuffer (buff[0]);
      return out;      
   }
   
}
