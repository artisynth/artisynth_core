package artisynth.core.opensim.components;

public class PinJointFactory extends JointBaseFactory<PinJoint> {

   public PinJointFactory () {
      super (PinJoint.class);
   }
   
   protected PinJointFactory(Class<? extends PinJoint> jointClass) {
      super(jointClass);
   }

}
