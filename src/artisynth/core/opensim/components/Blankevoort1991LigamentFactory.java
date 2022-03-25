package artisynth.core.opensim.components;

import org.w3c.dom.Element;

import artisynth.core.opensim.components.ForceSpringBaseFactory;

public class Blankevoort1991LigamentFactory
   extends ForceSpringBaseFactory<Blankevoort1991Ligament> {
   
   public Blankevoort1991LigamentFactory() {
      super(Blankevoort1991Ligament.class);
   }
   
   protected Blankevoort1991LigamentFactory (Class<? extends Blankevoort1991Ligament> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (Blankevoort1991Ligament comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("linear_stiffness".equals (name)) {
         comp.setLinearStiffness (parseDoubleValue (child));
      } 
      else if ("transition_strain".equals(name)) {
         comp.setLigamentTransitionStrain (parseDoubleValue (child));
      }
      else if ("damping_coefficient".equals(name)) {
         comp.setNormalizedDamping (parseDoubleValue (child));
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
