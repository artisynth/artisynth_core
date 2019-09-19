package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;

import artisynth.core.modelbase.*;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;
import artisynth.core.modelbase.FieldUtils.VectorFieldFunction;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.render.*;

public class VectorElementField<T extends VectorObject<T>> 
   extends VectorFemField<T> {

   protected ArrayList<T> myValues;
   protected ArrayList<T> myShellValues;

   protected class ElementFieldFunction 
      extends VectorFieldFunction<T> {

      public ElementFieldFunction () {
      }

      public VectorElementField<T> getField() {
         return VectorElementField.this;
      }

      public T eval (FieldPoint def) {
         if (def.getElementType() == 0) {
            return getValue (def.getElementNumber());
         }
         else {
            return getShellValue (def.getElementNumber());
         }
      }
   }

   public VectorFieldFunction<T> createFieldFunction (boolean useRestPos) {
      return new ElementFieldFunction();
   }

   protected void initValues () {
      myValues = new ArrayList<T>();
      myShellValues = new ArrayList<T>();
      updateValueLists();
      setRenderProps (createRenderProps());
   }

   protected void updateValueLists() {
      resizeArrayList (myValues, myFem.getElements().getNumberLimit());
      resizeArrayList (myShellValues, myFem.getShellElements().getNumberLimit());
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorElementField (Class<T> type) {
      super (type);
   }

   public VectorElementField (Class<T> type, FemModel3d fem) {
      super (type, fem, null);
      initValues ();
   }

   public VectorElementField (Class<T> type, FemModel3d fem, T defaultValue) {
      super (type, fem, defaultValue);
      initValues ();
   }

   public VectorElementField (String name, Class<T> type, FemModel3d fem) {
      this (type, fem, null);
      setName (name);
   }

   public VectorElementField (
      String name, Class<T> type, FemModel3d fem, T defaultValue) {
      this (type, fem, defaultValue);
      setName (name);
   }

   public T getValue (int elemNum) {
      T value = myValues.get (elemNum);
      if (value == null) {
         return myDefaultValue;
      }
      else {
         return value;
      }
   }

   public T getShellValue (int elemNum) {
      T value = myShellValues.get (elemNum);
      if (value == null) {
         return myDefaultValue;
      }
      else {
         return value;
      }
   }

   public T getValue (FemElement3dBase elem) {
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         return getValue (elem.getNumber());
      }
      else {
         return getShellValue (elem.getNumber());
      }
   }

   public void setValue (FemElement3dBase elem, T value) {
      String sizeErr = checkSize (value);
      if (sizeErr != null) {
         throw new IllegalArgumentException (
            "value for elem "+elem.getNumber()+": "+sizeErr);
      }
      T storedValue = createInstance();
      storedValue.set (value);
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         myValues.set (elem.getNumber(), storedValue);
      }
      else {
         myShellValues.set (elem.getNumber(), storedValue);
      }
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("values=");
      writeValues (
         pw, fmt, myValues, 
         new ElementWritableTest(myFem.getElements()));
      pw.print ("shellValues=");
      writeValues (
         pw, fmt, myShellValues, 
         new ElementWritableTest(myFem.getShellElements()));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new ArrayList<T>();
         scanValues (rtok, myValues);
         return true;
      }
      else if (scanAttributeName (rtok, "shellValues")) {
         myShellValues = new ArrayList<T>();
         scanValues (rtok, myShellValues);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      updateValueLists();
   }

   public void getSoftReferences (List<ModelComponent> refs) {
      for (int i=0; i<myValues.size(); i++) {
         if (myValues.get(i) != null) {
            refs.add (myFem.getElements().getByNumber(i));
         }
      }
      for (int i=0; i<myShellValues.size(); i++) {
         if (myShellValues.get(i) != null) {
            refs.add (myFem.getShellElements().getByNumber(i));
         }
      }
   }

   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         restoreReferencedValues (myValues, undoInfo);
         restoreReferencedValues (myShellValues, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValues, new ElementReferencedTest (myFem.getElements()), 
            undoInfo);
         removeUnreferencedValues (
            myShellValues, new ElementReferencedTest (myFem.getShellElements()), 
            undoInfo);
      }
   }

   public T getValue (Point3d pos) {
      Point3d loc = new Point3d();
      FemElement3dBase elem = myFem.findNearestElement (loc, pos);
      if (elem == null) {
         // shouldn't happen, but just in case
         return myDefaultValue;
      }
      return getValue (elem);
   }

   // build render object for rendering Vector3d values

   protected RenderObject buildRenderObject() {
      if (myRenderScale != 0 && hasThreeVectorValue()) {
         RenderObject robj = new RenderObject();
         robj.createLineGroup();
         Point3d pos = new Point3d();
         Vector3d vec = new Vector3d();
         for (int num=0; num<myValues.size(); num++) {
            if (getThreeVectorValue (vec, myValues.get(num))) {
               FemElement3d e = myFem.getElements().getByNumber(num);
               e.computeCentroid (pos);
               addLineSegment (robj, pos, vec);
            }
         }
         for (int num=0; num<myShellValues.size(); num++) {
            if (getThreeVectorValue (vec, myShellValues.get(num))) {
               ShellElement3d e = myFem.getShellElements().getByNumber(num);
               e.computeCentroid (pos);
               addLineSegment (robj, pos, vec);
            }
         }
         return robj;
      }
      else {
         return null;
      }
   }
}
