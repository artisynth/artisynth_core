/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.matrix.*;

/**
 * Base class for storing information about changes within a component
 * hierarchy.
 */
public class PropertyChangeEvent extends ComponentChangeEvent {
   // /**
   // * Convenience class for reporting property changes.
   // */
   // public static PropertyChangeEvent defaultEvent = new PropertyChangeEvent
   //();

   private String myPropName;

   public PropertyChangeEvent (ModelComponent comp, String name) {
      super (Code.PROPERTY_CHANGED, comp);
      myPropName = name;
   };

   public PropertyChangeEvent (Code code, ModelComponent comp, String name) {
      super (code, comp);
      myPropName = name;
   };

   // public PropertyChangeEvent()
   // {
   // super (Code.PROPERTY_CHANGED);
   // }

   public String getPropertyName() {
      return myPropName;
   }
}
