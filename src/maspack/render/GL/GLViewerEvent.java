/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.util.*;

/**
 * Event classreturned whenever the GLViewer executes a render operation.
 */
public class GLViewerEvent {
   private GLViewer myViewer;

   public GLViewerEvent (GLViewer viewer) {
      myViewer = viewer;
   }

   public GLViewer getViewer() {
      return myViewer;
   }
}
