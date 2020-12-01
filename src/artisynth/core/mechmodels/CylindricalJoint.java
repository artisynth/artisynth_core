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
import maspack.spatialmotion.CylindricalCoupling;
import maspack.util.DoubleInterval;
import maspack.util.ReaderTokenizer;

/**
 * Implements a 2 DOF cylindrical joint
 */
public class CylindricalJoint extends JointBase 
   implements CopyableComponent {

   public static PropertyList myProps =
      new PropertyList (CylindricalJoint.class, JointBase.class);

   private static DoubleInterval DEFAULT_Z_RANGE =
      new DoubleInterval ("[-inf,inf])");
   private DoubleInterval myZRange = new DoubleInterval(DEFAULT_Z_RANGE);

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
      myProps.add ("z", "slider distance", 0);
      myProps.add (
         "zRange", "range for z", DEFAULT_Z_RANGE);
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
         ((CylindricalCoupling)myCoupling).getTheta(TGD));
      return theta;
   }

   public void setTheta (double theta) {
      theta = myThetaRange.makeValid (theta);
      RigidTransform3d TGD = null;
      if (isConnectedToBodies()) {
         TGD = new RigidTransform3d();
         getCurrentTCD (TGD);
      }     
      ((CylindricalCoupling)myCoupling).setTheta(TGD, Math.toRadians(theta));
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
      CylindricalCoupling coupling = (CylindricalCoupling)myCoupling;
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

   public double getZ() {
      RigidTransform3d TGD = null;
      if (attachmentsInitialized()) {
         // initialize TGD to TCD; it will get projected to TGD within
         TGD = new RigidTransform3d();
         getCurrentTCD (TGD);
      }
      double z = ((CylindricalCoupling)myCoupling).getZ(TGD);
      return z;
   }

   public void setZ (double z) {
      z = myZRange.makeValid (z);
      RigidTransform3d TGD = null;
      if (isConnectedToBodies()) {
         TGD = new RigidTransform3d();
         getCurrentTCD (TGD);
      }     
      ((CylindricalCoupling)myCoupling).setZ(TGD, z);
      if (TGD != null) {
         // if we are connected to the hierarchy, adjust the poses of the
         // attached bodies appropriately.
         adjustPoses (TGD);
      }
   }

   public DoubleInterval getZRange () {
      return myZRange;
   }

   public void setZRange (DoubleInterval range) {
      CylindricalCoupling coupling = (CylindricalCoupling)myCoupling;
      coupling.setMaximumZ (range.getUpperBound());
      coupling.setMinimumZ (range.getLowerBound());
      myZRange.set (range);
      if (isConnectedToBodies()) {
         // if we are connected to the hierarchy, might have to update z
         double z = getZ();
         double clipped = myZRange.clipToRange (z);
         if (clipped != z) {
            setZ (clipped);
         }
      }
   }
   
   public void setZRange(double min, double max) {
      setZRange(new DoubleInterval(min, max));
   }

   // public NumericIntervalRange getZRangeRange() {
   //    return new NumericIntervalRange (DEFAULT_Z_RANGE);
   // }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      //setZRange (DEFAULT_Z_RANGE);
      setRenderProps (defaultRenderProps (null));
   }

   public CylindricalJoint() {
      myZRange = new DoubleInterval();
      myThetaRange = new DoubleInterval();
      myCoupling = new CylindricalCoupling ();
      setZRange (DEFAULT_Z_RANGE);
      setThetaRange (DEFAULT_THETA_RANGE);
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
   }

   public CylindricalJoint (RigidBody bodyA, RigidTransform3d TCA,
   RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, TCA, bodyB, XDB);
   }

   public CylindricalJoint (RigidBody bodyA, RigidTransform3d TCA,
   RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }
   
   public CylindricalJoint (ConnectableBody bodyA, RigidTransform3d TCW) {
      this();

      setBodies (bodyA, null, TCW);
   }

   public CylindricalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TCW) {
      this();
      setBodies (bodyA, bodyB, TCW);
   }

   public CylindricalJoint (
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

   public void setMinTheta (double min) {
      double max = myThetaRange.getUpperBound();
      setThetaRange (new DoubleInterval (min, max));
   }

   public void setMaxZ (double max) {
      double min = myZRange.getLowerBound();
      setZRange (new DoubleInterval (min, max));
   }

   public void setMinZ (double min) {
      double max = myZRange.getUpperBound();
      setZRange (new DoubleInterval (min, max));
   }

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
      CylindricalJoint copy = (CylindricalJoint)super.copy (flags, copyMap);
      copy.myCoupling = new CylindricalCoupling ();
      // copy.setNumConstraints (5);
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      copy.setZRange (myZRange);
      copy.setThetaRange (myThetaRange);
      return copy;
   }

}

