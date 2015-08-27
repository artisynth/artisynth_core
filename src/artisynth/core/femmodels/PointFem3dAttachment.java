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
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ObjectToken;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.util.TransformableGeometry;

public class PointFem3dAttachment extends PointAttachment {
   private FemElement myElement;
   private FemNode[] myNodes;
   private VectorNd myCoords;
   private Frame myFemFrame;
   // neither the point nor the FEM are frame-relative
   boolean myNoFrameRelativeP = true;

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
      myFemFrame = null;
      if (myNodes != null) {
         // check to see if nodes are associated with a frame-based FEM.
         // if so, then add that frame as a master
         for (int i=0; i<myNodes.length; i++) {
            FemNode n = myNodes[i];
            Frame nframe = n.getPointFrame();
            if (nframe != null) {
               if (myFemFrame == null) {
                  myFemFrame = nframe;
                  masters.add (myFemFrame);
               }
               else if (myFemFrame != nframe) {
                  throw new UnsupportedOperationException (
                     "PointFem3d attachment not supported for multiple " +
                     "frame-based FEMs");
               }
            }
         }
         // add nodes as master components as well
         for (int i=0; i<myNodes.length; i++) {
            masters.add (myNodes[i]);
         }
      }
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
      myNoFrameRelativeP = false;
      if (myFemFrame != null) {
         R2 = myFemFrame.getPose().R;
         Point3d lpos = new Point3d(); // local position in FemFrame
         lpos.inverseTransform (myFemFrame.getPose(), myPoint.getPosition());
         myFemFrame.computeLocalPointForceJacobian (
            myMasterBlocks[idx++], lpos, R1);
      }
      else if (R1 == null) {
         myNoFrameRelativeP = true;
      }
      if (!myNoFrameRelativeP) {
         RotationMatrix3d R = new RotationMatrix3d();
         if (R1 != null && R2 != null) {
            R.mulInverseLeft (R2, R1);
         }
         else if (R1 != null){
            R.set (R1);
         }
         else if (R2 != null) {
            R.transpose (R2);
         }
         for (int i=0; i<myNodes.length; i++) {
            Matrix3x3Block pblk = (Matrix3x3Block)myMasterBlocks[idx++];
            pblk.scale (myCoords.get(i), R);
         }
      }
      else {
         // no need to update blocks since blocks will not be used
      }
      return idx;
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
      for (FemElement3d elem : node0.getElementDependencies()) {
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
      return status;
   }

   private void dosetNodes (FemNode[] nodes, double[] weights) {
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
      myCoords = new VectorNd (nodes.length);  
      boolean converged = elem.getMarkerCoordinates (myCoords, pos, false);
      int numNodes = 0;
      // reduce any weights below reduceTol to 0 ...
      for (int i=0; i<myCoords.size(); i++) {
         double w = myCoords.get(i);
         if (Math.abs(w) <= reduceTol) {
            myCoords.set (i, 0);
            w = 0;
         }
         if (w != 0) {
            numNodes++;
         }
      }
      // only create nodes for these whose weights are non zero. 
      myNodes = new FemNode[numNodes];
      int k = 0;
      for (int i=0; i<nodes.length; i++) {
         double w = myCoords.get(i);
         if (w != 0) {
            myNodes[k] = nodes[i];
            myCoords.set (k, w);
            k++;
         }
      }      
      myCoords.setSize (numNodes);
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
      FemElement3d elem = fem.findNearestElement (loc, pos);
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
         updateMasterBlocks();
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

   public void getCurrentVel (Vector3d vel, Vector3d dvel) {
      if (myNodes != null) {
         double[] coords = myCoords.getBuffer();
         vel.setZero();
         for (int i = 0; i < myNodes.length; i++) {
            vel.scaledAdd (coords[i], myNodes[i].getPosition(), vel);
         }
         if (dvel != null) {
           computeVelDerivative (dvel); 
         }
      }
      else {
         vel.setZero();
         if (dvel != null) {
            dvel.setZero();
         }
      }
   }

   public void updateVelStates() {
      if (myPoint != null) {
         Vector3d velw = new Vector3d();
         getCurrentVel (velw, null);
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
            myCoords, myPoint.getPosition(), /*checkInside=*/true)) {
            resetElement = true;
         }
         if (resetElement) {
            FemModel3d fem = getFemModel();
            if (fem == null) {
               throw new InternalErrorException (
                  "PointFem3dAttachment has an assigned element but no FEM");
            }
            setFromFem (myPoint.getPosition(), fem);               
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
         if (myFemFrame != null) {
            Point3d ploc = new Point3d(myPoint.getPosition());
            ploc.inverseTransform (myFemFrame.getPose());
            myFemFrame.addPointForce (ploc, force);
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

   public void mulSubGT (MatrixBlock D, MatrixBlock B, int idx) {
      if (myNoFrameRelativeP) {
         addBlock (D, B, idx);
      }
      else {
         D.mulAdd (myMasterBlocks[idx], B);   
      }
   }

   public void mulSubG (MatrixBlock D, MatrixBlock B, int idx) {
      if (myNoFrameRelativeP) {
         addBlock (D, B, idx);
      }
      else {
         MatrixBlock G = myMasterBlocks[idx].createTranspose();
         D.mulAdd (B, G);         
      }
   }

   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
      if (myNoFrameRelativeP) {
         double s = myCoords.get (idx);
         ybuf[yoff  ] += s*xbuf[xoff  ];
         ybuf[yoff+1] += s*xbuf[xoff+1];
         ybuf[yoff+2] += s*xbuf[xoff+2];
      }
      else {
         myMasterBlocks[idx].mulAdd (ybuf, yoff, xbuf, xoff);
      }
   }

   protected static void scanNodes (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      ArrayList<Double> coordList = new ArrayList<Double>();
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN); // begin token
      while (ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
         coordList.add (rtok.scanNumber());
      }
      if (rtok.ttype != ']') {
         throw new IOException ("Expected ']', got " + rtok);
      }
      tokens.offer (ScanToken.END); // terminator token
      tokens.offer (new ObjectToken(ArraySupport.toDoubleArray (coordList)));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "element", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "nodes")) {
         tokens.offer (new StringToken ("nodes", rtok.lineno()));
         scanNodes (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }
   
   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "element")) {
         FemElement3d elem = postscanReference (
            tokens, FemElement3d.class, ancestor);
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
   
   protected static void writeNodes (
      PrintWriter pw, NumberFormat fmt,
      FemNode[] nodes, double[] weights, Object ref)

      throws IOException {
      CompositeComponent ancestor =
         ComponentUtils.castRefToAncestor (ref);
      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int i=0; i<nodes.length; i++) {
         pw.println (
            ComponentUtils.getWritePathName (ancestor, nodes[i])+" "+
            weights[i]);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myElement != null) {
         pw.println (
            "element="+ComponentUtils.getWritePathName (ancestor, myElement));
      }
      pw.print ("nodes=");
      writeNodes (pw, fmt, myNodes, myCoords.getBuffer(), ancestor);
   }

   @Override
   public void transformSlaveGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      updateAttachment();
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
      Point pnt, FemElement3d elem, Point3d loc, double reduceTol) {

      PointFem3dAttachment ax = null;
      // Figure out the coordinates for the attachment point
      VectorNd coords = new VectorNd (elem.numNodes());
      elem.getMarkerCoordinates (coords, loc!=null ? loc : pnt.getPosition(), false);
//      if (reduceTol > 0) {
//         FemNode3d[] nodes = elem.getNodes();
//         // Find number of coordinates which are close to zero
//         int numZero = 0;
//         for (int i=0; i<elem.numNodes(); i++) {
//            if (Math.abs(coords.get(i)) < reduceTol) {
//               numZero++;
//            }
//         }
//         // If we have coordinates close to zero, and the number of remaining
//         // coords is <= 4, then specify the nodes and coords explicitly
//         if (numZero > 0 && elem.numNodes()-numZero <= 4) {
//            int numc = elem.numNodes()-numZero;
//            double[] reducedCoords = new double[numc];
//            FemNode3d[] reducedNodes = new FemNode3d[numc];
//            int k = 0;
//            for (int i=0; i<elem.numNodes(); i++) {
//               if (Math.abs(coords.get(i)) >= reduceTol) {
//                  reducedCoords[k] = coords.get(i);
//                  reducedNodes[k] = nodes[i];
//                  k++;
//               }
//            }
//            ax = new PointFem3dAttachment (pnt);
//            ax.setFromNodes (reducedNodes, reducedCoords);
//            return ax;
//         }
//      }
      ax = new PointFem3dAttachment (pnt);
      ax.setFromElement (pnt.getPosition(), elem, reduceTol);


      //ax.myCoords.set (coords);
      return ax;
   }

   public void addMassToMasters() {
      double m = myPoint.getEffectiveMass();
      if (m != 0) {
         for (int i=0; i<myNodes.length; i++) {
            myNodes[i].addEffectiveMass (m*myCoords.get(i));
         }
      }
      myPoint.addEffectiveMass(-m);
   }
   
   public void addMassToMaster (MatrixBlock mblk, MatrixBlock sblk, int idx) {
      if (idx >= myNodes.length) {
         throw new IllegalArgumentException (
            "Master idx="+idx+" must be in the range [0,"+(myNodes.length-1)+"]");
      }
      if (!(mblk instanceof Matrix3x3Block)) {
         throw new IllegalArgumentException (
            "Master blk not instance of Matrix6dBlock");
      }
      if (!(sblk instanceof Matrix3x3Block)) {
         throw new IllegalArgumentException (
            "Master blk not instance of Matrix3x3Block");
      }
      double ci = myCoords.get(idx);
      // This is ci instead of ci*ci because mass is lumped ...
      double m = ci*((Matrix3x3Block)sblk).m00;
      Matrix3x3Block dblk = (Matrix3x3Block)mblk;
      dblk.m00 += m;
      dblk.m11 += m;
      dblk.m22 += m;
   }

   private boolean computeVelDerivative (Vector3d dvel) {
      Frame pframe = myPoint.getPointFrame();
      boolean isNonZero = false;

      // can we optimize this? If the attachment has been enforced, then can we
      // compute lp2 and lv2 directly from the point itself?
      Vector3d lv2 = new Vector3d();
      Vector3d lp2 = new Vector3d();
      double[] coords = myCoords.getBuffer();      
      for (int i=0; i<myNodes.length; i++) {
         FemNode node = myNodes[i];
         double w = coords[i];
         lv2.scaledAdd (w, node.getLocalVelocity());
         lp2.scaledAdd (w, node.getLocalPosition());
      }

      if (myFemFrame != null) {
         RotationMatrix3d R2 = myFemFrame.getPose().R;
         Twist vel2 = myFemFrame.getVelocity();
         Vector3d tmp1 = new Vector3d();
         Vector3d tmp2 = new Vector3d();
         tmp1.transform (R2, lp2);  // R2*lp2
         tmp2.transform (R2, lv2);  // R2*lv2
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
         // since Fem has no frame, lv2 and world velocity are the same
         dvel.sub (lv2);
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
//      FemNode nodes[] = myNodes;
//      if (point == null || nodes == null) {
//         throw new InternalErrorException ("null point and/or nodes");
//      }
//      super.connectToHierarchy ();
//      point.setAttached (this);
//      for (FemNode node : nodes) {
//         node.addMasterAttachment (this);
//      }
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      Point point = getPoint();
//      FemNode nodes[] = myNodes;
//      if (point == null || nodes == null) {
//         throw new InternalErrorException ("null point and/or nodes");
//      }
//      super.disconnectFromHierarchy();
//      point.setAttached (null);
//      for (FemNode node : nodes) {
//         node.removeMasterAttachment (this);
//      }
//   }

   public FemNode[] getNodes() {
      return myNodes;
   }
   
  @Override
   public void getHardReferences (List<ModelComponent> refs) {
      //Point point = getPoint();
//      FemNode nodes[] = myNodes;
//      if (point == null || nodes == null) {
//         throw new InternalErrorException ("null point and/or nodes");
//      }
      super.getHardReferences (refs);
      refs.add (getPoint());
      if (myNodes != null) {
         for (FemNode node : myNodes) {
            refs.add (node);
         }
      }
   }   
   
   public PointFem3dAttachment copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointFem3dAttachment a = (PointFem3dAttachment)super.copy (flags, copyMap);

      myFemFrame = null; // will be reset in collectMasters()
      if (myNodes != null) {
         a.myNodes = new FemNode[myNodes.length];
         for (int i=0; i<myNodes.length; i++) {
            FemNode node;
            if ((node = (FemNode)copyMap.get(myNodes[i])) == null) {
               node = myNodes[i];
            }
            a.myNodes[i] = node;
         }
      }
      if (myCoords != null) {
         a.myCoords = new VectorNd(myCoords);
      }
      if (myElement != null) {
         FemElement elem;
         if ((elem = (FemElement)copyMap.get(myElement)) == null) {
            elem = myElement;
         }
         a.myElement = elem;
      }
      
      return a;
   }
  

}
