package artisynth.demos.fem;

import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.util.RandomGenerator;

import java.util.*;

public class RobustCube extends FemBeam3d {

   public RobustCube() {
   }

   public RobustCube (String name) {

      super (name, "hex", 1.0, 1.0, 5, 5, /*options=*/0);

      RandomGenerator.setSeed (0x1234);

      Point3d pos = new Point3d();
      for (FemNode3d n : myFemMod.getNodes()) {
         n.setDynamic (true);
         n.setPosition (Point3d.ZERO);
         if (n.isDynamic()) {
            pos.setRandom();
            pos.normalize();
            pos.scale (1.0);
            n.setPosition (pos);
         }
      }
      myMechMod.setGravity (Vector3d.ZERO);
   }
   
}
