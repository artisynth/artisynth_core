package artisynth.models.testspace.template;

import artisynth.core.workspace.RootModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechModel;

public class SimpleBall extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      RigidBody ball = RigidBody.createSphere (
         "ball", /*radius*/.1, /*density*/1000, /*nslices*/32);
      mech.addRigidBody (ball);
   }
}
