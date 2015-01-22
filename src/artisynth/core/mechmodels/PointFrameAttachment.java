/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.spatialmotion.SpatialInertia;

import java.io.*;
import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class PointFrameAttachment extends PointAttachment {
   DynamicComponent[] myMasters;
   Point3d myLoc = new Point3d(); // location of point in frame coordinates
   Frame myFrame;
   Matrix3d myTmp = new Matrix3d();
   Vector3d myVec = new Vector3d();
   MatrixBlock myGT = null;

   public DynamicComponent[] getMasters() {
      return myMasters;
   }

   public Frame getFrame() {
      return myFrame;
   }

   public int numMasters() {
      return myMasters.length;
   }

   public void setLocation (Point3d pnt) {
      myLoc.set (pnt);
   }

   // public void setDefaultLocation() {
   //    myLoc.inverseTransform (
   //       myFrame.myState.XFrameToWorld, myPoint.myState.pos);
   // }

   public Point3d getLocation() {
      return myLoc;
   }

   protected void updateJacobian() {
      if (myFrame != null) {
         if (myGT == null) {
            if (myFrame.getVelStateSize() == 6) {
               myGT = new Matrix6x3Block();
            }
            else {
               myGT = new MatrixNdBlock(myFrame.getVelStateSize(), 3);
            }
         }
         myFrame.computePointForceJacobian (myGT, myLoc);
      }
   }

   void setFrame (Frame body) {
      myFrame = body;
      if (body instanceof DynamicComponent) {
         myMasters = new DynamicComponent[1];
         myMasters[0] = (DynamicComponent)myFrame;
      }
      else {
         myMasters = new DynamicComponent[0];
      }
      updateJacobian();
   }
   
   void setSlave (Point slave, Point3d loc) {
      myPoint = slave;
      if (loc != null) {
         myLoc.set (loc);
      }
      else {
         myFrame.computePointLocation (myLoc, myPoint.myState.pos);
      }
   }

   public void detachSlave() {
   }

   public PointFrameAttachment() {
      setFrame (null);
   }

   public void computePosState (Vector3d pos) {
      myFrame.computePointPosition (pos, myLoc);
   }

   public void updatePosStates() {
      computePosState (myPoint.myState.pos);
      updateJacobian();
   }

   public void updateVelStates() { 
      myFrame.computePointVelocity (myPoint.myState.vel, myLoc);
   }

   public void applyForces() {
      myFrame.addPointForce (myLoc, myPoint.myForce);
   }



//   // FIX
//   public void addScaledExternalForce (Point3d pnt, double s, Vector3d f) {
//      Wrench bodyForce = new Wrench();
//      Point3d bodyPnt = new Point3d(pnt);
//      bodyPnt.inverseTransform(myFrame.getPose());
//      myFrame.computeAppliedWrench (bodyForce, f, bodyPnt);
//      bodyForce.transform (myFrame.getPose().R);
//      myFrame.addScaledExternalForce (s, bodyForce);
//   }
   
   protected MatrixBlock createRowBlock (int colSize) {
      return createRowBlockNew (colSize);
   }

   protected MatrixBlock createColBlock (int rowSize) {
      return createColBlockNew (rowSize);
   }

   protected MatrixBlock createColBlockNew (int rowSize) {
      int vsize = myFrame.getVelStateSize();
      if (vsize == 6) {
         switch (rowSize) {
            case 1:
               return new Matrix1x6Block();
            case 3:
               return new Matrix3x6Block();
            case 6: 
               return new Matrix6dBlock();
            default:
               return new MatrixNdBlock(rowSize, 6);
         }
      }
      else {
         return new MatrixNdBlock (rowSize, vsize);
      }
   }

   protected MatrixBlock createRowBlockNew (int colSize) {
      int vsize = myFrame.getVelStateSize();
      if (vsize == 6) {
         switch (colSize) {
            case 1:
               return new Matrix6x1Block();
            case 3:
               return new Matrix6x3Block();
            case 6: 
               return new Matrix6dBlock();
            default:
               return new MatrixNdBlock(6, colSize);
         }
      }
      else {
         return new MatrixNdBlock(vsize, colSize);
      }
   }

   public void mulSubGT (MatrixBlock D, MatrixBlock B, int idx) {
      MatrixNdBlock Dcheck = new MatrixNdBlock (D.rowSize(), D.colSize());
      // Dcheck.set (D);
      // Dcheck.mulAdd (myGT, B);
      // mulSubGTNew (D, B, idx);
      // if (!Dcheck.epsilonEquals (D, 1e-10)) {
      //    System.out.println ("D=\n" + D);
      //    System.out.println ("Dcheck=\n" + Dcheck);
      // }
      D.mulAdd (myGT, B);
   }

   public void mulSubG (MatrixBlock D, MatrixBlock B, int idx) {
      if (myGT != null) {
         MatrixBlock G = myGT.createTranspose();
         D.mulAdd (B, G);
      }
      //mulSubGNew (D, B, idx);
   }

   // FIX: use getPointJacobian()? mulPointJacobian()?
   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {

      if (myGT != null) {
         myGT.mulAdd (ybuf, yoff, xbuf, xoff);
      }

      // RotationMatrix3d R = myFrame.myState.XFrameToWorld.R;

      // double bx = xbuf[xoff  ];
      // double by = xbuf[xoff+1];
      // double bz = xbuf[xoff+2];

      // double tx, ty, tz;

      // if (Frame.dynamicVelInWorldCoords) {
      //    myVec.transform (R, myLoc);
      //    tx = bx;
      //    ty = by;
      //    tz = bz;
      // }
      // else {
      //    myVec.set (myLoc);
      //    tx = bx*R.m00 + by*R.m10 + bz*R.m20;
      //    ty = bx*R.m01 + by*R.m11 + bz*R.m21;
      //    tz = bx*R.m02 + by*R.m12 + bz*R.m22;
      // }
      // double xx = myVec.y*tz - myVec.z*ty;
      // double xy = myVec.z*tx - myVec.x*tz;
      // double xz = myVec.x*ty - myVec.y*tx;

      // ybuf[yoff  ] += tx;
      // ybuf[yoff+1] += ty;
      // ybuf[yoff+2] += tz;
      // ybuf[yoff+3] += xx;
      // ybuf[yoff+4] += xy;
      // ybuf[yoff+5] += xz;
   }

   // FIX: use getPointJacobian()? mulPointJacobian()?
   public void mulSubGTNew (MatrixBlock D, MatrixBlock B, int idx) {

      RotationMatrix3d R = myFrame.myState.XFrameToWorld.R;
      Matrix3d TM = myTmp;

      if (Frame.dynamicVelInWorldCoords) {
         myVec.transform (R, myLoc);
      }
      else {
         myVec.set (myLoc);
      }

      if (D instanceof MatrixNdBlock) {
         MatrixNd DM = (MatrixNd)D; 
         double[] Abuf = DM.getBuffer();
         int w = DM.getBufferWidth();

         for (int j=0; j<DM.colSize(); j++) {
            double bx = B.get (0, j);
            double by = B.get (1, j);
            double bz = B.get (2, j);

            double tx, ty, tz;

            if (Frame.dynamicVelInWorldCoords) {
               tx = bx;
               ty = by;
               tz = bz;
            }
            else {
               tx = bx*R.m00 + by*R.m10 + bz*R.m20;
               ty = bx*R.m01 + by*R.m11 + bz*R.m21;
               tz = bx*R.m02 + by*R.m12 + bz*R.m22;
            }
            double xx = myVec.y*tz - myVec.z*ty;
            double xy = myVec.z*tx - myVec.x*tz;
            double xz = myVec.x*ty - myVec.y*tx;

            Abuf[    j] += tx; Abuf[  w+j] += ty; Abuf[2*w+j] += tz;
            Abuf[3*w+j] += xx; Abuf[4*w+j] += xy; Abuf[5*w+j] += xz;
         }
      }
      else {
         switch (B.colSize()) {
            case 1: {
               Matrix6x1 DM = (Matrix6x1)D;
               Matrix3x1 BM = (Matrix3x1)B;
               double bx = BM.m00;
               double by = BM.m10;
               double bz = BM.m20;

               double tx, ty, tz;

               if (Frame.dynamicVelInWorldCoords) {
                  tx = bx;
                  ty = by;
                  tz = bz;
               }
               else {
                  tx = bx*R.m00 + by*R.m10 + bz*R.m20;
                  ty = bx*R.m01 + by*R.m11 + bz*R.m21;
                  tz = bx*R.m02 + by*R.m12 + bz*R.m22;
               }               
               double xx = myVec.y*tz - myVec.z*ty;
               double xy = myVec.z*tx - myVec.x*tz;
               double xz = myVec.x*ty - myVec.y*tx;

               DM.m00 += tx; DM.m10 += ty; DM.m20 += tz;
               DM.m30 += xx; DM.m40 += xy; DM.m50 += xz;
               break;
            }
            case 3: {
               Matrix6x3 DM = (Matrix6x3)D;
               Matrix3x3Block BM = (Matrix3x3Block)B;

               if (Frame.dynamicVelInWorldCoords) {
                  TM.set (BM);
               }
               else {
                  TM.mulTransposeLeft (R, BM);
               }
               DM.addSubMatrix00 (TM);
               TM.crossProduct (myVec, TM);
               DM.addSubMatrix30 (TM);
               break;
            }
            case 6: {
               Matrix6d DM = (Matrix6d)D;
               Matrix3x6 BM = (Matrix3x6)B;
               
               BM.getSubMatrix00 (TM);
               if (!Frame.dynamicVelInWorldCoords) {
                  TM.mulTransposeLeft (R, TM);
               }
               DM.addSubMatrix00 (TM);
               TM.crossProduct (myVec, TM);
               DM.addSubMatrix30 (TM);

               BM.getSubMatrix03 (TM);
               if (!Frame.dynamicVelInWorldCoords) {
                  TM.mulTransposeLeft (R, TM);
               }
               DM.addSubMatrix03 (TM);
               TM.crossProduct (myVec, TM);
               DM.addSubMatrix33 (TM);
               break;
            }
            default:
               throw new UnsupportedOperationException (
                  "Not implemented for D of type "+D.getClass());
         }
      }
      //      System.out.println ("R=\n" + ((MatrixBase)D).toString ("%8.3f"));
   }

   // FIX: use getPointJacobian()? mulPointJacobian()?
   public void mulSubGNew (MatrixBlock D, MatrixBlock B, int idx) {

      RotationMatrix3d R = myFrame.myState.XFrameToWorld.R;
      Matrix3d TM = myTmp;

      if (Frame.dynamicVelInWorldCoords) {
         myVec.transform (R, myLoc);
      }
      else {
         myVec.set (myLoc);
      }

      if (D instanceof MatrixNdBlock) {
         MatrixNd DM = (MatrixNd)D; 
         double[] Abuf = DM.getBuffer();
         int w = DM.getBufferWidth();

         for (int i=0; i<DM.rowSize(); i++) {
            double bx = B.get (i, 0);
            double by = B.get (i, 1);
            double bz = B.get (i, 2);

            double tx, ty, tz;

            if (Frame.dynamicVelInWorldCoords) {
               tx = bx;
               ty = by;
               tz = bz;
            }
            else {
               tx = bx*R.m00 + by*R.m10 + bz*R.m20;
               ty = bx*R.m01 + by*R.m11 + bz*R.m21;
               tz = bx*R.m02 + by*R.m12 + bz*R.m22;
            }
            double xx = myVec.y*tz - myVec.z*ty;
            double xy = myVec.z*tx - myVec.x*tz;
            double xz = myVec.x*ty - myVec.y*tx;

            Abuf[i*w+0] += tx; Abuf[i*w+1] += ty; Abuf[i*w+2] += tz;
            Abuf[i*w+3] += xx; Abuf[i*w+4] += xy; Abuf[i*w+5] += xz;
         }
      }
      else {
         switch (B.rowSize()) {
            case 1: {
               Matrix1x6 DM = (Matrix1x6)D;
               Matrix1x3 BM = (Matrix1x3)B;
               double bx = BM.m00;
               double by = BM.m01;
               double bz = BM.m02;

               double tx, ty, tz;

               if (Frame.dynamicVelInWorldCoords) {
                  tx = bx;
                  ty = by;
                  tz = bz;
               }
               else {
                  tx = bx*R.m00 + by*R.m10 + bz*R.m20;
                  ty = bx*R.m01 + by*R.m11 + bz*R.m21;
                  tz = bx*R.m02 + by*R.m12 + bz*R.m22;
               }
               double xx = myVec.y*tz - myVec.z*ty;
               double xy = myVec.z*tx - myVec.x*tz;
               double xz = myVec.x*ty - myVec.y*tx;

               DM.m00 += tx; DM.m01 += ty; DM.m02 += tz;
               DM.m03 += xx; DM.m04 += xy; DM.m05 += xz;
               break;
            }
            case 3: {
               Matrix3x6 DM = (Matrix3x6)D;
               Matrix3x3Block BM = (Matrix3x3Block)B;

               if (Frame.dynamicVelInWorldCoords) {
                  TM.set (BM);
               }
               else {
                  TM.mul (BM, R);
               }
               DM.addSubMatrix00 (TM);
               TM.crossProduct (TM, myVec);
               TM.negate();
               DM.addSubMatrix03 (TM);
               break;
            }
            case 6: {
               Matrix6d DM = (Matrix6d)D;
               Matrix6x3 BM = (Matrix6x3)B;

               BM.getSubMatrix00 (TM);
               if (!Frame.dynamicVelInWorldCoords) {
                  TM.mul (R);
               }
               DM.addSubMatrix00 (TM);
               TM.crossProduct (TM, myVec);
               TM.negate();
               DM.addSubMatrix03 (TM);

               BM.getSubMatrix30 (TM);
               if (!Frame.dynamicVelInWorldCoords) {
                  TM.mul (R);
               }
               DM.addSubMatrix30 (TM);
               TM.crossProduct (TM, myVec);
               TM.negate();
               DM.addSubMatrix33 (TM);
               break;
            }
            default:
               throw new UnsupportedOperationException (
                  "Not implemented for D of type "+D.getClass());
         }
      }
      //      System.out.println ("R=\n" + ((MatrixBase)D).toString ("%8.3f"));
   }

   // FIX: use computePointLocation?
   public void updateAttachment() {
      myFrame.computePointLocation (myLoc, myPoint.myState.pos);
      // myLoc.inverseTransform (
      //    myFrame.myState.XFrameToWorld, myPoint.myState.pos);
      if (myPoint instanceof FrameMarker) {
         ((FrameMarker)myPoint).myLocation.set (myLoc);
      }
   }

   @Override
   public void transformSlaveGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      myPoint.transformGeometry (X, topObject, flags);
      if (myPoint instanceof FrameMarker) {
         // this is a bit of a hack. We call the FrameMarker method
         // so as to also update its local copy of myLocation and myRefPos
         ((FrameMarker)myPoint).updateAttachment();
      }
      else {
         updateAttachment();
      }
   }

   // FIX: use getPointJacobian?
   public int addTargetJacobian (SparseBlockMatrix J, int bi) { // FIX
      MatrixBlock blk = myGT.createTranspose();
      //Matrix3x6Block blk = new Matrix3x6Block();
      //setFrameJacobian (blk);
      J.addBlock (bi, myFrame.getSolveIndex(), blk);
      return bi++;      
   }

   // FIX: replace with getPointJacobian?
   /** 
    * Frame Jacobian is set to
    * <pre>
    *   [                      ]
    *   [  RBW     -RBW [loc]  ]
    *   [                      ]
    * </pre>
    */
   private void setFrameJacobian (MatrixBlock blk) {

      if (myGT != null) {
         int vsize = myGT.rowSize();
         if (vsize > 6) {
            MatrixNdBlock G = null;
            if (blk instanceof MatrixNdBlock) {
               G = (MatrixNdBlock)blk;
            }
            if (blk == null || blk.rowSize() != 3 || blk.colSize() != vsize) {
               throw new IllegalArgumentException (
                  "Expecting blk to be a MatrixNdBlock of size 3x"+vsize);
            }
            G.transpose ((MatrixNdBlock)myGT);
         }
         else {
            if (!(blk instanceof Matrix3x6Block)) {
               throw new IllegalArgumentException (
                  "Expecting blk to be a Matrix3x6Block");
            }
            Matrix3x6Block G = (Matrix3x6Block)blk;
            G.transpose ((Matrix6x3Block)myGT);
         }
      }
      else {
         blk.setZero();
      }
      
      // RotationMatrix3d R = myFrame.getPose().R;

      // if (Frame.dynamicVelInWorldCoords) {
      //    blk.setZero();
      //    blk.m00 = 1;
      //    blk.m11 = 1;
      //    blk.m22 = 1;

      //    double lxb = myLoc.x;
      //    double lyb = myLoc.y;
      //    double lzb = myLoc.z;
         
      //    double lxw = R.m00*lxb + R.m01*lyb + R.m02*lzb;
      //    double lyw = R.m10*lxb + R.m11*lyb + R.m12*lzb;
      //    double lzw = R.m20*lxb + R.m21*lyb + R.m22*lzb;

      //    blk.m04 =  lzw;
      //    blk.m05 = -lyw;
      //    blk.m15 =  lxw;

      //    blk.m13 = -lzw;
      //    blk.m23 =  lyw;
      //    blk.m24 = -lxw;
      // }
      // else {
      //    blk.m00 = R.m00; blk.m01 = R.m01; blk.m02 = R.m02;
      //    blk.m10 = R.m10; blk.m11 = R.m11; blk.m12 = R.m12;
      //    blk.m20 = R.m20; blk.m21 = R.m21; blk.m22 = R.m22;

      //    double lxb = myLoc.x;
      //    double lyb = myLoc.y;
      //    double lzb = myLoc.z;
         
      //    blk.m03 = -lzb*R.m01 + lyb*R.m02;
      //    blk.m13 = -lzb*R.m11 + lyb*R.m12;
      //    blk.m23 = -lzb*R.m21 + lyb*R.m22;

      //    blk.m04 =  lzb*R.m00 - lxb*R.m02;
      //    blk.m14 =  lzb*R.m10 - lxb*R.m12;
      //    blk.m24 =  lzb*R.m20 - lxb*R.m22;

      //    blk.m05 = -lyb*R.m00 + lxb*R.m01;
      //    blk.m15 = -lyb*R.m10 + lxb*R.m11;
      //    blk.m25 = -lyb*R.m20 + lxb*R.m21;
      // }
   }


   // FIX: add addPointMass to Frame?
   public void addMassToMaster (MatrixBlock mblk, MatrixBlock sblk, int idx) {
      
      if (idx != 0) {
         throw new IllegalArgumentException ("Master idx="+idx+"; can only be 0");
      }
      if (!(sblk instanceof Matrix3x3Block)) {
         throw new IllegalArgumentException (
            "Slave block not instance of Matrix3x3Block");
      }
      Matrix3x3Block slaveBlk = (Matrix3x3Block)sblk;
      double mass = slaveBlk.m00;
      if (mass != 0) {
         myFrame.computePointPosition (myVec, myLoc);
         // sub Frame position since vec only wanted in world orientation 
         myVec.sub (myFrame.getPosition());
         myFrame.addPointMass (mblk, mass, myVec);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "frame", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "loc")) {
         myLoc.scan (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }


   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "frame")) {
         setFrame (postscanReference (
            tokens, Frame.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("frame=");
      pw.println (ComponentUtils.getWritePathName (
                     ancestor, myFrame));
      pw.print ("loc=");
      myLoc.write (pw, fmt);
      pw.println ("");
   }

   // FIX
   public boolean getDerivative (double[] buf, int idx) {

      myFrame.computePointCoriolis (myVec, myLoc);

      // Twist tw = myFrame.getVelocity();

      // myVec.transform (myFrame.getPose().R, myLoc);
      // myVec.cross (tw.w, myVec);
      // // do we need to add this?
      // //myVec.add (tw.v);
      // myVec.cross (tw.w, myVec);

      buf[idx  ] = myVec.x;
      buf[idx+1] = myVec.y;
      buf[idx+2] = myVec.z;
      return true;
   }

   public PointFrameAttachment (Frame master, Point slave) {
      this();
      setFrame (master);
      setSlave (slave, null);
   }

   public PointFrameAttachment (Frame master, Point slave, Point3d loc) {
      this();
      setFrame (master);
      setSlave (slave, loc);
   }

   @Override
   public void connectToHierarchy () {
      Point point = getPoint();
      Frame frame = getFrame();
      if (point == null || frame == null) {
         throw new InternalErrorException ("null point and/or frame");
      }
      super.connectToHierarchy ();
      point.setAttached (this);
      frame.addMasterAttachment (this);
   }

   @Override
   public void disconnectFromHierarchy() {
      Point point = getPoint();
      Frame frame = getFrame();
      if (point == null || frame == null) {
         throw new InternalErrorException ("null point and/or frame");
      }
      super.disconnectFromHierarchy();
      point.setAttached (null);
      frame.removeMasterAttachment (this);
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      Point point = getPoint();
      Frame frame = getFrame();
      if (point == null || frame == null) {
         throw new InternalErrorException ("null point and/or frame");
      }
      super.getHardReferences (refs);
      refs.add (point);
      refs.add (frame);
   }

   public PointFrameAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointFrameAttachment a = (PointFrameAttachment)super.copy (flags, copyMap);

      a.myMasters = null;
      a.myTmp = new Matrix3d();
      a.myVec = new Vector3d();
      a.myLoc = new Point3d (myLoc);
      if (myFrame != null) {
         a.setFrame ((Frame)ComponentUtils.maybeCopy (flags, copyMap, myFrame));
      }
      return a;
   }
}
