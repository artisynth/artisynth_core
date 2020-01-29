package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class ConditionalPathPointFactory extends PathPointFactoryBase<ConditionalPathPoint>{
   
   public ConditionalPathPointFactory () {
      super(ConditionalPathPoint.class);
   }
   
   protected ConditionalPathPointFactory(Class<? extends ConditionalPathPoint> cppClass) {
      super(cppClass);
   }

   @Override
   protected boolean parseChild (ConditionalPathPoint comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("range".equals(name)) {
         double[] range = parseDoubleArrayValue (child);
         comp.setRange (range[0], range[1]);
      } else if ("coordinate".equals(name)) {
         comp.setCoordinate (parseTextValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
