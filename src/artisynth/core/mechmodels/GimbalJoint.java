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
 * Implements a 3 DOF spherical joint parameterized by roll-pitch-yaw
 * angles. Frames C and D share a common origin, with C free to assume any
 * orientation with respect to D. This orientation is described by a rotation
 * {@code roll} about the z axis of D, followed by a rotation {@code pitch}
 * about the rotated y axis, followed by a final rotation {@code yaw} about the
 * rotated x axis. All rotations are counter-clockwise.
 *
 * <p>The {@code roll}, {@code pitch} and {@code yaw} angles are available (in
 * degrees) as properties which can be read and also, under appropriate
 * circumstances, set.  Setting these values causes an adjustment in the
 * positions of one or both bodies connected to this joint, along with adjacent
 * bodies connected to them, with preference given to bodies that are not
 * attached to ``ground''.  If this is done during simulation, and particularly
 * if one or both of the bodies connected to this joint are moving dynamically,
 * the results will be unpredictable and will likely conflict with the
 * simulation.
 */
public class GimbalJoint extends SphericalJointBase {

   public static final int ROLL_IDX = GimbalCoupling.ROLL_IDX; 
   public static final int PITCH_IDX = GimbalCoupling.PITCH_IDX; 
   public static final int YAW_IDX = GimbalCoupling.YAW_IDX; 

   public static PropertyList myProps =
      new PropertyList (GimbalJoint.class, SphericalJointBase.class);
   
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;
   
   private static DoubleInterval DEFAULT_ANGLE_RANGE =
      new DoubleInterval ("[-inf,inf])");

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
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a {@code GimbalJoint} which is not attached to any bodies.  It
    * can subsequently be connected using one of the {@code setBodies} methods.
    */
   public GimbalJoint () {
      setDefaultValues();
      GimbalCoupling coupling = new GimbalCoupling();
      //coupling.setRangeType (SphericalRpyCoupling.RPY_LIMIT);
      myCoupling = coupling;

   }

   /**
    * Creates a {@code GimbalJoint} connecting two rigid bodies, {@code bodyA}
    * and {@code bodyB}. If A and B describe the coordinate frames of {@code
    * bodyA} and {@code bodyB}, then {@code TCA} and {@code TDB} give the
    * (fixed) transforms from the joint's C and D frames to A and B,
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
   public GimbalJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   /**
    * Creates a {@code GimbalJoint} connecting two connectable bodies,
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
   public GimbalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code GimbalJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code roll}, {@code pitch} and {@code
    * yaw} with have initial values of 0. D (and C) is located by {@code TDW},
    * which gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */   
   public GimbalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code GimbalJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, so that {@code roll}, {@code pitch} and {@code yaw}
    * with have initial values of 0. D (and C) is located by {@code TDW}, which
    * gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public GimbalJoint (
      ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, null, TDW);
   }

   /**
    * Creates a {@code GimbalJoint} connecting two connectable bodies, {@code
    * bodyA} and {@code bodyB}. The joint frames D and C are assumed to be
    * initially coincident, so that {@code roll}, {@code pitch} and {@code yaw}
    * with have initial values of 0. D (and C) is located (with respect to
    * world) so that its origin is at {@code originD} and its axes are aligned
    * with the world.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B, or {@code null} if {@code bodyA} is connected
    * to ground.
    * @param originD origin of frame D (world coordinates)
    */
   public GimbalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, Point3d originD) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (originD);
      setBodies (bodyA, bodyB, TDW);
   }

   public Vector3d getRpyRad() {
      VectorNd coords = new VectorNd(3);
      getCoordinates (coords);
      Vector3d rpy = new Vector3d();
      rpy.set (coords);
      return rpy;
   }

   public void setRpyRad (Vector3d rpy) {
      VectorNd coords = new VectorNd(3);
      coords.set (rpy);
      setCoordinates (coords);
   }

   /**
    * Queries this joint's roll value, in degrees. See {@link #setRoll} for
    * more details.
    *
    * @return current roll value
    */
   public double getRoll () {
      return RTOD*getCoordinate (ROLL_IDX);
   }

   /**
    * Sets this joint's roll value, in degrees. This describes the initial
    * counter-clockwise rotation of frame C about the z axis of frame D. See
    * this class's Javadoc header for a discussion of what happens when this
    * value is set.
    *
    * @param roll new roll value
    */
   public void setRoll (double roll) {
      setCoordinate (ROLL_IDX, DTOR*roll);
   }
   
   /**
    * Queries the roll range limits for this joint, in degrees. See {@link
    * #setRollRange(DoubleInterval)} for more details.
    *
    * @return roll range limits for this joint
    */
   public DoubleInterval getRollRange () {
      return getCoordinateRangeDeg (ROLL_IDX);
   }

   /**
    * Queries the lower roll range limit for this joint, in degrees.
    *
    * @return lower roll range limit
    */
   public double getMinRoll () {
      return getMinCoordinateDeg (ROLL_IDX);
   }

   /**
    * Queries the upper roll range limit for this joint, in degrees.
    *
    * @return upper roll range limit
    */
   public double getMaxRoll () {
      return getMaxCoordinateDeg (ROLL_IDX);
   }

   /**
    * Sets the roll range limits for this joint, in degrees. The default range
    * is {@code [-inf, inf]}, which implies no limits. If roll travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range roll range limits for this joint
    */
   public void setRollRange (DoubleInterval range) {
      setCoordinateRangeDeg (ROLL_IDX, range);
   }

   /**
    * Sets the roll range limits for this joint, in degrees. This is a
    * convenience wrapper for {@link #setRollRange(DoubleInterval)}.
    *
    * @param min minimum roll value
    * @param max maximum roll value
    */
   public void setRollRange (double min, double max) {
      setRollRange (new DoubleInterval (min, max));
   }

   /**
    * Sets the upper roll range limit for this joint, in degrees. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper roll range limit
    */
   public void setMaxRoll (double max) {
      setRollRange (new DoubleInterval (getMinRoll(), max));
   }

   /**
    * Sets the lower roll range limit for this joint, in degrees. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower roll range limit
    */
   public void setMinRoll (double min) {
      setRollRange (new DoubleInterval (min, getMaxRoll()));
   }

   /**
    * Queries this joint's pitch value, in degrees. See {@link #setPitch} for
    * more details.
    *
    * @return current pitch value
    */
   public double getPitch () {
      return RTOD*getCoordinate (PITCH_IDX);
   }

   /**
    * Sets this joint's pitch value, in degrees. This describes the
    * counter-clockwise rotation of frame C about its y axis <i>after</i> the
    * roll rotation. See this class's Javadoc header for a discussion of what
    * happens when this value is set.
    *
    * @param pitch new pitch value
    */
   public void setPitch (double pitch) {
      setCoordinate (PITCH_IDX, DTOR*pitch);
   }

   /**
    * Queries the pitch range limits for this joint, in degrees. See {@link
    * #setPitchRange(DoubleInterval)} for more details.
    *
    * @return pitch range limits for this joint
    */
   public DoubleInterval getPitchRange () {
      return getCoordinateRangeDeg (PITCH_IDX);
   }

   /**
    * Queries the lower pitch range limit for this joint, in degrees.
    *
    * @return lower pitch range limit
    */
   public double getMinPitch () {
      return getMinCoordinateDeg (PITCH_IDX);
   }

   /**
    * Queries the upper pitch range limit for this joint, in degrees.
    *
    * @return upper pitch range limit
    */
   public double getMaxPitch () {
      return getMaxCoordinateDeg (PITCH_IDX);
   }

   /**
    * Sets the pitch range limits for this joint, in degrees. The default range
    * is {@code [-inf, inf]}, which implies no limits. If pitch travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range pitch range limits for this joint
    */
   public void setPitchRange (DoubleInterval range) {
      // check if will likely lead to instabilities
      double k = 0;
      while(k*180-90 >= range.getLowerBound()) {
         k--;
      }
      k++;
      
      while(k*180-90 <= range.getUpperBound()) {
         if (range.withinRange(k*180-90)) {
            //throw new RuntimeException("Pitch range leads to instabilities: " + range.toString());
            System.err.println (
               "Pitch range for " + getName()  + " contains a singularity at " +
               (k*180-90) + " \u220a "+ range.toString());
            break;
         }
         k++;
      }
      setCoordinateRangeDeg (PITCH_IDX, range);            
   }

   /**
    * Sets the pitch range limits for this joint, in degrees. This is a
    * convenience wrapper for {@link #setPitchRange(DoubleInterval)}.
    *
    * @param min minimum pitch value
    * @param max maximum pitch value
    */
   public void setPitchRange (double min, double max) {
      setPitchRange (new DoubleInterval (min, max));
   }

   /**
    * Sets the upper pitch range limit for this joint, in degrees. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper pitch range limit
    */
   public void setMaxPitch (double max) {
      setPitchRange (new DoubleInterval (getMinPitch(), max));
   }

   /**
    * Sets the lower pitch range limit for this joint, in degrees. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower pitch range limit
    */
   public void setMinPitch (double min) {
      setPitchRange (new DoubleInterval (min, getMaxPitch()));
   }

   /**
    * Queries this joint's yaw value, in degrees. See {@link #setYaw} for
    * more details.
    *
    * @return current yaw value
    */
   public double getYaw () {
      return RTOD*getCoordinate (YAW_IDX);
   }

   /**
    * Sets this joint's yaw value, in degrees. This describes the
    * counter-clockwise rotation of frame C about its y axis <i>after</i> the
    * roll rotation. See this class's Javadoc header for a discussion of what
    * happens when this value is set.
    *
    * @param yaw new yaw value
    */
   public void setYaw (double yaw) {
      setCoordinate (YAW_IDX, DTOR*yaw);
   }

   /**
    * Queries the yaw range limits for this joint, in degrees. See {@link
    * #setYawRange(DoubleInterval)} for more details.
    *
    * @return yaw range limits for this joint
    */
   public DoubleInterval getYawRange () {
      return getCoordinateRangeDeg (YAW_IDX);
   }

   /**
    * Queries the lower yaw range limit for this joint, in degrees.
    *
    * @return lower yaw range limit
    */
   public double getMinYaw () {
      return getMinCoordinateDeg (YAW_IDX);
   }

   /**
    * Queries the upper yaw range limit for this joint, in degrees.
    *
    * @return upper yaw range limit
    */
   public double getMaxYaw () {
      return getMaxCoordinateDeg (YAW_IDX);
   }

   /**
    * Sets the yaw range limits for this joint, in degrees. The default range
    * is {@code [-inf, inf]}, which implies no limits. If yaw travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range yaw range limits for this joint
    */
   public void setYawRange (DoubleInterval range) {
      setCoordinateRangeDeg (YAW_IDX, range);
   }

   /**
    * Sets the yaw range limits for this joint, in degrees. This is a
    * convenience wrapper for {@link #setYawRange(DoubleInterval)}.
    *
    * @param min minimum yaw value
    * @param max maximum yaw value
    */
   public void setYawRange (double min, double max) {
      setYawRange (new DoubleInterval (min, max));
   }

   /**
    * Sets the upper yaw range limit for this joint, in degrees. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper yaw range limit
    */
   public void setMaxYaw (double max) {
      setYawRange (new DoubleInterval (getMinYaw(), max));
   }

   /**
    * Sets the lower yaw range limit for this joint, in degrees. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower yaw range limit
    */
   public void setMinYaw (double min) {
      setYawRange (new DoubleInterval (min, getMaxYaw()));
   }
   
   public void setApplyEuler(boolean apply) {
      ((GimbalCoupling)myCoupling).applyEuler = apply;
   }
   
   public boolean getApplyEuler() {
      return ((GimbalCoupling)myCoupling).applyEuler;
   }

}
