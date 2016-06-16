/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.Vector3d;

/**
 * Specifies objects that can be rendered by a Viewer implementing
 * the {@link Renderable} interface.
 *  
 * @author lloyd
 */
public interface IsRenderable {
   
   /**
    * A rendering hint that suggests the object should be rendered with
    * transparency enabled
    */
   public static final int TRANSPARENT = 0x1;
   
   /**
    * A rendering hint that suggests the object should be rendered at the
    * end (in the foreground), after clip planes (e.g. 2D renderables)
    */
   public static final int TWO_DIMENSIONAL = 0x2;
   
//   /**
//    * A rendering hint that suggests the data often changes, useful when
//    * setting up rendering caches
//    */
//   public static final int DYNAMIC_DRAW = 0x4;
   
   /**
    * Called prior to rendering to allow this object to update the internal
    * state required for rendering (such as by caching rendering coordinates).
    * The object may add internal objects to the list of objects being
    * rendered, by calling
    * <p>
    *    list.addIfVisible (obj);
    * </p>
    * for each of the objects in question.
    * @param list list of objects to be rendered
    */
   public void prerender (RenderList list);

   /**
    * Render this object using the functionality of the supplied
    * {@link Renderer}.
    * 
    * @param renderer
    * provides the functionality used to perform the rendering.
    * @param flags flags that may be used to control different 
    * aspects of the rendering. Flags are defined in {@link Renderer}
    * and currently include
    * {@link Renderer#HIGHLIGHT} and
    * {@link Renderer#SORT_FACES}.
    */
   public void render (Renderer renderer, int flags);

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
   public void updateBounds (Vector3d pmin, Vector3d pmax);

   /**
    * Returns a bit code giving rendering hints about this renderable. Current
    * bit codes include {@link #TRANSPARENT TRANSPARENT} and
    * {@link #TWO_DIMENSIONAL TWO_DIMENSIONAL}.
    * 
    * @return bit code of rendering hints.
    */
   public int getRenderHints();
   
   // public long getUniqueId();
}
