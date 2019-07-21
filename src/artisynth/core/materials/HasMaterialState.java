package artisynth.core.materials;

import artisynth.core.modelbase.*;

/**
 * Indicates materials that have state
 */
public interface HasMaterialState {
   
   public boolean hasState();

   public MaterialStateObject createStateObject();

   public void advanceState (MaterialStateObject state, double t0, double t1);
}
