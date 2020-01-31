package artisynth.demos.fem;

import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import maspack.matrix.*;
import maspack.util.RandomGenerator;

import java.util.*;

public class IncompressDemo extends FemBeam3d {

   public void build (String[] args) {

      build ("hex", 1.0, 1.0, 20, 10, /*options=*/0);

      myFemMod.setMaterial (new IncompNeoHookeanMaterial (10000, 1000000));

      RigidBody lbod = RigidBody.createBox ("lbod", 0.2, 1.2, 1.2, 1000);
      lbod.setPose (new RigidTransform3d (-0.6, 0, 0));
      lbod.setDynamic (false);
      myMechMod.addRigidBody (lbod);

      RigidBody rbod = RigidBody.createBox ("rbod", 0.2, 1.2, 1.2, 1000);
      rbod.setPose (new RigidTransform3d ( 0.6, 0, 0));
      rbod.setDynamic (false);
      myMechMod.addRigidBody (rbod);

      for (FemNode3d n : myFemMod.getNodes()) {
         double x = n.getPosition().x;
         if (x == 0.5) {
            myMechMod.attachPoint (n, rbod);
         }
         else if (x == -0.5) {
            myMechMod.attachPoint (n, lbod);
         }
      }

      NumericInputProbe iprobe = new NumericInputProbe (
         lbod, "targetPosition", 0, 5);
      iprobe.addData (new double[] {
            0, -0.6, 0, 0,
            5, -1.6, 0, 0
         }, NumericInputProbe.EXPLICIT_TIME);
      iprobe.setName ("left block x");
      addInputProbe (iprobe);

      iprobe = new NumericInputProbe (
         rbod, "targetPosition", 0, 5);
      iprobe.addData (new double[] {
            0,  0.6, 0, 0,
            5,  1.6, 0, 0
         }, NumericInputProbe.EXPLICIT_TIME);
            iprobe.setName ("right block x");
      addInputProbe (iprobe);

      NumericOutputProbe oprobe = new NumericOutputProbe (
         myFemMod, "volume", 0, 5, -1);
      oprobe.setName ("volume");
      oprobe.setAttachedFileName ("femVolume.txt");
      addOutputProbe (oprobe);

      for (int i=0; i<10; i++) {
         addWayPoint (0.5*i);
      }
      addBreakPoint (5.0);
      
      myMechMod.setGravity (Vector3d.ZERO);
   }
}
