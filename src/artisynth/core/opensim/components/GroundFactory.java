package artisynth.core.opensim.components;

public class GroundFactory extends PhysicalFrameFactory<Ground> {
   
   public GroundFactory() {
      super(Ground.class);
   }
   
   protected GroundFactory(Class<? extends Ground> instanceClass) {
      super(instanceClass);
   }
      
}
