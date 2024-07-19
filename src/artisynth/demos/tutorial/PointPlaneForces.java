package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.PointPlaneForce;
import artisynth.core.mechmodels.PointPlaneForce.ForceType;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

/**
 * Demonstrates soft collision between a particle and two planes, using
 * PointPlaneForce.
 */
public class PointPlaneForces extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create the particle that will collide with the planes
      Particle p = new Particle ("part", 1, 1.0, 0, 2.0);
      mech.addParticle (p);

      // create the PointPlaneForce for the left plane
      double stiffness = 1000;
      PointPlaneForce ppfL =
         new PointPlaneForce (p, new Vector3d (1, 0, 1), Point3d.ZERO);
      ppfL.setStiffness (stiffness);
      ppfL.setPlaneSize (5.0);
      ppfL.setUnilateral (true);
      mech.addForceEffector (ppfL);

      // create the PointPlaneForce for the right plane
      PointPlaneForce ppfR =
         new PointPlaneForce (p, new Vector3d (-1, 0, 1), Point3d.ZERO);
      ppfR.setStiffness (stiffness);
      ppfR.setPlaneSize (5.0);
      ppfR.setUnilateral (true);
      mech.addForceEffector (ppfR);

      // render properties: make the particle red, and the planes blue-gray
      RenderProps.setSphericalPoints (mech, 0.1, Color.RED);
      RenderProps.setFaceColor (mech, new Color (0.7f, 0.7f, 0.9f));
   }
}
