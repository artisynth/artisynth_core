package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class ObjectGroupFactory extends OpenSimObjectFactory<ObjectGroup> {

   public ObjectGroupFactory () {
      super(ObjectGroup.class);
   }
   
   protected ObjectGroupFactory(Class<? extends ObjectGroup> obClass) {
      super(obClass);
   }
   
   @Override
   protected boolean parseChild (ObjectGroup comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("members".equals(name)) {
         // string array
         String[] members = parseTextArrayValue (child);
         if (members != null) {
            for (String member : members) {
               comp.addMember (member);
            }
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
