/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;

import maspack.util.*;
import maspack.util.ParameterizedClass;
import maspack.util.ClassAliases;
import maspack.properties.*;

import java.io.*;

import artisynth.core.util.*;

public class ComponentUtils {
   public static final char componentPropertySeparator = ':';

   static class Dependencies {
      ArrayList<ModelComponent> myHard;
      ArrayList<ModelComponent> mySoft;
   }
   
   static protected void recursivelyAddDelete (
      Deque<ModelComponent> delete, HashSet<ModelComponent> updateSet, 
      ModelComponent c) {
      if (!c.isMarked()) {
         delete.offer (c);
         c.setMarked (true);
         if (updateSet != null) {
            updateSet.remove (c);
         }
         if (c instanceof CompositeComponent) {
            CompositeComponent cc = (CompositeComponent)c;
            for (int i=0; i<cc.numComponents(); i++) {
               recursivelyAddDelete (delete, updateSet, cc.get(i));
            }
         }
      }
   }
   
   static boolean ancestorIsMarked (ModelComponent c) {
      CompositeComponent parent;
      for (parent=c.getParent(); parent!=null; parent=parent.getParent()) {
         if (parent.isMarked()) {
            return true;
         }
      }
      return false;
   }

   protected static LinkedList<ModelComponent> removeDescendants (
      LinkedList<ModelComponent> comps) {

      for (ModelComponent c : comps) {
         c.setMarked (true);
      }
      LinkedList<ModelComponent> list = new LinkedList<ModelComponent>();
      for (ModelComponent c : comps) {
         if (!ancestorIsMarked (c)) {
            list.add (c);
         }
      }
      for (ModelComponent c : comps) {
         c.setMarked (false);
      }
      return list;
   }

   protected static void recursivelyGetDependencies (
      Dependencies deps, ModelComponent c,
      HashMap<ModelComponent,Dependencies> depMap) {

      Dependencies localDeps = depMap.get(c);
      if (localDeps != null) {
         if (localDeps.myHard != null) {
            deps.myHard.addAll (localDeps.myHard);
         }
         if (localDeps.mySoft != null) {
            deps.mySoft.addAll (localDeps.mySoft);
         }
      }
      if (c instanceof CompositeComponent) {
         CompositeComponent cc = (CompositeComponent)c;
         for (int i=0; i<cc.numComponents(); i++) {
            recursivelyGetDependencies (deps, cc.get(i), depMap);
         }
      }
   }

   protected static Dependencies getDependencies (
      ModelComponent c, HashMap<ModelComponent,Dependencies> depMap) {

      if (c instanceof CompositeComponent) {
         Dependencies deps = new Dependencies();
         deps.myHard = new ArrayList<ModelComponent>();
         deps.mySoft = new ArrayList<ModelComponent>();
         recursivelyGetDependencies (deps, c, depMap);
         return deps;
      }
      else {
         return depMap.get (c);
      }
   }
   
   protected static void recursivelyAddDependenices (
      LinkedList<ModelComponent> delete, HashSet<ModelComponent> softDeps,
      ModelComponent comp, HashMap<ModelComponent,Dependencies> depMap) {
      
      if (!comp.isMarked()) {
         Dependencies deps = getDependencies (comp, depMap);
         if (deps != null) {
            if (deps.myHard != null) {
               for (ModelComponent c : deps.myHard) {
                  recursivelyAddDependenices (delete, softDeps, c, depMap);
               }
            }
            if (deps.mySoft != null) {
               for (ModelComponent c : deps.mySoft) {
                  if (!c.isMarked()) {
                     softDeps.add (c);
                  }
               }
            }
         }
         if (!comp.isMarked()) {
            comp.setMarked (true);
            delete.add (comp);
            softDeps.remove (comp);
         }
         else {
            System.out.println (
               "Warrning: reference loop deletected for component " +
               getDiagnosticName(comp));
         }
      }      
   }

   // Takes a list of components and returns an extension of it that includes
   // all components which have a hard dependency on one or more components in
   // the original list. It also adds all components which have soft
   // dependencies into the list 'update'.
   //
   // The returned list is ordered with dependent components placed first, in
   // the same order that they should be deleted.
   //
   public static LinkedList<ModelComponent> findDependentComponents (
      List<ModelComponent> update, List<? extends ModelComponent> comps) {

      // find the common reference-containing ancestor for all components,
      // to give us a starting point to build the dependency map.
      ModelComponent acomp = findCommonAncestor (comps);
      if (acomp == null) {
         throw new IllegalArgumentException (
            "Components do not have a common ancestor");
      }
      CompositeComponent ancestor = farthestEncapsulatingAncestor(acomp);

      // Now we build a dependency map, which, for every component, provides a
      // list of both its hard and soft dependencies
      HashMap<ModelComponent,Dependencies> depMap =
         buildDependencyMap (ancestor);
      
      HashSet<ModelComponent> softDeps = new LinkedHashSet<ModelComponent>();

      LinkedList<ModelComponent> delete = new LinkedList<ModelComponent>();
      for (ModelComponent c : comps) {
         recursivelyAddDependenices (delete, softDeps, c, depMap);
      }
      // Now prune descendants: remove any component from both 'delete' and 
      // 'softDeps' which has an ancestor in 'delete'. We use the fact that
      // all components in 'delete' are marked at this point.
      for (ModelComponent c : softDeps){
         if (!ancestorIsMarked(c)) {
            update.add (c);
         }
      }
      Iterator<ModelComponent> it = delete.iterator();
      while (it.hasNext()) {
         ModelComponent c = it.next();
         if (ancestorIsMarked(c)) {
            c.setMarked (false);
            it.remove();
         }
      }
      for (ModelComponent c : delete) {
         c.setMarked (false);
      }
      
      // Need to rearrange the delete list so that all components with the same
      // parents are grouped together. This greatly improves the efficiency of
      // the resulting operation.
      //
      // Note that in doing this, we must be sure that the 'parents' appear in
      // the same order that they first appear in the list, in order to ensure
      // that components are deleted in the correct dependency order. We are
      // explicitly making the assumption that all components belonging to the
      // same parent form a closure with respect to the dependency ordering.
      LinkedHashMap<ModelComponent,LinkedList<ModelComponent>> parentMap =
         new LinkedHashMap<ModelComponent,LinkedList<ModelComponent>>();
      ModelComponent currentParent = null;
      LinkedList<ModelComponent> listForParent = null;
      ModelComponent root = null;
      for (ModelComponent c : delete) {
         ModelComponent parent = c.getParent();
         if (parent == null) {
            if (root == null) {
               root = c;
            }
            else if (c != root) {
               throw new InternalErrorException (
                  "Multiple root components found:\n" + 
                  getPathName(c)+"\n" + getPathName(root));
            }
         }
         else {
            if (parent != currentParent) {
               listForParent = parentMap.get (parent);
               if (listForParent == null) {
                  listForParent = new LinkedList<ModelComponent>();
                  parentMap.put (parent, listForParent);
               }
               currentParent = parent;
            }
            listForParent.add (c);
         }
      }
      delete.clear();
      if (root != null){
         delete.add (root);
      }
      for (LinkedList<ModelComponent> l : parentMap.values()) {
         delete.addAll (l);
      }
      return delete;
   }

   public static LinkedList<MutableCompositeComponent<?>> removeComponents (
      List<? extends ModelComponent> list, int[] indices) {
      if (indices != null && indices.length < list.size()) {
         throw new IllegalArgumentException ("index array not large enough");
      }
//       for (ModelComponent c : list) {
//          System.out.println (c);
//       }
      
      // "locals" are contiguous components with the same parent
      ModelComponent[] localComps = new ModelComponent[list.size()];
      int[] localIdxs = (indices != null ? new int[list.size()] : null);

      LinkedList<MutableCompositeComponent<?>> parents =
         new LinkedList<MutableCompositeComponent<?>>();
      if (list.size() == 0) {
         return parents;
      }

      MutableCompositeComponent<?> currentParent = null;
      int numlocs = 0;
      int k = 0; // index into component list
      ListIterator<? extends ModelComponent> it = list.listIterator();
      while (it.hasNext()) {
         ModelComponent c = it.next();
         // delete all components with the same parent as a group
         MutableCompositeComponent<?> parent = (MutableCompositeComponent<?>)c.getParent();
         parents.add (parent);
         if (currentParent != parent) {
            if (numlocs > 0) {
               currentParent.removeComponents (localComps, localIdxs, numlocs);
               if (indices != null) {
                  System.arraycopy (localIdxs, 0, indices, k, numlocs);
                  k += numlocs;
               }
            }
            numlocs = 0;
            currentParent = parent;
         }
         localComps[numlocs++] = c;
      }
      currentParent.removeComponents (localComps, localIdxs, numlocs);
      if (indices != null) {
         System.arraycopy (localIdxs, 0, indices, k, numlocs);
      }
      return parents;
   }

   public static boolean isAncestorSelected (ModelComponent comp) {
      for (CompositeComponent c=comp.getParent(); c != null; c=c.getParent()) {
         if (c.isSelected()) {
            return true;
         }
      }
      return false;      
   }
   
   public static void addComponents (
      List<? extends ModelComponent> list, int[] indices,
      List<MutableCompositeComponent<?>> parents) {

      addComponents (list, indices, parents, /*reverse=*/false);
   }

   public static void addComponentsInReverse (
      List<? extends ModelComponent> list, int[] indices,
      List<MutableCompositeComponent<?>> parents) {

      addComponents (list, indices, parents, /*reverse=*/true);
   }

   public static void addComponents (
      List<? extends ModelComponent> list, int[] indices,
      MutableCompositeComponent<?> parent) {

      LinkedList<MutableCompositeComponent<?>> singleParentList =
         new LinkedList<MutableCompositeComponent<?>>();
      singleParentList.add (parent);
      addComponents (list, indices, singleParentList, /*reverse=*/false);
   }

   public static void addComponents (
      List<? extends ModelComponent> list, int[] indices,
      List<MutableCompositeComponent<?>> parents, boolean reverse) {

      if (parents.size() != 1 && list.size() != parents.size()) {
         throw new IllegalArgumentException (
            "component and parent lists have different sizes");
      }
      if (indices != null && indices.length < list.size()) {
         throw new IllegalArgumentException (
            "index list length is smaller that component list size");
      }

      if (list.size() == 0) {
         return; // just in case ...
      }
      
      boolean singleParent = (parents.size() == 1);
      ListIterator<? extends ModelComponent> li;
      ListIterator<MutableCompositeComponent<?>> pi = null;
      int idx; // index into component list

      if (reverse) {
         li = list.listIterator (list.size());
         if (!singleParent) {
            pi = parents.listIterator (list.size());
         }
         idx = list.size()-1;
      }
      else {
         li = list.listIterator ();
         if (!singleParent) {
            pi = parents.listIterator ();
         }
         idx = 0;
      }

      ModelComponent[] localComps = new ModelComponent[list.size()];
      int[] localIdxs = (indices != null ? new int[list.size()] : null);

      MutableCompositeComponent<?> currentParent = parents.get(parents.size()-1);
      MutableCompositeComponent<?> parent = currentParent;
      int numCurrent = 0;
      while (reverse ? li.hasPrevious() : li.hasNext()) {
         ModelComponent c = (reverse ? li.previous() : li.next());
         if (!singleParent) {
            parent = (reverse ? pi.previous() : pi.next());
            if (currentParent != parent) {
               if (numCurrent > 0) {
                  currentParent.addComponents (localComps, localIdxs, numCurrent);
                  numCurrent = 0;
               }
               currentParent = parent;
            }
         }
         localComps[numCurrent] = c;
         if (indices != null) {
            localIdxs[numCurrent] = indices[idx];
            idx = (reverse ? idx-1 : idx+1);            
         }
         numCurrent++;
      }
      currentParent.addComponents (localComps, localIdxs, numCurrent);
   }

   /**
    * Returns true if a component is equal to, or is a descendant of a
    * particular ancestor component.
    * 
    * @param comp
    * component to check
    * @param ancestor
    * ancestor component
    * @return true if comp is contained within the hierarchy rooted at ancestor
    */
   public static boolean withinHierarchy (
      ModelComponent comp, ModelComponent ancestor) {
      if (ancestor == null) {
         return false;
      }
      while (comp != null) {
         if (comp == ancestor) {
            return true;
         }
         comp = comp.getParent();
      }
      return false;
   }

   /**
    * Returns true if a component is equal to, or is a descendant of a
    * particular object. If the object is not a ModelComponent, this method
    * returns false.
    * 
    * @param comp
    * component to check
    * @param topObject
    * candidate ancestor component
    * @return true if topObject is a ModelComponent and contains comp within its
    * hierarchy
    */
   public static boolean withinHierarchy (
      ModelComponent comp, TransformableGeometry topObject) {
      if (topObject instanceof ModelComponent) {
         return withinHierarchy (comp, (ModelComponent)topObject);
      }
      else {
         return false;
      }
   }

   static public boolean addCopyComponents (
      List<ModelComponent> list, ModelComponent comp) {
      LinkedList<ModelComponent> refs = new LinkedList<ModelComponent>();
      // get immediate referals for component

      if (!(comp instanceof CopyableComponent)) {
         return false;
      }
      CopyableComponent copyable = (CopyableComponent)comp;
      // if (!copyable.isDuplicatable()) {
      //    return false;
      // }
      if (!copyable.getCopyReferences (refs, comp)) {
         return false;
      }
      for (ModelComponent rcomp : refs) {
         if (!addCopyComponents (list, rcomp)) {
            return false;
         }
      }
      if (!comp.isMarked()) {
         list.add (comp);
         comp.setMarked (true);
      }
      return true;
   }

   // Extends a list of components to include all those
   // required to copy the component. If the components
   // cannot be copies, null is returned.
   public static LinkedList<ModelComponent> findCopyComponents (
      List<ModelComponent> comps) {
      LinkedList<ModelComponent> list = new LinkedList<ModelComponent>();
      boolean valid = true;
      for (ModelComponent c : comps) {
         if (!addCopyComponents (list, c)) {
            valid = false;
            break;
         }
      }
      for (ModelComponent c : list) {
         c.setMarked (false);
      }
      return valid ? list : null;
   }

   public static boolean addCopyReferences (
      List<ModelComponent> list, ModelComponent comp, ModelComponent ancestor) {
      if (!(comp instanceof CopyableComponent) ||
          !((CopyableComponent)comp).getCopyReferences (list, ancestor)) {
         return false;
      }
      if (!withinHierarchy (comp, ancestor)) {
         list.add (comp);
      }
      return true;
   }

//   public static <C extends ModelComponent> boolean addCopyReferences (
//      List<ModelComponent> list, Collection<C> comps, ModelComponent ancestor) {
//      for (ModelComponent c : comps) {
//         if (!(c instanceof CopyableComponent) | 
//             !((CopyableComponent)c).getCopyReferences (list, ancestor)) {
//            return false;
//         }
//         if (!withinHierarchy (c, ancestor)) {
//            list.add (c);
//         }
//      }
//      return true;
//   }

   /**
    * Returns the depth of a component within the component hierarchy. This
    * equals the number of ancestor components, so a component with no parent
    * would have a depth of 0, a component whose parent is the root would have a
    * depth of 1, etc.
    */
   public static int getDepth (ModelComponent comp) {
      CompositeComponent ancestor;
      int depth = 0;
      for (ancestor = comp.getParent(); ancestor != null; ancestor =
         ancestor.getParent()) {
         depth++;
      }
      return depth;
   }
   
   /**
    * Returns <code>true</code> if two components have a common ancestor.
    */
   public static boolean haveCommonAncestor (
      ModelComponent comp1, ModelComponent comp2) {
      return (findCommonAncestor (comp1, comp2) != null);
   }
   
   /**
    * Returns true if <code>comp2</code> is connected to the same
    * component hierarchy as <code>comp1</code>.
    */
   public static boolean areConnected (
      ModelComponent comp1, ModelComponent comp2) {
      return haveCommonAncestor (comp1, comp2);
   }
   
   /**
    * Returns the common ancestor, if any, for two model components. If there is
    * no common ancestor, returns null. If the components are the same, then
    * the (first) component is returned. If one component is an ancestor of
    * the other, then that component is returned.
    */
   public static ModelComponent findCommonAncestor (
      ModelComponent comp1, ModelComponent comp2) {
      int depth1 = getDepth (comp1);
      int depth2 = getDepth (comp2);
      ModelComponent ancestor1 = comp1;
      ModelComponent ancestor2 = comp2;
      while (depth2 > depth1) {
         ancestor2 = ancestor2.getParent();
         depth2--;
      }
      while (depth1 > depth2) {
         ancestor1 = ancestor1.getParent();
         depth1--;
      }
      while (depth1 > 0 && ancestor1 != ancestor2) {
         ancestor1 = ancestor1.getParent();
         ancestor2 = ancestor2.getParent();
         depth1--;
      }
      if (ancestor1 == ancestor2) {
         return ancestor1;
      }
      else {
         return null;
      }
   }

   /**
    * Returns {@code true} if components {@code comp1} and {@code comp2} are
    * connected via the component hierarchy, with the path running through the
    * specified intermediate component {@code viacomp} (which may equal {@code
    * comp1}, {@code comp2}, or any component on the path in between).
    *
    * @param comp1 first component
    * @param comp2 second component to which path is being checked
    * @param viacomp required intermediate component
    * @return {@code true} if components are connected via a path containing
    * {@code viacomp}.
    */
   public static boolean areConnectedVia (
      ModelComponent comp1, ModelComponent comp2, ModelComponent viacomp) {

      int depth1 = getDepth (comp1);
      int depth2 = getDepth (comp2);
      ModelComponent ancestor1 = comp1;
      ModelComponent ancestor2 = comp2;
      
      boolean viaFound = false;

      if (depth1 > depth2) {
         while (depth1 > depth2) {
            if (!viaFound && ancestor1 == viacomp) {
               viaFound = true;
            }
            ancestor1 = ancestor1.getParent();
            depth1--;
         }
      }
      else {
         while (depth2 > depth1) {
            if (!viaFound && ancestor2 == viacomp) {
               viaFound = true;
            }
            ancestor2 = ancestor2.getParent();
            depth2--;
         }
      }
      // ancestor1 and ancestor2 are now at the same depth. Move upward,
      // looking for a common ancestor, and checking for viacomp if it
      // hasn't been found yet
      while (depth1 >= 0) {
         if (!viaFound) {
            if (ancestor1 == viacomp || ancestor2 == viacomp) {
               viaFound = true;
            }
         }
         if (ancestor1 == ancestor2) {
            return viaFound;
         }
         ancestor1 = ancestor1.getParent();
         ancestor2 = ancestor2.getParent();
         depth1--;
      }
      return false;
   }

   /**
    * Returns the common ancestor, if any, for a list of model components.
    * If there is no common ancestor, returns null.
    */
   public static ModelComponent findCommonAncestor (
      List<? extends ModelComponent>comps) {
      boolean first = true;
      ModelComponent ancestor = null;
      for (ModelComponent c : comps) {
         if (first) {
            ancestor = c;
            first = false;
         }
         else {
            ancestor = findCommonAncestor (ancestor, c);
         }
         if (ancestor == null) {
            return null;
         }
      }
      return ancestor;
   }

   /**
    * Returns true if comp0 is an ancestor of comp1. If comp0 is not
    * an ancestor of comp1, or if it equals comp1, then the method
    * returns false.
    */
   public static boolean isAncestorOf (
      ModelComponent comp0, ModelComponent comp1) {
      ModelComponent ancestor=comp1.getParent();
      while (ancestor != null) {
         if (ancestor == comp0) {
            return true;
         }
         ancestor = ancestor.getParent();
      }
      return false;
   }

   /**
    * Returns true if all references associated with a component are recursively
    * contained beneath a specified ancestor.
    * 
    * @param ancestor
    * ancestor component beneath which reference should be contained
    * @param comp
    * component whose references should be checked
    * @return true if all references are recursively contained below the
    * specified ancestor
    */
   public static boolean referencesContained (
      CompositeComponent ancestor, ModelComponent comp) {
      LinkedList<ModelComponent> refs = new LinkedList<ModelComponent>();
      comp.getHardReferences (refs);
      comp.getSoftReferences (refs);
      for (ModelComponent c : refs) {
         if (!ModelComponentBase.recursivelyContains (ancestor, c)) {
            return false;
         }
      }
      return true;
   }
   

   /**
    * Returns name information for an object suitable for diagnostic
    * printing.
    */
   public static String getDiagnosticName (Object obj) {
      if (obj == null) {
         return "null";
      }
      else if (obj instanceof String) {
         return (String)obj;
      }
      else {
         String name = obj.getClass().getSimpleName();
         if (obj instanceof ModelComponent) {
            ModelComponent comp = (ModelComponent)obj;
            String path = getPathName (comp);
            if (!path.equals ("-1")) {
               name += " " + path;
            }
         }
         return name;
      }
   }

   protected static void recursivelyCheckReferenceContainment (
      ModelComponent comp, CompositeComponent ancestor) {
      LinkedList<ModelComponent> refs = new LinkedList<ModelComponent>();
      comp.getHardReferences (refs);
      comp.getSoftReferences (refs);
      for (ModelComponent c : refs) {
         if (c == null) {
            System.out.println ("null reference for " +
                                getDiagnosticName (comp));
         }
         if (!ModelComponentBase.recursivelyContains (ancestor, c)) {
            throw new IllegalStateException (
               getDiagnosticName(c)+" referenced by "+getDiagnosticName(comp)+
               " is outside containment hierarchy");
         }
      }
      if (comp instanceof CompositeComponent) {
         CompositeComponent ccomp = (CompositeComponent)comp;
         // Used to not recurse into models, but we do now since all references
         // are being checked in one go after the model is being created.
         //if (!ccomp.hierarchyContainsReferences()) {
            for (int i=0; i<ccomp.numComponents(); i++) {
               recursivelyCheckReferenceContainment (ccomp.get(i), ancestor);
            }
         //}
      }
   }
         
   public static void checkReferenceContainment (ModelComponent comp) {
      CompositeComponent ancestor = farthestEncapsulatingAncestor(comp);
//      /CompositeComponent ancestor = nearestEncapsulatingAncestor(comp);
      if (ancestor != null) {
         recursivelyCheckReferenceContainment (comp, ancestor);
      }
   }

   public static void checkReferenceContainment (
      ModelComponent comp, CompositeComponent ancestor) {
      recursivelyCheckReferenceContainment (comp, ancestor);
   }

   /**
    * Returns the closest ancestor of a component (or the component
    * itself) for which {@link 
    * CompositeComponent#hierarchyContainsReferences()
    * hierarchyContainsDependencies()} returns <code>true</code>.
    * That means all inter-component references are contained
    * within the ancestor's hierarchy. If no such ancestor is found, 
    * <code>null</code> is returned.
    * 
    * @return closest encapsulating ancestor
    */
   public static CompositeComponent nearestEncapsulatingAncestor (
      ModelComponent c) {
      CompositeComponent ancestor;
      if (c instanceof CompositeComponent) {
         ancestor = (CompositeComponent)c;
      }
      else {
         ancestor = c.getParent();
      }
      while (ancestor != null) {
         if (ancestor.hierarchyContainsReferences()) {
            return ancestor;
         }
         ancestor = ancestor.getParent();
      }
      return null;
   }

   /**
    * Returns the farthest ancestor of a component (or the component
    * itself) for which {@link 
    * CompositeComponent#hierarchyContainsReferences()
    * hierarchyContainsDependencies()} returns <code>true</code>.
    * That means all inter-component references are contained
    * within the ancestor's hierarchy. If no such ancestor is found, 
    * <code>null</code> is returned.
    * 
    * @return farthest encapsulating ancestor
    */
   public static CompositeComponent farthestEncapsulatingAncestor (
      ModelComponent c) {
      CompositeComponent ancestor;
      if (c instanceof CompositeComponent) {
         ancestor = (CompositeComponent)c;
      }
      else {
         ancestor = c.getParent();
      }
      CompositeComponent farthest = null;
      while (ancestor != null) {
         if (ancestor.hierarchyContainsReferences()) {
            farthest = ancestor;
         }
         ancestor = ancestor.getParent();
      }
      return farthest;
   }
   
   /**
    * Recursively finds all subcomponents of {@code comp} which are invalid
    * according to {@link ClassAliases#isClassValid}, along with their hard
    * dependencies, marks them to be non-writable, and returns a list of them.
    * 
    * @param comp component whose subcomponents should be checked
    * @return non-core invalid components and their hard dependencies
    */
   public static LinkedList<ModelComponent> markInvalidSubcomps (
      ModelComponent comp) {

      LinkedList<ModelComponent> nccomps = findInvalidSubcomps (comp);
      for (ModelComponent c : nccomps) {
         c.setWritable (false);
      }
      return nccomps;
   }

   /**
    * Sets all components in a list to be writable. This undoes the effect of
    * {@link #markInvalidSubcomps}.
    * 
    * @param nccomps list of invalid components and their hard dependencies
    * previously returned by {@link #markInvalidSubcomps}.
    */
   public static void unmarkInvalidSubcomps (
      LinkedList<ModelComponent> nccomps) {
      
      for (ModelComponent c : nccomps) {
         c.setWritable (true);
      }
   }

   /**
    * Recursively finds all subcomponents of {@code comp} which are invalid
    * according to {@link ClassAliases#isClassValid}. The resulting list of
    * invalid components is then extended to include all of their hard
    * dependencies.
    * 
    * @param comp component whose subcomponents should be checked
    * @return non-core invalid components and their hard dependencies
    */
   protected static <C> LinkedList<ModelComponent> findInvalidSubcomps (
      ModelComponent comp) {

      LinkedList<ModelComponent> incomp = new LinkedList<>();
      if (ClassAliases.getClassFilter() != null) {
         findInvalidSubcomps (incomp, comp);
         if (incomp.size() > 0) {
            LinkedList<ModelComponent> update = new LinkedList<ModelComponent>();
            incomp = findDependentComponents (update, incomp);
         }
      }
      return incomp;
   }

   /**
    * Recursively checks any subcomponents of {@code comp} to find those which
    * are invalid according to {@link ClassAliases#isClassValid}.
    *
    * @param incomp list to append invalid components to
    * @param comp component whose subcomponents should be checked
    */
   protected static void findInvalidSubcomps (
      LinkedList<ModelComponent> incomp, ModelComponent comp) {

      if (comp instanceof CompositeComponent) {
         CompositeComponent ccomp = (CompositeComponent)comp;
         for (int i=0; i<ccomp.numComponents(); i++) {
            ModelComponent c = ccomp.get(i);
            if (!ClassAliases.isClassValid (c)) {
               incomp.add (c);
            }
            findInvalidSubcomps (incomp, c);
         }
      }
   }

   public static <T extends ModelComponent> T loadComponent (
      File file, CompositeComponent ancestor, Class<T> expectedType)
      throws IOException {
      ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (file);
      rtok.nextToken();
      if (!rtok.tokenIsWord()) {
         throw new IOException ("component class name or alias expected, got "
         + rtok);
      }
      Class<?> cls = ClassAliases.resolveClass (rtok.sval);
      if (cls == null) {
         throw new IOException ("no class found corresponding to " + rtok.sval);
      }
      if (expectedType != null && !expectedType.isAssignableFrom (cls)) {
         throw new IOException ("file contains an instance of " + cls
         + " instead of " + expectedType);
      }
      T comp;
      try {
         comp = (T)cls.newInstance();
      }
      catch (Exception e) {
         throw new IOException ("cannot create instance of " + cls);
      }
      if (ancestor == null && comp instanceof CompositeComponent) {
         ancestor = (CompositeComponent)comp;
      }
      ScanWriteUtils.scanfull (rtok, comp, ancestor);
      return comp;
   }

   public static <T extends ModelComponent> T loadComponent (
      String fileName, CompositeComponent ancestor, Class<T> expectedType)
      throws IOException {
      return loadComponent (new File(fileName), ancestor, expectedType);
   }

   public static <T extends ModelComponent> T loadComponent (
      String fileName, Class<T> expectedType) throws IOException {
      return loadComponent (new File(fileName), null, expectedType);
   }

   public static ModelComponent loadComponent (
      String fileName, CompositeComponent ancestor) throws IOException {
      return loadComponent (new File(fileName), ancestor, ModelComponent.class);
   }

   public static ModelComponent loadComponent (String fileName)
      throws IOException {
      return loadComponent (new File(fileName), null, ModelComponent.class);
   }

   /**
    * Returns the ModelComponent, if any, associated with a given property.
    * 
    * @param prop
    * property to be queried
    * @return model component associated with the property
    */
   public static ModelComponent getPropertyComponent (Property prop) {
      HasProperties superHost = prop.getHost();
      while (superHost != null) {
         if (superHost instanceof ModelComponent) {
            return (ModelComponent)superHost;
         }
         else if (superHost instanceof CompositeProperty) {
            superHost = ((CompositeProperty)superHost).getPropertyHost();
         }
         else {
            superHost = null;
         }
      }
      return null;
   }

   private static boolean hasDotChar (String path) {
      int idot = 0;
      while (idot != -1) {
         idot = path.indexOf ('.', idot);
         if (idot != -1) {
            if (path.regionMatches (idot, "../", 0, 3)) {
               idot += 3;
            }
            else if (path.regionMatches (idot, "./", 0, 2)) {
               idot += 2;
            }
            else {
               return true;
            }
         }
      }
      return false;
   }

   public static ModelComponent findComponent (ModelComponent comp, String path) {
      // TODO still need to make this fast
      if (path.equals (".")) {
         return comp;
      }
      else if (path.equals ("..")) {
         return comp.getParent();
      }
      else if (hasDotChar (path)) {
         throw new IllegalArgumentException (
            "component path name '"+path+"' contains '.'");
      }
      if (comp == null) {
         return null;
      }
      int slashIdx = path.indexOf ('/');
      if (slashIdx != -1) {
         ModelComponent subc = null;
         if (slashIdx == 0) { // set subc to the root component
            subc = comp;
            while (subc.getParent() != null) {
               subc = subc.getParent();
            }
         }
         else {
            String subName = path.substring (0, slashIdx);
            if (subName.equals ("..")) {
               subc = comp.getParent();
            }
            else if (subName.equals (".")) {
               subc = comp;
            }
            else if (comp instanceof CompositeComponent) {
               subc = ((CompositeComponent)comp).get (subName);
            }
            if (subc == null) {
               return null;
            }
         }
         String subPath = path.substring (slashIdx + 1);
         if (subPath.length() == 0) {
            return (subc instanceof CompositeComponent ? subc : null);
         }
         else if (subPath.charAt (0) == '/') {
            throw new IllegalArgumentException (
               "double '/' not permitted in component path names");
         }
         return findComponent (subc, subPath);
      }
      else {
         if (path.equals ("null")) {
            return null;
         }
         else if (comp instanceof CompositeComponent) {
            return ((CompositeComponent)comp).get (path);
         }
         else {
            return null;
         }
      }
   }

   private static ModelComponent findSubComponent (
      ModelComponent comp, String path, int islash) {
      if (!(comp instanceof CompositeComponent)) {
         throw new IllegalArgumentException (
            "Property path has component prefix, but host component is not composite\n"
            + "path=\"" + path + "\"");
      }
      if (islash == 0) {
         throw new IllegalArgumentException (
            "Property path cannot begin with a '/'");
      }
      if (islash == path.length()) {
         throw new IllegalArgumentException (
            "No property name after component path");
      }
      ModelComponent subc =
         findComponent ((CompositeComponent)comp, path.substring (0, islash));
      if (subc == null) {
         System.out.println ("findSubComponent: cannot find component "
         + path.substring (0, islash));
      }
      return subc;
   }

   public static Property findProperty (ModelComponent comp, String path) {
      if (componentPropertySeparator == ':') {
         Property prop;
         try {
            prop = findPropertyColon (comp, path);
         }
         catch (Exception e) {
            prop = null;
         }
         if (prop == null && path.indexOf (":") == -1 && !path.equals (".")) {
            prop = findPropertyOld (comp, path);
            if (prop != null) {
               int lastSlashIdx = path.lastIndexOf ('/');
               String newPath =
                  (path.substring (0, lastSlashIdx) +
                   ":" + path.substring (lastSlashIdx + 1));
               System.out.println ("WARNING: old style property path\n" + path
               + "\n" + "should be replaced with\n" + newPath);
            }
         }
         return prop;
      }
      else {
         return findPropertyOld (comp, path);
      }
   }

   public static Object findComponentOrProperty (
      ModelComponent comp, String path) {
      return findComponentOrPropertyColon (comp, path);
   }

   private static Object findComponentOrPropertyColon (
      ModelComponent comp, String path) {
      if (path.indexOf (':') != -1) { // then it should be a property with a
                                       // colon separator
         return findPropertyColon (comp, path);
      }
      else if (path.indexOf ('/') != -1) { // then it should be a component
         return findComponent (comp, path);
      }
      else if (path.equals (".")) {
         return comp;
      }
      else if (path.equals ("..")) {
         return comp.getParent();
      }
      else {
         Object obj = findPropertyColon (comp, path);
         if (obj == null) {
            obj = findComponent (comp, path);
         }
         return obj;
      }
   }

   private static Property findPropertyOld (ModelComponent comp, String path) {
      ModelComponent subc;
      String propPath;
      int islash = path.lastIndexOf ('/');
      if (islash != -1) {
         subc = findSubComponent (comp, path, islash);
         propPath = path.substring (islash + 1);
      }
      else {
         subc = comp;
         propPath = path;
      }
      if (subc == null) {
         return null;
      }
      else if (!(subc instanceof HasProperties)) {
         return null;
      }
      else {
         Property prop =
            PropertyList.getProperty (propPath, (HasProperties)subc);
         return prop;
      }
   }

   private static Property findPropertyColon (ModelComponent comp, String path) {
      ModelComponent propComp = comp; // component holding the property
      String propPath = path;

      int colonIdx = path.indexOf (':');
      if (colonIdx != -1) {
         String compPath = path.substring (0, colonIdx);
         propPath = path.substring (colonIdx + 1);
         if (compPath.length() > 0) {
            propComp = findComponent (comp, compPath);
         }
      }
      if (propPath.indexOf (':') != -1) {
         throw new IllegalArgumentException (
            "Path has multiple ':' characters: " + path);
      }
      if (propPath.indexOf ('/') != -1) {
         throw new IllegalArgumentException (
            "Property part of path contains '/': " + path);
      }
      if (propComp == null) {
         return null;
      }
      else if (!(propComp instanceof HasProperties)) {
         return null;
      }
      else {
         Property prop =
            PropertyList.getProperty (propPath, (HasProperties)propComp);
         return prop;
      }
   }

   public static Property parseProperty (
      Object obj, CompositeComponent ancestor) throws IOException {
      
      if (obj instanceof StringToken) {
         StringToken tok = (StringToken)obj;
         Property prop = null;
         prop = ComponentUtils.findProperty (ancestor, tok.value());
         if (prop == null) {
            throw new IOException (
               "Can't find property corresponding to "+tok.value()+
               ", line "+tok.lineno());
         }
         return prop;
      }
      else {
         throw new IOException (
            "Token "+obj+" is not a StringToken");
      }  
   }

   public static ArrayList<Property> parseProperties (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      ArrayList<Property> list = new ArrayList<Property>();
      while (tokens.peek() != null) {
         list.add (parseProperty (tokens.poll(), ancestor));
      }
      tokens.poll(); // eat null terminator
      return list;
   }
   
   private static String getUsableName (ModelComponent c) {
      String name = c.getName();
      if (name != null) {
         CompositeComponent parent = c.getParent();
         if (parent != null && parent.get(name) != c) {
            return null;
         }
      }
      return name;
   }

   private static String getUsableShortName (ModelComponent c) {
      String name = null;
      if (c instanceof ComponentList) {
         ComponentList<?> cl = (ComponentList<?>)c;
         name = cl.getShortName();
         if (name != null) {
            CompositeComponent parent = c.getParent();
            if (parent != null && parent.get(name) != c) {
               return null;
            }
         }
      }
      return name;
   }

   public static String getPathName (ModelComponent comp) {
      ModelComponent c = comp;
      StringBuilder sbuf = new StringBuilder (256);
      while (c != null) {
         if (c != comp) {
            sbuf.insert (0, '/');
         }
         String name = getUsableName (c);
         if (name != null) {
            sbuf.insert (0, name);
         }
         else {
            sbuf.insert (0, c.getNumber());
         }
         c = c.getParent();
      }
      return sbuf.toString();
   }

   public static String getPathName (
      ModelComponent ancestor, ModelComponent comp) {
      return getPathName (ancestor, comp, /* compact= */false);
   }

   private static String getName (ModelComponent comp, boolean compact) {
      if (ModelComponentBase.useCompactPathNames && compact) {
         String shortName = getUsableShortName (comp);
         if (shortName != null) {
            return shortName;
         }
         else if (comp.getParent() != null) {
            return Integer.toString (comp.getNumber());
         }
         else {
            return comp.getName();
         }
      }
      else {
         String name = getUsableName (comp);
         if (name == null) {
            return Integer.toString (comp.getNumber());
         }
         else {
            return name;
         }
      }
   }

   /**
    * Returns a path name from a reference component to a target component. The
    * reference does not need to be an ancestor of the target.
    */
   public static String getPathName (
      ModelComponent reference, ModelComponent target, boolean compact) {
      // TODO: make this faster
      if (target == null) {
         return "null";
      }
      if (reference == null) {
         return target.getName();
      }
      else if (reference == target) {
         return ".";
      }

      int rdepth = getDepth (reference);
      int tdepth = getDepth (target);

      ModelComponent r = reference;
      ModelComponent c = target;

      // strings to form the path name will be assembled from back to front
      ArrayList<String> pathStrList = new ArrayList<String> (10);

      int d = Math.min (rdepth, tdepth);
      for (int i = 0; i < rdepth - d; i++) {
         r = r.getParent();
      }
      for (int i = 0; i < tdepth - d; i++) {
         pathStrList.add (getName (c, compact));
         c = c.getParent();
      }
      while (c != r && c != null) {
         pathStrList.add (getName (c, compact));
         c = c.getParent();
         r = r.getParent();
         d--;
      }
      StringBuilder sbuf = new StringBuilder (80);
      if (c == r) {
         for (int i = 0; i < rdepth - d; i++) {
            sbuf.append ("../");
         }
      }
      else {
         sbuf.append ("/");
      }
      for (int i = pathStrList.size() - 1; i >= 0; i--) {
         if (i < pathStrList.size() - 1) {
            sbuf.append ("/");
         }
         sbuf.append (pathStrList.get (i));
      }
      return sbuf.toString();
   }

   public static String getWritePropertyPathName (
      Property prop, ModelComponent topAncestor) {
      return Write.getQuotedString (
         doGetPropertyPathName (prop, topAncestor, /*excludeLeaf=*/false));
   }

   public static String getPropertyPathName (
      Property prop, ModelComponent topAncestor, boolean excludeLeaf) {
      return doGetPropertyPathName (prop, topAncestor, excludeLeaf);
   }

   public static String getPropertyPathName (Property prop) {
      return doGetPropertyPathName (
         prop, null, /*excludeLeaf=*/false);
   }

   private static String doGetPropertyPathName (
      Property prop, ModelComponent topAncestor, boolean excludeLeaf) {
      StringBuilder buf = new StringBuilder();
      if (!excludeLeaf) {
         buf.append (prop.getName());
      }
      HasProperties host = prop.getHost();
      while (!(host instanceof ModelComponent) &&
             (host instanceof CompositeProperty)) {
         CompositeProperty cprop = (CompositeProperty)host;
         if (buf.length() != 0) {
            buf.insert (0, '.');
         }
         buf.insert (0, cprop.getPropertyInfo().getName());
         host = cprop.getPropertyHost();
      }
      if (host instanceof ModelComponent) {
         String path;
         if (topAncestor == null) {
            path = getPathName ((ModelComponent)host);
         }
         else {
            path = getPathName (
               (ModelComponent)topAncestor, (ModelComponent)host);
         }
         if (!path.equals (".")) {
            if (buf.length() != 0) {
               buf.insert (0, componentPropertySeparator);
            }
            buf.insert (0, path);
         }
      }
      return buf.toString();
   }

   public static CompositeComponent getGrandParent (ModelComponent comp) {
      CompositeComponent parent = comp.getParent();
      if (parent == null) {
         return null;
      }
      return parent.getParent();
   }

   public static ModelComponent maybeGetCopy (
      int flags, Map<ModelComponent,ModelComponent> copyMap,CopyableComponent c) {

      ModelComponent cnew = c;
      if (c != null && (flags & CopyableComponent.COPY_REFERENCES) != 0) {
         cnew = copyMap.get (c);
         if (cnew == null) {
            cnew = c;
         }
      }
      return cnew;
   }

   public static ModelComponent maybeCopy (
      int flags, Map<ModelComponent,ModelComponent> copyMap,CopyableComponent c) {

      if (c != null && (flags & CopyableComponent.COPY_REFERENCES) != 0) {
         ModelComponent cnew = copyMap.get (c);
         if (cnew == null) {
            cnew = ((CopyableComponent)c).copy (flags, copyMap);
            copyMap.put (cnew, c);
            System.out.println (
               "ComponentUtils.maybeCopy: created "+cnew.getClass());
         }
         return cnew;
      }
      else {
         return c;
      }
   }
   
   @SuppressWarnings("unchecked")
   public static <E extends ModelComponent> void recursivelyFindComponents(Class<E> clazz, 
      CompositeComponent comp, List<E> out) {
   
     int n = comp.numComponents();
     
     for (int i=0; i<n; i++) {
        
        ModelComponent mc = comp.get(i);
        if (clazz.isAssignableFrom(mc.getClass())) {
           out.add((E)mc);
        }
        if (mc instanceof CompositeComponent) {
           recursivelyFindComponents(clazz, (CompositeComponent)mc, out);
        }
     }
   }

   public static String getWritePathName (
      CompositeComponent ancestor, ModelComponent comp) {
      if (comp==null) {
         return "null";
      }
      else {
         return Write.getQuotedString (
            getPathName (ancestor, comp, /* compact= */true));
      }
   }

   /**
    * Used for debugging. Recursively make sure that every descendent component
    * of this CompositeComponent has a valid path name.
    */
   public static void testPaths (CompositeComponent comp) {
      for (int idx=0; idx<comp.numComponents(); idx++) {
         ModelComponent c = comp.get(idx);
         if (c.getParent() != comp) {
            throw new InternalErrorException ("component '"
            + getPathName (c) + "' does not have parent '"
            + getPathName (comp) + "'");
         }
         if (c instanceof CompositeComponent) {
            testPaths ((CompositeComponent)c);
         }
         idx++;
      }
   }

   public static CompositeComponent castRefToAncestor (Object ref)
      throws IOException {
      try {
         return (CompositeComponent)ref;
      }
      catch (ClassCastException e) {
         throw new IOException ("reference object must be of type "
         + CompositeComponent.class.getName());
      }
   }

   protected static void recursivelyBuildDependencyMap (
      HashMap<ModelComponent,Dependencies> map,
      ModelComponent c) {

      ArrayList<ModelComponent> refs = new ArrayList<ModelComponent>();
      c.getHardReferences(refs);
      for (ModelComponent r : refs) {
         Dependencies deps = map.get(r);
         if (deps == null) {
            deps = new Dependencies();
            map.put (r, deps);
         }
         if (deps.myHard == null) {
            deps.myHard = new ArrayList<ModelComponent>();
         }
         deps.myHard.add (c);
      }
      refs.clear();
      c.getSoftReferences(refs);
      for (ModelComponent r : refs) {
         Dependencies deps = map.get(r);
         if (deps == null) {
            deps = new Dependencies();
            map.put (r, deps);
         }
         if (deps.mySoft == null) {
            deps.mySoft = new ArrayList<ModelComponent>();
         }
         deps.mySoft.add (c);
      }      
      if (c instanceof CompositeComponent) {
         CompositeComponent cc = (CompositeComponent)c;
         for (int i=0; i<cc.numComponents(); i++) {
            recursivelyBuildDependencyMap (map, cc.get(i));
         }
      }
   }

   protected static HashMap<ModelComponent,Dependencies>
      buildDependencyMap (
      CompositeComponent comp) {
      HashMap<ModelComponent,Dependencies> map =
         new HashMap<ModelComponent,Dependencies>();

      recursivelyBuildDependencyMap (map, comp);
      return map;
   }

   public static <C extends ModelComponent> boolean updateReferences (
      ModelComponent c, List<C> refs, boolean undo, Deque<Object> undoInfo) {

      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != ModelComponentBase.NULL_OBJ) {
            return ((ListRemove<C>)obj).undo();
         }
         else {
            return false;
         }
      }
      else {
         ListRemove<C> remove = null;
         if (refs != null) {
            for (int i=0; i<refs.size(); i++) {
               if (!ComponentUtils.areConnected (c, refs.get(i))) {
                  if (remove == null) {
                     remove = new ListRemove<C> (refs);
                  }
                  remove.requestRemove (i);
               }
            }
         }
         if (remove != null) {
            remove.remove();
            undoInfo.addLast (remove);
            return true;
         }
         else {
            undoInfo.addLast (ModelComponentBase.NULL_OBJ);
            return false;
         }
      }
   }

   public static void recursivelyConnect (
      ModelComponent comp, CompositeComponent connector) {
      comp.connectToHierarchy (connector);
      if (comp instanceof CompositeComponent) {
         CompositeComponent ccomp = (CompositeComponent)comp;
         for (int i=0; i<ccomp.numComponents(); i++) {
            recursivelyConnect (ccomp.get(i), connector);
         }
      }
   }

   public static void recursivelyDisconnect (
      ModelComponent comp, CompositeComponent connector) {
      comp.disconnectFromHierarchy (connector);
      if (comp instanceof CompositeComponent) {
         CompositeComponent ccomp = (CompositeComponent)comp;
         for (int i=0; i<ccomp.numComponents(); i++) {
            recursivelyDisconnect (ccomp.get(i), connector);
         }
      }
   }

}
