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
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.strainAtZeroForce  = value;
         }
      }
      else if ("strain_at_one_norm_force".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.strainAtOneNormForce = value;
         }
      }
      else if ("stiffness_at_low_force".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.stiffnessAtLowForce = value;
         }
      }
      else if ("stiffness_at_one_norm_force".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.stiffnessAtOneNormForce = value;
         }
      }
      else if ("curviness".equals(name) ||
               // not sure default curviness was ever used, but just in case
               "default_curviness".equals(name)) {
         Double value = parseDoubleValueIfPresent(child);
         if (value != null) {
            comp.curviness = value;
         }
      }
      else {
         success = super.parseChild (comp, child);
      }
         
      return success;
   }
   
}
