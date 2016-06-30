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
import artisynth.core.mechmodels.RigidEllipsoid;
import artisynth.core.workspace.RootModel;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

public class EllipsoidWrapTest extends RootModel {
   private boolean pointsAttached = false;

   private boolean collisionEnabled = false;

   private double planeZ = -20;

   private double myDensity = 0.2;

   protected static double size = 1.0;

   public void build (String[] args) {
      MechModel mechMod = new MechModel ("mechMod");

      mechMod.setGravity (0, 0, -9.8);
      mechMod.setFrameDamping (1.0);
      mechMod.setRotaryDamping (0.1);

      RigidBody block = new RigidBody ("block");
      PolygonalMesh mesh = MeshFactory.createBox (size, size, size);
      block.setMesh (mesh, null);
      block.setInertiaFromDensity (1);
      //mechMod.addRigidBody (block);

      Particle p0 = new Particle (0.1, size*3, -size, size / 2);
      p0.setDynamic (false);
      mechMod.addParticle (p0);

      Particle p1 = new Particle (0.1, -size*3, -size, size / 2);
      p1.setDynamic (false);
      mechMod.addParticle (p1);

      Particle p2 = new Particle (0.1, -size*3, size, size / 2);
      p2.setDynamic (false);
      mechMod.addParticle (p2);

      Particle p3 = new Particle (0.1, size*3, size, size / 2);
      p3.setDynamic (false);
      mechMod.addParticle (p3);

      RigidEllipsoid ellipsoid = new RigidEllipsoid (
         "ellipsoid", size, 2.5*size, size/2, myDensity, 50);
      ellipsoid.setPose (new RigidTransform3d (0, 0, 1.5*size, 0, 0, 0));
      // RigidEllipsoid ellipsoid = new RigidEllipsoid (
      //    "ellipsoid", 2*size, 4*size, 2*size, myDensity/10, 50);
      // ellipsoid.setPose (new RigidTransform3d (0, 0, 3*size, 0, 0, 0));
      //ellipsoid.setDynamic (false);

      mechMod.addRigidBody (ellipsoid);

      MultiPointSpring spring = new MultiPointSpring ("spring", 1, 0.1, 0);
      spring.addPoint (p0);
      spring.setSegmentWrappable (50);
      spring.addWrappable (ellipsoid);
      spring.addPoint (p1);
      spring.addPoint (p2);
      spring.setSegmentWrappable (50);
      spring.addPoint (p3);
      //spring.setWrapDamping (1.0);
      //spring.setWrapStiffness (10);
      //spring.setWrapH (0.01);
      mechMod.addMultiPointSpring (spring);

      spring.setDrawKnots (false);
      spring.setDrawABPoints (true);
      spring.setWrapDamping (100);

      addModel (mechMod);

      RenderProps.setPointStyle (mechMod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mechMod, size / 10);
      RenderProps.setLineStyle (mechMod, Renderer.LineStyle.CYLINDER);
      RenderProps.setLineRadius (mechMod, size / 30);
      RenderProps.setLineColor (mechMod, Color.red);

      createControlPanel (mechMod);
   }

   private void createControlPanel (MechModel mech) {
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (mech, "integrator");
      panel.addWidget (mech, "maxStepSize");
      panel.addWidget (mech, "gravity");
      panel.addWidget (mech, "multiPointSprings/spring:drawKnots");
      panel.addWidget (mech, "multiPointSprings/spring:drawABPoints");
      panel.addWidget (mech, "multiPointSprings/spring:drawABPoints");
      panel.addWidget (mech, "multiPointSprings/spring:wrapDamping");
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }
}
