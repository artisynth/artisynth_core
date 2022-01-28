package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.PointSkinAttachment.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class FemSkinCollide extends FemSkinTest {

   public static boolean omitFromMenu = false;

   SkinMeshBody mySkin = null;
   RenderProps mySkinProps;

   public void build (String[] args) {
      super.build (args);

      // get components from the super class
      MechModel mech = (MechModel)models().get("mech");
      SkinMeshBody skinBody = (SkinMeshBody)mech.meshBodies().get("skin");
      FemModel3d fem0 = (FemModel3d)mech.models().get("fem0");

      // make sure all FEM nodes are dynamic so it can fall freely
      for (FemNode3d n : fem0.getNodes()) {
         n.setDynamic (true);
      }

      // create a cylinder for the skin body to collide with
      RigidBody cylinder =
         RigidBody.createCylinder (
            "cylinder", 0.5, 2.0, /*density=*/1000.0, /*nsides=*/50);
      cylinder.setDynamic (false);
      cylinder.setPose (
         new RigidTransform3d (-0.5, 0, -1.0, 0, 0, Math.PI/2));
      mech.addRigidBody (cylinder);

      // enable collisions between the cylinder and the skin body
      CollisionBehavior cb = new CollisionBehavior (true, 0);
      mech.setCollisionBehavior (cylinder, skinBody, cb);
   }

   public void render (Renderer r, int flags) {
      super.render (r, flags);
   }


}
