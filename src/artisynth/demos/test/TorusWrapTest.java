package artisynth.demos.test;

import java.awt.Color;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidTorus;
import artisynth.core.workspace.RootModel;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

public class TorusWrapTest extends RootModel {
   private boolean pointsAttached = false;

   private boolean collisionEnabled = false;

   private double planeZ = -20;

   private double myDensity = 10;

   protected static double size = 1.0;

   public void build (String[] args) {
      MechModel mechMod = new MechModel ("mechMod");

      mechMod.setGravity (0, 0, -9.8);
      mechMod.setFrameDamping (10.0);
      mechMod.setRotaryDamping (0.1);

      RigidBody block = new RigidBody ("block");
      PolygonalMesh mesh = MeshFactory.createBox (size, size, size);
      block.setMesh (mesh, null);
      block.setInertiaFromDensity (1);
      //mechMod.addRigidBody (block);

      Particle p0 = new Particle (0.1, size*3, 0, size / 2);
      p0.setDynamic (false);
      mechMod.addParticle (p0);

      Particle p1 = new Particle (0.1, -size*3, 0, size / 2);
      p1.setDynamic (false);
      mechMod.addParticle (p1);

      Particle p2 = new Particle (0.1, -size*3, size, size / 2);
      p2.setDynamic (false);
      //mechMod.addParticle (p2);

      Particle p3 = new Particle (0.1, size*3, size, size / 2);
      p3.setDynamic (false);
      //mechMod.addParticle (p3);

      RigidTorus torus1 = new RigidTorus (
         "torus1", size, size/3, myDensity, 50, 30);
      RigidTorus torus2 = new RigidTorus (
         "torus2", size, size/3, myDensity, 50, 30);
      RigidTorus torus3 = new RigidTorus (
         "torus3", size, size/3, myDensity, 50, 30);
      torus1.setPose (new RigidTransform3d (-2*size, 0, 0, 0, Math.PI/2, 0));
      torus2.setPose (new RigidTransform3d (2*size, 0, 0, 0, Math.PI/2, 0));
      torus3.setPose (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      mechMod.addRigidBody (torus1);
      mechMod.addRigidBody (torus2);
      mechMod.addRigidBody (torus3);

      mechMod.setDefaultCollisionBehavior (true, 0);

      MultiPointSpring spring = new MultiPointSpring ("spring", 100, 0.1, 0);
      spring.addPoint (p0);
      spring.setSegmentWrappable (50);
      spring.addWrappable (torus1);
      spring.addWrappable (torus2);
      spring.addWrappable (torus3);
      spring.addPoint (p1);
      //spring.addPoint (p2);
      //spring.setSegmentWrappable (50);
      //spring.addPoint (p3);
      //spring.setWrapDamping (1.0);
      //spring.setWrapStiffness (10);
      //spring.setWrapH (0.01);
      mechMod.addMultiPointSpring (spring);

      spring.setDrawKnots (false);
      spring.setDrawABPoints (true);
      spring.setWrapDamping (100);
      spring.setMaxWrapIterations (10);

      addModel (mechMod);

      RenderProps.setPointStyle (mechMod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mechMod, size / 10);
      RenderProps.setLineStyle (mechMod, Renderer.LineStyle.CYLINDER);
      RenderProps.setLineRadius (mechMod, size / 30);
      RenderProps.setLineColor (mechMod, Color.red);
      RenderProps.setFaceColor (mechMod, new Color (238, 232, 170));

      createControlPanel (mechMod);
   }

   private void createControlPanel (MechModel mech) {
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (mech, "integrator");
      panel.addWidget (mech, "maxStepSize");
      panel.addWidget (mech, "gravity");
      panel.addWidget (mech, "multiPointSprings/spring:drawKnots");
      panel.addWidget (mech, "multiPointSprings/spring:drawABPoints");
      panel.addWidget (mech, "multiPointSprings/spring:wrapDamping");
      panel.addWidget (mech, "multiPointSprings/spring:maxWrapIterations");
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }
}
