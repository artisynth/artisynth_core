package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.RotAxisFrameMaterial;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class FrameSpringDemo extends RootModel {
   MechModel myMechMod;

   private static double inf = Double.POSITIVE_INFINITY;

   RigidBody myLowerArm;

   RigidBody myHand;

   RigidBody myHand2;

   private double getHeight (RigidBody body) {
      Point3d min = new Point3d (inf, inf, inf);
      Point3d max = new Point3d (-inf, -inf, -inf);

      body.updateBounds (min, max);
      return max.z - min.z;
   }

   private Point3d getCenter (RigidBody body) {
      Point3d min = new Point3d (inf, inf, inf);
      Point3d max = new Point3d (-inf, -inf, -inf);

      body.updateBounds (min, max);
      Point3d center = new Point3d();
      center.add (min, max);
      center.scale (0.5);
      return center;
   }

   public void addFrameSpring (
      RigidBody bodyA, RigidBody bodyB, double x, double y, double z,
      double kRot) {
      RigidTransform3d TDW = new RigidTransform3d();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      TDW.p.set (x, y, z);

      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      XDB.mulInverseLeft (bodyB.getPose(), TDW);

      FrameSpring spring = new FrameSpring (null);
      spring.setMaterial (new RotAxisFrameMaterial (0, kRot, 0, 0));
      //spring.setRotaryStiffness (kRot);
      spring.setAttachFrameA (TCA);
      spring.setAttachFrameB (XDB);
      myMechMod.attachFrameSpring (bodyA, bodyB, spring);
   }

   public RigidBody addBody (String bodyName, String meshName)
      throws IOException {
      double density = 1000;

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh =
         new PolygonalMesh (new File (ArtisynthPath.getSrcRelativePath (
            FrameSpringDemo.class, "geometry/" + meshName + ".obj")));
      mesh.scale (0.1);
      mesh.triangulate();
      body.setMesh (mesh, null);
      myMechMod.addRigidBody (body);

      body.setInertiaFromDensity (density);
      return body;
   }

   public HingeJoint addHingeJoint (
      RigidBody bodyA, RigidBody bodyB, double x, double y, double z) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      RigidTransform3d TDW = new RigidTransform3d();

      TDW.p.set (x, y, z);
      TDW.R.setAxisAngle (Vector3d.Y_UNIT, Math.toRadians (90));
      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      HingeJoint joint = new HingeJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftRadius (0.025);
      joint.setShaftLength (0.05);
      myMechMod.addBodyConnector (joint);
      return joint;
   }

   public SphericalJoint addSphericalJoint (
      RigidBody bodyA, RigidBody bodyB, double x, double y, double z) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      RigidTransform3d TDW = new RigidTransform3d();

      TDW.p.set (x, y, z);
      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      SphericalJoint joint = new SphericalJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setJointRadius (0.02);
      myMechMod.addBodyConnector (joint);
      return joint;
   }

   public SphericalJoint addSphericalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      SphericalJoint joint = new SphericalJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setJointRadius (0.02);
      myMechMod.addBodyConnector (joint);
      return joint;
   }

   private void setBodyPose (
      RigidBody body, double x, double y, double z, double roll, double pitch,
      double yaw) {
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (x, y, z);
      X.R.setRpy (Math.toRadians (roll),
                  Math.toRadians (pitch),
                  Math.toRadians (yaw));
      body.setPose (X);
   }

   public void build (String[] args) throws IOException {

      myMechMod = new MechModel ("frameSpring");
      myMechMod.setProfiling (false);
      myMechMod.setGravity (0, 0, -9.8);
      // myMechMod.setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      myMechMod.setFrameDamping (0.10);
      myMechMod.setRotaryDamping (0.01);
      //myMechMod.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

      myLowerArm = addBody ("lowerArm", "lowerArm");
      myHand = addBody ("hand", "hand");
      myHand2 = addBody ("hand2", "hand");

      double lowerArmZ = getHeight (myLowerArm) / 2 + getCenter (myLowerArm).z;
      double handZ = -(getCenter (myHand).z + getHeight (myHand) / 2);

      setBodyPose (myLowerArm, 0, 0, lowerArmZ, 0, 0, 0);
      setBodyPose (myHand, 0, 0, handZ, -90, 0, 0);
      setBodyPose (myHand2, 0, 0, handZ - getHeight (myHand), -90, 0, 0);

      RigidTransform3d X = new RigidTransform3d();

      RigidTransform3d TDW = new RigidTransform3d();
      // TDW.R.setAxisAngle (Vector3d.Y_UNIT, Math.PI/2);

      SphericalJoint joint = addSphericalJoint (myHand, myLowerArm, TDW);

      joint.setMaxRotation (100);
      TDW.p.set (0, 0, -getHeight (myHand));
      joint = addSphericalJoint (myHand2, myHand, TDW);
      joint.setMaxRotation (45);

      // HingeJoint joint = addHingeJoint (myLowerArm, myHand, 0, 0, 0);
      // joint.setMaximumTheta (Math.toRadians(30));
      // joint.setMinimumTheta (-Math.toRadians(30));

      addFrameSpring (myLowerArm, myHand, 0, 0, 0, 0.1);

      myLowerArm.setDynamic (false);

      X.R.setAxisAngle (Vector3d.X_UNIT, -Math.toRadians (140.0));
      // X.R.setAxisAngle (Vector3d.Y_UNIT, -Math.PI/2);
      // myMechMod.transformGeometry (X);

      addModel (myMechMod);
      //addMonitor (new ArmMover(myLowerArm));

      X.setIdentity();
      X.R.setRpy (0, Math.toRadians(-60), 0);
      myMechMod.transformGeometry (X);
   }

   // public StepAdjustment advance (double t0, double t1, int flags) {
   //    StepAdjustment adj = super.advance (t0, t1, flags);

   //    SolveMatrixTest tester = new SolveMatrixTest();
   //    tester.setYPRStiffness (true);
   //    System.out.println (
   //       "error=" + tester.testStiffness (myMechMod, 1e-8, "%12.3f"));

   //    return adj;
   // } 
   
}
