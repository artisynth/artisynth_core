/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;

import maspack.collision.PenetrationRegion;
import maspack.geometry.Vertex3d;
import maspack.matrix.MatrixBlock;
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

   ArrayList<ContactMaster> myMasters; // underlying master components which
                         // will be required to resolve the contact
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
      myMasters.clear();
   }

   public ArrayList<ContactMaster> getMasters() {
      return myMasters;
   }

   public boolean isControllable() {
      for (int i=0; i<myMasters.size(); i++) {
         if (myMasters.get(i).myComp.isControllable()) {
            return true;
         }
      }
      return false;
   }

   public ContactConstraint () {
      //myHandler = handler;
      myMasters = new ArrayList<ContactMaster>();
      myNormal = new Vector3d();
      myCpnt0 = new ContactPoint();
      myCpnt0.myVtxs = new Vertex3d[0];
      myIdentifyByPoint1 = false;
   }
   
   public ContactConstraint (ContactPoint cpnt0) {
      //myHandler = handler;
      myMasters = new ArrayList<ContactMaster>();
      myNormal = new Vector3d();
      myCpnt0 = new ContactPoint(cpnt0);
      myIdentifyByPoint1 = false;
   }

   public ContactConstraint (
      ContactPoint cpnt0, ContactPoint cpnt1) {
      //myHandler = handler;
      myMasters = new ArrayList<ContactMaster>();
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
      for (int k=0; k<myMasters.size(); k++) {
         ContactMaster cm = myMasters.get(k);
         int bi = cm.getSolveIndex();
         if (bi != -1) {
            MatrixBlock blk = cm.getBlock(myNormal);
            GT.addBlock(bi, bj, blk);
         }
      }
   }

   double computeFrictionDir (Vector3d dir) {

      // compute relative velocity into dir
      dir.setZero();

      for (int i=0; i<myMasters.size(); i++) {
         myMasters.get(i).addRelativeVelocity (dir);
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
      if (myMasters.size() > 0 && computeFrictionDir(dir) > 0 && mu > 0) {
         finfo[numf].mu = mu;
         finfo[numf].contactIdx0 = mySolveIndex;
         finfo[numf].contactIdx1 = -1;
         finfo[numf].flags = FrictionInfo.BILATERAL;
         for (int i=0; i<myMasters.size(); i++) {
            ContactMaster cm = myMasters.get(i);
            int bi = cm.getSolveIndex();
            if (bi != -1) {            
               MatrixBlock blk = cm.get1DFrictionBlock (dir);
               Vector3d tmp = new Vector3d();
               tmp.x = blk.get(0,0);
               tmp.y = blk.get(1,0);
               tmp.z = blk.get(2,0);
               DT.addBlock (bi, numf, blk);
            }
         }
         numf++;
      }
      return numf;      
   }

   public int add2DFrictionConstraints (
      SparseBlockMatrix DT, FrictionInfo[] finfo, double mu, int numf) {

      if (myMasters.size() > 0) {
         Vector3d dir1 = new Vector3d();
         Vector3d dir2 = new Vector3d();
         computeFrictionDirs (dir1, dir2);
         finfo[numf].mu = mu;
         finfo[numf].contactIdx0 = mySolveIndex;
         finfo[numf].contactIdx1 = -1;
         finfo[numf].flags = 0;
         for (int i=0; i<myMasters.size(); i++) {
            ContactMaster cm = myMasters.get(i);
            int bi = cm.getSolveIndex();
            if (bi != -1) {            
               MatrixBlock blk = cm.get2DFrictionBlock (dir1, dir2);
               DT.addBlock (bi, numf, blk);
            }
         }
         numf++;
      }
      return numf;      
   }

   protected void assignMasters (
      CollidableBody collidable, double w, ContactPoint cpnt) {
      
      if (collidable instanceof RigidBody) {
         myMasters.add (
            new ContactMaster ((RigidBody)collidable, w, cpnt));
      }
      else if (collidable instanceof RigidMeshComp) {
         myMasters.add (
            new ContactMaster (
               ((RigidMeshComp)collidable).getRigidBody(), w, cpnt));        
      }
      else {
         Vertex3d[] vtxs = cpnt.getVertices();
         double[] wgts = cpnt.getWeights();
         ArrayList<ContactMaster> newMasters = new ArrayList<ContactMaster>();
         for (int i=0; i<vtxs.length; i++) {
            newMasters.clear();
            collidable.getVertexMasters (newMasters, vtxs[i]);
            for (int j=0; j<newMasters.size(); j++) {
               ContactMaster cm = newMasters.get(j);
               // see if master has already been added; if so, just adjust weight
               boolean masterAlreadyAdded = false;
               for (int k=0; k<myMasters.size(); k++) {
                  ContactMaster pm = myMasters.get(k);
                  if (pm.myComp == cm.myComp) {
                     pm.myWeight += (w*wgts[i]*cm.myWeight);
                     masterAlreadyAdded = true;
                     break;
                  }
               }
               if (!masterAlreadyAdded) {
                  cm.myWeight = w*wgts[i]*cm.myWeight;
                  cm.myCpnt = cpnt;
                  myMasters.add (cm);
               }
            }
         }
      }
   }
   
   public void assignMasters (
      CollidableBody collidable0, CollidableBody collidable1) {

      myMasters.clear();

      // if (!(collidable1 instanceof RigidBody) &&
      //     collidable2 instanceof RigidBody) {
      //    // use loc for RigidBody masters for Fem-RB contact
      //    collidable1.getContactMasters (myMasters, 1, myInfo1);
      //    ((RigidBody)collidable2).getContactMastersX (myMasters, -1, myInfo2);
      //    return;
      // } 
      assignMasters (collidable0, 1, myCpnt0);
      assignMasters (collidable1, -1, myCpnt1);
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
