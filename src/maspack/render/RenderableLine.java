/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

public interface RenderableLine extends Renderable {
   public float[] getRenderCoords0();

   public float[] getRenderCoords1();

   /** 
    * Returns the rgb components (in the range [0,1]) of a color that overrides
    * the default color indicated by the render props.  If there is no
    * overriding color, then null should be returned.
    */
   public float[] getRenderColor();

   public boolean isSelected();
}
