package artisynth.demos.tutorial;

import java.io.IOException;

import maspack.matrix.Vector3d;
import artisynth.core.mechmodels.FullPlanarJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;

public class FullPlanarJointDemo extends LumbarFrameSpring {

   public FullPlanarJointDemo () {
      super();
   }

   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      MechModel mech = (MechModel)models ().get ("mech");
      RigidBody lumbar1 = mech.rigidBodies ().get ("lumbar1");
      
      // constrain to mid-sagittal plane: medio-lateral direction is in world y-axis
      FullPlanarJoint planarJoint = new FullPlanarJoint (lumbar1, Vector3d.Y_UNIT);
      mech.addRigidBodyConnector (planarJoint);
   }

}
