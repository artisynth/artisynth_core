/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.util.Map;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.Point3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.spatialmotion.UniversalCoupling;
import maspack.util.DoubleInterval;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Legacy class that implements a 2 DOF roll-pitch joint, which allows frame C
 * to rotate with respect to frame D. It behaves identically to {@link
 * UniversalJoint}, except that the roll-pitch angles describe the rotation of
 * D with respect to C, instead of C with respect to D.
 */
public class RollPitchJoint extends UniversalJoint {

   /**
    * Creates a {@code RollPitchJoint} which is not attached to any bodies.  It
    * can subsequently be connected using one of the {@code setBodies} methods.
    */
   public RollPitchJoint() {
      super();
      ((UniversalCoupling)myCoupling).setUseRDC(true);
      // use legacy rendering, with just line and point properties:
      setShaftLength (-1);
      setJointRadius (-1);
   }

   /**
    * Creates a {@code RollPitchJoint} connecting two rigid bodies, {@code
    * bodyA} and {@code bodyB}. If A and B describe the coordinate frames of
    * {@code bodyA} and {@code bodyB}, then {@code TCA} and {@code TDB} give
    * the (fixed) transforms from the joint's C and D frames to A and B,
    * respectively. Since C and D are specified independently, the joint
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
   public RollPitchJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }
   
   /**
    * Creates a {@code RollPitchJoint} connecting two connectable bodies,
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
   public RollPitchJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code RollPitchJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code roll} and {@code pitch} will have
    * initial values of 0. D (and C) is located by {@code TDW}, which gives the
    * transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */
   public RollPitchJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code RollPitchJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, so that {@code roll} and {@code pitch} will have
    * initial values of 0. D (and C) is located by {@code TDW}, which gives the
    * transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public RollPitchJoint (
      ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, null, TDW);
   }
}
