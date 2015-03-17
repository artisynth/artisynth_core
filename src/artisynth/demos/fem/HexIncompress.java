package artisynth.demos.fem;

import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import maspack.matrix.*;

import java.util.*;

public class HexIncompress extends FemBeam3d {

   public void build (String[] args) {

      //      super (name, "hex", 1.0, 0.2, 8, 2, VERTICAL|ADD_DISPLACEMENT);

      // myFemMod.setMaterial (
      //    new MooneyRivlinMaterial (150000.0, 0, 0, 0, 0, 15000000.0));
      // myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);

      build ("hex", 1.0, 1.0, 3, 3, VERTICAL|ADD_DISPLACEMENT);

      myFemMod.setMaterial (
         new MooneyRivlinMaterial (5000.0, 0, 0, 0, 0, 5000000.0));
      //myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);
      myMechMod.setGravity (new Vector3d (0, 0, -9.8));
   }

}

