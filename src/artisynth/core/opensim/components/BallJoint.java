package artisynth.core.opensim.components;

import java.util.ArrayList;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.opensim.components.Coordinate.MotionType;
import artisynth.core.mechmodels.GimbalJoint.AxisSet;
import maspack.matrix.RigidTransform3d;

public class BallJoint extends JointBase {

   @Override
   public BallJoint clone () {
      return (BallJoint)super.clone ();
   }

   @Override
   public artisynth.core.mechmodels.JointBase createJoint (
      RigidBody parent, RigidTransform3d TJP, 
      RigidBody child, RigidTransform3d TJC) {

      artisynth.core.mechmodels.GimbalJoint joint =
         new artisynth.core.mechmodels.GimbalJoint (AxisSet.XYZ);
      joint.setBodies (child, TJC, parent, TJP);
      
      setCoordinateRangesAndValues (joint);
      joint.setName (getName());
      return joint;
   }
   
}
