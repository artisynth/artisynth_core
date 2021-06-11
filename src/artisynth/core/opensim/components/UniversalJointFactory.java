package artisynth.core.opensim.components;

public class UniversalJointFactory extends JointBaseFactory<UniversalJoint> {

   public UniversalJointFactory () {
      super (UniversalJoint.class);
   }
   
   protected UniversalJointFactory(Class<? extends UniversalJoint> jointClass) {
      super(jointClass);
   }

}
