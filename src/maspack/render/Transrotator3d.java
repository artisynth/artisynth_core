/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.util.LinkedList;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Line;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLViewer;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.DrawMode;
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

   // Special constrainer for testing and debugging only
   private class AlignConstrainer implements Dragger3dConstrainer {

      public boolean updateRotation (
         RotationMatrix3d RDWnew, RigidTransform3d TDW) {

         AxisAlignedRotation alignedRot =
            AxisAlignedRotation.getNearest (RDWnew);
         RDWnew.set (alignedRot.getMatrix());
         return !RDWnew.equals (TDW.R);
      }

      public boolean updatePosition (
         Vector3d pnew, RigidTransform3d TDW) {

         pnew.x = 2*Math.round(pnew.x/2);
         pnew.y = 2*Math.round(pnew.y/2);
         pnew.z = 2*Math.round(pnew.z/2);

         return !pnew.equals (TDW.p);
      }
   }

   protected Dragger3dConstrainer myConstrainer = null;

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

   private static final Line xAxis = new Line (0, 0, 0, 1, 0, 0);
   private static final Line yAxis = new Line (0, 0, 0, 0, 1, 0);
   private static final Line zAxis = new Line (0, 0, 0, 0, 0, 1);

   private static final Plane xyPlane = new Plane (0, 0, 1, 0);
   private static final Plane yzPlane = new Plane (1, 0, 0, 0);
   private static final Plane zxPlane = new Plane (0, 1, 0, 0);
   
   private static RenderObject renderObject = null;

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

   public Dragger3dConstrainer getConstrainer () {
      return myConstrainer;
   }

   public void setConstrainer (Dragger3dConstrainer c) {
      myConstrainer = c;
   }

   public void render (Renderer renderer, int flags) {
      
      if (!myVisibleP) {
         return;
      }
      
      Shading savedShading = renderer.setShading (Shading.NONE);
      renderer.setLineWidth(myLineWidth);
      
      ColorMixing savedMixing =
         renderer.setVertexColorMixing (ColorMixing.REPLACE);

      renderer.pushModelMatrix();
      renderer.mulModelMatrix(myXDraggerToWorld);

      float[] coords = new float[3];
      if (myDragMode != DragMode.OFF && mySelectedComponent != NONE) {
         renderer.setColor(1, 1, 0);
         renderer.setPointSize(3);
         myPnt0.get(coords);
         renderer.drawPoint(coords);
         renderer.setPointSize(1);
      }
      renderer.scaleModelMatrix(mySize);
      
      if (renderObject == null) {
         renderObject = createTransrotatorRenderable();
      }
      
      // select appropriate color buffer
      if (mySelectedComponent != 0) {
          renderer.drawLines(renderObject, mySelectedComponent);
      }
      renderer.drawLines(renderObject, 0);
      
      renderer.popModelMatrix();

      if (myDragMode != DragMode.OFF && 
         (mySelectedComponent == X_ROTATE ||
          mySelectedComponent == Y_ROTATE ||
          mySelectedComponent == Z_ROTATE)) {
         // Draw rotation lines using the orientation at the time the drag was
         // started
         RigidTransform3d X = new RigidTransform3d (myXDraggerToWorld0);
         X.p.set (myXDraggerToWorld.p);
         
         renderer.pushModelMatrix();
         renderer.mulModelMatrix(X);
         final float[] coords0 = new float[]{0,0,0};

         renderer.setColor(0.5f, 0.5f, 0.5f);
         myPnt0.get(coords);
         renderer.drawLine(coords0, coords);
         
         renderer.setColor(1,1,0);
         myRotPnt.get(coords);
         renderer.drawLine(coords0, coords);

         renderer.popModelMatrix();
      }

      renderer.setLineWidth(1);
      renderer.setShading (savedShading);
      renderer.setVertexColorMixing (savedMixing);
   }
   
   private static void addLineStrip (RenderObject robj, int pidx0, int numv) {
      robj.beginBuild (DrawMode.LINE_STRIP);
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

   private static RenderObject createTransrotatorRenderable() {
      
      final int QUARTER_CIRCLE_RESOLUTION = 32;
      final int FULL_CIRCLE_RESOLUTION = 4*QUARTER_CIRCLE_RESOLUTION;
      final float TRANS_BOX_SIZE = 0.4f;
      final float TRANS_ROT_SIZE = 0.8f;
      
      RenderObject robj = new RenderObject();

      int RED    = robj.addColor (1.0f, 0.0f, 0.0f, 1.0f);
      int GREEN  = robj.addColor (0.0f, 1.0f, 0.0f, 1.0f);
      int BLUE   = robj.addColor (0.0f, 0.0f, 1.0f, 1.0f);
      int GRAY   = robj.addColor (0.5f, 0.5f, 0.5f, 1.0f);
      int YELLOW = robj.addColor (1.0f, 1.0f, 0.0f, 1.0f);

      int p0     = robj.addPosition (0.0f, 0.0f, 0.0f);
      int px     = robj.addPosition (1.0f, 0.0f, 0.0f);
      int py     = robj.addPosition (0.0f, 1.0f, 0.0f);
      int pz     = robj.addPosition (0.0f, 0.0f, 1.0f);

      float size = TRANS_BOX_SIZE;
      
      robj.addPosition (0.0f, size, 0.0f);
      robj.addPosition (0.0f, size, size);
      robj.addPosition (0.0f, 0.0f, size);
      robj.addPosition (size, 0.0f, size);
      robj.addPosition (size, 0.0f, 0.0f);
      robj.addPosition (size, size, 0.0f);
      robj.addPosition (0.0f, size, 0.0f);

      int pbox = pz+1;

      size = TRANS_ROT_SIZE;

      // x rotation
      for (int i = 0; i <= QUARTER_CIRCLE_RESOLUTION; i++) {
         double ang = 2*Math.PI*i/(FULL_CIRCLE_RESOLUTION);
         robj.addPosition (
            0f, size*(float)Math.cos (ang), size*(float)Math.sin(ang));
      }
      int protx  = pbox+7;

      // y rotation
      for (int i = 0; i <= QUARTER_CIRCLE_RESOLUTION; i++) {
         double ang = 2*Math.PI*i/(FULL_CIRCLE_RESOLUTION);
         robj.addPosition (
            size*(float)Math.cos (ang), 0f, size*(float)Math.sin(ang));
      }
      int proty  = protx+QUARTER_CIRCLE_RESOLUTION+1;

      // z rotation
      for (int i = 0; i <= QUARTER_CIRCLE_RESOLUTION; i++) {
         double ang = 2*Math.PI*i/(FULL_CIRCLE_RESOLUTION);
         robj.addPosition (
            size*(float)Math.cos (ang), size*(float)Math.sin(ang), 0f);
      }
      int protz  = proty+QUARTER_CIRCLE_RESOLUTION+1;

      for (int i=0; i<10; i++) {
         robj.createLineGroup();
      }

      robj.lineGroup (0);

      robj.setCurrentColor (RED);
      addLine (robj, p0, px);
      robj.setCurrentColor (GREEN);
      addLine (robj, p0, py);
      robj.setCurrentColor (BLUE);
      addLine (robj, p0, pz);

      robj.setCurrentColor (GRAY);
      addLineStrip (robj, pbox, 7);

      robj.setCurrentColor (RED);
      addLineStrip (robj, protx, QUARTER_CIRCLE_RESOLUTION+1);

      robj.setCurrentColor (GREEN);
      addLineStrip (robj, proty, QUARTER_CIRCLE_RESOLUTION+1);

      robj.setCurrentColor (BLUE);
      addLineStrip (robj, protz, QUARTER_CIRCLE_RESOLUTION+1);

      robj.setCurrentColor (YELLOW);
      
      robj.lineGroup (X_AXIS);
      addLine (robj, p0, px);
      robj.lineGroup (Y_AXIS);
      addLine (robj, p0, py);
      robj.lineGroup (Z_AXIS);
      addLine (robj, p0, pz);

      robj.lineGroup (YZ_PLANE);
      addLineStrip (robj, pbox, 3);
      robj.lineGroup (ZX_PLANE);
      addLineStrip (robj, pbox+2, 3);
      robj.lineGroup (XY_PLANE);
      addLineStrip (robj, pbox+4, 3);

      robj.lineGroup (X_ROTATE);
      addLineStrip (robj, protx, QUARTER_CIRCLE_RESOLUTION+1);

      robj.lineGroup (Y_ROTATE);
      addLineStrip (robj, proty, QUARTER_CIRCLE_RESOLUTION+1);

      robj.lineGroup (Z_ROTATE);
      addLineStrip (robj, protz, QUARTER_CIRCLE_RESOLUTION+1);
   
      return robj;
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
      draggerToEye.mul (e.getViewer().getViewMatrix(), myXDraggerToWorld);

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

   /**
    * Given a ray intersecting one of the rotational dragger components, find
    * the intersection point p on the component (in dragger coordinates),
    * along with the corresponding rotation R.
    */
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
      if (myConstrainer != null) {
         Vector3d pnew = new Vector3d();
         pnew.transform (myXDraggerToWorld.R, del);
         pnew.add (myXDraggerToWorld.p);
         if (myConstrainer.updatePosition (pnew, myXDraggerToWorld)) {
            Tinc.p.sub (pnew, myXDraggerToWorld.p);
            Tinc.p.inverseTransform (myXDraggerToWorld.R);
            T.mulXyz (Tinc.p.x, Tinc.p.y, Tinc.p.z);
            myXDraggerToWorld.p.set (pnew);
         }
         else {
            Tinc.p.setZero();
         }
      }
      else {
         myXDraggerToWorld.mulXyz (del.x, del.y, del.z);
         Tinc.p.set (del.x, del.y, del.z);
         T.mul (Tinc);
      }
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
      if (myConstrainer != null) {
         RotationMatrix3d Rnew = new RotationMatrix3d();
         Rnew.mul (myXDraggerToWorld.R, Tinc.R);
         
         RigidTransform3d Tnew = new RigidTransform3d (myXDraggerToWorld);
         if (myConstrainer.updateRotation (Rnew, myXDraggerToWorld)) {
            Tinc.R.mulInverseLeft (myXDraggerToWorld.R, Rnew);
            myXDraggerToWorld.R.set (Rnew);
            T.R.mul (Tinc.R);
            myRotPnt.transform (T.R, myPnt0);
         }
         else {
            Tinc.R.setIdentity();
         }
      }
      else {
         T.R.set (R);
         myXDraggerToWorld.R.mul (Tinc.R);
      }
   }

//   public void draggerSelected (MouseRayEvent e) {
//      intersectRayAndFixture (myPnt0, e.getRay());
//   }

   public boolean mousePressed (MouseRayEvent e) {
      
      // DragMode mode = getDragMode (e);
      DragMode mode = getDragMode ();
      if (mode != DragMode.OFF && mySelectedComponent != NONE) {
         myDragMode = mode;
         if (mySelectedComponent == X_ROTATE ||
             mySelectedComponent == Y_ROTATE ||
             mySelectedComponent == Z_ROTATE) {
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
         if (mySelectedComponent == X_ROTATE ||
             mySelectedComponent == Y_ROTATE ||
             mySelectedComponent == Z_ROTATE) {
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
