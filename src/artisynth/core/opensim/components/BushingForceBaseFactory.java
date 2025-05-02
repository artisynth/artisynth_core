package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class BushingForceBaseFactory<E extends BushingForceBase>
   extends ForceBaseFactory<E> {

   protected BushingForceBaseFactory (
      Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);

      switch (name) {
         // OpenSim 4
         case "socket_frame1": {
            comp.setSocketFrame1 (parseTextValue (child));
            break;
         }
         case "socket_frame2": {
            comp.setSocketFrame2 (parseTextValue (child));
            break;
         }
         case "frames": {
            OpenSimObjectFactory<? extends FrameList> factory =
               getFactory (FrameList.class);
            if (factory != null) {
               FrameList frms = factory.parse (child);
               comp.setFrames (frms);
            } else {
               success = false;
            }
            break;
         }
         // OpenSim 3
         case "body_1": {
            comp.setBody1 (parseTextValue (child));
            break;
         }
         case "location_body_1": {
            comp.setLocationBody1 (parsePoint3dValue(child));
            break;
         }
         case "orientation_body_1": {
            comp.setOrientationBody1 (parseOrientationValue (child));
            break;
         }
         case "body_2": {
            comp.setBody2 (parseTextValue (child));
            break;
         }
         case "location_body_2": {
            comp.setLocationBody2 (parsePoint3dValue(child));
            break;
         }
         case "orientation_body_2": {
            comp.setOrientationBody1 (parseOrientationValue (child));
            break;
         }
         // generic
         case "translational_damping": {
            comp.setTranslationalDamping (parseVector3dValue (child));
            break;
         }
         case "rotational_damping": {
            comp.setRotationalDamping (parseVector3dValue (child));
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
