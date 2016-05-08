/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;


public interface Renderable extends IsSelectable, HasRenderProps {
   // /**
   // * Returns the render properities assigned to this object. If no
   // * render properties are assigned, this routines returns null.
   // *
   // * @return current render properties for this object
   // */
   // RenderProps getRenderProps();

   // /**
   // * Assigns a new set of render properties to this object. An argument
   // * of <code>null</code> will remove render properties from this object.
   // *
   // * @param props new render properties for this object
   // */
   // void setRenderProps(RenderProps props);

   // /**
   // * Creates and returns render properties appropriate to this
   // * object.
   // *
   // * @return new render properties for this object
   // */
   // RenderProps createRenderProps();

   // /**
   // * Saves the render state for this component and places any
   // * necessary sub-components on the render list by calling
   // * {@link maspack.render.RenderList#addIfVisible
   // * RenderList.addIfVisible}.
   // *
   // * @param list render list into which renderable objects are placed.
   // */
   // void prerender (RenderList list);

   // // /**
   // // * Renders this component using a specified GL interface. If the
   // // * argument <code>selecting</code> is true, then the rendering is being
   // // * done in selection mode and steps should be taken to identify
   // // * selectable parts.
   // // *
   // // * @param drawable JOGL interface to Open GL
   // // * @param props render properties for the object
   // // * @param selecting if true, indicates that rendering is being
   // // * done in selection mode
   // // */
   // // void render (GLRenderer renderer);

   // // void updateBounds (Vector3d pmin, Vector3d pmax);

   // void handleSelection (
   // LinkedList<IsRenderable> path, int[] namestack, int idx);

}
