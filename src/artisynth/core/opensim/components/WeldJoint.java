package artisynth.core.opensim.components;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SolidJoint;
import maspack.matrix.RigidTransform3d;

public class WeldJoint extends JointBase {

   @Override
   public WeldJoint clone () {
      return (WeldJoint)super.clone ();
   }

   @Override
   public artisynth.core.mechmodels.JointBase createJoint (RigidBody parent, RigidTransform3d TJP, 
      RigidBody child, RigidTransform3d TJC) {
      
      SolidJoint sj = new SolidJoint (parent, TJP, child, TJC);
      
      sj.setName (getName());
      return sj;
   }
   
}
