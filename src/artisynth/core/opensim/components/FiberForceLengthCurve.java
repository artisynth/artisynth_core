package artisynth.core.opensim.components;

import maspack.matrix.Vector3d;
import maspack.interpolation.CubicHermiteSpline1d;
import artisynth.core.materials.AxialMuscleMaterialBase;

public class FiberForceLengthCurve extends OpenSimObject {

   static double DEFAULT_STRAIN_AT_ZERO_FORCE = 0.0;
   static double DEFAULT_STRAIN_AT_ONE_NORM_FORCE = 0.7;
   static double DEFAULT_STIFFNESS_AT_LOW_FORCE = 0.2;
   static double DEFAULT_STIFFNESS_AT_ONE_NORM_FORCE =
      2.0/(DEFAULT_STRAIN_AT_ONE_NORM_FORCE-DEFAULT_STRAIN_AT_ZERO_FORCE);
   static double DEFAULT_CURVINESS = 0.75;

   double strainAtZeroForce;
   double strainAtOneNormForce;
   double stiffnessAtLowForce;
   double stiffnessAtOneNormForce; 
   double curviness;

   public FiberForceLengthCurve() {
      strainAtZeroForce = DEFAULT_STRAIN_AT_ZERO_FORCE;
      strainAtOneNormForce = DEFAULT_STRAIN_AT_ONE_NORM_FORCE;
      stiffnessAtLowForce = DEFAULT_STIFFNESS_AT_LOW_FORCE;
      stiffnessAtOneNormForce = DEFAULT_STIFFNESS_AT_ONE_NORM_FORCE; 
      curviness = DEFAULT_CURVINESS;
   }

   public CubicHermiteSpline1d createCurve() {
      return AxialMuscleMaterialBase.createPassiveForceLengthCurve (
         strainAtZeroForce,
         strainAtOneNormForce,
         stiffnessAtLowForce,
         stiffnessAtOneNormForce, 
         curviness);
   }

   @Override
   public FiberForceLengthCurve clone () {
      return (FiberForceLengthCurve)super.clone ();
   }

}
