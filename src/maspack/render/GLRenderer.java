/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

//import javax.media.opengl.*;
//import javax.media.opengl.glu.*;
import java.awt.Color;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps.LineStyle;

public interface GLRenderer {

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
    * Flag requesting that all display lists be cleared
    */
   public static int CLEAR_MESH_DISPLAY_LISTS = 0x10;

   /**
    * Flag requesting components refresh any custom rendering info
    */
   public static int REFRESH = 0x20;
   
   // /** 
   //  * Flag requesting that the renderer is in selection mode.
   //  */   
   // public static int IS_SELECTING = 0x80000000;
   
   // /** 
   //  * Flag requesting that mesh edges should be drawn.
   //  */
   // public static int DRAW_EDGES = 0x40000000;


   public enum SelectionHighlighting {
      None, Color
   };

   public int getHeight();

   public int getWidth();

   public double getViewPlaneHeight();

   public double getViewPlaneWidth();

   // public double getFieldOfViewY();

   public double centerDistancePerPixel ();
   public double distancePerPixel (Vector3d p);

   public boolean isOrthogonal();

   public double getNearClipPlaneZ();

   public double getFarClipPlaneZ();

   public GL getGL();
   
   public GL2 getGL2();

   public GLU getGLU();

   public void rerender();

   // public RigidTransform3d getWorldToEye();

   public RigidTransform3d getEyeToWorld();

   // public void swapBuffers();

   // public boolean getAutoSwapBufferMode();

   // public void setAutoSwapBufferMode (boolean onOrOff);

   // GLContext getContext();

   /**
    * Returns true if the renderer is currently performing a selection
    * render. When this is the case, the application should avoid GL calls
    * affecting color and lighting.
    *
    * @return true if the renderer is in selection mode.
    */
   public boolean isSelecting();

   /**
    * Returns whether or not GL_LIGHTING is enabled.
    */
   public boolean isLightingEnabled();

   /**
    * Enables or disables GL_LIGHTING. The reason for doing this through the
    * renderer, rather than calling GL directly, is so that the renderer can
    * discard the request when performing a selection render.
    */
   public void setLightingEnabled (boolean enable);

   public void setColor (float[] rgb, boolean selected);

   public void setColor (float[] rgb);

   public void setColor (Color color);

   public void updateColor (float[] rgb, boolean selected);

   public void setColor (float r, float g, float b);

   public void setColor (float r, float g, float b, float a);

   public void setMaterial (Material material, boolean selected);

   public void setMaterialAndShading (
      RenderProps props, Material mat, boolean selected);
   
   public void setMaterialAndShading (
      RenderProps props, Material mat, float[] diffuseColor, boolean selected);
   
   public void restoreShading (RenderProps props);

   public void updateMaterial (
      RenderProps props, Material material, boolean selected);

   public void updateMaterial (
      RenderProps props, Material mat, float[] diffuseColor, boolean selected);

   public void setLineWidth (int width);
   
   public int getLineWidth ();
   
   public void setPointSize (int size);
   
   public int getPointSize ();
   
   public RenderProps.Shading getShadeModel();
   
   public void setShadeModel (RenderProps.Shading shading);
   
   public void drawSphere (RenderProps props, float[] coords);
   
   public void drawSphere (RenderProps props, float[] coords, double r);

   public void drawTet (RenderProps props, double scale,
                        float[] v0, float[] v1, float[] v2, float[] v3);

   public void drawHex (RenderProps props, double scale,
                        float[] v0, float[] v1, float[] v2, float[] v3,
                        float[] v4, float[] v5, float[] v6, float[] v7);

   public void drawWedge (RenderProps props, double scale,
                          float[] v0, float[] v1, float[] v2, 
                          float[] v3, float[] v4, float[] v5);

   public void drawPyramid (RenderProps props, double scale,
                          float[] v0, float[] v1, float[] v2, 
                          float[] v3, float[] v4);

   public void drawTaperedEllipsoid (
      RenderProps props, float[] coords0, float[] coords1);

   public void drawCylinder (RenderProps props, float[] coords0, float[] coords1);

   public void drawCylinder (
      RenderProps props, float[] coords0, float[] coords1, boolean capped);

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean selected);

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      float[] color, boolean selected);

   public void drawLine (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      boolean selected);

   public void drawSolidArrow (
      RenderProps props, float[] coords0, float[] coords1, boolean capped);

   public void drawSolidArrow (
      RenderProps props, float[] coords0, float[] coords1);

   public void drawArrow (
      RenderProps props, float[] coords0, float[] coords1, boolean capped,
      boolean selected);

   public void drawLines (
      RenderProps props, Iterator<? extends RenderableLine> iterator);

   public void drawLineStrip (
      RenderProps props, Iterable<float[]> vertexList, 
      LineStyle style, boolean isSelected);

   public void drawPoint (RenderProps props, float[] coords, boolean selected);

   public void drawPoints (
      RenderProps props, Iterator<? extends RenderablePoint> iterator);

   public void drawAxes (
      RenderProps props, RigidTransform3d X, double len, boolean selected);

//   public void drawAxes (
//      RenderProps props, RigidTransform3d X, double[] len, boolean selected);
   
   //public void drawMesh (RenderProps props, PolygonalMesh mesh, int flags);

   public boolean isTransparencyEnabled();

   public void setTransparencyEnabled (boolean enable);

   // public void drawXYGrid (double size, int numcells);

   public void setFaceMode (RenderProps.Faces mode);

   public void setDefaultFaceMode();

   public void mulTransform (AffineTransform3d X);

   public void mulTransform (RigidTransform3d X);

   public void pushMatrix();
   
   public void popMatrix();
   
   public SelectionHighlighting getSelectionHighlighting();

   public Color getSelectionColor();

   public Material getSelectionMaterial();
   
   /**
    * Direction pointing out of monitor
    */
   public Vector3d getZDirection();
   
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

   /**
    * Begins a selection query with the {\it query identifier}
    * <code>qid</code>.  If the rendering that occurs between this call and the
    * subsequent call to {@link #endSelectionQuery} results in anything being
    * rendered within the selection window, a {\it selection hit} will be
    * generated for <code>qid</code>.
    *
    * If called within the <code>render</code> method for a {@link
    * maspack.render.GLSelectable}, then <code>qid</code> must lie in the range
    * 0 to <code>numq</code>-1, where <code>numq</code> is the value returned
    * by {@link maspack.render.GLSelectable#numSelectionQueriesNeeded
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
    * Begins selection for a {@link maspack.render.GLSelectable}
    * <code>s</code>that
    * manages its own selection; this call should
    * be used in place of {@link #beginSelectionQuery} for such objects.
    * Selectables that manage their own selection are identified by
    * having a value <code>numq</code> >= 0, where <code>numq</code>
    * is the value returned by
    * {@link maspack.render.GLSelectable#numSelectionQueriesNeeded
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
    * restricts which objects can actually be selected. This allows
    * selection of objects that might otherwise be occluded within a scene.
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
   
   /**
    * Should be called before creating external display lists involving spheres,
    * ellipses, etc...
    * 
    * Ensures all internal display lists are up-to-date.  Otherwise,
    * there are cases where we create a display list outside the renderer,
    * but when rendering, an internal display list creation is triggered,
    * leading to a GL "invalid operation" error.
    */
   public void validateInternalDisplayLists(RenderProps props);
   
   public int checkGLError();
   
   public boolean checkAndPrintGLError();
   
}
