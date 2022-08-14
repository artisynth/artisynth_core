/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.Vector3d;
import artisynth.core.modelbase.ContactPoint;
import maspack.matrix.Point3d;
import maspack.util.DataBuffer;

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
   Vector3d myFrictionForce; // friction force associated with the contact
   boolean myIsBilateral; // if true, the contact constraint is bilateral

   /**
    * Constructs a new, empty ContactConstraintData object.
    */
   public ContactConstraintData () {
      myNormal = new Vector3d();
      myFrictionForce = new Vector3d();
   }

   /**
    * Constructs a ContactConstraintData object from the data in a 
    * {@link ContactConstraint}.
    * 
    * @param cons constraint providing the contact data
    * @param isBilateral if {@code true}, indicates the contact constraint
    * is bilateral
    */
   public ContactConstraintData (ContactConstraint cons, boolean isBilateral) {
      // we can store contact points by reference since they are regenerated
      // whenever contact constraints are updated
      myCpnt0 = cons.myCpnt0;
      myCpnt1 = cons.myCpnt1;
      myLambda = cons.myLambda;
      myPnt0OnCollidable1 = cons.myPnt0OnCollidable1;
      myNormal = new Vector3d(cons.myNormal);
      myFrictionForce = new Vector3d(cons.myFrictionForce);
      myIsBilateral = isBilateral;
   }

   /**
    * Returns the first point of this contact.
    * 
    * @return first contact point. Should not be modified.
    */
   public ContactPoint getContactPoint0() {
      return myCpnt0;
   }

   /**
    * Returns the position of the first point of this contact, in world
    * coordinates.
    * 
    * @return first point position. Should not be modified.
    */
   public Point3d getPosition0() {
      return myCpnt0.getPosition();
   }
   
   /**
    * Queries whether the first contact point is on the second collidable
    * of this contact. If {@code true}, the first point is on the second
    * collidable and the second point is on the first collidable. This
    * condition can only occur for collisions using vertex penetration
    * (see {@link CollisionBehavior.Method}). Otherwise, the first point is on 
    * the first collidable and the second point is on the second collidable.
    * 
    * @return {@code} if the first contact point is on the second collidable
    */
   public boolean point0OnCollidable1() {
      return myPnt0OnCollidable1;
   }
   
   /**
    * Returns the second point of this contact.
    * 
    * @return second contact point. Should not be modified.
    */
   public ContactPoint getContactPoint1() {
      return myCpnt1;
   }
   
   /**
    * Returns the position of the second point of this contact, in world
    * coordinates.
    * 
    * @return second point position. Should not be modified.
    */
   public Point3d getPosition1() {
      return myCpnt1.getPosition();
   }
   
   /**
    * Returns the value of the contact force along the contact normal.
    * The value is usually positive, but may briefly be negative
    * when contact is being handled using bilateral constraints which
    * have not yet separated.
    * 
    * @param contact force scalar
    */
   public double getContactForceScalar () {
      return myLambda;   
   }
   
   /**
    * Returns the contact force associated with this contact. The
    * force lies in the same direction as the contact normal, and
    * is given by {@code lam * nrm}, where {@code lam} and {@code nrm} are the 
    * values returned by {@link #getContactForceScalar} and {@link #getNormal}.
    * 
    * @return contact force
    */
   public Vector3d getContactForce() {
      Vector3d force = new Vector3d();
      force.scale (myLambda, myNormal);
      return force;      
   }

   /**
    * Returns the friction force associated with this contact. The
    * force, if any, will be perpendicular to the contact normal.
    * 
    * @return friction force. Should not be modified.
    */
   public Vector3d getFrictionForce() {
      return myFrictionForce;
   }

   /**
    * Returns the normal associated with this contact. The
    * normal has unit length and is directed from the first contact point 
    * (returned by {@link #getContactPoint0}), and directed toward the second
    * contact point (returned by {@link #getContactPoint0}).
    * 
    * @return contact normal. Should not be modified.
    */
   public Vector3d getNormal() {
      return myNormal;
   }

   /**
    * Queries whether or not the constraint associated with this contact
    * is bilateral.
    * 
    * @return {@true} if constraint is bilateral
    */
   boolean isBilateral() {
      return myIsBilateral;
   }
   
   /**
    * Saves the information of this ContactContraintData into a data buffer.
    * 
    * @param data buffer in which to store information
    */
   public void getState (DataBuffer data) {
      data.zputBool (myPnt0OnCollidable1);
      data.zputBool (myIsBilateral);
      ContactPoint.getState (data, myCpnt0);
      ContactPoint.getState (data, myCpnt1);
      data.dput (myLambda);
      data.dput (myNormal);
      data.dput (myFrictionForce);
   }

   /**
    * Loads the information of this ContactContraintData from a data buffer,
    * using the contact's collidable bodies to supply additional information.
    * 
    * @param data buffer from which to get information
    * @param collidable0 first collidable body associated with this contact
    * @param collidable1 second collidable body associated with this contact
    */
   public void setState (
      DataBuffer data, CollidableBody collidable0, CollidableBody collidable1) {

      myPnt0OnCollidable1 = data.zgetBool();
      myIsBilateral = data.zgetBool();
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
