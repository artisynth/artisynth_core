/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import javax.media.opengl.GLAutoDrawable;

import maspack.render.IsSelectable;

/**
 * Base class for helper classes that enable GLViewer to perform selection
 * operations.
 */
public abstract class GLSelector {

   protected double myRectX;
   protected double myRectY;
   protected double myRectW;
   protected double myRectH;

   protected GLViewer myViewer;

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

   public abstract void beginSelectionForObject (IsSelectable s, int idx);

   public abstract void endSelectionForObject ();
}

