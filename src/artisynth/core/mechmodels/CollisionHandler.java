/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;

import maspack.collision.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.mechmodels.CollisionManager.BehaviorSource;

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
   public static boolean useOneBilateralSet = false;
   
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

   LinkedHashMap<ContactPoint,ContactConstraint> myBilaterals0;
   LinkedHashMap<ContactPoint,ContactConstraint> myBilaterals1;
   ArrayList<ContactConstraint> myUnilaterals;
   // myPrevUnilaterals stores previous unilaterals so we have access
   // to force values in rendering code
   ArrayList<ContactConstraint> myPrevUnilaterals;
   int myMaxUnilaterals = 100;
   ContactInfo myLastContactInfo; // last contact info produced by this handler
   ContactInfo myRenderContactInfo; // contact info to be used for rendering

   boolean myStateNeedsContactInfo = false;
   
   HashSet<Vertex3d> myAttachedVertices0 = null;
   HashSet<Vertex3d> myAttachedVertices1 = null;
   boolean myAttachedVerticesValid = false;

   // rendering

   CollisionRenderer myRenderer;

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
      myBilaterals0 = new LinkedHashMap<ContactPoint,ContactConstraint>();
      myBilaterals1 = new LinkedHashMap<ContactPoint,ContactConstraint>();
      myUnilaterals = new ArrayList<ContactConstraint>();
      myPrevUnilaterals = new ArrayList<ContactConstraint>();
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
      boolean hashUsingFace, double distance) {

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
         cons = new ContactConstraint (cpnt0, cpnt1);
         cons.myIdentifyByPoint1 = hashUsingFace;
         putContact (contacts, cons);
         return cons;
      }
      else { // contact already exists
         double lam = cons.getForce();
         //do not activate constraint if contact is trying to separate
         if (lam < 0) {
            return null;
         }
         else if (cons.isActive() && -cons.getDistance() >= distance) {
            // if constraint exists and it has already been set with a distance
            // greater than the current one, don't return anything; leave the
            // current one alone. This is for cases where the same feature maps
            // onto more than one contact.
            return null;
         }
         else {
            // update contact points
            cons.setContactPoints (cpnt0, cpnt1);
            return cons;
         }
      }
   }

   public double computeCollisionConstraints (ContactInfo cinfo) {
      //clearRenderData();
      double maxpen = 0;
      if (cinfo != null) {

         // store unilateral data in myPrevUnilaterals so we have access to
         // force data in rendering code
         myPrevUnilaterals.clear();
         myPrevUnilaterals.addAll(myUnilaterals);
         myUnilaterals.clear();

         switch (getMethod()) {
            case VERTEX_PENETRATION: 
            case VERTEX_PENETRATION_BILATERAL:
            case VERTEX_EDGE_PENETRATION: {
               maxpen = computeVertexPenetrationConstraints (
                  cinfo, myCollidable0, myCollidable1);
               break;
            }
            case CONTOUR_REGION: {
               maxpen = computeContourRegionConstraints (
                  cinfo, myCollidable0, myCollidable1);
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
         removeInactiveContacts();
         myUnilaterals.clear();
         myPrevUnilaterals.clear();
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
      // collidable since that is more likely to persist.
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
      ArrayList<EdgeEdgeContact> eecs,
      CollidableBody collidable0, CollidableBody collidable1) {

      double maxpen = 0;

      if (eecs != null) {
         for (EdgeEdgeContact eec : eecs) {
            // Check if the contact has already been corrected by other contact
            // corrections.
            //if (eec.calculate()) {
   
            ContactPoint pnt0, pnt1;
            pnt0 = new ContactPoint (eec.point0, eec.edge0, eec.s0);
            pnt1 = new ContactPoint (eec.point1, eec.edge1, eec.s1);
   
            ContactConstraint cons;
            if (myBehavior.getBilateralVertexContact()) {
               cons = getContact (
                  myBilaterals0, pnt0, pnt1, false, eec.displacement);
            }
            else {
               cons = new ContactConstraint (pnt0, pnt1);
               myUnilaterals.add (cons);
            }
            // As long as the constraint exists and is not already marked 
            // as active, then we add it
            if (cons != null) {
               cons.setActive (true);
   
               double dist = setEdgeEdge (cons, eec, collidable0, collidable1);
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
         if (cpp.face != null) {
            pnt1 = new ContactPoint (cpp.position, cpp.face, cpp.coords);
         }
         else {
            pnt1 = new ContactPoint (cpp.position);
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
            if (useOneBilateralSet || collidable0 == myCollidable0) {
               cons = getContact (
                  myBilaterals0, pnt0, pnt1, hashUsingFace, cpp.distance);
            }
            else {
               cons = getContact (
                  myBilaterals1, pnt0, pnt1, hashUsingFace, cpp.distance);
            }
         }
         else {
            cons = new ContactConstraint (pnt0, pnt1);
            myUnilaterals.add (cons);
         }
         
         
         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null) {
            cons.setActive (true);

            double dist;
            if (cpp.getFace() != null) {
               dist = setVertexFace (cons, cpp, collidable0, collidable1);
            }
            else {
               dist = setVertexBody (cons, cpp, collidable0, collidable1);
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

   double computeVertexPenetrationConstraints (
      ContactInfo info, CollidableBody collidable0, CollidableBody collidable1) {
      double maxpen = 0;
      clearContactActivity();
      
      if (info != null) {
         CollidableBody col0 = collidable0;
         CollidableBody col1 = collidable1;
         ArrayList<PenetratingPoint> pnts0 = info.getPenetratingPoints(0);
         ArrayList<PenetratingPoint> pnts1 = info.getPenetratingPoints(1);
         if (isRigid(collidable0) && !isRigid(collidable1)) {
            // swap bodies so that we compute vertex penetrations of 
            // collidable1 with respect to collidable0
            col0 = collidable1;
            col1 = collidable0;
            pnts0 = info.getPenetratingPoints(1);
            pnts1 = info.getPenetratingPoints(0);
         }
         maxpen = computeVertexPenetrationConstraints (pnts0, col0, col1);
         if (!hasLowDOF (col1) || myBehavior.getBodyFaceContact()) {
            double pen = computeVertexPenetrationConstraints (pnts1, col1, col0);
            if (pen > maxpen) {
               maxpen = pen;
            }
         }
         if (getMethod() == CollisionBehavior.Method.VERTEX_EDGE_PENETRATION) {
            double pen = computeEdgePenetrationConstraints (
               info.getEdgeEdgeContacts(), collidable0, collidable1);
            if (pen > maxpen) {
               maxpen = pen;
            }
         }
      }
      removeInactiveContacts();
      //printContacts ("%g");
      return maxpen;
   }

   double computeContourRegionConstraints (
      ContactInfo info, CollidableBody collidable0, CollidableBody collidable1) {

      double maxpen = 0;

      //clearRenderData();

      // Currently, no correspondence is established between new contacts and
      // previous contacts. If there was, then we could set the multipliers for
      // the new contacts to something based on the old contacts, thereby
      // providing initial "warm start" values for the solver.

      if (info != null) {
         int numc = 0;
         info.setPointTol (myBehavior.myRigidPointTol);
         info.setContactPlaneTol (myBehavior.myRigidRegionTol);
         for (ContactPlane region : info.getContactPlanes()) {
            for (Point3d p : region.points) {
               if (numc >= myMaxUnilaterals)
                  break;

               ContactConstraint c = new ContactConstraint();

               c.setContactPoint0 (p);
               c.equateContactPoints();
               c.setNormal (region.normal);
               c.assignMasters (collidable0, collidable1);
               c.myContactArea = region.contactAreaPerPoint;

               maxpen = region.depth;
               c.setDistance (-region.depth);
               myUnilaterals.add (c);
               numc++;
            }
         }
      }
      return maxpen;
   }

   void clearContactData() {
      myBilaterals0.clear();
      myBilaterals1.clear();
      myUnilaterals.clear();
      myPrevUnilaterals.clear();
   }

   public void clearContactActivity() {
      for (ContactConstraint c : myBilaterals0.values()) {
         c.setActive (false);
         c.setDistance (0);
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.setActive (false);
         c.setDistance (0);
      }
   }

   public void removeInactiveContacts() {
      Iterator<ContactConstraint> it;
      it = myBilaterals0.values().iterator();
      while (it.hasNext()) {
         ContactConstraint c = it.next();
         if (!c.isActive()) {
            it.remove();
            //mycontactschanged = true;
         }
      }
      it = myBilaterals1.values().iterator();
      while (it.hasNext()) {
         ContactConstraint c = it.next();
         if (!c.isActive()) {
            it.remove();
            //mycontactschanged = true;
         }
      }
   }

   private void printContacts(String fmtStr) {
      Iterator<ContactConstraint> it;
      it = myBilaterals0.values().iterator();
      System.out.println ("mesh0");
      while (it.hasNext()) {
         ContactConstraint c = it.next();
         System.out.println (" " + c.toString(fmtStr));
      }
      it = myBilaterals1.values().iterator();
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
   
   Collection<ContactConstraint> getBilaterals0() {
      if (useOneBilateralSet) {
         ArrayList<ContactConstraint> list = new ArrayList<>();
         for (ContactConstraint cc : myBilaterals0.values()) {
            if (cc.myCpnt0.isOnCollidable (myCollidable0)) {
               list.add (cc);
            }
         }
         for (ContactConstraint cc : myBilaterals0.values()) {
            if (!cc.myCpnt0.isOnCollidable (myCollidable0)) {
               list.add (cc);
            }
         }
         return list;
      }
      else {
         return myBilaterals0.values();
      }
   }

   public void getConstrainedComponents (HashSet<DynamicComponent> set) {
      getConstraintComponents (set, getBilaterals0());
      getConstraintComponents (set, myBilaterals1.values());
      getConstraintComponents (set, myUnilaterals);      
   }
   
   @Override
   public void getBilateralSizes (VectorNi sizes) {
      for (int i=0; i<myBilaterals0.size(); i++) {
         sizes.append (1);
      }
      for (int i=0; i<myBilaterals1.size(); i++) {
         sizes.append (1);
      }
   }

   @Override
   public void getUnilateralSizes (VectorNi sizes) {
      for (int i=0; i<myUnilaterals.size(); i++) {
         sizes.append (1);
      }
   }

   public void getBilateralConstraints (List<ContactConstraint> list) {
      list.addAll (getBilaterals0());
      list.addAll (myBilaterals1.values());
   }

   @Override
   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      double[] dbuf = (dg != null ? dg.getBuffer() : null);

      for (ContactConstraint c : getBilaterals0()) {
         c.addConstraintBlocks (GT, GT.numBlockCols());
         if (dbuf != null) {
            dbuf[numb] = c.getDerivative();
         }
         numb++;
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.addConstraintBlocks (GT, GT.numBlockCols());
         if (dbuf != null) {
            dbuf[numb] = c.getDerivative();
         }
         numb++;
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
   public int getBilateralInfo(ConstraintInfo[] ginfo, int idx) {
      
      double[] fres = new double[] { 
         0, myCompliance, myDamping };

      ContactForceBehavior forceBehavior = getForceBehavior();
      
      for (ContactConstraint c : getBilaterals0()) {
         c.setSolveIndex (idx);
         ConstraintInfo gi = ginfo[idx++];
         if (c.getDistance() < -myBehavior.myPenetrationTol) {
            gi.dist = (c.getDistance() + myBehavior.myPenetrationTol);
         }
         else {
            gi.dist = 0;
         }
         if (forceBehavior != null) {
            forceBehavior.computeResponse (
               fres, c.myDistance, c.myCpnt0, c.myCpnt1, 
               c.myNormal, c.myContactArea);
         }
         gi.force =      fres[0];
         gi.compliance = fres[1];
         gi.damping =    fres[2];
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.setSolveIndex (idx);
         ConstraintInfo gi = ginfo[idx++];
         if (c.getDistance() < -myBehavior.myPenetrationTol) {
            gi.dist = (c.getDistance() + myBehavior.myPenetrationTol);
         }
         else {
            gi.dist = 0;
         }
         if (forceBehavior != null) {
            forceBehavior.computeResponse (
               fres, c.myDistance, c.myCpnt0, c.myCpnt1, 
               c.myNormal, c.myContactArea);
         }
         gi.force =      fres[0];
         gi.compliance = fres[1];
         gi.damping =    fres[2];
      }
      return idx;
   }

   int setBilateralForces (double[] buf, double s, int idx) {
      for (ContactConstraint c : getBilaterals0()) {
         c.setForce (buf[idx++]*s);
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.setForce (buf[idx++]*s);
      }
      return idx;
   }

   @Override
   public int setBilateralForces (VectorNd lam, double s, int idx) {
      idx = setBilateralForces (lam.getBuffer(), s, idx);
      return idx;
   }

   int getBilateralForces (double[] buf, int idx) {
      for (ContactConstraint c : getBilaterals0()) {
         buf[idx++] = c.getForce();
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         buf[idx++] = c.getForce();
      }
      return idx;
   }

   @Override
   public int getBilateralForces (VectorNd lam, int idx) {
      return getBilateralForces (lam.getBuffer(), idx);
   }

   @Override
   public void zeroForces() {
      for (ContactConstraint c : myBilaterals0.values()) {
         c.setForce (0);
      }   
      for (ContactConstraint c : myBilaterals1.values()) {
         c.setForce (0);
      }   
      for (int i=0; i<myUnilaterals.size(); i++) {
         myUnilaterals.get(i).setForce (0);
      }
   }

   @Override
   public int addUnilateralConstraints (
      SparseBlockMatrix NT, VectorNd dn, int numu) {

      double[] dbuf = (dn != null ? dn.getBuffer() : null);
      int bj = NT.numBlockCols();
      for (int i=0; i<myUnilaterals.size(); i++) {
         ContactConstraint c = myUnilaterals.get(i);
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
      
      for (int i=0; i<myUnilaterals.size(); i++) {
         ContactConstraint c = myUnilaterals.get(i);
         c.setSolveIndex (idx);
         ConstraintInfo ni = ninfo[idx++];
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
      }
      return idx;
   }

   int setUnilateralForces (double[] buf, double s, int idx) {
      for (int i=0; i<myUnilaterals.size(); i++) {
         myUnilaterals.get(i).setForce (buf[idx++]*s);
      }
      return idx;
   }

   @Override
   public int setUnilateralForces (VectorNd the, double s, int idx) {
      idx = setUnilateralForces (the.getBuffer(), s, idx);
      return idx;
   }

   int getUnilateralForces (double[] buf, int idx) {
      for (int i=0; i<myUnilaterals.size(); i++) {
         buf[idx++] = myUnilaterals.get(i).getForce();
      }
      return idx;
   }

   @Override
   public int getUnilateralForces (VectorNd the, int idx) {
      return getUnilateralForces (the.getBuffer(), idx);
   }

   public int maxFrictionConstraintSets() {
      return myBilaterals0.size() + myBilaterals1.size() + myUnilaterals.size();
   }

   private static double ftol = 1e-2;

   @Override
   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      double mu = myBehavior.myFriction;
      for (ContactConstraint c : getBilaterals0()) {
         numf = c.add1DFrictionConstraints (DT, finfo, mu, numf);
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         numf = c.add1DFrictionConstraints (DT, finfo, mu, numf);
      }
      for (int i=0; i<myUnilaterals.size(); i++) {
         ContactConstraint c = myUnilaterals.get(i);
         if (Math.abs(c.getForce())*mu < ftol) {
            continue;
         }
         numf = c.add2DFrictionConstraints (DT, finfo, mu, numf);
      }
      return numf;
   }

   public int numBilateralConstraints() {
      return myBilaterals0.size() + myBilaterals1.size();
   }

   public int numUnilateralConstraints() {
      return myUnilaterals.size();
   }

   /** 
    * {@inheritDoc}
    */
   public void getState (DataBuffer data) {

      data.zput (myBilaterals0.size());
      data.zput (myBilaterals1.size());
      data.zput (myUnilaterals.size());

      int numb = myBilaterals0.size() + myBilaterals1.size();
      int numu = myUnilaterals.size();

      for (ContactConstraint c : myBilaterals0.values()) {
         c.getState (data);
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.getState (data);
      }
      for (int i=0; i<myUnilaterals.size(); i++) {
         myUnilaterals.get(i).getState (data);
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
      int numb1 = data.zget();
      int numu = data.zget();

      for (int i=0; i<numb0; i++) {
         ContactConstraint c = new ContactConstraint();
         c.setState (data, myCollidable0, myCollidable1);
         putContact (myBilaterals0, c);
      }        
      for (int i=0; i<numb1; i++) {
         ContactConstraint c = new ContactConstraint();
         c.setState (data, myCollidable1, myCollidable0);
         putContact (myBilaterals1, c);
      }        
      for (int i=0; i<numu; i++) {
         ContactConstraint c = new ContactConstraint();
         c.setState (data, myCollidable0, myCollidable1);
         myUnilaterals.add (c);
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
      // contact constraints in both myBilaterals0 and myBilaterals1. The
      // associated vertices are stored either in cpnt0 or cpnt1.  For
      // myBilaterals0, cpnt0 and cpnt1 store the vertices associated
      // myCollidable0 and myCollidable1, respectively. The reverse is true for
      // myBilaterals1. Cpnt0 or cpnt1 are then used depending on whether col
      // equals myCollidable0 or myCollidable1. When cpnt1 is used, the scalar
      // force is negated since in that case the normal is oriented for the
      // opposite body.
      
      for (ContactConstraint c : getBilaterals0()) {
         if (c.myCpnt0.isOnCollidable (colA)) {            
            accumulateForces (
               map, c.myCpnt0, c.getNormal(), c.getForce());
         }
         else {
            accumulateForces (
               map, c.myCpnt1, c.getNormal(), -c.getForce());
         }
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         if (c.myCpnt0.isOnCollidable (colA)) {            
            accumulateForces (
               map, c.myCpnt0, c.getNormal(), c.getForce());
         }
         else {
            accumulateForces (
               map, c.myCpnt1, c.getNormal(), -c.getForce());
         }
      }
      // added by Fabien Pean, March 28, 2017
      for (ContactConstraint c : myUnilaterals) {
         if (c.myCpnt0.isOnCollidable (colA)) {            
            accumulateForces (
               map, c.myCpnt0, c.getNormal(), c.getForce());
         }
         else {
            accumulateForces (
               map, c.myCpnt1, c.getNormal(), -c.getForce());
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

   /* ===== End Render methods ===== */
   
}
