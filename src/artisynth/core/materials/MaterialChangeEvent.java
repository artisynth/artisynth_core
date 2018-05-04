package artisynth.core.materials;

import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Class for reporting changes in Material settings within model subcomponents.
 * Considered to be a DynamicActivityChangeEvent since changing a material can
 * affect solve matrix symmetry.
 */
public class MaterialChangeEvent extends DynamicActivityChangeEvent {

   public static MaterialChangeEvent defaultEvent =
      new MaterialChangeEvent();

   public MaterialChangeEvent() {
      super();
   }
  
   public MaterialChangeEvent(ModelComponent comp) {
      super(comp);
   }

}
