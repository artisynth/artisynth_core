/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6dBlock;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderableUtils;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.StringHolder;
import maspack.util.Disposable;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.Collidable.Group;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentListView;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.GeometryChangeEvent;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.HasNumericStateComponents;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformGeometryAction;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.*;

public class MechModel extends MechSystemBase implements
TransformableGeometry, ScalableUnits {

   boolean addConstraintJacobian = false;
   protected static boolean addConstraintForces = false;
   protected static boolean myAllowSelfIntersections = true;

   protected PointList<Particle> myParticles;
   protected PointList<Point> myPoints;
   protected AxialSpringList<AxialSpring> myAxialSprings;
   protected PointSpringList<MultiPointSpring> myMultiPointSprings;
   protected ComponentList<FrameSpring> myFrameSprings;
   protected ComponentList<ForceComponent> myForceEffectors;
   protected ComponentList<RigidBody> myRigidBodies;
   protected ComponentList<Frame> myFrames;
   protected ComponentList<MeshComponent> myMeshBodies;
   protected ComponentList<BodyConnector> myConnectors;
   protected ComponentList<ConstrainerBase> myConstrainers;
   protected PointList<FrameMarker> myFrameMarkers;
   protected CollisionManager myCollisionManager;

   protected ArrayList<DynamicComponent> myLocalDynamicComps;
   protected ArrayList<RequiresInitialize> myLocalInitComps;
   protected ArrayList<RequiresPrePostAdvance> myLocalPrePostAdvanceComps;
   protected ArrayList<MechSystemModel> myLocalModels;
   protected ArrayList<CollidableBody> myCollidableBodies;

   protected ComponentList<MuscleExciter> myExciterList;
   protected RenderableComponentList<RenderableComponent> myRenderables;
   protected Point3d myMinBound;
   protected Point3d myMaxBound;
   protected double myPointDamping = 0;
   protected PropertyMode myPointDampingMode = PropertyMode.Inherited;
   protected double myFrameDamping = 0;
   protected PropertyMode myFrameDampingMode = PropertyMode.Inherited;
   protected double myRotaryDamping = 0;
   protected PropertyMode myRotaryDampingMode = PropertyMode.Inherited;

   protected ComponentList<MechSystemModel> myModels;

   protected ComponentList<DynamicAttachmentComp> myAttachments;
   SparseNumberedBlockMatrix mySolveMatrix;

   // flag to indicate that CollidableBodies need re-indexing. This
   // will get set on a structure change event
   protected boolean myCollidableBodiesNeedIndexing = true;
   
   // flag to indicate that forces need updating. Since forces are always
   // updated before a call to advance() (within setDefaultInputs()), this
   // flag only needs to be set during the advance routine --
   // specifically, within the setState methods
   protected boolean myForcesNeedUpdating = false;

   // add the frame marker rotation effects when computing the stiffness
   // matrix. Note: this will make the stiffness matrix asymmetric, unless YPR
   // stiffness is used
   protected boolean myAddFrameMarkerStiffness = false;

   protected static double DEFAULT_POINT_DAMPING = 0;
   protected static double DEFAULT_FRAME_DAMPING = 0;
   protected static double DEFAULT_ROTARY_DAMPING = 0;
   protected static PosStabilization DEFAULT_STABILIZATION =
      PosStabilization.GlobalMass;

   protected static final Vector3d DEFAULT_GRAVITY = new Vector3d (0, 0, -9.8);

   protected Vector3d myGravity = new Vector3d(DEFAULT_GRAVITY);
   protected PropertyMode myGravityMode = PropertyMode.Inherited;

   protected static final double DEFAULT_PENETRATION_TOL = -1; 
   protected double myPenetrationTol = DEFAULT_PENETRATION_TOL;
   protected PropertyMode myPenetrationTolMode = PropertyMode.Inherited;

   protected static final double DEFAULT_ROTARY_LIMIT_TOL = 0.0001;
   protected double myRotaryLimitTol = DEFAULT_ROTARY_LIMIT_TOL;
   protected PropertyMode myRotaryLimitTolMode = PropertyMode.Inherited;

   protected double myMaxTranslationalVel = 1e10;
   protected double myMaxRotationalVel = 1e10;

   protected float[] myExcitationColor = null;
   protected PropertyMode myExcitationColorMode = PropertyMode.Explicit;
   protected double myMaxColoredExcitation = 1.0;
   protected PropertyMode myMaxColoredExcitationMode = PropertyMode.Explicit;

   public static PropertyList myProps =
      new PropertyList (MechModel.class, MechSystemBase.class);

   static {
      myProps.addInheritable (
         "gravity:Inherited", "acceleration of gravity", DEFAULT_GRAVITY);
      myProps.add (
         "stabilization", "position stabilization method", DEFAULT_STABILIZATION);
      myProps.addInheritable (
         "frameDamping:Inherited", "intrinsic translational damping",
         DEFAULT_FRAME_DAMPING);
      myProps.addInheritable (
         "rotaryDamping:Inherited", "intrinsic rotational damping",
         DEFAULT_ROTARY_DAMPING);
      myProps.addInheritable (
         "pointDamping:Inherited", "intrinsic translational damping",
         DEFAULT_POINT_DAMPING);
      myProps.addInheritable (
         "penetrationTol:Inherited", "collision penetration tolerance",
         DEFAULT_PENETRATION_TOL);
      myProps.addInheritable (
         "rotaryLimitTol:Inherited", "rotary limit tolerance",
         DEFAULT_ROTARY_LIMIT_TOL);
      myProps.add("staticTikhonovFactor", "Tikhonov regularization factor for static solves", 0);
      myProps.add("staticIncrements", "Number of load increments for incremental static solves", 20);
      myProps.addInheritable (
         "excitationColor", "color of activated muscles", null);
      myProps.addInheritable (
         "maxColoredExcitation",
         "excitation value for maximum colored excitation", 1.0, "[0,1]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MechModel() {
      this (null);
   }

   public void clear() {
      doclear();
   }

   protected void doclear() {
      setDefaultValues();
//      for (int i=0; i<numComponents(); i++) {
//         ModelComponent c = get (i);
//         if (c instanceof ComponentList &&
//             c != myCollisionManagerOld && c != myCollisionManager) {
//            ((ComponentList<?>)c).removeAll();
//         }
//      }
//      myCollisionManagerOld.clear();
      myCollisionManager.clear();
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myGravity = new Vector3d (DEFAULT_GRAVITY);
      myGravityMode = PropertyMode.Inherited;
      myPointDamping = DEFAULT_POINT_DAMPING;
      myPointDampingMode = PropertyMode.Inherited;
      myFrameDamping = DEFAULT_FRAME_DAMPING;
      myFrameDampingMode = PropertyMode.Inherited;
      myRotaryDamping = DEFAULT_ROTARY_DAMPING;
      myRotaryDampingMode = PropertyMode.Inherited;
      myMinBound = null;
      myMaxBound = null;
      myExcitationColor = null;
      myMaxColoredExcitation = 1.0;
      setMaxStepSize (0.01);
   }
   
   public MechModel (String name) {
      super (name);
      myParticles = new PointList<Particle> (Particle.class, "particles", "p");
      myPoints = new PointList<Point> (Point.class, "points", "ps");
      myModels =
         new ComponentList<MechSystemModel> (
            MechSystemModel.class, "models", "m");
      myAxialSprings =
         new AxialSpringList<AxialSpring> (
            AxialSpring.class, "axialSprings", "as");
      myMultiPointSprings =
         new PointSpringList<MultiPointSpring> (
            MultiPointSpring.class, "multiPointSprings", "ms");
      myFrameSprings =
         new ComponentList<FrameSpring> (
            FrameSpring.class, "frameSprings", "fs");
      myForceEffectors = 
         new ComponentList<ForceComponent> (
            ForceComponent.class, "forceEffectors", "f");
      myRigidBodies =
         new ComponentList<RigidBody> (RigidBody.class, "rigidBodies", "r");
      myFrames =
         new ComponentList<Frame> (Frame.class, "frames", "fr");
      myMeshBodies =
         new ComponentList<MeshComponent> (
            MeshComponent.class, "meshBodies", "mb");
      myFrameMarkers =
         new PointList<FrameMarker> (FrameMarker.class, "frameMarkers", "k");
      myFrameMarkers.setPointDamping (0);

      myConnectors =
         new ComponentList<BodyConnector> (
            BodyConnector.class, "bodyConnectors", "c");
      myConstrainers =
         new RenderableComponentList<ConstrainerBase> (
            ConstrainerBase.class, "particleConstraints", "pc");

      myAttachments =
         new ComponentList<DynamicAttachmentComp> (
            DynamicAttachmentComp.class, "attachments", "a");

      myExciterList =
         new ComponentList<MuscleExciter> (MuscleExciter.class, "exciters", "e");
      myRenderables =
         new RenderableComponentList<RenderableComponent> (
            RenderableComponent.class, "renderables", "re");

      //myCollisionManagerOld = new CollisionManagerOld(this);
      //myCollisionManagerOld.setName ("collisionManagerOld");
      myCollisionManager = new CollisionManager(this);
      myCollisionManager.setName ("collisionManager");

      addFixed (myRenderables);
      addFixed (myModels);
      addFixed (myParticles);
      addFixed (myPoints);
      addFixed (myRigidBodies);
      addFixed (myFrames);
      addFixed (myMeshBodies);
      addFixed (myFrameMarkers);
      addFixed (myConnectors);
      addFixed (myConstrainers);
      addFixed (myAttachments);
      addFixed (myAxialSprings);
      addFixed (myMultiPointSprings);
      addFixed (myFrameSprings);
      addFixed (myForceEffectors);
      addFixed (myExciterList);

      addFixed (myCollisionManager);         
 
      // set these to -1 so that they will be computed automatically
      // when their "get" methods are called.
      //myCollisionManager.setRigidPointTol (-1);
      //myCollisionManager.setRigidRegionTol (-1);
   }

   protected void updateHardwiredLists() {
      // TODO
   }

   /**
    * Queries whether frame marker rotational effects are added to
    * the stiffness matrix.
    *
    * @return {@code true} if frame marker rotational are added to
    * the stiffness matrix.
    */
   public boolean getAddFrameMarkerStiffness () {
      return myAddFrameMarkerStiffness;
   }

   /**
    * Sets whether frame marker rotational effects are added to the stiffness
    * matrix. This is {@code false} by default. If enabled, it will cause the
    * stiffness matrix to be asymmetric.
    *
    * @param enable if {@code true}, causes frame marker rotational to
    * be added to the stiffness matrix.
    */
   public void setAddFrameMarkerStiffness (boolean enable) {
      myAddFrameMarkerStiffness = enable;
   }

   /**
    * Returns an estimate of the radius of components of this MechModel.
    * Can be used to compute default tolerances.
    * 
    * @return estimated model radius
    */
   public double getRadius() {
      return RenderableUtils.getRadius (this);      
   }
   
   /**
    * Sets the default distance by which collidable bodies are allowed to
    * interpenetrate each other in order to preserve contact stability. If
    * specified as a negative number, the value will be set to a default value
    * based on the overall size of this <code>MechModel</code>.
    *
    * @param tol desired penetration tolerance
    */
   public void setPenetrationTol (double tol) {
      if (tol < 0) {
         tol = computeDefaultPenetrationTol();
      }
      myPenetrationTol = tol;
      myPenetrationTolMode =
         PropertyUtils.propagateValue (
            this, "penetrationTol", tol, myPenetrationTolMode);
   }

   /**
    * Returns the default distance by which collidable bodies are allowed to
    * interpenetrate each other in order to preserve contact stability.
    *
    * @return penetration tolerance
    */
   public double getPenetrationTol () {
      return myPenetrationTol;
   }

   public PropertyMode getPenetrationTolMode() {
      return myPenetrationTolMode;
   }

   public void setPenetrationTolMode (PropertyMode mode) {
      myPenetrationTolMode =
         PropertyUtils.setModeAndUpdate (
            this, "penetrationTol", myPenetrationTolMode, mode);
   }

   public void setPenetrationTolIfNecessary () {
      // If myPenetrationTolMode is Inherited, that means that the current
      // value was not set explicitly, and so we should compute an proper
      // default value based on the current MechModel radius an propagate this
      // down to descendents.
      //
      // Note also that if myPenetrationTolMode is Inherited, the property
      // system will have tried to set penetrationTol to the default value of
      // -1, but setPenetrationTol() have get turned this into a radius-based
      // default value. However, that value may be incorrect since it may have
      // been computed before all of the MechModel components have been
      // added. So we recompute the default value here.
      if (myPenetrationTolMode == PropertyMode.Inherited) {
         setPenetrationTol (-1);
      }
   }

   /**
    * Computes a default value for the penetration tolerance based
    * on the radius of this MechModel.
    *
    * @return default penetration tolerance
    */
   public double computeDefaultPenetrationTol() {
      double tol = 0.0001;
      double radius = RenderableUtils.getRadius (this);
      if (radius != 0) {
         tol = 1e-4*radius;
      }
      return tol;
   }

   static double getDefaultPenetrationTol (
      ModelComponent comp, double defaultTol) {

      double tol;
      MechModel mech = MechModel.topMechModel (comp);
      if (mech != null) {
         tol = mech.computeDefaultPenetrationTol();
      }
      else {
         tol = defaultTol;
      }
      return tol;
   }

   /**
    * Sets the rotary limit tolerance for this MechModel.
    *
    * @param tol new rotary limit tolerance 
    */
   public void setRotaryLimitTol (double tol) {
      myRotaryLimitTol = tol;
      myRotaryLimitTolMode =
         PropertyUtils.propagateValue (
            this, "rotaryLimitTol", tol, myRotaryLimitTolMode);
   }

   /**
    * Queries the rotary limit tolerance for this MechModel.
    * 
    * @return rotary limit tolerance 
    */
   public double getRotaryLimitTol () {
      return myRotaryLimitTol;
   }

   public PropertyMode getRotaryLimitTolMode() {
      return myRotaryLimitTolMode;
   }

   public void setRotaryLimitTolMode (PropertyMode mode) {
      myRotaryLimitTolMode =
         PropertyUtils.setModeAndUpdate (
            this, "rotaryLimitTol", myRotaryLimitTolMode, mode);
   }

   public Color getExcitationColor() {
      if (myExcitationColor == null) {
         return null;
      }
      else {
         return new Color (
            myExcitationColor[0], myExcitationColor[1], myExcitationColor[2]);
      }
   }
   public void setExcitationColor (Color color) {
      if (color == null) {
         myExcitationColor = null;
      }
      else {
         myExcitationColor = color.getRGBColorComponents(null);
      }
      myExcitationColorMode =
         PropertyUtils.propagateValue (
            this, "excitationColor", color, myExcitationColorMode);
   }

   public PropertyMode getExcitationColorMode() {
      return myExcitationColorMode;
   }

   public void setExcitationColorMode (PropertyMode mode) {
      myExcitationColorMode =
         PropertyUtils.setModeAndUpdate (
            this, "excitationColor", myExcitationColorMode, mode);
   }

   public double getMaxColoredExcitation() {
      return myMaxColoredExcitation;
   }

   public void setMaxColoredExcitation (double excitation) {
      myMaxColoredExcitation = excitation;
      myMaxColoredExcitationMode =
         PropertyUtils.propagateValue (
            this, "maxColoredExcitation", excitation, myMaxColoredExcitationMode);
   }

   public PropertyMode getMaxColoredExcitationMode() {
      return myMaxColoredExcitationMode;
   }

   public void setMaxColoredExcitationMode (PropertyMode mode) {
      myMaxColoredExcitationMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxColoredExcitation", myMaxColoredExcitationMode, mode);
   }

   /**
    * Sets the global friction coefficient in the collision manager for this
    * MechModel. Collision behaviors will inherit this value if their initial
    * friction coefficent was undefined (i.e., was specified with a value less
    * than 0).
    *
    * @param mu global friction coefficient
    */
   public void setFriction (double mu) {
      myCollisionManager.setFriction (mu);
   }

   /**
    * Returns the global friction coefficient in the collision manager for this
    * MechModel.
    *
    * @return global friction coefficient
    */
   public double getFriction () {
      return myCollisionManager.getFriction();
   }

   /** 
    * Specifies the default collision behavior for all primary collidable group
    * pairs (Rigid-Rigid, Rigid-Deformable, Deformable-Deformable,
    * Deformable-Self) associated with this MechModel.  This is a convenience
    * wrapper for {@link #setDefaultCollisionBehavior(CollisionBehavior)
    * setDefaultCollisionBehavior(behavior)}.
    * 
    * @param enabled if true, enables collisions
    * @param mu friction coefficient (ignored if enabled is false). If less
    * than zero, the value is undefined and will be inherited from the global
    * setting in this MechModel's collision manager.
    * @see #setFriction
    * @see #getFriction
    */
   public void setDefaultCollisionBehavior (boolean enabled, double mu) {
      setDefaultCollisionBehavior (new CollisionBehavior (enabled, mu));
   }

   /** 
    * Specifies a default collision behavior for all primary collidable group
    * pairs (Rigid-Rigid, Rigid-Deformable, Deformable-Deformable,
    * Deformable-Self) associated with this MechModel. The specified behavior
    * is copied into the internal behavior settings for these pairs.
    *
    * <p>This method is equivalent to calling
    * <pre>
    * setDefaultCollisionBehavior (Collidable.All, Collidable.All, behavior)
    * </pre>
    * 
    * @param behavior desired collision behavior
    */
   public void setDefaultCollisionBehavior (CollisionBehavior behavior) {
      myCollisionManager.setDefaultBehavior (
         Collidable.All, Collidable.All, behavior);
   }

   /** 
    * Gets the default collision behavior for a specified pair of primary
    * collidable groups.  Allowed collision groups are {@link Collidable#Rigid}
    * and {@link Collidable#Deformable}. In addition, the group {@link
    * Collidable#Self} can be paired with {@link Collidable#Deformable} to
    * obtain the default behavior for deformable self collisions; {@link
    * Collidable#Self} cannot be paired with other groups.
    * 
    * @param group0 first generic collidable group
    * @param group1 second generic collidable group
    * @return default collision behavior for the indicted collidable groups.
    */
   public CollisionBehavior getDefaultCollisionBehavior (
      Collidable.Group group0, Collidable.Group group1) {
      return myCollisionManager.getDefaultBehavior (group0, group1);
   }

   /**
    * Sets the default collision behavior, for this MechModel, for a specified
    * pair of collidable groups.  Allowed collidable groups include the primary
    * groups {@link Collidable#Rigid} and {@link Collidable#Deformable}, as
    * well as the composite groups {@link Collidable#AllBodies} (rigid and
    * deformable) and {@link Collidable#All} (all bodies plus self collision).
    * In addition, the group {@link Collidable#Self} can be paired with {@link
    * Collidable#Deformable}, {@link Collidable#AllBodies} or {@link
    * Collidable#All} to set the default self-collision behavior for deformable
    * compound bodies. {@link Collidable#Self} cannot be paired with {@link
    * Collidable#Self} or {@link Collidable#Rigid}.
    *
    * <p>This method works by setting the behaviors for the appropriate primary
    * collidable group pairs (Rigid-Rigid, Rigid-Deformable,
    * Deformable-Deformable, Deformable-Self) implied by the specified
    * groups.
    *
    * @param group0 first generic collidable group
    * @param group1 second generic collidable group
    * @param behavior desired collision behavior
    */
   public void setDefaultCollisionBehavior (
      Collidable.Group group0, Collidable.Group group1, 
      CollisionBehavior behavior) {

      myCollisionManager.setDefaultBehavior (group0, group1, behavior);
   }

   /**
    * Sets the default collision behavior, for this MechModel, for a specified
    * pair of generic collidable groups.  This is a convenience wrapper for
    * {@link
    * #setDefaultCollisionBehavior(Collidable.Group,Collidable.Group,CollisionBehavior)
    * setDefaultCollisionBehavior(group0,group1,behavior)}, where a default
    * <code>CollisionBehavior</code> object is created and set to reflect the
    * specified enabled and friction settings.
    *
    * @param group0 first generic collidable group
    * @param group1 second generic collidable group
    * @param enabled if true, enables collisions
    * @param mu friction coefficient (ignored if enabled is false). If
    * less than zero, the value is undefined and will be inherited from the global
    * setting in this MechModel's collision manager.
    * @see #setFriction
    * @see #getFriction
    */
   public void setDefaultCollisionBehavior (
      Collidable.Group group0, Collidable.Group group1, 
      boolean enabled, double mu) {

      setDefaultCollisionBehavior (
         group0, group1, new CollisionBehavior (enabled, mu));
   }

   /** 
    * Enables or disables collisions for a specified pair of collidables
    * <code>c0</code> and <code>c1</code>, overriding the default behavior or
    * any behavior specified for the pair with previous
    * <code>setBehavior</code> calls. This is a convenience wrapper for {@link
    * #setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setCollisionBehavior(c0,c1,behavior)}, where a default
    * <code>CollisionBehavior</code> object is created with the specified
    * enabled setting and a friction coefficient of 0.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @param enabled if true, enables collisions
    * @return collision behavior object that was created and set
    */
   public CollisionBehavior setCollisionBehavior (
      Collidable c0, Collidable c1, boolean enabled) {

      CollisionBehavior behav = new CollisionBehavior (enabled, /*mu=*/0);
      setCollisionBehavior (c0, c1, behav);
      return behav;
   }

   /** 
    * Sets the collision behavior for a specified pair of collidables
    * <code>c0</code> and <code>c1</code>, overriding the default behavior and
    * any behavior specified for the pair with previous
    * <code>setBehavior</code> calls. This is a convenience wrapper for {@link
    * #setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setCollisionBehavior(c0,c1,behavior)}, where a default
    * <code>CollisionBehavior</code> object is created and set to reflect the
    * specified enabled and friction settings.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @param enabled if true, enables collisions
    * @param mu friction coefficient (ignored if enabled is false). If less
    * than zero, the value is undefined and will be inherited from the global
    * setting in this MechModel's collision manager.
    * setting for this MechModel.
    * @return {@link CollisionBehavior} object describing the collision
    * behavior
    * @see #setFriction
    * @see #getFriction
    */
   public CollisionBehavior setCollisionBehavior (
      Collidable c0, Collidable c1, boolean enabled, double mu) {

      CollisionBehavior behav = new CollisionBehavior (enabled, mu);
      setCollisionBehavior (c0, c1, behav);
      return behav;
   }

   /** 
    * Sets the collision behavior for a specified pair of collidables
    * <code>c0</code> and <code>c1</code>, overriding the default behavior and
    * any behavior specified for the pair with previous
    * <code>setBehavior</code> calls. The behavior specified by this method
    * will be applied only among collidables for which this
    * <code>MechModel</code> is the lowest common model.
    *
    * <p>Since behaviors are added to the collision manager as sub-components,
    * the specified behavior cannot be currently set and in particular can not
    * be reused in other <code>setCollisionBehavior</code> calls. If reuse
    * is desired, the behavior should be copied:
    * <pre>
    *    CollisionBehavior behav = new CollisionBehavior();
    *    behav.setDrawIntersectionContours (true); 
    *    mesh.setCollisionBehavior (col0, col1, behav);
    *    behav = new CollisionBehavior(behav);
    *    mesh.setCollisionBehavior (col2, col3, behav); // OK
    * </pre>
    *
    * <p>There are restrictions on what pair of collidables can be
    * specified. The first collidable must be a specific collidable object. The
    * second may be a collidable group, such as {@link Collidable#Rigid} {@link
    * Collidable#All}, or {@link Collidable#Self}. Self-collision is specified
    * either using the group {@link Collidable#Self} or by specifying <code>c0
    * == c1</code>. If self-collision is specified, then <code>c0</code> must
    * be deformable and must also be a compound collidable (since
    * self-intersection is currently only supported among the sub-collidables
    * of a compound collidable).
    *
    * <p>If <code>c0</code> and/or <code>c1</code> are specific collidables, then
    * they must both be contained within the component hierarchy of this
    * <code>MechModel</code>. In addition, if both <code>c0</code> and
    * <code>c1</code> are specific collidables, then one cannot be a
    * sub-collidable of the other, and this <code>MechModel</code> must be the
    * lowest common model containing both of them.
    *
    * <p> If <code>c0</code> or <code>c1</code> are compound collidables, then
    * the behavior is applied to all appropriate pairs of their
    * sub-collidables.  If self-collision is specified, then the behavior is
    * applied among all sub-collidables whose {@link Collidable#getCollidable
    * getCollidable()} method returns <code>Colidability.ALL</code> or
    * <code>Colidability.INTERNAL</code>.
    *
    * <p>This method works by adding the indicated behavior to the collision
    * manager as a sub-component. If a behavior has been previously set for the
    * specified pair, the previous behavior is removed. The behavior can be
    * queried later using {@link #getCollisionBehavior
    * getCollisionBehavior(c0,c1)} and removed using {@link
    * #clearCollisionBehavior clearCollisionBehavior(c0,c1)}.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @param behavior desired collision behavior
    */
   public void setCollisionBehavior (
      Collidable c0, Collidable c1, CollisionBehavior behavior) {
      myCollisionManager.setBehavior (c0, c1, behavior);
   }

   /** 
    * Returns the collision behavior that was previously set using one of the
    * <code>setCollisionBehavior</code> methods.  If no behavior for the
    * indicated pair was set, <code>null</code> is returned.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return specific behavior for this pair of collidables
    */
   public CollisionBehavior getCollisionBehavior (Collidable c0, Collidable c1) {
      return myCollisionManager.getBehavior (c0, c1);
   }

   /** 
    * Clears the collision behavior that was previously set using one of the
    * <code>setCollisionBehavior</code> methods. If no behavior for the
    * indicated pair was set, the method returns <code>false</code>.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return <code>true</code> if the specific behavior had been set
    * and was removed
    */
   public boolean clearCollisionBehavior (Collidable c0, Collidable c1) {
      return myCollisionManager.clearBehavior (c0, c1);
   }

   /** 
    * Clears any collision behaviors that have been set using the
    * <code>setCollisionBehavior</code> methods.
    */
   public void clearCollisionBehaviors () {
      myCollisionManager.clearBehaviors();
   }
   
   /** 
    * Sets and returns a collision response for a specified pair of collidables
    * <code>c0</code> and <code>c1</code>, removing any response that has been
    * previoulsy specified for the same pair. This is a convenience wrapper for
    * {@link #setCollisionResponse(Collidable,Collidable,CollisionResponse)
    * setCollisionResponse(c0,c1,response)}, which creates and returns
    * the required <code>CollisionResponse</code> object.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return collision response object that was created and set
    */
   public CollisionResponse setCollisionResponse (Collidable c0, Collidable c1) {

      CollisionResponse resp = new CollisionResponse ();
      setCollisionResponse (c0, c1, resp);
      return resp;
   }

   /** 
    * Sets the collision response for a specified pair of collidables
    * <code>c0</code> and <code>c1</code>, removing any response that has been
    * previoulsy specified for the same pair. At every subsequent integration
    * step, the response object will be updated to contain the current
    * collision information for the collidable pair. Since responses are added
    * to the collision manager as sub-components, the specified response cannot
    * be currently set.
    *
    * <p>There are restrictions on what pair of collidables can be
    * specified. The first collidable must be a specific collidable object. The
    * second may be a collidable group, such as {@link Collidable#Rigid} {@link
    * Collidable#All}, or {@link Collidable#Self}. Self-collision is specified
    * either using the group {@link Collidable#Self} or by specifying <code>c0
    * == c1</code>. If self-collision is specified, then <code>c0</code> must
    * be deformable and must also be a compound collidable (since
    * self-intersection is currently only supported among the sub-collidables
    * of a compound collidable).
    *
    * <p>If <code>c0</code> and/or <code>c1</code> are specific collidables, then
    * they must both be contained within the component hierarchy of this
    * <code>MechModel</code>.
    *
    * <p>If <code>c0</code> or <code>c1</code> are compound collidables, then
    * the response is collected for all appropriate pairs of their
    * sub-collidables.  If self-collision is specified, then the response is
    * collected for all sub-collidables whose {@link Collidable#getCollidable
    * getCollidable()} method returns <code>Colidability.ALL</code> or
    * <code>Colidability.INTERNAL</code>.
    *
    * <p>This method works by adding the indicated response to the collision
    * manager as a sub-component. If a response has been previously set for the
    * specified pair, the previous response is removed. The response can be
    * queried later using {@link #getCollisionResponse
    * getCollisionResponse(c0,c1)} and removed using {@link
    * #clearCollisionResponse clearCollisionResponse(c0,c1)}.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @param response desired collision response
    */
   public void setCollisionResponse (
      Collidable c0, Collidable c1, CollisionResponse response) {
      myCollisionManager.setResponse (c0, c1, response);
   }

   /** 
    * Returns the collision response that was previously set using one of the
    * <code>setCollisionResponse</code> methods.  If no response for the
    * indicated pair was set, <code>null</code> is returned.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return specific response for this pair of collidables
    */
   public CollisionResponse getCollisionResponse (Collidable c0, Collidable c1) {
      return myCollisionManager.getResponse (c0, c1);
   }

   /** 
    * Clears the collision response that was previously set using one of the
    * <code>setCollisionResponse</code> methods. If no response for the
    * indicated pair was set, the method returns <code>false</code>.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return <code>true</code> if the specific response had been set
    * and was removed
    */
   public boolean clearCollisionResponse (Collidable c0, Collidable c1) {
      return myCollisionManager.clearResponse (c0, c1);
   }

   /** 
    * Clears any collision responses that have been set using the
    * <code>setCollisionResponse()</code> methods.
    */
   public void clearCollisionResponses () {
      myCollisionManager.clearResponses();
   }

   /**
    * Returns the behavior that controls collisions for a pair of specific
    * collidables <code>c0</code> and <code>c1</code>. This will be determined
    * by the lowest <code>MechModel</code> which contains both <code>c0</code>
    * and <code>c1</code>, and be either a default behavior, or an override
    * behavior specified will a <code>setBehavior</code> call. In some cases,
    * if <code>c0</code> and/or <code>c1</code> are not collidable, or if one
    * or both are compound collidables with different behaviors for their
    * sub-collidables (see below), no unique controlling behavior will exist
    * and this method will return <code>null</code>.
    *
    * <p>Both <code>c0</code> and <code>c1</code> must be specific collidables;
    * collidable groups (such as {@link Collidable#Rigid} or {@link
    * Collidable.Group}) are not allowed.
    *
    * <p>If <code>c0</code> or <code>c1</code> are compound collidables, then
    * if the same acting behavior is found among all pairs of sub-collidables
    * for which collisions are permitted, that behavior is returned; otherwise,
    * <code>null</code> is returned. In particular, if <code>c0</code> equals
    * <code>c1</code>, then this method searches for the same collision
    * behavior among all sub-collidables of <code>c0</code> for which
    * self-intersection is permitted (i.e., those whose {@link
    * Collidable#getCollidable getCollidable()} method returns
    * <code>Colidability.ALL</code> or <code>Colidability.INTERNAL</code>).
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return acting behavior for this pair of collidables, or
    * <code>null</code> if such behavior exists.
    */
   public CollisionBehavior getActingCollisionBehavior (
      Collidable c0, Collidable c1) {
      return myCollisionManager.getActingBehavior (c0, c1);
   }

   public void getCollidables (List<Collidable> list, int level) {
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.getCollidables (list, level+1);
      }
      recursivelyGetLocalComponents (this, list, Collidable.class);
      //recursivelyGetCollidables (this, list, level);
   }      

   /**
    * Return a list of all collidable bodies located within this
    * MechModel (including sub-MechModels).
    */
   ArrayList<CollidableBody> getCollidableBodies() {
      
      ArrayList<Collidable> collidables = new ArrayList<Collidable>();
      getCollidables (collidables, /*level=*/0);
      ArrayList<CollidableBody> cbodies = new ArrayList<CollidableBody>();
      for (Collidable c : collidables) {
         if (c instanceof CollidableBody) {
            cbodies.add ((CollidableBody)c);
         }
      }
      return cbodies;
   }
   
   /**
    * Update, if necessary, the indices of all collidable bodies. These
    * indices are used by the CollisionManager(s) to determine an
    * unambiguous ordering for pairs of collidable bodies. 
    */
   void updateCollidableBodyIndices() {
      if (myCollidableBodiesNeedIndexing) {
         ArrayList<CollidableBody> cbodies = getCollidableBodies();
         for (int i=0; i<cbodies.size(); i++) {
            cbodies.get(i).setCollidableIndex(i);
         }
         myCollidableBodiesNeedIndexing = false;
      }
   }

   // public void getTopCollidables (List<Collidable> list, int level) {
   //    recursivelyGetTopLocalComponents (this, list, Collidable.class);
   // } 
   

   // /**
   //  * Creates and returns a collision response object that contains information
   //  * about all the current collisions between a <code>target</code> collidable
   //  * and one or more <code>source</code> collidables.
   //  *
   //  * <p>If <code>target</code> and <code>source</code> are the same, then the
   //  * response will provide information on self-collisions within
   //  * <code>target</code>, which is the same as specifying <code>source =
   //  * Collidable.Self</code>. At present, self-collision is supported only for
   //  * deformable compound collidables; if this is not the case, then the
   //  * collision response object will be empty.
   //  *
   //  * <p>If <code>source</code> is set to <code>Collidable.All</code>, then the
   //  * collision response will contain information for all collisions involving
   //  * the <code>target</code>, including self-collisions if appropriate.
   //  *
   //  * @param target target collidable. Must be a specific collidable.
   //  * @param source source collidable(s). May be a specific collidable
   //  * or a colliable group.
   //  * @return collision response object for the specified target and source
   //  */
   // public CollisionResponse getCollisionResponse (
   //    Collidable target, Collidable source) {

   //    if (useNewCollisionManager) {
   //       return myCollisionManager.getCollisionResponse (target, source);
   //    }
   //    else {
   //       return null;
   //    }
   // }

//   public CollisionManagerOld getCollisionManagerOld() {
//      return null;
//   }
//
   public CollisionManager getCollisionManager() {
      return myCollisionManager;
   }

   public Integrator getIntegrator() {
      return myIntegrator;
   }
   
   public void setStaticTikhonovFactor(double eps) {
      if (mySolver != null) {
         mySolver.setStaticTikhonovFactor(eps);
      }
   }
   
   public double getStaticTikhonovFactor() {
      if (mySolver != null) {
         return mySolver.getStaticTikhonovFactor();
      }
      return 0;
   }
   
   public void setStaticIncrements(int n) {
      if (mySolver != null) {
         mySolver.setStaticIncrements(n);
      }
   }
   
   public int getStaticIncrements() {
      if (mySolver != null) {
         return mySolver.getStaticIncrements();
      }
      return 0;
   }

   public PointList<Particle> particles() {
      return myParticles;
   }

   public void addParticle (Particle p) {
      myParticles.add (p);
   }

   public void removeParticle (Particle p) {
      myParticles.remove (p);
   }

   public void clearParticles() {
      myParticles.removeAll();
   }

   public PointList<Point> points() {
      return myPoints;
   }

   public void addPoint (Point p) {
      myPoints.add (p);
   }

   public void removePoint (Point p) {
      myPoints.remove (p);
   }

   public void clearPoints() {
      myPoints.removeAll();
   }

   public RenderableComponentList<AxialSpring> axialSprings() {
      return myAxialSprings;
   }

   public RenderableComponentList<MultiPointSpring> multiPointSprings() {
      return myMultiPointSprings;
   }

   public ComponentListView<FrameSpring> frameSprings() {
      return myFrameSprings;
   }

   public ComponentListView<ForceComponent> forceEffectors() {
      return myForceEffectors;
   }

   public double getPointDamping() {
      return myPointDamping;
   }

   public void setPointDamping (double d) {
      myPointDamping = d;
      myPointDampingMode =
         PropertyUtils.propagateValue (
            this, "pointDamping", d, myPointDampingMode);
   }

   public PropertyMode getPointDampingMode() {
      return myPointDampingMode;
   }

   public void setPointDampingMode (PropertyMode mode) {
      myPointDampingMode =
         PropertyUtils.setModeAndUpdate (
            this, "pointDamping", myPointDampingMode, mode);
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

   /* ------- gravity stuff ---------- */

   public Vector3d getGravity() {
      return new Vector3d(myGravity);
   }

   public void setGravity (Vector3d g) {
      myGravity.set (g);
      myGravityMode = PropertyUtils.propagateValue (
         this, "gravity", g, myGravityMode);
   }

   public void setGravity (double gx, double gy, double gz) {
      setGravity (new Vector3d (gx, gy, gz));
   }

   public PropertyMode getGravityMode() {
      return myGravityMode;
   }

   public void setGravityMode (PropertyMode mode) {
      myGravityMode =
         PropertyUtils.setModeAndUpdate (
            this, "gravity", myGravityMode, mode);
   }

   /* ------ axial spring stuff ------- */

   public void addAxialSpring (AxialSpring s) {
      myAxialSprings.add (s);
   }

   public void attachAxialSpring (Point pnt0, Point pnt1, AxialSpring s) {
      s.setFirstPoint (pnt0);
      s.setSecondPoint (pnt1);
      myAxialSprings.add (s);
   }

   public void removeAxialSpring (AxialSpring s) {
      myAxialSprings.remove (s);
   }

   public void clearAxialSprings() {
      myAxialSprings.removeAll();
   }

   public void addMultiPointSpring (MultiPointSpring s) {
      myMultiPointSprings.add (s);
   }

   public void removeMultiPointSpring (MultiPointSpring s) {
      myMultiPointSprings.remove (s);
   }

   public void clearMultiPointSprings() {
      myMultiPointSprings.removeAll();
   }

   public void addFrameSpring (FrameSpring s) {
      myFrameSprings.add (s);
   }

   public void attachFrameSpring (Frame frameA, Frame frameB, FrameSpring s) {
      s.setFrameA (frameA);
      s.setFrameB (frameB);
      myFrameSprings.add (s);
   }

   public void removeFrameSpring (FrameSpring s) {
      myFrameSprings.remove (s);
   }

   public void clearFrameSprings() {
      myFrameSprings.removeAll();
   }

   public void addForceEffector (ForceComponent fe) {
      myForceEffectors.add (fe);
   }

   public void removeForceEffector (ForceEffector fe) {
      myForceEffectors.remove (fe);
   }

   public void clearForceEffectors() {
      myForceEffectors.removeAll();
   }

   public void addMuscleExciter (MuscleExciter mex) {
      // XXX check to see if existing targets are contained ...
      myExciterList.add (mex);
   }

   public boolean removeMuscleExciter (MuscleExciter mex) {
      return myExciterList.remove (mex);
   }

   public ComponentList<MuscleExciter> getMuscleExciters() {
      return myExciterList;
   }

   public ComponentListView<RigidBody> rigidBodies() {
      return myRigidBodies;
   }

   public void addRigidBody (RigidBody rb) {
      myRigidBodies.add (rb);
   }

   public void removeRigidBody (RigidBody rb) {
      myRigidBodies.remove (rb);
   }

   public void clearRigidBodies() {
      myRigidBodies.removeAll();
   }

   public ComponentListView<Frame> frames() {
      return myFrames;
   }

   public void addFrame (Frame rb) {
      myFrames.add (rb);
   }

   public void removeFrame (Frame rb) {
      myFrames.remove (rb);
   }

   public void clearFrames() {
      myFrames.removeAll();
   }

   public ComponentListView<MeshComponent> meshBodies() {
      return myMeshBodies;
   }

   public void addMeshBody (MeshComponent rb) {
      myMeshBodies.add (rb);
   }

   public void removeMeshBody (MeshComponent rb) {
      myMeshBodies.remove (rb);
   }

   public void clearMeshBodies() {
      myMeshBodies.removeAll();
   }

   public void addFrameMarker (FrameMarker mkr, Frame frame, Point3d loc) {
      mkr.setFrame (frame);
      if (loc != null) {
         mkr.setLocation (loc);
         mkr.setRefPos (mkr.getPosition ());
      }
      addFrameMarker (mkr);
   }
   
   /**
    * Creates and add a frame marker to a particular frame, with a location
    * specified relative to the frame's coordinate frame
    * 
    * @param frame frame object to add a marker to
    * @param loc marker location in frame coordinates
    * @return the created marker
    */
   public FrameMarker addFrameMarker (Frame frame, Point3d loc) {
      FrameMarker mkr = new FrameMarker(frame, loc);
      addFrameMarker(mkr);
      return mkr;
   }

   /**
    * Creates and add a frame marker to a particular frame, with a location
    * specified in world coordinates
    * 
    * @param frame frame object to add a marker to
    * @param pos marker position in world coordinates
    * @return the created marker
    */
   public FrameMarker addFrameMarkerWorld (Frame frame, Point3d pos) {
      FrameMarker mkr = new FrameMarker ();
      mkr.setFrame (frame);
      mkr.setWorldLocation (pos);
      addFrameMarker(mkr);
      return mkr;
   }

   public void addFrameMarker (FrameMarker mkr) {
      myFrameMarkers.add (mkr);
   }

   public void removeFrameMarker (FrameMarker mkr) {
      myFrameMarkers.remove (mkr);
   }

   public void clearFrameMarkers() {
      myFrameMarkers.removeAll();
   }

   public RenderableComponentList<FrameMarker> frameMarkers() {
      return myFrameMarkers;
   }

   /* ----- Rigid Body Constraints ------ */

   public ComponentListView<BodyConnector> bodyConnectors() {
      return myConnectors;
   }

   public void addBodyConnector (BodyConnector c) {
      myConnectors.add (c);
   }

   public void removeBodyConnector (BodyConnector c) {
      myConnectors.remove (c);
   }

   public void clearBodyConnectors() {
      myConnectors.removeAll();
   }

   /* ----- Particle Constraints ------ */

   public ComponentListView<ConstrainerBase> constrainers() {
      return myConstrainers;
   }

   public void addConstrainer (ConstrainerBase c) {
      myConstrainers.add (c);
   }

   public void removeConstrainer (ConstrainerBase c) {
      myConstrainers.remove (c);
   }

   public void clearConstrainers() {
      myConstrainers.removeAll();
   }

   public ComponentListView<RenderableComponent> renderables() {
      return myRenderables;
   }

   public void addRenderable (RenderableComponent rb) {
      myRenderables.add (rb);
   }

   public void removeRenderable (RenderableComponent rb) {
      myRenderables.remove (rb);
   }

   public void clearRenderables() {
      myRenderables.removeAll();
   }

   /* ----- Sub models ------ */

   public ComponentListView<MechSystemModel> models() {
      return myModels;
   }

   public void addModel (MechSystemModel m) {
      myModels.add (m);
   }

   public boolean removeModel (MechSystemModel m) {
      return myModels.remove (m);
   }

   public void clearModels() {
      myModels.removeAll();
   }

   /** 
    * Attaches a particle to a PointAttachable component, using both
    * component's current position state. The point may be relocated if
    * this is necessary to create an attachment.
    * 
    * @param p1 Point to connect. Must currently be a particle.
    * @param comp Component to attach the particle to.
    */
   public void attachPoint (Point p1, PointAttachable comp) {
      if (p1.getAttachment() instanceof ModelComponent) {
         throw new IllegalArgumentException (
            "point is already attached via an attachment component");
      }
      PointAttachment ax = comp.createPointAttachment (p1);
      if (DynamicAttachmentWorker.containsLoop (ax, p1, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      addAttachment (ax);
   }

   public void attachPoint (Point p1, RigidBody body, Point3d loc) {
      if (p1.getAttachment() instanceof ModelComponent) {
         throw new IllegalArgumentException (
            "point is already attached via an attachment component");
      }
      PointFrameAttachment rbax = new PointFrameAttachment (body, p1, loc);
      if (DynamicAttachmentWorker.containsLoop (rbax, p1, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      addAttachment (rbax);
   }

   public boolean detachPoint (Point p1) {
      DynamicAttachment at = p1.getAttachment();
      if (at instanceof DynamicAttachmentComp) {
         DynamicAttachmentComp ac = (DynamicAttachmentComp)at;
         if (ac.getParent() == myAttachments) {
            removeAttachment (ac);
            return true;
         }
      }
      return false;
   }

    /** 
    * Attaches a frame to a FrameAttachable component, with the
    * initial pose of the frame described by <code>TFW</code>usin
    * component's current position state. The frame may be relocated
    * if this is necessary to create an attachment.
    * 
    * @param frame Frame to connect.
    * @param comp component to attach the frame to.
    * @param TFW initial desired pose of the frame, in world coordinates 
    */
   public void attachFrame (
      Frame frame, FrameAttachable comp, RigidTransform3d TFW) {
      if (frame.isAttached()) {
         throw new IllegalArgumentException ("frame is already attached");
      }
      FrameAttachment ax = comp.createFrameAttachment (frame, TFW);
      if (DynamicAttachmentWorker.containsLoop (ax, frame, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      addAttachment (ax);
   }

   /** 
    * Attaches a frame to a FrameAttachable component, using each
    * component's current position state. The frame may be relocated
    * if this is necessary to create an attachment.
    * 
    * @param frame Frame to connect.
    * @param comp component to attach the frame to.
    */
   public void attachFrame (Frame frame, FrameAttachable comp) {
      attachFrame (frame, comp, frame.getPose());
   }

   public boolean detachFrame (Frame frame) {
      DynamicAttachment at = frame.getAttachment();
      if (at instanceof DynamicAttachmentComp) {
         DynamicAttachmentComp ac = (DynamicAttachmentComp)at;
         if (ac.getParent() == myAttachments) {
            removeAttachment (ac);
            return true;
         }
      }
      return false;
   }

   public ComponentList<DynamicAttachmentComp> attachments() {
      return myAttachments;
   }

   public void addAttachment (DynamicAttachmentComp ax) {
      if (ax.getSlave().isAttached()) {
         throw new IllegalStateException (
            "slave component "+ComponentUtils.getPathName(ax.getSlave())+
            " already attached via "+ ax.getSlave().getAttachment());
      }
      ax.updatePosStates();
      myAttachments.add (ax);
   }

   void removeAttachment (DynamicAttachmentComp ax) {
      myAttachments.remove (ax);
   }

  protected void recursivelyGetAttachments (
      CompositeComponent comp, List<DynamicAttachment> list, int level) {
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof MechSystemModel) {
            ((MechSystemModel)c).getAttachments (list, level+1);
         }
         else if (c instanceof HasAttachments) {
            ((HasAttachments)c).getAttachments (list);
         }
         else if (c instanceof DynamicAttachment) {
            list.add ((DynamicAttachment)c);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyGetAttachments ((CompositeComponent)c, list, level);
         }
      }
   }

   public void getAttachments (List<DynamicAttachment> list, int level) {
      recursivelyGetAttachments (this, list, level);
   }

   /**
    * Recursively create local attachments.
    */
   protected void recursivelyGetLocalAttachments (
      CompositeComponent comp, List<DynamicAttachment> list) {
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof MechSystemModel) {
            // ignore, we onyl want local attachments
         }
         else if (c instanceof HasAttachments) {
            ((HasAttachments)c).getAttachments (list);
         }
         else if (c instanceof DynamicAttachment) {
            list.add ((DynamicAttachment)c);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyGetLocalAttachments ((CompositeComponent)c, list);
         }
      }
   }

   /**
    * Create a list of local attachments (i.e., those that are not contained in
    * a sub-mechmodel).
    */
   protected List<DynamicAttachment> buildLocalAttachments() {
      ArrayList<DynamicAttachment> list = new ArrayList<DynamicAttachment>();
      recursivelyGetLocalAttachments (this, list);
      return list;
   }

   FunctionTimer timer = new FunctionTimer();

   public boolean getAddConstraintForces() {
      return addConstraintForces;
   }

   public void setAddConstraintForces (boolean enable) {
      addConstraintForces = enable;
   }

   public static boolean isActive (DynamicComponent c) {
      return c.isActive();
   }

   protected void recursivelyGetDynamicComponents (
      CompositeComponent comp, 
      List<DynamicComponent> active, 
      List<DynamicComponent> attached,
      List<DynamicComponent> parametric) {
      
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof DynamicComponent) {
            DynamicComponent dm = (DynamicComponent)c;
            if (dm.isActive()) {
               active.add (dm);
            }
            else if (dm.isAttached()) {
               attached.add (dm);
            }
            else {
               parametric.add (dm);
            }
         }
         else if (c instanceof MechSystemModel) {
            ((MechSystemModel)c).getDynamicComponents (
               active, attached, parametric);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyGetDynamicComponents (
               (CompositeComponent)c, active, attached, parametric);
         }
      }
   }

   protected void recursivelyGetDynamicComponents (
      CompositeComponent comp, List<DynamicComponent> comps) { 
      
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof DynamicComponent) {
            comps.add ((DynamicComponent)c);
         }
         else if (c instanceof MechSystemModel) {
            ((MechSystemModel)c).getDynamicComponents (comps);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyGetDynamicComponents (
               (CompositeComponent)c, comps);
         }
      }
   }

   public void getDynamicComponents (
      List<DynamicComponent> active, 
      List<DynamicComponent> attached,
      List<DynamicComponent> parametric) {

      recursivelyGetDynamicComponents (this, active, attached, parametric);
   }

   public void getDynamicComponents (List<DynamicComponent> comps) {
      recursivelyGetDynamicComponents (this, comps);
   }
 
   protected void recursivelyGetConstrainers (
      CompositeComponent comp, List<Constrainer> list, int level) {
      
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof BodyConnector) {
            BodyConnector rbc = (BodyConnector)c;
            if (rbc.isActive()) {
               list.add (rbc);
            }
         }
         else if (c instanceof Constrainer &&
                  !(c instanceof CollisionManager)) {
            list.add ((Constrainer)c);
         }
         else if (c instanceof MechSystemModel) {
            ((MechSystemModel)c).getConstrainers (list, level+1);
         }
         else if (c instanceof CollisionManager) {
            if (level == 0) {
               //((CollisionManager)c).getConstrainers (list);
               list.add ((CollisionManager)c);
            }
         }
         else if (c instanceof CompositeComponent) {
            recursivelyGetConstrainers ((CompositeComponent)c, list, level);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isBilateralStructureConstant() {
      return !myCollisionManager.usesBilateralConstraints();
   }
   
   public void getConstrainers (List<Constrainer> list, int level) {
      recursivelyGetConstrainers (this, list, level);
   }

   public void getForceEffectors (List<ForceEffector> list, int level) {
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.getForceEffectors (list, level+1);
      }
      recursivelyGetLocalComponents (this, list, ForceEffector.class);
      list.add (new GravityEffector());
   }

   public void addGeneralMassBlocks (SparseNumberedBlockMatrix M) {
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.addGeneralMassBlocks (M);
      }
   }
   
   @Override
   public void getMassMatrixValues (SparseNumberedBlockMatrix M, VectorNd f, double t) {
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.getMassMatrixValues (M, f, t);
      }
      updateLocalDynamicComponents();
      int bi;
      for (int i=0; i<myLocalDynamicComps.size(); i++) {
         DynamicComponent c = myLocalDynamicComps.get(i);
          if ((bi = c.getSolveIndex()) != -1) {
            c.getEffectiveMass (M.getBlock (bi, bi), t);
            c.getEffectiveMassForces (f, t, M.getBlockRowOffset (bi));
          }
      }
   }

   @Override
   public void mulInverseMass (
      SparseBlockMatrix M, VectorNd a, VectorNd f) {
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.mulInverseMass (M, a, f);
      }
      updateLocalDynamicComponents();
      double[] abuf = a.getBuffer();
      double[] fbuf = f.getBuffer();
      int asize = a.size();
      if (M.getAlignedBlockRow (asize) == -1) {
         throw new IllegalArgumentException (
            "size of 'a' not block aligned with 'M'");
      }
      if (f.size() < asize) {
         throw new IllegalArgumentException (
            "size of 'f' is less than the size of 'a'");
      }
      for (int i=0; i<myLocalDynamicComps.size(); i++) {
         DynamicComponent c = myLocalDynamicComps.get(i);
         int bi = c.getSolveIndex();
         if (bi != -1) {
            int idx = M.getBlockRowOffset (bi);
            if (idx < asize) {
               c.mulInverseEffectiveMass (M.getBlock (bi, bi), abuf, fbuf, idx);
            }
         }
      }
   }

   protected void recursivelyGetAuxStateComponents (
      CompositeComponent comp,
      List<HasNumericState> list, int level) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof MechSystemModel) {
            ((MechSystemModel)c).getAuxStateComponents (list, level+1);
         }
         else if (c instanceof CollisionManager) {
            CollisionManager cm = (CollisionManager)c;
            list.add (cm);
         }
         else if (c instanceof HasNumericStateComponents) {
            ((HasNumericStateComponents)c).getNumericStateComponents(list);
         }
         else if (c instanceof HasNumericState) {
            if (!(c instanceof DynamicComponent) &&
                  ((HasNumericState)c).hasState()) {
               // this will include inactive RigidBodyConnecters;
               // that should be OK
               list.add ((HasNumericState)c);
            }
         }
         else if (c instanceof CompositeComponent) {
            recursivelyGetAuxStateComponents (
               (CompositeComponent)c, list, level);
         }
      }
   }

   public void getAuxStateComponents (List<HasNumericState> list, int level) {
      recursivelyGetAuxStateComponents (this, list, level);
   }
   
   /**
    * {@inheritDoc} 
    */
   public void getSlaveObjectComponents (
      List<HasSlaveObjects> list, int level) {
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.getSlaveObjectComponents (list, level+1);
      }
      recursivelyGetLocalComponents (this, list, HasSlaveObjects.class);      
      //recursivelyGetSlaveObjectComponents (this, comps, level);
   }

   public void projectRigidBodyPositionConstraints() {
      mySolver.projectRigidBodyPosConstraints (0);
   }

   public int combineMatrixTypes (int type1, int type2) {
      return (type1 & type2);
   }

   public StepAdjustment preadvance (double t0, double t1, int flags) {
      if (t0 == 0) {
         mySolver.projectPosConstraints (0);
      }     
//      updateLocalAdvanceComponents();
//      for (RequiresPrePostAdvance c : myLocalPrePostAdvanceComps) {
//         c.preadvance (t0, t1, flags);
//      }
      return super.preadvance (t0, t1, flags);
   }


   private void zeroExternalForces() {
      updateLocalDynamicComponents();
      for (int i=0; i<myLocalDynamicComps.size(); i++) {
         myLocalDynamicComps.get(i).zeroExternalForces();
      }
   }

   public void recursivelyPrepareAdvance (
      double t0, double t1, int flags, int level) {

      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.recursivelyPrepareAdvance (t0, t1, flags, level+1);
      }
      updateLocalAdvanceComponents();
      for (RequiresPrePostAdvance c : myLocalPrePostAdvanceComps) {
         c.preadvance (t0, t1, flags);
      }
   }

   public void recursivelyFinalizeAdvance (
      StepAdjustment stepAdjust, double t0, double t1, int flags, int level) {

      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.recursivelyFinalizeAdvance (stepAdjust, t0, t1, flags, level+1);
      }
      updateLocalAdvanceComponents();
      for (RequiresPrePostAdvance c : myLocalPrePostAdvanceComps) {
         c.postadvance (t0, t1, flags);
      }
   }

   public void recursivelyInitialize (double t, int level) {
      // Local initialization should cause excitations of ExcitationComponents
      // to be zeroed at t = 0. Note that inputProbe.setState should do that
      // same thing, so we're just being extra careful here.
      updateLocalInitComponents();
      for (int i=0; i<myLocalInitComps.size(); i++) {
         myLocalInitComps.get(i).initialize (t);
      }
      if (t == 0) {
         myCollisionManager.initialize();
         if (level == 0) {
            setPenetrationTolIfNecessary();
         }            
         getPenetrationTol(); // make sure this is initialized
      }
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.recursivelyInitialize (t, level+1);
      }
      super.recursivelyInitialize (t, level);
   }

   public void setBounds (Point3d pmin, Point3d pmax) {
      if (myMinBound == null) {
         myMinBound = new Point3d();
      }
      if (myMaxBound == null) {
         myMaxBound = new Point3d();
      }
      myMinBound.set (pmin);
      myMaxBound.set (pmax);
   }

   public void setBounds (
      double xmin, double ymin, double zmin,
      double xmax, double ymax, double zmax) {

      setBounds (
         new Point3d (xmin, ymin, zmin), new Point3d (xmax, ymax, zmax));
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      doclear();
      super.scan (rtok, ref);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "minBound")) {
         myMinBound = new Point3d();
         myMinBound.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "maxBound")) {
         myMaxBound = new Point3d();
         myMaxBound.scan (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      if (myMinBound != null) {
         pw.println ("minBound=[" + myMinBound.toString (fmt) + "]");
      }
      if (myMaxBound != null) {
         pw.println ("maxBound=[" + myMaxBound.toString (fmt) + "]");
      }
      super.writeItems (pw, fmt, ancestor);
   }

   /* ======== Renderable implementation ======= */

   // protected void recursivelyPrerender (
   //    CompositeComponent comp, RenderList list) {

   //     for (int i=0; i<comp.numComponents(); i++) {
   //       ModelComponent c = comp.get (i);
   //       if (c instanceof Renderable) {
   //          list.addIfVisible ((Renderable)c);
   //       }
   //       else if (c instanceof CompositeComponent) {
   //          recursivelyPrerender ((CompositeComponent)c, list);
   //       }
   //    }     
   // }

   // public void prerender (RenderList list) {

   //    recursivelyPrerender (this, list);
   // }

   // protected void recursivelyUpdateBounds (
   //    CompositeComponent comp, Point3d pmin, Point3d pmax) {

   //    for (int i=0; i<comp.numComponents(); i++) {
   //       ModelComponent c = comp.get (i);
   //       if (c instanceof Renderable) {
   //          ((Renderable)c).updateBounds (pmin, pmax);
   //       }
   //       else if (c instanceof CompositeComponent) {
   //          recursivelyUpdateBounds ((CompositeComponent)c, pmin, pmax);
   //       }
   //    }

   // }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      if (myMinBound != null) {
         myMinBound.updateBounds (pmin, pmax);
      }
      if (myMaxBound != null) {
         myMaxBound.updateBounds (pmin, pmax);
      }
      super.updateBounds (pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {
   }

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
      
      // note that these bounds, if present, are explicitly set by the user, 
      // so it is appropriate to transform rather then recompute them
      if (myMinBound != null) {
         gtr.transformPnt (myMinBound);
      }
      if (myMaxBound != null) {
         gtr.transformPnt (myMaxBound);
      }     
   }

   /**
    * {@inheritDoc}
    */
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addTransformableDescendants (this, flags);
   }
   
   protected void recursivelyScaleDistance (CompositeComponent comp, double s) {
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof ScalableUnits) {
            ((ScalableUnits)c).scaleDistance (s);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyScaleDistance ((CompositeComponent)c, s);
         }
      }
   }               

   public void scaleDistance (double s) {
      recursivelyScaleDistance (this, s);

      for (DynamicAttachment a : buildLocalAttachments()) {
         a.updateAttachment();
      }

      myGravity.scale (s);
      if (myMinBound != null) {
         myMinBound.scale (s);
      }
      if (myMaxBound != null) {
         myMaxBound.scale (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      myRotaryDamping *= (s * s);
      if (myPenetrationTol != -1) {
         myPenetrationTol *= s;
      }
   }

   protected void recursivelyScaleMass (CompositeComponent comp, double s) {
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof ScalableUnits) {
            ((ScalableUnits)c).scaleMass (s);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyScaleMass ((CompositeComponent)c, s);
         }
      }
   }               


   public void scaleMass (double s) {
      recursivelyScaleMass (this, s);
      myPointDamping *= s;
      myFrameDamping *= s;
      myRotaryDamping *= s;
   }

   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hasState() {
      return true;
   }

   public static MechModel lowestCommonModel (
      ModelComponent comp1, ModelComponent comp2) {
      ModelComponent ancestor =
         ComponentUtils.findCommonAncestor (comp1, comp2);
      while (ancestor != null) {
         if (ancestor instanceof MechModel) {
            return (MechModel)ancestor;
         }
         else {
            ancestor = ancestor.getParent();
         }
      }
      return null;
   }

   /**
    * Returns the topmost MechModel, if any, that is associated with
    * a specific component. 
    * 
    * @param comp component to start with
    * @return topmost MechModel on or above <code>comp</code>, or
    * <code>null</code> if there is none.
    */
   public static MechModel topMechModel (ModelComponent comp) {
      MechModel mech = null;
      while (comp != null) {
         if (comp instanceof MechModel) {
            mech = (MechModel) comp;
         }
         comp=comp.getParent();
      }
      return mech;
   }

   /**
    * Returns the nearest MechModel, if any, that is an ancestor of a specific
    * component. If the component is itself a <code>MechModel</code>, the
    * component itself returned.
    * 
    * @param c component to start with
    * @return nearest MechModel on or above <code>comp</code>, or
    * <code>null</code> if there is none.
    */
   public static MechModel nearestMechModel (ModelComponent c) {
      while (c != null) {
         if (c instanceof MechModel) {
            return (MechModel)c;
         }               
         c = c.getParent();
      }
      return null;
   }         

   protected void clearCachedData (ComponentChangeEvent e) {
      super.clearCachedData(e);
      mySolveMatrix = null;
      myDynamicComponents = null;
      myLocalDynamicComps = null;
      myLocalInitComps = null;
      myLocalPrePostAdvanceComps = null;
      myLocalModels = null;
      if (e == null || e.getComponent() != myCollisionManager.behaviors()) {
         myCollisionManager.clearCachedData();
      }
      myForcesNeedUpdating = true;
      myCollidableBodiesNeedIndexing = true;
   }

   @Override
   protected void notifyStructureChanged (Object comp, boolean stateIsChanged) {
      if (comp == this) {
         updateHardwiredLists();
      }
      clearCachedData (null);
      super.notifyStructureChanged (comp, stateIsChanged);
   }

   private void handleGeometryChange (GeometryChangeEvent e) {
      // currently no need to do anything - slave state changes are handled by
      // lower level components, and attachments are handled by the
      // UpdateAttachmentAction
   }

   public void componentChanged (ComponentChangeEvent e) {
      if (e.getCode() == ComponentChangeEvent.Code.GEOMETRY_CHANGED) { 
         handleGeometryChange ((GeometryChangeEvent)e);
      }
      else { // invalidate everything for now
         clearCachedData (e);
      }
      notifyParentOfChange (e);
   }

   /**
    * {@inheritDoc}
    */
   public DynamicComponent checkVelocityStability() {
      updateDynamicComponentLists(); // PARANOID
      for (int i=0; i<myNumActive; i++) {
         DynamicComponent c = myDynamicComponents.get(i);
         if (c.velocityLimitExceeded (
                myMaxTranslationalVel, myMaxRotationalVel)) {
            return c;
         }
      }
      return null;
   }

   protected void recursivelyDispose (CompositeComponent comp) {
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof Disposable) {
            ((Disposable)c).dispose();
         }
         else if (c instanceof CompositeComponent) {
            recursivelyDispose ((CompositeComponent)c);
         }
      }     
   }

   /**
    * Used to find all MechSystemModel immediately referenced by this model.
    */
   protected void recursivelyGetLocalModels (
      CompositeComponent comp, List<MechSystemModel> list) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof MechSystemModel) {
            list.add ((MechSystemModel)c);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyGetLocalModels ((CompositeComponent)c, list);
         }
      }
   }
   
   protected void updateLocalModels () {
      if (myLocalModels == null) {
         myLocalModels = new ArrayList<MechSystemModel>();
         recursivelyGetLocalModels (this, myLocalModels);
      }
   }
   
   protected ArrayList<MechSystemModel> getLocalModels() {
      updateLocalModels();
      return myLocalModels;
   }

   protected void updateLocalInitComponents () {
      if (myLocalInitComps == null) {
         myLocalInitComps = new ArrayList<RequiresInitialize>();
         recursivelyGetLocalComponents (
            this, myLocalInitComps, RequiresInitialize.class);
         // recursivelyGetLocalInitComponents (this, myLocalInitComponents);
      }
   }
   
   protected void updateLocalAdvanceComponents () {
      if (myLocalPrePostAdvanceComps == null) {
         myLocalPrePostAdvanceComps = new ArrayList<RequiresPrePostAdvance>();
         recursivelyGetLocalComponents (
            this, myLocalPrePostAdvanceComps, RequiresPrePostAdvance.class);        
         // recursivelyGetLocalAdvanceComponents (
         //    this, myLocalAdvanceComponents);
      }
   }

   protected void updateLocalDynamicComponents () {
      if (myLocalDynamicComps == null) {
         myLocalDynamicComps = new ArrayList<DynamicComponent>();
         
         //recursivelyGetLocalDynamicComponents (this, myLocalDynamicComponents);
         recursivelyGetLocalComponents (
            this, myLocalDynamicComps, DynamicComponent.class);        
      }
   }
   
   public void dispose() {
      recursivelyDispose (this);
      mySolver.dispose();
   }

   // ForceEffector that implements gravity for MechModels
   private class GravityEffector implements ForceEffector {

      public void applyForces (double t) {
         updateLocalDynamicComponents();
         if (!myGravity.equals (Vector3d.ZERO)) {
            for (int i=0; i<myLocalDynamicComps.size(); i++) {
               DynamicComponent c = myLocalDynamicComps.get(i); 
               c.applyGravity (myGravity);
            }
         }
         // XXX hack
         myForcesNeedUpdating = false;
      }

      public void addSolveBlocks (SparseNumberedBlockMatrix M) {
         // nothing needed
      }

      public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
         // nothing needed
      }

      public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
         // nothing needed
      }

      public int getJacobianType() {
         return Matrix.SPD;
      }
   };

   class EnforceArticulationAction implements TransformGeometryAction {

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {

         updateDynamicComponentLists();
         if (gtr.isRestoring()) {
            RigidTransform3d TBW = new RigidTransform3d();
            for (int i=0; i<myNumActive; i++) {
               DynamicComponent dcomp = myDynamicComponents.get(i);
               if (dcomp instanceof Frame) {
                  TBW.set (gtr.restoreObject(TBW));
                  ((Frame)dcomp).setPose (TBW);
               }
            }
         }
         else {
            if (gtr.isSaving()) {
               for (int i=0; i<myNumActive; i++) {
                  DynamicComponent dcomp = myDynamicComponents.get(i);
                  if (dcomp instanceof Frame) {
                     RigidTransform3d TBW =
                        new RigidTransform3d(((Frame)dcomp).getPose());
                     gtr.saveObject (TBW);
                  }
               }
            }
            projectRigidBodyPositionConstraints();
         }
      }
   }

   class RequestEnforceArticulationAction implements TransformGeometryAction {

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {
         context.addAction (new EnforceArticulationAction());
      }
   }

   RequestEnforceArticulationAction myRequestEnforceArticulationAction =
      new RequestEnforceArticulationAction();

   public SparseBlockMatrix getActiveStiffnessMatrix() {
      SparseBlockMatrix K = super.getActiveStiffnessMatrix();
      if (myAddFrameMarkerStiffness) {
         // add asymmetric component due to frame markers
         for (FrameMarker mkr : frameMarkers()) {
            int bi = mkr.getFrame().getSolveIndex();
            Matrix6dBlock blk = (Matrix6dBlock)K.getBlock (bi, bi);
            if (blk != null) {
               Point3d lw = new Point3d(mkr.getLocation());
               lw.transform (mkr.getFrame().getPose().R);
               Vector3d f = mkr.getForce();
               Matrix3d Krot = new Matrix3d();
               Krot.outerProduct (lw, f);
               Krot.m00 -= lw.dot(f);
               Krot.m11 -= lw.dot(f);
               Krot.m22 -= lw.dot(f);
               blk.addSubMatrix33 (Krot);
            }         
         }
      }
      return K;
   }

   public SparseBlockMatrix getTrueStiffnessMatrix () {

      boolean saveIgnoreCoriolis = PointSpringBase.myIgnoreCoriolisInJacobian;
      boolean saveSymmetricJacobian = FrameSpring.mySymmetricJacobian;
      boolean saveAddFrameMarkerStiffness = myAddFrameMarkerStiffness;
      PointSpringBase.myIgnoreCoriolisInJacobian = false;
      FrameSpring.mySymmetricJacobian = false;
      myAddFrameMarkerStiffness = true;

      SparseBlockMatrix K = getActiveStiffnessMatrix();

      myAddFrameMarkerStiffness = saveAddFrameMarkerStiffness;
      PointSpringBase.myIgnoreCoriolisInJacobian = saveIgnoreCoriolis;
      FrameSpring.mySymmetricJacobian = saveSymmetricJacobian;

      return K;
   }

   /**
    * Returns the true active stiffness matrix with frame orientation
    * expressed using yaw-pitch-roll coordinates. This increases
    * the likelyhood that the matrix is symmetric.
    * @param modifiers if not {@code null}, specifies a list
    * of modifiers to apply to the true stiffness matrix before
    * converting it to yaw-pitch-roll coordinates.
    * 
    * @return true active yaw-pitch-roll stiffness matrix
    */
   public SparseBlockMatrix getYPRStiffnessMatrix (
      List<SolveMatrixModifier> modifiers) {

      SparseBlockMatrix K = getTrueStiffnessMatrix();
      VectorNd f = new VectorNd (getActiveVelStateSize());
      getActiveForces (f);
      ArrayList<DynamicComponent> comps = getActiveDynamicComponents();
      if (modifiers != null) {
         for (SolveMatrixModifier m : modifiers) {
            m.modify (K, f, comps);
         }
      }
      // convert to YPR formulation
      YPRStiffnessUtils.convertStiffnessToYPR (K, f, comps);
      return K;
   }

   /**
    * Returns the regular active stiffness matrix used by the
    * ArtiSynth solver. 
    * @param modifiers if not {@code null}, specifies a list
    * of modifiers to apply to the stiffness matrix.
    * 
    * @return regular active stiffness matrix
    */
   public SparseBlockMatrix getStiffnessMatrix (
      List<SolveMatrixModifier> modifiers) {

      boolean saveIgnoreCoriolis = PointSpringBase.myIgnoreCoriolisInJacobian;
      boolean saveSymmetricJacobian = FrameSpring.mySymmetricJacobian;
      boolean saveAddFrameMarkerStiffness = myAddFrameMarkerStiffness;
      PointSpringBase.myIgnoreCoriolisInJacobian = true;
      FrameSpring.mySymmetricJacobian = true;
      myAddFrameMarkerStiffness = false;

      SparseBlockMatrix K = getActiveStiffnessMatrix();

      myAddFrameMarkerStiffness = saveAddFrameMarkerStiffness;
      PointSpringBase.myIgnoreCoriolisInJacobian = saveIgnoreCoriolis;
      FrameSpring.mySymmetricJacobian = saveSymmetricJacobian;

      //System.out.println ("regular K, symmetric=" + K.isSymmetric(1e-4));

      VectorNd f = new VectorNd (getActiveVelStateSize());
      getActiveForces (f);
      ArrayList<DynamicComponent> comps = getActiveDynamicComponents();
      if (modifiers != null) {
         for (SolveMatrixModifier m : modifiers) {
            m.modify (K, f, comps);
         }
      }
      //System.out.println ("after mods, symmetric=" + K.isSymmetric(1e-4));
      // convert to YPR formulation
      YPRStiffnessUtils.convertStiffnessToYPR (K, f, comps);
      //System.out.println ("after YPR, symmetric=" + K.isSymmetric(1e-4));
      return K;
   }


}

