package artisynth.demos.fem;

import java.awt.Color;

import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.util.*;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;

public class Hex3dBlock extends Fem3dBlock {

   public void build (String[] args) {
      build ("hex", 3, 1, 0);
      //super (name, "hex", 1, 1, 0);
      MechModel mechMod = (MechModel)findComponent ("models/mech");
      //mechMod.setDefaultCollisionBehavior (true, 0);

      // FemModel3d fem = 
      //    (FemModel3d)mechMod.findComponent ("models/fem");
      // FemMarker mkr = fem.addMarker (new Point3d(.23, -0.02, -0.03));
      // Particle part = new Particle ("part", 1, 0.03, 0.01, -0.02);
      // RenderProps.setSphericalPoints (part, 0.015, Color.RED);
      // RenderProps.setSphericalPoints (mkr, 0.015, Color.GREEN);
      // mechMod.addParticle (part);
      // mechMod.attachPoint (part, fem);

      // RevoluteJoint joint =
      //    (RevoluteJoint)mechMod.findComponent ("bodyConnectors/0");
      // RigidBody block =
      //    (RigidBody)mechMod.findComponent ("rigidBodies/leftBody");

      //  TransformGeometryContext.transform (
      //     joint, new RigidTransform3d (0, 0, -0.15), 0);

       // TransformGeometryContext.transform (
       //    block, new RigidTransform3d (0, 0, -0.15), 0);
      // TransformGeometryContext.transform (
      //    new TransformableGeometry[] {joint, block},
      //    new RigidTransform3d (0, 0, 0.15), 0);
      
   }
}
