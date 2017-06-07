/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.render.GL.GLViewer;

public class RotatableScaler3d extends Transrotator3d {

   RotationMatrix3d myLastR = new RotationMatrix3d();

   public RotatableScaler3d() {
      super();
      myTransform = new AffineTransform3d();
      myIncrementalTransform = new AffineTransform3d();
   }

   public RotatableScaler3d (GLViewer viewer, double size) {
      this();
      setSize (size);
      //myViewer = viewer;
   }

   private void updatePosition (
      Point3d p1, boolean constrained, boolean repositioning) {

      if (repositioning) {
         Vector3d del = new Vector3d();
         del.sub (p1, myPnt0);
         if (constrained) {
            double s = getConstrainedStepSize();
            del.x = s*Math.round(del.x/s);
            del.y = s*Math.round(del.y/s);
            del.z = s*Math.round(del.z/s);
         }
         myXDraggerToWorld.mulXyz (del.x, del.y, del.z);
      }
      else {
         AffineTransform3d Tinc = (AffineTransform3d)myIncrementalTransform;
         AffineTransform3d T = (AffineTransform3d)myTransform;
         Vector3d d = new Vector3d(), o = new Vector3d();
         o.sub (myPnt0, T.p);

         if (constrained) {
            d.sub (p1, myPnt0);
            double s = getConstrainedStepSize();
            d.x = s*Math.round(d.x/s);
            d.y = s*Math.round(d.y/s);
            d.z = s*Math.round(d.z/s);
            d.add (o);
         }
         else {
            d.sub (p1, T.p);
         }

         double x, y, z;
         x = d.x == 0 ? 1 : (o.x == 0 ? 1e-10 : Math.abs (d.x / o.x));
         y = d.y == 0 ? 1 : (o.y == 0 ? 1e-10 : Math.abs (d.y / o.y));
         z = d.z == 0 ? 1 : (o.z == 0 ? 1e-10 : Math.abs (d.z / o.z));
//         x = d.x == 0 ? 1 : (o.x == 0 ? 1e-10 : d.x / o.x);
//         y = d.y == 0 ? 1 : (o.y == 0 ? 1e-10 : d.y / o.y);
//         z = d.z == 0 ? 1 : (o.z == 0 ? 1e-10 : d.z / o.z);

         Tinc.set (T);
         T.setIdentity();
         T.applyScaling (x, y, z);
         Tinc.mulInverseLeft (Tinc, T);
      }
   }

   protected void updateRotation (
      RotationMatrix3d Rot, boolean constrained, boolean repositioning) {
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
      RotationMatrix3d Rinc = new RotationMatrix3d();
      Rinc.mulInverseLeft (myLastR, R);
      myLastR.set (R);
      if (repositioning) {
         myXDraggerToWorld.R.mul (Rinc);
      }
   }

   public boolean mousePressed (MouseRayEvent e) {
      if (super.mousePressed (e)) {
         myLastR.setIdentity();
         return true;
      }
      else {
         return false;
      }
   }

   public boolean mouseDragged (MouseRayEvent e) {
      
      if (mySelectedComponent != NONE) {
         //         boolean constrained = dragIsConstrained (e);
         //         boolean repositioning = dragIsRepositioning (e);
         
         boolean constrained = dragIsConstrained ();
         boolean repositioning = dragIsRepositioning ();
         if (mySelectedComponent == X_ROTATE
            || mySelectedComponent == Y_ROTATE
            || mySelectedComponent == Z_ROTATE) {
            RotationMatrix3d R = new RotationMatrix3d();
            findRotation (R, myRotPnt, e.getRay());
            updateRotation (R, constrained, repositioning);
         }
         else {
            Point3d pnt = new Point3d();
            intersectRayAndFixture (pnt, e.getRay());
            updatePosition (pnt, constrained, repositioning);
         }
         if (!repositioning) {
            fireDraggerMoveListeners (
               myTransform, myIncrementalTransform, e.getModifiersEx());
         }
         return true;
      }
      
      return false;
   }

}
