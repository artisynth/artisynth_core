/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.HasCoordinateFrame;
import maspack.render.Renderer.AxisDrawStyle;

/**
 * A ModelComponent that implements Wrappable
 */
public interface WrapComponent extends 
   RenderableComponent, Wrappable, HasCoordinateFrame, HasSurfaceMesh {
   
   public double getAxisLength();

   public void setAxisLength (double len);

   public AxisDrawStyle getAxisDrawStyle();
   
}
