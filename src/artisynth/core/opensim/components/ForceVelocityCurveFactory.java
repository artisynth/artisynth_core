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
         comp.concentricSlopeAtVmax = parseDoubleValue(child);
      }
      else if ("concentric_slope_near_vmax".equals(name)) {
         comp.concentricSlopeNearVmax = parseDoubleValue(child);
      }
      else if ("isometric_slope".equals(name)) {
         comp.isometricSlope = parseDoubleValue(child);
      }
      else if ("eccentric_slope_at_vmax".equals(name)) {
         comp.eccentricSlopeAtVmax = parseDoubleValue(child);
      }
      else if ("eccentric_slope_near_vmax".equals(name)) {
         comp.eccentricSlopeNearVmax = parseDoubleValue(child);
      }
      else if ("max_eccentric_velocity_force_multiplier".equals(name)) {
         comp.maxEccentricVelocityForceMultiplier = parseDoubleValue(child);
      }
      else if ("concentric_curviness".equals(name)) {
         comp.concentricCurviness = parseDoubleValue(child);
      }
      else if ("eccentric_curviness".equals(name)) {
         comp.eccentricCurviness = parseDoubleValue(child);
      }
      else {
         success = super.parseChild (comp, child);
      }
         
      return success;
   }
   
}
