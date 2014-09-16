package artisynth.demos.fem;

import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.femmodels.*;

public class Hex3dBlock extends Fem3dBlock {

   public Hex3dBlock () {
   }

   public Hex3dBlock (String name) {
      super (name, "hex", 3, 1, 0);

      
   }
}
