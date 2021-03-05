/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.util.Map;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.Point3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.spatialmotion.UniversalCoupling;
import maspack.util.DoubleInterval;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Implements a 2 DOF rotary joint, which allows frame C to rotate with respect
 * to frame D by a rotation {@code roll} about the z axis, followed by a
 * rotation {@code pitch} about the rotated y axis. All rotations are
 * counter-clockwise.
 *
 * <p>The {@code roll} and {@code pitch} angles are available (in degrees) as
 * properties which can be read and also, under appropriate circumstances, set.
 * Setting these values causes an adjustment in the positions of one or both
 * bodies connected to this joint, along with adjacent bodies connected to
 * them, with preference given to bodies that are not attached to ``ground''.
 * If this is done during simulation, and particularly if one or both of the
 * bodies connected to this joint are moving dynamically, the results will be
 * unpredictable and will likely conflict with the simulation.
 */
public class UniversalJoint extends JointBase implements CopyableComponent {

   public static final int ROLL_IDX = UniversalCoupling.ROLL_IDX; 
   public static final int PITCH_IDX = UniversalCoupling.PITCH_IDX; 

   public static final double DEFAULT_JOINT_RADIUS = 0;
   protected double myJointRadius = DEFAULT_JOINT_RADIUS;

   private static DoubleInterval DEFAULT_ANGLE_RANGE =
      new DoubleInterval ("[-inf,inf])");

   public static PropertyList myProps =
      new PropertyList (UniversalJoint.class, JointBase.class);

   static {
      myProps.add (
         "roll", "joint roll angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "pitch", "joint pitch angle (degrees)", 0, "%8.3f 1E [-360,360]");
      myProps.add (
         "rollRange", "range for roll", DEFAULT_ANGLE_RANGE, "%8.3f 1E");
      myProps.add (
         "pitchRange", "range for pitch", DEFAULT_ANGLE_RANGE, "%8.3f 1E");
      myProps.add (
         "jointRadius",
         "radius used for rendering the joint", DEFAULT_JOINT_RADIUS);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a {@code UniversalJoint} which is not attached to any bodies.  It
    * can subsequently be connected using one of the {@code setBodies} methods.
    */
   public UniversalJoint() {
      setDefaultValues();
      setCoupling (new UniversalCoupling());
      setRollRange (DEFAULT_ANGLE_RANGE);
      setPitchRange (DEFAULT_ANGLE_RANGE);
   }

   /**
    * Creates a {@code UniversalJoint} connecting two rigid bodies, {@code
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
   public UniversalJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }
   
   /**
    * Creates a {@code UniversalJoint} connecting two connectable bodies,
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
   public UniversalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code UniversalJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code roll} and {@code pitch} will have
    * initial values of 0. D (and C) is located by {@code TDW}, which gives the
    * transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */
   public UniversalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code UniversalJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, so that {@code roll} and {@code pitch} will have
    * initial values of 0. D (and C) is located by {@code TDW}, which gives the
    * transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public UniversalJoint (
      ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, null, TDW);
   }

   /**
    * Returns the roll and pitch angles for this joint, in radians.  See {@link
    * #setRoll} and {@link #setPitch} for the definition of these angles.
    *
    * @return roll and pitch angles, in an array of length 2
    */
   public double[] getRollPitchRad() {
      VectorNd coords = new VectorNd(2);
      getCoordinates (coords);
      double[] angs = new double[2];
      angs[0] = coords.get(0);
      angs[1] = coords.get(1);
      return angs;
   }

   /**
    * Sets the roll and pitch angles for this joint, in radians.  See {@link
    * #setRoll} and {@link #setPitch} for the definition of these angles and
    * what happens when they are set.
    *
    * @param angs new roll and pitch angles, in an array of length 2
    */
   public void setRollPitchRad (double[] angs) {
      VectorNd coords = new VectorNd(2);
      coords.set (0, angs[0]);
      coords.set (1, angs[1]);
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
    * Returns a radius used for rendering this joint as a sphere. See {@link
    * #getJointRadius} for details.
    *
    * @return joint rendering radius
    */
   public double getJointRadius() {
      return myJointRadius;
   }

   /**
    * Sets a radius used for rendering this joint as a sphere.  The default
    * value is 0. Setting a value of -1 will invoke a legacy rendering method,
    * in which the joint is rendered using point rendering properties.
    *
    * @param r joint rendering radius
    */
   public void setJointRadius (double r) {
      myJointRadius = r;
   }

   /* --- begin Renderable implementation --- */

   private void computeRollAxisEndPoints (Point3d p0, Point3d p1, double slen) {
      RigidTransform3d TJW;
      if (((UniversalCoupling)myCoupling).getUseRDC()) {
         TJW = myRenderFrameC;
      }
      else {
         TJW = myRenderFrameD;
      }
      Vector3d uW = new Vector3d(); // joint axis vector in world coords
      // first set p0 to contact center in world coords
      p0.set (TJW.p);
      // now get axis unit vector in world coords
      uW.set (TJW.R.m02, TJW.R.m12, TJW.R.m22);
      p0.scaledAdd (-0.5 * slen, uW, p0);
      p1.scaledAdd (slen, uW, p0);
   }

   private void computePitchAxisEndPoints (Point3d p0, Point3d p1, double slen) {
      RigidTransform3d TJW;
      if (((UniversalCoupling)myCoupling).getUseRDC()) {
         TJW = myRenderFrameD;
      }
      else {
         TJW = myRenderFrameC;
      }
      Vector3d uW = new Vector3d(); // joint axis vector in world coords
      // first set p0 to contact center in world coords
      p0.set (TJW.p);
      // now get axis unit vector in world coords

      double skewAng = ((UniversalCoupling)myCoupling).getSkewAngle();
      if (skewAng != 0) {
         RotationMatrix3d R = TJW.R;
         double sa = Math.sin(skewAng);
         double ca = Math.cos(skewAng);
         // find pitch axis by rotating RDW about its x axis by skewAngke
         uW.set (
            ca*R.m01 + sa*R.m02, ca*R.m11 + sa*R.m12, ca*R.m21 + sa*R.m22);
      }
      else {
         uW.set (TJW.R.m01, TJW.R.m11, TJW.R.m21);
      }
      p0.scaledAdd (-0.5 * slen, uW, p0);
      p1.scaledAdd (slen, uW, p0);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      super.updateBounds (pmin, pmax);
      double rad = Math.max (getEffectiveShaftLength()/2, myJointRadius);
      if (rad > 0) {
         Vector3d center = getCurrentTDW().p;
         RenderableUtils.updateSphereBounds (pmin, pmax, center, rad);
      }
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      Vector3d center = myRenderFrameD.p;
      float[] coords0 = 
         new float[] { (float)center.x, (float)center.y, (float)center.z };

      if (myJointRadius < 0) {
         // legacy rendering as a point
         renderer.drawPoint (myRenderProps, coords0, isSelected());
      }
      else if (myJointRadius > 0) {
         renderer.setFaceColoring (myRenderProps, isSelected());
         renderer.drawSphere (coords0, myJointRadius);
      }
      double slen = getEffectiveShaftLength();
      if (slen > 0) {
         float[] coords1;

         Point3d p0 = new Point3d();
         Point3d p1 = new Point3d();

         if (myShaftLength < 0) {
            // legacy rendering style using lines
            computeRollAxisEndPoints (p0, p1, slen);
            coords0 = new float[] { (float)p0.x, (float)p0.y, (float)p0.z };
            coords1 = new float[] { (float)p1.x, (float)p1.y, (float)p1.z };
            renderer.drawLine (myRenderProps, coords0, coords1,
                               /*color=*/null, /*capped=*/true, isSelected());

            computePitchAxisEndPoints (p0, p1, slen);
            coords0 = new float[] { (float)p0.x, (float)p0.y, (float)p0.z };
            coords1 = new float[] { (float)p1.x, (float)p1.y, (float)p1.z };
            renderer.drawLine (myRenderProps, coords0, coords1,
                               /*color=*/null, /*capped=*/true, isSelected());
         }
         else {
            renderer.setFaceColoring (myRenderProps, isSelected());
            double r = getEffectiveShaftRadius();
            computeRollAxisEndPoints (p0, p1, slen);
            renderer.drawCylinder (p0, p1, r, /*capped=*/true);
            computePitchAxisEndPoints (p0, p1, slen);
            renderer.drawCylinder (p0, p1, r, /*capped=*/true);
         }
      }
   }

   /* --- end Renderable implementation --- */
}
