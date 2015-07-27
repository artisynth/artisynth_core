/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.LinkedList;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.render.GL.GLViewer;
import maspack.render.RenderObject.BuildMode;
import maspack.util.InternalErrorException;

public class Transrotator3d extends Dragger3dBase {
   protected AffineTransform3dBase myTransform;
   protected AffineTransform3dBase myIncrementalTransform;
   protected int mySelectedComponent = NONE;
   protected Point3d myPnt0 = new Point3d();
   protected Point3d myRotPnt = new Point3d();
   protected int myNumCircleSides = 64;
   protected RotationMatrix3d myRot0 = new RotationMatrix3d();
   protected RigidTransform3d myXDraggerToWorld0 = new RigidTransform3d();

   //protected GLViewer myViewer; // hack to get repaint

   protected static final double myPlaneBoxRelativeSize = 0.4;
   protected static final double myRotatorRelativeSize = 0.8;

   static final int NONE = 0;

   static final int X_AXIS = 1;
   static final int Y_AXIS = 2;
   static final int Z_AXIS = 3;

   static final int YZ_PLANE = 4;
   static final int ZX_PLANE = 5;
   static final int XY_PLANE = 6;
   
   static final int X_ROTATE = 7;
   static final int Y_ROTATE = 8;
   static final int Z_ROTATE = 9;

   private static Line xAxis = new Line (0, 0, 0, 1, 0, 0);
   private static Line yAxis = new Line (0, 0, 0, 0, 1, 0);
   private static Line zAxis = new Line (0, 0, 0, 0, 0, 1);

   private static Plane xyPlane = new Plane (0, 0, 1, 0);
   private static Plane yzPlane = new Plane (1, 0, 0, 0);
   private static Plane zxPlane = new Plane (0, 1, 0, 0);

   public Transrotator3d() {
      super();
      myTransform = new RigidTransform3d();
      myIncrementalTransform = new RigidTransform3d();
   }

   public Transrotator3d (GLViewer viewer, double size) {
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
      
      viewer.setLightingEnabled (false);
      viewer.setLineWidth(myLineWidth);

      viewer.pushModelMatrix();
      viewer.mulModelMatrix(myXDraggerToWorld);

      float[] coords = new float[3];
      if (myDragMode != DragMode.OFF && mySelectedComponent != NONE) {
         viewer.setColor(1, 1, 0);
         viewer.setPointSize(3);
         myPnt0.get(coords);;
         viewer.drawPoint(coords);
         viewer.setPointSize(1);
      }
      viewer.scaleModelMatrix(mySize);
      
      RenderObject ro = viewer.getSharedObject(Transrotator3d.class);
      if (ro == null || !ro.isValid()) {
         ro = createTransrotatorRenderable();
         viewer.addSharedObject(Transrotator3d.class, ro);
      }
      
      // select appropriate color buffer
      ro.colorSet(mySelectedComponent);  
      viewer.drawLines(ro);
      
      viewer.popModelMatrix();


      if (myDragMode != DragMode.OFF && 
         (mySelectedComponent == X_ROTATE
         || mySelectedComponent == Y_ROTATE
         || mySelectedComponent == Z_ROTATE)) {
         // Draw rotation lines using the orientation at the time the drag was
         // started
         RigidTransform3d X = new RigidTransform3d (myXDraggerToWorld0);
         X.p.set (myXDraggerToWorld.p);
         
         viewer.pushModelMatrix();
         viewer.mulModelMatrix(X);
         final float[] coords0 = new float[]{0,0,0};

         viewer.setColor(0.5f, 0.5f, 0.5f);
         myPnt0.get(coords);
         viewer.drawLine(coords0, coords);
         
         viewer.setColor(1,1,0);
         myRotPnt.get(coords);
         viewer.drawLine(coords0, coords);

         viewer.popModelMatrix();
      }

      viewer.setLineWidth(1);
      viewer.setLightingEnabled (true);

   }
   
   private static RenderObject createTransrotatorRenderable() {
      
      final int QUARTER_CIRCLE_RESOLUTION = 32;
      final int FULL_CIRCLE_RESOLUTION = 4*QUARTER_CIRCLE_RESOLUTION;
      final float TRANS_BOX_SIZE = 0.4f;
      final float TRANS_ROT_SIZE = 0.8f;
      
      RenderObject transrotr = new RenderObject();
      
      // 3 axis, 3 plane boxes
      int xcolor  = transrotr.addColor(1.0f, 0.0f, 0.0f, 1.0f);
      int ycolor  = transrotr.addColor(0.0f, 1.0f, 0.0f, 1.0f);
      int zcolor  = transrotr.addColor(0.0f, 0.0f, 1.0f, 1.0f);
      int yzcolor = transrotr.addColor(0.5f, 0.5f, 0.5f, 1.0f);
      int zxcolor = transrotr.addColor(0.5f, 0.5f, 0.5f, 1.0f);
      int xycolor = transrotr.addColor(0.5f, 0.5f, 0.5f, 1.0f);
      int xrcolor = transrotr.addColor(1.0f, 0.0f, 0.0f, 1.0f);
      int yrcolor = transrotr.addColor(0.0f, 1.0f, 0.0f, 1.0f);
      int zrcolor = transrotr.addColor(0.0f, 0.0f, 1.0f, 1.0f);
      
      // 9 more color sets, one each with a component highlighted yellow
      int[] colors = new int[]{xcolor,  ycolor,  zcolor,
                               yzcolor, zxcolor, xycolor,
                               xrcolor, yrcolor, zrcolor};
      for (int i=0; i<colors.length; ++i) {
         transrotr.createColorSetFrom(0);                // duplicate original color set
         transrotr.setColor(i, 1.0f, 1.0f, 0.0f, 1.0f);  // replace ith color with yellow
      }
      
      transrotr.beginBuild(BuildMode.LINES);
      // x-axis
      transrotr.color(xcolor);
      transrotr.vertex(0, 0, 0);
      transrotr.vertex(1, 0, 0);
      // y-axis
      transrotr.color(ycolor);
      transrotr.vertex(0, 0, 0);
      transrotr.vertex(0, 1, 0);
      // z-axis
      transrotr.color(zcolor);
      transrotr.vertex(0, 0, 0);
      transrotr.vertex(0, 0, 1);
      transrotr.endBuild();
      
      // yz-plane
      int v0, v1, v2;
      transrotr.color(yzcolor);
      v0 = transrotr.vertex(0, TRANS_BOX_SIZE, 0);
      v1 = transrotr.vertex(0, TRANS_BOX_SIZE, TRANS_BOX_SIZE);
      v2 = transrotr.vertex(0, 0, TRANS_BOX_SIZE);
      transrotr.addLineStrip(v0, v1, v2);

      // zx-plane
      transrotr.color(zxcolor);
      v0 = transrotr.vertex(0, 0, TRANS_BOX_SIZE);
      v1 = transrotr.vertex(TRANS_BOX_SIZE, 0, TRANS_BOX_SIZE);
      v2 = transrotr.vertex(TRANS_BOX_SIZE, 0, 0);
      transrotr.addLineStrip(v0, v1, v2);
      
      // xy-plane
      transrotr.color(xycolor);
      v0 = transrotr.vertex(TRANS_BOX_SIZE, 0, 0);
      v1 = transrotr.vertex(TRANS_BOX_SIZE, TRANS_BOX_SIZE, 0);
      v2 = transrotr.vertex(0, TRANS_BOX_SIZE, 0);
      transrotr.addLineStrip(v0, v1, v2);
      
   // x-rotation
      transrotr.beginBuild(BuildMode.LINE_STRIP);
      transrotr.color(xrcolor);
      for (int i = 0; i <= QUARTER_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         transrotr.vertex(
            0f, TRANS_ROT_SIZE*(float)Math.cos (ang), TRANS_ROT_SIZE*(float)Math.sin (ang));
      }
      transrotr.endBuild();
            
      // y-rotation
      transrotr.beginBuild(BuildMode.LINE_STRIP);
      transrotr.color(yrcolor);
      for (int i = 0; i <= QUARTER_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         transrotr.vertex(
            TRANS_ROT_SIZE*(float)Math.cos (ang), 0f, TRANS_ROT_SIZE*(float)Math.sin (ang));
      }
      transrotr.endBuild();
            
      // z-rotation
      transrotr.beginBuild(BuildMode.LINE_STRIP);
      transrotr.color(zrcolor);
      for (int i = 0; i <= QUARTER_CIRCLE_RESOLUTION; i++) {
         double ang = 2 * Math.PI * i / (FULL_CIRCLE_RESOLUTION);
         transrotr.vertex( 
            TRANS_ROT_SIZE*(float)Math.cos (ang), TRANS_ROT_SIZE*(float)Math.sin (ang), 0f);
      }
      transrotr.endBuild();
   
      return transrotr;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }

   private boolean rotationSelectCheck (
      double d, double tempDist, double minDist, double lineDist) {
      return d != Double.POSITIVE_INFINITY && tempDist < lineDist &&
         tempDist < minDist;
   };

   private int checkComponentSelection (MouseRayEvent e) {
      double distancePerPixel = e.distancePerPixel (myXDraggerToWorld.p);

      Line draggerRay = new Line (e.getRay());
      draggerRay.inverseTransform (myXDraggerToWorld);

      double lineDist = 5 * distancePerPixel; // was 0.05*mySize
      double minDist = Double.POSITIVE_INFINITY;
      double l, d, tempDist;
      int resultAxisOrPlane = NONE;
      RigidTransform3d draggerToEye = new RigidTransform3d();
      draggerToEye.mulInverseLeft (
         e.getViewer().getEyeToWorld(), myXDraggerToWorld);

      Point3d p = new Point3d();

      // First find the axis the mouse is within the bounds of. When within
      // multiple bounds, select the axis that the mouse is closest to.

      l = xAxis.nearestPoint (p, draggerRay);
      tempDist = draggerRay.distance (p);
      if (l >= 0 && l <= mySize && tempDist < lineDist) {
         resultAxisOrPlane = X_AXIS;
         minDist = tempDist;
      }

      l = yAxis.nearestPoint (p, draggerRay);
      tempDist = draggerRay.distance (p);
      if (l >= 0 && l <= mySize && tempDist < lineDist && tempDist < minDist)

      {
         resultAxisOrPlane = Y_AXIS;
         minDist = tempDist;
      }

      l = zAxis.nearestPoint (p, draggerRay);
      tempDist = draggerRay.distance (p);
      if (l >= 0 && l <= mySize && tempDist < lineDist && tempDist < minDist) {
         resultAxisOrPlane = Z_AXIS;
         minDist = tempDist;
      }

      // now check rotators, and if there is any that are selected and
      // closer to the mouse than any of the axes, then select it.
      double len = myRotatorRelativeSize * mySize;

      d = draggerRay.intersectPlane (p, yzPlane);
      tempDist = Math.abs (p.norm() - len);
      if (rotationSelectCheck (d, tempDist, minDist, lineDist) &&
          p.y >= 0 && p.z >= 0) {
         resultAxisOrPlane = X_ROTATE;
         minDist = tempDist;
      }

      d = draggerRay.intersectPlane (p, zxPlane);
      tempDist = Math.abs (p.norm() - len);
      if (rotationSelectCheck (d, tempDist, minDist, lineDist) &&
          p.x >= 0 && p.z >= 0) {
         resultAxisOrPlane = Y_ROTATE;
         minDist = tempDist;
      }

      d = draggerRay.intersectPlane (p, xyPlane);
      tempDist = Math.abs (p.norm() - len);
      if (rotationSelectCheck (d, tempDist, minDist, lineDist) &&
          p.x >= 0 && p.y >= 0) {
         resultAxisOrPlane = Z_ROTATE;
         minDist = tempDist;
      }

      // if any of the axes or rotators are selected, then
      // return the axis or rotator that the mouse is closest to.
      if (resultAxisOrPlane != NONE) {
         return resultAxisOrPlane;
      }

      // now check if the mouse is on any of the planes. If true,
      // then return the selected plane closest to the user.
      len = myPlaneBoxRelativeSize * mySize;
      minDist = Double.POSITIVE_INFINITY;

      d = draggerRay.intersectPlane (p, xyPlane);
      if (d != Double.POSITIVE_INFINITY && p.x >= 0 &&
          p.x <= len && p.y >= 0 &&  p.y <= len) {
         p.transform (draggerToEye);
         resultAxisOrPlane = XY_PLANE;
         minDist = p.z;
      }

      d = draggerRay.intersectPlane (p, yzPlane);
      if (d != Double.POSITIVE_INFINITY && p.y >= 0 &&
          p.y <= len && p.z >= 0 && p.z <= len) {
         p.transform (draggerToEye);
         if (p.z < minDist) {
            resultAxisOrPlane = YZ_PLANE;
            minDist = p.z;
         }
      }

      d = draggerRay.intersectPlane (p, zxPlane);
      if (d != Double.POSITIVE_INFINITY && p.z >= 0 &&
          p.z <= len && p.x >= 0 && p.x <= len) {
         p.transform (draggerToEye);
         if (p.z < minDist) {
            resultAxisOrPlane = ZX_PLANE;
         }
      }

      return resultAxisOrPlane;
   }

   protected void intersectRayAndFixture (Point3d p, Line ray) {
      Line draggerRay = new Line (ray);
      draggerRay.inverseTransform (myXDraggerToWorld);

      switch (mySelectedComponent) {
         case X_AXIS: {
            xAxis.nearestPoint (p, draggerRay);
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

   protected void findRotation (RotationMatrix3d R, Point3d p, Line ray) {
      Line draggerRay = new Line (ray);
      draggerRay.inverseTransform (myXDraggerToWorld0);

      switch (mySelectedComponent) {
         case X_ROTATE: {
            draggerRay.intersectPlane (p, yzPlane);
            R.setAxisAngle (xAxis.getDirection(), Math.atan2 (p.z, p.y));
            break;
         }
         case Y_ROTATE: {
            draggerRay.intersectPlane (p, zxPlane);
            R.setAxisAngle (yAxis.getDirection(), Math.atan2 (p.x, p.z));
            break;
         }
         case Z_ROTATE: {
            draggerRay.intersectPlane (p, xyPlane);
            R.setAxisAngle (zAxis.getDirection(), Math.atan2 (p.y, p.x));
            break;
         }
         default: {
            throw new InternalErrorException (
               "unexpected case " + mySelectedComponent);
         }
      }
      double mag = p.norm();
      if (mag != 0) {
         p.scale (myRotatorRelativeSize * mySize / mag);
      }
   }

   protected void updatePosition (Point3d p0, Point3d p1, boolean constrained) {
      RigidTransform3d Tinc = (RigidTransform3d)myIncrementalTransform;
      RigidTransform3d T = (RigidTransform3d)myTransform;
      Vector3d del = new Vector3d();
      del.sub (p1, p0);
      if (constrained) {
         double s = getConstrainedStepSize();
         del.x = s*Math.round(del.x/s);
         del.y = s*Math.round(del.y/s);
         del.z = s*Math.round(del.z/s);
      }
      myXDraggerToWorld.mulXyz (del.x, del.y, del.z);
      Tinc.p.set (del.x, del.y, del.z);
      T.mul (Tinc);
   }

   protected void updateRotation (RotationMatrix3d Rot, boolean constrained) {
      RigidTransform3d Tinc = (RigidTransform3d)myIncrementalTransform;
      RigidTransform3d T = (RigidTransform3d)myTransform;
      RotationMatrix3d R = new RotationMatrix3d();
      R.mulInverseLeft (myRot0, Rot);
      if (constrained) {
         AxisAngle axisAng = new AxisAngle();
         R.getAxisAngle (axisAng);
         double deg = Math.toDegrees (axisAng.angle);
         axisAng.angle = Math.toRadians (5*Math.round(deg/5));
         R.setAxisAngle (axisAng);
         myRotPnt.transform (R, myPnt0);
      }
      Tinc.R.mulInverseLeft (T.R, R);
      T.R.set (R);
      myXDraggerToWorld.R.mul (Tinc.R);
   }

//   public void draggerSelected (MouseRayEvent e) {
//      intersectRayAndFixture (myPnt0, e.getRay());
//   }

   public boolean mousePressed (MouseRayEvent e) {
      
      // DragMode mode = getDragMode (e);
      DragMode mode = getDragMode ();
      if (mode != DragMode.OFF && mySelectedComponent != NONE) {
         myDragMode = mode;
         if (mySelectedComponent == X_ROTATE
           || mySelectedComponent == Y_ROTATE
           || mySelectedComponent == Z_ROTATE) {
            myXDraggerToWorld0.set (myXDraggerToWorld);
            findRotation (myRot0, myPnt0, e.getRay());
            myRotPnt.set (myPnt0);
         }
         else {
            intersectRayAndFixture (myPnt0, e.getRay());
         }
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
         if (mySelectedComponent != NONE) {
            myIncrementalTransform.setIdentity();
            fireDraggerEndListeners (
               myTransform, myIncrementalTransform, e.getModifiersEx());
         }
         myDragMode = DragMode.OFF;
         clearFlags();
         return true;
      }
      return false;
   }

   public boolean mouseDragged (MouseRayEvent e) {
      if (mySelectedComponent != NONE) {
         // boolean constrained = dragIsConstrained (e);
         boolean constrained = dragIsConstrained ();
         if (mySelectedComponent == X_ROTATE
         || mySelectedComponent == Y_ROTATE
         || mySelectedComponent == Z_ROTATE) {
            RotationMatrix3d R = new RotationMatrix3d();
            findRotation (R, myRotPnt, e.getRay());
            updateRotation (R, constrained);
         }
         else {
            Point3d pnt = new Point3d();
            intersectRayAndFixture (pnt, e.getRay());
            updatePosition (myPnt0, pnt, constrained);
         }
         //if (!dragIsRepositioning(e)) {
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
