/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
2 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

/**
 * An object which maintains render properties for use in directing how it
 * should be rendered.
 */
public interface HasRenderProps {
   /**
    * Returns the render properities for this object. If no render properties
    * are assigned, this routines returns null.
    * 
    * @return current render properties for this object
    */
   RenderProps getRenderProps();

   /**
    * Assigns a new set of render properties to this object. An argument of
    * <code>null</code> will remove render properties from this object.
    * 
    * @param props
    * new render properties for this object
    */
   void setRenderProps (RenderProps props);

   /**
    * Factory method to create render properties appropriate to this object.
    * 
    * @return new render properties for this object
    */
   RenderProps createRenderProps();
}
