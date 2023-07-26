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
 * Implements a 4 DOF joint, in which the origin of frame C is constrained to
 * lie on the surface of an ellipsoid in frame D, while being free to rotate
 * about the axis normal to the ellipsoid surface and a second axis in the
 * surface tangent plane. The origin of C with respect to D is described by two
 * coordinate angles, {@code longitude} and {@code latitude}, which describe a
 * point on the ellipsoid in polar coordinates. If {@code c1}, {@code s1}
 * {@code c2}, and {@code s2} are the cosine and sine of {@code longitude} and
 * {@code latitude}, respectively, the semi-axis lengths of the ellipsoid are
 * {@code rx}, {@code ry}, and {@code rz}, and the origin of C is given with
 * respect to D by a vector {@code p}, then
 * <pre>
 *     [   rx s2   ]
 * p = [ -ry s1 c2 ]
 *     [  rz c1 c2 ]
 * </pre>
 * which is the equation of an ellipsoid in polar coordinates, with longitude
 * giving the rotation angle about the x axis.
 *
 * <p>The longitude and latitude coordinates define a frame with respect to D
 * known as "frame 2". With respect to D, the origin of frame 2 is {@code p}
 * described above, while the orientation of frame 2 is determined in
 * one of two ways:
 *
 * <ul>
 * 
 * <li>By default, the z axis is set to point outward from the surface normal,
 * while the x axis is the surface tangent direction imparted by the
 * {@code latitude} angle.
 *
 * <li>If the joint is set to be compatible with the OpenSim ellipsoid joint,
 * then the z axis is instead set to an approximation of the surface normal
 * given by {@code (p.x/rx, p.y/ry, p.z/rz)}, while the x direction is the same
 * as that which would result by treating the longitude and latitude
 * coordinates as the first two joints of an XYZ GimbalCoupling.
 *
 * </ul>
 *
 * Two additional angles, {@code theta} and {@code phi}, provide two additional
 * rotational degrees of freedom between frame 2 and frame C, consisting of a
 * rotation by {@code theta} about the frame 2 z axis, followed by an
 * additional rotation about a ``wing'' axis in the (rotated) x-y plane, where
 * the wing axis is defined by an angle {@code alpha} with respect to the y
 * axis. The default wing axis corresponds to the y axis (i.e., {@code alpha} =
 * 0).
 *
 * <p>The {@code longitude}, {@code latitude} {@code theta} {@code phi} values
 * are available as properties (given in degrees) which can be read and also,
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

   public static final int LONGITUDE_IDX = EllipsoidCoupling.LONGITUDE_IDX; 
   public static final int LATITUDE_IDX = EllipsoidCoupling.LATITUDE_IDX; 
   public static final int THETA_IDX = EllipsoidCoupling.THETA_IDX; 
   public static final int PHI_IDX = EllipsoidCoupling.PHI_IDX; 

   private static final double DEFAULT_PLANE_SIZE = 0;
   private double myPlaneSize = DEFAULT_PLANE_SIZE;
   
   private static final boolean DEFAULT_DRAW_ELLIPSOID = true;
   private boolean myDrawEllipsoid = DEFAULT_DRAW_ELLIPSOID;
   
   public static PropertyList myProps =
      new PropertyList (EllipsoidJoint.class, JointBase.class);
   
   PolygonalMesh ellipsoid;

   private static DoubleInterval DEFAULT_LONGITUDE_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_LATITUDE_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_THETA_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_PHI_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static boolean DEFAULT_LONGITUDE_LOCKED = false;
   private static boolean DEFAULT_LATITUDE_LOCKED = false;
   private static boolean DEFAULT_THETA_LOCKED = false;
   private static boolean DEFAULT_PHI_LOCKED = false;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createRenderProps (host);
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   static {
      myProps.add (
         "longitude", "longitude angle on the ellipsoid", 0, "[-360,360]");
      myProps.add (
         "longitudeRange", "range for longitude", DEFAULT_LONGITUDE_RANGE);
      myProps.add (
         "longitudeLocked isLongitudeLocked",
         "set whether longitude angle is locked", DEFAULT_LONGITUDE_LOCKED);
      myProps.add (
         "latitude", "latitude angle on the ellipsoid", 0, "[0,180]");
      myProps.add (
         "latitudeRange", "range for latitude", DEFAULT_LATITUDE_RANGE);
      myProps.add (
         "latitudeLocked isLatitudeLocked",
         "set whether latitude is locked", DEFAULT_LATITUDE_LOCKED);
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
      //     myProps.add (
      //         "planeSize", "renderable size of the plane", DEFAULT_PLANE_SIZE);
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
    * Creates an {@code EllipsoidJoint} which is not attached to any bodies.
    * It can subsequently be connected using one of the {@code setBodies}
    * methods.
    */
   public EllipsoidJoint(
      double a, double b, double c, double alpha, boolean useOpenSimApprox) {
      setCoupling (new EllipsoidCoupling(a, b, c, alpha, useOpenSimApprox));
      ellipsoid = MeshFactory.createEllipsoid (a, b, c, /*slices=*/100);
      RenderProps.setFaceStyle (ellipsoid, FaceStyle.NONE);
      RenderProps.setDrawEdges (ellipsoid, true);
      RenderProps.setEdgeColor (ellipsoid, Color.DARK_GRAY);
      setLongitudeRange (DEFAULT_LONGITUDE_RANGE);
      setLatitudeRange (DEFAULT_LATITUDE_RANGE);
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
    * @param rx semi-axis length along x
    * @param ry semi-axis length along y
    * @param rz semi-axis length along z
    * @param alpha angle of the second rotation axis, with respect to the y
    * axis of frame 2, as described in this class's Javadoc header
    * @param openSimCompatible if {@code true}, make this joint compatible with
    * the OpenSim ellispoid joint, as described in this class's Javadoc header.
    */
   public EllipsoidJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB,
      double rx, double ry, double rz, double alpha, boolean openSimCompatible) {
      this(rx, ry, rz, alpha, openSimCompatible);
      setBodies (bodyA, TCA, bodyB, TDB);
   }
   
   /**
    * Creates a {@code EllipsoidJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames C and D are located
    * independently with respect to world coordinates by {@code TCW} and {@code
    * TDW}. Specifying {@code bodyB} as {@code null} will cause {@code bodyA}
    * to be connected to ground.
    *
    * <p>
    * 
    *
    * @param bodyA body A
    * @param bodyB body B (or {@code null})
    * @param TCW initial transform from joint frame C to world
    * @param TDW initial transform from joint frame D to world
    * @param rx semi-axis length along x
    * @param ry semi-axis length along y
    * @param rz semi-axis length along z
    */
   public EllipsoidJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW,
      double rx, double ry, double rz) {
      this(rx, ry, rz, 0, false);
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   @Override
   public EllipsoidCoupling getCoupling () {
      return (EllipsoidCoupling)myCoupling;
   }

   public boolean isOpenSimCompatible() {
      return getCoupling().isOpenSimCompatible();
   }

   public double getAlpha() {
      return getCoupling().getAlpha();
   }

   /**
    * Queries this joint's longitude angle. See this class's Javadoc header for
    * a description of this coordinate.
    *
    * @return longitude angle (in degrees)
    */
   public double getLongitude() {
      return RTOD*getCoordinate (LONGITUDE_IDX);
   }

   /**
    * Sets this joint's longitude angle. See this class's Javadoc header for a
    * description of this coordinate.
    *
    * @param longitude new longitude angle (in degrees)
    */
   public void setLongitude (double longitude) {
      setCoordinate (LONGITUDE_IDX, DTOR*longitude);
   }

   /**
    * Queries the longitude angle range limits for this joint. See {@link
    * #setLongitudeRange(DoubleInterval)} for more details.
    *
    * @return longitude range limits for this joint (in degrees)
    */
   public DoubleInterval getLongitudeRange () {
      return getCoordinateRangeDeg (LONGITUDE_IDX);
   }

   /**
    * Queries the lower longitude range limit for this joint.
    *
    * @return lower longitude range limit (in degrees)
    */
   public double getMinLongitude () {
      return getMinCoordinateDeg (LONGITUDE_IDX);
   }

   /**
    * Queries the upper longitude range limit for this joint.
    *
    * @return upper longitude range limit (in degrees)
    */
   public double getMaxLongitude () {
      return getMaxCoordinateDeg (LONGITUDE_IDX);
   }

   /**
    * Sets the range limits of the longitude for this joint. The default range
    * is {@code [-inf, inf]}, which implies no limits. If the longitude travels
    * beyond these limits during dynamic simulation, unilateral constraints
    * will be activated to enforce them. Setting the lower limit to {@code
    * -inf} or the upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range longitude range limits for this joint (in degrees)
    */
   public void setLongitudeRange (DoubleInterval range) {
      setCoordinateRangeDeg (LONGITUDE_IDX, range);
   }

   /**
    * Sets the longitude range limits for this joint. This is a
    * convenience wrapper for {@link #setLongitudeRange(DoubleInterval)}.
    *
    * @param min minimum longitude angle (in degrees)
    * @param max maximum longitude angle (in degrees)
    */   
   public void setLongitudeRange(double min, double max) {
      setLongitudeRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper longitude range limit for this joint. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper longitude range limit (in degrees)
    */
   public void setMaxLongitude (double max) {
      setLongitudeRange (new DoubleInterval (getMinLongitude(), max));
   }

   /**
    * Sets the lower longitude range limit for this joint. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower longitude range limit (in degrees)
    */
   public void setMinLongitude (double min) {
      setLongitudeRange (new DoubleInterval (min, getMaxLongitude()));
   }

   /**
    * Queries whether the longitude coordinate for this joint is locked.
    *
    * @return {@code true} if longitude is locked
    */
   public boolean isLongitudeLocked() {
      return isCoordinateLocked (LONGITUDE_IDX);
   }

   /**
    * Set whether the longitude coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks longitude
    */
   public void setLongitudeLocked (boolean locked) {
      setCoordinateLocked (LONGITUDE_IDX, locked);
   }

   /**
    * Queries this joint's latitude angle. See this class's Javadoc header for
    * a description of this coordinate.
    *
    * @return latitude angle (in degrees)
    */
   public double getLatitude() {
      return RTOD*getCoordinate (LATITUDE_IDX);
   }

   /**
    * Sets this joint's latitude angle. See this class's Javadoc header for
    * a description of this coordinate.
    *
    * @param latitude new latitude angle (in degrees)
    */
   public void setLatitude (double latitude) {
      setCoordinate (LATITUDE_IDX, DTOR*latitude);
   }

   /**
    * Queries the latitude range limits for this joint. See {@link
    * #setLatitudeRange(DoubleInterval)} for more details.
    *
    * @return latitude range limits for this joint (in degrees)
    */
   public DoubleInterval getLatitudeRange () {
      return getCoordinateRangeDeg (LATITUDE_IDX);
   }

   /**
    * Queries the lower latitude range limit for this joint.
    *
    * @return lower latitude range limit (in degrees)
    */
   public double getMinLatitude () {
      return getMinCoordinateDeg (LATITUDE_IDX);
   }

   /**
    * Queries the upper latitude range limit for this joint.
    *
    * @return upper latitude range limit (in degrees)
    */
   public double getMaxLatitude () {
      return getMaxCoordinateDeg (LATITUDE_IDX);
   }

   /**
    * Sets the range limits of the latitude for this joint. The default range
    * is {@code [-inf, inf]}, which implies no limits. If the latitude travels
    * beyond these limits during dynamic simulation, unilateral constraints
    * will be activated to enforce them. Setting the lower limit to {@code
    * -inf} or the upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range latitude range limits for this joint (in degrees)
    */
   public void setLatitudeRange (DoubleInterval range) {
      setCoordinateRangeDeg (LATITUDE_IDX, range);
   }
   
   /**
    * Sets the latitude range limits for this joint. This is a convenience
    * wrapper for {@link #setLatitudeRange(DoubleInterval)}.
    *
    * @param min minimum latitude angle (in degrees)
    * @param max maximum latitude angle (in degrees)
    */
   public void setLatitudeRange(double min, double max) {
      setLatitudeRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper latitude range limit for this joint. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper latitude range limit (in degrees)
    */
   public void setMaxLatitude (double max) {
      setLatitudeRange (new DoubleInterval (getMinLatitude(), max));
   }

   /**
    * Sets the lower latitude range limit for this joint. Setting a
    * value of {@code -inf} removes the lower limit.
    *
    * @param min lower latitude range limit (in degrees)
    */
   public void setMinLatitude (double min) {
      setLatitudeRange (new DoubleInterval (min, getMaxLatitude()));
   }

   /**
    * Queries whether the latitude coordinate for this joint is locked.
    *
    * @return {@code true} if latitude is locked
    */
   public boolean isLatitudeLocked() {
      return isCoordinateLocked (LATITUDE_IDX);
   }

   /**
    * Set whether the latitude coordinate for this joint is locked.
    *
    * @param locked if {@code true}, locks latitude
    */
   public void setLatitudeLocked (boolean locked) {
      setCoordinateLocked (LATITUDE_IDX, locked);
   }

   /**
    * Queries this joint's theta angle. See this class's Javadoc header for a
    * description of this coordinate.  more details.
    *
    * @return theta angle (in degrees)
    */
   public double getTheta() {
      return RTOD*getCoordinate (THETA_IDX);
   }

   /**
    * Sets this joint's theta angle. See this class's Javadoc header for a
    * description of this coordinate.
    *
    * @param theta new theta angle (in degrees)
    */
   public void setTheta (double theta) {
      setCoordinate (THETA_IDX, DTOR*theta);
   }

   /**
    * Queries the theta angle range limits for this joint. See {@link
    * #setThetaRange(DoubleInterval)} for more details.
    *
    * @return theta range limits for this joint (in degrees)
    */
   public DoubleInterval getThetaRange () {
      return getCoordinateRangeDeg (THETA_IDX);
   }

   /**
    * Queries the lower theta range limit for this joint.
    *
    * @return lower theta range limit (in degrees)
    */
   public double getMinTheta () {
      return getMinCoordinateDeg (THETA_IDX);
   }

   /**
    * Queries the upper theta range limit for this joint.
    *
    * @return upper theta range limit (in degrees)
    */
   public double getMaxTheta () {
      return getMaxCoordinateDeg (THETA_IDX);
   }

   /**
    * Sets the range limits of theta for this joint. The default range
    * is {@code [-inf, inf]}, which implies no limits. If theta travels
    * beyond these limits during dynamic simulation, unilateral constraints
    * will be activated to enforce them. Setting the lower limit to {@code
    * -inf} or the upper limit to {@code inf} removes the lower or upper limit,
    * respectively. Specifying {@code range} as {@code null} will set the range
    * to {@code (-inf, inf)}.
    *
    * @param range theta range limits for this joint (in degrees)
    */
   public void setThetaRange (DoubleInterval range) {
      setCoordinateRangeDeg (THETA_IDX, range);
   }
   
   /**
    * Sets the theta range limits for this joint. This is a
    * convenience wrapper for {@link #setThetaRange(DoubleInterval)}.
    *
    * @param min minimum theta angle (in degrees)
    * @param max maximum theta angle (in degrees)
    */
   public void setThetaRange(double min, double max) {
      setThetaRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper theta range limit for this joint. Setting a
    * value of {@code inf} removes the upper limit.
    *
    * @param max upper theta range limit (in degrees)
    */
   public void setMaxTheta (double max) {
      setThetaRange (new DoubleInterval (getMinTheta(), max));
   }

   /**
    * Sets the lower theta range limit for this joint. Setting a value of
    * {@code -inf} removes the lower limit.
    *
    * @param min lower theta range limit (in degrees)
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
    * Queries this joint's phi angle. See this class's Javadoc header for a
    * description of this coordinate.
    *
    * @return phi angle (in degrees)
    */
   public double getPhi() {
      return RTOD*getCoordinate (PHI_IDX);
   }

   /**
    * Sets this joint's phi angle. See this class's Javadoc header for a
    * description of this coordinate.
    *
    * @param phi new phi angle (in degrees)
    */
   public void setPhi (double phi) {
      setCoordinate (PHI_IDX, DTOR*phi);
   }

   /**
    * Queries the phi angle range limits for this joint. See {@link
    * #setPhiRange(DoubleInterval)} for more details.
    *
    * @return phi range limits for this joint (in degrees)
    */
   public DoubleInterval getPhiRange () {
      return getCoordinateRangeDeg (PHI_IDX);
   }

   /**
    * Queries the lower phi range limit for this joint, in degrees.
    *
    * @return lower phi range limit (in degrees)
    */
   public double getMinPhi () {
      return getMinCoordinateDeg (PHI_IDX);
   }

   /**
    * Queries the upper phi range limit for this joint, in degrees.
    *
    * @return upper phi range limit (in degrees)
    */
   public double getMaxPhi () {
      return getMaxCoordinateDeg (PHI_IDX);
   }

   /**
    * Sets the range limits of phi for this joint. The default range is {@code
    * [-inf, inf]}, which implies no limits. If phi travels beyond these limits
    * during dynamic simulation, unilateral constraints will be activated to
    * enforce them. Setting the lower limit to {@code -inf} or the upper limit
    * to {@code inf} removes the lower or upper limit, respectively. Specifying
    * {@code range} as {@code null} will set the range to {@code (-inf, inf)}.
    *
    * @param range phi range limits for this joint (in degrees)
    */
   public void setPhiRange (DoubleInterval range) {
      setCoordinateRangeDeg (PHI_IDX, range);
   }
   
   /**
    * Sets the phi range limits for this joint. This is a
    * convenience wrapper for {@link #setPhiRange(DoubleInterval)}.
    *
    * @param min minimum phi angle (in degrees)
    * @param max maximum phi angle (in degrees)
    */
   public void setPhiRange(double min, double max) {
      setPhiRange(new DoubleInterval(min, max));
   }

   /**
    * Sets the upper phi range limit for this joint. Setting a value of {@code
    * inf} removes the upper limit.
    *
    * @param max upper phi range limit (in degrees)
    */
   public void setMaxPhi (double max) {
      setPhiRange (new DoubleInterval (getMinPhi(), max));
   }

   /**
    * Sets the lower phi range limit for this joint. Setting a value of {@code
    * -inf} removes the lower limit.
    *
    * @param min lower phi range limit (in degrees)
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
    * Queries the semi-axis lengths for this ellipsoid.
    *
    * @return 3-vector containing the semi-axis lengths
    */
   public Vector3d getSemiAxisLengths () {
      return getCoupling().getSemiAxisLengths();
   }
   
   // /**
   //  * Queries the size used to render this joint's tangent plane as a square.
   //  *
   //  * @return size used to render the plane
   //  */
   // public double getPlaneSize() {
   //    return myPlaneSize;
   // }

   // /**
   //  * Sets the size used to render this joint's tangent plane as a square.
   //  *
   //  * @param size used to render the plane
   //  */
   // public void setPlaneSize (double size) {
   //    myPlaneSize = size;
   // }
   
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
      if (myDrawEllipsoid) {
         // set bounds for the ellipsoid
         Vector3d axisLens = getSemiAxisLengths();
         RigidTransform3d TDW = getCurrentTDW();
         Vector3d p = new Vector3d();
         Vector3d axis = new Vector3d();
         for (int i=0; i<3; i++) {
            TDW.R.getColumn (i, axis);
            p.scaledAdd (axisLens.get(i), axis, TDW.p);
            p.updateBounds (pmin, pmax);
            p.scaledAdd (-axisLens.get(i), axis, TDW.p);
            p.updateBounds (pmin, pmax);
         }
      }
      // PlanarConnector.updateXYSquareBounds (
      //    pmin, pmax, getCurrentTDW(), myPlaneSize);
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      // PlanarConnector.renderXYSquare (
      //    renderer, myRenderProps, myRenderFrameC, myPlaneSize, isSelected());
      if (myDrawEllipsoid) {
         flags |= isSelected() ? Renderer.HIGHLIGHT : 0;
         ellipsoid.render (renderer, myRenderProps, flags);
      }
   }

   /* --- end Renderable implementation --- */

   // need to implement write and scan so we can handle the semi axis lengths
   // and the openSimCompatible settings.

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("semiAxisLengths=");
      getCoupling().getSemiAxisLengths().write (
         pw, fmt, /*withBrackets=*/true);      
      if (getCoupling().getAlpha() != 0) {
         pw.print ("alpha=" + fmt.format(getCoupling().getAlpha()));
      }
      if (getCoupling().isOpenSimCompatible()) {
         pw.print ("openSimCompatible=true");
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
      else if (scanAttributeName (rtok, "openSimCompatible")) {
         getCoupling().setOpenSimCompatible (rtok.scanBoolean());
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
