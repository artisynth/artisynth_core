package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import maspack.render.RenderProps;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;

public class FemBeam extends RootModel {

   // Models and dimensions
   FemModel3d fem;
   MechModel mech;
   double length = 1;
   double density = 10;
   double width = 0.3;
   double EPS = 1e-15;

   public void build (String[] args) throws IOException {

      // Create and add MechModel
      mech = new MechModel ("mech");
      addModel(mech);
      
      // Create and add FemModel
      fem = new FemModel3d ("fem");
      mech.add (fem);
      
      // Build hex beam using factory method
      FemFactory.createHexGrid (
         fem, length, width, width, /*nx=*/6, /*ny=*/3, /*nz=*/3);
      
      // Set FEM properties
      fem.setDensity (density);
      fem.setParticleDamping (0.1);
      fem.setMaterial (new LinearMaterial (4000, 0.33));
      
      // Fix left-hand nodes for boundary condition
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().x <= -length/2+EPS) {
            n.setDynamic (false);
         }
      }
      
      // Set rendering properties
      setRenderProps (fem);
      
   }

   // sets the FEM's render properties
   protected void setRenderProps (FemModel3d fem) {
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setFaceColor (fem, new Color (0.5f, 0.5f, 1f));
   }

}
