package artisynth.demos.fem;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class EmbeddedDemo extends EmbeddedHeart {

   public static boolean omitFromMenu = true;

   public void build (String[] args) throws IOException {
      super.build (args);

      MechModel mech = (MechModel)findComponent ("models/mech");
      RigidBody plate = mech.rigidBodies().get ("plate");
      FemMuscleModel heart = (FemMuscleModel)mech.findComponent ("models/heart");
      heart.setName ("fem");

      // attach top nodes
      Point3d max = new Point3d();
      RenderableUtils.getBounds (heart, null, max);
      for (FemNode3d n : heart.getNodes()) {
         if (Math.abs(n.getPosition().z-max.z) <= 1e-8) {
            n.setDynamic (false);
         }
      }

      FemMeshComp embeddedHeart = heart.getMeshComps().get("embedded");
      FemMeshComp collideSurface = heart.getMeshComps().get("collideSurface");
      
      heart.setSurfaceRendering (SurfaceRender.Shaded);

      RenderProps.setVisible (embeddedHeart, false);
      RenderProps.setVisible (plate, false);
      //RenderProps.setLineWidth (heart, 2);
      //RenderProps.setLineWidth (heart.getMuscleBundles(), 3);

      // turn off probe
      getInputProbes().get(0).setActive (false);
   }

}
