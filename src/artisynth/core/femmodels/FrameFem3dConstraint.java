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

   private FemElement3d myElement;
   private IntegrationPoint3d myIpnt;
   private IntegrationData3d myData;
   private RotationMatrix3d myRC;
   private Frame myFrame;

   private Vector3d[] myGNX;
   private Matrix3x6Block[] myMasterBlocks;
   //private Matrix3x3Block[] myVBlocks;
   //private Matrix3x3Block[] myOmegaBlocks;

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
         int bk = nodes[i].getSolveIndex();
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

      //RotationMatrix3d Rf = myFrame.getPose().R;
      //Matrix6x3Block blk = new Matrix6x3Block();
      //blk.setSubMatrix00 (Rf);
      //GT.addBlock (bf, bj, blk);

      // blk = new Matrix6x3Block();
      // Matrix3d Tmp = new Matrix3d();
      // Tmp.mulTransposeRight (Rf, myB);
      // blk.setSubMatrix30 (Tmp);
      // GT.addBlock (bf, bj+1, blk);

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

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      for (int i=0; i<6; i++) {
         myLam[i] = lam.get(idx++);
      }
      return idx;
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      for (int i=0; i<6; i++) {
         lam.set (idx++, myLam[i]);
      }
      return idx;
   }
   
   public void zeroImpulses() {
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
      //B.mulTransposeRight (B, myRC);
   }

   public double updateConstraints (double t, int flags) {
      // update P
      Matrix3d B = new Matrix3d();
      VectorNd N = myIpnt.getShapeWeights();
      FemNode3d[] nodes = myElement.getNodes();
      myIpnt.computeJacobianAndGradient (myElement.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (myIpnt.F);
      computeB (B, polard.getP());
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
      //System.out.println ("err=" + myErr.toString ("%13.5e"));
      //myErr.setZero();

      // // T was computed in the local frame. Multiple by current pose
      // // to get the pose error
      // T.mulInverseLeft (myFrame.getPose(), T);

      // myErr.set (T);

      return 0;
   }
   
   public void getConstrainedComponents (List<DynamicComponent> list) {
      if (myElement != null) {
         for (FemNode n : myElement.getNodes()) {
            list.add (n);
         }
      }
   }
   
   // public void setNodes (
   //    Collection<FemNode> nodes, Collection<Double> coords) {
   //    double[] _coords = new double[coords.size()];
   //    int i = 0;
   //    for (double w : coords) {
   //       _coords[i++] = w;
   //    }
   //    setNodes (nodes.toArray(new FemNode[0]), _coords);
   // }

   // public void setNodes (FemNode[] nodes, double[] coords) {
   //    dosetNodes (nodes, coords);
   //    //myElement = null;
   //    if (coords == null) {
   //       // compute node coordinates explicitly
   //       updateAttachment();
   //    }
   // }

   // private void dosetNodes (FemNode[] nodes, double[] coords) {
   //    if (nodes.length == 0) {
   //       throw new IllegalArgumentException (
   //          "Must have at least one node");
   //    }
   //    if (coords != null && nodes.length != coords.length) {
   //       throw new IllegalArgumentException (
   //          "nodes and coords have incompatible sizes: "+
   //          nodes.length+" vs. "+coords.length);
   //    }
   //    myCoords = new VectorNd (nodes.length);
   //    myNodes = new FemNode[nodes.length];
   //    for (int i=0; i<nodes.length; i++) {
   //       myNodes[i] = nodes[i];
   //       if (coords != null) {
   //          myCoords.set (i, coords[i]);
   //       }
   //    }
   // }


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
         // blk.crossProduct (R0, myGNX[i]);
         // blk.scale (-1);
         // myOmegaBlocks[i] = blk;

         // blk = new Matrix3x3Block();
         // blk.setIdentity();
         // blk.scale (N.get(i));
         // myVBlocks[i] = blk;

         Matrix3x6Block blkm = new Matrix3x6Block();
         blkm.m00 = N.get(i);
         blkm.m11 = N.get(i);
         blkm.m22 = N.get(i);
         myMasterBlocks[i] = blkm;
      }
   }

   public FemElement3d getElement() {
      return myElement;
   }

   public void setFromElement (RigidTransform3d T, FemElement3d elem) {
      Vector3d coords = new Vector3d();
      if (elem.getNaturalCoordinates (coords, new Point3d(T.p), 1000) < 0) {
         throw new NumericalException (
            "Can't find natural coords for "+T.p+" in element "+elem.getNumber());
      }
      myIpnt = IntegrationPoint3d.create (
         elem, coords.x, coords.y, coords.z, 1.0);
      myData = new IntegrationData3d();
      myData.computeRestJacobian (myIpnt.GNs, elem.getNodes());

      myIpnt.computeJacobianAndGradient (elem.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (myIpnt.F);  
      myRC.mulInverseLeft (polard.getR(), T.R);

      //computeBlocks (myData.myInvJ0, myRC);
      computeBlocks (myData.myInvJ0, RotationMatrix3d.IDENTITY);

      myElement = elem;
   }

   public void computeFrame (RigidTransform3d T) {
      T.setIdentity();
      VectorNd N = myIpnt.getShapeWeights();
      FemNode3d[] nodes = myElement.getNodes();
      myIpnt.computeJacobianAndGradient (myElement.getNodes(), myData.myInvJ0);
      for (int i=0; i<nodes.length; i++) {
         T.p.scaledAdd (N.get(i), nodes[i].getLocalPosition());
      }
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (myIpnt.F);
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
      myIpnt.computeJacobianAndGradient (myElement.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (myIpnt.F);
      computeB (B, polard.getP());
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
      myIpnt.computeJacobianAndGradient (myElement.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (myIpnt.F);
      computeB (B, polard.getP());
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
         T.R.mul (myRC);
      }
      else {
         // multiply by existing frame to get the new pose
         T.mul (myFrame.getPose(), T);
      }
      myFrame.setPose (T);
   }

   // public void updateVelStates() {
   //    if (myNodes != null) {
   //       double[] coords = myCoords.getBuffer();
   //       Vector3d vel = myPoint.getVelocity();
   //       vel.setZero();
   //       for (int i = 0; i < myNodes.length; i++) {
   //          vel.scaledAdd (coords[i], myNodes[i].getVelocity(), vel);
   //       }
   //    }
   // }

   // /**
   //  * Update attachment to reflect changes in the slave state.
   //  */
   // public void updateAttachment() {
   //    myElement.getMarkerCoordinates (myCoords, myPoint.getPosition());
   // }

   // public void applyForces() {
   //    if (myNodes != null) {
   //       double[] coords = myCoords.getBuffer();
   //       Vector3d force = myPoint.getForce();
   //       for (int i = 0; i < myNodes.length; i++) {
   //          Vector3d nodeForce = myNodes[i].getForce();
   //          nodeForce.scaledAdd (coords[i], force, nodeForce);
   //       }
   //    }
   // }

   // public void getRestPosition (Point3d pos) {
   //    pos.setZero();
   //    for (int i=0; i<myNodes.length; i++) {
   //       pos.scaledAdd (
   //          myCoords.get(i), ((FemNode3d)myNodes[i]).getRestPosition());
   //    }
   // }

   // protected void scanNodes (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   //    throws IOException {

   //    ArrayList<Double> coordList = new ArrayList<Double>();
   //    rtok.scanToken ('[');
   //    tokens.offer (ScanToken.BEGIN); // begin token
   //    while (ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
   //       coordList.add (rtok.scanNumber());
   //    }
   //    if (rtok.ttype != ']') {
   //       throw new IOException ("Expected ']', got " + rtok);
   //    }
   //    tokens.offer (ScanToken.END); // terminator token
   //    tokens.offer (new ObjectToken(ArraySupport.toDoubleArray (coordList)));
   // }

   // protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   //    throws IOException {

   //    rtok.nextToken();
   //    if (scanAttributeName (rtok, "nodes")) {
   //       tokens.offer (new StringToken ("nodes", rtok.lineno()));
   //       scanNodes (rtok, tokens);
   //       return true;
   //    }
   //    rtok.pushBack();
   //    return super.scanItem (rtok, tokens);
   // }

   // protected boolean postscanItem (
   // Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

   //    if (postscanAttributeName (tokens, "nodes")) {
   //       FemNode[] nodes = ScanWriteUtils.postscanReferences (
   //          tokens, FemNode.class, ancestor);
   //       double[] coords = (double[])tokens.poll().value();
   //       setNodes (nodes, coords);
   //       return true;
   //    }
   //    return super.postscanItem (tokens, ancestor);
   // }
   
   // protected void writeNodes (PrintWriter pw, NumberFormat fmt, Object ref)
   //    throws IOException {
   //    CompositeComponent ancestor =
   //       ComponentUtils.castRefToAncestor (ref);
   //    pw.println ("[");
   //    IndentingPrintWriter.addIndentation (pw, 2);
   //    for (int i=0; i<myNodes.length; i++) {
   //       pw.println (
   //          ComponentUtils.getWritePathName (ancestor, myNodes[i])+" "+
   //          myCoords.get(i));
   //    }
   //    IndentingPrintWriter.addIndentation (pw, -2);
   //    pw.println ("]");
   // }

   // public void writeItems (
   //    PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
   //    throws IOException {
   //    super.writeItems (pw, fmt, ancestor);
   //    pw.print ("nodes=");
   //    writeNodes (pw, fmt, ancestor);
   // }

   // @Override
   // public void transformSlaveGeometry (
   //    AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
   //    updateAttachment();
   // }

   // public int addTargetJacobian (SparseBlockMatrix J, int bi) {
   //    for (int i=0; i<myNodes.length; i++) {
   //       double c = myCoords.get(i);
   //       Matrix3x3DiagBlock blk = new Matrix3x3DiagBlock();
   //       blk.set (c, c, c);
   //       if (myNodes[i].getSolveIndex() != -1)
   //          J.addBlock (bi, myNodes[i].getSolveIndex(), blk);
   //    }
   //    return bi++;      
   // }

   // /** 
   //  * Create an attachment that connects a point to a FemElement.  If the point
   //  * lies outside the model, then the attachment will be created for the
   //  * location determined by projecting the point into the nearest element
   //  * and the new location will be returned in <code>loc</code>.
   //  * 
   //  * @param pnt Point to be attached
   //  * @param elem FemElement3d to attach the point to
   //  * @param loc point location with respect to the element. If <code>null</code>,
   //  * will be assumed to be pnt.getPosition().
   //  * @param reduceTol try to reduce the number of attached nodes by
   //  * removing those whose coordinate values are less then this number.
   //  * A value of zero ensures no reduction. If reduction is desired,
   //  * a value around 1e-5 is reasonable.
   //  * @return an attachment for 
   //  */
   // public static FrameFem3dConstraint create (
   //    Point pnt, FemElement3d elem, Point3d loc, double reduceTol) {

   //    FrameFem3dConstraint ax = null;
   //    // Figure out the coordinates for the attachment point
   //    VectorNd coords = new VectorNd (elem.numNodes());
   //    elem.getMarkerCoordinates (coords, loc!=null ? loc : pnt.getPosition());
   //    if (reduceTol > 0) {
   //       FemNode3d[] nodes = elem.getNodes();
   //       // Find number of coordinates which are close to zero
   //       int numZero = 0;
   //       for (int i=0; i<elem.numNodes(); i++) {
   //          if (Math.abs(coords.get(i)) < reduceTol) {
   //             numZero++;
   //          }
   //       }
   //       // If we have coordinates close to zero, and the number of remaining
   //       // coords is <= 4, then specify the nodes and coords explicitly
   //       if (numZero > 0 && elem.numNodes()-numZero <= 4) {
   //          int numc = elem.numNodes()-numZero;
   //          double[] reducedCoords = new double[numc];
   //          FemNode3d[] reducedNodes = new FemNode3d[numc];
   //          int k = 0;
   //          for (int i=0; i<elem.numNodes(); i++) {
   //             if (Math.abs(coords.get(i)) >= reduceTol) {
   //                reducedCoords[k] = coords.get(i);
   //                reducedNodes[k] = nodes[i];
   //                k++;
   //             }
   //          }
   //          ax = new FrameFem3dConstraint (pnt);
   //          ax.setNodes (reducedNodes, reducedCoords);
   //          return ax;
   //       }
   //    }
   //    ax = new FrameFem3dConstraint (pnt);
   //    ax.setFromElement (elem);


   //    ax.myCoords.set (coords);
   //    return ax;
   // }

   // public boolean getDerivative (double[] buf, int idx) {
   //    buf[idx  ] = 0;
   //    buf[idx+1] = 0;
   //    buf[idx+2] = 0;
   //    return false;
   // }

   public FrameFem3dConstraint (Frame frame, FemElement3d elem) {
      this();
      myFrame = frame;
      setFromElement (frame.getPose(), elem);
   }

   // public FrameFem3dConstraint copy (
   //    int flags, Map<ModelComponent,ModelComponent> copyMap) {
   //    FrameFem3dConstraint a = (FrameFem3dConstraint)super.copy (flags, copyMap);

   //    if (myNodes != null) {
   //       a.myNodes = new FemNode[myNodes.length];
   //       for (int i=0; i<myNodes.length; i++) {
   //          a.myNodes[i] = 
   //             (FemNode)ComponentUtils.maybeCopy (flags, copyMap, myNodes[i]);
   //       }
   //    }
   //    if (myCoords != null) {
   //       a.myCoords = new VectorNd(myCoords);
   //    }
   //    return a;
   // }
  
   public void render (GLRenderer renderer, int flags) {
   }
}
