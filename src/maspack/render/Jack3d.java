/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;

import javax.media.opengl.GLDrawable;
import javax.media.opengl.GL2;

public class Jack3d extends Dragger3dBase {
   protected int myCircleRes = 20;
   protected int mySelectedComponent = 0;
   protected Point3d myPnt0 = new Point3d();

   static final int NONE = 0;

   static final int X_AXIS = 1;
   static final int Y_AXIS = 2;
   static final int Z_AXIS = 3;

   static final int XY_CIRCLE = 4;
   static final int YZ_CIRCLE = 5;
   static final int ZX_CIRCLE = 6;

   private static Line xAxis = new Line (0, 0, 0, 1, 0, 0);
   private static Line yAxis = new Line (0, 0, 0, 0, 1, 0);
   private static Line zAxis = new Line (0, 0, 0, 0, 0, 1);

   private static Plane xyPlane = new Plane (0, 0, 1, 0);
   private static Plane yzPlane = new Plane (1, 0, 0, 0);
   private static Plane zxPlane = new Plane (0, 1, 0, 0);

   public Jack3d (double size) {
      super();
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

      gl.glColor3d (1f, 1f, 0f);
      gl.glPointSize (3);
      gl.glBegin (GL2.GL_POINTS);
      gl.glVertex3d (myPnt0.x, myPnt0.y, myPnt0.z);
      gl.glEnd();
      gl.glPointSize (1);

      gl.glColor3d (0, 0, 1f);

      // gl.glLoadName (X_AXIS);
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (mySize, 0.0, 0.0);
      gl.glVertex3d (-mySize, 0.0, 0.0);
      gl.glEnd();

      // gl.glLoadName (Y_AXIS);
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, mySize, 0.0);
      gl.glVertex3d (0, -mySize, 0.0);
      gl.glEnd();

      // gl.glLoadName (Z_AXIS);
      gl.glBegin (GL2.GL_LINES);
      gl.glVertex3d (0, 0, mySize);
      gl.glVertex3d (0, 0, -mySize);
      gl.glEnd();

      // circle in x-y plane
      // gl.glLoadName (XY_CIRCLE);
      gl.glColor3d (0, 1f, 0f);
      gl.glBegin (GL2.GL_LINE_LOOP);
      for (int i = 0; i < myCircleRes; i++) {
         double ang = 2 * Math.PI * i / myCircleRes;
         gl.glVertex3d (mySize * Math.cos (ang), mySize * Math.sin (ang), 0.0);
      }
      gl.glEnd();

      // circle in y-z plane
      // gl.glLoadName (YZ_CIRCLE);
      gl.glColor3d (0, 1f, 0f);
      gl.glBegin (GL2.GL_LINE_LOOP);
      for (int i = 0; i < myCircleRes; i++) {
         double ang = 2 * Math.PI * i / myCircleRes;
         gl.glVertex3d (0.0, mySize * Math.cos (ang), mySize * Math.sin (ang));
      }
      gl.glEnd();

      // circle in z-x plane
      // gl.glLoadName (ZX_CIRCLE);
      gl.glColor3d (0, 1f, 0f);
      gl.glBegin (GL2.GL_LINE_LOOP);
      for (int i = 0; i < myCircleRes; i++) {
         double ang = 2 * Math.PI * i / myCircleRes;
         gl.glVertex3d (mySize * Math.cos (ang), 0.0, -mySize * Math.sin (ang));
      }
      gl.glEnd();

      gl.glLineWidth (1);
      renderer.setLightingEnabled (true);
      gl.glPopMatrix();
   }

//   public void handleSelection (LinkedList list, int[] namestack, int idx) {
//      int id = namestack[idx];
//      switch (id) {
//         case X_AXIS:
//         case Y_AXIS:
//         case Z_AXIS:
//         case XY_CIRCLE:
//         case YZ_CIRCLE:
//         case ZX_CIRCLE: {
//            mySelectedComponent = id;
//            setSelected (true);
//            break;
//         }
//         default: {
//            mySelectedComponent = NONE;
//            break;
//         }
//      }
//   }

   public void getSelection (LinkedList<Object> list, int qid) {
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
         case XY_CIRCLE: {
            draggerRay.intersectPlane (p, xyPlane);
            p.z = 0;
            break;
         }
         case YZ_CIRCLE: {
            draggerRay.intersectPlane (p, yzPlane);
            p.x = 0;
            break;
         }
         case ZX_CIRCLE: {
            draggerRay.intersectPlane (p, zxPlane);
            p.y = 0;
            break;
         }
         default: {
            throw new InternalErrorException ("unexpected case");
         }
      }
   }

   private void updatePosition (Point3d p0, Point3d p1) {
      Vector3d planeNormal = null;

      switch (mySelectedComponent) {
         case X_AXIS:
         case Y_AXIS:
         case Z_AXIS: {
            Vector3d del = new Vector3d();
            del.sub (p1, p0);
            myXDraggerToWorld.mulXyz (del.x, del.y, del.z);
            break;
         }
         case XY_CIRCLE: {
            planeNormal = xyPlane.getNormal();
            break;
         }
         case YZ_CIRCLE: {
            planeNormal = yzPlane.getNormal();
            break;
         }
         case ZX_CIRCLE: {
            planeNormal = zxPlane.getNormal();
            break;
         }
      }
      if (planeNormal != null) {
         Vector3d cross = new Vector3d();
         cross.cross (p0, p1);
         double ang = Math.asin (cross.norm() / (p0.norm() * p1.norm()));
         if (cross.dot (planeNormal) < 0) {
            ang = -ang;
         }
         myXDraggerToWorld.R.mulAxisAngle (planeNormal, ang);
      }
   }

//   public void draggerSelected (MouseRayEvent e) {
//      intersectRayAndFixture (myPnt0, e.getRay());
//   }

   public boolean mouseReleased (MouseRayEvent e) {
      mySelectedComponent = NONE;
      return true;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      Point3d pnt = new Point3d();
      intersectRayAndFixture (pnt, e.getRay());
      updatePosition (myPnt0, pnt);
      return true;
   }
}
