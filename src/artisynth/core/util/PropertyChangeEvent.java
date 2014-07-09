/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import maspack.properties.*;

/**
 * Event indicating the change in value of a property.
 */
public class PropertyChangeEvent {

   String myPropName;
   HasProperties myHost;

   public PropertyChangeEvent (HasProperties host, String propName) {
      myHost = host;
      myPropName = propName;
   }

   public HasProperties getHost() {
      return myHost;
   }

   public String getPropName() {
      return myPropName;
   }

}
