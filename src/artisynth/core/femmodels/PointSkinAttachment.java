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
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.awt.Color;

import artisynth.core.femmodels.SkinMeshBody.FrameBlending;
import artisynth.core.femmodels.SkinMeshBody.FrameInfo;
import artisynth.core.femmodels.SkinMeshBody.FemModelInfo;
import artisynth.core.femmodels.SkinMeshBody.BodyInfo;
import artisynth.core.femmodels.SkinMeshBody.NearestPoint;
import artisynth.core.mechmodels.ContactMaster;
import artisynth.core.mechmodels.ContactPoint;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.DualQuaternionDeriv;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.util.IntegerToken;
import artisynth.core.util.ObjectToken;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.DualQuaternion;
import maspack.matrix.DualScalar;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix3x4Block;
import maspack.matrix.Matrix3x1Block;
import maspack.matrix.Matrix3x2Block;
import maspack.matrix.Matrix6x3;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6x3Block;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Point3d;
import maspack.matrix.PolarDecomposition3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.ArraySupport;
import maspack.util.DoubleHolder;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ObjectSizeAgent;
import maspack.util.RandomGenerator;
import maspack.util.ReaderTokenizer;
import maspack.properties.*;
import maspack.render.*;
import maspack.geometry.*;

/**
 * A special attachment class that is used to update a point's position based
 * on the state of the controlling bodies (such as Frames and FemModels) in a
 * SkinMeshBody. Instances of this class (without an actual slave Point) are
 * used to control the positions of each vertex in a SkinMeshBody.
 * 
 * <p>
 * The class maintains a list of <code>Connection</code> objects for each
 * underlying dynamic component (such as a <code>Frame</code> or
 * <code>FemNode3d</code>) that has a weighted influence on the point's final
 * value.
 */
public class PointSkinAttachment extends PointAttachment
   implements TransformableGeometry, ScalableUnits, ContactMaster {

   public boolean useFemMasterBlocksForContact = true;

   protected FrameConnection myFrameConnections = null;
   protected FemConnection myFemConnections = null;
   protected MatrixBlock[] myMasterBlocks = null;
   protected boolean myMasterBlocksValid = false;
   protected int[] myFemMasterIdxs = null;
   protected Point3d myBasePos;
   protected float myBaseWeight = 0;
   
   // extra fields in case this attachment is feature-based:
   protected boolean myFeatureBased = false;
   
   protected SkinMeshBody mySkinMesh = null;

   /**
    * Defines connections associated with this attachment.
    */
   public static abstract class SkinConnection<C extends SkinConnection<C>> {

      protected float myWeight;

      public enum Type {
         F,  // frame
         D,  // FEM displacement
         E,  // FEM element
      }

      /**
       * Returns a short string describing the type for this connection.
       */
      public abstract Type getType();

      /**
       * Returns the weight for this connection.
       */
      public float getWeight() {
         return myWeight;
      }

      /**
       * Sets the weight for this connection.
       */
      public void setWeight (double w) {
         myWeight = (float)w;
      }

      /**
       * Scales the weight for this connection.
       */
      public void scaleWeight (double s) {
         myWeight *= s;
      }
      
      public abstract C copy();

      public abstract C getNext();

      public abstract void setNext (C next);

//      public abstract void createVertexMasters (
//         List<ContactMaster> mlist, double wgt, ContactPoint cpnt);
//
      public abstract void write (
         PrintWriter pw, double wscale, NumberFormat fmt,
         CompositeComponent ancestor) throws IOException;

      public abstract void scan (
         ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException;

      public abstract void postscan (
         Deque<ScanToken> tokens, List<BodyInfo> bodyInfos) throws IOException;

      protected BodyInfo postscanBodyInfo (
         Deque<ScanToken> tokens, List<BodyInfo> bodyInfos,
         Class<? extends BodyInfo> type) throws IOException {

         if (bodyInfos == null) {
            throw new IOException ("No bodyInfos available");
         }
         ScanToken tok;
         if (!((tok=tokens.poll()) instanceof IntegerToken)) {
            throw new IOException ("Expected IntegerToken, got "+tok);
         }         
         int idx = ((IntegerToken)tok).value();
         if (idx >= bodyInfos.size()) {
            throw new IOException (
                "Master body index " + idx +
               " exceeds number of master bodies " + bodyInfos.size());
         }
         BodyInfo binfo = bodyInfos.get(idx);
         if (!(type.isInstance (binfo))) {
            throw new IOException (
               "Master body at index " + idx +
               " does not correspond to a " + type.getName());
         }
         return binfo;
      }

      public abstract void scaleDistance (double s);
   }

   public class FrameConnection extends SkinConnection<FrameConnection> {

      FrameInfo myFrameInfo;
      FrameConnection myNext;

      public SkinConnection.Type getType() {
         return Type.F;
      }

      public FrameConnection getNext() {
         return myNext;
      }

      public void setNext (FrameConnection next) {
         myNext = next;
      }

      public Frame getFrame() {
         return myFrameInfo.myFrame;
      }

      public FrameConnection() {
      }
   
      public FrameConnection (FrameInfo frameInfo, double weight) {
         myFrameInfo = frameInfo;
         myWeight = (float)weight;
      }

//      public void createVertexMasters (
//         List<ContactMaster> mlist, double wgt, ContactPoint cpnt) {
//         mlist.add (
//            new CompContactMaster (myFrameInfo.myFrame, wgt*myWeight));
//      }
//
      //@Override
      public int addVelocity (Vector3d v, MatrixBlock[] blks, int idx) {
         Frame frame = myFrameInfo.myFrame;
         mulTransposeAdd (
            v, (Matrix6x3Block)blks[idx++], frame.getVelocity());
         return idx;
      }
      
      //@Override
      public int applyForce (Vector3d f, MatrixBlock[] blks, int idx) {
         Wrench wr = new Wrench();
         mul (wr, (Matrix6x3Block)blks[idx++], f);
         myFrameInfo.myFrame.addForce (wr);
         return idx;
      }

      public void write (
         PrintWriter pw, double wscale, NumberFormat fmt,
         CompositeComponent ancestor) throws IOException {
         int frameIdx = myFrameInfo.getIndex();
         pw.println ("[ "+frameIdx+" "+fmt.format(wscale*myWeight)+" ]");
      }

      public void scan (
         ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

         rtok.scanToken ('[');
         int frameIdx = rtok.scanInteger();
         tokens.offer (new IntegerToken (frameIdx, rtok.lineno()));
         myWeight = (float)rtok.scanNumber();
         rtok.scanToken (']');
      }

      public void postscan (
         Deque<ScanToken> tokens, List<BodyInfo> bodyInfos) 
         throws IOException {

         myFrameInfo = (FrameInfo)postscanBodyInfo (
            tokens, bodyInfos, FrameInfo.class);
      }      

      public FrameConnection copy() {
         return new FrameConnection(myFrameInfo, myWeight);
      }

      public void scaleDistance (double s) {
         // nothing to do
      }

      public void scaledAdd (double s, FrameConnection fcon) {
         myWeight += s*fcon.myWeight;
      }

      protected void normalize() {
         // nothing to do
      }
   }

   public static abstract class FemConnection
      extends SkinConnection<FemConnection> {

      float[] myCoords;
      FemModelInfo myFemInfo;
      FemConnection myNext;

      public FemModel3d getFemModel() {
         return myFemInfo.getBody();
      }

      public FemConnection getNext() {
         return myNext;
      }

      public void setNext (FemConnection next) {
         myNext = next;
      }

//      public abstract int addVelocity (Vector3d v, MatrixBlock[] blks, int idx);
//      
//      public abstract int applyForce (Vector3d f, MatrixBlock[] blks, int idx);

      public abstract void addPosition (
         Vector3d pos, PolarDecomposition3d polard);

      public abstract int updateMasterBlocks (
         MatrixBlock[] blks, int[] blockIdxs, int bi);

      public abstract FemNode3d[] getNodes();

      // must override if subclass has offset
      public boolean hasOffset() {
         return false;
      }
      
      // must override if subclass has offset
      public double getOffset() {
         return 0;
      }
      
      // must override if subclass has offset
      public void setOffset (double off) {
      }

      protected abstract void normalize();

      /* --- begin ContactMaster implementation --- */

      public void add1DConstraintBlocks (
         SparseBlockMatrix GT, int bj, double scale, 
         ContactPoint cpnt, Vector3d dir) {
         FemNode3d[] nodes = getNodes();
         for (int i=0; i<nodes.length; i++) {
            int bi = nodes[i].getSolveIndex();
            if (bi != -1) {
               Matrix3x1Block blk = (Matrix3x1Block)GT.getBlock (bi, bj);
               if (blk == null) {
                  blk = new Matrix3x1Block();
                  GT.addBlock (bi, bj, blk);
               }
               blk.scaledAdd (scale*(myWeight*myCoords[i]), dir);
            }
         }
      }

      public void add2DConstraintBlocks (
         SparseBlockMatrix GT, int bj, double scale, 
         ContactPoint cpnt, Vector3d dir0, Vector3d dir1) {
         FemNode3d[] nodes = getNodes();
         for (int i=0; i<nodes.length; i++) {
            int bi = nodes[i].getSolveIndex();
            if (bi != -1) {
               Matrix3x2Block blk = (Matrix3x2Block)GT.getBlock (bi, bj);
               if (blk == null) {
                  blk = new Matrix3x2Block();
                  GT.addBlock (bi, bj, blk);
               }
               double s = scale*(myWeight*myCoords[i]);
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
         FemNode3d[] nodes = getNodes();
         for (int i=0; i<nodes.length; i++) {
            vel.scaledAdd (scale*(myWeight*myCoords[i]), nodes[i].getVelocity());
         }
      }

      public boolean isControllable() {
         for (FemNode3d node : getNodes()) {
            if (node.isControllable()) {
               return true;
            }
         }
         return false;
      }
   
      public int collectMasterComponents (
         HashSet<DynamicComponent> masters, boolean activeOnly) {
         int num = 0;
         for (FemNode3d node : getNodes()) {
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
   
   public class FemDispConnection extends FemConnection {

      public FemNode3d[] myNodes;

      public SkinConnection.Type getType() {
         return Type.D;
      }

      public FemDispConnection() {
      }

      public FemDispConnection (
         FemNode3d[] nodes, double[] coords, FemModelInfo finfo, double wgt) {

         if (coords != null) {
            myCoords = doubleToFloatArray (coords);
         }
         myNodes = Arrays.copyOf (nodes, nodes.length);
         myFemInfo = finfo;
         myWeight = (float)wgt;
      }

      public FemNode3d[] getNodes() {
         return myNodes;
      }

      public int numMasters() {
         return myCoords.length;
      }
   
//      public void createVertexMasters (
//         List<ContactMaster> mlist, double wgt, ContactPoint cpnt) {
//         for (int i=0; i<myNodes.length; i++) {
//            mlist.add (
//               new CompContactMaster (
//                  myNodes[i], wgt*(myWeight*myCoords[i])));
//         }
//      }

      public void addPosition (
         Vector3d pos, PolarDecomposition3d polard) {
         Vector3d tmp = new Vector3d();
         for (int i=0; i<myNodes.length; i++) {
            tmp.sub (myNodes[i].getPosition(), myNodes[i].getRestPosition());
            pos.scaledAdd (myWeight*myCoords[i], myNodes[i].getPosition());
            pos.scaledAdd (-myWeight*myCoords[i], myNodes[i].getRestPosition());
         }
         pos.scaledAdd (myWeight, myBasePos);
      }

      public int updateMasterBlocks (
         MatrixBlock[] blks, int[] blockIdxs, int bi) {
         for (int i=0; i<myNodes.length; i++) {
            Matrix3x3Block blk = (Matrix3x3Block)blks[blockIdxs[bi++]];
            blk.addDiagonal (myWeight*myCoords[i]);
         }
         return bi;
      }

      public FemDispConnection copy() {
         FemDispConnection dcon = 
            new FemDispConnection (
               myNodes, null, myFemInfo, myWeight);
         dcon.myCoords = Arrays.copyOf (myCoords, myCoords.length);
         return dcon;
      }

      public void scaleDistance (double s) {
         // nothing to do
      }

      public void write (
         PrintWriter pw, double wscale, NumberFormat fmt,
         CompositeComponent ancestor) throws IOException {

         int femIdx = myFemInfo.getIndex();
         pw.print ("[ "+femIdx+" "+fmt.format(wscale*myWeight));
         for (int i=0; i<myNodes.length; i++) {
            pw.print (" "+myNodes[i].getNumber()+" "+fmt.format(myCoords[i]));
         }
         pw.println ("]");
      }

      public void scan (
         ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

         rtok.scanToken ('[');
         int femIdx = rtok.scanInteger();
         tokens.offer (new IntegerToken (femIdx, rtok.lineno()));
         myWeight = (float)rtok.scanNumber();
         ArrayList<Integer> nodeNumbers = new ArrayList<>();
         ArrayList<Float> coordList = new ArrayList<>();
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            nodeNumbers.add (rtok.scanInteger());
            coordList.add ((float)rtok.scanNumber());
         }
         myCoords = ArraySupport.toFloatArray (coordList);
         // push node numbers onto the token queue for postscanning ...
         tokens.offer (new ObjectToken(ArraySupport.toIntArray (nodeNumbers)));
      }

      public void postscan (
         Deque<ScanToken> tokens, List<BodyInfo> bodyInfos)
         throws IOException {

         myFemInfo = (FemModelInfo)postscanBodyInfo (
            tokens, bodyInfos, FemModelInfo.class);
         ScanToken tok;
         if (!((tok=tokens.poll()) instanceof ObjectToken)) {
            throw new IOException ("Expected ObjectToken, got "+tok);
         }         
         int[] nodeNumbers = (int[])((ObjectToken)tok).value();
         myNodes = new FemNode3d[nodeNumbers.length];
         FemModel3d fem = myFemInfo.getFemModel();
         for (int i=0; i<nodeNumbers.length; i++) {
            FemNode3d n = fem.getNodeByNumber(nodeNumbers[i]);
            if (n == null) {
               throw new IOException (
                  "Node number " + nodeNumbers[i] +
                  " not found in FEM model " + ComponentUtils.getPathName(fem));
            }
            myNodes[i] = n;
         }
      }

      protected void normalize() {
         // nothing to do
      }

   }

   public static class ElementConnection
      extends FemConnection {

      FemElement3dBase myElem;
      
      Vector3d myD0;

      public SkinConnection.Type getType() {
         return Type.E;
      }

      public FemNode3d[] getNodes() {
         return myElem.getNodes();
      }

      public ElementConnection() {
      }

      public ElementConnection (FemElement3dBase elem, FemModelInfo finfo) {
         myElem = elem;
         myFemInfo = finfo;
         myCoords = new float[elem.numNodes()];
      }

      public ElementConnection (
         FemElement3dBase elem, double[] coords,
         Vector3d dr, FemModelInfo finfo, double wgt) {

         myElem = elem;
         if (coords != null) {
            myCoords = doubleToFloatArray (coords);
         }

         if (dr != null) {
            myD0 = new Vector3d (dr);
         }
         myFemInfo = finfo;
         myWeight = (float)wgt;
      }

      public FemElement3dBase getElement() {
         return myElem;
      }
   
//      public void createVertexMasters (
//         List<ContactMaster> mlist, double wgt, ContactPoint cpnt) {
//         FemNode3d[] nodes = myElem.getNodes();
//         for (int i=0; i<nodes.length; i++) {
//            mlist.add (
//               new CompContactMaster (
//                  nodes[i], wgt*(myWeight*myCoords[i])));
//         }
//      }
//
      public void addElementPos (Vector3d pos) {
         FemNode3d[] nodes = myElem.getNodes();
         for (int i=0; i<nodes.length; i++) {
            pos.scaledAdd (myCoords[i], nodes[i].getPosition());
         }
      }

      public void addPosition (
         Vector3d pos, PolarDecomposition3d polard) {
         FemNode3d[] nodes = myElem.getNodes();
         // main position part
         Vector3d tmp = new Vector3d();
         RotationMatrix3d R = myElem.getRotation (polard);
         tmp.transform (R, myD0);
         addElementPos (tmp);
         pos.scaledAdd (myWeight, tmp);
      }

      public int updateMasterBlocks (
         MatrixBlock[] blks, int[] blockIdxs, int bi) {
         // When there is no offset, each master block is given by
         //
         // w_i I
         // 
         // If there is an offset d0, they contain the rotational term
         //                            
         // - R [ GNX_i^T ] inv(B) [ d0 ] R^T
         //
         // where B = tr(H)I - H and H is the symmetric part of the polar
         // decomposition, R is the rotational part of the polar decomposition,
         // and GNX_i is the i-th node shape function derivative with respect
         // to material coordinates.
         //
         // GNX_i^T can be computed from the derivative with respect to natural
         // coordinates GNs_i by
         //
         // GNX_i^T = inv(J0)^T GNs_i^T
         //
         // where J0 is the rest Jacobian of the element's warping point.
         //
         // For computational efficiency, the rotational term can be rerranged
         // as
         //
         // - [ R inv(J0)^T GNs_i^T ] R inv(B) [ d0 ] R^T = [ RJ GNs_i^T ] RB
         // 
         // where RJ =  R inv(J0)^T and RB = - R inv(B) [ d0 ] R^T
         //
         Matrix3d RJ = null;
         Matrix3d RB = null;
         Matrix3d Tmp = null;
         Vector3d[] GNs = null; 
         Vector3d RGNX_i = null;

         if (myD0 != null) {
            RotationMatrix3d R = myElem.getRotation();
            Matrix3d H = myElem.getDeformation();

            RJ = new Matrix3d(); // R inv(J0)^T
            RJ.mulTransposeRight (R, myElem.getWarpingData().myInvJ0);
            RB = new Matrix3d(); // - R inv(B) [ d0 ] R^T

            // start by computing -B = H - tr(H)I
            RB.set (H);
            RB.addDiagonal (-RB.trace());
            // invert -B and then form  - R inv(B) [ d0 ] R^T
            RB.invert();
            RB.crossProduct (RB, myD0);
            RB.mulTranspose (R);
            RB.mul (R, RB);

            GNs = myElem.getWarpingPoint().getGNs();
            RGNX_i = new Vector3d();
            Tmp = new Matrix3d(); // use to compute block term
         }
         for (int i=0; i<myElem.numNodes(); i++) {
            Matrix3x3Block blk = (Matrix3x3Block)blks[blockIdxs[bi++]];
            if (myD0 != null) {
               RJ.mul (RGNX_i, GNs[i]);
               Tmp.crossProduct (RGNX_i, RB);
               Tmp.addDiagonal (myCoords[i]);
               blk.scaledAdd (myWeight, Tmp);
            }
            else {
               blk.addDiagonal (myWeight*myCoords[i]);
            }
         }
         return bi;
      }

//     @Override
//     public int addVelocity (Vector3d v, MatrixBlock[] blks, int idx) {
//        FemNode3d[] nodes = myElem.getNodes();
//        for (int i=0; i<nodes.length; i++) {
//           ((Matrix3d)blks[idx++]).mulTransposeAdd (
//              v, nodes[i].getVelocity(), v);
//        }
//        return idx;
//     }
//
//     @Override
//     public int applyForce (Vector3d f, MatrixBlock[] blks, int idx) {
//        FemNode3d[] nodes = myElem.getNodes();
//        Vector3d tmp = new Vector3d();
//        for (int i=0; i<nodes.length; i++) {
//           ((Matrix3d)blks[idx++]).mul (tmp, f);
//           nodes[i].addForce (tmp);
//        }
//        return idx;
//     }

      /* --- reimplementation of some ContactMaster methods --- */

      public void add1DConstraintBlocks (
         SparseBlockMatrix GT, int bj, double scale, 
         ContactPoint cpnt, Vector3d dir) {
         FemNode3d[] nodes = getNodes();
         for (int i=0; i<nodes.length; i++) {
            int bi = nodes[i].getSolveIndex();
            if (bi != -1) {
               Matrix3x1Block blk = (Matrix3x1Block)GT.getBlock (bi, bj);
               if (blk == null) {
                  blk = new Matrix3x1Block();
                  GT.addBlock (bi, bj, blk);
               }
               blk.scaledAdd (scale*(myWeight*myCoords[i]), dir);
            }
         }
      }

      public void add2DConstraintBlocks (
         SparseBlockMatrix GT, int bj, double scale, 
         ContactPoint cpnt, Vector3d dir0, Vector3d dir1) {
         FemNode3d[] nodes = getNodes();
         for (int i=0; i<nodes.length; i++) {
            int bi = nodes[i].getSolveIndex();
            if (bi != -1) {
               Matrix3x2Block blk = (Matrix3x2Block)GT.getBlock (bi, bj);
               if (blk == null) {
                  blk = new Matrix3x2Block();
                  GT.addBlock (bi, bj, blk);
               }
               double s = scale*(myWeight*myCoords[i]);
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
         FemNode3d[] nodes = getNodes();
         for (int i=0; i<nodes.length; i++) {
            vel.scaledAdd (scale*(myWeight*myCoords[i]), nodes[i].getVelocity());
         }
      }

      /* --- end ContactMaster reimplementation --- */

      public ElementConnection copy() {
         ElementConnection econ = 
            new ElementConnection (
               myElem, null, myD0, myFemInfo, myWeight);
         econ.myCoords = Arrays.copyOf (myCoords, myCoords.length);
         return econ;
      }

      public void scaleDistance (double s) {
         if (myD0 != null) {
            myD0.scale (s);
         }
         // nothing to do
      }

      public void write (
         PrintWriter pw, double wscale, NumberFormat fmt,
         CompositeComponent ancestor) throws IOException {

         IndentingPrintWriter.addIndentation (pw, 2);
         int femIdx = myFemInfo.getIndex();
         pw.print ("[ "+femIdx+" "+fmt.format(wscale*myWeight));
         pw.print (" "+myElem.getNumber()+" ");
         if (myD0 == null) {
            pw.println ("0 0 0");
         }
         else {
            pw.println (myD0.toString(fmt));
         }
         pw.print (fmt.format(myCoords[0]));
         for (int i=1; i<myCoords.length; i++) {
            pw.print (" " + fmt.format(myCoords[i]));
         }
         pw.println ("");
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }

      public void scan (
         ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

         rtok.scanToken ('[');
         int femIdx = rtok.scanInteger();
         tokens.offer (new IntegerToken (femIdx, rtok.lineno()));
         myWeight = (float)rtok.scanNumber();
         int elemIdx = rtok.scanInteger();
         tokens.offer (new IntegerToken (elemIdx, rtok.lineno()));
         Vector3d d0 = new Vector3d();
         d0.scan (rtok);
         if (d0.equals (Vector3d.ZERO)) {
            myD0 = null;
         }
         else {
            myD0 = d0;
         }
         ArrayList<Float> coordList = new ArrayList<>();
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            coordList.add ((float)rtok.scanNumber());
         }
         myCoords = ArraySupport.toFloatArray (coordList);
      }

      public void postscan (
         Deque<ScanToken> tokens, List<BodyInfo> bodyInfos)
         throws IOException {

         myFemInfo = (FemModelInfo)postscanBodyInfo (
            tokens, bodyInfos, FemModelInfo.class);

         ScanToken tok;
         if (!((tok=tokens.poll()) instanceof IntegerToken)) {
            throw new IOException ("Expected IntegerToken, got "+tok);
         }         
         int elemNum = ((IntegerToken)tok).value();
         FemModel3d fem = myFemInfo.getFemModel();
         myElem = fem.getElementByNumber(elemNum);
         if (myElem == null) {
            throw new IOException (
               "Element number " + elemNum + 
                  " not found in FEM model " + ComponentUtils.getPathName(fem));
         }
      }

      void scaledAdd (double s, ElementConnection econ) {
         if (econ.myElem != myElem) {
            throw new InternalErrorException (
               "Elements are not the same");
         }
         double sw = s*econ.myWeight;
         myWeight += sw;
         for (int i=0; i<econ.myCoords.length; i++) {
            myCoords[i] += sw*econ.myCoords[i];
         }
         if (econ.myD0 != null) {
            if (myD0 == null) {
               myD0 = new Vector3d();
            }
            myD0.scaledAdd (sw, econ.myD0);
         }
      }

      protected void normalize () {
         double invw = 1/myWeight;
         for (int i=0; i<myCoords.length; i++) {
            myCoords[i] *= invw;
         }
         if (myD0 != null) {
            myD0.scale (invw);
         }
      }

   }

   private FrameBlending getFrameBlending() {
      return getSkinMesh().getFrameBlending();
   }

   /**
    * Finds the SkinMeshBody, if any, associated with this attachment.
    */
   public SkinMeshBody getSkinMesh() {
      if (mySkinMesh != null) {
         return mySkinMesh;
      }
      else {
         CompositeComponent gparent = getGrandParent();
         if (gparent instanceof SkinMeshBody) {
            return (SkinMeshBody)gparent;
         }
         else {
            return null;
         }
      }
   }

   /**
    * Gets the base position for this attachment. Base positions are used for
    * displacement-based position computations.
    */
   public Point3d getBasePosition() {
      return myBasePos;
   }

   /**
    * Sets the base position for this attachment.
    */
   public void setBasePosition(Vector3d pos) {
      myBasePos.set(pos);
   }

   /**
    * Gets the base weight for this attachment. Base weights are used
    * to add a fraction of the base position to the skinned position.
    */
   public float getBaseWeight() {
      return myBaseWeight;
   }

   /**
    * Sets the base weight for this attachment.
    * 
    * @param w new base weight
    * @param normalize if {@code true} and the connection weights sum to a
    * non-zero value, scales the connections weights so that the total weight
    * sum remains unchanged
    */
   public void setBaseWeight (double w, boolean normalize) {
      myBaseWeight = (float)w;
      if (normalize) {
         double sumc = sumConnectionWeights();
         if (sumc != 0) {
            scaleConnectionWeights ((sumc+myBaseWeight-w)/sumc);
         }
      }
      myBaseWeight = (float)w;
   }

   /**
    * Returns the number of connections (to master components) used by this
    * attachment.
    */
   public int numConnections() {
      return numFrameConnections() + numFemConnections();
   }

   public SkinConnection getConnection (int idx) {
      int cnt = 0;
      for (SkinConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         if (cnt == idx) {
            return c;
         }
         cnt++;
      }
      for (SkinConnection c=myFemConnections; c!=null; c=c.getNext()) {
         if (cnt == idx) {
            return c;
         }
         cnt++;
      }  
      return null;
   }
   
   public double getConnectionWeight (int idx) {
      SkinConnection c = getConnection(idx);
      return (c != null ? c.getWeight() : 0); 
   }
   
   private int numConnections (SkinConnection first) {
      int cnt = 0;
      for (SkinConnection c=first; c!=null; c=c.getNext()) {
         cnt++;
      }
      return cnt;
   }

   /**
    * Clear the connections used by this attachment.
    */
   public void clearConnections() {
      myFrameConnections = null;
      myFemConnections = null;
      invalidateMasters();
   }
   
   /**
    * Add backrefs if this attachment is connected *and* controlling a point
    */
   public void addBackRefsIfConnected(){
      if (myPoint != null) {
         super.addBackRefsIfConnected();
      }
   }

   /**
    * Remove backrefs if this attachment is connected *and* controlling a point
    */
   public void removeBackRefsIfConnected(){
      if (myPoint != null &&
          (myFrameConnections != null || myFemConnections != null)) {
         super.removeBackRefsIfConnected();
      }
   }

   private SkinConnection getConnection (SkinConnection first, int idx) {
      int cnt = 0;
      for (SkinConnection c=first; c!=null; c=c.getNext()) {
         if (cnt == idx) {
            return c;
         }
         cnt++;
      }
      return null;
   }

   /**
    * Adds a Frame connection to this attachment. The Frame's displacement from
    * it's rest pose will be used to compute a weighted contribution to the
    * point value, using a mechanism determined by the blend type associated
    * with the SkinMeshBody (as returned by {@link
    * SkinMeshBody#getFrameBlending()}).
    * 
    * @param frameInfo
    * FrameInfo structure within the associated SkinMeshBody.
    * @param weight
    * connection weight.addFemConnection
    */
   public void addFrameConnection (FrameInfo frameInfo, double weight) {
      addFrameConnection (new FrameConnection(frameInfo, weight));
   }

   protected <C extends SkinConnection<C>> void appendConnection (C prev, C con) {
      while (prev.getNext() != null) {
         prev = prev.getNext();
      }
      prev.setNext (con);
      con.setNext (null);      
   }

   protected void addFrameConnection (FrameConnection fcon) {
      if (myFrameConnections == null) {
         myFrameConnections = fcon;
         fcon.myNext = null;
      }
      else {
         appendConnection (myFrameConnections, fcon);
      }
   }

   public FrameConnection getFrameConnections() {
      return myFrameConnections;
   }

   public FrameConnection getFrameConnection (int idx) {
      return (FrameConnection)getConnection (myFrameConnections, idx);
   }

   FrameConnection findMatchingFrameConnection (FrameConnection fcon) {
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         if (c.myFrameInfo == fcon.myFrameInfo) {
            return c;
         }
      }
      return null;
   }

   ElementConnection findMatchingElementConnection (ElementConnection econ) {
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         if (c instanceof ElementConnection) {
            ElementConnection ec = (ElementConnection)c;
            if (ec.myElem == econ.myElem) {
               return ec;
            }
         }
      }
      return null;
   }

   public int numFrameConnections() {
      return numConnections(myFrameConnections);
   }

   protected static void computeElementRotation (
      RotationMatrix3d R, FemElement3dBase elem, PolarDecomposition3d polard) {
      
      IntegrationPoint3d ipnt = elem.getWarpingPoint();
      IntegrationData3d idata = elem.getWarpingData();

      Matrix3d F = new Matrix3d();
      ipnt.computeGradient (F, elem.getNodes(), idata.myInvJ0);
      polard.factor (F);
      polard.getR (R);     
   }
   
   ElementConnection addElementConnection (
      Point3d pos, NearestPoint drec, FemModelInfo finfo, double wgt) {

      FemElement3dBase elem = drec.elem;

      VectorNd coords = new VectorNd (elem.numNodes());
      boolean converged = 
         elem.getMarkerCoordinates (coords, null, drec.nearPoint, false);

      ElementConnection econ;
      if (drec.distance != 0) {
         RotationMatrix3d R = new RotationMatrix3d();
         computeElementRotation (R, elem, new PolarDecomposition3d());
         
         Vector3d dr = new Vector3d(); // displacement in the rest position
         dr.sub (pos, drec.nearPoint);
         dr.inverseTransform (R);
         econ = new ElementConnection (
            elem, coords.getBuffer(), dr, finfo, wgt);
      }
      else {
         econ = new ElementConnection (
            elem, coords.getBuffer(), null, finfo, wgt);
      }
      addFemConnection (econ);
      return econ;
   }
  
    FemDispConnection addDisplacementConnection (
      Point3d pos, NearestPoint drec, FemModelInfo finfo, double wgt) {

      FemModel3d fem = finfo.getFemModel();

      FemNode3d[] nodes = new FemNode3d[3];
      double[] coords = new double[3];
      double[] barycentric = new double[3];
      int nnodes = 0;
      Vertex3d[] vtxs = drec.face.getVertices();
      barycentric[2] = drec.uv.y;
      barycentric[1] = drec.uv.x;
      barycentric[0] = 1 - barycentric[1] - barycentric[2];
      for (int k=0; k<3; k++) {
         if (barycentric[k] != 0) {
            FemNode3d node = fem.getSurfaceNode (vtxs[k]);
            if (node != null) {
               nodes[nnodes] = node;
               coords[nnodes] = barycentric[k];
               nnodes++;
            }
         }
      }
      FemDispConnection dcon = null;
      if (nnodes > 0) {
         dcon = new FemDispConnection (
            Arrays.copyOf (nodes, nnodes),
            Arrays.copyOf (coords, nnodes), finfo, wgt);
         addFemConnection (dcon);
      }
      return dcon;
   }

   protected void addScaledConnections (double s, PointSkinAttachment a) {
      for (FrameConnection c=a.myFrameConnections; c!=null; c=c.getNext()) {
         FrameConnection prev = findMatchingFrameConnection (c);
         if (prev == null) {
            prev = new FrameConnection (c.myFrameInfo, 0);
            addFrameConnection (prev);
         }
         prev.scaledAdd (s, c);
      }
      for (FemConnection c=a.myFemConnections; c!=null; c=c.getNext()) {
         if (c instanceof ElementConnection) {
            ElementConnection econ = (ElementConnection)c;
            ElementConnection prev = findMatchingElementConnection (econ);
            if (prev == null) {
               prev = new ElementConnection (econ.myElem, econ.myFemInfo);
               addFemConnection (prev);
            }
            prev.scaledAdd (s, econ);   
         }
      }
   }

   protected void normalize() {
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         c.normalize();
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         c.normalize();
      }
   }

   protected void addFemConnection (FemConnection fcon) {   
      if (myFemConnections == null) {
         myFemConnections = fcon;
         fcon.myNext = null;
      }
      else {
         appendConnection (myFemConnections, fcon);
      }
   }

   public FemConnection getFemConnections() {
      return myFemConnections;
   }

   public FemConnection getFemConnection (int idx) {
      return (FemConnection)getConnection (myFemConnections, idx);
   }

   public int numFemConnections() {
      return numConnections(myFemConnections);
   }

   static float[] doubleToFloatArray (double[] dvals) {
      float[] fvals = new float[dvals.length];
      for (int i=0; i<dvals.length; i++) {
         fvals[i] = (float)dvals[i];
      }
      return fvals;
   }

   public void invalidateMasters() {
      super.invalidateMasters();
      myMasterBlocks = null;
      myMasterBlocksValid = false;
   }  

   protected void printConnections () {
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         System.out.printf (
            "F %8.5f %s\n", c.getWeight(),
            ComponentUtils.getPathName (c.getFrame()));
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         System.out.printf (
            "%s %8.5f %s\n", c.getType().toString(), c.getWeight(),
            ComponentUtils.getPathName (c.getFemModel()));
      }
   }

   protected void collectMasters (List<DynamicComponent> masters) {
      //super.collectMasters (masters);
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         masters.add (c.getFrame());
      }
      LinkedHashSet<FemNode3d> nodeMasters = new LinkedHashSet<>();
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         for (FemNode3d n : c.getNodes()) {
            nodeMasters.add (n);
         }
      }
      masters.addAll (nodeMasters);
   }

//   public void createVertexMasters (
//      List<ContactMaster> mlist, double wgt, ContactPoint cpnt) {
//      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
//         c.createVertexMasters (mlist, wgt, cpnt);
//      }
//      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
//         c.createVertexMasters (mlist, wgt, cpnt);
//      }
//   }                                    
   
   /**
    * Returns the most recently updated set of master blocks for this
    * attachments. Used for testing and debugging only.
    */
   public MatrixBlock[] getMasterBlocks() {
      updateMasterBlocks();
      return myMasterBlocks;
   }

   protected void updateMasterBlocks() {
      if (!myMasterBlocksValid) {
         if (myMasterBlocks == null) {
            myMasterBlocks = allocateMasterBlocks();
            // Now determine master block indices for FemConnections.
            // First determine how large an array we need:
            int numIdxs = 0;
            for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
               numIdxs += c.getNodes().length;
            }
            myFemMasterIdxs = new int[numIdxs];
            // then populate this array for each FemConnection
            HashMap<FemNode3d,Integer> nodeMasterIdxs = new HashMap<>();
            int idx = numFrameConnections();
            int k = 0;
            for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
               for (FemNode3d n : c.getNodes()) {
                  Integer nodeIdx = nodeMasterIdxs.get(n);
                  if (nodeIdx == null) {
                     nodeIdx = idx++;
                     nodeMasterIdxs.put(n, nodeIdx);
                  }
                  myFemMasterIdxs[k++] = nodeIdx;
               }
            }
         }
         // update allocated master blocks
         int idx = updateFrameMasterBlocks (getFrameBlending());
         // zero remaining blocks so FemConnections can accumulate into them
         while (idx < myMasterBlocks.length) {
            myMasterBlocks[idx++].setZero();
         }
         int bi = 0;
         for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
            bi = c.updateMasterBlocks (myMasterBlocks, myFemMasterIdxs, bi);
         }
         myMasterBlocksValid = true;
      }
   }

   // protected void updateMasterBlocks() {
   //    if (myMasterBlocks != null) {
   //       int idx = updateFrameMasterBlocks (getFrameBlending());
   //       // zero remaining blocks so FemConnections can accumulate into them
   //       while (idx < myMasterBlocks.length) {
   //          myMasterBlocks[idx++].setZero();
   //       }
   //       int bi = 0;
   //       for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
   //          bi = c.updateMasterBlocks (myMasterBlocks, myFemMasterIdxs, bi);
   //       }
   //       myMasterBlocksValid = true;
   //    }
   // }
   
   /**
    * Creates an empty PointSkinAttachment.
    */
   public PointSkinAttachment () {
      myBasePos = new Point3d();
   }

   public PointSkinAttachment (Point slave) {
      this();
      myPoint = slave;
      setBasePosition (slave.getPosition());
   }
   
   void setSkinMesh (SkinMeshBody skinMesh) {
      mySkinMesh = skinMesh;
   }

   private void mul (Wrench wr, Matrix6x3 M, Vector3d f) {
      wr.f.x = M.m00*f.x + M.m01*f.y + M.m02*f.z;
      wr.f.y = M.m10*f.x + M.m11*f.y + M.m12*f.z;
      wr.f.z = M.m20*f.x + M.m21*f.y + M.m22*f.z;

      wr.m.x = M.m30*f.x + M.m31*f.y + M.m32*f.z;
      wr.m.y = M.m40*f.x + M.m41*f.y + M.m42*f.z;
      wr.m.z = M.m50*f.x + M.m51*f.y + M.m52*f.z;
   }

   private void mulTransposeAdd (Vector3d v, Matrix6x3 M, Twist tw) {
      double vx = tw.v.x;
      double vy = tw.v.y;
      double vz = tw.v.z;
      double wx = tw.w.x;
      double wy = tw.w.y;
      double wz = tw.w.z;

      v.x += M.m00*vx + M.m10*vy + M.m20*vz + M.m30*wx + M.m40*wy + M.m50*wz;
      v.y += M.m01*vx + M.m11*vy + M.m21*vz + M.m31*wx + M.m41*wy + M.m51*wz;
      v.z += M.m02*vx + M.m12*vy + M.m22*vz + M.m32*wx + M.m42*wy + M.m52*wz;
   }

   /**
    * Set M to
    * <pre>
    *   [   w I   ]
    *   [         ]
    *   [ [ w v ] ]
    * </pre>
    */
   private void setEyeCross (Matrix6x3 M, double w, Vector3d v) {

      M.m00 = w; M.m01 = 0; M.m02 = 0;
      M.m10 = 0; M.m11 = w; M.m12 = 0;
      M.m20 = 0; M.m21 = 0; M.m22 = w;

      M.m30 = 0.0;
      M.m31 = -w*v.z;
      M.m32 = w*v.y;

      M.m40 = w*v.z;
      M.m41 = 0.0;
      M.m42 = -w*v.x;

      M.m50 = -w*v.y;
      M.m51 = w*v.x;
      M.m52 = 0.0;
   }

   /**
    * Linearly combine the dual quaternions for the different frames, but do
    * not normalize. Returns the combined weight for all frames.
    */
   private double combineDualQuaternions (DualQuaternion dq) {
      double wtotal = 0;
      dq.setZero();
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         dq.scaledAdd (c.myWeight, c.myFrameInfo.myBlendQuaternion);
         wtotal += c.myWeight;
      }
      return wtotal;
   }

   protected void addFramePositions (Vector3d pos, FrameBlending blending) {
      switch (blending) {
         case DUAL_QUATERNION_LINEAR: {
            Point3d tmp = new Point3d();
            DualQuaternion tmpQ = new DualQuaternion();
            double wtotal = combineDualQuaternions (tmpQ);
            tmpQ.normalize();
            tmpQ.transform(tmp, myBasePos);
            pos.scaledAdd(wtotal, tmp);
            break;
         }
         case DUAL_QUATERNION_ITERATIVE: {
            Point3d tmp = new Point3d();
            SkinMeshBody skinMesh = getSkinMesh();
            int nframes = numFrameConnections();
            DualQuaternion tmpQ = new DualQuaternion();
            DualQuaternion[] dqs = new DualQuaternion[nframes];
            double[] weights = new double[nframes];
            double wtotal = 0;
            int i = 0;
            for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
               dqs[i] = c.myFrameInfo.myBlendQuaternion;
               weights[i] = c.myWeight;
               wtotal += c.myWeight;
               i++;
            }
            tmpQ.dualQuaternionIterativeBlending(
               weights, dqs, dqs.length,
               skinMesh.getDQBlendTolerance(),
               skinMesh.getDQMaxBlendSteps());
            tmpQ.transform (tmp, myBasePos);
            pos.scaledAdd (wtotal, tmp);
            break;
         }
         case LINEAR: {
            Point3d tmp = new Point3d();
            for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
               tmp.transform(c.myFrameInfo.myDeltaPose, myBasePos);
               tmp.scale(c.myWeight);
               pos.add(tmp);
            }
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "frameBlending "+blending+" not implemented");
         }
      }
   }

   /**
    * Update frame master blocks for LINEAR blending.
    */
   private int updateLinearFrameMasterBlocks () {
      int idx = 0;
      Point3d lw = new Point3d();
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         FrameInfo finfo = c.myFrameInfo;
         Matrix6x3Block blk = (Matrix6x3Block)myMasterBlocks[idx++];
         lw.inverseTransform (finfo.myBasePose, myBasePos);
         lw.transform (finfo.myFrame.getPose().R);
         setEyeCross (blk, c.myWeight, lw);
      }
      return idx;
   }

   /**
    * Update frame master blocks for DUAL_QUATERNION blending. Note: we only
    * have an implementation for DUAL_QUATERNION_LINEAR and use this for the
    * DUAL_QUATERNION_ITERATIVE as well.
    */
   private int updateDQFrameMasterBlocks () {
      DualQuaternion dq = new DualQuaternion();
      // XXX should figure out a way to use previously computed dq
      double wtotal = combineDualQuaternions (dq);
      DualScalar m = dq.normSquared();
      double m0 = m.getReal();
      double me = m.getDual();
      double sqrtM0 = Math.sqrt(m0);
      dq.normalize();

      Matrix3x4Block Jr = new Matrix3x4Block();
      Matrix3x4Block Jtt = new Matrix3x4Block();

      DualQuaternionDeriv.computeJrJtt (Jr, Jtt, dq, m0, me, myBasePos);
      
      Matrix3d DrDri = new Matrix3d();
      Matrix3d DrDti = new Matrix3d();
   
      int idx = 0;
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         FrameInfo finfo = c.myFrameInfo;
         Matrix6x3Block blk = (Matrix6x3Block)myMasterBlocks[idx++];

         DrDri.setZero();
         DrDri.mulAdd (Jr, finfo.myDQrDr);
         DrDri.mulAdd (Jtt, finfo.myDQtDr);
         DrDri.scale (wtotal*c.myWeight/sqrtM0);
         DrDri.transpose();
         blk.setSubMatrix30 (DrDri);

         DrDti.setZero();
         DrDti.mulAdd (Jtt, finfo.myDQrDr);
         DrDti.scale (wtotal*c.myWeight/sqrtM0);
         DrDti.transpose();
         blk.setSubMatrix00 (DrDti);
      }
      return idx;
   }

   public int updateFrameMasterBlocks (FrameBlending blending) {
      switch (blending) {
         case DUAL_QUATERNION_LINEAR: 
         case DUAL_QUATERNION_ITERATIVE: {
            return updateDQFrameMasterBlocks();
         }
         case LINEAR: {
            return updateLinearFrameMasterBlocks();
         }
         default: {
            throw new UnsupportedOperationException (
               "frameBlending "+blending+" not implemented");
         }     
      }
   }      

   public boolean debug = false;

   /**
    * Computes this attachment's point value from all the underlying master
    * components to which it is connected.
    */
   protected void computePosState (Vector3d pos) {
      pos.setZero();
      if (myFrameConnections != null) {
         addFramePositions (pos, getFrameBlending());
      }
      if (myFemConnections != null) {
         PolarDecomposition3d polard = new PolarDecomposition3d();
         for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
            c.addPosition (pos, polard);
         }
      }
      if (myBaseWeight != 0) {
         pos.scaledAdd (myBaseWeight, myBasePos);
      }
   }

   /**
    * Computes this attachment's point value from all the underlying master
    * components to which it is connected.
    */
   protected void computePosState (Vector3d pos, PolarDecomposition3d polard) {
      pos.setZero();
      if (myFrameConnections != null) {
         addFramePositions (pos, getFrameBlending());
      }
      if (myFemConnections != null) {
         for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
            c.addPosition (pos, polard);
         }
      }
      if (myBaseWeight != 0) {
         pos.scaledAdd (myBaseWeight, myBasePos);
      }
   }

   public void getCurrentPos (Vector3d pos) {
      computePosState (pos);
   }
   
   public void getCurrentVel (Vector3d vel, Vector3d dvel) {
      updateMasterBlocks(); // allocate master blocks if needed
      vel.setZero();
      int idx = 0;
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         idx = c.addVelocity (vel, myMasterBlocks, idx);
      }
      while (idx < myMasterBlocks.length) {
         FemNode3d node = (FemNode3d)myMasters[idx];
         ((Matrix3d)myMasterBlocks[idx++]).mulTransposeAdd (
            vel, node.getVelocity(), vel);
      }
      // for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
      //    idx = c.addVelocity (vel, myMasterBlocks, idx);
      // }
      if (dvel != null) {
         computeVelDerivative (dvel);
      }
   }

   public void notifyMastersOfPositionChange() {
      SkinMeshBody skinBody = getSkinMesh();
      if (skinBody != null) {
         skinBody.invalidateBodyPositionInfo();
      }
   }

   public void updatePosStates() {
      // make sure body states are updated since updatePosStates() gets called
      // *before* uodateSlavePos() in SkinMeshBody:
      getSkinMesh().maybeUpdateBodyPositionInfo();

      if (myPoint != null) {
         Point3d pntw = new Point3d();
         getCurrentPos(pntw);
         myPoint.setPosition (pntw);
         myMasterBlocksValid = false;
         // if (myMasterBlocks == null) {
         //    getMasterBlocks(); // allocate master blocks if needed
         // }
         // else {
         //    updateMasterBlocks();
         // }
      }
   }

   public void updateVelStates() {
      if (myPoint != null) {
         Vector3d vel = new Vector3d();
         getCurrentVel (vel, null);
         myPoint.setVelocity (vel);
      }
   }

   public void applyForces() {
      if (myPoint != null) {
         super.applyForces();
         addForce (myPoint.getForce());
      }
   }
   
   public void addForce (Vector3d f) {
      if (!f.equals (Vector3d.ZERO)) {
         updateMasterBlocks();
         int idx = 0;
         for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
            idx = c.applyForce (f, myMasterBlocks, idx);
         }
         Vector3d tmp = new Vector3d();
         while (idx < myMasterBlocks.length) {
            FemNode3d node = (FemNode3d)myMasters[idx];
            ((Matrix3d)myMasterBlocks[idx++]).mul (tmp, f);
            node.addForce (tmp);
         }
      }      
   }
   
   public void mulSubGTM(MatrixBlock D, MatrixBlock M, int idx) {
      if (myMasterBlocks != null) {
         D.mulAdd (myMasterBlocks[idx], M);
      }
   }

   public void mulSubMG(MatrixBlock D, MatrixBlock M, int idx) {
      if (myMasterBlocks != null) {
         MatrixBlock G = myMasterBlocks[idx].createTranspose();
         D.mulAdd (M, G);
      } 
   }

   public MatrixBlock getGT (int idx) {
      updateMasterBlocks();
      MatrixBlock blk = myMasterBlocks[idx].clone();
      blk.negate();
      return blk;
   }

   public void mulSubGT(
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
      if (myMasterBlocks != null) {
         myMasterBlocks[idx].mulAdd (ybuf, yoff, xbuf, xoff);
      }
   }

   public void updateAttachment() {
      SkinMeshBody skinMesh = getSkinMesh();
      if (myPoint != null && skinMesh != null) {
         if (myFeatureBased) {
            skinMesh.computeMeshAttachment (
               this, myPoint.getPosition());
         }
         else {
            skinMesh.computeAttachment (
               this, myPoint.getPosition(), /*weights=*/null);           
         }
      }
   }

   public void addMassToMasters() {
      // todo
   }
   
   protected void scanConnection(
      String id, ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      SkinConnection.Type type = null;
      try {
         type = SkinConnection.Type.valueOf (id);
      }
      catch (Exception e) {
         throw new IOException (
            "Connection type "+id+" not recognized");
      }
      SkinConnection c;
      switch (type) {
         case F: {
            c = new FrameConnection();
            addFrameConnection ((FrameConnection)c);
            break;
         }
         case D: {
            c = new FemDispConnection();
            addFemConnection ((FemDispConnection)c);
            break;
         } 
         case E: {
            c = new ElementConnection();
            addFemConnection ((ElementConnection)c);
            break;
         }
         default: {
            throw new IOException(
               "Scan not implemented for connection type " + type);
         }
      }
      c.scan (rtok, tokens);
   }

   protected void writeConnection(
      SkinConnection c, double wscale, PrintWriter pw, NumberFormat fmt,
      CompositeComponent ancestor)
      throws IOException {

      pw.print (c.getType() + " ");
      c.write (pw, wscale, fmt, ancestor);
   }

   protected void writeConnections (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      // find the total number of connections to write. Don't write
      // connections that have a non-writable master.
      double wtotal = 0;
      double wwrite = 0;
      ArrayList<SkinConnection> writableConnections =
         new ArrayList<>(numConnections());
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         // make sure frame is writable
         if (c.getFrame().isWritable()) {
            wwrite += c.getWeight();
            writableConnections.add (c);
         }
         wtotal += c.getWeight();
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         // make sure all nodes are writable
         boolean writable = true;
         for (FemNode3d node : c.getNodes()) {
            if (!node.isWritable()) {
               writable = false;
            }
         }
         if (writable) {
            wwrite += c.getWeight();
            writableConnections.add (c);
         }
         wtotal += c.getWeight();
      }
      double wscale = 1.0;
      if (writableConnections.size() < numConnections()) {
         // we are not writing all the connections, so the weights must be
         // renormalized. 
         wscale = wtotal/wwrite;
      }
      for (SkinConnection c : writableConnections) {
         writeConnection (c, wscale, pw, fmt, ancestor);
      }
   }


   protected boolean scanItem(ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName(rtok, "base")) {
         myBasePos.scan(rtok);
         return true;
      }
      else if (scanAttributeName(rtok, "baseWeight")) {
         myBaseWeight = (float)rtok.scanNumber();
         return true;
      }
      else if (scanAttributeName(rtok, "featureBased")) {
         myFeatureBased = rtok.scanBoolean();
         return true;
      }
      else if (scanAndStoreReference (rtok, "skinMesh", tokens)) {
         return true;
      }
      else if (scanAttributeName(rtok, "connections")) {
         tokens.offer(new StringToken("connections", rtok.lineno()));
         rtok.scanToken('[');
         while (rtok.nextToken() != ']') {
            if (!rtok.tokenIsWord()) {
               throw new IOException(
                  "Connection identifier expected," + rtok);
            }
            scanConnection (rtok.sval, rtok, tokens);
         }
         return true;
      }
      rtok.pushBack();
      return super.scanItem(rtok, tokens);
   }

   protected boolean postscanItem(
      Deque<ScanToken> tokens, CompositeComponent ancestor)
      throws IOException {

      if (postscanAttributeName (tokens, "skinMesh")) {
         mySkinMesh = postscanReference (tokens, SkinMeshBody.class, ancestor);
         return true;
      }
      else if (postscanAttributeName(tokens, "connections")) {
         SkinMeshBody skinMesh = getSkinMesh();
         if (skinMesh == null) {
            throw new IOException ("skinMesh not present");
         }
         List<BodyInfo> bodyInfos = skinMesh.getAllMasterBodyInfo();
         for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
            c.postscan (tokens, bodyInfos);
         }
         for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
            c.postscan (tokens, bodyInfos);
         }
         return true;
      }
      return super.postscanItem(tokens, ancestor);
   }

   public void postscan(
      Deque<ScanToken> tokens, CompositeComponent ancestor)
      throws IOException {
      super.postscan(tokens, ancestor);
   }

   public void writeItems(
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.print("base=");
      myBasePos.write(pw, fmt);
      pw.println("");
      if (myBaseWeight != 0) {
         pw.println("baseWeight=" + myBaseWeight);
      }
      if (mySkinMesh != null) {
         pw.println(
            "skinMesh="+ComponentUtils.getWritePathName (ancestor,mySkinMesh));
      }
      if (myFeatureBased) {
         pw.println("featureBased=true");
      }
      pw.println("connections=[");
      IndentingPrintWriter.addIndentation(pw, 2);
      writeConnections (pw, fmt, ancestor);
      IndentingPrintWriter.addIndentation(pw, -2);
      pw.println("]");
   }

   private boolean computeVelDerivative (Vector3d dvel) {
      boolean isNonZero = false;
      // TODO - FINISH
      return isNonZero;
   }
   
   public boolean getDerivative(double[] buf, int idx) {
      Vector3d dvel = new Vector3d();
      boolean isNonZero = computeVelDerivative (dvel);
      buf[idx  ] = dvel.x;
      buf[idx+1] = dvel.y;
      buf[idx+2] = dvel.z;
      return isNonZero;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      Point point = getPoint();
      if (point != null) {
         refs.add(point);
      }
   }

   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      ArrayList<DynamicComponent> masters = new ArrayList<>();
      collectMasters (masters);
      for (DynamicComponent dc : masters) { 
         refs.add (dc);
      }
   }

   public void scaleDistance(double s) {
      if (myBasePos != null) {
         myBasePos.scale (s);
      }
      for (SkinConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         c.scaleDistance (s);
      }
      for (SkinConnection c=myFemConnections; c!=null; c=c.getNext()) {
         c.scaleDistance (s);
      }  
   }

   public void scaleMass(double s) {
      // nothing to do
   }

   public PointSkinAttachment copy(
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      throw new UnsupportedOperationException(
         "copy not implemented for this component");
   }

   private boolean allMastersAreConnected (SkinConnection c) {
      // component to test for connection
      ModelComponent comp = this;
      if (getParent() == null) {
         // assume myPoint is a marker and this is its attachment
         comp = myPoint;
      }
      if (c instanceof FrameConnection) {
         FrameConnection fcon = (FrameConnection)c;
         return ComponentUtils.areConnected (comp, fcon.getFrame());
      }
      else if (c instanceof FemConnection) {
         FemConnection fcon = (FemConnection)c;
         for (FemNode3d node : fcon.getNodes()) {
            if (!ComponentUtils.areConnected (comp, node)) {
               return false;
            }
         }
         return true;
      }
      else {
         throw new UnsupportedOperationException (
            "allMastersAreConnected not implemented for "+c);
      }
   }

   private <C extends SkinConnection<C>> C link (ArrayList<C> connections) {
      C prev = null;
      C first = null;
      for (C c : connections) {
         if (prev != null) {
            prev.setNext (c);
         }
         else {
            first = c;
         }
         prev = c;
      }
      if (prev != null) {
         prev.setNext (null);
      }
      return first;
   }

   /**
    * Used for debugging
    */
   private void printConnectionWeights () {
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         System.out.println (""+c.getWeight()+" " + c);
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         System.out.println (""+c.getWeight()+" " + c);
      }
      System.out.println (""+getBaseWeight()+" base");
   }

   private double sumConnectionWeights () {
      double sumw = 0;
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         sumw += c.getWeight();
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         sumw += c.getWeight();
      }
      return sumw;
   }

   private double saveConnectionWeights (Deque<Object> undoInfo) {
      float[] weights = new float[numFrameConnections()+numFemConnections()+1];
      double sumw = 0;
      int k = 0;
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         weights[k++] = c.getWeight();
         sumw += c.getWeight();
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         weights[k++] = c.getWeight();
         sumw += c.getWeight();
      }
      weights[k++] = getBaseWeight();
      undoInfo.addLast (weights);
      return sumw;
   }   

   private void restoreConnectionWeights (float[] weights) {
      int k = 0;
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         c.setWeight (weights[k++]);
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         c.setWeight (weights[k++]);
      }
      setBaseWeight (weights[k++], /*normalize=*/false);
   }   

   private void scaleConnectionWeights (double s) {
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         c.scaleWeight(s);
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         c.scaleWeight(s);
      }
   }

   private <C extends SkinConnection<C>>
      void saveConnections (C first, Deque<Object> undoInfo) {
      ArrayList<C> old = new ArrayList<>();
      for (C c=first; c!=null; c=c.getNext()) {
         old.add (c.copy());
      }
      undoInfo.addLast (old);     
   }

   private <C extends SkinConnection<C>> ArrayList<C>
      findUpdatedConnections (C first) {

      if (first != null) {
         ArrayList<C> newConnections = new ArrayList<>();
         int numc = 0;
         for (C c=first; c!=null; c=c.getNext()) {
            if (allMastersAreConnected (c)) {
               newConnections.add (c);
            }
            numc++;
         }
         if (newConnections.size() != numc) {
            return newConnections;
         }
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            // saved weights
            float[] weights = (float[])obj;
            
            obj = undoInfo.removeFirst();
            if (obj != NULL_OBJ) {
               myFrameConnections = link((ArrayList<FrameConnection>)obj);
            }
            obj = undoInfo.removeFirst();
            if (obj != NULL_OBJ) {
               myFemConnections = link((ArrayList<FemConnection>)obj);
            }
            restoreConnectionWeights (weights);
            invalidateMasters();
         }
      }
      else {
         ArrayList<FrameConnection> newFrameConnections =
            findUpdatedConnections (myFrameConnections);

         ArrayList<FemConnection> newFemConnections =
            findUpdatedConnections (myFemConnections);

         if (newFrameConnections != null || newFemConnections != null) {
            // Will have to renormalize weights, so that the sum of all the
            // weights remains the same.
            double wold = saveConnectionWeights (undoInfo);
            if (newFrameConnections != null) {
               saveConnections (myFrameConnections, undoInfo);
               myFrameConnections = link(newFrameConnections);
            }
            else {
               undoInfo.addLast (NULL_OBJ);
            }
            if (newFemConnections != null) {
               saveConnections (myFemConnections, undoInfo);
               myFemConnections = link(newFemConnections);
            }
            else {
               undoInfo.addLast (NULL_OBJ);
            }
            double wnew = sumConnectionWeights();
            if (wnew == 0) {
               setBaseWeight (1, /*normalize=*/false);
            }
            else {
               scaleConnectionWeights (wold/wnew);
            }
            invalidateMasters();
         }
         else {
            // nothing was changed
            undoInfo.addLast (NULL_OBJ);
         }
      }
   }

   private int getSize (Object obj) {
      return (int)ObjectSizeAgent.getObjectSize(obj);
   }

   @Override
   public void connectToHierarchy (CompositeComponent hcomp) {
      if (myPoint != null) {
         // super.connectToHierarchy attaches the slave point and adds back
         // references from the masters to the attachment. Only need to do this
         // if there *is* a slave point - ignore for vertex attachments.
         super.connectToHierarchy (hcomp);
      }
   }

   @Override
   public void disconnectFromHierarchy (CompositeComponent hcomp) {
      if (myPoint != null) {
         // super.disconnectFromHierarchy detaches the slave point and removes
         // back references from the masters to the attachment. Only need to do
         // this if there *is* a slave point - ignore for vertex attachments.
         super.disconnectFromHierarchy (hcomp);
      }
   }

   /* --- TransformableGeometry implementation --- */

   /**
    * {@inheritDoc}
    */
   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   /**
    * {@inheritDoc}
    */
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // if we are simulating, make no changes
      if ((flags & TransformableGeometry.TG_SIMULATING) != 0) {
         return;
      } 
      // update d0 vectors in ElementConnections
      RotationMatrix3d R = new RotationMatrix3d();
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         if (c instanceof ElementConnection) {
            ElementConnection econ = (ElementConnection)c;
            if (gtr.isRestoring()) {
               econ.myD0.set (gtr.restoreObject (econ.myD0));
            }
            else {
               if (gtr.isSaving()) {
                  gtr.saveObject (new Vector3d (econ.myD0));
               }
               // compute element position to use as a reference/
               Vector3d ref = new Vector3d();
               econ.addElementPos (ref);
               computeElementRotation (
                  R, econ.myElem, new PolarDecomposition3d());
               Vector3d d = new Vector3d();
               d.transform (R, econ.myD0);
               gtr.computeTransformVec (d, d, ref);
               econ.myD0.inverseTransform (R, d);
            }
         }
      }
      // update base position
      gtr.transformPnt (myBasePos);
   }

   /**
    * {@inheritDoc}
    */
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // nothing to add
   }

   /* --- begin ContactMaster implementation --- */

   public void add1DConstraintBlocks (
      SparseBlockMatrix GT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir) {
         
      int nframes = 0;
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         Frame frame = c.getFrame();
         frame.add1DConstraintBlocks (GT, bj, scale*c.myWeight, cpnt, dir);
         nframes++;
      }
      if (useFemMasterBlocksForContact && nframes<numMasters()) {
         updateMasterBlocks();
         Vector3d u = new Vector3d();
         for (int idx=nframes; idx<myMasterBlocks.length; idx++) {
            int bi = myMasters[idx].getSolveIndex();
            if (bi != -1) {
               Matrix3d mblk = (Matrix3d)myMasterBlocks[idx];
               Matrix3x1Block blk = new Matrix3x1Block();
               mblk.mul (u, dir);
               blk.scale (scale, u);
               GT.addBlock (bi, bj, blk);
            }
         }
      }
      else {
         for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
            c.add1DConstraintBlocks (GT, bj, scale, cpnt, dir);
         }
      }
   }

   public void add2DConstraintBlocks (
      SparseBlockMatrix GT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir0, Vector3d dir1) {

      int nframes = 0;
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         Frame frame = c.getFrame();
         frame.add2DConstraintBlocks (GT, bj, scale*c.myWeight, cpnt, dir0, dir1);
         nframes++;
      }
      if (useFemMasterBlocksForContact && nframes<numMasters()) {
         updateMasterBlocks();
         Vector3d u = new Vector3d();
         for (int idx=nframes; idx<myMasterBlocks.length; idx++) {
            int bi = myMasters[idx].getSolveIndex();
            if (bi != -1) {
               Matrix3d mblk = (Matrix3d)myMasterBlocks[idx];
               Matrix3x2Block blk = new Matrix3x2Block();
               mblk.mul (u, dir0);
               blk.m00 = scale*u.x; blk.m10 = scale*u.y; blk.m20 = scale*u.z;
               mblk.mul (u, dir1);
               blk.m01 = scale*u.x; blk.m11 = scale*u.y; blk.m21 = scale*u.z;
               GT.addBlock (bi, bj, blk);
            }
            FemNode3d node = (FemNode3d)myMasters[idx];
         }
      }
      else {
         for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
            c.add2DConstraintBlocks (GT, bj, scale, cpnt, dir0, dir1);
         }
      }
   }
   
   public void addRelativeVelocity (
      Vector3d vel, double scale, ContactPoint cpnt) {

      int nframes = 0;
      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         Frame frame = c.getFrame();
         frame.addRelativeVelocity (vel, scale*c.myWeight, cpnt);
         nframes++;
      }
      if (useFemMasterBlocksForContact && nframes<numMasters()) {
         updateMasterBlocks();
         Vector3d u = new Vector3d();
         for (int idx=nframes; idx<myMasterBlocks.length; idx++) {
            FemNode3d node = (FemNode3d)myMasters[idx];
            ((Matrix3d)myMasterBlocks[idx]).mulTranspose (u, node.getVelocity());
            vel.scaledAdd (scale, u);
         }
      }
      else {
         for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
            c.addRelativeVelocity (vel, scale, cpnt);
         }
      }
   }

   public boolean isControllable() {

      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         if (c.getFrame().isControllable()) {
            return true;
         }
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         if (c.isControllable()) {
            return true;
         }
      }
      return false;
   }
   
   public int collectMasterComponents (
      HashSet<DynamicComponent> masters, boolean activeOnly) {
      int num = 0;

      for (FrameConnection c=myFrameConnections; c!=null; c=c.getNext()) {
         num += c.getFrame().collectMasterComponents (masters, activeOnly);
      }
      for (FemConnection c=myFemConnections; c!=null; c=c.getNext()) {
         num += c.collectMasterComponents (masters, activeOnly);
      }
      return num;
   }

   /* --- end ContactMaster implementation --- */

   private void printSizes() {
      int fcsize =
         getSize(new FrameConnection());
         
      int fdsize =
         getSize(new FemDispConnection()) +
         getSize(new double[3]) + 
         getSize(new Object[3]);

      int pasize =
         getSize(new PointSkinAttachment()) +
         getSize(new Point3d()) +
         3*fcsize + 3*fdsize;

      System.out.println (
         "double[3]: " + getSize(new double[3]));
      System.out.println (
         "FemNode3d[3]: " + getSize(new FemNode3d[3]));
      System.out.println (
         "FrameConnection: " + getSize(new FrameConnection()));
      System.out.println (
         "FemDispConnection: "+getSize(new FemDispConnection()));
      System.out.println (
         "FemDispConnection + 3 nodes: " + fdsize);
      System.out.println (
         "PointSkinAttachment: " + getSize(new PointSkinAttachment()));
      System.out.println (
         "PointSkinAttachment + 3 frames + 3 fems: " + pasize);
   }

   public static void main (String[] args) {
      PointSkinAttachment pa = new PointSkinAttachment();
      
      pa.printSizes();

      //RandomGenerator.setSeed (0x1234);
   }
}

/*

Memory usage:

FrameConnection (with three frames)

overhead       16
weight          4
frameInfo       4
next            4
----------------------
               28

FemDispConnection (with three nodes)

overhead       16
femInfo         4
weight          4
coords          4 + 32
nodes           4 + 32
next            4
----------------------
              100

ElementConnection (for hex, with eight nodes)

overhead       16
femInfo         4
elem            4
weight          4
coords          4 + 52 = 56
d0              4 + 40 = 44
next            4
R               4
P               4
----------------------
              148

PointSkinAttachment (with three frames and three FEMs)

overhead       16
super class    28
skinMesh        4
masters         4 + 16+27*4 = 128
masterBlocks    4
basePos         4 + 40
skinMesh        4
frameConnections 4 + 3*32
femConnections   4 + 3*148
----------------------
              776 vs 644 for typical mesh vertex


 */
