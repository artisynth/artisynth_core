package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class CustomJointFactory extends JointBaseFactory<CustomJoint> {

   public CustomJointFactory() {
      super(CustomJoint.class);
   }
   
   protected CustomJointFactory(Class<? extends CustomJoint> jointClass) {
      super(jointClass);
   }
   
   @Override
   protected boolean parseChild (CustomJoint comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("SpatialTransform".equals(name)) {
         
         OpenSimObjectFactory<? extends SpatialTransform> factory = getFactory (SpatialTransform.class);
         if (factory != null) {
            SpatialTransform st = factory.parse (child);
            comp.setSpatialTransform (st);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
