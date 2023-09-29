package artisynth.core.femmodels;

/**
 * Class containing a 3D element and an associatied numeric value.
 */
public class Element3dValuePair {
   public FemElement3dBase element;
   public double value;

   public Element3dValuePair (FemElement3dBase e, double v) {
      element = e;
      value = v;
   }
}
