package artisynth.core.opensim.components;

import maspack.matrix.Vector3d;
import maspack.interpolation.CubicHermiteSpline1d;
import artisynth.core.materials.AxialMuscleMaterialBase;

public class TendonForceLengthCurve extends OpenSimObject {

   static double DEFAULT_STRAIN_AT_ONE_NORM_FORCE = 0.049;
   static double DEFAULT_STIFFNESS_AT_ONE_NORM_FORCE =
      1.375/DEFAULT_STRAIN_AT_ONE_NORM_FORCE;
   static double DEFAULT_NORM_FORCE_AT_TOE_END = 2.0/3.0;
   static double DEFAULT_CURVINESS = 0.5;

   double strainAtOneNormForce;
   double stiffnessAtOneNormForce;
   double normForceAtToeEnd;
   double curviness;

   public TendonForceLengthCurve() {
      strainAtOneNormForce = DEFAULT_STRAIN_AT_ONE_NORM_FORCE;
      stiffnessAtOneNormForce = DEFAULT_STIFFNESS_AT_ONE_NORM_FORCE;
      normForceAtToeEnd = DEFAULT_NORM_FORCE_AT_TOE_END;
      curviness = DEFAULT_CURVINESS;
   }

   public CubicHermiteSpline1d createCurve() {
      return AxialMuscleMaterialBase.createTendonForceLengthCurve (
         strainAtOneNormForce,
         stiffnessAtOneNormForce,
         normForceAtToeEnd,
         curviness);
   }

   @Override
   public TendonForceLengthCurve clone () {
      return (TendonForceLengthCurve)super.clone ();
   }

}
