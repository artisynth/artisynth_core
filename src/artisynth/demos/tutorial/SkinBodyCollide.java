package artisynth.demos.tutorial;

import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import maspack.matrix.RigidTransform3d;

public class SkinBodyCollide extends AllBodySkinning {

   public void build (String[] args) {
      super.build (args);

      // get components from the super class
      MechModel mech = (MechModel)models().get("mech");
      SkinMeshBody skinBody = (SkinMeshBody)mech.meshBodies().get("skin");
      RigidBody block0 = mech.rigidBodies().get("block0");

      // set block0 dynamic so the skin body and its masters can
      // fall under gravity
      block0.setDynamic (true);
      
      // create a cylinder for the skin body to collide with
      RigidBody cylinder =
         RigidBody.createCylinder (
            "cylinder", 0.5, 2.0, /*density=*/1000.0, /*nsides=*/50);
      cylinder.setDynamic (false);
      cylinder.setPose (
         new RigidTransform3d (-0.5, 0, -1.5, 0, 0, Math.PI/2));
      mech.addRigidBody (cylinder);

      // enable collisions between the cylinder and the skin body
      CollisionBehavior cb = new CollisionBehavior (true, 0);
      mech.setCollisionBehavior (cylinder, skinBody, cb);
      mech.getCollisionManager().setReduceConstraints (true);
   }
}
