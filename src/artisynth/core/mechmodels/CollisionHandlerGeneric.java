/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;

import javax.media.opengl.GL2;

import maspack.collision.AbstractCollider;
import maspack.collision.ContactInfo;
import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.collision.MeshIntersectionContour;
import maspack.collision.MeshIntersectionPoint;
import maspack.collision.SignedDistanceCollider;
import maspack.collision.SurfaceMeshCollider;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TriTriIntersection;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.render.GLRenderer;
import maspack.render.Material;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Shading;
import maspack.util.DataBuffer;
import maspack.util.FunctionTimer;
import maspack.util.IntHolder;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.modelbase.ModelComponent;

public class CollisionHandlerGeneric extends CollisionHandler 
implements Constrainer {

   FunctionTimer ftimer = new FunctionTimer(); 

   private AbstractCollider collider;
   private SignedDistanceCollider mySDCollider;
   private RigidBodyContact myRBContact;

   Collidable myComponent0;
   Collidable myComponent1;

   DeformableCollisionData myDefData0;
   DeformableCollisionData myDefData1;

   RigidBodyCollisionData myRBData0;
   RigidBodyCollisionData myRBData1;

   double renderContacts = 0.7; // length of normal used to render
   boolean drawIntersectionFaces = false;
   boolean drawIntersectionContour = false;
   boolean drawIntersectionPoints = false;

   double myMu = 0;

   private boolean myRigidBodyPairP = false;
   // if true, ignore rigid bodies when doing Gauss-Siedel iterative steps
   public static boolean myIgnoreRBForGS = true;
   public static boolean renderContactNormals = false;
   //XXX public static boolean useSignedDistanceCollider = false;

   ContactInfo myLastContactInfo; // last contact info produced by this handler
   ContactInfo myRenderContactInfo; // contact info to be used for rendering

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

      public FaceSeg(Face face, Point3d p0, Point3d p1, 
         Point3d p2, Vector3d nrm) {
         this.p0 = p0;
         this.p1 = p1;
         this.p2 = p2;
         this.nrm = nrm;
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

   private double getMass (Collidable comp) {
      return comp.getMass();
   }

   /** 
    * Automatically compute compliance and damping for a given
    * penetration tolerance and acceleration.
    *
    * @param acc acceleration
    * @param tol desired penetration tolerance 
    */
   public void autoComputeCompliance (double acc, double tol) {
      double mass = getMass(myComponent0) + getMass(myComponent1);
      double dampingRatio = 1;
      myCompliance = tol/(acc*mass);
      myDamping = dampingRatio*2*Math.sqrt(mass/myCompliance);
   }

   // Begin special code for rendering contact points
   private ArrayList<LineSeg> myLineSegments;
   private ArrayList<LineSeg> myRenderSegments; // for rendering
   private ArrayList<FaceSeg> myFaceSegments; 
   private ArrayList<FaceSeg> myRenderFaces; // for rendering


   void addLineSegment (Point3d pnt0, Vector3d nrm, double len) {
      myLineSegments.add (new LineSeg (pnt0, nrm, len));
   }

   void clearLineSegments() {
      myLineSegments = new ArrayList<LineSeg>();
      myFaceSegments = null;
   }

   private void saveLineSegments() {
      myRenderSegments = myLineSegments;
      myRenderFaces = myFaceSegments;
   }

   void initialize() {
      myRenderSegments = null;
      myRenderFaces = null;
      myLineSegments = null;
      myLastContactInfo = null;
   }

   // End special code for rendering contact points

   // static final Object PRESENT = new Object();

   public void setRigidPointTolerance (double tol) {
      if (isRigidBodyPair()) {
         getRigidBodyContact().collider.setPointTolerance (tol);
      }
   }

   public double getRigidPointTolerance() {
      if (isRigidBodyPair()) {
         return getRigidBodyContact().collider.getPointTolerance();
      }
      else {
         return -1;
      }
   }

   public void setRigidRegionTolerance (double tol) {
      if (isRigidBodyPair()) {
         getRigidBodyContact().collider.setRegionTolerance (tol);
      }
   }

   public double getRigidRegionTolerance() {
      if (isRigidBodyPair()) {
         return getRigidBodyContact().collider.getRegionTolerance();
      }
      else {
         return -1;
      }
   }

   double getContactNormalLen() {
      ModelComponent ancestor = getGrandParent();
      if (ancestor instanceof CollisionManager) {
         return ((CollisionManager)ancestor).getContactNormalLen();
      }
      else {
         return 0;
      }
   }

   private AbstractCollider getCollider() {
      if (collider == null) {
         collider = SurfaceMeshCollider.newCollider();
      }
      return collider;
   }

   private SignedDistanceCollider getSDCollider() {
      if (mySDCollider == null) {
         mySDCollider = new SignedDistanceCollider();
      }
      return mySDCollider;
   }

   /**
    * Returns true if this collision pair is between two rigid bodies.
    */
   public boolean isRigidBodyPair() {
      return myRigidBodyPairP;
   }

   public RigidBodyContact getRigidBodyContact() {
      if (myRBContact == null && myRigidBodyPairP) {
         myRBContact =
            new RigidBodyContact (
               myRBData0.getBody(), myRBData0.getMesh(),
               myRBData1.getBody(), myRBData1.getMesh());
         myRBContact.setFriction (myMu);
         myRBContact.myCollisionHandler = this;
         myRBContact.setPenetrationTol (myPenetrationTol);
      }
      return myRBContact;
   }

   /**
    * Returns the coefficient of friction for this collision pair.
    */
   public double getFriction() {
      return myMu;
   }

   /**
    * Sets the coeffcient of friction for this collision pair.
    */
   public void setFriction (double mu) {
      myMu = mu;
      if (myRBContact != null) {
         myRBContact.setFriction (mu);
      }
      if (myDefData0 != null) {
         myDefData0.setFriction(myMu);
      }
      if (myDefData1 != null) {
         myDefData1.setFriction(myMu);
      }
   }

   private double myPenetrationTol = 0.001;
   private double myCompliance = 0;
   private double myDamping = 0;

   public double getPenetrationTol () {
      return myPenetrationTol;
   }

   public void setPenetrationTol (double tol) {
      myPenetrationTol = tol;
      if (myRBContact != null) {
         myRBContact.setPenetrationTol (tol);
      }
      if (myDefData0 != null) {
         myDefData0.setPenetrationTol(tol);
      }
      if (myDefData1 != null) {
         myDefData1.setPenetrationTol(tol);
      }
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

   CollisionHandlerGeneric (Collidable c0, Collidable c1) {
      this (c0, c1, 0);
   }

   CollisionHandlerGeneric (Collidable c0, Collidable c1, double fc) {

      myComponent0 = c0;
      myComponent1 = c1;
      myMu = fc;

      CollisionData cd0 = myComponent0.createCollisionData();
      CollisionData cd1 = myComponent1.createCollisionData();

      if (cd0 instanceof DeformableCollisionData) {
         myRBData0 = null;
         myDefData0 = (DeformableCollisionData)cd0;
         myDefData0.setFriction(myMu);
         myDefData0.setPenetrationTol(myPenetrationTol);
      } else if (cd0 instanceof RigidBodyCollisionData) {
         myRBData0 = (RigidBodyCollisionData)cd0;
         myDefData0 = null;
      }

      if (cd1 instanceof DeformableCollisionData) {
         myRBData1 = null;
         myDefData1 = (DeformableCollisionData)cd1;
         myDefData1.setFriction(myMu);
         myDefData1.setPenetrationTol(myPenetrationTol);
      } else if (cd1 instanceof RigidBodyCollisionData) {
         myRBData1 = (RigidBodyCollisionData)cd1;
         myDefData1 = null;
      } 

      if (myRBData0 != null && myRBData1 != null) {
         myRigidBodyPairP = true;
      }
   }

   public boolean equals (Object obj) {
      if (obj instanceof CollisionHandler) {
         CollisionHandler other = (CollisionHandler)obj;
         return ((myComponent0 == other.getCollidable0() &&
            myComponent1 == other.getCollidable1()) ||
            (myComponent1 == other.getCollidable0() &&
            myComponent0 == other.getCollidable1()));
      }
      else {
         return false;
      }
   }

   public Collidable getCollidable0() {
      return myComponent0;
   }

   public Collidable getCollidable1() {
      return myComponent1;
   }

   public Collidable getCollidable(int idx) {
      if (idx == 0) {
         return myComponent0;
      } else if (idx == 1){
         return myComponent1;
      }
      throw new IllegalArgumentException("Only has two collidables");
   }

   public boolean equals (Collidable c0, Collidable c1) {
      if ((myComponent0 == c0 && myComponent1 == c1) ||
         (myComponent0 == c1 && myComponent1 == c0)) {
         return true;
      }
      else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return myComponent0.hashCode() / 2 + myComponent1.hashCode() / 2;
   }

   public void updateFrictionConstraints () {
      if (myDefData0 != null) {
         myDefData0.updateFrictionConstraints();
      }
      if (myDefData1 != null) {
         myDefData1.updateFrictionConstraints();
      }
   }

   /**
    * Compute contacts and approximately correct interpenetrations for this
    * collision pair. Contacts will be assigned constraints, which will be used
    * later to constrain velocities and compute friction.
    * 
    * <p>
    * This method currently handles only deformable-deformable and
    * deformable-rigidBody collisions; rigidBody-rigidBody collisions are
    * handled separately.
    * 
    * @param t
    * current time (not currently used)
    */
   public double projectPosConstraints (double t) {
      double maxpen = 0;

      Collidable a = null, b = null;
      if ((myRBData0 != null && myDefData1 != null) ||
         (myDefData0 != null && myRBData1 != null)) {

         if (myRBData0 != null) {
            maxpen = projectPosRbDef (myRBData0, myDefData1);
            a = myRBData0.getComponent();
            b = myDefData1.getComponent();
         }
         else {
            maxpen = projectPosRbDef (myRBData1, myDefData0);
            a = myRBData1.getComponent();
            b = myDefData0.getComponent();
         }
      }
      else if (myDefData0 != null && myDefData1 != null) {
         maxpen = projectPosDefDef (myDefData0, myDefData1);
         a = myDefData0.getComponent();
         b = myDefData1.getComponent();
      }

      //System.out.println("Num contacts (" + a.getName()+ ", " 
      //   + b.getName()+ "): " + myLineSegments.size());

      myFaceSegments = null;  // clear

      return maxpen;

   }

   /**
    * Implements projectPosConstraints for the deformable-rigidBody case.
    */
   private double projectPosRbDef (RigidBodyCollisionData rbData, DeformableCollisionData defData) {

      PolygonalMesh meshrb = rbData.getMesh();
      PolygonalMesh meshdef = defData.getMesh();
      double maxpen = 0;

      // deactivate constraints; these may be reactivated later
      clearLineSegments();
      defData.clearContactActivity();

      // get contacts
      ContactInfo info;
      if (useSignedDistanceCollider) {
         info = getSDCollider().getContacts (meshrb, meshdef, false);
      }
      else {
         info = getCollider().getContacts (meshrb, meshdef, false);
      }

      if (info != null) {
         if (computeTimings) {
            ftimer.reset();
            ftimer.start();
         }
         maxpen = createRbDefContacts (
            rbData, info.points0, defData, info.points1, info.edgeEdgeContacts);
         if (computeTimings) {
            ftimer.stop();
            double t = ftimer.getTimeUsec();
            System.out.println("Function took " + t + " usec");
         }
      }
      myLastContactInfo = info;
      // remove contacts which are still inactive
      defData.removeInactiveContacts();
      return maxpen;
   }

   public boolean hasActiveContacts() {
      if (myDefData0 != null && myDefData0.hasActiveContacts()) {
         return true;
      }
      if (myDefData1 != null && myDefData1.hasActiveContacts()) {
         return true;
      }
      if (isRigidBodyPair() && myRBContact != null) {
         return myRBContact.hasActiveContact();
      }
      return false;
   }

   /**
    * Implements projectPosConstraints for the deformable-deformable case.
    */
   private double projectPosDefDef (
      DeformableCollisionData defData0, DeformableCollisionData defData1) {

      PolygonalMesh mesh0 = defData0.getMesh();
      PolygonalMesh mesh1 = defData1.getMesh();
      double maxpen = 0;

      // deactivate constraints; these may be reactivated later
      clearLineSegments();
      defData0.clearContactActivity();
      defData1.clearContactActivity();

      //      RigidBodyContact.checkMesh (mesh0);
      //      RigidBodyContact.checkMesh (mesh1);

      // get contacts
      ContactInfo info = getCollider().getContacts (mesh0, mesh1, false);

      if (info != null) {
         double max;
         //checkRegionPoints ("mesh0", info.points0);
         //checkRegionPoints ("mesh1", info.points1);
         max = createDefDefContacts (info.points0, defData0, info.points1, defData1);
         if (max > maxpen) {
            maxpen = max;
         }
         //         max = createDefDefContacts (info.points1, defData1, info.points0, defData0);
         //         if (max > maxpen) {
         //            maxpen = max;
         //         }
         // createEdgeEdgeConstraints(info.edgeEdgeContacts, correctPos);
      }

      myLastContactInfo = info;
      // remove contacts which are still inactive
      defData0.removeInactiveContacts();
      defData1.removeInactiveContacts();
      return maxpen;
   }

   /** 
    * Returns true if a contact has sufficient DOF for enforcement.  This means
    * that it must be directly or indirectly associated with at least one
    * active component.
    */
   private boolean contactHasDOF (DeformableContactConstraint cons) {
      return cons.isControllable();
   }

   void createEdgeEdgeConstraints (
      ArrayList<EdgeEdgeContact> eecs, DeformableCollisionData defData0,
      DeformableCollisionData defData1) {

      for (EdgeEdgeContact eec : eecs) {
         if (eec.calculate()) { // Check if the contact has already been
            // corrected by other contact corrections.

            DeformableContactConstraint cons = defData0.getContact (eec, true);
            // As long as the constraint exists and is not already marked 
            // as active, then we add it
            if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < eec.displacement ) )) {
               cons.setEdgeEdge (eec, myMu, defData0, defData1);               
               if (contactHasDOF (cons)) {
                  activateContact (
                     cons, -eec.displacement, myDefData0);
               }
            }
         }
      }
   }

   /**
    * Create the constraints, and apply one pass of interpenetration resolution,
    * for the contacts associated with one body of a deformable-deformable
    * contact.
    */
   private double createDefDefContacts (
      ArrayList<ContactPenetratingPoint> points, DeformableCollisionData data,
      ArrayList<ContactPenetratingPoint> otherPoints,  DeformableCollisionData otherData) {

      Vector3d normal = new Vector3d();
      double nrmlLen = getContactNormalLen();
      double maxpen = 0;

      if (reduceConstraints) {
         data.reduceConstraints(points, otherPoints, otherData);
      }

      for (ContactPenetratingPoint cpp : points) {
         if (!data.allowCollision(cpp, /*vertex*/ true, otherData)) {
            continue;
         } 

         // get or allocate a new contact
         DeformableContactConstraint cons = data.getContact (cpp, true);
         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < cpp.distance ) )) {

            double dist = cons.setVertexDeformable(cpp, myMu, data, otherData);
            //          System.out.println("dist = " + dist);

            if (contactHasDOF (cons)) {

               if (nrmlLen != 0) {
                  // compute normal from scratch because previous contacts
                  // may have caused it to change
                  cpp.face.computeNormal (normal);
                  addLineSegment (cpp.position, normal, nrmlLen);
               }

               activateContact (cons, dist, data);
               if (-dist > maxpen) {
                  maxpen = -dist;
               }
            }
         }
      }

      for (ContactPenetratingPoint cpp : otherPoints) {
         if (!otherData.allowCollision(cpp, /*vertex*/ true, data)) {
            continue;
         } 

         // get or allocate a new contact
         DeformableContactConstraint cons = otherData.getContact (cpp, true);
         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < cpp.distance ) )) {

            double dist = cons.setVertexDeformable(cpp, myMu, otherData, data);
            //          System.out.println("dist = " + dist);

            if (contactHasDOF (cons)) {
               if (nrmlLen != 0) {
                  // compute normal from scratch because previous contacts
                  // may have caused it to change
                  cpp.face.computeNormal (normal);
                  addLineSegment (cpp.position, normal, nrmlLen);
               }

               activateContact (cons, dist, otherData);
               if (-dist > maxpen) {
                  maxpen = -dist;
               }
            }
         }
      }

      // System.out.println("maxpen = " + maxpen);
      return maxpen;
   }

   public void activateContact (
      DeformableContactConstraint cons, double dist, DeformableCollisionData data) {

      if (!cons.isAdded()) { // new constraint, so we need to add it
         data.addContact (cons);
         cons.setAdded(true);
         cons.setActive(true);
      }
      else { // mark constraint as active again
         cons.setActive (true);
      }
      if (cons.componentsChanged()) {
         data.notifyContactsChanged();
      }
      cons.setDistance (dist);
      // cons.print(System.out);
   }

   private static final ArrayList<ContactPenetratingPoint> emptyCppList = new ArrayList<ContactPenetratingPoint>(0);
   /**
    * Create the constraints, and apply one pass of interpenetration resolution,
    * for the contacts associated with a deformable-rigidBody contact.
    */
   private double createRbDefContacts (
      RigidBodyCollisionData rbData, ArrayList<ContactPenetratingPoint> pointsRb,
      DeformableCollisionData defData, ArrayList<ContactPenetratingPoint> pointsDef, 
      ArrayList<EdgeEdgeContact> edgeEdgeContacts) { 

      if (reduceConstraints) {
         if (doBodyFaceContact) {
            defData.reduceConstraints(pointsDef, pointsRb, rbData);
         } else {
            defData.reduceConstraints(pointsDef, emptyCppList, rbData);
         }
      }

      Vector3d normal = new Vector3d();
      double nrmlLen = getContactNormalLen();
      double maxpen = 0;

      RigidBody body = rbData.getBody();
      RigidTransform3d XBW = body.getPose(); // body to world transform

      // add active vertex->RB contacts
      for (ContactPenetratingPoint cpp : pointsDef) {

         if (!defData.allowCollision(cpp, true, rbData)) {
            continue;
         }

         // get or allocate a new contact
         DeformableContactConstraint cons = defData.getContact (cpp, true);
         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < cpp.distance ) )) {
            double dist = cons.setVertexRigidBody(cpp, myMu, defData,
               rbData, useSignedDistanceCollider);

            if (contactHasDOF (cons)) {
               if (nrmlLen != 0) {
                  if (useSignedDistanceCollider) {
                     normal.set (cpp.normal);
                  } else {
                     normal.transform (XBW, cpp.face.getNormal());
                  }
                  addLineSegment (cpp.position, normal, nrmlLen);               
               }
               activateContact (cons, dist, defData);
               if (-dist > maxpen) {
                  maxpen = -dist;
               }
            }
         }
      }

      if (doBodyFaceContact) {
         // add active body->face contacts
         for (ContactPenetratingPoint cpp : pointsRb) {
            DeformableContactConstraint cons = defData.getContact (cpp, false);
            if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < cpp.distance ) )) {
               double dist = cons.setFaceRigidBody(cpp, myMu, defData,
                  rbData);
               if (contactHasDOF (cons)) {
                  if (nrmlLen != 0) {
                     if (useSignedDistanceCollider) {
                        normal.set (cpp.normal);
                     } else {
                        normal.transform (XBW, cpp.face.getNormal());
                     }
                     addLineSegment (cpp.position, normal, nrmlLen);               
                  }
                  activateContact (cons, dist, defData);
                  if (-dist > maxpen) {
                     maxpen = -dist;
                  }
               }
            }
         }
      }

      for (EdgeEdgeContact eec : edgeEdgeContacts) {
         if (eec.calculate()) {

            if (!defData.allowCollision(eec, true, rbData)) {
               continue;
            }

            DeformableContactConstraint cons = defData.getContact (eec, true);
            if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < eec.displacement ) )) {
               cons.setEdgeRigidBody(eec, myMu, defData, rbData);
               if (contactHasDOF (cons)) {
                  double dist = -eec.displacement;
                  activateContact (cons, dist, defData);
                  if (-dist > maxpen) {
                     maxpen = -dist;
                  }
               }
            }
         }
      }
      //System.out.println("constrained "+data.myConstraints.size()+" points");
      return maxpen;
   }

   /**
    * Notifies this CollisionPair that the component structure or activity has
    * changes and so the constraint structures will need to be recomputed.
    */
   public void structureOrActivityChanged() {
      if (myDefData0 != null) {
         myDefData0.clearContactData();
      }
      if (myDefData1 != null) {
         myDefData1.clearContactData();
      }
   }

   public void getBilateralSizes (VectorNi sizes) {
      if (myDefData0 != null) {
         myDefData0.getBilateralSizes (sizes);
      }
      if (myDefData1 != null) {
         myDefData1.getBilateralSizes (sizes);
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb, IntHolder changeCnt){
      if (myDefData0 != null) {
         numb = myDefData0.addBilateralConstraints (GT, dg, numb);
         if (myDefData0.contactsHaveChanged()) {
            changeCnt.value++;
         }
      }
      if (myDefData1 != null) {
         numb = myDefData1.addBilateralConstraints (GT, dg, numb);
         if (myDefData1.contactsHaveChanged()) {
            changeCnt.value++;
         }
      }
      // System.out.println ("CollisionPair.addBilaterals " + cnt);
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
      int idx0 = idx;
      if (myDefData0 != null) {
         idx = myDefData0.getBilateralInfo (ginfo, idx);
      }
      if (myDefData1 != null) {
         idx = myDefData1.getBilateralInfo (ginfo, idx);
      }
      for (int i=idx0; i<idx; i++) {
         ginfo[i].compliance = myCompliance;
         ginfo[i].damping = myDamping;
      }
      return idx;
   }

   // public int getBilateralOffsets (
   //    VectorNd Rg, VectorNd bg, int idx, int mode) {

   //    if (myFemData0 != null) {
   //       idx = myFemData0.getBilateralOffsets (Rg, bg, idx, mode);
   //    }
   //    if (myFemData1 != null) {
   //       idx = myFemData1.getBilateralOffsets (Rg, bg, idx, mode);
   //    }
   //    return idx;
   // }

   @Override
   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      if (myDefData0 != null) {
         idx = myDefData0.setBilateralImpulses (lam, idx);
      }
      if (myDefData1 != null) {
         idx = myDefData1.setBilateralImpulses (lam, idx);
      }
      return idx;
   }

   public void zeroImpulses() {
      if (myDefData0 != null) {
         myDefData0.zeroImpulses();
      }
      if (myDefData1 != null) {
         myDefData1.zeroImpulses();
      }      
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      if (myDefData0 != null) {
         idx = myDefData0.getBilateralImpulses (lam, idx);
      }
      if (myDefData1 != null) {
         idx = myDefData1.getBilateralImpulses (lam, idx);
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      int max = 0;
      if (myDefData0 != null) {
         max += myDefData0.maxFrictionConstraintSets();
      }
      if (myDefData1 != null) {
         max += myDefData1.maxFrictionConstraintSets();
      }
      return max;
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {
      if (myDefData0 != null) {
         numf = myDefData0.addFrictionConstraints (DT, finfo, numf);
      }
      if (myDefData1 != null) {
         numf = myDefData1.addFrictionConstraints (DT, finfo, numf);
      }
      // System.out.println ("CollisionPair.addBilaterals " + cnt);
      return numf;
   }

   @Override
   public void setDrawIntersectionContours(boolean set) {
      drawIntersectionContour = set;
   }

   @Override
   public boolean isDrawIntersectionContours() {
      return drawIntersectionContour;
   }

   @Override
   public void setDrawIntersectionFaces(boolean set) {
      drawIntersectionFaces = set;
   }

   @Override
   public boolean isDrawIntersectionFaces() {
      return drawIntersectionFaces;
   }

   @Override
   public void setDrawIntersectionPoints(boolean set) {
      drawIntersectionPoints = set;
   }

   @Override
   public boolean isDrawIntersectionPoints() {
      return drawIntersectionPoints;
   }

   public void prerender (RenderList list) {
      myRenderContactInfo = myLastContactInfo;
      saveLineSegments();


      if (drawIntersectionFaces && myRenderContactInfo != null 
         && myFaceSegments == null) {
         myFaceSegments = new ArrayList<FaceSeg>();
         buildFaceSegments(myRenderContactInfo, myFaceSegments);
      }
      myRenderFaces = myFaceSegments;

      if (myRigidBodyPairP && myRBContact != null) {
         myRBContact.prerender (list);
      }

   }

   protected void findInsideFaces(Face face, BVFeatureQuery query, PolygonalMesh mesh,
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

   protected void buildFaceSegments(ContactInfo info, ArrayList<FaceSeg> faces) {

      BVFeatureQuery query = new BVFeatureQuery();
      PolygonalMesh mesh0 = null;
      PolygonalMesh mesh1 = null;
      if (myDefData0 != null) {
         mesh0 = myDefData0.getMesh();
      } else {
         mesh0 = myRBData0.getMesh();
      }
      if (myDefData1 != null) {
         mesh1 = myDefData1.getMesh();
      } else {
         mesh1 = myRBData1.getMesh();
      }

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

      // Magnitude of offset vector to add to rendered contour lines.  The
      // offset is needed because the contours coexist with polygonal surfaces,
      // and rendering the latter would otherwise obscure rendering the former.
      offsetMag = 1.0*renderer.centerDistancePerPixel();

      renderer.getZDirection();
      Vector3d offDir = new Vector3d(renderer.getZDirection());

      // System.out.println("Z direction" + offDir);
      double scale = offsetMag/offDir.norm();
      offDir.scale(scale);

      //       if (myRigidBodyPairP && myRBContact != null) {
      //          myRBContact.render (renderer);
      //       }
      if (myRenderSegments != null) {
         for (LineSeg seg : myRenderSegments) {
            renderer.drawLine (
               props, seg.coords0, seg.coords1, /*selected=*/false);
         }
      }

      if (drawIntersectionContour && 
         props.getEdgeWidth() > 0 &&
         myRenderContactInfo != null) {

         gl.glLineWidth (props.getEdgeWidth());
         float[] rgb = props.getEdgeColorArray();
         if (rgb == null) {
            rgb = props.getLineColorArray();
         }
         renderer.setColor (rgb, false);
         renderer.setLightingEnabled (false);


         // offset lines
         if (myRenderContactInfo.contours != null) {
            for (MeshIntersectionContour contour : myRenderContactInfo.contours) {
               gl.glBegin (GL2.GL_LINE_LOOP);
               for (MeshIntersectionPoint p : contour) {
                  gl.glVertex3d (p.x + offDir.x, p.y + offDir.y, p.z + offDir.z);
               }
               gl.glEnd();
            }
         } else if (myRenderContactInfo.intersections != null){
            // use intersections to render lines
            gl.glBegin(GL2.GL_LINES);
            for (TriTriIntersection tsect : myRenderContactInfo.intersections) {
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
      if (drawIntersectionPoints && myRenderContactInfo != null) {

         if (myRenderContactInfo.intersections != null) {
            for (TriTriIntersection tsect : myRenderContactInfo.intersections) {
               for (Point3d pnt : tsect.points) {
                  pnt.get(coords);
                  renderer.drawPoint(props, coords, false);
               }
            }
         }

         if (myRenderContactInfo.points0 != null) {
            for (ContactPenetratingPoint cpp : myRenderContactInfo.points0) {
               if (cpp.distance > 0) {
                  cpp.vertex.getWorldPoint().get(coords);
                  renderer.drawPoint(props, coords, false);
                  //cpp.position.get(coords);
                  //renderer.drawPoint(props, coords, false);
               }
            }
         }

         if (myRenderContactInfo.points1 != null) {
            for (ContactPenetratingPoint cpp : myRenderContactInfo.points1) {
               if (cpp.distance > 0) {
                  cpp.vertex.getWorldPoint().get(coords);
                  renderer.drawPoint(props, coords, false);
                  // cpp.position.get(coords);
                  // renderer.drawPoint(props, coords, false);
               }
            }
         }

         if (myRenderContactInfo.edgeEdgeContacts != null) {
            for (EdgeEdgeContact eec : myRenderContactInfo.edgeEdgeContacts) {
               eec.point0.get(coords);
               renderer.drawPoint(props, coords, false);
               eec.point1.get(coords);
               renderer.drawPoint(props, coords, false);
            }
         }
      }

      if (drawIntersectionFaces && myRenderFaces != null) {
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
            for (FaceSeg seg : myRenderFaces) {
               gl.glNormal3d(seg.nrm.x, seg.nrm.y, seg.nrm.z);
               gl.glVertex3d(seg.p0.x + offDir.x, seg.p0.y + offDir.y, seg.p0.z + offDir.z);
               gl.glVertex3d(seg.p1.x + offDir.x, seg.p1.y + offDir.y, seg.p1.z + offDir.z);
               gl.glVertex3d(seg.p2.x + offDir.x, seg.p2.y + offDir.y, seg.p2.z + offDir.z);
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

   /** 
    * {@inheritDoc}
    */
   public void skipAuxState (
      DataBuffer data) {

      int dsize = data.zget();
      int zsize = data.zget();
      data.dskip (dsize);
      data.zskip (zsize);
   }

   /** 
    * {@inheritDoc}
    */
   public void getAuxState (DataBuffer data) {

      int didx0 = data.dsize();
      int zidx0 = data.zsize();
      data.zput (0);   // reserve space for size info
      data.zput (0);

      if (myDefData0 != null && myDefData1 != null) {
         myDefData0.getAuxState (data, myDefData1);
         myDefData1.getAuxState (data, myDefData0);
      } else if (myDefData0 != null && myRBData1 != null) {
         myDefData0.getAuxState(data, myRBData1);
      } else if (myDefData1 != null && myRBData0 != null) {
         myDefData1.getAuxState(data, myRBData0);
      } else {
         System.err.println("Error: we shouldn't be here"); 
      }

      data.zset (zidx0, data.dsize()-didx0); 
      data.zset (zidx0+1, data.zsize()-zidx0-2);
   }      

   /** 
    * {@inheritDoc}
    */
   public void getInitialAuxState (
      DataBuffer newData, DataBuffer oldData) {

      int didx0 = newData.dsize();
      int zidx0 = newData.zsize();
      newData.zput (0);   // reserve space for size info
      newData.zput (0);

      if (myDefData0 != null) {
         myDefData0.getInitialAuxState (newData, oldData);
      }
      if (myDefData1 != null) {
         myDefData1.getInitialAuxState (newData, oldData);
      }

      newData.zset (zidx0, newData.dsize()-didx0);
      newData.zset (zidx0+1, newData.zsize()-zidx0-2);
   }      

   /** 
    * {@inheritDoc}
    */
   public void setAuxState (DataBuffer data) {

      int dsize = data.zget(); // should use for sanity checking?
      int zsize = data.zget();

      int oldzidx = data.zoffset();
      int olddidx = data.doffset();

      if (myDefData0 != null && myDefData1 != null) {
         myDefData0.setAuxState (data, myDefData1);
         myDefData1.setAuxState (data, myDefData0);
      } else if (myDefData0 != null && myRBData1 != null) {
         myDefData0.setAuxState(data, myRBData1);
      } else if (myDefData1 != null && myRBData0 != null) {
         myDefData1.setAuxState(data, myRBData0);
      } else {
         System.err.println("Error: we shouldn't be here"); 
      }

      if ( (data.zoffset()-oldzidx) != zsize || 
         (data.doffset()-olddidx) != dsize) {
         throw new IllegalStateException("Failed to read state correctly");
      }


   }

   public double updateConstraints (double t, int flags) {
      if ((flags & MechSystem.COMPUTE_CONTACTS) != 0) {
         return projectPosConstraints (t);
      }
      else if ((flags & MechSystem.UPDATE_CONTACTS) != 0) {
         // right now just leave the same contacts in place ...
         return 0;
      }
      else {
         return 0;
      }
   }

   //   @Override
   //   public void connectToHierarchy () {
   //      super.connectToHierarchy ();
   //      if (myComponent0 instanceof ModelComponentBase) {
   //         ((ModelComponentBase)myComponent0).addBackReference (this);
   //      }
   //      if (myComponent1 instanceof ModelComponentBase) {
   //         ((ModelComponentBase)myComponent1).addBackReference (this);
   //      }
   //   }
   //
   //   @Override
   //   public void disconnectFromHierarchy() {
   //      super.disconnectFromHierarchy();
   //      if (myComponent0 instanceof ModelComponentBase) {
   //         ((ModelComponentBase)myComponent0).removeBackReference (this);
   //      }
   //      if (myComponent1 instanceof ModelComponentBase) {
   //         ((ModelComponentBase)myComponent1).removeBackReference (this);
   //      }
   //   }

   @Override
   void setLastContactInfo(ContactInfo info) {
      myLastContactInfo = info;
   }

   @Override
   ContactInfo getLastContactInfo() {
      return myLastContactInfo;
   }

}
