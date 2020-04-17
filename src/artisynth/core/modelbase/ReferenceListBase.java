/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.Collection;

import maspack.properties.PropertyList;
import maspack.util.InternalErrorException;

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

   public R addReference (C ref) {
      R rc = newReferenceComp();
      rc.setReference (ref);
      add (rc);
      return rc;
   }

   public void addReferences (Collection<C> refs) {
      for (C c : refs) {
         R rc = newReferenceComp();
         rc.setReference (c);
         add (rc);
      }
   }

   public C getReference (int idx) {
      return get(idx).getReference();
   }

   public boolean containsReference (C ref) {
      return indexOfReference(ref) != -1;
   }

   public int indexOfReference (C ref) {
      for (int i=0; i<size(); i++) {
         if (get(i).getReference() == ref) {
            return i;
         }
      }
      return -1;
   }         

   public void getReferences (Collection<C> col) {
      for (int i=0; i<size(); i++) {
         col.add (getReference(i));
      }
   }

   public boolean removeReference (C ref) {
      int idx = indexOfReference (ref);
      if (idx != -1) {
         remove (idx);
         return true;
      }
      else {
         return false;
      }
   }

   public boolean removeReferences (Collection<C> refs) {
      boolean removed = false;
      for (C c : refs) {
         if (remove (c)) {
            removed = true;
         }
      }
      return removed;
   }

}
