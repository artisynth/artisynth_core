/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.LinkedList;

import maspack.matrix.AxisAngle;
import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.DrawMode;
import maspack.render.GL.GLViewer;
import maspack.util.InternalErrorException;

public class Rotator3d extends Dragger3dBase {
   protected int mySelectedComponent = NONE;

   protected Point3d myPnt0 = new Point3d();
   protected Point3d myRotPnt = new Point3d();
   protected RotationMatrix3d myRot0 = new RotationMatrix3d();
   protected RotationMatrix3d myRot = new RotationMatrix3d();
   // protected boolean myDraggingP = false;
   protected RigidTransform3d myTransform;
   protected RigidTransform3d myIncrementalTransform;
   protected RigidTransform3d myXDraggerToWorld0 = new RigidTransform3d();

   protected GLViewer myViewer; // hack to get repaint

   static final int NONE = 0;

   static final int X_AXIS = 1;
   static final int Y_AXIS = 2;
   static final int Z_AXIS = 3;

   static final int OUTER_SPHERE = 4;

   private static final Line xAxis = new Line (0, 0, 0, 1, 0, 0);
   private static final Line yAxis = new Line (0, 0, 0, 0, 1, 0);
   private static final Line zAxis = new Line (0, 0, 0, 0, 0, 1);

   private static final Plane xyPlane = new Plane (0, 0, 1, 0);
   private static final Plane yzPlane = new Plane (1, 0, 0, 0);
   private static final Plane zxPlane = new Plane (0, 1, 0, 0);

   private static RenderObject renderObject = null;
   
   public Rotator3d() {
      super();
      myTransform = new RigidTransform3d();
      myIncrementalTransform = new RigidTransform3d();
   }

   public Rotator3d (GLViewer viewer, double size) {
      this();
      setSize (size);
      myViewer = viewer;
   }

   public void render (Renderer renderer, int flags) {
      if (!myVisibleP) {
         return;
      }
      
      renderer.pushModelMatrix();
      renderer.mulModelMatrix(myXDraggerToWorld);

      Shading savedShading = renderer.setShading (Shading.NONE);
      renderer.setLineWidth(myLineWidth);
      renderer.scaleModelMatrix(mySize);
      
      if (renderObject == null || !renderObject.isValid()) {
         renderObject = createRotatorRenderable();
      }
      
      // highlight appropriate axis
      if (mySelectedComponent != 0) {
         renderer.drawLines(renderObject, mySelectedComponent);
      }
      renderer.drawLines(renderObject, 0);
         
      renderer.popModelMatrix();
      
      if (myDragMode != DragMode.OFF) {
         switch (mySelectedComponent) {
            case X_AXIS:
            case Y_AXIS:
            case Z_AXIS: {

               final float[] coords0 = new float[]{0,0,0};
               float[] coords = new float[3];

               // Draw rotation lines using the orientation at the time the drag was
               // started
               RigidTransform3d X = new RigidTransform3d (myXDraggerToWorld0);
               X.p.set (myXDraggerToWorld.p);
               
               renderer.pushModelMatrix();
               renderer.mulModelMatrix(X);

               renderer.setColor(0.5f, 0.5f, 0.5f);
               myPnt0.get(coords);
               renderer.drawLine(coords0, coords);

               renderer.setColor(1, 1, 0);
               myRotPnt.get(coords);
               renderer.drawLine(coords0,coords);
               
               renderer.popModelMatrix();

               break;
            }
         }
      }

      renderer.setLineWidth(1);
      renderer.setShading (savedShading);
   }

   private static void addLineLoop (RenderObject robj, int pidx0, int numv) {
      robj.beginBuild (DrawMode.LINE_LOOP);
      for (int i=0; i<numv; i++) {
         robj.addVertex (pidx0+i);
      }
      robj.endBuild();
   }
   
   private RenderObject createRotatorRenderable() {
      
      final int QUARTER_CIRCLE_RESOLUTION = 32;
      final int FULL_CIRCLE_RESOLUTION = 4*QUARTER_CIRCLE_RESOLUTION;

      RenderObject rotr = new RenderObject();

      int xcolor = rotr.addColor(1f, 0f, 0f, 1f);
      int ycolor = rotr.addColor(0f, 1f, 0f, 1f);  
      int zcolor = rotr.addColor(0f, 0f, 1f, 1f);
      int YELLOW = rotr.addColor(1f, 1f, 0f, 1f);

      // circle around x-axis
      for (int i = 0; i <= FULL_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         rotr.addPosition (0f, (float)Math.cos(ang), (float)Math.sin(ang));
      }
      int protx = 0;
      
      // y-axis
      for (int i = 0; i <= FULL_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         rotr.addPosition ((float)Math.cos(ang), 0f, -(float)Math.sin(ang));
      }
      int proty = protx+FULL_CIRCLE_RESOLUTION+1;
      
      // z-axis
      for (int i = 0; i <= FULL_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         rotr.addPosition ((float)Math.cos(ang), (float)Math.sin (ang), 0f);
      }
      int protz = proty+FULL_CIRCLE_RESOLUTION+1;

      for (int i=0; i<4; i++) {
         rotr.createLineGroup();
      }     

      rotr.lineGroup (0);
      rotr.setCurrentColor(xcolor);
      addLineLoop (rotr, protx, FULL_CIRCLE_RESOLUTION+1);
      rotr.setCurrentColor(ycolor);
      addLineLoop (rotr, proty, FULL_CIRCLE_RESOLUTION+1);
      rotr.setCurrentColor(zcolor);
      addLineLoop (rotr, protz, FULL_CIRCLE_RESOLUTION+1);

      rotr.setCurrentColor(YELLOW);

      rotr.lineGroup (X_AXIS);
      addLineLoop (rotr, protx, FULL_CIRCLE_RESOLUTION+1);
      rotr.lineGroup (Y_AXIS);
      addLineLoop (rotr, proty, FULL_CIRCLE_RESOLUTION+1);
      rotr.lineGroup (Z_AXIS);
      addLineLoop (rotr, protz, FULL_CIRCLE_RESOLUTION+1);

      return rotr;
   }

   public boolean rotationSelectCheck (
      double d, double tempDist, double lineDist, double minDist) {
      return d != inf && tempDist < lineDist && tempDist < minDist;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   private int checkComponentSelection (MouseRayEvent e) {

      Line draggerRay = new Line (e.getRay());
      draggerRay.inverseTransform (myXDraggerToWorld);

      // double lineDist = 5*distancePerPixel; // 0.05*mySize;
      double lineDist = 5 * e.distancePerPixel (myXDraggerToWorld.p);
      double minDist = Double.POSITIVE_INFINITY;
      double d, tempDist;

      Point3d p = new Point3d();

      // check if the mouse is selecting a rotator, and return
      // the closest rotator if it is.
      int axis = NONE;

      d = draggerRay.intersectPlane (p, yzPlane);
      tempDist = Math.abs (p.norm() - mySize);
      if (rotationSelectCheck (d, tempDist, lineDist, minDist)) {
         axis = X_AXIS;
         minDist = tempDist;
      }

      d = draggerRay.intersectPlane (p, zxPlane);
      tempDist = Math.abs (p.norm() - mySize);
      if (rotationSelectCheck (d, tempDist, lineDist, minDist)) {
         axis = Y_AXIS;
         minDist = tempDist;
      }

      d = draggerRay.intersectPlane (p, xyPlane);
      tempDist = Math.abs (p.norm() - mySize);
      if (rotationSelectCheck (d, tempDist, lineDist, minDist)) {
         axis = Z_AXIS;
      }

      return axis;

   }

   private void findRotation (RotationMatrix3d R, Point3d p, Line ray) {
      Line draggerRay = new Line (ray);
      draggerRay.inverseTransform (myXDraggerToWorld0);

      switch (mySelectedComponent) {
         case X_AXIS: {
            draggerRay.intersectPlane (p, yzPlane);
            R.setAxisAngle (xAxis.getDirection(), Math.atan2 (p.z, p.y));
            break;
         }
         case Y_AXIS: {
            draggerRay.intersectPlane (p, zxPlane);
            R.setAxisAngle (yAxis.getDirection(), Math.atan2 (p.x, p.z));
            break;
         }
         case Z_AXIS: {
            draggerRay.intersectPlane (p, xyPlane);
            R.setAxisAngle (zAxis.getDirection(), Math.atan2 (p.y, p.x));
            break;
         }
         case OUTER_SPHERE: {
            break;
         }
         default: {
            throw new InternalErrorException (
               "unexpected case " + mySelectedComponent);
         }
      }
      double mag = p.norm();
      if (mag != 0) {
         p.scale (mySize / mag);
      }
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

   public boolean mousePressed (MouseRayEvent e) {
      // DragMode mode = getDragMode (e);
      DragMode mode = getDragMode ();
      if (mode != DragMode.OFF && mySelectedComponent != NONE) {
         myDragMode = mode;
         myXDraggerToWorld0.set (myXDraggerToWorld);
         findRotation (myRot0, myPnt0, e.getRay());
         myRotPnt.set (myPnt0);
         myTransform.setIdentity();
         myIncrementalTransform.setIdentity();
         fireDraggerBeginListeners (
            myTransform, myIncrementalTransform, e.getModifiersEx());
         return true;
      }
      return false;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      if (mySelectedComponent != NONE) {
         //boolean constrained = dragIsConstrained (e);
         boolean constrained = dragIsConstrained ();
         findRotation (myRot, myRotPnt, e.getRay());
         myRot.mulInverseLeft (myRot0, myRot);
         if (constrained) {
            AxisAngle axisAng = new AxisAngle();
            myRot.getAxisAngle (axisAng);
            double deg = Math.toDegrees (axisAng.angle);
            axisAng.angle = Math.toRadians (5*Math.round(deg/5));
            myRot.setAxisAngle (axisAng);
            myRotPnt.transform (myRot, myPnt0);
         }
         // myTransform.setIdentity();
         myIncrementalTransform.R.mulInverseLeft (myTransform.R, myRot);
         myTransform.R.set (myRot);
         // By default, do not move the rotator coordinate frame
         // with the rotation ...
         // myXDraggerToWorld.R.mul (myIncrementalTransform.R);
         //if (!dragIsRepositioning(e)) {
         if (!dragIsRepositioning()) {
            fireDraggerMoveListeners (
               myTransform, myIncrementalTransform, e.getModifiersEx());
         }
         else {
            myXDraggerToWorld.mul (myIncrementalTransform);
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
