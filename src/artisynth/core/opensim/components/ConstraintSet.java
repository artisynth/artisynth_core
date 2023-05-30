package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ComponentList;

public class ConstraintSet extends SetBase<ConstraintBase> 
   implements ModelComponentGenerator<ComponentList<ModelComponent>> {
   
   @Override
   public ConstraintSet clone () {
      return (ConstraintSet)super.clone ();
   }

   @Override
   public ComponentList<ModelComponent> createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      String name = getName ();
      if (name == null) {
         name = "constraintset";
      }
      ComponentList<ModelComponent> constraints = new ComponentList<> (ModelComponent.class, name);
      
      for (ConstraintBase constraint : objects()) {
         
         ModelComponent fc = constraint.createComponent(geometryPath, componentMap);
         if (fc == null) {
            System.err.println ("Failed to create constraint " + constraint.getName ());
         } else {
            constraints.add (fc);
         }
         
      }
      return constraints;
   }
   
   

}
