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
         comp.strainAtOneNormForce = parseDoubleValue(child);
      }
      else if ("stiffness_at_one_norm_force".equals(name)) {
         comp.stiffnessAtOneNormForce = parseDoubleValue(child);
      }
      else if ("norm_force_at_toe_end".equals(name)) {
         comp.normForceAtToeEnd = parseDoubleValue(child);
      }
      else if ("curviness".equals(name)) {
         comp.curviness = parseDoubleValue(child);
      }
      else {
         success = super.parseChild (comp, child);
      }
         
      return success;
   }
   
}
