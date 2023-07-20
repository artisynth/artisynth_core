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
import artisynth.core.mechmodels.GimbalJoint.AxisSet;
import java.util.*;
import java.io.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Implements a six DOF coupling that allows complete motion in space, but with
 * translational and rotational limits. The motion is parameterized by six
 * coordinates in the form of three translations along and three intrinsic
 * rotations about the X, Y, Z axes. The motion is constrained only if bounds
 * are set on the coordinates.
 *
 * <p>The {@code x}, {@code y}, {@code z}, {@code roll}, {@code pitch} and
 * {@code yaw} coordinates are available as properties which can be read and
 * also, under appropriate circumstances, set. The angle properties are
 * reported in degrees. Setting these values causes an adjustment in the
 * positions of one or both bodies connected to this joint, along with adjacent
 * bodies connected to them, with preference given to bodies that are not
 * attached to ``ground''.  If this is done during simulation, and particularly
 * if one or both of the bodies connected to this joint are moving dynamically,
 * the results will be unpredictable and will likely conflict with the
 * simulation.
 */
public class FreeJoint extends JointBase {

   public static final int X_IDX = FreeCoupling.X_IDX; 
   public static final int Y_IDX = FreeCoupling.Y_IDX; 
   public static final int Z_IDX = FreeCoupling.Z_IDX; 

   public static final int ROLL_IDX = FreeCoupling.ROLL_IDX; 
   public static final int PITCH_IDX = FreeCoupling.PITCH_IDX; 
   public static final int YAW_IDX = FreeCoupling.YAW_IDX; 

   public static PropertyList myProps =
      new PropertyList (FreeJoint.class, JointBase.class);
   
   private static double DOUBLE_PREC = 2.220446049250313e-16;
   private static double EPSILON = 10 * DOUBLE_PREC;
   
   private static DoubleInterval DEFAULT_COORD_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static boolean DEFAULT_X_LOCKED = false;
   private static boolean DEFAULT_Y_LOCKED = false;
   private static boolean DEFAULT_Z_LOCKED = false;
   private static boolean DEFAULT_ROLL_LOCKED = false;
   private static boolean DEFAULT_PITCH_LOCKED = false;
   private static boolean DEFAULT_YAW_LOCKED = false;

   static {
      myProps.add ("x", "x translation", 0);
      myProps.add ("y", "y translation", 0);
      myProps.add ("z", "z translation", 0);
      myProps.add (
         "roll", "joint roll angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "pitch", "joint pitch angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "yaw", "joint yaw angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add ("xRange", "range for x", DEFAULT_COORD_RANGE);
      myProps.add ("yRange", "range for y", DEFAULT_COORD_RANGE);
      myProps.add ("zRange", "range for z", DEFAULT_COORD_RANGE);
      myProps.add (
         "rollRange", "range for roll", DEFAULT_COORD_RANGE, "%8.3f 1E");
      myProps.add (
         "pitchRange", "range for pitch", DEFAULT_COORD_RANGE, "%8.3f 1E");
      myProps.add (
         "yawRange", "range for yaw", DEFAULT_COORD_RANGE, "%8.3f 1E");
      myProps.add (
         "xLocked isXLocked",
         "set whether x is locked", DEFAULT_X_LOCKED);
      myProps.add (
         "yLocked isYLocked",
         "set whether y is locked", DEFAULT_Y_LOCKED);
      myProps.add (
         "zLocked isZLocked",
         "set whether z is locked", DEFAULT_Z_LOCKED);
      myProps.add (
         "rollLocked isRollLocked",
         "set whether the roll coordinate is locked", DEFAULT_ROLL_LOCKED);
      myProps.add (
         "pitchLocked isPitchLocked",
         "set whether the pitch coordinate is locked", DEFAULT_PITCH_LOCKED);
      myProps.add (
         "yawLocked isYawLocked",
         "set whether the yaw coordinate is locked", DEFAULT_YAW_LOCKED);
      myProps.add (
         "applyEuler", "enable/disable Euler filter", true);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public AxisSet getAxes () {
      return AxisSet.getAxes(((FreeCoupling)myCoupling).getAxes());
   }

   protected void setAxes (AxisSet axes) {
      ((FreeCoupling)myCoupling).setAxes (axes.getCouplingAxes());
   }

   /**
    * Creates a {@code FreeJoint} which is not attached to any bodies.  It
    * can subsequently be connected using one of the {@code setBodies} methods.
    */
   public FreeJoint () {
      setDefaultValues();
      setCoupling (new FreeCoupling());
   }

   public FreeJoint (AxisSet axes) {
      setDefaultValues();
      setCoupling (new FreeCoupling(axes.getCouplingAxes()));
   }

   /**
    * Creates a {@code FreeJoint} connecting two rigid bodies, {@code bodyA}
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
   public FreeJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   /**
    * Creates a {@code FreeJoint} connecting two connectable bodies,
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
   public FreeJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code FreeJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code roll}, {@code pitch} and {@code
    * yaw} with have initial values of 0. D (and C) is located by {@code TDW},
    * which gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */   
   public FreeJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code FreeJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, so that {@code roll}, {@code pitch} and {@code yaw}
    * with have initial values of 0. D (and C) is located by {@code TDW}, which
    * gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public FreeJoint (
      ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, null, TDW);
   }

   /**
    * Creates a {@code FreeJoint} connecting two connectable bodies, {@code
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
   public FreeJoint (
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
    * Queries this joint's x value. See {@link #setX} for more details.
    *
    * @return current x value
    */
   public double getX () {
      return getCoordinate (X_IDX);
   }

   /**
    * Sets this joint's x value. This describes the translation of frame C
    * along the x axis of frame D. See this class's Javadoc header for a
    * discussion of what happens when this value is set.
    *
    * @param x new x value
    */
   public void setX (double x) {
      setCoordinate (X_IDX, x);
   }
   
   /**
    * Queries the x range limits for this joint. See {@link
    * #setXRange(DoubleInterval)} for more details.
    *
    * @return x range limits for this joint
    */
   public DoubleInterval getXRange () {
      return getCoordinateRange (X_IDX);
   }

   /**
    * Sets the x range limits for this joint. The default range
    * is {@code [-inf, inf]}, which implies no limits. If x travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range x range limits for this joint
    */
   public void setXRange (DoubleInterval range) {
      setCoordinateRange (X_IDX, range);
   }

   /**
    * Queries whether the x coordinate for this joint is locked.
    *
    * @return {@code true} if x is locked
    */
   public boolean isXLocked() {
      return isCoordinateLocked (X_IDX);
   }

   /**
    * Set whether the x coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks x
    */
   public void setXLocked (boolean locked) {
      setCoordinateLocked (X_IDX, locked);
   }

   /**
    * Queries this joint's y value. See {@link #setY} for more details.
    *
    * @return current y value
    */
   public double getY () {
      return getCoordinate (Y_IDX);
   }

   /**
    * Sets this joint's y value. This describes the translation of frame C
    * along the y axis of frame D. See this class's Javadoc header for a
    * discussion of what happens when this value is set.
    *
    * @param y new y value
    */
   public void setY (double y) {
      setCoordinate (Y_IDX, y);
   }
   
   /**
    * Queries the y range limits for this joint. See {@link
    * #setYRange(DoubleInterval)} for more details.
    *
    * @return y range limits for this joint
    */
   public DoubleInterval getYRange () {
      return getCoordinateRange (Y_IDX);
   }

   /**
    * Sets the y range limits for this joint. The default range
    * is {@code [-inf, inf]}, which implies no limits. If y travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range y range limits for this joint
    */
   public void setYRange (DoubleInterval range) {
      setCoordinateRange (Y_IDX, range);
   }

   /**
    * Queries whether the y coordinate for this joint is locked.
    *
    * @return {@code true} if y is locked
    */
   public boolean isYLocked() {
      return isCoordinateLocked (Y_IDX);
   }

   /**
    * Set whether the y coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks y
    */
   public void setYLocked (boolean locked) {
      setCoordinateLocked (Y_IDX, locked);
   }

   /**
    * Queries this joint's z value. See {@link #setZ} for more details.
    *
    * @return current z value
    */
   public double getZ () {
      return getCoordinate (Z_IDX);
   }

   /**
    * Sets this joint's z value. This describes the translation of frame C
    * along the z axis of frame D. See this class's Javadoc header for a
    * discussion of what happens when this value is set.
    *
    * @param z new z value
    */
   public void setZ (double z) {
      setCoordinate (Z_IDX, z);
   }
   
   /**
    * Queries the z range limits for this joint. See {@link
    * #setZRange(DoubleInterval)} for more details.
    *
    * @return z range limits for this joint
    */
   public DoubleInterval getZRange () {
      return getCoordinateRange (Z_IDX);
   }

   /**
    * Sets the z range limits for this joint. The default range
    * is {@code [-inf, inf]}, which implies no limits. If z travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range z range limits for this joint
    */
   public void setZRange (DoubleInterval range) {
      setCoordinateRange (Z_IDX, range);
   }

   /**
    * Queries whether the z coordinate for this joint is locked.
    *
    * @return {@code true} if z is locked
    */
   public boolean isZLocked() {
      return isCoordinateLocked (Z_IDX);
   }

   /**
    * Set whether the z coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks z
    */
   public void setZLocked (boolean locked) {
      setCoordinateLocked (Z_IDX, locked);
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
    * Queries whether the roll coordinate for this joint is locked.
    *
    * @return {@code true} if roll is locked
    */
   public boolean isRollLocked() {
      return isCoordinateLocked (ROLL_IDX);
   }

   /**
    * Set whether the roll coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks roll
    */
   public void setRollLocked (boolean locked) {
      setCoordinateLocked (ROLL_IDX, locked);
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
    * Queries whether the pitch coordinate for this joint is locked.
    *
    * @return {@code true} if pitch is locked
    */
   public boolean isPitchLocked() {
      return isCoordinateLocked (PITCH_IDX);
   }

   /**
    * Set whether the pitch coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks pitch
    */
   public void setPitchLocked (boolean locked) {
      setCoordinateLocked (PITCH_IDX, locked);
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
   
   /**
    * Queries whether the yaw coordinate for this joint is locked.
    *
    * @return {@code true} if yaw is locked
    */
   public boolean isYawLocked() {
      return isCoordinateLocked (YAW_IDX);
   }

   /**
    * Set whether the yaw coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks yaw
    */
   public void setYawLocked (boolean locked) {
      setCoordinateLocked (YAW_IDX, locked);
   }
   
   public void setApplyEuler(boolean apply) {
      ((FreeCoupling)myCoupling).setApplyEuler (apply);
   }
   
   public boolean getApplyEuler() {
      return ((FreeCoupling)myCoupling).getApplyEuler();
   }

   // need to implement write and scan so we can handle the 'axes' setting
   // properly

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (getAxes() != AxisSet.ZYX) {
         pw.print ("axes=" + getAxes());
      }
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "axes")) {
         setAxes (rtok.scanEnum (AxisSet.class));
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

}
