/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.spatialmotion.*;
import java.util.*;
import java.io.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;

/**
 * Legacy class that implements a 3 DOF spherical joint parameterized by
 * roll-pitch-yaw angles. It behaves identically to GimbalJoint, except that
 * the roll-pitch-yaw angles describe the orientation of D with respect to C,
 * instead of C with respect to D.
 */
public class SphericalRpyJoint extends GimbalJoint {

   /**
    * Creates a {@code SphericalRpyJoint} which is not attached to any bodies.  It
    * can subsequently be connected using one of the {@code setBodies} methods.
    */
   public SphericalRpyJoint () {
      super();
      // setting useRDC = true is what makes this differ from a GimbalJoint
      ((GimbalCoupling)myCoupling).setUseRDC (true);
      // use legacy rendering, with just point properties:
      setJointRadius (-1);
   }

   /**
    * Creates a {@code SphericalRpyJoint} connecting two rigid bodies, {@code
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
   public SphericalRpyJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB) {
      this();
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   /**
    * Creates a {@code SphericalRpyJoint} connecting two connectable bodies,
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
   public SphericalRpyJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW) {
      this();
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a {@code SphericalRpyJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code roll}, {@code pitch} and {@code
    * yaw} with have initial values of 0. D (and C) is located by {@code TDW},
    * which gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param bodyB body B
    * @param TDW initial transform from joint frames D and C to world
    */   
   public SphericalRpyJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a {@code SphericalRpyJoint} connecting a single connectable body,
    * {@code bodyA}, to ground. The joint frames D and C are assumed to be
    * initially coincident, so that {@code roll}, {@code pitch} and {@code yaw}
    * with have initial values of 0. D (and C) is located by {@code TDW}, which
    * gives the transform from D to world coordinates.
    *
    * @param bodyA body A
    * @param TDW initial transform from joint frames D and C to world
    */
   public SphericalRpyJoint (
      ConnectableBody bodyA, RigidTransform3d TDW) {
      this();
      setBodies(bodyA, null, TDW);
   }

   /**
    * Creates a {@code SphericalRpyJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}. The joint frames D and C are assumed to
    * be initially coincident, so that {@code roll}, {@code pitch} and {@code
    * yaw} with have initial values of 0. D (and C) is located (with respect to
    * world) so that its origin is at {@code pd} and its axes are aligned with
    * the world.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B, or {@code null} if {@code bodyA} is connected
    * to ground.
    * @param pd origin of frame D (world coordinates)
    */
   public SphericalRpyJoint (
      ConnectableBody bodyA, ConnectableBody bodyB, Point3d pd) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (pd);
      setBodies (bodyA, bodyB, TDW);
   }


   public void printCoords (String msg) {
      VectorNd coords = new VectorNd();
      getCoordinates(coords);
      coords.scale (180/Math.PI);
      System.out.println (msg);
      System.out.println (coords.toString ("%8.3f"));
   }

}
