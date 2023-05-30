package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;

public abstract class ConstraintBase extends OpenSimObject
   implements ModelComponentGenerator<ModelComponent> {

   private boolean isEnforced;

   protected ConstraintBase() {
      isEnforced = true;
   }
   
   public void setIsEnforced (boolean enable) {
      isEnforced = enable;
   }
   
   public boolean getIsEnforced() {
      return isEnforced;
   }
      
   @Override
   public ConstraintBase clone () {
      return (ConstraintBase)super.clone ();
   }

   public abstract ModelComponent createComponent (
      File geometryPath, ModelComponentMap componentMap);
   
}
