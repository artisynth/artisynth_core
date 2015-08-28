package artisynth.demos.tutorial;

import java.awt.Color;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;
import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.RigidEllipsoid;
import artisynth.core.mechmodels.RigidSphere;
import artisynth.core.mechmodels.SoftPlaneCollider;
import artisynth.core.workspace.RootModel;

public class CylinderWrapping extends RootModel {

    public void build (String[] args) {

      MechModel mechMod = new MechModel ("mechMod");
      addModel (mechMod);
      mechMod.setGravity (0, 0, -9.8);
      mechMod.setFrameDamping (100.0);
      mechMod.setRotaryDamping (10.0);

      double density = 150;
      double size = 1.0;

      Particle p1 = new Particle (0.1, -size, 0, 4*size);
      p1.setDynamic (false);
      mechMod.addParticle (p1);
      Particle p2 = new Particle (0.1, -size, 0, 0);
      p2.setDynamic (false);
      mechMod.addParticle (p2);

      // create cylindrical wrapping object
      RigidCylinder cylinder = new RigidCylinder (
         "cylinder", size/2, 3.5*size, density, 50);
      cylinder.setPose (new RigidTransform3d (0, 0, 1.5*size, 0, 0, Math.PI/2));
      cylinder.setDynamic (false);
      mechMod.addRigidBody (cylinder);

      // create ellipsoidal wrapping object
      double rad = 0.6*size;      
      RigidEllipsoid ball =
         new RigidEllipsoid ("ball", rad, 2*rad, rad, density, 50);
      ball.transformGeometry (new RigidTransform3d (size*3, 0, 0));
      mechMod.addRigidBody (ball);

      // attach a marker to the ball
      FrameMarker p0 = new FrameMarker ();
      double halfRoot2 = Math.sqrt(2)/2;
      mechMod.addFrameMarker (
         p0, ball, new Point3d (-rad*halfRoot2, 0, rad*halfRoot2));

      // enable collisions between the ball and cylinder
      mechMod.setCollisionBehavior (cylinder, ball, true);

      // create the spring, making the segments between p0-p1 and p1-p2 each
      // wrappable with 50 knot points each
      MultiPointSpring spring = new MultiPointSpring ("spring", 300, 1.0, 0);
      spring.addPoint (p0);
      spring.setSegmentWrappable (50, new Point3d[] {
            new Point3d(3, 0, 3)
         });
      spring.addWrappable (cylinder);
      spring.addWrappable (ball);
      spring.addPoint (p1);
      spring.setSegmentWrappable (50);
      spring.addPoint (p2);
      // optional: shrink wrapping segments around obstacles
      spring.updateWrapSegments();
      mechMod.addMultiPointSpring (spring);

      // set various rendering properties
      RenderProps.setSphericalPoints (p0, size/10, Color.WHITE);
      RenderProps.setSphericalPoints (p1, size/5, Color.BLUE);
      RenderProps.setSphericalPoints (p2, size/10, Color.WHITE);
      RenderProps.setSphericalPoints (spring, size/10, Color.GRAY);
      RenderProps.setCylindricalLines (spring, size/30, Color.RED);

      createControlPanel (spring);
   }

   private void createControlPanel (MultiPointSpring spring) {
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (this, "models/mechMod:integrator");
      panel.addWidget (this, "models/mechMod:maxStepSize");
      panel.addWidget (this, "models/mechMod:gravity");
      panel.addWidget (spring, "drawKnots");
      panel.addWidget (spring, "drawABPoints");
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }
}
