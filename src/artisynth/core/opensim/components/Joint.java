package artisynth.core.opensim.components;

import java.io.File;

public class Joint extends OpenSimObject implements ModelComponentGenerator<artisynth.core.mechmodels.JointBase>{
   
   private JointBase joint;
   
   public Joint() {
      joint = null;
   }
   
   public JointBase getJoint() {
      return joint;
   }
   
   public void setJoint(JointBase joint) {
      this.joint = joint;
      this.joint.setParent (this);
   }
   
   @Override
   public Joint clone () {
      Joint out = (Joint)super.clone ();
      if (joint != null) {
         out.setJoint (joint);
      }
      return out;
   }

   @Override
   public artisynth.core.mechmodels.JointBase createComponent (
      File geometryPath, ModelComponentMap componentMap) {
     
      JointBase joint = getJoint ();
      if (joint != null) {
         return joint.createComponent (geometryPath, componentMap);
      }
      return null;
   }

}
