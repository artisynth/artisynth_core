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

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix1x3Block;
import maspack.matrix.Matrix3x1Block;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix3x3DiagBlock;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNdBlock;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.util.*;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.*;

public class PointFem3dAttachment extends PointAttachment {
   //private FemElement myElement;
   private FemNode[] myNodes;
   private VectorNd myCoords;

   public void setPoint (Point pnt) {
      myPoint = pnt;
   }

   public PointFem3dAttachment() {
   }

   public PointFem3dAttachment (Point pnt) {
      myPoint = pnt;        
   }

   public FemNode[] getMasters() {
      if (myNodes != null) {
         return myNodes;
      }
      else {
         return null;
      }
   }

   public int numMasters() {
      if (myNodes != null) {
         return myNodes.length;
      }
      else {
         return 0;
      }
   }
   
//   @Override
//   public void addScaledExternalForce(Point3d pnt, double s, Vector3d f) {
//      // distribute force to nodes based on weights
//      for (int i=0; i<numMasters(); i++) {
//         myNodes[i].addScaledExternalForce(s*myCoords.get(i), f);
//      } 
//   }

   // public FemElement getElement() {
   //    return myElement;
   // }

   /**
    * If the nodes of this attachment correspond to the nodes of
    * a particular element, return that element. In other words,
    * the returned element
    */
   protected FemElement findElementForNodes (FemNode[] nodes) {
      
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

   public void setNodes (
      Collection<FemNode> nodes, Collection<Double> coords) {
      double[] _coords = new double[coords.size()];
      int i = 0;
      for (double w : coords) {
         _coords[i++] = w;
      }
      setNodes (nodes.toArray(new FemNode[0]), _coords);
   }

   public void setNodes (FemNode[] nodes, double[] coords) {
      dosetNodes (nodes, coords);
      //myElement = null;
      if (coords == null) {
         // compute node coordinates explicitly
         updateAttachment();
      }
   }

   private void dosetNodes (FemNode[] nodes, double[] coords) {
      if (nodes.length == 0) {
         throw new IllegalArgumentException (
            "Must have at least one node");
      }
      if (coords != null && nodes.length != coords.length) {
         throw new IllegalArgumentException (
            "nodes and coords have incompatible sizes: "+
            nodes.length+" vs. "+coords.length);
      }
      myCoords = new VectorNd (nodes.length);
      myNodes = new FemNode[nodes.length];
      for (int i=0; i<nodes.length; i++) {
         myNodes[i] = nodes[i];
         if (coords != null) {
            myCoords.set (i, coords[i]);
         }
      }
   }

   /** 
    * Sets the element associated with this attachment. If present, this is
    * used solely for updating the nodal coordinates in the method {@link
    * #updateAttachment updateAttachment}
    */  
   public void setFromElement (FemElement elem) {
      dosetNodes (elem.getNodes(), null);
      //myElement = elem;
   }

   public VectorNd getCoordinates() {
      return myCoords;
   }
   
   public double getCoordinate (int idx) {
      return myCoords.get(idx);
   }

   public void updatePosStates() {
      computePosState (myPoint.getPosition());
   }

   public void computePosState (Vector3d pos) {
      if (myNodes != null) {
         double[] coords = myCoords.getBuffer();
         pos.setZero();
         for (int i = 0; i < myNodes.length; i++) {
            pos.scaledAdd (coords[i], myNodes[i].getPosition(), pos);
         }
      }
   }

   public void updateVelStates() {
      if (myNodes != null) {
         double[] coords = myCoords.getBuffer();
         Vector3d vel = myPoint.getVelocity();
         vel.setZero();
         for (int i = 0; i < myNodes.length; i++) {
            vel.scaledAdd (coords[i], myNodes[i].getVelocity(), vel);
         }
      }
   }

   /**
    * Update attachment to reflect changes in the slave state.
    */
   public void updateAttachment() {
      FemElement elem = findElementForNodes (myNodes);
      if (elem != null) {
         elem.getMarkerCoordinates (myCoords, myPoint.getPosition());
      }
      else {
         computeNodeCoordinates();
      }
   }

   public void applyForces() {
      if (myNodes != null) {
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

   protected MatrixBlock createRowBlock (int colSize) {
      MatrixBlock blk;
      switch (colSize) {
         case 1:
            blk = new Matrix3x1Block();
            break;
         case 3:
            blk = new Matrix3x3Block();
            break;
         default: 
            blk = new MatrixNdBlock (3, colSize);
            break;
      }
      return blk;
   }

   protected MatrixBlock createColBlock (int rowSize) {
      MatrixBlock blk;
      switch (rowSize) {
         case 1:
            blk = new Matrix1x3Block();
            break;
         case 3:
            blk = new Matrix3x3Block();
            break;
         default: 
            blk = new MatrixNdBlock (rowSize, 3);
            break;
      }
      return blk;
   }

   private String blockAddr (MatrixBlock blk) {
      return "(" + blk.getBlockRow() + "," + blk.getBlockCol() + ")";
   }

   private void addBlock (MatrixBlock depBlk, MatrixBlock blk, int idx) {
      depBlk.scaledAdd (myCoords.get (idx), blk);
   }

   public void mulSubGT (MatrixBlock depBlk, MatrixBlock blk, int idx) {
      addBlock (depBlk, blk, idx);
   }

   public void mulSubG (MatrixBlock depBlk, MatrixBlock blk, int idx) {
      addBlock (depBlk, blk, idx);
   }

   public void mulSubGT (
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
      double s = myCoords.get (idx);
      ybuf[yoff  ] += s*xbuf[xoff  ];
      ybuf[yoff+1] += s*xbuf[xoff+1];
      ybuf[yoff+2] += s*xbuf[xoff+2];
   }

   protected void scanNodes (ReaderTokenizer rtok, Deque<ScanToken> tokens)
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
      if (scanAttributeName (rtok, "nodes")) {
         tokens.offer (new StringToken ("nodes", rtok.lineno()));
         scanNodes (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "nodes")) {
         FemNode[] nodes = ScanWriteUtils.postscanReferences (
            tokens, FemNode.class, ancestor);
         double[] coords = (double[])tokens.poll().value();
         setNodes (nodes, coords);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   protected void writeNodes (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      CompositeComponent ancestor =
         ComponentUtils.castRefToAncestor (ref);
      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int i=0; i<myNodes.length; i++) {
         pw.println (
            ComponentUtils.getWritePathName (ancestor, myNodes[i])+" "+
            myCoords.get(i));
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.print ("nodes=");
      writeNodes (pw, fmt, ancestor);
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
            if (warningCnt < WARNING_LIMIT) {
               System.out.println (
                  "PointFem3dAttachment: Warning: attempting to update "+
                  "attachment with "+ myNodes.length +
                  " nodes and no common element");
               warningCnt++;
            }
            else if (warningCnt == WARNING_LIMIT) {
               System.out.println (
                  "PointFem3dAttachment: Warning limit exceeded");
               warningCnt++;
            }
         }
      }
   }
   
   /** 
    * Create an attachment that connects a point to a FemElement.  If the point
    * lies outside the model, then the attachment will be created for the
    * location determined by projecting the point into the nearest element
    * and the new location will be returned in <code>loc</code>.
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
      elem.getMarkerCoordinates (coords, loc!=null ? loc : pnt.getPosition());
      if (reduceTol > 0) {
         FemNode3d[] nodes = elem.getNodes();
         // Find number of coordinates which are close to zero
         int numZero = 0;
         for (int i=0; i<elem.numNodes(); i++) {
            if (Math.abs(coords.get(i)) < reduceTol) {
               numZero++;
            }
         }
         // If we have coordinates close to zero, and the number of remaining
         // coords is <= 4, then specify the nodes and coords explicitly
         if (numZero > 0 && elem.numNodes()-numZero <= 4) {
            int numc = elem.numNodes()-numZero;
            double[] reducedCoords = new double[numc];
            FemNode3d[] reducedNodes = new FemNode3d[numc];
            int k = 0;
            for (int i=0; i<elem.numNodes(); i++) {
               if (Math.abs(coords.get(i)) >= reduceTol) {
                  reducedCoords[k] = coords.get(i);
                  reducedNodes[k] = nodes[i];
                  k++;
               }
            }
            ax = new PointFem3dAttachment (pnt);
            ax.setNodes (reducedNodes, reducedCoords);
            return ax;
         }
      }
      ax = new PointFem3dAttachment (pnt);
      ax.setFromElement (elem);


      ax.myCoords.set (coords);
      return ax;
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

   public boolean getDerivative (double[] buf, int idx) {
      buf[idx  ] = 0;
      buf[idx+1] = 0;
      buf[idx+2] = 0;
      return false;
   }

   public PointFem3dAttachment (Point pnt, FemElement elem) {
      this();
      setFromElement (elem);
      setPoint (pnt);
   }

   public PointFem3dAttachment (Point pnt, FemNode[] nodes) {
      this();
      setNodes (nodes, /*coords=*/null);
      setPoint (pnt);
   }

   public PointFem3dAttachment (Point pnt, FemNode[] nodes, double[] coords) {
      this();
      setNodes (nodes, coords);
      setPoint (pnt);
   }

   @Override
   public void connectToHierarchy () {
      Point point = getPoint();
      FemNode nodes[] = getMasters();
      if (point == null || nodes == null) {
         throw new InternalErrorException ("null point and/or nodes");
      }
      super.connectToHierarchy ();
      point.setAttached (this);
      for (FemNode node : nodes) {
         node.addMasterAttachment (this);
      }
   }

   @Override
   public void disconnectFromHierarchy() {
      Point point = getPoint();
      FemNode nodes[] = getMasters();
      if (point == null || nodes == null) {
         throw new InternalErrorException ("null point and/or nodes");
      }
      super.disconnectFromHierarchy();
      point.setAttached (null);
      for (FemNode node : nodes) {
         node.removeMasterAttachment (this);
      }
   }

  @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      Point point = getPoint();
      FemNode nodes[] = getMasters();
      if (point == null || nodes == null) {
         throw new InternalErrorException ("null point and/or nodes");
      }
      super.getHardReferences (refs);
      refs.add (point);
      for (FemNode node : nodes) {
         refs.add (node);
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
      return a;
   }
  

}
