package artisynth.core.materials;

import maspack.interpolation.CubicHermiteSpline1d;

/**
 * @deprecated This class has been replaced by the more appropriately named
 * {@link Millard2012AxialTendon}, which should be used instead.
 */
public class Millard2012AxialLigament extends Millard2012AxialTendon {

   public Millard2012AxialLigament() {
      super();
   }

   public Millard2012AxialLigament (
      double maxIsoForce, double tendonSlackLen) {
      super (maxIsoForce, tendonSlackLen);
   }

   public static CubicHermiteSpline1d getDefaultTendonForceLengthCurve() {
      return Millard2012AxialTendon.getDefaultTendonForceLengthCurve();
   }
}
