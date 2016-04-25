/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.spatialmotion.*;

import java.util.*;

import maspack.render.*;
import artisynth.core.modelbase.*;

import java.awt.Color;
import java.io.*;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class FullPlanarJoint extends BodyConnector 
   implements CopyableComponent {
   private double myAxisLength;
   private static final double defaultAxisLength = 1;

   private RigidTransform3d myRenderFrame = new RigidTransform3d();

   public static PropertyList myProps =
      new PropertyList (FullPlanarJoint.class, BodyConnector.class);

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createLineProps (host);
      return props;
   }

   protected static VectorNd ZERO_VEC = new VectorNd(3);

   static {
      myProps.add (
         "axisLength * *", "length of the axis for this joint",
         defaultAxisLength);
      myProps.add (
         "renderProps * *", "renderer properties", defaultRenderProps (null));
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      myAxisLength = defaultAxisLength;
      setRenderProps (defaultRenderProps (null));
   }

   public double getAxisLength() {
      return myAxisLength;
   }

   public void setAxisLength (double len) {
      myAxisLength = len;
   }

   public FullPlanarJoint() {
      setDefaultValues();
      myCoupling = new FullPlanarCoupling ();
   }

   public FullPlanarJoint (RigidBody bodyA, RigidTransform3d TCA,
                           RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   public FullPlanarJoint (RigidBody bodyA, RigidTransform3d TCA,
                           RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }
   
   public FullPlanarJoint (RigidBody bodyA, Vector3d worldPlaneNormal) {
      this();
      RigidTransform3d TDW = new RigidTransform3d ();
      TDW.R.setZDirection (worldPlaneNormal);
      RigidTransform3d TCA = new RigidTransform3d ();
      TCA.mulInverseLeft (bodyA.getPose (), TDW);
      setBodies (bodyA, TCA, null, TDW);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      RigidTransform3d TDW = getCurrentTDW();
      TDW.p.updateBounds (pmin, pmax);
   }

   public void prerender (RenderList list) {
      RigidTransform3d TDW = getCurrentTDW();
      myRenderFrame.set (TDW);
   }

   public void render (Renderer renderer, int flags) {
      if (myAxisLength != 0) {
         int lineWidth = myRenderProps.getLineWidth();
         renderer.drawAxes (
            myRenderFrame, myAxisLength, lineWidth, isSelected());
      }
//       Point3d p0 = new Point3d();
//       Point3d p1 = new Point3d();
//       computeAxisEndPoints (p0, p1, myRenderFrame);

//       float[] coords0 = new float[] { (float)p0.x, (float)p0.y, (float)p0.z };
//       float[] coords1 = new float[] { (float)p1.x, (float)p1.y, (float)p1.z };

//       renderer.drawLine (myRenderProps, coords0, coords1,
//       /* capped= */true, isSelected());
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myAxisLength *= s;
      myRenderProps.scaleDistance (s);
   }
   
   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FullPlanarJoint copy = (FullPlanarJoint)super.copy (flags, copyMap);
      copy.myCoupling = new FullPlanarCoupling ();
      // copy.setNumConstraints (5);
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      return copy;
   }

}
