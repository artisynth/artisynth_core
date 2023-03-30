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
         comp.minActiveNormFiberLength = parseDoubleValue(child);
      }
      else if ("transition_norm_fiber_length".equals(name)) {
         comp.transitionNormFiberLength  = parseDoubleValue(child);
      }
      else if ("max_norm_active_fiber_length".equals(name)) {
         comp.maxActiveNormFiberLength = parseDoubleValue(child);
      }
      else if ("shallow_ascending_slope".equals(name)) {
         comp.shallowAscendingSlope = parseDoubleValue(child);
      }
      else if ("minimum_value".equals(name)) {
         comp.minimumValue = parseDoubleValue(child);
      }
      else {
         success = super.parseChild (comp, child);
      }
         
      return success;
   }
   
}
