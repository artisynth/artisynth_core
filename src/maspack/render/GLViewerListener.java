/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

/**
 * Callback for GLViewer events
 */
public interface GLViewerListener {
   /**
    * A render occurred on the viewer.
    */
   public void renderOccurred (GLViewerEvent e);
}
