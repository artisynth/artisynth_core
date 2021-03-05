
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
import maspack.spatialmotion.HingeCoupling;
import maspack.util.DoubleInterval;
import maspack.util.ReaderTokenizer;

/**
 * Legacy class that implements a 1 DOF revolute joint, in which frame C
 * rotates about the z axis of frame D. It identically to HingeJoint, except
 * that the coordinate {@code theta} is describes the <i>clockwise</i> rotation
 * instead of the <i>counter-clockwise</i> rotation.
 */
public class RevoluteJoint extends HingeJoint {

   /**
    * Creates a {@code RevoluteJoint} which is not attached to any bodies.
    * It can subsequently be connected using one of the {@code setBodies}
    * methods.
    */
   public RevoluteJoint() {
      super();
      HingeCoupling coupling = (HingeCoupling)myCoupling;
      coupling.setThetaClockwise (true);
      // use legacy rendering, with just line properties:
      setShaftLength (-1);
   }

   /**
    * Creates a {@code RevoluteJoint} connecting two rigid bodies, {@code
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
   public RevoluteJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   /**
    * Creates a {@code RevoluteJoint} connecting two connectable bodies, {@code
    * bodyA} and {@code bodyB}. The joint frames C and D are located
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
   public RevoluteJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code RevoluteJoint} connecting two connectable bodies, {@code
    * bodyA} and {@code bodyB}. The joint frames D and C are assumed to be
    * initially coincident, so that {@code theta} will have an initial value of
    * 0. D (and C) is located by {@code TDW}, which gives the transform from D
    * to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */
   public RevoluteJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code RevoluteJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, so that {@code theta} will have an initial value of
    * 0. D (and C) is located by {@code TDW}, which gives the transform from D
    * to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public RevoluteJoint (ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, null, TDW);
   }

   /**
    * Creates a {@code RevoluteJoint} connecting two connectable bodies, {@code
    * bodyA} and {@code bodyB}. The joint frames D and C are assumed to be
    * initially coincident, so that {@code theta} will have an initial value of
    * 0. D (and C) is located (with respect to world) so that its origin is at
    * {@code originD} and its z axis in the direction of {@code zaxis}.
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
   public RevoluteJoint (
      RigidBody bodyA, ConnectableBody bodyB, Point3d originD, Vector3d zaxis) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (originD);
      TDW.R.setZDirection (zaxis);
      setBodies (bodyA, bodyB, TDW);
   }   

}
