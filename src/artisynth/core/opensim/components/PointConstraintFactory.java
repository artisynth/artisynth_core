package artisynth.core.opensim.components;

import maspack.matrix.Point3d;
import org.w3c.dom.Element;

public class PointConstraintFactory
   extends ConstraintBaseFactory<PointConstraint>{
   
   public PointConstraintFactory () {
      super(PointConstraint.class);
   }
   
   protected PointConstraintFactory (
      Class<? extends PointConstraint> clfClass) {
      super(clfClass);
   }

   @Override
   protected boolean parseChild (PointConstraint comp, Element child) {

      boolean success = true;
      
      String name = getNodeName (child);

      if ("body_1".equals (name)) {
         comp.setBody1 (parseTextValue (child));
      }
      else if ("socket_body_1".equals (name)) {
         comp.setSocketBody1 (parseTextValue (child));
      }
      else if ("location_body_1".equals (name)) {
         comp.setLocationBody1 (parsePoint3dValue (child));
      }
      else if ("body_2".equals (name)) {
         comp.setBody2 (parseTextValue (child));
      }
      else if ("socket_body_2".equals (name)) {
         comp.setSocketBody2 (parseTextValue (child));
      }
      else if ("location_body_2".equals (name)) {
         comp.setLocationBody2 (parsePoint3dValue (child));
      }
      else {
         success = super.parseChild (comp, child);
      }
      return success;
   }
   
}
