package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.RigidEllipsoid;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class CylinderWrapping extends RootModel {

    public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      mech.setFrameDamping (100.0); // set damping parameters
      mech.setRotaryDamping (10.0);

      double density = 150;

      Particle via0 = new Particle (/*mass=*/0, /*x,y,z=*/-1.0, 0.0, 4.0);
      via0.setDynamic (false);
      mech.addParticle (via0);
      Particle p1 = new Particle (/*mass=*/0, /*x,y,z=*/-3.0, 0.0, 0.0);
      p1.setDynamic (false);
      mech.addParticle (p1);

      // create cylindrical wrapping object
      RigidCylinder cylinder = new RigidCylinder (
         "cylinder", /*rad=*/0.5, /*height=*/3.5, density, /*nsides=*/50);
      cylinder.setPose (new RigidTransform3d (0, 0, 1.5, 0, 0, Math.PI/2));
      cylinder.setDynamic (false);
      mech.addRigidBody (cylinder);

      // create ellipsoidal wrapping object
      double rad = 0.6;      
      RigidEllipsoid ellipsoid = new RigidEllipsoid (
         "ellipsoid", /*a,b,c=*/rad, 2*rad, rad, density, /*nslices=*/50);
      ellipsoid.transformGeometry (new RigidTransform3d (3, 0, 0));
      mech.addRigidBody (ellipsoid);

      // attach a marker to the ellipsoid
      FrameMarker p0 = new FrameMarker ();
      double halfRoot2 = Math.sqrt(2)/2;
      mech.addFrameMarker (
         p0, ellipsoid, new Point3d (-rad*halfRoot2, 0, rad*halfRoot2));

      // enable collisions between the ellipsoid and cylinder
      mech.setCollisionBehavior (cylinder, ellipsoid, true);

      // create the spring, making both segments wrappable with 50 knots
      MultiPointSpring spring = new MultiPointSpring ("spring", 300, 1.0, 0);
      spring.addPoint (p0);
      spring.setSegmentWrappable (50);
      spring.addPoint (via0);
      spring.setSegmentWrappable (50);
      spring.addPoint (p1);
      spring.addWrappable (cylinder);
      spring.addWrappable (ellipsoid);
      mech.addMultiPointSpring (spring);

      // set various rendering properties
      RenderProps.setSphericalPoints (mech, 0.1, Color.WHITE);
      RenderProps.setSphericalPoints (p1, 0.2, Color.BLUE);
      RenderProps.setSphericalPoints (spring, 0.1, Color.GRAY);
      RenderProps.setCylindricalLines (spring, 0.03, Color.RED);

      createControlPanel (spring);
   }

   private void createControlPanel (MultiPointSpring spring) {
      ControlPanel panel = new ControlPanel ("options", "");
      // creates a panel to control knot and A/B point visibility
      panel.addWidget (spring, "drawKnots");
      panel.addWidget (spring, "drawABPoints");
      addControlPanel (panel);
   }
}
