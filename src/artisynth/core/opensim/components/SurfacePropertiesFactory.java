package artisynth.core.opensim.components;

import org.w3c.dom.Element;

import artisynth.core.opensim.components.SurfaceProperties.Representation;

public class SurfacePropertiesFactory extends OpenSimObjectFactory<SurfaceProperties> {

   protected SurfacePropertiesFactory (Class<? extends SurfaceProperties> instanceClass) {
      super (instanceClass);
   }
   
   public SurfacePropertiesFactory() {
      super(SurfaceProperties.class);
   }

   @Override
   protected boolean parseChild (SurfaceProperties comp, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      
      if ("texture".equals(cname)) {
         comp.setTexture (parseTextValue (child));
      } else if ("representation".equals(cname)) {
         comp.setRepresentation (Representation.get(parseIntegerValue(child)));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
   
}
