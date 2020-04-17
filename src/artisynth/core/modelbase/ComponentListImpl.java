/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;
import java.io.*;

import artisynth.core.util.*;
import artisynth.core.modelbase.ScanWriteUtils.ClassInfo;
import maspack.matrix.VectorObject;
import maspack.properties.*;
import maspack.util.*;
import maspack.util.ParameterizedClass;

public class ComponentListImpl<C extends ModelComponent> extends ScannableList<C>
   implements Iterable<C>, IndexedComponentList {

   // if not -1, indices at and beyond this value need to revalidated
   protected int myValidateIndices = -1;
   // if true, we need to reset all indices in number map
   protected boolean myResetIndices = false;
   // counts components as they are scanned in
   protected int myScanCnt;
   // CompositeComponet that we are implementing for
   protected CompositeComponent myComp;
   protected boolean myZeroBasedNumbering = true;

   protected ComponentMap myComponentMap;
   
   public ComponentListImpl (Class<C> type, CompositeComponent comp) {
      super (type);
      myComp = comp;
      myComponentMap = new ComponentMap();
   }

   // ========== Begin CompositeComponent implementation ==========
   
   protected void validateIndices() {
      if (myValidateIndices != -1) {
         if (myResetIndices) {
            myComponentMap.clearIndices();
            myResetIndices = false;
         }
         for (int i=myValidateIndices; i<size(); i++) {
            myComponentMap.resetIndex (get(i), i);
         }
         myValidateIndices = -1;
      }
   }

   public C get (String nameOrNumber) {
      if (myValidateIndices != -1) {
         validateIndices();
      }
      return (C)myComponentMap.getByNameOrNumber (nameOrNumber, this);
   }

   /**
    * get (int idx) is provided by ScannableList
    */

   public C getByNumber (int num) {
      if (myValidateIndices != -1) {
         validateIndices();
      }
      return (C)myComponentMap.getByNumber (num, this);
   }

   /**
    * {@inheritDoc}
    */
   public int numComponents() {
      return size();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int indexOf (ModelComponent comp) {
      if (myValidateIndices != -1) {
         validateIndices();
      }
      return myComponentMap.getIndex (comp.getNumber());
   }

   public int getNumberLimit() {
      validateIndices ();
      return myComponentMap.getNumberLimit();
   }

   public void componentChanged (ComponentChangeEvent e) {
      if (e.getCode() == ComponentChangeEvent.Code.NAME_CHANGED) {
         ModelComponent c = e.getComponent();
         if (c.getParent() == myComp) {
            updateNameMap (c.getName(), ((NameChangeEvent)e).getOldName(), c);
         }
      }
   }

   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponentMap.updateNameMap (newName, oldName, comp);
   }

   public int nextComponentNumber() {
      validateIndices ();
      return myComponentMap.nextNumber();
   }

   // ========== End CompositeComponent implementation ==========

   // ========== Begin MutableCompositeComponent implementation ===== 

   /**
    * {@inheritDoc}
    */
   public boolean add (C comp) {
      return addNumbered (comp, -1);
   }
   
   public void add (C comp, int idx) {
      addComponents (new ModelComponent[] { comp }, new int[] { idx }, 1);
   }

   public boolean addNumbered (C comp, int number) {
      if (number != -1 && this.getByNumber (number) != null) {
         number = -1;
      }
      boolean status = doAdd (comp, number);
      notifyStructureChanged (myComp, comp.hasState());
      return status;
   }
   
   public C set(int idx, C comp) {
      C prev = get(idx);
      int number = prev.getNumber();
      return setNumbered(idx, comp, number);
   }
   
   public C setNumbered (int idx, C comp, int number) {
      C status = doSet(idx, comp, number);
      notifyStructureChanged (myComp, comp.hasState());
      return status;
   }

   // prepare a component for insertion in the list
   private void initComponent (C comp, int number, int idx) {
      validateIndices ();
      if (number == -1) {
         number = myComponentMap.nextNumber();
      }
      ModelComponentBase.checkNameUniqueness (comp, myComp);      
      comp.setNumber (number);
      comp.setParent (myComp);
      myComponentMap.mapComponent (comp, idx, number);     
   }
   
   private boolean doAdd (C comp, int number) {
      // assume here that super.add always returns true
      initComponent (comp, number, size());
      super.add (comp);
      try {
         //ComponentUtils.checkReferenceContainment (comp);
         ComponentUtils.recursivelyConnect (comp, myComp);
      }
      catch (RuntimeException e) {
         // if connectToHierarchy() throws an exception, remove the component
         // and throw the exception back to the application
         super.remove (comp);
         myComponentMap.unmapComponent (comp);
         comp.setParent (null);
         throw e;
      }
      PropertyUtils.updateAllInheritedProperties (comp);
      return true;
   }

   private C doSet (int idx, C comp, int number) {
      C prev = get(idx);
      if (prev != null) {
         ComponentUtils.recursivelyDisconnect (prev, myComp);
         clearComponent (prev);
      }
      initComponent (comp, number, idx);
      super.set (idx, comp);
      ComponentUtils.recursivelyConnect (comp, myComp);
      PropertyUtils.updateAllInheritedProperties (comp);
      return prev;
   }

   // returns true if the indicated components do not have state
   private boolean isStateless (ModelComponent[] comps, int num) {
      for (int i=0; i<num; i++) {
         if (comps[i].hasState()) {
            return false;
         }
      }
      return true;
   }

   /**
    * Returns true if the added components are stateless
    */
   public void addComponents (
      ModelComponent[] comps, int[] indices, int ncomps) {

      if (comps.length < ncomps) {
         throw new IllegalArgumentException (
            "component array does not contain 'ncomps' components");
      }
      if (indices != null && indices.length < ncomps) {
         throw new IllegalArgumentException (
            "index array does not contain 'ncomps' components");
      }
      // easy way: allocate a temp array
      modCount++;
      int newSize = mySize + ncomps;
      try {
         //Class currentClass = null;
         //PropTreeCell inheritedVals = null;
         if (ncomps > 1) {
            ModelComponent[] temp = new ModelComponent[newSize];

            for (int k = 0; k < ncomps; k++) {
               ModelComponent comp = comps[k];
               // if no specified indices, place at end of list
               int idx = (indices == null ? mySize + k : indices[k]);
               if (idx >= newSize) {
                  throw new IndexOutOfBoundsException (
                     "Index: " + idx + ", Size: " + newSize);
               }
               temp[idx] = comp;
               initComponent ((C)comp, -1, idx);
            }
            // now consolidate list:
            ensureCapacity (newSize);
            int k = mySize - 1;
            for (int i = newSize - 1; i >= 0; i--) {
               if (temp[i] == null) {
                  myArray[i] = myArray[k--];
               }
               else {
                  myArray[i] = (C)temp[i];
               }
               myComponentMap.resetIndex (myArray[i], i);
               // myArray[i].setIndex (i);
            }
         }
         else {
            // if no specified indices, place at end of list
            int idx = (indices == null ? mySize : indices[0]);
            if (idx >= newSize) {
               throw new IndexOutOfBoundsException (
                  "Index: " + idx + ", Size: " + newSize);
            }
            ModelComponent comp = comps[0];
            initComponent ((C)comp, -1, idx);
            ensureCapacity (newSize);
            for (int i = mySize; i > idx; i--) {
               myArray[i] = myArray[i - 1];
               myComponentMap.resetIndex (myArray[i], i);
               // myArray[i].setIndex (i);
            }
            myArray[idx] = (C)comp;
            // comp.setIndex (idx);
         }
      }
      catch (ClassCastException e) {
         throw new IllegalArgumentException (
            "Components must be of type " + myComponentType);
      }

      mySize = newSize;
      boolean stateless = true;
      // now finish the connecting the components:
      PropTreeCell inheritedVals = null;
      Class currentClass = null;
      for (int i=0; i<ncomps; i++) {
         ModelComponent comp = comps[i];
         ComponentUtils.recursivelyConnect (comp, myComp);
         if (comp.hasState()) {
            stateless = false;
         }
         if (currentClass != comp.getClass()) {
            // inheritedVals contains information about inherited values for a
            // specific class that can be reused with updating inherited
            // properties for the same class.
            inheritedVals =
               PropertyUtils.updateAllInheritedProperties (comp);
            currentClass = comp.getClass();
         }
         else if (inheritedVals != null) {
            PropertyUtils.updateInheritedProperties (comp, inheritedVals);
         }
      }
      notifyStructureChanged (myComp, !stateless);
   }

   /**
    * {@inheritDoc}
    */
   public boolean remove (Object obj) {
      if (obj instanceof ModelComponent) {
         ModelComponent comp = (ModelComponent)obj;
         int idx = indexOf (comp);
         if (idx != -1) {
            remove (idx);
            return true;
         }
         else {
            return false;
         }
      }
      else {
         return false;
      }
   }

   public C remove (int idx) {
      if (idx >= size()) {
         throw new ArrayIndexOutOfBoundsException ("index: " + idx + " size: "
         + size());
      }
      if (myValidateIndices == -1 || idx < myValidateIndices) {
         myValidateIndices = idx;
      }
      C comp = super.remove (idx);
      ComponentUtils.recursivelyDisconnect (comp, myComp);
      clearComponent (comp);
      notifyStructureChanged (myComp, comp.hasState());
      return comp;
   }

   /**
    * Returns true if the added components are stateless
    */
   public void removeComponents (
      ModelComponent[] comps, int[] indices, int ncomps) {

      if (comps.length < ncomps) {
         throw new IllegalArgumentException (
            "component array does not contain 'ncomps' components");
      }
      if (indices != null && indices.length < ncomps) {
         throw new IllegalArgumentException (
            "index array does not contain 'ncomps' components");
      }
      modCount++;

      // mark the component for removal and call the removal code
      try {
         for (int k = 0; k < ncomps; k++) {
            ModelComponent c = comps[k];
            if (c.getParent() != myComp) {
               throw new IllegalArgumentException ("component not present");
            }
            if (c.isMarked()) {
               throw new InternalErrorException (
                  "some components specified for removal are marked");
            }
            ComponentUtils.recursivelyDisconnect (c, myComp);
            //myComponentMap.unmapComponent (c);
            //clearComponent ((C)c);
            c.setMarked (true);
            // save numbers since we will use the number field to temporarily
            // store indices
            if (indices != null) {
               indices[k] = c.getNumber();
            }
         }
      }
      catch (ClassCastException e) {
         throw e;
      }
      
      validateIndices ();
      int k = 0;
      for (int i = 0; i < mySize; i++) {
         C c = myArray[i];
         if (c.isMarked()) {
            if (indices != null) {
               c.setNumber (i); // temporarily store index in number field
            }
         }
         else {
            if (k < i) {
               myArray[k] = c;
               myComponentMap.resetIndex (c, k);
            }
            k++;
         }
      }
      if (mySize - k != ncomps) {
         throw new InternalErrorException (
            "list contains previously marked components");
      }
      // unmark the components and build the index list
      for (k = 0; k < ncomps; k++) {
         ModelComponent c = comps[k];
         c.setMarked (false);
         if (indices != null) {
            int num = indices[k];
            indices[k] = c.getNumber();
            c.setNumber (num);
         }
         clearComponent ((C)c);
      }
      mySize -= ncomps;
      notifyStructureChanged (myComp, !isStateless(comps, ncomps));
   }

   // returns true if stateless
   public void removeAll() {
      modCount++;
      boolean stateless = isStateless (myArray, mySize);
      for (int i = 0; i < mySize; i++) {
         C comp = myArray[i];
         ComponentUtils.recursivelyDisconnect (comp, myComp);
         comp.setParent (null);
         myArray[i] = null;
      }
      mySize = 0;
      myComponentMap.clear();
      notifyStructureChanged (myComp, !stateless);
   }

   public boolean removeAll (Collection<?> objs) {
      HashSet<ModelComponent> comps = new HashSet<ModelComponent>();
      for (Object obj : objs) {
         if (obj instanceof ModelComponent) {
            ModelComponent c = ((ModelComponent)obj);
            if (c.getParent() == myComp) {
               comps.add (c);
            }
         }
      }
      if (comps.size() > 0) {
         removeComponents (
            comps.toArray(new ModelComponent[0]), /*indices=*/null, comps.size());
         return true;
      }
      else {
         return false;
      }
   }

   private void clearComponent (C comp) {      
      myComponentMap.unmapComponent (comp);
      comp.setParent (null);
   }

   public void clear() {
      removeAll();
   }

   // ========== End MutableCompositeComponent implementation ===== 

   public void setZeroBasedNumbering (boolean enable) {
      if (myZeroBasedNumbering != enable) {
         int inc = enable ? -1 : 1;
         myComponentMap.incrementNumbers (inc);
         for (ModelComponent mc : this) {
            mc.setNumber (mc.getNumber()+inc);
         }
         myZeroBasedNumbering = enable;
      }
   }

   public boolean getZeroBasedNumbering() {
      return myZeroBasedNumbering;
   }

   protected void printClassTagIfNecessary (PrintWriter pw, C comp) {
      Class<?> paramType = null;
      if (comp instanceof ParameterizedClass) {
         ParameterizedClass pcomp = (ParameterizedClass)comp;
         if (pcomp.hasParameterizedType()) {
            paramType = pcomp.getParameterType();
         }
      }
      //if (!comp.getClass().isAssignableFrom (myComponentType) ||
      if (comp.getClass() != myComponentType || paramType != null) {
         String classTag;
         if (paramType == null) {
            classTag = ScanWriteUtils.getClassTag(comp);
         }
         else {
            classTag = ScanWriteUtils.getParameterizedClassTag(
               comp, paramType);
         }
         pw.print (classTag + " ");
      }
   }

   public void writeComponents (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      int i = 0;
      for (C comp : this) {
         if (comp.isWritable()) {
            int num = comp.getNumber();
            if (num != i) {
               pw.print (num + " ");
            }
            printClassTagIfNecessary (pw, comp);
            if (myComp.hierarchyContainsReferences()) {
               comp.write (pw, fmt, myComp);
            }
            else {
               comp.write (pw, fmt, ancestor);
            }
            i++;
         }
      }
   }

   public void writeComponentsByName (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      for (ModelComponent comp : this) {
         if (comp.isWritable()) {
            pw.print (comp.getName() + "=");
            if (myComp.hierarchyContainsReferences()) {
               comp.write (pw, fmt, myComp);
            }
            else {
               comp.write (pw, fmt, ancestor);
            }
         }
      }
   }

   protected boolean scannedCompMatches (
      C comp, ClassInfo<C> classInfo, int number)
      throws IOException {

      Class<?> cls =
         (classInfo != null ? classInfo.compClass : getTypeParameter());

      if (comp.getClass() != cls) {
         return false;
      }
      else if (comp instanceof ComponentList) {
         ComponentList clist = (ComponentList)comp;
         if (clist.hasParameterizedType() &&
             clist.getParameterType() != classInfo.typeParam) {
            return false;
         }
      }
      if (number != -1 && comp.getNumber() != number) {
         return false;
      }
      else {
         return true;
      }
   }

   public boolean scanAndStoreComponentByName (
      ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      if (rtok.ttype == ReaderTokenizer.TT_WORD) {
         ModelComponent comp = get (rtok.sval);
         if (comp != null) {
            rtok.scanToken ('=');
            tokens.offer (new ObjectToken (comp, rtok.lineno()));
            comp.scan (rtok, tokens);
            return true;
         }
      }
      return false;
   }

   // For testing: keep a list of classes that have been scanned so we can
   // estimate the coverage of write/scan testing
   //private static HashSet<Class<?>> myScannedClasses = new HashSet<Class<?>>();

   public void scanBegin() {
      myScanCnt = 0;
   }

   // returns true if stateless
   public void scanEnd() {
      if (myScanCnt > 0) {
         // new components were created and added
         boolean stateless = true;
         for (int i=0; i<size(); i++) {
            if (get(i).hasState()) {
               stateless = false;
            }
         }
         myComponentMap.collectFreeNumbers();
         notifyStructureChanged (myComp, !stateless);
      }
   }

   public boolean scanAndStoreComponent (
      ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      int compNumber = -1;
      ClassInfo<C> classInfo = null;
      if (rtok.tokenIsInteger()) {
         compNumber = (int)rtok.lval;
         if (compNumber < 0) {
            throw new IOException (
               "Negative component number, line " + rtok.lineno());
         }
         rtok.nextToken();
      }
      classInfo = scanClassTagIfPresent (rtok);
      C comp;
      if (myScanCnt < size()) {
         // use or replace existing component
         comp = get(myScanCnt);
         C newcomp = null;
         if (!comp.isFixed() ||
             !scannedCompMatches (comp, classInfo, compNumber)) {
            // try creating a new component to replace the existing one
            newcomp = newComponent(rtok, classInfo, /*warnOnly=*/true);
         }
         if (newcomp != null) {
            // was able to create a new component. Use this to replace the
            // old component
            ComponentUtils.recursivelyDisconnect (comp, myComp);
            clearComponent (comp);
            if (compNumber == -1) {
               compNumber = comp.getNumber();
            }
            comp = newcomp;
            tokens.offer (new ObjectToken (comp, rtok.lineno()));
            comp.scan (rtok, tokens);
            initComponent (comp, compNumber, myScanCnt);
            super.set (myScanCnt, comp);
         }
         else {
            // want to keep the old component, or new component could
            // not be created
            tokens.offer (new ObjectToken (comp, rtok.lineno()));
            comp.scan (rtok, tokens);
         }
         myScanCnt++;
      }
      else {
         // scan new component
         comp = newComponent(rtok, classInfo, /*warnOnly=*/true);
         if (comp != null) {
            tokens.offer (new ObjectToken (comp, rtok.lineno()));
            comp.scan (rtok, tokens);
            initComponent (comp, compNumber, size());
            super.add (comp);
            myScanCnt++;
         }
         else {
            ScanWriteUtils.scanAndDiscard (rtok);
         }
      }
      return true;
   }

   public boolean postscanComponent (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
      throws IOException {

      ScanToken tok = ScanWriteUtils.postscanComponentToken (tokens);
      if (tok != null) {
         ModelComponent comp = (ModelComponent)tok.value();
         if (!comp.isFixed()) {
            //comp.setParent (null);
            comp.postscan (tokens, ancestor);
            //comp.setParent (myComp);
            if (!ScanWriteUtils.connectAfterScanning) {
               try {
                  //ComponentUtils.checkReferenceContainment (comp);
                  comp.connectToHierarchy (myComp);
               }
               catch (Exception e) {
                  throw new IOException (
                     "Cannot connect component to hierarchy, component type "+
                     comp.getClass() + ", near line " + tok.lineno(), e);
               }
               PropertyUtils.updateAllInheritedProperties (comp);
            }
         }
         else {
            comp.postscan (tokens, ancestor);
         }
         return true;
      }     
      return false;
   }

   protected void notifyStructureChanged (Object comp) {
      notifyStructureChanged (comp, /*stateIsChanged=*/true);
   }
        
   protected void notifyStructureChanged (Object comp, boolean stateIsChanged) {
      if (comp instanceof CompositeComponent) {
         myComp.notifyParentOfChange (
            new StructureChangeEvent ((CompositeComponent)comp,stateIsChanged));
      }
      else if (!stateIsChanged) {
         myComp.notifyParentOfChange (
            StructureChangeEvent.defaultStateNotChangedEvent);
      }
      else {
         myComp.notifyParentOfChange (
            StructureChangeEvent.defaultEvent);
      }
   }

   /**
    * Reset the number map so that numbers and indices match.
    */
   public void resetNumbersToIndices() {
      myComponentMap.resetNumbersToIndices (size());
      for (int i=0; i<size(); i++) {
         get(i).setNumber(i);
      }
   }

   /**
    * Force update of numbering (for example, if a component's number
    * has been manually changed)
    */
   public void invalidateNumbers() {
      myValidateIndices = 0;
      myResetIndices = true;
   }

}
