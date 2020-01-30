package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.RenderableComponentList;

public class BodySet extends SetBase<Body> implements ModelComponentGenerator<RenderableComponentList<RigidBody>> {

   @Override
   public BodySet clone () {
      return (BodySet)super.clone ();
   }
   
   @Override
   public RenderableComponentList<RigidBody> createComponent (
      File geometryPath, ModelComponentMap componentMap) {
    
      String name = getName();
      if (name == null) {
         name = "bodyset";
      }
      RenderableComponentList<RigidBody> bodies = new RenderableComponentList<> (RigidBody.class, name);
      
      // add all rigid bodies
      for (Body body : objects()) {
         
         // create rigid body
         RigidBody rb = body.createComponent(geometryPath, componentMap);
         bodies.add (rb);
         
      }
    
      componentMap.put (this, bodies);
      return bodies;
   }
   
}
