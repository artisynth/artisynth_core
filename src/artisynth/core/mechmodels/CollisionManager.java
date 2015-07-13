/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.RenderableUtils;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.geometry.Vertex3d;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableCompositeBase;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;

/**
 * A special component that manages collisions between collidable bodies on
 * behalf of a MechModel.
 *
 * Because collision handling can be expensive, and also because it is not as
 * accurate as other aspects of the simulation (due largely to its
 * discontinuous nature), it is important to be able to control very precisely
 * how it is applied. The CollisionManager allows collision behavior to be
 * specified through 
 *
 * (a) default collision behaviors between generic body types (presently
 * Rigid and Deformable), and
 *
 * (b) specific behaviors between pairs of collidables which override
 * the default behaviors.
 *
 * The CollisionManager maintains:
 *
 * (1) A set of default behaviors;
 * 
 * (2) A set of override behaviors, which are stored as invisible
 * model components;
 * 
 * (3) A behavior map which describes the collision behavior for every
 * CollidableBody in the MechModel and which is updated on demand;
 *
 * (4) A set of CollisionHandlers which provides a collision handler for every
 * active collision behavior in the behavior map and which is also updated on
 * demand;
 *
 * Collidable components can be arranged hierarchically. Any component which is
 * a descendant of a Collidable A is known as a sub-collidable of A (a
 * sub-collidable does not need to be an immediate child; it only need to be a
 * descendant). Within a hierarchy, only the leaf nodes do that actual
 * colliding, and these should be instances of the sub-interface
 * CollidableBody. 
 *
 * Normally collidables within a hierarchy do not collide with each other.  The
 * exception is when either (a) self-collision is specified for one of the
 * ancestor nodes in the hierarchy, or (b) an explicit collision behavior is
 * set between members of the hierarchy. If a self-collision behavior is
 * specified for an ancestor node A, and A is deformable (i.e., its
 * isDeformable() method returns <code>true</code>), then that behavior will be
 * passed on to all pairs of sub-collidables of A for which A's method
 * allowSelfCollisions() returns true.
 * 
 * When a collision behavior is specified between two collidables A and B that
 * are *not* part of the same hierarchy, then that behavior is imparted to all
 * pairs of leaf-nodes located at or below A and B.
 */
public class CollisionManager extends RenderableCompositeBase 
   implements ScalableUnits, Constrainer {
   
   public static boolean DEFAULT_REDUCE_CONSTRAINTS = false;
   protected boolean myReduceConstraints = DEFAULT_REDUCE_CONSTRAINTS;

   CollisionHandlerList myCollisionHandlers;
   ArrayList<Constrainer> myConstrainers;
   protected boolean myCollisionHandlersValid = false;
   ComponentList<CollisionComponent> myCollisionComponents;
   // map from CollidablePairs to collision components giving default and
   // override collision behaviors
   CollisionComponent[] myDefaultBehaviors;
   HashMap<CollidablePair,CollisionBehavior> myBehaviorMap;
   boolean myMaskBehaviorMapClearP = false;

   protected static boolean myAllowSelfIntersections = true;

   private double myCollisionPointTolerance = -1;
   private double myCollisionRegionTolerance = -1;
   //private double myPenetrationLimit = -1;
   private double myContactNormalLen = Property.DEFAULT_DOUBLE;
   private boolean drawIntersectionContours = false;
   private boolean drawIntersectionFaces = false;
   private boolean drawIntersectionPoints = false;

   protected static final double DEFAULT_PENETRATION_TOL = 0.0001;
   protected double myPenetrationTol = DEFAULT_PENETRATION_TOL;
   protected PropertyMode myPenetrationTolMode = PropertyMode.Inherited;
   protected double myCollisionCompliance = 0;
   protected double myCollisionDamping = 0;
   protected double myCollisionAccel = 0;

   protected MechModel myMechModel;

   // Estimate of the radius of the set of collidable objects.
   // Used for computing default tolerances.
   protected double myCollisionArenaRadius = -1;

   public static PropertyList myProps =
      new PropertyList (CollisionManager.class, RenderableCompositeBase.class);

   static private RenderProps defaultRenderProps = new RenderProps();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);
      myProps.add ("reduceConstraints", "reduce the number of constraints", 
         DEFAULT_REDUCE_CONSTRAINTS);
      
      myProps.addInheritable (
         "penetrationTol:Inherited", "collision penetration tolerance",
         DEFAULT_PENETRATION_TOL);
      myProps.add (
         "collisionPointTol", "closness of collision points", -1);
      // myProps.add (
      //    "penetrationLimit", 
      //    "collision penetration limit for step reduction", -1);
      myProps.add (
         "contactNormalLen",
         "draw contact normals with indicated length", 0);
      myProps.add (
         "collisionAccel",
         "acceleration used to compute collision compliance from penetrationTol",
         0);
      myProps.add (
         "collisionCompliance",
         "compliance for collision response", 0, "[0,inf)");
      myProps.add (
         "collisionDamping",
         "damping for collision response", 0, "[0,inf)");
      myProps.add(
         "drawIntersectionFaces isDrawIntersectionFaces *", 
         "draw intersection faces", false);
      myProps.add(
         "drawIntersectionContours isDrawIntersectionContours *", 
         "draw intersection contours", false);
      myProps.add(
         "drawIntersectionPoints isDrawIntersectionPoints *", 
         "draw intersection points", false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private void setPenetrationTolInHandlers (double tol) {
      for (CollisionHandler ch : collisionHandlers()) {
         ch.setPenetrationTol (tol);
      }
   }
   /**
    * Sets the penetration tolerance for this component. Setting a
    * value of -1 will cause a default value to be computed based
    * on the radius of the topmost MechModel.
    *
    * @param tol new penetration tolerance 
    */
   public void setPenetrationTol (double tol) {
      if (tol < 0) {
         tol = getDefaultPenetrationTol (this, DEFAULT_PENETRATION_TOL);
      }
      if (myPenetrationTol != tol) {
         myPenetrationTol = tol;
         setPenetrationTolInHandlers (tol);
      }
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

   public static double getDefaultPenetrationTol (
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

   public double getCollisionArenaRadius () {
      if (myCollisionArenaRadius < 0) {
         if (myMechModel instanceof Renderable) {
            myCollisionArenaRadius =
               RenderableUtils.getRadius ((Renderable)myMechModel);
         }
         else {
            myCollisionArenaRadius = 0;
         }
      }
      return myCollisionArenaRadius;
   }

   public void setCollisionArenaRadius (double rad) {
      myCollisionArenaRadius = rad;
   }      

   public void setCollisionPointTol (double tol) {
      if (tol != myCollisionPointTolerance) {
         myCollisionPointTolerance = tol;
         tol = getCollisionPointTol(); // in case we need to compute default
         for (CollisionHandler ch : collisionHandlers()) {
            ch.setRigidPointTolerance (tol);
         }
      }
   }

   public double getCollisionPointTol () {
      if (myCollisionPointTolerance < 0) {
         myCollisionPointTolerance = 1e-2*getCollisionArenaRadius();
      }
      return myCollisionPointTolerance;
   }

   public void setCollisionRegionTol (double tol) {
      if (tol != myCollisionRegionTolerance) {
         myCollisionRegionTolerance = tol;
         tol = getCollisionRegionTol(); // in case we need to compute default
         for (CollisionHandler ch : collisionHandlers()) {
            ch.setRigidRegionTolerance (tol);
         }
      }
   }

   public double getCollisionRegionTol () {
      if (myCollisionRegionTolerance < 0) {
         myCollisionRegionTolerance = 1e-3*getCollisionArenaRadius();
      }
      return myCollisionRegionTolerance;
   }

   public void setContactNormalLen (double len) {
      if (len != myContactNormalLen) {
         myContactNormalLen = len;
      }
   }

    public double getContactNormalLen() {
      if (myContactNormalLen == Property.DEFAULT_DOUBLE) {
         myContactNormalLen = 0.1*getCollisionArenaRadius();
      }
      return myContactNormalLen;
   }

   public void setCollisionCompliance (double c) {
      if (c != myCollisionCompliance) {
         myCollisionCompliance = c;
         for (CollisionHandler ch : collisionHandlers()) {
            ch.setCompliance (c);
         }
      }
   }

   public double getCollisionCompliance() {
      return myCollisionCompliance;
   }

   public void setCollisionDamping (double d) {
      if (d != myCollisionDamping) {
         myCollisionDamping = d;
         for (CollisionHandler ch : collisionHandlers()) {
            ch.setDamping (d);
         }
      }
   }

   public double getCollisionDamping() {
      return myCollisionDamping;
   }

   public double getCollisionAccel() {
      return myCollisionAccel;
   }

   public void setCollisionAccel (double acc) {
      myCollisionAccel = acc;

      for (CollisionHandler ch : collisionHandlers()) {
         if (myCollisionAccel != 0) {
            ch.autoComputeCompliance (myCollisionAccel, getPenetrationTol());
         }
         else {
            ch.setCompliance (getCollisionCompliance());
            ch.setDamping (getCollisionDamping());
         }
      }
   }

   /**
    * Create a collision manager for a specific mech system.
    */
   public CollisionManager (MechModel mech) {

      myMechModel = mech;
      setRenderProps (createRenderProps());

      myCollisionHandlers =
         new CollisionHandlerList ("collisionHandlers", "ch");
      myCollisionHandlers.setNavpanelVisibility (NavpanelVisibility.HIDDEN);

      myCollisionComponents =
         new ComponentList<CollisionComponent> (
            CollisionComponent.class, "collisionComponents", "col");
      myCollisionComponents.setNavpanelVisibility (NavpanelVisibility.HIDDEN);

      // add collision handlers to the component hierarchy so that they will
      // inherit render properties 
      add (myCollisionHandlers);
      add (myCollisionComponents);

      initializeDefaultBehaviors();
   }

   public void clear() {
      setRenderProps (createRenderProps());
      myCollisionHandlers.removeAll();
      myCollisionComponents.removeAll();
      initializeDefaultBehaviors();
   }

   public void setFriction (double mu) {
      for (CollisionComponent r : myCollisionComponents) {
         r.setFriction (mu);
      }
   }

   static CollidablePair RIGID_RIGID =
      new CollidablePair (Collidable.RigidBody, Collidable.RigidBody);
   static CollidablePair DEFORMABLE_RIGID  =
      new CollidablePair (Collidable.Deformable, Collidable.RigidBody);
   static CollidablePair DEFORMABLE_DEFORMABLE  =
      new CollidablePair (Collidable.Deformable, Collidable.Deformable);
   static CollidablePair DEFORMABLE_SELF  =
      new CollidablePair (Collidable.Deformable, Collidable.Self);


   protected CollidablePair[] getDefaultPairs() {
      return new CollidablePair[] {
         RIGID_RIGID, 
         DEFORMABLE_RIGID, 
         DEFORMABLE_DEFORMABLE,
         DEFORMABLE_SELF
      };
   }

   private void initializeDefaultBehaviors() {
      CollidablePair[] pairs = getDefaultPairs();
      myDefaultBehaviors = new CollisionComponent[pairs.length];
      for (int i=0; i<pairs.length; i++) {
         myDefaultBehaviors[i] = 
            new CollisionComponent (pairs[i], new CollisionBehavior(false, 0));
      }
   }
   
   /** 
    * Specifies the default collision behavior for all default pairs
    * (RigidBody-RigidBody, RigidBody-Deformable, Deformable-Deformable,
    * Deformable-Self).
    * 
    * @param behavior desired collision behavior
    */
   public void setDefaultBehavior (CollisionBehavior behavior) {
      for (CollidablePair pair : getDefaultPairs()) {
         setDefaultBehavior (pair.getA(), pair.getB(), behavior);
      }
   }

   private CollisionBehavior getDefaultBehavior (CollidablePair pair) {
      CollisionComponent comp = null;
      for (int i=0; i<myDefaultBehaviors.length; i++) {
         if (myDefaultBehaviors[i].getPair().equals(pair)) {
            comp = myDefaultBehaviors[i];
            break;
         }
      }
      return comp != null ? comp.getBehavior() : null;
   }

   /** 
    * Gets the default collision behavior for a specified pair of generic
    * collidable types. Implements for
    * {@link MechModel#getDefaultCollisionBehavior
    * MechModel#getDefaultCollisionBehavior(typeA,typeB)}.
    * 
    * @param typeA first generic collidable type
    * @param typeB second generic collidable type
    * @return default collision behavior for the indicted collidable types.
    */
   public CollisionBehavior getDefaultCollisionBehavior (
      Collidable typeA, Collidable typeB) {

      CollidablePair pair = new CollidablePair (typeA, typeB);
      if (!pair.isValidDefault()) {
          throw new IllegalArgumentException (
             "Invalid generic collidable pair: "+pair.toString(getParent()));
      }
      CollisionBehavior behavior = getDefaultBehavior(pair);
      if (behavior == null) {
         throw new IllegalArgumentException (
            "No default behavior defined for "+pair);
      }
      return behavior;
   }

   /**
    * Sets the default collision behavior for a specified pair of generic
    * collidable types. Implements {@link
    * MechModel#setDefaultCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * MechModel.setDefaultCollisionBehavior(typeA,typeB,behavior)}.
    *
    * @param typeA first generic collidable type
    * @param typeB second generic collidable type
    * @param behavior specified collision behavior
    */
   public void setDefaultBehavior (
      Collidable typeA, Collidable typeB, CollisionBehavior behavior) {

      CollidablePair pair = new CollidablePair (typeA, typeB);
      if (!pair.isValidDefault()) {
          throw new IllegalArgumentException (
             "Invalid generic collidable pair: "+pair.toString(getParent()));
      }
      CollisionBehavior behav = getDefaultBehavior (pair);
      if (behav == null) {
         throw new IllegalArgumentException (
            "No default behavior defined for "+pair);
      }
      behav.set (behavior);
      notifyStructureChanged();
   }

   private CollidablePair validateCollidablePair (
      Collidable a, Collidable b, boolean forSetting) {

      CollidablePair pair = new CollidablePair (a, b);
      if (forSetting) {
         if (getParent() instanceof MechModel) {
         // make sure collidables both belong to the manager's MechModel
            MechModel mech = (MechModel)getParent();
            checkMechModelInclusion (mech, a);
            checkMechModelInclusion (mech, b);
         }
         if (ModelComponentBase.recursivelyContains (a, b)) {
            throw new IllegalArgumentException (
               "Collidable b is a descendant of collidable a");
         }
         if (ModelComponentBase.recursivelyContains (b, a)) {
            throw new IllegalArgumentException (
               "Collidable a is a descendant of collidable b");
         }
      }
      if (CollidablePair.isGeneric (a) ||
          CollidablePair.isGeneric (b)) {
         throw new IllegalArgumentException (
            "Generic collision pair "+
            pair.toString(getParent())+" not permitted");
      }
      if ((a==Collidable.Self && !b.isDeformable()) ||
          (b==Collidable.Self && !a.isDeformable())) {
         throw new IllegalArgumentException (
            "Illegal self-intersection pair "+
            pair.toString(getParent()));
      }
      if (a==Collidable.Self) {
         pair = new CollidablePair (a, a);
      }
      else if (b==Collidable.Self) {
         pair = new CollidablePair (b, b);
      }
      if (forSetting) {
         if (a == b) {
            if (!a.isDeformable()) {
               throw new IllegalArgumentException (
                  "Component "+ComponentUtils.getPathName(a)+
               " cannot self-intersect");
            }
         }
      }
      return pair;
   }

   private void checkMechModelInclusion (MechModel mech, Collidable col) {
      if (!ModelComponentBase.recursivelyContains (mech, col)) {
         throw new IllegalArgumentException (
            "Collidable "+ComponentUtils.getPathName(col) +
            " not contained within MechModel "+ComponentUtils.getPathName(mech));
      }
   }

   /** 
    * Sets collision behavior for a specified pair of Collidables, overriding,
    * if necessary, the primary behavior. Implements {@link
    * MechModel#setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * MechModel.setCollisionBehavior(a,b,behavior)}.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @param behavior specified collision behavior
    */
   public void setBehavior (
      Collidable a, Collidable b, CollisionBehavior behavior) {

      CollidablePair pair = validateCollidablePair (a, b, /*forSetting=*/true);
      setBehavior (pair, behavior);
   }
   
   /** 
    * Clears any collision behavior that has been defined for a specified
    * pair of Collidables to override the primary behavior. Implements
    * {@link MechModel#clearCollisionBehavior(Collidable,Collidable)
    * MechModel.clearCollisionBehavior(a,b)}.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @return <code>true</code> if an override behavior was specified
    * and removed for the indicated collidables.
    */
   public boolean clearCollisionBehavior (Collidable a, Collidable b) {

      CollidablePair pair = validateCollidablePair (a, b, /*forSetting=*/true);
      return clearBehavior (pair);
   }

   /** 
    * Clears any collision behaviors that have been defined to override
    * the default collision behaviors betweem pairs of Collidables.
    * Implements {@link MechModel#clearCollisionBehaviors()
    * MechModel.clearCollisionBehaviors()}.
    */
   public void clearCollisionBehaviors () {
      myCollisionComponents.clear();
   }
   
   private Collidable nearestCommonCollidableAncestor (
      Collidable a, Collidable b) {
         
      ModelComponent ancestor = ComponentUtils.findCommonAncestor (a, b);
      while (ancestor != null) {
         if (ancestor instanceof Collidable) {
            return (Collidable)ancestor;
         }
         ancestor = ancestor.getParent();
      }
      return null;      
   }
   
   /** 
    * Returns the CollisionCompenent in this model associated with
    * a particular collision override. If no such component exists,
    * <code>null</code> is returned. This method is provided mostly for
    * internal ArtiSynth use.
    * 
    * @param pair collidable pair
    * @return CollisionComponent in this model overriding collision
    * behavior for the specified pair, if any
    */
   public CollisionComponent getCollisionComponent (CollidablePair pair) {
      for (int i=0; i<myCollisionComponents.size(); i++) {
         CollisionComponent comp = myCollisionComponents.get(i);
         if (comp.getPair().equals (pair)) {
            return comp;
         }
      }     
      return null;
   }

   private CollisionBehavior getPrimaryCollisionBehavior (
      CollidableBody a, CollidableBody b) {
      MechModel lowModel = MechModel.lowestCommonModel (a, b);
      if (lowModel == null) {
         throw new IllegalArgumentException (
            "One or both components do not reside in this model:\n" +
            ComponentUtils.getPathName (a) + "\n" + 
            ComponentUtils.getPathName (b));
      }
      if (lowModel == myMechModel) {
         if (a == b) {
            // Self collision not currently supported for CollidableBody 
            return new CollisionBehavior (false, 0);
         }
         else if (a.isDeformable() != b.isDeformable()) {
            return getDefaultBehavior (DEFORMABLE_RIGID);
         }
         else if (a.isDeformable()) {
            return getDefaultBehavior (DEFORMABLE_DEFORMABLE);
         }
         else {
            return getDefaultBehavior (RIGID_RIGID);
         }
      }
      else {
         // climb to MechModel right below this one
         ModelComponent comp=lowModel.getParent();
         while (comp != getParent()) {
            if (comp instanceof MechModel) {
               lowModel = (MechModel)comp;
            }
            comp = comp.getParent();
         }
         CollisionManager cm = lowModel.getCollisionManager();
         return cm.getBehavior (new CollidablePair(a,b));
      }
   }

   /** 
    * Returns the collision behavior for a specified pair of collidables.  Each
    * collidable must be a particular component instance. Implements {@link
    * MechModel#getCollisionBehavior(Collidable,Collidable)
    * MechModel.getCollisionBehavior(a,b)}.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @return behavior for this pair of Collidables.
    */
   public CollisionBehavior getBehavior (Collidable a, Collidable b) {
      CollidablePair pair = validateCollidablePair (a, b, /*forSetting=*/false);
      return getBehavior (pair);
   }

   private CollisionHandler createCollisionHandler (
      CollidableBody c0, CollidableBody c1, CollisionBehavior behavior) {
      
      double mu = behavior.getFriction();
      CollisionHandler handler = new CollisionHandler (this, c0, c1, behavior);
      handler.setReduceConstraints (myReduceConstraints);
      handler.setRigidPointTolerance (getCollisionPointTol());
      handler.setRigidRegionTolerance (getCollisionRegionTol());

      double ptol = getPenetrationTol();
      handler.setPenetrationTol (ptol);
      if (myCollisionAccel != 0) {
         handler.autoComputeCompliance (myCollisionAccel, getPenetrationTol());
      }
      else {
         handler.setCompliance (getCollisionCompliance());
         handler.setDamping (getCollisionDamping());
      }
      return handler;
   }

   private boolean isCollidable (Collidable c) {
      return c.getCollidable() != Collidability.OFF;
   }

   private boolean isExternallyCollidable (Collidable c) {
      Collidability ca = c.getCollidable();
      return (ca == Collidability.ALL || ca == Collidability.EXTERNAL);
   }

   private boolean isInternallyCollidable (Collidable c) {
      Collidability ca = c.getCollidable();
      return (ca == Collidability.ALL || ca == Collidability.INTERNAL);
   }

   private void getExternallyCollidableBodies (
      List<CollidableBody> list, ModelComponent mc) {

      if (mc instanceof CompositeComponent) {
         CompositeComponent comp = (CompositeComponent)mc;
         for (int i=0; i<comp.numComponents(); i++) {
            ModelComponent c = comp.get (i);
            if (c instanceof Collidable) {
               Collidable col = (Collidable)c;
               if (isCollidableBody (col) && isExternallyCollidable (col)) {
                  list.add ((CollidableBody)c);
               }
            }
            getExternallyCollidableBodies (list, c);
         }
      }
   }

   private void getInternallyCollidableBodies (
      List<CollidableBody> list, ModelComponent mc) {

      if (mc instanceof CompositeComponent) {
         CompositeComponent comp = (CompositeComponent)mc;
         for (int i=0; i<comp.numComponents(); i++) {
            ModelComponent c = comp.get (i);
            if (c instanceof Collidable) {
               Collidable col = (Collidable)c;
               if (isCollidableBody (col) && isInternallyCollidable (col)) {
                  list.add ((CollidableBody)c);
               }
            }
            getInternallyCollidableBodies (list, c);
         }
      }
   }

   protected void updateHandlers() {
      updateBehaviorMap();
      if (!myCollisionHandlersValid) {
         myMaskBehaviorMapClearP = true;
         myCollisionHandlers.removeAll();
         
         ArrayList<Collidable> allCollidables = new ArrayList<Collidable> (1000);
         myMechModel.getCollidables (allCollidables, /*level=*/0);
         ArrayList<CollidableBody> collidables =
            new ArrayList<CollidableBody> (allCollidables.size());
         // reduce list to only collidable bodies
         for (Collidable c : allCollidables) {
            if (isCollidableBody(c)) {
               collidables.add ((CollidableBody)c);
            }
         }         
         
         //getAllCollidableBodies (collidables);

         for (int i=0; i<collidables.size(); i++) {
            CollidableBody ci = collidables.get(i);
            for (int j=i+1; j<collidables.size(); j++) {
               CollidableBody cj = collidables.get(j);
               CollisionBehavior behavior =
                  myBehaviorMap.get (new CollidablePair (ci, cj));
               if (behavior != null && behavior.isEnabled()) {
                  myCollisionHandlers.add (
                     createCollisionHandler (ci, cj, behavior));
               }
            }
         }
         myMaskBehaviorMapClearP = false;

         // This is a more efficient way to get the collision pairs, but it
         // gets them in a different order and so currently changes the results
         // of the regression testing:

         // for (Map.Entry<CollidablePair,CollisionBehavior> entry :
         //         myBehaviorMap.entrySet()) {
         //    Collidable a = entry.getKey().myCompA;
         //    Collidable b = entry.getKey().myCompB;
         //    System.out.println ("handler for "+
         //                        ComponentUtils.getPathName (a) + "-" +
         //                        ComponentUtils.getPathName (b));
         //    myCollisionHandlers.add (
         //       createCollisionHandler (a, b, entry.getValue())); 
         // }
         myConstrainers = new ArrayList<Constrainer>();
         for (CollisionHandler handler : myCollisionHandlers) {
            handler.setDrawIntersectionContours(drawIntersectionContours);
            handler.setDrawIntersectionFaces(drawIntersectionFaces);
            handler.setDrawIntersectionPoints(drawIntersectionPoints);
            myConstrainers.add (handler);
         }
         myCollisionHandlersValid = true;
      }
   }

   protected Collidable getCollidableAncestor (
      ModelComponent comp, ModelComponent top) {
      if (comp == top) {
         return null;
      }
      for (comp=comp.getParent(); comp != top; comp=comp.getParent()) {
         if (comp instanceof Collidable) {
            return (Collidable)comp;
         }
      }
      return null;
   }

   private boolean equal (CollisionBehavior b1, CollisionBehavior b2) {
      if ((b1 == null) != (b2 == null)) {
         return false;
      }
      else if (b1 == null) {
         return true;
      }
      else {
         return b1.equals (b2);
      }
   }         

   private void setBehaviorMap (CollidablePair pair, CollisionBehavior behavior) {
      if (!behavior.isEnabled()) {
         behavior = null;
      }      
      CollisionBehavior prev = myBehaviorMap.get (pair);
      if (!equal (prev, behavior)) {
         if (behavior == null) {
            myBehaviorMap.remove (pair);
         }
         else {
            myBehaviorMap.put (pair, behavior);
         }
      }
   }

   private CollisionBehavior getBehaviorMap (CollidablePair pair) {
      CollisionBehavior behavior = myBehaviorMap.get (pair);
      if (behavior == null) {
         return new CollisionBehavior(false, 0);
      }
      else {
         return behavior;
      }
   }

   private void applySelfIntersection (Collidable c, CollisionBehavior behavior) {
      ArrayList<CollidableBody> bodies = new ArrayList<CollidableBody>();
      if (isCollidableBody (c)) {
         throw new InternalErrorException (
            "Self-intersection not yet supported for individual Collidables");
      }
      if (isInternallyCollidable (c)) {
         getInternallyCollidableBodies (bodies, c);
         for (int i=0; i<bodies.size(); i++) {
            CollidableBody ai = bodies.get(i);
            for (int j=i+1; j<bodies.size(); j++) {
               CollidableBody aj = bodies.get(j);
               if (isInternallyCollidable(ai) &&
                   isInternallyCollidable(aj)) {
                  setBehaviorMap (new CollidablePair(ai, aj), behavior);
               }
               else {
                  setBehaviorMap (new CollidablePair(ai, aj),
                                  new CollisionBehavior(false, 0));
               }
            }
         }
      }
   }

   private void applyBehaviorOverride (
      CollidablePair pair, CollisionBehavior behavior) {

      Collidable a = pair.myCompA;
      Collidable b = pair.myCompB;
      if (isCollidableBody (a) && isCollidableBody (b)) {
         if (nearestCommonCollidableAncestor (a, b) != null) {
            // if a and b have a common ancester, INTERNAL collidability
            // must be enabled
            if (isInternallyCollidable (a) && isInternallyCollidable (b)) {
               setBehaviorMap (pair, behavior);
            }
         }
         else {
            // otherwise, EXTERNAL collidability must be enabled
            if (isExternallyCollidable (a) && isExternallyCollidable (b)) {
               setBehaviorMap (pair, behavior);
            }
         }
      }
      else if (a == b) {
         // self intersection case
         applySelfIntersection (a, behavior);
      }
      else {
         ArrayList<CollidableBody> bodiesA = new ArrayList<CollidableBody>();
         ArrayList<CollidableBody> bodiesB = new ArrayList<CollidableBody>();
         if (isCollidableBody (a) && isExternallyCollidable (a)) {
            bodiesA.add ((CollidableBody)a);
         }
         else if (isExternallyCollidable(a)) {
            getExternallyCollidableBodies (bodiesA, a);
         }
         if (isCollidableBody (b) && isExternallyCollidable (b)) {
            bodiesB.add ((CollidableBody)b);
         }
         else if (isExternallyCollidable(b)) {
            getExternallyCollidableBodies (bodiesB, b);
         }
         for (int i=0; i<bodiesA.size(); i++) {
            CollidableBody ai = bodiesA.get(i);
            for (int j=0; j<bodiesB.size(); j++) {
               CollidableBody bj = bodiesB.get(j);
               setBehaviorMap (new CollidablePair(ai, bj), behavior);
            }
         }
      }
   }

   private boolean clearBehavior (CollidablePair pair) {
      for (int i=0; i<myCollisionComponents.size(); i++) {
         if (myCollisionComponents.get(i).getPair().equals (pair)) {
            myCollisionComponents.remove (i);
            return true;
         }
      }
      return false;
   }
      
   public void setBehavior (CollidablePair pair, CollisionBehavior behavior) {
      updateBehaviorMap();
      myMaskBehaviorMapClearP = true;
      clearBehavior (pair);
      myCollisionComponents.add (new CollisionComponent (pair, behavior));
      myMaskBehaviorMapClearP = false;
      applyBehaviorOverride (pair, behavior);
   }

   private CollisionBehavior getBehavior (CollidablePair pair) {

      updateBehaviorMap();
      Collidable a = pair.myCompA;
      Collidable b = pair.myCompB;
      if (isCollidableBody (a) && isCollidableBody (b)) { // ALL
         return getBehaviorMap (pair);
      }
      else if (a == b) {
         // self intersection case
         ArrayList<CollidableBody> bodies = new ArrayList<CollidableBody>();
         if (isCollidableBody (a)) { // ALL
            throw new InternalErrorException (
               "Self-intersection not yet supported for MeshBodies");
         }
         CollisionBehavior behavior = null;
         if (isInternallyCollidable (a)) {
            getInternallyCollidableBodies (bodies, a);
            for (int i=0; i<bodies.size(); i++) {
               CollidableBody ai = bodies.get(i);
               for (int j=i+1; j<bodies.size(); j++) {
                  CollidableBody aj = bodies.get(j);
                  CollisionBehavior behav =
                     getBehaviorMap (new CollidablePair(ai, aj));
                  if (behavior == null) {
                     behavior = behav;
                  }
                  else if (!equal (behavior, behav)) {
                     return null;
                  }
               }
            }
         }
         return behavior == null ? new CollisionBehavior (false, 0) : behavior;
      }
      else {
         ArrayList<CollidableBody> bodiesA = new ArrayList<CollidableBody>();
         ArrayList<CollidableBody> bodiesB = new ArrayList<CollidableBody>();
         if (isCollidableBody (a) && isExternallyCollidable(a)) {
            bodiesA.add ((CollidableBody)a);
         }
         else if (isExternallyCollidable(a)) {
            getExternallyCollidableBodies (bodiesA, a);
         }
         if (isCollidableBody (b) && isExternallyCollidable(b)) {
            bodiesB.add ((CollidableBody)b);
         }
         else if (isExternallyCollidable(b)) {
            getExternallyCollidableBodies (bodiesB, b);
         }
         CollisionBehavior behavior = null;
         for (int i=0; i<bodiesA.size(); i++) {
            CollidableBody ai = bodiesA.get(i);
            for (int j=0; j<bodiesB.size(); j++) {
               CollidableBody bj = bodiesB.get(j);
               CollisionBehavior behav =
                  getBehaviorMap (new CollidablePair(ai, bj));
               if (behavior == null) {
                  behavior = behav;
               }
               else if (!equal (behavior, behav)) {
                  return null;
               }
            }
         }
         return behavior == null ? new CollisionBehavior (false, 0) : behavior;
      }
   }

   // /**
   //  * Collect all the CollidableBodies located under this manager's
   //  * MechModel.
   //  */
   // private void getAllCollidableBodies (List<CollidableBody> list) {
   //    if (myMechModel != null) {
   //       ArrayList<Collidable> allCollidables = new ArrayList<Collidable>(100);
   //       myMechModel.getCollidables (allCollidables, /*level=*/0);
   //       // return only collidable bodies
   //       for (Collidable c : allCollidables) {
   //          if (c instanceof CollidableBody &&
   //             // XXX hack until RigidBody and RigidCompositeBody are merged
   //             !(c instanceof RigidCompositeBody)) {
   //             list.add ((CollidableBody)c);
   //          }
   //       }
   //    }      
   // }

   private boolean isCollidableBody (Collidable c) {
      // XXX Need to call isCollidable() first, because FemModel3d uses that to
      // trigger on-demand surface mesh generation, without which the actual
      // FemMesh object containing the surface will have a null mesh.
      if (!isCollidable(c)) {
         return false;
      }
      else if (c instanceof CollidableBody &&
         // XXX hack until RigidBody and RigidCompositeBody are merged
         !(c instanceof RigidCompositeBody)) {
         CollidableBody body = (CollidableBody)c;
         return (body.getCollisionMesh() != null);
      }
      else {
         return false;
      }
   }
   
   private String getName (Collidable c) {
      if (CollidablePair.isGeneric(c)) {
         return c.toString();
      }
      else {
         return ComponentUtils.getPathName(c);
      }
   }

   private String getName (CollidablePair pair) {
      return getName(pair.myCompA) + "-" + getName(pair.myCompB);
   }      

   private MechModel nearestMechModel (ModelComponent c) {
      while (c != null) {
         if (c instanceof MechModel) {
            return (MechModel)c;
         }               
         c = c.getParent();
      }
      return null;
   }         

   private void updateBehaviorMap() {
      if (myBehaviorMap == null) {
         // mask map clearing since calls to isCollidable() might trigger
         // structure change events: e.g., auto generation of surface meshes in
         // FemModel3d
         myMaskBehaviorMapClearP = true;
         myBehaviorMap = new HashMap<CollidablePair,CollisionBehavior>();

         ArrayList<Collidable> collidables = new ArrayList<Collidable> (1000);
         myMechModel.getCollidables (collidables, /*level=*/0);
         //getAllCollidableBodies (collidables);

         CollisionBehavior selfBehavior = getDefaultBehavior (DEFORMABLE_SELF);
         if (!selfBehavior.isEnabled()) {
            selfBehavior = null;
         }

         // start by setting primary behavior
         for (int i=0; i<collidables.size(); i++) {
            Collidable ci = collidables.get(i);
            if (isCollidableBody(ci)) {
               for (int j=i+1; j<collidables.size(); j++) {
                  Collidable cj = collidables.get(j);
                  if (isCollidableBody(cj)) {
                     CollisionBehavior behavior =
                        getPrimaryCollisionBehavior (
                           (CollidableBody)ci, (CollidableBody)cj);
                     if (nearestCommonCollidableAncestor (ci, cj) == null) {
                        if (isExternallyCollidable (ci) &&
                            isExternallyCollidable (cj) &&
                            behavior != null && behavior.isEnabled()) {
                           // make sure behavior is a copy or
                           myBehaviorMap.put (
                              new CollidablePair (ci, cj), behavior);
                        }
                     }
                  }
               } 
            }
            else {
               if (ci.isDeformable() &&
                   selfBehavior != null &&
                   nearestMechModel(ci) == myMechModel) {
                  applySelfIntersection (ci, selfBehavior);
               }
            }
         }
         // now add in override behaviors
         for (CollisionComponent cc : myCollisionComponents) {
            applyBehaviorOverride (cc.getPair(), cc.getBehavior());
         }
         myMaskBehaviorMapClearP = false;
      }
   }

   /**
    * If necessary, rebuilds the collison pair list from the specified
    * contact info.
    */
   public CollisionHandlerList collisionHandlers() {
      updateHandlers();
      return myCollisionHandlers;
   }

   /**
    * Returns the collision handler for a specific pair of CollidableBodys, or
    * <code>null</code> if no collisions are enabled for that pair.  The query
    * is symmetric, so that <code>getCollisionHandler(A,B)</code> and
    * <code>getCollisionHandler(B,A)</code> will return the same value.
    *
    * <p> Note that collision handlers are currently regenerated whenever the
    * set of enabled collisions between objects changes. Any handler returned
    * by this method prior to such a change will then become invalid.
    *
    * @param colA first collidable
    * @param colB second collidable
    * @return collision handler (if any) for A and B.
    */
   public CollisionHandler getCollisionHandler (
      CollidableBody colA, CollidableBody colB) {
      updateHandlers();
      for (CollisionHandler handler : myCollisionHandlers) {
         if ((handler.myCollidable0 == colA && handler.myCollidable1 == colB) ||
             (handler.myCollidable0 == colB && handler.myCollidable1 == colA)) {
            return handler;
         }
      }
      return null;
   }

   /**
    * Returns a map specifying the contact impulses acting on the
    * CollidableBody <code>colA</code> in response to contact with another
    * CollidableBody <code>colB</code>. This method currently requires that
    * <code>colA</code> is a deformable body. If it is not, or if no collisions
    * are enabled between <code>colA</code> and <code>colB</code>, then
    * <code>null</code> is returned.
    *
    * <p>The map gives the most recently computed impulses acting on each
    * vertex of the collision mesh of <code>colA</code> (this is the same mesh
    * returned by {@link CollidableBody#getCollisionMesh}). Vertices for which
    * no impulses were computed do not appear in the map. To turn the impulses
    * into forces, one must divide by the current step size.
    *
    * <p>Contact impulses give the forces that arise in order to prevent
    * further interpenetration between <code>colA</code> and <code>colB</code>.
    * They do <i>not</i> include impulses that are computed to separate
    * <code>colA</code> and <code>colB</code> when they initially come into
    * contact.
    *
    * @param colA first collidable
    * @param colB second collidable
    * @return if appropriate, map giving the contact impulses acting on
    * <code>colA</code> in response to <code>colB</code>.
    */
   public Map<Vertex3d,Vector3d> getContactImpulses (
      CollidableBody colA, CollidableBody colB) {

      if (!colA.isDeformable()) {
         return null;
      }
      updateHandlers();
      CollisionHandler handler = null;
      for (CollisionHandler h : myCollisionHandlers) {
         if ((h.myCollidable0 == colA && h.myCollidable1 == colB) ||
             (h.myCollidable0 == colB && h.myCollidable1 == colA)) {
            handler = h;
         }
      }      
      if (handler == null) {
         return null;
      }
      else {
         return handler.getContactImpulses();
      }
   }

   public ComponentList<CollisionComponent> collisionComponents() {
      return myCollisionComponents;
   }

   protected void notifyStructureChanged () {
      myCollisionHandlersValid = false;
      if (!myMaskBehaviorMapClearP) {
         myBehaviorMap = null;
      }
      notifyParentOfChange (new StructureChangeEvent (this));
   }

   public void initialize() {
      for (CollisionHandler ch : collisionHandlers()) {
         ch.initialize();
      }      
   }

   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createRenderProps (this);
      props.setVisible (false);
      return props;
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      myCollisionHandlers.updateBounds (pmin, pmax);
   }

   public void prerender (RenderList list) {
      list.addIfVisible (collisionHandlers());
   }

   public void render (GLRenderer renderer, int flags) {
   }

   public void scaleDistance (double s) {
      if (myCollisionPointTolerance != -1) {
         myCollisionPointTolerance *= s;
      }
      if (myCollisionRegionTolerance != -1) {
         myCollisionRegionTolerance *= s;
      }
      if (myPenetrationTol != -1) {
         myPenetrationTol *= s;
      }
      myCollisionHandlersValid = false;
   }

   public void scaleMass (double s) {
      myCollisionDamping *= s;
      myCollisionCompliance /= s;
      myCollisionHandlersValid = false;
   }

   public void clearCachedData () {
      myCollisionHandlersValid = false;
      if (!myMaskBehaviorMapClearP) {
         myBehaviorMap = null;
      }
   }

   protected void writeDefaultBehaviors (PrintWriter pw, NumberFormat fmt) {
      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (CollisionComponent comp : myDefaultBehaviors) {
         CollidablePair pair = comp.getPair();
         CollisionBehavior behavior = comp.getBehavior();
         pw.println (
            pair.getA()+" "+pair.getB()+" "+
            behavior.isEnabled()+" "+fmt.format(behavior.getFriction()));
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   protected void scanDefaultBehaviors (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         Collidable typeA = scanDefaultCollidable (rtok);
         Collidable typeB = scanDefaultCollidable (rtok);
         boolean enabled = rtok.scanBoolean();
         double mu = rtok.scanNumber();
         setDefaultBehavior (
            typeA, typeB, new CollisionBehavior (enabled, mu));
      }
   }

   Collidable scanDefaultCollidable (ReaderTokenizer rtok) throws IOException {
      if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
         if (rtok.sval.equals ("RigidBody")) {
            return Collidable.RigidBody;
         }
         else if (rtok.sval.equals ("Deformable")) {
            return Collidable.Deformable;
         }
         else if (rtok.sval.equals ("Self")) {
            return Collidable.Self;
         }
         else {
            throw new IOException (
               "default collidable type "+rtok.sval+" not recognized, " + rtok);
         }
      }
      else {
         throw new IOException (
            "expected name of default collidable (RigidBody, Deformable, Self): "
            + rtok);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (scanAttributeName (rtok, "defaultBehaviors")) {
         scanDefaultBehaviors (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }   

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      pw.print ("defaultBehaviors=");
      writeDefaultBehaviors (pw, fmt);
      // only write out the collision components
      myComponents.writeComponentByName (
         pw, myCollisionComponents, fmt, ancestor);
   }

   @Override
   public void componentChanged (ComponentChangeEvent e) {
      if (!myMaskBehaviorMapClearP) {
         myBehaviorMap = null;
         myCollisionHandlersValid = false;            
      }
      if (e.getComponent() != myCollisionHandlers) {
         // no need to notify parent about changes to the collision handler
         // list; those are maintained as sub-components simply so that they can
         // inherit render properties
         notifyParentOfChange (e);
      }
   }

   public void setDrawIntersectionContours(boolean set) {
      drawIntersectionContours = set;
      for (CollisionHandler handler : collisionHandlers()) {
         handler.setDrawIntersectionContours(set);
      }
   }

   public boolean isDrawIntersectionContours() {
      return drawIntersectionContours;
   }

   public void setDrawIntersectionFaces(boolean set) {
      drawIntersectionFaces = set;
      for (CollisionHandler handler : collisionHandlers()) {
         handler.setDrawIntersectionFaces(set);
      }
   }

   public boolean isDrawIntersectionFaces() {
      return drawIntersectionFaces;
   }

   public void setDrawIntersectionPoints(boolean set) {
      drawIntersectionPoints = set;
      for (CollisionHandler handler : collisionHandlers()) {
         handler.setDrawIntersectionPoints(set);
      }
   }

   public boolean isDrawIntersectionPoints() {
      return drawIntersectionPoints;
   }
   
   public void setReduceConstraints(boolean set) {
      myReduceConstraints = set;
      for (CollisionHandler ch : collisionHandlers()) {
         ch.setReduceConstraints(set);
      }
   }
   
   public boolean getReduceConstraints() {
      return myReduceConstraints;
   }

   // ==== Begin Constrainer implementation ====

   public void getBilateralSizes (VectorNi sizes) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         myConstrainers.get(i).getBilateralSizes (sizes);
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         numb = myConstrainers.get(i).addBilateralConstraints (
            GT, dg, numb);
      }
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getBilateralInfo (ginfo, idx);
      }
      // if (idx-idx0 > 0) {
      //    for (int i=idx0; i<idx; i++) {
      //       System.out.println ("dist["+i+"]=" + ginfo[i].dist);
      //    }
      // }
      return idx;
   }

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).setBilateralImpulses (lam, h, idx);
      }
      return idx;
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getBilateralImpulses (lam, idx);
      }
      return idx;
   }
   
   public void zeroImpulses() {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         myConstrainers.get(i).zeroImpulses();
      }
   }

   public void getUnilateralSizes (VectorNi sizes) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         myConstrainers.get(i).getUnilateralSizes (sizes);
      }
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         numu = myConstrainers.get(i).addUnilateralConstraints (
            NT, dn, numu);
      }
      return numu;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getUnilateralInfo (ninfo, idx);
      }
      return idx;
   }

   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).setUnilateralImpulses (the, h, idx);
      }
      return idx;
   }

   public int getUnilateralImpulses (VectorNd the, int idx) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         idx = myConstrainers.get(i).getUnilateralImpulses (the, idx);
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      updateHandlers();
      int max = 0;
      for (int i=0; i<myConstrainers.size(); i++) {      
         max += myConstrainers.get(i).maxFrictionConstraintSets();
      }
      return max;
   }
   
   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {
      updateHandlers();
      for (int i=0; i<myConstrainers.size(); i++) {
         numf = myConstrainers.get(i).addFrictionConstraints (DT, finfo, numf);
      }
      return numf;
   }


   private class DescendingDistance implements Comparator<ContactConstraint> {

      public int compare (ContactConstraint c0, ContactConstraint c1) {
         if (c0.myDistance < c1.myDistance) {
            return -1;
         }
         else if (c0.myDistance == c1.myDistance) {
            return 0;
         }
         else {
            return 1;
         }
      }

      public boolean equals (Object obj) {
         return false;
      }
   }

   protected int numUnmarkedMasters (ContactConstraint c, boolean[] marked) {
      int num = 0;
      ArrayList<ContactMaster> masters = c.getMasters();
      for (int i=0; i<masters.size(); i++) {
         int idx = masters.get(i).myComp.getSolveIndex();
         if (idx < marked.length && marked[idx] == false) {
            num++;
         }
      }
      return num;
   }

   protected void markMasters (ContactConstraint c, boolean[] marked) {
      ArrayList<ContactMaster> masters = c.getMasters();
      for (int i=0; i<masters.size(); i++) {
         int idx = masters.get(i).myComp.getSolveIndex();
         if (idx < marked.length && marked[idx] == false) {
            marked[idx] = true;
         }
      }
   }

   public void reduceBilateralConstraints () {
      ArrayList<ContactConstraint> bilaterals =
         new ArrayList<ContactConstraint>();
      for (int i=0; i<myCollisionHandlers.size(); i++) {
         CollisionHandler handler =
            myCollisionHandlers.get(i);
         handler.getBilateralConstraints (bilaterals);
      }        
      Collections.sort (bilaterals, new DescendingDistance());
      // System.out.println ("REDUCING");
      // for (int i=0; i<bilaterals.size(); i++) {
      //    System.out.println (bilaterals.get(i).myDistance);
      // }
      int[] dofs = new int[myMechModel.numActiveComponents()];
      myMechModel.getDynamicSizes (dofs);

      boolean[] marked = new boolean[myMechModel.numActiveComponents()];

      // make sure each constraint has at least one unmarked active master
      for (int i=0; i<bilaterals.size(); i++) {
         ContactConstraint c = bilaterals.get(i);
         if (numUnmarkedMasters (c, marked) == 0) {
            c.setActive (false);
         }
         else {
            markMasters (c, marked);
         }
      }
   }

   public double updateConstraints (double t, int flags) {
      updateHandlers();
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      double maxpen = 0;
      for (int i=0; i<myCollisionHandlers.size(); i++) {
         CollisionHandler handler = myCollisionHandlers.get(i);
         double pen = handler.updateConstraints (t, flags);
         if (pen > maxpen) {
            maxpen = pen;
         }
      }
      timer.stop();

      if (myReduceConstraints) {
         reduceBilateralConstraints();
      }

      for (int i=0; i<myCollisionHandlers.size(); i++) {
         myCollisionHandlers.get(i).removeInactiveContacts();
         // System.out.println (
         //    "numc: " +
         //    myCollisionHandlers.get(i).myBilaterals0.size() + " " + 
         //    myCollisionHandlers.get(i).myBilaterals1.size());
      }
      
      return maxpen;
   }

   // // ***** Begin HasAuxState *****

   // public void advanceAuxState (double t0, double t1) {
   //    // contact constraints don't need to advance their aux state
   // }

   // public void skipAuxState (DataBuffer data) {
   //    updateHandlers();
   //    for (int i=0; i<myCollisionHandlers.size(); i++) {
   //       myCollisionHandlers.get(i).skipAuxState (data);
   //    }
   // }

   // public void getAuxState (DataBuffer data) {
   //    updateHandlers();
   //    for (int i=0; i<myCollisionHandlers.size(); i++) {
   //       myCollisionHandlers.get(i).getAuxState (data);
   //    }
   // }

   // public void getInitialAuxState (DataBuffer newData, DataBuffer oldData) {
   //    updateHandlers();
   //    for (int i=0; i<myCollisionHandlers.size(); i++) {
   //       myCollisionHandlers.get(i).getInitialAuxState (newData, oldData);
   //    }
   // }

   // public void setAuxState (DataBuffer data) {
   //    updateHandlers();
   //    for (int i=0; i<myCollisionHandlers.size(); i++) {
   //       CollisionHandler ch = myCollisionHandlers.get(i);
   //       ch.setAuxState (data);
   //    }
   // }

   // // ***** End HasAuxState *****
}
