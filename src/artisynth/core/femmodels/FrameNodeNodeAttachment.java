package artisynth.core.femmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

/**
 *
 * Connects a FrameNode3d to a FemNode3d. Let R and p denote the rotation and
 * translation of the frame, vf and wf the translational and rotational frame
 * velocity (world coordinates), xw and vw the position and velocity of the
 * node (world coordinates), and xl and vl the position and velocity of the
 * frame node (frame coordinates). Then the position and velocity
 * relationships are given by
 * <pre>
 *   xl = R^T (xw - p)
 *
 *   vl = -R^T vf + [xl] R^T wf + R^T vw
 * </pre>
 */
public class FrameNodeNodeAttachment
   implements DynamicAttachment {
   
   protected FrameNode3d myFrameNode;
   protected FemNode3d myNode;
   protected Frame myFrame;
   protected DynamicComponent[] myMasters;
   protected Matrix6x3Block myBlk0;

   protected boolean mySlaveAffectsStiffnessP = false;

   public boolean slaveAffectsStiffness () {
      return mySlaveAffectsStiffnessP;
   }

   public void setSlaveAffectsStiffness (boolean affects) {
      mySlaveAffectsStiffnessP = affects;
   }

   public int numMasters() {
      return 2;
   }

   public FrameNodeNodeAttachment (
      FrameNode3d frameNode, FemNode3d node) {
      myFrameNode = frameNode;
      myNode = node;
      myFrame = frameNode.getFrame();
      myMasters = new DynamicComponent[] { myFrame, myNode };
      myBlk0 = new Matrix6x3Block();
   }

   // dynamic attachment 

   public void updateAttachment() {
   }

   protected void updateBlk0() {
      RotationMatrix3d R = myFrame.getPose().R;

      myBlk0.m00 = -R.m00; myBlk0.m01 = -R.m01; myBlk0.m02 = -R.m02;
      myBlk0.m10 = -R.m10; myBlk0.m11 = -R.m11; myBlk0.m12 = -R.m12;
      myBlk0.m20 = -R.m20; myBlk0.m21 = -R.m21; myBlk0.m22 = -R.m22;      

      // stores the product R [ xl ] where xl is the frame node positiob
      Matrix3d RX = new Matrix3d();
      RX.crossProduct (R, myFrameNode.getPosition());

      myBlk0.m30 = -RX.m00; myBlk0.m31 = -RX.m01; myBlk0.m32 = -RX.m02;
      myBlk0.m40 = -RX.m10; myBlk0.m41 = -RX.m11; myBlk0.m42 = -RX.m12;
      myBlk0.m50 = -RX.m20; myBlk0.m51 = -RX.m21; myBlk0.m52 = -RX.m22;      
   }

   public void updatePosStates() {
      Point3d pos = new Point3d();
      pos.inverseTransform (myFrame.getPose(), myNode.getPosition());
      myFrameNode.setPosition (pos);         
      updateBlk0();
   }

   public void updateVelStates() {

      RotationMatrix3d R = myFrame.getPose().R;
      Twist frameVel = myFrame.getVelocity();
      Vector3d vel = new Vector3d();
      Vector3d tmp = new Vector3d();

      vel.sub (myNode.getVelocity(), frameVel.v);
      vel.inverseTransform (R);
      tmp.inverseTransform (R, frameVel.w);
      tmp.cross (myFrameNode.getPosition(), tmp);
      vel.add (tmp);
      myFrameNode.setVelocity (vel);
   }

   public void applyForces() {

      RotationMatrix3d R = myFrame.getPose().R;      
      Wrench frameForce = new Wrench();
      Vector3d frameNodeForce = myFrameNode.getForce();
      Vector3d tmp = new Vector3d();
      
      tmp.transform (R, frameNodeForce);
      // nodeForce += R * frameNodeForce
      myNode.addForce (tmp);
      // frameForce.f -= R * frameNodeForce
      frameForce.f.negate (tmp);
      // frameForce.m -= R * (frameNode.pos X frameNodeForce)
      tmp.cross (myFrameNode.getPosition(), frameNodeForce);
      tmp.transform (R);
      frameForce.m.negate (tmp);

      myFrame.addForce (frameForce);
   }

   public DynamicComponent getSlave() {
      return myFrameNode;
   }

   // don't need to add mass because frame based FEM model uses slave node mass
   public void addMassToMasters() {
   }  

   public boolean getDerivative (double[] buf, int idx) {
      // XXX finish
      buf[idx  ] = 0;
      buf[idx+1] = 0;
      buf[idx+2] = 0;
      return true;
   }
   
   public DynamicComponent[] getMasters() {
      return myMasters;
   }

   public void invalidateMasters() {
      throw new UnsupportedOperationException (
         "addMasterAttachment() not supported for FrameFemNode3d");
   }

   void mulSubGB (MatrixBlock D, MatrixBlock B, int idx) {
      if (idx == 0) {
         D.mulTransposeRightAdd (myBlk0, B);
      }
      else {
         D.mulTransposeRightAdd (myFrame.getPose().R, B);
      }
   }
   
   public void mulSubMG (MatrixBlock D, MatrixBlock M, int idx) {
      if (idx == 0) {
         D.mulTransposeRightAdd (M, myBlk0);
      }
      else {
         D.mulTransposeRightAdd (M, myFrame.getPose().R);
      }
   }

   public MatrixBlock getGT (int idx) {
      if (idx == 0) {
         Matrix6x3Block blk = new Matrix6x3Block();
         blk.negate (myBlk0);
         return blk;
      }
      else if (idx == 1) {
         Matrix3x3Block blk = new Matrix3x3Block();
         blk.set (myFrame.getPose().R);
         blk.negate();
         return blk;
      }
      else {
         throw new IllegalArgumentException (
            "Attachment has two masters so idx must be either 0 or 1");
      }      
   }

   public void mulSubGTM (MatrixBlock D, MatrixBlock M, int idx) {
      if (idx == 0) {
         D.mulAdd (myBlk0, M);
      }
      else {
         D.mulAdd (myFrame.getPose().R, M);
      }
   }

   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {

      if (idx == 0) {
         myBlk0.mulAdd (ybuf, yoff, xbuf, xoff);
      }
      else {
         RotationMatrix3d R = myFrame.getPose().R;

         double x0 = xbuf[xoff++];
         double x1 = xbuf[xoff++];
         double x2 = xbuf[xoff++];

         ybuf[yoff++] += (R.m00*x0 + R.m01*x1 + R.m02*x2);
         ybuf[yoff++] += (R.m10*x0 + R.m11*x1 + R.m12*x2);
         ybuf[yoff++] += (R.m20*x0 + R.m21*x1 + R.m22*x2);
      }
   }

   public void addBackRefs() {
      DynamicAttachmentBase.addBackRefs(this);
   }

   public void removeBackRefs() {
      DynamicAttachmentBase.removeBackRefs(this);
   }

}
