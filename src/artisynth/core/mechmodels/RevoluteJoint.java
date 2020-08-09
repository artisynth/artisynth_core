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
import maspack.spatialmotion.RevoluteCoupling;
import maspack.util.DoubleInterval;
import maspack.util.ReaderTokenizer;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class RevoluteJoint extends JointBase 
   implements CopyableComponent {

   public static PropertyList myProps =
      new PropertyList (RevoluteJoint.class, JointBase.class);

   private static DoubleInterval DEFAULT_THETA_RANGE =
      new DoubleInterval ("[-inf,inf])");
   private DoubleInterval myThetaRange = new DoubleInterval(DEFAULT_THETA_RANGE);

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
      myProps.get ("renderProps").setDefaultValue (defaultRenderProps (null));
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
   }

   public double getTheta() {
      RigidTransform3d TGD = null;
      if (attachmentsInitialized()) {
         // initialize TGD to TCD; it will get projected to TGD within
         TGD = new RigidTransform3d();
         getCurrentTCD (TGD);
      }
      double theta = Math.toDegrees (
         ((RevoluteCoupling)myCoupling).getTheta(TGD));
      return theta;
   }

   public void setTheta (double theta) {
      RigidTransform3d TGD = null;
      if (isConnectedToBodies()) {
         TGD = new RigidTransform3d();
      }     
      theta = myThetaRange.makeValid (theta);
      ((RevoluteCoupling)myCoupling).setTheta(TGD, Math.toRadians(theta));
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
      RevoluteCoupling coupling = (RevoluteCoupling)myCoupling;
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
   
   public void setThetaRange(double min, double max) {
      setThetaRange(new DoubleInterval(min, max));
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

   public RevoluteJoint() {
      myThetaRange = new DoubleInterval();
      myCoupling = new RevoluteCoupling ();
      setThetaRange (DEFAULT_THETA_RANGE);
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
   }

   public RevoluteJoint (RigidBody bodyA, RigidTransform3d TCA,
   RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, TCA, bodyB, XDB);
   }

   public RevoluteJoint (RigidBody bodyA, RigidTransform3d TCA,
   RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }
   
   public RevoluteJoint (ConnectableBody bodyA, RigidTransform3d TCW) {
      this();

      setBodies (bodyA, null, TCW);
   }

   public RevoluteJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TCW) {
      this();
      setBodies (bodyA, bodyB, TCW);
   }

   public RevoluteJoint (
      RigidBody bodyA, ConnectableBody bodyB, Point3d pc, Vector3d axis) {
      this();
      RigidTransform3d TCW = new RigidTransform3d();
      TCW.p.set (pc);
      TCW.R.setZDirection (axis);
      setBodies (bodyA, bodyB, TCW);
   }
   

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void setMaxTheta (double max) {
      double min = myThetaRange.getLowerBound();
      setThetaRange (new DoubleInterval (min, max));
   }

   // public double getMaxTheta() {
   //    return Math.toDegrees(((RevoluteCoupling)myCoupling).getMaximumTheta());
   // }

   public void setMinTheta (double min) {
      double max = myThetaRange.getUpperBound();
      setThetaRange (new DoubleInterval (min, max));
   }

   // public double getMinTheta() {
   //    return Math.toDegrees(((RevoluteCoupling)myCoupling).getMinimumTheta());
   // }

   private void computeAxisEndPoints (
      Point3d p0, Point3d p1, RigidTransform3d TDW) {
      Vector3d uW = new Vector3d(); // joint axis vector in world coords

      // first set p0 to contact center in world coords
      p0.set (TDW.p);
      // now get axis unit vector in world coords
      uW.set (TDW.R.m02, TDW.R.m12, TDW.R.m22);
      p0.scaledAdd (-0.5 * myAxisLength, uW, p0);
      p1.scaledAdd (myAxisLength, uW, p0);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      computeAxisEndPoints (p0, p1, getCurrentTDW());
      p0.updateBounds (pmin, pmax);
      p1.updateBounds (pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);

      if (myAxisLength > 0) {
         Point3d p0 = new Point3d();
         Point3d p1 = new Point3d();
         computeAxisEndPoints (p0, p1, myRenderFrameD);
   
         float[] coords0 = new float[] { (float)p0.x, (float)p0.y, (float)p0.z };
         float[] coords1 = new float[] { (float)p1.x, (float)p1.y, (float)p1.z };
   
         renderer.drawLine (myRenderProps, coords0, coords1,
                            /*color=*/null, /*capped=*/true, isSelected());
      }
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      RevoluteJoint copy = (RevoluteJoint)super.copy (flags, copyMap);
      copy.myCoupling = new RevoluteCoupling ();
      // copy.setNumConstraints (5);
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      copy.setThetaRange (myThetaRange);
      return copy;
   }

}
