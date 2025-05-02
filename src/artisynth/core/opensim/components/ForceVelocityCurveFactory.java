package artisynth.core.opensim.components;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ForceVelocityCurveFactory
   extends OpenSimObjectFactory<ForceVelocityCurve> {

   public ForceVelocityCurveFactory () {
      super (ForceVelocityCurve.class);
   }
   
   protected ForceVelocityCurveFactory(
      Class<? extends ForceVelocityCurve> curveClass) {
      super(curveClass);
   }
   
   @Override
   protected boolean parseChild (ForceVelocityCurve comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);

      if ("concentric_slope_at_vmax".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.concentricSlopeAtVmax = value;
         }
      }
      else if ("concentric_slope_near_vmax".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.concentricSlopeNearVmax = value;
         }
      }
      else if ("isometric_slope".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.isometricSlope = value;
         }
      }
      else if ("eccentric_slope_at_vmax".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.eccentricSlopeAtVmax = value;
         }
      }
      else if ("eccentric_slope_near_vmax".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.eccentricSlopeNearVmax = value;
         }
      }
      else if ("max_eccentric_velocity_force_multiplier".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.maxEccentricVelocityForceMultiplier = value;
         }
      }
      else if ("concentric_curviness".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.concentricCurviness = value;
         }
      }
      else if ("eccentric_curviness".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.eccentricCurviness = value;
         }
      }
      else {
         success = super.parseChild (comp, child);
      }
         
      return success;
   }
   
}
