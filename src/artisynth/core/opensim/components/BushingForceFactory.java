package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class BushingForceFactory extends ForceBaseFactory<BushingForce> {

   public BushingForceFactory() {
      super(BushingForce.class);
   }
   
   protected BushingForceFactory (Class<? extends BushingForce> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (BushingForce comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);

      // OpenSim 4
      if ("socket_frame1".equals(name)) {
         comp.setSocketFrame1 (parseTextValue (child));
      }
      else if ("socket_frame2".equals(name)) {
         comp.setSocketFrame2 (parseTextValue (child));
      }
      else if ("frames".equals(name)) {
         OpenSimObjectFactory<? extends FrameList> factory =
            getFactory (FrameList.class);
         if (factory != null) {
            FrameList frms = factory.parse (child);
            comp.setFrames (frms);
         } else {
            success = false;
         }
      }
      // OpenSim 3
      else if ("body_1".equals(name)) {
         comp.setBody1 (parseTextValue (child));
      }
      else if ("location_body_1".equals(name)) {
         comp.setLocationBody1 (parsePoint3dValue(child));
      }
      else if ("orientation_body_1".equals(name)) {
         comp.setOrientationBody1 (parseOrientationValue (child));
      }
      else if ("body_2".equals(name)) {
         comp.setBody2 (parseTextValue (child));
      }
      else if ("location_body_2".equals(name)) {
         comp.setLocationBody2 (parsePoint3dValue(child));
      }
      else if ("orientation_body_2".equals(name)) {
         comp.setOrientationBody1 (parseOrientationValue (child));
      }
      // generic
      else if ("translational_stiffness".equals(name)) {
         comp.setTranslationalStiffness (parseVector3dValue (child));
      }
      else if ("translational_damping".equals(name)) {
         comp.setTranslationalDamping (parseVector3dValue (child));
      }
      else if ("rotational_stiffness".equals(name)) {
         comp.setRotationalStiffness (parseVector3dValue (child));
      } 
      else if ("rotational_damping".equals(name)) {
         comp.setRotationalDamping (parseVector3dValue (child));
      }
      else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
