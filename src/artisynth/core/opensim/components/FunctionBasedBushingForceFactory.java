package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class FunctionBasedBushingForceFactory
   extends BushingForceBaseFactory<FunctionBasedBushingForce> {

   public FunctionBasedBushingForceFactory() {
      super(FunctionBasedBushingForce.class);
   }
   
   protected FunctionBasedBushingForceFactory (
      Class<? extends FunctionBasedBushingForce> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (FunctionBasedBushingForce comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);

      switch (name) {
         case "m_x_theta_x_function": {
            comp.m_x_theta_x_function = parseFunctionValue (child);
            break;
         }
         case "m_y_theta_y_function": {
            comp.m_y_theta_y_function = parseFunctionValue (child);
            break;
         }
         case "m_z_theta_z_function": {
            comp.m_z_theta_z_function = parseFunctionValue (child);
            break;
         }
         case "f_x_delta_x_function": {
            comp.f_x_delta_x_function = parseFunctionValue (child);
            break;
         }
         case "f_y_delta_y_function": {
            comp.f_y_delta_y_function = parseFunctionValue (child);
            break;
         }
         case "f_z_delta_z_function": {
            comp.f_z_delta_z_function = parseFunctionValue (child);
            break;
         }
         case "visual_aspect_ratio":
         case "moment_visual_scale":
         case "force_visual_scale": {
            // not currently implemented
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
