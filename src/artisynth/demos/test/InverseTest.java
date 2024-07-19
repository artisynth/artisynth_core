package artisynth.demos.test;
//import ilog.cplex.IloCplex;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import artisynth.core.gui.ControlPanel;
import artisynth.core.inverse.ConnectorForceRenderer;
import artisynth.core.inverse.TrackingController;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.util.DoubleInterval;
import maspack.util.PathFinder;

/**
 * @author Alexander Denk Copyright (c) 2024, by the Author: Alexander Denk
 * (UDE) University of Duisburg-Essen Chair of Mechanics and Robotics
 * alexander.denk@uni-due.de
 */
public class InverseTest extends RootModel {
   MechModel mech = new MechModel ();
   RigidBody thigh;
   RigidBody shank;
   RigidBody foot;
   Muscle muscle1;
   Muscle muscle2;
   Muscle muscle3;
   Muscle muscle4;
   Muscle muscle5;
   FrameMarker mkr1;
   FrameMarker mkr2;
   FrameMarker mkr3;
   FrameMarker mkr4;
   FrameMarker hipMkr;
   PlanarConnector connector;

   public void build (String[] args) throws IOException {
      addModel (mech);
      mech.setName ("InverseTest");
      mech.setGravity (new Vector3d (0, 0, -9.81));
      mech.setFrameDamping (0.01);
      mech.setRotaryDamping (0.2);
      PolygonalMesh mesh = null;
      // add bodies
      addBodies (mesh);
      // add joints
      addJoints ();
      // add Muscles
      addMuscles ();
      // add the tracking controller
      addTrackingController ();
      // create an output probe
      //createProbes ();
      // set render properties
      setRenderProps ();
   }

   public void addBodies (PolygonalMesh mesh) {
      double density = 2000.0;
      // create foot
      foot = RigidBody.createBox ("foot", 0.3, 0.15, 0.075, density);
      // adjust the foot to lie on the ground
      RigidTransform3d TFW = new RigidTransform3d (0.15, 0.0, 0.03875);
      foot.setPose (TFW);
      // foot.setDynamic (false);
      mech.addRigidBody (foot);
      // create shank from mesh
      double lenThigh = 0.5;
      double radThigh = 0.05;
      double lenShnk = lenThigh * 0.8;
      double radShnk = radThigh * 0.8;
      mesh = MeshFactory.createRoundedCylinder (radShnk, lenShnk, 16, 1, false);
      shank = RigidBody.createFromMesh ("shank", mesh, density, 1.0);
      // coordinate transformation from shank to world
      RigidTransform3d TSW = new RigidTransform3d (0, 0, lenShnk / 2 + radShnk);
      shank.setPose (TSW);
      mech.addRigidBody (shank);
   }

   public void addJoints () {
      // create ankle hinge joint
      Vector3d yAxis = new Vector3d (0, 1, 0);
      Point3d ankleOrigin = new Point3d (0, 0, foot.getPose ().p.z);
      HingeJoint ankle = new HingeJoint (shank, foot, ankleOrigin, yAxis);
      ankle.setName ("ankle");
      ankle.setCoordinateRangeDeg (0, new DoubleInterval (-90.0, 45.0));
      mech.addBodyConnector (ankle);
      // Create control panel for the joint angles
      ControlPanel panel = new ControlPanel ("Joint Coordinates");
      panel.addWidget (ankle, "theta");
      // create planar connector
      // Transformation with respect to world coordinates
      RigidTransform3d TDW = new RigidTransform3d (0, 0, 0);
      /*
       * pCA describes the coordinates of the connection point with respect to
       * the thigh. In this case, the FrameMarker describes the outermost point
       * of the thigh, that should be connected to the planar connector.
       * However, we are describing this point in local coordinates, so as a
       * z-offset from the thighs cog. So, that is half of its lenght + the
       * radius of the cylinder, 0.25 + 0.05
       */
      Vector3d pCA = new Vector3d (0, 0, -0.075);
      connector = new PlanarConnector (foot, pCA, TDW);
      connector.setPlaneSize (1);
      connector.setName ("ground connector");
      connector.setUnilateral (false);
      mech.addBodyConnector (connector);
      addControlPanel (panel);
   }

   public void addMotionTargets (TrackingController controller) {
      List<FrameMarker> sources = new ArrayList<FrameMarker> ();
      // Foot
      //FrameMarker src1 = new FrameMarker (0.15, 0.075, 0.03875);
      //src1.setFrame (foot);
      //sources.add (src1);
      FrameMarker src2 = new FrameMarker (0.15, 0.075, -0.03875);
      src2.setFrame (foot);
      sources.add (src2);
      //FrameMarker src3 = new FrameMarker (0.15, -0.075, 0.03875);
      //src3.setFrame (foot);
      //sources.add (src3);
      FrameMarker src4 = new FrameMarker (0.15, -0.075, -0.03875);
      src4.setFrame (foot);
      sources.add (src4);
      FrameMarker src5 = new FrameMarker (-0.15, 0.075, -0.03875);
      src5.setFrame (foot);
      sources.add (src5);
      FrameMarker src6 = new FrameMarker (-0.15, -0.075, -0.03875);
      src6.setFrame (foot);
      sources.add (src6);

      sources.forEach (s -> {
         mech.addFrameMarker (s);
         controller.addPointTarget (s);
      });

      // Shank
      controller.addPointTarget (mkr2);
      controller.addPointTarget (mkr3);
   }

   public void addMuscles () {
      // Foot marker
      mkr1 = new FrameMarker (0.15, 0, 0.03875);
      mkr1.setFrame (foot);
      mkr1.setName ("Foot Marker");
      mech.addFrameMarker (mkr1);
      // Shank marker
      mkr2 = new FrameMarker (0.04, 0, 0.1);
      mkr2.setFrame (shank);
      mkr2.setName ("Front Shank Marker");
      mech.addFrameMarker (mkr2);
      mkr3 = new FrameMarker (-0.04, 0, 0.1);
      mkr3.setFrame (shank);
      mkr3.setName ("Rear Shank Marker");
      mech.addFrameMarker (mkr3);
      // Define ankle flexor muscle
      muscle1 = new Muscle ("Ankle flexor", 0.5);
      muscle1.setPoints (mkr1, mkr2);
      // muscle1.setMaterial (new Thelen2003AxialMuscle ());
      muscle1.setMaterial (new SimpleAxialMuscle (30, 1, 150));
      mech.addAxialSpring (muscle1);
   }

   public void addTrackingController () throws IOException {
      TrackingController controller =
         new TrackingController (mech, "Motion controller");
      // controller.setL2Regularization ();
      controller.setDebug (true);
      addMotionTargets (controller);
      mech.axialSprings ().forEach (s -> {
         if (s.getClass ().equals (Muscle.class)) {
            controller.addExciter ((Muscle)s);
         }
      });
      
      //VectorNd tarlambda = new VectorNd (1);
      //ForceTargetTerm fTerm = controller.addForceTargetTerm ();
      //ForceTarget ft = fTerm.addForceTarget (connector);
      //ft.setTargetLambda (tarlambda);
      //ft.setArrowSize (0.1);
      controller.setProbeDuration (3.0);
      controller.createProbesAndPanel (this);
      addController (controller);

      reloadMotionProbes ();
      //reloadForceProbes ();
   }

   public void createProbes () throws IOException {
      // create input probes for external forces acting on the bottom marker
      String filename =
         PathFinder
            .getSourceRelativePath (
               this, "input/InverseTest_external_forces.txt");
      NumericInputProbe extForce =
         new NumericInputProbe (hipMkr, "externalForce", filename);
      extForce.setName ("external force");
      addInputProbe (extForce);
      // create output probe for unilateral constraint force
      double step = getMaxStepSize ();
      filename =
         PathFinder
            .getSourceRelativePath (
               this, "output/InverseTest_constraint_forces.txt");
      NumericOutputProbe uniForce =
         new NumericOutputProbe (
            connector, "unilateralForceInA", filename, step);
      uniForce.setName ("Unilateral constraint forces");
      uniForce.setStartStopTimes (0.0, 3.0);
      addOutputProbe (uniForce);
   }

   public void reloadForceProbes () throws IOException {
      if (getInputProbes ().get ("target forces") != null) {
         NumericInputProbe extForce =
            (NumericInputProbe)getInputProbes ().get ("target forces");
         String filename =
            PathFinder
               .getSourceRelativePath (
                  this, "input/InverseTest_target_forces.txt");
         extForce.setAttachedFileName (filename);
         extForce.load ();
         extForce.setActive (true);
      }
   }

   public void reloadMotionProbes () throws IOException {
      if (getInputProbes ().get ("target positions") != null) {
         NumericInputProbe motTarget =
            (NumericInputProbe)getInputProbes ().get ("target positions");
         String filename =
            PathFinder
               .getSourceRelativePath (
                  this, "input/InverseTest_target_positions.txt");
         motTarget.setAttachedFileName (filename);
         motTarget.load ();
         motTarget.setActive (true);
      }
   }

   public void setRenderProps () {
      // Frame Marker
      mech.frameMarkers ().forEach (m -> {
         RenderProps.setPointStyle (m, Renderer.PointStyle.SPHERE);
         RenderProps.setPointRadius (m, 0.01);
         RenderProps.setPointColor (m, Color.BLUE);
      });
      // Muscles
      if (mech.axialSprings () != null) {
         mech.axialSprings ().forEach (s -> {
            RenderProps.setLineColor (s, Color.RED);
            RenderProps.setLineStyle (s, LineStyle.SPINDLE);
            RenderProps.setLineRadius (s, 0.01);
            ((Muscle)s).setExcitationColor (Color.GREEN);
         });
         mech.setMaxColoredExcitation (1.0);
         // Point
         mech.points ().forEach (p -> {
            RenderProps.setPointStyle (p, Renderer.PointStyle.SPHERE);
            RenderProps.setPointColor (p, Color.BLUE);
            RenderProps.setPointRadius (p, 0.01);
         });
      }
      // Add a renderer for the connector force
      if (connector != null) {
         ConnectorForceRenderer forceRend =
            new ConnectorForceRenderer (connector);
         RenderProps props = forceRend.createRenderProps ();
         props.setLineStyle (LineStyle.CYLINDER);
         props.setLineRadius (0.025);
         props.setLineColor (Color.GREEN);
         forceRend.setRenderProps (props);
         forceRend.setArrowSize (1E-5);
         addMonitor (forceRend);
      }
      TrackingController con =
         (TrackingController)getControllers ().get ("Motion controller");
      if (con != null) {
         con.getMotionTargets ().forEach (t -> {
               RenderProps.setPointRadius ((Renderable)t, 0.01);
            });
         con.getMotionSources ().forEach (s -> {
               RenderProps.setPointRadius ((Renderable)s, 0.01);
            });
      }
      // set default view orientation
      setDefaultViewOrientation (AxisAlignedRotation.X_Z);
   }

   public InverseTest () {

   }

   public InverseTest (String name) throws IOException {
      super (name);
   }

   public static void main (String[] args) throws IOException {

   }
}
