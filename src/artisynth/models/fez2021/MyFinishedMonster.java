package artisynth.models.fez2021;

import java.awt.Color;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.util.PathFinder;

public class MyFinishedMonster extends RootModel {

   String geodir = PathFinder.getSourceRelativePath (this, "geometry/");

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // add a simple plate-shaped rigid body
      RigidBody plate = RigidBody.createBox (
         "plate", 1.0, 1.0, 0.1, /*density=*/1000.0);
      plate.setPose (new RigidTransform3d (0, 0, 0.2));
      mech.addRigidBody (plate);
      plate.setDynamic (false);

      // add a rigid body link defined by a mesh
      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         0.6, 0.2, 0.2, /*nslices=*/20);
      RigidBody link = RigidBody.createFromMesh (
         "link", mesh, /*density=*/1000.0, /*scale=*/1.0);
      mech.addRigidBody (link);

      // adjust the pose of the link
      link.setPose (new RigidTransform3d (0.3, 0, 0, 0, Math.PI/2, 0));

      // add another fixed rigid body defined by an external mesh
      try {
         mesh = new PolygonalMesh (geodir+"flange.obj");
      }
      catch (Exception e) {
      }
      RigidBody flange = RigidBody.createFromMesh ("flange", mesh, 1000.0, 1.0);
      flange.setDynamic (false);
      mech.addRigidBody (flange);
      
      // add a HingeJoint between the link and flange
      HingeJoint hinge = new HingeJoint (
         link, flange, new Point3d(0, 0, 0), new Vector3d (0, 1, 0));
      mech.addBodyConnector (hinge);
      // set render properties for the hinge
      hinge.setShaftLength (0.4);
      RenderProps.setFaceColor (hinge, Color.BLUE);

      // add some translational and rotational damping to the MechModel 
      mech.setFrameDamping (10.0);
      mech.setRotaryDamping (1.0);

      // rotate the link about the hinge
      hinge.setTheta (90.0);

      // draw points as small white spheres so we can see them
      RenderProps.setSphericalPoints (mech, 0.02, Color.WHITE);

      // add some FrameMarkers to the link and plate
      FrameMarker l0 = mech.addFrameMarkerWorld (
         link, new Point3d(-0.1, 0, -0.60));
      FrameMarker l1 = mech.addFrameMarkerWorld (
         plate, new Point3d(-0.48, 0, 0.15));
      FrameMarker r0 = mech.addFrameMarkerWorld (
         link, new Point3d(0.1, 0, -0.60));
      FrameMarker r1 = mech.addFrameMarkerWorld (
         plate, new Point3d(0.48, 0, 0.15));

      // add point-to-point muscles between the markers
      Muscle muscleL = new Muscle ();
      muscleL.setMaterial (new SimpleAxialMuscle (500, 0, 50));
      mech.attachAxialSpring (l0, l1, muscleL);

      Muscle muscleR = new Muscle ();
      muscleR.setMaterial (new SimpleAxialMuscle (450, 0, 50));
      mech.attachAxialSpring (r0, r1, muscleR);
      // render muscles as red spindles
      RenderProps.setSpindleLines (mech, 0.02, Color.RED);

      // add a control panel for muscle excitation
      ControlPanel panel = new ControlPanel ();
      panel.addWidget ("left excitation", muscleL, "excitation");
      panel.addWidget ("right excitation", muscleR, "excitation");
      addControlPanel (panel);

      // add an input probe to control the left excitation
      NumericInputProbe eprobe =
         new NumericInputProbe (muscleL, "excitation", 0, 10.0);
      eprobe.addData (
         new double[] { 0, 0,
                        1, 1,
                        3, 0,
                        5, 1,
                        6, 0,
                        7, 1,
                        8, 0,
                        9, 1,
                        10, 0},
             NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (eprobe);

      // add a controller to control the excitation instead
      ControllerBase controller = new MuscleController (muscleR);
      controller.setActive (false);
      addController (controller);
   }

   class MuscleController extends ControllerBase {
      Muscle myMuscle;
      
      MuscleController (Muscle muscle) {
         myMuscle = muscle;
      }
      
      public void apply (double t0, double t1) {
         double period = 2.0;
         double e = 0.5*(1-Math.cos (t0*2*Math.PI/period));
         myMuscle.setExcitation (e);
         System.out.println ("setting excitation=" + e);
      }
   }
}
