package artisynth.core.opensim.components;

public class SimmSpline extends NaturalCubicSpline {

   public SimmSpline() {
      super();
   }
   
   public SimmSpline (double[] x, double[] y) {
      super (x, y);
   }

   @Override
   public SimmSpline clone () {
      return (SimmSpline)super.clone ();
   }
   
}
