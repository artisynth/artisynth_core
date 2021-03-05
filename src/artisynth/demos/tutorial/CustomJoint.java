package artisynth.demos.tutorial;

import artisynth.core.mechmodels.ConnectableBody;
import artisynth.core.mechmodels.JointBase;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.RigidBodyConstraint;
import maspack.spatialmotion.RigidBodyCoupling;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.DoubleInterval;

/**
 * Custom joint that actually just implements a simple
 * version of the SlottedRevoluteJoint.
 */
public class CustomJoint extends JointBase {

   // indices for the joint coordinates 
   public static int X_IDX = 0;
   public static int THETA_IDX = 1;

   private static double DEFAULT_SLOT_DEPTH = 1;
   private double mySlotDepth = DEFAULT_SLOT_DEPTH;

   /**
    * Returns the slot render depth.
    *
    * @return slot render depth
    */
   public double getSlotDepth() {
      return mySlotDepth;
   }

   /**
    * Sets the slot render depth. This is the distance between the two lines
    * used to render the slot.
    *
    * @param d slot render depth
    */
   public void setSlotDepth (double d) {
      mySlotDepth = d;
   }

   /**
    * Custom coupling that implements the joint constraints and coordinates.
    */
   class CustomCoupling extends RigidBodyCoupling {

      public CustomCoupling() {
         super();
      }

      @Override
      public void projectToConstraints (
         RigidTransform3d TGD, RigidTransform3d TCD, VectorNd coords) {
         TGD.R.set (TCD.R);
         TGD.R.rotateZDirection (Vector3d.Z_UNIT);
         TGD.p.x = TCD.p.x;
         TGD.p.y = 0;
         TGD.p.z = 0;
         if (coords != null) {
            TCDToCoordinates (coords, TGD);
         }
      }

      @Override
      public void initializeConstraints () {
         // six constraints: 4 bilteral, 2 unilateral for coordinate limits
         addConstraint (BILATERAL|LINEAR);
         addConstraint (BILATERAL|LINEAR, new Wrench(0, 0, 1, 0, 0, 0));
         addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 1, 0, 0));
         addConstraint (BILATERAL|ROTARY, new Wrench(0, 0, 0, 0, 1, 0));
         addConstraint (LINEAR);
         addConstraint (ROTARY, new Wrench(0, 0, 0, 0, 0, 1));
         
         // two coordinates: x and theta
         addCoordinate (-1, 1, 0, getConstraint(4));
         addCoordinate (-2*Math.PI, 2*Math.PI, 0, getConstraint(5));
      }

      @Override
      public void updateConstraints (
         RigidTransform3d TGD, RigidTransform3d TCD, Twist errC,
         Twist velGD, boolean updateEngaged) {
         RigidBodyConstraint cons = getConstraint(0); // constraint along y
         double s = TGD.R.m10;  // sin (theta)
         double c = TGD.R.m00;  // cos (theta)
         // constraint wrench along y is constant in D but needs to be
         // transformed to C
         cons.setWrenchG (s, c, 0, 0, 0, 0);
         // derivative term:
         double dotTheta = velGD.w.z;
         //cinfo.distance = cinfo.wrenchC.dot (errC);
         cons.setDotWrenchG (c*dotTheta, -s*dotTheta, 0, 0, 0, 0);
         
         // update x limit constraint if necessary
         cons = getConstraint(4);
         if (cons.getEngaged() != 0) {
            // constraint wrench along x, transformed to C, is (-c, s, 0)
            cons.setWrenchG (c, -s, 0, 0, 0, 0);
            cons.setDotWrenchG (-s*dotTheta, -c*dotTheta, 0, 0, 0, 0);
         }
         // theta limit constraint is constant; no need to do anything
      }

      @Override
      public void TCDToCoordinates (VectorNd coords, RigidTransform3d TCD) {
         coords.set (X_IDX, TCD.p.x);
         double theta = Math.atan2 (TCD.R.m10, TCD.R.m00);
         coords.set (THETA_IDX, getCoordinate(THETA_IDX).nearestAngle(theta));
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void coordinatesToTCD (RigidTransform3d TCD, VectorNd coords) {

         double x = coords.get (X_IDX);
         double theta = coords.get (THETA_IDX);

         TCD.setIdentity();
         TCD.p.x = x;
         double c = Math.cos (theta);
         double s = Math.sin (theta);
         TCD.R.m00 = c;
         TCD.R.m11 = c;
         TCD.R.m01 = -s;
         TCD.R.m10 = s;
      }

   }

   /**
    * Creates a {@code CustomJoint} which is not attached to any
    * bodies.  It can subsequently be connected using one of the {@code
    * setBodies} methods.
    */
   public CustomJoint() {
      setCoupling (new CustomCoupling());
   }

   /**
    * Creates a {@code CustomJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code theta} and {@code x} will have
    * initial values of 0. D (and C) is located by {@code TDW}, which gives the
    * transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */
   public CustomJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
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
    * Queries this joint's x value. See {@link #setX} for more details.
    *
    * @return current x value
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
    * Sets the x range limits for this joint. The default range
    * is {@code [-inf, inf]}, which implies no limits. If x travels beyond
    * these limits during dynamic simulation, unilateral constraints will be
    * activated to enforce them. Setting the lower limit to {@code -inf} or the
    * upper limit to {@code inf} removes the lower or upper limit,
    * respectively.
    *
    * @param range x range limits for this joint
    */
   public void setXRange (DoubleInterval range) {
      setCoordinateRange (X_IDX, range);
   }

   /* --- begin Renderable implementation --- */

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);

      // extract z axis from frame C and the slider x axis from frame D
      Vector3d zAxisC = new Vector3d();
      Vector3d xAxisD = new Vector3d();
      myRenderFrameC.R.getColumn (2, zAxisC);
      myRenderFrameD.R.getColumn (0, xAxisD);
 
      // points for rendering lines
      Vector3d p0 = new Vector3d();
      Vector3d p1 = new Vector3d();
      
      // draw rotary axis wrt frame C
      p0.scaledAdd (myAxisLength/2, zAxisC, myRenderFrameC.p);
      p1.scaledAdd (-myAxisLength/2, zAxisC, myRenderFrameC.p);

      renderer.setLineColoring (myRenderProps, isSelected());

      renderer.drawCylinder (
         p0, p1, myRenderProps.getLineRadius(), /*capped=*/true);

      if (mySlotDepth > 0) {
         // draw sliders wrt frame D

         renderer.setLineWidth (3);

         DoubleInterval xrange = getXRange();
         double xmax = xrange.getUpperBound();
         double xmin = xrange.getLowerBound();
         
         // slider on +z side
         p0.scaledAdd (mySlotDepth/2, zAxisC, myRenderFrameD.p);
         p1.scaledAdd (xmax, xAxisD, p0);
         p0.scaledAdd (xmin, xAxisD);
         renderer.drawLine (p0, p1);

         // slider on -z side
         p0.scaledAdd (-mySlotDepth/2, zAxisC, myRenderFrameD.p);
         p1.scaledAdd (xmax, xAxisD, p0);
         p0.scaledAdd (xmin, xAxisD);
         renderer.drawLine (p0, p1);

         renderer.setLineWidth (1);
      }
   }

   /* --- end Renderable implementation --- */

}
