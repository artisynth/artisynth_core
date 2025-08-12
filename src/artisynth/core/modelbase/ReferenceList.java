/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.Collection;

import maspack.properties.PropertyList;

/**
 * Contains a list of references to other model components. Used to create
 * compnent groupings that are visible to the component hierarchy.
 */
public class ReferenceList<C extends ModelComponent> 
   extends ReferenceListBase<C,ReferenceComp<C>> {

   /**
    * Creates an empty, unnamed reference list.
    */
   public ReferenceList() {
      this (null);
   }

   /**
    * Creates an empty reference list with a specified name.
    *
    * @param name name of the list
    */
   public ReferenceList (String name) {
      super ((Class)ReferenceComp.class, name);
   }

   public boolean hasParameterizedType() {
      return false;
   }
}
