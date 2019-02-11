package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;

public class ViaPointMuscle extends RootModel {

   protected static double size = 1.0;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      mech.setFrameDamping (1.0); // set damping parameters
      mech.setRotaryDamping (0.1);

      // create block to which muscle will be attached
      RigidBody block = RigidBody.createBox (
         "block", /*widths=*/1.0, 1.0, 1.0, /*density=*/1.0);
      mech.addRigidBody (block);

      // create muscle start and end points
      Particle p0 = new Particle (/*mass=*/0.1, /*x,y,z=*/-3.0, 0, 0.5);
      p0.setDynamic (false);
      mech.addParticle (p0);
      Particle p1 = new Particle (/*mass=*/0.1, /*x,y,z=*/3.0, 0, 0.5);
      p1.setDynamic (false);
      mech.addParticle (p1);

      // create markers to serve as via points
      FrameMarker via0 = new FrameMarker();
      mech.addFrameMarker (via0, block, new Point3d (-0.5, 0, 0.5));
      FrameMarker via1 = new FrameMarker();
      mech.addFrameMarker (via1, block, new Point3d (0.5, 0, 0.5));

      // create muscle, set material, and add points
      MultiPointMuscle muscle = new MultiPointMuscle ();
      muscle.setMaterial (new SimpleAxialMuscle (/*k=*/1, /*d=*/0, /*maxf=*/10));
      muscle.addPoint (p0);
      muscle.addPoint (via0);
      muscle.addPoint (via1);
      muscle.addPoint (p1);
      mech.addMultiPointSpring (muscle);

      // set render properties
      RenderProps.setSphericalPoints (mech, 0.1, Color.WHITE);
      RenderProps.setCylindricalLines (mech, 0.03, Color.RED);

      createControlPanel();
   }

   private void createControlPanel() {
      // creates a panel to adjust the muscle excitation
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (this, "models/mech/multiPointSprings/0:excitation");
      addControlPanel (panel);
   }
}
