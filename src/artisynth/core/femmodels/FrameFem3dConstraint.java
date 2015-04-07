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

import maspack.matrix.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Class to manage the constraint between an FEM and it's coordinate frame.
 */
public class FrameFem3dConstraint {
   //private FemElement myElement;

   private FemElement3d myElement;
   private IntegrationPoint3d myIpnt;
   private IntegrationData3d myData;
   private RotationMatrix3d myRoff;

   public FrameFem3dConstraint() {
      myRoff = new RotationMatrix3d();
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

   public void setFromElement (RigidTransform3d T, FemElement3d elem) {
      Vector3d coords = new Vector3d();
      if (!elem.getNaturalCoordinatesRobust (coords, new Point3d(T.p), 1000)) {
         throw new NumericalException (
            "Can't find natural coords for "+T.p+" in element "+elem.getNumber());
      }
      myIpnt = IntegrationPoint3d.create (elem, coords.x, coords.y, coords.z, 1.0);
      myData = new IntegrationData3d();
      myData.computeRestJacobian (myIpnt.GNs, elem.getNodes());

      myIpnt.computeJacobianAndGradient (elem.getNodes(), myData.myInvJ0);
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (myIpnt.F);  
      myRoff.mulInverseLeft (polard.getR(), T.R);

      myElement = elem;
   }

   public void computeFrame (RigidTransform3d T) {
      T.setIdentity();
      VectorNd N = myIpnt.getShapeWeights();
      FemNode3d[] nodes = myElement.getNodes();
      myIpnt.computeJacobianAndGradient (myElement.getNodes(), myData.myInvJ0);
      for (int i=0; i<nodes.length; i++) {
         T.p.scaledAdd (N.get(i), nodes[i].getPosition());
      }
      PolarDecomposition3d polard = new PolarDecomposition3d();
      polard.factor (myIpnt.F);
      T.R.mul (polard.getR(), myRoff);
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

   public FrameFem3dConstraint (RigidTransform3d T, FemElement3d elem) {
      this();
      setFromElement (T, elem);
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
  

}
