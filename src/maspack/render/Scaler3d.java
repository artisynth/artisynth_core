/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.LinkedList;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLViewer;
import maspack.util.InternalErrorException;

public class Scaler3d extends Dragger3dBase {
   protected AffineTransform3d myTransform;
   protected AffineTransform3d myIncrementalTransform;
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

   public void render (Renderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }
      if (!(renderer instanceof GLViewer)) {
         return;
      }
      GLViewer viewer = (GLViewer)renderer;

      Shading savedShading = viewer.setShading (Shading.NONE);
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

      RenderObject ro = viewer.getSharedObject(Scaler3d.class);
      if (ro == null || !ro.isValid()) {
         ro = createScalerRenderable();
         viewer.addSharedObject(Scaler3d.class, ro);
      }
      
      ro.colorSet(mySelectedComponent);
      viewer.drawLines(ro);
      
      viewer.popModelMatrix();
      
      viewer.setLineWidth(1);
      viewer.setShading (savedShading);

   }
   
   private static RenderObject createScalerRenderable() {
      
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
         boolean constrained = dragIsConstrained ();
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
