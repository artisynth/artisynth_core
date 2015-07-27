/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.*;
import maspack.render.GL.GLViewer;

public class DrawToolEvent {
   protected Dragger3d mySource;
   protected GLViewer myViewer;
   protected long myModifiersEx;

   public DrawToolEvent (
      Dragger3d source, GLViewer viewer, int modifiersEx) {
      myViewer = viewer;
      myModifiersEx = modifiersEx;
      mySource = source;
   }

   public Dragger3d getSource() {
      return mySource;
   }

   public GLViewer getViewer() {
      return myViewer;
   }

   public long getModifiersEx() {
      return myModifiersEx;
   }
}
