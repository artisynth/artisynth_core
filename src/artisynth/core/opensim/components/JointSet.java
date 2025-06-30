package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;

public class JointSet extends SetBase<JointBase> 
   implements ModelComponentGenerator<
      RenderableComponentList<artisynth.core.mechmodels.JointBase>> {

   @Override
   public JointSet clone () {
      return (JointSet)super.clone ();
   }

   public RenderableComponentList<artisynth.core.mechmodels.JointBase>
      createComponent (File geometryPath, ModelComponentMap componentMap) {

      // create set of joints connecting bodies
      String name = getName();
      if (name == null) {
         name = "jointset";
      }
      RenderableComponentList<artisynth.core.mechmodels.JointBase> joints = 
         new RenderableComponentList<> (
            artisynth.core.mechmodels.JointBase.class, name);
      
      // add all joints
      for (JointBase joint : objects()) {
         
         // create joint
         artisynth.core.mechmodels.JointBase jb = 
            joint.createComponent(geometryPath, componentMap);
         if (jb == null) {
            System.err.println (
               "OpenSimParser: failed to parse joint " + joint.getName ());
         } else { 
            componentMap.put (joint, jb);
            joints.add (jb);
         }
         
      }
    
      componentMap.put (this, joints);
      return joints;
   }
   
}
