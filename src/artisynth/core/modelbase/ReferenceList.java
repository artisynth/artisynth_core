/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.*;
import java.util.*;

import artisynth.core.util.*;
import maspack.util.*;
import maspack.properties.*;

public class ReferenceList extends ComponentList<ReferenceComponent> {

   protected static NavpanelVisibility DEFAULT_NAVPANEL_VISIBILITY =
      NavpanelVisibility.ALWAYS;

   public static PropertyList myProps =
      new PropertyList (ReferenceList.class, ComponentList.class);

   static {
      myProps.setDefaultValue ("navpanelVisibility", DEFAULT_NAVPANEL_VISIBILITY);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   public ReferenceList() {
      this (null);
   }
   
   public ReferenceList(String name) {
      super (ReferenceComponent.class, name);
      setNavpanelVisibility (DEFAULT_NAVPANEL_VISIBILITY);
   }

   public boolean hasParameterizedType() {
      return false;
   }

   public void addReference (ModelComponent ref) {
      add (new ReferenceComponent (ref));
   }

   public void addReferences (Collection<ModelComponent> refs) {
      for (ModelComponent c : refs) {
         add (new ReferenceComponent (c));
      }
   }

   public ModelComponent getReference (int idx) {
      return get(idx).getReference();
   }

   public boolean containsReference (ModelComponent ref) {
      return indexOfReference(ref) != -1;
   }

   public int indexOfReference (ModelComponent ref) {
      for (int i=0; i<size(); i++) {
         if (get(i).getReference() == ref) {
            return i;
         }
      }
      return -1;
   }         

   public void getReferences (Collection<ModelComponent> col) {
      for (int i=0; i<size(); i++) {
         col.add (getReference(i));
      }
   }

   public boolean removeReference (ModelComponent ref) {
      int idx = indexOfReference (ref);
      if (idx != -1) {
         remove (idx);
         return true;
      }
      else {
         return false;
      }
   }

   public boolean removeReferences (Collection<ModelComponent> refs) {
      boolean removed = false;
      for (ModelComponent c : refs) {
         if (remove (c)) {
            removed = true;
         }
      }
      return removed;
   }

}
