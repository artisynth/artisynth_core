/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import javax.swing.JFrame;

import maspack.render.GL.GL2.GL2Viewer;

//import javax.swing.JToolBar;

public class GLViewerFrame extends JFrame {
   private static final long serialVersionUID = 1L;
   protected GLViewer viewer;

   public GLViewer getViewer() {
      return viewer;
   }

   public GLViewerFrame (String name, int width, int height) {
      super (name);
      viewer = new GL2Viewer (width, height);
      getContentPane().add (viewer.getCanvas());
      pack();
   }

   public GLViewerFrame (GLViewer shareWith, String name, int width, int height) {
      this (shareWith, name, width, height, false);
   }

   public GLViewerFrame (GLViewer shareWith, String name, int width,
   int height, boolean undecorated) {
      super (name);
      viewer = new GL2Viewer ((GL2Viewer)shareWith, width, height);
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
