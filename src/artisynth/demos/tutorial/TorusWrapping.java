package artisynth.demos.tutorial;

import maspack.geometry.*;
import maspack.collision.SurfaceMeshIntersector;
import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.workspace.RootModel;
import artisynth.core.modelbase.*;

import java.awt.Color;

/**
 * Example showing a spring wrapped completely around a torus
 */
public class TorusWrapping extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      mech.setFrameDamping (1.0); // set damping parameters
      mech.setRotaryDamping (10.0);

      // create the torus
      double DTOR = Math.PI/180;
      double innerRad = 0.75;
      double outerRad = 2.0;
      RigidTorus torus =
         new RigidTorus ("torus", outerRad, innerRad, /*density=*/1);
      torus.setPose (new RigidTransform3d (2, 0, -2,  0, DTOR*90, 0));
      mech.addRigidBody (torus);

      // create start and end points for the spring
      Particle p0 = new Particle (0, /*x,y,z=*/4, 0.2, 2);
      p0.setDynamic (false);
      mech.addParticle (p0);
      Particle p1 = new Particle (0, /*x,y,z=*/-3, -0.2, 2);
      p1.setDynamic (false);
      mech.addParticle (p1);

      // create a wrappable MultiPointSpring between p0 and p1, with initial
      // points specified so that it wraps around the torus
      MultiPointSpring spring =
         new MultiPointSpring (/*k=*/10, /*d=*/0, /*restlen=*/0);
      spring.addPoint (p0);
      spring.setSegmentWrappable (
         100, new Point3d[] {
            new Point3d (3, 0, 0),
            new Point3d (2, 0, -1),
            new Point3d (1, 0, 0),
            new Point3d (2, 0, 1),
            new Point3d (3, 0, 0),
            new Point3d (2, 0, -1),
         });
      spring.addPoint (p1);
      spring.addWrappable (torus);
      spring.updateWrapSegments(); // ``shrink wrap'' around torus
      mech.addMultiPointSpring (spring);

      // set render properties
      RenderProps.setSphericalPoints (mech, 0.1, Color.WHITE);
      RenderProps.setCylindricalLines (spring, 0.05, Color.RED);
      RenderProps.setFaceColor (torus, new Color (200, 200, 230));
   }
}
