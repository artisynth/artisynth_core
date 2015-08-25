/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import java.io.*;

public class PointParticleAttachment extends PointAttachment {

   // DynamicComponent[] myMasters;
   Particle myParticle;
   // neither the point nor the particle are frame-relative
   boolean myNoFrameRelativeP = true;

//   public DynamicComponent[] getMasters() {
//      if (myMasters == null) {
//         myMasters = new DynamicComponent[1];
//         myMasters[0] = myParticle;
//      }
//      return myMasters;
//   }

   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters (masters);
      Frame pframe = myParticle.getPointFrame();
      if (pframe != null) {
         masters.add (pframe);
      }
      masters.add (myParticle);
   }
   
   @Override
   protected int updateMasterBlocks() {
      int idx = super.updateMasterBlocks();

      RotationMatrix3d R1 = null;
      RotationMatrix3d R2 = null;
      if (idx == 1) {
         // then the point also has a frame; set R1 to that frame's rotation
         R1 = myPoint.getPointFrame().getPose().R;
      }
      Frame pframe = myParticle.getPointFrame();
      myNoFrameRelativeP = false;
      if (pframe != null) {
         R2 = pframe.getPose().R;
         pframe.computeLocalPointForceJacobian (
            myMasterBlocks[idx++], myParticle.getLocalPosition(), R1);
      }
      else if (R1 == null) {
         myNoFrameRelativeP = true;
      }         
      Matrix3x3Block pblk = (Matrix3x3Block)myMasterBlocks[idx++];
      if (!myNoFrameRelativeP) {

         if (R1 != null && R2 != null) {
            pblk.mulTransposeLeft (R2, R1);
         }
         else if (R1 != null) {
            pblk.set (R1);
         }
         else if (R2 != null) {
            pblk.transpose (R2);
         }
      }
      else {
         // don't really need to set the block because it won't be used in this
         // case
         pblk.setIdentity();
      }
      return idx;
   }
   
   public Particle getParticle() {
      return myParticle;
   }

//   public int numMasters() {
//      return 1;
//   }

   public void setParticle (Particle particle) {
      removeBackRefsIfConnected();
      myParticle = particle;
      invalidateMasters();
      addBackRefsIfConnected();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   void setPoint (Point point) {
      myPoint = point;
   }

   public PointParticleAttachment() {
   }

   public PointParticleAttachment (Particle master, Point slave) {
      this();
      setParticle (master);
      setPoint (slave);
   }

   public void updatePosStates() {
      Point3d pntw = new Point3d();
      getCurrentPos (pntw);
      myPoint.setPosition (pntw);
      updateMasterBlocks();
   }

   public void getCurrentPos (Vector3d pos) {
      pos.set (myParticle.getPosition());
   }
   
   public void getCurrentVel (Vector3d vel, Vector3d dvel) {
      vel.set (myParticle.getVelocity());
      if (dvel != null) {
         computeVelDerivative (dvel);
      }
   }

   public void updateVelStates() {
      myPoint.setVelocity (myParticle.getVelocity());
   }

   public void applyForces() {
      super.applyForces();
      myParticle.addForce (myPoint.myForce);
   }
   
//   public void addScaledExternalForce(Point3d pnt, double s, Vector3d f) {
//      myParticle.addScaledExternalForce(s, f);
//   }

//   protected MatrixBlock createRowBlock (int colSize) {
//      return createRowBlockNew (colSize);
//   }
//
//   protected MatrixBlock createColBlock (int rowSize) {
//      return createColBlockNew (rowSize);
//   }
//
//   protected MatrixBlock createRowBlockNew (int colSize) {
//      switch (colSize) {
//         case 1:
//            return new Matrix3x1Block();
//         case 3:
//            return new Matrix3x3Block();
//         case 6: 
//            return new Matrix3x6Block();
//         default:
//            return new MatrixNdBlock(3, colSize);
//      }
//   }
//
//   protected MatrixBlock createColBlockNew (int rowSize) {
//      switch (rowSize) {
//         case 1:
//            return new Matrix1x3Block();
//         case 3:
//            return new Matrix3x3Block();
//         case 6: 
//            return new Matrix6x3Block();
//         default:
//            return new MatrixNdBlock(rowSize, 3);
//      }
//   }

   public void mulSubGT (MatrixBlock D, MatrixBlock B, int idx) {
      if (myNoFrameRelativeP) {
         D.add (B);
      }
      else {
         D.mulAdd (myMasterBlocks[idx], B);         
      }
   }

   public void mulSubG (MatrixBlock D, MatrixBlock B, int idx) {
      if (myNoFrameRelativeP) {
         D.add (B);
      }
      else {
         MatrixBlock G = myMasterBlocks[idx].createTranspose();
         D.mulAdd (B, G);         
      }
   }

   protected void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
      if (myNoFrameRelativeP) {
         ybuf[yoff  ] += xbuf[xoff  ];
         ybuf[yoff+1] += xbuf[xoff+1];
         ybuf[yoff+2] += xbuf[xoff+2];
      }
      else {
         myMasterBlocks[idx].mulAdd (ybuf, yoff, xbuf, xoff);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "particle", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "particle")) {
         setParticle (postscanReference (
            tokens, Particle.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("particle=" + ComponentUtils.getWritePathName (
                     ancestor, myParticle));
   }

   public void updateAttachment() {
      // nothing to do here
   }

   @Override
   public void transformSlaveGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      myPoint.transformGeometry (X, topObject, flags);
      updateAttachment();
   }

   public void addMassToMasters() {
      double m = myPoint.getEffectiveMass();
      if (m != 0) {
         myParticle.addEffectiveMass (m);
      }
      myPoint.addEffectiveMass(-m);
   }
   
   public void addMassToMaster (MatrixBlock mblk, MatrixBlock sblk, int idx) {

      if (!(sblk instanceof Matrix3x3Block)) {
         throw new IllegalArgumentException (
            "Master blk not instance of Matrix3x3Block");
      }
      double m = ((Matrix3x3Block)sblk).m00;
      Frame pframe = myPoint.getPointFrame();
      Frame mframe = myParticle.getPointFrame();
      // TODO: need to remove mass from pframe
      if (mframe == null) {
         if (pframe != null && idx == 1 || pframe == null && idx == 0) {
            Matrix3x3Block dblk = (Matrix3x3Block)mblk;
            dblk.m00 += m;
            dblk.m11 += m;
            dblk.m22 += m;
         }
      }
   }

   private boolean computeVelDerivative (Vector3d dvel) {
      Frame pframe = myPoint.getPointFrame();
      Frame mframe = myParticle.getPointFrame();
      boolean isNonZero = false;

      if (mframe != null) {
         RotationMatrix3d R2 = mframe.getPose().R;
         Twist vel2 = mframe.getVelocity();
         Vector3d tmp1 = new Vector3d();
         Vector3d tmp2 = new Vector3d();
         tmp1.transform (R2, myParticle.getLocalPosition());  // R2*lp2
         tmp2.transform (R2, myParticle.getLocalVelocity());  // R2*lv2
         // tmp1 = w2 X R2*lp2 + R2*lv2
         tmp1.crossAdd (vel2.w, tmp1, tmp2);
         // dvel = w2 X R2*lv2 + w2 X tmp1
         dvel.cross (vel2.w, tmp2);
         dvel.crossAdd (vel2.w, tmp1, dvel);
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
         isNonZero = true;
      }
      else if (pframe != null) {
         RotationMatrix3d R1 = pframe.getPose().R;
         Twist vel1 = pframe.getVelocity();
         // dvel = R1^T (w1 X (u1 - R1*lv1 - lv2))
         dvel.transform (R1, myPoint.getLocalVelocity()); // R1*lv1
         dvel.negate();
         // since myParticle has no frame, lv2 and velocity are the same
         dvel.sub (myParticle.getVelocity());
         dvel.add (vel1.v);
         dvel.cross (vel1.w, dvel);
         dvel.inverseTransform (R1);
         isNonZero = true;
      }
      return isNonZero;
   }
   
   public boolean getDerivative (double[] buf, int idx) {
      Vector3d dvel = new Vector3d();
      boolean isNonZero = computeVelDerivative (dvel);
      buf[idx  ] = dvel.x;
      buf[idx+1] = dvel.y;
      buf[idx+2] = dvel.z;
      return isNonZero;
   }

//   @Override
//   public void connectToHierarchy () {
//      Point point = getPoint();
//      Particle particle = getParticle();
//      if (point == null) {
//         throw new InternalErrorException ("null point");
//      }
//      if (particle == null) {
//         throw new InternalErrorException ("null particle");
//      }
//      super.connectToHierarchy ();
//      point.setAttached (this);
//      particle.addMasterAttachment (this);
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      Point point = getPoint();
//      Particle particle = getParticle();
//      if (point == null || particle == null) {
//         throw new InternalErrorException ("null point and/or particle");
//      }
//      super.disconnectFromHierarchy();
//      point.setAttached (null);
//      particle.removeMasterAttachment (this);
//   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      Point point = getPoint();
      Particle particle = getParticle();
      if (point == null || particle == null) {
         throw new InternalErrorException ("null point and/or particle");
      }
      super.getHardReferences (refs);
      refs.add (point);
      refs.add (particle);
   }

   public PointParticleAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointParticleAttachment a =
         (PointParticleAttachment)super.copy (flags, copyMap);

      //a.myMasters = null;      
      if (myParticle != null) {
         a.myParticle =
            (Particle)ComponentUtils.maybeCopy (flags, copyMap, myParticle);
      }
      return a;
   }


}
