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

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.GLRenderer;
import maspack.render.RenderableUtils;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.StringHolder;
import maspack.util.Disposable;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.MechSystemSolver.MatrixSolver;
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentListView;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.GeometryChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.util.*;

public class MechModel extends MechSystemBase implements
TransformableGeometry, ScalableUnits, MechSystemModel {

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
   protected ComponentList<RigidBodyConnector> myConnectors;
   protected ComponentList<ConstrainerBase> myConstrainers;
   protected PointList<FrameMarker> myFrameMarkers;
   protected CollisionManager myCollisionManager;

   protected ArrayList<DynamicComponent> myLocalDynamicComps;
   protected ArrayList<RequiresInitialize> myLocalInitComps;
   protected ArrayList<RequiresPrePostAdvance> myLocalPrePostAdvanceComps;
   protected ArrayList<MechSystemModel> myLocalModels;

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

   protected ComponentList<DynamicAttachment> myAttachments;
   SparseNumberedBlockMatrix mySolveMatrix;

   String myPrintState = null;
   PrintWriter myPrintStateWriter = null;

   // flag to indicate that forces need updating. Since forces are always
   // updated before a call to advance() (within setDefaultInputs()), this
   // flag only needs to be set during the advance routine --
   // specifically, within the setState methods
   protected boolean myForcesNeedUpdating = false;

   protected static Integrator DEFAULT_INTEGRATOR =
      Integrator.ConstrainedBackwardEuler;
   protected static MatrixSolver DEFAULT_MATRIX_SOLVER = MatrixSolver.Pardiso;

   protected static double DEFAULT_POINT_DAMPING = 0;
   protected static double DEFAULT_FRAME_DAMPING = 0;
   protected static double DEFAULT_ROTARY_DAMPING = 0;
   protected static PosStabilization DEFAULT_STABILIZATION =
      PosStabilization.GlobalMass;

   protected Integrator myIntegrationMethod;
   protected MatrixSolver myMatrixSolver;

   protected static final Vector3d DEFAULT_GRAVITY = new Vector3d (0, 0, -9.8);

   protected Vector3d myGravity = new Vector3d(DEFAULT_GRAVITY);
   protected PropertyMode myGravityMode = PropertyMode.Inherited;

   protected static final double DEFAULT_PENETRATION_TOL = -1; 
   protected double myPenetrationTol = DEFAULT_PENETRATION_TOL;
   protected PropertyMode myPenetrationTolMode = PropertyMode.Inherited;

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
      myProps.add ("integrator * *", "integration method ", DEFAULT_INTEGRATOR);
      myProps.add (
         "matrixSolver * *", "matrix solver for implicit integration ",
         DEFAULT_MATRIX_SOLVER);
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
      for (int i=0; i<numComponents(); i++) {
         ModelComponent c = get (i);
         if (c instanceof ComponentList &&
             c != myCollisionManager) {
            ((ComponentList<?>)c).removeAll();
         }
      }
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
      setMatrixSolver (DEFAULT_MATRIX_SOLVER);
      setIntegrator (DEFAULT_INTEGRATOR);
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
         new ComponentList<RigidBodyConnector> (
            RigidBodyConnector.class, "rigidBodyConnectors", "c");
      myConstrainers =
         new ComponentList<ConstrainerBase> (
            ConstrainerBase.class, "particleConstraints", "pc");

      myAttachments =
         new ComponentList<DynamicAttachment> (
            DynamicAttachment.class, "attachments", "a");

      myExciterList =
         new ComponentList<MuscleExciter> (MuscleExciter.class, "exciters", "e");
      myRenderables =
         new RenderableComponentList<RenderableComponent> (
            RenderableComponent.class, "renderables", "re");

      myCollisionManager = new CollisionManager(this);
      myCollisionManager.setName ("collisionManager");

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
      addFixed (myRenderables);

      addFixed (myCollisionManager);
 
      setMatrixSolver (DEFAULT_MATRIX_SOLVER);
      setIntegrator (DEFAULT_INTEGRATOR);      
   }

   protected void updateHardwiredLists() {
      // TODO
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
   
   public void setPenetrationTol (double tol) {
      if (tol < 0) {
         tol = computeDefaultPenetrationTol();
      }
      myPenetrationTol = tol;
      myPenetrationTolMode =
         PropertyUtils.propagateValue (
            this, "penetrationTol", tol, myPenetrationTolMode);
   }

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
    */
   public double computeDefaultPenetrationTol() {
      double tol = 0.0001;
      double radius = RenderableUtils.getRadius (this);
      if (radius != 0) {
         tol = 1e-4*radius;
      }
      return tol;
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

   public void setFriction (double mu) {
      myCollisionManager.setFriction (mu);
   }
   // ZZZ 2733 old line count

   /** 
    * Specifies the default collision behavior for all default pairs
    * (RigidBody-RigidBody, RigidBody-Deformable, Deformable-Deformable,
    * Deformable-Self) associated with this MechModel. 
    * This is a convenience wrapper for
    * {@link #setDefaultCollisionBehavior(CollisionBehavior)
    * setDefaultCollisionBehavior(behavior)}.
    * 
    * @param enabled if true, enables collisions
    * @param mu friction coefficient (ignored if enabled is false)
    */
   public void setDefaultCollisionBehavior (boolean enabled, double mu) {
      setDefaultCollisionBehavior (new CollisionBehavior (enabled, mu));
   }

   /** 
    * Specifies a default collision behavior for all default pairs
    * (RigidBody-RigidBody, RigidBody-Deformable, Deformable-Deformable,
    * Deformable-Self) associated with this MechModel.
    * 
    * @param behavior desired collision behavior
    */
   public void setDefaultCollisionBehavior (CollisionBehavior behavior) {
      myCollisionManager.setDefaultBehavior (behavior);
   }

   /** 
    * Gets the default collision behavior
    * for a specified pair of generic collidable types. 
    * Allowed collidable types are {@link Collidable#RigidBody} and
    * {@link Collidable#Deformable}. In addition, the type
    * {@link Collidable#Self} can be paired with {@link Collidable#Deformable}
    * to obtain the default behavior for deformable self collisions;
    * {@link Collidable#Self} cannot be paired with other types.
    * 
    * @param typeA first generic collidable type
    * @param typeB second generic collidable type
    * @return default collision behavior for the indicted collidable types.
    */
   public CollisionBehavior getDefaultCollisionBehavior (
      Collidable typeA, Collidable typeB) {

      return myCollisionManager.getDefaultCollisionBehavior (typeA, typeB);
   }

   /**
    * Sets the default collision behavior, for this MechModel, for a specified
    * pair of generic collidable types.  Allowed collidable types are {@link
    * Collidable#RigidBody} and {@link Collidable#Deformable}. In addition, the
    * type {@link Collidable#Self} can be paired with {@link
    * Collidable#Deformable} to set the default behavior for deformable self
    * collisions; {@link Collidable#Self} cannot be paired with other types.
    *
    * @param typeA first generic collidable type
    * @param typeB second generic collidable type
    * @param behavior specified collision behavior
    */
   public void setDefaultCollisionBehavior (
      Collidable typeA, Collidable typeB, CollisionBehavior behavior) {

      myCollisionManager.setDefaultBehavior (typeA, typeB, behavior);
   }

   /**
    * Sets the default collision behavior, for this MechModel, for a specified
    * pair of generic collidable types.  This is a convenience wrapper for
    * {@link
    * #setDefaultCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setDefaultCollisionBehavior(typeA,typeB,behavior)}.
    *
    * @param typeA first generic collidable type
    * @param typeB second generic collidable type
    * @param enabled if true, enables collisions
    * @param mu friction coefficient (ignored if enabled is false)
    */
   public void setDefaultCollisionBehavior (
      Collidable typeA, Collidable typeB, boolean enabled, double mu) {

      setDefaultCollisionBehavior (
         typeA, typeB, new CollisionBehavior (enabled, mu));
   }

   /** 
    * Enables or disables collisions for a specified pair of collidables,
    * overriding, if necessary, the primary collision behavior. This is a
    * convenience wrapper for {@link
    * #setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setCollisionBehavior(a,b,behavior)}.
    * 
    * @param a first collidable
    * @param b second collidable
    * @param enabled if true, enables collisions
    */
   public void setCollisionBehavior (
      Collidable a, Collidable b, boolean enabled) {

      setCollisionBehavior (a, b, new CollisionBehavior (enabled, /*mu=*/0));
   }

   /** 
    * Sets collision behavior for a specified pair of collidables, overriding,
    * if necessary, the primary behavior. This is a convenience wrapper for
    * {@link
    * #setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setCollisionBehavior(a,b,behavior)}.
    * 
    * @param a first collidable
    * @param b second collidable
    * @param enabled if true, enables collisions
    * @param mu friction coefficient (ignored if enabled is false)
    */
   public void setCollisionBehavior (
      Collidable a, Collidable b, boolean enabled, double mu) {

      setCollisionBehavior (a, b, new CollisionBehavior (enabled, mu));
   }

   /** 
    * Sets collision behavior for a specified pair of collidables
    * <code>a</code> and <code>b</code>, overriding, if necessary, the primary
    * behavior. The primary behavior is the default collision behavior, unless
    * <code>a</code> and <code>b</code> both belong to a sub MechModel, in
    * which case it is the value of
    * {@link #getCollisionBehavior getCollisionBehavior(a,b)} for that
    * MechModel.
    * <p>
    * Each collidable must be a particular
    * component instance. A deformable body may be paired with {@link
    * Collidable#Self} to indicate self-intersection; otherwise, generic
    * designations (such as {@link Collidable#RigidBody}) are not allowed.
    *
    * <p>
    * If <code>a</code> or <code>b</code> contain sub-collidables (i.e.,
    * descendants components which are also <code>Collidable</code>),
    * then the behavior is applied to all pairs of these sub-collidables.
    * If <code>a</code> and <code>b</code> are the same, then the
    * behavior is applied to the self-collision among all sub-collidables
    * whose {@link Collidable#getCollidable getCollidable()} method
    * returns <code>Colidability.ALL</code> or
    * <code>Colidability.INTERNAL</code>.
    *
    * <p>
    * The behavior specified by this method can be removed
    * using {@link #clearCollisionBehavior clearCollisionBehavior(a,b)}.
    * 
    * @param a first collidable
    * @param b second collidable
    * @param behavior specified collision behavior
    */
   public void setCollisionBehavior (
      Collidable a, Collidable b, CollisionBehavior behavior) {
      myCollisionManager.setBehavior (a, b, behavior);
   }

   /** 
    * Clears any collision behavior that has been defined for
    * <code>a</code> and <code>b</code> (using the
    * <code>setCollisionBehavior()</code> methods)
    * to override the primary behavior. The collidables
    * that may be specified are described in the documentation for
    * {@link #setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setCollisionBehavior()}.
    * 
    * @param a first collidable
    * @param b second collidable
    * @return <code>true</code> if an override behavior was specified
    * and removed for the indicated collidables.
    */
   public boolean clearCollisionBehavior (Collidable a, Collidable b) {
      return myCollisionManager.clearCollisionBehavior (a, b);
   }

   /** 
    * Clears any collision behaviors that have been defined (using the
    * <code>setCollisionBehavior()</code> methods) to override the default
    * collision behaviors betweem pairs of Collidables.
    */
   public void clearCollisionBehaviors () {
      myCollisionManager.clearCollisionBehaviors();
   }
   

   /** 
    * Returns the collision behavior for a specified pair of collidables
    * <code>a</code> and <code>b</code>. Generic designations (such as {@link
    * Collidable#RigidBody}) are not allowed.
    * The returned behavior is the current effective behavior resulting from
    * the application of all default and explicit collision behavior
    * settings.
    *
    * <p> If <code>a</code> or <i><code>b</code> contain sub-collidables, then
    * if a consistent collision behavior is found amount all pairs of
    * sub-collidables, that behavior is returned; otherwise, <code>null</code>
    * is returned. If <code>a</code> equals <i><code>b</code>, then this method
    * searches for a consistent collision behavior among all sub-collidables of
    * <code>a</code> whose {@link Collidable#getCollidable getCollidable()}
    * method returns <code>Colidability.ALL</code> or
    * <code>Colidability.INTERNAL</code>.

    * 
    * @param a first collidable
    * @param b second collidable
    * @return behavior for this pair of collidables.
    */
   public CollisionBehavior getCollisionBehavior (Collidable a, Collidable b) {
      return myCollisionManager.getBehavior (a, b);
   }

   public void getCollidables (List<Collidable> list, int level) {
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.getCollidables (list, level+1);
      }
      recursivelyGetLocalComponents (this, list, Collidable.class);
      //recursivelyGetCollidables (this, list, level);
   }      

   public CollisionManager getCollisionManager() {
      return myCollisionManager;
   }

   // ZZZ

   public void setMatrixSolver (MatrixSolver method) {
      myMatrixSolver = method;
      if (mySolver != null) {
         mySolver.setMatrixSolver (method);
         myMatrixSolver = mySolver.getMatrixSolver();
      }
   }

   public Object validateMatrixSolver (
      MatrixSolver method, StringHolder errMsg) {
      if (mySolver != null && !mySolver.hasMatrixSolver (method)) {
         return PropertyUtils.correctedValue (getMatrixSolver(), "Solver not "
         + method + " not available", errMsg);
      }
      else {
         return PropertyUtils.validValue (method, errMsg);
      }
   }

   public MatrixSolver getMatrixSolver() {
      return myMatrixSolver;
   }

   public void setIntegrator (Integrator integrator) {
      myIntegrationMethod = integrator;
      if (mySolver != null) {
         mySolver.setIntegrator (integrator);
         if (mySolver.getIntegrator() != integrator) {
            myIntegrationMethod = mySolver.getIntegrator();
         }
      }
   }

   public Integrator getIntegrator() {
      return myIntegrationMethod;
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
    * Adds a frame marker to a particular frame, with the location
    * specified relative to the frame's coordinate frame
    * @param frame frame object to add a marker to
    * @param loc location within the frame (according to its own coordinate system)
    * @return the created marker
    */
   public FrameMarker addFrameMarker(Frame frame, Point3d loc) {
      FrameMarker mkr = new FrameMarker(frame, loc);
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

   public ComponentListView<RigidBodyConnector> rigidBodyConnectors() {
      return myConnectors;
   }

   public void addRigidBodyConnector (RigidBodyConnector c) {
      myConnectors.add (c);
   }

   public void removeRigidBodyConnector (RigidBodyConnector c) {
      myConnectors.remove (c);
   }

   public void clearRigidBodyConnectors() {
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
    * component's current position state.
    * @param p1 Point to connect. Must currently be a particle.
    * @param comp Component to attach the particle to.
    */
   public void attachPoint (Point p1, PointAttachable comp) {
      if (p1.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      PointAttachment ax = comp.createPointAttachment (p1);
      if (DynamicAttachment.containsLoop (ax, p1, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      addAttachment (ax);
   }

   public void attachPoint (Point p1, RigidBody body, Point3d loc) {
      if (p1.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      PointFrameAttachment rbax = new PointFrameAttachment (body, p1, loc);
      if (DynamicAttachment.containsLoop (rbax, p1, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      addAttachment (rbax);
   }

   public boolean detachPoint (Point p1) {
      DynamicAttachment a = p1.getAttachment();
      if (a != null && a.getParent() == myAttachments) {
         removeAttachment (a);
         return true;
      }
      else {
         return false;
      }
   }

    /** 
    * Attaches a frame to a FrameAttachable component, with the
    * initial pose of the frame described by <code>TFW</code>usin
    * component's current position state.
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
      if (DynamicAttachment.containsLoop (ax, frame, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      addAttachment (ax);
   }

   /** 
    * Attaches a frame to a FrameAttachable component, using each
    * component's current position state.
    * @param frame Frame to connect.
    * @param comp component to attach the frame to.
    */
   public void attachFrame (Frame frame, FrameAttachable comp) {
      attachFrame (frame, comp, frame.getPose());
   }

   public boolean detachFrame (Frame frame) {
      DynamicAttachment a = frame.getAttachment();
      if (a != null && a.getParent() == myAttachments) {
         removeAttachment (a);
         return true;
      }
      else {
         return false;
      }
   }

//   /** 
//    * Attaches a point to an FEM. The point in question is connected to either
//    * its containing element, or projected to the nearest surface element.
//    * 
//    * @param p1 Point to connect. Must currently be a particle.
//    * @param fem FemModel to connect the point to.
//    * @param reduceTol if > 0, attempts to reduce the number of nodes
//    * in the connection by removing any whose coordinate weights are
//    * less than <code>reduceTol</code>. 
//    */
//   public void attachPoint (Point p1, FemModel3d fem, double reduceTol) {
//      if (p1.isAttached()) {
//         throw new IllegalArgumentException ("point is already attached");
//      }
//      if (!(p1 instanceof Particle)) {
//         throw new IllegalArgumentException ("point is not a particle");
//      }
//      if (ComponentUtils.isAncestorOf (fem, p1)) {
//         throw new IllegalArgumentException (
//            "FemModel is an ancestor of the point");
//      }
//      Point3d loc = new Point3d();
//      FemElement3d elem = fem.findNearestElement (loc, p1.getPosition());
//      FemNode3d nearestNode = null;
//      double nearestDist = Double.MAX_VALUE;
//      for (FemNode3d n : elem.getNodes()) {
//         double d = n.distance (p1);
//         if (d < nearestDist) {
//            nearestNode = n;
//            nearestDist = d;
//         }
//      }
//      if (nearestDist <= reduceTol) {
//         // just attach to the node
//         attachPoint (p1, nearestNode);
//      }
//      else {
//         PointFem3dAttachment ax =
//            PointFem3dAttachment.create (p1, elem, loc, reduceTol);
//         // Coords are computed in createNearest.  the point's position will be
//         // updated, if necessary, by the addRemove hook when the attachement is
//         // added.
//         addAttachment (ax);
//      }
//   }

   // public void attachPoint (Point p1, RigidBody body) {
   //    attachPoint (p1, body, null);
   // }

   public ComponentList<DynamicAttachment> attachments() {
      return myAttachments;
   }

   public void addAttachment (DynamicAttachment ax) {
      ax.updatePosStates();
      myAttachments.add (ax);
   }

   void removeAttachment (DynamicAttachment ax) {
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

   public void getDynamicComponents (
      List<DynamicComponent> active, 
      List<DynamicComponent> attached,
      List<DynamicComponent> parametric) {

      recursivelyGetDynamicComponents (this, active, attached, parametric);
   }

   protected void recursivelyGetConstrainers (
      CompositeComponent comp, List<Constrainer> list, int level) {
      
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof RigidBodyConnector) {
            RigidBodyConnector rbc = (RigidBodyConnector)c;
            if (rbc.isActive()) {
               list.add (rbc);
            }
         }
         else if (c instanceof Constrainer && !(c instanceof CollisionManager)) {
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

   public void addGeneralMassBlocks (SparseBlockMatrix M) {
      updateLocalModels();
      for (MechSystemModel m : myLocalModels) {
         m.addGeneralMassBlocks (M);
      }
   }

   @Override
   public void getMassMatrixValues (SparseBlockMatrix M, VectorNd f, double t) {
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
      List<HasAuxState> list, int level) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof MechSystemModel) {
            ((MechSystemModel)c).getAuxStateComponents (list, level+1);
         }
         else if (c instanceof CollisionManager) {
            if (level == 0) {
               CollisionManager cm = (CollisionManager)c;
               // XXX Call updateCollisionHandlers() here, in order to get any
               // FEM surface mesh rebuilds out of the way. Rebuilds that
               // happen later will structure changes that will clear the
               // MechSystemBase component cache.
               cm.updateHandlers();
               list.addAll (cm.collisionHandlers());
               //list.add (cm);
            }
         }
         else if (c instanceof HasAuxState) {
            // this will include inactive RigidBodyConnecters;
            // that should be OK
            list.add ((HasAuxState)c);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyGetAuxStateComponents (
               (CompositeComponent)c, list, level);
         }
      }
   }

   public void getAuxStateComponents (List<HasAuxState> list, int level) {
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
      updateLocalAdvanceComponents();
      for (RequiresPrePostAdvance c : myLocalPrePostAdvanceComps) {
         c.preadvance (t0, t1, flags);
      }
      return super.preadvance (t0, t1, flags);
   }

   // public StepAdjustment advance (double t0, double t1, int flags) {

   //    initializeAdvance (t0, t1, flags);

   //    if (t0 == 0) {
   //       updateForces (t0);
   //    }

   //    if (!myDynamicsEnabled) {
   //       mySolver.nonDynamicSolve (t0, t1, myStepAdjust);
   //       recursivelyFinalizeAdvance (null, t0, t1, flags, 0);
   //    }
   //    else {
   //       if (t0 == 0 && myPrintState != null) {
   //          printState (myPrintState, 0);
   //       }
   //       checkState();
   //       mySolver.solve (t0, t1, myStepAdjust);
   //       DynamicComponent c = checkVelocityStability();
   //       if (c != null) {
   //          throw new NumericalException (
   //             "Unstable velocity detected, component " +
   //             ComponentUtils.getPathName (c));
   //       }
   //       recursivelyFinalizeAdvance (myStepAdjust, t0, t1, flags, 0);
   //       if (myPrintState != null) {
   //          printState (myPrintState, t1);
   //       }
   //    }

   //    finalizeAdvance (t0, t1, flags);
   //    return myStepAdjust;
   // }

   private void zeroExternalForces() {
      updateLocalDynamicComponents();
      for (int i=0; i<myLocalDynamicComps.size(); i++) {
         myLocalDynamicComps.get(i).zeroExternalForces();
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
      zeroExternalForces();
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

   public void updateBounds (Point3d pmin, Point3d pmax) {
      if (myMinBound != null) {
         myMinBound.updateBounds (pmin, pmax);
      }
      if (myMaxBound != null) {
         myMaxBound.updateBounds (pmin, pmax);
      }
      super.updateBounds (pmin, pmax);
   }

   public void render (GLRenderer renderer, int flags) {
   }

   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }

   protected void recursivelyTransformGeometry (
      CompositeComponent comp, 
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof TransformableGeometry) {
            ((TransformableGeometry)c).transformGeometry (X, topObject, flags);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyTransformGeometry (
               (CompositeComponent)c, X, topObject, flags);
         }
      }

   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

      recursivelyTransformGeometry (this, X, topObject, flags);

      //for (DynamicAttachment a : buildLocalAttachments()) {
      //   a.updateAttachment();
      //}
      if (myMinBound != null) {
         myMinBound.transform (X);
      }
      if (myMaxBound != null) {
         myMaxBound.transform (X);
      }
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

   protected void clearCachedData () {
      super.clearCachedData();
      mySolveMatrix = null;
      myDynamicComponents = null;
      myLocalDynamicComps = null;
      myLocalInitComps = null;
      myLocalPrePostAdvanceComps = null;
      myLocalModels = null;
      myCollisionManager.clearCachedData();
      myForcesNeedUpdating = true;
   }

   @Override
   protected void notifyStructureChanged (Object comp, boolean stateIsChanged) {
      if (comp == this) {
         updateHardwiredLists();
      }
      clearCachedData ();
      super.notifyStructureChanged (comp, stateIsChanged);
   }

   private void handleGeometryChange (GeometryChangeEvent e) {
   }

   public void componentChanged (ComponentChangeEvent e) {
      if (e.getCode() == ComponentChangeEvent.Code.GEOMETRY_CHANGED) { 
         // just update attachments
         handleGeometryChange ((GeometryChangeEvent)e);
      }
      else { // invalidate everything for now
         clearCachedData ();
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

}
