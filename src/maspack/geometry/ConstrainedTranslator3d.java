/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.LinkedList;

import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.Dragger3dBase;
import maspack.render.Renderer;
import maspack.render.MouseRayEvent;
//import maspack.render.Dragger3dBase.DragMode;
import maspack.render.RenderObject;
import maspack.render.GL.GLViewer;

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
      viewer.translateModelMatrix(myXDraggerToWorld.p);
      viewer.scaleModelMatrix(mySize);
      
      RenderObject ro = viewer.getSharedObject(ConstrainedTranslator3d.class);
      if (ro == null || !ro.isValid()) {
         ro = createRenderable();
         viewer.addSharedObject(ConstrainedTranslator3d.class, ro);
      }
      
      if (selected) {
         ro.colorSet(1);
      } else {
         ro.colorSet(0);
      }
      viewer.drawLines(ro);
      
      viewer.popModelMatrix();
      viewer.setLightingEnabled (true);
   }
   
   private RenderObject createRenderable() {
      RenderObject ro = new RenderObject();
      
      int xcolor = ro.addColor(1.0f, 0f, 0f, 1.0f);
      int ycolor = ro.addColor(0f, 1.0f, 0f, 1.0f);
      int zcolor = ro.addColor(0f, 0f, 1.0f, 1.0f);
      
      // selected color set
      ro.createColorSet();
      ro.setColor(0, 1.0f, 1.0f, 0f, 1.0f);
      ro.setColor(1, 1.0f, 1.0f, 0f, 1.0f);
      ro.setColor(2, 1.0f, 1.0f, 0f, 1.0f);
      
      ro.color(xcolor);
      ro.addLine(new float[]{-1,0,0}, new float[]{1,0,0}); // x-axis
      ro.color(ycolor);
      ro.addLine(new float[]{0,-1,0}, new float[]{0,1,0}); // y-axis
      ro.color(zcolor);
      ro.addLine(new float[]{0,0,-1}, new float[]{0,0,1}); // z-axis
            
      return ro;
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
      if (mesh == null) {
         return;
      }
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
