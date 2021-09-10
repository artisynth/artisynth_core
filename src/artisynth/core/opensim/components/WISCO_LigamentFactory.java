package artisynth.core.opensim.components;

import org.w3c.dom.Element;

import artisynth.core.opensim.components.ForceSpringBaseFactory;

public class WISCO_LigamentFactory
   extends ForceSpringBaseFactory<WISCO_Ligament> {
   
   public WISCO_LigamentFactory() {
      super(WISCO_Ligament.class);
   }
   
   protected WISCO_LigamentFactory (Class<? extends WISCO_Ligament> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (WISCO_Ligament comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("linear_stiffness".equals (name)) {
         comp.setLinearStiffness (parseDoubleValue (child));
      } 
      else if ("reference_strain".equals(name)) {
         comp.setReferenceStrain (parseDoubleValue (child));
      } 
      else if ("ligament_transition_stiffenss".equals(name)) {
         comp.setLigamentTransitionStrain (parseDoubleValue (child));
      }
      else if ("normalized_damping".equals(name)) {
         comp.setNormalizedDamping (parseDoubleValue (child));
      } 
      else if ("max_force".equals(name)) {
         comp.setMaxForce (parseDoubleValue (child));
      } 
      else if ("slack_length".equals(name)) {
         comp.setTendonSlackLength (parseDoubleValue (child));
      } 
      else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
}
