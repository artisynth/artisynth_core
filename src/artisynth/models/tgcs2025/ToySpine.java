package artisynth.models.tgcs2025;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.FrameSpring;
import artisynth.core.mechmodels.GimbalJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

/**
 * Toy example connecting together a set of rigid bodies formed from cervical
 * vertebra meshes. Data courtesy of Benedikt Sagl, Thomas Holzinger, and Dario
 * Cazzola.
 */
public class ToySpine extends RootModel {

   // keep some component references as class variables so they can be easily
   // accessed by methods and subclasses
   protected MechModel myMech;
   protected RigidBody myCerv2, myCerv3, myCerv4, myCerv5;
   protected Muscle myMuscleL, myMuscleR;

   // path to geometry folder, relative to the source folder for this class
   String geodir = getSourceRelativePath ("geometry/");

   /**
    * Creates a rigid body from a surface mesh and adds it to the MechModel.
    */
   RigidBody addBody (String name) {
      double density = 1000.0;
      // surface mesh path is inferred from the body name
      RigidBody body = RigidBody.createFromMesh (
         name, geodir + name + ".obj", density, /*scale*/1.0);
      body.centerPoseOnCenterOfMass(); // center frame on the centroid
      myMech.addRigidBody (body);
      return body;
   }

   /**
    * Attach two rigid bodies tother using a gimbal joint and frame spring.
    */
   void attachBodies (String name, RigidBody body1, RigidBody body2) {
      // TDW is the world coordinate frame for the joint/spring. Set this to
      // the midpoint between the two bodies.
      RigidTransform3d TDW = new RigidTransform3d();
      TDW.p.add (body1.getPose().p, body2.getPose().p);
      TDW.p.scale (0.5);
      // create a gimbal joint connecting body1 and body2 at TDW
      GimbalJoint joint = new GimbalJoint (body1, body2, TDW);
      joint.setName ("joint"+name);
      myMech.addBodyConnector (joint);

      // create a frame spring between body1 and body2 at TDW
      FrameSpring fspring = new FrameSpring ("spring"+name, 0, 0.001, 0, 0.0001);
      fspring.setFrames (body1, body2, TDW);
      myMech.attachFrameSpring (body1, body2, fspring);
   }

   /**
    * Build method for the model.
    */
   public void build (String[] args) throws IOException {
      // create a MechModel and add it to the RootModel
      myMech = new MechModel ("mech");
      addModel (myMech);

      // create bodies from meshes of cervical vertebrae
      myCerv2 = addBody ("cerv2");
      myCerv2.setDynamic (false);
      myCerv3 = addBody ("cerv3");
      myCerv4 = addBody ("cerv4");
      myCerv5 = addBody ("cerv5");
      // set the mesh color for the entire MechModel
      RenderProps.setFaceColor (myMech, new Color (1f, 1f, 0.8f));

      // add inertial damping
      myMech.setInertialDamping (1.0);

      // create attachments between the bodies
      attachBodies ("32", myCerv3, myCerv2);
      attachBodies ("43", myCerv4, myCerv3);
      attachBodies ("54", myCerv5, myCerv4);

      // create left and right points and markers to attach muscle to
      Point pntL = new Point ("pntL", new Point3d(0,  0.05, 0));
      myMech.addPoint (pntL);
      FrameMarker mkrL = myMech.addFrameMarkerWorld (
         myCerv5, new Point3d(0.0, 0.0262, -0.0416));

      Point pntR = new Point ("pntR", new Point3d(0, -0.05, 0));
      myMech.addPoint (pntR);
      FrameMarker mkrR = myMech.addFrameMarkerWorld (
         myCerv5, new Point3d(0.0, -0.0262, -0.0416));
      // render all points in the MechModel as white spheres
      RenderProps.setSphericalPoints (myMech, 0.001, Color.WHITE);

      // add point-to-point muscles between the points and markers
      myMuscleL = new Muscle ();
      myMuscleL.setMaterial (
         new SimpleAxialMuscle (/*stiffness*/0.1, /*damping*/0, /*maxForce*/0.1));
      myMech.attachAxialSpring (pntL, mkrL, myMuscleL);   

      myMuscleR = new Muscle ();
      myMuscleR.setMaterial (
         new SimpleAxialMuscle (/*stiffness*/0.1, /*damping*/0, /*maxForce*/0.1));
      myMech.attachAxialSpring (pntR, mkrR, myMuscleR);   

      // render all lines in the MechModel as red spindles
      RenderProps.setSpindleLines (myMech, 0.001, Color.RED);

      // add a control panel for muscle excitation
      ControlPanel panel = new ControlPanel ();
      panel.addWidget ("left excitation", myMuscleL, "excitation");
      panel.addWidget ("right excitation", myMuscleR, "excitation");
      addControlPanel (panel);

      // add an input probe to control the left excitation
      NumericInputProbe eprobe =
         new NumericInputProbe (myMuscleL, "excitation", 0, 10.0);
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
      eprobe.setActive (false);
      addInputProbe (eprobe);
      
      // add a controller to control the excitation instead
      ControllerBase controller = new MuscleController();
      controller.setActive (false);
      addController (controller);
   }

   /**
    * Controller to apply a sinusoidal excitation to the left muscle.
    */
   public class MuscleController extends ControllerBase {
      public void apply (double t0, double t1) {
         double period = 2.0;
         double e = 0.5*(1-Math.cos (t0*2*Math.PI/period));
         myMuscleL.setExcitation (e);
         System.out.println ("setting excitation=" + e);
      }
   }

   /**
    * Called if this model is read from a saved file.  Reinitializes the
    * MechModel and muscle references.
    */
   public void postscanInitialize() {
      myMech = (MechModel)findComponent ("models/mech");
      if (myMech != null) {
         myMuscleL = (Muscle)myMech.axialSprings().get(0);
         myMuscleR = (Muscle)myMech.axialSprings().get(1);
      }
   }
}
