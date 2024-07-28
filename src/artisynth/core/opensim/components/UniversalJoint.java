package artisynth.core.opensim.components;

import java.util.ArrayList;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.opensim.components.Coordinate.MotionType;
import artisynth.core.mechmodels.UniversalJoint.AxisSet;
import maspack.matrix.RigidTransform3d;

public class UniversalJoint extends JointBase {

   @Override
   public UniversalJoint clone () {
      return (UniversalJoint)super.clone ();
   }

   @Override
   public artisynth.core.mechmodels.JointBase createJoint (
      RigidBody parent, RigidTransform3d TJP, 
      RigidBody child, RigidTransform3d TJC) {

      artisynth.core.mechmodels.UniversalJoint joint =
         new artisynth.core.mechmodels.UniversalJoint (AxisSet.XY);
      joint.setBodies (child, TJC, parent, TJP);
      
      setCoordinateRangesAndValues (joint);
      joint.setName (getName());
      return joint;
   }
   
}
