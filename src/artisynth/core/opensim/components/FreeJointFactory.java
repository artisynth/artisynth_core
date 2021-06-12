package artisynth.core.opensim.components;

public class FreeJointFactory extends JointBaseFactory<FreeJoint> {

   public FreeJointFactory () {
      super (FreeJoint.class);
   }
   
   protected FreeJointFactory(Class<? extends FreeJoint> jointClass) {
      super(jointClass);
   }

}
