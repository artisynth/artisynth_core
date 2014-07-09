/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import maspack.matrix.Point3d;
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
import artisynth.core.femmodels.FemMesh;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableCompositeBase;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;

/**
 * A special component the manages collisions between collidable bodies.
 */
public class CollisionManager extends RenderableCompositeBase 
   implements ScalableUnits {
   
   public static boolean DEFAULT_USE_NEW_HANDLER = true;
   
   protected boolean myUseNewHandler = DEFAULT_USE_NEW_HANDLER;
   
   CollisionHandlerList myCollisionHandlers;
   protected boolean myCollisionHandlersValid = false;
   ComponentList<CollisionComponent> myCollisionComponents;
   HashMap<CollidablePair,CollisionComponent> myCollisionComponentMap;
   boolean myCollisionComponentMapValidP = false;

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
      myProps.add ("useNewHandler", "Use the new collision handler that allows" +
         " for collisions with embedded meshes", DEFAULT_USE_NEW_HANDLER);
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
      for (CollisionHandler ch : myCollisionHandlers) {
         ch.setPenetrationTol (tol);
      }
   }

   // public void setPenetrationTol (double tol) {
   //    if (tol != myPenetrationTol) {
   //       System.out.println ("setting " + tol);
   //       myPenetrationTol = tol;
   //       if (tol != -1) {
   //          setPenetrationTolInHandlers (tol);
   //       }
   //    }
   // }

   // public double getPenetrationTol() {
   //    if (myPenetrationTol == -1) {
   //       double tol = 0.0001;
   //       double radius = getCollisionArenaRadius();
   //       if (radius != 0) {
   //          tol = 1e-4*radius;
   //       }
   //       myPenetrationTol = tol;
   //       setPenetrationTolInHandlers (tol);
   //    }
   //    return myPenetrationTol;
   // }
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
         if (getParent() instanceof Renderable) {
            myCollisionArenaRadius =
               RenderableUtils.getRadius ((Renderable)getParent());
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
         for (CollisionHandler ch : myCollisionHandlers) {
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
         for (CollisionHandler ch : myCollisionHandlers) {
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
         for (CollisionHandler ch : myCollisionHandlers) {
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
         for (CollisionHandler ch : myCollisionHandlers) {
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

      for (CollisionHandler ch : myCollisionHandlers) {
         if (myCollisionAccel != 0) {
            ch.autoComputeCompliance (myCollisionAccel, getPenetrationTol());
         }
         else {
            ch.setCompliance (getCollisionCompliance());
            ch.setDamping (getCollisionDamping());
         }
      }
   }

   public CollisionManager() {

      setRenderProps (createRenderProps());

      myCollisionHandlers =
         new CollisionHandlerList ("collisionHandlers", "ch");
      myCollisionHandlers.setNavpanelVisibility (NavpanelVisibility.HIDDEN);

      myCollisionComponents =
         new ComponentList<CollisionComponent> (
            CollisionComponent.class, "collisionComponents", "col");
      myCollisionComponents.setNavpanelVisibility (NavpanelVisibility.HIDDEN);

      add (myCollisionHandlers);
      add (myCollisionComponents);

      initializeCollisionComponentMap();
   }

   public void clear() {
      setRenderProps (createRenderProps());
      myCollisionHandlers.removeAll();
      myCollisionComponents.removeAll();
      initializeCollisionComponentMap();
   }

   public void setFriction (double mu) {
      for (CollisionComponent r : myCollisionComponents) {
         r.setFriction (mu);
      }
   }

   protected CollidablePair[] getDefaultPairs() {
      return new CollidablePair[] {
         new CollidablePair (Collidable.RigidBody, Collidable.RigidBody),
         new CollidablePair (Collidable.Deformable, Collidable.RigidBody),
         new CollidablePair (Collidable.Deformable, Collidable.Deformable),
         new CollidablePair (Collidable.Deformable, Collidable.Self)
      };
   }

   protected HashMap<CollidablePair,CollisionComponent>
      getCollisionComponentMap() {
      if (!myCollisionComponentMapValidP) {
         updateCollisionComponentMap();
         myCollisionComponentMapValidP = true;
      }
      return myCollisionComponentMap;
   }

   protected void updateCollisionComponents() {
   }

   private void updateCollisionComponentMap () {
      // rebuild the collision component map from the (possibly changed) set of
      // CollisionComponents, while keeping the existing default behaviors.
      HashMap<CollidablePair,CollisionComponent> newmap =
         new HashMap<CollidablePair,CollisionComponent>();
      for (CollidablePair pair : myCollisionComponentMap.keySet()) {
         if (pair.isValidDefault()) {
            newmap.put (pair, myCollisionComponentMap.get(pair));
         }
      }
      for (int i=0; i<myCollisionComponents.size(); i++) {
         CollisionComponent r = myCollisionComponents.get(i);
         newmap.put (r.myPair, r);
      }
      myCollisionComponentMap = newmap;
   }

   private void initializeCollisionComponentMap () {
      myCollisionComponentMap = new HashMap<CollidablePair,CollisionComponent>();
      for (CollidablePair pair : getDefaultPairs()) {
         addDefaultCollisionBehavior (pair.getA(), pair.getB(), false, 0);
      }
   }
   
   private void addDefaultCollisionBehavior (
      Collidable typeA, Collidable typeB, boolean enabled, double mu) {
      CollidablePair pair = new CollidablePair (typeA, typeB);
      if (!pair.isValidDefault()) {
          throw new IllegalArgumentException (
             "Invalid generic collidable pair: "+pair.toString(getParent()));
      }
      CollisionComponent comp = 
         new CollisionComponent (pair, new CollisionBehavior (enabled, mu));
      myCollisionComponentMap.put (pair, comp);
   }
   
   /** 
    * Specifies the default collision behavior for all default pairs
    * (RigidBody-RigidBody, RigidBody-Deformable, Deformable-Deformable,
    * Deformable-Self).
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
    * Specifies the default collision behavior for all default pairs
    * (RigidBody-RigidBody, RigidBody-Deformable, Deformable-Deformable,
    * Deformable-Self).
    * 
    * @param behavior desired collision behavior
    */
   public void setDefaultCollisionBehavior (CollisionBehavior behavior) {

      for (CollidablePair pair : getDefaultPairs()) {
         setDefaultCollisionBehavior (pair.getA(), pair.getB(), behavior);
      }
   }

   /** 
    * Gets the default collision behavior,
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

      CollidablePair pair = new CollidablePair (typeA, typeB);
      if (!pair.isValidDefault()) {
          throw new IllegalArgumentException (
             "Invalid generic collidable pair: "+pair.toString(getParent()));
      }
      CollisionComponent comp = getCollisionComponentMap().get (pair);
      if (comp == null) {
         throw new IllegalArgumentException (
            "No default behavior defined for "+pair);
      }
      return comp.getBehavior();
   }

   /**
    * Sets the default collision behavior 
    * for a specified pair of generic collidable types.
    * This is a convenience wrapper for {@link
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
    * Sets the default collision behavior
    * for a specified pair of generic collidable types. 
    * Allowed collidable types are {@link Collidable#RigidBody} and
    * {@link Collidable#Deformable}. In addition, the type
    * {@link Collidable#Self} can be paired with {@link Collidable#Deformable}
    * to set the default behavior for deformable self collisions;
    * {@link Collidable#Self} cannot be paired with other types.
    *
    * @param typeA first generic collidable type
    * @param typeB second generic collidable type
    * @param behavior specified collision behavior
    */
   public void setDefaultCollisionBehavior (
      Collidable typeA, Collidable typeB, CollisionBehavior behavior) {

      CollidablePair pair = new CollidablePair (typeA, typeB);
      if (!pair.isValidDefault()) {
          throw new IllegalArgumentException (
             "Invalid generic collidable pair: "+pair.toString(getParent()));
      }
      CollisionComponent comp = getCollisionComponentMap().get (pair);
      if (comp == null) {
         throw new IllegalArgumentException (
            "No default behavior defined for "+pair);
      }
      comp.setBehavior (behavior);
      notifyStructureChanged();
   }

   public void setCollisionBehavior ( 
      Collidable a, Collidable b, boolean enabled) {
      setCollisionBehavior(
         a, b, new CollisionBehavior(enabled, /*mu=*/0));
   }

   private boolean isDeformableModel (ModelComponent c) {
      return !(c instanceof RigidBody);
   }

   private CollidablePair validateCollidablePair (Collidable a, Collidable b) {
      CollidablePair pair = new CollidablePair (a, b);
      return validateCollidablePair (pair);
   }

   private CollidablePair validateCollidablePair (CollidablePair pair) {
      if (CollidablePair.isGeneric (pair.myCompA) ||
          CollidablePair.isGeneric (pair.myCompB)) {
         throw new IllegalArgumentException (
            "Generic collision pair "+
            pair.toString(getParent())+" not permitted");
      }
      if ((pair.myCompA==Collidable.Self && !isDeformableModel(pair.myCompB)) ||
          (pair.myCompB==Collidable.Self && !isDeformableModel(pair.myCompA))) {
         throw new IllegalArgumentException (
            "Illegal self-intersection pair "+
            pair.toString(getParent()));
      }
      if (pair.myCompA==Collidable.Self) {
         pair = new CollidablePair (pair.myCompA, pair.myCompA);
      }
      else if (pair.myCompB==Collidable.Self) {
         pair = new CollidablePair (pair.myCompB, pair.myCompB);
      }
      if (pair.myCompA == pair.myCompB) {
         if (!isDeformableModel(pair.myCompA)) {
            throw new IllegalArgumentException (
               "Component "+ComponentUtils.getPathName(pair.myCompA)+
               " cannot self-intersect");
         }
      }
      return pair;
   }

   /** 
    * Sets collision behavior for a specified pair of Collidables, overriding,
    * if necessary, the primary behavior. Each collidable must be a particular
    * component instance. This is a convenience wrapper for
    * {@link #setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
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
    * component instance. A deformable body may be paired with {@link
    * Collidable#Self} to indicate self-intersection; otherwise, generic
    * designations (such as {@link Collidable#RigidBody}) are not allowed.
    *
    * <p> If the specified collision behavior equals the existing collision
    * behavior (as returned by {@link #getCollisionBehavior}), then this method
    * does nothing. Otherwise, if the collision behavior can be obtained by
    * removing an existing collision override for the pair, the override is
    * removed. Otherwise, a collision override is created for the desired
    * behavior, replacing any existing override.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @param behavior specified collision behavior
    */
   public void setCollisionBehavior (
      Collidable a, Collidable b, CollisionBehavior behavior) {

      CollidablePair pair = validateCollidablePair (a, b);
      CollisionComponent comp = getCollisionComponentMap().get (pair);
      if (comp != null) {
         if (!behavior.equals (comp.getBehavior())) {
            CollisionBehavior prim = getPrimaryCollisionBehavior (pair);
            if (behavior.equals (prim)) {
               removeCollisionOverride (comp);
            }
            else {
               comp.setBehavior (behavior);
               // notify structure change because higher level models may have to
               // clear internal collision structures to handle the new setting
               notifyStructureChanged ();
            }
         }
      }
      else {
         CollisionBehavior prim = getPrimaryCollisionBehavior (pair);
         if (!behavior.equals (prim)) {
            addCollisionOverride (pair, behavior);
         }
      }
   }
   
   protected CollisionBehavior getDefaultCollisionBehavior (CollidablePair pair) {
      CollisionComponent resp;
      Collidable a = pair.myCompA;
      Collidable b = pair.myCompB;
      if ((a instanceof RigidBody) != (b instanceof RigidBody)) {
         resp = getCollisionOverride (
            Collidable.Deformable, Collidable.RigidBody);
      }
      else if (a instanceof RigidBody) {
         resp = getCollisionOverride (
            Collidable.RigidBody, Collidable.RigidBody);
      }
      else {
         resp = getCollisionOverride (
            Collidable.Deformable, Collidable.Deformable);
      }
      return resp.getBehavior();
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
   public CollisionComponent getCollisionOverride (CollidablePair pair) {
      return getCollisionOverride(pair.getA(), pair.getB());
   }

   /** 
    * Returns the CollisionCompenent in this model associated with
    * a particular collision override. If no such component exists,
    * <code>null</code> is returned. This method is provided mostly for
    * internal ArtiSynth use.
    * 
    * @param a first collidable
    * @param b second collidable
    * @return CollisionComponent in this model overriding collision behavior
    * for (a, b), if any
    */
   public CollisionComponent getCollisionOverride (Collidable a, Collidable b) {
      HashMap<CollidablePair,CollisionComponent> map = getCollisionComponentMap();
      for (CollidablePair cp : map.keySet()) {
         if (cp.includesCollidables(a, b)) {
            return map.get(cp);
         }
      }
      return null;
   }

   /** 
    * Adds a CollisionComponent to override the primary
    * collision behavior for a specific pair of components.  Each collidable
    * must be a particular component instance. A deformable body may be paired
    * with {@link Collidable#Self} to indicate self-intersection; otherwise,
    * generic designations (such as {@link Collidable#RigidBody}) are not
    * allowed.  This method is mainly intended for ArtiSynth internal use.
    * 
    * @param pair specified pair of collidables
    * @param behavior desired override collision behavior
    */
   public void addCollisionOverride (
      CollidablePair pair, CollisionBehavior behavior) {
      pair = validateCollidablePair (pair);
      myCollisionComponents.add (new CollisionComponent (pair, behavior));
   }

   /** 
    * Removes a CollisionComponent, removing
    * the collision override for the associated pair of components.
    * This method is mainly intended for ArtiSynth internal use.
    * 
    * @param comp CollisionComponent overriding primary collision
    * behavior for a specific pair of components
    * @return true if the specified collision component was present
    */
   public boolean removeCollisionOverride (CollisionComponent comp) {
      return myCollisionComponents.remove (comp);
   }

   /** 
    * Removes all collision overrides, so
    * that the collision behavior between all components will
    * revert back to their primary behavior.
    */
   public void clearCollisionOverrides() {
      myCollisionComponents.clear();
   }

   /** 
    * Returns the primary collision behavior for a specified component
    * pair as seen by this model. A primary collision behavior is
    * the collision behavior that will occur in the absence of
    * any override behaviors in this model. Typically, this
    * will be the default behavior, unless both components in
    * the pair belong to a sub-MechModel, in which case the CollisionManager
    * in the sub-MechModel determines the behavior.
    *
    * <p>Each collidable must be a particular
    * component instance. A deformable body may be paired with {@link
    * Collidable#Self} to indicate self-intersection; otherwise, generic
    * designations (such as {@link Collidable#RigidBody}) are not allowed.
    * 
    * @param pair specified pair of collidables
    * @return primary collision behavior for the specified component pair
    */
   public CollisionBehavior getPrimaryCollisionBehavior (CollidablePair pair) {
      pair = validateCollidablePair (pair);
      MechModel lowModel = MechModel.lowestCommonModel (
         pair.myCompA, pair.myCompB);
      if (lowModel == null) {
         throw new IllegalArgumentException (
            "One or both components do not reside in this model:\n" +
            ComponentUtils.getPathName (pair.myCompA) + "\n" +
            ComponentUtils.getPathName (pair.myCompB));
      }
      CollisionManager cm = lowModel.getCollisionManager();
      CollisionBehavior behavior = cm.getDefaultCollisionBehavior (pair);
      ModelComponent comp = lowModel;
      while (comp != getParent()) {
         if (comp instanceof MechModel) {
            cm = ((MechModel)comp).getCollisionManager();
            CollisionComponent cc = cm.getCollisionOverride (pair);
            if (cc != null) {
               behavior = cc.getBehavior();
            }
         }
         comp = comp.getParent();
      }
      return behavior;
   }


   /** 
    * Returns the collision behavior for a specified pair of collidables.  Each
    * collidable must be a particular component instance. A deformable body may
    * be paired with {@link Collidable#Self} to indicate self-intersection;
    * otherwise, generic designations (such as {@link Collidable#RigidBody})
    * are not allowed.
    *
    * <p>If this CollisionManager contains a collision override for
    * the specified pair, then the behavior will be determined
    * by that override. Otherwise, the behavior will equal
    * that returned by {@link #getPrimaryCollisionBehavior}.
    * 
    * @param a first Collidable
    * @param b second Collidable
    * @return behavior for this pair of Collidables.
    */
   public CollisionBehavior getCollisionBehavior (Collidable a, Collidable b) {
      CollidablePair pair = validateCollidablePair (a, b);
      CollisionComponent cc = getCollisionOverride (pair);
      if (cc != null) {
         return cc.getBehavior();
      }
      else {
         return getPrimaryCollisionBehavior (pair);
      }
   }

   private CollisionHandler createCollisionHandler(Collidable c0, 
      Collidable c1, double mu) {
      if (myUseNewHandler) {
         return new CollisionHandlerGeneric(c0, c1, mu);
      } else {
         return new CollisionHandlerOld (c0, c1, mu);
      }
   }
   
   private CollisionHandler createCollisionPair (
      Collidable c0, Collidable c1, CollisionBehavior behavior) {
      
      CollisionHandler pair = 
         createCollisionHandler(c0, c1, behavior.getFriction());
         
      if (pair.isRigidBodyPair()) {
         pair.setRigidPointTolerance (getCollisionPointTol());
         pair.setRigidRegionTolerance (getCollisionRegionTol());
      }
      double ptol = getPenetrationTol();
      pair.setPenetrationTol (ptol);
      if (myCollisionAccel != 0) {
         pair.autoComputeCompliance (myCollisionAccel, getPenetrationTol());
      }
      else {
         pair.setCompliance (getCollisionCompliance());
         pair.setDamping (getCollisionDamping());
      }
      return pair;
   }

   private void createSelfCollisionPairs (
      FemModel3d fem, CollisionBehavior behavior) {
      
      // int num = fem.numSubSurfaces();
     
      MeshComponentList<FemMesh> meshes = fem.getMeshes ();
      int num = meshes.size ();
      
      for (int i=0; i<num; i++) {
         FemMesh fmeshi = meshes.get(i);
         if (!fmeshi.isSurfaceMesh () && fmeshi.isCollidable ()) {
            for (int j=i+1; j<num; j++) {
               // ignore surface mesh
               FemMesh fmeshj = meshes.get(j);
               if (!fmeshj.isSurfaceMesh() && fmeshj.isCollidable ())
               myCollisionHandlers.add ( createCollisionPair (fmeshi, fmeshj, behavior));
            }
         }
      }
   }

   /**
    * If necessary, rebuilds the collison pair list from the specified
    * contact info.
    */
   public CollisionHandlerList collisionHandlers() {
      if (!myCollisionHandlersValid) {
         
         myCollisionHandlers.removeAll();
         // guess at initial capacity for collidable list
         ArrayList<Collidable> collidables =
            new ArrayList<Collidable>(1000);
         if (getParent() instanceof MechModel) {
            ((MechModel)getParent()). getCollidables (collidables, /*level=*/0);
         }
         for (int i=0; i<collidables.size(); i++) {
            for (int j=i; j<collidables.size(); j++) {
               Collidable ci = collidables.get(i);
               Collidable cj = collidables.get(j);
               if (ci == cj) {
                  if (!(ci instanceof RigidBody) && myAllowSelfIntersections) {
                     // for now, implement self intersection using sub-surfaces
                     CollisionBehavior behavior =
                        getCollisionBehavior (ci, ci);
                     if (ci instanceof FemModel3d && behavior.isEnabled()) {
                        FemModel3d fem = (FemModel3d)ci;
                        
                        if (fem.getNumMeshes () > 1) {
                           createSelfCollisionPairs (fem, behavior);
                        }
                     }
                  }
               }
               else {
                  // note: adds in both directions
                  CollisionBehavior behavior = getCollisionBehavior (ci, cj);
                  if (behavior.isEnabled()) {
                     CollisionHandler ch = null;
                     if (ci.isCollidable () && cj.isCollidable ()) {
                        ch =
                           createCollisionPair(ci, cj, behavior);
                     }
                     if (ch != null) {
                        myCollisionHandlers.add (ch);
                     }
                  }
               }
            }
         }
         for (CollisionHandler handler : myCollisionHandlers) {
            handler.setDrawIntersectionContours(drawIntersectionContours);
            handler.setDrawIntersectionFaces(drawIntersectionFaces);
            handler.setDrawIntersectionPoints(drawIntersectionPoints);
         }
         
         myCollisionHandlersValid = true;
      }
      return myCollisionHandlers;
   }

   public ComponentList<CollisionComponent> collisionComponents() {
      return myCollisionComponents;
   }

   protected void notifyStructureChanged () {
      myCollisionHandlersValid = false;
      notifyParentOfChange (new StructureChangeEvent (this));
   }

   public void initialize() {
      for (CollisionHandler ch : myCollisionHandlers) {
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
      list.addIfVisible (myCollisionHandlers);
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
   }

   protected void writeDefaultBehaviors (PrintWriter pw, NumberFormat fmt) {
      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (CollidablePair pair : getDefaultPairs()) {
         CollisionBehavior behavior = getDefaultCollisionBehavior (pair);
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
         setDefaultCollisionBehavior (
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

   public void addAuxStateComponents (List<HasAuxState> comps) {
      for (CollisionHandler ch : collisionHandlers()) {
         if (ch.isRigidBodyPair()) {
            comps.add (ch.getRigidBodyContact());
         }
         else {
            comps.add (ch);
         }
      }
   }  

   // /**
   //  * Used to notify this CollisionManager that model structure has
   //  * changed and collision information may need to be updated.
   //  */
   // public void notifyStructureChanged() {
   //    myCollisionComponentMapValidP = false;
   // }

   @Override
   public void componentChanged (ComponentChangeEvent e) {
      myCollisionComponentMapValidP = false;
      // no need to propagate change event back to the MechModel,
      // esp since we aren't displaying these components anywhere
      //notifyParentOfChange (e);
   }

   public void getConstrainers (List<Constrainer> constrainers) {
      for (CollisionHandler pair : collisionHandlers()) {
         if (pair.isRigidBodyPair()) {
            constrainers.add (pair.getRigidBodyContact());
         }
      }            
      for (CollisionHandler pair : collisionHandlers()) {
         if (!pair.isRigidBodyPair()) {
            constrainers.add (pair);
         }
      }
   }
   
   public void setUseNewHandler(boolean set) {
      if (myUseNewHandler != set) {
         myUseNewHandler = set;
         myCollisionHandlersValid = false;
      }
   }
   
   public boolean getUseNewHandler() {
      return myUseNewHandler;
   }
   
   public void setDrawIntersectionContours(boolean set) {
      drawIntersectionContours = set;
      for (CollisionHandler handler : myCollisionHandlers) {
         handler.setDrawIntersectionContours(set);
      }
   }

   public boolean isDrawIntersectionContours() {
      return drawIntersectionContours;
   }

   public void setDrawIntersectionFaces(boolean set) {
      drawIntersectionFaces = set;
      for (CollisionHandler handler : myCollisionHandlers) {
         handler.setDrawIntersectionFaces(set);
      }
   }

   public boolean isDrawIntersectionFaces() {
      return drawIntersectionFaces;
   }

   public void setDrawIntersectionPoints(boolean set) {
      drawIntersectionPoints = set;
      for (CollisionHandler handler : myCollisionHandlers) {
         handler.setDrawIntersectionPoints(set);
      }
   }

   public boolean isDrawIntersectionPoints() {
      return drawIntersectionPoints;
   }

}
