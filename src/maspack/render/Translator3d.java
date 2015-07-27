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
   
   private static Line xAxis = new Line (0, 0, 0, 1, 0, 0);
   private static Line yAxis = new Line (0, 0, 0, 0, 1, 0);
   private static Line zAxis = new Line (0, 0, 0, 0, 0, 1);

   private static Plane xyPlane = new Plane (0, 0, 1, 0);
   private static Plane yzPlane = new Plane (1, 0, 0, 0);
   private static Plane zxPlane = new Plane (0, 1, 0, 0);

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
      if (!(renderer instanceof GLViewer)) {
         return;
      }
      GLViewer viewer = (GLViewer)renderer;

      viewer.setLightingEnabled (false);
      viewer.setLineWidth(myLineWidth);
      
      viewer.pushModelMatrix();
      viewer.mulModelMatrix(myXDraggerToWorld);
      
      float[] coords = new float[3];
      if (myDragMode != DragMode.OFF && mySelectedComponent != NONE) { 
         viewer.setColor(1.0f, 1.0f, 0f);
         viewer.setPointSize(3);
         myPnt0.get(coords);
         viewer.drawPoint(coords);
         viewer.setPointSize(1);
      }
      
      viewer.scaleModelMatrix(mySize);

      RenderObject ro = viewer.getSharedObject(Translator3d.class);
      if (ro == null || !ro.isValid()) {
         ro = createTranslatorRenderable();
         viewer.addSharedObject(Translator3d.class, ro);
      }
      
      // select appropriate color buffer
      ro.colorSet(mySelectedComponent);  
      viewer.drawLines(ro);
      
      viewer.popModelMatrix();
      
      viewer.setLineWidth(1);
      viewer.setLightingEnabled (true);
   }
   
   private static RenderObject createTranslatorRenderable() {
      
      final float TRANS_BOX_SIZE = 0.4f;
      
      RenderObject scalerr = new RenderObject();
      
      // 3 axis, 3 plane boxes
      int xcolor = scalerr.addColor(1.0f, 0.0f, 0.0f, 1.0f);
      int ycolor = scalerr.addColor(0.0f, 1.0f, 0.0f, 1.0f);
      int zcolor = scalerr.addColor(0.0f, 0.0f, 1.0f, 1.0f);
      int yzcolor = scalerr.addColor(0.5f, 0.5f, 0.5f, 1.0f);
      int zxcolor = scalerr.addColor(0.5f, 0.5f, 0.5f, 1.0f);
      int xycolor = scalerr.addColor(0.5f, 0.5f, 0.5f, 1.0f);
      
      // 6 more color sets, one each with a component highlighted yellow
      int[] colors = new int[]{xcolor,ycolor,zcolor,yzcolor,zxcolor,xycolor};
      for (int i=0; i<colors.length; ++i) {
         scalerr.createColorSetFrom(0);
         scalerr.setColor(i, 1.0f, 1.0f, 0.0f, 1.0f);
      }
      
      int v0, v1, v2;
      
      // x-axis
      scalerr.color(xcolor);
      v0 = scalerr.vertex(0, 0, 0);
      v1 = scalerr.vertex(1, 0, 0);
      scalerr.addLine(v0, v1);
      
      // y-axis
      scalerr.color(ycolor);
      v0 = scalerr.vertex(0, 0, 0);
      v1 = scalerr.vertex(0, 1, 0);
      scalerr.addLine(v0, v1);
      
      // z-axis
      scalerr.color(zcolor);
      v0 = scalerr.vertex(0, 0, 0);
      v1 = scalerr.vertex(0, 0, 1);
      scalerr.addLine(v0, v1);
      
      // yz-plane
      scalerr.color(yzcolor);
      v0 = scalerr.vertex(0, TRANS_BOX_SIZE, 0);
      v1 = scalerr.vertex(0, TRANS_BOX_SIZE, TRANS_BOX_SIZE);
      v2 = scalerr.vertex(0, 0, TRANS_BOX_SIZE);
      scalerr.addLineStrip(v0, v1, v2);

      // zx-plane
      scalerr.color(zxcolor);
      v0 = scalerr.vertex(0, 0, TRANS_BOX_SIZE);
      v1 = scalerr.vertex(TRANS_BOX_SIZE, 0, TRANS_BOX_SIZE);
      v2 = scalerr.vertex(TRANS_BOX_SIZE, 0, 0);
      scalerr.addLineStrip(v0, v1, v2);
      
      // xy-plane
      scalerr.color(xycolor);
      v0 = scalerr.vertex(TRANS_BOX_SIZE, 0, 0);
      v1 = scalerr.vertex(TRANS_BOX_SIZE, TRANS_BOX_SIZE, 0);
      v2 = scalerr.vertex(0, TRANS_BOX_SIZE, 0);
      scalerr.addLineStrip(v0, v1, v2);
   
      return scalerr;
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
      draggerToEye.mulInverseLeft (
         e.getViewer().getEyeToWorld(), myXDraggerToWorld);

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
         GLViewer viewer = e.getViewer();
         boolean constrainedToGrid = false;
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
         if (!constrainedToGrid) {
            // if not possible to constrain to grid, just constrain the step
            // size
            double s = getConstrainedStepSize(viewer);
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
