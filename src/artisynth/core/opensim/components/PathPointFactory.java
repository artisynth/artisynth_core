package artisynth.core.opensim.components;

public class PathPointFactory extends PathPointFactoryBase<PathPoint> {
   
   public PathPointFactory() {
      super(PathPoint.class);
   }
   
   protected PathPointFactory(Class<? extends PathPoint> ppClass) {
      super(ppClass);
   }

}
