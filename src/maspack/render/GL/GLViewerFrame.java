/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import javax.swing.JFrame;

import maspack.render.GL.GL2.GL2Viewer;
import maspack.render.GL.GL3.GL3Viewer;

//import javax.swing.JToolBar;

public class GLViewerFrame extends JFrame {
   private static final long serialVersionUID = 1L;
   protected GLViewer viewer;
   
   protected static GLViewer.Version defaultVersion = GLViewer.Version.GL2;

   public GLViewer getViewer() {
      return viewer;
   }

   public GLViewerFrame (String name, int width, int height) {
      this (name, width, height, defaultVersion);
   }

   public GLViewerFrame (
      String name, int width, int height, GLViewer.Version vers) {
      super (name);
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
      getContentPane().add (viewer.getCanvas());
      pack();
   }

   public GLViewerFrame (GLViewer shareWith, String name, int width, int height) {
      this (shareWith, name, width, height, false);
   }

   public GLViewerFrame (GLViewer shareWith, String name, int width,
   int height, boolean undecorated) {
      super (name);
      if (shareWith instanceof GL2Viewer) {
         viewer = new GL2Viewer ((GL2Viewer)shareWith, width, height);
      }
      else if (shareWith instanceof GL3Viewer) {
         viewer = new GL3Viewer ((GL3Viewer)shareWith, width, height);
      }
      else {
         throw new IllegalArgumentException (
            "Unknown GLViewer type: " + shareWith.getClass());
      }
      setUndecorated (undecorated);
      getContentPane().add (viewer.getCanvas());
      pack();
   }

   public void addRenderable (GLRenderable r) {
      viewer.addRenderable(r);
   }

   public void removeRenderable (GLRenderable r) {
      viewer.removeRenderable(r);
   }

   public void clearRenderables() {
      viewer.clearRenderables();
   }

}
