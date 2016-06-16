package artisynth.demos.fem;

import java.awt.*;
import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.femmodels.FemModel3d;
import artisynth.demos.fem.FemBeam3d;

public class QuadhexBeam3d extends FemBeam3d {

   public void build (String[] args) {
      super.build ("quadhex", 1.0, 0.2, 2, 1, /*options=*/0);


      myFemMod.setSurfaceRendering (FemModel3d.SurfaceRender.None);

      LinearMaterial lmat = new LinearMaterial (100000, 0.33);
      myFemMod.setMaterial (lmat);

      RenderProps.setLineColor (myFemMod.getElements().get(1), Color.RED);

      //super.build ("quadhex", 1.0, 0.4, 2, 1, VERTICAL | ADD_BLOCKS);
      //super (name, "quadhex", 1.0, 1.0, 2, 2, VERTICAL);

      //super (name, "quadhex", 4, 2, 0); //  /*options=*/ADD_MUSCLES);

      // myFemMod.setMaterial (
      //    new MooneyRivlinMaterial (150000.0, 0, 0, 0, 0, 15000000.0));
      // myFemMod.setSoftIncompMethod (IncompMethod.FULL);
      //myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);
   }
}
