package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import maspack.render.*;
import maspack.matrix.*;

import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

public class FemBeamWithMuscle extends FemBeam {

   protected Muscle createMuscle () {
      Muscle mus = new Muscle (/*name=*/null, /*restLength=*/0);
      mus.setMaterial (
         new SimpleAxialMuscle (/*stiffness=*/20, /*damping=*/10, /*maxf=*/10));
      RenderProps.setLineStyle (mus, RenderProps.LineStyle.ELLIPSOID);
      RenderProps.setLineColor (mus, Color.RED);
      RenderProps.setLineRadius (mus, 0.03);
      return mus;
   }

   protected FemMarker createMarker (
      FemModel3d fem, double x, double y, double z) {
      FemMarker mkr = new FemMarker (/*name=*/null, x, y, z);
      setSphereRendering (mkr, Color.BLUE, 0.02);
      fem.addMarker (mkr);
      return mkr;
   }

   public void build (String[] args) throws IOException {

      super.build (args);

      // create two muscles and attach them to the end of the FemModel 
      Muscle muscle = createMuscle();
      Particle p1 = new Particle (/*mass=*/0, -length/2, 0, 2*width);
      p1.setDynamic (false);
      setSphereRendering (p1, Color.BLUE, 0.02);
      FemMarker mkr = createMarker (fem, length/2-0.1, 0, width/2);

      muscle.setPoints (p1, mkr);
      mech.addParticle (p1);
      mech.addAxialSpring (muscle);
   }

}
