/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.HashSet;

import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.util.DataBuffer;
import maspack.util.NumberFormat;
import artisynth.core.mechmodels.MechSystem.FrictionInfo;

/**
 * Information for a contact constraint formed by a single contact point.
 * @author lloyd
 */
public class ContactConstraint {

   static final double DOUBLE_PREC = 2.2204460492503131e-16;

   ArrayList<ContactMaster> myMasters0; // underlying master components which
                         // will be required to resolve the contact
   ArrayList<ContactMaster> myMasters1;
   //CollisionHandler myHandler; // handler to which the constraint belongs
   ContactPoint myCpnt0; // first point associated with the contact
   ContactPoint myCpnt1; // second point associated with the contact
   double myLambda;      // most recent force used to enforce the constraint
   double myDistance;    // contact penetration distance
   boolean myActive;     // whether or not this constraint is active
   boolean myIdentifyByPoint1; // if true, this constraint should be identified
                         // by the second contact point instead of the first 
   Vector3d myNormal;    // contact direction normal
   int mySolveIndex;     // block index of this constraint in either the
                         // bilateral or unilateral constraint matrix (GT or NT)
   double myContactArea; // average area associated with the contact, or
                         // -1 if not available

   public double getForce() {
      return myLambda;
   }

   public void setForce (double lam) {
      myLambda = lam;
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
   }

   public ContactConstraint (
      ContactPoint cpnt0, ContactPoint cpnt1) {
      //myHandler = handler;
      myMasters0 = new ArrayList<ContactMaster>();
      myMasters1 = new ArrayList<ContactMaster>();
      myNormal = new Vector3d();
      myCpnt0 = new ContactPoint(cpnt0);
      myCpnt1 = new ContactPoint(cpnt1);
      myIdentifyByPoint1 = false;
   }

   public void setContactPoint0 (Point3d pnt) {
      myCpnt0.set (pnt);
   }

   public void setContactPoint1 (Point3d pnt) {
      if (myCpnt1 == null) {
         myCpnt1 = new ContactPoint();
      }
      myCpnt1.set (pnt);
   }

   public void setContactPoints (ContactPoint cpnt0, ContactPoint cpnt1) {
      myCpnt0 = cpnt0;
      myCpnt1 = cpnt1;
   }

   public void equateContactPoints (){
      myCpnt1 = myCpnt0;
   }

   public double getDerivative() {
      // TODO LATER
      return 0;
   }

   public void addConstraintBlocks (SparseBlockMatrix GT, int bj) {
      for (ContactMaster cm : myMasters0) {
         cm.add1DConstraintBlocks (GT, bj, 1, myCpnt0, myNormal);
      }
      for (ContactMaster cm : myMasters1) {
         cm.add1DConstraintBlocks (GT, bj, -1, myCpnt1, myNormal);
      }
   }

   double computeFrictionDir (Vector3d dir) {

      // compute relative velocity into dir
      dir.setZero();

      for (ContactMaster cm : myMasters0) {
         cm.addRelativeVelocity (dir, 1, myCpnt0);
      }
      for (ContactMaster cm : myMasters1) {
         cm.addRelativeVelocity (dir, -1, myCpnt1);
      }
      // turn this into tangential velocity
      dir.scaledAdd(-myNormal.dot(dir), myNormal);
      double mag = dir.norm();

      if (mag > DOUBLE_PREC) {
         dir.scale(-1/mag);
         return mag;
      }
      else {
         return 0;
      }
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
      SparseBlockMatrix DT, FrictionInfo[] finfo, double mu, int numf) {

      Vector3d dir = new Vector3d();
      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         if (computeFrictionDir(dir) > 0 && mu > 0) {
            finfo[numf].mu = mu;
            finfo[numf].contactIdx0 = mySolveIndex;
            finfo[numf].contactIdx1 = -1;
            finfo[numf].flags = FrictionInfo.BILATERAL;
            for (ContactMaster cm : myMasters0) {
               cm.add1DConstraintBlocks (DT, numf, 1, myCpnt0, dir);
            }
            for (ContactMaster cm : myMasters1) {
               cm.add1DConstraintBlocks (DT, numf, -1, myCpnt1, dir);
            }
            numf++;
         }
      }
      return numf;      
   }

   public int add2DFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, double mu, int numf) {

      if (myMasters0.size() > 0 || myMasters1.size() > 0) {
         Vector3d dir0 = new Vector3d();
         Vector3d dir1 = new Vector3d();
         computeFrictionDirs (dir0, dir1);
         finfo[numf].mu = mu;
         finfo[numf].contactIdx0 = mySolveIndex;
         finfo[numf].contactIdx1 = -1;
         finfo[numf].flags = 0;
         for (ContactMaster cm : myMasters0) {
            cm.add2DConstraintBlocks (DT, numf, 1, myCpnt0, dir0, dir1);
         }
         for (ContactMaster cm : myMasters1) {
            cm.add2DConstraintBlocks (DT, numf, -1, myCpnt1, dir0, dir1);
         }
         numf++;
      }
      return numf;      
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

      ContactPoint.getState (data, myCpnt0);
      ContactPoint.getState (data, myCpnt1);
      data.dput (myLambda);
      data.dput (myDistance);
      data.zputBool (myActive);
      data.zputBool (myIdentifyByPoint1);
      data.dput (myNormal);
      data.dput (myContactArea);
   }

   public void setState (
      DataBuffer data, CollidableBody collidable0, CollidableBody collidable1) {
      
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
   }

   public String toString (String fmtStr) {
      NumberFormat fmt = new NumberFormat (fmtStr);
      String str = "[ pnt0=" + myCpnt0.toString(fmtStr);
      str += " pnt1=" + myCpnt1.toString(fmtStr);
      str += " dist=" + fmt.format(myDistance) + " ]";
      return str;
   }
}
