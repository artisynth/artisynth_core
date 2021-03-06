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
import maspack.spatialmotion.PlanarTranslationCoupling;
import maspack.util.DoubleInterval;
import maspack.util.ReaderTokenizer;

/**
 * Implements a 2 DOF planer joint, in which the origin of frame C is
 * constrained to lie in the x-y plane of frame D and rotation is not
 * permitted. The x and y displacements of C are given by the coordinates
 * {@code x} and {@code y}.
 *
 * <p>The {@code x} and {@code y} values are available as properties which can
 * be read and also, under appropriate circumstances, set.  Setting these
 * values causes an adjustment in the positions of one or both bodies connected
 * to this joint, along with adjacent bodies connected to them, with preference
 * given to bodies that are not attached to ``ground''.  If this is done during
 * simulation, and particularly if one or both of the bodies connected to this
 * joint are moving dynamically, the results will be unpredictable and will
 * likely conflict with the simulation.
 */
public class PlanarTranslationJoint extends JointBase 
   implements CopyableComponent {

   public static final int X_IDX = PlanarTranslationCoupling.X_IDX; 
   public static final int Y_IDX = PlanarTranslationCoupling.Y_IDX; 

   private static final double DEFAULT_PLANE_SIZE = 0;
   private double myPlaneSize = DEFAULT_PLANE_SIZE;

   public static PropertyList myProps =
      new PropertyList (PlanarTranslationJoint.class, JointBase.class);

   private static DoubleInterval DEFAULT_X_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_Y_RANGE =
      new DoubleInterval ("[-inf,inf])");

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createRenderProps (host);
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   static {
      myProps.add ("x", "x translation distance", 0);
      myProps.add (
         "xRange", "range for x", DEFAULT_X_RANGE);
      myProps.add ("y", "y translation distance", 0);
      myProps.add (
         "yRange", "range for y", DEFAULT_Y_RANGE);
      myProps.get ("renderProps").setDefaultValue (defaultRenderProps(null));
      myProps.add (
         "planeSize", "renderable size of the plane", DEFAULT_PLANE_SIZE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myPlaneSize = DEFAULT_PLANE_SIZE;
      setRenderProps (defaultRenderProps (null));
   }

   /**
    * Creates a {@code PlanarTranslationJoint} which is not attached to any
    * bodies.  It can subsequently be connected using one of the {@code
    * setBodies} methods.
    */
   public PlanarTranslationJoint() {
      setCoupling (new PlanarTranslationCoupling());
      setXRange (DEFAULT_X_RANGE);
      setYRange (DEFAULT_Y_RANGE);
   }

   /**
    * Creates a {@code PlanarTranslationJoint} connecting two rigid bodies,
    * {@code bodyA} and {@code bodyB}. If A and B describe the coordinate
    * frames of {@code bodyA} and {@code bodyB}, then {@code TCA} and {@code
    * TDB} give the (fixed) transforms from the joint's C and D frames to A and
    * B, respectively. Since C and D are specified independently, the joint
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
   public PlanarTranslationJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }
   
   /**
    * Creates a {@code PlanarTranslationJoint} connecting two connectable
    * bodies, {@code bodyA} and {@code bodyB}. The joint frames C and D are
    * located independently with respect to world coordinates by {@code TCW}
    * and {@code TDW}.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B (or {@code null})
    * @param TCW initial transform from joint frame C to world
    * @param TDW initial transform from joint frame D to world
    */
   public PlanarTranslationJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code PlanarTranslationJoint} connecting two connectable
    * bodies, {@code bodyA} and {@code bodyB}. The joint frames D and C are
    * assumed to be initially coincident, so that {@code x} and {@code y} will
    * have initial values of 0. D (and C) is located by {@code TDW}, which
    * gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */
   public PlanarTranslationJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code PlanarTranslationJoint} connecting a single connectable
    * body, {@code bodyA}, to ground. The joint frames D and C are assumed to
    * be initially coincidentD, so that {@code x} and {@code y} will have
    * initial values of 0. D (and C) is located by {@code TDW}, which gives the
    * transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public PlanarTranslationJoint (
      ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, null, TDW);
   }

   /**
    * Creates a {@code PlanarTranslationJoint} connecting two connectable
    * bodies, {@code bodyA} and {@code bodyB}. The joint frames D and C are
    * assumed to be initially coincident, so that {@code x} and {@code y} will
    * have initial values of 0. D (and C) is located (with respect to world) so
    * that its origin is at {@code pd} and its z axis in the direction of
    * {@code zaxis}.
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
   public PlanarTranslationJoint (
      RigidBody bodyA, ConnectableBody bodyB, Point3d originD, Vector3d zaxis) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (originD);
      TDW.R.setZDirection (zaxis);
      setBodies (bodyA, bodyB, TDW);
   }   

   /**
    * Queries the x range limits for this joint. See {@link
    * #setXRange(DoubleInterval)} for more details.
    *
    * @return x range limits for this joint
    */
   public double getX() {
      return getCoordinate (X_IDX);
   }

   /**
    * Sets this joint's x value. This describes the displacement of frame C
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
    * Queries the lower x range limit for this joint, in degrees.
    *
    * @return lower x range limit
    */
   public double getMinX () {
      return getMinCoordinate (X_IDX);
   }

   /**
    * Queries the upper x range limit for this joint, in degrees.
    *
    * @return upper x range limit
    */
   public double getMaxX () {
      return getMaxCoordinate (X_IDX);
   }

   /**
    * Sets the x range limits for this joint. The default range is {@code
    * [-inf, inf]}, which implies no limits. If x travels beyond these limits
    * during dynamic simulation, unilateral constraints will be activated to
    * enforce them. Setting the lower limit to {@code -inf} or the upper limit
    * to {@code inf} removes the lower or upper limit, respectively. Specifying
    * {@code range} as {@code null} will set the range to {@code (-inf, inf)}.
    *
    * @param range x range limits for this joint
    */
   public void setXRange (DoubleInterval range) {
      setCoordinateRange (X_IDX, range);
   }

   /**
    * Sets the x range limits for this joint. This is a
    * convenience wrapper for {@link #setXRange(DoubleInterval)}.
    *
    * @param min minimum x value
    * @param max maximum x value
    */   
   public void setXRange(double min, double max) {
      setXRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper x range limit for this joint. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper x range limit
    */
   public void setMaxX (double max) {
      setXRange (new DoubleInterval (getMinX(), max));
   }

   /**
    * Sets the lower x range limit for this joint. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower x range limit
    */
   public void setMinX (double min) {
      setXRange (new DoubleInterval (min, getMaxX()));
   }

   /**
    * Queries this joint's y value. See {@link #setY} for more details.
    *
    * @return current y value
    */
   public double getY() {
      return getCoordinate (Y_IDX);
   }

   /**
    * Sets this joint's y value. This describes the displacement of frame C
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
    * Queries the lower x range limit for this joint, in degrees.
    *
    * @return lower x range limit
    */
   public double getMinY () {
      return getMinCoordinate (Y_IDX);
   }

   /**
    * Queries the upper x range limit for this joint, in degrees.
    *
    * @return upper x range limit
    */
   public double getMaxY () {
      return getMaxCoordinate (Y_IDX);
   }

   /**
    * Sets the y range limits for this joint. The default range is {@code
    * [-inf, inf]}, which implies no limits. If y travels beyond these limits
    * during dynamic simulation, unilateral constraints will be activated to
    * enforce them. Setting the lower limit to {@code -inf} or the upper limit
    * to {@code inf} removes the lower or upper limit, respectively. Specifying
    * {@code range} as {@code null} will set the range to {@code (-inf, inf)}.
    *
    * @param range y range limits for this joint
    */
   public void setYRange (DoubleInterval range) {
      setCoordinateRange (Y_IDX, range);
   }
   
   /**
    * Sets the y range limits for this joint. This is a
    * convenience wrapper for {@link #setYRange(DoubleInterval)}.
    *
    * @param min minimum y value
    * @param max maximum y value
    */
   public void setYRange(double min, double max) {
      setYRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper y range limit for this joint. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper y range limit
    */
   public void setMaxY (double max) {
      setYRange (new DoubleInterval (getMinY(), max));
   }

   /**
    * Sets the lower y range limit for this joint. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower y range limit
    */
   public void setMinY (double min) {
      setYRange (new DoubleInterval (min, getMaxY()));
   }

   /**
    * Queries the size used to render this joint's plane as a square.
    *
    * @return size used to render the plane
    */
   public double getPlaneSize() {
      return myPlaneSize;
   }

   /**
    * Sets the size used to render this joint's plane as a square.
    *
    * @param size used to render the plane
    */
   public void setPlaneSize (double size) {
      myPlaneSize = size;
   }

   /* --- begin Renderable implementation --- */

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      super.updateBounds (pmin, pmax);
      PlanarConnector.updateXYSquareBounds (
         pmin, pmax, getCurrentTDW(), myPlaneSize);
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      PlanarConnector.renderXYSquare (
         renderer, myRenderProps, myRenderFrameD, myPlaneSize, isSelected());
   }

   /* --- end Renderable implementation --- */
}
