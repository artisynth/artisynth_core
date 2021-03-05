package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.RotAxisFrameMaterial;
import artisynth.core.util.*;
import artisynth.core.gui.*;
import artisynth.core.driver.Main;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.LineStyle;
import maspack.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class LaymanModel extends MechModel {
   private static double ARM_SEP = 0.42;
   private static double LEG_SEP = 0.2;
   private static double inf = Double.POSITIVE_INFINITY;

   private static double DTOR = Math.PI / 180.0;
   private static double HEAD_Z = 0.1;
   private static double NECK_Z = -0.025;

   private static double WAIST_Z = -0.5;
   private static double TORSO_Z = -.25;
   private static double PELVIS_Z = -.6;

   private static double UPPER_ARM_Z = -.275;
   private static double SHOULDER_Z = -.125;
   private static double UPPER_LEG_Z = -.85;

   private static double FOOT_Y = -0.05;
   private static double PI = Math.PI;

   private static double K_ROT = 0.1;

   public static PropertyList myProps =
      new PropertyList (LaymanModel.class, MechModel.class);

   static {
      myProps.add ("friction", "friction coefficient", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private double myMu = 0.0;

   public double getFriction() {
      return myMu;
   }

   public void setFriction (double mu) {
      super.setFriction (mu);
      myMu = mu;
   }

   RigidBody myHead;
   RigidBody myTorso;
   RigidBody myPelvis;
   RigidBody myLUppArm;
   RigidBody myRUppArm;
   RigidBody myLLowArm;
   RigidBody myRLowArm;
   RigidBody myLHand;
   RigidBody myRHand;
   RigidBody myBin;
   RigidBody myLUppLeg;
   RigidBody myRUppLeg;
   RigidBody myLLowLeg;
   RigidBody myRLowLeg;
   RigidBody myLFoot;
   RigidBody myRFoot;

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

   // private void addContact (RigidBody bodyA, RigidBody bodyB, double mu)
   // {
   // myMechMod.addRigidBodyContact(new RigidBodyContact(bodyA, bodyB), mu);
   // }

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
      attachFrameSpring (bodyA, bodyB, spring);
   }

   public RigidBody addBody (String bodyName, String meshName)
      throws IOException {
      RigidBody body = RigidBody.createFromMesh (
         bodyName, LaymanDemo.class, "geometry/"+meshName+".obj", 1000, 0.1);
      addRigidBody (body);
      return body;
   }

   public SphericalJoint addSphericalJoint (
      RigidBody bodyA, RigidBody bodyB, double x, double y, double z,
      double maxAng) {
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (x, y, z);

      return addSphericalJoint (bodyA, bodyB, TDW, maxAng);
   }

   public SphericalJoint addSphericalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW, double maxAng) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      SphericalJoint joint = new SphericalJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setJointRadius (0.035);
      addBodyConnector (joint);
      if (maxAng < 180) {
         joint.setMaxRotation (maxAng);
      }
      return joint;
   }

   public HingeJoint addHingeJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      HingeJoint joint = new HingeJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setFaceColor (joint, Color.BLUE);
      joint.setShaftRadius (0.025);
      joint.setShaftLength (0.05);
      addBodyConnector (joint);
      return joint;
   }

   public HingeJoint addHingeJoint (
      RigidBody bodyA, RigidBody bodyB, double x, double y, double z) {
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.set (x, y, z);
      TDW.R.setAxisAngle (Vector3d.Y_UNIT, Math.toRadians (90));
      return addHingeJoint (bodyA, bodyB, TDW);
   }
   
   public LaymanModel() {
       super();
   }

   public LaymanModel (String name) throws IOException {
      super (name);

      setProfiling (false);
      setGravity (0, 0, -9.8);
      // setRigidBodyDamper (new FrameDamper (1.0, 4.0));
      setFrameDamping (0.50);
      setRotaryDamping (0.01);
      setIntegrator (MechSystemSolver.Integrator.BackwardEuler);
      // AxialSpring.myIgnoreCoriolisInJacobian = false;

      myHead = addBody ("head", "head");
      myHead.setPose (0, 0, HEAD_Z, 0, 0, 0);
      myTorso = addBody ("torso", "torso");
      myTorso.setPose (0, 0, TORSO_Z, 0, 0, 0);
      myPelvis = addBody ("pelvis", "pelvis");
      myPelvis.setPose (0, 0, PELVIS_Z, 0, 0, 0);

      myRUppArm = addBody ("rightUppArm", "upperArm");
      myLUppArm = addBody ("leftUppArm", "upperArm");
      myRLowArm = addBody ("rightLowArm", "lowerArm");
      myLLowArm = addBody ("leftLowArm", "lowerArm");
      myRHand = addBody ("rightHand", "hand");
      myLHand = addBody ("leftHand", "hand");

      double elbowZ =
         UPPER_ARM_Z + getCenter (myRUppArm).z - getHeight (myRUppArm) / 2;
      double wristZ = elbowZ - getHeight (myRLowArm);

      double lowerArmZ =
         elbowZ - getCenter (myRLowArm).z - getHeight (myRLowArm) / 2;
      double handZ = wristZ - getCenter (myRHand).z - getHeight (myRHand) / 2;

      myRUppArm.setPose (-ARM_SEP / 2, 0, UPPER_ARM_Z, 0, 0, 0);
      myLUppArm.setPose (ARM_SEP / 2, 0, UPPER_ARM_Z, 0, 0, 0);
      myRLowArm.setPose (-ARM_SEP / 2, 0, lowerArmZ, 0, 0, 0);
      myLLowArm.setPose (ARM_SEP / 2, 0, lowerArmZ, 0, 0, 0);
      myRHand.setPose (-ARM_SEP / 2, 0, handZ, 0, 0, 0);
      myLHand.setPose (ARM_SEP / 2, 0, handZ, 0, 0, 0);

      RigidTransform3d TDW = new RigidTransform3d();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d TCW = new RigidTransform3d();

      SphericalJoint sjoint;
      HingeJoint rjoint;

      TDW.p.set (-ARM_SEP / 2, 0, SHOULDER_Z);
      TDW.R.setRpy (-DTOR * 45, 0, DTOR * 80);
      sjoint = addSphericalJoint (myRUppArm, myTorso, TDW, 105);
      // override TCA so that we have an initial displacement
      TCA.p.set (0, 0, SHOULDER_Z - UPPER_ARM_Z);
      TCA.R.setRpy (0, 0, PI);
      TCW.mul (myRUppArm.getPose(), TCA);
      sjoint.setCurrentTCW (TCW);

      TDW.p.set (ARM_SEP / 2, 0, SHOULDER_Z);
      TDW.R.setRpy (DTOR * 45, 0, DTOR * 80);
      sjoint = addSphericalJoint (myLUppArm, myTorso, TDW, 105);
      TCW.mul (myLUppArm.getPose(), TCA);
      sjoint.setCurrentTCW (TCW);

      rjoint = addHingeJoint (myRUppArm, myRLowArm, -ARM_SEP / 2, 0, elbowZ);
      rjoint.setMinTheta (-5);
      rjoint.setMaxTheta (160);
      rjoint = addHingeJoint (myLUppArm, myLLowArm, ARM_SEP / 2, 0, elbowZ);
      rjoint.setMinTheta (-5);
      rjoint.setMaxTheta (160);

      addSphericalJoint (myRHand, myRLowArm, -ARM_SEP / 2, 0, wristZ, 45);
      addSphericalJoint (myLHand, myLLowArm, ARM_SEP / 2, 0, wristZ, 45);
      addFrameSpring (myRLowArm, myRHand, -ARM_SEP / 2, 0, wristZ, K_ROT/2);
      addFrameSpring (myLLowArm, myLHand, ARM_SEP / 2, 0, wristZ, K_ROT/2);

      myRUppLeg = addBody ("rightUppLeg", "upperLeg");
      myLUppLeg = addBody ("leftUppLeg", "upperLeg");
      myRLowLeg = addBody ("rightLowLeg", "lowerLeg");
      myLLowLeg = addBody ("leftLowLeg", "lowerLeg");
      myRFoot = addBody ("rightFoot", "foot");
      myLFoot = addBody ("leftFoot", "foot");

      double kneeZ =
         UPPER_LEG_Z + getCenter (myRUppLeg).z - getHeight (myRUppLeg) / 2;
      double ankleZ = kneeZ - getHeight (myRLowLeg);
      double hipZ = kneeZ + getHeight (myRUppLeg);

      double lowerLegZ =
         kneeZ - getCenter (myRLowLeg).z - getHeight (myRLowLeg) / 2;
      double footZ = ankleZ - getCenter (myRFoot).z - getHeight (myRFoot) / 2;

      myRUppLeg.setPose (-LEG_SEP / 2, 0, UPPER_LEG_Z, 0, 0, 0);
      myLUppLeg.setPose (LEG_SEP / 2, 0, UPPER_LEG_Z, 0, 0, 0);
      myRLowLeg.setPose (-LEG_SEP / 2, 0, lowerLegZ, 0, 0, 0);
      myLLowLeg.setPose (LEG_SEP / 2, 0, lowerLegZ, 0, 0, 0);
      myRFoot.setPose (-LEG_SEP / 2, FOOT_Y, footZ, 0, 0, 0);
      myLFoot.setPose (LEG_SEP / 2, FOOT_Y, footZ, 0, 0, 0);

      TDW.p.set (-LEG_SEP / 2, 0, hipZ);
      // TDW.R.setRpy (0, 0, PI);
      sjoint = addSphericalJoint (myRUppLeg, myPelvis, TDW, 180);
      sjoint.setMaxRotation (90, 45, 45);
      TDW.p.set (LEG_SEP / 2, 0, hipZ);
      // TDW.R.setRpy (0, 0, PI);
      sjoint = addSphericalJoint (myLUppLeg, myPelvis, TDW, 180);
      sjoint.setMaxRotation (90, 45, 45);

      rjoint = addHingeJoint (myRUppLeg, myRLowLeg, -LEG_SEP / 2, 0, kneeZ);
      rjoint.setMinTheta (-135);
      rjoint.setMaxTheta (5);
      rjoint = addHingeJoint (myLUppLeg, myLLowLeg, LEG_SEP / 2, 0, kneeZ);
      rjoint.setMinTheta (-135);
      rjoint.setMaxTheta (5);

      addSphericalJoint (myRFoot, myRLowLeg, -LEG_SEP / 2, 0, ankleZ, 45);
      addSphericalJoint (myLFoot, myLLowLeg, LEG_SEP / 2, 0, ankleZ, 45);
      addFrameSpring (myRLowLeg, myRFoot, -LEG_SEP / 2, 0, ankleZ, K_ROT);
      addFrameSpring (myLLowLeg, myLFoot, LEG_SEP / 2, 0, ankleZ, K_ROT);

      addSphericalJoint (myHead, myTorso, 0, 0, NECK_Z, 45);
      addSphericalJoint (myPelvis, myTorso, 0, 0, WAIST_Z, 30);

      //      HashSet<BodyPair> collisionPairs = new HashSet<BodyPair>();

      setFriction (0.2);
      setDefaultCollisionBehavior (true, getFriction());

      setCollisionBehavior (myTorso, myHead, false);
      setCollisionBehavior (myTorso, myRUppArm, false);
      setCollisionBehavior (myTorso, myLUppArm, false);
      setCollisionBehavior (myTorso, myPelvis, false);

      setCollisionBehavior (myRUppArm, myRLowArm, false);
      setCollisionBehavior (myRLowArm, myRHand, false);
      setCollisionBehavior (myLUppArm, myLLowArm, false);
      setCollisionBehavior (myLLowArm, myLHand, false);

      setCollisionBehavior (myPelvis, myRUppLeg, false);
      setCollisionBehavior (myPelvis, myLUppLeg, false);

      setCollisionBehavior (myRUppLeg, myRLowLeg, false);
      setCollisionBehavior (myRLowLeg, myRFoot, false);
      setCollisionBehavior (myLUppLeg, myLLowLeg, false);
      setCollisionBehavior (myLLowLeg, myLFoot, false);

      CollisionManager cm = getCollisionManager();
      cm.setRigidPointTol (1e-2);
      cm.setRigidRegionTol (1e-1);
      setPenetrationTol (1e-3);

      RenderProps r = new RenderProps();
      r.setFaceColor (new Color (0.8f, 0.8f, 1f));
      r.setPointStyle (Renderer.PointStyle.SPHERE);
      r.setPointColor (new Color (88f / 255f, 106f / 255f, 155f / 255f));
      r.setLineColor (new Color (0.6f, 1f, 0.6f));
      r.setLineStyle (LineStyle.CYLINDER);
      r.setLineRadius (0.02);
      setRenderProps (r);
   }

}
