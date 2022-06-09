/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;

import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.VectorNi;
import maspack.util.DataBuffer;
import maspack.util.NumberFormat;
import maspack.spatialmotion.FrictionInfo;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;

/**
 * Information for a contact constraint formed by a single contact point.
 * @author lloyd
 */
public class ContactConstraint {

   /**
    * Describes the static friction state of this contact. Used in
    * cases where static friction is implemented using bilateral constraints. 
    */
   public enum StictionState {

      UNSET, // state has not been set
      ON,    // static friction is engaged
      OFF    // static friction is disengaged      
   }
   
   static final double DOUBLE_PREC = 2.2204460492503131e-16;

   ArrayList<ContactMaster> myMasters0; // underlying master components which
                         // will be required to resolve the contact
   ArrayList<ContactMaster> myMasters1;
   //CollisionHandler myHandler; // handler to which the constraint belongs
   ContactPoint myCpnt0; // first point associated with the contact
   ContactPoint myCpnt1; // second point associated with the contact
   double myLambda;      // most recent force used to enforce the constraint
   int myState;          // for unilateral constraints, most recent LCP state 
   double myDistance;    // contact penetration distance
   boolean myActive;     // whether or not this constraint is active
   boolean myIdentifyByPoint1; // if true, this constraint should be identified
                         // by the cpnt1 instead of cpnt0
   boolean myPnt0OnCollidable1; // if true, cpnt0 is located on collidable1
                         // instead of collidable0
   Vector3d myNormal;    // contact direction normal
   int mySolveIndex;     // block index of this constraint in either the
                         // bilateral or unilateral constraint matrix (GT or NT)
   double myContactArea; // average area associated with the contact, or
                         // -1 if not available
   
   Vector3d myFrictionForce;
   double myPhi0;        // most recent friction forces
   double myPhi1;
   int myFrictionState0; // most recent friction states resulting for LCP solve
   int myFrictionState1;
   Vector3d myTangentialVelocity; // cached value of the tangential velocity

   public double getContactForce() {
      return myLambda;
   }
   
   int setForces (double[] buf, double s, int idx) {
      myLambda = s*buf[idx++];
      return idx;
   }

   int getForces (double[] buf, int idx) {
      buf[idx++] = myLambda;
      return idx;
   }
   
   int setState (int[] buf, int idx) {
      myState = buf[idx++];
      return idx;
   }
   
   int getState (int[] buf, int idx) {
      buf[idx++] = myState;
      return idx;
   }
   
   public void zeroForces() {
      myLambda = 0;
      myPhi0 = 0;
      myPhi1 = 0;
   }

   public int getSolveIndex() {
      return mySolveIndex;
   }

   public void setSolveIndex (int idx) {
      mySolveIndex = idx;
   }

   public double getDistance() {
      return myDistance;
   }

   public void setDistance (double d) {
      myDistance = d;
   }

   public Vector3d getNormal() {
      return myNormal;
   }

   public void setNormal (Vector3d nrm) {
      myNormal.set (nrm);
   }

   public boolean isActive() {
      return myActive;
   }

   public void setActive (boolean active) {
      myActive = active;
   }

   public void clearMasters() {
      myMasters0.clear();
      myMasters1.clear();
   }

   public int collectMasterComponents (
      HashSet<DynamicComponent> masters, boolean activeOnly) {
      int num = 0;
      for (ContactMaster cm : myMasters0) {
         num += cm.collectMasterComponents (masters, activeOnly);
      }
      for (ContactMaster cm : myMasters1) {
         num += cm.collectMasterComponents (masters, activeOnly);
      }
      return num;
   }
   
   /*
    * For debugging only. Return the component number of the 
    * first master component found, or -1 if no masters are found.
    */
   int getMasterNumber() {
      LinkedHashSet<DynamicComponent> masters = 
         new LinkedHashSet<DynamicComponent>();
      collectMasterComponents (masters, true);
      if (masters.size() > 0) {
         for (DynamicComponent dc : masters) {
            return dc.getNumber();
         }
      }
      return -1;
   }

   public boolean isControllable() {
      for (ContactMaster cm : myMasters0) {
         if (cm.isControllable()) {
            return true;
         }
      }
      for (ContactMaster cm : myMasters1) {
         if (cm.isControllable()) {
            return true;
         }
      }
      return false;
   }

   public ContactConstraint () {
      //myHandler = handler;
      myMasters0 = new ArrayList<ContactMaster>();
      myMasters1 = new ArrayList<ContactMaster>();
      myNormal = new Vector3d();
      myCpnt0 = new ContactPoint();
      myCpnt0.myVtxs = new Vertex3d[0];
      myIdentifyByPoint1 = false;
      myFrictionForce = new Vector3d();
   }

   public ContactConstraint (
      ContactPoint cpnt0, ContactPoint cpnt1, boolean pnt0OnCollidable1) {
      //myHandler = handler;
      myMasters0 = new ArrayList<ContactMaster>();
      myMasters1 = new ArrayList<ContactMaster>();
      myNormal = new Vector3d();
      myCpnt0 = new ContactPoint(cpnt0);
      myCpnt1 = new ContactPoint(cpnt1);
      myIdentifyByPoint1 = false;
      myFrictionForce = new Vector3d();
      myPnt0OnCollidable1 = pnt0OnCollidable1;
   }

   public void setContactPoint0 (Point3d pnt, boolean pnt0OnCollidable1) {
      myCpnt0.set (pnt);
      myPnt0OnCollidable1 = pnt0OnCollidable1;
   }

//   public void setContactPoint1 (Point3d pnt) {
//      if (myCpnt1 == null) {
//         myCpnt1 = new ContactPoint();
//      }
//      myCpnt1.set (pnt);
//   }

   public void setContactPoints (
      ContactPoint cpnt0, ContactPoint cpnt1, boolean pnt0OnCollidable1) {
//      if (myCpnt0 == null || !myCpnt0.verticesEqual (cpnt0)) {
//         myIdx = -1;
//         myFrictionIdx = -1;
//      }
      myCpnt0 = cpnt0;
//      if (myCpnt1 == null || !myCpnt1.verticesEqual (cpnt1)) {
//         myIdx = -1;
//         myFrictionIdx = -1;
//      }
      myCpnt1 = cpnt1;
      myPnt0OnCollidable1 = pnt0OnCollidable1;
   }

   public boolean isPnt0OnCollidable1() {
      return myPnt0OnCollidable1;
   }
   
   public void equateContactPoints (){
      myCpnt1 = myCpnt0;
   }

   public double getDerivative() {
      // TODO LATER
      return 0;
   }
   
   public int addBilateralConstraints (
      SparseBlockMatrix GT, double[] dbuf, int numb) {

      int bj = GT.numBlockCols();
      addConstraintBlocks (GT, bj);
      if (dbuf != null) {
         dbuf[numb] = getDerivative();
      }
      numb++;
      return numb;
   }

   public int getBilateralInfo (
      ConstraintInfo[] ginfo, int idx, double penTol, double[] fres,
      ContactForceBehavior forceBehavior) {

      setSolveIndex (idx);
      ConstraintInfo gi = ginfo[idx];
      if (getDistance() < -penTol) {
         gi.dist = (getDistance() + penTol);
      }
      else {
         gi.dist = 0;
      }
      if (forceBehavior != null) {
         forceBehavior.computeResponse (
            fres, myDistance, myCpnt0, myCpnt1, 
               myNormal, myContactArea);
      }
      gi.force =      fres[0];
      gi.compliance = fres[1];
      gi.damping =    fres[2];
      return ++idx;
   }
   
   public void addConstraintBlocks (SparseBlockMatrix GT, int bj) {
      for (ContactMaster cm : myMasters0) {
         cm.add1DConstraintBlocks (GT, bj, 1, myCpnt0, myNormal);
      }
      for (ContactMaster cm : myMasters1) {
         cm.add1DConstraintBlocks (GT, bj, -1, myCpnt1, myNormal);
      }
   }
   
   void computeTangentialVelocity (Vector3d vel) {
      vel.setZero();

      for (ContactMaster cm : myMasters0) {
         cm.addRelativeVelocity (vel, 1, myCpnt0);
      }
      for (ContactMaster cm : myMasters1) {
         cm.addRelativeVelocity (vel, -1, myCpnt1);
      }
      // turn this into tangential velocity
      vel.scaledAdd(-myNormal.dot(vel), myNormal);     
   }
   
   double computeFrictionDir (Vector3d dir) {
      if (myTangentialVelocity == null) {
         myTangentialVelocity = new Vector3d();
      }
      // compute relative velocity into dir
      computeTangentialVelocity (myTangentialVelocity);
      // normalize to obtain a direction
      dir.set (myTangentialVelocity);
      double mag = dir.norm();
      if (mag > DOUBLE_PREC) {
         dir.scale(-1/mag);
         return mag;
      }
      else {
         return 0;
      }
   }

   void zeroTangentialVelocity() {
      if (myTangentialVelocity == null) {
         myTangentialVelocity = new Vector3d();
      }
      myTangentialVelocity.setZero();
   }

   void computeFrictionDirs (Vector3d dir1, Vector3d dir2) {
      if (computeFrictionDir (dir1) == 0) {
         // choose an arbitrary direction perpendicular to the normal
         dir1.perpendicular (myNormal);         
      }
      dir1.negate();
      dir2.cross (myNormal, dir1);
   }

   public int add1DFrictionConstraints (
      SparseBlockMatrix DT, ArrayList<FrictionInfo> finfo, 
      double mu, double stictionCreep, int numf) {

      Vector3d dir = new Vector3d();
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         if (computeFrictionDir(dir) > 0 && mu > 0) {
            FrictionInfo info = finfo.get(numf);
            info.mu = mu;
            info.contactIdx0 = mySolveIndex;
            info.contactIdx1 = -1;
            info.flags = FrictionInfo.BILATERAL;
            info.stictionCreep = stictionCreep;
            info.blockIdx = DT.numBlockRows();
            info.blockSize = 1;
            for (ContactMaster cm : myMasters0) {
               cm.add1DConstraintBlocks (DT, numf, 1, myCpnt0, dir);
            }
            for (ContactMaster cm : myMasters1) {
               cm.add1DConstraintBlocks (DT, numf, -1, myCpnt1, dir);
            }
            numf++;
         }
         else {
            zeroTangentialVelocity();
         }
      }
      return numf;      
   }
   
   public int set1DFrictionForce (VectorNd phi, double s, int idx) {
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         if (myTangentialVelocity.norm() > 0) {
            myPhi0 = s*phi.get(idx++);
         }
      }
      return idx;
   }

   public int get1DFrictionForce (VectorNd phi, int idx) {
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         if (myTangentialVelocity.norm() > 0) {
            phi.set(idx++, myPhi0);
         }
      }
      return idx;
   }
   
   public int set1DFrictionState (VectorNi state, int idx) {
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         if (myTangentialVelocity.norm() > 0) {
            myFrictionState0 = state.get(idx++);
         }
      }
      return idx;
   }

   public int get1DFrictionState (VectorNi state, int idx) {
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         if (myTangentialVelocity.norm() > 0) {
            state.set(idx++, myFrictionState0);
         }
      }
      return idx;
   }

   public int add2DFrictionConstraints (
      SparseBlockMatrix DT, ArrayList<FrictionInfo> finfo, 
      double mu, double stictionCreep, int numf, 
      boolean prune, boolean bilateralContact) {

      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         FrictionInfo info = finfo.get(numf);
         if (CollisionHandler.myPrune && prune && 
            Math.abs(getContactForce())*mu <= CollisionHandler.ftol) {
            info.blockIdx = -1;
            info.blockSize = 2;
         }
         else {
            int bj = DT.numBlockCols();
            Vector3d dir0 = new Vector3d();
            Vector3d dir1 = new Vector3d();
            computeFrictionDirs (dir0, dir1);
            info.mu = mu;
            info.contactIdx0 = mySolveIndex;
            info.contactIdx1 = -1;
            info.flags = (bilateralContact ? FrictionInfo.BILATERAL : 0);
            info.stictionCreep = stictionCreep;
            info.blockIdx = bj;
            info.blockSize = 2;
            for (ContactMaster cm : myMasters0) {
               cm.add2DConstraintBlocks (DT, bj, 1, myCpnt0, dir0, dir1);
            }
            for (ContactMaster cm : myMasters1) {
               cm.add2DConstraintBlocks (DT, bj, -1, myCpnt1, dir0, dir1);
            }
         }
         numf++;
      }
      return numf;      
   }

   public int set2DFrictionForce (VectorNd phi, double s, int idx) {
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         myPhi0 = s*phi.get(idx++);
         myPhi1 = s*phi.get(idx++);
         // compute and store the actual friction force
         if (myPhi0 != 0 || myPhi1 != 0) {
            Vector3d dir0 = new Vector3d();
            Vector3d dir1 = new Vector3d();
            double mag = myTangentialVelocity.norm();
            if (mag > DOUBLE_PREC) {
               dir0.scale (-1/mag, myTangentialVelocity);
            }
            else {
               dir0.perpendicular (myNormal); 
            }
            dir0.negate();
            dir1.cross (myNormal, dir0);
            myFrictionForce.combine (myPhi0, dir0, myPhi1, dir1);
         }
         else {
            myFrictionForce.setZero();
         }
      }
      return idx;
   }

   public int get2DFrictionForce (VectorNd phi, int idx) {
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         phi.set(idx++, myPhi0);
         phi.set(idx++, myPhi1);
      }
      return idx;
   }
   
   public int set2DFrictionState (VectorNi state, int idx) {
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         myFrictionState0 = state.get(idx++);
         myFrictionState1 = state.get(idx++);
      }
      return idx;
   }

   public int get2DFrictionState (VectorNi state, int idx) {
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         state.set(idx++, myFrictionState0);
         state.set(idx++, myFrictionState1);
      }
      return idx;
   }

   protected void assignMasters (
      ArrayList<ContactMaster> masters, CollidableBody collidable,
      double w, ContactPoint cpnt) {
      
      if (collidable instanceof RigidBody) {
         masters.add ((RigidBody)collidable);
      }
//    RigidMeshComp no longer implements CollidableBody
//      else if (collidable instanceof RigidMeshComp) {
//         masters.add (((RigidMeshComp)collidable).getRigidBody());
//      }
      else {
         Vertex3d[] vtxs = cpnt.getVertices();
         double[] wgts = cpnt.getWeights();
         masters.add (new VertexContactMaster (collidable, vtxs, wgts)); 
      }
   }
   
   public void assignMasters (
      CollidableBody collidable0, CollidableBody collidable1) {

      clearMasters();
      assignMasters (myMasters0, collidable0, 1, myCpnt0);
      assignMasters (myMasters1, collidable1, 1, myCpnt1);
   }

   public void getState (DataBuffer data) {
      data.zputBool (myPnt0OnCollidable1);
      ContactPoint.getState (data, myCpnt0);
      ContactPoint.getState (data, myCpnt1);
      data.dput (myLambda);
      data.dput (myDistance);
      data.zputBool (myActive);
      data.zputBool (myIdentifyByPoint1);
      data.dput (myNormal);
      data.dput (myContactArea);
      // can we optimize the friction state?
      data.dput (myFrictionForce);
      data.dput (myPhi0);
      data.dput (myPhi1);
   }

   public void setState (
      DataBuffer data, CollidableBody collidable0, CollidableBody collidable1) {

      myPnt0OnCollidable1 = data.zgetBool();
      if (myPnt0OnCollidable1) {
         // switch collidable0 and collidable1
         CollidableBody tmp = collidable0;
         collidable0 = collidable1;
         collidable1 = tmp;
      }
      myCpnt0 = ContactPoint.setState (data, collidable0.getCollisionMesh());
      myCpnt1 = ContactPoint.setState (data, collidable1.getCollisionMesh());
      if (myCpnt1 == null) {
         myCpnt1 = myCpnt0;
      }
      myLambda = data.dget();
      myDistance = data.dget();
      myActive = data.zgetBool();
      myIdentifyByPoint1 = data.zgetBool();
      data.dget(myNormal);
      myContactArea = data.dget();
      assignMasters (collidable0, collidable1);
      data.dget(myFrictionForce);
      myPhi0 = data.dget();
      myPhi1 = data.dget();
      // clear previous indices. For CollisionHandlers whose masters are
      // invariant, indices will be set based simply on the
      // number of active constraints.
      //myIdx = -1; 
      //myFrictionIdx = -1;
   }

   public String toString (String fmtStr) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      String str = "[ pnt0=" + myCpnt0.toString(fmtStr);
      str += " pnt1=" + myCpnt1.toString(fmtStr);
      str += " dist=" + fmt.format(myDistance) + " ]";
      return str;
   }
}
