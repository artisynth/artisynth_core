package artisynth.core.opensim.components;

import maspack.matrix.Vector3d;
import maspack.interpolation.CubicHermiteSpline1d;
import artisynth.core.materials.Millard2012AxialMuscle;

public class ForceVelocityCurve extends OpenSimObject {

   static double DEFAULT_CONCENTRIC_SLOPE_AT_VMAX = 0.0;
   static double DEFAULT_CONCENTRIC_SLOPE_NEAR_VMAX = 0.25;
   static double DEFAULT_ISOMETRIC_SLOPE = 5.0;
   static double DEFAULT_ECCENTRIC_SLOPE_AT_VMAX = 0.0; 
   static double DEFAULT_ECCENTRIC_SLOPE_NEAR_VMAX = 0.15;
   static double DEFAULT_MAX_ECCENTRIC_VELOCITY_FORCE_MULTIPLIER = 1.4;
   static double DEFAULT_CONCENTRIC_CURVINESS = 0.6;
   static double DEFAULT_ECCENTRIC_CURVINESS = 0.9;

   double concentricSlopeAtVmax;
   double concentricSlopeNearVmax;
   double isometricSlope;
   double eccentricSlopeAtVmax; 
   double eccentricSlopeNearVmax;
   double maxEccentricVelocityForceMultiplier;
   double concentricCurviness;
   double eccentricCurviness;

   public ForceVelocityCurve() {
      concentricSlopeAtVmax = DEFAULT_CONCENTRIC_SLOPE_AT_VMAX;
      concentricSlopeNearVmax = DEFAULT_CONCENTRIC_SLOPE_NEAR_VMAX;
      isometricSlope = DEFAULT_ISOMETRIC_SLOPE;
      eccentricSlopeAtVmax =  DEFAULT_ECCENTRIC_SLOPE_AT_VMAX; 
      eccentricSlopeNearVmax = DEFAULT_ECCENTRIC_SLOPE_NEAR_VMAX;
      maxEccentricVelocityForceMultiplier =
         DEFAULT_MAX_ECCENTRIC_VELOCITY_FORCE_MULTIPLIER;
      concentricCurviness = DEFAULT_CONCENTRIC_CURVINESS;
      eccentricCurviness = DEFAULT_ECCENTRIC_CURVINESS;
   }

   public CubicHermiteSpline1d createCurve() {
      return Millard2012AxialMuscle.createForceVelocityCurve (
         concentricSlopeAtVmax,
         concentricSlopeNearVmax,
         isometricSlope,
         eccentricSlopeAtVmax, 
         eccentricSlopeNearVmax,
         maxEccentricVelocityForceMultiplier,
         concentricCurviness,
         eccentricCurviness);
   }

   @Override
   public ForceVelocityCurve clone () {
      return (ForceVelocityCurve)super.clone ();
   }


}
