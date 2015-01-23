package artisynth.demos.fem;

import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;

public class Hex3dBlock extends Fem3dBlock {

   public Hex3dBlock () {
   }

   public Hex3dBlock (String name) {
      super (name, "hex", 3, 1, 0);
      //super (name, "hex", 1, 1, 0);
      MechModel mechMod = (MechModel)findComponent ("models/mech");
      mechMod.setDefaultCollisionBehavior (true, 0);
      
   }
}
