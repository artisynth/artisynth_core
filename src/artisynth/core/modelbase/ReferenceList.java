/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.Collection;

import maspack.properties.PropertyList;

public class ReferenceList<C extends ModelComponent> 
   extends ReferenceListBase<C,ReferenceComp<C>> {

   public ReferenceList() {
      this (null);
   }

   public ReferenceList (String name) {
      super ((Class)ReferenceComp.class, name);
   }

   public boolean hasParameterizedType() {
      return false;
   }
}
