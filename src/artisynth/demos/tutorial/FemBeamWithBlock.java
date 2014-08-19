package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import maspack.render.*;
import maspack.matrix.*;

import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.workspace.RootModel;

public class FemBeamWithBlock extends FemBeam {

   public void build (String[] args) throws IOException {

      super.build (args);

      // create a block to add at the end
      RigidBody block = RigidBody.createBox (
         "block", width/2, 1.2*width, 1.2*width, 2*density);
      mech.addRigidBody (block);
      block.setPose (new RigidTransform3d (length/2+width/4, 0, 0));
      // attach the block to the FEM
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().x >= length/2-EPS) {
            mech.addAttachment (new PointFrameAttachment (block, n));
         }
      }
   }

}
