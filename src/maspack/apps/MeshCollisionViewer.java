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

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import maspack.collision.ContactInfo;
import maspack.collision.PenetratingPoint;
import maspack.collision.IntersectionContour;
import maspack.collision.IntersectionPoint;
import maspack.collision.SurfaceMeshCollider;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
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

public class MeshCollisionViewer extends GLViewerFrame
   implements ActionListener, IsRenderable {
   private static final long serialVersionUID = 1L;

   PolygonalMesh myMesh1;
   PolygonalMesh myMesh2;
   RenderProps myRenderProps;

   Transrotator3d myDragger1;
   Transrotator3d myDragger2;

   SurfaceMeshCollider myCollider;
   ContactInfo myContactInfo;

   private class DragHandler extends Dragger3dAdapter {
      PolygonalMesh myMesh;

      DragHandler (PolygonalMesh mesh) {
         myMesh = mesh;
      }

      public void draggerMove (Dragger3dEvent e) {
         RigidTransform3d Xinc = (RigidTransform3d)e.getIncrementalTransform();
         RigidTransform3d X = new RigidTransform3d();
         myMesh.getMeshToWorld (X);
         X.mul (Xinc);
         myMesh.setMeshToWorld (X);
         myContactInfo = myCollider.getContacts (myMesh1, myMesh2);
      }
   }

   private RenderProps createRenderProps (PolygonalMesh mesh) {
      RenderProps props = mesh.createRenderProps();

      props.setShading (smooth.value ? Shading.SMOOTH : Shading.FLAT);
      if (noDrawFaces.value) {
         props.setFaceStyle (FaceStyle.NONE);
      }
      else if (oneSided.value) {
         props.setFaceStyle (FaceStyle.FRONT);
      }
      else {
         props.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      }
      props.setDrawEdges (drawEdges.value);
      if (edgeColor[0] != -1) {
         props.setLineColor (new Color (
            edgeColor[0], edgeColor[1], edgeColor[2]));
      }
      Color gold = new Color (0.93f, 0.8f, 0.063f);
      Color gray = new Color (0.5f, 0.5f, 0.5f);

      props.setFaceColor (gray);
      props.setAlpha (0.5);
      //props.setBackColor (gray);
      return props;
   }

   public MeshCollisionViewer (PolygonalMesh mesh1, PolygonalMesh mesh2, int w, int h) {
      super ("TwoMeshViewer", w, h);

      myMesh1 = mesh1;
      myMesh2 = mesh2;

      myRenderProps = createRenderProps (myMesh1);

      myMesh1.triangulate();
      myMesh1.setRenderProps (myRenderProps);
      viewer.addRenderable (myMesh1);
      double rad1 = RenderableUtils.getRadius (myMesh1);
      myDragger1 = new Transrotator3d (viewer, 1.5*rad1);
      myDragger1.addListener (new DragHandler (myMesh1));
      viewer.addDragger (myDragger1);

      myMesh2.triangulate();
      myMesh2.setRenderProps (myRenderProps);
      viewer.addRenderable (myMesh2);
      double rad2 = RenderableUtils.getRadius (myMesh2);
      myDragger2 = new Transrotator3d (viewer, 1.5*rad2);
      myDragger2.addListener (new DragHandler (myMesh2));
      viewer.addDragger (myDragger2);

      RigidTransform3d X;

      X = new RigidTransform3d (-(rad1+rad2)/2, 0, 0);
      myDragger1.setDraggerToWorld (X);
      myMesh1.setMeshToWorld (X);

      X = new RigidTransform3d ( (rad1+rad2)/2, 0, 0);
      myDragger2.setDraggerToWorld (X);
      myMesh2.setMeshToWorld (X);

      viewer.autoFitPerspective ();
      viewer.setBackgroundColor (0f, 0, 0.2f);

      if (drawAxes.value) {
         if (axisLength.value > 0) {
            viewer.setAxisLength (axisLength.value);
         }
         else {
            viewer.setAxisLength (GLViewer.AUTO_FIT);
         }
      }

      viewer.addMouseInputListener (new MouseInputAdapter() {
         public void mousePressed (MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
               displayPopup (e);
            }
         }
      });

      viewer.addRenderable (this);

      SurfaceMeshCollider.setAjlCollision(true);
      myCollider = new SurfaceMeshCollider ();
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

   private void createRenderPropsPanel (PolygonalMesh mesh) {
      
      PropertyDialog dialog =
         new PropertyDialog (
            "Edit render props",
            new RenderPropsPanel (
               PropertyUtils.createProperties (mesh.getRenderProps())),
            "OK Cancel");
      dialog.locateRight (this);
      dialog.addGlobalValueChangeListener (
         new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               viewer.rerender();
            }
         }); 
      dialog.setVisible (true);
   }

   public void prerender (RenderList list) {
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public int getRenderHints() {
      return 0;
   }

   private int contourWidth = 2;
   private float[] contourColor = new float [] { 1f, 1f, 0 };

   public void render (Renderer renderer, int flags) {

      ContactInfo cinfo = myContactInfo;
      
      if (contourWidth > 0 && cinfo != null) {
         renderer.setShading (Shading.NONE);
         renderer.setLineWidth (contourWidth);
         renderer.setPointSize (contourWidth);
         renderer.setColor (contourColor, /*highlight=*/false);

         if (cinfo.getContours() != null) {
            for (IntersectionContour contour : cinfo.getContours()) {
               renderer.beginDraw (DrawMode.LINE_LOOP);
               for (IntersectionPoint p : contour) {
                  renderer.addVertex (p);
               }
               renderer.endDraw();
            }
         }
         Point3d pnt = new Point3d();
         renderer.beginDraw (DrawMode.POINTS);
         for (PenetratingPoint p : cinfo.getPenetratingPoints(0)) {
            pnt.set (p.vertex.pnt);
            pnt.transform (myMesh1.getMeshToWorld());
            renderer.addVertex (pnt);
         }
         for (PenetratingPoint p : cinfo.getPenetratingPoints(1)) {
            pnt.set (p.vertex.pnt);
            pnt.transform (myMesh2.getMeshToWorld());
            renderer.addVertex (pnt);
         }
         renderer.endDraw();

         renderer.setShading (Shading.FLAT);
         renderer.setLineWidth (1);
         renderer.setPointSize (1);
      }
   }   

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();

      if (cmd.equals ("Hide dragger1")) {
         myDragger1.setVisible (false);
      }
      else if (cmd.equals ("Show dragger1")) {
         myDragger1.setVisible (true);
      }
      else if (cmd.equals ("Hide dragger2")) {
         myDragger2.setVisible (false);
      }
      else if (cmd.equals ("Show dragger2")) {
         myDragger2.setVisible (true);
      }
      else if (cmd.equals ("Edit renderProps1")) {
         createRenderPropsPanel (myMesh1);
      }
      else if (cmd.equals ("Edit renderProps2")) {
         createRenderPropsPanel (myMesh2);
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
      if (myDragger1.isVisible()) {
         popup.add (createMenuItem ("Hide dragger1"));
      }
      else {
         popup.add (createMenuItem ("Show dragger1"));
      }
      if (myDragger2.isVisible()) {
         popup.add (createMenuItem ("Hide dragger2"));
      }
      else {
         popup.add (createMenuItem ("Show dragger2"));
      }
      popup.add (createMenuItem ("Edit renderProps1"));
      popup.add (createMenuItem ("Edit renderProps2"));
      popup.setLightWeightPopupEnabled (false);
      popup.show (evt.getComponent(), evt.getX(), evt.getY());
   }

   static BooleanHolder drawAxes = new BooleanHolder (false);
   static DoubleHolder axisLength = new DoubleHolder (-1);
   static BooleanHolder drawEdges = new BooleanHolder (false);
   static BooleanHolder noDrawFaces = new BooleanHolder (false);
   static float[] edgeColor = new float[] { -1, -1, -1 };
   static BooleanHolder smooth = new BooleanHolder (false);
   static BooleanHolder oneSided = new BooleanHolder (false);

   public static void main (String[] args) {
      IntHolder width = new IntHolder (640);
      IntHolder height = new IntHolder (480);

      ArgParser parser =
         new ArgParser (
            "java maspack.geometry.TwoMeshViewer [options] meshFile1 meshFile2");
      parser.addOption ("-width %d #width (pixels)", width);
      parser.addOption ("-height %d #height (pixels)", height);
      parser.addOption ("-drawAxes %v #draw coordinate axes", drawAxes);
      parser.addOption ("-drawEdges %v #draw mesh edges", drawEdges);
      parser.addOption ("-noDrawFaces %v #do not draw faces", noDrawFaces);
      parser.addOption ("-edgeColor %fX3 #edge color", edgeColor);
      parser.addOption ("-axisLength %f #coordinate axis length", axisLength);
      parser.addOption ("-smooth %v #use smooth shading", smooth);
      parser.addOption ("-oneSided %v #draw only front faces", oneSided);

      String[] otherArgs = parser.matchAllArgs (args, 0, 0);

      if (otherArgs == null || otherArgs.length != 2) {
         System.out.println ("Usage: " + parser.getSynopsisString());
         System.out.println ("Use -help for more info");
         System.exit(1);
      }
      PolygonalMesh mesh1 = null;
      PolygonalMesh mesh2 = null;

      try {
         mesh1 = new PolygonalMesh (new File (otherArgs[0]));
         mesh2 = new PolygonalMesh (new File (otherArgs[1]));
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }

      MeshCollisionViewer frame =
         new MeshCollisionViewer (mesh1, mesh2, width.value, height.value);
      frame.setVisible (true);
   }
}
