package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;

public class JointSet extends SetBase<JointBase> {

   @Override
   public JointSet clone () {
      return (JointSet)super.clone ();
   }

   public RenderableComponentList<ModelComponent> createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      // create set of joints connecting bodies
      String name = getName();
      if (name == null) {
         name = "jointset";
      }
      RenderableComponentList<ModelComponent> joints = 
         new RenderableComponentList<> (ModelComponent.class, name);
      
      // add all joints
      for (JointBase joint : objects()) {
         
         // create rigid body
         ModelComponent jb = 
            joint.createComponent(geometryPath, componentMap);
         if (jb == null) {
            System.err.println ("Failed to parse joint " + joint.getName ());
         } else { 
            joints.add (jb);
         }
         
      }
    
      componentMap.put (this, joints);
      return joints;
   }
   
}
