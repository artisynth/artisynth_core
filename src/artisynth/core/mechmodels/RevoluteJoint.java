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
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.spatialmotion.RevoluteCoupling;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

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
      RenderProps props = RenderProps.createLineProps (host);
      props.setLineColor (Color.BLUE);
      props.setLineStyle (RenderProps.LineStyle.CYLINDER);
      return props;
   }

   protected static VectorNd ZERO_VEC = new VectorNd(6);

   static {
      myProps.add ("theta", "joint angle", 0, "1E %8.3f [-360,360]");
      myProps.add (
         "thetaRange", "range for theta", DEFAULT_THETA_RANGE, "%8.3f 1E");
      myProps.add (
         "renderProps", "renderer properties", defaultRenderProps (null));
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
   }

   public double getTheta() {
      RigidTransform3d XAW = myBodyA.getPose();
      RigidTransform3d XBW = 
         myBodyB != null ? myBodyB.getPose() : RigidTransform3d.IDENTITY;
      
      // initialize XGD to XCD; it will get projected to XGD within
      // myCoupling.getTheta();
      RigidTransform3d XCA = new RigidTransform3d();
      RigidTransform3d XGD = new RigidTransform3d();
      getCurrentTCA (XCA);
      getCurrentTDB (XGD);
      XGD.mulInverseBoth (XGD, XBW);
      XGD.mul (XAW);
      XGD.mul (XCA);
      
      double theta = Math.toDegrees (
         ((RevoluteCoupling)myCoupling).getTheta(XGD));
      return theta;
   }

   public void setTheta (double theta) {
      theta = myThetaRange.makeValid (theta);
      RigidTransform3d XGD = new RigidTransform3d();
      ((RevoluteCoupling)myCoupling).setTheta(XGD, Math.toRadians(theta));
      if (getParent() != null) {
         // if we are connected to the hierarchy, adjust the poses of the
         // attached bodies appropriately.
         RigidTransform3d XBA = new RigidTransform3d();      
         XBA.mulInverseBoth (XGD, getTDB());
         XBA.mul (getTCA(), XBA);
         setPoses (XBA);
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
      if (getParent() != null) {
         // we are attached - might have to update theta
         double theta = getTheta();
         double clipped = myThetaRange.clipToRange (theta);
         if (clipped != theta) {
            setTheta (clipped);
         }
      }
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

   public RevoluteJoint (RigidBody bodyA, RigidTransform3d XCA,
   RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, XCA, bodyB, XDB);
   }

   public RevoluteJoint (RigidBody bodyA, RigidTransform3d XCA,
   RigidTransform3d XDW) {
      this();
      setBodies (bodyA, XCA, null, XDW);
   }
   
   public RevoluteJoint (RigidBody bodyA, RigidTransform3d XCW) {
      this();
      RigidTransform3d XCA = new RigidTransform3d();
      
      XCA.mulInverseLeft(bodyA.getPose(), XCW);
      setBodies(bodyA, XCA, null, XCW);
   }

   public RevoluteJoint (RigidBody bodyA, RigidBody bodyB, RigidTransform3d XCW) {
      this();
      RigidTransform3d XCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      
      XCA.mulInverseLeft(bodyA.getPose(), XCW);
      XDB.mulInverseLeft(bodyB.getPose(), XCW);
      setBodies(bodyA, XCA, bodyB, XDB);
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
      Point3d p0, Point3d p1, RigidTransform3d XDW) {
      Vector3d uW = new Vector3d(); // joint axis vector in world coords

      // first set p0 to contact center in world coords
      p0.set (XDW.p);
      // now get axis unit vector in world coords
      uW.set (XDW.R.m02, XDW.R.m12, XDW.R.m22);
      p0.scaledAdd (-0.5 * myAxisLength, uW, p0);
      p1.scaledAdd (myAxisLength, uW, p0);
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      computeAxisEndPoints (p0, p1, getCurrentTDW());
      p0.updateBounds (pmin, pmax);
      p1.updateBounds (pmin, pmax);
   }

   public void prerender (RenderList list) {
      RigidTransform3d XDW = getCurrentTDW();
      myRenderFrame.set (XDW);
   }

   public void render (GLRenderer renderer, int flags) {
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      computeAxisEndPoints (p0, p1, myRenderFrame);

      float[] coords0 = new float[] { (float)p0.x, (float)p0.y, (float)p0.z };
      float[] coords1 = new float[] { (float)p1.x, (float)p1.y, (float)p1.z };

      renderer.drawLine (myRenderProps, coords0, coords1,
                         /* capped= */true, isSelected());
   }

   // Scanning of the following properties must be deferred until after
   // references have been resolved:
   static String[] deferredProps = new String[] {"theta", "thetaRange"};

   public boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      rtok.nextToken();
      if (ScanWriteUtils.scanAndStorePropertyValues (rtok, this, deferredProps, tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (ScanWriteUtils.postscanPropertyValues (tokens, this, deferredProps)) {
         return true;
      }
      else {
         return super.postscanItem (tokens, ancestor);
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
      copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      copy.setThetaRange (myThetaRange);
      return copy;
   }

}
