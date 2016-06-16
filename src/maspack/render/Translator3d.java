/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.LinkedList;

import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.DrawMode;
import maspack.render.GL.GLGridPlane;
import maspack.render.GL.GLViewer;
import maspack.util.InternalErrorException;

public class Translator3d extends Dragger3dBase {

   private static final double EPS = 1e-13;

   protected RigidTransform3d myTransform;
   protected RigidTransform3d myIncrementalTransform;
   protected int myCircleRes = 20;
   protected int mySelectedComponent = NONE;
   protected Point3d myPnt0 = new Point3d();

   protected static final double myPlaneBoxRelativeSize = 0.4;

   static final int NONE = 0;

   static final int X_AXIS = 1;
   static final int Y_AXIS = 2;
   static final int Z_AXIS = 3;

   static final int ZX_PLANE = 4;
   static final int YZ_PLANE = 5;
   static final int XY_PLANE = 6;
   
   private static final Line xAxis = new Line (0, 0, 0, 1, 0, 0);
   private static final Line yAxis = new Line (0, 0, 0, 0, 1, 0);
   private static final Line zAxis = new Line (0, 0, 0, 0, 0, 1);

   private static final Plane xyPlane = new Plane (0, 0, 1, 0);
   private static final Plane yzPlane = new Plane (1, 0, 0, 0);
   private static final Plane zxPlane = new Plane (0, 1, 0, 0);
   
   private static RenderObject renderObject = null;

   public Translator3d() {
      super();
      myTransform = new RigidTransform3d();
      myIncrementalTransform = new RigidTransform3d();
   }

   public Translator3d (GLViewer viewer, double size) {
      this();
      setSize (size);
   }

   public void render (Renderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }
      
      Shading savedShading = renderer.setShading (Shading.NONE);
      renderer.setLineWidth(myLineWidth);
      
      renderer.pushModelMatrix();
      renderer.mulModelMatrix(myXDraggerToWorld);
      
      float[] coords = new float[3];
      if (myDragMode != DragMode.OFF && mySelectedComponent != NONE) { 
         renderer.setColor(1.0f, 1.0f, 0f);
         renderer.setPointSize(3);
         myPnt0.get(coords);
         renderer.drawPoint(coords);
         renderer.setPointSize(1);
      }
      
      renderer.scaleModelMatrix(mySize);

      if (renderObject == null) {
         renderObject = createTranslatorRenderable();
      }
      
      // draw selected component first
      if (mySelectedComponent != 0) {
         renderer.drawLines(renderObject, mySelectedComponent);
      }
      renderer.drawLines(renderObject, 0);
      
      renderer.popModelMatrix();
      
      renderer.setLineWidth(1);
      renderer.setShading (savedShading);
   }
   
   private static void addLineStrip (RenderObject robj, int pidx0, int numv) {
      robj.beginBuild (DrawMode.LINE_STRIP);
      for (int i=0; i<numv; i++) {
         robj.addVertex (pidx0+i);
      }
      robj.endBuild();
   }

   private static void addLine (RenderObject robj, int pidx0, int pidx1) {
      int vidx0 = robj.addVertex (pidx0);
      int vidx1 = robj.addVertex (pidx1);
      robj.addLine (vidx0, vidx1);
   }

   private static RenderObject createTranslatorRenderable() {
      
      final float TRANS_BOX_SIZE = 0.4f;
      
      RenderObject robj = new RenderObject();
      
      int RED    = robj.addColor (1.0f, 0.0f, 0.0f, 1.0f);
      int GREEN  = robj.addColor (0.0f, 1.0f, 0.0f, 1.0f);
      int BLUE   = robj.addColor (0.0f, 0.0f, 1.0f, 1.0f);
      int GRAY   = robj.addColor (0.5f, 0.5f, 0.5f, 1.0f);
      int YELLOW = robj.addColor (1.0f, 1.0f, 0.0f, 1.0f);
      
      int p0     = robj.addPosition (0.0f, 0.0f, 0.0f);
      int px     = robj.addPosition (1.0f, 0.0f, 0.0f);
      int py     = robj.addPosition (0.0f, 1.0f, 0.0f);
      int pz     = robj.addPosition (0.0f, 0.0f, 1.0f);

      float size = TRANS_BOX_SIZE;
      
      robj.addPosition (0.0f, size, 0.0f);
      robj.addPosition (0.0f, size, size);
      robj.addPosition (0.0f, 0.0f, size);
      robj.addPosition (size, 0.0f, size);
      robj.addPosition (size, 0.0f, 0.0f);
      robj.addPosition (size, size, 0.0f);
      robj.addPosition (0.0f, size, 0.0f);

      int pbox = pz+1;      

      for (int i=0; i<7; i++) {
         robj.createLineGroup();
      }

      robj.lineGroup (0);

      robj.setCurrentColor (RED);
      addLine (robj, p0, px);
      robj.setCurrentColor (GREEN);
      addLine (robj, p0, py);
      robj.setCurrentColor (BLUE);
      addLine (robj, p0, pz);

      robj.setCurrentColor (GRAY);
      addLineStrip (robj, pbox, 7);

      robj.setCurrentColor (YELLOW);
      
      robj.lineGroup (X_AXIS);
      addLine (robj, p0, px);
      robj.lineGroup (Y_AXIS);
      addLine (robj, p0, py);
      robj.lineGroup (Z_AXIS);
      addLine (robj, p0, pz);

      robj.lineGroup (YZ_PLANE);
      addLineStrip (robj, pbox, 3);
      robj.lineGroup (ZX_PLANE);
      addLineStrip (robj, pbox+2, 3);
      robj.lineGroup (XY_PLANE);
      addLineStrip (robj, pbox+4, 3);
   
      return robj;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   private int checkComponentSelection (MouseRayEvent e) {

      Line draggerRay = new Line (e.getRay());
      draggerRay.inverseTransform (myXDraggerToWorld);

      // double distancePerPixel = e.distancePerPixel (myXDraggerToWorld.p);
      // double lineDist = 5*distancePerPixel; // was 0.05*mySize

      double lineDist = 5 * e.distancePerPixel (myXDraggerToWorld.p);
      double minDist = Double.POSITIVE_INFINITY;
      double l, d, tempDist;
      int resultAxisOrPlane = NONE;

      RigidTransform3d draggerToEye = new RigidTransform3d();
      draggerToEye.mul (e.getViewer().getViewMatrix(), myXDraggerToWorld);

      // Line resultAxis = new Line (0, 0, 0, 0, 0, 0);

      Point3d p = new Point3d();

      // check axes first

      l = xAxis.nearestPoint (p, draggerRay);
      tempDist = draggerRay.distance (p);
      if (l >= 0 && l <= mySize && tempDist < lineDist) {
         resultAxisOrPlane = X_AXIS;
         minDist = tempDist;
      }

      l = yAxis.nearestPoint (p, draggerRay);
      tempDist = draggerRay.distance (p);
      if (l >= 0 && l <= mySize && tempDist < lineDist && tempDist < minDist) {
         resultAxisOrPlane = Y_AXIS;
         minDist = tempDist;
      }

      l = zAxis.nearestPoint (p, draggerRay);
      tempDist = draggerRay.distance (p);
      if (l >= 0 && l <= mySize && tempDist < lineDist && tempDist < minDist) {
         resultAxisOrPlane = Z_AXIS;
         minDist = tempDist;
      }

      if (resultAxisOrPlane != NONE) {
         return resultAxisOrPlane;
      }

      // now check if the mouse is on any of the planes. If true,
      // then return the selected plane closest to the user.
      double len = myPlaneBoxRelativeSize * mySize;
      minDist = inf;

      d = draggerRay.intersectPlane (p, xyPlane);
      if (d != inf && p.x >= 0 && p.x <= len && p.y >= 0 && p.y <= len) {
         p.transform (draggerToEye);
         resultAxisOrPlane = XY_PLANE;
         minDist = p.z;
      }

      d = draggerRay.intersectPlane (p, yzPlane);
      if (d != inf && p.y >= 0 && p.y <= len && p.z >= 0 && p.z <= len) {
         p.transform (draggerToEye);
         if (p.z < minDist) {
            resultAxisOrPlane = YZ_PLANE;
            minDist = p.z;
         }
      }

      d = draggerRay.intersectPlane (p, zxPlane);
      if (d != inf && p.z >= 0 && p.z <= len && p.x >= 0 && p.x <= len) {
         p.transform (draggerToEye);
         if (p.z < minDist) {
            resultAxisOrPlane = ZX_PLANE;
         }
      }

      return resultAxisOrPlane;
   }

   private void intersectRayAndFixture (Point3d p, Line ray) {
      Line draggerRay = new Line (ray);
      draggerRay.inverseTransform (myXDraggerToWorld);

      switch (mySelectedComponent) {
         case X_AXIS: {
            double l = xAxis.nearestPoint (p, draggerRay);
            p.y = p.z = 0;
            break;
         }
         case Y_AXIS: {
            yAxis.nearestPoint (p, draggerRay);
            p.x = p.z = 0;
            break;
         }
         case Z_AXIS: {
            zAxis.nearestPoint (p, draggerRay);
            p.x = p.y = 0;
            break;
         }
         case XY_PLANE: {
            draggerRay.intersectPlane (p, xyPlane);
            p.z = 0;
            break;
         }
         case YZ_PLANE: {
            draggerRay.intersectPlane (p, yzPlane);
            p.x = 0;
            break;
         }
         case ZX_PLANE: {
            draggerRay.intersectPlane (p, zxPlane);
            p.y = 0;
            break;
         }
         default: {
            throw new InternalErrorException (
               "unexpected case " + mySelectedComponent);
         }
      }
   }

   /**
    * Ensure that a given displacement is constrained (in dragger coordinates)
    * to the currently selected fixture.
    */
   private void constrainToFixture (Vector3d v) {

      switch (mySelectedComponent) {
         case X_AXIS: {
            v.y = v.z = 0;
            break;
         }
         case Y_AXIS: {
            v.x = v.z = 0;
            break;
         }
         case Z_AXIS: {
            v.x = v.y = 0;
            break;
         }
         case XY_PLANE: {
            v.z = 0;
            break;
         }
         case YZ_PLANE: {
            v.x = 0;
            break;
         }
         case ZX_PLANE: {
            v.y = 0;
            break;
         }
         default: {
            throw new InternalErrorException (
               "unexpected case " + mySelectedComponent);
         }
      }
   }

   private void updatePosition (Point3d p0, Point3d p1, MouseRayEvent e) {
      Vector3d del = new Vector3d();
      del.sub (p1, p0);
      // if (dragIsConstrained(e)) {
      if (dragIsConstrained()) {
         Renderer renderer = e.getViewer();
         boolean constrainedToGrid = false;
         
         if (renderer instanceof GLViewer) {
            GLViewer viewer = (GLViewer)renderer;
            if (viewer.getGridVisible()) {
               GLGridPlane grid = viewer.getGrid();
               RigidTransform3d XDraggerToGrid = new RigidTransform3d();
               XDraggerToGrid.mulInverseLeft (
                  grid.getGridToWorld(), myXDraggerToWorld);
               if (XDraggerToGrid.R.isAxisAligned (EPS)) {
                  // if the dragger orientation is axis-aligned with grid
                  // coordinates, then adjust constrained motions so that the
                  // dragger center always lies on a grid point. (Otherwise, this
                  // can't necessarily be done without causing the dragger to move
                  // off the specified line or plane of translation).
                  Point3d pa = new Point3d(del);
                  pa.transform (XDraggerToGrid);
                  grid.alignPoint (pa, pa);
                  pa.inverseTransform (XDraggerToGrid);
                  del.set (pa);
                  constrainToFixture (del);
                  constrainedToGrid = true;
               }
            }
         }
         
         if (!constrainedToGrid) {
            // if not possible to constrain to grid, just constrain the step
            // size
            double s = getConstrainedStepSize();
            del.x = s*Math.round(del.x/s);
            del.y = s*Math.round(del.y/s);
            del.z = s*Math.round(del.z/s);
         }
      }
      myXDraggerToWorld.mulXyz (del.x, del.y, del.z);
      myIncrementalTransform.p.set (del.x, del.y, del.z);
      myTransform.mul (myIncrementalTransform);
   }

//   public void draggerSelected (MouseRayEvent e) {
//      intersectRayAndFixture (myPnt0, e.getRay());
//   }

   public boolean mousePressed (MouseRayEvent e) {
      // DragMode mode = getDragMode (e);
      DragMode mode = getDragMode ();
      if (mode != DragMode.OFF && mySelectedComponent != NONE) {
         myDragMode = mode;
         intersectRayAndFixture (myPnt0, e.getRay());
         myTransform.setIdentity();
         myIncrementalTransform.setIdentity();
         fireDraggerBeginListeners (
            myTransform, myIncrementalTransform, e.getModifiersEx());
         return true;
      }
      return false;
   }

   public boolean mouseReleased (MouseRayEvent e) {
      if (myDragMode != DragMode.OFF) {
         myIncrementalTransform.setIdentity();
         fireDraggerEndListeners (
            myTransform, myIncrementalTransform, e.getModifiersEx());
         myDragMode = DragMode.OFF;
         clearFlags();
         return true;
      }
      return false;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      if (mySelectedComponent != NONE) {
         Point3d pnt = new Point3d();
         intersectRayAndFixture (pnt, e.getRay());
         updatePosition (myPnt0, pnt, e);
         // if (!dragIsRepositioning(e)) {
         if (!dragIsRepositioning()) {
            fireDraggerMoveListeners (
               myTransform, myIncrementalTransform, e.getModifiersEx());
         }
         return true;
      }
      return false;
   }

   public boolean mouseMoved (MouseRayEvent e) {
      int comp = checkComponentSelection (e);
      if (comp != mySelectedComponent) {
         mySelectedComponent = comp;
         e.getViewer().repaint();
         return true;
      }
      return false;
   }
}
