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
   private FemNode3d[] myNodes;
   private double[] myWeights;
   private IntegrationPoint3d myIpnt;
   private IntegrationData3d myData;

   private RotationMatrix3d myRC;
   private Frame myFrame;

   private Vector3d[] myGNX;
   private boolean myUseProcrustes = false;

   // B = (tr(P)I - P) R0^T, where P is the symmetric part of the
   // polar decomposition of F
   private Matrix3d myInvB = new Matrix3d();
   private Matrix3d myDotB = new Matrix3d();
   private Twist myErr = new Twist();
   // polar decomposition of the current deformation gradient
   private PolarDecomposition3d myPolard = new PolarDecomposition3d();

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
      for (int i=0; i<myNodes.length; i++) {
         int bk = myNodes[i].getLocalSolveIndex();
         if (bk != -1) {
            Matrix3x6Block blk = new Matrix3x6Block();
            double Ni = myWeights[i];
            blk.m00 = Ni;
            blk.m11 = Ni;
            blk.m22 = Ni;

            NxBinv.crossProduct (myGNX[i], myInvB);
            NxBinv.scale (-1);
            NxBinv.mulTransposeLeft (myRC, NxBinv);
            dgw.mulTransposeAdd (NxBinv, myNodes[i].getLocalVelocity(), dgw);

            NxBinv.mul (myRC);
            blk.setSubMatrix03 (NxBinv);
            GT.addBlock (bk, bj, blk);
         }
      }
      
      if (dg != null) {
         dg.set (numb+0, 0);
         dg.set (numb+1, 0);
         dg.set (numb+2, 0);

         // compute dgw = - inv(B) * dotB * dgw
         myDotB.mul (dgw, dgw);
         myInvB.mul (dgw, dgw);
         myRC.mulTranspose (dgw, dgw);
         dgw.scale (-1);
         
         //dgw.setZero();
         
         dg.set (numb+3, dgw.x);
         dg.set (numb+4, dgw.y);
         dg.set (numb+5, dgw.z);
      }
      numb += 6;
      return numb;
   }

   public int getBilateralInfo (
      ConstraintInfo[] ginfo, int idx) {

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

   private void updateDeformationGradient() {
      if (myElement == null || myUseProcrustes) {
         Matrix3d F = new Matrix3d();
         Vector3d pos = new Vector3d();
         for (int i=0; i<myNodes.length; i++) {
            pos.scaledAdd (myWeights[i], myNodes[i].getPosition());
         }
         for (int i=0; i<myNodes.length; i++) {
            Vector3d udef = new Vector3d();
            Vector3d gNX = myGNX[i];
            // XXX we should be able to use node positions directly without
            // subtracting pos
            udef.sub (myNodes[i].getPosition(), pos);
            F.addOuterProduct (
               udef.x, udef.y, udef.z, gNX.x, gNX.y, gNX.z);
         }
         myPolard.factor (F);         
      }
      else {
         Matrix3d F = new Matrix3d();
         myIpnt.computeGradient (F, myElement.getNodes(), myData.myInvJ0);
         myPolard.factor (F);
      }
   }

   public double updateConstraints (double t, int flags) {
      // update P
      Matrix3d B = new Matrix3d();
      Matrix3d H = new Matrix3d();
      updateDeformationGradient();
      myPolard.getH(H);
      computeB (B, H);
      myInvB.invert (B);

      // update dot P
      Matrix3d dF = new Matrix3d();
      for (int i=0; i<myNodes.length; i++) {
         dF.addOuterProduct (myNodes[i].getLocalVelocity(), myGNX[i]);
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
   
   public void getConstrainedComponents (HashSet<DynamicComponent> comps) {
      if (myNodes != null) {
         for (FemNode n : myNodes) {
            comps.add (n);
         }
      }
      if (myFrame != null) {
         comps.add (myFrame);
      }
   }

   private boolean hasFullRank (Vector3d[] vecs) {
      MatrixNd VT = new MatrixNd (vecs.length, 3);
      double[] vals = new double[3];
      for (int i=0; i<vecs.length; i++) {
         vecs[i].get(vals);
         VT.setRow (i, vals);
      }
      SVDecomposition svd = new SVDecomposition();
      svd.factor (VT);
      return svd.condition() < 1e12;      
   }
   
   private void initializeGNX (Matrix3d invJ0) {
      Vector3d[] GNs = myIpnt.GNs;
      myGNX = new Vector3d[GNs.length];
      for (int i=0; i<GNs.length; i++) {
         myGNX[i] = new Vector3d();
         invJ0.mulTranspose (myGNX[i], GNs[i]);
      }
   }

   private boolean initializeGNX (FemNode3d[] nodes, double[] weights) {
      myGNX = new Vector3d[nodes.length];
      Vector3d restPos = new Vector3d();
      for (int i=0; i<nodes.length; i++) {
         restPos.scaledAdd (weights[i], nodes[i].getRestPosition());
      }
      for (int i=0; i<nodes.length; i++) {
         Vector3d u = new Vector3d();
         u.sub (nodes[i].getRestPosition(), restPos);
         u.scale (weights[i]);
         myGNX[i] = u;
      }
      return hasFullRank (myGNX);
   }

   public FemElement3dBase getElement() {
      return myElement;
   }

   private FemModel3d getFemModel (ModelComponentBase comp) {
      ModelComponent gparent = comp.getGrandParent();
      if (gparent instanceof FemModel3d) {
         return (FemModel3d)gparent;
      }
      return null;
   }

   public void setFromElement (RigidTransform3d TFW, FemElement3dBase elem) {
      Vector3d coords = new Vector3d();
      if (elem.getNaturalCoordinates (coords, new Point3d(TFW.p), 1000) < 0) {
         throw new NumericalException (
            "Can't find natural coords for " +
            TFW.p + " in element " + elem.getNumber());
      }
      myIpnt = IntegrationPoint3d.create (
         elem, coords.x, coords.y, coords.z, 1.0);
      myData = new IntegrationData3d();
      myData.computeInverseRestJacobian (myIpnt, elem.getNodes());

      VectorNd N = myIpnt.getShapeWeights();
      myWeights = Arrays.copyOf (N.getBuffer(), N.size());
      myNodes = Arrays.copyOf (elem.getNodes(), elem.numNodes());

      myElement = elem;
      initializeGNX (myData.myInvJ0);

      updateDeformationGradient();
      myRC.mulInverseLeft (myPolard.getR(), TFW.R);
   }

   public void setFromNodes (
      RigidTransform3d TFW, Collection<FemNode3d> nodes, VectorNd weights) {

      int nnodes = nodes.size();
      if (nnodes != weights.size()) {
         throw new IllegalArgumentException (
            "'nodes' and 'weights' have different sizes: " +
            nnodes + " and " + weights.size());
      }
      myWeights = new double[nnodes];
      weights.get (myWeights);
      myNodes = nodes.toArray (new FemNode3d[0]);
      FemModel3d femodel = null;
      for (int i=0; i<nnodes; i++) {
         FemModel3d fem = getFemModel (myNodes[i]);
         if (fem == null) {
            throw new IllegalArgumentException (
               "Node "+i+" does not belong to a FEM model");
         }
         if (femodel == null) {
            femodel = fem;
         }
         else if (femodel != fem)  {
            throw new IllegalArgumentException (
               "Nodes do not all belong to a common FEM model");
         }
      }
      myIpnt = null;
      myData = null;
      myElement = null;

      boolean status = initializeGNX (myNodes, myWeights);
      //initMasterBlocks (weights);
      updateDeformationGradient();
      myRC.mulInverseLeft (myPolard.getR(), TFW.R);
   }

   public void computeFrame (RigidTransform3d T) {
      T.setIdentity();
      for (int i=0; i<myNodes.length; i++) {
         T.p.scaledAdd (myWeights[i], myNodes[i].getLocalPosition());
      }
      updateDeformationGradient();
      T.R.mul (myPolard.getR(), myRC);
   }

   /**
    * Computes the current frame velocity, in world coordinates.
    */
   public void computeVelocity (Twist vel) {

      Matrix3d B = new Matrix3d();
      vel.setZero();
      for (int i=0; i<myNodes.length; i++) {
         vel.v.scaledAdd (myWeights[i], myNodes[i].getVelocity());
      }
      updateDeformationGradient();
      Matrix3d H = new Matrix3d();
      myPolard.getH(H);
      computeB (B, H);
      myInvB.invert (B);

      RotationMatrix3d R = myFrame.getPose().R;
      Vector3d vloc = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         vloc.inverseTransform (R, myNodes[i].getVelocity());
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
      vel.setZero();
      for (int i=0; i<myNodes.length; i++) {
         vel.v.scaledAdd (myWeights[i], myNodes[i].getLocalVelocity());
      }
      updateDeformationGradient();
      Matrix3d H = new Matrix3d();
      myPolard.getH(H);
      computeB (B, H);
      myInvB.invert (B);

      Vector3d vloc = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         vloc.transform (myRC, myNodes[i].getLocalVelocity());
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

   public FrameFem3dConstraint (
      Frame frame, Collection<FemNode3d> nodes, VectorNd weights) {
      this();
      myFrame = frame;
      setFromNodes (frame.getPose(), nodes, weights);
   }
}
