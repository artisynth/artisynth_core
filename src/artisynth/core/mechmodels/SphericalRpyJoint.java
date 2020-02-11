/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.spatialmotion.*;
import java.util.*;
import java.io.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SphericalRpyJoint extends SphericalJointBase {

   public static PropertyList myProps =
      new PropertyList (SphericalRpyJoint.class, SphericalJointBase.class);
   
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;
   
   private static DoubleInterval DEFAULT_ANGLE_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private DoubleInterval myRollRange = new DoubleInterval(DEFAULT_ANGLE_RANGE);
   private DoubleInterval myPitchRange = new DoubleInterval(DEFAULT_ANGLE_RANGE);
   private DoubleInterval myYawRange = new DoubleInterval(DEFAULT_ANGLE_RANGE);

   static {
      myProps.add (
         "roll", "joint roll angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "pitch", "joint pitch angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "yaw", "joint yaw angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "rollRange", "range for roll", DEFAULT_ANGLE_RANGE, "%8.3f 1E");
      myProps.add (
         "pitchRange", "range for pitch", DEFAULT_ANGLE_RANGE, "%8.3f 1E");
      myProps.add (
         "yawRange", "range for yaw", DEFAULT_ANGLE_RANGE, "%8.3f 1E");
      myProps.add (
         "applyEuler", "enable/disable Euler filter", true);
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
   }

   public Vector3d getRpyRad() {
      Vector3d rpy = new Vector3d();
      RigidTransform3d TGD = null;
      if (attachmentsInitialized()) {
         // initialize TGD to TCD; it will get projected to TGD within
         TGD = new RigidTransform3d();
         getCurrentTCD (TGD);
      }
      ((SphericalCoupling)myCoupling).getRpy (rpy, TGD);
      return rpy;
   }

   public void setRpyRad (Vector3d rpy) {
      RigidTransform3d TGD = null;
      if (isConnectedToBodies()) {
         TGD = new RigidTransform3d();
      }      
      ((SphericalCoupling)myCoupling).setRpy (TGD, rpy);
      if (TGD != null) {
         // if we are connected to the hierarchy, adjust the poses of the
         // attached bodies appropriately.         
         adjustPoses (TGD);
      }
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public SphericalRpyJoint () {
      ((SphericalCoupling)myCoupling).setRangeType (SphericalCoupling.RPY_LIMIT);
   }

   public SphericalRpyJoint (RigidBody bodyA, RigidTransform3d TCA,
                             RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }

   public SphericalRpyJoint (RigidBody bodyA, RigidTransform3d TCA,
                             RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, TCA, bodyB, XDB);
   }
   
   public SphericalRpyJoint (RigidBody bodyA, RigidBody bodyB, RigidTransform3d XWJ) {
      this();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      
      TCA.mulInverseLeft(bodyA.getPose(), XWJ);
      XDB.mulInverseLeft(bodyB.getPose(), XWJ);
      
      setBodies(bodyA, TCA, bodyB, XDB);
      
   }
   
   public SphericalRpyJoint (ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TFW) {
      this();
      setBodies(bodyA, bodyB, TFW);
      
   }

   public double getRoll () {
      return Math.toDegrees (getRpyRad().z);
   }

   public void setRoll (double roll) {
      roll = myRollRange.makeValid (roll);
      Vector3d rpy = getRpyRad();
      rpy.z = Math.toRadians (roll);
      setRpyRad (rpy);
   }
   
   public DoubleInterval getRollRange () {
      return myRollRange;
   }

   public void setRollRange (DoubleInterval range) {
      SphericalCoupling coupling = (SphericalCoupling)myCoupling;
      coupling.setRollRange (
         Math.toRadians (range.getLowerBound()),
         Math.toRadians (range.getUpperBound()));
      myRollRange.set (range);
      if (isConnectedToBodies()) {
         // if we are connected to the hierarchy, might have to update theta
         double roll = getRoll();
         double clipped = myRollRange.clipToRange (roll);
         if (clipped != roll) {
            setRoll (clipped);
         }
      }      
   }

   public void setRollRange (double min, double max) {
      setRollRange (new DoubleInterval (min, max));
   }

   public double getPitch () {
      return Math.toDegrees (getRpyRad().y);
   }

   public void setPitch (double pitch) {
      pitch = myPitchRange.makeValid (pitch);
      Vector3d rpy = getRpyRad();
      rpy.y = Math.toRadians (pitch);
      setRpyRad (rpy);
   }

   public DoubleInterval getPitchRange () {
      return myPitchRange;
   }

   public void setPitchRange (DoubleInterval range) {
      SphericalCoupling coupling = (SphericalCoupling)myCoupling;
      
      // check if will likely lead to instabilities
      double k = 0;
      while(k*180-90 >= range.getLowerBound()) {
         k--;
      }
      k++;
      
      while(k*180-90 <= range.getUpperBound()) {
         if (range.withinRange(k*180-90)) {
            //throw new RuntimeException("Pitch range leads to instabilities: " + range.toString());
            System.err.println("Pitch range for " + getName()  + " contains a singularity at " +
               (k*180-90) + " \u220a "+ range.toString());
         }
         k++;
      }
            
      coupling.setPitchRange (
         Math.toRadians (range.getLowerBound()),
         Math.toRadians (range.getUpperBound()));
      myPitchRange.set (range);
      if (isConnectedToBodies()) {
         // if we are connected to the hierarchy, might have to update theta
         double pitch = getPitch();
         double clipped = myPitchRange.clipToRange (pitch);
         if (clipped != pitch) {
            setPitch (clipped);
         }
      }      
   }

   public void setPitchRange (double min, double max) {
      setPitchRange (new DoubleInterval (min, max));
   }

   public double getYaw () {
      return Math.toDegrees (getRpyRad().x);
   }

   public void setYaw (double yaw) {
      yaw = myYawRange.makeValid (yaw);
      Vector3d rpy = getRpyRad();
      rpy.x = Math.toRadians (yaw);
      setRpyRad (rpy);
   }

   public DoubleInterval getYawRange () {
      return myYawRange;
   }

   public void setYawRange (DoubleInterval range) {
      SphericalCoupling coupling = (SphericalCoupling)myCoupling;
      coupling.setYawRange (
         Math.toRadians (range.getLowerBound()),
         Math.toRadians (range.getUpperBound()));
      myYawRange.set (range);
      if (isConnectedToBodies()) {
         // if we are connected to the hierarchy, might have to update theta
         double yaw = getYaw();
         double clipped = myYawRange.clipToRange (yaw);
         if (clipped != yaw) {
            setYaw (clipped);
         }
      }      
   }

   public void setYawRange (double min, double max) {
      setYawRange (new DoubleInterval (min, max));
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SphericalRpyJoint copy = (SphericalRpyJoint)super.copy (flags, copyMap);
      copy.myRollRange = new DoubleInterval(myRollRange);
      copy.myPitchRange = new DoubleInterval(myPitchRange);
      copy.myYawRange = new DoubleInterval(myYawRange);
      return copy;
   }
   
   public void setApplyEuler(boolean apply) {
      ((SphericalCoupling)myCoupling).applyEuler = apply;
   }
   
   public boolean getApplyEuler() {
      return ((SphericalCoupling)myCoupling).applyEuler;
   }

}
