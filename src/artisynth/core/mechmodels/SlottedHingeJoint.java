/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.util.*;
import java.io.*;

import maspack.matrix.*;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.DrawMode;
import maspack.spatialmotion.SlottedHingeCoupling;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Implements a 2 DOF ``slotted revolute'' joint, in which frame C rotates
 * <i>clockwise</i> about the z axis of frame D by an angle {@code theta}, and
 * also translates along the x axis of D by a distance {@code x}.
 *
 * <p>The {@code theta} and {@code x} values are available as properties (with
 * {@code theta} given in degrees) which can be read and also, under
 * appropriate circumstances, set.  Setting these values causes an adjustment
 * in the positions of one or both bodies connected to this joint, along with
 * adjacent bodies connected to them, with preference given to bodies that are
 * not attached to ``ground''.  If this is done during simulation, and
 * particularly if one or both of the bodies connected to this joint are moving
 * dynamically, the results will be unpredictable and will likely conflict with
 * the simulation.
 */
public class SlottedHingeJoint extends JointBase 
   implements CopyableComponent {

   public static final int X_IDX = SlottedHingeCoupling.X_IDX; 
   public static final int THETA_IDX = SlottedHingeCoupling.THETA_IDX; 

   public static PropertyList myProps =
      new PropertyList (SlottedHingeJoint.class, JointBase.class);

   private static DoubleInterval DEFAULT_THETA_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static DoubleInterval DEFAULT_X_RANGE =
      new DoubleInterval ("[-inf,inf])");

   private static double DEFAULT_SLOT_DEPTH = 1;
   private double mySlotDepth = DEFAULT_SLOT_DEPTH;

   private static double DEFAULT_SLOT_WIDTH = 0;
   private double mySlotWidth = DEFAULT_SLOT_WIDTH;
   
   private static DoubleInterval DEFAULT_SLOT_RANGE = new DoubleInterval(0,0);
   private DoubleInterval mySlotRange = new DoubleInterval(DEFAULT_SLOT_RANGE); 
   
   // value of slotWidth used by the renderer (double buffer)
   private double mySlotWidthBuf = DEFAULT_SLOT_WIDTH; 

   // set of points used to render the slot edges
   private ArrayList<float[]> mySlotEdge0 = new ArrayList<float[]>();
   private ArrayList<float[]> mySlotEdge1 = new ArrayList<float[]>();
   // number of segments used to render edges if joint is deformable
   private static final int myNumEdgeSegments = 10;
   // when bodyB is deformable, point attachments are used to compute the slot
   // edge points from their rest positions
   private ArrayList<PointAttachment> mySlotEdgeAttachments0;
   private ArrayList<PointAttachment> mySlotEdgeAttachments1;

   static {
      myProps.add ("theta", "joint angle", 0, "1E %8.3f [-360,360]");
      myProps.add (
         "thetaRange", "range for theta", DEFAULT_THETA_RANGE, "%8.3f 1E");
      myProps.add (
         "x", "translation along the slot", 0);
      myProps.add (
         "xRange", "range for x", DEFAULT_X_RANGE, "%8.3f 1E");
      myProps.add (
         "slotDepth", 
         "depth of the slot for rendering purposes", DEFAULT_SLOT_DEPTH);
      myProps.add (
         "slotWidth", 
         "width of the slot for rendering purposes", DEFAULT_SLOT_WIDTH);
      myProps.add (
         "slotRange", 
         "range of the slot along x for rendering purposes",DEFAULT_SLOT_RANGE);      
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      //setThetaRange (DEFAULT_THETA_RANGE);
      setSlotRange (new DoubleInterval(DEFAULT_SLOT_RANGE));
   }

   /**
    * Creates a {@code SlottedRevoluteJoint} which is not attached to any
    * bodies.  It can subsequently be connected using one of the {@code
    * setBodies} methods.
    */
   public SlottedHingeJoint() {
      setCoupling (new SlottedHingeCoupling());
      setThetaRange (DEFAULT_THETA_RANGE);
      setXRange (DEFAULT_X_RANGE);
      myHasTranslation = true;
   }

   /**
    * Creates a {@code SlottedRevoluteJoint} connecting two rigid bodies,
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
   public SlottedHingeJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }
   
   /**
    * Creates a {@code SlottedRevoluteJoint} connecting two connectable bodies,
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
   public SlottedHingeJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code SlottedRevoluteJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code theta} and {@code x} will have
    * initial values of 0. D (and C) is located by {@code TDW}, which gives the
    * transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */
   public SlottedHingeJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code SlottedRevoluteJoint} connecting a single connectable
    * body, {@code bodyA}, to ground. The joint frames D and C are assumed to
    * be initially coincident, so that {@code theta} and {@code x} will have
    * initial values of 0. D (and C) is located by {@code TDW}, which gives the
    * transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public SlottedHingeJoint (ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, null, TDW);
   }

   /**
    * Creates a {@code SlottedRevoluteJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code theta} and {@code x} will have
    * initial values of 0. D (and C) is located (with respect to world) so that
    * its origin is at {@code pd} and its z axis in the direction of {@code
    * zaxis}.
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
   public SlottedHingeJoint (
      RigidBody bodyA, ConnectableBody bodyB, Point3d originD, Vector3d zaxis) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (originD);
      TDW.R.setZDirection (zaxis);
      setBodies (bodyA, bodyB, TDW);
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
   public void setThetaRange (double min, double max) {
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

   private void clearSlotRenderData() {
      mySlotEdgeAttachments0 = null;
      mySlotEdgeAttachments1 = null;
   }

   public double getSlotDepth() {
      return mySlotDepth;
   }

   public void setSlotDepth (double d) {
      if (d != mySlotDepth) {
         clearSlotRenderData();
         mySlotDepth = d;
      }
   }

   public double getSlotWidth() {
      return mySlotWidth;
   }

   public void setSlotWidth (double w) {
      if (w != mySlotWidth) {
         clearSlotRenderData();
         mySlotWidth = w;
      }
   }

   public DoubleInterval getSlotRange() {
      return mySlotRange;
   }

   public void setSlotRange (DoubleInterval range) {
      if (range == null) {
         range = new DoubleInterval (-INF, INF);
      }
      if (mySlotRange == null || !range.equals (mySlotRange)) {
         clearSlotRenderData();
         mySlotRange = new DoubleInterval (range);
      }
   }

   /* --- begin Renderable implementation --- */

   private void setEdgeSegmentStorage (ArrayList<float[]> edge, int nsegs) {
      if (edge.size() != 2*(nsegs+1)) {
         edge.clear();
         for (int i=0; i<2*(nsegs+1); i++) {
            edge.add (new float[3]);
         }
      }
   }
   
   //
   // XXX FIX: right now, BodyB must be in the rest position
   //
   private void createSlotEdgeAttachments() {
      mySlotEdgeAttachments0 = new ArrayList<PointAttachment>();
      mySlotEdgeAttachments1 = new ArrayList<PointAttachment>();
      Vector3d xAxis = new Vector3d();
      Vector3d yAxis = new Vector3d();
      Vector3d zAxis = new Vector3d();
      
      RigidTransform3d TDW = new RigidTransform3d();
      getCurrentTDW (TDW);
      DoubleInterval slotRange = getXRange();
      if (mySlotRange.getRange() > 0) {
         slotRange = mySlotRange;
      }      
      double xmin = slotRange.getLowerBound();
      double xmax = slotRange.getUpperBound();
      double width = getSlotWidth();
      double depth = getSlotDepth();
      double xdel = (xmax-xmin)/myNumEdgeSegments;
      TDW.R.getColumn (0, xAxis);
      TDW.R.getColumn (1, yAxis);
      TDW.R.getColumn (2, zAxis);
      Point3d pos = new Point3d();
      for (int i=0; i<=myNumEdgeSegments; i++) {
         pos.scaledAdd (xmin+i*xdel, xAxis, TDW.p);
         pos.scaledAdd (depth/2, zAxis);
         pos.scaledAdd (width/2, yAxis);
         mySlotEdgeAttachments0.add (
            myBodyB.createPointAttachment (new Point(pos)));
         pos.scaledAdd (-width, yAxis);
         mySlotEdgeAttachments0.add (
            myBodyB.createPointAttachment (new Point(pos)));
      }
      for (int i=0; i<=myNumEdgeSegments; i++) {
         pos.scaledAdd (xmin+i*xdel, xAxis, TDW.p);
         pos.scaledAdd (-depth/2, zAxis, pos);
         pos.scaledAdd (-width/2, yAxis);
         mySlotEdgeAttachments1.add (
            myBodyB.createPointAttachment (new Point(pos)));
         pos.scaledAdd (width, yAxis);
         mySlotEdgeAttachments1.add (
            myBodyB.createPointAttachment (new Point(pos)));
      }
   }

   private void computeSlotEdges() {
      if (myBodyB.isDeformable()) {
         if (mySlotEdgeAttachments0 == null ||
             mySlotWidth != mySlotWidthBuf) {
            createSlotEdgeAttachments();
            setEdgeSegmentStorage (mySlotEdge0, myNumEdgeSegments);
            setEdgeSegmentStorage (mySlotEdge1, myNumEdgeSegments);
         }
         Vector3d pos = new Vector3d();
         for (int i=0; i<mySlotEdgeAttachments0.size(); i++) {
            PointAttachment a = mySlotEdgeAttachments0.get(i);
            a.getCurrentPos (pos);
            pos.get (mySlotEdge0.get(i));
         }
         for (int i=0; i<mySlotEdgeAttachments1.size(); i++) {
            PointAttachment a = mySlotEdgeAttachments1.get(i);
            a.getCurrentPos (pos);
            pos.get (mySlotEdge1.get(i));
         }
      }
      else {
         RigidTransform3d TDW = new RigidTransform3d();
         getCurrentTDW (TDW);
         Vector3d xAxis = new Vector3d();
         Vector3d yAxis = new Vector3d();
         Vector3d zAxis = new Vector3d();
         DoubleInterval slotRange = getXRange();
         if (mySlotRange.getRange() > 0) {
            slotRange = mySlotRange;
         }
         double xmin = slotRange.getLowerBound();
         double xmax = slotRange.getUpperBound();         
         double depth = getSlotDepth();
         double width = getSlotWidth();
         TDW.R.getColumn (0, xAxis);
         TDW.R.getColumn (1, yAxis);
         TDW.R.getColumn (2, zAxis);
         setEdgeSegmentStorage (mySlotEdge0, 1);
         setEdgeSegmentStorage (mySlotEdge1, 1);
         Vector3d pos = new Vector3d();

         // triangle strip for slotEdge0 (2 triangles):
         pos.scaledAdd (depth/2, zAxis, TDW.p);
         pos.scaledAdd (xmin, xAxis);
         pos.scaledAdd (width/2, yAxis, pos);
         pos.get (mySlotEdge0.get(0));
         pos.scaledAdd (-width, yAxis, pos);
         pos.get (mySlotEdge0.get(1));
         pos.scaledAdd (xmax-xmin, xAxis);
         pos.get (mySlotEdge0.get(3));
         pos.scaledAdd (width, yAxis);
         pos.get (mySlotEdge0.get(2));

         // triangle strip for slotEdge1 (2 triangles):
         pos.scaledAdd (-depth/2, zAxis, TDW.p);
         pos.scaledAdd (xmin, xAxis);
         pos.scaledAdd (width/2, yAxis, pos);
         pos.get (mySlotEdge1.get(1));
         pos.scaledAdd (-width, yAxis, pos);
         pos.get (mySlotEdge1.get(0));
         pos.scaledAdd (xmax-xmin, xAxis);
         pos.get (mySlotEdge1.get(2));
         pos.scaledAdd (width, yAxis);
         pos.get (mySlotEdge1.get(3));
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      super.updateBounds (pmin, pmax);
      updateZShaftBounds (pmin, pmax, getCurrentTCW(), getShaftLength());
      computeSlotEdges();
      int numv = mySlotEdge0.size();
      RenderableUtils.updateBounds (pmin, pmax, mySlotEdge0.get(0));
      RenderableUtils.updateBounds (pmin, pmax, mySlotEdge0.get(numv-1));
      RenderableUtils.updateBounds (pmin, pmax, mySlotEdge1.get(0));
      RenderableUtils.updateBounds (pmin, pmax, mySlotEdge1.get(numv-1));
   }

   public void prerender (RenderList list) {
      super.prerender (list);
      //computeAxisEndPoints (getCurrentTCW());
      if (mySlotWidth > 0) {
         computeSlotEdges();
      }
      mySlotWidthBuf = mySlotWidth;
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      
      renderZShaft (renderer, myRenderFrameC);
      if (mySlotWidthBuf > 0) {
         renderer.setColor (myRenderProps.getFaceColorF(), isSelected());
         renderer.beginDraw (DrawMode.TRIANGLE_STRIP);
         for (int k=0; k<mySlotEdge0.size(); k++) {
            renderer.addVertex (mySlotEdge0.get(k));
         }
         renderer.endDraw ();
         renderer.beginDraw (DrawMode.TRIANGLE_STRIP);
         for (int k=0; k<mySlotEdge1.size(); k++) {
            renderer.addVertex (mySlotEdge1.get(k));
         }
         renderer.endDraw ();
      }
   }

   /* --- end Renderable implementation --- */

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SlottedHingeJoint copy =
         (SlottedHingeJoint)super.copy (flags, copyMap);
      copy.mySlotEdge0 = new ArrayList<float[]>();
      copy.mySlotEdge1 = new ArrayList<float[]>();
      copy.mySlotEdgeAttachments0 = null;
      copy.mySlotEdgeAttachments1 = null;
      return copy;
   }

}
