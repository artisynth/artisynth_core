/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GL2;

/**
 * Base class for helper classes that enable GLViewer to perform selection
 * operations.
 */
public abstract class GLSelector {

   double myRectX;
   double myRectY;
   double myRectW;
   double myRectH;

   GLViewer myViewer;

   public GLSelector (GLViewer viewer) {
      myViewer = viewer;
   }

   public void setRectangle (double x, double y, double w, double h) {
      myRectX = x;
      myRectY = y;
      myRectW = w;
      myRectH = h;
   }

   public abstract void setupSelection (GLAutoDrawable drawable);

   public abstract void processSelection (GLAutoDrawable drawable);

   public abstract void beginSelectionQuery (int idx);

   public abstract void endSelectionQuery ();

   public abstract void beginSelectionForObject (GLSelectable s, int idx);

   public abstract void endSelectionForObject ();
}

