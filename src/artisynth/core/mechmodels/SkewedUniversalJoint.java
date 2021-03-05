/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.spatialmotion.UniversalCoupling;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a skewed 2 DOF roll-pitch joint, in which the roll and pitch
 * joints are skewed with respect to each other by an angle {@code skewAngle},
 * such that the angle between them is {@code PI/2 - skewAngle}.
 */
public class SkewedUniversalJoint extends UniversalJoint {

   public double getSkewAngle () {
      return ((UniversalCoupling)myCoupling).getSkewAngle();
   }

   protected void setSkewAngle (double ang) {
      if (ang <= -Math.PI || ang >= Math.PI) {
         throw new IllegalArgumentException (
            "Skew angle must lie within the open interval (-PI,PI)");
      }
      ((UniversalCoupling)myCoupling).setSkewAngle (ang);
   }

   public SkewedUniversalJoint() {
      super();
   }

   /**
    * Creates a SkewedUniversalJoint between {@code bodyA} and {@code bodyB}.
    * The skew angle is defined such that the angle between the roll and pitch
    * axes is given by {@code PI/2 - skewAngle}. If A and B describe the
    * coordinate frames of {@code bodyA} and {@code bodyB}, then {@code TCA}
    * and {@code TDB} give the (fixed) transforms from the joint's C and D
    * frames to A and B, respectively. Since C and D are specified
    * independently, they may not initially be coincident.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground, with {@code TDB} then being the same as {@code
    * TDW}.
    *
    * @param bodyA first body connected to the joint
    * @param TCA transform from the joint C frame to body A
    * @param bodyB second body connected to the joint (or {@code null})
    * @param TDB transform from the joint D frame to body B
    * @param skewAngle skew angle (must lie in the open interval (-PI/2, PI/2).
    */
   public SkewedUniversalJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB, double skewAngle) {
      this();
      setSkewAngle (skewAngle);
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   /**
    * Creates a {@code SkewedUniversalJoint} connecting two connectable bodies,
    * {@code bodyA} and {@code bodyB}.  The skew angle is defined such that the
    * angle between the roll and pitch axes is given by {@code PI/2 -
    * skewAngle}. The joint frames C and D are located independently with
    * respect to world coordinates by {@code TCW} and {@code TDW}.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA body A
    * @param bodyB body B (or {@code null})
    * @param TCW initial transform from joint frame C to world
    * @param TDW initial transform from joint frame D to world
    */
   public SkewedUniversalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TCW, RigidTransform3d TDW, double skewAngle) {
      this();
      setSkewAngle (skewAngle);
      setBodies (bodyA, bodyB, TCW, TDW);
   }

   /**
    * Creates a SkewedUniversalJoint between {@code bodyA} and {@code bodyB},
    * with the initial joint coordinate frame D given (in world coordinates) by
    * {@code TDW}. The skew angle is defined such that the angle between
    * the roll and pitch axes is given by {@code PI/2 - skewAngle}.
    *
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA first body connected to the joint
    * @param bodyB second body connected to the joint (ot {@code null})
    * @param TDW joint coordinate frame D in world coordinates
    * @param skewAng skew angle (must lie in the open interval (-PI/2, PI/2).
    */
   public SkewedUniversalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TDW, double skewAng) {
      this();
      setSkewAngle (skewAng);
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a SkewedUniversalJoint between {@code bodyA} and {@code bodyB},
    * with the origin of D given by {@code originD} and the initial directions
    * of the roll and pitch axes given by {@code rollAxis} and {@code
    * pitchAxis}.
    * 
    * <p>Specifying {@code bodyB} as {@code null} will cause {@code bodyA} to
    * be connected to ground.
    *
    * @param bodyA first body connected to the joint
    * @param bodyB second body connected to the joint (ot {@code null})
    * @param originD initial origin of frame D (world coordinates)
    * @param rollAxis initial direction of the roll axis (world coordinates)
    * @param pitchAxis initial direction of the pitch axis (world coordinates)
    */
   public SkewedUniversalJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      Vector3d originD, Vector3d rollAxis, Vector3d pitchAxis) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      double skewAng = computeTDW (TDW, originD, rollAxis, pitchAxis);
      setSkewAngle (skewAng);
      setBodies(bodyA, bodyB, TDW);
   }

   private double computeTDW (
      RigidTransform3d TDW, Vector3d originD,
      Vector3d rollAxis, Vector3d pitchAxis) {

      if (rollAxis.norm() == 0) {
         throw new IllegalArgumentException ("rollAxis has zero length");
      }
      if (pitchAxis.norm() == 0) {
         throw new IllegalArgumentException ("pitchAxis has zero length");
      }
      TDW.p.set (originD);

      Vector3d zdir = new Vector3d(rollAxis);
      zdir.normalize();
      Vector3d pdir = new Vector3d(pitchAxis);
      pdir.normalize();
      Vector3d xdir = new Vector3d();
      xdir.cross (pdir, zdir);
      double xmag = xdir.norm();
      if (xmag < 1e-8) {
         throw new IllegalArgumentException (
            "rollAxis and pitchAxis are colinear within 1e-8");
      }
      TDW.R.setZXDirections (zdir, xdir);      
      return Math.atan2 (zdir.dot(pdir), xmag);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("skewAngle=" + fmt.format(getSkewAngle()));
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "skewAngle")) {
         setSkewAngle (rtok.scanNumber());
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

}

