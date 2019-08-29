package artisynth.core.femmodels;

import java.io.*;
import java.util.List;
import java.util.Arrays;
import java.util.Deque;

import maspack.matrix.*;
import maspack.util.*;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class DirectorFem3dAttachment extends DynamicAttachmentBase {

   private FemElement3dBase myElement;
   private FemNode[] myNodes;
   private double[] myWeights;
   private Vector3d myCoords;

   private IntegrationPoint3d myIpnt;
   private IntegrationData3d myData;
   private Vector3d[] myGNX;

   protected FemNode3d myNode;
   protected Vector3d myLocDir = new Vector3d();

   // updated in updatePosStates()
   private Matrix3d myInvB = new Matrix3d();
   private Matrix3d myB = new Matrix3d();
   private RigidTransform3d myTFM = new RigidTransform3d();
   // polar decomposition of the current deformation gradient
   private PolarDecomposition3d myPolard = new PolarDecomposition3d();

   protected DynamicComponent[] myMasters;
   protected MatrixBlock[] myMasterBlocks;  
   protected MatrixBlock myMasterBlk;

   public DirectorFem3dAttachment () {
   }

   public DirectorFem3dAttachment (FemNode3d node) {
      myNode = node;
   }
   
   public BackNode3d getSlave() {
      return myNode.getBackNode();
   }

   public BackNode3d getBackNode() {
      return myNode.getBackNode();
   }

   public FemNode3d getNode() {
      return myNode;
   }

   void setNode (FemNode3d node) {
      myNode = node;
   }

   public Vector3d getLocDir() {
      return myLocDir;
   }

   void setLocDir (Vector3d locDir) {
      myLocDir = new Vector3d (locDir);
   }

   protected void setFromElement (
      FemNode[] nodes, VectorNd coords, Vector3d ncoords, FemElement3dBase elem) {
      removeBackRefsIfConnected();

      myIpnt = IntegrationPoint3d.create (
         elem, ncoords.x, ncoords.y, ncoords.z, 1.0);
      myData = new IntegrationData3d();
      myData.computeInverseRestJacobian (myIpnt, elem.getNodes());     

      initializeGNX (myData.myInvJ0);
      myCoords = new Vector3d (ncoords);
      myNodes = Arrays.copyOf (nodes, nodes.length);
      myWeights = Arrays.copyOf (coords.getBuffer(), coords.size());

      myElement = elem;
      invalidateMasters();
      addBackRefsIfConnected();      
   }

   protected boolean setFromNodes (FemNode[] nodes, VectorNd coords) {
      removeBackRefsIfConnected();
      myCoords = null;

      myNodes = Arrays.copyOf (nodes, nodes.length);
      myWeights = Arrays.copyOf (coords.getBuffer(), coords.size());

      myIpnt = null;
      myData = null;
      myElement = null;

      boolean status = initializeGNX (myNodes, myWeights);

      invalidateMasters();
      addBackRefsIfConnected(); 
      return status;
   }

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
   
   private boolean initializeGNX (FemNode[] nodes, double[] weights) {

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

   /**
    * {@inheritDoc}
    */
   public DynamicComponent[] getMasters() {
      if (myMasters == null) {
         initializeMasters();
      }
      return myMasters;
   }

   public MatrixBlock[] getMasterBlocks() {
      return myMasterBlocks;
   }

   /**
    * {@inheritDoc}
    */
   public int numMasters() {
      if (myMasters == null) {
         initializeMasters();
      }
      return myMasters.length;
   }

   public void invalidateMasters() {
      myMasters = null;
   }

   protected void initializeMasters() {
      if (myNodes != null) {
         // add nodes as master components
         myMasters = new DynamicComponent[myNodes.length];
         myMasterBlocks = new MatrixBlock[myNodes.length];
         for (int i=0; i<myNodes.length; i++) {
            myMasters[i] = myNodes[i];
            myMasterBlocks[i] = new Matrix3x3Block();
         }
      }
      else {
         myMasters = new DynamicComponent[0];
         myMasterBlocks = new MatrixBlock[0];
      }
   }

   public void addMassToMasters() {
      double m = myNode.getBackNode().getEffectiveMass();
      if (m != 0) {
         PointFem3dAttachment.addMassToNodeMasters (
            myNodes, myWeights, m);
         myNode.getBackNode().addEffectiveMass(-m);
      }
   }

   protected void updateMasterBlocks() {
      if (myMasters == null) {
         initializeMasters();
      }
      RotationMatrix3d R = myTFM.R;

      for (int i=0; i<myNodes.length; i++) {
         Matrix3x3Block blk = (Matrix3x3Block)myMasterBlocks[i];
         double Ni = myWeights[i];

         blk.crossProduct (myGNX[i], myInvB);
         blk.crossProduct (blk, myLocDir);
         blk.mul (R, blk);
         // matrix should rotate frame force from world coords
         blk.mulTransposeRight (blk, R);

         blk.m00 += Ni;
         blk.m11 += Ni;
         blk.m22 += Ni;
      }
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

   private void updateDeformationGradient() {
      if (myElement == null) {
         Matrix3d F = new Matrix3d();
         Vector3d pos = new Vector3d();
         for (int i=0; i<myNodes.length; i++) {
            pos.scaledAdd (myWeights[i], myNodes[i].getPosition());
         }
         for (int i=0; i<myNodes.length; i++) {
            Vector3d udef = new Vector3d();
            Vector3d gNX = myGNX[i];
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

   private void computeFramePosition (Vector3d p) {
      p.setZero();
      for (int i=0; i<myNodes.length; i++) {
         p.scaledAdd (myWeights[i], myNodes[i].getPosition());
      }
   }

   public void computeFrame (RigidTransform3d TFM) {
      computeFramePosition (TFM.p);
      TFM.R.set (myPolard.getR());
   }

   public void computeVelocity (Vector3d velw) {
      // rotational component
      RotationMatrix3d R = myTFM.R;
      Vector3d vloc = new Vector3d();
      Vector3d w = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         vloc.inverseTransform (R, myNodes[i].getVelocity());
         w.crossAdd (myGNX[i], vloc, w);
      }
      myInvB.mul (w, w);
      w.transform (R);
      Vector3d dirw = new Vector3d();
      dirw.transform (R, myLocDir);
      velw.cross (w, dirw);
      velw.negate();
      // translational component
      for (int i=0; i<myNodes.length; i++) {
         velw.scaledAdd (myWeights[i], myNodes[i].getVelocity());
      }
      Vector3d velx = new Vector3d(velw);
      // alternate method: use master blocks
      velw.setZero();
      Vector3d v = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         Matrix3x3Block blk = (Matrix3x3Block)myMasterBlocks[i];
         blk.mulTranspose (v, myNodes[i].getVelocity());
         velw.add (v);
      }
      // if (!velx.epsilonEquals (velw, 1e-12)) {
      //    System.out.println ("velw=" + velw.toString ("%12.8f"));
      //    System.out.println ("velx=" + velx.toString ("%12.8f"));
      // }
      
   }

   // XXX FINISH how is LocDir set?

   @Override
   public void updatePosStates() {
      updatePosBasedVariables ();
      computeFrame (myTFM);
      Vector3d dirw = new Vector3d();
      dirw.transform (myTFM.R, myLocDir);
      Point3d bw = new Point3d (myTFM.p);
      bw.sub (dirw);
      myNode.setBackPosition (bw);
      updateMasterBlocks();
   }

   public void applyForces() {
      BackNode3d backNode = myNode.getBackNode();
      Vector3d forceA = backNode.getForce();
      Vector3d f = new Vector3d();
      for (int i=0; i<myNodes.length; i++) {
         Matrix3x3Block blk = (Matrix3x3Block)myMasterBlocks[i];
         blk.mul (f, forceA);
         myNodes[i].addForce (f);
      }
   }

   public void updateAttachment() {
      // TODO XXX FINISH
   }

   public void updateVelStates() {
      Vector3d velw = new Vector3d();
      computeVelocity (velw);
      myNode.getBackNode().setVelocity (velw);
   }

   public boolean getDerivative (double[] buf, int idx) {
      // TODO - implement later
      buf[idx  ] = 0;
      buf[idx+1] = 0;
      buf[idx+2] = 0;
      return false; // change to true when implemented
   }

   public void mulSubMG (
      MatrixBlock D, MatrixBlock M, int idx) {
      MatrixBlock G = myMasterBlocks[idx].createTranspose();
      D.mulAdd (M, G);      
   }

   public MatrixBlock getGT (int idx) {
      MatrixBlock blk = myMasterBlocks[idx].clone();
      blk.negate();
      return blk;
   }

   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
      myMasterBlocks[idx].mulAdd (ybuf, yoff, xbuf, xoff);
   }

   public void mulSubGTM (
      MatrixBlock D, MatrixBlock M, int idx) {
      D.mulAdd (myMasterBlocks[idx], M); 
   }

   // @Override
   // protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   //    throws IOException {

   //    rtok.nextToken();
   //    if (scanAndStoreReference (rtok, "node", tokens)) {
   //       return true;
   //    }
   //    else if (scanAttributeName (rtok, "nodes")) {
   //       tokens.offer (new StringToken ("nodes", rtok.lineno()));
   //       ScanWriteUtils.scanComponentsAndWeights (rtok, tokens);
   //       return true;
   //    }
   //    rtok.pushBack();
   //    return super.scanItem (rtok, tokens);
   // }


   // // XXX check do we need the scan/write methods?

   // @Override
   // protected boolean postscanItem (
   // Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

   //    if (postscanAttributeName (tokens, "node")) {
   //       myNode = postscanReference (tokens, FemNode3d.class, ancestor);
   //       return true;
   //    }
   //    else if (postscanAttributeName (tokens, "nodes")) {
   //       FemNode[] nodes = ScanWriteUtils.postscanReferences (
   //          tokens, FemNode.class, ancestor);
   //       double[] coords = (double[])tokens.poll().value();
   //       myNodes = Arrays.copyOf (nodes, nodes.length);
   //       myWeights = Arrays.copyOf (coords, coords.length);
   //       return true;
   //    }
   //    return super.postscanItem (tokens, ancestor);
   // }
   
   // @Override
   // public void writeItems (
   //    PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
   //    throws IOException {

   //    super.writeItems (pw, fmt, ancestor);

   //    pw.print ("node=");
   //    pw.println (ComponentUtils.getWritePathName (ancestor, myNode));

   //    pw.print ("nodes=");
   //    ScanWriteUtils.writeComponentsAndWeights (
   //       pw, fmt, myNodes, myWeights, ancestor);
   // }

   /**
    * Redefine to use the node instead of the backNode as hard
    * reference.
    */
   @Override 
   public void getHardReferences (List<ModelComponent> refs) {
      if (myNode != null) {
         refs.add (myNode);
      }
      for (DynamicComponent m : getMasters()) {
         refs.add (m);
      }
   } 
}
