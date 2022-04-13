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
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.CollisionManager.BehaviorSource;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.mechmodels.Collidable.Group;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
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
   public static boolean hashUnilaterals = true;
   
   // structural information

   CollisionManager myManager;
   CollidableBody myCollidable0;
   CollidableBody myCollidable1;
   CollisionHandler myNext; // horizontal link in HandlerTable
   CollisionHandler myDown; // vertical link in HandlerTable
   boolean myActive;

   // collision behavior
   CollisionBehavior myBehavior;
   BehaviorSource myBehaviorSource;
   
   double myCompliance = 0;  // keep local copies of compliance and damping,
   double myDamping = 0;     // since these may be computed automatically

   // collision response

   LinkedHashMap<ContactPoint,ContactConstraint> myBilaterals;
   // list of bilaterals arranged in order, with those for which cpnt0 is on
   // collidable0 first and those with cpnt0 on collidable1 second. This
   // is done simply to maintain exact numeric compatibility with some
   // legacy tests.
   ArrayList<ContactConstraint> myOrderedBilaterals;
   LinkedHashMap<ContactPoint,ContactConstraint> myUnilaterals;
   int myMaxUnilaterals = 100;
   ContactInfo myLastContactInfo; // last contact info produced by this handler
   ContactInfo myRenderContactInfo; // contact info to be used for rendering
   boolean myStateNeedsContactInfo = false;
   
   HashSet<Vertex3d> myAttachedVertices0 = null;
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
      if (myManager != null) {
         return myManager.getContactNormalLen();
      }
      else {
         return 0;
      }
   }

   double getContactForceLenScale() {
      if (myManager != null && myBehavior.getDrawContactForces()) {
         return myManager.getContactForceLenScale();
      }
      else {
         return 0;
      }
   }

   double getFrictionForceLenScale() {
      if (myManager != null && myBehavior.getDrawFrictionForces()) {
         return myManager.getContactForceLenScale();
      }
      else {
         return 0;
      }
   }

   void setLastContactInfo(ContactInfo info) {
      myLastContactInfo = info;

      // DataBuffer state = new DataBuffer();
      // info.getState (state);
      // ContactInfo check =
      //    new ContactInfo (
      //       myCollidable0.getCollisionMesh(),
      //       myCollidable1.getCollisionMesh());
      // check.setState (state);
      // if (!check.equals (info)) {
      //    throw new InternalErrorException ("save/load ContactInfo failed");
      // }
      
   }

   public ContactInfo getLastContactInfo() {
      return myLastContactInfo;
   }

   protected static boolean isRigid (CollidableBody col) {
      return (col instanceof RigidBody || col instanceof RigidMeshComp);
   }

   public CollisionHandler (CollisionManager manager) {
      myBilaterals = new LinkedHashMap<ContactPoint,ContactConstraint>();
      // HHH myUnilaterals = new ArrayList<ContactConstraint>();
      myUnilaterals = new LinkedHashMap<ContactPoint,ContactConstraint>();
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
   
   Method getMethod() {
      return getMethod (myCollidable0, myCollidable1, myBehavior);
   }      

   static Method getMethod (
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
      Vertex3d[] vtxs = cpnt.myVtxs;
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
            //System.out.println (" old "+cons.myIdx+" " + cons);
            return cons;
         }
      }
   }

   public double computeCollisionConstraints (ContactInfo cinfo) {
      //clearRenderData();
      double maxpen = 0;
      myOrderedBilaterals = null; // will be rebuilt on demand
      if (cinfo != null) {

         if (!hashUnilaterals) {
            myUnilaterals.clear();
         }

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
                  "Unimplemented collision method: "+getMethod());
            }
         }
      }
      else {
         clearContactActivity();
         //removeInactiveContacts();
         if (!hashUnilaterals) {
            myUnilaterals.clear();
         }
      }
      setLastContactInfo(cinfo);
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
               "Unimplemented collision method: "+getMethod(col0, col1, behav));
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
    * Set whether or not this component is active. An inactive setting
    * means that the handler is not currently in use.
    */
   void setActive (boolean active) {
      myActive = active;
   }

   /**
    * Returns whether or not this handler is active.
    */
   boolean isActive() {
      return myActive;
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
      disp.sub(cons.myCpnt0.myPoint, cons.myCpnt1.myPoint);
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
      disp.sub(cons.myCpnt0.myPoint, cons.myCpnt1.myPoint);
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
               if (hashUnilaterals) {
                  cons = getContact (
                     myUnilaterals, pnt0, pnt1, 
                     false, pnt0OnCollidable1, eec.displacement); 
               }
               else {
                  cons = new ContactConstraint (pnt0, pnt1, pnt0OnCollidable1);
                  putContact (myUnilaterals, cons);
               }
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
         if (cpp.face == null || (collidable1 instanceof RigidBody)) {
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
            if (hashUnilaterals) {
               cons = getContact (
                  myUnilaterals, pnt0, pnt1, 
                  hashUsingFace, collidable0==myCollidable1, cpp.distance);
            }
            else {
               cons = new ContactConstraint (
                  pnt0, pnt1, collidable0==myCollidable1);
               putContact (myUnilaterals, cons);
            }
            //myUnilaterals.add (cons);
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

   double computeVertexPenetrationConstraints (ContactInfo info) {
      double maxpen = 0;
      clearContactActivity();
      
      if (info != null) {
         CollidableBody col0 = myCollidable0;
         CollidableBody col1 = myCollidable1;
         ArrayList<PenetratingPoint> pnts0 = info.getPenetratingPoints(0);
         ArrayList<PenetratingPoint> pnts1 = info.getPenetratingPoints(1);
         if (isRigid(myCollidable0) && !isRigid(myCollidable1)) {
            // swap bodies so that we compute vertex penetrations of 
            // collidable1 with respect to collidable0
            col0 = myCollidable1;
            col1 = myCollidable0;
            pnts0 = info.getPenetratingPoints(1);
            pnts1 = info.getPenetratingPoints(0);
         }
         maxpen = computeVertexPenetrationConstraints (pnts0,col0,col1);
         if (!hasLowDOF (col1) || myBehavior.getBodyFaceContact()) {
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

      //clearRenderData();

      // Currently, no correspondence is established between new contacts and
      // previous contacts. If there was, then we could set the multipliers for
      // the new contacts to something based on the old contacts, thereby
      // providing initial "warm start" values for the solver.

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
               //myUnilaterals.add (c);
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
            Point3d ppnt = prev.myCpnt0.getPoint();
            ContactConstraint nearc = null;
            double mindist = Double.MAX_VALUE;
            // search using brute force since using KDTrees or other
            // accelerating structures is slower unless we have many points
            // (like > 1000).
            for (ContactConstraint c : getUnilaterals()) {
               Point3d cpnt = c.myCpnt0.getPoint();
               double dist = ppnt.distance (cpnt);
               if (dist < mindist) {
                  mindist = dist;
                  nearc = c;
               }
            }
            nearc.myState = prev.myState;
            nearc.myFrictionState0 = prev.myFrictionState0;
            nearc.myFrictionState1 = prev.myFrictionState1;
         }
      }
   }

   void clearContactData() {
      myBilaterals.clear();
      myUnilaterals.clear();
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
      Iterator<ContactConstraint> it;
      it = getBilaterals().iterator();
      while (it.hasNext()) {
         ContactConstraint c = it.next();
         if (!c.isActive()) {
            it.remove();
         }
      }
      if (hashUnilaterals) {
         it = myUnilaterals.values().iterator();
         while (it.hasNext()) {
            ContactConstraint c = it.next();
            if (!c.isActive()) {
               it.remove();
               //mycontactschanged = true;
            }
         }  
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

      int numb0 = numb;
      double[] dbuf = (dg != null ? dg.getBuffer() : null);
      for (ContactConstraint c : getOrderedBilaterals()) {
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
      
      for (ContactConstraint c : getOrderedBilaterals()) {
         idx = c.getBilateralInfo (
            ginfo, idx, penTol, fres, forceBehavior);
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

      int numu0 = numu;
      double[] dbuf = (dn != null ? dn.getBuffer() : null);
      int bj = NT.numBlockCols();
      for (ContactConstraint c : getUnilaterals()) {
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
      
      for (ContactConstraint c : getUnilaterals()) {
         c.setSolveIndex (idx);
         ConstraintInfo ni = ninfo[idx];
         if (c.getDistance() < -myBehavior.myPenetrationTol) {
            ni.dist = (c.getDistance() + myBehavior.myPenetrationTol);
         }
         else {
            ni.dist = 0;
         }
         if (forceBehavior != null) {
            forceBehavior.computeResponse (
               fres, c.myDistance, c.myCpnt0, c.myCpnt1, 
               c.myNormal, c.myContactArea);
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

      int ncols0 = DT.colSize();
      double mu = myBehavior.myFriction;
      if (mu <= 0) {
         return numf;
      }
      double creep = myBehavior.myStictionCreep;
      boolean bilateralContact = true;

      if (myManager.use2DFrictionForBilateralContact()) {
         for (ContactConstraint c : getOrderedBilaterals()) {
            numf = c.add2DFrictionConstraints (
               DT, finfo, mu, creep, numf, false, bilateralContact);
         }
      }
      else {
         for (ContactConstraint c : getOrderedBilaterals()) {
            numf = c.add1DFrictionConstraints (DT, finfo, mu, creep, numf);
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
            DT, finfo, mu, creep, numf, prune, bilateralContact);
      }
//      if (myHasInvariantMasters) {
//         // set prevFrictionIdxs independently of the contacts, since the
//         // constraint matrix block pattern doesn't change
//         int sizeD = DT.colSize()-ncols0;
//         int idx = myBaseDIdx;
//         int maxidx = myBaseDIdx+myPrevSizeD;
//         for (FrictionInfo fi : finfo) {
//            if (idx != -1) {
//               int nextidx = idx+fi.blockSize;
//               if (nextidx < maxidx) {
//                  fi.prevFrictionIdx = idx;
//                  idx = nextidx;
//               }
//               else {
//                  fi.prevFrictionIdx = -1;
//                  idx = -1;
//               }
//            }
//            else {
//               fi.prevFrictionIdx = -1;
//            }
//         }
//         myPrevSizeD = sizeD;
//         myBaseDIdx = ncols0;
//      } 
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
         fforce.add (c.myFrictionForce);
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
//      if (myHasInvariantMasters) {
//         data.zput (myPrevSizeG);
//         data.zput (myPrevSizeN);
//         data.zput (myPrevSizeD);
//         data.zput (myBaseGIdx);
//         data.zput (myBaseNIdx);
//         data.zput (myBaseDIdx);
//      }
      for (ContactConstraint c : getBilaterals()) {
         c.getState (data);
      }
      for (ContactConstraint c : getUnilaterals()) {
         c.getState (data);
      }
      if (myStateNeedsContactInfo && myLastContactInfo != null) {
         // don't save by reference - not portable with saved waypoint data
         //data.oput (myLastContactInfo);
         data.zputBool (true);
         myLastContactInfo.getState (data);
      }
      else {
         data.oput (null);
         data.zputBool (false);
      }
   }

   /** 
    * {@inheritDoc}
    */
   public void setState (DataBuffer data) {

      clearContactData();
      int numb0 = data.zget();
      int numu = data.zget();
//      if (myHasInvariantMasters) {
//         myPrevSizeG = data.zget();
//         myPrevSizeN = data.zget();
//         myPrevSizeD = data.zget();
//         myBaseGIdx = data.zget();
//         myBaseNIdx = data.zget();
//         myBaseDIdx = data.zget();
//      }
      for (int i=0; i<numb0; i++) {
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
      // not portable with saved waypoint data
      //myLastContactInfo = (ContactInfo)data.oget();
      boolean hasContactInfo = data.zgetBool();
      if (hasContactInfo) {
         ContactInfo cinfo = new ContactInfo (
            myCollidable0.getCollisionMesh(),
            myCollidable1.getCollisionMesh());
         cinfo.setState (data);
         myLastContactInfo = cinfo;
         myStateNeedsContactInfo = true;
      }
      updateCompliance (myBehavior);
   }

   /* ===== Begin Render methods ===== */

   void initialize() {
      myLastContactInfo = null;
   }

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
      if (myRenderContactInfo != null) {
         ArrayList<IntersectionContour> contours = 
            myRenderContactInfo.getContours();
         if (contours != null) {
            for (IntersectionContour contour : contours) {
               for (IntersectionPoint p : contour) {
                  p.updateBounds (pmin, pmax);
               }
            }
         }
      }
   }

   protected void accumulateForces (
      Map<Vertex3d,Vector3d> map, ContactPoint cpnt, Vector3d nrml, double lam) {
      Vertex3d[] vtxs = cpnt.getVertices();
      double[] wgts = cpnt.getWeights();
      for (int i=0; i<vtxs.length; i++) {
         Vector3d imp = map.get(vtxs[i]);
         if (imp == null) {
            imp = new Vector3d();
            map.put (vtxs[i], imp);
         }
         imp.scaledAdd (lam*wgts[i], nrml);
      }
   }

   protected void accumulateForcesUnilateral (
      Map<Vertex3d,Vector3d> map, ContactPoint cpnt, Vector3d nrml, double lam) {
      Vertex3d v = new Vertex3d (cpnt.getPoint ());
      Vector3d imp = new Vector3d();
      imp.scaledAdd (lam, nrml);
      map.put (v, imp);
   }
   
   void getContactForces (Map<Vertex3d,Vector3d> map, CollidableBody colA) {
      // add forces associated with vertices on colA. These will arise from
      // contact constraints in both the bilateral and unilateral constraint
      // sets. For each constraint, colA will be associated with either
      // cpnt0 or cpnt1. In the latter case, the scalar force is negated 
      // since the normal is oriented for the opposite body.
      
      for (ContactConstraint c : getOrderedBilaterals()) {
         if (c.myCpnt0.isOnCollidable (colA)) {            
            accumulateForces (
               map, c.myCpnt0, c.getNormal(), c.getContactForce());
         }
         else {
            accumulateForces (
               map, c.myCpnt1, c.getNormal(), -c.getContactForce());
         }
      }
      // added by Fabien Pean, March 28, 2017
      for (ContactConstraint c : getUnilaterals()) {
         if (c.myCpnt0.isOnCollidable (colA)) {            
            accumulateForces (
               map, c.myCpnt0, c.getNormal(), c.getContactForce());
         }
         else {
            accumulateForces (
               map, c.myCpnt1, c.getNormal(), -c.getContactForce());
         }
      }
   }

   /**
    * Get most recent ContactInfo info, for rendering purposes. If no collision
    * occured, this may be null.
    */
   public synchronized ContactInfo getRenderContactInfo() {
      return myRenderContactInfo;
   }

   void updateColorMapValues () {

      if (myLastContactInfo != null &&
          myBehavior.myDrawColorMap != ColorMapType.NONE) {

         int num = myBehavior.myColorMapCollidableNum;
         Collidable b0 = myBehavior.getCollidable(0);
         CollidableBody h0 = getCollidable(0);
         if (!(b0 instanceof Group)) {
            if (h0 != b0 && h0.getCollidableAncestor() != b0) {
               // then we want the *other* collidable body, so switch num
               num = (num == 0 ? 1 : 0);
            }
         }
         HashSet<Face> faces = new HashSet<Face>();
         HashMap<Vertex3d,Double> valueMap =
         myColorMapVertexValues =
            createVertexValueMap (faces, myLastContactInfo, num);

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

   HashMap<Vertex3d,Double> createVertexValueMap (
      HashSet<Face> faces, ContactInfo cinfo, int num) {
      
      HashMap<Vertex3d,Double> valueMap = new HashMap<Vertex3d,Double>();
      if (myBehavior.myDrawColorMap == ColorMapType.PENETRATION_DEPTH) {
         ArrayList<PenetratingPoint> points;
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
         PolygonalMesh mesh;
         if (num == 0) {
            mesh = myCollidable0.getCollisionMesh();
         }
         else {
            mesh = myCollidable1.getCollisionMesh();
         }
         
         for (ContactConstraint cc : getBilaterals()) {
            if (cc.myLambda > 0) {
               storeVertexForces (valueMap, cc, mesh);
            }
         }
         for (ContactConstraint cc : getUnilaterals()) {
            if (cc.myLambda > 0) {
               storeVertexForces (valueMap, cc, mesh);
            }
         }
         for (Map.Entry<Vertex3d,Double> entry : valueMap.entrySet()) {
            // convert forces to pressures
            Vertex3d vertex = entry.getKey();
            double lam = entry.getValue();
            // Pressure at the vertex is related to force at the vertex
            // by the formula
            // 
            //    force = 1/3 * pressure * adjacentFaceArea
            //
            double adjacentFaceArea = 0;
            Iterator<HalfEdge> it = vertex.getIncidentHalfEdges();
            while (it.hasNext()) {
               HalfEdge he = it.next();
               Face face = he.getFace();
               if (!faces.contains(face)) {
                  // update planar area for the face
                  face.computeNormal();
                  faces.add (face);
               }
               adjacentFaceArea += face.getPlanarArea();
            }
            double pressure = 3*lam/adjacentFaceArea;
            valueMap.put (vertex, pressure);              
         }
      }
      return valueMap;
   }            

   private boolean containsMeshVertices (ContactPoint cp, PolygonalMesh mesh) {
      return (cp.numVertices() > 0 && cp.getVertices()[0].getMesh() == mesh);
   }

   protected void storeVertexForces (
      HashMap<Vertex3d,Double> valueMap,
      ContactConstraint cc, PolygonalMesh mesh) {

      Vertex3d[] vtxs = null;
      double[] wgts = null;

      // check cpnt0 and cpnt1 for vertices belonging to the mesh
      if (containsMeshVertices (cc.myCpnt0, mesh)) {
         vtxs = cc.myCpnt0.getVertices();
         wgts = cc.myCpnt0.getWeights();
      }
      else if (containsMeshVertices (cc.myCpnt1, mesh)) {
         vtxs = cc.myCpnt1.getVertices();
         wgts = cc.myCpnt1.getWeights();
      }
      else {
         // have to find the face and vertices directly. Assume 
         // that we can use the position of cpnt0
         BVFeatureQuery query = new BVFeatureQuery();
         Vector2d uv = new Vector2d();
         Point3d nearPnt = new Point3d();
         Face face = query.getNearestFaceToPoint (
            nearPnt, uv, mesh, cc.myCpnt0.getPoint());
         if (face != null) {
            vtxs = face.getVertices();
            wgts = new double[] {1-uv.x-uv.y, uv.x, uv.y};
         }
      }
      // check vtxs == null just in case query.getNearestFaceToPoint failed
      // for some reason
      if (vtxs != null) {
         for (int i=0; i<vtxs.length; i++) {
            double lam = cc.myLambda*wgts[i];
            Double prevLam = valueMap.get (vtxs[i]);
            if (prevLam != null) {
               lam += prevLam;
            }
            valueMap.put (vtxs[i], lam);                     
         }
      }
   }

   /* ===== End Render methods ===== */
   
}
