package artisynth.core.opensim.components;

import java.util.ArrayList;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.opensim.components.Coordinate.MotionType;
import artisynth.core.mechmodels.GimbalJoint.AxisSet;
import maspack.matrix.RigidTransform3d;

public class GimbalJoint extends JointBase {

   @Override
   public GimbalJoint clone () {
      return (GimbalJoint)super.clone ();
   }

   @Override
   public artisynth.core.mechmodels.JointBase createJoint (
      RigidBody parent, RigidTransform3d TJP, 
      RigidBody child, RigidTransform3d TJC) {

      artisynth.core.mechmodels.GimbalJoint joint =
         new artisynth.core.mechmodels.GimbalJoint (AxisSet.XYZ);
      joint.setBodies (
         child, getJointTransformInChild(),
         parent, getJointTransformInParent());
      
      setCoordinateRangesAndValues (joint);
      joint.setName (getName());
      return joint;
   }
   
}
