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
//   DynamicComponent[] myMasters;
   Point3d myLoc = new Point3d(); // location of point in frame coordinates
   Frame myFrame;

   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters (masters);
      masters.add (myFrame);
   }
   
   @Override
   protected int updateMasterBlocks() {
      int idx = super.updateMasterBlocks();
      if (myFrame != null) {
         RotationMatrix3d R = null;
         if (idx == 1) {
            // then point has a frame; set R to the rotation matrix for that frame
            R = myPoint.getPointFrame().getPose().R;
         }
         myFrame.computeLocalPointForceJacobian (
            myMasterBlocks[idx++], myLoc, R);
      }
      return idx;
   }
   
   public Frame getFrame() {
      return myFrame;
   }

   public void setLocation (Point3d loc) {
      myLoc.set (loc);
   }

   public Point3d getLocation() {
      return myLoc;
   }

   // protected void updateJacobian() {
   //    if (myFrame != null) {
   //       if (myGT == null) {
   //          if (myFrame.getVelStateSize() == 6) {
   //             myGT = new Matrix6x3Block();
   //          }
   //          else {
   //             myGT = new MatrixNdBlock(myFrame.getVelStateSize(), 3);
   //          }
   //       }
   //       myFrame.computeWorldPointForceJacobian (myGT, myLoc);
   //    }
   // }

   void setFrame (Frame body, Point3d loc) {
      removeBackRefsIfConnected();
      myFrame = body;
      if (loc != null) {
         myLoc.set (loc);
      }
      else {
         myFrame.computePointLocation (myLoc, myPoint.getPosition());
      }     
      //updateJacobian();
      invalidateMasters();
      addBackRefsIfConnected();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }
   
   void setFrame (Frame body) {
      setFrame (body, null);
   }
   
   void setSlave (Point slave) {
      myPoint = slave;
   }

   public void detachSlave() {
   }


   public void getCurrentPos (Vector3d pos) {
      myFrame.computePointPosition (pos, myLoc);
   }
   
   public void getUndeformedPos (Vector3d pos) {
      Point3d pnt = new Point3d();
      pnt.transform (myFrame.getPose(), myLoc);
      pos.set (pnt);
   }
   
   public void setCurrentPos (Vector3d pos) {
      myFrame.computePointLocation (myLoc, pos);
   }

   public void getCurrentVel (Vector3d vel, Vector3d dvel) {
      myFrame.computePointVelocity (vel, myLoc);
      if (dvel != null) {
         
      }
   }

   public void updatePosStates() {
      Point3d pntw = new Point3d();
      getCurrentPos (pntw);
      myPoint.setPosition (pntw);
      //updateJacobian();
      updateMasterBlocks();
   }

   public void updateVelStates() {
      Vector3d velw = new Vector3d();
      myFrame.computePointVelocity (velw, myLoc);
      myPoint.setVelocity (velw);
   }

   public void applyForces() {
      super.applyForces();
      myFrame.addPointForce (myLoc, myPoint.myForce);
   }

   public void mulSubGT (MatrixBlock D, MatrixBlock B, int idx) {
      D.mulAdd (myMasterBlocks[idx], B);
   }

   public void mulSubG (MatrixBlock D, MatrixBlock B, int idx) {
      if (myMasterBlocks != null) {
         MatrixBlock G = myMasterBlocks[idx].createTranspose();
         D.mulAdd (B, G);
      }         
   }

   // FIX: use getPointJacobian()? mulPointJacobian()?
   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {

      if (myMasterBlocks != null) {
         myMasterBlocks[idx].mulAdd (ybuf, yoff, xbuf, xoff);
      }   
   }


   public void scale (double s) {
      myLoc.scale (s);
   }
   
   // FIX: use computePointLocation?
   public void updateAttachment() {
      myFrame.computePointLocation (myLoc, myPoint.getPosition());
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
      if (myMasterBlocks.length != 1) {
         throw new UnsupportedOperationException (
            "addTargetJacobian not supported for frame-relative points");
      }
      MatrixBlock blk = myMasterBlocks[0].createTranspose();
      J.addBlock (bi, myFrame.getSolveIndex(), blk);
      return bi++;      
   }

   public void addMassToMasters() {
      double m = myPoint.getEffectiveMass();
      if (m != 0) {
         myFrame.addEffectivePointMass (m, myLoc);
      } 
      myPoint.addEffectiveMass(-m);      
   }
   
   // FIX: add addPointMass to Frame?
   public void addMassToMaster (MatrixBlock mblk, MatrixBlock sblk, int idx) {
      if (!(sblk instanceof Matrix3x3Block)) {
         throw new IllegalArgumentException (
            "Slave block not instance of Matrix3x3Block");
      }
      Matrix3x3Block slaveBlk = (Matrix3x3Block)sblk;
      double mass = slaveBlk.m00;
      if (mass != 0) {
         Vector3d vec = new Vector3d();
         Frame pframe = myPoint.getPointFrame();
         // TODO: need to remove mass from pframe
         myFrame.computePointPosition (vec, myLoc);
         if (pframe != null && idx == 1 || pframe == null && idx == 0) {
            // sub Frame position since vec only wanted in world orientation 
            vec.sub (myFrame.getPosition());
            myFrame.addPointMass (mblk, mass, vec);
         }
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
         setFrame (
            postscanReference (tokens, Frame.class, ancestor), myLoc);
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
      myLoc.write (pw, fmt, /*withBrackets=*/true);
      pw.println ("");
   }
   
   private void computeVelDerivative (Vector3d dvel) {
      Frame pframe = myPoint.getPointFrame();

      RotationMatrix3d R2 = myFrame.getPose().R;
      Twist vel2 = myFrame.getVelocity();
      Vector3d tmp1 = new Vector3d();
      Vector3d tmp2 = new Vector3d();
      if (myFrame instanceof DeformableBody) {
         // XXX shouldn't have to check explicitly for deformable body;
         // Frame should have the necessary methods
         DeformableBody defb = (DeformableBody)myFrame;
         defb.computeDeformedPos (tmp1, myLoc);
         tmp1.transform (R2); // R2*lp2
         defb.computeDeformedVel (tmp2, myLoc);
         tmp2.transform (R2); // R2*lv2
         // tmp1 = w2 X R2*lp2 + R2*lv2
         tmp1.crossAdd (vel2.w, tmp1, tmp2);
         // dvel = w2 X R2*lv2 + w2 X tmp1
         dvel.cross (vel2.w, tmp2);
         dvel.crossAdd (vel2.w, tmp1, dvel);
      }
      else {
         // tmp1 = w2 X R2*lp2
         tmp1.transform (R2, myLoc);  // R2*lp2
         tmp1.cross (vel2.w, tmp1);
         // dvel = w2 X tmp1
         dvel.cross (vel2.w, tmp1);
      }
      if (pframe != null) {
         RotationMatrix3d R1 = pframe.getPose().R;
         Twist vel1 = pframe.getVelocity();
         tmp2.transform (R1, myPoint.getLocalVelocity());  // R1*lv1
         tmp2.negate();
         // tmp2 = -R1*lv1 - u2 + u1 - tmp1
         tmp2.sub (vel2.v);
         tmp2.add (vel1.v);
         tmp2.sub (tmp1);
         // dvel = R1^T (w1 X tmp2 + dvel)
         dvel.crossAdd (vel1.w, tmp2, dvel);
         dvel.inverseTransform (R1);            
      }
   }

   // FIX
   public boolean getDerivative (double[] buf, int idx) {
      Vector3d dvel = new Vector3d();
      computeVelDerivative (dvel);
      
      //myFrame.computePointCoriolis (dvel, myLoc);
      buf[idx  ] = dvel.x;
      buf[idx+1] = dvel.y;
      buf[idx+2] = dvel.z;
      return true;
   }

   public PointFrameAttachment() {
   }
   
   public PointFrameAttachment (Point slave) {
      this();
      setSlave (slave);
   }

   public PointFrameAttachment (Frame master, Point slave) {
      this (slave);
      setFrame (master);
   }

   public PointFrameAttachment (Frame master, Point slave, Point3d loc) {
      this (slave);
      setFrame (master, loc);
   }

//   @Override
//   public void connectToHierarchy () {
//      Point point = getPoint();
//      Frame frame = getFrame();
//      if (point == null || frame == null) {
//         throw new InternalErrorException ("null point and/or frame");
//      }
//      super.connectToHierarchy ();
//      point.setAttached (this);
//      frame.addMasterAttachment (this);
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      Point point = getPoint();
//      Frame frame = getFrame();
//      if (point == null || frame == null) {
//         throw new InternalErrorException ("null point and/or frame");
//      }
//      super.disconnectFromHierarchy();
//      point.setAttached (null);
//      frame.removeMasterAttachment (this);
//   }

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

      a.myLoc = new Point3d (myLoc);
      if (myFrame != null) {
         Frame frame;
         if ((frame = (Frame)copyMap.get(myFrame)) == null) {
            frame = myFrame;
         }
         a.setFrame (frame);
      }
      return a;
   }
}
