package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.mechmodels.ConstrainerBase;

public class ConstraintSet extends SetBase<ConstraintBase> 
   implements ModelComponentGenerator<ComponentList<ConstrainerBase>> {
   
   @Override
   public ConstraintSet clone () {
      return (ConstraintSet)super.clone ();
   }

   @Override
   public RenderableComponentList<ConstrainerBase> createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      String name = getName ();
      if (name == null) {
         name = "constraintset";
      }
      RenderableComponentList<ConstrainerBase> constraints = 
         new RenderableComponentList<> (ConstrainerBase.class, name);
      
      for (ConstraintBase constraint : objects()) {
         
         ModelComponent fc = constraint.createComponent(geometryPath, componentMap);
         if (fc == null) {
            System.err.println (
               "OpenSimParser: failed to create constraint " +
               constraint.getName ());
         } else {
            constraints.add ((ConstrainerBase)fc);
         }
         
      }
      return constraints;
   }
   
   

}
