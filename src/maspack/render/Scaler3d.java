/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.event.InputEvent;
import java.util.LinkedList;

import javax.media.opengl.GL2;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.InternalErrorException;

public class Scaler3d extends Dragger3dBase {
   protected AffineTransform3d myTransform;
   protected AffineTransform3d myIncrementalTransform;
   protected int myCircleRes = 20;
   protected int mySelectedComponent = NONE;
   protected Point3d myPnt0 = new Point3d();

   //protected GLViewer myViewer; // hack to get repaint

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

   public Scaler3d() {
      super();
      myTransform = new AffineTransform3d();
      myIncrementalTransform = new AffineTransform3d();
   }

   public Scaler3d (GLViewer viewer, double size) {
      this();
      setSize (size);
      //myViewer = viewer;
   }

   public void render (GLRenderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }
      GL2 gl = renderer.getGL2().getGL2();

      renderer.setLightingEnabled (false);
      gl.glLineWidth (myLineWidth);

      gl.glPushMatrix();
      GLViewer.mulTransform (gl, myXDraggerToWorld);

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

      // x-y plane box
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

      // y-z plane box
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

      // z-x plane box
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

      gl.glPopMatrix();

      gl.glLineWidth (1);
      renderer.setLightingEnabled (true);

      // gl.glEnable (GL2.GL_CULL_FACE);

   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }

   private boolean rotationSelectCheck (
      double d, double tempDist, double minDist, double lineDist) {
      return d != Double.POSITIVE_INFINITY && tempDist < lineDist &&
         tempDist < minDist;
   };

   private int checkComponentSelection (Line ray, double distancePerPixel) {
      Line draggerRay = new Line (ray);
      draggerRay.inverseTransform (myXDraggerToWorld);

      double lineDist = 5 * distancePerPixel;
      double l, d;

      Point3d p = new Point3d();

      // check axes first

      l = xAxis.nearestPoint (p, draggerRay);
      if (l >= 0 && l <= mySize) {
         if (draggerRay.distance (p) < lineDist) {
            return X_AXIS;
         }
      }
      l = yAxis.nearestPoint (p, draggerRay);
      if (l >= 0 && l <= mySize) {
         if (draggerRay.distance (p) < lineDist) {
            return Y_AXIS;
         }
      }
      l = zAxis.nearestPoint (p, draggerRay);
      if (l >= 0 && l <= mySize) {
         if (draggerRay.distance (p) < lineDist) {
            return Z_AXIS;
         }
      }

      // now check planes
      double len = myPlaneBoxRelativeSize * mySize;
      d = draggerRay.intersectPlane (p, xyPlane);
      if (d != inf && p.x >= 0 && p.x <= len && p.y >= 0 && p.y <= len) {
         return XY_PLANE;
      }

      d = draggerRay.intersectPlane (p, yzPlane);
      if (d != inf && p.y >= 0 && p.y <= len && p.z >= 0 && p.z <= len) {
         return YZ_PLANE;
      }

      d = draggerRay.intersectPlane (p, zxPlane);
      if (d != inf && p.z >= 0 && p.z <= len && p.x >= 0 && p.x <= len) {
         return ZX_PLANE;
      }
      return NONE;
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

   private void updatePosition (Point3d p1, boolean constrained) {
      Vector3d d = new Vector3d(), o = new Vector3d();

      o.sub (myPnt0, myTransform.p);

      if (constrained) {
         d.sub (p1, myPnt0);
         double s = getConstrainedStepSize();
         d.x = s*Math.round(d.x/s);
         d.y = s*Math.round(d.y/s);
         d.z = s*Math.round(d.z/s);
         d.add (o);
      }
      else {
         d.sub (p1, myTransform.p);
      }

      double x, y, z;
      x = d.x == 0 ? 1 : (o.x == 0 ? 1e-10 : Math.abs (d.x / o.x));
      y = d.y == 0 ? 1 : (o.y == 0 ? 1e-10 : Math.abs (d.y / o.y));
      z = d.z == 0 ? 1 : (o.z == 0 ? 1e-10 : Math.abs (d.z / o.z));

      myIncrementalTransform.set (myTransform);
      myTransform.setIdentity();
      myTransform.applyScaling (x, y, z);
      myIncrementalTransform.mulInverseLeft (
         myIncrementalTransform, myTransform);
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
         boolean constrained = dragIsConstrained (e);
         Point3d pnt = new Point3d();
         intersectRayAndFixture (pnt, e.getRay());
         updatePosition (pnt, constrained);
         fireDraggerMoveListeners (
            myTransform, myIncrementalTransform, e.getModifiersEx());
         return true;
      }
      return false;
   }

   public boolean mouseMoved (MouseRayEvent e) {
      double dpp = e.distancePerPixel (myXDraggerToWorld.p);
      int comp = checkComponentSelection (e.getRay(), dpp);
      if (comp != mySelectedComponent) {
         mySelectedComponent = comp;
         e.getViewer().repaint();
         return true;
      }
      return false;
   }
}
