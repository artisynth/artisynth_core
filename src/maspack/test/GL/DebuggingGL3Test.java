package maspack.test.GL;

import java.io.File;

import com.jogamp.opengl.GL3;

import maspack.render.Renderer;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GL3.GL3Viewer;
import maspack.test.GL.MultiViewer.SimpleSelectable;
import maspack.util.PathFinder;

public class DebuggingGL3Test extends GL3Tester {
   
   @Override
   protected void addContent (maspack.test.GL.MultiViewer mv) {
    
      SimpleSelectable ss = new SimpleSelectableBase() {
         
         final File[] shaders = {
             new File(PathFinder.findSourceDir (GL3Viewer.class) + "/shaders/debug_vertex.glsl"),
             new File(PathFinder.findSourceDir (GL3Viewer.class) + "/shaders/debug_fragment.glsl")
         };
         
         int vao = 0;
         int vbo = 0;
         boolean initialized = false;
         
         private void init(GL3 gl) {
            int[] ibuff = new int[10];
            gl.glGenVertexArrays (1, ibuff, 0);
            vao = ibuff[0];
            gl.glGenBuffers (1, ibuff, 1);
            vbo = ibuff[1];
            
            fill(gl);
            
            initialized = true;
         }
         
         private void fill(GL3 gl) {
            gl.glBindVertexArray (vao);
            float[] triangle = {
                                -1.0f, -1.0f, 0.0f,
                                 1.0f, -1.0f, 0.0f,
                                 0.0f,  1.0f, 0.0f
            };
            java.nio.ByteBuffer vbuff = java.nio.ByteBuffer.allocateDirect (4*triangle.length);
            for (float f : triangle) {
               vbuff.putFloat (f);
            }
            vbuff.flip ();
            
            gl.glBindBuffer (GL3.GL_ARRAY_BUFFER, vbo);
            gl.glBufferData (GL3.GL_ARRAY_BUFFER, vbuff.limit (), vbuff, GL3.GL_STATIC_DRAW);
         }
         
         @Override
         public void render (Renderer renderer, int flags) {
            GL3Viewer viewer = (GL3Viewer)renderer;
            GL3 gl = viewer.getGL().getGL3();
            GLSupport.checkAndPrintGLError (gl);
            viewer.setShaderOverride (shaders, shaders);

            if (!initialized) {
               init(gl);
            }

            viewer.useProgram (gl, shaders);
            
            gl.glClearColor(0.0f, 0.0f, 0.4f, 0.0f);
            gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
            
            // re-bind vao
            fill(gl);
            
            gl.glBindVertexArray (vao);
            gl.glEnableVertexAttribArray (0);
            gl.glBindBuffer (GL3.GL_ARRAY_BUFFER, vbo);
            gl.glVertexAttribPointer (0, 3, GL3.GL_FLOAT, false, 0, 0);
            
            gl.glDrawArrays (GL3.GL_TRIANGLES, 0, 3);
            gl.glDisableVertexAttribArray (0);
            gl.glBindVertexArray (0);
            
            viewer.setShaderOverride (null, null);
            
            
            
            // throw new RuntimeException("Exit rendering");
            
         }
      };
      
      mv.addRenderable (ss);
      
   }
   
   public static void main (String[] args) {
      DebuggingGL3Test tester = new DebuggingGL3Test ();
      tester.run ();
   }
}

