/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.*;

/**
 * Class to attach a frame to another frame.
 */
public class FrameFrameAttachment extends FrameAttachment {

  //private FemElement myElement;

   private Frame myMaster;
   // Transform from Slave to Master
   private RigidTransform3d myTFM = new RigidTransform3d();
   // Current transform from Slave to World
   private RigidTransform3d myTFW = new RigidTransform3d();

   private boolean myMasterBlockInWorldCoords = true;
   
   public boolean debug = false;

   public FrameFrameAttachment () {
   }

   public FrameFrameAttachment (Frame frame) {
      if (frame instanceof DeformableBody) {
         throw new IllegalArgumentException (
"Deformable bodies not supported as slaves in FrameFrameAttachments");
      }
      myFrame = frame;
   }

   public FrameFrameAttachment (Frame frame, Frame master) {
      this (frame);
      set (master, frame.getPose());
   }
   
   public FrameFrameAttachment(Frame frame, Frame master, RigidTransform3d TFM) {
      this(frame);
      set(frame, master, TFM);
   }

   @Override
   public boolean isFlexible() {
      return myMaster instanceof DeformableBody;
   }

   public Frame getMaster() {
      return myMaster;
   }

   public RigidTransform3d getTFM() {
      return myTFM;
   }

   protected void setMaster (Frame master) {
      removeBackRefsIfConnected();
      myMaster = master;
      invalidateMasters();
      addBackRefsIfConnected();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }
   
   protected void set (Frame frame, Frame master, RigidTransform3d TFM) {
      myFrame = frame;
      setMaster (master);
      myTFM.set (TFM);
      if (master != null) {
         myTFW.mul (master.getPose (), TFM);
      }
      else {
         myTFW.set (TFM);
      }
   }

   public void set (Frame master, RigidTransform3d TFW) {
      setMaster (master);
      if (master != null) {
         master.computeFrameLocation (myTFM, TFW);
      }
      else {
         myTFM.set (TFW);
      }
      myTFW.set (TFW);
   }

   public void setWithTFM (Frame master, RigidTransform3d TFM) {
      setMaster (master);
      if (myMaster instanceof DeformableBody) {
         DeformableBody defBody = (DeformableBody)myMaster;
         defBody.computeUndeformedFrame (myTFM, TFM);
      }
      else {
         myTFM.set (TFM);
      }
      if (myMaster != null) {
         myTFW.mul (myMaster.getPose(), myTFM);
      }
      else {
         myTFW.set (myTFM);
      }
   }

   @Override
   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters(masters);
      if (myMaster != null) {
         masters.add (myMaster);
      }
   }

   @Override
   public void updatePosStates() {
      if (myMaster != null) {
         if (myMasters == null) {
            initializeMasters();
         }
//        if (myMaster instanceof DeformableBody) {
//           DeformableBody defBody = (DeformableBody)myMaster;
//           defBody.computeDeformedPos (myPos, myTFM.p);
//           defBody.computeDeformationGradient (myF, myTFM.p);
//           myPolarD.factor (myF);
//        }
         // if (myFrame != null) {
         //    computeTFW (TFW);
         //    myFrame.setPose (TFW);
         // }
         // updateMasterBlocks();

         Twist vel = new Twist();
         myMaster.computeFramePosVel (
            myTFW, vel, myMasterBlocks[0], null, myTFM); 
         if (myFrame != null) {
            myFrame.setPose (myTFW);
         }
      }
   }

   @Override
   public void updateVelStates() {
      // XXX Is this right?
      // XXX Where is the elastic velocity for a deformable body?
      Twist velAW = new Twist();

      // add velocity from master
      Twist velFW = myMaster.getVelocity();
      Vector3d pAFw = new Vector3d();
      // rotate TFM.p into world coords
      pAFw.transform (myMaster.getPose().R, myTFM.p); 
      velAW.v.cross (velFW.w, pAFw);
      velAW.add (velFW);
      myFrame.setVelocity (velAW);
   }

   @Override
   public void updateAttachment() {
      if (myMaster != null && myFrame != null) {
         myTFW.set (myFrame.getPose());
         myMaster.computeFrameLocation (myTFM, myTFW);
      }
   }
   
   @Override
   public boolean setCurrentTFW (RigidTransform3d TFW) {
      if (myMaster != null) {
         myMaster.computeFrameLocation (myTFM, TFW);
         updatePosStates();
      }
      else {
         myTFM.set (TFW);
         myTFW.set (TFW);
      }
      return false;
   }

   VectorNd getMasterForce() {
      VectorNd force = new VectorNd(myMaster.getVelStateSize());
      for (int i=0; i<6; i++) {
         force.set (i, myMaster.getForce().get(i));
      }
      if (myMaster instanceof DeformableBody) {
         DeformableBody defb = (DeformableBody)myMaster;
         for (int i=0; i<defb.numElasticCoords(); i++) {
            force.set (i+6, defb.getElasticForce().get(i));
         }         
      }
      return force;
   }      
   
   void setMasterForce (VectorNd force) {
      for (int i=0; i<6; i++) {
         myMaster.getForce().set (i, force.get(i));
      }
      if (myMaster instanceof DeformableBody) {
         DeformableBody defb = (DeformableBody)myMaster;
         for (int i=0; i<defb.numElasticCoords(); i++) {
            defb.getElasticForce().set (i, force.get(i+6));
         }         
      }
   }      
   
   @Override
   public void applyForces() {
      Wrench forceA = myFrame.getForce();
      if (myMaster instanceof DeformableBody) {

         VectorNd oldForce = getMasterForce();

         myMaster.addFrameForce (myTFM, forceA);
         VectorNd addForce = getMasterForce();
         addForce.sub (oldForce);

         VectorNd chkForce = new VectorNd (myMaster.getVelStateSize());
         VectorNd forceV = new VectorNd(6);
         forceV.set (forceA);
         myMasterBlocks[0].mul (chkForce, forceV);
         double tol = Math.max(oldForce.norm(), addForce.norm())*1e-10;
         if (!addForce.epsilonEquals(chkForce, tol)) {
            System.out.println (
               "FrameFrameAttachment: force check mismatch");
            System.out.println ("DEF add=" + addForce.toString ("%12.8f"));
            System.out.println ("DEF chk=" + chkForce.toString ("%12.8f"));
         }
      }
      else {
         myMaster.addFrameForce (myTFM, forceA);
      }
   }

   @Override
   public boolean getDerivative (double[] buf, int idx) {

      Vector3d vec = new Vector3d();
      myMaster.computePointCoriolis (vec, myTFM.p);
      buf[idx  ] = vec.x;
      buf[idx+1] = vec.y;
      buf[idx+2] = vec.z;
      buf[idx+3] = 0;
      buf[idx+4] = 0;
      buf[idx+5] = 0;
      return false;
   }

   @Override
   public void addMassToMasters() {
      if (myFrame.getEffectiveMass() != 0) {
         if (myFrame instanceof RigidBody && myMaster instanceof RigidBody) {
            RigidBody body = (RigidBody)myFrame;
            SpatialInertia MB = new SpatialInertia(body.getEffectiveInertia());
            body.subEffectiveInertia (MB);
            ((RigidBody)myMaster).addEffectiveFrameMass (MB, myTFM);
         }
         else {
            throw new UnsupportedOperationException (
               "addMassToMasters() only supported between rigid bodies");
         }
      }
   }

   @Override
   public void getCurrentTFW (RigidTransform3d TFW) {
      if (myMaster != null) {
         TFW.set (myTFW);
         //computeTFW (TFW);
      }
      else {
         TFW.set (myTFM);
      }
   }

   @Override
   public void getUndeformedTFW (RigidTransform3d TFW) {
      if (isFlexible()) {
         TFW.mul (myMaster.getPose(), myTFM);
      }
      else {
         getCurrentTFW (TFW);
      }
   }

//   protected void computeTFW (RigidTransform3d TFW) {
//      if (myMaster instanceof DeformableBody) {
//         RigidTransform3d TFM = new RigidTransform3d();
//         // polar decomposition should already have been computed in 
//         // updatePosStates()
//         TFM.R.mul (myPolarD.getR(), myTFM.R);
//         TFM.p.set (myPos);
//         TFW.mul (myMaster.getPose(), TFM);
//      }
//      else {
//         TFW.mul (myMaster.getPose(), myTFM);
//      }      
//   }
//
   protected double[] getTotalVel (DeformableBody defBody) {
      int numc = defBody.numElasticCoords();
      double[] totalVel = new double[6+numc];
      defBody.getVelocity().get (totalVel);
      VectorNd evel = defBody.getElasticVel();
      for (int i=0; i<numc; i++) {
         totalVel[i+6] = evel.get(i);
      }
      return totalVel;
   }

   @Override
   public void getCurrentVel (Twist vel, Twist dg) {
      if (myMaster instanceof DeformableBody) {
         DeformableBody defBody = (DeformableBody)myMaster;
         // assumes that master blocks have been updated
         double[] totalVel = getTotalVel (defBody);
         double[] wvel = new double[6];
         myMasterBlocks[0].mulTransposeAdd (wvel, 0, totalVel, 0);
         vel.set (wvel);
         if (myMasterBlockInWorldCoords) {
            // master block gives velocity in world coordinates. Need to rotate
            // into coords of the attached frame
            vel.inverseTransform (myTFW.R);
         }

         // Twist chk = new Twist();
         // defBody.computeFrameVelocity (chk, myTFM);
         // chk.inverseTransform (myTFW.R);
         // System.out.println ("vel=" + vel.toString ("%12.8f"));
         // System.out.println ("chk=" + chk.toString ("%12.8f"));          
      }
      else if (myMaster != null) {
         double[] bvel = new double[6];
         double[] wvel = new double[6];
         myMaster.getVelocity().get (bvel);
         myMasterBlocks[0].mulTransposeAdd (wvel, 0, bvel, 0);
         vel.set (wvel);
         if (myMasterBlockInWorldCoords) {
            // master block gives velocity in world coordinates. Need to rotate
            // into coords of the attached frame
            vel.inverseTransform (myMaster.getPose().R);
            vel.inverseTransform (myTFM.R);
         }
      }
      else {
         vel.setZero();
      }
      
      if (dg != null) {
         if (myMaster != null) {
            Twist velm = myMaster.getVelocity();
            dg.v.cross (velm.v, velm.w);
            dg.v.inverseTransform (myMaster.getPose().R);
            dg.v.inverseTransform (myTFM.R);
            dg.w.setZero();
            // Need to fix this for deformable bodies
         }
         else {
            dg.setZero();
         }
      }
   }

   /**
    * Computes the master forces that result when a wr is applied in the
    * attached coordinate frame. The result vector <code>f</code> is auto-sized
    * to the correct size. The first six elements of <code>f</code> contain the
    * wrench applied to the master frame, rotated into world coordinates.
    *
    * @param f returns the master forces
    * @param wr applied wrench (in attached frame coordinates)
    */
   public void getMasterForces (VectorNd f, Wrench wr) {
      Wrench wtmp = new Wrench(wr);
      double[] wbuf = new double[6];
      if (myMaster != null) { 
         if (myMasterBlockInWorldCoords) {
            // master block expects force in world coordinates. Need to rotate
            // from coords of the attached frame
            wtmp.transform (myTFW.R);
         }
         wtmp.get (wbuf);
         // assumes that master blocks have been updated
         f.setSize (myMaster.getVelStateSize());
         f.setZero();
         myMasterBlocks[0].mulAdd (f.getBuffer(), 0, wbuf, 0);
      }
      else {
         f.setSize(6);
         f.setZero();
      }      
   }

//   public void getCurrentVelX (Twist vel, Twist dg) {
//      if (myMaster instanceof DeformableBody) {
//         DeformableBody defBody = (DeformableBody)myMaster;
//         // assumes that master blocks and myPolarD have been updated
//         RigidTransform3d TFM = new RigidTransform3d();
//         TFM.R.mul (myPolarD.getR(), myTFM.R);
//         TFM.p.set (myPos);
//
//         Twist velm = myMaster.getVelocity();
//         vel.inverseTransform (myMaster.getPose().R, velm);
//         vel.inverseTransform (TFM);
//
//         Twist veldef = new Twist();
//         defBody.computeDeformedFrameVel (veldef, myPolarD, myTFM);
//
//         vel.add (veldef);
//      }
//      else if (myMaster != null) {
//         Twist velm = myMaster.getVelocity();
//         vel.inverseTransform (myMaster.getPose().R, velm);
//         vel.inverseTransform (myTFM);
//      }
//      else {
//         vel.setZero();
//      }
//      
//      if (dg != null) {
//         if (myMaster != null) {
//            Twist velm = myMaster.getVelocity();
//            dg.v.cross (velm.v, velm.w);
//            dg.v.inverseTransform (myMaster.getPose().R);
//            dg.v.inverseTransform (myTFM.R);
//            dg.w.setZero();
//            // Need to fix this for deformable bodies
//         }
//         else {
//            dg.setZero();
//         }
//      }
//   }

   /**
    * {@inheritDoc}
    */
   public double getAverageMasterMass() {
      if (myMaster != null) {
         return myMaster.getMass(0);
      }
      else {
         return 0;
      }
   }

   /**
    * {@inheritDoc}
    */
   public double getAverageMasterInertia() {
      if (myMaster instanceof RigidBody) {
         RigidBody body = (RigidBody)myMaster;
         SpatialInertia inertia = body.getInertia();
         Vector3d com = new Vector3d(); // vector from COM to compliance frame
         com.sub (myTFM.p, inertia.getCenterOfMass());
         double l = com.norm();
         return inertia.getRotationalInertia().trace()/3 + inertia.getMass()*l*l;
      }
      else {
         return 0;
      }
   }

   
   // public void getRestPosition (Point3d pos) {
   //    pos.setZero();
   //    for (int i=0; i<myNodes.length; i++) {
   //       pos.scaledAdd (
   //          myCoords.get(i), ((FemNode3d)myNodes[i]).getRestPosition());
   //    }
   // }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "master", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "TFM")) {
         myTFM.scan (rtok);
         return true;         
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "master")) {
         Frame master = postscanReference (tokens, Frame.class, ancestor);
         setMaster (master);
         updatePosStates(); // need to update TFM, etc.
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println (
         "master="+ComponentUtils.getWritePathName (ancestor, myMaster));
      int writeFormat = RigidTransform3d.AXIS_ANGLE_STRING;
      if (fmt.isFullPrecisionDouble()) {
         // need to do MATRIX_3X4_STRING since that's the only thing
         // that gives us full precision save/restore
         writeFormat = RigidTransform3d.MATRIX_3X4_STRING;
      }
      pw.println ("TFM=" + myTFM.toString (fmt, writeFormat));
   }

//   @Override
//   public void connectToHierarchy () {
//      if (myFrame == null || myMaster == null) {
//         throw new InternalErrorException ("null frame and/or master");
//      }
//      super.connectToHierarchy ();
//      myFrame.setAttached (this);
//      myMaster.addMasterAttachment (this);
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      if (myFrame == null || myMaster == null) {
//         throw new InternalErrorException ("null frame and/or master");
//      }
//      super.disconnectFromHierarchy ();
//      myFrame.setAttached (null);
//      myMaster.removeMasterAttachment (this);
//   }

//   @Override
//   public void getHardReferences (List<ModelComponent> refs) {
//      super.getHardReferences (refs);
//      if (myFrame != null) {
//         refs.add (myFrame);
//      }
//      if (myMaster != null) {
//         refs.add (myMaster);
//      }
//   }   

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      if (myFrame != null) {
         if (!ComponentUtils.addCopyReferences (refs, myFrame, ancestor)) {
            return false;
         }
      }
      if (myMaster != null) {
         if (!ComponentUtils.addCopyReferences (refs, myMaster, ancestor)) {
            return false;
         }
      }
      return true;
   }
   
   public void scaleDistance (double s) {
      myTFM.p.scale (s);
   }   

   public FrameFrameAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FrameFrameAttachment a = (FrameFrameAttachment)super.copy (flags, copyMap);

      a.myTFM = new RigidTransform3d(myTFM);
      a.myTFW = new RigidTransform3d(myTFW);

      if (myFrame != null) {
         Frame frame =
            (Frame)ComponentUtils.maybeCopy (flags, copyMap, myFrame);
         a.myFrame = frame;
      }
      if (myMaster != null) {
         Frame master =
            (Frame)ComponentUtils.maybeCopy (flags, copyMap, myMaster);
         a.setMaster (master);
         if (master instanceof DeformableBody) {
            // do we need this?
            a.updatePosStates();
         }
      }
      return a;
   }
}
