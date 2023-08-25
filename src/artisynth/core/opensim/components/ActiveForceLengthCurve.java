package artisynth.core.opensim.components;

import maspack.matrix.Vector3d;
import maspack.interpolation.CubicHermiteSpline1d;
import artisynth.core.materials.Millard2012AxialMuscle;

public class ActiveForceLengthCurve extends OpenSimObject {

   static double DEFAULT_MIN_ACTIVE_NORM_FIBER_LENGTH = 0.4441;
   static double DEFAULT_TRANSITION_NORM_FIBER_LENGTH = 0.73;
   static double DEFAULT_MAX_ACTIVE_NORM_FIBER_LENGTH = 1.8123;
   static double DEFAULT_SHALLOW_ASCENDING_SLOPE = 0.8616;
   static double DEFAULT_MINIMUM_VALUE = 0.1;

   double minActiveNormFiberLength;
   double transitionNormFiberLength;
   double maxActiveNormFiberLength;
   double shallowAscendingSlope;
   double minimumValue;

   public ActiveForceLengthCurve() {
      minActiveNormFiberLength = DEFAULT_MIN_ACTIVE_NORM_FIBER_LENGTH;
      transitionNormFiberLength = DEFAULT_TRANSITION_NORM_FIBER_LENGTH;
      maxActiveNormFiberLength = DEFAULT_MAX_ACTIVE_NORM_FIBER_LENGTH;
      shallowAscendingSlope = DEFAULT_SHALLOW_ASCENDING_SLOPE;
      minimumValue = DEFAULT_MINIMUM_VALUE;
   }

   public CubicHermiteSpline1d createCurve() {
      return Millard2012AxialMuscle.createActiveForceLengthCurve (
         minActiveNormFiberLength,
         transitionNormFiberLength,
         maxActiveNormFiberLength,
         shallowAscendingSlope,
         minimumValue);
   }

   @Override
   public ActiveForceLengthCurve clone () {
      return (ActiveForceLengthCurve)super.clone ();
   }

}
