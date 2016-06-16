/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.swing.JToolBar;

import maspack.geometry.MeshFactory;
import maspack.geometry.NURBSCurve2d;
import maspack.geometry.Polygon2d;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.QuadBezierDistance2d;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector4d;
import maspack.render.DrawToolEvent;
import maspack.render.DrawToolListener;
import maspack.render.IsRenderableBase;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLViewer;
import maspack.util.IndentingPrintWriter;
import maspack.util.ReaderTokenizer;
import maspack.widgets.DraggerToolBar;
import maspack.widgets.DraggerToolBar.ButtonType;
import maspack.widgets.ViewerFrame;

public class NURBSCurve2dTest implements DrawToolListener {

   ViewerFrame myFrame;
   GLViewer myViewer;

   private static class DistanceGrid extends IsRenderableBase {

      double myX;
      double myY;
      int myNx;
      int myNy;

      Vector2d[] myGrid;
      Vector2d[] myNear;
      double[] myDist;

      private Vector2d[] alloc (
         double x, double y, double w, double h, int nx, int ny) {

         double x0 = x-w/2;
         double y0 = y-h/2;
         double dx = w/(nx-1);
         double dy = h/(ny-1);
         Vector2d[] array = new Vector2d[nx*ny];
         for (int i=0; i<nx; i++) {
            for (int j=0; j<ny; j++) {
               array[i*nx+j] = new Vector2d (x0+i*dx, y0+j*dy);
            }
         }
         return array;
      }

      DistanceGrid (
         double x, double y, double w, double h, int nx, int ny) {
         myX = x;
         myY = y;
         myNx = nx;
         myNy = ny;
         myGrid = alloc (x, y, w, h, nx, ny);
         myNear = alloc (x, y, w, h, nx, ny);
         myDist = new double[nx*ny];
      }
      
      public void render (Renderer renderer, int flags) {

         renderer.setShading (Shading.NONE);

         renderer.beginDraw (DrawMode.LINES);        
         for (int i=0; i<myGrid.length; i++) {
            Vector2d g = myGrid[i];
            Vector2d n = myNear[i];
            if (myDist[i] < 0) {
               renderer.setColor (1f, 0f, 1f);
            }
            else {
               renderer.setColor (0f, 1f, 1f);
            }
            if (!g.equals(n)) {
               renderer.addVertex (g.x, g.y, 0);
               renderer.addVertex (n.x, n.y, 0);
            }
         }
         renderer.endDraw();

         renderer.setColor (1f, 1f, 1f);
         renderer.setPointSize (2);
         renderer.beginDraw (DrawMode.POINTS);        
         for (int i=0; i<myGrid.length; i++) {
            Vector2d g = myGrid[i];
            Vector2d n = myNear[i];
            if (g.equals(n)) {
               renderer.addVertex (g.x, g.y, 0);
            }
         }
         renderer.endDraw();
         renderer.setShading (Shading.FLAT);
         renderer.setPointSize (1);
      }

      void update (NURBSCurve2d curve, double maxd) {
         QuadBezierDistance2d dist = new QuadBezierDistance2d(curve);
         for (int i=0; i<myGrid.length; i++) {
            //myDist[i] = dist.computeDistance (myNear[i], myGrid[i], maxd);
            myDist[i] = dist.computeInteriorDistance (myNear[i], myGrid[i]);
         }
      }
   }

   public NURBSCurve2dTest() {
      myFrame = new ViewerFrame("NURBS test", 640, 480);
      myFrame.addMenuBar();
      myFrame.addGridDisplay ();
      myFrame.addViewerToolBar (JToolBar.VERTICAL);
      myFrame.addDraggerToolBar (
         ButtonType.Select, ButtonType.Translate, ButtonType.Draw,
         ButtonType.Spline);
      DraggerToolBar draggerBar = myFrame.getDraggerToolBar();
      draggerBar.setDrawToolListener (this);
      myFrame.addPopupManager();
      myFrame.addKeyListener();
      myFrame.pack();

      myViewer = myFrame.getViewer();

      myViewer.setAxialView (AxisAlignedRotation.X_Y);

      NURBSCurve2d curve = new NURBSCurve2d();
      curve.setCircle (0, 0, 5);

      Polygon2d poly = new Polygon2d();
      poly.addVertex (0, 0);
      poly.addVertex (2, 1);
      poly.addVertex (3, 4);
      poly.addVertex (1, 5);
      poly.addVertex (0, 2);
      poly.addVertex (-2, 3);
      poly.addVertex (-4, 1);
      poly.addVertex (-3, -1);
      RenderProps.setLineColor (poly, Color.RED);      

      //myFrame.addRenderable (curve); 
      //myFrame.addRenderable (poly); 
      myViewer.autoFit();
   }

   public void writeCurve (String fileName, NURBSCurve2d curve) {
      try {
         PrintWriter pw = new IndentingPrintWriter (
            new PrintWriter (new BufferedWriter (new FileWriter (fileName))));
         curve.write (pw);
         pw.close();
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }

   public NURBSCurve2d readCurve (String fileName) {
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new BufferedReader (new FileReader (fileName)));
         NURBSCurve2d curve = new NURBSCurve2d();
         curve.read (rtok);
         return curve;
      }
      catch (Exception e) {
         e.printStackTrace(); 
         return null;
      }
   }

   public void drawToolAdded (DrawToolEvent e) {
   }

   public void drawToolBegin (DrawToolEvent e) {
   }

   public void drawToolEnd (DrawToolEvent e) {
      System.out.println ("Entered");
   }

   public void drawToolRemoved (DrawToolEvent e) {
   }

   public void addBox() {
      PolygonalMesh box = MeshFactory.createBox (1, 1, 1);
      myFrame.addRenderable (box);
   }

   public void addTestCurves() {
      
      Vector4d[] q4 = new Vector4d[] {
         new Vector4d (300, 100, 0, 1),
         new Vector4d (300, 300, 0, 1),
         new Vector4d (100, 300, 0, 1),
         new Vector4d (100, 100, 0, 1),
      };
      double[] k4 = new double[] {
         -2, -1, 0, 1, 2, 3, 4, 5, 6
      };

      Vector4d[] qclosed = new Vector4d[] {
         new Vector4d (150,  50, 0, 1),

         new Vector4d (250,  50, 0, 1),
         new Vector4d (300, 100, 0, 1),
         new Vector4d (350, 150, 0, 1),

         new Vector4d (350, 250, 0, 1),
         new Vector4d (300, 300, 0, 1),
         new Vector4d (250, 350, 0, 1),

         new Vector4d (150, 350, 0, 1),
         new Vector4d (100, 300, 0, 1),
         new Vector4d ( 50, 250, 0, 1),

         new Vector4d ( 50, 150, 0, 1),
         new Vector4d (100, 100, 0, 1),
      };
      double[] kclosed = new double[] {
         0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5
      };

      Vector4d[] qopen = new Vector4d[] {
         new Vector4d (100, 100, 0, 1),
         new Vector4d (150,  50, 0, 1),

         new Vector4d (250,  50, 0, 1),
         new Vector4d (300, 100, 0, 1),
         new Vector4d (350, 150, 0, 1),

         new Vector4d (350, 250, 0, 1),
         new Vector4d (300, 300, 0, 1),
         new Vector4d (250, 350, 0, 1),

         new Vector4d (150, 350, 0, 1),
         new Vector4d (100, 300, 0, 1),
         new Vector4d ( 50, 250, 0, 1),

         new Vector4d ( 50, 150, 0, 1),
         new Vector4d (100, 100, 0, 1),
      };
      double[] kopen = new double[] {
         1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 11,
      };

      Vector4d[] qsimple = new Vector4d[] {
         new Vector4d (100, 100, 0, 1),
         new Vector4d (200, 100, 0, 1), 
         new Vector4d (200, 200, 0, 1), 
         new Vector4d (200, 300, 0, 1), 
         new Vector4d (100, 300, 0, 1), 
         new Vector4d (50, 200, 0, 1), 
      };
      double[] ksimple = new double[] {
         0, 0, 1, 1
      };

      //NURBSCurve2d curve = new NURBSCurve2d (2, NURBSCurve2d.CLOSED, q4, null);
      NURBSCurve2d curve = new NURBSCurve2d (
         2, NURBSCurve2d.CLOSED, qsimple, null);
      curve.convertToBezier();

      writeCurve ("curve.txt", curve);
      NURBSCurve2d curve2 = readCurve ("curve.txt");

      NURBSCurve2d check =
         new NURBSCurve2d (3, NURBSCurve2d.CLOSED, q4, null);
      check.transform (new RigidTransform3d (300, 0, 0));

      DistanceGrid grid = new DistanceGrid (150, 200, 500, 500, 51, 51);
      grid.update (curve2, 50);

      QuadBezierDistance2d dist = new QuadBezierDistance2d(curve2);
      Vector2d pnt = new Vector2d (250, 200);
      Vector2d near = new Vector2d();
      double d = dist.computeDistance (near, pnt, 1e10);

      System.out.println ("max curvature=" + dist.computeMaxCurvature());

      myFrame.addRenderable (curve2);
      myFrame.addRenderable (grid);
   }

   public static void main (String[] args) {

      NURBSCurve2dTest tester = new NURBSCurve2dTest();

      tester.addTestCurves();
      //tester.addBox();

      tester.myFrame.getViewer().autoFit();
      tester.myFrame.setVisible(true);
   }
}

