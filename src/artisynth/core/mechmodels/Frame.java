/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MotionTarget.TargetActivity;
import artisynth.core.probes.TracingProbe;
import artisynth.core.probes.VectorTracingProbe;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.TransformableGeometry;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.spatialmotion.*;

import javax.media.opengl.*;

import java.util.*;

public class Frame extends DynamicComponentBase
   implements TransformableGeometry, ScalableUnits, DynamicComponent,
              Tracable, MotionTargetComponent, CopyableComponent, HasCoordinateFrame {

   public static boolean dynamicVelInWorldCoords = true;

   FrameState myState = new FrameState();
   protected FrameTarget myTarget = null;
   protected TargetActivity myTargetActivity = TargetActivity.Auto;
   protected Quaternion myQvel = new Quaternion();

   Wrench myForce;
   Wrench myExternalForce;
   public RigidTransform3d myRenderFrame; // public for debugging
   double myAxisLength = 0;
   protected MatrixBlock mySolveBlock;
   protected int mySolveBlockNum = -1;
   // protected Activity myActivity = Activity.Unknown;
   protected boolean mySolveBlockValidP = false;
   protected Point3d myTmpPos = new Point3d();
   protected double myFrameDamping = 0;
   protected PropertyMode myFrameDampingMode = PropertyMode.Inherited;
   protected double myRotaryDamping = 0;
   protected PropertyMode myRotaryDampingMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList (Frame.class, ModelComponentBase.class);

   protected Wrench myBodyForce = new Wrench(); // preallocated temporary

   static {
      myProps.add ("renderProps * *", "render properties", null);
      myProps.add ("pose * *", "pose state", null, "NE NW");
      myProps.add ("position", "position of the body coordinate frame",null,"NW");
      myProps.add (
         "orientation", "orientation of the body coordinate frame", null, "NW");
      myProps.add ("velocity * *", "velocity state", null, "NW");
      
      myProps.add (
         "targetPose", "target pose", RigidTransform3d.IDENTITY, "NE NW");
      myProps.add (
         "targetPosition", "target position of the body coordinate frame", 
         Point3d.ZERO, "NW");
      myProps.add (
         "targetOrientation", "target orientation for the body coordinate frame",
         AxisAngle.IDENTITY, "NW");
      myProps.add ("targetVelocity", "velocity target", Twist.ZERO, "NW");
      myProps.add ("targetActivity", "specifies which targets are active",
                   TargetActivity.Auto, "NW");
      
      myProps.addReadOnly ("force", "total force wrench", "NW");
      myProps.addReadOnly (
         "transForce", "translational component of total force wrench", "NW");
      myProps.addReadOnly ("moment", "moment component total force wrench", "NW");
      myProps.add ("externalForce * *", "external force wrench", null, "NW");
      myProps.add ("axisLength * *", "length of rendered frame axes", 1f);
      myProps.addInheritable (
         "frameDamping:Inherited", "intrinsic translational damping", 0.0);
      myProps.addInheritable (
         "rotaryDamping:Inherited", "intrinsic rotational damping", 0.0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Frame() {
      myState = new FrameState();
      myForce = new Wrench();
      myExternalForce = new Wrench();
      myRenderFrame = new RigidTransform3d();
      setRenderProps (createRenderProps());
      mySolveBlockValidP = false;
   }

   public Frame (RigidTransform3d X) {
      this();
      setPose (X);
   }

//   public FrameState getState() {
//      return myState;
//   }

   public Wrench getForce() {
      return myForce;
   }

   public void getForce (Wrench wr) {
      wr.set (myForce);
   }

   public void getBodyForce (Wrench wr) {
      wr.inverseTransform (myState.XFrameToWorld.R, myForce);
   }

   public Vector3d getTransForce() {
      return myForce.f;
   }

   public Vector3d getMoment() {
      return myForce.m;
   }

   public void setForce (Wrench w) {
      myForce.set (w);
   }

   public void addForce (Wrench w) {
      myForce.add (w);
   }

   public Wrench getExternalForce() {
      return myExternalForce;
   }

   public void setExternalForce (Wrench w) {
      myExternalForce.set (w);
   }
   
   public void addExternalForce (Wrench w) {
      myExternalForce.add(w);
   }
   
   public void addScaledExternalForce (double s, Wrench w) {
      myExternalForce.scaledAdd(s, w);
   }

   public Twist getVelocity() {
      return myState.getVelocity();
   }

   public void getVelocity(Twist v) {
      v.set (myState.getVelocity());
   }

   public void setVelocity (Twist v) {
      myState.setVelocity (v);
   }

   public void setVelocity (
      double vx, double vy, double vz, double wx, double wy, double wz) {
      setVelocity (new Twist (vx, vy, vz, wx, wy, wz));
   }

   public void setPose (RigidTransform3d XFrameToWorld) {
      myState.setPose (XFrameToWorld);
   }

   public RigidTransform3d getPose() {
      return myState.XFrameToWorld;
   }

   public void getPose (RigidTransform3d XFrameToWorld) {
      myState.getPose (XFrameToWorld);
   }

   public Point3d getPosition() {
      return new Point3d (myState.XFrameToWorld.p);
   }

   public void setPosition (Point3d pos) {
      myState.setPosition (pos);
   }

   public AxisAngle getOrientation() {
      return myState.getAxisAngle();
   }

   public void setOrientation (AxisAngle axisAng) {
      RigidTransform3d X = new RigidTransform3d (myState.XFrameToWorld);
      X.R.setAxisAngle (axisAng);
      setPose (X);
   }

   public Quaternion getRotation() {
      return myState.getRotation();
   }

   public void setRotation (Quaternion q) {
      myState.setRotation (q);
   }

   public void updatePose() {
      myState.updatePose();
   }

   /**
    * {@inheritDoc}
    */
   public void applyGravity (Vector3d gacc) {
      // subclasses with non-zero inertia must override this method
   }

   public double getAxisLength() {
      return myAxisLength;
   }

   public void setAxisLength (double len) {
      myAxisLength = Math.max (0, len);
   }

   /**
    * Computes the position, in world coordinates, of a point attached to this
    * frame.
    * 
    * @param pos
    * returns the point position
    * @param loc
    * position of the point, in body coordinates
    */
   public void computePointPosition (Vector3d pos, Point3d loc) {
      pos.transform (myState.XFrameToWorld.R, loc);
      pos.add (myState.XFrameToWorld.p);
   }


   /**
    * Computes the velocity, in world coordinates, of a point attached to this
    * frame.
    * 
    * @param vel
    * returns the point velocity
    * @param loc
    * position of the point, in body coordinates
    */
   public void computePointVelocity (Vector3d vel, Point3d loc) {
      computePointVelocity (vel, loc, myState.getVelocity());
   }

   /**
    * Computes the velocity, in world coordinates, of a point attached to this
    * frame.
    * 
    * @param vel
    * returns the point velocity
    * @param loc
    * position of the point, in body coordinates
    * @param frameVel
    * velocity of the frame, in rotated world coordinates
    */
   public void computePointVelocity (Vector3d vel, Point3d loc, Twist frameVel) {
      // use vel to store loc transformed into world coords
      vel.transform (myState.XFrameToWorld.R, loc);
      vel.crossAdd (frameVel.w, vel, frameVel.v);
   }

//   /** 
//    * Computes the portion of this frame's velocity that is parametrically
//    * determined. This may be non-zero if the component itself is parametric,
//    * or if it is attached, directly or indirectly, to one or more parametric
//    * components.
//    * 
//    * @param vel returns the parametric velocity
//    * @return false if there is no parametric velocity component.
//    */
//   public boolean computeParametricVelocity (Twist vel) {
//      if (isParametric()) {
//         vel.set (myState.getVelocity());
//         return true;
//      }
//      else if (myAttachment != null) {
//         return myAttachment.computeParametricVelocity (vel);
//      }
//      else {
//         vel.setZero();
//         return false;
//      }
//   }

//   /** 
//    * Computes the portion of this frame's velocity that is actively
//    * determined.
//    * 
//    * @param vel returns the active velocity
//    * @return false if there is no active velocity component.
//    */
//   public boolean computeActiveVelocity (Twist vel) {
//      if (isActive()) {
//         vel.set (myState.getVelocity());
//         return true;
//      }
//      else if (myAttachment != null) {
//         return myAttachment.computeActiveVelocity (vel);
//      }
//      else {
//         vel.setZero();
//         return false;
//      }
//   }

//   /**
//    * Applies an impulse defined by lam*gt to the velocity of this frame.  The
//    * impulse is assumed to be in body coordinates.  If the frame is attached,
//    * the impulse is passed on to the masters.  If the frame is parametrically
//    * controlled, nothing happens.
//    */
//   public void applyVelImpulse (double lam, Wrench gt) {
//      if (myAttachment != null) {
//         myAttachment.applyVelImpulse (lam, gt, /*ignoreRigidBodies=*/false);
//      }
//   }

//   /**
//    * Adjusts the velocity of this frame by applying an impulse lam*dir at a
//    * specific point on the frame. If the frame is attached, the impulse is
//    * passed on to the masters.  If the frame is parametrically controlled,
//    * nothing happens. The direction <code>dir</code> is given in world
//    * coordinates, while the point location <code>loc</code> is given in body
//    * coordinates.
//    */
//   public void applyVelImpulse (double lam, Vector3d dir, Vector3d loc) {
//      if (isActive()) {
//         computeAppliedWrench (myBodyForce, dir, loc);
//         applyVelImpulse (lam, myBodyForce);
//      }
//      else if (myAttachment != null) {
//         computeAppliedWrench (myBodyForce, dir, loc);
//         myAttachment.applyVelImpulse (
//            lam, myBodyForce, /*ignoreRigidBodies=*/false);
//      }
//   }

//   /**
//    * Applies an impulse defined by lam*gt to the position of this frame.  The
//    * impulse is assumed to be in body coordinates.  If the frame is attached,
//    * the impulse is passed on to the masters.  If the frame is parametrically
//    * controlled, nothing happens.
//    */
//   public void applyPosImpulse (double lam, Wrench gt) {
//      if (myAttachment != null) {
//         myAttachment.applyPosImpulse (lam, gt, /*ignoreRigidBodies=*/false);
//      }
//   }

//   /**
//    * Adjusts the position of this frame by applying an impulse lam*dir at a
//    * specific point on the frame. If the frame is attached, the impulse is
//    * passed on to the masters.  If the frame is parametrically controlled,
//    * nothing happens. The direction <code>dir</code> is given in world
//    * coordinates, while the point location <code>loc</code> is given in body
//    * coordinates.
//    */
//   public void applyPosImpulse (double lam, Vector3d dir, Vector3d loc) {
//      if (isActive()) {
//         computeAppliedWrench (myBodyForce, dir, loc);
//         applyPosImpulse (lam, myBodyForce);
//      }
//      else if (myAttachment != null) {
//         computeAppliedWrench (myBodyForce, dir, loc);
//         myAttachment.applyPosImpulse (
//            lam, myBodyForce, /*ignoreRigidBodies=*/false);
//      }
//   }

//   /**
//    * Returns the inverse mass felt along a certain direction.
//    */
//   public double getInverseMass (Twist dir) {
//      if (myAttachment != null) {
//         return myAttachment.getInverseMass (dir, /*ignoreRigidBodies=*/false);
//      }
//      else {
//         return 0;
//      }
//   }

//   /** 
//    * Returns the inverse mass associated with a specific attached point 
//    * moving in a particular direction. The direction is given in world
//    * coordinates, while the point location is given in body coordinates.
//    * 
//    * @param dir point direction (world coordinates)
//    * @param loc point location (body coordinates)
//    * @return inverse mass 
//    */
//   public double getInverseMass (Vector3d dir, Vector3d loc) {
//      if (myAttachment != null) {
//         computeAppliedWrench (myBodyForce, dir, loc);
//         return myAttachment.getInverseMass (
//            myBodyForce, /*ignoreRigidBodies=*/false );
//      }
//      else {
//         return 0;
//      }
//   }

   /**
    * Adds the effect of a force applied at a specific postion with respect to
    * this frame. The force is in world coordinates and the position in frame
    * coordinates.
    */
   public void addPointForce (Vector3d f, Point3d pos) {
      // transform position to world coordinates
      myTmpPos.transform (myState.XFrameToWorld.R, pos);
      myForce.f.add (f, myForce.f);
      myForce.m.crossAdd (myTmpPos, f, myForce.m);
   }

   /**
    * Computes the wrench (in body coordinates) produced by applying a
    * point at a particular point.
    *
    * @param wr returns the wrench in body coordinates
    * @param f applied force at the point (world coordinates)
    * @param p location of the point on the body (body coordinates)
    */
   public void computeAppliedWrench (Wrench wr, Vector3d f, Vector3d p) {
      wr.f.inverseTransform (myState.XFrameToWorld.R, f);
      wr.m.cross (p, wr.f);
   }

   /**
    * Computes the wrench (in body coordinates) produced by applying a
    * force at a particular point.
    *
    * @param wr returns the wrench in body coordinates
    * @param f applied force at the point (world coordinates)
    * @param p location of the point on the body (body coordinates)
    */
   public void computeAppliedWrench (Matrix6x1 wr, Vector3d f, Vector3d p) {
      myBodyForce.f.inverseTransform (myState.XFrameToWorld.R, f);
      wr.setWrench (myBodyForce.f, p);
   }

   public void resetTargets() {
      if (myTarget != null) {
         myTarget.syncState (TargetActivity.None, myState);
      }      
   }

   public TargetActivity getTargetActivity () {
      if (myTarget == null) {
         return myTargetActivity;
      }
      else {
         return myTarget.getActivity();
      }
   }

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
            myTarget = new FrameTarget (activity);
         }
         else {
            prevActivity = myTarget.getActivity();
            myTarget.setActivity (activity);
         }
         myTarget.syncState (prevActivity, myState);
      }
      myTargetActivity = activity;
   }

   protected void setDynamic (boolean dynamic) {
      if (myDynamicP && !dynamic) {
         if (myTarget != null) {
            myTarget.syncState (TargetActivity.None, myState);
         }
      }
      super.setDynamic (dynamic);
   }         

   public Point3d getTargetPosition () {
      if (myTarget == null) {
         return myState.pos;
      }
      else {
         return myTarget.getTargetPos (myState);
      }
   }

   public void setTargetPosition (Point3d pos) {
      if (myTarget == null) {
         myTarget = new FrameTarget (myTargetActivity);
         myTarget.setTargetRot (myState.rot);
      }
      myTarget.setTargetPos (pos);
   }

   public AxisAngle getTargetOrientation () {
      if (myTarget == null) {
         return myState.getAxisAngle();
      }
      else {
         return myTarget.getTargetAxisAngle (myState);
      }
   }

   public void setTargetOrientation (AxisAngle axisAng) {
      if (myTarget == null) {
         myTarget = new FrameTarget (myTargetActivity);
         myTarget.setTargetPos (myState.pos);
      }
      myTarget.setTargetRot (axisAng);
   }

   public RigidTransform3d getTargetPose () {
      if (myTarget == null) {
         return myState.XFrameToWorld;
      }
      else {
         return myTarget.getTargetPose (myState);
      }
   }

   public void setTargetPose (RigidTransform3d X) {
      if (myTarget == null) {
         myTarget = new FrameTarget (myTargetActivity);
      }
      myTarget.setTargetPos (X.p);
      myTarget.setTargetRot (X.R);
   }

   public Twist getTargetVelocity () {
      if (myTarget == null) {
         return myState.getVelocity();
      }
      else {
         return myTarget.getTargetVel (myState);
      }
   }

   public void setTargetVelocity (Twist vel) {
      if (myTarget == null) {
         myTarget = new FrameTarget (myTargetActivity);
      }
      myTarget.setTargetVel (vel);
   }

   public int getTargetVel (double[] velt, double s, double h, int idx) {
      if (myTarget == null) {
         return myState.getVel (velt, idx);
      }
      else {
         return myTarget.getTargetVel (velt, s, h, myState, idx);
      }
   }

   public int setTargetVel (double[] velt, int idx) {
      if (myTarget == null) {
         myTarget = new FrameTarget (myTargetActivity);
      }
      return myTarget.setTargetVel (velt, myState, idx);
   }

   public int getTargetPos (double[] post, double s, double h, int idx) {
      if (myTarget == null) {
         return myState.getPos (post, idx);
      }
      else {
         return myTarget.getTargetPos (post, s, h, myState, idx);
      }
   }

   public int setTargetPos (double[] post, int idx) {
      if (myTarget == null) {
         myTarget = new FrameTarget (myTargetActivity);
      }
      return myTarget.setTargetPos (post, idx);
   }

   public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      if (!isControllable()) {
         throw new IllegalStateException (
            "Target frame is not controllable");
      }
      Matrix6dBlock blk = new Matrix6dBlock();
      blk.setIdentity();
      J.addBlock (bi, getSolveIndex(), blk);
      return bi++;
   }

   public double getFrameDamping() {
      return myFrameDamping;
   }

   public void setFrameDamping (double d) {
      myFrameDamping = d;
      myFrameDampingMode =
         PropertyUtils.propagateValue (
            this, "frameDamping", d, myFrameDampingMode);
   }

   public PropertyMode getFrameDampingMode() {
      return myFrameDampingMode;
   }

   public void setFrameDampingMode (PropertyMode mode) {
      myFrameDampingMode =
         PropertyUtils.setModeAndUpdate (
            this, "frameDamping", myFrameDampingMode, mode);
   }

   public double getRotaryDamping() {
      return myRotaryDamping;
   }

   public void setRotaryDamping (double d) {
      myRotaryDamping = d;
      myRotaryDampingMode =
         PropertyUtils.propagateValue (
            this, "rotaryDamping", d, myRotaryDampingMode);
   }

   public PropertyMode getRotaryDampingMode() {
      return myRotaryDampingMode;
   }

   public void setRotaryDampingMode (PropertyMode mode) {
      myRotaryDampingMode =
         PropertyUtils.setModeAndUpdate (
            this, "rotaryDamping", myRotaryDampingMode, mode);
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineProps (this);
   }

   public void prerender (RenderList list) {
      myRenderFrame.set (myState.XFrameToWorld);
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      myState.pos.updateBounds (pmin, pmax);
   }

   public static void drawAxes (
      GLRenderer renderer, RigidTransform3d XFrameToWorld, float len) {
      
      GL2 gl = renderer.getGL2().getGL2();
      gl.glPushMatrix();
      renderer.setLightingEnabled (false);
      GLViewer.mulTransform (gl, XFrameToWorld);
      gl.glBegin (GL2.GL_LINES);
      renderer.setColor (1f, 0f, 0f);
      gl.glVertex3f (0f, 0f, 0f);
      gl.glVertex3f (len, 0f, 0f);
      renderer.setColor (0f, 1f, 0f);
      gl.glVertex3f (0f, 0f, 0f);
      gl.glVertex3f (0f, len, 0f);
      renderer.setColor (0f, 0f, 1f);
      gl.glVertex3f (0f, 0f, 0f);
      gl.glVertex3f (0f, 0f, len);
      gl.glEnd();
      renderer.setLightingEnabled (true);
      gl.glPopMatrix();
   }

   public void render (GLRenderer renderer, int flags) {
      if (myAxisLength > 0) {
         GL2 gl = renderer.getGL2().getGL2();
         gl.glLineWidth (myRenderProps.getLineWidth());
         drawAxes (renderer, myRenderFrame, (float)myAxisLength);
         gl.glLineWidth (1);
      }
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

      RigidTransform3d Xpose = new RigidTransform3d();
      AffineTransform3d Xlocal = new AffineTransform3d();

      Xpose.set (myState.XFrameToWorld);
      Xpose.mulAffineLeft (X, Xlocal.A);

      System.out.println ("Xpose=\n" + Xpose);

      myState.setPose (Xpose);
   }

   public void scaleDistance (double s) {
      myState.scaleDistance (s);
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      myRotaryDamping *= (s * s);
   }

   public void zeroExternalForces() {
      myExternalForce.setZero();
   }

   public void zeroForces() {
      myForce.setZero();
   }

   public void setForcesToExternal() {
      myForce.set (myExternalForce);
   }

   public void applyForces (double t) {
      if (myFrameDamping != 0 || myRotaryDamping != 0) {
         Twist velBody = myState.getVelocity();
         if (myFrameDamping != 0) {
            myForce.f.scaledAdd (-myFrameDamping, velBody.v);
         }
         if (myRotaryDamping != 0) {
            myForce.m.scaledAdd (-myRotaryDamping, velBody.w);
         }
      }
   }

   public void addVelJacobian (SparseNumberedBlockMatrix S, double s) {
      if (mySolveBlockNum != -1) {
         if (myFrameDamping != 0 || myRotaryDamping != 0) {
            FrameBlock blk =
               (FrameBlock)S.getBlockByNumber (mySolveBlockNum);
            blk.addFrameDamping (s * myFrameDamping, s * myRotaryDamping);
         }
      }
   }

   public void addPosJacobian (SparseNumberedBlockMatrix S, double s) {
      // nothing to do
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix S) {
      // nothing to do, block will already have been added
   }

   public int getJacobianType() {
      return Matrix.SPD;
   }

   public void scaleMass (double s) {
      myFrameDamping *= s;
      myRotaryDamping *= s;
   }

//   public FrameBlock getSolveBlock() {
//      return mySolveBlock;
//   }

   public MatrixBlock createMassBlock() {
      return new Matrix6dBlock();
   }

   public boolean isMassConstant() {
      return true;
   }

   public double getMass (double t) {
      return 0;
   }

   public void getMass (Matrix M, double t) {
      doGetInertia (M, SpatialInertia.ZERO);
   }

   public int getMassForces (VectorNd f, double t, int idx) {
      double[] buf = f.getBuffer();
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      return idx;
   }

//   public int adjustVelDeriv (VectorNd v, int idx) {
//      if (!dynamicVelInWorldCoords) {
//         Vector3d coriolisTerm = new Vector3d();
//         coriolisTerm.cross (myState.vel.w, myState.vel.v);
//         coriolisTerm.inverseTransform (myState.XFrameToWorld.R);
//
//         double[] buf = v.getBuffer();
//         buf[idx++] -= coriolisTerm.x;
//         buf[idx++] -= coriolisTerm.y;
//         buf[idx++] -= coriolisTerm.z;
//         idx += 3;
//      }
//      else {
//         idx += 6;
//      }
//      return idx;
//   }

//   public void getEffectiveMass (Matrix M) {
//      doGetInertia (M, SpatialInertia.ZERO);
//   }

   protected void doGetInertia (Matrix M, Matrix6d SI) {
      if (M instanceof Matrix6d) {
         ((Matrix6d)M).set (SI);
      }
      else {
         throw new IllegalArgumentException ("Matrix not instance of Matrix6d");
      }
   }

   public void addSolveBlock (SparseNumberedBlockMatrix S) {
      int bi = getSolveIndex();
      FrameBlock blk = new FrameBlock (this);
      mySolveBlockNum = S.addBlock (bi, bi, blk);
      mySolveBlock = blk;
   }

   public MatrixBlock createSolveBlock () {
      FrameBlock blk = new FrameBlock (this);
      mySolveBlock = blk;
      return blk;
   }
                                        
   public void setState (Frame frame) {
      myState.set (frame.myState);
   }

   public int setState (VectorNd x, int idx) {
      return myState.set (x, idx);
   }

   public void getState (FrameState state) {
      state.set (myState);
   }

   public int getState (VectorNd x, int idx) {
      return myState.get (x, idx);
   }

   public void setState (DynamicComponent c) {
      if (c instanceof Frame) {
         setState ((Frame)c);
      }
      else {
         throw new IllegalArgumentException ("component c is not a Frame");
      }
   }
   
//   public void setState (ComponentState state) {
//      try {
//         myState.set ((FrameState)state);
//      }
//      catch (ClassCastException e) {
//         throw new IllegalArgumentException (
//            "state is not an instance of FrameState");
//      }
//   }

//   public void getState (ComponentState state) {
//      try {
//         getState ((FrameState)state);
//      }
//      catch (ClassCastException e) {
//         throw new IllegalArgumentException (
//            "state is not an instance of FrameState");
//      }
//   }

//   public boolean hasState() {
//      return true;
//   }
//
//   public FrameState createState() {
//      return new FrameState();
//   }

   public int getPosState (double[] buf, int idx) {
      idx = myState.getPos (buf, idx);
      return idx;
   }

   public int setPosState (double[] buf, int idx) {
      idx = myState.setPos (buf, idx);
      return idx;
   }

   public void addPosImpulse (
      double[] xbuf, int xidx, double h, double[] vbuf, int vidx) {

      Twist vel = new Twist();
      vel.v.x = vbuf[vidx++];
      vel.v.y = vbuf[vidx++];
      vel.v.z = vbuf[vidx++];
      vel.w.x = vbuf[vidx++];
      vel.w.y = vbuf[vidx++];
      vel.w.z = vbuf[vidx++];   

      // XXX streamline this. This is only in the form it is to preserve exact
      // numeric compatibility with older code.

      RigidTransform3d X = new RigidTransform3d();
      X.p.set (xbuf[xidx], xbuf[xidx+1], xbuf[xidx+2]);
      Quaternion rot =
         new Quaternion (xbuf[xidx+3], xbuf[xidx+4], xbuf[xidx+5], xbuf[xidx+6]);
      rot.normalize();
      X.R.set (rot);

      if (dynamicVelInWorldCoords) {
         vel.extrapolateTransformWorld (X, h);
      }
      else {
         vel.extrapolateTransform (X, h);
      }
      rot.set (X.R);

      xbuf[xidx++] = X.p.x;
      xbuf[xidx++] = X.p.y;
      xbuf[xidx++] = X.p.z;

      xbuf[xidx++] = rot.s;
      xbuf[xidx++] = rot.u.x;
      xbuf[xidx++] = rot.u.y;
      xbuf[xidx++] = rot.u.z;

      // xbuf[xidx  ] += h*vbuf[vidx  ];
      // xbuf[xidx+1] += h*vbuf[vidx+1];
      // xbuf[xidx+2] += h*vbuf[vidx+2];

      // Vector3d w = new Vector3d (vbuf[vidx+3], vbuf[vidx+4], vbuf[vidx+5]);
      // double ang = w.norm();
      // if (ang > 0) {
      //    // TODO: make this more efficient
      //    w.scale (Math.sin(ang/2)/ang);
      //    Quaternion qp = new Quaternion (
      //       xbuf[xidx+3], xbuf[xidx+4], xbuf[xidx+5], xbuf[xidx+6]);
      //    Quaternion qv = new Quaternion (Math.cos(ang/2), w);
      //    if (dynamicVelInWorldCoords) {
      //       qp.mul (qv, qp);
      //    }
      //    else {
      //       qp.mul (qp, qv);
      //    }
      //    qp.normalize();
      //    xbuf[xidx+3] = qp.s;
      //    xbuf[xidx+4] = qp.u.x;
      //    xbuf[xidx+5] = qp.u.y;
      //    xbuf[xidx+6] = qp.u.z;
      // }
   }

   public int getPosDerivative (double[] dxdt, int idx) {
      dxdt[idx++] = myState.vel.v.x;
      dxdt[idx++] = myState.vel.v.y;
      dxdt[idx++] = myState.vel.v.z;
      myState.rotDerivative (myQvel);
      dxdt[idx++] = myQvel.s;
      dxdt[idx++] = myQvel.u.x;
      dxdt[idx++] = myQvel.u.y;
      dxdt[idx++] = myQvel.u.z;
      return idx;
   }

   public int getVelState (double[] buf, int idx) {
      if (dynamicVelInWorldCoords) {
         idx = myState.getVel (buf, idx);
      }
      else {
         idx = myState.getBodyVel (buf, idx, new Twist());
      }
      return idx;
   }

   public int getBodyVelState (double[] buf, int idx) {
      idx = myState.getBodyVel (buf, idx, new Twist());
      return idx;
   }

   public int getWorldVelState (double[] buf, int idx) {
      idx = myState.getVel (buf, idx);
      return idx;
   }

   public int setVelState (double[] buf, int idx) {
      if (dynamicVelInWorldCoords) {      
         idx = myState.setVel (buf, idx);
      }
      else {
         idx = myState.setBodyVel (buf, idx);
      }
      return idx;
   }

   public int setForce (double[] f, int idx) {
      myForce.f.x = f[idx++];
      myForce.f.y = f[idx++];
      myForce.f.z = f[idx++];
      myForce.m.x = f[idx++];
      myForce.m.y = f[idx++];
      myForce.m.z = f[idx++];
      return idx;
   }
 
   public int getForce (double[] f, int idx) {
      f[idx++] = myForce.f.x;
      f[idx++] = myForce.f.y;
      f[idx++] = myForce.f.z;
      f[idx++] = myForce.m.x;
      f[idx++] = myForce.m.y;
      f[idx++] = myForce.m.z;
      return idx;
   }
 
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      Frame comp = (Frame)super.copy (flags, copyMap);
      comp.myState = new FrameState();
      comp.myForce = new Wrench();
      comp.myExternalForce = new Wrench();
      comp.myRenderFrame = new RigidTransform3d();
      comp.myAxisLength = myAxisLength;
      comp.mySolveBlock = null;
      comp.mySolveBlockNum = -1;
      comp.mySolveBlockValidP = false;
      comp.setPose (getPose());
      return comp;
   }

   public void setBodyVelocity (Twist v) {
      myState.setBodyVelocity (v);
   }

   public void getBodyVelocity (Twist v) {
      myState.getBodyVelocity (v);
   }

   public String[] getTracables() {
      return new String[] { "transforce", "moment" };
   }

   public TracingProbe getTracingProbe (String tracableName) {
      if (tracableName.equals ("transforce")) {
         return new VectorTracingProbe (
            this, getProperty ("transForce"), getProperty ("position"), 1.0);
      }
      else if (tracableName.equals ("moment")) {
         return new VectorTracingProbe (
            this, getProperty ("moment"), getProperty ("position"), 1.0);
      }
      else {
         throw new IllegalArgumentException ("Unknown tracable '"
         + tracableName + "'");
      }
   }

   public int getVelStateSize() {
      return 6;
   }

   public int getPosStateSize() {
      return 7;
   }

   /**
    * {@inheritDoc}
    */
   public boolean velocityLimitExceeded (double tlimit, double rlimit) {
      Twist vel = getVelocity();
      if (vel.containsNaN() ||
          vel.v.infinityNorm() > tlimit ||
          vel.w.infinityNorm() > rlimit) {
         return true;
      }
      else {
         return false;
      }
   }

   @Override
   public boolean isDuplicatable() {
      return false;
   }

   @Override
   public boolean getCopyReferences(List<ModelComponent> refs,
      ModelComponent ancestor) {
      return false;
   }


}
