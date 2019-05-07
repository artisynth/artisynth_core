/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.util.DataBuffer;
import maspack.matrix.*;

public class RigidBodyConstraint {
//   int myBodyIdxA;
//   int myBodyIdxB;
   boolean myUnilateralP = false;
   Wrench myWrenchA = new Wrench();
   Wrench myWrenchB = new Wrench();
   Wrench myWrenchC = new Wrench();
   Wrench myDotWrenchC = new Wrench();
   int mySolveIndex;
   double myDistance;
   double myCompliance;
   double myDamping;
   double myDerivative;
   double myOffset;
   double myMultiplier;
   double myContactSpeed;
   double myContactAccel;
   double myFriction;
   double myFrictionLimit;
   Point3d myContactPoint;

   public void setWrenchA (Wrench wr) {
      myWrenchA.set (wr);
   }

   public Wrench getWrenchA() {
      return myWrenchA;
   }

   public void setWrenchB (Wrench wr) {
      myWrenchB.set (wr);
   }

   public Wrench getWrenchB() {
      return myWrenchB;
   }

   public void setWrenchC (Wrench wr) {
      myWrenchC.set (wr);
   }

   public Wrench getWrenchC() {
      return myWrenchC;
   }

   public void setDotWrenchC (Wrench wr) {
      myDotWrenchC.set (wr);
   }

   public Wrench getDotWrenchC() {
      return myDotWrenchC;
   }

   public RigidBodyConstraint() {
   }

   public boolean isUnilateral() {
      return myUnilateralP;
   }

   public void setUnilateral (boolean unilateral) {
      myUnilateralP = unilateral;
   }

   /** 
    * Returns the index of this constraint within either the GT or NT
    * matrix of the global system MLCP.
    *
    * @return index within either GT or NT
    */
   public int getSolveIndex() {
      return mySolveIndex;
   }

   public void setSolveIndex (int idx) {
      mySolveIndex = idx;
   }

   public void setDerivative (double d) {
      myDerivative = d;
   }

   public double getDerivative() {
      return myDerivative;
   }

   public void setOffset (double b) {
      myOffset = b;
   }

   public double getOffset() {
      return myOffset;
   }

   public void setMultiplier (double lam) {
      myMultiplier = lam;
   }

   public double getMultiplier() {
      return myMultiplier;
   }

   public void setDistance (double d) {
      myDistance = d;
   }

   public double getDistance() {
      return myDistance;
   }

   public void setCompliance (double c) {
      myCompliance = c;
   }

   public double getCompliance() {
      return myCompliance;
   }

   public void setDamping (double c) {
      myDamping = c;
   }

   public double getDamping() {
      return myDamping;
   }

   public void setContactSpeed (double s) {
      myContactSpeed = s;
   }

   public double getContactSpeed() {
      return myContactSpeed;
   }

   public void setContactAccel (double s) {
      myContactAccel = s;
   }

   public double getContactAccel() {
      return myContactAccel;
   }

   public void setFrictionLimit (double f) {
      myFrictionLimit = f;
   }

   public double getFrictionLimit() {
      return myFrictionLimit;
   }

   public void setFriction (double f) {
      myFriction = f;
   }

   public double getFriction() {
      return myFriction;
   }

   /**
    * Sets the contact point for point contact constraints. A supplied value of
    * null clears the contact point.
    * 
    * @param pnt
    * new contact point. The supplied value is copied internally.
    */
   public void setContactPoint (Point3d pnt) {
      if (pnt == null) {
         myContactPoint = null;
      }
      else {
         if (myContactPoint == null) {
            myContactPoint = new Point3d (pnt);
         }
         else {
            myContactPoint.set (pnt);
         }
      }
   }

   /**
    * Returns the contact point for point contact constraints, or null
    * otherwise. The point is given in the coordinates of bodyA. This is
    * intended for use by solvers in computing the friction.
    * 
    * return contact point for point contacts. Should not be modfied.
    */
   public Point3d getContactPoint() {
      return myContactPoint;
   }

   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append (myWrenchA + " ");
      builder.append (myWrenchB);
      return builder.toString();
   }

   public static int getStateSize() {
      return 17;
   }

   public void getState (DataBuffer data) {

      data.dput (myWrenchA.f);
      data.dput (myWrenchA.m);
      data.dput (myWrenchB.f);
      data.dput (myWrenchB.m);
      data.dput (myContactPoint);
      data.dput (myDistance);
      data.dput (myDerivative);
   }
   
   public void setState (DataBuffer data) {
      data.dget(myWrenchA.f);
      data.dget(myWrenchA.m);
      data.dget(myWrenchB.f);
      data.dget(myWrenchB.m);
      if (myContactPoint == null) {
         myContactPoint = new Point3d();
      }
      data.dget(myContactPoint);

      myDistance = data.dget();
      myDerivative = data.dget();      
   }

   
}
