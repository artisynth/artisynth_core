package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;

public abstract class ForceBase extends HasVisibleObject implements ModelComponentGenerator<ModelComponent> {
   
   private boolean isDisabled; // "Flag indicating whether the force is disabled or not. Disabled means"" that the force is not active in subsequent dynamics realizations."
  
   protected ForceBase() {
      isDisabled = false;
   }
   
   public boolean isDisabled() {
      return isDisabled;
   }
   
   public void setDisabled(boolean set) {
      isDisabled = set;
   }
   
   @Override
   public ForceBase clone () {
      return (ForceBase)super.clone ();
   }

   public abstract ModelComponent createComponent (File geometryPath, ModelComponentMap componentMap);
   
}
