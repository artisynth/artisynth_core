/**
 * Copyright c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;

import maspack.matrix.*;

/**
 * A viewer is a component that takes a collection of renderables and
 * renders them for a given viewpoint and set of lighting conditions.
 * It also implements the interface {@link Renderable}, which provides
 * the primary functionality by which renderables can render themselves.
 */
public interface Viewer extends Renderer {

   // rendering control

   /**
    * Adds a renderable to this viewer.

    * @param r renderable to add
    */
   public void addRenderable (IsRenderable r);

   /**
    * Removes a renderable from this viewer.
    *
    * @param r renderable to remove
    * @return <code>true</code> if <code>r</code> was present and removed
    */
   public boolean removeRenderable (IsRenderable r);

   /**
    * Removes all renderables from this viewer.
    */
   public void clearRenderables();

   /**
    * Sets the external render list for this viewer. This is a list of
    * renderables that is maintained independently of the set of renderables
    * maintained by {@link #addRenderable}, {@link #removeRenderable}, etc.
    *
    * <p> Generally, if an application specifies an external render list, it
    * should rebuild that list prior to each invocation of this viewer's {@link
    * #rerender(int)} method. This is typically dones as follows:
    * <pre>
    *    RenderList extList;
    *    ...
    *    extList.clear();
    *    for (every external renderable r) {
    *       extList.addIfVisible (r);
    *    }
    * </pre>
    * The call to {@link RenderList#addIfVisible(IsRenderable) addIfVisible()}
    * will place <code>r</code> onto the render list (if it is visible)
    * and also call its {@link IsRenderable#prerender prerender()} method.
    * Note in particular that <code>prerender()</code> will not be
    * called by the viewer.
    *
    * <p>External render lists are designed for situations where a set of
    * renderables is being rendered simultaneously by multiple viewers, and
    * consequently we don't want <code>prerender()</code> to be called multiple
    * times by different viewers.
    *
    * @param list new external render list, or <code>null</code> to clear
    * the external list
    */
   public void setExternalRenderList (RenderList list);

   /**
    * Returns the external render list for this viewer, or <code>null</code>
    * if the viewer does not have an external render list.
    * 
    * @return external render list
    */
   public RenderList getExternalRenderList();
   
   /**
    * Convenience methods that calls {@link #rerender(int)} with
    * <code>flags</code> = 0.
    */
   public void rerender();
   
   /**
    * Performs a rerender for this viewer. This does the following:
    *
    * <ol>
    * <li>Calls the {@link IsRenderable#prerender prerender()} method
    * for all renderables in the viewer and collects them into an
    * internal render list;</li> 
    *
    * <li>Requests a repaint for the viewer, during which it will call the
    * {@link IsRenderable#render render()} method for all renderables in
    * both the internal render list and the external render list (if
    * specified).</li>
    *
    * </ol>
    *
    * The <code>render()</code> methods are called with this viewer as the
    * renderer and the <code>flags</code> argument as the flags.
    * 
    * @param flags rendering flags
    */
   public void rerender(int flags);
   
   /**
    * Request a repaint operation for this viewer.
    */
   public void repaint();

   /**
    * Adds a render listener to this viewer. Render listeners are fired every
    * time the viewer performs a repaint.
    * 
    * @param l render listener to add.
    */
   public void addRenderListener (RenderListener l); 

   /**
    * Removes a render listener from this viewer.
    * @param l render listener to remove
    * @return <code>true</code> if <code>l</code> was present and removed.
    */
   public boolean removeRenderListener (RenderListener l);

   /**
    * Returns an array of all render listeners held by this viewer.
    * 
    * @return all render listeners
    */
   public RenderListener[] getRenderListeners();

   // selection control

   /**
    * Sets the highlighting style for this viewer. If the style is
    * specified as {@link Renderer.HighlightStyle#NONE},
    * then highlighting is deactivated.
    * Not all highlighting styles may be supported; if the specified
    * style is not supported this method does nothing and returns
    * <code>false</code>. 
    * @param style highlighting style to be set
    * @return <code>true</code> if the highlighting style is supported.
    * @see #hasHighlightStyle
    */
   public boolean setHighlightStyle (HighlightStyle style);

   /**
    * Returns <code>true</code> if this viewer supports the indicated
    * highlighting style.
    * 
    * @return <code>true</code> if <code>style</code> is supported
    */
   public boolean hasHighlightStyle(HighlightStyle style);

   /**
    * Enables or disables viewer-based selection. If this viewer does not
    * support selection (i.e., if {@link #hasSelection()} returns
    * <code>false</code>, then this method does nothing.
    * 
    * @param enable if <code>true</code>, enables selection
    */
   public void setSelectionEnabled (boolean enable);

   /**
    * Returns <code>true</code> if viewer-based selection is enabled.
    * 
    * @return <code>false</code> if selection is disabled or not supported.
    */
   public boolean isSelectionEnabled ();

   /**
    * Sets a selection filter for the viewer. This restricts which objects
    * are actually rendered when a selection render is performed, and therefore
    * restricts which objects can actually be selected. This allows selection
    * of objects that might otherwise be occluded within a scene.
    *
    * @param filter Selection filter to be applied
    */
   public void setSelectionFilter (ViewerSelectionFilter filter);

   /**
    * Returns the current selection filter for the viewer, if any.
    *
    * @return current selection filter, or <code>null</code> if there is none.
    */
   public ViewerSelectionFilter getSelectionFilter ();

   /**
    * Adds a specified light to this viewer and enables it.  In some cases, the
    * added light may exceed the number of lights supported by the renderer.
    * In that case, the light will still be added but will not be enabled.
    *
    * <p>Changes to the lighting caused by adding or removing lights, or by
    * changing the properties of existing lights, will take effect at the
    * beginning of the next repaint step.
    *
    * @param light light to add to the viewer
    * @return index of the added light
    */
    public int addLight (Light light);

    /**
     * Returns the number of lights in this viewer. This number will include
     * both predefined lights as well as those specifically defined by the
     * application.
     */
    public int numLights();

    /**
     * Get a specified light by index in this viewer. Lights will include
     * both predefined lights as well as those specifically defined by the
     * application.
     *
     * @param idx index of the light
     */
    public Light getLight (int idx);

    /**
     * Get the index of a specified light in this viewer.
     *
     * @param light light for which an index is sought 
     * @return Index of the light, or -1 if the viewer does not
     * contain the light.
     */
    public int getIndexOfLight (Light light);

//    /**
//     * Enables or disables a specified light within this viewer. If
//     * <code>enable</code> is <code>true</code>, the viewer is also updated
//     * to reflect any changes to the light's properties.
//     */
//    public boolean enableLight (Light light, boolean enable);

    /**
     * Removes the indicated light from this viewer.
     *
     * <p>Changes to the lighting caused by adding or removing lights, or by
     * changing the properties of existing lights, will take effect at the
     * beginning of the next repaint step.
     *
     * @param light light to be removed
     * @return <code>true</code> if the viewer contained the light
     * and it was removed
     */
    public boolean removeLight (Light light);

    /**
     * Removes the light with the specified index from this viewer.
     *
     * <p>Changes to the lighting caused by adding or removing lights, or by
     * changing the properties of existing lights, will take effect at the
     * beginning of the next repaint step.
     *
     * @param idx index of the light to be removed
     * @throws IndexOutOfBoundsException if the specified index was not in the
     * range 0 to {@link #numLights()}-1.
     */
    public void removeLight (int idx);   

   /**
    * Returns the background color for this viewer.
    * 
    * @return background color
    */
   public Color getBackgroundColor();

   /**
    * Returns the background color for this viewer. The color is returned as a
    * float array describing the color's RGBA values in the range [0,1]. The
    * returned array can be provided through the argument <code>rgba</code>.
    * Otherwise, if this argument is <code>null</code>, an array is allocated
    * internally.
    * 
    * @param rgba optional array for returning the values
    * @return rgba array describing the background color
    */
   public float[] getBackgroundColor(float[] rgba);

   /**
    * Sets the background color for this viewer.
    * 
    * @param color new background color
    */
   public void setBackgroundColor (Color color);

   /**
    * Sets the background color for this viewer.
    * 
    * @param rgba RGB (if length 3) or RGBA values (if length 4) for the
    * background color
    */
    public void setBackgroundColor (float[] rgba);

   // setting the viewpoint

   /**
    * Sets the eye-to-world transform for this viewer, using the canonical
    * parameters used by the GL <code>lookat</code> method. The eye-to-world
    * transform is the inverse of the view matrix.
    * 
    * @param eye
    * position of the eye, in world coordinates
    * @param center
    * point that the eye is looking at, in world coordinates
    * @param up
    * up direction, in world coordinates
    */
   public void setEyeToWorld (Point3d eye, Point3d center, Vector3d up);

   /**
    * Sets the eye-to-world transform for this viewer. This is the inverse of
    * the view matrix.
    * 
    * @param TEW new eye-to-world transform
    */
   public void setEyeToWorld (RigidTransform3d TEW);

   /**
    * Gets the eye-to-world transform for this viewer. This is
    * the inverse of the view matrix.
    * 
    * @param TEW returns the eye-to-world transform
    */
   public void getEyeToWorld (RigidTransform3d TEW);

   /**
    * Sets an axial (or axis-aligned) view. This is done by setting the 
    * rotational part of the eye-to-world transform to the axis-aligned
    * rotation <code>REW</code>, and then moving the eye position so that 
    * the center position lies along the new -z axis of the eye frame, 
    * while maintaining the current distance between the eye and the center. 
    * 
    * <p>The method also sets this viewer's `up'' vector to the y axis of 
    * <code>REW</code>, and saves <code>REW</code> itself as the current
    * axis-aligned view, which can be retrieved using {@link #getAxialView}.
    * 
    * @param REW axis-aligned rotational component for 
    * the eye-to-world transform
    * @see #getAxialView
    * @see #setUpVector
    * @see #getUpVector
    */
   public void setAxialView (AxisAlignedRotation REW);

   /**
    * Returns the current axis-aligned view. This is either the
    * one with which the viewer was initialized, or that
    * most recently set by {@link #setAxialView}.
    *  
    * @return current axis-aligned view.
    * @see #setAxialView
    */
   public AxisAlignedRotation getAxialView();

   /**
    * Sets the view matrix for this viewer. This is the transform from world to
    * eye coordinates.
    * 
    * @param TWE transform from world to eye coordinates.
    */
   public void setViewMatrix (RigidTransform3d TWE);

   /**
    * Returns the eye position. This corresponds
    * to the translational component of the eye-to-world transform.
    *
    * @return eye position, in world coordinates
    */
   public Point3d getEye();

   /**
    * Sets the center point for the viewer, and adjusts the eye-to-world
    * transform so that the eye's -z axis is directed at the center point. The
    * vertical direction is specified by the current up vector. This is
    * equivalent to
    * <pre>
    * setEyeToWorld (getEye(), center, getUpVector());
    * </pre>
    * 
    * @param center
    * new center location, in world coordinates
    * @see #getUpVector
    */
   public void setCenter (Point3d center);

   /**
    * Sets the eye coordinates to a specified location in world coordinates,
    * and adjusts the eye-to-world transform so that the eye's -z axis is
    * directed at the center point. The vertical direction is specified by the
    * current up vector. This is equivalent to
    * <pre>
    * setEyeToWorld (eye, getCenter(), getUpVector());
    * </pre>
    * 
    * @param eye
    * new eye location, in world coordinates
    * @see #getCenter
    * @see #getUpVector
    */
   public void setEye (Point3d eye);

   /**
    * Sets the ``up'' vector for this viewer to a specified value, and adjusts
    * the eye-to-world transform to account for this new vertical direction,
    * while the eye's -z axis is directed at the
    * center point. This is equivalent to
    * <pre>
    * setEyeToWorld (getEye(), getCenter(), up);
    * </pre>
    * 
    * @param up
    * new up vector, in world coordinates
    * @see #getEye
    * @see #getCenter
    */
   public void setUpVector (Vector3d up);

   /**
    * Returns the ``up'' vector which this viewer uses in some instances to
    * determine the direction of the y axis of the eye coordinate system.
    *
    * @return up vector, in world coordinates
    * @see #setUpVector
    * @see #setCenter
    * @see #setEye
    */
   public Vector3d getUpVector();

   /**
    * Sets the viewing frustum to a general perspective projection.  This is
    * done by specifying the edges locations of the view plane (i.e., the near
    * clipping plane) with respect to the x-y plane of the eye coordinates,
    * while also specifying the distances of the view plane and far clipping
    * plane along the -z axis.
    * 
    * @param left
    * left edge position of the view plane
    * @param right
    * right edge position of the view plane
    * @param bottom
    * bottom edge position of the near clipping plane
    * @param top
    * top position of the near clipping plane
    * @param near
    * near clipping plane position (along the -z axis; must be a positive
    * number)
    * @param far
    * far clipping plane position (along the -z axis; must be a positive number)
    */
   public void setPerspective (
      double left, double right, double bottom, double top, 
      double near, double far);

   /**
    * Sets the viewing frustum to a perspective projection centered about the -z
    * axis. This is done by specifying the vertical field of
    * view, along with the distances of the view plane and far clipping plane
    * along the -z axis. The field of view setting is also used
    * to update the value returned by
    * {@link #getVerticalFieldOfView getVerticalFieldOfView}.
    * 
    * @param fieldOfView
    * vertial field of view (in degrees)
    * @param near
    * near clipping plane position (along the -z axis; must be a positive
    * number)
    * @param far
    * far clipping plane position (along the -z axis; must be a positive number)
    */
   public void setPerspective (double fieldOfView, double near, double far);


   /**
    * Sets the viewing frustum to a general orthogonal projection. This is done
    * by specifying the edges locations of the view plane (i.e., the near
    * clipping plane) with respect to the x-y plane of the eye coordinates,
    * while also specifying the distances of the view plane and far clipping
    * plane along the -z axis.
    * 
    * @param left
    * left edge position of the near clipping plane
    * @param right
    * right edge position of the near clipping plane
    * @param bottom
    * bottom edge position of the near clipping plane
    * @param top
    * top position of the near clipping plane
    * @param near
    * near clipping plane position (along the -z axis; must be a positive
    * number)
    * @param far
    * far clipping plane position (along the -z axis; must be a positive number)
    */
   public void setOrthogonal (
      double left, double right, double bottom, double top, 
      double near, double far);

   /**
    * Sets the viewing frustum to an orthogonal projection centered about the
    * -z axis. This is done by specifying the vertical height of the field of
    * view, along with the distances of the view plane and far clipping plane
    * along the -z axis.
    * 
    * @param fieldHeight
    * vertical height of the field of view
    * @param near
    * near clipping plane position (along the -z axis; must be a positive
    * number)
    * @param far
    * far clipping plane position (along the -z axis; must be a positive number)
    */
   public void setOrthogonal (double fieldHeight, double near, double far);

   /**
    * Calls either {@link #autoFitOrtho} or {@link #autoFitPerspective},
    * depending on whether the current view is orthogonal or perspective.
    */
   public void autoFit();

   /**
    * Auto computes the eye and center positions and an orthogonal viewing
    * frustum to fit the current scence. This makes use of the {@link
    * IsRenderable#updateBounds} method of all the renderables to estimate
    * the scene center and an approximate radius r. It then computes a distance
    * d from the center to the eye using r = d tan(fov/2), where fov is the
    * field of view returned by {@link #getVerticalFieldOfView()} and
    * converted to radians. The eye frame orientation is adjusted so that
    * its $y$ axis is parallel to the current value of the ``up'' vector.
    *
    * @see #setUpVector
    * @see #getUpVector
    */
   public void autoFitOrtho();

   /**
    * Auto computes the eye and center positions and an perpective viewing
    * frustum to fit the current scence. This makes use of the {@link
    * IsRenderable#updateBounds} method of all the renderables to estimate
    * the scene center and an approximate radius r. It then computes a distance
    * d from the center to the eye using r = d tan(fov/2), where fov is the
    * field of view returned by {@link #getVerticalFieldOfView()} and
    * converted to radians. This field of view also used to used to compute the
    * perspective frustum. The eye frame orientation is adjusted so that
    * its $y$ axis is parallel to the current value of the ``up'' vector.
    *
    * @see #setUpVector
    * @see #getUpVector
    */
   public void autoFitPerspective();

   /**
    * Returns the default vertical field of view in degrees. This is used by
    * {@link #autoFitOrtho} and {@link #autoFitPerspective}.
    * 
    * @return default vertical field of view (degrees).
    */
   public double getVerticalFieldOfView();

   /**
    * Sets the default vertical field of view in degrees. This is used by
    * {@link #autoFitOrtho} and {@link #autoFitPerspective}.
    * 
    * @param fov new default vertical field of view (degrees).
    */
   public void setVerticalFieldOfView (double fov);

   /**
    * Adds a selection listener to this viewer that will fire whenever objects
    * are selected.
    *
    * @param l selection listener to add
    * @see #removeSelectionListener
    */
   public void addSelectionListener (ViewerSelectionListener l);

   /**
    * Removes a selection listener from this viewer.
    *
    * @param l selection listener to remove
    * @see #addSelectionListener
    */
   public boolean removeSelectionListener (ViewerSelectionListener l);

   /**
    * Returns a list of all the selection listener in this viewer.
    *
    * @return selection listeners in this viewer
    * @see #addSelectionListener
    * @see #removeSelectionListener
    */
   public ViewerSelectionListener[] getSelectionListeners();

   // fixtures, draggers and clip planes

   // GUI interaction control

}
