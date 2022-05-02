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
 * Information about a single contact constraint that is needed for later
 * processing, either in a collision response or for rendering.
 */
public class ContactConstraintData {
   
   static final double DOUBLE_PREC = 2.2204460492503131e-16;

   ContactPoint myCpnt0; // first point associated with the contact
   ContactPoint myCpnt1; // second point associated with the contact
   double myLambda;      // most recent force used to enforce the constraint
   boolean myPnt0OnCollidable1; // if true, cpnt0 is located on collidable1
   Vector3d myNormal;    // contact direction normal
   Vector3d myFrictionForce;

   public ContactConstraintData () {
      myNormal = new Vector3d();
      myFrictionForce = new Vector3d();
   }

   public ContactConstraintData (ContactConstraint cons) {
      // we can store contact points by reference since they are regenerated
      // whenever contact constraints are updated
      myCpnt0 = cons.myCpnt0;
      myCpnt1 = cons.myCpnt1;
      myLambda = cons.myLambda;
      myPnt0OnCollidable1 = cons.myPnt0OnCollidable1;
      myNormal = new Vector3d(cons.myNormal);
      myFrictionForce = new Vector3d(cons.myFrictionForce);
   }

   public double getContactForce() {
      return myLambda;
   }

   public Vector3d getNormal() {
      return myNormal;
   }

   public boolean isPnt0OnCollidable1() {
      return myPnt0OnCollidable1;
   }

   public void getState (DataBuffer data) {
      data.zputBool (myPnt0OnCollidable1);
      ContactPoint.getState (data, myCpnt0);
      ContactPoint.getState (data, myCpnt1);
      data.dput (myLambda);
      data.dput (myNormal);
      data.dput (myFrictionForce);
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
      data.dget(myNormal);
      data.dget(myFrictionForce);
   }

}
