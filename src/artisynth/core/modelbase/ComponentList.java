/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;
import java.io.*;

import artisynth.core.modelbase.CompositeComponent.NavpanelDisplay;
import artisynth.core.util.*;
import maspack.properties.*;
import maspack.util.*;
import maspack.util.ParameterizedClass;

public class ComponentList<C extends ModelComponent> extends ModelComponentBase
   implements ComponentListView<C>, MutableCompositeComponent<C>, Iterable<C>,
              Collection<C>, ParameterizedClass {

   // fields for CompositeComponent
   protected String myShortName;
   // if not -1, indices at and beyond this value need to revalidated
   protected boolean myEditableP = true;

   public static NavpanelDisplay DEFAULT_NAVPANEL_DISPLAY =
      NavpanelDisplay.NORMAL;
   protected NavpanelDisplay myNavpanelDisplay = DEFAULT_NAVPANEL_DISPLAY;

   protected ComponentListImpl<C> myComponents;

   public static PropertyList myProps =
      new PropertyList (ComponentList.class, ModelComponentBase.class);

   static {
      myProps.add ("shortName", "short name for this component", null, "NE");
      myProps.add (
         "navpanelDisplay", "navpanel display mode", DEFAULT_NAVPANEL_DISPLAY);
      myProps.setOptions ("navpanelVisibility", "AE");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ComponentList (Class<C> type) {
      super ();
      myComponents = new ComponentListImpl<C>(type, this);
   }

   public ComponentList (Class<C> type, String name) {
      this (type);
      setName (name);
   }

   public ComponentList (Class<C> type, String name, String shortName) {
      this (type);
      setName (name);
      setShortName (shortName);
   }

   // ========== Begin CompositeComponent implementation ==========

   /**
    * {@inheritDoc}
    */
   public C get (String nameOrNumber) {
      return myComponents.get (nameOrNumber);
   }

   /**
    * get (int idx) is provided by ScannableList
    */

   /**
    * {@inheritDoc}
    */
   public C getByNumber (int num) {
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
   @Override
   public int indexOf (ModelComponent comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * {@inheritDoc}
    */
   public C findComponent (String path) {
      return (C)ComponentUtils.findComponent (this, path);
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
      return myNavpanelDisplay;
   }
   
   /**
    * Sets the display mode for this component. This controls
    * how the component is displayed in a navigation panel. The default
    * setting is <code>NORMAL</code>.
    *
    * @param mode new display mode
    */
   public void setNavpanelDisplay (NavpanelDisplay mode) {
      if (myNavpanelDisplay != mode) {
         myNavpanelDisplay = mode;
         PropertyChangeEvent e = new PropertyChangeEvent (this, "navpanelDisplay");
         notifyParentOfChange (e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      notifyParentOfChange (e);
   }

   // XXX do we need this?
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

   /**
    * {@inheritDoc}
    */
   public int nextComponentNumber() {
      return myComponents.nextComponentNumber();
   }

   public int size() {
      return myComponents.size();
   }

   public C get(int idx) {
      return myComponents.get(idx);
   }

   public Iterator<C> iterator() {
      return myComponents.iterator();
   }

   public boolean contains (Object obj) {
      return myComponents.contains (obj);
   }

   public boolean isEmpty() {
      return myComponents.isEmpty();
   }

   public Class<C> getParameterType() {
      return myComponents.getTypeParameter();
   }
   
   public boolean hasParameterizedType() {
      return true;
   }
   
   public boolean addAll (Collection<? extends C> c) {
      return myComponents.addAll (c);
   }
   
   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      // hasChildren() might be called in the super() constructor, from the
      // property progagation code, before myComponents has been instantiated
      return myComponents != null && myComponents.size() > 0;
   }

   protected void setDefaultValues() {
      // setName (null);
      myFlags = 0;
   }

   // ========== End CompositeComponent implementation ==========

   // ========== Begin MutableCompositeComponent implementation ===== 

   /**
    * {@inheritDoc}
    */
   public boolean add (C comp) {
      return addNumbered (comp, -1);
   }

   public boolean addFixed (C comp) {
      comp.setFixed (true);
      return add (comp);
   }
   
   public void add (C comp, int idx) {
      addComponents (new ModelComponent[] { comp }, new int[] { idx }, 1);
   }

   // XXX we need this?
   public boolean addNumbered (C comp, int number) {
      
      return myComponents.addNumbered (comp, number);
      //notifyStructureChanged (this, !comp.hasState());
      //return status;
   }
   
   public C set (int idx, C comp) {
      return myComponents.set(idx, comp);
   }
   
   public C setNumbered (int idx, C comp, int number) {
      return myComponents.setNumbered(idx, comp, number);
   }

   /**
    * {@inheritDoc}
    */
   public void addComponents (ModelComponent[] comps, int[] indices, int ncomps) {
      //boolean stateless = myComponents.addComponents (comps, indices, ncomps);
      //notifyStructureChanged (this, stateless);
      myComponents.addComponents (comps, indices, ncomps);
   }

   /**
    * {@inheritDoc}
    */
   public boolean remove (Object obj) {
      return myComponents.remove (obj);
      // if (myComponents.remove (obj)) {
      //    notifyStructureChanged (this, !((ModelComponent)obj).hasState());
      //    return true;
      // }
      // else {
      //    return false;
      // }
   }

   public C remove (int idx) {
      return myComponents.remove (idx);
      // C comp = myComponents.remove (idx);
      // notifyStructureChanged (this, !comp.hasState());
      //return comp;
   }

   /**
    * {@inheritDoc}
    */
   public void removeComponents (
      ModelComponent[] comps, int[] indices, int ncomps) {
      // boolean stateless =
      //    myComponents.removeComponents (comps, indices, ncomps);
      // notifyStructureChanged (this, stateless);
      myComponents.removeComponents (comps, indices, ncomps);
   }

   public void removeAll () {
      // boolean stateless = myComponents.removeAll();
      // notifyStructureChanged (this, stateless);
      myComponents.removeAll();
   }

   public boolean retainAll (Collection<?> c) {
      return myComponents.retainAll(c);
   }

   public boolean removeAll (Collection<?> c) {
      return myComponents.removeAll(c);
   }

   public boolean containsAll (Collection<?> c) {
      return myComponents.containsAll(c);
   }

   public Object[] toArray() {
      return myComponents.toArray();      
   }

   public <C> C[] toArray (C[] array) {
      return myComponents.toArray(array);
   }

   public void clear() {
      removeAll();
   }

   public void ensureCapacity (int cap) {
      myComponents.ensureCapacity (cap);
   }         

   public boolean isEditable() {
      return myEditableP;
   }

   public void setEditable (boolean editable) {
      myEditableP = editable;
   }
   
   // ========== End MutableCompositeComponent implementation ===== 

   /**
    * Sets whether or not zero-based numbering is enabled for this component
    * list. Zero-based numbering implies that numbers start at zero.
    * Otherwise, numbers start at one.
    * 
    * @param enable if {@code true}, enabled zero-based numbering
    */
   public void setZeroBasedNumbering (boolean enable) {
      myComponents.setZeroBasedNumbering (enable);
   }

   /**
    * Queries if zero-based numbering is enabled for this component list.
    * See {@link #setZeroBasedNumbering}.
    * 
    * @return {@code true} if zero-based numbering is enabled 
    */
   public boolean getZeroBasedNumbering() {
      return myComponents.getZeroBasedNumbering();
   }

   public void setShortName (String name) {
      if (name != null && name.length() == 0) {
         throw new IllegalArgumentException (
            "non-null short name must have length >= 1");
      }
      if (myParent != null) {
         myParent.updateNameMap (name, myShortName, this);
      }
      myShortName = name;
   }

   public String getShortName() {
      return myShortName;
   }

   /**
    * Faster implementation using component parent pointer.
    */
   @Override
   public boolean contains (C comp) {
      return (comp.getParent() == this);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      if (hierarchyContainsReferences()) {
         ancestor = this;
      }
      super.writeItems (pw, fmt, ancestor);
      if (!myComponents.getZeroBasedNumbering()) {
         pw.println ("zeroBasedNumbering=false");
      }
      myComponents.writeComponents (pw, fmt, ancestor);
   }

   protected boolean scanAttributeName (
      ReaderTokenizer rtok, String name) throws IOException {
      if (rtok.ttype == ReaderTokenizer.TT_WORD && rtok.sval.equals (name)) {
         rtok.scanToken ('=');
         return true;
      }
      return false;
   }

   // For testing: keep a list of classes that have been scanned so we can
   // estimate the coverage of write/scan testing
   //private static HashSet<Class<?>> myScannedClasses = new HashSet<Class<?>>();

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clear();
      myComponents.scanBegin();
      super.scan (rtok, ref);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanProperty (rtok, this, tokens)) {
         return true;
      }
      else if (ScanWriteUtils.scanAttributeName (rtok, "zeroBasedNumbering")) {
         myComponents.setZeroBasedNumbering (rtok.scanBoolean());
         return true;
      }
      else if (myComponents.scanAndStoreComponent (rtok, tokens)) {
         return true;
      }
      rtok.pushBack();
      return false;
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
      throws IOException {

      if (ScanWriteUtils.postscanPropertyValue (tokens, ancestor)) {
         return true;
      }
      return myComponents.postscanComponent (tokens, ancestor);
   }

   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (hierarchyContainsReferences()) {
         ancestor = this;
      }
      super.postscan (tokens, ancestor);
      // boolean stateless = myComponents.scanEnd();
      // notifyStructureChanged (this, stateless);
      myComponents.scanEnd();
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

   public Object clone() throws CloneNotSupportedException {
      ComponentList comp = (ComponentList)super.clone();

      comp.myShortName = null;

      comp.myComponents =
         new ComponentListImpl<C>(myComponents.getTypeParameter(), comp);
      //comp.myComponents = (ComponentListImpl<C>)myComponents.clone();
      comp.myNavpanelDisplay = myNavpanelDisplay;
      return comp;
   }

   public ComponentList<C> copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      ComponentList<C> comp;
      try {
         comp = (ComponentList<C>)clone(); 
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException (
            "Clone not supported for " + getClass());
      }
      return comp;
   }
   
   /**
    * Invalidates stored numbers in cases where component numbers have been
    * manually changed 
    */
   public void invalidateNumbers() {
      myComponents.invalidateNumbers();
   }

   /**
    * Reset the numbering so that numbers and indices match.
    */
   public void resetNumbersToIndices() {
      myComponents.resetNumbersToIndices ();
   }
}
