/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.Map;

import maspack.matrix.*;
import maspack.properties.*;
import maspack.spatialmotion.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.Frame;

/**
 * Instance of Frame for FemModels
 */
public class FemModelFrame extends Frame {

   public static PropertyList myProps =
      new PropertyList (FemModelFrame.class, Frame.class);

   protected SpatialInertia mySpatialInertia;

   static {
      myProps.addReadOnly ("mass", "mass of the body");
      myProps.addReadOnly (
         "inertia getRotationalInertia",
         "rotational inertia of this body");
      myProps.addReadOnly (
         "centerOfMass", "center of mass of this body");
      myProps.add (
         "dynamic isDynamic", "true if component is dynamic (non-parametric)",
         true);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemModelFrame (String name) {
      super();
      mySpatialInertia = new SpatialInertia();
      setName (name);
      setDynamic (true);
   }

   public double getMass (double t) {
      return getMass();
   }

   public void getMass (Matrix M, double t) {
      if (M instanceof Matrix6d) {
         mySpatialInertia.getRotated ((Matrix6d)M, getPose().R);
      }
      else {
         throw new IllegalArgumentException (
            "Matrix not instance of Matrix6d");
      }
   }
   
   public int getEffectiveMassForces (VectorNd f, double t, int idx) {
      Twist bodyVel = new Twist();
      Wrench cforce = new Wrench();
      bodyVel.inverseTransform (getPose().R, myState.getVelocity());
      mySpatialInertia.coriolisForce (cforce, bodyVel);
      cforce.transform (getPose().R);

      double[] buf = f.getBuffer();
      buf[idx++] = -cforce.f.x;
      buf[idx++] = -cforce.f.y;
      buf[idx++] = -cforce.f.z;
      buf[idx++] = -cforce.m.x;
      buf[idx++] = -cforce.m.y;
      buf[idx++] = -cforce.m.z;
      return idx;
   }

   public void getInverseMass (Matrix Minv, Matrix M) {
      if (!(Minv instanceof Matrix6d)) {
         throw new IllegalArgumentException ("Minv not instance of Matrix6d");
      }
      if (!(M instanceof Matrix6d)) {
         throw new IllegalArgumentException ("M not instance of Matrix6d");
      }
      SpatialInertia.invert ((Matrix6d)Minv, (Matrix6d)M);
   }

   /**
    * Returns the mass of this body.
    */
   public double getMass() {
      return mySpatialInertia.getMass();
   }

   /**
    * Returns the rotational inertia of this body (in body coordinates).
    * 
    * @return rotational inertia (should not be modified).
    */
   public SymmetricMatrix3d getRotationalInertia() {
      return mySpatialInertia.getRotationalInertia();
   }

   /**
    * Returns the center of mass of this body (in body coordinates).
    * 
    * @return center of mass (should not be modified).
    */
   public Point3d getCenterOfMass() {
      return mySpatialInertia.getCenterOfMass();
   }

   public void setInertia (SpatialInertia M) {
      mySpatialInertia.set (M);
   }

   public void getInertia (SpatialInertia M) {
      M.set (mySpatialInertia);
   }

   public void setDynamic (boolean dynamic) {
      super.setDynamic (dynamic);
   }

   private FemModel3d getFem() {
      ModelComponent parent = getParent();
      if (parent instanceof FemModel3d) {
         return (FemModel3d)parent;
      }
      else {
         return null;
      }
   }

   protected void updatePosState() {
      FemModel3d fem = getFem();
      if (fem != null) {
         if (fem.usingAttachedRelativeFrame()) {
            ComponentList<FemNode3d> nodes = fem.getNodes();
            for (int i=0; i<nodes.size(); i++) {
               nodes.get(i).updatePosState();
            }
         }
      }
   }

   protected void updateVelState() {
      FemModel3d fem = getFem();
      if (fem != null) {
         if (fem.usingAttachedRelativeFrame()) {
            ComponentList<FemNode3d> nodes = fem.getNodes();
            for (int i=0; i<nodes.size(); i++) {
               nodes.get(i).updateVelState();
            }
         }
      }
   }

   @Override
   public FemModelFrame copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemModelFrame comp = (FemModelFrame)super.copy (flags, copyMap);

      comp.setDynamic (isDynamic());
      comp.mySpatialInertia = new SpatialInertia (mySpatialInertia);
      return comp;
   }


}
