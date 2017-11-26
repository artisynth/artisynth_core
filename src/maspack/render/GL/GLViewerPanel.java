/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import maspack.render.GL.GL2.GL2Viewer;
import maspack.render.GL.GL3.GL3Viewer;

public class GLViewerPanel extends JPanel {
   private static final long serialVersionUID = 1L;
   protected GLViewer viewer;
   protected String myErrMsg;

   protected static GLViewer.GLVersion defaultVersion = GLViewer.GLVersion.GL2;
   
   public GLViewer getViewer() {
      return viewer;
   }

   public GLViewerPanel (int width, int height) {
      this (width, height, defaultVersion);
   }
   
   public GLViewerPanel (int width, int height, GLViewer.GLVersion vers) {
      super();
      setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
      switch (vers) {
         case GL2: {
            viewer = new GL2Viewer (width, height); 
            break;
         }
         case GL3: {
            viewer = new GL3Viewer (width, height);                        
            break;
         }
         default:
            throw new IllegalArgumentException (
               "Unimplemented viewer type: " + vers);
      }
      add (viewer.getCanvas().getComponent());
   }
   
   public void dispose() {
      // clean-up
      viewer.dispose ();
   }

   public void setSize (Dimension d) {
      super.setSize (d);
      viewer.getCanvas().setSize (d);
   }

   public void setSize (int w, int h) {
      super.setSize (w, h);
      viewer.getCanvas().setSize (w, h);
   }

   public void setBounds (int x, int y, int w, int h) {
      super.setBounds (x, y, w, h);
      viewer.getCanvas().setSize (w, h);
   }

   public void setBounds (Rectangle r) {
      super.setBounds (r);
      viewer.getCanvas().setSize (r.width, r.height);
   }

   public void setErrorMessage (String msg) {
      myErrMsg = msg;
   }

   public String getErrorMessage() {
      return myErrMsg;
   }

}
