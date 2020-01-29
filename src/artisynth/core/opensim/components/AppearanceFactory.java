package artisynth.core.opensim.components;

import java.awt.Color;

import org.w3c.dom.Element;

public class AppearanceFactory extends OpenSimObjectFactory<Appearance> {

   protected AppearanceFactory (Class<? extends Appearance> instanceClass) {
      super (instanceClass);
   }
   
   public AppearanceFactory() {
      super(Appearance.class);
   }

   @Override
   protected boolean parseChild (Appearance comp, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      
      if ("visible".equals(cname)) {
         comp.setVisible (parseBooleanValue (child));
      } else if ("opacity".equals(cname)) {
         comp.setOpacity (parseDoubleValue(child));
      } else if ("color".equals(cname)) {
         double[] color = parseDoubleArrayValue(child); 
         comp.setColor (new Color((float)color[0], (float)color[1], (float)color[2])); 
      } else if ("SurfaceProperties".equals(cname)) {
         OpenSimObjectFactory<? extends SurfaceProperties> vof = getFactory (SurfaceProperties.class);
         if (vof != null) {
            SurfaceProperties vo = vof.parse(child);
            comp.setSurfaceProperties (vo);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
   
}
