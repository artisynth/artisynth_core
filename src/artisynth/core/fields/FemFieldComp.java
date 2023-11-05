package artisynth.core.fields;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import java.util.ArrayList;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.PointList;
import artisynth.core.femmodels.FemElement;
import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemElement3dList;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

public abstract class FemFieldComp
   extends ModelComponentBase implements FieldComponent {
   
   protected FemModel3d myFem;
   protected RenderProps myRenderProps;

   protected void setFem (FemModel3d fem) {
      myFem = fem;
   }

   public FemModel3d getFemModel() {
      return myFem;
   }
   
   /**
    * Check to ensure a particular node belongs to this field's FEM model.
    */
   protected void checkNodeBelongsToFem (FemNode3d node) {
      if (node.getGrandParent() != myFem) {
         throw new IllegalArgumentException (
            "Node does not belong to this field's FEM model");
      }
   }

   /**
    * Check to ensure a particular element belongs to this field's FEM model.
    */
   protected void checkElementBelongsToFem (FemElement3dBase elem) {
      if (elem.getGrandParent() != myFem) {
         throw new IllegalArgumentException (
            "Element does not belong to this field's FEM model");
      }
   }

   protected String elemName (FemElement3dBase elem) {
      if (elem.getElementClass() != ElementClass.VOLUMETRIC) {
         return "shell element "+elem.getNumber();
      }
      else {
         return "volumetric element "+elem.getNumber();
      }
   }

   /**
    * Should be called by subclasses whenever the field values are changed.
    */
   protected void notifyValuesChanged() {
   }

   /* ---- Begin I/O methods ---- */

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "fem", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("fem=" + ComponentUtils.getWritePathName (ancestor, myFem));
   }

   protected String elemName (ElementClass eclass, int num) {
      if (eclass == ElementClass.VOLUMETRIC) {
         return "volumetric element number "+num;
      }
      else {
         return "shell element number"+num;
      }
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "fem")) {
         FemModel3d fem = 
            ScanWriteUtils.postscanReference (
               tokens, FemModel3d.class, ancestor);
         setFem (fem);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected interface ReferenceTest {
      boolean isReferenced (int num);
   }

   protected interface WritableTest {
      boolean isWritable (int num);
   }

   protected class ElementReferencedTest implements ReferenceTest {
      FemElement3dList<? extends FemElement3dBase> myList;

      ElementReferencedTest (FemElement3dList<? extends FemElement3dBase> list) {
         myList = list;
      }

      public boolean isReferenced (int num) {
         return myList.getByNumber (num) != null;
      }      
   }

   protected class NodeReferencedTest implements ReferenceTest {
      PointList<FemNode3d> myList;

      NodeReferencedTest (PointList<FemNode3d> list) {
         myList = list;
      }

      public boolean isReferenced (int num) {
         return myList.getByNumber (num) != null;
      }      
   }

   protected class ElementWritableTest implements WritableTest {
      FemElement3dList<? extends FemElement3dBase> myList;

      ElementWritableTest (FemElement3dList<? extends FemElement3dBase> list) {
         myList = list;
      }

      public boolean isWritable (int num) {
         FemElement3dBase e = myList.getByNumber(num);
         return e != null && e.isWritable();
      }      
   }

   protected class NodeWritableTest implements WritableTest {
      PointList<FemNode3d> myList;

      NodeWritableTest (PointList<FemNode3d> list) {
         myList = list;
      }

      public boolean isWritable (int num) {
         FemNode3d n = myList.getByNumber(num);
         return n != null && n.isWritable();
      }      
   }
   
   /* ---- begin edit methods ---- */

   protected static class NumDoublePair {
      int myNum;
      double myValue;

      NumDoublePair (int num, double value) {
         myNum = num;
         myValue = value;
      }
   }
   
   protected void removeUnreferencedValues (
      DynamicDoubleArray values, DynamicBooleanArray valuesSet,
      ReferenceTest test, Deque<Object> undoInfo) {

      ArrayList<NumDoublePair> removedValues = new ArrayList<>();
      for (int i=0; i<values.size(); i++) {
         if (valuesSet.get(i)) {
            if (!test.isReferenced(i)) {
               removedValues.add (new NumDoublePair (i, values.get(i)));
               valuesSet.set (i, false);
            }
         }
      }
      if (removedValues.size() > 0) {
         undoInfo.addLast (removedValues);
         notifyValuesChanged();
      }
      else {
         undoInfo.addLast (NULL_OBJ);
      }     
   }

   protected void restoreReferencedValues (
      DynamicDoubleArray values, DynamicBooleanArray valuesSet,
      Deque<Object> undoInfo) {

      Object obj = undoInfo.removeFirst();
      if (obj != NULL_OBJ) {
         for (NumDoublePair pair : (ArrayList<NumDoublePair>)obj) {
            values.set (pair.myNum, pair.myValue);
            valuesSet.set (pair.myNum, true);
         }
         notifyValuesChanged();
      }
   }

   protected static class NumValuePair<T> {
      int myNum;
      T myValue;

      NumValuePair (int num, T value) {
         myNum = num;
         myValue = value;
      }
   }

   protected <T> void removeUnreferencedValues (
      ArrayList<T> values, ReferenceTest test, Deque<Object> undoInfo) {

      ArrayList<NumValuePair<T>> removedValues = new ArrayList<>();
      for (int i=0; i<values.size(); i++) {
         if (values.get(i) != null) {
            if (!test.isReferenced(i)) {
               removedValues.add (new NumValuePair<T> (i, values.get(i)));
               values.set (i, null);
            }
         }
      }
      if (removedValues.size() > 0) {
         undoInfo.addLast (removedValues);
         notifyValuesChanged();
      }
      else {
         undoInfo.addLast (NULL_OBJ);
      }     
   }

   protected <T> void restoreReferencedValues (
      ArrayList<T> values, Deque<Object> undoInfo) {

      Object obj = undoInfo.removeFirst();
      if (obj != NULL_OBJ) {
         for (NumValuePair<T> pair : (ArrayList<NumValuePair<T>>)obj) {
            values.set (pair.myNum, pair.myValue);
         }
         notifyValuesChanged();
      }
   }
   /**
    * {@inheritDoc}
    */
   public void clearCacheIfNecessary() {
   }

   protected void updateValueLists() {
   }

   protected <T> void resizeArrayList (
      ArrayList<T> list, int newsize) {
      while (list.size() < newsize) {
         list.add (null);
      }
      while (list.size() > newsize) {
         list.remove (list.size()-1);
      }
   }

   protected boolean allUnset (boolean[] valueset) {
      for (boolean b : valueset) {
         if (b) {
            return false;
         }
      }
      return true;
   }

   /* --- Begin partial implemetation of Renderable --- */

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps = RenderableComponentBase.updateRenderProps (
         this, myRenderProps, props);
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= IsRenderable.TRANSPARENT;
      }
      return code;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   // createRenderProps is defined in subclasses

   /* --- End partial implemetation of Renderable --- */

   /**
    * Returns {@code true} if this field is functionally equal to another field.
    * Intended mainly for testing and debugging.
    */
   public boolean equals (FemFieldComp comp) {
      return (myFem == comp.myFem);
   }
}
