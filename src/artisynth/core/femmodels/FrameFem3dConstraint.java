/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.render.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Class to manage the constraint between an FEM and it's coordinate frame.
 */
public class FrameFem3dConstraint extends ConstrainerBase {
   
   //private FemElement myElement;

   private FemElement3dBase myElement;
   private IntegrationPoint3d myIpnt;
   private IntegrationData3d myData;
   private RotationMatrix3d myRC;
   private Frame myFrame;

   private Vector3d[] myGNX;
   private Matrix3x6Block[] myMasterBlocks;

   // B = (tr(P)I - P) R0^T, where P is the symmetric part of the
   // polar decomposition of F
   private Matrix3d myInvB = new Matrix3d();
   private Matrix3d myDotB = new Matrix3d();
   private Twist myErr = new Twist();

   private double[] myLam = new double[6];

   public FrameFem3dConstraint() {
      myRC = new RotationMatrix3d();
   }

   public void getBilateralSizes (VectorNi sizes) {
      sizes.append (6);
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      
      int bj = GT.numBlockCols();
      GT.addCol (6);

      Vector3d dgw = new Vector3d();
      Matrix3d NxBinv = new Matrix3d();

      int bf = myFrame.getSolveIndex();
      FemNode3d[] nodes = myElement.getNodes();
      VectorNd N = myIpnt.getShapeWeights();
      for (int i=0; i<nodes.length; i++) {
         int bk = nodes[i].getLocalSolveIndex();
         if (bk != -1) {
            Matrix3x6Block blk = new Matrix3x6Block();
            double Ni = N.get(i);
            blk.m00 = Ni;
            blk.m11 = Ni;
            blk.m22 = Ni;

            NxBinv.crossProduct (myGNX[i], myInvB);
            NxBinv.scale (-1);
            NxBinv.mulTransposeLeft (myRC, NxBinv);
            dgw.mulTransposeAdd (NxBinv, nodes[i].getLocalVelocity(), dgw);

            NxBinv.mul (myRC);
            blk.setSubMatrix03 (NxBinv);
            GT.addBlock (bk, bj, blk);
         }
      }

      dg.set (numb++, 0);
      dg.set (numb++, 0);
      dg.set (numb++, 0);

      // compute dgw = - inv(B) * dotB * dgw
      myDotB.mul (dgw, dgw);
      myInvB.mul (dgw, dgw);
      myRC.mulTranspose (dgw, dgw);
      dgw.scale (-1);

      //dgw.setZero();

      dg.set (numb++, dgw.x);
      dg.set (numb++, dgw.y);
      dg.set (numb++, dgw.z);

      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {

      for (int i=0; i<6; i++) {
         ginfo[idx+i].compliance = 0;
         ginfo[idx+i].damping = 0;
         ginfo[idx+i].force = 0;
      }
      ginfo[idx++].dist = myErr.v.x;
      ginfo[idx++].dist = myErr.v.y;
      ginfo[idx++].dist = myErr.v.z;

      ginfo[idx++].dist = myErr.w.x;
      ginfo[idx++].dist = myErr.w.y;
      ginfo[idx++].dist = myErr.w.z;
      return idx;
   }

   public int setBilateralForces (VectorNd lam, double s, int idx) {
      for (int i=0; i<6; i++) {
         myLam[i] = lam.get(idx++)*s;
      }
      return idx;
   }

   public int getBilateralForces (VectorNd lam, int idx) {
      for (int i=0; i<6; i++) {
         lam.set (idx++, myLam[i]);
      }
      return idx;
   }
   
   public void zeroForces() {
      for (int i=0; i<6; i++) {
         myLam[i] = 0;
      }
   }

   /**
    * Computes (tr(P)I - P)
    */
   private void computeB (Matrix3d B, Matrix3d P) {
      B.negate (P);
      double tr = P.trace();
      B.m00 += tr;
      B.m11 += tr;
      B.m22 += tr;
   }

   public double updateConstraints (double t, int flags) {
      // update P
      Matrix3d B = new Matrix3d();
      Matrix3d H = new Matrix3d();
      VectorNd N = myIpnt.getShapeWeights();
      FemNode3d[] nodes = myElement.getNodes();
      Matrix3d F = new Matrix3d();
      myIpnt.computeGradient (F, myElement.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (F);
      polard.getH(H);
      computeB (B, H);
      myInvB.invert (B);

      // update dot P
      Matrix3d dF = new Matrix3d();
      for (int i=0; i<nodes.length; i++) {
         dF.addOuterProduct (nodes[i].getLocalVelocity(), myGNX[i]);
      }
      //dF.mulTransposeLeft (myRC, dF);
      // dot P is the symmetric part of dF
      Matrix3d dotP = new Matrix3d();
      dotP.transpose (dF);
      dotP.add (dF);
      dotP.scale (0.5);
      computeB (myDotB, dotP);

      // compute the error twist
      RigidTransform3d T = new RigidTransform3d();
      computeFrame (T);

      myErr.set (T);
      return 0;
   }
   
   public void getConstrainedComponents (List<DynamicComponent> list) {
      if (myElement != null) {
         for (FemNode n : myElement.getNodes()) {
            list.add (n);
         }
      }
   }
   
   private void computeBlocks (Matrix3d invJ0, RotationMatrix3d R0) {

      VectorNd N = myIpnt.getShapeWeights();
      Vector3d[] GNs = myIpnt.GNs;

      myGNX = new Vector3d[GNs.length];
      //myOmegaBlocks = new Matrix3x3Block[GNs.length];
      //myVBlocks = new Matrix3x3Block[GNs.length];
      myMasterBlocks = new Matrix3x6Block[GNs.length];
      for (int i=0; i<GNs.length; i++) {
         //Matrix3x3Block blk = new Matrix3x3Block();
         myGNX[i] = new Vector3d();
         invJ0.mulTranspose (myGNX[i], GNs[i]);

         Matrix3x6Block blkm = new Matrix3x6Block();
         blkm.m00 = N.get(i);
         blkm.m11 = N.get(i);
         blkm.m22 = N.get(i);
         myMasterBlocks[i] = blkm;
      }
   }

   public FemElement3dBase getElement() {
      return myElement;
   }

   public void setFromElement (RigidTransform3d T, FemElement3dBase elem) {
      Vector3d coords = new Vector3d();
      if (elem.getNaturalCoordinates (coords, new Point3d(T.p), 1000) < 0) {
         throw new NumericalException (
            "Can't find natural coords for "+T.p+" in element "+elem.getNumber());
      }
      myIpnt = IntegrationPoint3d.create (
         elem, coords.x, coords.y, coords.z, 1.0);
      myData = new IntegrationData3d();
      myData.computeInverseRestJacobian (myIpnt, elem.getNodes());

      Matrix3d F = new Matrix3d();
      myIpnt.computeGradient (F, elem.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (F);  
      myRC.mulInverseLeft (polard.getR(), T.R);

      //computeBlocks (myData.myInvJ0, myRC);
      computeBlocks (myData.myInvJ0, RotationMatrix3d.IDENTITY);

      myElement = elem;
   }

   public void computeFrame (RigidTransform3d T) {
      T.setIdentity();
      VectorNd N = myIpnt.getShapeWeights();
      FemNode3d[] nodes = myElement.getNodes();
      Matrix3d F = new Matrix3d();
      myIpnt.computeGradient (F, myElement.getNodes(), myData.myInvJ0);
      for (int i=0; i<nodes.length; i++) {
         T.p.scaledAdd (N.get(i), nodes[i].getLocalPosition());
      }
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (F);
      T.R.mul (polard.getR(), myRC);
   }

   /**
    * Computes the current frame velocity, in world coordinates.
    */
   public void computeVelocity (Twist vel) {

      Matrix3d B = new Matrix3d();
      VectorNd N = myIpnt.getShapeWeights();
      FemNode3d[] nodes = myElement.getNodes();

      vel.setZero();
      for (int i=0; i<nodes.length; i++) {
         vel.v.scaledAdd (N.get(i), nodes[i].getVelocity());
      }
      Matrix3d F = new Matrix3d();
      myIpnt.computeGradient (F, myElement.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (F);
      Matrix3d H = new Matrix3d();
      polard.getH(H);
      computeB (B, H);
      myInvB.invert (B);

      RotationMatrix3d R = myFrame.getPose().R;
      Vector3d vloc = new Vector3d();
      for (int i=0; i<nodes.length; i++) {
         vloc.inverseTransform (R, nodes[i].getVelocity());
         vloc.transform (myRC);
         vel.w.crossAdd (myGNX[i], vloc, vel.w);
      }
      myInvB.mul (vel.w, vel.w);
      vel.w.inverseTransform (myRC);
      vel.w.transform (R);
   }

   /**
    * Computes the current frame velocity, in frame coordinates.
    */
   public void computeFrameRelativeVelocity (Twist vel) {

      Matrix3d B = new Matrix3d();
      VectorNd N = myIpnt.getShapeWeights();
      FemNode3d[] nodes = myElement.getNodes();

      vel.setZero();
      for (int i=0; i<nodes.length; i++) {
         vel.v.scaledAdd (N.get(i), nodes[i].getLocalVelocity());
      }
      Matrix3d F = new Matrix3d();
      myIpnt.computeGradient (F, myElement.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (F);
      Matrix3d H = new Matrix3d();
      polard.getH(H);
      computeB (B, H);
      myInvB.invert (B);

      Vector3d vloc = new Vector3d();
      for (int i=0; i<nodes.length; i++) {
         vloc.transform (myRC, nodes[i].getLocalVelocity());
         vel.w.crossAdd (myGNX[i], vloc, vel.w);
      }
      myInvB.mul (vel.w, vel.w);
      vel.inverseTransform (myRC);
   }

   public void updateFramePose (boolean frameRelative) {
      RigidTransform3d T = new RigidTransform3d();
      computeFrame (T);
      if (!frameRelative) {
         //T.R.mul (myRC);
      }
      else {
         // multiply by existing frame to get the new pose
         T.mul (myFrame.getPose(), T);
      }
      myFrame.setPose (T);
   }

   public FrameFem3dConstraint (Frame frame, FemElement3dBase elem) {
      this();
      myFrame = frame;
      setFromElement (frame.getPose(), elem);
   }

   public void render (Renderer renderer, int flags) {
   }
}
