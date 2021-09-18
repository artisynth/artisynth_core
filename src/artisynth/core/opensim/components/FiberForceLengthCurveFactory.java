package artisynth.core.opensim.components;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class FiberForceLengthCurveFactory
   extends OpenSimObjectFactory<FiberForceLengthCurve> {

   public FiberForceLengthCurveFactory () {
      super (FiberForceLengthCurve.class);
   }
   
   protected FiberForceLengthCurveFactory(
      Class<? extends FiberForceLengthCurve> curveClass) {
      super(curveClass);
   }
   
   @Override
   protected boolean parseChild (FiberForceLengthCurve comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);

      if ("strain_at_zero_force".equals(name)) {
         comp.strainAtZeroForce  = parseDoubleValue(child);
      }
      else if ("strain_at_one_norm_force".equals(name)) {
         comp.strainAtOneNormForce = parseDoubleValue(child);
      }
      else if ("stiffness_at_low_force".equals(name)) {
         comp.stiffnessAtLowForce = parseDoubleValue(child);
      }
      else if ("stiffness_at_one_norm_force".equals(name)) {
         comp.stiffnessAtOneNormForce = parseDoubleValue(child);
      }
      else if ("default_curviness".equals(name)) {
         comp.curviness = parseDoubleValue(child);
      }
      else {
         success = super.parseChild (comp, child);
      }
         
      return success;
   }
   
}
