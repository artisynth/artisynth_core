package artisynth.demos.test;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Rectangle2D;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.widgets.*;
import maspack.properties.*;
import maspack.render.*;

import maspack.render.GL.GL2.*;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.ColorInterpolation;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.*;
import artisynth.core.gui.*;

public class RenderDemo extends RootModel {

   public static abstract class DrawBase extends RenderableComponentBase {

      public static PropertyList myProps =
         new PropertyList (DrawBase.class, RenderableComponentBase.class);

      static {
         myProps.add ("renderProps", "render properties", null);
      }

      public PropertyList getAllPropertyInfo() {
         return myProps;
      }

      public DrawBase() {
         setRenderProps (createRenderProps());
      }

      public void updateBounds (Point3d min, Vector3d max) {
         (new Point3d(0, 0, 0)).updateBounds (min, max);
         (new Point3d(1, 0, 1)).updateBounds (min, max);
      }

      public ControlPanel createControlPanel() {
         return null;
      }

      public AxisAngle getViewOrientation() {
         return new AxisAngle (1, 0, 0, Math.PI/2);
      }
   }

   private static class DrawAxes extends DrawBase {
      
      RigidTransform3d X;
      boolean select = true;

      public DrawAxes() {
         X = new RigidTransform3d();
         X.setRandom();
      }

      public void render (Renderer renderer, int flags) {
         renderer.setColor (Color.GRAY);
         //renderer.setSelectionHighlighting (true);
         renderer.drawAxes (X, 0.5, 4, false);
         // renderer.setLightingEnabled (false);
         // renderer.drawLine (0, 0, 0, 1, 0, 0);
         // renderer.setSelectionHighlighting (false);
         // renderer.drawLine (1, 0, 0, 1, 0, 1);
         // //renderer.setSelectionHighlighting (false);
         // renderer.setLightingEnabled (true);
      }
   }

   private static class DrawFrame extends DrawBase {

      public DrawFrame() {
      }

      private void drawArrowHead (Renderer renderer, int size) {

         Point3d p0 = new Point3d(size, 0, 0);
         Point3d p1 = new Point3d(-size, size/2, 0);
         Point3d p2 = new Point3d(-size, -size/2, 0);
         renderer.drawTriangle (p0, p1, p2);
      }         

      public void render (Renderer renderer, int flags) {
         int margin = 40;
         int arrowSize = 20;
         double length = 0.6*renderer.getScreenHeight();

         Point3d p0 = new Point3d (margin, margin, 0);
         Point3d px = new Point3d (margin+length, margin, 0);
         Point3d py = new Point3d (margin, margin+length, 0);

         renderer.setColor (Color.RED);
         renderer.setLineWidth (4);
         
         renderer.beginDraw (DrawMode.LINE_STRIP);
         renderer.addVertex (px);
         renderer.addVertex (p0);
         renderer.addVertex (py);
         renderer.endDraw();

         renderer.setLineWidth (1);

         renderer.translateModelMatrix (px.x, px.y, 0);
         drawArrowHead (renderer, arrowSize);
         renderer.translateModelMatrix (py.x-px.x, py.y-px.y, 0);
         renderer.rotateModelMatrix (90, 0, 0);
         drawArrowHead (renderer, arrowSize);
      }

      public int getRenderHints() {
         return TWO_DIMENSIONAL;
      }
   }

   private static class DrawOffset extends DrawBase {

      public DrawOffset() {
      }

      public void render (Renderer renderer, int flags) {

         renderer.setColor (Color.WHITE);
         renderer.drawCube (Vector3d.ZERO, 2.0);

         Point3d p0 = new Point3d (   0, -1.0, -0.6);
         Point3d p1 = new Point3d ( 0.6, -1.0,  0.6);
         Point3d p2 = new Point3d (-0.6, -1.0,  0.6);

         Point3d p3 = new Point3d ( 1.0, -0.6, -0.6);
         Point3d p4 = new Point3d ( 1.0,  0.6, -0.6);
         Point3d p5 = new Point3d ( 1.0,    0,  0.6);

         //renderer.pushModelMatrix();
         renderer.setDepthOffset (1);

         renderer.setColor (Color.RED);
         renderer.drawTriangle (p0, p1, p2);
         renderer.setColor (Color.BLUE);
         renderer.drawTriangle (p3, p4, p5);

         //renderer.popModelMatrix();         
      }
   }

   private static class DrawLine extends DrawBase {

      public DrawLine() {
         super();
         myRenderProps.setLineStyle (LineStyle.CYLINDER);
         myRenderProps.setLineColor (Color.BLUE);
         myRenderProps.setLineRadius (0.05);
      }

      public void render (Renderer renderer, int flags) {
         float[] p0 = new float[] {0, 0, 0.5f};
         float[] p1 = new float[] {1f, 0, 1f};

         renderer.drawLine (
            myRenderProps, p0, p1, null, /*capped=*/true, /*highlight=*/true);

         renderer.setPointSize (4);
         renderer.setShading (Shading.NONE);
         renderer.drawPoint (0, 0, 0);
         renderer.setShading (Shading.FLAT);
      }
   }

   private static class DrawLines extends DrawBase {

      public DrawLines() {
         super();
      }

      public void render (Renderer renderer, int flags) {
         float[] p0 = new float[] {-1f, 0, 0};
         float[] p1 = new float[] {1f, 0, 0};

         float[] p2 = new float[] {-1f, 0, 1f};
         float[] p3 = new float[] {1f, 0, 1f};

         float[] p4 = new float[] {-1f, 0, 2f};
         float[] p5 = new float[] {1f, 0, 2f};

         if (!renderer.isSelecting()) {
            renderer.setColor (myRenderProps.getLineColor());
         }
         renderer.drawCylinder (p0, p1, 0.1, /*capped=*/true);
         renderer.drawArrow (p2, p3, 0.1, /*capped=*/true);
         renderer.drawSpindle (p4, p5, 0.1);
      }
   }

   private static class DrawCone extends DrawBase {

      public DrawCone() {
         super();
      }

      public void render (Renderer renderer, int flags) {
         float[] p0 = new float[] {-1f, 0, 0};
         float[] p1 = new float[] {1f, 0, 0};

         renderer.setColor (0.7f, 0.7f, 1f);
         renderer.drawCone (p0, p1, 0.1, 0.5, /*capped=*/true);
      }
   }

   private static class DrawArrow extends DrawBase {

      public DrawArrow() {
         super();
         myRenderProps.setLineColor (Color.BLUE);
         myRenderProps.setLineStyle (LineStyle.CYLINDER);
         myRenderProps.setLineRadius (0.05);
      }

      public void render (Renderer renderer, int flags) {
         float[] p0 = new float[] {0, 0, 0.5f};
         float[] p1 = new float[] {1f, 0, 1f};

         renderer.setHighlighting (true);

         renderer.drawArrow (
            myRenderProps, p0, p1, /*capped=*/true, /*highlight=*/false);

         renderer.setShading (Shading.NONE);
         renderer.drawLine (0, 0, 0, 1, 0, 0);
         renderer.setHighlighting (false);
         renderer.drawLine (1, 0, 0, 1, 0, 1);
         //renderer.setSelectionHighlighting (false);
         renderer.setShading (Shading.FLAT);
      }
   }

   private static class DrawSpindle extends DrawBase {

      public DrawSpindle() {
         super();
         myRenderProps.setLineStyle (LineStyle.SPINDLE);
         myRenderProps.setLineColor (Color.BLUE);
         myRenderProps.setLineRadius (0.05);
      }

      public void render (Renderer renderer, int flags) {
         float[] p0 = new float[] {0, 0, 0.5f};
         float[] p1 = new float[] {1f, 0, 1f};

         renderer.setHighlighting (true);

         renderer.drawLine (
            myRenderProps, p0, p1, null, /*capped=*/true, /*highlight=*/false);

         renderer.setShading (Shading.NONE);
         renderer.drawLine (0, 0, 0, 1, 0, 0);
         //System.out.println ("enabled=" + renderer.isLightingEnabled());
         renderer.setHighlighting (false);
         renderer.drawLine (1, 0, 0, 1, 0, 1);
         //renderer.setSelectionHighlighting (false);
         renderer.setShading (Shading.FLAT);
      }
   }

   private static class DrawPoints extends DrawBase {

      public DrawPoints() {
         super();
         myRenderProps.setPointRadius (0.2);
         myRenderProps.setPointStyle (PointStyle.SPHERE);
         myRenderProps.setPointColor (Color.RED);
      }

      public void render (Renderer renderer, int flags) {
         float[] p0 = new float[] {0, 0, 0.5f};
         float[] p1 = new float[] {1f, 0, 1f};
         boolean highlight = false;

         renderer.setHighlighting (true);         

         renderer.setColor (Color.RED);
         renderer.drawPoint (myRenderProps, p0, highlight);
         renderer.drawPoint (myRenderProps, p1, highlight);
      }
   }

   private static class DrawLineStrip extends DrawBase {

      public DrawLineStrip() {
         super();
         myRenderProps.setLineRadius (0.05);
         myRenderProps.setLineColor (Color.BLUE);
      }

      public void render (Renderer renderer, int flags) {
         float[] p0 = new float[] {0, 0, 0.5f};
         float[] p1 = new float[] {1f, 0, 1f};
         float[] p2 = new float[] {0f, 0, 1f};

         ArrayList<float[]> pnts = new ArrayList<float[]>();
         pnts.add (p0);
         pnts.add (p1);
         pnts.add (p2);

         renderer.setHighlighting (true);

         renderer.drawLineStrip (
            myRenderProps, pnts, LineStyle.CYLINDER, /*highlight=*/true);

         renderer.setShading (Shading.NONE);
         renderer.drawLine (0, 0, 0, 1, 0, 0);
         renderer.setHighlighting (false);
         renderer.drawLine (1, 0, 0, 1, 0, 1);
         //renderer.setSelectionHighlighting (false);
         renderer.setShading (Shading.FLAT);
      }
   }

   private static class DrawSquare extends DrawBase {

      public void renderSingle (Renderer renderer) {
       // the corners of the square
         Vector3d p0 = new Vector3d (0, 0, 0);
         Vector3d p1 = new Vector3d (1, 0, 0);
         Vector3d p2 = new Vector3d (1, 0, 1);
         Vector3d p3 = new Vector3d (0, 0, 1);
         
         renderer.setShading (Shading.NONE);  // turn off lighting     

         renderer.setPointSize (6);
         renderer.setColor (Color.RED);
         renderer.drawPoint (p0);
         renderer.drawPoint (p1);
         renderer.drawPoint (p2);
         renderer.drawPoint (p3);

         renderer.setLineWidth (3);
         renderer.setColor (Color.BLUE);
         renderer.drawLine (p0, p1);
         renderer.drawLine (p1, p2);
         renderer.drawLine (p2, p3);        
         renderer.drawLine (p3, p0);        
         
         renderer.setShading (Shading.FLAT);  // restore lighting     
      }

      public void renderDrawMode (Renderer renderer) {
       // the corners of the square
         Vector3d p0 = new Vector3d (0, 0, 0);
         Vector3d p1 = new Vector3d (1, 0, 0);
         Vector3d p2 = new Vector3d (1, 0, 1);
         Vector3d p3 = new Vector3d (0, 0, 1);
         
         renderer.setShading (Shading.NONE);  // turn off lighting

         renderer.setPointSize (6);

         renderer.beginDraw (DrawMode.POINTS);
         renderer.setColor (Color.RED);
         renderer.addVertex (p0);
         renderer.addVertex (p1);
         renderer.addVertex (p2);
         renderer.addVertex (p3);
         renderer.endDraw();

         renderer.setLineWidth (3);
         renderer.setColor (Color.BLUE);
         renderer.beginDraw (DrawMode.LINE_LOOP);
         renderer.addVertex (p0);
         renderer.addVertex (p1);
         renderer.addVertex (p2);
         renderer.addVertex (p3);
         renderer.endDraw();

         renderer.setShading (Shading.FLAT);  // restore lighting
      }

      public void render (Renderer renderer, int flags) {
         renderSingle (renderer);
         //renderDrawMode (renderer);
      }
   }

   private static class DrawGrid extends DrawBase {

      public void updateBounds (Point3d min, Vector3d max) {
         (new Point3d(-1, -1, 0)).updateBounds (min, max);
         (new Point3d(1, 1, 0)).updateBounds (min, max);
      }

      RenderObject createGridObj (double w, double h, int nx, int ny) {
         
         RenderObject robj = new RenderObject();
         // create vertices and points ...
         for (int j=0; j<ny; j++) {
            for (int i=0; i<nx; i++) {
               float x = (float)(-w/2+i*w/(nx-1));
               float y = (float)(-h/2+j*h/(ny-1));
               int vi = robj.vertex (x, y, 0);
               robj.addPoint (vi);
            }
         }
         // create horizontal lines ...
         for (int j=0; j<ny; j++) {
            for (int i=0; i<nx-1; i++) {
               int v0 = j*nx + i;
               robj.addLine (v0, v0+1);
            }
         }
         // create vertical lines ...
         for (int i=0; i<nx; i++) {
            for (int j=0; j<ny-1; j++) {
               int v0 = j*nx + i;
               robj.addLine (v0, v0+nx);
            }
         }
         return robj;         
      }

      public void render (Renderer renderer, int flags) {

         RenderObject robj = createGridObj (2.0, 2.0, 5, 5);

         renderer.setColor (Color.RED);
         renderer.setPointSize (6);
         renderer.drawPoints (robj);
         renderer.setColor (0f, 0.5f, 0f); // dark green
         renderer.setLineWidth (3);
         renderer.drawLines (robj);
      }

      public void renderX (Renderer renderer, int flags) {

         RenderObject robj = createGridObj (2.0, 2.0, 5, 5);

         renderer.setColor (Color.RED);
         renderer.drawPoints (robj, PointStyle.SPHERE, 0.10);
         renderer.setColor (0f, 0.5f, 0f); // dark green
         renderer.drawLines (robj, LineStyle.SPINDLE, 0.05);
      }
   }

   private static class DrawBorderedTriangle extends DrawBase {

      public void updateBounds (Point3d min, Vector3d max) {
         (new Point3d(-1, -1, 0)).updateBounds (min, max);
         (new Point3d(1, 1, 0)).updateBounds (min, max);
      }

      RenderObject createRenderObj () {
         
         RenderObject robj = new RenderObject();

         // add vertices for outer border
         //robj.addNormal (0, 0, 1f);
         robj.vertex (-0.8f, -0.5f, 0);
         robj.vertex ( 0.8f, -0.5f, 0);
         robj.vertex ( 0.0f,  1.0f, 0);

         // add points for triangle
         robj.addNormal (0, 0, 1f);
         robj.vertex (-0.64f, -0.4f, 0);
         robj.vertex ( 0.64f, -0.4f, 0);
         robj.vertex ( 0.0f,   0.8f, 0);

         // add outer border
         robj.addLine (0, 1);
         robj.addLine (1, 2);
         robj.addLine (2, 0);

         // add triangle
         robj.addTriangle (3, 4, 5);

         return robj;         
      }

      RenderObject myRob;

      public void render (Renderer renderer, int flags) {
         if (myRob == null) {
            myRob = createRenderObj ();
         }
         renderer.setShading (Shading.NONE); // turn off lighting
         renderer.setColor (Color.RED);
         renderer.setLineWidth (3);
         renderer.drawLines (myRob);
         renderer.setShading (Shading.FLAT); // reset shading
         renderer.setColor (Color.GRAY);
         renderer.drawTriangles (myRob);
      }
   }

   private static class DrawSquares extends DrawBase {

      public void updateBounds (Vector3d min, Vector3d max) {
         (new Point3d(-4, -2, 0)).updateBounds (min, max);
         (new Point3d(4, 2, 0)).updateBounds (min, max);
      }

      public AxisAngle getViewOrientation() {
         return new AxisAngle (1, 0, 0, 0);
      }

      void addSquare (
         RenderObject robj, VertexIndexArray idxs,
         float x0, float y0, float x1, float y1, 
         float x2, float y2, float x3, float y3) {

         int v0 = robj.vertex (x0, y0, 0);
         int v1 = robj.vertex (x1, y1, 0);
         int v2 = robj.vertex (x2, y2, 0);
         int v3 = robj.vertex (x3, y3, 0);
         robj.addLine (v0, v1);
         robj.addLine (v1, v2);
         robj.addLine (v2, v3);
         robj.addLine (v3, v0);

         idxs.add (v0); idxs.add (v1); 
         idxs.add (v1); idxs.add (v2); 
         idxs.add (v2); idxs.add (v3); 
         idxs.add (v3); idxs.add (v0); 
      }

      RenderObject createRenderObj (VertexIndexArray idxs) {
         
         RenderObject robj = new RenderObject();
         addSquare (robj, idxs, -4f, -1f, -2f, -1f, -2f,  1f, -4f,  1f);
         addSquare (robj, idxs, -1f, -1f,  1f, -1f,  1f,  1f, -1f,  1f);
         addSquare (robj, idxs,  2f, -1f,  4f, -1f,  4f,  1f,  2f,  1f);
         return robj;         
      }

      RenderObject myRob;
      VertexIndexArray myIdxs;

      public void render (Renderer renderer, int flags) {
         if (myRob == null) {
            myIdxs = new VertexIndexArray();
            myRob = createRenderObj (myIdxs);
         }
         renderer.setLineWidth (4);
         renderer.setColor (Color.RED);
         renderer.drawVertices (myRob, myIdxs,  0, 8, DrawMode.LINES);
         renderer.setColor (Color.GREEN);
         renderer.drawVertices (myRob, myIdxs,  8, 8, DrawMode.LINES);
         renderer.setColor (Color.BLUE);
         renderer.drawVertices (myRob, myIdxs, 16, 8, DrawMode.LINES);
      }
   }

   private static class DrawSquaresX extends DrawBase {

      public void updateBounds (Vector3d min, Vector3d max) {
         (new Point3d(-4, -2, 0)).updateBounds (min, max);
         (new Point3d(4, 2, 0)).updateBounds (min, max);
      }

      public AxisAngle getViewOrientation() {
         return new AxisAngle (1, 0, 0, 0);
      }

      void addSquare (
         RenderObject robj, FeatureIndexArray fidxs, int fnum,
         float x0, float y0, float x1, float y1, 
         float x2, float y2, float x3, float y3) {

         int v0 = robj.vertex (x0, y0, 0);
         int v1 = robj.vertex (x1, y1, 0);
         int v2 = robj.vertex (x2, y2, 0);
         int v3 = robj.vertex (x3, y3, 0);
         robj.addLine (v0, v1);
         robj.addLine (v1, v2);
         robj.addLine (v2, v3);
         robj.addLine (v3, v0);
         fidxs.beginFeature (fnum);
         fidxs.addVertex (v0); fidxs.addVertex (v1); 
         fidxs.addVertex (v1); fidxs.addVertex (v2); 
         fidxs.addVertex (v2); fidxs.addVertex (v3); 
         fidxs.addVertex (v3); fidxs.addVertex (v0); 
         fidxs.endFeature();
      }

      RenderObject createRenderObj (FeatureIndexArray idxs) {
         
         RenderObject robj = new RenderObject();
         addSquare (robj, idxs, 0, -4f, -1f, -2f, -1f, -2f,  1f, -4f,  1f);
         addSquare (robj, idxs, 1, -1f, -1f,  1f, -1f,  1f,  1f, -1f,  1f);
         addSquare (robj, idxs, 2,  2f, -1f,  4f, -1f,  4f,  1f,  2f,  1f);
         return robj;         
      }

      RenderObject myRob;
      FeatureIndexArray myFidxs;

      void drawSquare (
         Renderer renderer, RenderObject robj,
         FeatureIndexArray fidxs, int fnum) {

         renderer.drawVertices (
            robj, fidxs.getVertices(),
            fidxs.getFeatureOffset(fnum), fidxs.getFeatureLength(fnum),
            DrawMode.LINES);
      }

      public void render (Renderer renderer, int flags) {
         if (myRob == null) {
            myFidxs = new FeatureIndexArray();
            myRob = createRenderObj (myFidxs);
         }
         renderer.setLineWidth (4);
         renderer.setColor (Color.RED);
         drawSquare (renderer, myRob, myFidxs, 0);
         renderer.setColor (Color.GREEN);
         drawSquare (renderer, myRob, myFidxs, 1);
         renderer.setColor (Color.BLUE);
         drawSquare (renderer, myRob, myFidxs, 2);
      }
   }

   private static class DrawText extends DrawBase {

      public void updateBounds (Point3d min, Vector3d max) {
         (new Point3d(0, -1, 0)).updateBounds (min, max);
         (new Point3d(1, 1, 0)).updateBounds (min, max);
      }

      public DrawText() {
         setupFonts();
      }

      Font myComic;
      Font mySerif;

      void setupFonts() {
         GraphicsEnvironment env =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
         for (Font font : env.getAllFonts ()) {
            if (font.getName().equals ("Comic Sans MS")) {
               myComic = font;
               break;
            }
         }
         if (myComic == null) {
            myComic = new Font(Font.MONOSPACED, Font.BOLD, 32);
         }
         else {
            myComic = myComic.deriveFont (Font.BOLD, 32);
         }
         mySerif = new Font(Font.SERIF, Font.PLAIN, 64);
      }

      public void render (Renderer renderer, int flags) {

         Vector3d pos = new Vector3d(-0.6, 0, 0);

         // draw text at (0,0)
         renderer.setShading (Shading.NONE);
         renderer.setColor (Color.WHITE);
         renderer.drawText ("Hello World!", pos, 0.2);

         // draw text rotated about the z axis
         renderer.setColor (Color.CYAN);
         String text = "Cowabunga";
         renderer.pushModelMatrix();
         renderer.rotateModelMatrix (30, 0, 0);
         pos.set (-0.3, 0.6, 0);
         renderer.drawText (myComic, "Cowabunga", pos, 0.25);
         renderer.popModelMatrix();

         // draw several centered lines, in a plane rotated about the x axis
         renderer.setColor (Color.ORANGE);
         renderer.pushModelMatrix();
         String[] textLines = new String[] {
            "Four score and", "seven years ago,",
            "in a galaxy", "far far", "away" };
         renderer.mulModelMatrix (
            new RigidTransform3d (0, -0.1, -1.0, 0, 0, -Math.toRadians(60)));
         pos.set (0, 0, 0);
         for (String line : textLines) {
            Rectangle2D rect = renderer.getTextBounds (mySerif, line, 0.25);
            pos.y -= rect.getHeight();
            pos.x = -rect.getWidth()/2;
            renderer.drawText (mySerif, line, pos, 0.25);
         }
         renderer.popModelMatrix();
      }

      public AxisAngle getViewOrientation() {
         return new AxisAngle (1, 0, 0, 0);
      }
   }

   private static class DrawGridX extends DrawBase {

      RenderObject createGridObj (double w, double h, int nx, int ny) {
         
         RenderObject robj = new RenderObject();
         // create vertices and points ...
         for (int j=0; j<ny; j++) {
            for (int i=0; i<nx; i++) {
               float x = (float)(-w/2+i*w/(nx-1));
               float y = (float)(-h/2+j*h/(ny-1));
               int vi = robj.vertex (x, y, 0);
               robj.addPoint (vi);
            }
         }
         // create horizontal lines inside first line group ...
         robj.createLineGroup();
         for (int j=0; j<ny; j++) {
            for (int i=0; i<nx-1; i++) {
               int v0 = j*nx + i;
               robj.addLine (v0, v0+1);
            }
         }
         // create vertical lines inside second line group ...
         robj.createLineGroup();
         for (int i=0; i<nx; i++) {
            for (int j=0; j<ny-1; j++) {
               int v0 = j*nx + i;
               robj.addLine (v0, v0+nx);
            }
         }
         return robj;         
      }

      public void render (Renderer renderer, int flags) {

         RenderObject grid = createGridObj (2.0, 2.0, 5, 5);

         renderer.setColor (Color.RED);
         renderer.drawPoints (grid, PointStyle.SPHERE, 0.10);
         renderer.setColor (0f, 0.5f, 0f); // dark green
         grid.lineGroup (0);
         renderer.drawLines (grid, LineStyle.SPINDLE, 0.05);
         renderer.setColor (Color.YELLOW);
         grid.lineGroup (1);
         renderer.drawLines (grid, LineStyle.SPINDLE, 0.05);
      }
   }

   private static class DrawOpenTet extends DrawBase {
      
      public void renderSingle (Renderer renderer) {
         // the corners of the tetrahedron
         Vector3d p0 = new Vector3d (0, 0, 0);
         Vector3d p1 = new Vector3d (1, 0, 0);
         Vector3d p2 = new Vector3d (0, 1, 0);
         Vector3d p3 = new Vector3d (0, 0, 1);
         
         // render both sides of each triangle:
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); 
         //renderer.setShading (Shading.NONE);
         renderer.setBackColor (new float[] {0, 0, 1});

         renderer.drawTriangle (p0, p2, p1);
         renderer.drawTriangle (p0, p3, p2);
         renderer.drawTriangle (p0, p1, p3);

         renderer.setFaceStyle (FaceStyle.FRONT); // restore default
      }

      public void renderDrawMode (Renderer renderer) {
         // the corners of the tetrahedron
         Vector3d p0 = new Vector3d (0, 0, 0);
         Vector3d p1 = new Vector3d (1, 0, 0);
         Vector3d p2 = new Vector3d (0, 1, 0);
         Vector3d p3 = new Vector3d (0, 0, 1);
         
         // render both sides of each triangle:
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); 

         renderer.beginDraw (DrawMode.TRIANGLES);
         // first triangle
         renderer.setNormal (0, 0, -1);
         renderer.addVertex (p0); 
         renderer.addVertex (p2);
         renderer.addVertex (p1);
         // second triangle
         renderer.setNormal (-1, 0, 0);
         renderer.addVertex (p0); 
         renderer.addVertex (p3);
         renderer.addVertex (p2);
         // third triangle
         renderer.setNormal (0, -1, 0);
         renderer.addVertex (p0);
         renderer.addVertex (p1);
         renderer.addVertex (p3);
         renderer.endDraw();

         renderer.setFaceStyle (FaceStyle.FRONT); // restore default
      }

      public void renderColors (Renderer renderer) {
         // the corners of the tetrahedron
         Vector3d p0 = new Vector3d (0, 0, 0);
         Vector3d p1 = new Vector3d (1, 0, 0);
         Vector3d p2 = new Vector3d (0, 1, 0);
         Vector3d p3 = new Vector3d (0, 0, 1);

         renderer.setShading (myRenderProps.getShading());
         renderer.setColorInterpolation (ColorInterpolation.HSV);
         
         // render both sides of each triangle:
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); 

         renderer.beginDraw (DrawMode.TRIANGLES);
         // first triangle
         renderer.setNormal (0, 0, -1);
         renderer.setColor (Color.RED);
         renderer.addVertex (p0); 
         renderer.setColor (Color.BLUE);
         renderer.addVertex (p2);
         renderer.setColor (Color.GREEN);
         renderer.addVertex (p1);
         // second triangle
         renderer.setNormal (-1, 0, 0);
         renderer.setColor (Color.RED);
         renderer.addVertex (p0); 
         renderer.setColor (Color.CYAN);
         renderer.addVertex (p3);
         renderer.setColor (Color.BLUE);
         renderer.addVertex (p2);
         // third triangle
         renderer.setNormal (0, -1, 0);
         renderer.setColor (Color.RED);
         renderer.addVertex (p0);
         renderer.setColor (Color.GREEN);
         renderer.addVertex (p1);
         renderer.setColor (Color.CYAN);
         renderer.addVertex (p3);
         renderer.endDraw();

         renderer.setFaceStyle (FaceStyle.FRONT); // restore default
      }

      public void render (Renderer renderer, int flags) {
         //renderSingle (renderer);
         //renderDrawMode (renderer);
         renderColors (renderer);
      }
   }

   private static class DrawRenderTet extends DrawBase {

      public void render (Renderer renderer, int flags) {
         
         // the corners of the tetrahedron
         
         RenderObject robj = new RenderObject();
         renderer.setShading (myRenderProps.getShading());
         renderer.setColorInterpolation (ColorInterpolation.HSV);

         // add positions and normals
         int pi0 = robj.addPosition (0, 0, 0);
         int pi1 = robj.addPosition (1, 0, 0);
         int pi2 = robj.addPosition (0, 1, 0);
         int pi3 = robj.addPosition (0, 0, 1);

         int ni0 = robj.addNormal (0, 0, -1);
         int ni1 = robj.addNormal (-1, 0, 0);
         int ni2 = robj.addNormal (0, -1, 0);

         int r = robj.addColor (Color.RED);
         int g = robj.addColor (Color.GREEN);
         int b = robj.addColor (Color.BLUE);
         int c = robj.addColor (Color.CYAN);


         // add three vertices per triangle, each with position and normal
         // information and color and texture coords undefined, and then
         // use these to define a triangle primitive

         int v0, v1, v2;

         // first triangle
         v0 = robj.addVertex (pi0, ni0, r, -1);
         v1 = robj.addVertex (pi2, ni0, b, -1);
         v2 = robj.addVertex (pi1, ni0, g, -1);
         robj.addTriangle (v0, v1, v2);

         // second triangle
         v0 = robj.addVertex (pi0, ni1, r, -1);
         v1 = robj.addVertex (pi3, ni1, c, -1);
         v2 = robj.addVertex (pi2, ni1, b, -1);
         robj.addTriangle (v0, v1, v2);

         // third triangle
         v0 = robj.addVertex (pi0, ni2, r, -1);
         v1 = robj.addVertex (pi1, ni2, g, -1);
         v2 = robj.addVertex (pi3, ni2, c, -1);
         robj.addTriangle (v0, v1, v2);


         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); 
         renderer.drawTriangles (robj);
         renderer.setFaceStyle (FaceStyle.FRONT); 
      }
   }

   private static class DrawRenderTet2 extends DrawBase {

      public void render (Renderer renderer, int flags) {
         
         // the corners of the tetrahedron
         
         RenderObject robj = new RenderObject();

         // add three vertices per triangle, each with position and normal
         // information and color and texture coords undefined, and then
         // use these to define a triangle primitive

         int v0, v1, v2;

         // first triangle
         robj.addNormal (0, 0, -1);
         v0 = robj.vertex (0, 0, 0);
         v1 = robj.vertex (0, 1, 0);
         v2 = robj.vertex (1, 0, 0);
         robj.addTriangle (v0, v1, v2);

         // second triangle
         robj.addNormal (-1, 0, 0);
         v0 = robj.vertex (0, 0, 0);
         v1 = robj.vertex (0, 0, 1);
         v2 = robj.vertex (0, 1, 0);
         robj.addTriangle (v0, v1, v2);

         // third triangle
         robj.addNormal (0, -1, 0);
         v0 = robj.vertex (0, 0, 0);
         v1 = robj.vertex (1, 0, 0);
         v2 = robj.vertex (0, 0, 1);
         robj.addTriangle (v0, v1, v2);

         
         renderer.setColor (0.8f, 0.8f, 0.8f);
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); 
         renderer.drawTriangles (robj);
         renderer.setFaceStyle (FaceStyle.FRONT); 
      }
   }

   private static class DrawRenderTet3 extends DrawBase {

      public void render (Renderer renderer, int flags) {
         
         // the corners of the tetrahedron
         
         RenderObject robj = new RenderObject();

         // add positions and normals
         int pi0 = robj.addPosition (0, 0, 0);
         int pi1 = robj.addPosition (1, 0, 0);
         int pi2 = robj.addPosition (0, 1, 0);
         int pi3 = robj.addPosition (0, 0, 1);

         // add three vertices per triangle, each with position and normal
         // information and color and texture coords undefined, and then
         // use these to define a triangle primitive

         int v0, v1, v2;

         // first triangle
         robj.addNormal (0, 0, -1);
         robj.addColor (Color.CYAN);
         v0 = robj.addVertex (pi0);
         v1 = robj.addVertex (pi2);
         v2 = robj.addVertex (pi1);
         robj.addTriangle (v0, v1, v2);

         // second triangle
         robj.addNormal (-1, 0, 0);
         robj.addColor (Color.WHITE);
         v0 = robj.addVertex (pi0);
         v1 = robj.addVertex (pi3);
         v2 = robj.addVertex (pi2);
         robj.addTriangle (v0, v1, v2);

         // third triangle
         robj.addNormal (0, -1, 0);
         robj.addColor (Color.MAGENTA);
         v0 = robj.addVertex (pi0);
         v1 = robj.addVertex (pi1);
         v2 = robj.addVertex (pi3);
         robj.addTriangle (v0, v1, v2);

         
         renderer.setColor (0.8f, 0.8f, 0.8f);
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); 
         renderer.drawTriangles (robj);
         renderer.setFaceStyle (FaceStyle.FRONT); 
      }
   }

   private static class DrawBasic extends DrawBase {

      public void render (Renderer renderer, int flags) {
         Vector3d pnt0 = new Vector3d (-1.0, 0, 0);
         Vector3d pnt1 = new Vector3d (1.0, 0.5, 0);

         renderer.setColor (Color.GRAY);
         renderer.drawSphere (pnt0, 0.5); // sphere with radius 0.5
         renderer.drawSphere (pnt1, 0.5); // sphere with radius 0.5
         renderer.setColor (Color.BLUE);
         renderer.drawCylinder (
            pnt0, pnt1, 0.1, /*capped=*/false); // line with radius 0.1
      }
   }

   private static class DrawCylinder extends DrawBase {

      public void render (Renderer renderer, int flags) {
         
         RenderObject cylinder = new RenderObject();
         int nSlices = 32;
         float height = 2;
      
         // reserve memory
         cylinder.ensurePositionCapacity(2*nSlices);  // top and bottom ring
         cylinder.ensureNormalCapacity(nSlices+2);    // sides and caps
         cylinder.ensureVertexCapacity(4*nSlices);    // top/bottom sides, top/bottom caps
         cylinder.ensureTriangleCapacity(2*nSlices+2*(nSlices-2));  // sides, caps
      
         // create cylinder sides
         cylinder.beginBuild(DrawMode.TRIANGLE_STRIP);
         for (int i=0; i<nSlices; i++) {
            double angle = 2*Math.PI/nSlices*i;
            float x = (float)Math.cos(angle);
            float y = (float)Math.sin(angle);
            cylinder.addNormal(x, y, 0);
            cylinder.vertex(x, y, height);  // top
            cylinder.vertex(x, y, 0);       // bottom
         }
         cylinder.endBuild();

         // connect ends around cylinder
         cylinder.addTriangle(2*nSlices-2, 2*nSlices-1, 0);
         cylinder.addTriangle(0, 2*nSlices-1, 1);
      
         // create top cap, using addVertex(pidx) to reuse positions that 
         // were added when building the sides 
         cylinder.beginBuild(DrawMode.TRIANGLE_FAN);
         cylinder.addNormal(0,0,1);
         for (int i=0; i<nSlices; i++) {
            cylinder.addVertex(2*i);    // even positions (top)
         }
         cylinder.endBuild();

         //  create bottom cap
         cylinder.beginBuild(DrawMode.TRIANGLE_FAN);
         cylinder.addNormal(0,0,-1);
         cylinder.addVertex(1);
         for (int i=1; i<nSlices; i++) {
            int j = nSlices-i;
            cylinder.addVertex(2*j+1);  // odd positions (bottom)
         }
         cylinder.endBuild();
         renderer.setShading (Shading.FLAT);
         renderer.setColor (0.8f, 0.8f, 0.8f);
         renderer.draw (cylinder);
      }
   }

   public static class DrawMappings extends DrawBase {

      ColorMapProps myTextureProps = null;
      NormalMapProps myNormalProps = null;
      BumpMapProps myBumpProps = null;

      boolean myTextureMapEnabled = true;
      boolean myNormalMapEnabled = true;
      boolean myBumpMapEnabled = true;

      RenderObject myRenderObj;

      public static PropertyList myProps =
         new PropertyList (DrawMappings.class, DrawBase.class);

      static {
         myProps.add ("textureMap", "texture map enabled", true);
         myProps.add ("normalMap", "normal map enabled", true);
         myProps.add ("bumpMap", "bump map enabled", true);
      }

      public void setTextureMap (boolean enable) {
         myTextureMapEnabled = enable;
      }

      public boolean getTextureMap () {
         return myTextureMapEnabled;
      }

      public void setNormalMap (boolean enable) {
         myNormalMapEnabled = enable;
      }

      public boolean getNormalMap () {
         return myNormalMapEnabled;
      }

      public void setBumpMap (boolean enable) {
         myBumpMapEnabled = enable;
      }

      public boolean getBumpMap () {
         return myBumpMapEnabled;
      }

      public PropertyList getAllPropertyInfo() {
         return myProps;
      }

      public DrawMappings() {
         createTextureProps();
         createNormalProps();
         createBumpProps();
         RenderProps.setShading (this, Shading.SMOOTH);
      }

      public void updateBounds (Point3d min, Vector3d max) {
         (new Point3d(-3, -1, 0)).updateBounds (min, max);
         (new Point3d( 3,  1, 0)).updateBounds (min, max);
      }

      String getDataFolder() {
         return PathFinder.expand ("${srcdir RenderDemo}");
      }

      public void createTextureProps() {
         // create texture mapping
         myTextureProps = new ColorMapProps ();
         myTextureProps.setFileName (getDataFolder()+"/texture_map.jpg");
         myTextureProps.setEnabled (true);         
      }

      public void createNormalProps() {
         // create normal mapping
         myNormalProps = new NormalMapProps ();
         myNormalProps.setFileName (getDataFolder()+"/foil_normal_map.png");
         myNormalProps.setScaling (1f);
         myNormalProps.setEnabled (true);         
      }

      public void createBumpProps() {
         // create normal mapping
         myBumpProps = new BumpMapProps ();
         myBumpProps.setFileName (getDataFolder()+"/egyptian_friz.png");
         myBumpProps.setScaling (2.5f);
         myBumpProps.setEnabled (true);         
      }

      public void prerender (RenderList list) {

         // create render object if necessary. This is a simple 6 x 2 plane,
         // centered on the origin, created from two triangles, with texture
         // coordinates assigned to each vertex.
         if (myRenderObj == null) {
            RenderObject robj = new RenderObject();

            robj.addNormal (0, 0, 1f);
            robj.addTextureCoord (0, 0);
            robj.vertex (-3f, -1f, 0f);
            robj.addTextureCoord (1, 0);
            robj.vertex ( 3f, -1f, 0);
            robj.addTextureCoord (1, 1);
            robj.vertex ( 3f,  1f, 0);
            robj.addTextureCoord (0, 1);
            robj.vertex (-3f,  1f, 0);

            robj.addTriangle (0, 1, 2);
            robj.addTriangle (0, 2, 3);

            myRenderObj = robj;
         }
      }           

      public void render (Renderer renderer, int flags) {

         float[] greenGold = new float[] {0.61f, 0.77f, 0.12f};
         float[] yellowGold = new float[] {1f, 0.44f, 0f};

         renderer.setShininess (20);                       // increase shininess
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); // see both sides
         renderer.setColor (greenGold);                    // base color
         renderer.setSpecular (yellowGold);                // reflected color
         renderer.setShading (myRenderProps.getShading());

         // set texture, normal and bump mappings if their properties are

         if (myTextureMapEnabled) {
            renderer.setColorMap (myTextureProps); 
         }
         if (myNormalMapEnabled) {
            renderer.setNormalMap (myNormalProps); 
         }
         if (myBumpMapEnabled) {
            renderer.setBumpMap (myBumpProps); 
         }

         if (false) {
            
            renderer.beginDraw (DrawMode.TRIANGLES);

            renderer.setNormal (0, 0, 1f);

            // first triangle
            renderer.setTextureCoord (0, 1);
            renderer.addVertex (-3f, -1f, 0f);
            renderer.setTextureCoord (1, 1);
            renderer.addVertex ( 3f, -1f, 0);
            renderer.setTextureCoord (1, 0);
            renderer.addVertex ( 3f,  1f, 0);

            // second triangle
            renderer.setTextureCoord (0, 1);
            renderer.addVertex (-3f, -1f, 0f);
            renderer.setTextureCoord (1, 0);
            renderer.addVertex ( 3f,  1f, 0);
            renderer.setTextureCoord (0, 0);
            renderer.addVertex (-3f,  1f, 0);

            renderer.endDraw(); 
         }
         else {
            renderer.drawTriangles (myRenderObj);
         }

         renderer.setColorMap (null);
         renderer.setNormalMap (null);
         renderer.setBumpMap (null);
      }

      public ControlPanel createControlPanel() {
         ControlPanel panel = new ControlPanel();
         panel.addWidget (this, "textureMap");
         panel.addWidget (this, "normalMap");
         panel.addWidget (this, "bumpMap");
         return panel;
      }

      public AxisAngle getViewOrientation() {
         return new AxisAngle (1, 0, 0, 0);
      }

   }

   // public static class DrawMappingsMesh extends DrawBase {

   //    String getDataFolder() {
   //       return PathFinder.expand ("${srcdir RenderDemo}");
   //    }

   //    ColorMapProps myTextureProps = null;
   //    NormalMapProps myNormalProps = null;
   //    BumpMapProps myBumpProps = null;

   //    boolean myTextureMapEnabled = true;
   //    boolean myNormalMapEnabled = true;
   //    boolean myBumpMapEnabled = true;

   //    public DrawMappingsMesh() {
   //       setupMeshAndProps();
   //    }

   //    public void createMappingProps() {

   //       // create texture mapping
   //       myTextureProps = new ColorMapProps ();
   //       myTextureProps.setFileName (getDataFolder()+"/texture_map.jpg");
   //       myTextureProps.setEnabled (true);         

   //       // create normal mapping
   //       myNormalProps = new NormalMapProps ();
   //       myNormalProps.setFileName (getDataFolder()+"/foil_normal_map.png");
   //       myNormalProps.setScaling (1f);
   //       myNormalProps.setEnabled (true);         

   //       // create normal mapping
   //       myBumpProps = new BumpMapProps ();
   //       myBumpProps.setFileName (getDataFolder()+"/egyptian_friz.png");
   //       myBumpProps.setScaling (2.5f);
   //       myBumpProps.setEnabled (true);         
   //    }

   //    PolygonalMesh myMesh;
   //    PolygonalMeshRenderer myRenderer;
   //    RenderProps myRenderProps;

   //    void setupMeshAndProps() {

   //       // create a 6 x 2 plane mesh made from two triangles
   //       myMesh = MeshFactory.createRectangle (6, 2, 1, 1, /*texture=*/true);

   //       float[] greenGold = new float[] {0.61f, 0.77f, 0.12f};
   //       float[] yellowGold = new float[] {1f, 0.44f, 0f};

   //       RenderProps props = new RenderProps();

   //       props.setShininess (10);                       // increase shininess
   //       props.setFaceStyle (FaceStyle.FRONT_AND_BACK); // see both sides
   //       props.setFaceColor (greenGold);                // base color
   //       props.setSpecular (yellowGold);                // reflected color

   //       // create mapping properties and set them in the render properties
   //       createMappingProps();
   //       if (myTextureMapEnabled) {
   //          props.setColorMap (myTextureProps);
   //       }
   //       if (myNormalMapEnabled) {
   //          props.setNormalMap (myNormalProps);
   //       }
   //       if (myBumpMapEnabled) {
   //          props.setBumpMap (myBumpProps);
   //       }
   //       myRenderProps = props;
   //    }

   //    public void prerender (RenderList list) {
   //       if (myRenderer == null) {
   //          myRenderer = new PolygonalMeshRenderer();
   //       }
   //       myRenderer.prerender (myMesh, myRenderProps);
   //    }

   //    public void render (Renderer renderer, int flags) {
   //       myRenderer.render (renderer, myMesh, myRenderProps, flags);
   //    }
   // }

   public void build (String[] args) {
      RandomGenerator.setSeed (0x2234);

      DrawBase drawable;
      //drawable = new DrawSquare();
      //drawable = new DrawAxes();
      //drawable = new DrawPoints();
      //drawable = new DrawArrow();
      //drawable = new DrawLineStrip();
      //drawable = new DrawSpindle();
      //drawable = new DrawOpenTet();
      //drawable = new DrawGrid();
      //drawable = new DrawGridX();
      //drawable = new DrawBasic();
      //drawable = new DrawFrame();
      //drawable = new DrawRenderTet();
      //drawable = new DrawRenderTet3();
      //drawable = new DrawCylinder();
      //drawable = new DrawCone();
      //drawable = new DrawLines();
      //drawable = new DrawMappings();
      //drawable = new DrawOffset();
      //drawable = new DrawBorderedTriangle();
      //drawable = new DrawMappingsMesh();
      drawable = new DrawText();
      //drawable = new DrawSquares();
      //drawable = new DrawSquaresX();

      addRenderable (drawable);
      setDefaultViewOrientation (drawable.getViewOrientation());

      ControlPanel panel = drawable.createControlPanel();
      if (panel != null) {
         addControlPanel (panel);
      }
      
   }
}
