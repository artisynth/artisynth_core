package artisynth.demos.fem;

import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.util.RandomGenerator;

import java.util.*;

public class RobustCube extends FemBeam3d {

   public void build (String[] args) {

      build ("hex", 1.0, 1.0, 5, 5, /*options=*/0);

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
      myMechMod.setDynamicsEnabled (false);         
   }

    public StepAdjustment advance (double t0, double t1, int flags) {
       if (t1 == 0.5) {
          myMechMod.setDynamicsEnabled (true);
       }
       return super.advance (t0, t1, flags);
    }
}
