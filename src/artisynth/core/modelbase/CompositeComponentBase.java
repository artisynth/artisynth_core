/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;

import maspack.util.*;
import maspack.properties.*;
import maspack.render.*;

import java.io.*;

import artisynth.core.modelbase.CompositeComponent.NavpanelDisplay;
import artisynth.core.util.*;

public class CompositeComponentBase extends ModelComponentBase
   implements CompositeComponent {

   protected ComponentListImpl<ModelComponent> myComponents =
      new ComponentListImpl<ModelComponent>(ModelComponent.class, this);

   private NavpanelDisplay myDisplayMode = NavpanelDisplay.NORMAL;

   public CompositeComponentBase (String name) {
      super (name);
   }

   public CompositeComponentBase() {
      this (null);
   }

   // ========== Begin ModelComponent overrides ==========

   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public Iterator<ModelComponent> iterator() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      // hasChildren() might be called in the super() constructor, from the
      // property progagation code, before myComponents has been instantiated
      return myComponents != null && myComponents.size() > 0;
   }

   public boolean hasState() {
      return true;
   }

   // ========== End ModelComponent overrides ==========

   // ========== Begin CompositeComponent implementation ==========

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (String nameOrNumber) {
      return myComponents.get (nameOrNumber);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (int idx) {
      return myComponents.get (idx);
   } 

   /**
    * {@inheritDoc}
    */
   public ModelComponent getByNumber (int num) {
      return myComponents.getByNumber (num);
   }

   /**
    * {@inheritDoc}
    */
   public int numComponents() {
      return myComponents.size();
   }

   /**
    * {@inheritDoc}
    */
   public int indexOf (ModelComponent comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent findComponent (String path) {
      return ComponentUtils.findComponent (this, path);
   }

   /**
    * {@inheritDoc}
    */
   public int getNumberLimit() {
      return myComponents.getNumberLimit();
   }

   /**
    * {@inheritDoc}
    */
   public NavpanelDisplay getNavpanelDisplay() {
      return myDisplayMode;
   }
   
   /**
    * Sets the display mode for this component. This controls
    * how the component is displayed in a navigation panel. The default
    * setting is <code>NORMAL</code>.
    *
    * @param mode new display mode
    */
   public void setDisplayMode (NavpanelDisplay mode) {
      myDisplayMode = mode;
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      notifyParentOfChange (e);
   }

   /**
    * {@inheritDoc}
    */
   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponents.updateNameMap (newName, oldName, comp);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hierarchyContainsReferences() {
      return false;
   }

   // ========== End CompositeComponent implementation ==========

   protected void add (ModelComponent comp) {
      myComponents.add (comp);
   }
   
   protected boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }
   
   protected void removeAll() {
      myComponents.removeAll();
   }

   protected void notifyStructureChanged (Object comp) {
      notifyStructureChanged (comp, /*stateIsChanged=*/true);
   }
        
   protected void notifyStructureChanged (Object comp, boolean stateIsChanged) {
      if (comp instanceof CompositeComponent) {
         notifyParentOfChange (
            new StructureChangeEvent ((CompositeComponent)comp,stateIsChanged));
      }
      else if (!stateIsChanged) {
         notifyParentOfChange (
            StructureChangeEvent.defaultStateNotChangedEvent);
      }
      else {
         notifyParentOfChange (
            StructureChangeEvent.defaultEvent);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanProperty (rtok, this, tokens)) {
         return true;
      }
      else if (myComponents.scanAndStoreComponentByName (rtok, tokens)) {
         return true;
      }
      rtok.pushBack();
      return false;
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (myComponents.postscanComponent (tokens, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
      public void scan (
         ReaderTokenizer rtok, Object ref) throws IOException {

      myComponents.scanBegin();
      super.scan (rtok, ref);
   }

   @Override
   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (hierarchyContainsReferences()) {
         ancestor = this;
      }
      super.postscan (tokens, ancestor);
      myComponents.scanEnd();
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      myComponents.writeComponentsByName (pw, fmt, ancestor);
   }

   public CompositeComponentBase copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      CompositeComponentBase ccomp =
         (CompositeComponentBase)super.copy (flags, copyMap);

      ccomp.myComponents =
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      ccomp.myDisplayMode = myDisplayMode;
      
      return ccomp;
   }

}
