/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;


/**
 * Event class returned whenever the Renderer executes a render operation.
 */
public class RendererEvent {
   private Renderer myRenderer;

   public RendererEvent (Renderer renderer) {
      myRenderer = renderer;
   }

   public Renderer getRenderer() {
      return myRenderer;
   }
}
