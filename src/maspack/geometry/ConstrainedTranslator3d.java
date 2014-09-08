/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.LinkedList;

import javax.media.opengl.GL2;

import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.Dragger3dBase;
import maspack.render.GLRenderer;
import maspack.render.GLViewer;
import maspack.render.MouseRayEvent;
//import maspack.render.Dragger3dBase.DragMode;

/**
 * A translational dragger that keeps its origin attached to the surface
 * of a PolygonalMesh. 
 * 
 * <p>This class is defined in maspack.geometry instead
 * of maspack.render so that the latter will not depend on the former.
 * 
 * @author lloyd
 */
public class ConstrainedTranslator3d extends Dragger3dBase {
   PolygonalMesh mesh = null;

   Point3d firstLocation = new Point3d();
   Face face;
   Vector3d duv = new Vector3d();
   TriangleIntersector intersector = new TriangleIntersector();
   BVFeatureQuery query = new BVFeatureQuery();

   boolean selected = false;

   Vector3d planeNormal = new Vector3d();
   Plane plane = new Plane();
   Point3d planeLocation = new Point3d();
   Vector2d coords = new Vector2d();

   RigidTransform3d transform = new RigidTransform3d();
   RigidTransform3d myIncrementalTransform = new RigidTransform3d();

   private static Line xAxis = new Line (0, 0, 0, 1, 0, 0);
   private static Line yAxis = new Line (0, 0, 0, 0, 1, 0);
   private static Line zAxis = new Line (0, 0, 0, 0, 0, 1);

   public ConstrainedTranslator3d() {
      super();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public void render (GLRenderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }

      GL2 gl = renderer.getGL2().getGL2();

      renderer.setLightingEnabled (false);
      gl.glLineWidth (myLineWidth);

      gl.glPushMatrix();
      gl.glTranslated (
         myXDraggerToWorld.p.x, myXDraggerToWorld.p.y, myXDraggerToWorld.p.z);

      gl.glBegin (GL2.GL_LINES);

      if (selected)
         gl.glColor3d (1, 1, 0);

      if (!selected)
         gl.glColor3d (1, 0, 0);
      gl.glVertex3d (-mySize, 0, 0);
      gl.glVertex3d (mySize, 0, 0);

      if (!selected)
         gl.glColor3d (0, 1, 0);
      gl.glVertex3d (0, -mySize, 0);
      gl.glVertex3d (0, mySize, 0);

      if (!selected)
         gl.glColor3d (0, 0, 1);
      gl.glVertex3d (0, 0, -mySize);
      gl.glVertex3d (0, 0, mySize);

      gl.glEnd();

      gl.glPopMatrix();

      renderer.setLightingEnabled (true);
   }

   public boolean mousePressed (MouseRayEvent e) {
      // DragMode mode = getDragMode (e);
      DragMode mode = getDragMode ();
      if (mode != DragMode.OFF && myVisibleP) {
         double dpp = e.distancePerPixel (myXDraggerToWorld.p);
         if (checkComponentSelection (e.getRay(), dpp)) {
            myDragMode = mode;
            transform.setIdentity();
            updateLocation (e);

            fireDraggerBeginListeners (
               transform, myIncrementalTransform, e.getModifiersEx());
            return true;
         }
      }
      return false;
   }

   public boolean mouseReleased (MouseRayEvent e) {
      if (myDragMode != DragMode.OFF) {
         if (myVisibleP) {
            double dpp = e.distancePerPixel (myXDraggerToWorld.p);
            if (checkComponentSelection (e.getRay(), dpp)) {
               updateLocation (e);
               firstLocation.set (myXDraggerToWorld.p);

               fireDraggerEndListeners (
                  transform, myIncrementalTransform, e.getModifiersEx());
            }
         }
         myDragMode = DragMode.OFF;
         clearFlags();
         return true;
      }
      return false;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      if (myVisibleP && mesh != null) {
         updateLocation (e);

         fireDraggerMoveListeners (
            transform, myIncrementalTransform, e.getModifiersEx());
         return true;
      }
      return false;
   }

   public boolean mouseMoved (MouseRayEvent e) {
      if (myVisibleP) {
         double dpp = e.distancePerPixel (myXDraggerToWorld.p);
         boolean over = checkComponentSelection (e.getRay(), dpp);
         if (over != selected) {
            selected = over;
            return true;
         }
      }
      return false;
   }

   public PolygonalMesh getMesh() {
      return mesh;
   }

   public void setMesh (PolygonalMesh mesh) {
      this.mesh = mesh;
   }

   public void setLocation (Point3d location) {
      this.firstLocation.set (location);
      myXDraggerToWorld.p.set (location);
      transform.setIdentity();
   }

   private void updateLocation (MouseRayEvent e) {
      Line ray = e.getRay();
      Point3d origin = ray.getOrigin();
      Vector3d direction = ray.getDirection();

      //face = obbt.intersect (origin, direction, duv, intersector);
      face = query.nearestFaceAlongRay (
         null, duv, mesh, origin, direction);

      if (face != null) {
         myXDraggerToWorld.p.scaledAdd (duv.x, direction, origin);
      }
      else {
         GLViewer viewer = e.getViewer();

         Point3d location = new Point3d (myXDraggerToWorld.p);

         viewer.getEyeToWorld().R.getColumn (2, planeNormal);
         plane.set (planeNormal, location);
         plane.intersectRay (planeLocation, direction, origin);

         face =
            query.nearestFaceToPoint (
               location, coords, mesh, planeLocation);

         duv.x = myXDraggerToWorld.p.distance (viewer.getEyeToWorld().p);
         duv.y = coords.x;
         duv.z = coords.y;

         myXDraggerToWorld.p.set (location);
      }
      myIncrementalTransform.p.set (transform.p);
      transform.p.sub (myXDraggerToWorld.p, firstLocation);
      myIncrementalTransform.p.sub (transform.p, myIncrementalTransform.p);
   }

   private boolean checkComponentSelection (Line ray, double distancePerPixel) {
      Line draggerRay = new Line (ray);
      draggerRay.inverseTransform (myXDraggerToWorld);

      double lineDist = 5 * distancePerPixel;
      double l;

      Point3d p = new Point3d();

      // check axes first

      l = xAxis.nearestPoint (p, draggerRay);
      if (l >= -mySize && l <= mySize) {
         if (draggerRay.distance (p) < lineDist) {
            return true;
         }
      }
      l = yAxis.nearestPoint (p, draggerRay);
      if (l >= -mySize && l <= mySize) {
         if (draggerRay.distance (p) < lineDist) {
            return true;
         }
      }
      l = zAxis.nearestPoint (p, draggerRay);
      if (l >= -mySize && l <= mySize) {
         if (draggerRay.distance (p) < lineDist) {
            return true;
         }
      }

      return false;
   }
}
