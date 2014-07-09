/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.GLLightingFunc;

import maspack.matrix.*;

public class GLHSVShaderTest implements GLRenderable {
   
   public void prerender (RenderList list) {
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      new Vector3d (-2, -2, -2).updateBounds (pmin, pmax);
      new Vector3d ( 2,  2,  2).updateBounds (pmin, pmax);
   }

   public void render (GLRenderer renderer, int flags) {
      GL2 gl = renderer.getGL2().getGL2();

      int shader = GLHSVShader.getShaderProgram(gl);
      if (shader != -1) {
         gl.glUseProgramObjectARB (shader);
      }

      gl.glDisable (GLLightingFunc.GL_LIGHTING);
      gl.glShadeModel (GL2.GL_SMOOTH);
      gl.glBegin (GL.GL_TRIANGLES);
      gl.glColor3d (0, 1, 1);
      gl.glVertex3d (0, 1, 0);
      gl.glColor3d (.6, 1, 1);
      gl.glVertex3d (1, 1, 0);
      gl.glVertex3d (0, 1, 1);
      gl.glVertex3d (0, 1, 0);
      gl.glEnd ();
      gl.glEnable (GLLightingFunc.GL_LIGHTING);

   }

   public int getRenderHints() {
      return 0;
   }

   public static void main (String[] args) {
      GLViewerFrame vframe = new GLViewerFrame ("GLHSVShader test", 640, 480);
      
      vframe.setVisible (true);
      GLViewer viewer = vframe.getViewer();
      viewer.setCenter (new Point3d (0, 1, 0));
      viewer.addRenderable (new GLHSVShaderTest());
   }
}
