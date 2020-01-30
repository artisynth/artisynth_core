package artisynth.core.opensim.components;

public class WeldJointFactory extends JointBaseFactory<WeldJoint> {

   public WeldJointFactory () {
      super (WeldJoint.class);
   }
   
   protected WeldJointFactory(Class<? extends WeldJoint> jointClass) {
      super(jointClass);
   }

}
