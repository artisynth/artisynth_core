/**
 * Copyright (c) 2023, by the Authors: John E Lloyd (UBC), Ian Stavness (USask)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.*;
import java.util.Deque;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshFactory;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.spatialmotion.EllipsoidCoupling;
import maspack.util.*;

/**
 * Implements a 3 DOF joint, in which the origin of frame C is constrained to lie 
 * on the surface of an ellipsoid in frame D, while being free to rotate about the 
 * axis normal to the ellipsoid surface. The x and y displacements of C are given by 
 * the coordinates {@code x} and {@code y}, while the (counter-clockwise) rotation
 * of C about z is given by the coordinate {@code theta} and rotation of C about the
 * y is given by the coordinate {@code phi}.
 *
 * <p>The {@code x}, {@code y} {@code theta} {@code phi} values are available as
 * properties (given in degrees) which can be read and also,
 * under appropriate circumstances, set.  Setting these values causes an
 * adjustment in the positions of one or both bodies connected to this joint,
 * along with adjacent bodies connected to them, with preference given to
 * bodies that are not attached to ``ground''.  If this is done during
 * simulation, and particularly if one or both of the bodies connected to this
 * joint are moving dynamically, the results will be unpredictable and will
 * likely conflict with the simulation.
 */
public class EllipsoidJoint extends JointBase 
   implements CopyableComponent {

   public static final int X_IDX = EllipsoidCoupling.X_IDX; 
   public static final int Y_IDX = EllipsoidCoupling.Y_IDX; 
   public static final int THETA_IDX = EllipsoidCoupling.THETA_IDX; 
   public static final int PHI_IDX = EllipsoidCoupling.PHI_IDX; 

   private static final double DEFAULT_PLANE_SIZE = 0;
   private double myPlaneSize = DEFAULT_PLANE_SIZE;
   
   private static final boolean DEFAULT_DRAW_ELLIPSOID = true;
   private boolean myDrawEllipsoid = DEFAULT_DRAW_ELLIPSOID;
   
   public static PropertyList myProps =
      new PropertyList (EllipsoidJoint.class, JointBase.class);
   
   PolygonalMesh ellipsoid;

   private static DoubleInterval DEFAULT_X_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_Y_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_THETA_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_PHI_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static boolean DEFAULT_X_LOCKED = false;
   private static boolean DEFAULT_Y_LOCKED = false;
   private static boolean DEFAULT_THETA_LOCKED = false;
   private static boolean DEFAULT_PHI_LOCKED = false;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createRenderProps (host);
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   static {
      myProps.add ("x", "u ellipsoid paramter", 0, "[-360,360]");
      myProps.add (
         "xRange", "range for x", DEFAULT_X_RANGE);
      myProps.add (
         "xLocked isXLocked",
         "set whether x is locked", DEFAULT_X_LOCKED);
      myProps.add ("y", "v ellipsoid parameter", 0, "[0,180]");
      myProps.add (
         "yRange", "range for y", DEFAULT_Y_RANGE);
      myProps.add (
         "yLocked isYLocked",
         "set whether y is locked", DEFAULT_Y_LOCKED);
      myProps.add ("theta", "joint angle (degrees)", 0, "1E %8.3f [-360,360]");
      myProps.add (
         "thetaRange", "range for theta", DEFAULT_THETA_RANGE, "%8.3f 1E");
      myProps.add (
         "thetaLocked isThetaLocked",
         "set whether theta is locked", DEFAULT_THETA_LOCKED);
      myProps.add ("phi", "joint angle (degrees)", 0, "1E %8.3f [-360,360]");
      myProps.add (
         "phiRange", "range for phi", DEFAULT_PHI_RANGE, "%8.3f 1E");
      myProps.add (
         "phiLocked isPhiLocked",
         "set whether phi is locked", DEFAULT_PHI_LOCKED);
      myProps.get ("renderProps").setDefaultValue (defaultRenderProps(null));
      myProps.add (
         "planeSize", "renderable size of the plane", DEFAULT_PLANE_SIZE);
      myProps.add (
         "drawEllipsoid",
         "draw the ellipsoid surface", DEFAULT_DRAW_ELLIPSOID);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myPlaneSize = DEFAULT_PLANE_SIZE;
      setRenderProps (defaultRenderProps (null));
   }

   public boolean getDrawEllipsoid() {
      return myDrawEllipsoid;
   }

   public void setDrawEllipsoid (boolean enable) {
      myDrawEllipsoid = enable;
   }

   /**
    * Creates a {@code PlanarJoint} which is not attached to any
    * bodies.  It can subsequently be connected using one of the {@code
    * setBodies} methods.
    */
   public EllipsoidJoint(
      double a, double b, double c, double alpha, boolean useOpenSimApprox) {
      setCoupling (new EllipsoidCoupling(a, b, c, alpha, useOpenSimApprox));
      ellipsoid = MeshFactory.createEllipsoid (a, b, c, /*slices=*/100);
      RenderProps.setFaceStyle (ellipsoid, FaceStyle.NONE);
      RenderProps.setDrawEdges (ellipsoid, true);
      RenderProps.setEdgeColor (ellipsoid, Color.DARK_GRAY);
      setXRange (DEFAULT_X_RANGE);
      setYRange (DEFAULT_Y_RANGE);
      setThetaRange (DEFAULT_THETA_RANGE);      
      setPhiRange (DEFAULT_PHI_RANGE);
   }
   
   /**
    * Creates a {@code EllipsoidJoint} connecting two rigid bodies, {@code bodyA}
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
   public EllipsoidJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB,
      double a, double b, double c, double alpha, boolean useOpenSimApprox) {
      this(a, b, c, alpha, useOpenSimApprox);
      setBodies (bodyA, TCA, bodyB, TDB);
   }
   
   /**
    * Creates a {@code EllipsoidJoint} connecting two connectable bodies, {@code
    * bodyA} and {@code bodyB}. The joint frames C and D are located
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
   public EllipsoidJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW, double a, double b, double c) {
      this(a, b, c, 0, false);
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   @Override
   public EllipsoidCoupling getCoupling () {
      return (EllipsoidCoupling)myCoupling;
   }

   public boolean getUseOpenSimApprox() {
      return getCoupling().getUseOpenSimApprox();
   }

   public double getAlpha() {
      return getCoupling().getAlpha();
   }

   /**
    * Queries the x range limits for this joint. See {@link
    * #setXRange(DoubleInterval)} for more details.
    *
    * @return x range limits for this joint
    */
   public double getX() {
      return RTOD*getCoordinate (X_IDX);
   }

   /**
    * Sets this joint's x value. This describes the displacement of frame C
    * along the x axis of frame D. See this class's Javadoc header for a
    * discussion of what happens when this value is set.
    *
    * @param x new x value
    */
   public void setX (double x) {
      setCoordinate (X_IDX, DTOR*x);
   }

   /**
    * Queries the x range limits for this joint. See {@link
    * #setXRange(DoubleInterval)} for more details.
    *
    * @return x range limits for this joint
    */
   public DoubleInterval getXRange () {
      return getCoordinateRangeDeg (X_IDX);
   }

   /**
    * Queries the lower x range limit for this joint, in degrees.
    *
    * @return lower x range limit
    */
   public double getMinX () {
      return getMinCoordinateDeg (X_IDX);
   }

   /**
    * Queries the upper x range limit for this joint, in degrees.
    *
    * @return upper x range limit
    */
   public double getMaxX () {
      return getMaxCoordinateDeg (X_IDX);
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
      setCoordinateRangeDeg (X_IDX, range);
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
   public double getY() {
      return RTOD*getCoordinate (Y_IDX);
   }

   /**
    * Sets this joint's y value. This describes the displacement of frame C
    * along the y axis of frame D. See this class's Javadoc header for a
    * discussion of what happens when this value is set.
    *
    * @param y new y value
    */
   public void setY (double y) {
      setCoordinate (Y_IDX, DTOR*y);
   }

   /**
    * Queries the y range limits for this joint. See {@link
    * #setYRange(DoubleInterval)} for more details.
    *
    * @return y range limits for this joint
    */
   public DoubleInterval getYRange () {
      return getCoordinateRangeDeg (Y_IDX);
   }

   /**
    * Queries the lower x range limit for this joint, in degrees.
    *
    * @return lower x range limit
    */
   public double getMinY () {
      return getMinCoordinateDeg (Y_IDX);
   }

   /**
    * Queries the upper x range limit for this joint, in degrees.
    *
    * @return upper x range limit
    */
   public double getMaxY () {
      return getMaxCoordinateDeg (Y_IDX);
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
      setCoordinateRangeDeg (Y_IDX, range);
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
    * Queries this joint's theta value, in degrees. See {@link #setTheta} for
    * more details.
    *
    * @return current theta value
    */
   public double getTheta() {
      return RTOD*getCoordinate (THETA_IDX);
   }

   /**
    * Sets this joint's theta value, in degrees. This describes the clockwise
    * rotation of frame C about the z axis of frame D. See this class's Javadoc
    * header for a discussion of what happens when this value is set.
    *
    * @param theta new theta value
    */
   public void setTheta (double theta) {
      setCoordinate (THETA_IDX, DTOR*theta);
   }

   /**
    * Queries the theta range limits for this joint, in degrees. See {@link
    * #setThetaRange(DoubleInterval)} for more details.
    *
    * @return theta range limits for this joint
    */
   public DoubleInterval getThetaRange () {
      return getCoordinateRangeDeg (THETA_IDX);
   }

   /**
    * Queries the lower theta range limit for this joint, in degrees.
    *
    * @return lower theta range limit
    */
   public double getMinTheta () {
      return getMinCoordinateDeg (THETA_IDX);
   }

   /**
    * Queries the upper theta range limit for this joint, in degrees.
    *
    * @return upper theta range limit
    */
   public double getMaxTheta () {
      return getMaxCoordinateDeg (THETA_IDX);
   }

   /**
    * Sets the theta range limits for this joint, in degrees. The default range
    * is {@code [-inf, inf]}, which implies no limits. If theta travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range theta range limits for this joint
    */
   public void setThetaRange (DoubleInterval range) {
      setCoordinateRangeDeg (THETA_IDX, range);
   }
   
   /**
    * Sets the theta range limits for this joint. This is a
    * convenience wrapper for {@link #setThetaRange(DoubleInterval)}.
    *
    * @param min minimum theta value
    * @param max maximum theta value
    */
   public void setThetaRange(double min, double max) {
      setThetaRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper theta range limit for this joint, in degrees. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper theta range limit
    */
   public void setMaxTheta (double max) {
      setThetaRange (new DoubleInterval (getMinTheta(), max));
   }

   /**
    * Sets the lower theta range limit for this joint, in degrees. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower theta range limit
    */
   public void setMinTheta (double min) {
      setThetaRange (new DoubleInterval (min, getMaxTheta()));
   }

   /**
    * Queries whether the theta coordinate for this joint is locked.
    *
    * @return {@code true} if theta is locked
    */
   public boolean isThetaLocked() {
      return isCoordinateLocked (THETA_IDX);
   }

   /**
    * Set whether the theta coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks theta
    */
   public void setThetaLocked (boolean locked) {
      setCoordinateLocked (THETA_IDX, locked);
   }

   /**
    * Queries this joint's phi value, in degrees. See {@link #setPhi} for
    * more details.
    *
    * @return current phi value
    */
   public double getPhi() {
      return RTOD*getCoordinate (PHI_IDX);
   }

   /**
    * Sets this joint's phi value, in degrees. This describes the
    * rotation of frame C about the y axis of frame D. See this class's Javadoc
    * header for a discussion of what happens when this value is set.
    *
    * @param phi new phi value
    */
   public void setPhi (double phi) {
      setCoordinate (PHI_IDX, DTOR*phi);
   }

   /**
    * Queries the phi range limits for this joint, in degrees. See {@link
    * #setPhiRange(DoubleInterval)} for more details.
    *
    * @return phi range limits for this joint
    */
   public DoubleInterval getPhiRange () {
      return getCoordinateRangeDeg (PHI_IDX);
   }

   /**
    * Queries the lower phi range limit for this joint, in degrees.
    *
    * @return lower phi range limit
    */
   public double getMinPhi () {
      return getMinCoordinateDeg (PHI_IDX);
   }

   /**
    * Queries the upper phi range limit for this joint, in degrees.
    *
    * @return upper phi range limit
    */
   public double getMaxPhi () {
      return getMaxCoordinateDeg (PHI_IDX);
   }

   /**
    * Sets the phi range limits for this joint, in degrees. The default range
    * is {@code [-inf, inf]}, which implies no limits. If phi travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range phi range limits for this joint
    */
   public void setPhiRange (DoubleInterval range) {
      setCoordinateRangeDeg (PHI_IDX, range);
   }
   
   /**
    * Sets the phi range limits for this joint. This is a
    * convenience wrapper for {@link #setPhiRange(DoubleInterval)}.
    *
    * @param min minimum phi value
    * @param max maximum phi value
    */
   public void setPhiRange(double min, double max) {
      setPhiRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper phi range limit for this joint, in degrees. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper phi range limit
    */
   public void setMaxPhi (double max) {
      setPhiRange (new DoubleInterval (getMinPhi(), max));
   }

   /**
    * Sets the lower phi range limit for this joint, in degrees. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower phi range limit
    */
   public void setMinPhi (double min) {
      setPhiRange (new DoubleInterval (min, getMaxPhi()));
   }

   /**
    * Queries whether the phi coordinate for this joint is locked.
    *
    * @return {@code true} if phi is locked
    */
   public boolean isPhiLocked() {
      return isCoordinateLocked (PHI_IDX);
   }

   /**
    * Set whether the phi coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks phi
    */
   public void setPhiLocked (boolean locked) {
      setCoordinateLocked (PHI_IDX, locked);
   }
   
   /**
    * Queries the size used to render this joint's tangent plane as a square.
    *
    * @return size used to render the plane
    */
   public double getPlaneSize() {
      return myPlaneSize;
   }

   /**
    * Sets the size used to render this joint's tangent plane as a square.
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

   @Override
   public void prerender (RenderList list) {
      super.prerender (list);
      if (myDrawEllipsoid) {
         ellipsoid.XMeshToWorld.set (myRenderFrameD);
         ellipsoid.prerender (myRenderProps);
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      super.updateBounds (pmin, pmax);
      PlanarConnector.updateXYSquareBounds (
         pmin, pmax, getCurrentTDW(), myPlaneSize);
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      PlanarConnector.renderXYSquare (
         renderer, myRenderProps, myRenderFrameC, myPlaneSize, isSelected());
      if (myDrawEllipsoid) {
         flags |= isSelected() ? Renderer.HIGHLIGHT : 0;
         ellipsoid.render (renderer, myRenderProps, flags);
      }
   }

   /* --- end Renderable implementation --- */

   // need to implement write and scan so we can handle the semi axis lengths
   // and the useOpenSimApprox settings.

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("semiAxisLengths=");
      getCoupling().getSemiAxisLengths().write (
         pw, fmt, /*withBrackets=*/true);      
      if (getCoupling().getAlpha() != 0) {
         pw.print ("alpha=" + fmt.format(getCoupling().getAlpha()));
      }
      if (getCoupling().getUseOpenSimApprox()) {
         pw.print ("useOpenSimApprox=true");
      }
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "semiAxisLengths")) {
         Vector3d lens = new Vector3d();
         lens.scan (rtok);
         getCoupling().setSemiAxisLengths (lens);
         return true;
      }
      else if (scanAttributeName (rtok, "useOpenSimApprox")) {
         getCoupling().setUseOpenSimApprox (rtok.scanBoolean());
         return true;
      }
      else if (scanAttributeName (rtok, "alpha")) {
         getCoupling().setAlpha (rtok.scanNumber());
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }
}
