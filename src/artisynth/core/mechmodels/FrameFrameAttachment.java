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

   private PolarDecomposition3d myPolarD = null;
   private Matrix3d myF = null;
   private Vector3d myPos = null;

   private boolean myMasterBlockInWorldCoords = true;

   public FrameFrameAttachment () {
   }

   public FrameFrameAttachment (Frame frame) {
      myFrame = frame;
   }

   public FrameFrameAttachment (Frame frame, Frame master) {
      this (frame);
      set (master, frame.getPose());
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
      if (master instanceof DeformableBody) {
         myF = new Matrix3d();
         myPolarD = new PolarDecomposition3d();
         myPos = new Vector3d();
      }
      else {
         myF = null;
         myPolarD = null;
         myPos = null;
      }
      myMaster = master;
      invalidateMasters();
      addBackRefsIfConnected();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   public void set (Frame master, RigidTransform3d TFW) {
      setMaster (master);
      if (master != null) {
         RigidTransform3d TFM = new RigidTransform3d();
         TFM.mulInverseLeft (master.getPose(), TFW);
         doSetTFM (TFM);
      }
      else {
         myTFM.set (TFW);
      }
   }

   public void setWithTFM (Frame master, RigidTransform3d TFM) {
      setMaster (master);
      doSetTFM (TFM);
   }

   private void doSetTFM (RigidTransform3d TFM) {
      if (myMaster instanceof DeformableBody) {
         DeformableBody defBody = (DeformableBody)myMaster;
         defBody.computeUndeformedFrame (myTFM, myPolarD, TFM);
      }
      else {
         myTFM.set (TFM);
      }
   }      

   @Override
   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters(masters);
      if (myMaster != null) {
         masters.add (myMaster);
      }
   }

   protected void setElasticPartOfMasterBlock (
      MatrixNd blk, boolean worldCoords) {

      DeformableBody defBody = (DeformableBody)myMaster;
      int numc = defBody.numElasticCoords();
      if (blk.rowSize() != 6+numc) {
         throw new InternalErrorException (
            "Master block should be 6 x "+numc+"; is "+blk.getSize());
      }
      MatrixNd Pi = new MatrixNd (6, numc);
      defBody.computeElasticJacobian (Pi, myPolarD, myTFM, worldCoords);
      // set the lower n x 6 submatrix of blk to the transpose of Pi
      double[] bbuf = blk.getBuffer();
      double[] pbuf = Pi.getBuffer();
      if (blk.getBufferWidth() != 6) {
         throw new InternalErrorException (
            "Master block has buffer width of "+blk.getBufferWidth()+
            "; expecting 6");
      }
      int k = 36;
      for (int i=0; i<numc; i++) {
         for (int j=0; j<6; j++) {
            bbuf[k++] = pbuf[j*numc+i];
         }
      }
   }

   @Override
   protected int updateMasterBlocks() {

      boolean worldCoords = myMasterBlockInWorldCoords;

      int idx = super.updateMasterBlocks();
      if (idx != 0) {
         throw new UnsupportedOperationException (
            "Unsupported case: master block added in FrameAttachment base class");
      }
      if (myMasterBlocks.length > 0) {
         Vector3d pAFw = new Vector3d();
         MatrixBlock blk = myMasterBlocks[idx++];
         RotationMatrix3d RFW = null;
         if (!worldCoords) {
            RFW = new RotationMatrix3d();
            RFW.mul (myMaster.getPose().R, myTFM.R);
         }
         if (myMaster instanceof DeformableBody) {
            if (!(blk instanceof MatrixNdBlock)) {
               throw new InternalErrorException (
                  "Master block is not an instance of MatrixNdBlock; is " +
                  blk.getClass());
            }
            setElasticPartOfMasterBlock ((MatrixNdBlock)blk, worldCoords);
            // rotate into world coords
            pAFw.transform (myMaster.getPose().R, myPos); 
         }
         else {
            // rotate TFM.p into world coords
            pAFw.transform (myMaster.getPose().R, myTFM.p);
         }
         computeFrameFrameJacobian (blk, pAFw, RFW);
      }
      return idx;
   }

   @Override
   public void updatePosStates() {
      if (myMaster != null) {
         if (myMaster instanceof DeformableBody) {
            DeformableBody defBody = (DeformableBody)myMaster;
            defBody.computeDeformedPos (myPos, myTFM.p);
            defBody.computeDeformationGradient (myF, myTFM.p);
            myPolarD.factor (myF);
         }
         if (myFrame != null) {
            RigidTransform3d TFW = new RigidTransform3d();
            computeTFW (TFW);
            myFrame.setPose (TFW);
         }
         updateMasterBlocks();
      }
   }

   @Override
   public void updateVelStates() {
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
         RigidTransform3d TFM = new RigidTransform3d();         
         TFM.mulInverseLeft (myMaster.getPose(), myFrame.getPose());
         doSetTFM (TFM);
      }
   }
   
   @Override
   public void setCurrentTFW (RigidTransform3d TFW) {
      if (myMaster != null) {
         RigidTransform3d TFM = new RigidTransform3d(); 
         TFM.mulInverseLeft (myMaster.getPose(), TFW);
         doSetTFM (TFM);
         if (myMaster instanceof DeformableBody) {
            updatePosStates();
         }
      }
      else {
         doSetTFM (TFW);
      }
   }
   
   @Override
   public void applyForces() {
      Wrench forceA = myFrame.getForce();
      Wrench forceF = new Wrench(forceA);
      Vector3d pAFw = new Vector3d();
      // rotate TFM.p into world coords
      pAFw.transform (myMaster.getPose().R, myTFM.p); 
      forceF.m.crossAdd (pAFw, forceA.f, forceF.m);
      myMaster.addForce (forceF);
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
   public void addMassToMaster (MatrixBlock mblk, MatrixBlock sblk, int i) {
      if (myFrame.getMass(0) != 0) {
         throw new UnsupportedOperationException (
            "addMassToMaster() not yet supported for frames with non-zero mass");
      }
   }

   @Override
   public void addMassToMasters() {
      if (myFrame.getMass(0) != 0) {
         if (myFrame instanceof RigidBody && myMaster instanceof RigidBody) {
            RigidBody body = (RigidBody)myFrame;
            SpatialInertia MB = new SpatialInertia(body.getEffectiveInertia());
            SpatialInertia MX = new SpatialInertia();
            RigidTransform3d TFM = new RigidTransform3d();
            TFM.mulInverseLeft (myMaster.getPose(), myFrame.getPose());
            MX.set (MB);
            MX.transform (TFM);
            ((RigidBody)myMaster).addEffectiveInertia (MX);
            MB.negate();
            body.addEffectiveInertia (MB);
         }
         else {
            throw new UnsupportedOperationException (
               "addMassToMasters() only supported between rigid bodies");
         }
      }
   }

   @Override
   public void transformSlaveGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      updateAttachment();
   }

   @Override
   public void getCurrentTFW (RigidTransform3d TFW) {
      if (myMaster != null) {
         computeTFW (TFW);
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

   protected void computeTFW (RigidTransform3d TFW) {
      if (myMaster instanceof DeformableBody) {
         RigidTransform3d TFM = new RigidTransform3d();
         // polar decomposition should already have been computed in 
         // updatePosStates()
         TFM.R.mul (myPolarD.getR(), myTFM.R);
         TFM.p.set (myPos);
         TFW.mul (myMaster.getPose(), TFM);
      }
      else {
         TFW.mul (myMaster.getPose(), myTFM);
      }      
   }

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
            vel.inverseTransform (myMaster.getPose().R);
            vel.inverseTransform (myPolarD.getR());
            vel.inverseTransform (myTFM.R);
         }
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
            wtmp.transform (myTFM.R);
            if (myMaster instanceof DeformableBody) {
               wtmp.transform (myPolarD.getR());
            }
            wtmp.transform (myMaster.getPose().R);
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
         if (master instanceof DeformableBody) {
            // reset the TFM and update deformable info
            doSetTFM (new RigidTransform3d(myTFM));
            updatePosStates();
         }
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

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      if (myFrame != null) {
         refs.add (myFrame);
      }
      if (myMaster != null) {
         refs.add (myMaster);
      }
   }   

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

//   public void transformGeometry (
//      AffineTransform3dBase X, RigidTransform3d TFW) {
//      PolarDecomposition3d pd = new PolarDecomposition3d();
//      pd.factor (X.getMatrix());
//
//      if (TFW == null) {
//         TFW = new RigidTransform3d();
//         getCurrentTFW (TFW);
//      }
//      RigidTransform3d TFWnew = new RigidTransform3d();
//      TFWnew.p.mulAdd (X.getMatrix(), TFW.p, X.getOffset());
//      TFWnew.R.mul (pd.getR(), TFW.R);
//      update
//      if (myMaster != null) {
//         RigidTransform3d TFMnew = new RigidTransform3d();
//         TFMnew.mulInverseLeft (myMaster.getPose(), TFWnew);
//         myTFM.set (TFMnew);
//      }
//      else {
//         myTFM.set (TFWnew);
//      }
//   }

   public FrameFrameAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FrameFrameAttachment a = (FrameFrameAttachment)super.copy (flags, copyMap);

      a.myTFM = new RigidTransform3d(myTFM);

      if (myPos != null) {
         a.myPos = new Vector3d();
      }
      if (myF != null) {
         a.myF = new Matrix3d();
      }
      if (myPolarD != null) {
         a.myPolarD = new PolarDecomposition3d();
      }

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
            // reset the TFM and update deformable info
            a.doSetTFM (new RigidTransform3d(a.myTFM));
            a.updatePosStates();
         }
      }
      return a;
   }
}
