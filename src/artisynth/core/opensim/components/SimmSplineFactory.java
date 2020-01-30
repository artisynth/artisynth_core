package artisynth.core.opensim.components;

public class SimmSplineFactory extends CubicSplineBaseFactory<SimmSpline> {

   public SimmSplineFactory () {
      super(SimmSpline.class);
   }
   
   protected SimmSplineFactory(Class<? extends SimmSpline> splineClass) {
      super(splineClass);
   }
   
}
