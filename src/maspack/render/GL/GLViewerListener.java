/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import javax.media.opengl.GLAutoDrawable;

/**
 * Callback for GLViewer events
 */
public interface GLViewerListener {
   
   /**
    * Before a init is called
    */
   public void preinit(GLViewerEvent e, GLAutoDrawable drawable);
   
   /**
    * After an init is called
    */
   public void postinit(GLViewerEvent e, GLAutoDrawable drawable);
   
   /**
    * Before a dispose is called
    */
   public void predispose(GLViewerEvent e, GLAutoDrawable drawable);
   
   /**
    * After a dispose is called
    */
   public void postdispose(GLViewerEvent e, GLAutoDrawable drawable);
   
   /**
    * Before a display occurs
    */
   public void predisplay(GLViewerEvent e, GLAutoDrawable drawable);
   
   /**
    * A display occurred on the viewer.
    */
   public void postdisplay(GLViewerEvent e, GLAutoDrawable drawable);
   
   /**
    * Before a display occurs
    */
   public void prereshape(GLViewerEvent e, GLAutoDrawable drawable, int x, int y, int width, int height);
   
   /**
    * A display occurred on the viewer.
    */
   public void postreshape(GLViewerEvent e, GLAutoDrawable drawable, int x, int y, int width, int height);
   
}
