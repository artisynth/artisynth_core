package artisynth.core.femmodels;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;

import java.util.ArrayList;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.PropertyDesc.TypeCode;
import maspack.properties.PropertyDesc;

public abstract class VectorFemField<T extends VectorObject<T>>
   extends FemFieldComp implements VectorField<T>, ParameterizedClass {
   
   protected T myDefaultValue = null;
   protected Class<T> myTypeParameter = null;
   protected TypeCode myValueType = TypeCode.OTHER;

   protected void initType (Class<T> type) {
      myTypeParameter = type;
      myValueType = PropertyDesc.getTypeCode(type);
   }
   
   public Class<T> getTypeParameter() {
      return myTypeParameter;
   }   

   public boolean hasParameterizedType() {
      return true;
   }   

   protected String checkSize (T value) {
      // size check only needed when T is VectorNd or MatrixNd
      return null;
   } 

   protected T createInstance () {
      try {
         return myTypeParameter.newInstance();
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Cannot create instance of "+myTypeParameter);
      }
   }

   protected void initFemAndDefaultValue (FemModel3d fem, T defaultValue) {
      if (defaultValue == null) {
         myDefaultValue = createInstance();
      }
      else {
         String sizeErr = checkSize (defaultValue);
         if (sizeErr != null) {
            throw new IllegalArgumentException (
               "default value: "+sizeErr);
         }
         myDefaultValue = defaultValue;
      }
      setFem (fem);
   }     

   public VectorFemField (Class<T> type) {
      initType (type);
   }

   public VectorFemField (Class<T> type, FemModel3d fem) {
      initType (type);
      initFemAndDefaultValue (fem, null);
   }

   public VectorFemField (Class<T> type, FemModel3d fem, T defaultValue) {
      initType (type);
      initFemAndDefaultValue (fem, defaultValue);
   }

   protected T scanValue (ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
         return null;
      }
      else {
         rtok.pushBack();
         T value = (T)PropertyDesc.scanValue (
            rtok, myValueType, myTypeParameter);
         String sizeErr = checkSize (value);
         if (sizeErr != null) {
            throw new IOException (
               "scanned value: "+sizeErr+", line "+rtok.lineno());
         }
         return value;
      }
   }

   protected <S> void writeValues (
      PrintWriter pw, NumberFormat fmt, ArrayList<S> values) throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (S val : values) {
         if (val == null) {
            pw.println ("null");
         }
         else {
            PropertyDesc.writeValue (
               val, pw, myValueType, myTypeParameter, fmt, null, null);
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected <S> void writeValueArrays (
      PrintWriter pw, NumberFormat fmt,
      ArrayList<S[]> valueArrays) throws IOException {

      pw.println ("[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (S[] varray : valueArrays) {
         if (varray == null) {
            pw.println ("null");
         }
         else {
            pw.print ("[ ");
            IndentingPrintWriter.addIndentation (pw, 2);
            for (int k=0; k<varray.length; k++) {
               writeValue (pw, fmt, varray[k]);
            }
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }
 
   protected <S> void scanValues (
      ReaderTokenizer rtok, ArrayList<S> values) throws IOException {
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         S value;
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            value = null;
         }
         else {
            rtok.pushBack();
            value = (S)PropertyDesc.scanValue (
               rtok, myValueType, myTypeParameter);
         }
         values.add (value);
      }
   }

   protected <S> void scanValueArrays (
      ReaderTokenizer rtok, ArrayList<S[]> valueArrays) throws IOException {
      ArrayList<S> values = new ArrayList<>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
            valueArrays.add (null);
         }
         else {
            if (rtok.ttype != '[') {
               throw new IOException ("Expecting token '[', got "+rtok);
            }
            values.clear();
            while (rtok.nextToken() != ']') {
               S value;
               if (rtok.tokenIsWord() && rtok.sval.equals ("null")) {
                  value = null;
               }
               else {
                  rtok.pushBack();
                  value = (S)PropertyDesc.scanValue (
                     rtok, myValueType, myTypeParameter);
               }
               values.add (value);              
            }
            valueArrays.add ((S[])values.toArray());
         }
      }
   }

   protected <S> void writeValue (
      PrintWriter pw, NumberFormat fmt, S val) throws IOException {
      if (val == null) {
         pw.println ("null");
      }
      else {
         PropertyDesc.writeValue (
            val, pw, myValueType, myTypeParameter, fmt, null, /*ancestor=*/null);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "type")) {
         Class<T> clazz = (Class<T>)Scan.scanClass (rtok);
         initType (clazz);
         return true;
      }
      else if (scanAttributeName (rtok, "defaultValue")) {
         myDefaultValue = scanValue (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);      
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("type=" + myTypeParameter.getName());
      pw.print ("defaultValue=");
      writeValue (pw, fmt, myDefaultValue);
   }


   public abstract T getValue (Point3d pos);
}
