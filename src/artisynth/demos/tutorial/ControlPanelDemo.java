package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

/**
 * Demo that shows how to create a ControlPanel to adjust some physical and
 * rendering parameters of simple model consisting of springs and spheres.
 */
public class ControlPanelDemo extends RootModel {

   double myStiffness = 10.0; // default spring stiffness
   double myMaxForce = 100.0; // excitation force multiplier
   double DTOR = Math.PI/180; // degrees to radians

   // Create and attach a simple muscle with default parameters between p0 and p1
   Muscle attachMuscle (String name, MechModel mech, Point p0, Point p1) {
      Muscle mus = new Muscle (name);
      mus.setMaterial (
         new SimpleAxialMuscle (myStiffness, /*damping=*/0, myMaxForce));
      mech.attachAxialSpring (p0, p1, mus);
      return mus;
   }

   public void build (String[] args) {
      // create a mech model with zero gravity
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      mech.setGravity (0, 0, 0);
      mech.setInertialDamping (0.1); // add some damping

      double density = 100.0;
      double particleMass = 50.0;

      // create three spheres, each with a different color, along with a marker
      // to attach a spring to, and arrange them roughly around the origin
      RigidBody sphere0 = RigidBody.createIcosahedralSphere (
         "sphere0", 0.5, density, /*ndivs=*/2);
      FrameMarker mkr0 = mech.addFrameMarker (sphere0, new Point3d(0, 0, 0.5));
      sphere0.setPose (new RigidTransform3d (1, 0, -0.5,  0, -DTOR*60, 0));
      RenderProps.setFaceColor (sphere0, new Color(0f, 0.4f, 0.8f));
      mech.addRigidBody (sphere0);

      RigidBody sphere1 = RigidBody.createIcosahedralSphere (
         "sphere1", 0.5, 1.5*density, /*ndivs=*/2);
      FrameMarker mkr1 = mech.addFrameMarker (sphere1, new Point3d(0, 0, 0.5));
      sphere1.setPose (new RigidTransform3d (-1, 0, -0.5,  0, DTOR*60, 0));
      RenderProps.setFaceColor (sphere1, new Color(0f, 0.8f, 0.4f));
      mech.addRigidBody (sphere1);

      RigidBody sphere2 = RigidBody.createIcosahedralSphere (
         "sphere2", 0.5, 1.5*density, /*ndivs=*/2);
      FrameMarker mkr2 = mech.addFrameMarker (sphere2, new Point3d(0, 0, 0.5));
      sphere2.setPose (new RigidTransform3d (0, 0, 1.1,  0, -DTOR*180, 0));
      RenderProps.setFaceColor (sphere2, new Color(0f, 0.8f, 0.8f));
      mech.addRigidBody (sphere2);

      // create three muscles to connect the bodies via their markers
      Muscle muscle0 = attachMuscle ("muscle0", mech, mkr1, mkr0);
      Muscle muscle1 = attachMuscle ("muscle1", mech, mkr0, mkr2);
      Muscle muscle2 = attachMuscle ("muscle2", mech, mkr1, mkr2);

      // enable collisions between the spheres
      mech.setDefaultCollisionBehavior (true, /*mu=*/0);

      // render muscles as red spindles
      RenderProps.setSpindleLines (mech, 0.05, Color.RED);
      // render markers as white spheres. Note: unlike rigid bodies, markers
      // normally have null render properties, and so we need to explicitly set
      // their render properties for use in the control panel
      RenderProps.setSphericalPoints (mkr0, 0.1, Color.WHITE);
      RenderProps.setSphericalPoints (mkr1, 0.1, Color.WHITE);
      RenderProps.setSphericalPoints (mkr2, 0.1, Color.WHITE);

      // create a control panel to collectively set muscle excitation and
      // stiffness, inertial damping, sphere visibility and color, and marker
      // color. Muscle excitations and sphere colors can also be set
      // individually.
      ControlPanel panel = new ControlPanel();
      panel.addWidget ("excitation", muscle0, muscle1, muscle2);
      panel.addWidget ("excitation 0", "excitation", muscle0);
      panel.addWidget ("excitation 1", "excitation", muscle1);
      panel.addWidget ("excitation 2", "excitation", muscle2);
      panel.addWidget (
         "stiffness", "material.stiffness", muscle0, muscle1, muscle2);
      panel.addWidget ("inertialDamping", sphere0, sphere1, sphere2);
      panel.addWidget (
         "spheres visible", "renderProps.visible", sphere0, sphere1, sphere2);
      panel.addWidget (
         "spheres color", "renderProps.faceColor", sphere0, sphere1, sphere2);
      panel.addWidget ("sphere 0 color", "renderProps.faceColor", sphere0);
      panel.addWidget ("sphere 1 color", "renderProps.faceColor", sphere1);
      panel.addWidget ("sphere 2 color", "renderProps.faceColor", sphere2);
      panel.addWidget (
         "marker color", "renderProps.pointColor", mkr0, mkr1, mkr2);
      addControlPanel (panel);
   }
}
