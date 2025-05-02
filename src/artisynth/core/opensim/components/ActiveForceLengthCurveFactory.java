package artisynth.core.opensim.components;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ActiveForceLengthCurveFactory
   extends OpenSimObjectFactory<ActiveForceLengthCurve> {

   public ActiveForceLengthCurveFactory () {
      super (ActiveForceLengthCurve.class);
   }
   
   protected ActiveForceLengthCurveFactory(
      Class<? extends ActiveForceLengthCurve> curveClass) {
      super(curveClass);
   }
   
   @Override
   protected boolean parseChild (ActiveForceLengthCurve comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);

      if ("min_norm_active_fiber_length".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.minActiveNormFiberLength = value;
         }
      }
      else if ("transition_norm_fiber_length".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.transitionNormFiberLength  = value;
         }
      }
      else if ("max_norm_active_fiber_length".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.maxActiveNormFiberLength = value;
         }
      }
      else if ("shallow_ascending_slope".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.shallowAscendingSlope = value;
         }
      }
      else if ("minimum_value".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.minimumValue = value;
         }
      }
      else {
         success = super.parseChild (comp, child);
      }
         
      return success;
   }
   
}
