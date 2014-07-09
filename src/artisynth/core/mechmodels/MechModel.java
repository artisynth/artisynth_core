/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix;
import maspack.matrix.NumericalException;
import maspack.matrix.Point3d;
import maspack.matrix.SparseNumberedBlockMatrix;
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
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.PointFem3dAttachment;
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
   protected AxialSpringList<AxialSpring> myAxialSprings;
   protected PointSpringList<MultiPointSpring> myMultiPointSprings;
   protected ComponentList<FrameSpring> myFrameSprings;
   protected ComponentList<ForceComponent> myForceEffectors;
   protected ComponentList<RigidBody> myRigidBodies;
   protected ComponentList<MeshComponent> myMeshBodies;
   protected ComponentList<RigidBodyConnector> myConnectors;
   protected ComponentList<ConstrainerBase> myConstrainers;
   protected PointList<FrameMarker> myFrameMarkers;
   protected CollisionManager myCollisionManager;

   protected ArrayList<DynamicMechComponent> myLocalDynamicComps;
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

   protected boolean myProfiling = false;
   protected boolean myProfileUpdateForces = false;
   protected long myUpdateTime;
   protected long mySolveTime;

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
   protected static boolean DEFAULT_PROFILING = false;
   protected static PosStabilization DEFAULT_STABILIZATION =
      PosStabilization.Default;

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
      myProps.add (
         "profiling", "print time step information", DEFAULT_PROFILING);
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

      myCollisionManager = new CollisionManager();
      myCollisionManager.setName ("collisionManager");

      addFixed (myModels);
      addFixed (myParticles);
      addFixed (myRigidBodies);
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
    * This is a wrapper for the CollisionManager method
    * {@link CollisionManager#setDefaultCollisionBehavior(CollisionBehavior)
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
    * This is a wrapper for the CollisionManager method
    * {@link CollisionManager#setDefaultCollisionBehavior(CollisionBehavior)
    * setDefaultCollisionBehavior(behavior)}.
    * 
    * @param behavior desired collision behavior
    */
   public void setDefaultCollisionBehavior (CollisionBehavior behavior) {
      myCollisionManager.setDefaultCollisionBehavior (behavior);
   }

   /** 
    * Gets the default collision behavior, for this MechModel, 
    * for a specified pair of generic collidable types.
    * This is a wrapper for the CollisionManager method
    * {@link CollisionManager#getDefaultCollisionBehavior(Collidable,Collidable)
    * getDefaultCollisionBehavior(typeA,typeB)}.
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
    * pair of generic collidable types.  This is a wrapper for the
    * CollisionManager method {@link
    * CollisionManager#setDefaultCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setDefaultCollisionBehavior(typeA,typeB,behavior)}.
    *
    * @param typeA first generic collidable type
    * @param typeB second generic collidable type
    * @param behavior specified collision behavior
    */
   public void setDefaultCollisionBehavior (
      Collidable typeA, Collidable typeB, CollisionBehavior behavior) {

      myCollisionManager.setDefaultCollisionBehavior (typeA, typeB, behavior);
   }

   /**
    * Sets the default collision behavior, for this MechModel, for a specified
    * pair of generic collidable types.  This is a wrapper for the
    * CollisionManager method {@link
    * CollisionManager#setDefaultCollisionBehavior(Collidable,Collidable,CollisionBehavior)
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
    * Enables or disables collisions for a specified pair of Collidables,
    * overriding, if necessary, the primary collision behavior.  Each
    * collidable must be a particular component instance. If collisions are
    * enabled, the friction coefficient is set to 0. This is a wrapper for the
    * CollisionManager method {@link
    * CollisionManager#setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setCollisionBehavior(a,b,behavior)}.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @param enabled if true, enables collisions
    */
   public void setCollisionBehavior (
      Collidable a, Collidable b, boolean enabled) {

      setCollisionBehavior (a, b, new CollisionBehavior (enabled, /*mu=*/0));
   }

   /** 
    * Sets collision behavior for a specified pair of Collidables, overriding,
    * if necessary, the primary behavior. Each collidable must be a particular
    * component instance. This is a wrapper for the CollisionManager method
    * {@link
    * CollisionManager#setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setCollisionBehavior(a,b,behavior)}.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @param enabled if true, enables collisions
    * @param mu friction coefficient (ignored if enabled is false)
    */
   public void setCollisionBehavior (
      Collidable a, Collidable b, boolean enabled, double mu) {

      setCollisionBehavior (a, b, new CollisionBehavior (enabled, mu));
   }

   /** 
    * Sets collision behavior for a specified pair of Collidables, overriding,
    * if necessary, the primary behavior.  Each collidable must be a particular
    * component instance. This is a wrapper for the
    * CollisionManager method {@link
    * CollisionManager#setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * setCollisionBehavior(a,b,behavior)}.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @param behavior specified collision behavior
    */
   public void setCollisionBehavior (
      Collidable a, Collidable b, CollisionBehavior behavior) {
      myCollisionManager.setCollisionBehavior (a, b, behavior);
   }

   /** 
    * Returns the collision behavior for a specified pair of collidables.  Each
    * collidable must be a particular component instance.  Each collidable must
    * be a particular component instance. This is a wrapper for the
    * CollisionManager method {@link
    * CollisionManager#getCollisionBehavior(Collidable,Collidable)
    * getCollisionBehavior(a,b)}.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @return behavior for this pair of Collidables.
    */
   public CollisionBehavior getCollisionBehavior (Collidable a, Collidable b) {
      return myCollisionManager.getCollisionBehavior (a, b);
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

   public void attachPoint (Point p1, Particle p2) {
      if (p1.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      PointParticleAttachment ax = new PointParticleAttachment (p2, p1);
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

   /** 
    * Attaches a point to an FEM. The point in question is connected to either
    * its containing element, or projected to the nearest surface element.
    * 
    * @param p1 Point to connect. Must currently be a particle.
    * @param fem FemModel to connect the point to.
    * @param reduceTol if > 0, attempts to reduce the number of nodes
    * in the connection by removing any whose coordinate weights are
    * less than <code>reduceTol</code>. 
    */
   public void attachPoint (Point p1, FemModel3d fem, double reduceTol) {
      if (p1.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      if (!(p1 instanceof Particle)) {
         throw new IllegalArgumentException ("point is not a particle");
      }
      if (ComponentUtils.isAncestorOf (fem, p1)) {
         throw new IllegalArgumentException (
            "FemModel is an ancestor of the point");
      }
      Point3d loc = new Point3d();
      FemElement3d elem = fem.findNearestElement (loc, p1.getPosition());
      FemNode3d nearestNode = null;
      double nearestDist = Double.MAX_VALUE;
      for (FemNode3d n : elem.getNodes()) {
         double d = n.distance (p1);
         if (d < nearestDist) {
            nearestNode = n;
            nearestDist = d;
         }
      }
      if (nearestDist <= reduceTol) {
         // just attach to the node
         attachPoint (p1, nearestNode);
      }
      else {
         PointFem3dAttachment ax =
            PointFem3dAttachment.create (p1, elem, loc, reduceTol);
         // Coords are computed in createNearest.  the point's position will be
         // updated, if necessary, by the addRemove hook when the attachement is
         // added.
         addAttachment (ax);
      }
   }

   /** 
    * Attaches a point to the element of an FEM. The points coordinates
    * within the element are determined using its current position,
    * whether or not the point is outside the element.
    * @param p1 Point to connect. Must currently be a particle.
    * @param elem Element to connect the point to.
    */
   public void attachPoint (Point p1, FemElement3d elem) {
      if (p1.isAttached()) {
         throw new IllegalArgumentException ("point is already attached");
      }
      if (!(p1 instanceof Particle)) {
         throw new IllegalArgumentException ("point is not a particle");
      }
      if (ComponentUtils.isAncestorOf (elem.getGrandParent(), p1)) {
         throw new IllegalArgumentException (
            "Element's FemModel is an ancestor of the point");
      }
      PointFem3dAttachment ax = new PointFem3dAttachment (p1, elem);
      // call update attachment to compute the coordinates.
      // the point's position will be updated, if necessary, by the
      // addRemove hook when the attachment is added.
      ax.updateAttachment();
      addAttachment (ax);
   }

   public void attachPoint (Point p1, RigidBody body) {
      attachPoint (p1, body, null);
   }

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

   public static boolean isActive (DynamicMechComponent c) {
      return c.isActive();
   }

   protected void recursivelyGetDynamicComponents (
      CompositeComponent comp, 
      List<DynamicMechComponent> active, 
      List<DynamicMechComponent> attached,
      List<DynamicMechComponent> parametric) {
      
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof DynamicMechComponent) {
            DynamicMechComponent dm = (DynamicMechComponent)c;
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
      List<DynamicMechComponent> active, 
      List<DynamicMechComponent> attached,
      List<DynamicMechComponent> parametric) {

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
         else if (c instanceof Constrainer) {
            list.add ((Constrainer)c);
         }
         else if (c instanceof MechSystemModel) {
            ((MechSystemModel)c).getConstrainers (list, level+1);
         }
         else if (c instanceof CollisionManager) {
            if (level == 0) {
               ((CollisionManager)c).getConstrainers (list);
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

   protected void recursivelyGetAuxStateComponents (
      CompositeComponent comp,
      List<HasAuxState> list, int level) {

      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof MechSystemModel) {
            ((MechSystemModel)c).getAuxStateComponents (list, level+1);
         }
         else if (c instanceof HasAuxState) {
            // this will include inactive RigidBodyConnecters;
            // that should be OK
            list.add ((HasAuxState)c);
         }
         else if (c instanceof CollisionManager) {
            if (level == 0) {
               ((CollisionManager)c).addAuxStateComponents (list);
            }
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

   public synchronized String getPrintState() {
      return myPrintState;
   }

   public synchronized void setPrintState (String fmt) {
      myPrintState = fmt;
   }

   public synchronized PrintWriter openPrintStateFile (String name)
      throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
      }
      myPrintStateWriter = new PrintWriter (
         new BufferedWriter (new FileWriter (name)));
      return myPrintStateWriter;
   }

   public synchronized PrintWriter reopenPrintStateFile (String name)
      throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
      }
      myPrintStateWriter = new PrintWriter (
         new BufferedWriter (new FileWriter (name, /*append=*/true)));
      return myPrintStateWriter;
   }

   public synchronized void closePrintStateFile () throws IOException {
      if (myPrintStateWriter != null) {
         myPrintStateWriter.close();
      }
   }

   private synchronized void printState (String fmt, double t) {
      updateDynamicComponentLists();
      VectorNd x = new VectorNd (myActivePosStateSize);
      VectorNd v = new VectorNd (myActiveVelStateSize);
      getActivePosState (x, 0);
      // Hack: get vel in body coords until data is converted ...
      getActiveVelState (v, 0, /*bodyCoords=*/false);
      if (myPrintStateWriter == null) {
         System.out.println ("t="+t+":");
         System.out.println ("v: " + v.toString (fmt));
         System.out.println ("x: " + x.toString (fmt));
      }
      else {
         myPrintStateWriter.println ("t="+t+":");
         myPrintStateWriter.println ("v: " + v.toString (fmt));
         myPrintStateWriter.println ("x: " + x.toString (fmt));
         myPrintStateWriter.flush();
      }
   }

   private void checkState() {
//      updateDynamicComponents();
//      RootModel root = Main.getRootModel();
//      if (root.isCheckEnabled()) {
//         VectorNd x = new VectorNd (myActivePosStateSize);
//         VectorNd v = new VectorNd (myActiveVelStateSize);
//         getActivePosState (x, 0);
//         getActiveVelState (v, 0);
//         root.checkWrite ("v: " + v.toString ("%g"));
//         root.checkWrite ("x: " + x.toString ("%g"));
//      }
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

   public StepAdjustment advance (
      double t0, double t1, int flags) {
      // System.out.println ("advancing " + (t1sec-t0sec));

      if (myProfiling) {
         mySolveTime = System.nanoTime();
      }

      // if (t0 == 0) {
      //    mySolver.projectPosConstraints (t0);
      // }
      StepAdjustment stepAdjust = new StepAdjustment();

      if (!myDynamicsEnabled) {
         mySolver.nonDynamicSolve (t0, t1, stepAdjust);
         recursivelyFinalizeAdvance (null, t0, t1, flags, 0);
      }
      else {
         if (t0 == 0 && myPrintState != null) {
            printState (myPrintState, 0);
         }
         checkState();

         // this is a waste since we only need to update forces
         // if an external constraint acted on the system
         if (t0 == 0) {
            updateForces (t0);
         }
         mySolver.solve (t0, t1, stepAdjust);
         DynamicMechComponent c = checkVelocityStability();
         if (c != null) {
            throw new NumericalException (
               "Unstable velocity detected, component " +
               ComponentUtils.getPathName (c));
         }
         recursivelyFinalizeAdvance (stepAdjust, t0, t1, flags, 0);
         if (myPrintState != null) {
            printState (myPrintState, t1);
         }
      }

      if (myProfiling) {
         mySolveTime = System.nanoTime() - mySolveTime;
         System.out.println (
            "T1=" + t1 +
            " updateTime=" + myUpdateTime/1e6 + " solveTime=" + mySolveTime/1e6);
      }

      return stepAdjust;
   }

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

      for (DynamicAttachment a : buildLocalAttachments()) {
         a.updateAttachment();
      }
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

   public void setProfiling (boolean enable) {
      myProfiling = enable;
   }

   public boolean getProfiling() {
      return myProfiling;
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
   public DynamicMechComponent checkVelocityStability() {
      updateDynamicComponentLists(); // PARANOID
      for (int i=0; i<myNumActive; i++) {
         DynamicMechComponent c = myDynamicComponents.get(i);
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
         myLocalDynamicComps = new ArrayList<DynamicMechComponent>();
         
         //recursivelyGetLocalDynamicComponents (this, myLocalDynamicComponents);
         recursivelyGetLocalComponents (
            this, myLocalDynamicComps, DynamicMechComponent.class);        
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
               DynamicMechComponent c = myLocalDynamicComps.get(i); 
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
