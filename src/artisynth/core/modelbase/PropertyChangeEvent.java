/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.properties.HasProperties;

/**
 * Reports changes in a component's properties.
 */
public class PropertyChangeEvent extends ComponentChangeEvent {
   // /**
   // * Convenience class for reporting property changes.
   // */
   // public static PropertyChangeEvent defaultEvent = new PropertyChangeEvent
   //();

   private String myPropName;
   
   // use a separate host field in case the host is not a model component
   private HasProperties myHost;

   public PropertyChangeEvent (ModelComponent comp, String name) {
      super (Code.PROPERTY_CHANGED, comp);
      myPropName = name;
      myHost = comp;
   };

   public PropertyChangeEvent (Code code, ModelComponent comp, String name) {
      super (code, comp);
      myPropName = name;
      myHost = comp;
   };

   public PropertyChangeEvent (HasProperties host, String name) {
      super (Code.PROPERTY_CHANGED, null);
      myPropName = name;
      myHost = host;
   };

   public String getPropertyName() {
      return myPropName;
   }

   public HasProperties getHost() {
      return myHost;
   }
 
}
