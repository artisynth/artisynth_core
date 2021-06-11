package artisynth.core.opensim.components;

public class BallJointFactory extends JointBaseFactory<BallJoint> {

   public BallJointFactory () {
      super (BallJoint.class);
   }
   
   protected BallJointFactory(Class<? extends BallJoint> jointClass) {
      super(jointClass);
   }

}
