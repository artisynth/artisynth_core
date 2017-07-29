/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import com.jogamp.opengl.GL;

import maspack.render.Renderer;


public interface GLRenderer extends Renderer {

   public GL getGL();
   
}
