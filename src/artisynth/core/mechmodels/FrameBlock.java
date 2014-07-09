/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.spatialmotion.*;

public class FrameBlock extends Matrix6dBlock {
   Frame myFrame;
   Matrix3d myRFR = new Matrix3d();
   Matrix3d myTmp = new Matrix3d();

   public FrameBlock (Frame frame) {
      super();
      set (frame);
   }

   public void set (Frame frame) {
      myFrame = frame;
   }

   public void addInertiaToBlock (SpatialInertia M) {
      double mass = M.getMass();
      Point3d com = M.getCenterOfMass();

      double mcx = mass * com.x;
      double mcy = mass * com.y;
      double mcz = mass * com.z;

      //      System.out.println ("add");

      m00 += mass;
      m11 += mass;
      m22 += mass;

      m04 += mcz;
      m05 -= mcy;
      m15 += mcx;
      m13 -= mcz;
      m23 += mcy;
      m24 -= mcx;

      m31 -= mcz;
      m32 += mcy;
      m42 -= mcx;
      m40 += mcz;
      m50 -= mcy;
      m51 += mcx;

      addSubMatrix33 (M.getOffsetRotationalInertia());
   }

   public void addFrameDamping (double dt, double dr) {

      m00 -= dt;
      m11 -= dt;
      m22 -= dt;
      m33 -= dr;
      m44 -= dr;
      m55 -= dr;
   }

   // public void addDamping (FrameDamper fdamper, double s)
   // {
   // addFrameDamping (s*fdamper.myDampingT, s*fdamper.myDampingR);
   // }

   // public void addDamping (PointDamper pdamper, Point3d pnt, double s)
   // {
   // addPointDamping (s*pdamper.myDamping, pnt);
   // }

   // public void addPointDamping (double d, Point3d pnt)
   // {
   // double dx = d*pnt.x;
   // double dy = d*pnt.y;
   // double dz = d*pnt.z;

   // M00.m00 -= d;
   // M00.m11 -= d;
   // M00.m22 -= d;

   // M01.m01 -= dz;
   // M01.m02 += dy;
   // M01.m12 -= dx;

   // M01.m10 += dz;
   // M01.m20 -= dy;
   // M01.m21 += dx;

   // M10.m01 += dz;
   // M10.m02 -= dy;
   // M10.m12 += dx;

   // M10.m10 -= dz;
   // M10.m20 += dy;
   // M10.m21 -= dx;
   // }

//    public void addBodyBodyJacobian (Matrix3d F, RotationMatrix3d R, Point3d p) {
//       myRFR.mul (F, R);
//       myRFR.mulTransposeLeft (R, myRFR);
//       M00.add (myRFR);
//       myTmp.crossProduct (myRFR, p);
//       M01.sub (myTmp);
//       myTmp.crossProduct (p, myRFR);
//       M10.add (myTmp);
//       myTmp.crossProduct (myTmp, p);
//       M11.sub (myTmp);
//    }

//    public void addBodyBodyJacobian (
//       Matrix3d F, RotationMatrix3d R1, Point3d p1, RotationMatrix3d R2,
//       Point3d p2) {
//       myRFR.mul (F, R2);
//       myRFR.mulTransposeLeft (R1, myRFR);
//       M00.add (myRFR);
//       myTmp.crossProduct (myRFR, p2);
//       M01.sub (myTmp);
//       myTmp.crossProduct (p1, myRFR);
//       M10.add (myTmp);
//       myTmp.crossProduct (myTmp, p2);
//       M11.sub (myTmp);
//    }

//    public void subBodyBodyJacobian (
//       Matrix3d F, RotationMatrix3d R1, Point3d p1, RotationMatrix3d R2,
//       Point3d p2) {
//       myRFR.mul (F, R2);
//       myRFR.mulTransposeLeft (R1, myRFR);
//       M00.sub (myRFR);
//       myTmp.crossProduct (myRFR, p2);
//       M01.add (myTmp);
//       myTmp.crossProduct (p1, myRFR);
//       M10.sub (myTmp);
//       myTmp.crossProduct (myTmp, p2);
//       M11.add (myTmp);
//    }

}
