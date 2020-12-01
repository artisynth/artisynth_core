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
import maspack.geometry.InverseDistanceWeights;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Class to attach a frame to a FEM and it's coordinate frame.
 */
public class FrameFem3dAttachment extends FrameAttachment {

   private static final double DOUBLE_PREC = 1e-16;

   private boolean myMasterBlockInWorldCoords = true;
   private boolean myUseProcrustes = false;

   // myCoords is used when frame is associate with an element
   private Vector3d myCoords = new Vector3d(); 
   private FemElement3dBase myElement;
   private FemNode3d[] myNodes;
   private double[] myWeights;
   private RotationMatrix3d myRFD = new RotationMatrix3d();

   // initialized when element is assigned
   private FemModel3d myFem;

   private IntegrationPoint3d myIpnt;
   private IntegrationData3d myData;
   private Vector3d[] myGNX;

   // updated in updatePosStates()
   private Matrix3d myInvB = new Matrix3d();
   private Matrix3d myB = new Matrix3d();
   private RigidTransform3d myTFM = new RigidTransform3d();
   // polar decomposition of the current deformation gradient
   private PolarDecomposition3d myPolard = new PolarDecomposition3d();

   // updated only when needed demand
   private Matrix3d myDotB = new Matrix3d();

   // used for implementing constraints
   private double[] myLam = new double[6];
   private Twist myErr = new Twist();

   public FrameFem3dAttachment () {
      myRFD = new RotationMatrix3d();
   }

   public FrameFem3dAttachment (Frame frame) {
      myRFD = new RotationMatrix3d();
      if (frame instanceof DeformableBody) {
         throw new IllegalArgumentException (
            "Deformable bodies not supported for FrameFem3dAttachment");
      }
      myFrame = frame;
   }

   public FrameFem3dAttachment (Frame frame, FemModel3d fem) {
      this(frame);
      setFromFem (frame.getPose(), fem);
   }

   @Override
   public boolean isFlexible() {
      return true;
   }

   private FemModel3d getFemModel (ModelComponentBase comp) {
      ModelComponent gparent = comp.getGrandParent();
      if (gparent instanceof FemModel3d) {
         return (FemModel3d)gparent;
      }
      return null;
   }

   private FemModelFrame getFemFrameX() {
      if (myFem.usingAttachedRelativeFrame()) {
         return myFem.getFrame();
      }
      else {
         return null;
      }
   }

   private FemModelFrame getFemFrame() {
      return null;
   }

   // =========== Constraint code - need to finish =======

   public void getBilateralSizes (VectorNi sizes) {
      sizes.append (3);
      sizes.append (3);
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

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

   protected void updateDotB (Vector3d wD) {
      // update dot P
      Matrix3d dF = new Matrix3d();
      for (int i=0; i<myNodes.length; i++) {
         dF.addOuterProduct (myNodes[i].getVelocity(), myGNX[i]);
      }
      Matrix3d dotP = new Matrix3d();
      Matrix3d wP = new Matrix3d();
      Matrix3d H = new Matrix3d();
      myPolard.getH(H);
      wP.crossProduct (wD, H);
      dotP.mulTransposeLeft (myPolard.getR(), dF);
      dotP.sub (wP);
      computeB (myDotB, dotP);
   }

   public double updateConstraints (double t, int flags) {

      updatePosBasedVariables();

      // compute the error twist
      RigidTransform3d T = new RigidTransform3d();
      computeFrame (T); // compute TFM
      myTFM.set (T);

      myErr.set (T);
      return 0;
   }
   
   // =========== end of constraint code

   private void initializeGNX (Matrix3d invJ0) {

      Vector3d[] GNs = myIpnt.GNs;
      myGNX = new Vector3d[GNs.length];
      for (int i=0; i<GNs.length; i++) {
         myGNX[i] = new Vector3d();
         invJ0.mulTranspose (myGNX[i], GNs[i]);
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

   public FemNode[] getNodes() {
      return myNodes;
   }

   protected void doSetFromElement (
      FemElement3dBase elem, Vector3d coords, boolean maybeConnected) {

      if (maybeConnected) {
         removeBackRefsIfConnected();
      }
      myIpnt = IntegrationPoint3d.create (
         elem, coords.x, coords.y, coords.z, 1.0);
      myData = new IntegrationData3d();
      myData.computeInverseRestJacobian (myIpnt, elem.getNodes());

      VectorNd N = myIpnt.getShapeWeights();
      myWeights = Arrays.copyOf (N.getBuffer(), N.size());
      myNodes = Arrays.copyOf (elem.getNodes(), elem.numNodes());

      if (myUseProcrustes) {
         initializeGNX (elem.getNodes(), myWeights);
      }
      else {
         initializeGNX (myData.myInvJ0);
      }
      myElement = elem;
      myFem = getFemModel (elem);
      invalidateMasters();
      if (maybeConnected) {
         addBackRefsIfConnected();
      }
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   protected boolean doSetFromNodes (
      FemNode3d[] nodes, double[] weights, boolean maybeConnected) {

      if (maybeConnected) {
         removeBackRefsIfConnected();
      }
      myFem = null;
      for (int i=0; i<nodes.length; i++) {
         FemModel3d fem = getFemModel (nodes[i]);
         if (fem == null) {
            throw new IllegalArgumentException (
               "Node "+i+" does not belong to a FEM model");
         }
         if (myFem == null) {
            myFem = fem;
         }
         else if (myFem != fem)  {
            throw new IllegalArgumentException (
               "Nodes do not all belong to a common FEM model");
         }
      }
      
      myIpnt = null;
      myData = null;
      myWeights = Arrays.copyOf (weights, weights.length);
      myNodes = Arrays.copyOf (nodes, nodes.length);         

      boolean status = initializeGNX (nodes, weights);
      myElement = null;
      invalidateMasters();
      if (maybeConnected) {
         addBackRefsIfConnected();
      }
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      return status;
   }

   public boolean setFromElement (RigidTransform3d T, FemElement3dBase elem) {
      Vector3d coords = new Vector3d();
      boolean converged = 
         elem.getNaturalCoordinates (coords, new Point3d(T.p), 1000) >= 0;

      myCoords.set (coords);
      doSetFromElement (elem, coords, /*maybeConnected=*/true);

      updateDeformationGradient();
      myRFD.mulInverseLeft (myPolard.getR(), T.R);
      return converged;
   }
   
   public boolean setFromNodes (
      RigidTransform3d TFW, 
      Collection<FemNode3d> nodes, VectorNd weights) {

      return setFromNodes (
         TFW, nodes.toArray(new FemNode3d[0]), weights.getBuffer());
   }

   public boolean setFromNodes (
      RigidTransform3d TFW, Collection<FemNode3d> nodes) {

      return setFromNodes (TFW, nodes.toArray(new FemNode3d[0]));
   }

   public boolean setFromNodes (
      RigidTransform3d TFW, FemNode3d[] nodes, double[] weights) {
      
      if (nodes.length > weights.length) {
         throw new IllegalArgumentException (
         "Number of weights is less than the number of nodes");
      }      
      boolean status = doSetFromNodes (nodes, weights, /*maybeConnected=*/true);
      updateDeformationGradient();
      myRFD.mulInverseLeft (myPolard.getR(), TFW.R);
      return status;
   }
   
   public boolean setFromNodes (RigidTransform3d TFW, FemNode3d[] nodes) {
      
      ArrayList<Vector3d> support = 
      new ArrayList<Vector3d>(nodes.length);
      for (int i=0; i<nodes.length; i++) {
         support.add (nodes[i].getPosition());
      }
      InverseDistanceWeights idweights = 
      new InverseDistanceWeights (1, 1, /*normalize=*/true);
      VectorNd weights = new VectorNd();
      boolean status = idweights.compute (weights, TFW.p, support);
      doSetFromNodes (nodes, weights.getBuffer(), /*maybeConnected=*/true);
      updateDeformationGradient();
      myRFD.mulInverseLeft (myPolard.getR(), TFW.R);
      return status;
   }    

   public boolean debug = false;

   protected boolean resetFromElement (
      RigidTransform3d T, FemElement3dBase elem) {

      // first, see if we need to reset the weights ...

      Vector3d coords = new Vector3d();
      if (elem.getNaturalCoordinates (
             coords, new Point3d(T.p), 50) < 0) {
         throw new NumericalException (
            "Can't find natural coords for "+T.p+
            " in element "+elem.getNumber());
      }
      // if (debug) {
      //    System.out.println ("coords=" + coords);
      // }
      
      if (!elem.coordsAreInside(coords)) {
         return false;
      }
      myCoords.set (coords);
      doSetFromElement (elem, coords, /*maybeConnected=*/true);
      updateDeformationGradient();
      myRFD.mulInverseLeft (myPolard.getR(), T.R);
      return true;
   }

   public void setFromFem (RigidTransform3d TFW, FemModel3d fem) {
      setFromFem (TFW, fem, /*project=*/true);
   }

   public boolean setFromFem (
      RigidTransform3d TFW, FemModel3d fem, boolean project) {
      Point3d loc = new Point3d();
      Point3d pos = new Point3d(TFW.p);
      FemElement3dBase elem = fem.findNearestElement (loc, pos);
      if (!loc.equals (pos)) {
         if (!project) {
            return false;
         }
         TFW = new RigidTransform3d (TFW);
         TFW.p.set (loc);
      }
      setFromElement (TFW, elem);
      return true;
   }

   /**
    * Computes (tr(P)I - P), where P is the symmetric part of the polar
    * decomposition of F
    */
   private void computeB (Matrix3d B, Matrix3d P) {
      B.negate (P);
      double tr = P.trace();
      B.m00 += tr;
      B.m11 += tr;
      B.m22 += tr;
   }

   public void updateFramePose (boolean frameRelative) {
      RigidTransform3d T = new RigidTransform3d();
      computeFrame (T);
      if (!frameRelative) {
         T.R.mul (myRFD);
      }
      else {
         // multiply by existing frame to get the new pose
         T.mul (myFrame.getPose(), T);
      }
      myFrame.setPose (T);
   }

   @Override
   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters(masters);
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         masters.add (femFrame);
      }
      for (int i=0; i<myNodes.length; i++) {
         masters.add (myNodes[i]);
      }      
   }


   @Override
   protected int updateMasterBlocks() {
      int idx = super.updateMasterBlocks();
      if (idx != 0) {
         throw new UnsupportedOperationException (
            "Unsupported case: master block added in FrameAttachment base class");
      }
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         Vector3d pFMw = new Vector3d();
         // rotate TFM.p into world coords
         pFMw.transform (femFrame.getPose().R, myTFM.p); 
         computeFrameFrameJacobian (
            myMasterBlocks[idx++], pFMw, /*RBW=*/null);
      }
      RotationMatrix3d RE = new RotationMatrix3d();
      RE.mulInverseRight (myTFM.R, myRFD);

      Matrix3d M = new Matrix3d();
      for (int i=0; i<myNodes.length; i++) {
         Matrix3x6Block blk = (Matrix3x6Block)myMasterBlocks[idx++];
         double Ni = myWeights[i];

         M.setDiagonal (Ni, Ni, Ni);
         if (myMasterBlockInWorldCoords) {
            // matrix should rotate frame force from world coords
            if (femFrame != null) {
               M.mulTransposeRight (M, femFrame.getPose().R);
            }
         }

         blk.setSubMatrix00 (M);

         M.crossProduct (myGNX[i], myInvB);
         M.scale (-1);
         M.mul (RE, M);
         if (myMasterBlockInWorldCoords) {
            // matrix should rotate frame force from world coords
            M.mulTransposeRight (M, RE);
            if (femFrame != null) {
               M.mulTransposeRight (M, femFrame.getPose().R);
            }
         }
         else {
            // matrix should rotate frame force from frame coords
            M.mul (M, myRFD);
         }
         blk.setSubMatrix03 (M);
      }
      return idx;      
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

   private void updatePosBasedVariables () {
      Matrix3d B = new Matrix3d();
      Matrix3d H = new Matrix3d();
      updateDeformationGradient();
      myPolard.getH(H);
      computeB (B, H);
      myInvB.invert (B);
      myB.set (B);
   }

   @Override
   public void updatePosStates() {
      RigidTransform3d TAW = new RigidTransform3d();
      updatePosBasedVariables ();
      computeFrame (TAW); // start by computing TFM
      myTFM.set (TAW);
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         TAW.mul (femFrame.getPose(), TAW);
      }
      if (myFrame != null) {
         myFrame.setPose (TAW);
      }
      updateMasterBlocks();
   }

   @Override
   public void updateVelStates() {
      Twist velAW = new Twist();
      computeVelocity (velAW);
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         RigidTransform3d TFW = femFrame.getPose();
         // rotate velocity from F into W
         velAW.transform (TFW.R);
         // add effects from velFW
         Twist velFW = femFrame.getVelocity();
         Vector3d pFMw = new Vector3d();
         pFMw.transform (TFW.R, myTFM.p); // rotate TFM.p into world coords
         velAW.v.crossAdd (velFW.w, pFMw, velAW.v);
         velAW.add (velFW);
      }
      myFrame.setVelocity (velAW);
   }

   @Override
   public void updateAttachment() {
      if (myFrame != null) {
         setCurrentTFW (myFrame.getPose());
      }
   }
   
   @Override
   public boolean setCurrentTFW (RigidTransform3d TFW) {
      if (myElement != null) {
         if (!resetFromElement (TFW, myElement)) {
            FemModel3d fem = getFemModel (myElement);
            if (fem == null) {
               throw new InternalErrorException (
                  "FrameFem3dAttachment has an assigned element but no FEM");
            }
            setFromFem (TFW, fem);
            updatePosStates();
            return true;
         }
         else {
            return false;
         }
      }
      else {
         updateDeformationGradient();
         myRFD.mulInverseLeft (myPolard.getR(), TFW.R);
         return true;
      }
   }

   private void computeFramePosition (Vector3d p) {
      p.setZero();
      for (int i=0; i<myNodes.length; i++) {
         p.scaledAdd (myWeights[i], myNodes[i].getPosition());
      }
   }
   
   public void computeFrame (RigidTransform3d TFM) {
      computeFramePosition (TFM.p);
      TFM.R.mul (myPolard.getR(), myRFD);
   }

   public void computeRestFrame (RigidTransform3d TFM) {
      TFM.setIdentity();
      for (int i=0; i<myNodes.length; i++) {
         TFM.p.scaledAdd (myWeights[i], myNodes[i].getRestPosition());
      }
      TFM.R.set (myRFD);
   }

   public void computeVelocity (Twist velFM) {

      velFM.setZero();
      for (int i=0; i<myNodes.length; i++) {
         velFM.v.scaledAdd (myWeights[i], myNodes[i].getVelocity());
      }
      RotationMatrix3d RE = new RotationMatrix3d();
      RE.mulInverseRight (myTFM.R, myRFD);
      Vector3d vloc = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         vloc.inverseTransform (RE, myNodes[i].getVelocity());
         velFM.w.crossAdd (myGNX[i], vloc, velFM.w);
      }
      myInvB.mul (velFM.w, velFM.w);
      velFM.w.transform (RE);
   }

   protected void computeNodeForce (
      Vector3d f, Matrix3x6Block blk, Wrench forceA) {

      // diagonal entries should all have the same weight value
      f.x = blk.m00*forceA.f.x;
      f.y = blk.m11*forceA.f.y;
      f.z = blk.m22*forceA.f.z;

      double mx = forceA.m.x;
      double my = forceA.m.y;
      double mz = forceA.m.z;

      f.x += blk.m03*mx + blk.m04*my + blk.m05*mz;
      f.y += blk.m13*mx + blk.m14*my + blk.m15*mz;
      f.z += blk.m23*mx + blk.m24*my + blk.m25*mz;
   }


   @Override
   public void applyForces() {
      Wrench forceA = myFrame.getForce();
      int idx = 0;
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         Wrench forceF = new Wrench(forceA);
         Vector3d pFMw = new Vector3d();
         // rotate TFM.p into world coords
         pFMw.transform (femFrame.getPose().R, myTFM.p); 
         forceF.m.crossAdd (pFMw, forceA.f, forceF.m);
         femFrame.addForce (forceF);
         idx++;
      }
      Vector3d f = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         Matrix3x6Block blk = (Matrix3x6Block)myMasterBlocks[idx++];
         computeNodeForce (f, blk, forceA);
         myNodes[i].addForce (f);
      }
   }

   @Override
   public boolean getDerivative (double[] buf, int idx) {
      Twist vel = new Twist();
      Twist dvel = new Twist();
      getCurrentWorldVel (vel, dvel);
      buf[idx  ] = dvel.v.x;
      buf[idx+1] = dvel.v.y;
      buf[idx+2] = dvel.v.z;
      buf[idx+3] = dvel.w.x;
      buf[idx+4] = dvel.w.y;
      buf[idx+5] = dvel.w.z;
      return true;
   }

   @Override
   public void addMassToMasters() {
      if (myFrame.getEffectiveMass() != 0) {
         if (myFrame instanceof RigidBody) {
            RigidBody body = (RigidBody)myFrame;
            SpatialInertia MB = new SpatialInertia(body.getEffectiveInertia());
            double mass = MB.getMass();
            PointFem3dAttachment.addMassToNodeMasters (
               myNodes, myWeights, mass);
            body.subEffectiveInertia (MB);
         }
         else {
            throw new UnsupportedOperationException (
               "addMassToMasters() only supported for rigid bodies");
         }
      }
   }

   @Override
   public void getCurrentTFW (RigidTransform3d TFW) {

      computeFrame (TFW); 
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         TFW.mul (femFrame.getPose(), TFW);
      }
   }

   @Override
   public void getUndeformedTFW (RigidTransform3d TFW) {
      computeRestFrame (TFW); 
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         TFW.mul (femFrame.getPose(), TFW);
      }
   }

   private void zeroArray (double[] vec) {
      for (int i=0; i<vec.length; i++) {
         vec[i] = 0;
      }
   }

   public void getCurrentVel (Twist vel, Twist dg) {
      Frame femFrame = getFemFrame();
      Twist dgx = new Twist();
      getCurrentWorldVel (vel, null);
      if (femFrame != null) {
         vel.inverseTransform (femFrame.getPose().R);
      }
      vel.inverseTransform (myTFM.R);
      if (dg != null) {
         Twist fvel = new Twist(); // velocity due to frame, if present
         if (femFrame != null) {
            femFrame.getBodyVelocity (fvel);
            fvel.inverseTransform (myTFM);
         }
         Twist evel = new Twist(); // twist due to elastic deformation
         evel.sub (vel, fvel);
         dg.v.cross (evel.v, evel.w);
         if (femFrame != null) {
            Vector3d pFMinF = new Vector3d();
            Vector3d tmp = new Vector3d();
            pFMinF.inverseTransform (myTFM.R, myTFM.p);
            dg.v.crossAdd (fvel.v, vel.w, dg.v);
            tmp.cross (fvel.w, pFMinF); 
            tmp.cross (fvel.w, tmp);
            dg.v.add (tmp);
            dg.v.crossAdd (fvel.w, evel.v, dg.v);
         }
         Vector3d wD = new Vector3d();
         wD.transform (myRFD, evel.w);
         computeAngularVelDeriv (dg.w, wD);
         dg.w.inverseTransform (myRFD);
         if (femFrame != null) {
            dg.w.crossAdd (fvel.w, evel.w, dg.w);
         }
      }
   }   

   private void computeAngularVelDeriv (Vector3d dw, Vector3d wD) {

      dw.setZero();
      RotationMatrix3d RE = new RotationMatrix3d();
      RE.mulInverseRight (myTFM.R, myRFD);
      Vector3d vloc = new Vector3d();
      
      Vector3d wXv = new Vector3d();
      updateDotB (wD);
      Vector3d term2 = new Vector3d();
      
      for (int i=0; i<myNodes.length; i++) {
         vloc.inverseTransform (RE, myNodes[i].getVelocity());
         wXv.cross (wD, vloc);
         term2.crossAdd (myGNX[i], wXv, term2);
         dw.crossAdd (myGNX[i], vloc, dw);
      }
      
      myInvB.mul (dw, dw);
      myDotB.mul (dw, dw);
      dw.add (term2);
      myInvB.mul (dw, dw);
      dw.negate();
   }

   /**
    * For testing only ...
    */
   public void getCurrentVelX (Twist vel, Twist dg) {

      RotationMatrix3d RFM = myTFM.R;
      
      vel.setZero();
      for (int i=0; i<myNodes.length; i++) {
         vel.v.scaledAdd (myWeights[i], myNodes[i].getVelocity());
      }
      vel.v.inverseTransform (RFM);

      RotationMatrix3d RE = new RotationMatrix3d();
      RE.mulInverseRight (RFM, myRFD);
      Vector3d vloc = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         vloc.inverseTransform (RE, myNodes[i].getVelocity());
         vel.w.crossAdd (myGNX[i], vloc, vel.w);
      }
      myInvB.mul (vel.w, vel.w);
      vel.w.inverseTransform (myRFD);

      // now add the frame velocity of the FEM 
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         Twist fvel = new Twist();
         femFrame.getBodyVelocity (fvel);
         fvel.inverseTransform (myTFM);
         vel.add (fvel);
      }
   }   

   public void getCurrentWorldVel (Twist vel, Twist dg) {

      double[] wvel = new double[6];
      double[] nvel = new double[3];
      Twist fvel = new Twist(); // velocity due to frame, if present
      int idx = 0;
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         MatrixBlock blk = myMasterBlocks[idx++];
         double[] fvelx = new double[6];
         femFrame.getVelState (fvelx, 0);
         blk.mulTransposeAdd (wvel, 0, fvelx, 0);
         fvel.set (wvel);
      }
      for (int i=0; i<myNodes.length; i++) {
         MatrixBlock blk = myMasterBlocks[idx++];
         myNodes[i].getVelState (nvel, 0);
         blk.mulTransposeAdd (wvel, 0, nvel, 0);
      }
      vel.set (wvel);
      // assumes that master blocks are in world coordinates
      if (dg != null) {
         Twist evel = new Twist();
         if (femFrame != null) {
            evel.sub (vel, fvel);
            dg.v.cross (fvel.w, evel.v);
            dg.v.scale (2);
            Vector3d pFMinW = new Vector3d(myTFM.p);
            pFMinW.transform (femFrame.getPose().R);
            Vector3d xvec = new Vector3d();
            xvec.cross (fvel.w, pFMinW);
            dg.v.crossAdd (fvel.w, xvec, dg.v);  
         }
         else {
            evel.set (vel);
            dg.v.setZero();
         }
         Vector3d wD = new Vector3d(evel.w);
         if (femFrame != null) {
            wD.inverseTransform (femFrame.getPose().R);
         }
         wD.inverseTransform (myPolard.getR());
         computeAngularVelDeriv (dg.w, wD);
         dg.w.transform (myPolard.getR());
         if (femFrame != null) {
            dg.w.transform (femFrame.getPose().R);
            dg.w.crossAdd (fvel.w, vel.w, dg.w);
         }
      }
   }   

   /**
    * For testing only ...
    */
   public void getCurrentWorldVelX (Twist vel, Twist dg) {

      RotationMatrix3d RFM = myTFM.R;
     
      vel.setZero();
      for (int i=0; i<myNodes.length; i++) {
         vel.v.scaledAdd (myWeights[i], myNodes[i].getVelocity());
      }
      RotationMatrix3d RE = new RotationMatrix3d();
      RE.mulInverseRight (RFM, myRFD);
      Vector3d vloc = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         vloc.inverseTransform (RE, myNodes[i].getVelocity());
         vel.w.crossAdd (myGNX[i], vloc, vel.w);
      }
      myInvB.mul (vel.w, vel.w);
      vel.w.transform (RE);

      // now add the frame velocity of the FEM 
      Frame femFrame = getFemFrame();
      if (femFrame != null) {
         vel.transform (femFrame.getPose().R);
         Twist fvel = new Twist();
         femFrame.getVelocity (fvel);
         vel.add (fvel);
         Vector3d pFMinW = new Vector3d(myTFM.p);
         pFMinW.transform (femFrame.getPose().R);
         vel.v.crossAdd (fvel.w, pFMinW, vel.v);
      }
   }   

   public RigidTransform3d getTFM() {
      return myTFM;
   }

   /**
    * {@inheritDoc}
    */
   public double getAverageMasterMass() {
      // Note quite sure what to do here. Do we want the mass of the whole
      // FEM? Assume yes for now ...
      return (myFem != null ? myFem.getMass() : 0);
   }

   /**
    * {@inheritDoc}
    */
   public double getAverageMasterInertia() {
      // Note quite sure what to do here. Do we want the inertia of the whole
      // FEM? Assume yes for now ...
      if (myFem != null) {
         RigidTransform3d T = new RigidTransform3d();
         computeFrame (T);
         PointList<FemNode3d> nodes = myFem.getNodes();
         Vector3d loc = new Vector3d();
         double sum = 0;
         for (int i=0; i<nodes.size(); i++) {
            FemNode3d n = nodes.get(i);
            loc.sub (n.getPosition(), T.p);
            sum += n.getMass()*loc.normSquared();
         }
         // returned value is 1/3 the trace of the inertia matrix
         return 2.0/3.0*sum;
      }
      else {
         return 0;
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "element", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "RFD")) {
         myRFD.scan (rtok);
         return true;         
      }
      else if (scanAttributeName (rtok, "nodes")) {
         tokens.offer (new StringToken ("nodes", rtok.lineno()));
         ScanWriteUtils.scanComponentsAndWeights (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "coords")) {
         myCoords.scan (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "element")) {
         FemElement3dBase elem = postscanReference (
            tokens, FemElement3dBase.class, ancestor);
         doSetFromElement (elem, myCoords, /*maybeConnected=*/false);
         return true;
      }
      else if (postscanAttributeName (tokens, "nodes")) {
         FemNode3d[] nodes = ScanWriteUtils.postscanReferences (
            tokens, FemNode3d.class, ancestor);
         double[] weights = (double[])tokens.poll().value();
         doSetFromNodes (nodes, weights, /*maybeConnected=*/false);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      if (myElement != null) {
         pw.println (
            "element="+ComponentUtils.getWritePathName (ancestor, myElement));
         pw.println ("coords=" + myCoords.toString (fmt));
      }
      else {
         pw.print ("nodes=");
         ScanWriteUtils.writeComponentsAndWeights (
            pw, fmt, myNodes, myWeights, ancestor);
      }
      int writeFormat = RigidTransform3d.AXIS_ANGLE_STRING;
      if (fmt.isFullPrecisionDouble()) {
         // need to do MATRIX_3X4_STRING since that's the only thing
         // that gives us full precision save/restore
         writeFormat = RigidTransform3d.MATRIX_3X4_STRING;
      }
      pw.println ("RFD=" + myRFD.toString (fmt, writeFormat));
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      // should probably make this a soft reference instead
      if (myElement != null) {
         refs.add (myElement);
      }
   }   

   /**
    * Nothing to do for scale distance, since attachment is based
    * on weights.
    */
   public void scaleDistance (double m) {
   }
   
   /**
    * Nothing to do for scale distance, since attachment is based
    * on weights.
    */
   public void transformGeometry (
      AffineTransform3dBase X, RigidTransform3d TFW) {
   }

   public FrameFem3dAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FrameFem3dAttachment a = (FrameFem3dAttachment)super.copy (flags, copyMap);

      // Need to implement if we want to support copying

      // a.myCoords = new Vector3d(myCoords);
      // a.myRFD = new RotationMatrix3d(myRFD);
      // if (myElement != null) {
      //    FemElement3d elem =
      //       (FemElement3d)ComponentUtils.maybeCopy (flags, copyMap, myElement);
      //    a.doSetFromElement (elem, a.myCoords);
      // }
      return a;
   }
}
