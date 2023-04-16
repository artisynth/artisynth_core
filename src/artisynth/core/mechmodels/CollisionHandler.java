/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionBehavior.VertexPenetrations;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.CollisionManager.BehaviorSource;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.materials.ContactForceBehavior;
import artisynth.core.mechmodels.Collidable.Group;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.modelbase.ContactPoint;
import artisynth.core.util.ScalarRange;
import maspack.collision.ContactInfo;
import maspack.collision.ContactPlane;
import maspack.collision.EdgeEdgeContact;
import maspack.collision.IntersectionContour;
import maspack.collision.IntersectionPoint;
import maspack.collision.PenetratingPoint;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.HalfEdge;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector2d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.properties.PropertyList;
import maspack.render.HasRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.solvers.LCPSolver;
import maspack.spatialmotion.FrictionInfo;
import maspack.util.DataBuffer;
import maspack.util.DoubleInterval;
import maspack.util.InternalErrorException;

/**
 * Class that generates the contact constraints between a specific
 * pair of collidable bodies. CollisionHandlers are created on-demand
 * by the CollisionManager whenever two bodies are found to collide.
 */
public class CollisionHandler extends ConstrainerBase 
   implements HasRenderProps, Renderable {

   //public static boolean useSignedDistanceCollider = false;
   public static boolean computeTimings = false;
   // this doesn't quite work yet - problems with save/load state:
   // for debugging and testing only!:
   public static boolean preventBilateralSeparation = false;
   
   // structural information

   CollisionManager myManager;
   CollidableBody myCollidable0;
   CollidableBody myCollidable1;
   CollisionHandler myNext; // horizontal link in HandlerTable
   CollisionHandler myDown; // vertical link in HandlerTable

   // collision behavior
   CollisionBehavior myBehavior;
   BehaviorSource myBehaviorSource;
   
   double myCompliance = 0;  // keep local copies of compliance and damping,
   double myDamping = 0;     // since these may be computed automatically

   // collision response

   LinkedHashMap<ContactPoint,ContactConstraint> myBilaterals;
   ArrayList<ContactData> myLastBilateralData;
   // list of bilaterals arranged in order, with those for which cpnt0 is on
   // collidable0 first and those with cpnt0 on collidable1 second. This
   // is done simply to maintain exact numeric compatibility with some
   // legacy tests.
   ArrayList<ContactConstraint> myOrderedBilaterals;
   LinkedHashMap<ContactPoint,ContactConstraint> myUnilaterals;
   ArrayList<ContactData> myLastUnilateralData;
   int myMaxUnilaterals = 100;
   ContactInfo myContactInfo; // most recent contact info for this handler
   ContactInfo myLastContactInfo; // previous contact info for this handler
   boolean myStateNeedsContactInfo = false;
   
   // Set of vertices on collidable0 which are attached to colliable1
   HashSet<Vertex3d> myAttachedVertices0 = null;
   // Set of vertices on collidable1 which are attached to colliable0
   HashSet<Vertex3d> myAttachedVertices1 = null;
   boolean myAttachedVerticesValid = false;

   // rendering
   
   CollisionRenderer myRenderer;
   // for rendering, map from vertices to pressure or penetration depth values:
   HashMap<Vertex3d,Double> myColorMapVertexValues;
   HashSet<Face> myColorMapFaces; // faces to be used for color map rendering

   // misc

   public static PropertyList myProps =
      new PropertyList (CollisionHandler.class, ConstrainerBase.class);

   static private RenderProps defaultRenderProps = new RenderProps();

   static {
      myProps.add (
         "renderProps * *", "render properties for this collision handler",
         defaultRenderProps);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public RenderProps getRenderProps() {
      RenderProps props = null;
      if (myBehavior != null) {
         props = myBehavior.getRenderProps();
      }
      if (props == null && myManager != null) {
         props = myManager.getRenderProps();
      }
      return props;
   }

   double getContactNormalLen() {
      double len = 0;
      if (myManager != null) {
         len = myManager.getContactNormalLen();
         if (myBehavior.getRenderingCollidable() == 1) {
            len = -len;
         }
      }
      return len;
   }

   double getContactForceLenScale() {
      double scale = 0;
      if (myManager != null && myBehavior.getDrawContactForces()) {
         scale = myManager.getContactForceLenScale();
         if (myBehavior.getRenderingCollidable() == 1) {
            scale = -scale;
         }
      }
      return scale;
   }

   double getFrictionForceLenScale() {
      double scale = 0;
      if (myManager != null && myBehavior.getDrawFrictionForces()) {
         scale = myManager.getContactForceLenScale();
         if (myBehavior.getRenderingCollidable() == 1) {
            scale = -scale;
         }
      }
      return scale;
   }

   public ContactInfo getLastContactInfo() {
      return myLastContactInfo;
   }

   protected static boolean isRigid (CollidableBody col) {
      return (col instanceof RigidBody || col instanceof RigidMeshComp);
   }

   public CollisionHandler (CollisionManager manager) {
      myBilaterals = new LinkedHashMap<ContactPoint,ContactConstraint>();
      myLastBilateralData = new ArrayList<>();
      // HHH myUnilaterals = new ArrayList<ContactConstraint>();
      myUnilaterals = new LinkedHashMap<ContactPoint,ContactConstraint>();
      myLastUnilateralData = new ArrayList<>();
      //myCollider = SurfaceMeshCollider.newCollider();
      myManager = manager;
   }

   public CollisionHandler (
      CollisionManager manager,
      CollidableBody col0, CollidableBody col1, 
      CollisionBehavior behav, BehaviorSource src) {

      this (manager);
      set (col0, col1, behav, src);
   }
   
   private Method getMethod() {
      return getMethod (myCollidable0, myCollidable1, myBehavior);
   }      

   private static Method getMethod (
      CollidableBody col0, CollidableBody col1, CollisionBehavior behav) {      
      Method method = behav.getMethod();
      if (behav.getColliderType() == ColliderType.SIGNED_DISTANCE) {
         method = CollisionBehavior.Method.VERTEX_PENETRATION;
      }
      else if (method == Method.DEFAULT) {
         if (isRigid(col0) && isRigid(col1)) {
            method = CollisionBehavior.Method.CONTOUR_REGION;
         }
         else {
            method = CollisionBehavior.Method.VERTEX_PENETRATION;
         }
      }
      return method;
   }

   void set (
      CollidableBody col0, CollidableBody col1, 
      CollisionBehavior behav, BehaviorSource src) {
      myCollidable0 = col0;
      myCollidable1 = col1;
      myBehavior = behav;
      myBehaviorSource = src;
   }
   
   void setBehavior (CollisionBehavior behav, BehaviorSource src) {
      myBehavior = behav;
      myBehaviorSource = src;
   }

   CollisionBehavior getBehavior () {
      return myBehavior;
   }

   BehaviorSource getBehaviorSource () {
      return myBehaviorSource;
   }

   public static boolean attachedNearContact (
      ContactPoint cpnt, Collidable collidable, 
      Set<Vertex3d> attachedVertices) {
   
      // Basic idea:
      // for (each vertex v associated with cpnt) {
      //    if (all contact masters of v are attached to collidable1) 
      //       return true;
      //    }
      // }
      if (attachedVertices == null || attachedVertices.size() == 0) {
         return false;
      }
      Vertex3d[] vtxs = cpnt.getVertices();
      for (int i=0; i<vtxs.length; i++) {
         if (attachedVertices.contains (vtxs[i])) {
            return true;
         }
      }
      if (vtxs.length == 1) {
         // vertices associated with cpnt expanded to include surrounding
         // vertices as well.
         Iterator<HalfEdge> it = vtxs[0].getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            if (attachedVertices.contains (he.getHead())) {
               return true;
            }
         }
      }
      return false;
   }

   protected void putContact (
      HashMap<ContactPoint,ContactConstraint> contacts, 
      ContactConstraint cons) {
      if (cons.myIdentifyByPoint1) {
         contacts.put (cons.myCpnt1, cons);
      }
      else {
         contacts.put (cons.myCpnt0, cons);
      }
   }

   public ContactConstraint getContact (
      HashMap<ContactPoint,ContactConstraint> contacts,
      ContactPoint cpnt0, ContactPoint cpnt1, 
      boolean hashUsingFace, boolean pnt0OnCollidable1, double distance) {

      ContactConstraint cons = null;
      if (hashUsingFace && cpnt1.getVertices() != null) {
         // if hashUsingFace==true and cpnt1 actually has face vertices,
         // get the contact using those face vertices
         cons = contacts.get (cpnt1);
      }
      else {
         cons = contacts.get (cpnt0);
      }
      if (cons == null) {
         cons = new ContactConstraint (cpnt0, cpnt1, pnt0OnCollidable1);
         cons.myIdentifyByPoint1 = hashUsingFace;
         putContact (contacts, cons);
         return cons;
      }
      else { // contact already exists
         double lam = cons.getContactForce();
         //do not activate constraint if contact is trying to separate
         if (!preventBilateralSeparation && lam < 0) {
            return null;
         }
         else if (cons.isActive() && -cons.getDistance() >= distance) {
            // Not sure this happens any more - all contacts have been
            // deactivated before this method is called ...
            
            // if constraint exists and it has already been set with a distance
            // greater than the current one, don't return anything; leave the
            // current one alone. This is for cases where the same feature maps
            // onto more than one contact.
            return null;
         }
         else {
            // update contact points
            cons.setContactPoints (cpnt0, cpnt1, pnt0OnCollidable1);
            return cons;
         }
      }
   }

   /**
    * Called through the CollisionManager.
    */
   void saveLastConstraintData () {
      myLastBilateralData.clear();
      for (ContactConstraint cc : getOrderedBilaterals()) {
         myLastBilateralData.add (
            new ContactData(cc, /*bilateral=*/true)); 
      }
      myLastUnilateralData.clear();
      for (ContactConstraint cc : getUnilaterals()) {
         myLastUnilateralData.add (
            new ContactData(cc, /*bilateral=*/false)); 
      }
      myLastContactInfo = myContactInfo;
      myOrderedBilaterals = null; // will be rebuilt on demand
      myContactInfo = null;
   }

   public double computeCollisionConstraints (ContactInfo cinfo) {
      //clearRenderData();
      double maxpen = 0;
      if (cinfo != null) {
         switch (getMethod()) {
            case VERTEX_PENETRATION: 
            case VERTEX_PENETRATION_BILATERAL:
            case VERTEX_EDGE_PENETRATION: {
               maxpen = computeVertexPenetrationConstraints (cinfo);
               break;
            }
            case CONTOUR_REGION: {
               maxpen = computeContourRegionConstraints (cinfo);
               break;
            }
            case INACTIVE: {
               // do nothing
               maxpen = 0;
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented contact method: "+getMethod());
            }
         }
         myContactInfo = cinfo;
      }
      else {
         clearContactActivity();
         //removeInactiveContacts();
      }
      updateCompliance(myBehavior);
      return maxpen;
   }

   static boolean usesBilateralConstraints (
      CollidableBody col0, CollidableBody col1, CollisionBehavior behav) {

      switch (getMethod(col0, col1, behav)) {
         case VERTEX_PENETRATION: 
         case VERTEX_PENETRATION_BILATERAL:
         case VERTEX_EDGE_PENETRATION: {
            return true;
         }
         case CONTOUR_REGION:
         case INACTIVE: {
            return false;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented contact method: "+getMethod(col0, col1, behav));
         }        
      }
   }
   
   public CollidableBody getCollidable (int cidx) {
      if (cidx == 0) {
         return myCollidable0;
      }
      else if (cidx == 1) {
         return myCollidable1;
      }
      else {
         throw new IllegalArgumentException (
            "collidable index must be 0 or 1");
      }      
   }
   
   /**
    * Returns 0 if a specified collidable is associated with this handler's
    * first collidable body, 1 if it is associated with the second body, 
    * and -1 if it is associated with neither.
    * 
    * @param col collidable to inspect
    * @return index of the body associated with <code>col</code>. 
    */
   public int getBodyIndex (Collidable col) {
      if (myCollidable0 == col || 
          myCollidable0.getCollidableAncestor() == col) {
         return 0;         
      }
      else if (myCollidable1 == col || 
               myCollidable1.getCollidableAncestor() == col) {
         return 1;
      }
      else {
         return -1;
      }
   }

   public CollidableBody getOtherCollidable (CollidableBody cb) {
      if (cb == myCollidable0) {
         return myCollidable1;
      }
      else if (cb == myCollidable1) {
         return myCollidable0;
      }
      else {
         return null;
      }
   }

   public CollidablePair getCollidablePair() {
      return new CollidablePair (myCollidable0, myCollidable1);
   }

   /**
    * Returns next handler in a row of a CollisionHandlerTable.
    * 
    * @return next handler in the row
    */
   public CollisionHandler getNext() {
      return myNext;
   }
   
   /**
    * Sets next handler in a row of a CollisionHandlerTable.
    * 
    * @param next next handler to add to the row
    */
   public void setNext (CollisionHandler next) {
      myNext = next;
   }

   /**
    * Returns next handler in a column of a CollisionHandlerTable.
    * 
    * @return next handler in the column
    */
   public CollisionHandler getDown() {
      return myDown;
   }
   
   /**
    * Sets next handler in a column of a CollisionHandlerTable.
    * 
    * @param down next handler to add to the column
    */
   public void setDown (CollisionHandler down) {
      myDown = down;
   }

   /**
    * Makes this handler inactive by clearing all contact data.
    */
   void clearActivity() {
      clearContactData();
   }
   
   /**
    * Make active by setting an empty ContactInfo structure, if
    * necessary. Used for testing only.
    */
   void makeActive() {
      if (!isActive()) {
         myContactInfo = new ContactInfo(null,null);
      }
   }

   /**
    * Returns whether or not this handler is active. A handler is active if it
    * has any contact data, current or last. Inactive contact handlers are 
    * automatically removed from the ContactHandlerTable.
    */
   boolean isActive() {
      return (hasContactData() || hasLastContactData());
   }
   
   /**
    * Returns true if this handler has current contact data.
    */
   boolean hasContactData() {
      return (
         myContactInfo != null || 
         myBilaterals.size() > 0 || 
         myUnilaterals.size() > 0);
   }
   
   /**
    * Returns true if this handler has last contact data.
    */
   boolean hasLastContactData() {
      return (
         myLastContactInfo != null || 
         myLastBilateralData.size() > 0 || 
         myLastUnilateralData.size() > 0);
   }

   
   double setVertexFace (
      ContactConstraint cons, PenetratingPoint cpp,
      CollidableBody collidable0, CollidableBody collidable1) {

      // compute normal from the opposing face
      cpp.face.computeNormal (cons.myNormal);
      PolygonalMesh mesh = collidable1.getCollisionMesh();
      // convert to world coordinates if necessary
      if (!mesh.meshToWorldIsIdentity()) {
         cons.myNormal.transform (mesh.getMeshToWorld());
      }
      cons.myContactArea = cpp.getContactArea();
      cons.assignMasters (collidable0, collidable1);

      // This should be -cpp.distance - do we need to compute this?
      Vector3d disp = new Vector3d();
      disp.sub(cons.myCpnt0.getPosition(), cons.myCpnt1.getPosition());
      double dist = disp.dot(cons.myNormal);
      return dist;
   }

   double setVertexBody (
      ContactConstraint cons, PenetratingPoint cpp,
      CollidableBody collidable0, CollidableBody collidable1) {

      // get the normal directly from the penetrating point
      cons.myNormal.set (cpp.getNormal());
      cons.myContactArea = -1;      
      cons.assignMasters (collidable0, collidable1);

      // This should be -cpp.distance - do we need to compute this?
      Vector3d disp = new Vector3d();
      disp.sub(cons.myCpnt0.getPosition(), cons.myCpnt1.getPosition());
      double dist = disp.dot(cons.myNormal);

      return dist;
   }

   double setEdgeEdge (
      ContactConstraint cons, EdgeEdgeContact eecs,
      CollidableBody collidable0, CollidableBody collidable1) {

      cons.myNormal.negate (eecs.point1ToPoint0Normal);
      cons.assignMasters (collidable0, collidable1);
      cons.myContactArea = eecs.getContactArea();
      return -eecs.displacement;
   }

   boolean hashContactUsingFace (
      CollidableBody collidable0, CollidableBody collidable1) {
      // for vertex penetrating constraints, if collidable0 has low DOF and its
      // mesh has more vertices than that of collidable1, then we hash the
      // contact using the face on collidable1 instead of the vertex on
      // collidable0 since that is more likely to persist.
      PolygonalMesh mesh0 = collidable0.getCollisionMesh();
      PolygonalMesh mesh1 = collidable1.getCollisionMesh();
      return (!myBehavior.isCompliant() && hasLowDOF (collidable0) &&
              mesh0.numVertices() > mesh1.numVertices());
   }

   protected boolean isCompletelyAttached (
      DynamicComponent comp,
      CollidableBody collidable0, CollidableBody collidable1) {
      DynamicAttachment attachment = comp.getAttachment();
      if (attachment == null) {
         return false;
      }
      else {
         DynamicComponent[] attachMasters = attachment.getMasters();
         for (int k=0; k<attachMasters.length; k++) {
            CollidableDynamicComponent mcomp = null;
            if (attachMasters[k] instanceof CollidableDynamicComponent) {
               mcomp = (CollidableDynamicComponent)attachMasters[k];
            }
            if (collidable0.containsContactMaster (mcomp)) {
               // ignore
               continue;
            }
            if (mcomp == null || !collidable1.containsContactMaster (mcomp)) {
               return false;
            }
         }
      }
      return true;
   }
   
   protected boolean isContainedIn (
      DynamicComponent comp, CollidableBody collidable) {
      if (comp instanceof CollidableDynamicComponent) {
         return collidable.containsContactMaster (
            (CollidableDynamicComponent)comp);
      }
      else {
         return false;
      }
   }
   
   protected boolean vertexAttachedToCollidable (
      Vertex3d vtx, CollidableBody collidable0, CollidableBody collidable1) {
      ArrayList<ContactMaster> masters = new ArrayList<ContactMaster>();
      collidable0.collectVertexMasters (masters, vtx);
      // vertex is considered attached if 
      // (a) all master components are completely attached to collidable1, or
      // (b) all master components are actually contained in collidable1
      HashSet<DynamicComponent> dcomps = new HashSet<>();
      for (ContactMaster cm : masters) {
         cm.collectMasterComponents (dcomps, /*activeOnly=*/false);
      }
      for (DynamicComponent comp : dcomps) {
         if (!isCompletelyAttached (comp, collidable0, collidable1) &&
             !isContainedIn (comp, collidable1)) {
            return false;
         }
      }
      return true;
   }

   protected HashSet<Vertex3d> computeAttachedVertices (
      CollidableBody collidable0, CollidableBody collidable1) {

      if (isRigid (collidable0)) {
         return null;
      }
      PolygonalMesh mesh = collidable0.getCollisionMesh();
      HashSet<Vertex3d> attached = new HashSet<Vertex3d>();
      for (Vertex3d vtx : mesh.getVertices()) {
         if (vertexAttachedToCollidable (vtx, collidable0, collidable1)) {
            attached.add (vtx);
         }
      }
      return attached.size() > 0 ? attached : null;
   }

   protected void updateAttachedVertices() {
      if (!myAttachedVerticesValid) {
         myAttachedVertices0 =
            computeAttachedVertices (myCollidable0, myCollidable1);
         myAttachedVertices1 =
            computeAttachedVertices (myCollidable1, myCollidable0);
         myAttachedVerticesValid = true;
      }
   }

   double computeEdgePenetrationConstraints (
      ArrayList<EdgeEdgeContact> eecs, Collidable collidable1) {

      double maxpen = 0;

      if (eecs != null) {
         for (EdgeEdgeContact eec : eecs) {
            // Check if the contact has already been corrected by other contact
            // corrections.
            //if (eec.calculate()) {
            ContactPoint pnt0, pnt1;
            pnt0 = new ContactPoint (eec.point0, eec.edge0, eec.s0);
            if (collidable1 instanceof RigidBody) {
               pnt1 = new ContactPoint (eec.point1);
            }
            else {
               pnt1 = new ContactPoint (eec.point1, eec.edge1, eec.s1);
            }
            boolean pnt0OnCollidable1 = false;
   
            ContactConstraint cons;
            if (myBehavior.getBilateralVertexContact()) {
               cons = getContact (
                  myBilaterals, pnt0, pnt1, 
                  false, pnt0OnCollidable1, eec.displacement);
            }
            else {
               cons = getContact (
                  myUnilaterals, pnt0, pnt1, 
                  false, pnt0OnCollidable1, eec.displacement); 
            }
            // As long as the constraint exists and is not already marked 
            // as active, then we add it
            if (cons != null) {
               cons.setActive (true);
   
               double dist = setEdgeEdge (
                  cons, eec, myCollidable0, myCollidable1);
               if (!cons.isControllable()) {
                  cons.setActive (false);
                  continue;
               } 
               cons.setDistance (dist);
               if (-dist > maxpen) {
                  maxpen = -dist;
               }               
            }
            //}
         }
      }
      return maxpen;
   }

   double computeVertexPenetrationConstraints (
      ArrayList<PenetratingPoint> points,
      CollidableBody collidable0, CollidableBody collidable1) {
      
      double maxpen = 0;
      boolean hashUsingFace = hashContactUsingFace (collidable0, collidable1);

      updateAttachedVertices();

      for (PenetratingPoint cpp : points) {
         ContactPoint pnt0, pnt1;
         pnt0 = new ContactPoint (cpp.vertex);
         if (cpp.face == null) { // || (collidable1 instanceof RigidBody)) {
            pnt1 = new ContactPoint (cpp.position);
         }
         else {
            pnt1 = new ContactPoint (cpp.position, cpp.face, cpp.coords);
         }
         
         HashSet<Vertex3d> attachedVtxs0 = myAttachedVertices0;
         HashSet<Vertex3d> attachedVtxs1 = myAttachedVertices1;
         if (collidable0 == myCollidable1) {
            // flip attached vertex lists
            attachedVtxs0 = myAttachedVertices1;
            attachedVtxs1 = myAttachedVertices0;
         }

         if (!collidable0.allowCollision (
                pnt0, collidable1, attachedVtxs0) ||
             !collidable1.allowCollision (
                pnt1, collidable0, attachedVtxs1)) {
            continue;
         }

         ContactConstraint cons;
         if (myBehavior.getBilateralVertexContact()) {
            cons = getContact (
               myBilaterals, pnt0, pnt1, 
               hashUsingFace, collidable0==myCollidable1, cpp.distance);
         }
         else {
            cons = getContact (
               myUnilaterals, pnt0, pnt1, 
               hashUsingFace, collidable0==myCollidable1, cpp.distance);
         }
         
         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null) {
            cons.setActive (true);
            double dist;
            if (cpp.face == null) { // || (collidable1 instanceof RigidBody)) {
               dist = setVertexBody (cons, cpp, collidable0, collidable1);
            }
            else {
               dist = setVertexFace (cons, cpp, collidable0, collidable1);
            }
            if (!cons.isControllable()) {
               cons.setActive (false);
               continue;
            }
            cons.setDistance (dist);
            // activateContact (cons, dist, data);
            if (-dist > maxpen) {
               maxpen = -dist;
            }
         }
      }
      //System.out.println ("");
      return maxpen;
   }

   /**
      multiple constraint handling
      mark all existing constraints inactive
      for (each contact c) {
         if (c matches an existing constraint con) {
            if (con is not trying to separate) {
               if (con is active) {
                  if (penetration is greater) {
                     return con;
                  }
                  else {
                     return null;
                  }
               }
               else {
                  return con;
               }
            }
         }
   */

   static boolean hasLowDOF (CollidableBody collidable) {
      // XXX should formalize this better
      return isRigid (collidable);
   }

   private boolean usingTwoWayContact() {
      switch (myBehavior.getVertexPenetrations()) {
         case BOTH_COLLIDABLES: {
            return true;
         }
         case FIRST_COLLIDABLE:
         case SECOND_COLLIDABLE: {
            return false;
         }
         case AUTO: {
            return (!hasLowDOF (myCollidable0) && !hasLowDOF (myCollidable1));
         }
         default:{
            throw new InternalErrorException (
               "Unimplemented TwoWayContact mode :" +
               myBehavior.getVertexPenetrations());
         }
      }
   }
   
   double computeVertexPenetrationConstraints (ContactInfo info) {
      double maxpen = 0;
      clearContactActivity();
      
      if (info != null) {
         CollidableBody col0 = myCollidable0;
         CollidableBody col1 = myCollidable1;
         ArrayList<PenetratingPoint> pnts0 = info.getPenetratingPoints(0);
         ArrayList<PenetratingPoint> pnts1 = info.getPenetratingPoints(1);
          if (!usingTwoWayContact()) {
             // see if we need to swap the body for which vertex penetrations 
             // should be computed
             VertexPenetrations vpen = myBehavior.getVertexPenetrations();
             if (((vpen == VertexPenetrations.SECOND_COLLIDABLE) ||
                  (vpen == VertexPenetrations.AUTO &&
                   isRigid(myCollidable0) && !isRigid(myCollidable1)))) {
                col0 = myCollidable1;
                col1 = myCollidable0;
                pnts0 = info.getPenetratingPoints(1);
                pnts1 = info.getPenetratingPoints(0);              
             }                
          }
         maxpen = computeVertexPenetrationConstraints (pnts0,col0,col1);
         if (usingTwoWayContact()) {
            double pen = computeVertexPenetrationConstraints (pnts1,col1,col0);
            if (pen > maxpen) {
               maxpen = pen;
            }
         }
         if (getMethod() == CollisionBehavior.Method.VERTEX_EDGE_PENETRATION) {
            double pen = computeEdgePenetrationConstraints (
               info.getEdgeEdgeContacts(), col1);
            if (pen > maxpen) {
               maxpen = pen;
            }
         }
      }
      //removeInactiveContacts();
      //printContacts ("%g");
      return maxpen;
   }

   double computeContourRegionConstraints (ContactInfo info) {
      
      double maxpen = 0;

      ArrayList<ContactConstraint> prevUnilaterals = new ArrayList<>();
      prevUnilaterals.addAll (myUnilaterals.values());
      myUnilaterals.clear();
      
      if (info != null) {
         int numc = 0;
         info.setPointTol (myBehavior.myRigidPointTol);
         info.setContactPlaneTol (myBehavior.myRigidRegionTol);
         for (ContactPlane region : info.getContactPlanes()) {
            for (Point3d p : region.points) {
               if (numc >= myMaxUnilaterals)
                  break;

               ContactConstraint c = new ContactConstraint();

               c.setContactPoint0 (p, /*pnt0OnCollidable1=*/false);
               c.equateContactPoints();
               c.setNormal (region.normal);
               c.assignMasters (myCollidable0, myCollidable1);
               c.myContactArea = region.contactAreaPerPoint;

               maxpen = region.depth;
               c.setDistance (-region.depth);
               putContact (myUnilaterals, c);
               c.setActive (true);
               numc++;
            }
         }
         if (numc > 0) {
            estimateStateFromPreviousContacts(prevUnilaterals);
         }
      }
      return maxpen;
   }

   private void estimateStateFromPreviousContacts (
      ArrayList<ContactConstraint> prevUnilaterals) {
      // very simple algorithm: for each previous contact whose contact state
      // is basic, find the nearest new contact and set its state and friction
      // state to that of the the previous contact. Other new states will not
      // be set.      
      for (ContactConstraint prev : prevUnilaterals) {
         if (prev.myState == LCPSolver.Z_VAR) {
            Point3d ppnt = prev.myCpnt0.getPosition();
            ContactConstraint nearc = null;
            double mindist = Double.MAX_VALUE;
            // search using brute force since using KDTrees or other
            // accelerating structures is slower unless we have many points
            // (like > 1000).
            for (ContactConstraint c : getUnilaterals()) {
               Point3d cpnt = c.myCpnt0.getPosition();
               double dist = ppnt.distance (cpnt);
               if (dist < mindist) {
                  mindist = dist;
                  nearc = c;
               }
            }
            nearc.myState = prev.myState;
            nearc.myLambda = prev.myLambda;
            nearc.myFrictionState0 = prev.myFrictionState0;
            nearc.myFrictionState1 = prev.myFrictionState1;
            if (prev.myHasFrictionDir) {
               nearc.myFrictionDir0.set (prev.myFrictionDir0);
               nearc.myPhi0 = prev.myPhi0;
               nearc.myPhi1 = prev.myPhi1;
               nearc.myHasFrictionDir = true;
            }
            else {
               nearc.myHasFrictionDir = false;
            }
         }
      }
   }

   void clearContactData() {
      myContactInfo = null;
      myLastContactInfo = null;
      myBilaterals.clear();
      myUnilaterals.clear();
      myLastBilateralData.clear();
      myLastUnilateralData.clear();
   }

   public void clearContactActivity() {
      for (ContactConstraint c : getBilaterals()) {
         c.setActive (false);
         c.setDistance (0);
      }
      for (ContactConstraint c : myUnilaterals.values()) {
         c.setActive (false);
         c.setDistance (0);
      }
   }

   public void removeInactiveContacts() {
      if (myContactInfo != null) {
         Iterator<ContactConstraint> it;
         it = getBilaterals().iterator();
         while (it.hasNext()) {
            ContactConstraint c = it.next();
            if (!c.isActive()) {
               it.remove();
            }
         }
         it = myUnilaterals.values().iterator();
         while (it.hasNext()) {
            ContactConstraint c = it.next();
            if (!c.isActive()) {
               it.remove();
               //mycontactschanged = true;
            }
         }
      }
      else {
         myBilaterals.clear();
         myUnilaterals.clear();
      }
   }

   private void printContacts(String fmtStr) {
      Iterator<ContactConstraint> it;
      it = getBilaterals().iterator();
      while (it.hasNext()) {
         ContactConstraint c = it.next();
         System.out.println (" " + c.toString(fmtStr));
      }
   }

   private void updateCompliance (CollisionBehavior behav) {
      if (behav.getCompliance() != 0) {
         myCompliance = behav.getCompliance();
         myDamping = behav.getDamping();
      }
      else if (behav.getAcceleration() != 0) {
         // auto compute compliance based on accleration and penetration tol
         double mass = myCollidable0.getMass() + myCollidable1.getMass();
         double dampingRatio = 1;
         double tol = behav.getPenetrationTol();
         myCompliance = tol/(behav.getAcceleration()*mass);
         myDamping = dampingRatio*2*Math.sqrt(mass/myCompliance);
      }
      else {
         myCompliance = 0;
         myDamping = 0;
      }
   }
   // begin constrainer implementation

   public double updateConstraints (double t, int flags) {
      // STUB - not used. 
      return 0;
   }

   private void getConstraintComponents (
      HashSet<DynamicComponent> set, Collection<ContactConstraint> contacts) {
      for (ContactConstraint cc : contacts) {
         cc.collectMasterComponents (set, /*activeOnly=*/false);
      }
   }
   
   public void getConstrainedComponents (List<DynamicComponent> list) {
      HashSet<DynamicComponent> set = new HashSet<DynamicComponent>();
      getConstrainedComponents (set);
      list.addAll (set);
   }
   
   /**
    * Return the bilaterals in an order such that those whose vertices
    * are associated with collidable0 are returned first, and those
    * associated with collidable1 second. This is done to maintain
    * numeric compatibility with legacy simulation results.
    */
   Collection<ContactConstraint> getOrderedBilaterals() {
      if (myOrderedBilaterals == null) {
         ArrayList<ContactConstraint> list = new ArrayList<>();
         for (ContactConstraint cc : getBilaterals()) {
            if (!cc.myPnt0OnCollidable1) {
               list.add (cc);
            }
         }
         for (ContactConstraint cc : getBilaterals()) {
            if (cc.myPnt0OnCollidable1) {
               list.add (cc);
            }
         }
         myOrderedBilaterals = list;
      }
      return myOrderedBilaterals;
   }


   Collection<ContactConstraint> getBilaterals() {
      return myBilaterals.values();
   }

   Collection<ContactConstraint> getUnilaterals() {
      return myUnilaterals.values();
   }

   public void getConstrainedComponents (HashSet<DynamicComponent> set) {
      getConstraintComponents (set, getBilaterals());
      getConstraintComponents (set, getUnilaterals());
   }
   
   @Override
   public void getBilateralSizes (VectorNi sizes) {
      int numb = getBilaterals().size();
      for (int i=0; i<numb; i++) {
         sizes.append (1);
      }
   }

   @Override
   public void getUnilateralSizes (VectorNi sizes) {
      int numu = getUnilaterals().size();
      for (int i=0; i<numu; i++) {
         sizes.append (1);
      }
   }

   public void getBilateralConstraints (List<ContactConstraint> list) {
      list.addAll (getBilaterals());
   }

   @Override
   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {
      double[] dbuf = (dg != null ? dg.getBuffer() : null);
      int solveIdx = GT.colSize(); 
      for (ContactConstraint c : getOrderedBilaterals()) {
         c.setSolveIndex (solveIdx++);
         if (numb >= dbuf.length) {
            System.out.println (
               "  access error: " +myCollidable0.getName()+" "+
            myCollidable1.getName() + " len=" + dbuf.length);
         }
         numb = c.addBilateralConstraints (GT, dbuf, numb);
      }
      return numb;
   }

   ContactForceBehavior getForceBehavior() {
      if (myBehavior.myForceBehavior != null) {
         return myBehavior.myForceBehavior;
      }
      else if (myManager.myForceBehavior != null) {
         return myManager.myForceBehavior;
      }
      else {
         return null;
      }
   }

   @Override
   public int getBilateralInfo (
      ConstraintInfo[] ginfo, int idx) {
      
      double[] fres = new double[] { 
         0, myCompliance, myDamping };

      ContactForceBehavior forceBehavior = getForceBehavior();
      double penTol = myBehavior.myPenetrationTol;
      
      int flags = 
         (usingTwoWayContact() ? ContactForceBehavior.TWO_WAY_CONTACT : 0);
      for (ContactConstraint c : getOrderedBilaterals()) {
         ConstraintInfo gi = ginfo[idx];
         if (c.getDistance() < -penTol) {
            gi.dist = (c.getDistance() + penTol);
         }
         else {
            gi.dist = 0;
         }
         if (forceBehavior != null) {
            forceBehavior.computeResponse (
               fres, c.myDistance, c.myCpnt0, c.myCpnt1, 
               c.myNormal, c.myContactArea, flags);
         }
         gi.force =      fres[0];
         gi.compliance = fres[1];
         gi.damping =    fres[2];
         idx++;
      }
      return idx;
   }

   int setBilateralForces (double[] buf, double s, int idx) {
      for (ContactConstraint c : getOrderedBilaterals()) {
         idx = c.setForces (buf, s, idx);
      }
      return idx;
   }

   @Override
   public int setBilateralForces (VectorNd lam, double s, int idx) {
      return setBilateralForces (lam.getBuffer(), s, idx);
   }

   int getBilateralForces (double[] buf, int idx) {
      for (ContactConstraint c : getOrderedBilaterals()) {
         idx = c.getForces (buf, idx);
      }
      return idx;
   }

   @Override
   public int getBilateralForces (VectorNd lam, int idx) {
      return getBilateralForces (lam.getBuffer(), idx);
   }

   @Override
   public void zeroForces() {
      for (ContactConstraint c : getBilaterals()) {
         c.zeroForces();
      }   
      for (ContactConstraint c : getUnilaterals()) {
         c.zeroForces();
      }
   }

   @Override
   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {

      double[] dbuf = (dn != null ? dn.getBuffer() : null);
      int bj = NT.numBlockCols();
      int solveIdx = NT.colSize(); 
      for (ContactConstraint c : getUnilaterals()) {
         c.setSolveIndex (solveIdx++);
         c.addConstraintBlocks (NT, bj++);
         if (dbuf != null) {
            dbuf[numu] = c.getDerivative();
         }
         numu++;
      }
      return numu;
   }
   
   @Override
   public int getUnilateralInfo (ConstraintInfo[] ninfo, int idx) {

      double[] fres = new double[] {
         0, myCompliance, myDamping };

      ContactForceBehavior forceBehavior = getForceBehavior();
       double penTol = myBehavior.myPenetrationTol;
      
      int flags = 
         (usingTwoWayContact() ? ContactForceBehavior.TWO_WAY_CONTACT : 0);
      for (ContactConstraint c : getUnilaterals()) {
         ConstraintInfo ni = ninfo[idx];
         if (c.getDistance() < -penTol) {
            ni.dist = (c.getDistance() + penTol);
         }
         else {
            ni.dist = 0;
         }
         if (forceBehavior != null) {
            forceBehavior.computeResponse (
               fres, c.myDistance, c.myCpnt0, c.myCpnt1, 
               c.myNormal, c.myContactArea, flags);
         }
         ni.force =      fres[0];
         ni.compliance = fres[1];
         ni.damping =    fres[2];
         idx++;
      }
      return idx;
   }

   int setUnilateralForces (double[] buf, double s, int idx) {
      for (ContactConstraint c : getUnilaterals()) {
         idx = c.setForces (buf, s, idx);
      }
      return idx;
   }

   @Override
   public int setUnilateralForces (VectorNd the, double s, int idx) {
      idx = setUnilateralForces (the.getBuffer(), s, idx);
      return idx;
   }

   int getUnilateralForces (double[] buf, int idx) {
      for (ContactConstraint c : getUnilaterals()) {
         idx = c.getForces (buf, idx);
      }
      return idx;
   }

   @Override
   public int getUnilateralForces (VectorNd the, int idx) {
      return getUnilateralForces (the.getBuffer(), idx);
   }
   
   public int setUnilateralState (VectorNi state, int idx) {
      int[] buf = state.getBuffer();
      for (ContactConstraint c : getUnilaterals()) {
         idx = c.setState (buf, idx);
      } 
      return idx;
   }

   public int getUnilateralState (VectorNi state, int idx) {
      int[] buf = state.getBuffer();
      for (ContactConstraint c : getUnilaterals()) {
         idx = c.getState (buf, idx);
      } 
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return myBilaterals.size() + myUnilaterals.size();
   }

   static double ftol = 1e-2;
   public static boolean myPrune = true;

   @Override
   public int addFrictionConstraints (
      SparseBlockMatrix DT, ArrayList<FrictionInfo> finfo, 
      boolean prune, int numf) {

      double mu = myBehavior.myFriction;
      if (mu <= 0) {
         return numf;
      }
      double creep = myBehavior.myStictionCreep;
      double compliance = myBehavior.myStictionCompliance;
      boolean bilateralContact = true;

      if (myManager.use2DFrictionForBilateralContact()) {
         for (ContactConstraint c : getOrderedBilaterals()) {
            numf = c.add2DFrictionConstraints (
               DT, finfo, mu, creep, compliance, numf, false, bilateralContact);
         }
      }
      else {
         for (ContactConstraint c : getOrderedBilaterals()) {
            numf = c.add1DFrictionConstraints (
               DT, finfo, mu, creep, compliance, numf);
         }
      }

      bilateralContact = false;
      for (ContactConstraint c : getUnilaterals()) {
         if (!myPrune) {
            if (Math.abs(c.getContactForce())*mu <= ftol) {
               continue;
            }
         }
         numf = c.add2DFrictionConstraints (
            DT, finfo, mu, creep, compliance, numf, prune, bilateralContact);
      }
      return numf;
   }

   /**
    * {@inheritDoc}
    */
   public int setFrictionForces (VectorNd phi, double s, int idx) {
      double mu = myBehavior.myFriction;
      if (mu > 0) {

         if (myManager.use2DFrictionForBilateralContact()) {
            for (ContactConstraint c : getOrderedBilaterals()) {
               idx = c.set2DFrictionForce (phi, s, idx);
            }
         }
         else {
            for (ContactConstraint c : getOrderedBilaterals()) {
               idx = c.set1DFrictionForce (phi, s, idx);
            }
         }

         for (ContactConstraint c : getUnilaterals()) {
            if (!myPrune) {
               if (Math.abs(c.getContactForce())*mu <= ftol) {
                  continue;
               }    
            }
            idx = c.set2DFrictionForce (phi, s, idx);
         }
      }
      return idx;
   }

   /**
    * {@inheritDoc}
    */
   public int getFrictionForces (VectorNd phi, int idx) {
      double mu = myBehavior.myFriction;
      if (mu > 0) {

         if (myManager.use2DFrictionForBilateralContact()) {
            for (ContactConstraint c : getOrderedBilaterals()) {
               idx = c.get2DFrictionForce (phi, idx);
            }
         }
         else {
            for (ContactConstraint c : getOrderedBilaterals()) {
               idx = c.get1DFrictionForce (phi, idx);
            }
         }
         for (ContactConstraint c : getUnilaterals()) {
            if (!myPrune) {
               if (Math.abs(c.getContactForce())*mu <= ftol) {
                  continue;
               }
            }
            idx = c.get2DFrictionForce (phi, idx);
         }
      }
      return idx;
   }
   
   public int setFrictionState (VectorNi state, int idx) {
      double mu = myBehavior.myFriction;
      if (mu > 0) {

         if (myManager.use2DFrictionForBilateralContact()) {
            for (ContactConstraint c : getOrderedBilaterals()) {
               idx = c.set2DFrictionState (state, idx);
            }
         }
         else {
            for (ContactConstraint c : getOrderedBilaterals()) {
               idx = c.set1DFrictionState (state, idx);
            }
         }
         for (ContactConstraint c : getUnilaterals()) {
            if (!myPrune) {
               if (Math.abs(c.getContactForce())*mu <= ftol) {
                  continue;
               }     
            }
            idx = c.set2DFrictionState (state, idx);
         }
      }
      return idx;
   }
   
   public int getFrictionState (VectorNi state, int idx) {
      double mu = myBehavior.myFriction;
      if (mu > 0) {

         if (myManager.use2DFrictionForBilateralContact()) {
            for (ContactConstraint c : getOrderedBilaterals()) {
               idx = c.get2DFrictionState (state, idx);
            }
         }
         else {
            for (ContactConstraint c : getOrderedBilaterals()) {
               idx = c.get1DFrictionState (state, idx);
            }
         }
         for (ContactConstraint c : getUnilaterals()) {
            if (!myPrune) {
               if (Math.abs(c.getContactForce())*mu <= ftol) {
                  continue;
               }
            }
            idx = c.get2DFrictionState (state, idx);
         }
      }
      return idx;  
   }
   

   public void printNetContactForces() {
      Vector3d fforce = new Vector3d();
      Vector3d nforce = new Vector3d();

      for (ContactConstraint c : getUnilaterals()) {
         fforce.add (c.getFrictionForce());
         nforce.scaledAdd (c.getContactForce(), c.myNormal);
      }

      System.out.println ("net friction: "+fforce.toString("%10.5f"));
      System.out.println ("net normal:   "+nforce.toString("%10.5f"));
      System.out.printf  ("ratio:        %g\n", fforce.norm()/nforce.norm());
   }
   
   public int numBilateralConstraints() {
      return myBilaterals.size();
   }

   public int numUnilateralConstraints() {
      return myUnilaterals.size();
   }

   /** 
    * {@inheritDoc}
    */
   public void getState (DataBuffer data) {
      data.zput (myBilaterals.size());
      data.zput (myUnilaterals.size());
      for (ContactConstraint c : getBilaterals()) {
         c.getState (data);
      }
      for (ContactConstraint c : getUnilaterals()) {
         c.getState (data);
      }
      // If state needs contact info, then we need to save both
      // contactInfo and lastContactInfo, which is annoying but
      // no way around it at the moment.
      if (myStateNeedsContactInfo && myContactInfo != null) {
         data.zputBool (true);
         myContactInfo.getState (data);   
      }
      else {
         data.zputBool (false); // contact info not saved
      }
      if (myStateNeedsContactInfo && myLastContactInfo != null) {
         data.zputBool (true);
         myLastContactInfo.getState (data);
         data.zput (myLastBilateralData.size());
         data.zput (myLastUnilateralData.size());
         for (ContactData c : myLastBilateralData) {
            c.getState (data);
         }
         for (ContactData c : myLastUnilateralData) {
            c.getState (data);
         }
      }
      else {
         data.zputBool (false); // last contact info not saved
      }
   }

   /** 
    * {@inheritDoc}
    */
   public void setState (DataBuffer data) {

      clearContactData();
      int numb = data.zget();
      int numu = data.zget();
      for (int i=0; i<numb; i++) {
         ContactConstraint c = new ContactConstraint();
         c.setState (data, myCollidable0, myCollidable1);
         putContact (myBilaterals, c);
      }        
      for (int i=0; i<numu; i++) {
         ContactConstraint c = new ContactConstraint();
         c.setState (data, myCollidable0, myCollidable1);
         //HHHmyUnilaterals.add (c);
         putContact (myUnilaterals, c);
      }        
      boolean hasContactInfo = data.zgetBool();
      if (hasContactInfo) {
         ContactInfo cinfo = new ContactInfo (
            myCollidable0.getCollisionMesh(),
            myCollidable1.getCollisionMesh());
         cinfo.setState (data);
         myContactInfo = cinfo;
         myStateNeedsContactInfo = true; 
      }
      boolean hasLastContactInfo = data.zgetBool();
      if (hasLastContactInfo) {
         ContactInfo cinfo = new ContactInfo (
            myCollidable0.getCollisionMesh(),
            myCollidable1.getCollisionMesh());
         cinfo.setState (data);
         myLastContactInfo = cinfo;
         myStateNeedsContactInfo = true;
         numb = data.zget();
         numu = data.zget();
         for (int i=0; i<numb; i++) {
            ContactData c = new ContactData();
            c.setState (data, myCollidable0, myCollidable1);
            myLastBilateralData.add (c);
         }        
         for (int i=0; i<numu; i++) {
            ContactData c = new ContactData();
            c.setState (data, myCollidable0, myCollidable1);
            myLastUnilateralData.add (c);
         }
      }
      updateCompliance (myBehavior);
   }

   void initialize() {
      myContactInfo = null;
      myLastContactInfo = null;
      myLastBilateralData.clear();
      myLastUnilateralData.clear();
   }

   /* ==== Methods for creating vertex-based maps of force and pressure === */

   /**
    * Collect last forces associated with vertices on colA. These will 
    * arise from contact constraints in both the bilateral and unilateral 
    * constraint sets. For each constraint, colA will be associated with 
    * either cpnt0 or cpnt1. In the latter case, the scalar force is negated 
    * since the normal is oriented for the opposite body.
    * 
    * <p>This method is used by the collision response code and so
    * uses the last contact set.
    * 
    * @param map map from vertices to contact forces
    * @param colA collidable for which contact forces are desired
    */
   void collectLastContactForces (
      Map<Vertex3d,Vector3d> map, CollidableBody colA) {
      
      createVertexForceMap (map, colA == myCollidable0 ? 0 : 1);
   }

   /**
    * Create a vertex pressure map from a vertex force map.
    */
   static HashMap<Vertex3d,Double> createVertexPressureMap (
      HashSet<Face> faces, Map<Vertex3d,Vector3d> forceMap) {
      HashMap<Vertex3d,Double> map = new LinkedHashMap<>();
      Vector3d xsum = new Vector3d();
      Vector3d funit = new Vector3d();
      for (Map.Entry<Vertex3d,Vector3d> entry : forceMap.entrySet()) {
         // Convert forces to pressures. In general, pressure on a surface is
         // given by p = f / A, where f is the force and A is the component of
         // the surface area perpendicular to the force. For a vertex, we have
         //
         // p = fmag / sum A_i
         //
         // where fmag = ||f|| and A_i is the portion of each adjacent face
         // area perpendicular to the force. A_i is in turn computed from
         //
         // A_i = funit . a_i / 3
         //
         // where funit = f/fmag and a_i = (u_i X v_i) / 2 is the area vector
         // computed from the cross product of the inbound and outbound face
         // edges. We divide by 3 since each vertex is allocated 1/3 of the
         // area of each triangular face.
         //
         // Letting xsum = sum_i (u_i X v_i), we have
         //
         // p = 6 fmag / funit . xsum
         //
         funit.set (entry.getValue());
         double fmag = funit.norm();
         if (fmag > 0) {
            funit.scale (1/fmag);
            Vertex3d vertex = entry.getKey();
            vertex.sumEdgeCrossProductsWorld (xsum);
            // Assume pressure > 0 if vertex normal and force are in opposite
            // directions. Pressure can be negative if funit.dot(xsum) > 0
            double pressure = -6*fmag/funit.dot(xsum);
            map.put (vertex, pressure);
            if (faces != null) {
               // add all faces adjacent to this vertex
               Iterator<HalfEdge> it = vertex.getIncidentHalfEdges();
               while (it.hasNext()) {
                  faces.add(it.next().getFace());
               }
            }
         } 
      }
      return map;
   }
   
   private HashMap<Vertex3d,Double> createVertexPressureMap (
      HashSet<Face> faces, int num) {

      HashMap<Vertex3d,Vector3d> forceMap = new LinkedHashMap<>();
      createVertexForceMap(forceMap, num);
      return createVertexPressureMap (faces, forceMap);
   }


   /**
    * Create map between vertices and contact forces for the collision meshes
    * of the collidale indexed by {@code num}.
    */
   private void createVertexForceMap (
      Map<Vertex3d,Vector3d> forceMap, int num) {
      
      // Create a vertex force map for the vertices on the opposite collidable.
      // In the case of two-way contact, these will to be added to forceMap.
      HashMap<Vertex3d,Vector3d> oppositeMap = new HashMap<>();

      // Start by finding mesh vertex forces for both the selected and opposite
      // collidables.
      for (ContactData cd : myLastBilateralData) {
         if (cd.myLambda > 0) {
            collectVertexForces (forceMap, oppositeMap, cd, num);
         }
      }
      for (ContactData cd : myLastUnilateralData) {
         if (cd.myLambda > 0) {
            collectVertexForces (forceMap, oppositeMap, cd, num);
         }
      }
      if (oppositeMap.size() == 0) {
         // no opposite forces, so we are done
         return;
      }
      if (forceMap.size() > 0) {
         // since oppositeMap.size also > 0, which implies two-way vertex
         // penetration collisions
         for (ContactData cd : myLastBilateralData) {
            addOppositeVertexForces (forceMap, oppositeMap, cd, num);
         }
         for (ContactData cd : myLastUnilateralData) {
            addOppositeVertexForces (forceMap, oppositeMap, cd, num);
         }
      }
      else {
         // map opposite forces onto the vertices of the penetrating points
         PolygonalMesh opmesh;
         if (num == 0) {
            opmesh = myCollidable1.getCollisionMesh();
         }
         else {
            opmesh = myCollidable0.getCollisionMesh();
         }
         for (PenetratingPoint p : myLastContactInfo.getPenetratingPoints(num)) {
            transferForceToVertex (forceMap, oppositeMap, p, opmesh);
         }
      }
   }

   private double computeVertexArea (Vertex3d vtx) {
      Vector3d xprod = new Vector3d();
      vtx.sumEdgeCrossProducts (xprod);
      return xprod.norm()/6;
   }

   private void collectVertexForces (
      Map<Vertex3d,Vector3d> forceMap, Map<Vertex3d,Vector3d> oppositeMap,
      ContactData cd, int num) {

      Vertex3d[] vtxs = null;
      double[] wgts = null;
      double lam = cd.myLambda;

      if (cd.myCpnt0.numVertices() > 0) {
         vtxs = cd.myCpnt0.getVertices();
         wgts = cd.myCpnt0.getWeights();
      }

      // true if cpnt0 is on the collidable specified by num
      boolean cpnt0OnCollidable = ((num == 0) ^ cd.myPnt0OnCollidable1);

      if (vtxs == null) {
         // Will only happen with the CONTOUR_REGION contact method. Need
         // to find the face and vertices directly. Assume that we can use
         // the position of cpnt0
         PolygonalMesh mesh;
         if (num == 0) {
            mesh = myCollidable0.getCollisionMesh();
         }
         else {
            mesh = myCollidable1.getCollisionMesh();
         }
         Vector2d uv = new Vector2d();
         Point3d nearPnt = new Point3d();
         Face face = BVFeatureQuery.getNearestFaceToPoint (
            nearPnt, uv, mesh, cd.myCpnt0.getPosition());
         if (face != null) {
            vtxs = face.getVertices();
            wgts = new double[] {1-uv.x-uv.y, uv.x, uv.y};
         }
         else {
            // shouldn't happen, but just in case face not found
            return;
         }
         if (!cpnt0OnCollidable) {
            // negate lam since the normal will be pointing the other way
            lam = -lam;
            // set cpnt0OnCollidable true since vertices have been computed for
            // collidable's mesh and we want to add forces to forceMap:
            cpnt0OnCollidable = true;
         }
      }
      if (cpnt0OnCollidable) {
         // add forces to forceMap
         Vector3d nrm = cd.getNormal();
         for (int i=0; i<vtxs.length; i++) {
            Vector3d force = forceMap.get (vtxs[i]);
            if (force == null) {
               force = new Vector3d();
               forceMap.put (vtxs[i], force);
            }
            force.scaledAdd (lam*wgts[i], nrm);
         }
      }
      else {
         // add forces to oppositeMap
         Vector3d nrm = cd.getNormal();
         for (int i=0; i<vtxs.length; i++) {
            Vector3d force = oppositeMap.get (vtxs[i]);
            if (force == null) {
               force = new Vector3d();
               oppositeMap.put (vtxs[i], force);
            }
            double area = computeVertexArea (vtxs[i]);
            // Use -lam because we want the force acting on num.  Store force
            // per unit area so we can properly distribute it onto the other
            // mesh
            force.scaledAdd (-lam*wgts[i]/area, nrm);
         }
      }
   }

   private void addOppositeVertexForces (
      Map<Vertex3d,Vector3d> forceMap, Map<Vertex3d,Vector3d> oppositeMap,
      ContactData cd, int num) {

      if ((num == 0 && cd.myPnt0OnCollidable1) ||
          (num == 1 && !cd.myPnt0OnCollidable1)) {
         // cpnt0 is not on the collidable corresponding to num
         return;
      }
      Vertex3d[] vtxs0 = null;
      double[] wgts0 = null;
      Vertex3d[] vtxs1 = null;
      double[] wgts1 = null;
      
      if (cd.myCpnt0.numVertices() > 0) {
         vtxs0 = cd.myCpnt0.getVertices();
         wgts0 = cd.myCpnt0.getWeights();
      }
      if (cd.myCpnt1.numVertices() > 0) {
         vtxs1 = cd.myCpnt1.getVertices();
         wgts1 = cd.myCpnt1.getWeights();
      }
      if (vtxs0 != null && vtxs1 != null) {
         for (int i=0; i<vtxs0.length; i++) {
            Vector3d force = forceMap.get (vtxs0[i]);
            if (force == null) {
               // force might be null if cd.myLamba <= 0
               force = new Vector3d();
               forceMap.put (vtxs0[i], force);
            }
            // add force contributions for the opposite mesh
            Vector3d opforce = new Vector3d();
            for (int j=0; j<vtxs1.length; j++) {
               Vector3d force1 = oppositeMap.get(vtxs1[j]);
               if (force1 != null) {
                  opforce.scaledAdd (wgts1[j], force1);
               }
            }
            force.scaledAdd (computeVertexArea (vtxs0[i]), opforce);
         }
      }
   }

   private void transferForceToVertex (
      Map<Vertex3d,Vector3d> forceMap, Map<Vertex3d,Vector3d> oppositeMap,
      PenetratingPoint p, PolygonalMesh opmesh) {
      
      Vertex3d vtx0 = p.vertex;
      Vertex3d[] vtxs1;
      double[] wgts1;

      Face face = p.face;
      Vector2d uv = p.coords;
      if (face == null) {
         // must find the nearest face on the opposite mesh         
         uv = new Vector2d();
         Point3d nearPnt = new Point3d();
         face = BVFeatureQuery.getNearestFaceToPoint (
            nearPnt, uv, opmesh, p.getPosition());
         if (face == null) {
            // just in case
            return;
         }
      }
      vtxs1 = face.getVertices();
      wgts1 = new double[] {1-uv.x-uv.y, uv.x, uv.y};

      // add force contributions for the opposite mesh
      Vector3d opforce = new Vector3d();
      for (int j=0; j<vtxs1.length; j++) {
         Vector3d force1 = oppositeMap.get(vtxs1[j]);
         if (force1 != null) {
            opforce.scaledAdd (wgts1[j], force1);
         }
      }
      Vector3d force = new Vector3d();
      force.scale (computeVertexArea (vtx0), opforce);
      forceMap.put (vtx0, force);
   }

   /* ===== Begin Render methods ===== */

   public void prerender (RenderProps props) {
      if (myRenderer == null) {
         myRenderer = new CollisionRenderer();
      }
      myRenderer.prerender (this, props);
   }

   public void prerender (RenderList list) {
      prerender (getRenderProps());
   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   public void render (Renderer renderer, int flags) {
      render (renderer, getRenderProps(), flags);
   }

   // Twist lastmomentumchange = null;

   public void render (Renderer renderer, RenderProps props, int flags) {

      if (myRenderer == null) {
         myRenderer = new CollisionRenderer();
      }
      myRenderer.render (renderer, this, props, flags);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      ContactInfo cinfo = myLastContactInfo;
      if (cinfo != null) {
         ArrayList<IntersectionContour> contours = cinfo.getContours();
         if (contours != null) {
            for (IntersectionContour contour : contours) {
               for (IntersectionPoint p : contour) {
                  p.updateBounds (pmin, pmax);
               }
            }
         }
      }
   }
   
   /**
    * Returns {@code true} if collidble0 for this handler corresponds to the
    * first collidable specified by its behavior.
    */
   boolean collidable0MatchesBehavior() {
      return myBehavior.collidable0MatchesBody(getCollidable(0));
   }
 
   /**
    * Called by the CollisionManager.
    */
   void updateColorMapValues () {

      ContactInfo cinfo = myLastContactInfo;
      if (cinfo != null && myBehavior.myDrawColorMap != ColorMapType.NONE) {

         int num = myBehavior.myRenderingCollidableNum;
//         if (!collidable0MatchesBehavior()) {
//            // then we want the *other* collidable body, so switch num
//            num = (num == 0 ? 1 : 0);
//         }
         HashSet<Face> faces = new HashSet<Face>();
         HashMap<Vertex3d,Double> valueMap =
            createVertexValueMap (faces, cinfo, num);

         if (faces.size() > 0) {
            // get value range
            double minv = 0;
            double maxv = 0;
            for (Double d : valueMap.values()) {
               if (d > maxv) {
                  maxv = d;
               }
               else if (d < minv) {
                  minv = d;
               }
            }
            if (myBehavior.myColorMapRange != null) {
               // behavior has its own range object, so update that
               myBehavior.myColorMapRange.updateInterval (minv, maxv);
            }
            else {
               // update global range object in CollisionManager
               ScalarRange range = myManager.getColorMapRange();
               if (range.getUpdating() == ScalarRange.Updating.AUTO_FIT) {
                  // expand interval to account for multiple color maps
                  range.expandInterval (new DoubleInterval(minv, maxv));
               }
               else {
                  range.updateInterval (minv, maxv);
               }
            }
            myColorMapVertexValues = valueMap;
            myColorMapFaces = faces;
            return;
         }
      }
      myColorMapFaces = null;
      myColorMapVertexValues = null;
   }

   /**
    * Used to create color map values.
    */
   private HashMap<Vertex3d,Double> createVertexValueMap (
      HashSet<Face> faces, ContactInfo cinfo, int num) {
      
      HashMap<Vertex3d,Double> valueMap;
      if (myBehavior.myDrawColorMap == ColorMapType.PENETRATION_DEPTH) {
         valueMap = new LinkedHashMap<>();
         for (PenetratingPoint pp : cinfo.getPenetratingPoints (num)) {
            valueMap.put (pp.vertex, pp.distance);
         }
         for (Vertex3d vertex : valueMap.keySet()) {
            Iterator<HalfEdge> it = vertex.getIncidentHalfEdges();
            while (it.hasNext()) {
               HalfEdge he = it.next();
               Face face = he.getFace();
               if (!faces.contains(face)) {
                  faces.add (face);
               }
            }
         }
      }
      else if (myBehavior.myDrawColorMap == ColorMapType.CONTACT_PRESSURE) {
         valueMap = createVertexPressureMap (faces, num);
      }
      else {
         valueMap = new LinkedHashMap<>(); // empty map
      }
      return valueMap;
   }            

   /* ===== End Render methods ===== */
   
}
