/**
 * Copyright c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;

import maspack.matrix.AffineTransform2d;
import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform2d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;

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
    * Flag requesting that an object be rendered with highlighting.
    */   
   public static int HIGHLIGHT = 0x1;

//   /** 
//    * Flag requesting that vertex coloring should be used for mesh rendering.
//    */
//   public static int VERTEX_COLORING = 0x2;
//
//   /** 
//    * Flag requesting color interpolation in HSV space, if possible.
//    */
//   public static int HSV_COLOR_INTERPOLATION = 0x4;

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
    * Specifies how highlighting is performed by this renderer.
    */
   public enum HighlightStyle {
      /**
       * Highlighting is not supported or is deactivated
       */
      NONE, 
      
      /**
       * Highlighting is performed by rendering 
       * objects using a special <i>highlight color</i>, as returned
       * by {@link #getHighlightColor(float[])}. 
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
       * Smoothing shading, where normal information is interpolated
       * across the primitive.
       */
      SMOOTH,
      
      /**
       * Variation on smooth shading, intended for metallic surfaces. May
       * be equivalent to SMOOTH in some renderer implementations.
       */
      METAL,
      
//      /**
//       * Smoothing shading, where normal information is interpolated
//       * across the primitive.
//       */
//      GOURAUD,
//
//      /**
//       * Smooth shading based on the Phong shading model
//       */
//      PHONG,

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
    * Returns the current center point
    *
    * @return center point, in world coordinates
    */
   public Point3d getCenter();
   
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
    * {@link #hasVertexColorMixing} to test whether a method is suuported.
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
   
   /**
    * Draws a single line between two points in model coordinates,
    * using the current line width, material, and shading.  This method
    * is functionally equivalent to {@link #drawLine(Vector3d,Vector3d)}.
    * 
    * @param pnt0 array (of length 3) giving the first point
    * @param pnt1 array (of length 3) giving the second point
    */
   public void drawLine (float[] pnt0, float[] pnt1);

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
    * Draws an axis-aligned cube with a specified width centered at a point
    * in model coordinates, using the current shading and material.
    * 
    * @param pnt center of the cube
    * @param w width of the cube
    */
   public void drawCube (Vector3d pnt, double w);

   /**
    * Draws an axis-aligned cube with a specified width centered at a point
    * in model coordinates, using the current shading and material.
    * 
    * @param pnt center of the cube
    * @param w width of the cube
    */
   public void drawCube (float[] pnt, double w);   

   /**
    * Draws an axis-aligned box in model coordinates, using the current 
    * shading and material.
    * 
    * @param pnt center of the box
    * @param wx width in x
    * @param wy width in y
    * @param wz width in z
    */
   public void drawBox (float[] pnt, double wx, double wy, double wz);
   
   /**
    * Draws an axis-aligned box in model coordinates, using the current 
    * shading and material.
    * 
    * @param pnt center of the box
    * @param widths x,y,z widths of the box
    */
   public void drawBox (Vector3d pnt, Vector3d widths);
   
   /**
    * Draws a box centered on the specified transform in model coordinates,
    * using the current shading and material.
    * 
    * @param TBM transform from box to model coordinates on which
    * the box is centered
    * @param widths x,y,z widths of the box
    */
   public void drawBox (RigidTransform3d TBM, Vector3d widths);
   
   /**
    * Draws a spindle between two points in model coordinates, 
    * using the current shading and material. The resolution
    * is specified by {@link #getSurfaceResolution}.
    * 
    * @param pnt0 first end point
    * @param pnt1 second end point
    * @param rad radius of the spindle
    */
   public void drawSpindle (Vector3d pnt0, Vector3d pnt1, double rad);

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
      Vector3d pnt0, Vector3d pnt1, double rad0, double rad1, boolean capped);
   
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
   public void drawArrow (
      Vector3d pnt0, Vector3d pnt1, double rad, boolean capped);

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
   public void drawArrow (
      float[] pnt0, float[] pnt1, double rad, boolean capped);

   /**
    * Draws a set of coordinate axes representing a rigid coordinate frame
    * <code>X</code>. The origin point for the axes is <code>X.p</code>, and
    * the directions for each axis are given by the three columns of
    * <code>X.R</code>. Each axis is drawn as a pixel-based line with a length
    * <code>len</code> and a width <code>width</code>. By default, the x, y,
    * and z axes are drawn using the colors red, green, and blue, unless
    * <code>highlight</code> is <code>true</code> and the highlight style is
    * {@link HighlightStyle#COLOR}, in which case all axes are drawn using the
    * renderer's highlight color.
    *  
    * @param X coordinate frame defining the axes
    * @param len axis length
    * @param width width of each axis (in pixels)
    * @param highlight if <code>true</code>, indicates that the axes should be
    * highlighted.
    */
   public void drawAxes (
      RigidTransform3d X, double len, int width, boolean highlight);

   /**
    * Draws a set of coordinate axes representing a rigid coordinate frame 
    * <code>X</code>. This method is functionally equivalent to {@link
    * #drawAxes(RigidTransform3d,double,int,boolean)}, except that the
    * lengths for each axis are individually specified. Axes with a
    * a specified length of 0 will not be drawn.
    *  
    * @param X coordinate frame defining the axes
    * @param lens lengths for each axis
    * @param width width of each axis (in pixels)
    * @param highlight if <code>true</code>, indicates that the axes should be
    * highlighted.
    */
   public void drawAxes (
      RigidTransform3d X, double[] lens, int width, boolean highlight);

   /**
    * Draws a solid set of coordinate axes representing a rigid coordinate
    * frame <code>X</code>. The origin point for the axes is <code>X.p</code>,
    * and the directions for each axis are given by the three columns of
    * <code>X.R</code>. Each axis is drawn an arrow-tipped cylinder with a
    * length <code>len</code> and cylinder radius <code>rad</code>. By default,
    * the x, y, and z axes are drawn using the colors red, green, and blue,
    * unless <code>highlight</code> is <code>true</code> and the highlight
    * style is {@link HighlightStyle#COLOR}, in which case all axes are drawn
    * using the renderer's highlight color.
    *  
    * @param X coordinate frame defining the axes
    * @param len axis length
    * @param rad axis cylinder radius
    * @param highlight if <code>true</code>, indicates that the axes should be
    * highlighted.
    */
   public void drawSolidAxes (
      RigidTransform3d X, double len, double rad, boolean highlight);

   /**
    * Draws a solid set of coordinate axes representing a rigid coordinate
    * frame <code>X</code>. This method is functionally equivalent to {@link
    * #drawSolidAxes(RigidTransform3d,double,double,boolean)}, except that the
    * lengths for each axis are individually specified. Axes with a
    * a specified length of 0 will not be drawn.
    *  
    * @param X coordinate frame defining the axes
    * @param lens lengths for each axis
    * @param rad axis cylinder radius
    * @param highlight if <code>true</code>, indicates that the axes should be
    * highlighted.
    */
   public void drawSolidAxes (
      RigidTransform3d X, double[] lens, double rad, boolean highlight);
   
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
    * is {@link PointStyle#CUBE} or {@link PointStyle#SPHERE}, then the 
    * point is drawn as a solid with a radius given by
    * <code>props.getPointRadius()</code>. If <code>highlight</code>
    * is <code>true</code> and the highlight style is
    * {@link HighlightStyle#COLOR}, then the point will
    * be drawn using the highlight color rather than the point color
    * specified in <code>props</code>.
    * 
    * @param props render properties used for drawing the point
    * @param pnt location of the point
    * @param highlight if <code>true</code>, indicates that highlighting, if
    * enabled, should be applied to the point
    */
   public void drawPoint (RenderProps props, float[] pnt, boolean highlight);

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
    * <code>props.getLineRadius()</code>. If <code>highlight</code>
    * is <code>true</code> and the highlight style is
    * {@link HighlightStyle#COLOR}, then the line will
    * be drawn using the highlight color rather than the line color
    * specified in <code>props</code>.
    * 
    * @param props render properties used for drawing the line
    * @param pnt0 first point
    * @param pnt1 second point
    * @param highlight if <code>true</code>, indicates that highlighting, if
    * enabled, should be applied to the line
    */
   public void drawLine (
      RenderProps props, float[] pnt0, float[] pnt1, boolean highlight);

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
    * @param highlight if <code>true</code>, indicates that highlighting, if
    * enabled, should be applied to the line
    */
   public void drawLine (
      RenderProps props, float[] pnt0, float[] pnt1, float[] color,
      boolean capped, boolean highlight);

   /**
    * Draws a single line between two points in model coordinates, using the
    * line style, radius, line color, and shading specified by the render
    * properties argument <code>props</code>. This method is functionally
    * equivalent to {@link #drawLine(RenderProps,float[],float[],boolean)},
    * except for the additional control provided by the argument
    * <code>capped</code>.
    * 
    * @param props render properties used for drawing the line
    * @param pnt0 first point
    * @param pnt1 second point
    * @param capped if <code>true</code>, specifies that the line
    * should have a solid cap on its ends for 
    * styles such as {@link LineStyle#CYLINDER} and @link LineStyle#ARROW}
    * @param highlight if <code>true</code>, indicates that highlighting, if
    * enabled, should be applied to the line
    */
   public void drawLine (
      RenderProps props, float[] pnt0, float[] pnt1,
      boolean capped, boolean highlight);

   /**
    * Draws an arrow between two points in model coordinates,
    * using the radius, line color, and shading specified by the
    * render properties argument <code>props</code>. The method
    * behaves similarly to 
    * {@link #drawArrow(float[],float[],double,boolean)},
    * with the cylinder radius determine instead by
    * <code>props.getLineRadius()</code>. 
    * 
    * @param props render properties used for drawing the arrow
    * @param pnt0 first point
    * @param pnt1 second point
    * @param capped if <code>true</code>, specifies that the arrow
    * should have a solid cap on the bottom
    * @param highlight if <code>true</code>, indicates that highlighting, if
    * enabled, should be applied to the arrow
    */
   public void drawArrow (
      RenderProps props, float[] pnt0, float[] pnt1, boolean capped,
      boolean highlight);

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
    * <code>props.getLineRadius()</code>. If <code>highlight</code>
    * is <code>true</code> and the highlight style is
    * {@link HighlightStyle#COLOR}, then the strip will
    * be drawn using the highlight color rather than the line color
    * specified in <code>props</code>.
    * 
    * @param props render properties used for drawing the strip
    * @param pnts list of points used for drawing the strip
    * @param style line style to be used for the strip
    * @param highlight if <code>true</code>, indicates that highlighting, if
    * enabled, should be applied to the strip
    */
   public void drawLineStrip (
      RenderProps props, Iterable<float[]> pnts, 
      LineStyle style, boolean highlight);   
   
   /**
    * Computes and returns the logical bounding box of the supplied
    * text.  The width is the total advance, from baseline
    * of the first character, to the advance after the last
    * character.  The height is the line height (ascent +
    * descent + leading).  The origin is placed at the baseline,
    * with the position specifying the bottom-left corner.
    * Note that in some fonts, some characters may extend
    * outside of these bounds.  
    * This method is mainly used for manual alignment of text.
    * @param font font to use for sizing
    * @param str String of which to compute the bounding box 
    * @param emSize size of the 'em' unit
    * @return rectangular bounds
    */
   public Rectangle2D getTextBounds(Font font, String str, double emSize);
   
   /**
    * Sets default font for text rendering
    * @param font default font. The font size largely impacts
    * the resolution of the renderer characters.  A value of at least
    * 32pt is recommended.
    */
   public void setDefaultFont(Font font);
   
   /**
    * Retrieves the default font.
    * @return default font.
    */
   public Font getDefaultFont();

   /**
    * Queries whether or not this renderer supports text rendering.
    * 
    * @return <code>true</code> if text rendering is supported.
    */
   public boolean hasTextRendering();
   
   /**
    * Draws text in the x-y plane in model coordinates. Uses a default font.
    *
    * @param str string to render
    * @param pos position of the lower left of the text box (model coordinates)
    * @param emSize size of an `em' unit in model coordinates
    * @return the advance distance in the x-direction (width of text)
    */
   public double drawText(String str, float[] pos, double emSize);
   
   /**
    * Draws text in the x-y plane in model coordinates.
    * 
    * @param font the font to use.  The font size largely impacts
    * the resolution of the renderer characters.  A value of at least
    * 32pt is recommended.
    * @param str string to render
    * @param pos position of the lower left of the text box (model coordinates)
    * @param emSize size of an `em' unit in model coordinates
    * @return the advance distance in the x-direction (width of text)
    */
   public double drawText(Font font, String str, float[] pos, double emSize);
   
   /**
    * Draws text in the x-y plane in model coordinates. Uses a default font.
    * 
    * @param str string to render
    * @param pos position of the lower left of the text box (model coordinates)
    * @param emSize size of an `em' unit in model coordinates
    * @return the advance distance in the x-direction (width of text)
    */
   public double drawText(String str, Vector3d pos, double emSize);
   
   /**
    * Draws text in the x-y plane in model coordinates.
    * 
    * @param font the font to use.  The font size largely impacts
    * the resolution of the renderer characters.  A value of at least
    * 32pt is recommended.
    * @param str string to render
    * @param pos position of the lower left of the text box (model coordinates)
    * @param emSize size of an `em' unit in model coordinates
    * @return the advance distance in the x-direction (width of text)
    */
   public double drawText(Font font, String str, Vector3d pos, double emSize);
   
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
    * @see #is2DRendering
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
    * <code>rgba</code>, and enables or disables highlighting
    * according to the value of <code>highlight</code>.
    * If highlighting is requested and the highlight style is
    * {@link HighlightStyle#COLOR}, the highlight color will override
    * the diffuse/ambient color settings. This method also clears any 
    * back color that may be present. It is therefore equivalent to calling
    * <pre>
    *    setFrontColor (rgba);
    *    setBackColor (null);
    *    setHighlighting (highlight);
    * </pre>
    * 
    * @param rgba array of length 3 or 4 specifying RGB or RGBA values in
    * the range [0,1]. Alpha is only applied to the diffuse color
    * and is assumed to be 1.0 if not specified.
    * @param highlight if <code>true</code>, enables highlighting
    * <code>rgba</code>
    */
   public void setColor (float[] rgba, boolean highlight);
   
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
    * <code>props</code>, and enables or disables highlighting
    * according to the value of <code>highlight</code>.  The back color is set
    * to <code>null</code>, and the emission, shininess, and specular values
    * are set either from <code>props</code> or from default values.
    * 
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring} with
    * <code>props.getPointColor()</code> supplying the <code>rgba</code> value.
    * If highlighting is requested and the highlight method equals
    * {@link HighlightStyle#COLOR}, the highlight color will override the
    * diffuse/ambient color settings.
    *
    * @param props supplies the shininess and point color values
    * @param highlight if <code>true</code>, enables highlighting
    */
   public void setPointColoring (RenderProps props, boolean highlight);
   
   /**
    * Sets the diffuse and ambient colors to the line color in
    * <code>props</code>, and enables or disables highlighting
    * according to the value of <code>highlight</code>. The back color is set
    * to <code>null</code>, and the emission, shininess, and specular values
    * are set either from <code>props</code> or from default values.
    *
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring} with
    * <code>props.getLineColor()</code> supplying the <code>rgba</code> value.
    * If highlighting is requested and the highlight method equals
    * {@link HighlightStyle#COLOR}, the highlight color will override the
    * diffuse/ambient color settings.
    *
    * @param props supplies the shininess and line color values
    * @param highlight if <code>true</code>, enables highlighting
    */
   public void setLineColoring (RenderProps props, boolean highlight);
   
   /**
    * Sets the diffuse and ambient colors to the edge color in
    * <code>props</code>, and enables or disables highlighting
    * according to the value of <code>highlight</code>. If the edge color is
    * <code>null</code> then the line color in <code>props</code> is used
    * instead. The back color is set to <code>null</code>, and the emission,
    * shininess, and specular values are set either from <code>props</code> or
    * from default values.
    *
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring} with
    * <code>props.getEdgeColor()</code> or <code>props.getLineColor()</code>
    * supplying the <code>rgba</code> value.  If highlighting is
    * requested and the highlight method equals {@link HighlightStyle#COLOR},
    * the highlight color will override the diffuse/ambient color settings.
    *
    * @param props supplies the shininess and edge (or line) color values
    * @param highlight if <code>true</code>, enables highlighting
    */
   public void setEdgeColoring (RenderProps props, boolean highlight);

   /**
    * Sets the diffuse and ambient colors to the face color in
    * <code>props</code>, and enables or disables highlighting
    * according to the value of <code>highlight</code>. The back color will also
    * be set to the (possibly <code>null</code>) value of the back color in
    * <code>props</code>. If the back color is not <code>null</code>, then this
    * will be used to provide the coloring for back faces when they are
    * visible. The emission, shininess, and specular values are set either from
    * <code>props</code> or from default values.
    *
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring} with
    * <code>props.getFaceColor()</code> supplying the <code>rgba</code> value,
    * except that the back color is set from <code>props.getBackColor()</code>
    * instead of being set to <code>null</code>. If highlighting is
    * requested and the highlight method equals {@link HighlightStyle#COLOR},
    * the highlight color will override the diffuse/ambient color settings.
    *
    * @param props supplies the shininess and front and back color values
    * @param highlight if <code>true</code>, enables highlighting
    */
   public void setFaceColoring (RenderProps props, boolean highlight);
   
   /**
    * Sets the diffuse and ambient colors to <code>rgba</code>, and enables or
    * disables highlighting according to the value of
    * <code>highlight</code>. The back color will also be set to the (possibly
    * <code>null</code>) value of the back color in <code>props</code>. If the
    * back color is not <code>null</code>, then this will be used to provide
    * the coloring for back faces when they are visible. The emission,
    * shininess, and specular values are set either from <code>props</code> or
    * from default values.
    *
    * <p>The resulting behavior is equivalent to {@link #setPropsColoring},
    * except that the back color is set from <code>props.getBackColor()</code>
    * instead of being set to <code>null</code>. If highlighting is
    * requested and the highlight method equals {@link HighlightStyle#COLOR},
    * the highlight color will override the diffuse/ambient color settings.
    *
    * @param props supplies the shininess and back color values
    * @param rgba an array of length 3 or 4 specifying RGB or RGBA values
    * for the color the range [0,1]. Alpha is only applied to the diffuse
    * color and is set to <code>props.getAlpha()</code> if not specified.
    * @param highlight if <code>true</code>, enables highlighting
    */
   public void setFaceColoring (
      RenderProps props, float[] rgba, boolean highlight);
      
   /**
    * Sets the diffuse and ambient colors to <code>rgba</code>, and enables or
    * disables highlighting according to the value of
    * <code>highlight</code>. If <code>rgba</code> only has a length of 3 then
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
    *   setHighlighting (highlight)
    * </pre>
    * If highlighting is requested and the
    * highlight method equals {@link HighlightStyle#COLOR}, the highlight
    * color will override the diffuse/ambient color settings.
    *
    * @param props supplies the shininess value
    * @param rgba an array of length 3 or 4 specifying RGB or RGBA values for
    * the color the range [0,1]. Alpha is only applied to the diffuse color and
    * is set to <code>props.getAlpha()</code> if not specified.
    * @param highlight if <code>true</code>, enables highlighting
    */
   public void setPropsColoring (
      RenderProps props, float[] rgba, boolean highlight);

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
    * Queries whether or not this renderer supports color mapping.
    * 
    * @return <code>true</code> if color mapping is supported.
    */
   public boolean hasColorMapping();
   
   /**
    * Queries whether or not a specified method for combining color
    * map and material coloring is supported by this Renderer.
    *
    * @return <code>true</code> if the color mixing method is supported
    */   
   public boolean hasColorMapMixing (ColorMixing cmix);
   
   /**
    * If color mapping is supported, sets up a color map according
    * to the properties specified by <code>props</code>, or removes
    * any color map if <code>props</code> is <code>null</code>.
    * Once a color map is set, it will be applied to any subsequent
    * draw operation for which the vertices contain texture coordinates.
    * 
    * @param props properties for the color map, or <code>null</code>
    * to disable
    * @return the previous color map properties
    */
   public ColorMapProps setColorMap(ColorMapProps props);

   /**
    * Returns the properties for the most recently set color map, or
    * <code>null</code> if no color map is currently set or color mapping
    * is not supported.
    *
    * @return current color map properties.
    * @see #setColorMap
    */
   public ColorMapProps getColorMap();
   
   /**
    * Queries whether or not this renderer supports normal mapping.
    * 
    * @return <code>true</code> if normal mapping is supported.
    */
   public boolean hasNormalMapping();
   
   /**
    * If normal mapping is supported, sets up a normal map according
    * to the properties specified by <code>props</code>, or removes
    * normal mapping if <code>props</code> is <code>null</code>.
    * Once normal mapping is set, it will be applied to any subsequent
    * draw operation for which the vertices contain texture coordinates.
    * At present, texture coordinates can only be specified for draw
    * operations involving a {@link RenderObject}.
    *  
    * <p>For normal mapping to work, shading must not be set to {@link
    * Shading#NONE} or {@link Shading#FLAT}.
    *
   * @param props properties for the normal mapping, or <code>null</code>
    * to disable
    * @return the previous normal map properties
    */
   public NormalMapProps setNormalMap(NormalMapProps props);
   
   /**
    * Returns the most recently set normal mapping properties, or
    * <code>null</code> if no normal mapping has been set or normal mapping
    * is not supported.
    *
    * @return current normal mapping properties.
    * @see #setNormalMap
    */
   public NormalMapProps getNormalMap();
   
   /**
    * Queries whether or not this renderer supports bump mapping.
    * 
    * @return <code>true</code> if bump mapping is supported.
    */
   public boolean hasBumpMapping();
   
   /**
    * If bump mapping is supported, sets up bump mapping according
    * to the properties specified by <code>props</code>, or removes
    * bump mapping if <code>props</code> is <code>null</code>.
    * Once bump mapping is set, it will be applied to any subsequent
    * draw operation for which the vertices contain texture coordinates.
    * At present, texture coordinates can only be specified for draw
    * operations involving a {@link RenderObject}.
    * 
    * <p>For bump mapping to work, shading must not be set to {@link
    * Shading#NONE} or {@link Shading#FLAT}.
    *
    * @param props properties for the bump mapping, or <code>null</code>
    * to disable
    * @return the previous bump map properties
    */
   public BumpMapProps setBumpMap(BumpMapProps props);
   
   /**
    * Returns the most recently set bump mapping properties, or
    * <code>null</code> if no bump mapping has been set or bump mapping
    * is not supported.
    *
    * @return current bump mapping properties.
    * @see #setBumpMap
    */
   public BumpMapProps getBumpMap();
   

   //==========================================================================
   // RENDER OBJECTS
   //==========================================================================
   
   /**
    * Draws all the triangles in the first triangle group of the
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
    * Draws a selection of triangles in the specified triangle group of the
    * render object, using the current material and shading.
    *
    * @param robj render object
    * @param gidx triangle group index
    * @param offset triangle offset at which to start rendering
    * @param count number of triangles to draw
    */
   public void drawTriangles (RenderObject robj, int gidx, int offset, int count);
   
   /**
    * Draws all the lines in the first line group of the
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
    * Draws all the lines in the first line group of the specified
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
    * Draws a selection of lines in the specified line group of the supplied
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
    * @param offset line offset at which to begin drawing
    * @param count number of lines to draw
    * @param style line style to use for drawing
    * @param rad radius for solid lines or width for pixel-based lines
    */
   public void drawLines (RenderObject robj, int gidx, int offset, int count,
      LineStyle style, double rad);
   
   /**
    * Draws all the points in the first point group of the
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
    * Draws all the points in the first point group of the specified
    * render object, using the current material and shading. The points are
    * drawn either as pixel-based points or as solids, according to the
    * specified points style. For points drawn using the style {@link
    * PointStyle#POINT}, the argument <code>rad</code> gives the point size,
    * whereas for {@link PointStyle#CUBE} it gives the cube half-width,
    * and for {@link PointStyle#SPHERE} it gives the sphere radius.
    * 
    * @param robj render object
    * @param style point style to use for drawing
    * @param rad radius for spheres or width for pixel-based points
    */
   public void drawPoints (RenderObject robj, PointStyle style, double rad);
   
   /**
    * Draws all the points in the specified point group of the supplied
    * render object, using the current material and shading. The points are
    * drawn either as pixel-based points or as solids, according to the
    * specified points style. For points drawn using the style {@link
    * PointStyle#POINT}, the argument <code>rad</code> gives the point size,
    * whereas for {@link PointStyle#CUBE} it gives the cube half-width, and for
    * {@link PointStyle#SPHERE} it gives the sphere radius.
    * 
    * @param robj render object
    * @param gidx point group index
    * @param style point style to use for drawing
    * @param rad radius for spheres or width for pixel-based points
    */
   public void drawPoints (RenderObject robj, int gidx, PointStyle style, double rad);
   
   /**
    * Draws a selection of points in the specified point group of the supplied
    * render object, using the current material and shading. The points are
    * drawn either as pixel-based points or as solids, according to the
    * specified points style. For points drawn using the style {@link
    * PointStyle#POINT}, the argument <code>rad</code> gives the point size,
    * whereas for {@link PointStyle#CUBE} it gives the cube half-width, and for
    * {@link PointStyle#SPHERE} it gives the sphere radius.
    * 
    * @param robj render object
    * @param gidx point group index
    * @param offset point offset at which to begin within the point group
    * @param count number of points to draw
    * @param style point style to use for drawing
    * @param rad radius for spheres or width for pixel-based points
    */
   public void drawPoints (RenderObject robj, int gidx, 
      int offset, int count, PointStyle style, double rad);
   
   /**
    * Draws all the vertices associated with the specified RenderObject,
    * using a specified drawing mode and the current material and shading.
    * 
    * @param robj render object
    * @param mode drawing mode to be used for drawing the vertices
    */
   public void drawVertices (RenderObject robj, DrawMode mode);
   
   /**
    * Draws a selection of vertices associated with the specified RenderObject,
    * using a specified drawing mode and the current material and shading. This
    * version of the method should be favoured for persistent index lists, since
    * the {@link VertexIndexArray} can detect whether or not it has been modified,
    * which allows for caching.  
    * 
    * @param robj render object
    * @param idxs vertex indices
    * @param mode drawing mode to be used for drawing the vertices
    */
   public void drawVertices (RenderObject robj, VertexIndexArray idxs, DrawMode mode);
   
   /**
    * Draws a selection of vertices associated with the specified RenderObject,
    * using a specified drawing mode and the current material and shading. This
    * version of the method should be favoured for persistent index lists, since
    * the {@link VertexIndexArray} can detect whether or not it has been modified,
    * which allows for caching.  Additionally, this method allows specification of
    * an offset and vertex count.
    * 
    * @param robj render object
    * @param idxs vertex indices
    * @param offset index offset
    * @param count index count
    * @param mode drawing mode to be used for drawing the vertices
    */
   public void drawVertices (RenderObject robj, VertexIndexArray idxs, 
      int offset, int count, DrawMode mode);
   
   /**
    * Draws all the primitives in the first point, line and triangles groups.
    * @param robj render object
    */
   public void draw (RenderObject robj);
   
   /**
    * Draws the specified group of triangles, repeated for each instance
    * in rinst.
    * @param robj object to draw
    * @param gidx triangle group to draw
    * @param rinst instance to draw
    */
   public void drawTriangles(RenderObject robj, int gidx, RenderInstances rinst);

   /**
    * Draws the specified group of lines, repeated for each instance
    * in rinst.  Lines are drawn as {@link LineStyle#LINE} and use the
    * current line-width, as determined by {@link #getLineWidth()}.
    * @param robj object to draw
    * @param gidx line group to draw
    * @param rinst instance to draw
    */
   public void drawLines(RenderObject robj, int gidx, RenderInstances rinst);
   
   /**
    * Draws the specified group of points, repeated for each instance
    * in rinst.  Points are drawn as {@link PointStyle#POINT} and use the
    * current point-size, as determined by {@link #getPointSize()}.
    * 
    * @param robj object to draw
    * @param gidx point group to draw
    * @param rinst instance to draw
    */
   public void drawPoints(RenderObject robj, int gidx, RenderInstances rinst);
     
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
    * Sets the model matrix to perform scaling and translation in the
    * x-y plane so that the model coordinates (left,bottom) and
    * (right,top) map onto (-1,1) and (1,1) in world coordinates. 
    * When in 2d rendering mode, this means that 
    * (left,bottom) and (right,top) will correspond to the
    * lower left and top right of the screen.
    *   
    * @param left x value corresponding to -1 in world coordinates 
    * @param right x value corresponding to 1 in world coordinates 
    * @param bottom y value corresponding to -1 in world coordinates 
    * @param top y value corresponding to 1 in world coordinates 
    * @see #is2DRendering
    */
   public void setModelMatrix2d (
      double left, double right, double bottom, double top);
   
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
   
//   /**
//    * Adds a depth offset to the model matrix to help prevent "fighting" in the
//    * depth buffer. This is done by translating the model frame by an offset
//    * along the eye frame's z axis, with positive values moving the model frame
//    * closer to the eye. Each unit of offset equals approximately the distance
//    * needed to move by one unit of depth buffer precision, evaluated at a
//    * distance from the eye equal to the distance to the far clipping plane.
//    * 
//    * @param zOffset z offset to add to the model frame
//    */
//   public void addDepthOffset (double zOffset);

   /**
    * Set a depth offset for the projection matrix. Each unit represents 
    * enough depth to account for one bin in the depth buffer. Positive
    * values bring following objects closer to the screen, while negative
    * values send them father away. The default value is 0.
    * 
    * <p>Depth offsets are used to help resolve z-fighting, in which 
    * overlapping primitives drawn in the same plane compete for visibility.
    * If the plane has a considerable tilt with respect to the viewer,
    * then an offset larger than one may be needed to resolve the issue.
    * 
    * @param offset new depth offset value
    */
   public void setDepthOffset(int offset);

   /**
    * The current depth offset level. See {@link #setDepthOffset} for
    * a description. The default value is 0.
    * 
    * @return the current depth offset (in depth bins)
    */
   public int getDepthOffset();
   
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
   
//   /**
//    * Flag requesting that all display lists be cleared
//    */
//   public static int CLEAR_RENDER_CACHE = 0x100;
//
//   /**
//    * Flag requesting components refresh any custom rendering info
//    */
//   public static int UPDATE_RENDER_CACHE = 0x200;
   
   /**
    * Sets the texture coordinate transformation matrix to the specified 
    * transform. The texture matrix is applied to texture coordinates,
    * useful for tiling, or when compacting multiple texture sources into
    * a single large texture. 
    * 
    * @param T new texture transform matrix
    */
   public void setTextureMatrix (AffineTransform2dBase T);   
   
   /**
    * Gets the current texture matrix. The texture matrix is the transformation
    * applied to any texture coordinates. If the matrix is a rigid
    * transformation, the returned value is a {@link RigidTransform2d};
    * otherwise, it is a more general {@link AffineTransform2d}.
    * 
    * @return texture matrix value (may be modified by the user)
    */
   public AffineTransform2dBase getTextureMatrix();
   
   /**
    * Gets the current texture matrix. The texture matrix is the transformation
    * applied to texture coordinates.
    * 
    * @param X returns the current texture matrix value
    * @see #setTextureMatrix(AffineTransform2dBase)
    */
   public void getTextureMatrix (AffineTransform2d X);
   
   /**
    * Queries whether or not this Renderer supports selection.
    * 
    * @return <code>true</code> if selection is supported.
    */
   public boolean hasSelection();
   
   /**
    * Returns true if the renderer is currently performing a selection
    * render. When this is the case, the application should avoid calls
    * affecting color and lighting.
    *
    * @return true if the renderer is in selection mode.
    */
   public boolean isSelecting();
   
   /**
    * Returns the highlight style used by this renderer. This
    * specifies how rendered objects are highlighted when highlighting
    * is enabled. A value of {@link HighlightStyle#NONE} indicates that
    * highlighting is deactivated.
    * 
    * @return current highlighting method.
    */
   public HighlightStyle getHighlightStyle();

   /**
    * Returns the color that is used to highlight objects when
    * the highlighting method is {@link HighlightStyle#COLOR}.
    * 
    * @param rgba array of length 3 or 4 in which the RGB or RGBA components
    * of the highlight color are returned.
    */
   public void getHighlightColor (float[] rgba);

   /**
    * Enables or disables highlighting. If highlighting is enabled,
    * subsequent primitives will be rendered in a highlighted fashion
    * using the style indicated by {@link HighlightStyle}. If the
    * style is {@link HighlightStyle#COLOR}, highlighting is
    * done by setting the effective color to the highlight color.
    *
    * @param enable if <code>true</code>, enable highlighting.
    * @return previous highlighting value
    * @see #getHighlightColor
    */
   public boolean setHighlighting (boolean enable);
   
   /**
    * Queries whether or not highlighting is enabled.
    * 
    * @return <code>true</code> if highlighting is enabled
    */
   public boolean getHighlighting();   
   
   /**
    * Begins a selection query with the {\it query identifier}
    * <code>qid</code>.  If the rendering that occurs between this call and the
    * subsequent call to {@link #endSelectionQuery} results in anything being
    * rendered within the selection window, a {\it selection hit} will be
    * generated for <code>qid</code>.
    *
    * If called within the <code>render</code> method for a {@link
    * maspack.render.IsSelectable}, then <code>qid</code> must lie in the 
    * range 0 to <code>numq</code>-1, where <code>numq</code> is the value 
    * returned by 
    * {@link maspack.render.IsSelectable#numSelectionQueriesNeeded
    * IsSelectable.numSelectionQueriesNeeded()}.  Selection queries cannot be
    * nested.  If a query identifier is used more than once, it is assumed
    * as part of the same query. 
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
    * Begins selection for a {@link maspack.render.IsSelectable}
    * <code>s</code>that
    * manages its own selection; this call should
    * be used in place of {@link #beginSelectionQuery} for such objects.
    * Selectables that manage their own selection are identified by
    * having a value {@code numq >= 0}, where <code>numq</code>
    * is the value returned by
    * {@link maspack.render.IsSelectable#numSelectionQueriesNeeded}
    * IsSelectable#numSelectionQueriesNeeded{}}.
    * The argument <code>qid</code> is the current selection query identifier,
    * which after the call should be increased by <code>numq</code>.
    *
    * @param s Selectable that is managing its own sub-selection
    * @param qid current selection query identifier
    */
   public void beginSubSelection (IsSelectable s, int qid);

   /**
    * Ends object sub-selection that was initiated with a call
    * to {@link #beginSubSelection}.
    *
    * @see #endSubSelection
    */
   public void endSubSelection ();

   /**
    * Returns true if <code>s</code> is selectable in the current selection
    * context. This will be the case if <code>s.isSelectable()</code> returns
    * <code>true</code>, and <code>s</code> also passes whatever selection
    * filter might currently be set in the renderer.
    */
   public boolean isSelectable (IsSelectable s);
   
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
    * Adds a vertex to a primitive being drawn while in draw mode. 
    * 
    * @param pnt coordinates for the vertex
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void addVertex (float[] pnt);

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
    * added while in draw mode. This texture coordinate will remain in effect
    * until a subsequent <code>setTexcoord</code> call. The coordinate should
    * be within the range [0, 1]
    * 
    * @param tx texture x coordinate
    * @param ty texture y coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setTextureCoord (float tx, float ty);

   /**
    * Sets the texture coordinate to be associated with the next vertex to be
    * added while in draw mode. This method is functionally equivalent to
    * {@link #setTextureCoord(float,float)}.
    * 
    * @param tx texture x coordinate
    * @param ty texture y coordinate
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setTextureCoord (double tx, double ty);
   
   /**
    * Sets the texture coordinate to be associated with the next vertex to be
    * added while in draw mode. This method is functionally equivalent to to
    * {@link #setTextureCoord(float,float)}.
    * 
    * @param tex texture coordinates
    * @see #beginDraw
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void setTextureCoord (Vector2d tex);

   /**
    * Ends draw mode. This is analogous to <code>glEnd()</code> in the old
    * GL specification.
    * @throws IllegalStateException if the renderer is not in draw mode
    */
   public void endDraw();

   /**
    * Restores the renderer to its default state. If strict checking is enabled,
    * then this method will throw an exception if the graphics state is
    * currently inside a draw block, or the model matrix stack is not empty.
    * 
    * @param strictChecking if <code>true</code>, enables strict
    * checking.
    * @throws IllegalStateException if strict checking is enabled and fails.
    */
   public void restoreDefaultState(boolean strictChecking);
}
