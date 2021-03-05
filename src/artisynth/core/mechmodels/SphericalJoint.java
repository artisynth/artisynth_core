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
 * Implements a 3 DOF spherical joint in which frames C and D share an origin
 * and C is free to assume any orientation with respect to D. This class does
 * not provide any coordinates to represent the orientation, but it does allow
 * for bounds to be set on C's orientation with respect to D. These bounds may
 * take the form of either a maximum rotation, as described for {@link
 * #setMaxRotation(double,double,double)}, or a maximum tilt, as described
 * for {@link #setMaxTilt(double)}.
 */
public class SphericalJoint extends SphericalJointBase {

   public static PropertyList myProps =
      new PropertyList (SphericalJoint.class, SphericalJointBase.class);

   protected static Vector3d myDefaultMaxRotation =
      new Vector3d (180.0, 180.0, 180.0);

   protected static VectorNd ZERO_VEC4 = new VectorNd(4);

   static {
      myProps.add (
         "maxRotation", "maximum x, y, z rotation (degrees)",
         myDefaultMaxRotation);
      myProps.add (
         "rotationLimited isRotationLimited", "enables rotation limits", false);
      myProps.addReadOnly (
         "tilt", "tilt angle between z axes (degrees)");
      myProps.add (
         "maxTilt", "maximum tilt angle (degrees)", 180.0);
      myProps.add (
         "tiltLimited isTiltLimited", "enables tilt limits", false);
      myProps.get ("compliance").setDefaultValue (ZERO_VEC4);
      myProps.get ("damping").setDefaultValue (ZERO_VEC4);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a {@code SphericalJoint} which is not attached to any bodies.  It
    * can subsequently be connected using one of the {@code setBodies} methods.
    */
   public SphericalJoint() {
      setDefaultValues();
      myCoupling = new SphericalCoupling ();
   }

   /**
    * Creates a {@code SphericalJoint} connecting two rigid bodies, {@code
    * bodyA} and {@code bodyB}. If A and B describe the coordinate frames of
    * {@code bodyA} and {@code bodyB}, then {@code TCA} and {@code TDB} give
    * the (fixed) transforms from the joint's C and D frames to A and B,
    * respectively. Since C and D are specified independently, the joint
    * transform TCD may not necessarily be initialized to the identity.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground, with {@code TDB} then being the same as {@code
    * TDW}.
    *
    * @param bodyA rigid body A
    * @param TCA transform from joint frame C to body frame A
    * @param bodyB rigid body B (or {@code null})
    * @param TDB transform from joint frame D to body frame B
    */
   public SphericalJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   /**
    * Creates a {@code SphericalJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames C and D are located
    * independently with respect to world coordinates by {@code TCW} and {@code
    * TDW}.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B (or {@code null})
    * @param TCW initial transform from joint frame C to world
    * @param TDW initial transform from joint frame D to world
    */
   public SphericalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code SphericalJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, with D (and C) is located by {@code TDW}, which
    * gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */   
   public SphericalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code SphericalJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, with D (and C) is located by {@code TDW}, which
    * gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public SphericalJoint (
      ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, null, TDW);
   }

   /**
    * Creates a {@code SphericalJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, with D (and C) is located (with respect to
    * world) so that its origin is at {@code pointD} and its orientation is
    * aligned with the world.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B, or {@code null} if {@code bodyA} is connected
    * to ground.
    * @param originD origin of frame D (world coordinates)
    */
   public SphericalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, Point3d originD) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (originD);
      setBodies (bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code SphericalJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, with D (and C) is located (with respect to world)
    * so that its origin is at {@code originD} and its orientation is aligned
    * with the world.
    *
    * @param bodyA body A
    * @param originD origin of frame D (world coordinates)
    */
   public SphericalJoint (ConnectableBody bodyA, Point3d originD) {
      this (bodyA, null, originD);
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

   public double getTilt() {
      RigidTransform3d TCD = null;
      if (attachmentsInitialized()) {
         // get TCD for estimating coordinates
         TCD = new RigidTransform3d();
         getCurrentTCD (TCD);
      }
      return RTOD*((SphericalCoupling)myCoupling).getTilt (TCD);
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
}
