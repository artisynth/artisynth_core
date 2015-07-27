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

public class GLViewerPanel extends JPanel {
   private static final long serialVersionUID = 1L;
   protected GL2Viewer viewer;
   protected String myErrMsg;

   public GL2Viewer getViewer() {
      return viewer;
   }

   public GLViewerPanel (int width, int height) {
      super();
      setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
      viewer = new GL2Viewer (width, height);
      add (viewer.getCanvas());
   }
   
   public void dispose() {
      // clean-up
      remove(viewer.getCanvas());  // safely remove GL canvas
   }

   public void setSize (Dimension d) {
      super.setSize (d);
   }

   public void setSize (int w, int h) {
      super.setSize (w, h);
   }

   public void setBounds (int x, int y, int w, int h) {
      super.setBounds (x, y, w, h);
      viewer.getCanvas().setSize (w, h);
   }

   public void setBounds (Rectangle r) {
      super.setBounds (r);
   }

   public void setErrorMessage (String msg) {
      myErrMsg = msg;
   }

   public String getErrorMessage() {
      return myErrMsg;
   }

}
