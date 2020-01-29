package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.modelbase.ModelComponent;

/**
 * Creates Artisynth model components
 */
public interface ModelComponentGenerator<E extends ModelComponent> {

   /**
    * Creates and returns a model component, using the provided component map
    * to look up other dependent objects.  The newly created component should
    * also be added to the componentMap.
    * 
    * @param geometryPath path to search for geometry files
    * @param componentMap map for looking up other objects
    * @return created component
    */
   public E createComponent(File geometryPath, ModelComponentMap componentMap);
   
}
