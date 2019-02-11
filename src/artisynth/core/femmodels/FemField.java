package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

public class FemField<T> extends ModelComponentBase 
   implements IndexedField<T> {
   
   ArrayList<T> myValues;
   ArrayList<T> myIndexedValues;
   T myDefaultValue = null;
   Class<?> myValueClass = null;
   TypeCode myValueType = TypeCode.OTHER;
   FemModel3d myFem;

   private void setValueClass (Class<T> valueClass) {
      myValueClass = valueClass;
      myValueType = PropertyDesc.getTypeCode(valueClass);
   }
   
   protected void initialize (
      FemModel3d fem, int numValues, T defaultValue, Class<T> valueClass) {
      myValues = new ArrayList<T>(numValues);
      myDefaultValue = defaultValue;
      setValueClass (valueClass);
      myFem = fem;      
   }

   public FemField () {
   }

   public FemField (
      FemModel3d fem, int numValues, T defaultValue) {
      if (defaultValue == null) {
         throw new IllegalArgumentException (
            "Must specify value class in constructor if defaultValue is null");
      }
      initialize (fem, numValues, defaultValue, (Class<T>)defaultValue.getClass());
   }

   public FemField (
      FemModel3d fem, int numValues, T defaultValue, Class<T> valueClass) {
      initialize (fem, numValues, defaultValue, valueClass);
   }

   protected void setAndFill (
      ArrayList<T> list, int idx, T value, T defaultValue) {
      int k = list.size() - idx;
      if (k > 0) {
         list.set (idx, value);
      }
      else {
         while (k++ < 0) {
            list.add (defaultValue);
         }
         list.add (value);
      }      
   }

   protected ArrayList<T> getIndexedValues() {
      if (myIndexedValues == null) {
         myIndexedValues = new ArrayList<T>();
      }
      return myIndexedValues;
   }            

   public void clearIndexedValues() {
      myIndexedValues = null;
   }

   public void ensureIndexedCapacity (int cap) {
      ArrayList<T> indexedValues = getIndexedValues();
      indexedValues.ensureCapacity (cap);
   }

   public T getValue (int elemNum) {
      if (elemNum < myValues.size()) {
         return myValues.get(elemNum);
      }
      else {
         return myDefaultValue;
      }
   }

   public void setValue (int elemNum, T value) {
      setAndFill (myValues, elemNum, value, myDefaultValue);
   }

   public T getValue (Vector3d pnt) {
      Point3d loc = new Point3d();
      FemElement3dBase elem = myFem.findNearestElement (loc, new Point3d(pnt));
      // TODO: if loc != pnt, then we are outside the element and we may want
      // to handle this differently - like by returning the default value.     
      return getValue (elem.getNumber());
   }

   public T getValueByIndex (int idx) {
      ArrayList<T> indexedValues = getIndexedValues();
      if (idx < indexedValues.size()) {
         return indexedValues.get(idx);
      }
      else {
         return null;
      }
   }

   public void setValueByIndex (int idx, T value) {
      setAndFill (getIndexedValues(), idx, value, null);
   }

   protected T scanValue (ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
         return null;
      }
      else {
         rtok.pushBack();
         return (T)PropertyDesc.scanValue (rtok, myValueType, myValueClass);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "fem", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "valueClass")) {
         Class<T> clazz = (Class<T>)Scan.scanClass (rtok);
         setValueClass (clazz);
         return true;
      }
      else if (scanAttributeName (rtok, "defaultValue")) {
         myDefaultValue = scanValue (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "values")) {
         myValues = new ArrayList<T>();
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            myValues.add (scanValue (rtok));
         }
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
      pw.println ("valueClass=" + myValueClass.getName());
      pw.print ("defaultValue=");
      PropertyDesc.writeValue (
         myDefaultValue, pw, myValueType, myValueClass, fmt, null, ancestor);
      pw.println ("values=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (T val : myValues) {
         if (val == null) {
            pw.println ("null");
         }
         else {
            PropertyDesc.writeValue (
               val, pw, myValueType, myValueClass, fmt, null, ancestor);
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "fem")) {
         myFem = 
            ScanWriteUtils.postscanReference (
               tokens, FemModel3d.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }  
   
}
