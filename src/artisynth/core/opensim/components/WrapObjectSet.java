package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.RenderableComponentList;

public class WrapObjectSet extends SetBase<WrapObject> implements ModelComponentGenerator<RenderableComponentList<RigidBody>> {

   @Override
   public WrapObjectSet clone () {
      return (WrapObjectSet)super.clone ();
   }
   
   @Override
   public RenderableComponentList<RigidBody> createComponent (
      File geometryPath, ModelComponentMap componentMap) {
     
      String name = getName();
      if (name == null) {
         name = "wrapobjectset";
      }
      RenderableComponentList<RigidBody> wraps = new RenderableComponentList<> (RigidBody.class, name);
      
      for (WrapObject wo : objects()) {
         RigidBody wrappable = (RigidBody)wo.createComponent (geometryPath, componentMap);
         wraps.add (wrappable);
      }
      
      componentMap.put (this, wraps);
      
      return wraps;
   }
   
}
