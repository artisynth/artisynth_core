package artisynth.models.testspace;

import artisynth.core.workspace.RootModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechModel;

// All ArtiSynth models extend RootModel
public class SimpleBall extends RootModel {

   // build method assembles the model
   public void build (String[] args) {
      // create a MechModel to contain other mechanical components
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a spherical ball and add it to the MechModel
      RigidBody ball = RigidBody.createSphere (
         "ball", /*radius*/.1, /*density*/1000, /*nslices*/32);
      mech.addRigidBody (ball);
   }
}
