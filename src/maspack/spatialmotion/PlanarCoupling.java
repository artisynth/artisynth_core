/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;

public class PlanarCoupling extends RigidBodyCoupling {
   private boolean myUnilateral = false;
   private Vector3d myNrm = new Vector3d(0, 0, 1);

   public void setUnilateral (boolean unilateral) {
      if (myUnilateral != unilateral) {
         getConstraint(0).setUnilateral (unilateral);
         myUnilateral = unilateral;
      }
   }

   public boolean isUnilateral() {
      return myUnilateral;
   }

   public PlanarCoupling() {
      super();
   }

   public void getNormal (Vector3d nrm) {
      nrm.set (myNrm);
   }

   /**
    * Returns the normal associated with this coupling. Should not be modified.
    */
   public Vector3d getNormal () {
      return myNrm;
   }

   public void setNormal (Vector3d nrm) {
      myNrm.set (nrm);
   }

//   public PlanarCoupling (RigidTransform3d TCA, RigidTransform3d XDB) {
//      this();
//      setXFA (TCA);
//      setXDB (XDB);
//   }

   @Override
   public int numUnilaterals() {
      return myUnilateral ? 1 : 0;
   }

   @Override
   public int numBilaterals() {
      return myUnilateral ? 0 : 1;
   }

   @Override
   public void projectToConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
      TGD.set (TCD);
      TGD.p.scaledAdd (-TGD.p.dot (myNrm), myNrm);
   }

   public void initializeConstraints () {
      if (!myUnilateral){
         addConstraint (BILATERAL|LINEAR);
      }
      else {
         addConstraint (LINEAR);
      }
   }

   @Override
   public void updateConstraints (
      RigidTransform3d TGD, RigidTransform3d TCD, Twist errC, 
      Twist velGD, boolean updateEngaged) {
      
      Vector3d nrmC = new Vector3d();
      nrmC.inverseTransform (TGD.R, myNrm);

      RigidBodyConstraint cons = getConstraint(0);
      cons.wrenchG.m.setZero();
      cons.wrenchG.f.set (nrmC);
      double d = cons.wrenchG.dot (errC);
      if (updateEngaged && myUnilateral) {
         updateEngaged (cons, d, 0, INF, velGD);
      }
      cons.distance = d;
      cons.dotWrenchG.setZero();
   }

   public void transformGeometry (
      GeometryTransformer gt, RigidTransform3d TCW, RigidTransform3d TDW) {
      
      Vector3d nrm = new Vector3d (myNrm);
      RotationMatrix3d RDW = new RotationMatrix3d(TDW.R);
      
      // transform normal into world coordinates and create the corresponding
      // plane
      nrm.transform (RDW);
      Plane plane = new Plane (nrm, nrm.dot(TDW.p));
      // apply the world coordinate-based transform to the plane
      gt.transform (plane, TDW.p);
      nrm.set (plane.getNormal());
      // rotate the normal base to the new (transformed) D coordinates
      gt.transform (RDW, TDW.p);
      nrm.inverseTransform (RDW);

      myNrm.set (nrm);
   }

   /**
    * {@inheritDoc}
    */
   public void coordinatesToTCD (
      RigidTransform3d TCD, VectorNd coords) {
      TCD.setIdentity();
   }

   public PlanarCoupling clone() {
      PlanarCoupling copy = (PlanarCoupling)super.clone();

      copy.myNrm = new Vector3d(myNrm);
      return copy;
   }

}
