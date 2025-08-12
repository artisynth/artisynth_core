/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.Collection;
import java.util.HashSet;

import maspack.properties.PropertyList;
import maspack.util.InternalErrorException;
import maspack.util.DynamicIntArray;

/**
 * Base class for reference lists that can be used to create compnent groupings
 * that are visible to the component hierarchy.
 */
public class ReferenceListBase<C extends ModelComponent,R extends ReferenceComp<C>> 
   extends ComponentList<R> {

   protected static NavpanelVisibility DEFAULT_NAVPANEL_VISIBILITY =
      NavpanelVisibility.ALWAYS;

   public static PropertyList myProps =
      new PropertyList (ReferenceListBase.class, ComponentList.class);

   static {
      myProps.setDefaultValue ("navpanelVisibility", DEFAULT_NAVPANEL_VISIBILITY);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ReferenceListBase() {
      this (null, null);
   }

   public ReferenceListBase (Class<R> clazz, String name) {
      super (clazz, name);
      setNavpanelVisibility (DEFAULT_NAVPANEL_VISIBILITY);
   }

   protected R newReferenceComp() {
      try {
         return getParameterType().newInstance();
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Cannot create instance of " + getParameterType());
      }
   }

   /**
    * Adds a component reference to this list.
    *
    * @param comp component to be referenced
    */
   public R addReference (C comp) {
      R rc = newReferenceComp();
      rc.setReference (comp);
      add (rc);
      return rc;
   }

   /**
    * Adds multiple component references to this list.
    *
    * @param comps components to be referenced
    */
   public void addReferences (Collection<C> comps) {
      for (C c : comps) {
         R rc = newReferenceComp();
         rc.setReference (c);
         add (rc);
      }
   }

   /**
    * Returns the {@code idx}-th referenced component in this list.
    *
    * @param idx index of the reference
    * @return referenced component
    */
   public C getReference (int idx) {
      return get(idx).getReference();
   }

   /**
    * Get all the referenced components in this list.
    *
    * @param comps collects all the referenced components.
    */
   public void getReferences (Collection<C> comps) {
      for (int i=0; i<size(); i++) {
         comps.add (getReference(i));
      }
   }

   /**
    * Queries the number of references in this list. This is
    * simply the size of the list.
    *
    * @return number of references in the list
    */
   public int numReferences () {
      return size();
   }

   /**
    * Checks if this list contains a reference to a specified component.
    *
    * @param comp component to check
    * @return {@code true} if a reference is present
    */
   public boolean containsReference (C comp) {
      return indexOfReference(comp) != -1;
   }

   /**
    * Returns the index of the first occurance of a referenced component in
    * this list, or -1 no reference is present.
    *
    * @param comp component whose index is requested
    * @return index of the reference, or -1 if no reference is present
    */
   public int indexOfReference (C comp) {
      for (int i=0; i<size(); i++) {
         if (get(i).getReference() == comp) {
            return i;
         }
      }
      return -1;
   }         

   /**
    * Remove the first reference to a specified component from this list.
    *
    * @param comp component whose reference is to be removed
    * @return {@code false} if this list did not contain a reference to
    * {@code comp}
    */
   public boolean removeReference (C comp) {
      int idx = indexOfReference (comp);
      if (idx != -1) {
         remove (idx);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Removes alls references to the specified components from this list.
    *
    * @param comps components whose references are to be removed
    * @return {@code false} if this list did not contain a reference to any of
    * the specified components
    */
   public boolean removeReferences (Collection<C> comps) {
      boolean removed = false;

      DynamicIntArray removeIdxs = new DynamicIntArray();
      HashSet<C> removeSet = new HashSet<>(comps);
      for (int i=0; i<size(); i++) {
         if (removeSet.contains (get(i).getReference())) {
            removeIdxs.add (i);
         }
      }
      if (removeIdxs.size() > 0) {
         for (int k=removeIdxs.size()-1; k>=0; k--) {
            remove (removeIdxs.get(k));
         }
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Remove all references from this list.
    */
   public void removeAllReferences() {
      removeAll();
   }


}
