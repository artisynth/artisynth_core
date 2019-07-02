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
import maspack.render.MouseRayEvent;
//import maspack.render.Dragger3dBase.DragMode;
import maspack.render.RenderObject;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;

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
   private static RenderObject renderObject = null;

   public ConstrainedTranslator3d() {
      super();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public void render (Renderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }
      
      Shading savedShading = renderer.setShading (Shading.NONE);
      renderer.setLineWidth(myLineWidth);
      
      renderer.pushModelMatrix();
      Vector3d t = myXDraggerToWorld.p;
      renderer.translateModelMatrix(t.x, t.y, t.z);
      renderer.scaleModelMatrix(mySize);
      
      if (renderObject == null) {
         renderObject = createRenderable(); 
      }
      
      int gidx = selected ? 1 : 0;
      renderer.drawLines(renderObject, gidx);
      
      renderer.popModelMatrix();
      renderer.setShading (savedShading);
   }
   
   private RenderObject createRenderable() {
      RenderObject ro = new RenderObject();
      
      int xcolor = ro.addColor(1.0f,   0f,   0f, 1.0f);
      int ycolor = ro.addColor(  0f, 1.0f,   0f, 1.0f);
      int zcolor = ro.addColor(  0f,   0f, 1.0f, 1.0f);
      int YELLOW = ro.addColor(1.0f, 1.0f,   0f, 1.0f);
      
      ro.createLineGroup();
      ro.createLineGroup();

      ro.lineGroup(0);
      ro.setCurrentColor(xcolor);
      ro.addLine(new float[]{-1,0,0}, new float[]{1,0,0}); // x-axis
      ro.setCurrentColor(ycolor);
      ro.addLine(new float[]{0,-1,0}, new float[]{0,1,0}); // y-axis
      ro.setCurrentColor(zcolor);
      ro.addLine(new float[]{0,0,-1}, new float[]{0,0,1}); // z-axis

      ro.setCurrentColor(YELLOW);

      ro.lineGroup(1);
      ro.addLine(new float[]{-1,0,0}, new float[]{1,0,0}); // x-axis
      ro.addLine(new float[]{0,-1,0}, new float[]{0,1,0}); // y-axis
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
         // myXDraggerToWorld.p now on surface
         myXDraggerToWorld.p.scaledAdd (duv.x, direction, origin);
      }
      else {
         Renderer renderer = e.getViewer();

         Point3d location = new Point3d (myXDraggerToWorld.p);
         RigidTransform3d EyeToWorld = renderer.getViewMatrix();
         EyeToWorld.invert();

         EyeToWorld.R.getColumn (2, planeNormal);  // normal to eye
         plane.set (planeNormal, location);        // plane through current dragger
         plane.intersectRay (planeLocation, direction, origin); // intersect plane with current click

         face = query.nearestFaceToPoint (
               location, coords, mesh, planeLocation); // find nearest point on constraint surface

         duv.x = myXDraggerToWorld.p.distance (EyeToWorld.p);
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
