package artisynth.core.materials;

import maspack.properties.*;
import maspack.interpolation.CubicHermiteSpline1d;

public class Millard2012AxialTendon extends AxialTendonBase {

   protected static CubicHermiteSpline1d myDefaultTendonForceLengthCurve;   

   public Millard2012AxialTendon() {
      setTendonForceLengthCurve (getDefaultTendonForceLengthCurve());
   }

   public Millard2012AxialTendon (
      double maxIsoForce, double tendonSlackLen) {
      
      this();
      setMaxIsoForce (maxIsoForce);
      setTendonSlackLength (tendonSlackLen);
   }

   public static CubicHermiteSpline1d getDefaultTendonForceLengthCurve() {
      if (myDefaultTendonForceLengthCurve == null) {
         double strainAtOneNormForce = 0.049;
         myDefaultTendonForceLengthCurve =
            Millard2012AxialMuscle.createTendonForceLengthCurve (
               strainAtOneNormForce, 1.375/strainAtOneNormForce, 2.0/3.0, 0.5);
      }
      return myDefaultTendonForceLengthCurve;
   }

   /**
    * {@inheritDoc}
    */
   protected double computeTendonForce (double tln) {
      return myTendonForceLengthCurve.evalY (tln);
   }

   /**
    * {@inheritDoc}
    */
   protected double computeDTendonForce (double tln) {
      return myTendonForceLengthCurve.evalDy (tln);
   }

}
