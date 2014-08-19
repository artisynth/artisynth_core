package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import maspack.render.*;

import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

public class FemBeam extends RootModel {

   FemModel3d fem;
   MechModel mech;
   double length = 1;
   double density = 10;
   double width = 0.3;
   double EPS = 1e-15;

   public void build (String[] args) throws IOException {

      mech = new MechModel ("mech");
      fem = new FemModel3d ("fem");
      fem.setDensity (density);
      fem.setParticleDamping (0.1);
      FemFactory.createHexGrid (
         fem, length, width, width, /*nx=*/6, /*ny=*/3, /*nz=*/3);
      fem.setMaterial (new LinearMaterial (10000, 0.33));
      fem.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      setRenderProps (fem);

      // fix left hand nodes
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().x <= -length/2+EPS) {
            n.setDynamic (false);
         }
      }

      mech.add (fem);
      addModel (mech);
   }

   protected void setRenderProps (FemModel3d fem) {
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setFaceColor (fem, new Color (0.5f, 0.5f, 1f));
   }

   protected void setSphereRendering (Point pnt, Color color, double r) {
      RenderProps.setPointColor (pnt, color);
      RenderProps.setPointStyle (pnt, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (pnt, r);
   }

}
