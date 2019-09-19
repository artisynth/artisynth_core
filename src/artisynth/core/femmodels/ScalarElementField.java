package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;
import artisynth.core.modelbase.FieldUtils.ScalarFieldFunction;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;

public class ScalarElementField extends ScalarFemField {

   protected DynamicDoubleArray myValues;
   protected DynamicBooleanArray myValuesSet;
   protected DynamicDoubleArray myShellValues;
   protected DynamicBooleanArray myShellValuesSet;

   protected class ElementFieldFunction 
      extends ScalarFieldFunction {

      public ElementFieldFunction () {
      }

      public ScalarElementField getField() {
         return ScalarElementField.this;
      }

      public double eval (FieldPoint def) {
         if (def.getElementType() == 0) {
            return getValue (def.getElementNumber());
         }
         else {
            return getShellValue (def.getElementNumber());
         }
      }
   }

   public ScalarFieldFunction createFieldFunction (boolean useRestPos) {
      return new ElementFieldFunction();
   }

   protected void initValues () {
      myValues = new DynamicDoubleArray();
      myValuesSet = new DynamicBooleanArray();
      myShellValues = new DynamicDoubleArray();
      myShellValuesSet = new DynamicBooleanArray();
      updateValueLists();
   }

   protected void updateValueLists() {
      int maxElements = myFem.getElements().getNumberLimit();
      int maxShellElements = myFem.getShellElements().getNumberLimit();
      myValues.resize (maxElements);
      myValuesSet.resize (maxElements);
      myShellValues.resize (maxShellElements);
      myShellValuesSet.resize (maxShellElements);
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public ScalarElementField () {
   }

   public ScalarElementField (FemModel3d fem, double defaultValue) {
      super (fem, defaultValue);
      initValues ();
   }

   public ScalarElementField (FemModel3d fem) {
      super (fem, 0);
      initValues ();
   }

   public ScalarElementField (String name, FemModel3d fem) {
      this (fem);
      setName (name);
   }

   public ScalarElementField (String name, FemModel3d fem, double defaultValue) {
      this (fem, defaultValue);
      setName (name);
   }

   public double getValue (int elemNum) {
      if (myValuesSet.get(elemNum)) {
         return myValues.get(elemNum);
      }
      else {
         return myDefaultValue;
      }
   }

   public double getShellValue (int elemNum) {
      if (myShellValuesSet.get(elemNum)) {
         return myShellValues.get(elemNum);
      }
      else {
         return myDefaultValue;
      }
   }

   public double getValue (FemElement3dBase elem) {
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         return getValue (elem.getNumber());
      }
      else {
         return getShellValue (elem.getNumber());
      }
   }

   public void setValue (FemElement3dBase elem, double value) {
      int elemNum = elem.getNumber();
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         myValues.set (elemNum, value);
         myValuesSet.set (elemNum, true);
      }
      else {
         myShellValues.set (elemNum, value);
         myShellValuesSet.set (elemNum, true);
      }
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("values=");
      writeValues (
         pw, fmt, myValues, myValuesSet, 
         new ElementWritableTest(myFem.getElements()));
      pw.print ("shellValues=");
      writeValues (
         pw, fmt, myShellValues, myShellValuesSet, 
         new ElementWritableTest(myFem.getShellElements()));
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "values")) {
         myValues = new DynamicDoubleArray();
         myValuesSet = new DynamicBooleanArray();
         scanValues (rtok, myValues, myValuesSet);
         return true;
      }
      else if (scanAttributeName (rtok, "shellValues")) {
         myShellValues = new DynamicDoubleArray();
         myShellValuesSet = new DynamicBooleanArray();
         scanValues (rtok, myShellValues, myShellValuesSet);
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
         if (myValuesSet.get(i)) {
            refs.add (myFem.getElements().getByNumber(i));
         }
      }
      for (int i=0; i<myShellValues.size(); i++) {
         if (myShellValuesSet.get(i)) {
            refs.add (myFem.getShellElements().getByNumber(i));
         }
      }
   }

//   private boolean elementIsReferenced (int num) {
//      return myFem.getElements().getByNumber(num) != null;
//   }
//
//   private boolean shellElementIsReferenced (int num) {
//      return myFem.getShellElements().getByNumber(num) != null;
//   }

   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      if (undo) {
         restoreReferencedValues (myValues, myValuesSet, undoInfo);
         restoreReferencedValues (myShellValues, myShellValuesSet, undoInfo);
      }
      else {
         removeUnreferencedValues (
            myValues, myValuesSet,
            new ElementReferencedTest(myFem.getElements()), undoInfo);
         removeUnreferencedValues (
            myShellValues, myShellValuesSet,
            new ElementReferencedTest(myFem.getShellElements()), undoInfo);
      }
   }

   public double getValue (Point3d pos) {
      Point3d loc = new Point3d();
      FemElement3dBase elem = myFem.findNearestElement (loc, pos);
      if (elem == null) {
         // shouldn't happen, but just in case
         return myDefaultValue;
      }
      return getValue (getElementIndex (elem));
   }
   

}
