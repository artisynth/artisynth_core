package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class DisplayGeometryFactory extends VisibleBaseFactory<DisplayGeometry> {

   public DisplayGeometryFactory () {
      super (DisplayGeometry.class);
   }
   
   protected DisplayGeometryFactory(Class<? extends DisplayGeometry> dgClass) {
      super(dgClass);
   }

   @Override
   protected boolean parseChild (DisplayGeometry comp, Element child) {
    
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("geometry_file".equals (name)) {
         comp.setGeometryFile (parseTextValue (child));
      } else if ("texture_file".equals (name)) {
         comp.setTextureFile (parseTextValue (child));
      } else if ("transform".equals (name)) {
         comp.setTransform (parseTransformValue(child));
      } else if ("scale_factors".equals (name)) {
         comp.setScaleFactors (parseVector3dValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
