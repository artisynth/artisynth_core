package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;

public class ForceSet extends SetBase<ForceBase> implements ModelComponentGenerator<RenderableComponentList<ModelComponent>> {
   
   @Override
   public ForceSet clone () {
      return (ForceSet)super.clone ();
   }

   @Override
   public RenderableComponentList<ModelComponent> createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      String name = getName ();
      if (name == null) {
         name = "forceset";
      }
      RenderableComponentList<ModelComponent> forces = new RenderableComponentList<> (ModelComponent.class, name);
      
      for (ForceBase force : objects()) {
         
         ModelComponent fc = force.createComponent(geometryPath, componentMap);
         if (fc == null) {
            System.err.println ("Failed to create force " + force.getName ());
         } else {
            forces.add (fc);
         }
         
      }
      
      
      return forces;
   }
   
   

}
