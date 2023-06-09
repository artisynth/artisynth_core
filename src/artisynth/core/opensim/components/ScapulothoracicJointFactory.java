package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class ScapulothoracicJointFactory extends JointBaseFactory<ScapulothoracicJoint> {

   public ScapulothoracicJointFactory() {
      super(ScapulothoracicJoint.class);
   }
   
   protected ScapulothoracicJointFactory(Class<? extends ScapulothoracicJoint> jointClass) {
      super(jointClass);
   }
   
   @Override
   protected boolean parseChild (ScapulothoracicJoint comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("thoracic_ellipsoid_radii_x_y_z".equals (name)) {
         comp.setThoracicEllipsoidRadii (parseVector3dValue (child));
      }
      else if ("scapula_winging_axis_origin".equals (name)) {
         double[] origin = parseDoubleArrayValue (child);
         comp.setScapulaWingingAxisOrigin (origin[0], origin[1]);
      }
      else if ("scapula_winging_axis_direction".equals (name)) {
         double direction = parseDoubleValue (child);
         comp.setScapulaWingingAxisDirection (direction);
      }
      else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
