package artisynth.core.opensim.components;

public class GimbalJointFactory extends JointBaseFactory<GimbalJoint> {

   public GimbalJointFactory () {
      super (GimbalJoint.class);
   }
   
   protected GimbalJointFactory(Class<? extends GimbalJoint> jointClass) {
      super(jointClass);
   }

}
