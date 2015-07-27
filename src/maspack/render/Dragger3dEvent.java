/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.*;
import maspack.render.GL.GLViewer;

public class Dragger3dEvent {
   protected Dragger3d mySource;
   protected AffineTransform3dBase myTransform;
   protected GLViewer myViewer;
   protected AffineTransform3dBase myIncrementalTransform;
   // protected boolean myAbsolute;
   protected long myModifiersEx;

   // private Dragger3dEvent (
   // Dragger3d source, AffineTransform3dBase X, boolean absolute, int
   // modifiersEx)
   // {
   // myTransform = X;
   // myAbsolute = absolute;
   // myModifiersEx = modifiersEx;
   // mySource = source;
   // }

   public Dragger3dEvent (
      Dragger3d source, GLViewer viewer, AffineTransform3dBase X,
      AffineTransform3dBase Xinc, int modifiersEx) {
      myTransform = X;
      myViewer = viewer;
      myIncrementalTransform = Xinc;
      myModifiersEx = modifiersEx;
      mySource = source;
   }

   public Dragger3d getSource() {
      return mySource;
   }

   public GLViewer getViewer() {
      return myViewer;
   }

   public AffineTransform3dBase getTransform() {
      return myTransform;
   }

   public AffineTransform3dBase getIncrementalTransform() {
      return myIncrementalTransform;
   }

   public long getModifiersEx() {
      return myModifiersEx;
   }

   // public boolean isAbsolute()
   // {
   // return myAbsolute;
   // }
}
