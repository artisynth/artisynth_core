package artisynth.core.materials;

import maspack.util.DataBuffer;
/**
 * Stores state information for viscoelastic behavior
 */
public abstract class ViscoelasticState {

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
