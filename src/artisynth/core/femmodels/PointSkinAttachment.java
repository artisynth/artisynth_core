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
import java.util.Deque;
import java.util.List;
import java.util.Map;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.DualQuaternion;
import maspack.matrix.Matrix1x6Block;
import maspack.matrix.Matrix3x6Block;
import maspack.matrix.Matrix6dBlock;
import maspack.matrix.Matrix6x1Block;
import maspack.matrix.Matrix6x3Block;
import maspack.matrix.MatrixBlock;
import maspack.matrix.MatrixNdBlock;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.spatialmotion.Wrench;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.femmodels.SkinMesh.FrameBlending;
import artisynth.core.femmodels.SkinMesh.FrameInfo;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.util.TransformableGeometry;

/**
 * A special attachment class that is used to update a point's position based on
 * the state of the controlling bodies (such as Frames and FemModels) in a
 * SkinMesh. Instances of this class (without an actual slave Point) are used to
 * control the positions of each vertex in a SkinMesh.
 * 
 * <p>
 * The class maintains a list of <code>Connection</code> objects for each
 * underlying dynamic component (such as a <code>Frame</code> or
 * <code>FemNode3d</code>) that has a weighted influence on the point's final
 * value.
 */
public class PointSkinAttachment extends PointAttachment
   implements ScalableUnits {

   protected Connection[] myConnections = new Connection[0];
   protected int myNumConnections;
   protected SkinMesh mySkinMesh = null;
   protected DynamicComponent[] myMasters = null;
   protected Point3d myBasePos;

   public void setPoint (Point pnt) {
      myPoint = pnt;
   }

   /**
    * Returns the SkinMesh associated with this attachment.
    */
   public SkinMesh getSkinMesh() {
      return mySkinMesh;
   }

   /**
    * Sets the SkinMesh associated with this attachment. Will be called by
    * objects utilizing this attachment.
    */
   public void setSkinMesh(SkinMesh skinMesh) {
      mySkinMesh = skinMesh;
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
    * Returns the number of connections (to master components) used by this
    * attachment.
    */
   public int numConnections() {
      return myNumConnections;
   }

   /**
    * Clear the connections used by this attachment.
    */
   public void clearConnections() {
      myConnections = new Connection[0];
      myNumConnections = 0;
      myMasters = null;
   }

   /**
    * Gets a specific connection used by this attachent.
    * 
    * @param idx
    * index of the connection. Must be in the range 0 to
    * {@link #numConnections()}.
    */
   public Connection getConnection(int idx) {
      return myConnections[idx];
   }

   private void reallocateConnections(int num) {
      Connection[] newcons = new Connection[num];
      for (int i = 0; i < myNumConnections; i++) {
         newcons[i] = myConnections[i];
      }
      myConnections = newcons;
   }

   /**
    * Returns the weight for a connection used by this attachment.
    * 
    * @param idx
    * index of the connection. Must be in the range 0 to
    * {@link #numConnections()}.
    */
   public double getWeight(int idx) {
      return myConnections[idx].myWeight;
   }

   /**
    * Sets the weight for a connection used by this attachment.
    * 
    * @param idx
    * index of the connection. Must be in the range 0 to
    * {@link #numConnections()}.
    */
   public void setWeight(int idx, double w) {
      myConnections[idx].myWeight = w;
   }

   /**
    * Returns the underlying master component for a connection used by this
    * attachment. If the connection has no master component, then
    * <code>null</code> is returned.
    * 
    * @param idx
    * index of the connection. Must be in the range 0 to
    * {@link #numConnections()}.
    */
   public DynamicComponent getMaster(int idx) {
      return myConnections[idx].getMaster();
   }

   /**
    * Adds a Frame connection to this attachment. The Frame's displacement from
    * it's rest pose will be used to compute a weighted contribution to the
    * point value, using a mechanism determined by the blend type associated
    * with the SkinMesh (as returned by {@link SkinMesh#getFrameBlending()}).
    * 
    * @param frameInfo
    * FrameInfo structure within the associated SkinMesh.
    * @param weight
    * connection weight.
    */
   public void addFrameConnection(FrameInfo frameInfo, double weight) {
      addConnection(new FrameConnection(frameInfo, weight));
   }

   /**
    * Adds a displacment-based FemNode connection to this attachment. The node's
    * displacement from it's rest position will be used to compute a weighted
    * contribution to the point value.
    * 
    * @param node
    * FemNode
    * @param weight
    * connection weight.
    */
   public void addFemDisplacementConnection(FemNode3d node, double weight) {
      addConnection(new FemDisplacementConnection(node, weight));
   }

   /**
    * Adds a base point connection to this attachment. The point's base position
    * (as returned by {@link #getBasePosition()} will be used to compute a
    * weighted contribution to the point value.
    * 
    * @param weight
    * connection weight.
    */
   public void addBaseConnection(double weight) {
      addConnection(new BaseConnection(weight));
   }

   /**
    * Adds an arbitrary particle connection to this attachment. The particle's
    * current position will be used to compute a weighted contribution to the
    * point value.
    * 
    * @param particle
    * particle associated with the attachment.
    * @param weight
    * connection weight.
    */
   public void addParticleConnection(Particle particle, double weight) {
      addConnection(new ParticleConnection(particle, weight));
   }

   Point3d tmp = new Point3d();

//   public void addScaledExternalForce(Point3d pnt, double s, Vector3d f) {
//
//      for (int i = 0; i<myNumConnections; i++) {
//         myConnections[i].distributeExternalForce(pnt, s, f, tmp);
//      }
//
//   }

   /**
    * Minimizes the connection storage space. Should be called after all
    * connections have been added.
    */
   public void trimToSize() {
      if (myNumConnections < myConnections.length) {
         reallocateConnections(myNumConnections);
      }
   }

   /**
    * Should be called after all connections have been added. 
    */
   public void finalizeConnections() {
      trimToSize();
   }

   protected void ensureCapacity(int n) {
      if (n > myConnections.length) {
         reallocateConnections(Math.max(4, 3 * myConnections.length / 2));
      }
   }

   protected void addConnection(Connection connection) {
      int numc = myNumConnections;
      ensureCapacity(numc + 1);
      myConnections[numc] = connection;
      myNumConnections++;
   }

   /**
    * Base class for connections associated with this attachment.
    */
   public abstract class Connection {

      public double myWeight;

      /**
       * Returns the weight of this connection.
       */
      public double getWeight() {
         return myWeight;
      }

      /**
       * Sets the weight of this connection.
       */
      public void setWeight(double w) {
         myWeight = w;
      }

      /**
       * Returns the master component associated with this connection.
       */
      public abstract DynamicComponent getMaster();

      /**
       * Adds this connection's point value contribution to <code>pos</code>.
       * Not all connections implement this method; sometimes the calculation is
       * more complex. If this method does nothing, then it should return
       * <code>false</code>.
       * 
       * @param pos
       * accumulates point value contribution
       * @param tmp
       * temporary vector for optional use
       * @return true if this method is active
       */
      public abstract boolean accumulate (Vector3d pos, Vector3d tmp);

      public abstract Connection copy();
      
      public abstract boolean addPointForce (Vector3d f);
   }

   public class FrameConnection extends Connection {

      FrameInfo myFrameInfo;

      public FrameConnection (FrameInfo frameInfo, double w) {
         myFrameInfo = frameInfo;
         myWeight = w;
      }

      public int getFrameIndex() {
         return myFrameInfo.myIndex;
      }

      public DynamicComponent getMaster() {
         return myFrameInfo.myFrame;
      }

      public boolean accumulate(Vector3d pos, Vector3d tmp) {
         if (mySkinMesh.getFrameBlending() == FrameBlending.LINEAR) {
            tmp.transform(myFrameInfo.myDeltaPose, myBasePos);
            tmp.scale(myWeight);
            pos.add(tmp);
            return true;
         }
         else {
            return false;
         }
      }

      public FrameConnection copy() {
         return new FrameConnection(myFrameInfo, myWeight);
      }

      private Wrench bodyForce = new Wrench();

      @Override
      public boolean addPointForce (Vector3d f) {
         // NOTE: this method only assumes LINEAR BLENDING 
         Point3d loc = new Point3d();
         Vector3d ftmp = new Vector3d();
         // get point location in frame coordinates
         loc.inverseTransform (myFrameInfo.myBasePose, myBasePos);
         // then use this to apply forces
         ftmp.scale (myWeight, f);
         myFrameInfo.myFrame.addPointForce (loc, ftmp);
         return true;
      }

//      @Override
//      public boolean addExternalPointForce (Vector3d f) {
//         // NOTE: this method only assumes LINEAR BLENDING 
//         Point3d loc = new Point3d();
//         Vector3d ftmp = new Vector3d();
//         // get point location in frame coordinates
//         loc.inverseTransform (myFrameInfo.myBasePose, myBasePos);
//         // then use this to apply forces
//         ftmp.scale (myWeight, f);
//         myFrameInfo.myFrame.addExternalPointForce (loc, ftmp);
//         return true;
//      }

//      @Override
//      public boolean zeroExternalForces () {
//         myFrameInfo.myFrame.zeroExternalForces();
//         return true;
//      }

//      @Override
//      public boolean distributeExternalForce(Vector3d pos, double s,
//         Vector3d force, Vector3d tmp) {
//         // if (mySkinMesh.getFrameBlending() == FrameBlending.LINEAR) {
//         FrameInfo finfo = myFrameInfo;
//         Frame frame = finfo.myFrame;
//         tmp.inverseTransform(frame.getPose(), pos);
//         frame.computeAppliedWrench(bodyForce, force, tmp);
//         bodyForce.transform(frame.getPose().R);
//         frame.addScaledExternalForce(s * myWeight, bodyForce);
//         return true;
//         // }
//         // else {
//         // return false;
//         // }
//      }

   }

   public class ParticleConnection extends Connection {

      public Particle myParticle;

      public ParticleConnection (Particle particle, double w) {
         myParticle = particle;
         myWeight = w;
      }

      public DynamicComponent getMaster() {
         return myParticle;
      }

      @Override
      public boolean accumulate(Vector3d pos, Vector3d tmp) {
         pos.scaledAdd(myWeight, myParticle.getPosition());
         return true;
      }

//      @Override
//      public boolean distributeExternalForce(Vector3d pos, double s,
//         Vector3d force,
//         Vector3d tmp) {
//         myParticle.addScaledExternalForce(s * myWeight, force);
//         return true;
//      }

      @Override
      public boolean addPointForce (Vector3d f) {
         myParticle.addScaledForce (myWeight, f);
         return true;
      }

//      @Override
//      public boolean addExternalPointForce (Vector3d f) {
//         myParticle.addScaledExternalForce (myWeight, f);
//         return true;
//      }

//      @Override
//      public boolean zeroExternalForces () {
//         myParticle.zeroExternalForces();
//         return true;
//      }

      public ParticleConnection copy() {
         return new ParticleConnection(myParticle, myWeight);
      }

   }

   public class BaseConnection extends Connection {

      public BaseConnection (double w) {
         myWeight = w;
      }

      public DynamicComponent getMaster() {
         return null;
      }

      public boolean accumulate(Vector3d pos, Vector3d tmp) {
         pos.scaledAdd(myWeight, myBasePos);
         return true;
      }

//      @Override
//      public boolean distributeExternalForce(Vector3d pos, double s,
//         Vector3d force,
//         Vector3d tmp) {
//         return false;
//      }

      @Override
      public boolean addPointForce (Vector3d f) {
         return false;
      }

//      @Override
//      public boolean addExternalPointForce (Vector3d f) {
//         return false;
//      }

//      @Override
//      public boolean zeroExternalForces () {
//         return false;
//      }

      public BaseConnection copy() {
         return new BaseConnection(myWeight);
      }

   }

   public class FemDisplacementConnection extends Connection {

      public FemNode3d myNode;

      public FemDisplacementConnection (FemNode3d node, double w) {
         myNode = node;
         myWeight = w;
      }

      public DynamicComponent getMaster() {
         return myNode;
      }

      public boolean accumulate(Vector3d pos, Vector3d tmp) {
         pos.scaledAdd(myWeight, myNode.getPosition());
         pos.scaledAdd(-myWeight, myNode.getRestPosition());
         return true;
      }

//      @Override
//      public boolean distributeExternalForce(Vector3d pos, double s,
//         Vector3d force,
//         Vector3d tmp) {
//         myNode.addScaledExternalForce(s * myWeight, force);
//         return true;
//      }

      @Override
      public boolean addPointForce (Vector3d f) {
         myNode.addScaledForce (myWeight, f);
         return true;
      }

//      @Override
//      public boolean addExternalPointForce (Vector3d f) {
//         myNode.addScaledExternalForce (myWeight, f);
//         return true;
//      }

//      @Override
//      public boolean zeroExternalForces () {
//         myNode.zeroExternalForces();
//         return true;
//      }

      public FemDisplacementConnection copy() {
         return new FemDisplacementConnection(myNode, myWeight);
      }
   }

   protected void doInitializeMasters() {
      // Note sure if we should require a skinMesh here or not ...
      // if (mySkinMesh == null) {
      // throw new IllegalStateException (
      // "SkinMesh is not set within this attacher");
      // }
      ArrayList<DynamicComponent> masters = new ArrayList<DynamicComponent>();
      for (int i = 0; i < myNumConnections; i++) {
         DynamicComponent m = myConnections[i].getMaster();
         if (m != null) {
            masters.add(m);
         }
      }
      myMasters = masters.toArray(new DynamicComponent[0]);
   }

//   /**
//    * {@inheritDoc}
//    */
//   public DynamicComponent[] getMasters() {
//      if (myMasters == null) {
//         doInitializeMasters();
//      }
//      return myMasters;
//   }

   protected void collectMasters (List<DynamicComponent> masters) {
      super.collectMasters (masters);
      for (int i = 0; i < myNumConnections; i++) {
         DynamicComponent m = myConnections[i].getMaster();
         if (m != null) {
            masters.add(m);
         }
      }
   }
   
   @Override
   protected int updateMasterBlocks() {
      // TODO: need to finish this to make attachments work correctly
      int idx = super.updateMasterBlocks();
      return idx;
   }
   
//   /**
//    * {@inheritDoc}
//    */
//   public int numMasters() {
//      if (myMasters == null) {
//         doInitializeMasters();
//      }
//      return myMasters.length;
//   }

   /**
    * Creates an empty PointSkinAttachment. The associated skin mesh will have
    * to be set later.
    */
   public PointSkinAttachment () {
      myBasePos = new Point3d();
   }

   /**
    * Creates an empty PointSkinAttachment to associated with a specific
    * skinMesh.
    */
   public PointSkinAttachment (SkinMesh skinMesh) {
      this();
      setSkinMesh(skinMesh);
   }

   /**
    * Computes this attachment's point value from all the underlying master
    * components to which it is connected.
    */
   protected void computePosState(Vector3d pos, SkinMesh skinMesh) {

      DualQuaternion tmpQ = null;
      DualQuaternion[] dualqs = null;
      double[] weights = null;
      pos.setZero();
      Point3d tmp = new Point3d();
      int fidx = 0; // frame counter for interative dualQuaternion blending
      double dualw = 0; // weight for just the dual quaternion part
      for (int k = 0; k < myNumConnections; k++) {
         Connection c = myConnections[k];
         if (!c.accumulate(pos, tmp)) {
            // Go case-by-case when accumulate method is inactive
            if (c instanceof FrameConnection) {
               FrameInfo finfo = ((FrameConnection)c).myFrameInfo;
               switch (skinMesh.getFrameBlending()) {
                  case DUAL_QUATERNION_LINEAR: {
                     if (tmpQ == null) {
                        tmpQ = new DualQuaternion();
                        tmpQ.scale(c.myWeight, finfo.myBlendQuaternion);
                     }
                     else {
                        tmpQ.scaledAdd(c.myWeight, finfo.myBlendQuaternion);
                     }
                     dualw += c.myWeight;
                     break;
                  }
                  case DUAL_QUATERNION_ITERATIVE: {
                     if (weights == null) {
                        int numf = skinMesh.numFrames();
                        weights = new double[numf];
                        dualqs = new DualQuaternion[numf];
                     }
                     dualqs[fidx] = finfo.myBlendQuaternion;
                     weights[fidx] = c.myWeight;
                     dualw += c.myWeight;
                     fidx++;
                     break;
                  }
                  default:
                     break;
               }
            }
         }
      }
      if (tmpQ != null) {
         tmpQ.normalize();
         tmpQ.transform(tmp, myBasePos);
         pos.scaledAdd(dualw, tmp);
      }
      else if (skinMesh.getFrameBlending() == FrameBlending.DUAL_QUATERNION_ITERATIVE && fidx > 0) {
         tmpQ = new DualQuaternion();
         tmpQ.dualQuaternionIterativeBlending(
            weights, dualqs, fidx,
            SkinMesh.DQ_BLEND_TOLERANCE, SkinMesh.DQ_MAX_BLEND_STEPS);
         tmpQ.transform(tmp, myBasePos);
         pos.scaledAdd(dualw, tmp);
      }
   }

   public void getCurrentPos(Vector3d pos) {
      computePosState(pos, mySkinMesh);
   }
   
   public void getCurrentVel (Vector3d vel, Vector3d dvel) {
      vel.setZero();
      // TODO FINISH this, as well as computeVelDerivative()
      if (dvel != null) {
         computeVelDerivative (dvel);
      }
   }

   public void updatePosStates() {
      if (myPoint != null) {
         Point3d pntw = new Point3d();
         getCurrentPos(pntw);
         myPoint.setPosition (pntw);
         updateMasterBlocks();
      }
   }

   public void updateVelStates() {
      if (myPoint != null) {
         // TODO: finish implementation
      }
   }

   public void applyForces() {
      if (myPoint != null) {
         super.applyForces();
         Vector3d f = myPoint.getForce();
         if (!f.equals (Vector3d.ZERO)) {
            for (int i = 0; i<myNumConnections; i++) {
               myConnections[i].addPointForce (f);
            }
         }
      }
   }

//   protected MatrixBlock createRowBlock(int colSize) {
//      return createRowBlockNew(colSize);
//   }
//
//   protected MatrixBlock createColBlock(int rowSize) {
//      return createColBlockNew(rowSize);
//   }
//
//   protected MatrixBlock createColBlockNew(int rowSize) {
//      switch (rowSize) {
//         case 1:
//            return new Matrix1x6Block();
//         case 3:
//            return new Matrix3x6Block();
//         case 6:
//            return new Matrix6dBlock();
//         default:
//            return new MatrixNdBlock(rowSize, 6);
//      }
//   }
//
//   protected MatrixBlock createRowBlockNew(int colSize) {
//      switch (colSize) {
//         case 1:
//            return new Matrix6x1Block();
//         case 3:
//            return new Matrix6x3Block();
//         case 6:
//            return new Matrix6dBlock();
//         default:
//            return new MatrixNdBlock(6, colSize);
//      }
//   }

   public void mulSubGT(MatrixBlock D, MatrixBlock B, int idx) {
   }

   public void mulSubG(MatrixBlock D, MatrixBlock B, int idx) {
   }

   public void mulSubGT(
      double[] ybuf, int yoff, double[] xbuf, int xoff, int idx) {
   }

   public void updateAttachment() {
   }

   @Override
   public void transformSlaveGeometry(
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
   }

   public void addMassToMasters() {
   }
   
   public void addMassToMaster(MatrixBlock mblk, MatrixBlock sblk, int idx) {
   }

   protected boolean scanConnection(
      String id, ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      if (id.equals("F")) {
         if (!ScanWriteUtils.scanAndStoreReference(rtok, tokens)) {
            throw new IOException("Expected component reference, got " + rtok);
         }
         double w = rtok.scanNumber();
         addFrameConnection(null, w);
         return true;
      }
      else if (id.equals("P")) {
         if (!ScanWriteUtils.scanAndStoreReference(rtok, tokens)) {
            throw new IOException("Expected component reference, got " + rtok);
         }
         double w = rtok.scanNumber();
         // particle will be filled in by postscan ...
         addParticleConnection(null, w);
         return true;
      }
      else if (id.equals("D")) {
         if (!ScanWriteUtils.scanAndStoreReference(rtok, tokens)) {
            throw new IOException("Expected component reference, got " + rtok);
         }
         double w = rtok.scanNumber();
         // femNode will be filled in by postscan ...
         addFemDisplacementConnection(null, w);
         return true;
      }
      else if (id.equals("B")) {
         double w = rtok.scanNumber();
         addBaseConnection(w);
         return true;
      }
      else {
         return false;
      }
   }

   protected boolean writeConnection(
      Connection c, PrintWriter pw, NumberFormat fmt,
      CompositeComponent ancestor)
      throws IOException {

      if (c instanceof FrameConnection) {
         String framePath =
            ComponentUtils.getWritePathName(
               ancestor, c.getMaster());
         pw.println("F " + framePath + " " + fmt.format(c.getWeight()));
         return true;
      }
      else if (c instanceof ParticleConnection) {
         String particlePath =
            ComponentUtils.getWritePathName(
               ancestor, c.getMaster());
         pw.print("P " + particlePath + " " + fmt.format(c.getWeight()));
         return true;
      }
      else if (c instanceof FemDisplacementConnection) {
         String nodePath =
            ComponentUtils.getWritePathName(
               ancestor, c.getMaster());
         pw.print("D " + nodePath + " " + fmt.format(c.getWeight()));
         return true;
      }
      else if (c instanceof BaseConnection) {
         pw.println("B " + fmt.format(c.getWeight()));
         return true;
      }
      else {
         return false;
      }
   }

   protected boolean scanItem(ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName(rtok, "base")) {
         myBasePos.scan(rtok);
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
            if (!scanConnection(rtok.sval, rtok, tokens)) {
               throw new IOException(
                  "Connection identifier " + rtok.sval +
                     " not recognized," + rtok);
            }
         }
         return true;
      }
      rtok.pushBack();
      return super.scanItem(rtok, tokens);
   }

   protected boolean postscanItem(
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName(tokens, "connections")) {
         for (int i=0; i<myNumConnections; i++) {
            Connection c = myConnections[i];
            if (c instanceof FrameConnection) {
               Frame frame = postscanReference(tokens, Frame.class, ancestor);
               ((FrameConnection)c).myFrameInfo =
                  mySkinMesh.getFrameInfo(frame);
            }
            else if (c instanceof ParticleConnection) {
               ((ParticleConnection)c).myParticle =
                  postscanReference(tokens, Particle.class, ancestor);
            }
            else if (c instanceof FemDisplacementConnection) {
               ((FemDisplacementConnection)c).myNode =
                  postscanReference(tokens, FemNode3d.class, ancestor);
            }
         }
         return true;
      }
      return super.postscanItem(tokens, ancestor);
   }

   public void postscan(
      Deque<ScanToken> tokens, CompositeComponent ancestor)
      throws IOException {
      super.postscan(tokens, ancestor);
      finalizeConnections();
   }

   public void writeItems(
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      pw.print("base=");
      myBasePos.write(pw, fmt);
      pw.println("");
      pw.println("connections=[");
      IndentingPrintWriter.addIndentation(pw, 2);
      for (int i = 0; i < myNumConnections; i++) {
         Connection c = myConnections[i];
         if (!writeConnection(c, pw, fmt, ancestor)) {
            throw new UnsupportedOperationException(
               "Write not implemented for connection type " + c.getClass());
         }
      }
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

//   @Override
//   public void connectToHierarchy() {
//      super.connectToHierarchy();
//      Point point = getPoint();
//      if (point != null) {
//         point.setAttached(this);
//      }
//      DynamicComponent masters[] = getMasters();
//      if (masters != null) {
//         for (DynamicComponent m : masters) {
//            m.addMasterAttachment(this);
//         }
//      }
//   }
//
//   @Override
//   public void disconnectFromHierarchy() {
//      super.disconnectFromHierarchy();
//      Point point = getPoint();
//      if (point != null) {
//         point.setAttached(null);
//      }
//      DynamicComponent masters[] = getMasters();
//      if (masters != null) {
//         for (DynamicComponent m : masters) {
//            m.removeMasterAttachment(this);
//         }
//      }
//   }

   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      Point point = getPoint();
      if (point != null) {
         refs.add(point);
      }
      for (int i = 0; i < myNumConnections; i++) {
         ModelComponent m = myConnections[i].getMaster();
         if (m != null) {
            refs.add(myConnections[i].getMaster());
         }
      }
   }

   public void scaleDistance(double s) {
      myBasePos.scale(s);
   }

   public void scaleMass(double s) {
      // nothing to do
   }

   public PointSkinAttachment copy(
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointSkinAttachment a = (PointSkinAttachment)super.copy(flags, copyMap);

      throw new UnsupportedOperationException(
         "copy not implemented for this component");

      // myConnections = null; // need to duplicate
      // a.myNumConnections = myNumConnections;
      // a.mySkinMesh = mySkinMesh; // probably OK
      // a.myMasters = null; // OK
      // if (myBasePos != null) { // OK
      // a.myBasePos = new Point3d(myBasePos);
      // }
      // return a;
   }

   // Scale BaseConnection weights
   public void scaleBaseWeights(double s, Connection[] connections) {
      for (int i = 0; i < connections.length; i++) {
         if (connections[i] instanceof BaseConnection) {
            connections[i].myWeight *= s;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            myConnections = (Connection[])obj;
            myNumConnections = myConnections.length;
            myMasters = null;
         }
      }
      else {
         ArrayList<Connection> updated = new ArrayList<Connection>();
         // total original weight of all base connections
         double wbaseOld = 0;
         // total weight of all removed displacement connections
         double wdispDel = 0;
         for (int i = 0; i < myNumConnections; i++) {
            Connection c = myConnections[i];
            ModelComponent m = c.getMaster();
            if (m == null || ComponentUtils.isConnected (this, m)) {
               updated.add(c);
               if (c instanceof BaseConnection) {
                  wbaseOld += c.myWeight;
               }
            }
            else {
               if (c instanceof FemDisplacementConnection) {
                  wdispDel += c.getWeight();
               }
            }
         }
         if (updated.size() < myNumConnections) {
            Connection[] old = new Connection[myNumConnections];
            for (int i = 0; i < myNumConnections; i++) {
               old[i] = myConnections[i].copy();
            }
            // updated.clear();
            // updated.add (new BaseConnection (1));
            myConnections = updated.toArray(new Connection[0]);
            myNumConnections = myConnections.length;

            // Now we have to renormalize weights. Total weights are given by
            //
            // F + B + B_D + D = 1 + D, with B_D = D
            //
            // where F, B, and D are the frame, base, displacement base, and
            // displacement weights. When displacements are removed, we do not
            // change the weights of their corresponding base displacement
            // connections. Instead, we scale *all* base connections so that
            // the new value of B + B_D equals B + B_D - D_R, where D_R is the
            // weight of all displacements that have been deleted. Then, to
            // finish, we normalize all remaining weights so that F + B + B_D =
            // 1.
            if (wbaseOld != 0) {
               scaleBaseWeights((wbaseOld - wdispDel) / wbaseOld, myConnections);
            }
            double wtotal = 0;
            for (int i=0; i<myNumConnections; i++) {
               Connection c = myConnections[i];
               if (!(c instanceof FemDisplacementConnection)) {
                  wtotal += c.myWeight;
               }
            }
            for (int i=0; i<myNumConnections; i++) {
               Connection c = myConnections[i];
               c.myWeight /= wtotal;
            }
            myMasters = null;          
            undoInfo.addLast (old);
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
      }
   }

}
