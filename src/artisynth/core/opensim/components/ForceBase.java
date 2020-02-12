package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;

public abstract class ForceBase extends HasVisibleObject implements ModelComponentGenerator<ModelComponent> {
   
   // private boolean isDisabled;    // "Flag indicating whether the force is disabled or not. Disabled means that the force is not active in subsequent dynamics realizations."
   private boolean appliesForce;     // now called appliesForce (OpenSim 4.0)
   
   protected ForceBase() {
      appliesForce = true;
   }
   
   public boolean isDisabled() {
      return !appliesForce;
   }
   
   public void setDisabled(boolean set) {
      appliesForce = !set;
   }
   
   public boolean getAppliesForce() {
      return appliesForce;
   }
   
   public void setAppliesForce(boolean set) {
      appliesForce = set;
   }
   
   @Override
   public ForceBase clone () {
      return (ForceBase)super.clone ();
   }

   public abstract ModelComponent createComponent (File geometryPath, ModelComponentMap componentMap);
   
}
