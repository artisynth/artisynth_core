package artisynth.core.femmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

/**
 * Connects a FemNode3d to a FrameNode3d. Let R and p denote the rotation and
 * translation of the frame, vf and wf the translational and rotational frame
 * velocity (world coordinates), xw and vw the position and velocity of the
 * node (world coordinates), and xl and vl the position and velocity of the
 * frame node (frame coordinates). Then the position and velocity
 * relationships are given by
 * <pre>
 *   xw = R xl + p
 *
 *   vw = vf - [R xl] wf + R vl
 * </pre>
 */
public class NodeFrameNodeAttachment implements DynamicAttachment {
      
   protected FemNode3d myNode;
   protected FrameNode3d myFrameNode;
   protected DynamicComponent[] myMasters = null;
   protected Point3d myLocW;
   protected boolean mySlaveAffectsStiffnessP;

   public NodeFrameNodeAttachment (FemNode3d node, FrameNode3d frameNode) {
      myNode = node;
      myFrameNode = frameNode;
      myLocW = new Point3d(); // frame node position rotated to world coords
      myMasters = new DynamicComponent[] { myFrameNode.myFrame, myFrameNode };
   }

   public boolean slaveAffectsStiffness() {
      return mySlaveAffectsStiffnessP;
   }
   
   public void setSlaveAffectsStiffness (boolean affects) {
      mySlaveAffectsStiffnessP = affects;
   }
   
   public void updateAttachment() {
   }

   public void updatePosStates() {
      RigidTransform3d TFW = myFrameNode.myFrame.getPose();
      Point3d framePos = myFrameNode.getPosition();
      myNode.getPosition().transform (TFW, framePos);

      myLocW.transform (TFW.R, framePos);
   }

   public void updateVelStates() {

      RotationMatrix3d R = myFrameNode.myFrame.getPose().R;
      Twist fvel = myFrameNode.myFrame.getVelocity();
      Vector3d vel = new Vector3d();
      Vector3d tmp = new Vector3d();

      vel.transform (R, myFrameNode.getVelocity());
      tmp.negate (myFrameNode.getPosition());
      tmp.transform (R);
      vel.crossAdd (tmp, fvel.w, vel);
      vel.add (fvel.v);
      myNode.setVelocity (vel);
   }

   public void applyForces() {
      
      Wrench wr = new Wrench();
      Vector3d fw = myNode.getForce();
      RotationMatrix3d R = myFrameNode.myFrame.getPose().R;
      Vector3d tmp = new Vector3d();

      tmp.transform (R, myFrameNode.getPosition());
      wr.m.cross (tmp, fw);
      wr.f.set (fw);
      tmp.inverseTransform (R, fw);
      myFrameNode.addForce (tmp);
      myFrameNode.myFrame.addForce (wr);
   }

   public DynamicComponent getSlave() {
      return myNode;
   }

   public void addMassToMasters() {
      double m = myNode.getEffectiveMass();
      if (m != 0) {
         myFrameNode.addEffectiveMass (m);
      }
      myNode.addEffectiveMass(-m);      
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

   public int numMasters() {
      return 2;
   }

   public void invalidateMasters() {
      throw new UnsupportedOperationException (
         "addMasterAttachment() not supported for FrameFemNode3d");
   }
   
   public void mulSubMG (MatrixBlock D, MatrixBlock M, int idx) {
      RotationMatrix3d R = myFrameNode.myFrame.getPose().R;
      if (idx == 0) {
         double lx = myLocW.x;
         double ly = myLocW.y;
         double lz = myLocW.z;
         // D must be n x 6, M must be n x 3
         if (D.isFixedSize() && M.isFixedSize()) {
            switch (D.rowSize()) {
               case 1: {
                  Matrix1x6 DF = (Matrix1x6)D;
                  Matrix1x3 MF = (Matrix1x3)M;

                  DF.m00 += MF.m00;
                  DF.m01 += MF.m01;
                  DF.m02 += MF.m02;

                  DF.m03 += (ly*MF.m02 - lz*MF.m01);
                  DF.m04 += (lz*MF.m00 - lx*MF.m02);
                  DF.m05 += (lx*MF.m01 - ly*MF.m00);
                  break;
               }
               case 2: {
                  Matrix2x6 DF = (Matrix2x6)D;
                  Matrix2x3 MF = (Matrix2x3)M;

                  DF.m00 += MF.m00;
                  DF.m01 += MF.m01;
                  DF.m02 += MF.m02;

                  DF.m10 += MF.m10;
                  DF.m11 += MF.m11;
                  DF.m12 += MF.m12;

                  DF.m03 += (ly*MF.m02 - lz*MF.m01);
                  DF.m04 += (lz*MF.m00 - lx*MF.m02);
                  DF.m05 += (lx*MF.m01 - ly*MF.m00);

                  DF.m13 += (ly*MF.m12 - lz*MF.m11);
                  DF.m14 += (lz*MF.m10 - lx*MF.m12);
                  DF.m15 += (lx*MF.m11 - ly*MF.m10);
                  break;
               }
               case 3: {
                  Matrix3x6 DF = (Matrix3x6)D;
                  Matrix3d MF = (Matrix3d)M;

                  DF.m00 += MF.m00;
                  DF.m01 += MF.m01;
                  DF.m02 += MF.m02;

                  DF.m10 += MF.m10;
                  DF.m11 += MF.m11;
                  DF.m12 += MF.m12;

                  DF.m20 += MF.m20;
                  DF.m21 += MF.m21;
                  DF.m22 += MF.m22;

                  DF.m03 += (ly*MF.m02 - lz*MF.m01);
                  DF.m04 += (lz*MF.m00 - lx*MF.m02);
                  DF.m05 += (lx*MF.m01 - ly*MF.m00);

                  DF.m13 += (ly*MF.m12 - lz*MF.m11);
                  DF.m14 += (lz*MF.m10 - lx*MF.m12);
                  DF.m15 += (lx*MF.m11 - ly*MF.m10);

                  DF.m23 += (ly*MF.m22 - lz*MF.m21);
                  DF.m24 += (lz*MF.m20 - lx*MF.m22);
                  DF.m25 += (lx*MF.m21 - ly*MF.m20);
                  break;
               }
               case 6: {
                  Matrix6d DF = (Matrix6d)D;
                  Matrix6x3 MF = (Matrix6x3)M;

                  DF.m00 += MF.m00;
                  DF.m01 += MF.m01;
                  DF.m02 += MF.m02;

                  DF.m10 += MF.m10;
                  DF.m11 += MF.m11;
                  DF.m12 += MF.m12;

                  DF.m20 += MF.m20;
                  DF.m21 += MF.m21;
                  DF.m22 += MF.m22;

                  DF.m30 += MF.m30;
                  DF.m31 += MF.m31;
                  DF.m32 += MF.m32;

                  DF.m40 += MF.m40;
                  DF.m41 += MF.m41;
                  DF.m42 += MF.m42;

                  DF.m50 += MF.m50;
                  DF.m51 += MF.m51;
                  DF.m52 += MF.m52;

                  DF.m03 += (ly*MF.m02 - lz*MF.m01);
                  DF.m04 += (lz*MF.m00 - lx*MF.m02);
                  DF.m05 += (lx*MF.m01 - ly*MF.m00);

                  DF.m13 += (ly*MF.m12 - lz*MF.m11);
                  DF.m14 += (lz*MF.m10 - lx*MF.m12);
                  DF.m15 += (lx*MF.m11 - ly*MF.m10);

                  DF.m23 += (ly*MF.m22 - lz*MF.m21);
                  DF.m24 += (lz*MF.m20 - lx*MF.m22);
                  DF.m25 += (lx*MF.m21 - ly*MF.m20);

                  DF.m33 += (ly*MF.m32 - lz*MF.m31);
                  DF.m34 += (lz*MF.m30 - lx*MF.m32);
                  DF.m35 += (lx*MF.m31 - ly*MF.m30);

                  DF.m43 += (ly*MF.m42 - lz*MF.m41);
                  DF.m44 += (lz*MF.m40 - lx*MF.m42);
                  DF.m45 += (lx*MF.m41 - ly*MF.m40);

                  DF.m53 += (ly*MF.m52 - lz*MF.m51);
                  DF.m54 += (lz*MF.m50 - lx*MF.m52);
                  DF.m55 += (lx*MF.m51 - ly*MF.m50);
                  break;
               }
            }
         }
         else {
            for (int k=0; k<D.rowSize(); k++) {
               double Bk0 = M.get(k, 0);
               double Bk1 = M.get(k, 1);
               double Bk2 = M.get(k, 2);

               D.set (k, 0, D.get(k, 0) + Bk0);
               D.set (k, 1, D.get(k, 1) + Bk1);
               D.set (k, 2, D.get(k, 2) + Bk2);

               D.set (k, 3, D.get(k, 3) + (ly*Bk2 - lz*Bk1));
               D.set (k, 4, D.get(k, 4) + (lz*Bk0 - lx*Bk2));
               D.set (k, 5, D.get(k, 5) + (lx*Bk1 - ly*Bk0));
            }
         }
      }
      else if (idx == 1) {
         D.mulAdd (M, R);
      }
   }

   public MatrixBlock getGT (int idx) {
      if (idx == 0) {
         Matrix6x3Block blk = new Matrix6x3Block();
         blk.m00 = -1;
         blk.m11 = -1;
         blk.m22 = -1;

         blk.m31 = myLocW.z; blk.m32 = -myLocW.y;
         blk.m40 = -myLocW.z; blk.m42 = myLocW.x;
         blk.m50 = myLocW.y; blk.m51 = -myLocW.x;
         return blk;
      }
      else if (idx == 1) {
         Matrix3x3Block blk = new Matrix3x3Block();
         blk.transpose (myFrameNode.myFrame.getPose().R);
         blk.negate();
         return blk;
      }
      else {
         throw new IllegalArgumentException (
            "Attachment has two masters so idx must be either 0 or 1");
      }
   }

   public void mulSubGTM (MatrixBlock D, MatrixBlock M, int idx) {
      RotationMatrix3d R = myFrameNode.myFrame.getPose().R;
      if (idx == 0) {
         double lx = myLocW.x;
         double ly = myLocW.y;
         double lz = myLocW.z;
         // D must be 6 x n, M must be 3 x n
         if (D.isFixedSize() && M.isFixedSize()) {
            switch (D.colSize()) {
               case 1: {
                  Matrix6x1 DF = (Matrix6x1)D;
                  Matrix3x1 MF = (Matrix3x1)M;

                  DF.m00 += MF.m00;
                  DF.m10 += MF.m10;
                  DF.m20 += MF.m20;

                  DF.m30 += (ly*MF.m20 - lz*MF.m10);
                  DF.m40 += (lz*MF.m00 - lx*MF.m20);
                  DF.m50 += (lx*MF.m10 - ly*MF.m00);
                  break;
               }
               case 2: {
                  Matrix6x2 DF = (Matrix6x2)D;
                  Matrix3x2 MF = (Matrix3x2)M;

                  DF.m00 += MF.m00;
                  DF.m10 += MF.m10;
                  DF.m20 += MF.m20;

                  DF.m01 += MF.m01;
                  DF.m11 += MF.m11;
                  DF.m21 += MF.m21;

                  DF.m30 += (ly*MF.m20 - lz*MF.m10);
                  DF.m40 += (lz*MF.m00 - lx*MF.m20);
                  DF.m50 += (lx*MF.m10 - ly*MF.m00);

                  DF.m31 += (ly*MF.m21 - lz*MF.m11);
                  DF.m41 += (lz*MF.m01 - lx*MF.m21);
                  DF.m51 += (lx*MF.m11 - ly*MF.m01);
                  break;
               }
               case 3: {
                  Matrix6x3 DF = (Matrix6x3)D;
                  Matrix3d MF = (Matrix3d)M;

                  DF.m00 += MF.m00;
                  DF.m10 += MF.m10;
                  DF.m20 += MF.m20;

                  DF.m01 += MF.m01;
                  DF.m11 += MF.m11;
                  DF.m21 += MF.m21;

                  DF.m02 += MF.m02;
                  DF.m12 += MF.m12;
                  DF.m22 += MF.m22;

                  DF.m30 += (ly*MF.m20 - lz*MF.m10);
                  DF.m40 += (lz*MF.m00 - lx*MF.m20);
                  DF.m50 += (lx*MF.m10 - ly*MF.m00);

                  DF.m31 += (ly*MF.m21 - lz*MF.m11);
                  DF.m41 += (lz*MF.m01 - lx*MF.m21);
                  DF.m51 += (lx*MF.m11 - ly*MF.m01);

                  DF.m32 += (ly*MF.m22 - lz*MF.m12);
                  DF.m42 += (lz*MF.m02 - lx*MF.m22);
                  DF.m52 += (lx*MF.m12 - ly*MF.m02);
                  break;
               }
               case 6: {
                  Matrix6d DF = (Matrix6d)D;
                  Matrix3x6 MF = (Matrix3x6)M;

                  DF.m00 += MF.m00;
                  DF.m10 += MF.m10;
                  DF.m20 += MF.m20;

                  DF.m01 += MF.m01;
                  DF.m11 += MF.m11;
                  DF.m21 += MF.m21;

                  DF.m02 += MF.m02;
                  DF.m12 += MF.m12;
                  DF.m22 += MF.m22;

                  DF.m03 += MF.m03;
                  DF.m13 += MF.m13;
                  DF.m23 += MF.m23;

                  DF.m04 += MF.m04;
                  DF.m14 += MF.m14;
                  DF.m24 += MF.m24;

                  DF.m05 += MF.m05;
                  DF.m15 += MF.m15;
                  DF.m25 += MF.m25;

                  DF.m30 += (ly*MF.m20 - lz*MF.m10);
                  DF.m40 += (lz*MF.m00 - lx*MF.m20);
                  DF.m50 += (lx*MF.m10 - ly*MF.m00);

                  DF.m31 += (ly*MF.m21 - lz*MF.m11);
                  DF.m41 += (lz*MF.m01 - lx*MF.m21);
                  DF.m51 += (lx*MF.m11 - ly*MF.m01);

                  DF.m32 += (ly*MF.m22 - lz*MF.m12);
                  DF.m42 += (lz*MF.m02 - lx*MF.m22);
                  DF.m52 += (lx*MF.m12 - ly*MF.m02);

                  DF.m33 += (ly*MF.m23 - lz*MF.m13);
                  DF.m43 += (lz*MF.m03 - lx*MF.m23);
                  DF.m53 += (lx*MF.m13 - ly*MF.m03);

                  DF.m34 += (ly*MF.m24 - lz*MF.m14);
                  DF.m44 += (lz*MF.m04 - lx*MF.m24);
                  DF.m54 += (lx*MF.m14 - ly*MF.m04);

                  DF.m35 += (ly*MF.m25 - lz*MF.m15);
                  DF.m45 += (lz*MF.m05 - lx*MF.m25);
                  DF.m55 += (lx*MF.m15 - ly*MF.m05);
                  break;
               }
            }
         }
         else {
            for (int k=0; k<D.colSize(); k++) {
               double B0k = M.get(0, k);
               double B1k = M.get(1, k);
               double B2k = M.get(2, k);

               D.set (0, k, D.get(0, k) + B0k);
               D.set (1, k, D.get(1, k) + B1k);
               D.set (2, k, D.get(2, k) + B2k);

               D.set (3, k, D.get(3, k) + (ly*B2k - lz*B1k));
               D.set (4, k, D.get(4, k) + (lz*B0k - lx*B2k));
               D.set (5, k, D.get(5, k) + (lx*B1k - ly*B0k));
            }
         }
      }
      else if (idx == 1) {
         D.mulTransposeLeftAdd (R, M);
      }
   }

   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {

      RotationMatrix3d R = myFrameNode.myFrame.getPose().R;
      if (idx == 0) {
         double lx = myLocW.x;
         double ly = myLocW.y;
         double lz = myLocW.z;

         double x0 = xbuf[xoff++];
         double x1 = xbuf[xoff++];
         double x2 = xbuf[xoff++];

         ybuf[yoff++] += x0;
         ybuf[yoff++] += x1;
         ybuf[yoff++] += x2;

         ybuf[yoff++] += (ly*x2 - lz*x1);
         ybuf[yoff++] += (lz*x0 - lx*x2);
         ybuf[yoff++] += (lx*x1 - ly*x0);
      }
      else if (idx == 1) {
         double x0 = xbuf[xoff++];
         double x1 = xbuf[xoff++];
         double x2 = xbuf[xoff++];

         ybuf[yoff++] += (R.m00*x0 + R.m10*x1 + R.m20*x2);
         ybuf[yoff++] += (R.m01*x0 + R.m11*x1 + R.m21*x2);
         ybuf[yoff++] += (R.m02*x0 + R.m12*x1 + R.m22*x2);
      }
   }

   public void addBackRefs() {
      DynamicAttachmentBase.addBackRefs(this);
   }

   public void removeBackRefs() {
      DynamicAttachmentBase.removeBackRefs(this);
   }


}

