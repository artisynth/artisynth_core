/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.*;
import java.awt.event.InputEvent;

import maspack.matrix.*;
import maspack.util.*;

import javax.media.opengl.GLDrawable;
import javax.media.opengl.GL2;

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

   static final int XY_PLANE = 4;
   static final int YZ_PLANE = 5;
   static final int ZX_PLANE = 6;

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

   public void render (GLRenderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }
      GL2 gl = renderer.getGL2().getGL2();
      gl.glPushMatrix();
      GLViewer.mulTransform (gl, myXDraggerToWorld);

      renderer.setLightingEnabled (false);
      gl.glLineWidth (myLineWidth);

      if (myDragMode != DragMode.OFF) {      
         gl.glColor3d (1f, 1f, 0f);
         gl.glPointSize (3);
         gl.glBegin (GL2.GL_POINTS);
         gl.glVertex3d (myPnt0.x, myPnt0.y, myPnt0.z);
         gl.glEnd();
         gl.glPointSize (1);
      }

      if (mySelectedComponent == X_AXIS) {
         gl.glColor3d (1f, 1f, 0);
      }
      else {
         gl.glColor3d (1f, 0, 0);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, 0.0, 0.0);
      gl.glVertex3d (mySize, 0.0, 0.0);
      gl.glEnd();

      if (mySelectedComponent == Y_AXIS) {
         gl.glColor3d (1f, 1f, 0);
      }
      else {
         gl.glColor3d (0, 1f, 0);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, 0.0, 0.0);
      gl.glVertex3d (0, mySize, 0.0);
      gl.glEnd();

      if (mySelectedComponent == Z_AXIS) {
         gl.glColor3d (1f, 1f, 0);
      }
      else {
         gl.glColor3d (0, 0, 1f);
      }
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, 0.0, 0.0);
      gl.glVertex3d (0, 0, mySize);
      gl.glEnd();

      double len = myPlaneBoxRelativeSize * mySize;

      // gl.glDisable (GL2.GL_CULL_FACE);

      // circle in x-y plane

      if (mySelectedComponent == XY_PLANE) {
         gl.glColor3d (1f, 1f, 0);
      }
      else {
         gl.glColor3d (0.5, 0.5, 0.5);
      }

      gl.glBegin (GL2.GL_LINE_STRIP);
      gl.glVertex3d (len, 0, 0);
      gl.glVertex3d (len, len, 0);
      gl.glVertex3d (0, len, 0);
      gl.glEnd();

      // circle in y-z plane
      if (mySelectedComponent == YZ_PLANE) {
         gl.glColor3d (1f, 1f, 0);
      }
      else {
         gl.glColor3d (0.5, 0.5, 0.5);
      }

      gl.glBegin (GL2.GL_LINE_STRIP);
      gl.glVertex3d (0, len, 0);
      gl.glVertex3d (0, len, len);
      gl.glVertex3d (0, 0, len);
      gl.glEnd();

      // circle in z-x plane
      if (mySelectedComponent == ZX_PLANE) {
         gl.glColor3d (1f, 1f, 0);
      }
      else {
         gl.glColor3d (0.5, 0.5, 0.5);
      }

      gl.glBegin (GL2.GL_LINE_STRIP);
      gl.glVertex3d (0, 0, len);
      gl.glVertex3d (len, 0, len);
      gl.glVertex3d (len, 0, 0);
      gl.glEnd();

      gl.glLineWidth (1);
      renderer.setLightingEnabled (true);

      // gl.glEnable (GL2.GL_CULL_FACE);
      gl.glPopMatrix();
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
      if (dragIsConstrained(e)) {
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
      DragMode mode = getDragMode (e);
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
         return true;
      }
      return false;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      if (mySelectedComponent != NONE) {
         Point3d pnt = new Point3d();
         intersectRayAndFixture (pnt, e.getRay());
         updatePosition (myPnt0, pnt, e);
         if (!dragIsRepositioning(e)) {
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
