package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.driver.*;
import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class SphericalJointDemo extends RootModel {
   protected MechModel myMechMod;

   private static double inf = Double.POSITIVE_INFINITY;

   protected RigidBody myLowerArm;
   protected RigidBody myHand;

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

   public RigidBody addBody (String bodyName, String meshName)
      throws IOException {
      double density = 1000;

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh =
         new PolygonalMesh (new File (ArtisynthPath.getSrcRelativePath (
            SphericalJointDemo.class, "geometry/" + meshName + ".obj")));
      mesh.scale (0.1);
      mesh.triangulate();
      body.setMesh (mesh, null);
      myMechMod.addRigidBody (body);
      body.setInertiaFromDensity (density);
      return body;
   }

   public SphericalJoint addSphericalJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      SphericalJoint joint = new SphericalJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setPointStyle (joint, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (joint, Color.BLUE);
      RenderProps.setPointRadius (joint, 0.025);
      joint.setAxisLength (0.05);
      myMechMod.addBodyConnector (joint);
      return joint;
   }

   public SphericalRpyJoint addSphericalRpyJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      SphericalRpyJoint joint = new SphericalRpyJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setPointStyle (joint, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (joint, Color.BLUE);
      RenderProps.setPointRadius (joint, 0.025);
      joint.setAxisLength (0.05);
      myMechMod.addBodyConnector (joint);
      return joint;
   }

   private FrameMarker addTipMarker (RigidBody tip) {
      FrameMarker mkr = new FrameMarker ("tipMarker");
      myMechMod.addFrameMarker (
         mkr, tip, new Point3d (0, 0, -getHeight(tip)/2));

      RenderProps.setPointStyle (mkr, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (mkr, Color.BLUE);
      RenderProps.setPointRadius (mkr, 0.01);
      return mkr;
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

      boolean useRpyJoint = true;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-joint")) {
            i++;
            if (i == args.length) {
               System.out.println (
                  "WARNING: option '-joint' need an additional argument");
            }
            else if (args[i].equals ("rpy")) {
               useRpyJoint = true;
            }
            else if (args[i].equals ("regular")) {
               useRpyJoint = false;
            }
            else {
               System.out.println (
                  "WARNING: argument for option '-joint' must be " +
                  "'rpy' or 'regular'");
            }
         }
         else {
            System.out.println ("WARNING: unknown option "+args[i]);
         }
      }
      

      myMechMod = new MechModel ("sphericalJoint");
      myMechMod.setProfiling (false);
      myMechMod.setGravity (0, 0, -9.8);
      myMechMod.setFrameDamping (0.10);
      myMechMod.setRotaryDamping (0.01);
      myMechMod.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

      myLowerArm = addBody ("lowerArm", "lowerArm");
      myHand = addBody ("hand", "hand");

      double lowerArmZ = getHeight (myLowerArm) / 2 + getCenter (myLowerArm).z;
      double handZ = -(getCenter (myHand).z + getHeight (myHand) / 2);

      setBodyPose (myLowerArm, 0, 0, lowerArmZ, 0, 0, 0);
      setBodyPose (myHand, 0, 0, handZ, -90, 0, 0);

      addTipMarker (myHand);

      RigidTransform3d X = new RigidTransform3d();

      myLowerArm.setDynamic (false);

      RigidTransform3d TDW = new RigidTransform3d();

      SphericalRpyJoint rpyJoint = null;
      SphericalJoint regJoint = null;

      if (useRpyJoint) {
         TDW.R.setRpy (0, 0, Math.PI/2);
         rpyJoint = addSphericalRpyJoint (myLowerArm, myHand, TDW);
         rpyJoint.setRollRange (-90, 90);
         rpyJoint.setPitchRange (-45, 45);
         rpyJoint.setYawRange (-90, 90);
         rpyJoint.setRoll (-30);
         rpyJoint.setYaw (-1);
      }
      else {
         regJoint = addSphericalJoint (myHand, myLowerArm, TDW);
         regJoint.setMaxRotation (100);
         regJoint.setMaxTilt (120);
      }

      addModel (myMechMod);

      X.setIdentity();
      X.R.setRpy (0, Math.toRadians(-180), 0);
      myMechMod.transformGeometry (X);

      if (!useRpyJoint) {
         // set hand so that it will fall to the limit
         double l = getHeight(myHand)/2;
         double sin45 = Math.sin(Math.PI/4);
         double cos45 = Math.cos(Math.PI/4);
         myHand.setPose (
            new RigidTransform3d (l*cos45,0,l*sin45, -Math.PI/2,0,3*Math.PI/4));
      }

      ControlPanel panel = new ControlPanel();
      panel.addWidget (myMechMod.getProperty("integrator"));
      if (useRpyJoint) {
         panel.addWidget (rpyJoint.getProperty("roll"));
         panel.addWidget (rpyJoint.getProperty("pitch"));
         panel.addWidget (rpyJoint.getProperty("yaw"));
         panel.addWidget (rpyJoint.getProperty("rollRange"));
         panel.addWidget (rpyJoint.getProperty("pitchRange"));
         panel.addWidget (rpyJoint.getProperty("yawRange"));
      }
      else {
         panel.addWidget (regJoint.getProperty("maxRotation"));
         panel.addWidget (regJoint.getProperty("rotationLimited"));
         panel.addWidget (regJoint.getProperty("maxTilt"));
         panel.addWidget (regJoint.getProperty("tiltLimited"));
      }
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);

   }
}
