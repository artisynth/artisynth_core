package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import maspack.collision.AbstractCollider;
import maspack.collision.ContactInfo;
import maspack.collision.MeshCollider;
import maspack.collision.SurfaceMeshIntersector;
import maspack.collision.SurfaceMeshIntersector.RegionType;
import maspack.collision.SignedDistanceCollider;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.DistanceGrid;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.util.DataBuffer;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.FunctionTimer;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.Collidable.Group;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableCompositeBase;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScalarRange;
import artisynth.core.util.ScanToken;

/**
 * A special component that manages collisions between collidable bodies on
 * behalf of a MechModel.
 *
 *<p>
 * Because collision handling can be expensive, and also because it is not as
 * accurate as other aspects of the simulation (due largely to its
 * discontinuous nature), it is important to be able to control very precisely
 * how it is applied. The CollisionManager allows collision behavior to be
 * specified through 
 *
 *<p>
 * (a) default collision behaviors between generic collidable groups, and
 *
 *<p>
 * (b) specific behaviors between pairs of collidables which override
 * the default behaviors.
 *
 *<p>
 * The CollisionManager maintains:
 *
 *<p>
 * (1) A set of default behaviors;
 * 
 *<p>
 * (2) A set of override behaviors, which are stored as invisible
 * model components;
 * 
 *<p>
 * (3) A behavior map which describes the collision behavior for every
 * CollidableBody in the MechModel and which is updated on demand;
 *
 *<p>
 * (4) A set of CollisionHandlers which provides a collision handler for every
 * active collision behavior in the behavior map and which is also updated on
 * demand;
 *
 *<p>
 * Collidable components can be arranged hierarchically. Any component which is
 * a descendant of a Collidable A is known as a sub-collidable of A (a
 * sub-collidable does not need to be an immediate child; it only need to be a
 * descendant). Within a hierarchy, only the leaf nodes do that actual
 * colliding, and these should be instances of the sub-interface
 * CollidableBody. 
 *
 *<p>
 * Normally collidables within a hierarchy do not collide with each other.  The
 * exception is when either (a) self-collision is specified for one of the
 * ancestor nodes in the hierarchy, or (b) an explicit collision behavior is
 * set between members of the hierarchy. If a self-collision behavior is
 * specified for an ancestor node A, and A is deformable (i.e., its
 * isDeformable() method returns <code>true</code>), then that behavior will be
 * passed on to all pairs of sub-collidables of A for which A's method
 * allowSelfCollisions() returns true.
 * 
 *<p>
 * When a collision behavior is specified between two collidables A and B that
 * are *not* part of the same hierarchy, then that behavior is imparted to all
 * pairs of leaf-nodes located at or below A and B.
 */
public class CollisionManager extends RenderableCompositeBase
   implements ScalableUnits, Constrainer, HasNumericState {

   // Current assumptions:
   //
   // 1) Collidable hierarchies are no more than one deep
   //
   // 2) Child collidables have the same type (rigid or deformable) as their
   // ancestor.

   // If true, updateConstraints() will create handlers for every possible
   // collision pair, using dummy ContactInfo structures. This is used for
   // testing purposes only.
   boolean myContactTestMode = false;

   static final int CONTACT_TEST_MODE = 0x1000000;

   MechModel myMechModel;
   CollisionBehaviorList myBehaviors;
   CollisionResponseList myResponses;
   CollisionHandlerTable myHandlerTable;
   ArrayList<CollisionHandler> myHandlers;
   LinkedHashMap<CollidablePair,CollisionBehavior> myExplicitBehaviors;
   LinkedHashMap<CollidableBody,CollisionBehavior> myRigidExtBehaviors;
   LinkedHashMap<CollidableBody,CollisionBehavior> myDeformableExtBehaviors;
   LinkedHashMap<Collidable,CollisionBehavior> myDeformableIntBehaviors;

   ArrayList<CollidableBody> myRigidExts;
   ArrayList<CollidableBody> myDeformableExts;
   ArrayList<CollidableBody> myDeformableInts;

   HashMap<CollidablePair,ArrayList<CollisionResponse>> myPairResponses;
   HashMap<CollidableBody,ArrayList<CollisionResponse>> myGroupResponses;

   boolean myBehaviorStructuresValid = false;
   boolean myResponseStructuresValid = false;
   boolean myHandlerTableValid = false;
   int myUsesBilateralConstraints = -1; // -1 means we don't know

   AbstractCollider myCollider = null;

   SurfaceMeshIntersector myAjlIntersector = null;
   SignedDistanceCollider mySDCollider = null;
   MeshCollider myTriTriCollider = null;

   double myMaxpen; // accumulates maximum penetration 

   /**
    * Specifies the collider that generates contact information between the
    * two meshes. Contact information is returned in a 
    * {@link maspack.collision.ContactInfo ContactInfo} structure.
    */
   public enum ColliderType {
      /**
       * Original method that finds all triangle intersections between the
       * meshes and groups these into regions to determine interpenetrating
       * vertices. Does not compute penetration regions or intersection
       * contours. 
       */
      TRI_INTERSECTION,
      
      /**
       * Newer method that uses the triangle intersections between meshes
       * to determine the actual intersection contours (and associated
       * penetration regions) between the meshes.
       */
      AJL_CONTOUR,

      /**
       * A collider based on using a signed-distance function to determine
       * interpenetration contacts between bodies. This method can
       * be fast, but provides only limited contact information,
       * particularly compared to <code>AJL_CONTOUR</code>, supports
       * only contacts based on vertex penetration, and at least
       * one of the colliding bodies must be non-deformable.
       */
      SIGNED_DISTANCE,

      /**
       * Experimental collider for continuous collision detection.
       */
      CONTINUOUS
   };

   static ColliderType myDefaultColliderType = ColliderType.TRI_INTERSECTION;
   ColliderType myColliderType = myDefaultColliderType;
   PropertyMode myColliderTypeMode = PropertyMode.Inherited;

   static Method defaultMethod = Method.DEFAULT;
   Method myMethod = defaultMethod;
   PropertyMode myMethodMode = PropertyMode.Inherited;

   private static CollidablePair RIGID_RIGID =
      new CollidablePair (Collidable.Rigid, Collidable.Rigid);
   private static CollidablePair DEFORMABLE_RIGID  =
      new CollidablePair (Collidable.Deformable, Collidable.Rigid);
   private static CollidablePair DEFORMABLE_DEFORMABLE  =
      new CollidablePair (Collidable.Deformable, Collidable.Deformable);
   private static CollidablePair DEFORMABLE_SELF  =
      new CollidablePair (Collidable.Deformable, Collidable.Self);
   
   public static void setDefaultColliderType (ColliderType type) {
      myDefaultColliderType = type;
   }
   
   public static ColliderType getDefaultColliderType() {
      return myDefaultColliderType;
   }
   
   /**
    * Describes where to look for the collision behavior of a given
    * collidable.
    */
   enum BehaviorSource {
      EXPLICIT,
      EXTERNAL,
      INTERNAL;
   }
   
   CollisionBehavior myDefaultRigidRigid;
   CollisionBehavior myDefaultDeformableRigid;
   CollisionBehavior myDefaultDeformableDeformable;
   CollisionBehavior myDefaultDeformableSelf;

   private static CollidablePair[] myDefaultPairs =
      new CollidablePair[] {
         RIGID_RIGID, 
         DEFORMABLE_RIGID, 
         DEFORMABLE_DEFORMABLE,
         DEFORMABLE_SELF
      };

   public int numDefaultPairs() {
      return myDefaultPairs.length;
   }

   // property definitions

   static double defaultFriction = 0;
   double myFriction = defaultFriction;
   PropertyMode myFrictionMode = PropertyMode.Inherited;

   static boolean defaultBilateralVertexContact = true;
   boolean myBilateralVertexContact = defaultBilateralVertexContact;
   PropertyMode myBilateralVertexContactMode = PropertyMode.Inherited;

   static boolean defaultReduceConstraints = false;
   boolean myReduceConstraints = defaultReduceConstraints;
   PropertyMode myReduceConstraintsMode = PropertyMode.Inherited;

   static boolean defaultBodyFaceContact = false;
   boolean myBodyFaceContact = defaultBodyFaceContact;
   PropertyMode myBodyFaceContactMode = PropertyMode.Inherited;

   // rigidRegionTol and rigidPointTol are both computed automatically
   // at the first initialize() if their values are not -1:
   
   static double defaultRigidRegionTol = -1;
   double myRigidRegionTol = defaultRigidRegionTol;
   static PropertyMode defaultRigidRegionTolMode = PropertyMode.Explicit;
   PropertyMode myRigidRegionTolMode = defaultRigidRegionTolMode;

   static double defaultRigidPointTol = -1;
   double myRigidPointTol = defaultRigidPointTol;
   static PropertyMode defaultRigidPointTolMode = PropertyMode.Explicit;
   PropertyMode myRigidPointTolMode = defaultRigidPointTolMode;

   static double defaultCompliance = 0.0;
   double myCompliance = defaultCompliance;
   PropertyMode myComplianceMode = PropertyMode.Inherited;

   static double defaultDamping = 0.0;
   double myDamping = defaultDamping;
   PropertyMode myDampingMode = PropertyMode.Inherited;

   static double defaultAcceleration = 0;
   double myAcceleration = defaultAcceleration;
   PropertyMode myAccelerationMode = PropertyMode.Inherited;

   private double myContactNormalLen = Property.DEFAULT_DOUBLE;

   static double DEFAULT_CONTACT_FORCE_LEN_SCALE = 1.0;
   private double myContactForceLenScale = DEFAULT_CONTACT_FORCE_LEN_SCALE;
   
   // Estimate of the radius of the set of collidable objects.
   // Used for computing default tolerances.
   protected double myCollisionArenaRadius = -1;

   static boolean defaultDrawIntersectionContours = false;
   boolean myDrawIntersectionContours = defaultDrawIntersectionContours;
   PropertyMode myDrawIntersectionContoursMode = PropertyMode.Inherited;

   static boolean defaultDrawIntersectionFaces = false;
   boolean myDrawIntersectionFaces = defaultDrawIntersectionFaces;
   PropertyMode myDrawIntersectionFacesMode = PropertyMode.Inherited;

   static boolean defaultDrawIntersectionPoints = false;
   boolean myDrawIntersectionPoints = defaultDrawIntersectionPoints;
   PropertyMode myDrawIntersectionPointsMode = PropertyMode.Inherited;

   static boolean defaultDrawContactNormals = false;
   boolean myDrawContactNormals = defaultDrawContactNormals;
   PropertyMode myDrawContactNormalsMode = PropertyMode.Inherited;

   static boolean defaultDrawContactForces = false;
   boolean myDrawContactForces = defaultDrawContactForces;
   PropertyMode myDrawContactForcesMode = PropertyMode.Inherited;

   static ColorInterpolation defaultColorMapInterpolation =
      ColorInterpolation.HSV;
   ColorInterpolation myColorMapInterpolation = defaultColorMapInterpolation;
   PropertyMode myColorMapInterpolationMode = PropertyMode.Inherited;

   static ColorMapType defaultDrawColorMap = ColorMapType.NONE;
   ColorMapType myDrawColorMap = defaultDrawColorMap;
   PropertyMode myDrawColorMapMode = PropertyMode.Inherited;

   static int defaultColorMapCollidableNum = 0;
   int myColorMapCollidableNum = defaultColorMapCollidableNum;
   PropertyMode myColorMapCollidableMode = PropertyMode.Inherited;

   static ColorMapBase defaultColorMap = new HueColorMap (2.0/3, 0);
   ColorMapBase myColorMap = defaultColorMap.copy();

   static ScalarRange defaultColorMapRange = new ScalarRange();
   ScalarRange myColorMapRange = defaultColorMapRange.clone();

   ContactForceBehavior myForceBehavior;

   public static PropertyList myProps =
      new PropertyList (CollisionManager.class, RenderableCompositeBase.class);

   static private RenderProps defaultRenderProps = new RenderProps();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);

      myProps.addInheritable (
         "friction:Inherited", "friction coefficient", defaultFriction);

      myProps.addInheritable (
         "bilateralVertexContact:Inherited",
         "allow bilateral constraints for vertex-based contacts", 
         defaultBilateralVertexContact);

      myProps.addInheritable (
         "reduceConstraints:Inherited",
         "try to reduce the number of constraints", 
         defaultReduceConstraints);

      myProps.addInheritable (
         "bodyFaceContact:Inherited",
         "add contacts for interpenetrating rigid body vertices",
         defaultBodyFaceContact);

      myProps.addInheritable (
         "rigidRegionTol", "region size tolerance for creating contact planes",
         defaultRigidRegionTol);

      myProps.addInheritable (
         "rigidPointTol", "point tolerance for creating contact planes",
         defaultRigidPointTol);

      myProps.add (
         "contactNormalLen",
         "draw contact normals with indicated length", Property.DEFAULT_DOUBLE);

      myProps.add (
         "contactForceLenScale",
         "length scale to be used when drawing contact forces",
         DEFAULT_CONTACT_FORCE_LEN_SCALE);

      myProps.addInheritable (
         "acceleration:Inherited",
         "acceleration used to compute collision compliance from penetrationTol",
         defaultAcceleration);

      myProps.addInheritable (
         "compliance:Inherited", "compliance for each contact constraint",
         defaultCompliance, "[0,inf)");
      myProps.addInheritable (
         "damping:Inherited", "damping for each contact constraint",
         defaultDamping, "[0,inf)");

      myProps.addInheritable (
         "drawIntersectionFaces:Inherited", 
         "draw intersection faces", defaultDrawIntersectionFaces);
      myProps.addInheritable (
         "drawIntersectionContours:Inherited", 
         "draw intersection contours", defaultDrawIntersectionContours);
      myProps.addInheritable (
         "drawIntersectionPoints:Inherited", 
         "draw intersection points", defaultDrawIntersectionPoints);
      myProps.addInheritable (
         "drawContactNormals:Inherited", 
         "draw normals at each contact point", defaultDrawContactNormals);
      myProps.addInheritable (
         "drawContactForces:Inherited", 
         "draw forces at each contact point", defaultDrawContactForces);
      myProps.addInheritable (
         "drawColorMap:Inherited", 
         "draw a color map of the specified data",
         defaultDrawColorMap);
      myProps.addInheritable (
         "colorMapCollidable:Inherited", 
         "number of the collidable (0 or 1) on which the color map show be drawn",
         defaultColorMapCollidableNum, "[0,1] NoSlider");

       myProps.add (
         "colorMapRange", "range for drawing color maps", 
         defaultColorMapRange);
      myProps.addInheritable (
         "colorMapInterpolation",
         "explicit setting for how to interpolate color map (RGB or HSV)",
         defaultColorMapInterpolation);

      myProps.add (
         "colorMap", "color map for penetration plotting", 
         defaultColorMap, "CE");

      myProps.addInheritable (
         "method:Inherited", "collision handling method", defaultMethod);

      myProps.addInheritable (
         "colliderType", "type of collider to use for collisions",
         myDefaultColliderType);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private void setDefaultBehaviorVariables() {
      myDefaultRigidRigid = myBehaviors.get(0);
      myDefaultDeformableRigid = myBehaviors.get(1);
      myDefaultDeformableDeformable = myBehaviors.get(2);
      myDefaultDeformableSelf = myBehaviors.get(3);
   }

   private void initializeDefaultBehaviors() {
      CollidablePair[] pairs = myDefaultPairs;
      for (int i=0; i<pairs.length; i++) {
         CollisionBehavior behav = new CollisionBehavior(false, -1);
         behav.setCollidablePair (pairs[i]);
         behav.setName (pairs[i].createComponentName(myMechModel));
         myBehaviors.addFixed (behav);
      }
      setDefaultBehaviorVariables();
   }

   public CollisionManager (MechModel mech) {

      myMechModel = mech;

      setDefaultValues();
      
      myBehaviors =
         new CollisionBehaviorList ("behaviors", "b");
      myResponses =
         new CollisionResponseList ("responses", "r");
      //myBehaviors.setNavpanelVisibility (NavpanelVisibility.HIDDEN);
      myHandlerTable = new CollisionHandlerTable(this);
      myHandlers = new ArrayList<CollisionHandler>();

      myExplicitBehaviors = new LinkedHashMap<CollidablePair,CollisionBehavior>();
      myRigidExtBehaviors =
         new LinkedHashMap<CollidableBody,CollisionBehavior>();
      myDeformableExtBehaviors =
         new LinkedHashMap<CollidableBody,CollisionBehavior>();
      myDeformableIntBehaviors =
         new LinkedHashMap<Collidable,CollisionBehavior>();
      initializeDefaultBehaviors();

      myPairResponses =
         new HashMap<CollidablePair,ArrayList<CollisionResponse>>();
      myGroupResponses =
         new HashMap<CollidableBody,ArrayList<CollisionResponse>>();

      myRigidExts = new ArrayList<CollidableBody>();
      myDeformableExts = new ArrayList<CollidableBody>();
      myDeformableInts = new ArrayList<CollidableBody>();

      myBehaviorStructuresValid = false;
      myResponseStructuresValid = false;
      myHandlerTableValid = false;

      add (myBehaviors);
      add (myResponses);
   }

   public void clear() {
      setRenderProps (createRenderProps());
      myHandlerTable.clear();
      myHandlers.clear();
      myBehaviors.removeAll();
      myResponses.removeAll();
      initializeDefaultBehaviors();
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      setRenderProps (createRenderProps());
      myFriction = defaultFriction;
      myFrictionMode = PropertyMode.Inherited;
      myBilateralVertexContact = defaultBilateralVertexContact;
      myBilateralVertexContactMode = PropertyMode.Inherited;
      myCompliance = defaultCompliance;
      myComplianceMode = PropertyMode.Inherited;
      myDamping = defaultDamping;
      myDampingMode = PropertyMode.Inherited;
      myRigidRegionTol = defaultRigidRegionTol;
      myRigidRegionTolMode = defaultRigidRegionTolMode;
      myRigidPointTol = defaultRigidPointTol;
      myRigidPointTolMode = defaultRigidPointTolMode;
      myReduceConstraints = defaultReduceConstraints;
      myReduceConstraintsMode = PropertyMode.Inherited;
      myBodyFaceContact = defaultBodyFaceContact;
      myBodyFaceContactMode = PropertyMode.Inherited;
      myDrawIntersectionContours = defaultDrawIntersectionContours;
      myDrawIntersectionContoursMode = PropertyMode.Inherited;
      myDrawIntersectionFaces = defaultDrawIntersectionFaces;
      myDrawIntersectionFacesMode = PropertyMode.Inherited;
      myDrawIntersectionPoints = defaultDrawIntersectionPoints;
      myDrawIntersectionPointsMode = PropertyMode.Inherited;
      myDrawContactNormals = defaultDrawContactNormals;
      myDrawContactNormalsMode = PropertyMode.Inherited;
      myDrawContactForces = defaultDrawContactForces;
      myDrawContactForcesMode = PropertyMode.Inherited;
      myDrawColorMap = defaultDrawColorMap;
      myDrawColorMapMode = PropertyMode.Inherited;
      myColorMapInterpolation = defaultColorMapInterpolation;
      myColorMapInterpolationMode = PropertyMode.Inherited;
      myColorMapCollidableNum = defaultColorMapCollidableNum;
      myColorMapCollidableMode = PropertyMode.Inherited;
      setColorMapRange (defaultColorMapRange);
      myForceBehavior = null;
      myMethod = defaultMethod;
      myMethodMode = PropertyMode.Inherited;
      myColliderType = myDefaultColliderType;
      myColliderTypeMode = PropertyMode.Inherited;
   }

   ArrayList<CollisionHandler> collisionHandlers() {
      return myHandlers;
   }

   void collectHandlers (ArrayList<CollisionHandler> handlers) {
      for (MechSystemModel m : myMechModel.getLocalModels()) {
         if (m instanceof MechModel) {
            CollisionManager cm = ((MechModel)m).getCollisionManager();
            cm.myHandlerTable.collectHandlers (handlers);
         }
      }
      myHandlerTable.collectHandlers (handlers);       
   }

   // property accessors

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

   public void setContactNormalLen (double len) {
      if (len != myContactNormalLen) {
         if (len < 0) {
            len = getDefaultContactNormalLen();
         }
         myContactNormalLen = len;
      }
   }

   public double getContactNormalLen() {
      return myContactNormalLen;
   }

   public double getDefaultContactNormalLen() {
      return 0.1*getCollisionArenaRadius();
   }

   public void setContactForceLenScale (double scale) {
      if (scale != myContactForceLenScale) {
         myContactForceLenScale = scale;
      }
   }

    public double getContactForceLenScale() {
      return myContactForceLenScale;
   }
    
   /** 
    * Gets the Coulomb friction coefficient
    * 
    * @return friction coefficient
    */
   public double getFriction() {
      return myFriction;
   }

   /** 
    * Sets the Coulomb friction coefficent
    * 
    * @param mu friction coefficient
    */
   public void setFriction (double mu) {
      myFriction = mu;
      myFrictionMode =
         PropertyUtils.propagateValue (
            this, "friction", myFriction, myFrictionMode);      
   }

   public void setFrictionMode (PropertyMode mode) {
      myFrictionMode =
         PropertyUtils.setModeAndUpdate (this, "friction", myFrictionMode, mode);
   }

   public PropertyMode getFrictionMode() {
      return myFrictionMode;
   }

   /** 
    * Returns whether bilateral constraints should be used for vertex-based
    * contact.
    * 
    * @return {@code true} if bilateral constraints should be used
    */
   public boolean getBilateralVertexContact() {
      return myBilateralVertexContact;
   }

   /** 
    * Set whether bilateral constraints should be used for vertex-based
    * contact.
    * 
    * @param enable if {@code true}, enables bilateral constraints
    */
   public void setBilateralVertexContact (boolean enable) {
      myBilateralVertexContact = enable;
      myBilateralVertexContactMode =
         PropertyUtils.propagateValue (
            this, "bilateralVertexContact",
            myBilateralVertexContact, myBilateralVertexContactMode);
   }

   public void setBilateralVertexContactMode (PropertyMode mode) {
      myBilateralVertexContactMode =
         PropertyUtils.setModeAndUpdate (
            this, "bilateralVertexContact",
            myBilateralVertexContactMode, mode);
   }

   public PropertyMode getBilateralVertexContactMode() {
      return myBilateralVertexContactMode;
   }

   /** 
    * Gets the default rigid region tolerance associated with all collision
    * behaviors.
    * 
    * @return rigid region tolerance
    */
   public double getRigidRegionTol() {
      return myRigidRegionTol;
   }

   /** 
    * Sets the default rigid region tolerance associated with all collision
    * behaviors.
    * 
    * @param tol new rigid region tolerance
    */
   public void setRigidRegionTol (double tol) {
      if (tol < 0) {
         tol = getDefaultRigidRegionTol();
      }
      myRigidRegionTol = tol;
      myRigidRegionTolMode =
         PropertyUtils.propagateValue (
            this, "rigidRegionTol", myRigidRegionTol, myRigidRegionTolMode);
   }

   public void setRigidRegionTolMode (PropertyMode mode) {
      myRigidRegionTolMode =
         PropertyUtils.setModeAndUpdate (
            this, "rigidRegionTol", myRigidRegionTolMode, mode);
   }

   public PropertyMode getRigidRegionTolMode() {
      return myRigidRegionTolMode;
   }
   
   public double getDefaultRigidRegionTol() {
      return 1e-3*getCollisionArenaRadius();
   }

   /** 
    * Gets the default rigid point tolerance associated with all collision
    * behaviors.  This is the point clustering distance used when computing
    * contact planes.
    * 
    * @return rigid point tolerance
    */
   public double getRigidPointTol() {
      return myRigidPointTol;
   }

   /** 
    * Sets the default rigid point tolerance associated with all collision
    * behaviors.
    * 
    * @param tol new rigid point tolerance
    */
   public void setRigidPointTol (double tol) {
      if (tol < 0) {
         tol = getDefaultRigidPointTol();
      }
      myRigidPointTol = tol;
      myRigidPointTolMode =
         PropertyUtils.propagateValue (
            this, "rigidPointTol", myRigidPointTol, myRigidPointTolMode);
   }

   public void setRigidPointTolMode (PropertyMode mode) {
      myRigidPointTolMode =
         PropertyUtils.setModeAndUpdate (
            this, "rigidPointTol", myRigidPointTolMode, mode);
   }

   public PropertyMode getRigidPointTolMode() {
      return myRigidPointTolMode;
   }
   
   public double getDefaultRigidPointTol() {
      return 1e-2*getCollisionArenaRadius();
   }
   
   /** 
    * Queries whether constraint reduction is enabled.
    * 
    * @return true if constraint reduction is enabled
    */
   public boolean getReduceConstraints() {
      return myReduceConstraints;
   }

   /** 
    * Sets whether or not constraint reduction is enabled.
    * 
    * @param enable true if constraint reduction should be enabled
    */
   public void setReduceConstraints (boolean enable) {
      myReduceConstraints = enable;
      myReduceConstraintsMode =
         PropertyUtils.propagateValue (
            this, "reduceConstraints", myReduceConstraints,myReduceConstraintsMode);
   }

   public void setReduceConstraintsMode (PropertyMode mode) {
      myReduceConstraintsMode =
         PropertyUtils.setModeAndUpdate (
            this, "reduceConstraints", myReduceConstraintsMode, mode);
   }

   public PropertyMode getReduceConstraintsMode() {
      return myReduceConstraintsMode;
   }

   /** 
    * Queries whether body face contact is enabled. See {@link
    * #setBodyFaceContact} for details.
    * 
    * @return true if body face face is enabled
    */
   public boolean getBodyFaceContact() {
      return myBodyFaceContact;
   }

   /** 
    * Enables or disables body face contact. If enabled, this means that for
    * rigid-deformable contact, contacts are also computed based on the rigid
    * body vertices that are penetrating the deformable body. The default value
    * for this property is <code>false</code>, since such contacts can result
    * in an overconstrained system.
    * 
    * @param enable true if body face contact should be enabled
    */
   public void setBodyFaceContact (boolean enable) {
      myBodyFaceContact = enable;
      myBodyFaceContactMode =
         PropertyUtils.propagateValue (
            this, "bodyFaceContact", myBodyFaceContact, myBodyFaceContactMode);

   }

   public void setBodyFaceContactMode (PropertyMode mode) {
      myBodyFaceContactMode =
         PropertyUtils.setModeAndUpdate (
            this, "bodyFaceContact", myBodyFaceContactMode, mode);
   }

   public PropertyMode getBodyFaceContactMode() {
      return myBodyFaceContactMode;
   }

   /** 
    * Gets the default contact compliance associated will all collision
    * behaviors.
    * 
    * @return contact compliance
    */
   public double getCompliance() {
      return myCompliance;
   }

   /** 
    * Sets the default contact compliance associated with all collision
    * behaviors.
    * 
    * @param c new contact compliance
    */
   public void setCompliance (double c) {
      myCompliance = c;
      myComplianceMode =
         PropertyUtils.propagateValue (
            this, "compliance", myCompliance, myComplianceMode);      
   }

   public void setComplianceMode (PropertyMode mode) {
      myComplianceMode =
         PropertyUtils.setModeAndUpdate (
            this, "compliance", myComplianceMode, mode);
   }

   public PropertyMode getComplianceMode() {
      return myComplianceMode;
   }

   /** 
    * Gets the default contact damping associated with all collision
    * behaviors.
    * 
    * @return contact damping
    */
   public double getDamping() {
      return myDamping;
   }

   /** 
    * Sets the default contact damping associated with all collision behaviors.
    * 
    * @param d new contact damping
    */
   public void setDamping (double d) {
      myDamping = d;
      myDampingMode =
      PropertyUtils.propagateValue (
         this, "damping", myDamping, myDampingMode);      
   }

   public void setDampingMode (PropertyMode mode) {
      myDampingMode =
         PropertyUtils.setModeAndUpdate (this, "damping", myDampingMode, mode);
   }

   public PropertyMode getDampingMode() {
      return myDampingMode;
   }

   /**
    * Returns the desired collision acceleration. See {@link #setAcceleration}.
    */
   public double getAcceleration() {
      return myAcceleration;
   }

   /**
    * Sets a desired acceleration for collision response which will be used to
    * automatically compute collision compliance if the specified compliance
    * (as returned by {@link #getCompliance}) is zero.
    *
    * <p>This property is currently experimental and not guaranteed to produce
    * reliable results.
    *
    * @param acc desired collision acceleration
    */
   public void setAcceleration (double acc) {
      myAcceleration = acc;
      myAccelerationMode =
      PropertyUtils.propagateValue (
         this, "acceleration", myAcceleration, myAccelerationMode);        
   }

   public void setAccelerationMode (PropertyMode mode) {
      myAccelerationMode =
         PropertyUtils.setModeAndUpdate (
            this, "acceleration", myAccelerationMode, mode);
   }

   public PropertyMode getAccelerationMode() {
      return myAccelerationMode;
   }

   public boolean getDrawIntersectionContours() {
      return myDrawIntersectionContours;
   }

   public void setDrawIntersectionContours (boolean enable) {
      myDrawIntersectionContours = enable;
      myDrawIntersectionContoursMode =
         PropertyUtils.propagateValue (
            this, "drawIntersectionContours",
            myDrawIntersectionContours, myDrawIntersectionContoursMode);      
   }

   public void setDrawIntersectionContoursMode (PropertyMode mode) {
      myDrawIntersectionContoursMode =
         PropertyUtils.setModeAndUpdate (
            this, "drawIntersectionContours",
            myDrawIntersectionContoursMode, mode);
   }

   public PropertyMode getDrawIntersectionContoursMode() {
      return myDrawIntersectionContoursMode;
   }

   public boolean getDrawIntersectionFaces() {
      return myDrawIntersectionFaces;
   }

   public void setDrawIntersectionFaces (boolean enable) {
      myDrawIntersectionFaces = enable;
      myDrawIntersectionFacesMode =
         PropertyUtils.propagateValue (
            this, "drawIntersectionFaces",
            myDrawIntersectionFaces, myDrawIntersectionFacesMode);      
   }

   public void setDrawIntersectionFacesMode (PropertyMode mode) {
      myDrawIntersectionFacesMode =
         PropertyUtils.setModeAndUpdate (
            this, "drawIntersectionFaces", myDrawIntersectionFacesMode, mode);
   }

   public PropertyMode getDrawIntersectionFacesMode() {
      return myDrawIntersectionFacesMode;
   }

   public boolean getDrawIntersectionPoints() {
      return myDrawIntersectionPoints;
   }

   public void setDrawIntersectionPoints (boolean enable) {
      myDrawIntersectionPoints = enable;
      myDrawIntersectionPointsMode =
         PropertyUtils.propagateValue (
            this, "drawIntersectionPoints",
            myDrawIntersectionPoints, myDrawIntersectionPointsMode);      
   }

   public void setDrawIntersectionPointsMode (PropertyMode mode) {
      myDrawIntersectionPointsMode =
         PropertyUtils.setModeAndUpdate (
            this, "drawIntersectionPoints", myDrawIntersectionPointsMode, mode);
   }

   public PropertyMode getDrawIntersectionPointsMode() {
      return myDrawIntersectionPointsMode;
   }

   public boolean getDrawContactNormals() {
      return myDrawContactNormals;
   }

   public void setDrawContactNormals (boolean enable) {
      myDrawContactNormals = enable;
      myDrawContactNormalsMode =
         PropertyUtils.propagateValue (
            this, "drawContactNormals",
            myDrawContactNormals, myDrawContactNormalsMode);      
   }

   public void setDrawContactNormalsMode (PropertyMode mode) {
      myDrawContactNormalsMode =
         PropertyUtils.setModeAndUpdate (
            this, "drawContactNormals", myDrawContactNormalsMode, mode);
   }

   public PropertyMode getDrawContactNormalsMode() {
      return myDrawContactNormalsMode;
   }

   public boolean getDrawContactForces() {
      return myDrawContactForces;
   }

   public void setDrawContactForces (boolean enable) {
      myDrawContactForces = enable;
      myDrawContactForcesMode =
         PropertyUtils.propagateValue (
            this, "drawContactForces",
            myDrawContactForces, myDrawContactForcesMode);
   }

   public void setDrawContactForcesMode (PropertyMode mode) {
      myDrawContactForcesMode =
         PropertyUtils.setModeAndUpdate (
            this, "drawContactForces", myDrawContactForcesMode, mode);
   }

   public PropertyMode getDrawContactForcesMode() {
      return myDrawContactForcesMode;
   }

   public void setColorMapInterpolation (ColorInterpolation interp) {
      myColorMapInterpolation = interp;
      myColorMapInterpolationMode =
         PropertyUtils.propagateValue (
            this, "colorMapInterpolation",
            myColorMapInterpolation, myColorMapInterpolationMode);
   }
   
   public ColorInterpolation getColorMapInterpolation() {
      return myColorMapInterpolation;
   }

   public void setColorMapInterpolationMode (PropertyMode mode) {
      myColorMapInterpolationMode =
         PropertyUtils.setModeAndUpdate (
            this, "colorMapInterpolation", myColorMapInterpolationMode, mode);
   }

   public PropertyMode getColorMapInterpolationMode() {
      return myColorMapInterpolationMode;
   }

   public ColorMapType getDrawColorMap() {
      return myDrawColorMap;
   }

   public void setDrawColorMap (ColorMapType type) {
      myDrawColorMap = type;
      myDrawColorMapMode =
         PropertyUtils.propagateValue (
            this, "drawColorMap",
            myDrawColorMap, myDrawColorMapMode);
   }

   public void setDrawColorMapMode (PropertyMode mode) {
      myDrawColorMapMode =
         PropertyUtils.setModeAndUpdate (
            this, "drawColorMap", myDrawColorMapMode, mode);
   }

   public PropertyMode getDrawColorMapMode() {
      return myDrawColorMapMode;
   }

   public int getColorMapCollidable() {
      return myColorMapCollidableNum;
   }

   public void setColorMapCollidable (int colNum) {
      if (colNum != 0 && colNum != 1) {
         throw new InternalErrorException ("colNum must be 0 or 1");
      }
      myColorMapCollidableNum = colNum;
      myColorMapCollidableMode =
         PropertyUtils.propagateValue (
            this, "colorMapCollidable",
            myColorMapCollidableNum, myColorMapCollidableMode);
   }

   public void setColorMapCollidableMode (PropertyMode mode) {
      myColorMapCollidableMode =
         PropertyUtils.setModeAndUpdate (
            this, "colorMapCollidable", myColorMapCollidableMode, mode);
   }

   public PropertyMode getColorMapCollidableMode() {
      return myColorMapCollidableMode;
   }

   public void setColorMap (ColorMapBase map) {
      ColorMapBase newMap = map.copy();
      PropertyUtils.updateCompositeProperty (
            this, "colorMap", myColorMap, newMap);
      myColorMap = newMap;
   }
   
   public ColorMapBase getColorMap() {
      return myColorMap;
   }
   
   public void setColorMapRange (ScalarRange range) {
      ScalarRange newRange = range.clone();
      PropertyUtils.updateCompositeProperty (
            this, "colorMapRange", myColorMapRange, newRange);
      myColorMapRange = newRange;
   }
   
   public ScalarRange getColorMapRange() {
      return myColorMapRange;
   }
   
   public ContactForceBehavior getForceBehavior () {
      return myForceBehavior;
   }

   public void setForceBehavior (ContactForceBehavior fb) {
      myForceBehavior = fb;
   }

   /** 
    * Returns the collision method to be used for collisions.
    * 
    * @return collision method
    */
   public Method getMethod() {
      return myMethod;
   }

   /** 
    * Set the collision method to be used for collisions. The default value is
    * {@link Method#DEFAULT}, which means that the method will be determined
    * based on the colliding bodies.
    * 
    * @param method collision method to be used
    */
   public void setMethod (Method method) {
      myMethod = method;
      myMethodMode =
         PropertyUtils.propagateValue (
            this, "method", myMethod, myMethodMode);      
   }

   public void setMethodMode (PropertyMode mode) {
      myMethodMode =
         PropertyUtils.setModeAndUpdate (this, "method", myMethodMode, mode);
   }

   public PropertyMode getMethodMode() {
      return myMethodMode;
   }

   /** 
    * Returns the collider type to be used for determining collisions.
    * 
    * @return collider type
    */
   public ColliderType getColliderType() {
      return myColliderType;
   }

   /** 
    * Set the collider type to be used for determining collisions.
    * 
    * @param ctype type new collider type
    */
   public void setColliderType (ColliderType ctype) {
      myColliderType = ctype;
      myColliderTypeMode =
         PropertyUtils.propagateValue (
            this, "colliderType", myColliderType, myColliderTypeMode);      
      // changing the collider type will invalidate previous state information
      notifyParentOfChange (new DynamicActivityChangeEvent (this));
   }

   public void setColliderTypeMode (PropertyMode mode) {
      ColliderType prev = myColliderType;
      myColliderTypeMode =
         PropertyUtils.setModeAndUpdate (
            this, "colliderType", myColliderTypeMode, mode);
      if (myColliderType != prev) {
         // changing the collider type will invalidate previous state information
         notifyParentOfChange (new DynamicActivityChangeEvent (this));
      }
   }

   public PropertyMode getColliderTypeMode() {
      return myColliderTypeMode;
   }

   // end of property accessors

   // behavior and response accessors

   public CollisionBehaviorList behaviors() {
      return myBehaviors;
   }

   public int numBehaviors() {
      return myBehaviors.size();
   }
        
   public CollisionBehavior getBehavior (int idx) {
      return myBehaviors.get (idx);
   }

   public CollisionResponseList responses() {
      return myResponses;
   }

   public int numResponses() {
      return myResponses.size();
   }
        
   public CollisionResponse getResponse (int idx) {
      return myResponses.get (idx);
   }

   /**
    * Implements {@link
    * MechModel#setDefaultCollisionBehavior(Collidable.Group,Collidable.Group,CollisionBehavior)
    * MechModel.setDefaultCollisionBehavior(groupA,groupB,behavior)}; see
    * documentation for that method.
    *
    * @param groupA first generic collidable group
    * @param groupB second generic collidable group
    * @param behavior desired collision behavior
    */
   public void setDefaultBehavior (
      Group groupA, Group groupB, CollisionBehavior behavior) {

      /**
       * This method examine the specified collision group pair and sets the
       * appropriate primary default collisions pairs, which are Rigid-Rigid,
       * Deformable-Rigid, Deformable-Deformable, and Deformable-Self.
       */
      validateDefaultPair (groupA, groupB);
      // first see if self collisions are included
      if (groupA == Collidable.Self ||
          groupB == Collidable.Self ||
          (groupA == Collidable.All && groupB != Collidable.Rigid) ||
          (groupB == Collidable.All && groupA != Collidable.Rigid)) {
         myDefaultDeformableSelf.set (behavior);
      }
      // for remainder, All is equivalent to Bodies
      if (groupA == Collidable.All) {
         groupA = Collidable.AllBodies;
      }
      if (groupB == Collidable.All) {
         groupB = Collidable.AllBodies;
      }
      if (groupA == Collidable.AllBodies && groupB == Collidable.AllBodies) {
         // set all body defaults
         myDefaultRigidRigid.set (behavior);
         myDefaultDeformableRigid.set (behavior);
         myDefaultDeformableDeformable.set (behavior);
      }
      else if (groupA == Collidable.AllBodies) {
         if (groupB == Collidable.Rigid) {
            myDefaultRigidRigid.set (behavior);
            myDefaultDeformableRigid.set (behavior);
         }
         else if (groupB == Collidable.Deformable) {
            myDefaultDeformableRigid.set (behavior);
            myDefaultDeformableDeformable.set (behavior);
         }
      }
      else if (groupB == Collidable.AllBodies) {
         if (groupA == Collidable.Rigid) {
            myDefaultRigidRigid.set (behavior);
            myDefaultDeformableRigid.set (behavior);
         }
         else if (groupA == Collidable.Deformable) {
            myDefaultDeformableRigid.set (behavior);
            myDefaultDeformableDeformable.set (behavior);
         }
      }
      // if we got this far then only rigid or deformable specified
      else if (groupA == Collidable.Rigid) {
         if (groupB == Collidable.Rigid) {
            myDefaultRigidRigid.set (behavior);
         }
         else if (groupB == Collidable.Deformable) {
            myDefaultDeformableRigid.set (behavior);
         }
      }
      else if (groupA == Collidable.Deformable) {
         if (groupB == Collidable.Rigid) {
            myDefaultDeformableRigid.set (behavior);
         }
         else if (groupB == Collidable.Deformable) {
            myDefaultDeformableDeformable.set (behavior);
         }
      }
      // will to rebuild internal structures
      myBehaviorStructuresValid = false;
      notifyParentOfChange (new StructureChangeEvent (this));

   }

   /** 
    * Implements for {@link MechModel#getDefaultCollisionBehavior
    * MechModel#getDefaultCollisionBehavior(groupA,groupB)}; see documentation
    * for that method.
    * 
    * @param groupA first generic collidable group
    * @param groupB second generic collidable group
    * @return default collision behavior for the indicted collidable groups.
    */
   public CollisionBehavior getDefaultBehavior (Group groupA, Group groupB) {

      validateDefaultPair (groupA, groupB);

      /**
       * These specified collidable groups are restricted to Rigid-Rigid,
       * Deformable-Rigid, Deformable-Deformable, and Deformable-Self, because
       * those are the only ones for which behaviors are actually specified.
       */
      if (groupA==Collidable.All || groupA==Collidable.AllBodies || 
          groupB==Collidable.All || groupB==Collidable.AllBodies) {
         throw new IllegalArgumentException (
            "Collidable groups restricted to Rigid, Deformable, and Self");
      }
      CollidablePair pair = new CollidablePair (groupA, groupB);
      if (groupA == Collidable.Rigid) {
         if (groupB == Collidable.Rigid) {
            return myDefaultRigidRigid;
         }
         else if (groupB == Collidable.Deformable) {
            return myDefaultDeformableRigid;
         }         
      }
      else if (groupA == Collidable.Deformable) {
         if (groupB == Collidable.Rigid) {
            return myDefaultDeformableRigid;
         }
         else if (groupB == Collidable.Deformable) {
            return myDefaultDeformableDeformable;
         }
         else if (groupB == Collidable.Self) {
            return myDefaultDeformableSelf;
         }
      }
      else if (groupA == Collidable.Self) {
         if (groupB == Collidable.Deformable) {
            return myDefaultDeformableSelf;
         }
      }
      throw new InternalErrorException (
         "No default behavior defined for "+pair);
   }

   /**
    * Checks to see if a collidable is contained within the component hierarchy
    * under a MechModel.
    */
   private void checkMechModelInclusion (MechModel mech, Collidable col) {
      if (!ModelComponentBase.recursivelyContains (mech, col)) {
         throw new IllegalArgumentException (
            "Collidable "+ComponentUtils.getPathName(col) +
            " not contained within MechModel "+ComponentUtils.getPathName(mech));
      }
   }

   /**
    * If two collidables are sub-collidables, then return the
    * nearest compound collidable that is an ancestor of both,
    * or <code>null</code> if no such collidable exists.
    */
   static Collidable nearestCommonCollidableAncestor (
      Collidable c0, Collidable c1) {

      Collidable ancestor0 = c0.getCollidableAncestor();
      Collidable ancestor1 = c1.getCollidableAncestor();

      if (ancestor0 == null || ancestor1 == null) {
         // one or both don't have ancestors
         return null;
      }
      else if (ancestor0.getCollidableAncestor() == null &&
               ancestor1.getCollidableAncestor() == null) {
         // both ancestors are at the top; just see if thay are the same
         return (ancestor0 == ancestor1 ? ancestor0 : null);
      }
      else {
         throw new UnsupportedOperationException (
            "Collidable tree depths greater than 1 not currently supported");
      }
   }
   
   static boolean isCollidableBody (Collidable c) {
      // XXX Need to call isCollidable() first, because FemModel3d uses that to
      // trigger on-demand surface mesh generation, without which the actual
      // FemMeshComp object containing the surface will have a null mesh.
      if (!isCollidable(c)) {
         return false;
      }
      else if (c instanceof CollidableBody) {
         // XXX hack until RigidBody and RigidCompositeBody are merged
         //!(c instanceof RigidBody)) {
         CollidableBody body = (CollidableBody)c;
         return (body.getCollisionMesh() != null);
      }
      else {
         return false;
      }
   }
   
   static boolean isCollidable (Collidable c) {
      return c.getCollidable() != Collidability.OFF;
   }

   static boolean isExternallyCollidable (Collidable c) {
      Collidability ca = c.getCollidable();
      return (ca == Collidability.ALL || ca == Collidability.EXTERNAL);
   }

   static boolean isInternallyCollidable (Collidable c) {
      Collidability ca = c.getCollidable();
      return (ca == Collidability.ALL || ca == Collidability.INTERNAL);
   }

   static ArrayList<CollidableBody> getExternallyCollidableBodies (
      Collidable c) {
      ArrayList<CollidableBody> bodies = new ArrayList<CollidableBody>();
      if (isCollidableBody (c) && isExternallyCollidable (c)) {
         bodies.add ((CollidableBody)c);
      }
      else {
         getExternallyCollidableBodies (bodies, c);
      }
      return bodies;
   }

   private static void getExternallyCollidableBodies (
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

   static ArrayList<CollidableBody> getInternallyCollidableBodies (
      Collidable c) {
      ArrayList<CollidableBody> bodies = new ArrayList<CollidableBody>();
      // it is assumed that c is a compounf=d collidable
      getInternallyCollidableBodies (bodies, c);
      return bodies;
   }

   private static void getInternallyCollidableBodies (
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
   
   void validateDefaultPair (Group g0, Group g1) {
      String errMsg = null;
      if ((g0 == Collidable.Self && g1 == Collidable.Rigid) ||
          (g0 == Collidable.Rigid && g1 == Collidable.Self)) { 
         errMsg = "Rigid cannot be combined with Self";
      }
      else if (g0 == Collidable.Self && g1 == Collidable.Self) {
         errMsg = "Self cannot be combined with Self";
      }
      if (errMsg != null) {
         throw new IllegalArgumentException ("Invalid default pair: "+errMsg);
      }
   }
   
   /**
    * Make sure a specified collidable pair is suitable for
    * <code>setBehavior()</code>, <code>setResponse()</code>, etc.  Also, swap
    * order if necessary to ensure that c0 is specific, and if c0 == c1, return
    * (c0, Self) instead.
    */
   CollidablePair validateBehaviorResponsePair (
      Collidable c0, Collidable c1, boolean requiresLowestCommonModel) {
      
      if (c0 instanceof Group) {
         throw new IllegalArgumentException (
            "First collidable cannot be a group: " +
            new CollidablePair(c0,c1));
      }
      
      // make sure c0 is included under the MechModel
      checkMechModelInclusion (myMechModel, c0);
      if (!(c1 instanceof Group)) {
         // c1 is specific too
         // make sure c1 is also included under the MechModel
         checkMechModelInclusion (myMechModel, c1);
         if (requiresLowestCommonModel) {
            // make sure that MechModel is the lowest MechModel that
            // contains both c0 and c1         
            if (MechModel.lowestCommonModel (c0, c1) != myMechModel) {
               throw new IllegalArgumentException (
                  "Both collidables belong to a lower MechModel");
            }
         }
         // make sure that c0 and c1 are not descendents of each other
         if (ModelComponentBase.recursivelyContains (c0, c1)) {
            throw new IllegalArgumentException (
               "Collidable c1 is a descendant of collidable c0");
         }
         if (ModelComponentBase.recursivelyContains (c1, c0)) {
            throw new IllegalArgumentException (
               "Collidable c0 is a descendant of collidable c1");
         }
      }
      if (c0 == c1 || c1 == Collidable.Self) {
         // pair specifies self collision
         if (!c0.isDeformable() || !c0.isCompound()) {
            throw new IllegalArgumentException (
               "Component "+ComponentUtils.getPathName(c0)+
               " cannot self-intersect");
         }
         return new CollidablePair (c0, Group.Self);
      }
      else {
         return new CollidablePair (c0, c1);
      }
   }

   /** 
    * Implements {@link
    * MechModel#setCollisionBehavior(Collidable,Collidable,CollisionBehavior)
    * MechModel.setCollisionBehavior(c0,c1,behavior)}; see documentation for that
    * method.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @param behavior desired collision behavior
    */
   public void setBehavior (
      Collidable c0, Collidable c1, CollisionBehavior behavior) {

      if (behavior.getParent() == myBehaviors) {
         throw new IllegalArgumentException (
            "collision manager already contains specified behavior component");
      }
      CollidablePair pair = validateBehaviorResponsePair (c0, c1, true);
      CollisionBehavior prev = myBehaviors.get (pair);
      if (prev != null) {
         myBehaviors.remove (prev);
      }
      behavior.setName (pair.createComponentName(myMechModel));
      behavior.setCollidablePair (pair);
      myBehaviors.add (behavior);
      myBehaviorStructuresValid = false;
   }

   /**
    * Implements {@link 
    * MechModel#getCollisionBehavior(Collidable,Collidable)
    * MechModel.getCollisionBehavior(c0,c1)}. See documentation for that method.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return specific behavior for this pair of collidables
    */
   public CollisionBehavior getBehavior (Collidable c0, Collidable c1) {
      CollidablePair pair = validateBehaviorResponsePair (c0, c1, true);
      return myBehaviors.get (pair);
   }

   /** 
    * Implements {@link 
    * MechModel#clearCollisionBehavior(Collidable,Collidable)
    * MechModel.clearCollisionBehavior(c0,c1)}. See documentation for that method.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return <code>true</code> if the specific behavior had been set
    * and was removed for the indicated collidable pair.
    */
   public boolean clearBehavior (Collidable c0, Collidable c1) {
      CollidablePair pair = validateBehaviorResponsePair (c0, c1, true);
      CollisionBehavior behav = myBehaviors.get (pair);
      if (behav != null) {
         myBehaviors.remove (behav);
         myBehaviorStructuresValid = false;
         return true;
      }
      else {
         return false;
      }
   }

   /** 
    * Implements {@link 
    * MechModel#clearCollisionBehaviors()
    * MechModel.clearCollisionBehaviors()}. See documentation for that method. 
    */
   public void clearBehaviors () {
      // Don't remove [0,numDefaultPairs()-1] because these are reserved
      // for default behaviors. Proceed in reverse order for greater efficiency
      // because myBehaviors uses an array-list implementation.
      for (int k=myBehaviors.size()-1; k>=numDefaultPairs(); k--) {
         myBehaviors.remove(k);
      }
   }   

   /** 
    * Implements {@link
    * MechModel#setCollisionResponse(Collidable,Collidable,CollisionResponse)
    * MechModel.setCollisionResponse(c0,c1,response)}; see documentation for that
    * method.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @param response desired collision response
    */
   public void setResponse (
      Collidable c0, Collidable c1, CollisionResponse response) {

      if (response.getParent() == myResponses) {
         throw new IllegalArgumentException (
            "collision manager already contains specified response component");
      }
      CollidablePair pair = validateBehaviorResponsePair (c0, c1, false);
      CollisionResponse prev = myResponses.get (pair);
      if (prev != null) {
         myResponses.remove (prev);
      }
      response.setName (pair.createComponentName(myMechModel));
      response.setCollidablePair (pair);
      myResponses.add (response);
      myResponseStructuresValid = false;
   }

   /**
    * Implements {@link 
    * MechModel#getCollisionResponse(Collidable,Collidable)
    * MechModel.getCollisionResponse(c0,c1)}. See documentation for that method.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return specific response for this pair of collidables
    */
   public CollisionResponse getResponse (Collidable c0, Collidable c1) {
      CollidablePair pair = validateBehaviorResponsePair (c0, c1, false);
      return myResponses.get (pair);
   }

   /** 
    * Implements {@link 
    * MechModel#clearCollisionResponse(Collidable,Collidable)
    * MechModel.clearCollisionResponse(c0,c1)}. See documentation for that method.
    * 
    * @param c0 first collidable
    * @param c1 second collidable
    * @return <code>true</code> if the specific response had been set
    * and was removed for the indicated collidable pair.
    */
   public boolean clearResponse (Collidable c0, Collidable c1) {
      CollidablePair pair = validateBehaviorResponsePair (c0, c1, false);
      CollisionResponse behav = myResponses.get (pair);
      if (behav != null) {
         myResponses.remove (behav);
         myResponseStructuresValid = false;
         return true;
      }
      else {
         return false;
      }
   }

   /** 
    * Implements {@link 
    * MechModel#clearCollisionResponses()
    * MechModel.clearCollisionResponses()}. See documentation for that method. 
    */
   public void clearResponses () {
      myResponses.removeAll();
   }   

   private void setExplicitBehavior (
      HashMap<CollidablePair,CollisionBehavior> explicitMap,
      CollidablePair pair, CollisionBehavior behavior) {

      Collidable c0 = pair.myComp0;
      Collidable c1 = pair.myComp1;
      
      if (isCollidableBody (c0) && isCollidableBody (c1)) {
         if (nearestCommonCollidableAncestor (c0, c1) != null) {
            // if c0 and c1 have a common ancester, INTERNAL collidability
            // must be enabled
            if (isInternallyCollidable (c0) && isInternallyCollidable (c1)) {
               explicitMap.put (pair, behavior);
            }
         }
         else {
            // otherwise, EXTERNAL collidability must be enabled
            if (isExternallyCollidable (c0) && isExternallyCollidable (c1)) {
               explicitMap.put (pair, behavior);
            }
         }
      }
      // NOTE: self intersection case c0 == c1 should no happen because
      // it gets turned into c0,Self and handled before this method is called
      else {
         ArrayList<CollidableBody> bodiesA = getExternallyCollidableBodies (c0);
         ArrayList<CollidableBody> bodiesB = getExternallyCollidableBodies (c1);
         for (int i=0; i<bodiesA.size(); i++) {
            CollidableBody ai = bodiesA.get(i);
            for (int j=0; j<bodiesB.size(); j++) {
               CollidableBody bj = bodiesB.get(j);
               explicitMap.put (new CollidablePair(ai, bj), behavior);
            }
         }
      }
   }

   void recursivelyGetTopCollidables (
      List<Collidable> list, CompositeComponent comp) {
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get(i);
         if (c instanceof Collidable) {
            list.add ((Collidable)c);
         }
         else if (c instanceof CompositeComponent &&
                  !(c instanceof MechModel)) {
            recursivelyGetTopCollidables (list, (CompositeComponent)c);
         }
      }
   }

   public void getTopCollidables (List<Collidable> list) {
      recursivelyGetTopCollidables (list, myMechModel);
   }      
   
   void updateBehaviorStructures() {
      if (!myBehaviorStructuresValid) {
         
         ArrayList<CollidableBody> bodies;

         myExplicitBehaviors.clear();
         myRigidExtBehaviors.clear();
         myDeformableExtBehaviors.clear();
         myDeformableIntBehaviors.clear();

         // iterate through behavior list in reverse order so that the entries
         // in the behavior maps are sorted in decreasing order of priority.
         // Only go as low as myDefaultPairs.length because below that is
         // reserved for default behaviors.
         for (int k=myBehaviors.size()-1; k>=numDefaultPairs(); k--) {
            CollisionBehavior behav = myBehaviors.get(k);
            CollidablePair pair = behav.getCollidablePair();
            Collidable c0 = pair.get(0);
            Collidable c1 = pair.get(1);
            if (!(c0 instanceof Group) && (c1 instanceof Group)) {
               Group g1 = (Group)c1;
               if (g1.includesSelf()) {
                  if (myDeformableIntBehaviors.get (c0) == null) {
                     myDeformableIntBehaviors.put (c0, behav);
                  }
               }
               if (g1.includesRigid() || g1.includesDeformable()) {
                  bodies = getExternallyCollidableBodies (c0);
                  if (g1.includesRigid()) {
                     for (CollidableBody cb : bodies) {
                        if (myRigidExtBehaviors.get (cb) == null) {
                           myRigidExtBehaviors.put (cb, behav);
                        }
                     }
                  }
                  if (g1.includesDeformable()) {
                     for (CollidableBody cb : bodies) {
                        if (myDeformableExtBehaviors.get (cb) == null) {
                           myDeformableExtBehaviors.put (cb, behav);
                        }
                     }
                  }
               }
            }
            else {
               setExplicitBehavior (myExplicitBehaviors, pair, behav);
            }
         }
         // now update the lists of collidable bodies
         ArrayList<Collidable> collidables = new ArrayList<Collidable>();
         getTopCollidables (collidables);

         myRigidExts.clear();
         myDeformableExts.clear();
         myDeformableInts.clear();

         ArrayList<CollidableBody> list = new ArrayList<CollidableBody>();

         for (Collidable c : collidables) {
            if (c.isDeformable()) {
               if (isCollidableBody (c)) {
                  myDeformableExts.add ((CollidableBody)c); // NOT CALLED
               }
               else {
                  list.clear();
                  getExternallyCollidableBodies (list, c);
                  myDeformableExts.addAll (list);

                  list.clear();
                  getInternallyCollidableBodies (list, c);
                  myDeformableInts.addAll (list);
               }
            }
            else {
               if (isCollidableBody (c)) {
                  myRigidExts.add ((CollidableBody)c);
               }
               else {
                  list.clear();
                  getExternallyCollidableBodies (list, c);
                  myRigidExts.addAll (list);
               }
            }
         }
         myBehaviorStructuresValid = true;
      }
   }

   void updateResponseStructures() {
      if (!myResponseStructuresValid) {

         myPairResponses.clear();
         myGroupResponses.clear();

         for (CollisionResponse resp : myResponses) {
            updateResponseStructures (resp);
         }
         myResponseStructuresValid = true;
      }
   }

   private boolean nameEquals (Collidable c, String name) {
      String str = ComponentUtils.getPathName (c);
      return str.equals (name);
   }

   void addPairResponse (
      CollidableBody cb0, CollidableBody cb1, CollisionResponse resp) {
      
      CollidablePair pair = new CollidablePair (cb0, cb1);
      ArrayList<CollisionResponse> resps = myPairResponses.get (pair);
      if (resps == null) {
         resps = new ArrayList<CollisionResponse>();
         myPairResponses.put (pair, resps);
      }
      resps.add (resp);
   }

   void addGroupResponse (
      CollidableBody cb, CollisionResponse resp) {
      
      ArrayList<CollisionResponse> resps = myGroupResponses.get (cb);
      if (resps == null) {
         resps = new ArrayList<CollisionResponse>();
         myGroupResponses.put (cb, resps);
      }
      resps.add (resp);
   }

   void updateResponseStructures (CollisionResponse resp) {

      CollidablePair pair = resp.getCollidablePair();
      Collidable c0 = pair.get(0);
      Collidable c1 = pair.get(1);

      if (c1 instanceof Group) {
         //String name0 = ComponentUtils.getPathName (c0);

         Group g1 = (Group)c1;
         if (g1.includesSelf() && c0.isDeformable()) {
            if (c0.isCompound()) {
               ArrayList<CollidableBody> ints =
                  getInternallyCollidableBodies (c0);
               for (int i=0; i<ints.size(); i++) {
                  CollidableBody ci = ints.get(i);
                  for (int j=i+1; j<ints.size(); j++) {
                     CollidableBody cj = ints.get(j);
                     addPairResponse (ci, cj, resp);
                  }
               }
            }
            else if (c0 instanceof CollidableBody) {
               CollidableBody cb0 = (CollidableBody)c0;
               Collidable a0 = c0.getCollidableAncestor();
               if (a0 != null && a0.isDeformable()) {
                  ArrayList<CollidableBody> ints =
                     getInternallyCollidableBodies (a0);
                  for (int i=0; i<ints.size(); i++) {
                     CollidableBody ci = ints.get(i);
                     if (ci != cb0) {
                        addPairResponse (cb0, ci, resp);
                     }
                  }
               }
            }
         }
         if (g1.includesRigid() || g1.includesDeformable()) {
            ArrayList<CollidableBody> exts0 = getExternallyCollidableBodies (c0);
            for (int i=0; i<exts0.size(); i++) {
               CollidableBody ci = exts0.get(i);
               addGroupResponse (ci, resp);
            }
         }         
      }
      else {
         Collidable ancestor = nearestCommonCollidableAncestor (c0, c1);
         if (ancestor != null) {
            addPairResponse ((CollidableBody)c0, (CollidableBody)c1, resp);
         }
         else {
            ArrayList<CollidableBody> exts0 = getExternallyCollidableBodies (c0);
            ArrayList<CollidableBody> exts1 = getExternallyCollidableBodies (c1);
            for (int i=0; i<exts0.size(); i++) {
               CollidableBody ci = exts0.get(i);
               for (int j=0; j<exts1.size(); j++) {
                  CollidableBody cj = exts1.get(j);
                  addPairResponse (ci, cj, resp);
               }
            }         
         }
      }
   }

   int updateGroupResponses (
      CollidableBody cb0, CollidableBody cb1, CollisionHandler ch) {

      int numr = 0;
      //String name0 = ComponentUtils.getPathName (cb0);
      //String name1 = ComponentUtils.getPathName (cb1);

      ArrayList<CollisionResponse> resps = myGroupResponses.get (cb0);
      if (resps != null) {
         for (CollisionResponse resp : resps) {
            Group g1 = (Group)resp.getCollidable(1);
            if (cb1.isDeformable()) {
               if (g1.includesDeformable()) {
                  resp.addHandler (ch);
                  numr++;
               }
            }
            else {
               if (g1.includesRigid()) {
                  resp.addHandler (ch);
                  numr++;
               }
            }
         }
      }
      return numr;
   }

   int updateResponses (CollisionHandler ch) {
      CollidablePair pair = ch.getCollidablePair();
      CollidableBody cb0 = (CollidableBody)pair.myComp0;
      CollidableBody cb1 = (CollidableBody)pair.myComp1;

//      String name0 = ComponentUtils.getPathName (cb0);
//      String name1 = ComponentUtils.getPathName (cb1);

      int refcnt = 0;
      ArrayList<CollisionResponse> resps = myPairResponses.get (pair);
      if (resps != null) {
         for (CollisionResponse resp : resps) {
            resp.addHandler (ch);
            refcnt++;
         }
      }
      if (nearestCommonCollidableAncestor (cb0, cb1) == null) {
         refcnt += updateGroupResponses (cb0, cb1, ch);
         refcnt += updateGroupResponses (cb1, cb0, ch);
      }
      return refcnt;
   }

   private void updateHandlerTable() {
      if (!myHandlerTableValid) {
         MechModel topMech = MechModel.topMechModel(this);
         topMech.updateCollidableBodyIndices();
         ArrayList<CollidableBody> bodies = myMechModel.getCollidableBodies();
         myHandlerTable.reinitialize (bodies);     
         myHandlers.clear();
         collectHandlers (myHandlers);
         myHandlerTableValid = true;
      }
   }

   CollisionHandlerTable getHandlerTable() {
      updateHandlerTable();
      return myHandlerTable;
   }

   private CollisionBehavior dominantBehavior (
      CollisionBehavior b0, CollisionBehavior b1, CollisionBehavior def) {

      if (b0 != null && b1 != null) {
         if (myBehaviors.indexOf(b0) < myBehaviors.indexOf(b1)) {
            return b1;
         }
         else {
            return b0;
         }
      }
      else if (b0 != null) {
         return b0;
      }
      else if (b1 != null) {
         return b1;
      }
      else {
         return def;
      }
   }

   boolean debug = false;

   String toStr (CollisionBehavior behav) {
      return CollisionManagerTest.toStr(behav);
   }

   private CollisionBehavior getExternalBehavior (
      CollidableBody c0, CollidableBody c1) {
      CollisionBehavior behav0;         
      CollisionBehavior behav1;

      if (c0.isDeformable()) {
         behav1 = myDeformableExtBehaviors.get (c1);
         if (c1.isDeformable()) {
            behav0 = myDeformableExtBehaviors.get (c0);
            return dominantBehavior (
               behav0, behav1, myDefaultDeformableDeformable);
         }
         else {
            behav0 = myRigidExtBehaviors.get (c0);
            return dominantBehavior (
               behav0, behav1, myDefaultDeformableRigid);
         }
      }
      else {
         behav1 = myRigidExtBehaviors.get (c1);
         if (c1.isDeformable()) {
            behav0 = myDeformableExtBehaviors.get (c0);
            return dominantBehavior (
               behav0, behav1, myDefaultDeformableRigid);
         }
         else {
            behav0 = myRigidExtBehaviors.get (c0);
            return dominantBehavior (
               behav0, behav1, myDefaultRigidRigid);
         }
      }
   }

   private CollisionBehavior getInternalBehavior (Collidable c0) {
      CollisionBehavior behav = myDeformableIntBehaviors.get (c0);
      if (behav == null) {
         return myDefaultDeformableSelf;
      }
      else {
         return behav;
      }
   }

   private CollisionBehavior getExplicitBehavior (
      CollidableBody c0, CollidableBody c1) {
      return myExplicitBehaviors.get (new CollidablePair (c0, c1));
   }

   private CollisionBehavior getBodyBehavior (
      CollidableBody c0, CollidableBody c1) {

      if (c0 == c1) {
         return null;
      }
      CollisionBehavior behav = 
         myExplicitBehaviors.get (new CollidablePair(c0, c1));
      if (behav != null) {
         return behav;
      }
      Collidable ancestor = nearestCommonCollidableAncestor (c0, c1);

      if (ancestor == null) {
         
         if (isExternallyCollidable (c0) &&
             isExternallyCollidable (c1)) {
            return getExternalBehavior (c0, c1);
         }
      }
      else if (ancestor.isDeformable()) {
         if (isInternallyCollidable (c0) &&
             isInternallyCollidable (c1)) {
            return getInternalBehavior (ancestor);
         }
      }
      return null;
   }

   /** 
    * Implements {@link 
    * MechModel#getActingCollisionBehavior
    * MechModel.getActingCollisionBehavior()}.
    * See documentation for that method.
    *
    * @param c0 first collidable
    * @param c1 second collidable
    * @return behavior for this pair of collidables, or <code>null</code>
    * if no common behavior is found.
    */
   public CollisionBehavior getActingBehavior (Collidable c0, Collidable c1) {
      updateBehaviorStructures();

      MechModel mech = MechModel.lowestCommonModel (c0, c1);
      return mech.getCollisionManager().doGetActingBehavior (c0, c1);
   }
   
   CollisionBehavior doGetActingBehavior (Collidable c0, Collidable c1) {
      updateBehaviorStructures();  

      if (c0 instanceof Group || c1 instanceof Group) {
         CollidablePair pair = new CollidablePair (c0, c1);
         throw new IllegalArgumentException (
            "Pair "+pair.toString(myMechModel)+" contains a collidable group");
      }
      CollidableBody cb0 = null;
      CollidableBody cb1 = null;
      if (isCollidableBody (c0)) {
         cb0 = (CollidableBody)c0;
      }
      if (isCollidableBody (c1)) {
         cb1 = (CollidableBody)c1;
      }
      if (cb0 != null && cb1 != null) {
         return getBodyBehavior (cb0, cb1);
      }
      else if (c0 == c1) {
         // requesting self collision behavior.
         if (!c0.isCompound() || !c0.isDeformable()) {
            return null;
         }
         ArrayList<CollidableBody> ibods = getInternallyCollidableBodies (c0);
         CollisionBehavior behavior = null;
         boolean behaviorSet = false;
         for (int i=0; i<ibods.size(); i++) {
            CollidableBody cbi = ibods.get(i);
            for (int j=i+1; j<ibods.size(); j++) {
               CollidableBody cbj = ibods.get(j);
               CollisionBehavior behav = getExplicitBehavior (cbi, cbj);
               if (!behaviorSet) {
                  behavior = behav;
                  behaviorSet = true;
               }
               else if (behavior != behav) {
                  return null;
               }
            }
         }
         if (behavior != null) {
            return behavior;
         }
         else {
            return getInternalBehavior (c0);
         }
      }
      else {
         // no behavior if one collidable is an ancestor of another
         if (c0.getCollidableAncestor() == c1 || 
             c1.getCollidableAncestor() == c0) {
            return null;
         }
         ArrayList<CollidableBody> ebods0 = getExternallyCollidableBodies (c0);
         ArrayList<CollidableBody> ebods1 = getExternallyCollidableBodies (c1);
         CollisionBehavior behavior = null;
         boolean behaviorSet = false;
         for (int i=0; i<ebods0.size(); i++) {
            CollidableBody cbi = ebods0.get(i);
            for (int j=0; j<ebods1.size(); j++) {
               CollidableBody cbj = ebods1.get(j);
               CollisionBehavior behav = getExplicitBehavior (cbi, cbj);
               if (behav == null) {
                  behav = getExternalBehavior (cbi, cbj);
               }
               if (!behaviorSet) {
                  behavior = behav;
                  behaviorSet = true;
               }
               else if (behavior != behav) {
                  return null;
               }
            }
         }
         return behavior;
      }
   }

   ContactInfo computeContactInfo (
      CollidableBody c0, CollidableBody c1, CollisionBehavior behav) {

      PolygonalMesh mesh0 = c0.getCollisionMesh();
      PolygonalMesh mesh1 = c1.getCollisionMesh();
      ContactInfo cinfo;
      ColliderType colliderType = behav.getColliderType();
      if (colliderType == ColliderType.SIGNED_DISTANCE) {
         // if using signed distance collider, at least one collidable
         // must be rigid and support signed distance grids
         if ((c0.isDeformable() || !c0.hasDistanceGrid()) &&
             (c1.isDeformable() || !c1.hasDistanceGrid())) {
            colliderType = ColliderType.AJL_CONTOUR;
         }
      }
      //FunctionTimer timer = new FunctionTimer();
      //timer.start();
      switch (colliderType) {
         case AJL_CONTOUR: {
            if (myAjlIntersector == null) {
               myAjlIntersector = new SurfaceMeshIntersector();
            }
            // types of regions that we need to compute for mesh0 and mesh1
            RegionType regions0 = RegionType.INSIDE;
            RegionType regions1 = RegionType.INSIDE;
            Method method = behav.getMethod();
            if (method != Method.VERTEX_EDGE_PENETRATION &&
                method != Method.CONTOUR_REGION &&
                behav.getBodyFaceContact() == false) {
               // vertex penetration method may not require computing
               // regions for both meshes
               if (CollisionHandler.isRigid (c0) && 
                   !CollisionHandler.isRigid (c1)) {
                  regions0 = RegionType.NONE;
               }
               else if (CollisionHandler.isRigid (c1) && 
                        !CollisionHandler.isRigid (c0)) {
                  regions1 = RegionType.NONE;
               }
            }
            cinfo = myAjlIntersector.findContoursAndRegions (
               mesh0, regions0, mesh1, regions1);
            break;
         }
         case TRI_INTERSECTION: {
            if (myTriTriCollider == null) {
               myTriTriCollider = new MeshCollider();
            }
            cinfo = myTriTriCollider.getContacts (mesh0, mesh1);
            break;
         }
         case SIGNED_DISTANCE: {
            if (mySDCollider == null) {
               mySDCollider = new SignedDistanceCollider();
            }
            DistanceGridComp gcomp0 = c0.getDistanceGridComp();
            DistanceGridComp gcomp1 = c1.getDistanceGridComp();
            cinfo = mySDCollider.getContacts (
               mesh0, gcomp0 != null ? gcomp0.getGrid() : null,
               mesh1, gcomp1 != null ? gcomp1.getGrid() : null);
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented collider type " + colliderType);
         }
      }
      return cinfo;    
   }
   
   void checkForContact (
      CollidableBody c0, CollidableBody c1, 
      CollisionBehavior behav, BehaviorSource src, boolean testMode) {
      
      if (c0.getCollidableIndex() > c1.getCollidableIndex()) {
         // swap the collidable references to reflect their handler order
         CollidableBody tmp = c0;
         c0 = c1;
         c1 = tmp;
      }
      ContactInfo cinfo;
      if (testMode) {
         cinfo = new ContactInfo (c0.getCollisionMesh(), c1.getCollisionMesh());
      }
      else {
         cinfo = computeContactInfo (c0, c1, behav);
         //timer.stop();
         //System.out.println ("time=" + timer.getTimeUsec());
         //cinfo = myCollider.getContacts (mesh0, mesh1);
      }
      if (cinfo != null) {
         addOrUpdateHandler (cinfo, c0, c1, behav, src);
      }     
   }

   CollisionBehavior getBehavior (
      CollidableBody c0, CollidableBody c1, BehaviorSource src) {
      switch (src) {
         case EXPLICIT: {
            return getExplicitBehavior (c0, c1);
         }
         case EXTERNAL: {
            return getExternalBehavior (c0, c1);
         }
         case INTERNAL: {
            Collidable ancestor = nearestCommonCollidableAncestor (c0, c1);
            if (ancestor != null) {
               return getInternalBehavior (ancestor);
            }
            else {
               return null;
            }
         }
         default: {
            throw new UnsupportedOperationException (
               "Unknown behavior source: "+ src);
         }
      }
   }
   
   /**
    * Check external collisions for different component lists cols0 and cols1.
    */
   void checkExternalCollisions (
      ArrayList<CollidableBody> cols0,
      ArrayList<CollidableBody> cols1,
      boolean testMode) {

      for (int i=0; i<cols0.size(); i++) {
         CollidableBody ci = cols0.get(i);
         for (int j=0; j<cols1.size(); j++) {
            CollidableBody cj = cols1.get(j);
            if (getExplicitBehavior (ci, cj) == null) {
               CollisionBehavior behav = getExternalBehavior (ci, cj);
               if (behav.isEnabled()) {
                  checkForContact (
                     ci, cj, behav, BehaviorSource.EXTERNAL, testMode);
               }
            }               
         }
      }
   }

   /**
    * Check external collisions among elements of the same component list cols
    */
   void checkExternalCollisions (
      ArrayList<CollidableBody> cols, boolean testMode) {

      for (int i=0; i<cols.size(); i++) {
         CollidableBody ci = cols.get(i);
         for (int j=i+1; j<cols.size(); j++) {
            CollidableBody cj = cols.get(j);
            if (getExplicitBehavior (ci, cj) == null &&
                nearestCommonCollidableAncestor (ci, cj) == null) {
               CollisionBehavior behav = getExternalBehavior (ci, cj);
               if (behav.isEnabled()) {
                  checkForContact (
                     ci, cj, behav, BehaviorSource.EXTERNAL, testMode);
               }
            }               
         }
      }
   }

   /**
    * Check internal collisions among elements of the same component list cols
    */
   void checkInternalCollisions (
      ArrayList<CollidableBody> cols, boolean testMode) {

      for (int i=0; i<cols.size(); i++) {
         CollidableBody ci = cols.get(i);
         for (int j=i+1; j<cols.size(); j++) {
            CollidableBody cj = cols.get(j);
            if (getExplicitBehavior (ci, cj) == null) {
               Collidable ancestor = nearestCommonCollidableAncestor (ci, cj);
               if (ancestor != null) {
                  CollisionBehavior behav = getInternalBehavior (ancestor);
                  if (behav.isEnabled()) {
                     checkForContact (
                        ci, cj, behav, BehaviorSource.INTERNAL, testMode);
                  }
               }
            }               
         }
      }
   }

   private void addOrUpdateHandler (
      ContactInfo cinfo, CollidableBody c0, CollidableBody c1,
      CollisionBehavior behav, BehaviorSource src) {

      CollisionHandler ch = myHandlerTable.get (c0, c1);
      if (ch == null) {
         //ch = new CollisionHandlerX (this, c0, c1, behav);
         ch = myHandlerTable.put (c0, c1, behav, src);
      }
      else {
         ch.setBehavior (behav, src);
      }
      ch.setActive (true);
      ch.myStateNeedsContactInfo = isVisible(ch);
      double pen = ch.computeCollisionConstraints (cinfo);
      if (pen > myMaxpen) {
         myMaxpen = pen;
      }
   }

   /**
    * Implements {@link 
    * MechModel#getCollisionResponse(Collidable,Collidable)
    * MechModel.getCollisionResponse(c0,c1)}.
    * See documentation for that method.
    *
    * @param c0 first collidable. Must be a specific collidable.
    * @param c1 second collidable(s). May be a specific collidable
    * or a colliable group.
    * @return collision response object for the specified collidables
    */
   public CollisionResponse getCollisionResponse (
      Collidable c0, Collidable c1) {

      if (c0 instanceof Group) {
         throw new IllegalArgumentException (
            "First collidable must not be a collidable group");
      }
      CollisionResponse resp = new CollisionResponse();
      resp.setCollidablePair (c0, c1);
      if (!(c1 instanceof Group)) {
         // collisions will be handled by the lowest common MechModel,
         // so look for handlers there
         MechModel mech = MechModel.lowestCommonModel (c0, c1);
         resp.collectHandlers (mech.getCollisionManager().myHandlerTable);
      }
      else {
         // collisions could be handled by any MechModel above the 
         // first collidable, so need to check all of these
         MechModel mech = MechModel.nearestMechModel (c0);
         resp.collectHandlers (mech.getCollisionManager().myHandlerTable);
         while (mech != myMechModel) {
            mech = MechModel.nearestMechModel (mech.getParent());
            resp.collectHandlers (mech.getCollisionManager().myHandlerTable);
         }
      }
      return resp;
   }

   // ===== structure methods =====

   @Override
   public void componentChanged (ComponentChangeEvent e) {
      if (e.getComponent() == myBehaviors) {
         myBehaviorStructuresValid = false;
      }
      else if (e.getComponent() == myBehaviors) {
         myBehaviorStructuresValid = false;
      }
      notifyParentOfChange (e);
   }

   public void initialize() {
      // need to call updateBehaviorStructures to check all collidables and 
      // cause FemModel3d to update its surface mesh components
      updateBehaviorStructures(); 
      for (CollisionHandler ch : collisionHandlers()) {
         ch.initialize();
      }
      if (myRigidRegionTol == -1) {
         setRigidRegionTol(getDefaultRigidRegionTol());
      }
      if (myRigidPointTol == -1) {
         setRigidPointTol(getDefaultRigidPointTol());
      }
      if (myContactNormalLen == Property.DEFAULT_DOUBLE) {
         setContactNormalLen(getDefaultContactNormalLen());
      }
   }

   public void clearCachedData () {
      myBehaviorStructuresValid = false;
      myResponseStructuresValid = false;
      myHandlerTableValid = false;
      myUsesBilateralConstraints = -1;
   }

   // ===== Scan and write code ====

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      super.scan (rtok, ref);
      setDefaultBehaviorVariables();
      myBehaviorStructuresValid = false;
      myBehaviorStructuresValid = false;
      myHandlerTableValid = false;
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (scanAttributeName (rtok, "forceBehavior")) {
         // TODO: scan force behavior
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }   

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (myForceBehavior != null) {
         pw.println ("forceBehavior=");
         // TODO: write force behavior
      }
      super.writeItems (pw, fmt, ancestor); 
   }

   // ===== ScalableUnits methods ======

   public void scaleDistance (double s) {
//      if (myPenetrationTol != -1) {
//         myPenetrationTol *= s;
//      }
      if (myRigidPointTol != -1) {
         myRigidPointTol *= s;
      }
      if (myRigidRegionTol != -1) {
         myRigidRegionTol *= s;
      }
      if (myContactNormalLen != Property.DEFAULT_DOUBLE) {
         myContactNormalLen *= s;
      }
      myContactForceLenScale *= s;
      myAcceleration *= s;
      myColorMapRange.scale (s);
      myRenderProps.scaleDistance (s);
      for (CollisionBehavior behav : myBehaviors) {
         behav.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      myCompliance /= s;
      myDamping *= s;
      myContactForceLenScale *= s;
      for (CollisionBehavior behav : myBehaviors) {
         behav.scaleMass (s);
      }
   }

   // ===== Constraint reduction code =====

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

   protected int markActiveMasters (
      ContactConstraint c, HashSet<DynamicComponent> marked) {
      return c.collectMasterComponents (marked, /*activeOnly=*/true);
   }

   public void reduceBilateralConstraints (
      ArrayList<ContactConstraint> bilaterals) {
      Collections.sort (bilaterals, new DescendingDistance());
      int[] dofs = new int[myMechModel.numActiveComponents()];
      myMechModel.getDynamicDOFs (dofs);

      boolean[] mx = new boolean[myMechModel.numActiveComponents()];
      HashSet<DynamicComponent> marked = new HashSet<>();

      // make sure each constraint has at least one unmarked active master
      for (int i=0; i<bilaterals.size(); i++) {
         ContactConstraint c = bilaterals.get(i);
         if (markActiveMasters (c, marked) == 0) {
            c.setActive (false);
         }
      }
   }

   // ==== Begin Constrainer implementation ====
   
   public int maxNumContourPoints = 0;
   
   public double updateConstraints (double t, int flags) {
      
      // this method will only be called from the top level
      if ((flags & MechSystem.UPDATE_CONTACTS) != 0) {
         // right now just leave the same contacts in place ...
         return myHandlers.size() == 0 ? -1 : 0;
      }

      myHandlers.clear();
      double maxpen = updateConstraints (myHandlers, t, flags);
      int nump = 0;
      for (CollisionHandler ch : myHandlers) {
         nump += ch.getLastContactInfo().numContourPoints();
      }
      if (nump > maxNumContourPoints ) {
         maxNumContourPoints = nump;
      }
      return myHandlers.size() == 0 ? -1 : maxpen;
   }
   
   double updateConstraints (
      ArrayList<CollisionHandler> handlers, double t, int flags) {

      boolean testMode = ((flags & CONTACT_TEST_MODE) != 0);

      //updateCollider();
      updateBehaviorStructures();
      updateResponseStructures();
      updateHandlerTable();
      
      myMaxpen = 0;
      // start of handlers added by this manager and all sub MechModels
      int hidx0 = handlers.size(); 

      ArrayList<MechModel> subMechs = new ArrayList<MechModel>();
      subMechs.add (myMechModel);
      for (MechSystemModel m : myMechModel.getLocalModels()) {
         if (m instanceof MechModel) {
            MechModel mech = (MechModel)m;
            double pen = mech.getCollisionManager().updateConstraints (
               handlers, t, flags);
            if (pen > myMaxpen) {
               myMaxpen = pen;
            }
            subMechs.add (mech);
         }
      }

      // start of handlers added by this manager only
      int hidx1 = handlers.size(); 
      myHandlerTable.setHandlerActivity (false);

      // compute explicit collisions
      for (Map.Entry<CollidablePair,CollisionBehavior> e :
              myExplicitBehaviors.entrySet()) {
         CollisionBehavior behav = e.getValue();
         if (behav.isEnabled()) {
            CollidablePair pair = e.getKey();
            CollidableBody c0 = (CollidableBody)pair.myComp0;
            CollidableBody c1 = (CollidableBody)pair.myComp1;
            checkForContact (c0, c1, behav, BehaviorSource.EXPLICIT, testMode); 
         }
      }
      // compute implicit collisions

      checkExternalCollisions (myRigidExts, testMode);
      checkExternalCollisions (myDeformableExts, myRigidExts, testMode);
      checkExternalCollisions (myDeformableExts, testMode);
      checkInternalCollisions (myDeformableInts, testMode);

      for (int i=0; i<subMechs.size(); i++) {
         CollisionManager cmi = null;
         cmi = subMechs.get(i).getCollisionManager();
         for (int j=i+1; j<subMechs.size(); j++) {
            CollisionManager cmj = null;
            cmj = subMechs.get(j).getCollisionManager();
            
            checkExternalCollisions (
               cmi.myRigidExts, cmj.myRigidExts, testMode);
            checkExternalCollisions (
               cmi.myRigidExts, cmj.myDeformableExts, testMode);
            checkExternalCollisions (
               cmi.myDeformableExts, cmj.myRigidExts, testMode);
            checkExternalCollisions (
               cmi.myDeformableExts, cmj.myDeformableExts, testMode);
         }
      }

      myHandlerTable.removeInactiveHandlers();
      myHandlerTable.collectHandlers (handlers);

      // for handlers just added by this manager, reduce constraints
      // constraints if necessary and remove all inactive contacts
      ArrayList<ContactConstraint> reducedBilaterals =
         new ArrayList<ContactConstraint>();
      for (int i=hidx1; i<handlers.size(); i++) {
         CollisionHandler handler = handlers.get(i);
         if (handler.getBehavior().getReduceConstraints()) {
            handler.getBilateralConstraints (reducedBilaterals);
         }
      }
      if (reducedBilaterals.size() > 0) {
         reduceBilateralConstraints (reducedBilaterals);
      }
      for (int i=hidx1; i<handlers.size(); i++) {      
         handlers.get(i).removeInactiveContacts();
      }
      
      // Now update collision responses, if any. 
      if (myResponses.size() > 0) {
         for (CollisionResponse resp : myResponses) {
            resp.clearHandlers();
         }
         // check possible references among all handlers added by this manager
         // as well as sub MechModels
         for (int i=hidx0; i<handlers.size(); i++) {
            CollisionHandler ch = handlers.get(i);
            // if a handler was referenced by a collision response, we
            // need to store its contact info 
            if (updateResponses (ch) > 0) {
               ch.myStateNeedsContactInfo = true;
            }
         }
      }

      return myMaxpen;
   }   

   public void getBilateralSizes (VectorNi sizes) {
      for (int i=0; i<myHandlers.size(); i++) {
         myHandlers.get(i).getBilateralSizes (sizes);
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      for (int i=0; i<myHandlers.size(); i++) {
         numb = myHandlers.get(i).addBilateralConstraints (
            GT, dg, numb);
      }
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
      for (int i=0; i<myHandlers.size(); i++) {
         idx = myHandlers.get(i).getBilateralInfo (ginfo, idx);
      }
      return idx;
   }

   public int setBilateralForces (VectorNd lam, double s, int idx) {
      for (int i=0; i<myHandlers.size(); i++) {
         idx = myHandlers.get(i).setBilateralForces (lam, s, idx);
      }
      return idx;
   }

   public int getBilateralForces (VectorNd lam, int idx) {
      for (int i=0; i<myHandlers.size(); i++) {
         idx = myHandlers.get(i).getBilateralForces (lam, idx);
      }
      return idx;
   }
   
   public void zeroForces() {
      for (int i=0; i<myHandlers.size(); i++) {
         myHandlers.get(i).zeroForces();
      }
   }

   public void getUnilateralSizes (VectorNi sizes) {
      //System.out.println ("unilateral sizes");
      for (int i=0; i<myHandlers.size(); i++) {
         myHandlers.get(i).getUnilateralSizes (sizes);
//         CollisionHandler ch = myHandlers.get(i);
//         int numu = ch.numUnilateralConstraints();
//         if (numu > 0) {
//            System.out.println (" "+numu+" "+ch.getCollidablePair());
//         }
      }
   }

   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {
      for (int i=0; i<myHandlers.size(); i++) {
         numu = myHandlers.get(i).addUnilateralConstraints (
            NT, dn, numu);
      }
      return numu;
   }

   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {
      for (int i=0; i<myHandlers.size(); i++) {
         idx = myHandlers.get(i).getUnilateralInfo (ninfo, idx);
      }
      return idx;
   }

   public int setUnilateralForces (VectorNd the, double s, int idx) {
      for (int i=0; i<myHandlers.size(); i++) {
         idx = myHandlers.get(i).setUnilateralForces (the, s, idx);
      }
      return idx;
   }

   public int getUnilateralForces (VectorNd the, int idx) {
      for (int i=0; i<myHandlers.size(); i++) {
         idx = myHandlers.get(i).getUnilateralForces (the, idx);
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      int max = 0;
      for (int i=0; i<myHandlers.size(); i++) {      
         max += myHandlers.get(i).maxFrictionConstraintSets();
      }
      return max;
   }
   
   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {
      for (int i=0; i<myHandlers.size(); i++) {
         numf = myHandlers.get(i).addFrictionConstraints (DT, finfo, numf);
      }
      return numf;
   }

   public void getConstrainedComponents (List<DynamicComponent> list) {
      // STUB - not currently used
   }


   // ===== BEGIN query of bilateral use =====
   //
   // The next set of methods are used to query whether or not this collision
   // manager may use bilateral constraints. It's complicated because we need
   // to make the same collidable peir queries that we use during regular
   // collision detection.

   /**
    * Queries whether or not this collision manager may use bilateral
    * constraints given its current collision configuration.
    *
    * @return {@code true} if this collision manager may use bilateral
    * constraints
    */
   public boolean usesBilateralConstraints() {
      if (myUsesBilateralConstraints == -1) {
         myUsesBilateralConstraints = (usesBilaterals() ? 1 : 0);
      }
      return myUsesBilateralConstraints != 0 ? true : false;
   }

   // check bilateral use among externally collidable pairs
   private boolean usesBilateralsExt (
      ArrayList<CollidableBody> cols0, ArrayList<CollidableBody> cols1) {
      for (CollidableBody c0 : cols0) {
         for (CollidableBody c1 : cols1) {
            if (getExplicitBehavior (c0, c1) == null) {
               CollisionBehavior behav = getExternalBehavior (c0, c1);
               if (behav.isEnabled()) {
                  if (CollisionHandler.usesBilateralConstraints (c0, c1, behav)) {
                     return true;
                  }

               }
            }               
         }
      }
      return false;
   }

   // check bilateral use among internally collidable pairs
   private boolean usesBilateralsInt (ArrayList<CollidableBody> cols) {
      for (CollidableBody c0 : cols) {
         for (CollidableBody c1 : cols) {
            if (getExplicitBehavior (c0, c1) == null) {
               Collidable ancestor = nearestCommonCollidableAncestor (c0, c1);
               if (ancestor != null) {
                  CollisionBehavior behav = getInternalBehavior (ancestor);
                  if (behav.isEnabled()) {
                     if (CollisionHandler.usesBilateralConstraints (
                            c0, c1, behav)) {
                        return true;
                     }
                  }
               }
            }               
         }
      }
      return false;
   }   

   // check bilateral use among all collidables under this manager
   private boolean usesBilaterals() {
      updateBehaviorStructures();
      updateResponseStructures();
      updateHandlerTable();

      // check sub mech models first
      ArrayList<MechModel> subMechs = new ArrayList<MechModel>();
      subMechs.add (myMechModel);
      for (MechSystemModel m : myMechModel.getLocalModels()) {
         if (m instanceof MechModel) {
            MechModel mech = (MechModel)m;
            if (mech.getCollisionManager().usesBilaterals()) {
               return true;
            }
            subMechs.add (mech);
         }
      }     

      // query explicit collisions
      for (Map.Entry<CollidablePair,CollisionBehavior> e :
              myExplicitBehaviors.entrySet()) {
         CollisionBehavior behav = e.getValue();
         if (behav.isEnabled()) {
            CollidablePair pair = e.getKey();
            CollidableBody c0 = (CollidableBody)pair.myComp0;
            CollidableBody c1 = (CollidableBody)pair.myComp1;
            if (CollisionHandler.usesBilateralConstraints (c0, c1, behav)) {
               return true;
            }
         }
      }
      // query implicit collisions
      if (usesBilateralsExt (myRigidExts, myRigidExts) ||
          usesBilateralsExt (myDeformableExts, myRigidExts) || 
          usesBilateralsExt (myDeformableExts, myDeformableExts) ||
          usesBilateralsInt (myDeformableInts)) {
         return true;
      }

      for (int i=0; i<subMechs.size(); i++) {
         CollisionManager cmi = null;
         cmi = subMechs.get(i).getCollisionManager();
         for (int j=i+1; j<subMechs.size(); j++) {
            CollisionManager cmj = null;
            cmj = subMechs.get(j).getCollisionManager();
            
            if (usesBilateralsExt (cmi.myRigidExts, cmj.myRigidExts) ||
                usesBilateralsExt (cmi.myRigidExts, cmj.myDeformableExts) ||
                usesBilateralsExt (cmi.myDeformableExts, cmj.myRigidExts) ||
                usesBilateralsExt (cmi.myDeformableExts, cmj.myDeformableExts)){
               return true;
            }
         }
      }    
      return false;
   }

   // ===== END query of bilateral use =====
   
   // ===== RenderableComponent methods ======

   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createRenderProps (this);
      props.setVisible (false);
      return props;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (CollisionHandler ch : myHandlers) {
         ch.updateBounds (pmin, pmax);
      }
   }

   private boolean isVisible (CollisionHandler ch) {
      if (!myRenderProps.isVisible()) {
         /// this collision manager must be visible
         return false;
      }
      RenderProps hprops = ch.getRenderProps();
      return (hprops==null || hprops.isVisible());
   }

   public void prerender (RenderList list) {
      for (CollisionHandler ch : myHandlers) {
         list.addIfVisible (ch);
      }     
   }

   public void render (Renderer renderer, int flags) {
   }

   // ***** Begin HasNumericState *****

   public void advanceState (double t0, double t1) {
      // contact constraints don't need to advance their aux state
   }

   public void getState (DataBuffer data) {
      updateHandlerTable();
      myHandlerTable.getState (data);
   }

   public void setState (DataBuffer data) {
      updateBehaviorStructures();
      if (!myHandlerTableValid) {
         MechModel topMech = MechModel.topMechModel(this);
         topMech.updateCollidableBodyIndices();
         ArrayList<CollidableBody> bodies = myMechModel.getCollidableBodies();
         // initialize, but *without* handlers, since these should be added
         // by setState. However, if the table was not valid, all states
         // except the zero state should have also been invalidated, and the
         // zero state has no handlers and hence and empty table
         myHandlerTable.initialize (bodies);     
         myHandlerTableValid = true;
      }
      else {
         myHandlerTable.removeAllHandlers();
      }
      myHandlerTable.setState (data);
      if (myMechModel == MechModel.topMechModel(this)) {
         myHandlers.clear();
         collectHandlers (myHandlers);
      }
   }

   /* TODO:

DONE  prevent multiple behaviors and response object from being added

DONE  update renderer to use only contactInfo

DONE  implement aux state for ContactInfo

DONE  fix drawPenetrationDepth to use the right mesh for the behavior

DONE  check collision stuff in the uiguide

DONE  check implement scan/write

DONE  check component removal

DONE  check save/restore aux state

DONE  fix GUI for collision control

OK    does initialize have to call down to the other collision managers?

DONE  Fix autoComputeCompliance(): who should call this?

DONE  Fix CollisionHandler.doBodyFaceContact

DONE  Fix CollisionHandler.useSignedDistanceCollider

DONE  test new drawing

DONE  add drawContactNormals to documentation example

DONE  document drawing collision penetration

DONE  create example for drawing collision penetration

DONE  check usage of MechModel.getCollisionBehavior();

DONE  add documentation for nested MechModels

DONE  add documentation for new CollisionResponse

DONE  proofread API documentation.
      
DONE  proofread document

DONE  Fix setting of default properties like rigidRegionTol      

DONE  compile documentation and javadocs

DONE  prepare update log

DONE  add save/load for CollisionResponse

DONE  Replace CollisionComponent with CollisonComponentX

DONE  Decide on isEmpty() for collision response and update docs

LATER should behavior and response objects have to make their first collidable
      specific?

DONE  describe the methods that should be used to determine corresponding
      collidables in CollisionHandler.

DONE  figure out get0() vs. get(0) for CollidablePair

DONE  Fix use of lowDOF in CollisionHandler

DONE  run all tests, with testSaveAndRestoreState enabled

OK    saveLoad
      contactTest
      mechModelTest
      unit tests
      
DONE  change penetration render example to disable collison handler

DONE  document the disabling of collisions
            
DONE  add penetration region argument for getForceBehavior
      
DONE  check for changed properties and "penetrationTol"

      install documentation and javadocs

      remove Collision*Old, CollisionHandlerList

DONE  getCollisionBehavior() has been replaced with getActingCollisionBehavior(),
      so check code for this

DONE  check for solo calls to cm.setContactNormalLen()

DONE  check for old properties:
         collisionCompliance, collisionDamping, collisionPointTol,
         collisionRegionTol, penetrationTol, 

DONE  setDrawIntersectionContours() - oralCavityTongue

DONE  CollisionManager.setForceBehavior (a, b, fb); - move to CollisionBehavior

DONE  getCollisionHandler() and getContactImpulses() replaced with response API
      collisionHandlers() no longer publically accessible

NO    separate default behaviors from supplied ones?

DONE  make sure that all the collision pair possibilities make sense.

DONE  have setCollisionBehavior return the created handler

DONE  change over - replace CollisionManager with CollisionMangerNew

DONE  fix setting default collision behaviors

DONE  Fix MechModel.getCollisionBehavior() - should it return the pair, or
      should it be getActingCollision() behavior?

DONE  implement collison response API

DONE  add drawContactNormals

DONE  Get rid of CollisionManagerNew.RIGID_RIGID, etc. ?

DONE  Change signature of setDefaultCollisionBehavior to use Collidable.Group?

DONE  implement set/get state

DONE  implement scan/write

DONE  document getActingCollisonBehavior()

DONE  build and add ScalarRange object

DONE  if behaviors are going to be visible in NavPanel, give them good names

DONE  document CollisionResponse

DONE  change CollsionResponse.getCollisionHandlers() to getHandlers()?

NO    Maybe make behavior a composite property?

DONE  access to behavior should defer to appropriate MechModel

DONE  check method completeness

DONE  new collision handler

DONE  add collision handler table

DONE  make CollisionManagerNew implement Collidable

DONE  make sure handlers are collected for ALL MechModel levels

DONE  add hasSubCollidables to Collidable


DONE  add properties to CollisionManagerNew

DONE  change isDrawIntersectionXXX to getDrawIntersectionXXX

OK    collider was removed from CollisionHandler, but that means we need
      CollisionManager when calling updateConstraints(). How to handle this? Do
      we want to move constraint management back into CollisionManager?

---------------------------------------------------------------------------

Adding SignedDistanceCollider

DONE  Make sure that normal and position in the SD cpp are in world coordinates

DONE  Modify collision handling for SDC so that ContactPoints on the
      rigid body don't need vertices      

DONE  Make sure that SDC implies VERTEX_PENETRATION

DONE  add ability to restrict region calculations in getContacts()

      add ability to specify signed distance grid size to mesh

    */

}
