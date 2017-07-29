package maspack.render.GL;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JFrame;

public class JOGLTest implements GLEventListener{
   @Override
   public void display(GLAutoDrawable drawable) {
      displayGL2 (drawable);
   }

   void displayGL2 (GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();

      gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (-1000, 0, 0);
      gl.glVertex3d (1000, 0, 0);
      gl.glEnd ();
      // draw things here
      
      gl.glFinish();

      // method body
   }

   void displayGL3 (GLAutoDrawable drawable) {
      GL3 gl = drawable.getGL().getGL3();

      gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

      // gl.glBegin (GL2.GL_LINES);
      // gl.glVertex3d (-1000, 0, 0);
      // gl.glVertex3d (1000, 0, 0);
      // gl.glEnd ();
      // draw things here
      
      gl.glFinish();
      // method body
   }

   @Override
   public void dispose(GLAutoDrawable arg0) {
      //method body
   }
   @Override
   public void init(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();

      gl.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
      // method body
   }
   @Override
   public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
      int arg4) {
      // method body
   }
   public static void main(String[] args) {
      //getting the capabilities object of GL2 profile
      final GLProfile profile = GLProfile.get(GLProfile.GL2);
      GLCapabilities capabilities = new GLCapabilities(profile);
      // The canvas 
      final GLJPanel glcanvas = new GLJPanel(capabilities);
      JOGLTest b = new JOGLTest();
      glcanvas.addGLEventListener(b);
      glcanvas.setSize(400, 400);
      //creating frame
      final JFrame frame = new JFrame (" Basic Frame");
      //adding canvas to it
      frame.getContentPane().add(glcanvas);
      frame.setSize(frame.getContentPane().getPreferredSize());
      frame.setVisible(true);
   }//end of main
}//end of classimport
