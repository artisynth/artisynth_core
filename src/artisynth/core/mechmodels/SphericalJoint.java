/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.properties.*;
import maspack.spatialmotion.*;

import java.util.*;

import artisynth.core.modelbase.*;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SphericalJoint extends SphericalJointBase {

   public static PropertyList myProps =
      new PropertyList (SphericalJoint.class, SphericalJointBase.class);

   protected static Vector3d myDefaultMaxRotation =
      new Vector3d (180.0, 180.0, 180.0);

   protected static VectorNd ZERO_VEC = new VectorNd(6);

   static {
      myProps.add (
         "maxRotation", "maximum x, y, z rotation (degrees)",
         myDefaultMaxRotation);
      myProps.add (
         "rotationLimited isRotationLimited", "enables rotation limits", false);
      myProps.add (
         "maxTilt", "maximum tilt angle (degrees)", 180.0);
      myProps.add (
         "tiltLimited isTiltLimited", "enables tilt limits", false);
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
   }

   public boolean isTiltLimited() {
      SphericalCoupling coupling = (SphericalCoupling)myCoupling;
      return coupling.getRangeType() == SphericalCoupling.TILT_LIMIT;
   }

   public void setTiltLimited (boolean enable) {
      if (isTiltLimited() != enable) {
         SphericalCoupling coupling = (SphericalCoupling)myCoupling;
         if (enable) {
            coupling.setRangeType (SphericalCoupling.TILT_LIMIT);
         }
         else {
            coupling.setRangeType (0);
         }
      }
   }

   public boolean isRotationLimited() {
      SphericalCoupling coupling = (SphericalCoupling)myCoupling;
      return coupling.getRangeType() == SphericalCoupling.ROTATION_LIMIT;
   }

   public void setRotationLimited (boolean enable) {
      if (isRotationLimited() != enable) {
         SphericalCoupling coupling = (SphericalCoupling)myCoupling;
         if (enable) {
            coupling.setRangeType (SphericalCoupling.ROTATION_LIMIT);
         }
         else {
            coupling.setRangeType (0);
         }
      }
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public SphericalJoint() {
   }

   public SphericalJoint (RigidBody bodyA, RigidTransform3d TCA,
                          RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }

   public SphericalJoint (RigidBody bodyA, RigidTransform3d TCA,
                          RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, TCA, bodyB, XDB);
   }
   
   public SphericalJoint (RigidBody bodyA, Point3d worldPnt) {
      this();
      
      // world position/orientation of joint
      RigidTransform3d XWJ =  new RigidTransform3d();
      XWJ.setTranslation(worldPnt);
      XWJ.setRotation(new AxisAngle(1,0,0,0));
      
      RigidTransform3d TCA = new RigidTransform3d();      
      TCA.mulInverseLeft(bodyA.getPose(), XWJ);
      
      setBodies(bodyA, TCA, null, XWJ);
   }
   
   public SphericalJoint (RigidBody bodyA, RigidBody bodyB, Point3d worldPnt ) {
      this();
      
      // world position/orientation of joint
      RigidTransform3d XWJ =  new RigidTransform3d();
      XWJ.setTranslation(worldPnt);
      XWJ.setRotation(new AxisAngle(1,0,0,0));
      
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      
      TCA.mulInverseLeft(bodyA.getPose(), XWJ);
      XDB.mulInverseLeft(bodyB.getPose(), XWJ);
      
      setBodies(bodyA, TCA, bodyB, XDB);
   }
   
   public SphericalJoint (RigidBody bodyA, RigidBody bodyB, RigidTransform3d XWJ) {
      this();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      
      TCA.mulInverseLeft(bodyA.getPose(), XWJ);
      XDB.mulInverseLeft(bodyB.getPose(), XWJ);
      
      setBodies(bodyA, TCA, bodyB, XDB);
      
   }

   public void setMaximumTilt (double max) {
      ((SphericalCoupling)myCoupling).setMaximumTilt (max);
      ((SphericalCoupling)myCoupling).setRangeType (
         SphericalCoupling.TILT_LIMIT);
   }

   public double getMaximumTilt() {
      return ((SphericalCoupling)myCoupling).getMaximumTilt();
   }

   public void setMaxTilt (double max) {
      setMaximumTilt (Math.toRadians(max));
   }

   public double getMaxTilt() {
      return Math.toDegrees(((SphericalCoupling)myCoupling).getMaximumTilt());
   }

   public void setMaxRotation (Vector3d maxRot) {
      setMaxRotation (maxRot.x, maxRot.y, maxRot.z);
   }
   
   public void setMaxRotation (double maxx, double maxy, double maxz) {
      ((SphericalCoupling)myCoupling).setMaximumRotation (
         Math.toRadians(maxx), Math.toRadians(maxy), Math.toRadians(maxz));
      ((SphericalCoupling)myCoupling).setRangeType (
         SphericalCoupling.ROTATION_LIMIT);
   }

   public void setMaxRotation (double max) {
      setMaxRotation (max, max, max);
   }

   public Vector3d getMaxRotation() {
      double maxRot[] = new double[3];
      ((SphericalCoupling)myCoupling).getMaximumRotation (maxRot);
      return new Vector3d (
         Math.toDegrees(maxRot[0]),
         Math.toDegrees(maxRot[1]),
         Math.toDegrees(maxRot[2]));
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SphericalJoint copy = (SphericalJoint)super.copy (flags, copyMap);
      return copy;
   }

}
