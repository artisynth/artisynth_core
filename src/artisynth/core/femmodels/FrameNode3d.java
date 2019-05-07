package artisynth.core.femmodels;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.geometry.*;
import maspack.util.*;
import maspack.render.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public class FrameNode3d extends DynamicComponentBase {
   
   protected FemNode3d myNode;

   Point3d myPos = new Point3d();
   Vector3d myVel = new Vector3d();
   Vector3d myForce = new Vector3d();
   Frame myFrame;
   double myEffectiveMass = 0;
   
   FrameNode3d (FemNode3d node, Frame frame) {
      myNode = node;
      myFrame = frame;      
      myPos.inverseTransform (myFrame.getPose(), node.getPosition());
      myVel.inverseTransform (myFrame.getPose(), node.getVelocity());
   }

   public Frame getFrame() {
      return myFrame;
   }

   public Point3d getPosition() {
      return myPos;
   }

   public void setPosition (Point3d pos) {
      myPos.set (pos);
   }

   public Vector3d getVelocity() {
      return myVel;
   }

   public void setVelocity (Vector3d vel) {
      myVel.set (vel);
   }

   public Vector3d getForce() {
      return myForce;
   }

   public void setForce (Vector3d f) {
      myForce.set (f);
   }

   public void addForce (Vector3d f) {
      myForce.add (f);
   }

   /**
    * {@inheritDoc}
    */
   public MatrixBlock createMassBlock() {
      return myNode.createMassBlock();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMassConstant() {
      return myNode.isMassConstant();
   }

   /**
    * {@inheritDoc}
    */
   public double getMass (double t) {
      return 0; // all mass is effective
   }

   /**
    * {@inheritDoc}
    */
   public void getMass (Matrix M, double t) {
      doGetMass (M, 0);
   }

   /**
    * {@inheritDoc}
    */
   public int getEffectiveMassForces (VectorNd f, double t, int idx) {
      double[] buf = f.getBuffer();
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      return idx;      
   }

   /**
    * {@inheritDoc}
    */
   public void getInverseMass (Matrix Minv, Matrix M) {
      if (!(Minv instanceof Matrix3d)) {
         throw new IllegalArgumentException ("Minv not instance of Matrix3d");
      }
      if (!(M instanceof Matrix3d)) {
         throw new IllegalArgumentException ("M not instance of Matrix3d");
      }
      double inv = 1/((Matrix3d)M).m00;
      ((Matrix3d)Minv).setDiagonal (inv, inv, inv);
   }

   /**
    * {@inheritDoc}
    */
   public void resetEffectiveMass() {
      myEffectiveMass = 0;
   }

   public void addEffectiveMass (double m) {
      myEffectiveMass += m;
   }   

   /**
    * {@inheritDoc}
    */
   public void getEffectiveMass (Matrix M, double t) {
      doGetMass (M, myEffectiveMass);
   }

   /**
    * {@inheritDoc}
    */
   public double getEffectiveMass() {
      return myEffectiveMass;
   }

   /**
    * {@inheritDoc}
    */
   public int mulInverseEffectiveMass (
      Matrix M, double[] a, double[] f, int idx) {
      double minv = 1/myEffectiveMass;
      a[idx++] = minv*f[idx];
      a[idx++] = minv*f[idx];
      a[idx++] = minv*f[idx];
      return idx;
   }

   protected void doGetMass (Matrix M, double m) {
      if (M instanceof Matrix3d) {
         ((Matrix3d)M).setDiagonal (m, m, m);
      }
      else {
         throw new IllegalArgumentException ("Matrix not instance of Matrix3d");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addSolveBlock (SparseNumberedBlockMatrix S) {
      int bi = getSolveIndex();
      Matrix3x3Block blk = new Matrix3x3Block();
      S.addBlock (bi, bi, blk);
   }

   public void addPosImpulse (
      double[] xbuf, int xidx, double h, double[] vbuf, int vidx) {

      xbuf[xidx  ] += h*vbuf[vidx  ];
      xbuf[xidx+1] += h*vbuf[vidx+1];
      xbuf[xidx+2] += h*vbuf[vidx+2];
   }

   public int getPosDerivative (double[] buf, int idx) {
      // XXX is this what we want? Or do we need Coriolis terms?
      buf[idx++] = myVel.x;
      buf[idx++] = myVel.y;
      buf[idx++] = myVel.z;
      return idx;
   }

   public int getPosState (double[] buf, int idx) {
      buf[idx++] = myPos.x;
      buf[idx++] = myPos.y;
      buf[idx++] = myPos.z;
      return idx;
   }

   public int setPosState (double[] buf, int idx) {
      myPos.x = buf[idx++];
      myPos.y = buf[idx++];
      myPos.z = buf[idx++];
      return idx;
   }

   public int getVelState (double[] buf, int idx) {
      buf[idx++] = myVel.x;
      buf[idx++] = myVel.y;
      buf[idx++] = myVel.z;
      return idx;
   }

   public int setVelState (double[] buf, int idx) {
      myVel.x = buf[idx++];
      myVel.y = buf[idx++];
      myVel.z = buf[idx++];
      return idx;
   }

   public int setForce (double[] buf, int idx) {
      myForce.x = buf[idx++];
      myForce.y = buf[idx++];
      myForce.z = buf[idx++];
      return idx;
   }

   public int addForce (double[] buf, int idx) {
      myForce.x += buf[idx++];
      myForce.y += buf[idx++];
      myForce.z += buf[idx++];
      return idx;
   }

   public int getForce (double[] buf, int idx) {
      buf[idx++] = myForce.x;
      buf[idx++] = myForce.y;
      buf[idx++] = myForce.z;
      return idx;
   }

   public int getPosStateSize() {
      return 3;
   }

   public int getVelStateSize() {
      return 3;
   }

   public void zeroForces() {
      myForce.setZero();
   }

   public void zeroExternalForces() {
   }

   public void applyExternalForces() {
   }
   
   public boolean velocityLimitExceeded (double tlimit, double rlimit) {
      return myNode.velocityLimitExceeded (tlimit, rlimit);
   }

   /**
    * Shouldn't need this because gravity is applied to world nodes
    */
   public void applyGravity (Vector3d gacc) {
      myForce.scaledAdd (getMass(0), gacc);
   }
   
   public boolean hasForce() {
      return myNode.hasForce();
   }

   public void getState (DataBuffer data) {
      data.dput (myPos);
      data.dput (myVel);
      if (MechSystemBase.mySaveForcesAsState) {
         data.dput (myForce);
      }
   }

   public void setState (DataBuffer data) {
      data.dget (myPos);
      data.dget (myVel);
      if (MechSystemBase.mySaveForcesAsState) {
         data.dget (myForce);
      }
   }

   public void setRandomPosState() {
      myPos.setRandom();
   }
   
   public void setRandomVelState() {
      myVel.setRandom();
   }
   
   public void setRandomForce() {
      myForce.setRandom();
   }
   
   // ForceEffector - these are all no-ops, since damping is applied by FEM model

   public void applyForces (double t) {
      // nothing to do
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      // nothing to do
   }

   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      // nothing to do
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s)  {
      // nothing to do
   }

   public int getJacobianType() {
      return Matrix.SPD;
   }

   // dynamic attachment 

   // Transformable geometry

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      gtr.transformPnt (myPos);
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencoes
   }

   @Override
   public void render (Renderer renderer, int flags) {
   }   
}
