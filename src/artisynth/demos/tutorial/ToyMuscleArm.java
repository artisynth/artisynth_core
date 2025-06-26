package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.util.PathFinder;

/**
 * A toy two-link mechanical arm controlled by two sets of opposing muscles.
 */
public class ToyMuscleArm extends RootModel {
   // geodir is folder in which to locate mesh data:
   protected String geodir = PathFinder.getSourceRelativePath (this, "data/");
   protected double density = 1000.0;   // default density
   protected double stiffness = 1000.0; // passive muscle stiffness

   protected MechModel myMech;     // mech model
   protected FrameMarker myTipMkr; // marker at tip of the arm
   protected HingeJoint myHinge0;  // lower hinge joint
   protected HingeJoint myHinge1;  // upper hinge joint
   protected RigidBody myLink0;    // lower link
   protected RigidBody myLink1;    // upper link

   /**
    * Add an axial muscle between body0 and body1 using markers attached at
    * world coords (x0,0,z0) and (x1,0,z1), respectively.
    */
   public Muscle addMuscle (
      RigidBody body0, double x0, double z0, 
      RigidBody body1, double x1, double z1) {

      FrameMarker l0 = myMech.addFrameMarkerWorld (body0, new Point3d(x0, 0, z0));
      FrameMarker l1 = myMech.addFrameMarkerWorld (body1, new Point3d(x1, 0, z1));
      Muscle muscle = new Muscle ();
      muscle.setMaterial (
         new SimpleAxialMuscle (stiffness, /*damping=*/0, /*fmax=*/1000));
      myMech.attachAxialSpring (l0, l1, muscle);
      muscle.setRestLength (muscle.getLength()); // init rest length
      return muscle;
   }

   /**
    * Add an wrapped muscle between body0 and body1 using markers attached at
    * world coords (x0,0,z0) and (x1,0,z1) and wrapping around wrapBody.
    */
   public MultiPointMuscle addWrappedMuscle (
      RigidBody body0, double x0, double z0,
      RigidBody body1, double x1, double z1, Wrappable wrapBody) {

      FrameMarker l0 = myMech.addFrameMarkerWorld (body0, new Point3d(x0, 0, z0));
      FrameMarker l1 = myMech.addFrameMarkerWorld (body1, new Point3d(x1, 0, z1));
      MultiPointMuscle muscle = new MultiPointMuscle ();
      muscle.setMaterial (
         new SimpleAxialMuscle (stiffness, /*damping=*/0, /*fmax=*/500));
      muscle.addPoint (l0);
      muscle.setSegmentWrappable (/*numknots=*/50);
      muscle.addPoint (l1);
      muscle.addWrappable (wrapBody);
      muscle.updateWrapSegments(); // shrink wrap to current wrappable
      muscle.setRestLength (muscle.getLength()); // init rest length
      myMech.addMultiPointSpring (muscle);
      return muscle;
   }

   /**
    * Adds a hinge joint between body0 and body1, at world coordinates
    * (x0,0,z0) and with the joint axis parallel to y. Joint limits are set to
    * minDeg and maxDeg (in degrees).
    */
   public HingeJoint addHingeJoint (
      RigidBody body0, RigidBody body1,
      double x0, double z0, double minDeg, double maxDeg) {

      HingeJoint hinge = new HingeJoint (
         body0, body1, new Point3d(x0, 0, z0), new Vector3d (0, 1, 0));
      myMech.addBodyConnector (hinge);
      hinge.setThetaRange (minDeg, maxDeg);
      // set render properties for the hinge
      hinge.setShaftLength (0.4);
      RenderProps.setFaceColor (hinge, Color.BLUE);
      return hinge;
   }

   public void build (String[] args) throws IOException {
      myMech = new MechModel ("mech");
      myMech.setInertialDamping (1.0);
      addModel (myMech);

      // create base body
      PolygonalMesh mesh = new PolygonalMesh (geodir+"bracketedBase.obj");
      RigidBody base = RigidBody.createFromMesh ("base", mesh, density, 1.0);
      base.setDynamic (false);
      myMech.addRigidBody (base);

      // create rigid body for link0 and place it above the origin
      mesh = MeshFactory.createRoundedBox (0.6, 0.2, 0.2, /*nslices=*/20);
      myLink0 = RigidBody.createFromMesh ("link0", mesh, density, /*scale=*/1.0);
      myLink0.setPose (new RigidTransform3d (0, 0, 0.3));
      myMech.addRigidBody (myLink0);

      // create rigid body for link1 and place it after link0
      mesh = MeshFactory.createRoundedBox (0.6, 0.10, 0.15, /*nslices=*/20);
      myLink1 = RigidBody.createFromMesh ("link1", mesh, density, /*scale=*/1.0);
      myLink1.setPose (new RigidTransform3d (0, 0, 0.9));
      myMech.addRigidBody (myLink1);

      // create massless cylinder for wrapping surface and attach it to link1
      RigidCylinder cylinder = new RigidCylinder (
         "wrapSurface", /*rad=*/0.12, /*h=*/0.25, /*density=*/0, /*nsegs=*/32);
      cylinder.setPose (new RigidTransform3d (0, 0, 0.6,  0, 0, Math.PI/2));
      myMech.addRigidBody (cylinder);
      myMech.attachFrame (cylinder, myLink1);     
      
      // add a hinge joints between links
      myHinge0 = addHingeJoint (myLink0, base, 0, 0, -70, 70);
      myHinge1 = addHingeJoint (myLink1, myLink0, 0, 0.6, -120, 120);

      Muscle muscleL0 = addMuscle (myLink0, -0.1, 0.4, base, -0.48, -0.15);
      Muscle muscleR0 = addMuscle (myLink0,  0.1, 0.4, base,  0.48, -0.15);
      MultiPointMuscle muscleL1 = addWrappedMuscle (
         myLink1, -0.05, 1.2, myLink0, -0.1, 0.5, cylinder);
      MultiPointMuscle muscleR1 = addWrappedMuscle (
         myLink1,  0.05, 1.2, myLink0,  0.1, 0.5, cylinder);

      // add a marker at the tip
      myTipMkr = myMech.addFrameMarkerWorld (
         myLink1, new Point3d(0.0, 0.0, 1.25));

      // add a control panel for muscle excitations
      ControlPanel panel = new ControlPanel ();
      panel.addWidget ("excitation L0", muscleL0, "excitation");
      panel.addWidget ("excitation R0", muscleR0, "excitation");
      panel.addWidget ("excitation L1", muscleL1, "excitation");
      panel.addWidget ("excitation R1", muscleR1, "excitation");
      addControlPanel (panel);

      // render properties: muscles as red spindles, points as white spheres,
      // bodies blue-gray, tip marker green, and wrap cylinder purple-gray
      RenderProps.setSpindleLines (myMech, 0.02, Color.RED);
      RenderProps.setSphericalPoints (myMech, 0.02, Color.WHITE);
      RenderProps.setFaceColor (myMech, new Color (0.71f, 0.71f, 0.85f));
      RenderProps.setPointColor (myTipMkr, Color.GREEN);
      RenderProps.setFaceColor (cylinder, new Color (0.75f, 0.61f, 0.75f));
   }
}
