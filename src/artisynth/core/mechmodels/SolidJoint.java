/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Map;

import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.spatialmotion.SolidCoupling;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SolidJoint extends JointBase implements CopyableComponent {

   public static PropertyList myProps =
      new PropertyList (SolidJoint.class, JointBase.class);
   
   protected static VectorNd ZERO_VEC = new VectorNd(6);

   static {
      myProps.add (
         "compliance", "compliance for each constraint", ZERO_VEC);
      myProps.add (
         "damping", "damping for each constraint", ZERO_VEC);
   } 
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public SolidJoint() {
      myCoupling = new SolidCoupling ();
   }

   public SolidJoint (RigidBody bodyA, RigidTransform3d TCA,
   RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, TCA, bodyB, XDB);
   }

   public SolidJoint (RigidBody bodyA, RigidTransform3d TCA,
   RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }

   public SolidJoint (RigidBody bodyA, RigidTransform3d TCW) {
      this();
      RigidTransform3d TCA = new RigidTransform3d();
      TCA.mulInverseLeft(bodyA.getPose(), TCW);
      setBodies (bodyA, null, TCW);
   }

   public SolidJoint (RigidBody bodyA, RigidBody bodyB, RigidTransform3d XWJ) {
      this();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      
      TCA.mulInverseLeft(bodyA.getPose(), XWJ);
      XDB.mulInverseLeft(bodyB.getPose(), XWJ);
      
      setBodies(bodyA, TCA, bodyB, XDB);
      
   }
   
   public SolidJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TFW) {
      this();
      setBodies(bodyA, bodyB, TFW);
      
   }
   
   public SolidJoint(RigidBody bodyA, RigidBody bodyB) {
      this();
      RigidTransform3d TCA = new RigidTransform3d();  // identity
      RigidTransform3d XDB = new RigidTransform3d();
      
      if (bodyB != null) {
         XDB.mulInverseLeft(bodyB.getPose(), bodyA.getPose());
         setBodies(bodyA, TCA, bodyB, XDB);
      } else {
         setBodies (bodyA, bodyB, bodyA.getPose ());
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
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      return copy;
   }

}
