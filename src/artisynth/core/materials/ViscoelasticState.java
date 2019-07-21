package artisynth.core.materials;

import artisynth.core.modelbase.*;
import maspack.util.DataBuffer;

/**
 * Stores state information for viscoelastic behavior
 */
public abstract class ViscoelasticState implements MaterialStateObject {

   public abstract int getStateSize();

   /** 
    * Stores the state data in a DataBuffer
    */
   public abstract void getState (DataBuffer data);

   /** 
    * Sets the state data from a DataBuffer.
    */
   public abstract void setState (DataBuffer data);

}
