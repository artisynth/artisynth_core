/**
 * Copyright c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Vector3d;
import maspack.matrix.Matrix4d;
import maspack.render.RenderProps.LineStyle;
import maspack.render.RenderProps.PointStyle;
import maspack.render.RenderProps.Shading;
import maspack.render.RenderProps.Faces;
import maspack.render.GL.GLSelectable;
import maspack.render.GL.GLSelectionFilter;

public interface Renderer {

   /**
    * Indicates the space in which color interpolation is performed 
    * by this Renderer.
    */
   public enum ColorInterpolation {
      /**
       * Color interpolation is disabled.
       */
      NONE,
      
      /**
       * Color interpolation is performed in RGB space.
       */
      RGB,
      
      /**
       * Color interpolation is performed in HSV space. This is often
       * appropriate for doing pure color-based plotting.
       */
      HSV
   };
   
   // FINISH
   public enum ColorMixing {
      NONE,  // ignore
      REPLACE,
      MODULATE,
      DECAL
   }

   // Render flags. FINISH
   /** 
    * Flag requesting that an object be rendered as though it is selected.
    */   
   public static int SELECTED = 0x1;

   /** 
    * Flag requesting that vertex coloring should be used for mesh rendering.
    */
   public static int VERTEX_COLORING = 0x2;

   /** 
    * Flag requesting color interpolation in HSV space, if possible.
    */
   public static int HSV_COLOR_INTERPOLATION = 0x4;
   
   /**
    * Defines various vertex-based primitives
    */
   public enum VertexDrawMode {
      /**
       * A collection of points, one per vertex
       */
      POINTS,
      
      /**
       * A collection of line segments, with every two vertices
       * defining a single segment
       */
      LINES,
      
      /**
       * A continuous line strip formed from a sequence of vertices
       */
      LINE_STRIP,
      
      /**
       * A continuous line loop formed from a sequence of vertices
       */
      LINE_LOOP,
      
      /**
       * A collection of triangles, with each three successive vertices
       * defining a single triangle
       */
      TRIANGLES,
      
      /**
       * A triangle strip. The first three vertices form the initial
       * triangle {0, 1, 2}. Then subsequent vertices form the triangles
       * {2, 1, 3}, {2, 3, 4}, {4, 3, 5}, etc.
       */
      TRIANGLE_STRIP,
      
      /**
       * A triangle fan. The first three vertices form the initial
       * triangle {0, 1, 2}. Then each additional vertex i defines a new 
       * triangle formed from the vertices {0, i-1, i}.
       */
      TRIANGLE_FAN
   }

   /**
    * Specifies how selected items are highlighted by this renderer.
    */
   public enum SelectionHighlighting {
      /**
       * Selection highlighting is not supported or is disabled
       */
      NONE, 
      
      /**
       * Selection highlighting is performed by rendering selected
       * objects using a special <i>selection color</i>, as returned
       * by {@link #getSelectionColor(float[])}. 
       */
      COLOR
   };

   /**
    * Returns the screen height, in pixels
    * 
    * @return screen height
    */
   public int getScreenHeight();

   /**
    * Returns the screen width, in pixels
    * 
    * @return screen width
    */
   public int getScreenWidth();

   /**
    * Returns the height of the view plane, in model coordinates. The
    * view plane corresponds to the clipping plane nearest the viewer. 
    * 
    * @return view plane height
    */
   public double getViewPlaneHeight();

   /**
    * Returns the width of the view plane, in model coordinates. The
    * view plane corresponds to the clipping plane nearest the viewer. 
    * 
    * @return view plane width
    */
   public double getViewPlaneWidth();

   // public double getFieldOfViewY();

   /**
    * Returns the approximate magnitude of the displacement of the viewing
    * center point that corresponds to a screen displacement of one pixel.
    * The displacement is given with respect to model coordinates.
    *
    * @return approximate displacement magnitude
    */
   public double centerDistancePerPixel ();
   
   /**
    * Returns the approximate magnitude of the displacement of a point
    * <code>p</code> that corresponds to a screen displacement of one pixel.
    * Both <code>p</code> and the displacement are given with respect to model
    * coordinates.
    *
    * @param p point undergoing the displacement 
    * @return approximate displacement magnitude
    */
   public double distancePerPixel (Vector3d p);

   /**
    * Returns <code>true</code> if this renderer is currently configured for
    * orthogonal projection.
    * 
    * @return <code>true</code> if projection is orthogonal
    */
   public boolean isOrthogonal();

   /**
    * Returns the distance, in model coordinates, from the eye to tne near clip
    * plane. This is a positive number.
    *
    * @return distance to the near clip plane
    * @see #getFarClipPlaneZ
    */
   public double getNearClipPlaneZ();

   /**
    * Returns the distance, in model coordinates, from the eye to the far clip
    * plane. This is a positive number.
    *
    * @return distance to the far clip plane
    * @see #getNearClipPlaneZ
    */
   public double getFarClipPlaneZ();
   
   /**
    * Returns the current size for rendering points, in pixels.
    * 
    * @return current point size (in pixels).
    * @see #setDefaultPointSize
    * @see #setPointSize
    */
   public float getPointSize();
   
   /**
    * Sets the size for rendering points, in pixels. The default size is 1.
    * 
    * @param size new point size (in pixels).
    * @see #setDefaultPointSize
    * @see #getPointSize
    */
   public void setPointSize(float size);
   
   /**
    * Sets the size for rendering points to its default value, which is 1.
    * 
    * @see #setPointSize
    * @see #getPointSize
    */
   public void setDefaultPointSize();
   
   /**
    * Returns the current width for rendering lines, in pixels.
    * 
    * @return current line width (in pixels).
    * @see #setDefaultLineWidth
    * @see #setLineWidth
    */
   public float getLineWidth();
   
   /**
    * Sets the width for rendering lines, in pixels. The default size is 1.
    * 
    * @param width new line width (in pixels).
    * @see #setDefaultLineWidth
    * @see #getLineWidth
    */
   public void setLineWidth (float width);
   
   /**
    * Sets the width for rendering points to its default value, which is 1.
    * 
    * @see #setLineWidth
    * @see #getLineWidth
    */
   public void setDefaultLineWidth();

   /**
    * Returns the current mode for rendering faces.
    * 
    * @return current face rendering mode
    * @see #setDefaultFaceMode
    * @see #setFaceMode
    */
   public Faces getFaceMode ();

   /**
    * Sets the mode for rendering faces. This determines whether
    * the front and/or back of each face is rendered. The default value
    * is {@link Faces.FRONT}.
    * 
    * @param mode new face rendering mode
    * @see #setDefaultFaceMode
    * @see #getFaceMode
    */
   public void setFaceMode (Faces mode);

   /**
    * Sets the mode for rendering faces to its default value, which is
    * {@link Faces.FRONT}.
    * 
    * @see #setFaceMode
    * @see #getFaceMode
    */
   public void setDefaultFaceMode();

   /**
    * Returns the color interpolation currently being used by this Renderer.
    * 
    * @return current color interpolation
    */
   public ColorInterpolation getColorInterpolation();

   /**
    * Sets the color interpolation to be used by this renderer. The default
    * value is {@link ColorInterpolation#RGB}, but 
    * {@link ColorInterpolation#HSV} is often more appropriate for
    * displaying information that is purely color-based. 
    * 
    * @param interp new color interpolation mode
    */
   public void setColorInterpolation (ColorInterpolation interp);
   
   // FINISH
   /**
    * Specify method for combining material color and vertex color
    * @param cmix
    */
   public void setColorMixing(ColorMixing cmix);  
   
   // FINISH
   public ColorMixing getColorMixing();
   
   /**
    * Specify method for combining texture color with underlying material color
    * @param tmix
    */
   public void setTextureMixing(ColorMixing tmix);
   
   public ColorMixing getTextureMixing();

   // FINISH: remove this, replace with setDefaultShading()
   public void restoreShading (RenderProps props);

   /**
    * Returns the current shading model used by this renderer. A shading model
    * of {@link Shading.NONE} turns off lighting and causes primitives to be
    * rendered in solid colors, using the current diffuse color.
    *
    * @return current shading model
    */
   public Shading getShadeModel();

   /**
    * Sets the shading model used by this renderer. The default value is {@link
    * Shading#FLAT}, in which one normal is used per primitive.  A shading
    * model of {@link Shading.NONE} turns off lighting and causes primitives to
    * be rendered in solid colors, using the current diffuse color.
    *
    * @param shading new shading model to be used
    */
   public void setShadeModel (Shading shading);

   /**
    * Sets the default shading model for this renderer, which is
    * {@link Shading#FLAT}.
    */
   public void setDefaultShadeModel ();

   /**
    * Returns <code>true</code> if lighting is currently enabled.
    * Lighting is enabled by default.
    * 
    * @return <code>true</code> if lighting is enabled.
    * @see #setLightingEnabled
    */
   public boolean isLightingEnabled();

   /**
    * Enables or disables lighting. Disabling lighting is equivalent
    * in effect to setting shading to {@link Shading.NONE}. However,
    * disabling the lighting does not affect the current shading
    * value, which takes effect again as soon as lighting is re-enabled.
    *
    * @param enable specifies whether to enable or disable lighting.
    */
   public void setLightingEnabled (boolean enable);   

   // Drawing primitives

   /**
    * Draws a single point located at <code>pnt</code> in model coordinates,
    * using the current point size, material, and shading. Since no normal is
    * specified, this method should generally be called with either lighting
    * disabled or with shading set to {@link Shading.NONE}.
    *
    * @param pnt location of the point
    */
   public void drawPoint (Vector3d pnt);
   
   /**
    * Draws a single point located at <code>px</code>, <code>py</code> and
    * <code>pz</code> in model coordinates, using the current point size,
    * material, and shading. This method is functionally equivalent to
    * {@link #drawPoint(Vector3d)}.
    *
    * @param px x coordinate of the point
    * @param py y coordinate of the point
    * @param pz z coordinate of the point
    */
   public void drawPoint (double px, double py, double pz);

   /**
    * Draws a single point located at <code>pnt</code> in model coordinates,
    * using the current point size, material, and shading. This method
    * is functionally equivalent to {@link #drawPoint(Vector3d)}.
    *
    * @param pnt array (of length 3) giving the point to draw
    */
   public void drawPoint (float[] pnt);
   
   // REMOVE
   /**
    * Draws a single point located at <code>pnt</code>, and with a normal
    * <code>nrm</code>, in model coordinates, using the current point size,
    * material, and shading. 
    *
    * @param pnt array (of length 3) giving the point location
    * @param nrm array (of length 3) giving the point normal
    */
   public void drawPoint (float[] pnt, float[] nrm);
   
   // REMOVE
   public void drawPoint (float x, float y, float z);

   /**
    * Draws a single line between two points in model coordinates,
    * using the current line width, material, and shading. Since no normal are
    * specified, this method should generally be called with either lighting
    * disabled or with shading set to {@link Shading.NONE}.
    * 
    * @param pnt0 first point
    * @param pnt1 second point
    */
   public void drawLine (Vector3d pnt0, Vector3d pnt1);
   
   /**
    * Draws a single line between two points in model coordinates,
    * using the current line width, material, and shading. This method
    * is functionally equivalent to {@link #drawLine(Vector3d,Vector3d)}.
    *
    * @param px0 x coordinate of first point
    * @param py0 y coordinate of first point
    * @param pz0 z coordinate of first point
    * @param px1 x coordinate of second point
    * @param py1 y coordinate of second point
    * @param pz1 z coordinate of second point
    */
   public void drawLine (
      double px0, double py0, double pz0, double px1, double py1, double pz1);
   
   // REMOVE
   public void drawLine (
      float x0, float y0, float z0, float x1, float y1, float z1);
   
   /**
    * Draws a single line between two points in model coordinates,
    * using the current line width, material, and shading.  This method
    * is functionally equivalent to {@link #drawLine(Vector3d,Vector3d)}.
    * 
    * @param pnt0 array (of length 3) giving the first point
    * @param pnt1 array (of length 3) giving the second point
    */
   public void drawLine (float[] pnt0, float[] pnt1);

   // REMOVE
   /**
    * Draws a single line between two points, with specified normals, in model
    * coordinates, using the current line width, material, and shading.
    * 
    * @param pnt0 array (of length 3) giving the first point
    * @param nrm0 array (of length 3) giving the first normal
    * @param pnt1 array (of length 3) giving the second point
    * @param nrm1 array (of length 3) giving the second normal
    */
   public void drawLine (
      float[] pnt0, float[] nrm0, float[] pnt1, float[] nrm1);

   /**
    * Draws a single triangular face specified by three points, using the
    * current shading, lighting and material. The triangle's normal is computed
    * and used for all three vertices, so that unless shading is set to {@link
    * Shading#NONE}, the shading will be effectively flat.
    *
    * @param pnt0 first point
    * @param pnt1 second point
    * @param pnt2 third point
    */
   void drawTriangle (Vector3d pnt0, Vector3d pnt1, Vector3d pnt2);

   /**
    * Draws a single triangular face specified by three points, using the
    * current shading, lighting and material.  This method
    * is functionally equivalent to is functionally
    * equivalent to {@link #drawTriangle(Vector3d,Vector3d,Vector3d)}.
    *
    * @param pnt0 first point
    * @param pnt1 second point
    * @param pnt2 third point
    */
   void drawTriangle (float[] pnt0, float[] pnt1, float[] pnt2);
   
   // Solid drawing primitives
   
   /**
    * Returns the resolution used for creating mesh representations
    * of curved solids. 
    * 
    * @return resolution for approximating curved surfaces
    * @see #setCurvedMeshResolution
    */
   public int getCurvedMeshResolution();
   
   /**
    * Sets the resolution used for creating mesh representations
    * of curved solids. The number is approximately the
    * number of linear segments used to approximate the solid about
    * its principal circumference. It is understood to
    * be only a lower bound, and the actual resolution may be higher.
    * 
    * @param resolution for curved surfaces
    */
   public void setCurvedMeshResolution (int nsegs);
   
   /**
    * Draws a sphere with a specified radius centered at a point
    * in model coordinates, using the current shading and material.
    * The point is located in model coordinates. The resolution
    * of the sphere is specified by {@link #getCurvedMeshResoltion}.
    * 
    * @param pnt center of the sphere
    * @param rad radius of the sphere
    */
   public void drawSphere (Vector3d pnt, double rad);
   
   /**
    * Draws a sphere with a specified radius centered at a point
    * in model coordinates, using the current shading and material.
    * This method is functionally equivalent to
    * {@link #drawSphere(Vector3d,double)}.
    * 
    * @param pnt center of the sphere
    * @param rad radius of the sphere
    */
   public void drawSphere (float[] pnt, double rad);   

   /**
    * Draws a tapered ellipsoid between two points in model coordinates, 
    * using the current shading and material. The resolution
    * is specified by {@link #getCurvedMeshResolution}.
    * 
    * @param pnt0 first end point
    * @param pnt1 second end point
    * @param rad radius of the ellipsoid 
    */
   public void drawTaperedEllipsoid (float[] pnt0, float[] pnt1, double rad);

//   // REPLACE with drawLine (style)?
//   public void drawCylinder (float[] pnt0, float[] pnt1, double rad);

   /**
    * Draws a cylinder between two points in model coordinates, 
    * using the current shading and material. The resolution
    * is specified by {@link #getCurvedMeshResolution}.
    * 
    * @param pnt0 first end point
    * @param pnt1 second end point
    * @param rad radius of the cylinder
    * @param capped if <code>true</code>, indicates that the cylinder
    * should have a solid cap on each end
    */
   public void drawCylinder (
      float[] pnt0, float[] pnt1, double rad, boolean capped);
   
//   // REMOVE
//   public void drawCone (RenderProps props, float[] coords0, float[] coords1);

   /**
    * Draws a cone between two points in model coordinates, 
    * using the current shading and material. A cone is like a cylinder,
    * except that it can have different radii at the end points. The resolution
    * is specified by {@link #getCurvedMeshResolution}.
    * 
    * @param pnt0 first end point
    * @param pnt1 second end point
    * @param rad0 radius at first end point
    * @param rad1 radius at second end point
    * @param capped if <code>true</code>, indicates that the cone
    * should have a solid cap on each end
    */
   public void drawCone (
      float[] pnt0, float[] pnt1, double rad0, double rad1, boolean capped);
   
   /**
    * Draws a solid arrow between two points in model coordinates, 
    * using the current shading and material. The arrow is rendered as a
    * cylinder with radius <code>rad</code>, topped with a conical arrow
    * head pointing towards the second end point. The arrow head has
    * a base radius of <code>3*rad</code> and length given by
    * <p>
    * min (6*rad, len/2)
    * </p>
    * where <code>len</code> is the distance between the two end points.
    * The resolution is specified by {@link #getCurvedMeshResolution}.
    * 
    * @param pnt0 first end point
    * @param pnt1 second end point
    * @param rad radius of the cylinder
    * @param capped if <code>true</code>, indicates that the arrow
    * should have a solid cap on the bottom
    */
   public void drawSolidArrow (
      float[] pnt0, float[] pnt1, double rad, boolean capped);

//   public void drawSolidArrow (
//      RenderProps props, float[] coords0, float[] coords1);
   
   /**
    * Draws a set of coordinate axes representing a rigid coordinate frame 
    * <code>X</code>. The origin point for the axes is <code>X.p</code>,
    * and the directions for each axis are given by the three columns 
    * of <code>X.R</code>. Each axis is drawn as a pixel-based line with a 
    * length <code>len</code> and a width <code>width</code>. By default,
    * the x, y, and z axes are drawn using the colors red, green, and blue,
    * unless <code>selected</code> is <code>true</code> and selection
    * highlighting is {@link SelectionHighlighting#COLOR}, in which case
    * all axes are drawn using the renderer's selection color.
    *  
    * @param X coordinate frame defining the axes
    * @param len length of each axis
    * @param width width of each axis (in pixels)
    * @param selected if <code>true</code>, indicates that the axes should be
    * highlighted as selected.
    */
   public void drawAxes (
      RigidTransform3d X, double len, int width, boolean selected);

   /**
    * Draws a set of coordinate axes representing a rigid coordinate frame 
    * <code>X</code>. This method is functionally equivalent to {@link
    * #drawAxes(RigidTransform3d,double,int,boolean)}, except that the
    * length for each axis are individually specified.
    *  
    * @param X coordinate frame defining the axes
    * @param lens lengths for each axis
    * @param width width of each axis (in pixels)
    * @param selected if <code>true</code>, indicates that the axes should be
    * highlighted as selected.
    */
   public void drawAxes (
      RigidTransform3d X, double[] lens, int width, boolean selected);
   
   // Drawing primitives that use RenderProps

   /**
    * Draws a single point located at <code>pnt</code> in model coordinates,
    * using the point style, size, point color, and shading specified by the
    * render properties argument <code>props</code>. 
    * 
    * <p>If <code>props.getPointStyle()</code>
    * specifies a point style of {@link PointStyle#POINT}, then the 
    * point is drawn as a pixel-based point with a size given by
    * <code>props.getPointSize()</code>. Otherwise, if the point style
    * is {@link PointStyle#SPHERE}, then the 
    * point is drawn as a solid sphere with a radius given by
    * <code>props.getPointRadius()</code>. If <code>selected</code>
    * is <code>true</code> and selection highlighting is
    * {@link SelectionHighlighting#COLOR}, then the point will
    * be drawn using the selection color rather than the point color
    * specified in <code>props</code>.
    * 
    * @param props render properties used for drawing the point
    * @param pnt location of the point
    * @param selected if <code>true</code>, indicates that selection
    * highlighting, if enabled, should be applied to the point
    */
   public void drawPoint (RenderProps props, float[] pnt, boolean selected);

   /**
    * Draws a single line between two points in model coordinates,
    * using the line style, radius, line color, and shading specified by the
    * render properties argument <code>props</code>. 
    * 
    * <p>If <code>props.getLineStyle()</code>
    * specifies a line style of {@link LineStyle#LINE}, then the 
    * line is drawn as a pixel-based line with a width given by
    * <code>props.getLineWidth()</code>. For other line styles,
    * the line is drawn as a solid with a nominal radius given by 
    * <code>props.getLineRadius()</code>. If <code>selected</code>
    * is <code>true</code> and selection highlighting is
    * {@link SelectionHighlighting#COLOR}, then the line will
    * be drawn using the selection color rather than the line color
    * specified in <code>props</code>.
    * 
    * @param props render properties used for drawing the line
    * @param pnt0 first point
    * @param pnt1 second point
    * @param selected if <code>true</code>, indicates that selection
    * highlighting, if enabled, should be applied to the line
    */
   public void drawLine (
      RenderProps props, float[] pnt0, float[] pnt1, boolean selected);

   /**
    * Draws a single line between two points in model coordinates, using the
    * line style, radius, line color, and shading specified by the render
    * properties argument <code>props</code>. This method is functionally
    * equivalent to {@link #drawLine(RenderProps,float[],float[],boolean)},
    * except for the additional control provided by the arguments
    * <code>color</code> and <code>capped</code>.
    * 
    * @param props render properties used for drawing the line
    * @param pnt0 first point
    * @param pnt1 second point
    * @param color if not <code>null</code>, specifies an alternate
    * color that should be used in place of the line color specified
    * in <code>props</code>
    * @param capped if <code>true</code>, specifies that the line
    * should have a solid cap on its ends for 
    * styles such as {@link LineStyle#CYLINDER} and @link LineStyle#ARROW}
    * @param selected if <code>true</code>, indicates that selection
    * highlighting, if enabled, should be applied to the line
    */
   public void drawLine (
      RenderProps props, float[] pnt0, float[] pnt1, float[] color,
      boolean capped, boolean selected);

//   public void drawLine (
//      RenderProps props, float[] coords0, float[] coords1, boolean capped,
//      boolean selected);

   /**
    * Draws an arrow between two points in model coordinates,
    * using the radius, line color, and shading specified by the
    * render properties argument <code>props</code>. The method
    * behaves similarly to 
    * {@link #drawSolidArrow(float[],float[],double,boolean)},
    * with the cylinder radius determine instead by
    * <code>props.getLineRadius()</code>. 
    * 
    * @param props render properties used for drawing the arrow
    * @param pnt0 first point
    * @param pnt1 second point
    * @param capped if <code>true</code>, specifies that the arrow
    * should have a solid cap on the bottom
    * @param selected if <code>true</code>, indicates that selection
    * highlighting, if enabled, should be applied to the arrow
    */
   public void drawArrow (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      boolean selected);

//   // REMOVE
//   public void drawLines (
//      RenderProps props, Iterator<? extends RenderableLine> iterator);

   /**
    * Draws a line strip between a series of points in model coordinates,
    * using the radius, line color, and shading specified by the
    * render properties argument <code>props</code>, and the line
    * style specified by <code>style</code>.
    * 
    * <p>If the line style is {@link LineStyle#LINE}, then the 
    * strip is drawn using pixel-based lines with a width given by
    * <code>props.getLineWidth()</code>. For other line styles,
    * the strip is drawn as solids with a nominal radius given by 
    * <code>props.getLineRadius()</code>. If <code>selected</code>
    * is <code>true</code> and selection highlighting is
    * {@link SelectionHighlighting#COLOR}, then the strip will
    * be drawn using the selection color rather than the line color
    * specified in <code>props</code>.
    * 
    * @param props render properties used for drawing the strip
    * @param pntList list of points used for drawing the strip
    * @param style line style to be used for the strip
    * @param selected if <code>true</code>, indicates that selection
    * highlighting, if enabled, should be applied to the strip
    */
   public void drawLineStrip (
      RenderProps props, Iterable<float[]> pntList, 
      LineStyle style, boolean isSelected);   

   public boolean isTransparencyEnabled();

   public void setTransparencyEnabled (boolean enable);

   // public void drawXYGrid (double size, int numcells);

   /**
    * Gives the direction, in model coordinates, of a vector that
    * is perpendicular to the screen and points towards the viewer.
    * The corresponds to the Z direction in eye coordinates.
    * 
    * @return current z direction in eye coordinates (can be modified)
    */
   public Vector3d getEyeZDirection();
   
   /**
    * Start displaying 2D objects, dimensions given by pixels
    */
   public void begin2DRendering();
   
   /**
    * Start displaying 2D objects, dimensions governed by 
    * supplied width/height
    */
   public void begin2DRendering(double w, double h);
   
   /**
    * Finalize 2D rendering, returning to default 3D mode
    */
   public void end2DRendering();
   
   /**
    * Check whether renderer is currently in 2D mode
    */
   public boolean is2DRendering();

   //===============================================================================
   // COLORS and Materials
   //===============================================================================
   
   /**
    * Sets the diffuse and ambient colors to the value specified by 
    * <code>rgba</code>, unless <code>selected</code> is <code>true</code>
    * and selection highlighting equals {@link SelectionHighligthing#COLOR},
    * in which case the renderer's selection color is used.
    * 
    * <p>This method also clears any back color that may be present.
    * It is therefore equivalent to calling
    * <pre>
    *    setFrontColor (color);
    *    setBackColor (null);
    * </pre>
    * where <code>color</code> is either <code>rgba</code> or the selection
    * color, as described above.
    * 
    * @param rgba array of length 3 or 4 specifying RGB or RGBA values in
    * the range [0,1]. Alpha is only applied to the diffuse color
    * and is assumed to be 1.0 if not specified.
    * @param selected if <code>true</code> and if color selection highlighting
    * is enabled, the color is set to the selection color instead of
    * <code>rgba</code>
    */
   public void setColor (float[] rgba, boolean selected);
   
//   /**
//    * Main material color (diffuse/ambient)
//    * @param frontRgba
//    * @param backRgba
//    * @param selected
//    */
//   public void setColor (float[] frontRgba, float[] backRgba, boolean selected);

   /**
    * Sets the diffuse and ambient colors to the value specified by 
    * <code>rgba</code>. This method also clears any back color that may be 
    * present, and is therefore equivalent to calling
    * <pre>
    *    setFrontColor (rgba);
    *    setBackColor (null);
    * </pre>
    * 
    * @param rgba array of length 3 or 4 specifying RGB or RGBA values in
    * the range [0,1]. Alpha is only applied to the diffuse color
    * and is assumed to be 1.0 if not specified.
    */
   public void setColor (float[] rgba);
   
   /**
    * Sets the diffuse and ambient colors to the renderer's selection color.
    * This method also clears any back color that may be present, and so
    * is therefore equivalent to calling
    * <pre>
    *    setColor (color, true);
    * </pre>
    * where <code>color</code> is ignored.
    */
   public void setColorSelected ();   
   
   /**
    * Sets the diffuse and ambient colors to be used for subsequent rendering
    * of primitives. The method is functionaly equivalent to
    * {@link #setColor(float[])}, only with alpha assumed to be 1.
    * 
    * @param r red value
    * @param g green value
    * @param b blue value
    */
   public void setColor (float r, float g, float b);


   /**
    * Sets the diffuse and ambient colors to be used for subsequent rendering
    * of primitives. The method is functionaly equivalent to
    * {@link #setColor(float[])}.
    * 
    * @param r red value
    * @param g green value
    * @param b blue value
    * @param a alpha value
    */
   public void setColor (float r, float g, float b, float a);
   // not used
//   public void setColor (Color color);
   // not used
//   public void setColor (float[] frontRgba, float[] backRgba);
   // not used
//   public void setColor (Color frontColor, Color backColor);

   /**
    * Sets the diffuse and ambient colors to be used for subsequent rendering
    * of primitives. This is also the color that is used for solid
    * color rendering if lighting is disabled or shading is set to 
    * {@link Shading#NONE}. By default, this color is used for both the
    * front and back faces of triangles, but an alternative color
    * can be specified for back faces using {@link #setBackColor}.
    * 
    * @param rgba array of length 3 or 4 specifying RGB or RGBA values in
    * the range [0,1]. Alpha is only applied to the diffuse color
    * and is assumed to be 1.0 if not specified.
    */
   public void setFrontColor (float[] rgba);
   
   /**
    * Optionally sets the diffuse and ambient colors used for the 
    * subsequent of the back faces of triangles. If <code>rgba</code>
    * is set to <code>null</code>, then back coloring is disabled 
    * and the front color is used instead.
    * 
    * @param rgba <code>null</code> to disable back coloring, or
    * an array of length 3 or 4 specifying RGB or RGBA values in
    * the range [0,1]. Alpha is only applied to the diffuse color
    * and is assumed to be 1.0 if not specified.
    */
   public void setBackColor (float[] rgba);
   
   /**
    * Sets the emission color to be used for subsequent rendering
    * of primitives. The default emission color has a value
    * of <code>(0, 0, 0)</code>.
    * 
    * @param rgb array of length 3 specifying RGB values in
    * the range [0,1].
    */
   public void setEmission (float[] rgb);
   
   /**
    * Sets the specular color to be used for subsequent rendering
    * of primitives. The default specular color has a value
    * of <code>(0.1, 0.1, 0.1)</code>.
    * 
    * @param rgb array of length 3 specifying RGB values in
    * the range [0,1].
    */
   public void setSpecular (float[] rgb);
   
   /**
    * Sets the shininess parameter to be used for subsequent rendering
    * of primitives. The value should be in the range [0,128], and
    * the default value is 32.
    * 
    * @param s shininess parameter
    */
   public void setShininess (float s);
   
   /**
    * Sets the alpha value for the existing diffuse color.
    * 
    * @param a alpha value, in the range [0,1].
    */
   public void setAlpha (float a);
   
//   NOT USED
//   void setMaterial (float[] rgba);
   
   // USED ONLY IN GLVIEWER
   public void setMaterial (
      float[] frontRgba, float[] backRgba, float shininess, boolean selected); 

   /**
    * Sets the front diffuse and ambient colors to the point color in
    * <code>props</code>, or to the selection color if <code>selected</code> is
    * <code>true</code> and selection highlighting equals {@link
    * SelectionHighlighting#COLOR}. The
    * shininess is set to <code>props.getShininess()</code>, while
    * the back color is set to <code>null</code> and 
    * the emission and specular colors are set to their default values.
    * Summarizing:
    * <pre>
    *   frontColor = props point color or selection color
    *   backColor = null
    *   shininess = props shininess
    *   emission = default value
    *   specular = default value
    * </pre>
    *
    * @param props supplies the shininess and point color values
    * @param selected if <code>true</code> and if color selection highlighting
    * is enabled, causes the diffuse and ambient colors to be set to the
    * selection color.
    */
   public void setPointLighting (RenderProps props, boolean selected);
   
   /**
    * Sets the front diffuse and ambient colors to the line color in
    * <code>props</code>, or to the selection color if <code>selected</code> is
    * <code>true</code> and selection highlighting equals {@link
    * SelectionHighlighting#COLOR}. The
    * shininess is set to <code>props.getShininess()</code>, while
    * the back color is set to <code>null</code> and 
    * the emission and specular colors are set to their default values.
    * Summarizing:
    * <pre>
    *   frontColor = props line or selection color
    *   backColor = null
    *   shininess = props shininess
    *   emission = default value
    *   specular = default value
    * </pre>
    *
    * @param props supplies the shininess and line color values
    * @param selected if <code>true</code> and if color selection highlighting
    * is enabled, causes the diffuse and ambient colors to be set to the
    * selection color.
    */
   public void setLineLighting (RenderProps props, boolean selected);
   
   /**
    * Sets the front diffuse and ambient colors to the edge color in
    * <code>props</code>, or to the selection color if <code>selected</code> is
    * <code>true</code> and selection highlighting equals {@link
    * SelectionHighlighting#COLOR}. If the edge color is <code>null</code>
    * then the line color in <code>props</code> is used instead. The
    * shininess is set to <code>props.getShininess()</code>, while
    * the back color is set to <code>null</code> and 
    * the emission and specular colors are set to their default values.
    * Summarizing:
    * <pre>
    *   frontColor = props edge (or line) color or selection color
    *   backColor = null
    *   shininess = props shininess
    *   emission = default value
    *   specular = default value
    * </pre>
    *
    * @param props supplies the shininess and edge (or line) color values
    * @param selected if <code>true</code> and if color selection highlighting
    * is enabled, causes the diffuse and ambient colors to be set to the
    * selection color.
    */
   public void setEdgeLighting (RenderProps props, boolean selected);

   /**
    * Sets the front and back diffuse and ambient colors to the face color
    * and back color in <code>props</code>, or to the selection color if
    * <code>selected</code> is <code>true</code> and selection highlighting
    * equals {@link SelectionHighlighting#COLOR}. If the back color of
    * <code>props</code> is <code>null</code>, this will just cause the
    * renderer's back color to also be set to <code>null</code>.
    * The shininess is set to <code>props.getShininess()</code>, while
    * the emission and specular colors are set to their
    * default values.  Summarizing:
    * <pre>
    *   frontColor = props face color or selection color
    *   backColor = props back color or selection color
    *   shininess = props shininess
    *   emission = default value
    *   specular = default value
    * </pre>
    *
    * @param props supplies the shininess and front and back color values
    * @param selected if <code>true</code> and if color selection highlighting
    * is enabled, causes the diffuse and ambient colors to be set to the
    * selection color.
    */
   public void setFaceLighting (RenderProps props, boolean selected);
   
   /**
    * Sets the front and back diffuse and ambient colors to
    * <code>frontRgba</code> and the back color in <code>props</code>
    * (respectively), or to the selection color if <code>selected</code> is
    * <code>true</code> and selection highlighting equals {@link
    * SelectionHighlighting#COLOR}. If the back color of <code>props</code> is
    * <code>null</code>, this will just cause the renderer's back color to also
    * be set to <code>null</code>.  The shininess is set to
    * <code>props.getShininess()</code>, while the emission and specular colors
    * are set to their default values.  Summarizing:
    * <pre>
    *   frontColor = frontRgba or selection color
    *   backColor = props back color or selection color
    *   shininess = props shininess
    *   emission = default value
    *   specular = default value
    * </pre>
    *
    * @param props supplies the shininess and back color values
    * @param frontRgba an array of length 3 or 4 specifying RGB or RGBA values
    * for the front color the range [0,1]. Alpha is only applied to the diffuse
    * color and is set to <code>props.getAlpha()</code> if not specified.
    * @param selected if <code>true</code> and if color selection highlighting
    * is enabled, causes the diffuse and ambient colors to be set to the
    * selection color.
    */
   public void setFaceLighting (
      RenderProps props, float[] frontRgba, boolean selected);
      
   /**
    * Sets the front diffuse and ambient colors to <code>frontRgba</code>, or
    * to the selection color if <code>selected</code> is <code>true</code> and
    * selection highlighting equals {@link SelectionHighlighting#COLOR}. If the
    * back color of <code>props</code> is <code>null</code>, this will just
    * cause the renderer's back color to also be set to <code>null</code>.  The
    * shininess is set to <code>props.getShininess()</code>, while the back
    * color is set to <code>null</code> and the emission
    * and specular colors are set to their default values.  Summarizing:
    * <pre>
    *   frontColor = frontRgba or selection color
    *   backColor = null
    *   shininess = props shininess
    *   emission = default value
    *   specular = default value
    * </pre>
    *
    * @param props supplies the shininess value
    * @param frontRgba an array of length 3 or 4 specifying RGB or RGBA values
    * for the front color the range [0,1]. Alpha is only applied to the diffuse
    * color and is set to <code>props.getAlpha()</code> if not specified.
    * @param selected if <code>true</code> and if color selection highlighting
    * is enabled, causes the diffuse and ambient colors to be set to the
    * selection color.
    */
   public void setPropsMaterial (
      RenderProps props, float[] frontRgba, boolean selected);

   /**
    * Sets the shading appropriate to the point style specified in
    * <code>props</code>. If <code>props.getPointStyle()</code> is {@link
    * PointStyle#POINT}, then shading is set to {@link Shading#NONE};
    * otherwise, it is set to <code>props.getShading()</code>.
    * 
    * @param props properties giving the shading and point style
    */
   public void setPointShading (RenderProps props);
   
   /**
    * Sets the shading appropriate to the line style specified in
    * <code>props</code>. If <code>props.getLineStyle()</code> is {@link
    * LineStyle#LINE}, then shading is set to {@link Shading#NONE};
    * otherwise, it is set to <code>props.getShading()</code>.
    * 
    * @param props properties giving the shading and line style
    */
   public void setLineShading (RenderProps props);
   
   // XXX will these be in use?
   public void setMaterial (Material material, boolean selected);
   
   // USED ONLY IN GLViewer and GLXViewer
   public void setMaterial (Material material, float[] diffuseColor,
      boolean selected);
   
//   // NOT USED
//   public void setMaterial (Material frontMaterial, float[] frontDiffuse,
//      Material backMaterial, float[] backDiffuse, 
//      boolean selected);

//   public void setMaterialAndShading (
//      RenderProps props, Material mat, boolean selected);
   
//   public void setMaterialAndShading (
//      RenderProps props, Material mat, float[] diffuseColor, boolean selected);
   
//   public void setMaterialAndShading (
//      RenderProps props, Material frontMaterial, float[] frontDiffuse,
//      Material backMaterial, float[] backDiffuse, 
//      boolean selected);
   
//   public void updateMaterial (
//      RenderProps props, Material material, boolean selected);
   
//   public void updateMaterial (
//      RenderProps props, Material mat, float[] diffuseColor, boolean selected);
   
//   public void updateMaterial (
//      RenderProps props, Material frontMaterial, float[] frontDiffuse, 
//      Material backMaterial, float[] backDiffuse, boolean selected);
   
   //===============================================================================
   // BASIC PRIMITIVE DRAWING
   //===============================================================================
   
   // XXX maybe phase this out?  Might need some way to set point size though

   
   
   /**
    * Draw a set of points (the plural version of drawPoint(float[])).
    */
   // REMOVE
   public void drawPoints (Iterable<float[]> points);
   
   // REMOVE
   public void drawPoints(Iterable<float[]> points, Iterable<float[]> normals);
   
   // REMOVE
   public void drawLines(Iterable<float[]> coords);
   
   // REMOVE
   public void drawLines(Iterable<float[]> coords, Iterable<float[]> normals);
   
   // REMOVE
   public void drawLineStrip(Iterable<float[]> coords, Iterable<float[]> normals);
   
   // REMOVE
   void drawTriangle (float[] p0, float[] n0, float[] p1, float[] n1, float[] p2, float[] n2);

   // REMOVE
   void drawTriangles (Iterable<float[]> points);
   
   // REMOVE
   void drawTriangles (Iterable<float[]> points, Iterable<float[]> normals);
   
   //==========================================================================
   // RENDER OBJECTS
   //==========================================================================
   
   /**
    * Draws all the triangles in the currently active triangle group of the
    * specified render object, using the current material and shading.
    *
    * @param robj render object
    */
   public void drawTriangles (RenderObject robj);
   
   /**
    * Draws all the lines in the currently active line group of the
    * specified render object, using the current material and shading.
    * 
    * @param robj render object
    */
   public void drawLines (RenderObject robj);
   
   /**
    * Draws all the lines in the currently active line group of the specified
    * render object, using the current material and shading.  The lines are
    * drawn either as pixel-based lines or as solid primitives, according to
    * the specified line style. For lines drawn using the style {@link
    * LineStyle#LINE}, the argument <code>rad</code> gives the line width,
    * whereas for solid primitives ({@link LineStyle#CYLINDER}, {@link
    * LineStyle#SOLID_ARROW}, {@link LineStyle#TAPPERD_ELLIPSOID}), it gives
    * the nominal radius.
    * 
    * @param robj render object
    * @param style line style to use for drawing
    * @param rad radius for solid lines or width for pixel-based lines
    */
   public void drawLines (RenderObject robj, LineStyle style, double rad);
   
   /**
    * Draws all the points in the currently active point group of the
    * specified render object, using the current material and shading.
    * 
    * @param robj render object
    */
   public void drawPoints (RenderObject robj);
   
   /**
    * Draws all the points in the currently active point group of the specified
    * render object, using the current material and shading. The points are
    * drawn either as pixel-based points or as solid spheres, according to the
    * specified points style. For lines drawn using the style {@link
    * PointStyle#POINT}, the argument <code>rad</code> gives the point size,
    * whereas for {@link PointStyle#SPHERE} it gives the sphere radius.
    * 
    * @param robj render object
    * @param style point style to use for drawing
    * @param rad radius for spheres or width for pixel-based points
    */
   public void drawPoints (RenderObject robj, PointStyle style, double rad);
   
   // FINISH
   /**
    * 
    * @param robj render object
    * @param mode
    */
   public void drawVertices (RenderObject robj, VertexDrawMode mode);
   
   /**
    * Draw all currently active groups of points, lines, and triangles in the
    * specified render object, using the current material and shading.
    * 
    * @param robj render object
    */
   public void draw (RenderObject robj);
   
   // FINISH
   /**
    * 
    * @param key
    * @return
    */
   public RenderObject getSharedObject(Object key);
   
   // FINISH
   /**
    * 
    * @param key
    * @param r
    */
   public void addSharedObject(Object key, RenderObject r);

   // FINISH
   /**
    * 
    * @param key
    */
   public void removeSharedObject(Object key);
   
   // MATRICES
   
   /**
    * Saves the model matrix by pushing it onto the model matrix stack.
    */
   public void pushModelMatrix();
   
   /**
    * Translates the current model matrix by applying a translation
    * in the current model coordinate frame. If the model matrix is described
    * by the affine transform
    * <pre>
    *  [  M   p  ]
    *  [         ]
    *  [  0   1  ]
    * </pre>
    * where <code>M</code> is a 3 X 3 matrix and <code>p</code> is a 3 X 1
    * translation vector, and if <code>t</code> is the translation vector,
    * then this method sets the model matrix to
    * <pre>
    *  [  M   M t + p ]
    *  [              ]
    *  [  0      1    ]
    * </pre> 
    * 
    * @param tx translation along x
    * @param ty translation along y
    * @param tz translation along z
    */
   public void translateModelMatrix (double tx, double ty, double tz);
   
   /**
    * Scales the current model matrix. If the model matrix is described
    * by the affine transform
    * <pre>
    *  [  M   p  ]
    *  [         ]
    *  [  0   1  ]
    * </pre>
    * where <code>M</code> is a 3 X 3 matrix and <code>p</code> is a 3 X 1
    * translation vector, and <code>s</code> is the scale factor, then
    * this method sets the model matrix to 
    * <pre>
    *  [ s M  p  ]
    *  [         ]
    *  [  0   1  ]
    * </pre> 
    *  
    * @param s scale factor
    */
   public void scaleModelMatrix (double s);
   
   /**
    * Post-multiplies the model matrix by the specified transform X,
    * which may be either a {@link RigidTransform3d} or an
    * {@link AffineTransform3d}. If the model matrix is described
    * by the affine transform
    * <pre>
    *  [  M   p  ]
    *  [         ]
    *  [  0   1  ]
    * </pre>
    * where <code>M</code> is a 3 X 3 matrix and <code>p</code> is a 3 X 1
    * translation vector, and the transform X is described by
    * <pre>
    *  [  A   b  ]
    *  [         ]
    *  [  0   1  ]
    * </pre>
    * then this method sets the model matrix to
    * <pre>
    *  [ M A   M b + p ]
    *  [               ]
    *  [  0       1    ]
    * </pre>
    *  
    * @param X transform to multiply the model matrix by
    */  
   public void mulModelMatrix (AffineTransform3dBase X);
   
   /**
    * Restores the model matrix by popping it off the model matrix stack
    * 
    * @return <code>false</code> if the model matrix stack is empty
    */
   public boolean popModelMatrix();

   // FINISH
   public RigidTransform3d getEyeToWorld();
   
   // FINISH: do we need?
   /**
    * Saves the view matrix by pushing it onto the view matrix stack.
    */
   public void pushViewMatrix();
   
   // FINISH: do we need?
   /**
    * Restores the view matrix by popping it off the view matrix stack
    * 
    * @return <code>false</code> if the view matrix stack is empty
    */
   public boolean popViewMatrix();
   
   // FINISH: do we need this?
   /**
    * Saves the projection matrix by pushing it onto the projection matrix 
    * stack.
    */
   public void pushProjectionMatrix();
   
   // FINISH: do we need this?
   /**
    * Restores the projection matrix by popping it off the projection 
    * matrix stack
    * 
    * @return <code>false</code> if the projection matrix stack is empty
    */
   public boolean popProjectionMatrix();
   
   /**
    * Flag requesting that all display lists be cleared
    */
   public static int CLEAR_RENDER_CACHE = 0x100;

   /**
    * Flag requesting components refresh any custom rendering info
    */
   public static int UPDATE_RENDER_CACHE = 0x200;
   
   /**
    * Returns true if the renderer is currently performing a selection
    * render. When this is the case, the application should avoid calls
    * affecting color and lighting.
    *
    * @return true if the renderer is in selection mode.
    */
   public boolean isSelecting();
   
   /**
    * Returns the selection highlighting method used by this renderer.
    * This specifies how objects which are indicated to be <i>selected</i>
    * are drawn by the renderer in a way so that they stand out. A
    * value of {@link SelectionHighlighting#NONE} indicates that
    * no selection highlighting is enabled.
    * 
    * @return current selection highlighting method.
    */
   public SelectionHighlighting getSelectionHighlighting();

   /**
    * Returns the color that is used to highlight selected objects when
    * the selection highlighting method is {@link SelectionHighlighting#COLOR}.
    * 
    * @param rgba array of length 3 or 4 in which the RGB or RGBA components
    * of the selection color are returned.
    */
   public void getSelectionColor (float[] rgba);
   
   /**
    * Begins a selection query with the {\it query identifier}
    * <code>qid</code>.  If the rendering that occurs between this call and the
    * subsequent call to {@link #endSelectionQuery} results in anything being
    * rendered within the selection window, a {\it selection hit} will be
    * generated for <code>qid</code>.
    *
    * If called within the <code>render</code> method for a {@link
    * maspack.render.GL.GLSelectable}, then <code>qid</code> must lie in the range
    * 0 to <code>numq</code>-1, where <code>numq</code> is the value returned
    * by {@link maspack.render.GL.GLSelectable#numSelectionQueriesNeeded
    * GLSelectable.numSelectionQueriesNeeded()}.  Selection queries cannot be
    * nested, and a given query identifier should be used only once.
    *
    * @param qid identifier for the selection query
    * @see #endSelectionQuery
    */
   public void beginSelectionQuery (int qid);

   /**
    * Ends a selection query that was initiated with a call to
    * {@link #beginSelectionQuery}.
    *
    * @see #beginSelectionQuery
    */
   public void endSelectionQuery ();

   /**
    * Begins selection for a {@link maspack.render.GL.GLSelectable}
    * <code>s</code>that
    * manages its own selection; this call should
    * be used in place of {@link #beginSelectionQuery} for such objects.
    * Selectables that manage their own selection are identified by
    * having a value <code>numq</code> >= 0, where <code>numq</code>
    * is the value returned by
    * {@link maspack.render.GL.GLSelectable#numSelectionQueriesNeeded
    * GLSelectable#numSelectionQueriesNeeded{}}.
    * The argument <code>qid</code> is the current selection query identifier,
    * which after the call should be increased by <code>numq</code>.
    *
    * @param s Selectable that is managing its own sub-selection
    * @param qid current selection query identifier
    */
   public void beginSubSelection (GLSelectable s, int qid);

   /**
    * Ends object sub-selection that was initiated with a call
    * to {@link #beginSubSelection}.
    *
    * @see #endSubSelection
    */
   public void endSubSelection ();

   /**
    * Sets a selection filter for the renderer. This restricts which objects
    * are actually rendered when a selection render is performed, and therefore
    * restricts which objects can actually be selected. This allows selection
    * of objects that might otherwise be occluded within a scene.
    *
    * @param filter Selection filter to be applied
    */
   public void setSelectionFilter (GLSelectionFilter filter);

   /**
    * Returns the current selection filter for the renderer, if any.
    *
    * @return current selection filter, or <code>null</code> if there is none.
    */
   public GLSelectionFilter getSelectionFilter ();

   /**
    * Returns true if <code>s</code> is selectable in the current selection
    * context. This will be the case if <code>s.isSelectable()</code> returns
    * <code>true</code>, and <code>s</code> also passes whatever selection
    * filter might currently be set in the renderer.
    */
   public boolean isSelectable (GLSelectable s);
   
   // FINISH
   public void rerender();

   // FINISH
   /**
    * Re-draw contents of renderer
    */
   public void repaint();

   /**
    * Begins draw mode. This is analogous to <code>glBegin()</code> in the
    * old GL specification. Once in draw mode, the application can specify
    * the vertices (and if necessary, normals) that are used to draw the
    * primitive that is specified by the mode parameter. 
    * 
    * @param mode specifies the primitive to be built while in draw mode.
    * @throws IllegalStateException if the renderer is currently in draw mode
    */
   public void beginDraw (VertexDrawMode mode);

   /**
    * Adds a vertex to a primitive being drawn while in draw mode. 
    * 
    * @param x vertex x coordinate
    * @param y vertex y coordinate
    * @param z vertex z coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void addVertex (float x, float y, float z);

   /**
    * Adds a vertex to a primitive being drawn while in draw mode. 
    * 
    * @param x vertex x coordinate
    * @param y vertex y coordinate
    * @param z vertex z coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void addVertex (double x, double y, double z);

   /**
    * Adds a vertex to a primitive being drawn while in draw mode. 
    * 
    * @param pnt coordinates for the vertex
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void addVertex (Vector3d pnt);

   /**
    * Sets the normal to be associated with the next vertex to be
    * added while in draw mode. This normal will remain in effect until
    * a subsequent <code>setNormal</code> call. The normal does not
    * need to be normalized.
    * 
    * @param x normal x coordinate
    * @param y normal y coordinate
    * @param z normal z coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setNormal (float x, float y, float z);

   /**
    * Sets the normal to be associated with the next vertex to be
    * added while in draw mode. This method is functionally equivalent to
    * {@link #setNormal(float,float,float)}. 
    * 
    * @param x normal x coordinate
    * @param y normal y coordinate
    * @param z normal z coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setNormal (double x, double y, double z);

   /**
    * Sets the normal to be associated with the next vertex to be
    * added while in draw mode. This method is functionally equivalent to
    * to {@link #setNormal(float,float,float)}. 
    * 
    * @param nrm normal coordinates
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setNormal (Vector3d nrm);

   /**
    * Ends draw mode. This is analogous to <code>glEnd()</code> in the old
    * GL specification.
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void endDraw();
   
}
