package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class BushingForceFactory
   extends BushingForceBaseFactory<BushingForce> {

   public BushingForceFactory() {
      super(BushingForce.class);
   }
   
   protected BushingForceFactory (
      Class<? extends BushingForce> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (BushingForce comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);

      switch (name) {
         case "translational_stiffness": {
            comp.setTranslationalStiffness (parseVector3dValue (child));
            break;
         }
         case "rotational_stiffness": {
            comp.setRotationalStiffness (parseVector3dValue (child));
            break;
         } 
         default: {
            success = super.parseChild (comp, child);
            break;
         }
      }
      
      return success;
   }

}
