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
   protected Point3d myLoc = new Point3d(); // location of point in frame coords
   Frame myFrame;
   protected MatrixBlock[] myMasterBlocks;

   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters (masters);
      masters.add (myFrame);
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

   public Frame getFrame() {
      return myFrame;
   }

   public void setLocation (Point3d loc) {
      myLoc.set (loc);
   }

   public Point3d getLocation() {
      return myLoc;
   }

   protected void setFrame (Frame body, Point3d loc) {
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
      if (MechSystemBase.useAllDynamicComps) {
         notifyParentOfChange (
            new DynamicActivityChangeEvent (this, /*stateChanged=*/false));
      }
      else {
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
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
      if (myFrame == null) {
         return;
      }
      if (myMasterBlocks == null) {
         myMasterBlocks = allocateMasterBlocks();
      } 
      Point3d posw = new Point3d();
      Vector3d velw = new Vector3d();
      myFrame.computePointPosVel (posw, velw, myMasterBlocks[0], null, myLoc);
      myPoint.setPosition (posw);
      //updateJacobian();
      //updateMasterBlocks();
   }

   public void updateVelStates() {
      if (myFrame == null) {
         return;
      }
      Vector3d velw = new Vector3d();
      myFrame.computePointVelocity (velw, myLoc);
      myPoint.setVelocity (velw);
   }

   public void applyForces() {
      super.applyForces();
      myFrame.addPointForce (myLoc, myPoint.myForce);
   }

   public void mulSubGTM (MatrixBlock D, MatrixBlock M, int idx) {
      D.mulAdd (myMasterBlocks[idx], M);
   }

   public void mulSubMG (MatrixBlock D, MatrixBlock M, int idx) {
      if (myMasterBlocks != null) {
         MatrixBlock G = myMasterBlocks[idx].createTranspose();
         D.mulAdd (M, G);
      }         
   }

   public MatrixBlock getGT (int idx) {
      if (myMasterBlocks != null) {
         MatrixBlock blk = myMasterBlocks[idx].clone();
         blk.negate();
         return blk;
      }
      else {
         return null;
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

   // FIX: use getPointJacobian?
   public int addTargetJacobian (SparseBlockMatrix J, int bi) { // FIX
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
   
   // FIX
   public boolean getDerivative (double[] buf, int idx) {
      Vector3d dvel = new Vector3d();
      //computeVelDerivative (dvel);
      
      myFrame.computePointCoriolis (dvel, myLoc);
      buf[idx  ] = dvel.x;
      buf[idx+1] = dvel.y;
      buf[idx+2] = dvel.z;
      return true;
   }

   public PointFrameAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointFrameAttachment a = (PointFrameAttachment)super.copy (flags, copyMap);

      a.myLoc = new Point3d (myLoc);
      a.myMasterBlocks = null; // will be reinitialized
      if (myFrame != null) {
         Frame frame = (Frame)ComponentUtils.maybeCopy (flags, copyMap, myFrame);
         a.setFrame (frame);
      }
      return a;
   }
}
