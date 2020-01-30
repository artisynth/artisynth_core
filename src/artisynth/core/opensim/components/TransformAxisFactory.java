package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class TransformAxisFactory extends OpenSimObjectFactory<TransformAxis> {

   public TransformAxisFactory () {
      super (TransformAxis.class);
   }
   
   protected TransformAxisFactory(Class<? extends TransformAxis> taClass) {
      super(taClass);
   }
   
   @Override
   protected boolean parseChild (TransformAxis comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("axis".equals(name)) {
         comp.setAxis (parseVector3dValue (child));
      } else if ("coordinates".equals(name)) {
         comp.setCoordinates (parseTextArrayValue (child));
      } else if ("function".equals (name)) {
         FunctionBase func = parseFunctionValue (child);
         if (func != null) {
            comp.setFunction (func);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
