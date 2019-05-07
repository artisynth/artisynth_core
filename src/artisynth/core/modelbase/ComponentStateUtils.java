/**
 * Copyright (c) 2019, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.HashMap;
import java.util.List;

/**
 * Utility methods involving ComponentState
 */
public class ComponentStateUtils {
   
   /**
    * Provides a generic implementation of {@link
    * HasState#getInitialState} for classes that use NumericState.
    */
   public static void getInitialState (
      ComponentState newstate, ComponentState oldstate,
      List<? extends HasNumericState> comps) {

      if (!(newstate instanceof NumericState)) {
         throw new IllegalArgumentException (
            "newstate not a NumericState");
      }
      NumericState nstate = (NumericState)newstate;
      NumericState ostate = null;

      HashMap<HasNumericState,NumericState.DataFrame> compMap = 
         new HashMap<HasNumericState,NumericState.DataFrame>();

      if (oldstate != null) {
         if (!(oldstate instanceof NumericState)) {
            throw new IllegalArgumentException (
               "oldstate not a NumericState");
         }
         ostate = (NumericState)oldstate;
         if (!ostate.hasDataFrames()) {
            throw new IllegalArgumentException (
               "oldstate does not have frames");
         }
         ostate.resetOffsets();
         for (int k=0; k<ostate.numDataFrames(); k++) {
            NumericState.DataFrame frame = ostate.getDataFrame(k);
            compMap.put (frame.getComp(), frame);
         }           
      }
      for (HasNumericState c : comps) {
         NumericState.DataFrame frame = compMap.get(c);
         if (frame != null && frame.getVersion()==c.getStateVersion()) {
            nstate.getState (frame, ostate);
         }
         else {
            nstate.getState (c);
         }
      }
   }
}
