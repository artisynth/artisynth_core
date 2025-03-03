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
import maspack.spatialmotion.EllipsoidCoupling3d;
import maspack.util.*;

/**
 * Implements a 3 DOF joint, in which the origin of frame C is constrained to lie 
 * on the surface of an ellipsoid in frame D, while being free to rotate about the 
 * axis normal to the ellipsoid surface. The x and y displacements of C are given by 
 * the coordinates {@code x} and {@code y}, while the (counter-clockwise) rotation
 * of C about z is given by the coordinate {@code theta}.
 *
 * <p>The {@code x}, {@code y} {@code theta} values are available as
 * properties (given in degrees) which can be read and also,
 * under appropriate circumstances, set.  Setting these values causes an
 * adjustment in the positions of one or both bodies connected to this joint,
 * along with adjacent bodies connected to them, with preference given to
 * bodies that are not attached to ``ground''.  If this is done during
 * simulation, and particularly if one or both of the bodies connected to this
 * joint are moving dynamically, the results will be unpredictable and will
 * likely conflict with the simulation.
 */
public class EllipsoidJoint3d extends JointBase 
   implements CopyableComponent {

   public static final int X_IDX = EllipsoidCoupling3d.X_IDX; 
   public static final int Y_IDX = EllipsoidCoupling3d.Y_IDX; 
   public static final int THETA_IDX = EllipsoidCoupling3d.THETA_IDX; 

   private static final double DEFAULT_PLANE_SIZE = 0;
   private double myPlaneSize = DEFAULT_PLANE_SIZE;

   private static final boolean DEFAULT_DRAW_ELLIPSOID = true;
   private boolean myDrawEllipsoid = DEFAULT_DRAW_ELLIPSOID;
   
   public static PropertyList myProps =
      new PropertyList (EllipsoidJoint3d.class, JointBase.class);
   
   PolygonalMesh ellipsoid;

   private static DoubleInterval DEFAULT_X_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_Y_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_THETA_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_PHI_RANGE =
      new DoubleInterval ("[-inf,inf])");
   
   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createRenderProps (host);
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   static {
      myProps.add ("x", "u ellipsoid paramter", 0, "[-360,360]");
      myProps.add (
         "xRange", "range for x", DEFAULT_X_RANGE);
      myProps.add ("y", "v ellipsoid parameter", 0, "[0,180]");
      myProps.add (
         "yRange", "range for y", DEFAULT_Y_RANGE);
      myProps.add ("theta", "joint angle (degrees)", 0, "1E %8.3f [-360,360]");
      myProps.add (
         "thetaRange", "range for theta", DEFAULT_THETA_RANGE, "%8.3f 1E");
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

   protected void setDefaultValues() {
      super.setDefaultValues();
      myPlaneSize = DEFAULT_PLANE_SIZE;
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
   public EllipsoidJoint3d (
      double a, double b, double c, boolean useOpenSimApprox) {
      setCoupling (new EllipsoidCoupling3d(a, b, c, useOpenSimApprox));
      ellipsoid = MeshFactory.createEllipsoid (a, b, c, /*slices=*/100);
      RenderProps.setFaceStyle (ellipsoid, FaceStyle.NONE);
      RenderProps.setDrawEdges (ellipsoid, true);
      RenderProps.setEdgeColor (ellipsoid, Color.DARK_GRAY);
      setXRange (DEFAULT_X_RANGE);
      setYRange (DEFAULT_Y_RANGE);
      setThetaRange (DEFAULT_THETA_RANGE);      
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
   public EllipsoidJoint3d (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB,
      double a, double b, double c, boolean useOpenSimApprox) {
      this(a, b, c, useOpenSimApprox);
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
   public EllipsoidJoint3d (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW,
      double a, double b, double c, boolean useOpenSimApprox) {
      this(a, b, c, useOpenSimApprox);
      setBodies (bodyA, bodyB, TCW, TDW);
   }
   
   @Override
   public EllipsoidCoupling3d getCoupling () {
      return (EllipsoidCoupling3d)myCoupling;
   }

   public boolean getUseOpenSimApprox() {
      return getCoupling().getUseOpenSimApprox();
   }

   /**
    * Queries the x range limits for this joint. See {@link
    * #setXRange(DoubleInterval)} for more details.
    *
    * @return x range limits for this joint
    */
   public double getX() {
      double x = RTOD*getCoordinate (X_IDX);
      return x;
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
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

}
