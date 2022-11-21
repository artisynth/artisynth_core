package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

/**
 * Program to test a Maxwell element implemented two ways: by two AxialSprings
 * in series, one with stiffness and the other with damping, and by using a
 * MaxwellAxialMaterial. The resulting spring length from both methods is
 * printed in the advance() method.
 */
public class MaxwellElement extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, 0);
      addModel (mech);

      // Implementation using two AxialSprings. Need three points - anchor,
      // middle point, and end point where force is applied
      Particle p0 = new Particle (/*mass=*/1, 0, 0, 0.25);
      mech.addParticle (p0);
      p0.setDynamic (false);
      Particle p1 = new Particle (/*mass=*/0.0001, 1, 0, 0.25);
      mech.addParticle (p1);
      Particle p2 = new Particle (/*mass=*/1, 1.1, 0, 0.25);
      mech.addParticle (p2);

      AxialSpring spr0 =
         new AxialSpring (/*stiffness=*/10, /*damping=*/0, /*restLength=*/1);
      mech.attachAxialSpring (p0, p1, spr0);
      AxialSpring spr1 =
         new AxialSpring (/*stiffness=*/0, /*damping=*/1, /*restLength=*/0);
      mech.attachAxialSpring (p1, p2, spr1);
      p2.setExternalForce (new Vector3d(1,0,0)); // external force on end point

      // Implementation using AxialMaxwellMaterial. Need two points - anchor
      // and end point where force is applied
      Particle p3 = new Particle (/*mass=*/1, 0, 0, 0);
      mech.addParticle (p3);
      p3.setDynamic (false);
      Particle p4 = new Particle (/*mass=*/1, 1.1, 0, 0);
      mech.addParticle (p4);

      AxialSpring spr2 = new AxialSpring ();
      spr2.setRestLength (1);
      spr2.setMaterial (
         new MaxwellAxialMaterial (
            /*stiffness=*/10, /*damping=*/1, /*springLength*/1));
      mech.attachAxialSpring (p3, p4, spr2);
      p4.setExternalForce (new Vector3d(1,0,0)); // external force on end point

      // set render properties
      RenderProps.setSphericalPoints (mech, 0.02, Color.WHITE);
      RenderProps.setSphericalPoints (p1, 0.02, Color.BLUE);
      RenderProps.setSpindleLines (mech, 0.02, Color.RED);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      StepAdjustment sa = super.advance (t0, t1, flags);
      // print out the spring lengths (lk) computed by both methods to see how
      // well they compare. Get these lengths, respectively, from the x
      // coordinate of the mid particle of the two spring implementation and
      // the springLength property of the AxialMaxwellMaterial.

      MechModel mech = (MechModel)findComponent ("models/mech");
      Point midPoint = mech.particles().get(1);
      AxialSpring auxMatSpring = mech.axialSprings().get(2);
      
      System.out.printf ("t1=%5.3f\n", t1);
      System.out.printf ("  lk (two spring): %g\n", midPoint.getPosition().x);
      System.out.printf (
         "  lk (maxwellMat): %g\n",
         ((MaxwellAxialMaterial)auxMatSpring.getMaterial()).getSpringLength());               
      return sa;
   }

}
