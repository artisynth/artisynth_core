/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;

import javax.media.opengl.GL2;

import maspack.collision.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.RenderProps.Shading;
import maspack.util.*;

import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.modelbase.*;

public class CollisionHandler extends ConstrainerBase 
   implements HasRenderProps, GLRenderable {

   public enum Method {
      VERTEX_PENETRATION,
      VERTEX_EDGE_PENETRATION,
      CONTOUR_REGION
   }

   SignedDistanceCollider mySDCollider;
   LinkedHashMap<ContactPoint,ContactConstraint> myBilaterals0;
   LinkedHashMap<ContactPoint,ContactConstraint> myBilaterals1;
   ArrayList<ContactConstraint> myUnilaterals;
   int myMaxUnilaterals = 100;

   CollisionManager myManager;
   CollidableBody myCollidable0;
   CollidableBody myCollidable1;
   PolygonalMesh myMesh0;
   PolygonalMesh myMesh1;
   AbstractCollider myCollider;

   double myFriction;
   double myPenetrationTol = 0.001;
   double myCompliance = 0;
   double myDamping = 0;

   boolean myDrawIntersectionContours = false;
   boolean myDrawIntersectionFaces = false;
   boolean myDrawIntersectionPoints = false;

   ContactInfo myLastContactInfo; // last contact info produced by this handler
   ContactInfo myRenderContactInfo; // contact info to be used for rendering

   HashSet<Vertex3d> myAttachedVertices0 = null;
   HashSet<Vertex3d> myAttachedVertices1 = null;
   boolean myAttachedVerticesValid = false;

   CollisionHandler.Method myMethod = CollisionHandler.myDefaultMethod;
   public static boolean useSignedDistanceCollider = false;
   
   public static boolean doBodyFaceContact = false;
   public static boolean computeTimings = false;
   
   public boolean reduceConstraints = false;

   public static Method myDefaultMethod = Method.VERTEX_PENETRATION;

   public static PropertyList myProps =
      new PropertyList (CollisionHandler.class, ConstrainerBase.class);

   static private RenderProps defaultRenderProps = new RenderProps();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   double getContactNormalLen() {
      if (myManager != null) {
         return myManager.getContactNormalLen();
      }
      else {
         return 0;
      }
      // ModelComponent ancestor = getGrandParent();
      // if (ancestor instanceof CollisionManager) {
      //    return ((CollisionManager)ancestor).getContactNormalLen();
      // }
      // else {
      //    return 0;
      // }
   }

   void setLastContactInfo(ContactInfo info) {
      myLastContactInfo = info;
   }

   /**
    * Returns the coefficient of friction for this collision pair.
    */
   public double getFriction() {
      return myFriction;
   }

   /**
    * Sets the coeffcient of friction for this collision pair.
    */
   public void setFriction (double mu) {
      myFriction = mu;
   }

   public double getPenetrationTol () {
      return myPenetrationTol;
   }

   public void setPenetrationTol (double tol) {
      myPenetrationTol = tol;
   }

   public void setCompliance (double c) {
      myCompliance = c;
   }

   public double getCompliance() {
      return myCompliance;
   }

   public void setDamping (double d) {
      myDamping = d;
   }

   public double getDamping() {
      return myDamping;
   }

   public void setRigidPointTolerance (double tol) {
      // XXX should we always set this?
      if (myMethod == CollisionHandler.Method.CONTOUR_REGION) {
         myCollider.setPointTolerance (tol);
      }
   }

   public double getRigidPointTolerance() {
      // XXX should we always set this?
      if (myMethod == CollisionHandler.Method.CONTOUR_REGION) {
         return myCollider.getPointTolerance();
      }
      else {
         return -1;
      }
   }

   public void setRigidRegionTolerance (double tol) {
      // XXX should we always set this?
      if (myMethod == CollisionHandler.Method.CONTOUR_REGION) {
         myCollider.setRegionTolerance (tol);
      }
   }

   public double getRigidRegionTolerance() {
      // XXX should we always set this?
      if (myMethod == CollisionHandler.Method.CONTOUR_REGION) {
         return myCollider.getRegionTolerance();
      }
      else {
         return -1;
      }
   }

   protected boolean isRigid (CollidableBody col) {
      return (col instanceof RigidBody || col instanceof RigidMesh);
   }

   public void setDrawIntersectionContours (boolean set) {
      myDrawIntersectionContours = set;
   }

   public boolean isDrawIntersectionContours() {
      return myDrawIntersectionContours;
   }
   
   public void setDrawIntersectionFaces (boolean set) {
      myDrawIntersectionFaces = set;
   }

   public boolean isDrawIntersectionFaces() {
      return myDrawIntersectionFaces;
   }

   public void setDrawIntersectionPoints (boolean set) {
      myDrawIntersectionPoints = set;
   }

   public boolean isDrawIntersectionPoints() {
      return myDrawIntersectionPoints;
   }

   public boolean isReduceConstraints() {
      return reduceConstraints;
   }
   
   public void setReduceConstraints(boolean set) {
      reduceConstraints = set;
   }

   public CollisionHandler (CollisionManager manager) {
      myBilaterals0 = new LinkedHashMap<ContactPoint,ContactConstraint>();
      myBilaterals1 = new LinkedHashMap<ContactPoint,ContactConstraint>();
      myUnilaterals = new ArrayList<ContactConstraint>();
      myCollider = SurfaceMeshCollider.newCollider();
      myManager = manager;
   }

   public CollisionHandler (
      CollisionManager manager,
      CollidableBody col0, CollidableBody col1, double mu) {

      this (manager);
      myCollidable0 = col0;
      myCollidable1 = col1;
      if (isRigid(col0) && isRigid(col1)) {
         myMethod = CollisionHandler.Method.CONTOUR_REGION;
      }
      else if (col0 instanceof RigidBody) {
         myCollidable0 = col1;
         myCollidable1 = col0;        
      }
      setFriction (mu);
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
      cons = contacts.get (hashUsingFace ? cpnt1 : cpnt0);

      if (cons == null) {
         cons = new ContactConstraint (this, cpnt0, cpnt1);
         cons.myIdentifyByPoint1 = hashUsingFace;
         putContact (contacts, cons);
         return cons;
      }
      else { // contact already exists
         double lam = cons.getImpulse();
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

   public double computeCollisionConstraints (double t) {

      clearRenderData();

      myMesh0 = myCollidable0.getCollisionMesh();
      myMesh1 = myCollidable1.getCollisionMesh();

      boolean needContours = (myMethod==CollisionHandler.Method.CONTOUR_REGION);
      ContactInfo info = myCollider.getContacts (myMesh0, myMesh1, needContours);
      double maxpen;
      switch (myMethod) {
         case VERTEX_PENETRATION: 
         case VERTEX_EDGE_PENETRATION: {
            maxpen = computeVertexPenetrationConstraints (
               info, myCollidable0, myCollidable1);
            break;
         }
         case CONTOUR_REGION: {
            maxpen = computeContourRegionConstraints (
               info, myCollidable0, myCollidable1);
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented collision method: "+myMethod);
         }
      }
      myLastContactInfo = info;
      return maxpen;
   }

   Collidable getCollidable (ContactPenetratingPoint cpp) {
      if (cpp.vertex.getMesh() == myMesh0) {
         return myCollidable0;
      }
      else {
         return myCollidable1;
      }
   }

   double setVertexFace (
      ContactConstraint cons, ContactPenetratingPoint cpp,
      CollidableBody collidable0, CollidableBody collidable1) {

      //cons.setContactPoint2 (cpp.position, cpp.face, cpp.coords);
      cpp.face.computeNormal (cons.myNormal);
      PolygonalMesh mesh = collidable1.getCollisionMesh();
      // convert to world coordinates if necessary
      if (!mesh.meshToWorldIsIdentity()) {
         cons.myNormal.transform (mesh.getMeshToWorld());
      }
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
      return (hasLowDOF (collidable0) &&
              mesh0.getNumVertices() > mesh1.getNumVertices());
   }

   protected boolean isCompletelyAttached (
      DynamicComponent comp, CollidableBody collidable) {
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
            if (mcomp == null || !collidable.containsContactMaster (mcomp)) {
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
      collidable0.getVertexMasters (masters, vtx);
      // vertex is considered attached if 
      // (a) all masters are completely attached to collidable1, or
      // (b) all masters are actually contained in collidable1
      for (int i=0; i<masters.size(); i++) {
         DynamicComponent comp = masters.get(i).myComp;
         if (!isCompletelyAttached (comp, collidable1) &&
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
//         System.out.println (
//            ComponentUtils.getPathName(myCollidable0) + " " +
//            ComponentUtils.getPathName(myCollidable1));
//         System.out.println (
//            "num attached0: " + 
//            (myAttachedVertices0 != null ? myAttachedVertices0.size() : 0));
//         System.out.println (
//            "num attached1: " + 
//            (myAttachedVertices1 != null ? myAttachedVertices1.size() : 0));
      }
   }

   double computeEdgePenetrationConstraints (
      ArrayList<EdgeEdgeContact> eecs,
      CollidableBody collidable0, CollidableBody collidable1) {

      double maxpen = 0;

      for (EdgeEdgeContact eec : eecs) {
         // Check if the contact has already been corrected by other contact
         // corrections.
         if (eec.calculate()) {

            ContactPoint pnt0, pnt1;
            pnt0 = new ContactPoint (eec.point0, eec.edge0, eec.s0);
            pnt1 = new ContactPoint (eec.point1, eec.edge1, eec.s1);

            ContactConstraint cons = getContact (
               myBilaterals0, pnt0, pnt1, false, eec.displacement);
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
         }
      }
      return maxpen;
   }

   double computeVertexPenetrationConstraints (
      ArrayList<ContactPenetratingPoint> points,
      CollidableBody collidable0, CollidableBody collidable1) {

      double nrmlLen = getContactNormalLen();
      double maxpen = 0;
      Vector3d normal = new Vector3d();
      boolean hashUsingFace = hashContactUsingFace (collidable0, collidable1);

      updateAttachedVertices();
      for (ContactPenetratingPoint cpp : points) {

         ContactPoint pnt0, pnt1;
         pnt0 = new ContactPoint (cpp.vertex);
         pnt1 = new ContactPoint (cpp.position, cpp.face, cpp.coords);
         
         // TODO: finish this ...
         if (!collidable0.allowCollision (
                pnt0, collidable1, myAttachedVertices0) ||
             !collidable1.allowCollision (
                pnt1, collidable0, myAttachedVertices1)) {
            continue;
         }

         ContactConstraint cons;
         if (collidable0 == myCollidable0) {
            cons = getContact (
               myBilaterals0, pnt0, pnt1, hashUsingFace, cpp.distance);
         }
         else {
            cons = getContact (
               myBilaterals1, pnt0, pnt1, hashUsingFace, cpp.distance);
         }
         
         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null) {
            cons.setActive (true);

            double dist = setVertexFace (cons, cpp, collidable0, collidable1);
            if (!cons.isControllable()) {
               cons.setActive (false);
               continue;
            }
            cons.setDistance (dist);

            if (nrmlLen != 0) {
               // compute normal from scratch because previous contacts
               // may have caused it to change
               cpp.face.computeNormal (normal);
               PolygonalMesh mesh1 = collidable1.getCollisionMesh();
               if (!mesh1.meshToWorldIsIdentity()) {
                  normal.transform (mesh1.getMeshToWorld());
               }
               addLineSegment (cpp.position, normal, nrmlLen);
            }
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

   boolean hasLowDOF (CollidableBody collidable) {
      // XXX should formalize this better
      return isRigid (collidable);
   }

   double computeVertexPenetrationConstraints (
      ContactInfo info, CollidableBody collidable0, CollidableBody collidable1) {
      double maxpen = 0;
      clearContactActivity();
      if (info != null) {
         maxpen = computeVertexPenetrationConstraints (
            info.points0, collidable0, collidable1);
         if (!hasLowDOF (collidable1) || doBodyFaceContact) {
            double pen = computeVertexPenetrationConstraints (
               info.points1, collidable1, collidable0);
            if (pen > maxpen) {
               maxpen = pen;
            }
         }
         if (myMethod == CollisionHandler.Method.VERTEX_EDGE_PENETRATION) {
            double pen = computeEdgePenetrationConstraints (
               info.edgeEdgeContacts, collidable0, collidable1);
            if (pen > maxpen) {
               maxpen = pen;
            }
         }
      }
      removeInactiveContacts();
      return maxpen;
   }

   double computeContourRegionConstraints (
      ContactInfo info, CollidableBody collidable0, CollidableBody collidable1) {

      myUnilaterals.clear();
      double maxpen = 0;

      double nrmlLen = getContactNormalLen();
      clearRenderData();

      // Currently, no correspondence is established between new contacts and
      // previous contacts. If there was, then we could set the multipliers for
      // the new contacts to something based on the old contacts, thereby
      // providing initial "warm start" values for the solver.
      int numc = 0;
      if (info != null) {

         for (ContactRegion region : info.regions) {
            for (Point3d p : region.points) {
               if (numc >= myMaxUnilaterals)
                  break;

               ContactConstraint c = new ContactConstraint(this);
               c.setContactPoint0 (p);
               c.equateContactPoints();
               c.setNormal (region.normal);
               c.assignMasters (collidable0, collidable1);

               if (nrmlLen != 0) {
                  addLineSegment (p, region.normal, nrmlLen);
               }

               maxpen = region.depth;
               c.setDistance (-region.depth);
               myUnilaterals.add (c);
               numc++;
            }
         }
      }
      setLastContactInfo(info);
      return maxpen;
   }

   void clearContactData() {
      myBilaterals0.clear();
      myBilaterals1.clear();
      myUnilaterals.clear();
   }

   public void clearContactActivity() {
      for (ContactConstraint c : myBilaterals0.values()) {
         c.setActive (false);
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.setActive (false);
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

   /** 
    * automatically compute compliance and damping for a given
    * penetration tolerance and acceleration.
    *
    * @param acc acceleration
    * @param tol desired penetration tolerance 
    */
   public void autoComputeCompliance (double acc, double tol) {
      // todo: does getmass() do what we want here?
      double mass = myCollidable0.getMass() + myCollidable1.getMass();
      double dampingRatio = 1;
      myCompliance = tol/(acc*mass);
      myDamping = dampingRatio*2*Math.sqrt(mass/myCompliance);
   }   

   void addCollisionComponent (
      ContactConstraint con, Point3d pnt, Feature feat) {
   }

   // begin constrainer implementation

   public double updateConstraints (double t, int flags) {
      if ((flags & MechSystem.COMPUTE_CONTACTS) != 0) {
         return computeCollisionConstraints (t);
      }
      else if ((flags & MechSystem.UPDATE_CONTACTS) != 0) {
         // right now just leave the same contacts in place ...
         return 0;
      }
      else {
         return 0;
      }
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
      list.addAll (myBilaterals0.values());
      list.addAll (myBilaterals1.values());
   }

   @Override
   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      double[] dbuf = (dg != null ? dg.getBuffer() : null);

      for (ContactConstraint c : myBilaterals0.values()) {
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

   @Override
   public int getBilateralInfo(ConstraintInfo[] ginfo, int idx) {
      for (ContactConstraint c : myBilaterals0.values()) {
         c.setSolveIndex (idx);
         ConstraintInfo gi = ginfo[idx++];
         if (c.getDistance() < -myPenetrationTol) {
            gi.dist = (c.getDistance() + myPenetrationTol);
         }
         else {
            gi.dist = 0;
         }
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.setSolveIndex (idx);
         ConstraintInfo gi = ginfo[idx++];
         if (c.getDistance() < -myPenetrationTol) {
            gi.dist = (c.getDistance() + myPenetrationTol);
         }
         else {
            gi.dist = 0;
         }
      }
      return idx;
   }

   @Override
   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      for (ContactConstraint c : myBilaterals0.values()) {
         c.setImpulse (lam.get (idx++));
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.setImpulse (lam.get (idx++));
      }
      return idx;
   }

   @Override
   public int getBilateralImpulses (VectorNd lam, int idx) {
      for (ContactConstraint c : myBilaterals0.values()) {
         lam.set (idx++, c.getImpulse());
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         lam.set (idx++, c.getImpulse());
      }
      return idx;
   }

   @Override
   public void zeroImpulses() {
      for (ContactConstraint c : myBilaterals0.values()) {
         c.setImpulse (0);
      }   
      for (ContactConstraint c : myBilaterals1.values()) {
         c.setImpulse (0);
      }   
      for (int i=0; i<myUnilaterals.size(); i++) {
         myUnilaterals.get(i).setImpulse (0);
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

     for (int i=0; i<myUnilaterals.size(); i++) {
         ContactConstraint c = myUnilaterals.get(i);
         c.setSolveIndex (idx);
         ConstraintInfo ni = ninfo[idx++];
         if (c.getDistance() < -myPenetrationTol) {
            ni.dist = (c.getDistance() + myPenetrationTol);
         }
         else {
            ni.dist = 0;
         }
         ni.compliance = getCompliance();
         ni.damping = getDamping();
      }
      return idx;
   }

   @Override
   public int setUnilateralImpulses (VectorNd the, double h, int idx) {
      double[] buf = the.getBuffer();
      for (int i=0; i<myUnilaterals.size(); i++) {
         myUnilaterals.get(i).setImpulse (buf[idx++]);
      }
      return idx;
   }

   @Override
   public int getUnilateralImpulses (VectorNd the, int idx) {
      double[] buf = the.getBuffer();
      for (int i=0; i<myUnilaterals.size(); i++) {
         buf[idx++] = myUnilaterals.get(i).getImpulse();
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      return myBilaterals0.size() + myBilaterals1.size() + myUnilaterals.size();
   }

   @Override
   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

      for (ContactConstraint c : myBilaterals0.values()) {
         numf = c.add1DFrictionConstraints (DT, finfo, myFriction, numf);
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         numf = c.add1DFrictionConstraints (DT, finfo, myFriction, numf);
      }
      for (int i=0; i<myUnilaterals.size(); i++) {
         ContactConstraint c = myUnilaterals.get(i);
         if (Math.abs(c.getImpulse())*myFriction < 1e-4) {
            continue;
         }
         numf = c.add2DFrictionConstraints (DT, finfo, myFriction, numf);
      }
      return numf;
   }

   /** 
    * {@inheritDoc}
    */
   public void skipAuxState (DataBuffer data) {
      int numb = data.zget();
      int numu = data.zget();
      for (int i=0; i<numu+numb; i++) {
         ContactConstraint.skipState (data);
      }
   }

   /** 
    * {@inheritDoc}
    */
   public void getAuxState (DataBuffer data) {

      data.zput (myBilaterals0.size());
      data.zput (myBilaterals1.size());
      data.zput (myUnilaterals.size());
      for (ContactConstraint c : myBilaterals0.values()) {
         c.getState (data);
      }
      for (ContactConstraint c : myBilaterals1.values()) {
         c.getState (data);
      }
      for (int i=0; i<myUnilaterals.size(); i++) {
         myUnilaterals.get(i).getState (data);
      }
   }

   /** 
    * {@inheritDoc}
    */
   public void setAuxState (DataBuffer data) {

      clearContactData();
      int numb0 = data.zget();
      int numb1 = data.zget();
      int numu = data.zget();
      for (int i=0; i<numb0; i++) {
         ContactConstraint c = new ContactConstraint(this);
         c.setState (data, myCollidable0, myCollidable1);
         putContact (myBilaterals0, c);
      }        
      for (int i=0; i<numb1; i++) {
         ContactConstraint c = new ContactConstraint(this);
         c.setState (data, myCollidable1, myCollidable0);
         putContact (myBilaterals1, c);
      }        
      for (int i=0; i<numu; i++) {
         ContactConstraint c = new ContactConstraint(this);
         c.setState (data, myCollidable0, myCollidable1);
         myUnilaterals.add (c);
      }        
   }

   /** 
    * {@inheritDoc}
    */
   public void getInitialAuxState (DataBuffer newData, DataBuffer oldData) {

      // just create a state in which there are no contacts
      newData.zput (0); // no bilaterals
      newData.zput (0); // no bilaterals
      newData.zput (0); // no unilaterals
   }

   /* ===== Begin Render methods ===== */

   private class FaceSeg {
      Face face;
      Point3d p0;
      Point3d p1;
      Point3d p2;
      Vector3d nrm;

      public FaceSeg(Face face) {
         this.face = face;
         nrm = face.getWorldNormal();
         HalfEdge he = face.firstHalfEdge();
         p0 = he.head.getWorldPoint();
         he = he.getNext();
         p1 = he.head.getWorldPoint();
         he = he.getNext();
         p2 = he.head.getWorldPoint();
         he = he.getNext();
      }
   }

   private class LineSeg {
      float[] coords0;
      float[] coords1;

      public LineSeg (Point3d pnt0, Vector3d nrm, double len) {
         coords0 = new float[3];
         coords1 = new float[3];

         coords0[0] = (float)pnt0.x;
         coords0[1] = (float)pnt0.y;
         coords0[2] = (float)pnt0.z;

         coords1[0] = (float)(pnt0.x + len*nrm.x);
         coords1[1] = (float)(pnt0.y + len*nrm.y);
         coords1[2] = (float)(pnt0.z + len*nrm.z);
      }
   }

   private ArrayList<LineSeg> myLineSegments;
   private ArrayList<LineSeg> myRenderSegments; // for rendering
   private ArrayList<FaceSeg> myFaceSegments; 
   private ArrayList<FaceSeg> myRenderFaces; // for rendering

   void clearRenderData() {
      myLineSegments = new ArrayList<LineSeg>();
      myFaceSegments = null;
   }

   void addLineSegment (Point3d pnt0, Vector3d nrm, double len) {
      myLineSegments.add (new LineSeg (pnt0, nrm, len));
   }

   private synchronized void saveRenderData() {
      myRenderSegments = myLineSegments;
      myRenderFaces = myFaceSegments;
      myRenderContactInfo = myLastContactInfo;
   }

   void initialize() {
      myRenderSegments = null;
      myRenderFaces = null;
      myLineSegments = null;
      myLastContactInfo = null;
   }

   public void prerender (RenderList list) {
      if (myDrawIntersectionFaces &&
          myLastContactInfo != null &&
          myFaceSegments == null) {
         myFaceSegments = new ArrayList<FaceSeg>();
         buildFaceSegments(myLastContactInfo, myFaceSegments);
      }
      saveRenderData();

      // if (myMethod == Method.CONTOUR_REGION) {
      //    myRBContact.prerender (list);
      // }

   }

   protected void findInsideFaces (
      Face face, BVFeatureQuery query, PolygonalMesh mesh,
      ArrayList<FaceSeg> faces) {

      face.setVisited();
      Point3d pnt = new Point3d();
      HalfEdge he = face.firstHalfEdge();
      for (int i=0; i<3; i++) {
         if (he.opposite != null) {
            Face oFace = he.opposite.getFace();
            if (!oFace.isVisited()) {
               // check if inside
               oFace.computeWorldCentroid(pnt);

               boolean inside = query.isInsideOrientedMesh(mesh, pnt, -1);
               if (inside) {
                  FaceSeg seg = new FaceSeg(oFace); 
                  faces.add(seg);
                  findInsideFaces(oFace, query, mesh, faces);
               }
            }
         }
         he = he.getNext();
      }

   }

   protected void buildFaceSegments (ContactInfo info, ArrayList<FaceSeg> faces) {

      BVFeatureQuery query = new BVFeatureQuery();

      PolygonalMesh mesh0 = myCollidable0.getCollisionMesh();
      PolygonalMesh mesh1 = myCollidable1.getCollisionMesh();

      // mark faces as visited and add segments
      for (TriTriIntersection isect : info.intersections) {
         isect.face0.setVisited();
         isect.face1.setVisited();

         // add partials?
      }

      // mark interior faces and add segments
      for (TriTriIntersection isect : info.intersections) {
         if (isect.face0.getMesh() != mesh0) {
            findInsideFaces(isect.face0, query, mesh0, faces);
            findInsideFaces(isect.face1, query, mesh1, faces);
         } else {
            findInsideFaces(isect.face0, query, mesh1, faces);
            findInsideFaces(isect.face1, query, mesh0, faces);
         }
      }

      for (TriTriIntersection isect : info.intersections) {
         isect.face0.clearVisited();
         isect.face1.clearVisited();
      }

      // clear visited flag for use next time
      for (FaceSeg seg : faces) {
         seg.face.clearVisited();
      }

   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   public void render (GLRenderer renderer, int flags) {
      render (renderer, myRenderProps, flags);
   }

   // Twist lastmomentumchange = null;

   public void render (GLRenderer renderer, RenderProps props, int flags) {

      GL2 gl = renderer.getGL2();

      ArrayList<LineSeg> renderSegments = null;
      ArrayList<FaceSeg> renderFaces = null;
      ContactInfo renderContactInfo = null;
      
      synchronized (this) {
         renderSegments = myRenderSegments;
         renderFaces = myRenderFaces;
         renderContactInfo = myRenderContactInfo;
      }

      // Magnitude of offset vector to add to rendered contour lines.  The
      // offset is needed because the contours coexist with polygonal surfaces,
      // and rendering the latter would otherwise obscure rendering the former.
      double offsetMag = 1.0*renderer.centerDistancePerPixel();

      renderer.getZDirection();
      Vector3d offDir = new Vector3d(renderer.getZDirection());

      // System.out.println("Z direction" + offDir);
      double scale = offsetMag/offDir.norm();
      offDir.scale(scale);

      //       if (myRigidBodyPairP && myRBContact != null) {
      //          myRBContact.render (renderer);
      //       }
      if (renderSegments != null) {
         for (LineSeg seg : renderSegments) {
            renderer.drawLine (
               props, seg.coords0, seg.coords1, /*selected=*/false);
         }
      }

      if (myDrawIntersectionContours && 
         props.getEdgeWidth() > 0 &&
         renderContactInfo != null) {

         gl.glLineWidth (props.getEdgeWidth());
         float[] rgb = props.getEdgeColorArray();
         if (rgb == null) {
            rgb = props.getLineColorArray();
         }
         renderer.setColor (rgb, false);
         renderer.setLightingEnabled (false);


         // offset lines
         if (renderContactInfo.contours != null) {
            for (MeshIntersectionContour contour : renderContactInfo.contours) {
               gl.glBegin (GL2.GL_LINE_LOOP);
               for (MeshIntersectionPoint p : contour) {
                  gl.glVertex3d (p.x + offDir.x, p.y + offDir.y, p.z + offDir.z);
               }
               gl.glEnd();
            }
         } else if (renderContactInfo.intersections != null){
            // use intersections to render lines
            gl.glBegin(GL2.GL_LINES);
            for (TriTriIntersection tsect : renderContactInfo.intersections) {
               gl.glVertex3d(tsect.points[0].x + offDir.x, 
                  tsect.points[0].y + offDir.y, tsect.points[0].z + offDir.z);
               gl.glVertex3d(tsect.points[1].x + offDir.x,
                  tsect.points[1].y + offDir.y, tsect.points[1].z + offDir.z);
            }
            gl.glEnd();
         }

         renderer.setLightingEnabled (true);
         gl.glLineWidth (1);
      }

      float[] coords = new float[3];
      if (myDrawIntersectionPoints && renderContactInfo != null) {

         if (renderContactInfo.intersections != null) {
            for (TriTriIntersection tsect : renderContactInfo.intersections) {
               for (Point3d pnt : tsect.points) {
                  pnt.get(coords);
                  renderer.drawPoint(props, coords, false);
               }
            }
         }

         if (renderContactInfo.points0 != null) {
            for (ContactPenetratingPoint cpp : renderContactInfo.points0) {
               if (cpp.distance > 0) {
                  cpp.vertex.getWorldPoint().get(coords);
                  renderer.drawPoint(props, coords, false);
                  //cpp.position.get(coords);
                  //renderer.drawPoint(props, coords, false);
               }
            }
         }

         if (renderContactInfo.points1 != null) {
            for (ContactPenetratingPoint cpp : renderContactInfo.points1) {
               if (cpp.distance > 0) {
                  cpp.vertex.getWorldPoint().get(coords);
                  renderer.drawPoint(props, coords, false);
                  // cpp.position.get(coords);
                  // renderer.drawPoint(props, coords, false);
               }
            }
         }

         if (renderContactInfo.edgeEdgeContacts != null) {
            for (EdgeEdgeContact eec : renderContactInfo.edgeEdgeContacts) {
               eec.point0.get(coords);
               renderer.drawPoint(props, coords, false);
               eec.point1.get(coords);
               renderer.drawPoint(props, coords, false);
            }
         }
      }

      if (myDrawIntersectionFaces && renderFaces != null) {
         gl.glPushMatrix();

         Material faceMat = props.getFaceMaterial();
         Shading shading = props.getShading();
         if (shading != Shading.NONE) {
            faceMat.apply (gl, GL2.GL_FRONT_AND_BACK);
            gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
         }

         if (props.getFaceStyle() != RenderProps.Faces.NONE) {
            int[] savedShadeModel = new int[1];
            gl.glGetIntegerv (GL2.GL_SHADE_MODEL, savedShadeModel, 0);

            if (shading == Shading.NONE) {
               renderer.setLightingEnabled (false);
               renderer.setColor (
                  props.getFaceColorArray(), false);
            }
            else if (shading != Shading.FLAT && !renderer.isSelecting()) {
               gl.glShadeModel (GL2.GL_SMOOTH);
            }
            else { // shading == Shading.FLAT
               gl.glShadeModel (GL2.GL_FLAT);
            }

            byte[] savedCullFaceEnabled = new byte[1];
            int[] savedCullFaceMode = new int[1];

            gl.glGetBooleanv (GL2.GL_CULL_FACE, savedCullFaceEnabled, 0);
            gl.glGetIntegerv (GL2.GL_CULL_FACE_MODE, savedCullFaceMode, 0);

            RenderProps.Faces faces = props.getFaceStyle();
            switch (faces) {
               case FRONT_AND_BACK: {
                  gl.glDisable (GL2.GL_CULL_FACE);
                  break;
               }
               case FRONT: {
                  gl.glCullFace (GL2.GL_BACK);
                  break;
               }
               case BACK: {
                  gl.glCullFace (GL2.GL_FRONT);
                  break;
               }
               default:
                  break;
            }

            // offset
            offDir.scale(0.5);   // half as far as lines

            // draw faces
            gl.glBegin(GL2.GL_TRIANGLES);
            for (FaceSeg seg : renderFaces) {
               gl.glNormal3d(seg.nrm.x, seg.nrm.y, seg.nrm.z);
               gl.glVertex3d(
                  seg.p0.x + offDir.x, seg.p0.y + offDir.y, seg.p0.z + offDir.z);
               gl.glVertex3d(
                  seg.p1.x + offDir.x, seg.p1.y + offDir.y, seg.p1.z + offDir.z);
               gl.glVertex3d(
                  seg.p2.x + offDir.x, seg.p2.y + offDir.y, seg.p2.z + offDir.z);
            }
            gl.glEnd();

            if (savedCullFaceEnabled[0] != 0) {
               gl.glEnable (GL2.GL_CULL_FACE);
            }
            else {
               gl.glDisable (GL2.GL_CULL_FACE);
            }
            gl.glCullFace (savedCullFaceMode[0]);


            if (shading == Shading.NONE) {
               renderer.setLightingEnabled (true);
            }
            gl.glShadeModel (savedShadeModel[0]);
         }
         gl.glPopMatrix();
      }

   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      if (myRenderContactInfo != null && myRenderContactInfo.contours != null) {
         for (MeshIntersectionContour contour : myRenderContactInfo.contours) {
            for (MeshIntersectionPoint p : contour) {
               p.updateBounds (pmin, pmax);
            }
         }
      }
      if (myLineSegments != null) {
         Point3d pnt = new Point3d();
         for (LineSeg strip : myLineSegments) {
            pnt.set (strip.coords0[0], strip.coords0[1], strip.coords0[2]);
            pnt.updateBounds (pmin, pmax);
            pnt.set (strip.coords1[0], strip.coords1[1], strip.coords1[2]);
            pnt.updateBounds (pmin, pmax);
         }
      }
   }

   /* ===== End Render methods ===== */
   
}
