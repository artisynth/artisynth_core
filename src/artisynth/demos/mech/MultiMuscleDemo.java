package artisynth.demos.mech;

import java.awt.Color;

import maspack.matrix.Point3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import artisynth.core.materials.LinearAxialMuscle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.gui.ControlPanel;

public class MultiMuscleDemo extends MultiSpringDemo {

   public void build (String[] args) {
      super.build (args);

      MechModel mechMod = (MechModel)models().get ("mechMod");
      RigidBody block = mechMod.rigidBodies ().get ("block");
      
      Particle p0 = new Particle (0.1, 0, -size * 3, size / 2);
      p0.setDynamic (false);
      mechMod.addParticle (p0);

      Particle p1 = new Particle (0.1, 0, size * 3, size / 2);
      p1.setDynamic (false);
      mechMod.addParticle (p1);

      FrameMarker mkr0 = new FrameMarker();
      mechMod.addFrameMarker (mkr0, block, new Point3d (0, -size / 2, size / 2));

      FrameMarker mkr1 = new FrameMarker();
      mechMod.addFrameMarker (mkr1, block, new Point3d (0, size / 2, size / 2));

      MultiPointMuscle muscle = new MultiPointMuscle ();
      LinearAxialMuscle mat = new LinearAxialMuscle();
      mat.setForceScaling (1);
      mat.setMaxForce (10);
      mat.setMaxLength (size);
      mat.setPassiveFraction (0.1);
      muscle.setMaterial(mat);
      muscle.addPoint (p0);
      muscle.addPoint (mkr0);
      muscle.addPoint (mkr1);
      muscle.addPoint (p1);
      mechMod.addMultiPointSpring (muscle);

      RenderProps.setLineColor (mechMod, Color.BLUE);
      RenderProps.setLineStyle (muscle, LineStyle.SPINDLE);
      RenderProps.setLineRadius (muscle, 0.1);      
      RenderProps.setLineColor (muscle, Color.RED);

      ControlPanel panel = getControlPanels().get(0);
      panel.addWidget (muscle, "excitation");
      panel.pack();

   }
}
