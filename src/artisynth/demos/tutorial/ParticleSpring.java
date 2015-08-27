package artisynth.demos.tutorial;

import java.awt.Color;
import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

/**
 * Demo of two particles connected by a spring
 */
public class ParticleSpring extends RootModel {

   public void build (String[] args) {

      // create MechModel and add to RootModel
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create the components
      Particle p1 = new Particle ("p1", /*mass=*/2, /*x,y,z=*/0, 0, 0);
      Particle p2 = new Particle ("p2", /*mass=*/2, /*x,y,z=*/1, 0, 0);
      AxialSpring spring = new AxialSpring ("spr", /*restLength=*/0);
      spring.setPoints (p1, p2);
      spring.setMaterial (
         new LinearAxialMaterial (/*stiffness=*/20, /*damping=*/10));

      // add components to the mech model
      mech.addParticle (p1);
      mech.addParticle (p2);
      mech.addAxialSpring (spring);

      p1.setDynamic (false);                // first particle set to be fixed

      // increase model bounding box for the viewer
      mech.setBounds (/*min=*/-1, 0, -1, /*max=*/1, 0, 0);  
      // set render properties for the components
      RenderProps.setSphericalPoints (p1, 0.06, Color.RED);
      RenderProps.setSphericalPoints (p2, 0.06, Color.RED);
      RenderProps.setCylindricalLines (spring, 0.02, Color.BLUE);
   }
}
