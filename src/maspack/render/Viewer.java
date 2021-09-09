/**
 * Copyright c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.*;

import maspack.matrix.*;
import maspack.render.GL.GLClipPlane;

/**
 * A viewer is a component that takes a collection of renderables and
 * renders them for a given viewpoint and set of lighting conditions.
 * It also implements the interface {@link Renderable}, which provides
 * the primary functionality by which renderables can render themselves.
 */
public interface Viewer extends Renderer {

   // rendering control

   /**
    * Controls how the viewer responds to rotation control inputs specified as
    * horizontal and vertical angular displacements in the viewing plane.
    */
   public enum RotationMode {
      /**
       * The horizontal displacement describes a rotation about the vertical
       * (``up'') direction (as returned by {@link #getUpVector}), while the
       * vertical displacement controls the elevation of the eye position. This
       * mode has the advantage that the ``up'' direction always remains
       * parallel to the vertical direction of the viewer plane. However,
       * because of this, the eye-to-world rotation cannot be adjusted to an
       * arbitrary value.
       */
      FIXED_VERTICAL,
   
      /**
       * The horizontal and vertical displacements describe instantaneous
       * angular velocity components of the eye-to-world rotation. This allows
       * the eye-to-world rotation to be adjusted to arbitrary values, but the
       * ``up'' direction will generally not remain parallel to the vertical
       * direction of the viewer plane.
       */
      CONTINUOUS;
   }

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

   //==========================================================================
   // Selection control
   //==========================================================================

   /**
    * Queries the highlighting style for this viewer.
    *
    * @return current highlighting style 
    */
   public HighlightStyle getHighlightStyle();

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
    * Sets the highlight color used by this viewer.
    *
    * @param color new highlight color
    */
   public void setHighlightColor (Color color);

   /**
    * Queries the highlight color used by this viewer.
    *
    * @return viewer highlight color
    */
   public Color getHighlightColor();

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
    * Set whether elliptic selection is enabled for this viewer.  Elliptic
    * selection is selection style in which an elliptic cursor is used to
    * ``paint'' the current set of selected components.
    *
    * @param if {@code true}, enables elliptic selection
    * @see #getEllipticSelection
    */
   public void setEllipticSelection (boolean enable);

   /**
    * Queries whether elliptic selection is enabled for this viewer.
    *
    * @return {@code true} if elliptic selection is enabled
    * @see #setEllipticSelection
    */
   public boolean getEllipticSelection();

   /**
    * Set whether or not selection is done when the mouse is pressed.
    * If enabled, this automatically diables "drag selection".
    * 
    * @param enable
    * Whether or not selection is enabled.
    */
   public void setSelectOnPress (boolean enable);

   /**
    * Returns true if "select on press" is enabled for the viewers.
    * 
    * @return true if "select on press" is enabled
    */
   public boolean getSelectOnPress(); 

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
    * @param r red color component
    * @param g red color component
    * @param b red color component
    */
   public void setBackgroundColor (float r, float g, float b);

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
    * Returns a transform from world coordinates to center coordinates, with the
    * axes aligned to match the current eyeToWorld transform. Seen through the
    * viewer, this will appear centered on the view frame with z pointing toward
    * the view, y pointing up, and x pointing to the right.
    *
    * @param TCW returns the center-to-world transform
    */
   public void getCenterToWorld (RigidTransform3d TCW);

   /**
    * Returns a transform from world coordinates to center coordinates.  This
    * performs the same function as {@link
    * #getCenterToWorld(RigidTransform3d)}, only with transform allocated and
    * returned.
    *
    * @return the center-to-world transform
    */
   public RigidTransform3d getCenterToWorld ();

   /**
    * Rotate the eye coordinate frame about the center point. How the rotation
    * angles are processed depends on the <i>rotation mode</i> controlled using
    * {@link #setRotationMode} and {@link #getRotationMode}.
    * 
    * @param xang
    * amount of horizontal rotation (in radians)
    * @param yang
    * amount of vertical rotation (in radians)
    */
   public void rotate (double xang, double yang);

   /**
    * Translate the eye position with respect to the x-y plane of the eye
    * frame.  The center point is translated by the same amount.
    * 
    * @param delx
    * x translation amount
    * @param dely
    * y translation amount
    */
   public void translate (double delx, double dely);

   /**
    * Zoom in or out by a specified scale factor. A factor larger than one zooms
    * out, while a factor less than one zooms in. In orthographic projection,
    * zoom is accomplished changing the frustum size. In perspective projection,
    * it is accomplished by moving the eye position along the z axis of the eye
    * frame.
    * 
    * @param s
    * scale factor
    */
   public void zoom (double s);  

   /**
    * Sets the rotation mode that controls how the viewer responds to
    * interactive rotation requests specified as horizontal and vertical
    * angular displacements in the view plane. The default rotation mode is
    * {@link RotationMode#FIXED_VERTICAL}.
    *
    * @param mode new rotation mode
    */
   public void setRotationMode (RotationMode mode);

   /**
    * Queries the rotation mode that controls how the viewer responds to
    * interactive rotation requests. See {@link #setRotationMode}.
    *
    * @return current rotation mode
    */
   public RotationMode getRotationMode();

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
    * Calls {@link #autoFitOrtho} or {@link #autoFitPerspective} depending on
    * whether {@code enable} is {@code true} or {@code false}.
    * 
    * @param enable enables (and auto-fits) orthographic view if {@code true} and
    * perspective view if {@code false}
    */
   public void setOrthographicView (boolean enable);

  /**
    * Returns true if the current viewing projection is orthogonal.
    * 
    * @return true if viewing projection is orthogonal
    */
   public boolean isOrthogonal();

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

   /**
    * Queries the length used for rendering coordinate axes in this viewer.
    *
    * @return axis rendering length
    */
   public double getAxisLength();

   /**
    * Sets the length used for rendering coordinate axes in this viewer.  A
    * length {@code <= 0} implies that coordinate axes will not be rendered.
    *
    * @return len axis rendering length
    */
   public void setAxisLength (double len);

   /**
    * Sets the viewer grid to be visible.
    *
    * @param if {@code true}, makes the grid visible
    */
   public void setGridVisible (boolean visible);

   /**
    * Sets the position and minimum size of the viewer grid.1`
    *
    * @param pcenter position of the grid center
    * @param r radius value, which equals 1/4 the minimum size
    */
   public void setGridSizeAndPosition (Point3d pcenter, double r);

   /**
    * Queries whether the viewer grid is visible.
    *
    * @return {@code true} if the grid is visible
    */
   public boolean getGridVisible();

   /**
    * Returns this viewer's grid.
    *
    * @return viewer grid
    */
   public GridPlane getGrid();

   //==========================================================================
   //  Clip Planes
   //==========================================================================

   /**
    * Queries the maximum number of clip surfaces available to this viewer.
    * This depends on the underlying graphics implementation. Each clip plane
    * held by the viewer will require one surface if clipping is enabled, or
    * two surfaces if slicing is enabled. Clipping will be curtailed if the
    * total number of required clip surfaces exceeds the maximum.
    *
    * @return maximum number of clip surfaces
    */
   public int maxClipSurfaces();

   /**
    * Queries the number of clip surfaces currently available to this viewer.
    * This will equal the value returned by {@link #maxClipSurface} minus the
    * number required by all the currently held clip planes, with the result
    * set to 0 if negative. Clipping will be curtailed if the number of
    * available surfaces is 0.
    * 
    * @return number of currently available clip surfaces
    */
   public int numFreeClipSurfaces();

   /**
    * Creates and add a clip plane to this viewer. The pose is centered on the
    * origin and aligned with the current axial view, while this size is set to
    * a default value based on the current viewing frustum. This method is
    * equivalent to calling {@code addClipPlane(null,0)}.
    * 
    * @return created clip plane
    */
   public GLClipPlane addClipPlane();

   /**
    * Creates and add a clip plane to this viewer.
    * 
    * @param TPW transform specifying the clip plane pose with respect to world
    * coordinates. If {@code null}, the pose is centered on the origin and
    * aligned with the current axial view.
    * @param size visible width of the clip plane in each direction. If
    * {@code <= 0}, this is set to a default value based on the current
    * viewing frustum.
    * @return created clip plane
    */
   public GLClipPlane addClipPlane (RigidTransform3d TPW, double size);

   /**
    * Adds a clip plane to this viewer.
    * 
    * @param clipPlane clip plane to add
    */
   public void addClipPlane (GLClipPlane clipPlane);

   /**
    * Returns the {@code idx}-th clip plane held by this viewer. {@code idx}
    * must be {@code <} {@code #getNumClipPlanes}.

    * @param idx index of the desired clip plane
    * @return the {@code idx}-th clip plane 
    */
   public GLClipPlane getClipPlane (int idx);

   /**
    * Queries the number of clip planes currently held by this viewer.
    * 
    * @return current number of viewer clip planes
    */
   public int getNumClipPlanes();

   /**
    * Returns all the clip planes currently held by this viewer.
    * 
    * @return current viewer clip planes
    */
   public GLClipPlane[] getClipPlanes();

   /**
    * Removes a clip plane from this viewer.
    * 
    * @param clipPlane clip plane to remove
    * @return
    */
   public boolean removeClipPlane (GLClipPlane clipPlane);

   /**
    * Removes all clip planes from this viewer.
    */
   public void clearClipPlanes();

   //==========================================================================
   // Mouse and keyboard listeners
   //==========================================================================

   /**
    * Adds a mouse input listener to this viewer.
    *
    * @param l listener to be added
    */
   public void addMouseInputListener (MouseInputListener l);

   /**
    * Removes a mouse input listener from this viewer.
    *
    * @param l listener to be removed
    */
   public void removeMouseInputListener (MouseInputListener l);

   /**
    * Returns the mouse input listeners currently added to this viewer.
    *
    * @return current mouse input listeners
    */
   public MouseInputListener[] getMouseInputListeners();

   /**
    * Adds a key listener to this viewer.
    *
    * @param l listener to be added
    */
   public void addKeyListener (KeyListener l);

   /**
    * Removes a key listener from this viewer.
    *
    * @param l listener to be removed
    */
   public void removeKeyListener (KeyListener l);

   /**
    * Returns the key listeners currently added to this viewer.
    *
    * @return current key listeners
    */
   public KeyListener[] getKeyListeners();

   /**
    * Queries whether view rotation is enabled.
    *
    * @return {@code true} if view rotation is enabled
    */
   public boolean isViewRotationEnabled();

   /**
    * Sets whether view rotation is enabled. If it is not, then it will not be
    * possible to rotate the view interactively.  The default value is {@code
    * true},
    *
    * @param enable if {@code true}, enables view rotation
    */   
   public void setViewRotationEnabled (boolean enable);

   //==========================================================================
   // Draggers and draw tools
   //==========================================================================

   /**
    * Sets the draw tool for this viewer.
    *
    * @oaram d draw tool to set
    */
   public void setDrawTool (Dragger3d d);

   /**
    * Queries the draw tool, if any, for this viewer.
    *
    * @return current draw tool, or {@code null} if there is none
    */
   public Dragger3d getDrawTool();

   /**
    * Adds an application-defined dragger fixture to this viewer.
    *
    * @param d dragger to add
    */
   public void addDragger (Dragger3d d);

   /**
    * Removes an application-defined dragger fixture from this viewer.
    *
    * @param d dragger to remove
    */
   public void removeDragger (Dragger3d d);

   /**
    * Removes all application-defined dragger fixtures from this viewer.
    */
   public void clearDraggers();

   /**
    * Returns a list of all application-defined draggers currently added to
    * this viewer. The returned list is a copy and may therefore be modified.
    *
    * @return list of current application-defined draggers
    */
   public LinkedList<Dragger3d> getDraggers();

   /**
    * Returns a list of all draggers currently active for this viewer,
    * including those that are application-defined as well as those those that
    * are created internally. The returned list should not be modified,
    * although the draggers within the list may be.
    *
    * <p>This method is intended for use by the mouse input handler
    * that manages dragger activity.
    * 
    * @return list of all currently active draggers
    */
   public List<Dragger3d> getAllDraggers();

   /**
    * Returns the x component of the origin of the component implementing this
    * viewer.
    *
    * @return origin x component
    */
   public int getScreenX();

   /**
    * Returns the y component of the origin of the component implementing this
    * viewer.
    *
    * @return origin y component
    */
   public int getScreenY();

   /**
    * Sets the screen cursor for this viewer.
    *
    * @param cursor new screen cursor
    */
   public void setScreenCursor (Cursor cursor);

   /**
    * Queries the screen cursor for this viewer.
    *
    * @param return viewer screen cursor
    */
   public Cursor getScreenCursor();


   /**
    * Queries whether an elliptic cursor is active for this viewer.
    *
    * @return {@code true} if an elliptic cursor is active
    */
   public boolean getEllipticCursorActive();
   
   /**
    * Sets whether an elliptic cursor is active for this viewer.
    *
    * @param if {@code true}, activates an elliptic cursor
    */
   public void setEllipticCursorActive (boolean active);
   
   /**
    * Queries the elliptic cursor size for this viewer.
    *
    * @return viewer's elliptic cursor size
    */
   public Vector2d getEllipticCursorSize();
   
   /**
    * Sets the elliptic cursor size for this viewer.
    *
    * @return size new elliptic cursor size
    */
   public void setEllipticCursorSize (Vector2d size);
   
   /**
    * Resets the elliptic cursor size for this viewer to its default value.
    */
   public void resetEllipticCursorSize();

   /**
    * Sets a <i>drag box</i> to be displayed by the viewer. This is a
    * rectangular screen area that is usually selected by a drag operaion.
    * Specifying a {@code null} value means that no drag box should be
    * displayed.
    *
    * @param box drag box in screen coordinates, or {@code null}
    * if the drag box is to be removed
    */
   public void setDragBox (Rectangle box);

   /**
    * Queries information about any drag box currently being displayed.
    *
    * @return rectangle describing the screen coordinates of the current drag
    * box, or {@code null} if no box is currently being displayed.
    */
   public Rectangle getDragBox();

   /**
    * Queries the AWT component associated with this viewer.
    *
    * @return viewer AWT component
    */
   public Component getComponent();

   // fixtures, draggers and clip planes

   // GUI interaction control
   
   /**
    * Disposes of the resources used by this viewer.
    */
   public void dispose();

}
