package artisynth.core.opensim.components;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SolidJoint;

public class WeldJoint extends JointBase {

   @Override
   public WeldJoint clone () {
      return (WeldJoint)super.clone ();
   }

   @Override
   public artisynth.core.mechmodels.JointBase createJoint (RigidBody parent, RigidBody child) {
      
      SolidJoint sj = null;
      
      if (getReverse ()) {
         sj = new SolidJoint (child, getJointTransformInChild(), parent, getJointTransformInParent());
      } else {
         sj = new SolidJoint (parent, getJointTransformInParent(), child, getJointTransformInChild());
      }
      sj.setName (getName());
      return sj;
   }
   
}
