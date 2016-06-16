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
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.util.InternalErrorException;

public class Jack3d extends Dragger3dBase {
   protected int myCircleRes = 64;
   protected int mySelectedComponent = 0;
   protected Point3d myPnt0 = new Point3d();

   static final int NONE = 0;

   static final int X_AXIS = 1;
   static final int Y_AXIS = 2;
   static final int Z_AXIS = 3;

   static final int X_ROTATE = 4;
   static final int Y_ROTATE = 5;
   static final int Z_ROTATE = 6;

   private static Line xAxis = new Line (0, 0, 0, 1, 0, 0);
   private static Line yAxis = new Line (0, 0, 0, 0, 1, 0);
   private static Line zAxis = new Line (0, 0, 0, 0, 0, 1);

   private static Plane xyPlane = new Plane (0, 0, 1, 0);
   private static Plane yzPlane = new Plane (1, 0, 0, 0);
   private static Plane zxPlane = new Plane (0, 1, 0, 0);
   
   private static RenderObject renderObject = null;

   public Jack3d (double size) {
      super();
      setSize (size);
   }

   public void render (Renderer renderer, int flags) {

      if (!myVisibleP) {
         return;
      }

      renderer.pushModelMatrix();
      renderer.mulModelMatrix(myXDraggerToWorld);

      Shading savedShading = renderer.setShading (Shading.NONE);
      renderer.setLineWidth(myLineWidth);
      renderer.setPointSize(3);

      float[] coords = new float[3];
      renderer.setColor(1, 1, 0);
      myPnt0.get(coords);
      renderer.drawPoint(coords);

      renderer.scaleModelMatrix(mySize);

      if (renderObject == null) {
         renderObject = createJackRenderable();
      }

      // draw selected component first
      if (mySelectedComponent != 0) {
         renderer.drawLines(renderObject, mySelectedComponent);
      }
      renderer.drawLines(renderObject, 0);

      renderer.setLineWidth(1);
      renderer.setShading (savedShading);
      renderer.popModelMatrix();

   }

   private static void addLineLoop (RenderObject robj, int pidx0, int numv) {
      robj.beginBuild (DrawMode.LINE_LOOP);
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

   private RenderObject createJackRenderable() {

      final int QUARTER_CIRCLE_RESOLUTION = 32;
      final int FULL_CIRCLE_RESOLUTION = 4*QUARTER_CIRCLE_RESOLUTION;

      RenderObject robj = new RenderObject();

      int RED    = robj.addColor (1.0f, 0.0f, 0.0f, 1.0f);
      int GREEN  = robj.addColor (0.0f, 1.0f, 0.0f, 1.0f);
      int BLUE   = robj.addColor (0.0f, 0.0f, 1.0f, 1.0f);
      int GRAY   = robj.addColor (0.5f, 0.5f, 0.5f, 1.0f);
      int YELLOW = robj.addColor (1.0f, 1.0f, 0.0f, 1.0f);
      
      int px     = robj.addPosition (1.0f, 0.0f, 0.0f);
      int py     = robj.addPosition (0.0f, 1.0f, 0.0f);
      int pz     = robj.addPosition (0.0f, 0.0f, 1.0f);
      int nx     = robj.addPosition (-1.0f, 0.0f, 0.0f);
      int ny     = robj.addPosition (0.0f, -1.0f, 0.0f);
      int nz     = robj.addPosition (0.0f, 0.0f, -1.0f);

      // circle around x-axis
      for (int i = 0; i <= FULL_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         robj.addPosition (0f, (float)Math.cos(ang), (float)Math.sin(ang));
      }
      int protx = nz+1;
      
      // y-axis
      for (int i = 0; i <= FULL_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         robj.addPosition ((float)Math.cos(ang), 0f, -(float)Math.sin(ang));
      }
      int proty = protx+FULL_CIRCLE_RESOLUTION+1;
      
      // z-axis
      for (int i = 0; i <= FULL_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         robj.addPosition ((float)Math.cos(ang), (float)Math.sin (ang), 0f);
      }
      int protz = proty+FULL_CIRCLE_RESOLUTION+1;

      for (int i=0; i<7; i++) {
         robj.createLineGroup();
      }

      robj.lineGroup (0);

      robj.setCurrentColor (RED);
      addLine (robj, nx, px);
      robj.setCurrentColor (GREEN);
      addLine (robj, ny, py);
      robj.setCurrentColor (BLUE);
      addLine (robj, nz, pz);
      
      robj.setCurrentColor (RED);
      addLineLoop (robj, protx, FULL_CIRCLE_RESOLUTION+1);

      robj.setCurrentColor (GREEN);
      addLineLoop (robj, proty, FULL_CIRCLE_RESOLUTION+1);

      robj.setCurrentColor (BLUE);
      addLineLoop (robj, protz, FULL_CIRCLE_RESOLUTION+1);

      robj.setCurrentColor (YELLOW);

      robj.lineGroup (X_AXIS);
      addLine (robj, nx, px);
      robj.lineGroup (Y_AXIS);
      addLine (robj, ny, py);
      robj.lineGroup (Z_AXIS);
      addLine (robj, nz, pz);

      robj.lineGroup (X_ROTATE);
      addLineLoop (robj, protx, FULL_CIRCLE_RESOLUTION+1);
      robj.lineGroup (Y_ROTATE);
      addLineLoop (robj, proty, FULL_CIRCLE_RESOLUTION+1);
      robj.lineGroup (Z_ROTATE);
      addLineLoop (robj, protz, FULL_CIRCLE_RESOLUTION+1);

      return robj;
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

   public boolean rotationSelectCheck (
      double d, double tempDist, double lineDist, double minDist) {
      return d != inf && tempDist < lineDist && tempDist < minDist;
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
      if (l >= -mySize && l <= mySize && tempDist < lineDist) {
         resultAxisOrPlane = X_AXIS;
         minDist = tempDist;
      }

      l = yAxis.nearestPoint (p, draggerRay);
      tempDist = draggerRay.distance (p);
      if (l >=-mySize && l <= mySize && tempDist < lineDist && tempDist < minDist) {
         resultAxisOrPlane = Y_AXIS;
         minDist = tempDist;
      }

      l = zAxis.nearestPoint (p, draggerRay);
      tempDist = draggerRay.distance (p);
      if (l >= -mySize && l <= mySize && tempDist < lineDist && tempDist < minDist) {
         resultAxisOrPlane = Z_AXIS;
         minDist = tempDist;
      }

      if (resultAxisOrPlane != NONE) {
         return resultAxisOrPlane;
      }

      // now check rotators, and if there is any that are selected and
      // closer to the mouse than any of the axes, then select it.
      double len = mySize;

      d = draggerRay.intersectPlane (p, yzPlane);
      tempDist = Math.abs (p.norm() - mySize);
      if (rotationSelectCheck (d, tempDist, lineDist, minDist)) {
         resultAxisOrPlane = X_ROTATE;
         minDist = tempDist;
      }

      d = draggerRay.intersectPlane (p, zxPlane);
      tempDist = Math.abs (p.norm() - mySize);
      if (rotationSelectCheck (d, tempDist, lineDist, minDist)) {
         resultAxisOrPlane = Y_ROTATE;
         minDist = tempDist;
      }

      d = draggerRay.intersectPlane (p, xyPlane);
      tempDist = Math.abs (p.norm() - mySize);
      if (rotationSelectCheck (d, tempDist, lineDist, minDist)) {
         resultAxisOrPlane = Z_ROTATE;
      }

      // if any of the axes or rotators are selected, then
      // return the axis or rotator that the mouse is closest to.
      if (resultAxisOrPlane != NONE) {
         return resultAxisOrPlane;
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
         case Z_ROTATE: {
            draggerRay.intersectPlane (p, xyPlane);
            p.z = 0;
            break;
         }
         case X_ROTATE: {
            draggerRay.intersectPlane (p, yzPlane);
            p.x = 0;
            break;
         }
         case Y_ROTATE: {
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
         case Z_ROTATE: {
            planeNormal = xyPlane.getNormal();
            break;
         }
         case X_ROTATE: {
            planeNormal = yzPlane.getNormal();
            break;
         }
         case Y_ROTATE: {
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

   public boolean mouseMoved ( MouseRayEvent e ) {
      int comp = checkComponentSelection (e);
      if (comp != mySelectedComponent) {
         mySelectedComponent = comp;
         e.getViewer().repaint();
         return true;
      }
      return false;
   }

   public boolean mouseReleased (MouseRayEvent e) {
      mySelectedComponent = NONE;
      clearFlags();
      return true;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      Point3d pnt = new Point3d();
      intersectRayAndFixture (pnt, e.getRay());
      updatePosition (myPnt0, pnt);
      return true;
   }
}
