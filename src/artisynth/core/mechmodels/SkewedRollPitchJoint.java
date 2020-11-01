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
import maspack.spatialmotion.RollPitchCoupling;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a skewed 2 DOF roll-pitch joint, it which the roll and pitch
 * joints are skewed with respect to each other by and angle {@code skewAngle},
 * such that the angle between them is {@code PI/2 - skewAngle}.
 */
public class SkewedRollPitchJoint extends RollPitchJoint {

   public double getSkewAngle () {
      return mySkewAngle;
   }

   protected void setSkewAngle (double ang) {
      if (ang <= -Math.PI || ang >= Math.PI) {
         throw new IllegalArgumentException (
            "Skew angle must lie within the open interval (-PI,PI)");
      }
      mySkewAngle = ang;
      myCoupling = new RollPitchCoupling (ang);
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
   }

   public SkewedRollPitchJoint() {
      super();
   }

   /**
    * Creates a SkewedRollPitchJoint between {@code bodyA} and {@code bodyB},
    * with the initial joint coordinate frame D given (in world coordinates) by
    * {@code TDW}. The skew angle is defined such that the angle between
    * the roll and pitch axes is given by {@code PI/2 - skewAngle}.
    *
    * @param bodyA first body connected to the joint
    * @param bodyB second body connected to the joint (ot {@code null}
    * if the first body is connected to ground
    * @param TDW joint coordinate frame D in world coordinates
    * @param skewAngle skew angle (must lie in the open interval (-PI/2, PI/2).
    */
   public SkewedRollPitchJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      RigidTransform3d TDW, double skewAng) {
      this();
      setSkewAngle (skewAng);
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a SkewedRollPitchJoint between {@code bodyA} and {@code bodyB},
    * with the initial joint pose given by the point {@code pc} and
    * 
    *
    * @param bodyA first body connected to the joint
    * @param bodyB second body connected to the joint (ot {@code null}
    * if the first body is connected to ground
    * @param pc initial center of the joint coordinate frame D (world coordinates)
    * @param rollAxis initial direction of the roll axis (world coordinates)
    * @param pitchAxis initial direction of the pitch axis (world coordinates)
    */
   public SkewedRollPitchJoint (
      ConnectableBody bodyA, ConnectableBody bodyB,
      Vector3d pc, Vector3d rollAxis, Vector3d pitchAxis) {
      this();
      RigidTransform3d TDW = new RigidTransform3d();
      double skewAng = computeTDW (TDW, pc, rollAxis, pitchAxis);
      setSkewAngle (skewAng);
      setBodies(bodyA, bodyB, TDW);
   }

   /**
    * Creates a SkewedRollPitchJoint between {@code bodyA} and {@code bodyB}.
    * The skew angle is defined such that the angle between the roll and pitch
    * axes is given by {@code PI/2 - skewAngle}.
    *
    * @param bodyA first body connected to the joint
    * @param TCA transform from the joint C frame to body A
    * @param bodyB second body connected to the joint (or {@code null}
    * if the first body is connected to ground)
    * @param TDB transform from the joint D frame to body B
    * @param skewAngle skew angle (must lie in the open interval (-PI/2, PI/2).
    */
   public SkewedRollPitchJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d TDB, double skewAngle) {
      this();
      setSkewAngle (skewAngle);
      setBodies (bodyA, TCA, bodyB, TDB);
   }

   private double computeTDW (
      RigidTransform3d TDW, Vector3d pc, Vector3d rollAxis, Vector3d pitchAxis) {

      if (rollAxis.norm() == 0) {
         throw new IllegalArgumentException ("rollAxis has zero length");
      }
      if (pitchAxis.norm() == 0) {
         throw new IllegalArgumentException ("pitchAxis has zero length");
      }
      TDW.p.set (pc);

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

      pw.print ("skewAngle=" + fmt.format(mySkewAngle));
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

