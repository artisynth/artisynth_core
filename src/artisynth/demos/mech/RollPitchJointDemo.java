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

public class RollPitchJointDemo extends RootModel {
   protected MechModel myMechMod;

   private static double inf = Double.POSITIVE_INFINITY;

   protected RigidBody myBase;
   protected RigidBody myTip;

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
            RollPitchJointDemo.class, "geometry/" + meshName + ".obj")));
      mesh.scale (0.1);
      mesh.triangulate();
      body.setMesh (mesh, null);
      myMechMod.addRigidBody (body);

      body.setInertiaFromDensity (density);
      return body;
   }

   public RigidBody addTip (String bodyName, double len, double r)
      throws IOException {

      double density = 1000;
      int nslices = 12;

      PolygonalMesh mesh =
         MeshFactory.createRoundedCylinder (
            r, len, nslices, /*nsegs=*/1, /*flatBottom=*/false);
      RigidBody body = RigidBody.createFromMesh (
         bodyName, mesh, density, /*scale=*/1.0);
      myMechMod.addRigidBody (body);
      return body;
   }

   public RollPitchJoint addRollPitchJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();

      XDB.mulInverseLeft (bodyB.getPose(), TDW);
      TCA.mulInverseLeft (bodyA.getPose(), TDW);
      RollPitchJoint joint = new RollPitchJoint (bodyA, TCA, bodyB, XDB);
      RenderProps.setPointStyle (joint, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (joint, Color.BLUE);
      RenderProps.setPointRadius (joint, 0.025);
      joint.setAxisLength (0.05);
      myMechMod.addBodyConnector (joint);
      return joint;
   }

   public SphericalRpyJoint addSphericalJoint (
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

   private FrameMarker addTipMarker (RigidBody tip) {
      FrameMarker mkr = new FrameMarker ("tipMarker");
      myMechMod.addFrameMarker (
         mkr, tip, new Point3d (0, 0, -getHeight(tip)/2));

      RenderProps.setPointStyle (mkr, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (mkr, Color.BLUE);
      RenderProps.setPointRadius (mkr, 0.01);
      return mkr;
   }

   public void build (String[] args) throws IOException {

      myMechMod = new MechModel ("rollPitchJointDemo");
      myMechMod.setProfiling (false);
      myMechMod.setGravity (0, 0, -9.8);
      myMechMod.setFrameDamping (0.10);
      myMechMod.setRotaryDamping (0.001);
      myMechMod.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler);

      myBase = addBody ("base", "lowerArm");
      //myTip = addBody ("tip", "hand");
      myTip = addTip ("tip", 0.1, 0.02);

      double baseZ = getHeight (myBase) / 2 + getCenter (myBase).z;
      double tipZ = -(getCenter (myTip).z + getHeight (myTip) / 2);

      setBodyPose (myBase, 0, 0, baseZ, 0, 0, 0);
      setBodyPose (myTip, 0, 0, tipZ, -90, 0, 0);

      addTipMarker (myTip);

      RigidTransform3d X = new RigidTransform3d();

      myBase.setDynamic (false);

      // RollPitchJoint joint = addRollPitchJoint (myTip, myBase, TDW);
      // joint.setMaxRotation (100);
      // TDW.p.set (0, 0, -getHeight (myTip));
      // joint = addRollPitchJoint (myTip2, myTip, TDW);
      // joint.setMaxRotation (45);

      RigidTransform3d TDW = new RigidTransform3d();
      //TDW.p.set (0, 0, -getHeight (myTip));
      TDW.R.setRpy (0, Math.PI, 0);
      RollPitchJoint joint = addRollPitchJoint (myBase, myTip, TDW);
      // SphericalRpyJoint joint = addSphericalJoint (myBase, myTip, TDW);
      joint.setRollRange (-90, 90);
      joint.setPitchRange (-100, 100);
      joint.setRoll (0);
      joint.setPitch (45);

      addModel (myMechMod);

      X.setIdentity();
      X.R.setRpy (0, Math.toRadians(-180), 0);
      myMechMod.transformGeometry (X);

      ControlPanel panel = new ControlPanel();
      panel.addWidget (myMechMod.getProperty("integrator"));
      panel.addWidget (joint.getProperty("roll"));
      panel.addWidget (joint.getProperty("pitch"));
      panel.addWidget (joint.getProperty("rollRange"));
      panel.addWidget (joint.getProperty("pitchRange"));
      panel.pack();
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);

   }
}
