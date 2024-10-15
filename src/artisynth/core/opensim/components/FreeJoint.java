package artisynth.core.opensim.components;

import java.util.ArrayList;

import artisynth.core.mechmodels.RigidBody;
import maspack.matrix.RigidTransform3d;
import artisynth.core.mechmodels.GimbalJoint.AxisSet;

public class FreeJoint extends JointBase {

   @Override
   public FreeJoint clone () {
      return (FreeJoint)super.clone ();
   }

   @Override
   public artisynth.core.mechmodels.JointBase createJoint (
      RigidBody parent, RigidTransform3d TJP, 
      RigidBody child, RigidTransform3d TJC) {

      artisynth.core.mechmodels.FreeJoint joint =
         new artisynth.core.mechmodels.FreeJoint (AxisSet.XYZ);
      joint.setBodies (child, TJC, parent, TJP);
      
      setCoordinateRangesAndValues (joint);
      joint.setName (getName());
      return joint;
   }
   
}
