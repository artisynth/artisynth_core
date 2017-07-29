/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL.GL2;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.IsRenderable;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewerFrame;

public class GLHSVShaderTest implements IsRenderable {
   
   public void prerender (RenderList list) {
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      new Vector3d (-2, -2, -2).updateBounds (pmin, pmax);
      new Vector3d ( 2,  2,  2).updateBounds (pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {
      
      if (renderer instanceof GL2Viewer) {
         GL2 gl = ((GL2Viewer)renderer).getGL2();

         long shader = GLHSVShader.getShaderProgram(gl);
         if (shader != -1) {
            gl.glUseProgramObjectARB ((int)shader);
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
