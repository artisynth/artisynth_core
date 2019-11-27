package artisynth.core.femmodels;

import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;
import maspack.render.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MotionTarget.TargetActivity;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class BackNode3d extends DynamicComponentBase 
   implements MotionTargetComponent {

   FemNode3d myNode;
   Point3d myPos = new Point3d();
   Point3d myRest = new Point3d();
   Vector3d myVel = new Vector3d();
   Vector3d myForce = new Vector3d();
   Vector3d myInternalForce = new Vector3d();
   float[] myRenderCoords = new float[3];
   double myEffectiveMass = 0;
   // see comments under getMass() for how BackNode mass is defined
   double myMass = 0;

   boolean myRestValidP = false;
   boolean myRestExplicitP = false;
   boolean myPosValidP = false;

   public BackNode3d (FemNode3d node) {
      myNode = node;
      myPosValidP = false;
   }

   public Point3d getPosition() {
      if (!myPosValidP) {
         setPositionToRest();
      }
      return myPos;
   }

   public void setPosition (Point3d pos) {
      myPos.set (pos);
      myPosValidP = true;
   }
   
   public void setPosition (Point3d frontPos, Vector3d dir) {
      myPos.sub (frontPos, dir);
      myPosValidP = true;
   }
   
   public void scalePosition (double s, Point3d frontPos) {
      Vector3d dir = new Vector3d();
      dir.sub (frontPos, myPos);
      myPos.scaledAdd (-s, dir, frontPos);
   }
   
   public void clearPosition() {
      myPosValidP = false;
   }
 
   public boolean isPositionValid() {
      return myPosValidP;
   }

   public void setPositionToRest() {
      myPos.set (getRestPosition());
      myPosValidP = true;
   }

   public Vector3d getVelocity() {
      return myVel;
   }

   public void setVelocity (Vector3d vel) {
      myVel.set (vel);
   }

   public Point3d getRestPosition() {
      if (!myRestValidP) {
         Vector3d dir = new Vector3d();
         myNode.computeRestDirector (dir);
         myRest.sub (myNode.myRest, dir);
         myRestValidP = true;
      }
      return myRest;
   }

   public void setRestPosition (Point3d rest) {
      myRest.set (rest);
      myRestValidP = true;
      myRestExplicitP = true;
   }

   public void setRestPosition (Point3d frontRest, Vector3d restDir) {
      myRest.sub (frontRest, restDir);
      myRestValidP = true;
      myRestExplicitP = true;
   }
   
   public void scaleRestPosition (double s, Point3d frontRest) {
      Vector3d dir = new Vector3d();
      dir.sub (frontRest, myRest);
      myRest.scaledAdd (-s, dir, frontRest);
      // do NOT mark as explicit
   }

   public void clearRestPosition() {
      myRestExplicitP = false;
      myRestValidP = false;
   }

   public boolean isRestPositionExplicit() {
      return myRestExplicitP;
   }

   public boolean isRestPositionValid() {
      return myRestValidP;
   }

   public Point3d getRenderPosition() {
      Point3d coords =
         new Point3d (myRenderCoords[0], myRenderCoords[1], myRenderCoords[2]);
      return coords;
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

   public void addScaledForce (double s, Vector3d f) {
      myForce.scaledAdd (s, f);
   }

   public void subForce (Vector3d f) {
      myForce.sub (f);
   }

   public Vector3d getInternalForce() {
      return myInternalForce;
   }

   /**
    * {@inheritDoc}
    */
   public MatrixBlock createMassBlock() {
      return new Matrix3x3DiagBlock();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMassConstant() {
      return true;
   }

   /**
    * This method is required by the interface, but is not currently used.
    */
   public double getMass (double t) {
      return getMass();
   }

   /**
    * Returns the mass for this backnode. This is the portion of the front
    * node's mass that is used as the effective mass for the back node.
    * Correspondingly, the front node's effective mass is its own mass minus
    * the back node mass.
    * 
    * <p>If computed from density, the back node's mass is half of
    * the total nodal mass due to shell elements. Otherwise, if the
    * front node's mass is explicitly set, the back node's mass is set
    * to half the front node mass.
    * @return mass of this backnode
    */
   public double getMass() {
      myNode.getMass(); // call to ensure mass is updated if necessary
      return myMass;
   }

   /**
    * This method is required by the interface, but is not currently used.
    */
   public void getMass (Matrix M, double t) {
      doGetMass (M, getMass());
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
      myEffectiveMass = myMass;
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
      Point3d pos = getPosition();
      buf[idx++] = pos.x;
      buf[idx++] = pos.y;
      buf[idx++] = pos.z;
      return idx;
   }

   public int setPosState (double[] buf, int idx) {
      myPos.x = buf[idx++];
      myPos.y = buf[idx++];
      myPos.z = buf[idx++];
      myPosValidP = true;
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
      if (myVel.containsNaN() || myVel.infinityNorm() > tlimit) {
         return true;
      }  
      else {
         return false;
      }
   }

   /**
    * Note: this method is required by the interface, but is not currently
    * used since gravity is applied directly to nodes within 
    * FemModel3d.updateNodeForces(), and gravity action is lumped
    * together with the front node.
    */
   public void applyGravity (Vector3d gacc) {
      myForce.scaledAdd (getMass(0), gacc);
   }
   
   public boolean hasForce() {
      return false;
   }

   public void getState (DataBuffer data) {
      data.dput (getPosition());
      data.dput (myVel);
      if (MechSystemBase.mySaveForcesAsState) {
         data.dput (myForce);
      }
   }

   public void setState (DataBuffer data) {
      data.dget (myPos);
      data.dget (myVel);
      myPosValidP = true;
      if (MechSystemBase.mySaveForcesAsState) {
         data.dget (myForce);
      }
   }

   public void setRandomPosState() {
      myPos.setRandom();
      myPosValidP = true;
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

   // Transformable geometry

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      if (myPosValidP) {
         gtr.transformPnt (myPos);
      }
      if (myRestValidP && (flags & TransformableGeometry.TG_SIMULATING) == 0) {
         gtr.transformPnt (myRest);
      }
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencoes
   }

   @Override
   public void render (Renderer renderer, int flags) {
   }

   public void setDynamic (boolean enable) {
      super.setDynamic (enable);
   }

   /* --- Motion target stuff --- */

   protected TargetActivity myTargetActivity = TargetActivity.Auto;
   protected PointTarget myTarget = null;

   @Override
   public TargetActivity getTargetActivity () {
      if (myTarget == null) {
         return myTargetActivity;
      }
      else {
         return myTarget.getActivity();
      }
   }

   @Override
   public void setTargetActivity (TargetActivity activity) {
      if (activity == null) {
         throw new IllegalArgumentException ("activity cannot be null");
      }
      if (activity == TargetActivity.None) {
         myTarget = null;
      }
      else {
         TargetActivity prevActivity = TargetActivity.None;
         if (myTarget == null) {
            myTarget = new PointTarget (activity);
         }
         else {
            prevActivity = myTarget.getActivity();
            myTarget.setActivity (activity);
         }
         myTarget.syncState (prevActivity, getPosition(), myVel);
      }
      myTargetActivity = activity;
   }

   @Override
   public int getTargetVel (double[] velt, double s, double h, int idx) {
      if (myTarget == null) {
         velt[idx++] = myVel.x;
         velt[idx++] = myVel.y;
         velt[idx++] = myVel.z;
         return idx;
      }
      else {
         return myTarget.getTargetVel (velt, s, h, getPosition(), myVel, idx);
      }
   }

   @Override
   public int getTargetPos (double[] post, double s, double h, int idx) {
      Point3d pos = getPosition();
      if (myTarget == null) {
         post[idx++] = pos.x;
         post[idx++] = pos.y;
         post[idx++] = pos.z;
         return idx;
      }
      else {
         return myTarget.getTargetPos (post, s, h, pos, myVel, idx);
      }
   }

   @Override
   public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      if (!isControllable()) {
         throw new IllegalStateException (
            "Target point is not controllable");
      }
      Matrix3x3DiagBlock blk = new Matrix3x3DiagBlock();
      blk.setIdentity();
      J.addBlock (bi, getSolveIndex(), blk);
      return bi++;
   }

   @Override
   public void resetTargets () {
      // TODO Auto-generated method stub
      
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      rtok.nextToken();
      if (scanAttributeName (rtok, "position")) {
         myPos.scan (rtok);
         myPosValidP = true;
         return true;
      }
      else if (scanAttributeName (rtok, "velocity")) {
         myVel.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "rest")) {
         myRest.scan (rtok);
         myRestValidP = true;
         return true;
      }
      else if (scanAttributeName (rtok, "restExplicit")) {
         myRestExplicitP = rtok.scanBoolean();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.println ("position=[ " + getPosition().toString (fmt) + " ]");
      if (!myVel.equals(Vector3d.ZERO)) {
         pw.println ("velocity=[ " + myVel.toString (fmt) + " ]");
      }
      pw.println ("rest=[ " + getRestPosition().toString (fmt) + " ]");
      if (myRestExplicitP) {
         pw.println ("restExplicit=true");
      }
   }  

   public void saveRenderCoords() {
      Point3d pos = getPosition();
      myRenderCoords[0] = (float)pos.x;
      myRenderCoords[1] = (float)pos.y;
      myRenderCoords[2] = (float)pos.z;
   }

   public float[] getRenderCoords() {
      return myRenderCoords;
   }
   
   @Override
   public BackNode3d copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      BackNode3d back = (BackNode3d)super.copy (flags, copyMap);
      
      if ((flags & CopyableComponent.REST_POSITION) != 0) {
         back.myPos = new Point3d(myRest);
         back.myVel = new Vector3d();
         back.myRest = new Point3d(myRest);
         back.myForce = new Vector3d();
      }
      else {
         back.myPos = new Point3d(myPos);
         back.myVel = new Vector3d(myVel);
         back.myRest = new Point3d(myRest);
         back.myForce = new Vector3d(myForce);
      }

      back.myInternalForce = new Vector3d();
      back.myRenderCoords = new float[3];      
      
      return back;
   }

}
