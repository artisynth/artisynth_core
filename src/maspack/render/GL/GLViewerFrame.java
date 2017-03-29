/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import maspack.render.IsRenderable;
import maspack.render.GL.GL2.GL2Viewer;
import maspack.render.GL.GL3.GL3Viewer;

//import javax.swing.JToolBar;

public class GLViewerFrame extends JFrame {
   private static final long serialVersionUID = 1L;
   protected GLViewer viewer;
   
   protected static GLViewer.GLVersion defaultVersion = GLViewer.GLVersion.GL3;

   public GLViewer getViewer() {
      return viewer;
   }

   public GLViewerFrame (String name, int width, int height) {
      this (name, width, height, defaultVersion);
   }

   public GLViewerFrame (
      String name, int width, int height, GLViewer.GLVersion vers) {
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
      getContentPane().add (viewer.getCanvas().getComponent());
      pack();
      
      addWindowListener (new WindowAdapter() {
         @Override
         public void windowClosed (WindowEvent e) {
            viewer.dispose ();  // cleanup
         }
      });
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
      getContentPane().add (viewer.getCanvas().getComponent());
      pack();
      
      addWindowListener (new WindowAdapter() {
         @Override
         public void windowClosed (WindowEvent e) {
            viewer.dispose ();  // cleanup
         }
      });
   }

   public void addRenderable (IsRenderable r) {
      viewer.addRenderable(r);
   }

   public void removeRenderable (IsRenderable r) {
      viewer.removeRenderable(r);
   }

   public void clearRenderables() {
      viewer.clearRenderables();
   }

}
