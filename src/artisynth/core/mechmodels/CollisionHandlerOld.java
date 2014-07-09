/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.media.opengl.GL2;

import maspack.collision.AbstractCollider;
import maspack.collision.ContactInfo;
import maspack.collision.ContactPenetratingPoint;
import maspack.collision.EdgeEdgeContact;
import maspack.collision.MeshIntersectionContour;
import maspack.collision.MeshIntersectionPoint;
import maspack.collision.SignedDistanceCollider;
import maspack.collision.SurfaceMeshCollider;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.util.DataBuffer;
import maspack.util.FunctionTimer;
import maspack.util.IntHolder;
import artisynth.core.femmodels.FemContactConstraint;
import artisynth.core.femmodels.FemMesh;
import artisynth.core.femmodels.FemMeshVertex;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;

public class CollisionHandlerOld extends CollisionHandler 
implements Constrainer {

   FunctionTimer ftimer = new FunctionTimer(); 

   private AbstractCollider collider;
   private SignedDistanceCollider mySDCollider;
   private RigidBodyContact myRBContact;

   Collidable myComponent0;
   Collidable myComponent1;

   FemData myFemData0;
   FemData myFemData1;

   RBData myRBData0;
   RBData myRBData1;

   boolean femSubdivision = false;
   double renderContacts = 0.7; // length of normal used to render
   double myMu = 0;

   private boolean myRigidBodyPairP = false;
   // if true, ignore rigid bodies when doing Gauss-Siedel iterative steps
   public static boolean myIgnoreRBForGS = true;
   public static boolean renderContactNormals = false;
   //XXX public static boolean useSignedDistanceCollider = false;

   ContactInfo myLastContactInfo; // last contact info produced by this handler
   ContactInfo myRenderContactInfo; // contact info to be used for rendering

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

   private double getMass (ModelComponent comp) {
      if (comp instanceof RigidBody) {
         return ((RigidBody)comp).getMass();
      }
      else if (comp instanceof FemModel) {
         return ((FemModel)comp).getMass();
      }
      else {
         throw new UnsupportedOperationException (
            "getMass() not supported from object type "+ comp.getClass());
      }
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


   void addLineSegment (Point3d pnt0, Vector3d nrm, double len) {
      myLineSegments.add (new LineSeg (pnt0, nrm, len));
   }

   void clearLineSegments() {
      myLineSegments = new ArrayList<LineSeg>();
   }

   private void saveLineSegments() {
      myRenderSegments = myLineSegments;
   }

   void initialize() {
      myRenderSegments = null;
      myLineSegments = null;
      myLastContactInfo = null;
   }

   // End special code for rendering contact points

   static final Object PRESENT = new Object();

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

   private boolean isVisible() {
      ModelComponent parent = getParent();
      if (myRenderProps != null) {
         return myRenderProps.isVisible();
      }
      else if (parent instanceof Renderable) {
         return ((Renderable)parent).getRenderProps().isVisible();
      }
      else {
         return false;
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
   }

   /**
    * Stores information for a specific rigid body associated with this
    * collision pair.
    */
   private class RBData {
      RigidBody myBody;
      PolygonalMesh myMesh;

      RBData(RigidBody body, PolygonalMesh mesh) {
         myBody = body;
         myMesh = mesh;
      }
      
      RBData(RigidBody body) {
         this(body, body.getMesh ());
      }

      PolygonalMesh getMesh() {
         return myMesh;
      }

      RigidBody getBody() {
         return myBody;
      }

   }

   /**
    * Stores information for a specific deformable body associated with this
    * collision pair. This information mainly includes the set of contact
    * constraints currently associated with the body.
    */
   private class FemData {
      FemModel3d myFemMod;
      PolygonalMesh myMesh;
      
      boolean myContactsChanged = true;
      private FemContactConstraint myCstub;

      LinkedHashMap<FemContactConstraint,FemContactConstraint> myConstraints;
      // LinkedHashSet<FemContactConstraint> mySet;

      FemData (FemModel3d femMod, PolygonalMesh mesh) {
         myFemMod = femMod;
         myMesh = mesh;
         myCstub = new FemContactConstraint();
         myConstraints =
            new LinkedHashMap<FemContactConstraint,FemContactConstraint> (
               myFemMod.getNodes().size());
      }
      
      FemData (FemModel3d femMod) {
        this(femMod, femMod.getSurfaceMesh ());
      }

      PolygonalMesh getMesh() {
        return myMesh;
      }
      
      FemModel3d getComponent() {
         return myFemMod;
      }

      void getBilateralSizes (VectorNi sizes) {
         for (int i=0; i<myConstraints.values().size(); i++) {
            sizes.append (1);
         }
      }

      /**
       * Adds the matrix blocks associated with the bodies contact constraints
       * to the constraint matrix transpose. This method is used when building
       * the constraint matrix structure. Blocks should be added starting at the
       * block column indicated by numb. The block column number is then updated
       * and returned by this method.
       * 
       * <p>
       * Deformable contact constraints are currently implemented as bilateral
       * constraints to save computational effort. The unilateral nature of the
       * constraint is handled by examining its impulse after the velocity
       * solve. If the impulse is negative, that indicates that the constraint
       * is trying to separate and constraint is removed.
       * 
       * @param GT
       * constraint matrix transpose
       * @param numb
       * block column number where the blocks should begin
       * @return updated block column number
       */
      int addBilateralConstraints (SparseBlockMatrix GT, VectorNd dg, int numb) {

         double[] dbuf = (dg != null ? dg.getBuffer() : null);

         for (FemContactConstraint c : myConstraints.values()) {
            c.addConstraintBlocks (GT, GT.numBlockCols());
            if (dbuf != null) {
               dbuf[numb] = c.getDerivative();
            }
            numb++;
         }
         return numb;
      }

      int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {

         for (FemContactConstraint c : myConstraints.values()) {
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

      //       int getBilateralOffsets (
      //          VectorNd Rg, VectorNd bg, int idx, int mode) {

      //          for (FemContactConstraint c : myConstraints.values()) {
      //             double off = 0;
      //             if (mode == MechSystem.FORCE_MODE) {
      //                off = 0;
      //             }
      // //            else if (mode == MechSystem.VELOCITY_MODE) {
      // //               off = c.getVelocityOffset();
      // //            }
      //             else if (mode == MechSystem.POSITION_MODE) {
      //                if (c.myDistance < -myPenetrationTol) {
      //                   off = -(c.myDistance + myPenetrationTol);
      //                }
      //             }
      //             else {
      //                throw new InternalErrorException ("Unknown mode " + mode);
      //             }
      //             c.setSolveIndex (idx);
      //             bg.set (idx, off);
      //             Rg.set (idx, 0);
      //             idx++;
      //          }
      //          return idx;

      //       }

      int maxFrictionConstraintSets() {
         return myConstraints.values().size();
      }

      int addFrictionConstraints (
         SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {

         for (FemContactConstraint c : myConstraints.values()) {
            numf = c.addFrictionConstraints (DT, finfo, numf);
         }
         return numf;
      }

      /**
       * Sets the impulses associated with the contact constraints. This is
       * called by the solver after performing a solve. The resulting impulse
       * information is then used by the constraint to compute friction and to
       * decide when contact constraints should be broken.
       * 
       * @param lam
       * vector containing impulse values
       * @param idx
       * starting index for constraint impulses associated with this collision
       * pair
       * @return updated index value (i.e., idx plus the number of contact
       * constraints).
       */
      int setBilateralImpulses (VectorNd lam, int idx) {
         for (FemContactConstraint c : myConstraints.values()) {
            c.setImpulse (lam.get (idx++));
         }
         return idx;
      }

      void zeroImpulses() {
         for (FemContactConstraint c : myConstraints.values()) {
            c.setImpulse (0);
         }        
      }

      int getBilateralImpulses (VectorNd lam, int idx) {
         for (FemContactConstraint c : myConstraints.values()) {
            lam.set (idx++, c.getImpulse());
         }
         return idx;
      }

      void updateFrictionConstraints () {
         for (FemContactConstraint c : myConstraints.values()) {
            c.updateFriction ();
         }
      }         

      /**
       * Deactivates all the current contact constraints. Constraints may be
       * reactivated later if they are found in the next collision detection and
       * are not separating.
       */
      void clearContactActivity() {
         for (FemContactConstraint c : myConstraints.values()) {
            c.setActive (false);
         }
      }

      /**
       * Returns true if this FEM data set contains an active contact involving
       * a specified vertex or pair of vertices.
       */
      boolean hasActiveContact (FemMeshVertex vtx0, FemMeshVertex vtx1) {
         myCstub.setVertices (vtx0, vtx1);
         FemContactConstraint cons = myConstraints.get (myCstub);
         return (cons != null && cons.isActive());
      }

      /**
       * Gets a constraint for either a vertex-face or an edge-edge contact.
       * Vertex-face contacts are identified by their vertex. Edge-edge contacts
       * are identified by the two vertices of the edge contained within this
       * deformable body. If a matching constraint already exists and is not
       * trying to separate, then that is returned; Otherwise, null is returned.
       * Note that only the constraint container is found or allocated here;
       * it's contents are set later.
       * 
       * @param vtx0 vertex associated with the contact
       * @param vtx1
       * second vertex associated with an edge contact, or null
       * @return constraint structure
       */
      public FemContactConstraint getContact (
         FemMeshVertex vtx0, FemMeshVertex vtx1) {

         myCstub.setVertices (vtx0, vtx1);
         FemContactConstraint cons = myConstraints.get (myCstub);
         if (cons == null) {
            cons = new FemContactConstraint (vtx0, vtx1);
            return cons;
         }
         else // contact already exists
         {
            double lam = cons.getImpulse();

            // do not activate constraint if contact is trying to separate
            if (lam < 0) {
               return null;
            }
            // old code to set a threshold requiring a minimum separating force
            // The idea was that the separation force should be enough to
            // overcome max displacement in .1 secs
            // double maxDisplacement =
            //    getCharacteristicSize() * myPenetrationTol;
            // if (lam / cons.getEffectiveMass() < -maxDisplacement / 0.1) {
            // trying to separate, so no contact ...
            //   return null;
            // 
            else { // will need to set this active later
               return cons;
            }
         }
      }

      /**
       * Gets a constraint for either a body-face contact.
       * If a matching constraint already exists and is not
       * trying to separate, then that is returned; Otherwise, null is returned.
       * Note that only the constraint container is found or allocated here;
       * it's contents are set later.
       * 
       * @param vtx0 vertex associated with the contact
       * @param vtx1
       * second vertex associated with an edge contact, or null
       * @return constraint structure
       */
      public FemContactConstraint getContact (Face face) {

         myCstub.setFace(face);
         FemContactConstraint cons = myConstraints.get (myCstub);
         if (cons == null) {
            cons = new FemContactConstraint ();
            cons.setFace(face);
            cons.setActive(true);
            return cons;
         }
         else // contact already exists
         {
            double lam = cons.getImpulse();

            // do not activate constraint if contact is trying to separate
            if (lam < 0) {
               return null;
            }
            // old code to set a threshold requiring a minimum separating force
            // The idea was that the separation force should be enough to
            // overcome max displacement in .1 secs
            // double maxDisplacement =
            //    getCharacteristicSize() * myPenetrationTol;
            // if (lam / cons.getEffectiveMass() < -maxDisplacement / 0.1) {
            // trying to separate, so no contact ...
            //   return null;
            // 
            else { // will need to set this active later
               return cons;
            }
         }
      }

      public void addContact (FemContactConstraint c) {
         myConstraints.put (c, c);
         myContactsChanged = true;
      }

      /**
       * Removes all contact contraints which are currently inactive. This is
       * called after all new constraints have been determined.
       */
      void removeInactiveContacts() {
         Iterator<FemContactConstraint> it =
            myConstraints.values().iterator();
         while (it.hasNext()) {
            FemContactConstraint c = it.next();
            if (!c.isActive()) {
               it.remove();
               myContactsChanged = true;
            }
         }
      }

      /**
       * Notification that the components associated with a specific contact
       * constraint has changed, and therefore the GT matrix will have a
       * different structure for the next step.
       */
      void notifyContactsChanged() {
         myContactsChanged = true;
      }

      /**
       * Returns true if the contact structure has changed such that the GT
       * matrix will have a different structure and will have to be reanalyzed.
       * Contact structure will change if contact constraints are added or
       * removed, or if the components associated with a specific constraint
       * have changed.
       * 
       * @return true if contact structure has changed.
       */
      boolean contactsHaveChanged() {
         boolean changed = myContactsChanged;
         myContactsChanged = false;
         return changed;
      }

      /**
       * Clears all the contact constraint data. This is done whenever there is
       * a change in component structure or activity, requiring that contact
       * information be rebuilt from scratch.
       */
      void clearContactData() {
         myConstraints.clear();
      }

      /** 
       * {@inheritDoc}
       */
      public void skipAuxState (
         DataBuffer data, StateContext context) {
         data.zskip (2);
         for (FemContactConstraint c : myConstraints.values()) {
            c.skipAuxStateOld (data);
         }
      }

      public void getAuxState (DataBuffer data) {
         data.zput (myConstraints.size());
         // write out contacts as having changed regardless, since we may need
         // to reanalyze the KKT matrix:
         //data.zadd ((myContactsChanged ? 1 : 0);
         data.zput (1);
         for (FemContactConstraint c : myConstraints.values()) {
            c.getAuxStateOld (data);
         }
      }      

      public void setAuxState (
         DataBuffer data, 
         FemModel3d model, PolygonalMesh mesh0, 
         PolygonalMesh mesh1, Collidable other) {

         clearContactData();
         int nc = data.zget();
         myContactsChanged = (data.zget() == 1);
         for (int i=0; i<nc; i++) {
            FemContactConstraint c = new FemContactConstraint();
            c.setAuxStateOld (data, model, mesh0, mesh1, other);
            c.setFriction (myMu);
            myConstraints.put (c, c);
         }
      }

      public void getInitialAuxState (
         DataBuffer newData, DataBuffer oldData) {
         // just create a state in which there are no contacts

         newData.zput (0); // no contact constraints
         newData.zput (1); // contacts have changed
      }

      public boolean hasActiveContacts() {
         for (FemContactConstraint fcc : myConstraints.values()) {
            if (fcc.isActive()) {
               return true;
            }
         }
         return false;
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

   CollisionHandlerOld (Collidable c0, Collidable c1) {
      this (c0, c1, 0);
   }

   CollisionHandlerOld (
      Collidable c0, Collidable c1, double fc) {

      myComponent0 = c0;
      myComponent1 = c1;

      if (myComponent0 instanceof FemModel3d) {
         myFemData0 = new FemData ((FemModel3d)myComponent0);
      } else if (myComponent0 instanceof RigidBody) {
         myRBData0 = new RBData((RigidBody)myComponent0);
      } else if (myComponent0 instanceof FemMesh) {
         FemMesh mesh = (FemMesh)myComponent0;
         myComponent0 = mesh.getFem ();
         myFemData0 = new FemData (mesh.getFem (), (PolygonalMesh)mesh.getMesh());
      } else if (myComponent0 instanceof RigidMeshComponent) {
         RigidMeshComponent mesh = (RigidMeshComponent)myComponent0;
         myComponent0 = mesh.getRigidBody ();
         myRBData0 = new RBData ((RigidBody)myComponent0, (PolygonalMesh)mesh.getMesh ());
      }

      if (myComponent1 instanceof FemModel3d) {
         myFemData1 = new FemData ((FemModel3d)myComponent1);
      } else if (myComponent1 instanceof RigidBody) {
         myRBData1 = new RBData((RigidBody)myComponent1);
      } else if (myComponent1 instanceof FemMesh) {
         FemMesh mesh = (FemMesh)myComponent1;
         myComponent1 = mesh.getFem ();
         myFemData1 = new FemData (mesh.getFem (), (PolygonalMesh)mesh.getMesh());
      } else if (myComponent1 instanceof RigidMeshComponent) {
         RigidMeshComponent mesh = (RigidMeshComponent)myComponent1;
         myComponent1 = mesh.getRigidBody();
         myRBData1 = new RBData ((RigidBody)myComponent1, (PolygonalMesh)mesh.getMesh ());
      }

      if (myRBData0 != null && myRBData1 != null) {
         myRigidBodyPairP = true;
      }
      myMu = fc;
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

   public boolean getFemSubdivision() {
      return femSubdivision;
   }

   public void setFemSubdivision (boolean enabled) {
      femSubdivision = enabled;
   }

   public void updateFrictionConstraints () {
      if (myFemData0 != null) {
         myFemData0.updateFrictionConstraints();
      }
      if (myFemData1 != null) {
         myFemData1.updateFrictionConstraints();
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
      
      if ((myComponent0 instanceof RigidBody &&
         myComponent1 instanceof FemModel3d) ||
         (myComponent0 instanceof FemModel3d &&
            myComponent1 instanceof RigidBody)) {
         // RigidBody rb;
         //FemModel3d fem;

         if (myComponent0 instanceof RigidBody) {
            //rb = (RigidBody)myComponent0;
            //fem = (FemModel3d)myComponent1;
            maxpen = projectPosRbFem (myRBData0, myFemData1);
            a = myRBData0.getBody();
            b = myFemData1.getComponent();
         }
         else {
            //rb = (RigidBody)myComponent1;
            //fem = (FemModel3d)myComponent0;
            maxpen = projectPosRbFem (myRBData1, myFemData0);
            a = myRBData1.getBody();
            b = myFemData0.getComponent();
         }
      }
      else if (myComponent0 instanceof FemModel3d &&
         myComponent1 instanceof FemModel3d) {
         maxpen = projectPosFemFem (myFemData0, myFemData1);
         a = myFemData0.getComponent();
         b = myFemData1.getComponent();
      }
      
      //System.out.println("Num contacts (" + a.getName()+ ", " 
      //   + b.getName()+ "): " + myLineSegments.size());
      
      return maxpen;
   }

   /**
    * Implements projectPosConstraints for the deformable-rigidBody case.
    */
   private double projectPosRbFem (RBData rbData, FemData femData) {

      PolygonalMesh meshrb = rbData.getMesh();
      PolygonalMesh meshfem = femData.getMesh();
      double maxpen = 0;

      // deactivate constraints; these may be reactivated later
      clearLineSegments();
      femData.clearContactActivity();

      // get contacts
      ContactInfo info;
      if (useSignedDistanceCollider) {
         info = getSDCollider().getContacts (meshrb, meshfem, false);
      }
      else {
         info = getCollider().getContacts (meshrb, meshfem, false);
      }

      
      if (info != null) {
         if (computeTimings) {
            ftimer.reset();
            ftimer.start();
         }
         if (doBodyFaceContact) {
            maxpen = createRbFemContacts (
               rbData, info.points1, info.points0, info.edgeEdgeContacts, femData);
         } else {
            maxpen = createRbFemContactsOld (
               rbData, info.points1, info.points0, info.edgeEdgeContacts, femData);
         }
         if (computeTimings) {
            ftimer.stop();
            double t = ftimer.getTimeUsec();
            System.out.println("Function took " + t + " usec");
         }
      }
      myLastContactInfo = info;
      // remove contacts which are still inactive
      femData.removeInactiveContacts();
      return maxpen;
   }

   /**
    * Implements projectPosConstraints for the deformable-deformable case.
    */
   private double projectPosFemFem (
      FemData femData0, FemData femData1) {

      PolygonalMesh mesh0 = femData0.getMesh();
      PolygonalMesh mesh1 = femData1.getMesh();
      double maxpen = 0;

      // deactvate constraints; these may be reactivated later
      clearLineSegments();
      femData0.clearContactActivity();
      femData1.clearContactActivity();

//      RigidBodyContact.checkMesh (mesh0);
//      RigidBodyContact.checkMesh (mesh1);

     // get contacts
      ContactInfo info = getCollider().getContacts (mesh0, mesh1, false);

      if (info != null) {
         double max;
         //checkRegionPoints ("mesh0", info.points0);
         //checkRegionPoints ("mesh1", info.points1);
         max = createFemFemContacts (info.points0, femData0, femData1);
         if (max > maxpen) {
            maxpen = max;
         }
         max = createFemFemContacts (info.points1, femData1, femData0);
         if (max > maxpen) {
            maxpen = max;
         }
         // createEdgeEdgeConstraints(info.edgeEdgeContacts, correctPos);
      }

      myLastContactInfo = info;
      // remove contacts which are still inactive
      femData0.removeInactiveContacts();
      femData1.removeInactiveContacts();

      return maxpen;
   }

   private boolean vertexIsAttached (Vertex3d vtx, ModelComponent comp) {
      boolean compIsFem = (comp instanceof FemModel);      
      FemNode node = ((FemMeshVertex)vtx).getPoint();
      if (node.isAttached()) {
         DynamicMechComponent[] masters = node.getAttachment().getMasters();
         for (int i=0; i<masters.length; i++) {
            if (compIsFem) {
               if (ComponentUtils.getGrandParent (masters[i]) == comp) {
                  return true;
               }
            }
            else {
               if (masters[i] == comp) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private boolean faceAttachedTo (Face face, ModelComponent comp) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (vertexIsAttached (he.getHead(), comp)) {
            return true;
         }
         he = he.getNext();
      }
      while (he != he0);
      return false;
   }

   private boolean vertexRegionAttachedTo (Vertex3d vtx, ModelComponent comp) {
      if (vertexIsAttached (vtx, comp)) {
         return true;
      }
      Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge he = it.next();
         if (vertexIsAttached (he.getTail(), comp)) {
            return true;
         }
      }
      return false;
   }

   /** 
    * Returns true if a contact has sufficient DOF for enforcement.  This means
    * that it must be directly or indirectly associated with at least one
    * active component.
    */
   private boolean contactHasDOF (FemContactConstraint cons) {
      return cons.isControllable();
   }

   void createEdgeEdgeConstraints (
      ArrayList<EdgeEdgeContact> eecs) {
      for (EdgeEdgeContact eec : eecs) {
         if (eec.calculate()) { // Check if the contact has already been
            // corrected by other contact corrections.
            FemMeshVertex v0t = (FemMeshVertex)eec.edge0.tail;
            FemMeshVertex v0h = (FemMeshVertex)eec.edge0.head;
            FemMeshVertex v1t = (FemMeshVertex)eec.edge1.tail;
            FemMeshVertex v1h = (FemMeshVertex)eec.edge1.head;
            FemContactConstraint cons = myFemData0.getContact (v0t, v0h);
            // As long as the constraint exists and is not already marked 
            // as active, then we add it
            if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < eec.displacement ) )) {
               cons.setEdgeEdge (
                  eec.point1ToPoint0Normal, myMu, v1t, v1h,
                  v0t, v0h, 1 - eec.s1, eec.s1, 1 - eec.s0, eec.s0);
               if (contactHasDOF (cons)) {
                  activateContact (
                     cons, -eec.displacement, myFemData0);
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
   private double createFemFemContacts (
      ArrayList<ContactPenetratingPoint> points, FemData data,
      FemData otherData) {

      Vector3d disp = new Vector3d();
      Vector3d normal = new Vector3d();

      double nrmlLen = getContactNormalLen();
      double maxpen = 0;

      for (ContactPenetratingPoint cpp : points) {
         FemMeshVertex vtx0 = (FemMeshVertex)cpp.vertex;
         FemMeshVertex vtx1 = (FemMeshVertex)cpp.face.getVertex(0);
         FemMeshVertex vtx2 = (FemMeshVertex)cpp.face.getVertex(1);
         FemMeshVertex vtx3 = (FemMeshVertex)cpp.face.getVertex(2);
         FemNode3d node0 = (FemNode3d)vtx0.getPoint();
         FemNode3d node1 = (FemNode3d)vtx1.getPoint();

         if (faceAttachedTo (cpp.face, otherData.myFemMod) ||
            vertexRegionAttachedTo (cpp.vertex, otherData.myFemMod)) {
            // ignore this contact since it is in a region where the bodies are
            // connected
            continue;
         }

         // if (!node.isActive() && !node0.isActive() &&
         // !node1.isActive() && !node2.isActive())
         // { continue;
         // }

         double w0 = 1.0 - (cpp.coords.x + cpp.coords.y);
         double w1 = cpp.coords.x;
         double w2 = cpp.coords.y;

         // compute normal from scratch because previous contacts
         // may have caused it to change
         cpp.face.computeNormal (normal);

         // get or allocate a new contact
         FemContactConstraint cons = data.getContact (vtx0, null);
         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < cpp.distance ) )) {
            cons.setVertexFace (
               normal, myMu, vtx0, vtx1, vtx2, vtx3, w0, w1, w2);
            if (contactHasDOF (cons)) {
               if (nrmlLen != 0) {
                  addLineSegment (cpp.position, normal, nrmlLen);
               }
               disp.sub (node0.getPosition(), node1.getPosition());
               double dist = disp.dot(normal);
               activateContact (cons, dist, data);
               if (-dist > maxpen) {
                  maxpen = dist;
               }
            }
         }
      }
      return maxpen;
   }

   public void activateContact (
      FemContactConstraint cons, double dist, FemData data) {

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
   }
   
   /**
    * Create the constraints, and apply one pass of interpenetration resolution,
    * for the contacts associated with a deformable-rigidBody contact.
    */
   private double createRbFemContactsOld (
      RBData rbData, ArrayList<ContactPenetratingPoint> pointsFem, 
      ArrayList<ContactPenetratingPoint> pointsRb, 
      ArrayList<EdgeEdgeContact> edgeEdgeContacts, FemData femData) { 

      RigidBody body = rbData.getBody();
      Vector3d normal = new Vector3d();
      Vector3d disp = new Vector3d();
      Point3d loc = new Point3d(); // point on rigid body, in body coordinates
      RigidTransform3d XBW = body.getPose(); // body to world transform

      double nrmlLen = getContactNormalLen();
      double maxpen = 0;

      // Create contacts based on FEM vertices which are interpenetrated
      // the rigid body
      for (ContactPenetratingPoint cpp : pointsFem) {

         //System.out.println("cpp: " + cpp.vertex + ", " + cpp.position + ", " + cpp.distance + ", " 
         //   + cpp.normal + ", " + cpp.coords);
         
         FemMeshVertex vtx = (FemMeshVertex)cpp.vertex;
         FemNode3d node = (FemNode3d)vtx.getPoint();
         if (!node.isActive() && !body.isActive()) {
            continue;
         }
         if (vertexRegionAttachedTo (vtx, body)) {
            continue;
         }

         loc.inverseTransform (XBW, cpp.position);
         if (useSignedDistanceCollider) {
            normal.set (cpp.normal);
         }
         else {
            normal.transform (XBW, cpp.face.getNormal());
         }

         // get or allocate a new contact
         FemContactConstraint cons = femData.getContact (vtx, null);
         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < cpp.distance ) )) {
            cons.setVertexBody (normal, myMu, vtx, 1, body, loc, 1);
            if (contactHasDOF (cons)) {
               if (nrmlLen != 0) {
                  addLineSegment (cpp.position, normal, nrmlLen);               
               }
               disp.sub (node.getPosition(), cpp.position);
               double dist = disp.dot(normal);
               //System.out.println("Contact: pos " + cpp.position +" loc " + loc + ", norm "  + normal + ", dist "+ dist);
               //cons.printConstraintInfo(System.out);
               activateContact (cons, dist, femData);
               if (-dist > maxpen) {
                  maxpen = -dist;
               }
            }
         }
         
      }

      // Create other contacts based on rigid body vertices which are
      // interpenetrating the FEM. 
      HashMap<Face,Double> faceContactDist = new HashMap<Face,Double>();
      HashMap<Face,ContactPenetratingPoint> faceContactPoint =
         new HashMap<Face,ContactPenetratingPoint>();
      
      // for each interpenetrating rigid body vertex, find the associated
      // nearest face on the FEM surface mesh. Create a hash map of all such
      // faces, and for each face, find the rigid body vertex that has the
      // greatest penetration distance.
      for (ContactPenetratingPoint cpp : pointsRb) {
         loc.transform (XBW, cpp.vertex.pnt);
         disp.sub (loc, cpp.position);
         double d = disp.dot (cpp.face.getNormal());
         //System.out.println ("d=" + d + " " + cpp.distance);
         Double minDist = faceContactDist.get(cpp.face);
         if (minDist == null || d < minDist.doubleValue()) {
            minDist = new Double(d);
            faceContactDist.put (cpp.face, minDist);
            faceContactPoint.put (cpp.face, cpp);
         }
      }
      // For each interpenetrated FEM surface face, create a contact constraint
      // using the most interpenetrating vertex (as identified in the preceding
      // for loop).
      for (Face f : faceContactPoint.keySet()) {
         // ignore FEM faces which have vertices that are themselves
         // interpenetrating the rigid body, or that are otherwise attached to
         // the rigid body.

         boolean faceIsFree = true;
         HalfEdge he = f.firstHalfEdge();
         do {
            if (femData.hasActiveContact ((FemMeshVertex)he.getHead(), null)) {
               faceIsFree = false;
               break;
            }
            he = he.getNext();
         }
         while (he != f.firstHalfEdge());
         if (faceAttachedTo (f, body)) {
            faceIsFree = false;
         }
         if (faceIsFree) {
         // John Lloyd, Sep 19 2013: not sure why the active part of this code
         // has been commented out; this happened before the code was committed
         // on 1/26/2010.
         //   ContactPenetratingPoint cpp = faceContactPoint.get(f);
         //   FemContactConstraint cons = data.getContact (f);
         //   if (cons != null) {
         //      setFaceBodyConstraint (
         //         cons, f.getNormal(), f, cpp.coords, body, cpp.vertex.pnt);
         //      if (contactHasDOF (cons)) {
         //         addLineSegment (cpp.position, f.getNormal());
         //         loc.transform (XBW, cpp.vertex.pnt);
         //         disp.sub (loc, cpp.position);
         //         activateContact (
         //            cons, disp.dot(f.getNormal()), data, correctPos);
         //      }
         //   }
         }
      }

      for (EdgeEdgeContact eec : edgeEdgeContacts) {
         if (eec.calculate()) {
            FemMeshVertex vt = (FemMeshVertex)eec.edge1.getTail();
            FemMeshVertex vh = (FemMeshVertex)eec.edge1.getHead();
            FemNode3d nodet = (FemNode3d)vt.getPoint();
            FemNode3d nodeh = (FemNode3d)vh.getPoint();
            if (!nodet.isActive() && !nodeh.isActive() && !body.isActive()) {
               continue;
            }

            double wh = 1.0 - eec.s1;
            double wt = eec.s1;

            loc.inverseTransform (XBW, eec.point1);
            normal.transform (XBW, eec.point1ToPoint0Normal);
            normal.normalize();

            FemContactConstraint cons = femData.getContact (vt, vh);
            // As long as the constraint exists and is not already marked 
            // as active, then we add it
            if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < eec.displacement ) )) {
               cons.setEdgeBody (
                  normal, myMu, vt, vh, wt, wh, body, loc, 1);
               if (contactHasDOF (cons)) {
                  double dist = -eec.displacement;
                  activateContact (cons, dist, femData);
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
    * Create the constraints, and apply one pass of interpenetration resolution,
    * for the contacts associated with a deformable-rigidBody contact.
    */
   private double createRbFemContacts (
      RBData rbData, ArrayList<ContactPenetratingPoint> pointsFem, 
      ArrayList<ContactPenetratingPoint> pointsRb, 
      ArrayList<EdgeEdgeContact> edgeEdgeContacts, FemData femData) { 

      Vector3d disp = new Vector3d();
      Vector3d normal = new Vector3d();
      Point3d loc = new Point3d(); // point on rigid body, in body coordinates

      double nrmlLen = getContactNormalLen();
      double maxpen = 0;

      RigidBody body = rbData.getBody();
      RigidTransform3d XBW = body.getPose(); // body to world transform

      // Edit: Sanchez, Sept 24 2013
      // Previous system failed to catch a lot of collisions.  We would
      // get RB vertices highly penetrated into FEM, but no push-back
      // since the the fem vertices were also marginally interpenetrating 
      // the RB.
      //
      // New system:
      //     Label all vtx->rb constraints as active
      //     For each face
      //          find max penetrating rb vtx inside fem
      //     
      //     Loop through each face
      //        Loop through each 'active' vertex
      //        If max rb->fem penetration > ALL active vtx->rb
      //           de-activate fem vtx->rb,
      //           
      //     Loop through active fem vtx->rb constraints
      //        add constraint
      //   
      //     Loop through each face
      //        if no vertices active, add rb->fem face constraint
      

      // Create list of contacts based on FEM vertices which are 
      // interpenetrated the rigid body.
      HashMap<Vertex3d, Double> vtxDistanceMap = 
         new HashMap<Vertex3d, Double>(pointsFem.size());
      HashMap<Vertex3d, ContactPenetratingPoint> vtxCppMap = 
         new HashMap<Vertex3d, ContactPenetratingPoint>(pointsFem.size());

      for (ContactPenetratingPoint cpp : pointsFem) {
         FemMeshVertex vtx = (FemMeshVertex)cpp.vertex;
         FemNode3d node = (FemNode3d)vtx.getPoint();
         if (!node.isActive() && !body.isActive()) {
            continue;
         }
         if (vertexRegionAttachedTo (vtx, body)) {
            continue;
         }
         if (useSignedDistanceCollider) {
            normal.set (cpp.normal);
         }
         else {
            normal.transform (XBW, cpp.face.getNormal());
         }
         disp.sub (node.getPosition(), cpp.position);
         double dist = disp.dot(normal);

         vtxDistanceMap.put(vtx,  dist);
         vtxCppMap.put(vtx, cpp);
      }


      HashMap<Face,Double> faceContactDist = new HashMap<Face,Double>();
      HashMap<Face,ContactPenetratingPoint> faceContactPoint =
         new HashMap<Face,ContactPenetratingPoint>();

      // for each interpenetrating rigid body vertex, find the associated
      // nearest face on the FEM surface mesh. Create a hash map of all such
      // faces, and for each face, find the rigid body vertex that has the
      // greatest penetration distance.
      for (ContactPenetratingPoint cpp : pointsRb) {
         loc.transform (XBW, cpp.vertex.pnt);
         disp.sub (loc, cpp.position);
         double d = disp.dot (cpp.face.getNormal());
         //System.out.println ("d=" + d + " " + cpp.distance);
         Double minDist = faceContactDist.get(cpp.face);
         if (minDist == null || d < minDist.doubleValue()) {
            minDist = new Double(d);
            faceContactDist.put (cpp.face, minDist);
            faceContactPoint.put (cpp.face, cpp);
         }
      }

      // For each interpenetrated FEM surface face, create a contact constraint
      // using the most interpenetrating vertex (as identified in the preceding
      // for loop).
      for (Face f : faceContactPoint.keySet()) {

         double faceDist = faceContactDist.get(f);
         boolean useFace = true;
         HalfEdge he = f.firstHalfEdge();
         if (faceAttachedTo (f, body)) {
            useFace = false;
         } else {
            do {
               FemMeshVertex vtx = (FemMeshVertex)he.getHead();
               Double d = vtxDistanceMap.get(vtx);
               if (d != null && d.doubleValue() < faceDist) {
                  useFace = false;
                  break;
               }
               he = he.getNext();
            }
            while (he != f.firstHalfEdge());
         }

         // next face if we are not using it;
         if (!useFace) {
            continue;
         }

         // disable all vertices
         do {
            FemMeshVertex vtx = (FemMeshVertex)he.getHead();
            vtxDistanceMap.put(vtx, null);
            vtxCppMap.put(vtx, null);
            he = he.getNext();
         }
         while (he != f.firstHalfEdge());         
      }

      // add active vertex->RB contacts
      for (ContactPenetratingPoint cpp : vtxCppMap.values()) {
         if (cpp != null) {
            FemMeshVertex vtx = (FemMeshVertex)cpp.vertex;
            FemNode3d node = (FemNode3d)vtx.getPoint();

            // get or allocate a new contact
            FemContactConstraint cons = femData.getContact (vtx, null);
            // As long as the constraint exists and is not already marked 
            // as active, then we add it
            if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < cpp.distance ) )) {
               loc.inverseTransform (XBW, cpp.position);
               if (useSignedDistanceCollider) {
                  normal.set (cpp.normal);
               }
               else {
                  normal.transform (XBW, cpp.face.getNormal());
               }
               disp.sub (node.getPosition(), cpp.position);
               double dist = disp.dot(normal);
               
               cons.setVertexBody (normal, myMu, vtx, 1, body, loc, 1);
               if (contactHasDOF (cons)) {
                  if (nrmlLen != 0) {
                     addLineSegment (cpp.position, normal, nrmlLen);               
                  }
                  activateContact (cons, dist, femData);
                  if (-dist > maxpen) {
                     maxpen = -dist;
                  }
               }
            }
         }
      }

      // add active body->face contacts
      for (Face f : faceContactPoint.keySet()) {
      
         boolean useFace = true;
         
         HalfEdge he = f.firstHalfEdge();
         do {
            FemMeshVertex vtx = (FemMeshVertex)he.getHead();
            ContactPenetratingPoint cpp = vtxCppMap.get(vtx);
            if (cpp != null) {
               useFace = false;
               break;
            }
            he = he.getNext();
         }
         while (he != f.firstHalfEdge());
         
         if (!useFace) {
            continue;
         }
         
         ContactPenetratingPoint cpp = faceContactPoint.get(f);
         FemContactConstraint cons = femData.getContact (f);

         // As long as the constraint exists and is not already marked 
         // as active, then we add it
         if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < cpp.distance ) )) {
            
            FemMeshVertex vtx0 = (FemMeshVertex)cpp.face.getVertex(0);
            FemMeshVertex vtx1 = (FemMeshVertex)cpp.face.getVertex(1);
            FemMeshVertex vtx2 = (FemMeshVertex)cpp.face.getVertex(2);

            double w0 = 1.0 - (cpp.coords.x + cpp.coords.y);
            double w1 = cpp.coords.x;
            double w2 = cpp.coords.y;

            // compute normal from scratch because previous contacts
            // may have caused it to change
            cpp.face.computeNormal (normal);

            cons.setBodyFace(normal, myMu, 
               vtx0, vtx1, vtx2, w0, w1, w2, cpp.vertex, body, cpp.vertex.pnt, 1);

            if (contactHasDOF (cons)) {
               if (nrmlLen != 0) {
                  addLineSegment (cpp.position, normal, nrmlLen);
               }
               loc.transform (XBW, cpp.vertex.pnt);
               disp.sub (loc, cpp.position);
               double dist = disp.dot(normal);
               activateContact (cons, dist, femData);
               if (-dist > maxpen) {
                  maxpen = dist;
               }
            }
         }
      }

      for (EdgeEdgeContact eec : edgeEdgeContacts) {
         if (eec.calculate()) {
            FemMeshVertex vt = (FemMeshVertex)eec.edge1.getTail();
            FemMeshVertex vh = (FemMeshVertex)eec.edge1.getHead();
            FemNode3d nodet = (FemNode3d)vt.getPoint();
            FemNode3d nodeh = (FemNode3d)vh.getPoint();
            if (!nodet.isActive() && !nodeh.isActive() && !body.isActive()) {
               continue;
            }

            double wh = 1.0 - eec.s1;
            double wt = eec.s1;

            loc.inverseTransform (XBW, eec.point1);
            normal.transform (XBW, eec.point1ToPoint0Normal);
            normal.normalize();

            FemContactConstraint cons = femData.getContact (vt, vh);
            if (cons != null && ( !cons.isActive() || ( -cons.getDistance() < eec.displacement ) )) {
               cons.setEdgeBody (
                  normal, myMu, vt, vh, wt, wh, body, loc, 1);
               if (contactHasDOF (cons)) {
                  double dist = -eec.displacement;
                  activateContact (cons, dist, femData);
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
   
   public boolean hasActiveContacts() {
      if (myFemData0 != null && myFemData0.hasActiveContacts()) {
         return true;
      }
      if (myFemData1 != null && myFemData1.hasActiveContacts()) {
         return true;
      }
      if (isRigidBodyPair() && myRBContact != null) {
         return myRBContact.hasActiveContact();
      }
      return false;
   }

   /**
    * Notifies this CollisionPair that the component structure or activity has
    * changes and so the constraint structures will need to be recomputed.
    */
   public void structureOrActivityChanged() {
      if (myFemData0 != null) {
         myFemData0.clearContactData();
      }
      if (myFemData1 != null) {
         myFemData1.clearContactData();
      }
   }

   public void getBilateralSizes (VectorNi sizes) {
      if (myFemData0 != null) {
         myFemData0.getBilateralSizes (sizes);
      }
      if (myFemData1 != null) {
         myFemData1.getBilateralSizes (sizes);
      }
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb, IntHolder changeCnt){
      if (myFemData0 != null) {
         numb = myFemData0.addBilateralConstraints (GT, dg, numb);
         if (myFemData0.contactsHaveChanged()) {
            changeCnt.value++;
         }
      }
      if (myFemData1 != null) {
         numb = myFemData1.addBilateralConstraints (GT, dg, numb);
         if (myFemData1.contactsHaveChanged()) {
            changeCnt.value++;
         }
      }
      // System.out.println ("CollisionPair.addBilaterals " + cnt);
      return numb;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
      int idx0 = idx;
      if (myFemData0 != null) {
         idx = myFemData0.getBilateralInfo (ginfo, idx);
      }
      if (myFemData1 != null) {
         idx = myFemData1.getBilateralInfo (ginfo, idx);
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

   public int setBilateralImpulses (VectorNd lam, double h, int idx) {
      if (myFemData0 != null) {
         idx = myFemData0.setBilateralImpulses (lam, idx);
      }
      if (myFemData1 != null) {
         idx = myFemData1.setBilateralImpulses (lam, idx);
      }
      return idx;
   }

   public void zeroImpulses() {
      if (myFemData0 != null) {
         myFemData0.zeroImpulses();
      }
      if (myFemData1 != null) {
         myFemData1.zeroImpulses();
      }      
   }

   public int getBilateralImpulses (VectorNd lam, int idx) {
      if (myFemData0 != null) {
         idx = myFemData0.getBilateralImpulses (lam, idx);
      }
      if (myFemData1 != null) {
         idx = myFemData1.getBilateralImpulses (lam, idx);
      }
      return idx;
   }

   public int maxFrictionConstraintSets() {
      int max = 0;
      if (myFemData0 != null) {
         max += myFemData0.maxFrictionConstraintSets();
      }
      if (myFemData1 != null) {
         max += myFemData1.maxFrictionConstraintSets();
      }
      return max;
   }

   public int addFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, int numf) {
      if (myFemData0 != null) {
         numf = myFemData0.addFrictionConstraints (DT, finfo, numf);
      }
      if (myFemData1 != null) {
         numf = myFemData1.addFrictionConstraints (DT, finfo, numf);
      }
      // System.out.println ("CollisionPair.addBilaterals " + cnt);
      return numf;
   }

   public void prerender (RenderList list) {
      myRenderContactInfo = myLastContactInfo;
      saveLineSegments();
      if (myRigidBodyPairP && myRBContact != null) {
         myRBContact.prerender (list);
      }
   }

   public RenderProps createRenderProps() {
      return RenderProps.createLineEdgeProps (this);
   }

   public void render (GLRenderer renderer, int flags) {
      render (renderer, myRenderProps, flags);
   }
   // ArrayList<Vector3d[]> renderlines = new ArrayList<Vector3d[]>();
   // ArrayList<Vector3d> renderpoints = new ArrayList<Vector3d>();

   // Twist lastmomentumchange = null;

   public void render (GLRenderer renderer, RenderProps props, int flags) {
      GL2 gl = renderer.getGL2();
      //       if (myRigidBodyPairP && myRBContact != null) {
      //          myRBContact.render (renderer);
      //       }
      if (myRenderSegments != null) {
         for (LineSeg seg : myRenderSegments) {
            renderer.drawLine (
               props, seg.coords0, seg.coords1, /*selected=*/false);
         }
      }
      if (props.getEdgeWidth() > 0 &&
         myRenderContactInfo != null &&
         myRenderContactInfo.contours != null) {
         gl.glLineWidth (props.getEdgeWidth());
         float[] rgb = props.getEdgeColorArray();
         if (rgb == null) {
            rgb = props.getLineColorArray();
         }
         renderer.setColor (rgb, false);
         renderer.setLightingEnabled (false);
         for (MeshIntersectionContour contour : myRenderContactInfo.contours) {
            gl.glBegin (GL2.GL_LINE_LOOP);
            for (MeshIntersectionPoint p : contour) {
               gl.glVertex3d (p.x, p.y, p.z);
            }
            gl.glEnd();
         }
         gl.glEnable (GL2.GL_LIGHTING);
         gl.glLineWidth (1);
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
      data.zput (0);   // reserve space
      data.zput (0);

      if (myFemData0 != null) {
         myFemData0.getAuxState (data);
      }
      if (myFemData1 != null) {
         myFemData1.getAuxState (data);
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
      newData.zput (0);   // reserve space 
      newData.zput (0);

      if (myFemData0 != null) {
         myFemData0.getInitialAuxState (newData, oldData);
      }
      if (myFemData1 != null) {
         myFemData1.getInitialAuxState (newData, oldData);
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

      PolygonalMesh mesh0 = null;
      PolygonalMesh mesh1 = null;
      FemModel3d fem0 = null;
      FemModel3d fem1 = null;
      
      RigidBody body = null;

      if (myFemData0 != null) {
         mesh0 = myFemData0.getMesh();
         fem0 = myFemData0.getComponent();
      }
      if (myFemData1 != null) {
         mesh1 = myFemData1.getMesh();
         fem1 = myFemData1.getComponent();
      }
      if (myComponent0 instanceof RigidBody) {
         body = (RigidBody)myComponent0;
      }
      else if (myComponent1 instanceof RigidBody) {
         body = (RigidBody)myComponent1;
      }


      if (myFemData0 != null && myFemData1 != null) {
         myFemData0.setAuxState (data, fem0, mesh0, mesh1, fem1);
         myFemData1.setAuxState (data, fem1, mesh1, mesh0, fem0);
      } else if (myFemData0 != null && body != null) {
         myFemData0.setAuxState(data, fem0, mesh0, null, body);
      } else if (myFemData1 != null && body != null) {
         myFemData1.setAuxState(data, fem1, mesh1, null, body);
      } else {
         System.err.println("CollisionHandlerOld.setAuxState(...)\n\tError: we shouldn't be here"); 
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

   @Override
   public void setDrawIntersectionContours(boolean set) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean isDrawIntersectionContours() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void setDrawIntersectionFaces(boolean set) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean isDrawIntersectionFaces() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void setDrawIntersectionPoints(boolean set) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean isDrawIntersectionPoints() {
      // TODO Auto-generated method stub
      return false;
   }

}
