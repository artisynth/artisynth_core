package artisynth.core.opensim.components;

public class NaturalCubicSplineFactory extends CubicSplineBaseFactory<NaturalCubicSpline> {

   public NaturalCubicSplineFactory () {
      super (NaturalCubicSpline.class);
   }
   
   protected NaturalCubicSplineFactory(Class<? extends NaturalCubicSpline> splineClass) {
      super(splineClass);
   }
   
}
 