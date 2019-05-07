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
import maspack.spatialmotion.SlottedRevoluteCoupling;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SlottedRevoluteJoint extends JointBase 
   implements CopyableComponent {

   public static PropertyList myProps =
      new PropertyList (SlottedRevoluteJoint.class, JointBase.class);

   private static DoubleInterval DEFAULT_THETA_RANGE =
      new DoubleInterval ("[-inf,inf])");
   private DoubleInterval myThetaRange = new DoubleInterval(DEFAULT_THETA_RANGE);

   private static DoubleInterval DEFAULT_X_RANGE =
      new DoubleInterval ("[-inf,inf])");
   private DoubleInterval myXRange = new DoubleInterval(DEFAULT_X_RANGE);

   private static double DEFAULT_SLOT_WIDTH = 1;
   private double mySlotWidth = DEFAULT_SLOT_WIDTH;

   // points used to render the revolute axis
   private float[] myAxisPnt0 = new float[3];
   private float[] myAxisPnt1 = new float[3];
   // set of points used to render the slot edges
   private ArrayList<float[]> mySlotEdge0 = new ArrayList<float[]>();
   private ArrayList<float[]> mySlotEdge1 = new ArrayList<float[]>();
   // number of segments used to render edges if joint is deformable
   private static final int myNumEdgeSegments = 10;
   // when bodyB is deformable, point attachments are used to compute the slot
   // edge points from their rest positions
   private ArrayList<PointAttachment> mySlotEdgeAttachments0;
   private ArrayList<PointAttachment> mySlotEdgeAttachments1;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createPointLineProps (host);
      props.setLineColor (Color.BLUE);
      props.setLineStyle (LineStyle.CYLINDER);
      return props;
   }

   protected static VectorNd ZERO_VEC = new VectorNd(6);

   static {
      myProps.add ("theta", "joint angle", 0, "1E %8.3f [-360,360]");
      myProps.add (
         "thetaRange", "range for theta", DEFAULT_THETA_RANGE, "%8.3f 1E");
      myProps.add (
         "x", "translation along the slot", 0);
      myProps.add (
         "xRange", "range for x", DEFAULT_X_RANGE, "%8.3f 1E");
      myProps.get ("renderProps").setDefaultValue (defaultRenderProps (null));
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
      myProps.add (
         "slotWidth", "width of the slot", DEFAULT_SLOT_WIDTH);
   }

   public double getTheta() {
      RigidTransform3d TGD = null;
      if (attachmentsInitialized()) {      
         // initialize TGD to TCD; it will get projected to TGD within
         TGD = new RigidTransform3d();
         getCurrentTCD (TGD);
      }      
      double theta = Math.toDegrees (
         ((SlottedRevoluteCoupling)myCoupling).getTheta(TGD));
      return theta;
   }

   public void setTheta (double theta) {
      RigidTransform3d TGD = null;
      if (isConnectedToBodies()) {
         TGD = new RigidTransform3d();
      }      
      theta = myThetaRange.makeValid (theta);
      ((SlottedRevoluteCoupling)myCoupling).setTheta(TGD, Math.toRadians(theta));
      if (TGD != null) {
         // if we are connected to the hierarchy, adjust the poses of the
         // attached bodies appropriately.
         adjustPoses (TGD);
      }
   }

   public double getX() {
      RigidTransform3d TGD = null;
      if (attachmentsInitialized()) {
         TGD = new RigidTransform3d();
         // initialize TGD to TCD; it will get projected to TGD within
         getCurrentTCD (TGD);
      }      
      double x = ((SlottedRevoluteCoupling)myCoupling).getX(TGD);
      return x;
   }

   public void setX (double x) {
      RigidTransform3d TGD = null;
      if (isConnectedToBodies()) {
         TGD = new RigidTransform3d();
      }
      x = myXRange.makeValid (x);
      ((SlottedRevoluteCoupling)myCoupling).setX(TGD, x);
      if (TGD != null) {
         // if we are connected to the hierarchy, adjust the poses of the
         // attached bodies appropriately.
         adjustPoses (TGD);
      }
   }

   public DoubleInterval getThetaRange () {
      return myThetaRange;
   }

   public void setThetaRange (DoubleInterval range) {
      SlottedRevoluteCoupling coupling = (SlottedRevoluteCoupling)myCoupling;
      coupling.setMaximumTheta (Math.toRadians(range.getUpperBound()));
      coupling.setMinimumTheta (Math.toRadians(range.getLowerBound()));
      myThetaRange.set (range);
      if (isConnectedToBodies()) {
         // if we are connected to the hierarchy, might have to update theta
         double theta = getTheta();
         double clipped = myThetaRange.clipToRange (theta);
         if (clipped != theta) {
            setTheta (clipped);
         }
      }
   }

   public DoubleInterval getXRange () {
      return myXRange;
   }

   public void setXRange (DoubleInterval range) {
      SlottedRevoluteCoupling coupling = (SlottedRevoluteCoupling)myCoupling;
      coupling.setMaximumX (range.getUpperBound());
      coupling.setMinimumX (range.getLowerBound());
      myXRange.set (range);
      if (isConnectedToBodies()) {
         // if we are connected to the hierarchy, might have to update x
         double x = getX();
         double clipped = myXRange.clipToRange (x);
         if (clipped != x) {
            setX (clipped);
         }
      }
   }

   public double getSlotWidth() {
      return mySlotWidth;
   }

   public void setSlotWidth (double w) {
      mySlotWidth = w;
   }

   // public NumericIntervalRange getThetaRangeRange() {
   //    return new NumericIntervalRange (DEFAULT_THETA_RANGE);
   // }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      //setThetaRange (DEFAULT_THETA_RANGE);
      setRenderProps (defaultRenderProps (null));
   }

   public SlottedRevoluteJoint() {
      myThetaRange = new DoubleInterval();
      myCoupling = new SlottedRevoluteCoupling ();
      setThetaRange (DEFAULT_THETA_RANGE);
      setThetaRange (DEFAULT_X_RANGE);
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
      myHasTranslation = true;
   }

   public SlottedRevoluteJoint (RigidBody bodyA, RigidTransform3d TCA,
   RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, TCA, bodyB, XDB);
   }

   public SlottedRevoluteJoint (RigidBody bodyA, RigidTransform3d TCA,
   RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }
   
   public SlottedRevoluteJoint (ConnectableBody bodyA, RigidTransform3d TCW) {
      this();

      setBodies(bodyA, null, TCW);
   }

   public SlottedRevoluteJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TCW) {
      this();

      setBodies(bodyA, bodyB, TCW);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void setMaxTheta (double max) {
      double min = myThetaRange.getLowerBound();
      setThetaRange (new DoubleInterval (min, max));
   }

   public void setMinTheta (double min) {
      double max = myThetaRange.getUpperBound();
      setThetaRange (new DoubleInterval (min, max));
   }

   public void setMaxX (double max) {
      double min = myXRange.getLowerBound();
      setXRange (new DoubleInterval (min, max));
   }

   public double getMaxX () {
      return myXRange.getUpperBound();
   }

   public void setMinX (double min) {
      double max = myXRange.getUpperBound();
      setXRange (new DoubleInterval (min, max));
   }

   public double getMinX () {
      return myXRange.getLowerBound();
   }

   private void computeAxisEndPoints (RigidTransform3d TCW) {
      Vector3d uW = new Vector3d(); // joint axis vector in world coords
      Vector3d pos = new Vector3d();

      uW.set (TCW.R.m02, TCW.R.m12, TCW.R.m22);
      pos.scaledAdd (-0.5 * myAxisLength, uW, TCW.p);
      pos.get (myAxisPnt0);
      pos.scaledAdd (myAxisLength, uW, pos);
      pos.get (myAxisPnt1);
   }

   private void setEdgeSegmentStorage (ArrayList<float[]> edge, int nsegs) {
      if (edge.size() != nsegs+1) {
         edge.clear();
         for (int i=0; i<nsegs+1; i++) {
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
      Vector3d zAxis = new Vector3d();
      
      RigidTransform3d TDW = new RigidTransform3d();
      getCurrentTDW (TDW);
      double width = getSlotWidth();
      double xmax = getMaxX();
      double xmin = getMinX();
      double xdel = (xmax-xmin)/myNumEdgeSegments;
      TDW.R.getColumn (0, xAxis);
      TDW.R.getColumn (2, zAxis);
      Point3d pos = new Point3d();
      for (int i=0; i<=myNumEdgeSegments; i++) {
         pos.scaledAdd (xmin+i*xdel, xAxis, TDW.p);
         pos.scaledAdd (width/2, zAxis, pos);
         mySlotEdgeAttachments0.add (
            myBodyB.createPointAttachment (new Point(pos)));
      }
      for (int i=0; i<=myNumEdgeSegments; i++) {
         pos.scaledAdd (xmin+i*xdel, xAxis, TDW.p);
         pos.scaledAdd (-width/2, zAxis, pos);
         mySlotEdgeAttachments1.add (
            myBodyB.createPointAttachment (new Point(pos)));
      }
   }

   private void computeSlotEdges() {
      if (myBodyB.isDeformable()) {
         if (mySlotEdgeAttachments0 == null) {
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
         Vector3d zAxis = new Vector3d();
         double width = getSlotWidth();
         TDW.R.getColumn (0, xAxis);
         TDW.R.getColumn (2, zAxis);
         setEdgeSegmentStorage (mySlotEdge0, 1);
         setEdgeSegmentStorage (mySlotEdge1, 1);
         Vector3d pos = new Vector3d();

         pos.scaledAdd (getMaxX(), xAxis, TDW.p);
         pos.scaledAdd (width/2, zAxis, pos);
         pos.get (mySlotEdge0.get(0));
         pos.scaledAdd (-width, zAxis, pos);
         pos.get (mySlotEdge1.get(0));

         pos.scaledAdd (getMinX(), xAxis, TDW.p);
         pos.scaledAdd (width/2, zAxis, pos);
         pos.get (mySlotEdge0.get(1));
         pos.scaledAdd (-width, zAxis, pos);
         pos.get (mySlotEdge1.get(1));
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      computeAxisEndPoints (getCurrentTCW());
      RenderableUtils.updateBounds (pmin, pmax, myAxisPnt0);
      RenderableUtils.updateBounds (pmin, pmax, myAxisPnt1);
      computeSlotEdges();
      int numv = mySlotEdge0.size();
      RenderableUtils.updateBounds (pmin, pmax, mySlotEdge0.get(0));
      RenderableUtils.updateBounds (pmin, pmax, mySlotEdge0.get(numv-1));
      RenderableUtils.updateBounds (pmin, pmax, mySlotEdge1.get(0));
      RenderableUtils.updateBounds (pmin, pmax, mySlotEdge1.get(numv-1));
   }

   public void prerender (RenderList list) {
      super.prerender (list);
      computeAxisEndPoints (getCurrentTCW());
      computeSlotEdges();
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);

      renderer.drawLine (myRenderProps, myAxisPnt0, myAxisPnt1,
                         /*color=*/null, /*capped=*/true, isSelected());
      renderer.drawLineStrip (
         myRenderProps, mySlotEdge0, LineStyle.LINE, isSelected());
      renderer.drawLineStrip (
         myRenderProps, mySlotEdge1, LineStyle.LINE, isSelected());
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SlottedRevoluteJoint copy =
         (SlottedRevoluteJoint)super.copy (flags, copyMap);
      copy.myCoupling = new SlottedRevoluteCoupling ();
      // copy.setNumConstraints (5);
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      copy.setThetaRange (myThetaRange);
      copy.myAxisPnt0 = new float[3];
      copy.myAxisPnt1 = new float[3];
      copy.setXRange (myXRange);
      copy.mySlotEdge0 = new ArrayList<float[]>();
      copy.mySlotEdge1 = new ArrayList<float[]>();
      copy.mySlotEdgeAttachments0 = null;
      copy.mySlotEdgeAttachments1 = null;
      return copy;
   }

}
