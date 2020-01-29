package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class JointFactory extends OpenSimObjectFactory<Joint> {

   public JointFactory() {
      super (Joint.class);
   }
   
   protected JointFactory(Class<? extends Joint> jointClass) {
      super(jointClass);
   }
   
   @Override
   protected boolean parseChild (Joint comp, Element child) {
      
      // standard children
      boolean success = super.parseChild (comp, child);

      // remaining children must be of type JointBase
      if (!success) {
         success = true;
         OpenSimObjectFactory<? extends JointBase> factory = 
            findFactory (JointBase.class, child);
         
         if (factory != null) {
            JointBase joint = factory.parse (child);
            comp.setJoint (joint);
         } else {
            success = false;
         }
      }
      
      return success;
   }
   
}
