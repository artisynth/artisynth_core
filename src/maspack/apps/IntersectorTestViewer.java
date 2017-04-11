/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import maspack.collision.ContactInfo;
import maspack.collision.PenetratingPoint;
import maspack.collision.IntersectionContour;
import maspack.collision.IntersectionPoint;
import maspack.collision.*;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyUtils;
import maspack.render.Dragger3dAdapter;
import maspack.render.Dragger3dEvent;
import maspack.render.IsRenderable;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.DrawMode;
import maspack.render.Transrotator3d;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewerFrame;
import maspack.widgets.PropertyDialog;
import maspack.widgets.RenderPropsPanel;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.IntHolder;

public class IntersectorTestViewer extends GLViewerFrame
   implements ActionListener, IsRenderable {
   private static final long serialVersionUID = 1L;

   RenderProps myRenderProps;
   RenderProps myCSGRenderProps;
   SurfaceMeshIntersectorTest myTester;

   private class RenderObjs {
      PolygonalMesh mesh0;
      PolygonalMesh mesh1;
      ContactInfo contactInfo;
      PolygonalMesh imesh;
      PolygonalMesh umesh;
   }

   PolygonalMesh myLastMesh0;
   PolygonalMesh myLastMesh1;

   RenderObjs myRenderObjs;

   boolean myDrawIntersection;
   boolean myDrawUnion;
   boolean mySpinView;

   public IntersectorTestViewer (int w, int h) {
      super ("TwoMeshViewer", w, h);

      myRenderProps = new RenderProps();
      myCSGRenderProps = new RenderProps();

      myRenderProps.setDrawEdges (true);
      myRenderProps.setFaceStyle (FaceStyle.NONE);
      myRenderProps.setShading (Shading.NONE);

      //viewer.setBackgroundColor (0f, 0, 0.2f);
      viewer.setDefaultAxialView (AxisAlignedRotation.X_Y);

      viewer.addMouseInputListener (new MouseInputAdapter() {
         public void mousePressed (MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
               displayPopup (e);
            }
         }
      });

      myTester = new SurfaceMeshIntersectorTest (-1);
      myTester.setSaveTestResults (true);
      myTester.setTestFailFileName ("contactTestFail.out");
      viewer.addRenderable (this);
   }

   private class RenderThread extends Thread {
      public void run() {
         while (!myTestFinished) {
            try {
               Thread.sleep (100);
            }
            catch (Exception e) {
            }
            RenderObjs objs = new RenderObjs();

            boolean resize = false;
            synchronized (myTester) {
               objs.contactInfo = myTester.getLastContactInfo();
               PolygonalMesh mesh0 = myTester.getLastMesh0();
               if (mesh0 != myLastMesh0) {
                  resize = true;
                  myLastMesh0 = mesh0;
               }
               if (mesh0 != null) {
                  mesh0 = new PolygonalMesh (mesh0);
                  mesh0.setMeshToWorld (myTester.getLastMeshToWorld0());
               }
               objs.mesh0 = mesh0;
               PolygonalMesh mesh1 = myTester.getLastMesh1();
               if (mesh1 != myLastMesh1) {
                  resize = true;
                  myLastMesh1 = mesh1;
               }
               if (mesh1 != null) {
                  mesh1 = new PolygonalMesh (mesh1);
                  mesh1.setMeshToWorld (myTester.getLastMeshToWorld1());
               }
               objs.mesh1 = mesh1;
               objs.imesh = myTester.getLastIMesh();
               objs.umesh = myTester.getLastUMesh();
            }
            myRenderObjs = objs;


            if (resize) {
               viewer.autoFitPerspective();
            }
            if (mySpinView) {
               viewer.rotate (Math.toRadians (5), 0);
            }
            viewer.rerender();
         }
      }
   }

   boolean myTestFinished = false;

   private void runTest() {
      RenderThread renderThread = new RenderThread();
      renderThread.start();
      myTester.runtest();
      myTestFinished = true;
   }

   private void locateRight (Window win, Component ref) {
      Window refWin;
      Point compLoc;
      if (ref instanceof Window) {
         refWin = (Window)ref;
         compLoc = new Point();
      }
      else {
         refWin = SwingUtilities.windowForComponent (ref);
         compLoc = SwingUtilities.convertPoint (ref, 0, 0, refWin);
      }
      if (refWin == null) {
         return;
      }
      Point refLoc = refWin.getLocation();
      Dimension refSize = refWin.getSize();

      Point newLoc = new Point (refLoc.x + refSize.width, refLoc.y + compLoc.y);
      win.setLocation (newLoc);
   }

   public void prerender (RenderList list) {

      RenderObjs objs = myRenderObjs;
      if (objs == null) {
         return;
      }
      if (objs.mesh0 != null) {
         objs.mesh0.prerender (myRenderProps);
      }
      if (objs.mesh1 != null) {
         objs.mesh1.prerender (myRenderProps);
      } 
      if (myDrawIntersection && objs.imesh != null) {
         objs.imesh.prerender (myCSGRenderProps);
      }
      if (myDrawUnion && objs.umesh != null) {
         objs.umesh.prerender (myCSGRenderProps);
      }
  }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      RenderObjs objs = myRenderObjs;
      if (objs != null) {
         if (objs.mesh0 != null) {
            objs.mesh0.updateBounds (pmin, pmax);
         }
         if (objs.mesh1 != null) {
            objs.mesh1.updateBounds (pmin, pmax);
         }
      }
   }

   public int getRenderHints() {
      return 0;
   }

   private int contourWidth = 2;
   private float[] contourColor = new float [] { 1f, 1f, 0 };

   public void render (Renderer renderer, int flags) {

      RenderObjs objs = myRenderObjs;
      if (objs == null) {
         return;
      }
      
      if (objs.mesh0 != null) {
         objs.mesh0.render (renderer, myRenderProps, flags);
      }
      if (objs.mesh1 != null) {
         objs.mesh1.render (renderer, myRenderProps, flags);
      }
      if (myDrawIntersection && objs.imesh != null) {
         objs.imesh.render (renderer, myCSGRenderProps, flags);
      }
      if (myDrawUnion && objs.umesh != null) {
         objs.umesh.render (renderer, myCSGRenderProps, flags);
      }

      ArrayList<IntersectionContour> contours = null;
      if (objs.contactInfo != null) {
         contours = objs.contactInfo.getContours();
      }
      
      if (contours != null) {
         renderer.setLineWidth (3);
         renderer.setColor (Color.RED);
         renderer.setShading (Shading.NONE);
         for (IntersectionContour c : contours) {
            if (c.isClosed()) {
               renderer.beginDraw (DrawMode.LINE_LOOP);
            }
            else {
               renderer.beginDraw (DrawMode.LINE_STRIP);
            }
            for (IntersectionPoint p : c) {
               renderer.addVertex ((float)p.x, (float)p.y, (float)p.z);
            }
            renderer.endDraw();
         }           
         renderer.setShading (Shading.FLAT);
         renderer.setLineWidth (1);
      }
   }   

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();

      if (cmd.equals ("Enable intersection rendering")) {
         myDrawIntersection = true;
      }
      else if (cmd.equals ("Disable intersection rendering")) {
         myDrawIntersection = false;
      }
      else if (cmd.equals ("Enable union rendering")) {
         myDrawUnion = true;
      }
      else if (cmd.equals ("Disable union rendering")) {
         myDrawUnion = false;
      }
   }

   private JMenuItem createMenuItem (String cmd) {
      JMenuItem item = new JMenuItem (cmd);
      item.addActionListener (this);
      item.setActionCommand (cmd);
      return item;
   }

   private void displayPopup (MouseEvent evt) {
      JPopupMenu popup = new JPopupMenu();
      if (myDrawIntersection) {
         popup.add (createMenuItem ("Disable intersection rendering"));
      }
      else {
         popup.add (createMenuItem ("Enable intersection rendering"));
      }
      if (myDrawUnion) {
         popup.add (createMenuItem ("Disable union rendering"));
      }
      else {
         popup.add (createMenuItem ("Enable union rendering"));
      }
      popup.setLightWeightPopupEnabled (false);
      popup.show (evt.getComponent(), evt.getX(), evt.getY());
   }

   static BooleanHolder drawIntersection = new BooleanHolder (false);
   static BooleanHolder drawUnion = new BooleanHolder (false);
   static BooleanHolder spinView = new BooleanHolder (false);

   public static void main (String[] args) {
      IntHolder width = new IntHolder (640);
      IntHolder height = new IntHolder (480);

      ArgParser parser =
         new ArgParser (
            "java maspack.geometry.TwoMeshViewer [options] meshFile1 meshFile2");
      parser.addOption ("-width %d #width (pixels)", width);
      parser.addOption ("-height %d #height (pixels)", height);
      parser.addOption (
         "-drawIntersection %v #draw mesh intersection", drawIntersection);
      parser.addOption (
         "-drawUnion %v #draw mesh union", drawUnion);
      parser.addOption (
         "-spinView %v #automatically rotate view", spinView);

      String[] otherArgs = parser.matchAllArgs (args, 0, 0);

      IntersectorTestViewer frame =
         new IntersectorTestViewer (width.value, height.value);
      frame.myDrawIntersection = drawIntersection.value;
      frame.myDrawUnion = drawUnion.value;
      frame.mySpinView = spinView.value;
      frame.setVisible (true);
      frame.runTest();
   }
}
