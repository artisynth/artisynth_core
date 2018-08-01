/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix6dBlock;
import maspack.matrix.Matrix6x3Block;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Point3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import artisynth.core.mechmodels.MotionTarget.TargetActivity;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.HasCoordinateFrame;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.Traceable;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;

public class Frame extends DynamicComponentBase
   implements TransformableGeometry, ScalableUnits, DynamicComponent,
              Traceable, MotionTargetComponent, CopyableComponent,
              HasCoordinateFrame, CollidableDynamicComponent,
              PointAttachable, FrameAttachable {

   public static boolean dynamicVelInWorldCoords = true;

   protected FrameState myState = new FrameState();
   protected FrameTarget myTarget = null;
   protected TargetActivity myTargetActivity = TargetActivity.Auto;
   protected Quaternion myQvel = new Quaternion();

   protected Wrench myForce;
   protected Wrench myExternalForce;
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

   //protected MeshComponentList<RigidMeshComp> myMeshList;

   static {                 
      myProps.add (
         "renderProps * *", "render properties", null);
      myProps.add (
         "pose * *", "pose state", null, "NE NW");
      myProps.add (
         "position", "position of the body coordinate frame",null,"NW");
      myProps.add (
         "orientation", "orientation of the body coordinate frame", null, "NW");
      myProps.add (
         "rotation", "rotation of the body coordinate frame",null,"NW");
      myProps.add ("velocity * *", "velocity state", null, "NW");
      
      myProps.add (
         "targetPose", "target pose", RigidTransform3d.IDENTITY, "NE NW");
      myProps.add (
         "targetPosition", "target position of the body coordinate frame", 
         Point3d.ZERO, "NW");
      myProps.add (
         "targetOrientation", "target orientation for the body coordinate frame",
         AxisAngle.IDENTITY, "NW");
      myProps.add (
         "targetVelocity", "velocity target", Twist.ZERO, "NW");
      myProps.add (
         "targetActivity", "specifies which targets are active",
         TargetActivity.Auto, "NW");
      
      myProps.addReadOnly (
         "force", "total force wrench", "NW");
      myProps.addReadOnly (
         "transForce", "translational component of total force wrench", "NW");
      myProps.addReadOnly (
         "moment", "moment component total force wrench", "NW");
      myProps.add (
         "externalForce * *", "external force wrench", null, "NW");
      myProps.add (
         "axisLength * *", "length of rendered frame axes", 1f);
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
      //initializeChildComponents();
   }

   // protected void initializeChildComponents() {
   //    myComponents = 
   //       new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
   //    myMeshList =
   //       new MeshComponentList<RigidMeshComp>(
   //          RigidMeshComp.class, "meshes", "msh");
   //    add(myMeshList);
   // }

   public Frame (RigidTransform3d X) {
      this();
      setPose (X);
   }

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
      updateVelState();
   }

   public void setVelocity (
      double vx, double vy, double vz, double wx, double wy, double wz) {
      setVelocity (new Twist (vx, vy, vz, wx, wy, wz));
   }

   public void setPose (RigidTransform3d XFrameToWorld) {
      myState.setPose (XFrameToWorld);
      updatePosState();
   }
   
   public void transformPose (RigidTransform3d T) {
      RigidTransform3d TFW = new RigidTransform3d();
      TFW.mul (T, myState.getPose());
      myState.setPose (TFW);
      updatePosState();
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
      updatePosState();
   }

   public AxisAngle getOrientation() {
      return myState.getAxisAngle();
   }

   public void getOrientation(AxisAngle axisAng) {
      axisAng.set(myState.getAxisAngle());
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
      updatePosState();
   }

//   public void updatePose() {
//      myState.updatePose();
//   }

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
    * Computes the location, in body coordinates, of a point whose position
    * is given in world coordinates. This is the inverse computation
    * of {@link #computePointPosition}.
    * 
    * @param loc
    * returns the point location
    * @param pos
    * position of the point, in world coordinates
    */
   public void computePointLocation (Vector3d loc, Vector3d pos) {
      loc.sub (pos, myState.XFrameToWorld.p);
      loc.inverseTransform (myState.XFrameToWorld.R);
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
    * Adds a point mass to a matrix that contains the spatial
    * inertia for this Frame.
    *
    * @param M matrix containing existing spatial inertia
    * @param m mass of the point
    * @param loc location of the point (in local frame coordinates)
    */
   public void addPointMass (Matrix M, double m, Vector3d loc) {
      SpatialInertia.addPointMass (M, m, loc);
   }

   /**
    * Computes the force Jacobian, in world coordinates, for a point attached
    * to this frame. If the velocity state size (vsize) of this frame is 6,
    * then GT should be an instance of Matrix6x3Block. Otherwise, it should be
    * an instance of MatrixNdBlock with a size of vsize x 3.
    * 
    * For the Matrix6x3Block case, the Jacobian is a 6x3 matrix with 
    * the following form:
    * <pre>
    * [     I     ]
    * [           ]
    * [ [ R loc ] ]
    * </pre>
    * where I is the 3x3 identity matrix, R is the frame orientation matrix,
    * and [ x ] denotes the 3x3 skew-symmetric cross product matrix.
    *
    * @param GT returns the force Jacobian
    * @param loc
    * position of the point, in body coordinates
    */
    public void computeWorldPointForceJacobian (MatrixBlock GT, Point3d loc) {
       
       Matrix6x3Block blk;
       try {
          blk = (Matrix6x3Block)GT;
       }
       catch (ClassCastException e) {
          throw new IllegalArgumentException (
             "GT is not an instance of Matrix6x3Block, is "+GT.getClass());
       }       
       RotationMatrix3d R = getPose().R;
       blk.setZero();
       blk.m00 = 1;
       blk.m11 = 1;
       blk.m22 = 1;

       double lxb = loc.x;
       double lyb = loc.y;
       double lzb = loc.z;
         
       double lxw = R.m00*lxb + R.m01*lyb + R.m02*lzb;
       double lyw = R.m10*lxb + R.m11*lyb + R.m12*lzb;
       double lzw = R.m20*lxb + R.m21*lyb + R.m22*lzb;

       blk.m40 =  lzw;
       blk.m50 = -lyw;
       blk.m51 =  lxw;
       
       blk.m31 = -lzw;
       blk.m32 =  lyw;
       blk.m42 = -lxw;
    }

    /**
     * Computes a force Jacobian for a point attached to this frame, and then
     * optionally rotates its columns using a rotation matrix R. If the
     * velocity state size (vsize) of this frame is 6, then G should be an
     * instance of Matrix6x3Block. Otherwise, it should be an instance of
     * MatrixNdBlock with a size of vsize x 3.
     * 
     * If the rotation matrix R is <code>null</code>, then the Jacobian
     * takes the following form for the 6x3 matrix case:
     * <pre>
     * [     I       ]
     * [             ]
     * [ [ RF loc ]  ]
     * </pre>
     * where RF is the frame orientation matrix, loc is the location of the
     * point in frame coordinates, and [ x ] denotes the 3x3 skew-symmetric
     * cross product matrix.
     *
     * If the rotation matrix R is not <code>null</code>, then the
     * 6x3 case takes the following form:
     * <pre>
     * [     R         ]
     * [               ]
     * [ [ RF loc ]  R ]
     * </pre>
     *
     * @param GT returns the force Jacobian
     * @param loc location of the point, relative to the frame
     * @param R optional rotation transform
     */
     public void computeLocalPointForceJacobian (
        MatrixBlock GT, Vector3d loc, RotationMatrix3d R) {
        
        Matrix6x3Block blk;
        try {
           blk = (Matrix6x3Block)GT;
        }
        catch (ClassCastException e) {
           throw new IllegalArgumentException (
              "GT is not an instance of Matrix6x3Block, is "+GT.getClass());
        }       

        Vector3d locw = new Vector3d();
        locw.transform (getPose().R, loc);
        double x = locw.x;
        double y = locw.y;
        double z = locw.z;

        if (R == null) {
           blk.m00 = 1;
           blk.m01 = 0;
           blk.m02 = 0;
           blk.m10 = 0;
           blk.m11 = 1;
           blk.m12 = 0;
           blk.m20 = 0;
           blk.m21 = 0;
           blk.m22 = 1;

           blk.m30 = 0;
           blk.m31 = -z;
           blk.m32 = y;
           blk.m40 = z;
           blk.m41 = 0;
           blk.m42 = -x;           
           blk.m50 = -y;
           blk.m51 = x;
           blk.m52 = 0;
        }
        else {
           blk.m00 = R.m00;
           blk.m01 = R.m01;
           blk.m02 = R.m02;
           blk.m10 = R.m10;
           blk.m11 = R.m11;
           blk.m12 = R.m12;
           blk.m20 = R.m20;
           blk.m21 = R.m21;
           blk.m22 = R.m22;

           blk.m30 = y*R.m20 - z*R.m10;
           blk.m31 = y*R.m21 - z*R.m11;
           blk.m32 = y*R.m22 - z*R.m12;
           blk.m40 = z*R.m00 - x*R.m20;
           blk.m41 = z*R.m01 - x*R.m21;
           blk.m42 = z*R.m02 - x*R.m22;
           blk.m50 = x*R.m10 - y*R.m00;
           blk.m51 = x*R.m11 - y*R.m01;
           blk.m52 = x*R.m12 - y*R.m02;
        }
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

   /**
    * Computes the velocity, in world coordinates, of a point attached to this
    * frame.
    * 
    * @param vel
    * returns the point velocity
    * @param loc
    * position of the point, in body coordinates
    * @param pvel
    * independent velocity for the point, in body coordinates
    */
   public void computePointVelocity (Vector3d vel, Point3d loc, Vector3d pvel) {
      // use vel to store loc transformed into world coords
      Twist frameVel = myState.getVelocity();
      vel.transform (myState.XFrameToWorld.R, loc);
      vel.crossAdd (frameVel.w, vel, frameVel.v);
      Vector3d tmp = new Vector3d();
      tmp.transform (myState.XFrameToWorld.R, pvel);
      vel.add (tmp);
   }

   /**
    * Computes the velocity derivative of a point attached to this frame
    * that is due to the current velocity of the frame.
    * 
    * @param cor
    * returns the point Coriolis term (in world coordinates)
    * @param loc
    * position of the point, in body coordinates
    */
   public void computePointCoriolis (Vector3d cor, Vector3d loc) {
      Twist tw = getVelocity();

      cor.transform (getPose().R, loc);
      cor.cross (tw.w, cor);
      cor.cross (tw.w, cor);      
   }

   /**
    * Subtracts from <code>wr</code> the wrench arising from applying a force
    * <code>f</code> on a point <code>loc</code>.
    *
    * @param wr wrench from which force is subtracted (world coordinates
    * @param loc location of the point (body coordinates)
    * @param f force acting on the point (world coordinates)
    */
   public void subPointForce (Wrench wr, Point3d loc, Vector3d f) {
      // transform position to world coordinates
      myTmpPos.transform (myState.XFrameToWorld.R, loc);
      wr.f.sub (f);
      wr.m.crossAdd (f, myTmpPos, wr.m);
   }

   /**
    * Adds to <code>wr</code> the wrench arising from applying a force
    * <code>f</code> on a point <code>loc</code>.
    *
    * @param wr wrench in which force is accumulated (world coordinates
    * @param loc location of the point (body coordinates)
    * @param f force applied to the point (world coordinates)
    */
   public void addPointForce (Wrench wr, Point3d loc, Vector3d f) {
      // transform position to world coordinates
      myTmpPos.transform (myState.XFrameToWorld.R, loc);
      wr.f.add (f);
      wr.m.crossAdd (myTmpPos, f, wr.m);
   }

   /**
    * Adds to this body's forces the wrench arising from applying a force
    * <code>f</code> on a point <code>loc</code>.
    *
    * @param loc location of the point (body coordinates)
    * @param f force applied to the point (world coordinates)
    */
   public void addPointForce (Point3d loc, Vector3d f) {
      addPointForce (myForce, loc, f);
   }
   
   /**
    * Subtracts from this body's forces the wrench arising from applying a force
    * <code>f</code> on a point <code>loc</code>.
    *
    * @param loc location of the point (body coordinates)
    * @param f force applied to the point (world coordinates)
    */
   public void subPointForce (Point3d loc, Vector3d f) {
      subPointForce (myForce, loc, f);
   }

//   /**
//    * Adds to this body's external forces the wrench arising from
//    * applying a force <code>f</code> on a point <code>loc</code>.
//    *
//    * @param loc location of the point (body coordinates)
//    * @param f force applied to the point (world coordinates)
//    */
//   public void addExternalPointForce (Point3d loc, Vector3d f) {
//      addPointForce (myExternalForce, loc, f);
//   }

   /**
    * Computes the wrench (in body coordinates) produced by applying a
    * force at a particular point.
    *
    * @param wr returns the wrench in body coordinates
    * @param f applied force at the point (world coordinates)
    * @param p location of the point on the body (body coordinates)
    */
   public void computeAppliedWrench (Wrench wr, Vector3d f, Vector3d p) {
      wr.f.inverseTransform (myState.XFrameToWorld.R, f);
      wr.m.cross (p, wr.f);
   }

//   /**
//    * Computes the wrench (in body coordinates) produced by applying a
//    * force at a particular point.
//    *
//    * @param wr returns the wrench in body coordinates
//    * @param f applied force at the point (world coordinates)
//    * @param p location of the point on the body (body coordinates)
//    */
//   public void computeAppliedWrench (Matrix6x1 wr, Vector3d f, Vector3d p) {
//      myBodyForce.f.inverseTransform (myState.XFrameToWorld.R, f);
//      wr.setWrench (myBodyForce.f, p);
//   }

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

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      myState.pos.updateBounds (pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {
      if (myAxisLength > 0) {
         int lineWidth = myRenderProps.getLineWidth();
         renderer.drawAxes (
            myRenderFrame, myAxisLength, lineWidth, isSelected());
         //renderer.setLineWidth (1);
      }
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      super.transformGeometry (gtr, context, flags);

      // transform the pose
      RigidTransform3d Xpose = new RigidTransform3d();
      Xpose.set (myState.XFrameToWorld);
      gtr.transform (Xpose);
      myState.setPose (Xpose);
   } 
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      super.addTransformableDependencies (context, flags);
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

   // public void setForcesToExternal() {
   //    myForce.set (myExternalForce);
   // }

   public void applyExternalForces() {
      myForce.add (myExternalForce);
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

   public void getEffectiveMass (Matrix M, double t) {
      doGetInertia (M, SpatialInertia.ZERO);
   }

   public int mulInverseEffectiveMass (
      Matrix M, double[] a, double[] f, int idx) {
      a[idx++] = 0;
      a[idx++] = 0;
      a[idx++] = 0;
      a[idx++] = 0;
      a[idx++] = 0;
      a[idx++] = 0;
      return idx;
   }

   public void resetEffectiveMass() {
   }

   /**
    * Adds a point mass to the effective spatial inertia for this Frame.
    *
    * @param m mass of the point
    * @param loc location of the point (in local frame coordinates)
    */
   public void addEffectivePointMass (double m, Vector3d loc) {
      // subclasses must override if necessary; Frame itself has no inertia
   }

   public int getEffectiveMassForces (VectorNd f, double t, int idx) {
      double[] buf = f.getBuffer();
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      return idx;
   }

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

//   public MatrixBlock createSolveBlock () {
//      FrameBlock blk = new FrameBlock (this);
//      mySolveBlock = blk;
//      return blk;
//   }
                                        
   public void setState (Frame frame) {
      myState.set (frame.myState);
      updatePosState();
      updateVelState();
   }

//   public int setState (VectorNd x, int idx) {
//      return myState.set (x, idx);
//   }

//   public void getState (FrameState state) {
//      state.set (myState);
//   }

//   public int getState (VectorNd x, int idx) {
//      return myState.get (x, idx);
//   }

//   public void setState (DynamicComponent c) {
//      if (c instanceof Frame) {
//         setState ((Frame)c);
//      }
//      else {
//         throw new IllegalArgumentException ("component c is not a Frame");
//      }
//   }
   
   public int getPosState (double[] buf, int idx) {
      idx = myState.getPos (buf, idx);
      return idx;
   }

   public int setPosState (double[] buf, int idx) {
      idx = myState.setPos (buf, idx);
      updatePosState();
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
      updateVelState();
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
 
   public Frame copy (
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
      updateVelState();
   }

   public void getBodyVelocity (Twist v) {
      myState.getBodyVelocity (v);
   }

   /**
    * {@inheritDoc}
    */
   public String[] getTraceables() {
      return new String[] { "transforce", "moment" };
   }
   
   /**
    * {@inheritDoc}
    */
   public String getTraceablePositionProperty (String traceableName) {
      return "position";
   }

   public PointFrameAttachment createPointAttachment (Point pnt) {
      PointFrameAttachment pfa = new PointFrameAttachment (this, pnt, null);
      if (DynamicAttachment.containsLoop (pfa, pnt, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      return pfa;
   }

   public FrameFrameAttachment createFrameAttachment (
      Frame frame, RigidTransform3d TFW) {
      if (frame == null && TFW == null) {
         throw new IllegalArgumentException (
            "frame and TFW cannot both be null");
      }
      if (frame != null && frame.isAttached()) {
         throw new IllegalArgumentException ("frame is already attached");
      }
      FrameFrameAttachment ffa = new FrameFrameAttachment (frame);
      ffa.set (this, TFW);
      if (frame != null) {
         if (DynamicAttachment.containsLoop (ffa, frame, null)) {
            throw new IllegalArgumentException (
               "attachment contains loop");
         }
      }
      return ffa;
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

   public void setContactConstraint (
      double[] buf, double w, Vector3d dir, ContactPoint cpnt) {

      double lx = cpnt.myPoint.x - myState.pos.x;
      double ly = cpnt.myPoint.y - myState.pos.y;
      double lz = cpnt.myPoint.z - myState.pos.z;

      double nx = w*dir.x;
      double ny = w*dir.y;
      double nz = w*dir.z;

      buf[0] = nx;
      buf[1] = ny;
      buf[2] = nz;
      buf[3] = ly*nz - lz*ny;
      buf[4] = lz*nx - lx*nz;
      buf[5] = lx*ny - ly*nx;
   }

   public void addToPointVelocity (
      Vector3d vel, double w, ContactPoint cpnt) {
      
      // get point in world-oriented body coords
      Vector3d v = myState.vel.v;
      Vector3d o = myState.vel.w; // o for omega
      double lx = cpnt.myPoint.x - myState.pos.x;
      double ly = cpnt.myPoint.y - myState.pos.y;
      double lz = cpnt.myPoint.z - myState.pos.z;
      vel.x += w*(v.x - ly*o.z + lz*o.y);
      vel.y += w*(v.y - lz*o.x + lx*o.z);
      vel.z += w*(v.z - lx*o.y + ly*o.x);
   }

   /**
    * Called whenever the position state is changed. Subclasses
    * can use this as a hook to update anything that depends
    * on the position state.
    */
   protected void updatePosState() {
      // for (MeshComponent mc : myMeshList) {
      //    mc.setMeshToWorld (myState.XFrameToWorld);
      // }
   }
   
   /**
    * Called whenever the velocity state is changed. Subclasses
    * can use this as a hook to update anything that depends
    * on the velocity state.
    */
   protected void updateVelState() {
      
   }
//   public boolean requiresContactVertexInfo() {
//      return false;
//   }

}
