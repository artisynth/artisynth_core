/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.IOException;
import java.util.Deque;
import java.util.Map;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.RenderProps;
import maspack.spatialmotion.SliderCoupling;
import maspack.util.DoubleInterval;
import maspack.util.ReaderTokenizer;

/**
 * Implements a 1 DOF prismatic joint, in which frame C translates along the z
 * axis of frame D by a distance {@code z}.
 *
 * <p>The {@code z} value is available as a property which can be read and
 * also, under appropriate circumstances, set.  Setting this value causes an
 * adjustment in the positions of one or both bodies connected to this joint,
 * along with adjacent bodies connected to them, with preference given to
 * bodies that are not attached to ``ground''.  If this is done during
 * simulation, and particularly if one or both of the bodies connected to this
 * joint are moving dynamically, the results will be unpredictable and will
 * likely conflict with the simulation.
 */
public class SliderJoint extends JointBase 
   implements CopyableComponent {

   public static final int Z_IDX = SliderCoupling.Z_IDX; 

   public static PropertyList myProps =
      new PropertyList (SliderJoint.class, JointBase.class);

   private static DoubleInterval DEFAULT_Z_RANGE =
      new DoubleInterval ("[-inf,inf])");

   static {
      myProps.add ("z", "slider distance", 0);
      myProps.add (
         "zRange", "range for z", DEFAULT_Z_RANGE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Creates a {@code PrismaticJoint} which is not attached to any bodies.  It
    * can subsequently be connected using one of the {@code setBodies} methods.
    */
   public SliderJoint() {
      setCoupling (new SliderCoupling());
      setZRange (DEFAULT_Z_RANGE);
   }

   /**
    * Creates a {@code PrismaticJoint} connecting two rigid bodies, {@code
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
   public SliderJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   /**
    * Creates a {@code PrismaticJoint} connecting two connectable bodies,
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
   public SliderJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code PrismaticJoint} connecting two connectable bodies, {@code
    * bodyA} and {@code bodyB}. The joint frames D and C are assumed to be
    * initially coincident, so that {@code z} will have an initial value of
    * 0. D (and C) is located by {@code TDW}, which gives the transform from D
    * to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */
   public SliderJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code PrismaticJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, so that {@code z} will have an initial value of
    * 0. D (and C) is located by {@code TDW}, which gives the transform from D
    * to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public SliderJoint (ConnectableBody bodyA, RigidTransform3d TDW) {
      this();

      setBodies (bodyA, null, TDW);
   }

   /**
    * Creates a {@code PrismaticJoint} connecting two connectable bodies, {@code
    * bodyA} and {@code bodyB}. The joint frames D and C are assumed to be
    * initially coincident, so that {@code z} will have an initial value of
    * 0. D (and C) is located (with respect to world) so that its origin is at
    * {@code pd} and its z axis in the direction of {@code zaxis}.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B, or {@code null} if {@code bodyA} is connected
    * to ground.
    * @param originD origin of frame D (world coordinates)
    * @param zaxis direction of frame D's z axis (world coordinates)
    */
   public SliderJoint (
      RigidBody bodyA, ConnectableBody bodyB, Point3d originD, Vector3d zaxis) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (originD);
      TDW.R.setZDirection (zaxis);
      setBodies (bodyA, bodyB, TDW);
   }   

   /**
    * Queries this joint's z value. See {@link #setZ} for more details.
    *
    * @return current z value
    */
   public double getZ() {
      return getCoordinate (Z_IDX);
   }

   /**
    * Sets this joint's z value. This describes the displacement of frame C
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
    * Queries the lower z range limit for this joint, in degrees.
    *
    * @return lower z range limit
    */
   public double getMinZ () {
      return getMinCoordinate (Z_IDX);
   }

   /**
    * Queries the upper z range limit for this joint, in degrees.
    *
    * @return upper z range limit
    */
   public double getMaxZ () {
      return getMaxCoordinate (Z_IDX);
   }

   /**
    * Sets the z range limits for this joint. The default range is {@code
    * [-inf, inf]}, which implies no limits. If z travels beyond these limits
    * during dynamic simulation, unilateral constraints will be activated to
    * enforce them. Setting the lower limit to {@code -inf} or the upper limit
    * to {@code inf} removes the lower or upper limit, respectively. Specifying
    * {@code range} as {@code null} will set the range to {@code (-inf, inf)}.
    *
    * @param range z range limits for this joint
    */
   public void setZRange (DoubleInterval range) {
      setCoordinateRange (Z_IDX, range);
   }
   
   /**
    * Sets the z range limits for this joint. This is a
    * convenience wrapper for {@link #setZRange(DoubleInterval)}.
    *
    * @param min minimum z value
    * @param max maximum z value
    */
   public void setZRange(double min, double max) {
      setZRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper z range limit for this joint. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper z range limit
    */
   public void setMaxZ (double max) {
      setZRange (new DoubleInterval (getMinZ(), max));
   }

   /**
    * Sets the lower z range limit for this joint. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower z range limit
    */
   public void setMinZ (double min) {
      setZRange (new DoubleInterval (min, getMaxZ()));
   }

   /* --- begin Renderable implementation --- */

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      super.updateBounds (pmin, pmax);
      updateZShaftBounds (pmin, pmax, getCurrentTDW(), getShaftLength());
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      renderZShaft (renderer, myRenderFrameD);
   }

   /* --- end Renderable implementation --- */
}
