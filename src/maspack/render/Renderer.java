/**
 * Copyright c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
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
   
   /**
    * Specifies how vertex coloring or textures are combined with material
    * coloring
    */
   public enum ColorMixing {

      /**
       * Vertex coloring or textures are ignored
       */
      NONE,  

      /**
       * Vertex coloring or textures replace the diffuse coloring
       */
      REPLACE,

      /**
       * Vertex coloring or textures are multiplicatively combined
       * with the diffuse coloring
       */
      MODULATE,

      /**
       * Vertex coloring or textures are combined based on the diffuse
       * color's alpha value
       */
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
    * Flag requesting that faces of a mesh be sorted before rendering
    */
   public static int SORT_FACES = 0x8;
   
   /**
    * Defines various vertex-based primitives
    */
   public enum DrawMode {
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
   public enum HighlightStyle {
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
    * Specifies the current shading style.
    */
   public static enum Shading {
      /**
       * Flat shading, whereby a single normal is used for each
       * primitive
       */
      FLAT,

      /**
       * Smoothing shading based on the Gouraud shading model
       */
      GOURAUD,

      /**
       * Smooth shading based on the Phong shading model
       */
      PHONG,

      /**
       * No shading. This is equivalent to disabling lighting
       */
      NONE
   }

   /**
    * For some drawing primitives, specifies the rendering style for points.
    */
   public static enum PointStyle {
      /**
       * Draw points as solid spheres
       */
      SPHERE,

      /**
       * Draw points as cubes
       */
      CUBE,
      
      /**
       * Draw points using pixel-based primitives
       */
      POINT
   }

   /**
    * For some drawing primitives, specifies the rendering style for line
    * segments.
    */
   public static enum LineStyle {
      /**
       * Draw lines using pixel-based primitives
       */
      LINE,

      /**
       * Draw lines as solid cylinders
       */
      CYLINDER,

      /**
       * Draw lines as solid arrows
       */
      SOLID_ARROW,

      /**
       * Draw lines as solid spindles
       */
      SPINDLE
   }

   /**
    * When rendering triangles, specifies which sides are drawn, where
    * <i>front</i> and <i>back</i> correspond to the sides associated with
    * counter-clockwise and clockwise vertex ordering, respectively.
    */
   public static enum FaceStyle {
      /**
       * Draw the back side
       */
      BACK,

      /**
       * Draw the front side
       */
      FRONT,

      /**
       * Draw both sides
       */
      FRONT_AND_BACK, 

      /**
       * Draw neither side
       */
      NONE
   }

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
    * Returns the displacement distance of the center point, in a plane
    * parallel to the view plane, that corresponds to a screen displacement of
    * one pixel.  The displacement is given with respect to model coordinates.
    *
    * @return displacement distance corresponding to one pixel
    */
   public double centerDistancePerPixel ();
   
   /**
    * Returns the displacement distance of a point <code>p</code>, in a plane
    * parallel to the view plane, that corresponds to a screen displacement
    * of one pixel.  Both <code>p</code> and the displacement are given with
    * respect to model coordinates.
    *
    * @param p point undergoing the displacement 
    * @return displacement distance corresponding to one pixel
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
    * Returns the distance, in model coordinates, from the eye to view plane
    * (which corresponds to the far clip plane). This is a positive number.
    *
    * @return distance to the near clip plane
    * @see #getFarPlaneDistance
    */
   public double getViewPlaneDistance();

   /**
    * Returns the distance, in model coordinates, from the eye to the far clip
    * plane. This is a positive number.
    *
    * @return distance to the far clip plane
    * @see #getViewPlaneDistance
    */
   public double getFarPlaneDistance();
   
   /**
    * Returns the current size for rendering points, in pixels.
    * 
    * @return current point size (in pixels).
    * @see #setPointSize
    */
   public float getPointSize();
   
   /**
    * Sets the size for rendering points, in pixels. The default size is 1.
    * 
    * @param size new point size (in pixels).
    * @see #getPointSize
    */
   public void setPointSize(float size);
   
//   /**
//    * Sets the size for rendering points to its default value, which is 1.
//    * 
//    * @see #setPointSize
//    * @see #getPointSize
//    */
//   public void setDefaultPointSize();
   
   /**
    * Returns the current width for rendering lines, in pixels.
    * 
    * @return current line width (in pixels).
    * @see #setLineWidth
    */
   public float getLineWidth();
   
   /**
    * Sets the width for rendering lines, in pixels. The default size is 1.
    * 
    * @param width new line width (in pixels).
    * @see #getLineWidth
    */
   public void setLineWidth (float width);
   
//   /**
//    * Sets the width for rendering points to its default value, which is 1.
//    * 
//    * @see #setLineWidth
//    * @see #getLineWidth
//    */
//   public void setDefaultLineWidth();

   /**
    * Returns the current mode for rendering faces.
    * 
    * @return current face rendering mode
    * @see #setFaceStyle
    */
   public FaceStyle getFaceStyle ();

   /**
    * Sets the style for rendering faces. This determines whether
    * the front and/or back of each face is rendered. The default value
    * is {@link FaceStyle#FRONT}.
    * 
    * @param style new face style
    * @return previous face style
    * @see #getFaceStyle
    */
   public FaceStyle setFaceStyle (FaceStyle style);

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
    * @return previous color interpolation mode
    */
   public ColorInterpolation setColorInterpolation (ColorInterpolation interp);
   
   /**
    * Queries whether or not a specified method for combining vertex coloring
    * and material coloring is supported by this Renderer.
    *
    * @return <code>true</code> if the color mixing method is supported
    */
   public boolean hasVertexColorMixing (ColorMixing cmix);

   /**
    * Returns the method used for combining vertex coloring and material
    * coloring.
    *
    * @return current color mixing method
    */
   public ColorMixing getVertexColorMixing();

   /**
    * Sets the method used for combining vertex coloring and material coloring.
    * This Renderer may not support all methods. If a method is not supported,
    * then the color mixing method will remain unchanged.  Applications can use
    * {@link #hasColorMixing} to test whether a method is suuported.
    *
    * @param cmix new color mixing method
    * @return previous color mixing method
    */
   public ColorMixing setVertexColorMixing (ColorMixing cmix); 

   /**
    * Returns the current shading model used by this renderer. A shading model
    * of {@link Shading#NONE} turns off lighting and causes primitives to be
    * rendered in solid colors, using the current diffuse color.
    *
    * @return current shading model
    */
   public Shading getShading();

   /**
    * Sets the shading model used by this renderer. The default value is {@link
    * Shading#FLAT}, in which one normal is used per primitive.  A shading
    * model of {@link Shading#NONE} turns off lighting and causes primitives to
    * be rendered in solid colors, using the current diffuse color.
    *
    * @param shading new shading model to be used
    * @return previous shading model
    */
   public Shading setShading (Shading shading);

//   /**
//    * Sets the default shading model for this renderer, which is
//    * {@link Shading#FLAT}.
//    */
//   public void setDefaultShading ();

//   /**
//    * Returns <code>true</code> if lighting is currently enabled.
//    * Lighting is enabled by default.
//    * 
//    * @return <code>true</code> if lighting is enabled.
//    * @see #setLightingEnabled
//    */
//   public boolean isLightingEnabled();
//
//   /**
//    * Enables or disables lighting. Disabling lighting is equivalent
//    * in effect to setting shading to {@link Shading#NONE}. However,
//    * disabling the lighting does not affect the current shading
//    * value, which takes effect again as soon as lighting is re-enabled.
//    *
//    * @param enable specifies whether to enable or disable lighting.
//    * @return previous lighting enabled setting
//    */
//   public boolean setLightingEnabled (boolean enable);   

   // Drawing primitives

   /**
    * Draws a single point located at <code>pnt</code> in model coordinates,
    * using the current point size, material, and shading. Since no normal is
    * specified, this method should generally be called with either lighting
    * disabled or with shading set to {@link Shading#NONE}.
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
   
//   // REMOVE
//   /**
//    * Draws a single point located at <code>pnt</code>, and with a normal
//    * <code>nrm</code>, in model coordinates, using the current point size,
//    * material, and shading. 
//    *
//    * @param pnt array (of length 3) giving the point location
//    * @param nrm array (of length 3) giving the point normal
//    */
//   public void drawPoint (float[] pnt, float[] nrm);
   
//   // REMOVE
//   public void drawPoint (float x, float y, float z);

   /**
    * Draws a single line between two points in model coordinates,
    * using the current line width, material, and shading. Since no normal are
    * specified, this method should generally be called with either lighting
    * disabled or with shading set to {@link Shading#NONE}.
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
   
//   // REMOVE
//   public void drawLine (
//      float x0, float y0, float z0, float x1, float y1, float z1);
   
   /**
    * Draws a single line between two points in model coordinates,
    * using the current line width, material, and shading.  This method
    * is functionally equivalent to {@link #drawLine(Vector3d,Vector3d)}.
    * 
    * @param pnt0 array (of length 3) giving the first point
    * @param pnt1 array (of length 3) giving the second point
    */
   public void drawLine (float[] pnt0, float[] pnt1);

//   // REMOVE
//   /**
//    * Draws a single line between two points, with specified normals, in model
//    * coordinates, using the current line width, material, and shading.
//    * 
//    * @param pnt0 array (of length 3) giving the first point
//    * @param nrm0 array (of length 3) giving the first normal
//    * @param pnt1 array (of length 3) giving the second point
//    * @param nrm1 array (of length 3) giving the second normal
//    */
//   public void drawLine (
//      float[] pnt0, float[] nrm0, float[] pnt1, float[] nrm1);

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
    * @see #setSurfaceResolution
    */
   public int getSurfaceResolution();
   
   /**
    * Sets the resolution used for creating mesh representations
    * of curved solids. The number represents the number of line
    * segments that would be used to approximate a circle. This
    * level of resolution is then employed to create the mesh.
    * 
    * @param nsegs resolution for curved surfaces
    * @return previous resolution
    */
   public int setSurfaceResolution (int nsegs);
   
   /**
    * Draws a sphere with a specified radius centered at a point
    * in model coordinates, using the current shading and material.
    * The point is located in model coordinates. The resolution
    * of the sphere is specified by {@link #getSurfaceResolution}.
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
    * Draws a spindle between two points in model coordinates, 
    * using the current shading and material. The resolution
    * is specified by {@link #getSurfaceResolution}.
    * 
    * @param pnt0 first end point
    * @param pnt1 second end point
    * @param rad radius of the spindle
    */
   public void drawSpindle (float[] pnt0, float[] pnt1, double rad);

//   // REPLACE with drawLine (style)?
//   public void drawCylinder (float[] pnt0, float[] pnt1, double rad);

   /**
    * Draws a cylinder between two points in model coordinates, 
    * using the current shading and material. The resolution
    * is specified by {@link #getSurfaceResolution}.
    * 
    * @param pnt0 first end point
    * @param pnt1 second end point
    * @param rad radius of the cylinder
    * @param capped if <code>true</code>, indicates that the cylinder
    * should have a solid cap on each end
    */
   public void drawCylinder (
      float[] pnt0, float[] pnt1, double rad, boolean capped);
   
   /**
    * Draws a cylinder between two points in model coordinates, 
    * using the current shading and material. The resolution
    * is specified by {@link #getSurfaceResolution}.
    * 
    * @param pnt0 first end point
    * @param pnt1 second end point
    * @param rad radius of the cylinder
    * @param capped if <code>true</code>, indicates that the cylinder
    * should have a solid cap on each end
    */
   public void drawCylinder (
      Vector3d pnt0, Vector3d pnt1, double rad, boolean capped);

   /**
    * Draws a cone between two points in model coordinates, 
    * using the current shading and material. A cone is like a cylinder,
    * except that it can have different radii at the end points. The resolution
    * is specified by {@link #getSurfaceResolution}.
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
    * The resolution is specified by {@link #getSurfaceResolution}.
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
    * highlighting is {@link HighlightStyle#COLOR}, in which case
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
    * {@link HighlightStyle#COLOR}, then the point will
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
    * {@link HighlightStyle#COLOR}, then the line will
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
      RenderProps props, float[] pnt0, float[] pnt1, boolean capped,
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
    * {@link HighlightStyle#COLOR}, then the strip will
    * be drawn using the selection color rather than the line color
    * specified in <code>props</code>.
    * 
    * @param props render properties used for drawing the strip
    * @param pnts list of points used for drawing the strip
    * @param style line style to be used for the strip
    * @param selected if <code>true</code>, indicates that selection
    * highlighting, if enabled, should be applied to the strip
    */
   public void drawLineStrip (
      RenderProps props, Iterable<float[]> pnts, 
      LineStyle style, boolean selected);   

   /**
    * Gives the direction, in world coordinates, of a vector that
    * is perpendicular to the screen and points towards the viewer.
    * The corresponds to the Z direction in eye coordinates.
    * 
    * @return current z direction of the eye frame
    */
   public Vector3d getEyeZDirection();
   
   /**
    * Puts this Renderer into 2D rendering mode, or returns <code>false</code>
    * if 2D rendering is not supported. This method behaves identically
    * to {@link #begin2DRendering(double,double)}, only with the existing
    * screen width and height used for <code>w</code> and <code>h</code>.
    *
    * @return <code>true</code> if 2D rendering is supported
    * @see #getScreenWidth
    * @see #getScreenHeight
    * @see #has2DRendering
    * @throws IllegalStateException if this Renderer is currently in 2D
    * rendering mode.
    */
   public boolean begin2DRendering();
   
   /**
    * Puts this Renderer into 2D rendering mode, or returns <code>false</code>
    * if 2D rendering is not supported. If 2D rendering is supported, then the
    * depth buffer is disabled, the model and view matrices are set to the 
    * identity, and the projection matrix is redefined to provide an 
    * orthographic projection with the world frame located at the bottom
    * left corner of the screen with the x axis horizontal and
    * pointing to the right, and the y axis vertical. The scaling is
    * set so that the width and height of the screen map to 
    * the ranges (0, w) and (0, h), respectively. If 2D rendering is not
    * supported, then no changes are made to any of the matrices and this
    * method will have no effect.
    *
    * @param w width of the screen coordinates
    * @param h height of the screen coordinates
    * @return <code>true</code> if 2D rendering is supported
    * @see #has2DRendering
    * @throws IllegalStateException if this Renderer is currently in 2D
    * rendering mode.
    */
   public boolean begin2DRendering (double w, double h);

   /**
    * Returns <code>true</code> if this Renderer supports 2D rendering mode.
    *
    * @return <code>true</code> if 2D rendering is supported
    * @see #begin2DRendering()
    * @see #begin2DRendering(double,double)
    * @see #end2DRendering
    */
   public boolean has2DRendering();
   
   /**
    * Take this Renderer out of 2D rendering mode, by reenabling the depth
    * buffer and restoring the model and projection matrices to the values they
    * had when 2D rendering mode was first entered.
    *
    * @throws IllegalStateException if this Renderer is not currently in 2D
    * rendering mode.
    */
   public void end2DRendering();
   
   /**
    * Check whether the Renderer is currently in 2D rendering mode.
    * 
    * @return <code>true</code> if in 2D rendering mode.
    */
   public boolean is2DRendering();

   //===============================================================================
   // COLORS and Materials
   //===============================================================================
   
   /**
    * Sets the diffuse and ambient colors to the value specified by
    * <code>rgba</code>, and enables or disables selection highlighting
    * according to the value of <code>selected</code>.
    * If selection highlighting is enabled and the highlighting method
    * equals {@link HighlightStyle#COLOR}, the selection color will override
    * the diffuse/ambient color settings. This method also clears any 
    * back color that may be present. It is therefore equivalent to calling
    * <pre>
    *    setFrontColor (rgba);
    *    setBackColor (null);
    *    setSelectionHighlighting (selected);
    * </pre>
    * 
    * @param rgba array of length 3 or 4 specifying RGB or RGBA values in
    * the range [0,1]. Alpha is only applied to the diffuse color
    * and is assumed to be 1.0 if not specified.
    * @param selected if <code>true</code>, enables selection highlighting
    * <code>rgba</code>
    */
   public void setColor (float[] rgba, boolean selected);
   
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
    * Sets the diffuse and ambient colors to be used for subsequent rendering
    * of primitives. The method is functionally equivalent to
    * {@link #setColor(float[])}, with the RGBA values being obtained
    * from the color components of <code>color</code>.
    * 
    * @param color specifies RGBA color values.
    */
   public void setColor (Color color);
   
   /**
    * Sets the diffuse and ambient colors to be used for subsequent rendering
    * of primitives. The method is functionally equivalent to
    * {@link #setColor(float[])}, only with alpha assumed to be 1.
    * 
    * @param r red value
    * @param g green value
    * @param b blue value
    */
   public void setColor (float r, float g, float b);

   /**
    * Sets the diffuse and ambient colors to be used for subsequent rendering
    * of primitives. The method is functionally equivalent to
    * {@link #setColor(float[])}.
    * 
    * @param r red value
    * @param g green value
    * @param b blue value
    * @param a alpha value
    */
   public void setColor (float r, float g, float b, float a);

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
    * Returns the RGB or RGBA values for the front color as a float array.  The
    * application may supply the array via the argument
    * <code>rgba</code>. Otherwise, Otherwise, if <code>rgba</code> is
    * <code>null</code>, an array of length 4 will be allocated for returning
    * the RGBA values. The default RGBA values for the front color are
    * <code>(1, 1, 1, 1)</code>.
    *    
    * @param rgba optional storage of length 3 or 4 for returning either
    * the RGB or RGBA values.
    * @return RGB or RGBA values for the front color
    */
   public float[] getFrontColor (float[] rgba);
   
   /**
    * Optionally sets the diffuse and ambient colors used for the subsequent
    * rendering of the back faces of triangles. If <code>rgba</code> is set to
    * <code>null</code>, then the back color is cleared and back faces
    * will be rendered using the front color. By default, the back color is
    * <code>null</code>.
    * 
    * @param rgba <code>null</code> to clear back coloring, or
    * an array of length 3 or 4 specifying RGB or RGBA values in
    * the range [0,1]. Alpha is only applied to the diffuse color
    * and is assumed to be 1.0 if not specified.
    */
   public void setBackColor (float[] rgba);
   
   /**
    * Returns the RGB or RGBA values for the back color as a float array,
    * or <code>null</code> if no back color is set. 
    * The application may supply the array via the argument <code>rgba</code>. Otherwise, 
    * Otherwise, if <code>rgba</code> is <code>null</code>
    * and the back color is set, 
    * an array of length 4 will be allocated for returning the RGBA values.
    *    
    * @param rgba optional storage of length 3 or 4 for returning either
    * the RGB or RGBA values.
    * @return RGB or RGBA values for the back color, or <code>null</code>
    * if no back color is set.
    */
   public float[] getBackColor (float[] rgba);
   
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
    * Returns the RGB values for the emission color as a float array.
    * The application may supply the array via the argument <code>rgb</code>.
    * Otherwise, if <code>rgb</code> is <code>null</code>, 
    * an array will be allocated for returning the RGB values.
    *    
    * @param rgb optional storage for returning the RGB values.
    * @return RGB values for the emission color
    */
   public float[] getEmission (float[] rgb);
   
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
    * Returns the RGB values for the specular color as a float array.
    * The application may supply the array via the argument <code>rgb</code>.
    * Otherwise, if <code>rgb</code> is <code>null</code>, 
    * an array will be allocated for returning the RGB values.
    *    
    * @param rgb optional storage for returning the RGB values.
    * @return RGB values for the specular color
    */
   public float[] getSpecular (float[] rgb);
   
   /**
    * Sets the shininess parameter to be used for subsequent rendering
    * of primitives (see {@link #getShininess}. This should be in the 
    * range [0,128], and the default value is 32.
    * 
    * @param s shininess parameter
    */
   public void setShininess (float s);
   
   /**
    * Returns the current shininess parameter. This is the specular exponent 
    * of the lighting equation, and is in the range [0,128],
    * 
    * @return shininess parameter
    */
   public float getShininess();
   
   /**
    * Sets the alpha value for the front material.
    * 
    * @param a alpha value, in the range [0,1].
    */
   public void setFrontAlpha (float a);

   /**
    * Sets the diffuse and ambient colors to the point color in
    * <code>props</code>, and enables or disables selection highlighting
    * according to the value of <code>selected</code>.  The back color is set
    * to <code>null</code>, and the emission, shininess, and specular values
    * are set either from <code>props</code> or from default values.
    * 
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring} with
    * <code>props.getPointColor()</code> supplying the <code>rgba</code> value.
    * If selection highlighting is enabled and the highlighting method equals
    * {@link HighlightStyle#COLOR}, the selection color will override the
    * diffuse/ambient color settings.
    *
    * @param props supplies the shininess and point color values
    * @param selected if <code>true</code>, enables selection highlighting
    */
   public void setPointColoring (RenderProps props, boolean selected);
   
   /**
    * Sets the diffuse and ambient colors to the line color in
    * <code>props</code>, and enables or disables selection highlighting
    * according to the value of <code>selected</code>. The back color is set
    * to <code>null</code>, and the emission, shininess, and specular values
    * are set either from <code>props</code> or from default values.
    *
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring} with
    * <code>props.getLineColor()</code> supplying the <code>rgba</code> value.
    * If selection highlighting is enabled and the highlighting method equals
    * {@link HighlightStyle#COLOR}, the selection color will override the
    * diffuse/ambient color settings.
    *
    * @param props supplies the shininess and line color values
    * @param selected if <code>true</code>, enables selection highlighting
    */
   public void setLineColoring (RenderProps props, boolean selected);
   
   /**
    * Sets the diffuse and ambient colors to the edge color in
    * <code>props</code>, and enables or disables selection highlighting
    * according to the value of <code>selected</code>. If the edge color is
    * <code>null</code> then the line color in <code>props</code> is used
    * instead. The back color is set to <code>null</code>, and the emission,
    * shininess, and specular values are set either from <code>props</code> or
    * from default values.
    *
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring} with
    * <code>props.getEdgeColor()</code> or <code>props.getLineColor()</code>
    * supplying the <code>rgba</code> value.  If selection highlighting is
    * enabled and the highlighting method equals {@link HighlightStyle#COLOR},
    * the selection color will override the diffuse/ambient color settings.
    *
    * @param props supplies the shininess and edge (or line) color values
    * @param selected if <code>true</code>, enables selection highlighting
    */
   public void setEdgeColoring (RenderProps props, boolean selected);

   /**
    * Sets the diffuse and ambient colors to the face color in
    * <code>props</code>, and enables or disables selection highlighting
    * according to the value of <code>selected</code>. The back color will also
    * be set to the (possibly <code>null</code>) value of the back color in
    * <code>props</code>. If the back color is not <code>null</code>, then this
    * will be used to provide the coloring for back faces when they are
    * visible. The emission, shininess, and specular values are set either from
    * <code>props</code> or from default values.
    *
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring} with
    * <code>props.getFaceColor()</code> supplying the <code>rgba</code> value,
    * except that the back color is set from <code>props.getBackColor()</code>
    * instead of being set to <code>null</code>. If selection highlighting is
    * enabled and the highlighting method equals {@link HighlightStyle#COLOR},
    * the selection color will override the diffuse/ambient color settings.
    *
    * @param props supplies the shininess and front and back color values
    * @param selected if <code>true</code>, enables selection highlighting
    */
   public void setFaceColoring (RenderProps props, boolean selected);
   
   /**
    * Sets the diffuse and ambient colors to <code>rgba</code>, and enables or
    * disables selection highlighting according to the value of
    * <code>selected</code>. The back color will also be set to the (possibly
    * <code>null</code>) value of the back color in <code>props</code>. If the
    * back color is not <code>null</code>, then this will be used to provide
    * the coloring for back faces when they are visible. The emission,
    * shininess, and specular values are set either from <code>props</code> or
    * from default values.
    *
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring},
    * except that the back color is set from <code>props.getBackColor()</code>
    * instead of being set to <code>null</code>. If selection highlighting is
    * enabled and the highlighting method equals {@link HighlightStyle#COLOR},
    * the selection color will override the diffuse/ambient color settings.
    *
    * @param props supplies the shininess and back color values
    * @param rgba an array of length 3 or 4 specifying RGB or RGBA values
    * for the color the range [0,1]. Alpha is only applied to the diffuse
    * color and is set to <code>props.getAlpha()</code> if not specified.
    * @param selected if <code>true</code>, enables selection highlighting
    */
   public void setFaceColoring (
      RenderProps props, float[] rgba, boolean selected);
      
   /**
    * Sets the diffuse and ambient colors to <code>rgba</code>, and enables or
    * disables selection highlighting according to the value of
    * <code>selected</code>. If <code>rgba</code> only has a length of 3 then
    * the front alpha value is supplied by <code>props.getAlpha()</code>.  The
    * shininess is set to <code>props.getShininess()</code>, while the back
    * color is set to <code>null</code>, the emission color is set to its
    * default value, and the specular color is set to either
    * <code>props.getSpecular()</code>, or to its default value if the former
    * is <code>null</code>. This behavior is equivalent to the following:
    * <pre>
    *   setFrontColor (rgba);
    *   if (rgba.length == 3) {
    *      setFrontAlpha (props.getAlpha());
    *   }
    *   setBackColor (null);
    *   setShininess (props.getShininess());
    *   setEmission (DEFAULT_EMISSION_VALUE);
    *   specular = props.getSpecular();
    *   setSpecular (specular != null ? specular : DEFAULT_SPECULAR_VALUE);
    *   setSelectionHighlighting (selected)
    * </pre>
    * If selection highlighting is enabled and the
    * highlighting method equals {@link HighlightStyle#COLOR}, the selection
    * color will override the diffuse/ambient color settings.
    *
    * @param props supplies the shininess value
    * @param rgba an array of length 3 or 4 specifying RGB or RGBA values for
    * the color the range [0,1]. Alpha is only applied to the diffuse color and
    * is set to <code>props.getAlpha()</code> if not specified.
    * @param selected if <code>true</code>, enables selection highlighting
    */
   public void setPropsColoring (
      RenderProps props, float[] rgba, boolean selected);

   /**
    * Sets the shading appropriate to the point style specified in
    * <code>props</code>. If <code>props.getPointStyle()</code> is {@link
    * PointStyle#POINT}, then shading is set to {@link Shading#NONE};
    * otherwise, it is set to <code>props.getShading()</code>.
    * 
    * @param props properties giving the shading and point style
    * @return the previous shading setting
    */
   public Shading setPointShading (RenderProps props);
   
   /**
    * Sets the shading appropriate to the line style specified in
    * <code>props</code>. If <code>props.getLineStyle()</code> is {@link
    * LineStyle#LINE}, then shading is set to {@link Shading#NONE};
    * otherwise, it is set to <code>props.getShading()</code>.
    * 
    * @param props properties giving the shading and line style
    * @return the previous shading setting
    */
   public Shading setLineShading (RenderProps props);
   
   /**
    * Sets the shading to that shading specified by
    * <code>props.getShading()</code>.
    * 
    * @param props properties giving the shading
    * @return the previous shading setting
    */
   public Shading setPropsShading (RenderProps props);
   
   /**
    * Sets the color-map texture properties specified by
    * <code>props</code>
    * @param props properties giving texture information, null
    * to disable texture
    * @return the previous color map texture properties
    */
   public TextureMapProps setTextureMapProps(TextureMapProps props);
   
   /**
    * Sets the normal-mapping texture properties specified by
    * <code>props</code>
    * @param props properties giving texture information, null
    * to disable texture
    * @return the previous normal texture properties
    */
   public NormalMapProps setNormalMapProps(NormalMapProps props);
   
   /**
    * Sets the bump-mapping texture properties specified by
    * <code>props</code>
    * @param props properties giving texture information, null
    * to disable texture
    * @return the previous normal texture properties
    */
   public BumpMapProps setBumpMapProps(BumpMapProps props);
   
   //==========================================================================
   // RENDER OBJECTS
   //==========================================================================
   
   /**
    * Draws all the triangles in the active triangle group of the
    * specified render object, using the current material and shading.
    *
    * @param robj render object
    */
   public void drawTriangles (RenderObject robj);
   
   /**
    * Draws all the triangles in the specified triangle group of the
    * render object, using the current material and shading.
    *
    * @param robj render object
    * @param gidx triangle group index
    */
   public void drawTriangles (RenderObject robj, int gidx);
   
   /**
    * Draws all the lines in the active line group of the
    * specified render object, using the current material and shading.
    * 
    * @param robj render object
    */
   public void drawLines (RenderObject robj);
   
   /**
    * Draws all the lines in the specified line group of the
    * render object, using the current material and shading.
    * 
    * @param robj render object
    * @param gidx line group index
    */
   public void drawLines (RenderObject robj, int gidx);
   
   /**
    * Draws all the lines in the active line group of the specified
    * render object, using the current material and shading.  The lines are
    * drawn either as pixel-based lines or as solid primitives, according to
    * the specified line style. For lines drawn using the style {@link
    * LineStyle#LINE}, the argument <code>rad</code> gives the line width,
    * whereas for solid primitives ({@link LineStyle#CYLINDER}, {@link
    * LineStyle#SOLID_ARROW}, {@link LineStyle#SPINDLE}), it gives
    * the nominal radius.
    * 
    * @param robj render object
    * @param style line style to use for drawing
    * @param rad radius for solid lines or width for pixel-based lines
    */
   public void drawLines (RenderObject robj, LineStyle style, double rad);
   
   /**
    * Draws all the lines in the specified line group of the supplied
    * render object, using the current material and shading.  The lines are
    * drawn either as pixel-based lines or as solid primitives, according to
    * the specified line style. For lines drawn using the style {@link
    * LineStyle#LINE}, the argument <code>rad</code> gives the line width,
    * whereas for solid primitives ({@link LineStyle#CYLINDER}, {@link
    * LineStyle#SOLID_ARROW}, {@link LineStyle#SPINDLE}), it gives
    * the nominal radius.
    * 
    * @param robj render object
    * @param gidx line group index
    * @param style line style to use for drawing
    * @param rad radius for solid lines or width for pixel-based lines
    */
   public void drawLines (RenderObject robj, int gidx, LineStyle style, double rad);
   
   /**
    * Draws all the points in the active point group of the
    * specified render object, using the current material and shading.
    * 
    * @param robj render object
    */
   public void drawPoints (RenderObject robj);
   
   /**
    * Draws all the points in the specified point group of the
    * supplied render object, using the current material and shading.
    * 
    * @param robj render object
    * @param gidx point group index
    */
   public void drawPoints (RenderObject robj, int gidx);
   
   /**
    * Draws all the points in the active point group of the specified
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
   
   /**
    * Draws all the points in the specified point group of the supplied
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
   public void drawPoints (RenderObject robj, int gidx, PointStyle style, double rad);
   
   /**
    * Draws all the vertices associated with the specified RenderObject,
    * using a specified drawing mode and the current material and shading.
    * 
    * @param robj render object
    * @param mode drawing mode to be used for drawing the vertices
    */
   public void drawVertices (RenderObject robj, DrawMode mode);
   
   /**
    * Draws the current point group, line group, and triangle group.
    * @param robj
    */
   public void draw(RenderObject robj);
     
   // MATRICES
   
   /**
    * Saves the model matrix by pushing it onto the model matrix stack.
    */
   public void pushModelMatrix();
   
   /**
    * Gets the current model matrix. The model matrix is the transformation
    * from model coordinates to world coordinates. If the matrix is a rigid
    * transformation, the returned value is a {@link RigidTransform3d};
    * otherwise, it is a more general {@link AffineTransform3d}.
    * 
    * @return model matrix value (may be modified by the user)
    */
   public AffineTransform3dBase getModelMatrix();
   
   /**
    * Gets the current model matrix. The model matrix is the transformation
    * from model coordinates to world coordinates. 
    * 
    * @param X returns the current model matrix value
    */
   public void getModelMatrix (AffineTransform3d X);
   
   /**
    * Translates the model frame. This is done post-multiplying the current
    * model matrix by a translation matrix. If the model matrix is described
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
    * Rotates the model frame. This is done post-multiplying the current
    * model matrix by a rotation matrix formed from three successive 
    * rotations: a rotation of <code>zdeg</code> degrees about the z
    * axis, followed by a rotation of <code>ydeg</code> degrees about the
    * new y axis, and finally a rotation of <code>xdeg</code> degrees about
    * the new x axis. If the model matrix is described by the affine transform
    * <pre>
    *  [  M   p  ]
    *  [         ]
    *  [  0   1  ]
    * </pre>
    * where <code>M</code> is a 3 X 3 matrix and <code>p</code> is a 3 X 1
    * translation vector, and if <code>R</code> is the rotation matrix,
    * then this method sets the model matrix to
    * <pre>
    *  [  M R   p ]
    *  [          ]
    *  [   0    1 ]
    * </pre> 
    * 
    * @param zdeg rotation about z axis (degrees)
    * @param ydeg rotation about new y axis (degrees)
    * @param xdeg rotation about new x axis (degrees)
    */
   public void rotateModelMatrix(double zdeg, double ydeg, double xdeg);
   
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
    * Sets the model matrix to the specified transform. The model matrix
    * is the transformation from model coordinates to world coordinates. 
    * 
    * @param X new model matrix value
    */
   public void setModelMatrix (AffineTransform3dBase X);
   
   /**
    * Restores the model matrix by popping it off the model matrix stack
    * 
    * @return <code>false</code> if the model matrix stack is empty
    */
   public boolean popModelMatrix();
   
   /**
    * Gets the current view matrix. The view matrix is the transformation
    * from world coordinates to eye coordinates.
    * 
    * @return view matrix value (may be modified by the user)
    */
   public RigidTransform3d getViewMatrix();
   
   /**
    * Gets the current view matrix. The view matrix is the transformation
    * from world coordinates to eye coordinates. 
    * 
    * @param TWE returns the current transform from world to eye coordinates
    */
   public void getViewMatrix (RigidTransform3d TWE);
   
//   // FINISH
//   public RigidTransform3d getEyeToWorld();
   
//   // FINISH: do we need?
//   /**
//    * Saves the view matrix by pushing it onto the view matrix stack.
//    */
//   public void pushViewMatrix();
//   
//   // FINISH: do we need?
//   /**
//    * Restores the view matrix by popping it off the view matrix stack
//    * 
//    * @return <code>false</code> if the view matrix stack is empty
//    */
//   public boolean popViewMatrix();
//   
//   // FINISH: do we need this?
//   /**
//    * Saves the projection matrix by pushing it onto the projection matrix 
//    * stack.
//    */
//   public void pushProjectionMatrix();
//   
//   // FINISH: do we need this?
//   /**
//    * Restores the projection matrix by popping it off the projection 
//    * matrix stack
//    * 
//    * @return <code>false</code> if the projection matrix stack is empty
//    */
//   public boolean popProjectionMatrix();
   
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
    * The material color to use if the renderer is currently performing a selection
    * render. This is mainly used for color-based selection.
    * 
    * @param c selection color
    */
   public void setSelectingColor(Color c);
   
   /**
    * The material color to use if the renderer is currently performing a selection
    * render. This is mainly used for color-based selection.
    * 
    * @param r red
    * @param g green
    * @param b blue
    * @param a alpha
    */
   public void setSelectingColor(float r, float g, float b, float a);
   
   /**
    * Returns the selection highlighting method used by this renderer.
    * This specifies how objects which are indicated to be <i>selected</i>
    * are drawn by the renderer in a way so that they stand out. A
    * value of {@link HighlightStyle#NONE} indicates that
    * no selection highlighting is enabled.
    * 
    * @return current selection highlighting method.
    */
   public HighlightStyle getSelectionHighlightStyle();

   /**
    * Returns the color that is used to highlight selected objects when
    * the selection highlighting method is {@link HighlightStyle#COLOR}.
    * 
    * @param rgba array of length 3 or 4 in which the RGB or RGBA components
    * of the selection color are returned.
    */
   public void getHighlightColor (float[] rgba);

   /**
    * Enables or disables selection highlighting, so that
    * subsequent primitives will be rendered in a highlighted fashion
    * to visually indicate that they are selected. 
    * When selection highlighting equals {@link HighlightStyle#COLOR}, 
    * this is done by setting the effective color to the selection color.
    *
    * @param enable if <code>true</code>, enable selection highlighting.
    * @return previous selection highlighting value
    * @see #getSelectionColor
    */
   public boolean setSelectionHighlighting (boolean enable);
   
   /**
    * Queries whether or not selection highlighting is enabled.
    * 
    * @return <code>true</code> if selection highlighting is enabled
    */
   public boolean getSelectionHighlighting();   
   
   /**
    * Begins a selection query with the {\it query identifier}
    * <code>qid</code>.  If the rendering that occurs between this call and the
    * subsequent call to {@link #endSelectionQuery} results in anything being
    * rendered within the selection window, a {\it selection hit} will be
    * generated for <code>qid</code>.
    *
    * If called within the <code>render</code> method for a {@link
    * maspack.render.GL.GLSelectable}, then <code>qid</code> must lie in the 
    * range 0 to <code>numq</code>-1, where <code>numq</code> is the value 
    * returned by 
    * {@link maspack.render.GL.GLSelectable#numSelectionQueriesNeeded
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
   
   // FINISH: do we need?
   //public void rerender();

//   // FINISH: do we need?
//   /**
//    * Re-draw contents of renderer
//    */
//   public void repaint();

   /**
    * Begins draw mode. This is analogous to <code>glBegin()</code> in the
    * old GL specification. Once in draw mode, the application can specify
    * the vertices (and if necessary, normals) that are used to draw the
    * primitive that is specified by the mode parameter. 
    * 
    * @param mode specifies the primitive to be built while in draw mode.
    * @throws IllegalStateException if the renderer is currently in draw mode
    */
   public void beginDraw (DrawMode mode);

   /**
    * Adds a vertex to a primitive being drawn while in draw mode. 
    * 
    * @param px vertex x coordinate
    * @param py vertex y coordinate
    * @param pz vertex z coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void addVertex (float px, float py, float pz);

   /**
    * Adds a vertex to a primitive being drawn while in draw mode. 
    * 
    * @param px vertex x coordinate
    * @param py vertex y coordinate
    * @param pz vertex z coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void addVertex (double px, double py, double pz);

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
    * @param nx normal x coordinate
    * @param ny normal y coordinate
    * @param nz normal z coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setNormal (float nx, float ny, float nz);

   /**
    * Sets the normal to be associated with the next vertex to be
    * added while in draw mode. This method is functionally equivalent to
    * {@link #setNormal(float,float,float)}. 
    * 
    * @param nx normal x coordinate
    * @param ny normal y coordinate
    * @param nz normal z coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setNormal (double nx, double ny, double nz);
   
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
    * Sets the texture coordinate to be associated with the next vertex to be
    * added while in draw mode. This texture coordinate will remain in effect until
    * a subsequent <code>setTexcoord</code> call. The coordinate should be
    * within the range [0, 1]
    * 
    * @param tx texture x coordinate
    * @param ty texture y coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setTexcoord (float tx, float ty);

   /**
    * Sets the texture coordinate to be associated with the next vertex to be
    * added while in draw mode. This method is functionally equivalent to
    * {@link #setTexcoord(float,float,float)}. 
    * 
    * @param tx texture x coordinate
    * @param ty texture y coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setTexcoord (double tx, double ty);
   
   /**
    * Sets the texture coordinate to be associated with the next vertex to be
    * added while in draw mode. This method is functionally equivalent to
    * to {@link #setTexcoord(float,float,float)}. 
    * 
    * @param tex texture coordinates
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setTexcoord (Vector2d tex);

   /**
    * Ends draw mode. This is analogous to <code>glEnd()</code> in the old
    * GL specification.
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void endDraw();
}
