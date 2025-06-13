package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.WrapComponent;
import artisynth.core.modelbase.RenderableComponentList;

public class WrapObjectSet extends SetBase<WrapObject> 
   implements ModelComponentGenerator<RenderableComponentList<WrapComponent>> {

   @Override
   public WrapObjectSet clone () {
      return (WrapObjectSet)super.clone ();
   }
   
   @Override
   public RenderableComponentList<WrapComponent> createComponent (
      File geometryPath, ModelComponentMap componentMap) {
     
      String name = getName();
      if (name == null) {
         name = "wrapobjectset";
      }
      RenderableComponentList<WrapComponent> wraps =
         new RenderableComponentList<> (WrapComponent.class, name);
      
      for (WrapObject wo : objects()) {
         WrapComponent wrappable = 
            wo.createComponent (geometryPath, componentMap);
         wraps.add (wrappable);
      }
      
      componentMap.put (this, wraps);
      
      return wraps;
   }
   
}
