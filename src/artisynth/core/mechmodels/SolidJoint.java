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

   public SolidJoint() {
      setCoupling (new SolidCoupling());
   }

   public SolidJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   public SolidJoint (
      RigidBody bodyA, RigidTransform3d TCA, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, TCA, null, TDW);
   }

   public SolidJoint (ConnectableBody bodyA, RigidTransform3d TCW) {
      this();
      setBodies (bodyA, null, TCW);
   }

   public SolidJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
   }
   
   public SolidJoint (RigidBody bodyA, RigidBody bodyB) {
      this();
      RigidTransform3d TCA = new RigidTransform3d();  // identity
      RigidTransform3d TDB = new RigidTransform3d();
      
      if (bodyB != null) {
         TDB.mulInverseLeft(bodyB.getPose(), bodyA.getPose());
         setBodies(bodyA, TCA, bodyB, TDB);
      } 
      else {
         setBodies (bodyA, bodyB, bodyA.getPose ());
      }
      
   }
}
