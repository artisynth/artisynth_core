/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import maspack.geometry.InverseDistanceWeights;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix3x1Block;
import maspack.matrix.Matrix3x2Block;
import maspack.matrix.Matrix3x3DiagBlock;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Point3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.Twist;
import maspack.util.ArraySupport;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.ContactMaster;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.LinearPointConstraint;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ObjectToken;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;

public class PointFem3dAttachment
   extends PointAttachment implements ContactMaster {
                                          
   protected FemElement myElement;
   protected FemNode[] myNodes;
   protected VectorNd myCoords;

   // may need to also store natural coordinates since they are needed by the
   // ShellNodeFem3dAttachment subclass
   protected Vector3d myNatCoords;

   public PointFem3dAttachment() {
   }

   public PointFem3dAttachment (Point pnt) {
      myPoint = pnt;        
   }

   public PointFem3dAttachment (Point pnt, FemModel3d fem) {
      myPoint = pnt;
      setFromFem (pnt.getPosition(), fem);
   }
   
   public PointFem3dAttachment (Point pnt, FemNode[] nodes, double[] coords) {
      this(pnt);
      setFromNodes(nodes, coords);
   }
   
   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters (masters);
      if (myNodes != null) {
         // add nodes as master components
         for (int i=0; i<myNodes.length; i++) {
            masters.add (myNodes[i]);
         }
      }
   }
   
   FemModel3d getFemModel () {
      if (myElement != null) {
         ModelComponent gparent = myElement.getGrandParent();
         if (gparent instanceof FemModel3d) {
            return (FemModel3d)gparent;
         }
      }
      return null;
   }

   public FemElement getElement() {
      return myElement;
   }
   
   /**
    * If the nodes of this attachment correspond to the nodes of a particular
    * element, return that element. Otherwise, return null.
    */
   protected static FemElement findElementForNodes (FemNode[] nodes) {
      if (nodes == null || nodes.length == 0) {
         return null;
      }
      if (!(nodes[0] instanceof FemNode3d)) {
         return null;
      }
      FemNode3d node0 = (FemNode3d)nodes[0];
      for (FemElement3dBase elem : node0.getElementDependencies()) {
         FemNode[] enodes = elem.getNodes();
         if (enodes.length == nodes.length) {
            int i = 0;
            for (i=0; i<enodes.length; i++) {
               if (enodes[i] != nodes[i]) {
                  break;
               }
            }
            if (i == enodes.length) {
               // all nodes are the same, so element found
               return elem;
            }
         }
      }
      return null;
   }

   public void setFromNodes (
      Collection<? extends FemNode> nodes, VectorNd weights) {
      setFromNodes (
         nodes.toArray(new FemNode[0]), weights.getBuffer());
   }
   
   public boolean setFromNodes (
      Point3d pos, Collection<? extends FemNode> nodes) {
      return setFromNodes (pos, nodes.toArray(new FemNode[0]));
   }

   public void setFromNodes (FemNode[] nodes, double[] weights) {
      dosetNodes (nodes, weights);
      myElement = null;
      myNatCoords = null;
   }

   public boolean setFromNodes (Point3d pos, FemNode[] nodes) {
      ArrayList<Vector3d> support = new ArrayList<Vector3d>(nodes.length);
      for (int i=0; i<nodes.length; i++) {
         support.add (nodes[i].getPosition());
      }
      InverseDistanceWeights idweights = 
         new InverseDistanceWeights (1, 1, /*normalize=*/true);
      VectorNd weights = new VectorNd();
      boolean status = idweights.compute (weights, pos, support);
      dosetNodes (nodes, weights.getBuffer());
      myElement = null;
      myNatCoords = null;
      return status;
   }

   protected void dosetNodes (FemNode[] nodes, double[] weights) {
      if (nodes.length == 0) {
         throw new IllegalArgumentException (
            "Must have at least one node");
      }
      if (nodes.length > weights.length) {
         throw new IllegalArgumentException (
            "Number of weights is less than the number of nodes");
      }
      removeBackRefsIfConnected();
      myCoords = new VectorNd (nodes.length);
      myNodes = new FemNode[nodes.length];
      for (int i=0; i<nodes.length; i++) {
         myNodes[i] = nodes[i];
         if (weights != null) {
            myCoords.set (i, weights[i]);
         }
      }
      invalidateMasters();
      addBackRefsIfConnected();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }
   
   public boolean setFromElement (Point3d pos, FemElement elem) {
      return setFromElement (pos, elem, /*reduceTol=*/0);
   }
    
   public boolean setFromElement (
      Point3d pos, FemElement elem, double reduceTol) {
      removeBackRefsIfConnected();
      FemNode[] nodes = elem.getNodes();
      VectorNd coords = new VectorNd (nodes.length);  
      myNatCoords = new Vector3d();
      boolean converged =
         elem.getMarkerCoordinates (coords, myNatCoords, pos, false);
      int numNodes = 0;

      // Set weights whose absolute value is below reduceTol to w.
      // Only use weights > 0 for the nodes, *unless* reduceTol < 0,
      // in which we will use all the nodes
      for (int i=0; i<coords.size(); i++) {
         double w = coords.get(i);
         if (Math.abs(w) <= reduceTol) {
            coords.set (i, 0);
            w = 0;
         }
         if (reduceTol < 0 || w != 0) {
            numNodes++;
         }
      }
      myNodes = new FemNode[numNodes];
      myCoords = new VectorNd (numNodes); 
      // set the final nodes
      int k = 0;
      for (int i=0; i<nodes.length; i++) {
         double w = coords.get(i);
         if (reduceTol < 0 || w != 0) {
            myNodes[k] = nodes[i];
            myCoords.set (k, w);
            k++;
         }
      }
      myElement = elem;
      invalidateMasters();
      addBackRefsIfConnected();
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      return converged;
   }

   public void setFromFem (Point3d pos, FemModel3d fem) {
      setFromFem (pos, fem, /*project=*/true);
   }

   public boolean setFromFem (Point3d pos, FemModel3d fem, boolean project) {
      Point3d loc = new Point3d();
      FemElement3dBase elem = fem.findNearestElement (loc, pos);
      if (!loc.equals (pos)) {
         if (!project) {
            return false;
         }
         pos = new Point3d(loc);
      }
      setFromElement (pos, elem);
      return true;
   }

   public VectorNd getCoordinates() {
      return myCoords;
   }
   
   public double getCoordinate (int idx) {
      return myCoords.get(idx);
   }

   public void updatePosStates() {
      if (myPoint != null) {
         Point3d pntw = new Point3d();
         getCurrentPos (pntw);
         myPoint.setPosition (pntw);
      }
   }

   public void getCurrentPos (Vector3d pos) {
      if (myNodes != null) {
         double[] coords = myCoords.getBuffer();
         pos.setZero();
         for (int i = 0; i < myNodes.length; i++) {
            pos.scaledAdd (coords[i], myNodes[i].getPosition(), pos);
            
         }
      }
   }

   public void getCurrentVel (Vector3d vel) {
      if (myNodes != null) {
         double[] coords = myCoords.getBuffer();
         vel.setZero();
         for (int i = 0; i < myNodes.length; i++) {
            vel.scaledAdd (coords[i], myNodes[i].getVelocity(), vel);
         }
      }
      else {
         vel.setZero();
      }
   }

   public void updateVelStates() {
      if (myPoint != null) {
         Vector3d velw = new Vector3d();
         getCurrentVel (velw);
         myPoint.setVelocity (velw);
      }
   }

   /**
    * Update attachment to reflect changes in the slave state.
    */
   public void updateAttachment() {
      if (myElement != null) {
         boolean resetElement = false;
         if (myCoords.size() != myElement.numNodes()) {
            resetElement = true;
         }
         else if (!myElement.getMarkerCoordinates (
            myCoords, myNatCoords, myPoint.getPosition(), /*checkInside=*/true)) {
            resetElement = true;
         }
         if (resetElement) {
            FemModel3d fem = getFemModel();
            if (fem == null) {
               throw new InternalErrorException (
                  "PointFem3dAttachment has an assigned element but no FEM");
            }
            setFromFem (myPoint.getPosition(), fem);               
            updatePosStates();
         }
      }
   }

   public void applyForces() {
      super.applyForces();
      if (myNodes != null && myPoint != null) {
         double[] coords = myCoords.getBuffer();
         Vector3d force = myPoint.getForce();
         for (int i = 0; i < myNodes.length; i++) {
            Vector3d nodeForce = myNodes[i].getForce();
            nodeForce.scaledAdd (coords[i], force, nodeForce);
         }
      }
   }

   public void getRestPosition (Point3d pos) {
      pos.setZero();
      for (int i=0; i<myNodes.length; i++) {
         pos.scaledAdd (
            myCoords.get(i), ((FemNode3d)myNodes[i]).getRestPosition());
      }
   }

   private void addBlock (MatrixBlock depBlk, MatrixBlock blk, int idx) {
      depBlk.scaledAdd (myCoords.get (idx), blk);
   }

   public void mulSubGTM (MatrixBlock D, MatrixBlock M, int idx) {
      addBlock (D, M, idx);
   }

   public void mulSubMG (MatrixBlock D, MatrixBlock M, int idx) {
      addBlock (D, M, idx);
   }

   public MatrixBlock getGT (int idx) {
      Matrix3x3Block blk = new Matrix3x3Block();
      double s = myCoords.get (idx);
      blk.setDiagonal (-s, -s, -s);
      return blk;
   }

   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
      double s = myCoords.get (idx);
      ybuf[yoff  ] += s*xbuf[xoff  ];
      ybuf[yoff+1] += s*xbuf[xoff+1];
      ybuf[yoff+2] += s*xbuf[xoff+2];
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "element", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "ncoords")) {
         myNatCoords = new Vector3d();
         myNatCoords.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "nodes")) {
         tokens.offer (new StringToken ("nodes", rtok.lineno()));
         ScanWriteUtils.scanComponentsAndWeights (rtok, tokens);
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
         myElement = elem;
         return true;
      }
      else if (postscanAttributeName (tokens, "nodes")) {
         FemNode[] nodes = ScanWriteUtils.postscanReferences (
            tokens, FemNode.class, ancestor);
         double[] coords = (double[])tokens.poll().value();
         dosetNodes (nodes, coords);
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
      }
      if (myNatCoords != null) {
         pw.println (
            "ncoords=[ "+myNatCoords.toString (fmt) + "]");
      }
      pw.print ("nodes=");
      ScanWriteUtils.writeComponentsAndWeights (
         pw, fmt, myNodes, myCoords.getBuffer(), ancestor);
   }

   public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      for (int i=0; i<myNodes.length; i++) {
         double c = myCoords.get(i);
         Matrix3x3DiagBlock blk = new Matrix3x3DiagBlock();
         blk.set (c, c, c);
         if (myNodes[i].getSolveIndex() != -1)
            J.addBlock (bi, myNodes[i].getSolveIndex(), blk);
      }
      return bi++;      
   }

   private void solve (Vector2d s, Vector3d d0, Vector3d d1, Vector3d b) {
      double m00 = d0.dot(d0);
      double m01 = d0.dot(d1);
      double m10 = d1.dot(d0);
      double m11 = d1.dot(d1);
      double det = m00*m11 - m01*m10;
      double b0 = d0.dot(b);
      double b1 = d1.dot(b);
      s.x = ( m11*b0 - m01*b1)/det;
      s.y = (-m10*b0 + m00*b1)/det;
   }

   private void quadShape (Vector3d p, double s0, double s1, 
                   Vector3d v0, Vector3d v1, Vector3d v2, Vector3d v3) {
      p.scale ((1-s0)*(1-s1)/4.0, v0);
      p.scaledAdd ((1+s0)*(1-s1)/4.0, v1);
      p.scaledAdd ((1+s0)*(1+s1)/4.0, v2);
      p.scaledAdd ((1-s0)*(1+s1)/4.0, v3);
   }

   private void computeSForQuad (
      Vector2d s, Vector3d p,
      Vector3d v0, Vector3d v1, Vector3d v2, Vector3d v3) {

      Vector3d df0 = new Vector3d(); // tmp
      Vector3d df1 = new Vector3d(); // tmp
      Vector3d b = new Vector3d(); // tmp

      int niter = 20;
      double s0 = 0;
      double s1 = 0;
      int i;
      for (i=0; i<niter; i++) {
         df0.x = 0.25*(-(1-s1)*v0.x + (1-s1)*v1.x + (1+s1)*v2.x - (1+s1)*v3.x);
         df0.y = 0.25*(-(1-s1)*v0.y + (1-s1)*v1.y + (1+s1)*v2.y - (1+s1)*v3.y);
         df0.z = 0.25*(-(1-s1)*v0.z + (1-s1)*v1.z + (1+s1)*v2.z - (1+s1)*v3.z);

         df1.x = 0.25*(-(1-s0)*v0.x - (1+s0)*v1.x + (1+s0)*v2.x + (1-s0)*v3.x);
         df1.y = 0.25*(-(1-s0)*v0.y - (1+s0)*v1.y + (1+s0)*v2.y + (1-s0)*v3.y);
         df1.z = 0.25*(-(1-s0)*v0.z - (1+s0)*v1.z + (1+s0)*v2.z + (1-s0)*v3.z);

         quadShape (b, s0, s1, v0, v1, v2, v3);
         b.sub (p, b);
         solve (s, df0, df1, b);
         s0 += s.x;
         s1 += s.y;
         if (s.norm() < 1e-10) {
            break;
         }
      }
      if (i==niter) {
         System.out.println ("Warning: computeSForQuad did not converge");
      }
      s.x = s0;
      s.y = s1;
   }

   private static int warningCnt = 0;
   private static final int WARNING_LIMIT = 100;

   public void computeNodeCoordinates() {
      // The idea here is to first find the natural coordinates s, which will
      // have either dimension 1 or 2. The coordinates are then computed
      // from the shape functions, which are themselves a function of the
      // natural coordinates.

      // To find the natural coordinates, we find s to minimize ||f(s) - p||
      // where p is the current point position and f(s) is the position as a
      // function of the natural coordinates. We do a minimization because for
      // general node attachments the point is constrained to a subspace
      // defined by the nodes in question and so f(s) cannot reproduce an
      // arbitrary position.

      // The minimization is done using Newton's method, each step of
      // which consists of
      //   
      //   T    T
      // (A A) A  ds = p - f(s)
      //
      // where A = df/ds. In some cases the problem is linear and
      // the above step provides the exact solution.
      Vector3d p = myPoint.getPosition();

      switch (myNodes.length) {
         case 1: {
            myCoords.set (0, 1.0);
            break;
         }
         case 2: {
            Vector3d df0 = new Vector3d(); // tmp
            Vector3d b = new Vector3d(); // tmp

            Vector3d v0 = myNodes[0].getPosition();
            Vector3d v1 = myNodes[1].getPosition();
            df0.sub (v1, v0);
            b.sub (p, v0);
            double s = df0.dot(b)/df0.dot(df0);
            myCoords.set (0, 1-s);
            myCoords.set (1, s);
            break;
         }
         case 3:{
            Vector3d df0 = new Vector3d(); // tmp
            Vector3d df1 = new Vector3d(); // tmp
            Vector3d b = new Vector3d(); // tmp

            Vector3d v0 = myNodes[0].getPosition();
            Vector3d v1 = myNodes[1].getPosition();
            Vector3d v2 = myNodes[2].getPosition();
            Vector2d s = new Vector2d();
            df0.sub (v1, v0);
            df1.sub (v2, v0);
            b.sub (p, v0);
            solve (s, df0, df1, b);
            myCoords.set (0, 1-s.x-s.y);
            myCoords.set (1, s.x);
            myCoords.set (2, s.y);
            break;
         }
         case 4:{
            Vector3d v0 = myNodes[0].getPosition();
            Vector3d v1 = myNodes[1].getPosition();
            Vector3d v2 = myNodes[2].getPosition();
            Vector3d v3 = myNodes[3].getPosition();
            Vector2d s = new Vector2d();

            computeSForQuad (s, p, v0, v1, v2, v3);
            double s0 = s.x;
            double s1 = s.y;
            myCoords.set (0, (1-s0)*(1-s1)/4.0);
            myCoords.set (1, (1+s0)*(1-s1)/4.0);
            myCoords.set (2, (1+s0)*(1+s1)/4.0);
            myCoords.set (3, (1-s0)*(1+s1)/4.0);
            break;
         }
         default: {
            ArrayList<Vector3d> support = 
               new ArrayList<Vector3d>(myNodes.length);
            for (int i=0; i<myNodes.length; i++) {
               support.add (myNodes[i].getPosition());
            }
            InverseDistanceWeights idweights = 
               new InverseDistanceWeights (1, 1, /*normalize=*/true);
            VectorNd weights = new VectorNd();
            if (!idweights.compute (weights, p, support)) {
               System.out.println (
                  "Warning: PointFem3dAttachment: insufficient node support");
            }
            myCoords.set (weights);
         }
      }
   }
   
   /** 
    * Create an attachment that connects a point to a FemElement
    * within a specified element.
    * 
    * @param pnt Point to be attached
    * @param elem FemElement3d to attach the point to
    * @param loc point location with respect to the element. If <code>null</code>,
    * will be assumed to be pnt.getPosition().
    * @param reduceTol try to reduce the number of attached nodes by
    * removing those whose coordinate values are less then this number.
    * A value of zero ensures no reduction. If reduction is desired,
    * a value around 1e-5 is reasonable.
    * @return an attachment for 
    */
   public static PointFem3dAttachment create (
      Point pnt, FemElement3dBase elem, Point3d loc, double reduceTol) {

      PointFem3dAttachment ax = null;
      // Figure out the coordinates for the attachment point
      VectorNd coords = new VectorNd (elem.numNodes());
      elem.getMarkerCoordinates (
         coords, null, loc!=null ? loc : pnt.getPosition(), false);
      ax = new PointFem3dAttachment (pnt);
      ax.setFromElement (pnt.getPosition(), elem, reduceTol);
      //ax.myCoords.set (coords);
      return ax;
   }

   /***
    * Distribute a mass {@code m} amongst the master nodes on the basis of
    * their coordinate weights. To guard against negative weights, we use their
    * absolute value and then renormalize so that they sum to unity. Negative
    * weights will occur when the attached point lies outside of an element
    * associated with the nodes.
    *
    * <p>Using absolute values is necessary because we are using a lumped mass
    * formulation; if we instead used a consistent mass formulation, the
    * resulting distributed normal mass would be
    * <pre>
    *   m weights weights^T
    * </pre>
    * which is symmetric positive definite and so there would be no need to
    * guard against negative weights.
    * 
    * @param nodes nodes to which mass should be added
    * @param weights coordinate weights of each node
    * @param m mass to be distributed
    */
   public static void addMassToNodeMasters (
      FemNode[] nodes, double[] weights, double m) {
      double[] absWeights = new double[nodes.length];
      double sum = 0;
      for (int i=0; i<nodes.length; i++) {
         double w = weights[i];
         if (w < 0) {
            w = -w;
         }
         absWeights[i] = w;
         sum += w;
      }
      double scale = m/sum; // mass times normalization scale factor
      for (int i=0; i<nodes.length; i++) {
         nodes[i].addEffectiveMass (scale*absWeights[i]);
      }
   }
   
   public void addMassToMasters() {
      double m = myPoint.getEffectiveMass();
      if (m != 0) {
         addMassToNodeMasters (myNodes, myCoords.getBuffer(), m);
         myPoint.addEffectiveMass(-m);
      }
   }
   
   public boolean getDerivative (double[] buf, int idx) {
      buf[idx  ] = 0;
      buf[idx+1] = 0;
      buf[idx+2] = 0;
      return false;
   }

   public FemNode[] getNodes() {
      return myNodes;
   }
   
  @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      // should probably make this a soft reference instead
      if (myElement != null) {
         refs.add (myElement);
      }
  }
   
   public PointFem3dAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointFem3dAttachment a = (PointFem3dAttachment)super.copy (flags, copyMap);

      if (myNodes != null) {
         a.myNodes = new FemNode[myNodes.length];
         for (int i=0; i<myNodes.length; i++) {
            a.myNodes[i] = 
            (FemNode)ComponentUtils.maybeCopy (flags, copyMap, myNodes[i]);  
         }
      }
      if (myCoords != null) {
         a.myCoords = new VectorNd(myCoords);
      }
      if (myElement != null) {
         a.myElement = 
            (FemElement)ComponentUtils.maybeCopy (flags, copyMap, myElement);  
      }
      
      return a;
   }

   /* --- begin ContactMaster implementation --- */

   public void add1DConstraintBlocks (
      SparseBlockMatrix GT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir) {
      for (int i=0; i<myNodes.length; i++) {
         int bi = myNodes[i].getSolveIndex();
         if (bi != -1) {
            Matrix3x1Block blk = (Matrix3x1Block)GT.getBlock (bi, bj);
            if (blk == null) {
               blk = new Matrix3x1Block();
               GT.addBlock (bi, bj, blk);
            }
            blk.scaledAdd (scale*myCoords.get(i), dir);
         }
      }
   }

   public void add2DConstraintBlocks (
      SparseBlockMatrix GT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir0, Vector3d dir1) {
      for (int i=0; i<myNodes.length; i++) {
         int bi = myNodes[i].getSolveIndex();
         if (bi != -1) {
            Matrix3x2Block blk = (Matrix3x2Block)GT.getBlock (bi, bj);
            if (blk == null) {
               blk = new Matrix3x2Block();
               GT.addBlock (bi, bj, blk);
            }
            double s = scale*myCoords.get(i);
            blk.m00 += s*dir0.x;
            blk.m10 += s*dir0.y;
            blk.m20 += s*dir0.z;
            blk.m01 += s*dir1.x;
            blk.m11 += s*dir1.y;
            blk.m21 += s*dir1.z;
         }
      }
   }
   
   public void addRelativeVelocity (
      Vector3d vel, double scale, ContactPoint cpnt) {
      for (int i=0; i<myNodes.length; i++) {
         vel.scaledAdd (scale*myCoords.get(i), myNodes[i].getVelocity());
      }
   }

   public boolean isControllable() {
      for (FemNode node : myNodes) {
         if (node.isControllable()) {
            return true;
         }
      }
      return false;
   }
   
   public int collectMasterComponents (
      HashSet<DynamicComponent> masters, boolean activeOnly) {
      int num = 0;
      for (FemNode node : myNodes) {
         if (!activeOnly || node.isActive()) {
            if (masters.add (node)) {
               num++;
            }
         }
      }
      return num;
   }

   /* --- end ContactMaster implementation --- */  

}
