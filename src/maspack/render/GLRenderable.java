/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.Point3d;

public interface GLRenderable {
   /**
    * A rendering hint that suggests the object should be rendered with
    * transparency enabled
    */
   public static final int TRANSLUCENT = 0x1;
   
   /**
    * A rendering hint that suggests the object should be rendered at the
    * end (in the foreground), after clip planes (e.g. 2D renderables)
    */
   public static final int TWO_DIMENSIONAL = 0x2;
   
   /**
    * Prepare for rendering, and potentially add itself to a list to be drawn
    * by a GLRenderer.
    * @param list
    */
   public void prerender (RenderList list);

   /**
    * Render this object using Open GL via the JOGL.
    * 
    * @param renderer
    * renderer object which is used to perform the rendering. Provides pointers
    * to GL and GLU, along with helper functions.
    * @param flags supplies flags that may be used to control different 
    * aspects of the rendering. Flags are defined in {@link GLRenderer}
    * and currently include
    * {@link GLRenderer#SELECTED}, 
    * {@link GLRenderer#VERTEX_COLORING}, 
    * {@link GLRenderer#HSV_COLOR_INTERPOLATION}, 
    * {@link GLRenderer#SORT_FACES}, and
    * {@link GLRenderer#CLEAR_MESH_DISPLAY_LISTS}.
    */
   public void render (GLRenderer renderer, int flags);

   /**
    * Update the minimum and maximum points for this object. In an x-y-z
    * coordinate system with x directed to the right and y directed upwards, the
    * minimum and maximum points can be thought of as defining the
    * left-lower-far and right-upper-near corners of a bounding cube. This
    * method should only reduce the elements of the minimum point and increase
    * the elements of the maximum point, since it may be used as part of an
    * iteration to determine the bounding cube for several different objects.
    * 
    * @param pmin
    * minimum point
    * @param pmax
    * maximum point
    */
   public void updateBounds (Point3d pmin, Point3d pmax);

   /**
    * Returns a bit code giving rendering hints about this renderable. Current
    * bit codes include {@link #TRANSLUCENT TRANSLUCENT}.
    * 
    * @return bit code of rendering hints.
    */
   public int getRenderHints();
}
