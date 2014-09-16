/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.spatialmotion.SolidCoupling;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SolidJoint extends JointBase implements CopyableComponent {

   public static PropertyList myProps =
      new PropertyList (SolidJoint.class, JointBase.class);
   
   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createLineProps (host);
      return props;
   }

   protected static VectorNd ZERO_VEC = new VectorNd(6);

   static {
      myProps.add (
         "renderProps", "renderer properties", defaultRenderProps (null));
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
      setRenderProps (defaultRenderProps (null));
   }
   
   public SolidJoint() {
      myCoupling = new SolidCoupling ();
   }

   public SolidJoint (RigidBody bodyA, RigidTransform3d XFA,
   RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, XFA, bodyB, XDB);
   }

   public SolidJoint (RigidBody bodyA, RigidTransform3d XFA,
   RigidTransform3d XDW) {
      this();
      setBodies (bodyA, XFA, null, XDW);
   }
   
   public SolidJoint (RigidBody bodyA, RigidBody bodyB, RigidTransform3d XWJ) {
      this();
      RigidTransform3d XFA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      
      XFA.mulInverseLeft(bodyA.getPose(), XWJ);
      XDB.mulInverseLeft(bodyB.getPose(), XWJ);
      
      setBodies(bodyA, XFA, bodyB, XDB);
      
   }
   
   public SolidJoint(RigidBody bodyA, RigidBody bodyB) {
      this();
      RigidTransform3d XFA = new RigidTransform3d();  // identity
      RigidTransform3d XDB = new RigidTransform3d();
      XDB.mulInverseLeft(bodyB.getPose(), bodyA.getPose());
      
      setBodies(bodyA, XFA, bodyB, XDB);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

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
      if (myAxisLength != 0) {
         renderer.drawAxes (
            myRenderProps, myRenderFrame, myAxisLength, isSelected());
      }
   }

   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SolidJoint copy = (SolidJoint)super.copy (flags, copyMap);
      copy.myCoupling = new SolidCoupling ();
      // copy.setNumConstraints (5);
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      copy.setBodies (copy.myBodyA, getTFA(), copy.myBodyB, getTDB());
      return copy;
   }

}
