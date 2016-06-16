/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.MouseInputAdapter;

import argparser.ArgParser;
import argparser.BooleanHolder;
import argparser.DoubleHolder;
import argparser.IntHolder;
import argparser.StringHolder;
import maspack.geometry.MeshFactory;
import maspack.geometry.NURBSCurve3d;
import maspack.geometry.NURBSMesh;
import maspack.geometry.NURBSObject;
import maspack.geometry.NURBSSurface;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.io.WavefrontReader;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.render.RenderProps;
import maspack.render.ViewerSelectionEvent;
import maspack.render.ViewerSelectionListener;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewer.GLVersion;
import maspack.render.GL.GLViewerFrame;

public class NURBSViewer extends GLViewerFrame {
   private static final long serialVersionUID = 1L;
   ArrayList<NURBSObject> nurbsList = new ArrayList<NURBSObject> (10);
   Hashtable<NURBSObject,NURBSMesh> meshTable =
      new Hashtable<NURBSObject,NURBSMesh> (10);

   ArrayList<Vector4d> selectedPnts = new ArrayList<Vector4d>();

   class SelectionHandler implements ViewerSelectionListener {
      private void clearSelection() {
         selectedPnts.clear();
         for (Iterator<NURBSObject> it = nurbsList.iterator(); it.hasNext();) {
            NURBSObject nurbsObj = (NURBSObject)it.next();
            for (int i = 0; i < nurbsObj.numControlPoints(); i++) {
               nurbsObj.selectControlPoint (i, false);
            }
         }

      }

      public void itemsSelected (ViewerSelectionEvent e) {
         boolean holdSelection, selectAll;

         long modEx = e.getModifiersEx();
         holdSelection = ((modEx & MouseEvent.SHIFT_DOWN_MASK) != 0);
         selectAll = ((modEx & MouseEvent.ALT_DOWN_MASK) != 0);

         if (!holdSelection) {
            clearSelection();
         }
         if (e.numSelectedQueries() > 0) {
            List<LinkedList<?>> itemPaths = e.getSelectedObjects();
            for (LinkedList<?> path : itemPaths) {
               if (path.getFirst() instanceof NURBSObject) {
                  NURBSObject nurbsObj = (NURBSObject)path.getFirst();
                  if (path.size() > 1 && path.get (1) instanceof Integer) {
                     int idx = ((Integer)path.get (1)).intValue();
                     if (!nurbsObj.controlPointIsSelected (idx)) {
                        nurbsObj.selectControlPoint (idx, true);
                        selectedPnts.add (nurbsObj.getControlPoints()[idx]);
                     }
                     else {
                        nurbsObj.selectControlPoint (idx, false);
                        selectedPnts.remove (nurbsObj.getControlPoints()[idx]);
                     }
                     if (!selectAll) {
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   class MouseHandler extends MouseInputAdapter {
      boolean dragging = false;
      int lastX;
      int lastY;

      public void mousePressed (MouseEvent e) {
         // check for selection
         int modEx = e.getModifiersEx();
         if ((modEx & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            if (selectedPnts.size() > 0) {
               dragging = true;
               lastX = e.getX();
               lastY = e.getY();
            }
         }
      }

      public void mouseReleased (MouseEvent e) {
         dragging = false;
      }

      public void mouseDragged (MouseEvent e) {
         if (dragging) {
            RigidTransform3d XV = new RigidTransform3d();
            viewer.getViewMatrix (XV);
            Vector3d del =
               new Vector3d (e.getX() - lastX, lastY - e.getY(), 0);
            del.inverseTransform (XV);
            del.scale (viewer.centerDistancePerPixel());
            Vector4d del4d = new Vector4d (del.x, del.y, del.z, 0);
            for (Iterator<Vector4d> it = selectedPnts.iterator(); it.hasNext();) {
               ((Vector4d)it.next()).add (del4d);
            }
            for (Enumeration<NURBSObject> en = meshTable.keys(); en.hasMoreElements();) {
               NURBSSurface surf = (NURBSSurface)en.nextElement();
               NURBSMesh mesh = (NURBSMesh)meshTable.get (surf);
               mesh.updateVertices (surf);
               collide (mesh);
            }
            lastX = e.getX();
            lastY = e.getY();
            viewer.getCanvas().repaint();
         }
      }
   }

   public void addNURBS (File file) throws IOException {
      WavefrontReader wfr = new WavefrontReader(file);
      wfr.parse ();

      Vector4d[] allControlPnts = wfr.getHomogeneousPoints();
      for (WavefrontReader.Curve curve : wfr.getCurveList()) {
         NURBSCurve3d curveCopy = new NURBSCurve3d();
         try {
            curveCopy.set (curve, allControlPnts);
         }
         catch (IllegalArgumentException e) {
            throw new IOException (e.getMessage());
         }
         addNURBS (curveCopy);
      }
      for (WavefrontReader.Surface surf : wfr.getSurfaceList()) {
         NURBSSurface surfCopy = new NURBSSurface();
         try {
            surfCopy.set (surf, allControlPnts);
         }
         catch (IllegalArgumentException e) {
            throw new IOException (e.getMessage());
         }
         addNURBS (surfCopy);
      }
   }

   public NURBSViewer (int w, int h) {
      super ("NURBSViewer", w, h);
      init();
   }

   public NURBSViewer (int w, int h, GLVersion version) {
      super ("NURBSViewer", w, h, version);
      init();
   }

   private void init() {
      MouseHandler mouseHandler = new MouseHandler();
      viewer.getCanvas().addMouseListener (mouseHandler);
      viewer.getCanvas().addMouseMotionListener (mouseHandler);
      viewer.addSelectionListener (new SelectionHandler());
   }

   public void addNURBS (NURBSObject nurbs) {
      viewer.addRenderable (nurbs);
      nurbsList.add (nurbs);
   }

   public void addNURBSWithMesh (NURBSObject nurbs) {
      addNURBS (nurbs);
      if (nurbs instanceof NURBSSurface) {
         NURBSMesh mesh = new NURBSMesh();
         mesh.set ((NURBSSurface)nurbs, /* triangular= */true);
         // mesh.setRenderEdges (true);
         Color gold = new Color (0.93f, 0.8f, 0.063f);
         RenderProps.setFaceColor (mesh, gold);
         meshTable.put (nurbs, mesh);
         System.out.println ("mesh.numVerts=" + mesh.numVertices());
         viewer.addRenderable (mesh);
      }

   }

   // special stuff for demonstrating collisions
   PolygonalMesh colmesh;
   Point3d colCenter = new Point3d (15, 15, 0);
   Vector3d diff = new Vector3d();
   double colRad = 10;

   void collidePoint (Point3d pnt) {
      diff.sub (pnt, colCenter);
      double magSqr = diff.normSquared();
      if (magSqr < colRad * colRad) {
         double mag = Math.sqrt (magSqr);
         diff.scale (1 / mag);
         pnt.scaledAdd (colRad - mag, diff, pnt);
      }
   }

   void collide (PolygonalMesh mesh) {
      if (colmesh != null) {
         for (Iterator<Vertex3d> it = mesh.getVertices().iterator(); it.hasNext();) {
            collidePoint (it.next().pnt);
         }
      }
   }

   void addCollidable() {
      colmesh =
         MeshFactory.createQuadSphere (
            colRad, 32, colCenter.x, colCenter.y, colCenter.z);
      RenderProps props = colmesh.getRenderProps();
      props.setFaceColor (new Color (0.5f, 0.5f, 0.5f));
      props.setAlpha (0.5);
      viewer.addRenderable (colmesh);
   }

   static BooleanHolder drawAxes = new BooleanHolder (false);
   static DoubleHolder axisLength = new DoubleHolder (-1);
   static BooleanHolder addSphere = new BooleanHolder (false);
   static BooleanHolder addCircle = new BooleanHolder (false);
   static BooleanHolder addMesh = new BooleanHolder (false);
   static BooleanHolder collider = new BooleanHolder (false);
   static IntHolder glVersion = new IntHolder (2);

   public static void main (String[] args) {
      StringHolder fileName = new StringHolder();
      IntHolder width = new IntHolder (400);
      IntHolder height = new IntHolder (400);

      ArgParser parser = new ArgParser ("java maspack.geometry.NURBSViewer");
      parser.addOption ("-width %d #width (pixels)", width);
      parser.addOption ("-height %d #height (pixels)", height);
      parser.addOption ("-drawAxes %v #draw coordinate axes", drawAxes);
      parser.addOption ("-file %s #wavefront file name", fileName);
      parser.addOption ("-sphere %v #create a NURBS sphere", addSphere);
      parser.addOption ("-circle %v #create a NURBS circle", addCircle);
      parser.addOption ("-mesh %v #create a mesh for surfaces", addMesh);
      parser.addOption (
         "-collider %v #create a colliding object for meshes", collider);
      parser.addOption ("-axisLength %f #coordinate axis length", axisLength);
      parser.addOption (
         "-GLVersion %d{2,3} " + "#version of openGL for graphics", glVersion);

      parser.matchAllArgs (args);

      NURBSViewer viewFrame = null;
      try {
         GLVersion glv = (glVersion.value == 3 ? GLVersion.GL3 : GLVersion.GL2);
         viewFrame = new NURBSViewer (width.value, height.value, glv);
         GLViewer viewer = viewFrame.getViewer();
         if (fileName.value != null) {
            viewFrame.addNURBS (new File (fileName.value));
         }
         if (addSphere.value) {
            NURBSSurface sphere = new NURBSSurface();
            sphere.setSphere (0, 0, 0, 10);
            if (addMesh.value) {
               viewFrame.addNURBSWithMesh (sphere);
            }
            else {
               viewFrame.addNURBS (sphere);
            }
         }
         if (addCircle.value) {
            NURBSCurve3d circle = new NURBSCurve3d();
            circle.setCircle (0, 0, 10);
            viewFrame.addNURBS (circle);
         }

         viewer.autoFitPerspective ();
         if (drawAxes.value) {
            if (axisLength.value > 0) {
               viewer.setAxisLength (axisLength.value);
            }
            else {
               viewer.setAxisLength (GLViewer.AUTO_FIT);
            }
         }
         if (collider.value) {
            viewFrame.addCollidable();
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }

      viewFrame.setVisible (true);
   }
}
