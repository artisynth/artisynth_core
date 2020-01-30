package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class SpatialTransformFactory extends OpenSimObjectFactory<SpatialTransform> {

   protected SpatialTransformFactory () {
      super (SpatialTransform.class);
   }
   
   @Override
   protected boolean parseChild (SpatialTransform comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("TransformAxis".equals(name)) {
         OpenSimObjectFactory<? extends TransformAxis> factory = getFactory (TransformAxis.class);
         if (factory != null) {
            TransformAxis ta = factory.parse (child);
            comp.addTransformAxis (ta);
         } else {
            success = false;
         }
      } else {
         success = false;
      }
      
      return success;
   }

}
