package artisynth.core.opensim.components;

import java.awt.Color;

import org.w3c.dom.Element;

import artisynth.core.opensim.components.VisibleBase.DisplayPreference;

public abstract class VisibleBaseFactory<E extends VisibleBase> extends OpenSimObjectFactory<E> {

   protected VisibleBaseFactory (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E comp, Element child) {
    
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("color".equals (name)) {
         double dvals[] = parseDoubleArrayValue(child);
         comp.setColor (new Color((float)dvals[0], (float)dvals[1], (float)dvals[2]));
      } else if ("display_preference".equals (name)) {
         int id = parseIntegerValue (child);
         comp.setDisplayPreference (DisplayPreference.get (id));
      } else if ("opacity".equalsIgnoreCase (name)) {
         comp.setOpacity (parseDoubleValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
