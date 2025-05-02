package artisynth.core.opensim.components;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TendonForceLengthCurveFactory
   extends OpenSimObjectFactory<TendonForceLengthCurve> {

   public TendonForceLengthCurveFactory () {
      super (TendonForceLengthCurve.class);
   }
   
   protected TendonForceLengthCurveFactory(
      Class<? extends TendonForceLengthCurve> curveClass) {
      super(curveClass);
   }
   
   @Override
   protected boolean parseChild (TendonForceLengthCurve comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);

      if ("strain_at_one_norm_force".equals(name)) {
         Double value = parseDoubleValueIfPresent(child); 
         if (value != null) {
            comp.strainAtOneNormForce = value;
         }
      }
      else if ("stiffness_at_one_norm_force".equals(name)) {
         Double value = parseDoubleValueIfPresent(child); 
         if (value != null) {
            comp.stiffnessAtOneNormForce = value;
         }
      }
      else if ("norm_force_at_toe_end".equals(name)) {
         Double value = parseDoubleValueIfPresent(child); 
         if (value != null) {
            comp.normForceAtToeEnd = value;
         }
      }
      else if ("curviness".equals(name)) {
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
